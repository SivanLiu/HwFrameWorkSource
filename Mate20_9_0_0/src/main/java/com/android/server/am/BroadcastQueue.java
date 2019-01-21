package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.hsm.HwSystemManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.util.EventLog;
import android.util.Flog;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.server.HwServiceFactory;
import com.android.server.display.DisplayTransformManager;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.policy.HwPolicyFactory;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import com.android.server.wm.WindowState;
import com.huawei.pgmng.common.Utils;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public class BroadcastQueue extends AbsBroadcastQueue {
    static final int BROADCAST_CHECKTIMEOUT_MSG = 203;
    static final int BROADCAST_INTENT_MSG = 200;
    static final int BROADCAST_TIMEOUT_MSG = 201;
    static final int CHECK_INTERVAL = ((int) (10000.0f * ActivityManagerService.SCALE_ANR));
    static final int MAX_BROADCAST_HISTORY;
    static final int MAX_BROADCAST_SUMMARY_HISTORY;
    static final int MAYBE_BROADCAST_BG_TIMEOUT = 5000;
    static final int MAYBE_BROADCAST_FG_TIMEOUT = 2000;
    static final int SCHEDULE_TEMP_WHITELIST_MSG = 202;
    private static final String TAG = "BroadcastQueue";
    private static final String TAG_BROADCAST = "BroadcastQueue";
    private static final String TAG_MU = "BroadcastQueue_MU";
    private static final boolean mIsBetaUser;
    final BroadcastRecord[] mBroadcastHistory = new BroadcastRecord[MAX_BROADCAST_HISTORY];
    final Intent[] mBroadcastSummaryHistory = new Intent[MAX_BROADCAST_SUMMARY_HISTORY];
    boolean mBroadcastsScheduled = false;
    final boolean mDelayBehindServices;
    final BroadcastHandler mHandler;
    int mHistoryNext = 0;
    final ArrayList<BroadcastRecord> mOrderedBroadcasts = new ArrayList();
    final ArrayList<BroadcastRecord> mParallelBroadcasts = new ArrayList();
    BroadcastRecord mPendingBroadcast = null;
    int mPendingBroadcastRecvIndex;
    boolean mPendingBroadcastTimeoutMessage;
    final String mQueueName;
    final ActivityManagerService mService;
    final long[] mSummaryHistoryDispatchTime = new long[MAX_BROADCAST_SUMMARY_HISTORY];
    final long[] mSummaryHistoryEnqueueTime = new long[MAX_BROADCAST_SUMMARY_HISTORY];
    final long[] mSummaryHistoryFinishTime = new long[MAX_BROADCAST_SUMMARY_HISTORY];
    int mSummaryHistoryNext = 0;
    final long mTimeoutPeriod;

    private final class AppNotResponding implements Runnable {
        private static final String GET_FOCUSED_WINDOW_METHOD_NAME = "getFocusedWindow";
        private static final String WINDOW_MANAGER_SERVICE_CLASS_NAME = "com.android.server.wm.WindowManagerService";
        private final String mAnnotation;
        private final ProcessRecord mApp;

        public AppNotResponding(ProcessRecord app, String annotation) {
            this.mApp = app;
            this.mAnnotation = annotation;
        }

        public void run() {
            BroadcastQueue.this.mService.mAppErrors.appNotResponding(this.mApp, null, null, isAboveSystem(), this.mAnnotation);
        }

        private boolean isAboveSystem() {
            WindowState focusedWindow = getCurFocusedWindow();
            return focusedWindow == null || windowTypeToLayerLw(focusedWindow.mAttrs.type, focusedWindow.canAddInternalSystemWindow()) >= windowTypeToLayerLw(2003, true);
        }

        private int windowTypeToLayerLw(int type, boolean canAddInternalSystemWindow) {
            return HwPolicyFactory.getHwPhoneWindowManager().getWindowLayerFromTypeLw(type, canAddInternalSystemWindow);
        }

        private WindowState getCurFocusedWindow() {
            try {
                Class<?> cls = Class.forName(WINDOW_MANAGER_SERVICE_CLASS_NAME);
                if (cls != null) {
                    Method method = cls.getDeclaredMethod(GET_FOCUSED_WINDOW_METHOD_NAME, new Class[0]);
                    if (method != null) {
                        method.setAccessible(true);
                        return (WindowState) method.invoke(BroadcastQueue.this.mService.mWindowManager, new Object[0]);
                    }
                }
            } catch (Exception e) {
                Slog.e("BroadcastQueue", "BroadcastQueue AppNotResponding getCurFocusedWindow failed", e);
            }
            return null;
        }
    }

    private final class BroadcastHandler extends Handler {
        public BroadcastHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 200:
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        Slog.v("BroadcastQueue", "Received BROADCAST_INTENT_MSG");
                    }
                    BroadcastQueue.this.processNextBroadcast(true);
                    return;
                case BroadcastQueue.BROADCAST_TIMEOUT_MSG /*201*/:
                    synchronized (BroadcastQueue.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            BroadcastQueue.this.broadcastTimeoutLocked(true);
                        } finally {
                            while (true) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                break;
                            }
                        }
                    }
                    return;
                case BroadcastQueue.BROADCAST_CHECKTIMEOUT_MSG /*203*/:
                    synchronized (BroadcastQueue.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            BroadcastQueue.this.handleMaybeTimeoutBC();
                            BroadcastQueue.this.uploadRadarMessage(2803, null);
                        } finally {
                            while (true) {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                break;
                            }
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }

    static {
        boolean z = false;
        int i = (ActivityManager.isLowRamDeviceStatic() || SystemProperties.getBoolean("ro.config.hw_low_ram", false)) ? 10 : 50;
        MAX_BROADCAST_HISTORY = i;
        i = (ActivityManager.isLowRamDeviceStatic() || SystemProperties.getBoolean("ro.config.hw_low_ram", false)) ? 25 : DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR;
        MAX_BROADCAST_SUMMARY_HISTORY = i;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) == 3) {
            z = true;
        }
        mIsBetaUser = z;
    }

    private void handleMaybeTimeoutBC() {
        if (this.mOrderedBroadcasts.size() == 0) {
            Slog.w("BroadcastQueue", "handleMaybeTimeoutBC, but mOrderedBroadcasts is null");
            return;
        }
        BroadcastRecord r = (BroadcastRecord) this.mOrderedBroadcasts.get(0);
        if (r.nextReceiver <= 0) {
            Slog.w("BroadcastQueue", "handleMaybeTimeoutBC Timeout on receiver with nextReceiver <= 0");
            return;
        }
        StringBuilder stringBuilder;
        String pkg = null;
        String pid = null;
        String target = null;
        BroadcastFilter curReceiver = r.receivers.get(r.nextReceiver - 1);
        if (curReceiver instanceof BroadcastFilter) {
            BroadcastFilter bf = curReceiver;
            pkg = bf.packageName;
            if (bf.receiverList != null) {
                int iPid = bf.receiverList.pid;
                if (iPid <= 0 && bf.receiverList.app != null) {
                    iPid = bf.receiverList.app.pid;
                }
                pid = String.valueOf(iPid);
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("PackageName:");
            stringBuilder.append(pkg);
            target = stringBuilder.toString();
        } else if (curReceiver instanceof ResolveInfo) {
            ResolveInfo info = (ResolveInfo) curReceiver;
            if (info.activityInfo != null) {
                pkg = info.activityInfo.applicationInfo.packageName;
                stringBuilder = new StringBuilder(128);
                stringBuilder.append("ReceiverName:");
                ComponentName.appendShortString(stringBuilder, pkg, info.activityInfo.name);
                target = stringBuilder.toString();
            }
        }
        Utils.handleTimeOut("broadcast", pkg, pid);
        ActivityManagerService activityManagerService = this.mService;
        stringBuilder = new StringBuilder();
        stringBuilder.append(target);
        stringBuilder.append("+ActionName:");
        stringBuilder.append(r.intent.getAction());
        activityManagerService.checkOrderedBroadcastTimeoutLocked(stringBuilder.toString(), 0, false);
    }

    BroadcastQueue(ActivityManagerService service, Handler handler, String name, long timeoutPeriod, boolean allowDelayBehindServices) {
        this.mService = service;
        this.mHandler = new BroadcastHandler(handler.getLooper());
        this.mQueueName = name;
        this.mTimeoutPeriod = timeoutPeriod;
        this.mDelayBehindServices = allowDelayBehindServices;
    }

    public String toString() {
        return this.mQueueName;
    }

    public boolean isPendingBroadcastProcessLocked(int pid) {
        return this.mPendingBroadcast != null && this.mPendingBroadcast.curApp.pid == pid;
    }

    public void enqueueParallelBroadcastLocked(BroadcastRecord r) {
        this.mParallelBroadcasts.add(r);
        enqueueBroadcastHelper(r);
    }

    public void enqueueOrderedBroadcastLocked(BroadcastRecord r) {
        this.mOrderedBroadcasts.add(r);
        enqueueBroadcastHelper(r);
    }

    private void enqueueBroadcastHelper(BroadcastRecord r) {
        r.enqueueClockTime = System.currentTimeMillis();
        if (Trace.isTagEnabled(64)) {
            Trace.asyncTraceBegin(64, createBroadcastTraceTitle(r, 0), System.identityHashCode(r));
        }
    }

    public final BroadcastRecord replaceParallelBroadcastLocked(BroadcastRecord r) {
        return replaceBroadcastLocked(this.mParallelBroadcasts, r, "PARALLEL");
    }

    public final BroadcastRecord replaceOrderedBroadcastLocked(BroadcastRecord r) {
        return replaceBroadcastLocked(this.mOrderedBroadcasts, r, "ORDERED");
    }

    private BroadcastRecord replaceBroadcastLocked(ArrayList<BroadcastRecord> queue, BroadcastRecord r, String typeForLogging) {
        Intent intent = r.intent;
        for (int i = queue.size() - 1; i > 0; i--) {
            BroadcastRecord old = (BroadcastRecord) queue.get(i);
            if (old.userId == r.userId && intent.filterEquals(old.intent)) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("***** DROPPING ");
                    stringBuilder.append(typeForLogging);
                    stringBuilder.append(" [");
                    stringBuilder.append(this.mQueueName);
                    stringBuilder.append("]: ");
                    stringBuilder.append(intent);
                    Slog.v("BroadcastQueue", stringBuilder.toString());
                }
                queue.set(i, r);
                return old;
            }
        }
        return null;
    }

    private final void processCurBroadcastLocked(BroadcastRecord r, ProcessRecord app, boolean skipOomAdj) throws RemoteException {
        BroadcastRecord broadcastRecord = r;
        ProcessRecord processRecord = app;
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(processRecord.uid, processRecord.pid, BehaviorId.BROADCASTQUEUE_PROCESSCURBROADCASTLOCKED, new Object[]{broadcastRecord.intent});
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Process cur broadcast ");
            stringBuilder.append(broadcastRecord);
            stringBuilder.append(" for app ");
            stringBuilder.append(processRecord);
            Slog.v("BroadcastQueue", stringBuilder.toString());
        }
        if (processRecord.thread == null) {
            throw new RemoteException();
        } else if (processRecord.inFullBackup) {
            skipReceiverLocked(r);
        } else {
            broadcastRecord.receiver = processRecord.thread.asBinder();
            broadcastRecord.curApp = processRecord;
            processRecord.curReceivers.add(broadcastRecord);
            processRecord.forceProcessStateUpTo(10);
            this.mService.updateLruProcessLocked(processRecord, false, null);
            if (!skipOomAdj) {
                this.mService.updateOomAdjLocked();
            }
            broadcastRecord.intent.setComponent(broadcastRecord.curComponent);
            StringBuilder stringBuilder2;
            try {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Delivering to component ");
                    stringBuilder2.append(broadcastRecord.curComponent);
                    stringBuilder2.append(": ");
                    stringBuilder2.append(broadcastRecord);
                    Slog.v("BroadcastQueue", stringBuilder2.toString());
                }
                this.mService.notifyPackageUse(broadcastRecord.intent.getComponent().getPackageName(), 3);
                processRecord.thread.scheduleReceiver(new Intent(broadcastRecord.intent), broadcastRecord.curReceiver, this.mService.compatibilityInfoForPackageLocked(broadcastRecord.curReceiver.applicationInfo), broadcastRecord.resultCode, broadcastRecord.resultData, broadcastRecord.resultExtras, broadcastRecord.ordered, broadcastRecord.userId, processRecord.repProcState);
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Process cur broadcast ");
                    stringBuilder2.append(broadcastRecord);
                    stringBuilder2.append(" DELIVERED for app ");
                    stringBuilder2.append(processRecord);
                    Slog.v("BroadcastQueue", stringBuilder2.toString());
                }
                if (!true) {
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Process cur broadcast ");
                        stringBuilder2.append(broadcastRecord);
                        stringBuilder2.append(": NOT STARTED!");
                        Slog.v("BroadcastQueue", stringBuilder2.toString());
                    }
                    broadcastRecord.receiver = null;
                    broadcastRecord.curApp = null;
                    processRecord.curReceivers.remove(broadcastRecord);
                }
            } catch (Throwable th) {
                if (!false) {
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Process cur broadcast ");
                        stringBuilder2.append(broadcastRecord);
                        stringBuilder2.append(": NOT STARTED!");
                        Slog.v("BroadcastQueue", stringBuilder2.toString());
                    }
                    broadcastRecord.receiver = null;
                    broadcastRecord.curApp = null;
                    processRecord.curReceivers.remove(broadcastRecord);
                }
            }
        }
    }

    public boolean sendPendingBroadcastsLocked(ProcessRecord app) {
        boolean didSomething = false;
        BroadcastRecord br = this.mPendingBroadcast;
        if (br != null && br.curApp.pid > 0 && br.curApp.pid == app.pid) {
            if (br.curApp != app) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("App mismatch when sending pending broadcast to ");
                stringBuilder.append(app.processName);
                stringBuilder.append(", intended target is ");
                stringBuilder.append(br.curApp.processName);
                Slog.e("BroadcastQueue", stringBuilder.toString());
                return false;
            }
            try {
                this.mPendingBroadcast = null;
                processCurBroadcastLocked(br, app, false);
                didSomething = true;
            } catch (Exception e) {
                Exception e2 = e;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Exception in new application when starting receiver ");
                stringBuilder2.append(br.curComponent.flattenToShortString());
                Slog.w("BroadcastQueue", stringBuilder2.toString(), e2);
                logBroadcastReceiverDiscardLocked(br);
                finishReceiverLocked(br, br.resultCode, br.resultData, br.resultExtras, br.resultAbort, false);
                scheduleBroadcastsLocked();
                br.state = 0;
                throw new RuntimeException(e2.getMessage());
            }
        }
        return didSomething;
    }

    public void skipPendingBroadcastLocked(int pid) {
        BroadcastRecord br = this.mPendingBroadcast;
        if (br != null && br.curApp.pid == pid) {
            br.state = 0;
            br.nextReceiver = this.mPendingBroadcastRecvIndex;
            this.mPendingBroadcast = null;
            scheduleBroadcastsLocked();
        }
    }

    public void skipCurrentReceiverLocked(ProcessRecord app) {
        BroadcastRecord r = null;
        if (this.mOrderedBroadcasts.size() > 0) {
            BroadcastRecord br = (BroadcastRecord) this.mOrderedBroadcasts.get(0);
            if (br.curApp == app) {
                r = br;
            }
        }
        if (r == null && this.mPendingBroadcast != null && this.mPendingBroadcast.curApp == app) {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[");
                stringBuilder.append(this.mQueueName);
                stringBuilder.append("] skip & discard pending app ");
                stringBuilder.append(r);
                Slog.v("BroadcastQueue", stringBuilder.toString());
            }
            r = this.mPendingBroadcast;
        }
        if (r != null) {
            skipReceiverLocked(r);
        }
    }

    private void skipReceiverLocked(BroadcastRecord r) {
        logBroadcastReceiverDiscardLocked(r);
        finishReceiverLocked(r, r.resultCode, r.resultData, r.resultExtras, r.resultAbort, false);
        scheduleBroadcastsLocked();
    }

    public void scheduleBroadcastsLocked() {
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Schedule broadcasts [");
            stringBuilder.append(this.mQueueName);
            stringBuilder.append("]: current=");
            stringBuilder.append(this.mBroadcastsScheduled);
            Slog.v("BroadcastQueue", stringBuilder.toString());
        }
        if (!this.mBroadcastsScheduled) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(200, this));
            this.mBroadcastsScheduled = true;
        }
    }

    public BroadcastRecord getMatchingOrderedReceiver(IBinder receiver) {
        if (this.mOrderedBroadcasts.size() > 0) {
            BroadcastRecord r = (BroadcastRecord) this.mOrderedBroadcasts.get(0);
            if (r != null && r.receiver == receiver) {
                return r;
            }
        }
        return null;
    }

    public boolean finishReceiverLocked(BroadcastRecord r, int resultCode, String resultData, Bundle resultExtras, boolean resultAbort, boolean waitForServices) {
        boolean z = true;
        if (r.curApp != null) {
            HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(r.curApp.uid, r.curApp.pid, BehaviorId.BROADCASTQUEUE_FINISHRECEIVERLOCKED, new Object[]{r.intent});
        }
        int state = r.state;
        ActivityInfo receiver = r.curReceiver;
        r.state = 0;
        if (state == 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("finishReceiver [");
            stringBuilder.append(this.mQueueName);
            stringBuilder.append("] called but state is IDLE");
            Slog.w("BroadcastQueue", stringBuilder.toString());
        }
        r.receiver = null;
        r.intent.setComponent(null);
        if (r.curApp != null && r.curApp.curReceivers.contains(r)) {
            r.curApp.curReceivers.remove(r);
        }
        if (r.curFilter != null) {
            r.curFilter.receiverList.curBroadcast = null;
        }
        r.curFilter = null;
        r.curReceiver = null;
        r.curApp = null;
        this.mPendingBroadcast = null;
        r.resultCode = resultCode;
        r.resultData = resultData;
        r.resultExtras = resultExtras;
        if (resultAbort && (r.intent.getFlags() & 134217728) == 0) {
            r.resultAbort = resultAbort;
        } else {
            r.resultAbort = false;
        }
        if (waitForServices && r.curComponent != null && r.queue.mDelayBehindServices && r.queue.mOrderedBroadcasts.size() > 0 && r.queue.mOrderedBroadcasts.get(0) == r) {
            ActivityInfo nextReceiver;
            if (r.nextReceiver < r.receivers.size()) {
                Object obj = r.receivers.get(r.nextReceiver);
                nextReceiver = obj instanceof ActivityInfo ? (ActivityInfo) obj : null;
            } else {
                nextReceiver = null;
            }
            if ((receiver == null || nextReceiver == null || receiver.applicationInfo.uid != nextReceiver.applicationInfo.uid || !receiver.processName.equals(nextReceiver.processName)) && this.mService.mServices.hasBackgroundServicesLocked(r.userId)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Delay finish: ");
                stringBuilder2.append(r.curComponent.flattenToShortString());
                Slog.i("BroadcastQueue", stringBuilder2.toString());
                r.state = 4;
                return false;
            }
        }
        r.curComponent = null;
        if (!(state == 1 || state == 3)) {
            z = false;
        }
        return z;
    }

    public void backgroundServicesFinishedLocked(int userId) {
        if (this.mOrderedBroadcasts.size() > 0) {
            BroadcastRecord br = (BroadcastRecord) this.mOrderedBroadcasts.get(0);
            if (br.userId == userId && br.state == 4) {
                Slog.i("BroadcastQueue", "Resuming delayed broadcast");
                br.curComponent = null;
                br.state = 0;
                processNextBroadcast(false);
            }
        }
    }

    void performReceiveLocked(ProcessRecord app, IIntentReceiver receiver, Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
        ProcessRecord processRecord = app;
        if (processRecord != null) {
            HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(processRecord.uid, processRecord.pid, BehaviorId.BROADCASTQUEUE_PERFORMRECEIVELOCKED, new Object[]{intent});
        }
        if (processRecord == null) {
            receiver.performReceive(intent, resultCode, data, extras, ordered, sticky, sendingUser);
        } else if (processRecord.thread != null) {
            try {
                processRecord.thread.scheduleRegisteredReceiver(receiver, intent, resultCode, data, extras, ordered, sticky, sendingUser, processRecord.repProcState);
            } catch (RemoteException e) {
                RemoteException ex = e;
                synchronized (this.mService) {
                    ActivityManagerService.boostPriorityForLockedSection();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Can't deliver broadcast to ");
                    stringBuilder.append(processRecord.processName);
                    stringBuilder.append(" (pid ");
                    stringBuilder.append(processRecord.pid);
                    stringBuilder.append("). Crashing it.");
                    Slog.w("BroadcastQueue", stringBuilder.toString());
                    processRecord.scheduleCrash("can't deliver broadcast");
                    throw ex;
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        } else {
            throw new RemoteException("app.thread must not be null");
        }
    }

    private void deliverToRegisteredReceiverLocked(BroadcastRecord r, BroadcastFilter filter, boolean ordered, int index) {
        StringBuilder stringBuilder;
        int opCode;
        StringBuilder stringBuilder2;
        StringBuilder stringBuilder3;
        BroadcastRecord broadcastRecord = r;
        BroadcastFilter broadcastFilter = filter;
        boolean skip = false;
        if (broadcastFilter.requiredPermission != null) {
            if (this.mService.checkComponentPermission(broadcastFilter.requiredPermission, broadcastRecord.callingPid, broadcastRecord.callingUid, -1, true) != 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Permission Denial: broadcasting ");
                stringBuilder.append(broadcastRecord.intent.toString());
                stringBuilder.append(" from ");
                stringBuilder.append(broadcastRecord.callerPackage);
                stringBuilder.append(" (pid=");
                stringBuilder.append(broadcastRecord.callingPid);
                stringBuilder.append(", uid=");
                stringBuilder.append(broadcastRecord.callingUid);
                stringBuilder.append(") requires ");
                stringBuilder.append(broadcastFilter.requiredPermission);
                stringBuilder.append(" due to registered receiver ");
                stringBuilder.append(broadcastFilter);
                Slog.w("BroadcastQueue", stringBuilder.toString());
                skip = true;
            } else {
                opCode = AppOpsManager.permissionToOpCode(broadcastFilter.requiredPermission);
                if (!(opCode == -1 || this.mService.mAppOpsService.noteOperation(opCode, broadcastRecord.callingUid, broadcastRecord.callerPackage) == 0)) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Appop Denial: broadcasting ");
                    stringBuilder2.append(broadcastRecord.intent.toString());
                    stringBuilder2.append(" from ");
                    stringBuilder2.append(broadcastRecord.callerPackage);
                    stringBuilder2.append(" (pid=");
                    stringBuilder2.append(broadcastRecord.callingPid);
                    stringBuilder2.append(", uid=");
                    stringBuilder2.append(broadcastRecord.callingUid);
                    stringBuilder2.append(") requires appop ");
                    stringBuilder2.append(AppOpsManager.permissionToOp(broadcastFilter.requiredPermission));
                    stringBuilder2.append(" due to registered receiver ");
                    stringBuilder2.append(broadcastFilter);
                    Slog.w("BroadcastQueue", stringBuilder2.toString());
                    skip = true;
                }
            }
        }
        if (!(skip || broadcastRecord.requiredPermissions == null || broadcastRecord.requiredPermissions.length <= 0)) {
            opCode = 0;
            while (opCode < broadcastRecord.requiredPermissions.length) {
                String requiredPermission = broadcastRecord.requiredPermissions[opCode];
                if (this.mService.checkComponentPermission(requiredPermission, broadcastFilter.receiverList.pid, broadcastFilter.receiverList.uid, -1, true) == 0) {
                    int appOp = AppOpsManager.permissionToOpCode(requiredPermission);
                    if (appOp != -1 && appOp != broadcastRecord.appOp && this.mService.mAppOpsService.noteOperation(appOp, broadcastFilter.receiverList.uid, broadcastFilter.packageName) != 0) {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Appop Denial: receiving ");
                        stringBuilder4.append(broadcastRecord.intent.toString());
                        stringBuilder4.append(" to ");
                        stringBuilder4.append(broadcastFilter.receiverList.app);
                        stringBuilder4.append(" (pid=");
                        stringBuilder4.append(broadcastFilter.receiverList.pid);
                        stringBuilder4.append(", uid=");
                        stringBuilder4.append(broadcastFilter.receiverList.uid);
                        stringBuilder4.append(") requires appop ");
                        stringBuilder4.append(AppOpsManager.permissionToOp(requiredPermission));
                        stringBuilder4.append(" due to sender ");
                        stringBuilder4.append(broadcastRecord.callerPackage);
                        stringBuilder4.append(" (uid ");
                        stringBuilder4.append(broadcastRecord.callingUid);
                        stringBuilder4.append(")");
                        Slog.w("BroadcastQueue", stringBuilder4.toString());
                        skip = true;
                        break;
                    }
                    opCode++;
                } else {
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("Permission Denial: receiving ");
                    stringBuilder5.append(broadcastRecord.intent.toString());
                    stringBuilder5.append(" to ");
                    stringBuilder5.append(broadcastFilter.receiverList.app);
                    stringBuilder5.append(" (pid=");
                    stringBuilder5.append(broadcastFilter.receiverList.pid);
                    stringBuilder5.append(", uid=");
                    stringBuilder5.append(broadcastFilter.receiverList.uid);
                    stringBuilder5.append(") requires ");
                    stringBuilder5.append(requiredPermission);
                    stringBuilder5.append(" due to sender ");
                    stringBuilder5.append(broadcastRecord.callerPackage);
                    stringBuilder5.append(" (uid ");
                    stringBuilder5.append(broadcastRecord.callingUid);
                    stringBuilder5.append(")");
                    Slog.w("BroadcastQueue", stringBuilder5.toString());
                    skip = true;
                    break;
                }
            }
            if (broadcastRecord.intent != null && "android.provider.Telephony.SMS_RECEIVED".equals(broadcastRecord.intent.getAction())) {
                HwSystemManager.insertSendBroadcastRecord(broadcastFilter.packageName, broadcastRecord.intent.getAction(), broadcastFilter.receiverList.uid);
            }
        }
        if (!skip && ((broadcastRecord.requiredPermissions == null || broadcastRecord.requiredPermissions.length == 0) && this.mService.checkComponentPermission(null, broadcastFilter.receiverList.pid, broadcastFilter.receiverList.uid, -1, true) != 0)) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Permission Denial: security check failed when receiving ");
            stringBuilder2.append(broadcastRecord.intent.toString());
            stringBuilder2.append(" to ");
            stringBuilder2.append(broadcastFilter.receiverList.app);
            stringBuilder2.append(" (pid=");
            stringBuilder2.append(broadcastFilter.receiverList.pid);
            stringBuilder2.append(", uid=");
            stringBuilder2.append(broadcastFilter.receiverList.uid);
            stringBuilder2.append(") due to sender ");
            stringBuilder2.append(broadcastRecord.callerPackage);
            stringBuilder2.append(" (uid ");
            stringBuilder2.append(broadcastRecord.callingUid);
            stringBuilder2.append(")");
            Slog.w("BroadcastQueue", stringBuilder2.toString());
            skip = true;
        }
        if (!(skip || broadcastRecord.appOp == -1 || this.mService.mAppOpsService.noteOperation(broadcastRecord.appOp, broadcastFilter.receiverList.uid, broadcastFilter.packageName) == 0)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Appop Denial: receiving ");
            stringBuilder3.append(broadcastRecord.intent.toString());
            stringBuilder3.append(" to ");
            stringBuilder3.append(broadcastFilter.receiverList.app);
            stringBuilder3.append(" (pid=");
            stringBuilder3.append(broadcastFilter.receiverList.pid);
            stringBuilder3.append(", uid=");
            stringBuilder3.append(broadcastFilter.receiverList.uid);
            stringBuilder3.append(") requires appop ");
            stringBuilder3.append(AppOpsManager.opToName(broadcastRecord.appOp));
            stringBuilder3.append(" due to sender ");
            stringBuilder3.append(broadcastRecord.callerPackage);
            stringBuilder3.append(" (uid ");
            stringBuilder3.append(broadcastRecord.callingUid);
            stringBuilder3.append(")");
            Slog.w("BroadcastQueue", stringBuilder3.toString());
            skip = true;
        }
        if (!this.mService.mIntentFirewall.checkBroadcast(broadcastRecord.intent, broadcastRecord.callingUid, broadcastRecord.callingPid, broadcastRecord.resolvedType, broadcastFilter.receiverList.uid)) {
            skip = true;
        }
        if (!skip && this.mService.shouldPreventSendBroadcast(broadcastRecord.intent, broadcastFilter.packageName, broadcastRecord.callingUid, broadcastRecord.callingPid, broadcastRecord.callerPackage, broadcastRecord.userId)) {
            skip = true;
        }
        if (!skip && (broadcastFilter.receiverList.app == null || broadcastFilter.receiverList.app.killed || broadcastFilter.receiverList.app.crashing)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Skipping deliver [");
            stringBuilder3.append(this.mQueueName);
            stringBuilder3.append("] ");
            stringBuilder3.append(broadcastRecord);
            stringBuilder3.append(" to ");
            stringBuilder3.append(broadcastFilter.receiverList);
            stringBuilder3.append(": process gone or crashing");
            Slog.w("BroadcastQueue", stringBuilder3.toString());
            skip = true;
        }
        boolean visibleToInstantApps = (broadcastRecord.intent.getFlags() & DumpState.DUMP_COMPILER_STATS) != 0;
        if (!(skip || visibleToInstantApps || !broadcastFilter.instantApp || broadcastFilter.receiverList.uid == broadcastRecord.callingUid)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Instant App Denial: receiving ");
            stringBuilder.append(broadcastRecord.intent.toString());
            stringBuilder.append(" to ");
            stringBuilder.append(broadcastFilter.receiverList.app);
            stringBuilder.append(" (pid=");
            stringBuilder.append(broadcastFilter.receiverList.pid);
            stringBuilder.append(", uid=");
            stringBuilder.append(broadcastFilter.receiverList.uid);
            stringBuilder.append(") due to sender ");
            stringBuilder.append(broadcastRecord.callerPackage);
            stringBuilder.append(" (uid ");
            stringBuilder.append(broadcastRecord.callingUid);
            stringBuilder.append(") not specifying FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS");
            Slog.w("BroadcastQueue", stringBuilder.toString());
            skip = true;
        }
        if (!(skip || broadcastFilter.visibleToInstantApp || !broadcastRecord.callerInstantApp || broadcastFilter.receiverList.uid == broadcastRecord.callingUid)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Instant App Denial: receiving ");
            stringBuilder.append(broadcastRecord.intent.toString());
            stringBuilder.append(" to ");
            stringBuilder.append(broadcastFilter.receiverList.app);
            stringBuilder.append(" (pid=");
            stringBuilder.append(broadcastFilter.receiverList.pid);
            stringBuilder.append(", uid=");
            stringBuilder.append(broadcastFilter.receiverList.uid);
            stringBuilder.append(") requires receiver be visible to instant apps due to sender ");
            stringBuilder.append(broadcastRecord.callerPackage);
            stringBuilder.append(" (uid ");
            stringBuilder.append(broadcastRecord.callingUid);
            stringBuilder.append(")");
            Slog.w("BroadcastQueue", stringBuilder.toString());
            skip = true;
        }
        if (skip) {
            broadcastRecord.delivery[index] = 2;
        } else if (this.mService.mPermissionReviewRequired && !requestStartTargetPermissionsReviewIfNeededLocked(broadcastRecord, broadcastFilter.packageName, broadcastFilter.owningUserId)) {
            broadcastRecord.delivery[index] = 2;
        } else if (!getMtmBRManagerEnabled(10) || !getMtmBRManager().iawareProcessBroadcast(0, ordered ^ 1, broadcastRecord, broadcastFilter)) {
            broadcastRecord.delivery[index] = 1;
            if (ordered) {
                broadcastRecord.receiver = broadcastFilter.receiverList.receiver.asBinder();
                broadcastRecord.curFilter = broadcastFilter;
                broadcastFilter.receiverList.curBroadcast = broadcastRecord;
                broadcastRecord.state = 2;
                if (broadcastFilter.receiverList.app != null) {
                    broadcastRecord.curApp = broadcastFilter.receiverList.app;
                    broadcastFilter.receiverList.app.curReceivers.add(broadcastRecord);
                    this.mService.updateOomAdjLocked(broadcastRecord.curApp, true);
                }
            }
            try {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                    StringBuilder stringBuilder6 = new StringBuilder();
                    stringBuilder6.append("Delivering to ");
                    stringBuilder6.append(broadcastFilter);
                    stringBuilder6.append(" : ");
                    stringBuilder6.append(broadcastRecord);
                    Slog.i("BroadcastQueue", stringBuilder6.toString());
                }
                if (broadcastFilter.receiverList.app == null || !broadcastFilter.receiverList.app.inFullBackup) {
                    performReceiveLocked(broadcastFilter.receiverList.app, broadcastFilter.receiverList.receiver, new Intent(broadcastRecord.intent), broadcastRecord.resultCode, broadcastRecord.resultData, broadcastRecord.resultExtras, broadcastRecord.ordered, broadcastRecord.initialSticky, broadcastRecord.userId);
                } else if (ordered) {
                    skipReceiverLocked(r);
                }
                if (ordered) {
                    broadcastRecord.state = 3;
                }
            } catch (RemoteException e) {
                StringBuilder stringBuilder7 = new StringBuilder();
                stringBuilder7.append("Failure sending broadcast ");
                stringBuilder7.append(broadcastRecord.intent);
                Slog.w("BroadcastQueue", stringBuilder7.toString(), e);
                if (ordered) {
                    broadcastRecord.receiver = null;
                    broadcastRecord.curFilter = null;
                    broadcastFilter.receiverList.curBroadcast = null;
                    if (broadcastFilter.receiverList.app != null) {
                        broadcastFilter.receiverList.app.curReceivers.remove(broadcastRecord);
                    }
                }
            }
        }
    }

    private boolean requestStartTargetPermissionsReviewIfNeededLocked(BroadcastRecord receiverRecord, String receivingPackageName, int receivingUserId) {
        BroadcastRecord broadcastRecord = receiverRecord;
        String str = receivingPackageName;
        final int i = receivingUserId;
        if (!this.mService.getPackageManagerInternalLocked().isPermissionsReviewRequired(str, i)) {
            return true;
        }
        boolean callerForeground = broadcastRecord.callerApp == null || broadcastRecord.callerApp.setSchedGroup != 0;
        if (!callerForeground || broadcastRecord.intent.getComponent() == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("u");
            stringBuilder.append(i);
            stringBuilder.append(" Receiving a broadcast in package");
            stringBuilder.append(str);
            stringBuilder.append(" requires a permissions review");
            Slog.w("BroadcastQueue", stringBuilder.toString());
        } else {
            IIntentSender target = this.mService.getIntentSenderLocked(1, broadcastRecord.callerPackage, broadcastRecord.callingUid, broadcastRecord.userId, null, null, 0, new Intent[]{broadcastRecord.intent}, new String[]{broadcastRecord.intent.resolveType(this.mService.mContext.getContentResolver())}, 1409286144, null);
            final Intent intent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
            intent.addFlags(276824064);
            intent.putExtra("android.intent.extra.PACKAGE_NAME", str);
            intent.putExtra("android.intent.extra.INTENT", new IntentSender(target));
            if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("u");
                stringBuilder2.append(i);
                stringBuilder2.append(" Launching permission review for package ");
                stringBuilder2.append(str);
                Slog.i("BroadcastQueue", stringBuilder2.toString());
            }
            this.mHandler.post(new Runnable() {
                public void run() {
                    BroadcastQueue.this.mService.mContext.startActivityAsUser(intent, new UserHandle(i));
                }
            });
        }
        return false;
    }

    final void scheduleTempWhitelistLocked(int uid, long duration, BroadcastRecord r) {
        if (duration > 2147483647L) {
            duration = 2147483647L;
        }
        StringBuilder b = new StringBuilder();
        b.append("broadcast:");
        UserHandle.formatUid(b, r.callingUid);
        b.append(":");
        if (r.intent.getAction() != null) {
            b.append(r.intent.getAction());
        } else if (r.intent.getComponent() != null) {
            r.intent.getComponent().appendShortString(b);
        } else if (r.intent.getData() != null) {
            b.append(r.intent.getData());
        }
        this.mService.tempWhitelistUidLocked(uid, duration, b.toString());
    }

    final boolean isSignaturePerm(String[] perms) {
        if (perms == null) {
            return false;
        }
        IPackageManager pm = AppGlobals.getPackageManager();
        int i = perms.length - 1;
        while (i >= 0) {
            try {
                PermissionInfo pi = pm.getPermissionInfo(perms[i], PackageManagerService.PLATFORM_PACKAGE_NAME, 0);
                if (pi == null || (pi.protectionLevel & 31) != 2) {
                    return false;
                }
                i--;
            } catch (RemoteException e) {
                return false;
            }
        }
        return true;
    }

    final void processNextBroadcast(boolean fromMsg) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                processNextBroadcastLocked(fromMsg, false);
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:394:0x0f20 A:{SYNTHETIC, Splitter:B:394:0x0f20} */
    /* JADX WARNING: Removed duplicated region for block: B:407:0x0fc2  */
    /* JADX WARNING: Removed duplicated region for block: B:406:0x0f97  */
    /* JADX WARNING: Removed duplicated region for block: B:413:0x0fd9  */
    /* JADX WARNING: Removed duplicated region for block: B:410:0x0fcc  */
    /* JADX WARNING: Removed duplicated region for block: B:417:0x0ffc  */
    /* JADX WARNING: Removed duplicated region for block: B:416:0x0ff9  */
    /* JADX WARNING: Removed duplicated region for block: B:422:0x1060  */
    /* JADX WARNING: Removed duplicated region for block: B:420:0x1012  */
    /* JADX WARNING: Removed duplicated region for block: B:382:0x0e85 A:{ExcHandler: RemoteException (e android.os.RemoteException), Splitter:B:375:0x0e6d} */
    /* JADX WARNING: Removed duplicated region for block: B:140:0x049e  */
    /* JADX WARNING: Removed duplicated region for block: B:424:0x1067 A:{LOOP_END, LOOP:2: B:73:0x023a->B:424:0x1067} */
    /* JADX WARNING: Removed duplicated region for block: B:435:0x04fe A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:394:0x0f20 A:{SYNTHETIC, Splitter:B:394:0x0f20} */
    /* JADX WARNING: Removed duplicated region for block: B:406:0x0f97  */
    /* JADX WARNING: Removed duplicated region for block: B:407:0x0fc2  */
    /* JADX WARNING: Removed duplicated region for block: B:410:0x0fcc  */
    /* JADX WARNING: Removed duplicated region for block: B:413:0x0fd9  */
    /* JADX WARNING: Removed duplicated region for block: B:416:0x0ff9  */
    /* JADX WARNING: Removed duplicated region for block: B:417:0x0ffc  */
    /* JADX WARNING: Removed duplicated region for block: B:420:0x1012  */
    /* JADX WARNING: Removed duplicated region for block: B:422:0x1060  */
    /* JADX WARNING: Removed duplicated region for block: B:384:0x0e92 A:{ExcHandler: RuntimeException (e java.lang.RuntimeException), Splitter:B:373:0x0e6b} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:382:0x0e85, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:383:0x0e86, code skipped:
            r29 = r3;
            r30 = r5;
            r12 = r6;
            r13 = r7;
            r18 = r40;
            r44 = r41;
     */
    /* JADX WARNING: Missing block: B:384:0x0e92, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:385:0x0e93, code skipped:
            r12 = r48;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    final void processNextBroadcastLocked(boolean fromMsg, boolean skipOomAdj) {
        StringBuilder stringBuilder;
        int N;
        StringBuilder stringBuilder2;
        int i;
        StringBuilder stringBuilder3;
        ProcessRecord proc;
        boolean z;
        boolean isDead;
        StringBuilder stringBuilder4;
        RemoteException e;
        int i2;
        boolean isSingleton;
        ResolveInfo info;
        String targetProcess;
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("processNextBroadcast [");
            stringBuilder.append(this.mQueueName);
            stringBuilder.append("]: ");
            stringBuilder.append(this.mParallelBroadcasts.size());
            stringBuilder.append(" parallel broadcasts, ");
            stringBuilder.append(this.mOrderedBroadcasts.size());
            stringBuilder.append(" ordered broadcasts");
            Slog.v("BroadcastQueue", stringBuilder.toString());
        }
        this.mService.updateCpuStats();
        if (fromMsg) {
            if (!this.mBroadcastsScheduled) {
                Slog.e("BroadcastQueue", "processNextBroadcast before mBroadcastsScheduled is set true", new RuntimeException("here").fillInStackTrace());
            }
            this.mBroadcastsScheduled = false;
        }
        while (this.mParallelBroadcasts.size() > 0) {
            BroadcastRecord r = (BroadcastRecord) this.mParallelBroadcasts.remove(0);
            r.dispatchTime = SystemClock.uptimeMillis();
            r.dispatchClockTime = System.currentTimeMillis();
            if (Trace.isTagEnabled(64)) {
                Trace.asyncTraceEnd(64, createBroadcastTraceTitle(r, 0), System.identityHashCode(r));
                Trace.asyncTraceBegin(64, createBroadcastTraceTitle(r, 1), System.identityHashCode(r));
            }
            N = r.receivers.size();
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Processing parallel broadcast [");
                stringBuilder2.append(this.mQueueName);
                stringBuilder2.append("] ");
                stringBuilder2.append(r);
                Slog.v("BroadcastQueue", stringBuilder2.toString());
            }
            if (getMtmBRManagerEnabled(10)) {
                getMtmBRManager().iawareStartCountBroadcastSpeed(true, r);
            }
            for (i = 0; i < N; i++) {
                Object target = r.receivers.get(i);
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Delivering non-ordered on [");
                    stringBuilder3.append(this.mQueueName);
                    stringBuilder3.append("] to registered ");
                    stringBuilder3.append(target);
                    stringBuilder3.append(": ");
                    stringBuilder3.append(r);
                    Slog.v("BroadcastQueue", stringBuilder3.toString());
                }
                if (!enqueueProxyBroadcast(true, r, target)) {
                    deliverToRegisteredReceiverLocked(r, (BroadcastFilter) target, false, i);
                } else if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("parallel ");
                    stringBuilder3.append(this.mQueueName);
                    stringBuilder3.append(" broadcast:(");
                    stringBuilder3.append(r);
                    stringBuilder3.append(") should be proxyed, target:(");
                    stringBuilder3.append(target);
                    stringBuilder3.append(")");
                    Slog.v("BroadcastQueue", stringBuilder3.toString());
                }
            }
            if (getMtmBRManagerEnabled(10)) {
                getMtmBRManager().iawareEndCountBroadcastSpeed(r);
            }
            addBroadcastToHistoryLocked(r);
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Done with parallel broadcast [");
                stringBuilder2.append(this.mQueueName);
                stringBuilder2.append("] ");
                stringBuilder2.append(r);
                Slog.v("BroadcastQueue", stringBuilder2.toString());
            }
        }
        if (this.mPendingBroadcast != null) {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("processNextBroadcast [");
                stringBuilder.append(this.mQueueName);
                stringBuilder.append("]: waiting for ");
                stringBuilder.append(this.mPendingBroadcast.curApp);
                Slog.v("BroadcastQueue", stringBuilder.toString());
            }
            if (this.mPendingBroadcast.curApp.pid > 0) {
                synchronized (this.mService.mPidsSelfLocked) {
                    proc = (ProcessRecord) this.mService.mPidsSelfLocked.get(this.mPendingBroadcast.curApp.pid);
                    if (proc != null) {
                        if (!proc.crashing) {
                            z = false;
                            isDead = z;
                        }
                    }
                    z = true;
                    isDead = z;
                }
            } else {
                proc = (ProcessRecord) this.mService.mProcessNames.get(this.mPendingBroadcast.curApp.processName, this.mPendingBroadcast.curApp.uid);
                boolean z2 = proc == null || !proc.pendingStart;
                isDead = z2;
            }
            if (isDead) {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("pending app  [");
                stringBuilder4.append(this.mQueueName);
                stringBuilder4.append("]");
                stringBuilder4.append(this.mPendingBroadcast.curApp);
                stringBuilder4.append(" died before responding to broadcast");
                Slog.w("BroadcastQueue", stringBuilder4.toString());
                this.mPendingBroadcast.state = 0;
                this.mPendingBroadcast.nextReceiver = this.mPendingBroadcastRecvIndex;
                this.mPendingBroadcast = null;
            } else {
                return;
            }
        }
        isDead = false;
        while (true) {
            boolean looped = isDead;
            if (this.mOrderedBroadcasts.size() == 0) {
                this.mService.scheduleAppGcsLocked();
                if (looped) {
                    this.mService.updateOomAdjLocked();
                }
                return;
            }
            BroadcastRecord r2 = (BroadcastRecord) this.mOrderedBroadcasts.get(0);
            isDead = false;
            int numReceivers = r2.receivers != null ? r2.receivers.size() : 0;
            if (this.mService.mProcessesReady && r2.dispatchTime > 0) {
                long now = SystemClock.uptimeMillis();
                if (r2.anrCount > 0) {
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("intentAction=");
                    stringBuilder4.append(r2.intent.getAction());
                    stringBuilder4.append("dispatchTime=");
                    stringBuilder4.append(r2.dispatchTime);
                    stringBuilder4.append(" numReceivers=");
                    stringBuilder4.append(numReceivers);
                    stringBuilder4.append(" nextReceiver=");
                    stringBuilder4.append(r2.nextReceiver);
                    stringBuilder4.append(" now=");
                    stringBuilder4.append(now);
                    stringBuilder4.append(" state=");
                    stringBuilder4.append(r2.state);
                    stringBuilder4.append(" curReceiver  = ");
                    stringBuilder4.append(r2.curReceiver);
                    Slog.w("BroadcastQueue", stringBuilder4.toString());
                }
                if (numReceivers > 0 && now > r2.dispatchTime + ((2 * this.mTimeoutPeriod) * ((long) numReceivers))) {
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Hung broadcast [");
                    stringBuilder4.append(this.mQueueName);
                    stringBuilder4.append("] discarded after timeout failure: now=");
                    stringBuilder4.append(now);
                    stringBuilder4.append(" dispatchTime=");
                    stringBuilder4.append(r2.dispatchTime);
                    stringBuilder4.append(" startTime=");
                    stringBuilder4.append(r2.receiverTime);
                    stringBuilder4.append(" intent=");
                    stringBuilder4.append(r2.intent);
                    stringBuilder4.append(" numReceivers=");
                    stringBuilder4.append(numReceivers);
                    stringBuilder4.append(" nextReceiver=");
                    stringBuilder4.append(r2.nextReceiver);
                    stringBuilder4.append(" state=");
                    stringBuilder4.append(r2.state);
                    Slog.w("BroadcastQueue", stringBuilder4.toString());
                    broadcastTimeoutLocked(false);
                    isDead = true;
                    r2.state = 0;
                }
                if (r2.anrCount > 0 && numReceivers == 0 && now > r2.dispatchTime && r2.curReceiver != null && this == r2.queue) {
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append(" dispatchTime=");
                    stringBuilder4.append(r2.dispatchTime);
                    stringBuilder4.append(" numReceivers=");
                    stringBuilder4.append(numReceivers);
                    stringBuilder4.append(" nextReceiver=");
                    stringBuilder4.append(r2.nextReceiver);
                    stringBuilder4.append(" now=");
                    stringBuilder4.append(now);
                    stringBuilder4.append(" state=");
                    stringBuilder4.append(r2.state);
                    stringBuilder4.append(" curReceiver  = ");
                    stringBuilder4.append(r2.curReceiver);
                    stringBuilder4.append("finish curReceiver");
                    Slog.w("BroadcastQueue", stringBuilder4.toString());
                    logBroadcastReceiverDiscardLocked(r2);
                    finishReceiverLocked(r2, r2.resultCode, r2.resultData, r2.resultExtras, r2.resultAbort, 0);
                    scheduleBroadcastsLocked();
                    r2.state = 0;
                    return;
                }
            }
            int numReceivers2 = numReceivers;
            boolean forceReceive = isDead;
            if (r2.state != 0) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("processNextBroadcast(");
                    stringBuilder.append(this.mQueueName);
                    stringBuilder.append(") called when not idle (state=");
                    stringBuilder.append(r2.state);
                    stringBuilder.append(")");
                    Slog.d("BroadcastQueue", stringBuilder.toString());
                }
                return;
            }
            if (r2.receivers == null || r2.nextReceiver >= numReceivers2 || r2.resultAbort || forceReceive) {
                BroadcastRecord r3;
                if (r2.resultTo != null) {
                    try {
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            try {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Finishing broadcast [");
                                stringBuilder.append(this.mQueueName);
                                stringBuilder.append("] ");
                                stringBuilder.append(r2.intent.getAction());
                                stringBuilder.append(" app=");
                                stringBuilder.append(r2.callerApp);
                                Slog.i("BroadcastQueue", stringBuilder.toString());
                            } catch (RemoteException e2) {
                                e = e2;
                                r3 = r2;
                                i2 = numReceivers2;
                                isSingleton = null;
                            }
                        }
                        r3 = r2;
                        isSingleton = false;
                        try {
                            performReceiveLocked(r2.callerApp, r2.resultTo, new Intent(r2.intent), r2.resultCode, r2.resultData, r2.resultExtras, false, false, r2.userId);
                            r3.resultTo = null;
                        } catch (RemoteException e3) {
                            e = e3;
                        }
                    } catch (RemoteException e4) {
                        e = e4;
                        r3 = r2;
                        i2 = numReceivers2;
                        isSingleton = null;
                        r3.resultTo = isSingleton;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Failure [");
                        stringBuilder4.append(this.mQueueName);
                        stringBuilder4.append("] sending broadcast result of ");
                        stringBuilder4.append(r3.intent);
                        Slog.w("BroadcastQueue", stringBuilder4.toString(), e);
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        }
                        cancelBroadcastTimeoutLocked();
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Finished with ordered broadcast ");
                        stringBuilder.append(r3);
                        Slog.v("BroadcastQueue", stringBuilder.toString());
                        addBroadcastToHistoryLocked(r3);
                        this.mService.addBroadcastStatLocked(r3.intent.getAction(), r3.callerPackage, r3.manifestCount, r3.manifestSkipCount, r3.finishTime - r3.dispatchTime);
                        this.mOrderedBroadcasts.remove(0);
                        r2 = null;
                        looped = true;
                        if (r2 == null) {
                        }
                    }
                } else {
                    r3 = r2;
                    i2 = numReceivers2;
                    isSingleton = false;
                }
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v("BroadcastQueue", "Cancelling BROADCAST_TIMEOUT_MSG");
                }
                cancelBroadcastTimeoutLocked();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Finished with ordered broadcast ");
                stringBuilder.append(r3);
                Slog.v("BroadcastQueue", stringBuilder.toString());
                addBroadcastToHistoryLocked(r3);
                if (r3.intent.getComponent() == null && r3.intent.getPackage() == null && (r3.intent.getFlags() & 1073741824) == 0) {
                    this.mService.addBroadcastStatLocked(r3.intent.getAction(), r3.callerPackage, r3.manifestCount, r3.manifestSkipCount, r3.finishTime - r3.dispatchTime);
                }
                this.mOrderedBroadcasts.remove(0);
                r2 = null;
                looped = true;
            } else {
                isSingleton = false;
            }
            if (r2 == null) {
                int i3 = r2.nextReceiver;
                r2.nextReceiver = i3 + 1;
                int recIdx = i3;
                Object target2 = r2.receivers.get(recIdx);
                StringBuilder stringBuilder5;
                if (enqueueProxyBroadcast(false, r2, target2)) {
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("orderd ");
                    stringBuilder5.append(this.mQueueName);
                    stringBuilder5.append(" broadcast:(");
                    stringBuilder5.append(r2);
                    stringBuilder5.append(") should be proxyed, target:(");
                    stringBuilder5.append(target2);
                    stringBuilder5.append(")");
                    Flog.i(104, stringBuilder5.toString());
                    scheduleBroadcastsLocked();
                    return;
                }
                reportMediaButtonToAware(r2, target2);
                r2.receiverTime = SystemClock.uptimeMillis();
                if (recIdx == 0) {
                    r2.dispatchTime = r2.receiverTime;
                    r2.dispatchClockTime = System.currentTimeMillis();
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("dispatch ordered broadcast [");
                    stringBuilder5.append(this.mQueueName);
                    stringBuilder5.append("] ");
                    stringBuilder5.append(r2);
                    stringBuilder5.append(" enqueued ");
                    stringBuilder5.append(r2.dispatchClockTime - r2.enqueueClockTime);
                    stringBuilder5.append(" ms ago, has ");
                    stringBuilder5.append(r2.receivers.size());
                    stringBuilder5.append(" receivers");
                    Flog.i(104, stringBuilder5.toString());
                    updateSRMSStatisticsData(r2);
                    if (Trace.isTagEnabled(64)) {
                        Trace.asyncTraceEnd(64, createBroadcastTraceTitle(r2, 0), System.identityHashCode(r2));
                        Trace.asyncTraceBegin(64, createBroadcastTraceTitle(r2, 1), System.identityHashCode(r2));
                    }
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Processing ordered broadcast [");
                        stringBuilder.append(this.mQueueName);
                        stringBuilder.append("] ");
                        stringBuilder.append(r2);
                        Slog.v("BroadcastQueue", stringBuilder.toString());
                    }
                }
                if (!this.mPendingBroadcastTimeoutMessage) {
                    long timeoutTime = r2.receiverTime + this.mTimeoutPeriod;
                    long checkTime = r2.receiverTime + ((long) CHECK_INTERVAL);
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Submitting BROADCAST_TIMEOUT_MSG [");
                        stringBuilder3.append(this.mQueueName);
                        stringBuilder3.append("] for ");
                        stringBuilder3.append(r2);
                        stringBuilder3.append(" at ");
                        stringBuilder3.append(timeoutTime);
                        stringBuilder3.append(". Next frozen check time is ");
                        stringBuilder3.append(checkTime);
                        Slog.v("BroadcastQueue", stringBuilder3.toString());
                    }
                    setBroadcastTimeoutLocked(checkTime);
                }
                BroadcastOptions brOptions = r2.options;
                BroadcastFilter nextReceiver = r2.receivers.get(recIdx);
                if (nextReceiver instanceof BroadcastFilter) {
                    BroadcastFilter filter = nextReceiver;
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Delivering ordered [");
                        stringBuilder4.append(this.mQueueName);
                        stringBuilder4.append("] to registered ");
                        stringBuilder4.append(filter);
                        stringBuilder4.append(": ");
                        stringBuilder4.append(r2);
                        Slog.v("BroadcastQueue", stringBuilder4.toString());
                    }
                    deliverToRegisteredReceiverLocked(r2, filter, r2.ordered, recIdx);
                    if (r2.receiver == null || !r2.ordered) {
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Quick finishing [");
                            stringBuilder4.append(this.mQueueName);
                            stringBuilder4.append("]: ordered=");
                            stringBuilder4.append(r2.ordered);
                            stringBuilder4.append(" receiver=");
                            stringBuilder4.append(r2.receiver);
                            Slog.v("BroadcastQueue", stringBuilder4.toString());
                        }
                        r2.state = 0;
                        scheduleBroadcastsLocked();
                    } else if (brOptions != null && brOptions.getTemporaryAppWhitelistDuration() > 0) {
                        scheduleTempWhitelistLocked(filter.owningUid, brOptions.getTemporaryAppWhitelistDuration(), r2);
                    }
                    return;
                }
                int i4;
                boolean skip;
                Object nextReceiver2;
                String targetProcess2;
                ResolveInfo info2 = (ResolveInfo) nextReceiver;
                ComponentName component = new ComponentName(info2.activityInfo.applicationInfo.packageName, info2.activityInfo.name);
                isDead = false;
                if (brOptions != null && (info2.activityInfo.applicationInfo.targetSdkVersion < brOptions.getMinManifestReceiverApiLevel() || info2.activityInfo.applicationInfo.targetSdkVersion > brOptions.getMaxManifestReceiverApiLevel())) {
                    isDead = true;
                }
                N = this.mService.checkComponentPermission(info2.activityInfo.permission, r2.callingPid, r2.callingUid, info2.activityInfo.applicationInfo.uid, info2.activityInfo.exported);
                if (!isDead && N != 0) {
                    StringBuilder stringBuilder6;
                    if (info2.activityInfo.exported) {
                        stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("Permission Denial: broadcasting ");
                        stringBuilder6.append(r2.intent.toString());
                        stringBuilder6.append(" from ");
                        stringBuilder6.append(r2.callerPackage);
                        stringBuilder6.append(" (pid=");
                        stringBuilder6.append(r2.callingPid);
                        stringBuilder6.append(", uid=");
                        stringBuilder6.append(r2.callingUid);
                        stringBuilder6.append(") requires ");
                        stringBuilder6.append(info2.activityInfo.permission);
                        stringBuilder6.append(" due to receiver ");
                        stringBuilder6.append(component.flattenToShortString());
                        Slog.w("BroadcastQueue", stringBuilder6.toString());
                    } else {
                        stringBuilder6 = new StringBuilder();
                        stringBuilder6.append("Permission Denial: broadcasting ");
                        stringBuilder6.append(r2.intent.toString());
                        stringBuilder6.append(" from ");
                        stringBuilder6.append(r2.callerPackage);
                        stringBuilder6.append(" (pid=");
                        stringBuilder6.append(r2.callingPid);
                        stringBuilder6.append(", uid=");
                        stringBuilder6.append(r2.callingUid);
                        stringBuilder6.append(") is not exported from uid ");
                        stringBuilder6.append(info2.activityInfo.applicationInfo.uid);
                        stringBuilder6.append(" due to receiver ");
                        stringBuilder6.append(component.flattenToShortString());
                        Slog.w("BroadcastQueue", stringBuilder6.toString());
                    }
                    isDead = true;
                } else if (!(isDead || info2.activityInfo.permission == null)) {
                    i = AppOpsManager.permissionToOpCode(info2.activityInfo.permission);
                    if (!(i == -1 || this.mService.mAppOpsService.noteOperation(i, r2.callingUid, r2.callerPackage) == 0)) {
                        StringBuilder stringBuilder7 = new StringBuilder();
                        stringBuilder7.append("Appop Denial: broadcasting ");
                        stringBuilder7.append(r2.intent.toString());
                        stringBuilder7.append(" from ");
                        stringBuilder7.append(r2.callerPackage);
                        stringBuilder7.append(" (pid=");
                        stringBuilder7.append(r2.callingPid);
                        stringBuilder7.append(", uid=");
                        stringBuilder7.append(r2.callingUid);
                        stringBuilder7.append(") requires appop ");
                        stringBuilder7.append(AppOpsManager.permissionToOp(info2.activityInfo.permission));
                        stringBuilder7.append(" due to registered receiver ");
                        stringBuilder7.append(component.flattenToShortString());
                        Slog.w("BroadcastQueue", stringBuilder7.toString());
                        isDead = true;
                    }
                }
                boolean skip2 = isDead;
                if (!(skip2 || info2.activityInfo.applicationInfo.uid == 1000 || r2.requiredPermissions == null || r2.requiredPermissions.length <= 0)) {
                    i3 = 0;
                    while (true) {
                        i4 = i3;
                        if (i4 >= r2.requiredPermissions.length) {
                            break;
                        }
                        String requiredPermission = r2.requiredPermissions[i4];
                        try {
                            e = AppGlobals.getPackageManager().checkPermission(requiredPermission, info2.activityInfo.applicationInfo.packageName, UserHandle.getUserId(info2.activityInfo.applicationInfo.uid));
                        } catch (RemoteException e5) {
                            e = -1;
                        }
                        N = e;
                        if (N != 0) {
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Permission Denial: receiving ");
                            stringBuilder4.append(r2.intent);
                            stringBuilder4.append(" to ");
                            stringBuilder4.append(component.flattenToShortString());
                            stringBuilder4.append(" requires ");
                            stringBuilder4.append(requiredPermission);
                            stringBuilder4.append(" due to sender ");
                            stringBuilder4.append(r2.callerPackage);
                            stringBuilder4.append(" (uid ");
                            stringBuilder4.append(r2.callingUid);
                            stringBuilder4.append(")");
                            Slog.w("BroadcastQueue", stringBuilder4.toString());
                            skip2 = true;
                            break;
                        }
                        int perm;
                        i3 = AppOpsManager.permissionToOpCode(requiredPermission);
                        if (i3 == -1 || i3 == r2.appOp) {
                            perm = N;
                        } else {
                            perm = N;
                            if (this.mService.mAppOpsService.noteOperation(i3, info2.activityInfo.applicationInfo.uid, info2.activityInfo.packageName) != 0) {
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("Appop Denial: receiving ");
                                stringBuilder4.append(r2.intent);
                                stringBuilder4.append(" to ");
                                stringBuilder4.append(component.flattenToShortString());
                                stringBuilder4.append(" requires appop ");
                                stringBuilder4.append(AppOpsManager.permissionToOp(requiredPermission));
                                stringBuilder4.append(" due to sender ");
                                stringBuilder4.append(r2.callerPackage);
                                stringBuilder4.append(" (uid ");
                                stringBuilder4.append(r2.callingUid);
                                stringBuilder4.append(")");
                                Slog.w("BroadcastQueue", stringBuilder4.toString());
                                skip2 = true;
                                N = perm;
                                break;
                            }
                        }
                        i3 = i4 + 1;
                        N = perm;
                    }
                    if (r2.intent != null && "android.provider.Telephony.SMS_RECEIVED".equals(r2.intent.getAction())) {
                        HwSystemManager.insertSendBroadcastRecord(info2.activityInfo.applicationInfo.packageName, r2.intent.getAction(), info2.activityInfo.applicationInfo.uid);
                    }
                }
                i4 = N;
                if (!(skip2 || r2.appOp == -1 || this.mService.mAppOpsService.noteOperation(r2.appOp, info2.activityInfo.applicationInfo.uid, info2.activityInfo.packageName) == 0)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Appop Denial: receiving ");
                    stringBuilder.append(r2.intent);
                    stringBuilder.append(" to ");
                    stringBuilder.append(component.flattenToShortString());
                    stringBuilder.append(" requires appop ");
                    stringBuilder.append(AppOpsManager.opToName(r2.appOp));
                    stringBuilder.append(" due to sender ");
                    stringBuilder.append(r2.callerPackage);
                    stringBuilder.append(" (uid ");
                    stringBuilder.append(r2.callingUid);
                    stringBuilder.append(")");
                    Slog.w("BroadcastQueue", stringBuilder.toString());
                    skip2 = true;
                }
                if (skip2) {
                    skip = skip2;
                } else {
                    skip = this.mService.mIntentFirewall.checkBroadcast(r2.intent, r2.callingUid, r2.callingPid, r2.resolvedType, info2.activityInfo.applicationInfo.uid) ^ 1;
                }
                if (skip) {
                    nextReceiver2 = nextReceiver;
                } else {
                    nextReceiver2 = nextReceiver;
                    if (this.mService.shouldPreventSendBroadcast(r2.intent, info2.activityInfo.packageName, r2.callingUid, r2.callingPid, r2.callerPackage, r2.userId)) {
                        skip = true;
                    }
                }
                z = false;
                try {
                    z = this.mService.isSingleton(info2.activityInfo.processName, info2.activityInfo.applicationInfo, info2.activityInfo.name, info2.activityInfo.flags);
                } catch (SecurityException e6) {
                    Slog.w("BroadcastQueue", e6.getMessage());
                    skip = true;
                }
                isSingleton = z;
                if (!((info2.activityInfo.flags & 1073741824) == 0 || ActivityManager.checkUidPermission("android.permission.INTERACT_ACROSS_USERS", info2.activityInfo.applicationInfo.uid) == 0)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Permission Denial: Receiver ");
                    stringBuilder.append(component.flattenToShortString());
                    stringBuilder.append(" requests FLAG_SINGLE_USER, but app does not hold ");
                    stringBuilder.append("android.permission.INTERACT_ACROSS_USERS");
                    Slog.w("BroadcastQueue", stringBuilder.toString());
                    skip = true;
                }
                if (!(skip || !info2.activityInfo.applicationInfo.isInstantApp() || r2.callingUid == info2.activityInfo.applicationInfo.uid)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Instant App Denial: receiving ");
                    stringBuilder.append(r2.intent);
                    stringBuilder.append(" to ");
                    stringBuilder.append(component.flattenToShortString());
                    stringBuilder.append(" due to sender ");
                    stringBuilder.append(r2.callerPackage);
                    stringBuilder.append(" (uid ");
                    stringBuilder.append(r2.callingUid);
                    stringBuilder.append(") Instant Apps do not support manifest receivers");
                    Slog.w("BroadcastQueue", stringBuilder.toString());
                    skip = true;
                }
                if (!skip && r2.callerInstantApp && (info2.activityInfo.flags & DumpState.DUMP_DEXOPT) == 0 && r2.callingUid != info2.activityInfo.applicationInfo.uid) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Instant App Denial: receiving ");
                    stringBuilder.append(r2.intent);
                    stringBuilder.append(" to ");
                    stringBuilder.append(component.flattenToShortString());
                    stringBuilder.append(" requires receiver have visibleToInstantApps set due to sender ");
                    stringBuilder.append(r2.callerPackage);
                    stringBuilder.append(" (uid ");
                    stringBuilder.append(r2.callingUid);
                    stringBuilder.append(")");
                    Slog.w("BroadcastQueue", stringBuilder.toString());
                    skip = true;
                }
                if (r2.curApp != null && r2.curApp.crashing) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Skipping deliver ordered [");
                    stringBuilder.append(this.mQueueName);
                    stringBuilder.append("] ");
                    stringBuilder.append(r2);
                    stringBuilder.append(" to ");
                    stringBuilder.append(r2.curApp);
                    stringBuilder.append(": process crashing");
                    Slog.w("BroadcastQueue", stringBuilder.toString());
                    skip = true;
                }
                if (!skip) {
                    z = false;
                    try {
                        z = AppGlobals.getPackageManager().isPackageAvailable(info2.activityInfo.packageName, UserHandle.getUserId(info2.activityInfo.applicationInfo.uid));
                    } catch (Exception e7) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Exception getting recipient info for ");
                        stringBuilder2.append(info2.activityInfo.packageName);
                        Slog.w("BroadcastQueue", stringBuilder2.toString(), e7);
                    }
                    if (!z) {
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Skipping delivery to ");
                            stringBuilder.append(info2.activityInfo.packageName);
                            stringBuilder.append(" / ");
                            stringBuilder.append(info2.activityInfo.applicationInfo.uid);
                            stringBuilder.append(" : package no longer available");
                            Slog.v("BroadcastQueue", stringBuilder.toString());
                        }
                        skip = true;
                    }
                }
                if (HwServiceFactory.getHwNLPManager().shouldSkipGoogleNlp(r2.intent, info2.activityInfo.processName)) {
                    skip = true;
                }
                if (!(!this.mService.mPermissionReviewRequired || skip || requestStartTargetPermissionsReviewIfNeededLocked(r2, info2.activityInfo.packageName, UserHandle.getUserId(info2.activityInfo.applicationInfo.uid)))) {
                    skip = true;
                }
                numReceivers = info2.activityInfo.applicationInfo.uid;
                if (r2.callingUid != 1000 && isSingleton && this.mService.isValidSingletonCall(r2.callingUid, numReceivers)) {
                    info2.activityInfo = this.mService.getActivityInfoForUser(info2.activityInfo, 0);
                }
                String targetProcess3 = info2.activityInfo.processName;
                ProcessRecord app = this.mService.getProcessRecordLocked(targetProcess3, info2.activityInfo.applicationInfo.uid, false);
                if (skip) {
                    targetProcess2 = targetProcess3;
                } else {
                    targetProcess2 = targetProcess3;
                    i3 = this.mService.getAppStartModeLocked(info2.activityInfo.applicationInfo.uid, info2.activityInfo.packageName, info2.activityInfo.applicationInfo.targetSdkVersion, -1, true, false, false);
                    if (i3 != 0) {
                        if (i3 == 3) {
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Background execution disabled: receiving ");
                            stringBuilder4.append(r2.intent);
                            stringBuilder4.append(" to ");
                            stringBuilder4.append(component.flattenToShortString());
                            Slog.w("BroadcastQueue", stringBuilder4.toString());
                            skip = true;
                        } else if ((r2.intent.getFlags() & DumpState.DUMP_VOLUMES) != 0 || (r2.intent.getComponent() == null && r2.intent.getPackage() == null && (r2.intent.getFlags() & DumpState.DUMP_SERVICE_PERMISSIONS) == 0 && !this.mService.isExcludedInBGCheck(component.getPackageName(), null) && !isSignaturePerm(r2.requiredPermissions))) {
                            this.mService.addBackgroundCheckViolationLocked(r2.intent.getAction(), component.getPackageName());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Background execution not allowed: receiving ");
                            stringBuilder4.append(r2.intent);
                            stringBuilder4.append(" to ");
                            stringBuilder4.append(component.flattenToShortString());
                            Slog.w("BroadcastQueue", stringBuilder4.toString());
                            skip = true;
                        }
                    }
                }
                if (!(skip || "android.intent.action.ACTION_SHUTDOWN".equals(r2.intent.getAction()) || this.mService.mUserController.isUserRunning(UserHandle.getUserId(info2.activityInfo.applicationInfo.uid), 0))) {
                    skip = true;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Skipping delivery to ");
                    stringBuilder.append(info2.activityInfo.packageName);
                    stringBuilder.append(" / ");
                    stringBuilder.append(info2.activityInfo.applicationInfo.uid);
                    stringBuilder.append(" : user is not running");
                    Slog.w("BroadcastQueue", stringBuilder.toString());
                }
                if (skip) {
                } else {
                    if (this.mService.shouldPreventSendReceiver(r2.intent, info2, r2.callingPid, r2.callingUid, app, r2.callerApp)) {
                        skip = true;
                    }
                }
                if (skip) {
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Skipping delivery of ordered [");
                        stringBuilder.append(this.mQueueName);
                        stringBuilder.append("] ");
                        stringBuilder.append(r2);
                        stringBuilder.append(" for whatever reason");
                        Slog.v("BroadcastQueue", stringBuilder.toString());
                    }
                    r2.delivery[recIdx] = 2;
                    r2.receiver = null;
                    r2.curFilter = null;
                    r2.state = 0;
                    r2.manifestSkipCount++;
                    scheduleBroadcastsLocked();
                    return;
                }
                String str;
                int receiverUid;
                String targetProcess4;
                r2.manifestCount++;
                r2.delivery[recIdx] = 1;
                r2.state = 1;
                r2.curComponent = component;
                r2.curReceiver = info2.activityInfo;
                if (ActivityManagerDebugConfig.DEBUG_MU && r2.callingUid > 100000) {
                    str = TAG_MU;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Updated broadcast record activity info for secondary user, ");
                    stringBuilder.append(info2.activityInfo);
                    stringBuilder.append(", callingUid = ");
                    stringBuilder.append(r2.callingUid);
                    stringBuilder.append(", uid = ");
                    stringBuilder.append(numReceivers);
                    Slog.v(str, stringBuilder.toString());
                }
                if (brOptions != null && brOptions.getTemporaryAppWhitelistDuration() > 0) {
                    scheduleTempWhitelistLocked(numReceivers, brOptions.getTemporaryAppWhitelistDuration(), r2);
                }
                try {
                    AppGlobals.getPackageManager().setPackageStoppedState(r2.curComponent.getPackageName(), false, UserHandle.getUserId(r2.callingUid));
                } catch (RemoteException e8) {
                } catch (IllegalArgumentException e9) {
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Failed trying to unstop package ");
                    stringBuilder4.append(r2.curComponent.getPackageName());
                    stringBuilder4.append(": ");
                    stringBuilder4.append(e9);
                    Slog.w("BroadcastQueue", stringBuilder4.toString());
                }
                ComponentName componentName;
                Object obj;
                if (app == null || app.thread == null || app.killed) {
                    componentName = component;
                    info = info2;
                    receiverUid = numReceivers;
                    obj = nextReceiver2;
                    targetProcess4 = targetProcess2;
                } else {
                    try {
                        try {
                            app.addPackage(info2.activityInfo.packageName, (long) info2.activityInfo.applicationInfo.versionCode, this.mService.mProcessStats);
                            processCurBroadcastLocked(r2, app, skipOomAdj);
                            return;
                        } catch (RemoteException e10) {
                        } catch (RuntimeException e11) {
                            RuntimeException e12 = e11;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Failed sending broadcast to ");
                            stringBuilder4.append(r2.curComponent);
                            stringBuilder4.append(" with ");
                            stringBuilder4.append(r2.intent);
                            Slog.wtf("BroadcastQueue", stringBuilder4.toString(), e12);
                            logBroadcastReceiverDiscardLocked(r2);
                            finishReceiverLocked(r2, r2.resultCode, r2.resultData, r2.resultExtras, r2.resultAbort, 0);
                            scheduleBroadcastsLocked();
                            r2.state = 0;
                            return;
                        }
                    } catch (RemoteException e13) {
                        e = e13;
                        ProcessRecord processRecord = app;
                        componentName = component;
                        info = info2;
                        receiverUid = numReceivers;
                        obj = nextReceiver2;
                        targetProcess4 = targetProcess2;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Exception when sending broadcast to ");
                        stringBuilder4.append(r2.curComponent);
                        Slog.w("BroadcastQueue", stringBuilder4.toString(), e);
                        if (this.mService.mUserController.getCurrentUserId() == 0) {
                        }
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        }
                        if (getMtmBRManagerEnabled(11)) {
                        }
                        if ((r2.intent.getFlags() & DumpState.DUMP_HANDLE) != 0) {
                        }
                        proc = this.mService.startProcessLocked(targetProcess, info.activityInfo.applicationInfo, true, r2.intent.getFlags() | 4, "broadcast", r2.curComponent, (r2.intent.getFlags() & DumpState.DUMP_HANDLE) != 0 ? skip2 : false, false, false);
                        r2.curApp = proc;
                        if (proc == null) {
                        }
                    } catch (RuntimeException e14) {
                    }
                }
                if (this.mService.mUserController.getCurrentUserId() == 0) {
                    try {
                        str = info.activityInfo.applicationInfo.packageName;
                        String sourceDir = info.activityInfo.applicationInfo.sourceDir;
                        String publicSourceDir = info.activityInfo.applicationInfo.publicSourceDir;
                        ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(str, 0, UserHandle.getUserId(info.activityInfo.applicationInfo.uid));
                        if (applicationInfo != null) {
                            if (!(applicationInfo.sourceDir.equals(sourceDir) && applicationInfo.publicSourceDir.equals(publicSourceDir))) {
                                StringBuilder stringBuilder8 = new StringBuilder();
                                stringBuilder8.append(str);
                                stringBuilder8.append(" is replaced, sourceDir is changed from ");
                                stringBuilder8.append(sourceDir);
                                stringBuilder8.append(" to ");
                                stringBuilder8.append(applicationInfo.sourceDir);
                                stringBuilder8.append(", publicSourceDir is changed from ");
                                stringBuilder8.append(publicSourceDir);
                                stringBuilder8.append(" to ");
                                stringBuilder8.append(applicationInfo.publicSourceDir);
                                Slog.e("BroadcastQueue", stringBuilder8.toString());
                                info.activityInfo.applicationInfo = applicationInfo;
                            }
                        } else {
                            return;
                        }
                    } catch (RemoteException e15) {
                    }
                }
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Need to start app [");
                    stringBuilder.append(this.mQueueName);
                    stringBuilder.append("] ");
                    targetProcess = targetProcess4;
                    stringBuilder.append(targetProcess);
                    stringBuilder.append(" for broadcast ");
                    stringBuilder.append(r2);
                    Slog.v("BroadcastQueue", stringBuilder.toString());
                } else {
                    targetProcess = targetProcess4;
                }
                if (getMtmBRManagerEnabled(11)) {
                    skip2 = true;
                    if (getMtmBRManager().iawareProcessBroadcast(1, false, r2, target2)) {
                        return;
                    }
                }
                skip2 = true;
                proc = this.mService.startProcessLocked(targetProcess, info.activityInfo.applicationInfo, true, r2.intent.getFlags() | 4, "broadcast", r2.curComponent, (r2.intent.getFlags() & DumpState.DUMP_HANDLE) != 0 ? skip2 : false, false, false);
                r2.curApp = proc;
                if (proc == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to launch app ");
                    stringBuilder.append(info.activityInfo.applicationInfo.packageName);
                    stringBuilder.append(SliceAuthority.DELIMITER);
                    stringBuilder.append(receiverUid);
                    stringBuilder.append(" for broadcast ");
                    stringBuilder.append(r2.intent);
                    stringBuilder.append(": process is bad");
                    Slog.w("BroadcastQueue", stringBuilder.toString());
                    logBroadcastReceiverDiscardLocked(r2);
                    finishReceiverLocked(r2, r2.resultCode, r2.resultData, r2.resultExtras, r2.resultAbort, null);
                    scheduleBroadcastsLocked();
                    r2.state = 0;
                    return;
                }
                this.mPendingBroadcast = r2;
                this.mPendingBroadcastRecvIndex = recIdx;
                return;
            }
            boolean recIdx2 = isSingleton;
            isDead = looped;
        }
    }

    final void setBroadcastTimeoutLocked(long timeoutTime) {
        if (!this.mPendingBroadcastTimeoutMessage) {
            this.mHandler.sendMessageAtTime(this.mHandler.obtainMessage(BROADCAST_TIMEOUT_MSG, this), timeoutTime);
            Message msg = this.mHandler.obtainMessage(BROADCAST_CHECKTIMEOUT_MSG, this);
            if ("background".equals(this.mQueueName) || "bgthirdapp".equals(this.mQueueName) || "bgkeyapp".equals(this.mQueueName)) {
                this.mHandler.sendMessageDelayed(msg, 5000);
            } else {
                this.mHandler.sendMessageDelayed(msg, 2000);
            }
            this.mPendingBroadcastTimeoutMessage = true;
        }
    }

    final void cancelBroadcastTimeoutLocked() {
        if (this.mPendingBroadcastTimeoutMessage) {
            this.mHandler.removeMessages(BROADCAST_TIMEOUT_MSG, this);
            this.mHandler.removeMessages(BROADCAST_CHECKTIMEOUT_MSG, this);
            this.mPendingBroadcastTimeoutMessage = false;
        }
    }

    final void broadcastTimeoutLocked(boolean fromMsg) {
        boolean z = false;
        if (fromMsg) {
            this.mPendingBroadcastTimeoutMessage = false;
        }
        if (this.mOrderedBroadcasts.size() != 0) {
            long now = SystemClock.uptimeMillis();
            BroadcastRecord r = (BroadcastRecord) this.mOrderedBroadcasts.get(0);
            if (fromMsg) {
                if (this.mService.mProcessesReady) {
                    long timeoutTime = r.receiverTime + this.mTimeoutPeriod;
                    long checkTime = r.receiverTime + ((long) CHECK_INTERVAL);
                    if (checkTime > now) {
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Premature timeout [");
                            stringBuilder.append(this.mQueueName);
                            stringBuilder.append("] @ ");
                            stringBuilder.append(now);
                            stringBuilder.append(": resetting BROADCAST_TIMEOUT_MSG for ");
                            stringBuilder.append(timeoutTime);
                            stringBuilder.append(". Next frozen check time is ");
                            stringBuilder.append(checkTime);
                            Slog.v("BroadcastQueue", stringBuilder.toString());
                        }
                        setBroadcastTimeoutLocked(checkTime);
                        return;
                    } else if (timeoutTime > now) {
                        if (this.mService.isTopProcessLocked(((BroadcastRecord) this.mOrderedBroadcasts.get(0)).curApp)) {
                            long anrTimeDiff = timeoutTime - now;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("ANR has been triggered ");
                            stringBuilder2.append(anrTimeDiff);
                            stringBuilder2.append("ms earlier because it caused frozen problem.");
                            Slog.w("BroadcastQueue", stringBuilder2.toString());
                        } else {
                            setBroadcastTimeoutLocked(((long) CHECK_INTERVAL) + now);
                            return;
                        }
                    }
                }
                return;
            }
            BroadcastRecord br = (BroadcastRecord) this.mOrderedBroadcasts.get(0);
            StringBuilder stringBuilder3;
            if (br.state == 4) {
                String str = "BroadcastQueue";
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Waited long enough for: ");
                stringBuilder3.append(br.curComponent != null ? br.curComponent.flattenToShortString() : "(null)");
                Slog.i(str, stringBuilder3.toString());
                br.curComponent = null;
                br.state = 0;
                processNextBroadcast(false);
                return;
            }
            BroadcastFilter curReceiver;
            if (r.curApp != null && r.curApp.debugging) {
                z = true;
            }
            boolean debugging = z;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Timeout of broadcast ");
            stringBuilder4.append(r);
            stringBuilder4.append(" - receiver=");
            stringBuilder4.append(r.receiver);
            stringBuilder4.append(", started ");
            stringBuilder4.append(now - r.receiverTime);
            stringBuilder4.append("ms ago");
            Slog.w("BroadcastQueue", stringBuilder4.toString());
            r.receiverTime = now;
            if (!debugging) {
                r.anrCount++;
            }
            ProcessRecord app = null;
            String anrMessage = null;
            if (r.nextReceiver > 0) {
                curReceiver = r.receivers.get(r.nextReceiver - 1);
                r.delivery[r.nextReceiver - 1] = 3;
            } else {
                curReceiver = r.curReceiver;
            }
            BroadcastFilter curReceiver2 = curReceiver;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Receiver during timeout of ");
            stringBuilder3.append(r);
            stringBuilder3.append(" : ");
            stringBuilder3.append(curReceiver2);
            Slog.w("BroadcastQueue", stringBuilder3.toString());
            logBroadcastReceiverDiscardLocked(r);
            if (curReceiver2 == null || !(curReceiver2 instanceof BroadcastFilter)) {
                app = r.curApp;
            } else {
                BroadcastFilter bf = curReceiver2;
                if (!(bf.receiverList.pid == 0 || bf.receiverList.pid == ActivityManagerService.MY_PID)) {
                    synchronized (this.mService.mPidsSelfLocked) {
                        app = (ProcessRecord) this.mService.mPidsSelfLocked.get(bf.receiverList.pid);
                    }
                }
            }
            ProcessRecord app2 = app;
            if (app2 != null) {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Broadcast of ");
                stringBuilder4.append(r.intent.toString());
                anrMessage = stringBuilder4.toString();
            }
            String anrMessage2 = anrMessage;
            if (this.mPendingBroadcast == r) {
                this.mPendingBroadcast = null;
            }
            String anrMessage3 = anrMessage2;
            finishReceiverLocked(r, r.resultCode, r.resultData, r.resultExtras, r.resultAbort, null);
            scheduleBroadcastsLocked();
            if (anrMessage3 == null || !"android.intent.action.PRE_BOOT_COMPLETED".equals(r.intent.getAction())) {
                if (!(debugging || anrMessage3 == null)) {
                    this.mHandler.post(new AppNotResponding(app2, anrMessage3));
                }
                return;
            }
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Skip anr of PRE_BOOT_COMPLETED for app :");
            stringBuilder3.append(app2);
            Slog.w("BroadcastQueue", stringBuilder3.toString());
        }
    }

    private final int ringAdvance(int x, int increment, int ringSize) {
        x += increment;
        if (x < 0) {
            return ringSize - 1;
        }
        if (x >= ringSize) {
            return 0;
        }
        return x;
    }

    private final void addBroadcastToHistoryLocked(BroadcastRecord original) {
        if (original.callingUid >= 0) {
            original.finishTime = SystemClock.uptimeMillis();
            if (Trace.isTagEnabled(64)) {
                Trace.asyncTraceEnd(64, createBroadcastTraceTitle(original, 1), System.identityHashCode(original));
            }
            BroadcastRecord historyRecord = original.maybeStripForHistory();
            this.mBroadcastHistory[this.mHistoryNext] = historyRecord;
            this.mHistoryNext = ringAdvance(this.mHistoryNext, 1, MAX_BROADCAST_HISTORY);
            this.mBroadcastSummaryHistory[this.mSummaryHistoryNext] = historyRecord.intent;
            this.mSummaryHistoryEnqueueTime[this.mSummaryHistoryNext] = historyRecord.enqueueClockTime;
            this.mSummaryHistoryDispatchTime[this.mSummaryHistoryNext] = historyRecord.dispatchClockTime;
            this.mSummaryHistoryFinishTime[this.mSummaryHistoryNext] = System.currentTimeMillis();
            this.mSummaryHistoryNext = ringAdvance(this.mSummaryHistoryNext, 1, MAX_BROADCAST_SUMMARY_HISTORY);
        }
    }

    boolean cleanupDisabledPackageReceiversLocked(String packageName, Set<String> filterByClasses, int userId, boolean doit) {
        int i;
        boolean didSomething = false;
        for (i = this.mParallelBroadcasts.size() - 1; i >= 0; i--) {
            didSomething |= ((BroadcastRecord) this.mParallelBroadcasts.get(i)).cleanupDisabledPackageReceiversLocked(packageName, filterByClasses, userId, doit);
            if (!doit && didSomething) {
                return true;
            }
        }
        for (i = this.mOrderedBroadcasts.size() - 1; i >= 0; i--) {
            didSomething |= ((BroadcastRecord) this.mOrderedBroadcasts.get(i)).cleanupDisabledPackageReceiversLocked(packageName, filterByClasses, userId, doit);
            if (!doit && didSomething) {
                return true;
            }
        }
        return didSomething;
    }

    final void logBroadcastReceiverDiscardLocked(BroadcastRecord r) {
        int logIndex = r.nextReceiver - 1;
        if (logIndex < 0 || logIndex >= r.receivers.size()) {
            if (logIndex < 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Discarding broadcast before first receiver is invoked: ");
                stringBuilder.append(r);
                Slog.w("BroadcastQueue", stringBuilder.toString());
            }
            EventLog.writeEvent(EventLogTags.AM_BROADCAST_DISCARD_APP, new Object[]{Integer.valueOf(-1), Integer.valueOf(System.identityHashCode(r)), r.intent.getAction(), Integer.valueOf(r.nextReceiver), "NONE"});
            return;
        }
        BroadcastFilter curReceiver = r.receivers.get(logIndex);
        if (curReceiver instanceof BroadcastFilter) {
            BroadcastFilter bf = curReceiver;
            EventLog.writeEvent(EventLogTags.AM_BROADCAST_DISCARD_FILTER, new Object[]{Integer.valueOf(bf.owningUserId), Integer.valueOf(System.identityHashCode(r)), r.intent.getAction(), Integer.valueOf(logIndex), Integer.valueOf(System.identityHashCode(bf))});
            return;
        }
        ResolveInfo ri = (ResolveInfo) curReceiver;
        EventLog.writeEvent(EventLogTags.AM_BROADCAST_DISCARD_APP, new Object[]{Integer.valueOf(UserHandle.getUserId(ri.activityInfo.applicationInfo.uid)), Integer.valueOf(System.identityHashCode(r)), r.intent.getAction(), Integer.valueOf(logIndex), ri.toString()});
    }

    private String createBroadcastTraceTitle(BroadcastRecord record, int state) {
        String str = "Broadcast %s from %s (%s) %s";
        Object[] objArr = new Object[4];
        objArr[0] = state == 0 ? "in queue" : "dispatched";
        objArr[1] = record.callerPackage == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : record.callerPackage;
        objArr[2] = record.callerApp == null ? "process unknown" : record.callerApp.toShortString();
        objArr[3] = record.intent == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : record.intent.getAction();
        return String.format(str, objArr);
    }

    final boolean isIdle() {
        return this.mParallelBroadcasts.isEmpty() && this.mOrderedBroadcasts.isEmpty() && this.mPendingBroadcast == null;
    }

    void writeToProto(ProtoOutputStream proto, long fieldId) {
        int i;
        int i2;
        int i3;
        ProtoOutputStream protoOutputStream = proto;
        long token = proto.start(fieldId);
        protoOutputStream.write(1138166333441L, this.mQueueName);
        for (i = this.mParallelBroadcasts.size() - 1; i >= 0; i--) {
            ((BroadcastRecord) this.mParallelBroadcasts.get(i)).writeToProto(protoOutputStream, 2246267895810L);
        }
        for (i2 = this.mOrderedBroadcasts.size() - 1; i2 >= 0; i2--) {
            ((BroadcastRecord) this.mOrderedBroadcasts.get(i2)).writeToProto(protoOutputStream, 2246267895811L);
        }
        if (this.mPendingBroadcast != null) {
            this.mPendingBroadcast.writeToProto(protoOutputStream, 1146756268036L);
        }
        i2 = this.mHistoryNext;
        i = i2;
        do {
            i3 = -1;
            i = ringAdvance(i, -1, MAX_BROADCAST_HISTORY);
            BroadcastRecord r = this.mBroadcastHistory[i];
            if (r != null) {
                r.writeToProto(protoOutputStream, 2246267895813L);
                continue;
            }
        } while (i != i2);
        int i4 = this.mSummaryHistoryNext;
        i = i4;
        int lastIndex = i4;
        while (true) {
            int lastIndex2;
            int ringIndex = ringAdvance(i, i3, MAX_BROADCAST_SUMMARY_HISTORY);
            Intent intent = this.mBroadcastSummaryHistory[ringIndex];
            if (intent == null) {
                lastIndex2 = lastIndex;
            } else {
                lastIndex2 = lastIndex;
                long summaryToken = protoOutputStream.start(2246267895814L);
                intent.writeToProto(protoOutputStream, 1146756268033L, false, true, true, false);
                protoOutputStream.write(1112396529666L, this.mSummaryHistoryEnqueueTime[ringIndex]);
                protoOutputStream.write(1112396529667L, this.mSummaryHistoryDispatchTime[ringIndex]);
                protoOutputStream.write(1112396529668L, this.mSummaryHistoryFinishTime[ringIndex]);
                protoOutputStream.end(summaryToken);
            }
            i4 = lastIndex2;
            if (ringIndex == i4) {
                protoOutputStream.end(token);
                return;
            }
            lastIndex = i4;
            i = ringIndex;
            i3 = -1;
        }
    }

    boolean dumpLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage, boolean needSep) {
        boolean needSep2;
        int i;
        StringBuilder stringBuilder;
        PrintWriter printWriter = pw;
        String str = dumpPackage;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        boolean z = true;
        if (this.mParallelBroadcasts.size() > 0 || this.mOrderedBroadcasts.size() > 0 || this.mPendingBroadcast != null) {
            BroadcastRecord br;
            StringBuilder stringBuilder2;
            boolean printed = false;
            boolean needSep3 = needSep;
            for (int i2 = this.mParallelBroadcasts.size() - 1; i2 >= 0; i2--) {
                br = (BroadcastRecord) this.mParallelBroadcasts.get(i2);
                if (str == null || str.equals(br.callerPackage)) {
                    if (!printed) {
                        if (needSep3) {
                            pw.println();
                        }
                        needSep3 = true;
                        printed = true;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("  Active broadcasts [");
                        stringBuilder2.append(this.mQueueName);
                        stringBuilder2.append("]:");
                        printWriter.println(stringBuilder2.toString());
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  Active Broadcast ");
                    stringBuilder2.append(this.mQueueName);
                    stringBuilder2.append(" #");
                    stringBuilder2.append(i2);
                    stringBuilder2.append(":");
                    printWriter.println(stringBuilder2.toString());
                    br.dump(printWriter, "    ", sdf);
                }
            }
            printed = false;
            needSep2 = true;
            for (i = this.mOrderedBroadcasts.size() - 1; i >= 0; i--) {
                br = (BroadcastRecord) this.mOrderedBroadcasts.get(i);
                if (str == null || str.equals(br.callerPackage)) {
                    if (!printed) {
                        if (needSep2) {
                            pw.println();
                        }
                        needSep2 = true;
                        printed = true;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("  Active ordered broadcasts [");
                        stringBuilder2.append(this.mQueueName);
                        stringBuilder2.append("]:");
                        printWriter.println(stringBuilder2.toString());
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  Active Ordered Broadcast ");
                    stringBuilder2.append(this.mQueueName);
                    stringBuilder2.append(" #");
                    stringBuilder2.append(i);
                    stringBuilder2.append(":");
                    printWriter.println(stringBuilder2.toString());
                    ((BroadcastRecord) this.mOrderedBroadcasts.get(i)).dump(printWriter, "    ", sdf);
                }
            }
            if (str == null || (this.mPendingBroadcast != null && str.equals(this.mPendingBroadcast.callerPackage))) {
                if (needSep2) {
                    pw.println();
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("  Pending broadcast [");
                stringBuilder3.append(this.mQueueName);
                stringBuilder3.append("]:");
                printWriter.println(stringBuilder3.toString());
                if (this.mPendingBroadcast != null) {
                    this.mPendingBroadcast.dump(printWriter, "    ", sdf);
                } else {
                    printWriter.println("    (null)");
                }
                needSep2 = true;
            }
        } else {
            needSep2 = needSep;
        }
        i = -1;
        int lastIndex = this.mHistoryNext;
        boolean needSep4 = needSep2;
        needSep2 = false;
        int ringIndex = lastIndex;
        do {
            ringIndex = ringAdvance(ringIndex, -1, MAX_BROADCAST_HISTORY);
            BroadcastRecord r = this.mBroadcastHistory[ringIndex];
            if (r != null) {
                i++;
                if (str == null || str.equals(r.callerPackage)) {
                    if (!needSep2) {
                        if (needSep4) {
                            pw.println();
                        }
                        needSep4 = true;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("  Historical broadcasts [");
                        stringBuilder.append(this.mQueueName);
                        stringBuilder.append("]:");
                        printWriter.println(stringBuilder.toString());
                        needSep2 = true;
                    }
                    if (dumpAll) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("  Historical Broadcast ");
                        stringBuilder.append(this.mQueueName);
                        stringBuilder.append(" #");
                        printWriter.print(stringBuilder.toString());
                        printWriter.print(i);
                        printWriter.println(":");
                        r.dump(printWriter, "    ", sdf);
                        continue;
                    } else {
                        printWriter.print("  #");
                        printWriter.print(i);
                        printWriter.print(": ");
                        printWriter.println(r);
                        printWriter.print("    ");
                        printWriter.println(r.intent.toShortString(true, true, true, false));
                        if (!(r.targetComp == null || r.targetComp == r.intent.getComponent())) {
                            printWriter.print("    targetComp: ");
                            printWriter.println(r.targetComp.toShortString());
                        }
                        Bundle bundle = r.intent.getExtras();
                        if (!(bundle == null || isFromEmailMDM(r.intent))) {
                            printWriter.print("    extras: ");
                            printWriter.println(bundle.toString());
                            continue;
                        }
                    }
                }
            }
        } while (ringIndex != lastIndex);
        if (str == null) {
            int lastIndex2 = this.mSummaryHistoryNext;
            ringIndex = lastIndex2;
            if (dumpAll) {
                needSep2 = false;
                i = -1;
                lastIndex = ringIndex;
            } else {
                lastIndex = ringIndex;
                ringIndex = i;
                while (ringIndex > 0 && lastIndex != lastIndex2) {
                    lastIndex = ringAdvance(lastIndex, -1, MAX_BROADCAST_SUMMARY_HISTORY);
                    if (this.mBroadcastHistory[lastIndex] != null) {
                        ringIndex--;
                    }
                }
            }
            while (true) {
                lastIndex = ringAdvance(lastIndex, -1, MAX_BROADCAST_SUMMARY_HISTORY);
                ringIndex = this.mBroadcastSummaryHistory[lastIndex];
                if (ringIndex != 0) {
                    if (!needSep2) {
                        if (needSep4) {
                            pw.println();
                        }
                        needSep4 = true;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("  Historical broadcasts summary [");
                        stringBuilder.append(this.mQueueName);
                        stringBuilder.append("]:");
                        printWriter.println(stringBuilder.toString());
                        needSep2 = true;
                    }
                    if (!dumpAll && i >= 50) {
                        printWriter.println("  ...");
                        break;
                    }
                    i++;
                    printWriter.print("  #");
                    printWriter.print(i);
                    printWriter.print(": ");
                    printWriter.println(ringIndex.toShortString(false, z, z, false));
                    printWriter.print("    ");
                    TimeUtils.formatDuration(this.mSummaryHistoryDispatchTime[lastIndex] - this.mSummaryHistoryEnqueueTime[lastIndex], printWriter);
                    printWriter.print(" dispatch ");
                    TimeUtils.formatDuration(this.mSummaryHistoryFinishTime[lastIndex] - this.mSummaryHistoryDispatchTime[lastIndex], printWriter);
                    printWriter.println(" finish");
                    printWriter.print("    enq=");
                    printWriter.print(sdf.format(new Date(this.mSummaryHistoryEnqueueTime[lastIndex])));
                    printWriter.print(" disp=");
                    printWriter.print(sdf.format(new Date(this.mSummaryHistoryDispatchTime[lastIndex])));
                    printWriter.print(" fin=");
                    printWriter.println(sdf.format(new Date(this.mSummaryHistoryFinishTime[lastIndex])));
                    Bundle bundle2 = ringIndex.getExtras();
                    if (!(bundle2 == null || "android.intent.action.PHONE_STATE".equals(ringIndex.getAction()) || "android.intent.action.NEW_OUTGOING_CALL".equals(ringIndex.getAction()) || isFromEmailMDM(ringIndex))) {
                        printWriter.print("    extras: ");
                        printWriter.println(bundle2.toString());
                    }
                }
                if (lastIndex == lastIndex2) {
                    break;
                }
                z = true;
            }
        }
        return needSep4;
    }

    private boolean isFromEmailMDM(Intent intent) {
        if (intent == null) {
            return false;
        }
        return "com.huawei.devicepolicy.action.POLICY_CHANGED".equals(intent.getAction());
    }

    public void cleanupBroadcastLocked(ProcessRecord app) {
    }

    private void updateSRMSStatisticsData(BroadcastRecord r) {
        if (!mIsBetaUser || !this.mService.getIawareResourceFeature(1)) {
            return;
        }
        if ("bgkeyapp".equals(this.mQueueName) || "fgkeyapp".equals(this.mQueueName)) {
            long elapsedTime = r.dispatchClockTime - r.enqueueClockTime;
            if (elapsedTime >= 0 && elapsedTime <= 20) {
                this.mService.updateSRMSStatisticsData(10);
            } else if (elapsedTime > 20 && elapsedTime <= 60) {
                this.mService.updateSRMSStatisticsData(11);
            } else if (elapsedTime > 60 && elapsedTime <= 100) {
                this.mService.updateSRMSStatisticsData(12);
            } else if (elapsedTime > 100) {
                this.mService.updateSRMSStatisticsData(13);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("elapsedTime error [");
                stringBuilder.append(this.mQueueName);
                stringBuilder.append("] for ");
                stringBuilder.append(r);
                Slog.w("BroadcastQueue", stringBuilder.toString());
            }
        }
    }
}
