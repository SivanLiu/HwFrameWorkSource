package com.android.server.display;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.common.HwFrameworkFactory;
import android.content.ComponentName;
import android.content.Context;
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
import android.hardware.display.DisplayManagerInternal.DisplayPowerCallbacks;
import android.hardware.display.DisplayManagerInternal.DisplayPowerRequest;
import android.net.Uri;
import android.os.Bundle;
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
import android.zrhung.IZrHung;
import android.zrhung.ZrHungData;
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
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.policy.WindowManagerPolicy.KeyguardDismissDoneListener;
import com.android.server.policy.WindowManagerPolicy.ScreenOffListener;
import com.android.server.policy.WindowManagerPolicy.ScreenOnListener;
import java.io.PrintWriter;
import java.util.List;

final class DisplayPowerController implements Callbacks, ManualBrightnessCallbacks {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final int BRIGHTNESS_FOR_PROXIMITY_POSITIVE = -2;
    private static final int BRIGHTNESS_RAMP_RATE_FAST = 200;
    private static final int BRIGHTNESS_RAMP_RATE_SLOW = 40;
    private static final int COLOR_FADE_OFF_ANIMATION_DURATION_MILLIS = 150;
    private static final int COLOR_FADE_ON_ANIMATION_DURATION_MILLIS = 250;
    private static boolean DEBUG = false;
    private static boolean DEBUG_Controller = false;
    private static boolean DEBUG_FPLOG = true;
    private static final boolean DEBUG_PRETEND_PROXIMITY_SENSOR_ABSENT = false;
    private static final int DISPALY_SCREEN_OFF = 7;
    private static final int DISPALY_SCREEN_ON = 6;
    private static final int GET_RUNNING_TASKS_FROM_AMS_WARNING_DURATION_MILLIS = 500;
    private static final int LIGHT_SENSOR_RATE_MILLIS = 1000;
    private static final int MSG_CONFIGURE_BRIGHTNESS = 5;
    private static final int MSG_PROXIMITY_SENSOR_DEBOUNCED = 2;
    private static final int MSG_SCREEN_OFF_UNBLOCKED = 4;
    private static final int MSG_SCREEN_ON_FOR_KEYGUARD_DISMISS_DONE = 8;
    private static final int MSG_SCREEN_ON_UNBLOCKED = 3;
    private static final int MSG_SET_TEMPORARY_AUTO_BRIGHTNESS_ADJUSTMENT = 7;
    private static final int MSG_SET_TEMPORARY_BRIGHTNESS = 6;
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
    private boolean mAppliedBrightnessBoost;
    private boolean mAppliedDimming;
    private boolean mAppliedLowPower;
    private boolean mAppliedScreenBrightnessOverride;
    private boolean mAppliedTemporaryAutoBrightnessAdjustment;
    private boolean mAppliedTemporaryBrightness;
    private float mAutoBrightnessAdjustment;
    private boolean mAutoBrightnessAdjustmentChanged = false;
    private boolean mAutoBrightnessEnabled = false;
    private Light mAutoCustomBackLight;
    private AutomaticBrightnessController mAutomaticBrightnessController;
    private Light mBackLight;
    private final IBatteryStats mBatteryStats;
    private final DisplayBlanker mBlanker;
    private boolean mBrightnessBucketsInDozeConfig;
    private BrightnessConfiguration mBrightnessConfiguration;
    private BrightnessMappingStrategy mBrightnessMapper;
    private boolean mBrightnessModeChangeNoClearOffsetEnable = false;
    private boolean mBrightnessModeChanged = false;
    private final int mBrightnessRampRateFast;
    private final int mBrightnessRampRateSlow;
    private final BrightnessTracker mBrightnessTracker;
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
    private int mCurrentScreenBrightnessSetting;
    private int mCurrentScreenBrightnessSettingForDB;
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
                    String str = DisplayPowerController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[LABC] onSensorChanged----BL =  ");
                    stringBuilder.append(Backlight);
                    stringBuilder.append(", AL=  ");
                    stringBuilder.append(Ambientlight);
                    stringBuilder.append(", FeedBack=  ");
                    stringBuilder.append(FeedBack);
                    Slog.d(str, stringBuilder.toString());
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
    private int mLastBrightnessForAutoBrightnessDB = 0;
    private int mLastBrightnessTarget;
    private boolean mLastStatus = false;
    private int mLastUserSetScreenBrightness;
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
    private float mPendingAutoBrightnessAdjustment;
    private int mPendingBacklight = -1;
    private int mPendingProximity = -1;
    private long mPendingProximityDebounceTime = -1;
    private boolean mPendingRequestChangedLocked;
    private DisplayPowerRequest mPendingRequestLocked;
    private int mPendingScreenBrightnessSetting;
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
                boolean positive = false;
                float distance = event.values[0];
                if (distance >= 0.0f && distance < DisplayPowerController.this.mProximityThreshold) {
                    positive = true;
                }
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
                IHwSmartBackLightController access$300 = DisplayPowerController.this.mHwSmartBackLightController;
                DisplayPowerController.this.mHwSmartBackLightController;
                access$300.updateBrightnessState(1);
            }
            if (DisplayPowerController.this.mPowerPolicyChangeFromDimming) {
                DisplayPowerController.this.mPowerPolicyChangeFromDimming = false;
                if (DisplayPowerController.DEBUG && DisplayPowerController.this.mPowerRequest != null) {
                    String str = DisplayPowerController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("update mPowerPolicyChangeFromDimming mPowerRequest.policy=");
                    stringBuilder.append(DisplayPowerController.this.mPowerRequest.policy);
                    Slog.d(str, stringBuilder.toString());
                }
                if (!(DisplayPowerController.this.mPowerRequest == null || DisplayPowerController.this.mContext == null || DisplayPowerController.this.mPowerRequest.policy != 0)) {
                    System.putIntForUser(DisplayPowerController.this.mContext.getContentResolver(), "screen_auto_brightness", 0, DisplayPowerController.this.mCurrentUserId);
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
    private final int mScreenBrightnessDefault;
    private final int mScreenBrightnessDimConfig;
    private final int mScreenBrightnessDozeConfig;
    private int mScreenBrightnessForVr;
    private final int mScreenBrightnessForVrDefault;
    private final int mScreenBrightnessForVrRangeMaximum;
    private final int mScreenBrightnessForVrRangeMinimum;
    private RampAnimator<DisplayPowerState> mScreenBrightnessRampAnimator;
    private final int mScreenBrightnessRangeMaximum;
    private final int mScreenBrightnessRangeMinimum;
    private boolean mScreenOffBecauseOfProximity;
    private long mScreenOffBlockStartRealTime;
    private boolean mScreenOnBecauseOfPhoneProximity;
    private long mScreenOnBlockStartRealTime;
    private long mScreenOnForKeyguardDismissBlockStartRealTime;
    private final SensorManager mSensorManager;
    private int mSetAutoBackLight = -1;
    private int mSetBrightnessNoLimitAnimationTime = 500;
    private final SettingsObserver mSettingsObserver;
    private int mSkipRampState = 0;
    private final boolean mSkipScreenOnBrightnessRamp;
    private boolean mSmartBackLightEnabled;
    private boolean mSmartBackLightSupported;
    private float mTemporaryAutoBrightnessAdjustment;
    private int mTemporaryScreenBrightness;
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
                    DisplayPowerController.this.mBrightnessConfiguration = (BrightnessConfiguration) msg.obj;
                    DisplayPowerController.this.updatePowerState();
                    return;
                case 6:
                    DisplayPowerController.this.mTemporaryScreenBrightness = msg.arg1;
                    DisplayPowerController.this.updatePowerState();
                    return;
                case 7:
                    DisplayPowerController.this.mTemporaryAutoBrightnessAdjustment = Float.intBitsToFloat(msg.arg1);
                    DisplayPowerController.this.updatePowerState();
                    return;
                case 8:
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

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            DisplayPowerController.this.handleSettingsChange(false);
        }
    }

    private final class ScreenOffUnblocker implements ScreenOffListener {
        private ScreenOffUnblocker() {
        }

        /* synthetic */ ScreenOffUnblocker(DisplayPowerController x0, AnonymousClass1 x1) {
            this();
        }

        public void onScreenOff() {
            DisplayPowerController.this.mHandler.sendMessage(DisplayPowerController.this.mHandler.obtainMessage(4, this));
        }
    }

    private final class ScreenOnForKeyguardDismissUnblocker implements KeyguardDismissDoneListener {
        private ScreenOnForKeyguardDismissUnblocker() {
        }

        /* synthetic */ ScreenOnForKeyguardDismissUnblocker(DisplayPowerController x0, AnonymousClass1 x1) {
            this();
        }

        public void onKeyguardDismissDone() {
            long delay = SystemClock.elapsedRealtime() - DisplayPowerController.this.mScreenOnForKeyguardDismissBlockStartRealTime;
            if (delay > 1000) {
                String str = DisplayPowerController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("fingerunlock--onKeyguardDismissDone delay ");
                stringBuilder.append(delay);
                Slog.i(str, stringBuilder.toString());
            }
            Message msg = DisplayPowerController.this.mHandler.obtainMessage(8, this);
            msg.setAsynchronous(true);
            DisplayPowerController.this.mHandler.sendMessage(msg);
        }
    }

    private final class ScreenOnUnblocker implements ScreenOnListener {
        private ScreenOnUnblocker() {
        }

        /* synthetic */ ScreenOnUnblocker(DisplayPowerController x0, AnonymousClass1 x1) {
            this();
        }

        public void onScreenOn() {
            DisplayPowerController.this.mHandler.sendMessage(DisplayPowerController.this.mHandler.obtainMessage(3, this));
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
        z = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        DEBUG_Controller = z;
    }

    private void setPowerStatus(boolean powerStatus) {
        if (this.mAutomaticBrightnessController != null) {
            this.mAutomaticBrightnessController.setPowerStatus(powerStatus);
        }
    }

    public void setAodAlpmState(int globalState) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mGlobalAlpmState = ");
        stringBuilder.append(globalState);
        Slog.i(str, stringBuilder.toString());
        this.mGlobalAlpmState = globalState;
        if (this.mGlobalAlpmState == 1) {
            sendUpdatePowerState();
            this.mDisplayEngineInterface.setScene("SCENE_AOD", "ACTION_MODE_OFF");
        } else if (this.mGlobalAlpmState == 0) {
            this.mDisplayEngineInterface.setScene("SCENE_AOD", "ACTION_MODE_ON");
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setAnimationTime animationEnabled=");
            stringBuilder.append(animationEnabled);
            stringBuilder.append(",millisecond=");
            stringBuilder.append(millisecond);
            Slog.i(str, stringBuilder.toString());
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
        Resources resources;
        DisplayPowerController displayPowerController;
        String str;
        Context context2 = context;
        this.mHandler = new DisplayControllerHandler(handler.getLooper());
        this.mBrightnessTracker = new BrightnessTracker(context2, null);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mCallbacks = callbacks;
        this.mBatteryStats = BatteryStatsService.getService();
        this.mLights = (LightsManager) LocalServices.getService(LightsManager.class);
        SensorManager sensorManager2 = sensorManager;
        this.mSensorManager = sensorManager2;
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mBlanker = blanker;
        this.mContext = context2;
        this.mBackLight = this.mLights.getLight(0);
        NEED_NEW_BRIGHTNESS_PROCESS = this.mBackLight.isHighPrecision();
        Resources resources2 = context.getResources();
        int screenBrightnessSettingMinimum = clampAbsoluteBrightness(resources2.getInteger(17694862));
        this.mScreenBrightnessDozeConfig = clampAbsoluteBrightness(resources2.getInteger(17694856));
        this.mScreenBrightnessDimConfig = clampAbsoluteBrightness(resources2.getInteger(17694855));
        this.mScreenBrightnessRangeMinimum = Math.min(screenBrightnessSettingMinimum, this.mScreenBrightnessDimConfig);
        this.mScreenBrightnessRangeMaximum = clampAbsoluteBrightness(resources2.getInteger(17694861));
        this.mScreenBrightnessDefault = clampAbsoluteBrightness(resources2.getInteger(17694860));
        this.mScreenBrightnessForVrRangeMinimum = clampAbsoluteBrightness(resources2.getInteger(17694859));
        this.mScreenBrightnessForVrRangeMaximum = clampAbsoluteBrightness(resources2.getInteger(17694858));
        this.mScreenBrightnessForVrDefault = clampAbsoluteBrightness(resources2.getInteger(17694857));
        this.mUseSoftwareAutoBrightnessConfig = resources2.getBoolean(17956895);
        this.mAllowAutoBrightnessWhileDozingConfig = resources2.getBoolean(17956872);
        this.mBrightnessRampRateFast = resources2.getInteger(17694745);
        this.mBrightnessRampRateSlow = resources2.getInteger(17694746);
        this.mSkipScreenOnBrightnessRamp = resources2.getBoolean(17957024);
        if (this.mUseSoftwareAutoBrightnessConfig) {
            int initialLightSensorRate;
            float dozeScaleFactor = resources2.getFraction(18022403, 1, 1);
            int[] brightLevels = resources2.getIntArray(17236004);
            int[] darkLevels = resources2.getIntArray(17236005);
            int[] luxHysteresisLevels = resources2.getIntArray(17236006);
            HysteresisLevels hysteresisLevels = new HysteresisLevels(brightLevels, darkLevels, luxHysteresisLevels);
            long brighteningLightDebounce = (long) resources2.getInteger(17694732);
            long darkeningLightDebounce = (long) resources2.getInteger(17694733);
            boolean autoBrightnessResetAmbientLuxAfterWarmUp = resources2.getBoolean(17956891);
            int lightSensorWarmUpTimeConfig = resources2.getInteger(17694798);
            int lightSensorRate = resources2.getInteger(17694735);
            int initialLightSensorRate2 = resources2.getInteger(17694734);
            long darkeningLightDebounce2 = darkeningLightDebounce;
            if (initialLightSensorRate2 == -1) {
                initialLightSensorRate = lightSensorRate;
            } else {
                if (initialLightSensorRate2 > lightSensorRate) {
                    String str2 = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Expected config_autoBrightnessInitialLightSensorRate (");
                    stringBuilder.append(initialLightSensorRate2);
                    stringBuilder.append(") to be less than or equal to config_autoBrightnessLightSensorRate (");
                    stringBuilder.append(lightSensorRate);
                    stringBuilder.append(").");
                    Slog.w(str2, stringBuilder.toString());
                }
                initialLightSensorRate = initialLightSensorRate2;
            }
            this.mBrightnessMapper = BrightnessMappingStrategy.create(resources2);
            long brighteningLightDebounce2;
            int[] iArr;
            int[] iArr2;
            int[] iArr3;
            int i;
            if (this.mBrightnessMapper != null) {
                IHwAutomaticBrightnessController iadm = HwServiceFactory.getHuaweiAutomaticBrightnessController();
                if (iadm != null) {
                    Resources resources3 = resources2;
                    resources = resources3;
                    displayPowerController = this;
                    displayPowerController.mAutomaticBrightnessController = iadm.getInstance(this, handler.getLooper(), sensorManager2, this.mBrightnessMapper, lightSensorWarmUpTimeConfig, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum, dozeScaleFactor, lightSensorRate, initialLightSensorRate, brighteningLightDebounce, darkeningLightDebounce2, autoBrightnessResetAmbientLuxAfterWarmUp, hysteresisLevels, this.mContext);
                } else {
                    int lightSensorRate2 = lightSensorRate;
                    brighteningLightDebounce2 = brighteningLightDebounce;
                    iArr = luxHysteresisLevels;
                    iArr2 = darkLevels;
                    iArr3 = brightLevels;
                    i = screenBrightnessSettingMinimum;
                    resources = resources2;
                    displayPowerController = this;
                    displayPowerController.mAutomaticBrightnessController = new AutomaticBrightnessController(displayPowerController, handler.getLooper(), sensorManager, displayPowerController.mBrightnessMapper, lightSensorWarmUpTimeConfig, displayPowerController.mScreenBrightnessRangeMinimum, displayPowerController.mScreenBrightnessRangeMaximum, dozeScaleFactor, lightSensorRate2, initialLightSensorRate, brighteningLightDebounce2, darkeningLightDebounce2, autoBrightnessResetAmbientLuxAfterWarmUp, hysteresisLevels);
                }
            } else {
                brighteningLightDebounce2 = brighteningLightDebounce;
                iArr = luxHysteresisLevels;
                iArr2 = darkLevels;
                iArr3 = brightLevels;
                i = screenBrightnessSettingMinimum;
                resources = resources2;
                displayPowerController = this;
                displayPowerController.mUseSoftwareAutoBrightnessConfig = false;
            }
            displayPowerController.fpDataCollector = FingerprintUnlockDataCollector.getInstance();
        } else {
            resources = resources2;
            displayPowerController = this;
        }
        displayPowerController.mColorFadeEnabled = ActivityManager.isLowRamDeviceStatic() ^ 1;
        Resources resources4 = resources;
        displayPowerController.mColorFadeFadesConfig = resources4.getBoolean(17956888);
        displayPowerController.mDisplayBlanksAfterDozeConfig = resources4.getBoolean(17956932);
        displayPowerController.mBrightnessBucketsInDozeConfig = resources4.getBoolean(17956933);
        displayPowerController.mProximitySensor = displayPowerController.mSensorManager.getDefaultSensor(8);
        if (displayPowerController.mProximitySensor != null) {
            displayPowerController.mProximityThreshold = Math.min(displayPowerController.mProximitySensor.getMaximumRange(), 5.0f);
        }
        displayPowerController.mDisplayEngineInterface = HwServiceFactory.getDisplayEngineInterface();
        if (displayPowerController.mDisplayEngineInterface != null) {
            displayPowerController.mDisplayEngineInterface.initialize();
            displayPowerController.mUsingSRE = displayPowerController.mDisplayEngineInterface.getSupported("FEATURE_SRE");
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DisplayEngineInterface getSupported SRE:");
            stringBuilder2.append(displayPowerController.mUsingSRE);
            Slog.i(str, stringBuilder2.toString());
        }
        displayPowerController.mCurrentScreenBrightnessSetting = getScreenBrightnessSetting();
        displayPowerController.mScreenBrightnessForVr = getScreenBrightnessForVrSetting();
        displayPowerController.mAutoBrightnessAdjustment = getAutoBrightnessAdjustmentSetting();
        displayPowerController.mTemporaryScreenBrightness = -1;
        displayPowerController.mPendingScreenBrightnessSetting = -1;
        displayPowerController.mTemporaryAutoBrightnessAdjustment = Float.NaN;
        displayPowerController.mPendingAutoBrightnessAdjustment = Float.NaN;
        int smartBackLightConfig = SystemProperties.getInt("ro.config.hw_smart_backlight", 1);
        StringBuilder stringBuilder3;
        if (displayPowerController.mUsingSRE || smartBackLightConfig == 1) {
            if (displayPowerController.mUsingSRE) {
                Slog.i(TAG, "Use SRE instead of SBL");
            } else {
                displayPowerController.mSmartBackLightSupported = true;
                if (DEBUG) {
                    Slog.i(TAG, "get ro.config.hw_smart_backlight = 1");
                }
            }
            int smartBackLightSetting = System.getInt(displayPowerController.mContext.getContentResolver(), "smart_backlight_enable", -1);
            if (smartBackLightSetting == -1) {
                if (DEBUG) {
                    Slog.i(TAG, "get Settings.System.SMART_BACKLIGHT failed, set default value to 1");
                }
                System.putInt(displayPowerController.mContext.getContentResolver(), "smart_backlight_enable", 1);
            } else if (DEBUG) {
                str = TAG;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("get Settings.System.SMART_BACKLIGHT = ");
                stringBuilder3.append(smartBackLightSetting);
                Slog.i(str, stringBuilder3.toString());
            }
        } else if (DEBUG) {
            str = TAG;
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("get ro.config.hw_smart_backlight = ");
            stringBuilder3.append(smartBackLightConfig);
            stringBuilder3.append(", mUsingSRE = false, don't support sbl or sre");
            Slog.i(str, stringBuilder3.toString());
        }
        IHwNormalizedManualBrightnessController iadm2 = HwServiceFactory.getHuaweiManualBrightnessController();
        if (iadm2 != null) {
            displayPowerController.mManualBrightnessController = iadm2.getInstance(displayPowerController, displayPowerController.mContext, displayPowerController.mSensorManager);
            if (DEBUG) {
                Slog.i(TAG, "HBM ManualBrightnessController initialized");
            }
        } else {
            displayPowerController.mManualBrightnessController = new ManualBrightnessController(displayPowerController);
        }
        if (displayPowerController.mUseSensorHubLABC) {
            displayPowerController.mLABCSensor = displayPowerController.mSensorManager.getDefaultSensor(65543);
            if (displayPowerController.mLABCSensor == null) {
                Slog.e(TAG, "[LABC] Get LABC Sensor failed !! ");
            }
        } else if (displayPowerController.mSmartBackLightSupported) {
            displayPowerController.mHwSmartBackLightController = HwServiceFactory.getHwSmartBackLightController();
            if (displayPowerController.mHwSmartBackLightController != null) {
                displayPowerController.mUsingHwSmartBackLightController = displayPowerController.mHwSmartBackLightController.checkIfUsingHwSBL();
                displayPowerController.mHwSmartBackLightController.StartHwSmartBackLightController(displayPowerController.mContext, displayPowerController.mLights, displayPowerController.mSensorManager);
            }
        }
        displayPowerController.mDisplayEffectMonitor = HwServiceFactory.getDisplayEffectMonitor(displayPowerController.mContext);
        if (displayPowerController.mDisplayEffectMonitor == null) {
            Slog.e(TAG, "getDisplayEffectMonitor failed!");
        }
        HwServiceFactory.loadHwBrightnessProcessors(displayPowerController.mAutomaticBrightnessController, displayPowerController.mManualBrightnessController);
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

    public boolean requestPowerState(DisplayPowerRequest request, boolean waitForNegativeProximity) {
        boolean z;
        if (DEBUG && DEBUG_Controller) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("requestPowerState: ");
            stringBuilder.append(request);
            stringBuilder.append(", waitForNegativeProximity=");
            stringBuilder.append(waitForNegativeProximity);
            Slog.d(str, stringBuilder.toString());
        }
        synchronized (this.mLock) {
            boolean changed = false;
            if (waitForNegativeProximity) {
                try {
                    if (!this.mPendingWaitForNegativeProximityLocked) {
                        this.mPendingWaitForNegativeProximityLocked = true;
                        changed = true;
                    }
                } finally {
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
            if (changed && !this.mPendingRequestChangedLocked) {
                this.mPendingRequestChangedLocked = true;
                sendUpdatePowerStateLocked();
            }
            z = this.mDisplayReadyLocked;
        }
        return z;
    }

    public BrightnessConfiguration getDefaultBrightnessConfiguration() {
        return this.mAutomaticBrightnessController.getDefaultConfig();
    }

    private void sendUpdatePowerState() {
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
        this.mPowerState = new DisplayPowerState(this.mContext, this.mBlanker, this.mColorFadeEnabled ? new ColorFade(0) : null);
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
        float brightness = convertToNits(this.mPowerState.getScreenBrightness());
        if (brightness >= 0.0f) {
            this.mBrightnessTracker.start(brightness);
        }
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("screen_brightness"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("screen_brightness_for_vr"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("screen_auto_brightness_adj"), false, this.mSettingsObserver, -1);
    }

    private int getBrightness(boolean autoBrightnessAdjustmentChanged) {
        if (autoBrightnessAdjustmentChanged) {
            return this.mPendingBacklight;
        }
        return 0;
    }

    /* JADX WARNING: Missing block: B:70:0x014b, code skipped:
            if (r10 == false) goto L_0x0150;
     */
    /* JADX WARNING: Missing block: B:71:0x014d, code skipped:
            initialize();
     */
    /* JADX WARNING: Missing block: B:72:0x0150, code skipped:
            r1 = -1;
            r2 = false;
            r3 = r7.mPowerRequest.policy;
     */
    /* JADX WARNING: Missing block: B:73:0x0157, code skipped:
            if (r3 == 4) goto L_0x0176;
     */
    /* JADX WARNING: Missing block: B:74:0x0159, code skipped:
            switch(r3) {
                case 0: goto L_0x0173;
                case 1: goto L_0x015e;
                default: goto L_0x015c;
            };
     */
    /* JADX WARNING: Missing block: B:75:0x015c, code skipped:
            r3 = 2;
     */
    /* JADX WARNING: Missing block: B:77:0x0162, code skipped:
            if (r7.mPowerRequest.dozeScreenState == 0) goto L_0x0169;
     */
    /* JADX WARNING: Missing block: B:78:0x0164, code skipped:
            r3 = r7.mPowerRequest.dozeScreenState;
     */
    /* JADX WARNING: Missing block: B:79:0x0169, code skipped:
            r3 = 3;
     */
    /* JADX WARNING: Missing block: B:81:0x016c, code skipped:
            if (r7.mAllowAutoBrightnessWhileDozingConfig != false) goto L_0x0178;
     */
    /* JADX WARNING: Missing block: B:82:0x016e, code skipped:
            r1 = r7.mPowerRequest.dozeScreenBrightness;
     */
    /* JADX WARNING: Missing block: B:83:0x0173, code skipped:
            r3 = 1;
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:84:0x0176, code skipped:
            r3 = 5;
     */
    /* JADX WARNING: Missing block: B:86:0x017d, code skipped:
            if (r7.mProximitySensor == null) goto L_0x01f6;
     */
    /* JADX WARNING: Missing block: B:88:0x0183, code skipped:
            if (r7.mPowerRequest.useProximitySensor == false) goto L_0x0198;
     */
    /* JADX WARNING: Missing block: B:89:0x0185, code skipped:
            if (r3 == 1) goto L_0x0198;
     */
    /* JADX WARNING: Missing block: B:90:0x0187, code skipped:
            setProximitySensorEnabled(true);
     */
    /* JADX WARNING: Missing block: B:91:0x018c, code skipped:
            if (r7.mScreenOffBecauseOfProximity != false) goto L_0x01cd;
     */
    /* JADX WARNING: Missing block: B:93:0x0190, code skipped:
            if (r7.mProximity != 1) goto L_0x01cd;
     */
    /* JADX WARNING: Missing block: B:94:0x0192, code skipped:
            r7.mScreenOffBecauseOfProximity = true;
            sendOnProximityPositiveWithWakelock();
     */
    /* JADX WARNING: Missing block: B:96:0x019a, code skipped:
            if (r7.mWaitingForNegativeProximity == false) goto L_0x01aa;
     */
    /* JADX WARNING: Missing block: B:98:0x019e, code skipped:
            if (r7.mScreenOffBecauseOfProximity == false) goto L_0x01aa;
     */
    /* JADX WARNING: Missing block: B:100:0x01a2, code skipped:
            if (r7.mProximity != 1) goto L_0x01aa;
     */
    /* JADX WARNING: Missing block: B:101:0x01a4, code skipped:
            if (r3 == 1) goto L_0x01aa;
     */
    /* JADX WARNING: Missing block: B:102:0x01a6, code skipped:
            setProximitySensorEnabled(true);
     */
    /* JADX WARNING: Missing block: B:104:0x01ac, code skipped:
            if (r7.mWaitingForNegativeProximity != false) goto L_0x01c2;
     */
    /* JADX WARNING: Missing block: B:106:0x01b0, code skipped:
            if (r7.mScreenOffBecauseOfProximity == false) goto L_0x01c2;
     */
    /* JADX WARNING: Missing block: B:108:0x01b4, code skipped:
            if (r7.mProximity == -1) goto L_0x01c2;
     */
    /* JADX WARNING: Missing block: B:109:0x01b6, code skipped:
            if (r3 != 1) goto L_0x01c2;
     */
    /* JADX WARNING: Missing block: B:111:0x01bc, code skipped:
            if (r7.mPowerRequest.useProximitySensor == false) goto L_0x01c2;
     */
    /* JADX WARNING: Missing block: B:112:0x01be, code skipped:
            setProximitySensorEnabled(true);
     */
    /* JADX WARNING: Missing block: B:114:0x01c6, code skipped:
            if (r7.mPowerRequest.useProximitySensor != false) goto L_0x01cb;
     */
    /* JADX WARNING: Missing block: B:115:0x01c8, code skipped:
            setProximitySensorEnabled(false);
     */
    /* JADX WARNING: Missing block: B:116:0x01cb, code skipped:
            r7.mWaitingForNegativeProximity = false;
     */
    /* JADX WARNING: Missing block: B:118:0x01cf, code skipped:
            if (r7.mScreenOffBecauseOfProximity != false) goto L_0x01da;
     */
    /* JADX WARNING: Missing block: B:120:0x01d3, code skipped:
            if (r7.mProximity != 1) goto L_0x01da;
     */
    /* JADX WARNING: Missing block: B:121:0x01d5, code skipped:
            r7.mScreenOffBecauseOfProximity = true;
            sendOnProximityPositiveWithWakelock();
     */
    /* JADX WARNING: Missing block: B:123:0x01dc, code skipped:
            if (r7.mScreenOffBecauseOfProximity == false) goto L_0x01f8;
     */
    /* JADX WARNING: Missing block: B:125:0x01e0, code skipped:
            if (r7.mProximity == 1) goto L_0x01f8;
     */
    /* JADX WARNING: Missing block: B:126:0x01e2, code skipped:
            r7.mScreenOffBecauseOfProximity = false;
     */
    /* JADX WARNING: Missing block: B:127:0x01e8, code skipped:
            if (r7.mPowerRequest.useProximitySensor == false) goto L_0x01f2;
     */
    /* JADX WARNING: Missing block: B:129:0x01ee, code skipped:
            if (r7.mPowerRequest.useProximitySensorbyPhone == false) goto L_0x01f2;
     */
    /* JADX WARNING: Missing block: B:130:0x01f0, code skipped:
            r7.mScreenOnBecauseOfPhoneProximity = true;
     */
    /* JADX WARNING: Missing block: B:131:0x01f2, code skipped:
            sendOnProximityNegativeWithWakelock();
     */
    /* JADX WARNING: Missing block: B:132:0x01f6, code skipped:
            r7.mWaitingForNegativeProximity = false;
     */
    /* JADX WARNING: Missing block: B:134:0x01fa, code skipped:
            if (r7.mScreenOffBecauseOfProximity == false) goto L_0x0203;
     */
    /* JADX WARNING: Missing block: B:136:0x0200, code skipped:
            if (r7.mPowerRequest.useProximitySensorbyPhone != false) goto L_0x0203;
     */
    /* JADX WARNING: Missing block: B:137:0x0202, code skipped:
            r3 = 1;
     */
    /* JADX WARNING: Missing block: B:138:0x0203, code skipped:
            sre_init(r3);
            hbm_init(r3);
            sendScreenStateToDE(r3);
     */
    /* JADX WARNING: Missing block: B:139:0x020c, code skipped:
            if (r2 == false) goto L_0x0227;
     */
    /* JADX WARNING: Missing block: B:141:0x0210, code skipped:
            if (r7.mLastWaitBrightnessMode == false) goto L_0x0220;
     */
    /* JADX WARNING: Missing block: B:143:0x0216, code skipped:
            if (r7.mPowerRequest.brightnessWaitMode != false) goto L_0x0220;
     */
    /* JADX WARNING: Missing block: B:145:0x021c, code skipped:
            if (r7.mPowerRequest.brightnessWaitRet != false) goto L_0x0220;
     */
    /* JADX WARNING: Missing block: B:146:0x021e, code skipped:
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:147:0x0220, code skipped:
            r5 = false;
     */
    /* JADX WARNING: Missing block: B:148:0x0221, code skipped:
            if (r5 != false) goto L_0x0225;
     */
    /* JADX WARNING: Missing block: B:149:0x0223, code skipped:
            r12 = 1;
     */
    /* JADX WARNING: Missing block: B:150:0x0225, code skipped:
            r12 = 0;
     */
    /* JADX WARNING: Missing block: B:151:0x0226, code skipped:
            r2 = r2 & r12;
     */
    /* JADX WARNING: Missing block: B:152:0x0227, code skipped:
            r12 = r2;
            r13 = r7.mPowerState.getScreenState();
            animateScreenStateChange(r3, r12);
            r14 = r7.mPowerState.getScreenState();
     */
    /* JADX WARNING: Missing block: B:153:0x0237, code skipped:
            if (r14 != 1) goto L_0x023e;
     */
    /* JADX WARNING: Missing block: B:154:0x0239, code skipped:
            r1 = 0;
            r7.mWakeupFromSleep = true;
            r7.mProximityPositive = false;
     */
    /* JADX WARNING: Missing block: B:156:0x023f, code skipped:
            if (r14 != 5) goto L_0x0243;
     */
    /* JADX WARNING: Missing block: B:157:0x0241, code skipped:
            r1 = r7.mScreenBrightnessForVr;
     */
    /* JADX WARNING: Missing block: B:158:0x0243, code skipped:
            if (r1 >= 0) goto L_0x0254;
     */
    /* JADX WARNING: Missing block: B:160:0x0249, code skipped:
            if (r7.mPowerRequest.screenBrightnessOverride <= 0) goto L_0x0254;
     */
    /* JADX WARNING: Missing block: B:161:0x024b, code skipped:
            r1 = r7.mPowerRequest.screenBrightnessOverride;
            r7.mTemporaryScreenBrightness = -1;
            r7.mAppliedScreenBrightnessOverride = true;
     */
    /* JADX WARNING: Missing block: B:162:0x0254, code skipped:
            r7.mAppliedScreenBrightnessOverride = false;
     */
    /* JADX WARNING: Missing block: B:164:0x0258, code skipped:
            if (r7.mAllowAutoBrightnessWhileDozingConfig == false) goto L_0x0262;
     */
    /* JADX WARNING: Missing block: B:166:0x025e, code skipped:
            if (android.view.Display.isDozeState(r14) == false) goto L_0x0262;
     */
    /* JADX WARNING: Missing block: B:167:0x0260, code skipped:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:168:0x0262, code skipped:
            r2 = false;
     */
    /* JADX WARNING: Missing block: B:169:0x0263, code skipped:
            r16 = r2;
     */
    /* JADX WARNING: Missing block: B:170:0x0269, code skipped:
            if (r7.mPowerRequest.useAutoBrightness == false) goto L_0x0277;
     */
    /* JADX WARNING: Missing block: B:171:0x026b, code skipped:
            if (r14 == 2) goto L_0x026f;
     */
    /* JADX WARNING: Missing block: B:172:0x026d, code skipped:
            if (r16 == false) goto L_0x0277;
     */
    /* JADX WARNING: Missing block: B:173:0x026f, code skipped:
            if (r1 >= 0) goto L_0x0277;
     */
    /* JADX WARNING: Missing block: B:175:0x0273, code skipped:
            if (r7.mAutomaticBrightnessController == null) goto L_0x0277;
     */
    /* JADX WARNING: Missing block: B:176:0x0275, code skipped:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:177:0x0277, code skipped:
            r2 = false;
     */
    /* JADX WARNING: Missing block: B:178:0x0278, code skipped:
            r5 = r2;
            r25 = updateUserSetScreenBrightness();
     */
    /* JADX WARNING: Missing block: B:179:0x027d, code skipped:
            if (r25 != false) goto L_0x0281;
     */
    /* JADX WARNING: Missing block: B:180:0x027f, code skipped:
            if (r5 == false) goto L_0x0283;
     */
    /* JADX WARNING: Missing block: B:181:0x0281, code skipped:
            r7.mTemporaryScreenBrightness = -1;
     */
    /* JADX WARNING: Missing block: B:183:0x0285, code skipped:
            if (r7.mTemporaryScreenBrightness <= 0) goto L_0x028c;
     */
    /* JADX WARNING: Missing block: B:184:0x0287, code skipped:
            r1 = r7.mTemporaryScreenBrightness;
            r7.mAppliedTemporaryBrightness = true;
     */
    /* JADX WARNING: Missing block: B:185:0x028c, code skipped:
            r7.mAppliedTemporaryBrightness = false;
     */
    /* JADX WARNING: Missing block: B:186:0x028e, code skipped:
            r26 = updateAutoBrightnessAdjustment();
     */
    /* JADX WARNING: Missing block: B:187:0x0292, code skipped:
            if (r26 == false) goto L_0x0298;
     */
    /* JADX WARNING: Missing block: B:188:0x0294, code skipped:
            r7.mTemporaryAutoBrightnessAdjustment = Float.NaN;
     */
    /* JADX WARNING: Missing block: B:190:0x029e, code skipped:
            if (java.lang.Float.isNaN(r7.mTemporaryAutoBrightnessAdjustment) != false) goto L_0x02a7;
     */
    /* JADX WARNING: Missing block: B:191:0x02a0, code skipped:
            r2 = r7.mTemporaryAutoBrightnessAdjustment;
            r7.mAppliedTemporaryAutoBrightnessAdjustment = true;
     */
    /* JADX WARNING: Missing block: B:192:0x02a4, code skipped:
            r27 = r2;
     */
    /* JADX WARNING: Missing block: B:193:0x02a7, code skipped:
            r2 = r7.mAutoBrightnessAdjustment;
            r7.mAppliedTemporaryAutoBrightnessAdjustment = false;
     */
    /* JADX WARNING: Missing block: B:195:0x02b0, code skipped:
            if (r7.mPowerRequest.boostScreenBrightness == false) goto L_0x02b9;
     */
    /* JADX WARNING: Missing block: B:196:0x02b2, code skipped:
            if (r1 == 0) goto L_0x02b9;
     */
    /* JADX WARNING: Missing block: B:197:0x02b4, code skipped:
            r1 = 255;
            r7.mAppliedBrightnessBoost = true;
     */
    /* JADX WARNING: Missing block: B:198:0x02b9, code skipped:
            r7.mAppliedBrightnessBoost = false;
     */
    /* JADX WARNING: Missing block: B:199:0x02bb, code skipped:
            r28 = r1;
     */
    /* JADX WARNING: Missing block: B:200:0x02bd, code skipped:
            if (r28 >= 0) goto L_0x02c5;
     */
    /* JADX WARNING: Missing block: B:201:0x02bf, code skipped:
            if (r26 != false) goto L_0x02c3;
     */
    /* JADX WARNING: Missing block: B:202:0x02c1, code skipped:
            if (r25 == false) goto L_0x02c5;
     */
    /* JADX WARNING: Missing block: B:203:0x02c3, code skipped:
            r1 = true;
     */
    /* JADX WARNING: Missing block: B:204:0x02c5, code skipped:
            r1 = false;
     */
    /* JADX WARNING: Missing block: B:205:0x02c6, code skipped:
            r6 = r1;
            r1 = false;
     */
    /* JADX WARNING: Missing block: B:206:0x02ca, code skipped:
            if (r7.mAutomaticBrightnessController == null) goto L_0x02fb;
     */
    /* JADX WARNING: Missing block: B:207:0x02cc, code skipped:
            r7.mAutomaticBrightnessController.updatePowerPolicy(r7.mPowerRequest.policy);
            r1 = r7.mAutomaticBrightnessController.hasUserDataPoints();
            r7.mAutomaticBrightnessController.configure(r5, r7.mBrightnessConfiguration, ((float) r7.mLastUserSetScreenBrightness) / 255.0f, r25, r27, r26, r7.mPowerRequest.policy);
     */
    /* JADX WARNING: Missing block: B:208:0x02fb, code skipped:
            r15 = r1;
            r7.mAutomaticBrightnessController.updateCurrentUserId(r7.mPowerRequest.userId);
            r7.mManualBrightnessController.updateCurrentUserId(r7.mPowerRequest.userId);
            r4 = android.provider.Settings.System.getFloatForUser(r7.mContext.getContentResolver(), "screen_auto_brightness_adj", 0.0f, r7.mPowerRequest.userId);
            r1 = NEED_NEW_BRIGHTNESS_PROCESS;
     */
    /* JADX WARNING: Missing block: B:209:0x0323, code skipped:
            if (r7.mUseSensorHubLABC != false) goto L_0x0413;
     */
    /* JADX WARNING: Missing block: B:211:0x0327, code skipped:
            if (DEBUG == false) goto L_0x0356;
     */
    /* JADX WARNING: Missing block: B:213:0x032b, code skipped:
            if (r7.mAutoBrightnessEnabled == r5) goto L_0x0356;
     */
    /* JADX WARNING: Missing block: B:214:0x032d, code skipped:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("mode change : autoBrightnessEnabled=");
            r2.append(r5);
            r2.append(",adjustment=");
            r2.append(r4);
            r2.append(",state=");
            r2.append(r14);
            android.util.Slog.d(r1, r2.toString());
            r7.mAutoBrightnessEnabled = r5;
     */
    /* JADX WARNING: Missing block: B:216:0x0358, code skipped:
            if (NEED_NEW_BRIGHTNESS_PROCESS == false) goto L_0x044c;
     */
    /* JADX WARNING: Missing block: B:218:0x035e, code skipped:
            if (r7.mPowerRequest.screenAutoBrightness <= 0) goto L_0x036e;
     */
    /* JADX WARNING: Missing block: B:219:0x0360, code skipped:
            r7.mAutomaticBrightnessController.updateIntervenedAutoBrightness(r7.mPowerRequest.screenAutoBrightness);
            r7.mScreenBrightnessRampAnimator.mAutoBrightnessIntervened = true;
     */
    /* JADX WARNING: Missing block: B:220:0x036e, code skipped:
            r7.mScreenBrightnessRampAnimator.mAutoBrightnessIntervened = false;
     */
    /* JADX WARNING: Missing block: B:221:0x0372, code skipped:
            r7.mScreenBrightnessRampAnimator.mIsAutoBrightnessMode = r5;
     */
    /* JADX WARNING: Missing block: B:222:0x0378, code skipped:
            if (r7.mBrightnessModeChanged == false) goto L_0x0380;
     */
    /* JADX WARNING: Missing block: B:223:0x037a, code skipped:
            r7.mScreenBrightnessRampAnimator.mIsFirstValidAutoBrightness = r5;
            r7.mBrightnessModeChanged = false;
     */
    /* JADX WARNING: Missing block: B:224:0x0380, code skipped:
            r1 = r7.mScreenBrightnessRampAnimator;
            r2 = r7.mAutomaticBrightnessController.getUpdateAutoBrightnessCount();
     */
    /* JADX WARNING: Missing block: B:225:0x038c, code skipped:
            if (r7.mPowerRequest.screenAutoBrightness <= 0) goto L_0x0390;
     */
    /* JADX WARNING: Missing block: B:226:0x038e, code skipped:
            r3 = true;
     */
    /* JADX WARNING: Missing block: B:227:0x0390, code skipped:
            r3 = false;
     */
    /* JADX WARNING: Missing block: B:228:0x0391, code skipped:
            r1.updateBrightnessRampPara(r5, r2, r3, r7.mPowerRequest.policy);
            r7.mfastAnimtionFlag = r7.mAutomaticBrightnessController.getPowerStatus();
            r7.mScreenBrightnessRampAnimator.updateFastAnimationFlag(r7.mfastAnimtionFlag);
            r7.mCoverModeAnimationFast = r7.mAutomaticBrightnessController.getCoverModeFastResponseFlag();
            r7.mScreenBrightnessRampAnimator.updateCoverModeFastAnimationFlag(r7.mCoverModeAnimationFast);
            r7.mScreenBrightnessRampAnimator.updateCameraModeChangeAnimationEnable(r7.mAutomaticBrightnessController.getCameraModeChangeAnimationEnable());
            r7.mScreenBrightnessRampAnimator.updateGameModeChangeAnimationEnable(r7.mAutomaticBrightnessController.getAnimationGameChangeEnable());
     */
    /* JADX WARNING: Missing block: B:229:0x03d2, code skipped:
            if (r7.mAutomaticBrightnessController.getReadingModeBrightnessLineEnable() == false) goto L_0x03df;
     */
    /* JADX WARNING: Missing block: B:230:0x03d4, code skipped:
            r7.mScreenBrightnessRampAnimator.updateReadingModeChangeAnimationEnable(r7.mAutomaticBrightnessController.getReadingModeChangeAnimationEnable());
     */
    /* JADX WARNING: Missing block: B:231:0x03df, code skipped:
            r7.mScreenBrightnessRampAnimator.setBrightnessAnimationTime(r7.mAnimationEnabled, r7.mMillisecond);
            r7.mScreenBrightnessRampAnimator.updateScreenLockedAnimationEnable(r7.mKeyguardIsLocked);
            r7.mOutdoorAnimationFlag = r7.mAutomaticBrightnessController.getOutdoorAnimationFlag();
            r7.mScreenBrightnessRampAnimator.updateOutdoorAnimationFlag(r7.mOutdoorAnimationFlag);
            updatemManualModeAnimationEnable();
            updateManualPowerSavingAnimationEnable();
            updateManualThermalModeAnimationEnable();
            updateBrightnessModeAnimationEnable();
            updateDarkAdaptDimmingEnable();
            r7.mBackLight.updateBrightnessAdjustMode(r5);
     */
    /* JADX WARNING: Missing block: B:233:0x0415, code skipped:
            if (r7.mLABCSensorEnabled == false) goto L_0x044c;
     */
    /* JADX WARNING: Missing block: B:234:0x0417, code skipped:
            if (r5 == false) goto L_0x0443;
     */
    /* JADX WARNING: Missing block: B:235:0x0419, code skipped:
            if (r26 == false) goto L_0x0443;
     */
    /* JADX WARNING: Missing block: B:237:0x041d, code skipped:
            if (DEBUG == false) goto L_0x0437;
     */
    /* JADX WARNING: Missing block: B:238:0x041f, code skipped:
            r0 = TAG;
            r1 = new java.lang.StringBuilder();
            r1.append("[LABC]  A = ");
            r1.append(r7.mSetAutoBackLight);
            android.util.Slog.d(r0, r1.toString());
     */
    /* JADX WARNING: Missing block: B:239:0x0437, code skipped:
            r7.mAutoCustomBackLight.sendCustomBackLight(r7.mSetAutoBackLight);
            r7.mAutoBrightnessAdjustmentChanged = true;
            r7.mLastStatus = true;
     */
    /* JADX WARNING: Missing block: B:240:0x0442, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:241:0x0443, code skipped:
            if (r5 != false) goto L_0x044c;
     */
    /* JADX WARNING: Missing block: B:243:0x0447, code skipped:
            if (r7.mLastStatus != true) goto L_0x044c;
     */
    /* JADX WARNING: Missing block: B:244:0x0449, code skipped:
            r7.mLastStatus = false;
     */
    /* JADX WARNING: Missing block: B:245:0x044c, code skipped:
            r18 = r4;
            r29 = r5;
            r8 = r6;
     */
    /* JADX WARNING: Missing block: B:246:0x046b, code skipped:
            if (waitScreenBrightness(r14, r7.mPowerRequest.brightnessWaitMode, r7.mLastWaitBrightnessMode, r7.mPowerRequest.brightnessWaitRet, r7.mPowerRequest.skipWaitKeyguardDismiss) == false) goto L_0x0474;
     */
    /* JADX WARNING: Missing block: B:247:0x046d, code skipped:
            r28 = 0;
            r7.mWindowManagerPolicy.setInterceptInputForWaitBrightness(true);
     */
    /* JADX WARNING: Missing block: B:249:0x0476, code skipped:
            if (r7.mGlobalAlpmState != 0) goto L_0x047a;
     */
    /* JADX WARNING: Missing block: B:250:0x0478, code skipped:
            r28 = 0;
     */
    /* JADX WARNING: Missing block: B:252:0x047e, code skipped:
            if (r7.mPowerRequest.boostScreenBrightness == false) goto L_0x0484;
     */
    /* JADX WARNING: Missing block: B:253:0x0480, code skipped:
            if (r28 == 0) goto L_0x0484;
     */
    /* JADX WARNING: Missing block: B:254:0x0482, code skipped:
            r28 = 255;
     */
    /* JADX WARNING: Missing block: B:255:0x0484, code skipped:
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:256:0x0485, code skipped:
            if (r28 >= 0) goto L_0x051d;
     */
    /* JADX WARNING: Missing block: B:257:0x0487, code skipped:
            r1 = r27;
            r3 = r29;
     */
    /* JADX WARNING: Missing block: B:258:0x048c, code skipped:
            if (r3 == false) goto L_0x04e4;
     */
    /* JADX WARNING: Missing block: B:260:0x0490, code skipped:
            if (r7.mUseSensorHubLABC != false) goto L_0x0499;
     */
    /* JADX WARNING: Missing block: B:261:0x0492, code skipped:
            r4 = r7.mAutomaticBrightnessController.getAutomaticScreenBrightness();
     */
    /* JADX WARNING: Missing block: B:262:0x0499, code skipped:
            r4 = getBrightness(r3);
     */
    /* JADX WARNING: Missing block: B:263:0x049e, code skipped:
            r1 = r7.mAutomaticBrightnessController.getAutomaticScreenBrightnessAdjustment();
     */
    /* JADX WARNING: Missing block: B:264:0x04a4, code skipped:
            if (r4 >= 0) goto L_0x04e6;
     */
    /* JADX WARNING: Missing block: B:266:0x04b6, code skipped:
            if ((android.os.SystemClock.uptimeMillis() - r7.mAutomaticBrightnessController.getLightSensorEnableTime()) <= 195) goto L_0x04e6;
     */
    /* JADX WARNING: Missing block: B:267:0x04b8, code skipped:
            if (r4 == -2) goto L_0x04c9;
     */
    /* JADX WARNING: Missing block: B:268:0x04ba, code skipped:
            r4 = android.provider.Settings.System.getInt(r7.mContext.getContentResolver(), "screen_brightness", 100);
     */
    /* JADX WARNING: Missing block: B:270:0x04cb, code skipped:
            if (DEBUG == false) goto L_0x04e6;
     */
    /* JADX WARNING: Missing block: B:271:0x04cd, code skipped:
            r5 = TAG;
            r6 = new java.lang.StringBuilder();
            r6.append("failed to get auto brightness so set brightness based on SCREEN_BRIGHTNESS:");
            r6.append(r4);
            android.util.Slog.d(r5, r6.toString());
     */
    /* JADX WARNING: Missing block: B:272:0x04e4, code skipped:
            r4 = r28;
     */
    /* JADX WARNING: Missing block: B:273:0x04e6, code skipped:
            if (r4 < 0) goto L_0x050a;
     */
    /* JADX WARNING: Missing block: B:274:0x04e8, code skipped:
            r2 = clampScreenBrightness(r4);
     */
    /* JADX WARNING: Missing block: B:275:0x04ee, code skipped:
            if (r7.mUseSensorHubLABC != false) goto L_0x04f8;
     */
    /* JADX WARNING: Missing block: B:277:0x04f2, code skipped:
            if (r7.mAppliedAutoBrightness == false) goto L_0x0501;
     */
    /* JADX WARNING: Missing block: B:278:0x04f4, code skipped:
            if (r26 != false) goto L_0x0501;
     */
    /* JADX WARNING: Missing block: B:279:0x04f6, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:281:0x04fa, code skipped:
            if (r7.mAppliedAutoBrightness == false) goto L_0x0501;
     */
    /* JADX WARNING: Missing block: B:283:0x04fe, code skipped:
            if (r7.mAutoBrightnessAdjustmentChanged != false) goto L_0x0501;
     */
    /* JADX WARNING: Missing block: B:284:0x0500, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:285:0x0501, code skipped:
            putScreenBrightnessSetting(r2);
            r7.mAppliedAutoBrightness = true;
            r28 = r2;
     */
    /* JADX WARNING: Missing block: B:286:0x050a, code skipped:
            if (r4 != -2) goto L_0x050e;
     */
    /* JADX WARNING: Missing block: B:287:0x050c, code skipped:
            r2 = true;
     */
    /* JADX WARNING: Missing block: B:288:0x050e, code skipped:
            r2 = false;
     */
    /* JADX WARNING: Missing block: B:289:0x050f, code skipped:
            r7.mAppliedAutoBrightness = r2;
            r28 = r4;
     */
    /* JADX WARNING: Missing block: B:291:0x0515, code skipped:
            if (r27 == r1) goto L_0x051a;
     */
    /* JADX WARNING: Missing block: B:292:0x0517, code skipped:
            putAutoBrightnessAdjustmentSetting(r1);
     */
    /* JADX WARNING: Missing block: B:293:0x051a, code skipped:
            r1 = false;
     */
    /* JADX WARNING: Missing block: B:294:0x051d, code skipped:
            r3 = r29;
            r1 = false;
            r7.mAppliedAutoBrightness = false;
     */
    /* JADX WARNING: Missing block: B:295:0x0522, code skipped:
            r7.mAutoBrightnessAdjustmentChanged = r1;
     */
    /* JADX WARNING: Missing block: B:296:0x0524, code skipped:
            if (r28 >= 0) goto L_0x052f;
     */
    /* JADX WARNING: Missing block: B:298:0x052a, code skipped:
            if (android.view.Display.isDozeState(r14) == false) goto L_0x052f;
     */
    /* JADX WARNING: Missing block: B:299:0x052c, code skipped:
            r1 = r7.mScreenBrightnessDozeConfig;
     */
    /* JADX WARNING: Missing block: B:300:0x052f, code skipped:
            r1 = r28;
     */
    /* JADX WARNING: Missing block: B:301:0x0531, code skipped:
            if (r1 >= 0) goto L_0x054a;
     */
    /* JADX WARNING: Missing block: B:303:0x0537, code skipped:
            if (r7.mPowerRequest.useAutoBrightness != false) goto L_0x054a;
     */
    /* JADX WARNING: Missing block: B:304:0x0539, code skipped:
            r1 = clampScreenBrightness(getScreenBrightnessSetting());
     */
    /* JADX WARNING: Missing block: B:305:0x0543, code skipped:
            if (NEED_NEW_BRIGHTNESS_PROCESS == false) goto L_0x054a;
     */
    /* JADX WARNING: Missing block: B:306:0x0545, code skipped:
            r7.mAutomaticBrightnessController.updateIntervenedAutoBrightness(r1);
     */
    /* JADX WARNING: Missing block: B:308:0x054b, code skipped:
            if (r14 != 2) goto L_0x057c;
     */
    /* JADX WARNING: Missing block: B:310:0x0551, code skipped:
            if (r7.mPowerRequest.policy != 0) goto L_0x057c;
     */
    /* JADX WARNING: Missing block: B:312:0x0555, code skipped:
            if (r1 <= r7.mScreenBrightnessRangeMinimum) goto L_0x0565;
     */
    /* JADX WARNING: Missing block: B:313:0x0557, code skipped:
            r1 = java.lang.Math.max(java.lang.Math.min(r1 - 10, r7.mScreenBrightnessDimConfig), r7.mScreenBrightnessRangeMinimum);
     */
    /* JADX WARNING: Missing block: B:314:0x0565, code skipped:
            r2 = TAG;
            r4 = new java.lang.StringBuilder();
            r4.append("set brightness to DIM brightness:");
            r4.append(r1);
            android.util.Slog.i(r2, r4.toString());
     */
    /* JADX WARNING: Missing block: B:316:0x0581, code skipped:
            if (r7.mPowerRequest.policy != 2) goto L_0x05ae;
     */
    /* JADX WARNING: Missing block: B:318:0x0585, code skipped:
            if (r1 <= r7.mScreenBrightnessRangeMinimum) goto L_0x0595;
     */
    /* JADX WARNING: Missing block: B:319:0x0587, code skipped:
            r1 = java.lang.Math.max(java.lang.Math.min(r1 - 10, r7.mScreenBrightnessDimConfig), r7.mScreenBrightnessRangeMinimum);
     */
    /* JADX WARNING: Missing block: B:321:0x0599, code skipped:
            if (android.util.HwPCUtils.isPcCastModeInServer() == false) goto L_0x05a5;
     */
    /* JADX WARNING: Missing block: B:323:0x059f, code skipped:
            if (android.util.HwPCUtils.enabledInPad() != false) goto L_0x05a5;
     */
    /* JADX WARNING: Missing block: B:324:0x05a1, code skipped:
            r1 = 0;
            blackScreenOnPcMode();
     */
    /* JADX WARNING: Missing block: B:326:0x05a7, code skipped:
            if (r7.mAppliedDimming != false) goto L_0x05aa;
     */
    /* JADX WARNING: Missing block: B:327:0x05a9, code skipped:
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:328:0x05aa, code skipped:
            r7.mAppliedDimming = true;
     */
    /* JADX WARNING: Missing block: B:330:0x05b0, code skipped:
            if (r7.mAppliedDimming == false) goto L_0x05b6;
     */
    /* JADX WARNING: Missing block: B:331:0x05b2, code skipped:
            r0 = false;
            r7.mAppliedDimming = false;
     */
    /* JADX WARNING: Missing block: B:333:0x05bc, code skipped:
            if (r7.mPowerRequest.lowPowerMode == false) goto L_0x05dc;
     */
    /* JADX WARNING: Missing block: B:335:0x05c0, code skipped:
            if (r1 <= r7.mScreenBrightnessRangeMinimum) goto L_0x05d3;
     */
    /* JADX WARNING: Missing block: B:336:0x05c2, code skipped:
            r1 = java.lang.Math.max((int) (((float) r1) * java.lang.Math.min(r7.mPowerRequest.screenLowPowerBrightnessFactor, 1.0f)), r7.mScreenBrightnessRangeMinimum);
     */
    /* JADX WARNING: Missing block: B:338:0x05d5, code skipped:
            if (r7.mAppliedLowPower != false) goto L_0x05d8;
     */
    /* JADX WARNING: Missing block: B:339:0x05d7, code skipped:
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:340:0x05d8, code skipped:
            r7.mAppliedLowPower = true;
     */
    /* JADX WARNING: Missing block: B:342:0x05de, code skipped:
            if (r7.mAppliedLowPower == false) goto L_0x05e4;
     */
    /* JADX WARNING: Missing block: B:343:0x05e0, code skipped:
            r0 = false;
            r7.mAppliedLowPower = false;
     */
    /* JADX WARNING: Missing block: B:344:0x05e4, code skipped:
            r2 = r0;
     */
    /* JADX WARNING: Missing block: B:345:0x05e7, code skipped:
            if (r7.mPendingScreenOff != false) goto L_0x06eb;
     */
    /* JADX WARNING: Missing block: B:347:0x05eb, code skipped:
            if (r7.mSkipScreenOnBrightnessRamp == false) goto L_0x061b;
     */
    /* JADX WARNING: Missing block: B:349:0x05ee, code skipped:
            if (r14 != 2) goto L_0x0618;
     */
    /* JADX WARNING: Missing block: B:351:0x05f2, code skipped:
            if (r7.mSkipRampState != 0) goto L_0x05fe;
     */
    /* JADX WARNING: Missing block: B:353:0x05f6, code skipped:
            if (r7.mDozing == false) goto L_0x05fe;
     */
    /* JADX WARNING: Missing block: B:354:0x05f8, code skipped:
            r7.mInitialAutoBrightness = r1;
            r7.mSkipRampState = 1;
     */
    /* JADX WARNING: Missing block: B:356:0x0601, code skipped:
            if (r7.mSkipRampState != 1) goto L_0x060f;
     */
    /* JADX WARNING: Missing block: B:358:0x0605, code skipped:
            if (r7.mUseSoftwareAutoBrightnessConfig == false) goto L_0x060f;
     */
    /* JADX WARNING: Missing block: B:360:0x0609, code skipped:
            if (r1 == r7.mInitialAutoBrightness) goto L_0x060f;
     */
    /* JADX WARNING: Missing block: B:361:0x060b, code skipped:
            r7.mSkipRampState = 2;
     */
    /* JADX WARNING: Missing block: B:363:0x0612, code skipped:
            if (r7.mSkipRampState != 2) goto L_0x061b;
     */
    /* JADX WARNING: Missing block: B:364:0x0614, code skipped:
            r7.mSkipRampState = 0;
     */
    /* JADX WARNING: Missing block: B:365:0x0618, code skipped:
            r7.mSkipRampState = 0;
     */
    /* JADX WARNING: Missing block: B:367:0x061c, code skipped:
            if (r14 == 5) goto L_0x0623;
     */
    /* JADX WARNING: Missing block: B:368:0x061e, code skipped:
            if (r13 != 5) goto L_0x0621;
     */
    /* JADX WARNING: Missing block: B:369:0x0621, code skipped:
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:370:0x0623, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:372:0x0625, code skipped:
            if (r14 != 2) goto L_0x062d;
     */
    /* JADX WARNING: Missing block: B:374:0x0629, code skipped:
            if (r7.mSkipRampState == 0) goto L_0x062d;
     */
    /* JADX WARNING: Missing block: B:375:0x062b, code skipped:
            r5 = true;
     */
    /* JADX WARNING: Missing block: B:376:0x062d, code skipped:
            r5 = false;
     */
    /* JADX WARNING: Missing block: B:378:0x0633, code skipped:
            if (android.view.Display.isDozeState(r14) == false) goto L_0x063b;
     */
    /* JADX WARNING: Missing block: B:380:0x0637, code skipped:
            if (r7.mBrightnessBucketsInDozeConfig == false) goto L_0x063b;
     */
    /* JADX WARNING: Missing block: B:381:0x0639, code skipped:
            r6 = true;
     */
    /* JADX WARNING: Missing block: B:382:0x063b, code skipped:
            r6 = false;
     */
    /* JADX WARNING: Missing block: B:384:0x063e, code skipped:
            if (r7.mColorFadeEnabled == false) goto L_0x064c;
     */
    /* JADX WARNING: Missing block: B:386:0x0648, code skipped:
            if (r7.mPowerState.getColorFadeLevel() != 1.0f) goto L_0x064c;
     */
    /* JADX WARNING: Missing block: B:387:0x064a, code skipped:
            r4 = true;
     */
    /* JADX WARNING: Missing block: B:388:0x064c, code skipped:
            r4 = false;
     */
    /* JADX WARNING: Missing block: B:390:0x064f, code skipped:
            if (r7.mAppliedTemporaryBrightness != false) goto L_0x0658;
     */
    /* JADX WARNING: Missing block: B:392:0x0653, code skipped:
            if (r7.mAppliedTemporaryAutoBrightnessAdjustment == false) goto L_0x0656;
     */
    /* JADX WARNING: Missing block: B:393:0x0656, code skipped:
            r9 = false;
     */
    /* JADX WARNING: Missing block: B:394:0x0658, code skipped:
            r9 = true;
     */
    /* JADX WARNING: Missing block: B:395:0x0659, code skipped:
            if (r5 != false) goto L_0x06bf;
     */
    /* JADX WARNING: Missing block: B:396:0x065b, code skipped:
            if (r6 != false) goto L_0x06bf;
     */
    /* JADX WARNING: Missing block: B:397:0x065d, code skipped:
            if (r0 != false) goto L_0x06bf;
     */
    /* JADX WARNING: Missing block: B:398:0x065f, code skipped:
            if (r4 == false) goto L_0x06bf;
     */
    /* JADX WARNING: Missing block: B:399:0x0661, code skipped:
            if (r9 != false) goto L_0x06bf;
     */
    /* JADX WARNING: Missing block: B:400:0x0663, code skipped:
            r30 = r0;
     */
    /* JADX WARNING: Missing block: B:401:0x0667, code skipped:
            if (r7.mBrightnessBucketsInDozeConfig == false) goto L_0x066a;
     */
    /* JADX WARNING: Missing block: B:403:0x066b, code skipped:
            if (r14 != 2) goto L_0x06a5;
     */
    /* JADX WARNING: Missing block: B:405:0x066f, code skipped:
            if (r7.mImmeBright != false) goto L_0x0675;
     */
    /* JADX WARNING: Missing block: B:407:0x0673, code skipped:
            if (r7.mWakeupFromSleep == false) goto L_0x06a5;
     */
    /* JADX WARNING: Missing block: B:408:0x0675, code skipped:
            r7.mImmeBright = false;
     */
    /* JADX WARNING: Missing block: B:409:0x067e, code skipped:
            if (r7.mAutomaticBrightnessController.getRebootFirstBrightnessAnimationEnable() == false) goto L_0x069c;
     */
    /* JADX WARNING: Missing block: B:411:0x0682, code skipped:
            if (r7.mRebootWakeupFromSleep == false) goto L_0x069c;
     */
    /* JADX WARNING: Missing block: B:412:0x0684, code skipped:
            if (r1 <= 0) goto L_0x0697;
     */
    /* JADX WARNING: Missing block: B:413:0x0687, code skipped:
            if (r2 == false) goto L_0x068c;
     */
    /* JADX WARNING: Missing block: B:414:0x0689, code skipped:
            r0 = r7.mBrightnessRampRateSlow;
     */
    /* JADX WARNING: Missing block: B:415:0x068c, code skipped:
            r0 = r7.mBrightnessRampRateFast;
     */
    /* JADX WARNING: Missing block: B:416:0x068e, code skipped:
            animateScreenBrightness(r1, r0);
            r7.mRebootWakeupFromSleep = false;
            r7.mWakeupFromSleep = false;
     */
    /* JADX WARNING: Missing block: B:417:0x0697, code skipped:
            animateScreenBrightness(r1, 0);
     */
    /* JADX WARNING: Missing block: B:419:0x069d, code skipped:
            if (r1 <= 0) goto L_0x06a1;
     */
    /* JADX WARNING: Missing block: B:420:0x069f, code skipped:
            r7.mWakeupFromSleep = false;
     */
    /* JADX WARNING: Missing block: B:421:0x06a1, code skipped:
            animateScreenBrightness(r1, 0);
     */
    /* JADX WARNING: Missing block: B:423:0x06ac, code skipped:
            if (r7.mAutomaticBrightnessController.getSetbrightnessImmediateEnableForCaliTest() == false) goto L_0x06b4;
     */
    /* JADX WARNING: Missing block: B:424:0x06ae, code skipped:
            animateScreenBrightness(r1, 0);
     */
    /* JADX WARNING: Missing block: B:425:0x06b4, code skipped:
            if (r2 == false) goto L_0x06b9;
     */
    /* JADX WARNING: Missing block: B:426:0x06b6, code skipped:
            r0 = r7.mBrightnessRampRateSlow;
     */
    /* JADX WARNING: Missing block: B:427:0x06b9, code skipped:
            r0 = r7.mBrightnessRampRateFast;
     */
    /* JADX WARNING: Missing block: B:428:0x06bb, code skipped:
            animateScreenBrightness(r1, r0);
     */
    /* JADX WARNING: Missing block: B:429:0x06bf, code skipped:
            r30 = r0;
     */
    /* JADX WARNING: Missing block: B:430:0x06c1, code skipped:
            if (r9 == false) goto L_0x06d5;
     */
    /* JADX WARNING: Missing block: B:432:0x06c7, code skipped:
            if (r7.mPowerRequest.useAutoBrightness != false) goto L_0x06d5;
     */
    /* JADX WARNING: Missing block: B:433:0x06c9, code skipped:
            if (r2 == false) goto L_0x06ce;
     */
    /* JADX WARNING: Missing block: B:434:0x06cb, code skipped:
            r0 = r7.mBrightnessRampRateSlow;
     */
    /* JADX WARNING: Missing block: B:435:0x06ce, code skipped:
            r0 = r7.mBrightnessRampRateFast;
     */
    /* JADX WARNING: Missing block: B:436:0x06d0, code skipped:
            animateScreenBrightness(r1, r0);
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:437:0x06d5, code skipped:
            r0 = false;
            animateScreenBrightness(r1, 0);
     */
    /* JADX WARNING: Missing block: B:438:0x06d9, code skipped:
            if (r1 <= 0) goto L_0x06dd;
     */
    /* JADX WARNING: Missing block: B:439:0x06db, code skipped:
            r7.mWakeupFromSleep = r0;
     */
    /* JADX WARNING: Missing block: B:441:0x06df, code skipped:
            if (NEED_NEW_BRIGHTNESS_PROCESS == false) goto L_0x06e6;
     */
    /* JADX WARNING: Missing block: B:442:0x06e1, code skipped:
            r7.mAutomaticBrightnessController.saveOffsetAlgorithmParas();
     */
    /* JADX WARNING: Missing block: B:443:0x06e6, code skipped:
            if (r9 != false) goto L_0x06eb;
     */
    /* JADX WARNING: Missing block: B:444:0x06e8, code skipped:
            notifyBrightnessChanged(r1, r8, r15);
     */
    /* JADX WARNING: Missing block: B:445:0x06eb, code skipped:
            r7.mLastWaitBrightnessMode = r7.mPowerRequest.brightnessWaitMode;
     */
    /* JADX WARNING: Missing block: B:446:0x06f3, code skipped:
            if (r7.mPendingScreenOnUnblocker != null) goto L_0x0715;
     */
    /* JADX WARNING: Missing block: B:448:0x06f7, code skipped:
            if (r7.mColorFadeEnabled == false) goto L_0x0709;
     */
    /* JADX WARNING: Missing block: B:450:0x06ff, code skipped:
            if (r7.mColorFadeOnAnimator.isStarted() != false) goto L_0x0715;
     */
    /* JADX WARNING: Missing block: B:452:0x0707, code skipped:
            if (r7.mColorFadeOffAnimator.isStarted() != false) goto L_0x0715;
     */
    /* JADX WARNING: Missing block: B:454:0x0711, code skipped:
            if (r7.mPowerState.waitUntilClean(r7.mCleanListener) == false) goto L_0x0715;
     */
    /* JADX WARNING: Missing block: B:455:0x0713, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:456:0x0715, code skipped:
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:457:0x0716, code skipped:
            r4 = r0;
     */
    /* JADX WARNING: Missing block: B:458:0x071d, code skipped:
            if (r7.mWindowManagerPolicy.getInterceptInputForWaitBrightness() == false) goto L_0x0731;
     */
    /* JADX WARNING: Missing block: B:460:0x0723, code skipped:
            if (r7.mPowerRequest.brightnessWaitMode != false) goto L_0x0731;
     */
    /* JADX WARNING: Missing block: B:462:0x0727, code skipped:
            if (r7.mPendingScreenOnForKeyguardDismissUnblocker != null) goto L_0x0731;
     */
    /* JADX WARNING: Missing block: B:463:0x0729, code skipped:
            if (r4 == false) goto L_0x0731;
     */
    /* JADX WARNING: Missing block: B:464:0x072b, code skipped:
            r7.mWindowManagerPolicy.setInterceptInputForWaitBrightness(false);
     */
    /* JADX WARNING: Missing block: B:465:0x0731, code skipped:
            if (r4 == false) goto L_0x073d;
     */
    /* JADX WARNING: Missing block: B:467:0x0739, code skipped:
            if (r7.mScreenBrightnessRampAnimator.isAnimating() != false) goto L_0x073d;
     */
    /* JADX WARNING: Missing block: B:468:0x073b, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:469:0x073d, code skipped:
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:470:0x073e, code skipped:
            r5 = r0;
     */
    /* JADX WARNING: Missing block: B:471:0x073f, code skipped:
            if (r4 == false) goto L_0x0751;
     */
    /* JADX WARNING: Missing block: B:473:0x0742, code skipped:
            if (r14 == 1) goto L_0x0751;
     */
    /* JADX WARNING: Missing block: B:475:0x0746, code skipped:
            if (r7.mReportedScreenStateToPolicy != 1) goto L_0x0751;
     */
    /* JADX WARNING: Missing block: B:476:0x0748, code skipped:
            setReportedScreenState(2);
            r7.mWindowManagerPolicy.screenTurnedOn();
     */
    /* JADX WARNING: Missing block: B:477:0x0751, code skipped:
            if (r5 != false) goto L_0x076a;
     */
    /* JADX WARNING: Missing block: B:479:0x0755, code skipped:
            if (r7.mUnfinishedBusiness != false) goto L_0x076a;
     */
    /* JADX WARNING: Missing block: B:481:0x0759, code skipped:
            if (DEBUG == false) goto L_0x0762;
     */
    /* JADX WARNING: Missing block: B:482:0x075b, code skipped:
            android.util.Slog.d(TAG, "Unfinished business...");
     */
    /* JADX WARNING: Missing block: B:483:0x0762, code skipped:
            r7.mCallbacks.acquireSuspendBlocker();
            r7.mUnfinishedBusiness = true;
     */
    /* JADX WARNING: Missing block: B:484:0x076a, code skipped:
            if (r4 == false) goto L_0x078b;
     */
    /* JADX WARNING: Missing block: B:485:0x076c, code skipped:
            if (r11 == false) goto L_0x078b;
     */
    /* JADX WARNING: Missing block: B:486:0x076e, code skipped:
            r6 = r7.mLock;
     */
    /* JADX WARNING: Missing block: B:487:0x0770, code skipped:
            monitor-enter(r6);
     */
    /* JADX WARNING: Missing block: B:490:0x0773, code skipped:
            if (r7.mPendingRequestChangedLocked != false) goto L_0x0783;
     */
    /* JADX WARNING: Missing block: B:491:0x0775, code skipped:
            r7.mDisplayReadyLocked = true;
     */
    /* JADX WARNING: Missing block: B:492:0x077a, code skipped:
            if (DEBUG == false) goto L_0x0783;
     */
    /* JADX WARNING: Missing block: B:493:0x077c, code skipped:
            android.util.Slog.d(TAG, "Display ready!");
     */
    /* JADX WARNING: Missing block: B:494:0x0783, code skipped:
            monitor-exit(r6);
     */
    /* JADX WARNING: Missing block: B:495:0x0784, code skipped:
            sendOnStateChangedWithWakelock();
     */
    /* JADX WARNING: Missing block: B:500:0x078b, code skipped:
            if (r5 == false) goto L_0x07b4;
     */
    /* JADX WARNING: Missing block: B:502:0x078f, code skipped:
            if (r7.mUnfinishedBusiness == false) goto L_0x07b4;
     */
    /* JADX WARNING: Missing block: B:504:0x0793, code skipped:
            if (DEBUG == false) goto L_0x079c;
     */
    /* JADX WARNING: Missing block: B:505:0x0795, code skipped:
            android.util.Slog.d(TAG, "Finished business...");
     */
    /* JADX WARNING: Missing block: B:507:0x079e, code skipped:
            if (DEBUG_FPLOG == false) goto L_0x07ab;
     */
    /* JADX WARNING: Missing block: B:509:0x07a2, code skipped:
            if (r7.fpDataCollector == null) goto L_0x07ab;
     */
    /* JADX WARNING: Missing block: B:510:0x07a4, code skipped:
            if (r1 <= 0) goto L_0x07ab;
     */
    /* JADX WARNING: Missing block: B:511:0x07a6, code skipped:
            r7.fpDataCollector.reportScreenTurnedOn();
     */
    /* JADX WARNING: Missing block: B:512:0x07ab, code skipped:
            r0 = false;
            r7.mUnfinishedBusiness = false;
            r7.mCallbacks.releaseSuspendBlocker();
     */
    /* JADX WARNING: Missing block: B:513:0x07b4, code skipped:
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:515:0x07b6, code skipped:
            if (r14 == 2) goto L_0x07ba;
     */
    /* JADX WARNING: Missing block: B:516:0x07b8, code skipped:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:517:0x07ba, code skipped:
            r7.mDozing = r0;
     */
    /* JADX WARNING: Missing block: B:518:0x07bc, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updatePowerState() {
        Throwable th;
        boolean mustInitialize = false;
        synchronized (this.mLock) {
            try {
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
                    }
                } else if (this.mPendingRequestChangedLocked) {
                    String str;
                    StringBuilder stringBuilder;
                    this.mBrightnessModeChanged = this.mPowerRequest.useAutoBrightness != this.mPendingRequestLocked.useAutoBrightness;
                    if (this.mBrightnessModeChanged && this.mPowerRequest.screenBrightnessOverride > 0 && this.mPendingRequestLocked.screenBrightnessOverride < 0) {
                        this.mBrightnessModeChanged = false;
                        if (DEBUG) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("mBrightnessModeChanged without db,brightness=");
                            stringBuilder2.append(this.mPowerRequest.screenBrightnessOverride);
                            stringBuilder2.append(",mPendingBrightness=");
                            stringBuilder2.append(this.mPendingRequestLocked.screenBrightnessOverride);
                            Slog.i(str2, stringBuilder2.toString());
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
                    if (!(!this.mBrightnessModeChanged || this.mCurrentUserIdChange || !this.mPowerRequest.useAutoBrightness || isCoverModeChanged || this.mIsCoverModeClosed || this.mBrightnessModeChangeNoClearOffsetEnable || this.mModeToAutoNoClearOffsetEnable)) {
                        updateAutoBrightnessAdjustFactor(0.0f);
                        if (DEBUG) {
                            Slog.d(TAG, "AdjustPositionBrightness set 0");
                        }
                    }
                    if (this.mBrightnessModeChangeNoClearOffsetEnable) {
                        this.mBrightnessModeChangeNoClearOffsetEnable = false;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("set mBrightnessModeChangeNoClearOffsetEnable=");
                        stringBuilder.append(this.mBrightnessModeChangeNoClearOffsetEnable);
                        Slog.i(str, stringBuilder.toString());
                    }
                    if (this.mBrightnessModeChanged && this.mModeToAutoNoClearOffsetEnable) {
                        this.mModeToAutoNoClearOffsetEnable = false;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("set mModeToAutoNoClearOffsetEnable1=");
                        stringBuilder.append(this.mModeToAutoNoClearOffsetEnable);
                        Slog.i(str, stringBuilder.toString());
                    }
                    this.mCurrentUserIdChange = false;
                    this.mWaitingForNegativeProximity |= this.mPendingWaitForNegativeProximityLocked;
                    this.mPendingWaitForNegativeProximityLocked = false;
                    this.mPendingRequestChangedLocked = false;
                    this.mDisplayReadyLocked = false;
                    writeAutoBrightnessDbEnable(this.mPowerRequest.useAutoBrightness);
                    if (this.mUseSensorHubLABC) {
                        boolean z = this.mLastStatus;
                    }
                }
                boolean mustInitialize2 = mustInitialize;
                try {
                    this.mScreenOnBecauseOfPhoneProximity = false;
                    boolean mustNotify = this.mDisplayReadyLocked ^ 1;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("blackScreenOnPcMode ");
            stringBuilder.append(e);
            HwPCUtils.log(str, stringBuilder.toString());
        }
    }

    private void sre_init(int state) {
        if (!this.mUseSensorHubLABC) {
            String str;
            StringBuilder stringBuilder;
            if (this.mSmartBackLightSupported && this.mSmartBackLightEnabled != this.mPowerRequest.useSmartBacklight) {
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mPowerRequest.useSmartBacklight change ");
                    stringBuilder.append(this.mSmartBackLightEnabled);
                    stringBuilder.append(" -> ");
                    stringBuilder.append(this.mPowerRequest.useSmartBacklight);
                    Slog.i(str, stringBuilder.toString());
                }
                this.mSmartBackLightEnabled = this.mPowerRequest.useSmartBacklight;
            }
            if (this.mUsingHwSmartBackLightController) {
                this.mHwSmartBackLightController.updatePowerState(state, this.mSmartBackLightEnabled);
            }
            if (this.mDisplayEngineInterface != null) {
                boolean lightSensorOnEnable = wantScreenOn(state);
                if (this.mLightSensorOnEnable != lightSensorOnEnable) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("LightSensorEnable change ");
                    stringBuilder2.append(this.mLightSensorOnEnable);
                    stringBuilder2.append(" -> ");
                    stringBuilder2.append(lightSensorOnEnable);
                    Slog.i(str2, stringBuilder2.toString());
                    this.mDisplayEngineInterface.updateLightSensorState(lightSensorOnEnable);
                    this.mLightSensorOnEnable = lightSensorOnEnable;
                }
            }
            if (this.mUsingSRE && this.mDisplayEngineInterface != null) {
                this.mSREEnabled = this.mPowerRequest.useSmartBacklight;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mPowerRequest.useSmartBacklight : ");
                stringBuilder.append(this.mSREEnabled);
                Slog.i(str, stringBuilder.toString());
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
            boolean isManulMode = true ^ this.mPowerRequest.useAutoBrightness;
            this.mAutomaticBrightnessController.setManualModeEnableForPg(isManulMode);
            this.mManualBrightnessController.updatePowerState(state, isManulMode);
        }
    }

    private void sendScreenStateToDE(int state) {
        if (this.mDisplayEngineInterface != null) {
            int currentState = getScreenOnState(state);
            if (this.mIsScreenOn != currentState) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ScreenState change ");
                stringBuilder.append(this.mIsScreenOn);
                stringBuilder.append(" -> ");
                stringBuilder.append(currentState);
                Slog.i(str, stringBuilder.toString());
                if (currentState == 1) {
                    this.mDisplayEngineInterface.setScene("SCENE_REAL_POWERMODE", "ACTION_MODE_ON");
                    HwServiceFactory.setPowerState(6);
                } else {
                    this.mDisplayEngineInterface.setScene("SCENE_REAL_POWERMODE", "ACTION_MODE_OFF");
                    HwServiceFactory.setPowerState(7);
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
            this.mPendingScreenOnForKeyguardDismissUnblocker = new ScreenOnForKeyguardDismissUnblocker(this, null);
            this.mScreenOnForKeyguardDismissBlockStartRealTime = SystemClock.elapsedRealtime();
            Slog.i(TAG, "Blocking screen on until keyguard dismiss done.");
            IZrHung iZrHung = HwFrameworkFactory.getZrHung("zrhung_wp_screenon_framework");
            if (iZrHung != null) {
                ZrHungData arg = new ZrHungData();
                arg.putString("addScreenOnInfo", "Blocking screen on until keyguard dismiss done");
                iZrHung.addInfo(arg);
            }
        }
    }

    private void unblockScreenOnForKeyguardDismiss() {
        if (this.mPendingScreenOnForKeyguardDismissUnblocker != null) {
            this.mPendingScreenOnForKeyguardDismissUnblocker = null;
            long delay = SystemClock.elapsedRealtime() - this.mScreenOnForKeyguardDismissBlockStartRealTime;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fingerunlock--Unblocked screen on for keyguard dismiss after ");
            stringBuilder.append(delay);
            stringBuilder.append(" ms");
            Slog.i(str, stringBuilder.toString());
            IZrHung iZrHung = HwFrameworkFactory.getZrHung("zrhung_wp_screenon_framework");
            if (iZrHung != null) {
                ZrHungData arg = new ZrHungData();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unblocked screen on for keyguard dismiss after ");
                stringBuilder2.append(delay);
                stringBuilder2.append(" ms");
                arg.putString("addScreenOnInfo", stringBuilder2.toString());
                iZrHung.addInfo(arg);
            }
        }
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
            this.mPendingScreenOnUnblocker = new ScreenOnUnblocker(this, null);
            this.mScreenOnBlockStartRealTime = SystemClock.elapsedRealtime();
            if (Jlog.isPerfTest()) {
                Jlog.i(2205, "JL_PWRSCRON_DPC_BLOCKSCREENON");
            }
            Flog.i(NativeResponseCode.SERVICE_FOUND, "UL_Power Blocking screen on until initial contents have been drawn.");
            IZrHung iZrHung = HwFrameworkFactory.getZrHung("zrhung_wp_screenon_framework");
            if (iZrHung != null) {
                ZrHungData arg = new ZrHungData();
                arg.putString("addScreenOnInfo", "Blocking screen on until initial contents have been drawn");
                iZrHung.addInfo(arg);
            }
        }
    }

    private void unblockScreenOn() {
        if (this.mPendingScreenOnUnblocker != null) {
            this.mPendingScreenOnUnblocker = null;
            long delay = SystemClock.elapsedRealtime() - this.mScreenOnBlockStartRealTime;
            if (Jlog.isPerfTest()) {
                Jlog.i(2206, "JL_PWRSCRON_DPC_UNBLOCKSCREENON");
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UL_Power Unblocked screen on after ");
            stringBuilder.append(delay);
            stringBuilder.append(" ms");
            Flog.i(NativeResponseCode.SERVICE_FOUND, stringBuilder.toString());
            Trace.asyncTraceEnd(131072, SCREEN_ON_BLOCKED_TRACE_NAME, 0);
            IZrHung iZrHung = HwFrameworkFactory.getZrHung("zrhung_wp_screenon_framework");
            if (iZrHung != null) {
                ZrHungData arg = new ZrHungData();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unblocked screen on after ");
                stringBuilder2.append(delay);
                stringBuilder2.append(" ms");
                arg.putString("addScreenOnInfo", stringBuilder2.toString());
                iZrHung.addInfo(arg);
            }
        }
    }

    private void blockScreenOff() {
        if (this.mPendingScreenOffUnblocker == null) {
            Trace.asyncTraceBegin(131072, SCREEN_OFF_BLOCKED_TRACE_NAME, 0);
            this.mPendingScreenOffUnblocker = new ScreenOffUnblocker(this, null);
            this.mScreenOffBlockStartRealTime = SystemClock.elapsedRealtime();
            Slog.i(TAG, "UL_Power Blocking screen off");
        }
    }

    private void unblockScreenOff() {
        if (this.mPendingScreenOffUnblocker != null) {
            this.mPendingScreenOffUnblocker = null;
            long delay = SystemClock.elapsedRealtime() - this.mScreenOffBlockStartRealTime;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UL_PowerUnblocked screen off after ");
            stringBuilder.append(delay);
            stringBuilder.append(" ms");
            Slog.i(str, stringBuilder.toString());
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
            if (!(!isOff || this.mScreenOffBecauseOfProximity || this.mScreenOnBecauseOfPhoneProximity)) {
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
            this.mWindowManagerPolicy.screenTurnedOff();
            setBrightnessAnimationTime(false, 500);
            setBrightnessNoLimit(-1, 500);
            this.mBrightnessModeChangeNoClearOffsetEnable = this.mPoweroffModeChangeAutoEnable;
            if (this.mPoweroffModeChangeAutoEnable) {
                this.mPoweroffModeChangeAutoEnable = false;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("poweroff set mPoweroffModeChangeAutoEnable=");
                stringBuilder.append(this.mPoweroffModeChangeAutoEnable);
                Slog.i(str, stringBuilder.toString());
            }
        } else if (!(isOff || isDoze || this.mReportedScreenStateToPolicy != 3)) {
            unblockScreenOff();
            this.mWindowManagerPolicy.screenTurnedOff();
            setReportedScreenState(0);
        }
        if (!(isOff || isDoze || this.mReportedScreenStateToPolicy != 0)) {
            setReportedScreenState(1);
            if (this.mPowerState.getColorFadeLevel() == 0.0f) {
                blockScreenOn();
            } else {
                unblockScreenOn();
            }
            this.mWindowManagerPolicy.screenTurningOn(this.mPendingScreenOnUnblocker);
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
        if (DEBUG && DEBUG_Controller) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("waitScreenBrightness displayState = ");
            stringBuilder.append(displayState);
            stringBuilder.append(" curReqWaitBright = ");
            stringBuilder.append(curReqWaitBright);
            stringBuilder.append(" lastReqWaitBright = ");
            stringBuilder.append(lastReqWaitBright);
            stringBuilder.append(" enableBright = ");
            stringBuilder.append(enableBright);
            stringBuilder.append(" skipWaitKeyguardDismiss = ");
            stringBuilder.append(skipWaitKeyguardDismiss);
            Slog.i(str, stringBuilder.toString());
        }
        boolean z = true;
        if (displayState == 2) {
            if (curReqWaitBright) {
                return true;
            }
            if (lastReqWaitBright && enableBright && !skipWaitKeyguardDismiss) {
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

    private int clampScreenBrightnessForVr(int value) {
        return MathUtils.constrain(value, this.mScreenBrightnessForVrRangeMinimum, this.mScreenBrightnessForVrRangeMaximum);
    }

    private int clampScreenBrightness(int value) {
        return MathUtils.constrain(value, this.mScreenBrightnessRangeMinimum, this.mScreenBrightnessRangeMaximum);
    }

    private void animateScreenBrightness(int target, int rate) {
        int brightnessTargetReal = target;
        if (!(this.mPowerRequest == null || this.mContext == null || this.mPowerRequest.useAutoBrightness || this.mLastBrightnessForAutoBrightnessDB == 0 || target != 0)) {
            int brightnessDB = System.getIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", this.mScreenBrightnessDefault, this.mCurrentUserId);
            if (brightnessDB != 0) {
                System.putIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", 0, this.mCurrentUserId);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("LabcCoverMode manualMode set screen_auto_brightness db=0 when poweroff,OrigbrightnessDB=");
                    stringBuilder.append(brightnessDB);
                    Slog.i(str, stringBuilder.toString());
                }
            }
        }
        this.mLastBrightnessForAutoBrightnessDB = target;
        if (!this.mPowerRequest.useAutoBrightness && brightnessTargetReal > 0) {
            this.mManualBrightnessController.updateManualBrightness(brightnessTargetReal);
            brightnessTargetReal = this.mManualBrightnessController.getManualBrightness();
            if (this.mManualBrightnessController.needFastestRateForManualBrightness()) {
                rate = 0;
            }
            this.mAutomaticBrightnessController.setBackSensorCoverModeBrightness(brightnessTargetReal);
        }
        if (DEBUG) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Animating brightness: target=");
            stringBuilder2.append(target);
            stringBuilder2.append(", rate=");
            stringBuilder2.append(rate);
            stringBuilder2.append(",brightnessTargetReal=");
            stringBuilder2.append(brightnessTargetReal);
            stringBuilder2.append(",AutoBrightness=");
            stringBuilder2.append(this.mPowerRequest.useAutoBrightness);
            Slog.d(str2, stringBuilder2.toString());
        }
        if (target >= 0 && brightnessTargetReal >= 0) {
            if (target == 0 && rate != 0) {
                rate = 0;
                Slog.e(TAG, "Animating brightness rate is invalid when screen off, set rate to 0");
            }
            if (this.mScreenBrightnessRampAnimator.animateTo(brightnessTargetReal, rate)) {
                Trace.traceCounter(131072, "TargetScreenBrightness", target);
                try {
                    if (this.mUsingHwSmartBackLightController && this.mSmartBackLightEnabled && rate > 0) {
                        if (this.mScreenBrightnessRampAnimator.isAnimating()) {
                            IHwSmartBackLightController iHwSmartBackLightController = this.mHwSmartBackLightController;
                            IHwSmartBackLightController iHwSmartBackLightController2 = this.mHwSmartBackLightController;
                            iHwSmartBackLightController.updateBrightnessState(0);
                        } else if (DEBUG) {
                            Slog.i(TAG, "brightness changed but not animating");
                        }
                    }
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("brightness=");
                    stringBuilder3.append(brightnessTargetReal);
                    HwLog.dubaie("DUBAI_TAG_BRIGHTNESS", stringBuilder3.toString());
                    this.mBatteryStats.noteScreenBrightness(target);
                    sendTargetBrightnessToMonitor(target);
                } catch (RemoteException e) {
                }
            } else if (!(this.mPowerRequest.useAutoBrightness || this.mLastBrightnessTarget == target)) {
                Trace.traceCounter(131072, "TargetScreenBrightness", target);
                try {
                    this.mBatteryStats.noteScreenBrightness(target);
                } catch (RemoteException e2) {
                }
            }
            this.mLastBrightnessTarget = target;
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
        if (this.mDisplayBlanksAfterDozeConfig && Display.isDozeState(this.mPowerState.getScreenState()) && !Display.isDozeState(target)) {
            this.mPowerState.prepareColorFade(this.mContext, this.mColorFadeFadesConfig ? 2 : 0);
            if (this.mColorFadeOffAnimator != null) {
                this.mColorFadeOffAnimator.end();
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
            if (!(this.mScreenBrightnessRampAnimator.isAnimating() && this.mPowerState.getScreenState() == 2) && setScreenState(5)) {
                this.mPowerState.setColorFadeLevel(1.0f);
                this.mPowerState.dismissColorFade();
            }
        } else if (target == 3) {
            if (!(this.mScreenBrightnessRampAnimator.isAnimating() && this.mPowerState.getScreenState() == 2) && setScreenState(3)) {
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
            } else {
                if (performScreenOffTransition && !checkPhoneWindowIsTop()) {
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
        if (this.mProximitySensorEnabled && (this.mPendingProximity != 0 || positive)) {
            if (this.mPendingProximity != 1 || !positive) {
                HwServiceFactory.reportProximitySensorEventToIAware(positive);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UL_Power handleProximitySensorEvent positive:");
                stringBuilder.append(positive);
                Slog.d(str, stringBuilder.toString());
                this.mHandler.removeMessages(2);
                if (positive) {
                    this.mPendingProximity = 1;
                    setPendingProximityDebounceTime(0 + time);
                } else {
                    this.mPendingProximity = 0;
                    setPendingProximityDebounceTime(0 + time);
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

    private void handleSettingsChange(boolean userSwitch) {
        this.mPendingScreenBrightnessSetting = getScreenBrightnessSetting();
        if (userSwitch) {
            this.mCurrentScreenBrightnessSetting = this.mPendingScreenBrightnessSetting;
            if (this.mAutomaticBrightnessController != null) {
                this.mAutomaticBrightnessController.resetShortTermModel();
            }
        }
        this.mPendingAutoBrightnessAdjustment = getAutoBrightnessAdjustmentSetting();
        this.mScreenBrightnessForVr = getScreenBrightnessForVrSetting();
        if (!this.mPowerRequest.useAutoBrightness) {
            sendUpdatePowerState();
        }
    }

    private float getAutoBrightnessAdjustmentSetting() {
        float adj = System.getFloatForUser(this.mContext.getContentResolver(), "screen_auto_brightness_adj", 0.0f, this.mCurrentUserId);
        if (Float.isNaN(adj)) {
            return 0.0f;
        }
        return clampAutoBrightnessAdjustment(adj);
    }

    private int getScreenBrightnessSetting() {
        return clampAbsoluteBrightness(System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", this.mScreenBrightnessDefault, this.mCurrentUserId));
    }

    private int getScreenBrightnessForVrSetting() {
        return clampScreenBrightnessForVr(System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_for_vr", this.mScreenBrightnessForVrDefault, this.mCurrentUserId));
    }

    private void putScreenBrightnessSetting(int brightness) {
        this.mCurrentScreenBrightnessSetting = brightness;
        if (this.mCurrentScreenBrightnessSettingForDB != brightness) {
            System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness", brightness, this.mCurrentUserId);
        }
        this.mCurrentScreenBrightnessSettingForDB = brightness;
    }

    private void putAutoBrightnessAdjustmentSetting(float adjustment) {
        this.mAutoBrightnessAdjustment = adjustment;
        System.putFloatForUser(this.mContext.getContentResolver(), "screen_auto_brightness_adj", adjustment, this.mCurrentUserId);
    }

    private boolean updateAutoBrightnessAdjustment() {
        if (Float.isNaN(this.mPendingAutoBrightnessAdjustment)) {
            return false;
        }
        if (this.mAutoBrightnessAdjustment == this.mPendingAutoBrightnessAdjustment) {
            this.mPendingAutoBrightnessAdjustment = Float.NaN;
            return false;
        }
        this.mAutoBrightnessAdjustment = this.mPendingAutoBrightnessAdjustment;
        this.mPendingAutoBrightnessAdjustment = Float.NaN;
        return true;
    }

    private boolean updateUserSetScreenBrightness() {
        if (this.mPendingScreenBrightnessSetting < 0) {
            return false;
        }
        if (this.mCurrentScreenBrightnessSetting == this.mPendingScreenBrightnessSetting) {
            this.mPendingScreenBrightnessSetting = -1;
            return false;
        }
        this.mCurrentScreenBrightnessSetting = this.mPendingScreenBrightnessSetting;
        this.mLastUserSetScreenBrightness = this.mPendingScreenBrightnessSetting;
        this.mPendingScreenBrightnessSetting = -1;
        return true;
    }

    private void notifyBrightnessChanged(int brightness, boolean userInitiated, boolean hadUserDataPoint) {
        float brightnessInNits = convertToNits(brightness);
        if (this.mPowerRequest.useAutoBrightness && brightnessInNits >= 0.0f && this.mAutomaticBrightnessController != null) {
            float f;
            if (this.mPowerRequest.lowPowerMode) {
                f = this.mPowerRequest.screenLowPowerBrightnessFactor;
            } else {
                f = 1.0f;
            }
            float powerFactor = f;
            this.mBrightnessTracker.notifyBrightnessChanged(brightnessInNits, userInitiated, powerFactor, hadUserDataPoint, this.mAutomaticBrightnessController.isDefaultConfig());
        }
    }

    private float convertToNits(int backlight) {
        if (this.mBrightnessMapper != null) {
            return this.mBrightnessMapper.convertToNits(backlight);
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  mDisplayReadyLocked=");
            stringBuilder.append(this.mDisplayReadyLocked);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mPendingRequestLocked=");
            stringBuilder.append(this.mPendingRequestLocked);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mPendingRequestChangedLocked=");
            stringBuilder.append(this.mPendingRequestChangedLocked);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mPendingWaitForNegativeProximityLocked=");
            stringBuilder.append(this.mPendingWaitForNegativeProximityLocked);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mPendingUpdatePowerStateLocked=");
            stringBuilder.append(this.mPendingUpdatePowerStateLocked);
            pw.println(stringBuilder.toString());
        }
        pw.println();
        pw.println("Display Power Controller Configuration:");
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mScreenBrightnessDozeConfig=");
        stringBuilder2.append(this.mScreenBrightnessDozeConfig);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mScreenBrightnessDimConfig=");
        stringBuilder2.append(this.mScreenBrightnessDimConfig);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mScreenBrightnessRangeMinimum=");
        stringBuilder2.append(this.mScreenBrightnessRangeMinimum);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mScreenBrightnessRangeMaximum=");
        stringBuilder2.append(this.mScreenBrightnessRangeMaximum);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mScreenBrightnessDefault=");
        stringBuilder2.append(this.mScreenBrightnessDefault);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mScreenBrightnessForVrRangeMinimum=");
        stringBuilder2.append(this.mScreenBrightnessForVrRangeMinimum);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mScreenBrightnessForVrRangeMaximum=");
        stringBuilder2.append(this.mScreenBrightnessForVrRangeMaximum);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mScreenBrightnessForVrDefault=");
        stringBuilder2.append(this.mScreenBrightnessForVrDefault);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mUseSoftwareAutoBrightnessConfig=");
        stringBuilder2.append(this.mUseSoftwareAutoBrightnessConfig);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mAllowAutoBrightnessWhileDozingConfig=");
        stringBuilder2.append(this.mAllowAutoBrightnessWhileDozingConfig);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mBrightnessRampRateFast=");
        stringBuilder2.append(this.mBrightnessRampRateFast);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mBrightnessRampRateSlow=");
        stringBuilder2.append(this.mBrightnessRampRateSlow);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mSkipScreenOnBrightnessRamp=");
        stringBuilder2.append(this.mSkipScreenOnBrightnessRamp);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mColorFadeFadesConfig=");
        stringBuilder2.append(this.mColorFadeFadesConfig);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mColorFadeEnabled=");
        stringBuilder2.append(this.mColorFadeEnabled);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mDisplayBlanksAfterDozeConfig=");
        stringBuilder2.append(this.mDisplayBlanksAfterDozeConfig);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("  mBrightnessBucketsInDozeConfig=");
        stringBuilder2.append(this.mBrightnessBucketsInDozeConfig);
        pw.println(stringBuilder2.toString());
        this.mHandler.runWithScissors(new Runnable() {
            public void run() {
                DisplayPowerController.this.dumpLocal(pw);
            }
        }, 1000);
    }

    private void dumpLocal(PrintWriter pw) {
        pw.println();
        pw.println("Display Power Controller Thread State:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mPowerRequest=");
        stringBuilder.append(this.mPowerRequest);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mUnfinishedBusiness=");
        stringBuilder.append(this.mUnfinishedBusiness);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mWaitingForNegativeProximity=");
        stringBuilder.append(this.mWaitingForNegativeProximity);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mProximitySensor=");
        stringBuilder.append(this.mProximitySensor);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mProximitySensorEnabled=");
        stringBuilder.append(this.mProximitySensorEnabled);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mProximityThreshold=");
        stringBuilder.append(this.mProximityThreshold);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mProximity=");
        stringBuilder.append(proximityToString(this.mProximity));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mPendingProximity=");
        stringBuilder.append(proximityToString(this.mPendingProximity));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mPendingProximityDebounceTime=");
        stringBuilder.append(TimeUtils.formatUptime(this.mPendingProximityDebounceTime));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenOffBecauseOfProximity=");
        stringBuilder.append(this.mScreenOffBecauseOfProximity);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mLastUserSetScreenBrightness=");
        stringBuilder.append(this.mLastUserSetScreenBrightness);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mCurrentScreenBrightnessSetting=");
        stringBuilder.append(this.mCurrentScreenBrightnessSetting);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mPendingScreenBrightnessSetting=");
        stringBuilder.append(this.mPendingScreenBrightnessSetting);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mTemporaryScreenBrightness=");
        stringBuilder.append(this.mTemporaryScreenBrightness);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mAutoBrightnessAdjustment=");
        stringBuilder.append(this.mAutoBrightnessAdjustment);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mTemporaryAutoBrightnessAdjustment=");
        stringBuilder.append(this.mTemporaryAutoBrightnessAdjustment);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mPendingAutoBrightnessAdjustment=");
        stringBuilder.append(this.mPendingAutoBrightnessAdjustment);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenBrightnessForVr=");
        stringBuilder.append(this.mScreenBrightnessForVr);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mAppliedAutoBrightness=");
        stringBuilder.append(this.mAppliedAutoBrightness);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mAppliedDimming=");
        stringBuilder.append(this.mAppliedDimming);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mAppliedLowPower=");
        stringBuilder.append(this.mAppliedLowPower);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mAppliedScreenBrightnessOverride=");
        stringBuilder.append(this.mAppliedScreenBrightnessOverride);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mAppliedTemporaryBrightness=");
        stringBuilder.append(this.mAppliedTemporaryBrightness);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mDozing=");
        stringBuilder.append(this.mDozing);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mSkipRampState=");
        stringBuilder.append(skipRampStateToString(this.mSkipRampState));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mInitialAutoBrightness=");
        stringBuilder.append(this.mInitialAutoBrightness);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenOnBlockStartRealTime=");
        stringBuilder.append(this.mScreenOnBlockStartRealTime);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenOffBlockStartRealTime=");
        stringBuilder.append(this.mScreenOffBlockStartRealTime);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mPendingScreenOnUnblocker=");
        stringBuilder.append(this.mPendingScreenOnUnblocker);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mPendingScreenOffUnblocker=");
        stringBuilder.append(this.mPendingScreenOffUnblocker);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mPendingScreenOff=");
        stringBuilder.append(this.mPendingScreenOff);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mReportedToPolicy=");
        stringBuilder.append(reportedToPolicyToString(this.mReportedScreenStateToPolicy));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mScreenBrightnessRampAnimator.isAnimating()=");
        stringBuilder.append(this.mScreenBrightnessRampAnimator.isAnimating());
        pw.println(stringBuilder.toString());
        if (this.mColorFadeOnAnimator != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mColorFadeOnAnimator.isStarted()=");
            stringBuilder.append(this.mColorFadeOnAnimator.isStarted());
            pw.println(stringBuilder.toString());
        }
        if (this.mColorFadeOffAnimator != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mColorFadeOffAnimator.isStarted()=");
            stringBuilder.append(this.mColorFadeOffAnimator.isStarted());
            pw.println(stringBuilder.toString());
        }
        if (this.mPowerState != null) {
            this.mPowerState.dump(pw);
        }
        if (this.mAutomaticBrightnessController != null) {
            this.mAutomaticBrightnessController.dump(pw);
        }
        if (this.mBrightnessTracker != null) {
            pw.println();
            this.mBrightnessTracker.dump(pw);
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Auto-brightness spline: ");
                stringBuilder.append(spline);
                Slog.d(str, stringBuilder.toString());
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

    private static String skipRampStateToString(int state) {
        switch (state) {
            case 0:
                return "RAMP_STATE_SKIP_NONE";
            case 1:
                return "RAMP_STATE_SKIP_INITIAL";
            case 2:
                return "RAMP_STATE_SKIP_AUTOBRIGHT";
            default:
                return Integer.toString(state);
        }
    }

    private static int clampAbsoluteBrightness(int value) {
        return MathUtils.constrain(value, 0, 255);
    }

    private static float clampAutoBrightnessAdjustment(float value) {
        return MathUtils.constrain(value, -1.0f, 1.0f);
    }

    private boolean checkPhoneWindowIsTop() {
        String incalluiPackageName = "com.android.incallui";
        String incalluiClassName = "com.android.incallui.InCallActivity";
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        long startTime = SystemClock.elapsedRealtime();
        List<RunningTaskInfo> tasksInfo = activityManager.getRunningTasks(1);
        long getRunningTasksDuration = SystemClock.elapsedRealtime() - startTime;
        if (getRunningTasksDuration > 500) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Check Phone Window is top, get the Running Tasks duration: ");
            stringBuilder.append(getRunningTasksDuration);
            Slog.i(str, stringBuilder.toString());
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
            if (this.mPowerRequest.policy == 2 || !autoEnable) {
                this.mBackLight.writeAutoBrightnessDbEnable(false);
            } else if (!this.mPowerPolicyChangeFromDimming) {
                this.mBackLight.writeAutoBrightnessDbEnable(true);
            }
        }
    }

    public void updateProximityState(boolean proximityState) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateProximityState:");
            stringBuilder.append(proximityState);
            Slog.d(str, stringBuilder.toString());
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
        if (this.mManualBrightnessController != null && this.mScreenBrightnessRampAnimator != null && this.mAutomaticBrightnessController != null) {
            int brightnessMode = System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 1, this.mPowerRequest.userId);
            boolean autoPowerSavingUseManualAnimationTimeEnable = this.mAutomaticBrightnessController.getAutoPowerSavingUseManualAnimationTimeEnable();
            boolean manualPowerSavingAnimationEnable;
            if (brightnessMode == 1 && autoPowerSavingUseManualAnimationTimeEnable) {
                manualPowerSavingAnimationEnable = this.mAutomaticBrightnessController.getAutoPowerSavingAnimationEnable();
                this.mScreenBrightnessRampAnimator.updateManualPowerSavingAnimationEnable(manualPowerSavingAnimationEnable);
                if (manualPowerSavingAnimationEnable) {
                    this.mAutomaticBrightnessController.setAutoPowerSavingAnimationEnable(false);
                    return;
                }
                return;
            }
            manualPowerSavingAnimationEnable = this.mManualBrightnessController.getManualPowerSavingAnimationEnable();
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

    private void updateDarkAdaptDimmingEnable() {
        if (this.mAutomaticBrightnessController != null && this.mScreenBrightnessRampAnimator != null) {
            boolean darkAdaptDimmingEnable = this.mAutomaticBrightnessController.getDarkAdaptDimmingEnable();
            this.mScreenBrightnessRampAnimator.updateDarkAdaptAnimationDimmingEnable(darkAdaptDimmingEnable);
            if (darkAdaptDimmingEnable) {
                this.mAutomaticBrightnessController.clearDarkAdaptDimmingEnable();
            }
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("set mModeToAutoNoClearOffsetEnable=");
            stringBuilder.append(this.mModeToAutoNoClearOffsetEnable);
            Slog.d(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("set mPoweroffModeChangeAutoEnable=");
            stringBuilder.append(this.mPoweroffModeChangeAutoEnable);
            stringBuilder.append(",mNoClearOffsetEnable=");
            stringBuilder.append(this.mBrightnessModeChangeNoClearOffsetEnable);
            Slog.d(str, stringBuilder.toString());
        }
    }

    public void setBrightnessNoLimit(int brightness, int time) {
        this.mSetBrightnessNoLimitAnimationTime = time;
        this.mAutomaticBrightnessController.setBrightnessNoLimit(brightness, time);
        this.mManualBrightnessController.setBrightnessNoLimit(brightness, time);
    }

    public boolean hwBrightnessSetData(String name, Bundle data, int[] result) {
        return HwServiceFactory.hwBrightnessSetData(name, data, result);
    }

    public boolean hwBrightnessGetData(String name, Bundle data, int[] result) {
        return HwServiceFactory.hwBrightnessGetData(name, data, result);
    }
}
