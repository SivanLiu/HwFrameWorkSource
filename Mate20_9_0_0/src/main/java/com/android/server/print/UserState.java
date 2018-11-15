package com.android.server.print;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IInterface;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.print.IPrintDocumentAdapter;
import android.print.IPrintJobStateChangeListener;
import android.print.IPrintServicesChangeListener;
import android.print.IPrinterDiscoveryObserver;
import android.print.IPrinterDiscoveryObserver.Stub;
import android.print.PrintAttributes;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.print.PrinterInfo.Builder;
import android.printservice.PrintServiceInfo;
import android.printservice.recommendation.IRecommendationsChangeListener;
import android.printservice.recommendation.RecommendationInfo;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.BackgroundThread;
import com.android.internal.print.DumpUtils;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.pm.DumpState;
import com.android.server.print.RemotePrintService.PrintServiceCallbacks;
import com.android.server.print.RemotePrintServiceRecommendationService.RemotePrintServiceRecommendationServiceCallbacks;
import com.android.server.print.RemotePrintSpooler.PrintSpoolerCallbacks;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.IntSupplier;

final class UserState implements PrintSpoolerCallbacks, PrintServiceCallbacks, RemotePrintServiceRecommendationServiceCallbacks {
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final boolean DEBUG = true;
    private static final String LOG_TAG = "UserState";
    private static final int SERVICE_RESTART_DELAY_MILLIS = 500;
    private final ArrayMap<ComponentName, RemotePrintService> mActiveServices = new ArrayMap();
    private final Context mContext;
    private boolean mDestroyed;
    private final Set<ComponentName> mDisabledServices = new ArraySet();
    private final List<PrintServiceInfo> mInstalledServices = new ArrayList();
    private boolean mIsInstantServiceAllowed;
    private final Object mLock;
    private final PrintJobForAppCache mPrintJobForAppCache = new PrintJobForAppCache(this, null);
    private List<PrintJobStateChangeListenerRecord> mPrintJobStateChangeListenerRecords;
    private List<RecommendationInfo> mPrintServiceRecommendations;
    private List<ListenerRecord<IRecommendationsChangeListener>> mPrintServiceRecommendationsChangeListenerRecords;
    private RemotePrintServiceRecommendationService mPrintServiceRecommendationsService;
    private List<ListenerRecord<IPrintServicesChangeListener>> mPrintServicesChangeListenerRecords;
    private PrinterDiscoverySessionMediator mPrinterDiscoverySession;
    private final Intent mQueryIntent = new Intent("android.printservice.PrintService");
    private final RemotePrintSpooler mSpooler;
    private final SimpleStringSplitter mStringColonSplitter = new SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);
    private final int mUserId;

    private abstract class ListenerRecord<T extends IInterface> implements DeathRecipient {
        final T listener;

        public abstract void onBinderDied();

        public ListenerRecord(T listener) throws RemoteException {
            this.listener = listener;
            listener.asBinder().linkToDeath(this, 0);
        }

        public void destroy() {
            this.listener.asBinder().unlinkToDeath(this, 0);
        }

        public void binderDied() {
            this.listener.asBinder().unlinkToDeath(this, 0);
            onBinderDied();
        }
    }

    private final class PrintJobForAppCache {
        private final SparseArray<List<PrintJobInfo>> mPrintJobsForRunningApp;

        private PrintJobForAppCache() {
            this.mPrintJobsForRunningApp = new SparseArray();
        }

        /* synthetic */ PrintJobForAppCache(UserState x0, AnonymousClass1 x1) {
            this();
        }

        public boolean onPrintJobCreated(final IBinder creator, final int appId, PrintJobInfo printJob) {
            try {
                creator.linkToDeath(new DeathRecipient() {
                    public void binderDied() {
                        creator.unlinkToDeath(this, 0);
                        synchronized (UserState.this.mLock) {
                            PrintJobForAppCache.this.mPrintJobsForRunningApp.remove(appId);
                        }
                    }
                }, 0);
                synchronized (UserState.this.mLock) {
                    List<PrintJobInfo> printJobsForApp = (List) this.mPrintJobsForRunningApp.get(appId);
                    if (printJobsForApp == null) {
                        printJobsForApp = new ArrayList();
                        this.mPrintJobsForRunningApp.put(appId, printJobsForApp);
                    }
                    printJobsForApp.add(printJob);
                }
                return true;
            } catch (RemoteException e) {
                return false;
            }
        }

        public void onPrintJobStateChanged(PrintJobInfo printJob) {
            synchronized (UserState.this.mLock) {
                List<PrintJobInfo> printJobsForApp = (List) this.mPrintJobsForRunningApp.get(printJob.getAppId());
                if (printJobsForApp == null) {
                    return;
                }
                int printJobCount = printJobsForApp.size();
                for (int i = 0; i < printJobCount; i++) {
                    if (((PrintJobInfo) printJobsForApp.get(i)).getId().equals(printJob.getId())) {
                        printJobsForApp.set(i, printJob);
                    }
                }
            }
        }

        public PrintJobInfo getPrintJob(PrintJobId printJobId, int appId) {
            synchronized (UserState.this.mLock) {
                List<PrintJobInfo> printJobsForApp = (List) this.mPrintJobsForRunningApp.get(appId);
                if (printJobsForApp == null) {
                    return null;
                }
                int printJobCount = printJobsForApp.size();
                for (int i = 0; i < printJobCount; i++) {
                    PrintJobInfo printJob = (PrintJobInfo) printJobsForApp.get(i);
                    if (printJob.getId().equals(printJobId)) {
                        return printJob;
                    }
                }
                return null;
            }
        }

        public List<PrintJobInfo> getPrintJobs(int appId) {
            synchronized (UserState.this.mLock) {
                List<PrintJobInfo> bucket;
                List<PrintJobInfo> printJobs = null;
                if (appId == -2) {
                    int bucketCount = this.mPrintJobsForRunningApp.size();
                    for (int i = 0; i < bucketCount; i++) {
                        List<PrintJobInfo> bucket2 = (List) this.mPrintJobsForRunningApp.valueAt(i);
                        if (printJobs == null) {
                            printJobs = new ArrayList();
                        }
                        printJobs.addAll(bucket2);
                    }
                } else {
                    bucket = (List) this.mPrintJobsForRunningApp.get(appId);
                    if (bucket != null) {
                        if (null == null) {
                            printJobs = new ArrayList();
                        }
                        printJobs.addAll(bucket);
                    }
                }
                if (printJobs != null) {
                    return printJobs;
                }
                bucket = Collections.emptyList();
                return bucket;
            }
        }

        public void dumpLocked(DualDumpOutputStream dumpStream) {
            DualDumpOutputStream dualDumpOutputStream = dumpStream;
            int bucketCount = this.mPrintJobsForRunningApp.size();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 < bucketCount) {
                    int appId = this.mPrintJobsForRunningApp.keyAt(i2);
                    List<PrintJobInfo> bucket = (List) this.mPrintJobsForRunningApp.valueAt(i2);
                    int printJobCount = bucket.size();
                    i = 0;
                    while (true) {
                        int j = i;
                        if (j >= printJobCount) {
                            break;
                        }
                        long token = dualDumpOutputStream.start("cached_print_jobs", 2246267895813L);
                        dualDumpOutputStream.write("app_id", 1120986464257L, appId);
                        int i3 = i2;
                        long token2 = token;
                        DumpUtils.writePrintJobInfo(UserState.this.mContext, dualDumpOutputStream, "print_job", 1146756268034L, (PrintJobInfo) bucket.get(j));
                        dualDumpOutputStream.end(token2);
                        i = j + 1;
                        i2 = i3;
                    }
                    i = i2 + 1;
                } else {
                    return;
                }
            }
        }
    }

    private abstract class PrintJobStateChangeListenerRecord implements DeathRecipient {
        final int appId;
        final IPrintJobStateChangeListener listener;

        public abstract void onBinderDied();

        public PrintJobStateChangeListenerRecord(IPrintJobStateChangeListener listener, int appId) throws RemoteException {
            this.listener = listener;
            this.appId = appId;
            listener.asBinder().linkToDeath(this, 0);
        }

        public void destroy() {
            this.listener.asBinder().unlinkToDeath(this, 0);
        }

        public void binderDied() {
            this.listener.asBinder().unlinkToDeath(this, 0);
            onBinderDied();
        }
    }

    private class PrinterDiscoverySessionMediator {
        private final RemoteCallbackList<IPrinterDiscoveryObserver> mDiscoveryObservers = new RemoteCallbackList<IPrinterDiscoveryObserver>() {
            public void onCallbackDied(IPrinterDiscoveryObserver observer) {
                synchronized (UserState.this.mLock) {
                    PrinterDiscoverySessionMediator.this.stopPrinterDiscoveryLocked(observer);
                    PrinterDiscoverySessionMediator.this.removeObserverLocked(observer);
                }
            }
        };
        private boolean mIsDestroyed;
        private final ArrayMap<PrinterId, PrinterInfo> mPrinters = new ArrayMap();
        private final List<IBinder> mStartedPrinterDiscoveryTokens = new ArrayList();
        private final List<PrinterId> mStateTrackedPrinters = new ArrayList();

        PrinterDiscoverySessionMediator() {
            Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$PrinterDiscoverySessionMediator$Ou3LUs53hzSrIma0FHPj2g3gePc.INSTANCE, this, new ArrayList(UserState.this.mActiveServices.values())));
        }

        public void addObserverLocked(IPrinterDiscoveryObserver observer) {
            this.mDiscoveryObservers.register(observer);
            if (!this.mPrinters.isEmpty()) {
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$PrinterDiscoverySessionMediator$vhz2AcQkYu3SdMlMt9bsncMGW7E.INSTANCE, this, observer, new ArrayList(this.mPrinters.values())));
            }
        }

        public void removeObserverLocked(IPrinterDiscoveryObserver observer) {
            this.mDiscoveryObservers.unregister(observer);
            if (this.mDiscoveryObservers.getRegisteredCallbackCount() == 0) {
                destroyLocked();
            }
        }

        public final void startPrinterDiscoveryLocked(IPrinterDiscoveryObserver observer, List<PrinterId> priorityList) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not starting dicovery - session destroyed");
                return;
            }
            boolean discoveryStarted = this.mStartedPrinterDiscoveryTokens.isEmpty() ^ true;
            this.mStartedPrinterDiscoveryTokens.add(observer.asBinder());
            if (discoveryStarted && priorityList != null && !priorityList.isEmpty()) {
                UserState.this.validatePrinters(priorityList);
            } else if (this.mStartedPrinterDiscoveryTokens.size() <= 1) {
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$PrinterDiscoverySessionMediator$MT8AtQ4cegoEAucY7Fm8C8TCrjo.INSTANCE, this, new ArrayList(UserState.this.mActiveServices.values()), priorityList));
            }
        }

        public final void stopPrinterDiscoveryLocked(IPrinterDiscoveryObserver observer) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not stopping dicovery - session destroyed");
            } else if (this.mStartedPrinterDiscoveryTokens.remove(observer.asBinder()) && this.mStartedPrinterDiscoveryTokens.isEmpty()) {
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$PrinterDiscoverySessionMediator$TNeLGO1RKf0CucB-BMQ_M0UyoRs.INSTANCE, this, new ArrayList(UserState.this.mActiveServices.values())));
            }
        }

        public void validatePrintersLocked(List<PrinterId> printerIds) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not validating pritners - session destroyed");
                return;
            }
            List<PrinterId> remainingList = new ArrayList(printerIds);
            while (!remainingList.isEmpty()) {
                Iterator<PrinterId> iterator = remainingList.iterator();
                List<PrinterId> updateList = new ArrayList();
                ComponentName serviceName = null;
                while (iterator.hasNext()) {
                    PrinterId printerId = (PrinterId) iterator.next();
                    if (printerId != null) {
                        if (updateList.isEmpty()) {
                            updateList.add(printerId);
                            serviceName = printerId.getServiceName();
                            iterator.remove();
                        } else if (printerId.getServiceName().equals(serviceName)) {
                            updateList.add(printerId);
                            iterator.remove();
                        }
                    }
                }
                RemotePrintService service = (RemotePrintService) UserState.this.mActiveServices.get(serviceName);
                if (service != null) {
                    Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$PrinterDiscoverySessionMediator$Sqq0rjax7wbbY4ugrdxXopSyMNM.INSTANCE, this, service, updateList));
                }
            }
        }

        public final void startPrinterStateTrackingLocked(PrinterId printerId) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not starting printer state tracking - session destroyed");
            } else if (!this.mStartedPrinterDiscoveryTokens.isEmpty()) {
                boolean containedPrinterId = this.mStateTrackedPrinters.contains(printerId);
                this.mStateTrackedPrinters.add(printerId);
                if (!containedPrinterId) {
                    RemotePrintService service = (RemotePrintService) UserState.this.mActiveServices.get(printerId.getServiceName());
                    if (service != null) {
                        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$PrinterDiscoverySessionMediator$iQrjLK8luujjjp1uW3VGCsAZK_g.INSTANCE, this, service, printerId));
                    }
                }
            }
        }

        public final void stopPrinterStateTrackingLocked(PrinterId printerId) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not stopping printer state tracking - session destroyed");
            } else if (!this.mStartedPrinterDiscoveryTokens.isEmpty() && this.mStateTrackedPrinters.remove(printerId)) {
                RemotePrintService service = (RemotePrintService) UserState.this.mActiveServices.get(printerId.getServiceName());
                if (service != null) {
                    Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$PrinterDiscoverySessionMediator$_XymASnzhemmGwK4Nu5RUIT0ahk.INSTANCE, this, service, printerId));
                }
            }
        }

        public void onDestroyed() {
        }

        public void destroyLocked() {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not destroying - session destroyed");
                return;
            }
            int i;
            this.mIsDestroyed = true;
            int printerCount = this.mStateTrackedPrinters.size();
            int i2 = 0;
            for (i = 0; i < printerCount; i++) {
                UserState.this.stopPrinterStateTracking((PrinterId) this.mStateTrackedPrinters.get(i));
            }
            i = this.mStartedPrinterDiscoveryTokens.size();
            while (i2 < i) {
                stopPrinterDiscoveryLocked(Stub.asInterface((IBinder) this.mStartedPrinterDiscoveryTokens.get(i2)));
                i2++;
            }
            Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$PrinterDiscoverySessionMediator$TAWPnRTK22Veu2-mmKNSJCvnBoU.INSTANCE, this, new ArrayList(UserState.this.mActiveServices.values())));
        }

        public void onPrintersAddedLocked(List<PrinterInfo> printers) {
            Log.i(UserState.LOG_TAG, "onPrintersAddedLocked()");
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not adding printers - session destroyed");
                return;
            }
            List<PrinterInfo> addedPrinters = null;
            int addedPrinterCount = printers.size();
            for (int i = 0; i < addedPrinterCount; i++) {
                PrinterInfo printer = (PrinterInfo) printers.get(i);
                PrinterInfo oldPrinter = (PrinterInfo) this.mPrinters.put(printer.getId(), printer);
                if (oldPrinter == null || !oldPrinter.equals(printer)) {
                    if (addedPrinters == null) {
                        addedPrinters = new ArrayList();
                    }
                    addedPrinters.add(printer);
                }
            }
            if (addedPrinters != null) {
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$PrinterDiscoverySessionMediator$lfSsgTy_1NLRRkjOH_yL2Tk_x2w.INSTANCE, this, addedPrinters));
            }
        }

        public void onPrintersRemovedLocked(List<PrinterId> printerIds) {
            Log.i(UserState.LOG_TAG, "onPrintersRemovedLocked()");
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not removing printers - session destroyed");
                return;
            }
            List<PrinterId> removedPrinterIds = null;
            int removedPrinterCount = printerIds.size();
            for (int i = 0; i < removedPrinterCount; i++) {
                PrinterId removedPrinterId = (PrinterId) printerIds.get(i);
                if (this.mPrinters.remove(removedPrinterId) != null) {
                    if (removedPrinterIds == null) {
                        removedPrinterIds = new ArrayList();
                    }
                    removedPrinterIds.add(removedPrinterId);
                }
            }
            if (removedPrinterIds != null) {
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$PrinterDiscoverySessionMediator$CjemUQP8s7wG-dq-pIggj9Oze6I.INSTANCE, this, removedPrinterIds));
            }
        }

        public void onServiceRemovedLocked(RemotePrintService service) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not updating removed service - session destroyed");
                return;
            }
            removePrintersForServiceLocked(service.getComponentName());
            service.destroy();
        }

        public void onCustomPrinterIconLoadedLocked(PrinterId printerId) {
            Log.i(UserState.LOG_TAG, "onCustomPrinterIconLoadedLocked()");
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not updating printer - session destroyed");
                return;
            }
            PrinterInfo printer = (PrinterInfo) this.mPrinters.get(printerId);
            if (printer != null) {
                PrinterInfo newPrinter = new Builder(printer).incCustomPrinterIconGen().build();
                this.mPrinters.put(printerId, newPrinter);
                ArrayList<PrinterInfo> addedPrinters = new ArrayList(1);
                addedPrinters.add(newPrinter);
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$PrinterDiscoverySessionMediator$y51cj-jOuPNqkjzP4R89xJuclvo.INSTANCE, this, addedPrinters));
            }
        }

        public void onServiceDiedLocked(RemotePrintService service) {
            UserState.this.removeServiceLocked(service);
        }

        public void onServiceAddedLocked(RemotePrintService service) {
            if (this.mIsDestroyed) {
                Log.w(UserState.LOG_TAG, "Not updating added service - session destroyed");
                return;
            }
            Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$nSUd_Gl040MrfHGSQHSjunnnXaY.INSTANCE, service));
            if (!this.mStartedPrinterDiscoveryTokens.isEmpty()) {
                Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$gs6W8Li-g_ih6LLUIbTqHmyAoh0.INSTANCE, service, null));
            }
            int trackedPrinterCount = this.mStateTrackedPrinters.size();
            for (int i = 0; i < trackedPrinterCount; i++) {
                PrinterId printerId = (PrinterId) this.mStateTrackedPrinters.get(i);
                if (printerId.getServiceName().equals(service.getComponentName())) {
                    Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$qhnzLVwIUlj5cUdZ9YacT2IXyug.INSTANCE, service, printerId));
                }
            }
        }

        public void dumpLocked(DualDumpOutputStream dumpStream) {
            int i;
            int i2;
            int i3;
            dumpStream.write("is_destroyed", 1133871366145L, UserState.this.mDestroyed);
            dumpStream.write("is_printer_discovery_in_progress", 1133871366146L, this.mStartedPrinterDiscoveryTokens.isEmpty() ^ 1);
            int observerCount = this.mDiscoveryObservers.beginBroadcast();
            int i4 = 0;
            for (i = 0; i < observerCount; i++) {
                dumpStream.write("printer_discovery_observers", 2237677961219L, ((IPrinterDiscoveryObserver) this.mDiscoveryObservers.getBroadcastItem(i)).toString());
            }
            this.mDiscoveryObservers.finishBroadcast();
            i = this.mStartedPrinterDiscoveryTokens.size();
            for (i2 = 0; i2 < i; i2++) {
                dumpStream.write("discovery_requests", 2237677961220L, ((IBinder) this.mStartedPrinterDiscoveryTokens.get(i2)).toString());
            }
            i2 = this.mStateTrackedPrinters.size();
            for (i3 = 0; i3 < i2; i3++) {
                DumpUtils.writePrinterId(dumpStream, "tracked_printer_requests", 2246267895813L, (PrinterId) this.mStateTrackedPrinters.get(i3));
            }
            i3 = this.mPrinters.size();
            while (i4 < i3) {
                DualDumpOutputStream dualDumpOutputStream = dumpStream;
                DumpUtils.writePrinterInfo(UserState.this.mContext, dualDumpOutputStream, "printer", 2246267895814L, (PrinterInfo) this.mPrinters.valueAt(i4));
                i4++;
            }
        }

        private void removePrintersForServiceLocked(ComponentName serviceName) {
            if (!this.mPrinters.isEmpty()) {
                int i;
                int printerCount = this.mPrinters.size();
                int i2 = 0;
                List<PrinterId> removedPrinterIds = null;
                for (i = 0; i < printerCount; i++) {
                    PrinterId printerId = (PrinterId) this.mPrinters.keyAt(i);
                    if (printerId.getServiceName().equals(serviceName)) {
                        if (removedPrinterIds == null) {
                            removedPrinterIds = new ArrayList();
                        }
                        removedPrinterIds.add(printerId);
                    }
                }
                if (removedPrinterIds != null) {
                    i = removedPrinterIds.size();
                    while (i2 < i) {
                        this.mPrinters.remove(removedPrinterIds.get(i2));
                        i2++;
                    }
                    Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$PrinterDiscoverySessionMediator$CjemUQP8s7wG-dq-pIggj9Oze6I.INSTANCE, this, removedPrinterIds));
                }
            }
        }

        private void handleDispatchPrintersAdded(List<PrinterInfo> addedPrinters) {
            int observerCount = this.mDiscoveryObservers.beginBroadcast();
            for (int i = 0; i < observerCount; i++) {
                handlePrintersAdded((IPrinterDiscoveryObserver) this.mDiscoveryObservers.getBroadcastItem(i), addedPrinters);
            }
            this.mDiscoveryObservers.finishBroadcast();
        }

        private void handleDispatchPrintersRemoved(List<PrinterId> removedPrinterIds) {
            int observerCount = this.mDiscoveryObservers.beginBroadcast();
            for (int i = 0; i < observerCount; i++) {
                handlePrintersRemoved((IPrinterDiscoveryObserver) this.mDiscoveryObservers.getBroadcastItem(i), removedPrinterIds);
            }
            this.mDiscoveryObservers.finishBroadcast();
        }

        private void handleDispatchCreatePrinterDiscoverySession(List<RemotePrintService> services) {
            int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                ((RemotePrintService) services.get(i)).createPrinterDiscoverySession();
            }
        }

        private void handleDispatchDestroyPrinterDiscoverySession(List<RemotePrintService> services) {
            int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                ((RemotePrintService) services.get(i)).destroyPrinterDiscoverySession();
            }
            onDestroyed();
        }

        private void handleDispatchStartPrinterDiscovery(List<RemotePrintService> services, List<PrinterId> printerIds) {
            int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                ((RemotePrintService) services.get(i)).startPrinterDiscovery(printerIds);
            }
        }

        private void handleDispatchStopPrinterDiscovery(List<RemotePrintService> services) {
            int serviceCount = services.size();
            for (int i = 0; i < serviceCount; i++) {
                ((RemotePrintService) services.get(i)).stopPrinterDiscovery();
            }
        }

        private void handleValidatePrinters(RemotePrintService service, List<PrinterId> printerIds) {
            service.validatePrinters(printerIds);
        }

        private void handleStartPrinterStateTracking(RemotePrintService service, PrinterId printerId) {
            service.startPrinterStateTracking(printerId);
        }

        private void handleStopPrinterStateTracking(RemotePrintService service, PrinterId printerId) {
            service.stopPrinterStateTracking(printerId);
        }

        private void handlePrintersAdded(IPrinterDiscoveryObserver observer, List<PrinterInfo> printers) {
            try {
                observer.onPrintersAdded(new ParceledListSlice(printers));
            } catch (RemoteException re) {
                Log.e(UserState.LOG_TAG, "Error sending added printers", re);
            }
        }

        private void handlePrintersRemoved(IPrinterDiscoveryObserver observer, List<PrinterId> printerIds) {
            try {
                observer.onPrintersRemoved(new ParceledListSlice(printerIds));
            } catch (RemoteException re) {
                Log.e(UserState.LOG_TAG, "Error sending removed printers", re);
            }
        }
    }

    public UserState(Context context, int userId, Object lock, boolean lowPriority) {
        this.mContext = context;
        this.mUserId = userId;
        this.mLock = lock;
        this.mSpooler = new RemotePrintSpooler(context, userId, lowPriority, this);
        synchronized (this.mLock) {
            readInstalledPrintServicesLocked();
            upgradePersistentStateIfNeeded();
            readDisabledPrintServicesLocked();
        }
        prunePrintServices();
        onConfigurationChanged();
    }

    public void increasePriority() {
        this.mSpooler.increasePriority();
    }

    public void onPrintJobQueued(PrintJobInfo printJob) {
        RemotePrintService service;
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            service = (RemotePrintService) this.mActiveServices.get(printJob.getPrinterId().getServiceName());
        }
        if (service != null) {
            service.onPrintJobQueued(printJob);
        } else {
            this.mSpooler.setPrintJobState(printJob.getId(), 6, this.mContext.getString(17040967));
        }
    }

    public void onAllPrintJobsForServiceHandled(ComponentName printService) {
        RemotePrintService service;
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            service = (RemotePrintService) this.mActiveServices.get(printService);
        }
        if (service != null) {
            service.onAllPrintJobsHandled();
        }
    }

    public void removeObsoletePrintJobs() {
        this.mSpooler.removeObsoletePrintJobs();
    }

    public Bundle print(String printJobName, IPrintDocumentAdapter adapter, PrintAttributes attributes, String packageName, int appId) {
        Throwable th;
        int i = appId;
        PrintJobInfo printJob = new PrintJobInfo();
        printJob.setId(new PrintJobId());
        printJob.setAppId(i);
        printJob.setLabel(printJobName);
        printJob.setAttributes(attributes);
        printJob.setState(1);
        printJob.setCopies(1);
        printJob.setCreationTime(System.currentTimeMillis());
        if (!this.mPrintJobForAppCache.onPrintJobCreated(adapter.asBinder(), i, printJob)) {
            return null;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.print.PRINT_DIALOG");
            intent.setData(Uri.fromParts("printjob", printJob.getId().flattenToString(), null));
            intent.putExtra("android.print.intent.extra.EXTRA_PRINT_DOCUMENT_ADAPTER", adapter.asBinder());
            intent.putExtra("android.print.intent.extra.EXTRA_PRINT_JOB", printJob);
            try {
                intent.putExtra("android.content.extra.PACKAGE_NAME", packageName);
                IntentSender intentSender = PendingIntent.getActivityAsUser(this.mContext, 0, intent, 1409286144, null, new UserHandle(this.mUserId)).getIntentSender();
                Bundle result = new Bundle();
                result.putParcelable("android.print.intent.extra.EXTRA_PRINT_JOB", printJob);
                result.putParcelable("android.print.intent.extra.EXTRA_PRINT_DIALOG_INTENT", intentSender);
                Binder.restoreCallingIdentity(identity);
                return result;
            } catch (Throwable th2) {
                th = th2;
                Binder.restoreCallingIdentity(identity);
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            String str = packageName;
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public List<PrintJobInfo> getPrintJobInfos(int appId) {
        List<PrintJobInfo> cachedPrintJobs = this.mPrintJobForAppCache.getPrintJobs(appId);
        ArrayMap<PrintJobId, PrintJobInfo> result = new ArrayMap();
        int cachedPrintJobCount = cachedPrintJobs.size();
        int i = 0;
        for (int i2 = 0; i2 < cachedPrintJobCount; i2++) {
            PrintJobInfo cachedPrintJob = (PrintJobInfo) cachedPrintJobs.get(i2);
            result.put(cachedPrintJob.getId(), cachedPrintJob);
            cachedPrintJob.setTag(null);
            cachedPrintJob.setAdvancedOptions(null);
        }
        List<PrintJobInfo> printJobs = this.mSpooler.getPrintJobInfos(null, -1, appId);
        if (printJobs != null) {
            int printJobCount = printJobs.size();
            while (i < printJobCount) {
                PrintJobInfo printJob = (PrintJobInfo) printJobs.get(i);
                result.put(printJob.getId(), printJob);
                printJob.setTag(null);
                printJob.setAdvancedOptions(null);
                i++;
            }
        }
        return new ArrayList(result.values());
    }

    public PrintJobInfo getPrintJobInfo(PrintJobId printJobId, int appId) {
        PrintJobInfo printJob = this.mPrintJobForAppCache.getPrintJob(printJobId, appId);
        if (printJob == null) {
            printJob = this.mSpooler.getPrintJobInfo(printJobId, appId);
        }
        if (printJob != null) {
            printJob.setTag(null);
            printJob.setAdvancedOptions(null);
        }
        return printJob;
    }

    public Icon getCustomPrinterIcon(PrinterId printerId) {
        Icon icon = this.mSpooler.getCustomPrinterIcon(printerId);
        if (icon == null) {
            RemotePrintService service = (RemotePrintService) this.mActiveServices.get(printerId.getServiceName());
            if (service != null) {
                service.requestCustomPrinterIcon(printerId);
            }
        }
        return icon;
    }

    public void cancelPrintJob(PrintJobId printJobId, int appId) {
        PrintJobInfo printJobInfo = this.mSpooler.getPrintJobInfo(printJobId, appId);
        if (printJobInfo != null) {
            this.mSpooler.setPrintJobCancelling(printJobId, true);
            if (printJobInfo.getState() != 6) {
                PrinterId printerId = printJobInfo.getPrinterId();
                if (printerId != null) {
                    RemotePrintService printService;
                    ComponentName printServiceName = printerId.getServiceName();
                    synchronized (this.mLock) {
                        printService = (RemotePrintService) this.mActiveServices.get(printServiceName);
                    }
                    if (printService != null) {
                        printService.onRequestCancelPrintJob(printJobInfo);
                    } else {
                        return;
                    }
                }
            }
            this.mSpooler.setPrintJobState(printJobId, 7, null);
        }
    }

    public void restartPrintJob(PrintJobId printJobId, int appId) {
        PrintJobInfo printJobInfo = getPrintJobInfo(printJobId, appId);
        if (printJobInfo != null && printJobInfo.getState() == 6) {
            this.mSpooler.setPrintJobState(printJobId, 2, null);
        }
    }

    public List<PrintServiceInfo> getPrintServices(int selectionFlags) {
        List<PrintServiceInfo> selectedServices;
        synchronized (this.mLock) {
            selectedServices = null;
            int installedServiceCount = this.mInstalledServices.size();
            for (int i = 0; i < installedServiceCount; i++) {
                PrintServiceInfo installedService = (PrintServiceInfo) this.mInstalledServices.get(i);
                installedService.setIsEnabled(this.mActiveServices.containsKey(new ComponentName(installedService.getResolveInfo().serviceInfo.packageName, installedService.getResolveInfo().serviceInfo.name)));
                if (installedService.isEnabled()) {
                    if ((selectionFlags & 1) == 0) {
                    }
                } else if ((selectionFlags & 2) == 0) {
                }
                if (selectedServices == null) {
                    selectedServices = new ArrayList();
                }
                selectedServices.add(installedService);
            }
        }
        return selectedServices;
    }

    public void setPrintServiceEnabled(ComponentName serviceName, boolean isEnabled) {
        synchronized (this.mLock) {
            boolean isChanged = false;
            if (isEnabled) {
                isChanged = this.mDisabledServices.remove(serviceName);
            } else {
                int numServices = this.mInstalledServices.size();
                for (int i = 0; i < numServices; i++) {
                    if (((PrintServiceInfo) this.mInstalledServices.get(i)).getComponentName().equals(serviceName)) {
                        this.mDisabledServices.add(serviceName);
                        isChanged = true;
                        break;
                    }
                }
            }
            if (isChanged) {
                writeDisabledPrintServicesLocked(this.mDisabledServices);
                MetricsLogger.action(this.mContext, 511, isEnabled ^ 1);
                onConfigurationChangedLocked();
            }
        }
    }

    public List<RecommendationInfo> getPrintServiceRecommendations() {
        return this.mPrintServiceRecommendations;
    }

    public void createPrinterDiscoverySession(IPrinterDiscoveryObserver observer) {
        this.mSpooler.clearCustomPrinterIconCache();
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrinterDiscoverySession == null) {
                this.mPrinterDiscoverySession = new PrinterDiscoverySessionMediator() {
                    public void onDestroyed() {
                        UserState.this.mPrinterDiscoverySession = null;
                    }
                };
                this.mPrinterDiscoverySession.addObserverLocked(observer);
            } else {
                this.mPrinterDiscoverySession.addObserverLocked(observer);
            }
        }
    }

    public void destroyPrinterDiscoverySession(IPrinterDiscoveryObserver observer) {
        synchronized (this.mLock) {
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.removeObserverLocked(observer);
        }
    }

    public void startPrinterDiscovery(IPrinterDiscoveryObserver observer, List<PrinterId> printerIds) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.startPrinterDiscoveryLocked(observer, printerIds);
        }
    }

    public void stopPrinterDiscovery(IPrinterDiscoveryObserver observer) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.stopPrinterDiscoveryLocked(observer);
        }
    }

    public void validatePrinters(List<PrinterId> printerIds) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mActiveServices.isEmpty()) {
            } else if (this.mPrinterDiscoverySession == null) {
            } else {
                this.mPrinterDiscoverySession.validatePrintersLocked(printerIds);
            }
        }
    }

    public void startPrinterStateTracking(PrinterId printerId) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mActiveServices.isEmpty()) {
            } else if (this.mPrinterDiscoverySession == null) {
            } else {
                this.mPrinterDiscoverySession.startPrinterStateTrackingLocked(printerId);
            }
        }
    }

    public void stopPrinterStateTracking(PrinterId printerId) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mActiveServices.isEmpty()) {
            } else if (this.mPrinterDiscoverySession == null) {
            } else {
                this.mPrinterDiscoverySession.stopPrinterStateTrackingLocked(printerId);
            }
        }
    }

    public void addPrintJobStateChangeListener(IPrintJobStateChangeListener listener, int appId) throws RemoteException {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintJobStateChangeListenerRecords == null) {
                this.mPrintJobStateChangeListenerRecords = new ArrayList();
            }
            this.mPrintJobStateChangeListenerRecords.add(new PrintJobStateChangeListenerRecord(listener, appId) {
                public void onBinderDied() {
                    synchronized (UserState.this.mLock) {
                        if (UserState.this.mPrintJobStateChangeListenerRecords != null) {
                            UserState.this.mPrintJobStateChangeListenerRecords.remove(this);
                        }
                    }
                }
            });
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0045, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removePrintJobStateChangeListener(IPrintJobStateChangeListener listener) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintJobStateChangeListenerRecords == null) {
                return;
            }
            int recordCount = this.mPrintJobStateChangeListenerRecords.size();
            for (int i = 0; i < recordCount; i++) {
                PrintJobStateChangeListenerRecord record = (PrintJobStateChangeListenerRecord) this.mPrintJobStateChangeListenerRecords.get(i);
                if (record.listener.asBinder().equals(listener.asBinder())) {
                    record.destroy();
                    this.mPrintJobStateChangeListenerRecords.remove(i);
                    break;
                }
            }
            if (this.mPrintJobStateChangeListenerRecords.isEmpty()) {
                this.mPrintJobStateChangeListenerRecords = null;
            }
        }
    }

    public void addPrintServicesChangeListener(IPrintServicesChangeListener listener) throws RemoteException {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintServicesChangeListenerRecords == null) {
                this.mPrintServicesChangeListenerRecords = new ArrayList();
            }
            this.mPrintServicesChangeListenerRecords.add(new ListenerRecord<IPrintServicesChangeListener>(listener) {
                public void onBinderDied() {
                    synchronized (UserState.this.mLock) {
                        if (UserState.this.mPrintServicesChangeListenerRecords != null) {
                            UserState.this.mPrintServicesChangeListenerRecords.remove(this);
                        }
                    }
                }
            });
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0047, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removePrintServicesChangeListener(IPrintServicesChangeListener listener) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintServicesChangeListenerRecords == null) {
                return;
            }
            int recordCount = this.mPrintServicesChangeListenerRecords.size();
            for (int i = 0; i < recordCount; i++) {
                ListenerRecord<IPrintServicesChangeListener> record = (ListenerRecord) this.mPrintServicesChangeListenerRecords.get(i);
                if (((IPrintServicesChangeListener) record.listener).asBinder().equals(listener.asBinder())) {
                    record.destroy();
                    this.mPrintServicesChangeListenerRecords.remove(i);
                    break;
                }
            }
            if (this.mPrintServicesChangeListenerRecords.isEmpty()) {
                this.mPrintServicesChangeListenerRecords = null;
            }
        }
    }

    public void addPrintServiceRecommendationsChangeListener(IRecommendationsChangeListener listener) throws RemoteException {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintServiceRecommendationsChangeListenerRecords == null) {
                this.mPrintServiceRecommendationsChangeListenerRecords = new ArrayList();
                this.mPrintServiceRecommendationsService = new RemotePrintServiceRecommendationService(this.mContext, UserHandle.getUserHandleForUid(this.mUserId), this);
            }
            this.mPrintServiceRecommendationsChangeListenerRecords.add(new ListenerRecord<IRecommendationsChangeListener>(listener) {
                public void onBinderDied() {
                    synchronized (UserState.this.mLock) {
                        if (UserState.this.mPrintServiceRecommendationsChangeListenerRecords != null) {
                            UserState.this.mPrintServiceRecommendationsChangeListenerRecords.remove(this);
                        }
                    }
                }
            });
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0050, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void removePrintServiceRecommendationsChangeListener(IRecommendationsChangeListener listener) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrintServiceRecommendationsChangeListenerRecords == null) {
                return;
            }
            int recordCount = this.mPrintServiceRecommendationsChangeListenerRecords.size();
            for (int i = 0; i < recordCount; i++) {
                ListenerRecord<IRecommendationsChangeListener> record = (ListenerRecord) this.mPrintServiceRecommendationsChangeListenerRecords.get(i);
                if (((IRecommendationsChangeListener) record.listener).asBinder().equals(listener.asBinder())) {
                    record.destroy();
                    this.mPrintServiceRecommendationsChangeListenerRecords.remove(i);
                    break;
                }
            }
            if (this.mPrintServiceRecommendationsChangeListenerRecords.isEmpty()) {
                this.mPrintServiceRecommendationsChangeListenerRecords = null;
                this.mPrintServiceRecommendations = null;
                this.mPrintServiceRecommendationsService.close();
                this.mPrintServiceRecommendationsService = null;
            }
        }
    }

    public void onPrintJobStateChanged(PrintJobInfo printJob) {
        this.mPrintJobForAppCache.onPrintJobStateChanged(printJob);
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$d-WQxYwbHYb6N0le5ohwQsWVdjw.INSTANCE, this, printJob.getId(), PooledLambda.obtainSupplier(printJob.getAppId()).recycleOnUse()));
    }

    public void onPrintServicesChanged() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$LdWYUAKz4cbWqoxOD4oZ_ZslKdg.INSTANCE, this));
    }

    public void onPrintServiceRecommendationsUpdated(List<RecommendationInfo> recommendations) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$f3loorfBpq9Tu3Vl5vt4Ul321ok.INSTANCE, this, recommendations));
    }

    public void onPrintersAdded(List<PrinterInfo> printers) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mActiveServices.isEmpty()) {
            } else if (this.mPrinterDiscoverySession == null) {
            } else {
                this.mPrinterDiscoverySession.onPrintersAddedLocked(printers);
            }
        }
    }

    public void onPrintersRemoved(List<PrinterId> printerIds) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mActiveServices.isEmpty()) {
            } else if (this.mPrinterDiscoverySession == null) {
            } else {
                this.mPrinterDiscoverySession.onPrintersRemovedLocked(printerIds);
            }
        }
    }

    public void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon) {
        this.mSpooler.onCustomPrinterIconLoaded(printerId, icon);
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.onCustomPrinterIconLoadedLocked(printerId);
        }
    }

    public void onServiceDied(RemotePrintService service) {
        synchronized (this.mLock) {
            throwIfDestroyedLocked();
            if (this.mActiveServices.isEmpty()) {
                return;
            }
            failActivePrintJobsForService(service.getComponentName());
            service.onAllPrintJobsHandled();
            this.mActiveServices.remove(service.getComponentName());
            Handler.getMain().sendMessageDelayed(PooledLambda.obtainMessage(-$$Lambda$UserState$lM4y7oOfdlEk7JJ3u_zy-rL_-YI.INSTANCE, this), 500);
            if (this.mPrinterDiscoverySession == null) {
                return;
            }
            this.mPrinterDiscoverySession.onServiceDiedLocked(service);
        }
    }

    public void updateIfNeededLocked() {
        throwIfDestroyedLocked();
        readConfigurationLocked();
        onConfigurationChangedLocked();
    }

    public void destroyLocked() {
        throwIfDestroyedLocked();
        this.mSpooler.destroy();
        for (RemotePrintService service : this.mActiveServices.values()) {
            service.destroy();
        }
        this.mActiveServices.clear();
        this.mInstalledServices.clear();
        this.mDisabledServices.clear();
        if (this.mPrinterDiscoverySession != null) {
            this.mPrinterDiscoverySession.destroyLocked();
            this.mPrinterDiscoverySession = null;
        }
        this.mDestroyed = true;
    }

    public void dump(DualDumpOutputStream dumpStream) {
        synchronized (this.mLock) {
            int i;
            long token;
            dumpStream.write("user_id", 1120986464257L, this.mUserId);
            int installedServiceCount = this.mInstalledServices.size();
            int i2 = 0;
            for (i = 0; i < installedServiceCount; i++) {
                token = dumpStream.start("installed_services", 2246267895810L);
                PrintServiceInfo installedService = (PrintServiceInfo) this.mInstalledServices.get(i);
                ResolveInfo resolveInfo = installedService.getResolveInfo();
                com.android.internal.util.dump.DumpUtils.writeComponentName(dumpStream, "component_name", 1146756268033L, new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name));
                com.android.internal.util.dump.DumpUtils.writeStringIfNotNull(dumpStream, "settings_activity", 1138166333442L, installedService.getSettingsActivityName());
                com.android.internal.util.dump.DumpUtils.writeStringIfNotNull(dumpStream, "add_printers_activity", 1138166333443L, installedService.getAddPrintersActivityName());
                com.android.internal.util.dump.DumpUtils.writeStringIfNotNull(dumpStream, "advanced_options_activity", 1138166333444L, installedService.getAdvancedOptionsActivityName());
                dumpStream.end(token);
            }
            for (ComponentName disabledService : this.mDisabledServices) {
                com.android.internal.util.dump.DumpUtils.writeComponentName(dumpStream, "disabled_services", 2246267895811L, disabledService);
            }
            i = this.mActiveServices.size();
            while (i2 < i) {
                token = dumpStream.start("actives_services", 2246267895812L);
                ((RemotePrintService) this.mActiveServices.valueAt(i2)).dump(dumpStream);
                dumpStream.end(token);
                i2++;
            }
            this.mPrintJobForAppCache.dumpLocked(dumpStream);
            if (this.mPrinterDiscoverySession != null) {
                token = dumpStream.start("discovery_service", 2246267895814L);
                this.mPrinterDiscoverySession.dumpLocked(dumpStream);
                dumpStream.end(token);
            }
        }
        long token2 = dumpStream.start("print_spooler_state", 1146756268039L);
        this.mSpooler.dump(dumpStream);
        dumpStream.end(token2);
    }

    private void readConfigurationLocked() {
        readInstalledPrintServicesLocked();
        readDisabledPrintServicesLocked();
    }

    private void readInstalledPrintServicesLocked() {
        Set<PrintServiceInfo> tempPrintServices = new HashSet();
        int queryIntentFlags = 268435588;
        if (this.mIsInstantServiceAllowed) {
            queryIntentFlags = 268435588 | DumpState.DUMP_VOLUMES;
        }
        List<ResolveInfo> installedServices = this.mContext.getPackageManager().queryIntentServicesAsUser(this.mQueryIntent, queryIntentFlags, this.mUserId);
        int count = installedServices.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo installedService = (ResolveInfo) installedServices.get(i);
            if ("android.permission.BIND_PRINT_SERVICE".equals(installedService.serviceInfo.permission)) {
                tempPrintServices.add(PrintServiceInfo.create(this.mContext, installedService));
            } else {
                ComponentName serviceName = new ComponentName(installedService.serviceInfo.packageName, installedService.serviceInfo.name);
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Skipping print service ");
                stringBuilder.append(serviceName.flattenToShortString());
                stringBuilder.append(" since it does not require permission ");
                stringBuilder.append("android.permission.BIND_PRINT_SERVICE");
                Slog.w(str, stringBuilder.toString());
            }
        }
        this.mInstalledServices.clear();
        this.mInstalledServices.addAll(tempPrintServices);
    }

    private void upgradePersistentStateIfNeeded() {
        if (Secure.getStringForUser(this.mContext.getContentResolver(), "enabled_print_services", this.mUserId) != null) {
            Set<ComponentName> enabledServiceNameSet = new HashSet();
            readPrintServicesFromSettingLocked("enabled_print_services", enabledServiceNameSet);
            ArraySet<ComponentName> disabledServices = new ArraySet();
            int numInstalledServices = this.mInstalledServices.size();
            for (int i = 0; i < numInstalledServices; i++) {
                ComponentName serviceName = ((PrintServiceInfo) this.mInstalledServices.get(i)).getComponentName();
                if (!enabledServiceNameSet.contains(serviceName)) {
                    disabledServices.add(serviceName);
                }
            }
            writeDisabledPrintServicesLocked(disabledServices);
            Secure.putStringForUser(this.mContext.getContentResolver(), "enabled_print_services", null, this.mUserId);
        }
    }

    private void readDisabledPrintServicesLocked() {
        Set<ComponentName> tempDisabledServiceNameSet = new HashSet();
        readPrintServicesFromSettingLocked("disabled_print_services", tempDisabledServiceNameSet);
        if (!tempDisabledServiceNameSet.equals(this.mDisabledServices)) {
            this.mDisabledServices.clear();
            this.mDisabledServices.addAll(tempDisabledServiceNameSet);
        }
    }

    private void readPrintServicesFromSettingLocked(String setting, Set<ComponentName> outServiceNames) {
        String settingValue = Secure.getStringForUser(this.mContext.getContentResolver(), setting, this.mUserId);
        if (!TextUtils.isEmpty(settingValue)) {
            SimpleStringSplitter splitter = this.mStringColonSplitter;
            splitter.setString(settingValue);
            while (splitter.hasNext()) {
                String string = splitter.next();
                if (!TextUtils.isEmpty(string)) {
                    ComponentName componentName = ComponentName.unflattenFromString(string);
                    if (componentName != null) {
                        outServiceNames.add(componentName);
                    }
                }
            }
        }
    }

    private void writeDisabledPrintServicesLocked(Set<ComponentName> disabledServices) {
        StringBuilder builder = new StringBuilder();
        for (ComponentName componentName : disabledServices) {
            if (builder.length() > 0) {
                builder.append(COMPONENT_NAME_SEPARATOR);
            }
            builder.append(componentName.flattenToShortString());
        }
        Secure.putStringForUser(this.mContext.getContentResolver(), "disabled_print_services", builder.toString(), this.mUserId);
    }

    private ArrayList<ComponentName> getInstalledComponents() {
        ArrayList<ComponentName> installedComponents = new ArrayList();
        int installedCount = this.mInstalledServices.size();
        for (int i = 0; i < installedCount; i++) {
            ResolveInfo resolveInfo = ((PrintServiceInfo) this.mInstalledServices.get(i)).getResolveInfo();
            installedComponents.add(new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name));
        }
        return installedComponents;
    }

    public void prunePrintServices() {
        ArrayList<ComponentName> installedComponents;
        this.mSpooler.getRemoteInstanceLazyFirstly();
        synchronized (this.mLock) {
            installedComponents = getInstalledComponents();
            if (this.mDisabledServices.retainAll(installedComponents)) {
                writeDisabledPrintServicesLocked(this.mDisabledServices);
            }
            if (Secure.getStringForUser(this.mContext.getContentResolver(), "disabled_print_services", this.mUserId) == null && isChineseVersion() && !installedComponents.isEmpty()) {
                ComponentName name = (ComponentName) installedComponents.get(0);
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("name :");
                stringBuilder.append(name);
                Log.i(str, stringBuilder.toString());
                if ("com.android.bips.BuiltInPrintService".equals(name.getClassName())) {
                    this.mDisabledServices.add(name);
                    writeDisabledPrintServicesLocked(this.mDisabledServices);
                }
            }
        }
        this.mSpooler.pruneApprovedPrintServices(installedComponents);
    }

    private boolean isChineseVersion() {
        return "zh".equals(SystemProperties.get("ro.product.locale.language")) && "CN".equals(SystemProperties.get("ro.product.locale.region"));
    }

    private void onConfigurationChangedLocked() {
        ArrayList<ComponentName> installedComponents = getInstalledComponents();
        int installedCount = installedComponents.size();
        for (int i = 0; i < installedCount; i++) {
            ComponentName serviceName = (ComponentName) installedComponents.get(i);
            if (this.mDisabledServices.contains(serviceName)) {
                RemotePrintService service = (RemotePrintService) this.mActiveServices.remove(serviceName);
                if (service != null) {
                    removeServiceLocked(service);
                }
            } else if (!this.mActiveServices.containsKey(serviceName)) {
                addServiceLocked(new RemotePrintService(this.mContext, serviceName, this.mUserId, this.mSpooler, this));
            }
        }
        Iterator<Entry<ComponentName, RemotePrintService>> iterator = this.mActiveServices.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<ComponentName, RemotePrintService> entry = (Entry) iterator.next();
            RemotePrintService service2 = (RemotePrintService) entry.getValue();
            if (!installedComponents.contains((ComponentName) entry.getKey())) {
                removeServiceLocked(service2);
                iterator.remove();
            }
        }
        onPrintServicesChanged();
    }

    private void addServiceLocked(RemotePrintService service) {
        this.mActiveServices.put(service.getComponentName(), service);
        if (this.mPrinterDiscoverySession != null) {
            this.mPrinterDiscoverySession.onServiceAddedLocked(service);
        }
    }

    private void removeServiceLocked(RemotePrintService service) {
        failActivePrintJobsForService(service.getComponentName());
        if (this.mPrinterDiscoverySession != null) {
            this.mPrinterDiscoverySession.onServiceRemovedLocked(service);
        } else {
            service.destroy();
        }
    }

    private void failActivePrintJobsForService(ComponentName serviceName) {
        if (Looper.getMainLooper().isCurrentThread()) {
            BackgroundThread.getHandler().sendMessage(PooledLambda.obtainMessage(-$$Lambda$UserState$HoM_sy_T_4RiQGYcbixewHZ2IMA.INSTANCE, this, serviceName));
        } else {
            failScheduledPrintJobsForServiceInternal(serviceName);
        }
    }

    private void failScheduledPrintJobsForServiceInternal(ComponentName serviceName) {
        List<PrintJobInfo> printJobs = this.mSpooler.getPrintJobInfos(serviceName, -4, -2);
        if (printJobs != null) {
            long identity = Binder.clearCallingIdentity();
            try {
                int printJobCount = printJobs.size();
                for (int i = 0; i < printJobCount; i++) {
                    this.mSpooler.setPrintJobState(((PrintJobInfo) printJobs.get(i)).getId(), 6, this.mContext.getString(17040967));
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }
    }

    private void throwIfDestroyedLocked() {
        if (this.mDestroyed) {
            throw new IllegalStateException("Cannot interact with a destroyed instance.");
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0015, code:
            r1 = r2.size();
            r3 = 0;
     */
    /* JADX WARNING: Missing block: B:10:0x001a, code:
            if (r3 >= r1) goto L_0x003c;
     */
    /* JADX WARNING: Missing block: B:11:0x001c, code:
            r4 = (com.android.server.print.UserState.PrintJobStateChangeListenerRecord) r2.get(r3);
     */
    /* JADX WARNING: Missing block: B:12:0x0025, code:
            if (r4.appId == -2) goto L_0x002b;
     */
    /* JADX WARNING: Missing block: B:14:0x0029, code:
            if (r4.appId != r0) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:16:?, code:
            r4.listener.onPrintJobStateChanged(r9);
     */
    /* JADX WARNING: Missing block: B:17:0x0031, code:
            r5 = move-exception;
     */
    /* JADX WARNING: Missing block: B:18:0x0032, code:
            android.util.Log.e(LOG_TAG, "Error notifying for print job state change", r5);
     */
    /* JADX WARNING: Missing block: B:20:0x003c, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleDispatchPrintJobStateChanged(PrintJobId printJobId, IntSupplier appIdSupplier) {
        int appId = appIdSupplier.getAsInt();
        synchronized (this.mLock) {
            if (this.mPrintJobStateChangeListenerRecords == null) {
                return;
            }
            List<PrintJobStateChangeListenerRecord> records = new ArrayList(this.mPrintJobStateChangeListenerRecords);
        }
        int i++;
    }

    /* JADX WARNING: Missing block: B:9:0x0011, code:
            r0 = r1.size();
            r2 = 0;
     */
    /* JADX WARNING: Missing block: B:10:0x0016, code:
            if (r2 >= r0) goto L_0x0031;
     */
    /* JADX WARNING: Missing block: B:13:?, code:
            ((android.print.IPrintServicesChangeListener) ((com.android.server.print.UserState.ListenerRecord) r1.get(r2)).listener).onPrintServicesChanged();
     */
    /* JADX WARNING: Missing block: B:14:0x0026, code:
            r4 = move-exception;
     */
    /* JADX WARNING: Missing block: B:15:0x0027, code:
            android.util.Log.e(LOG_TAG, "Error notifying for print services change", r4);
     */
    /* JADX WARNING: Missing block: B:17:0x0031, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleDispatchPrintServicesChanged() {
        synchronized (this.mLock) {
            if (this.mPrintServicesChangeListenerRecords == null) {
                return;
            }
            List<ListenerRecord<IPrintServicesChangeListener>> records = new ArrayList(this.mPrintServicesChangeListenerRecords);
        }
        int i++;
    }

    /* JADX WARNING: Missing block: B:9:0x0013, code:
            r0 = r1.size();
            r2 = 0;
     */
    /* JADX WARNING: Missing block: B:10:0x0018, code:
            if (r2 >= r0) goto L_0x0033;
     */
    /* JADX WARNING: Missing block: B:13:?, code:
            ((android.printservice.recommendation.IRecommendationsChangeListener) ((com.android.server.print.UserState.ListenerRecord) r1.get(r2)).listener).onRecommendationsChanged();
     */
    /* JADX WARNING: Missing block: B:14:0x0028, code:
            r4 = move-exception;
     */
    /* JADX WARNING: Missing block: B:15:0x0029, code:
            android.util.Log.e(LOG_TAG, "Error notifying for print service recommendations change", r4);
     */
    /* JADX WARNING: Missing block: B:17:0x0033, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleDispatchPrintServiceRecommendationsUpdated(List<RecommendationInfo> recommendations) {
        synchronized (this.mLock) {
            if (this.mPrintServiceRecommendationsChangeListenerRecords == null) {
                return;
            } else {
                List<ListenerRecord<IRecommendationsChangeListener>> records = new ArrayList(this.mPrintServiceRecommendationsChangeListenerRecords);
                this.mPrintServiceRecommendations = recommendations;
            }
        }
        int i++;
    }

    private void onConfigurationChanged() {
        synchronized (this.mLock) {
            onConfigurationChangedLocked();
        }
    }

    public boolean getBindInstantServiceAllowed() {
        return this.mIsInstantServiceAllowed;
    }

    public void setBindInstantServiceAllowed(boolean allowed) {
        synchronized (this.mLock) {
            this.mIsInstantServiceAllowed = allowed;
            updateIfNeededLocked();
        }
    }
}
