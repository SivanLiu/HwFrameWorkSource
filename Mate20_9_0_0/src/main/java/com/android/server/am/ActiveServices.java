package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ServiceStartArgs;
import android.content.ComponentName;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.Intent.FilterComparison;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteCallback.OnResultListener;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.TransactionTooLargeException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Jlog;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.StatsLog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.webkit.WebViewZygote;
import com.android.internal.app.procstats.ServiceState;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.server.AppStateTracker;
import com.android.server.AppStateTracker.Listener;
import com.android.server.LocalServices;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pg.PGManagerInternal;
import com.android.server.pm.DumpState;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.wm.WindowManagerInternal;
import com.huawei.pgmng.log.LogPower;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ActiveServices {
    static final int CHECK_INTERVAL = ((int) (10000.0f * ActivityManagerService.SCALE_ANR));
    private static final boolean DEBUG_DELAYED_SERVICE = ActivityManagerDebugConfig.DEBUG_SERVICE;
    private static final boolean DEBUG_DELAYED_STARTS = DEBUG_DELAYED_SERVICE;
    private static final String HW_PARENT_CONTROL = "com.huawei.parentcontrol";
    static final boolean IS_FPGA = boardname.contains("fpga");
    static final int LAST_ANR_LIFETIME_DURATION_MSECS = 7200000;
    private static final boolean LOG_SERVICE_START_STOP = true;
    static final int SERVICE_BACKGROUND_TIMEOUT = (SERVICE_TIMEOUT * 10);
    private static final long SERVICE_CONNECTIONS_THRESHOLD = 100;
    private static final boolean SERVICE_RESCHEDULE = SystemProperties.getBoolean("ro.am.reschedule_service", false);
    static final int SERVICE_START_FOREGROUND_TIMEOUT = 10000;
    static final int SERVICE_TIMEOUT = ((int) ((IS_FPGA ? 2.0E7f : 20000.0f) * ActivityManagerService.SCALE_ANR));
    static final int SERVICE_WAIT_TIMEOUT = 3000;
    private static final boolean SHOW_DUNGEON_NOTIFICATION = false;
    private static final String TAG = "ActivityManager";
    private static final String TAG_MU = "ActivityManager_MU";
    private static final String TAG_SERVICE = "ActivityManager";
    private static final String TAG_SERVICE_EXECUTING = "ActivityManager";
    private static final long TOOK_THRESHOLD_MS = 1000;
    static String boardname = SystemProperties.get("ro.board.boardname", "0");
    private static final boolean enableANRWarning = SystemProperties.getBoolean("ro.anr.warning.enable", false);
    final ActivityManagerService mAm;
    final ArrayList<ServiceRecord> mDestroyingServices = new ArrayList();
    String mLastAnrDump;
    final Runnable mLastAnrDumpClearer;
    final int mMaxStartingBackground;
    final ArrayList<ServiceRecord> mPendingServices = new ArrayList();
    final ArrayList<ServiceRecord> mRestartingServices = new ArrayList();
    boolean mScreenOn;
    final ArrayMap<IBinder, ArrayList<ConnectionRecord>> mServiceConnections = new ArrayMap();
    final SparseArray<ServiceMap> mServiceMap = new SparseArray();
    private ArrayList<ServiceRecord> mTmpCollectionResults = null;

    static final class ActiveForegroundApp {
        boolean mAppOnTop;
        long mEndTime;
        long mHideTime;
        CharSequence mLabel;
        int mNumActive;
        String mPackageName;
        boolean mShownWhileScreenOn;
        boolean mShownWhileTop;
        long mStartTime;
        long mStartVisibleTime;
        int mUid;

        ActiveForegroundApp() {
        }
    }

    final class ServiceDumper {
        private final String[] args;
        private final boolean dumpAll;
        private final String dumpPackage;
        private final FileDescriptor fd;
        private final ItemMatcher matcher;
        private boolean needSep;
        private final long nowReal = SystemClock.elapsedRealtime();
        private boolean printed;
        private boolean printedAnything;
        private final PrintWriter pw;
        private final ArrayList<ServiceRecord> services = new ArrayList();
        final /* synthetic */ ActiveServices this$0;

        ServiceDumper(ActiveServices this$0, FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
            ActiveServices activeServices = this$0;
            String[] strArr = args;
            String str = dumpPackage;
            this.this$0 = activeServices;
            boolean z = false;
            this.needSep = false;
            this.printedAnything = false;
            this.printed = false;
            this.fd = fd;
            this.pw = pw;
            this.args = strArr;
            this.dumpAll = dumpAll;
            this.dumpPackage = str;
            this.matcher = new ItemMatcher();
            this.matcher.build(strArr, opti);
            int[] users = activeServices.mAm.mUserController.getUsers();
            int length = users.length;
            int i = 0;
            while (i < length) {
                ServiceMap smap = activeServices.getServiceMapLocked(users[i]);
                if (smap.mServicesByName.size() > 0) {
                    int si = z;
                    while (si < smap.mServicesByName.size()) {
                        ServiceRecord r = (ServiceRecord) smap.mServicesByName.valueAt(si);
                        if (this.matcher.match(r, r.name) && (str == null || str.equals(r.appInfo.packageName))) {
                            this.services.add(r);
                        }
                        si++;
                        activeServices = this$0;
                    }
                }
                i++;
                activeServices = this$0;
                z = false;
            }
        }

        private void dumpHeaderLocked() {
            this.pw.println("ACTIVITY MANAGER SERVICES (dumpsys activity services)");
            if (this.this$0.mLastAnrDump != null) {
                this.pw.println("  Last ANR service:");
                this.pw.print(this.this$0.mLastAnrDump);
                this.pw.println();
            }
        }

        void dumpLocked() {
            dumpHeaderLocked();
            try {
                for (int user : this.this$0.mAm.mUserController.getUsers()) {
                    int serviceIdx = 0;
                    while (serviceIdx < this.services.size() && ((ServiceRecord) this.services.get(serviceIdx)).userId != user) {
                        serviceIdx++;
                    }
                    this.printed = false;
                    if (serviceIdx < this.services.size()) {
                        this.needSep = false;
                        while (serviceIdx < this.services.size()) {
                            ServiceRecord r = (ServiceRecord) this.services.get(serviceIdx);
                            serviceIdx++;
                            if (r.userId != user) {
                                break;
                            }
                            dumpServiceLocalLocked(r);
                        }
                        this.needSep |= this.printed;
                    }
                    dumpUserRemainsLocked(user);
                }
            } catch (Exception e) {
                Slog.w(ActivityManagerService.TAG, "Exception in dumpServicesLocked", e);
            }
            dumpRemainsLocked();
        }

        void dumpWithClient() {
            synchronized (this.this$0.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    dumpHeaderLocked();
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            try {
                for (int user : this.this$0.mAm.mUserController.getUsers()) {
                    int serviceIdx = 0;
                    while (serviceIdx < this.services.size() && ((ServiceRecord) this.services.get(serviceIdx)).userId != user) {
                        serviceIdx++;
                    }
                    this.printed = false;
                    if (serviceIdx < this.services.size()) {
                        this.needSep = false;
                        while (serviceIdx < this.services.size()) {
                            ServiceRecord r = (ServiceRecord) this.services.get(serviceIdx);
                            serviceIdx++;
                            if (r.userId != user) {
                                break;
                            }
                            synchronized (this.this$0.mAm) {
                                ActivityManagerService.boostPriorityForLockedSection();
                                dumpServiceLocalLocked(r);
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            dumpServiceClient(r);
                        }
                        this.needSep |= this.printed;
                    }
                    synchronized (this.this$0.mAm) {
                        ActivityManagerService.boostPriorityForLockedSection();
                        dumpUserRemainsLocked(user);
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            } catch (Exception e) {
                Slog.w(ActivityManagerService.TAG, "Exception in dumpServicesLocked", e);
            } catch (Throwable th) {
                while (true) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            synchronized (this.this$0.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    dumpRemainsLocked();
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }

        private void dumpUserHeaderLocked(int user) {
            if (!this.printed) {
                if (this.printedAnything) {
                    this.pw.println();
                }
                PrintWriter printWriter = this.pw;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  User ");
                stringBuilder.append(user);
                stringBuilder.append(" active services:");
                printWriter.println(stringBuilder.toString());
                this.printed = true;
            }
            this.printedAnything = true;
            if (this.needSep) {
                this.pw.println();
            }
        }

        private void dumpServiceLocalLocked(ServiceRecord r) {
            dumpUserHeaderLocked(r.userId);
            this.pw.print("  * ");
            this.pw.println(r);
            if (this.dumpAll) {
                r.dump(this.pw, "    ");
                this.needSep = true;
                return;
            }
            this.pw.print("    app=");
            this.pw.println(r.app);
            this.pw.print("    created=");
            TimeUtils.formatDuration(r.createRealTime, this.nowReal, this.pw);
            this.pw.print(" started=");
            this.pw.print(r.startRequested);
            this.pw.print(" connections=");
            this.pw.println(r.connections.size());
            if (r.connections.size() > 0) {
                this.pw.println("    Connections:");
                for (int conni = 0; conni < r.connections.size(); conni++) {
                    ArrayList<ConnectionRecord> clist = (ArrayList) r.connections.valueAt(conni);
                    for (int i = 0; i < clist.size(); i++) {
                        ConnectionRecord conn = (ConnectionRecord) clist.get(i);
                        this.pw.print("      ");
                        this.pw.print(conn.binding.intent.intent.getIntent().toShortString(false, false, false, false));
                        this.pw.print(" -> ");
                        ProcessRecord proc = conn.binding.client;
                        this.pw.println(proc != null ? proc.toShortString() : "null");
                    }
                }
            }
        }

        private void dumpServiceClient(ServiceRecord r) {
            ProcessRecord proc = r.app;
            if (proc != null) {
                IApplicationThread thread = proc.thread;
                if (thread != null) {
                    this.pw.println("    Client:");
                    this.pw.flush();
                    TransferPipe tp;
                    try {
                        tp = new TransferPipe();
                        thread.dumpService(tp.getWriteFd(), r, this.args);
                        tp.setBufferPrefix("      ");
                        tp.go(this.fd, 2000);
                        tp.kill();
                    } catch (IOException e) {
                        PrintWriter printWriter = this.pw;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("      Failure while dumping the service: ");
                        stringBuilder.append(e);
                        printWriter.println(stringBuilder.toString());
                    } catch (RemoteException e2) {
                        this.pw.println("      Got a RemoteException while dumping the service");
                    } catch (Throwable th) {
                        tp.kill();
                    }
                    this.needSep = true;
                }
            }
        }

        private void dumpUserRemainsLocked(int user) {
            int si;
            ServiceMap smap = this.this$0.getServiceMapLocked(user);
            this.printed = false;
            int SN = smap.mDelayedStartList.size();
            for (si = 0; si < SN; si++) {
                ServiceRecord r = (ServiceRecord) smap.mDelayedStartList.get(si);
                if (this.matcher.match(r, r.name) && (this.dumpPackage == null || this.dumpPackage.equals(r.appInfo.packageName))) {
                    if (!this.printed) {
                        if (this.printedAnything) {
                            this.pw.println();
                        }
                        PrintWriter printWriter = this.pw;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("  User ");
                        stringBuilder.append(user);
                        stringBuilder.append(" delayed start services:");
                        printWriter.println(stringBuilder.toString());
                        this.printed = true;
                    }
                    this.printedAnything = true;
                    this.pw.print("  * Delayed start ");
                    this.pw.println(r);
                }
            }
            this.printed = false;
            si = smap.mStartingBackground.size();
            for (int si2 = 0; si2 < si; si2++) {
                ServiceRecord r2 = (ServiceRecord) smap.mStartingBackground.get(si2);
                if (this.matcher.match(r2, r2.name) && (this.dumpPackage == null || this.dumpPackage.equals(r2.appInfo.packageName))) {
                    if (!this.printed) {
                        if (this.printedAnything) {
                            this.pw.println();
                        }
                        PrintWriter printWriter2 = this.pw;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("  User ");
                        stringBuilder2.append(user);
                        stringBuilder2.append(" starting in background:");
                        printWriter2.println(stringBuilder2.toString());
                        this.printed = true;
                    }
                    this.printedAnything = true;
                    this.pw.print("  * Starting bg ");
                    this.pw.println(r2);
                }
            }
        }

        private void dumpRemainsLocked() {
            int i;
            ServiceRecord r;
            int i2 = 0;
            if (this.this$0.mPendingServices.size() > 0) {
                this.printed = false;
                for (i = 0; i < this.this$0.mPendingServices.size(); i++) {
                    r = (ServiceRecord) this.this$0.mPendingServices.get(i);
                    if (this.matcher.match(r, r.name) && (this.dumpPackage == null || this.dumpPackage.equals(r.appInfo.packageName))) {
                        this.printedAnything = true;
                        if (!this.printed) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.needSep = true;
                            this.pw.println("  Pending services:");
                            this.printed = true;
                        }
                        this.pw.print("  * Pending ");
                        this.pw.println(r);
                        r.dump(this.pw, "    ");
                    }
                }
                this.needSep = true;
            }
            if (this.this$0.mRestartingServices.size() > 0) {
                this.printed = false;
                for (i = 0; i < this.this$0.mRestartingServices.size(); i++) {
                    r = (ServiceRecord) this.this$0.mRestartingServices.get(i);
                    if (this.matcher.match(r, r.name) && (this.dumpPackage == null || this.dumpPackage.equals(r.appInfo.packageName))) {
                        this.printedAnything = true;
                        if (!this.printed) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.needSep = true;
                            this.pw.println("  Restarting services:");
                            this.printed = true;
                        }
                        this.pw.print("  * Restarting ");
                        this.pw.println(r);
                        r.dump(this.pw, "    ");
                    }
                }
                this.needSep = true;
            }
            if (this.this$0.mDestroyingServices.size() > 0) {
                this.printed = false;
                for (i = 0; i < this.this$0.mDestroyingServices.size(); i++) {
                    r = (ServiceRecord) this.this$0.mDestroyingServices.get(i);
                    if (this.matcher.match(r, r.name) && (this.dumpPackage == null || this.dumpPackage.equals(r.appInfo.packageName))) {
                        this.printedAnything = true;
                        if (!this.printed) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.needSep = true;
                            this.pw.println("  Destroying services:");
                            this.printed = true;
                        }
                        this.pw.print("  * Destroy ");
                        this.pw.println(r);
                        r.dump(this.pw, "    ");
                    }
                }
                this.needSep = true;
            }
            if (this.dumpAll) {
                this.printed = false;
                for (i = 0; i < this.this$0.mServiceConnections.size(); i++) {
                    ArrayList<ConnectionRecord> r2 = (ArrayList) this.this$0.mServiceConnections.valueAt(i);
                    for (int i3 = 0; i3 < r2.size(); i3++) {
                        ConnectionRecord cr = (ConnectionRecord) r2.get(i3);
                        if (this.matcher.match(cr.binding.service, cr.binding.service.name) && (this.dumpPackage == null || (cr.binding.client != null && this.dumpPackage.equals(cr.binding.client.info.packageName)))) {
                            this.printedAnything = true;
                            if (!this.printed) {
                                if (this.needSep) {
                                    this.pw.println();
                                }
                                this.needSep = true;
                                this.pw.println("  Connection bindings to services:");
                                this.printed = true;
                            }
                            this.pw.print("  * ");
                            this.pw.println(cr);
                            cr.dump(this.pw, "    ");
                        }
                    }
                }
            }
            if (this.matcher.all) {
                long nowElapsed = SystemClock.elapsedRealtime();
                int[] users = this.this$0.mAm.mUserController.getUsers();
                int length = users.length;
                while (i2 < length) {
                    int user = users[i2];
                    boolean printedUser = false;
                    ServiceMap smap = (ServiceMap) this.this$0.mServiceMap.get(user);
                    if (smap != null) {
                        for (int i4 = smap.mActiveForegroundApps.size() - 1; i4 >= 0; i4--) {
                            ActiveForegroundApp aa = (ActiveForegroundApp) smap.mActiveForegroundApps.valueAt(i4);
                            if (this.dumpPackage == null || this.dumpPackage.equals(aa.mPackageName)) {
                                if (!printedUser) {
                                    printedUser = true;
                                    this.printedAnything = true;
                                    if (this.needSep) {
                                        this.pw.println();
                                    }
                                    this.needSep = true;
                                    this.pw.print("Active foreground apps - user ");
                                    this.pw.print(user);
                                    this.pw.println(":");
                                }
                                this.pw.print("  #");
                                this.pw.print(i4);
                                this.pw.print(": ");
                                this.pw.println(aa.mPackageName);
                                if (aa.mLabel != null) {
                                    this.pw.print("    mLabel=");
                                    this.pw.println(aa.mLabel);
                                }
                                this.pw.print("    mNumActive=");
                                this.pw.print(aa.mNumActive);
                                this.pw.print(" mAppOnTop=");
                                this.pw.print(aa.mAppOnTop);
                                this.pw.print(" mShownWhileTop=");
                                this.pw.print(aa.mShownWhileTop);
                                this.pw.print(" mShownWhileScreenOn=");
                                this.pw.println(aa.mShownWhileScreenOn);
                                this.pw.print("    mStartTime=");
                                TimeUtils.formatDuration(aa.mStartTime - nowElapsed, this.pw);
                                this.pw.print(" mStartVisibleTime=");
                                TimeUtils.formatDuration(aa.mStartVisibleTime - nowElapsed, this.pw);
                                this.pw.println();
                                if (aa.mEndTime != 0) {
                                    this.pw.print("    mEndTime=");
                                    TimeUtils.formatDuration(aa.mEndTime - nowElapsed, this.pw);
                                    this.pw.println();
                                }
                            }
                        }
                        if (smap.hasMessagesOrCallbacks()) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.printedAnything = true;
                            this.needSep = true;
                            this.pw.print("  Handler - user ");
                            this.pw.print(user);
                            this.pw.println(":");
                            smap.dumpMine(new PrintWriterPrinter(this.pw), "    ");
                        }
                    }
                    i2++;
                }
            }
            if (!this.printedAnything) {
                this.pw.println("  (nothing)");
            }
        }
    }

    private final class ServiceLookupResult {
        final String permission;
        final ServiceRecord record;

        ServiceLookupResult(ServiceRecord _record, String _permission) {
            this.record = _record;
            this.permission = _permission;
        }
    }

    final class ServiceMap extends Handler {
        static final int MSG_BG_START_TIMEOUT = 1;
        static final int MSG_UPDATE_FOREGROUND_APPS = 2;
        final ArrayMap<String, ActiveForegroundApp> mActiveForegroundApps = new ArrayMap();
        boolean mActiveForegroundAppsChanged;
        final ArrayList<ServiceRecord> mDelayedStartList = new ArrayList();
        final ArrayMap<FilterComparison, ServiceRecord> mServicesByIntent = new ArrayMap();
        final ArrayMap<ComponentName, ServiceRecord> mServicesByName = new ArrayMap();
        final ArrayList<ServiceRecord> mStartingBackground = new ArrayList();
        final int mUserId;

        ServiceMap(Looper looper, int userId) {
            super(looper);
            this.mUserId = userId;
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (ActiveServices.this.mAm) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            rescheduleDelayedStartsLocked();
                        } finally {
                            while (true) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                break;
                            }
                        }
                    }
                    return;
                case 2:
                    ActiveServices.this.updateForegroundApps(this);
                    return;
                default:
                    return;
            }
        }

        void ensureNotStartingBackgroundLocked(ServiceRecord r) {
            String str;
            StringBuilder stringBuilder;
            if (this.mStartingBackground.remove(r)) {
                if (ActiveServices.DEBUG_DELAYED_STARTS) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("No longer background starting: ");
                    stringBuilder.append(r);
                    Slog.v(str, stringBuilder.toString());
                }
                rescheduleDelayedStartsLocked();
            }
            if (this.mDelayedStartList.remove(r) && ActiveServices.DEBUG_DELAYED_STARTS) {
                str = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("No longer delaying start: ");
                stringBuilder.append(r);
                Slog.v(str, stringBuilder.toString());
            }
        }

        void rescheduleDelayedStartsLocked() {
            String str;
            StringBuilder stringBuilder;
            ServiceRecord r;
            removeMessages(1);
            long now = SystemClock.uptimeMillis();
            int i = 0;
            int N = this.mStartingBackground.size();
            while (i < N) {
                ServiceRecord r2 = (ServiceRecord) this.mStartingBackground.get(i);
                if (r2.startingBgTimeout <= now) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Waited long enough for: ");
                    stringBuilder.append(r2);
                    Slog.i(str, stringBuilder.toString());
                    this.mStartingBackground.remove(i);
                    N--;
                    i--;
                }
                i++;
            }
            while (this.mDelayedStartList.size() > 0 && this.mStartingBackground.size() < ActiveServices.this.mMaxStartingBackground) {
                r = (ServiceRecord) this.mDelayedStartList.remove(0);
                if (ActiveServices.DEBUG_DELAYED_STARTS) {
                    String str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("REM FR DELAY LIST (exec next): ");
                    stringBuilder2.append(r);
                    Slog.v(str2, stringBuilder2.toString());
                }
                if (r.pendingStarts.size() <= 0) {
                    String str3 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("**** NO PENDING STARTS! ");
                    stringBuilder3.append(r);
                    stringBuilder3.append(" startReq=");
                    stringBuilder3.append(r.startRequested);
                    stringBuilder3.append(" delayedStop=");
                    stringBuilder3.append(r.delayedStop);
                    Slog.w(str3, stringBuilder3.toString());
                } else {
                    if (ActiveServices.DEBUG_DELAYED_SERVICE && this.mDelayedStartList.size() > 0) {
                        Slog.v(ActivityManagerService.TAG, "Remaining delayed list:");
                        for (int i2 = 0; i2 < this.mDelayedStartList.size(); i2++) {
                            str = ActivityManagerService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("  #");
                            stringBuilder.append(i2);
                            stringBuilder.append(": ");
                            stringBuilder.append(this.mDelayedStartList.get(i2));
                            Slog.v(str, stringBuilder.toString());
                        }
                    }
                    r.delayed = false;
                    try {
                        ProcessRecord servicePR = ActiveServices.this.mAm.getProcessRecordLocked(r.processName, r.appInfo.uid, false);
                        if (!(r.appInfo.uid < 10000 || servicePR == null || servicePR.thread == null)) {
                            LogPower.push(148, "serviceboot", r.packageName, Integer.toString(servicePR.pid));
                        }
                        ActiveServices.this.startServiceInnerLocked(this, ((StartItem) r.pendingStarts.get(0)).intent, r, false, true);
                    } catch (TransactionTooLargeException e) {
                    }
                }
            }
            if (this.mStartingBackground.size() > 0) {
                r = (ServiceRecord) this.mStartingBackground.get(0);
                long when = r.startingBgTimeout > now ? r.startingBgTimeout : now;
                if (ActiveServices.DEBUG_DELAYED_SERVICE) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Top bg start is ");
                    stringBuilder.append(r);
                    stringBuilder.append(", can delay others up to ");
                    stringBuilder.append(when);
                    Slog.v(str, stringBuilder.toString());
                }
                sendMessageAtTime(obtainMessage(1), when);
            }
            if (this.mStartingBackground.size() < ActiveServices.this.mMaxStartingBackground) {
                ActiveServices.this.mAm.backgroundServicesFinishedLocked(this.mUserId);
            }
        }
    }

    private class ServiceRestarter implements Runnable {
        private ServiceRecord mService;

        private ServiceRestarter() {
        }

        /* synthetic */ ServiceRestarter(ActiveServices x0, AnonymousClass1 x1) {
            this();
        }

        void setService(ServiceRecord service) {
            this.mService = service;
        }

        public void run() {
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActiveServices.this.performServiceRestartLocked(this.mService);
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
    }

    class ForcedStandbyListener extends Listener {
        ForcedStandbyListener() {
        }

        public void stopForegroundServicesForUidPackage(int uid, String packageName) {
            synchronized (ActiveServices.this.mAm) {
                try {
                    int i;
                    ActivityManagerService.boostPriorityForLockedSection();
                    ServiceMap smap = ActiveServices.this.getServiceMapLocked(UserHandle.getUserId(uid));
                    int N = smap.mServicesByName.size();
                    ArrayList<ServiceRecord> toStop = new ArrayList(N);
                    for (i = 0; i < N; i++) {
                        ServiceRecord r = (ServiceRecord) smap.mServicesByName.valueAt(i);
                        if ((uid == r.serviceInfo.applicationInfo.uid || packageName.equals(r.serviceInfo.packageName)) && r.isForeground) {
                            toStop.add(r);
                        }
                    }
                    i = toStop.size();
                    if (i > 0 && ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        String str = ActivityManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Package ");
                        stringBuilder.append(packageName);
                        stringBuilder.append(SliceAuthority.DELIMITER);
                        stringBuilder.append(uid);
                        stringBuilder.append(" entering FAS with foreground services");
                        Slog.i(str, stringBuilder.toString());
                    }
                    for (int i2 = 0; i2 < i; i2++) {
                        ServiceRecord r2 = (ServiceRecord) toStop.get(i2);
                        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                            String str2 = ActivityManagerService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("  Stopping fg for service ");
                            stringBuilder2.append(r2);
                            Slog.i(str2, stringBuilder2.toString());
                        }
                        ActiveServices.this.setServiceForegroundInnerLocked(r2, 0, null, 0);
                    }
                } finally {
                    while (true) {
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
        }
    }

    public ActiveServices(ActivityManagerService service) {
        int i = 1;
        this.mScreenOn = true;
        this.mLastAnrDumpClearer = new Runnable() {
            public void run() {
                synchronized (ActiveServices.this.mAm) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        ActiveServices.this.mLastAnrDump = null;
                    } finally {
                        while (true) {
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                }
            }
        };
        this.mAm = service;
        int maxBg = 0;
        try {
            maxBg = Integer.parseInt(SystemProperties.get("ro.config.max_starting_bg", "0"));
        } catch (RuntimeException e) {
        }
        if (maxBg > 0) {
            i = maxBg;
        } else if (!ActivityManager.isLowRamDeviceStatic()) {
            i = 8;
        }
        this.mMaxStartingBackground = i;
    }

    void systemServicesReady() {
        ((AppStateTracker) LocalServices.getService(AppStateTracker.class)).addListener(new ForcedStandbyListener());
    }

    ServiceRecord getServiceByNameLocked(ComponentName name, int callingUser) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            String str = TAG_MU;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getServiceByNameLocked(");
            stringBuilder.append(name);
            stringBuilder.append("), callingUser = ");
            stringBuilder.append(callingUser);
            Slog.v(str, stringBuilder.toString());
        }
        return (ServiceRecord) getServiceMapLocked(callingUser).mServicesByName.get(name);
    }

    boolean hasBackgroundServicesLocked(int callingUser) {
        ServiceMap smap = (ServiceMap) this.mServiceMap.get(callingUser);
        return smap != null && smap.mStartingBackground.size() >= this.mMaxStartingBackground;
    }

    private ServiceMap getServiceMapLocked(int callingUser) {
        ServiceMap smap = (ServiceMap) this.mServiceMap.get(callingUser);
        if (smap != null) {
            return smap;
        }
        smap = new ServiceMap(this.mAm.mHandler.getLooper(), callingUser);
        this.mServiceMap.put(callingUser, smap);
        return smap;
    }

    ArrayMap<ComponentName, ServiceRecord> getServicesLocked(int callingUser) {
        return getServiceMapLocked(callingUser).mServicesByName;
    }

    private boolean appRestrictedAnyInBackground(int uid, String packageName) {
        return this.mAm.mAppOpsService.checkOperation(70, uid, packageName) != 0;
    }

    ComponentName startServiceLocked(IApplicationThread caller, Intent service, String resolvedType, int callingPid, int callingUid, boolean fgRequired, String callingPackage, int userId) throws TransactionTooLargeException {
        String str;
        StringBuilder stringBuilder;
        String str2;
        boolean callerFg;
        StringBuilder stringBuilder2;
        IApplicationThread iApplicationThread = caller;
        Intent intent = service;
        int i = callingPid;
        int i2 = callingUid;
        String str3 = callingPackage;
        if (DEBUG_DELAYED_STARTS) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startService: ");
            stringBuilder.append(intent);
            stringBuilder.append(" type=");
            str2 = resolvedType;
            stringBuilder.append(str2);
            stringBuilder.append(" args=");
            stringBuilder.append(service.getExtras());
            Slog.v(str, stringBuilder.toString());
        } else {
            str2 = resolvedType;
        }
        if (iApplicationThread != null) {
            ProcessRecord callerApp = this.mAm.getRecordForAppLocked(iApplicationThread);
            if (callerApp != null) {
                callerFg = callerApp.setSchedGroup != 0;
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to find app for caller ");
                stringBuilder2.append(iApplicationThread);
                stringBuilder2.append(" (pid=");
                stringBuilder2.append(i);
                stringBuilder2.append(") when starting service ");
                stringBuilder2.append(intent);
                throw new SecurityException(stringBuilder2.toString());
            }
        }
        callerFg = true;
        boolean callerFg2 = callerFg;
        this.mAm.setServiceFlagLocked(1);
        boolean callerFg3 = callerFg2;
        String str4 = str3;
        ServiceLookupResult res = retrieveServiceLocked(intent, str2, str3, i, i2, userId, true, callerFg3, false, false);
        this.mAm.setServiceFlagLocked(0);
        if (res == null) {
            return null;
        }
        if (res.record == null) {
            return new ComponentName("!", res.permission != null ? res.permission : "private to package");
        }
        ServiceRecord r = res.record;
        if (this.mAm.mUserController.exists(r.userId)) {
            String str5;
            boolean callerFg4;
            boolean forceSilentAbort;
            boolean fgRequired2;
            boolean callerFg5;
            ServiceRecord r2;
            boolean bgLaunch = this.mAm.isUidActiveLocked(r.appInfo.uid) ^ 1;
            callerFg = false;
            if (bgLaunch && appRestrictedAnyInBackground(r.appInfo.uid, r.packageName)) {
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    str5 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Forcing bg-only service start only for ");
                    stringBuilder2.append(r.shortName);
                    stringBuilder2.append(" : bgLaunch=");
                    stringBuilder2.append(bgLaunch);
                    stringBuilder2.append(" callerFg=");
                    callerFg4 = callerFg3;
                    stringBuilder2.append(callerFg4);
                    Slog.d(str5, stringBuilder2.toString());
                } else {
                    callerFg4 = callerFg3;
                }
                callerFg = true;
            } else {
                callerFg4 = callerFg3;
            }
            boolean forcedStandby = callerFg;
            if (fgRequired) {
                int mode = this.mAm.mAppOpsService.checkOperation(76, r.appInfo.uid, r.packageName);
                if (mode != 3) {
                    switch (mode) {
                        case 0:
                            break;
                        case 1:
                            String str6 = ActivityManagerService.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("startForegroundService not allowed due to app op: service ");
                            stringBuilder3.append(intent);
                            stringBuilder3.append(" to ");
                            stringBuilder3.append(r.name.flattenToShortString());
                            stringBuilder3.append(" from pid=");
                            stringBuilder3.append(i);
                            stringBuilder3.append(" uid=");
                            stringBuilder3.append(i2);
                            stringBuilder3.append(" pkg=");
                            stringBuilder3.append(str4);
                            Slog.w(str6, stringBuilder3.toString());
                            forceSilentAbort = true;
                            fgRequired2 = false;
                            break;
                        default:
                            return new ComponentName("!!", "foreground not allowed as per app op");
                    }
                }
            }
            fgRequired2 = fgRequired;
            forceSilentAbort = false;
            if (forcedStandby || !(r.startRequested || fgRequired2)) {
                callerFg5 = callerFg4;
                r2 = r;
                int allowed = this.mAm.getAppStartModeLocked(r.appInfo.uid, r.packageName, r.appInfo.targetSdkVersion, i, false, false, forcedStandby);
                if (allowed != 0) {
                    str5 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Background start not allowed: service ");
                    stringBuilder2.append(intent);
                    stringBuilder2.append(" to ");
                    stringBuilder2.append(r2.name.flattenToShortString());
                    stringBuilder2.append(" from pid=");
                    stringBuilder2.append(i);
                    stringBuilder2.append(" uid=");
                    stringBuilder2.append(i2);
                    stringBuilder2.append(" pkg=");
                    stringBuilder2.append(str4);
                    stringBuilder2.append(" startFg?=");
                    stringBuilder2.append(fgRequired2);
                    Slog.w(str5, stringBuilder2.toString());
                    if (allowed == 1 || forceSilentAbort) {
                        return null;
                    }
                    if (forcedStandby && fgRequired2) {
                        if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                            Slog.v(ActivityManagerService.TAG, "Silently dropping foreground service launch due to FAS");
                        }
                        return null;
                    }
                    UidRecord uidRec = (UidRecord) this.mAm.mActiveUids.get(r2.appInfo.uid);
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("app is in background uid ");
                    stringBuilder4.append(uidRec);
                    return new ComponentName("?", stringBuilder4.toString());
                }
            }
            callerFg5 = callerFg4;
            callerFg3 = bgLaunch;
            r2 = r;
            PGManagerInternal pgm = (PGManagerInternal) LocalServices.getService(PGManagerInternal.class);
            if (pgm == null || str4 == null || this.mAm.getUidStateLocked(i2) == 2 || str4.equals(r2.name.getPackageName()) || !pgm.isServiceProxy(r2.name, null)) {
                if (r2.appInfo.targetSdkVersion < 26 && fgRequired2) {
                    if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK || ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        str = ActivityManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("startForegroundService() but host targets ");
                        stringBuilder.append(r2.appInfo.targetSdkVersion);
                        stringBuilder.append(" - not requiring startForeground()");
                        Slog.i(str, stringBuilder.toString());
                    }
                    fgRequired2 = false;
                }
                NeededUriGrants neededGrants = this.mAm.checkGrantUriPermissionFromIntentLocked(i2, r2.packageName, intent, service.getFlags(), null, r2.userId);
                if (this.mAm.mPermissionReviewRequired && !requestStartTargetPermissionsReviewIfNeededLocked(r2, str4, i2, intent, callerFg5, userId)) {
                    return null;
                }
                if (unscheduleServiceRestartLocked(r2, i2, false) && ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("START SERVICE WHILE RESTART PENDING: ");
                    stringBuilder.append(r2);
                    Slog.v(str, stringBuilder.toString());
                }
                r2.lastActivity = SystemClock.uptimeMillis();
                r2.startRequested = true;
                r2.delayedStop = false;
                r2.fgRequired = fgRequired2;
                StartItem startItem = r0;
                ArrayList arrayList = r2.pendingStarts;
                StartItem startItem2 = new StartItem(r2, false, r2.makeNextStartId(), intent, neededGrants, i2);
                arrayList.add(startItem);
                if (fgRequired2) {
                    this.mAm.mAppOpsService.startOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, r2.appInfo.uid, r2.packageName, true);
                }
                ServiceMap smap = getServiceMapLocked(r2.userId);
                callerFg = false;
                boolean callerFg6 = callerFg5;
                if (!callerFg6 && !fgRequired2 && r2.app == null && this.mAm.mUserController.hasStartedUserState(r2.userId)) {
                    ProcessRecord proc = this.mAm.getProcessRecordLocked(r2.processName, r2.appInfo.uid, false);
                    String str7;
                    StringBuilder stringBuilder5;
                    if (proc == null || proc.curProcState > 10) {
                        if (DEBUG_DELAYED_SERVICE) {
                            str7 = ActivityManagerService.TAG;
                            stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("Potential start delay of ");
                            stringBuilder5.append(r2);
                            stringBuilder5.append(" in ");
                            stringBuilder5.append(proc);
                            Slog.v(str7, stringBuilder5.toString());
                        }
                        if (r2.delayed) {
                            if (DEBUG_DELAYED_STARTS) {
                                str7 = ActivityManagerService.TAG;
                                stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("Continuing to delay: ");
                                stringBuilder5.append(r2);
                                Slog.v(str7, stringBuilder5.toString());
                            }
                            return r2.name;
                        } else if (smap.mStartingBackground.size() >= this.mMaxStartingBackground) {
                            str7 = ActivityManagerService.TAG;
                            stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("Delaying start of: ");
                            stringBuilder5.append(r2);
                            Slog.i(str7, stringBuilder5.toString());
                            smap.mDelayedStartList.add(r2);
                            r2.delayed = true;
                            return r2.name;
                        } else {
                            if (DEBUG_DELAYED_STARTS) {
                                str7 = ActivityManagerService.TAG;
                                stringBuilder5 = new StringBuilder();
                                stringBuilder5.append("Not delaying: ");
                                stringBuilder5.append(r2);
                                Slog.v(str7, stringBuilder5.toString());
                            }
                            callerFg = true;
                        }
                    } else if (proc.curProcState >= 9) {
                        callerFg = true;
                        if (DEBUG_DELAYED_STARTS) {
                            str7 = ActivityManagerService.TAG;
                            stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("Not delaying, but counting as bg: ");
                            stringBuilder5.append(r2);
                            Slog.v(str7, stringBuilder5.toString());
                        }
                    } else if (DEBUG_DELAYED_STARTS) {
                        stringBuilder2 = new StringBuilder(128);
                        stringBuilder2.append("Not potential delay (state=");
                        stringBuilder2.append(proc.curProcState);
                        stringBuilder2.append(' ');
                        stringBuilder2.append(proc.adjType);
                        String reason = proc.makeAdjReason();
                        if (reason != null) {
                            stringBuilder2.append(' ');
                            stringBuilder2.append(reason);
                        }
                        stringBuilder2.append("): ");
                        stringBuilder2.append(r2.toString());
                        Slog.v(ActivityManagerService.TAG, stringBuilder2.toString());
                    }
                } else if (DEBUG_DELAYED_STARTS) {
                    if (callerFg6 || fgRequired2) {
                        str5 = ActivityManagerService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Not potential delay (callerFg=");
                        stringBuilder2.append(callerFg6);
                        stringBuilder2.append(" uid=");
                        stringBuilder2.append(i2);
                        stringBuilder2.append(" pid=");
                        stringBuilder2.append(i);
                        stringBuilder2.append(" fgRequired=");
                        stringBuilder2.append(fgRequired2);
                        stringBuilder2.append("): ");
                        stringBuilder2.append(r2);
                        Slog.v(str5, stringBuilder2.toString());
                    } else if (r2.app != null) {
                        str5 = ActivityManagerService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Not potential delay (cur app=");
                        stringBuilder2.append(r2.app);
                        stringBuilder2.append("): ");
                        stringBuilder2.append(r2);
                        Slog.v(str5, stringBuilder2.toString());
                    } else {
                        str5 = ActivityManagerService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Not potential delay (user ");
                        stringBuilder2.append(r2.userId);
                        stringBuilder2.append(" not started): ");
                        stringBuilder2.append(r2);
                        Slog.v(str5, stringBuilder2.toString());
                    }
                }
                boolean addToStarting = callerFg;
                if (!(r2.appInfo.uid < 10000 || str4 == null || str4.equals(r2.packageName))) {
                    LogPower.push(148, "serviceboot", r2.packageName, Integer.toString(0), new String[]{str4});
                }
                return startServiceInnerLocked(smap, intent, r2, callerFg6, addToStarting);
            }
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("start service is proxy: ");
            stringBuilder.append(r2.name);
            Slog.i(str, stringBuilder.toString());
            return null;
        }
        str = ActivityManagerService.TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Trying to start service with non-existent user! ");
        stringBuilder.append(r.userId);
        Slog.w(str, stringBuilder.toString());
        return null;
    }

    private boolean requestStartTargetPermissionsReviewIfNeededLocked(ServiceRecord r, String callingPackage, int callingUid, Intent service, boolean callerFg, int userId) {
        ServiceRecord serviceRecord = r;
        Intent intent = service;
        int i;
        if (!this.mAm.getPackageManagerInternalLocked().isPermissionsReviewRequired(serviceRecord.packageName, serviceRecord.userId)) {
            i = userId;
            return true;
        } else if (callerFg) {
            IIntentSender target = this.mAm.getIntentSenderLocked(4, callingPackage, callingUid, userId, null, null, 0, new Intent[]{intent}, new String[]{intent.resolveType(this.mAm.mContext.getContentResolver())}, 1409286144, null);
            final Intent intent2 = new Intent("android.intent.action.REVIEW_PERMISSIONS");
            intent2.addFlags(276824064);
            intent2.putExtra("android.intent.extra.PACKAGE_NAME", serviceRecord.packageName);
            intent2.putExtra("android.intent.extra.INTENT", new IntentSender(target));
            if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("u");
                stringBuilder.append(serviceRecord.userId);
                stringBuilder.append(" Launching permission review for package ");
                stringBuilder.append(serviceRecord.packageName);
                Slog.i(str, stringBuilder.toString());
            }
            i = userId;
            this.mAm.mHandler.post(new Runnable() {
                public void run() {
                    ActiveServices.this.mAm.mContext.startActivityAsUser(intent2, new UserHandle(i));
                }
            });
            return false;
        } else {
            String str2 = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("u");
            stringBuilder2.append(serviceRecord.userId);
            stringBuilder2.append(" Starting a service in package");
            stringBuilder2.append(serviceRecord.packageName);
            stringBuilder2.append(" requires a permissions review");
            Slog.w(str2, stringBuilder2.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:36:0x00d2, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    ComponentName startServiceInnerLocked(ServiceMap smap, Intent service, ServiceRecord r, boolean callerFg, boolean addToStarting) throws TransactionTooLargeException {
        ServiceMap serviceMap = smap;
        ServiceRecord serviceRecord = r;
        ServiceState stracker = r.getTracker();
        Throwable th = true;
        if (stracker != null) {
            stracker.setStarted(true, this.mAm.mProcessStats.getMemFactorLocked(), serviceRecord.lastActivity);
        }
        boolean z = false;
        serviceRecord.callStart = false;
        String error = serviceRecord.stats.getBatteryStats();
        synchronized (error) {
            try {
                serviceRecord.stats.startRunningLocked();
            } finally {
                Intent intent = service;
                while (true) {
                }
            }
        }
        error = this.mAm.mHwAMSEx;
        ApplicationInfo applicationInfo = serviceRecord.appInfo;
        if (error != null) {
            return new ComponentName("!!", error);
        }
        if (serviceRecord.startRequested && addToStarting) {
            if (serviceMap.mStartingBackground.size() == 0) {
                z = th;
            }
            boolean first = z;
            serviceMap.mStartingBackground.add(serviceRecord);
            serviceRecord.startingBgTimeout = SystemClock.uptimeMillis() + this.mAm.mConstants.BG_START_TIMEOUT;
            if (DEBUG_DELAYED_SERVICE) {
                RuntimeException here = new RuntimeException("here");
                here.fillInStackTrace();
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Starting background (first=");
                stringBuilder.append(first);
                stringBuilder.append("): ");
                stringBuilder.append(serviceRecord);
                Slog.v(str, stringBuilder.toString(), here);
            } else if (DEBUG_DELAYED_STARTS) {
                String str2 = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Starting background (first=");
                stringBuilder2.append(first);
                stringBuilder2.append("): ");
                stringBuilder2.append(serviceRecord);
                Slog.v(str2, stringBuilder2.toString());
            }
            if (first) {
                serviceMap.rescheduleDelayedStartsLocked();
            }
        } else if (callerFg || serviceRecord.fgRequired) {
            serviceMap.ensureNotStartingBackgroundLocked(serviceRecord);
        }
        return serviceRecord.name;
    }

    private void stopServiceLocked(ServiceRecord service) {
        if (service.delayed) {
            if (DEBUG_DELAYED_STARTS) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Delaying stop of pending: ");
                stringBuilder.append(service);
                Slog.v(str, stringBuilder.toString());
            }
            service.delayedStop = true;
            return;
        }
        synchronized (service.stats.getBatteryStats()) {
            service.stats.stopRunningLocked();
        }
        service.startRequested = false;
        if (service.tracker != null) {
            service.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
        }
        service.callStart = false;
        bringDownServiceIfNeededLocked(service, false, false);
    }

    int stopServiceLocked(IApplicationThread caller, Intent service, String resolvedType, int userId) {
        StringBuilder stringBuilder;
        String str;
        IApplicationThread iApplicationThread = caller;
        Intent intent = service;
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            String str2 = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("stopService: ");
            stringBuilder.append(intent);
            stringBuilder.append(" type=");
            str = resolvedType;
            stringBuilder.append(str);
            Slog.v(str2, stringBuilder.toString());
        } else {
            str = resolvedType;
        }
        ProcessRecord callerApp = this.mAm.getRecordForAppLocked(iApplicationThread);
        if (iApplicationThread == null || callerApp != null) {
            ServiceLookupResult r = retrieveServiceLocked(intent, str, null, Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, false, false);
            if (r == null) {
                return 0;
            }
            if (r.record == null) {
                return -1;
            }
            long origId = Binder.clearCallingIdentity();
            try {
                stopServiceLocked(r.record);
                return 1;
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to find app for caller ");
            stringBuilder.append(iApplicationThread);
            stringBuilder.append(" (pid=");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(") when stopping service ");
            stringBuilder.append(intent);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    void stopInBackgroundLocked(int uid) {
        ServiceMap services = (ServiceMap) this.mServiceMap.get(UserHandle.getUserId(uid));
        ArrayList<ServiceRecord> stopping = null;
        if (services != null) {
            int i;
            ServiceRecord service;
            for (i = services.mServicesByName.size() - 1; i >= 0; i--) {
                service = (ServiceRecord) services.mServicesByName.valueAt(i);
                if (service != null && service.appInfo.uid == uid && service.startRequested && this.mAm.getAppStartModeLocked(service.appInfo.uid, service.packageName, service.appInfo.targetSdkVersion, -1, false, false, false) != 0) {
                    if (stopping == null) {
                        stopping = new ArrayList();
                    }
                    String compName = service.name.flattenToShortString();
                    EventLogTags.writeAmStopIdleService(service.appInfo.uid, compName);
                    StringBuilder sb = new StringBuilder(64);
                    sb.append("Stopping service due to app idle: ");
                    UserHandle.formatUid(sb, service.appInfo.uid);
                    sb.append(" ");
                    TimeUtils.formatDuration(service.createRealTime - SystemClock.elapsedRealtime(), sb);
                    sb.append(" ");
                    sb.append(compName);
                    Slog.w(ActivityManagerService.TAG, sb.toString());
                    stopping.add(service);
                }
            }
            if (stopping != null) {
                for (i = stopping.size() - 1; i >= 0; i--) {
                    service = (ServiceRecord) stopping.get(i);
                    service.delayed = false;
                    services.ensureNotStartingBackgroundLocked(service);
                    stopServiceLocked(service);
                }
            }
        }
    }

    IBinder peekServiceLocked(Intent service, String resolvedType, String callingPackage) {
        ServiceLookupResult r = retrieveServiceLocked(service, resolvedType, callingPackage, Binder.getCallingPid(), Binder.getCallingUid(), UserHandle.getCallingUserId(), false, false, false, false);
        if (r == null) {
            return null;
        }
        if (r.record != null) {
            IntentBindRecord ib = (IntentBindRecord) r.record.bindings.get(r.record.intent);
            if (ib != null) {
                return ib.binder;
            }
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Permission Denial: Accessing service from pid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(", uid=");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(" requires ");
        stringBuilder.append(r.permission);
        throw new SecurityException(stringBuilder.toString());
    }

    boolean stopServiceTokenLocked(ComponentName className, IBinder token, int startId) {
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopServiceToken: ");
            stringBuilder.append(className);
            stringBuilder.append(" ");
            stringBuilder.append(token);
            stringBuilder.append(" startId=");
            stringBuilder.append(startId);
            Slog.v(str, stringBuilder.toString());
        }
        ServiceRecord r = findServiceLocked(className, token, UserHandle.getCallingUserId());
        if (r == null) {
            return false;
        }
        if (startId >= 0) {
            StartItem si = r.findDeliveredStart(startId, false, false);
            if (si != null) {
                while (r.deliveredStarts.size() > 0) {
                    StartItem cur = (StartItem) r.deliveredStarts.remove(0);
                    cur.removeUriPermissionsLocked();
                    if (cur == si) {
                        break;
                    }
                }
            }
            if (r.getLastStartId() != startId) {
                return false;
            }
            if (r.deliveredStarts.size() > 0) {
                String str2 = ActivityManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("stopServiceToken startId ");
                stringBuilder2.append(startId);
                stringBuilder2.append(" is last, but have ");
                stringBuilder2.append(r.deliveredStarts.size());
                stringBuilder2.append(" remaining args");
                Slog.w(str2, stringBuilder2.toString());
            }
        }
        synchronized (r.stats.getBatteryStats()) {
            r.stats.stopRunningLocked();
        }
        r.startRequested = false;
        if (r.tracker != null) {
            r.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
        }
        r.callStart = false;
        long origId = Binder.clearCallingIdentity();
        bringDownServiceIfNeededLocked(r, false, false);
        Binder.restoreCallingIdentity(origId);
        return true;
    }

    public void setServiceForegroundLocked(ComponentName className, IBinder token, int id, Notification notification, int flags) {
        int userId = UserHandle.getCallingUserId();
        long origId = Binder.clearCallingIdentity();
        try {
            ServiceRecord r = findServiceLocked(className, token, userId);
            if (r != null) {
                setServiceForegroundInnerLocked(r, id, notification, flags);
            }
            Binder.restoreCallingIdentity(origId);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
        }
    }

    boolean foregroundAppShownEnoughLocked(ActiveForegroundApp aa, long nowElapsed) {
        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Shown enough: pkg=");
            stringBuilder.append(aa.mPackageName);
            stringBuilder.append(", uid=");
            stringBuilder.append(aa.mUid);
            Slog.d(str, stringBuilder.toString());
        }
        aa.mHideTime = JobStatus.NO_LATEST_RUNTIME;
        long minTime;
        if (aa.mShownWhileTop) {
            if (!ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                return true;
            }
            Slog.d(ActivityManagerService.TAG, "YES - shown while on top");
            return true;
        } else if (this.mScreenOn || aa.mShownWhileScreenOn) {
            long j;
            minTime = aa.mStartVisibleTime;
            if (aa.mStartTime != aa.mStartVisibleTime) {
                j = this.mAm.mConstants.FGSERVICE_SCREEN_ON_AFTER_TIME;
            } else {
                j = this.mAm.mConstants.FGSERVICE_MIN_SHOWN_TIME;
            }
            minTime += j;
            if (nowElapsed >= minTime) {
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(ActivityManagerService.TAG, "YES - shown long enough with screen on");
                }
                return true;
            }
            j = this.mAm.mConstants.FGSERVICE_MIN_REPORT_TIME + nowElapsed;
            aa.mHideTime = j > minTime ? j : minTime;
            if (!ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                return false;
            }
            String str2 = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("NO -- wait ");
            stringBuilder2.append(aa.mHideTime - nowElapsed);
            stringBuilder2.append(" with screen on");
            Slog.d(str2, stringBuilder2.toString());
            return false;
        } else {
            minTime = aa.mEndTime + this.mAm.mConstants.FGSERVICE_SCREEN_ON_BEFORE_TIME;
            if (nowElapsed >= minTime) {
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(ActivityManagerService.TAG, "YES - gone long enough with screen off");
                }
                return true;
            }
            aa.mHideTime = minTime;
            if (!ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                return false;
            }
            String str3 = ActivityManagerService.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("NO -- wait ");
            stringBuilder3.append(aa.mHideTime - nowElapsed);
            stringBuilder3.append(" with screen off");
            Slog.d(str3, stringBuilder3.toString());
            return false;
        }
    }

    void updateForegroundApps(ServiceMap smap) {
        ArrayList<ActiveForegroundApp> active = null;
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long now = SystemClock.elapsedRealtime();
                long nextUpdateTime = JobStatus.NO_LATEST_RUNTIME;
                if (smap != null) {
                    if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        String str = ActivityManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Updating foreground apps for user ");
                        stringBuilder.append(smap.mUserId);
                        Slog.d(str, stringBuilder.toString());
                    }
                    for (int i = smap.mActiveForegroundApps.size() - 1; i >= 0; i--) {
                        ActiveForegroundApp aa = (ActiveForegroundApp) smap.mActiveForegroundApps.valueAt(i);
                        if (aa.mEndTime != 0) {
                            if (foregroundAppShownEnoughLocked(aa, now)) {
                                smap.mActiveForegroundApps.removeAt(i);
                                smap.mActiveForegroundAppsChanged = true;
                            } else if (aa.mHideTime < nextUpdateTime) {
                                nextUpdateTime = aa.mHideTime;
                            }
                        }
                        if (!aa.mAppOnTop) {
                            if (active == null) {
                                active = new ArrayList();
                            }
                            if (ActivityManagerDebugConfig.HWFLOW) {
                                String str2 = ActivityManagerService.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(" updateForegroundApps Adding active: pkg= ");
                                stringBuilder2.append(aa.mPackageName);
                                stringBuilder2.append(", uid=");
                                stringBuilder2.append(aa.mUid);
                                Slog.i(str2, stringBuilder2.toString());
                            }
                            active.add(aa);
                        }
                    }
                    smap.removeMessages(2);
                    if (nextUpdateTime < JobStatus.NO_LATEST_RUNTIME) {
                        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                            String str3 = ActivityManagerService.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Next update time in: ");
                            stringBuilder3.append(nextUpdateTime - now);
                            Slog.d(str3, stringBuilder3.toString());
                        }
                        smap.sendMessageAtTime(smap.obtainMessage(2), (SystemClock.uptimeMillis() + nextUpdateTime) - SystemClock.elapsedRealtime());
                    }
                }
                if (smap.mActiveForegroundAppsChanged) {
                    smap.mActiveForegroundAppsChanged = false;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private void requestUpdateActiveForegroundAppsLocked(ServiceMap smap, long timeElapsed) {
        Message msg = smap.obtainMessage(2);
        if (timeElapsed != 0) {
            smap.sendMessageAtTime(msg, (SystemClock.uptimeMillis() + timeElapsed) - SystemClock.elapsedRealtime());
            return;
        }
        smap.mActiveForegroundAppsChanged = true;
        smap.sendMessage(msg);
    }

    private void decActiveForegroundAppLocked(ServiceMap smap, ServiceRecord r) {
        ActiveForegroundApp active = (ActiveForegroundApp) smap.mActiveForegroundApps.get(r.packageName);
        if (active != null) {
            active.mNumActive--;
            if (active.mNumActive <= 0) {
                active.mEndTime = SystemClock.elapsedRealtime();
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(ActivityManagerService.TAG, "Ended running of service");
                }
                if (foregroundAppShownEnoughLocked(active, active.mEndTime)) {
                    smap.mActiveForegroundApps.remove(r.packageName);
                    smap.mActiveForegroundAppsChanged = true;
                    requestUpdateActiveForegroundAppsLocked(smap, 0);
                } else if (active.mHideTime < JobStatus.NO_LATEST_RUNTIME) {
                    requestUpdateActiveForegroundAppsLocked(smap, active.mHideTime);
                }
            }
        }
    }

    void updateScreenStateLocked(boolean screenOn) {
        if (this.mScreenOn != screenOn) {
            this.mScreenOn = screenOn;
            if (screenOn) {
                long nowElapsed = SystemClock.elapsedRealtime();
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(ActivityManagerService.TAG, "Screen turned on");
                }
                for (int i = this.mServiceMap.size() - 1; i >= 0; i--) {
                    ServiceMap smap = (ServiceMap) this.mServiceMap.valueAt(i);
                    long nextUpdateTime = JobStatus.NO_LATEST_RUNTIME;
                    boolean changed = false;
                    for (int j = smap.mActiveForegroundApps.size() - 1; j >= 0; j--) {
                        ActiveForegroundApp active = (ActiveForegroundApp) smap.mActiveForegroundApps.valueAt(j);
                        if (active.mEndTime != 0) {
                            if (!active.mShownWhileScreenOn && active.mStartVisibleTime == active.mStartTime) {
                                active.mStartVisibleTime = nowElapsed;
                                active.mEndTime = nowElapsed;
                            }
                            if (foregroundAppShownEnoughLocked(active, nowElapsed)) {
                                smap.mActiveForegroundApps.remove(active.mPackageName);
                                smap.mActiveForegroundAppsChanged = true;
                                changed = true;
                            } else if (active.mHideTime < nextUpdateTime) {
                                nextUpdateTime = active.mHideTime;
                            }
                        } else if (!active.mShownWhileScreenOn) {
                            active.mShownWhileScreenOn = true;
                            active.mStartVisibleTime = nowElapsed;
                        }
                    }
                    if (changed) {
                        requestUpdateActiveForegroundAppsLocked(smap, 0);
                    } else if (nextUpdateTime < JobStatus.NO_LATEST_RUNTIME) {
                        requestUpdateActiveForegroundAppsLocked(smap, nextUpdateTime);
                    }
                }
            }
        }
    }

    void foregroundServiceProcStateChangedLocked(UidRecord uidRec) {
        ServiceMap smap = (ServiceMap) this.mServiceMap.get(UserHandle.getUserId(uidRec.uid));
        if (smap != null) {
            boolean changed = false;
            for (int j = smap.mActiveForegroundApps.size() - 1; j >= 0; j--) {
                ActiveForegroundApp active = (ActiveForegroundApp) smap.mActiveForegroundApps.valueAt(j);
                if (active.mUid == uidRec.uid) {
                    if (uidRec.curProcState <= 2) {
                        if (!active.mAppOnTop) {
                            active.mAppOnTop = true;
                            changed = true;
                        }
                        active.mShownWhileTop = true;
                    } else if (active.mAppOnTop) {
                        active.mAppOnTop = false;
                        changed = true;
                    }
                }
            }
            if (changed) {
                requestUpdateActiveForegroundAppsLocked(smap, 0);
            }
        }
    }

    private void setServiceForegroundInnerLocked(ServiceRecord r, int id, Notification notification, int flags) {
        ServiceRecord serviceRecord = r;
        int i = id;
        Notification notification2 = notification;
        boolean z = false;
        if (i == 0) {
            if (serviceRecord.isForeground) {
                ServiceMap smap = getServiceMapLocked(serviceRecord.userId);
                if (smap != null) {
                    decActiveForegroundAppLocked(smap, serviceRecord);
                }
                serviceRecord.isForeground = false;
                this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, serviceRecord.appInfo.uid, serviceRecord.packageName);
                StatsLog.write(60, serviceRecord.appInfo.uid, serviceRecord.shortName, 2);
                if (serviceRecord.app != null) {
                    this.mAm.updateLruProcessLocked(serviceRecord.app, false, null);
                    updateServiceForegroundLocked(serviceRecord.app, true);
                }
            }
            if ((flags & 1) != 0) {
                cancelForegroundNotificationLocked(r);
                serviceRecord.foregroundId = 0;
                serviceRecord.foregroundNoti = null;
            } else if (serviceRecord.appInfo.targetSdkVersion >= 21) {
                r.stripForegroundServiceFlagFromNotification();
                if ((flags & 2) != 0) {
                    serviceRecord.foregroundId = 0;
                    serviceRecord.foregroundNoti = null;
                }
            }
        } else if (notification2 != null) {
            String str;
            StringBuilder stringBuilder;
            if (serviceRecord.appInfo.isInstantApp()) {
                switch (this.mAm.mAppOpsService.checkOperation(68, serviceRecord.appInfo.uid, serviceRecord.appInfo.packageName)) {
                    case 0:
                        break;
                    case 1:
                        str = ActivityManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Instant app ");
                        stringBuilder.append(serviceRecord.appInfo.packageName);
                        stringBuilder.append(" does not have permission to create foreground services, ignoring.");
                        Slog.w(str, stringBuilder.toString());
                        return;
                    case 2:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Instant app ");
                        stringBuilder.append(serviceRecord.appInfo.packageName);
                        stringBuilder.append(" does not have permission to create foreground services");
                        throw new SecurityException(stringBuilder.toString());
                    default:
                        this.mAm.enforcePermission("android.permission.INSTANT_APP_FOREGROUND_SERVICE", serviceRecord.app.pid, serviceRecord.appInfo.uid, "startForeground");
                        break;
                }
            } else if (serviceRecord.appInfo.targetSdkVersion >= 28) {
                this.mAm.enforcePermission("android.permission.FOREGROUND_SERVICE", serviceRecord.app.pid, serviceRecord.appInfo.uid, "startForeground");
            }
            boolean alreadyStartedOp = false;
            if (serviceRecord.fgRequired) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE || ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                    String str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Service called startForeground() as required: ");
                    stringBuilder2.append(serviceRecord);
                    Slog.i(str2, stringBuilder2.toString());
                }
                serviceRecord.fgRequired = false;
                serviceRecord.fgWaiting = false;
                alreadyStartedOp = true;
                this.mAm.mHandler.removeMessages(66, serviceRecord);
            }
            boolean ignoreForeground = false;
            try {
                String str3;
                StringBuilder stringBuilder3;
                int mode = this.mAm.mAppOpsService.checkOperation(76, serviceRecord.appInfo.uid, serviceRecord.packageName);
                if (mode != 3) {
                    switch (mode) {
                        case 0:
                            break;
                        case 1:
                            str3 = ActivityManagerService.TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Service.startForeground() not allowed due to app op: service ");
                            stringBuilder3.append(serviceRecord.shortName);
                            Slog.w(str3, stringBuilder3.toString());
                            ignoreForeground = true;
                            break;
                        default:
                            throw new SecurityException("Foreground not allowed as per app op");
                    }
                }
                if (!ignoreForeground && appRestrictedAnyInBackground(serviceRecord.appInfo.uid, serviceRecord.packageName)) {
                    str3 = ActivityManagerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Service.startForeground() not allowed due to bg restriction: service ");
                    stringBuilder3.append(serviceRecord.shortName);
                    Slog.w(str3, stringBuilder3.toString());
                    updateServiceForegroundLocked(serviceRecord.app, false);
                    ignoreForeground = true;
                }
                if (!ignoreForeground) {
                    if (serviceRecord.foregroundId != i) {
                        cancelForegroundNotificationLocked(r);
                        serviceRecord.foregroundId = i;
                    }
                    notification2.flags |= 64;
                    serviceRecord.foregroundNoti = notification2;
                    if (!serviceRecord.isForeground) {
                        ServiceMap smap2 = getServiceMapLocked(serviceRecord.userId);
                        if (smap2 != null) {
                            ActiveForegroundApp active = (ActiveForegroundApp) smap2.mActiveForegroundApps.get(serviceRecord.packageName);
                            if (active == null) {
                                active = new ActiveForegroundApp();
                                active.mPackageName = serviceRecord.packageName;
                                active.mUid = serviceRecord.appInfo.uid;
                                active.mShownWhileScreenOn = this.mScreenOn;
                                if (!(serviceRecord.app == null || serviceRecord.app.uidRecord == null)) {
                                    if (serviceRecord.app.uidRecord.curProcState <= 2) {
                                        z = true;
                                    }
                                    active.mShownWhileTop = z;
                                    active.mAppOnTop = z;
                                }
                                long elapsedRealtime = SystemClock.elapsedRealtime();
                                active.mStartVisibleTime = elapsedRealtime;
                                active.mStartTime = elapsedRealtime;
                                smap2.mActiveForegroundApps.put(serviceRecord.packageName, active);
                                requestUpdateActiveForegroundAppsLocked(smap2, 0);
                            }
                            active.mNumActive++;
                        }
                        serviceRecord.isForeground = true;
                        this.mAm.mAppOpsService.startOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, serviceRecord.appInfo.uid, serviceRecord.packageName, true);
                        StatsLog.write(60, serviceRecord.appInfo.uid, serviceRecord.shortName, 1);
                    }
                    r.postNotification();
                    if (serviceRecord.app != null) {
                        updateServiceForegroundLocked(serviceRecord.app, true);
                    }
                    getServiceMapLocked(serviceRecord.userId).ensureNotStartingBackgroundLocked(serviceRecord);
                    this.mAm.notifyPackageUse(serviceRecord.serviceInfo.packageName, 2);
                } else if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Suppressing startForeground() for FAS ");
                    stringBuilder.append(serviceRecord);
                    Slog.d(str, stringBuilder.toString());
                }
                if (alreadyStartedOp) {
                    this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, serviceRecord.appInfo.uid, serviceRecord.packageName);
                }
            } catch (Throwable th) {
                if (alreadyStartedOp) {
                    this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, serviceRecord.appInfo.uid, serviceRecord.packageName);
                }
            }
        } else {
            throw new IllegalArgumentException("null notification");
        }
    }

    private void cancelForegroundNotificationLocked(ServiceRecord r) {
        if (!(r == null || r.foregroundId == 0)) {
            ServiceMap sm = getServiceMapLocked(r.userId);
            if (sm != null) {
                int i = sm.mServicesByName.size() - 1;
                while (i >= 0) {
                    ServiceRecord other = (ServiceRecord) sm.mServicesByName.valueAt(i);
                    if (other == null || other == r || other.foregroundId != r.foregroundId || !other.packageName.equals(r.packageName)) {
                        i--;
                    } else {
                        return;
                    }
                }
            }
            r.cancelNotification();
        }
    }

    private void updateServiceForegroundLocked(ProcessRecord proc, boolean oomAdj) {
        boolean anyForeground = false;
        for (int i = proc.services.size() - 1; i >= 0; i--) {
            ServiceRecord sr = (ServiceRecord) proc.services.valueAt(i);
            if (sr.isForeground || sr.fgRequired) {
                anyForeground = true;
                break;
            }
        }
        this.mAm.updateProcessForegroundLocked(proc, anyForeground, oomAdj);
    }

    private void updateWhitelistManagerLocked(ProcessRecord proc) {
        proc.whitelistManager = false;
        for (int i = proc.services.size() - 1; i >= 0; i--) {
            if (((ServiceRecord) proc.services.valueAt(i)).whitelistManager) {
                proc.whitelistManager = true;
                return;
            }
        }
    }

    public void updateServiceConnectionActivitiesLocked(ProcessRecord clientProc) {
        if (clientProc != null && clientProc.connections != null) {
            ArraySet<ProcessRecord> updatedProcesses = null;
            for (int i = 0; i < clientProc.connections.size(); i++) {
                ProcessRecord proc = ((ConnectionRecord) clientProc.connections.valueAt(i)).binding.service.app;
                if (!(proc == null || proc == clientProc)) {
                    if (updatedProcesses == null) {
                        updatedProcesses = new ArraySet();
                    } else if (updatedProcesses.contains(proc)) {
                    }
                    updatedProcesses.add(proc);
                    updateServiceClientActivitiesLocked(proc, null, false);
                }
            }
        }
    }

    private boolean updateServiceClientActivitiesLocked(ProcessRecord proc, ConnectionRecord modCr, boolean updateLru) {
        if (modCr != null && modCr.binding.client != null && modCr.binding.client.activities.size() <= 0) {
            return false;
        }
        boolean anyClientActivities = false;
        for (int i = proc.services.size() - 1; i >= 0 && !anyClientActivities; i--) {
            ServiceRecord sr = (ServiceRecord) proc.services.valueAt(i);
            for (int conni = sr.connections.size() - 1; conni >= 0 && !anyClientActivities; conni--) {
                ArrayList<ConnectionRecord> clist = (ArrayList) sr.connections.valueAt(conni);
                for (int cri = clist.size() - 1; cri >= 0; cri--) {
                    ConnectionRecord cr = (ConnectionRecord) clist.get(cri);
                    if (cr.binding.client != null && cr.binding.client != proc && cr.binding.client.activities.size() > 0) {
                        anyClientActivities = true;
                        break;
                    }
                }
            }
        }
        if (anyClientActivities == proc.hasClientActivities) {
            return false;
        }
        proc.hasClientActivities = anyClientActivities;
        if (updateLru) {
            this.mAm.updateLruProcessLocked(proc, anyClientActivities, null);
        }
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:242:0x058c  */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x034e A:{SYNTHETIC, Splitter:B:113:0x034e} */
    /* JADX WARNING: Removed duplicated region for block: B:122:0x0371 A:{SYNTHETIC, Splitter:B:122:0x0371} */
    /* JADX WARNING: Removed duplicated region for block: B:140:0x03cd A:{SYNTHETIC, Splitter:B:140:0x03cd} */
    /* JADX WARNING: Removed duplicated region for block: B:146:0x03d8  */
    /* JADX WARNING: Removed duplicated region for block: B:152:0x03df A:{SYNTHETIC, Splitter:B:152:0x03df} */
    /* JADX WARNING: Removed duplicated region for block: B:158:0x03f0 A:{SYNTHETIC, Splitter:B:158:0x03f0} */
    /* JADX WARNING: Removed duplicated region for block: B:178:0x044f  */
    /* JADX WARNING: Removed duplicated region for block: B:165:0x0403 A:{SYNTHETIC, Splitter:B:165:0x0403} */
    /* JADX WARNING: Removed duplicated region for block: B:182:0x0460  */
    /* JADX WARNING: Removed duplicated region for block: B:202:0x049b A:{SYNTHETIC, Splitter:B:202:0x049b} */
    /* JADX WARNING: Removed duplicated region for block: B:207:0x04e5 A:{SYNTHETIC, Splitter:B:207:0x04e5} */
    /* JADX WARNING: Removed duplicated region for block: B:230:0x055c A:{SYNTHETIC, Splitter:B:230:0x055c} */
    /* JADX WARNING: Removed duplicated region for block: B:242:0x058c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    int bindServiceLocked(IApplicationThread caller, IBinder token, Intent service, String resolvedType, IServiceConnection connection, int flags, String callingPackage, int userId) throws TransactionTooLargeException {
        String str;
        StringBuilder stringBuilder;
        String str2;
        Throwable clist;
        long origId;
        long origId2;
        Object obj;
        ActivityRecord activityRecord;
        boolean z;
        Intent intent;
        IApplicationThread iApplicationThread = caller;
        IBinder iBinder = token;
        Intent intent2 = service;
        String str3 = callingPackage;
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("bindService: ");
            stringBuilder.append(intent2);
            stringBuilder.append(" type=");
            str2 = resolvedType;
            stringBuilder.append(str2);
            stringBuilder.append(" conn=");
            stringBuilder.append(connection.asBinder());
            stringBuilder.append(" flags=0x");
            stringBuilder.append(Integer.toHexString(flags));
            Slog.v(str, stringBuilder.toString());
        } else {
            str2 = resolvedType;
        }
        ProcessRecord callerApp = this.mAm.getRecordForAppLocked(iApplicationThread);
        int i;
        if (callerApp != null) {
            String str4;
            StringBuilder stringBuilder2;
            ActivityRecord activity = null;
            if (iBinder != null) {
                activity = ActivityRecord.isInStackLocked(token);
                if (activity == null) {
                    str4 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Binding with unknown activity: ");
                    stringBuilder2.append(iBinder);
                    Slog.w(str4, stringBuilder2.toString());
                    return 0;
                }
            }
            ActivityRecord activity2 = activity;
            int clientLabel = 0;
            PendingIntent clientIntent = null;
            boolean isCallerSystem = callerApp.info.uid == 1000;
            if (isCallerSystem) {
                intent2.setDefusable(true);
                clientIntent = (PendingIntent) intent2.getParcelableExtra("android.intent.extra.client_intent");
                if (clientIntent != null) {
                    clientLabel = intent2.getIntExtra("android.intent.extra.client_label", 0);
                    if (clientLabel != 0) {
                        intent2 = service.cloneFilter();
                    }
                }
            }
            Intent service2 = intent2;
            int clientLabel2 = clientLabel;
            PendingIntent clientIntent2 = clientIntent;
            if ((flags & 134217728) != 0) {
                this.mAm.enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "BIND_TREAT_LIKE_ACTIVITY");
            }
            StringBuilder stringBuilder3;
            if ((flags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0 && !isCallerSystem) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Non-system caller ");
                stringBuilder3.append(iApplicationThread);
                stringBuilder3.append(" (pid=");
                stringBuilder3.append(Binder.getCallingPid());
                stringBuilder3.append(") set BIND_ALLOW_WHITELIST_MANAGEMENT when binding service ");
                stringBuilder3.append(service2);
                throw new SecurityException(stringBuilder3.toString());
            } else if ((flags & DumpState.DUMP_CHANGES) == 0 || isCallerSystem) {
                boolean callerFg = callerApp.setSchedGroup != 0;
                boolean isBindExternal = (flags & Integer.MIN_VALUE) != 0;
                boolean z2 = true;
                boolean allowInstant = (flags & DumpState.DUMP_CHANGES) != 0;
                this.mAm.setServiceFlagLocked(2);
                i = 2;
                boolean callerFg2 = callerFg;
                Intent service3 = service2;
                ActivityRecord activity3 = activity2;
                i = 0;
                ProcessRecord callerApp2 = callerApp;
                ServiceLookupResult res = retrieveServiceLocked(service2, str2, str3, Binder.getCallingPid(), Binder.getCallingUid(), userId, true, callerFg2, isBindExternal, allowInstant);
                this.mAm.setServiceFlagLocked(i);
                if (res == null) {
                    return i;
                }
                if (res.record == null) {
                    return -1;
                }
                String str5;
                ProcessRecord callerApp3;
                String str6;
                boolean permissionsReviewRequired;
                boolean callerFg3;
                ServiceRecord s = res.record;
                PGManagerInternal pgm = (PGManagerInternal) LocalServices.getService(PGManagerInternal.class);
                if (pgm != null) {
                    str5 = callingPackage;
                    if (str5 != null) {
                        callerApp3 = callerApp2;
                        if (!(this.mAm.getUidStateLocked(callerApp3.info.uid) == 2 || str5.equals(s.name.getPackageName()) || !pgm.isServiceProxy(s.name, null))) {
                            str6 = ActivityManagerService.TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("bind service is proxy: ");
                            stringBuilder3.append(s.name);
                            Slog.i(str6, stringBuilder3.toString());
                            return i;
                        }
                    }
                    callerApp3 = callerApp2;
                } else {
                    callerApp3 = callerApp2;
                    str5 = callingPackage;
                }
                if (this.mAm.mPermissionReviewRequired && this.mAm.getPackageManagerInternalLocked().isPermissionsReviewRequired(s.packageName, s.userId)) {
                    z2 = callerFg2;
                    if (z2) {
                        final ServiceRecord serviceRecord = s;
                        service2 = service3;
                        permissionsReviewRequired = true;
                        callerFg3 = z2;
                        callerApp = callerApp3;
                        final boolean z3 = callerFg3;
                        str3 = str5;
                        final IServiceConnection iServiceConnection = connection;
                        boolean permissionsReviewRequired2 = new RemoteCallback(new OnResultListener() {
                            public void onResult(Bundle result) {
                                synchronized (ActiveServices.this.mAm) {
                                    long identity;
                                    try {
                                        ActivityManagerService.boostPriorityForLockedSection();
                                        identity = Binder.clearCallingIdentity();
                                        if (ActiveServices.this.mPendingServices.contains(serviceRecord)) {
                                            if (ActiveServices.this.mAm.getPackageManagerInternalLocked().isPermissionsReviewRequired(serviceRecord.packageName, serviceRecord.userId)) {
                                                ActiveServices.this.unbindServiceLocked(iServiceConnection);
                                            } else {
                                                try {
                                                    ActiveServices.this.mAm.mHwAMSEx.setHbsMiniAppUid(serviceRecord.appInfo, service2);
                                                    ActiveServices.this.bringUpServiceLocked(serviceRecord, service2.getFlags(), z3, false, false);
                                                } catch (RemoteException e) {
                                                }
                                            }
                                            Binder.restoreCallingIdentity(identity);
                                            ActivityManagerService.resetPriorityAfterLockedSection();
                                            return;
                                        }
                                        Binder.restoreCallingIdentity(identity);
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                    } catch (Throwable th) {
                                        ActivityManagerService.resetPriorityAfterLockedSection();
                                    }
                                }
                            }
                        });
                        final Intent intent3 = new Intent("android.intent.action.REVIEW_PERMISSIONS");
                        intent3.addFlags(276824064);
                        intent3.putExtra("android.intent.extra.PACKAGE_NAME", s.packageName);
                        intent3.putExtra("android.intent.extra.REMOTE_CALLBACK", permissionsReviewRequired2);
                        if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
                            str4 = ActivityManagerService.TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("u");
                            stringBuilder4.append(s.userId);
                            stringBuilder4.append(" Launching permission review for package ");
                            stringBuilder4.append(s.packageName);
                            Slog.i(str4, stringBuilder4.toString());
                        }
                        i = userId;
                        this.mAm.mHandler.post(new Runnable() {
                            public void run() {
                                ActiveServices.this.mAm.mContext.startActivityAsUser(intent3, new UserHandle(i));
                            }
                        });
                    } else {
                        str = ActivityManagerService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("u");
                        stringBuilder2.append(s.userId);
                        stringBuilder2.append(" Binding to a service in package");
                        stringBuilder2.append(s.packageName);
                        stringBuilder2.append(" requires a permissions review");
                        Slog.w(str, stringBuilder2.toString());
                        return i;
                    }
                }
                i = userId;
                ServiceLookupResult serviceLookupResult = res;
                PGManagerInternal pGManagerInternal = pgm;
                callerFg3 = callerFg2;
                callerApp = callerApp3;
                str3 = str5;
                permissionsReviewRequired = false;
                long origId3 = Binder.clearCallingIdentity();
                boolean z4;
                boolean z5;
                try {
                    long origId4;
                    Intent service4;
                    AppBindRecord b;
                    ConnectionRecord connectionRecord;
                    AppBindRecord b2;
                    ConnectionRecord c;
                    IBinder binder;
                    ArrayList<ConnectionRecord> clist2;
                    ActivityRecord activity4;
                    int connectionSize;
                    ArrayList<ConnectionRecord> clist3;
                    ProcessRecord app;
                    if (unscheduleServiceRestartLocked(s, callerApp.info.uid, false)) {
                        try {
                            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                str6 = ActivityManagerService.TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("BIND SERVICE WHILE RESTART PENDING: ");
                                stringBuilder3.append(s);
                                Slog.v(str6, stringBuilder3.toString());
                            }
                        } catch (Throwable th) {
                            clist = th;
                            origId = origId3;
                            Binder.restoreCallingIdentity(origId);
                            throw clist;
                        }
                    }
                    if ((flags & 1) != 0) {
                        s.lastActivity = SystemClock.uptimeMillis();
                        if (!s.hasAutoCreateConnections()) {
                            ServiceState stracker = s.getTracker();
                            if (stracker != null) {
                                stracker.setBound(true, this.mAm.mProcessStats.getMemFactorLocked(), s.lastActivity);
                                origId4 = origId3;
                                this.mAm.startAssociationLocked(callerApp.uid, callerApp.processName, callerApp.curProcState, s.appInfo.uid, s.name, s.processName);
                                service4 = service3;
                                try {
                                    this.mAm.grantEphemeralAccessLocked(callerApp.userId, service4, s.appInfo.uid, UserHandle.getAppId(callerApp.uid));
                                    this.mAm.mHwAMSEx.reportServiceRelationIAware(1, s, callerApp);
                                    b = s.retrieveAppBindingLocked(service4, callerApp);
                                    connectionRecord = connectionRecord;
                                    z4 = isBindExternal;
                                    isBindExternal = true;
                                    b2 = b;
                                    origId2 = origId4;
                                    z5 = allowInstant;
                                    allowInstant = service4;
                                    try {
                                        c = new ConnectionRecord(b, activity3, connection, flags, clientLabel2, clientIntent2);
                                        binder = connection.asBinder();
                                        clist2 = (ArrayList) s.connections.get(binder);
                                        if (clist2 == null) {
                                            try {
                                                clist2 = new ArrayList();
                                                s.connections.put(binder, clist2);
                                            } catch (Throwable th2) {
                                                clist = th2;
                                                obj = allowInstant;
                                                activityRecord = activity3;
                                            }
                                        }
                                        clist2.add(c);
                                        b2.connections.add(c);
                                        activity4 = activity3;
                                        if (activity4 != null) {
                                            try {
                                                if (activity4.connections == null) {
                                                    activity4.connections = new HashSet();
                                                }
                                                activity4.connections.add(c);
                                            } catch (Throwable th3) {
                                                clist = th3;
                                                activityRecord = activity4;
                                                obj = allowInstant;
                                                origId = origId2;
                                                Binder.restoreCallingIdentity(origId);
                                                throw clist;
                                            }
                                        }
                                        b2.client.connections.add(c);
                                        connectionSize = s.connections.size();
                                        if (ActivityManagerDebugConfig.HWFLOW && ((long) connectionSize) > SERVICE_CONNECTIONS_THRESHOLD) {
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("bindServiceLocked ");
                                            stringBuilder.append(s);
                                            stringBuilder.append(",connection size= ");
                                            stringBuilder.append(connectionSize);
                                            stringBuilder.append(",callingPackage= ");
                                            stringBuilder.append(str3);
                                            Flog.i(102, stringBuilder.toString());
                                        }
                                        if ((c.flags & 8) != 0) {
                                            b2.client.hasAboveClient = true;
                                        }
                                        if ((c.flags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0) {
                                            s.whitelistManager = true;
                                        }
                                        if (s.app != null) {
                                            updateServiceClientActivitiesLocked(s.app, c, true);
                                        }
                                        clist2 = (ArrayList) this.mServiceConnections.get(binder);
                                        if (clist2 == null) {
                                            clist2 = new ArrayList();
                                            this.mServiceConnections.put(binder, clist2);
                                        }
                                        clist3 = clist2;
                                        clist3.add(c);
                                        if ((flags & 1) == 0) {
                                            try {
                                                this.mAm.mHwAMSEx.setHbsMiniAppUid(s.appInfo, allowInstant);
                                                s.lastActivity = SystemClock.uptimeMillis();
                                                ServiceRecord serviceRecord2 = s;
                                                connectionSize = allowInstant.getFlags();
                                                activityRecord = activity4;
                                                obj = allowInstant;
                                                allowInstant = c;
                                                try {
                                                    if (bringUpServiceLocked(serviceRecord2, connectionSize, callerFg3, false, permissionsReviewRequired) != null) {
                                                        Binder.restoreCallingIdentity(origId2);
                                                        return 0;
                                                    }
                                                    origId = origId2;
                                                } catch (Throwable th4) {
                                                    clist = th4;
                                                    origId = origId2;
                                                    z = callerFg3;
                                                    Binder.restoreCallingIdentity(origId);
                                                    throw clist;
                                                }
                                            } catch (Throwable th5) {
                                                clist = th5;
                                                activityRecord = activity4;
                                                obj = allowInstant;
                                                origId = origId2;
                                                z = callerFg3;
                                                Binder.restoreCallingIdentity(origId);
                                                throw clist;
                                            }
                                        }
                                        ArrayList<ConnectionRecord> arrayList = clist3;
                                        int i2 = connectionSize;
                                        activityRecord = activity4;
                                        IBinder iBinder2 = binder;
                                        obj = allowInstant;
                                        origId = origId2;
                                        allowInstant = c;
                                        try {
                                            if (s.app != null) {
                                                boolean z6;
                                                if ((flags & 134217728) != 0) {
                                                    try {
                                                        s.app.treatLikeActivity = true;
                                                    } catch (Throwable th6) {
                                                        clist = th6;
                                                        Binder.restoreCallingIdentity(origId);
                                                        throw clist;
                                                    }
                                                }
                                                if (s.whitelistManager) {
                                                    s.app.whitelistManager = true;
                                                }
                                                ActivityManagerService activityManagerService = this.mAm;
                                                ProcessRecord processRecord = s.app;
                                                if (!s.app.hasClientActivities) {
                                                    if (!s.app.treatLikeActivity) {
                                                        z6 = false;
                                                        activityManagerService.updateLruProcessLocked(processRecord, z6, b2.client);
                                                        this.mAm.updateOomAdjLocked(s.app, true);
                                                    }
                                                }
                                                z6 = true;
                                                activityManagerService.updateLruProcessLocked(processRecord, z6, b2.client);
                                                this.mAm.updateOomAdjLocked(s.app, true);
                                            }
                                            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                                str6 = ActivityManagerService.TAG;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("Bind ");
                                                stringBuilder2.append(s);
                                                stringBuilder2.append(" with ");
                                                stringBuilder2.append(b2);
                                                stringBuilder2.append(": received=");
                                                stringBuilder2.append(b2.intent.received);
                                                stringBuilder2.append(" apps=");
                                                stringBuilder2.append(b2.intent.apps.size());
                                                stringBuilder2.append(" doRebind=");
                                                stringBuilder2.append(b2.intent.doRebind);
                                                Slog.v(str6, stringBuilder2.toString());
                                            }
                                            if (s.app != null) {
                                                try {
                                                    if (b2.intent.received) {
                                                        allowInstant.conn.connected(s.name, b2.intent.binder, false);
                                                        if (b2.intent.apps.size() == 1 && b2.intent.doRebind) {
                                                            callerFg = callerFg3;
                                                            try {
                                                                requestServiceBindingLocked(s, b2.intent, callerFg, true);
                                                                getServiceMapLocked(s.userId).ensureNotStartingBackgroundLocked(s);
                                                                Binder.restoreCallingIdentity(origId);
                                                                app = s.app;
                                                                app = this.mAm.getProcessRecordLocked(s.processName, s.appInfo.uid, true);
                                                                if (app != null) {
                                                                }
                                                                return 1;
                                                            } catch (Throwable th7) {
                                                                clist = th7;
                                                                z = callerFg;
                                                            }
                                                        } else {
                                                            callerFg = callerFg3;
                                                            getServiceMapLocked(s.userId).ensureNotStartingBackgroundLocked(s);
                                                            Binder.restoreCallingIdentity(origId);
                                                            app = s.app;
                                                            if (app == null && s.processName != null) {
                                                                app = this.mAm.getProcessRecordLocked(s.processName, s.appInfo.uid, true);
                                                            }
                                                            if (app != null || app.uid < 10000 || callerApp.pid == app.pid || callerApp.info == null || app.info == null || callerApp.info.packageName == null || callerApp.info.packageName.equals(app.info.packageName)) {
                                                            } else {
                                                                z = callerFg;
                                                                LogPower.push(148, "bindservice", s.packageName, Integer.toString(app.pid), new String[]{str3});
                                                                LogPower.push(166, s.processName, Integer.toString(callerApp.pid), Integer.toString(app.pid), new String[]{"service"});
                                                            }
                                                            return 1;
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    String str7 = ActivityManagerService.TAG;
                                                    StringBuilder stringBuilder5 = new StringBuilder();
                                                    stringBuilder5.append("Failure sending service ");
                                                    stringBuilder5.append(s.shortName);
                                                    stringBuilder5.append(" to connection ");
                                                    stringBuilder5.append(allowInstant.conn.asBinder());
                                                    stringBuilder5.append(" (in ");
                                                    stringBuilder5.append(allowInstant.binding.client.processName);
                                                    stringBuilder5.append(")");
                                                    Slog.w(str7, stringBuilder5.toString(), e);
                                                } catch (Throwable th8) {
                                                    clist = th8;
                                                    z = callerFg3;
                                                    Binder.restoreCallingIdentity(origId);
                                                    throw clist;
                                                }
                                            }
                                            callerFg = callerFg3;
                                        } catch (Throwable th9) {
                                            clist = th9;
                                            z = callerFg3;
                                            Binder.restoreCallingIdentity(origId);
                                            throw clist;
                                        }
                                    } catch (Throwable th10) {
                                        clist = th10;
                                        obj = allowInstant;
                                        activityRecord = activity3;
                                        z = callerFg3;
                                        origId = origId2;
                                        Binder.restoreCallingIdentity(origId);
                                        throw clist;
                                    }
                                    try {
                                        if (!b2.intent.requested) {
                                            requestServiceBindingLocked(s, b2.intent, callerFg, false);
                                        }
                                        getServiceMapLocked(s.userId).ensureNotStartingBackgroundLocked(s);
                                        Binder.restoreCallingIdentity(origId);
                                        app = s.app;
                                        app = this.mAm.getProcessRecordLocked(s.processName, s.appInfo.uid, true);
                                        if (app != null) {
                                        }
                                        return 1;
                                    } catch (Throwable th11) {
                                        clist = th11;
                                        z = callerFg;
                                        Binder.restoreCallingIdentity(origId);
                                        throw clist;
                                    }
                                } catch (Throwable th12) {
                                    clist = th12;
                                    intent = service4;
                                    z4 = isBindExternal;
                                    z5 = allowInstant;
                                    activityRecord = activity3;
                                    z = callerFg3;
                                    origId = origId4;
                                    Binder.restoreCallingIdentity(origId);
                                    throw clist;
                                }
                            }
                        }
                    }
                    origId4 = origId3;
                    try {
                        this.mAm.startAssociationLocked(callerApp.uid, callerApp.processName, callerApp.curProcState, s.appInfo.uid, s.name, s.processName);
                        service4 = service3;
                        this.mAm.grantEphemeralAccessLocked(callerApp.userId, service4, s.appInfo.uid, UserHandle.getAppId(callerApp.uid));
                        this.mAm.mHwAMSEx.reportServiceRelationIAware(1, s, callerApp);
                        b = s.retrieveAppBindingLocked(service4, callerApp);
                        connectionRecord = connectionRecord;
                        z4 = isBindExternal;
                        isBindExternal = true;
                        b2 = b;
                        origId2 = origId4;
                        z5 = allowInstant;
                        allowInstant = service4;
                        c = new ConnectionRecord(b, activity3, connection, flags, clientLabel2, clientIntent2);
                        binder = connection.asBinder();
                        clist2 = (ArrayList) s.connections.get(binder);
                        if (clist2 == null) {
                        }
                        clist2.add(c);
                        b2.connections.add(c);
                        activity4 = activity3;
                        if (activity4 != null) {
                        }
                    } catch (Throwable th13) {
                        clist = th13;
                        z4 = isBindExternal;
                        z5 = allowInstant;
                        intent = service3;
                        activityRecord = activity3;
                        z = callerFg3;
                        origId = origId4;
                        Binder.restoreCallingIdentity(origId);
                        throw clist;
                    }
                    try {
                        b2.client.connections.add(c);
                        connectionSize = s.connections.size();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("bindServiceLocked ");
                        stringBuilder.append(s);
                        stringBuilder.append(",connection size= ");
                        stringBuilder.append(connectionSize);
                        stringBuilder.append(",callingPackage= ");
                        stringBuilder.append(str3);
                        Flog.i(102, stringBuilder.toString());
                        if ((c.flags & 8) != 0) {
                        }
                        if ((c.flags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0) {
                        }
                        if (s.app != null) {
                        }
                        clist2 = (ArrayList) this.mServiceConnections.get(binder);
                        if (clist2 == null) {
                        }
                        clist3 = clist2;
                        clist3.add(c);
                        if ((flags & 1) == 0) {
                        }
                        if (s.app != null) {
                        }
                        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                        }
                        if (s.app != null) {
                        }
                        callerFg = callerFg3;
                        if (b2.intent.requested) {
                        }
                        getServiceMapLocked(s.userId).ensureNotStartingBackgroundLocked(s);
                        Binder.restoreCallingIdentity(origId);
                        app = s.app;
                        app = this.mAm.getProcessRecordLocked(s.processName, s.appInfo.uid, true);
                        if (app != null) {
                        }
                        return 1;
                    } catch (Throwable th14) {
                        clist = th14;
                        activityRecord = activity4;
                        obj = allowInstant;
                        z = callerFg3;
                        origId = origId2;
                        Binder.restoreCallingIdentity(origId);
                        throw clist;
                    }
                } catch (Throwable th15) {
                    clist = th15;
                    origId = origId3;
                    z4 = isBindExternal;
                    z5 = allowInstant;
                    intent = service3;
                    activityRecord = activity3;
                    z = callerFg3;
                    Binder.restoreCallingIdentity(origId);
                    throw clist;
                }
            } else {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Non-system caller ");
                stringBuilder3.append(iApplicationThread);
                stringBuilder3.append(" (pid=");
                stringBuilder3.append(Binder.getCallingPid());
                stringBuilder3.append(") set BIND_ALLOW_INSTANT when binding service ");
                stringBuilder3.append(service2);
                throw new SecurityException(stringBuilder3.toString());
            }
        }
        i = userId;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unable to find app for caller ");
        stringBuilder.append(iApplicationThread);
        stringBuilder.append(" (pid=");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(") when binding service ");
        stringBuilder.append(intent2);
        throw new SecurityException(stringBuilder.toString());
    }

    void publishServiceLocked(ServiceRecord r, Intent intent, IBinder service) {
        ServiceRecord serviceRecord = r;
        Object obj = intent;
        IBinder iBinder = service;
        long origId = Binder.clearCallingIdentity();
        FilterComparison filter;
        ConnectionRecord c;
        try {
            String str;
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PUBLISHING ");
                stringBuilder.append(serviceRecord);
                stringBuilder.append(" ");
                stringBuilder.append(obj);
                stringBuilder.append(": ");
                stringBuilder.append(iBinder);
                Slog.v(str, stringBuilder.toString());
            }
            if (serviceRecord != null) {
                filter = new FilterComparison(obj);
                IntentBindRecord b = (IntentBindRecord) serviceRecord.bindings.get(filter);
                IntentBindRecord intentBindRecord;
                if (b == null || b.received) {
                    intentBindRecord = b;
                } else {
                    StringBuilder stringBuilder2;
                    b.binder = iBinder;
                    b.requested = true;
                    b.received = true;
                    int connectionSize = serviceRecord.connections.size();
                    if (ActivityManagerDebugConfig.HWFLOW && ((long) connectionSize) > SERVICE_CONNECTIONS_THRESHOLD) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("publishServiceLocked ");
                        stringBuilder2.append(serviceRecord);
                        stringBuilder2.append(",connection size= ");
                        stringBuilder2.append(connectionSize);
                        Flog.i(102, stringBuilder2.toString());
                    }
                    long start = SystemClock.uptimeMillis();
                    int conni = connectionSize - 1;
                    while (true) {
                        int conni2 = conni;
                        if (conni2 < 0) {
                            break;
                        }
                        Intent intent2;
                        ArrayList<ConnectionRecord> clist = (ArrayList) serviceRecord.connections.valueAt(conni2);
                        conni = 0;
                        while (true) {
                            int i = conni;
                            if (i >= clist.size()) {
                                break;
                            }
                            FilterComparison filter2;
                            c = (ConnectionRecord) clist.get(i);
                            if (filter.equals(c.binding.intent.intent)) {
                                filter2 = filter;
                                intentBindRecord = b;
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    str = ActivityManagerService.TAG;
                                    filter = new StringBuilder();
                                    filter.append("Publishing to: ");
                                    filter.append(c);
                                    Slog.v(str, filter.toString());
                                }
                                c.conn.connected(serviceRecord.name, iBinder, false);
                            } else {
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    str = ActivityManagerService.TAG;
                                    filter2 = filter;
                                    filter = new StringBuilder();
                                    intentBindRecord = b;
                                    filter.append("Not publishing to: ");
                                    filter.append(c);
                                    Slog.v(str, filter.toString());
                                } else {
                                    filter2 = filter;
                                    intentBindRecord = b;
                                }
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    str = ActivityManagerService.TAG;
                                    filter = new StringBuilder();
                                    filter.append("Bound intent: ");
                                    filter.append(c.binding.intent.intent);
                                    Slog.v(str, filter.toString());
                                }
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    str = ActivityManagerService.TAG;
                                    filter = new StringBuilder();
                                    filter.append("Published intent: ");
                                    filter.append(obj);
                                    Slog.v(str, filter.toString());
                                }
                            }
                            conni = i + 1;
                            filter = filter2;
                            b = intentBindRecord;
                            intent2 = intent;
                        }
                        intentBindRecord = b;
                        conni = conni2 - 1;
                        intent2 = intent;
                    }
                    intentBindRecord = b;
                    filter = SystemClock.uptimeMillis();
                    if (ActivityManagerDebugConfig.HWFLOW && filter - start > 1000) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("publishServiceLocked ");
                        stringBuilder2.append(serviceRecord);
                        stringBuilder2.append(",took ");
                        stringBuilder2.append(filter - start);
                        stringBuilder2.append("ms");
                        Flog.i(102, stringBuilder2.toString());
                    }
                }
                serviceDoneExecutingLocked(serviceRecord, this.mDestroyingServices.contains(serviceRecord), false);
            }
            Binder.restoreCallingIdentity(origId);
        } catch (Exception e) {
            filter = ActivityManagerService.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Failure sending service ");
            stringBuilder3.append(serviceRecord.name);
            stringBuilder3.append(" to connection ");
            stringBuilder3.append(c.conn.asBinder());
            stringBuilder3.append(" (in ");
            stringBuilder3.append(c.binding.client.processName);
            stringBuilder3.append(")");
            Slog.w(filter, stringBuilder3.toString(), e);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
        }
    }

    boolean unbindServiceLocked(IServiceConnection connection) {
        IBinder binder = connection.asBinder();
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unbindService: conn=");
            stringBuilder.append(binder);
            Slog.v(str, stringBuilder.toString());
        }
        ArrayList<ConnectionRecord> clist = (ArrayList) this.mServiceConnections.get(binder);
        if (clist == null) {
            String str2 = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unbind failed: could not find connection for ");
            stringBuilder2.append(connection.asBinder());
            Slog.w(str2, stringBuilder2.toString());
            return false;
        }
        boolean z;
        long origId = Binder.clearCallingIdentity();
        while (true) {
            try {
                z = true;
                if (clist.size() <= 0) {
                    break;
                }
                ConnectionRecord r = (ConnectionRecord) clist.get(0);
                removeConnectionLocked(r, null, null);
                if (clist.size() > 0 && clist.get(0) == r) {
                    String str3 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Connection ");
                    stringBuilder3.append(r);
                    stringBuilder3.append(" not removed for binder ");
                    stringBuilder3.append(binder);
                    Slog.wtf(str3, stringBuilder3.toString());
                    clist.remove(0);
                }
                if (r.binding.service.app != null) {
                    if (r.binding.service.app.whitelistManager) {
                        updateWhitelistManagerLocked(r.binding.service.app);
                    }
                    if ((r.flags & 134217728) != 0) {
                        r.binding.service.app.treatLikeActivity = true;
                        ActivityManagerService activityManagerService = this.mAm;
                        ProcessRecord processRecord = r.binding.service.app;
                        if (!r.binding.service.app.hasClientActivities) {
                            if (!r.binding.service.app.treatLikeActivity) {
                                z = false;
                            }
                        }
                        activityManagerService.updateLruProcessLocked(processRecord, z, null);
                    }
                    z = this.mAm;
                    z.updateOomAdjLocked(r.binding.service.app, false);
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
        this.mAm.updateOomAdjLocked();
        return z;
    }

    void unbindFinishedLocked(ServiceRecord r, Intent intent, boolean doRebind) {
        long origId = Binder.clearCallingIdentity();
        if (r != null) {
            try {
                IntentBindRecord b = (IntentBindRecord) r.bindings.get(new FilterComparison(intent));
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unbindFinished in ");
                    stringBuilder.append(r);
                    stringBuilder.append(" at ");
                    stringBuilder.append(b);
                    stringBuilder.append(": apps=");
                    stringBuilder.append(b != null ? b.apps.size() : 0);
                    Slog.v(str, stringBuilder.toString());
                }
                boolean inDestroying = this.mDestroyingServices.contains(r);
                if (b != null) {
                    if (b.apps.size() <= 0 || inDestroying) {
                        b.doRebind = true;
                    } else {
                        boolean inFg = false;
                        for (int i = b.apps.size() - 1; i >= 0; i--) {
                            ProcessRecord client = ((AppBindRecord) b.apps.valueAt(i)).client;
                            if (client != null && client.setSchedGroup != 0) {
                                inFg = true;
                                break;
                            }
                        }
                        try {
                            requestServiceBindingLocked(r, b, inFg, true);
                        } catch (TransactionTooLargeException e) {
                        }
                    }
                }
                serviceDoneExecutingLocked(r, inDestroying, false);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(origId);
            }
        }
        Binder.restoreCallingIdentity(origId);
    }

    private final ServiceRecord findServiceLocked(ComponentName name, IBinder token, int userId) {
        IBinder r = getServiceByNameLocked(name, userId);
        return r == token ? r : null;
    }

    /*  JADX ERROR: JadxRuntimeException in pass: EliminatePhiNodes
        jadx.core.utils.exceptions.JadxRuntimeException: Assign predecessor not found for B:34:0x00e4 from B:207:?
        	at jadx.core.dex.visitors.ssa.EliminatePhiNodes.replaceMerge(EliminatePhiNodes.java:102)
        	at jadx.core.dex.visitors.ssa.EliminatePhiNodes.replaceMergeInstructions(EliminatePhiNodes.java:68)
        	at jadx.core.dex.visitors.ssa.EliminatePhiNodes.visit(EliminatePhiNodes.java:31)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private com.android.server.am.ActiveServices.ServiceLookupResult retrieveServiceLocked(android.content.Intent r30, java.lang.String r31, java.lang.String r32, int r33, int r34, int r35, boolean r36, boolean r37, boolean r38, boolean r39) {
        /*
        r29 = this;
        r1 = r29;
        r15 = r30;
        r14 = r32;
        r13 = r33;
        r11 = r34;
        r10 = 0;
        r0 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_SERVICE;
        if (r0 == 0) goto L_0x0039;
    L_0x000f:
        r0 = "ActivityManager";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "retrieveServiceLocked: ";
        r2.append(r3);
        r2.append(r15);
        r3 = " type=";
        r2.append(r3);
        r12 = r31;
        r2.append(r12);
        r3 = " callingUid=";
        r2.append(r3);
        r2.append(r11);
        r2 = r2.toString();
        android.util.Slog.v(r0, r2);
        goto L_0x003b;
    L_0x0039:
        r12 = r31;
    L_0x003b:
        r0 = r1.mAm;
        r2 = r0.mUserController;
        r6 = 0;
        r7 = 1;
        r8 = "service";
        r9 = 0;
        r3 = r13;
        r4 = r11;
        r5 = r35;
        r9 = r2.handleIncomingUser(r3, r4, r5, r6, r7, r8, r9);
        r0 = r1.mAm;
        r2 = r0.mPidsSelfLocked;
        monitor-enter(r2);
        r0 = r1.mAm;	 Catch:{ all -> 0x053f }
        r0 = r0.mPidsSelfLocked;	 Catch:{ all -> 0x053f }
        r0 = r0.get(r13);	 Catch:{ all -> 0x053f }
        r0 = (com.android.server.am.ProcessRecord) r0;	 Catch:{ all -> 0x053f }
        r12 = r0;	 Catch:{ all -> 0x053f }
        monitor-exit(r2);	 Catch:{ all -> 0x053f }
        r8 = r1.getServiceMapLocked(r9);
        r7 = r30.getComponent();
        if (r7 == 0) goto L_0x008d;
    L_0x0068:
        r0 = r8.mServicesByName;
        r0 = r0.get(r7);
        r10 = r0;
        r10 = (com.android.server.am.ServiceRecord) r10;
        r0 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_SERVICE;
        if (r0 == 0) goto L_0x008d;
    L_0x0075:
        if (r10 == 0) goto L_0x008d;
    L_0x0077:
        r0 = "ActivityManager";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "Retrieved by component: ";
        r2.append(r3);
        r2.append(r10);
        r2 = r2.toString();
        android.util.Slog.v(r0, r2);
    L_0x008d:
        if (r10 != 0) goto L_0x00bb;
    L_0x008f:
        if (r38 != 0) goto L_0x00bb;
    L_0x0091:
        r0 = new android.content.Intent$FilterComparison;
        r0.<init>(r15);
        r2 = r8.mServicesByIntent;
        r2 = r2.get(r0);
        r10 = r2;
        r10 = (com.android.server.am.ServiceRecord) r10;
        r2 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_SERVICE;
        if (r2 == 0) goto L_0x00bb;
    L_0x00a3:
        if (r10 == 0) goto L_0x00bb;
    L_0x00a5:
        r2 = "ActivityManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "Retrieved by intent: ";
        r3.append(r4);
        r3.append(r10);
        r3 = r3.toString();
        android.util.Slog.v(r2, r3);
    L_0x00bb:
        if (r10 == 0) goto L_0x00d9;
    L_0x00bd:
        r0 = r10.serviceInfo;
        r0 = r0.flags;
        r0 = r0 & 4;
        if (r0 == 0) goto L_0x00d9;
    L_0x00c5:
        r0 = r10.packageName;
        r0 = r14.equals(r0);
        if (r0 != 0) goto L_0x00d9;
    L_0x00cd:
        r10 = 0;
        r0 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_SERVICE;
        if (r0 == 0) goto L_0x00d9;
    L_0x00d2:
        r0 = "ActivityManager";
        r2 = "Whoops, can't use existing external service";
        android.util.Slog.v(r0, r2);
    L_0x00d9:
        r6 = 0;
        if (r10 != 0) goto L_0x0388;
    L_0x00dc:
        r0 = 268436480; // 0x10000400 float:2.524663E-29 double:1.32625243E-315;
        if (r39 == 0) goto L_0x00e4;
    L_0x00e1:
        r2 = 8388608; // 0x800000 float:1.17549435E-38 double:4.144523E-317;
        r0 = r0 | r2;
    L_0x00e4:
        r16 = r0;
        r0 = r1.mAm;	 Catch:{ RemoteException -> 0x0376 }
        r2 = r0.getPackageManagerInternalLocked();	 Catch:{ RemoteException -> 0x0376 }
        r3 = r15;
        r4 = r31;
        r5 = r16;
        r6 = r9;
        r17 = r7;
        r7 = r11;
        r0 = r2.resolveService(r3, r4, r5, r6, r7);	 Catch:{ RemoteException -> 0x0370 }
        r2 = r0;
        if (r2 == 0) goto L_0x0103;
    L_0x00fc:
        r6 = r2.serviceInfo;	 Catch:{ RemoteException -> 0x00ff }
        goto L_0x0104;
    L_0x00ff:
        r0 = move-exception;
        r7 = r9;
        goto L_0x037d;
    L_0x0103:
        r6 = 0;
    L_0x0104:
        r0 = r6;
        if (r0 == 0) goto L_0x0126;
    L_0x0107:
        r3 = r1.mAm;	 Catch:{ RemoteException -> 0x0370 }
        r4 = 0;
        r18 = r8;
        r8 = r3;
        r7 = r9;
        r9 = r0;
        r6 = r10;
        r10 = r13;
        r5 = r11;
        r3 = r13;
        r13 = r4;
        r4 = r14;
        r14 = r15;
        r8 = r8.shouldPreventStartService(r9, r10, r11, r12, r13, r14);	 Catch:{ RemoteException -> 0x0120 }
        if (r8 == 0) goto L_0x011e;	 Catch:{ RemoteException -> 0x0120 }
    L_0x011c:
        r9 = 0;	 Catch:{ RemoteException -> 0x0120 }
        return r9;	 Catch:{ RemoteException -> 0x0120 }
    L_0x011e:
        r9 = 0;	 Catch:{ RemoteException -> 0x0120 }
        goto L_0x012e;	 Catch:{ RemoteException -> 0x0120 }
    L_0x0120:
        r0 = move-exception;	 Catch:{ RemoteException -> 0x0120 }
        r10 = r6;	 Catch:{ RemoteException -> 0x0120 }
    L_0x0122:
        r8 = r18;	 Catch:{ RemoteException -> 0x0120 }
        goto L_0x037d;	 Catch:{ RemoteException -> 0x0120 }
    L_0x0126:
        r18 = r8;	 Catch:{ RemoteException -> 0x0120 }
        r7 = r9;	 Catch:{ RemoteException -> 0x0120 }
        r6 = r10;	 Catch:{ RemoteException -> 0x0120 }
        r5 = r11;	 Catch:{ RemoteException -> 0x0120 }
        r3 = r13;	 Catch:{ RemoteException -> 0x0120 }
        r4 = r14;	 Catch:{ RemoteException -> 0x0120 }
        r9 = 0;	 Catch:{ RemoteException -> 0x0120 }
    L_0x012e:
        if (r0 != 0) goto L_0x0154;	 Catch:{ RemoteException -> 0x0120 }
    L_0x0130:
        r8 = "ActivityManager";	 Catch:{ RemoteException -> 0x0120 }
        r10 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0120 }
        r10.<init>();	 Catch:{ RemoteException -> 0x0120 }
        r11 = "Unable to start service ";	 Catch:{ RemoteException -> 0x0120 }
        r10.append(r11);	 Catch:{ RemoteException -> 0x0120 }
        r10.append(r15);	 Catch:{ RemoteException -> 0x0120 }
        r11 = " U=";	 Catch:{ RemoteException -> 0x0120 }
        r10.append(r11);	 Catch:{ RemoteException -> 0x0120 }
        r10.append(r7);	 Catch:{ RemoteException -> 0x0120 }
        r11 = ": not found";	 Catch:{ RemoteException -> 0x0120 }
        r10.append(r11);	 Catch:{ RemoteException -> 0x0120 }
        r10 = r10.toString();	 Catch:{ RemoteException -> 0x0120 }
        android.util.Slog.w(r8, r10);	 Catch:{ RemoteException -> 0x0120 }
        return r9;	 Catch:{ RemoteException -> 0x0120 }
    L_0x0154:
        r8 = new android.content.ComponentName;	 Catch:{ RemoteException -> 0x0120 }
        r10 = r0.applicationInfo;	 Catch:{ RemoteException -> 0x0120 }
        r10 = r10.packageName;	 Catch:{ RemoteException -> 0x0120 }
        r11 = r0.name;	 Catch:{ RemoteException -> 0x0120 }
        r8.<init>(r10, r11);	 Catch:{ RemoteException -> 0x0120 }
        r10 = r0.flags;	 Catch:{ RemoteException -> 0x0120 }
        r10 = r10 & 4;	 Catch:{ RemoteException -> 0x0120 }
        if (r10 == 0) goto L_0x020e;	 Catch:{ RemoteException -> 0x0120 }
    L_0x0165:
        if (r38 == 0) goto L_0x01f7;	 Catch:{ RemoteException -> 0x0120 }
    L_0x0167:
        r10 = r0.exported;	 Catch:{ RemoteException -> 0x0120 }
        if (r10 == 0) goto L_0x01db;	 Catch:{ RemoteException -> 0x0120 }
    L_0x016b:
        r10 = r0.flags;	 Catch:{ RemoteException -> 0x0120 }
        r10 = r10 & 2;	 Catch:{ RemoteException -> 0x0120 }
        if (r10 == 0) goto L_0x01bf;	 Catch:{ RemoteException -> 0x0120 }
    L_0x0171:
        r10 = android.app.AppGlobals.getPackageManager();	 Catch:{ RemoteException -> 0x0120 }
        r11 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;	 Catch:{ RemoteException -> 0x0120 }
        r10 = r10.getApplicationInfo(r4, r11, r7);	 Catch:{ RemoteException -> 0x0120 }
        if (r10 == 0) goto L_0x01a8;	 Catch:{ RemoteException -> 0x0120 }
    L_0x017d:
        r11 = new android.content.pm.ServiceInfo;	 Catch:{ RemoteException -> 0x0120 }
        r11.<init>(r0);	 Catch:{ RemoteException -> 0x0120 }
        r0 = r11;	 Catch:{ RemoteException -> 0x0120 }
        r11 = new android.content.pm.ApplicationInfo;	 Catch:{ RemoteException -> 0x0120 }
        r13 = r0.applicationInfo;	 Catch:{ RemoteException -> 0x0120 }
        r11.<init>(r13);	 Catch:{ RemoteException -> 0x0120 }
        r0.applicationInfo = r11;	 Catch:{ RemoteException -> 0x0120 }
        r11 = r0.applicationInfo;	 Catch:{ RemoteException -> 0x0120 }
        r13 = r10.packageName;	 Catch:{ RemoteException -> 0x0120 }
        r11.packageName = r13;	 Catch:{ RemoteException -> 0x0120 }
        r11 = r0.applicationInfo;	 Catch:{ RemoteException -> 0x0120 }
        r13 = r10.uid;	 Catch:{ RemoteException -> 0x0120 }
        r11.uid = r13;	 Catch:{ RemoteException -> 0x0120 }
        r11 = new android.content.ComponentName;	 Catch:{ RemoteException -> 0x0120 }
        r13 = r10.packageName;	 Catch:{ RemoteException -> 0x0120 }
        r14 = r8.getClassName();	 Catch:{ RemoteException -> 0x0120 }
        r11.<init>(r13, r14);	 Catch:{ RemoteException -> 0x0120 }
        r8 = r11;	 Catch:{ RemoteException -> 0x0120 }
        r15.setComponent(r8);	 Catch:{ RemoteException -> 0x0120 }
        goto L_0x0210;	 Catch:{ RemoteException -> 0x0120 }
    L_0x01a8:
        r11 = new java.lang.SecurityException;	 Catch:{ RemoteException -> 0x0120 }
        r13 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0120 }
        r13.<init>();	 Catch:{ RemoteException -> 0x0120 }
        r14 = "BIND_EXTERNAL_SERVICE failed, could not resolve client package ";	 Catch:{ RemoteException -> 0x0120 }
        r13.append(r14);	 Catch:{ RemoteException -> 0x0120 }
        r13.append(r4);	 Catch:{ RemoteException -> 0x0120 }
        r13 = r13.toString();	 Catch:{ RemoteException -> 0x0120 }
        r11.<init>(r13);	 Catch:{ RemoteException -> 0x0120 }
        throw r11;	 Catch:{ RemoteException -> 0x0120 }
    L_0x01bf:
        r10 = new java.lang.SecurityException;	 Catch:{ RemoteException -> 0x0120 }
        r11 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0120 }
        r11.<init>();	 Catch:{ RemoteException -> 0x0120 }
        r13 = "BIND_EXTERNAL_SERVICE failed, ";	 Catch:{ RemoteException -> 0x0120 }
        r11.append(r13);	 Catch:{ RemoteException -> 0x0120 }
        r11.append(r8);	 Catch:{ RemoteException -> 0x0120 }
        r13 = " is not an isolatedProcess";	 Catch:{ RemoteException -> 0x0120 }
        r11.append(r13);	 Catch:{ RemoteException -> 0x0120 }
        r11 = r11.toString();	 Catch:{ RemoteException -> 0x0120 }
        r10.<init>(r11);	 Catch:{ RemoteException -> 0x0120 }
        throw r10;	 Catch:{ RemoteException -> 0x0120 }
    L_0x01db:
        r10 = new java.lang.SecurityException;	 Catch:{ RemoteException -> 0x0120 }
        r11 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0120 }
        r11.<init>();	 Catch:{ RemoteException -> 0x0120 }
        r13 = "BIND_EXTERNAL_SERVICE failed, ";	 Catch:{ RemoteException -> 0x0120 }
        r11.append(r13);	 Catch:{ RemoteException -> 0x0120 }
        r11.append(r8);	 Catch:{ RemoteException -> 0x0120 }
        r13 = " is not exported";	 Catch:{ RemoteException -> 0x0120 }
        r11.append(r13);	 Catch:{ RemoteException -> 0x0120 }
        r11 = r11.toString();	 Catch:{ RemoteException -> 0x0120 }
        r10.<init>(r11);	 Catch:{ RemoteException -> 0x0120 }
        throw r10;	 Catch:{ RemoteException -> 0x0120 }
    L_0x01f7:
        r10 = new java.lang.SecurityException;	 Catch:{ RemoteException -> 0x0120 }
        r11 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0120 }
        r11.<init>();	 Catch:{ RemoteException -> 0x0120 }
        r13 = "BIND_EXTERNAL_SERVICE required for ";	 Catch:{ RemoteException -> 0x0120 }
        r11.append(r13);	 Catch:{ RemoteException -> 0x0120 }
        r11.append(r8);	 Catch:{ RemoteException -> 0x0120 }
        r11 = r11.toString();	 Catch:{ RemoteException -> 0x0120 }
        r10.<init>(r11);	 Catch:{ RemoteException -> 0x0120 }
        throw r10;	 Catch:{ RemoteException -> 0x0120 }
    L_0x020e:
        if (r38 != 0) goto L_0x0352;	 Catch:{ RemoteException -> 0x0120 }
    L_0x0210:
        if (r7 <= 0) goto L_0x0252;	 Catch:{ RemoteException -> 0x0120 }
    L_0x0212:
        r10 = r1.mAm;	 Catch:{ RemoteException -> 0x0120 }
        r11 = r0.processName;	 Catch:{ RemoteException -> 0x0120 }
        r13 = r0.applicationInfo;	 Catch:{ RemoteException -> 0x0120 }
        r14 = r0.name;	 Catch:{ RemoteException -> 0x0120 }
        r9 = r0.flags;	 Catch:{ RemoteException -> 0x0120 }
        r9 = r10.isSingleton(r11, r13, r14, r9);	 Catch:{ RemoteException -> 0x0120 }
        if (r9 == 0) goto L_0x0237;	 Catch:{ RemoteException -> 0x0120 }
    L_0x0222:
        r9 = r1.mAm;	 Catch:{ RemoteException -> 0x0120 }
        r10 = r0.applicationInfo;	 Catch:{ RemoteException -> 0x0120 }
        r10 = r10.uid;	 Catch:{ RemoteException -> 0x0120 }
        r9 = r9.isValidSingletonCall(r5, r10);	 Catch:{ RemoteException -> 0x0120 }
        if (r9 == 0) goto L_0x0237;
    L_0x022e:
        r9 = 0;
        r7 = 0;
        r7 = r1.getServiceMapLocked(r7);	 Catch:{ RemoteException -> 0x024d }
        r18 = r7;	 Catch:{ RemoteException -> 0x024d }
        goto L_0x0238;	 Catch:{ RemoteException -> 0x024d }
    L_0x0237:
        r9 = r7;	 Catch:{ RemoteException -> 0x024d }
    L_0x0238:
        r7 = new android.content.pm.ServiceInfo;	 Catch:{ RemoteException -> 0x024d }
        r7.<init>(r0);	 Catch:{ RemoteException -> 0x024d }
        r0 = r7;	 Catch:{ RemoteException -> 0x024d }
        r7 = r1.mAm;	 Catch:{ RemoteException -> 0x024d }
        r10 = r0.applicationInfo;	 Catch:{ RemoteException -> 0x024d }
        r7 = r7.getAppInfoForUser(r10, r9);	 Catch:{ RemoteException -> 0x024d }
        r0.applicationInfo = r7;	 Catch:{ RemoteException -> 0x024d }
        r10 = r9;
        r7 = r18;
        r9 = r0;
        goto L_0x0256;
    L_0x024d:
        r0 = move-exception;
        r10 = r6;
        r7 = r9;
        goto L_0x0122;
    L_0x0252:
        r9 = r0;
        r10 = r7;
        r7 = r18;
    L_0x0256:
        r0 = r7.mServicesByName;	 Catch:{ RemoteException -> 0x034d }
        r0 = r0.get(r8);	 Catch:{ RemoteException -> 0x034d }
        r0 = (com.android.server.am.ServiceRecord) r0;	 Catch:{ RemoteException -> 0x034d }
        r6 = r0;	 Catch:{ RemoteException -> 0x034d }
        r0 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_SERVICE;	 Catch:{ RemoteException -> 0x034d }
        if (r0 == 0) goto L_0x027b;	 Catch:{ RemoteException -> 0x034d }
    L_0x0263:
        if (r6 == 0) goto L_0x027b;	 Catch:{ RemoteException -> 0x034d }
    L_0x0265:
        r0 = "ActivityManager";	 Catch:{ RemoteException -> 0x034d }
        r11 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x034d }
        r11.<init>();	 Catch:{ RemoteException -> 0x034d }
        r13 = "Retrieved via pm by intent: ";	 Catch:{ RemoteException -> 0x034d }
        r11.append(r13);	 Catch:{ RemoteException -> 0x034d }
        r11.append(r6);	 Catch:{ RemoteException -> 0x034d }
        r11 = r11.toString();	 Catch:{ RemoteException -> 0x034d }
        android.util.Slog.v(r0, r11);	 Catch:{ RemoteException -> 0x034d }
    L_0x027b:
        if (r6 != 0) goto L_0x0340;	 Catch:{ RemoteException -> 0x034d }
    L_0x027d:
        if (r36 == 0) goto L_0x0340;	 Catch:{ RemoteException -> 0x034d }
    L_0x027f:
        r0 = new android.content.Intent$FilterComparison;	 Catch:{ RemoteException -> 0x034d }
        r11 = r30.cloneFilter();	 Catch:{ RemoteException -> 0x034d }
        r0.<init>(r11);	 Catch:{ RemoteException -> 0x034d }
        r11 = r0;	 Catch:{ RemoteException -> 0x034d }
        r0 = new com.android.server.am.ActiveServices$ServiceRestarter;	 Catch:{ RemoteException -> 0x034d }
        r13 = 0;	 Catch:{ RemoteException -> 0x034d }
        r0.<init>(r1, r13);	 Catch:{ RemoteException -> 0x034d }
        r13 = r0;	 Catch:{ RemoteException -> 0x034d }
        r0 = r1.mAm;	 Catch:{ RemoteException -> 0x034d }
        r0 = r0.mBatteryStatsService;	 Catch:{ RemoteException -> 0x034d }
        r0 = r0.getActiveStatistics();	 Catch:{ RemoteException -> 0x034d }
        r14 = r0;	 Catch:{ RemoteException -> 0x034d }
        monitor-enter(r14);	 Catch:{ RemoteException -> 0x034d }
        r0 = r9.applicationInfo;	 Catch:{ all -> 0x0339 }
        r0 = r0.uid;	 Catch:{ all -> 0x0339 }
        r27 = r2;
        r2 = r9.packageName;	 Catch:{ all -> 0x033e }
        r3 = r9.name;	 Catch:{ all -> 0x033e }
        r21 = r14.getServiceStatsLocked(r0, r2, r3);	 Catch:{ all -> 0x033e }
        monitor-exit(r14);	 Catch:{ all -> 0x033e }
        r0 = new com.android.server.am.ServiceRecord;	 Catch:{ RemoteException -> 0x034d }
        r2 = r1.mAm;	 Catch:{ RemoteException -> 0x034d }
        r19 = r0;	 Catch:{ RemoteException -> 0x034d }
        r20 = r2;	 Catch:{ RemoteException -> 0x034d }
        r22 = r8;	 Catch:{ RemoteException -> 0x034d }
        r23 = r11;	 Catch:{ RemoteException -> 0x034d }
        r24 = r9;	 Catch:{ RemoteException -> 0x034d }
        r25 = r37;	 Catch:{ RemoteException -> 0x034d }
        r26 = r13;	 Catch:{ RemoteException -> 0x034d }
        r19.<init>(r20, r21, r22, r23, r24, r25, r26);	 Catch:{ RemoteException -> 0x034d }
        r2 = r0;
        r13.setService(r2);	 Catch:{ RemoteException -> 0x0334 }
        r0 = r7.mServicesByName;	 Catch:{ RemoteException -> 0x0334 }
        r0.put(r8, r2);	 Catch:{ RemoteException -> 0x0334 }
        r0 = r7.mServicesByIntent;	 Catch:{ RemoteException -> 0x0334 }
        r0.put(r11, r2);	 Catch:{ RemoteException -> 0x0334 }
        r0 = r1.mPendingServices;	 Catch:{ RemoteException -> 0x0334 }
        r0 = r0.size();	 Catch:{ RemoteException -> 0x0334 }
        r0 = r0 + -1;	 Catch:{ RemoteException -> 0x0334 }
    L_0x02d4:
        if (r0 < 0) goto L_0x0318;	 Catch:{ RemoteException -> 0x0334 }
    L_0x02d6:
        r3 = r1.mPendingServices;	 Catch:{ RemoteException -> 0x0334 }
        r3 = r3.get(r0);	 Catch:{ RemoteException -> 0x0334 }
        r3 = (com.android.server.am.ServiceRecord) r3;	 Catch:{ RemoteException -> 0x0334 }
        r6 = r3.serviceInfo;	 Catch:{ RemoteException -> 0x0334 }
        r6 = r6.applicationInfo;	 Catch:{ RemoteException -> 0x0334 }
        r6 = r6.uid;	 Catch:{ RemoteException -> 0x0334 }
        r4 = r9.applicationInfo;	 Catch:{ RemoteException -> 0x0334 }
        r4 = r4.uid;	 Catch:{ RemoteException -> 0x0334 }
        if (r6 != r4) goto L_0x0311;	 Catch:{ RemoteException -> 0x0334 }
    L_0x02ea:
        r4 = r3.name;	 Catch:{ RemoteException -> 0x0334 }
        r4 = r4.equals(r8);	 Catch:{ RemoteException -> 0x0334 }
        if (r4 == 0) goto L_0x0311;	 Catch:{ RemoteException -> 0x0334 }
    L_0x02f2:
        r4 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_SERVICE;	 Catch:{ RemoteException -> 0x0334 }
        if (r4 == 0) goto L_0x030c;	 Catch:{ RemoteException -> 0x0334 }
    L_0x02f6:
        r4 = "ActivityManager";	 Catch:{ RemoteException -> 0x0334 }
        r6 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0334 }
        r6.<init>();	 Catch:{ RemoteException -> 0x0334 }
        r5 = "Remove pending: ";	 Catch:{ RemoteException -> 0x0334 }
        r6.append(r5);	 Catch:{ RemoteException -> 0x0334 }
        r6.append(r3);	 Catch:{ RemoteException -> 0x0334 }
        r5 = r6.toString();	 Catch:{ RemoteException -> 0x0334 }
        android.util.Slog.v(r4, r5);	 Catch:{ RemoteException -> 0x0334 }
    L_0x030c:
        r4 = r1.mPendingServices;	 Catch:{ RemoteException -> 0x0334 }
        r4.remove(r0);	 Catch:{ RemoteException -> 0x0334 }
    L_0x0311:
        r0 = r0 + -1;	 Catch:{ RemoteException -> 0x0334 }
        r4 = r32;	 Catch:{ RemoteException -> 0x0334 }
        r5 = r34;	 Catch:{ RemoteException -> 0x0334 }
        goto L_0x02d4;	 Catch:{ RemoteException -> 0x0334 }
    L_0x0318:
        r0 = com.android.server.am.ActivityManagerDebugConfig.DEBUG_SERVICE;	 Catch:{ RemoteException -> 0x0334 }
        if (r0 == 0) goto L_0x0332;	 Catch:{ RemoteException -> 0x0334 }
    L_0x031c:
        r0 = "ActivityManager";	 Catch:{ RemoteException -> 0x0334 }
        r3 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0334 }
        r3.<init>();	 Catch:{ RemoteException -> 0x0334 }
        r4 = "Retrieve created new service: ";	 Catch:{ RemoteException -> 0x0334 }
        r3.append(r4);	 Catch:{ RemoteException -> 0x0334 }
        r3.append(r2);	 Catch:{ RemoteException -> 0x0334 }
        r3 = r3.toString();	 Catch:{ RemoteException -> 0x0334 }
        android.util.Slog.v(r0, r3);	 Catch:{ RemoteException -> 0x0334 }
    L_0x0332:
        r6 = r2;
        goto L_0x0340;
    L_0x0334:
        r0 = move-exception;
        r8 = r7;
        r7 = r10;
        r10 = r2;
        goto L_0x037d;
    L_0x0339:
        r0 = move-exception;
        r27 = r2;
    L_0x033c:
        monitor-exit(r14);	 Catch:{ all -> 0x033e }
        throw r0;	 Catch:{ RemoteException -> 0x034d }
    L_0x033e:
        r0 = move-exception;
        goto L_0x033c;
        r18 = r7;
        r14 = r10;
        r9 = r33;
        r11 = r32;
        r13 = r34;
        r10 = r6;
        goto L_0x03b6;
    L_0x034d:
        r0 = move-exception;
        r8 = r7;
        r7 = r10;
        r10 = r6;
        goto L_0x037d;
    L_0x0352:
        r27 = r2;
        r2 = new java.lang.SecurityException;	 Catch:{ RemoteException -> 0x0120 }
        r3 = new java.lang.StringBuilder;	 Catch:{ RemoteException -> 0x0120 }
        r3.<init>();	 Catch:{ RemoteException -> 0x0120 }
        r4 = "BIND_EXTERNAL_SERVICE failed, ";	 Catch:{ RemoteException -> 0x0120 }
        r3.append(r4);	 Catch:{ RemoteException -> 0x0120 }
        r3.append(r8);	 Catch:{ RemoteException -> 0x0120 }
        r4 = " is not an externalService";	 Catch:{ RemoteException -> 0x0120 }
        r3.append(r4);	 Catch:{ RemoteException -> 0x0120 }
        r3 = r3.toString();	 Catch:{ RemoteException -> 0x0120 }
        r2.<init>(r3);	 Catch:{ RemoteException -> 0x0120 }
        throw r2;	 Catch:{ RemoteException -> 0x0120 }
    L_0x0370:
        r0 = move-exception;
        r18 = r8;
        r7 = r9;
        r6 = r10;
        goto L_0x037d;
    L_0x0376:
        r0 = move-exception;
        r17 = r7;
        r18 = r8;
        r7 = r9;
        r6 = r10;
        r14 = r7;
        r18 = r8;
        r9 = r33;
        r11 = r32;
        r13 = r34;
        goto L_0x03b6;
    L_0x0388:
        r17 = r7;
        r18 = r8;
        r7 = r9;
        r6 = r10;
        if (r36 == 0) goto L_0x03ae;
    L_0x0390:
        r0 = r6.serviceInfo;
        if (r0 == 0) goto L_0x03ae;
    L_0x0394:
        r2 = r1.mAm;
        r3 = r6.serviceInfo;
        r0 = 1;
        r9 = r33;
        r11 = r32;
        r4 = r9;
        r13 = r34;
        r5 = r13;
        r10 = r6;
        r6 = r12;
        r14 = r7;
        r7 = r0;
        r8 = r15;
        r0 = r2.shouldPreventStartService(r3, r4, r5, r6, r7, r8);
        if (r0 == 0) goto L_0x03b6;
    L_0x03ac:
        r2 = 0;
        return r2;
    L_0x03ae:
        r10 = r6;
        r14 = r7;
        r9 = r33;
        r11 = r32;
        r13 = r34;
    L_0x03b6:
        r0 = 102; // 0x66 float:1.43E-43 double:5.04E-322;
        if (r10 == 0) goto L_0x051b;
    L_0x03ba:
        r2 = r1.mAm;
        r3 = r10.permission;
        r4 = r10.appInfo;
        r6 = r4.uid;
        r7 = r10.exported;
        r4 = r9;
        r5 = r13;
        r2 = r2.checkComponentPermission(r3, r4, r5, r6, r7);
        if (r2 == 0) goto L_0x045c;
    L_0x03cc:
        r0 = r10.exported;
        if (r0 != 0) goto L_0x0421;
    L_0x03d0:
        r0 = "ActivityManager";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "Permission Denial: Accessing service ";
        r2.append(r3);
        r3 = r10.name;
        r2.append(r3);
        r3 = " from pid=";
        r2.append(r3);
        r2.append(r9);
        r3 = ", uid=";
        r2.append(r3);
        r2.append(r13);
        r3 = " that is not exported from uid ";
        r2.append(r3);
        r3 = r10.appInfo;
        r3 = r3.uid;
        r2.append(r3);
        r2 = r2.toString();
        android.util.Slog.w(r0, r2);
        r0 = new com.android.server.am.ActiveServices$ServiceLookupResult;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "not exported from uid ";
        r2.append(r3);
        r3 = r10.appInfo;
        r3 = r3.uid;
        r2.append(r3);
        r2 = r2.toString();
        r3 = 0;
        r0.<init>(r3, r2);
        return r0;
    L_0x0421:
        r0 = "ActivityManager";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "Permission Denial: Accessing service ";
        r2.append(r3);
        r3 = r10.name;
        r2.append(r3);
        r3 = " from pid=";
        r2.append(r3);
        r2.append(r9);
        r3 = ", uid=";
        r2.append(r3);
        r2.append(r13);
        r3 = " requires ";
        r2.append(r3);
        r3 = r10.permission;
        r2.append(r3);
        r2 = r2.toString();
        android.util.Slog.w(r0, r2);
        r0 = new com.android.server.am.ActiveServices$ServiceLookupResult;
        r2 = r10.permission;
        r3 = 0;
        r0.<init>(r3, r2);
        return r0;
    L_0x045c:
        r2 = r10.permission;
        if (r2 == 0) goto L_0x04ab;
    L_0x0460:
        if (r11 == 0) goto L_0x04ab;
    L_0x0462:
        r2 = r10.permission;
        r2 = android.app.AppOpsManager.permissionToOpCode(r2);
        r3 = -1;
        if (r2 == r3) goto L_0x04ab;
    L_0x046b:
        r3 = r1.mAm;
        r3 = r3.mAppOpsService;
        r3 = r3.noteOperation(r2, r13, r11);
        if (r3 == 0) goto L_0x04ab;
    L_0x0475:
        r0 = "ActivityManager";
        r3 = new java.lang.StringBuilder;
        r3.<init>();
        r4 = "Appop Denial: Accessing service ";
        r3.append(r4);
        r4 = r10.name;
        r3.append(r4);
        r4 = " from pid=";
        r3.append(r4);
        r3.append(r9);
        r4 = ", uid=";
        r3.append(r4);
        r3.append(r13);
        r4 = " requires appop ";
        r3.append(r4);
        r4 = android.app.AppOpsManager.opToName(r2);
        r3.append(r4);
        r3 = r3.toString();
        android.util.Slog.w(r0, r3);
        r3 = 0;
        return r3;
    L_0x04ab:
        r2 = r1.mAm;
        r2 = r2.mIntentFirewall;
        r3 = r10.name;
        r8 = r10.appInfo;
        r4 = r15;
        r5 = r13;
        r6 = r9;
        r7 = r31;
        r2 = r2.checkService(r3, r4, r5, r6, r7, r8);
        if (r2 != 0) goto L_0x04e2;
    L_0x04be:
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "prevent by firewall Unable to start service ";
        r2.append(r3);
        r2.append(r15);
        r3 = " U=";
        r2.append(r3);
        r2.append(r14);
        r3 = ": force null";
        r2.append(r3);
        r2 = r2.toString();
        android.util.Flog.w(r0, r2);
        r2 = 0;
        return r2;
    L_0x04e2:
        r2 = r1.mAm;
        r3 = r10.serviceInfo;
        r4 = r13;
        r5 = r9;
        r6 = r11;
        r7 = r14;
        r2 = r2.shouldPreventStartService(r3, r4, r5, r6, r7);
        if (r2 == 0) goto L_0x0514;
    L_0x04f0:
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "prevent by trustspace Unable to start service ";
        r2.append(r3);
        r2.append(r15);
        r3 = " U=";
        r2.append(r3);
        r2.append(r14);
        r3 = ": force null";
        r2.append(r3);
        r2 = r2.toString();
        android.util.Flog.w(r0, r2);
        r2 = 0;
        return r2;
    L_0x0514:
        r2 = 0;
        r0 = new com.android.server.am.ActiveServices$ServiceLookupResult;
        r0.<init>(r10, r2);
        return r0;
    L_0x051b:
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "retrieve service ";
        r2.append(r3);
        r2.append(r15);
        r3 = " U=";
        r2.append(r3);
        r2.append(r14);
        r3 = ": ret null";
        r2.append(r3);
        r2 = r2.toString();
        android.util.Flog.i(r0, r2);
        r2 = 0;
        return r2;
    L_0x053f:
        r0 = move-exception;
        r28 = r14;
        r14 = r9;
        r9 = r13;
        r13 = r11;
        r11 = r28;
    L_0x0547:
        monitor-exit(r2);	 Catch:{ all -> 0x0549 }
        throw r0;
    L_0x0549:
        r0 = move-exception;
        goto L_0x0547;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ActiveServices.retrieveServiceLocked(android.content.Intent, java.lang.String, java.lang.String, int, int, int, boolean, boolean, boolean, boolean):com.android.server.am.ActiveServices$ServiceLookupResult");
    }

    private final void bumpServiceExecutingLocked(ServiceRecord r, boolean fg, String why) {
        String str;
        StringBuilder stringBuilder;
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(">>> EXECUTING ");
            stringBuilder.append(why);
            stringBuilder.append(" of ");
            stringBuilder.append(r);
            stringBuilder.append(" in app ");
            stringBuilder.append(r.app);
            stringBuilder.append(", r.executeNesting: ");
            stringBuilder.append(r.executeNesting);
            stringBuilder.append(", fg: ");
            stringBuilder.append(fg);
            stringBuilder.append(", r.app.execServicesFg: ");
            int i = 0;
            stringBuilder.append(r.app == null ? false : r.app.execServicesFg);
            stringBuilder.append(", r.app.executingServices.size: ");
            if (r.app != null) {
                i = r.app.executingServices.size();
            }
            stringBuilder.append(i);
            Slog.v(str, stringBuilder.toString());
        } else if (ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(">>> EXECUTING ");
            stringBuilder.append(why);
            stringBuilder.append(" of ");
            stringBuilder.append(r.shortName);
            Slog.v(str, stringBuilder.toString());
        }
        boolean timeoutNeeded = true;
        if (this.mAm.mBootPhase < 600 && r.app != null && r.app.pid == Process.myPid()) {
            String str2 = ActivityManagerService.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Too early to start/bind service in system_server: Phase=");
            stringBuilder2.append(this.mAm.mBootPhase);
            stringBuilder2.append(" ");
            stringBuilder2.append(r.getComponentName());
            Slog.w(str2, stringBuilder2.toString());
            timeoutNeeded = false;
        }
        long now = SystemClock.uptimeMillis();
        if (r.executeNesting == 0) {
            r.executeFg = fg;
            ServiceState stracker = r.getTracker();
            if (stracker != null) {
                stracker.setExecuting(true, this.mAm.mProcessStats.getMemFactorLocked(), now);
            }
            if (r.app != null) {
                r.app.executingServices.add(r);
                ProcessRecord processRecord = r.app;
                processRecord.execServicesFg |= fg;
                if (timeoutNeeded && r.app.executingServices.size() == 1) {
                    scheduleServiceTimeoutLocked(r.app);
                }
            }
        } else if (!(r.app == null || !fg || r.app.execServicesFg)) {
            r.app.execServicesFg = true;
            if (timeoutNeeded) {
                scheduleServiceTimeoutLocked(r.app);
            }
        }
        r.executeFg |= fg;
        r.executeNesting++;
        r.executingStart = now;
    }

    private final boolean requestServiceBindingLocked(ServiceRecord r, IntentBindRecord i, boolean execInFg, boolean rebind) throws TransactionTooLargeException {
        if (r.app == null || r.app.thread == null) {
            return false;
        }
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("requestBind ");
            stringBuilder.append(i);
            stringBuilder.append(": requested=");
            stringBuilder.append(i.requested);
            stringBuilder.append(" rebind=");
            stringBuilder.append(rebind);
            Slog.d(str, stringBuilder.toString());
        }
        if ((!i.requested || rebind) && i.apps.size() > 0) {
            try {
                bumpServiceExecutingLocked(r, execInFg, "bind");
                r.app.forceProcessStateUpTo(9);
                r.app.thread.scheduleBindService(r, i.intent.getIntent(), rebind, r.app.repProcState);
                if (!rebind) {
                    i.requested = true;
                }
                i.hasBound = true;
                i.doRebind = false;
            } catch (TransactionTooLargeException e) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Crashed while binding ");
                    stringBuilder2.append(r);
                    Slog.v(ActivityManagerService.TAG, stringBuilder2.toString(), e);
                }
                boolean inDestroying = this.mDestroyingServices.contains(r);
                serviceDoneExecutingLocked(r, inDestroying, inDestroying);
                throw e;
            } catch (RemoteException e2) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    String str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Crashed while binding ");
                    stringBuilder3.append(r);
                    Slog.v(str2, stringBuilder3.toString());
                }
                boolean inDestroying2 = this.mDestroyingServices.contains(r);
                serviceDoneExecutingLocked(r, inDestroying2, inDestroying2);
                return false;
            }
        }
        return true;
    }

    private final boolean scheduleServiceRestartLocked(ServiceRecord r, boolean allowCancel) {
        ActiveServices activeServices = this;
        ServiceRecord serviceRecord = r;
        boolean z = allowCancel;
        boolean canceled = false;
        StringBuilder stringBuilder;
        if (activeServices.mAm.isShuttingDownLocked()) {
            String str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not scheduling restart of crashed service ");
            stringBuilder.append(serviceRecord.shortName);
            stringBuilder.append(" - system is shutting down");
            Slog.w(str, stringBuilder.toString());
            return false;
        }
        ServiceMap smap = activeServices.getServiceMapLocked(serviceRecord.userId);
        ServiceRecord cur;
        String str2;
        StringBuilder stringBuilder2;
        if (smap.mServicesByName.get(serviceRecord.name) != serviceRecord) {
            cur = (ServiceRecord) smap.mServicesByName.get(serviceRecord.name);
            str2 = ActivityManagerService.TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Attempting to schedule restart of ");
            stringBuilder2.append(serviceRecord);
            stringBuilder2.append(" when found in map: ");
            stringBuilder2.append(cur);
            Slog.wtf(str2, stringBuilder2.toString());
            return false;
        }
        long now;
        long now2 = SystemClock.uptimeMillis();
        int i = 3;
        if ((serviceRecord.serviceInfo.applicationInfo.flags & 8) == 0) {
            boolean canceled2;
            long now3;
            long minDuration = activeServices.mAm.mConstants.SERVICE_RESTART_DURATION;
            long resetTime = activeServices.mAm.mConstants.SERVICE_RESET_RUN_DURATION;
            int N = serviceRecord.deliveredStarts.size();
            if (N > 0) {
                ServiceMap smap2;
                int i2 = N - 1;
                while (true) {
                    int i3 = i2;
                    if (i3 < 0) {
                        break;
                    }
                    StartItem si = (StartItem) serviceRecord.deliveredStarts.get(i3);
                    si.removeUriPermissionsLocked();
                    if (si.intent != null) {
                        String str3;
                        StringBuilder stringBuilder3;
                        if (!z) {
                            canceled2 = canceled;
                        } else if (si.deliveryCount >= i || si.doneExecutingCount >= 6) {
                            str3 = ActivityManagerService.TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Canceling start item ");
                            stringBuilder3.append(si.intent);
                            stringBuilder3.append(" in service ");
                            stringBuilder3.append(serviceRecord.name);
                            Slog.w(str3, stringBuilder3.toString());
                            canceled = true;
                        } else {
                            canceled2 = canceled;
                        }
                        serviceRecord.pendingStarts.add(0, si);
                        smap2 = smap;
                        long resetTime2 = 2 * (SystemClock.uptimeMillis() - si.deliveredTime);
                        if (SERVICE_RESCHEDULE && DEBUG_DELAYED_SERVICE) {
                            str3 = ActivityManagerService.TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Can add more delay !!! si.deliveredTime ");
                            now3 = now2;
                            stringBuilder3.append(si.deliveredTime);
                            stringBuilder3.append(" dur ");
                            stringBuilder3.append(resetTime2);
                            stringBuilder3.append(" si.deliveryCount ");
                            stringBuilder3.append(si.deliveryCount);
                            stringBuilder3.append(" si.doneExecutingCount ");
                            stringBuilder3.append(si.doneExecutingCount);
                            stringBuilder3.append(" allowCancel ");
                            stringBuilder3.append(z);
                            Slog.w(str3, stringBuilder3.toString());
                        } else {
                            now3 = now2;
                        }
                        if (minDuration < resetTime2) {
                            minDuration = resetTime2;
                        }
                        if (resetTime < resetTime2) {
                            resetTime = resetTime2;
                        }
                        canceled = canceled2;
                        i2 = i3 - 1;
                        smap = smap2;
                        now2 = now3;
                        i = 3;
                    }
                    smap2 = smap;
                    now3 = now2;
                    i2 = i3 - 1;
                    smap = smap2;
                    now2 = now3;
                    i = 3;
                }
                canceled2 = canceled;
                smap2 = smap;
                now3 = now2;
                serviceRecord.deliveredStarts.clear();
            } else {
                now3 = now2;
                canceled2 = false;
            }
            serviceRecord.totalRestartCount++;
            if (SERVICE_RESCHEDULE && DEBUG_DELAYED_SERVICE) {
                String str4 = ActivityManagerService.TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("r.name ");
                stringBuilder4.append(serviceRecord.name);
                stringBuilder4.append(" N ");
                stringBuilder4.append(N);
                stringBuilder4.append(" minDuration ");
                stringBuilder4.append(minDuration);
                stringBuilder4.append(" resetTime ");
                stringBuilder4.append(resetTime);
                stringBuilder4.append(" now ");
                now = now3;
                stringBuilder4.append(now);
                stringBuilder4.append(" r.restartDelay ");
                stringBuilder4.append(serviceRecord.restartDelay);
                stringBuilder4.append(" r.restartTime+resetTime ");
                stringBuilder4.append(serviceRecord.restartTime + resetTime);
                stringBuilder4.append(" allowCancel ");
                stringBuilder4.append(z);
                Slog.w(str4, stringBuilder4.toString());
            } else {
                now = now3;
            }
            if (serviceRecord.restartDelay == 0) {
                serviceRecord.restartCount++;
                serviceRecord.restartDelay = minDuration;
                activeServices = this;
            } else if (serviceRecord.crashCount > 1) {
                activeServices = this;
                serviceRecord.restartDelay = activeServices.mAm.mConstants.BOUND_SERVICE_CRASH_RESTART_DURATION * ((long) (serviceRecord.crashCount - 1));
            } else {
                activeServices = this;
                if (now > serviceRecord.restartTime + resetTime) {
                    serviceRecord.restartCount = 1;
                    serviceRecord.restartDelay = minDuration;
                } else {
                    serviceRecord.restartDelay *= (long) activeServices.mAm.mConstants.SERVICE_RESTART_DURATION_FACTOR;
                    if (serviceRecord.restartDelay < minDuration) {
                        serviceRecord.restartDelay = minDuration;
                    }
                }
            }
            serviceRecord.nextRestartTime = serviceRecord.restartDelay + now;
            if (SERVICE_RESCHEDULE && DEBUG_DELAYED_SERVICE) {
                String str5 = ActivityManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("r.name ");
                stringBuilder.append(serviceRecord.name);
                stringBuilder.append(" N ");
                stringBuilder.append(N);
                stringBuilder.append(" minDuration ");
                stringBuilder.append(minDuration);
                stringBuilder.append(" resetTime ");
                stringBuilder.append(resetTime);
                stringBuilder.append(" now ");
                stringBuilder.append(now);
                stringBuilder.append(" r.restartDelay ");
                stringBuilder.append(serviceRecord.restartDelay);
                stringBuilder.append(" r.restartTime+resetTime ");
                stringBuilder.append(serviceRecord.restartTime + resetTime);
                stringBuilder.append(" r.nextRestartTime ");
                stringBuilder.append(serviceRecord.nextRestartTime);
                stringBuilder.append(" allowCancel ");
                stringBuilder.append(z);
                Slog.w(str5, stringBuilder.toString());
            }
            while (true) {
                boolean repeat;
                int N2;
                long resetTime3;
                canceled = false;
                now2 = activeServices.mAm.mConstants.SERVICE_MIN_RESTART_TIME_BETWEEN;
                i = activeServices.mRestartingServices.size() - 1;
                while (i >= 0) {
                    ServiceRecord r2 = (ServiceRecord) activeServices.mRestartingServices.get(i);
                    if (r2 != serviceRecord) {
                        repeat = canceled;
                        N2 = N;
                        resetTime3 = resetTime;
                        if (serviceRecord.nextRestartTime >= r2.nextRestartTime - now2 && serviceRecord.nextRestartTime < r2.nextRestartTime + now2) {
                            serviceRecord.nextRestartTime = r2.nextRestartTime + now2;
                            serviceRecord.restartDelay = serviceRecord.nextRestartTime - now;
                            repeat = true;
                            break;
                        }
                    }
                    repeat = canceled;
                    N2 = N;
                    resetTime3 = resetTime;
                    i--;
                    canceled = repeat;
                    N = N2;
                    resetTime = resetTime3;
                    z = allowCancel;
                }
                repeat = canceled;
                N2 = N;
                resetTime3 = resetTime;
                if (!repeat) {
                    break;
                }
                N = N2;
                resetTime = resetTime3;
                z = allowCancel;
            }
            canceled = canceled2;
            z = false;
        } else {
            now = now2;
            serviceRecord.totalRestartCount++;
            z = false;
            serviceRecord.restartCount = 0;
            serviceRecord.restartDelay = 0;
            serviceRecord.nextRestartTime = now;
        }
        if (!activeServices.mRestartingServices.contains(serviceRecord)) {
            serviceRecord.createdFromFg = z;
            activeServices.mRestartingServices.add(serviceRecord);
            serviceRecord.makeRestarting(activeServices.mAm.mProcessStats.getMemFactorLocked(), now);
        }
        cancelForegroundNotificationLocked(r);
        activeServices.mAm.mHandler.removeCallbacks(serviceRecord.restarter);
        activeServices.mAm.mHandler.postAtTime(serviceRecord.restarter, serviceRecord.nextRestartTime);
        serviceRecord.nextRestartTime = SystemClock.uptimeMillis() + serviceRecord.restartDelay;
        String str6 = ActivityManagerService.TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Scheduling restart of crashed service ");
        stringBuilder.append(serviceRecord.shortName);
        stringBuilder.append(" in ");
        stringBuilder.append(serviceRecord.restartDelay);
        stringBuilder.append("ms");
        Slog.w(str6, stringBuilder.toString());
        if (SERVICE_RESCHEDULE && DEBUG_DELAYED_SERVICE) {
            for (int i4 = activeServices.mRestartingServices.size() - 1; i4 >= 0; i4--) {
                cur = (ServiceRecord) activeServices.mRestartingServices.get(i4);
                str2 = ActivityManagerService.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Restarting list - i ");
                stringBuilder2.append(i4);
                stringBuilder2.append(" r2.nextRestartTime ");
                stringBuilder2.append(cur.nextRestartTime);
                stringBuilder2.append(" r2.name ");
                stringBuilder2.append(cur.name);
                Slog.w(str2, stringBuilder2.toString());
            }
        }
        EventLog.writeEvent(EventLogTags.AM_SCHEDULE_SERVICE_RESTART, new Object[]{Integer.valueOf(serviceRecord.userId), serviceRecord.shortName, Long.valueOf(serviceRecord.restartDelay)});
        return canceled;
    }

    final void performServiceRestartLocked(ServiceRecord r) {
        if (!this.mRestartingServices.contains(r)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("no need to performServiceRestart for r:");
            stringBuilder.append(r);
            Flog.i(102, stringBuilder.toString());
        } else if (isServiceNeededLocked(r, false, false)) {
            try {
                if (SERVICE_RESCHEDULE) {
                    boolean shouldDelay = false;
                    ActivityRecord top_rc = null;
                    ActivityStack stack = this.mAm.getFocusedStack();
                    if (stack != null) {
                        top_rc = stack.topRunningActivityLocked();
                    }
                    if (!(top_rc == null || top_rc.nowVisible || r.shortName.contains(top_rc.packageName))) {
                        shouldDelay = true;
                    }
                    if (shouldDelay) {
                        if (DEBUG_DELAYED_SERVICE) {
                            String str = ActivityManagerService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Reschedule service restart due to app launch r.shortName ");
                            stringBuilder2.append(r.shortName);
                            stringBuilder2.append(" r.app = ");
                            stringBuilder2.append(r.app);
                            Slog.v(str, stringBuilder2.toString());
                        }
                        r.resetRestartCounter();
                        scheduleServiceRestartLocked(r, true);
                    } else {
                        bringUpServiceLocked(r, r.intent.getIntent().getFlags(), r.createdFromFg, true, false);
                    }
                } else {
                    bringUpServiceLocked(r, r.intent.getIntent().getFlags(), r.createdFromFg, true, false);
                }
            } catch (TransactionTooLargeException e) {
                Flog.w(102, "performServiceRestart TransactionTooLarge e:", e);
            }
        } else {
            String str2 = ActivityManagerService.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Restarting service that is not needed: ");
            stringBuilder3.append(r);
            Slog.wtf(str2, stringBuilder3.toString());
        }
    }

    private final boolean unscheduleServiceRestartLocked(ServiceRecord r, int callingUid, boolean force) {
        if (!force && r.restartDelay == 0) {
            return false;
        }
        boolean removed = this.mRestartingServices.remove(r);
        if (removed || callingUid != r.appInfo.uid) {
            r.resetRestartCounter();
        }
        if (removed) {
            clearRestartingIfNeededLocked(r);
        }
        this.mAm.mHandler.removeCallbacks(r.restarter);
        return true;
    }

    private void clearRestartingIfNeededLocked(ServiceRecord r) {
        if (r.restartTracker != null) {
            boolean stillTracking = false;
            for (int i = this.mRestartingServices.size() - 1; i >= 0; i--) {
                if (((ServiceRecord) this.mRestartingServices.get(i)).restartTracker == r.restartTracker) {
                    stillTracking = true;
                    break;
                }
            }
            if (!stillTracking) {
                r.restartTracker.setRestarting(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                r.restartTracker = null;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:82:0x027e  */
    /* JADX WARNING: Removed duplicated region for block: B:88:0x02ba  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x02c3  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String bringUpServiceLocked(ServiceRecord r, int intentFlags, boolean execInFg, boolean whileRestarting, boolean permissionsReviewRequired) throws TransactionTooLargeException {
        StringBuilder stringBuilder;
        ServiceRecord serviceRecord = r;
        boolean z = execInFg;
        StringBuilder stringBuilder2;
        if (serviceRecord.app != null && serviceRecord.app.thread != null) {
            sendServiceArgsLocked(serviceRecord, z, false);
            return null;
        } else if (whileRestarting || !this.mRestartingServices.contains(serviceRecord)) {
            String str;
            StringBuilder stringBuilder3;
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                str = ActivityManagerService.TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Bringing up ");
                stringBuilder3.append(serviceRecord);
                stringBuilder3.append(" ");
                stringBuilder3.append(serviceRecord.intent);
                stringBuilder3.append(" fg=");
                stringBuilder3.append(serviceRecord.fgRequired);
                Slog.v(str, stringBuilder3.toString());
            }
            if (this.mRestartingServices.remove(serviceRecord)) {
                if (HW_PARENT_CONTROL.equals(serviceRecord.packageName)) {
                    Slog.i(ActivityManagerService.TAG, "parentcontrol reset restart counter");
                    r.resetRestartCounter();
                }
                clearRestartingIfNeededLocked(r);
            }
            if (serviceRecord.delayed) {
                if (DEBUG_DELAYED_STARTS) {
                    str = ActivityManagerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("REM FR DELAY LIST (bring up): ");
                    stringBuilder3.append(serviceRecord);
                    Slog.v(str, stringBuilder3.toString());
                }
                getServiceMapLocked(serviceRecord.userId).mDelayedStartList.remove(serviceRecord);
                serviceRecord.delayed = false;
            }
            if (this.mAm.mUserController.hasStartedUserState(serviceRecord.userId)) {
                ProcessRecord app;
                String hostingType;
                ProcessRecord app2;
                try {
                    AppGlobals.getPackageManager().setPackageStoppedState(serviceRecord.packageName, false, serviceRecord.userId);
                } catch (RemoteException e) {
                } catch (IllegalArgumentException e2) {
                    String str2 = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed trying to unstop package ");
                    stringBuilder.append(serviceRecord.packageName);
                    stringBuilder.append(": ");
                    stringBuilder.append(e2);
                    Slog.w(str2, stringBuilder.toString());
                }
                boolean isolated = (serviceRecord.serviceInfo.flags & 2) != 0;
                String procName = serviceRecord.processName;
                String hostingType2 = "service";
                if (isolated) {
                    app = serviceRecord.isolatedProc;
                    if (WebViewZygote.isMultiprocessEnabled() && serviceRecord.serviceInfo.packageName.equals(WebViewZygote.getPackageName())) {
                        hostingType = "webview_service";
                        app2 = app;
                        if (app2 == null || permissionsReviewRequired) {
                            app = app2;
                        } else {
                            if (whileRestarting) {
                                this.mAm.shouldPreventRestartService(serviceRecord.serviceInfo, true);
                            }
                            ProcessRecord startProcessLocked = this.mAm.startProcessLocked(procName, serviceRecord.appInfo, true, intentFlags, hostingType, serviceRecord.name, false, isolated, 0, false, null, null, initPCEntryArgs(r), null);
                            app = startProcessLocked;
                            if (startProcessLocked == null) {
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Unable to launch app ");
                                stringBuilder4.append(serviceRecord.appInfo.packageName);
                                stringBuilder4.append(SliceAuthority.DELIMITER);
                                stringBuilder4.append(serviceRecord.appInfo.uid);
                                stringBuilder4.append(" for service ");
                                stringBuilder4.append(serviceRecord.intent.getIntent());
                                stringBuilder4.append(": process is bad");
                                String msg = stringBuilder4.toString();
                                Slog.w(ActivityManagerService.TAG, msg);
                                try {
                                    bringDownServiceLocked(r);
                                } catch (IllegalStateException app22) {
                                    ProcessRecord processRecord = app22;
                                    String str3 = ActivityManagerService.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Exception when bring down Service ");
                                    stringBuilder.append(serviceRecord.shortName);
                                    Slog.w(str3, stringBuilder.toString(), app22);
                                }
                                return msg;
                            } else if (isolated) {
                                serviceRecord.isolatedProc = app;
                            }
                        }
                        if (serviceRecord.fgRequired) {
                            if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                                str = ActivityManagerService.TAG;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Whitelisting ");
                                stringBuilder3.append(UserHandle.formatUid(serviceRecord.appInfo.uid));
                                stringBuilder3.append(" for fg-service launch");
                                Slog.v(str, stringBuilder3.toString());
                            }
                            this.mAm.tempWhitelistUidLocked(serviceRecord.appInfo.uid, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, "fg-service-launch");
                        }
                        if (!this.mPendingServices.contains(serviceRecord)) {
                            this.mPendingServices.add(serviceRecord);
                        }
                        if (serviceRecord.delayedStop) {
                            serviceRecord.delayedStop = false;
                            if (serviceRecord.startRequested) {
                                if (DEBUG_DELAYED_STARTS) {
                                    str = ActivityManagerService.TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Applying delayed stop (in bring up): ");
                                    stringBuilder2.append(serviceRecord);
                                    Slog.v(str, stringBuilder2.toString());
                                }
                                stopServiceLocked(r);
                            }
                        }
                        return null;
                    }
                }
                app = this.mAm.getProcessRecordLocked(procName, serviceRecord.appInfo.uid, false);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    str = TAG_MU;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("bringUpServiceLocked: appInfo.uid=");
                    stringBuilder5.append(serviceRecord.appInfo.uid);
                    stringBuilder5.append(" app=");
                    stringBuilder5.append(app);
                    stringBuilder5.append(" app.thread=");
                    stringBuilder5.append(app != null ? app.thread : null);
                    Slog.v(str, stringBuilder5.toString());
                }
                if (!(app == null || app.thread == null)) {
                    try {
                        app.addPackage(serviceRecord.appInfo.packageName, serviceRecord.appInfo.longVersionCode, this.mAm.mProcessStats);
                        realStartServiceLocked(serviceRecord, app, z);
                        return null;
                    } catch (TransactionTooLargeException e3) {
                        throw e3;
                    } catch (RemoteException e4) {
                        String str4 = ActivityManagerService.TAG;
                        StringBuilder stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("Exception when starting service ");
                        stringBuilder6.append(serviceRecord.shortName);
                        Slog.w(str4, stringBuilder6.toString(), e4);
                    }
                }
                hostingType = hostingType2;
                app22 = app;
                if (app22 == null) {
                }
                app = app22;
                if (serviceRecord.fgRequired) {
                }
                if (this.mPendingServices.contains(serviceRecord)) {
                }
                if (serviceRecord.delayedStop) {
                }
                return null;
            }
            str = new StringBuilder();
            str.append("Unable to launch app ");
            str.append(serviceRecord.appInfo.packageName);
            str.append(SliceAuthority.DELIMITER);
            str.append(serviceRecord.appInfo.uid);
            str.append(" for service ");
            str.append(serviceRecord.intent.getIntent());
            str.append(": user ");
            str.append(serviceRecord.userId);
            str.append(" is stopped");
            str = str.toString();
            Slog.w(ActivityManagerService.TAG, str);
            bringDownServiceLocked(r);
            return str;
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("do nothing when waiting for a restart and Bringing up ");
            stringBuilder2.append(serviceRecord);
            stringBuilder2.append(" ");
            stringBuilder2.append(serviceRecord.intent);
            Flog.i(102, stringBuilder2.toString());
            return null;
        }
    }

    private final void requestServiceBindingsLocked(ServiceRecord r, boolean execInFg) throws TransactionTooLargeException {
        int i = r.bindings.size() - 1;
        while (i >= 0 && requestServiceBindingLocked(r, (IntentBindRecord) r.bindings.valueAt(i), execInFg, false)) {
            i--;
        }
    }

    private final void realStartServiceLocked(ServiceRecord r, ProcessRecord app, boolean execInFg) throws RemoteException {
        ServiceRecord serviceRecord = r;
        ProcessRecord processRecord = app;
        boolean z = execInFg;
        if (processRecord.thread != null) {
            StringBuilder stringBuilder;
            if (ActivityManagerDebugConfig.DEBUG_MU) {
                String str = TAG_MU;
                stringBuilder = new StringBuilder();
                stringBuilder.append("realStartServiceLocked, ServiceRecord.uid = ");
                stringBuilder.append(serviceRecord.appInfo.uid);
                stringBuilder.append(", ProcessRecord.uid = ");
                stringBuilder.append(processRecord.uid);
                Slog.v(str, stringBuilder.toString());
            }
            serviceRecord.app = processRecord;
            long uptimeMillis = SystemClock.uptimeMillis();
            serviceRecord.lastActivity = uptimeMillis;
            serviceRecord.restartTime = uptimeMillis;
            boolean newService = processRecord.services.add(serviceRecord);
            bumpServiceExecutingLocked(serviceRecord, z, "create");
            this.mAm.updateLruProcessLocked(processRecord, false, null);
            updateServiceForegroundLocked(serviceRecord.app, false);
            this.mAm.updateOomAdjLocked();
            boolean created = false;
            String str2;
            StringBuilder stringBuilder2;
            String str3;
            try {
                boolean z2;
                int lastPeriod = serviceRecord.shortName.lastIndexOf(46);
                EventLogTags.writeAmCreateService(serviceRecord.userId, System.identityHashCode(r), lastPeriod >= 0 ? serviceRecord.shortName.substring(lastPeriod) : serviceRecord.shortName, serviceRecord.app.uid, serviceRecord.app.pid);
                synchronized (serviceRecord.stats.getBatteryStats()) {
                    serviceRecord.stats.startLaunchedLocked();
                }
                this.mAm.notifyPackageUse(serviceRecord.serviceInfo.packageName, 1);
                processRecord.forceProcessStateUpTo(9);
                processRecord.thread.scheduleCreateService(serviceRecord, serviceRecord.serviceInfo, this.mAm.compatibilityInfoForPackageLocked(serviceRecord.serviceInfo.applicationInfo), processRecord.repProcState);
                r.postNotification();
                if (!true) {
                    created = this.mDestroyingServices.contains(serviceRecord);
                    serviceDoneExecutingLocked(serviceRecord, created, created);
                    if (newService) {
                        processRecord.services.remove(serviceRecord);
                        serviceRecord.app = null;
                        if (SERVICE_RESCHEDULE && DEBUG_DELAYED_SERVICE) {
                            str2 = ActivityManagerService.TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(" Failed to create Service !!!! .This will introduce huge delay...  ");
                            stringBuilder2.append(serviceRecord.shortName);
                            stringBuilder2.append(" in ");
                            stringBuilder2.append(serviceRecord.restartDelay);
                            stringBuilder2.append("ms");
                            Slog.w(str2, stringBuilder2.toString());
                        }
                    }
                    if (created) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Destroying no retry when creating service ");
                        stringBuilder3.append(serviceRecord);
                        Flog.w(102, stringBuilder3.toString());
                    } else {
                        scheduleServiceRestartLocked(serviceRecord, false);
                    }
                }
                if (serviceRecord.whitelistManager) {
                    processRecord.whitelistManager = true;
                }
                requestServiceBindingsLocked(serviceRecord, z);
                updateServiceClientActivitiesLocked(processRecord, null, true);
                if (serviceRecord.startRequested && serviceRecord.callStart && serviceRecord.pendingStarts.size() == 0) {
                    ArrayList arrayList = serviceRecord.pendingStarts;
                    StartItem startItem = r2;
                    StartItem startItem2 = new StartItem(serviceRecord, false, r.makeNextStartId(), null, null, 0);
                    arrayList.add(startItem);
                }
                sendServiceArgsLocked(serviceRecord, z, true);
                if (serviceRecord.delayed) {
                    if (DEBUG_DELAYED_STARTS) {
                        str3 = ActivityManagerService.TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("REM FR DELAY LIST (new proc): ");
                        stringBuilder4.append(serviceRecord);
                        Slog.v(str3, stringBuilder4.toString());
                    }
                    getServiceMapLocked(serviceRecord.userId).mDelayedStartList.remove(serviceRecord);
                    z2 = false;
                    serviceRecord.delayed = false;
                } else {
                    z2 = false;
                }
                if (serviceRecord.delayedStop) {
                    serviceRecord.delayedStop = z2;
                    if (serviceRecord.startRequested) {
                        if (DEBUG_DELAYED_STARTS) {
                            str3 = ActivityManagerService.TAG;
                            created = new StringBuilder();
                            created.append("Applying delayed stop (from start): ");
                            created.append(serviceRecord);
                            Slog.v(str3, created.toString());
                        }
                        stopServiceLocked(r);
                    }
                }
            } catch (DeadObjectException e) {
                try {
                    str2 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Application dead when creating service ");
                    stringBuilder2.append(serviceRecord);
                    Slog.w(str2, stringBuilder2.toString());
                    this.mAm.appDiedLocked(processRecord);
                    throw e;
                } catch (Throwable th) {
                    if (!created) {
                        boolean inDestroying = this.mDestroyingServices.contains(serviceRecord);
                        serviceDoneExecutingLocked(serviceRecord, inDestroying, inDestroying);
                        if (newService) {
                            processRecord.services.remove(serviceRecord);
                            serviceRecord.app = null;
                            if (SERVICE_RESCHEDULE && DEBUG_DELAYED_SERVICE) {
                                str3 = ActivityManagerService.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(" Failed to create Service !!!! .This will introduce huge delay...  ");
                                stringBuilder2.append(serviceRecord.shortName);
                                stringBuilder2.append(" in ");
                                stringBuilder2.append(serviceRecord.restartDelay);
                                stringBuilder2.append("ms");
                                Slog.w(str3, stringBuilder2.toString());
                            }
                        }
                        if (inDestroying) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Destroying no retry when creating service ");
                            stringBuilder.append(serviceRecord);
                            Flog.w(102, stringBuilder.toString());
                        } else {
                            scheduleServiceRestartLocked(serviceRecord, false);
                        }
                    }
                }
            }
        } else {
            throw new RemoteException();
        }
    }

    private final void sendServiceArgsLocked(ServiceRecord r, boolean execInFg, boolean oomAdjusted) throws TransactionTooLargeException {
        String str;
        StringBuilder stringBuilder;
        int N = r.pendingStarts.size();
        if (N != 0) {
            int i;
            ArrayList<ServiceStartArgs> args = new ArrayList();
            while (true) {
                i = 0;
                if (r.pendingStarts.size() <= 0) {
                    break;
                }
                String str2;
                StartItem si = (StartItem) r.pendingStarts.remove(0);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Sending arguments to: ");
                    stringBuilder2.append(r);
                    stringBuilder2.append(" ");
                    stringBuilder2.append(r.intent);
                    stringBuilder2.append(" args=");
                    stringBuilder2.append(si.intent);
                    Slog.v(str2, stringBuilder2.toString());
                }
                if (si.intent != null || N <= 1) {
                    si.deliveredTime = SystemClock.uptimeMillis();
                    r.deliveredStarts.add(si);
                    si.deliveryCount++;
                    if (si.neededGrants != null) {
                        this.mAm.grantUriPermissionUncheckedFromIntentLocked(si.neededGrants, si.getUriPermissionsLocked());
                    }
                    this.mAm.grantEphemeralAccessLocked(r.userId, si.intent, r.appInfo.uid, UserHandle.getAppId(si.callingId));
                    bumpServiceExecutingLocked(r, execInFg, "start");
                    if (!oomAdjusted) {
                        oomAdjusted = true;
                        this.mAm.updateOomAdjLocked(r.app, true);
                    }
                    if (r.fgRequired && !r.fgWaiting) {
                        if (r.isForeground) {
                            if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                                str2 = ActivityManagerService.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Service already foreground; no new timeout: ");
                                stringBuilder3.append(r);
                                Slog.i(str2, stringBuilder3.toString());
                            }
                            r.fgRequired = false;
                        } else {
                            if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK != 0) {
                                i = ActivityManagerService.TAG;
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Launched service must call startForeground() within timeout: ");
                                stringBuilder4.append(r);
                                Slog.i(i, stringBuilder4.toString());
                            }
                            scheduleServiceForegroundTransitionTimeoutLocked(r);
                        }
                    }
                    i = 0;
                    if (si.deliveryCount > 1) {
                        i = 0 | 2;
                    }
                    if (si.doneExecutingCount > 0) {
                        i |= 1;
                    }
                    args.add(new ServiceStartArgs(si.taskRemoved, si.id, i, si.intent));
                }
            }
            ParceledListSlice<ServiceStartArgs> slice = new ParceledListSlice(args);
            slice.setInlineCountLimit(4);
            Exception caughtException = null;
            try {
                r.app.thread.scheduleServiceArgs(r, slice);
            } catch (TransactionTooLargeException e) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Transaction too large for ");
                    stringBuilder.append(args.size());
                    stringBuilder.append(" args, first: ");
                    stringBuilder.append(((ServiceStartArgs) args.get(0)).args);
                    Slog.v(str, stringBuilder.toString());
                }
                Slog.w(ActivityManagerService.TAG, "Failed delivering service starts", e);
                caughtException = e;
            } catch (RemoteException e2) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    str = ActivityManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Crashed while sending args: ");
                    stringBuilder.append(r);
                    Slog.v(str, stringBuilder.toString());
                }
                Slog.w(ActivityManagerService.TAG, "Failed delivering service starts", e2);
                caughtException = e2;
            } catch (Exception e22) {
                Slog.w(ActivityManagerService.TAG, "Unexpected exception", e22);
                caughtException = e22;
            }
            if (caughtException != null) {
                boolean inDestroying = this.mDestroyingServices.contains(r);
                while (i < args.size()) {
                    serviceDoneExecutingLocked(r, inDestroying, inDestroying);
                    i++;
                }
                if (caughtException instanceof TransactionTooLargeException) {
                    throw ((TransactionTooLargeException) caughtException);
                }
            }
        }
    }

    private final boolean isServiceNeededLocked(ServiceRecord r, boolean knowConn, boolean hasConn) {
        if (r.startRequested) {
            return true;
        }
        if (!knowConn) {
            hasConn = r.hasAutoCreateConnections();
        }
        if (hasConn) {
            return true;
        }
        return false;
    }

    private final void bringDownServiceIfNeededLocked(ServiceRecord r, boolean knowConn, boolean hasConn) {
        StringBuilder stringBuilder;
        if (isServiceNeededLocked(r, knowConn, hasConn)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("ServiceNeeded not bring down service:");
            stringBuilder.append(r);
            Flog.i(102, stringBuilder.toString());
        } else if (this.mPendingServices.contains(r)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("still in launching not bring down service:");
            stringBuilder.append(r);
            Flog.i(102, stringBuilder.toString());
        } else {
            try {
                bringDownServiceLocked(r);
            } catch (IllegalStateException e) {
                Slog.w(ActivityManagerService.TAG, "Exception when r is illegal!");
            }
        }
    }

    private final void bringDownServiceLocked(ServiceRecord r) {
        StringBuilder stringBuilder;
        int conni;
        StringBuilder stringBuilder2;
        String str;
        String str2;
        StringBuilder stringBuilder3;
        int i;
        ServiceRecord serviceRecord = r;
        int connectionSize = serviceRecord.connections.size();
        if (ActivityManagerDebugConfig.HWFLOW && ((long) connectionSize) > SERVICE_CONNECTIONS_THRESHOLD) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("bringDownServiceLocked ");
            stringBuilder.append(serviceRecord);
            stringBuilder.append(",connection size= ");
            stringBuilder.append(connectionSize);
            Flog.i(102, stringBuilder.toString());
        }
        long start = SystemClock.uptimeMillis();
        int conni2 = connectionSize - 1;
        while (true) {
            conni = conni2;
            int i2 = 0;
            if (conni < 0) {
                break;
            }
            ArrayList<ConnectionRecord> c = (ArrayList) serviceRecord.connections.valueAt(conni);
            while (i2 < c.size()) {
                ConnectionRecord cr = (ConnectionRecord) c.get(i2);
                cr.serviceDead = true;
                try {
                    cr.conn.connected(serviceRecord.name, null, true);
                } catch (Exception e) {
                    String str3 = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failure disconnecting service ");
                    stringBuilder2.append(serviceRecord.name);
                    stringBuilder2.append(" to connection ");
                    stringBuilder2.append(((ConnectionRecord) c.get(i2)).conn.asBinder());
                    stringBuilder2.append(" (in ");
                    stringBuilder2.append(((ConnectionRecord) c.get(i2)).binding.client.processName);
                    stringBuilder2.append(")");
                    Slog.w(str3, stringBuilder2.toString(), e);
                }
                i2++;
            }
            conni2 = conni - 1;
        }
        long diff = SystemClock.uptimeMillis() - start;
        if (diff > 1000) {
            if (ActivityManagerDebugConfig.HWFLOW) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("bringDownServiceLocked ");
                stringBuilder.append(serviceRecord);
                stringBuilder.append(",took ");
                stringBuilder.append(diff);
                stringBuilder.append("ms");
                Flog.i(102, stringBuilder.toString());
            }
            Jlog.d(377, r.toString(), (int) diff, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        if (serviceRecord.app != null && serviceRecord.app.thread != null) {
            conni2 = serviceRecord.bindings.size() - 1;
            while (true) {
                conni = conni2;
                if (conni < 0) {
                    break;
                }
                IntentBindRecord ibr = (IntentBindRecord) serviceRecord.bindings.valueAt(conni);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    str = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Bringing down binding ");
                    stringBuilder2.append(ibr);
                    stringBuilder2.append(": hasBound=");
                    stringBuilder2.append(ibr.hasBound);
                    Slog.v(str, stringBuilder2.toString());
                }
                if (ibr.hasBound) {
                    try {
                        bumpServiceExecutingLocked(serviceRecord, false, "bring down unbind");
                        this.mAm.updateOomAdjLocked(serviceRecord.app, true);
                        ibr.hasBound = false;
                        ibr.requested = false;
                        serviceRecord.app.thread.scheduleUnbindService(serviceRecord, ibr.intent.getIntent());
                    } catch (Exception e2) {
                        str2 = ActivityManagerService.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Exception when unbinding service ");
                        stringBuilder3.append(serviceRecord.shortName);
                        Slog.w(str2, stringBuilder3.toString(), e2);
                        serviceProcessGoneLocked(r);
                    }
                }
                conni2 = conni - 1;
            }
        }
        if (serviceRecord.fgRequired) {
            str = ActivityManagerService.TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Bringing down service while still waiting for start foreground: ");
            stringBuilder4.append(serviceRecord);
            Slog.w(str, stringBuilder4.toString());
            serviceRecord.fgRequired = false;
            serviceRecord.fgWaiting = false;
            this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, serviceRecord.appInfo.uid, serviceRecord.packageName);
            this.mAm.mHandler.removeMessages(66, serviceRecord);
            if (serviceRecord.app != null) {
                Message msg = this.mAm.mHandler.obtainMessage(69);
                msg.obj = serviceRecord.app;
                msg.getData().putCharSequence("servicerecord", r.toString());
                this.mAm.mHandler.sendMessage(msg);
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            RuntimeException here = new RuntimeException();
            here.fillInStackTrace();
            String str4 = ActivityManagerService.TAG;
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("Bringing down ");
            stringBuilder5.append(serviceRecord);
            stringBuilder5.append(" ");
            stringBuilder5.append(serviceRecord.intent);
            Slog.v(str4, stringBuilder5.toString(), here);
        }
        serviceRecord.destroyTime = SystemClock.uptimeMillis();
        conni2 = serviceRecord.userId;
        conni = System.identityHashCode(r);
        if (serviceRecord.app != null) {
            i = serviceRecord.app.pid;
        } else {
            i = -1;
        }
        EventLogTags.writeAmDestroyService(conni2, conni, i);
        ServiceMap smap = getServiceMapLocked(serviceRecord.userId);
        ServiceRecord found = (ServiceRecord) smap.mServicesByName.remove(serviceRecord.name);
        if (found == null || found == serviceRecord) {
            smap.mServicesByIntent.remove(serviceRecord.intent);
            serviceRecord.totalRestartCount = 0;
            unscheduleServiceRestartLocked(serviceRecord, 0, true);
            for (conni2 = this.mPendingServices.size() - 1; conni2 >= 0; conni2--) {
                if (this.mPendingServices.get(conni2) == serviceRecord) {
                    this.mPendingServices.remove(conni2);
                    if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                        str2 = ActivityManagerService.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Removed pending: ");
                        stringBuilder3.append(serviceRecord);
                        Slog.v(str2, stringBuilder3.toString());
                    }
                }
            }
            cancelForegroundNotificationLocked(r);
            if (serviceRecord.isForeground) {
                decActiveForegroundAppLocked(smap, serviceRecord);
                this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, serviceRecord.appInfo.uid, serviceRecord.packageName);
                StatsLog.write(60, serviceRecord.appInfo.uid, serviceRecord.shortName, 2);
            }
            serviceRecord.isForeground = false;
            serviceRecord.foregroundId = 0;
            serviceRecord.foregroundNoti = null;
            r.clearDeliveredStartsLocked();
            serviceRecord.pendingStarts.clear();
            if (serviceRecord.app != null) {
                synchronized (serviceRecord.stats.getBatteryStats()) {
                    serviceRecord.stats.stopLaunchedLocked();
                }
                serviceRecord.app.services.remove(serviceRecord);
                if (serviceRecord.whitelistManager) {
                    updateWhitelistManagerLocked(serviceRecord.app);
                }
                if (serviceRecord.app.thread != null) {
                    updateServiceForegroundLocked(serviceRecord.app, false);
                    try {
                        bumpServiceExecutingLocked(serviceRecord, false, "destroy");
                        this.mDestroyingServices.add(serviceRecord);
                        serviceRecord.destroying = true;
                        this.mAm.updateOomAdjLocked(serviceRecord.app, true);
                        serviceRecord.app.thread.scheduleStopService(serviceRecord);
                    } catch (Exception e22) {
                        String str5 = ActivityManagerService.TAG;
                        StringBuilder stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("Exception when destroying service ");
                        stringBuilder6.append(serviceRecord.shortName);
                        Slog.w(str5, stringBuilder6.toString(), e22);
                        serviceProcessGoneLocked(r);
                    }
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Removed service that has no process: ");
                    stringBuilder.append(serviceRecord);
                    Flog.i(102, stringBuilder.toString());
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Removed service that is not running: ");
                stringBuilder.append(serviceRecord);
                Flog.i(102, stringBuilder.toString());
            }
            if (serviceRecord.bindings.size() > 0) {
                serviceRecord.bindings.clear();
            }
            if (serviceRecord.restarter instanceof ServiceRestarter) {
                ((ServiceRestarter) serviceRecord.restarter).setService(null);
            }
            conni2 = this.mAm.mProcessStats.getMemFactorLocked();
            long now = SystemClock.uptimeMillis();
            if (serviceRecord.tracker != null) {
                serviceRecord.tracker.setStarted(false, conni2, now);
                serviceRecord.tracker.setBound(false, conni2, now);
                if (serviceRecord.executeNesting == 0) {
                    serviceRecord.tracker.clearCurrentOwner(serviceRecord, false);
                    serviceRecord.tracker = null;
                }
            }
            smap.ensureNotStartingBackgroundLocked(serviceRecord);
            return;
        }
        smap.mServicesByName.put(serviceRecord.name, found);
        StringBuilder stringBuilder7 = new StringBuilder();
        stringBuilder7.append("Bringing down ");
        stringBuilder7.append(serviceRecord);
        stringBuilder7.append(" but actually running ");
        stringBuilder7.append(found);
        throw new IllegalStateException(stringBuilder7.toString());
    }

    void removeConnectionLocked(ConnectionRecord c, ProcessRecord skipApp, ActivityRecord skipAct) {
        IBinder binder = c.conn.asBinder();
        AppBindRecord b = c.binding;
        ServiceRecord s = b.service;
        ArrayList<ConnectionRecord> clist = (ArrayList) s.connections.get(binder);
        if (clist != null) {
            clist.remove(c);
            if (clist.size() == 0) {
                s.connections.remove(binder);
            }
        }
        b.connections.remove(c);
        if (!(c.activity == null || c.activity == skipAct || c.activity.connections == null)) {
            c.activity.connections.remove(c);
        }
        if (b.client != skipApp) {
            b.client.connections.remove(c);
            ProcessRecord pr = b.client;
            if (!(s.app == null || s.app.uid < 10000 || pr.pid == s.app.pid || s.app.info == null || pr.info == null || s.app.info.packageName == null || s.app.info.packageName.equals(pr.info.packageName))) {
                LogPower.push(167, s.processName, Integer.toString(pr.pid), Integer.toString(s.app.pid), new String[]{"service"});
            }
            if ((c.flags & 8) != 0) {
                b.client.updateHasAboveClientLocked();
            }
            if ((c.flags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0) {
                s.updateWhitelistManager();
                if (!(s.whitelistManager || s.app == null)) {
                    updateWhitelistManagerLocked(s.app);
                }
            }
            if (s.app != null) {
                updateServiceClientActivitiesLocked(s.app, c, true);
            }
        }
        clist = (ArrayList) this.mServiceConnections.get(binder);
        if (clist != null) {
            clist.remove(c);
            if (clist.size() == 0) {
                this.mServiceConnections.remove(binder);
            }
        }
        this.mAm.stopAssociationLocked(b.client.uid, b.client.processName, s.appInfo.uid, s.name);
        this.mAm.mHwAMSEx.reportServiceRelationIAware(3, s, b.client);
        if (b.connections.size() == 0) {
            b.intent.apps.remove(b.client);
        }
        if (!c.serviceDead) {
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Disconnecting binding ");
                stringBuilder.append(b.intent);
                stringBuilder.append(": shouldUnbind=");
                stringBuilder.append(b.intent.hasBound);
                Slog.v(str, stringBuilder.toString());
            }
            if (s.app != null && s.app.thread != null && b.intent.apps.size() == 0 && b.intent.hasBound) {
                try {
                    bumpServiceExecutingLocked(s, false, "unbind");
                    if (b.client != s.app && (c.flags & 32) == 0 && s.app.setProcState <= 12) {
                        this.mAm.updateLruProcessLocked(s.app, false, null);
                    }
                    this.mAm.updateOomAdjLocked(s.app, true);
                    b.intent.hasBound = false;
                    b.intent.doRebind = false;
                    s.app.thread.scheduleUnbindService(s, b.intent.intent.getIntent());
                } catch (Exception e) {
                    String str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Exception when unbinding service ");
                    stringBuilder2.append(s.shortName);
                    Slog.w(str2, stringBuilder2.toString(), e);
                    if (!s.app.killedByAm) {
                        serviceProcessGoneLocked(s);
                    }
                }
            }
            this.mPendingServices.remove(s);
            if ((c.flags & 1) != 0) {
                boolean hasAutoCreate = s.hasAutoCreateConnections();
                if (!(hasAutoCreate || s.tracker == null)) {
                    s.tracker.setBound(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                }
                bringDownServiceIfNeededLocked(s, true, hasAutoCreate);
            }
        }
    }

    void serviceDoneExecutingLocked(ServiceRecord r, int type, int startId, int res) {
        boolean inDestroying = this.mDestroyingServices.contains(r);
        StringBuilder stringBuilder;
        if (r != null) {
            if (type == 1) {
                r.callStart = true;
                if (res != 1000) {
                    switch (res) {
                        case 0:
                        case 1:
                            r.findDeliveredStart(startId, false, true);
                            r.stopIfKilled = false;
                            break;
                        case 2:
                            r.findDeliveredStart(startId, false, true);
                            if (r.getLastStartId() == startId) {
                                r.stopIfKilled = true;
                                break;
                            }
                            break;
                        case 3:
                            StartItem si = r.findDeliveredStart(startId, false, false);
                            if (si != null) {
                                si.deliveryCount = 0;
                                si.doneExecutingCount++;
                                r.stopIfKilled = true;
                                break;
                            }
                            break;
                        default:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown service start result: ");
                            stringBuilder.append(res);
                            throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                r.findDeliveredStart(startId, true, true);
                if (res == 0) {
                    r.callStart = false;
                }
            } else if (type == 2) {
                if (inDestroying) {
                    if (r.executeNesting != 1) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Service done with onDestroy, but executeNesting=");
                        stringBuilder2.append(r.executeNesting);
                        stringBuilder2.append(": ");
                        stringBuilder2.append(r);
                        Flog.w(102, stringBuilder2.toString());
                        String str = ActivityManagerService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Service done with onDestroy, but executeNesting=");
                        stringBuilder2.append(r.executeNesting);
                        stringBuilder2.append(": ");
                        stringBuilder2.append(r);
                        Slog.w(str, stringBuilder2.toString());
                        r.executeNesting = 1;
                    }
                } else if (r.app != null) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Service done with onDestroy, but not inDestroying:");
                    stringBuilder3.append(r);
                    stringBuilder3.append(", app=");
                    stringBuilder3.append(r.app);
                    Flog.w(102, stringBuilder3.toString());
                }
            }
            long origId = Binder.clearCallingIdentity();
            serviceDoneExecutingLocked(r, inDestroying, inDestroying);
            Binder.restoreCallingIdentity(origId);
            return;
        }
        String str2 = ActivityManagerService.TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Done executing unknown service from pid ");
        stringBuilder.append(Binder.getCallingPid());
        Slog.w(str2, stringBuilder.toString());
    }

    private void serviceProcessGoneLocked(ServiceRecord r) {
        if (r.tracker != null) {
            int memFactor = this.mAm.mProcessStats.getMemFactorLocked();
            long now = SystemClock.uptimeMillis();
            r.tracker.setExecuting(false, memFactor, now);
            r.tracker.setBound(false, memFactor, now);
            r.tracker.setStarted(false, memFactor, now);
        }
        serviceDoneExecutingLocked(r, true, true);
    }

    private void serviceDoneExecutingLocked(ServiceRecord r, boolean inDestroying, boolean finishing) {
        String str;
        StringBuilder stringBuilder;
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("<<< DONE EXECUTING ");
            stringBuilder.append(r);
            stringBuilder.append(": nesting=");
            stringBuilder.append(r.executeNesting);
            stringBuilder.append(", inDestroying=");
            stringBuilder.append(inDestroying);
            stringBuilder.append(", app=");
            stringBuilder.append(r.app);
            Slog.v(str, stringBuilder.toString());
        } else if (ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING) {
            str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("<<< DONE EXECUTING ");
            stringBuilder.append(r.shortName);
            Slog.v(str, stringBuilder.toString());
        }
        r.executeNesting--;
        if (r.executeNesting <= 0) {
            if (r.app != null) {
                StringBuilder stringBuilder2;
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    str = ActivityManagerService.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Nesting at 0 of ");
                    stringBuilder2.append(r.shortName);
                    Slog.v(str, stringBuilder2.toString());
                }
                r.app.execServicesFg = false;
                r.app.executingServices.remove(r);
                if (r.app.executingServices.size() == 0) {
                    if (ActivityManagerDebugConfig.DEBUG_SERVICE || ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING) {
                        str = ActivityManagerService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("No more executingServices of ");
                        stringBuilder2.append(r.shortName);
                        Slog.v(str, stringBuilder2.toString());
                    }
                    this.mAm.mHandler.removeMessages(12, r.app);
                    this.mAm.mHandler.removeMessages(99, r.app);
                } else if (r.executeFg) {
                    for (int i = r.app.executingServices.size() - 1; i >= 0; i--) {
                        if (((ServiceRecord) r.app.executingServices.valueAt(i)).executeFg) {
                            r.app.execServicesFg = true;
                            break;
                        }
                    }
                }
                if (inDestroying) {
                    if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                        str = ActivityManagerService.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("doneExecuting remove destroying ");
                        stringBuilder2.append(r);
                        Slog.v(str, stringBuilder2.toString());
                    }
                    this.mDestroyingServices.remove(r);
                    r.bindings.clear();
                }
                this.mAm.updateOomAdjLocked(r.app, true);
            }
            r.executeFg = false;
            if (r.tracker != null) {
                r.tracker.setExecuting(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                if (finishing) {
                    r.tracker.clearCurrentOwner(r, false);
                    r.tracker = null;
                }
            }
            if (finishing) {
                if (!(r.app == null || r.app.persistent)) {
                    r.app.services.remove(r);
                    if (r.whitelistManager) {
                        updateWhitelistManagerLocked(r.app);
                    }
                }
                r.app = null;
            }
        }
    }

    boolean attachApplicationLocked(ProcessRecord proc, String processName) throws RemoteException {
        boolean didSomething = false;
        int i = 0;
        if (this.mPendingServices.size() > 0) {
            ServiceRecord sr = null;
            boolean didSomething2 = false;
            int i2 = 0;
            while (i2 < this.mPendingServices.size()) {
                try {
                    sr = (ServiceRecord) this.mPendingServices.get(i2);
                    if (proc != sr.isolatedProc) {
                        if (proc.uid == sr.appInfo.uid) {
                            if (!processName.equals(sr.processName)) {
                            }
                        }
                        i2++;
                    }
                    this.mPendingServices.remove(i2);
                    i2--;
                    proc.addPackage(sr.appInfo.packageName, sr.appInfo.longVersionCode, this.mAm.mProcessStats);
                    realStartServiceLocked(sr, proc, sr.createdFromFg);
                    didSomething2 = true;
                    if (!isServiceNeededLocked(sr, false, false)) {
                        bringDownServiceLocked(sr);
                    }
                    i2++;
                } catch (RemoteException didSomething3) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception in new application when starting service ");
                    stringBuilder.append(sr.shortName);
                    Slog.w(ActivityManagerService.TAG, stringBuilder.toString(), didSomething3);
                    throw didSomething3;
                }
            }
            didSomething3 = didSomething2;
        }
        if (this.mRestartingServices.size() > 0) {
            while (true) {
                int i3 = i;
                if (i3 >= this.mRestartingServices.size()) {
                    break;
                }
                ServiceRecord sr2 = (ServiceRecord) this.mRestartingServices.get(i3);
                if (proc == sr2.isolatedProc || (proc.uid == sr2.appInfo.uid && processName.equals(sr2.processName))) {
                    this.mAm.mHandler.removeCallbacks(sr2.restarter);
                    this.mAm.mHandler.post(sr2.restarter);
                }
                i = i3 + 1;
            }
        }
        return didSomething3;
    }

    void processStartTimedOutLocked(ProcessRecord proc) {
        int i = 0;
        while (i < this.mPendingServices.size()) {
            ServiceRecord sr = (ServiceRecord) this.mPendingServices.get(i);
            if ((proc.uid == sr.appInfo.uid && proc.processName.equals(sr.processName)) || sr.isolatedProc == proc) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Forcing bringing down service: ");
                stringBuilder.append(sr);
                Slog.w(str, stringBuilder.toString());
                sr.isolatedProc = null;
                this.mPendingServices.remove(i);
                i--;
                bringDownServiceLocked(sr);
            }
            i++;
        }
    }

    private boolean collectPackageServicesLocked(String packageName, Set<String> filterByClasses, boolean evenPersistent, boolean doit, boolean killProcess, ArrayMap<ComponentName, ServiceRecord> services) {
        boolean didSomething = false;
        for (int i = services.size() - 1; i >= 0; i--) {
            ServiceRecord service = (ServiceRecord) services.valueAt(i);
            if (service != null) {
                boolean sameComponent = packageName == null || (service.packageName.equals(packageName) && (filterByClasses == null || filterByClasses.contains(service.name.getClassName())));
                if (sameComponent && (service.app == null || evenPersistent || !service.app.persistent)) {
                    if (!doit) {
                        return true;
                    }
                    didSomething = true;
                    String str = ActivityManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  Force stopping service ");
                    stringBuilder.append(service);
                    Slog.i(str, stringBuilder.toString());
                    if (service.app != null) {
                        service.app.removed = killProcess;
                        if (!service.app.persistent) {
                            service.app.services.remove(service);
                            if (service.whitelistManager) {
                                updateWhitelistManagerLocked(service.app);
                            }
                        }
                        if (service.app.executingServices.size() == 1 && service.app.executingServices.contains(service)) {
                            str = ActivityManagerService.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Remove timeout message for service: ");
                            stringBuilder.append(service);
                            Slog.w(str, stringBuilder.toString());
                            service.app.execServicesFg = false;
                            this.mAm.mHandler.removeMessages(12, service.app);
                            this.mAm.mHandler.removeMessages(99, service.app);
                        }
                        service.app.executingServices.remove(service);
                    }
                    service.app = null;
                    service.isolatedProc = null;
                    if (this.mTmpCollectionResults == null) {
                        this.mTmpCollectionResults = new ArrayList();
                    }
                    this.mTmpCollectionResults.add(service);
                }
            }
        }
        return didSomething;
    }

    boolean bringDownDisabledPackageServicesLocked(String packageName, Set<String> filterByClasses, int userId, boolean evenPersistent, boolean killProcess, boolean doit) {
        int i;
        String str = packageName;
        int i2 = userId;
        boolean didSomething = false;
        if (this.mTmpCollectionResults != null) {
            this.mTmpCollectionResults.clear();
        }
        if (i2 == -1) {
            i = this.mServiceMap.size() - 1;
            while (true) {
                int i3 = i;
                if (i3 < 0) {
                    break;
                }
                didSomething |= collectPackageServicesLocked(str, filterByClasses, evenPersistent, doit, killProcess, ((ServiceMap) this.mServiceMap.valueAt(i3)).mServicesByName);
                if (!doit && didSomething) {
                    return true;
                }
                if (doit && filterByClasses == null) {
                    forceStopPackageLocked(str, ((ServiceMap) this.mServiceMap.valueAt(i3)).mUserId);
                }
                i = i3 - 1;
            }
        } else {
            ServiceMap smap = (ServiceMap) this.mServiceMap.get(i2);
            if (smap != null) {
                didSomething = collectPackageServicesLocked(str, filterByClasses, evenPersistent, doit, killProcess, smap.mServicesByName);
            }
            if (doit && filterByClasses == null) {
                forceStopPackageLocked(str, i2);
            }
        }
        if (this.mTmpCollectionResults != null) {
            for (i = this.mTmpCollectionResults.size() - 1; i >= 0; i--) {
                bringDownServiceLocked((ServiceRecord) this.mTmpCollectionResults.get(i));
            }
            this.mTmpCollectionResults.clear();
        }
        return didSomething;
    }

    void forceStopPackageLocked(String packageName, int userId) {
        ServiceMap smap = (ServiceMap) this.mServiceMap.get(userId);
        if (smap != null && smap.mActiveForegroundApps.size() > 0) {
            for (int i = smap.mActiveForegroundApps.size() - 1; i >= 0; i--) {
                if (((ActiveForegroundApp) smap.mActiveForegroundApps.valueAt(i)).mPackageName.equals(packageName)) {
                    smap.mActiveForegroundApps.removeAt(i);
                    smap.mActiveForegroundAppsChanged = true;
                }
            }
            if (smap.mActiveForegroundAppsChanged) {
                requestUpdateActiveForegroundAppsLocked(smap, 0);
            }
        }
    }

    void cleanUpRemovedTaskLocked(TaskRecord tr, ComponentName component, Intent baseIntent) {
        int i;
        ArrayList<ServiceRecord> services = new ArrayList();
        ArrayMap<ComponentName, ServiceRecord> alls = getServicesLocked(tr.userId);
        for (i = alls.size() - 1; i >= 0; i--) {
            ServiceRecord sr = (ServiceRecord) alls.valueAt(i);
            if (sr != null && sr.packageName.equals(component.getPackageName())) {
                services.add(sr);
            }
        }
        if (services.size() > 0) {
            LogPower.push(148, "cleanUpservice", component.getPackageName());
        }
        i = services.size() - 1;
        while (true) {
            int i2 = i;
            if (i2 >= 0) {
                ServiceRecord sr2 = (ServiceRecord) services.get(i2);
                if (sr2.startRequested) {
                    if ((sr2.serviceInfo.flags & 1) != 0) {
                        String str = ActivityManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Stopping service ");
                        stringBuilder.append(sr2.shortName);
                        stringBuilder.append(": remove task");
                        Slog.i(str, stringBuilder.toString());
                        stopServiceLocked(sr2);
                    } else {
                        sr2.pendingStarts.add(new StartItem(sr2, true, sr2.getLastStartId(), baseIntent, null, 0));
                        if (!(sr2.app == null || sr2.app.thread == null)) {
                            try {
                                sendServiceArgsLocked(sr2, true, false);
                            } catch (TransactionTooLargeException e) {
                            }
                        }
                    }
                }
                i = i2 - 1;
            } else {
                return;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:143:0x0281 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0265  */
    /* JADX WARNING: Removed duplicated region for block: B:143:0x0281 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0265  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final void killServicesLocked(ProcessRecord app, boolean allowRestart) {
        IBinder iBinder;
        ServiceRecord r;
        ProcessRecord processRecord = app;
        int i = 1;
        int i2 = processRecord.connections.size() - 1;
        while (true) {
            iBinder = null;
            if (i2 < 0) {
                break;
            }
            removeConnectionLocked((ConnectionRecord) processRecord.connections.valueAt(i2), processRecord, null);
            i2--;
        }
        updateServiceConnectionActivitiesLocked(app);
        processRecord.connections.clear();
        processRecord.whitelistManager = false;
        int i3 = processRecord.services.size() - 1;
        while (i3 >= 0) {
            ServiceRecord sr = (ServiceRecord) processRecord.services.valueAt(i3);
            synchronized (sr.stats.getBatteryStats()) {
                sr.stats.stopLaunchedLocked();
            }
            if (!(sr.app == processRecord || sr.app == null || sr.app.persistent)) {
                sr.app.services.remove(sr);
            }
            sr.app = iBinder;
            sr.isolatedProc = iBinder;
            sr.executeNesting = 0;
            sr.forceClearTracker();
            if (this.mDestroyingServices.remove(sr) && ActivityManagerDebugConfig.DEBUG_SERVICE) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("killServices remove destroying ");
                stringBuilder.append(sr);
                Slog.v(str, stringBuilder.toString());
            }
            int bindingi = sr.bindings.size() - 1;
            while (bindingi >= 0) {
                IntentBindRecord b = (IntentBindRecord) sr.bindings.valueAt(bindingi);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    String str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Killing binding ");
                    stringBuilder2.append(b);
                    stringBuilder2.append(": shouldUnbind=");
                    stringBuilder2.append(b.hasBound);
                    Slog.v(str2, stringBuilder2.toString());
                }
                b.binder = iBinder;
                b.hasBound = false;
                b.received = false;
                b.requested = false;
                for (int appi = b.apps.size() - 1; appi >= 0; appi--) {
                    ProcessRecord proc = (ProcessRecord) b.apps.keyAt(appi);
                    if (!(proc.killedByAm || proc.thread == null)) {
                        AppBindRecord abind = (AppBindRecord) b.apps.valueAt(appi);
                        boolean hasCreate = false;
                        for (int conni = abind.connections.size() - 1; conni >= 0; conni--) {
                            if ((((ConnectionRecord) abind.connections.valueAt(conni)).flags & 49) == 1) {
                                hasCreate = true;
                                break;
                            }
                        }
                        if (!hasCreate) {
                        }
                    }
                }
                bindingi--;
                iBinder = null;
            }
            i3--;
            iBinder = null;
        }
        ServiceMap smap = getServiceMapLocked(processRecord.userId);
        int i4 = processRecord.services.size() - 1;
        boolean preventRestart = false;
        boolean getPreventResult = false;
        boolean allowRestart2 = allowRestart;
        while (i4 >= 0) {
            ServiceMap smap2;
            ServiceRecord sr2 = (ServiceRecord) processRecord.services.valueAt(i4);
            if (!processRecord.persistent) {
                processRecord.services.removeAt(i4);
            }
            ServiceRecord curRec = (ServiceRecord) smap.mServicesByName.get(sr2.name);
            StringBuilder stringBuilder3;
            String str3;
            if (curRec == sr2) {
                if (allowRestart2) {
                    allowRestart2 = this.mAm.isAcquireAppServiceResourceLocked(sr2, processRecord);
                    if (!allowRestart2) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Service ");
                        stringBuilder3.append(sr2);
                        stringBuilder3.append(" in process ");
                        stringBuilder3.append(processRecord);
                        stringBuilder3.append(" prevent restart by RMS srname: ");
                        stringBuilder3.append(sr2.name);
                        Flog.i(102, stringBuilder3.toString());
                    }
                }
                if (allowRestart2 && ((long) sr2.crashCount) >= this.mAm.mConstants.BOUND_SERVICE_MAX_CRASH_RETRY && (sr2.serviceInfo.applicationInfo.flags & 8) == 0) {
                    str3 = ActivityManagerService.TAG;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Service crashed ");
                    stringBuilder3.append(sr2.crashCount);
                    stringBuilder3.append(" times, stopping: ");
                    stringBuilder3.append(sr2);
                    Slog.w(str3, stringBuilder3.toString());
                    EventLog.writeEvent(EventLogTags.AM_SERVICE_CRASHED_TOO_MUCH, new Object[]{Integer.valueOf(sr2.userId), Integer.valueOf(sr2.crashCount), sr2.shortName, Integer.valueOf(processRecord.pid)});
                    bringDownServiceLocked(sr2);
                } else {
                    if (!allowRestart2) {
                        smap2 = smap;
                    } else if (this.mAm.mUserController.isUserRunning(sr2.userId, 0)) {
                        boolean canceled = scheduleServiceRestartLocked(sr2, i);
                        boolean bringDown = false;
                        if (sr2.startRequested) {
                            if (!sr2.stopIfKilled && !canceled) {
                                smap2 = smap;
                                if (!bringDown) {
                                }
                                i4--;
                                smap = smap2;
                                i = 1;
                            } else if (sr2.pendingStarts.size() == 0) {
                                sr2.startRequested = false;
                                if (sr2.tracker != null) {
                                    smap2 = smap;
                                    sr2.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                                } else {
                                    smap2 = smap;
                                }
                                if (!sr2.hasAutoCreateConnections()) {
                                    bringDownServiceLocked(sr2);
                                    bringDown = true;
                                }
                                if (!bringDown) {
                                    if (!getPreventResult) {
                                        preventRestart = this.mAm.shouldPreventRestartService(sr2.serviceInfo, false);
                                        getPreventResult = true;
                                    }
                                    boolean allowRestart3 = !preventRestart;
                                    if (!allowRestart3) {
                                        bringDownServiceLocked(sr2);
                                    }
                                    allowRestart2 = allowRestart3;
                                }
                                i4--;
                                smap = smap2;
                                i = 1;
                            }
                        }
                        smap2 = smap;
                        if (!bringDown) {
                        }
                        i4--;
                        smap = smap2;
                        i = 1;
                    } else {
                        smap2 = smap;
                    }
                    bringDownServiceLocked(sr2);
                    i4--;
                    smap = smap2;
                    i = 1;
                }
            } else if (curRec != null) {
                str3 = ActivityManagerService.TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Service ");
                stringBuilder3.append(sr2);
                stringBuilder3.append(" in process ");
                stringBuilder3.append(processRecord);
                stringBuilder3.append(" not same as in map: ");
                stringBuilder3.append(curRec);
                Slog.wtf(str3, stringBuilder3.toString());
            }
            smap2 = smap;
            i4--;
            smap = smap2;
            i = 1;
        }
        if (!allowRestart2) {
            processRecord.services.clear();
            for (i2 = this.mRestartingServices.size() - 1; i2 >= 0; i2--) {
                r = (ServiceRecord) this.mRestartingServices.get(i2);
                if (r.processName.equals(processRecord.processName) && r.serviceInfo.applicationInfo.uid == processRecord.info.uid) {
                    this.mRestartingServices.remove(i2);
                    clearRestartingIfNeededLocked(r);
                }
            }
            for (i2 = this.mPendingServices.size() - 1; i2 >= 0; i2--) {
                r = (ServiceRecord) this.mPendingServices.get(i2);
                if (r.processName.equals(processRecord.processName) && r.serviceInfo.applicationInfo.uid == processRecord.info.uid) {
                    this.mPendingServices.remove(i2);
                }
            }
        }
        i2 = this.mDestroyingServices.size();
        while (i2 > 0) {
            i2--;
            r = (ServiceRecord) this.mDestroyingServices.get(i2);
            if (r.app == processRecord) {
                r.forceClearTracker();
                this.mDestroyingServices.remove(i2);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    String str4 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("killServices remove destroying ");
                    stringBuilder4.append(r);
                    Slog.v(str4, stringBuilder4.toString());
                }
            }
        }
        processRecord.executingServices.clear();
    }

    RunningServiceInfo makeRunningServiceInfoLocked(ServiceRecord r) {
        RunningServiceInfo info = new RunningServiceInfo();
        info.service = r.name;
        if (r.app != null) {
            info.pid = r.app.pid;
        }
        info.uid = r.appInfo.uid;
        info.process = r.processName;
        info.foreground = r.isForeground;
        info.activeSince = r.createRealTime;
        info.started = r.startRequested;
        info.clientCount = r.connections.size();
        info.crashCount = r.crashCount;
        info.lastActivityTime = r.lastActivity;
        if (r.isForeground) {
            info.flags |= 2;
        }
        if (r.startRequested) {
            info.flags |= 1;
        }
        if (r.app != null && r.app.pid == ActivityManagerService.MY_PID) {
            info.flags |= 4;
        }
        if (r.app != null && r.app.persistent) {
            info.flags |= 8;
        }
        for (int conni = r.connections.size() - 1; conni >= 0; conni--) {
            ArrayList<ConnectionRecord> connl = (ArrayList) r.connections.valueAt(conni);
            for (int i = 0; i < connl.size(); i++) {
                ConnectionRecord conn = (ConnectionRecord) connl.get(i);
                if (conn.clientLabel != 0) {
                    info.clientPackage = conn.binding.client.info.packageName;
                    info.clientLabel = conn.clientLabel;
                    return info;
                }
            }
        }
        return info;
    }

    List<RunningServiceInfo> getRunningServiceInfoLocked(int maxNum, int flags, int callingUid, boolean allowed, boolean canInteractAcrossUsers) {
        ArrayList<RunningServiceInfo> res = new ArrayList();
        long ident = Binder.clearCallingIdentity();
        int i = 0;
        if (canInteractAcrossUsers) {
            try {
                int[] users = this.mAm.mUserController.getUsers();
                for (int ui = 0; ui < users.length && res.size() < maxNum; ui++) {
                    ArrayMap<ComponentName, ServiceRecord> alls = getServicesLocked(users[ui]);
                    for (int i2 = 0; i2 < alls.size() && res.size() < maxNum; i2++) {
                        ServiceRecord sr = (ServiceRecord) alls.valueAt(i2);
                        if (sr != null) {
                            res.add(makeRunningServiceInfoLocked(sr));
                        }
                    }
                }
                while (i < this.mRestartingServices.size() && res.size() < maxNum) {
                    ServiceRecord r = (ServiceRecord) this.mRestartingServices.get(i);
                    if (r != null) {
                        RunningServiceInfo info = makeRunningServiceInfoLocked(r);
                        info.restarting = r.nextRestartTime;
                        res.add(info);
                    }
                    i++;
                }
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        } else {
            int userId = UserHandle.getUserId(callingUid);
            ArrayMap<ComponentName, ServiceRecord> alls2 = getServicesLocked(userId);
            for (int i3 = 0; i3 < alls2.size() && res.size() < maxNum; i3++) {
                ServiceRecord sr2 = (ServiceRecord) alls2.valueAt(i3);
                if ((allowed || !(sr2 == null || sr2.app == null || sr2.app.uid != callingUid)) && sr2 != null) {
                    res.add(makeRunningServiceInfoLocked(sr2));
                }
            }
            while (i < this.mRestartingServices.size() && res.size() < maxNum) {
                ServiceRecord r2 = (ServiceRecord) this.mRestartingServices.get(i);
                if (r2.userId == userId && ((allowed || !(r2 == null || r2.app == null || r2.app.uid != callingUid)) && r2 != null)) {
                    RunningServiceInfo info2 = makeRunningServiceInfoLocked(r2);
                    info2.restarting = r2.nextRestartTime;
                    res.add(info2);
                }
                i++;
            }
        }
        Binder.restoreCallingIdentity(ident);
        return res;
    }

    public PendingIntent getRunningServiceControlPanelLocked(ComponentName name) {
        ServiceRecord r = getServiceByNameLocked(name, UserHandle.getUserId(Binder.getCallingUid()));
        if (r != null) {
            for (int conni = r.connections.size() - 1; conni >= 0; conni--) {
                ArrayList<ConnectionRecord> conn = (ArrayList) r.connections.valueAt(conni);
                for (int i = 0; i < conn.size(); i++) {
                    if (((ConnectionRecord) conn.get(i)).clientIntent != null) {
                        return ((ConnectionRecord) conn.get(i)).clientIntent;
                    }
                }
            }
        }
        return null;
    }

    /* JADX WARNING: Missing block: B:66:0x0197, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
     */
    /* JADX WARNING: Missing block: B:67:0x019a, code skipped:
            if (r16 == null) goto L_0x01a9;
     */
    /* JADX WARNING: Missing block: B:68:0x019c, code skipped:
            r1.mAm.mAppErrors.appNotResponding(r8, null, null, false, r16);
     */
    /* JADX WARNING: Missing block: B:69:0x01a9, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void serviceTimeout(ProcessRecord proc) {
        String anrMessage;
        Boolean frozenAnr;
        Throwable th;
        ProcessRecord processRecord = proc;
        String anrMessage2 = null;
        Boolean frozenAnr2 = Boolean.valueOf(false);
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (processRecord.executingServices.size() == 0) {
                    anrMessage = null;
                    frozenAnr = frozenAnr2;
                } else if (processRecord.thread == null) {
                    anrMessage = null;
                    frozenAnr = frozenAnr2;
                } else {
                    int i;
                    Boolean frozenAnr3;
                    long now = SystemClock.uptimeMillis();
                    if (processRecord.execServicesFg) {
                        try {
                            i = SERVICE_TIMEOUT;
                        } catch (Throwable th2) {
                            th = th2;
                        }
                    } else {
                        i = SERVICE_BACKGROUND_TIMEOUT;
                    }
                    long maxTime = now - ((long) i);
                    ServiceRecord timeout = null;
                    long nextTime = 0;
                    int i2 = 1;
                    int i3 = processRecord.executingServices.size() - 1;
                    while (i3 >= 0) {
                        int i4;
                        ServiceRecord sr = (ServiceRecord) processRecord.executingServices.valueAt(i3);
                        if (processRecord.execServicesFg) {
                            anrMessage = anrMessage2;
                            frozenAnr = frozenAnr2;
                            try {
                                ServiceRecord sr2 = sr;
                                if (sr.executingStart < now - ((long) CHECK_INTERVAL)) {
                                    anrMessage2 = sr2;
                                    if (this.mAm.isTopProcessLocked(anrMessage2.app)) {
                                        timeout = anrMessage2;
                                        frozenAnr2 = Boolean.valueOf(true);
                                        frozenAnr3 = frozenAnr2;
                                        break;
                                    }
                                    i4 = 1;
                                } else {
                                    anrMessage2 = sr2;
                                    i4 = 1;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                anrMessage2 = anrMessage;
                                frozenAnr2 = frozenAnr;
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                throw th;
                            }
                        }
                        anrMessage = anrMessage2;
                        frozenAnr = frozenAnr2;
                        i4 = i2;
                        anrMessage2 = sr;
                        if (anrMessage2.executingStart < maxTime) {
                            timeout = anrMessage2;
                            frozenAnr2 = Boolean.valueOf(false);
                            frozenAnr3 = frozenAnr2;
                            break;
                        }
                        if (anrMessage2.executingStart > nextTime) {
                            nextTime = anrMessage2.executingStart;
                        }
                        i3--;
                        i2 = i4;
                        anrMessage2 = anrMessage;
                        frozenAnr2 = frozenAnr;
                    }
                    anrMessage = anrMessage2;
                    frozenAnr3 = frozenAnr2;
                    if (timeout != null) {
                        try {
                            if (this.mAm.mLruProcesses.contains(processRecord)) {
                                String str;
                                if (frozenAnr3.booleanValue()) {
                                    anrMessage2 = (((long) SERVICE_TIMEOUT) + timeout.executingStart) - now;
                                    str = ActivityManagerService.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("ANR has been triggered ");
                                    stringBuilder.append(anrMessage2);
                                    stringBuilder.append("ms earlier because it caused frozen problem.");
                                    Slog.w(str, stringBuilder.toString());
                                }
                                str = ActivityManagerService.TAG;
                                anrMessage2 = new StringBuilder();
                                anrMessage2.append("Timeout executing service: ");
                                anrMessage2.append(timeout);
                                Slog.w(str, anrMessage2.toString());
                                StringWriter sw = new StringWriter();
                                anrMessage2 = new FastPrintWriter(sw, false, 1024);
                                anrMessage2.println(timeout);
                                timeout.dump(anrMessage2, "    ");
                                anrMessage2.close();
                                this.mLastAnrDump = sw.toString();
                                this.mAm.mHandler.removeCallbacks(this.mLastAnrDumpClearer);
                                this.mAm.mHandler.postDelayed(this.mLastAnrDumpClearer, SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT);
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("executing service ");
                                stringBuilder2.append(timeout.shortName);
                                anrMessage = stringBuilder2.toString();
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            frozenAnr2 = frozenAnr3;
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    long j = maxTime;
                    Message msg = this.mAm.mHandler.obtainMessage(12);
                    msg.obj = processRecord;
                    if (nextTime == 0) {
                        Slog.e(ActivityManagerService.TAG, "nextTime invaild, remove the message");
                        this.mAm.mHandler.removeMessages(12, processRecord);
                    } else if (((long) CHECK_INTERVAL) + nextTime < now) {
                        this.mAm.mHandler.sendMessageAtTime(msg, ((long) (processRecord.execServicesFg ? SERVICE_TIMEOUT : SERVICE_BACKGROUND_TIMEOUT)) + nextTime);
                    } else {
                        this.mAm.mHandler.sendMessageAtTime(msg, ((long) (processRecord.execServicesFg ? CHECK_INTERVAL : SERVICE_BACKGROUND_TIMEOUT)) + nextTime);
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th5) {
                th = th5;
                anrMessage = null;
                frozenAnr = frozenAnr2;
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:25:0x0060=Splitter:B:25:0x0060, B:15:0x001c=Splitter:B:15:0x001c} */
    /* JADX WARNING: Missing block: B:21:0x003d, code skipped:
            com.android.server.am.ActivityManagerService.resetPriorityAfterLockedSection();
            r0 = r1;
     */
    /* JADX WARNING: Missing block: B:22:0x0041, code skipped:
            if (r0 == null) goto L_0x005f;
     */
    /* JADX WARNING: Missing block: B:23:0x0043, code skipped:
            r3 = r9.mAm.mAppErrors;
            r1 = new java.lang.StringBuilder();
            r1.append("Context.startForegroundService() did not then call Service.startForeground(): ");
            r1.append(r10);
            r3.appNotResponding(r0, null, null, false, r1.toString());
     */
    /* JADX WARNING: Missing block: B:24:0x005f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void serviceForegroundTimeout(ServiceRecord r) {
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (r.fgRequired) {
                    if (!r.destroying) {
                        ProcessRecord app = r.app;
                        if (app == null || !app.debugging) {
                            if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                                String str = ActivityManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Service foreground-required timeout for ");
                                stringBuilder.append(r);
                                Slog.i(str, stringBuilder.toString());
                            }
                            r.fgWaiting = false;
                            stopServiceLocked(r);
                        } else {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void updateServiceApplicationInfoLocked(ApplicationInfo applicationInfo) {
        ServiceMap serviceMap = (ServiceMap) this.mServiceMap.get(UserHandle.getUserId(applicationInfo.uid));
        if (serviceMap != null) {
            ArrayMap<ComponentName, ServiceRecord> servicesByName = serviceMap.mServicesByName;
            for (int j = servicesByName.size() - 1; j >= 0; j--) {
                ServiceRecord serviceRecord = (ServiceRecord) servicesByName.valueAt(j);
                if (applicationInfo.packageName.equals(serviceRecord.appInfo.packageName)) {
                    serviceRecord.appInfo = applicationInfo;
                    serviceRecord.serviceInfo.applicationInfo = applicationInfo;
                }
            }
        }
    }

    void serviceForegroundCrash(ProcessRecord app, CharSequence serviceRecord) {
        ActivityManagerService activityManagerService = this.mAm;
        int i = app.uid;
        int i2 = app.pid;
        String str = app.info.packageName;
        int i3 = app.userId;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Context.startForegroundService() did not then call Service.startForeground(): ");
        stringBuilder.append(serviceRecord);
        activityManagerService.crashApplication(i, i2, str, i3, stringBuilder.toString());
    }

    void scheduleServiceTimeoutLocked(ProcessRecord proc) {
        if (proc.executingServices.size() == 0 || proc.thread == null) {
            Flog.i(102, "no need to schedule service timeout");
            return;
        }
        Message msg = this.mAm.mHandler.obtainMessage(12);
        msg.obj = proc;
        this.mAm.mHandler.sendMessageDelayed(msg, (long) (proc.execServicesFg ? CHECK_INTERVAL : SERVICE_BACKGROUND_TIMEOUT));
        msg = this.mAm.mHandler.obtainMessage(99);
        msg.obj = proc;
        this.mAm.mHandler.sendMessageDelayed(msg, 3000);
    }

    void scheduleServiceForegroundTransitionTimeoutLocked(ServiceRecord r) {
        if (r.app.executingServices.size() != 0 && r.app.thread != null) {
            Message msg = this.mAm.mHandler.obtainMessage(66);
            msg.obj = r;
            r.fgWaiting = true;
            this.mAm.mHandler.sendMessageDelayed(msg, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
    }

    ServiceDumper newServiceDumperLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        return new ServiceDumper(this, fd, pw, args, opti, dumpAll, dumpPackage);
    }

    protected void writeToProto(ProtoOutputStream proto, long fieldId) {
        ProtoOutputStream protoOutputStream = proto;
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long outterToken = proto.start(fieldId);
                int[] users = this.mAm.mUserController.getUsers();
                int length = users.length;
                int i = 0;
                while (i < length) {
                    int i2;
                    int user = users[i];
                    ServiceMap smap = (ServiceMap) this.mServiceMap.get(user);
                    if (smap == null) {
                        i2 = i;
                    } else {
                        long token = protoOutputStream.start(2246267895809L);
                        protoOutputStream.write(1120986464257L, user);
                        ArrayMap<ComponentName, ServiceRecord> alls = smap.mServicesByName;
                        int i3 = 0;
                        while (i3 < alls.size()) {
                            i2 = i;
                            ((ServiceRecord) alls.valueAt(i3)).writeToProto(protoOutputStream, 2246267895810L);
                            i3++;
                            i = i2;
                        }
                        i2 = i;
                        protoOutputStream.end(token);
                    }
                    i = i2 + 1;
                }
                protoOutputStream.end(outterToken);
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    protected boolean dumpService(FileDescriptor fd, PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        int i;
        int i2;
        ArrayList<ServiceRecord> services = new ArrayList();
        Predicate<ServiceRecord> filter = DumpUtils.filterRecord(name);
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                i = 0;
                for (int user : this.mAm.mUserController.getUsers()) {
                    ServiceMap smap = (ServiceMap) this.mServiceMap.get(user);
                    if (smap != null) {
                        ArrayMap<ComponentName, ServiceRecord> alls = smap.mServicesByName;
                        for (i2 = 0; i2 < alls.size(); i2++) {
                            ServiceRecord r1 = (ServiceRecord) alls.valueAt(i2);
                            if (filter.test(r1)) {
                                services.add(r1);
                            }
                        }
                    }
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        if (services.size() <= 0) {
            return false;
        }
        services.sort(Comparator.comparing(-$$Lambda$Y_KRxxoOXfy-YceuDG7WHd46Y_I.INSTANCE));
        boolean needSep = false;
        while (true) {
            i2 = i;
            if (i2 >= services.size()) {
                return true;
            }
            if (needSep) {
                pw.println();
            }
            needSep = true;
            dumpService(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, fd, pw, (ServiceRecord) services.get(i2), args, dumpAll);
            i = i2 + 1;
        }
    }

    private void dumpService(String prefix, FileDescriptor fd, PrintWriter pw, ServiceRecord r, String[] args, boolean dumpAll) {
        String innerPrefix = new StringBuilder();
        innerPrefix.append(prefix);
        innerPrefix.append("  ");
        innerPrefix = innerPrefix.toString();
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                pw.print(prefix);
                pw.print("SERVICE ");
                pw.print(r.shortName);
                pw.print(" ");
                pw.print(Integer.toHexString(System.identityHashCode(r)));
                pw.print(" pid=");
                if (r.app != null) {
                    pw.println(r.app.pid);
                } else {
                    pw.println("(not running)");
                }
                if (dumpAll) {
                    r.dump(pw, innerPrefix);
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        if (r.app != null && r.app.thread != null) {
            pw.print(prefix);
            pw.println("  Client:");
            pw.flush();
            TransferPipe tp;
            StringBuilder stringBuilder;
            try {
                tp = new TransferPipe();
                r.app.thread.dumpService(tp.getWriteFd(), r, args);
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("    ");
                tp.setBufferPrefix(stringBuilder.toString());
                tp.go(fd);
                tp.kill();
            } catch (IOException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("    Failure while dumping the service: ");
                stringBuilder.append(e);
                pw.println(stringBuilder.toString());
            } catch (RemoteException e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(prefix);
                stringBuilder.append("    Got a RemoteException while dumping the service");
                pw.println(stringBuilder.toString());
            } catch (Throwable th) {
                tp.kill();
            }
        }
    }

    private String[] initPCEntryArgs(ServiceRecord r) {
        if (!HwPCUtils.isPcCastModeInServer()) {
            return null;
        }
        String[] entryPointArgs = null;
        if (TextUtils.equals(r.intent.getIntent().getAction(), "android.view.InputMethod")) {
            WindowManagerInternal wmi = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
            if (wmi != null && HwPCUtils.enabledInPad()) {
                entryPointArgs = new String[]{String.valueOf(HwPCUtils.getPCDisplayID())};
            } else if (wmi == null || !wmi.isHardKeyboardAvailable()) {
                entryPointArgs = new String[]{String.valueOf(0)};
            } else {
                entryPointArgs = new String[]{String.valueOf(this.mAm.mWindowManager.getFocusedDisplayId())};
            }
        }
        if (TextUtils.equals(r.appInfo.packageName, "com.huawei.desktop.systemui") || TextUtils.equals(r.appInfo.packageName, "com.huawei.desktop.explorer")) {
            entryPointArgs = new String[]{String.valueOf(-HwPCUtils.getPCDisplayID())};
        }
        return entryPointArgs;
    }
}
