package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.BrightnessConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.util.ArrayMap;
import android.util.HwNormalizedSpline;
import android.util.HwNormalizedSpline.DarkAdaptState;
import android.util.Log;
import android.util.Slog;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.display.AutomaticBrightnessController.Callbacks;
import com.android.server.display.DarkAdaptDetector.AdaptState;
import com.android.server.display.DisplayEffectMonitor.MonitorModule;
import com.android.server.display.HwBrightnessPgSceneDetection.HwBrightnessPgSceneDetectionCallbacks;
import com.android.server.display.HwBrightnessXmlLoader.Data;
import com.android.server.gesture.GestureNavConst;
import com.android.server.lights.LightsManager;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import com.huawei.displayengine.DisplayEngineManager;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class HwNormalizedAutomaticBrightnessController extends AutomaticBrightnessController implements HwBrightnessPgSceneDetectionCallbacks {
    private static final int BACK_SENSOR_COVER_MODE_BEIGHTNESS = -3;
    private static final int BRIGHTNESS_FOR_PROXIMITY_POSITIVE = -2;
    private static final int BRIGHTNESS_FOR_SENSOR_NOT_READY_WHEN_WAKEUP = -1;
    private static final boolean DEBUG;
    private static final String KEY_READING_MODE_SWITCH = "hw_reading_mode_display_switch";
    private static final int MAXDEFAULTBRIGHTNESS = 255;
    private static final int MINDEFAULTBRIGHTNESS = 4;
    private static final int MODE_DEFAULT = 0;
    private static final int MODE_TOP_GAME = 1;
    private static final int MSG_CoverMode_DEBOUNCED = 4;
    private static final int MSG_PROXIMITY_SENSOR_DEBOUNCED = 3;
    private static final int MSG_REPORT_PROXIMITY_STATE = 2;
    private static final int MSG_UPDATE_AMBIENT_LUX = 1;
    private static final long POWER_ON_LUX_ABANDON_COUNT_MAX = 3;
    private static final int POWER_ON_LUX_COUNT_MAX = 8;
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private static final int PROXIMITY_UNKNOWN = -1;
    public static final String SCREEN_BRIGHTNESS_MODE_LAST = "screen_brightness_mode_last";
    private static String TAG = "HwNormalizedAutomaticBrightnessController";
    private static final int TIME_DELAYED_USING_PROXIMITY_STATE = 500;
    private static final int TIME_PRINT_SENSOR_VALUE_INTERVAL = 4000;
    private static final int TIME_SENSOR_REPORT_NONE_VALUE = 400;
    private static int mDeviceActualBrightnessLevel;
    private static int mDeviceActualBrightnessNit;
    private static int mDeviceStandardBrightnessNit;
    private static HwNormalizedSpline mHwNormalizedScreenAutoBrightnessSpline;
    private static final Object mLock = new Object();
    private int SENSOR_OPTION;
    private long gameModeEnterTimestamp;
    private long gameModeQuitTimestamp;
    private boolean mAllowLabcUseProximity;
    private float mAmbientLuxLast;
    private float mAmbientLuxOffset;
    private boolean mAnimationGameChangeEnable;
    private float mAutoBrightnessOut;
    private final HandlerThread mAutoBrightnessProcessThread;
    private boolean mAutoPowerSavingAnimationEnable;
    private boolean mAutoPowerSavingBrighnessLineDisableForDemo;
    private int mBrightnessOutForLog;
    private boolean mCameraModeChangeAnimationEnable;
    private boolean mCameraModeEnable;
    private final Context mContext;
    private int mCoverModeFastResponseTimeDelay;
    private boolean mCoverStateFast;
    private CryogenicPowerProcessor mCryogenicProcessor;
    private boolean mCurrentUserChanging;
    private int mCurrentUserId;
    private int mCurveLevel;
    private DarkAdaptDetector mDarkAdaptDetector;
    private boolean mDarkAdaptDimmingEnable;
    private AdaptState mDarkAdaptState;
    private final Data mData;
    private float mDefaultBrightness;
    private DisplayEffectMonitor mDisplayEffectMonitor;
    private volatile boolean mDragFinished;
    private int mDualSensorRawAmbient;
    private int mEyeProtectionMode;
    private ContentObserver mEyeProtectionModeObserver;
    private int mGameLevel;
    private boolean mGameModeEnableForOffset;
    private HwRingBuffer mHwAmbientLightRingBuffer;
    private HwRingBuffer mHwAmbientLightRingBufferTrace;
    private HwAmbientLuxFilterAlgo mHwAmbientLuxFilterAlgo;
    private HwBrightnessMapping mHwBrightnessMapping;
    private HwBrightnessPgSceneDetection mHwBrightnessPgSceneDetection;
    private HwBrightnessPowerSavingCurve mHwBrightnessPowerSavingCurve;
    private HwDualSensorEventListenerImpl mHwDualSensorEventListenerImpl;
    private HwEyeProtectionControllerImpl mHwEyeProtectionController;
    private int mHwLastReportedSensorValue;
    private long mHwLastReportedSensorValueTime;
    private int mHwLastSensorValue;
    private final HwNormalizedAutomaticBrightnessHandler mHwNormalizedAutomaticBrightnessHandler;
    private long mHwPrintLogTime;
    private int mHwRateMillis;
    private boolean mHwReportValueWhenSensorOnChange;
    private boolean mIntervenedAutoBrightnessEnable;
    private boolean mIsBrightnessLimitedByThermal;
    private boolean mIsclosed;
    private long mLastAmbientLightToMonitorTime;
    private int mLastDefaultBrightness;
    private DisplayEngineManager mManager;
    private Runnable mMaxBrightnessFromCryogenicDelayedRunnable;
    private Handler mMaxBrightnessFromCryogenicHandler;
    private boolean mPolicyChangeFromDim;
    private long mPowerOffVehicleTimestamp;
    private boolean mPowerOnEnable;
    private int mPowerOnLuxAbandonCount;
    private int mPowerOnLuxCount;
    private boolean mPowerOnOffStatus;
    private long mPowerOnVehicleTimestamp;
    private int mPowerPolicy;
    private String mPowerStateNameForMonitor;
    private boolean mPowerStatus;
    private int mProximity;
    private boolean mProximityPositive;
    private long mProximityReportTime;
    private final Sensor mProximitySensor;
    private boolean mProximitySensorEnabled;
    private final SensorEventListener mProximitySensorListener;
    private int mReadingMode;
    private boolean mReadingModeChangeAnimationEnable;
    private boolean mReadingModeEnable;
    private ContentObserver mReadingModeObserver;
    private int mSceneLevel;
    private volatile int mScreenBrightnessBeforeAdj;
    private ScreenStateReceiver mScreenStateReceiver;
    private boolean mScreenStatus;
    private SensorObserver mSensorObserver;
    private TouchProximityDetector mTouchProximityDetector;
    private boolean mVehicleModeQuitEnable;
    private boolean mWakeupCoverBrightnessEnable;

    private final class HwNormalizedAutomaticBrightnessHandler extends Handler {
        public HwNormalizedAutomaticBrightnessHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    HwNormalizedAutomaticBrightnessController.this.handleUpdateAmbientLuxMsg();
                    return;
                case 2:
                    HwNormalizedAutomaticBrightnessController.this.handleProximitySensorEvent();
                    return;
                case 3:
                    HwNormalizedAutomaticBrightnessController.this.debounceProximitySensor();
                    return;
                case 4:
                    HwNormalizedAutomaticBrightnessController.this.setCoverModeFastResponseFlag();
                    return;
                default:
                    return;
            }
        }
    }

    private class ScreenStateReceiver extends BroadcastReceiver {
        public ScreenStateReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BOOT_COMPLETED");
            filter.setPriority(1000);
            HwNormalizedAutomaticBrightnessController.this.mContext.registerReceiver(this, filter);
        }

        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                Slog.e(HwNormalizedAutomaticBrightnessController.TAG, "Invalid input parameter!");
                return;
            }
            String access$300 = HwNormalizedAutomaticBrightnessController.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BroadcastReceiver.onReceive() action:");
            stringBuilder.append(intent.getAction());
            Slog.i(access$300, stringBuilder.toString());
            access$300 = intent.getAction();
            Object obj = -1;
            if (access$300.hashCode() == 798292259 && access$300.equals("android.intent.action.BOOT_COMPLETED")) {
                obj = null;
            }
            if (obj == null && !HwNormalizedAutomaticBrightnessController.this.mHwBrightnessPgSceneDetection.getPGBLListenerRegisted()) {
                HwNormalizedAutomaticBrightnessController.this.mHwBrightnessPgSceneDetection.registerPgBLightSceneListener(HwNormalizedAutomaticBrightnessController.this.mContext);
                if (HwNormalizedAutomaticBrightnessController.DEBUG) {
                    String access$3002 = HwNormalizedAutomaticBrightnessController.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("BOOT_COMPLETED: auto in registerPgBLightSceneChangedListener,=");
                    stringBuilder2.append(HwNormalizedAutomaticBrightnessController.this.mHwBrightnessPgSceneDetection.getPGBLListenerRegisted());
                    Slog.d(access$3002, stringBuilder2.toString());
                }
            }
        }
    }

    private class SensorObserver implements Observer {
        public void update(Observable o, Object arg) {
            long[] data = (long[]) arg;
            int lux = (int) data[0];
            long systemTimeStamp = data[2];
            long sensorTimeStamp = data[3];
            if (HwNormalizedAutomaticBrightnessController.DEBUG && systemTimeStamp - HwNormalizedAutomaticBrightnessController.this.mLightSensorEnableTime < 4000) {
                String access$300 = HwNormalizedAutomaticBrightnessController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ambient lux=");
                stringBuilder.append(lux);
                stringBuilder.append(",systemTimeStamp =");
                stringBuilder.append(systemTimeStamp);
                Slog.d(access$300, stringBuilder.toString());
            }
            if ((!HwServiceFactory.shouldFilteInvalidSensorVal((float) lux) || AutomaticBrightnessController.INT_BRIGHTNESS_COVER_MODE != 0) && !HwNormalizedAutomaticBrightnessController.this.interceptHandleLightSensorEvent(sensorTimeStamp, (float) lux)) {
                HwNormalizedAutomaticBrightnessController.this.handleLightSensorEvent(systemTimeStamp, (float) lux);
                int access$1500 = HwNormalizedAutomaticBrightnessController.this.SENSOR_OPTION;
                HwNormalizedAutomaticBrightnessController.this.mHwDualSensorEventListenerImpl;
                if (access$1500 == 2) {
                    HwNormalizedAutomaticBrightnessController.this.mDualSensorRawAmbient = (int) data[4];
                }
            }
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
        mDeviceActualBrightnessLevel = 0;
        mDeviceActualBrightnessNit = 0;
        mDeviceStandardBrightnessNit = 0;
        mDeviceActualBrightnessLevel = getDeviceActualBrightnessLevel();
        mDeviceActualBrightnessNit = getDeviceActualBrightnessNit();
        mDeviceStandardBrightnessNit = getDeviceStandardBrightnessNit();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DeviceActualLevel=");
            stringBuilder.append(mDeviceActualBrightnessLevel);
            stringBuilder.append(",DeviceActualBrightnessNit=");
            stringBuilder.append(mDeviceActualBrightnessNit);
            stringBuilder.append(",DeviceStandardBrightnessNit=");
            stringBuilder.append(mDeviceStandardBrightnessNit);
            Slog.i(str, stringBuilder.toString());
        }
    }

    private static int getDeviceActualBrightnessLevel() {
        try {
            return ((LightsManager) LocalServices.getService(LightsManager.class)).getLight(0).getDeviceActualBrightnessLevel();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int getDeviceActualBrightnessNit() {
        try {
            return ((LightsManager) LocalServices.getService(LightsManager.class)).getLight(0).getDeviceActualBrightnessNit();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static int getDeviceStandardBrightnessNit() {
        try {
            return ((LightsManager) LocalServices.getService(LightsManager.class)).getLight(0).getDeviceStandardBrightnessNit();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static HwNormalizedSpline createHwNormalizedAutoBrightnessSpline(Context context) {
        try {
            mHwNormalizedScreenAutoBrightnessSpline = HwNormalizedSpline.createHwNormalizedSpline(context, mDeviceActualBrightnessLevel, mDeviceActualBrightnessNit, mDeviceStandardBrightnessNit);
            return mHwNormalizedScreenAutoBrightnessSpline;
        } catch (IllegalArgumentException ex) {
            Slog.e(TAG, "Could not create auto-brightness spline.", ex);
            return null;
        }
    }

    public void updateStateRecognition(boolean usePwrBLightCurve, int appType) {
        if (this.mLightSensorEnabled && !this.mAutoPowerSavingBrighnessLineDisableForDemo) {
            if (this.mGameLevel == 21 && mHwNormalizedScreenAutoBrightnessSpline.getPowerSavingBrighnessLineEnable()) {
                Slog.i(TAG, "GameBrightMode no orig powerSaving");
                return;
            }
            if (this.mData.autoPowerSavingUseManualAnimationTimeEnable) {
                if (mHwNormalizedScreenAutoBrightnessSpline.getPowerSavingModeBrightnessChangeEnable(this.mAmbientLux, usePwrBLightCurve)) {
                    this.mAutoPowerSavingAnimationEnable = true;
                } else {
                    this.mAutoPowerSavingAnimationEnable = false;
                }
            }
            mHwNormalizedScreenAutoBrightnessSpline.setPowerSavingModeEnable(usePwrBLightCurve);
            if (this.mHwBrightnessPowerSavingCurve != null) {
                this.mHwBrightnessPowerSavingCurve.setPowerSavingEnable(usePwrBLightCurve);
            }
            updateAutoBrightness(true);
        }
    }

    private static boolean isDemoVersion() {
        String vendor = SystemProperties.get("ro.hw.vendor", "");
        String country = SystemProperties.get("ro.hw.country", "");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("vendor:");
        stringBuilder.append(vendor);
        stringBuilder.append(",country:");
        stringBuilder.append(country);
        Slog.i(str, stringBuilder.toString());
        return "demo".equalsIgnoreCase(vendor) || "demo".equalsIgnoreCase(country);
    }

    public boolean getAnimationGameChangeEnable() {
        boolean animationEnable = this.mAnimationGameChangeEnable && this.mData.gameModeEnable;
        if (!this.mHwAmbientLuxFilterAlgo.getProximityPositiveEnable()) {
            this.mAnimationGameChangeEnable = false;
        }
        if (DEBUG && animationEnable != this.mAnimationGameChangeEnable) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GameBrightMode set dimming animationEnable=");
            stringBuilder.append(this.mAnimationGameChangeEnable);
            Slog.d(str, stringBuilder.toString());
        }
        return animationEnable;
    }

    public boolean getGameModeEnable() {
        return this.mData.gameModeEnable;
    }

    public boolean getAutoPowerSavingUseManualAnimationTimeEnable() {
        return this.mData.autoPowerSavingUseManualAnimationTimeEnable;
    }

    public boolean getAutoPowerSavingAnimationEnable() {
        return this.mAutoPowerSavingAnimationEnable;
    }

    public void setAutoPowerSavingAnimationEnable(boolean enable) {
        this.mAutoPowerSavingAnimationEnable = enable;
    }

    public HwNormalizedAutomaticBrightnessController(Callbacks callbacks, Looper looper, SensorManager sensorManager, BrightnessMappingStrategy mapper, int lightSensorWarmUpTime, int brightnessMin, int brightnessMax, float dozeScaleFactor, int lightSensorRate, int initialLightSensorRate, long brighteningLightDebounceConfig, long darkeningLightDebounceConfig, boolean resetAmbientLuxAfterWarmUpConfig, HysteresisLevels hysteresisLevels, Context context) {
        SensorManager sensorManager2 = sensorManager;
        Context context2 = context;
        super(callbacks, looper, sensorManager, mapper, lightSensorWarmUpTime, brightnessMin, brightnessMax, dozeScaleFactor, lightSensorRate, initialLightSensorRate, brighteningLightDebounceConfig, darkeningLightDebounceConfig, resetAmbientLuxAfterWarmUpConfig, hysteresisLevels);
        this.mPowerStatus = false;
        this.mScreenStatus = false;
        this.mPowerOnLuxCount = 0;
        this.mPowerOnLuxAbandonCount = 0;
        this.mCurrentUserId = 0;
        this.mCurrentUserChanging = false;
        this.mHwRateMillis = 300;
        this.mHwPrintLogTime = -1;
        this.mHwLastSensorValue = -1;
        this.mHwLastReportedSensorValue = -1;
        this.mHwLastReportedSensorValueTime = -1;
        this.mHwReportValueWhenSensorOnChange = true;
        this.mHwAmbientLightRingBuffer = new HwRingBuffer(10);
        this.mHwAmbientLightRingBufferTrace = new HwRingBuffer(50);
        this.mPowerPolicy = 0;
        this.mPolicyChangeFromDim = false;
        this.mIntervenedAutoBrightnessEnable = false;
        this.mProximity = -1;
        this.mCoverModeFastResponseTimeDelay = 2500;
        this.mCoverStateFast = false;
        this.mIsclosed = false;
        this.mCameraModeEnable = false;
        this.mReadingModeEnable = false;
        this.mCameraModeChangeAnimationEnable = false;
        this.mReadingModeChangeAnimationEnable = false;
        this.mDefaultBrightness = -1.0f;
        this.mBrightnessOutForLog = -1;
        this.mAmbientLuxOffset = -1.0f;
        this.SENSOR_OPTION = -1;
        this.mDualSensorRawAmbient = -1;
        this.mScreenStateReceiver = null;
        this.mScreenBrightnessBeforeAdj = -1;
        this.mDragFinished = true;
        this.mReadingMode = 0;
        this.mEyeProtectionMode = 0;
        this.mCurveLevel = -1;
        this.mSceneLevel = -1;
        this.mGameLevel = -1;
        this.gameModeEnterTimestamp = 0;
        this.gameModeQuitTimestamp = 0;
        this.mGameModeEnableForOffset = false;
        this.mAnimationGameChangeEnable = false;
        this.mAutoPowerSavingBrighnessLineDisableForDemo = false;
        this.mAutoPowerSavingAnimationEnable = false;
        this.mReadingModeObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                HwNormalizedAutomaticBrightnessController.this.mReadingMode = System.getIntForUser(HwNormalizedAutomaticBrightnessController.this.mContext.getContentResolver(), HwNormalizedAutomaticBrightnessController.KEY_READING_MODE_SWITCH, 0, HwNormalizedAutomaticBrightnessController.this.mCurrentUserId);
                String access$300 = HwNormalizedAutomaticBrightnessController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mReadingMode is ");
                stringBuilder.append(HwNormalizedAutomaticBrightnessController.this.mReadingMode);
                stringBuilder.append(", mEyeProtectionMode is ");
                stringBuilder.append(HwNormalizedAutomaticBrightnessController.this.mEyeProtectionMode);
                Slog.i(access$300, stringBuilder.toString());
                if (HwNormalizedAutomaticBrightnessController.this.mEyeProtectionMode != 1 && HwNormalizedAutomaticBrightnessController.this.mEyeProtectionMode != 3) {
                    return;
                }
                if (HwNormalizedAutomaticBrightnessController.this.mReadingMode == 1) {
                    HwNormalizedAutomaticBrightnessController.this.setReadingModeBrightnessLineEnable(true);
                } else {
                    HwNormalizedAutomaticBrightnessController.this.setReadingModeBrightnessLineEnable(false);
                }
            }
        };
        this.mEyeProtectionModeObserver = new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                HwNormalizedAutomaticBrightnessController.this.mEyeProtectionMode = System.getIntForUser(HwNormalizedAutomaticBrightnessController.this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, HwNormalizedAutomaticBrightnessController.this.mCurrentUserId);
                String access$300 = HwNormalizedAutomaticBrightnessController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mEyeProtectionMode is ");
                stringBuilder.append(HwNormalizedAutomaticBrightnessController.this.mEyeProtectionMode);
                stringBuilder.append(", mReadingMode is ");
                stringBuilder.append(HwNormalizedAutomaticBrightnessController.this.mReadingMode);
                Slog.i(access$300, stringBuilder.toString());
                if (HwNormalizedAutomaticBrightnessController.this.mReadingMode != 1) {
                    return;
                }
                if (HwNormalizedAutomaticBrightnessController.this.mEyeProtectionMode == 1 || HwNormalizedAutomaticBrightnessController.this.mEyeProtectionMode == 3) {
                    HwNormalizedAutomaticBrightnessController.this.setReadingModeBrightnessLineEnable(true);
                } else {
                    HwNormalizedAutomaticBrightnessController.this.setReadingModeBrightnessLineEnable(false);
                }
            }
        };
        this.mPowerOnOffStatus = false;
        this.mPowerOnVehicleTimestamp = 0;
        this.mPowerOffVehicleTimestamp = 0;
        this.mVehicleModeQuitEnable = false;
        this.mWakeupCoverBrightnessEnable = false;
        this.mProximitySensorListener = new SensorEventListener() {
            public void onSensorChanged(SensorEvent event) {
                if (HwNormalizedAutomaticBrightnessController.this.mProximitySensorEnabled) {
                    boolean z = false;
                    float distance = event.values[0];
                    HwNormalizedAutomaticBrightnessController.this.mProximityReportTime = SystemClock.uptimeMillis();
                    HwNormalizedAutomaticBrightnessController hwNormalizedAutomaticBrightnessController = HwNormalizedAutomaticBrightnessController.this;
                    if (distance >= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && distance < 5.0f) {
                        z = true;
                    }
                    hwNormalizedAutomaticBrightnessController.mProximityPositive = z;
                    if (HwNormalizedAutomaticBrightnessController.DEBUG) {
                        String access$300 = HwNormalizedAutomaticBrightnessController.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mProximitySensorListener: time = ");
                        stringBuilder.append(HwNormalizedAutomaticBrightnessController.this.mProximityReportTime);
                        stringBuilder.append("; distance = ");
                        stringBuilder.append(distance);
                        Slog.d(access$300, stringBuilder.toString());
                    }
                    if (!HwNormalizedAutomaticBrightnessController.this.mWakeupFromSleep && HwNormalizedAutomaticBrightnessController.this.mProximityReportTime - HwNormalizedAutomaticBrightnessController.this.mLightSensorEnableTime > 500) {
                        HwNormalizedAutomaticBrightnessController.this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessage(2);
                    }
                }
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mPowerOnEnable = false;
        this.mHwAmbientLuxFilterAlgo = new HwAmbientLuxFilterAlgo(lightSensorRate);
        this.mScreenAutoBrightnessSpline = createHwNormalizedAutoBrightnessSpline(context);
        this.mAutoBrightnessProcessThread = new HandlerThread(TAG);
        this.mAutoBrightnessProcessThread.start();
        this.mHwNormalizedAutomaticBrightnessHandler = new HwNormalizedAutomaticBrightnessHandler(this.mAutoBrightnessProcessThread.getLooper());
        this.mHwReportValueWhenSensorOnChange = this.mHwAmbientLuxFilterAlgo.reportValueWhenSensorOnChange();
        this.mProximitySensor = sensorManager2.getDefaultSensor(8);
        this.mAllowLabcUseProximity = this.mHwAmbientLuxFilterAlgo.needToUseProximity();
        if (SystemProperties.getInt("ro.config.hw_eyes_protection", 7) != 0) {
            this.mHwEyeProtectionController = new HwEyeProtectionControllerImpl(context2, this);
        }
        this.mData = HwBrightnessXmlLoader.getData();
        this.mContext = context2;
        this.mDisplayEffectMonitor = DisplayEffectMonitor.getInstance(context);
        if (this.mDisplayEffectMonitor == null) {
            Slog.e(TAG, "getDisplayEffectMonitor failed!");
        }
        sendXmlConfigToMonitor();
        this.mHwBrightnessPgSceneDetection = new HwBrightnessPgSceneDetection(this, this.mData.pgSceneDetectionDarkenDelayTime, this.mData.pgSceneDetectionBrightenDelayTime, this.mContext);
        this.mHwDualSensorEventListenerImpl = HwDualSensorEventListenerImpl.getInstance(sensorManager2, this.mContext);
        this.SENSOR_OPTION = this.mHwDualSensorEventListenerImpl.getModuleSensorOption(TAG);
        if (this.mData.darkAdapterEnable) {
            this.mDarkAdaptDetector = new DarkAdaptDetector(this.mData);
        }
        this.mHwBrightnessMapping = new HwBrightnessMapping(this.mData.brightnessMappingPoints);
        if (this.mData.pgReregisterScene) {
            this.mScreenStateReceiver = new ScreenStateReceiver();
        }
        this.mHwBrightnessPowerSavingCurve = new HwBrightnessPowerSavingCurve(this.mData.manualBrightnessMaxLimit, this.mData.screenBrightnessMinNit, this.mData.screenBrightnessMaxNit);
        if (this.mData.cryogenicEnable) {
            this.mMaxBrightnessFromCryogenicHandler = new Handler();
            this.mMaxBrightnessFromCryogenicDelayedRunnable = new Runnable() {
                public void run() {
                    HwNormalizedAutomaticBrightnessController.this.setMaxBrightnessFromCryogenicDelayed();
                }
            };
        }
        if (this.mData.readingModeEnable) {
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor(KEY_READING_MODE_SWITCH), true, this.mReadingModeObserver, -1);
            this.mReadingMode = System.getIntForUser(this.mContext.getContentResolver(), KEY_READING_MODE_SWITCH, 0, this.mCurrentUserId);
            this.mContext.getContentResolver().registerContentObserver(System.getUriFor(Utils.KEY_EYES_PROTECTION), true, this.mEyeProtectionModeObserver, -1);
            this.mEyeProtectionMode = System.getIntForUser(this.mContext.getContentResolver(), Utils.KEY_EYES_PROTECTION, 0, this.mCurrentUserId);
            Slog.i(TAG, "readingModeEnable enable ...");
        }
        if (this.mData.touchProximityEnable) {
            this.mTouchProximityDetector = new TouchProximityDetector(this.mData);
        }
        if (this.mData.autoPowerSavingBrighnessLineDisableForDemo) {
            this.mAutoPowerSavingBrighnessLineDisableForDemo = isDemoVersion();
        }
    }

    public HwNormalizedAutomaticBrightnessController(Callbacks callbacks, Looper looper, SensorManager sensorManager, BrightnessMappingStrategy mapper, int lightSensorWarmUpTime, int brightnessMin, int brightnessMax, float dozeScaleFactor, Context context) {
        this(callbacks, looper, sensorManager, mapper, lightSensorWarmUpTime, brightnessMin, brightnessMax, dozeScaleFactor, 0, 0, 0, 0, false, null, context);
        if (SystemProperties.getInt("ro.config.hw_eyes_protection", 7) != 0) {
            this.mHwEyeProtectionController = new HwEyeProtectionControllerImpl(context, this);
            return;
        }
        Context context2 = context;
    }

    public void configure(boolean enable, float adjustment, boolean dozing) {
    }

    public void configure(boolean enable, BrightnessConfiguration configuration, float brightness, boolean userChangedBrightness, float adjustment, boolean userChangedAutoBrightnessAdjustment, int displayPolicy) {
        boolean z = false;
        boolean dozing = displayPolicy == 1;
        if (this.mLightSensorEnabled && !enable) {
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(1);
            this.mHwAmbientLuxFilterAlgo.clear();
            if (!this.mHwReportValueWhenSensorOnChange) {
                clearSensorData();
            }
            this.mLastAmbientLightToMonitorTime = 0;
            if (this.mDarkAdaptDetector != null) {
                this.mDarkAdaptDetector.setAutoModeOff();
                this.mDarkAdaptDimmingEnable = false;
            }
        }
        if (!enable) {
            this.mHwAmbientLuxFilterAlgo.setPowerStatus(false);
        }
        super.configure(enable, configuration, brightness, userChangedBrightness, adjustment, userChangedAutoBrightnessAdjustment, displayPolicy);
        if (this.mLightSensorEnabled && -1 == this.mHwPrintLogTime) {
            this.mHwPrintLogTime = this.mLightSensorEnableTime;
        }
        if (!(!enable || dozing || this.mHwBrightnessPgSceneDetection.getPGBLListenerRegisted())) {
            this.mHwBrightnessPgSceneDetection.registerPgBLightSceneListener(this.mContext);
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PowerSaving auto in registerPgBLightSceneChangedListener,=");
                stringBuilder.append(this.mHwBrightnessPgSceneDetection.getPGBLListenerRegisted());
                Slog.d(str, stringBuilder.toString());
            }
        }
        if (this.mAllowLabcUseProximity && enable && !dozing) {
            z = true;
        }
        setProximitySensorEnabled(z);
        if (this.mTouchProximityDetector == null) {
            return;
        }
        if (enable) {
            this.mTouchProximityDetector.enable();
        } else {
            this.mTouchProximityDetector.disable();
        }
    }

    public int getAutomaticScreenBrightness() {
        if (this.mWakeupFromSleep && SystemClock.uptimeMillis() - this.mLightSensorEnableTime < 200) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mWakeupFromSleep= ");
                stringBuilder.append(this.mWakeupFromSleep);
                stringBuilder.append(",currentTime=");
                stringBuilder.append(SystemClock.uptimeMillis());
                stringBuilder.append(",mLightSensorEnableTime=");
                stringBuilder.append(this.mLightSensorEnableTime);
                Slog.d(str, stringBuilder.toString());
            }
            return -1;
        } else if (needToSetBrightnessBaseIntervened()) {
            return super.getAutomaticScreenBrightness();
        } else {
            if (needToSetBrightnessBaseProximity()) {
                return -2;
            }
            return super.getAutomaticScreenBrightness();
        }
    }

    private boolean needToSetBrightnessBaseIntervened() {
        return this.mIntervenedAutoBrightnessEnable && this.mAllowLabcUseProximity;
    }

    public int getAutoBrightnessBaseInOutDoorLimit(int brightness) {
        int tmpBrightnessOut = brightness;
        if (this.mAmbientLux >= ((float) this.mData.outDoorThreshold) || !this.mData.autoModeInOutDoorLimitEnble) {
            return tmpBrightnessOut;
        }
        return tmpBrightnessOut < this.mData.manualBrightnessMaxLimit ? tmpBrightnessOut : this.mData.manualBrightnessMaxLimit;
    }

    public void setPersonalizedBrightnessCurveLevel(int curveLevel) {
        String str;
        StringBuilder stringBuilder;
        if (!(curveLevel == 19 || curveLevel == 18 || curveLevel == 20 || curveLevel == 21)) {
            if (curveLevel != this.mCurveLevel) {
                if (mHwNormalizedScreenAutoBrightnessSpline != null) {
                    mHwNormalizedScreenAutoBrightnessSpline.setPersonalizedBrightnessCurveLevel(curveLevel);
                } else {
                    Slog.e(TAG, "NewCurveMode setPersonalizedBrightnessCurveLevel fail,mHwNormalizedScreenAutoBrightnessSpline==null");
                }
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("NewCurveMode setPersonalizedBrightnessCurveLevel curveLevel=");
                    stringBuilder2.append(curveLevel);
                    Slog.d(str2, stringBuilder2.toString());
                }
            }
            this.mCurveLevel = curveLevel;
        }
        if (!(!this.mData.gameModeEnable || curveLevel == 19 || curveLevel == 18)) {
            int gameLevel;
            if (curveLevel == 21) {
                gameLevel = 21;
            } else {
                gameLevel = 20;
            }
            if (gameLevel != this.mGameLevel) {
                mHwNormalizedScreenAutoBrightnessSpline.setGameCurveLevel(gameLevel);
                this.mAnimationGameChangeEnable = true;
                if (gameLevel == 21) {
                    this.mGameModeEnableForOffset = true;
                    this.mHwAmbientLuxFilterAlgo.setGameModeEnable(true);
                    this.gameModeEnterTimestamp = SystemClock.elapsedRealtime();
                    long timeDelta = this.gameModeEnterTimestamp - this.gameModeQuitTimestamp;
                    if (timeDelta > this.mData.gameModeClearOffsetTime) {
                        float ambientLuxOffset = mHwNormalizedScreenAutoBrightnessSpline.getGameModeAmbientLuxForOffset();
                        if (!this.mData.gameModeOffsetValidAmbientLuxEnable || ambientLuxOffset == -1.0f) {
                            mHwNormalizedScreenAutoBrightnessSpline.clearGameOffsetDelta();
                        } else {
                            mHwNormalizedScreenAutoBrightnessSpline.resetGameModeOffsetFromHumanFactor((int) calculateOffsetMinBrightness(ambientLuxOffset, 1), (int) calculateOffsetMaxBrightness(ambientLuxOffset, 1));
                        }
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("GameBrightMode enterGame timeDelta=");
                        stringBuilder3.append(timeDelta);
                        Slog.i(str3, stringBuilder3.toString());
                    }
                } else {
                    this.mHwAmbientLuxFilterAlgo.setGameModeEnable(false);
                    this.mGameModeEnableForOffset = false;
                    this.gameModeQuitTimestamp = SystemClock.elapsedRealtime();
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("GameBrightMode updateAutoBrightness,gameLevel=");
                stringBuilder.append(gameLevel);
                Slog.i(str, stringBuilder.toString());
                updateAutoBrightness(true);
            }
            this.mGameLevel = gameLevel;
        }
        if (this.mData.vehicleModeEnable && (curveLevel == 19 || curveLevel == 18)) {
            if (mHwNormalizedScreenAutoBrightnessSpline != null) {
                mHwNormalizedScreenAutoBrightnessSpline.setSceneCurveLevel(curveLevel);
                long timDelta;
                if (curveLevel == 19) {
                    this.mVehicleModeQuitEnable = false;
                    timDelta = SystemClock.elapsedRealtime() - this.mPowerOnVehicleTimestamp;
                    if (timDelta > this.mData.vehicleModeEnterTimeForPowerOn) {
                        updateAutoBrightness(true);
                        if (DEBUG) {
                            String str4 = TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("VehicleBrightMode updateAutoBrightness curveLevel=");
                            stringBuilder4.append(curveLevel);
                            stringBuilder4.append(",timDelta=");
                            stringBuilder4.append(timDelta);
                            Slog.i(str4, stringBuilder4.toString());
                        }
                    }
                } else if (curveLevel == 18 && this.mVehicleModeQuitEnable) {
                    timDelta = SystemClock.elapsedRealtime() - this.mPowerOnVehicleTimestamp;
                    boolean vehicleModeBrightnessEnable = mHwNormalizedScreenAutoBrightnessSpline.getVehicleModeBrightnessEnable();
                    if (timDelta < this.mData.vehicleModeQuitTimeForPowerOn && vehicleModeBrightnessEnable) {
                        mHwNormalizedScreenAutoBrightnessSpline.setVehicleModeQuitEnable();
                        String str5 = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("VehicleBrightMode mVehicleModeQuitEnable timDelta=");
                        stringBuilder.append(timDelta);
                        Slog.i(str5, stringBuilder.toString());
                    }
                }
                if (DEBUG && this.mSceneLevel != curveLevel) {
                    str = TAG;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("VehicleBrightMode set curveLevel=");
                    stringBuilder5.append(curveLevel);
                    Slog.d(str, stringBuilder5.toString());
                }
                this.mSceneLevel = curveLevel;
            } else {
                Slog.e(TAG, "VehicleBrightMode setSceneCurveLevel fail,mHwNormalizedScreenAutoBrightnessSpline==null");
            }
        }
    }

    public void updateNewBrightnessCurveTmp() {
        if (mHwNormalizedScreenAutoBrightnessSpline != null) {
            mHwNormalizedScreenAutoBrightnessSpline.updateNewBrightnessCurveTmp();
            sendPersonalizedCurveAndParamToMonitor(mHwNormalizedScreenAutoBrightnessSpline.getPersonalizedDefaultCurve(), mHwNormalizedScreenAutoBrightnessSpline.getPersonalizedAlgoParam());
            return;
        }
        Slog.e(TAG, "NewCurveMode updateNewBrightnessCurveTmp fail,mHwNormalizedScreenAutoBrightnessSpline==null");
    }

    public void updateNewBrightnessCurve() {
        if (mHwNormalizedScreenAutoBrightnessSpline != null) {
            mHwNormalizedScreenAutoBrightnessSpline.updateNewBrightnessCurve();
        } else {
            Slog.e(TAG, "NewCurveMode updateNewBrightnessCurve fail,mHwNormalizedScreenAutoBrightnessSpline==null");
        }
    }

    public List<PointF> getCurrentDefaultNewCurveLine() {
        List<PointF> brighntessList = new ArrayList();
        if (mHwNormalizedScreenAutoBrightnessSpline != null) {
            return mHwNormalizedScreenAutoBrightnessSpline.getCurrentDefaultNewCurveLine();
        }
        return brighntessList;
    }

    public void updateIntervenedAutoBrightness(int brightness) {
        this.mAutoBrightnessOut = (float) brightness;
        this.mIntervenedAutoBrightnessEnable = true;
        if (this.mData.cryogenicEnable) {
            this.mMaxBrightnessSetByCryogenicBypass = true;
        }
        if (this.mData.manualMode) {
            String str;
            StringBuilder stringBuilder;
            if (this.mDragFinished) {
                this.mScreenBrightnessBeforeAdj = getAutomaticScreenBrightness();
                this.mDragFinished = false;
            }
            this.mDefaultBrightness = mHwNormalizedScreenAutoBrightnessSpline.getCurrentDefaultBrightnessNoOffset();
            float lux = mHwNormalizedScreenAutoBrightnessSpline.getCurrentAmbientLuxForBrightness();
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwAutoBrightnessIn=");
                stringBuilder.append(brightness);
                stringBuilder.append(",defaultBrightness=");
                stringBuilder.append(this.mDefaultBrightness);
                stringBuilder.append(",lux=");
                stringBuilder.append(lux);
                Slog.i(str, stringBuilder.toString());
            }
            if (this.mAutoBrightnessOut >= ((float) this.mData.manualBrightnessMaxLimit)) {
                if (lux > ((float) this.mData.outDoorThreshold)) {
                    int autoBrightnessOutTmp = ((int) this.mAutoBrightnessOut) < this.mData.manualBrightnessMaxLimit ? (int) this.mAutoBrightnessOut : this.mData.manualBrightnessMaxLimit;
                    this.mAutoBrightnessOut = autoBrightnessOutTmp > ((int) this.mDefaultBrightness) ? (float) autoBrightnessOutTmp : (float) ((int) this.mDefaultBrightness);
                } else {
                    this.mAutoBrightnessOut = this.mAutoBrightnessOut < ((float) this.mData.manualBrightnessMaxLimit) ? this.mAutoBrightnessOut : (float) this.mData.manualBrightnessMaxLimit;
                }
            }
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("HwAutoBrightnessOut=");
                stringBuilder.append(this.mAutoBrightnessOut);
                Slog.i(str, stringBuilder.toString());
            }
            super.updateIntervenedAutoBrightness((int) this.mAutoBrightnessOut);
            return;
        }
        super.updateIntervenedAutoBrightness((int) this.mAutoBrightnessOut);
    }

    private int getSensorData() {
        synchronized (this.mHwAmbientLightRingBuffer) {
            long time = SystemClock.uptimeMillis();
            int N = this.mHwAmbientLightRingBuffer.size();
            int sum;
            if (N > 0) {
                int i;
                sum = 0;
                for (i = N - 1; i >= 0; i--) {
                    sum = (int) (((float) sum) + this.mHwAmbientLightRingBuffer.getLux(i));
                }
                i = sum / N;
                if (i >= 0) {
                    this.mHwLastSensorValue = i;
                }
                this.mHwAmbientLightRingBuffer.clear();
                if (time - this.mHwPrintLogTime > 4000) {
                    N = this.mHwAmbientLightRingBufferTrace.size();
                    if (DEBUG) {
                        Slog.d("lux trace:", this.mHwAmbientLightRingBufferTrace.toString(N));
                    }
                    this.mHwAmbientLightRingBufferTrace.clear();
                    this.mHwPrintLogTime = time;
                }
                int i2 = this.mHwLastSensorValue;
                return i2;
            } else if (time - this.mHwLastReportedSensorValueTime < 400) {
                sum = this.mHwLastSensorValue;
                return sum;
            } else {
                sum = this.mHwLastReportedSensorValue;
                return sum;
            }
        }
    }

    private void clearSensorData() {
        synchronized (this.mHwAmbientLightRingBuffer) {
            this.mHwAmbientLightRingBuffer.clear();
            int N = this.mHwAmbientLightRingBufferTrace.size();
            if (DEBUG) {
                Slog.d("lux trace:", this.mHwAmbientLightRingBufferTrace.toString(N));
            }
            this.mHwAmbientLightRingBufferTrace.clear();
            this.mHwLastReportedSensorValueTime = -1;
            this.mHwLastReportedSensorValue = -1;
            this.mHwLastSensorValue = -1;
            this.mHwPrintLogTime = -1;
        }
    }

    private float getTouchProximityProcessedLux(boolean isFirstLux, float lux) {
        if (this.mTouchProximityDetector == null) {
            return lux;
        }
        if (!isFirstLux) {
            boolean needUseLastLux = !this.mTouchProximityDetector.isCurrentLuxValid() && lux < this.mAmbientLuxLast;
            if (needUseLastLux) {
                lux = this.mAmbientLuxLast;
            }
        }
        this.mTouchProximityDetector.startNextLux();
        this.mAmbientLuxLast = lux;
        return lux;
    }

    private void reportLightSensorEventToAlgo(long time, float lux) {
        this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(1);
        lux = getTouchProximityProcessedLux(this.mAmbientLuxValid ^ true, lux);
        if (!this.mAmbientLuxValid) {
            String str;
            StringBuilder stringBuilder;
            this.mWakeupFromSleep = false;
            this.mAmbientLuxValid = true;
            this.mHwAmbientLuxFilterAlgo.isFirstAmbientLux(true);
            if (this.mData.dayModeAlgoEnable || this.mData.offsetResetEnable) {
                this.mHwAmbientLuxFilterAlgo.setAutoModeEnableFirstLux(lux);
                this.mHwAmbientLuxFilterAlgo.setDayModeEnable();
                if (this.mData.dayModeAlgoEnable) {
                    mHwNormalizedScreenAutoBrightnessSpline.setDayModeEnable(this.mHwAmbientLuxFilterAlgo.getDayModeEnable());
                }
                if (this.mData.offsetResetEnable) {
                    this.mAmbientLuxOffset = mHwNormalizedScreenAutoBrightnessSpline.getCurrentAmbientLuxForOffset();
                    if (this.mAmbientLuxOffset != -1.0f) {
                        int mOffsetScreenBrightnessMinByAmbientLux = (int) calculateOffsetMinBrightness(this.mAmbientLuxOffset, 0);
                        int mOffsetScreenBrightnessMaxByAmbientLux = (int) calculateOffsetMaxBrightness(this.mAmbientLuxOffset, 0);
                        if (mHwNormalizedScreenAutoBrightnessSpline.getPersonalizedBrightnessCurveEnable()) {
                            float defaultBrightness = mHwNormalizedScreenAutoBrightnessSpline.getDefaultBrightness(this.mAmbientLuxOffset);
                            float currentBrightness = mHwNormalizedScreenAutoBrightnessSpline.getNewCurrentBrightness(this.mAmbientLuxOffset);
                            mOffsetScreenBrightnessMinByAmbientLux += ((int) currentBrightness) - ((int) defaultBrightness);
                            mOffsetScreenBrightnessMaxByAmbientLux += ((int) currentBrightness) - ((int) defaultBrightness);
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("NewCurveMode new offset MinByAmbientLux=");
                            stringBuilder2.append(mOffsetScreenBrightnessMinByAmbientLux);
                            stringBuilder2.append(",maxByAmbientLux");
                            stringBuilder2.append(mOffsetScreenBrightnessMaxByAmbientLux);
                            Slog.i(str2, stringBuilder2.toString());
                        }
                        mHwNormalizedScreenAutoBrightnessSpline.reSetOffsetFromHumanFactor(this.mHwAmbientLuxFilterAlgo.getOffsetResetEnable(), mOffsetScreenBrightnessMinByAmbientLux, mOffsetScreenBrightnessMaxByAmbientLux);
                    }
                    unlockDarkAdaptLine();
                }
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DayMode:dayModeEnable=");
                    stringBuilder.append(this.mHwAmbientLuxFilterAlgo.getDayModeEnable());
                    stringBuilder.append(",offsetEnable=");
                    stringBuilder.append(this.mHwAmbientLuxFilterAlgo.getOffsetResetEnable());
                    Slog.d(str, stringBuilder.toString());
                }
            }
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mAmbientLuxValid=");
                stringBuilder.append(this.mAmbientLuxValid);
                stringBuilder.append(",mWakeupFromSleep= ");
                stringBuilder.append(this.mWakeupFromSleep);
                Slog.d(str, stringBuilder.toString());
            }
        }
        this.mHwAmbientLuxFilterAlgo.handleLightSensorEvent(time, lux);
        this.mAmbientLux = this.mHwAmbientLuxFilterAlgo.getCurrentAmbientLux();
        boolean isDarkAdaptStateChanged = handleDarkAdaptDetector(lux);
        if (this.mHwAmbientLuxFilterAlgo.needToUpdateBrightness() || isDarkAdaptStateChanged) {
            if (DEBUG) {
                String str3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("need to update brightness: mAmbientLux=");
                stringBuilder3.append(this.mAmbientLux);
                Slog.d(str3, stringBuilder3.toString());
            }
            this.mHwAmbientLuxFilterAlgo.brightnessUpdated();
            updateAutoBrightness(true);
        }
        if (!this.mHwReportValueWhenSensorOnChange) {
            this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessageDelayed(1, (long) this.mHwRateMillis);
        }
        if (this.mProximityPositive) {
            this.mLastAmbientLightToMonitorTime = 0;
        } else {
            sendAmbientLightToMonitor(time, lux);
        }
        sendDefaultBrightnessToMonitor();
    }

    private float calculateOffsetMinBrightness(float amLux, int mode) {
        if (amLux < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            Slog.w(TAG, "amlux<0, return offsetMIN");
            return 4.0f;
        }
        float offsetMinBrightness = 4.0f;
        HwXmlAmPoint temp1 = null;
        List<HwXmlAmPoint> brightnessPoints;
        if (mode == 1) {
            brightnessPoints = this.mData.gameModeAmbientLuxValidBrightnessPoints;
        } else {
            brightnessPoints = this.mData.ambientLuxValidBrightnessPoints;
        }
        for (HwXmlAmPoint temp : brightnessPoints) {
            if (temp1 == null) {
                temp1 = temp;
            }
            if (amLux < temp.x) {
                HwXmlAmPoint temp2 = temp;
                if (temp2.x <= temp1.x) {
                    offsetMinBrightness = 4.0f;
                    if (DEBUG) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("OffsetMinBrightness_temp1.x <= temp2.x,x");
                        stringBuilder.append(temp.x);
                        stringBuilder.append(", y = ");
                        stringBuilder.append(temp.y);
                        Slog.i(str, stringBuilder.toString());
                    }
                } else {
                    offsetMinBrightness = (((temp2.y - temp1.y) / (temp2.x - temp1.x)) * (amLux - temp1.x)) + temp1.y;
                }
                return offsetMinBrightness;
            }
            temp1 = temp;
            offsetMinBrightness = temp1.y;
        }
        return offsetMinBrightness;
    }

    private float calculateOffsetMaxBrightness(float amLux, int mode) {
        if (amLux < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
            Slog.w(TAG, "amlux<0, return offsetMAX");
            return 255.0f;
        }
        float offsetMaxBrightness = 255.0f;
        HwXmlAmPoint temp1 = null;
        List<HwXmlAmPoint> brightnessPoints;
        if (mode == 1) {
            brightnessPoints = this.mData.gameModeAmbientLuxValidBrightnessPoints;
        } else {
            brightnessPoints = this.mData.ambientLuxValidBrightnessPoints;
        }
        for (HwXmlAmPoint temp : brightnessPoints) {
            if (temp1 == null) {
                temp1 = temp;
            }
            if (amLux < temp.x) {
                HwXmlAmPoint temp2 = temp;
                if (temp2.x <= temp1.x) {
                    offsetMaxBrightness = 255.0f;
                    if (DEBUG) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("OffsetMaxBrightness_temp1.x <= temp2.x,x");
                        stringBuilder.append(temp.x);
                        stringBuilder.append(", z = ");
                        stringBuilder.append(temp.z);
                        Slog.i(str, stringBuilder.toString());
                    }
                } else {
                    offsetMaxBrightness = (((temp2.z - temp1.z) / (temp2.x - temp1.x)) * (amLux - temp1.x)) + temp1.z;
                }
                return offsetMaxBrightness;
            }
            temp1 = temp;
            offsetMaxBrightness = temp1.z;
        }
        return offsetMaxBrightness;
    }

    private void sendAmbientLightToMonitor(long time, float lux) {
        if (this.mDisplayEffectMonitor != null) {
            if (this.mLastAmbientLightToMonitorTime == 0 || time <= this.mLastAmbientLightToMonitorTime) {
                this.mLastAmbientLightToMonitorTime = time;
                return;
            }
            int durationInMs = (int) (time - this.mLastAmbientLightToMonitorTime);
            this.mLastAmbientLightToMonitorTime = time;
            ArrayMap<String, Object> params = new ArrayMap();
            params.put(MonitorModule.PARAM_TYPE, "ambientLightCollection");
            params.put("lightValue", Integer.valueOf((int) lux));
            params.put("durationInMs", Integer.valueOf(durationInMs));
            params.put("brightnessMode", "AUTO");
            if (this.mDualSensorRawAmbient >= 0) {
                params.put("rawLightValue", Integer.valueOf(this.mDualSensorRawAmbient));
            }
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    private void sendDefaultBrightnessToMonitor() {
        if (this.mDisplayEffectMonitor != null) {
            int defaultBrightness = (int) mHwNormalizedScreenAutoBrightnessSpline.getCurrentDefaultBrightnessNoOffset();
            if (this.mLastDefaultBrightness != defaultBrightness) {
                this.mLastDefaultBrightness = defaultBrightness;
                int lightValue = (int) mHwNormalizedScreenAutoBrightnessSpline.getCurrentAmbientLuxForBrightness();
                ArrayMap<String, Object> params = new ArrayMap();
                params.put(MonitorModule.PARAM_TYPE, "algoDefaultBrightness");
                params.put("lightValue", Integer.valueOf(lightValue));
                params.put("brightness", Integer.valueOf(defaultBrightness));
                params.put("brightnessMode", "AUTO");
                this.mDisplayEffectMonitor.sendMonitorParam(params);
            }
        }
    }

    private void sendPowerStateToMonitor(int policy) {
        if (this.mDisplayEffectMonitor != null) {
            String newStateName;
            switch (policy) {
                case 0:
                case 1:
                    newStateName = AwareJobSchedulerConstants.BAR_STATUS_OFF;
                    break;
                case 2:
                    newStateName = "DIM";
                    break;
                case 3:
                    newStateName = AwareJobSchedulerConstants.BAR_STATUS_ON;
                    break;
                case 4:
                    newStateName = "VR";
                    break;
                default:
                    newStateName = AwareJobSchedulerConstants.BAR_STATUS_OFF;
                    break;
            }
            if (this.mPowerStateNameForMonitor != newStateName) {
                this.mPowerStateNameForMonitor = newStateName;
                ArrayMap<String, Object> params = new ArrayMap();
                params.put(MonitorModule.PARAM_TYPE, "powerStateUpdate");
                params.put("powerState", newStateName);
                this.mDisplayEffectMonitor.sendMonitorParam(params);
            }
        }
    }

    private void sendXmlConfigToMonitor() {
        if (this.mDisplayEffectMonitor != null) {
            ArrayMap<String, Object> params = new ArrayMap();
            params.put(MonitorModule.PARAM_TYPE, "xmlConfig");
            params.put("enable", Boolean.valueOf(this.mData.monitorEnable));
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0034, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void sendPersonalizedCurveAndParamToMonitor(List<Short> curve, List<Float> algoParam) {
        if (this.mDisplayEffectMonitor != null && curve != null && !curve.isEmpty() && algoParam != null && !algoParam.isEmpty()) {
            ArrayMap<String, Object> params = new ArrayMap();
            params.put(MonitorModule.PARAM_TYPE, "personalizedCurveAndParam");
            params.put("personalizedCurve", curve);
            params.put("personalizedParam", algoParam);
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    protected void setBrightnessLimitedByThermal(boolean isLimited) {
        sendThermalLimitToMonitor(isLimited);
    }

    private void sendThermalLimitToMonitor(boolean isLimited) {
        if (this.mDisplayEffectMonitor != null && this.mIsBrightnessLimitedByThermal != isLimited) {
            this.mIsBrightnessLimitedByThermal = isLimited;
            ArrayMap<String, Object> params = new ArrayMap();
            params.put(MonitorModule.PARAM_TYPE, "thermalLimit");
            params.put("isLimited", Boolean.valueOf(isLimited));
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    protected void handleLightSensorEvent(long time, float lux) {
        if (mHwNormalizedScreenAutoBrightnessSpline.getCalibrationTestEable()) {
            this.mSetbrightnessImmediateEnable = true;
            getLightSensorFromDB();
            return;
        }
        this.mSetbrightnessImmediateEnable = false;
        if (!this.mAmbientLuxValid || this.mHwReportValueWhenSensorOnChange) {
            reportLightSensorEventToAlgo(time, lux);
            if (!this.mHwReportValueWhenSensorOnChange) {
                synchronized (mLock) {
                    this.mHwLastReportedSensorValue = (int) lux;
                    this.mHwLastReportedSensorValueTime = time;
                }
                return;
            }
            return;
        }
        synchronized (this.mHwAmbientLightRingBuffer) {
            this.mHwAmbientLightRingBuffer.push(time, lux);
            this.mHwAmbientLightRingBufferTrace.push(time, lux);
            this.mHwLastReportedSensorValue = (int) lux;
            this.mHwLastReportedSensorValueTime = time;
        }
    }

    protected void getLightSensorFromDB() {
        this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(1);
        this.mAmbientLuxValid = true;
        float ambientLux = mHwNormalizedScreenAutoBrightnessSpline.getAmbientValueFromDB();
        if (((int) (ambientLux * 10.0f)) != ((int) (this.mAmbientLux * 10.0f))) {
            this.mAmbientLux = ambientLux;
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setAmbientLuxDB=");
                stringBuilder.append(this.mAmbientLux);
                Slog.d(str, stringBuilder.toString());
            }
            updateAutoBrightness(true);
        }
    }

    private void setMaxBrightnessFromCryogenicDelayed() {
        if (DEBUG) {
            Slog.d(TAG, "mMaxBrightnessSetByCryogenicBypassDelayed=false");
        }
        this.mMaxBrightnessSetByCryogenicBypassDelayed = false;
        if (this.mMaxBrightnessSetByCryogenic < 255 && this.mLightSensorEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cryogenic set mMaxBrightnessSetByCryogenic=");
            stringBuilder.append(this.mMaxBrightnessSetByCryogenic);
            Slog.i(str, stringBuilder.toString());
            this.mCallbacks.updateBrightness();
        }
    }

    public void setPowerStatus(boolean powerStatus) {
        String str;
        StringBuilder stringBuilder;
        if (this.mData.cryogenicEnable) {
            if (powerStatus) {
                this.mPowerOnTimestamp = SystemClock.elapsedRealtime();
                if (this.mPowerOnTimestamp - this.mPowerOffTimestamp > this.mData.cryogenicActiveScreenOffIntervalInMillis) {
                    if (DEBUG) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("mPowerOnTimestamp - mPowerOffTimestamp=");
                        stringBuilder.append(this.mPowerOnTimestamp - this.mPowerOffTimestamp);
                        stringBuilder.append(", apply Cryogenic brightness limit(");
                        stringBuilder.append(this.mMaxBrightnessSetByCryogenic);
                        stringBuilder.append(")!");
                        Slog.d(str, stringBuilder.toString());
                    }
                    this.mMaxBrightnessSetByCryogenicBypass = false;
                }
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mMaxBrightnessSetByCryogenicBypass=");
                    stringBuilder.append(this.mMaxBrightnessSetByCryogenicBypass);
                    stringBuilder.append(" mMaxBrightnessSetByCryogenicBypassDelayed=");
                    stringBuilder.append(this.mMaxBrightnessSetByCryogenicBypassDelayed);
                    Slog.d(str, stringBuilder.toString());
                }
                if (this.mMaxBrightnessSetByCryogenic == 255) {
                    this.mMaxBrightnessSetByCryogenicBypassDelayed = true;
                    if (DEBUG) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("No Cryogenic brightness limit! Then it should be active ");
                        stringBuilder.append(((double) this.mData.cryogenicLagTimeInMillis) / 60000.0d);
                        stringBuilder.append("min later!");
                        Slog.d(str, stringBuilder.toString());
                    }
                    if (!(this.mMaxBrightnessFromCryogenicHandler == null || this.mMaxBrightnessFromCryogenicDelayedRunnable == null)) {
                        this.mMaxBrightnessFromCryogenicHandler.removeCallbacks(this.mMaxBrightnessFromCryogenicDelayedRunnable);
                        this.mMaxBrightnessFromCryogenicHandler.postDelayed(this.mMaxBrightnessFromCryogenicDelayedRunnable, this.mData.cryogenicLagTimeInMillis);
                    }
                }
            } else {
                this.mPowerOffTimestamp = SystemClock.elapsedRealtime();
                if (this.mCryogenicProcessor != null) {
                    this.mCryogenicProcessor.onScreenOff();
                }
            }
        }
        if (DEBUG && this.mPowerStatus != powerStatus) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("set power status:mPowerStatus=");
            stringBuilder.append(this.mPowerStatus);
            stringBuilder.append(",powerStatus=");
            stringBuilder.append(powerStatus);
            Slog.d(str, stringBuilder.toString());
        }
        if (this.mPowerStatus != powerStatus && powerStatus && this.mData.coverModeDayEnable) {
            updateCoverModeDayBrightness();
        }
        if (this.mPowerOnOffStatus != powerStatus) {
            if (!powerStatus) {
                boolean enableTmp = mHwNormalizedScreenAutoBrightnessSpline.getNewCurveEableTmp();
                boolean setCurveOk = mHwNormalizedScreenAutoBrightnessSpline.setNewCurveEnable(enableTmp);
                if (enableTmp) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("NewCurveMode poweroff updateNewCurve(tem--real),enableTmp=");
                    stringBuilder2.append(enableTmp);
                    stringBuilder2.append(",powerStatus=");
                    stringBuilder2.append(powerStatus);
                    Slog.d(str2, stringBuilder2.toString());
                }
            }
            mHwNormalizedScreenAutoBrightnessSpline.setPowerStatus(powerStatus);
            if (this.mData.vehicleModeEnable) {
                if (powerStatus) {
                    this.mPowerOnVehicleTimestamp = SystemClock.elapsedRealtime();
                    if (this.mPowerOnVehicleTimestamp - this.mPowerOffVehicleTimestamp > this.mData.vehicleModeDisableTimeMillis) {
                        this.mVehicleModeQuitEnable = true;
                        if (DEBUG) {
                            str = TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("VehicleBrightMode mVehicleModeQuitEnable OnOfftime=");
                            stringBuilder3.append(this.mPowerOnVehicleTimestamp - this.mPowerOffVehicleTimestamp);
                            Slog.d(str, stringBuilder3.toString());
                        }
                    }
                } else {
                    this.mVehicleModeQuitEnable = false;
                    this.mPowerOffVehicleTimestamp = SystemClock.elapsedRealtime();
                }
            }
        }
        this.mPowerOnOffStatus = powerStatus;
        this.mPowerStatus = powerStatus;
        this.mScreenStatus = powerStatus;
        this.mWakeupFromSleep = powerStatus;
        this.mHwAmbientLuxFilterAlgo.setPowerStatus(powerStatus);
        if (!this.mPowerStatus) {
            this.mPowerOnLuxAbandonCount = 0;
            this.mPowerOnLuxCount = 0;
            this.mWakeupCoverBrightnessEnable = false;
        }
        if (this.mHwEyeProtectionController != null) {
            this.mHwEyeProtectionController.onScreenStateChanged(powerStatus);
        }
    }

    private void updateCoverModeDayBrightness() {
        boolean isClosed = HwServiceFactory.isCoverClosed();
        int brightnessMode = System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 1, this.mCurrentUserId);
        if (isClosed && brightnessMode == 1) {
            int openHour = Calendar.getInstance().get(11);
            if (openHour >= this.mData.converModeDayBeginTime && openHour < this.mData.coverModeDayEndTime) {
                setCoverModeDayEnable(true);
                this.mWakeupCoverBrightnessEnable = true;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("LabcCoverMode,isClosed=");
                stringBuilder.append(isClosed);
                stringBuilder.append(",openHour=");
                stringBuilder.append(openHour);
                stringBuilder.append(",coverModeBrightness=");
                stringBuilder.append(this.mData.coverModeDayBrightness);
                Slog.i(str, stringBuilder.toString());
            }
        }
    }

    protected boolean interceptHandleLightSensorEvent(long time, float lux) {
        String str;
        StringBuilder stringBuilder;
        if (SystemClock.uptimeMillis() < ((long) this.mLightSensorWarmUpTimeConfig) + this.mLightSensorEnableTime) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("sensor not ready yet at time ");
            stringBuilder.append(time);
            Slog.i(str, stringBuilder.toString());
            return true;
        } else if (this.mCurrentUserChanging) {
            return true;
        } else {
            if (this.mPowerStatus) {
                this.mPowerOnLuxAbandonCount++;
                this.mPowerOnLuxCount++;
                if (this.mPowerOnLuxCount > getpowerOnFastResponseLuxNum()) {
                    if (DEBUG) {
                        str = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("set power status:false,powerOnFastResponseLuxNum=");
                        stringBuilder2.append(getpowerOnFastResponseLuxNum());
                        Slog.d(str, stringBuilder2.toString());
                    }
                    this.mPowerStatus = false;
                    this.mHwAmbientLuxFilterAlgo.setPowerStatus(false);
                }
                if (this.mLightSensorEnableElapsedTimeNanos - time > 0) {
                    if (DEBUG) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("abandon handleLightSensorEvent:");
                        stringBuilder.append(lux);
                        Slog.d(str, stringBuilder.toString());
                    }
                    return true;
                }
            }
            return false;
        }
    }

    public void updateAutoBrightnessAdjustFactor(float adjustFactor) {
        this.mIntervenedAutoBrightnessEnable = false;
        float lux = this.mHwAmbientLuxFilterAlgo.getOffsetValidAmbientLux();
        if (this.mData.offsetValidAmbientLuxEnable) {
            float luxCurrent = this.mHwAmbientLuxFilterAlgo.getCurrentAmbientLux();
            boolean proximityPositiveEnable = this.mHwAmbientLuxFilterAlgo.getProximityPositiveEnable();
            float positionBrightness = adjustFactor * 255.0f;
            float defautBrightness = mHwNormalizedScreenAutoBrightnessSpline.getCurrentDefaultBrightnessNoOffset();
            if (proximityPositiveEnable && ((int) positionBrightness) > ((int) defautBrightness)) {
                lux = luxCurrent;
            }
            this.mHwAmbientLuxFilterAlgo.setCurrentAmbientLux(lux);
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AdjustPositionBrightness=");
            stringBuilder.append((int) (adjustFactor * 255.0f));
            stringBuilder.append(",lux=");
            stringBuilder.append(lux);
            Slog.i(str, stringBuilder.toString());
        }
        if (this.mGameModeEnableForOffset) {
            mHwNormalizedScreenAutoBrightnessSpline.updateLevelGameWithLux(255.0f * adjustFactor, lux);
        } else {
            mHwNormalizedScreenAutoBrightnessSpline.updateLevelWithLux(255.0f * adjustFactor, lux);
        }
    }

    protected boolean setAutoBrightnessAdjustment(float adjustment) {
        return false;
    }

    public void saveOffsetAlgorithmParas() {
    }

    private void handleUpdateAmbientLuxMsg() {
        reportLightSensorEventToAlgo(SystemClock.uptimeMillis(), (float) getSensorData());
    }

    protected void updateBrightnessIfNoAmbientLuxReported() {
        if (this.mWakeupFromSleep) {
            this.mWakeupFromSleep = false;
            this.mCallbacks.updateBrightness();
            this.mFirstAutoBrightness = false;
            this.mUpdateAutoBrightnessCount++;
            if (DEBUG) {
                Slog.d(TAG, "sensor doesn't report lux in 200ms");
            }
        }
    }

    public void updateCurrentUserId(int userId) {
        if (userId != this.mCurrentUserId) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("user change from  ");
                stringBuilder.append(this.mCurrentUserId);
                stringBuilder.append(" into ");
                stringBuilder.append(userId);
                Slog.d(str, stringBuilder.toString());
            }
            this.mCurrentUserId = userId;
            this.mCurrentUserChanging = true;
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(1);
            this.mAmbientLuxValid = false;
            this.mHwAmbientLuxFilterAlgo.clear();
            mHwNormalizedScreenAutoBrightnessSpline.updateCurrentUserId(userId);
            this.mCurrentUserChanging = false;
        }
    }

    private void setProximitySensorEnabled(boolean enable) {
        if (enable) {
            if (!this.mProximitySensorEnabled) {
                this.mProximitySensorEnabled = true;
                getSensorManager().registerListener(this.mProximitySensorListener, this.mProximitySensor, 3, this.mHwNormalizedAutomaticBrightnessHandler);
                if (DEBUG) {
                    Slog.d(TAG, "open proximity sensor");
                }
            }
        } else if (this.mProximitySensorEnabled) {
            this.mProximitySensorEnabled = false;
            this.mProximity = -1;
            getSensorManager().unregisterListener(this.mProximitySensorListener);
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(3);
            this.mCallbacks.updateProximityState(false);
            if (DEBUG) {
                Slog.d(TAG, "close proximity sensor");
            }
        }
    }

    private void processProximityState() {
        int proximity = this.mHwAmbientLuxFilterAlgo.getProximityState();
        if (this.mProximity != proximity) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mProximity=");
                stringBuilder.append(this.mProximity);
                stringBuilder.append(",proximity=");
                stringBuilder.append(proximity);
                Slog.d(str, stringBuilder.toString());
            }
            if (this.mProximity == 1 && proximity == 0) {
                this.mFirstBrightnessAfterProximityNegative = true;
            }
            this.mProximity = proximity;
            if (this.mProximity != -1) {
                if (this.mProximity == 1) {
                    this.mCallbacks.updateProximityState(true);
                } else if (this.mProximity == 0) {
                    this.mCallbacks.updateProximityState(false);
                }
            }
        }
    }

    private void handleProximitySensorEvent() {
        this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(3);
        this.mHwAmbientLuxFilterAlgo.handleProximitySensorEvent(this.mProximityReportTime, this.mProximityPositive);
        processProximityState();
        if (this.mHwAmbientLuxFilterAlgo.needToSendProximityDebounceMsg()) {
            this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessageAtTime(3, this.mHwAmbientLuxFilterAlgo.getPendingProximityDebounceTime());
        }
    }

    private void debounceProximitySensor() {
        if (DEBUG) {
            Slog.d(TAG, "process MSG_PROXIMITY_SENSOR_DEBOUNCED");
        }
        this.mHwAmbientLuxFilterAlgo.debounceProximitySensor();
        processProximityState();
    }

    public void updatePowerPolicy(int policy) {
        boolean powerOnEnable = wantScreenOn(policy);
        if (powerOnEnable != this.mPowerOnEnable) {
            setPowerStatus(powerOnEnable);
        }
        this.mPowerOnEnable = powerOnEnable;
        if (this.mPowerPolicy != 2 || policy == 2) {
            this.mPolicyChangeFromDim = false;
        } else {
            this.mPolicyChangeFromDim = true;
        }
        this.mPowerPolicy = policy;
        sendPowerStateToMonitor(policy);
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

    private boolean needToSetBrightnessBaseProximity() {
        boolean z = true;
        if (this.mProximity != 1 || this.mBrightnessEnlarge || this.mUpdateAutoBrightnessCount <= 1 || this.mPowerPolicy == 2 || this.mPolicyChangeFromDim) {
            z = false;
        }
        boolean needToSet = z;
        if (DEBUG && needToSet) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mProximity= ");
            stringBuilder.append(this.mProximity);
            stringBuilder.append(",mBrightnessEnlarge=");
            stringBuilder.append(this.mBrightnessEnlarge);
            stringBuilder.append(",mUpdateAutoBrightnessCount=");
            stringBuilder.append(this.mUpdateAutoBrightnessCount);
            stringBuilder.append(",mPowerPolicy=");
            stringBuilder.append(this.mPowerPolicy);
            stringBuilder.append(",mPolicyChangeFromDim=");
            stringBuilder.append(this.mPolicyChangeFromDim);
            Slog.d(str, stringBuilder.toString());
        }
        return needToSet;
    }

    public void setSplineEyeProtectionControlFlag(boolean flag) {
        if (mHwNormalizedScreenAutoBrightnessSpline != null) {
            mHwNormalizedScreenAutoBrightnessSpline.setEyeProtectionControlFlag(flag);
        }
    }

    public void setReadingModeBrightnessLineEnable(boolean readingMode) {
        if (mHwNormalizedScreenAutoBrightnessSpline != null && getReadingModeBrightnessLineEnable()) {
            mHwNormalizedScreenAutoBrightnessSpline.setReadingModeEnable(readingMode);
            if ((readingMode || !this.mReadingModeEnable) && (!readingMode || this.mReadingModeEnable)) {
                this.mReadingModeChangeAnimationEnable = false;
            } else {
                this.mReadingModeChangeAnimationEnable = true;
                updateAutoBrightness(true);
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setReadingModeControlFlag: ");
                stringBuilder.append(readingMode);
                stringBuilder.append(", mReadingModeChangeAnimationEnable: ");
                stringBuilder.append(this.mReadingModeChangeAnimationEnable);
                Slog.d(str, stringBuilder.toString());
            }
            this.mReadingModeEnable = readingMode;
        }
    }

    public boolean getPowerStatus() {
        return this.mPowerStatus;
    }

    public boolean getScreenStatus() {
        return this.mScreenStatus;
    }

    public void setCoverModeStatus(boolean isclosed) {
        if (isclosed) {
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(4);
        }
        if (!isclosed && this.mIsclosed) {
            this.mCoverStateFast = true;
            this.mHwAmbientLuxFilterAlgo.setCoverModeFastResponseFlag(this.mCoverStateFast);
            this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessageDelayed(4, (long) this.mCoverModeFastResponseTimeDelay);
        }
        this.mIsclosed = isclosed;
        this.mHwAmbientLuxFilterAlgo.setCoverModeStatus(isclosed);
    }

    public void setCoverModeFastResponseFlag() {
        this.mCoverStateFast = false;
        this.mHwAmbientLuxFilterAlgo.setCoverModeFastResponseFlag(this.mCoverStateFast);
        mHwNormalizedScreenAutoBrightnessSpline.setNoOffsetEnable(this.mCoverStateFast);
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LabcCoverMode FastResponseFlag =");
            stringBuilder.append(this.mCoverStateFast);
            Slog.i(str, stringBuilder.toString());
        }
    }

    public boolean getCoverModeFastResponseFlag() {
        return this.mCoverStateFast;
    }

    public void setBackSensorCoverModeBrightness(int brightness) {
        if (this.mData.backSensorCoverModeEnable && brightness > 0) {
            this.mScreenAutoBrightness = brightness;
            this.mHwAmbientLuxFilterAlgo.setBackSensorCoverModeBrightness(brightness);
        }
    }

    public int getpowerOnFastResponseLuxNum() {
        if (this.mHwAmbientLuxFilterAlgo == null) {
            return 8;
        }
        return this.mHwAmbientLuxFilterAlgo.getpowerOnFastResponseLuxNum();
    }

    public void updateAutoDBWhenSameBrightness(int brightness) {
        int brightnessAutoDB = System.getIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", 0, this.mCurrentUserId);
        int brightnessManualDB = System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", 0, this.mCurrentUserId);
        if (brightnessAutoDB != brightness && brightnessManualDB == brightness) {
            System.putIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", brightness, this.mCurrentUserId);
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("OrigAutoDB=");
                stringBuilder.append(brightnessAutoDB);
                stringBuilder.append(",ManualDB=");
                stringBuilder.append(brightnessManualDB);
                stringBuilder.append(",brightness=");
                stringBuilder.append(brightness);
                stringBuilder.append(",mScreenAutoBrightness=");
                stringBuilder.append(this.mScreenAutoBrightness);
                Slog.d(str, stringBuilder.toString());
            }
        }
    }

    public boolean getCameraModeBrightnessLineEnable() {
        if (this.mHwAmbientLuxFilterAlgo == null) {
            return false;
        }
        return this.mHwAmbientLuxFilterAlgo.getCameraModeBrightnessLineEnable();
    }

    public boolean getReadingModeBrightnessLineEnable() {
        if (this.mHwAmbientLuxFilterAlgo == null) {
            return false;
        }
        return this.mHwAmbientLuxFilterAlgo.getReadingModeBrightnessLineEnable();
    }

    public void setCameraModeBrightnessLineEnable(boolean cameraModeEnable) {
        if (mHwNormalizedScreenAutoBrightnessSpline != null && getCameraModeBrightnessLineEnable()) {
            mHwNormalizedScreenAutoBrightnessSpline.setCameraModeEnable(cameraModeEnable);
            if (this.mCameraModeEnable != cameraModeEnable) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("CameraModeEnable change cameraModeEnable=");
                    stringBuilder.append(cameraModeEnable);
                    Slog.d(str, stringBuilder.toString());
                }
                this.mCameraModeChangeAnimationEnable = true;
                updateAutoBrightness(true);
            }
            this.mCameraModeEnable = cameraModeEnable;
        }
    }

    public boolean getCameraModeChangeAnimationEnable() {
        boolean animationEnable = this.mCameraModeChangeAnimationEnable;
        this.mCameraModeChangeAnimationEnable = false;
        return animationEnable;
    }

    public boolean getReadingModeChangeAnimationEnable() {
        boolean mStatus = this.mReadingModeChangeAnimationEnable;
        this.mReadingModeChangeAnimationEnable = false;
        return mStatus;
    }

    public void setKeyguardLockedStatus(boolean isLocked) {
        if (this.mHwAmbientLuxFilterAlgo == null) {
            Slog.e(TAG, "mHwAmbientLuxFilterAlgo=null");
        } else {
            this.mHwAmbientLuxFilterAlgo.setKeyguardLockedStatus(isLocked);
        }
    }

    public boolean getRebootAutoModeEnable() {
        return this.mData.rebootAutoModeEnable;
    }

    public boolean getOutdoorAnimationFlag() {
        return this.mHwAmbientLuxFilterAlgo.getOutdoorAnimationFlag();
    }

    public int getCoverModeBrightnessFromLastScreenBrightness() {
        if (this.mData.backSensorCoverModeEnable) {
            return -3;
        }
        int brightnessMode = System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 1, this.mCurrentUserId);
        if (this.mWakeupCoverBrightnessEnable && !this.mHwAmbientLuxFilterAlgo.getCoverModeDayEnable()) {
            this.mWakeupCoverBrightnessEnable = false;
        }
        if (brightnessMode != 1 || this.mWakeupCoverBrightnessEnable) {
            return this.mData.coverModeDayBrightness;
        }
        return this.mHwAmbientLuxFilterAlgo.getCoverModeBrightnessFromLastScreenBrightness();
    }

    public void setCoverModeDayEnable(boolean coverModeDayEnable) {
        this.mHwAmbientLuxFilterAlgo.setCoverModeDayEnable(coverModeDayEnable);
    }

    public int setScreenBrightnessMappingtoIndoorMax(int brightness) {
        if (brightness == -1 || !this.mData.manualMode || brightness > 255) {
            return brightness;
        }
        int brightnessOut;
        String str;
        StringBuilder stringBuilder;
        if (brightness < 4) {
            brightnessOut = 4;
            if (DEBUG && this.mBrightnessOutForLog != 4) {
                this.mBrightnessOutForLog = 4;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mScreenBrightnessOverrideFromWindowManagerMapping brightnessIn=");
                stringBuilder.append(brightness);
                stringBuilder.append(",brightnessOut=");
                stringBuilder.append(4);
                Slog.d(str, stringBuilder.toString());
            }
        } else {
            brightnessOut = (((brightness - 4) * (this.mData.manualBrightnessMaxLimit - 4)) / 251) + 4;
            if (!(!DEBUG || brightness == brightnessOut || this.mBrightnessOutForLog == brightnessOut)) {
                this.mBrightnessOutForLog = brightnessOut;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mScreenBrightnessOverrideFromWindowManagerMapping brightnessIn=");
                stringBuilder.append(brightness);
                stringBuilder.append(",brightnessOut=");
                stringBuilder.append(brightnessOut);
                Slog.d(str, stringBuilder.toString());
            }
        }
        return brightnessOut;
    }

    public void setManualModeEnableForPg(boolean manualModeEnableForPg) {
    }

    public void setMaxBrightnessFromThermal(int brightness) {
        int mappingBrightness = brightness;
        if (brightness > 0) {
            if (this.mData.thermalModeBrightnessMappingEnable) {
                mappingBrightness = this.mHwBrightnessMapping.getMappingBrightnessForRealNit(brightness);
            }
            this.mMaxBrightnessSetByThermal = mappingBrightness;
        } else {
            this.mMaxBrightnessSetByThermal = 255;
        }
        if (this.mLightSensorEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ThermalMode set auto MaxBrightness=");
            stringBuilder.append(brightness);
            stringBuilder.append(",mappingBrightness=");
            stringBuilder.append(mappingBrightness);
            Slog.i(str, stringBuilder.toString());
            this.mCallbacks.updateBrightness();
        }
    }

    public void setMaxBrightnessFromCryogenic(int brightness) {
        if (this.mData.cryogenicEnable) {
            int mappingBrightness = brightness;
            if (brightness > 0) {
                if (this.mData.cryogenicModeBrightnessMappingEnable) {
                    mappingBrightness = this.mHwBrightnessMapping.getMappingBrightnessForRealNit(brightness);
                }
                this.mMaxBrightnessSetByCryogenic = mappingBrightness;
            } else {
                this.mMaxBrightnessSetByCryogenic = 255;
            }
            if (this.mLightSensorEnabled) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Cryogenic set auto MaxBrightness=");
                stringBuilder.append(brightness);
                stringBuilder.append(",mMaxBrightnessSetByCryogenic=");
                stringBuilder.append(this.mMaxBrightnessSetByCryogenic);
                Slog.i(str, stringBuilder.toString());
                this.mCallbacks.updateBrightness();
            }
        }
    }

    public boolean getRebootFirstBrightnessAnimationEnable() {
        return this.mData.rebootFirstBrightnessAnimationEnable;
    }

    public int getAdjustLightValByPgMode(int rawLightVal) {
        if (this.mHwBrightnessPgSceneDetection != null) {
            int mPgBrightness = this.mHwBrightnessPgSceneDetection.getAdjustLightValByPgMode(rawLightVal);
            if (this.mData.pgModeBrightnessMappingEnable && rawLightVal > this.mData.manualBrightnessMaxLimit) {
                mPgBrightness = this.mHwBrightnessMapping.getMappingBrightnessForRealNit(mPgBrightness);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PG_POWER_SAVE_MODE auto mPgBrightness=");
                    stringBuilder.append(mPgBrightness);
                    stringBuilder.append(",rawLightVal=");
                    stringBuilder.append(rawLightVal);
                    Slog.d(str, stringBuilder.toString());
                }
            }
            return mPgBrightness;
        }
        Slog.w(TAG, "mHwBrightnessPgSceneDetection=null");
        return rawLightVal;
    }

    public int getPowerSavingBrightness(int brightness) {
        if ((mHwNormalizedScreenAutoBrightnessSpline == null || mHwNormalizedScreenAutoBrightnessSpline.getPersonalizedBrightnessCurveEnable()) && this.mHwBrightnessPowerSavingCurve != null && mHwNormalizedScreenAutoBrightnessSpline != null && mHwNormalizedScreenAutoBrightnessSpline.getPowerSavingBrighnessLineEnable()) {
            return this.mHwBrightnessPowerSavingCurve.getPowerSavingBrightness(brightness);
        }
        return brightness;
    }

    public void setBrightnessNoLimit(int brightness, int time) {
        if (brightness <= 0 || brightness > 255) {
            this.mBrightnessNoLimitSetByApp = -1;
        } else {
            this.mBrightnessNoLimitSetByApp = brightness;
        }
        if (this.mLightSensorEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setBrightnessNoLimit set auto Brightness=");
            stringBuilder.append(brightness);
            stringBuilder.append(",time=");
            stringBuilder.append(time);
            Slog.i(str, stringBuilder.toString());
            this.mCallbacks.updateBrightness();
        }
    }

    protected boolean setLightSensorEnabled(boolean enable) {
        HwDualSensorEventListenerImpl hwDualSensorEventListenerImpl;
        String str;
        StringBuilder stringBuilder;
        if (enable) {
            if (!this.mLightSensorEnabled) {
                this.mLightSensorEnabled = true;
                this.mFirstAutoBrightness = true;
                this.mUpdateAutoBrightnessCount = 0;
                this.mLightSensorEnableTime = SystemClock.uptimeMillis();
                this.mLightSensorEnableElapsedTimeNanos = SystemClock.elapsedRealtimeNanos();
                this.mCurrentLightSensorRate = this.mInitialLightSensorRate;
                int i = this.SENSOR_OPTION;
                hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
                if (i == -1) {
                    this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, this.mCurrentLightSensorRate * 1000, this.mHandler);
                } else {
                    i = this.SENSOR_OPTION;
                    hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
                    if (i == 0) {
                        this.mSensorObserver = new SensorObserver();
                        this.mHwDualSensorEventListenerImpl.attachFrontSensorData(this.mSensorObserver);
                    } else {
                        i = this.SENSOR_OPTION;
                        hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
                        if (i == 1) {
                            this.mSensorObserver = new SensorObserver();
                            this.mHwDualSensorEventListenerImpl.attachBackSensorData(this.mSensorObserver);
                        } else {
                            i = this.SENSOR_OPTION;
                            hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
                            if (i == 2) {
                                this.mSensorObserver = new SensorObserver();
                                this.mHwDualSensorEventListenerImpl.attachFusedSensorData(this.mSensorObserver);
                            }
                        }
                    }
                }
                if (this.mWakeupFromSleep) {
                    this.mHandler.sendEmptyMessageAtTime(4, this.mLightSensorEnableTime + 200);
                }
                if (DEBUG) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Enable LightSensor at time:mLightSensorEnableTime=");
                    stringBuilder.append(SystemClock.uptimeMillis());
                    stringBuilder.append(",mLightSensorEnableElapsedTimeNanos=");
                    stringBuilder.append(this.mLightSensorEnableElapsedTimeNanos);
                    Slog.d(str, stringBuilder.toString());
                }
                this.mScreenBrightnessBeforeAdj = -1;
                this.mDragFinished = true;
                return true;
            }
        } else if (this.mLightSensorEnabled) {
            this.mLightSensorEnabled = false;
            this.mFirstAutoBrightness = false;
            Slog.i(TAG, "Disable LightSensor starting...");
            this.mRecentLightSamples = 0;
            this.mAmbientLightRingBuffer.clear();
            clearFilterAlgoParas();
            if (NEED_NEW_FILTER_ALGORITHM) {
                this.mAmbientLightRingBufferFilter.clear();
            }
            this.mCurrentLightSensorRate = -1;
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(4);
            int i2 = this.SENSOR_OPTION;
            hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
            if (i2 == -1) {
                this.mSensorManager.unregisterListener(this.mLightSensorListener);
            } else {
                i2 = this.SENSOR_OPTION;
                HwDualSensorEventListenerImpl hwDualSensorEventListenerImpl2 = this.mHwDualSensorEventListenerImpl;
                if (i2 != 0) {
                    i2 = this.SENSOR_OPTION;
                    hwDualSensorEventListenerImpl2 = this.mHwDualSensorEventListenerImpl;
                    if (i2 != 1) {
                        i2 = this.SENSOR_OPTION;
                        hwDualSensorEventListenerImpl2 = this.mHwDualSensorEventListenerImpl;
                        if (i2 == 2 && this.mSensorObserver != null) {
                            this.mHwDualSensorEventListenerImpl.detachFusedSensorData(this.mSensorObserver);
                        }
                    } else if (this.mSensorObserver != null) {
                        this.mHwDualSensorEventListenerImpl.detachBackSensorData(this.mSensorObserver);
                    }
                } else if (this.mSensorObserver != null) {
                    this.mHwDualSensorEventListenerImpl.detachFrontSensorData(this.mSensorObserver);
                }
            }
            this.mAmbientLuxValid = this.mResetAmbientLuxAfterWarmUpConfig ^ true;
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Disable LightSensor at time:");
                stringBuilder.append(SystemClock.uptimeMillis());
                Slog.d(str, stringBuilder.toString());
            }
        }
        return false;
    }

    private boolean handleDarkAdaptDetector(float lux) {
        if (this.mDarkAdaptDetector == null || this.mCoverStateFast || this.mHwAmbientLuxFilterAlgo.getProximityPositiveEnable()) {
            return false;
        }
        this.mDarkAdaptDetector.updateLux(lux, this.mAmbientLux);
        AdaptState newState = this.mDarkAdaptDetector.getState();
        if (this.mDarkAdaptState == newState) {
            return false;
        }
        DarkAdaptState splineDarkAdaptState;
        if (this.mDarkAdaptState == AdaptState.UNADAPTED && newState == AdaptState.ADAPTING) {
            this.mDarkAdaptDimmingEnable = true;
        } else {
            this.mDarkAdaptDimmingEnable = false;
        }
        this.mDarkAdaptState = newState;
        switch (newState) {
            case UNADAPTED:
                splineDarkAdaptState = DarkAdaptState.UNADAPTED;
                break;
            case ADAPTING:
                splineDarkAdaptState = DarkAdaptState.ADAPTING;
                break;
            case ADAPTED:
                splineDarkAdaptState = DarkAdaptState.ADAPTED;
                break;
            default:
                splineDarkAdaptState = null;
                break;
        }
        mHwNormalizedScreenAutoBrightnessSpline.setDarkAdaptState(splineDarkAdaptState);
        return true;
    }

    private void unlockDarkAdaptLine() {
        if (this.mDarkAdaptDetector != null && this.mHwAmbientLuxFilterAlgo.getOffsetResetEnable()) {
            mHwNormalizedScreenAutoBrightnessSpline.unlockDarkAdaptLine();
        }
    }

    public boolean getDarkAdaptDimmingEnable() {
        return this.mDarkAdaptDetector != null && this.mDarkAdaptDimmingEnable && this.mLightSensorEnabled;
    }

    public void clearDarkAdaptDimmingEnable() {
        this.mDarkAdaptDimmingEnable = false;
    }

    public void getUserDragInfo(Bundle data) {
        String str;
        StringBuilder stringBuilder;
        boolean isDeltaValid = mHwNormalizedScreenAutoBrightnessSpline.isDeltaValid();
        mHwNormalizedScreenAutoBrightnessSpline.resetUserDragLimitFlag();
        int targetBL = getAutomaticScreenBrightness();
        int i = -3;
        if (this.mMaxBrightnessSetByCryogenic < 255) {
            targetBL = targetBL >= this.mMaxBrightnessSetByCryogenic ? -3 : targetBL;
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mMaxBrightnessSetByCryogenic=");
            stringBuilder2.append(this.mMaxBrightnessSetByCryogenic);
            Slog.i(str, stringBuilder2.toString());
        }
        if (this.mMaxBrightnessSetByThermal < 255) {
            if (targetBL < this.mMaxBrightnessSetByThermal) {
                i = targetBL;
            }
            targetBL = i;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mMaxBrightnessSetByThermal=");
            stringBuilder.append(this.mMaxBrightnessSetByThermal);
            Slog.i(str, stringBuilder.toString());
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("getUserDragInfo startBL=");
        stringBuilder.append(this.mScreenBrightnessBeforeAdj);
        stringBuilder.append(", targetBL=");
        stringBuilder.append(targetBL);
        Slog.i(str, stringBuilder.toString());
        str = "DeltaValid";
        boolean z = isDeltaValid && !this.mDragFinished;
        data.putBoolean(str, z);
        data.putInt("StartBrightness", this.mScreenBrightnessBeforeAdj);
        data.putInt("EndBrightness", targetBL);
        data.putInt("FilteredAmbientLight", (int) (this.mAmbientLux + 0.5f));
        data.putBoolean("ProximityPositive", this.mProximityPositive);
        this.mDragFinished = true;
    }

    public void registerCryogenicProcessor(CryogenicPowerProcessor processor) {
        this.mCryogenicProcessor = processor;
    }
}
