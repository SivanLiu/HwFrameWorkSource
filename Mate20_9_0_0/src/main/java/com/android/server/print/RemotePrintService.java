package com.android.server.print;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ParceledListSlice;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.UserHandle;
import android.print.PrintJobId;
import android.print.PrintJobInfo;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.printservice.IPrintService;
import android.printservice.IPrintServiceClient.Stub;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.dump.DumpUtils;
import com.android.internal.util.function.pooled.PooledLambda;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

final class RemotePrintService implements DeathRecipient {
    private static final boolean DEBUG = true;
    private static final String LOG_TAG = "RemotePrintService";
    private boolean mBinding;
    private final PrintServiceCallbacks mCallbacks;
    private final ComponentName mComponentName;
    private final Context mContext;
    private boolean mDestroyed;
    private List<PrinterId> mDiscoveryPriorityList;
    private boolean mHasActivePrintJobs;
    private boolean mHasPrinterDiscoverySession;
    private final Intent mIntent;
    private final Object mLock = new Object();
    private final List<Runnable> mPendingCommands = new ArrayList();
    private IPrintService mPrintService;
    private final RemotePrintServiceClient mPrintServiceClient;
    private final ServiceConnection mServiceConnection = new RemoteServiceConneciton(this, null);
    private boolean mServiceDied;
    private final RemotePrintSpooler mSpooler;
    @GuardedBy("mLock")
    private List<PrinterId> mTrackedPrinterList;
    private final int mUserId;

    public interface PrintServiceCallbacks {
        void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon);

        void onPrintersAdded(List<PrinterInfo> list);

        void onPrintersRemoved(List<PrinterId> list);

        void onServiceDied(RemotePrintService remotePrintService);
    }

    private static final class RemotePrintServiceClient extends Stub {
        private final WeakReference<RemotePrintService> mWeakService;

        public RemotePrintServiceClient(RemotePrintService service) {
            this.mWeakService = new WeakReference(service);
        }

        public List<PrintJobInfo> getPrintJobInfos() {
            RemotePrintService service = (RemotePrintService) this.mWeakService.get();
            if (service == null) {
                return null;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                List<PrintJobInfo> printJobInfos = service.mSpooler.getPrintJobInfos(service.mComponentName, -4, -2);
                return printJobInfos;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public PrintJobInfo getPrintJobInfo(PrintJobId printJobId) {
            RemotePrintService service = (RemotePrintService) this.mWeakService.get();
            if (service == null) {
                return null;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                PrintJobInfo printJobInfo = service.mSpooler.getPrintJobInfo(printJobId, -2);
                return printJobInfo;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean setPrintJobState(PrintJobId printJobId, int state, String error) {
            RemotePrintService service = (RemotePrintService) this.mWeakService.get();
            if (service == null) {
                return false;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                boolean printJobState = service.mSpooler.setPrintJobState(printJobId, state, error);
                return printJobState;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public boolean setPrintJobTag(PrintJobId printJobId, String tag) {
            RemotePrintService service = (RemotePrintService) this.mWeakService.get();
            if (service == null) {
                return false;
            }
            long identity = Binder.clearCallingIdentity();
            try {
                boolean printJobTag = service.mSpooler.setPrintJobTag(printJobId, tag);
                return printJobTag;
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        public void writePrintJobData(ParcelFileDescriptor fd, PrintJobId printJobId) {
            RemotePrintService service = (RemotePrintService) this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mSpooler.writePrintJobData(fd, printJobId);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void setProgress(PrintJobId printJobId, float progress) {
            RemotePrintService service = (RemotePrintService) this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mSpooler.setProgress(printJobId, progress);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void setStatus(PrintJobId printJobId, CharSequence status) {
            RemotePrintService service = (RemotePrintService) this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mSpooler.setStatus(printJobId, status);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void setStatusRes(PrintJobId printJobId, int status, CharSequence appPackageName) {
            RemotePrintService service = (RemotePrintService) this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mSpooler.setStatus(printJobId, status, appPackageName);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onPrintersAdded(ParceledListSlice printers) {
            RemotePrintService service = (RemotePrintService) this.mWeakService.get();
            if (service != null) {
                List<PrinterInfo> addedPrinters = printers.getList();
                throwIfPrinterIdsForPrinterInfoTampered(service.mComponentName, addedPrinters);
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mCallbacks.onPrintersAdded(addedPrinters);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        public void onPrintersRemoved(ParceledListSlice printerIds) {
            RemotePrintService service = (RemotePrintService) this.mWeakService.get();
            if (service != null) {
                List<PrinterId> removedPrinterIds = printerIds.getList();
                throwIfPrinterIdsTampered(service.mComponentName, removedPrinterIds);
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mCallbacks.onPrintersRemoved(removedPrinterIds);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }

        private void throwIfPrinterIdsForPrinterInfoTampered(ComponentName serviceName, List<PrinterInfo> printerInfos) {
            int printerInfoCount = printerInfos.size();
            for (int i = 0; i < printerInfoCount; i++) {
                throwIfPrinterIdTampered(serviceName, ((PrinterInfo) printerInfos.get(i)).getId());
            }
        }

        private void throwIfPrinterIdsTampered(ComponentName serviceName, List<PrinterId> printerIds) {
            int printerIdCount = printerIds.size();
            for (int i = 0; i < printerIdCount; i++) {
                throwIfPrinterIdTampered(serviceName, (PrinterId) printerIds.get(i));
            }
        }

        private void throwIfPrinterIdTampered(ComponentName serviceName, PrinterId printerId) {
            if (printerId == null || !printerId.getServiceName().equals(serviceName)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid printer id: ");
                stringBuilder.append(printerId);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public void onCustomPrinterIconLoaded(PrinterId printerId, Icon icon) throws RemoteException {
            RemotePrintService service = (RemotePrintService) this.mWeakService.get();
            if (service != null) {
                long identity = Binder.clearCallingIdentity();
                try {
                    service.mCallbacks.onCustomPrinterIconLoaded(printerId, icon);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
            }
        }
    }

    private class RemoteServiceConneciton implements ServiceConnection {
        private RemoteServiceConneciton() {
        }

        /* synthetic */ RemoteServiceConneciton(RemotePrintService x0, AnonymousClass1 x1) {
            this();
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            if (RemotePrintService.this.mDestroyed || !RemotePrintService.this.mBinding) {
                RemotePrintService.this.mContext.unbindService(RemotePrintService.this.mServiceConnection);
                return;
            }
            RemotePrintService.this.mBinding = false;
            RemotePrintService.this.mPrintService = IPrintService.Stub.asInterface(service);
            try {
                service.linkToDeath(RemotePrintService.this, 0);
                try {
                    RemotePrintService.this.mPrintService.setClient(RemotePrintService.this.mPrintServiceClient);
                    if (RemotePrintService.this.mServiceDied && RemotePrintService.this.mHasPrinterDiscoverySession) {
                        RemotePrintService.this.handleCreatePrinterDiscoverySession();
                    }
                    if (RemotePrintService.this.mServiceDied && RemotePrintService.this.mDiscoveryPriorityList != null) {
                        RemotePrintService.this.handleStartPrinterDiscovery(RemotePrintService.this.mDiscoveryPriorityList);
                    }
                    synchronized (RemotePrintService.this.mLock) {
                        if (RemotePrintService.this.mServiceDied && RemotePrintService.this.mTrackedPrinterList != null) {
                            int trackedPrinterCount = RemotePrintService.this.mTrackedPrinterList.size();
                            for (int i = 0; i < trackedPrinterCount; i++) {
                                RemotePrintService.this.handleStartPrinterStateTracking((PrinterId) RemotePrintService.this.mTrackedPrinterList.get(i));
                            }
                        }
                    }
                    while (!RemotePrintService.this.mPendingCommands.isEmpty()) {
                        ((Runnable) RemotePrintService.this.mPendingCommands.remove(0)).run();
                    }
                    if (!(RemotePrintService.this.mHasPrinterDiscoverySession || RemotePrintService.this.mHasActivePrintJobs)) {
                        RemotePrintService.this.ensureUnbound();
                    }
                    RemotePrintService.this.mServiceDied = false;
                } catch (RemoteException re) {
                    String str = RemotePrintService.LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error setting client for: ");
                    stringBuilder.append(service);
                    Slog.e(str, stringBuilder.toString(), re);
                    RemotePrintService.this.handleBinderDied();
                }
            } catch (RemoteException e) {
                RemotePrintService.this.handleBinderDied();
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            RemotePrintService.this.mBinding = true;
        }
    }

    public RemotePrintService(Context context, ComponentName componentName, int userId, RemotePrintSpooler spooler, PrintServiceCallbacks callbacks) {
        this.mContext = context;
        this.mCallbacks = callbacks;
        this.mComponentName = componentName;
        this.mIntent = new Intent().setComponent(this.mComponentName);
        this.mUserId = userId;
        this.mSpooler = spooler;
        this.mPrintServiceClient = new RemotePrintServiceClient(this);
    }

    public ComponentName getComponentName() {
        return this.mComponentName;
    }

    public void destroy() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$tI07K2u4Z5L72sd1hvSEunGclrg.INSTANCE, this));
    }

    private void handleDestroy() {
        stopTrackingAllPrinters();
        if (this.mDiscoveryPriorityList != null) {
            handleStopPrinterDiscovery();
        }
        if (this.mHasPrinterDiscoverySession) {
            handleDestroyPrinterDiscoverySession();
        }
        ensureUnbound();
        this.mDestroyed = true;
    }

    public void binderDied() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$uBWTskFvpksxzoYevxmiaqdMXas.INSTANCE, this));
    }

    private void handleBinderDied() {
        if (this.mPrintService != null) {
            this.mPrintService.asBinder().unlinkToDeath(this, 0);
        }
        this.mPrintService = null;
        this.mServiceDied = true;
        this.mCallbacks.onServiceDied(this);
    }

    public void onAllPrintJobsHandled() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$1cbVOJkW_ULFS1xH-T-tbALCzHI.INSTANCE, this));
    }

    private void handleOnAllPrintJobsHandled() {
        this.mHasActivePrintJobs = false;
        if (isBound()) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("] onAllPrintJobsHandled()");
            Slog.i(str, stringBuilder.toString());
            if (!this.mHasPrinterDiscoverySession) {
                ensureUnbound();
            }
        } else if (!this.mServiceDied || this.mHasPrinterDiscoverySession) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() {
                public void run() {
                    RemotePrintService.this.handleOnAllPrintJobsHandled();
                }
            });
        } else {
            ensureUnbound();
        }
    }

    public void onRequestCancelPrintJob(PrintJobInfo printJob) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$tL9wtChZzY3dei-ul1VudkrPO20.INSTANCE, this, printJob));
    }

    private void handleRequestCancelPrintJob(final PrintJobInfo printJob) {
        if (isBound()) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("] requestCancelPrintJob()");
            Slog.i(str, stringBuilder.toString());
            try {
                this.mPrintService.requestCancelPrintJob(printJob);
                return;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error canceling a pring job.", re);
                return;
            }
        }
        ensureBound();
        this.mPendingCommands.add(new Runnable() {
            public void run() {
                RemotePrintService.this.handleRequestCancelPrintJob(printJob);
            }
        });
    }

    public void onPrintJobQueued(PrintJobInfo printJob) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$KGsYx3sHW6vGymod4UmBTazYSks.INSTANCE, this, printJob));
    }

    private void handleOnPrintJobQueued(final PrintJobInfo printJob) {
        this.mHasActivePrintJobs = true;
        if (isBound()) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("] onPrintJobQueued()");
            Slog.i(str, stringBuilder.toString());
            try {
                this.mPrintService.onPrintJobQueued(printJob);
                return;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error announcing queued pring job.", re);
                return;
            }
        }
        ensureBound();
        this.mPendingCommands.add(new Runnable() {
            public void run() {
                RemotePrintService.this.handleOnPrintJobQueued(printJob);
            }
        });
    }

    public void createPrinterDiscoverySession() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$pgSurbN2geCgHp9vfTAIFm5XvgQ.INSTANCE, this));
    }

    private void handleCreatePrinterDiscoverySession() {
        this.mHasPrinterDiscoverySession = true;
        if (isBound()) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("] createPrinterDiscoverySession()");
            Slog.i(str, stringBuilder.toString());
            try {
                this.mPrintService.createPrinterDiscoverySession();
                return;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error creating printer discovery session.", re);
                return;
            }
        }
        ensureBound();
        this.mPendingCommands.add(new Runnable() {
            public void run() {
                RemotePrintService.this.handleCreatePrinterDiscoverySession();
            }
        });
    }

    public void destroyPrinterDiscoverySession() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$ru7USNI_O2DIDwflMPlEsqA_IY4.INSTANCE, this));
    }

    private void handleDestroyPrinterDiscoverySession() {
        this.mHasPrinterDiscoverySession = false;
        if (isBound()) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("] destroyPrinterDiscoverySession()");
            Slog.i(str, stringBuilder.toString());
            try {
                this.mPrintService.destroyPrinterDiscoverySession();
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error destroying printer dicovery session.", re);
            }
            if (!this.mHasActivePrintJobs) {
                ensureUnbound();
            }
        } else if (!this.mServiceDied || this.mHasActivePrintJobs) {
            ensureBound();
            this.mPendingCommands.add(new Runnable() {
                public void run() {
                    RemotePrintService.this.handleDestroyPrinterDiscoverySession();
                }
            });
        } else {
            ensureUnbound();
        }
    }

    public void startPrinterDiscovery(List<PrinterId> priorityList) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$jrFOjxtIoMNm8S0KNTqIDHuv4oY.INSTANCE, this, priorityList));
    }

    private void handleStartPrinterDiscovery(final List<PrinterId> priorityList) {
        this.mDiscoveryPriorityList = new ArrayList();
        if (priorityList != null) {
            this.mDiscoveryPriorityList.addAll(priorityList);
        }
        if (isBound()) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("] startPrinterDiscovery()");
            Slog.i(str, stringBuilder.toString());
            try {
                this.mPrintService.startPrinterDiscovery(priorityList);
                return;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error starting printer dicovery.", re);
                return;
            }
        }
        ensureBound();
        this.mPendingCommands.add(new Runnable() {
            public void run() {
                RemotePrintService.this.handleStartPrinterDiscovery(priorityList);
            }
        });
    }

    public void stopPrinterDiscovery() {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$FH95Crnc6zH421SxRw9RxPyl0YY.INSTANCE, this));
    }

    private void handleStopPrinterDiscovery() {
        this.mDiscoveryPriorityList = null;
        if (isBound()) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("] stopPrinterDiscovery()");
            Slog.i(str, stringBuilder.toString());
            stopTrackingAllPrinters();
            try {
                this.mPrintService.stopPrinterDiscovery();
                return;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error stopping printer discovery.", re);
                return;
            }
        }
        ensureBound();
        this.mPendingCommands.add(new Runnable() {
            public void run() {
                RemotePrintService.this.handleStopPrinterDiscovery();
            }
        });
    }

    public void validatePrinters(List<PrinterId> printerIds) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$q0Rw93bA7P79FpkLlFZXs5xcOoc.INSTANCE, this, printerIds));
    }

    private void handleValidatePrinters(final List<PrinterId> printerIds) {
        if (isBound()) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("] validatePrinters()");
            Slog.i(str, stringBuilder.toString());
            try {
                this.mPrintService.validatePrinters(printerIds);
                return;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error requesting printers validation.", re);
                return;
            }
        }
        ensureBound();
        this.mPendingCommands.add(new Runnable() {
            public void run() {
                RemotePrintService.this.handleValidatePrinters(printerIds);
            }
        });
    }

    public void startPrinterStateTracking(PrinterId printerId) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$aHc-cJYzTXxafcxxvfW2janFHIc.INSTANCE, this, printerId));
    }

    public void requestCustomPrinterIcon(PrinterId printerId) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$TsHHZCuIB3sKEZ8IZ0oPokZZO6g.INSTANCE, this, printerId));
    }

    private void handleRequestCustomPrinterIcon(PrinterId printerId) {
        if (isBound()) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("] requestCustomPrinterIcon()");
            Slog.i(str, stringBuilder.toString());
            try {
                this.mPrintService.requestCustomPrinterIcon(printerId);
                return;
            } catch (RemoteException re) {
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Error requesting icon for ");
                stringBuilder2.append(printerId);
                Slog.e(str2, stringBuilder2.toString(), re);
                return;
            }
        }
        ensureBound();
        this.mPendingCommands.add(new -$$Lambda$RemotePrintService$e0Ck2QZDih6p896nITpWZ_zOduk(this, printerId));
    }

    private void handleStartPrinterStateTracking(final PrinterId printerId) {
        synchronized (this.mLock) {
            if (this.mTrackedPrinterList == null) {
                this.mTrackedPrinterList = new ArrayList();
            }
            this.mTrackedPrinterList.add(printerId);
        }
        if (isBound()) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("] startPrinterTracking()");
            Slog.i(str, stringBuilder.toString());
            try {
                this.mPrintService.startPrinterStateTracking(printerId);
                return;
            } catch (RemoteException re) {
                Slog.e(LOG_TAG, "Error requesting start printer tracking.", re);
                return;
            }
        }
        ensureBound();
        this.mPendingCommands.add(new Runnable() {
            public void run() {
                RemotePrintService.this.handleStartPrinterStateTracking(printerId);
            }
        });
    }

    public void stopPrinterStateTracking(PrinterId printerId) {
        Handler.getMain().sendMessage(PooledLambda.obtainMessage(-$$Lambda$RemotePrintService$L2EQSyIHled1ZVO5GCaBXmvtCQQ.INSTANCE, this, printerId));
    }

    /* JADX WARNING: Missing block: B:12:0x0020, code:
            if (isBound() != false) goto L_0x0030;
     */
    /* JADX WARNING: Missing block: B:13:0x0022, code:
            ensureBound();
            r3.mPendingCommands.add(new com.android.server.print.RemotePrintService.AnonymousClass10(r3));
     */
    /* JADX WARNING: Missing block: B:14:0x0030, code:
            r0 = LOG_TAG;
            r1 = new java.lang.StringBuilder();
            r1.append("[user: ");
            r1.append(r3.mUserId);
            r1.append("] stopPrinterTracking()");
            android.util.Slog.i(r0, r1.toString());
     */
    /* JADX WARNING: Missing block: B:16:?, code:
            r3.mPrintService.stopPrinterStateTracking(r4);
     */
    /* JADX WARNING: Missing block: B:17:0x0053, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:18:0x0054, code:
            android.util.Slog.e(LOG_TAG, "Error requesting stop printer tracking.", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleStopPrinterStateTracking(final PrinterId printerId) {
        synchronized (this.mLock) {
            if (this.mTrackedPrinterList == null || !this.mTrackedPrinterList.remove(printerId)) {
            } else if (this.mTrackedPrinterList.isEmpty()) {
                this.mTrackedPrinterList = null;
            }
        }
    }

    private void stopTrackingAllPrinters() {
        synchronized (this.mLock) {
            if (this.mTrackedPrinterList == null) {
                return;
            }
            for (int i = this.mTrackedPrinterList.size() - 1; i >= 0; i--) {
                PrinterId printerId = (PrinterId) this.mTrackedPrinterList.get(i);
                if (printerId.getServiceName().equals(this.mComponentName)) {
                    handleStopPrinterStateTracking(printerId);
                }
            }
        }
    }

    public void dump(DualDumpOutputStream proto) {
        DumpUtils.writeComponentName(proto, "component_name", 1146756268033L, this.mComponentName);
        proto.write("is_destroyed", 1133871366146L, this.mDestroyed);
        proto.write("is_bound", 1133871366147L, isBound());
        proto.write("has_discovery_session", 1133871366148L, this.mHasPrinterDiscoverySession);
        proto.write("has_active_print_jobs", 1133871366149L, this.mHasActivePrintJobs);
        int i = 0;
        proto.write("is_discovering_printers", 1133871366150L, this.mDiscoveryPriorityList != null);
        synchronized (this.mLock) {
            if (this.mTrackedPrinterList != null) {
                int numTrackedPrinters = this.mTrackedPrinterList.size();
                while (i < numTrackedPrinters) {
                    com.android.internal.print.DumpUtils.writePrinterId(proto, "tracked_printers", 2246267895815L, (PrinterId) this.mTrackedPrinterList.get(i));
                    i++;
                }
            }
        }
    }

    private boolean isBound() {
        return this.mPrintService != null;
    }

    private void ensureBound() {
        if (!isBound() && !this.mBinding) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("] ensureBound()");
            Slog.i(str, stringBuilder.toString());
            this.mBinding = true;
            if (!this.mContext.bindServiceAsUser(this.mIntent, this.mServiceConnection, 71303169, new UserHandle(this.mUserId))) {
                String str2 = LOG_TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[user: ");
                stringBuilder2.append(this.mUserId);
                stringBuilder2.append("] could not bind to ");
                stringBuilder2.append(this.mIntent);
                Slog.i(str2, stringBuilder2.toString());
                this.mBinding = false;
                if (!this.mServiceDied) {
                    handleBinderDied();
                }
            }
        }
    }

    private void ensureUnbound() {
        if (isBound() || this.mBinding) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[user: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append("] ensureUnbound()");
            Slog.i(str, stringBuilder.toString());
            this.mBinding = false;
            this.mPendingCommands.clear();
            this.mHasActivePrintJobs = false;
            this.mHasPrinterDiscoverySession = false;
            this.mDiscoveryPriorityList = null;
            synchronized (this.mLock) {
                this.mTrackedPrinterList = null;
            }
            if (isBound()) {
                try {
                    this.mPrintService.setClient(null);
                } catch (RemoteException e) {
                }
                this.mPrintService.asBinder().unlinkToDeath(this, 0);
                this.mPrintService = null;
                this.mContext.unbindService(this.mServiceConnection);
            }
        }
    }
}
