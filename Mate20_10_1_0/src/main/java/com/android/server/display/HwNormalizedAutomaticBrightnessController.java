package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.hardware.display.BrightnessConfiguration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.HwNormalizedSpline;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.Spline;
import com.android.server.HwServiceFactory;
import com.android.server.LocalServices;
import com.android.server.display.AutomaticBrightnessController;
import com.android.server.display.DarkAdaptDetector;
import com.android.server.display.DisplayEffectMonitor;
import com.android.server.display.HwBrightnessBatteryDetection;
import com.android.server.display.HwBrightnessPgSceneDetection;
import com.android.server.display.HwBrightnessXmlLoader;
import com.android.server.gesture.GestureNavConst;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import com.android.server.rms.iaware.hiber.constant.AppHibernateCst;
import com.huawei.android.fsm.HwFoldScreenManagerEx;
import com.huawei.displayengine.DisplayEngineManager;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

public class HwNormalizedAutomaticBrightnessController extends AutomaticBrightnessController implements HwBrightnessPgSceneDetection.HwBrightnessPgSceneDetectionCallbacks, HwBrightnessBatteryDetection.Callbacks {
    private static final int AMBIENT_LIGHT_MONITOR_SAMPLING_INTERVAL_MS = 2000;
    private static final int BACK_SENSOR_COVER_MODE_BRIGHTNESS = -3;
    private static final float BRIGHTNESS_ADJUSTMENT_RATIO = 2.0f;
    private static final int BRIGHTNESS_FOR_PROXIMITY_POSITIVE = -2;
    private static final int BRIGHTNESS_FOR_SENSOR_NOT_READY_WHEN_WAKEUP = -1;
    private static final float BRIGHTNESS_MAX_ADJUSTMENT = 1.0f;
    private static final int DC_MODE_ON_NUM = 1;
    private static final int DEFAULT_VALUE = 0;
    private static final float DEFAUL_OFFSET_LUX = -1.0f;
    private static final int DRAG_NO_VALID_BRIGHTNESS = -3;
    private static final int DUAL_SENSOR_BACK_LUX_INDEX = 5;
    private static final int DUAL_SENSOR_FRONT_LUX_INDEX = 4;
    private static final int DUAL_SENSOR_FUSED_LUX_INDEX = 0;
    private static final int DUAL_SENSOR_MAX_INDEX = 6;
    private static final int DUAL_SENSOR_SENSOR_TIME_INDEX = 3;
    private static final int DUAL_SENSOR_SYSTEM_TIME_INDEX = 2;
    private static final int ENABLE_LIGHT_SENSOR_TIME_OUT = 200;
    private static final int EYE_PROTECTION_MODE_CONFIGURE_NUM = 7;
    private static final String FRONT_CAMERA = "1";
    private static final int GAME_IS_DISABLE_AUTO_BRIGHTNESS = 29;
    private static final int GAME_IS_DISABLE_AUTO_BRIGHTNESS_CLOSE = 0;
    private static final int GAME_IS_DISABLE_AUTO_BRIGHTNESS_OPEN = 1;
    private static final int GAME_IS_FRONT_STATE = 27;
    private static final int GAME_MODE_ENTER = 21;
    private static final int GAME_MODE_QUIT = 20;
    private static final int GAME_NOT_DISABLE_AUTO_BRIGHTNESS = 28;
    private static final int GAME_NOT_FRONT_STATE = 26;
    private static final int HOME_MODE_ENTER = 23;
    private static final int HOME_MODE_QUIT = 22;
    /* access modifiers changed from: private */
    public static final boolean HWDEBUG;
    /* access modifiers changed from: private */
    public static final boolean HWFLOW = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final String HW_CUSTOMIZATION_SCREEN_BRIGHTNESS_MODE = "hw_customization_screen_brightness_mode";
    private static final String KEY_DC_BRIGHTNESS_DIMMING_SWITCH = "hw_dc_brightness_dimming_switch";
    private static final String KEY_READING_MODE_SWITCH = "hw_reading_mode_display_switch";
    private static final int LOG_POWER_ON_MS = 2000;
    private static final int MAX_DEFAULT_BRIGHTNESS = 255;
    private static final int MIN_DEFAULT_BRIGHTNESS = 4;
    private static final int MODE_DEFAULT = 0;
    private static final int MODE_LUX_MIN_MAX = 2;
    private static final int MODE_TOP_GAME = 1;
    private static final int MSG_COVER_MODE_DEBOUNCED = 4;
    private static final int MSG_FRONT_CAMERA_UPDATE_BRIGHTNESS = 11;
    private static final int MSG_FRONT_CAMERA_UPDATE_DIMMING_ENABLE = 12;
    private static final int MSG_FULL_SCREEN_VIDEO = 7;
    private static final int MSG_GAME_IS_DISABLE_AUTO_BRIGHTNESS = 9;
    private static final int MSG_GAME_IS_DISABLE_AUTO_BRIGHTNESS_SET_MODE = 10;
    private static final int MSG_PROXIMITY_ENABLE_STATE = 8;
    private static final int MSG_PROXIMITY_SENSOR_DEBOUNCED = 3;
    private static final int MSG_REPORT_PROXIMITY_STATE = 2;
    private static final int MSG_UPDATE_AMBIENT_LUX = 1;
    private static final int MSG_UPDATE_AUTO_BRIGHTNESS = 6;
    private static final int MSG_UPDATE_LANDSCAPE = 5;
    private static final int NORMALIZED_MAX_DEFAULT_BRIGHTNESS = 10000;
    private static final int NO_MAPPING_BRIGHTNESS = -1;
    private static final long POWER_ON_LUX_ABANDON_COUNT_MAX = 3;
    private static final int POWER_ON_LUX_COUNT_MAX = 8;
    private static final int PROXIMITY_EVENT_DISTANCE_INDEX = 0;
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private static final float PROXIMITY_POSITIVE_DISTANCE = 5.0f;
    private static final int PROXIMITY_UNKNOWN = -1;
    private static final int READING_MODE_ON_NUM = 1;
    private static final String SCREEN_BRIGHTNESS_MODE_LAST = "screen_brightness_mode_last";
    /* access modifiers changed from: private */
    public static String TAG = "HwNormalizedAutomaticBrightnessController";
    private static final int TIME_DELAYED_USING_PROXIMITY_STATE = 500;
    private static final int TIME_PRINT_SENSOR_VALUE_INTERVAL = 4000;
    private static final int TIME_SENSOR_REPORT_NONE_VALUE = 400;
    private static final int VEHICLE_MOE_ENTER = 19;
    private static final int VEHICLE_MOE_QUIT = 18;
    private static final int VIDEO_MODE_ENTER = 25;
    private static final int VIDEO_MODE_QUIT = 24;
    private static int sDeviceActualBrightnessLevel = 0;
    private static int sDeviceActualBrightnessNit = 0;
    private static int sDeviceStandardBrightnessNit = 0;
    private static HwNormalizedSpline sHwNormalizedScreenAutoBrightnessSpline;
    private static Light sLight;
    private boolean mAllowLabcUseProximity;
    private float mAmbientLuxForFrontCamera;
    private float mAmbientLuxLast;
    private float mAmbientLuxLastLast;
    private float mAmbientLuxOffset = DEFAUL_OFFSET_LUX;
    private boolean mAnimationGameChangeEnable;
    private float mAutoBrightnessOut;
    private final HandlerThread mAutoBrightnessProcessThread;
    private boolean mAutoPowerSavingAnimationEnable;
    private boolean mAutoPowerSavingBrighnessLineDisableForDemo;
    private HwBrightnessBatteryDetection mBatteryStateReceiver;
    private boolean mBrightnessChageUpStatus;
    /* access modifiers changed from: private */
    public boolean mBrightnessModeSetAutoEnable;
    private int mBrightnessOutForLog = -1;
    private CameraManager.AvailabilityCallback mCameraAvailableCallback;
    private CameraManager mCameraManager;
    private boolean mCameraModeChangeAnimationEnable = false;
    private boolean mCameraModeEnable = false;
    /* access modifiers changed from: private */
    public final Context mContext;
    private int mCoverModeFastResponseTimeDelay = GestureNavConst.CHECK_AFT_TIMEOUT;
    private boolean mCoverStateFast = false;
    private CryogenicPowerProcessor mCryogenicProcessor;
    /* access modifiers changed from: private */
    public String mCurCameraId;
    /* access modifiers changed from: private */
    public int mCurrentAutoBrightness;
    private int mCurrentDarkMode;
    /* access modifiers changed from: private */
    public int mCurrentDisplayMode;
    /* access modifiers changed from: private */
    public boolean mCurrentProximityEnable;
    private boolean mCurrentUserChanging = false;
    /* access modifiers changed from: private */
    public int mCurrentUserId = 0;
    private int mCurveLevel;
    private DarkAdaptDetector mDarkAdaptDetector;
    private boolean mDarkAdaptDimmingEnable;
    private DarkAdaptDetector.AdaptState mDarkAdaptState;
    /* access modifiers changed from: private */
    public final HwBrightnessXmlLoader.Data mData;
    private boolean mDcModeBrightnessEnable;
    private DcModeObserver mDcModeObserver;
    private boolean mDcModeObserverInitialize;
    private float mDefaultBrightness = DEFAUL_OFFSET_LUX;
    private DisplayEffectMonitor mDisplayEffectMonitor;
    private boolean mDisplayModeListenerEnabled;
    private volatile boolean mDragFinished;
    /* access modifiers changed from: private */
    public int mDualSensorRawAmbient = -1;
    private int mEyeProtectionMode;
    private final HwFoldScreenManagerEx.FoldDisplayModeListener mFoldDisplayModeListener;
    private boolean mFrontCameraAppKeepBrightnessEnable;
    private boolean mFrontCameraDimmingEnable;
    private int mFrontCameraMaxBrightness;
    private boolean mGameDisableAutoBrightnessModeEnable;
    private boolean mGameDisableAutoBrightnessModeKeepOffsetEnable;
    private int mGameDisableAutoBrightnessModeStatus;
    private boolean mGameIsFrontEnable;
    private int mGameLevel;
    private boolean mGameModeEnableForOffset;
    private long mGameModeEnterTimestamp;
    private long mGameModeQuitTimestamp;
    private HwRingBuffer mHwAmbientLightRingBuffer = new HwRingBuffer(10);
    private HwRingBuffer mHwAmbientLightRingBufferTrace = new HwRingBuffer(50);
    private HwAmbientLuxFilterAlgo mHwAmbientLuxFilterAlgo;
    private HwBrightnessMapping mHwBrightnessMapping;
    /* access modifiers changed from: private */
    public HwBrightnessPgSceneDetection mHwBrightnessPgSceneDetection;
    private HwBrightnessPowerSavingCurve mHwBrightnessPowerSavingCurve;
    /* access modifiers changed from: private */
    public HwDualSensorEventListenerImpl mHwDualSensorEventListenerImpl;
    private HwEyeProtectionControllerImpl mHwEyeProtectionController;
    private int mHwLastReportedSensorValue = -1;
    private long mHwLastReportedSensorValueTime = -1;
    private int mHwLastSensorValue = -1;
    /* access modifiers changed from: private */
    public final HwNormalizedAutomaticBrightnessHandler mHwNormalizedAutomaticBrightnessHandler;
    private long mHwPrintLogTime = -1;
    private int mHwRateMillis = 300;
    private boolean mHwReportValueWhenSensorOnChange = true;
    private boolean mIntervenedAutoBrightnessEnable = false;
    private boolean mIsBrightnessLimitedByThermal;
    private boolean mIsClosed = false;
    private boolean mIsDarkenAmbientEnable;
    private boolean mIsProximitySceneModeOpened;
    private boolean mIsVideoPlay;
    /* access modifiers changed from: private */
    public boolean mLandscapeModeState;
    private LandscapeStateReceiver mLandscapeStateReceiver;
    private long mLastAmbientLightToMonitorTime;
    private float mLastAmbientLuxForFrontCamera;
    private int mLastBrightnessModeForGame;
    private int mLastDefaultBrightness;
    private int mLowBatteryMaxBrightness;
    private int mLuxMaxBrightness;
    private int mLuxMinBrightness;
    private DisplayEngineManager mManager;
    private Runnable mMaxBrightnessFromCryogenicDelayedRunnable;
    private Handler mMaxBrightnessFromCryogenicHandler;
    private long mNightUpModePowerOffTimestamp;
    private long mNightUpModePowerOnOffTimeDelta;
    private long mNightUpModePowerOnTimestamp;
    private boolean mNightUpTimeEnable;
    private boolean mNightUpTimeFirstCheckEnable;
    private int mPendingProximity = -1;
    private long mPendingProximityDebounceTime = -1;
    private boolean mPolicyChangeFromDim = false;
    private long mPowerOffVehicleTimestamp;
    private boolean mPowerOnEnable;
    private int mPowerOnLuxAbandonCount = 0;
    private int mPowerOnLuxCount = 0;
    private boolean mPowerOnOffStatus;
    private long mPowerOnVehicleTimestamp;
    private int mPowerPolicy = 0;
    private String mPowerStateNameForMonitor;
    private boolean mPowerStatus = false;
    private int mProximity = -1;
    private int mProximityForCallBack = -1;
    /* access modifiers changed from: private */
    public boolean mProximityPositive;
    /* access modifiers changed from: private */
    public long mProximityReportTime;
    private final Sensor mProximitySensor;
    /* access modifiers changed from: private */
    public boolean mProximitySensorEnabled;
    private final SensorEventListener mProximitySensorListener;
    /* access modifiers changed from: private */
    public int mReadingMode;
    private boolean mReadingModeChangeAnimationEnable = false;
    private boolean mReadingModeEnable = false;
    private ContentObserver mReadingModeObserver;
    private int mResetAmbientLuxDisableBrightnessOffset;
    private int mSceneLevel;
    private int mScreenBrightnesOut;
    private volatile int mScreenBrightnessBeforeAdj;
    private ScreenStateReceiver mScreenStateReceiver;
    private boolean mScreenStatus = false;
    private SensorObserver mSensorObserver;
    /* access modifiers changed from: private */
    public int mSensorOption = -1;
    private long mSetCurrentAutoBrightnessTime;
    private SettingsObserver mSettingsObserver;
    private boolean mSettingsObserverInitialize;
    private TouchProximityDetector mTouchProximityDetector;
    private boolean mVehicleModeQuitEnable;
    private boolean mVideoModeEnable;
    private boolean mWakeupCoverBrightnessEnable;

    static {
        boolean z = true;
        if (!Log.HWLog && (!Log.HWModuleLog || !Log.isLoggable(TAG, 3))) {
            z = false;
        }
        HWDEBUG = z;
        loadDeviceBrightness();
        if (HWFLOW) {
            Slog.i(TAG, "DeviceActualLevel=" + sDeviceActualBrightnessLevel + ",DeviceActualBrightnessNit=" + sDeviceActualBrightnessNit + ",DeviceStandardBrightnessNit=" + sDeviceStandardBrightnessNit);
        }
    }

    public HwNormalizedAutomaticBrightnessController(Callbacks callbacks, Looper looper, SensorManager sensorManager, Sensor lightSensor, BrightnessMappingStrategy mapper, int lightSensorWarmUpTime, int brightnessMin, int brightnessMax, float dozeScaleFactor, int lightSensorRate, int initialLightSensorRate, long brighteningLightDebounceConfig, long darkeningLightDebounceConfig, boolean resetAmbientLuxAfterWarmUpConfig, HysteresisLevels ambientBrightnessThresholds, HysteresisLevels screenBrightnessThresholds, long shortTermModelTimeout, PackageManager packageManager, Context context) {
        super(callbacks, looper, sensorManager, lightSensor, mapper, lightSensorWarmUpTime, brightnessMin, brightnessMax, dozeScaleFactor, lightSensorRate, initialLightSensorRate, brighteningLightDebounceConfig, darkeningLightDebounceConfig, resetAmbientLuxAfterWarmUpConfig, ambientBrightnessThresholds, screenBrightnessThresholds, shortTermModelTimeout, packageManager);
        Spline spline = null;
        this.mScreenStateReceiver = null;
        this.mScreenBrightnessBeforeAdj = -1;
        this.mDragFinished = true;
        this.mReadingMode = 0;
        this.mEyeProtectionMode = 0;
        this.mCurveLevel = -1;
        this.mSceneLevel = -1;
        this.mGameLevel = 20;
        this.mGameModeEnterTimestamp = 0;
        this.mGameModeQuitTimestamp = 0;
        this.mGameModeEnableForOffset = false;
        this.mAnimationGameChangeEnable = false;
        this.mDcModeObserver = null;
        this.mDcModeObserverInitialize = true;
        this.mDcModeBrightnessEnable = false;
        this.mAutoPowerSavingBrighnessLineDisableForDemo = false;
        this.mLandscapeStateReceiver = null;
        this.mLandscapeModeState = false;
        this.mSettingsObserver = null;
        this.mSettingsObserverInitialize = true;
        this.mCurrentAutoBrightness = 0;
        this.mResetAmbientLuxDisableBrightnessOffset = 0;
        this.mBrightnessChageUpStatus = false;
        this.mSetCurrentAutoBrightnessTime = -1;
        this.mIsProximitySceneModeOpened = false;
        this.mIsVideoPlay = false;
        this.mVideoModeEnable = false;
        this.mCurrentDisplayMode = 0;
        this.mDisplayModeListenerEnabled = false;
        this.mCurrentDarkMode = 16;
        this.mLowBatteryMaxBrightness = 255;
        this.mLuxMinBrightness = 4;
        this.mLuxMaxBrightness = 255;
        this.mAutoPowerSavingAnimationEnable = false;
        this.mPowerOnOffStatus = false;
        this.mPowerOnVehicleTimestamp = 0;
        this.mPowerOffVehicleTimestamp = 0;
        this.mVehicleModeQuitEnable = false;
        this.mWakeupCoverBrightnessEnable = false;
        this.mPowerOnEnable = false;
        this.mGameDisableAutoBrightnessModeEnable = false;
        this.mLastBrightnessModeForGame = 1;
        this.mGameIsFrontEnable = false;
        this.mGameDisableAutoBrightnessModeKeepOffsetEnable = false;
        this.mGameDisableAutoBrightnessModeStatus = 0;
        this.mBrightnessModeSetAutoEnable = false;
        this.mNightUpTimeEnable = false;
        this.mNightUpTimeFirstCheckEnable = true;
        this.mIsDarkenAmbientEnable = false;
        this.mNightUpModePowerOnTimestamp = 0;
        this.mNightUpModePowerOffTimestamp = 0;
        this.mNightUpModePowerOnOffTimeDelta = 0;
        this.mCurCameraId = null;
        this.mFrontCameraMaxBrightness = 255;
        this.mLastAmbientLuxForFrontCamera = 0.0f;
        this.mAmbientLuxForFrontCamera = 0.0f;
        this.mFrontCameraDimmingEnable = false;
        this.mReadingModeObserver = new ContentObserver(new Handler()) {
            /* class com.android.server.display.HwNormalizedAutomaticBrightnessController.AnonymousClass1 */

            public void onChange(boolean selfChange) {
                HwNormalizedAutomaticBrightnessController hwNormalizedAutomaticBrightnessController = HwNormalizedAutomaticBrightnessController.this;
                int unused = hwNormalizedAutomaticBrightnessController.mReadingMode = Settings.System.getIntForUser(hwNormalizedAutomaticBrightnessController.mContext.getContentResolver(), HwNormalizedAutomaticBrightnessController.KEY_READING_MODE_SWITCH, 0, HwNormalizedAutomaticBrightnessController.this.mCurrentUserId);
                if (HwNormalizedAutomaticBrightnessController.this.mReadingMode == 1) {
                    HwNormalizedAutomaticBrightnessController.this.setReadingModeBrightnessLineEnable(true);
                } else {
                    HwNormalizedAutomaticBrightnessController.this.setReadingModeBrightnessLineEnable(false);
                }
            }
        };
        this.mProximitySensorListener = new SensorEventListener() {
            /* class com.android.server.display.HwNormalizedAutomaticBrightnessController.AnonymousClass2 */

            public void onSensorChanged(SensorEvent event) {
                if (HwNormalizedAutomaticBrightnessController.this.mProximitySensorEnabled) {
                    boolean z = false;
                    float distance = event.values[0];
                    long unused = HwNormalizedAutomaticBrightnessController.this.mProximityReportTime = SystemClock.uptimeMillis();
                    HwNormalizedAutomaticBrightnessController hwNormalizedAutomaticBrightnessController = HwNormalizedAutomaticBrightnessController.this;
                    if (distance >= 0.0f && distance < HwNormalizedAutomaticBrightnessController.PROXIMITY_POSITIVE_DISTANCE) {
                        z = true;
                    }
                    boolean unused2 = hwNormalizedAutomaticBrightnessController.mProximityPositive = z;
                    if (HwNormalizedAutomaticBrightnessController.HWFLOW) {
                        String access$800 = HwNormalizedAutomaticBrightnessController.TAG;
                        Slog.i(access$800, "HwBrightnessProximity onSensorChanged: time = " + HwNormalizedAutomaticBrightnessController.this.mProximityReportTime + ",distance = " + distance + ",mWakeupFromSleep=" + HwNormalizedAutomaticBrightnessController.this.mWakeupFromSleep);
                    }
                    if (!HwNormalizedAutomaticBrightnessController.this.mWakeupFromSleep && HwNormalizedAutomaticBrightnessController.this.mProximityReportTime - HwNormalizedAutomaticBrightnessController.this.mLightSensorEnableTime > 500) {
                        HwNormalizedAutomaticBrightnessController.this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessage(2);
                    }
                }
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        this.mCameraAvailableCallback = new CameraManager.AvailabilityCallback() {
            /* class com.android.server.display.HwNormalizedAutomaticBrightnessController.AnonymousClass3 */

            public void onCameraAvailable(String cameraId) {
                if ("1".equals(cameraId)) {
                    if (HwNormalizedAutomaticBrightnessController.HWFLOW) {
                        String access$800 = HwNormalizedAutomaticBrightnessController.TAG;
                        Slog.i(access$800, "onCameraAvailable mCurCameraId=" + HwNormalizedAutomaticBrightnessController.this.mCurCameraId + ",-->null");
                    }
                    String unused = HwNormalizedAutomaticBrightnessController.this.mCurCameraId = null;
                    HwNormalizedAutomaticBrightnessController.this.updateFrontCameraMaxBrightness();
                }
            }

            public void onCameraUnavailable(String cameraId) {
                if ("1".equals(cameraId)) {
                    if (HwNormalizedAutomaticBrightnessController.HWFLOW) {
                        String access$800 = HwNormalizedAutomaticBrightnessController.TAG;
                        Slog.i(access$800, "onCameraAvailable mCurCameraId=" + HwNormalizedAutomaticBrightnessController.this.mCurCameraId + "->cameraId=" + cameraId);
                    }
                    String unused = HwNormalizedAutomaticBrightnessController.this.mCurCameraId = cameraId;
                    HwNormalizedAutomaticBrightnessController.this.updateFrontCameraMaxBrightness();
                }
            }
        };
        this.mFoldDisplayModeListener = new HwFoldScreenManagerEx.FoldDisplayModeListener() {
            /* class com.android.server.display.HwNormalizedAutomaticBrightnessController.AnonymousClass4 */

            public void onScreenDisplayModeChange(int displayMode) {
                if (HwNormalizedAutomaticBrightnessController.HWDEBUG) {
                    String access$800 = HwNormalizedAutomaticBrightnessController.TAG;
                    Slog.i(access$800, "onScreenDisplayModeChange displayMode=" + displayMode);
                }
                if (HwNormalizedAutomaticBrightnessController.this.mCurrentDisplayMode != displayMode) {
                    if (HwNormalizedAutomaticBrightnessController.HWFLOW) {
                        String access$8002 = HwNormalizedAutomaticBrightnessController.TAG;
                        Slog.i(access$8002, "mCurrentDisplayMode=" + HwNormalizedAutomaticBrightnessController.this.mCurrentDisplayMode + "-->displayMode=" + displayMode);
                    }
                    int unused = HwNormalizedAutomaticBrightnessController.this.mCurrentDisplayMode = displayMode;
                }
            }
        };
        this.mHwAmbientLuxFilterAlgo = new HwAmbientLuxFilterAlgo(lightSensorRate);
        Optional<HwNormalizedSpline> hwSpline = createHwNormalizedAutoBrightnessSpline(context);
        this.mScreenAutoBrightnessSpline = hwSpline.isPresent() ? (Spline) hwSpline.get() : spline;
        this.mAutoBrightnessProcessThread = new HandlerThread(TAG);
        this.mAutoBrightnessProcessThread.start();
        this.mHwNormalizedAutomaticBrightnessHandler = new HwNormalizedAutomaticBrightnessHandler(this.mAutoBrightnessProcessThread.getLooper());
        this.mHwReportValueWhenSensorOnChange = this.mHwAmbientLuxFilterAlgo.reportValueWhenSensorOnChange();
        this.mProximitySensor = sensorManager.getDefaultSensor(8);
        if (SystemProperties.getInt("ro.config.hw_eyes_protection", 7) != 0) {
            this.mHwEyeProtectionController = new HwEyeProtectionControllerImpl(context, this);
        }
        this.mData = HwBrightnessXmlLoader.getData();
        this.mAllowLabcUseProximity = this.mData.allowLabcUseProximity;
        this.mContext = context;
        this.mDisplayEffectMonitor = DisplayEffectMonitor.getInstance(context);
        if (this.mDisplayEffectMonitor == null) {
            Slog.e(TAG, "getDisplayEffectMonitor failed!");
        }
        sendXmlConfigToMonitor();
        this.mHwBrightnessPgSceneDetection = new HwBrightnessPgSceneDetection(this, this.mData.pgSceneDetectionDarkenDelayTime, this.mData.pgSceneDetectionBrightenDelayTime, this.mContext);
        this.mHwDualSensorEventListenerImpl = HwDualSensorEventListenerImpl.getInstance(sensorManager, this.mContext);
        this.mSensorOption = this.mHwDualSensorEventListenerImpl.getModuleSensorOption(TAG);
        if (this.mData.darkAdapterEnable) {
            this.mDarkAdaptDetector = new DarkAdaptDetector(this.mData);
        }
        this.mHwBrightnessMapping = new HwBrightnessMapping(this.mData.brightnessMappingPoints);
        if (this.mData.pgReregisterScene) {
            this.mScreenStateReceiver = new ScreenStateReceiver();
        }
        if (this.mData.landscapeBrightnessModeEnable) {
            this.mHwNormalizedAutomaticBrightnessHandler.post(new Runnable() {
                /* class com.android.server.display.$$Lambda$HwNormalizedAutomaticBrightnessController$cKSdCbp2olcXTDFK3LOe7_jSTuk */

                public final void run() {
                    HwNormalizedAutomaticBrightnessController.this.lambda$new$0$HwNormalizedAutomaticBrightnessController();
                }
            });
        }
        this.mHwBrightnessPowerSavingCurve = new HwBrightnessPowerSavingCurve(this.mData.manualBrightnessMaxLimit, this.mData.screenBrightnessMinNit, this.mData.screenBrightnessMaxNit);
        initOptionalFunction();
    }

    public /* synthetic */ void lambda$new$0$HwNormalizedAutomaticBrightnessController() {
        this.mLandscapeStateReceiver = new LandscapeStateReceiver();
        Slog.i(TAG, "registerReceiver LandscapeStateReceiver");
    }

    private void initOptionalFunction() {
        if (this.mData.cryogenicEnable) {
            this.mMaxBrightnessFromCryogenicHandler = new Handler();
            this.mMaxBrightnessFromCryogenicDelayedRunnable = new Runnable() {
                /* class com.android.server.display.HwNormalizedAutomaticBrightnessController.AnonymousClass5 */

                public void run() {
                    HwNormalizedAutomaticBrightnessController.this.setMaxBrightnessFromCryogenicDelayed();
                }
            };
        }
        if (this.mData.readingModeEnable) {
            this.mHwNormalizedAutomaticBrightnessHandler.post(new Runnable() {
                /* class com.android.server.display.$$Lambda$HwNormalizedAutomaticBrightnessController$eeMfaamq8XCMUoJLRTENogK1nx4 */

                public final void run() {
                    HwNormalizedAutomaticBrightnessController.this.lambda$initOptionalFunction$1$HwNormalizedAutomaticBrightnessController();
                }
            });
        }
        if (this.mData.touchProximityEnable) {
            this.mTouchProximityDetector = new TouchProximityDetector(this.mData);
        }
        if (this.mData.autoPowerSavingBrighnessLineDisableForDemo) {
            this.mAutoPowerSavingBrighnessLineDisableForDemo = isDemoVersion();
        }
        if (this.mData.luxlinePointsForBrightnessLevelEnable) {
            this.mSettingsObserver = new SettingsObserver(this.mHwNormalizedAutomaticBrightnessHandler);
        }
        if (this.mData.dcModeEnable) {
            this.mDcModeObserver = new DcModeObserver(this.mHwNormalizedAutomaticBrightnessHandler);
        }
        initBrightnessParaForSpline();
        if (this.mData.gameDisableAutoBrightnessModeEnable) {
            updateGameDisableAutoBrightnessModeEnable();
            Slog.i(TAG, "init updateGameDisableAutoBrightnessModeEnable");
        }
        if (this.mData.batteryModeEnable) {
            this.mHwNormalizedAutomaticBrightnessHandler.post(new Runnable() {
                /* class com.android.server.display.$$Lambda$HwNormalizedAutomaticBrightnessController$9cq1iKhbsVSK6H6TTC1lISxI37w */

                public final void run() {
                    HwNormalizedAutomaticBrightnessController.this.lambda$initOptionalFunction$2$HwNormalizedAutomaticBrightnessController();
                }
            });
        }
    }

    public /* synthetic */ void lambda$initOptionalFunction$1$HwNormalizedAutomaticBrightnessController() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_READING_MODE_SWITCH), true, this.mReadingModeObserver, -1);
        this.mReadingMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_READING_MODE_SWITCH, 0, this.mCurrentUserId);
        if (this.mReadingMode == 1) {
            setReadingModeBrightnessLineEnable(true);
        }
        Slog.i(TAG, "readingModeEnable enable ...");
    }

    public /* synthetic */ void lambda$initOptionalFunction$2$HwNormalizedAutomaticBrightnessController() {
        this.mBatteryStateReceiver = new HwBrightnessBatteryDetection(this, this.mContext);
    }

    private void initBrightnessParaForSpline() {
        if (this.mData.brightnessOffsetLuxModeEnable) {
            initBrightnessOffsetPara();
        }
        if (this.mData.twoPointOffsetModeEnable) {
            initTwoPointBrightnessOffsetPara();
        }
        if (this.mData.videoFullScreenModeEnable) {
            initVideoFullScreenModeBrightnessPara(this.mData.videoFullScreenModeEnable, this.mData.brightnessLineForVideoFullScreenMode);
        }
        if (this.mData.dayModeNewCurveEnable && this.mData.dayModeAlgoEnable) {
            initDayModeBrightnessPara(this.mData.dayModeNewCurveEnable, this.mData.dayModeBrightnessLinePoints);
        }
    }

    private void initVideoFullScreenModeBrightnessPara(boolean enable, List<PointF> list) {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline == null) {
            Slog.w(TAG, "initVideoFullScreenModeBrightnessPara fail, sHwNormalizedScreenAutoBrightnessSpline=null");
        } else {
            hwNormalizedSpline.initVideoFullScreenModeBrightnessPara(enable, list);
        }
    }

    private void initDayModeBrightnessPara(boolean dayModeNewCurveEnable, List<PointF> dayModeBrightnessLinePoints) {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline == null) {
            Slog.w(TAG, "initDayModeBrightnessPara fail, sHwNormalizedScreenAutoBrightnessSpline=null");
        } else {
            hwNormalizedSpline.initDayModeBrightnessPara(dayModeNewCurveEnable, dayModeBrightnessLinePoints);
        }
    }

    private void initBrightnessOffsetPara() {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline != null) {
            hwNormalizedSpline.initBrightenOffsetLux(this.mData.brightnessOffsetLuxModeEnable, this.mData.brightenOffsetLuxTh1, this.mData.brightenOffsetLuxTh2, this.mData.brightenOffsetLuxTh3);
            sHwNormalizedScreenAutoBrightnessSpline.initBrightenOffsetNoValidDarkenLux(this.mData.brightenOffsetEffectMinLuxEnable, this.mData.brightenOffsetNoValidDarkenLuxTh1, this.mData.brightenOffsetNoValidDarkenLuxTh2, this.mData.brightenOffsetNoValidDarkenLuxTh3, this.mData.brightenOffsetNoValidDarkenLuxTh4);
            sHwNormalizedScreenAutoBrightnessSpline.initBrightenOffsetNoValidBrightenLux(this.mData.brightenOffsetNoValidBrightenLuxTh1, this.mData.brightenOffsetNoValidBrightenLuxTh2, this.mData.brightenOffsetNoValidBrightenLuxTh3, this.mData.brightenOffsetNoValidBrightenLuxTh4);
            sHwNormalizedScreenAutoBrightnessSpline.initDarkenOffsetLux(this.mData.darkenOffsetLuxTh1, this.mData.darkenOffsetLuxTh2, this.mData.darkenOffsetLuxTh3);
            sHwNormalizedScreenAutoBrightnessSpline.initDarkenOffsetNoValidBrightenLux(this.mData.darkenOffsetNoValidBrightenLuxTh1, this.mData.darkenOffsetNoValidBrightenLuxTh2, this.mData.darkenOffsetNoValidBrightenLuxTh3, this.mData.darkenOffsetNoValidBrightenLuxTh4);
            sHwNormalizedScreenAutoBrightnessSpline.initBrightnessOffsetTmpValidPara(this.mData.brightnessOffsetTmpValidEnable, this.mData.brightenOffsetNoValidSavedLuxTh1, this.mData.brightenOffsetNoValidSavedLuxTh2);
        }
    }

    private void initTwoPointBrightnessOffsetPara() {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline != null) {
            hwNormalizedSpline.initTwoPointOffsetPara(this.mData.twoPointOffsetModeEnable, this.mData.twoPointOffsetLuxTh, this.mData.twoPointOffsetAdjionLuxTh, this.mData.twoPointOffsetNoValidLuxTh);
            sHwNormalizedScreenAutoBrightnessSpline.initTwoPointOffsetLowLuxPara(this.mData.lowBrightenOffsetNoValidBrightenLuxTh, this.mData.lowDarkenOffsetNoValidBrightenLuxTh, this.mData.lowBrightenOffsetNoValidDarkenLuxTh, this.mData.lowDarkenOffsetNoValidDarkenLuxTh, this.mData.lowDarkenOffsetDarkenBrightnessRatio);
            sHwNormalizedScreenAutoBrightnessSpline.initTwoPointOffsetHighLuxPara(this.mData.highBrightenOffsetNoValidBrightenLuxTh, this.mData.highDarkenOffsetNoValidBrightenLuxTh, this.mData.highBrightenOffsetNoValidDarkenLuxTh, this.mData.highDarkenOffsetNoValidDarkenLuxTh);
        }
    }

    private static void loadDeviceBrightness() {
        LightsManager lightsManager = (LightsManager) LocalServices.getService(LightsManager.class);
        if (lightsManager == null) {
            Slog.e(TAG, "loadDeviceBrightness() get LightsManager failed");
            return;
        }
        Light lcdLight = lightsManager.getLight(0);
        if (lcdLight == null) {
            Slog.e(TAG, "loadDeviceBrightness() get Light failed");
            return;
        }
        sDeviceActualBrightnessLevel = lcdLight.getDeviceActualBrightnessLevel();
        sDeviceActualBrightnessNit = lcdLight.getDeviceActualBrightnessNit();
        sDeviceStandardBrightnessNit = lcdLight.getDeviceStandardBrightnessNit();
    }

    private static Optional<HwNormalizedSpline> createHwNormalizedAutoBrightnessSpline(Context context) {
        try {
            sHwNormalizedScreenAutoBrightnessSpline = HwNormalizedSpline.createHwNormalizedSpline(context, sDeviceActualBrightnessLevel, sDeviceActualBrightnessNit, sDeviceStandardBrightnessNit);
            return Optional.of(sHwNormalizedScreenAutoBrightnessSpline);
        } catch (IllegalArgumentException ex) {
            Slog.e(TAG, "Could not create auto-brightness spline.", ex);
            return Optional.empty();
        }
    }

    @Override // com.android.server.display.HwBrightnessPgSceneDetection.HwBrightnessPgSceneDetectionCallbacks
    public void updateStateRecognition(boolean PowerSavingCurveEnable, int appType) {
        if (this.mLightSensorEnabled && !this.mAutoPowerSavingBrighnessLineDisableForDemo) {
            HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
            if (hwNormalizedSpline == null) {
                Slog.w(TAG, "sHwNormalizedScreenAutoBrightnessSpline is null, no orig powerSaving");
            } else if (this.mGameLevel != 21 || !hwNormalizedSpline.getPowerSavingBrighnessLineEnable()) {
                if (this.mData.autoPowerSavingUseManualAnimationTimeEnable) {
                    this.mAutoPowerSavingAnimationEnable = sHwNormalizedScreenAutoBrightnessSpline.getPowerSavingModeBrightnessChangeEnable(this.mAmbientLux, PowerSavingCurveEnable);
                }
                sHwNormalizedScreenAutoBrightnessSpline.setPowerSavingModeEnable(PowerSavingCurveEnable);
                HwBrightnessPowerSavingCurve hwBrightnessPowerSavingCurve = this.mHwBrightnessPowerSavingCurve;
                if (hwBrightnessPowerSavingCurve != null) {
                    hwBrightnessPowerSavingCurve.setPowerSavingEnable(PowerSavingCurveEnable);
                }
                updateAutoBrightness(true, false);
            } else {
                Slog.i(TAG, "GameBrightMode no orig powerSaving");
            }
        }
    }

    private static boolean isDemoVersion() {
        String vendor2 = SystemProperties.get("ro.hw.vendor", "");
        String country = SystemProperties.get("ro.hw.country", "");
        String str = TAG;
        Slog.i(str, "vendor:" + vendor2 + ",country:" + country);
        return "demo".equalsIgnoreCase(vendor2) || "demo".equalsIgnoreCase(country);
    }

    public boolean getAnimationGameChangeEnable() {
        boolean animationEnable = this.mAnimationGameChangeEnable && this.mData.gameModeEnable;
        if (!this.mHwAmbientLuxFilterAlgo.getProximityPositiveEnable()) {
            this.mAnimationGameChangeEnable = false;
        }
        if (HWFLOW && animationEnable != this.mAnimationGameChangeEnable) {
            String str = TAG;
            Slog.i(str, "GameBrightMode set dimming animationEnable=" + this.mAnimationGameChangeEnable);
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
            DarkAdaptDetector darkAdaptDetector = this.mDarkAdaptDetector;
            if (darkAdaptDetector != null) {
                darkAdaptDetector.setAutoModeOff();
                this.mDarkAdaptDimmingEnable = false;
            }
        }
        if (!enable) {
            this.mHwAmbientLuxFilterAlgo.setPowerStatus(false);
        }
        if (this.mData.foldScreenModeEnable) {
            setFoldDisplayModeEnable(enable && !dozing);
        }
        HwNormalizedAutomaticBrightnessController.super.configure(enable, configuration, brightness, userChangedBrightness, adjustment, userChangedAutoBrightnessAdjustment, displayPolicy);
        if (this.mLightSensorEnabled && this.mHwPrintLogTime == -1) {
            this.mHwPrintLogTime = this.mLightSensorEnableTime;
        }
        if (enable && !dozing && !this.mHwBrightnessPgSceneDetection.getPgRecognitionListenerRegisted()) {
            this.mHwBrightnessPgSceneDetection.registerPgRecognitionListener(this.mContext);
            if (HWFLOW) {
                Slog.i(TAG, "PowerSaving auto in registerPgBLightSceneChangedListener,=" + this.mHwBrightnessPgSceneDetection.getPgRecognitionListenerRegisted());
            }
        }
        if (this.mData.proximitySceneModeEnable) {
            if (this.mIsProximitySceneModeOpened && enable && !dozing) {
                z = true;
            }
            updateProximitySensorEnabledMsg(z);
        } else {
            if (this.mAllowLabcUseProximity && enable && !dozing) {
                z = true;
            }
            updateProximitySensorEnabledMsg(z);
        }
        setTouchProximityEnabled(enable);
        updateContentObserver();
        if (this.mData.frontCameraMaxBrightnessEnable && this.mCameraManager == null) {
            this.mCameraManager = (CameraManager) this.mContext.getSystemService("camera");
            this.mCameraManager.registerAvailabilityCallback(this.mCameraAvailableCallback, (Handler) null);
            Slog.i(TAG, "registerAvailabilityCallback for auto frontCameraMaxBrightness");
        }
    }

    private void setTouchProximityEnabled(boolean enable) {
        TouchProximityDetector touchProximityDetector = this.mTouchProximityDetector;
        if (touchProximityDetector == null) {
            return;
        }
        if (enable) {
            touchProximityDetector.enable();
        } else {
            touchProximityDetector.disable();
        }
    }

    private void updateContentObserver() {
        if (this.mData.luxlinePointsForBrightnessLevelEnable && this.mSettingsObserverInitialize) {
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_auto_brightness"), false, this.mSettingsObserver, -1);
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_brightness"), false, this.mSettingsObserver, -1);
            this.mSettingsObserverInitialize = false;
            Slog.i(TAG, "mSettingsObserver Initialize");
        }
        if (this.mData.dcModeEnable && this.mDcModeObserverInitialize) {
            this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_DC_BRIGHTNESS_DIMMING_SWITCH), true, this.mDcModeObserver, -1);
            this.mDcModeObserverInitialize = false;
            Slog.i(TAG, "DcModeObserver Initialize");
            updateDcMode();
        }
    }

    public int getAutomaticScreenBrightness() {
        int i;
        int i2;
        int i3;
        if (this.mWakeupFromSleep && SystemClock.uptimeMillis() - this.mLightSensorEnableTime < 200) {
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "mWakeupFromSleep= " + this.mWakeupFromSleep + ",currentTime=" + SystemClock.uptimeMillis() + ",mLightSensorEnableTime=" + this.mLightSensorEnableTime);
            }
            this.mScreenBrightnesOut = -1;
        } else if (needToSetBrightnessBaseIntervened()) {
            this.mScreenBrightnesOut = HwNormalizedAutomaticBrightnessController.super.getAutomaticScreenBrightness();
        } else if (needToSetBrightnessBaseProximity()) {
            this.mScreenBrightnesOut = -2;
        } else {
            this.mScreenBrightnesOut = HwNormalizedAutomaticBrightnessController.super.getAutomaticScreenBrightness();
        }
        if (this.mScreenBrightnesOut > this.mLowBatteryMaxBrightness && this.mData.batteryModeEnable) {
            if (HWFLOW) {
                String str2 = TAG;
                Slog.i(str2, "mScreenBrightnesOut = " + this.mScreenBrightnesOut + ", mLowBatteryMaxBrightness = " + this.mLowBatteryMaxBrightness);
            }
            this.mScreenBrightnesOut = this.mLowBatteryMaxBrightness;
        }
        if (!this.mIntervenedAutoBrightnessEnable && this.mData.luxMinMaxBrightnessEnable && (i2 = this.mScreenBrightnesOut) < (i3 = this.mLuxMinBrightness) && i2 > 0) {
            this.mScreenBrightnesOut = i3;
        }
        if (this.mData.frontCameraMaxBrightnessEnable && (i = this.mScreenBrightnesOut) > this.mFrontCameraMaxBrightness && i > 0) {
            if (HWDEBUG) {
                String str3 = TAG;
                Slog.i(str3, "mScreenBrightnesOut = " + this.mScreenBrightnesOut + "--> mFrontCameraMaxBrightness= " + this.mFrontCameraMaxBrightness);
            }
            this.mScreenBrightnesOut = this.mFrontCameraMaxBrightness;
        }
        return this.mScreenBrightnesOut;
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
            String action = intent.getAction();
            String access$800 = HwNormalizedAutomaticBrightnessController.TAG;
            Slog.i(access$800, "BroadcastReceiver.onReceive() action:" + action);
            if ("android.intent.action.BOOT_COMPLETED".equals(action) && !HwNormalizedAutomaticBrightnessController.this.mHwBrightnessPgSceneDetection.getPgRecognitionListenerRegisted()) {
                HwNormalizedAutomaticBrightnessController.this.mHwBrightnessPgSceneDetection.registerPgRecognitionListener(HwNormalizedAutomaticBrightnessController.this.mContext);
                if (HwNormalizedAutomaticBrightnessController.HWFLOW) {
                    String access$8002 = HwNormalizedAutomaticBrightnessController.TAG;
                    Slog.i(access$8002, "BOOT_COMPLETED: auto in registerPgBLightSceneChangedListener,=" + HwNormalizedAutomaticBrightnessController.this.mHwBrightnessPgSceneDetection.getPgRecognitionListenerRegisted());
                }
            }
        }
    }

    private boolean needToSetBrightnessBaseIntervened() {
        return this.mIntervenedAutoBrightnessEnable && (this.mAllowLabcUseProximity || this.mData.proximitySceneModeEnable);
    }

    public int getAutoBrightnessBaseInOutDoorLimit(int brightness) {
        int tmpBrightnessOut;
        if (this.mAmbientLux >= ((float) this.mData.outDoorThreshold) || !this.mData.autoModeInOutDoorLimitEnble) {
            return brightness;
        }
        if (brightness < this.mData.manualBrightnessMaxLimit) {
            tmpBrightnessOut = brightness;
        } else {
            tmpBrightnessOut = this.mData.manualBrightnessMaxLimit;
        }
        return tmpBrightnessOut;
    }

    public void setPersonalizedBrightnessCurveLevel(int curveLevel) {
        if (sHwNormalizedScreenAutoBrightnessSpline == null) {
            String str = TAG;
            Slog.i(str, "setPersonalizedBrightnessCurveLevel failed!,curveLevel=" + curveLevel + ",sHwNormalizedScreenAutoBrightnessSpline=null");
        } else if (curveLevel == 19 || curveLevel == 18) {
            updateVehicleState(curveLevel);
        } else if (curveLevel == 23 || curveLevel == 22) {
            updateHomeModeState(curveLevel);
        } else if (curveLevel == 29 || curveLevel == 28) {
            updateGameDisableAutoBrightnessModeState(curveLevel);
        } else {
            updateBrightnessCurveState(curveLevel);
        }
    }

    private void updateVehicleState(int curveLevel) {
        if (this.mData.vehicleModeEnable) {
            if (curveLevel == 19 || curveLevel == 18) {
                HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
                if (hwNormalizedSpline == null) {
                    Slog.w(TAG, "VehicleBrightMode updateVehicleState fail,HwNormalizedScreenAutoBrightnessSpline==null");
                    return;
                }
                hwNormalizedSpline.setSceneCurveLevel(curveLevel);
                if (curveLevel == 19) {
                    this.mVehicleModeQuitEnable = false;
                    long timDelta = SystemClock.elapsedRealtime() - this.mPowerOnVehicleTimestamp;
                    if (timDelta > this.mData.vehicleModeEnterTimeForPowerOn) {
                        updateAutoBrightness(true, false);
                        if (HWFLOW) {
                            String str = TAG;
                            Slog.i(str, "VehicleBrightMode updateAutoBrightness curveLevel=" + curveLevel + ",timDelta=" + timDelta);
                        }
                    }
                } else if (this.mVehicleModeQuitEnable) {
                    long timDelta2 = SystemClock.elapsedRealtime() - this.mPowerOnVehicleTimestamp;
                    boolean vehicleModeBrightnessEnable = sHwNormalizedScreenAutoBrightnessSpline.getVehicleModeBrightnessEnable();
                    if (timDelta2 < this.mData.vehicleModeQuitTimeForPowerOn && vehicleModeBrightnessEnable) {
                        sHwNormalizedScreenAutoBrightnessSpline.setVehicleModeQuitEnable();
                        String str2 = TAG;
                        Slog.i(str2, "VehicleBrightMode mVehicleModeQuitEnable timDelta=" + timDelta2);
                    }
                }
                if (this.mSceneLevel != curveLevel) {
                    if (HWFLOW) {
                        String str3 = TAG;
                        Slog.i(str3, "VehicleBrightMode set curveLevel=" + curveLevel);
                    }
                    this.mSceneLevel = curveLevel;
                }
            }
        }
    }

    private void updateBrightnessCurveState(int curveLevel) {
        if (HWDEBUG) {
            String str = TAG;
            Slog.d(str, "updateBrightnessCurveState, curveLevel=" + curveLevel);
        }
        updateGameIsFrontState(curveLevel);
        updateGameModeCurveState(curveLevel);
        updateVideoModeCurveState(curveLevel);
        updatePersonalCurveModeCureState(curveLevel);
    }

    private void updateGameModeCurveState(int curveLevel) {
        if (this.mData.gameModeEnable) {
            if (curveLevel == 21 || curveLevel == 20) {
                HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
                if (hwNormalizedSpline == null) {
                    Slog.w(TAG, "GameBrightMode updateVehicleState fail,HwNormalizedScreenAutoBrightnessSpline==null");
                } else if (this.mGameLevel != curveLevel) {
                    hwNormalizedSpline.setGameCurveLevel(curveLevel);
                    if (this.mGameLevel != curveLevel) {
                        this.mAnimationGameChangeEnable = true;
                    }
                    if (curveLevel == 21) {
                        this.mGameModeEnableForOffset = true;
                        this.mHwAmbientLuxFilterAlgo.setGameModeEnable(true);
                        setProximitySceneMode(true);
                        this.mGameModeEnterTimestamp = SystemClock.elapsedRealtime();
                        long timeDelta = this.mGameModeEnterTimestamp - this.mGameModeQuitTimestamp;
                        if (timeDelta > this.mData.gameModeClearOffsetTime) {
                            float ambientLuxOffset = sHwNormalizedScreenAutoBrightnessSpline.getGameModeAmbientLuxForOffset();
                            if (!this.mData.gameModeOffsetValidAmbientLuxEnable || ambientLuxOffset == DEFAUL_OFFSET_LUX) {
                                sHwNormalizedScreenAutoBrightnessSpline.clearGameOffsetDelta();
                            } else {
                                sHwNormalizedScreenAutoBrightnessSpline.resetGameModeOffsetFromHumanFactor(calculateOffsetMinBrightness(ambientLuxOffset, 1), calculateOffsetMaxBrightness(ambientLuxOffset, 1));
                            }
                            sHwNormalizedScreenAutoBrightnessSpline.resetGameBrightnessLimitation();
                            String str = TAG;
                            Slog.i(str, "GameBrightMode enterGame timeDelta=" + timeDelta);
                        }
                    } else {
                        this.mHwAmbientLuxFilterAlgo.setGameModeEnable(false);
                        setProximitySceneMode(false);
                        this.mGameModeEnableForOffset = false;
                        this.mGameModeQuitTimestamp = SystemClock.elapsedRealtime();
                    }
                    String str2 = TAG;
                    Slog.i(str2, "GameBrightMode updateAutoBrightness,curveLevel=" + curveLevel);
                    updateAutoBrightness(true, false);
                    this.mGameLevel = curveLevel;
                }
            }
        }
    }

    private void updateVideoModeCurveState(int curveLevel) {
        if (this.mData.videoFullScreenModeEnable) {
            if (curveLevel != 25 && curveLevel != 24) {
                return;
            }
            if (sHwNormalizedScreenAutoBrightnessSpline == null) {
                Slog.w(TAG, "VideoBrightMode no update,HwNormalizedScreenAutoBrightnessSpline==null");
                return;
            }
            this.mVideoModeEnable = curveLevel == 25;
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "VideoBrightMode set curveLevel=" + curveLevel + ",mVideoModeEnable=" + this.mVideoModeEnable);
            }
        }
    }

    private void quitVehicleMode() {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline == null) {
            Slog.w(TAG, "VehicleBrightMode quitVehicleMode fail,HwNormalizedScreenAutoBrightnessSpline==null");
        } else if (this.mHwAmbientLuxFilterAlgo == null) {
            Slog.w(TAG, "VehicleBrightMode quitVehicleMode fail,mHwAmbientLuxFilterAlgo==null");
        } else {
            hwNormalizedSpline.setVehicleModeQuitEnable();
        }
    }

    private void updateHomeModeState(int curveLevel) {
        if (this.mData.homeModeEnable) {
            if (curveLevel != 23 && curveLevel != 22) {
                return;
            }
            if (this.mHwAmbientLuxFilterAlgo == null) {
                Slog.w(TAG, "HomeBrightMode updateHomeModeState fail,mHwAmbientLuxFilterAlgo==null");
            } else if (sHwNormalizedScreenAutoBrightnessSpline == null) {
                Slog.w(TAG, "HomeBrightMode setDayModeEnable fail,sHwNormalizedScreenAutoBrightnessSpline==null");
            } else {
                boolean homeModeEnable = curveLevel == 23;
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "HomeBrightMode set curveLevel=" + curveLevel + ",homeModeEnable=" + homeModeEnable);
                }
                this.mHwAmbientLuxFilterAlgo.setHomeModeEnable(homeModeEnable);
                if (this.mData.dayModeAlgoEnable) {
                    sHwNormalizedScreenAutoBrightnessSpline.setDayModeEnable(this.mHwAmbientLuxFilterAlgo.getDayModeEnable());
                }
                if (homeModeEnable) {
                    quitVehicleMode();
                }
                updateAutoBrightness(true, false);
            }
        }
    }

    private void updatePersonalCurveModeCureState(int curveLevel) {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline == null) {
            Slog.w(TAG, "NewCurveMode updateVehicleState fail,HwNormalizedScreenAutoBrightnessSpline==null");
            return;
        }
        if (curveLevel != this.mCurveLevel) {
            hwNormalizedSpline.setPersonalizedBrightnessCurveLevel(curveLevel);
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "NewCurveMode setPersonalizedBrightnessCurveLevel curveLevel=" + curveLevel);
            }
        }
        this.mCurveLevel = curveLevel;
    }

    public void updateNewBrightnessCurveTmp() {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline != null) {
            hwNormalizedSpline.updateNewBrightnessCurveTmp();
            sendPersonalizedCurveAndParamToMonitor(sHwNormalizedScreenAutoBrightnessSpline.getPersonalizedDefaultCurve(), sHwNormalizedScreenAutoBrightnessSpline.getPersonalizedAlgoParam());
            return;
        }
        Slog.e(TAG, "NewCurveMode updateNewBrightnessCurveTmp fail,mSpline==null");
    }

    public void updateNewBrightnessCurve() {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline != null) {
            hwNormalizedSpline.updateNewBrightnessCurve();
        } else {
            Slog.e(TAG, "NewCurveMode updateNewBrightnessCurve fail,sHwNormalizedScreenAutoBrightnessSpline==null");
        }
    }

    public List<PointF> getCurrentDefaultNewCurveLine() {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline != null) {
            return hwNormalizedSpline.getCurrentDefaultNewCurveLine();
        }
        return null;
    }

    public void updateIntervenedAutoBrightness(int brightness) {
        this.mAutoBrightnessOut = (float) brightness;
        this.mIntervenedAutoBrightnessEnable = true;
        if (this.mData.cryogenicEnable) {
            this.mMaxBrightnessSetByCryogenicBypass = true;
        }
        if (sHwNormalizedScreenAutoBrightnessSpline != null) {
            if (!this.mData.manualMode) {
                HwNormalizedAutomaticBrightnessController.super.updateIntervenedAutoBrightness((int) this.mAutoBrightnessOut);
                return;
            }
            if (this.mDragFinished) {
                this.mScreenBrightnessBeforeAdj = getAutomaticScreenBrightness();
                this.mDragFinished = false;
            }
            this.mDefaultBrightness = sHwNormalizedScreenAutoBrightnessSpline.getCurrentDefaultBrightnessNoOffset();
            float lux = sHwNormalizedScreenAutoBrightnessSpline.getCurrentAmbientLuxForBrightness();
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "HwAutoBrightnessIn=" + brightness + ",defaultBrightness=" + this.mDefaultBrightness + ",lux=" + lux);
            }
            if (this.mAutoBrightnessOut >= ((float) this.mData.manualBrightnessMaxLimit)) {
                if (lux > ((float) this.mData.outDoorThreshold)) {
                    int autoBrightnessOutTmp = ((int) this.mAutoBrightnessOut) < this.mData.manualBrightnessMaxLimit ? (int) this.mAutoBrightnessOut : this.mData.manualBrightnessMaxLimit;
                    float f = this.mDefaultBrightness;
                    this.mAutoBrightnessOut = autoBrightnessOutTmp > ((int) f) ? (float) autoBrightnessOutTmp : (float) ((int) f);
                } else {
                    this.mAutoBrightnessOut = this.mAutoBrightnessOut < ((float) this.mData.manualBrightnessMaxLimit) ? this.mAutoBrightnessOut : (float) this.mData.manualBrightnessMaxLimit;
                }
            }
            if (this.mData.frontCameraMaxBrightnessEnable) {
                float f2 = this.mAutoBrightnessOut;
                int i = this.mFrontCameraMaxBrightness;
                if (f2 >= ((float) i)) {
                    this.mAutoBrightnessOut = (float) i;
                }
            }
            if (HWFLOW) {
                String str2 = TAG;
                Slog.i(str2, "HwAutoBrightnessOut=" + this.mAutoBrightnessOut);
            }
            HwNormalizedAutomaticBrightnessController.super.updateIntervenedAutoBrightness((int) this.mAutoBrightnessOut);
        }
    }

    /* JADX INFO: Multiple debug info for r5v2 int: [D('average' int), D('i' int)] */
    private int getSensorData() {
        synchronized (this.mHwAmbientLightRingBuffer) {
            long time = SystemClock.uptimeMillis();
            int bufferSize = this.mHwAmbientLightRingBuffer.size();
            if (bufferSize > 0) {
                int sum = 0;
                for (int i = bufferSize - 1; i >= 0; i--) {
                    sum = (int) (((float) sum) + this.mHwAmbientLightRingBuffer.getLux(i));
                }
                int i2 = sum / bufferSize;
                if (i2 >= 0) {
                    this.mHwLastSensorValue = i2;
                }
                this.mHwAmbientLightRingBuffer.clear();
                if (time - this.mHwPrintLogTime > 4000) {
                    int bufferSize2 = this.mHwAmbientLightRingBufferTrace.size();
                    if (HWFLOW) {
                        Slog.d("lux trace:", this.mHwAmbientLightRingBufferTrace.toString(bufferSize2));
                    }
                    this.mHwAmbientLightRingBufferTrace.clear();
                    this.mHwPrintLogTime = time;
                }
                return this.mHwLastSensorValue;
            } else if (time - this.mHwLastReportedSensorValueTime < 400) {
                return this.mHwLastSensorValue;
            } else {
                return this.mHwLastReportedSensorValue;
            }
        }
    }

    private void clearSensorData() {
        synchronized (this.mHwAmbientLightRingBuffer) {
            this.mHwAmbientLightRingBuffer.clear();
            int bufferSize = this.mHwAmbientLightRingBufferTrace.size();
            if (HWFLOW) {
                Slog.d("lux trace:", this.mHwAmbientLightRingBufferTrace.toString(bufferSize));
            }
            this.mHwAmbientLightRingBufferTrace.clear();
            this.mHwLastReportedSensorValueTime = -1;
            this.mHwLastReportedSensorValue = -1;
            this.mHwLastSensorValue = -1;
            this.mHwPrintLogTime = -1;
        }
    }

    private float getTouchProximityProcessedLux(boolean isFirstLux, float lux) {
        String str;
        float luxOut = lux;
        TouchProximityDetector touchProximityDetector = this.mTouchProximityDetector;
        if (touchProximityDetector == null) {
            return lux;
        }
        if (!isFirstLux) {
            boolean isCurrentLuxValid = touchProximityDetector.isCurrentLuxValid();
            if (bypassTouchProximityResult()) {
                isCurrentLuxValid = true;
            }
            boolean needUseLastLux = true;
            updateTouchProximityState(!isCurrentLuxValid);
            float lastLux = this.mAmbientLuxLastLast;
            float f = this.mAmbientLuxLast;
            if (lastLux <= f) {
                lastLux = f;
            }
            if (isCurrentLuxValid || luxOut >= lastLux) {
                needUseLastLux = false;
            }
            if (HWDEBUG) {
                String str2 = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("TouchProximityDetector isValid=");
                sb.append(isCurrentLuxValid);
                sb.append(", lux=");
                sb.append(luxOut);
                if (needUseLastLux) {
                    str = "->" + lastLux;
                } else {
                    str = "";
                }
                sb.append(str);
                Slog.d(str2, sb.toString());
            }
            if (needUseLastLux) {
                luxOut = lastLux;
            }
        }
        this.mTouchProximityDetector.startNextLux();
        this.mAmbientLuxLastLast = isFirstLux ? luxOut : this.mAmbientLuxLast;
        this.mAmbientLuxLast = luxOut;
        return luxOut;
    }

    private void reportLightSensorEventToAlgo(long time, float lux) {
        this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(1);
        float luxOut = getTouchProximityProcessedLux(!this.mAmbientLuxValid, lux);
        updateFirstAmbientLuxPara(luxOut);
        this.mHwAmbientLuxFilterAlgo.handleLightSensorEvent(time, luxOut);
        this.mAmbientLux = this.mHwAmbientLuxFilterAlgo.getCurrentAmbientLux();
        boolean isDarkAdaptStateChanged = handleDarkAdaptDetector(luxOut);
        if (this.mData.frontCameraMaxBrightnessEnable) {
            this.mAmbientLuxForFrontCamera = this.mHwAmbientLuxFilterAlgo.getAmbientLuxForFrontCamera();
            if ((this.mLastAmbientLuxForFrontCamera <= this.mData.frontCameraLuxThreshold && this.mAmbientLuxForFrontCamera > this.mData.frontCameraLuxThreshold) || (this.mLastAmbientLuxForFrontCamera > this.mData.frontCameraLuxThreshold && this.mAmbientLuxForFrontCamera <= this.mData.frontCameraLuxThreshold)) {
                String str = TAG;
                Slog.i(str, "updateFrontCameraMaxBrightness mLastAmbientLuxForFrontCamera=" + this.mLastAmbientLuxForFrontCamera + ",mAmbientLuxForFrontCamera=" + this.mAmbientLuxForFrontCamera);
                updateFrontCameraMaxBrightness();
                this.mLastAmbientLuxForFrontCamera = this.mAmbientLuxForFrontCamera;
            }
        }
        if (this.mHwAmbientLuxFilterAlgo.needToUpdateBrightness() || isDarkAdaptStateChanged) {
            updateDarkenAmbientEnable();
            if (this.mData.luxMinMaxBrightnessEnable) {
                updateLuxMinMaxBrightness(this.mAmbientLux);
            }
            if (HWFLOW) {
                String str2 = TAG;
                Slog.i(str2, "need to update brightness: mAmbientLux=" + this.mAmbientLux);
            }
            this.mHwAmbientLuxFilterAlgo.updateNeedToUpdateBrightnessFlag();
            updateDarkTimeDelayFromBrightnessEnable();
            updateSecondDarkenModeNoResponseLongEnable();
            updateAutoBrightness(true, false);
        }
        if (!this.mHwReportValueWhenSensorOnChange) {
            this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessageDelayed(1, (long) this.mHwRateMillis);
        }
        if (!this.mProximityPositive) {
            sendAmbientLightToMonitor(time, luxOut);
        } else {
            this.mLastAmbientLightToMonitorTime = 0;
        }
        sendDefaultBrightnessToMonitor();
    }

    private void updateFirstAmbientLuxPara(float lux) {
        if (!this.mAmbientLuxValid) {
            this.mWakeupFromSleep = false;
            this.mAmbientLuxValid = true;
            this.mHwAmbientLuxFilterAlgo.updateFirstAmbientLuxEnable(true);
            if (sHwNormalizedScreenAutoBrightnessSpline != null) {
                if (this.mData.dayModeAlgoEnable || this.mData.offsetResetEnable) {
                    this.mHwAmbientLuxFilterAlgo.setAutoModeEnableFirstLux(lux);
                    this.mHwAmbientLuxFilterAlgo.setDayModeEnable();
                    if (this.mData.dayModeAlgoEnable) {
                        sHwNormalizedScreenAutoBrightnessSpline.setDayModeEnable(this.mHwAmbientLuxFilterAlgo.getDayModeEnable());
                    }
                    updateOffsetPara();
                    if (HWFLOW) {
                        String str = TAG;
                        Slog.i(str, "DayMode:dayModeEnable=" + this.mHwAmbientLuxFilterAlgo.getDayModeEnable() + ",offsetEnable=" + this.mHwAmbientLuxFilterAlgo.getOffsetResetEnable());
                    }
                }
                if (HWFLOW) {
                    String str2 = TAG;
                    Slog.i(str2, "mAmbientLuxValid=" + this.mAmbientLuxValid + ",mWakeupFromSleep= " + this.mWakeupFromSleep);
                }
            }
        }
    }

    private void updateOffsetPara() {
        if (this.mData.offsetResetEnable && sHwNormalizedScreenAutoBrightnessSpline != null) {
            if (this.mData.twoPointOffsetModeEnable) {
                resetTwoPointOffsetFromHumanFactor(this.mHwAmbientLuxFilterAlgo.getOffsetResetEnable());
            }
            this.mAmbientLuxOffset = sHwNormalizedScreenAutoBrightnessSpline.getCurrentAmbientLuxForOffset();
            float f = this.mAmbientLuxOffset;
            if (f != DEFAUL_OFFSET_LUX) {
                int offsetScreenBrightnessMinByAmbientLux = calculateOffsetMinBrightness(f, 0);
                int offsetScreenBrightnessMaxByAmbientLux = calculateOffsetMaxBrightness(this.mAmbientLuxOffset, 0);
                if (sHwNormalizedScreenAutoBrightnessSpline.getPersonalizedBrightnessCurveEnable()) {
                    float defaultBrightness = sHwNormalizedScreenAutoBrightnessSpline.getDefaultBrightness(this.mAmbientLuxOffset);
                    float currentBrightness = sHwNormalizedScreenAutoBrightnessSpline.getNewCurrentBrightness(this.mAmbientLuxOffset);
                    offsetScreenBrightnessMinByAmbientLux += ((int) currentBrightness) - ((int) defaultBrightness);
                    offsetScreenBrightnessMaxByAmbientLux += ((int) currentBrightness) - ((int) defaultBrightness);
                    String str = TAG;
                    Slog.i(str, "NewCurveMode new offset MinByAmbientLux=" + offsetScreenBrightnessMinByAmbientLux + ",maxByAmbientLux" + offsetScreenBrightnessMaxByAmbientLux);
                }
                sHwNormalizedScreenAutoBrightnessSpline.reSetOffsetFromHumanFactor(this.mHwAmbientLuxFilterAlgo.getOffsetResetEnable(), offsetScreenBrightnessMinByAmbientLux, offsetScreenBrightnessMaxByAmbientLux);
            }
            unlockDarkAdaptLine();
        }
    }

    private void resetTwoPointOffsetFromHumanFactor(boolean enable) {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline != null) {
            float ambientLuxOffsetLow = hwNormalizedSpline.getCurrentLowAmbientLuxForTwoPointOffset();
            float ambientLuxOffsetHigh = sHwNormalizedScreenAutoBrightnessSpline.getCurrentHighAmbientLuxForTwoPointOffset();
            float ambientLuxOffsetTmp = sHwNormalizedScreenAutoBrightnessSpline.getCurrentTmpAmbientLuxForTwoPointOffset();
            if (ambientLuxOffsetLow != DEFAUL_OFFSET_LUX) {
                sHwNormalizedScreenAutoBrightnessSpline.resetTwoPointOffsetLowFromHumanFactor(enable, calculateOffsetMinBrightness(ambientLuxOffsetLow, 0), calculateOffsetMaxBrightness(ambientLuxOffsetLow, 0));
            }
            if (ambientLuxOffsetHigh != DEFAUL_OFFSET_LUX) {
                sHwNormalizedScreenAutoBrightnessSpline.resetTwoPointOffsetHighFromHumanFactor(enable, calculateOffsetMinBrightness(ambientLuxOffsetHigh, 0), calculateOffsetMaxBrightness(ambientLuxOffsetHigh, 0));
            }
            if (ambientLuxOffsetTmp != DEFAUL_OFFSET_LUX) {
                sHwNormalizedScreenAutoBrightnessSpline.resetTwoPointOffsetTmpFromHumanFactor(enable, calculateOffsetMinBrightness(ambientLuxOffsetHigh, 0), calculateOffsetMaxBrightness(ambientLuxOffsetHigh, 0));
            }
        }
    }

    private void updateDarkTimeDelayFromBrightnessEnable() {
        HwNormalizedSpline hwNormalizedSpline;
        if (this.mData.darkTimeDelayEnable && (hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline) != null) {
            float defaultBrightness = hwNormalizedSpline.getNewDefaultBrightness(this.mAmbientLux);
            if (this.mAmbientLux < this.mData.darkTimeDelayLuxThreshold || defaultBrightness >= this.mData.darkTimeDelayBrightness) {
                this.mHwAmbientLuxFilterAlgo.setDarkTimeDelayFromBrightnessEnable(false);
                return;
            }
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "DarkTimeDelay mAmbientLux=" + this.mAmbientLux + ",defaultBrightness=" + defaultBrightness + ",thresh=" + this.mData.darkTimeDelayBrightness);
            }
            this.mHwAmbientLuxFilterAlgo.setDarkTimeDelayFromBrightnessEnable(true);
        }
    }

    private void updateSecondDarkenModeNoResponseLongEnable() {
        HwNormalizedSpline hwNormalizedSpline;
        if (this.mData.secondDarkenModeEnable && this.mData.secondDarkenModeNoResponseDarkenTime > 0 && this.mData.secondDarkenModeNoResponseDarkenTimeMin > 0 && this.mData.secondDarkenModeMinLuxTh > 0.0f && (hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline) != null) {
            float normalizedBrightness = hwNormalizedSpline.interpolate(this.mData.secondDarkenModeMinLuxTh) * 10000.0f;
            if (((int) this.mAmbientLux) == ((int) this.mData.secondDarkenModeMinLuxTh)) {
                float brightness = (255.0f * normalizedBrightness) / 10000.0f;
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "secondDarkenMode lux=" + this.mData.secondDarkenModeMinLuxTh + ",brightness=" + brightness + ",normalizedBrightness=" + normalizedBrightness + ",noFlickertarget=" + this.mData.darkenNoFlickerTarget);
                }
            }
            this.mHwAmbientLuxFilterAlgo.updateSecondDarkenModeNoResponseLongEnable(normalizedBrightness < this.mData.darkenNoFlickerTarget);
        }
    }

    private int calculateOffsetMinBrightness(float amLux, int mode) {
        List<HwXmlAmPoint> brightnessPoints;
        if (amLux < 0.0f) {
            Slog.w(TAG, "amlux<0, return offsetMIN");
            return 4;
        }
        float offsetMinBrightness = 4.0f;
        HwXmlAmPoint prePoint = null;
        if (mode == 1) {
            brightnessPoints = this.mData.gameModeAmbientLuxValidBrightnessPoints;
        } else if (mode == 2) {
            brightnessPoints = this.mData.luxMinMaxBrightnessPoints;
        } else {
            brightnessPoints = this.mData.ambientLuxValidBrightnessPoints;
        }
        Iterator<HwXmlAmPoint> iter = brightnessPoints.iterator();
        while (true) {
            if (!iter.hasNext()) {
                break;
            }
            HwXmlAmPoint curPoint = iter.next();
            if (prePoint == null) {
                prePoint = curPoint;
            }
            if (amLux >= curPoint.x) {
                prePoint = curPoint;
                offsetMinBrightness = prePoint.y;
            } else if (curPoint.x <= prePoint.x) {
                offsetMinBrightness = 4.0f;
                String str = TAG;
                Slog.w(str, "OffsetMinBrightness_prePoint.x <= nextPoint.x,x" + curPoint.x + ", y = " + curPoint.y);
            } else {
                offsetMinBrightness = (((curPoint.y - prePoint.y) / (curPoint.x - prePoint.x)) * (amLux - prePoint.x)) + prePoint.y;
            }
        }
        return (int) offsetMinBrightness;
    }

    private int calculateOffsetMaxBrightness(float amLux, int mode) {
        List<HwXmlAmPoint> brightnessPoints;
        if (amLux < 0.0f) {
            Slog.w(TAG, "amlux<0, return offsetMAX");
            return 255;
        }
        float offsetMaxBrightness = 255.0f;
        HwXmlAmPoint prePoint = null;
        if (mode == 1) {
            brightnessPoints = this.mData.gameModeAmbientLuxValidBrightnessPoints;
        } else {
            brightnessPoints = this.mData.ambientLuxValidBrightnessPoints;
        }
        Iterator<HwXmlAmPoint> iter = brightnessPoints.iterator();
        while (true) {
            if (!iter.hasNext()) {
                break;
            }
            HwXmlAmPoint curPoint = iter.next();
            if (prePoint == null) {
                prePoint = curPoint;
            }
            if (amLux >= curPoint.x) {
                prePoint = curPoint;
                offsetMaxBrightness = prePoint.z;
            } else if (curPoint.x <= prePoint.x) {
                offsetMaxBrightness = 255.0f;
                String str = TAG;
                Slog.w(str, "OffsetMaxBrightness_prePoint.x <= nextPoint.x,x" + curPoint.x + ", z = " + curPoint.z);
            } else {
                offsetMaxBrightness = (((curPoint.z - prePoint.z) / (curPoint.x - prePoint.x)) * (amLux - prePoint.x)) + prePoint.z;
            }
        }
        return (int) offsetMaxBrightness;
    }

    private void sendAmbientLightToMonitor(long time, float lux) {
        if (this.mDisplayEffectMonitor != null) {
            long j = this.mLastAmbientLightToMonitorTime;
            if (j == 0 || time <= j) {
                this.mLastAmbientLightToMonitorTime = time;
                return;
            }
            int durationInMs = (int) (time - j);
            if (durationInMs >= 2000) {
                this.mLastAmbientLightToMonitorTime = time;
                ArrayMap<String, Object> params = new ArrayMap<>();
                params.put(DisplayEffectMonitor.MonitorModule.PARAM_TYPE, "ambientLightCollection");
                params.put("lightValue", Integer.valueOf((int) lux));
                params.put("durationInMs", Integer.valueOf(durationInMs));
                params.put("brightnessMode", "AUTO");
                int i = this.mDualSensorRawAmbient;
                if (i >= 0) {
                    params.put("rawLightValue", Integer.valueOf(i));
                }
                this.mDisplayEffectMonitor.sendMonitorParam(params);
            }
        }
    }

    private void sendDefaultBrightnessToMonitor() {
        HwNormalizedSpline hwNormalizedSpline;
        int defaultBrightness;
        if (this.mDisplayEffectMonitor != null && (hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline) != null && this.mLastDefaultBrightness != (defaultBrightness = (int) hwNormalizedSpline.getCurrentDefaultBrightnessNoOffset())) {
            this.mLastDefaultBrightness = defaultBrightness;
            ArrayMap<String, Object> params = new ArrayMap<>();
            params.put(DisplayEffectMonitor.MonitorModule.PARAM_TYPE, "algoDefaultBrightness");
            params.put("lightValue", Integer.valueOf((int) sHwNormalizedScreenAutoBrightnessSpline.getCurrentAmbientLuxForBrightness()));
            params.put("brightness", Integer.valueOf(defaultBrightness));
            params.put("brightnessMode", "AUTO");
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    private void sendPowerStateToMonitor(int policy) {
        String newStateName;
        if (this.mDisplayEffectMonitor != null) {
            if (policy == 0 || policy == 1) {
                newStateName = AwareJobSchedulerConstants.BAR_STATUS_OFF;
            } else if (policy == 2) {
                newStateName = "DIM";
            } else if (policy == 3) {
                newStateName = AwareJobSchedulerConstants.BAR_STATUS_ON;
            } else if (policy != 4) {
                newStateName = AwareJobSchedulerConstants.BAR_STATUS_OFF;
            } else {
                newStateName = "VR";
            }
            if (this.mPowerStateNameForMonitor != newStateName) {
                this.mPowerStateNameForMonitor = newStateName;
                ArrayMap<String, Object> params = new ArrayMap<>();
                params.put(DisplayEffectMonitor.MonitorModule.PARAM_TYPE, "powerStateUpdate");
                params.put("powerState", newStateName);
                this.mDisplayEffectMonitor.sendMonitorParam(params);
            }
        }
    }

    private void sendXmlConfigToMonitor() {
        if (this.mDisplayEffectMonitor != null) {
            ArrayMap<String, Object> params = new ArrayMap<>();
            params.put(DisplayEffectMonitor.MonitorModule.PARAM_TYPE, "xmlConfig");
            params.put("enable", Boolean.valueOf(this.mData.monitorEnable));
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    private void sendPersonalizedCurveAndParamToMonitor(List<Short> curve, List<Float> algoParam) {
        if (this.mDisplayEffectMonitor != null && curve != null && !curve.isEmpty() && algoParam != null && !algoParam.isEmpty()) {
            ArrayMap<String, Object> params = new ArrayMap<>();
            params.put(DisplayEffectMonitor.MonitorModule.PARAM_TYPE, "personalizedCurveAndParam");
            params.put("personalizedCurve", curve);
            params.put("personalizedParam", algoParam);
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    /* access modifiers changed from: protected */
    public void setBrightnessLimitedByThermal(boolean isLimited) {
        sendThermalLimitToMonitor(isLimited);
    }

    private void sendThermalLimitToMonitor(boolean isLimited) {
        if (this.mDisplayEffectMonitor != null && this.mIsBrightnessLimitedByThermal != isLimited) {
            this.mIsBrightnessLimitedByThermal = isLimited;
            ArrayMap<String, Object> params = new ArrayMap<>();
            params.put(DisplayEffectMonitor.MonitorModule.PARAM_TYPE, "thermalLimit");
            params.put("isLimited", Boolean.valueOf(isLimited));
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    /* access modifiers changed from: protected */
    public void handleLightSensorEvent(long time, float lux) {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline == null || !hwNormalizedSpline.getCalibrationTestEable()) {
            this.mSetbrightnessImmediateEnable = false;
            if (!this.mAmbientLuxValid || this.mHwReportValueWhenSensorOnChange) {
                reportLightSensorEventToAlgo(time, lux);
                if (!this.mHwReportValueWhenSensorOnChange) {
                    synchronized (this.mHwAmbientLightRingBuffer) {
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
            return;
        }
        this.mSetbrightnessImmediateEnable = true;
        getLightSensorFromDb();
    }

    private void getLightSensorFromDb() {
        this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(1);
        this.mAmbientLuxValid = true;
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline != null) {
            float ambientLux = hwNormalizedSpline.getAmbientValueFromDb();
            if (((int) (ambientLux * 10.0f)) != ((int) (this.mAmbientLux * 10.0f))) {
                this.mAmbientLux = ambientLux;
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "setAmbientLuxDB=" + this.mAmbientLux);
                }
                updateAutoBrightness(true, false);
            }
        }
    }

    /* access modifiers changed from: private */
    public void setMaxBrightnessFromCryogenicDelayed() {
        if (HWFLOW) {
            Slog.i(TAG, "mMaxBrightnessSetByCryogenicBypassDelayed=false");
        }
        this.mMaxBrightnessSetByCryogenicBypassDelayed = false;
        if (this.mMaxBrightnessSetByCryogenic < 255 && this.mLightSensorEnabled) {
            String str = TAG;
            Slog.i(str, "Cryogenic set mMaxBrightnessSetByCryogenic=" + this.mMaxBrightnessSetByCryogenic);
            this.mCallbacks.updateBrightness();
        }
    }

    public void setPowerStatus(boolean powerStatus) {
        updateCryogenicPara(powerStatus);
        if (HWFLOW && this.mPowerStatus != powerStatus) {
            String str = TAG;
            Slog.i(str, "set power status:mPowerStatus=" + this.mPowerStatus + ",powerStatus=" + powerStatus);
        }
        if (this.mPowerStatus != powerStatus && powerStatus && this.mData.coverModeDayEnable) {
            updateCoverModeDayBrightness();
        }
        updateVehiclePara(powerStatus);
        this.mPowerStatus = powerStatus;
        this.mScreenStatus = powerStatus;
        this.mWakeupFromSleep = powerStatus;
        this.mWakeupForFirstAutoBrightness = powerStatus;
        this.mHwAmbientLuxFilterAlgo.setPowerStatus(powerStatus);
        if (!this.mPowerStatus) {
            this.mPowerOnLuxAbandonCount = 0;
            this.mPowerOnLuxCount = 0;
            this.mWakeupCoverBrightnessEnable = false;
        }
    }

    private void updateCryogenicPara(boolean powerStatus) {
        Runnable runnable;
        if (this.mData.cryogenicEnable) {
            if (powerStatus) {
                this.mPowerOnTimestamp = SystemClock.elapsedRealtime();
                if (this.mPowerOnTimestamp - this.mPowerOffTimestamp > this.mData.cryogenicActiveScreenOffIntervalInMillis) {
                    if (HWFLOW) {
                        String str = TAG;
                        Slog.i(str, "mPowerOnTimestamp - mPowerOffTimestamp=" + (this.mPowerOnTimestamp - this.mPowerOffTimestamp) + ", apply Cryogenic brightness limit(" + this.mMaxBrightnessSetByCryogenic + ")!");
                    }
                    this.mMaxBrightnessSetByCryogenicBypass = false;
                }
                if (HWFLOW) {
                    String str2 = TAG;
                    Slog.i(str2, "mMaxBrightnessSetByCryogenicBypass=" + this.mMaxBrightnessSetByCryogenicBypass + " mMaxBrightnessSetByCryogenicBypassDelayed=" + this.mMaxBrightnessSetByCryogenicBypassDelayed);
                }
                if (this.mMaxBrightnessSetByCryogenic == 255) {
                    this.mMaxBrightnessSetByCryogenicBypassDelayed = true;
                    if (HWFLOW) {
                        String str3 = TAG;
                        Slog.d(str3, "No Cryogenic brightness limit! Then it should be active " + (this.mData.cryogenicLagTimeInMillis / AppHibernateCst.DELAY_ONE_MINS) + "min later!");
                    }
                    Handler handler = this.mMaxBrightnessFromCryogenicHandler;
                    if (handler != null && (runnable = this.mMaxBrightnessFromCryogenicDelayedRunnable) != null) {
                        handler.removeCallbacks(runnable);
                        this.mMaxBrightnessFromCryogenicHandler.postDelayed(this.mMaxBrightnessFromCryogenicDelayedRunnable, this.mData.cryogenicLagTimeInMillis);
                        return;
                    }
                    return;
                }
                return;
            }
            this.mPowerOffTimestamp = SystemClock.elapsedRealtime();
            CryogenicPowerProcessor cryogenicPowerProcessor = this.mCryogenicProcessor;
            if (cryogenicPowerProcessor != null) {
                cryogenicPowerProcessor.onScreenOff();
            }
        }
    }

    private void updateVehiclePara(boolean powerStatus) {
        HwNormalizedSpline hwNormalizedSpline;
        if (!(this.mPowerOnOffStatus == powerStatus || (hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline) == null)) {
            if (!powerStatus) {
                boolean enableTmp = hwNormalizedSpline.getNewCurveEableTmp();
                sHwNormalizedScreenAutoBrightnessSpline.setNewCurveEnable(enableTmp);
                if (enableTmp) {
                    String str = TAG;
                    Slog.i(str, "NewCurveMode poweroff updateNewCurve(tem--real),enableTmp=" + enableTmp + ",powerStatus=" + powerStatus);
                }
            }
            sHwNormalizedScreenAutoBrightnessSpline.setPowerStatus(powerStatus);
            updateVehicleQuitPara(powerStatus);
        }
        this.mPowerOnOffStatus = powerStatus;
    }

    private void updateVehicleQuitPara(boolean powerStatus) {
        if (this.mData.vehicleModeEnable) {
            if (powerStatus) {
                updateVehicleQuitEnable();
                return;
            }
            this.mVehicleModeQuitEnable = false;
            this.mPowerOffVehicleTimestamp = SystemClock.elapsedRealtime();
        }
    }

    private void updateVehicleQuitEnable() {
        HwNormalizedSpline hwNormalizedSpline;
        this.mPowerOnVehicleTimestamp = SystemClock.elapsedRealtime();
        if (this.mPowerOnVehicleTimestamp - this.mPowerOffVehicleTimestamp > this.mData.vehicleModeDisableTimeMillis && (hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline) != null) {
            boolean vehicleEnable = hwNormalizedSpline.getVehicleModeBrightnessEnable();
            boolean vehicleQuitEnable = sHwNormalizedScreenAutoBrightnessSpline.getVehicleModeQuitForPowerOnEnable();
            if (vehicleEnable && vehicleQuitEnable) {
                sHwNormalizedScreenAutoBrightnessSpline.setVehicleModeQuitEnable();
                if (HWFLOW) {
                    Slog.i(TAG, "VehicleBrightMode quit from lastOnScreen");
                }
            }
            this.mVehicleModeQuitEnable = true;
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "VehicleBrightMode mVehicleModeQuitEnable OnOfftime=" + (this.mPowerOnVehicleTimestamp - this.mPowerOffVehicleTimestamp));
            }
        }
    }

    private void updateCoverModeDayBrightness() {
        int openHour;
        boolean isClosed = HwServiceFactory.isCoverClosed();
        int brightnessMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 1, this.mCurrentUserId);
        if (isClosed && brightnessMode == 1 && (openHour = Calendar.getInstance().get(11)) >= this.mData.converModeDayBeginTime && openHour < this.mData.coverModeDayEndTime) {
            setCoverModeDayEnable(true);
            this.mWakeupCoverBrightnessEnable = true;
            String str = TAG;
            Slog.i(str, "LabcCoverMode,isClosed=" + isClosed + ",openHour=" + openHour + ",coverModeBrightness=" + this.mData.coverModeDayBrightness);
        }
    }

    /* access modifiers changed from: protected */
    public boolean isInValidLightSensorEvent(long time, float lux) {
        long currentTime = SystemClock.uptimeMillis();
        if (currentTime < ((long) this.mLightSensorWarmUpTimeConfig) + this.mLightSensorEnableTime) {
            Slog.i(TAG, "sensor not ready yet at time " + time);
            return true;
        } else if (this.mCurrentUserChanging) {
            return true;
        } else {
            if (this.mPowerStatus) {
                this.mPowerOnLuxAbandonCount++;
                this.mPowerOnLuxCount++;
                if (this.mPowerOnLuxCount > getPowerOnFastResponseLuxNum() && currentTime > this.mLightSensorEnableTime + this.mData.powerOnFastResponseTime) {
                    if (HWFLOW) {
                        Slog.i(TAG, "set power status:false,mPowerOnLuxCount=" + this.mPowerOnLuxCount + ",powerOnFastResponseLuxNum=" + getPowerOnFastResponseLuxNum() + ",currentTime=" + currentTime + ",deltaTime= " + (currentTime - this.mLightSensorEnableTime));
                    }
                    this.mPowerStatus = false;
                    this.mHwAmbientLuxFilterAlgo.setPowerStatus(false);
                }
                if (this.mLightSensorEnableElapsedTimeNanos - time > 0) {
                    if (HWFLOW) {
                        Slog.i(TAG, "abandon handleLightSensorEvent:" + lux);
                    }
                    return true;
                }
            }
            return false;
        }
    }

    public void updateAutoBrightnessAdjustFactor(float adjustFactor) {
        this.mIntervenedAutoBrightnessEnable = false;
        if (sHwNormalizedScreenAutoBrightnessSpline == null) {
            Slog.w(TAG, "updateAutoBrightnessAdjustFactor,sHwNormalizedScreenAutoBrightnessSpline==null");
            return;
        }
        float lux = this.mHwAmbientLuxFilterAlgo.getOffsetValidAmbientLux();
        if (this.mData.offsetValidAmbientLuxEnable) {
            float luxCurrent = this.mHwAmbientLuxFilterAlgo.getCurrentAmbientLux();
            boolean proximityPositiveEnable = this.mHwAmbientLuxFilterAlgo.getProximityPositiveEnable();
            float positionBrightness = adjustFactor * 255.0f;
            float defautBrightness = sHwNormalizedScreenAutoBrightnessSpline.getCurrentDefaultBrightnessNoOffset();
            if (proximityPositiveEnable && ((int) positionBrightness) > ((int) defautBrightness)) {
                lux = luxCurrent;
            }
            this.mHwAmbientLuxFilterAlgo.setCurrentAmbientLux(lux);
        }
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "AdjustPositionBrightness=" + ((int) (adjustFactor * 255.0f)) + ",lux=" + lux);
        }
        float brightnessNewOffset = adjustFactor * 255.0f;
        if (this.mData.luxMinMaxBrightnessEnable && ((int) brightnessNewOffset) < this.mLuxMinBrightness && ((int) brightnessNewOffset) > 0) {
            String str2 = TAG;
            Slog.i(str2, "AdjustPositionBrightness,brightnessNewOffset=" + brightnessNewOffset + "-->mLuxMinBrightness=" + this.mLuxMinBrightness);
            brightnessNewOffset = (float) this.mLuxMinBrightness;
        }
        if (this.mData.frontCameraMaxBrightnessEnable && ((int) brightnessNewOffset) > this.mFrontCameraMaxBrightness && ((int) brightnessNewOffset) > 0) {
            String str3 = TAG;
            Slog.i(str3, "AdjustPositionBrightness,brightnessNewOffset=" + brightnessNewOffset + "-->mFrontCameraMaxBrightness=" + this.mFrontCameraMaxBrightness);
            brightnessNewOffset = (float) this.mFrontCameraMaxBrightness;
        }
        if (this.mGameModeEnableForOffset) {
            sHwNormalizedScreenAutoBrightnessSpline.updateLevelGameWithLux(brightnessNewOffset, lux);
        } else {
            sHwNormalizedScreenAutoBrightnessSpline.updateLevelWithLux(brightnessNewOffset, lux);
        }
        int brightnessOffset = (int) (255.0f * adjustFactor);
        if (this.mResetAmbientLuxDisableBrightnessOffset == 0 && brightnessOffset > this.mData.resetAmbientLuxDisableBrightnessOffset) {
            updateBrightnessModeChangeManualState(false);
        }
        if (brightnessOffset > this.mData.resetAmbientLuxDisableBrightnessOffset) {
            this.mResetAmbientLuxDisableBrightnessOffset = brightnessOffset;
        } else {
            this.mResetAmbientLuxDisableBrightnessOffset = 0;
        }
    }

    /* access modifiers changed from: protected */
    public boolean setAutoBrightnessAdjustment(float adjustment) {
        return false;
    }

    /* access modifiers changed from: private */
    public void handleUpdateAmbientLuxMsg() {
        reportLightSensorEventToAlgo(SystemClock.uptimeMillis(), (float) getSensorData());
    }

    /* access modifiers changed from: private */
    public final class HwNormalizedAutomaticBrightnessHandler extends Handler {
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
                case 5:
                    HwNormalizedAutomaticBrightnessController hwNormalizedAutomaticBrightnessController = HwNormalizedAutomaticBrightnessController.this;
                    hwNormalizedAutomaticBrightnessController.updateLandscapeMode(hwNormalizedAutomaticBrightnessController.mLandscapeModeState);
                    return;
                case 6:
                    HwNormalizedAutomaticBrightnessController hwNormalizedAutomaticBrightnessController2 = HwNormalizedAutomaticBrightnessController.this;
                    hwNormalizedAutomaticBrightnessController2.updateCurrentAutoBrightness(hwNormalizedAutomaticBrightnessController2.mCurrentAutoBrightness);
                    return;
                case 7:
                    HwNormalizedAutomaticBrightnessController.this.updateFullScreenVideoState();
                    return;
                case 8:
                    HwNormalizedAutomaticBrightnessController hwNormalizedAutomaticBrightnessController3 = HwNormalizedAutomaticBrightnessController.this;
                    hwNormalizedAutomaticBrightnessController3.setProximitySensorEnabled(hwNormalizedAutomaticBrightnessController3.mCurrentProximityEnable);
                    return;
                case 9:
                    HwNormalizedAutomaticBrightnessController.this.updateGameDisableAutoBrightnessModeEnableDbMsg();
                    return;
                case 10:
                    HwNormalizedAutomaticBrightnessController hwNormalizedAutomaticBrightnessController4 = HwNormalizedAutomaticBrightnessController.this;
                    hwNormalizedAutomaticBrightnessController4.setAutoBrightnessMode(hwNormalizedAutomaticBrightnessController4.mBrightnessModeSetAutoEnable);
                    return;
                case 11:
                    HwNormalizedAutomaticBrightnessController.this.updateAutoBrightness(11);
                    return;
                case 12:
                    HwNormalizedAutomaticBrightnessController.this.setFrontCameraBrightnessDimmingEnable(false);
                    return;
                default:
                    return;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void updateBrightnessIfNoAmbientLuxReported() {
        if (this.mWakeupFromSleep) {
            this.mWakeupFromSleep = false;
            this.mCallbacks.updateBrightness();
            this.mFirstAutoBrightness = false;
            this.mUpdateAutoBrightnessCount++;
            if (HWFLOW) {
                Slog.i(TAG, "sensor doesn't report lux in 200ms");
            }
        }
    }

    public void updateCurrentUserId(int userId) {
        if (userId != this.mCurrentUserId) {
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "user change from  " + this.mCurrentUserId + " into " + userId);
            }
            this.mCurrentUserId = userId;
            this.mCurrentUserChanging = true;
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(1);
            this.mAmbientLuxValid = false;
            this.mHwAmbientLuxFilterAlgo.clear();
            HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
            if (hwNormalizedSpline != null) {
                hwNormalizedSpline.updateCurrentUserId(userId);
            }
            this.mCurrentUserChanging = false;
            updateDcMode();
        }
    }

    private void updateProximitySensorEnabledMsg(boolean enable) {
        if (this.mCurrentProximityEnable != enable) {
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "proximity sensor updateProximitySensorEnabledMsg,enable=" + enable);
            }
            this.mCurrentProximityEnable = enable;
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(8);
            this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessage(8);
        }
    }

    /* access modifiers changed from: private */
    public void setProximitySensorEnabled(boolean enable) {
        if (enable) {
            if (!this.mProximitySensorEnabled) {
                if (HWFLOW) {
                    Slog.i(TAG, "open proximity sensor start ...");
                }
                this.mProximitySensorEnabled = true;
                this.mSensorManager.registerListener(this.mProximitySensorListener, this.mProximitySensor, 3, this.mHwNormalizedAutomaticBrightnessHandler);
                if (HWFLOW) {
                    Slog.i(TAG, "open proximity sensor");
                }
            }
        } else if (this.mProximitySensorEnabled) {
            this.mProximitySensorEnabled = false;
            this.mProximity = -1;
            this.mProximityForCallBack = -1;
            this.mPendingProximity = -1;
            this.mSensorManager.unregisterListener(this.mProximitySensorListener);
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(3);
            this.mCallbacks.updateProximityState(false);
            if (HWFLOW) {
                Slog.i(TAG, "close proximity sensor");
            }
        }
    }

    private void processProximityStateForCallBack() {
        if (this.mProximityForCallBack != this.mProximity) {
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "HwBrightnessProximity mProximityForCallBack=" + this.mProximityForCallBack + "-->mProximity=" + this.mProximity);
            }
            if (this.mProximityForCallBack == 1 && this.mProximity == 0) {
                this.mFirstBrightnessAfterProximityNegative = true;
            }
            this.mProximityForCallBack = this.mProximity;
            int i = this.mProximityForCallBack;
            if (i == 1) {
                this.mCallbacks.updateProximityState(true);
            } else if (i == 0) {
                this.mCallbacks.updateProximityState(false);
            }
        }
    }

    /* access modifiers changed from: private */
    public void handleProximitySensorEvent() {
        handleProximitySensorEventInternal(this.mProximityReportTime, this.mProximityPositive);
        processProximityStateForCallBack();
    }

    private void handleProximitySensorEventInternal(long time, boolean positive) {
        if (this.mPendingProximity == 0 && !positive) {
            return;
        }
        if (this.mPendingProximity != 1 || !positive) {
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(3);
            if (positive) {
                this.mPendingProximity = 1;
                this.mPendingProximityDebounceTime = ((long) this.mData.proximityPositiveDebounceTime) + time;
            } else {
                this.mPendingProximity = 0;
                this.mPendingProximityDebounceTime = ((long) this.mData.proximityNegativeDebounceTime) + time;
            }
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "HwBrightnessProximity mPendingProximity=" + this.mPendingProximity + ",time=" + time + ",mPendingProximityDebounceTime=" + this.mPendingProximityDebounceTime);
            }
            debounceProximitySensorStateInternal();
        }
    }

    private void debounceProximitySensorStateInternal() {
        if (this.mPendingProximity != -1 && this.mPendingProximityDebounceTime >= 0) {
            long now = SystemClock.uptimeMillis();
            long j = this.mPendingProximityDebounceTime;
            if (j <= now) {
                if (this.mProximity != this.mPendingProximity) {
                    if (HWFLOW) {
                        String str = TAG;
                        Slog.i(str, "HwBrightnessProximity mProximity=" + this.mProximity + "-->mPendingProximity=" + this.mPendingProximity);
                    }
                    this.mProximity = this.mPendingProximity;
                }
                HwAmbientLuxFilterAlgo hwAmbientLuxFilterAlgo = this.mHwAmbientLuxFilterAlgo;
                boolean z = true;
                if (this.mProximity != 1) {
                    z = false;
                }
                hwAmbientLuxFilterAlgo.setProximityState(z);
                clearPendingProximityDebounceTime();
                return;
            }
            this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessageAtTime(3, j);
            if (HWDEBUG) {
                String str2 = TAG;
                Slog.d(str2, "HwBrightnessProximity MSG_PROXIMITY_SENSOR_DEBOUNCED.,mPendingTime=" + this.mPendingProximityDebounceTime);
            }
        }
    }

    private void setProximityState(boolean isProximityPositiveState) {
        HwAmbientLuxFilterAlgo hwAmbientLuxFilterAlgo = this.mHwAmbientLuxFilterAlgo;
        if (hwAmbientLuxFilterAlgo == null) {
            String str = TAG;
            Slog.w(str, "mHwAmbientLuxFilterAlgo==null, isProximityPositiveState=" + isProximityPositiveState);
            return;
        }
        hwAmbientLuxFilterAlgo.setProximityState(isProximityPositiveState);
    }

    private void clearPendingProximityDebounceTime() {
        if (this.mPendingProximityDebounceTime >= 0) {
            this.mPendingProximityDebounceTime = -1;
        }
    }

    /* access modifiers changed from: private */
    public void debounceProximitySensor() {
        debounceProximitySensorStateInternal();
        processProximityStateForCallBack();
    }

    public void updatePowerPolicy(int policy) {
        boolean powerOnEnable = wantScreenOn(policy);
        if (powerOnEnable != this.mPowerOnEnable) {
            updateNightUpModeTime(powerOnEnable);
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

    private void updateDarkenAmbientEnable() {
        if (this.mData.nightUpModeEnable) {
            if (this.mAmbientLux < this.mData.nightUpModeLuxThreshold) {
                this.mIsDarkenAmbientEnable = true;
                return;
            }
            this.mIsDarkenAmbientEnable = false;
            if (this.mNightUpTimeEnable) {
                this.mNightUpTimeEnable = false;
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "NightUpBrightMode mNightUpTimeEnable set false, mAmbientLux=" + this.mAmbientLux);
                }
            }
        }
    }

    private void updateNightUpModeTime(boolean powerOnEnable) {
        if (this.mData.nightUpModeEnable) {
            if (powerOnEnable) {
                this.mNightUpModePowerOnTimestamp = SystemClock.elapsedRealtime();
                this.mNightUpModePowerOnOffTimeDelta = this.mNightUpModePowerOnTimestamp - this.mNightUpModePowerOffTimestamp;
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "NightUpBrightMode mPowerOnOffTimeDelta=" + this.mNightUpModePowerOnOffTimeDelta + ",nightUpModeSwitchTimeMillis=" + this.mData.nightUpModeSwitchTimeMillis);
                }
                if (this.mNightUpModePowerOnOffTimeDelta >= ((long) this.mData.nightUpModeSwitchTimeMillis) || this.mNightUpTimeFirstCheckEnable) {
                    if (this.mNightUpTimeFirstCheckEnable) {
                        this.mNightUpTimeFirstCheckEnable = false;
                    }
                    updateCurrentTimeForNightUpMode();
                    return;
                }
                return;
            }
            this.mNightUpModePowerOffTimestamp = SystemClock.elapsedRealtime();
        }
    }

    private void updateCurrentTimeForNightUpMode() {
        int currentHour = Calendar.getInstance().get(11);
        this.mNightUpTimeEnable = false;
        if (this.mData.nightUpModeBeginHourTime < this.mData.nightUpModeEndHourTime) {
            if (currentHour >= this.mData.nightUpModeBeginHourTime && currentHour < this.mData.nightUpModeEndHourTime) {
                this.mNightUpTimeEnable = true;
            }
        } else if (currentHour >= this.mData.nightUpModeBeginHourTime || currentHour < this.mData.nightUpModeEndHourTime) {
            this.mNightUpTimeEnable = true;
        }
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "NightUpBrightMode updateCurrentTimeForNightUpMode, mNightUpTimeEnable=" + this.mNightUpTimeEnable);
        }
    }

    public boolean getNightUpPowerOnWithDimmingEnable() {
        boolean dimmingEnable = false;
        if (!this.mData.nightUpModeEnable) {
            return false;
        }
        if (this.mNightUpTimeEnable && this.mIsDarkenAmbientEnable) {
            dimmingEnable = true;
        }
        if (HWDEBUG) {
            String str = TAG;
            Slog.i(str, "NightUpBrightMode mNightUpTimeEnable=" + this.mNightUpTimeEnable + ",mIsDarkenAmbientEnable=" + this.mIsDarkenAmbientEnable);
        }
        return dimmingEnable;
    }

    private static boolean wantScreenOn(int state) {
        if (state == 2 || state == 3) {
            return true;
        }
        return false;
    }

    private boolean needToSetBrightnessBaseProximity() {
        boolean needToSet = true;
        if (!this.mBrightnessEnlarge && this.mBrightnessNoLimitSetByApp > 0) {
            this.mBrightnessEnlarge = true;
            String str = TAG;
            Slog.i(str, "set mBrightnessEnlarge=" + this.mBrightnessEnlarge + ",mBrightnessNoLimitSetByApp=" + this.mBrightnessNoLimitSetByApp);
        }
        if (!this.mBrightnessEnlarge && this.mWakeupForFirstAutoBrightness && this.mProximity == 1) {
            this.mBrightnessEnlarge = true;
            if (HWFLOW) {
                String str2 = TAG;
                Slog.i(str2, "mWakeupForFirstAutoBrightness set mBrightnessEnlarge=" + this.mBrightnessEnlarge);
            }
        }
        if (this.mProximity != 1 || this.mBrightnessEnlarge || this.mUpdateAutoBrightnessCount <= 1 || this.mPowerPolicy == 2 || this.mPolicyChangeFromDim) {
            needToSet = false;
        }
        if (HWFLOW && needToSet) {
            String str3 = TAG;
            Slog.i(str3, "mProximity= " + this.mProximity + ",mBrightnessEnlarge=" + this.mBrightnessEnlarge + ",mUpdateAutoBrightnessCount=" + this.mUpdateAutoBrightnessCount + ",mPowerPolicy=" + this.mPowerPolicy + ",mPolicyChangeFromDim=" + this.mPolicyChangeFromDim);
        }
        return needToSet;
    }

    public void setSplineEyeProtectionControlFlag(boolean flag) {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline != null) {
            hwNormalizedSpline.setEyeProtectionControlFlag(flag);
        }
    }

    /* access modifiers changed from: private */
    public void setReadingModeBrightnessLineEnable(boolean readingMode) {
        if (sHwNormalizedScreenAutoBrightnessSpline != null && getReadingModeBrightnessLineEnable()) {
            sHwNormalizedScreenAutoBrightnessSpline.setReadingModeEnable(readingMode);
            if ((readingMode || !this.mReadingModeEnable) && (!readingMode || this.mReadingModeEnable)) {
                this.mReadingModeChangeAnimationEnable = false;
            } else {
                this.mReadingModeChangeAnimationEnable = true;
                updateAutoBrightness(true, false);
            }
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "setReadingModeControlFlag: " + readingMode + ", mReadingModeChangeAnimationEnable: " + this.mReadingModeChangeAnimationEnable);
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

    public void setCoverModeStatus(boolean isClosed) {
        if (isClosed) {
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(4);
        }
        if (!isClosed && this.mIsClosed) {
            this.mCoverStateFast = true;
            this.mHwAmbientLuxFilterAlgo.setCoverModeFastResponseFlag(this.mCoverStateFast);
            this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessageDelayed(4, (long) this.mCoverModeFastResponseTimeDelay);
        }
        this.mIsClosed = isClosed;
        this.mHwAmbientLuxFilterAlgo.setCoverModeStatus(isClosed);
    }

    /* access modifiers changed from: private */
    public void setCoverModeFastResponseFlag() {
        this.mCoverStateFast = false;
        this.mHwAmbientLuxFilterAlgo.setCoverModeFastResponseFlag(this.mCoverStateFast);
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline != null) {
            hwNormalizedSpline.setNoOffsetEnable(this.mCoverStateFast);
        }
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "LabcCoverMode FastResponseFlag =" + this.mCoverStateFast);
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

    private int getPowerOnFastResponseLuxNum() {
        return this.mData.powerOnFastResponseLuxNum;
    }

    private boolean getCameraModeBrightnessLineEnable() {
        return this.mData.cameraModeEnable;
    }

    public boolean getReadingModeBrightnessLineEnable() {
        return this.mData.readingModeEnable;
    }

    public void setCameraModeBrightnessLineEnable(boolean cameraModeEnable) {
        if (sHwNormalizedScreenAutoBrightnessSpline != null && getCameraModeBrightnessLineEnable()) {
            sHwNormalizedScreenAutoBrightnessSpline.setCameraModeEnable(cameraModeEnable);
            if (this.mCameraModeEnable != cameraModeEnable) {
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "CameraModeEnable change cameraModeEnable=" + cameraModeEnable);
                }
                this.mCameraModeChangeAnimationEnable = true;
                updateAutoBrightness(true, false);
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
        boolean lastModeEnable = this.mReadingModeChangeAnimationEnable;
        this.mReadingModeChangeAnimationEnable = false;
        return lastModeEnable;
    }

    public void setKeyguardLockedStatus(boolean isLocked) {
        HwAmbientLuxFilterAlgo hwAmbientLuxFilterAlgo = this.mHwAmbientLuxFilterAlgo;
        if (hwAmbientLuxFilterAlgo == null) {
            Slog.e(TAG, "mHwAmbientLuxFilterAlgo=null");
        } else {
            hwAmbientLuxFilterAlgo.setKeyguardLockedStatus(isLocked);
        }
    }

    public boolean getRebootAutoModeEnable() {
        String enterpriseCotaVersion = SystemProperties.get("ro.product.EcotaVersion", "");
        if (enterpriseCotaVersion == null || enterpriseCotaVersion.isEmpty() || Settings.System.getIntForUser(this.mContext.getContentResolver(), HW_CUSTOMIZATION_SCREEN_BRIGHTNESS_MODE, 1, this.mCurrentUserId) != 0) {
            return this.mData.rebootAutoModeEnable;
        }
        Slog.i(TAG, "Brightness mode has been customized to manual mode");
        return false;
    }

    public boolean getOutdoorAnimationFlag() {
        return this.mHwAmbientLuxFilterAlgo.getOutdoorAnimationFlag();
    }

    public int getCoverModeBrightnessFromLastScreenBrightness() {
        if (this.mData.backSensorCoverModeEnable) {
            return -3;
        }
        int brightnessMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 1, this.mCurrentUserId);
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
        int brightnessOut;
        if (brightness == -1 || !this.mData.manualMode || brightness > 255) {
            HwBrightnessPgSceneDetection.setQRCodeAppBrightnessNoPowerSaving(false);
            return brightness;
        }
        if (brightness < 4) {
            HwBrightnessPgSceneDetection.setQRCodeAppBrightnessNoPowerSaving(false);
            brightnessOut = 4;
            if (HWFLOW && this.mBrightnessOutForLog != 4) {
                this.mBrightnessOutForLog = 4;
                String str = TAG;
                Slog.i(str, "mScreenBrightnessOverrideFromWindowManagerMapping brightnessIn=" + brightness + ",brightnessOut=" + 4);
            }
        } else {
            if (this.mData.QRCodeBrightnessminLimit <= 0 || !this.mHwBrightnessPgSceneDetection.isQrCodeAppBoostBrightness(brightness) || this.mIsVideoPlay) {
                HwBrightnessPgSceneDetection.setQRCodeAppBrightnessNoPowerSaving(false);
                brightnessOut = (((brightness - 4) * (this.mData.manualBrightnessMaxLimit - 4)) / 251) + 4;
            } else {
                HwBrightnessPgSceneDetection.setQRCodeAppBrightnessNoPowerSaving(true);
                brightnessOut = this.mData.manualBrightnessMaxLimit;
                if (HWFLOW && this.mBrightnessOutForLog != brightnessOut) {
                    String str2 = TAG;
                    Slog.i(str2, "QrCodeBrightness=" + brightness + "-->brightnessOut=" + brightnessOut);
                }
            }
            if (!(brightness == brightnessOut || this.mBrightnessOutForLog == brightnessOut)) {
                this.mBrightnessOutForLog = brightnessOut;
                String str3 = TAG;
                Slog.i(str3, "mScreenBrightnessOverrideFromWindowManagerMapping brightnessIn=" + brightness + ",brightnessOut=" + brightnessOut);
            }
        }
        return brightnessOut;
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
            Slog.i(str, "ThermalMode set auto MaxBrightness=" + brightness + ",mappingBrightness=" + mappingBrightness);
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
                Slog.i(str, "Cryogenic set auto MaxBrightness=" + brightness + ",mMaxBrightnessSetByCryogenic=" + this.mMaxBrightnessSetByCryogenic);
                this.mCallbacks.updateBrightness();
            }
        }
    }

    public boolean getRebootFirstBrightnessAnimationEnable() {
        return this.mData.rebootFirstBrightnessAnimationEnable;
    }

    /* access modifiers changed from: protected */
    public int getAdjustLightValByPgMode(int rawLightVal) {
        HwBrightnessPgSceneDetection hwBrightnessPgSceneDetection = this.mHwBrightnessPgSceneDetection;
        if (hwBrightnessPgSceneDetection != null) {
            int mPgBrightness = hwBrightnessPgSceneDetection.getAdjustLightValByPgMode(rawLightVal);
            if (this.mData.pgModeBrightnessMappingEnable && rawLightVal > this.mData.manualBrightnessMaxLimit) {
                mPgBrightness = this.mHwBrightnessMapping.getMappingBrightnessForRealNit(mPgBrightness);
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "PG_POWER_SAVE_MODE auto mPgBrightness=" + mPgBrightness + ",rawLightVal=" + rawLightVal);
                }
            }
            return mPgBrightness;
        }
        Slog.w(TAG, "mHwBrightnessPgSceneDetection=null");
        return rawLightVal;
    }

    public int getPowerSavingBrightness(int brightness) {
        HwNormalizedSpline hwNormalizedSpline;
        HwNormalizedSpline hwNormalizedSpline2 = sHwNormalizedScreenAutoBrightnessSpline;
        if ((hwNormalizedSpline2 == null || hwNormalizedSpline2.getPersonalizedBrightnessCurveEnable()) && this.mHwBrightnessPowerSavingCurve != null && (hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline) != null && hwNormalizedSpline.getPowerSavingBrighnessLineEnable()) {
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
            Slog.i(str, "setBrightnessNoLimit set auto Brightness=" + brightness + ",time=" + time);
            this.mCallbacks.updateBrightness();
        }
    }

    /* access modifiers changed from: protected */
    public boolean setLightSensorEnabled(boolean enable) {
        if (enable) {
            if (!this.mLightSensorEnabled) {
                if (HWFLOW) {
                    Slog.i(TAG, "Enable LightSensor start ...");
                }
                this.mLightSensorEnabled = true;
                this.mFirstAutoBrightness = true;
                this.mUpdateAutoBrightnessCount = 0;
                this.mLightSensorEnableTime = SystemClock.uptimeMillis();
                this.mLightSensorEnableElapsedTimeNanos = SystemClock.elapsedRealtimeNanos();
                this.mCurrentLightSensorRate = this.mInitialLightSensorRate;
                registerSensor(this.mInitialLightSensorRate);
                if (this.mWakeupFromSleep) {
                    this.mHandler.sendEmptyMessageAtTime(6, this.mLightSensorEnableTime + 200);
                }
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "Enable LightSensor at time:mLightSensorEnableTime=" + SystemClock.uptimeMillis() + ",mLightSensorEnableElapsedTimeNanos=" + this.mLightSensorEnableElapsedTimeNanos);
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
            this.mCurrentLightSensorRate = -1;
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(6);
            if (this.mData.frontCameraMaxBrightnessEnable && this.mFrontCameraDimmingEnable) {
                this.mFrontCameraDimmingEnable = false;
            }
            unregisterSensor();
            this.mAmbientLuxValid = !this.mResetAmbientLuxAfterWarmUpConfig;
            if (HWFLOW) {
                String str2 = TAG;
                Slog.i(str2, "Disable LightSensor at time:" + SystemClock.uptimeMillis());
            }
        }
        return false;
    }

    private void registerSensor(int sensorRate) {
        int i = this.mSensorOption;
        HwDualSensorEventListenerImpl hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
        if (i == -1) {
            this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, sensorRate * 1000, (Handler) this.mHandler);
        } else if (i == 0) {
            this.mSensorObserver = new SensorObserver();
            this.mHwDualSensorEventListenerImpl.attachFrontSensorData(this.mSensorObserver);
        } else if (i == 1) {
            this.mSensorObserver = new SensorObserver();
            this.mHwDualSensorEventListenerImpl.attachBackSensorData(this.mSensorObserver);
        } else if (i == 2) {
            this.mSensorObserver = new SensorObserver();
            this.mHwDualSensorEventListenerImpl.attachFusedSensorData(this.mSensorObserver);
        } else {
            String str = TAG;
            Slog.w(str, "mSensorOption is not valid, no register sensor, mSensorOption=" + this.mSensorOption);
        }
    }

    private void unregisterSensor() {
        int i = this.mSensorOption;
        HwDualSensorEventListenerImpl hwDualSensorEventListenerImpl = this.mHwDualSensorEventListenerImpl;
        if (i == -1) {
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
        } else if (i == 0) {
            SensorObserver sensorObserver = this.mSensorObserver;
            if (sensorObserver != null) {
                hwDualSensorEventListenerImpl.detachFrontSensorData(sensorObserver);
            }
        } else if (i == 1) {
            SensorObserver sensorObserver2 = this.mSensorObserver;
            if (sensorObserver2 != null) {
                hwDualSensorEventListenerImpl.detachBackSensorData(sensorObserver2);
            }
        } else if (i == 2) {
            SensorObserver sensorObserver3 = this.mSensorObserver;
            if (sensorObserver3 != null) {
                hwDualSensorEventListenerImpl.detachFusedSensorData(sensorObserver3);
            }
        } else {
            String str = TAG;
            Slog.w(str, "mSensorOption is not valid, no unregister sensor, mSensorOption=" + this.mSensorOption);
        }
    }

    private boolean handleDarkAdaptDetector(float lux) {
        HwNormalizedSpline.DarkAdaptState splineDarkAdaptState;
        if (this.mDarkAdaptDetector == null || this.mCoverStateFast || this.mHwAmbientLuxFilterAlgo.getProximityPositiveEnable() || sHwNormalizedScreenAutoBrightnessSpline == null) {
            return false;
        }
        this.mDarkAdaptDetector.updateLux(lux, this.mAmbientLux);
        DarkAdaptDetector.AdaptState newState = this.mDarkAdaptDetector.getState();
        DarkAdaptDetector.AdaptState adaptState = this.mDarkAdaptState;
        if (adaptState == newState) {
            return false;
        }
        if (adaptState == DarkAdaptDetector.AdaptState.UNADAPTED && newState == DarkAdaptDetector.AdaptState.ADAPTING) {
            this.mDarkAdaptDimmingEnable = true;
        } else {
            this.mDarkAdaptDimmingEnable = false;
        }
        this.mDarkAdaptState = newState;
        int i = AnonymousClass6.$SwitchMap$com$android$server$display$DarkAdaptDetector$AdaptState[newState.ordinal()];
        if (i == 1) {
            splineDarkAdaptState = HwNormalizedSpline.DarkAdaptState.UNADAPTED;
        } else if (i == 2) {
            splineDarkAdaptState = HwNormalizedSpline.DarkAdaptState.ADAPTING;
        } else if (i != 3) {
            splineDarkAdaptState = null;
        } else {
            splineDarkAdaptState = HwNormalizedSpline.DarkAdaptState.ADAPTED;
        }
        sHwNormalizedScreenAutoBrightnessSpline.setDarkAdaptState(splineDarkAdaptState);
        return true;
    }

    /* renamed from: com.android.server.display.HwNormalizedAutomaticBrightnessController$6  reason: invalid class name */
    static /* synthetic */ class AnonymousClass6 {
        static final /* synthetic */ int[] $SwitchMap$com$android$server$display$DarkAdaptDetector$AdaptState = new int[DarkAdaptDetector.AdaptState.values().length];

        static {
            try {
                $SwitchMap$com$android$server$display$DarkAdaptDetector$AdaptState[DarkAdaptDetector.AdaptState.UNADAPTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$server$display$DarkAdaptDetector$AdaptState[DarkAdaptDetector.AdaptState.ADAPTING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$server$display$DarkAdaptDetector$AdaptState[DarkAdaptDetector.AdaptState.ADAPTED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private void unlockDarkAdaptLine() {
        if (this.mDarkAdaptDetector != null && sHwNormalizedScreenAutoBrightnessSpline != null && this.mHwAmbientLuxFilterAlgo.getOffsetResetEnable()) {
            sHwNormalizedScreenAutoBrightnessSpline.unlockDarkAdaptLine();
        }
    }

    public boolean getDarkAdaptDimmingEnable() {
        return this.mDarkAdaptDetector != null && this.mDarkAdaptDimmingEnable && this.mLightSensorEnabled;
    }

    public void clearDarkAdaptDimmingEnable() {
        this.mDarkAdaptDimmingEnable = false;
    }

    private class SensorObserver implements Observer {
        public SensorObserver() {
        }

        public void update(Observable o, Object arg) {
            if (arg instanceof long[]) {
                long[] data = (long[]) arg;
                long systemTimeStamp = data[2];
                long sensorTimeStamp = data[3];
                int lux = getfoldScreenLux((int) data[0], data, systemTimeStamp);
                if ((!HwServiceFactory.shouldFilteInvalidSensorVal((float) lux) || AutomaticBrightnessController.INT_BRIGHTNESS_COVER_MODE != 0) && !HwNormalizedAutomaticBrightnessController.this.isInValidLightSensorEvent(sensorTimeStamp, (float) lux)) {
                    HwNormalizedAutomaticBrightnessController.this.handleLightSensorEvent(systemTimeStamp, (float) lux);
                    int access$3200 = HwNormalizedAutomaticBrightnessController.this.mSensorOption;
                    HwDualSensorEventListenerImpl unused = HwNormalizedAutomaticBrightnessController.this.mHwDualSensorEventListenerImpl;
                    if (access$3200 == 2) {
                        int unused2 = HwNormalizedAutomaticBrightnessController.this.mDualSensorRawAmbient = (int) data[4];
                    }
                }
            }
        }

        /* JADX WARNING: Code restructure failed: missing block: B:11:0x002f, code lost:
            if (r5 != 4) goto L_0x0037;
         */
        private int getfoldScreenLux(int lux, long[] data, long systemTimeStamp) {
            int luxOut = lux;
            int fusedLux = lux;
            int frontLux = lux;
            int backLux = lux;
            if (HwNormalizedAutomaticBrightnessController.this.mData.foldScreenModeEnable && data != null && data.length >= 6) {
                fusedLux = (int) data[0];
                frontLux = (int) data[4];
                backLux = (int) data[5];
                int access$1300 = HwNormalizedAutomaticBrightnessController.this.mCurrentDisplayMode;
                if (access$1300 != 1) {
                    if (access$1300 == 2) {
                        luxOut = frontLux;
                    } else if (access$1300 == 3) {
                        luxOut = backLux;
                    }
                }
                luxOut = fusedLux;
            }
            if (systemTimeStamp - HwNormalizedAutomaticBrightnessController.this.mLightSensorEnableTime < 2000) {
                if (HwNormalizedAutomaticBrightnessController.this.mData.foldScreenModeEnable) {
                    if (HwNormalizedAutomaticBrightnessController.HWFLOW) {
                        String access$800 = HwNormalizedAutomaticBrightnessController.TAG;
                        Slog.i(access$800, "ambient lux=" + luxOut + ",systemTimeStamp=" + systemTimeStamp + ",frontLux=" + frontLux + ",backLux=" + backLux + ",fusedLux=" + fusedLux + ",disMode=" + HwNormalizedAutomaticBrightnessController.this.mCurrentDisplayMode);
                    }
                } else if (HwNormalizedAutomaticBrightnessController.HWFLOW) {
                    String access$8002 = HwNormalizedAutomaticBrightnessController.TAG;
                    Slog.i(access$8002, "ambient lux=" + luxOut + ",systemTimeStamp=" + systemTimeStamp);
                }
            }
            return luxOut;
        }
    }

    public void getUserDragInfo(Bundle data) {
        if (sHwNormalizedScreenAutoBrightnessSpline != null) {
            int targetBrightness = getAutomaticScreenBrightness();
            int i = -3;
            if (this.mMaxBrightnessSetByCryogenic < 255) {
                targetBrightness = targetBrightness >= this.mMaxBrightnessSetByCryogenic ? -3 : targetBrightness;
                String str = TAG;
                Slog.i(str, "mMaxBrightnessSetByCryogenic=" + this.mMaxBrightnessSetByCryogenic);
            }
            if (this.mMaxBrightnessSetByThermal < 255) {
                if (targetBrightness < this.mMaxBrightnessSetByThermal) {
                    i = targetBrightness;
                }
                targetBrightness = i;
                String str2 = TAG;
                Slog.i(str2, "mMaxBrightnessSetByThermal=" + this.mMaxBrightnessSetByThermal);
            }
            String str3 = TAG;
            Slog.i(str3, "getUserDragInfo startBL=" + this.mScreenBrightnessBeforeAdj + ", targetBrightness=" + targetBrightness);
            boolean isDeltaValid = sHwNormalizedScreenAutoBrightnessSpline.isDeltaValid();
            sHwNormalizedScreenAutoBrightnessSpline.resetUserDragLimitFlag();
            data.putBoolean("DeltaValid", isDeltaValid && !this.mDragFinished);
            data.putInt("StartBrightness", this.mScreenBrightnessBeforeAdj);
            data.putInt("EndBrightness", targetBrightness);
            data.putInt("FilteredAmbientLight", (int) (this.mAmbientLux + 0.5f));
            data.putBoolean("ProximityPositive", this.mProximityPositive);
            this.mDragFinished = true;
        }
    }

    public void setVideoPlayStatus(boolean isVideoPlay) {
        this.mIsVideoPlay = isVideoPlay;
        String str = TAG;
        Slog.i(str, "setVideoPlayStatus, mIsVideoPlay= " + this.mIsVideoPlay);
        if (this.mData.videoFullScreenModeEnable) {
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(7);
            this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessageDelayed(7, (long) this.mData.videoFullScreenModeDelayTime);
        }
    }

    /* access modifiers changed from: private */
    public void updateFullScreenVideoState() {
        boolean fullScreenVideoState = false;
        if (this.mIsVideoPlay && this.mVideoModeEnable) {
            fullScreenVideoState = true;
        }
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "updateFullScreenVideoState=" + fullScreenVideoState + ",mIsVideoPlay=" + this.mIsVideoPlay + ",mVideoModeEnable=" + this.mVideoModeEnable);
        }
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline != null) {
            hwNormalizedSpline.setVideoFullScreenModeBrightnessEnable(fullScreenVideoState);
        } else {
            Slog.e(TAG, "setVideoFullScreenModeBrightnessEnable fail, sHwNormalizedScreenAutoBrightnessSpline=null");
        }
    }

    public void registerCryogenicProcessor(CryogenicPowerProcessor processor) {
        this.mCryogenicProcessor = processor;
    }

    public float getAutomaticScreenBrightnessAdjustmentNew(int brightness) {
        return MathUtils.constrain(((((float) brightness) * 2.0f) / ((float) this.mData.manualBrightnessMaxLimit)) - 1.0f, (float) DEFAUL_OFFSET_LUX, 1.0f);
    }

    private class LandscapeStateReceiver extends BroadcastReceiver {
        public LandscapeStateReceiver() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.CONFIGURATION_CHANGED");
            filter.setPriority(1000);
            HwNormalizedAutomaticBrightnessController.this.mContext.registerReceiver(this, filter);
        }

        public void onReceive(Context context, Intent intent) {
            if (context == null || intent == null) {
                Slog.e(HwNormalizedAutomaticBrightnessController.TAG, "LandscapeStateReceiver Invalid input parameter!");
            } else if ("android.intent.action.CONFIGURATION_CHANGED".equals(intent.getAction())) {
                int ori = HwNormalizedAutomaticBrightnessController.this.mContext.getResources().getConfiguration().orientation;
                if (ori == 2) {
                    boolean unused = HwNormalizedAutomaticBrightnessController.this.mLandscapeModeState = true;
                } else if (ori == 1) {
                    boolean unused2 = HwNormalizedAutomaticBrightnessController.this.mLandscapeModeState = false;
                } else if (HwNormalizedAutomaticBrightnessController.HWDEBUG) {
                    Slog.d(HwNormalizedAutomaticBrightnessController.TAG, "LandScapeBrightMode ORIENTATION no change");
                }
                if (HwNormalizedAutomaticBrightnessController.HWFLOW) {
                    String access$800 = HwNormalizedAutomaticBrightnessController.TAG;
                    Slog.i(access$800, "LandScapeBrightMode MSG_UPDATE_LANDSCAPE mLandscapeModeState=" + HwNormalizedAutomaticBrightnessController.this.mLandscapeModeState);
                }
                HwNormalizedAutomaticBrightnessController hwNormalizedAutomaticBrightnessController = HwNormalizedAutomaticBrightnessController.this;
                hwNormalizedAutomaticBrightnessController.sendLandscapeStateUpdate(hwNormalizedAutomaticBrightnessController.mLandscapeModeState);
                if (HwNormalizedAutomaticBrightnessController.this.mData.darkModeEnable) {
                    HwNormalizedAutomaticBrightnessController.this.updateDarkMode();
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void sendLandscapeStateUpdate(boolean enable) {
        this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(5);
        if (enable) {
            this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessageDelayed(5, (long) this.mData.landscapeModeEnterDelayTime);
        } else {
            this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessageDelayed(5, (long) this.mData.landscapeModeQuitDelayTime);
        }
        if (HWDEBUG) {
            String str = TAG;
            Slog.i(str, "LandScapeBrightMode MSG_UPDATE_LANDSCAPE mLandscapeModeState=" + this.mLandscapeModeState + ",bTime=" + this.mData.landscapeModeEnterDelayTime + ",dTime=" + this.mData.landscapeModeQuitDelayTime);
        }
    }

    /* access modifiers changed from: private */
    public void updateLandscapeMode(boolean enable) {
        HwAmbientLuxFilterAlgo hwAmbientLuxFilterAlgo = this.mHwAmbientLuxFilterAlgo;
        if (hwAmbientLuxFilterAlgo != null) {
            hwAmbientLuxFilterAlgo.updateLandscapeMode(enable);
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "LandScapeBrightMode real LandScapeState,ModeState=" + enable);
            }
        }
    }

    private void updateTouchProximityState(boolean touchProximityState) {
        if (this.mHwAmbientLuxFilterAlgo != null) {
            if (HWDEBUG) {
                String str = TAG;
                Slog.i(str, "LandScapeBrightMode touchProximityState=" + touchProximityState + ",bypassTouchProximityResult=" + bypassTouchProximityResult());
            }
            this.mHwAmbientLuxFilterAlgo.updateTouchProximityState(touchProximityState);
        }
    }

    private boolean bypassTouchProximityResult() {
        if (this.mData.landscapeModeUseTouchProximity) {
            return !this.mLandscapeModeState;
        }
        return false;
    }

    public void updateBrightnessModeChangeManualState(boolean enable) {
        HwAmbientLuxFilterAlgo hwAmbientLuxFilterAlgo = this.mHwAmbientLuxFilterAlgo;
        if (hwAmbientLuxFilterAlgo != null) {
            hwAmbientLuxFilterAlgo.updateBrightnessModeChangeManualState(enable);
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            HwNormalizedAutomaticBrightnessController.this.handleBrightnessSettingsChange();
        }
    }

    /* access modifiers changed from: private */
    public void handleBrightnessSettingsChange() {
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, this.mCurrentUserId) == 1) {
            handleAutoBrightnessSettingsChange();
        } else {
            handleManualBrightnessSettingsChange();
        }
    }

    private void handleManualBrightnessSettingsChange() {
        int manualBrightnessDb = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", 0, this.mCurrentUserId);
        if (this.mCurrentAutoBrightness != manualBrightnessDb) {
            int autoBrightnessDb = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", 0, this.mCurrentUserId);
            if (autoBrightnessDb != manualBrightnessDb && autoBrightnessDb > 0) {
                Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", manualBrightnessDb, this.mCurrentUserId);
            }
            updateCurrentAutoBrightness(manualBrightnessDb);
            if (HWDEBUG) {
                String str = TAG;
                Slog.i(str, "updateCurrentAutoBrightness from manualBrightnessDb=" + manualBrightnessDb + ",autoBrightnessDb=" + autoBrightnessDb);
            }
            this.mCurrentAutoBrightness = manualBrightnessDb;
        }
    }

    private void handleAutoBrightnessSettingsChange() {
        int i;
        boolean brightnessChageUpStatus = false;
        int autoBrightnessDb = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", 0, this.mCurrentUserId);
        long time = SystemClock.uptimeMillis();
        if (this.mCurrentAutoBrightness != autoBrightnessDb) {
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(6);
            if (autoBrightnessDb > this.mCurrentAutoBrightness) {
                brightnessChageUpStatus = true;
            }
            if (autoBrightnessDb == 0 || (i = this.mCurrentAutoBrightness) == 0 || brightnessChageUpStatus != this.mBrightnessChageUpStatus || ((autoBrightnessDb > i && time - this.mSetCurrentAutoBrightnessTime > ((long) this.mData.brightnessChageUpDelayTime)) || (autoBrightnessDb < this.mCurrentAutoBrightness && time - this.mSetCurrentAutoBrightnessTime > ((long) this.mData.brightnessChageDownDelayTime)))) {
                this.mSetCurrentAutoBrightnessTime = time;
                this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessage(6);
                if (HWFLOW && (autoBrightnessDb == 0 || this.mCurrentAutoBrightness == 0)) {
                    String str = TAG;
                    Slog.i(str, "updateCurrentAutoBrightness now,brightness=" + autoBrightnessDb);
                }
            } else {
                this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessageDelayed(6, (long) this.mData.brightnessChageDefaultDelayTime);
            }
            this.mCurrentAutoBrightness = autoBrightnessDb;
            this.mBrightnessChageUpStatus = brightnessChageUpStatus;
        }
    }

    public void setProximitySceneMode(boolean enable) {
        if (this.mData.proximitySceneModeEnable && enable != this.mIsProximitySceneModeOpened) {
            if (HWDEBUG) {
                String str = TAG;
                Slog.i(str, "setProximitySceneMode enable=" + enable + "mData.proximitySceneModeEnable =" + this.mData.proximitySceneModeEnable);
            }
            this.mIsProximitySceneModeOpened = enable;
            setProximitySensorEnabled(enable);
            if (!enable) {
                this.mHwAmbientLuxFilterAlgo.setProximityState(false);
            }
        }
    }

    private final class DcModeObserver extends ContentObserver {
        public DcModeObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            HwNormalizedAutomaticBrightnessController.this.handleDcModeSettingsChange();
        }
    }

    /* access modifiers changed from: private */
    public void handleDcModeSettingsChange() {
        updateDcMode();
    }

    private void updateDcMode() {
        int dcMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_DC_BRIGHTNESS_DIMMING_SWITCH, 0, this.mCurrentUserId);
        boolean z = true;
        if (dcMode != 1) {
            z = false;
        }
        this.mDcModeBrightnessEnable = z;
        HwAmbientLuxFilterAlgo hwAmbientLuxFilterAlgo = this.mHwAmbientLuxFilterAlgo;
        if (hwAmbientLuxFilterAlgo != null) {
            hwAmbientLuxFilterAlgo.setDcModeBrightnessEnable(this.mDcModeBrightnessEnable);
        }
        if (HWFLOW) {
            Slog.i(TAG, "DcModeBrightnessEnable=" + this.mDcModeBrightnessEnable + ",dcMode=" + dcMode);
        }
    }

    /* access modifiers changed from: private */
    public void updateCurrentAutoBrightness(int brightness) {
        if (this.mHwAmbientLuxFilterAlgo != null) {
            if (HWDEBUG) {
                String str = TAG;
                Slog.i(str, "updateCurrentAutoBrightness realBrightness=" + brightness);
            }
            this.mHwAmbientLuxFilterAlgo.setCurrentAutoBrightness(brightness);
        }
    }

    public boolean getFastDarkenDimmingEnable() {
        int brightnessMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, this.mCurrentUserId);
        HwAmbientLuxFilterAlgo hwAmbientLuxFilterAlgo = this.mHwAmbientLuxFilterAlgo;
        if (hwAmbientLuxFilterAlgo == null || brightnessMode != 1) {
            return false;
        }
        return hwAmbientLuxFilterAlgo.getFastDarkenDimmingEnable();
    }

    public boolean getKeyguardUnlockedFastDarkenDimmingEnable() {
        int brightnessMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, this.mCurrentUserId);
        HwAmbientLuxFilterAlgo hwAmbientLuxFilterAlgo = this.mHwAmbientLuxFilterAlgo;
        if (hwAmbientLuxFilterAlgo == null || brightnessMode != 1) {
            return false;
        }
        return hwAmbientLuxFilterAlgo.getKeyguardUnlockedFastDarkenDimmingEnable();
    }

    private void setFoldDisplayModeEnable(boolean enable) {
        if (enable) {
            if (!this.mDisplayModeListenerEnabled) {
                this.mDisplayModeListenerEnabled = true;
                HwFoldScreenManagerEx.registerFoldDisplayMode(this.mFoldDisplayModeListener);
                this.mCurrentDisplayMode = HwFoldScreenManagerEx.getDisplayMode();
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "open FoldDisplayModeListener,mCurrentDisplayMode=" + this.mCurrentDisplayMode);
                }
            }
        } else if (this.mDisplayModeListenerEnabled) {
            this.mDisplayModeListenerEnabled = false;
            HwFoldScreenManagerEx.unregisterFoldDisplayMode(this.mFoldDisplayModeListener);
            if (HWFLOW) {
                Slog.i(TAG, "close FoldDisplayModeListener");
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateDarkMode() {
        int currentDarkMode = getCurrentDarkMode();
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "DarkBrightMode currentDarkMode=" + currentDarkMode);
        }
        if ((currentDarkMode == 32 && this.mCurrentDarkMode != 32) || (this.mCurrentDarkMode == 32 && currentDarkMode != 32)) {
            if (currentDarkMode == 32) {
                updateDarkModeBrightness(true, this.mData.darkModeOffsetMinBrightness);
            } else {
                updateDarkModeBrightness(false, 4);
            }
            String str2 = TAG;
            Slog.i(str2, "DarkBrightMode updateDarkMode, currentDarkMode=" + currentDarkMode);
            updateAutoBrightness(true, false);
        }
        if (currentDarkMode == 32) {
            this.mCurrentDarkMode = 32;
        } else {
            this.mCurrentDarkMode = 16;
        }
    }

    private int getCurrentDarkMode() {
        Context context = this.mContext;
        if (context == null) {
            return 16;
        }
        return context.getResources().getConfiguration().uiMode & 48;
    }

    private void updateDarkModeBrightness(boolean isDarkModeEnable, int minOffsetBrightness) {
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "DarkBrightMode, isDarkModeEnable=" + isDarkModeEnable + ",minOffsetBrightness=" + minOffsetBrightness);
        }
        HwAmbientLuxFilterAlgo hwAmbientLuxFilterAlgo = this.mHwAmbientLuxFilterAlgo;
        if (hwAmbientLuxFilterAlgo != null) {
            hwAmbientLuxFilterAlgo.updateDarkModeEnable(isDarkModeEnable);
        }
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline != null) {
            hwNormalizedSpline.updateDarkModeBrightness(isDarkModeEnable, minOffsetBrightness);
        }
    }

    @Override // com.android.server.display.HwBrightnessBatteryDetection.Callbacks
    public void updateBrightnessFromBattery(int lowBatteryMaxBrightness) {
        this.mLowBatteryMaxBrightness = lowBatteryMaxBrightness;
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "mLowBatteryMaxBrightness = " + this.mLowBatteryMaxBrightness);
        }
        if (this.mLightSensorEnabled) {
            this.mCallbacks.updateBrightness();
        }
    }

    private void updateLuxMinMaxBrightness(float lux) {
        this.mLuxMinBrightness = calculateOffsetMinBrightness(lux, 2);
        this.mLuxMaxBrightness = calculateOffsetMaxBrightness(lux, 2);
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "updateLuxMinMaxBrightness, lux = " + lux + ",mLuxMinBrightness= " + this.mLuxMinBrightness + ",mLuxMaxBrightness=" + this.mLuxMaxBrightness);
        }
    }

    public int getCurrentBrightnessNit() {
        Bundle data = new Bundle();
        int[] result = new int[1];
        Light light = getLight();
        if (light == null) {
            return 0;
        }
        int currentBrightnessNit = 0;
        if (light.getHwBrightnessData("CurrentBrightness", data, result)) {
            int currentBrightness = data.getInt("Brightness");
            currentBrightnessNit = this.mHwBrightnessMapping.convertBrightnessLevelToNit(currentBrightness);
            if (HWDEBUG) {
                String str = TAG;
                Slog.i(str, "currentBrightness=" + currentBrightness + ",currentBrightnessNit=" + currentBrightnessNit);
            }
        }
        return currentBrightnessNit;
    }

    public int getMinBrightnessNit() {
        return (int) this.mData.screenBrightnessMinNit;
    }

    public int getMaxBrightnessNit() {
        return (int) this.mData.screenBrightnessMaxNit;
    }

    public void setMaxBrightnessNitFromThermal(int brightnessNit) {
        float brightnessNitTmp = (float) brightnessNit;
        if (brightnessNitTmp < this.mData.screenBrightnessMinNit && brightnessNitTmp > 0.0f) {
            String str = TAG;
            Slog.w(str, "ThermalMode brightnessNit=" + brightnessNit + " < minNit=" + this.mData.screenBrightnessMinNit);
            brightnessNitTmp = this.mData.screenBrightnessMinNit;
        }
        if (brightnessNitTmp > this.mData.screenBrightnessMaxNit) {
            String str2 = TAG;
            Slog.w(str2, "ThermalMode brightnessNit=" + brightnessNit + " > maxNit=" + this.mData.screenBrightnessMaxNit);
            float brightnessNitTmp2 = this.mData.screenBrightnessMaxNit;
        }
        int maxBrightnessLevel = this.mHwBrightnessMapping.convertBrightnessNitToLevel(brightnessNit);
        if (HWFLOW) {
            String str3 = TAG;
            Slog.i(str3, "ThermalMode setMaxBrightnessNitFromThermal brightnessNit=" + brightnessNit + "-->maxBrightnessLevel=" + maxBrightnessLevel);
        }
        setMaxBrightnessFromThermal(maxBrightnessLevel);
    }

    private static Light getLight() {
        if (sLight == null) {
            sLight = ((LightsManager) LocalServices.getService(LightsManager.class)).getLight(0);
        }
        return sLight;
    }

    public int getAmbientLux() {
        HwAmbientLuxFilterAlgo hwAmbientLuxFilterAlgo = this.mHwAmbientLuxFilterAlgo;
        if (hwAmbientLuxFilterAlgo == null) {
            return 0;
        }
        return hwAmbientLuxFilterAlgo.getCurrentFilteredAmbientLux();
    }

    public int getBrightnessLevel(int lux) {
        HwNormalizedSpline hwNormalizedSpline = sHwNormalizedScreenAutoBrightnessSpline;
        if (hwNormalizedSpline == null) {
            return 0;
        }
        return (int) hwNormalizedSpline.getDefaultBrightness((float) lux);
    }

    private void updateGameIsFrontState(int curveLevel) {
        if ((curveLevel == 27 || curveLevel == 26) && this.mData.gameDisableAutoBrightnessModeEnable) {
            this.mGameIsFrontEnable = curveLevel == 27;
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "GameDabMode mGameIsFrontEnable=" + this.mGameIsFrontEnable + ",mGameDisableAutoBrightnessModeEnable=" + this.mGameDisableAutoBrightnessModeEnable + ",mLastBrightnessModeForGame=" + this.mLastBrightnessModeForGame + ",curveLevel=" + curveLevel);
            }
            if (this.mGameDisableAutoBrightnessModeEnable) {
                int currentBrightnessMode = getCurrentBrightnessMode();
                if (this.mGameIsFrontEnable) {
                    backupCurrentBrightnessMode(currentBrightnessMode);
                    if (currentBrightnessMode == 1) {
                        setAutoBrightnessMode(false);
                    }
                } else if (this.mLastBrightnessModeForGame == 1 && currentBrightnessMode == 0) {
                    setAutoBrightnessMode(true);
                } else if (HWFLOW) {
                    String str2 = TAG;
                    Slog.i(str2, "GameDabMode no need recoveryMode,LastBrightnessModeForGame=" + this.mLastBrightnessModeForGame + ",currentBrightnessMode=" + currentBrightnessMode);
                }
            }
        }
    }

    private void updateAutoBrightnessModeStatus() {
        int currentBrightnessMode = getCurrentBrightnessMode();
        if (this.mGameDisableAutoBrightnessModeEnable) {
            backupCurrentBrightnessMode(currentBrightnessMode);
            if (currentBrightnessMode == 1 && this.mGameIsFrontEnable) {
                this.mBrightnessModeSetAutoEnable = false;
                this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(10);
                this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessage(10);
            } else if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "GameDabMode no need process brightnessMode,currentBrightnessMode=" + currentBrightnessMode + ",mGameIsFrontEnable=" + this.mGameIsFrontEnable);
            }
        } else {
            int i = this.mLastBrightnessModeForGame;
            if (i != currentBrightnessMode && i == 1) {
                this.mBrightnessModeSetAutoEnable = true;
                this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(10);
                this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessage(10);
            } else if (HWFLOW) {
                String str2 = TAG;
                Slog.i(str2, "GameDabMode no need process,mLastBrightnessModeForGame=" + this.mLastBrightnessModeForGame + ",currentBrightnessMode=" + currentBrightnessMode);
            }
        }
        if (HWFLOW) {
            String str3 = TAG;
            Slog.i(str3, "GameDabMode updateAutoBrightnessModeStatus(),mGameDisableAutoBrightnessModeEnable=" + this.mGameDisableAutoBrightnessModeEnable + ",mLastBrightnessModeForGame=" + this.mLastBrightnessModeForGame + ",currentBrightnessMode =" + currentBrightnessMode);
        }
    }

    private void backupCurrentBrightnessMode(int brightnessMode) {
        if (this.mLastBrightnessModeForGame != brightnessMode) {
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "GameDabMode backupBrightnessMode,mLastBrightnessModeForGame=" + this.mLastBrightnessModeForGame + "-->brightnessMode=" + brightnessMode);
            }
            this.mLastBrightnessModeForGame = brightnessMode;
        } else if (HWFLOW) {
            String str2 = TAG;
            Slog.i(str2, "GameDabMode no need backupBrightnessMode, brightnessMode=" + brightnessMode);
        }
    }

    private int getCurrentBrightnessMode() {
        Context context = this.mContext;
        if (context == null) {
            return 0;
        }
        return Settings.System.getIntForUser(context.getContentResolver(), "screen_brightness_mode", 0, this.mCurrentUserId);
    }

    /* access modifiers changed from: private */
    public void setAutoBrightnessMode(boolean isAutoMode) {
        if (this.mContext != null) {
            if (isAutoMode != (getCurrentBrightnessMode() == 1)) {
                if (isAutoMode) {
                    this.mGameDisableAutoBrightnessModeKeepOffsetEnable = true;
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 1, this.mCurrentUserId);
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), "hw_screen_brightness_mode_value", 1, this.mCurrentUserId);
                } else {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, this.mCurrentUserId);
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), "hw_screen_brightness_mode_value", 0, this.mCurrentUserId);
                }
                String str = TAG;
                Slog.i(str, "GameDabMode setAutoBrightnessMode isAutoMode=" + isAutoMode);
            }
        }
    }

    public boolean getGameDisableAutoBrightnessModeKeepOffsetEnable() {
        if (!this.mData.gameDisableAutoBrightnessModeEnable) {
            return false;
        }
        return this.mGameDisableAutoBrightnessModeKeepOffsetEnable;
    }

    public void setGameDisableAutoBrightnessModeKeepOffsetEnable(boolean enable) {
        this.mGameDisableAutoBrightnessModeKeepOffsetEnable = enable;
    }

    private void updateGameDisableAutoBrightnessModeState(int curveLevel) {
        if (curveLevel == 29 || curveLevel == 28) {
            this.mGameDisableAutoBrightnessModeEnable = curveLevel == 29;
            updateGameDisableAutoBrightnessModeEnableDb();
            updateAutoBrightnessModeStatus();
        }
    }

    private void updateGameDisableAutoBrightnessModeEnableDb() {
        if (this.mContext != null) {
            if (this.mGameDisableAutoBrightnessModeEnable) {
                this.mGameDisableAutoBrightnessModeStatus = 1;
            } else {
                this.mGameDisableAutoBrightnessModeStatus = 0;
            }
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(9);
            this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessage(9);
        }
    }

    /* access modifiers changed from: private */
    public void updateGameDisableAutoBrightnessModeEnableDbMsg() {
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "GameDabMode updateGameDisableAutoBrightnessModeEnableDbMsg,ModeStatus=" + this.mGameDisableAutoBrightnessModeStatus);
        }
        Settings.System.putIntForUser(this.mContext.getContentResolver(), "game_disable_auto_brightness_mode", this.mGameDisableAutoBrightnessModeStatus, this.mCurrentUserId);
    }

    private void updateGameDisableAutoBrightnessModeEnable() {
        int gameDisableAutoBrightnessMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), "game_disable_auto_brightness_mode", 0, this.mCurrentUserId);
        if (gameDisableAutoBrightnessMode == 1) {
            this.mGameDisableAutoBrightnessModeEnable = true;
        } else {
            this.mGameDisableAutoBrightnessModeEnable = false;
        }
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "GameDabMode gameDisableAutoBrightnessMode=" + gameDisableAutoBrightnessMode + ",mGameDisableAutoBrightnessModeEnable=" + this.mGameDisableAutoBrightnessModeEnable);
        }
    }

    public boolean getGameDisableAutoBrightnessModeStatus() {
        if (!this.mData.gameDisableAutoBrightnessModeEnable) {
            return false;
        }
        updateGameDisableAutoBrightnessModeEnable();
        return this.mGameDisableAutoBrightnessModeEnable;
    }

    /* access modifiers changed from: private */
    public void updateFrontCameraMaxBrightness() {
        int brightness;
        if (!this.mData.frontCameraMaxBrightnessEnable) {
            this.mFrontCameraMaxBrightness = 255;
            return;
        }
        if (this.mAmbientLuxForFrontCamera >= this.mData.frontCameraLuxThreshold || !"1".equals(this.mCurCameraId) || this.mFrontCameraAppKeepBrightnessEnable) {
            brightness = 255;
        } else {
            brightness = this.mData.frontCameraMaxBrightness;
        }
        if (brightness != this.mFrontCameraMaxBrightness) {
            updateFrontCameraBrightnessDimmingEnable();
            this.mFrontCameraMaxBrightness = brightness;
            if (this.mLightSensorEnabled) {
                this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(11);
                this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessageDelayed(11, (long) this.mData.frontCameraUpdateBrightnessDelayTime);
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "updateFrontCameraMaxBrightness, atuo brightness=" + brightness + ",lux=" + this.mAmbientLuxForFrontCamera + ",mKeepBrightnessEnable=" + this.mFrontCameraAppKeepBrightnessEnable);
                }
            }
        }
    }

    private void updateFrontCameraBrightnessDimmingEnable() {
        this.mFrontCameraDimmingEnable = this.mScreenAutoBrightness > this.mData.frontCameraMaxBrightness;
        if (this.mFrontCameraDimmingEnable) {
            this.mHwNormalizedAutomaticBrightnessHandler.removeMessages(12);
            this.mHwNormalizedAutomaticBrightnessHandler.sendEmptyMessageDelayed(12, (long) this.mData.frontCameraUpdateDimmingEnableTime);
        }
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "mFrontCameraDimmingEnable=" + this.mFrontCameraDimmingEnable + ",mScreenAutoBrightness=" + this.mScreenAutoBrightness);
        }
    }

    /* access modifiers changed from: private */
    public void setFrontCameraBrightnessDimmingEnable(boolean dimmingEnable) {
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "setFrontCameraBrightnessDimmingEnable,dimmingEnable=" + dimmingEnable);
        }
        this.mFrontCameraDimmingEnable = dimmingEnable;
    }

    /* access modifiers changed from: private */
    public void updateAutoBrightness(int msg) {
        if (this.mCallbacks == null) {
            Slog.w(TAG, "mCallbacks==null,no updateBrightness");
            return;
        }
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "updateAutoBrightness for callback,msg=" + msg);
        }
        this.mCallbacks.updateBrightness();
    }

    public boolean getFrontCameraDimmingEnable() {
        return this.mData.frontCameraMaxBrightnessEnable && this.mFrontCameraDimmingEnable;
    }

    public void setFrontCameraAppEnableState(boolean enable) {
        if (enable != this.mFrontCameraAppKeepBrightnessEnable) {
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "setFrontCameraAppEnableState=" + enable);
            }
            this.mFrontCameraAppKeepBrightnessEnable = enable;
        }
    }
}
