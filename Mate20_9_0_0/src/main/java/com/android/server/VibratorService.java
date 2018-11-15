package com.android.server;

import android.app.AppOpsManager;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.hardware.input.InputManager.InputDeviceListener;
import android.icu.text.DateFormat;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IVibratorService.Stub;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.PowerManagerInternal;
import android.os.PowerManagerInternal.LowPowerModeListener;
import android.os.PowerSaveState;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.Trace;
import android.os.VibrationEffect;
import android.os.VibrationEffect.OneShot;
import android.os.VibrationEffect.Prebaked;
import android.os.VibrationEffect.Waveform;
import android.os.Vibrator;
import android.os.WorkSource;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.util.DebugUtils;
import android.util.Flog;
import android.util.Slog;
import android.util.SparseArray;
import android.view.InputDevice;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.DumpUtils;
import com.android.server.job.controllers.JobStatus;
import com.huawei.android.os.IHwVibrator;
import huawei.android.security.IHwBehaviorCollectManager.BehaviorId;
import huawei.cust.HwCustUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

public class VibratorService extends Stub implements InputDeviceListener {
    private static final boolean DEBUG = false;
    private static final int DEFAULT_CODE = -2;
    private static final long DEFAULT_VIBRATE_TIME = 60;
    private static final long[] DEFAULT_VIBRATE_WAVEFORM = new long[]{DEFAULT_VIBRATE_TIME};
    private static final long[] DOUBLE_CLICK_EFFECT_FALLBACK_TIMINGS = new long[]{0, 30, 100, 30};
    private static final int EXCEPTION_CODE = -1;
    private static final int HWVibrator_SUPPORT = 0;
    private static final long MAX_HAPTIC_FEEDBACK_DURATION = 5000;
    private static final int SCALE_HIGH = 1;
    private static final float SCALE_HIGH_GAMMA = 0.5f;
    private static final int SCALE_LOW = -1;
    private static final float SCALE_LOW_GAMMA = 1.5f;
    private static final int SCALE_LOW_MAX_AMPLITUDE = 192;
    private static final int SCALE_NONE = 0;
    private static final float SCALE_NONE_GAMMA = 1.0f;
    private static final int SCALE_VERY_HIGH = 2;
    private static final float SCALE_VERY_HIGH_GAMMA = 0.25f;
    private static final int SCALE_VERY_LOW = -2;
    private static final float SCALE_VERY_LOW_GAMMA = 2.0f;
    private static final int SCALE_VERY_LOW_MAX_AMPLITUDE = 168;
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String TAG = "VibratorService";
    private final boolean mAllowPriorityVibrationsInLowPowerMode;
    private final AppOpsManager mAppOps;
    private final IBatteryStats mBatteryStatsService;
    private final Context mContext;
    private int mCurVibUid = -1;
    @GuardedBy("mLock")
    private Vibration mCurrentVibration;
    private HwCustCbsUtils mCust;
    private final int mDefaultVibrationAmplitude;
    private final SparseArray<VibrationEffect> mFallbackEffects;
    private final Handler mH = new Handler();
    private int mHapticFeedbackIntensity;
    HwInnerVibratorService mHwInnerService = new HwInnerVibratorService(this);
    private InputManager mIm;
    private boolean mInputDeviceListenerRegistered;
    private final ArrayList<Vibrator> mInputDeviceVibrators = new ArrayList();
    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                synchronized (VibratorService.this.mLock) {
                    if (VibratorService.this.mCurrentVibration != null && (!(VibratorService.this.mCurrentVibration.isHapticFeedback() && VibratorService.this.mCurrentVibration.isFromSystem()) && (VibratorService.this.mCust == null || !VibratorService.this.mCust.isNotAllowPkg(VibratorService.this.mCurrentVibration.opPkg)))) {
                        VibratorService.this.doCancelVibrateLocked();
                    }
                }
            }
        }
    };
    private final Object mLock = new Object();
    private boolean mLowPowerMode;
    private int mNotificationIntensity;
    private PowerManagerInternal mPowerManagerInternal;
    private final LinkedList<VibrationInfo> mPreviousVibrations;
    private final int mPreviousVibrationsLimit;
    private final SparseArray<ScaleLevel> mScaleLevels;
    private SettingsObserver mSettingObserver;
    private final boolean mSupportsAmplitudeControl;
    private volatile VibrateThread mThread;
    private final WorkSource mTmpWorkSource = new WorkSource();
    private boolean mVibrateInputDevicesSetting;
    private final Runnable mVibrationEndRunnable = new Runnable() {
        public void run() {
            VibratorService.this.onVibrationFinished();
        }
    };
    private Vibrator mVibrator;
    private final WakeLock mWakeLock;

    public class HwInnerVibratorService extends IHwVibrator.Stub {
        private static final String TAG = "HwInnerVibratorService";
        VibratorService service;

        HwInnerVibratorService(VibratorService vs) {
            this.service = vs;
        }

        public boolean isSupportHwVibrator(String type) {
            VibratorService vibratorService = this.service;
            int isSupport = VibratorService.checkHwVibrator(type);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isSupportHwVibrator type:");
            stringBuilder.append(type);
            stringBuilder.append(", isSupport:");
            stringBuilder.append(isSupport);
            Slog.i(str, stringBuilder.toString());
            if (isSupport == 0) {
                return true;
            }
            return false;
        }

        public void setHwVibrator(int uid, String opPkg, IBinder token, String type) {
            Throwable th;
            int i = uid;
            String str = type;
            Trace.traceBegin(8388608, "setHwVibrator");
            String str2;
            if (str == null) {
                try {
                    Slog.i(TAG, "setHwVibrator type is null");
                    Trace.traceEnd(8388608);
                    return;
                } catch (Throwable th2) {
                    th = th2;
                    str2 = opPkg;
                    Trace.traceEnd(8388608);
                    throw th;
                }
            }
            if (!str.startsWith("haptic.control.")) {
                VibratorService.this.mContext.enforceCallingOrSelfPermission("android.permission.VIBRATE", "setHwVibrator");
            }
            VibratorService.this.verifyIncomingUid(i);
            int pid = Binder.getCallingPid();
            String str3 = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setHwVibrator on: uid is ");
            stringBuilder.append(i);
            stringBuilder.append(",pid is ");
            stringBuilder.append(pid);
            stringBuilder.append(",packagename = ");
            str2 = opPkg;
            try {
                stringBuilder.append(str2);
                stringBuilder.append(",type is ");
                stringBuilder.append(str);
                Slog.i(str3, stringBuilder.toString());
                Vibration vib = new Vibration(VibratorService.this, token, VibrationEffect.createWaveform(VibratorService.DEFAULT_VIBRATE_WAVEFORM, -1), 0, i, str2, null);
                vib.setType(str);
                VibratorService.this.linkVibration(vib);
                long ident = Binder.clearCallingIdentity();
                try {
                    synchronized (VibratorService.this.mLock) {
                        VibratorService.this.doCancelVibrateLocked();
                        VibratorService.this.mCurrentVibration = vib;
                        VibratorService.this.startVibrationLocked(vib);
                        VibratorService.this.addToPreviousVibrationsLocked(vib);
                    }
                    Binder.restoreCallingIdentity(ident);
                    Trace.traceEnd(8388608);
                } catch (Throwable th3) {
                    Binder.restoreCallingIdentity(ident);
                }
            } catch (Throwable th4) {
                th = th4;
                Trace.traceEnd(8388608);
                throw th;
            }
        }

        public void stopHwVibrator(int uid, String opPkg, IBinder token, String type) {
            if (type == null) {
                Slog.i(TAG, "stopHwVibrator type is null");
                return;
            }
            if (!type.startsWith("haptic.control.")) {
                VibratorService.this.mContext.enforceCallingOrSelfPermission("android.permission.VIBRATE", "stopHwVibrator");
            }
            if (type == null) {
                Slog.i(TAG, "stopHwVibrator type is null");
                return;
            }
            synchronized (VibratorService.this.mLock) {
                if (VibratorService.this.mCurrentVibration != null && VibratorService.this.mCurrentVibration.token == token && VibratorService.this.mCurrentVibration.getType() != null && VibratorService.this.mCurrentVibration.getType().equals(type)) {
                    int pid = Binder.getCallingPid();
                    long ident = Binder.clearCallingIdentity();
                    try {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("stopHwVibrator on: uid is ");
                        stringBuilder.append(uid);
                        stringBuilder.append(",pid is ");
                        stringBuilder.append(pid);
                        stringBuilder.append(",packagename = ");
                        stringBuilder.append(opPkg);
                        stringBuilder.append(",type is ");
                        stringBuilder.append(type);
                        Slog.i(str, stringBuilder.toString());
                        VibratorService.this.doCancelVibrateLocked();
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }
        }

        public void setHwParameter(String command) {
            VibratorService vibratorService = this.service;
            VibratorService.setParameter(command);
        }

        public String getHwParameter(String command) {
            VibratorService vibratorService = this.service;
            return VibratorService.getParameter(command);
        }
    }

    private static final class ScaleLevel {
        public final float gamma;
        public final int maxAmplitude;

        public ScaleLevel(float gamma) {
            this(gamma, 255);
        }

        public ScaleLevel(float gamma, int maxAmplitude) {
            this.gamma = gamma;
            this.maxAmplitude = maxAmplitude;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ScaleLevel{gamma=");
            stringBuilder.append(this.gamma);
            stringBuilder.append(", maxAmplitude=");
            stringBuilder.append(this.maxAmplitude);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean SelfChange) {
            VibratorService.this.updateVibrators();
        }
    }

    private class VibrateThread extends Thread {
        private boolean mForceStop;
        private final int mUid;
        private final int mUsageHint;
        private final Waveform mWaveform;

        VibrateThread(Waveform waveform, int uid, int usageHint) {
            this.mWaveform = waveform;
            this.mUid = uid;
            this.mUsageHint = usageHint;
            VibratorService.this.mTmpWorkSource.set(uid);
            VibratorService.this.mWakeLock.setWorkSource(VibratorService.this.mTmpWorkSource);
        }

        private long delayLocked(long duration) {
            Trace.traceBegin(8388608, "delayLocked");
            long durationRemaining = duration;
            if (duration > 0) {
                try {
                    long bedtime = SystemClock.uptimeMillis() + duration;
                    do {
                        try {
                            wait(durationRemaining);
                        } catch (InterruptedException e) {
                        }
                        if (this.mForceStop) {
                            break;
                        }
                        durationRemaining = bedtime - SystemClock.uptimeMillis();
                    } while (durationRemaining > 0);
                    long j = duration - durationRemaining;
                    Trace.traceEnd(8388608);
                    return j;
                } catch (Throwable th) {
                    Trace.traceEnd(8388608);
                }
            } else {
                Trace.traceEnd(8388608);
                return 0;
            }
        }

        public void run() {
            Process.setThreadPriority(-8);
            VibratorService.this.mWakeLock.acquire();
            try {
                if (playWaveform()) {
                    VibratorService.this.onVibrationFinished();
                }
                VibratorService.this.mWakeLock.release();
            } catch (Throwable th) {
                VibratorService.this.mWakeLock.release();
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:20:0x0064 A:{Catch:{ all -> 0x007c }} */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean playWaveform() {
            Trace.traceBegin(8388608, "playWaveform");
            try {
                boolean z;
                synchronized (this) {
                    long[] timings = this.mWaveform.getTimings();
                    int[] amplitudes = this.mWaveform.getAmplitudes();
                    int len = timings.length;
                    int repeat = this.mWaveform.getRepeatIndex();
                    int index = 0;
                    long j = 0;
                    long onDuration = 0;
                    while (!this.mForceStop) {
                        if (index < len) {
                            int amplitude = amplitudes[index];
                            int index2 = index + 1;
                            long duration = timings[index];
                            if (duration <= j) {
                                index = index2;
                            } else {
                                long onDuration2;
                                if (amplitude == 0) {
                                    j = duration;
                                } else if (onDuration <= j) {
                                    j = duration;
                                    onDuration2 = getTotalOnDuration(timings, amplitudes, index2 - 1, repeat);
                                    VibratorService.this.doVibratorOn(onDuration2, amplitude, this.mUid, this.mUsageHint);
                                    onDuration = delayLocked(j);
                                    if (amplitude != 0) {
                                        onDuration2 -= onDuration;
                                    }
                                    onDuration = onDuration2;
                                    index = index2;
                                } else {
                                    j = duration;
                                    VibratorService.this.doVibratorSetAmplitude(amplitude);
                                }
                                onDuration2 = onDuration;
                                onDuration = delayLocked(j);
                                if (amplitude != 0) {
                                }
                                onDuration = onDuration2;
                                index = index2;
                            }
                        } else if (repeat < 0) {
                            break;
                        } else {
                            index = repeat;
                        }
                        j = 0;
                    }
                    z = this.mForceStop ^ 1;
                }
                Trace.traceEnd(8388608);
                return z;
            } catch (Throwable th) {
                Trace.traceEnd(8388608);
            }
        }

        public void cancel() {
            synchronized (this) {
                VibratorService.this.mThread.mForceStop = true;
                VibratorService.this.mThread.notify();
            }
        }

        private long getTotalOnDuration(long[] timings, int[] amplitudes, int startIndex, int repeatIndex) {
            int i = startIndex;
            long timing = 0;
            while (amplitudes[i] != 0) {
                int i2 = i + 1;
                timing += timings[i];
                if (i2 >= timings.length) {
                    if (repeatIndex < 0) {
                        break;
                    }
                    i = repeatIndex;
                    continue;
                } else {
                    i = i2;
                    continue;
                }
                if (i == startIndex) {
                    return 1000;
                }
            }
            return timing;
        }
    }

    private class Vibration implements DeathRecipient {
        public VibrationEffect effect;
        private int mode;
        public final String opPkg;
        public VibrationEffect originalEffect;
        public final long startTime;
        public final long startTimeDebug;
        public final IBinder token;
        private String type;
        public final int uid;
        public final int usageHint;

        /* synthetic */ Vibration(VibratorService x0, IBinder x1, VibrationEffect x2, int x3, int x4, String x5, AnonymousClass1 x6) {
            this(x1, x2, x3, x4, x5);
        }

        public void setMode(int m) {
            this.mode = m;
        }

        public int getMode() {
            return this.mode;
        }

        public void setType(String t) {
            this.type = t;
        }

        public String getType() {
            return this.type;
        }

        private Vibration(IBinder token, VibrationEffect effect, int usageHint, int uid, String opPkg) {
            this.mode = 0;
            this.type = null;
            this.token = token;
            this.effect = effect;
            this.startTime = SystemClock.elapsedRealtime();
            this.startTimeDebug = System.currentTimeMillis();
            this.usageHint = usageHint;
            this.uid = uid;
            this.opPkg = opPkg;
        }

        public void binderDied() {
            synchronized (VibratorService.this.mLock) {
                if (this == VibratorService.this.mCurrentVibration) {
                    VibratorService.this.doCancelVibrateLocked();
                }
            }
        }

        public boolean hasTimeoutLongerThan(long millis) {
            long duration = this.effect.getDuration();
            return duration >= 0 && duration > millis;
        }

        public boolean isHapticFeedback() {
            boolean z = true;
            if (this.effect instanceof Prebaked) {
                switch (this.effect.getId()) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                        return true;
                    default:
                        Slog.w(VibratorService.TAG, "Unknown prebaked vibration effect, assuming it isn't haptic feedback.");
                        return false;
                }
            }
            long duration = this.effect.getDuration();
            if (duration < 0 || duration >= VibratorService.MAX_HAPTIC_FEEDBACK_DURATION) {
                z = false;
            }
            return z;
        }

        public boolean isNotification() {
            int i = this.usageHint;
            if (i != 5) {
                switch (i) {
                    case 7:
                    case 8:
                    case 9:
                        break;
                    default:
                        return false;
                }
            }
            return true;
        }

        public boolean isRingtone() {
            return this.usageHint == 6;
        }

        public boolean isFromSystem() {
            return this.uid == 1000 || this.uid == 0 || VibratorService.SYSTEM_UI_PACKAGE.equals(this.opPkg);
        }

        public VibrationInfo toInfo() {
            return new VibrationInfo(this.startTimeDebug, this.effect, this.originalEffect, this.usageHint, this.uid, this.opPkg, this.type);
        }
    }

    private static class VibrationInfo {
        private final VibrationEffect mEffect;
        private final String mOpPkg;
        private final VibrationEffect mOriginalEffect;
        private final long mStartTimeDebug;
        private final String mType;
        private final int mUid;
        private final int mUsageHint;

        public VibrationInfo(long startTimeDebug, VibrationEffect effect, VibrationEffect originalEffect, int usageHint, int uid, String opPkg, String type) {
            this.mStartTimeDebug = startTimeDebug;
            this.mEffect = effect;
            this.mOriginalEffect = originalEffect;
            this.mUsageHint = usageHint;
            this.mUid = uid;
            this.mOpPkg = opPkg;
            this.mType = type;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startTime: ");
            stringBuilder.append(DateFormat.getDateTimeInstance().format(new Date(this.mStartTimeDebug)));
            stringBuilder.append(", effect: ");
            stringBuilder.append(this.mEffect);
            stringBuilder.append(", originalEffect: ");
            stringBuilder.append(this.mOriginalEffect);
            stringBuilder.append(", usageHint: ");
            stringBuilder.append(this.mUsageHint);
            stringBuilder.append(", uid: ");
            stringBuilder.append(this.mUid);
            stringBuilder.append(", opPkg: ");
            stringBuilder.append(this.mOpPkg);
            stringBuilder.append(", type: ");
            stringBuilder.append(this.mType);
            return stringBuilder.toString();
        }
    }

    private final class VibratorShellCommand extends ShellCommand {
        private static final long MAX_VIBRATION_MS = 200;
        private final IBinder mToken;

        /* synthetic */ VibratorShellCommand(VibratorService x0, IBinder x1, AnonymousClass1 x2) {
            this(x1);
        }

        private VibratorShellCommand(IBinder token) {
            this.mToken = token;
        }

        public int onCommand(String cmd) {
            if ("vibrate".equals(cmd)) {
                return runVibrate();
            }
            return handleDefaultCommands(cmd);
        }

        private int runVibrate() {
            Trace.traceBegin(8388608, "runVibrate");
            PrintWriter pw;
            try {
                int zenMode = Global.getInt(VibratorService.this.mContext.getContentResolver(), "zen_mode");
                if (zenMode != 0) {
                    pw = getOutPrintWriter();
                    pw.print("Ignoring because device is on DND mode ");
                    pw.println(DebugUtils.flagsToString(Global.class, "ZEN_MODE_", zenMode));
                    if (pw != null) {
                        $closeResource(null, pw);
                    }
                    Trace.traceEnd(8388608);
                    return 0;
                }
            } catch (SettingNotFoundException e) {
            } catch (Throwable th) {
                if (pw != null) {
                    $closeResource(r5, pw);
                }
            }
            try {
                long duration = Long.parseLong(getNextArgRequired());
                if (duration <= MAX_VIBRATION_MS) {
                    String description = getNextArg();
                    if (description == null) {
                        description = "Shell command";
                    }
                    String description2 = description;
                    String str = description2;
                    VibratorService.this.vibrate(Binder.getCallingUid(), str, VibrationEffect.createOneShot(duration, -1), 0, this.mToken);
                    return 0;
                }
                throw new IllegalArgumentException("maximum duration is 200");
            } finally {
                Trace.traceEnd(8388608);
            }
        }

        private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
            if (x0 != null) {
                try {
                    x1.close();
                    return;
                } catch (Throwable th) {
                    x0.addSuppressed(th);
                    return;
                }
            }
            x1.close();
        }

        /* JADX WARNING: Missing block: B:9:0x0037, code:
            if (r0 != null) goto L_0x0039;
     */
        /* JADX WARNING: Missing block: B:10:0x0039, code:
            $closeResource(r1, r0);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Vibrator commands:");
            pw.println("  help");
            pw.println("    Prints this help text.");
            pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            pw.println("  vibrate duration [description]");
            pw.println("    Vibrates for duration milliseconds; ignored when device is on DND ");
            pw.println("    (Do Not Disturb) mode.");
            pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            if (pw != null) {
                $closeResource(null, pw);
            }
        }
    }

    static native int checkHwVibrator(String str);

    static native String getParameter(String str);

    static native int setHwVibrator(String str);

    static native int setParameter(String str);

    static native int stopHwVibrator(String str);

    static native boolean vibratorExists();

    static native void vibratorInit();

    static native void vibratorOff();

    static native void vibratorOn(long j);

    static native long vibratorPerformEffect(long j, long j2);

    static native void vibratorSetAmplitude(int i);

    static native boolean vibratorSupportsAmplitudeControl();

    static native int vibratorWrite(int i);

    VibratorService(Context context) {
        this.mCust = (HwCustCbsUtils) HwCustUtils.createObj(HwCustCbsUtils.class, new Object[]{context});
        vibratorInit();
        vibratorOff();
        this.mSupportsAmplitudeControl = vibratorSupportsAmplitudeControl();
        this.mContext = context;
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "*vibrator*");
        this.mWakeLock.setReferenceCounted(true);
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mBatteryStatsService = IBatteryStats.Stub.asInterface(ServiceManager.getService("batterystats"));
        this.mPreviousVibrationsLimit = this.mContext.getResources().getInteger(17694850);
        this.mDefaultVibrationAmplitude = this.mContext.getResources().getInteger(17694774);
        this.mAllowPriorityVibrationsInLowPowerMode = this.mContext.getResources().getBoolean(17956875);
        this.mPreviousVibrations = new LinkedList();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        context.registerReceiver(this.mIntentReceiver, filter);
        VibrationEffect clickEffect = createEffectFromResource(17236051);
        VibrationEffect doubleClickEffect = VibrationEffect.createWaveform(DOUBLE_CLICK_EFFECT_FALLBACK_TIMINGS, -1);
        VibrationEffect heavyClickEffect = createEffectFromResource(17236015);
        VibrationEffect tickEffect = createEffectFromResource(17235996);
        this.mFallbackEffects = new SparseArray();
        this.mFallbackEffects.put(0, clickEffect);
        this.mFallbackEffects.put(1, doubleClickEffect);
        this.mFallbackEffects.put(2, tickEffect);
        this.mFallbackEffects.put(5, heavyClickEffect);
        this.mScaleLevels = new SparseArray();
        this.mScaleLevels.put(-2, new ScaleLevel(SCALE_VERY_LOW_GAMMA, SCALE_VERY_LOW_MAX_AMPLITUDE));
        this.mScaleLevels.put(-1, new ScaleLevel(SCALE_LOW_GAMMA, SCALE_LOW_MAX_AMPLITUDE));
        this.mScaleLevels.put(0, new ScaleLevel(1.0f));
        this.mScaleLevels.put(1, new ScaleLevel(0.5f));
        this.mScaleLevels.put(2, new ScaleLevel(SCALE_VERY_HIGH_GAMMA));
    }

    private VibrationEffect createEffectFromResource(int resId) {
        return createEffectFromTimings(getLongIntArray(this.mContext.getResources(), resId));
    }

    private static VibrationEffect createEffectFromTimings(long[] timings) {
        if (timings == null || timings.length == 0) {
            return null;
        }
        if (timings.length == 1) {
            return VibrationEffect.createOneShot(timings[0], -1);
        }
        return VibrationEffect.createWaveform(timings, -1);
    }

    public void systemReady() {
        Trace.traceBegin(8388608, "VibratorService#systemReady");
        try {
            this.mIm = (InputManager) this.mContext.getSystemService(InputManager.class);
            this.mVibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
            this.mSettingObserver = new SettingsObserver(this.mH);
            this.mPowerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
            this.mPowerManagerInternal.registerLowPowerModeObserver(new LowPowerModeListener() {
                public int getServiceType() {
                    return 2;
                }

                public void onLowPowerModeChanged(PowerSaveState result) {
                    VibratorService.this.updateVibrators();
                }
            });
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor("vibrate_input_devices"), true, this.mSettingObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor("haptic_feedback_intensity"), true, this.mSettingObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor("notification_vibration_intensity"), true, this.mSettingObserver, -1);
            this.mContext.registerReceiver(new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    VibratorService.this.updateVibrators();
                }
            }, new IntentFilter("android.intent.action.USER_SWITCHED"), null, this.mH);
            updateVibrators();
        } finally {
            Trace.traceEnd(8388608);
        }
    }

    public boolean hasVibrator() {
        return doVibratorExists();
    }

    public boolean hasAmplitudeControl() {
        boolean z;
        synchronized (this.mInputDeviceVibrators) {
            z = this.mSupportsAmplitudeControl && this.mInputDeviceVibrators.isEmpty();
        }
        return z;
    }

    private void verifyIncomingUid(int uid) {
        if (uid != Binder.getCallingUid() && Binder.getCallingPid() != Process.myPid()) {
            this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        }
    }

    private static boolean verifyVibrationEffect(VibrationEffect effect) {
        if (effect == null) {
            Slog.wtf(TAG, "effect must not be null");
            return false;
        }
        try {
            effect.validate();
            return true;
        } catch (Exception e) {
            Slog.wtf(TAG, "Encountered issue when verifying VibrationEffect.", e);
            return false;
        }
    }

    public void hwVibrate(int uid, String opPkg, int usageHint, IBinder token, int mode) {
        Throwable th;
        String str = opPkg;
        Trace.traceBegin(8388608, "hwVibrate");
        try {
            if (!"com.huawei.android.launcher".equals(str) || this.mContext.checkCallingOrSelfPermission("android.permission.VIBRATE") == 0) {
                verifyIncomingUid(uid);
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hw Vibrating for  opPkg ");
                stringBuilder.append(str);
                Slog.d(str2, stringBuilder.toString());
                Vibration vib = new Vibration(this, token, VibrationEffect.createOneShot(DEFAULT_VIBRATE_TIME, -1), usageHint, uid, str, null);
                try {
                    vib.setMode(mode);
                    linkVibration(vib);
                    long ident = Binder.clearCallingIdentity();
                    try {
                        synchronized (this.mLock) {
                            doCancelVibrateLocked();
                            this.mCurrentVibration = vib;
                            if (-1 == startVibrationLocked(vib)) {
                                vib.setMode(0);
                                startVibrationLocked(vib);
                            }
                            addToPreviousVibrationsLocked(vib);
                        }
                        Binder.restoreCallingIdentity(ident);
                        Trace.traceEnd(8388608);
                        return;
                    } catch (Throwable th2) {
                        Binder.restoreCallingIdentity(ident);
                    }
                } catch (Throwable th3) {
                    th = th3;
                    Trace.traceEnd(8388608);
                    throw th;
                }
            }
            throw new SecurityException("Requires VIBRATE permission");
        } catch (Throwable th4) {
            th = th4;
            int i = mode;
            Trace.traceEnd(8388608);
            throw th;
        }
    }

    private static long[] getLongIntArray(Resources r, int resid) {
        int[] ar = r.getIntArray(resid);
        if (ar == null) {
            return null;
        }
        long[] out = new long[ar.length];
        for (int i = 0; i < ar.length; i++) {
            out[i] = (long) ar[i];
        }
        return out;
    }

    public void vibrate(int uid, String opPkg, VibrationEffect effect, int usageHint, IBinder token) {
        Throwable th;
        VibrationEffect vibrationEffect = effect;
        Trace.traceBegin(8388608, "vibrate");
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.VIBRATOR_VIBRATE);
        int i;
        String str;
        try {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.VIBRATE") != 0) {
                i = uid;
                str = opPkg;
                throw new SecurityException("Requires VIBRATE permission");
            } else if (token == null) {
                Slog.e(TAG, "token must not be null");
                Trace.traceEnd(8388608);
            } else {
                verifyIncomingUid(uid);
                if (verifyVibrationEffect(effect)) {
                    synchronized (this.mLock) {
                        if ((vibrationEffect instanceof OneShot) && this.mCurrentVibration != null && (this.mCurrentVibration.effect instanceof OneShot)) {
                            OneShot newOneShot = (OneShot) vibrationEffect;
                            OneShot currentOneShot = this.mCurrentVibration.effect;
                            if (this.mCurrentVibration.hasTimeoutLongerThan(newOneShot.getDuration()) && newOneShot.getAmplitude() == currentOneShot.getAmplitude()) {
                                Trace.traceEnd(8388608);
                                return;
                            }
                        }
                        try {
                            if (isRepeatingVibration(effect) || this.mCurrentVibration == null || !isRepeatingVibration(this.mCurrentVibration.effect)) {
                                Vibration vib = new Vibration(this, token, vibrationEffect, usageHint, uid, opPkg, null);
                                int pid = Binder.getCallingPid();
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("vibrate on: uid is ");
                                try {
                                    stringBuilder.append(uid);
                                    stringBuilder.append(",pid is ");
                                    stringBuilder.append(pid);
                                    stringBuilder.append(",packagename = ");
                                } catch (Throwable th2) {
                                    th = th2;
                                    str = opPkg;
                                    try {
                                        throw th;
                                    } catch (Throwable th3) {
                                        th = th3;
                                        Trace.traceEnd(8388608);
                                        throw th;
                                    }
                                }
                                long ident;
                                try {
                                    stringBuilder.append(opPkg);
                                    Flog.i(NativeResponseCode.SERVICE_REGISTERED, stringBuilder.toString());
                                    linkVibration(vib);
                                    ident = Binder.clearCallingIdentity();
                                    doCancelVibrateLocked();
                                    startVibrationLocked(vib);
                                    addToPreviousVibrationsLocked(vib);
                                    Binder.restoreCallingIdentity(ident);
                                    Trace.traceEnd(8388608);
                                    return;
                                } catch (Throwable th4) {
                                    th = th4;
                                    throw th;
                                }
                            }
                            Trace.traceEnd(8388608);
                            return;
                        } catch (Throwable th5) {
                            th = th5;
                            i = uid;
                            str = opPkg;
                            throw th;
                        }
                    }
                }
                Trace.traceEnd(8388608);
            }
        } catch (Throwable th6) {
            th = th6;
            i = uid;
            str = opPkg;
            Trace.traceEnd(8388608);
            throw th;
        }
    }

    private static boolean isRepeatingVibration(VibrationEffect effect) {
        return effect.getDuration() == JobStatus.NO_LATEST_RUNTIME;
    }

    private void addToPreviousVibrationsLocked(Vibration vib) {
        if (this.mPreviousVibrations.size() > this.mPreviousVibrationsLimit) {
            this.mPreviousVibrations.removeFirst();
        }
        this.mPreviousVibrations.addLast(vib.toInfo());
    }

    public void cancelVibrate(IBinder token) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendBehavior(BehaviorId.VIBRATOR_CANCELVIBRATE);
        this.mContext.enforceCallingOrSelfPermission("android.permission.VIBRATE", "cancelVibrate");
        synchronized (this.mLock) {
            if (this.mCurrentVibration != null && this.mCurrentVibration.token == token) {
                int uid = Binder.getCallingUid();
                int pid = Binder.getCallingPid();
                long ident = Binder.clearCallingIdentity();
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Canceling vibration. UID:");
                    stringBuilder.append(uid);
                    stringBuilder.append(", PID:");
                    stringBuilder.append(pid);
                    Flog.i(NativeResponseCode.SERVICE_REGISTERED, stringBuilder.toString());
                    doCancelVibrateLocked();
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
    }

    @GuardedBy("mLock")
    private void doCancelVibrateLocked() {
        Trace.asyncTraceEnd(8388608, "vibration", 0);
        Trace.traceBegin(8388608, "doCancelVibrateLocked");
        try {
            this.mH.removeCallbacks(this.mVibrationEndRunnable);
            if (this.mThread != null) {
                this.mThread.cancel();
                this.mThread = null;
            }
            if (!cancelHwVibrate()) {
                doVibratorOff();
            }
            reportFinishVibrationLocked();
        } finally {
            Trace.traceEnd(8388608);
        }
    }

    public void onVibrationFinished() {
        synchronized (this.mLock) {
            doCancelVibrateLocked();
        }
    }

    @GuardedBy("mLock")
    private int startVibrationLocked(Vibration vib) {
        Trace.traceBegin(8388608, "startVibrationLocked");
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startVibrationLocked vib.getMode:");
            stringBuilder.append(vib.getMode());
            Flog.i(NativeResponseCode.SERVICE_REGISTERED, stringBuilder.toString());
            int i = -2;
            if (!isAllowedToVibrateLocked(vib)) {
                return i;
            }
            int intensity = getCurrentIntensityLocked(vib);
            if (intensity == 0) {
                Trace.traceEnd(8388608);
                return i;
            } else if (this.mCust == null || this.mCust.allowVibrateWhenSlient(this.mContext, vib.opPkg)) {
                int mode = getAppOpMode(vib);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("startVibrationLocked mode:");
                stringBuilder2.append(mode);
                Flog.i(NativeResponseCode.SERVICE_REGISTERED, stringBuilder2.toString());
                int doVibratorHwOn;
                if (mode != 0) {
                    if (mode == 2) {
                        String str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Would be an error: vibrate from uid ");
                        stringBuilder2.append(vib.uid);
                        Slog.w(str, stringBuilder2.toString());
                    }
                    Trace.traceEnd(8388608);
                    return i;
                } else if (vib.getMode() != 0) {
                    doVibratorHwOn = doVibratorHwOn(vib.getMode());
                    Trace.traceEnd(8388608);
                    return doVibratorHwOn;
                } else if (vib.getType() != null) {
                    doVibratorHwOn = setHwVibrator(vib.getType());
                    Trace.traceEnd(8388608);
                    return doVibratorHwOn;
                } else {
                    applyVibrationIntensityScalingLocked(vib, intensity);
                    startVibrationInnerLocked(vib);
                    Trace.traceEnd(8388608);
                    return i;
                }
            } else {
                Trace.traceEnd(8388608);
                return i;
            }
        } finally {
            Trace.traceEnd(8388608);
        }
    }

    @GuardedBy("mLock")
    private void startVibrationInnerLocked(Vibration vib) {
        Trace.traceBegin(8388608, "startVibrationInnerLocked");
        try {
            this.mCurrentVibration = vib;
            if (vib.effect instanceof OneShot) {
                Trace.asyncTraceBegin(8388608, "vibration", 0);
                OneShot oneShot = vib.effect;
                doVibratorOn(oneShot.getDuration(), oneShot.getAmplitude(), vib.uid, vib.usageHint);
                this.mH.postDelayed(this.mVibrationEndRunnable, oneShot.getDuration());
            } else if (vib.effect instanceof Waveform) {
                Trace.asyncTraceBegin(8388608, "vibration", 0);
                this.mThread = new VibrateThread(vib.effect, vib.uid, vib.usageHint);
                this.mThread.start();
            } else if (vib.effect instanceof Prebaked) {
                Trace.asyncTraceBegin(8388608, "vibration", 0);
                long timeout = doVibratorPrebakedEffectLocked(vib);
                if (timeout > 0) {
                    this.mH.postDelayed(this.mVibrationEndRunnable, timeout);
                }
            } else {
                Slog.e(TAG, "Unknown vibration type, ignoring");
            }
            Trace.traceEnd(8388608);
        } catch (Throwable th) {
            Trace.traceEnd(8388608);
        }
    }

    private boolean isAllowedToVibrateLocked(Vibration vib) {
        if (!this.mLowPowerMode) {
            return true;
        }
        if ((this.mCust != null && this.mCust.isAllowLowPowerPkg(vib.opPkg)) || vib.usageHint == 6 || vib.usageHint == 4 || vib.usageHint == 11 || vib.usageHint == 7) {
            return true;
        }
        return false;
    }

    private int getCurrentIntensityLocked(Vibration vib) {
        if (vib.isNotification() || vib.isRingtone()) {
            return this.mNotificationIntensity;
        }
        if (vib.isHapticFeedback()) {
            return this.mHapticFeedbackIntensity;
        }
        return 2;
    }

    private void applyVibrationIntensityScalingLocked(Vibration vib, int intensity) {
        if (vib.effect instanceof Prebaked) {
            vib.effect.setEffectStrength(intensityToEffectStrength(intensity));
            return;
        }
        int defaultIntensity;
        if (vib.isNotification() || vib.isRingtone()) {
            defaultIntensity = this.mVibrator.getDefaultNotificationVibrationIntensity();
        } else if (vib.isHapticFeedback()) {
            defaultIntensity = this.mVibrator.getDefaultHapticFeedbackIntensity();
        } else {
            return;
        }
        ScaleLevel scale = (ScaleLevel) this.mScaleLevels.get(intensity - defaultIntensity);
        if (scale == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No configured scaling level! (current=");
            stringBuilder.append(intensity);
            stringBuilder.append(", default= ");
            stringBuilder.append(defaultIntensity);
            stringBuilder.append(")");
            Slog.e(str, stringBuilder.toString());
            return;
        }
        VibrationEffect scaledEffect = null;
        if (vib.effect instanceof OneShot) {
            scaledEffect = vib.effect.resolve(this.mDefaultVibrationAmplitude).scale(scale.gamma, scale.maxAmplitude);
        } else if (vib.effect instanceof Waveform) {
            scaledEffect = vib.effect.resolve(this.mDefaultVibrationAmplitude).scale(scale.gamma, scale.maxAmplitude);
        } else {
            Slog.w(TAG, "Unable to apply intensity scaling, unknown VibrationEffect type");
        }
        if (scaledEffect != null) {
            vib.originalEffect = vib.effect;
            vib.effect = scaledEffect;
        }
    }

    private int doVibratorHwOn(int mode) {
        int vibratorWrite;
        synchronized (this.mInputDeviceVibrators) {
            vibratorWrite = vibratorWrite(mode);
        }
        return vibratorWrite;
    }

    private int getAppOpMode(Vibration vib) {
        int mode = this.mAppOps.checkAudioOpNoThrow(3, vib.usageHint, vib.uid, vib.opPkg);
        if (mode == 0) {
            return this.mAppOps.startOpNoThrow(3, vib.uid, vib.opPkg);
        }
        return mode;
    }

    @GuardedBy("mLock")
    private void reportFinishVibrationLocked() {
        Trace.traceBegin(8388608, "reportFinishVibrationLocked");
        try {
            if (this.mCurrentVibration != null) {
                this.mAppOps.finishOp(3, this.mCurrentVibration.uid, this.mCurrentVibration.opPkg);
                unlinkVibration(this.mCurrentVibration);
                this.mCurrentVibration = null;
            }
            Trace.traceEnd(8388608);
        } catch (Throwable th) {
            Trace.traceEnd(8388608);
        }
    }

    private void linkVibration(Vibration vib) {
        if (vib.effect instanceof Waveform) {
            try {
                vib.token.linkToDeath(vib, 0);
            } catch (RemoteException e) {
            }
        }
    }

    private void unlinkVibration(Vibration vib) {
        if (vib.effect instanceof Waveform) {
            vib.token.unlinkToDeath(vib, 0);
        }
    }

    private void updateVibrators() {
        synchronized (this.mLock) {
            boolean devicesUpdated = updateInputDeviceVibratorsLocked();
            boolean lowPowerModeUpdated = updateLowPowerModeLocked();
            updateVibrationIntensityLocked();
            if (devicesUpdated || lowPowerModeUpdated) {
                doCancelVibrateLocked();
            }
        }
    }

    private boolean updateInputDeviceVibratorsLocked() {
        boolean changed = false;
        int i = 0;
        boolean vibrateInputDevices = false;
        try {
            vibrateInputDevices = System.getIntForUser(this.mContext.getContentResolver(), "vibrate_input_devices", -2) > 0;
        } catch (SettingNotFoundException e) {
        }
        if (vibrateInputDevices != this.mVibrateInputDevicesSetting) {
            changed = true;
            this.mVibrateInputDevicesSetting = vibrateInputDevices;
        }
        if (this.mVibrateInputDevicesSetting) {
            if (!this.mInputDeviceListenerRegistered) {
                this.mInputDeviceListenerRegistered = true;
                this.mIm.registerInputDeviceListener(this, this.mH);
            }
        } else if (this.mInputDeviceListenerRegistered) {
            this.mInputDeviceListenerRegistered = false;
            this.mIm.unregisterInputDeviceListener(this);
        }
        this.mInputDeviceVibrators.clear();
        if (!this.mVibrateInputDevicesSetting) {
            return changed;
        }
        int[] ids = this.mIm.getInputDeviceIds();
        while (i < ids.length) {
            InputDevice device = this.mIm.getInputDevice(ids[i]);
            if (device != null) {
                Vibrator vibrator = device.getVibrator();
                if (vibrator.hasVibrator()) {
                    this.mInputDeviceVibrators.add(vibrator);
                }
            }
            i++;
        }
        return true;
    }

    private boolean updateLowPowerModeLocked() {
        boolean lowPowerMode = this.mPowerManagerInternal.getLowPowerState(2).batterySaverEnabled;
        if (lowPowerMode == this.mLowPowerMode) {
            return false;
        }
        this.mLowPowerMode = lowPowerMode;
        return true;
    }

    private void updateVibrationIntensityLocked() {
        this.mHapticFeedbackIntensity = System.getIntForUser(this.mContext.getContentResolver(), "haptic_feedback_intensity", this.mVibrator.getDefaultHapticFeedbackIntensity(), -2);
        this.mNotificationIntensity = System.getIntForUser(this.mContext.getContentResolver(), "notification_vibration_intensity", this.mVibrator.getDefaultNotificationVibrationIntensity(), -2);
    }

    public void onInputDeviceAdded(int deviceId) {
        updateVibrators();
    }

    public void onInputDeviceChanged(int deviceId) {
        updateVibrators();
    }

    public void onInputDeviceRemoved(int deviceId) {
        updateVibrators();
    }

    private boolean doVibratorExists() {
        return vibratorExists();
    }

    private void doVibratorOn(long millis, int amplitude, int uid, int usageHint) {
        Trace.traceBegin(8388608, "doVibratorOn");
        try {
            synchronized (this.mInputDeviceVibrators) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Turning vibrator on for ");
                stringBuilder.append(millis);
                stringBuilder.append(" ms.");
                Flog.i(NativeResponseCode.SERVICE_REGISTERED, stringBuilder.toString());
                if (amplitude == -1) {
                    amplitude = this.mDefaultVibrationAmplitude;
                }
                noteVibratorOnLocked(uid, millis);
                int vibratorCount = this.mInputDeviceVibrators.size();
                if (vibratorCount != 0) {
                    AudioAttributes attributes = new Builder().setUsage(usageHint).build();
                    for (int i = 0; i < vibratorCount; i++) {
                        ((Vibrator) this.mInputDeviceVibrators.get(i)).vibrate(millis, attributes);
                    }
                } else {
                    vibratorOn(millis);
                    doVibratorSetAmplitude(amplitude);
                }
            }
            Trace.traceEnd(8388608);
        } catch (Throwable th) {
            Trace.traceEnd(8388608);
        }
    }

    private void doVibratorSetAmplitude(int amplitude) {
        if (this.mSupportsAmplitudeControl) {
            vibratorSetAmplitude(amplitude);
        }
    }

    private void doVibratorOff() {
        Trace.traceBegin(8388608, "doVibratorOff");
        try {
            synchronized (this.mInputDeviceVibrators) {
                noteVibratorOffLocked();
                int vibratorCount = this.mInputDeviceVibrators.size();
                if (vibratorCount != 0) {
                    for (int i = 0; i < vibratorCount; i++) {
                        ((Vibrator) this.mInputDeviceVibrators.get(i)).cancel();
                    }
                } else {
                    vibratorOff();
                }
            }
            Trace.traceEnd(8388608);
        } catch (Throwable th) {
            Trace.traceEnd(8388608);
        }
    }

    @GuardedBy("mLock")
    private long doVibratorPrebakedEffectLocked(Vibration vib) {
        Vibration vibration = vib;
        Trace.traceBegin(8388608, "doVibratorPrebakedEffectLocked");
        try {
            boolean usingInputDeviceVibrators;
            Prebaked prebaked = (Prebaked) vibration.effect;
            synchronized (this.mInputDeviceVibrators) {
                usingInputDeviceVibrators = this.mInputDeviceVibrators.isEmpty() ^ 1;
            }
            if (!usingInputDeviceVibrators) {
                long timeout = vibratorPerformEffect((long) prebaked.getId(), (long) prebaked.getEffectStrength());
                if (timeout > 0) {
                    noteVibratorOnLocked(vibration.uid, timeout);
                    Trace.traceEnd(8388608);
                    return timeout;
                }
            }
            if (prebaked.shouldFallback()) {
                VibrationEffect effect = getFallbackEffect(prebaked.getId());
                if (effect == null) {
                    Slog.w(TAG, "Failed to play prebaked effect, no fallback");
                    Trace.traceEnd(8388608);
                    return 0;
                }
                Vibration fallbackVib = new Vibration(this, vibration.token, effect, vibration.usageHint, vibration.uid, vibration.opPkg, null);
                int intensity = getCurrentIntensityLocked(fallbackVib);
                linkVibration(fallbackVib);
                applyVibrationIntensityScalingLocked(fallbackVib, intensity);
                startVibrationInnerLocked(fallbackVib);
                Trace.traceEnd(8388608);
                return 0;
            }
            Trace.traceEnd(8388608);
            return 0;
        } catch (Throwable th) {
            Trace.traceEnd(8388608);
        }
    }

    private VibrationEffect getFallbackEffect(int effectId) {
        return (VibrationEffect) this.mFallbackEffects.get(effectId);
    }

    private static int intensityToEffectStrength(int intensity) {
        switch (intensity) {
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return 2;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Got unexpected vibration intensity: ");
                stringBuilder.append(intensity);
                Slog.w(str, stringBuilder.toString());
                return 2;
        }
    }

    private void noteVibratorOnLocked(int uid, long millis) {
        try {
            this.mBatteryStatsService.noteVibratorOn(uid, millis);
            this.mCurVibUid = uid;
        } catch (RemoteException e) {
        }
    }

    private void noteVibratorOffLocked() {
        if (this.mCurVibUid >= 0) {
            try {
                this.mBatteryStatsService.noteVibratorOff(this.mCurVibUid);
            } catch (RemoteException e) {
            }
            this.mCurVibUid = -1;
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            pw.println("Vibrator Service:");
            synchronized (this.mLock) {
                pw.print("  mCurrentVibration=");
                if (this.mCurrentVibration != null) {
                    pw.println(this.mCurrentVibration.toInfo().toString());
                } else {
                    pw.println("null");
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("  mLowPowerMode=");
                stringBuilder.append(this.mLowPowerMode);
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("  mHapticFeedbackIntensity=");
                stringBuilder.append(this.mHapticFeedbackIntensity);
                pw.println(stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append("  mNotificationIntensity=");
                stringBuilder.append(this.mNotificationIntensity);
                pw.println(stringBuilder.toString());
                pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                pw.println("  Previous vibrations:");
                Iterator it = this.mPreviousVibrations.iterator();
                while (it.hasNext()) {
                    VibrationInfo info = (VibrationInfo) it.next();
                    pw.print("    ");
                    pw.println(info.toString());
                }
            }
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
        new VibratorShellCommand(this, this, null).exec(this, in, out, err, args, callback, resultReceiver);
    }

    public IBinder getHwInnerService() {
        return this.mHwInnerService;
    }

    private boolean cancelHwVibrate() {
        if (this.mCurrentVibration == null || this.mCurrentVibration.getType() == null) {
            return false;
        }
        stopHwVibrator(this.mCurrentVibration.getType());
        return true;
    }
}
