package com.android.server.power;

import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManagerInternal;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.metrics.LogMaker;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.WorkSource;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.util.EventLog;
import android.util.Flog;
import android.util.Jlog;
import android.util.Log;
import android.util.Slog;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.app.IBatteryStats;
import com.android.internal.logging.MetricsLogger;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.pg.PGManagerInternal;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.huawei.pgmng.common.Utils;

final class Notifier {
    private static boolean DEBUG = false;
    private static final int INTERACTIVE_STATE_ASLEEP = 2;
    private static final int INTERACTIVE_STATE_AWAKE = 1;
    private static final int INTERACTIVE_STATE_UNKNOWN = 0;
    private static final int MSG_BROADCAST = 2;
    private static final int MSG_PROFILE_TIMED_OUT = 5;
    private static final int MSG_RESUME_SYSTEM_BRIGHTNESS = 100;
    private static final int MSG_SCREEN_BRIGHTNESS_BOOST_CHANGED = 4;
    private static final int MSG_USER_ACTIVITY = 1;
    private static final int MSG_WIRED_CHARGING_STARTED = 6;
    private static final int MSG_WIRELESS_CHARGING_STARTED = 3;
    private static final String TAG = "PowerManagerNotifier";
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new Builder().setContentType(4).build();
    private static final VibrationEffect WIRELESS_CHARGING_VIBRATION_EFFECT = VibrationEffect.createWaveform(WIRELESS_VIBRATION_TIME, WIRELESS_VIBRATION_AMPLITUDE, -1);
    private static final int[] WIRELESS_VIBRATION_AMPLITUDE = new int[]{1, 4, 11, 25, 44, 67, 91, 114, 123, 103, 79, 55, 34, 17, 7, 2};
    private static final long[] WIRELESS_VIBRATION_TIME = new long[]{40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40};
    private static final boolean mFactoryModeEnable = "factory".equals(SystemProperties.get("ro.runmode", "normal"));
    private final ActivityManagerInternal mActivityManagerInternal;
    private final AppOpsManager mAppOps;
    private final IBatteryStats mBatteryStats;
    private boolean mBrightnessModeChangeNoClearOffset = false;
    private boolean mBroadcastInProgress;
    private long mBroadcastStartTime;
    private int mBroadcastedInteractiveState;
    private final Context mContext;
    private final BroadcastReceiver mGoToSleepBroadcastDone = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_DONE, new Object[]{Integer.valueOf(0), Long.valueOf(SystemClock.uptimeMillis() - Notifier.this.mBroadcastStartTime), Integer.valueOf(1)});
            Notifier.this.sendNextBroadcast();
        }
    };
    private final NotifierHandler mHandler;
    private final InputManagerInternal mInputManagerInternal;
    private final InputMethodManagerInternal mInputMethodManagerInternal;
    private boolean mInteractive = true;
    private int mInteractiveChangeReason;
    private boolean mInteractiveChanging;
    private final Object mLock = new Object();
    private PGManagerInternal mPGManagerInternal;
    private boolean mPendingGoToSleepBroadcast;
    private int mPendingInteractiveState;
    private boolean mPendingWakeUpBroadcast;
    private final WindowManagerPolicy mPolicy;
    private final BroadcastReceiver mScreeBrightnessBoostChangedDone = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Notifier.this.mSuspendBlocker.release();
        }
    };
    private final Intent mScreenBrightnessBoostIntent;
    private final Intent mScreenOffIntent;
    private final Intent mScreenOnIntent;
    private final StatusBarManagerInternal mStatusBarManagerInternal;
    private final SuspendBlocker mSuspendBlocker;
    private final boolean mSuspendWhenScreenOffDueToProximityConfig;
    private final TrustManager mTrustManager;
    private boolean mUserActivityPending;
    private final Vibrator mVibrator;
    private final BroadcastReceiver mWakeUpBroadcastDone = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_DONE, new Object[]{Integer.valueOf(1), Long.valueOf(SystemClock.uptimeMillis() - Notifier.this.mBroadcastStartTime), Integer.valueOf(1)});
            Notifier.this.sendNextBroadcast();
        }
    };

    private final class NotifierHandler extends Handler {
        public NotifierHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i != 100) {
                switch (i) {
                    case 1:
                        Notifier.this.sendUserActivity();
                        return;
                    case 2:
                        Notifier.this.sendNextBroadcast();
                        return;
                    case 3:
                        Notifier.this.showWirelessChargingStarted(msg.arg1);
                        return;
                    case 4:
                        Notifier.this.sendBrightnessBoostChangedBroadcast();
                        return;
                    case 5:
                        Notifier.this.lockProfile(msg.arg1);
                        return;
                    case 6:
                        Notifier.this.showWiredChargingStarted();
                        return;
                    default:
                        return;
                }
            }
            Notifier.this.resumeSystemBrightness();
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    public Notifier(Looper looper, Context context, IBatteryStats batteryStats, SuspendBlocker suspendBlocker, WindowManagerPolicy policy) {
        this.mContext = context;
        this.mBatteryStats = batteryStats;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mSuspendBlocker = suspendBlocker;
        this.mPolicy = policy;
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        this.mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
        this.mInputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
        this.mStatusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
        this.mTrustManager = (TrustManager) this.mContext.getSystemService(TrustManager.class);
        this.mVibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        this.mHandler = new NotifierHandler(looper);
        this.mScreenOnIntent = new Intent("android.intent.action.SCREEN_ON");
        this.mScreenOnIntent.addFlags(1344274432);
        this.mScreenOffIntent = new Intent("android.intent.action.SCREEN_OFF");
        this.mScreenOffIntent.addFlags(1478492160);
        this.mScreenBrightnessBoostIntent = new Intent("android.os.action.SCREEN_BRIGHTNESS_BOOST_CHANGED");
        this.mScreenBrightnessBoostIntent.addFlags(1342177280);
        this.mSuspendWhenScreenOffDueToProximityConfig = context.getResources().getBoolean(17957045);
        try {
            this.mBatteryStats.noteInteractive(true);
        } catch (RemoteException e) {
        }
    }

    public void onWakeLockAcquired(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource, String historyTag) {
        String str;
        String str2 = packageName;
        int i = ownerUid;
        WorkSource workSource2 = workSource;
        Utils.noteWakelock(flags, tag, i, ownerPid, workSource2, 160);
        int monitorType = getBatteryStatsWakeLockMonitorType(flags);
        WorkSource workSource3;
        if (monitorType >= 0) {
            boolean z = i == 1000 && (flags & 1073741824) != 0;
            boolean unimportantForLogging = z;
            if (workSource2 != null) {
                try {
                    this.mBatteryStats.noteStartWakelockFromSource(workSource2, ownerPid, tag, historyTag, monitorType, unimportantForLogging);
                    workSource3 = workSource2;
                } catch (RemoteException e) {
                    str = tag;
                    workSource3 = workSource2;
                    return;
                }
            }
            workSource3 = workSource2;
            try {
                this.mBatteryStats.noteStartWakelock(i, ownerPid, tag, historyTag, monitorType, unimportantForLogging);
                this.mAppOps.startOpNoThrow(40, i, str2);
            } catch (RemoteException e2) {
                str = tag;
                return;
            }
            if (monitorType == 0) {
                if (this.mPGManagerInternal == null) {
                    this.mPGManagerInternal = (PGManagerInternal) LocalServices.getService(PGManagerInternal.class);
                }
                if (this.mPGManagerInternal != null) {
                    try {
                        this.mPGManagerInternal.noteStartWakeLock(tag, workSource3, str2, i);
                        return;
                    } catch (RemoteException e3) {
                        return;
                    }
                }
            }
            str = tag;
            return;
        }
        str = tag;
        workSource3 = workSource2;
    }

    public void onLongPartialWakeLockStart(String tag, int ownerUid, WorkSource workSource, String historyTag) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onLongPartialWakeLockStart: ownerUid=");
            stringBuilder.append(ownerUid);
            stringBuilder.append(", workSource=");
            stringBuilder.append(workSource);
            Slog.d(str, stringBuilder.toString());
        }
        if (workSource != null) {
            try {
                this.mBatteryStats.noteLongPartialWakelockStartFromSource(tag, historyTag, workSource);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        this.mBatteryStats.noteLongPartialWakelockStart(tag, historyTag, ownerUid);
    }

    public void onLongPartialWakeLockFinish(String tag, int ownerUid, WorkSource workSource, String historyTag) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onLongPartialWakeLockFinish: ownerUid=");
            stringBuilder.append(ownerUid);
            stringBuilder.append(", workSource=");
            stringBuilder.append(workSource);
            Slog.d(str, stringBuilder.toString());
        }
        if (workSource != null) {
            try {
                this.mBatteryStats.noteLongPartialWakelockFinishFromSource(tag, historyTag, workSource);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        this.mBatteryStats.noteLongPartialWakelockFinish(tag, historyTag, ownerUid);
    }

    public void onWakeLockChanging(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource, String historyTag, int newFlags, String newTag, String newPackageName, int newOwnerUid, int newOwnerPid, WorkSource newWorkSource, String newHistoryTag) {
        int i = newFlags;
        int i2 = newOwnerUid;
        WorkSource workSource2 = newWorkSource;
        int monitorType = getBatteryStatsWakeLockMonitorType(flags);
        int newMonitorType = getBatteryStatsWakeLockMonitorType(i);
        String str;
        String str2;
        int i3;
        if (workSource == null || workSource2 == null || monitorType < 0 || newMonitorType < 0) {
            str = newTag;
            str2 = newPackageName;
            i3 = newOwnerPid;
            onWakeLockReleased(flags, tag, packageName, ownerUid, ownerPid, workSource, historyTag);
            onWakeLockAcquired(i, str, str2, i2, newOwnerPid, newWorkSource, newHistoryTag);
            return;
        }
        Utils.noteWakelock(flags, tag, ownerUid, ownerPid, workSource, workSource2);
        if (DEBUG) {
            String str3 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onWakeLockChanging: flags=");
            stringBuilder.append(i);
            stringBuilder.append(", tag=\"");
            str = newTag;
            stringBuilder.append(str);
            stringBuilder.append("\", packageName=");
            str2 = newPackageName;
            stringBuilder.append(str2);
            stringBuilder.append(", ownerUid=");
            stringBuilder.append(i2);
            stringBuilder.append(", ownerPid=");
            i3 = newOwnerPid;
            stringBuilder.append(i3);
            stringBuilder.append(", workSource=");
            stringBuilder.append(workSource2);
            Slog.d(str3, stringBuilder.toString());
        } else {
            str = newTag;
            str2 = newPackageName;
            i3 = newOwnerPid;
        }
        boolean z = i2 == 1000 && (1073741824 & i) != 0;
        try {
            this.mBatteryStats.noteChangeWakelockFromSource(workSource, ownerPid, tag, historyTag, monitorType, workSource2, i3, str, newHistoryTag, newMonitorType, z);
            if (monitorType == 0) {
                if (this.mPGManagerInternal == null) {
                    this.mPGManagerInternal = (PGManagerInternal) LocalServices.getService(PGManagerInternal.class);
                }
                if (this.mPGManagerInternal != null) {
                    this.mPGManagerInternal.noteChangeWakeLock(tag, workSource, packageName, ownerUid, str, workSource2, str2, i2);
                }
            }
        } catch (RemoteException e) {
        }
    }

    public void onWakeLockReleased(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource, String historyTag) {
        String str;
        String str2 = packageName;
        int i = ownerUid;
        WorkSource workSource2 = workSource;
        Utils.noteWakelock(flags, tag, i, ownerPid, workSource2, 161);
        int monitorType = getBatteryStatsWakeLockMonitorType(flags);
        if (monitorType >= 0) {
            if (workSource2 != null) {
                try {
                    this.mBatteryStats.noteStopWakelockFromSource(workSource2, ownerPid, tag, historyTag, monitorType);
                } catch (RemoteException e) {
                    str = tag;
                    return;
                }
            }
            this.mBatteryStats.noteStopWakelock(i, ownerPid, tag, historyTag, monitorType);
            this.mAppOps.finishOp(40, i, str2);
            if (monitorType == 0) {
                if (this.mPGManagerInternal == null) {
                    this.mPGManagerInternal = (PGManagerInternal) LocalServices.getService(PGManagerInternal.class);
                }
                if (this.mPGManagerInternal != null) {
                    try {
                        this.mPGManagerInternal.noteStopWakeLock(tag, workSource2, str2, i);
                        return;
                    } catch (RemoteException e2) {
                        return;
                    }
                }
            }
            str = tag;
            return;
        }
        str = tag;
    }

    private int getBatteryStatsWakeLockMonitorType(int flags) {
        int i = NetworkConstants.ARP_HWTYPE_RESERVED_HI & flags;
        if (i == 1) {
            return 0;
        }
        if (i == 6 || i == 10) {
            return 1;
        }
        if (i == 32) {
            return this.mSuspendWhenScreenOffDueToProximityConfig ? -1 : 0;
        } else {
            if (i == 64 || i != 128) {
                return -1;
            }
            return 18;
        }
    }

    public void onWakefulnessChangeStarted(final int wakefulness, int reason) {
        boolean interactive = PowerManagerInternal.isInteractive(wakefulness);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UL_Power onWakefulnessChangeStarted: wakefulness=");
        stringBuilder.append(wakefulness);
        stringBuilder.append(", reason=");
        stringBuilder.append(reason);
        stringBuilder.append(", interactive=");
        stringBuilder.append(interactive);
        stringBuilder.append(", mInteractive=");
        stringBuilder.append(this.mInteractive);
        stringBuilder.append(", mInteractiveChanging=");
        stringBuilder.append(this.mInteractiveChanging);
        Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
        this.mHandler.post(new Runnable() {
            public void run() {
                Notifier.this.mActivityManagerInternal.onWakefulnessChanged(wakefulness);
            }
        });
        if (this.mInteractive != interactive) {
            if (this.mInteractiveChanging) {
                handleLateInteractiveChange();
            }
            this.mInputManagerInternal.setInteractive(interactive);
            this.mInputMethodManagerInternal.setInteractive(interactive);
            try {
                this.mBatteryStats.noteInteractive(interactive);
            } catch (RemoteException e) {
            }
            this.mInteractive = interactive;
            this.mInteractiveChangeReason = reason;
            this.mInteractiveChanging = true;
            handleEarlyInteractiveChange();
        }
    }

    public void onWakefulnessChangeFinished() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UL_Power onWakefulnessChangeFinished mInteractiveChanging=");
        stringBuilder.append(this.mInteractiveChanging);
        stringBuilder.append(", mInteractive=");
        stringBuilder.append(this.mInteractive);
        Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
        if (this.mInteractiveChanging) {
            this.mInteractiveChanging = false;
            handleLateInteractiveChange();
            if (Jlog.isPerfTest()) {
                Jlog.i(2207, "JL_PWRSCRON_NOTIFIER_WAKEFINISH");
            }
            if (this.mInteractive) {
                Jlog.d(8, "JL_PMS_WAKEUP_FINISHED");
            }
        }
    }

    private void handleEarlyInteractiveChange() {
        synchronized (this.mLock) {
            if (this.mInteractive) {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        Notifier.this.mPolicy.startedWakingUp();
                    }
                });
                this.mPendingInteractiveState = 1;
                this.mPendingWakeUpBroadcast = true;
                updatePendingBroadcastLocked();
            } else {
                final int why = translateOffReason(this.mInteractiveChangeReason);
                this.mHandler.post(new Runnable() {
                    public void run() {
                        Notifier.this.mPolicy.startedGoingToSleep(why);
                    }
                });
            }
        }
    }

    private void handleLateInteractiveChange() {
        synchronized (this.mLock) {
            if (this.mInteractive) {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        Notifier.this.mPolicy.finishedWakingUp();
                    }
                });
            } else {
                if (this.mUserActivityPending) {
                    this.mUserActivityPending = false;
                    this.mHandler.removeMessages(1);
                }
                final int why = translateOffReason(this.mInteractiveChangeReason);
                this.mHandler.post(new Runnable() {
                    public void run() {
                        LogMaker log = new LogMaker(198);
                        log.setType(2);
                        log.setSubtype(why);
                        MetricsLogger.action(log);
                        EventLogTags.writePowerScreenState(0, why, 0, 0, 0);
                        Notifier.this.mPolicy.finishedGoingToSleep(why);
                    }
                });
                this.mPendingInteractiveState = 2;
                this.mPendingGoToSleepBroadcast = true;
                updatePendingBroadcastLocked();
            }
        }
    }

    private static int translateOffReason(int reason) {
        if (reason == 100) {
            return 6;
        }
        if (reason == 102) {
            return 7;
        }
        switch (reason) {
            case 1:
                return 1;
            case 2:
                return 3;
            default:
                return 2;
        }
    }

    public void onScreenBrightnessBoostChanged() {
        if (DEBUG) {
            Slog.d(TAG, "onScreenBrightnessBoostChanged");
        }
        this.mSuspendBlocker.acquire();
        Message msg = this.mHandler.obtainMessage(4);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    public void onUserActivity(int event, int uid) {
        try {
            this.mBatteryStats.noteUserActivity(uid, event);
        } catch (RemoteException e) {
        }
        synchronized (this.mLock) {
            if (!this.mUserActivityPending) {
                this.mUserActivityPending = true;
                Message msg = this.mHandler.obtainMessage(1);
                msg.setAsynchronous(true);
                this.mHandler.sendMessage(msg);
            }
        }
    }

    public void onWakeUp(String reason, int reasonUid, String opPackageName, int opUid) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UL_Power onWakeUp: event=");
        stringBuilder.append(reason);
        stringBuilder.append(", reasonUid=");
        stringBuilder.append(reasonUid);
        stringBuilder.append(" opPackageName=");
        stringBuilder.append(opPackageName);
        stringBuilder.append(" opUid=");
        stringBuilder.append(opUid);
        Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
        try {
            this.mBatteryStats.noteWakeUp(reason, reasonUid);
            if (opPackageName != null) {
                this.mAppOps.noteOpNoThrow(61, opUid, opPackageName);
            }
        } catch (RemoteException e) {
        }
    }

    public void onProfileTimeout(int userId) {
        Message msg = this.mHandler.obtainMessage(5);
        msg.setAsynchronous(true);
        msg.arg1 = userId;
        this.mHandler.sendMessage(msg);
    }

    public void onWirelessChargingStarted(int batteryLevel) {
        if (DEBUG) {
            Slog.d(TAG, "onWirelessChargingStarted");
        }
    }

    public void onWiredChargingStarted() {
        if (DEBUG) {
            Slog.d(TAG, "onWiredChargingStarted");
        }
    }

    private void updatePendingBroadcastLocked() {
        if (!this.mBroadcastInProgress && this.mPendingInteractiveState != 0) {
            if (this.mPendingWakeUpBroadcast || this.mPendingGoToSleepBroadcast || this.mPendingInteractiveState != this.mBroadcastedInteractiveState) {
                this.mBroadcastInProgress = true;
                this.mSuspendBlocker.acquire();
                Message msg = this.mHandler.obtainMessage(2);
                msg.setAsynchronous(true);
                this.mHandler.sendMessage(msg);
            }
        }
    }

    private void finishPendingBroadcastLocked() {
        this.mBroadcastInProgress = false;
        this.mSuspendBlocker.release();
    }

    private void sendUserActivity() {
        synchronized (this.mLock) {
            if (this.mUserActivityPending) {
                this.mUserActivityPending = false;
                this.mPolicy.userActivity();
                return;
            }
        }
    }

    /* JADX WARNING: Missing block: B:32:0x0049, code skipped:
            android.util.EventLog.writeEvent(com.android.server.EventLogTags.POWER_SCREEN_BROADCAST_SEND, 1);
     */
    /* JADX WARNING: Missing block: B:33:0x004e, code skipped:
            if (r1 != 1) goto L_0x0054;
     */
    /* JADX WARNING: Missing block: B:34:0x0050, code skipped:
            sendWakeUpBroadcast();
     */
    /* JADX WARNING: Missing block: B:35:0x0054, code skipped:
            sendGoToSleepBroadcast();
     */
    /* JADX WARNING: Missing block: B:36:0x0057, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void sendNextBroadcast() {
        synchronized (this.mLock) {
            if (this.mBroadcastedInteractiveState == 0) {
                this.mPendingWakeUpBroadcast = false;
                this.mBroadcastedInteractiveState = 1;
            } else if (this.mBroadcastedInteractiveState == 1) {
                if (!(this.mPendingWakeUpBroadcast || this.mPendingGoToSleepBroadcast)) {
                    if (this.mPendingInteractiveState != 2) {
                        finishPendingBroadcastLocked();
                        return;
                    }
                }
                this.mPendingGoToSleepBroadcast = false;
                this.mBroadcastedInteractiveState = 2;
            } else {
                if (!(this.mPendingWakeUpBroadcast || this.mPendingGoToSleepBroadcast)) {
                    if (this.mPendingInteractiveState != 1) {
                        finishPendingBroadcastLocked();
                        return;
                    }
                }
                this.mPendingWakeUpBroadcast = false;
                this.mBroadcastedInteractiveState = 1;
            }
            this.mBroadcastStartTime = SystemClock.uptimeMillis();
            int powerState = this.mBroadcastedInteractiveState;
        }
    }

    private void sendBrightnessBoostChangedBroadcast() {
        Flog.i(NativeResponseCode.SERVICE_FOUND, "PowerManagerNotifier Sending brightness boost changed broadcast.");
        this.mContext.sendOrderedBroadcastAsUser(this.mScreenBrightnessBoostIntent, UserHandle.ALL, null, this.mScreeBrightnessBoostChangedDone, this.mHandler, 0, null, null);
    }

    private void sendWakeUpBroadcast() {
        Flog.i(NativeResponseCode.SERVICE_FOUND, "UL_Power Sending wake up broadcast.");
        if (this.mActivityManagerInternal.isSystemReady()) {
            this.mContext.sendOrderedBroadcastAsUser(this.mScreenOnIntent, UserHandle.ALL, null, this.mWakeUpBroadcastDone, this.mHandler, 0, null, null);
            return;
        }
        EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, new Object[]{Integer.valueOf(2), Integer.valueOf(1)});
        sendNextBroadcast();
    }

    private void resumeSystemBrightness() {
        String str;
        StringBuilder stringBuilder;
        ContentResolver cr = this.mContext.getContentResolver();
        int userHandle = ActivityManager.getCurrentUser();
        String mode = System.getStringForUser(cr, "hw_screen_brightness_mode_value", userHandle);
        String modeCurrent = System.getStringForUser(cr, "screen_brightness_mode", userHandle);
        if (!(mode == null || modeCurrent == null)) {
            if ((modeCurrent.equals("0") && mode.equals("1")) || (modeCurrent.equals("1") && mode.equals("0"))) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("resumeSystemBrightness modeCurrent=");
                stringBuilder.append(modeCurrent);
                stringBuilder.append(",mode=");
                stringBuilder.append(mode);
                Slog.i(str, stringBuilder.toString());
                System.putStringForUser(cr, "screen_brightness_mode", mode, userHandle);
            }
            if (mode.equals("0")) {
                str = System.getStringForUser(cr, "hw_screen_temp_brightness", userHandle);
                String lastBrightnessTmp = System.getStringForUser(cr, "screen_brightness", userHandle);
                if (!(str == null || lastBrightnessTmp == null || str.equals(lastBrightnessTmp))) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("resumeSystemBrightness lastBrightnessTmp=");
                    stringBuilder2.append(lastBrightnessTmp);
                    stringBuilder2.append(",setlastBrightness=");
                    stringBuilder2.append(str);
                    Slog.i(str2, stringBuilder2.toString());
                    System.putStringForUser(cr, "screen_brightness", str, userHandle);
                }
            }
        }
        if (modeCurrent != null && mode != null) {
            if (modeCurrent.equals("0") && mode.equals("1")) {
                System.putStringForUser(cr, "screen_auto_brightness", "0", userHandle);
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("manul2auto set mBrightnessModeChangeNoClearOffset=");
                    stringBuilder.append(this.mBrightnessModeChangeNoClearOffset);
                    Slog.d(str, stringBuilder.toString());
                }
                this.mBrightnessModeChangeNoClearOffset = true;
                return;
            }
            this.mBrightnessModeChangeNoClearOffset = false;
        }
    }

    public boolean getBrightnessModeChangeNoClearOffset() {
        return this.mBrightnessModeChangeNoClearOffset;
    }

    public void setBrightnessModeChangeNoClearOffset(boolean enable) {
        this.mBrightnessModeChangeNoClearOffset = enable;
    }

    private void sendGoToSleepBroadcast() {
        Flog.i(NativeResponseCode.SERVICE_LOST, "UL_Power Sending go to sleep broadcast.");
        if (this.mActivityManagerInternal.isSystemReady()) {
            this.mContext.sendOrderedBroadcastAsUser(this.mScreenOffIntent, UserHandle.ALL, null, this.mGoToSleepBroadcastDone, this.mHandler, 0, null, null);
            if (mFactoryModeEnable) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("no resumeSystemBrightness,mFactoryModeEnable=");
                stringBuilder.append(mFactoryModeEnable);
                Slog.i(str, stringBuilder.toString());
                return;
            }
            Message msg = this.mHandler.obtainMessage(100);
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
        } else {
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, new Object[]{Integer.valueOf(3), Integer.valueOf(1)});
            sendNextBroadcast();
        }
    }

    private void playChargingStartedSound() {
        String soundPath = Global.getString(this.mContext.getContentResolver(), "wireless_charging_started_sound");
        if (isChargingFeedbackEnabled() && soundPath != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("file://");
            stringBuilder.append(soundPath);
            Uri soundUri = Uri.parse(stringBuilder.toString());
            if (soundUri != null) {
                Ringtone sfx = RingtoneManager.getRingtone(this.mContext, soundUri);
                if (sfx != null) {
                    sfx.setStreamType(1);
                    sfx.play();
                }
            }
        }
    }

    private void showWirelessChargingStarted(int batteryLevel) {
        playWirelessChargingVibration();
        playChargingStartedSound();
        if (this.mStatusBarManagerInternal != null) {
            this.mStatusBarManagerInternal.showChargingAnimation(batteryLevel);
        }
        this.mSuspendBlocker.release();
    }

    private void showWiredChargingStarted() {
        this.mSuspendBlocker.release();
    }

    private void lockProfile(int userId) {
        this.mTrustManager.setDeviceLockedForUser(userId, true);
    }

    private void playWirelessChargingVibration() {
        boolean z = false;
        if (Global.getInt(this.mContext.getContentResolver(), "charging_vibration_enabled", 0) != 0) {
            z = true;
        }
        if (z && isChargingFeedbackEnabled()) {
            this.mVibrator.vibrate(WIRELESS_CHARGING_VIBRATION_EFFECT, VIBRATION_ATTRIBUTES);
        }
    }

    private boolean isChargingFeedbackEnabled() {
        boolean enabled = Global.getInt(this.mContext.getContentResolver(), "charging_sounds_enabled", 1) != 0;
        boolean dndOff = Global.getInt(this.mContext.getContentResolver(), "zen_mode", 1) == 0;
        if (enabled && dndOff) {
            return true;
        }
        return false;
    }
}
