package com.android.server.power;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.SynchronousUserSwitchObserver;
import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.SensorManager;
import android.hardware.SystemSensorManager;
import android.hardware.display.DisplayManagerInternal;
import android.hardware.display.DisplayManagerInternal.DisplayPowerCallbacks;
import android.hardware.display.DisplayManagerInternal.DisplayPowerRequest;
import android.hardware.input.InputManagerInternal;
import android.iawareperf.UniPerf;
import android.metrics.LogMaker;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.BatteryManager;
import android.os.BatteryManagerInternal;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IAodStateCallback;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.IHwBrightnessCallback;
import android.os.IPowerManager.Stub;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.BacklightBrightness;
import android.os.PowerManagerInternal;
import android.os.PowerManagerInternal.LowPowerModeListener;
import android.os.PowerSaveState;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.WorkSource;
import android.os.WorkSource.WorkChain;
import android.pc.IHwPCManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.service.dreams.DreamManagerInternal;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.Flog;
import android.util.HwPCUtils;
import android.util.Jlog;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.zrhung.IZrHung;
import android.zrhung.ZrHungData;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IBatteryStats;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.EventLogTags;
import com.android.server.HwServiceExFactory;
import com.android.server.HwServiceFactory;
import com.android.server.LockGuard;
import com.android.server.ServiceThread;
import com.android.server.Watchdog;
import com.android.server.Watchdog.Monitor;
import com.android.server.am.BatteryStatsService;
import com.android.server.am.IHwPowerInfoService;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.job.controllers.JobStatus;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.pm.PackageManagerService;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.batterysaver.BatterySaverController;
import com.android.server.power.batterysaver.BatterySaverStateMachine;
import com.android.server.power.batterysaver.BatterySavingStats;
import com.android.server.utils.PriorityDump;
import com.huawei.android.os.IHwPowerDAMonitorCallback;
import com.huawei.android.os.IHwPowerManager;
import huawei.cust.HwCustUtils;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class PowerManagerService extends AbsPowerManagerService implements Monitor, IHwPowerManagerInner {
    private static final String AOD_MODE_CMD = "/sys/class/graphics/fb0/alpm_setting";
    private static final String AOD_STATE_CMD = "/sys/class/graphics/fb0/alpm_function";
    private static final int BACK_SENSOR_COVER_MODE_BEIGHTNESS = -3;
    protected static final boolean DEBUG;
    private static final boolean DEBUG_ALL;
    private static boolean DEBUG_Controller = false;
    protected static final boolean DEBUG_SPEW = DEBUG;
    private static final int DEFAULT_DOUBLE_TAP_TO_WAKE = 0;
    private static final int DEFAULT_SCREEN_OFF_TIMEOUT = 15000;
    private static final int DEFAULT_SLEEP_TIMEOUT = -1;
    private static final int DIRTY_ACTUAL_DISPLAY_POWER_STATE_UPDATED = 8;
    private static final int DIRTY_BATTERY_STATE = 256;
    private static final int DIRTY_BOOT_COMPLETED = 16;
    private static final int DIRTY_DOCK_STATE = 1024;
    private static final int DIRTY_IS_POWERED = 64;
    private static final int DIRTY_PROXIMITY_POSITIVE = 512;
    private static final int DIRTY_QUIESCENT = 4096;
    private static final int DIRTY_SCREEN_BRIGHTNESS_BOOST = 2048;
    private static final int DIRTY_SETTINGS = 32;
    private static final int DIRTY_STAY_ON = 128;
    private static final int DIRTY_USER_ACTIVITY = 4;
    private static final int DIRTY_VR_MODE_CHANGED = 8192;
    protected static final int DIRTY_WAIT_BRIGHT_MODE = 16384;
    protected static final int DIRTY_WAKEFULNESS = 2;
    protected static final int DIRTY_WAKE_LOCKS = 1;
    private static final int EYE_PROTECTIION_OFF = 0;
    private static final int EYE_PROTECTIION_ON = 1;
    private static final int EYE_PROTECTIION_ON_BY_USER = 3;
    private static final int HALT_MODE_REBOOT = 1;
    private static final int HALT_MODE_REBOOT_SAFE_MODE = 2;
    private static final int HALT_MODE_SHUTDOWN = 0;
    private static final int HIGH_PRECISION_MAX_BRIGHTNESS = 10000;
    private static final String KEY_EYES_PROTECTION = "eyes_protection_mode";
    private static final String LAST_REBOOT_PROPERTY = "persist.sys.boot.reason";
    private static final HashSet<String> LOG_DROP_SET = new HashSet<String>() {
        {
            add("RILJ1001");
            add("LocationManagerService1000");
            add("*alarm*1000");
            add("*dexopt*1000");
            add("bluetooth_timer1002");
            add("GnssLocationProvider1000");
        }
    };
    static final long MIN_LONG_WAKE_CHECK_INTERVAL = 60000;
    private static final long MIN_TIME_FACE_DETECT_BEFORE_DIM = 1000;
    private static final int MSG_CHECK_FOR_LONG_WAKELOCKS = 4;
    private static final int MSG_FACE_DETECT_BEFORE_DIM = 103;
    private static final int MSG_POWERKEY_WAKEUP = 102;
    private static final int MSG_PROXIMITY_POSITIVE = 5;
    private static final int MSG_RECORD_WAKEUP = 104;
    private static final int MSG_SANDMAN = 2;
    private static final int MSG_SCREEN_BRIGHTNESS_BOOST_TIMEOUT = 3;
    private static final int MSG_USER_ACTIVITY_TIMEOUT = 1;
    protected static final int MSG_WAIT_BRIGHT_TIMEOUT = 101;
    private static final int POWER_FEATURE_DOUBLE_TAP_TO_WAKE = 1;
    private static final String REASON_BATTERY_THERMAL_STATE = "shutdown,thermal,battery";
    private static final String REASON_LOW_BATTERY = "shutdown,battery";
    private static final String REASON_REBOOT = "reboot";
    private static final String REASON_SHUTDOWN = "shutdown";
    private static final String REASON_THERMAL_SHUTDOWN = "shutdown,thermal";
    private static final String REASON_USERREQUESTED = "shutdown,userrequested";
    private static final int SCREEN_BRIGHTNESS_BOOST_TIMEOUT = 5000;
    private static final int SCREEN_ON_LATENCY_WARNING_MS = 200;
    private static final String SYSTEM_PROPERTY_QUIESCENT = "ro.boot.quiescent";
    private static final String SYSTEM_PROPERTY_RETAIL_DEMO_ENABLED = "sys.retaildemo.enabled";
    private static final String TAG = "PowerManagerService";
    private static final String TAG_PowerMS = "PowerMS";
    private static final String TRACE_SCREEN_ON = "Screen turning on";
    private static final int USER_ACTIVITY_SCREEN_BRIGHT = 1;
    private static final int USER_ACTIVITY_SCREEN_DIM = 2;
    private static final int USER_ACTIVITY_SCREEN_DREAM = 4;
    private static final int USER_TYPE_CHINA_BETA = 3;
    private static final int USER_TYPE_OVERSEA_BETA = 5;
    private static final String WAKEUP_REASON = "WakeUpReason";
    private static final int WAKE_LOCK_BUTTON_BRIGHT = 8;
    private static final int WAKE_LOCK_CPU = 1;
    private static final int WAKE_LOCK_DOZE = 64;
    private static final int WAKE_LOCK_DRAW = 128;
    private static final int WAKE_LOCK_PROXIMITY_SCREEN_OFF = 16;
    private static final int WAKE_LOCK_SCREEN_BRIGHT = 2;
    private static final int WAKE_LOCK_SCREEN_DIM = 4;
    private static final int WAKE_LOCK_STAY_AWAKE = 32;
    private static final String incalluiPackageName = "com.android.incallui";
    private static final boolean mIsPowerTurnTorchOff = SystemProperties.getBoolean("ro.config.power_turn_torch_off", false);
    private static final boolean mSupportAod = "1".equals(SystemProperties.get("ro.config.support_aod", null));
    protected static final boolean mSupportFaceDetect;
    private static final String machineCarPackageName = "com.huawei.vdrive";
    private static boolean sQuiescent;
    private static final boolean sSupportFaceRecognition = SystemProperties.getBoolean("ro.config.face_recognition", false);
    private boolean inVdriveBackLightMode;
    protected boolean mAdjustTimeNextUserActivity;
    private int mAlpmState;
    private boolean mAlwaysOnEnabled;
    private final AmbientDisplayConfiguration mAmbientDisplayConfiguration;
    private IAppOpsService mAppOps;
    private Light mAttentionLight;
    protected boolean mAuthSucceeded;
    private int mAutoBrightnessLevel;
    private Light mBackLight;
    BacklightBrightness mBacklightBrightness;
    private int mBatteryLevel;
    private boolean mBatteryLevelLow;
    private int mBatteryLevelWhenDreamStarted;
    private BatteryManagerInternal mBatteryManagerInternal;
    private final BatterySaverController mBatterySaverController;
    private final BatterySaverPolicy mBatterySaverPolicy;
    private final BatterySaverStateMachine mBatterySaverStateMachine;
    private final BatterySavingStats mBatterySavingStats;
    private IBatteryStats mBatteryStats;
    protected boolean mBootCompleted;
    private Runnable[] mBootCompletedRunnables;
    private boolean mBrightnessUseTwilight;
    protected boolean mBrightnessWaitModeEnabled;
    protected boolean mBrightnessWaitRet;
    final Constants mConstants;
    private final Context mContext;
    private int mCoverModeBrightness;
    private int mCurrentUserId;
    private HwCustPowerManagerService mCust;
    private boolean mDecoupleHalAutoSuspendModeFromDisplayConfig;
    private boolean mDecoupleHalInteractiveModeFromDisplayConfig;
    private boolean mDeviceIdleMode;
    int[] mDeviceIdleTempWhitelist;
    int[] mDeviceIdleWhitelist;
    protected int mDirty;
    protected DisplayManagerInternal mDisplayManagerInternal;
    private final DisplayPowerCallbacks mDisplayPowerCallbacks;
    private final DisplayPowerRequest mDisplayPowerRequest;
    private boolean mDisplayReady;
    private final SuspendBlocker mDisplaySuspendBlocker;
    private int mDockState;
    private boolean mDoubleTapWakeEnabled;
    private boolean mDozeAfterScreenOff;
    private int mDozeScreenBrightnessOverrideFromDreamManager;
    private int mDozeScreenStateOverrideFromDreamManager;
    private boolean mDrawWakeLockOverrideFromSidekick;
    private DreamManagerInternal mDreamManager;
    private boolean mDreamsActivateOnDockSetting;
    private boolean mDreamsActivateOnSleepSetting;
    private boolean mDreamsActivatedOnDockByDefaultConfig;
    private boolean mDreamsActivatedOnSleepByDefaultConfig;
    private int mDreamsBatteryLevelDrainCutoffConfig;
    private int mDreamsBatteryLevelMinimumWhenNotPoweredConfig;
    private int mDreamsBatteryLevelMinimumWhenPoweredConfig;
    private boolean mDreamsEnabledByDefaultConfig;
    private boolean mDreamsEnabledOnBatteryConfig;
    private boolean mDreamsEnabledSetting;
    private boolean mDreamsSupportedConfig;
    private boolean mDropLogs;
    private int mEyesProtectionMode;
    private boolean mFirstBoot;
    private boolean mForceDoze;
    private int mForegroundProfile;
    private boolean mHalAutoSuspendModeEnabled;
    private boolean mHalInteractiveModeEnabled;
    protected final PowerManagerHandler mHandler;
    private final ServiceThread mHandlerThread;
    private boolean mHoldingDisplaySuspendBlocker;
    private boolean mHoldingWakeLockSuspendBlocker;
    HwInnerPowerManagerService mHwInnerService;
    IHwPowerManagerServiceEx mHwPowerEx;
    private IHwPowerInfoService mHwPowerInfoService;
    private InputManagerInternal mInputManagerInternal;
    private boolean mIsCoverModeEnabled;
    private boolean mIsPowerInfoStatus;
    private boolean mIsPowered;
    private boolean mIsVrModeEnabled;
    private boolean mKeyguardLocked;
    private KeyguardManager mKeyguardManager;
    private long mLastInteractivePowerHintTime;
    private long mLastOneSecActivityTime;
    private long mLastScreenBrightnessBoostTime;
    protected long mLastSleepTime;
    protected long mLastSleepTimeDuoToFastFP;
    private long mLastUserActivityTime;
    private long mLastUserActivityTimeNoChangeLights;
    protected long mLastWakeTime;
    private long mLastWarningAboutUserActivityPermission;
    private boolean mLightDeviceIdleMode;
    protected LightsManager mLightsManager;
    protected final Object mLock;
    private final ArrayList<LowPowerModeListener> mLowPowerModeListeners;
    private long mMaximumScreenDimDurationConfig;
    private float mMaximumScreenDimRatioConfig;
    private long mMaximumScreenOffTimeoutFromDeviceAdmin;
    private long mMinimumScreenOffTimeoutConfig;
    protected Notifier mNotifier;
    private long mNotifyLongDispatched;
    private long mNotifyLongNextCheck;
    private long mNotifyLongScheduled;
    private long mOverriddenTimeout;
    private int mPlugType;
    private WindowManagerPolicy mPolicy;
    HwPowerDAMonitorProxy mPowerProxy;
    private final SparseArray<ProfilePowerState> mProfilePowerState;
    protected boolean mProximityPositive;
    protected boolean mRequestWaitForNegativeProximity;
    private boolean mSandmanScheduled;
    private boolean mSandmanSummoned;
    private boolean mScreenBrightnessBoostInProgress;
    private int mScreenBrightnessModeSetting;
    private int mScreenBrightnessOverrideFromWindowManager;
    private int mScreenBrightnessSetting;
    private int mScreenBrightnessSettingDefault;
    private int mScreenBrightnessSettingMaximum;
    private int mScreenBrightnessSettingMinimum;
    private long mScreenOffTimeoutSetting;
    private boolean mScreenTimeoutFlag;
    private SettingsObserver mSettingsObserver;
    protected boolean mSkipWaitKeyguardDismiss;
    private long mSleepTimeoutSetting;
    private int mSmartBacklightEnableSetting;
    private boolean mStayOn;
    private int mStayOnWhilePluggedInSetting;
    private boolean mSupportsDoubleTapWakeConfig;
    private final ArrayList<SuspendBlocker> mSuspendBlockers;
    private boolean mSuspendWhenScreenOffDueToProximityConfig;
    private boolean mSystemReady;
    private int mTemporaryScreenAutoBrightnessSettingOverride;
    private boolean mTheaterModeEnabled;
    private final SparseArray<UidState> mUidState;
    private boolean mUidsChanged;
    private boolean mUidsChanging;
    private boolean mUpdateBacklightBrightnessFlag;
    private int mUserActivitySummary;
    private long mUserActivityTimeoutOverrideFromWindowManager;
    private boolean mUserFirstBoot;
    private boolean mUserInactiveOverrideFromWindowManager;
    private final IVrStateCallbacks mVrStateCallbacks;
    private int mWakeLockSummary;
    private final SuspendBlocker mWakeLockSuspendBlocker;
    protected final ArrayList<WakeLock> mWakeLocks;
    private boolean mWakeUpWhenPluggedOrUnpluggedConfig;
    private boolean mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig;
    protected int mWakefulness;
    private boolean mWakefulnessChanging;
    private WirelessChargerDetector mWirelessChargerDetector;
    private boolean misBetaUser;

    /* renamed from: com.android.server.power.PowerManagerService$4 */
    class AnonymousClass4 implements Runnable {
        final /* synthetic */ boolean val$confirm;
        final /* synthetic */ int val$haltMode;
        final /* synthetic */ String val$reason;

        AnonymousClass4(int i, boolean z, String str) {
            this.val$haltMode = i;
            this.val$confirm = z;
            this.val$reason = str;
        }

        public void run() {
            synchronized (this) {
                if (this.val$haltMode == 2) {
                    ShutdownThread.rebootSafeMode(PowerManagerService.this.getUiContext(), this.val$confirm);
                } else if (this.val$haltMode == 1) {
                    ShutdownThread.reboot(PowerManagerService.this.getUiContext(), this.val$reason, this.val$confirm);
                } else {
                    ShutdownThread.shutdown(PowerManagerService.this.getUiContext(), this.val$reason, this.val$confirm);
                }
            }
        }
    }

    private final class BatteryReceiver extends BroadcastReceiver {
        private BatteryReceiver() {
        }

        /* synthetic */ BatteryReceiver(PowerManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.handleBatteryStateChangedLocked();
                if (PowerManagerService.this.mCust != null) {
                    PowerManagerService.this.mCust.handleDreamLocked();
                }
            }
            PowerManagerService powerManagerService = PowerManagerService.this;
            boolean z = 3 == SystemProperties.getInt("ro.logsystem.usertype", 0) || 5 == SystemProperties.getInt("ro.logsystem.usertype", 0) || SystemProperties.getBoolean("persist.sys.huawei.debug.on", false) || SystemProperties.getBoolean("hwlog.remotedebug", false);
            powerManagerService.misBetaUser = z;
            if (PowerManagerService.this.misBetaUser && !PowerManagerService.this.mIsPowerInfoStatus) {
                Slog.i(PowerManagerService.TAG, "getHwPowerInfoService instance");
                PowerManagerService.this.mHwPowerInfoService = HwServiceFactory.getHwPowerInfoService(PowerManagerService.this.mContext, true);
                if (PowerManagerService.this.mHwPowerInfoService != null) {
                    PowerManagerService.this.mIsPowerInfoStatus = true;
                }
            } else if (!PowerManagerService.this.misBetaUser && PowerManagerService.this.mIsPowerInfoStatus) {
                PowerManagerService.this.mIsPowerInfoStatus = false;
                Slog.i(PowerManagerService.TAG, "getHwPowerInfoService uninstance");
                PowerManagerService.this.mHwPowerInfoService = HwServiceFactory.getHwPowerInfoService(PowerManagerService.this.mContext, false);
            }
        }
    }

    private final class BinderService extends Stub {
        private static final int MAX_DEFAULT_BRIGHTNESS = 255;
        private static final int SEEK_BAR_RANGE = 10000;
        private double mCovertFactor;
        private int mMaximumBrightness;
        private int mMinimumBrightness;
        ArrayList<String> mWakeLockPackageNameList;

        private BinderService() {
            this.mCovertFactor = 1.7999999523162842d;
            this.mMinimumBrightness = 4;
            this.mMaximumBrightness = 255;
            this.mWakeLockPackageNameList = new ArrayList();
        }

        /* synthetic */ BinderService(PowerManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
            new PowerManagerShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
        }

        public void acquireWakeLockWithUid(IBinder lock, int flags, String tag, String packageName, int uid) {
            if (uid < 0) {
                uid = Binder.getCallingUid();
            }
            acquireWakeLock(lock, flags, tag, packageName, new WorkSource(uid), null);
        }

        public void setStartDreamFromOtherFlag(boolean flag) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            if (PowerManagerService.this.mCust != null) {
                PowerManagerService.this.mCust.setStartDreamFromUser(flag);
            }
        }

        public void setMirrorLinkPowerStatus(boolean status) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            PowerManagerService.this.mBackLight.setMirrorLinkBrightnessStatus(false);
            if (PowerManagerService.this.mInputManagerInternal != null) {
                PowerManagerService.this.mInputManagerInternal.setMirrorLinkInputStatus(false);
            }
            String str = PowerManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setMirrorLinkPowerStatus status");
            stringBuilder.append(status);
            Slog.d(str, stringBuilder.toString());
        }

        public boolean startDream() {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            boolean z = false;
            if (PowerManagerService.this.mCust == null) {
                return false;
            }
            HwCustPowerManagerService access$1800 = PowerManagerService.this.mCust;
            if (PowerManagerService.this.mWakefulness == 3) {
                z = true;
            }
            return access$1800.startDream(z);
        }

        public boolean stopDream() {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            if (PowerManagerService.this.mCust != null) {
                return PowerManagerService.this.mCust.stopDream();
            }
            return false;
        }

        public void powerHint(int hintId, int data) {
            if (PowerManagerService.this.mSystemReady) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                PowerManagerService.this.powerHintInternal(hintId, data);
            }
        }

        public void acquireWakeLock(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag) {
            Throwable th;
            int i = flags;
            String str = tag;
            String str2 = packageName;
            WorkSource workSource = ws;
            if (lock == null) {
                throw new IllegalArgumentException("lock must not be null");
            } else if (str2 == null) {
                throw new IllegalArgumentException("packageName must not be null");
            } else if (PowerManagerService.this.isAppCanGetDrawWakeLock(i, str2, workSource, str) && !PowerManagerService.this.isAppWakeLockFilterTag(i, str2, workSource)) {
                PowerManager.validateWakeLockParameters(flags, tag);
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
                if ((i & 64) != 0) {
                    PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                }
                if (workSource == null || ws.isEmpty()) {
                    workSource = null;
                } else {
                    PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
                }
                WorkSource ws2 = workSource;
                int uid = Binder.getCallingUid();
                int pid = Binder.getCallingPid();
                long ident = Binder.clearCallingIdentity();
                if ((268435456 & i) != 0 && PowerManagerService.this.mHwPowerEx.isAwarePreventScreenOn(str2, str)) {
                    i &= -268435457;
                }
                int flags2 = i;
                long ident2;
                try {
                    String str3 = str;
                    ident2 = ident;
                    try {
                        if (true == PowerManagerService.this.acquireProxyWakeLock(lock, flags2, str3, str2, ws2, historyTag, uid, pid)) {
                            Binder.restoreCallingIdentity(ident2);
                            return;
                        }
                        PowerManagerService.this.acquireWakeLockInternal(lock, flags2, tag, packageName, ws2, historyTag, uid, pid);
                        Binder.restoreCallingIdentity(ident2);
                    } catch (Throwable th2) {
                        th = th2;
                        Binder.restoreCallingIdentity(ident2);
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    ident2 = ident;
                    Binder.restoreCallingIdentity(ident2);
                    throw th;
                }
            }
        }

        public void releaseWakeLock(IBinder lock, int flags) {
            if (lock != null) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.releaseProxyWakeLock(lock);
                    PowerManagerService.this.releaseWakeLockInternal(lock, flags);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                throw new IllegalArgumentException("lock must not be null");
            }
        }

        public int setColorTemperature(int colorTemper) {
            String str;
            StringBuilder stringBuilder;
            if (1000 != UserHandle.getAppId(Binder.getCallingUid())) {
                str = PowerManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("permission not allowed. uid = ");
                stringBuilder.append(Binder.getCallingUid());
                Slog.e(str, stringBuilder.toString());
                return -1;
            }
            str = PowerManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setColorTemperature");
            stringBuilder.append(colorTemper);
            Slog.d(str, stringBuilder.toString());
            return PowerManagerService.this.setColorTemperatureInternal(colorTemper);
        }

        public int updateRgbGamma(float red, float green, float blue) {
            if (1000 == UserHandle.getAppId(Binder.getCallingUid())) {
                return PowerManagerService.this.updateRgbGammaInternal(red, green, blue);
            }
            String str = PowerManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("permission not allowed. uid = ");
            stringBuilder.append(Binder.getCallingUid());
            Slog.e(str, stringBuilder.toString());
            return -1;
        }

        public void updateWakeLockUids(IBinder lock, int[] uids) {
            WorkSource ws = null;
            if (uids != null) {
                ws = new WorkSource();
                for (int add : uids) {
                    ws.add(add);
                }
            }
            updateWakeLockWorkSource(lock, ws, null);
        }

        public void updateWakeLockWorkSource(IBinder lock, WorkSource ws, String historyTag) {
            if (lock != null) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.WAKE_LOCK", null);
                if (ws == null || ws.isEmpty()) {
                    ws = null;
                } else {
                    PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.UPDATE_DEVICE_STATS", null);
                }
                int callingUid = Binder.getCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    if (true == PowerManagerService.this.updateProxyWakeLockWorkSource(lock, ws, historyTag, callingUid)) {
                        Binder.restoreCallingIdentity(ident);
                        return;
                    }
                    PowerManagerService.this.updateWakeLockWorkSourceInternal(lock, ws, historyTag, callingUid);
                    Binder.restoreCallingIdentity(ident);
                } catch (IllegalArgumentException e) {
                    String str = PowerManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception when search wack lock :");
                    stringBuilder.append(e);
                    Slog.e(str, stringBuilder.toString());
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                throw new IllegalArgumentException("lock must not be null");
            }
        }

        public boolean isWakeLockLevelSupported(int level) {
            long ident = Binder.clearCallingIdentity();
            try {
                boolean access$4200 = PowerManagerService.this.isWakeLockLevelSupportedInternal(level);
                return access$4200;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void userActivity(long eventTime, int event, int flags) {
            long now = SystemClock.uptimeMillis();
            if (PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.DEVICE_POWER") != 0 && PowerManagerService.this.mContext.checkCallingOrSelfPermission("android.permission.USER_ACTIVITY") != 0) {
                synchronized (PowerManagerService.this.mLock) {
                    if (now >= PowerManagerService.this.mLastWarningAboutUserActivityPermission + BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS) {
                        PowerManagerService.this.mLastWarningAboutUserActivityPermission = now;
                        String str = PowerManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Ignoring call to PowerManager.userActivity() because the caller does not have DEVICE_POWER or USER_ACTIVITY permission.  Please fix your app!   pid=");
                        stringBuilder.append(Binder.getCallingPid());
                        stringBuilder.append(" uid=");
                        stringBuilder.append(Binder.getCallingUid());
                        Slog.w(str, stringBuilder.toString());
                    }
                }
            } else if (eventTime <= now) {
                int uid = Binder.getCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.userActivityInternal(eventTime, event, flags, uid);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                throw new IllegalArgumentException("event time must not be in the future");
            }
        }

        public void wakeUp(long eventTime, String reason, String opPackageName) {
            if (eventTime <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                if (Jlog.isPerfTest()) {
                    Jlog.i(2202, "JL_PWRSCRON_PMS_WAKEUP");
                }
                int uid = Binder.getCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.wakeUpInternal(eventTime, reason, uid, opPackageName, uid);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                throw new IllegalArgumentException("event time must not be in the future");
            }
        }

        public void goToSleep(long eventTime, int reason, int flags) {
            if (eventTime <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                int uid = Binder.getCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.goToSleepInternal(eventTime, reason, flags, uid);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                throw new IllegalArgumentException("event time must not be in the future");
            }
        }

        public void nap(long eventTime) {
            if (eventTime <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                int uid = Binder.getCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.napInternal(eventTime, uid);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                throw new IllegalArgumentException("event time must not be in the future");
            }
        }

        public boolean isInteractive() {
            long ident = Binder.clearCallingIdentity();
            try {
                boolean access$4700 = PowerManagerService.this.isInteractiveInternal();
                return access$4700;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isPowerSaveMode() {
            long ident = Binder.clearCallingIdentity();
            try {
                boolean isEnabled = PowerManagerService.this.mBatterySaverController.isEnabled();
                return isEnabled;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public PowerSaveState getPowerSaveState(int serviceType) {
            long ident = Binder.clearCallingIdentity();
            try {
                PowerSaveState batterySaverPolicy = PowerManagerService.this.mBatterySaverPolicy.getBatterySaverPolicy(serviceType, PowerManagerService.this.mBatterySaverController.isEnabled());
                return batterySaverPolicy;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean setPowerSaveMode(boolean enabled) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                boolean access$5000 = PowerManagerService.this.setLowPowerModeInternal(enabled);
                return access$5000;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isDeviceIdleMode() {
            long ident = Binder.clearCallingIdentity();
            try {
                boolean isDeviceIdleModeInternal = PowerManagerService.this.isDeviceIdleModeInternal();
                return isDeviceIdleModeInternal;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean isLightDeviceIdleMode() {
            long ident = Binder.clearCallingIdentity();
            try {
                boolean isLightDeviceIdleModeInternal = PowerManagerService.this.isLightDeviceIdleModeInternal();
                return isLightDeviceIdleModeInternal;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int getLastShutdownReason() {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                int lastShutdownReasonInternal = PowerManagerService.this.getLastShutdownReasonInternal(PowerManagerService.LAST_REBOOT_PROPERTY);
                return lastShutdownReasonInternal;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void reboot(boolean confirm, String reason, boolean wait) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            if ("recovery".equals(reason) || "recovery-update".equals(reason)) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.RECOVERY", null);
            }
            int uid = Binder.getCallingUid();
            int pid = Binder.getCallingPid();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PowerManagerService reboot_reason:");
            stringBuilder.append(reason);
            stringBuilder.append(", uid=");
            stringBuilder.append(uid);
            stringBuilder.append(", pid=");
            stringBuilder.append(pid);
            Flog.e(1600, stringBuilder.toString());
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(1, confirm, reason, wait);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void rebootSafeMode(boolean confirm, boolean wait) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(2, confirm, "safemode", wait);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void shutdown(boolean confirm, String reason, boolean wait) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            int uid = Binder.getCallingUid();
            int pid = Binder.getCallingPid();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("PowerManagerService shutdown  uid=");
            stringBuilder.append(uid);
            stringBuilder.append(", pid=");
            stringBuilder.append(pid);
            Flog.e(1600, stringBuilder.toString());
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.shutdownOrRebootInternal(0, confirm, reason, wait);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void crash(String message) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.REBOOT", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.crashInternal(message);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setStayOnSetting(int val) {
            int uid = Binder.getCallingUid();
            if (uid == 0 || Settings.checkAndNoteWriteSettingsOperation(PowerManagerService.this.mContext, uid, Settings.getPackageNameForUid(PowerManagerService.this.mContext, uid), true)) {
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.setStayOnSettingInternal(val);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }

        public void setTemporaryScreenBrightnessSettingOverride(int brightness) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            PowerManagerService.this.sendTempBrightnessToMonitor("tempManualBrightness", brightness);
            long ident = Binder.clearCallingIdentity();
            try {
                if (PowerManagerService.this.mDisplayManagerInternal != null) {
                    PowerManagerService.this.mDisplayManagerInternal.setTemporaryScreenBrightnessSettingOverride(brightness);
                }
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(float adj) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            Binder.restoreCallingIdentity(Binder.clearCallingIdentity());
        }

        public void setTemporaryScreenAutoBrightnessSettingOverride(int brightness) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            PowerManagerService.this.sendTempBrightnessToMonitor("tempAutoBrightness", brightness);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setTemporaryScreenAutoBrightnessSettingOverrideInternal(brightness);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int convertSeekbarProgressToBrightness(int progress) {
            if (progress < 0) {
                if (PowerManagerService.DEBUG) {
                    String str = PowerManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("brightnessSeekbar progress=");
                    stringBuilder.append(progress);
                    stringBuilder.append(" <min=0");
                    Slog.i(str, stringBuilder.toString());
                }
                progress = 0;
            }
            if (progress > 10000) {
                if (PowerManagerService.DEBUG) {
                    String str2 = PowerManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("brightnessSeekbar progress=");
                    stringBuilder2.append(progress);
                    stringBuilder2.append(" >max=");
                    stringBuilder2.append(10000);
                    Slog.i(str2, stringBuilder2.toString());
                }
                progress = 10000;
            }
            if (SystemProperties.getInt("ro.config.hw_high_bright_mode", 1) == 1) {
                this.mMaximumBrightness = PowerManagerService.this.mDisplayManagerInternal.getMaxBrightnessForSeekbar();
            } else {
                this.mMaximumBrightness = 255;
            }
            return Math.round((((float) Math.pow((double) (((float) progress) / 10000.0f), this.mCovertFactor)) * ((float) (this.mMaximumBrightness - this.mMinimumBrightness))) + ((float) this.mMinimumBrightness));
        }

        public float convertBrightnessToSeekbarPercentage(float brightness) {
            String str;
            StringBuilder stringBuilder;
            if (SystemProperties.getInt("ro.config.hw_high_bright_mode", 1) == 1) {
                this.mMaximumBrightness = PowerManagerService.this.mDisplayManagerInternal.getMaxBrightnessForSeekbar();
            } else {
                this.mMaximumBrightness = 255;
            }
            if (brightness > ((float) this.mMaximumBrightness)) {
                if (PowerManagerService.DEBUG) {
                    str = PowerManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HBM brightness=");
                    stringBuilder.append(brightness);
                    stringBuilder.append(" >Max=");
                    stringBuilder.append(this.mMaximumBrightness);
                    Slog.i(str, stringBuilder.toString());
                }
                brightness = (float) this.mMaximumBrightness;
            }
            if (brightness < ((float) this.mMinimumBrightness)) {
                if (PowerManagerService.DEBUG) {
                    str = PowerManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("brightnessSeekbar brightness=");
                    stringBuilder.append(brightness);
                    stringBuilder.append(" <min=");
                    stringBuilder.append(this.mMinimumBrightness);
                    Slog.i(str, stringBuilder.toString());
                }
                brightness = (float) this.mMinimumBrightness;
            }
            return (float) Math.pow((double) ((brightness - ((float) this.mMinimumBrightness)) / ((float) (this.mMaximumBrightness - this.mMinimumBrightness))), 1.0d / this.mCovertFactor);
        }

        public void setBrightnessAnimationTime(boolean animationEnabled, int millisecond) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            if (PowerManagerService.DEBUG) {
                String str = PowerManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setAnimationTime animationEnabled=");
                stringBuilder.append(animationEnabled);
                stringBuilder.append(",millisecond=");
                stringBuilder.append(millisecond);
                Slog.i(str, stringBuilder.toString());
            }
            PowerManagerService.this.mDisplayManagerInternal.setBrightnessAnimationTime(animationEnabled, millisecond);
        }

        public void onCoverModeChanged(boolean iscovered) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            if (PowerManagerService.this.mIsCoverModeEnabled != iscovered) {
                PowerManagerService.this.updatePowerStateLocked();
            }
            PowerManagerService.this.mIsCoverModeEnabled = iscovered;
            if (PowerManagerService.DEBUG) {
                String str = PowerManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("coverModeChange mIsCoverModeEnabled=");
                stringBuilder.append(PowerManagerService.this.mIsCoverModeEnabled);
                Slog.d(str, stringBuilder.toString());
            }
        }

        public int getCoverModeBrightnessFromLastScreenBrightness() {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            return PowerManagerService.this.mDisplayManagerInternal.getCoverModeBrightnessFromLastScreenBrightness();
        }

        public void setMaxBrightnessFromThermal(int brightness) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            PowerManagerService.this.mDisplayManagerInternal.setMaxBrightnessFromThermal(brightness);
        }

        public void setBrightnessNoLimit(int brightness, int time) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            PowerManagerService.this.mDisplayManagerInternal.setBrightnessNoLimit(brightness, time);
        }

        public void setModeToAutoNoClearOffsetEnable(boolean enable) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            PowerManagerService.this.mDisplayManagerInternal.setModeToAutoNoClearOffsetEnable(enable);
        }

        public void setAodAlpmState(int globalState) {
            PowerManagerService.this.mAlpmState = globalState;
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            PowerManagerService.this.mDisplayManagerInternal.setAodAlpmState(globalState);
        }

        public void setAttentionLight(boolean on, int color) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setAttentionLightInternal(on, color);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setDozeAfterScreenOff(boolean on) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setDozeAfterScreenOffInternal(on);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void boostScreenBrightness(long eventTime) {
            if (eventTime <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                int uid = Binder.getCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.boostScreenBrightnessInternal(eventTime, uid);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                throw new IllegalArgumentException("event time must not be in the future");
            }
        }

        public boolean isScreenBrightnessBoosted() {
            long ident = Binder.clearCallingIdentity();
            try {
                boolean access$5900 = PowerManagerService.this.isScreenBrightnessBoostedInternal();
                return access$5900;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (DumpUtils.checkDumpPermission(PowerManagerService.this.mContext, PowerManagerService.TAG, pw)) {
                long ident = Binder.clearCallingIdentity();
                boolean isDumpProto = false;
                for (String arg : args) {
                    if (arg.equals(PriorityDump.PROTO_ARG)) {
                        isDumpProto = true;
                    }
                }
                if (isDumpProto) {
                    try {
                        PowerManagerService.this.dumpProto(fd);
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(ident);
                    }
                } else {
                    PowerManagerService.this.dumpInternal(pw);
                }
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void startWakeUpReady(long eventTime, String opPackageName) {
            if (eventTime <= SystemClock.uptimeMillis()) {
                PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                int uid = Binder.getCallingUid();
                long ident = Binder.clearCallingIdentity();
                try {
                    PowerManagerService.this.startWakeUpReadyInternal(eventTime, uid, opPackageName);
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            } else {
                throw new IllegalArgumentException("event time must not be in the future");
            }
        }

        public void stopWakeUpReady(long eventTime, boolean enableBright, String opPackageName) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            int uid = Binder.getCallingUid();
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.stopWakeUpReadyInternal(eventTime, uid, enableBright, opPackageName);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setAuthSucceeded() {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setAuthSucceededInternal();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void setPowerState(boolean state) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                PowerManagerService.this.setPowerStateInternal(state);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int getDisplayPanelType() {
            int type = -1;
            long ident = Binder.clearCallingIdentity();
            try {
                type = PowerManagerService.this.getDisplayPanelTypeInternal();
                return type;
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public int hwBrightnessSetData(String name, Bundle data) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            return PowerManagerService.this.hwBrightnessSetDataInternal(name, data);
        }

        public int hwBrightnessGetData(String name, Bundle data) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            return PowerManagerService.this.hwBrightnessGetDataInternal(name, data);
        }

        public int hwBrightnessRegisterCallback(IHwBrightnessCallback cb, List<String> filter) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            return PowerManagerService.this.hwBrightnessRegisterCallbackInternal(cb, filter);
        }

        public int hwBrightnessUnregisterCallback(IHwBrightnessCallback cb) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            return PowerManagerService.this.hwBrightnessUnregisterCallbackInternal(cb);
        }

        public void updateBlockedUids(int uid, boolean isBlocked) {
        }

        public boolean isHighPrecision() {
            boolean isHighPrecision;
            synchronized (PowerManagerService.this.mLock) {
                isHighPrecision = PowerManagerService.this.mBackLight.isHighPrecision();
            }
            return isHighPrecision;
        }

        public boolean isUsingSkipWakeLock(int uid, String tag) {
            return PowerManagerService.this.isSkipWakeLockUsing(uid, tag);
        }

        public void regeditAodStateCallback(IAodStateCallback callback) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            Slog.i(PowerManagerService.TAG, "AOD PowerManagerService regeditAodStateCallback()");
            if (PowerManagerService.mSupportAod) {
                PowerManagerService.this.mPolicy.regeditAodStateCallback(callback);
            }
        }

        public void unregeditAodStateCallback(IAodStateCallback callback) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            Slog.i(PowerManagerService.TAG, "AOD PowerManagerService unregeditAodStateCallback()");
            if (PowerManagerService.mSupportAod) {
                PowerManagerService.this.mPolicy.unregeditAodStateCallback(callback);
            }
        }

        public void setAodState(int globalState, int alpmMode) {
            Slog.i(PowerManagerService.TAG, "AOD PowerManagerService setAodState()");
            if (PowerManagerService.mSupportAod) {
                String str = PowerManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setAodStateBySysfs:  globalState=");
                stringBuilder.append(globalState);
                stringBuilder.append(", AlpmMode=");
                stringBuilder.append(alpmMode);
                Slog.d(str, stringBuilder.toString());
                if (globalState != -1) {
                    PowerManagerService.this.setAodStateBySysfs(PowerManagerService.AOD_STATE_CMD, globalState);
                }
                if (alpmMode != -1) {
                    PowerManagerService.this.setAodStateBySysfs(PowerManagerService.AOD_MODE_CMD, alpmMode);
                }
            }
        }

        public int getAodState(String file) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            Slog.i(PowerManagerService.TAG, "AOD PowerManagerService getAodState()");
            if (PowerManagerService.mSupportAod) {
                return PowerManagerService.this.getAodStateBySysfs(file);
            }
            return -1;
        }

        public void setDozeOverrideFromAod(int screenState, int screenBrightness, IBinder binder) {
            PowerManagerService.this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            Slog.i(PowerManagerService.TAG, "AOD PowerManagerService setDozeOverrideFromAod()");
            if (PowerManagerService.mSupportAod) {
                synchronized (PowerManagerService.this.mLock) {
                    switch (screenState) {
                        case 0:
                        case 2:
                            PowerManagerService.this.mForceDoze = false;
                            break;
                        case 1:
                            PowerManagerService.this.mForceDoze = false;
                            PowerManagerService.this.setWakefulnessLocked(0, 0);
                            break;
                        case 3:
                            if (!(PowerManagerService.this.mForceDoze || PowerManagerInternal.isInteractive(PowerManagerService.this.mWakefulness))) {
                                PowerManagerService.this.setWakefulnessLocked(3, 0);
                                PowerManagerService.this.mForceDoze = true;
                                break;
                            }
                        case 4:
                            break;
                        default:
                            PowerManagerService.this.mForceDoze = false;
                            break;
                    }
                    if (screenBrightness < -1 || screenBrightness > 255) {
                        screenBrightness = -1;
                    }
                    String str = PowerManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("setDozeOverrideFromAod screenState = ");
                    stringBuilder.append(screenState);
                    stringBuilder.append(", Brightness = ");
                    stringBuilder.append(screenBrightness);
                    stringBuilder.append(", ForceDoze = ");
                    stringBuilder.append(PowerManagerService.this.mForceDoze);
                    stringBuilder.append(", wakefulness = ");
                    stringBuilder.append(PowerManagerInternal.isInteractive(PowerManagerService.this.mWakefulness));
                    Slog.d(str, stringBuilder.toString());
                    PowerManagerService.this.setDozeOverrideFromAodLocked(screenState, screenBrightness);
                }
            }
        }

        public List<String> getWakeLockPackageName() {
            List list;
            synchronized (PowerManagerService.this.mLock) {
                this.mWakeLockPackageNameList.clear();
                for (int i = 0; i < PowerManagerService.this.mWakeLocks.size(); i++) {
                    WakeLock wakeLock = (WakeLock) PowerManagerService.this.mWakeLocks.get(i);
                    if (wakeLock != null && ((wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 6 || (wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 10 || (wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 26)) {
                        this.mWakeLockPackageNameList.add(wakeLock.mPackageName);
                    }
                }
                list = this.mWakeLockPackageNameList;
            }
            return list;
        }

        public IBinder getHwInnerService() {
            return PowerManagerService.this.mHwInnerService;
        }
    }

    private final class Constants extends ContentObserver {
        private static final boolean DEFAULT_NO_CACHED_WAKE_LOCKS = true;
        private static final String KEY_NO_CACHED_WAKE_LOCKS = "no_cached_wake_locks";
        public boolean NO_CACHED_WAKE_LOCKS = true;
        private final KeyValueListParser mParser = new KeyValueListParser(',');
        private ContentResolver mResolver;

        public Constants(Handler handler) {
            super(handler);
        }

        public void start(ContentResolver resolver) {
            this.mResolver = resolver;
            this.mResolver.registerContentObserver(Global.getUriFor("power_manager_constants"), false, this);
            updateConstants();
        }

        public void onChange(boolean selfChange, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (PowerManagerService.this.mLock) {
                try {
                    this.mParser.setString(Global.getString(this.mResolver, "power_manager_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(PowerManagerService.TAG, "Bad alarm manager settings", e);
                }
                this.NO_CACHED_WAKE_LOCKS = this.mParser.getBoolean(KEY_NO_CACHED_WAKE_LOCKS, true);
            }
        }

        void dump(PrintWriter pw) {
            pw.println("  Settings power_manager_constants:");
            pw.print("    ");
            pw.print(KEY_NO_CACHED_WAKE_LOCKS);
            pw.print("=");
            pw.println(this.NO_CACHED_WAKE_LOCKS);
        }

        void dumpProto(ProtoOutputStream proto) {
            long constantsToken = proto.start(1146756268033L);
            proto.write(1133871366145L, this.NO_CACHED_WAKE_LOCKS);
            proto.end(constantsToken);
        }
    }

    private final class DockReceiver extends BroadcastReceiver {
        private DockReceiver() {
        }

        /* synthetic */ DockReceiver(PowerManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                int dockState = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                if (PowerManagerService.this.mDockState != dockState) {
                    PowerManagerService.this.mDockState = dockState;
                    PowerManagerService powerManagerService = PowerManagerService.this;
                    powerManagerService.mDirty |= 1024;
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }
        }
    }

    private final class DreamReceiver extends BroadcastReceiver {
        private DreamReceiver() {
        }

        /* synthetic */ DreamReceiver(PowerManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            SystemClock.sleep(50);
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.scheduleSandmanLocked();
            }
        }
    }

    private final class ForegroundProfileObserver extends SynchronousUserSwitchObserver {
        private ForegroundProfileObserver() {
        }

        /* synthetic */ ForegroundProfileObserver(PowerManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onUserSwitching(int newUserId) throws RemoteException {
        }

        public void onForegroundProfileSwitch(int newProfileId) throws RemoteException {
            long now = SystemClock.uptimeMillis();
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.mForegroundProfile = newProfileId;
                PowerManagerService.this.maybeUpdateForegroundProfileLastActivityLocked(now);
            }
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface HaltMode {
    }

    public class HwInnerPowerManagerService extends IHwPowerManager.Stub {
        PowerManagerService mPMS;

        HwInnerPowerManagerService(PowerManagerService pms) {
            this.mPMS = pms;
        }

        public boolean registerPowerMonitorCallback(IHwPowerDAMonitorCallback callback) {
            PowerManagerService.this.mPowerProxy.registerPowerMonitorCallback(callback);
            return true;
        }
    }

    private final class LocalService extends PowerManagerInternal {
        private LocalService() {
        }

        /* synthetic */ LocalService(PowerManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void setScreenBrightnessOverrideFromWindowManager(int screenBrightness) {
            if (screenBrightness < -1) {
                screenBrightness = -1;
            }
            PowerManagerService.this.setScreenBrightnessOverrideFromWindowManagerInternal(screenBrightness);
        }

        public void setDozeOverrideFromDreamManager(int screenState, int screenBrightness) {
            switch (screenState) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    break;
                default:
                    screenState = 0;
                    break;
            }
            if (screenBrightness < -1 || screenBrightness > 255) {
                screenBrightness = -1;
            }
            PowerManagerService.this.setDozeOverrideFromDreamManagerInternal(screenState, screenBrightness);
        }

        public void setUserInactiveOverrideFromWindowManager() {
            PowerManagerService.this.setUserInactiveOverrideFromWindowManagerInternal();
        }

        public void setUserActivityTimeoutOverrideFromWindowManager(long timeoutMillis) {
            PowerManagerService.this.setUserActivityTimeoutOverrideFromWindowManagerInternal(timeoutMillis);
        }

        public void setDrawWakeLockOverrideFromSidekick(boolean keepState) {
            PowerManagerService.this.setDrawWakeLockOverrideFromSidekickInternal(keepState);
        }

        public void setMaximumScreenOffTimeoutFromDeviceAdmin(int userId, long timeMs) {
            PowerManagerService.this.setMaximumScreenOffTimeoutFromDeviceAdminInternal(userId, timeMs);
        }

        public PowerSaveState getLowPowerState(int serviceType) {
            return PowerManagerService.this.mBatterySaverPolicy.getBatterySaverPolicy(serviceType, PowerManagerService.this.mBatterySaverController.isEnabled());
        }

        public void registerLowPowerModeObserver(LowPowerModeListener listener) {
            PowerManagerService.this.mBatterySaverController.addListener(listener);
        }

        public boolean setDeviceIdleMode(boolean enabled) {
            return PowerManagerService.this.setDeviceIdleModeInternal(enabled);
        }

        public boolean setLightDeviceIdleMode(boolean enabled) {
            return PowerManagerService.this.setLightDeviceIdleModeInternal(enabled);
        }

        public void setDeviceIdleWhitelist(int[] appids) {
            PowerManagerService.this.setDeviceIdleWhitelistInternal(appids);
        }

        public void setDeviceIdleTempWhitelist(int[] appids) {
            PowerManagerService.this.setDeviceIdleTempWhitelistInternal(appids);
        }

        public void startUidChanges() {
            PowerManagerService.this.startUidChangesInternal();
        }

        public void finishUidChanges() {
            PowerManagerService.this.finishUidChangesInternal();
        }

        public void updateUidProcState(int uid, int procState) {
            PowerManagerService.this.updateUidProcStateInternal(uid, procState);
        }

        public void uidGone(int uid) {
            PowerManagerService.this.uidGoneInternal(uid);
        }

        public void uidActive(int uid) {
            PowerManagerService.this.uidActiveInternal(uid);
        }

        public void uidIdle(int uid) {
            PowerManagerService.this.uidIdleInternal(uid);
        }

        public void powerHint(int hintId, int data) {
            PowerManagerService.this.powerHintInternal(hintId, data);
        }

        public boolean isUserActivityScreenDimOrDream() {
            return ((PowerManagerService.this.mUserActivitySummary & 2) == 0 && (PowerManagerService.this.mUserActivitySummary & 4) == 0) ? false : true;
        }

        public boolean shouldUpdatePCScreenState() {
            boolean z = true;
            if (PowerManagerService.this.mProximityPositive && (PowerManagerService.this.mWakeLockSummary & 16) != 0 && PowerManagerService.this.mWakefulness == 1) {
                z = false;
            }
            return z;
        }
    }

    protected final class PowerManagerHandler extends Handler {
        public PowerManagerHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            switch (i) {
                case 1:
                    PowerManagerService.this.handleUserActivityTimeout();
                    HwFrameworkFactory.getHwApsImpl().StopSdrForSpecial("autosleep", -1);
                    return;
                case 2:
                    PowerManagerService.this.handleSandman();
                    return;
                case 3:
                    PowerManagerService.this.handleScreenBrightnessBoostTimeout();
                    return;
                case 4:
                    PowerManagerService.this.checkForLongWakeLocks();
                    return;
                case 5:
                    PowerManagerService.this.mPolicy.onProximityPositive();
                    return;
                default:
                    switch (i) {
                        case 101:
                            PowerManagerService.this.handleWaitBrightTimeout();
                            return;
                        case 102:
                            PowerManagerService.this.mContext.sendBroadcast(new Intent("wakeup_by_power"), "com.android.keyguard.permission.POWER_KEY_WAKEUP");
                            return;
                        case 103:
                            PowerManagerService.this.registerFaceDetect();
                            return;
                        case 104:
                            String str = PowerManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Face Dectect wakeUpInternal type:");
                            stringBuilder.append(msg.obj);
                            Slog.d(str, stringBuilder.toString());
                            Global.putString(PowerManagerService.this.mContext.getContentResolver(), PowerManagerService.WAKEUP_REASON, msg.obj == null ? "unknow" : msg.obj.toString());
                            return;
                        default:
                            return;
                    }
            }
        }
    }

    private static final class ProfilePowerState {
        long mLastUserActivityTime = SystemClock.uptimeMillis();
        boolean mLockingNotified;
        long mScreenOffTimeout;
        final int mUserId;
        int mWakeLockSummary;

        public ProfilePowerState(int userId, long screenOffTimeout) {
            this.mUserId = userId;
            this.mScreenOffTimeout = screenOffTimeout;
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.handleSettingsChangedLocked();
            }
        }
    }

    static final class UidState {
        boolean mActive;
        int mNumWakeLocks;
        int mProcState;
        final int mUid;

        UidState(int uid) {
            this.mUid = uid;
        }
    }

    private final class UserSwitchedReceiver extends BroadcastReceiver {
        private UserSwitchedReceiver() {
        }

        /* synthetic */ UserSwitchedReceiver(PowerManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            synchronized (PowerManagerService.this.mLock) {
                PowerManagerService.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", 0);
                if (PowerManagerService.DEBUG) {
                    String str = PowerManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("user changed:mCurrentUserId=");
                    stringBuilder.append(PowerManagerService.this.mCurrentUserId);
                    Slog.d(str, stringBuilder.toString());
                }
                PowerManagerService.this.handleSettingsChangedLocked();
            }
            if (PowerManagerService.mSupportFaceDetect) {
                PowerManagerService.this.unregisterFaceDetect();
                PowerManagerService.this.stopIntelliService();
            }
            PowerManagerService.this.setColorTemperatureAccordingToSetting();
        }
    }

    protected final class WakeLock implements DeathRecipient {
        public long mAcquireTime;
        public boolean mDisabled;
        public int mFlags;
        public String mHistoryTag;
        public final IBinder mLock;
        public boolean mNotifiedAcquired;
        public boolean mNotifiedLong;
        public final int mOwnerPid;
        public final int mOwnerUid;
        public final String mPackageName;
        public String mTag;
        public final UidState mUidState;
        public WorkSource mWorkSource;

        public WakeLock(IBinder lock, int flags, String tag, String packageName, WorkSource workSource, String historyTag, int ownerUid, int ownerPid, UidState uidState) {
            this.mLock = lock;
            this.mFlags = flags;
            this.mTag = tag;
            this.mPackageName = packageName;
            this.mWorkSource = PowerManagerService.copyWorkSource(workSource);
            this.mHistoryTag = historyTag;
            this.mOwnerUid = ownerUid;
            this.mOwnerPid = ownerPid;
            this.mUidState = uidState;
        }

        public void binderDied() {
            PowerManagerService.this.handleWakeLockDeath(this);
        }

        public boolean hasSameProperties(int flags, String tag, WorkSource workSource, int ownerUid, int ownerPid) {
            return this.mFlags == flags && this.mTag.equals(tag) && hasSameWorkSource(workSource) && this.mOwnerUid == ownerUid && this.mOwnerPid == ownerPid;
        }

        public void updateProperties(int flags, String tag, String packageName, WorkSource workSource, String historyTag, int ownerUid, int ownerPid) {
            StringBuilder stringBuilder;
            if (!this.mPackageName.equals(packageName)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Existing wake lock package name changed: ");
                stringBuilder.append(this.mPackageName);
                stringBuilder.append(" to ");
                stringBuilder.append(packageName);
                throw new IllegalStateException(stringBuilder.toString());
            } else if (this.mOwnerUid != ownerUid) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Existing wake lock uid changed: ");
                stringBuilder.append(this.mOwnerUid);
                stringBuilder.append(" to ");
                stringBuilder.append(ownerUid);
                throw new IllegalStateException(stringBuilder.toString());
            } else if (this.mOwnerPid == ownerPid) {
                this.mFlags = flags;
                this.mTag = tag;
                updateWorkSource(workSource);
                this.mHistoryTag = historyTag;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Existing wake lock pid changed: ");
                stringBuilder.append(this.mOwnerPid);
                stringBuilder.append(" to ");
                stringBuilder.append(ownerPid);
                throw new IllegalStateException(stringBuilder.toString());
            }
        }

        public boolean hasSameWorkSource(WorkSource workSource) {
            return Objects.equals(this.mWorkSource, workSource);
        }

        public void updateWorkSource(WorkSource workSource) {
            this.mWorkSource = PowerManagerService.copyWorkSource(workSource);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mLock:");
            stringBuilder.append(Objects.hashCode(this.mLock));
            stringBuilder.append(" ");
            sb.append(stringBuilder.toString());
            sb.append(getLockLevelString());
            sb.append(" '");
            sb.append(this.mTag);
            sb.append("'");
            sb.append(getLockFlagsString());
            if (this.mDisabled) {
                sb.append(" DISABLED");
            }
            if (this.mNotifiedAcquired) {
                sb.append(" ACQ=");
                TimeUtils.formatDuration(this.mAcquireTime - SystemClock.uptimeMillis(), sb);
            }
            if (this.mNotifiedLong) {
                sb.append(" LONG");
            }
            sb.append(" (uid=");
            sb.append(this.mOwnerUid);
            if (this.mOwnerPid != 0) {
                sb.append(" pid=");
                sb.append(this.mOwnerPid);
            }
            if (this.mWorkSource != null) {
                sb.append(" ws=");
                sb.append(this.mWorkSource);
            }
            sb.append(")");
            return sb.toString();
        }

        public void writeToProto(ProtoOutputStream proto, long fieldId) {
            long wakeLockToken = proto.start(fieldId);
            proto.write(1159641169921L, this.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI);
            proto.write(1138166333442L, this.mTag);
            long wakeLockFlagsToken = proto.start(1146756268035L);
            boolean z = false;
            proto.write(1133871366145L, (this.mFlags & 268435456) != 0);
            if ((this.mFlags & 536870912) != 0) {
                z = true;
            }
            proto.write(1133871366146L, z);
            proto.end(wakeLockFlagsToken);
            proto.write(1133871366148L, this.mDisabled);
            if (this.mNotifiedAcquired) {
                proto.write(1112396529669L, this.mAcquireTime);
            }
            proto.write(1133871366150L, this.mNotifiedLong);
            proto.write(1120986464263L, this.mOwnerUid);
            proto.write(1120986464264L, this.mOwnerPid);
            if (this.mWorkSource != null) {
                this.mWorkSource.writeToProto(proto, 1146756268041L);
            }
            proto.end(wakeLockToken);
        }

        private String getLockLevelString() {
            int i = this.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI;
            if (i == 1) {
                return "PARTIAL_WAKE_LOCK             ";
            }
            if (i == 6) {
                return "SCREEN_DIM_WAKE_LOCK          ";
            }
            if (i == 10) {
                return "SCREEN_BRIGHT_WAKE_LOCK       ";
            }
            if (i == 26) {
                return "FULL_WAKE_LOCK                ";
            }
            if (i == 32) {
                return "PROXIMITY_SCREEN_OFF_WAKE_LOCK";
            }
            if (i == 64) {
                return "DOZE_WAKE_LOCK                ";
            }
            if (i != 128) {
                return "???                           ";
            }
            return "DRAW_WAKE_LOCK                ";
        }

        private String getLockFlagsString() {
            StringBuilder stringBuilder;
            String result = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            if ((this.mFlags & 268435456) != 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(result);
                stringBuilder.append(" ACQUIRE_CAUSES_WAKEUP");
                result = stringBuilder.toString();
            }
            if ((this.mFlags & 536870912) == 0) {
                return result;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(result);
            stringBuilder.append(" ON_AFTER_RELEASE");
            return stringBuilder.toString();
        }
    }

    private final class SuspendBlockerImpl implements SuspendBlocker {
        private final String mName;
        private int mReferenceCount;
        private final String mTraceName;

        public SuspendBlockerImpl(String name) {
            this.mName = name;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SuspendBlocker (");
            stringBuilder.append(name);
            stringBuilder.append(")");
            this.mTraceName = stringBuilder.toString();
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mReferenceCount != 0) {
                    String str = PowerManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Suspend blocker \"");
                    stringBuilder.append(this.mName);
                    stringBuilder.append("\" was finalized without being released!");
                    Slog.wtf(str, stringBuilder.toString());
                    this.mReferenceCount = 0;
                    PowerManagerService.nativeReleaseSuspendBlocker(this.mName);
                    Trace.asyncTraceEnd(131072, this.mTraceName, 0);
                }
                super.finalize();
            } catch (Throwable th) {
                super.finalize();
            }
        }

        public void acquire() {
            synchronized (this) {
                this.mReferenceCount++;
                if (this.mReferenceCount == 1) {
                    if (PowerManagerService.DEBUG_Controller) {
                        String str = PowerManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Acquiring suspend blocker \"");
                        stringBuilder.append(this.mName);
                        stringBuilder.append("\".");
                        Slog.d(str, stringBuilder.toString());
                    }
                    Trace.asyncTraceBegin(131072, this.mTraceName, 0);
                    PowerManagerService.nativeAcquireSuspendBlocker(this.mName);
                }
            }
        }

        public void release() {
            synchronized (this) {
                this.mReferenceCount--;
                String str;
                StringBuilder stringBuilder;
                if (this.mReferenceCount == 0) {
                    if (PowerManagerService.DEBUG_Controller) {
                        str = PowerManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Releasing suspend blocker \"");
                        stringBuilder.append(this.mName);
                        stringBuilder.append("\".");
                        Slog.d(str, stringBuilder.toString());
                    }
                    PowerManagerService.nativeReleaseSuspendBlocker(this.mName);
                    Trace.asyncTraceEnd(131072, this.mTraceName, 0);
                } else if (this.mReferenceCount < 0) {
                    str = PowerManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Suspend blocker \"");
                    stringBuilder.append(this.mName);
                    stringBuilder.append("\" was released without being acquired!");
                    Slog.wtf(str, stringBuilder.toString(), new Throwable());
                    this.mReferenceCount = 0;
                }
            }
        }

        public String toString() {
            String stringBuilder;
            synchronized (this) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.mName);
                stringBuilder2.append(": ref count=");
                stringBuilder2.append(this.mReferenceCount);
                stringBuilder = stringBuilder2.toString();
            }
            return stringBuilder;
        }

        public void writeToProto(ProtoOutputStream proto, long fieldId) {
            long sbToken = proto.start(fieldId);
            synchronized (this) {
                proto.write(1138166333441L, this.mName);
                proto.write(1120986464258L, this.mReferenceCount);
            }
            proto.end(sbToken);
        }
    }

    private static native void nativeAcquireSuspendBlocker(String str);

    private native void nativeInit();

    private static native void nativeReleaseSuspendBlocker(String str);

    private static native void nativeSendPowerHint(int i, int i2);

    private static native void nativeSetAutoSuspend(boolean z);

    private static native void nativeSetFeature(int i, int i2);

    public static native void nativeSetFsEnable(boolean z);

    private static native void nativeSetInteractive(boolean z);

    /*  JADX ERROR: JadxOverflowException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxOverflowException: Failed compute block dominance frontier
        	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:48)
        	at jadx.core.utils.ErrorsCounter.methodError(ErrorsCounter.java:82)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:29)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private void shutdownOrRebootInternal(int r4, boolean r5, java.lang.String r6, boolean r7) {
        /*
        r3 = this;
        r0 = r3.mHandler;
        if (r0 == 0) goto L_0x0008;
    L_0x0004:
        r0 = r3.mSystemReady;
        if (r0 != 0) goto L_0x0011;
    L_0x0008:
        r0 = com.android.server.RescueParty.isAttemptingFactoryReset();
        if (r0 == 0) goto L_0x0036;
    L_0x000e:
        lowLevelReboot(r6);
    L_0x0011:
        r0 = new com.android.server.power.PowerManagerService$4;
        r0.<init>(r4, r5, r6);
        r1 = com.android.server.UiThread.getHandler();
        r1 = android.os.Message.obtain(r1, r0);
        r2 = 1;
        r1.setAsynchronous(r2);
        r2 = com.android.server.UiThread.getHandler();
        r2.sendMessage(r1);
        if (r7 == 0) goto L_0x0035;
    L_0x002b:
        monitor-enter(r0);
    L_0x002c:
        r0.wait();	 Catch:{ InterruptedException -> 0x0033 }
    L_0x002f:
        goto L_0x002c;
    L_0x0030:
        r2 = move-exception;
        monitor-exit(r0);	 Catch:{ all -> 0x0030 }
        throw r2;
    L_0x0033:
        r2 = move-exception;
        goto L_0x002f;
    L_0x0035:
        return;
    L_0x0036:
        r0 = new java.lang.IllegalStateException;
        r1 = "Too early to call shutdown() or reboot()";
        r0.<init>(r1);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.power.PowerManagerService.shutdownOrRebootInternal(int, boolean, java.lang.String, boolean):void");
    }

    static {
        boolean z = true;
        boolean z2 = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z2;
        z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        DEBUG_Controller = z2;
        z2 = SystemProperties.getInt("ro.config.face_detect", 0) == 1 && SystemProperties.getBoolean("ro.config.face_smart_keepon", true);
        mSupportFaceDetect = z2;
        if (!Log.HWLog && SystemProperties.getBoolean("ro.config.pms_log_filter_enable", true)) {
            z = false;
        }
        DEBUG_ALL = z;
    }

    public PowerManagerService(Context context) {
        super(context);
        this.mLock = LockGuard.installNewLock(1);
        this.mSuspendBlockers = new ArrayList();
        this.mWakeLocks = new ArrayList();
        this.mLastOneSecActivityTime = 0;
        this.mDisplayPowerRequest = new DisplayPowerRequest();
        this.mDockState = 0;
        this.mMaximumScreenOffTimeoutFromDeviceAdmin = JobStatus.NO_LATEST_RUNTIME;
        this.mScreenBrightnessOverrideFromWindowManager = -1;
        this.mOverriddenTimeout = -1;
        this.mUserActivityTimeoutOverrideFromWindowManager = -1;
        this.mTemporaryScreenAutoBrightnessSettingOverride = -1;
        this.mBacklightBrightness = new BacklightBrightness(255, 0, 128);
        this.mDozeScreenStateOverrideFromDreamManager = 0;
        this.mDozeScreenBrightnessOverrideFromDreamManager = -1;
        this.mLastWarningAboutUserActivityPermission = Long.MIN_VALUE;
        this.mDeviceIdleWhitelist = new int[0];
        this.mDeviceIdleTempWhitelist = new int[0];
        this.mUidState = new SparseArray();
        this.mFirstBoot = true;
        this.mAuthSucceeded = false;
        this.mLastSleepTimeDuoToFastFP = 0;
        this.mAdjustTimeNextUserActivity = false;
        this.mUserFirstBoot = true;
        this.mKeyguardManager = null;
        this.mKeyguardLocked = false;
        this.mUpdateBacklightBrightnessFlag = false;
        this.mLowPowerModeListeners = new ArrayList();
        this.mCurrentUserId = 0;
        this.mEyesProtectionMode = 0;
        this.mAlpmState = -1;
        this.mProfilePowerState = new SparseArray();
        this.misBetaUser = false;
        this.mIsPowerInfoStatus = false;
        this.mDropLogs = false;
        this.mDisplayPowerCallbacks = new DisplayPowerCallbacks() {
            private int mDisplayState = 0;

            public void onStateChanged() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService powerManagerService = PowerManagerService.this;
                    powerManagerService.mDirty |= 8;
                    PowerManagerService.this.updatePowerStateLocked();
                }
            }

            public void hwBrightnessOnStateChanged(String what, int arg1, int arg2, Bundle data) {
                PowerManagerService.this.notifyHwBrightnessCallbacks(what, arg1, arg2, data);
            }

            public void onProximityPositive() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.this.mProximityPositive = true;
                    PowerManagerService powerManagerService = PowerManagerService.this;
                    powerManagerService.mDirty |= 512;
                    if (!PowerManagerService.this.isPhoneHeldWakeLock() || HwPCUtils.isPcCastModeInServer()) {
                        Flog.i(NativeResponseCode.SERVICE_LOST, "UL_Power onProximityPositive -> updatePowerStateLocked");
                        PowerManagerService.this.updatePowerStateLocked();
                    } else if (PowerManagerService.this.goToSleepNoUpdateLocked(SystemClock.uptimeMillis(), 100, 0, 1000)) {
                        Flog.i(NativeResponseCode.SERVICE_LOST, "UL_Power onProximityPositivebyPhone -> updatePowerStateLocked");
                        PowerManagerService.this.updatePowerStateLocked();
                    }
                    Message msg = PowerManagerService.this.mHandler.obtainMessage(5);
                    msg.setAsynchronous(true);
                    PowerManagerService.this.mHandler.sendMessage(msg);
                }
            }

            public void onProximityNegative() {
                synchronized (PowerManagerService.this.mLock) {
                    PowerManagerService.this.mProximityPositive = false;
                    PowerManagerService powerManagerService = PowerManagerService.this;
                    powerManagerService.mDirty |= 512;
                    Jlog.d(77, "JL_WAKEUP_REASON_PROX");
                    Flog.i(NativeResponseCode.SERVICE_FOUND, "UL_Power onProximityNegative -> updatePowerStateLocked");
                    PowerManagerService.this.userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 0, 1000);
                    if (PowerManagerService.this.wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), "android.server.power:POWER", 1000, PowerManagerService.this.mContext.getOpPackageName(), 1000) || PowerManagerService.this.wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), "onProximityNegative", 1000, PowerManagerService.this.mContext.getOpPackageName(), 1000) || HwPCUtils.isPcCastModeInServer()) {
                        Flog.i(NativeResponseCode.SERVICE_FOUND, "UL_Power onProximityNegative by Phone");
                        PowerManagerService.this.updatePowerStateLocked();
                    }
                }
            }

            public void onDisplayStateChange(int state) {
                synchronized (PowerManagerService.this.mLock) {
                    if (this.mDisplayState != state) {
                        this.mDisplayState = state;
                        if (state == 1 || (PowerManagerService.mSupportAod && state == 4)) {
                            if (!PowerManagerService.this.mDecoupleHalInteractiveModeFromDisplayConfig) {
                                PowerManagerService.this.setHalInteractiveModeLocked(false);
                            }
                            if (!PowerManagerService.this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
                                PowerManagerService.this.setHalAutoSuspendModeLocked(true);
                            }
                        } else {
                            if (!PowerManagerService.this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
                                PowerManagerService.this.setHalAutoSuspendModeLocked(false);
                            }
                            if (!PowerManagerService.this.mDecoupleHalInteractiveModeFromDisplayConfig) {
                                PowerManagerService.this.setHalInteractiveModeLocked(true);
                            }
                        }
                        if (PowerManagerService.mSupportAod) {
                            PowerManagerService.this.mPolicy.onPowerStateChange(state);
                        }
                    }
                }
            }

            public void acquireSuspendBlocker() {
                PowerManagerService.this.mDisplaySuspendBlocker.acquire();
            }

            public void releaseSuspendBlocker() {
                PowerManagerService.this.mDisplaySuspendBlocker.release();
            }

            public String toString() {
                String stringBuilder;
                synchronized (this) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("state=");
                    stringBuilder2.append(Display.stateToString(this.mDisplayState));
                    stringBuilder = stringBuilder2.toString();
                }
                return stringBuilder;
            }
        };
        this.mVrStateCallbacks = new IVrStateCallbacks.Stub() {
            public void onVrStateChanged(boolean enabled) {
                PowerManagerService.this.powerHintInternal(7, enabled);
                synchronized (PowerManagerService.this.mLock) {
                    if (PowerManagerService.this.mIsVrModeEnabled != enabled) {
                        PowerManagerService.this.setVrModeEnabled(enabled);
                        PowerManagerService powerManagerService = PowerManagerService.this;
                        powerManagerService.mDirty |= 8192;
                        PowerManagerService.this.updatePowerStateLocked();
                    }
                }
            }
        };
        this.inVdriveBackLightMode = false;
        this.mForceDoze = false;
        this.mHwPowerEx = null;
        this.mHwInnerService = new HwInnerPowerManagerService(this);
        this.mPowerProxy = new HwPowerDAMonitorProxy();
        this.mHwPowerEx = HwServiceExFactory.getHwPowerManagerServiceEx(this, context);
        this.mContext = context;
        this.mCust = (HwCustPowerManagerService) HwCustUtils.createObj(HwCustPowerManagerService.class, new Object[]{this.mContext});
        this.mHandlerThread = new ServiceThread(TAG, -4, false);
        this.mHandlerThread.start();
        this.mHandler = new PowerManagerHandler(this.mHandlerThread.getLooper());
        this.mConstants = new Constants(this.mHandler);
        this.mAmbientDisplayConfiguration = new AmbientDisplayConfiguration(this.mContext);
        this.mBatterySavingStats = new BatterySavingStats(this.mLock);
        this.mBatterySaverPolicy = new BatterySaverPolicy(this.mLock, this.mContext, this.mBatterySavingStats);
        this.mBatterySaverController = new BatterySaverController(this.mLock, this.mContext, BackgroundThread.get().getLooper(), this.mBatterySaverPolicy, this.mBatterySavingStats);
        this.mBatterySaverStateMachine = new BatterySaverStateMachine(this.mLock, this.mContext, this.mBatterySaverController);
        synchronized (this.mLock) {
            this.mWakeLockSuspendBlocker = createSuspendBlockerLocked("PowerManagerService.WakeLocks");
            this.mDisplaySuspendBlocker = createSuspendBlockerLocked("PowerManagerService.Display");
            this.mDisplaySuspendBlocker.acquire();
            this.mHoldingDisplaySuspendBlocker = true;
            this.mHalAutoSuspendModeEnabled = false;
            this.mHalInteractiveModeEnabled = true;
            this.mWakefulness = 1;
            sQuiescent = SystemProperties.get(SYSTEM_PROPERTY_QUIESCENT, "0").equals("1");
            nativeInit();
            nativeSetAutoSuspend(false);
            nativeSetInteractive(true);
            nativeSetFeature(1, 0);
        }
    }

    @VisibleForTesting
    PowerManagerService(Context context, BatterySaverPolicy batterySaverPolicy) {
        super(context);
        this.mLock = LockGuard.installNewLock(1);
        this.mSuspendBlockers = new ArrayList();
        this.mWakeLocks = new ArrayList();
        this.mLastOneSecActivityTime = 0;
        this.mDisplayPowerRequest = new DisplayPowerRequest();
        this.mDockState = 0;
        this.mMaximumScreenOffTimeoutFromDeviceAdmin = JobStatus.NO_LATEST_RUNTIME;
        this.mScreenBrightnessOverrideFromWindowManager = -1;
        this.mOverriddenTimeout = -1;
        this.mUserActivityTimeoutOverrideFromWindowManager = -1;
        this.mTemporaryScreenAutoBrightnessSettingOverride = -1;
        this.mBacklightBrightness = new BacklightBrightness(255, 0, 128);
        this.mDozeScreenStateOverrideFromDreamManager = 0;
        this.mDozeScreenBrightnessOverrideFromDreamManager = -1;
        this.mLastWarningAboutUserActivityPermission = Long.MIN_VALUE;
        this.mDeviceIdleWhitelist = new int[0];
        this.mDeviceIdleTempWhitelist = new int[0];
        this.mUidState = new SparseArray();
        this.mFirstBoot = true;
        this.mAuthSucceeded = false;
        this.mLastSleepTimeDuoToFastFP = 0;
        this.mAdjustTimeNextUserActivity = false;
        this.mUserFirstBoot = true;
        this.mKeyguardManager = null;
        this.mKeyguardLocked = false;
        this.mUpdateBacklightBrightnessFlag = false;
        this.mLowPowerModeListeners = new ArrayList();
        this.mCurrentUserId = 0;
        this.mEyesProtectionMode = 0;
        this.mAlpmState = -1;
        this.mProfilePowerState = new SparseArray();
        this.misBetaUser = false;
        this.mIsPowerInfoStatus = false;
        this.mDropLogs = false;
        this.mDisplayPowerCallbacks = /* anonymous class already generated */;
        this.mVrStateCallbacks = /* anonymous class already generated */;
        this.inVdriveBackLightMode = false;
        this.mForceDoze = false;
        this.mHwPowerEx = null;
        this.mHwInnerService = new HwInnerPowerManagerService(this);
        this.mPowerProxy = new HwPowerDAMonitorProxy();
        this.mContext = context;
        this.mHandlerThread = new ServiceThread(TAG, -4, false);
        this.mHandlerThread.start();
        this.mHandler = new PowerManagerHandler(this.mHandlerThread.getLooper());
        this.mConstants = new Constants(this.mHandler);
        this.mAmbientDisplayConfiguration = new AmbientDisplayConfiguration(this.mContext);
        this.mDisplaySuspendBlocker = null;
        this.mWakeLockSuspendBlocker = null;
        this.mBatterySavingStats = new BatterySavingStats(this.mLock);
        this.mBatterySaverPolicy = batterySaverPolicy;
        this.mBatterySaverController = new BatterySaverController(this.mLock, context, BackgroundThread.getHandler().getLooper(), batterySaverPolicy, this.mBatterySavingStats);
        this.mBatterySaverStateMachine = new BatterySaverStateMachine(this.mLock, this.mContext, this.mBatterySaverController);
    }

    public void onStart() {
        publishBinderService("power", new BinderService(this, null));
        publishLocalService(PowerManagerInternal.class, new LocalService(this, null));
        Watchdog.getInstance().addMonitor(this);
        Watchdog.getInstance().addThread(this.mHandler);
    }

    public void onBootPhase(int phase) {
        synchronized (this.mLock) {
            if (phase == 600) {
                incrementBootCount();
            } else if (phase == 1000) {
                long now = SystemClock.uptimeMillis();
                this.mBootCompleted = true;
                this.mDirty |= 16;
                this.mBatterySaverStateMachine.onBootCompleted();
                userActivityNoUpdateLocked(now, 0, 0, 1000);
                updatePowerStateLocked();
                postAfterBootCompleted(new Runnable() {
                    public void run() {
                        PowerManagerService.this.sendBootCompletedToMonitor();
                    }
                });
                if (!ArrayUtils.isEmpty(this.mBootCompletedRunnables)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Posting ");
                    stringBuilder.append(this.mBootCompletedRunnables.length);
                    stringBuilder.append(" delayed runnables");
                    Slog.d(str, stringBuilder.toString());
                    for (Runnable r : this.mBootCompletedRunnables) {
                        BackgroundThread.getHandler().post(r);
                    }
                }
                this.mBootCompletedRunnables = null;
            }
        }
    }

    public void systemReady(IAppOpsService appOps) {
        synchronized (this.mLock) {
            this.mSystemReady = true;
            this.mAppOps = appOps;
            this.mDreamManager = (DreamManagerInternal) getLocalService(DreamManagerInternal.class);
            this.mDisplayManagerInternal = (DisplayManagerInternal) getLocalService(DisplayManagerInternal.class);
            this.mPolicy = (WindowManagerPolicy) getLocalService(WindowManagerPolicy.class);
            this.mBatteryManagerInternal = (BatteryManagerInternal) getLocalService(BatteryManagerInternal.class);
            PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
            this.mScreenBrightnessSettingMinimum = pm.getMinimumScreenBrightnessSetting();
            this.mScreenBrightnessSettingMaximum = pm.getMaximumScreenBrightnessSetting();
            this.mScreenBrightnessSettingDefault = pm.getDefaultScreenBrightnessSetting();
            SensorManager sensorManager = new SystemSensorManager(this.mContext, this.mHandler.getLooper());
            this.mBatteryStats = BatteryStatsService.getService();
            this.mNotifier = new Notifier(Looper.getMainLooper(), this.mContext, this.mBatteryStats, createSuspendBlockerLocked("PowerManagerService.Broadcasts"), this.mPolicy);
            this.mWirelessChargerDetector = new WirelessChargerDetector(sensorManager, createSuspendBlockerLocked("PowerManagerService.WirelessChargerDetector"), this.mHandler);
            this.mSettingsObserver = new SettingsObserver(this.mHandler);
            this.mLightsManager = (LightsManager) getLocalService(LightsManager.class);
            this.mAttentionLight = this.mLightsManager.getLight(5);
            this.mBackLight = this.mLightsManager.getLight(0);
            this.mInputManagerInternal = (InputManagerInternal) getLocalService(InputManagerInternal.class);
            this.mDisplayManagerInternal.initPowerManagement(this.mDisplayPowerCallbacks, this.mHandler, sensorManager);
            try {
                ActivityManager.getService().registerUserSwitchObserver(new ForegroundProfileObserver(this, null), TAG);
            } catch (RemoteException e) {
            }
            readConfigurationLocked();
            updateSettingsLocked();
            this.mDirty |= 256;
            updatePowerStateLocked();
        }
        ContentResolver resolver = this.mContext.getContentResolver();
        this.mConstants.start(resolver);
        this.mBatterySaverController.systemReady();
        this.mBatterySaverPolicy.systemReady();
        resolver.registerContentObserver(Secure.getUriFor("screensaver_enabled"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Secure.getUriFor("screensaver_activate_on_sleep"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Secure.getUriFor("screensaver_activate_on_dock"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(System.getUriFor("screen_off_timeout"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Secure.getUriFor("sleep_timeout"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Global.getUriFor("stay_on_while_plugged_in"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(System.getUriFor("screen_brightness_mode"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(System.getUriFor("screen_auto_brightness_adj"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Global.getUriFor("theater_mode_on"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Secure.getUriFor("doze_always_on"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Secure.getUriFor("double_tap_to_wake"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(Global.getUriFor("device_demo_mode"), false, this.mSettingsObserver, 0);
        resolver.registerContentObserver(System.getUriFor("smart_backlight_enable"), false, this.mSettingsObserver, -1);
        resolver.registerContentObserver(System.getUriFor(KEY_EYES_PROTECTION), false, this.mSettingsObserver, -2);
        if (this.mCust != null) {
            this.mCust.systemReady(this.mBatteryManagerInternal, this.mDreamManager, this.mSettingsObserver);
        }
        IVrManager vrManager = (IVrManager) getBinderService("vrmanager");
        if (vrManager != null) {
            try {
                vrManager.registerListener(this.mVrStateCallbacks);
            } catch (RemoteException e2) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to register VR mode state listener: ");
                stringBuilder.append(e2);
                Slog.e(str, stringBuilder.toString());
            }
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.setPriority(1000);
        this.mContext.registerReceiver(new BatteryReceiver(this, null), filter, null, this.mHandler);
        filter = new IntentFilter();
        filter.addAction("android.intent.action.DREAMING_STARTED");
        filter.addAction("android.intent.action.DREAMING_STOPPED");
        this.mContext.registerReceiver(new DreamReceiver(this, null), filter, null, this.mHandler);
        filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(new UserSwitchedReceiver(this, null), filter, null, this.mHandler);
        filter = new IntentFilter();
        filter.addAction("android.intent.action.DOCK_EVENT");
        this.mContext.registerReceiver(new DockReceiver(this, null), filter, null, this.mHandler);
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
    }

    private void readConfigurationLocked() {
        Resources resources = this.mContext.getResources();
        this.mDecoupleHalAutoSuspendModeFromDisplayConfig = resources.getBoolean(17957002);
        this.mDecoupleHalInteractiveModeFromDisplayConfig = resources.getBoolean(17957003);
        this.mWakeUpWhenPluggedOrUnpluggedConfig = resources.getBoolean(17957054);
        this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig = resources.getBoolean(17956885);
        this.mSuspendWhenScreenOffDueToProximityConfig = resources.getBoolean(17957044);
        this.mDreamsSupportedConfig = resources.getBoolean(17956943);
        this.mDreamsEnabledByDefaultConfig = resources.getBoolean(17956941);
        this.mDreamsActivatedOnSleepByDefaultConfig = resources.getBoolean(17956940);
        this.mDreamsActivatedOnDockByDefaultConfig = resources.getBoolean(17956939);
        this.mDreamsEnabledOnBatteryConfig = resources.getBoolean(17956942);
        this.mDreamsBatteryLevelMinimumWhenPoweredConfig = resources.getInteger(17694785);
        this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig = resources.getInteger(17694784);
        this.mDreamsBatteryLevelDrainCutoffConfig = resources.getInteger(17694783);
        this.mDozeAfterScreenOff = resources.getBoolean(17956936);
        this.mMinimumScreenOffTimeoutConfig = (long) resources.getInteger(17694819);
        this.mMaximumScreenDimDurationConfig = (long) resources.getInteger(17694814);
        this.mMaximumScreenDimRatioConfig = resources.getFraction(18022402, 1, 1);
        this.mSupportsDoubleTapWakeConfig = resources.getBoolean(17957035);
        if (this.mCust != null) {
            this.mDreamsSupportedConfig = this.mCust.readConfigurationLocked(this.mDreamsSupportedConfig);
        }
    }

    private void updateSettingsLocked() {
        String str;
        StringBuilder stringBuilder;
        String str2;
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean z = true;
        this.mDreamsEnabledSetting = Secure.getIntForUser(resolver, "screensaver_enabled", this.mDreamsEnabledByDefaultConfig, -2) != 0;
        this.mDreamsActivateOnSleepSetting = Secure.getIntForUser(resolver, "screensaver_activate_on_sleep", this.mDreamsActivatedOnSleepByDefaultConfig, -2) != 0;
        this.mDreamsActivateOnDockSetting = Secure.getIntForUser(resolver, "screensaver_activate_on_dock", this.mDreamsActivatedOnDockByDefaultConfig, -2) != 0;
        this.mScreenOffTimeoutSetting = (long) System.getIntForUser(resolver, "screen_off_timeout", 15000, -2);
        this.mSleepTimeoutSetting = (long) Secure.getIntForUser(resolver, "sleep_timeout", -1, -2);
        this.mStayOnWhilePluggedInSetting = Global.getInt(resolver, "stay_on_while_plugged_in", 3);
        if (this.mCust != null) {
            this.mCust.updateSettingsLocked();
        }
        this.mTheaterModeEnabled = Global.getInt(this.mContext.getContentResolver(), "theater_mode_on", 0) == 1;
        this.mAlwaysOnEnabled = this.mAmbientDisplayConfiguration.alwaysOnEnabled(-2);
        if (this.mSupportsDoubleTapWakeConfig) {
            boolean doubleTapWakeEnabled = Secure.getIntForUser(resolver, "double_tap_to_wake", 0, -2) != 0;
            if (doubleTapWakeEnabled != this.mDoubleTapWakeEnabled) {
                this.mDoubleTapWakeEnabled = doubleTapWakeEnabled;
                nativeSetFeature(1, this.mDoubleTapWakeEnabled);
            }
        }
        String retailDemoValue = UserManager.isDeviceInDemoMode(this.mContext) ? "1" : "0";
        if (!retailDemoValue.equals(SystemProperties.get(SYSTEM_PROPERTY_RETAIL_DEMO_ENABLED))) {
            SystemProperties.set(SYSTEM_PROPERTY_RETAIL_DEMO_ENABLED, retailDemoValue);
        }
        int oldScreenBrightnessSetting = this.mScreenBrightnessSetting;
        this.mScreenBrightnessSetting = System.getIntForUser(resolver, "screen_brightness", this.mScreenBrightnessSettingDefault, this.mCurrentUserId);
        if (oldScreenBrightnessSetting != this.mScreenBrightnessSetting) {
            if (DEBUG) {
                String str3 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mScreenBrightnessSetting=");
                stringBuilder2.append(this.mScreenBrightnessSetting);
                stringBuilder2.append(",userid=");
                stringBuilder2.append(this.mCurrentUserId);
                Slog.d(str3, stringBuilder2.toString());
            }
            sendManualBrightnessToMonitor(this.mScreenBrightnessSetting, PackageManagerService.PLATFORM_PACKAGE_NAME);
        }
        int oldScreenBrightnessModeSetting = this.mScreenBrightnessModeSetting;
        if (this.mFirstBoot && getRebootAutoModeEnable()) {
            int autoBrightnessMode = System.getIntForUser(resolver, "screen_brightness_mode", 0, this.mCurrentUserId);
            if (autoBrightnessMode == 0) {
                System.putIntForUser(resolver, "screen_brightness_mode", 1, this.mCurrentUserId);
                System.putIntForUser(resolver, "hw_screen_brightness_mode_value", 1, this.mCurrentUserId);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("RebootAutoMode, set autoBrightnessMode=1, origMode=");
                stringBuilder.append(autoBrightnessMode);
                stringBuilder.append(",mScreenBrightnessModeSetting=");
                stringBuilder.append(this.mScreenBrightnessModeSetting);
                Slog.i(str, stringBuilder.toString());
            }
            this.mScreenBrightnessModeSetting = 1;
            this.mFirstBoot = false;
        } else {
            this.mScreenBrightnessModeSetting = System.getIntForUser(resolver, "screen_brightness_mode", 0, this.mCurrentUserId);
        }
        if (oldScreenBrightnessModeSetting != this.mScreenBrightnessModeSetting) {
            if (DEBUG) {
                str2 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("mScreenBrightnessModeSetting=");
                stringBuilder3.append(this.mScreenBrightnessModeSetting);
                stringBuilder3.append(",userid=");
                stringBuilder3.append(this.mCurrentUserId);
                Slog.d(str2, stringBuilder3.toString());
            }
            str2 = PackageManagerService.PLATFORM_PACKAGE_NAME;
            if (this.mScreenBrightnessModeSetting != 0) {
                z = false;
            }
            sendBrightnessModeToMonitor(z, str2);
        }
        if (this.mUserFirstBoot) {
            if (System.getStringForUser(resolver, "hw_screen_brightness_mode_value", -2) == null) {
                str2 = System.getStringForUser(resolver, "screen_brightness_mode", -2);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Firstboot get SCREEN_BRIGHTNESS_MODE=");
                stringBuilder.append(str2);
                Slog.i(str, stringBuilder.toString());
                System.putStringForUser(resolver, "hw_screen_brightness_mode_value", str2, -2);
            }
            this.mUserFirstBoot = false;
        }
        this.mSmartBacklightEnableSetting = System.getIntForUser(resolver, "smart_backlight_enable", 0, 0);
        this.mEyesProtectionMode = System.getIntForUser(this.mContext.getContentResolver(), KEY_EYES_PROTECTION, 0, -2);
        this.mDirty |= 32;
    }

    private void postAfterBootCompleted(Runnable r) {
        if (this.mBootCompleted) {
            BackgroundThread.getHandler().post(r);
            return;
        }
        Slog.d(TAG, "Delaying runnable until system is booted");
        this.mBootCompletedRunnables = (Runnable[]) ArrayUtils.appendElement(Runnable.class, this.mBootCompletedRunnables, r);
    }

    private void handleSettingsChangedLocked() {
        updateSettingsLocked();
        updatePowerStateLocked();
    }

    private boolean shouldDropLogs(String tag, String packageName, int uid) {
        if (DEBUG_ALL) {
            return false;
        }
        HashSet hashSet = LOG_DROP_SET;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append(uid);
        if (hashSet.contains(stringBuilder.toString())) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:37:0x00d1 A:{Catch:{ all -> 0x00c9, all -> 0x0141 }} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0084 A:{SYNTHETIC, Splitter: B:26:0x0084} */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x012e A:{Catch:{ RemoteException -> 0x0133, all -> 0x014a }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void acquireWakeLockInternal(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag, int uid, int pid) {
        Throwable th;
        String str = tag;
        String str2 = packageName;
        int i = uid;
        if (this.mSystemReady) {
            Object obj = this.mLock;
            synchronized (obj) {
                int i2;
                Object obj2;
                IBinder iBinder;
                int i3;
                try {
                    WorkSource workSource;
                    int index;
                    WakeLock wakeLock;
                    WakeLock wakeLock2;
                    if (DEBUG_SPEW) {
                        try {
                            boolean shouldDropLogs = shouldDropLogs(str, str2, i);
                            this.mDropLogs = shouldDropLogs;
                            if (!shouldDropLogs) {
                                String str3 = TAG_PowerMS;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("acquire:L=");
                                stringBuilder.append(Objects.hashCode(lock));
                                stringBuilder.append(",F=0x");
                                stringBuilder.append(Integer.toHexString(flags));
                                stringBuilder.append(",T=\"");
                                stringBuilder.append(str);
                                stringBuilder.append("\",N=");
                                stringBuilder.append(str2);
                                stringBuilder.append(",WS=");
                                workSource = ws;
                                stringBuilder.append(workSource);
                                stringBuilder.append(",U=");
                                stringBuilder.append(i);
                                stringBuilder.append(",P=");
                                i2 = pid;
                                stringBuilder.append(i2);
                                Slog.d(str3, stringBuilder.toString());
                                index = findWakeLockIndexLocked(lock);
                                if (index < 0) {
                                    try {
                                        WakeLock wakeLock3 = (WakeLock) this.mWakeLocks.get(index);
                                        if (wakeLock3.hasSameProperties(flags, str, workSource, i, i2)) {
                                            obj2 = obj;
                                        } else {
                                            obj2 = obj;
                                            notifyWakeLockChangingLocked(wakeLock3, flags, str, str2, i, pid, ws, historyTag);
                                            wakeLock3.updateProperties(flags, str, str2, ws, historyTag, uid, pid);
                                        }
                                        wakeLock = null;
                                        iBinder = lock;
                                        wakeLock2 = wakeLock3;
                                        i3 = uid;
                                    } catch (Throwable th2) {
                                        th = th2;
                                        iBinder = lock;
                                        i3 = uid;
                                        throw th;
                                    }
                                }
                                int i4 = index;
                                obj2 = obj;
                                i3 = uid;
                                try {
                                    UidState state = (UidState) this.mUidState.get(i3);
                                    if (state == null) {
                                        state = new UidState(i3);
                                        state.mProcState = 19;
                                        this.mUidState.put(i3, state);
                                    }
                                    UidState state2 = state;
                                    state2.mNumWakeLocks++;
                                    wakeLock = new WakeLock(lock, flags, str, str2, ws, historyTag, i3, pid, state2);
                                } catch (Throwable th3) {
                                    th = th3;
                                    iBinder = lock;
                                    throw th;
                                }
                                try {
                                    lock.linkToDeath(wakeLock, 0);
                                    this.mWakeLocks.add(wakeLock);
                                    setWakeLockDisabledStateLocked(wakeLock);
                                    wakeLock2 = wakeLock;
                                    wakeLock = 1;
                                } catch (RemoteException ex) {
                                    RemoteException remoteException = ex;
                                    throw new IllegalArgumentException("Wake lock is already dead.");
                                } catch (Throwable th4) {
                                    th = th4;
                                    throw th;
                                }
                                applyWakeLockFlagsOnAcquireLocked(wakeLock2, i3);
                                this.mDirty = 1 | this.mDirty;
                                updatePowerStateLocked();
                                if (wakeLock != null) {
                                    notifyWakeLockAcquiredLocked(wakeLock2);
                                }
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            workSource = ws;
                            i2 = pid;
                            iBinder = lock;
                            obj2 = obj;
                            i3 = i;
                            throw th;
                        }
                    }
                    workSource = ws;
                    i2 = pid;
                    index = findWakeLockIndexLocked(lock);
                    if (index < 0) {
                    }
                    applyWakeLockFlagsOnAcquireLocked(wakeLock2, i3);
                    this.mDirty = 1 | this.mDirty;
                    updatePowerStateLocked();
                    if (wakeLock != null) {
                    }
                } catch (Throwable th6) {
                    th = th6;
                    iBinder = lock;
                    obj2 = obj;
                    i3 = i;
                    throw th;
                }
            }
        }
    }

    private static boolean isScreenLock(WakeLock wakeLock) {
        int i = wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI;
        if (i == 6 || i == 10 || i == 26) {
            return true;
        }
        return false;
    }

    protected void applyWakeLockFlagsOnAcquireLocked(WakeLock wakeLock, int uid) {
        if ((wakeLock.mFlags & 268435456) != 0 && isScreenLock(wakeLock)) {
            String opPackageName;
            int i;
            if (wakeLock.mWorkSource == null || wakeLock.mWorkSource.getName(0) == null) {
                opPackageName = wakeLock.mPackageName;
                if (wakeLock.mWorkSource != null) {
                    i = wakeLock.mWorkSource.get(0);
                } else {
                    i = wakeLock.mOwnerUid;
                }
            } else {
                opPackageName = wakeLock.mWorkSource.getName(0);
                i = wakeLock.mWorkSource.get(0);
            }
            int opUid = i;
            wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), wakeLock.mTag, opUid, opPackageName, opUid);
        }
    }

    /* JADX WARNING: Missing block: B:12:0x003a, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void releaseWakeLockInternal(IBinder lock, int flags) {
        if (this.mSystemReady) {
            synchronized (this.mLock) {
                int index = findWakeLockIndexLocked(lock);
                if (index >= 0) {
                    WakeLock wakeLock = (WakeLock) this.mWakeLocks.get(index);
                    if (DEBUG_SPEW) {
                        boolean shouldDropLogs = shouldDropLogs(wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid);
                        this.mDropLogs = shouldDropLogs;
                        if (!shouldDropLogs) {
                            String str = TAG_PowerMS;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("release:L=");
                            stringBuilder.append(Objects.hashCode(lock));
                            stringBuilder.append(",F=0x");
                            stringBuilder.append(Integer.toHexString(flags));
                            stringBuilder.append(",T=\"");
                            stringBuilder.append(wakeLock.mTag);
                            stringBuilder.append("\",N=");
                            stringBuilder.append(wakeLock.mPackageName);
                            stringBuilder.append("\",WS=");
                            stringBuilder.append(wakeLock.mWorkSource);
                            stringBuilder.append(",U=");
                            stringBuilder.append(wakeLock.mOwnerUid);
                            stringBuilder.append(",P=");
                            stringBuilder.append(wakeLock.mOwnerPid);
                            Slog.d(str, stringBuilder.toString());
                        }
                    }
                    if ((flags & 1) != 0) {
                        this.mRequestWaitForNegativeProximity = true;
                    }
                    wakeLock.mLock.unlinkToDeath(wakeLock, 0);
                    removeWakeLockLocked(wakeLock, index);
                } else if (DEBUG_SPEW) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("releaseWakeLockInternal: lock=");
                    stringBuilder2.append(Objects.hashCode(lock));
                    stringBuilder2.append(" [not found], flags=0x");
                    stringBuilder2.append(Integer.toHexString(flags));
                    Slog.d(str2, stringBuilder2.toString());
                }
            }
        }
    }

    public void handleWakeLockDeath(WakeLock wakeLock) {
        synchronized (this.mLock) {
            if (DEBUG_SPEW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleWakeLockDeath: lock=");
                stringBuilder.append(Objects.hashCode(wakeLock.mLock));
                stringBuilder.append(" [");
                stringBuilder.append(wakeLock.mTag);
                stringBuilder.append("]");
                Slog.d(str, stringBuilder.toString());
            }
            int index = this.mWakeLocks.indexOf(wakeLock);
            if (index < 0) {
                return;
            }
            removeWakeLockLocked(wakeLock, index);
        }
    }

    protected void removeWakeLockLocked(WakeLock wakeLock, int index) {
        this.mWakeLocks.remove(index);
        UidState state = wakeLock.mUidState;
        state.mNumWakeLocks--;
        if (state.mNumWakeLocks <= 0 && state.mProcState == 19) {
            this.mUidState.remove(state.mUid);
        }
        notifyWakeLockReleasedLocked(wakeLock);
        applyWakeLockFlagsOnReleaseLocked(wakeLock);
        this.mDirty |= 1;
        updatePowerStateLocked();
    }

    private void applyWakeLockFlagsOnReleaseLocked(WakeLock wakeLock) {
        if ((wakeLock.mFlags & 536870912) != 0 && isScreenLock(wakeLock)) {
            userActivityNoUpdateLocked(SystemClock.uptimeMillis(), 0, 1, wakeLock.mOwnerUid);
        }
    }

    /* JADX WARNING: Missing block: B:39:0x00ea, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateWakeLockWorkSourceInternal(IBinder lock, WorkSource ws, String historyTag, int callingUid) {
        Throwable th;
        int i;
        IBinder iBinder;
        WorkSource workSource = ws;
        synchronized (this.mLock) {
            String str;
            try {
                int index = findWakeLockIndexLocked(lock);
                StringBuilder stringBuilder;
                if (index < 0) {
                    try {
                        if (DEBUG_Controller) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("updateWakeLockWorkSourceInternal: lock=");
                            stringBuilder.append(Objects.hashCode(lock));
                            stringBuilder.append(" [not found], ws=");
                            stringBuilder.append(workSource);
                            Slog.d(str, stringBuilder.toString());
                        }
                        if (workSource == null) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("updateWakeLockWorkSourceInternal: lock=");
                            stringBuilder.append(Objects.hashCode(lock));
                            stringBuilder.append(" [not found], ws=");
                            stringBuilder.append(workSource);
                            Slog.e(str, stringBuilder.toString());
                            return;
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Wake lock not active: ");
                        try {
                            stringBuilder.append(lock);
                            stringBuilder.append(" from uid ");
                            stringBuilder.append(callingUid);
                            throw new IllegalArgumentException(stringBuilder.toString());
                        } catch (Throwable th2) {
                            th = th2;
                            str = historyTag;
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        iBinder = lock;
                        i = callingUid;
                        str = historyTag;
                        throw th;
                    }
                }
                iBinder = lock;
                i = callingUid;
                WakeLock wakeLock = (WakeLock) this.mWakeLocks.get(index);
                if (DEBUG_Controller) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("updateWakeLockWorkSourceInternal: lock=");
                    stringBuilder.append(Objects.hashCode(lock));
                    stringBuilder.append(" [");
                    stringBuilder.append(wakeLock.mTag);
                    stringBuilder.append("], ws=");
                    stringBuilder.append(workSource);
                    Slog.d(str, stringBuilder.toString());
                }
                if (wakeLock.hasSameWorkSource(workSource)) {
                    str = historyTag;
                } else {
                    notifyWakeLockChangingLocked(wakeLock, wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, workSource, historyTag);
                    wakeLock.mHistoryTag = historyTag;
                    wakeLock.updateWorkSource(workSource);
                }
            } catch (Throwable th4) {
                th = th4;
                throw th;
            }
        }
    }

    protected int findWakeLockIndexLocked(IBinder lock) {
        int count = this.mWakeLocks.size();
        for (int i = 0; i < count; i++) {
            if (((WakeLock) this.mWakeLocks.get(i)).mLock == lock) {
                return i;
            }
        }
        return -1;
    }

    protected void notifyWakeLockAcquiredLocked(WakeLock wakeLock) {
        if (this.mSystemReady && !wakeLock.mDisabled) {
            wakeLock.mNotifiedAcquired = true;
            this.mNotifier.onWakeLockAcquired(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
            restartNofifyLongTimerLocked(wakeLock);
            if ((wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 26 || (wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 10) {
                notifyWakeLockToIAware(wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mPackageName, wakeLock.mTag);
            }
        }
    }

    private void enqueueNotifyLongMsgLocked(long time) {
        this.mNotifyLongScheduled = time;
        Message msg = this.mHandler.obtainMessage(4);
        msg.setAsynchronous(true);
        this.mHandler.sendMessageAtTime(msg, time);
    }

    private void restartNofifyLongTimerLocked(WakeLock wakeLock) {
        wakeLock.mAcquireTime = SystemClock.uptimeMillis();
        if ((wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 1 && this.mNotifyLongScheduled == 0) {
            enqueueNotifyLongMsgLocked(wakeLock.mAcquireTime + 60000);
        }
    }

    private void notifyWakeLockLongStartedLocked(WakeLock wakeLock) {
        if (this.mSystemReady && !wakeLock.mDisabled) {
            wakeLock.mNotifiedLong = true;
            this.mNotifier.onLongPartialWakeLockStart(wakeLock.mTag, wakeLock.mOwnerUid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
        }
    }

    private void notifyWakeLockLongFinishedLocked(WakeLock wakeLock) {
        if (wakeLock.mNotifiedLong) {
            wakeLock.mNotifiedLong = false;
            this.mNotifier.onLongPartialWakeLockFinish(wakeLock.mTag, wakeLock.mOwnerUid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
        }
    }

    protected void notifyWakeLockChangingLocked(WakeLock wakeLock, int flags, String tag, String packageName, int uid, int pid, WorkSource ws, String historyTag) {
        WakeLock wakeLock2 = wakeLock;
        if (this.mSystemReady && wakeLock2.mNotifiedAcquired) {
            this.mNotifier.onWakeLockChanging(wakeLock2.mFlags, wakeLock2.mTag, wakeLock2.mPackageName, wakeLock2.mOwnerUid, wakeLock2.mOwnerPid, wakeLock2.mWorkSource, wakeLock2.mHistoryTag, flags, tag, packageName, uid, pid, ws, historyTag);
            notifyWakeLockLongFinishedLocked(wakeLock);
            restartNofifyLongTimerLocked(wakeLock);
        }
    }

    private void notifyWakeLockReleasedLocked(WakeLock wakeLock) {
        if (this.mSystemReady && wakeLock.mNotifiedAcquired) {
            wakeLock.mNotifiedAcquired = false;
            wakeLock.mAcquireTime = 0;
            this.mNotifier.onWakeLockReleased(wakeLock.mFlags, wakeLock.mTag, wakeLock.mPackageName, wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mWorkSource, wakeLock.mHistoryTag);
            notifyWakeLockLongFinishedLocked(wakeLock);
            if ((wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 26 || (wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 10) {
                notifyWakeLockReleaseToIAware(wakeLock.mOwnerUid, wakeLock.mOwnerPid, wakeLock.mPackageName, wakeLock.mTag);
            }
        }
    }

    /* JADX WARNING: Missing block: B:26:0x0031, code:
            return r1;
     */
    /* JADX WARNING: Missing block: B:28:0x0033, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isWakeLockLevelSupportedInternal(int level) {
        synchronized (this.mLock) {
            boolean z = true;
            if (!(level == 1 || level == 6 || level == 10 || level == 26)) {
                if (level != 32) {
                    if (!(level == 64 || level == 128)) {
                        return false;
                    }
                } else if (!(this.mSystemReady && this.mDisplayManagerInternal.isProximitySensorAvailable())) {
                    z = false;
                }
            }
        }
    }

    private void userActivityFromNative(long eventTime, int event, int flags) {
        userActivityInternal(eventTime, event, flags, 1000);
    }

    protected void userActivityInternal(long eventTime, int event, int flags, int uid) {
        synchronized (this.mLock) {
            if (userActivityNoUpdateLocked(eventTime, event, flags, uid)) {
                updatePowerStateLocked();
            }
        }
    }

    protected boolean userActivityNoUpdateLocked(long eventTime, int event, int flags, int uid) {
        if (DEBUG_SPEW && eventTime - this.mLastOneSecActivityTime >= 1000) {
            this.mLastOneSecActivityTime = eventTime;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("userActivity:eventTime=");
            stringBuilder.append(eventTime);
            stringBuilder.append(",event=");
            stringBuilder.append(event);
            stringBuilder.append(",flags=0x");
            stringBuilder.append(Integer.toHexString(flags));
            stringBuilder.append(",uid=");
            stringBuilder.append(uid);
            Slog.d(str, stringBuilder.toString());
        }
        if ((eventTime < this.mLastSleepTime && !this.mAdjustTimeNextUserActivity) || eventTime < this.mLastWakeTime || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        if (this.mAdjustTimeNextUserActivity) {
            this.mAdjustTimeNextUserActivity = false;
        }
        Trace.traceBegin(131072, "userActivity");
        try {
            if (eventTime > this.mLastInteractivePowerHintTime) {
                powerHintInternal(2, 0);
                this.mLastInteractivePowerHintTime = eventTime;
            }
            this.mNotifier.onUserActivity(event, uid);
            if (this.mUserInactiveOverrideFromWindowManager) {
                this.mUserInactiveOverrideFromWindowManager = false;
                this.mOverriddenTimeout = -1;
            }
            if (this.mWakefulness == 0 || this.mWakefulness == 3 || (flags & 2) != 0) {
                Trace.traceEnd(131072);
                return false;
            }
            maybeUpdateForegroundProfileLastActivityLocked(eventTime);
            if ((flags & 1) != 0) {
                if (eventTime > this.mLastUserActivityTimeNoChangeLights && eventTime > this.mLastUserActivityTime) {
                    if (mSupportFaceDetect) {
                        unregisterFaceDetect();
                    }
                    this.mLastUserActivityTimeNoChangeLights = eventTime;
                    this.mDirty |= 4;
                    if (event == 1) {
                        this.mDirty |= 4096;
                    }
                    Trace.traceEnd(131072);
                    return true;
                }
            } else if (eventTime > this.mLastUserActivityTime) {
                if (mSupportFaceDetect) {
                    unregisterFaceDetect();
                }
                this.mLastUserActivityTime = eventTime;
                this.mDirty |= 4;
                if (event == 1) {
                    this.mDirty |= 4096;
                }
                Trace.traceEnd(131072);
                return true;
            }
            Trace.traceEnd(131072);
            return false;
        } catch (Throwable th) {
            Trace.traceEnd(131072);
        }
    }

    private void maybeUpdateForegroundProfileLastActivityLocked(long eventTime) {
        ProfilePowerState profile = (ProfilePowerState) this.mProfilePowerState.get(this.mForegroundProfile);
        if (profile != null && eventTime > profile.mLastUserActivityTime) {
            profile.mLastUserActivityTime = eventTime;
        }
    }

    private void wakeUpInternal(long eventTime, String reason, int uid, String opPackageName, int opUid) {
        synchronized (this.mLock) {
            if (Jlog.isPerfTest()) {
                Jlog.i(2203, "JL_PWRSCRON_PMS_WAKEUPINTERNAL");
            }
            if (wakeUpNoUpdateLocked(eventTime, reason, uid, opPackageName, opUid)) {
                updatePowerStateLocked();
            }
        }
    }

    protected boolean wakeUpNoUpdateLocked(long eventTime, String reason, int reasonUid, String opPackageName, int opUid) {
        Throwable th;
        IZrHung iZrHung;
        long j;
        long j2 = eventTime;
        String str = reason;
        int i = reasonUid;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UL_Power wakeUpNoUpdateLocked: eventTime=");
        stringBuilder.append(j2);
        stringBuilder.append(", uid=");
        stringBuilder.append(i);
        Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
        stopPickupTrunOff();
        boolean reasonPower = "android.policy:POWER".equalsIgnoreCase(str);
        if ((j2 >= this.mLastSleepTime || (this.mLastSleepTimeDuoToFastFP == this.mLastSleepTime && reasonPower)) && ((this.mWakefulness != 1 || this.mBrightnessWaitModeEnabled) && this.mBootCompleted && this.mSystemReady && (!this.mProximityPositive || reasonPower))) {
            if (j2 < this.mLastSleepTime) {
                boolean z = this.mLastSleepTimeDuoToFastFP == this.mLastSleepTime && reasonPower;
                this.mAdjustTimeNextUserActivity = z;
            }
            if (mSupportFaceDetect) {
                startIntelliService();
            }
            IZrHung iZrHung2 = HwFrameworkFactory.getZrHung("zrhung_wp_screenon_framework");
            if (iZrHung2 != null) {
                ZrHungData arg = new ZrHungData();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("wakeUpNoUpdateLocked: reason=");
                stringBuilder2.append(str);
                stringBuilder2.append(", uid=");
                stringBuilder2.append(i);
                arg.putString("addScreenOnInfo", stringBuilder2.toString());
                iZrHung2.addInfo(arg);
                if (reasonPower) {
                    iZrHung2.start(null);
                }
            }
            Trace.asyncTraceBegin(131072, TRACE_SCREEN_ON, 0);
            Trace.traceBegin(131072, "wakeUp");
            UniPerf.getInstance().uniPerfEvent(4102, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, new int[0]);
            try {
                int i2 = this.mWakefulness;
                if (i2 != 0) {
                    switch (i2) {
                        case 2:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("UL_Power Waking up from dream (uid=");
                            stringBuilder.append(i);
                            stringBuilder.append(" reason=");
                            stringBuilder.append(str);
                            stringBuilder.append(")...");
                            Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
                            Jlog.d(6, "JL_PMS_WAKEFULNESS_DREAMING");
                            break;
                        case 3:
                            try {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("UL_Power Waking up from dozing (uid=");
                                stringBuilder.append(i);
                                stringBuilder.append(" reason=");
                                stringBuilder.append(str);
                                stringBuilder.append(")...");
                                Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
                                Jlog.d(7, "JL_PMS_WAKEFULNESS_NAPPING");
                                break;
                            } catch (Throwable th2) {
                                th = th2;
                                iZrHung = iZrHung2;
                                j = 131072;
                                Trace.traceEnd(j);
                                throw th;
                            }
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("UL_Power Waking up from sleep (uid=");
                stringBuilder.append(i);
                stringBuilder.append(" reason=");
                stringBuilder.append(str);
                stringBuilder.append(")...");
                Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
                if (Jlog.isPerfTest()) {
                    Jlog.i(2204, "JL_PWRSCRON_PMS_ASLEEP");
                }
                Jlog.d(5, "JL_PMS_WAKEFULNESS_ASLEEP");
                this.mForceDoze = false;
                this.mLastWakeTime = j2;
                setWakefulnessLocked(1, 0);
                disableBrightnessWaitLocked(false);
                this.mNotifier.onWakeUp(str, i, opPackageName, opUid);
                j = 131072;
                try {
                    userActivityNoUpdateLocked(j2, 0, 0, i);
                    sendPowerkeyWakeupMsg(reasonPower);
                    Trace.traceEnd(j);
                    if (mSupportFaceDetect || sSupportFaceRecognition) {
                        PowerManagerHandler powerManagerHandler = this.mHandler;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(str);
                        stringBuilder3.append("#");
                        stringBuilder3.append(i);
                        powerManagerHandler.obtainMessage(104, 0, 0, stringBuilder3.toString()).sendToTarget();
                    }
                    return true;
                } catch (Throwable th3) {
                    th = th3;
                    Trace.traceEnd(j);
                    throw th;
                }
            } catch (Throwable th4) {
                th = th4;
                iZrHung = iZrHung2;
                j = 131072;
                Trace.traceEnd(j);
                throw th;
            }
        }
        notifyWakeupResult(false);
        return false;
    }

    private void sendPowerkeyWakeupMsg(boolean reasonPower) {
        if (reasonPower && mIsPowerTurnTorchOff) {
            this.mHandler.sendEmptyMessage(102);
        }
    }

    /* JADX WARNING: Missing block: B:20:0x0060, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void goToSleepInternal(long eventTime, int reason, int flags, int uid) {
        synchronized (this.mLock) {
            if (isCarMachineHeldWakeLock()) {
                if (this.inVdriveBackLightMode) {
                    this.mBackLight.setMirrorLinkBrightness(255);
                    this.inVdriveBackLightMode = false;
                    this.mBackLight.setMirrorLinkBrightnessStatus(false);
                    if (this.mInputManagerInternal != null) {
                        this.mInputManagerInternal.setMirrorLinkInputStatus(false);
                    }
                } else {
                    this.mBackLight.setMirrorLinkBrightness(0);
                    this.inVdriveBackLightMode = true;
                    this.mBackLight.setMirrorLinkBrightnessStatus(true);
                    if (this.mInputManagerInternal != null) {
                        this.mInputManagerInternal.setMirrorLinkInputStatus(true);
                    }
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("VCar mode goToSleepInternal inVdriveBackLightMode=");
                stringBuilder.append(this.inVdriveBackLightMode);
                Slog.d(str, stringBuilder.toString());
            } else if (goToSleepNoUpdateLocked(eventTime, reason, flags, uid)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean lightScreenIfBlack() {
        try {
            IHwPCManager pcMgr = HwPCUtils.getHwPCManager();
            if (!(pcMgr == null || pcMgr.isScreenPowerOn())) {
                HwPCUtils.log(TAG, "screen from OFF to ON");
                pcMgr.setScreenPower(true);
                return true;
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("lightScreenIfBlack ");
            stringBuilder.append(e);
            HwPCUtils.log(str, stringBuilder.toString());
        }
        return false;
    }

    protected boolean goToSleepNoUpdateLocked(long eventTime, int reason, int flags, int uid) {
        long j = eventTime;
        int reason2 = reason;
        int i = flags;
        int i2 = uid;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UL_Power goToSleepNoUpdateLocked: eventTime=");
        stringBuilder.append(j);
        stringBuilder.append(", reason=");
        stringBuilder.append(reason2);
        stringBuilder.append(", flags=");
        stringBuilder.append(i);
        stringBuilder.append(", uid=");
        stringBuilder.append(i2);
        Flog.i(NativeResponseCode.SERVICE_LOST, stringBuilder.toString());
        if (j < this.mLastWakeTime || this.mWakefulness == 0 || this.mWakefulness == 3 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        if (this.mWakefulness == 1 && this.mWakefulnessChanging && reason2 == 4) {
            if (DEBUG_SPEW) {
                Slog.d(TAG, "the current screen status is not really screen-on");
            }
            return false;
        }
        StringBuilder stringBuilder2;
        IZrHung iZrHung = HwFrameworkFactory.getZrHung("zrhung_wp_screenon_framework");
        if (iZrHung != null) {
            ZrHungData arg = new ZrHungData();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("goToSleepNoUpdateLocked: reason=");
            stringBuilder2.append(reason2);
            stringBuilder2.append(", uid=");
            stringBuilder2.append(i2);
            arg.putString("addScreenOnInfo", stringBuilder2.toString());
            iZrHung.addInfo(arg);
        }
        if (HwPCUtils.isPcCastModeInServer()) {
            if (102 == reason2) {
                Slog.d(TAG, "Do not gotosleep by incallui when on the phone in PcCastMode");
                return false;
            } else if (4 == reason2 && lightScreenIfBlack()) {
                return false;
            }
        }
        if (mSupportFaceDetect) {
            unregisterFaceDetect();
            stopIntelliService();
        }
        boolean isVRMode = false;
        if (HwFrameworkFactory.getVRSystemServiceManager() != null) {
            isVRMode = HwFrameworkFactory.getVRSystemServiceManager().isVRMode();
        }
        if (isVRMode && reason2 == 4) {
            Slog.d(TAG, "VR mode enabled, skipping gotoSleep by power key.");
            return false;
        }
        String str;
        Trace.traceBegin(131072, "goToSleep");
        StringBuilder stringBuilder3;
        String str2;
        switch (reason2) {
            case 1:
                str = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("UL_Power Going to sleep due to device administration policy (uid ");
                stringBuilder3.append(i2);
                stringBuilder3.append(")...");
                Slog.i(str, stringBuilder3.toString());
                break;
            case 2:
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("UL_Power Going to sleep due to screen timeout (uid ");
                stringBuilder2.append(i2);
                stringBuilder2.append(")...");
                Slog.i(str, stringBuilder2.toString());
                Jlog.d(79, "goToSleep due to screen timeout");
                break;
            case 3:
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("UL_Power Going to sleep due to lid switch (uid ");
                stringBuilder2.append(i2);
                stringBuilder2.append(")...");
                Slog.i(str, stringBuilder2.toString());
                Jlog.d(79, "goToSleep due to lid");
                break;
            case 4:
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("UL_Power Going to sleep due to power button (uid ");
                stringBuilder2.append(i2);
                stringBuilder2.append(")...");
                Slog.i(str2, stringBuilder2.toString());
                Jlog.d(15, "goToSleep due to powerkey");
                break;
            case 5:
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("UL_Power Going to sleep due to HDMI standby (uid ");
                stringBuilder2.append(i2);
                stringBuilder2.append(")...");
                Slog.i(str, stringBuilder2.toString());
                Jlog.d(79, "goToSleep due to HDMI");
                break;
            case 6:
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("UL_Power Going to sleep due to sleep button (uid ");
                stringBuilder2.append(i2);
                stringBuilder2.append(")...");
                Slog.i(str2, stringBuilder2.toString());
                Jlog.d(15, "goToSleep due to sleepbutton");
                break;
            case 7:
                str = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Going to sleep by an accessibility service request (uid ");
                stringBuilder3.append(i2);
                stringBuilder3.append(")...");
                Slog.i(str, stringBuilder3.toString());
                break;
            default:
                switch (reason2) {
                    case 100:
                        Slog.i(TAG, "UL_Power Going to sleep due to proximity...");
                        Jlog.d(78, "goToSleep due to proximity");
                        break;
                    case 101:
                        Slog.i(TAG, "UL_Power Going to sleep due to wait brightness timeout...");
                        Jlog.d(79, "gotoToSleep due to wait brightness timeout");
                        break;
                    case 102:
                        Slog.i(TAG, "UL_Power Going to sleep due to called by incallui when on the phone...");
                        Jlog.d(79, "goToSleep due to called by incallui when on the phone");
                        break;
                    default:
                        try {
                            str = TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("UL_Power Going to sleep by application request (uid ");
                            stringBuilder4.append(i2);
                            stringBuilder4.append(")...");
                            Slog.i(str, stringBuilder4.toString());
                            Jlog.d(79, "goToSleep by app");
                            reason2 = 0;
                            break;
                        } catch (Throwable th) {
                            Trace.traceEnd(131072);
                        }
                }
        }
        this.mLastSleepTime = j;
        this.mSandmanSummoned = true;
        setWakefulnessLocked(3, reason2);
        int numWakeLocks = this.mWakeLocks.size();
        int numWakeLocksCleared = 0;
        for (int i3 = 0; i3 < numWakeLocks; i3++) {
            int i4 = ((WakeLock) this.mWakeLocks.get(i3)).mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI;
            if (i4 == 6 || i4 == 10 || i4 == 26) {
                numWakeLocksCleared++;
            }
        }
        EventLogTags.writePowerSleepRequested(numWakeLocksCleared);
        if ((i & 1) != 0 || this.mBrightnessWaitModeEnabled) {
            if (this.mBrightnessWaitModeEnabled) {
                this.mLastSleepTimeDuoToFastFP = j;
                if (DEBUG) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("goToSleep mLastSleepTimeDuoToFastFP=");
                    stringBuilder2.append(this.mLastSleepTimeDuoToFastFP);
                    Slog.d(str, stringBuilder2.toString());
                }
            }
            disableBrightnessWaitLocked(false);
            str = TAG;
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("AOD goToSleepNoUpdateLocked mForceDoze=");
            stringBuilder5.append(this.mForceDoze);
            Slog.i(str, stringBuilder5.toString());
            if (!(mSupportAod && this.mForceDoze)) {
                reallyGoToSleepNoUpdateLocked(j, i2);
            }
        }
        Trace.traceEnd(131072);
        return true;
    }

    private void napInternal(long eventTime, int uid) {
        synchronized (this.mLock) {
            if (napNoUpdateLocked(eventTime, uid)) {
                updatePowerStateLocked();
            }
        }
    }

    private boolean napNoUpdateLocked(long eventTime, int uid) {
        String str;
        if (DEBUG_SPEW) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("napNoUpdateLocked: eventTime=");
            stringBuilder.append(eventTime);
            stringBuilder.append(", uid=");
            stringBuilder.append(uid);
            Slog.d(str, stringBuilder.toString());
        }
        if (eventTime < this.mLastWakeTime || this.mWakefulness != 1 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(131072, "nap");
        try {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Nap time (uid ");
            stringBuilder2.append(uid);
            stringBuilder2.append(")...");
            Slog.i(str, stringBuilder2.toString());
            this.mSandmanSummoned = true;
            setWakefulnessLocked(2, 0);
            return true;
        } finally {
            Trace.traceEnd(131072);
        }
    }

    private boolean reallyGoToSleepNoUpdateLocked(long eventTime, int uid) {
        String str;
        if (DEBUG_SPEW) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("reallyGoToSleepNoUpdateLocked: eventTime=");
            stringBuilder.append(eventTime);
            stringBuilder.append(", uid=");
            stringBuilder.append(uid);
            Slog.d(str, stringBuilder.toString());
        }
        if (eventTime < this.mLastWakeTime || this.mWakefulness == 0 || !this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        Trace.traceBegin(131072, "reallyGoToSleep");
        try {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Sleeping (uid ");
            stringBuilder2.append(uid);
            stringBuilder2.append(")...");
            Slog.i(str, stringBuilder2.toString());
            setWakefulnessLocked(0, 2);
            return true;
        } finally {
            Trace.traceEnd(131072);
        }
    }

    @VisibleForTesting
    protected void setWakefulnessLocked(int wakefulness, int reason) {
        if (this.mWakefulness != wakefulness || this.mBrightnessWaitModeEnabled) {
            this.mWakefulness = wakefulness;
            this.mWakefulnessChanging = true;
            this.mDirty |= 2;
            if (this.mNotifier != null) {
                this.mNotifier.onWakefulnessChangeStarted(wakefulness, reason);
            }
            notifyWakeupResult(true);
            return;
        }
        notifyWakeupResult(false);
    }

    private void logSleepTimeoutRecapturedLocked() {
        long savedWakeTimeMs = this.mOverriddenTimeout - SystemClock.uptimeMillis();
        if (savedWakeTimeMs >= 0) {
            EventLogTags.writePowerSoftSleepRequested(savedWakeTimeMs);
            this.mOverriddenTimeout = -1;
        }
    }

    private void logScreenOn() {
        Trace.asyncTraceEnd(131072, TRACE_SCREEN_ON, 0);
        int latencyMs = (int) (SystemClock.uptimeMillis() - this.mLastWakeTime);
        LogMaker log = new LogMaker(198);
        log.setType(1);
        log.setSubtype(0);
        log.setLatency((long) latencyMs);
        MetricsLogger.action(log);
        EventLogTags.writePowerScreenState(1, 0, 0, 0, latencyMs);
        if (latencyMs >= 200) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Screen on took ");
            stringBuilder.append(latencyMs);
            stringBuilder.append(" ms");
            Slog.w(str, stringBuilder.toString());
        }
    }

    private void finishWakefulnessChangeIfNeededLocked() {
        if (this.mWakefulnessChanging && this.mDisplayReady && (this.mWakefulness != 3 || (this.mWakeLockSummary & 64) != 0)) {
            if (this.mWakefulness == 3 || this.mWakefulness == 0) {
                logSleepTimeoutRecapturedLocked();
            }
            if (this.mWakefulness == 1) {
                logScreenOn();
            }
            this.mWakefulnessChanging = false;
            this.mNotifier.onWakefulnessChangeFinished();
        }
    }

    protected void updatePowerStateLocked() {
        if (this.mSystemReady && this.mDirty != 0) {
            if (!Thread.holdsLock(this.mLock)) {
                Slog.wtf(TAG, "Power manager lock was not held when calling updatePowerStateLocked");
            }
            Trace.traceBegin(131072, "updatePowerState");
            try {
                updateIsPoweredLocked(this.mDirty);
                updateStayOnLocked(this.mDirty);
                updateScreenBrightnessBoostLocked(this.mDirty);
                long now = SystemClock.uptimeMillis();
                int dirtyPhase2 = 0;
                while (true) {
                    int dirtyPhase1 = this.mDirty;
                    dirtyPhase2 |= dirtyPhase1;
                    this.mDirty = 0;
                    updateWakeLockSummaryLocked(dirtyPhase1);
                    updateUserActivitySummaryLocked(now, dirtyPhase1);
                    if (!updateWakefulnessLocked(dirtyPhase1)) {
                        break;
                    }
                }
                updateProfilesLocked(now);
                updateDreamLocked(dirtyPhase2, updateDisplayPowerStateLocked(dirtyPhase2));
                finishWakefulnessChangeIfNeededLocked();
                updateSuspendBlockerLocked();
            } finally {
                Trace.traceEnd(131072);
            }
        }
    }

    private void updateProfilesLocked(long now) {
        int numProfiles = this.mProfilePowerState.size();
        for (int i = 0; i < numProfiles; i++) {
            ProfilePowerState profile = (ProfilePowerState) this.mProfilePowerState.valueAt(i);
            if (isProfileBeingKeptAwakeLocked(profile, now)) {
                profile.mLockingNotified = false;
            } else if (!profile.mLockingNotified) {
                profile.mLockingNotified = true;
                this.mNotifier.onProfileTimeout(profile.mUserId);
            }
        }
    }

    private boolean isProfileBeingKeptAwakeLocked(ProfilePowerState profile, long now) {
        return profile.mLastUserActivityTime + profile.mScreenOffTimeout > now || (profile.mWakeLockSummary & 32) != 0 || (this.mProximityPositive && (profile.mWakeLockSummary & 16) != 0);
    }

    private void updateIsPoweredLocked(int dirty) {
        if ((dirty & 256) != 0) {
            boolean wasPowered = this.mIsPowered;
            int oldPlugType = this.mPlugType;
            boolean oldLevelLow = this.mBatteryLevelLow;
            this.mIsPowered = this.mBatteryManagerInternal.isPowered(7);
            this.mPlugType = this.mBatteryManagerInternal.getPlugType();
            this.mBatteryLevel = this.mBatteryManagerInternal.getBatteryLevel();
            this.mBatteryLevelLow = this.mBatteryManagerInternal.getBatteryLevelLow();
            if (DEBUG_SPEW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateIsPoweredLocked: wasPowered=");
                stringBuilder.append(wasPowered);
                stringBuilder.append(", mIsPowered=");
                stringBuilder.append(this.mIsPowered);
                stringBuilder.append(", oldPlugType=");
                stringBuilder.append(oldPlugType);
                stringBuilder.append(", mPlugType=");
                stringBuilder.append(this.mPlugType);
                stringBuilder.append(", mBatteryLevel=");
                stringBuilder.append(this.mBatteryLevel);
                Slog.d(str, stringBuilder.toString());
            }
            if (!(wasPowered == this.mIsPowered && oldPlugType == this.mPlugType)) {
                this.mDirty |= 64;
                boolean dockedOnWirelessCharger = this.mWirelessChargerDetector.update(this.mIsPowered, this.mPlugType);
                long now = SystemClock.uptimeMillis();
                if (shouldWakeUpWhenPluggedOrUnpluggedLocked(wasPowered, oldPlugType, dockedOnWirelessCharger)) {
                    long j = now;
                    wakeUpNoUpdateLocked(j, "android.server.power:POWER", 1000, this.mContext.getOpPackageName(), 1000);
                }
                userActivityNoUpdateLocked(now, 0, 0, 1000);
                if (this.mBootCompleted) {
                    if (this.mIsPowered && !BatteryManager.isPlugWired(oldPlugType) && BatteryManager.isPlugWired(this.mPlugType)) {
                        this.mNotifier.onWiredChargingStarted();
                    } else if (dockedOnWirelessCharger) {
                        this.mNotifier.onWirelessChargingStarted(this.mBatteryLevel);
                    }
                }
            }
            this.mBatterySaverStateMachine.setBatteryStatus(this.mIsPowered, this.mBatteryLevel, this.mBatteryLevelLow);
        }
    }

    private boolean shouldWakeUpWhenPluggedOrUnpluggedLocked(boolean wasPowered, int oldPlugType, boolean dockedOnWirelessCharger) {
        if (!this.mWakeUpWhenPluggedOrUnpluggedConfig) {
            return false;
        }
        if (wasPowered && !this.mIsPowered && oldPlugType == 4) {
            return false;
        }
        if (!wasPowered && this.mIsPowered && this.mPlugType == 4 && !dockedOnWirelessCharger) {
            return false;
        }
        if (this.mIsPowered && this.mWakefulness == 2) {
            return false;
        }
        if (this.mTheaterModeEnabled && !this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig) {
            return false;
        }
        if (this.mAlwaysOnEnabled && this.mWakefulness == 3) {
            return false;
        }
        return true;
    }

    private void updateStayOnLocked(int dirty) {
        if ((dirty & 288) != 0) {
            boolean wasStayOn = this.mStayOn;
            if (this.mStayOnWhilePluggedInSetting == 0 || isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked()) {
                this.mStayOn = false;
            } else {
                this.mStayOn = this.mBatteryManagerInternal.isPowered(this.mStayOnWhilePluggedInSetting);
            }
            if (this.mStayOn != wasStayOn) {
                this.mDirty |= 128;
            }
        }
    }

    private void updateWakeLockSummaryLocked(int dirty) {
        if ((dirty & 3) != 0) {
            int i;
            int i2 = 0;
            this.mWakeLockSummary = 0;
            int numProfiles = this.mProfilePowerState.size();
            for (i = 0; i < numProfiles; i++) {
                ((ProfilePowerState) this.mProfilePowerState.valueAt(i)).mWakeLockSummary = 0;
            }
            i = this.mWakeLocks.size();
            for (int i3 = 0; i3 < i; i3++) {
                WakeLock wakeLock = (WakeLock) this.mWakeLocks.get(i3);
                int wakeLockFlags = getWakeLockSummaryFlags(wakeLock);
                this.mWakeLockSummary |= wakeLockFlags;
                for (int j = 0; j < numProfiles; j++) {
                    ProfilePowerState profile = (ProfilePowerState) this.mProfilePowerState.valueAt(j);
                    if (wakeLockAffectsUser(wakeLock, profile.mUserId)) {
                        profile.mWakeLockSummary |= wakeLockFlags;
                    }
                }
            }
            this.mWakeLockSummary = adjustWakeLockSummaryLocked(this.mWakeLockSummary);
            while (i2 < numProfiles) {
                ProfilePowerState profile2 = (ProfilePowerState) this.mProfilePowerState.valueAt(i2);
                profile2.mWakeLockSummary = adjustWakeLockSummaryLocked(profile2.mWakeLockSummary);
                i2++;
            }
            if (DEBUG_Controller) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateWakeLockSummaryLocked: mWakefulness=");
                stringBuilder.append(PowerManagerInternal.wakefulnessToString(this.mWakefulness));
                stringBuilder.append(", mWakeLockSummary=0x");
                stringBuilder.append(Integer.toHexString(this.mWakeLockSummary));
                Slog.d(str, stringBuilder.toString());
            }
        }
    }

    private int adjustWakeLockSummaryLocked(int wakeLockSummary) {
        if (this.mWakefulness != 3) {
            wakeLockSummary &= -193;
        }
        if (this.mWakefulness == 0 || (wakeLockSummary & 64) != 0) {
            wakeLockSummary &= -15;
        }
        if ((wakeLockSummary & 6) != 0) {
            if (this.mWakefulness == 1) {
                wakeLockSummary |= 33;
            } else if (this.mWakefulness == 2) {
                wakeLockSummary |= 1;
            }
        }
        if ((wakeLockSummary & 128) != 0) {
            return wakeLockSummary | 1;
        }
        return wakeLockSummary;
    }

    private int getWakeLockSummaryFlags(WakeLock wakeLock) {
        int i = wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI;
        if (i != 1) {
            if (i == 6) {
                return 4;
            }
            if (i == 10) {
                return 2;
            }
            if (i == 26) {
                return 10;
            }
            if (i == 32) {
                return 16;
            }
            if (i == 64) {
                return 64;
            }
            if (i == 128) {
                return 128;
            }
        } else if (!wakeLock.mDisabled) {
            return 1;
        }
        return 0;
    }

    private boolean wakeLockAffectsUser(WakeLock wakeLock, int userId) {
        boolean z = false;
        if (wakeLock.mWorkSource != null) {
            for (int k = 0; k < wakeLock.mWorkSource.size(); k++) {
                if (userId == UserHandle.getUserId(wakeLock.mWorkSource.get(k))) {
                    return true;
                }
            }
            ArrayList<WorkChain> workChains = wakeLock.mWorkSource.getWorkChains();
            if (workChains != null) {
                for (int k2 = 0; k2 < workChains.size(); k2++) {
                    if (userId == UserHandle.getUserId(((WorkChain) workChains.get(k2)).getAttributionUid())) {
                        return true;
                    }
                }
            }
        }
        if (userId == UserHandle.getUserId(wakeLock.mOwnerUid)) {
            z = true;
        }
        return z;
    }

    void checkForLongWakeLocks() {
        synchronized (this.mLock) {
            long now = SystemClock.uptimeMillis();
            this.mNotifyLongDispatched = now;
            long when = now - 60000;
            long nextCheckTime = JobStatus.NO_LATEST_RUNTIME;
            int numWakeLocks = this.mWakeLocks.size();
            for (int i = 0; i < numWakeLocks; i++) {
                WakeLock wakeLock = (WakeLock) this.mWakeLocks.get(i);
                if ((wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 1 && wakeLock.mNotifiedAcquired && !wakeLock.mNotifiedLong) {
                    if (wakeLock.mAcquireTime < when) {
                        notifyWakeLockLongStartedLocked(wakeLock);
                    } else {
                        long checkTime = wakeLock.mAcquireTime + 60000;
                        if (checkTime < nextCheckTime) {
                            nextCheckTime = checkTime;
                        }
                    }
                }
            }
            this.mNotifyLongScheduled = 0;
            this.mHandler.removeMessages(4);
            if (nextCheckTime != JobStatus.NO_LATEST_RUNTIME) {
                this.mNotifyLongNextCheck = nextCheckTime;
                enqueueNotifyLongMsgLocked(nextCheckTime);
            } else {
                this.mNotifyLongNextCheck = 0;
            }
        }
    }

    private void updateUserActivitySummaryLocked(long now, int dirty) {
        if ((dirty & 39) != 0) {
            long screenOffTimeout;
            this.mHandler.removeMessages(1);
            if (mSupportFaceDetect) {
                this.mHandler.removeMessages(103);
            }
            if (this.mWakefulness == 1 || this.mWakefulness == 2 || this.mWakefulness == 3) {
                long nextTimeout;
                long sleepTimeout = getSleepTimeoutLocked();
                screenOffTimeout = getScreenOffTimeoutLocked(sleepTimeout);
                long screenDimDuration = getScreenDimDurationLocked(screenOffTimeout);
                boolean userInactiveOverride = this.mUserInactiveOverrideFromWindowManager;
                long screenOffTimeout2 = screenOffTimeout;
                long nextProfileTimeout = getNextProfileTimeoutLocked(now);
                this.mUserActivitySummary = 0;
                boolean startNoChangeLights = false;
                long nextTimeout2 = 0;
                long nextProfileTimeout2 = nextProfileTimeout;
                if (this.mLastUserActivityTime >= this.mLastWakeTime) {
                    nextTimeout = (this.mLastUserActivityTime + screenOffTimeout2) - screenDimDuration;
                    if (now < nextTimeout) {
                        this.mUserActivitySummary = 1;
                    } else {
                        long nextTimeout3 = this.mLastUserActivityTime + screenOffTimeout2;
                        if (now < nextTimeout3) {
                            this.mUserActivitySummary = 2;
                        }
                        nextTimeout = nextTimeout3;
                    }
                } else {
                    nextTimeout = nextTimeout2;
                }
                if (this.mUserActivitySummary == 0 && this.mLastUserActivityTimeNoChangeLights >= this.mLastWakeTime) {
                    nextTimeout = this.mLastUserActivityTimeNoChangeLights + screenOffTimeout2;
                    if (now < nextTimeout) {
                        boolean startNoChangeLights2;
                        if (this.mDisplayPowerRequest.policy == 3 || this.mDisplayPowerRequest.policy == 4) {
                            this.mUserActivitySummary = 1;
                            startNoChangeLights2 = true;
                        } else if (this.mDisplayPowerRequest.policy == 2) {
                            this.mUserActivitySummary = 2;
                            startNoChangeLights2 = true;
                        }
                        startNoChangeLights = startNoChangeLights2;
                    }
                }
                if (this.mUserActivitySummary == 0) {
                    if (sleepTimeout >= 0) {
                        long anyUserActivity = Math.max(this.mLastUserActivityTime, this.mLastUserActivityTimeNoChangeLights);
                        if (anyUserActivity >= this.mLastWakeTime) {
                            nextTimeout = anyUserActivity + sleepTimeout;
                            if (now < nextTimeout) {
                                this.mUserActivitySummary = 4;
                            }
                        }
                    } else {
                        this.mUserActivitySummary = 4;
                        nextTimeout = -1;
                    }
                }
                if (this.mUserActivitySummary != 4 && userInactiveOverride) {
                    if ((this.mUserActivitySummary & 3) != 0 && nextTimeout >= now && this.mOverriddenTimeout == -1) {
                        this.mOverriddenTimeout = nextTimeout;
                    }
                    this.mUserActivitySummary = 4;
                    nextTimeout = -1;
                }
                if (nextProfileTimeout2 > 0) {
                    screenOffTimeout = Math.min(nextTimeout, nextProfileTimeout2);
                } else {
                    screenOffTimeout = nextTimeout;
                }
                if (this.mUserActivitySummary != 0 && screenOffTimeout >= 0) {
                    scheduleUserInactivityTimeout(screenOffTimeout);
                    if (needFaceDetect(screenOffTimeout, now, startNoChangeLights)) {
                        Message msg1 = this.mHandler.obtainMessage(103);
                        msg1.setAsynchronous(true);
                        this.mHandler.sendMessageAtTime(msg1, screenOffTimeout - 1000);
                    }
                }
            } else {
                this.mUserActivitySummary = 0;
                screenOffTimeout = 0;
            }
            if (DEBUG_Controller) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateUserActivitySummaryLocked: mWakefulness=");
                stringBuilder.append(PowerManagerInternal.wakefulnessToString(this.mWakefulness));
                stringBuilder.append(", mUserActivitySummary=0x");
                stringBuilder.append(Integer.toHexString(this.mUserActivitySummary));
                stringBuilder.append(", nextTimeout=");
                stringBuilder.append(TimeUtils.formatUptime(screenOffTimeout));
                Slog.d(str, stringBuilder.toString());
            }
        }
    }

    private void scheduleUserInactivityTimeout(long timeMs) {
        Message msg = this.mHandler.obtainMessage(1);
        msg.setAsynchronous(true);
        this.mHandler.sendMessageAtTime(msg, timeMs);
    }

    private long getNextProfileTimeoutLocked(long now) {
        long nextTimeout = -1;
        int numProfiles = this.mProfilePowerState.size();
        for (int i = 0; i < numProfiles; i++) {
            ProfilePowerState profile = (ProfilePowerState) this.mProfilePowerState.valueAt(i);
            long timeout = profile.mLastUserActivityTime + profile.mScreenOffTimeout;
            if (timeout > now && (nextTimeout == -1 || timeout < nextTimeout)) {
                nextTimeout = timeout;
            }
        }
        return nextTimeout;
    }

    private void handleUserActivityTimeout() {
        synchronized (this.mLock) {
            if (DEBUG_SPEW) {
                Slog.d(TAG, "UL_Power handleUserActivityTimeout");
            }
            this.mDirty |= 4;
            this.mScreenTimeoutFlag = true;
            updatePowerStateLocked();
            this.mScreenTimeoutFlag = false;
        }
    }

    private long getSleepTimeoutLocked() {
        long timeout = this.mSleepTimeoutSetting;
        if (timeout <= 0) {
            return -1;
        }
        return Math.max(timeout, this.mMinimumScreenOffTimeoutConfig);
    }

    private long getScreenOffTimeoutLocked(long sleepTimeout) {
        long timeout = this.mScreenOffTimeoutSetting;
        if (isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked()) {
            timeout = Math.min(timeout, this.mMaximumScreenOffTimeoutFromDeviceAdmin);
        }
        if (this.mUserActivityTimeoutOverrideFromWindowManager >= 0) {
            timeout = Math.min(timeout, this.mUserActivityTimeoutOverrideFromWindowManager);
        }
        if (sleepTimeout >= 0) {
            timeout = Math.min(timeout, sleepTimeout);
        }
        if (getAdjustedMaxTimeout((int) timeout, (int) this.mMinimumScreenOffTimeoutConfig) > 0) {
            return Math.min(timeout, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
        }
        return Math.max(timeout, this.mMinimumScreenOffTimeoutConfig);
    }

    private long getScreenDimDurationLocked(long screenOffTimeout) {
        int maxDimRatio = Integer.parseInt(SystemProperties.get("sys.aps.maxDimRatio", "-1"));
        long dimDuration = -1;
        if (maxDimRatio != -1) {
            dimDuration = (long) HwFrameworkFactory.getHwApsImpl().getCustScreenDimDurationLocked((int) screenOffTimeout);
        }
        if (dimDuration == -1 || maxDimRatio == -1) {
            return Math.min(this.mMaximumScreenDimDurationConfig, (long) (((float) screenOffTimeout) * this.mMaximumScreenDimRatioConfig));
        }
        return dimDuration;
    }

    private boolean updateWakefulnessLocked(int dirty) {
        if ((dirty & 1687) == 0 || this.mWakefulness != 1 || !isItBedTimeYetLocked()) {
            return false;
        }
        if (DEBUG_SPEW) {
            Slog.d(TAG, "UL_Power updateWakefulnessLocked: Bed time...");
        }
        long time = SystemClock.uptimeMillis();
        if (shouldNapAtBedTimeLocked()) {
            return napNoUpdateLocked(time, 1000);
        }
        return goToSleepNoUpdateLocked(time, 2, 0, 1000);
    }

    private boolean shouldNapAtBedTimeLocked() {
        return this.mDreamsActivateOnSleepSetting || (this.mDreamsActivateOnDockSetting && this.mDockState != 0);
    }

    private boolean isItBedTimeYetLocked() {
        boolean keepAwake = isBeingKeptAwakeLocked();
        if (DEBUG && this.mScreenTimeoutFlag && keepAwake) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UL_Power Screen timeout occured. mStayOn = ");
            stringBuilder.append(this.mStayOn);
            stringBuilder.append(", mProximityPositive = ");
            stringBuilder.append(this.mProximityPositive);
            stringBuilder.append(", mWakeLockSummary = 0x");
            stringBuilder.append(Integer.toHexString(this.mWakeLockSummary));
            stringBuilder.append(", mUserActivitySummary = 0x");
            stringBuilder.append(Integer.toHexString(this.mUserActivitySummary));
            stringBuilder.append(", mScreenBrightnessBoostInProgress = ");
            stringBuilder.append(this.mScreenBrightnessBoostInProgress);
            Slog.i(str, stringBuilder.toString());
            if ((this.mWakeLockSummary & 32) != 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Wake Locks: size = ");
                stringBuilder.append(this.mWakeLocks.size());
                Slog.i(str, stringBuilder.toString());
                Iterator it = this.mWakeLocks.iterator();
                while (it.hasNext()) {
                    WakeLock wl = (WakeLock) it.next();
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("WakeLock:");
                    stringBuilder2.append(wl.toString());
                    Slog.i(str2, stringBuilder2.toString());
                }
            }
        }
        if (mSupportFaceDetect && this.mScreenTimeoutFlag && ((this.mWakeLockSummary & 32) != 0 || this.mStayOn)) {
            unregisterFaceDetect();
        }
        return this.mBootCompleted && !keepAwake;
    }

    private boolean isBeingKeptAwakeLocked() {
        return this.mStayOn || ((this.mProximityPositive && !isPhoneHeldWakeLock()) || (this.mWakeLockSummary & 32) != 0 || (this.mUserActivitySummary & 3) != 0 || this.mScreenBrightnessBoostInProgress);
    }

    private void updateDreamLocked(int dirty, boolean displayBecameReady) {
        if (((dirty & 1015) != 0 || displayBecameReady) && this.mDisplayReady) {
            scheduleSandmanLocked();
        }
    }

    private void scheduleSandmanLocked() {
        if (!this.mSandmanScheduled) {
            this.mSandmanScheduled = true;
            Message msg = this.mHandler.obtainMessage(2);
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
        }
    }

    /* JADX WARNING: Missing block: B:68:0x00f2, code:
            return;
     */
    /* JADX WARNING: Missing block: B:81:0x013c, code:
            return;
     */
    /* JADX WARNING: Missing block: B:83:0x013e, code:
            if (r4 == false) goto L_0x015f;
     */
    /* JADX WARNING: Missing block: B:85:0x0142, code:
            if (r14.mCust == null) goto L_0x015a;
     */
    /* JADX WARNING: Missing block: B:87:0x014a, code:
            if (r14.mCust.isChargingAlbumSupported() == false) goto L_0x015a;
     */
    /* JADX WARNING: Missing block: B:89:0x0152, code:
            if (r14.mCust.isStartDreamFromUser() != false) goto L_0x015f;
     */
    /* JADX WARNING: Missing block: B:90:0x0154, code:
            r14.mDreamManager.stopDream(false);
     */
    /* JADX WARNING: Missing block: B:91:0x015a, code:
            r14.mDreamManager.stopDream(false);
     */
    /* JADX WARNING: Missing block: B:92:0x015f, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleSandman() {
        int wakefulness;
        boolean z;
        boolean cust;
        boolean isDreaming;
        synchronized (this.mLock) {
            boolean startDreaming;
            this.mSandmanScheduled = false;
            wakefulness = this.mWakefulness;
            z = true;
            if (this.mSandmanSummoned && this.mDisplayReady) {
                cust = this.mCust != null && this.mCust.isChargingAlbumSupported() && this.mCust.isStartDreamFromUser();
                startDreaming = canDreamLocked() || canDozeLocked() || cust;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("startDreaming = ");
                stringBuilder.append(startDreaming);
                Slog.e(str, stringBuilder.toString());
                if (this.mCust != null) {
                    this.mCust.setStartDreamFromUser(false);
                }
                this.mSandmanSummoned = false;
            } else {
                startDreaming = false;
            }
            cust = startDreaming;
        }
        if (this.mDreamManager != null) {
            if (cust) {
                this.mDreamManager.stopDream(false);
                DreamManagerInternal dreamManagerInternal = this.mDreamManager;
                if (wakefulness != 3) {
                    z = false;
                }
                dreamManagerInternal.startDream(z);
            }
            isDreaming = this.mDreamManager.isDreaming();
        } else {
            isDreaming = false;
        }
        z = isDreaming;
        synchronized (this.mLock) {
            if (cust && z) {
                this.mBatteryLevelWhenDreamStarted = this.mBatteryLevel;
                if (wakefulness == 3) {
                    Slog.i(TAG, "Dozing...");
                } else {
                    Slog.i(TAG, "Dreaming...");
                }
            }
            if (this.mSandmanSummoned || this.mWakefulness != wakefulness) {
            } else if (wakefulness == 2) {
                if (z && canDreamLocked()) {
                    if (this.mDreamsBatteryLevelDrainCutoffConfig >= 0 && this.mBatteryLevel < this.mBatteryLevelWhenDreamStarted - this.mDreamsBatteryLevelDrainCutoffConfig && !isBeingKeptAwakeLocked()) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Stopping dream because the battery appears to be draining faster than it is charging.  Battery level when dream started: ");
                        stringBuilder2.append(this.mBatteryLevelWhenDreamStarted);
                        stringBuilder2.append("%.  Battery level now: ");
                        stringBuilder2.append(this.mBatteryLevel);
                        stringBuilder2.append("%.");
                        Slog.i(str2, stringBuilder2.toString());
                    } else if (mSupportFaceDetect) {
                        unregisterFaceDetect();
                    }
                }
                if (isItBedTimeYetLocked()) {
                    goToSleepNoUpdateLocked(SystemClock.uptimeMillis(), 2, 0, 1000);
                    updatePowerStateLocked();
                } else {
                    wakeUpNoUpdateLocked(SystemClock.uptimeMillis(), "android.server.power:DREAM", 1000, this.mContext.getOpPackageName(), 1000);
                    updatePowerStateLocked();
                }
            } else if (wakefulness == 3) {
                if (z || (mSupportAod && this.mForceDoze)) {
                } else {
                    reallyGoToSleepNoUpdateLocked(SystemClock.uptimeMillis(), 1000);
                    updatePowerStateLocked();
                }
            }
        }
    }

    private boolean canDreamLocked() {
        if (this.mWakefulness != 2 || !this.mDreamsSupportedConfig || ((!this.mDreamsEnabledSetting && (this.mCust == null || !this.mCust.isChargingAlbumEnabled() || !this.mCust.isChargingAlbumSupported())) || !this.mDisplayPowerRequest.isBrightOrDim() || this.mDisplayPowerRequest.isVr() || (this.mUserActivitySummary & 7) == 0 || !this.mBootCompleted)) {
            return false;
        }
        if (this.mCust != null && this.mCust.canDreamLocked()) {
            return false;
        }
        if (!isBeingKeptAwakeLocked()) {
            if (!this.mIsPowered && !this.mDreamsEnabledOnBatteryConfig) {
                return false;
            }
            if (!this.mIsPowered && this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig >= 0 && this.mBatteryLevel < this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig) {
                return false;
            }
            if (this.mIsPowered && this.mDreamsBatteryLevelMinimumWhenPoweredConfig >= 0 && this.mBatteryLevel < this.mDreamsBatteryLevelMinimumWhenPoweredConfig) {
                return false;
            }
        }
        if (this.mCust == null || !this.mCust.isChargingAlbumSupported() || this.mCust.isChargingAlbumEnabled()) {
            return true;
        }
        return false;
    }

    private boolean canDozeLocked() {
        return this.mWakefulness == 3;
    }

    private boolean updateDisplayPowerStateLocked(int dirty) {
        boolean oldDisplayReady = this.mDisplayReady;
        if ((dirty & 30783) != 0) {
            int screenBrightnessOverride;
            boolean autoBrightness;
            int newScreenState = getDesiredScreenPolicyLocked();
            boolean z = this.mEyesProtectionMode == 1 || this.mEyesProtectionMode == 3;
            boolean eyeprotectionMode = z;
            if (newScreenState == 3 && this.mDisplayPowerRequest.policy == 0 && !eyeprotectionMode) {
                Slog.d(TAG, "setColorTemperatureAccordingToSetting");
                setColorTemperatureAccordingToSetting();
            }
            this.mDisplayPowerRequest.policy = newScreenState;
            if (this.mIsCoverModeEnabled) {
                screenBrightnessOverride = getCoverModeBrightness();
                updateAutoBrightnessDBforSeekbar(screenBrightnessOverride, newScreenState);
                autoBrightness = false;
            } else if (isValidBrightness(this.mScreenBrightnessOverrideFromWindowManager)) {
                autoBrightness = false;
                screenBrightnessOverride = this.mScreenBrightnessOverrideFromWindowManager;
            } else {
                autoBrightness = this.mScreenBrightnessModeSetting == 1;
                screenBrightnessOverride = -1;
            }
            boolean keyguardLocked = getKeyguardLockedStatus();
            if (this.mKeyguardLocked != keyguardLocked) {
                this.mDisplayManagerInternal.setKeyguardLockedStatus(keyguardLocked);
            }
            this.mKeyguardLocked = keyguardLocked;
            boolean updateBacklightBrightnessFlag = false;
            if (this.mScreenBrightnessOverrideFromWindowManager > 255) {
                updateBacklightBrightnessFlag = this.mBacklightBrightness.updateBacklightBrightness(this.mScreenBrightnessOverrideFromWindowManager);
            }
            this.mDisplayManagerInternal.setBacklightBrightness(this.mBacklightBrightness);
            if (this.mUpdateBacklightBrightnessFlag != updateBacklightBrightnessFlag) {
                this.mDisplayManagerInternal.setCameraModeBrightnessLineEnable(updateBacklightBrightnessFlag);
            }
            this.mUpdateBacklightBrightnessFlag = updateBacklightBrightnessFlag;
            if (screenBrightnessOverride >= 0) {
                screenBrightnessOverride = Math.max(Math.min(screenBrightnessOverride, this.mScreenBrightnessSettingMaximum), this.mScreenBrightnessSettingMinimum);
            }
            this.mDisplayPowerRequest.screenBrightnessOverride = screenBrightnessOverride;
            if (this.mNotifier.getBrightnessModeChangeNoClearOffset()) {
                this.mDisplayManagerInternal.setPoweroffModeChangeAutoEnable(true);
                this.mNotifier.setBrightnessModeChangeNoClearOffset(false);
            }
            this.mDisplayPowerRequest.useAutoBrightness = autoBrightness;
            this.mDisplayPowerRequest.useSmartBacklight = this.mSmartBacklightEnableSetting == 1;
            this.mDisplayPowerRequest.screenAutoBrightness = this.mTemporaryScreenAutoBrightnessSettingOverride;
            this.mDisplayPowerRequest.useProximitySensor = shouldUseProximitySensorLocked();
            if (this.mDisplayPowerRequest.useProximitySensor) {
                DisplayPowerRequest displayPowerRequest = this.mDisplayPowerRequest;
                boolean z2 = isPhoneHeldWakeLock() && !HwPCUtils.isPcCastModeInServer();
                displayPowerRequest.useProximitySensorbyPhone = z2;
            }
            this.mDisplayPowerRequest.boostScreenBrightness = shouldBoostScreenBrightness();
            this.mDisplayPowerRequest.userId = this.mCurrentUserId;
            updatePowerRequestFromBatterySaverPolicy(this.mDisplayPowerRequest);
            if (this.mDisplayPowerRequest.policy == 1) {
                this.mDisplayPowerRequest.dozeScreenState = this.mDozeScreenStateOverrideFromDreamManager;
                if (!((this.mWakeLockSummary & 128) == 0 || this.mDrawWakeLockOverrideFromSidekick)) {
                    if (this.mDisplayPowerRequest.dozeScreenState == 4) {
                        this.mDisplayPowerRequest.dozeScreenState = 3;
                    }
                    if (this.mDisplayPowerRequest.dozeScreenState == 6) {
                        this.mDisplayPowerRequest.dozeScreenState = 2;
                    }
                }
                this.mDisplayPowerRequest.dozeScreenBrightness = this.mDozeScreenBrightnessOverrideFromDreamManager;
            } else {
                this.mDisplayPowerRequest.dozeScreenState = 0;
                this.mDisplayPowerRequest.dozeScreenBrightness = -1;
            }
            this.mDisplayPowerRequest.brightnessWaitMode = this.mBrightnessWaitModeEnabled;
            this.mDisplayPowerRequest.brightnessWaitRet = this.mBrightnessWaitRet;
            this.mDisplayPowerRequest.skipWaitKeyguardDismiss = this.mSkipWaitKeyguardDismiss;
            this.mDisplayReady = this.mDisplayManagerInternal.requestPowerState(this.mDisplayPowerRequest, this.mRequestWaitForNegativeProximity);
            this.mRequestWaitForNegativeProximity = false;
            if ((dirty & 4096) != 0) {
                sQuiescent = false;
            }
            if (DEBUG_Controller) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ready=");
                stringBuilder.append(this.mDisplayReady);
                stringBuilder.append(",policy=");
                stringBuilder.append(this.mDisplayPowerRequest.policy);
                stringBuilder.append(",wkful=");
                stringBuilder.append(this.mWakefulness);
                stringBuilder.append(",wlsum=0x");
                stringBuilder.append(Integer.toHexString(this.mWakeLockSummary));
                stringBuilder.append(",uasum=0x");
                stringBuilder.append(Integer.toHexString(this.mUserActivitySummary));
                stringBuilder.append(",boostinprogress=");
                stringBuilder.append(this.mScreenBrightnessBoostInProgress);
                stringBuilder.append(",waitmodeenable=");
                stringBuilder.append(this.mBrightnessWaitModeEnabled);
                stringBuilder.append(",mode=");
                stringBuilder.append(this.mDisplayPowerRequest.useAutoBrightness);
                stringBuilder.append(",userId=");
                stringBuilder.append(this.mDisplayPowerRequest.userId);
                stringBuilder.append(",mIsVrModeEnabled=");
                stringBuilder.append(this.mIsVrModeEnabled);
                stringBuilder.append(",sQuiescent=");
                stringBuilder.append(sQuiescent);
                Slog.d(str, stringBuilder.toString());
            }
        }
        if (!this.mDisplayReady || oldDisplayReady) {
            return false;
        }
        return true;
    }

    private void updateScreenBrightnessBoostLocked(int dirty) {
        if ((dirty & 2048) != 0 && this.mScreenBrightnessBoostInProgress) {
            long now = SystemClock.uptimeMillis();
            this.mHandler.removeMessages(3);
            if (this.mLastScreenBrightnessBoostTime > this.mLastSleepTime) {
                long boostTimeout = this.mLastScreenBrightnessBoostTime + 5000;
                if (boostTimeout > now) {
                    Message msg = this.mHandler.obtainMessage(3);
                    msg.setAsynchronous(true);
                    this.mHandler.sendMessageAtTime(msg, boostTimeout);
                    return;
                }
            }
            this.mScreenBrightnessBoostInProgress = false;
            this.mNotifier.onScreenBrightnessBoostChanged();
            userActivityNoUpdateLocked(now, 0, 0, 1000);
        }
    }

    private boolean shouldBoostScreenBrightness() {
        return !this.mIsVrModeEnabled && this.mScreenBrightnessBoostInProgress;
    }

    private static boolean isValidBrightness(int value) {
        return value >= 0 && value <= 255;
    }

    @VisibleForTesting
    int getDesiredScreenPolicyLocked() {
        if (this.mWakefulness == 0 || sQuiescent) {
            return 0;
        }
        if (this.mWakefulness == 3) {
            if ((this.mWakeLockSummary & 64) != 0) {
                return 1;
            }
            if (this.mDozeAfterScreenOff) {
                return 0;
            }
        }
        if (this.mIsVrModeEnabled) {
            return 4;
        }
        if ((this.mWakeLockSummary & 2) == 0 && (this.mUserActivitySummary & 1) == 0 && this.mBootCompleted && !this.mScreenBrightnessBoostInProgress) {
            return 2;
        }
        return 3;
    }

    protected void notifyHwBrightnessCallbacks(String what, int arg1, int arg2, Bundle data) {
    }

    private boolean shouldUseProximitySensorLocked() {
        return (this.mIsVrModeEnabled || (this.mWakeLockSummary & 16) == 0) ? false : true;
    }

    private void updateSuspendBlockerLocked() {
        boolean needWakeLockSuspendBlocker = (this.mWakeLockSummary & 1) != 0;
        boolean needDisplaySuspendBlocker = needDisplaySuspendBlockerLocked();
        boolean autoSuspend = needDisplaySuspendBlocker ^ 1;
        boolean interactive = this.mDisplayPowerRequest.isBrightOrDim();
        if (!autoSuspend && this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
            setHalAutoSuspendModeLocked(false);
        }
        if (needWakeLockSuspendBlocker && !this.mHoldingWakeLockSuspendBlocker) {
            this.mWakeLockSuspendBlocker.acquire();
            this.mHoldingWakeLockSuspendBlocker = true;
        }
        if (needDisplaySuspendBlocker && !this.mHoldingDisplaySuspendBlocker) {
            Slog.i(TAG, "need acquire DisplaySuspendBlocker");
            this.mDisplaySuspendBlocker.acquire();
            this.mHoldingDisplaySuspendBlocker = true;
        }
        if (this.mDecoupleHalInteractiveModeFromDisplayConfig && (interactive || this.mDisplayReady)) {
            setHalInteractiveModeLocked(interactive);
        }
        if (!needWakeLockSuspendBlocker && this.mHoldingWakeLockSuspendBlocker) {
            this.mWakeLockSuspendBlocker.release();
            this.mHoldingWakeLockSuspendBlocker = false;
        }
        if (!needDisplaySuspendBlocker && this.mHoldingDisplaySuspendBlocker) {
            Slog.i(TAG, "need release DisplaySuspendBlocker");
            this.mDisplaySuspendBlocker.release();
            this.mHoldingDisplaySuspendBlocker = false;
        }
        if (autoSuspend && this.mDecoupleHalAutoSuspendModeFromDisplayConfig) {
            setHalAutoSuspendModeLocked(true);
        }
    }

    /* JADX WARNING: Missing block: B:11:0x001c, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean needDisplaySuspendBlockerLocked() {
        if (!this.mDisplayReady) {
            return true;
        }
        if ((!this.mDisplayPowerRequest.isBrightOrDim() || (this.mDisplayPowerRequest.useProximitySensor && this.mProximityPositive && this.mSuspendWhenScreenOffDueToProximityConfig)) && !this.mScreenBrightnessBoostInProgress) {
            return false;
        }
        return true;
    }

    private void setHalAutoSuspendModeLocked(boolean enable) {
        if (enable != this.mHalAutoSuspendModeEnabled) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Setting HAL auto-suspend mode to ");
                stringBuilder.append(enable);
                Slog.d(str, stringBuilder.toString());
            }
            this.mHalAutoSuspendModeEnabled = enable;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setHalAutoSuspend(");
            stringBuilder2.append(enable);
            stringBuilder2.append(")");
            Trace.traceBegin(131072, stringBuilder2.toString());
            try {
                nativeSetAutoSuspend(enable);
                if (this.misBetaUser && this.mHwPowerInfoService != null) {
                    synchronized (this.mHwPowerInfoService) {
                        this.mHwPowerInfoService.notePowerInfoSuspendState(enable);
                    }
                }
            } finally {
                Trace.traceEnd(131072);
            }
        }
    }

    private void setHalInteractiveModeLocked(boolean enable) {
        if (enable != this.mHalInteractiveModeEnabled) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Setting HAL interactive mode to ");
                stringBuilder.append(enable);
                Slog.d(str, stringBuilder.toString());
            }
            this.mHalInteractiveModeEnabled = enable;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("setHalInteractive(");
            stringBuilder2.append(enable);
            stringBuilder2.append(")");
            Trace.traceBegin(131072, stringBuilder2.toString());
            try {
                nativeSetInteractive(enable);
            } finally {
                Trace.traceEnd(131072);
            }
        }
    }

    private boolean isInteractiveInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = PowerManagerInternal.isInteractive(this.mWakefulness) && (!this.mBrightnessWaitModeEnabled || this.mAuthSucceeded);
        }
        return z;
    }

    private boolean setLowPowerModeInternal(boolean enabled) {
        synchronized (this.mLock) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setLowPowerModeInternal ");
                stringBuilder.append(enabled);
                stringBuilder.append(" mIsPowered=");
                stringBuilder.append(this.mIsPowered);
                Slog.d(str, stringBuilder.toString());
            }
            if (this.mIsPowered) {
                return false;
            }
            this.mBatterySaverStateMachine.setBatterySaverEnabledManually(enabled);
            return true;
        }
    }

    boolean isDeviceIdleModeInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDeviceIdleMode;
        }
        return z;
    }

    boolean isLightDeviceIdleModeInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mLightDeviceIdleMode;
        }
        return z;
    }

    private void handleBatteryStateChangedLocked() {
        this.mDirty |= 256;
        updatePowerStateLocked();
    }

    private void crashInternal(final String message) {
        Thread t = new Thread("PowerManagerService.crash()") {
            public void run() {
                throw new RuntimeException(message);
            }
        };
        try {
            t.start();
            t.join();
        } catch (InterruptedException e) {
            Slog.wtf(TAG, e);
        }
    }

    @VisibleForTesting
    void updatePowerRequestFromBatterySaverPolicy(DisplayPowerRequest displayPowerRequest) {
        PowerSaveState state = this.mBatterySaverPolicy.getBatterySaverPolicy(7, this.mBatterySaverController.isEnabled());
        displayPowerRequest.lowPowerMode = state.batterySaverEnabled;
        displayPowerRequest.screenLowPowerBrightnessFactor = state.brightnessFactor;
    }

    void setStayOnSettingInternal(int val) {
        Global.putInt(this.mContext.getContentResolver(), "stay_on_while_plugged_in", val);
    }

    void setMaximumScreenOffTimeoutFromDeviceAdminInternal(int userId, long timeMs) {
        if (userId < 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attempt to set screen off timeout for invalid user: ");
            stringBuilder.append(userId);
            Slog.wtf(str, stringBuilder.toString());
            return;
        }
        synchronized (this.mLock) {
            if (userId == 0) {
                this.mMaximumScreenOffTimeoutFromDeviceAdmin = timeMs;
            } else if (timeMs == JobStatus.NO_LATEST_RUNTIME || timeMs == 0) {
                this.mProfilePowerState.delete(userId);
            } else {
                ProfilePowerState profile = (ProfilePowerState) this.mProfilePowerState.get(userId);
                if (profile != null) {
                    profile.mScreenOffTimeout = timeMs;
                } else {
                    this.mProfilePowerState.put(userId, new ProfilePowerState(userId, timeMs));
                    this.mDirty |= 1;
                }
            }
            this.mDirty |= 32;
            updatePowerStateLocked();
        }
    }

    /* JADX WARNING: Missing block: B:10:0x0010, code:
            if (r3 == false) goto L_0x0019;
     */
    /* JADX WARNING: Missing block: B:11:0x0012, code:
            com.android.server.EventLogTags.writeDeviceIdleOnPhase("power");
     */
    /* JADX WARNING: Missing block: B:12:0x0019, code:
            com.android.server.EventLogTags.writeDeviceIdleOffPhase("power");
     */
    /* JADX WARNING: Missing block: B:14:0x0020, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean setDeviceIdleModeInternal(boolean enabled) {
        synchronized (this.mLock) {
            if (this.mDeviceIdleMode == enabled) {
                return false;
            }
            this.mDeviceIdleMode = enabled;
            updateWakeLockDisabledStatesLocked();
        }
    }

    boolean setLightDeviceIdleModeInternal(boolean enabled) {
        synchronized (this.mLock) {
            if (this.mLightDeviceIdleMode != enabled) {
                this.mLightDeviceIdleMode = enabled;
                return true;
            }
            return false;
        }
    }

    void setDeviceIdleWhitelistInternal(int[] appids) {
        synchronized (this.mLock) {
            this.mDeviceIdleWhitelist = appids;
            if (this.mDeviceIdleMode) {
                updateWakeLockDisabledStatesLocked();
            }
        }
    }

    void setDeviceIdleTempWhitelistInternal(int[] appids) {
        synchronized (this.mLock) {
            this.mDeviceIdleTempWhitelist = appids;
            if (this.mDeviceIdleMode) {
                updateWakeLockDisabledStatesLocked();
            }
        }
    }

    void startUidChangesInternal() {
        synchronized (this.mLock) {
            this.mUidsChanging = true;
        }
    }

    void finishUidChangesInternal() {
        synchronized (this.mLock) {
            this.mUidsChanging = false;
            if (this.mUidsChanged) {
                updateWakeLockDisabledStatesLocked();
                this.mUidsChanged = false;
            }
        }
    }

    private void handleUidStateChangeLocked() {
        if (this.mUidsChanging) {
            this.mUidsChanged = true;
        } else {
            updateWakeLockDisabledStatesLocked();
        }
    }

    void updateUidProcStateInternal(int uid, int procState) {
        synchronized (this.mLock) {
            UidState state = (UidState) this.mUidState.get(uid);
            if (state == null) {
                state = new UidState(uid);
                this.mUidState.put(uid, state);
            }
            boolean z = false;
            boolean oldShouldAllow = state.mProcState <= 10;
            state.mProcState = procState;
            if (state.mNumWakeLocks > 0) {
                if (this.mDeviceIdleMode) {
                    handleUidStateChangeLocked();
                } else if (!state.mActive) {
                    if (procState <= 10) {
                        z = true;
                    }
                    if (oldShouldAllow != z) {
                        handleUidStateChangeLocked();
                    }
                }
            }
        }
    }

    void uidGoneInternal(int uid) {
        synchronized (this.mLock) {
            int index = this.mUidState.indexOfKey(uid);
            if (index >= 0) {
                UidState state = (UidState) this.mUidState.valueAt(index);
                state.mProcState = 19;
                state.mActive = false;
                this.mUidState.removeAt(index);
                if (this.mDeviceIdleMode && state.mNumWakeLocks > 0) {
                    handleUidStateChangeLocked();
                }
            }
        }
    }

    void uidActiveInternal(int uid) {
        synchronized (this.mLock) {
            UidState state = (UidState) this.mUidState.get(uid);
            if (state == null) {
                state = new UidState(uid);
                state.mProcState = 18;
                this.mUidState.put(uid, state);
            }
            state.mActive = true;
            if (state.mNumWakeLocks > 0) {
                handleUidStateChangeLocked();
            }
        }
    }

    void uidIdleInternal(int uid) {
        synchronized (this.mLock) {
            UidState state = (UidState) this.mUidState.get(uid);
            if (state != null) {
                state.mActive = false;
                if (state.mNumWakeLocks > 0) {
                    handleUidStateChangeLocked();
                }
            }
        }
    }

    private void updateWakeLockDisabledStatesLocked() {
        boolean changed = false;
        int numWakeLocks = this.mWakeLocks.size();
        for (int i = 0; i < numWakeLocks; i++) {
            WakeLock wakeLock = (WakeLock) this.mWakeLocks.get(i);
            if ((wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 1 && setWakeLockDisabledStateLocked(wakeLock)) {
                changed = true;
                if (wakeLock.mDisabled) {
                    notifyWakeLockReleasedLocked(wakeLock);
                } else {
                    notifyWakeLockAcquiredLocked(wakeLock);
                }
            }
        }
        if (changed) {
            this.mDirty |= 1;
            updatePowerStateLocked();
        }
    }

    protected boolean setWakeLockDisabledStateLocked(WakeLock wakeLock) {
        if ((wakeLock.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 1) {
            boolean disabled = false;
            int appid = UserHandle.getAppId(wakeLock.mOwnerUid);
            if (appid >= 10000) {
                if (this.mConstants.NO_CACHED_WAKE_LOCKS) {
                    if (wakeLock.mPackageName == null || !wakeLock.mPackageName.equals("com.android.deskclock")) {
                        boolean z = (wakeLock.mUidState.mActive || wakeLock.mUidState.mProcState == 19 || wakeLock.mUidState.mProcState <= 10) ? false : true;
                        disabled = z;
                    } else {
                        disabled = false;
                    }
                }
                if (this.mDeviceIdleMode) {
                    UidState state = wakeLock.mUidState;
                    if (Arrays.binarySearch(this.mDeviceIdleWhitelist, appid) < 0 && Arrays.binarySearch(this.mDeviceIdleTempWhitelist, appid) < 0 && state.mProcState != 19 && state.mProcState > 4) {
                        disabled = true;
                    }
                }
            }
            if (wakeLock.mDisabled != disabled) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("wakeLock.mDisabled:");
                stringBuilder.append(disabled);
                stringBuilder.append(", mDeviceIdleMode:");
                stringBuilder.append(this.mDeviceIdleMode);
                stringBuilder.append(", mProcState:");
                stringBuilder.append(wakeLock.mUidState.mProcState);
                Slog.d(str, stringBuilder.toString());
                wakeLock.mDisabled = disabled;
                return true;
            }
        }
        return false;
    }

    private boolean isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked() {
        return this.mMaximumScreenOffTimeoutFromDeviceAdmin >= 0 && this.mMaximumScreenOffTimeoutFromDeviceAdmin < JobStatus.NO_LATEST_RUNTIME;
    }

    /* JADX WARNING: Missing block: B:10:0x000e, code:
            if (r5 == false) goto L_0x0012;
     */
    /* JADX WARNING: Missing block: B:11:0x0010, code:
            r3 = 3;
     */
    /* JADX WARNING: Missing block: B:12:0x0012, code:
            r3 = 0;
     */
    /* JADX WARNING: Missing block: B:13:0x0013, code:
            r1.setFlashing(r6, 2, r3, 0);
     */
    /* JADX WARNING: Missing block: B:14:0x0016, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setAttentionLightInternal(boolean on, int color) {
        synchronized (this.mLock) {
            if (this.mSystemReady) {
                Light light = this.mAttentionLight;
            }
        }
    }

    private void setDozeAfterScreenOffInternal(boolean on) {
        synchronized (this.mLock) {
            this.mDozeAfterScreenOff = on;
        }
    }

    /* JADX WARNING: Missing block: B:16:0x004f, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void boostScreenBrightnessInternal(long eventTime, int uid) {
        synchronized (this.mLock) {
            if (!this.mSystemReady || this.mWakefulness == 0 || eventTime < this.mLastScreenBrightnessBoostTime) {
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Brightness boost activated (uid ");
                stringBuilder.append(uid);
                stringBuilder.append(")...");
                Slog.i(str, stringBuilder.toString());
                this.mLastScreenBrightnessBoostTime = eventTime;
                if (!this.mScreenBrightnessBoostInProgress) {
                    this.mScreenBrightnessBoostInProgress = true;
                    this.mNotifier.onScreenBrightnessBoostChanged();
                }
                this.mDirty |= 2048;
                userActivityNoUpdateLocked(eventTime, 0, 0, uid);
                updatePowerStateLocked();
            }
        }
    }

    private boolean isScreenBrightnessBoostedInternal() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mScreenBrightnessBoostInProgress;
        }
        return z;
    }

    private void handleScreenBrightnessBoostTimeout() {
        synchronized (this.mLock) {
            if (DEBUG_SPEW) {
                Slog.d(TAG, "handleScreenBrightnessBoostTimeout");
            }
            this.mDirty |= 2048;
            updatePowerStateLocked();
        }
    }

    private void setScreenBrightnessOverrideFromWindowManagerInternal(int brightness) {
        synchronized (this.mLock) {
            brightness = setScreenBrightnessMappingtoIndoorMax(brightness);
            if (this.mScreenBrightnessOverrideFromWindowManager != brightness) {
                this.mScreenBrightnessOverrideFromWindowManager = brightness;
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mScreenBrightnessOverrideFromWindowManager=");
                    stringBuilder.append(this.mScreenBrightnessOverrideFromWindowManager);
                    Slog.d(str, stringBuilder.toString());
                }
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    public int setScreenBrightnessMappingtoIndoorMax(int brightness) {
        if (this.mDisplayManagerInternal != null) {
            return this.mDisplayManagerInternal.setScreenBrightnessMappingtoIndoorMax(brightness);
        }
        return brightness;
    }

    private void setUserInactiveOverrideFromWindowManagerInternal() {
        synchronized (this.mLock) {
            this.mUserInactiveOverrideFromWindowManager = true;
            this.mDirty |= 4;
            updatePowerStateLocked();
        }
    }

    private void setUserActivityTimeoutOverrideFromWindowManagerInternal(long timeoutMillis) {
        synchronized (this.mLock) {
            if (this.mUserActivityTimeoutOverrideFromWindowManager != timeoutMillis) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mUserActivityTimeoutOverrideFromWindowManager=");
                    stringBuilder.append(timeoutMillis);
                    Slog.d(str, stringBuilder.toString());
                }
                this.mUserActivityTimeoutOverrideFromWindowManager = timeoutMillis;
                EventLogTags.writeUserActivityTimeoutOverride(timeoutMillis);
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    private void setTemporaryScreenAutoBrightnessSettingOverrideInternal(int brightness) {
        synchronized (this.mLock) {
            if (this.mTemporaryScreenAutoBrightnessSettingOverride != brightness) {
                if (brightness == -1) {
                    this.mDisplayManagerInternal.updateAutoBrightnessAdjustFactor(((float) this.mTemporaryScreenAutoBrightnessSettingOverride) / 255.0f);
                }
                this.mTemporaryScreenAutoBrightnessSettingOverride = brightness;
                this.mDirty |= 32;
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mTemporaryScreenAutoBrightnessSettingOverride=");
                    stringBuilder.append(this.mTemporaryScreenAutoBrightnessSettingOverride);
                    Slog.d(str, stringBuilder.toString());
                }
                updatePowerStateLocked();
            }
        }
    }

    private void setDozeOverrideFromDreamManagerInternal(int screenState, int screenBrightness) {
        synchronized (this.mLock) {
            if (!(this.mDozeScreenStateOverrideFromDreamManager == screenState && this.mDozeScreenBrightnessOverrideFromDreamManager == screenBrightness)) {
                this.mDozeScreenStateOverrideFromDreamManager = screenState;
                this.mDozeScreenBrightnessOverrideFromDreamManager = screenBrightness;
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    private void setDrawWakeLockOverrideFromSidekickInternal(boolean keepState) {
        synchronized (this.mLock) {
            if (this.mDrawWakeLockOverrideFromSidekick != keepState) {
                this.mDrawWakeLockOverrideFromSidekick = keepState;
                this.mDirty |= 32;
                updatePowerStateLocked();
            }
        }
    }

    @VisibleForTesting
    void setVrModeEnabled(boolean enabled) {
        this.mIsVrModeEnabled = enabled;
    }

    private void powerHintInternal(int hintId, int data) {
        if (hintId != 8 || data != 1 || !this.mBatterySaverController.isLaunchBoostDisabled()) {
            nativeSendPowerHint(hintId, data);
        }
    }

    public static void lowLevelShutdown(String reason) {
        if (reason == null) {
            reason = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("shutdown,");
        stringBuilder.append(reason);
        SystemProperties.set("sys.powerctl", stringBuilder.toString());
    }

    public static void lowLevelReboot(String reason) {
        if (reason == null) {
            reason = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        if (reason.equals("quiescent")) {
            sQuiescent = true;
            reason = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        } else if (reason.endsWith(",quiescent")) {
            sQuiescent = true;
            reason = reason.substring(0, (reason.length() - "quiescent".length()) - 1);
        }
        if (reason.equals("recovery") || reason.equals("recovery-update")) {
            reason = "recovery";
        }
        if (sQuiescent) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(reason);
            stringBuilder.append(",quiescent");
            reason = stringBuilder.toString();
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("reboot,");
        stringBuilder2.append(reason);
        SystemProperties.set("sys.powerctl", stringBuilder2.toString());
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Slog.wtf(TAG, "Unexpected return from lowLevelReboot!");
    }

    public void monitor() {
        synchronized (this.mLock) {
        }
    }

    protected void dumpInternal(PrintWriter pw) {
        WirelessChargerDetector wcd;
        pw.println("POWER MANAGER (dumpsys power)\n");
        synchronized (this.mLock) {
            int i;
            StringBuilder stringBuilder;
            pw.println("Power Manager State:");
            this.mConstants.dump(pw);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDirty=0x");
            stringBuilder2.append(Integer.toHexString(this.mDirty));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mWakefulness=");
            stringBuilder2.append(PowerManagerInternal.wakefulnessToString(this.mWakefulness));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mWakefulnessChanging=");
            stringBuilder2.append(this.mWakefulnessChanging);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mIsPowered=");
            stringBuilder2.append(this.mIsPowered);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mPlugType=");
            stringBuilder2.append(this.mPlugType);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mBatteryLevel=");
            stringBuilder2.append(this.mBatteryLevel);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mBatteryLevelWhenDreamStarted=");
            stringBuilder2.append(this.mBatteryLevelWhenDreamStarted);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDockState=");
            stringBuilder2.append(this.mDockState);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mStayOn=");
            stringBuilder2.append(this.mStayOn);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mProximityPositive=");
            stringBuilder2.append(this.mProximityPositive);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mBootCompleted=");
            stringBuilder2.append(this.mBootCompleted);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mSystemReady=");
            stringBuilder2.append(this.mSystemReady);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mHalAutoSuspendModeEnabled=");
            stringBuilder2.append(this.mHalAutoSuspendModeEnabled);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mHalInteractiveModeEnabled=");
            stringBuilder2.append(this.mHalInteractiveModeEnabled);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mWakeLockSummary=0x");
            stringBuilder2.append(Integer.toHexString(this.mWakeLockSummary));
            pw.println(stringBuilder2.toString());
            pw.print("  mNotifyLongScheduled=");
            if (this.mNotifyLongScheduled == 0) {
                pw.print("(none)");
            } else {
                TimeUtils.formatDuration(this.mNotifyLongScheduled, SystemClock.uptimeMillis(), pw);
            }
            pw.println();
            pw.print("  mNotifyLongDispatched=");
            if (this.mNotifyLongDispatched == 0) {
                pw.print("(none)");
            } else {
                TimeUtils.formatDuration(this.mNotifyLongDispatched, SystemClock.uptimeMillis(), pw);
            }
            pw.println();
            pw.print("  mNotifyLongNextCheck=");
            if (this.mNotifyLongNextCheck == 0) {
                pw.print("(none)");
            } else {
                TimeUtils.formatDuration(this.mNotifyLongNextCheck, SystemClock.uptimeMillis(), pw);
            }
            pw.println();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mUserActivitySummary=0x");
            stringBuilder2.append(Integer.toHexString(this.mUserActivitySummary));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mRequestWaitForNegativeProximity=");
            stringBuilder2.append(this.mRequestWaitForNegativeProximity);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mSandmanScheduled=");
            stringBuilder2.append(this.mSandmanScheduled);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mSandmanSummoned=");
            stringBuilder2.append(this.mSandmanSummoned);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mBatteryLevelLow=");
            stringBuilder2.append(this.mBatteryLevelLow);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mLightDeviceIdleMode=");
            stringBuilder2.append(this.mLightDeviceIdleMode);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDeviceIdleMode=");
            stringBuilder2.append(this.mDeviceIdleMode);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDeviceIdleWhitelist=");
            stringBuilder2.append(Arrays.toString(this.mDeviceIdleWhitelist));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDeviceIdleTempWhitelist=");
            stringBuilder2.append(Arrays.toString(this.mDeviceIdleTempWhitelist));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mLastWakeTime=");
            stringBuilder2.append(TimeUtils.formatUptime(this.mLastWakeTime));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mLastSleepTime=");
            stringBuilder2.append(TimeUtils.formatUptime(this.mLastSleepTime));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mLastUserActivityTime=");
            stringBuilder2.append(TimeUtils.formatUptime(this.mLastUserActivityTime));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mLastUserActivityTimeNoChangeLights=");
            stringBuilder2.append(TimeUtils.formatUptime(this.mLastUserActivityTimeNoChangeLights));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mLastInteractivePowerHintTime=");
            stringBuilder2.append(TimeUtils.formatUptime(this.mLastInteractivePowerHintTime));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mLastScreenBrightnessBoostTime=");
            stringBuilder2.append(TimeUtils.formatUptime(this.mLastScreenBrightnessBoostTime));
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mScreenBrightnessBoostInProgress=");
            stringBuilder2.append(this.mScreenBrightnessBoostInProgress);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDisplayReady=");
            stringBuilder2.append(this.mDisplayReady);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mHoldingWakeLockSuspendBlocker=");
            stringBuilder2.append(this.mHoldingWakeLockSuspendBlocker);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mHoldingDisplaySuspendBlocker=");
            stringBuilder2.append(this.mHoldingDisplaySuspendBlocker);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mBrightnessWaitModeEnabled=");
            stringBuilder2.append(this.mBrightnessWaitModeEnabled);
            pw.println(stringBuilder2.toString());
            pw.println();
            pw.println("Settings and Configuration:");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDecoupleHalAutoSuspendModeFromDisplayConfig=");
            stringBuilder2.append(this.mDecoupleHalAutoSuspendModeFromDisplayConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDecoupleHalInteractiveModeFromDisplayConfig=");
            stringBuilder2.append(this.mDecoupleHalInteractiveModeFromDisplayConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mWakeUpWhenPluggedOrUnpluggedConfig=");
            stringBuilder2.append(this.mWakeUpWhenPluggedOrUnpluggedConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig=");
            stringBuilder2.append(this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mTheaterModeEnabled=");
            stringBuilder2.append(this.mTheaterModeEnabled);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mSuspendWhenScreenOffDueToProximityConfig=");
            stringBuilder2.append(this.mSuspendWhenScreenOffDueToProximityConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDreamsSupportedConfig=");
            stringBuilder2.append(this.mDreamsSupportedConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDreamsEnabledByDefaultConfig=");
            stringBuilder2.append(this.mDreamsEnabledByDefaultConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDreamsActivatedOnSleepByDefaultConfig=");
            stringBuilder2.append(this.mDreamsActivatedOnSleepByDefaultConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDreamsActivatedOnDockByDefaultConfig=");
            stringBuilder2.append(this.mDreamsActivatedOnDockByDefaultConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDreamsEnabledOnBatteryConfig=");
            stringBuilder2.append(this.mDreamsEnabledOnBatteryConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDreamsBatteryLevelMinimumWhenPoweredConfig=");
            stringBuilder2.append(this.mDreamsBatteryLevelMinimumWhenPoweredConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDreamsBatteryLevelMinimumWhenNotPoweredConfig=");
            stringBuilder2.append(this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDreamsBatteryLevelDrainCutoffConfig=");
            stringBuilder2.append(this.mDreamsBatteryLevelDrainCutoffConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDreamsEnabledSetting=");
            stringBuilder2.append(this.mDreamsEnabledSetting);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDreamsActivateOnSleepSetting=");
            stringBuilder2.append(this.mDreamsActivateOnSleepSetting);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDreamsActivateOnDockSetting=");
            stringBuilder2.append(this.mDreamsActivateOnDockSetting);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDozeAfterScreenOff=");
            stringBuilder2.append(this.mDozeAfterScreenOff);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mMinimumScreenOffTimeoutConfig=");
            stringBuilder2.append(this.mMinimumScreenOffTimeoutConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mMaximumScreenDimDurationConfig=");
            stringBuilder2.append(this.mMaximumScreenDimDurationConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mMaximumScreenDimRatioConfig=");
            stringBuilder2.append(this.mMaximumScreenDimRatioConfig);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mScreenOffTimeoutSetting=");
            stringBuilder2.append(this.mScreenOffTimeoutSetting);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mSleepTimeoutSetting=");
            stringBuilder2.append(this.mSleepTimeoutSetting);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mMaximumScreenOffTimeoutFromDeviceAdmin=");
            stringBuilder2.append(this.mMaximumScreenOffTimeoutFromDeviceAdmin);
            stringBuilder2.append(" (enforced=");
            stringBuilder2.append(isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked());
            stringBuilder2.append(")");
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mStayOnWhilePluggedInSetting=");
            stringBuilder2.append(this.mStayOnWhilePluggedInSetting);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mScreenBrightnessSetting=");
            stringBuilder2.append(this.mScreenBrightnessSetting);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mScreenBrightnessModeSetting=");
            stringBuilder2.append(this.mScreenBrightnessModeSetting);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mScreenBrightnessOverrideFromWindowManager=");
            stringBuilder2.append(this.mScreenBrightnessOverrideFromWindowManager);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mUserActivityTimeoutOverrideFromWindowManager=");
            stringBuilder2.append(this.mUserActivityTimeoutOverrideFromWindowManager);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mUserInactiveOverrideFromWindowManager=");
            stringBuilder2.append(this.mUserInactiveOverrideFromWindowManager);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDozeScreenStateOverrideFromDreamManager=");
            stringBuilder2.append(this.mDozeScreenStateOverrideFromDreamManager);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDrawWakeLockOverrideFromSidekick=");
            stringBuilder2.append(this.mDrawWakeLockOverrideFromSidekick);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDozeScreenBrightnessOverrideFromDreamManager=");
            stringBuilder2.append(this.mDozeScreenBrightnessOverrideFromDreamManager);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mScreenBrightnessSettingMinimum=");
            stringBuilder2.append(this.mScreenBrightnessSettingMinimum);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mScreenBrightnessSettingMaximum=");
            stringBuilder2.append(this.mScreenBrightnessSettingMaximum);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mScreenBrightnessSettingDefault=");
            stringBuilder2.append(this.mScreenBrightnessSettingDefault);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mDoubleTapWakeEnabled=");
            stringBuilder2.append(this.mDoubleTapWakeEnabled);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mIsVrModeEnabled=");
            stringBuilder2.append(this.mIsVrModeEnabled);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mForegroundProfile=");
            stringBuilder2.append(this.mForegroundProfile);
            pw.println(stringBuilder2.toString());
            long sleepTimeout = getSleepTimeoutLocked();
            long screenOffTimeout = getScreenOffTimeoutLocked(sleepTimeout);
            long screenDimDuration = getScreenDimDurationLocked(screenOffTimeout);
            pw.println();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Sleep timeout: ");
            stringBuilder3.append(sleepTimeout);
            stringBuilder3.append(" ms");
            pw.println(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Screen off timeout: ");
            stringBuilder3.append(screenOffTimeout);
            stringBuilder3.append(" ms");
            pw.println(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Screen dim duration: ");
            stringBuilder3.append(screenDimDuration);
            stringBuilder3.append(" ms");
            pw.println(stringBuilder3.toString());
            pw.println();
            pw.print("UID states (changing=");
            pw.print(this.mUidsChanging);
            pw.print(" changed=");
            pw.print(this.mUidsChanged);
            pw.println("):");
            int i2 = 0;
            for (i = 0; i < this.mUidState.size(); i++) {
                UidState state = (UidState) this.mUidState.valueAt(i);
                pw.print("  UID ");
                UserHandle.formatUid(pw, this.mUidState.keyAt(i));
                pw.print(": ");
                if (state.mActive) {
                    pw.print("  ACTIVE ");
                } else {
                    pw.print("INACTIVE ");
                }
                pw.print(" count=");
                pw.print(state.mNumWakeLocks);
                pw.print(" state=");
                pw.println(state.mProcState);
            }
            pw.println();
            pw.println("Looper state:");
            this.mHandler.getLooper().dump(new PrintWriterPrinter(pw), "  ");
            pw.println();
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Wake Locks: size=");
            stringBuilder4.append(this.mWakeLocks.size());
            pw.println(stringBuilder4.toString());
            Iterator it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                WakeLock wl = (WakeLock) it.next();
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(wl);
                pw.println(stringBuilder.toString());
            }
            pw.println();
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Suspend Blockers: size=");
            stringBuilder4.append(this.mSuspendBlockers.size());
            pw.println(stringBuilder4.toString());
            it = this.mSuspendBlockers.iterator();
            while (it.hasNext()) {
                SuspendBlocker sb = (SuspendBlocker) it.next();
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(sb);
                pw.println(stringBuilder.toString());
            }
            pw.println();
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Display Power: ");
            stringBuilder4.append(this.mDisplayPowerCallbacks);
            pw.println(stringBuilder4.toString());
            this.mBatterySaverPolicy.dump(pw);
            this.mBatterySaverStateMachine.dump(pw);
            pw.println();
            i = this.mProfilePowerState.size();
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("Profile power states: size=");
            stringBuilder5.append(i);
            pw.println(stringBuilder5.toString());
            while (i2 < i) {
                ProfilePowerState profile = (ProfilePowerState) this.mProfilePowerState.valueAt(i2);
                pw.print("  mUserId=");
                pw.print(profile.mUserId);
                pw.print(" mScreenOffTimeout=");
                pw.print(profile.mScreenOffTimeout);
                pw.print(" mWakeLockSummary=");
                pw.print(profile.mWakeLockSummary);
                pw.print(" mLastUserActivityTime=");
                pw.print(profile.mLastUserActivityTime);
                pw.print(" mLockingNotified=");
                pw.println(profile.mLockingNotified);
                i2++;
            }
            wcd = this.mWirelessChargerDetector;
        }
        if (wcd != null) {
            wcd.dump(pw);
        }
    }

    private void dumpProto(FileDescriptor fd) {
        WirelessChargerDetector wcd;
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        synchronized (this.mLock) {
            long settingsAndConfigurationToken;
            long stayOnWhilePluggedInToken;
            this.mConstants.dumpProto(proto);
            proto.write(1120986464258L, this.mDirty);
            proto.write(1159641169923L, this.mWakefulness);
            proto.write(1133871366148L, this.mWakefulnessChanging);
            proto.write(1133871366149L, this.mIsPowered);
            proto.write(1159641169926L, this.mPlugType);
            proto.write(1120986464263L, this.mBatteryLevel);
            proto.write(1120986464264L, this.mBatteryLevelWhenDreamStarted);
            proto.write(1159641169929L, this.mDockState);
            proto.write(1133871366154L, this.mStayOn);
            proto.write(1133871366155L, this.mProximityPositive);
            proto.write(1133871366156L, this.mBootCompleted);
            proto.write(1133871366157L, this.mSystemReady);
            proto.write(1133871366158L, this.mHalAutoSuspendModeEnabled);
            proto.write(1133871366159L, this.mHalInteractiveModeEnabled);
            long activeWakeLocksToken = proto.start(1146756268048L);
            int i = 0;
            proto.write(1133871366145L, (this.mWakeLockSummary & 1) != 0);
            proto.write(1133871366146L, (this.mWakeLockSummary & 2) != 0);
            proto.write(1133871366147L, (this.mWakeLockSummary & 4) != 0);
            proto.write(1133871366148L, (this.mWakeLockSummary & 8) != 0);
            proto.write(1133871366149L, (this.mWakeLockSummary & 16) != 0);
            proto.write(1133871366150L, (this.mWakeLockSummary & 32) != 0);
            proto.write(1133871366151L, (this.mWakeLockSummary & 64) != 0);
            proto.write(1133871366152L, (this.mWakeLockSummary & 128) != 0);
            proto.end(activeWakeLocksToken);
            proto.write(1112396529681L, this.mNotifyLongScheduled);
            proto.write(1112396529682L, this.mNotifyLongDispatched);
            proto.write(1112396529683L, this.mNotifyLongNextCheck);
            long userActivityToken = proto.start(1146756268052L);
            proto.write(1133871366145L, (this.mUserActivitySummary & 1) != 0);
            proto.write(1133871366146L, (this.mUserActivitySummary & 2) != 0);
            proto.write(1133871366147L, (this.mUserActivitySummary & 4) != 0);
            proto.end(userActivityToken);
            proto.write(1133871366165L, this.mRequestWaitForNegativeProximity);
            proto.write(1133871366166L, this.mSandmanScheduled);
            proto.write(1133871366167L, this.mSandmanSummoned);
            proto.write(1133871366168L, this.mBatteryLevelLow);
            proto.write(1133871366169L, this.mLightDeviceIdleMode);
            proto.write(1133871366170L, this.mDeviceIdleMode);
            for (int write : this.mDeviceIdleWhitelist) {
                proto.write(2220498092059L, write);
            }
            for (int id : this.mDeviceIdleTempWhitelist) {
                proto.write(2220498092060L, id);
            }
            proto.write(1112396529693L, this.mLastWakeTime);
            proto.write(1112396529694L, this.mLastSleepTime);
            proto.write(1112396529695L, this.mLastUserActivityTime);
            proto.write(1112396529696L, this.mLastUserActivityTimeNoChangeLights);
            proto.write(1112396529697L, this.mLastInteractivePowerHintTime);
            proto.write(1112396529698L, this.mLastScreenBrightnessBoostTime);
            proto.write(1133871366179L, this.mScreenBrightnessBoostInProgress);
            proto.write(1133871366180L, this.mDisplayReady);
            proto.write(1133871366181L, this.mHoldingWakeLockSuspendBlocker);
            proto.write(1133871366182L, this.mHoldingDisplaySuspendBlocker);
            long settingsAndConfigurationToken2 = proto.start(1146756268071L);
            proto.write(1133871366145L, this.mDecoupleHalAutoSuspendModeFromDisplayConfig);
            proto.write(1133871366146L, this.mDecoupleHalInteractiveModeFromDisplayConfig);
            proto.write(1133871366147L, this.mWakeUpWhenPluggedOrUnpluggedConfig);
            proto.write(1133871366148L, this.mWakeUpWhenPluggedOrUnpluggedInTheaterModeConfig);
            proto.write(1133871366149L, this.mTheaterModeEnabled);
            proto.write(1133871366150L, this.mSuspendWhenScreenOffDueToProximityConfig);
            proto.write(1133871366151L, this.mDreamsSupportedConfig);
            proto.write(1133871366152L, this.mDreamsEnabledByDefaultConfig);
            proto.write(1133871366153L, this.mDreamsActivatedOnSleepByDefaultConfig);
            proto.write(1133871366154L, this.mDreamsActivatedOnDockByDefaultConfig);
            proto.write(1133871366155L, this.mDreamsEnabledOnBatteryConfig);
            proto.write(1172526071820L, this.mDreamsBatteryLevelMinimumWhenPoweredConfig);
            proto.write(1172526071821L, this.mDreamsBatteryLevelMinimumWhenNotPoweredConfig);
            proto.write(1172526071822L, this.mDreamsBatteryLevelDrainCutoffConfig);
            proto.write(1133871366159L, this.mDreamsEnabledSetting);
            proto.write(1133871366160L, this.mDreamsActivateOnSleepSetting);
            proto.write(1133871366161L, this.mDreamsActivateOnDockSetting);
            proto.write(1133871366162L, this.mDozeAfterScreenOff);
            proto.write(1120986464275L, this.mMinimumScreenOffTimeoutConfig);
            proto.write(1120986464276L, this.mMaximumScreenDimDurationConfig);
            proto.write(1108101562389L, this.mMaximumScreenDimRatioConfig);
            proto.write(1120986464278L, this.mScreenOffTimeoutSetting);
            proto.write(1172526071831L, this.mSleepTimeoutSetting);
            proto.write(1120986464280L, Math.min(this.mMaximumScreenOffTimeoutFromDeviceAdmin, 2147483647L));
            proto.write(1133871366169L, isMaximumScreenOffTimeoutFromDeviceAdminEnforcedLocked());
            long stayOnWhilePluggedInToken2 = proto.start(1146756268058L);
            proto.write(1133871366145L, (this.mStayOnWhilePluggedInSetting & 1) != 0);
            proto.write(1133871366146L, (this.mStayOnWhilePluggedInSetting & 2) != 0);
            proto.write(1133871366147L, (this.mStayOnWhilePluggedInSetting & 4) != 0);
            proto.end(stayOnWhilePluggedInToken2);
            proto.write(1159641169947L, this.mScreenBrightnessModeSetting);
            proto.write(1172526071836L, this.mScreenBrightnessOverrideFromWindowManager);
            proto.write(1176821039133L, this.mUserActivityTimeoutOverrideFromWindowManager);
            proto.write(1133871366174L, this.mUserInactiveOverrideFromWindowManager);
            proto.write(1159641169951L, this.mDozeScreenStateOverrideFromDreamManager);
            proto.write(1133871366180L, this.mDrawWakeLockOverrideFromSidekick);
            proto.write(1108101562400L, this.mDozeScreenBrightnessOverrideFromDreamManager);
            activeWakeLocksToken = proto.start(1146756268065L);
            proto.write(1120986464257L, this.mScreenBrightnessSettingMinimum);
            proto.write(1120986464258L, this.mScreenBrightnessSettingMaximum);
            proto.write(1120986464259L, this.mScreenBrightnessSettingDefault);
            proto.end(activeWakeLocksToken);
            proto.write(1133871366178L, this.mDoubleTapWakeEnabled);
            proto.write(1133871366179L, this.mIsVrModeEnabled);
            proto.end(settingsAndConfigurationToken2);
            long sleepTimeout = getSleepTimeoutLocked();
            activeWakeLocksToken = getScreenOffTimeoutLocked(sleepTimeout);
            long screenDimDuration = getScreenDimDurationLocked(activeWakeLocksToken);
            proto.write(1172526071848L, sleepTimeout);
            proto.write(1120986464297L, activeWakeLocksToken);
            activeWakeLocksToken = screenDimDuration;
            proto.write(1120986464298L, activeWakeLocksToken);
            proto.write(1133871366187L, this.mUidsChanging);
            proto.write(1133871366188L, this.mUidsChanged);
            while (true) {
                int i2 = i;
                if (i2 >= this.mUidState.size()) {
                    break;
                }
                UidState state = (UidState) this.mUidState.valueAt(i2);
                long screenDimDuration2 = activeWakeLocksToken;
                activeWakeLocksToken = proto.start(2246267895853L);
                int uid = this.mUidState.keyAt(i2);
                settingsAndConfigurationToken = settingsAndConfigurationToken2;
                proto.write(1120986464257L, uid);
                proto.write(1138166333442L, UserHandle.formatUid(uid));
                stayOnWhilePluggedInToken = stayOnWhilePluggedInToken2;
                proto.write(1133871366147L, state.mActive);
                proto.write(1120986464260L, state.mNumWakeLocks);
                proto.write(1159641169925L, ActivityManager.processStateAmToProto(state.mProcState));
                proto.end(activeWakeLocksToken);
                i = i2 + 1;
                activeWakeLocksToken = screenDimDuration2;
                settingsAndConfigurationToken2 = settingsAndConfigurationToken;
                stayOnWhilePluggedInToken2 = stayOnWhilePluggedInToken;
            }
            settingsAndConfigurationToken = settingsAndConfigurationToken2;
            stayOnWhilePluggedInToken = stayOnWhilePluggedInToken2;
            this.mBatterySaverStateMachine.dumpProto(proto, 1146756268082L);
            this.mHandler.getLooper().writeToProto(proto, 1146756268078L);
            Iterator it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                ((WakeLock) it.next()).writeToProto(proto, 2246267895855L);
            }
            it = this.mSuspendBlockers.iterator();
            while (it.hasNext()) {
                ((SuspendBlocker) it.next()).writeToProto(proto, 2246267895856L);
            }
            wcd = this.mWirelessChargerDetector;
        }
        if (wcd != null) {
            wcd.writeToProto(proto, 1146756268081L);
        }
        proto.flush();
    }

    private SuspendBlocker createSuspendBlockerLocked(String name) {
        SuspendBlocker suspendBlocker = new SuspendBlockerImpl(name);
        this.mSuspendBlockers.add(suspendBlocker);
        return suspendBlocker;
    }

    private void incrementBootCount() {
        synchronized (this.mLock) {
            int count;
            try {
                count = Global.getInt(getContext().getContentResolver(), "boot_count");
            } catch (SettingNotFoundException e) {
                count = 0;
            }
            Global.putInt(getContext().getContentResolver(), "boot_count", count + 1);
        }
    }

    private static WorkSource copyWorkSource(WorkSource workSource) {
        return workSource != null ? new WorkSource(workSource) : null;
    }

    @VisibleForTesting
    int getLastShutdownReasonInternal(String lastRebootReasonProperty) {
        String line = SystemProperties.get(lastRebootReasonProperty);
        if (line == null) {
            return 0;
        }
        int i = -1;
        switch (line.hashCode()) {
            case -2117951935:
                if (line.equals(REASON_THERMAL_SHUTDOWN)) {
                    i = 3;
                    break;
                }
                break;
            case -1099647817:
                if (line.equals(REASON_LOW_BATTERY)) {
                    i = 4;
                    break;
                }
                break;
            case -934938715:
                if (line.equals(REASON_REBOOT)) {
                    i = 1;
                    break;
                }
                break;
            case -852189395:
                if (line.equals(REASON_USERREQUESTED)) {
                    i = 2;
                    break;
                }
                break;
            case -169343402:
                if (line.equals(REASON_SHUTDOWN)) {
                    i = 0;
                    break;
                }
                break;
            case 1218064802:
                if (line.equals(REASON_BATTERY_THERMAL_STATE)) {
                    i = 5;
                    break;
                }
                break;
        }
        switch (i) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 6;
            default:
                return 0;
        }
    }

    protected boolean isPhoneHeldWakeLock() {
        if ((this.mWakeLockSummary & 16) != 0) {
            Iterator it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                WakeLock wl = (WakeLock) it.next();
                if (incalluiPackageName.equals(wl.mPackageName) && (wl.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 32) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isCarMachineHeldWakeLock() {
        if ((this.mWakeLockSummary & 2) != 0) {
            Iterator it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                WakeLock wl = (WakeLock) it.next();
                if (machineCarPackageName.equals(wl.mPackageName) && (wl.mFlags & NetworkConstants.ARP_HWTYPE_RESERVED_HI) == 10) {
                    return true;
                }
            }
        }
        return false;
    }

    private void setAodStateBySysfs(String file, int command) {
        IOException e;
        String str;
        StringBuilder stringBuilder;
        Slog.i(TAG, "AOD PowerManagerService setAodStateBySysfs()");
        if (mSupportAod) {
            this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            String strCmd = Integer.toString(command);
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(strCmd.getBytes());
                fileOutputStream.flush();
                try {
                    fileOutputStream.close();
                } catch (IOException e2) {
                    e = e2;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            } catch (FileNotFoundException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("File not found: ");
                stringBuilder.append(e3.toString());
                Slog.e(str, stringBuilder.toString());
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e4) {
                        e = e4;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (IOException e5) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error accessing file: ");
                stringBuilder.append(e5.toString());
                Slog.e(str, stringBuilder.toString());
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e6) {
                        e5 = e6;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (Exception e7) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception occur: ");
                stringBuilder.append(e7.toString());
                Slog.e(str, stringBuilder.toString());
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e8) {
                        e5 = e8;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (Throwable th) {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException e9) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error closing file: ");
                        stringBuilder.append(e9.toString());
                        Slog.e(TAG, stringBuilder.toString());
                    }
                }
            }
        }
        return;
        stringBuilder.append("Error closing file: ");
        stringBuilder.append(e5.toString());
        Slog.e(str, stringBuilder.toString());
    }

    private int getAodStateBySysfs(String file) {
        String str;
        StringBuilder stringBuilder;
        IOException e;
        int aodState = -1;
        Slog.i(TAG, "AOD PowerManagerService getAodStateBySysfs()");
        if (!mSupportAod) {
            return -1;
        }
        this.mContext.enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
        BufferedReader reader = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            String tempString = reader.readLine();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getAodStateBySysfs:tempString ");
            stringBuilder.append(tempString);
            Slog.i(str, stringBuilder.toString());
            str = "aod_function =";
            if (tempString != null && tempString.startsWith(str)) {
                tempString = tempString.substring(str.length()).trim();
            }
            aodState = Integer.parseInt(tempString);
            try {
                reader.close();
            } catch (IOException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getAodStateBySysfs IOException");
                stringBuilder.append(e2.toString());
                Slog.e(str, stringBuilder.toString());
            }
            try {
                fis.close();
            } catch (IOException e3) {
                e2 = e3;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
        } catch (NumberFormatException e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NumberFormatException: ");
            stringBuilder.append(e4.toString());
            Slog.e(str, stringBuilder.toString());
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e22) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getAodStateBySysfs IOException");
                    stringBuilder.append(e22.toString());
                    Slog.e(str, stringBuilder.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e5) {
                    e22 = e5;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (FileNotFoundException e6) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("File not found: ");
            stringBuilder.append(e6.toString());
            Slog.e(str, stringBuilder.toString());
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e222) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getAodStateBySysfs IOException");
                    stringBuilder.append(e222.toString());
                    Slog.e(str, stringBuilder.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e7) {
                    e222 = e7;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (IOException e2222) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error accessing file: ");
            stringBuilder.append(e2222.toString());
            Slog.e(str, stringBuilder.toString());
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e22222) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getAodStateBySysfs IOException");
                    stringBuilder.append(e22222.toString());
                    Slog.e(str, stringBuilder.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e8) {
                    e22222 = e8;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Exception e9) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception occur: ");
            stringBuilder.append(e9.toString());
            Slog.e(str, stringBuilder.toString());
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e222222) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getAodStateBySysfs IOException");
                    stringBuilder.append(e222222.toString());
                    Slog.e(str, stringBuilder.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e10) {
                    e222222 = e10;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e11) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getAodStateBySysfs IOException");
                    stringBuilder2.append(e11.toString());
                    Slog.e(str2, stringBuilder2.toString());
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e112) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("e1 IOException ");
                    stringBuilder.append(e112.toString());
                    Slog.e(TAG, stringBuilder.toString());
                }
            }
        }
        return aodState;
        stringBuilder.append("e1 IOException ");
        stringBuilder.append(e222222.toString());
        Slog.e(str, stringBuilder.toString());
        return aodState;
    }

    private boolean isAppCanGetDrawWakeLock(int flags, String packageName, WorkSource ws, String tag) {
        if (this.mAlpmState != 0 || (flags & 128) == 0 || !"Window:StatusBar".equals(tag) || ws == null || !"com.android.systemui".equals(ws.getName(0))) {
            return true;
        }
        Slog.e(TAG, "isCanGetWakeLock:systemui statusbar canot get draw_wake_lock in aod mode");
        return false;
    }

    private void setDozeOverrideFromAodLocked(int screenState, int screenBrightness) {
        Slog.i(TAG, "AOD PowerManagerService setDozeOverrideFromAodLocked()");
        if (mSupportAod) {
            if (!(this.mDozeScreenStateOverrideFromDreamManager == screenState && this.mDozeScreenBrightnessOverrideFromDreamManager == screenBrightness)) {
                this.mDozeScreenStateOverrideFromDreamManager = screenState;
                this.mDozeScreenBrightnessOverrideFromDreamManager = screenBrightness;
                this.mDirty |= 32;
                updatePowerStateLocked();
                if (screenState == 2 || screenState == 3) {
                    if (this.mBackLight != null && this.mBackLight.isHighPrecision()) {
                        screenBrightness = (screenBrightness * 10000) / 255;
                    }
                    this.mDisplayManagerInternal.forceDisplayState(screenState, screenBrightness);
                }
            }
        }
    }

    public void regeditAodStateCallback(IAodStateCallback callback) {
        Slog.i(TAG, "AOD PowerManagerService regeditAodStateCallback()");
        if (mSupportAod) {
            this.mPolicy.regeditAodStateCallback(callback);
        }
    }

    public void unregeditAodStateCallback(IAodStateCallback callback) {
        Slog.i(TAG, "AOD PowerManagerService unregeditAodStateCallback()");
        if (mSupportAod) {
            this.mPolicy.unregeditAodStateCallback(callback);
        }
    }

    protected void sendTempBrightnessToMonitor(String paramType, int brightness) {
    }

    protected void sendBrightnessModeToMonitor(boolean manualMode, String packageName) {
    }

    protected void sendManualBrightnessToMonitor(int brightness, String packageName) {
    }

    protected void sendBootCompletedToMonitor() {
    }

    public boolean getKeyguardLockedStatus() {
        if (this.mKeyguardManager != null) {
            return this.mKeyguardManager.isKeyguardLocked();
        }
        Slog.e(TAG, "mKeyguardManager=null");
        return false;
    }

    private int getCoverModeBrightness() {
        String str;
        StringBuilder stringBuilder;
        int coverModeBrightness = this.mScreenBrightnessSettingDefault;
        if (this.mDisplayManagerInternal != null) {
            coverModeBrightness = this.mDisplayManagerInternal.getCoverModeBrightnessFromLastScreenBrightness();
            if (!(coverModeBrightness == -3 || isValidBrightness(coverModeBrightness))) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("not valid coverModeBrightness=");
                stringBuilder.append(coverModeBrightness);
                stringBuilder.append(",setDefault=");
                stringBuilder.append(this.mScreenBrightnessSettingDefault);
                Slog.e(str, stringBuilder.toString());
                coverModeBrightness = this.mScreenBrightnessSettingDefault;
            }
        }
        if (DEBUG && coverModeBrightness != this.mCoverModeBrightness) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("coverModeBrightness=");
            stringBuilder.append(coverModeBrightness);
            stringBuilder.append(",lastcoverModeBrightness=");
            stringBuilder.append(this.mCoverModeBrightness);
            Slog.d(str, stringBuilder.toString());
        }
        this.mCoverModeBrightness = coverModeBrightness;
        return this.mCoverModeBrightness;
    }

    private void updateAutoBrightnessDBforSeekbar(int level, int state) {
        ContentResolver resolver = this.mContext.getContentResolver();
        int autoBrightnessMode = System.getIntForUser(resolver, "screen_brightness_mode", 0, this.mCurrentUserId);
        int autoBrightnessDB = System.getIntForUser(resolver, "screen_auto_brightness", 0, this.mCurrentUserId);
        if (autoBrightnessMode != 1) {
            return;
        }
        if ((this.mAutoBrightnessLevel != level || autoBrightnessDB != level) && level >= 0 && state != 0) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("LabcCoverMode mAutoBrightnessLevel=");
                stringBuilder.append(this.mAutoBrightnessLevel);
                stringBuilder.append(",level=");
                stringBuilder.append(level);
                stringBuilder.append(",autoBrightnessDB=");
                stringBuilder.append(autoBrightnessDB);
                stringBuilder.append(",state=");
                stringBuilder.append(state);
                Slog.d(str, stringBuilder.toString());
            }
            System.putIntForUser(resolver, "screen_auto_brightness", level, this.mCurrentUserId);
            this.mAutoBrightnessLevel = level;
        }
    }

    public boolean getRebootAutoModeEnable() {
        return this.mDisplayManagerInternal.getRebootAutoModeEnable();
    }

    private boolean needFaceDetect(long nextTimeout, long now, boolean startNoChangeLights) {
        if (!mSupportFaceDetect) {
            return false;
        }
        boolean hasNoChangeLights = this.mLastUserActivityTimeNoChangeLights >= this.mLastWakeTime && this.mLastUserActivityTimeNoChangeLights > this.mLastUserActivityTime;
        if (((this.mUserActivitySummary != 1 || hasNoChangeLights) && !startNoChangeLights) || nextTimeout - now < 1000 || isKeyguardLocked() || (this.mWakeLockSummary & 32) != 0 || this.mStayOn) {
            return false;
        }
        return true;
    }

    private boolean isKeyguardLocked() {
        if (this.mPolicy == null || !this.mPolicy.isKeyguardLocked()) {
            return false;
        }
        return true;
    }

    public HwPowerDAMonitorProxy getPowerMonitor() {
        return this.mPowerProxy;
    }
}
