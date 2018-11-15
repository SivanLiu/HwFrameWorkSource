package com.android.server.display;

import android.content.Context;
import android.graphics.PointF;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.System;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.server.HwServiceFactory;
import com.android.server.display.DisplayEffectMonitor.MonitorModule;
import com.android.server.display.HwBrightnessPgSceneDetection.HwBrightnessPgSceneDetectionCallbacks;
import com.android.server.display.HwBrightnessXmlLoader.Data;
import com.android.server.display.HwLightSensorController.LightSensorCallbacks;
import com.android.server.display.ManualBrightnessController.ManualBrightnessCallbacks;
import com.android.server.gesture.GestureNavConst;
import java.util.ArrayList;
import java.util.List;

public class HwNormalizedManualBrightnessController extends ManualBrightnessController implements LightSensorCallbacks, HwBrightnessPgSceneDetectionCallbacks {
    private static final int BACK_SENSOR_REPORT_TIMEOUT = 300;
    private static boolean DEBUG = false;
    private static final int DEFAULT = 0;
    private static final float DEFAULT_POWERSAVING_RATIO = 1.0f;
    private static final int INDOOR = 1;
    private static final int MAXDEFAULTBRIGHTNESS = 255;
    private static final int MINDEFAULTBRIGHTNESS = 4;
    private static final int MSG_SENSOR_TIMEOUT = 1;
    private static final int OUTDOOR = 2;
    private static String TAG = "HwNormalizedManualBrightnessController";
    private int mAlgoSmoothLightValue;
    private boolean mAmbientLuxTimeOut;
    private boolean mAmbientLuxValid;
    private int mAutoBrightnessLevel = -1;
    private int mBackSensorCoverBrightness = -1;
    private int mBackSensorCoverLux = -1;
    private int mBrightnessNoLimitSetByApp = -1;
    private boolean mBrightnessNoLimitSetByAppAnimationEnable = false;
    private boolean mBrightnessNoLimitSetByAppEnable = false;
    private final Context mContext;
    private int mCurrentUserId = 0;
    private final Data mData = HwBrightnessXmlLoader.getData();
    private float mDefaultBrightness = 100.0f;
    private DisplayEffectMonitor mDisplayEffectMonitor;
    private Handler mHandler;
    private HwBrightnessMapping mHwBrightnessMapping;
    private HwBrightnessPgSceneDetection mHwBrightnessPgSceneDetection;
    private final boolean mIsBackSensorEnable;
    private long mLastAmbientLightToMonitorTime;
    private HwLightSensorController mLightSensorController = null;
    private boolean mLightSensorEnable;
    private int mManualAmbientLux;
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

    private final class HwNormalizedManualBrightnessHandler extends Handler {
        private HwNormalizedManualBrightnessHandler() {
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                HwNormalizedManualBrightnessController.this.updateBrightnessIfNoAmbientLuxReported();
            }
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
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
        if (this.mData.backSensorCoverModeEnable) {
            this.mHandler = new HwNormalizedManualBrightnessHandler();
        }
        if (this.mData.manualPowerSavingBrighnessLineDisableForDemo) {
            this.mManualPowerSavingBrighnessLineDisableForDemo = isDemoVersion();
        }
    }

    private void parseManualModePowerSavingCure(String powerSavingCure) {
        if (powerSavingCure == null || powerSavingCure.length() <= 0) {
            Slog.i(TAG, "powerSavingCure == null");
            return;
        }
        if (this.mPowerSavingBrighnessLinePointsList != null) {
            this.mPowerSavingBrighnessLinePointsList.clear();
        } else {
            this.mPowerSavingBrighnessLinePointsList = new ArrayList();
        }
        String[] powerSavingPoints = powerSavingCure.split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
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
        if (this.mPowerSavingBrighnessLinePointsList != null) {
            i = this.mPowerSavingBrighnessLinePointsList.size();
            for (int i2 = 0; i2 < i; i2++) {
                float[] temp = (float[]) this.mPowerSavingBrighnessLinePointsList.get(i2);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ManualPowerSavingPointsList x = ");
                    stringBuilder.append(temp[0]);
                    stringBuilder.append(", y = ");
                    stringBuilder.append(temp[1]);
                    Slog.d(str, stringBuilder.toString());
                }
            }
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

    public void updatePowerState(int state, boolean enable) {
        if (this.mManualModeEnable != enable) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("HBM SensorEnable change ");
                stringBuilder.append(this.mManualModeEnable);
                stringBuilder.append(" -> ");
                stringBuilder.append(enable);
                Slog.i(str, stringBuilder.toString());
            }
            this.mManualModeEnable = enable;
        }
        if (this.mManualModeEnable) {
            setLightSensorEnabled(wantScreenOn(state));
        } else {
            setLightSensorEnabled(this.mManualModeEnable);
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
                if (this.mHandler != null) {
                    this.mHandler.sendEmptyMessageDelayed(1, 300);
                }
                if (DEBUG) {
                    Slog.i(TAG, "ManualMode sensor enable");
                }
            }
            boolean pGBLListenerRegisted = this.mHwBrightnessPgSceneDetection.getPGBLListenerRegisted();
            if (this.mData.manualPowerSavingBrighnessLineEnable && !pGBLListenerRegisted) {
                this.mHwBrightnessPgSceneDetection.registerPgBLightSceneListener(this.mContext);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PowerSaving Manul in registerPgBLightSceneChangedListener,=");
                    stringBuilder.append(this.mHwBrightnessPgSceneDetection.getPGBLListenerRegisted());
                    Slog.d(str, stringBuilder.toString());
                }
            }
        } else if (this.mLightSensorEnable) {
            this.mLightSensorEnable = false;
            this.mLightSensorController.disableSensor();
            this.mOutdoorDetector.clearAmbientLightRingBuffer();
            if (this.mHandler != null) {
                this.mHandler.removeMessages(1);
            }
            if (DEBUG) {
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

    public void updateStateRecognition(boolean usePwrBLightCurve, int appType) {
        if (this.mData.manualPowerSavingBrighnessLineEnable && this.mManualModeEnable && !this.mManualPowerSavingBrighnessLineDisableForDemo) {
            this.mManualPowerSavingEnable = usePwrBLightCurve;
            this.mManualPowerSavingAnimationEnable = getPowerSavingModeBrightnessChangeEnable(this.mManualbrightness, usePwrBLightCurve);
            float powerRatio = covertBrightnessToPowerRatio(this.mManualbrightness);
            int tembrightness = (int) (((float) this.mManualbrightness) * powerRatio);
            int pgModeBrightness = this.mHwBrightnessPgSceneDetection.getAdjustLightValByPgMode(tembrightness);
            if (pgModeBrightness != this.mManualbrightness) {
                powerRatio = this.mHwBrightnessPgSceneDetection.getPgPowerModeRatio();
                tembrightness = pgModeBrightness;
            }
            if (this.mManualbrightnessLog != tembrightness) {
                int brightnessNit = covertBrightnessLevelToNit(this.mManualbrightness);
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PowerSaving,ManualMode mManualbrightness=");
                    stringBuilder.append(this.mManualbrightness);
                    stringBuilder.append(",brightnessNit=");
                    stringBuilder.append(brightnessNit);
                    stringBuilder.append(",powerRatio=");
                    stringBuilder.append(powerRatio);
                    stringBuilder.append(",maxNit=");
                    stringBuilder.append(this.mData.screenBrightnessMaxNit);
                    stringBuilder.append(",MinNit=");
                    stringBuilder.append(this.mData.screenBrightnessMinNit);
                    stringBuilder.append(",usePwrBLightCurve=");
                    stringBuilder.append(usePwrBLightCurve);
                    stringBuilder.append(",appType=");
                    stringBuilder.append(appType);
                    Slog.d(str, stringBuilder.toString());
                }
                this.mManualbrightnessLog = tembrightness;
                this.mCallbacks.updateManualBrightnessForLux();
                return;
            }
            return;
        }
        this.mManualPowerSavingAnimationEnable = false;
        this.mManualPowerSavingEnable = false;
    }

    private boolean getPowerSavingModeBrightnessChangeEnable(int brightness, boolean usePowerSavingModeCurveEnable) {
        boolean powerSavingModeBrightnessChangeEnable = false;
        if (this.mUsePowerSavingModeCurveEnable != usePowerSavingModeCurveEnable) {
            int tembrightness = (int) (((float) brightness) * covertBrightnessLevelToPowerRatio(brightness));
            if (brightness != tembrightness) {
                powerSavingModeBrightnessChangeEnable = true;
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("PowerSaving Enable=");
                    stringBuilder.append(true);
                    stringBuilder.append(",Pgbrightness=");
                    stringBuilder.append(tembrightness);
                    stringBuilder.append(",brightness=");
                    stringBuilder.append(brightness);
                    Slog.d(str, stringBuilder.toString());
                }
            }
        }
        this.mUsePowerSavingModeCurveEnable = usePowerSavingModeCurveEnable;
        return powerSavingModeBrightnessChangeEnable;
    }

    private float covertBrightnessLevelToPowerRatio(int brightness) {
        if ((!this.mData.manualMode || this.mManualbrightness < this.mData.manualBrightnessMaxLimit) && this.mData.manualPowerSavingBrighnessLineEnable) {
            return getPowerSavingRatio(covertBrightnessLevelToNit(this.mManualbrightness));
        }
        return 1.0f;
    }

    public int getManualBrightness() {
        if (this.mBrightnessNoLimitSetByApp > 0) {
            return this.mBrightnessNoLimitSetByApp;
        }
        int brightnessNit;
        String str;
        StringBuilder stringBuilder;
        String str2;
        StringBuilder stringBuilder2;
        if (this.mData.backSensorCoverModeEnable) {
            if (this.mBackSensorCoverBrightness > 0) {
                if (this.mAutoBrightnessLevel != this.mBackSensorCoverBrightness && System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, this.mCurrentUserId) == 1) {
                    System.putIntForUser(this.mContext.getContentResolver(), "screen_auto_brightness", this.mBackSensorCoverBrightness, this.mCurrentUserId);
                    if (DEBUG) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("LabcCoverMode mAutoBrightnessLevel=");
                        stringBuilder3.append(this.mAutoBrightnessLevel);
                        stringBuilder3.append(",mBackSensorCoverBrightness=");
                        stringBuilder3.append(this.mBackSensorCoverBrightness);
                        Slog.d(str3, stringBuilder3.toString());
                    }
                }
                this.mAutoBrightnessLevel = this.mBackSensorCoverBrightness;
                return this.mBackSensorCoverBrightness;
            } else if (!(this.mAmbientLuxValid || this.mAmbientLuxTimeOut || !HwServiceFactory.isCoverClosed())) {
                this.mAutoBrightnessLevel = -1;
                return -1;
            }
        }
        float powerSavingRatio = covertBrightnessToPowerRatio(this.mManualbrightness);
        if (DEBUG && Math.abs(this.mPowerRatio - powerSavingRatio) > 1.0E-7f) {
            brightnessNit = covertBrightnessLevelToNit(this.mManualbrightness);
            this.mPowerRatio = powerSavingRatio;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PowerSaving powerSavingRatio=");
            stringBuilder.append(powerSavingRatio);
            stringBuilder.append(",mManualbrightness=");
            stringBuilder.append(this.mManualbrightness);
            stringBuilder.append(",brightnessNit=");
            stringBuilder.append(brightnessNit);
            Slog.d(str, stringBuilder.toString());
        }
        brightnessNit = (int) (((float) this.mManualbrightness) * powerSavingRatio);
        if (brightnessNit < 4) {
            brightnessNit = 4;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("warning mManualbrightness < min,Manualbrightness=");
            stringBuilder.append(4);
            Slog.w(str, stringBuilder.toString());
        }
        if (brightnessNit > 255) {
            brightnessNit = 255;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("warning mManualbrightness > max,Manualbrightness=");
            stringBuilder.append(255);
            Slog.w(str, stringBuilder.toString());
        }
        this.mManualbrightnessOut = brightnessNit;
        if (!this.mData.manualMode) {
            this.mManualbrightnessOut = brightnessNit;
            if (DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mManualbrightnessOut=");
                stringBuilder.append(this.mManualbrightnessOut);
                stringBuilder.append(",mData.manualMode=");
                stringBuilder.append(this.mData.manualMode);
                Slog.i(str, stringBuilder.toString());
            }
        } else if (this.mManualbrightnessOut >= this.mData.manualBrightnessMaxLimit) {
            float defaultBrightness = getDefaultBrightnessLevelNew(this.mData.defaultBrighnessLinePoints, (float) this.mManualAmbientLux);
            if (this.mOutdoorScene == 2) {
                int mManualbrightnessTmpMin = brightnessNit < this.mData.manualBrightnessMaxLimit ? brightnessNit : this.mData.manualBrightnessMaxLimit;
                this.mManualbrightnessOut = mManualbrightnessTmpMin > ((int) defaultBrightness) ? mManualbrightnessTmpMin : (int) defaultBrightness;
                if (DEBUG) {
                    String str4 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("mManualbrightnessOut=");
                    stringBuilder4.append(this.mManualbrightnessOut);
                    stringBuilder4.append(",defaultBrightness=");
                    stringBuilder4.append(defaultBrightness);
                    stringBuilder4.append(",AutoLux=");
                    stringBuilder4.append(this.mManualAmbientLux);
                    Slog.i(str4, stringBuilder4.toString());
                }
            } else {
                this.mManualbrightnessOut = brightnessNit < this.mData.manualBrightnessMaxLimit ? brightnessNit : this.mData.manualBrightnessMaxLimit;
                if (DEBUG) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("mManualbrightnessOut1=");
                    stringBuilder2.append(this.mManualbrightnessOut);
                    stringBuilder2.append(",defaultBrightness=");
                    stringBuilder2.append(defaultBrightness);
                    stringBuilder2.append(",AutoLux=");
                    stringBuilder2.append(this.mManualAmbientLux);
                    Slog.i(str2, stringBuilder2.toString());
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
            if (DEBUG) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("PG_POWER_SAVE_MODE mManualbrightnessOut=");
                stringBuilder2.append(this.mManualbrightnessOut);
                stringBuilder2.append(",mManualbrightness=");
                stringBuilder2.append(this.mManualbrightness);
                stringBuilder2.append(",mPowerRatio=");
                stringBuilder2.append(this.mPowerRatio);
                Slog.d(str2, stringBuilder2.toString());
            }
        }
        if (this.mManualbrightnessOut > this.mMaxBrightnessSetByThermal) {
            if (DEBUG) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ThermalMode OrgManualbrightnessOut=");
                stringBuilder2.append(this.mManualbrightnessOut);
                stringBuilder2.append(",mMaxBrightnessSetByThermal=");
                stringBuilder2.append(this.mMaxBrightnessSetByThermal);
                Slog.d(str2, stringBuilder2.toString());
            }
            this.mManualbrightnessOut = this.mMaxBrightnessSetByThermal;
        }
        return this.mManualbrightnessOut;
    }

    public int getMaxBrightnessForSeekbar() {
        return this.mData.manualMode ? this.mData.manualBrightnessMaxLimit : 255;
    }

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
        if (needUpdateManualBrightness) {
            this.mCallbacks.updateManualBrightnessForLux();
            this.mOutdoorDetector.setLuxChangedFlagForHBM();
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mManualAmbientLux =");
                stringBuilder.append(this.mManualAmbientLux);
                stringBuilder.append(", mManualModeAnimationEnable=");
                stringBuilder.append(this.mManualModeAnimationEnable);
                Slog.i(str, stringBuilder.toString());
            }
        } else {
            this.mManualModeAnimationEnable = false;
        }
        sendAmbientLightToMonitor(timeInMs, (float) lux);
        sendDefaultBrightnessToMonitor();
    }

    private void updateBrightnessIfNoAmbientLuxReported() {
        if (!this.mAmbientLuxValid) {
            this.mAmbientLuxTimeOut = true;
            if (DEBUG) {
                Slog.d(TAG, "BackSensorCoverMode sensor doesn't report lux in 300ms");
            }
            this.mCallbacks.updateManualBrightnessForLux();
        }
    }

    private boolean isManualMode() {
        return System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 1, this.mCurrentUserId) == 0;
    }

    private boolean needUpdateBrightWhileCoverClosed(int mixedSensorValue) {
        if (!this.mIsBackSensorEnable) {
            return false;
        }
        if (HwServiceFactory.isCoverClosed()) {
            int backSensorValue = this.mLightSensorController.getBackSensorValue();
            this.mBackSensorCoverLux = this.mBackSensorCoverLux > backSensorValue ? this.mBackSensorCoverLux : backSensorValue;
            this.mBackSensorCoverLux = this.mBackSensorCoverLux > mixedSensorValue ? this.mBackSensorCoverLux : mixedSensorValue;
            if (this.mData.backSensorCoverModeMinLuxInRing > 0 && isPhoneInRing()) {
                this.mBackSensorCoverLux = this.mBackSensorCoverLux > this.mData.backSensorCoverModeMinLuxInRing ? this.mBackSensorCoverLux : this.mData.backSensorCoverModeMinLuxInRing;
            }
            int backSensorCoverBrightness = (int) getDefaultBrightnessLevelNew(this.mData.backSensorCoverModeBrighnessLinePoints, (float) this.mBackSensorCoverLux);
            boolean isManualMode = isManualMode();
            if (isManualMode && backSensorCoverBrightness < this.mManualbrightness) {
                backSensorCoverBrightness = -1;
            }
            if (backSensorCoverBrightness == this.mBackSensorCoverBrightness) {
                return false;
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("BackSensorCoverMode mixed=");
                stringBuilder.append(mixedSensorValue);
                stringBuilder.append(", back=");
                stringBuilder.append(backSensorValue);
                stringBuilder.append(", lux=");
                stringBuilder.append(this.mBackSensorCoverLux);
                stringBuilder.append(", bright=");
                stringBuilder.append(backSensorCoverBrightness);
                stringBuilder.append(", isManualMode=");
                stringBuilder.append(isManualMode);
                Slog.d(str, stringBuilder.toString());
            }
            this.mBackSensorCoverBrightness = backSensorCoverBrightness;
            return true;
        }
        if (this.mBackSensorCoverBrightness > 0) {
            boolean isManualMode2 = isManualMode();
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("BackSensorCoverMode cover open, isManualMode=");
                stringBuilder2.append(isManualMode2);
                Slog.d(str2, stringBuilder2.toString());
            }
            this.mBackSensorCoverLux = -1;
            this.mBackSensorCoverBrightness = -1;
            if (isManualMode2) {
                return true;
            }
        }
        return false;
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
        }
    }

    public boolean needFastestRateForManualBrightness() {
        if (this.mData.backSensorCoverModeEnable && this.mBackSensorCoverBrightness > 0 && HwServiceFactory.isCoverClosed()) {
            return true;
        }
        return false;
    }

    private float covertBrightnessToPowerRatio(int brightness) {
        if ((this.mData.manualMode && this.mManualbrightness >= this.mData.manualBrightnessMaxLimit) || !this.mData.manualPowerSavingBrighnessLineEnable) {
            return 1.0f;
        }
        int brightnessNit = covertBrightnessLevelToNit(this.mManualbrightness);
        float powerRatio = 1.0f;
        if (this.mManualPowerSavingEnable) {
            powerRatio = getPowerSavingRatio(brightnessNit);
        }
        return powerRatio;
    }

    private int covertBrightnessLevelToNit(int brightness) {
        if (brightness == 0) {
            return brightness;
        }
        if (brightness < 4) {
            brightness = 4;
        }
        if (brightness > 255) {
            brightness = 255;
        }
        return (int) (((((float) (brightness - 4)) * (this.mData.screenBrightnessMaxNit - this.mData.screenBrightnessMinNit)) / 251.0f) + this.mData.screenBrightnessMinNit);
    }

    private float getPowerSavingRatio(int brightnssnit) {
        StringBuilder stringBuilder;
        if (this.mPowerSavingBrighnessLinePointsList == null || this.mPowerSavingBrighnessLinePointsList.size() == 0 || brightnssnit < 0) {
            String str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("PowerSavingBrighnessLinePointsList warning,set PowerSavingRatio,brightnssnit=");
            stringBuilder.append(brightnssnit);
            Slog.e(str, stringBuilder.toString());
            return 1.0f;
        }
        int linePointsListLength = this.mPowerSavingBrighnessLinePointsList.size();
        float[] temp = (float[]) this.mPowerSavingBrighnessLinePointsList.get(0);
        if (((float) brightnssnit) < temp[0]) {
            return 1.0f;
        }
        String str2;
        float tmpPowerSavingRatio = 1.0f;
        float[] temp1 = null;
        for (int i = 0; i <= linePointsListLength - 1; i++) {
            float[] temp2 = (float[]) this.mPowerSavingBrighnessLinePointsList.get(i);
            if (temp1 == null) {
                temp1 = temp2;
            }
            if (((float) brightnssnit) < temp2[0]) {
                float[] temp22 = temp2;
                if (temp22[0] <= temp1[0]) {
                    tmpPowerSavingRatio = 1.0f;
                    Slog.w(TAG, "temp2[0] <= temp1[0] warning,set default tmpPowerSavingRatio");
                } else {
                    tmpPowerSavingRatio = (((temp22[1] - temp1[1]) / (temp22[0] - temp1[0])) * (((float) brightnssnit) - temp1[0])) + temp1[1];
                }
                if (tmpPowerSavingRatio > 1.0f || tmpPowerSavingRatio < GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO) {
                    str2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("tmpPowerSavingRatio warning,set default value, tmpPowerSavingRatio= ");
                    stringBuilder.append(this.mPowerRatio);
                    Slog.w(str2, stringBuilder.toString());
                    tmpPowerSavingRatio = 1.0f;
                }
                return tmpPowerSavingRatio;
            }
            temp1 = temp2;
            tmpPowerSavingRatio = temp1[1];
        }
        str2 = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("tmpPowerSavingRatio warning,set default value, tmpPowerSavingRatio= ");
        stringBuilder.append(this.mPowerRatio);
        Slog.w(str2, stringBuilder.toString());
        tmpPowerSavingRatio = 1.0f;
        return tmpPowerSavingRatio;
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
            params.put("brightnessMode", "MANUAL");
            this.mDisplayEffectMonitor.sendMonitorParam(params);
        }
    }

    private void sendDefaultBrightnessToMonitor() {
        if (this.mDisplayEffectMonitor != null) {
            int lightValue = (int) this.mOutdoorDetector.getFilterLuxFromManualMode();
            if (this.mAlgoSmoothLightValue != lightValue) {
                this.mAlgoSmoothLightValue = lightValue;
                ArrayMap<String, Object> params = new ArrayMap();
                params.put(MonitorModule.PARAM_TYPE, "algoDefaultBrightness");
                params.put("lightValue", Integer.valueOf(lightValue));
                params.put("brightness", Integer.valueOf(0));
                params.put("brightnessMode", "MANUAL");
                this.mDisplayEffectMonitor.sendMonitorParam(params);
            }
        }
    }

    public float getDefaultBrightnessLevelNew(List<PointF> linePointsList, float lux) {
        List<PointF> linePointsListIn = linePointsList;
        float brightnessLevel = this.mDefaultBrightness;
        PointF temp1 = null;
        for (PointF temp : linePointsListIn) {
            if (temp1 == null) {
                temp1 = temp;
            }
            if (lux < temp.x) {
                PointF temp2 = temp;
                if (temp2.x > temp1.x) {
                    return (((temp2.y - temp1.y) / (temp2.x - temp1.x)) * (lux - temp1.x)) + temp1.y;
                }
                brightnessLevel = this.mDefaultBrightness;
                if (!DEBUG) {
                    return brightnessLevel;
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("DefaultBrighness_temp1.x <= temp2.x,x");
                stringBuilder.append(temp.x);
                stringBuilder.append(", y = ");
                stringBuilder.append(temp.y);
                Slog.i(str, stringBuilder.toString());
                return brightnessLevel;
            }
            temp1 = temp;
            brightnessLevel = temp1.y;
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ThermalMode set Manual MaxBrightness=");
            stringBuilder.append(brightness);
            stringBuilder.append(",mappingBrightness=");
            stringBuilder.append(mappingBrightness);
            Slog.i(str, stringBuilder.toString());
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setBrightnessNoLimit set brightness=");
        stringBuilder.append(brightness);
        stringBuilder.append(",time=");
        stringBuilder.append(time);
        Slog.i(str, stringBuilder.toString());
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
}
