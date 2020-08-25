package com.android.server.display;

import android.content.Context;
import android.graphics.PointF;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.HwLog;
import android.util.Log;
import android.util.Slog;
import com.android.server.HwServiceFactory;
import com.android.server.display.DisplayEffectMonitor;
import com.android.server.display.HwBrightnessBatteryDetection;
import com.android.server.display.HwBrightnessPgSceneDetection;
import com.android.server.display.HwBrightnessXmlLoader;
import com.android.server.display.HwLightSensorController;
import com.android.server.display.ManualBrightnessController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HwNormalizedManualBrightnessController extends ManualBrightnessController implements HwLightSensorController.LightSensorCallbacks, HwBrightnessPgSceneDetection.HwBrightnessPgSceneDetectionCallbacks, HwBrightnessBatteryDetection.Callbacks {
    private static final int AMBIENT_LIGHT_MONITOR_SAMPLING_INTERVAL_MS = 2000;
    private static final int BACK_SENSOR_REPORT_TIMEOUT = 300;
    private static final int DEFAULT = 0;
    private static final float DEFAULT_POWERSAVING_RATIO = 1.0f;
    private static final String FRONT_CAMERA = "1";
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final int INDOOR = 1;
    private static final int MAXDEFAULTBRIGHTNESS = 255;
    private static final int MINDEFAULTBRIGHTNESS = 4;
    private static final int MSG_FRONT_CAMERA_UPDATE_BRIGHTNESS = 2;
    private static final int MSG_FRONT_CAMERA_UPDATE_DIMMING_ENABLE = 3;
    private static final int MSG_SENSOR_TIMEOUT = 1;
    private static final int OUTDOOR = 2;
    private static String TAG = "HwNormalizedManualBrightnessController";
    private int mAlgoSmoothLightValue;
    private boolean mAmbientLuxTimeOut;
    private boolean mAmbientLuxValid;
    private int mAutoBrightnessLevel = -1;
    private int mBackSensorCoverBrightness = -1;
    private int mBackSensorCoverLux = -1;
    private boolean mBatteryModeStatus = false;
    private int mBrightnessNoLimitSetByApp = -1;
    private boolean mBrightnessNoLimitSetByAppAnimationEnable = false;
    private boolean mBrightnessNoLimitSetByAppEnable = false;
    private CameraManager.AvailabilityCallback mCameraAvailableCallback = new CameraManager.AvailabilityCallback() {
        /* class com.android.server.display.HwNormalizedManualBrightnessController.AnonymousClass1 */

        public void onCameraAvailable(String cameraId) {
            if ("1".equals(cameraId)) {
                String unused = HwNormalizedManualBrightnessController.this.mCurCameraId = null;
                HwNormalizedManualBrightnessController.this.updateFrontCameraMaxBrightness();
            }
        }

        public void onCameraUnavailable(String cameraId) {
            if ("1".equals(cameraId)) {
                String unused = HwNormalizedManualBrightnessController.this.mCurCameraId = cameraId;
                HwNormalizedManualBrightnessController.this.updateFrontCameraMaxBrightness();
            }
        }
    };
    private CameraManager mCameraManager;
    private final Context mContext;
    /* access modifiers changed from: private */
    public String mCurCameraId = null;
    private int mCurrentUserId = 0;
    private final HwBrightnessXmlLoader.Data mData = HwBrightnessXmlLoader.getData();
    private float mDefaultBrightness = 100.0f;
    private int mDefaultBrightnessNit = 20;
    private DisplayEffectMonitor mDisplayEffectMonitor;
    private boolean mFrontCameraAppKeepBrightnessEnable;
    private boolean mFrontCameraDimmingEnable = false;
    private int mFrontCameraMaxBrightness = 255;
    private Handler mHandler;
    private HwBrightnessBatteryDetection mHwBrightnessBatteryDetection;
    private HwBrightnessMapping mHwBrightnessMapping;
    private HwBrightnessPgSceneDetection mHwBrightnessPgSceneDetection;
    private final HwNormalizedManualBrightnessBatteryHandler mHwNormalizedManualBrightnessBatteryHandler;
    private final boolean mIsBackSensorEnable;
    private long mLastAmbientLightToMonitorTime;
    private float mLastAmbientLuxForFrontCamera = 0.0f;
    private HwLightSensorController mLightSensorController = null;
    private boolean mLightSensorEnable;
    private int mLowBatteryMaxBrightness = 255;
    private int mManualAmbientLux;
    private float mManualAmbientLuxForCamera = -1.0f;
    private final HandlerThread mManualBrightnessProcessThread;
    private boolean mManualModeAnimationEnable = false;
    private boolean mManualModeEnable;
    private boolean mManualPowerSavingAnimationEnable = false;
    private boolean mManualPowerSavingBrighnessLineDisableForDemo = false;
    private boolean mManualPowerSavingEnable = false;
    private int mManualbrightness = -1;
    private int mManualbrightnessLog = -1;
    private int mManualbrightnessOut = -1;
    private int mMaxBrightnessSetByThermal = 255;
    private HwNormalizedManualBrightnessThresholdDetector mOutdoorDetector = null;
    private int mOutdoorScene;
    private float mPowerRatio = 1.0f;
    List<float[]> mPowerSavingBrighnessLinePointsList = null;
    private boolean mThermalModeAnimationEnable = false;
    private boolean mThermalModeEnable = false;
    private boolean mUsePowerSavingModeCurveEnable = false;

    static {
        boolean z = false;
        if (Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3))) {
            z = true;
        }
        HWDEBUG = z;
    }

    public HwNormalizedManualBrightnessController(ManualBrightnessCallbacks callbacks, Context context, SensorManager sensorManager) {
        super(callbacks);
        this.mLightSensorController = new HwLightSensorController(context, this, sensorManager, this.mData.lightSensorRateMills, TAG);
        this.mOutdoorDetector = new HwNormalizedManualBrightnessThresholdDetector(this.mData);
        this.mDisplayEffectMonitor = DisplayEffectMonitor.getInstance(context);
        if (this.mDisplayEffectMonitor == null) {
            Slog.e(TAG, "getDisplayEffectMonitor failed!");
        }
        parseManualModePowerSavingCure(SystemProperties.get("ro.config.blight_power_curve", ""));
        this.mContext = context;
        this.mHwBrightnessPgSceneDetection = new HwBrightnessPgSceneDetection(this, this.mData.pgSceneDetectionDarkenDelayTime, this.mData.pgSceneDetectionBrightenDelayTime, this.mContext);
        this.mHwBrightnessMapping = new HwBrightnessMapping(this.mData.brightnessMappingPoints);
        this.mIsBackSensorEnable = this.mLightSensorController.isBackSensorEnable();
        if (this.mData.backSensorCoverModeEnable || this.mData.frontCameraMaxBrightnessEnable) {
            this.mHandler = new HwNormalizedManualBrightnessHandler();
        }
        if (this.mData.manualPowerSavingBrighnessLineDisableForDemo) {
            this.mManualPowerSavingBrighnessLineDisableForDemo = isDemoVersion();
        }
        this.mManualBrightnessProcessThread = new HandlerThread(TAG);
        this.mManualBrightnessProcessThread.start();
        this.mHwNormalizedManualBrightnessBatteryHandler = new HwNormalizedManualBrightnessBatteryHandler(this.mManualBrightnessProcessThread.getLooper());
        if (this.mData.batteryModeEnable) {
            this.mHwNormalizedManualBrightnessBatteryHandler.post(new Runnable() {
                /* class com.android.server.display.$$Lambda$HwNormalizedManualBrightnessController$GVu8D1MFGQP9YDO7lwf4bjCc3nA */

                public final void run() {
                    HwNormalizedManualBrightnessController.this.lambda$new$0$HwNormalizedManualBrightnessController();
                }
            });
        }
    }

    public /* synthetic */ void lambda$new$0$HwNormalizedManualBrightnessController() {
        this.mHwBrightnessBatteryDetection = new HwBrightnessBatteryDetection(this, this.mContext);
    }

    private void parseManualModePowerSavingCure(String powerSavingCure) {
        if (powerSavingCure == null || powerSavingCure.length() <= 0) {
            Slog.i(TAG, "powerSavingCure == null");
            return;
        }
        List<float[]> list = this.mPowerSavingBrighnessLinePointsList;
        if (list != null) {
            list.clear();
        } else {
            this.mPowerSavingBrighnessLinePointsList = new ArrayList();
        }
        String[] powerSavingPoints = powerSavingCure.split(";");
        int i = 0;
        while (i < powerSavingPoints.length) {
            try {
                String[] point = powerSavingPoints[i].split(",");
                float x = Float.parseFloat(point[0]);
                float y = Float.parseFloat(point[1]);
                this.mPowerSavingBrighnessLinePointsList.add(new float[]{x, y});
                i++;
            } catch (NumberFormatException e) {
                this.mPowerSavingBrighnessLinePointsList.clear();
                Slog.w(TAG, "parse ManualPowerSaving curve error");
                return;
            }
        }
        List<float[]> list2 = this.mPowerSavingBrighnessLinePointsList;
        if (list2 != null) {
            int listSize = list2.size();
            for (int i2 = 0; i2 < listSize; i2++) {
                float[] temp = this.mPowerSavingBrighnessLinePointsList.get(i2);
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "ManualPowerSavingPointsList x = " + temp[0] + ", y = " + temp[1]);
                }
            }
        }
    }

    private static boolean wantScreenOn(int state) {
        if (state == 2 || state == 3) {
            return true;
        }
        return false;
    }

    public void updatePowerState(int state, boolean enable) {
        if (this.mManualModeEnable != enable) {
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "HBM SensorEnable change " + this.mManualModeEnable + " -> " + enable);
            }
            this.mManualModeEnable = enable;
        }
        boolean z = this.mManualModeEnable;
        if (z) {
            setLightSensorEnabled(wantScreenOn(state));
        } else {
            setLightSensorEnabled(z);
        }
        if (this.mData.frontCameraMaxBrightnessEnable && this.mCameraManager == null) {
            this.mCameraManager = (CameraManager) this.mContext.getSystemService("camera");
            this.mCameraManager.registerAvailabilityCallback(this.mCameraAvailableCallback, (Handler) null);
            Slog.i(TAG, "registerAvailabilityCallback for manual frontCameraMaxBrightness");
        }
    }

    private void setLightSensorEnabled(boolean enable) {
        if (enable) {
            if (!this.mLightSensorEnable) {
                this.mLightSensorEnable = true;
                this.mLightSensorController.enableSensor();
                this.mAmbientLuxValid = false;
                this.mAmbientLuxTimeOut = false;
                this.mBackSensorCoverLux = -1;
                this.mBackSensorCoverBrightness = -1;
                Handler handler = this.mHandler;
                if (handler != null) {
                    handler.sendEmptyMessageDelayed(1, 300);
                }
                if (HWFLOW) {
                    Slog.i(TAG, "ManualMode sensor enable");
                }
            }
            boolean pgRecognitionListenerRegisted = this.mHwBrightnessPgSceneDetection.getPgRecognitionListenerRegisted();
            if (this.mData.manualPowerSavingBrighnessLineEnable && !pgRecognitionListenerRegisted) {
                this.mHwBrightnessPgSceneDetection.registerPgRecognitionListener(this.mContext);
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "PowerSaving Manul in registerPgBLightSceneChangedListener,=" + this.mHwBrightnessPgSceneDetection.getPgRecognitionListenerRegisted());
                }
            }
        } else if (this.mLightSensorEnable) {
            this.mLightSensorEnable = false;
            this.mLightSensorController.disableSensor();
            this.mOutdoorDetector.clearAmbientLightRingBuffer();
            if (this.mData.frontCameraMaxBrightnessEnable && this.mFrontCameraDimmingEnable) {
                this.mFrontCameraDimmingEnable = false;
            }
            Handler handler2 = this.mHandler;
            if (handler2 != null) {
                handler2.removeMessages(1);
            }
            if (HWFLOW) {
                Slog.i(TAG, "ManualMode sensor disable");
            }
        }
        this.mLastAmbientLightToMonitorTime = 0;
    }

    public void updateManualBrightness(int brightness) {
        this.mManualbrightness = brightness;
        this.mManualbrightnessOut = brightness;
    }

    private static boolean isDemoVersion() {
        String vendor2 = SystemProperties.get("ro.hw.vendor", "");
        String country = SystemProperties.get("ro.hw.country", "");
        String str = TAG;
        Slog.i(str, "vendor:" + vendor2 + ",country:" + country);
        return "demo".equalsIgnoreCase(vendor2) || "demo".equalsIgnoreCase(country);
    }

    @Override // com.android.server.display.HwBrightnessPgSceneDetection.HwBrightnessPgSceneDetectionCallbacks
    public void updateStateRecognition(boolean usePwrBLightCurve, int appType) {
        if (!this.mData.manualPowerSavingBrighnessLineEnable || !this.mManualModeEnable || this.mManualPowerSavingBrighnessLineDisableForDemo) {
            this.mManualPowerSavingAnimationEnable = false;
            this.mManualPowerSavingEnable = false;
            return;
        }
        this.mManualPowerSavingEnable = usePwrBLightCurve;
        this.mManualPowerSavingAnimationEnable = getPowerSavingModeBrightnessChangeEnable(this.mManualbrightness, usePwrBLightCurve);
        float powerRatio = covertBrightnessToPowerRatio(this.mManualbrightness);
        int tembrightness = (int) (((float) this.mManualbrightness) * powerRatio);
        if (this.mData.brightnessLevelToNitMappingEnable) {
            tembrightness = convertNitToBrightnessLevelFromRealLinePoints(this.mData.brightnessLevelToNitLinePoints, ((float) convertBrightnessLevelToNit(this.mManualbrightness)) * powerRatio, this.mManualbrightness);
        }
        if (usePwrBLightCurve) {
            HwLog.dubaie("DUBAI_TAG_BACKLIGHT_DISCOUNT", "ratio=" + ((int) (100.0f * powerRatio)));
        }
        int pgModeBrightness = this.mHwBrightnessPgSceneDetection.getAdjustLightValByPgMode(tembrightness);
        if (pgModeBrightness != this.mManualbrightness) {
            powerRatio = this.mHwBrightnessPgSceneDetection.getPgPowerModeRatio();
            tembrightness = pgModeBrightness;
        }
        if (this.mManualbrightnessLog != tembrightness) {
            int brightnessNit = convertBrightnessLevelToNit(this.mManualbrightness);
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "PowerSaving,ManualMode mManualbrightness=" + this.mManualbrightness + ",brightnessNit=" + brightnessNit + ",powerRatio=" + powerRatio + ",maxNit=" + this.mData.screenBrightnessMaxNit + ",MinNit=" + this.mData.screenBrightnessMinNit + ",usePwrBLightCurve=" + usePwrBLightCurve + ",appType=" + appType);
            }
            this.mManualbrightnessLog = tembrightness;
            this.mCallbacks.updateManualBrightnessForLux();
        }
    }

    private boolean getPowerSavingModeBrightnessChangeEnable(int brightness, boolean usePowerSavingModeCurveEnable) {
        boolean powerSavingModeBrightnessChangeEnable = false;
        if (this.mUsePowerSavingModeCurveEnable != usePowerSavingModeCurveEnable) {
            float powerRatio = covertBrightnessLevelToPowerRatio(brightness);
            int tembrightness = (int) (((float) brightness) * powerRatio);
            if (this.mData.brightnessLevelToNitMappingEnable) {
                tembrightness = convertNitToBrightnessLevelFromRealLinePoints(this.mData.brightnessLevelToNitLinePoints, ((float) convertBrightnessLevelToNit(brightness)) * powerRatio, brightness);
            }
            if (brightness != tembrightness && !HwServiceFactory.isCoverClosed()) {
                powerSavingModeBrightnessChangeEnable = true;
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "PowerSaving Enable=" + true + ",Pgbrightness=" + tembrightness + ",brightness=" + brightness);
                }
            }
        }
        this.mUsePowerSavingModeCurveEnable = usePowerSavingModeCurveEnable;
        return powerSavingModeBrightnessChangeEnable;
    }

    private float covertBrightnessLevelToPowerRatio(int brightness) {
        if ((!this.mData.manualMode || this.mManualbrightness < this.mData.manualBrightnessMaxLimit) && this.mData.manualPowerSavingBrighnessLineEnable) {
            return getPowerSavingRatio(convertBrightnessLevelToNit(this.mManualbrightness));
        }
        return 1.0f;
    }

    public int getManualBrightness() {
        int i = this.mBrightnessNoLimitSetByApp;
        if (i > 0) {
            return i;
        }
        if (this.mData.backSensorCoverModeEnable) {
            int i2 = this.mBackSensorCoverBrightness;
            if (i2 > 0) {
                if (this.mAutoBrightnessLevel != i2 && Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, this.mCurrentUserId) == 1) {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", this.mBackSensorCoverBrightness, this.mCurrentUserId);
                    if (HWFLOW) {
                        String str = TAG;
                        Slog.i(str, "coverLevel=" + this.mAutoBrightnessLevel + ",mBack=" + this.mBackSensorCoverBrightness);
                    }
                }
                int brightnessMode = this.mBackSensorCoverBrightness;
                this.mAutoBrightnessLevel = brightnessMode;
                return brightnessMode;
            } else if (!this.mAmbientLuxValid && !this.mAmbientLuxTimeOut && HwServiceFactory.isCoverClosed()) {
                this.mAutoBrightnessLevel = -1;
                return -1;
            }
        }
        float powerSavingRatio = covertBrightnessToPowerRatio(this.mManualbrightness);
        if (HWFLOW && Math.abs(this.mPowerRatio - powerSavingRatio) > 1.0E-7f) {
            int brightnessNit = convertBrightnessLevelToNit(this.mManualbrightness);
            this.mPowerRatio = powerSavingRatio;
            String str2 = TAG;
            Slog.i(str2, "PowerSaving powerSavingRatio=" + powerSavingRatio + ",mManualbrightness=" + this.mManualbrightness + ",brightnessNit=" + brightnessNit);
        }
        int mManualbrightnessTmp = (int) (((float) this.mManualbrightness) * powerSavingRatio);
        if (this.mData.brightnessLevelToNitMappingEnable) {
            mManualbrightnessTmp = convertNitToBrightnessLevelFromRealLinePoints(this.mData.brightnessLevelToNitLinePoints, ((float) convertBrightnessLevelToNit(this.mManualbrightness)) * powerSavingRatio, this.mManualbrightness);
        }
        this.mManualbrightnessOut = getValidBrightness(mManualbrightnessTmp);
        if (!this.mData.manualMode) {
            this.mManualbrightnessOut = mManualbrightnessTmp;
            if (HWFLOW) {
                String str3 = TAG;
                Slog.i(str3, "mManualbrightnessOut=" + this.mManualbrightnessOut + ",mData.manualMode=" + this.mData.manualMode);
            }
        } else if (this.mManualbrightnessOut >= this.mData.manualBrightnessMaxLimit) {
            float defaultBrightness = getDefaultBrightnessLevelNew(this.mData.defaultBrightnessLinePoints, (float) this.mManualAmbientLux);
            if (this.mOutdoorScene == 2) {
                int mManualbrightnessTmpMin = mManualbrightnessTmp < this.mData.manualBrightnessMaxLimit ? mManualbrightnessTmp : this.mData.manualBrightnessMaxLimit;
                this.mManualbrightnessOut = mManualbrightnessTmpMin > ((int) defaultBrightness) ? mManualbrightnessTmpMin : (int) defaultBrightness;
                if (HWFLOW) {
                    String str4 = TAG;
                    Slog.i(str4, "mManualbrightnessOut=" + this.mManualbrightnessOut + ",defaultBrightness=" + defaultBrightness + ",AutoLux=" + this.mManualAmbientLux);
                }
            } else {
                this.mManualbrightnessOut = mManualbrightnessTmp < this.mData.manualBrightnessMaxLimit ? mManualbrightnessTmp : this.mData.manualBrightnessMaxLimit;
                if (HWFLOW) {
                    String str5 = TAG;
                    Slog.i(str5, "mManualbrightnessOut1=" + this.mManualbrightnessOut + ",defaultBrightness=" + defaultBrightness + ",AutoLux=" + this.mManualAmbientLux);
                }
            }
        }
        int pgModeBrightness = this.mHwBrightnessPgSceneDetection.getAdjustLightValByPgMode(this.mManualbrightnessOut);
        if (this.mData.pgModeBrightnessMappingEnable && this.mManualbrightnessOut > this.mData.manualBrightnessMaxLimit) {
            pgModeBrightness = this.mHwBrightnessMapping.getMappingBrightnessForRealNit(pgModeBrightness);
        }
        if (pgModeBrightness != this.mManualbrightnessOut) {
            this.mPowerRatio = this.mHwBrightnessPgSceneDetection.getPgPowerModeRatio();
            this.mManualbrightnessOut = pgModeBrightness;
            if (HWFLOW) {
                String str6 = TAG;
                Slog.i(str6, "PG_POWER_SAVE_MODE mOut=" + this.mManualbrightnessOut + ",mPowerRatio=" + this.mPowerRatio);
            }
        }
        if (this.mManualbrightnessOut > this.mMaxBrightnessSetByThermal) {
            if (HWFLOW) {
                String str7 = TAG;
                Slog.i(str7, "ThermalMode Org=" + this.mManualbrightnessOut + ",mThermal=" + this.mMaxBrightnessSetByThermal);
            }
            this.mManualbrightnessOut = this.mMaxBrightnessSetByThermal;
        }
        if (this.mManualbrightnessOut > this.mLowBatteryMaxBrightness && this.mData.batteryModeEnable) {
            if (HWFLOW) {
                String str8 = TAG;
                Slog.i(str8, "mOut = " + this.mManualbrightnessOut + ", mLowBatteryMax= " + this.mLowBatteryMaxBrightness);
            }
            this.mManualbrightnessOut = this.mLowBatteryMaxBrightness;
        }
        if (this.mData.luxMinMaxBrightnessEnable && this.mManualbrightnessOut > this.mFrontCameraMaxBrightness) {
            if (HWFLOW) {
                String str9 = TAG;
                Slog.i(str9, "mOut = " + this.mManualbrightnessOut + ", mFrontCameraMaxBrightness= " + this.mFrontCameraMaxBrightness);
            }
            this.mManualbrightnessOut = this.mFrontCameraMaxBrightness;
        }
        return this.mManualbrightnessOut;
    }

    private int getValidBrightness(int brightness) {
        int brightnessOut = brightness;
        if (brightnessOut < 4) {
            String str = TAG;
            Slog.w(str, "warning mManualbrightness < min,brightnessOut=" + brightnessOut);
            brightnessOut = 4;
        }
        if (brightnessOut <= 255) {
            return brightnessOut;
        }
        String str2 = TAG;
        Slog.w(str2, "warning mManualbrightness > max,brightnessOut=" + brightnessOut);
        return 255;
    }

    public int getMaxBrightnessForSeekbar() {
        if (this.mData.manualMode) {
            return this.mData.manualBrightnessMaxLimit;
        }
        return 255;
    }

    @Override // com.android.server.display.HwLightSensorController.LightSensorCallbacks
    public void processSensorData(long timeInMs, int lux, int cct) {
        this.mAmbientLuxValid = true;
        boolean needUpdateManualBrightness = false;
        if (this.mData.backSensorCoverModeEnable && needUpdateBrightWhileCoverClosed(lux)) {
            needUpdateManualBrightness = true;
        }
        this.mOutdoorDetector.handleLightSensorEvent(timeInMs, (float) lux);
        this.mOutdoorScene = this.mOutdoorDetector.getIndoorOutdoorFlagForHBM();
        if (this.mOutdoorDetector.getLuxChangedFlagForHBM()) {
            needUpdateManualBrightness = true;
            this.mManualModeAnimationEnable = true;
        }
        this.mManualAmbientLux = (int) this.mOutdoorDetector.getAmbientLuxForHBM();
        if (this.mData.frontCameraMaxBrightnessEnable) {
            this.mManualAmbientLuxForCamera = (float) ((int) this.mOutdoorDetector.getAmbientLuxForFrontCamera());
            if ((this.mLastAmbientLuxForFrontCamera >= this.mData.frontCameraLuxThreshold && this.mManualAmbientLuxForCamera < this.mData.frontCameraLuxThreshold) || (this.mLastAmbientLuxForFrontCamera < this.mData.frontCameraLuxThreshold && this.mManualAmbientLuxForCamera >= this.mData.frontCameraLuxThreshold)) {
                String str = TAG;
                Slog.i(str, "updateFrontCameraMaxBrightness mLastAmbientLuxForFrontCamera=" + this.mLastAmbientLuxForFrontCamera + ",mManualAmbientLuxForCamera=" + this.mManualAmbientLuxForCamera);
                updateFrontCameraMaxBrightness();
                this.mLastAmbientLuxForFrontCamera = this.mManualAmbientLuxForCamera;
            }
        }
        if (needUpdateManualBrightness) {
            this.mCallbacks.updateManualBrightnessForLux();
            this.mOutdoorDetector.setLuxChangedFlagForHBM();
            if (HWFLOW) {
                String str2 = TAG;
                Slog.i(str2, "mManualAmbientLux =" + this.mManualAmbientLux + ", mManualModeAnimationEnable=" + this.mManualModeAnimationEnable);
            }
        } else {
            this.mManualModeAnimationEnable = false;
        }
        sendAmbientLightToMonitor(timeInMs, (float) lux);
        sendDefaultBrightnessToMonitor();
    }

    private final class HwNormalizedManualBrightnessHandler extends Handler {
        private HwNormalizedManualBrightnessHandler() {
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                HwNormalizedManualBrightnessController.this.updateBrightnessIfNoAmbientLuxReported();
            } else if (i == 2) {
                HwNormalizedManualBrightnessController.this.updateBrightness(2);
            } else if (i == 3) {
                HwNormalizedManualBrightnessController.this.setFrontCameraBrightnessDimmingEnable(false);
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateBrightnessIfNoAmbientLuxReported() {
        if (!this.mAmbientLuxValid) {
            this.mAmbientLuxTimeOut = true;
            if (HWFLOW) {
                Slog.i(TAG, "BackSensorCoverMode sensor doesn't report lux in 300ms");
            }
            this.mCallbacks.updateManualBrightnessForLux();
            updateAutoBrightnessDBOnCoverClosed();
        }
    }

    private void updateAutoBrightnessDBOnCoverClosed() {
        int i;
        if (HwServiceFactory.isCoverClosed()) {
            int brightnessMode = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, this.mCurrentUserId);
            int autoBrightnessDB = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", 0, this.mCurrentUserId);
            if (brightnessMode == 1 && (i = this.mManualbrightnessOut) > 0 && autoBrightnessDB != i) {
                Settings.System.putIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", this.mManualbrightnessOut, this.mCurrentUserId);
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "LabcCoverMode mBackSensorCoverBrightness=" + this.mBackSensorCoverBrightness + ",mManualbrightnessOut=" + this.mManualbrightnessOut);
                }
            }
        }
    }

    private boolean isManualMode() {
        return Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 1, this.mCurrentUserId) == 0;
    }

    private boolean needUpdateBrightWhileCoverClosed(int mixedSensorValue) {
        if (!this.mIsBackSensorEnable) {
            return false;
        }
        if (!HwServiceFactory.isCoverClosed()) {
            if (this.mBackSensorCoverBrightness > 0) {
                boolean isManualMode = isManualMode();
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "BackSensorCoverMode cover open, isManualMode=" + isManualMode);
                }
                this.mBackSensorCoverLux = -1;
                this.mBackSensorCoverBrightness = -1;
                if (isManualMode) {
                    return true;
                }
            }
            return false;
        }
        int backSensorValue = this.mLightSensorController.getBackSensorValue();
        int i = this.mBackSensorCoverLux;
        if (i <= backSensorValue) {
            i = backSensorValue;
        }
        this.mBackSensorCoverLux = i;
        int i2 = this.mBackSensorCoverLux;
        if (i2 <= mixedSensorValue) {
            i2 = mixedSensorValue;
        }
        this.mBackSensorCoverLux = i2;
        if (this.mData.backSensorCoverModeMinLuxInRing > 0 && isPhoneInRing()) {
            this.mBackSensorCoverLux = this.mBackSensorCoverLux > this.mData.backSensorCoverModeMinLuxInRing ? this.mBackSensorCoverLux : this.mData.backSensorCoverModeMinLuxInRing;
        }
        int backSensorCoverBrightness = (int) getDefaultBrightnessLevelNew(this.mData.backSensorCoverModeBrighnessLinePoints, (float) this.mBackSensorCoverLux);
        boolean isManualMode2 = isManualMode();
        if (isManualMode2 && backSensorCoverBrightness < this.mManualbrightness) {
            backSensorCoverBrightness = -1;
        }
        int backSensorCoverBrightness2 = updateBackSensorCoverBrightness(backSensorCoverBrightness);
        if (backSensorCoverBrightness2 == this.mBackSensorCoverBrightness) {
            return false;
        }
        if (HWFLOW) {
            String str2 = TAG;
            Slog.i(str2, "BackSensorCoverMode mixed=" + mixedSensorValue + ", back=" + backSensorValue + ", lux=" + this.mBackSensorCoverLux + ", bright=" + backSensorCoverBrightness2 + ", isManualMode=" + isManualMode2);
        }
        this.mBackSensorCoverBrightness = backSensorCoverBrightness2;
        return true;
    }

    private int updateBackSensorCoverBrightness(int backSensorCoverBrightness) {
        int autoBrightnessDb;
        Context context = this.mContext;
        if (context == null || backSensorCoverBrightness >= (autoBrightnessDb = Settings.System.getIntForUser(context.getContentResolver(), "screen_auto_brightness", 0, this.mCurrentUserId))) {
            return backSensorCoverBrightness;
        }
        if (HWFLOW && this.mBackSensorCoverBrightness != autoBrightnessDb) {
            String str = TAG;
            Slog.i(str, "BackSensorCoverMode backSensorCoverBrightnessOut=" + backSensorCoverBrightness + "-->autoBrightnessDb=" + autoBrightnessDb);
        }
        return autoBrightnessDb;
    }

    private boolean isPhoneInRing() {
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            int phoneCount = TelephonyManager.getDefault().getPhoneCount();
            for (int i = 0; i < phoneCount; i++) {
                if (TelephonyManager.getDefault().getCallState(i) != 0) {
                    return true;
                }
            }
            return false;
        } else if (TelephonyManager.getDefault().getCallState(SubscriptionManager.getDefaultSubscriptionId()) != 0) {
            return true;
        } else {
            return false;
        }
    }

    public void updateCurrentUserId(int userId) {
        if (userId != this.mCurrentUserId) {
            if (HWFLOW) {
                String str = TAG;
                Slog.i(str, "user change from  " + this.mCurrentUserId + " into " + userId);
            }
            this.mCurrentUserId = userId;
        }
    }

    public boolean needFastestRateForManualBrightness() {
        if (!this.mData.backSensorCoverModeEnable || this.mBackSensorCoverBrightness <= 0 || !HwServiceFactory.isCoverClosed()) {
            return false;
        }
        return true;
    }

    private float covertBrightnessToPowerRatio(int brightness) {
        if ((this.mData.manualMode && this.mManualbrightness >= this.mData.manualBrightnessMaxLimit) || !this.mData.manualPowerSavingBrighnessLineEnable || HwServiceFactory.isCoverClosed()) {
            return 1.0f;
        }
        int brightnessNit = convertBrightnessLevelToNit(this.mManualbrightness);
        if (this.mManualPowerSavingEnable) {
            return getPowerSavingRatio(brightnessNit);
        }
        return 1.0f;
    }

    private int convertBrightnessLevelToNit(int brightness) {
        if (brightness == 0) {
            return brightness;
        }
        if (brightness < 4) {
            brightness = 4;
        }
        if (brightness > 255) {
            brightness = 255;
        }
        return convertBrightnessLevelToNitInternal(brightness);
    }

    private float getPowerSavingRatio(int brightnssnit) {
        List<float[]> list = this.mPowerSavingBrighnessLinePointsList;
        if (list == null || list.size() == 0 || brightnssnit < 0) {
            String str = TAG;
            Slog.e(str, "PowerSavingBrighnessLinePointsList warning,set PowerSavingRatio,brightnssnit=" + brightnssnit);
            return 1.0f;
        }
        int linePointsListLength = this.mPowerSavingBrighnessLinePointsList.size();
        if (((float) brightnssnit) < this.mPowerSavingBrighnessLinePointsList.get(0)[0]) {
            return 1.0f;
        }
        float[] temp1 = null;
        float tmpPowerSavingRatio = 1.0f;
        int i = 0;
        while (true) {
            if (i > linePointsListLength - 1) {
                break;
            }
            float[] temp = this.mPowerSavingBrighnessLinePointsList.get(i);
            if (temp1 == null) {
                temp1 = temp;
            }
            if (((float) brightnssnit) >= temp[0]) {
                temp1 = temp;
                tmpPowerSavingRatio = temp1[1];
                i++;
            } else if (temp[0] <= temp1[0]) {
                tmpPowerSavingRatio = 1.0f;
                Slog.w(TAG, "temp2[0] <= temp1[0] warning,set default tmpPowerSavingRatio");
            } else {
                tmpPowerSavingRatio = (((temp[1] - temp1[1]) / (temp[0] - temp1[0])) * (((float) brightnssnit) - temp1[0])) + temp1[1];
            }
        }
        if (tmpPowerSavingRatio <= 1.0f && tmpPowerSavingRatio >= 0.0f) {
            return tmpPowerSavingRatio;
        }
        String str2 = TAG;
        Slog.w(str2, "tmpPowerSavingRatio warning,set default value, tmpPowerSavingRatio= " + this.mPowerRatio);
        return 1.0f;
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
                params.put("brightnessMode", "MANUAL");
                this.mDisplayEffectMonitor.sendMonitorParam(params);
            }
        }
    }

    private void sendDefaultBrightnessToMonitor() {
        int lightValue;
        if (this.mDisplayEffectMonitor != null && this.mAlgoSmoothLightValue != (lightValue = (int) this.mOutdoorDetector.getFilterLuxFromManualMode())) {
            this.mAlgoSmoothLightValue = lightValue;
            ArrayMap<String, Object> params = new ArrayMap<>();
            params.put(DisplayEffectMonitor.MonitorModule.PARAM_TYPE, "algoDefaultBrightness");
            params.put("lightValue", Integer.valueOf(lightValue));
            params.put("brightness", 0);
            params.put("brightnessMode", "MANUAL");
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    public float getDefaultBrightnessLevelNew(List<PointF> linePointsList, float lux) {
        float brightnessLevel = this.mDefaultBrightness;
        PointF temp1 = null;
        for (PointF temp : linePointsList) {
            if (temp1 == null) {
                temp1 = temp;
            }
            if (lux >= temp.x) {
                temp1 = temp;
                brightnessLevel = temp1.y;
            } else if (temp.x > temp1.x) {
                return (((temp.y - temp1.y) / (temp.x - temp1.x)) * (lux - temp1.x)) + temp1.y;
            } else {
                float brightnessLevel2 = this.mDefaultBrightness;
                if (!HWFLOW) {
                    return brightnessLevel2;
                }
                String str = TAG;
                Slog.i(str, "DefaultBrighness_temp1.x <= temp2.x,x" + temp.x + ", y = " + temp.y);
                return brightnessLevel2;
            }
        }
        return brightnessLevel;
    }

    public boolean getManualModeAnimationEnable() {
        return this.mManualModeAnimationEnable;
    }

    public boolean getManualModeEnable() {
        return this.mData.manualMode;
    }

    public boolean getManualPowerSavingAnimationEnable() {
        return this.mManualPowerSavingAnimationEnable;
    }

    public void setManualPowerSavingAnimationEnable(boolean manualPowerSavingAnimationEnable) {
        this.mManualPowerSavingAnimationEnable = manualPowerSavingAnimationEnable;
    }

    public boolean getManualThermalModeEnable() {
        return this.mManualModeEnable && this.mThermalModeEnable;
    }

    public boolean getManualThermalModeAnimationEnable() {
        return this.mThermalModeAnimationEnable;
    }

    public void setManualThermalModeAnimationEnable(boolean thermalModeAnimationEnable) {
        this.mThermalModeAnimationEnable = thermalModeAnimationEnable;
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
        if (this.mManualModeEnable) {
            String str = TAG;
            Slog.i(str, "ThermalMode set Manual MaxBrightness=" + brightness + ",mappingBrightness=" + mappingBrightness);
            this.mThermalModeAnimationEnable = true;
            this.mThermalModeEnable = true;
            this.mCallbacks.updateManualBrightnessForLux();
            return;
        }
        this.mThermalModeAnimationEnable = false;
        this.mThermalModeEnable = false;
    }

    public boolean getBrightnessSetByAppEnable() {
        return this.mBrightnessNoLimitSetByAppEnable;
    }

    public boolean getBrightnessSetByAppAnimationEnable() {
        return this.mBrightnessNoLimitSetByAppAnimationEnable;
    }

    public void setBrightnessNoLimit(int brightness, int time) {
        String str = TAG;
        Slog.i(str, "setBrightnessNoLimit set brightness=" + brightness + ",time=" + time);
        if (brightness > 0) {
            this.mBrightnessNoLimitSetByApp = brightness;
        } else {
            this.mBrightnessNoLimitSetByApp = -1;
        }
        if (brightness <= 0 || brightness > 255) {
            this.mBrightnessNoLimitSetByAppAnimationEnable = false;
            this.mBrightnessNoLimitSetByAppEnable = false;
        } else {
            this.mBrightnessNoLimitSetByAppAnimationEnable = true;
            this.mBrightnessNoLimitSetByAppEnable = true;
        }
        if (this.mManualModeEnable) {
            this.mCallbacks.updateManualBrightnessForLux();
        }
    }

    private int convertBrightnessLevelToNitInternal(int brightness) {
        float brightnessNitTmp;
        if (this.mData.brightnessLevelToNitMappingEnable) {
            brightnessNitTmp = convertBrightnessLevelToNitFromRealLinePoints(this.mData.brightnessLevelToNitLinePoints, (float) brightness);
        } else {
            brightnessNitTmp = ((((float) (brightness - 4)) * (this.mData.screenBrightnessMaxNit - this.mData.screenBrightnessMinNit)) / 251.0f) + this.mData.screenBrightnessMinNit;
        }
        if (brightnessNitTmp < this.mData.screenBrightnessMinNit) {
            brightnessNitTmp = this.mData.screenBrightnessMinNit;
        }
        if (brightnessNitTmp > this.mData.screenBrightnessMaxNit) {
            brightnessNitTmp = this.mData.screenBrightnessMaxNit;
        }
        return (int) (0.5f + brightnessNitTmp);
    }

    private float convertBrightnessLevelToNitFromRealLinePoints(List<PointF> linePoints, float brightness) {
        int i = this.mDefaultBrightnessNit;
        float brightnessNitTmp = (float) i;
        if (linePoints == null) {
            return (float) i;
        }
        PointF temp1 = null;
        Iterator<PointF> it = linePoints.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PointF pointItem = it.next();
            if (temp1 == null) {
                temp1 = pointItem;
            }
            if (brightness >= pointItem.x) {
                temp1 = pointItem;
                brightnessNitTmp = temp1.y;
            } else if (pointItem.x <= temp1.x) {
                brightnessNitTmp = (float) this.mDefaultBrightnessNit;
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "LevelToNit,brightnessNitTmpdefault=" + this.mDefaultBrightnessNit + ",for_temp1.x <= temp2.x,x" + pointItem.x + ", y = " + pointItem.y);
                }
            } else {
                brightnessNitTmp = (((pointItem.y - temp1.y) / (pointItem.x - temp1.x)) * (brightness - temp1.x)) + temp1.y;
            }
        }
        if (HWDEBUG) {
            String str2 = TAG;
            Slog.d(str2, "LevelToNit,brightness=" + brightness + ",TobrightnessNitTmp=" + brightnessNitTmp + ",mDefaultBrightnessNit=" + this.mDefaultBrightnessNit);
        }
        return brightnessNitTmp;
    }

    private int convertNitToBrightnessLevelFromRealLinePoints(List<PointF> linePoints, float brightnessNit, int defaultBrightness) {
        float brightnessLevel = (float) defaultBrightness;
        if (linePoints == null) {
            return defaultBrightness;
        }
        PointF temp1 = null;
        Iterator<PointF> it = linePoints.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PointF pointItem = it.next();
            if (temp1 == null) {
                temp1 = pointItem;
            }
            if (brightnessNit >= pointItem.y) {
                temp1 = pointItem;
                brightnessLevel = temp1.x;
            } else if (pointItem.y <= temp1.y) {
                brightnessLevel = (float) defaultBrightness;
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "NitToBrightnessLevel,brightnessLeveldefault=" + defaultBrightness + ",for_temp1.y <= temp2.y,x" + pointItem.x + ", y = " + pointItem.y);
                }
            } else {
                brightnessLevel = (((pointItem.x - temp1.x) / (pointItem.y - temp1.y)) * (brightnessNit - temp1.y)) + temp1.x;
            }
        }
        if (HWDEBUG) {
            String str2 = TAG;
            Slog.d(str2, "NitToBrightnessLevel,brightnessNit=" + brightnessNit + ",TobrightnessLevel=" + brightnessLevel + ",defaultBrightness=" + defaultBrightness);
        }
        return (int) (0.5f + brightnessLevel);
    }

    @Override // com.android.server.display.HwBrightnessBatteryDetection.Callbacks
    public void updateBrightnessFromBattery(int lowBatteryMaxBrightness) {
        this.mLowBatteryMaxBrightness = lowBatteryMaxBrightness;
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "mLowBatteryMaxBrightness = " + this.mLowBatteryMaxBrightness);
        }
        if (this.mManualModeEnable) {
            this.mCallbacks.updateManualBrightnessForLux();
        }
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

    public int getAmbientLux() {
        HwNormalizedManualBrightnessThresholdDetector hwNormalizedManualBrightnessThresholdDetector = this.mOutdoorDetector;
        if (hwNormalizedManualBrightnessThresholdDetector == null) {
            return 0;
        }
        return hwNormalizedManualBrightnessThresholdDetector.getCurrentFilteredAmbientLux();
    }

    public int getBrightnessLevel(int lux) {
        return 0;
    }

    private final class HwNormalizedManualBrightnessBatteryHandler extends Handler {
        public HwNormalizedManualBrightnessBatteryHandler(Looper looper) {
            super(looper, null, true);
        }
    }

    /* access modifiers changed from: private */
    public void updateFrontCameraMaxBrightness() {
        int brightness;
        if (!this.mData.frontCameraMaxBrightnessEnable) {
            this.mFrontCameraMaxBrightness = 255;
            return;
        }
        if (this.mManualAmbientLuxForCamera >= this.mData.frontCameraLuxThreshold || !"1".equals(this.mCurCameraId) || this.mFrontCameraAppKeepBrightnessEnable) {
            brightness = 255;
        } else {
            brightness = this.mData.frontCameraMaxBrightness;
        }
        if (brightness != this.mFrontCameraMaxBrightness) {
            updateFrontCameraBrightnessDimmingEnable();
            this.mFrontCameraMaxBrightness = brightness;
            if (this.mLightSensorEnable) {
                Handler handler = this.mHandler;
                if (handler != null) {
                    handler.removeMessages(2);
                    this.mHandler.sendEmptyMessageDelayed(2, (long) this.mData.frontCameraUpdateBrightnessDelayTime);
                }
                if (HWFLOW) {
                    String str = TAG;
                    Slog.i(str, "updateFrontCameraMaxBrightness, manual brightness=" + brightness + ",lux=" + this.mManualAmbientLuxForCamera + ",mKeepBrightnessEnable=" + this.mFrontCameraAppKeepBrightnessEnable);
                }
            }
        }
    }

    private void updateFrontCameraBrightnessDimmingEnable() {
        Handler handler;
        this.mFrontCameraDimmingEnable = this.mManualbrightness > this.mData.frontCameraMaxBrightness;
        if (this.mFrontCameraDimmingEnable && (handler = this.mHandler) != null) {
            handler.removeMessages(3);
            this.mHandler.sendEmptyMessageDelayed(3, (long) this.mData.frontCameraUpdateDimmingEnableTime);
        }
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "mFrontCameraDimmingEnable=" + this.mFrontCameraDimmingEnable + ",mManualbrightness=" + this.mManualbrightness);
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
    public void updateBrightness(int msg) {
        if (this.mCallbacks == null) {
            Slog.w(TAG, "mCallbacks==null,no updateBrightness");
            return;
        }
        if (HWFLOW) {
            String str = TAG;
            Slog.i(str, "updateBrightness for callback,msg=" + msg);
        }
        this.mCallbacks.updateManualBrightnessForLux();
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
