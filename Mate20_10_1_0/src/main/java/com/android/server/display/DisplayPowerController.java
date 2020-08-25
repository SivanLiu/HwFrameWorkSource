package com.android.server.display;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.AmbientBrightnessDayStats;
import android.hardware.display.BrightnessChangeEvent;
import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.DisplayManagerInternal;
import android.metrics.LogMaker;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.pc.IHwPCManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Flog;
import android.util.HwLog;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.TimeUtils;
import android.view.Display;
import android.zrhung.IZrHung;
import android.zrhung.ZrHungData;
import com.android.internal.app.IBatteryStats;
import com.android.internal.logging.MetricsLogger;
import com.android.server.FingerprintDataInterface;
import com.android.server.HwServiceExFactory;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.NsdService;
import com.android.server.am.BatteryStatsService;
import com.android.server.display.AutomaticBrightnessController;
import com.android.server.display.IHwDisplayPowerControllerEx;
import com.android.server.display.ManualBrightnessController;
import com.android.server.display.RampAnimator;
import com.android.server.display.whitebalance.DisplayWhiteBalanceController;
import com.android.server.display.whitebalance.DisplayWhiteBalanceFactory;
import com.android.server.display.whitebalance.DisplayWhiteBalanceSettings;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.policy.WindowManagerPolicy;
import java.io.PrintWriter;
import java.util.List;

/* access modifiers changed from: package-private */
public final class DisplayPowerController implements AutomaticBrightnessController.Callbacks, ManualBrightnessController.ManualBrightnessCallbacks, DisplayWhiteBalanceController.Callbacks, IHwDisplayPowerControllerEx.Callbacks {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int BRIGHTNESS_FOR_PROXIMITY_POSITIVE = -2;
    private static final int COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 150;
    private static final int COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 250;
    private static final int COVER_MODE_DEFAULT_BRIGHTNESS = 33;
    /* access modifiers changed from: private */
    public static boolean DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static boolean DEBUG_Controller = false;
    private static final boolean DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT = false;
    private static final int GET_RUNNING_TASKS_FROM_AMS_WARNING_DURATION_MILLIS = 500;
    private static final int MSG_CONFIGURE_BRIGHTNESS = 5;
    private static final int MSG_PROXIMITY_SENSOR_DEBOUNCED = 2;
    private static final int MSG_SCREEN_OFF_UNBLOCKED = 4;
    private static final int MSG_SCREEN_ON_EX_UNBLOCKED = 103;
    private static final int MSG_SCREEN_ON_UNBLOCKED = 3;
    private static final int MSG_SET_TEMPORARY_AUTO_BRIGHTNESS_ADJUSTMENT = 7;
    private static final int MSG_SET_TEMPORARY_BRIGHTNESS = 6;
    private static final int MSG_TP_KEEP_STATE_CHANGED = 101;
    private static final int MSG_UPDATE_POWER_STATE = 1;
    private static final int MSG_UPDATE_SCREEN_STATE = 102;
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private static final int PROXIMITY_SENSOR_NEGATIVE_DEBOUNCE_DELAY = 0;
    private static final int PROXIMITY_SENSOR_POSITIVE_DEBOUNCE_DELAY = 0;
    private static final int PROXIMITY_UNKNOWN = -1;
    private static final int RAMP_STATE_SKIP_AUTOBRIGHT = 2;
    private static final int RAMP_STATE_SKIP_INITIAL = 1;
    private static final int RAMP_STATE_SKIP_NONE = 0;
    private static final int REPORTED_TO_POLICY_SCREEN_OFF = 0;
    private static final int REPORTED_TO_POLICY_SCREEN_ON = 2;
    private static final int REPORTED_TO_POLICY_SCREEN_TURNING_OFF = 3;
    private static final int REPORTED_TO_POLICY_SCREEN_TURNING_ON = 1;
    private static final int SCREEN_DIM_MINIMUM_REDUCTION = 10;
    private static final String SCREEN_OFF_BLOCKED_TRACE_NAME = "Screen off blocked";
    private static final String SCREEN_ON_BLOCKED_TRACE_NAME = "Screen on blocked";
    private static final int SCREEN_STATE_HOLD_ON = 2;
    private static final int SCREEN_STATE_OFF = 0;
    private static final int SCREEN_STATE_ON = 1;
    private static final String TAG = "DisplayPowerController";
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final boolean USE_COLOR_FADE_ON_ANIMATION = false;
    private static final boolean mSupportAod = "1".equals(SystemProperties.get("ro.config.support_aod", (String) null));
    private FingerprintDataInterface fpDataCollector;
    private final boolean mAllowAutoBrightnessWhileDozingConfig;
    private boolean mAnimationEnabled;
    private final Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() {
        /* class com.android.server.display.DisplayPowerController.AnonymousClass1 */

        public void onAnimationStart(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            DisplayPowerController.this.sendUpdatePowerState();
        }

        public void onAnimationRepeat(Animator animation) {
        }

        public void onAnimationCancel(Animator animation) {
        }
    };
    private boolean mAppliedAutoBrightness;
    private boolean mAppliedBrightnessBoost;
    private boolean mAppliedDimming;
    private boolean mAppliedLowPower;
    private boolean mAppliedScreenBrightnessOverride;
    private boolean mAppliedTemporaryAutoBrightnessAdjustment;
    private boolean mAppliedTemporaryBrightness;
    private float mAutoBrightnessAdjustment;
    private boolean mAutoBrightnessEnabled = false;
    private AutomaticBrightnessController mAutomaticBrightnessController;
    /* access modifiers changed from: private */
    public Light mBackLight;
    /* access modifiers changed from: private */
    public final IBatteryStats mBatteryStats;
    private final DisplayBlanker mBlanker;
    private boolean mBrightnessBucketsInDozeConfig;
    /* access modifiers changed from: private */
    public BrightnessConfiguration mBrightnessConfiguration;
    private BrightnessMappingStrategy mBrightnessMapper;
    private boolean mBrightnessModeChangeNoClearOffsetEnable = false;
    private boolean mBrightnessModeChanged = false;
    private final int mBrightnessRampRateFast;
    private final int mBrightnessRampRateSlow;
    private BrightnessReason mBrightnessReason = new BrightnessReason();
    private BrightnessReason mBrightnessReasonTemp = new BrightnessReason();
    private final BrightnessTracker mBrightnessTracker;
    /* access modifiers changed from: private */
    public final DisplayManagerInternal.DisplayPowerCallbacks mCallbacks;
    private final Runnable mCleanListener = new Runnable() {
        /* class com.android.server.display.DisplayPowerController.AnonymousClass3 */

        public void run() {
            DisplayPowerController.this.sendUpdatePowerState();
        }
    };
    private final boolean mColorFadeEnabled;
    private boolean mColorFadeFadesConfig;
    private ObjectAnimator mColorFadeOffAnimator;
    private ObjectAnimator mColorFadeOnAnimator;
    /* access modifiers changed from: private */
    public final Context mContext;
    private boolean mCoverModeAnimationFast = false;
    private int mCurrentScreenBrightnessSetting;
    private int mCurrentScreenBrightnessSettingForDB;
    /* access modifiers changed from: private */
    public int mCurrentUserId = 0;
    private boolean mCurrentUserIdChange = false;
    private boolean mDisplayBlanksAfterDozeConfig;
    private HwServiceFactory.IDisplayEffectMonitor mDisplayEffectMonitor;
    private HwServiceFactory.IDisplayEngineInterface mDisplayEngineInterface = null;
    private boolean mDisplayReadyLocked;
    private final DisplayWhiteBalanceController mDisplayWhiteBalanceController;
    private final DisplayWhiteBalanceSettings mDisplayWhiteBalanceSettings;
    private boolean mDozing;
    private int mGlobalAlpmState = -1;
    /* access modifiers changed from: private */
    public final DisplayControllerHandler mHandler;
    /* access modifiers changed from: private */
    public IHwDisplayPowerControllerEx mHwDisplayPowerEx = null;
    /* access modifiers changed from: private */
    public HwServiceFactory.IHwSmartBackLightController mHwSmartBackLightController;
    private int mInitialAutoBrightness;
    private boolean mIsCoverModeClosed = true;
    private int mIsScreenOn = 0;
    private boolean mKeyguardIsLocked = false;
    private int mLastBrightnessForAutoBrightnessDB = 0;
    private int mLastBrightnessTarget;
    private int mLastUserSetScreenBrightness;
    private boolean mLastWaitBrightnessMode;
    private boolean mLightSensorOnEnable = false;
    private final LightsManager mLights;
    private final Object mLock = new Object();
    private ManualBrightnessController mManualBrightnessController = null;
    private int mMillisecond;
    private boolean mModeToAutoNoClearOffsetEnable = false;
    private final Runnable mOnProximityNegativeRunnable = new Runnable() {
        /* class com.android.server.display.DisplayPowerController.AnonymousClass6 */

        public void run() {
            DisplayPowerController.this.mCallbacks.onProximityNegative();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private final Runnable mOnProximityPositiveRunnable = new Runnable() {
        /* class com.android.server.display.DisplayPowerController.AnonymousClass5 */

        public void run() {
            DisplayPowerController.this.mCallbacks.onProximityPositive();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private final Runnable mOnStateChangedRunnable = new Runnable() {
        /* class com.android.server.display.DisplayPowerController.AnonymousClass4 */

        public void run() {
            DisplayPowerController.this.mCallbacks.onStateChanged();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private boolean mOutdoorAnimationFlag = false;
    private float mPendingAutoBrightnessAdjustment;
    private int mPendingProximity = -1;
    private long mPendingProximityDebounceTime = -1;
    private boolean mPendingRequestChangedLocked;
    private DisplayManagerInternal.DisplayPowerRequest mPendingRequestLocked;
    private int mPendingScreenBrightnessSetting;
    private boolean mPendingScreenOff;
    /* access modifiers changed from: private */
    public ScreenOffUnblocker mPendingScreenOffUnblocker;
    /* access modifiers changed from: private */
    public ScreenOnExUnblocker mPendingScreenOnExUnblocker;
    /* access modifiers changed from: private */
    public ScreenOnUnblocker mPendingScreenOnUnblocker;
    private boolean mPendingUpdatePowerStateLocked;
    private boolean mPendingWaitForNegativeProximityLocked;
    /* access modifiers changed from: private */
    public boolean mPowerPolicyChangeFromDimming;
    /* access modifiers changed from: private */
    public DisplayManagerInternal.DisplayPowerRequest mPowerRequest;
    private DisplayPowerState mPowerState;
    private boolean mPoweroffModeChangeAutoEnable = false;
    private int mProximity = -1;
    /* access modifiers changed from: private */
    public boolean mProximityPositive = false;
    private Sensor mProximitySensor;
    /* access modifiers changed from: private */
    public boolean mProximitySensorEnabled;
    private final SensorEventListener mProximitySensorListener = new SensorEventListener() {
        /* class com.android.server.display.DisplayPowerController.AnonymousClass8 */

        public void onSensorChanged(SensorEvent event) {
            if (DisplayPowerController.this.mProximitySensorEnabled) {
                long time = SystemClock.uptimeMillis();
                boolean z = false;
                float distance = event.values[0];
                boolean positive = distance >= 0.0f && distance < DisplayPowerController.this.mProximityThreshold;
                boolean unused = DisplayPowerController.this.mProximitySensorPositive = positive;
                boolean tpKeeped = DisplayPowerController.this.mHwDisplayPowerEx != null && DisplayPowerController.this.mHwDisplayPowerEx.getTpKeep();
                DisplayPowerController displayPowerController = DisplayPowerController.this;
                if (positive || tpKeeped) {
                    z = true;
                }
                displayPowerController.handleProximitySensorEvent(time, z);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    /* access modifiers changed from: private */
    public boolean mProximitySensorPositive;
    /* access modifiers changed from: private */
    public float mProximityThreshold;
    private final RampAnimator.Listener mRampAnimatorListener = new RampAnimator.Listener() {
        /* class com.android.server.display.DisplayPowerController.AnonymousClass2 */

        @Override // com.android.server.display.RampAnimator.Listener
        public void onAnimationEnd() {
            if (DisplayPowerController.this.mUsingHwSmartBackLightController && DisplayPowerController.this.mSmartBackLightEnabled) {
                HwServiceFactory.IHwSmartBackLightController access$400 = DisplayPowerController.this.mHwSmartBackLightController;
                HwServiceFactory.IHwSmartBackLightController unused = DisplayPowerController.this.mHwSmartBackLightController;
                access$400.updateBrightnessState(1);
            }
            if (DisplayPowerController.this.mPowerPolicyChangeFromDimming) {
                boolean unused2 = DisplayPowerController.this.mPowerPolicyChangeFromDimming = false;
                if (DisplayPowerController.DEBUG && DisplayPowerController.this.mPowerRequest != null) {
                    Slog.i(DisplayPowerController.TAG, "update mPowerPolicyChangeFromDimming mPowerRequest.policy=" + DisplayPowerController.this.mPowerRequest.policy);
                }
                if (!(DisplayPowerController.this.mPowerRequest == null || DisplayPowerController.this.mContext == null || DisplayPowerController.this.mPowerRequest.policy != 0)) {
                    Settings.System.putIntForUser(DisplayPowerController.this.mContext.getContentResolver(), "screen_auto_brightness", 0, DisplayPowerController.this.mCurrentUserId);
                    Slog.i(DisplayPowerController.TAG, "update mPowerPolicyChangeFromDimming set screen_auto_brightness db=0 when poweroff from dim");
                }
                DisplayPowerController.this.mBackLight.writeAutoBrightnessDbEnable(true);
            }
            if (DisplayPowerController.this.mProximityPositive) {
                try {
                    DisplayPowerController.this.mBatteryStats.noteScreenBrightness(DisplayPowerController.this.mScreenBrightnessRampAnimator.getCurrentBrightness());
                } catch (RemoteException e) {
                }
            }
            DisplayPowerController.this.sendUpdatePowerState();
        }
    };
    private boolean mRebootWakeupFromSleep = true;
    private int mReportedScreenStateToPolicy;
    private boolean mSREEnabled = false;
    private boolean mSREInitialized = false;
    private final int mScreenBrightnessDefault;
    private final int mScreenBrightnessDimConfig;
    private final int mScreenBrightnessDozeConfig;
    private int mScreenBrightnessForVr;
    private final int mScreenBrightnessForVrDefault;
    private final int mScreenBrightnessForVrRangeMaximum;
    private final int mScreenBrightnessForVrRangeMinimum;
    /* access modifiers changed from: private */
    public RampAnimator<DisplayPowerState> mScreenBrightnessRampAnimator;
    private final int mScreenBrightnessRangeMaximum;
    private final int mScreenBrightnessRangeMinimum;
    private boolean mScreenOffBecauseOfProximity;
    private long mScreenOffBlockStartRealTime;
    private boolean mScreenOnBecauseOfPhoneProximity;
    private long mScreenOnBlockStartRealTime;
    private long mScreenOnExBlockStartRealTime;
    private final SensorManager mSensorManager;
    private int mSetBrightnessNoLimitAnimationTime = 500;
    private final SettingsObserver mSettingsObserver;
    private boolean mShouldWaitScreenOnExBlocker;
    private int mSkipRampState = 0;
    private final boolean mSkipScreenOnBrightnessRamp;
    /* access modifiers changed from: private */
    public boolean mSmartBackLightEnabled;
    private boolean mSmartBackLightSupported;
    /* access modifiers changed from: private */
    public float mTemporaryAutoBrightnessAdjustment;
    /* access modifiers changed from: private */
    public int mTemporaryScreenBrightness;
    private boolean mUnfinishedBusiness;
    private boolean mUseSoftwareAutoBrightnessConfig;
    /* access modifiers changed from: private */
    public boolean mUsingHwSmartBackLightController = false;
    private boolean mUsingSRE = false;
    private boolean mWaitingForNegativeProximity;
    private boolean mWakeupFromSleep = true;
    private final WindowManagerPolicy mWindowManagerPolicy;
    private boolean mfastAnimtionFlag = false;

    static {
        boolean z = false;
        if (Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3))) {
            z = true;
        }
        DEBUG_Controller = z;
    }

    public void setAodAlpmState(int globalState) {
        Slog.i(TAG, "mGlobalAlpmState = " + globalState);
        this.mGlobalAlpmState = globalState;
        int i = this.mGlobalAlpmState;
        if (i == 1) {
            sendUpdatePowerState();
            this.mDisplayEngineInterface.setScene("SCENE_AOD", "ACTION_MODE_OFF");
        } else if (i == 0) {
            this.mDisplayEngineInterface.setScene("SCENE_AOD", "ACTION_MODE_ON");
        }
    }

    public void setBacklightBrightness(PowerManager.BacklightBrightness backlightBrightness) {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.setBacklightBrightness(backlightBrightness);
        }
    }

    public void setCameraModeBrightnessLineEnable(boolean cameraModeBrightnessLineEnable) {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.setCameraModeBrightnessLineEnable(cameraModeBrightnessLineEnable);
        }
    }

    public void updateAutoBrightnessAdjustFactor(float adjustFactor) {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.updateAutoBrightnessAdjustFactor(adjustFactor);
        }
    }

    public int getMaxBrightnessForSeekbar() {
        return this.mManualBrightnessController.getMaxBrightnessForSeekbar();
    }

    public void setBrightnessAnimationTime(boolean animationEnabled, int millisecond) {
        if (DEBUG) {
            Slog.i(TAG, "setAnimationTime animationEnabled=" + animationEnabled + ",millisecond=" + millisecond);
        }
        this.mAnimationEnabled = animationEnabled;
        this.mMillisecond = millisecond;
    }

    public void setKeyguardLockedStatus(boolean isLocked) {
        if (this.mKeyguardIsLocked != isLocked) {
            AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
            if (automaticBrightnessController != null) {
                automaticBrightnessController.setKeyguardLockedStatus(isLocked);
            }
            RampAnimator<DisplayPowerState> rampAnimator = this.mScreenBrightnessRampAnimator;
            if (rampAnimator != null) {
                rampAnimator.updateScreenLockedAnimationEnable(isLocked);
            }
            this.mKeyguardIsLocked = isLocked;
        }
    }

    public boolean getRebootAutoModeEnable() {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController == null) {
            return false;
        }
        return automaticBrightnessController.getRebootAutoModeEnable();
    }

    public DisplayPowerController(Context context, DisplayManagerInternal.DisplayPowerCallbacks callbacks, Handler handler, SensorManager sensorManager, DisplayBlanker blanker) {
        Resources resources;
        String str;
        DisplayPowerController displayPowerController;
        String str2;
        int initialLightSensorRate;
        this.mHandler = new DisplayControllerHandler(handler.getLooper());
        this.mBrightnessTracker = new BrightnessTracker(context, null);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mCallbacks = callbacks;
        this.mBatteryStats = BatteryStatsService.getService();
        this.mLights = (LightsManager) LocalServices.getService(LightsManager.class);
        this.mSensorManager = sensorManager;
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mBlanker = blanker;
        this.mContext = context;
        this.mBackLight = this.mLights.getLight(0);
        Resources resources2 = context.getResources();
        int screenBrightnessSettingMinimum = clampAbsoluteBrightness(resources2.getInteger(17694889));
        this.mScreenBrightnessDozeConfig = clampAbsoluteBrightness(resources2.getInteger(17694883));
        this.mScreenBrightnessDimConfig = clampAbsoluteBrightness(resources2.getInteger(17694882));
        this.mScreenBrightnessRangeMinimum = Math.min(screenBrightnessSettingMinimum, this.mScreenBrightnessDimConfig);
        this.mScreenBrightnessRangeMaximum = clampAbsoluteBrightness(resources2.getInteger(17694888));
        this.mScreenBrightnessDefault = clampAbsoluteBrightness(resources2.getInteger(17694887));
        this.mScreenBrightnessForVrRangeMinimum = clampAbsoluteBrightness(resources2.getInteger(17694886));
        this.mScreenBrightnessForVrRangeMaximum = clampAbsoluteBrightness(resources2.getInteger(17694885));
        this.mScreenBrightnessForVrDefault = clampAbsoluteBrightness(resources2.getInteger(17694884));
        this.mUseSoftwareAutoBrightnessConfig = resources2.getBoolean(17891367);
        this.mAllowAutoBrightnessWhileDozingConfig = resources2.getBoolean(17891342);
        this.mBrightnessRampRateFast = resources2.getInteger(17694752);
        this.mBrightnessRampRateSlow = resources2.getInteger(17694753);
        this.mSkipScreenOnBrightnessRamp = resources2.getBoolean(17891519);
        if (this.mUseSoftwareAutoBrightnessConfig) {
            float dozeScaleFactor = resources2.getFraction(18022406, 1, 1);
            HysteresisLevels ambientBrightnessThresholds = new HysteresisLevels(resources2.getIntArray(17235980), resources2.getIntArray(17235981), resources2.getIntArray(17235982));
            HysteresisLevels screenBrightnessThresholds = new HysteresisLevels(resources2.getIntArray(17236054), resources2.getIntArray(17236057), resources2.getIntArray(17236058));
            long brighteningLightDebounce = (long) resources2.getInteger(17694737);
            long darkeningLightDebounce = (long) resources2.getInteger(17694738);
            boolean autoBrightnessResetAmbientLuxAfterWarmUp = resources2.getBoolean(17891362);
            int lightSensorWarmUpTimeConfig = resources2.getInteger(17694822);
            int lightSensorRate = resources2.getInteger(17694740);
            int initialLightSensorRate2 = resources2.getInteger(17694739);
            if (initialLightSensorRate2 == -1) {
                initialLightSensorRate = lightSensorRate;
            } else {
                if (initialLightSensorRate2 > lightSensorRate) {
                    Slog.w(TAG, "Expected config_autoBrightnessInitialLightSensorRate (" + initialLightSensorRate2 + ") to be less than or equal to config_autoBrightnessLightSensorRate (" + lightSensorRate + ").");
                }
                initialLightSensorRate = initialLightSensorRate2;
            }
            int shortTermModelTimeout = resources2.getInteger(17694741);
            Sensor lightSensor = findDisplayLightSensor(resources2.getString(17039839));
            this.mBrightnessMapper = BrightnessMappingStrategy.create(resources2);
            if (this.mBrightnessMapper != null) {
                HwServiceFactory.IHwAutomaticBrightnessController iadm = HwServiceFactory.getHuaweiAutomaticBrightnessController();
                if (iadm != null) {
                    PackageManager packageManager = context.getPackageManager();
                    Context context2 = this.mContext;
                    str = TAG;
                    displayPowerController = this;
                    resources = resources2;
                    displayPowerController.mAutomaticBrightnessController = iadm.getInstance(displayPowerController, handler.getLooper(), sensorManager, lightSensor, this.mBrightnessMapper, lightSensorWarmUpTimeConfig, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum, dozeScaleFactor, lightSensorRate, initialLightSensorRate, brighteningLightDebounce, darkeningLightDebounce, autoBrightnessResetAmbientLuxAfterWarmUp, ambientBrightnessThresholds, screenBrightnessThresholds, (long) shortTermModelTimeout, packageManager, context2);
                } else {
                    str = TAG;
                    resources = resources2;
                    displayPowerController = this;
                    displayPowerController.mAutomaticBrightnessController = new AutomaticBrightnessController(this, handler.getLooper(), sensorManager, lightSensor, displayPowerController.mBrightnessMapper, lightSensorWarmUpTimeConfig, displayPowerController.mScreenBrightnessRangeMinimum, displayPowerController.mScreenBrightnessRangeMaximum, dozeScaleFactor, lightSensorRate, initialLightSensorRate, brighteningLightDebounce, darkeningLightDebounce, autoBrightnessResetAmbientLuxAfterWarmUp, ambientBrightnessThresholds, screenBrightnessThresholds, (long) shortTermModelTimeout, context.getPackageManager());
                }
            } else {
                str = TAG;
                resources = resources2;
                displayPowerController = this;
                displayPowerController.mUseSoftwareAutoBrightnessConfig = false;
            }
            displayPowerController.fpDataCollector = FingerprintDataInterface.getInstance();
        } else {
            str = TAG;
            resources = resources2;
            displayPowerController = this;
        }
        displayPowerController.mColorFadeEnabled = !ActivityManager.isLowRamDeviceStatic();
        displayPowerController.mColorFadeFadesConfig = resources.getBoolean(17891359);
        displayPowerController.mDisplayBlanksAfterDozeConfig = resources.getBoolean(17891410);
        displayPowerController.mBrightnessBucketsInDozeConfig = resources.getBoolean(17891411);
        displayPowerController.mProximitySensor = displayPowerController.mSensorManager.getDefaultSensor(8);
        Sensor sensor = displayPowerController.mProximitySensor;
        if (sensor != null) {
            displayPowerController.mProximityThreshold = Math.min(sensor.getMaximumRange(), (float) TYPICAL_PROXIMITY_THRESHOLD);
        }
        displayPowerController.mDisplayEngineInterface = HwServiceFactory.getDisplayEngineInterface();
        HwServiceFactory.IDisplayEngineInterface iDisplayEngineInterface = displayPowerController.mDisplayEngineInterface;
        if (iDisplayEngineInterface != null) {
            iDisplayEngineInterface.initialize();
            displayPowerController.mUsingSRE = displayPowerController.mDisplayEngineInterface.getSupported("FEATURE_SRE");
            str2 = str;
            Slog.i(str2, "DisplayEngineInterface getSupported SRE:" + displayPowerController.mUsingSRE);
        } else {
            str2 = str;
        }
        displayPowerController.mCurrentScreenBrightnessSetting = getScreenBrightnessSetting();
        displayPowerController.mScreenBrightnessForVr = getScreenBrightnessForVrSetting();
        displayPowerController.mAutoBrightnessAdjustment = getAutoBrightnessAdjustmentSetting();
        displayPowerController.mTemporaryScreenBrightness = -1;
        displayPowerController.mPendingScreenBrightnessSetting = -1;
        displayPowerController.mTemporaryAutoBrightnessAdjustment = Float.NaN;
        displayPowerController.mPendingAutoBrightnessAdjustment = Float.NaN;
        DisplayWhiteBalanceSettings displayWhiteBalanceSettings = null;
        DisplayWhiteBalanceController displayWhiteBalanceController = null;
        try {
            displayWhiteBalanceSettings = new DisplayWhiteBalanceSettings(displayPowerController.mContext, displayPowerController.mHandler);
            displayWhiteBalanceController = DisplayWhiteBalanceFactory.create(displayPowerController.mHandler, displayPowerController.mSensorManager, resources);
            displayWhiteBalanceSettings.setCallbacks(displayPowerController);
            displayWhiteBalanceController.setCallbacks(displayPowerController);
        } catch (Exception e) {
            Slog.e(str2, "failed to set up display white-balance: " + e);
        }
        displayPowerController.mDisplayWhiteBalanceSettings = displayWhiteBalanceSettings;
        displayPowerController.mDisplayWhiteBalanceController = displayWhiteBalanceController;
        int smartBackLightConfig = SystemProperties.getInt("ro.config.hw_smart_backlight", 1);
        if (displayPowerController.mUsingSRE || smartBackLightConfig == 1) {
            if (displayPowerController.mUsingSRE) {
                Slog.i(str2, "Use SRE instead of SBL");
            } else {
                displayPowerController.mSmartBackLightSupported = true;
                if (DEBUG) {
                    Slog.i(str2, "get ro.config.hw_smart_backlight = 1");
                }
            }
            int smartBackLightSetting = Settings.System.getInt(displayPowerController.mContext.getContentResolver(), "smart_backlight_enable", -1);
            if (smartBackLightSetting == -1) {
                if (DEBUG) {
                    Slog.i(str2, "get Settings.System.SMART_BACKLIGHT failed, set default value to 1");
                }
                Settings.System.putInt(displayPowerController.mContext.getContentResolver(), "smart_backlight_enable", 1);
            } else if (DEBUG) {
                Slog.i(str2, "get Settings.System.SMART_BACKLIGHT = " + smartBackLightSetting);
            }
        } else if (DEBUG) {
            Slog.i(str2, "get ro.config.hw_smart_backlight = " + smartBackLightConfig + ", mUsingSRE = false, don't support sbl or sre");
        }
        if (displayPowerController.mSmartBackLightSupported) {
            displayPowerController.mHwSmartBackLightController = HwServiceFactory.getHwSmartBackLightController();
            HwServiceFactory.IHwSmartBackLightController iHwSmartBackLightController = displayPowerController.mHwSmartBackLightController;
            if (iHwSmartBackLightController != null) {
                displayPowerController.mUsingHwSmartBackLightController = iHwSmartBackLightController.checkIfUsingHwSBL();
                displayPowerController.mHwSmartBackLightController.StartHwSmartBackLightController(displayPowerController.mContext, displayPowerController.mLights, displayPowerController.mSensorManager);
            }
        }
        HwServiceFactory.IHwNormalizedManualBrightnessController iadm2 = HwServiceFactory.getHuaweiManualBrightnessController();
        if (iadm2 != null) {
            displayPowerController.mManualBrightnessController = iadm2.getInstance(displayPowerController, displayPowerController.mContext, displayPowerController.mSensorManager);
            if (DEBUG) {
                Slog.i(str2, "HBM ManualBrightnessController initialized");
            }
        } else {
            displayPowerController.mManualBrightnessController = new ManualBrightnessController(displayPowerController);
        }
        displayPowerController.mDisplayEffectMonitor = HwServiceFactory.getDisplayEffectMonitor(displayPowerController.mContext);
        if (displayPowerController.mDisplayEffectMonitor == null) {
            Slog.e(str2, "getDisplayEffectMonitor failed!");
        }
        displayPowerController.mHwDisplayPowerEx = HwServiceExFactory.getHwDisplayPowerControllerEx(displayPowerController.mContext, displayPowerController);
        IHwDisplayPowerControllerEx iHwDisplayPowerControllerEx = displayPowerController.mHwDisplayPowerEx;
        if (iHwDisplayPowerControllerEx != null) {
            iHwDisplayPowerControllerEx.initTpKeepParamters();
        }
        displayPowerController.mShouldWaitScreenOnExBlocker = displayPowerController.mWindowManagerPolicy.shouldWaitScreenOnExBlocker();
    }

    private Sensor findDisplayLightSensor(String sensorType) {
        if (!TextUtils.isEmpty(sensorType)) {
            List<Sensor> sensors = this.mSensorManager.getSensorList(-1);
            for (int i = 0; i < sensors.size(); i++) {
                Sensor sensor = sensors.get(i);
                if (sensorType.equals(sensor.getStringType())) {
                    return sensor;
                }
            }
        }
        return this.mSensorManager.getDefaultSensor(5);
    }

    public boolean isProximitySensorAvailable() {
        return this.mProximitySensor != null;
    }

    public ParceledListSlice<BrightnessChangeEvent> getBrightnessEvents(int userId, boolean includePackage) {
        return this.mBrightnessTracker.getEvents(userId, includePackage);
    }

    public void onSwitchUser(int newUserId) {
        handleSettingsChange(true);
        this.mBrightnessTracker.onSwitchUser(newUserId);
    }

    public ParceledListSlice<AmbientBrightnessDayStats> getAmbientBrightnessStats(int userId) {
        return this.mBrightnessTracker.getAmbientBrightnessStats(userId);
    }

    public void persistBrightnessTrackerState() {
        this.mBrightnessTracker.persistBrightnessTrackerState();
    }

    /* access modifiers changed from: private */
    public void updateScreenState() {
        blockScreenOn();
        this.mWindowManagerPolicy.setFoldingScreenOffState(true);
        this.mWindowManagerPolicy.screenTurningOn(this.mPendingScreenOnUnblocker);
    }

    private void sendUpdatetScreenStateLocked() {
        if (!this.mPendingUpdatePowerStateLocked) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(102));
        }
    }

    public boolean requestScreenState() {
        synchronized (this.mLock) {
            sendUpdatetScreenStateLocked();
        }
        return true;
    }

    public boolean requestPowerState(DisplayManagerInternal.DisplayPowerRequest request, boolean waitForNegativeProximity) {
        boolean z;
        if (DEBUG && DEBUG_Controller) {
            Slog.d(TAG, "requestPowerState: " + request + ", waitForNegativeProximity=" + waitForNegativeProximity);
        }
        synchronized (this.mLock) {
            boolean changed = false;
            if (waitForNegativeProximity) {
                if (!this.mPendingWaitForNegativeProximityLocked) {
                    this.mPendingWaitForNegativeProximityLocked = true;
                    changed = true;
                }
            }
            if (this.mPendingRequestLocked == null) {
                this.mPendingRequestLocked = new DisplayManagerInternal.DisplayPowerRequest(request);
                changed = true;
            } else if (!this.mPendingRequestLocked.equals(request)) {
                this.mPendingRequestLocked.copyFrom(request);
                changed = true;
            }
            if (changed) {
                this.mDisplayReadyLocked = false;
            }
            if (changed && !this.mPendingRequestChangedLocked) {
                this.mPendingRequestChangedLocked = true;
                sendUpdatePowerStateLocked();
            }
            z = this.mDisplayReadyLocked;
        }
        return z;
    }

    public BrightnessConfiguration getDefaultBrightnessConfiguration() {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController == null) {
            return null;
        }
        return automaticBrightnessController.getDefaultConfig();
    }

    /* access modifiers changed from: private */
    public void sendUpdatePowerState() {
        synchronized (this.mLock) {
            sendUpdatePowerStateLocked();
        }
    }

    private void sendUpdatePowerStateLocked() {
        if (!this.mPendingUpdatePowerStateLocked) {
            this.mPendingUpdatePowerStateLocked = true;
            this.mHandler.sendMessage(this.mHandler.obtainMessage(1));
        }
    }

    private void initialize() {
        this.mPowerState = new DisplayPowerState(this.mBlanker, this.mColorFadeEnabled ? new ColorFade(0) : null);
        if (this.mColorFadeEnabled) {
            this.mColorFadeOnAnimator = ObjectAnimator.ofFloat(this.mPowerState, DisplayPowerState.COLOR_FADE_LEVEL, 0.0f, 1.0f);
            this.mColorFadeOnAnimator.setDuration(250L);
            this.mColorFadeOnAnimator.addListener(this.mAnimatorListener);
            this.mColorFadeOffAnimator = ObjectAnimator.ofFloat(this.mPowerState, DisplayPowerState.COLOR_FADE_LEVEL, 1.0f, 0.0f);
            this.mColorFadeOffAnimator.setDuration(150L);
            this.mColorFadeOffAnimator.addListener(this.mAnimatorListener);
        }
        HwServiceFactory.IHwRampAnimator iadm = HwServiceFactory.getHwNormalizedRampAnimator();
        if (iadm != null) {
            this.mScreenBrightnessRampAnimator = iadm.getInstance(this.mPowerState, DisplayPowerState.SCREEN_BRIGHTNESS, this.mContext);
        } else {
            this.mScreenBrightnessRampAnimator = new RampAnimator<>(this.mPowerState, DisplayPowerState.SCREEN_BRIGHTNESS);
        }
        this.mScreenBrightnessRampAnimator.setListener(this.mRampAnimatorListener);
        try {
            this.mBatteryStats.noteScreenState(this.mPowerState.getScreenState());
            this.mBatteryStats.noteScreenBrightness(this.mPowerState.getScreenBrightness());
        } catch (RemoteException e) {
        }
        float brightness = convertToNits(this.mPowerState.getScreenBrightness());
        if (brightness >= 0.0f) {
            this.mBrightnessTracker.start(brightness);
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_brightness"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_brightness_for_vr"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_auto_brightness_adj"), false, this.mSettingsObserver, -1);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r28v0, resolved type: com.android.server.display.DisplayPowerController */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v38, types: [int, boolean] */
    /* JADX WARN: Type inference failed for: r3v39 */
    /* JADX WARN: Type inference failed for: r3v41 */
    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:100:0x01bb, code lost:
        r12.setTPDozeMode(r28.mPowerRequest.useProximitySensor);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:102:0x01c6, code lost:
        if (r28.mPowerRequest.useProximitySensor == false) goto L_0x01db;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:103:0x01c8, code lost:
        if (r10 == 1) goto L_0x01db;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:104:0x01ca, code lost:
        setProximitySensorEnabled(true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:105:0x01cf, code lost:
        if (r28.mScreenOffBecauseOfProximity != false) goto L_0x0210;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:107:0x01d3, code lost:
        if (r28.mProximity != 1) goto L_0x0210;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:108:0x01d5, code lost:
        r28.mScreenOffBecauseOfProximity = true;
        sendOnProximityPositiveWithWakelock();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:110:0x01dd, code lost:
        if (r28.mWaitingForNegativeProximity == false) goto L_0x01ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:112:0x01e1, code lost:
        if (r28.mScreenOffBecauseOfProximity == false) goto L_0x01ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:114:0x01e5, code lost:
        if (r28.mProximity != 1) goto L_0x01ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:115:0x01e7, code lost:
        if (r10 == 1) goto L_0x01ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:116:0x01e9, code lost:
        setProximitySensorEnabled(true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:118:0x01ef, code lost:
        if (r28.mWaitingForNegativeProximity != false) goto L_0x0205;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:120:0x01f3, code lost:
        if (r28.mScreenOffBecauseOfProximity == false) goto L_0x0205;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:122:0x01f7, code lost:
        if (r28.mProximity == -1) goto L_0x0205;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:123:0x01f9, code lost:
        if (r10 != 1) goto L_0x0205;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:125:0x01ff, code lost:
        if (r28.mPowerRequest.useProximitySensor == false) goto L_0x0205;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:126:0x0201, code lost:
        setProximitySensorEnabled(true);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:128:0x0209, code lost:
        if (r28.mPowerRequest.useProximitySensor != false) goto L_0x020e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:129:0x020b, code lost:
        setProximitySensorEnabled(false);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:130:0x020e, code lost:
        r28.mWaitingForNegativeProximity = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:132:0x0212, code lost:
        if (r28.mScreenOffBecauseOfProximity != false) goto L_0x021d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:134:0x0216, code lost:
        if (r28.mProximity != 1) goto L_0x021d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:135:0x0218, code lost:
        r28.mScreenOffBecauseOfProximity = true;
        sendOnProximityPositiveWithWakelock();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:137:0x021f, code lost:
        if (r28.mScreenOffBecauseOfProximity == false) goto L_0x023b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:139:0x0223, code lost:
        if (r28.mProximity == 1) goto L_0x023b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:140:0x0225, code lost:
        r28.mScreenOffBecauseOfProximity = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:141:0x022b, code lost:
        if (r28.mPowerRequest.useProximitySensor == false) goto L_0x0235;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:143:0x0231, code lost:
        if (r28.mPowerRequest.useProximitySensorbyPhone == false) goto L_0x0235;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:144:0x0233, code lost:
        r28.mScreenOnBecauseOfPhoneProximity = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:145:0x0235, code lost:
        sendOnProximityNegativeWithWakelock();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:146:0x0239, code lost:
        r28.mWaitingForNegativeProximity = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:148:0x023d, code lost:
        if (r28.mScreenOffBecauseOfProximity == false) goto L_0x0246;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:150:0x0243, code lost:
        if (r28.mPowerRequest.useProximitySensorbyPhone != false) goto L_0x0246;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:151:0x0245, code lost:
        r10 = 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:152:0x0246, code lost:
        sre_init(r10);
        hbm_init(r10);
        sendScreenStateToDE(r10);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:153:0x024f, code lost:
        if (r9 == false) goto L_0x026a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:155:0x0253, code lost:
        if (r28.mLastWaitBrightnessMode == false) goto L_0x0263;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:157:0x0259, code lost:
        if (r28.mPowerRequest.brightnessWaitMode != false) goto L_0x0263;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:159:0x025f, code lost:
        if (r28.mPowerRequest.brightnessWaitRet != false) goto L_0x0263;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:160:0x0261, code lost:
        r12 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:161:0x0263, code lost:
        r12 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:162:0x0264, code lost:
        if (r12 != false) goto L_0x0268;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:163:0x0266, code lost:
        r14 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:164:0x0268, code lost:
        r14 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:165:0x0269, code lost:
        r9 = r9 & r14;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:166:0x026a, code lost:
        r12 = r28.mPowerState.getScreenState();
        animateScreenStateChange(r10, r9);
        r10 = r28.mPowerState.getScreenState();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:167:0x027a, code lost:
        if (r10 != 1) goto L_0x0286;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:168:0x027c, code lost:
        r4 = 0;
        r28.mBrightnessReasonTemp.setReason(5);
        r28.mWakeupFromSleep = true;
        r28.mProximityPositive = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:169:0x0286, code lost:
        if (r10 != 5) goto L_0x0290;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:170:0x0288, code lost:
        r4 = r28.mScreenBrightnessForVr;
        r28.mBrightnessReasonTemp.setReason(6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:171:0x0290, code lost:
        if (r4 >= 0) goto L_0x02a7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:173:0x0296, code lost:
        if (r28.mPowerRequest.screenBrightnessOverride <= 0) goto L_0x02a7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:174:0x0298, code lost:
        r4 = r28.mPowerRequest.screenBrightnessOverride;
        r28.mBrightnessReasonTemp.setReason(7);
        r28.mTemporaryScreenBrightness = -1;
        r28.mAppliedScreenBrightnessOverride = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:175:0x02a7, code lost:
        r28.mAppliedScreenBrightnessOverride = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:177:0x02ab, code lost:
        if (r28.mAllowAutoBrightnessWhileDozingConfig == false) goto L_0x02b5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:179:0x02b1, code lost:
        if (android.view.Display.isDozeState(r10) == false) goto L_0x02b5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:180:0x02b3, code lost:
        r14 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:181:0x02b5, code lost:
        r14 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:183:0x02ba, code lost:
        if (r28.mPowerRequest.useAutoBrightness == false) goto L_0x02c8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:184:0x02bc, code lost:
        if (r10 == 2) goto L_0x02c0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:185:0x02be, code lost:
        if (r14 == false) goto L_0x02c8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:186:0x02c0, code lost:
        if (r4 >= 0) goto L_0x02c8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:188:0x02c4, code lost:
        if (r28.mAutomaticBrightnessController == null) goto L_0x02c8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:189:0x02c6, code lost:
        r15 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:190:0x02c8, code lost:
        r15 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:191:0x02c9, code lost:
        r25 = updateUserSetScreenBrightness();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:192:0x02cd, code lost:
        if (r25 != false) goto L_0x02d1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:193:0x02cf, code lost:
        if (r15 == false) goto L_0x02d3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:194:0x02d1, code lost:
        r28.mTemporaryScreenBrightness = -1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:196:0x02d5, code lost:
        if (r28.mTemporaryScreenBrightness <= 0) goto L_0x02e3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:197:0x02d7, code lost:
        r4 = r28.mTemporaryScreenBrightness;
        r28.mAppliedTemporaryBrightness = true;
        r28.mBrightnessReasonTemp.setReason(8);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:198:0x02e3, code lost:
        r28.mAppliedTemporaryBrightness = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:199:0x02e5, code lost:
        r6 = updateAutoBrightnessAdjustment();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:200:0x02e9, code lost:
        if (r6 == false) goto L_0x02ef;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:201:0x02eb, code lost:
        r28.mTemporaryAutoBrightnessAdjustment = Float.NaN;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:203:0x02f5, code lost:
        if (java.lang.Float.isNaN(r28.mTemporaryAutoBrightnessAdjustment) != false) goto L_0x02fd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:204:0x02f7, code lost:
        r13 = r28.mTemporaryAutoBrightnessAdjustment;
        r3 = 1;
        r28.mAppliedTemporaryAutoBrightnessAdjustment = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:205:0x02fd, code lost:
        r13 = r28.mAutoBrightnessAdjustment;
        r3 = 2;
        r28.mAppliedTemporaryAutoBrightnessAdjustment = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:207:0x0306, code lost:
        if (r28.mPowerRequest.boostScreenBrightness == false) goto L_0x0316;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:208:0x0308, code lost:
        if (r4 == 0) goto L_0x0316;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:209:0x030a, code lost:
        r4 = 255;
        r28.mBrightnessReasonTemp.setReason(9);
        r28.mAppliedBrightnessBoost = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:210:0x0316, code lost:
        r28.mAppliedBrightnessBoost = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:211:0x0319, code lost:
        if (r4 >= 0) goto L_0x0321;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:212:0x031b, code lost:
        if (r6 != false) goto L_0x031f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:213:0x031d, code lost:
        if (r25 == false) goto L_0x0321;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:214:0x031f, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:215:0x0321, code lost:
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:216:0x0322, code lost:
        r11 = false;
        r7 = r28.mAutomaticBrightnessController;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:217:0x0325, code lost:
        if (r7 == null) goto L_0x0362;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:218:0x0327, code lost:
        r7.updatePowerPolicy(r28.mPowerRequest.policy);
        r11 = r28.mAutomaticBrightnessController.hasUserDataPoints();
        r27 = r3;
        r28.mAutomaticBrightnessController.configure(r15, r28.mBrightnessConfiguration, ((float) r28.mLastUserSetScreenBrightness) / 255.0f, r25, r13, r6, r28.mPowerRequest.policy);
        r28.mAutomaticBrightnessController.updateCurrentUserId(r28.mPowerRequest.userId);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:219:0x0362, code lost:
        r27 = r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:220:0x0366, code lost:
        r28.mManualBrightnessController.updateCurrentUserId(r28.mPowerRequest.userId);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:221:0x0371, code lost:
        if (r28.mAutoBrightnessEnabled == r15) goto L_0x0398;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:223:0x0375, code lost:
        if (com.android.server.display.DisplayPowerController.DEBUG == false) goto L_0x0396;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:224:0x0377, code lost:
        android.util.Slog.i(com.android.server.display.DisplayPowerController.TAG, "mode change : autoBrightnessEnabled=" + r15 + ",state=" + r10);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:225:0x0396, code lost:
        r28.mAutoBrightnessEnabled = r15;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:226:0x0398, code lost:
        updatemAnimationState(r15);
        updatemManualModeAnimationEnable();
        updateManualPowerSavingAnimationEnable();
        updateManualThermalModeAnimationEnable();
        updateBrightnessModeAnimationEnable();
        updateDarkAdaptDimmingEnable();
        updateFastDarkenDimmingEnable();
        updateNightUpPowerOnWithDimmingEnable();
        r28.mBackLight.updateBrightnessAdjustMode(r15);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:227:0x03bd, code lost:
        if (waitScreenBrightness(r10, r28.mPowerRequest.brightnessWaitMode) == false) goto L_0x03c0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:228:0x03bf, code lost:
        r4 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:230:0x03c2, code lost:
        if (r28.mGlobalAlpmState != 0) goto L_0x03c5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:231:0x03c4, code lost:
        r4 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:233:0x03c6, code lost:
        if (r4 >= 0) goto L_0x0464;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:234:0x03c8, code lost:
        r3 = r13;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:235:0x03c9, code lost:
        if (r15 == false) goto L_0x0429;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:236:0x03cb, code lost:
        r4 = r28.mAutomaticBrightnessController.getAutomaticScreenBrightness();
        r3 = r28.mAutomaticBrightnessController.getAutomaticScreenBrightnessAdjustmentNew(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:237:0x03d7, code lost:
        if (r4 >= 0) goto L_0x0422;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:239:0x03e9, code lost:
        if ((android.os.SystemClock.uptimeMillis() - r28.mAutomaticBrightnessController.getLightSensorEnableTime()) <= 195) goto L_0x0422;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:241:0x03ec, code lost:
        if (r4 == -2) goto L_0x0403;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:242:0x03ee, code lost:
        r18 = false;
        r19 = r3;
        r4 = android.provider.Settings.System.getInt(r28.mContext.getContentResolver(), "screen_brightness", 100);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:243:0x0403, code lost:
        r18 = false;
        r19 = r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:245:0x0409, code lost:
        if (com.android.server.display.DisplayPowerController.DEBUG == false) goto L_0x0426;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:246:0x040b, code lost:
        android.util.Slog.i(com.android.server.display.DisplayPowerController.TAG, "failed to get auto brightness, get SCREEN_BRIGHTNESS:" + r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:247:0x0422, code lost:
        r18 = false;
        r19 = r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:248:0x0426, code lost:
        r3 = r19;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:249:0x0429, code lost:
        r18 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:250:0x042b, code lost:
        if (r4 < 0) goto L_0x044a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:251:0x042d, code lost:
        r2 = clampScreenBrightness(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:252:0x0433, code lost:
        if (r28.mAppliedAutoBrightness == false) goto L_0x043a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:253:0x0435, code lost:
        if (r6 != false) goto L_0x043a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:254:0x0437, code lost:
        r18 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:255:0x043a, code lost:
        putScreenBrightnessSetting(r2);
        r28.mAppliedAutoBrightness = true;
        r28.mBrightnessReasonTemp.setReason(4);
        r4 = r2;
        r2 = r18;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:257:0x044b, code lost:
        if (r4 != -2) goto L_0x044f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:258:0x044d, code lost:
        r2 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:259:0x044f, code lost:
        r2 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:260:0x0450, code lost:
        r28.mAppliedAutoBrightness = r2;
        r2 = r18;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:262:0x0456, code lost:
        if (r13 == r3) goto L_0x045c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:263:0x0458, code lost:
        putAutoBrightnessAdjustmentSetting(r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:264:0x045c, code lost:
        r27 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:265:0x045f, code lost:
        r18 = r2;
        r2 = r27;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:266:0x0464, code lost:
        r18 = false;
        r28.mAppliedAutoBrightness = false;
        r2 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:267:0x046d, code lost:
        if (r4 >= 0) goto L_0x047d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:269:0x0473, code lost:
        if (android.view.Display.isDozeState(r10) == false) goto L_0x047d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:270:0x0475, code lost:
        r4 = r28.mScreenBrightnessDozeConfig;
        r28.mBrightnessReasonTemp.setReason(3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:271:0x047d, code lost:
        if (r4 >= 0) goto L_0x049a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:273:0x0483, code lost:
        if (r28.mPowerRequest.useAutoBrightness != false) goto L_0x049a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:274:0x0485, code lost:
        r4 = clampScreenBrightness(getScreenBrightnessSetting());
        r28.mBrightnessReasonTemp.setReason(1);
        r3 = r28.mAutomaticBrightnessController;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:275:0x0495, code lost:
        if (r3 == null) goto L_0x049a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:276:0x0497, code lost:
        r3.updateIntervenedAutoBrightness(r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:278:0x049b, code lost:
        if (r10 != 2) goto L_0x04cd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:280:0x04a1, code lost:
        if (r28.mPowerRequest.policy != 0) goto L_0x04cd;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:282:0x04a5, code lost:
        if (r4 <= r28.mScreenBrightnessRangeMinimum) goto L_0x04b6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:283:0x04a7, code lost:
        r4 = java.lang.Math.max(java.lang.Math.min(r4 - 10, r28.mScreenBrightnessDimConfig), r28.mScreenBrightnessRangeMinimum);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:284:0x04b6, code lost:
        android.util.Slog.i(com.android.server.display.DisplayPowerController.TAG, "set brightness to DIM brightness:" + r4);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:286:0x04d2, code lost:
        if (r28.mPowerRequest.policy != 2) goto L_0x04fa;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:288:0x04d6, code lost:
        if (r4 <= r28.mScreenBrightnessRangeMinimum) goto L_0x04ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:289:0x04d8, code lost:
        r3 = java.lang.Math.max(java.lang.Math.min(r4 - 10, r28.mScreenBrightnessDimConfig), r28.mScreenBrightnessRangeMinimum);
        r28.mBrightnessReasonTemp.addModifier(1);
        r4 = r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:290:0x04ed, code lost:
        blackScreenOnPcMode();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:291:0x04f2, code lost:
        if (r28.mAppliedDimming != false) goto L_0x04f6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:292:0x04f4, code lost:
        r18 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:293:0x04f6, code lost:
        r28.mAppliedDimming = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:295:0x04fc, code lost:
        if (r28.mAppliedDimming == false) goto L_0x0503;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:296:0x04fe, code lost:
        r18 = false;
        r28.mAppliedDimming = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:298:0x0509, code lost:
        if (r28.mPowerRequest.lowPowerMode == false) goto L_0x0536;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:300:0x050d, code lost:
        if (r4 <= r28.mScreenBrightnessRangeMinimum) goto L_0x052c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:301:0x050f, code lost:
        r4 = java.lang.Math.max((int) (((float) r4) * java.lang.Math.min(r28.mPowerRequest.screenLowPowerBrightnessFactor, 1.0f)), r28.mScreenBrightnessRangeMinimum);
        r28.mBrightnessReasonTemp.addModifier(2);
        r4 = r4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:303:0x052e, code lost:
        if (r28.mAppliedLowPower != false) goto L_0x0532;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:304:0x0530, code lost:
        r18 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:305:0x0532, code lost:
        r28.mAppliedLowPower = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:307:0x0538, code lost:
        if (r28.mAppliedLowPower == false) goto L_0x053f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:308:0x053a, code lost:
        r18 = false;
        r28.mAppliedLowPower = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:310:0x0541, code lost:
        if (r28.mPendingScreenOff != false) goto L_0x0663;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:312:0x0545, code lost:
        if (r28.mSkipScreenOnBrightnessRamp == false) goto L_0x0577;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:314:0x0548, code lost:
        if (r10 != 2) goto L_0x0574;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:316:0x054c, code lost:
        if (r28.mSkipRampState != 0) goto L_0x0558;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:318:0x0550, code lost:
        if (r28.mDozing == false) goto L_0x0558;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:319:0x0552, code lost:
        r28.mInitialAutoBrightness = r4;
        r28.mSkipRampState = 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:321:0x055b, code lost:
        if (r28.mSkipRampState != 1) goto L_0x0569;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:323:0x055f, code lost:
        if (r28.mUseSoftwareAutoBrightnessConfig == false) goto L_0x0569;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:325:0x0563, code lost:
        if (r4 == r28.mInitialAutoBrightness) goto L_0x0569;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:326:0x0565, code lost:
        r28.mSkipRampState = 2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:328:0x056c, code lost:
        if (r28.mSkipRampState != 2) goto L_0x0572;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:329:0x056e, code lost:
        r28.mSkipRampState = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:331:0x0574, code lost:
        r28.mSkipRampState = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:333:0x057f, code lost:
        if (android.common.HwFrameworkFactory.getVRSystemServiceManager().isVRDeviceConnected() == false) goto L_0x0583;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:334:0x0581, code lost:
        r4 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:336:0x0584, code lost:
        if (r10 == 5) goto L_0x058b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:337:0x0586, code lost:
        if (r12 != 5) goto L_0x0589;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:338:0x0589, code lost:
        r3 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:339:0x058b, code lost:
        r3 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:341:0x058d, code lost:
        if (r10 != 2) goto L_0x0595;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:343:0x0591, code lost:
        if (r28.mSkipRampState == 0) goto L_0x0595;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:344:0x0593, code lost:
        r7 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:345:0x0595, code lost:
        r7 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:347:0x059b, code lost:
        if (android.view.Display.isDozeState(r10) == false) goto L_0x05a5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:349:0x05a1, code lost:
        if (r28.mBrightnessBucketsInDozeConfig == false) goto L_0x05a7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:350:0x05a3, code lost:
        r6 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:352:0x05a7, code lost:
        r6 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:354:0x05ac, code lost:
        if (r28.mColorFadeEnabled == false) goto L_0x05bc;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:356:0x05b8, code lost:
        if (r28.mPowerState.getColorFadeLevel() != 1.0f) goto L_0x05bc;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:357:0x05ba, code lost:
        r9 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:358:0x05bc, code lost:
        r9 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:360:0x05c1, code lost:
        if (r28.mAppliedTemporaryBrightness != false) goto L_0x05ca;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:362:0x05c5, code lost:
        if (r28.mAppliedTemporaryAutoBrightnessAdjustment == false) goto L_0x05c8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:363:0x05c8, code lost:
        r12 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:364:0x05ca, code lost:
        r12 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:365:0x05cb, code lost:
        if (r7 != false) goto L_0x062f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:366:0x05cd, code lost:
        if (r6 != false) goto L_0x062f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:367:0x05cf, code lost:
        if (r3 != false) goto L_0x062f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:368:0x05d1, code lost:
        if (r9 == false) goto L_0x062f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:369:0x05d3, code lost:
        if (r12 != false) goto L_0x062f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:371:0x05d9, code lost:
        if (r28.mBrightnessBucketsInDozeConfig == false) goto L_0x05dc;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:373:0x05dd, code lost:
        if (r10 != 2) goto L_0x0614;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:375:0x05e1, code lost:
        if (r28.mWakeupFromSleep == false) goto L_0x0614;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:376:0x05e3, code lost:
        r3 = r28.mAutomaticBrightnessController;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:377:0x05e5, code lost:
        if (r3 == null) goto L_0x060b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:379:0x05eb, code lost:
        if (r3.getRebootFirstBrightnessAnimationEnable() == false) goto L_0x0609;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:381:0x05ef, code lost:
        if (r28.mRebootWakeupFromSleep == false) goto L_0x0609;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:382:0x05f1, code lost:
        if (r4 <= 0) goto L_0x0604;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:383:0x05f4, code lost:
        if (r18 == false) goto L_0x05f9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:384:0x05f6, code lost:
        r3 = r28.mBrightnessRampRateSlow;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:385:0x05f9, code lost:
        r3 = r28.mBrightnessRampRateFast;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:386:0x05fb, code lost:
        animateScreenBrightness(r4, r3);
        r28.mRebootWakeupFromSleep = false;
        r28.mWakeupFromSleep = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:387:0x0604, code lost:
        animateScreenBrightness(r4, 0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:388:0x0609, code lost:
        r3 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:389:0x060b, code lost:
        r3 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:390:0x060c, code lost:
        if (r4 <= 0) goto L_0x0610;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:391:0x060e, code lost:
        r28.mWakeupFromSleep = r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:392:0x0610, code lost:
        animateScreenBrightness(r4, r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:393:0x0614, code lost:
        r3 = r28.mAutomaticBrightnessController;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:394:0x0616, code lost:
        if (r3 == null) goto L_0x0624;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:396:0x061c, code lost:
        if (r3.getSetbrightnessImmediateEnableForCaliTest() == false) goto L_0x0624;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:397:0x061e, code lost:
        animateScreenBrightness(r4, 0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:398:0x0624, code lost:
        if (r18 == false) goto L_0x0629;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:399:0x0626, code lost:
        r3 = r28.mBrightnessRampRateSlow;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:400:0x0629, code lost:
        r3 = r28.mBrightnessRampRateFast;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:401:0x062b, code lost:
        animateScreenBrightness(r4, r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:403:0x0631, code lost:
        if (r12 == false) goto L_0x0645;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:405:0x0637, code lost:
        if (r28.mPowerRequest.useAutoBrightness != false) goto L_0x0645;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:406:0x0639, code lost:
        if (r18 == false) goto L_0x063e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:407:0x063b, code lost:
        r3 = r28.mBrightnessRampRateSlow;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:408:0x063e, code lost:
        r3 = r28.mBrightnessRampRateFast;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:409:0x0640, code lost:
        animateScreenBrightness(r4, r3);
        r3 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:410:0x0645, code lost:
        r3 = false;
        animateScreenBrightness(r4, 0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:411:0x0649, code lost:
        if (r4 <= 0) goto L_0x064d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:412:0x064b, code lost:
        r28.mWakeupFromSleep = r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:413:0x064d, code lost:
        if (r12 != false) goto L_0x0661;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:414:0x064f, code lost:
        if (r0 == false) goto L_0x065c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:415:0x0651, code lost:
        r3 = r28.mAutomaticBrightnessController;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:416:0x0653, code lost:
        if (r3 == null) goto L_0x065b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:418:0x0659, code lost:
        if (r3.hasValidAmbientLux() != false) goto L_0x065c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:419:0x065b, code lost:
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:420:0x065c, code lost:
        notifyBrightnessChanged(r4, r0, r11);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:423:0x066a, code lost:
        r28.mLastWaitBrightnessMode = r28.mPowerRequest.brightnessWaitMode;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:424:0x0678, code lost:
        if (r28.mBrightnessReasonTemp.equals(r28.mBrightnessReason) == false) goto L_0x067c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:425:0x067a, code lost:
        if (r2 == 0) goto L_0x06b6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:426:0x067c, code lost:
        android.util.Slog.v(com.android.server.display.DisplayPowerController.TAG, "Brightness [" + r4 + "] reason changing to: '" + r28.mBrightnessReasonTemp.toString(r2) + "', previous reason: '" + r28.mBrightnessReason + "'.");
        r28.mBrightnessReason.set(r28.mBrightnessReasonTemp);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:428:0x06b8, code lost:
        if (r28.mDisplayWhiteBalanceController == null) goto L_0x06d7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:430:0x06bb, code lost:
        if (r10 != 2) goto L_0x06d1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:432:0x06c3, code lost:
        if (r28.mDisplayWhiteBalanceSettings.isEnabled() == false) goto L_0x06d1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:433:0x06c5, code lost:
        r28.mDisplayWhiteBalanceController.setEnabled(true);
        r28.mDisplayWhiteBalanceController.updateDisplayColorTemperature();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:434:0x06d1, code lost:
        r28.mDisplayWhiteBalanceController.setEnabled(false);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:436:0x06d9, code lost:
        if (r28.mPendingScreenOnUnblocker != null) goto L_0x06ff;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:438:0x06dd, code lost:
        if (r28.mPendingScreenOnExUnblocker != null) goto L_0x06ff;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:440:0x06e1, code lost:
        if (r28.mColorFadeEnabled == false) goto L_0x06f3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:442:0x06e9, code lost:
        if (r28.mColorFadeOnAnimator.isStarted() != false) goto L_0x06ff;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:444:0x06f1, code lost:
        if (r28.mColorFadeOffAnimator.isStarted() != false) goto L_0x06ff;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:446:0x06fb, code lost:
        if (r28.mPowerState.waitUntilClean(r28.mCleanListener) == false) goto L_0x06ff;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:447:0x06fd, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:448:0x06ff, code lost:
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:450:0x0701, code lost:
        if (r0 == false) goto L_0x070d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:452:0x0709, code lost:
        if (r28.mScreenBrightnessRampAnimator.isAnimating() != false) goto L_0x070d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:453:0x070b, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:454:0x070d, code lost:
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:456:0x070f, code lost:
        if (r0 == false) goto L_0x0721;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:458:0x0712, code lost:
        if (r10 == 1) goto L_0x0721;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:460:0x0716, code lost:
        if (r28.mReportedScreenStateToPolicy != 1) goto L_0x0721;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:461:0x0718, code lost:
        setReportedScreenState(2);
        r28.mWindowManagerPolicy.screenTurnedOn();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:462:0x0721, code lost:
        if (r0 != false) goto L_0x073a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:464:0x0725, code lost:
        if (r28.mUnfinishedBusiness != false) goto L_0x073a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:466:0x0729, code lost:
        if (com.android.server.display.DisplayPowerController.DEBUG == false) goto L_0x0732;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:467:0x072b, code lost:
        android.util.Slog.i(com.android.server.display.DisplayPowerController.TAG, "Unfinished business...");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:468:0x0732, code lost:
        r28.mCallbacks.acquireSuspendBlocker();
        r28.mUnfinishedBusiness = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:469:0x073a, code lost:
        if (r0 == false) goto L_0x075b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:470:0x073c, code lost:
        if (r8 == false) goto L_0x075b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:471:0x073e, code lost:
        r9 = r28.mLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:472:0x0740, code lost:
        monitor-enter(r9);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:475:0x0743, code lost:
        if (r28.mPendingRequestChangedLocked != false) goto L_0x0753;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:476:0x0745, code lost:
        r28.mDisplayReadyLocked = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:477:0x074a, code lost:
        if (com.android.server.display.DisplayPowerController.DEBUG == false) goto L_0x0753;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:478:0x074c, code lost:
        android.util.Slog.i(com.android.server.display.DisplayPowerController.TAG, "Display ready!");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:479:0x0753, code lost:
        monitor-exit(r9);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:480:0x0754, code lost:
        sendOnStateChangedWithWakelock();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:484:0x075b, code lost:
        if (r0 == false) goto L_0x077e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:486:0x075f, code lost:
        if (r28.mUnfinishedBusiness == false) goto L_0x077e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:488:0x0763, code lost:
        if (com.android.server.display.DisplayPowerController.DEBUG == false) goto L_0x076c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:489:0x0765, code lost:
        android.util.Slog.i(com.android.server.display.DisplayPowerController.TAG, "Finished business...");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:490:0x076c, code lost:
        r0 = r28.fpDataCollector;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:491:0x076e, code lost:
        if (r0 == null) goto L_0x0775;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:492:0x0770, code lost:
        if (r4 <= 0) goto L_0x0775;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:493:0x0772, code lost:
        r0.reportScreenTurnedOn();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:494:0x0775, code lost:
        r0 = false;
        r28.mUnfinishedBusiness = false;
        r28.mCallbacks.releaseSuspendBlocker();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:495:0x077e, code lost:
        r0 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:497:0x0780, code lost:
        if (r10 == 2) goto L_0x0783;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:498:0x0782, code lost:
        r0 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:499:0x0783, code lost:
        r28.mDozing = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:500:0x0789, code lost:
        if (r5 == r28.mPowerRequest.policy) goto L_?;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:501:0x078b, code lost:
        logDisplayPolicyChanged(r28.mPowerRequest.policy);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:507:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:508:?, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x017e, code lost:
        if (r2 == false) goto L_0x0183;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x0180, code lost:
        initialize();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x0183, code lost:
        r4 = -1;
        r4 = -1;
        r4 = -1;
        r4 = -1;
        r9 = false;
        r9 = false;
        r9 = false;
        r9 = false;
        r10 = r28.mPowerRequest.policy;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x018a, code lost:
        if (r10 == 0) goto L_0x01ae;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x018c, code lost:
        if (r10 == 1) goto L_0x0194;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:85:0x018e, code lost:
        if (r10 == 4) goto L_0x0192;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:86:0x0190, code lost:
        r10 = 2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:87:0x0192, code lost:
        r10 = 5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:89:0x0198, code lost:
        if (r28.mPowerRequest.dozeScreenState == 0) goto L_0x019f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:90:0x019a, code lost:
        r10 = r28.mPowerRequest.dozeScreenState;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:91:0x019f, code lost:
        r10 = 3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:93:0x01a2, code lost:
        if (r28.mAllowAutoBrightnessWhileDozingConfig != false) goto L_0x01b1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:94:0x01a4, code lost:
        r4 = r28.mPowerRequest.dozeScreenBrightness;
        r28.mBrightnessReasonTemp.setReason(2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:95:0x01ae, code lost:
        r10 = 1;
        r9 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:97:0x01b5, code lost:
        if (r28.mProximitySensor == null) goto L_0x0239;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:98:0x01b7, code lost:
        r12 = r28.mHwDisplayPowerEx;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:99:0x01b9, code lost:
        if (r12 == null) goto L_0x01c2;
     */
    public void updatePowerState() {
        int previousPolicy;
        boolean mustInitialize = false;
        mustInitialize = false;
        this.mBrightnessReasonTemp.set(null);
        synchronized (this.mLock) {
            try {
                this.mPendingUpdatePowerStateLocked = false;
                if (this.mPendingRequestLocked != null) {
                    if (this.mPowerRequest == null) {
                        this.mPowerRequest = new DisplayManagerInternal.DisplayPowerRequest(this.mPendingRequestLocked);
                        this.mWaitingForNegativeProximity = this.mPendingWaitForNegativeProximityLocked;
                        this.mPendingWaitForNegativeProximityLocked = false;
                        this.mPendingRequestChangedLocked = false;
                        mustInitialize = true;
                        previousPolicy = 3;
                    } else if (this.mPendingRequestChangedLocked) {
                        previousPolicy = this.mPowerRequest.policy;
                        this.mBrightnessModeChanged = this.mPowerRequest.useAutoBrightness != this.mPendingRequestLocked.useAutoBrightness;
                        if (this.mBrightnessModeChanged && this.mPowerRequest.useAutoBrightness) {
                            updateBrightnessModeChangeManualState(this.mPowerRequest.useAutoBrightness);
                        }
                        if (this.mBrightnessModeChanged && this.mPowerRequest.screenBrightnessOverride > 0 && this.mPendingRequestLocked.screenBrightnessOverride < 0) {
                            this.mBrightnessModeChanged = false;
                            if (DEBUG) {
                                Slog.i(TAG, "mBrightnessModeChanged without db,brightness=" + this.mPowerRequest.screenBrightnessOverride + ",mPendingBrightness=" + this.mPendingRequestLocked.screenBrightnessOverride);
                            }
                        }
                        if (this.mPowerRequest.policy == 2 && this.mPendingRequestLocked.policy != 2) {
                            this.mPowerPolicyChangeFromDimming = true;
                        }
                        if (this.mCurrentUserId != this.mPendingRequestLocked.userId) {
                            this.mCurrentUserIdChange = true;
                            this.mCurrentUserId = this.mPendingRequestLocked.userId;
                            this.mBackLight.updateUserId(this.mCurrentUserId);
                        }
                        this.mPowerRequest.copyFrom(this.mPendingRequestLocked);
                        boolean isClosed = HwServiceFactory.isCoverClosed();
                        boolean isCoverModeChanged = false;
                        if (isClosed != this.mIsCoverModeClosed) {
                            this.mIsCoverModeClosed = isClosed;
                            isCoverModeChanged = true;
                        }
                        updateCoverModeStatus(isClosed);
                        boolean modeChangeKeepBrightnessOffset = this.mAutomaticBrightnessController.getGameDisableAutoBrightnessModeKeepOffsetEnable();
                        if (this.mBrightnessModeChanged && !this.mCurrentUserIdChange && this.mPowerRequest.useAutoBrightness && !isCoverModeChanged && !this.mIsCoverModeClosed && !this.mBrightnessModeChangeNoClearOffsetEnable && !this.mModeToAutoNoClearOffsetEnable && !modeChangeKeepBrightnessOffset) {
                            updateAutoBrightnessAdjustFactor(0.0f);
                            if (DEBUG) {
                                Slog.i(TAG, "AdjustPositionBrightness set 0");
                            }
                        }
                        if (this.mBrightnessModeChangeNoClearOffsetEnable) {
                            this.mBrightnessModeChangeNoClearOffsetEnable = false;
                            Slog.i(TAG, "set mBrightnessModeChangeNoClearOffsetEnable=" + this.mBrightnessModeChangeNoClearOffsetEnable);
                        }
                        if (this.mBrightnessModeChanged && this.mModeToAutoNoClearOffsetEnable) {
                            this.mModeToAutoNoClearOffsetEnable = false;
                            Slog.i(TAG, "set mModeToAutoNoClearOffsetEnable1=" + this.mModeToAutoNoClearOffsetEnable);
                        }
                        if (this.mBrightnessModeChanged && modeChangeKeepBrightnessOffset) {
                            this.mAutomaticBrightnessController.setGameDisableAutoBrightnessModeKeepOffsetEnable(false);
                            Slog.i(TAG, "setGameDisableAutoBrightnessModeKeepOffsetEnable=false");
                        }
                        this.mCurrentUserIdChange = false;
                        this.mWaitingForNegativeProximity |= this.mPendingWaitForNegativeProximityLocked;
                        this.mPendingWaitForNegativeProximityLocked = false;
                        this.mPendingRequestChangedLocked = false;
                        this.mDisplayReadyLocked = false;
                        writeAutoBrightnessDbEnable(this.mPowerRequest.useAutoBrightness);
                    } else {
                        previousPolicy = this.mPowerRequest.policy;
                    }
                    try {
                        boolean mustNotify = !this.mDisplayReadyLocked;
                        this.mScreenOnBecauseOfPhoneProximity = false;
                    } catch (Throwable th) {
                        th = th;
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    private void blackScreenOnPcMode() {
        if ((HwPCUtils.isPcCastModeInServer() || HwPCUtils.getPhoneDisplayID() != -1) && !HwPCUtils.enabledInPad() && !HwPCUtils.isDisallowLockScreenForHwMultiDisplay()) {
            HwPCUtils.log(TAG, "black Screen in PC mode");
            try {
                IHwPCManager pcMgr = HwPCUtils.getHwPCManager();
                if (pcMgr != null) {
                    pcMgr.setScreenPower(false);
                }
            } catch (RemoteException e) {
                HwPCUtils.log(TAG, "blackScreenOnPcMode RemoteException.");
            }
        }
    }

    private void sre_init(int state) {
        boolean lightSensorOnEnable;
        if (this.mSmartBackLightSupported && this.mSmartBackLightEnabled != this.mPowerRequest.useSmartBacklight) {
            if (DEBUG) {
                Slog.i(TAG, "mPowerRequest.useSmartBacklight change " + this.mSmartBackLightEnabled + " -> " + this.mPowerRequest.useSmartBacklight);
            }
            this.mSmartBackLightEnabled = this.mPowerRequest.useSmartBacklight;
        }
        if (this.mUsingHwSmartBackLightController) {
            this.mHwSmartBackLightController.updatePowerState(state, this.mSmartBackLightEnabled);
        }
        if (!(this.mDisplayEngineInterface == null || this.mLightSensorOnEnable == (lightSensorOnEnable = wantScreenOn(state)))) {
            Slog.i(TAG, "LightSensorEnable change " + this.mLightSensorOnEnable + " -> " + lightSensorOnEnable);
            this.mDisplayEngineInterface.updateLightSensorState(lightSensorOnEnable);
            this.mLightSensorOnEnable = lightSensorOnEnable;
        }
        if (this.mUsingSRE && this.mDisplayEngineInterface != null) {
            if (!this.mSREInitialized || this.mSREEnabled != this.mPowerRequest.useSmartBacklight) {
                this.mSREInitialized = true;
                this.mSREEnabled = this.mPowerRequest.useSmartBacklight;
                Slog.i(TAG, "mPowerRequest.useSmartBacklight : " + this.mSREEnabled);
                if (this.mSREEnabled) {
                    this.mDisplayEngineInterface.setScene("SCENE_SRE", "ACTION_MODE_ON");
                } else {
                    this.mDisplayEngineInterface.setScene("SCENE_SRE", "ACTION_MODE_OFF");
                }
            }
        }
    }

    private void hbm_init(int state) {
        if (SystemProperties.getInt("ro.config.hw_high_bright_mode", 1) == 1 && getManualModeEnable()) {
            boolean isManulMode = true ^ this.mPowerRequest.useAutoBrightness;
            AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
            if (automaticBrightnessController != null) {
                automaticBrightnessController.setManualModeEnableForPg(isManulMode);
            }
            this.mManualBrightnessController.updatePowerState(state, isManulMode);
        }
    }

    private void sendScreenStateToDE(int state) {
        int currentState;
        if (this.mDisplayEngineInterface != null && this.mIsScreenOn != (currentState = getScreenOnState(state))) {
            Slog.i(TAG, "ScreenState change " + this.mIsScreenOn + " -> " + currentState);
            if (currentState == 1) {
                this.mDisplayEngineInterface.setScene("SCENE_REAL_POWERMODE", "ACTION_MODE_ON");
                HwServiceFactory.setPowerState(0);
            } else {
                this.mDisplayEngineInterface.setScene("SCENE_REAL_POWERMODE", "ACTION_MODE_OFF");
                HwServiceFactory.setPowerState(1);
            }
            this.mIsScreenOn = currentState;
        }
    }

    private void updateCoverModeStatus(boolean isClosed) {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.setCoverModeStatus(isClosed);
        }
    }

    @Override // com.android.server.display.AutomaticBrightnessController.Callbacks
    public void updateBrightness() {
        sendUpdatePowerState();
    }

    @Override // com.android.server.display.ManualBrightnessController.ManualBrightnessCallbacks
    public void updateManualBrightnessForLux() {
        sendUpdatePowerState();
    }

    public void setBrightnessConfiguration(BrightnessConfiguration c) {
        this.mHandler.obtainMessage(5, c).sendToTarget();
    }

    public void setTemporaryBrightness(int brightness) {
        this.mHandler.obtainMessage(6, brightness, 0).sendToTarget();
    }

    public void setTemporaryAutoBrightnessAdjustment(float adjustment) {
        this.mHandler.obtainMessage(7, Float.floatToIntBits(adjustment), 0).sendToTarget();
    }

    private void blockScreenOn() {
        if (this.mPendingScreenOnUnblocker == null) {
            Trace.asyncTraceBegin(131072, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
            this.mPendingScreenOnUnblocker = new ScreenOnUnblocker();
            this.mScreenOnBlockStartRealTime = SystemClock.elapsedRealtime();
            Flog.i((int) NsdService.NativeResponseCode.SERVICE_FOUND, "UL_Power Blocking screen on until initial contents have been drawn.");
            IZrHung iZrHung = HwFrameworkFactory.getZrHung("zrhung_wp_screenon_framework");
            if (iZrHung != null) {
                ZrHungData arg = new ZrHungData();
                arg.putString("addScreenOnInfo", "Blocking screen on until initial contents have been drawn");
                iZrHung.addInfo(arg);
            }
        }
    }

    /* access modifiers changed from: private */
    public void unblockScreenOn() {
        if (this.mPendingScreenOnUnblocker != null) {
            this.mPendingScreenOnUnblocker = null;
            long delay = SystemClock.elapsedRealtime() - this.mScreenOnBlockStartRealTime;
            Flog.i((int) NsdService.NativeResponseCode.SERVICE_FOUND, "UL_Power Unblocked screen on after " + delay + " ms");
            Trace.asyncTraceEnd(131072, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
            IZrHung iZrHung = HwFrameworkFactory.getZrHung("zrhung_wp_screenon_framework");
            if (iZrHung != null) {
                ZrHungData arg = new ZrHungData();
                arg.putString("addScreenOnInfo", "Unblocked screen on after " + delay + " ms");
                iZrHung.addInfo(arg);
            }
        }
    }

    private void blockScreenOnEx() {
        if (this.mShouldWaitScreenOnExBlocker && this.mPendingScreenOnExUnblocker == null) {
            this.mPendingScreenOnExUnblocker = new ScreenOnExUnblocker();
            this.mScreenOnExBlockStartRealTime = SystemClock.elapsedRealtime();
            Flog.i((int) NsdService.NativeResponseCode.SERVICE_FOUND, "UL_Power Blocking screen on until additional contents has been ready.");
        }
    }

    /* access modifiers changed from: private */
    public void unblockScreenOnEx() {
        if (this.mShouldWaitScreenOnExBlocker && this.mPendingScreenOnExUnblocker != null) {
            this.mPendingScreenOnExUnblocker = null;
            Flog.i((int) NsdService.NativeResponseCode.SERVICE_FOUND, "UL_Power Unblocked screen on ex after " + (SystemClock.elapsedRealtime() - this.mScreenOnExBlockStartRealTime) + " ms");
        }
    }

    private void blockScreenOff() {
        if (this.mPendingScreenOffUnblocker == null) {
            Trace.asyncTraceBegin(131072, SCREEN_OFF_BLOCKED_TRACE_NAME, 0);
            this.mPendingScreenOffUnblocker = new ScreenOffUnblocker();
            this.mScreenOffBlockStartRealTime = SystemClock.elapsedRealtime();
            Slog.i(TAG, "UL_Power Blocking screen off");
        }
    }

    /* access modifiers changed from: private */
    public void unblockScreenOff() {
        if (this.mPendingScreenOffUnblocker != null) {
            this.mPendingScreenOffUnblocker = null;
            long delay = SystemClock.elapsedRealtime() - this.mScreenOffBlockStartRealTime;
            Slog.i(TAG, "UL_PowerUnblocked screen off after " + delay + " ms");
            Trace.asyncTraceEnd(131072, SCREEN_OFF_BLOCKED_TRACE_NAME, 0);
        }
    }

    private boolean setScreenState(int state) {
        return setScreenState(state, false);
    }

    private boolean setScreenState(int state, boolean reportOnly) {
        boolean isOff = state == 1;
        if (this.mPowerState.getScreenState() != state) {
            if (isOff && !this.mScreenOffBecauseOfProximity && !this.mScreenOnBecauseOfPhoneProximity) {
                if (this.mReportedScreenStateToPolicy == 2) {
                    setReportedScreenState(3);
                    blockScreenOff();
                    this.mWindowManagerPolicy.screenTurningOff(this.mPendingScreenOffUnblocker);
                    unblockScreenOff();
                } else if (this.mPendingScreenOffUnblocker != null) {
                    return false;
                }
            }
            if (!reportOnly) {
                Trace.traceCounter(131072, "ScreenState", state);
                this.mPowerState.setScreenState(state);
                try {
                    this.mBatteryStats.noteScreenState(state);
                } catch (RemoteException e) {
                }
            }
        }
        boolean isDoze = mSupportAod && (state == 3 || state == 4);
        if (isOff && this.mReportedScreenStateToPolicy != 0 && !this.mScreenOffBecauseOfProximity && !this.mScreenOnBecauseOfPhoneProximity) {
            setReportedScreenState(0);
            unblockScreenOn();
            unblockScreenOnEx();
            this.mWindowManagerPolicy.screenTurnedOff();
            setBrightnessAnimationTime(false, 500);
            setBrightnessNoLimit(-1, 500);
            boolean z = this.mPoweroffModeChangeAutoEnable;
            this.mBrightnessModeChangeNoClearOffsetEnable = z;
            if (z) {
                this.mPoweroffModeChangeAutoEnable = false;
                Slog.i(TAG, "poweroff set mPoweroffModeChangeAutoEnable=" + this.mPoweroffModeChangeAutoEnable);
            }
        } else if (!isOff && !isDoze && this.mReportedScreenStateToPolicy == 3) {
            unblockScreenOff();
            this.mWindowManagerPolicy.screenTurnedOff();
            setReportedScreenState(0);
        }
        if (!isOff && !isDoze && this.mReportedScreenStateToPolicy == 0) {
            setReportedScreenState(1);
            if (this.mPowerState.getColorFadeLevel() == 0.0f) {
                blockScreenOn();
                blockScreenOnEx();
            } else {
                unblockScreenOn();
                unblockScreenOnEx();
            }
            this.mWindowManagerPolicy.screenTurningOn(this.mPendingScreenOnUnblocker);
            if (this.mShouldWaitScreenOnExBlocker) {
                this.mWindowManagerPolicy.screenTurningOnEx(this.mPendingScreenOnExUnblocker);
            }
        }
        return this.mPendingScreenOnUnblocker == null && this.mPendingScreenOnExUnblocker == null;
    }

    private void setReportedScreenState(int state) {
        Trace.traceCounter(131072, "ReportedScreenStateToPolicy", state);
        this.mReportedScreenStateToPolicy = state;
    }

    private boolean waitScreenBrightness(int displayState, boolean curReqWaitBright) {
        if (DEBUG && DEBUG_Controller) {
            Slog.i(TAG, "waitScreenBrightness displayState = " + displayState + " curReqWaitBright = " + curReqWaitBright);
        }
        if (displayState != 2 || !curReqWaitBright) {
            return false;
        }
        return true;
    }

    private int clampScreenBrightnessForVr(int value) {
        return MathUtils.constrain(value, this.mScreenBrightnessForVrRangeMinimum, this.mScreenBrightnessForVrRangeMaximum);
    }

    private int clampScreenBrightness(int value) {
        return MathUtils.constrain(value, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum);
    }

    private void animateScreenBrightness(int target, int rate) {
        int brightnessDB;
        int brightnessTargetReal = target;
        DisplayManagerInternal.DisplayPowerRequest displayPowerRequest = this.mPowerRequest;
        if (!(displayPowerRequest == null || this.mContext == null || displayPowerRequest.useAutoBrightness || this.mLastBrightnessForAutoBrightnessDB == 0 || target != 0 || (brightnessDB = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", this.mScreenBrightnessDefault, this.mCurrentUserId)) == 0)) {
            Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", 0, this.mCurrentUserId);
            if (DEBUG) {
                Slog.i(TAG, "LabcCoverMode manualMode set screen_auto_brightness db=0 when poweroff,OrigbrightnessDB=" + brightnessDB);
            }
        }
        this.mLastBrightnessForAutoBrightnessDB = target;
        DisplayManagerInternal.DisplayPowerRequest displayPowerRequest2 = this.mPowerRequest;
        if (displayPowerRequest2 != null && !displayPowerRequest2.useAutoBrightness && brightnessTargetReal > 0) {
            this.mManualBrightnessController.updateManualBrightness(brightnessTargetReal);
            brightnessTargetReal = this.mManualBrightnessController.getManualBrightness();
            if (this.mManualBrightnessController.needFastestRateForManualBrightness()) {
                rate = 0;
            }
            AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
            if (automaticBrightnessController != null) {
                automaticBrightnessController.setBackSensorCoverModeBrightness(brightnessTargetReal);
            }
        }
        if (this.mPowerRequest != null) {
            Slog.i(TAG, "Animating brightness: target=" + target + ", rate=" + rate + ",brightnessTargetReal=" + brightnessTargetReal + ",AutoBrightness=" + this.mPowerRequest.useAutoBrightness);
        }
        if (target >= 0 && brightnessTargetReal >= 0) {
            if (target == 0 && rate != 0) {
                rate = 0;
                Slog.e(TAG, "Animating brightness rate is invalid when screen off, set rate to 0");
            }
            if (this.mScreenBrightnessRampAnimator.animateTo(brightnessTargetReal, rate)) {
                if (this.mUsingHwSmartBackLightController && this.mSmartBackLightEnabled && rate > 0) {
                    if (this.mScreenBrightnessRampAnimator.isAnimating()) {
                        this.mHwSmartBackLightController.updateBrightnessState(0);
                    } else if (DEBUG) {
                        Slog.i(TAG, "brightness changed but not animating");
                    }
                }
                Trace.traceCounter(131072, "TargetScreenBrightness", target);
                try {
                    HwLog.dubaie("DUBAI_TAG_BRIGHTNESS", "brightness=" + brightnessTargetReal);
                    this.mBatteryStats.noteScreenBrightness(target);
                    sendTargetBrightnessToMonitor(target);
                } catch (RemoteException e) {
                }
            } else {
                DisplayManagerInternal.DisplayPowerRequest displayPowerRequest3 = this.mPowerRequest;
                if (!(displayPowerRequest3 == null || displayPowerRequest3.useAutoBrightness || this.mLastBrightnessTarget == target)) {
                    Trace.traceCounter(131072, "TargetScreenBrightness", target);
                    try {
                        this.mBatteryStats.noteScreenBrightness(target);
                    } catch (RemoteException e2) {
                    }
                }
            }
            this.mLastBrightnessTarget = target;
        }
    }

    private void animateScreenStateChange(int target, boolean performScreenOffTransition) {
        synchronized (this.mLock) {
            if (this.mScreenOffBecauseOfProximity) {
                this.mPowerState.mScreenChangedReason = 100;
            } else {
                this.mPowerState.mScreenChangedReason = this.mPendingRequestLocked.mScreenChangeReason;
            }
            Slog.i(TAG, "mPowerState.mScreenChangedReason: " + this.mPowerState.mScreenChangedReason + " mPendingRequestLocked.mScreenChangeReason " + this.mPendingRequestLocked.mScreenChangeReason);
        }
        int i = 2;
        if (this.mColorFadeEnabled && (this.mColorFadeOnAnimator.isStarted() || this.mColorFadeOffAnimator.isStarted())) {
            if (target == 2) {
                this.mPendingScreenOff = false;
            } else {
                return;
            }
        }
        if (this.mDisplayBlanksAfterDozeConfig && Display.isDozeState(this.mPowerState.getScreenState()) && !Display.isDozeState(target)) {
            this.mPowerState.prepareColorFade(this.mContext, this.mColorFadeFadesConfig ? 2 : 0);
            ObjectAnimator objectAnimator = this.mColorFadeOffAnimator;
            if (objectAnimator != null) {
                objectAnimator.end();
            }
            setScreenState(1, target != 1);
        }
        if (this.mPendingScreenOff && target != 1) {
            setScreenState(1);
            this.mPendingScreenOff = false;
            this.mPowerState.dismissColorFadeResources();
        }
        if (target == 2) {
            if (setScreenState(2)) {
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
            }
        } else if (target == 5) {
            if ((!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() != 2) && setScreenState(5)) {
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
            }
        } else if (target == 3) {
            if ((!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() != 2) && setScreenState(3)) {
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
            }
        } else if (target == 4) {
            if (!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() == 4) {
                if (this.mPowerState.getScreenState() != 4) {
                    if (setScreenState(3)) {
                        setScreenState(4);
                    } else {
                        return;
                    }
                }
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
            }
        } else if (target != 6) {
            this.mPendingScreenOff = true;
            if (!this.mColorFadeEnabled) {
                this.mPowerState.setColorFadeLevel(0.0f);
            }
            if (this.mPowerState.getColorFadeLevel() == 0.0f) {
                setScreenState(1);
                this.mPendingScreenOff = false;
                this.mPowerState.dismissColorFadeResources();
                return;
            }
            if (performScreenOffTransition && !checkPhoneWindowIsTop()) {
                DisplayPowerState displayPowerState = this.mPowerState;
                Context context = this.mContext;
                if (!this.mColorFadeFadesConfig) {
                    i = 1;
                }
                if (displayPowerState.prepareColorFade(context, i) && this.mPowerState.getScreenState() != 1) {
                    this.mColorFadeOffAnimator.start();
                    return;
                }
            }
            this.mColorFadeOffAnimator.end();
        } else if (!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() == 6) {
            if (this.mPowerState.getScreenState() != 6) {
                if (setScreenState(2)) {
                    setScreenState(6);
                } else {
                    return;
                }
            }
            this.mPowerState.setColorFadeLevel(1.0f);
            this.mPowerState.dismissColorFade();
        }
    }

    private void setProximitySensorEnabled(boolean enable) {
        if (enable) {
            if (!this.mProximitySensorEnabled) {
                this.mProximitySensorEnabled = true;
                this.mSensorManager.registerListener(this.mProximitySensorListener, this.mProximitySensor, 3, this.mHandler);
            }
        } else if (this.mProximitySensorEnabled) {
            this.mProximitySensorEnabled = false;
            this.mProximity = -1;
            this.mPendingProximity = -1;
            this.mHandler.removeMessages(2);
            this.mSensorManager.unregisterListener(this.mProximitySensorListener);
            clearPendingProximityDebounceTime();
        }
    }

    /* access modifiers changed from: private */
    public void handleProximitySensorEvent(long time, boolean positive) {
        if (!this.mProximitySensorEnabled) {
            return;
        }
        if (this.mPendingProximity == 0 && !positive) {
            return;
        }
        if (this.mPendingProximity != 1 || !positive) {
            HwServiceFactory.reportProximitySensorEventToIAware(positive);
            Slog.i(TAG, "UL_Power handleProximitySensorEvent positive:" + positive);
            this.mHandler.removeMessages(2);
            if (positive) {
                this.mPendingProximity = 1;
                setPendingProximityDebounceTime(0 + time);
            } else {
                this.mPendingProximity = 0;
                setPendingProximityDebounceTime(0 + time);
            }
            IHwDisplayPowerControllerEx iHwDisplayPowerControllerEx = this.mHwDisplayPowerEx;
            if (iHwDisplayPowerControllerEx != null) {
                iHwDisplayPowerControllerEx.sendProximityBroadcast(positive);
            }
            debounceProximitySensor();
        }
    }

    /* access modifiers changed from: private */
    public void debounceProximitySensor() {
        if (this.mProximitySensorEnabled && this.mPendingProximity != -1 && this.mPendingProximityDebounceTime >= 0) {
            if (this.mPendingProximityDebounceTime <= SystemClock.uptimeMillis()) {
                this.mProximity = this.mPendingProximity;
                updatePowerState();
                clearPendingProximityDebounceTime();
                return;
            }
            this.mHandler.sendMessageAtTime(this.mHandler.obtainMessage(2), this.mPendingProximityDebounceTime);
        }
    }

    private void clearPendingProximityDebounceTime() {
        if (this.mPendingProximityDebounceTime >= 0) {
            this.mPendingProximityDebounceTime = -1;
            this.mCallbacks.releaseSuspendBlocker();
        }
    }

    private void setPendingProximityDebounceTime(long debounceTime) {
        if (this.mPendingProximityDebounceTime < 0) {
            this.mCallbacks.acquireSuspendBlocker();
        }
        this.mPendingProximityDebounceTime = debounceTime;
    }

    private void sendOnStateChangedWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnStateChangedRunnable);
    }

    private void logDisplayPolicyChanged(int newPolicy) {
        LogMaker log = new LogMaker(1696);
        log.setType(6);
        log.setSubtype(newPolicy);
        MetricsLogger.action(log);
    }

    /* access modifiers changed from: private */
    public void handleSettingsChange(boolean userSwitch) {
        this.mPendingScreenBrightnessSetting = getScreenBrightnessSetting();
        if (userSwitch) {
            this.mCurrentScreenBrightnessSetting = this.mPendingScreenBrightnessSetting;
            AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
            if (automaticBrightnessController != null) {
                automaticBrightnessController.resetShortTermModel();
            }
        }
        this.mPendingAutoBrightnessAdjustment = getAutoBrightnessAdjustmentSetting();
        this.mScreenBrightnessForVr = getScreenBrightnessForVrSetting();
        if (!this.mPowerRequest.useAutoBrightness) {
            sendUpdatePowerState();
        }
    }

    private float getAutoBrightnessAdjustmentSetting() {
        float adj = Settings.System.getFloatForUser(this.mContext.getContentResolver(), "screen_auto_brightness_adj", 0.0f, this.mCurrentUserId);
        if (Float.isNaN(adj)) {
            return 0.0f;
        }
        return clampAutoBrightnessAdjustment(adj);
    }

    private int getScreenBrightnessSetting() {
        return clampAbsoluteBrightness(Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", this.mScreenBrightnessDefault, this.mCurrentUserId));
    }

    private int getScreenBrightnessForVrSetting() {
        return clampScreenBrightnessForVr(Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_for_vr", this.mScreenBrightnessForVrDefault, this.mCurrentUserId));
    }

    private void putScreenBrightnessSetting(int brightness) {
        this.mCurrentScreenBrightnessSetting = brightness;
        if (this.mCurrentScreenBrightnessSettingForDB != brightness) {
            Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness", brightness, this.mCurrentUserId);
        }
        this.mCurrentScreenBrightnessSettingForDB = brightness;
    }

    private void putAutoBrightnessAdjustmentSetting(float adjustment) {
        this.mAutoBrightnessAdjustment = adjustment;
        Settings.System.putFloatForUser(this.mContext.getContentResolver(), "screen_auto_brightness_adj", adjustment, this.mCurrentUserId);
    }

    private boolean updateAutoBrightnessAdjustment() {
        if (Float.isNaN(this.mPendingAutoBrightnessAdjustment)) {
            return false;
        }
        float f = this.mAutoBrightnessAdjustment;
        float f2 = this.mPendingAutoBrightnessAdjustment;
        if (f == f2) {
            this.mPendingAutoBrightnessAdjustment = Float.NaN;
            return false;
        }
        this.mAutoBrightnessAdjustment = f2;
        this.mPendingAutoBrightnessAdjustment = Float.NaN;
        return true;
    }

    private boolean updateUserSetScreenBrightness() {
        int i = this.mPendingScreenBrightnessSetting;
        if (i < 0) {
            return false;
        }
        if (this.mCurrentScreenBrightnessSetting == i) {
            this.mPendingScreenBrightnessSetting = -1;
            this.mTemporaryScreenBrightness = -1;
            return false;
        }
        this.mCurrentScreenBrightnessSetting = i;
        this.mLastUserSetScreenBrightness = i;
        this.mPendingScreenBrightnessSetting = -1;
        this.mTemporaryScreenBrightness = -1;
        return true;
    }

    private void notifyBrightnessChanged(int brightness, boolean userInitiated, boolean hadUserDataPoint) {
        float powerFactor;
        float brightnessInNits = convertToNits(brightness);
        if (this.mPowerRequest.useAutoBrightness && brightnessInNits >= 0.0f && this.mAutomaticBrightnessController != null) {
            if (this.mPowerRequest.lowPowerMode) {
                powerFactor = this.mPowerRequest.screenLowPowerBrightnessFactor;
            } else {
                powerFactor = 1.0f;
            }
            this.mBrightnessTracker.notifyBrightnessChanged(brightnessInNits, userInitiated, powerFactor, hadUserDataPoint, this.mAutomaticBrightnessController.isDefaultConfig());
        }
    }

    private float convertToNits(int backlight) {
        BrightnessMappingStrategy brightnessMappingStrategy = this.mBrightnessMapper;
        if (brightnessMappingStrategy != null) {
            return brightnessMappingStrategy.convertToNits(backlight);
        }
        return -1.0f;
    }

    private void sendOnProximityPositiveWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnProximityPositiveRunnable);
    }

    private void sendOnProximityNegativeWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnProximityNegativeRunnable);
    }

    public void dump(final PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println();
            pw.println("Display Power Controller Locked State:");
            pw.println("  mDisplayReadyLocked=" + this.mDisplayReadyLocked);
            pw.println("  mPendingRequestLocked=" + this.mPendingRequestLocked);
            pw.println("  mPendingRequestChangedLocked=" + this.mPendingRequestChangedLocked);
            pw.println("  mPendingWaitForNegativeProximityLocked=" + this.mPendingWaitForNegativeProximityLocked);
            pw.println("  mPendingUpdatePowerStateLocked=" + this.mPendingUpdatePowerStateLocked);
        }
        pw.println();
        pw.println("Display Power Controller Configuration:");
        pw.println("  mScreenBrightnessDozeConfig=" + this.mScreenBrightnessDozeConfig);
        pw.println("  mScreenBrightnessDimConfig=" + this.mScreenBrightnessDimConfig);
        pw.println("  mScreenBrightnessRangeMinimum=" + this.mScreenBrightnessRangeMinimum);
        pw.println("  mScreenBrightnessRangeMaximum=" + this.mScreenBrightnessRangeMaximum);
        pw.println("  mScreenBrightnessDefault=" + this.mScreenBrightnessDefault);
        pw.println("  mScreenBrightnessForVrRangeMinimum=" + this.mScreenBrightnessForVrRangeMinimum);
        pw.println("  mScreenBrightnessForVrRangeMaximum=" + this.mScreenBrightnessForVrRangeMaximum);
        pw.println("  mScreenBrightnessForVrDefault=" + this.mScreenBrightnessForVrDefault);
        pw.println("  mUseSoftwareAutoBrightnessConfig=" + this.mUseSoftwareAutoBrightnessConfig);
        pw.println("  mAllowAutoBrightnessWhileDozingConfig=" + this.mAllowAutoBrightnessWhileDozingConfig);
        pw.println("  mBrightnessRampRateFast=" + this.mBrightnessRampRateFast);
        pw.println("  mBrightnessRampRateSlow=" + this.mBrightnessRampRateSlow);
        pw.println("  mSkipScreenOnBrightnessRamp=" + this.mSkipScreenOnBrightnessRamp);
        pw.println("  mColorFadeFadesConfig=" + this.mColorFadeFadesConfig);
        pw.println("  mColorFadeEnabled=" + this.mColorFadeEnabled);
        pw.println("  mDisplayBlanksAfterDozeConfig=" + this.mDisplayBlanksAfterDozeConfig);
        pw.println("  mBrightnessBucketsInDozeConfig=" + this.mBrightnessBucketsInDozeConfig);
        this.mHandler.runWithScissors(new Runnable() {
            /* class com.android.server.display.DisplayPowerController.AnonymousClass7 */

            public void run() {
                DisplayPowerController.this.dumpLocal(pw);
            }
        }, 1000);
    }

    /* access modifiers changed from: private */
    public void dumpLocal(PrintWriter pw) {
        pw.println();
        pw.println("Display Power Controller Thread State:");
        pw.println("  mPowerRequest=" + this.mPowerRequest);
        pw.println("  mUnfinishedBusiness=" + this.mUnfinishedBusiness);
        pw.println("  mWaitingForNegativeProximity=" + this.mWaitingForNegativeProximity);
        pw.println("  mProximitySensor=" + this.mProximitySensor);
        pw.println("  mProximitySensorEnabled=" + this.mProximitySensorEnabled);
        pw.println("  mProximityThreshold=" + this.mProximityThreshold);
        pw.println("  mProximity=" + proximityToString(this.mProximity));
        pw.println("  mPendingProximity=" + proximityToString(this.mPendingProximity));
        pw.println("  mPendingProximityDebounceTime=" + TimeUtils.formatUptime(this.mPendingProximityDebounceTime));
        pw.println("  mScreenOffBecauseOfProximity=" + this.mScreenOffBecauseOfProximity);
        pw.println("  mLastUserSetScreenBrightness=" + this.mLastUserSetScreenBrightness);
        pw.println("  mCurrentScreenBrightnessSetting=" + this.mCurrentScreenBrightnessSetting);
        pw.println("  mPendingScreenBrightnessSetting=" + this.mPendingScreenBrightnessSetting);
        pw.println("  mTemporaryScreenBrightness=" + this.mTemporaryScreenBrightness);
        pw.println("  mAutoBrightnessAdjustment=" + this.mAutoBrightnessAdjustment);
        pw.println("  mBrightnessReason=" + this.mBrightnessReason);
        pw.println("  mTemporaryAutoBrightnessAdjustment=" + this.mTemporaryAutoBrightnessAdjustment);
        pw.println("  mPendingAutoBrightnessAdjustment=" + this.mPendingAutoBrightnessAdjustment);
        pw.println("  mScreenBrightnessForVr=" + this.mScreenBrightnessForVr);
        pw.println("  mAppliedAutoBrightness=" + this.mAppliedAutoBrightness);
        pw.println("  mAppliedDimming=" + this.mAppliedDimming);
        pw.println("  mAppliedLowPower=" + this.mAppliedLowPower);
        pw.println("  mAppliedScreenBrightnessOverride=" + this.mAppliedScreenBrightnessOverride);
        pw.println("  mAppliedTemporaryBrightness=" + this.mAppliedTemporaryBrightness);
        pw.println("  mDozing=" + this.mDozing);
        pw.println("  mSkipRampState=" + skipRampStateToString(this.mSkipRampState));
        pw.println("  mInitialAutoBrightness=" + this.mInitialAutoBrightness);
        pw.println("  mScreenOnBlockStartRealTime=" + this.mScreenOnBlockStartRealTime);
        pw.println("  mScreenOffBlockStartRealTime=" + this.mScreenOffBlockStartRealTime);
        pw.println("  mPendingScreenOnUnblocker=" + this.mPendingScreenOnUnblocker);
        pw.println("  mPendingScreenOnExUnblocker=" + this.mPendingScreenOnExUnblocker);
        pw.println("  mPendingScreenOffUnblocker=" + this.mPendingScreenOffUnblocker);
        pw.println("  mPendingScreenOff=" + this.mPendingScreenOff);
        pw.println("  mReportedToPolicy=" + reportedToPolicyToString(this.mReportedScreenStateToPolicy));
        if (this.mScreenBrightnessRampAnimator != null) {
            pw.println("  mScreenBrightnessRampAnimator.isAnimating()=" + this.mScreenBrightnessRampAnimator.isAnimating());
        }
        if (this.mColorFadeOnAnimator != null) {
            pw.println("  mColorFadeOnAnimator.isStarted()=" + this.mColorFadeOnAnimator.isStarted());
        }
        if (this.mColorFadeOffAnimator != null) {
            pw.println("  mColorFadeOffAnimator.isStarted()=" + this.mColorFadeOffAnimator.isStarted());
        }
        DisplayPowerState displayPowerState = this.mPowerState;
        if (displayPowerState != null) {
            displayPowerState.dump(pw);
        }
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.dump(pw);
        }
        if (this.mBrightnessTracker != null) {
            pw.println();
            this.mBrightnessTracker.dump(pw);
        }
        pw.println();
        DisplayWhiteBalanceController displayWhiteBalanceController = this.mDisplayWhiteBalanceController;
        if (displayWhiteBalanceController != null) {
            displayWhiteBalanceController.dump(pw);
            this.mDisplayWhiteBalanceSettings.dump(pw);
        }
    }

    private static String proximityToString(int state) {
        if (state == -1) {
            return "Unknown";
        }
        if (state == 0) {
            return "Negative";
        }
        if (state != 1) {
            return Integer.toString(state);
        }
        return "Positive";
    }

    private static String reportedToPolicyToString(int state) {
        if (state == 0) {
            return "REPORTED_TO_POLICY_SCREEN_OFF";
        }
        if (state == 1) {
            return "REPORTED_TO_POLICY_SCREEN_TURNING_ON";
        }
        if (state != 2) {
            return Integer.toString(state);
        }
        return "REPORTED_TO_POLICY_SCREEN_ON";
    }

    private static boolean wantScreenOn(int state) {
        if (state == 2 || state == 3) {
            return true;
        }
        return false;
    }

    private static int getScreenOnState(int state) {
        if (state != 1) {
            return state != 2 ? 2 : 1;
        }
        return 0;
    }

    private static String skipRampStateToString(int state) {
        if (state == 0) {
            return "RAMP_STATE_SKIP_NONE";
        }
        if (state == 1) {
            return "RAMP_STATE_SKIP_INITIAL";
        }
        if (state != 2) {
            return Integer.toString(state);
        }
        return "RAMP_STATE_SKIP_AUTOBRIGHT";
    }

    private static int clampAbsoluteBrightness(int value) {
        return MathUtils.constrain(value, 0, 255);
    }

    private static float clampAutoBrightnessAdjustment(float value) {
        return MathUtils.constrain(value, -1.0f, 1.0f);
    }

    /* access modifiers changed from: private */
    public final class DisplayControllerHandler extends Handler {
        public DisplayControllerHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            switch (i) {
                case 1:
                    DisplayPowerController.this.updatePowerState();
                    return;
                case 2:
                    DisplayPowerController.this.debounceProximitySensor();
                    return;
                case 3:
                    if (DisplayPowerController.this.mPendingScreenOnUnblocker == msg.obj) {
                        DisplayPowerController.this.unblockScreenOn();
                        DisplayPowerController.this.updatePowerState();
                        return;
                    }
                    return;
                case 4:
                    if (DisplayPowerController.this.mPendingScreenOffUnblocker == msg.obj) {
                        DisplayPowerController.this.unblockScreenOff();
                        DisplayPowerController.this.updatePowerState();
                        return;
                    }
                    return;
                case 5:
                    BrightnessConfiguration unused = DisplayPowerController.this.mBrightnessConfiguration = (BrightnessConfiguration) msg.obj;
                    DisplayPowerController.this.updatePowerState();
                    return;
                case 6:
                    int unused2 = DisplayPowerController.this.mTemporaryScreenBrightness = msg.arg1;
                    DisplayPowerController.this.updatePowerState();
                    return;
                case 7:
                    float unused3 = DisplayPowerController.this.mTemporaryAutoBrightnessAdjustment = Float.intBitsToFloat(msg.arg1);
                    DisplayPowerController.this.updatePowerState();
                    return;
                default:
                    switch (i) {
                        case 101:
                            DisplayPowerController displayPowerController = DisplayPowerController.this;
                            boolean z = true;
                            if (msg.arg1 != 1) {
                                z = false;
                            }
                            displayPowerController.handlerTpKeepStateChanged(z);
                            return;
                        case 102:
                            DisplayPowerController.this.updateScreenState();
                            return;
                        case 103:
                            if (DisplayPowerController.this.mPendingScreenOnExUnblocker == msg.obj) {
                                DisplayPowerController.this.unblockScreenOnEx();
                                DisplayPowerController.this.updatePowerState();
                                return;
                            }
                            return;
                        default:
                            return;
                    }
            }
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            DisplayPowerController.this.handleSettingsChange(false);
        }
    }

    /* access modifiers changed from: private */
    public final class ScreenOnUnblocker implements WindowManagerPolicy.ScreenOnListener {
        private ScreenOnUnblocker() {
        }

        @Override // com.android.server.policy.WindowManagerPolicy.ScreenOnListener
        public void onScreenOn() {
            DisplayPowerController.this.mHandler.sendMessage(DisplayPowerController.this.mHandler.obtainMessage(3, this));
        }
    }

    /* access modifiers changed from: private */
    public final class ScreenOnExUnblocker implements WindowManagerPolicy.ScreenOnExListener {
        private ScreenOnExUnblocker() {
        }

        @Override // com.android.server.policy.WindowManagerPolicy.ScreenOnExListener
        public void onScreenOnEx() {
            DisplayPowerController.this.mHandler.sendMessage(DisplayPowerController.this.mHandler.obtainMessage(103, this));
        }
    }

    private boolean checkPhoneWindowIsTop() {
        long startTime = SystemClock.elapsedRealtime();
        List<ActivityManager.RunningTaskInfo> tasksInfo = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningTasks(1);
        long getRunningTasksDuration = SystemClock.elapsedRealtime() - startTime;
        if (getRunningTasksDuration > 500) {
            Slog.i(TAG, "Check Phone Window is top, get the Running Tasks duration: " + getRunningTasksDuration);
        }
        if (tasksInfo != null && tasksInfo.size() > 0) {
            ComponentName cn = tasksInfo.get(0).topActivity;
            if ("com.android.incallui".equals(cn.getPackageName()) && "com.android.incallui.InCallActivity".equals(cn.getClassName())) {
                if (DEBUG) {
                    Slog.i(TAG, "checkPhoneWindowIsTop: incallui window is top");
                }
                return true;
            }
        }
        return false;
    }

    private void writeAutoBrightnessDbEnable(boolean autoEnable) {
        if (this.mPowerRequest.policy == 2 || !autoEnable) {
            this.mBackLight.writeAutoBrightnessDbEnable(false);
        } else if (!this.mPowerPolicyChangeFromDimming) {
            this.mBackLight.writeAutoBrightnessDbEnable(true);
        }
    }

    @Override // com.android.server.display.IHwDisplayPowerControllerEx.Callbacks
    public void onTpKeepStateChanged(boolean tpKeeped) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(101, tpKeeped ? 1 : 0, 0));
    }

    /* access modifiers changed from: private */
    public void handlerTpKeepStateChanged(boolean tpKeeped) {
        if (DEBUG) {
            Slog.i(TAG, "TpKeepChanged tpKeeped:" + tpKeeped + ", sensorPositive:" + this.mProximitySensorPositive);
        }
        handleProximitySensorEvent(SystemClock.uptimeMillis(), this.mProximitySensorPositive || tpKeeped);
    }

    private void updatemAnimationState(boolean autoBrightnessEnabled) {
        DisplayManagerInternal.DisplayPowerRequest displayPowerRequest;
        if (this.mAutomaticBrightnessController != null && this.mScreenBrightnessRampAnimator != null && (displayPowerRequest = this.mPowerRequest) != null) {
            if (displayPowerRequest.screenAutoBrightness > 0) {
                this.mAutomaticBrightnessController.updateIntervenedAutoBrightness(this.mPowerRequest.screenAutoBrightness);
            }
            boolean z = false;
            if (this.mBrightnessModeChanged) {
                this.mBrightnessModeChanged = false;
            }
            RampAnimator<DisplayPowerState> rampAnimator = this.mScreenBrightnessRampAnimator;
            int updateAutoBrightnessCount = this.mAutomaticBrightnessController.getUpdateAutoBrightnessCount();
            if (this.mPowerRequest.screenAutoBrightness > 0) {
                z = true;
            }
            rampAnimator.updateBrightnessRampPara(autoBrightnessEnabled, updateAutoBrightnessCount, z, this.mPowerRequest.policy);
            this.mfastAnimtionFlag = this.mAutomaticBrightnessController.getPowerStatus();
            this.mScreenBrightnessRampAnimator.updateFastAnimationFlag(this.mfastAnimtionFlag);
            this.mCoverModeAnimationFast = this.mAutomaticBrightnessController.getCoverModeFastResponseFlag();
            this.mScreenBrightnessRampAnimator.updateCoverModeFastAnimationFlag(this.mCoverModeAnimationFast);
            this.mScreenBrightnessRampAnimator.updateCameraModeChangeAnimationEnable(this.mAutomaticBrightnessController.getCameraModeChangeAnimationEnable());
            this.mScreenBrightnessRampAnimator.updateGameModeChangeAnimationEnable(this.mAutomaticBrightnessController.getAnimationGameChangeEnable());
            if (this.mAutomaticBrightnessController.getReadingModeBrightnessLineEnable()) {
                this.mScreenBrightnessRampAnimator.updateReadingModeChangeAnimationEnable(this.mAutomaticBrightnessController.getReadingModeChangeAnimationEnable());
            }
            this.mScreenBrightnessRampAnimator.setBrightnessAnimationTime(this.mAnimationEnabled, this.mMillisecond);
            this.mOutdoorAnimationFlag = this.mAutomaticBrightnessController.getOutdoorAnimationFlag();
            this.mScreenBrightnessRampAnimator.updateOutdoorAnimationFlag(this.mOutdoorAnimationFlag);
        }
    }

    @Override // com.android.server.display.AutomaticBrightnessController.Callbacks
    public void updateProximityState(boolean proximityState) {
        if (DEBUG) {
            Slog.i(TAG, "updateProximityState:" + proximityState);
        }
        this.mProximityPositive = proximityState;
        this.mScreenBrightnessRampAnimator.updateProximityState(proximityState);
    }

    public boolean getManualModeEnable() {
        ManualBrightnessController manualBrightnessController = this.mManualBrightnessController;
        if (manualBrightnessController != null) {
            return manualBrightnessController.getManualModeEnable();
        }
        Slog.e(TAG, "mManualBrightnessController=null");
        return false;
    }

    public void updatemManualModeAnimationEnable() {
        RampAnimator<DisplayPowerState> rampAnimator;
        if (getManualModeEnable() && (rampAnimator = this.mScreenBrightnessRampAnimator) != null) {
            rampAnimator.updatemManualModeAnimationEnable(this.mManualBrightnessController.getManualModeAnimationEnable());
        }
    }

    public void updateManualPowerSavingAnimationEnable() {
        boolean frontCameraDimmingEnable;
        if (this.mManualBrightnessController != null && this.mScreenBrightnessRampAnimator != null && this.mAutomaticBrightnessController != null) {
            int brightnessMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 1, this.mCurrentUserId);
            boolean autoPowerSavingUseManualAnimationTimeEnable = this.mAutomaticBrightnessController.getAutoPowerSavingUseManualAnimationTimeEnable();
            if (brightnessMode != 1 || !autoPowerSavingUseManualAnimationTimeEnable) {
                boolean manualPowerSavingAnimationEnable = this.mManualBrightnessController.getManualPowerSavingAnimationEnable();
                this.mScreenBrightnessRampAnimator.updateManualPowerSavingAnimationEnable(manualPowerSavingAnimationEnable);
                if (manualPowerSavingAnimationEnable) {
                    this.mManualBrightnessController.setManualPowerSavingAnimationEnable(false);
                }
            } else {
                boolean manualPowerSavingAnimationEnable2 = this.mAutomaticBrightnessController.getAutoPowerSavingAnimationEnable();
                this.mScreenBrightnessRampAnimator.updateManualPowerSavingAnimationEnable(manualPowerSavingAnimationEnable2);
                if (manualPowerSavingAnimationEnable2) {
                    this.mAutomaticBrightnessController.setAutoPowerSavingAnimationEnable(false);
                }
            }
            if (brightnessMode == 1) {
                frontCameraDimmingEnable = this.mAutomaticBrightnessController.getFrontCameraDimmingEnable();
            } else {
                frontCameraDimmingEnable = this.mManualBrightnessController.getFrontCameraDimmingEnable();
            }
            this.mScreenBrightnessRampAnimator.updateFrontCameraDimmingEnable(frontCameraDimmingEnable);
        }
    }

    public void updateManualThermalModeAnimationEnable() {
        ManualBrightnessController manualBrightnessController = this.mManualBrightnessController;
        if (manualBrightnessController != null && this.mScreenBrightnessRampAnimator != null && manualBrightnessController.getManualThermalModeEnable()) {
            boolean manualThermalModeAnimationEnable = this.mManualBrightnessController.getManualThermalModeAnimationEnable();
            this.mScreenBrightnessRampAnimator.updateManualThermalModeAnimationEnable(manualThermalModeAnimationEnable);
            if (manualThermalModeAnimationEnable) {
                this.mManualBrightnessController.setManualThermalModeAnimationEnable(false);
            }
        }
    }

    public void updateBrightnessModeAnimationEnable() {
        ManualBrightnessController manualBrightnessController = this.mManualBrightnessController;
        if (manualBrightnessController != null && this.mScreenBrightnessRampAnimator != null && manualBrightnessController.getBrightnessSetByAppEnable()) {
            this.mScreenBrightnessRampAnimator.updateBrightnessModeAnimationEnable(this.mManualBrightnessController.getBrightnessSetByAppAnimationEnable(), this.mSetBrightnessNoLimitAnimationTime);
        }
    }

    private void updateDarkAdaptDimmingEnable() {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null && this.mScreenBrightnessRampAnimator != null) {
            boolean darkAdaptDimmingEnable = automaticBrightnessController.getDarkAdaptDimmingEnable();
            this.mScreenBrightnessRampAnimator.updateDarkAdaptAnimationDimmingEnable(darkAdaptDimmingEnable);
            if (darkAdaptDimmingEnable) {
                this.mAutomaticBrightnessController.clearDarkAdaptDimmingEnable();
            }
        }
    }

    /* access modifiers changed from: private */
    public final class ScreenOffUnblocker implements WindowManagerPolicy.ScreenOffListener {
        private ScreenOffUnblocker() {
        }

        @Override // com.android.server.policy.WindowManagerPolicy.ScreenOffListener
        public void onScreenOff() {
            DisplayPowerController.this.mHandler.sendMessage(DisplayPowerController.this.mHandler.obtainMessage(4, this));
        }
    }

    /* access modifiers changed from: package-private */
    public void setAutoBrightnessLoggingEnabled(boolean enabled) {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.setLoggingEnabled(enabled);
        }
    }

    public int getCoverModeBrightnessFromLastScreenBrightness() {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController == null) {
            return 33;
        }
        return automaticBrightnessController.getCoverModeBrightnessFromLastScreenBrightness();
    }

    public void setMaxBrightnessFromThermal(int brightness) {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.setMaxBrightnessFromThermal(brightness);
        }
        this.mManualBrightnessController.setMaxBrightnessFromThermal(brightness);
    }

    public void setModeToAutoNoClearOffsetEnable(boolean enable) {
        this.mModeToAutoNoClearOffsetEnable = enable;
        if (DEBUG) {
            Slog.i(TAG, "set mModeToAutoNoClearOffsetEnable=" + this.mModeToAutoNoClearOffsetEnable);
        }
    }

    public int setScreenBrightnessMappingtoIndoorMax(int brightness) {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController == null) {
            return brightness;
        }
        return automaticBrightnessController.setScreenBrightnessMappingtoIndoorMax(brightness);
    }

    private void sendTargetBrightnessToMonitor(int brightness) {
        if (this.mDisplayEffectMonitor != null) {
            ArrayMap<String, Object> params = new ArrayMap<>();
            params.put("paramType", "algoDiscountBrightness");
            params.put("brightness", Integer.valueOf(brightness));
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    public void setPoweroffModeChangeAutoEnable(boolean enable) {
        this.mPoweroffModeChangeAutoEnable = enable;
        this.mBrightnessModeChangeNoClearOffsetEnable = this.mPoweroffModeChangeAutoEnable;
        if (DEBUG) {
            Slog.i(TAG, "set mPoweroffModeChangeAutoEnable=" + this.mPoweroffModeChangeAutoEnable + ",mNoClearOffsetEnable=" + this.mBrightnessModeChangeNoClearOffsetEnable);
        }
    }

    public void setBrightnessNoLimit(int brightness, int time) {
        this.mSetBrightnessNoLimitAnimationTime = time;
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.setBrightnessNoLimit(brightness, time);
        }
        this.mManualBrightnessController.setBrightnessNoLimit(brightness, time);
    }

    public void updateBrightnessModeChangeManualState(boolean enable) {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null) {
            automaticBrightnessController.updateBrightnessModeChangeManualState(enable);
        }
    }

    public void updateFastDarkenDimmingEnable() {
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null && this.mScreenBrightnessRampAnimator != null) {
            this.mScreenBrightnessRampAnimator.updateFastDarkenDimmingEnable(automaticBrightnessController.getFastDarkenDimmingEnable());
            this.mScreenBrightnessRampAnimator.updateKeyguardUnlockedFastDarkenDimmingEnable(this.mAutomaticBrightnessController.getKeyguardUnlockedFastDarkenDimmingEnable());
        }
    }

    private void updateNightUpPowerOnWithDimmingEnable() {
        RampAnimator<DisplayPowerState> rampAnimator;
        AutomaticBrightnessController automaticBrightnessController = this.mAutomaticBrightnessController;
        if (automaticBrightnessController != null && (rampAnimator = this.mScreenBrightnessRampAnimator) != null) {
            rampAnimator.updateNightUpPowerOnWithDimmingEnable(automaticBrightnessController.getNightUpPowerOnWithDimmingEnable());
        }
    }

    @Override // com.android.server.display.whitebalance.DisplayWhiteBalanceController.Callbacks
    public void updateWhiteBalance() {
        sendUpdatePowerState();
    }

    /* access modifiers changed from: package-private */
    public void setDisplayWhiteBalanceLoggingEnabled(boolean enabled) {
        DisplayWhiteBalanceController displayWhiteBalanceController = this.mDisplayWhiteBalanceController;
        if (displayWhiteBalanceController != null) {
            displayWhiteBalanceController.setLoggingEnabled(enabled);
            this.mDisplayWhiteBalanceSettings.setLoggingEnabled(enabled);
        }
    }

    /* access modifiers changed from: package-private */
    public void setAmbientColorTemperatureOverride(float cct) {
        DisplayWhiteBalanceController displayWhiteBalanceController = this.mDisplayWhiteBalanceController;
        if (displayWhiteBalanceController != null) {
            displayWhiteBalanceController.setAmbientColorTemperatureOverride(cct);
            sendUpdatePowerState();
        }
    }

    private final class BrightnessReason {
        static final int ADJUSTMENT_AUTO = 2;
        static final int ADJUSTMENT_AUTO_TEMP = 1;
        static final int MODIFIER_DIMMED = 1;
        static final int MODIFIER_LOW_POWER = 2;
        static final int MODIFIER_MASK = 3;
        static final int REASON_AUTOMATIC = 4;
        static final int REASON_BOOST = 9;
        static final int REASON_DOZE = 2;
        static final int REASON_DOZE_DEFAULT = 3;
        static final int REASON_MANUAL = 1;
        static final int REASON_MAX = 9;
        static final int REASON_OVERRIDE = 7;
        static final int REASON_SCREEN_OFF = 5;
        static final int REASON_TEMPORARY = 8;
        static final int REASON_UNKNOWN = 0;
        static final int REASON_VR = 6;
        public int modifier;
        public int reason;

        private BrightnessReason() {
        }

        public void set(BrightnessReason other) {
            int i = 0;
            setReason(other == null ? 0 : other.reason);
            if (other != null) {
                i = other.modifier;
            }
            setModifier(i);
        }

        public void setReason(int reason2) {
            if (reason2 < 0 || reason2 > 9) {
                Slog.w(DisplayPowerController.TAG, "brightness reason out of bounds: " + reason2);
                return;
            }
            this.reason = reason2;
        }

        public void setModifier(int modifier2) {
            if ((modifier2 & -4) != 0) {
                Slog.w(DisplayPowerController.TAG, "brightness modifier out of bounds: 0x" + Integer.toHexString(modifier2));
                return;
            }
            this.modifier = modifier2;
        }

        public void addModifier(int modifier2) {
            setModifier(this.modifier | modifier2);
        }

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof BrightnessReason)) {
                return false;
            }
            BrightnessReason other = (BrightnessReason) obj;
            if (other.reason == this.reason && other.modifier == this.modifier) {
                return true;
            }
            return false;
        }

        public String toString() {
            return toString(0);
        }

        public String toString(int adjustments) {
            StringBuilder sb = new StringBuilder();
            sb.append(reasonToString(this.reason));
            sb.append(" [");
            if ((adjustments & 1) != 0) {
                sb.append(" temp_adj");
            }
            if ((adjustments & 2) != 0) {
                sb.append(" auto_adj");
            }
            if ((this.modifier & 2) != 0) {
                sb.append(" low_pwr");
            }
            if ((this.modifier & 1) != 0) {
                sb.append(" dim");
            }
            int strlen = sb.length();
            if (sb.charAt(strlen - 1) == '[') {
                sb.setLength(strlen - 2);
            } else {
                sb.append(" ]");
            }
            return sb.toString();
        }

        private String reasonToString(int reason2) {
            switch (reason2) {
                case 1:
                    return "manual";
                case 2:
                    return "doze";
                case 3:
                    return "doze_default";
                case 4:
                    return "automatic";
                case 5:
                    return "screen_off";
                case 6:
                    return "vr";
                case 7:
                    return "override";
                case 8:
                    return "temporary";
                case 9:
                    return "boost";
                default:
                    return Integer.toString(reason2);
            }
        }
    }

    public boolean setHwBrightnessData(String name, Bundle data, int[] result) {
        IHwDisplayPowerControllerEx iHwDisplayPowerControllerEx = this.mHwDisplayPowerEx;
        if (iHwDisplayPowerControllerEx != null) {
            return iHwDisplayPowerControllerEx.setHwBrightnessData(name, data, result);
        }
        return false;
    }

    public boolean getHwBrightnessData(String name, Bundle data, int[] result) {
        IHwDisplayPowerControllerEx iHwDisplayPowerControllerEx = this.mHwDisplayPowerEx;
        if (iHwDisplayPowerControllerEx != null) {
            return iHwDisplayPowerControllerEx.getHwBrightnessData(name, data, result);
        }
        return false;
    }

    @Override // com.android.server.display.IHwDisplayPowerControllerEx.Callbacks
    public AutomaticBrightnessController getAutomaticBrightnessController() {
        return this.mAutomaticBrightnessController;
    }

    @Override // com.android.server.display.IHwDisplayPowerControllerEx.Callbacks
    public ManualBrightnessController getManualBrightnessController() {
        return this.mManualBrightnessController;
    }

    public void setBiometricDetectState(int state) {
        DisplayPowerState displayPowerState = this.mPowerState;
        if (displayPowerState != null) {
            displayPowerState.setBiometricDetectState(state);
        }
    }
}
