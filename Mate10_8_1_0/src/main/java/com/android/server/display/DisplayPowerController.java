package com.android.server.display;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManagerInternal.DisplayPowerCallbacks;
import android.hardware.display.DisplayManagerInternal.DisplayPowerRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager.BacklightBrightness;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.pc.IHwPCManager;
import android.provider.Settings.System;
import android.util.ArrayMap;
import android.util.Flog;
import android.util.HwLog;
import android.util.HwPCUtils;
import android.util.Jlog;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.Spline;
import android.util.TimeUtils;
import android.view.Display;
import android.view.WindowManagerPolicy;
import android.view.WindowManagerPolicy.KeyguardDismissDoneListener;
import android.view.WindowManagerPolicy.ScreenOffListener;
import android.view.WindowManagerPolicy.ScreenOnListener;
import com.android.internal.app.IBatteryStats;
import com.android.server.FingerprintUnlockDataCollector;
import com.android.server.HwServiceFactory;
import com.android.server.HwServiceFactory.IDisplayEffectMonitor;
import com.android.server.HwServiceFactory.IDisplayEngineInterface;
import com.android.server.HwServiceFactory.IHwAutomaticBrightnessController;
import com.android.server.HwServiceFactory.IHwNormalizedManualBrightnessController;
import com.android.server.HwServiceFactory.IHwRampAnimator;
import com.android.server.HwServiceFactory.IHwSmartBackLightController;
import com.android.server.LocalServices;
import com.android.server.am.BatteryStatsService;
import com.android.server.display.AutomaticBrightnessController.Callbacks;
import com.android.server.display.ManualBrightnessController.ManualBrightnessCallbacks;
import com.android.server.display.RampAnimator.Listener;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.huawei.android.aod.HwAodManagerEx;
import java.io.PrintWriter;
import java.util.List;

final class DisplayPowerController implements Callbacks, ManualBrightnessCallbacks {
    static final /* synthetic */ boolean -assertionsDisabled = (DisplayPowerController.class.desiredAssertionStatus() ^ 1);
    private static final int BRIGHTNESS_FOR_PROXIMITY_POSITIVE = -2;
    private static final int BRIGHTNESS_RAMP_RATE_FAST = 200;
    private static final int BRIGHTNESS_RAMP_RATE_SLOW = 40;
    private static final int COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 150;
    private static final int COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 250;
    private static boolean DEBUG = false;
    private static boolean DEBUG_Controller = false;
    private static boolean DEBUG_FPLOG = false;
    private static final boolean DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT = false;
    private static final int DISPALY_SCREEN_OFF = 7;
    private static final int DISPALY_SCREEN_ON = 6;
    private static final int GET_RUNNING_TASKS_FROM_AMS_WARNING_DURATION_MILLIS = 500;
    private static final int LIGHT_SENSOR_RATE_MILLIS = 1000;
    private static final int MSG_PROXIMITY_SENSOR_DEBOUNCED = 2;
    private static final int MSG_SCREEN_OFF_UNBLOCKED = 4;
    private static final int MSG_SCREEN_ON_FOR_KEYGUARD_DISMISS_DONE = 5;
    private static final int MSG_SCREEN_ON_UNBLOCKED = 3;
    private static final int MSG_UPDATE_POWER_STATE = 1;
    private static boolean NEED_NEW_BRIGHTNESS_PROCESS = false;
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
    private static final String USE_SENSORHUB_LABC_PROP = "use_sensorhub_labc";
    private static final boolean mSupportAod = "1".equals(SystemProperties.get("ro.config.support_aod", null));
    private FingerprintUnlockDataCollector fpDataCollector;
    private final boolean mAllowAutoBrightnessWhileDozingConfig;
    private boolean mAnimationEnabled;
    private final AnimatorListener mAnimatorListener = new AnimatorListener() {
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
    private boolean mAppliedDimming;
    private boolean mAppliedLowPower;
    private boolean mAutoBrightnessAdjustmentChanged = false;
    private boolean mAutoBrightnessEnabled = false;
    private Light mAutoCustomBackLight;
    private AutomaticBrightnessController mAutomaticBrightnessController;
    private Light mBackLight;
    private final IBatteryStats mBatteryStats;
    private final DisplayBlanker mBlanker;
    private boolean mBrightnessBucketsInDozeConfig;
    private boolean mBrightnessModeChangeNoClearOffsetEnable = false;
    private boolean mBrightnessModeChanged = false;
    private final int mBrightnessRampRateFast;
    private final int mBrightnessRampRateSlow;
    private final DisplayPowerCallbacks mCallbacks;
    private final Runnable mCleanListener = new Runnable() {
        public void run() {
            DisplayPowerController.this.sendUpdatePowerState();
        }
    };
    private final boolean mColorFadeEnabled;
    private boolean mColorFadeFadesConfig;
    private ObjectAnimator mColorFadeOffAnimator;
    private ObjectAnimator mColorFadeOnAnimator;
    private final Context mContext;
    private boolean mCoverModeAnimationFast = false;
    private int mCurrentUserId = 0;
    private boolean mCurrentUserIdChange = false;
    private boolean mDisplayBlanksAfterDozeConfig;
    private IDisplayEffectMonitor mDisplayEffectMonitor;
    private IDisplayEngineInterface mDisplayEngineInterface = null;
    private boolean mDisplayReadyLocked;
    private boolean mDozing;
    private int mFeedBack = 0;
    private int mGlobalAlpmState = -1;
    private final DisplayControllerHandler mHandler;
    private IHwSmartBackLightController mHwSmartBackLightController;
    private boolean mImmeBright;
    private int mInitialAutoBrightness;
    private boolean mIsCoverModeClosed = true;
    private int mIsScreenOn = 0;
    private boolean mKeyguardIsLocked = false;
    private boolean mLABCEnabled;
    private Sensor mLABCSensor;
    private boolean mLABCSensorEnabled;
    private final SensorEventListener mLABCSensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (DisplayPowerController.this.mLABCSensorEnabled && DisplayPowerController.this.mLABCEnabled) {
                int Backlight = (int) event.values[0];
                int Ambientlight = (int) event.values[1];
                int FeedBack = (int) event.values[2];
                if (DisplayPowerController.DEBUG) {
                    Slog.d(DisplayPowerController.TAG, "[LABC] onSensorChanged----BL =  " + Backlight + ", AL=  " + Ambientlight + ", FeedBack=  " + FeedBack);
                }
                if (Backlight >= 0) {
                    DisplayPowerController.this.mPendingBacklight = Backlight;
                    DisplayPowerController.this.sendUpdatePowerState();
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private int mLastBacklight = 102;
    private boolean mLastStatus = false;
    private boolean mLastWaitBrightnessMode;
    private boolean mLightSensorOnEnable = false;
    private final LightsManager mLights;
    private final Object mLock = new Object();
    private ManualBrightnessController mManualBrightnessController = null;
    private Light mManualCustomBackLight;
    private int mMillisecond;
    private boolean mModeToAutoNoClearOffsetEnable = false;
    private final Runnable mOnProximityNegativeRunnable = new Runnable() {
        public void run() {
            DisplayPowerController.this.mCallbacks.onProximityNegative();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private final Runnable mOnProximityPositiveRunnable = new Runnable() {
        public void run() {
            DisplayPowerController.this.mCallbacks.onProximityPositive();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private final Runnable mOnStateChangedRunnable = new Runnable() {
        public void run() {
            DisplayPowerController.this.mCallbacks.onStateChanged();
            DisplayPowerController.this.mCallbacks.releaseSuspendBlocker();
        }
    };
    private boolean mOutdoorAnimationFlag = false;
    private int mPendingBacklight = -1;
    private int mPendingProximity = -1;
    private long mPendingProximityDebounceTime = -1;
    private boolean mPendingRequestChangedLocked;
    private DisplayPowerRequest mPendingRequestLocked;
    private boolean mPendingScreenOff;
    private ScreenOffUnblocker mPendingScreenOffUnblocker;
    private ScreenOnForKeyguardDismissUnblocker mPendingScreenOnForKeyguardDismissUnblocker;
    private ScreenOnUnblocker mPendingScreenOnUnblocker;
    private boolean mPendingUpdatePowerStateLocked;
    private boolean mPendingWaitForNegativeProximityLocked;
    private boolean mPowerPolicyChangeFromDimming;
    private DisplayPowerRequest mPowerRequest;
    private DisplayPowerState mPowerState;
    private boolean mPoweroffModeChangeAutoEnable = false;
    private int mProximity = -1;
    private boolean mProximityPositive = false;
    private Sensor mProximitySensor;
    private boolean mProximitySensorEnabled;
    private final SensorEventListener mProximitySensorListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {
            if (DisplayPowerController.this.mProximitySensorEnabled) {
                long time = SystemClock.uptimeMillis();
                float distance = event.values[0];
                boolean positive = distance >= 0.0f && distance < DisplayPowerController.this.mProximityThreshold;
                DisplayPowerController.this.handleProximitySensorEvent(time, positive);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private float mProximityThreshold;
    private final Listener mRampAnimatorListener = new Listener() {
        public void onAnimationEnd() {
            if (DisplayPowerController.this.mUsingHwSmartBackLightController && DisplayPowerController.this.mSmartBackLightEnabled) {
                DisplayPowerController.this.mHwSmartBackLightController.updateBrightnessState(1);
            }
            if (DisplayPowerController.this.mPowerPolicyChangeFromDimming) {
                DisplayPowerController.this.mPowerPolicyChangeFromDimming = false;
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
    private final int mScreenBrightnessDarkConfig;
    private final int mScreenBrightnessDimConfig;
    private final int mScreenBrightnessDozeConfig;
    private RampAnimator<DisplayPowerState> mScreenBrightnessRampAnimator;
    private final int mScreenBrightnessRangeMaximum;
    private final int mScreenBrightnessRangeMinimum;
    private boolean mScreenOffBecauseOfProximity;
    private long mScreenOffBlockStartRealTime;
    private long mScreenOnBlockStartRealTime;
    private long mScreenOnForKeyguardDismissBlockStartRealTime;
    private final SensorManager mSensorManager;
    private int mSetAutoBackLight = -1;
    private int mSetBrightnessNoLimitAnimationTime = 500;
    private int mSkipRampState = 0;
    private final boolean mSkipScreenOnBrightnessRamp;
    private boolean mSmartBackLightEnabled;
    private boolean mSmartBackLightSupported;
    private boolean mUnfinishedBusiness;
    private boolean mUseSensorHubLABC = false;
    private boolean mUseSoftwareAutoBrightnessConfig;
    private boolean mUsingHwSmartBackLightController = false;
    private boolean mUsingSRE = false;
    private boolean mWaitingForNegativeProximity;
    private boolean mWakeupFromSleep = true;
    private final WindowManagerPolicy mWindowManagerPolicy;
    private boolean mfastAnimtionFlag = false;

    private final class DisplayControllerHandler extends Handler {
        public DisplayControllerHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
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
                    if (DisplayPowerController.this.mPendingScreenOnForKeyguardDismissUnblocker == msg.obj) {
                        DisplayPowerController.this.mImmeBright = true;
                        DisplayPowerController.this.unblockScreenOnForKeyguardDismiss();
                        DisplayPowerController.this.updatePowerState();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private final class ScreenOffUnblocker implements ScreenOffListener {
        private ScreenOffUnblocker() {
        }

        public void onScreenOff() {
            Message msg = DisplayPowerController.this.mHandler.obtainMessage(4, this);
            msg.setAsynchronous(true);
            DisplayPowerController.this.mHandler.sendMessage(msg);
        }
    }

    private final class ScreenOnForKeyguardDismissUnblocker implements KeyguardDismissDoneListener {
        private ScreenOnForKeyguardDismissUnblocker() {
        }

        public void onKeyguardDismissDone() {
            long delay = SystemClock.elapsedRealtime() - DisplayPowerController.this.mScreenOnForKeyguardDismissBlockStartRealTime;
            if (delay > 1000) {
                Slog.i(DisplayPowerController.TAG, "fingerunlock--onKeyguardDismissDone delay " + delay);
            }
            Message msg = DisplayPowerController.this.mHandler.obtainMessage(5, this);
            msg.setAsynchronous(true);
            DisplayPowerController.this.mHandler.sendMessage(msg);
        }
    }

    private final class ScreenOnUnblocker implements ScreenOnListener {
        private ScreenOnUnblocker() {
        }

        public void onScreenOn() {
            Message msg = DisplayPowerController.this.mHandler.obtainMessage(3, this);
            msg.setAsynchronous(true);
            DisplayPowerController.this.mHandler.sendMessage(msg);
        }
    }

    static {
        boolean z;
        boolean z2 = true;
        if (Log.HWINFO) {
            z = true;
        } else if (Log.HWModuleLog) {
            z = Log.isLoggable(TAG, 4);
        } else {
            z = false;
        }
        DEBUG = z;
        if (Log.HWLog) {
            z = true;
        } else if (Log.HWModuleLog) {
            z = Log.isLoggable(TAG, 3);
        } else {
            z = false;
        }
        DEBUG_Controller = z;
        if (!DEBUG) {
            z2 = false;
        }
        DEBUG_FPLOG = z2;
    }

    private void setPowerStatus(boolean powerStatus) {
        if (this.mAutomaticBrightnessController != null) {
            this.mAutomaticBrightnessController.setPowerStatus(powerStatus);
        }
    }

    public void setAodAlpmState(int globalState) {
        Slog.i(TAG, "mGlobalAlpmState = " + globalState);
        this.mGlobalAlpmState = globalState;
        if (this.mGlobalAlpmState == 1) {
            sendUpdatePowerState();
        }
    }

    public void setBacklightBrightness(BacklightBrightness backlightBrightness) {
        this.mAutomaticBrightnessController.setBacklightBrightness(backlightBrightness);
    }

    public void setCameraModeBrightnessLineEnable(boolean cameraModeBrightnessLineEnable) {
        this.mAutomaticBrightnessController.setCameraModeBrightnessLineEnable(cameraModeBrightnessLineEnable);
    }

    public void updateAutoBrightnessAdjustFactor(float adjustFactor) {
        this.mAutomaticBrightnessController.updateAutoBrightnessAdjustFactor(adjustFactor);
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
        this.mKeyguardIsLocked = isLocked;
        this.mAutomaticBrightnessController.setKeyguardLockedStatus(this.mKeyguardIsLocked);
    }

    public boolean getRebootAutoModeEnable() {
        return this.mAutomaticBrightnessController.getRebootAutoModeEnable();
    }

    public DisplayPowerController(Context context, DisplayPowerCallbacks callbacks, Handler handler, SensorManager sensorManager, DisplayBlanker blanker) {
        this.mHandler = new DisplayControllerHandler(handler.getLooper());
        this.mCallbacks = callbacks;
        this.mBatteryStats = BatteryStatsService.getService();
        this.mLights = (LightsManager) LocalServices.getService(LightsManager.class);
        this.mSensorManager = sensorManager;
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mBlanker = blanker;
        this.mContext = context;
        this.mBackLight = this.mLights.getLight(0);
        NEED_NEW_BRIGHTNESS_PROCESS = this.mBackLight.isHighPrecision();
        Resources resources = context.getResources();
        int screenBrightnessSettingMinimum = clampAbsoluteBrightness(resources.getInteger(17694851));
        this.mScreenBrightnessDozeConfig = clampAbsoluteBrightness(resources.getInteger(17694845));
        this.mScreenBrightnessDimConfig = clampAbsoluteBrightness(resources.getInteger(17694844));
        this.mScreenBrightnessDarkConfig = clampAbsoluteBrightness(resources.getInteger(17694843));
        if (this.mScreenBrightnessDarkConfig > this.mScreenBrightnessDimConfig) {
            Slog.w(TAG, "Expected config_screenBrightnessDark (" + this.mScreenBrightnessDarkConfig + ") to be less than or equal to " + "config_screenBrightnessDim (" + this.mScreenBrightnessDimConfig + ").");
        }
        if (this.mScreenBrightnessDarkConfig > screenBrightnessSettingMinimum) {
            Slog.w(TAG, "Expected config_screenBrightnessDark (" + this.mScreenBrightnessDarkConfig + ") to be less than or equal to " + "config_screenBrightnessSettingMinimum (" + screenBrightnessSettingMinimum + ").");
        }
        int screenBrightnessRangeMinimum = Math.min(Math.min(screenBrightnessSettingMinimum, this.mScreenBrightnessDimConfig), this.mScreenBrightnessDarkConfig);
        this.mScreenBrightnessRangeMaximum = 255;
        this.mUseSoftwareAutoBrightnessConfig = resources.getBoolean(17956894);
        this.mAllowAutoBrightnessWhileDozingConfig = resources.getBoolean(17956872);
        this.mUseSensorHubLABC = SystemProperties.getBoolean(USE_SENSORHUB_LABC_PROP, false);
        this.mBrightnessRampRateFast = resources.getInteger(17694747);
        this.mBrightnessRampRateSlow = resources.getInteger(17694748);
        this.mSkipScreenOnBrightnessRamp = resources.getBoolean(17957019);
        int lightSensorRate = resources.getInteger(17694737);
        int initialLightSensorRate = resources.getInteger(17694736);
        if (initialLightSensorRate == -1) {
            initialLightSensorRate = lightSensorRate;
        } else if (initialLightSensorRate > lightSensorRate) {
            Slog.w(TAG, "Expected config_autoBrightnessInitialLightSensorRate (" + initialLightSensorRate + ") to be less than or equal to " + "config_autoBrightnessLightSensorRate (" + lightSensorRate + ").");
        }
        long brighteningLightDebounce = (long) resources.getInteger(17694734);
        long darkeningLightDebounce = (long) resources.getInteger(17694735);
        boolean autoBrightnessResetAmbientLuxAfterWarmUp = resources.getBoolean(17956890);
        int ambientLightHorizon = resources.getInteger(17694733);
        float autoBrightnessAdjustmentMaxGamma = resources.getFraction(18022400, 1, 1);
        HysteresisLevels hysteresisLevels = new HysteresisLevels(resources.getIntArray(17236004), resources.getIntArray(17236005), resources.getIntArray(17236006));
        if (this.mUseSoftwareAutoBrightnessConfig) {
            int[] lux = resources.getIntArray(17235986);
            int[] screenBrightness = resources.getIntArray(17235985);
            int lightSensorWarmUpTimeConfig = resources.getInteger(17694798);
            float dozeScaleFactor = resources.getFraction(18022403, 1, 1);
            if (!this.mUseSensorHubLABC) {
                Spline screenAutoBrightnessSpline = createAutoBrightnessSpline(lux, screenBrightness);
                if (screenAutoBrightnessSpline == null) {
                    Slog.e(TAG, "Error in config.xml.  config_autoBrightnessLcdBacklightValues (size " + screenBrightness.length + ") " + "must be monotic and have exactly one more entry than " + "config_autoBrightnessLevels (size " + lux.length + ") " + "which must be strictly increasing.  " + "Auto-brightness will be disabled.");
                    this.mUseSoftwareAutoBrightnessConfig = false;
                } else {
                    int bottom = clampAbsoluteBrightness(screenBrightness[0]);
                    if (this.mScreenBrightnessDarkConfig > bottom) {
                        Slog.w(TAG, "config_screenBrightnessDark (" + this.mScreenBrightnessDarkConfig + ") should be less than or equal to the first value of " + "config_autoBrightnessLcdBacklightValues (" + bottom + ").");
                    }
                    if (bottom < screenBrightnessRangeMinimum) {
                        screenBrightnessRangeMinimum = bottom;
                    }
                    IHwAutomaticBrightnessController iadm = HwServiceFactory.getHuaweiAutomaticBrightnessController();
                    if (iadm != null) {
                        this.mAutomaticBrightnessController = iadm.getInstance(this, handler.getLooper(), sensorManager, screenAutoBrightnessSpline, lightSensorWarmUpTimeConfig, screenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum, dozeScaleFactor, lightSensorRate, initialLightSensorRate, brighteningLightDebounce, darkeningLightDebounce, autoBrightnessResetAmbientLuxAfterWarmUp, ambientLightHorizon, autoBrightnessAdjustmentMaxGamma, hysteresisLevels, this.mContext);
                    } else {
                        this.mAutomaticBrightnessController = new AutomaticBrightnessController(this, handler.getLooper(), sensorManager, screenAutoBrightnessSpline, lightSensorWarmUpTimeConfig, screenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum, dozeScaleFactor, lightSensorRate, initialLightSensorRate, brighteningLightDebounce, darkeningLightDebounce, autoBrightnessResetAmbientLuxAfterWarmUp, ambientLightHorizon, autoBrightnessAdjustmentMaxGamma, hysteresisLevels);
                    }
                }
            }
            this.fpDataCollector = FingerprintUnlockDataCollector.getInstance();
        }
        this.mScreenBrightnessRangeMinimum = screenBrightnessRangeMinimum;
        this.mColorFadeEnabled = ActivityManager.isLowRamDeviceStatic() ^ 1;
        this.mColorFadeFadesConfig = resources.getBoolean(17956887);
        this.mDisplayBlanksAfterDozeConfig = resources.getBoolean(17956929);
        this.mBrightnessBucketsInDozeConfig = resources.getBoolean(17956930);
        this.mProximitySensor = this.mSensorManager.getDefaultSensor(8);
        if (this.mProximitySensor != null) {
            this.mProximityThreshold = Math.min(this.mProximitySensor.getMaximumRange(), 5.0f);
        }
        this.mDisplayEngineInterface = HwServiceFactory.getDisplayEngineInterface();
        if (this.mDisplayEngineInterface != null) {
            this.mDisplayEngineInterface.initialize();
            this.mUsingSRE = this.mDisplayEngineInterface.getSupported("FEATURE_SRE");
            Slog.i(TAG, "DisplayEngineInterface getSupported SRE:" + this.mUsingSRE);
        }
        int smartBackLightConfig = SystemProperties.getInt("ro.config.hw_smart_backlight", 1);
        if (this.mUsingSRE || smartBackLightConfig == 1) {
            if (this.mUsingSRE) {
                Slog.i(TAG, "Use SRE instead of SBL");
            } else {
                this.mSmartBackLightSupported = true;
                if (DEBUG) {
                    Slog.i(TAG, "get ro.config.hw_smart_backlight = 1");
                }
            }
            int smartBackLightSetting = System.getInt(this.mContext.getContentResolver(), "smart_backlight_enable", -1);
            if (smartBackLightSetting == -1) {
                if (DEBUG) {
                    Slog.i(TAG, "get Settings.System.SMART_BACKLIGHT failed, set default value to 1");
                }
                System.putInt(this.mContext.getContentResolver(), "smart_backlight_enable", 1);
            } else if (DEBUG) {
                Slog.i(TAG, "get Settings.System.SMART_BACKLIGHT = " + smartBackLightSetting);
            }
        } else if (DEBUG) {
            Slog.i(TAG, "get ro.config.hw_smart_backlight = " + smartBackLightConfig + ", mUsingSRE = false, don't support sbl or sre");
        }
        IHwNormalizedManualBrightnessController iadm2 = HwServiceFactory.getHuaweiManualBrightnessController();
        if (iadm2 != null) {
            this.mManualBrightnessController = iadm2.getInstance(this, this.mContext, this.mSensorManager);
            if (DEBUG) {
                Slog.i(TAG, "HBM ManualBrightnessController initialized");
            }
        } else {
            this.mManualBrightnessController = new ManualBrightnessController(this);
        }
        if (this.mUseSensorHubLABC) {
            this.mLABCSensor = this.mSensorManager.getDefaultSensor(65543);
            if (this.mLABCSensor == null) {
                Slog.e(TAG, "[LABC] Get LABC Sensor failed !! ");
            }
        } else if (this.mSmartBackLightSupported) {
            this.mHwSmartBackLightController = HwServiceFactory.getHwSmartBackLightController();
            if (this.mHwSmartBackLightController != null) {
                this.mUsingHwSmartBackLightController = this.mHwSmartBackLightController.checkIfUsingHwSBL();
                this.mHwSmartBackLightController.StartHwSmartBackLightController(this.mContext, this.mLights, this.mSensorManager);
            }
        }
        this.mDisplayEffectMonitor = HwServiceFactory.getDisplayEffectMonitor(this.mContext);
        if (this.mDisplayEffectMonitor == null) {
            Slog.e(TAG, "getDisplayEffectMonitor failed!");
        }
    }

    public boolean isProximitySensorAvailable() {
        return this.mProximitySensor != null;
    }

    public boolean requestPowerState(DisplayPowerRequest request, boolean waitForNegativeProximity) {
        boolean z;
        if (DEBUG && DEBUG_Controller) {
            Slog.d(TAG, "requestPowerState: " + request + ", waitForNegativeProximity=" + waitForNegativeProximity);
        }
        synchronized (this.mLock) {
            boolean changed = false;
            if (waitForNegativeProximity) {
                if ((this.mPendingWaitForNegativeProximityLocked ^ 1) != 0) {
                    this.mPendingWaitForNegativeProximityLocked = true;
                    changed = true;
                }
            }
            if (this.mPendingRequestLocked == null) {
                this.mPendingRequestLocked = new DisplayPowerRequest(request);
                changed = true;
            } else if (!this.mPendingRequestLocked.equals(request)) {
                this.mPendingRequestLocked.copyFrom(request);
                changed = true;
            }
            if (changed) {
                this.mDisplayReadyLocked = false;
            }
            if (changed && (this.mPendingRequestChangedLocked ^ 1) != 0) {
                this.mPendingRequestChangedLocked = true;
                sendUpdatePowerStateLocked();
            }
            z = this.mDisplayReadyLocked;
        }
        return z;
    }

    private void sendUpdatePowerState() {
        synchronized (this.mLock) {
            sendUpdatePowerStateLocked();
        }
    }

    private void sendUpdatePowerStateLocked() {
        if (!this.mPendingUpdatePowerStateLocked) {
            this.mPendingUpdatePowerStateLocked = true;
            Message msg = this.mHandler.obtainMessage(1);
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
        }
    }

    private void initialize() {
        ColorFade colorFade = null;
        Context context = this.mContext;
        DisplayBlanker displayBlanker = this.mBlanker;
        if (this.mColorFadeEnabled) {
            colorFade = new ColorFade(0);
        }
        this.mPowerState = new DisplayPowerState(context, displayBlanker, colorFade);
        this.mAutoCustomBackLight = this.mLights.getLight(LightsManager.LIGHT_ID_AUTOCUSTOMBACKLIGHT);
        this.mManualCustomBackLight = this.mLights.getLight(LightsManager.LIGHT_ID_MANUALCUSTOMBACKLIGHT);
        if (this.mColorFadeEnabled) {
            this.mColorFadeOnAnimator = ObjectAnimator.ofFloat(this.mPowerState, DisplayPowerState.COLOR_FADE_LEVEL, new float[]{0.0f, 1.0f});
            this.mColorFadeOnAnimator.setDuration(250);
            this.mColorFadeOnAnimator.addListener(this.mAnimatorListener);
            this.mColorFadeOffAnimator = ObjectAnimator.ofFloat(this.mPowerState, DisplayPowerState.COLOR_FADE_LEVEL, new float[]{1.0f, 0.0f});
            this.mColorFadeOffAnimator.setDuration(150);
            this.mColorFadeOffAnimator.addListener(this.mAnimatorListener);
        }
        IHwRampAnimator iadm = HwServiceFactory.getHwNormalizedRampAnimator();
        if (iadm != null) {
            this.mScreenBrightnessRampAnimator = iadm.getInstance(this.mPowerState, DisplayPowerState.SCREEN_BRIGHTNESS);
        } else {
            this.mScreenBrightnessRampAnimator = new RampAnimator(this.mPowerState, DisplayPowerState.SCREEN_BRIGHTNESS);
        }
        this.mScreenBrightnessRampAnimator.setListener(this.mRampAnimatorListener);
        try {
            this.mBatteryStats.noteScreenState(this.mPowerState.getScreenState());
            this.mBatteryStats.noteScreenBrightness(this.mPowerState.getScreenBrightness());
        } catch (RemoteException e) {
        }
    }

    private int getBrightness(boolean autoBrightnessAdjustmentChanged) {
        if (autoBrightnessAdjustmentChanged) {
            return this.mPendingBacklight;
        }
        return 0;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updatePowerState() {
        boolean mustInitialize = false;
        boolean autoBrightnessAdjustmentChanged = false;
        synchronized (this.mLock) {
            this.mPendingUpdatePowerStateLocked = false;
            if (this.mPendingRequestLocked == null) {
                return;
            }
            if (this.mPowerRequest == null) {
                this.mPowerRequest = new DisplayPowerRequest(this.mPendingRequestLocked);
                this.mWaitingForNegativeProximity = this.mPendingWaitForNegativeProximityLocked;
                this.mPendingWaitForNegativeProximityLocked = false;
                this.mPendingRequestChangedLocked = false;
                mustInitialize = true;
                if (this.mUseSensorHubLABC) {
                    this.mLastStatus = true;
                    autoBrightnessAdjustmentChanged = true;
                }
            } else if (this.mPendingRequestChangedLocked) {
                autoBrightnessAdjustmentChanged = this.mPowerRequest.screenAutoBrightnessAdjustment != this.mPendingRequestLocked.screenAutoBrightnessAdjustment;
                boolean z = this.mPowerRequest.useAutoBrightness != this.mPendingRequestLocked.useAutoBrightness ? this.mPowerRequest.brightnessSetByUser == this.mPendingRequestLocked.brightnessSetByUser : false;
                this.mBrightnessModeChanged = z;
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
                if (!(!this.mBrightnessModeChanged || (this.mCurrentUserIdChange ^ 1) == 0 || !this.mPowerRequest.useAutoBrightness || isCoverModeChanged || (this.mIsCoverModeClosed ^ 1) == 0 || (this.mBrightnessModeChangeNoClearOffsetEnable ^ 1) == 0 || (this.mModeToAutoNoClearOffsetEnable ^ 1) == 0)) {
                    updateAutoBrightnessAdjustFactor(0.0f);
                    if (DEBUG) {
                        Slog.d(TAG, "AdjustPositionBrightness set 0");
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
                this.mCurrentUserIdChange = false;
                this.mWaitingForNegativeProximity |= this.mPendingWaitForNegativeProximityLocked;
                this.mPendingWaitForNegativeProximityLocked = false;
                this.mPendingRequestChangedLocked = false;
                this.mDisplayReadyLocked = false;
                writeAutoBrightnessDbEnable(this.mPowerRequest.useAutoBrightness);
                if (this.mUseSensorHubLABC && !this.mLastStatus) {
                    autoBrightnessAdjustmentChanged = true;
                }
            }
            boolean mustNotify = this.mDisplayReadyLocked ^ 1;
        }
    }

    private void blackScreenOnPcMode() {
        HwPCUtils.log(TAG, "brightness set 0 in PC mode");
        try {
            IHwPCManager pcMgr = HwPCUtils.getHwPCManager();
            if (pcMgr != null) {
                pcMgr.setScreenPower(false);
            }
        } catch (RemoteException e) {
            HwPCUtils.log(TAG, "blackScreenOnPcMode " + e);
        }
    }

    private void sre_init(int state) {
        if (!this.mUseSensorHubLABC) {
            if (this.mSmartBackLightSupported && this.mSmartBackLightEnabled != this.mPowerRequest.useSmartBacklight) {
                if (DEBUG) {
                    Slog.i(TAG, "mPowerRequest.useSmartBacklight change " + this.mSmartBackLightEnabled + " -> " + this.mPowerRequest.useSmartBacklight);
                }
                this.mSmartBackLightEnabled = this.mPowerRequest.useSmartBacklight;
            }
            if (this.mUsingHwSmartBackLightController) {
                this.mHwSmartBackLightController.updatePowerState(state, this.mSmartBackLightEnabled);
            }
            if (this.mDisplayEngineInterface != null) {
                boolean lightSensorOnEnable = wantScreenOn(state);
                if (this.mLightSensorOnEnable != lightSensorOnEnable) {
                    Slog.i(TAG, "LightSensorEnable change " + this.mLightSensorOnEnable + " -> " + lightSensorOnEnable);
                    this.mDisplayEngineInterface.updateLightSensorState(lightSensorOnEnable);
                    this.mLightSensorOnEnable = lightSensorOnEnable;
                }
            }
            if (this.mUsingSRE && this.mDisplayEngineInterface != null) {
                this.mSREEnabled = this.mPowerRequest.useSmartBacklight;
                Slog.i(TAG, "mPowerRequest.useSmartBacklight : " + this.mSREEnabled);
                if (this.mSREEnabled) {
                    this.mDisplayEngineInterface.setScene("SCENE_SRE", "ACTION_MODE_ON");
                } else {
                    this.mDisplayEngineInterface.setScene("SCENE_SRE", "ACTION_MODE_OFF");
                }
            }
        } else if (this.mLABCSensor != null) {
            this.mLABCEnabled = true;
            setLABCEnabled(wantScreenOn(state));
        }
    }

    private void hbm_init(int state) {
        if (SystemProperties.getInt("ro.config.hw_high_bright_mode", 1) == 1 && getManualModeEnable()) {
            boolean isManulMode = this.mPowerRequest.useAutoBrightness ^ 1;
            this.mAutomaticBrightnessController.setManualModeEnableForPg(isManulMode);
            this.mManualBrightnessController.updatePowerState(state, isManulMode);
        }
    }

    private void sendScreenStateToDE(int state) {
        if (this.mDisplayEngineInterface != null) {
            int currentState = getScreenOnState(state);
            if (this.mIsScreenOn != currentState) {
                Slog.i(TAG, "ScreenState change " + this.mIsScreenOn + " -> " + currentState);
                if (currentState == 1) {
                    this.mDisplayEngineInterface.setScene("SCENE_REAL_POWERMODE", "ACTION_MODE_ON");
                    HwAodManagerEx.setPowerState(6);
                } else {
                    this.mDisplayEngineInterface.setScene("SCENE_REAL_POWERMODE", "ACTION_MODE_OFF");
                    HwAodManagerEx.setPowerState(7);
                }
                this.mIsScreenOn = currentState;
            }
        }
    }

    private void updateCoverModeStatus(boolean isClosed) {
        if (this.mAutomaticBrightnessController != null) {
            this.mAutomaticBrightnessController.setCoverModeStatus(isClosed);
        }
    }

    public void updateBrightness() {
        sendUpdatePowerState();
    }

    public void updateManualBrightnessForLux() {
        sendUpdatePowerState();
    }

    private void blockScreenOnForKeyguardDismiss() {
        if (this.mPendingScreenOnForKeyguardDismissUnblocker == null) {
            this.mPendingScreenOnForKeyguardDismissUnblocker = new ScreenOnForKeyguardDismissUnblocker();
            this.mScreenOnForKeyguardDismissBlockStartRealTime = SystemClock.elapsedRealtime();
            Slog.i(TAG, "Blocking screen on until keyguard dismiss done.");
        }
    }

    private void unblockScreenOnForKeyguardDismiss() {
        if (this.mPendingScreenOnForKeyguardDismissUnblocker != null) {
            this.mPendingScreenOnForKeyguardDismissUnblocker = null;
            Slog.i(TAG, "fingerunlock--Unblocked screen on for keyguard dismiss after " + (SystemClock.elapsedRealtime() - this.mScreenOnForKeyguardDismissBlockStartRealTime) + " ms");
        }
    }

    private void blockScreenOn() {
        if (this.mPendingScreenOnUnblocker == null) {
            Trace.asyncTraceBegin(131072, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
            this.mPendingScreenOnUnblocker = new ScreenOnUnblocker();
            this.mScreenOnBlockStartRealTime = SystemClock.elapsedRealtime();
            if (Jlog.isPerfTest()) {
                Jlog.i(2205, "JL_PWRSCRON_DPC_BLOCKSCREENON");
            }
            Flog.i(NativeResponseCode.SERVICE_FOUND, "DisplayPowerController Blocking screen on until initial contents have been drawn.");
        }
    }

    private void unblockScreenOn() {
        if (this.mPendingScreenOnUnblocker != null) {
            this.mPendingScreenOnUnblocker = null;
            long delay = SystemClock.elapsedRealtime() - this.mScreenOnBlockStartRealTime;
            if (Jlog.isPerfTest()) {
                Jlog.i(2206, "JL_PWRSCRON_DPC_UNBLOCKSCREENON");
            }
            Flog.i(NativeResponseCode.SERVICE_FOUND, "DisplayPowerController Unblocked screen on after " + delay + " ms");
            Trace.asyncTraceEnd(131072, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
        }
    }

    private void blockScreenOff() {
        if (this.mPendingScreenOffUnblocker == null) {
            Trace.asyncTraceBegin(131072, SCREEN_OFF_BLOCKED_TRACE_NAME, 0);
            this.mPendingScreenOffUnblocker = new ScreenOffUnblocker();
            this.mScreenOffBlockStartRealTime = SystemClock.elapsedRealtime();
            Slog.i(TAG, "Blocking screen off");
        }
    }

    private void unblockScreenOff() {
        if (this.mPendingScreenOffUnblocker != null) {
            this.mPendingScreenOffUnblocker = null;
            Slog.i(TAG, "Unblocked screen off after " + (SystemClock.elapsedRealtime() - this.mScreenOffBlockStartRealTime) + " ms");
            Trace.asyncTraceEnd(131072, SCREEN_OFF_BLOCKED_TRACE_NAME, 0);
        }
    }

    private boolean setScreenState(int state) {
        return setScreenState(state, false);
    }

    private boolean setScreenState(int state, boolean reportOnly) {
        boolean z = true;
        boolean isOff = state == 1;
        if (this.mPowerState.getScreenState() != state) {
            if (isOff && (!this.mScreenOffBecauseOfProximity || (this.mPowerRequest.useProximitySensor && this.mPowerRequest.useProximitySensorbyPhone))) {
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
                this.mPowerState.setScreenState(state);
                try {
                    this.mBatteryStats.noteScreenState(state);
                } catch (RemoteException e) {
                }
            }
        }
        boolean isDoze = mSupportAod && (state == 3 || state == 4);
        if (isOff && this.mReportedScreenStateToPolicy != 0 && (!this.mScreenOffBecauseOfProximity || (this.mPowerRequest.useProximitySensor && this.mPowerRequest.useProximitySensorbyPhone))) {
            setReportedScreenState(0);
            unblockScreenOn();
            this.mWindowManagerPolicy.screenTurnedOff();
            setPowerStatus(false);
            setBrightnessAnimationTime(false, 500);
            setBrightnessNoLimit(-1, 500);
            this.mBrightnessModeChangeNoClearOffsetEnable = this.mPoweroffModeChangeAutoEnable;
            if (this.mPoweroffModeChangeAutoEnable) {
                this.mPoweroffModeChangeAutoEnable = false;
                Slog.i(TAG, "poweroff set mPoweroffModeChangeAutoEnable=" + this.mPoweroffModeChangeAutoEnable);
            }
        } else if (!(isOff || (isDoze ^ 1) == 0 || this.mReportedScreenStateToPolicy != 3)) {
            unblockScreenOff();
            this.mWindowManagerPolicy.screenTurnedOff();
            setReportedScreenState(0);
        }
        if (!(isOff || (isDoze ^ 1) == 0 || this.mReportedScreenStateToPolicy != 0)) {
            setReportedScreenState(1);
            if (this.mPowerState.getColorFadeLevel() == 0.0f) {
                blockScreenOn();
            } else {
                unblockScreenOn();
            }
            this.mWindowManagerPolicy.screenTurningOn(this.mPendingScreenOnUnblocker);
            setPowerStatus(true);
        }
        if (this.mPendingScreenOnUnblocker != null) {
            z = false;
        }
        return z;
    }

    private void setReportedScreenState(int state) {
        Trace.traceCounter(131072, "ReportedScreenStateToPolicy", state);
        this.mReportedScreenStateToPolicy = state;
    }

    private boolean waitScreenBrightness(int displayState, boolean curReqWaitBright, boolean lastReqWaitBright, boolean enableBright, boolean skipWaitKeyguardDismiss) {
        boolean z = true;
        if (DEBUG && DEBUG_Controller) {
            Slog.i(TAG, "waitScreenBrightness displayState = " + displayState + " curReqWaitBright = " + curReqWaitBright + " lastReqWaitBright = " + lastReqWaitBright + " enableBright = " + enableBright + " skipWaitKeyguardDismiss = " + skipWaitKeyguardDismiss);
        }
        if (displayState == 2) {
            if (curReqWaitBright) {
                return true;
            }
            if (lastReqWaitBright && enableBright && (skipWaitKeyguardDismiss ^ 1) != 0) {
                blockScreenOnForKeyguardDismiss();
                this.mWindowManagerPolicy.waitKeyguardDismissDone(this.mPendingScreenOnForKeyguardDismissUnblocker);
            }
        } else if (this.mPendingScreenOnForKeyguardDismissUnblocker != null) {
            unblockScreenOnForKeyguardDismiss();
            this.mWindowManagerPolicy.cancelWaitKeyguardDismissDone();
        }
        if (this.mPendingScreenOnForKeyguardDismissUnblocker == null) {
            z = false;
        }
        return z;
    }

    private int clampScreenBrightness(int value) {
        return MathUtils.constrain(value, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum);
    }

    private void animateScreenBrightness(int target, int rate) {
        if (DEBUG) {
            Slog.d(TAG, "Animating brightness: target=" + target + ", rate=" + rate);
        }
        if (target >= 0) {
            if (target == 0 && rate != 0) {
                rate = 0;
                Slog.e(TAG, "Animating brightness rate is invalid when screen off, set rate to 0");
            }
            if (this.mScreenBrightnessRampAnimator.animateTo(target, rate)) {
                Trace.traceCounter(131072, "TargetScreenBrightness", target);
                try {
                    if (this.mUsingHwSmartBackLightController && this.mSmartBackLightEnabled && rate > 0) {
                        if (this.mScreenBrightnessRampAnimator.isAnimating()) {
                            this.mHwSmartBackLightController.updateBrightnessState(0);
                        } else if (DEBUG) {
                            Slog.i(TAG, "brightness changed but not animating");
                        }
                    }
                    HwLog.dubaie("DUBAI_TAG_BRIGHTNESS", "brightness=" + target);
                    this.mBatteryStats.noteScreenBrightness(target);
                    sendTargetBrightnessToMonitor(target);
                } catch (RemoteException e) {
                }
            }
        }
    }

    private void animateScreenStateChange(int target, boolean performScreenOffTransition) {
        int i = 2;
        if (this.mColorFadeEnabled && (this.mColorFadeOnAnimator.isStarted() || this.mColorFadeOffAnimator.isStarted())) {
            if (target == 2) {
                this.mPendingScreenOff = false;
            } else {
                return;
            }
        }
        if (this.mDisplayBlanksAfterDozeConfig && Display.isDozeState(this.mPowerState.getScreenState()) && (Display.isDozeState(target) ^ 1) != 0) {
            boolean z;
            this.mPowerState.prepareColorFade(this.mContext, this.mColorFadeFadesConfig ? 2 : 0);
            if (this.mColorFadeOffAnimator != null) {
                this.mColorFadeOffAnimator.end();
            }
            if (target != 1) {
                z = true;
            } else {
                z = false;
            }
            setScreenState(1, z);
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
            if (!(this.mScreenBrightnessRampAnimator.isAnimating() && this.mPowerState.getScreenState() == 2) && setScreenState(5)) {
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
            }
        } else if (target == 3) {
            if (!(this.mScreenBrightnessRampAnimator.isAnimating() && this.mPowerState.getScreenState() == 2) && setScreenState(3)) {
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
            }
        } else if (target != 4) {
            this.mPendingScreenOff = true;
            if (!this.mColorFadeEnabled) {
                this.mPowerState.setColorFadeLevel(0.0f);
            }
            if (this.mPowerState.getColorFadeLevel() == 0.0f) {
                setScreenState(1);
                this.mPendingScreenOff = false;
                this.mPowerState.dismissColorFadeResources();
            } else {
                if (performScreenOffTransition && (checkPhoneWindowIsTop() ^ 1) != 0) {
                    DisplayPowerState displayPowerState = this.mPowerState;
                    Context context = this.mContext;
                    if (!this.mColorFadeFadesConfig) {
                        i = 1;
                    }
                    if (displayPowerState.prepareColorFade(context, i) && this.mPowerState.getScreenState() != 1) {
                        this.mColorFadeOffAnimator.start();
                    }
                }
                this.mColorFadeOffAnimator.end();
            }
        } else if (!this.mScreenBrightnessRampAnimator.isAnimating() || this.mPowerState.getScreenState() == 4) {
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
    }

    private void setProximitySensorEnabled(boolean enable) {
        if (enable) {
            if (!this.mProximitySensorEnabled) {
                this.mProximitySensorEnabled = true;
                this.mSensorManager.registerListener(this.mProximitySensorListener, this.mProximitySensor, 1, this.mHandler);
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

    private void handleProximitySensorEvent(long time, boolean positive) {
        if (this.mProximitySensorEnabled && (this.mPendingProximity != 0 || (positive ^ 1) == 0)) {
            if (this.mPendingProximity != 1 || !positive) {
                HwServiceFactory.reportProximitySensorEventToIAware(positive);
                Slog.d(TAG, "handleProximitySensorEvent positive:" + positive);
                this.mHandler.removeMessages(2);
                if (positive) {
                    this.mPendingProximity = 1;
                    setPendingProximityDebounceTime(time + 0);
                } else {
                    this.mPendingProximity = 0;
                    setPendingProximityDebounceTime(time + 0);
                }
                debounceProximitySensor();
            }
        }
    }

    private void debounceProximitySensor() {
        if (this.mProximitySensorEnabled && this.mPendingProximity != -1 && this.mPendingProximityDebounceTime >= 0) {
            if (this.mPendingProximityDebounceTime <= SystemClock.uptimeMillis()) {
                this.mProximity = this.mPendingProximity;
                updatePowerState();
                clearPendingProximityDebounceTime();
                return;
            }
            Message msg = this.mHandler.obtainMessage(2);
            msg.setAsynchronous(true);
            this.mHandler.sendMessageAtTime(msg, this.mPendingProximityDebounceTime);
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

    private void setLABCEnabled(boolean enable) {
        if (enable) {
            if (!this.mLABCSensorEnabled) {
                this.mLABCSensorEnabled = true;
                this.mSensorManager.registerListener(this.mLABCSensorListener, this.mLABCSensor, 500000);
            }
        } else if (this.mLABCSensorEnabled) {
            this.mLABCSensorEnabled = false;
            this.mSensorManager.unregisterListener(this.mLABCSensorListener);
        }
    }

    private void sendOnStateChangedWithWakelock() {
        this.mCallbacks.acquireSuspendBlocker();
        this.mHandler.post(this.mOnStateChangedRunnable);
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
        pw.println("  mScreenBrightnessDarkConfig=" + this.mScreenBrightnessDarkConfig);
        pw.println("  mScreenBrightnessRangeMinimum=" + this.mScreenBrightnessRangeMinimum);
        pw.println("  mScreenBrightnessRangeMaximum=" + this.mScreenBrightnessRangeMaximum);
        pw.println("  mUseSoftwareAutoBrightnessConfig=" + this.mUseSoftwareAutoBrightnessConfig);
        pw.println("  mAllowAutoBrightnessWhileDozingConfig=" + this.mAllowAutoBrightnessWhileDozingConfig);
        pw.println("  mColorFadeFadesConfig=" + this.mColorFadeFadesConfig);
        this.mHandler.runWithScissors(new Runnable() {
            public void run() {
                DisplayPowerController.this.dumpLocal(pw);
            }
        }, 1000);
    }

    private void dumpLocal(PrintWriter pw) {
        pw.println();
        pw.println("Display Power Controller Thread State:");
        pw.println("  mPowerRequest=" + this.mPowerRequest);
        pw.println("  mWaitingForNegativeProximity=" + this.mWaitingForNegativeProximity);
        pw.println("  mProximitySensor=" + this.mProximitySensor);
        pw.println("  mProximitySensorEnabled=" + this.mProximitySensorEnabled);
        pw.println("  mProximityThreshold=" + this.mProximityThreshold);
        pw.println("  mProximity=" + proximityToString(this.mProximity));
        pw.println("  mPendingProximity=" + proximityToString(this.mPendingProximity));
        pw.println("  mPendingProximityDebounceTime=" + TimeUtils.formatUptime(this.mPendingProximityDebounceTime));
        pw.println("  mScreenOffBecauseOfProximity=" + this.mScreenOffBecauseOfProximity);
        pw.println("  mAppliedAutoBrightness=" + this.mAppliedAutoBrightness);
        pw.println("  mAppliedDimming=" + this.mAppliedDimming);
        pw.println("  mAppliedLowPower=" + this.mAppliedLowPower);
        pw.println("  mPendingScreenOnUnblocker=" + this.mPendingScreenOnUnblocker);
        pw.println("  mPendingScreenOff=" + this.mPendingScreenOff);
        pw.println("  mReportedToPolicy=" + reportedToPolicyToString(this.mReportedScreenStateToPolicy));
        pw.println("  mScreenBrightnessRampAnimator.isAnimating()=" + this.mScreenBrightnessRampAnimator.isAnimating());
        if (this.mColorFadeOnAnimator != null) {
            pw.println("  mColorFadeOnAnimator.isStarted()=" + this.mColorFadeOnAnimator.isStarted());
        }
        if (this.mColorFadeOffAnimator != null) {
            pw.println("  mColorFadeOffAnimator.isStarted()=" + this.mColorFadeOffAnimator.isStarted());
        }
        if (this.mPowerState != null) {
            this.mPowerState.dump(pw);
        }
        if (this.mAutomaticBrightnessController != null) {
            this.mAutomaticBrightnessController.dump(pw);
        }
    }

    private static String proximityToString(int state) {
        switch (state) {
            case -1:
                return "Unknown";
            case 0:
                return "Negative";
            case 1:
                return "Positive";
            default:
                return Integer.toString(state);
        }
    }

    private static String reportedToPolicyToString(int state) {
        switch (state) {
            case 0:
                return "REPORTED_TO_POLICY_SCREEN_OFF";
            case 1:
                return "REPORTED_TO_POLICY_SCREEN_TURNING_ON";
            case 2:
                return "REPORTED_TO_POLICY_SCREEN_ON";
            default:
                return Integer.toString(state);
        }
    }

    private static boolean wantScreenOn(int state) {
        switch (state) {
            case 2:
            case 3:
                return true;
            default:
                return false;
        }
    }

    private static boolean isScreenOn(int state) {
        return state != 1;
    }

    private static int getScreenOnState(int state) {
        switch (state) {
            case 1:
                return 0;
            case 2:
                return 1;
            default:
                return 2;
        }
    }

    private static Spline createAutoBrightnessSpline(int[] lux, int[] brightness) {
        if (lux == null || lux.length == 0 || brightness == null || brightness.length == 0) {
            Slog.e(TAG, "Could not create auto-brightness spline.");
            return null;
        }
        try {
            int n = brightness.length;
            float[] x = new float[n];
            float[] y = new float[n];
            y[0] = normalizeAbsoluteBrightness(brightness[0]);
            for (int i = 1; i < n; i++) {
                x[i] = (float) lux[i - 1];
                y[i] = normalizeAbsoluteBrightness(brightness[i]);
            }
            Spline spline = Spline.createSpline(x, y);
            if (DEBUG) {
                Slog.d(TAG, "Auto-brightness spline: " + spline);
                for (float v = 1.0f; v < ((float) lux[lux.length - 1]) * 1.25f; v *= 1.25f) {
                    Slog.d(TAG, String.format("  %7.1f: %7.1f", new Object[]{Float.valueOf(v), Float.valueOf(spline.interpolate(v))}));
                }
            }
            return spline;
        } catch (IllegalArgumentException ex) {
            Slog.e(TAG, "Could not create auto-brightness spline.", ex);
            return null;
        }
    }

    private static float normalizeAbsoluteBrightness(int value) {
        return ((float) clampAbsoluteBrightness(value)) / 255.0f;
    }

    private static int clampAbsoluteBrightness(int value) {
        return MathUtils.constrain(value, 0, 255);
    }

    private boolean checkPhoneWindowIsTop() {
        String incalluiPackageName = "com.android.incallui";
        String incalluiClassName = "com.android.incallui.InCallActivity";
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        long startTime = SystemClock.elapsedRealtime();
        List<RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(1);
        long getRunningTasksDuration = SystemClock.elapsedRealtime() - startTime;
        if (getRunningTasksDuration > 500) {
            Slog.i(TAG, "Check Phone Window is top, get the Running Tasks duration: " + getRunningTasksDuration);
        }
        if (tasksInfo != null && tasksInfo.size() > 0) {
            ComponentName cn = ((RunningTaskInfo) tasksInfo.get(0)).topActivity;
            if (incalluiPackageName.equals(cn.getPackageName()) && incalluiClassName.equals(cn.getClassName())) {
                if (DEBUG) {
                    Slog.i(TAG, "checkPhoneWindowIsTop: incallui window is top");
                }
                return true;
            }
        }
        return false;
    }

    private void writeAutoBrightnessDbEnable(boolean autoEnable) {
        if (NEED_NEW_BRIGHTNESS_PROCESS) {
            if (this.mPowerRequest.policy == 2 || (autoEnable ^ 1) != 0) {
                this.mBackLight.writeAutoBrightnessDbEnable(false);
            } else if (!this.mPowerPolicyChangeFromDimming) {
                this.mBackLight.writeAutoBrightnessDbEnable(true);
            }
        }
    }

    public void updateProximityState(boolean proximityState) {
        if (DEBUG) {
            Slog.d(TAG, "updateProximityState:" + proximityState);
        }
        this.mProximityPositive = proximityState;
        this.mScreenBrightnessRampAnimator.updateProximityState(proximityState);
    }

    public boolean getManualModeEnable() {
        if (this.mManualBrightnessController != null) {
            return this.mManualBrightnessController.getManualModeEnable();
        }
        Slog.e(TAG, "mManualBrightnessController=null");
        return false;
    }

    public void updatemManualModeAnimationEnable() {
        if (getManualModeEnable() && this.mScreenBrightnessRampAnimator != null) {
            this.mScreenBrightnessRampAnimator.updatemManualModeAnimationEnable(this.mManualBrightnessController.getManualModeAnimationEnable());
        }
    }

    public void updateManualPowerSavingAnimationEnable() {
        if (this.mManualBrightnessController != null && this.mScreenBrightnessRampAnimator != null) {
            boolean manualPowerSavingAnimationEnable = this.mManualBrightnessController.getManualPowerSavingAnimationEnable();
            this.mScreenBrightnessRampAnimator.updateManualPowerSavingAnimationEnable(manualPowerSavingAnimationEnable);
            if (manualPowerSavingAnimationEnable) {
                this.mManualBrightnessController.setManualPowerSavingAnimationEnable(false);
            }
        }
    }

    public void updateManualThermalModeAnimationEnable() {
        if (this.mManualBrightnessController != null && this.mScreenBrightnessRampAnimator != null && this.mManualBrightnessController.getManualThermalModeEnable()) {
            boolean manualThermalModeAnimationEnable = this.mManualBrightnessController.getManualThermalModeAnimationEnable();
            this.mScreenBrightnessRampAnimator.updateManualThermalModeAnimationEnable(manualThermalModeAnimationEnable);
            if (manualThermalModeAnimationEnable) {
                this.mManualBrightnessController.setManualThermalModeAnimationEnable(false);
            }
        }
    }

    public void updateBrightnessModeAnimationEnable() {
        if (this.mManualBrightnessController != null && this.mScreenBrightnessRampAnimator != null && this.mManualBrightnessController.getBrightnessSetByAppEnable()) {
            this.mScreenBrightnessRampAnimator.updateBrightnessModeAnimationEnable(this.mManualBrightnessController.getBrightnessSetByAppAnimationEnable(), this.mSetBrightnessNoLimitAnimationTime);
        }
    }

    public int getCoverModeBrightnessFromLastScreenBrightness() {
        return this.mAutomaticBrightnessController.getCoverModeBrightnessFromLastScreenBrightness();
    }

    public void setMaxBrightnessFromThermal(int brightness) {
        this.mAutomaticBrightnessController.setMaxBrightnessFromThermal(brightness);
        this.mManualBrightnessController.setMaxBrightnessFromThermal(brightness);
    }

    public void setModeToAutoNoClearOffsetEnable(boolean enable) {
        this.mModeToAutoNoClearOffsetEnable = enable;
        if (DEBUG) {
            Slog.d(TAG, "set mModeToAutoNoClearOffsetEnable=" + this.mModeToAutoNoClearOffsetEnable);
        }
    }

    public int setScreenBrightnessMappingtoIndoorMax(int brightness) {
        return this.mAutomaticBrightnessController.setScreenBrightnessMappingtoIndoorMax(brightness);
    }

    private void sendTargetBrightnessToMonitor(int brightness) {
        if (this.mDisplayEffectMonitor != null) {
            ArrayMap<String, Object> params = new ArrayMap();
            params.put("paramType", "algoDiscountBrightness");
            params.put("brightness", Integer.valueOf(brightness));
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    public void setPoweroffModeChangeAutoEnable(boolean enable) {
        this.mPoweroffModeChangeAutoEnable = enable;
        this.mBrightnessModeChangeNoClearOffsetEnable = this.mPoweroffModeChangeAutoEnable;
        if (DEBUG) {
            Slog.d(TAG, "set mPoweroffModeChangeAutoEnable=" + this.mPoweroffModeChangeAutoEnable + ",mNoClearOffsetEnable=" + this.mBrightnessModeChangeNoClearOffsetEnable);
        }
    }

    public void setBrightnessNoLimit(int brightness, int time) {
        this.mSetBrightnessNoLimitAnimationTime = time;
        this.mAutomaticBrightnessController.setBrightnessNoLimit(brightness, time);
        this.mManualBrightnessController.setBrightnessNoLimit(brightness, time);
    }
}
