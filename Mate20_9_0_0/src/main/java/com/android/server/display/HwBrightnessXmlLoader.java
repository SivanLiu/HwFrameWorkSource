package com.android.server.display;

import android.graphics.PointF;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.lights.Light;
import com.android.server.lights.LightsManager;
import com.android.server.net.HwNetworkStatsService;
import com.android.server.rms.iaware.cpu.CPUFeature;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.displayengine.IDisplayEngineServiceEx;
import com.huawei.displayengine.IDisplayEngineServiceEx.Stub;
import com.huawei.msdp.devicestatus.DeviceStatusConstant;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

final class HwBrightnessXmlLoader {
    private static final boolean HWDEBUG;
    private static final boolean HWFLOW;
    private static final String LCD_PANEL_TYPE_PATH = "/sys/class/graphics/fb0/lcd_model";
    private static final String TAG = "HwBrightnessXmlLoader";
    private static final String TOUCH_OEM_INFO_PATH = "/sys/touchscreen/touch_oem_info";
    private static final String XML_EXT = ".xml";
    private static final String XML_NAME = "LABCConfig.xml";
    private static final String XML_NAME_NOEXT = "LABCConfig";
    private static Data mData = new Data();
    private static HwBrightnessXmlLoader mLoader;
    private static final Object mLock = new Object();
    private final int mDeviceActualBrightnessLevel;

    public static class Data {
        public float adapted2UnadaptShortFilterLux = 5.0f;
        public int adapting2AdaptedOffDurationFilterSec = 1800;
        public int adapting2AdaptedOffDurationMaxSec = 28800;
        public int adapting2AdaptedOffDurationMinSec = 10;
        public int adapting2AdaptedOnClockNoFilterBeginHour = 21;
        public int adapting2AdaptedOnClockNoFilterEndHour = 7;
        public float adapting2UnadaptShortFilterLux = 5.0f;
        public boolean allowLabcUseProximity = false;
        public List<HwXmlAmPoint> ambientLuxValidBrightnessPoints = new ArrayList();
        public boolean animatedStepRoundEnabled = false;
        public boolean animatingForRGBWEnable = false;
        public boolean animationEqualRatioEnable = false;
        public float autoFastTimeFor255 = 0.5f;
        public boolean autoModeInOutDoorLimitEnble = false;
        public boolean autoPowerSavingBrighnessLineDisableForDemo = false;
        public boolean autoPowerSavingUseManualAnimationTimeEnable = false;
        public List<PointF> backSensorCoverModeBrighnessLinePoints = new ArrayList();
        public boolean backSensorCoverModeEnable = false;
        public int backSensorCoverModeMinLuxInRing = 0;
        public int brighenDebounceTime = 1000;
        public int brighenDebounceTimeForSmallThr = HwAPPQoEUtils.APP_TYPE_STREAMING;
        public int brightTimeDelay = 1000;
        public boolean brightTimeDelayEnable = false;
        public float brightTimeDelayLuxThreshold = 30.0f;
        public float brightenDebounceTimeParaBig = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        public float brightenDeltaLuxPara = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        public float brightenGradualTime = 1.0f;
        public int brightenThresholdFor255 = 1254;
        public List<PointF> brightenlinePoints = new ArrayList();
        public boolean brightnessCalibrationEnabled = false;
        public List<PointF> brightnessMappingPoints = new ArrayList();
        public float cameraAnimationTime = 3.0f;
        public boolean cameraModeEnable = false;
        public int converModeDayBeginTime = 6;
        public List<PointF> coverModeBrighnessLinePoints = new ArrayList();
        public long coverModeBrightenResponseTime = 1000;
        public long coverModeDarkenResponseTime = 1000;
        public int coverModeDayBrightness = CPUFeature.MSG_RESET_ON_FIRE;
        public boolean coverModeDayEnable = false;
        public int coverModeDayEndTime = 18;
        public float coverModeFirstLux = 2210.0f;
        public boolean coverModelastCloseScreenEnable = false;
        public long cryogenicActiveScreenOffIntervalInMillis = HwNetworkStatsService.UPLOAD_INTERVAL;
        public boolean cryogenicEnable = false;
        public long cryogenicLagTimeInMillis = HwNetworkStatsService.UPLOAD_INTERVAL;
        public long cryogenicMaxBrightnessTimeOut = 9000;
        public boolean cryogenicModeBrightnessMappingEnable = false;
        public boolean darkAdapterEnable = false;
        public int darkLightLevelMaxThreshold = 0;
        public int darkLightLevelMinThreshold = 0;
        public float darkLightLevelRatio = 1.0f;
        public float darkLightLuxDelta = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        public float darkLightLuxMaxThreshold = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        public float darkLightLuxMinThreshold = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        public int darkTimeDelay = 10000;
        public float darkTimeDelayBeta0 = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        public float darkTimeDelayBeta1 = 1.0f;
        public float darkTimeDelayBeta2 = 0.333f;
        public boolean darkTimeDelayEnable = false;
        public float darkTimeDelayLuxThreshold = 50.0f;
        public int darkenCurrentFor255 = DeviceStatusConstant.TYPE_HEAD_DOWN;
        public int darkenDebounceTime = HwAPPQoEUtils.APP_TYPE_STREAMING;
        public int darkenDebounceTimeForSmallThr = 8000;
        public float darkenDebounceTimeParaBig = 1.0f;
        public float darkenDeltaLuxPara = 1.0f;
        public float darkenGradualTime = 3.0f;
        public float darkenGradualTimeMax = 3.0f;
        public float darkenGradualTimeMin = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        public int darkenTargetFor255 = 1254;
        public List<PointF> darkenlinePoints = new ArrayList();
        public boolean dayModeAlgoEnable = false;
        public int dayModeBeginTime = 5;
        public int dayModeEndTime = 23;
        public int dayModeModifyMinBrightness = 6;
        public int dayModeModifyNumPoint = 3;
        public int dayModeSwitchTime = 30;
        public List<PointF> defaultBrighnessLinePoints = new ArrayList();
        public float defaultBrightness = 100.0f;
        public float dimTime = 3.0f;
        public List<HwXmlAmPoint> gameModeAmbientLuxValidBrightnessPoints = new ArrayList();
        public float gameModeBrightenAnimationTime = 0.5f;
        public long gameModeBrightenDebounceTime = 1000;
        public long gameModeClearOffsetTime = MemoryConstant.MIN_INTERVAL_OP_TIMEOUT;
        public long gameModeDarkenDebounceTime = MemoryConstant.MIN_INTERVAL_OP_TIMEOUT;
        public float gameModeDarkentenAnimationTime = 0.5f;
        public float gameModeDarkentenLongAnimationTime = 0.5f;
        public int gameModeDarkentenLongCurrent = 0;
        public int gameModeDarkentenLongTarget = 0;
        public boolean gameModeEnable = false;
        public boolean gameModeOffsetValidAmbientLuxEnable = false;
        public int inDoorThreshold = HwAPPQoEUtils.APP_TYPE_STREAMING;
        public float initDoubleSensorInterfere = 8.0f;
        public int initNumLastBuffer = 10;
        public float initSigmoidFuncSlope = 0.75f;
        public int initSlowReponseBrightTime = 0;
        public int initSlowReponseUpperLuxThreshold = 20;
        public int initUpperLuxThreshold = 20;
        public long initValidCloseTime = -1;
        public float keyguardAnimationBrightenTime = 0.5f;
        public float keyguardAnimationDarkenTime = -1.0f;
        public float keyguardLuxThreshold = 20.0f;
        public int keyguardResponseBrightenTime = 500;
        public int keyguardResponseDarkenTime = -1;
        public boolean lastCloseScreenEnable = false;
        public int lightSensorRateMills = 300;
        public float manualAnimationBrightenTime = 0.5f;
        public float manualAnimationDarkenTime = 0.5f;
        public int manualBrighenDebounceTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
        public List<PointF> manualBrightenlinePoints = new ArrayList();
        public int manualBrightnessMaxLimit = 255;
        public int manualBrightnessMinLimit = 4;
        public int manualDarkenDebounceTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
        public List<PointF> manualDarkenlinePoints = new ArrayList();
        public float manualFastTimeFor255 = 0.5f;
        public boolean manualMode = false;
        public float manualPowerSavingAnimationBrightenTime = 0.5f;
        public float manualPowerSavingAnimationDarkenTime = 0.5f;
        public boolean manualPowerSavingBrighnessLineDisableForDemo = false;
        public boolean manualPowerSavingBrighnessLineEnable = false;
        public float manualThermalModeAnimationBrightenTime = 0.5f;
        public float manualThermalModeAnimationDarkenTime = 0.5f;
        public float minAnimatingStep = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        public boolean monitorEnable = false;
        public int offsetBrightenDebounceTime = 1000;
        public int offsetDarkenDebounceTime = 1000;
        public boolean offsetResetEnable = false;
        public int offsetResetShortLuxDelta = 50000;
        public int offsetResetShortSwitchTime = 10;
        public int offsetResetSwitchTime = 10;
        public boolean offsetValidAmbientLuxEnable = false;
        public int outDoorThreshold = 8000;
        public float outdoorAnimationBrightenTime = 1.5f;
        public float outdoorAnimationDarkenTime = -1.0f;
        public int outdoorLowerLuxThreshold = 1000;
        public float outdoorResponseBrightenRatio = -1.0f;
        public int outdoorResponseBrightenTime = -1;
        public int outdoorResponseCount = 5;
        public float outdoorResponseDarkenRatio = -1.0f;
        public int outdoorResponseDarkenTime = -1;
        public boolean pgModeBrightnessMappingEnable = false;
        public boolean pgReregisterScene = false;
        public int pgSceneDetectionBrightenDelayTime = 500;
        public int pgSceneDetectionDarkenDelayTime = 2500;
        public int postMaxMinAvgFilterNoFilterNum = 6;
        public int postMaxMinAvgFilterNum = 5;
        public int postMeanFilterNoFilterNum = 4;
        public int postMeanFilterNum = 3;
        public int postMethodNum = 2;
        public int powerOnBrightenDebounceTime = 500;
        public int powerOnDarkenDebounceTime = 1000;
        public int powerOnFastResponseLuxNum = 8;
        public int preMeanFilterNoFilterNum = 7;
        public int preMeanFilterNum = 3;
        public int preMethodNum = 0;
        public float preWeightedMeanFilterAlpha = 0.5f;
        public float preWeightedMeanFilterLuxTh = 12.0f;
        public int preWeightedMeanFilterMaxFuncLuxNum = 3;
        public int preWeightedMeanFilterNoFilterNum = 7;
        public int preWeightedMeanFilterNum = 3;
        public float proximityLuxThreshold = 20.0f;
        public int proximityNegativeDebounceTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
        public int proximityPositiveDebounceTime = 150;
        public int proximityResponseBrightenTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
        public float ratioForBrightnenSmallThr = 1.0f;
        public float ratioForDarkenSmallThr = 1.0f;
        public float readingAnimationTime = 3.0f;
        public boolean readingModeEnable = false;
        public boolean rebootAutoModeEnable = false;
        public int rebootFirstBrightness = 10000;
        public boolean rebootFirstBrightnessAnimationEnable = false;
        public float rebootFirstBrightnessAutoTime = 3.0f;
        public float rebootFirstBrightnessManualTime = 3.0f;
        public boolean reportValueWhenSensorOnChange = true;
        public float sceneAmbientLuxMaxWeight = 0.5f;
        public float sceneAmbientLuxMinWeight = 0.5f;
        public int sceneGapPoints = 29;
        public int sceneMaxPoints = 0;
        public int sceneMinPoints = 29;
        public float screenBrightnessMaxNit = 530.0f;
        public float screenBrightnessMinNit = 2.0f;
        public int stabilityConstant = 5;
        public int stabilityTime1 = 20;
        public int stabilityTime2 = 10;
        public boolean thermalModeBrightnessMappingEnable = false;
        public boolean touchProximityEnable = false;
        public float touchProximityYNearbyRatio = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        public int unadapt2AdaptedOffDurationMinSec = 2700;
        public int unadapt2AdaptedOnClockNoFilterBeginHour = 21;
        public int unadapt2AdaptedOnClockNoFilterEndHour = 7;
        public float unadapt2AdaptedShortFilterLux = 5.0f;
        public int unadapt2AdaptingDimSec = 60;
        public float unadapt2AdaptingLongFilterLux = 1.0f;
        public int unadapt2AdaptingLongFilterSec = GestureNavConst.GESTURE_GO_HOME_MIN_DISTANCE_THRESHOLD;
        public float unadapt2AdaptingShortFilterLux = 5.0f;
        public boolean useVariableStep = false;
        public long vehicleModeDisableTimeMillis = MemoryConstant.MIN_INTERVAL_OP_TIMEOUT;
        public boolean vehicleModeEnable = false;
        public long vehicleModeEnterTimeForPowerOn = 200;
        public long vehicleModeQuitTimeForPowerOn = 200;

        public Data() {
            loadDefaultConfig();
        }

        public void printData() {
            if (HwBrightnessXmlLoader.HWFLOW) {
                String str = HwBrightnessXmlLoader.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("printData() lightSensorRateMills=");
                stringBuilder.append(this.lightSensorRateMills);
                stringBuilder.append(", brighenDebounceTime=");
                stringBuilder.append(this.brighenDebounceTime);
                stringBuilder.append(", darkenDebounceTime=");
                stringBuilder.append(this.darkenDebounceTime);
                stringBuilder.append(", brightenDebounceTimeParaBig=");
                stringBuilder.append(this.brightenDebounceTimeParaBig);
                stringBuilder.append(", darkenDebounceTimeParaBig=");
                stringBuilder.append(this.darkenDebounceTimeParaBig);
                stringBuilder.append(", brightenDeltaLuxPara=");
                stringBuilder.append(this.brightenDeltaLuxPara);
                stringBuilder.append(", darkenDeltaLuxPara=");
                stringBuilder.append(this.darkenDeltaLuxPara);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() stabilityConstant=");
                stringBuilder.append(this.stabilityConstant);
                stringBuilder.append(", stabilityTime1=");
                stringBuilder.append(this.stabilityTime1);
                stringBuilder.append(", stabilityTime2=");
                stringBuilder.append(this.stabilityTime2);
                stringBuilder.append(", brighenDebounceTimeForSmallThr=");
                stringBuilder.append(this.brighenDebounceTimeForSmallThr);
                stringBuilder.append(", darkenDebounceTimeForSmallThr=");
                stringBuilder.append(this.darkenDebounceTimeForSmallThr);
                stringBuilder.append(", ratioForBrightnenSmallThr=");
                stringBuilder.append(this.ratioForBrightnenSmallThr);
                stringBuilder.append(", ratioForDarkenSmallThr=");
                stringBuilder.append(this.ratioForDarkenSmallThr);
                stringBuilder.append(", rebootAutoModeEnable=");
                stringBuilder.append(this.rebootAutoModeEnable);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() darkTimeDelayEnable=");
                stringBuilder.append(this.darkTimeDelayEnable);
                stringBuilder.append(", darkTimeDelay=");
                stringBuilder.append(this.darkTimeDelay);
                stringBuilder.append(", darkTimeDelayLuxThreshold=");
                stringBuilder.append(this.darkTimeDelayLuxThreshold);
                stringBuilder.append(", coverModeFirstLux=");
                stringBuilder.append(this.coverModeFirstLux);
                stringBuilder.append(", lastCloseScreenEnable=");
                stringBuilder.append(this.lastCloseScreenEnable);
                stringBuilder.append(", coverModeBrightenResponseTime=");
                stringBuilder.append(this.coverModeBrightenResponseTime);
                stringBuilder.append(", coverModeDarkenResponseTime=");
                stringBuilder.append(this.coverModeDarkenResponseTime);
                stringBuilder.append(", coverModelastCloseScreenEnable=");
                stringBuilder.append(this.coverModelastCloseScreenEnable);
                stringBuilder.append(", coverModeDayEnable =");
                stringBuilder.append(this.coverModeDayEnable);
                stringBuilder.append(", coverModeDayBrightness =");
                stringBuilder.append(this.coverModeDayBrightness);
                stringBuilder.append(", converModeDayBeginTime =");
                stringBuilder.append(this.converModeDayBeginTime);
                stringBuilder.append(", coverModeDayEndTime =");
                stringBuilder.append(this.coverModeDayEndTime);
                stringBuilder.append(", postMaxMinAvgFilterNoFilterNum=");
                stringBuilder.append(this.postMaxMinAvgFilterNoFilterNum);
                stringBuilder.append(", postMaxMinAvgFilterNum=");
                stringBuilder.append(this.postMaxMinAvgFilterNum);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() brightTimeDelayEnable=");
                stringBuilder.append(this.brightTimeDelayEnable);
                stringBuilder.append(", brightTimeDelay=");
                stringBuilder.append(this.brightTimeDelay);
                stringBuilder.append(", brightTimeDelayLuxThreshold=");
                stringBuilder.append(this.brightTimeDelayLuxThreshold);
                stringBuilder.append(", preMethodNum=");
                stringBuilder.append(this.preMethodNum);
                stringBuilder.append(", preMeanFilterNoFilterNum=");
                stringBuilder.append(this.preMeanFilterNoFilterNum);
                stringBuilder.append(", preMeanFilterNum=");
                stringBuilder.append(this.preMeanFilterNum);
                stringBuilder.append(", postMethodNum=");
                stringBuilder.append(this.postMethodNum);
                stringBuilder.append(", postMeanFilterNoFilterNum=");
                stringBuilder.append(this.postMeanFilterNoFilterNum);
                stringBuilder.append(", postMeanFilterNum=");
                stringBuilder.append(this.postMeanFilterNum);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() preWeightedMeanFilterNoFilterNum=");
                stringBuilder.append(this.preWeightedMeanFilterNoFilterNum);
                stringBuilder.append(",preWeightedMeanFilterNum=");
                stringBuilder.append(this.preWeightedMeanFilterNum);
                stringBuilder.append(",preWeightedMeanFilterMaxFuncLuxNum=");
                stringBuilder.append(this.preWeightedMeanFilterMaxFuncLuxNum);
                stringBuilder.append(",preWeightedMeanFilterAlpha=");
                stringBuilder.append(this.preWeightedMeanFilterAlpha);
                stringBuilder.append(",preWeightedMeanFilterLuxTh=");
                stringBuilder.append(this.preWeightedMeanFilterLuxTh);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() darkTimeDelayBeta0=");
                stringBuilder.append(this.darkTimeDelayBeta0);
                stringBuilder.append(",darkTimeDelayBeta1=");
                stringBuilder.append(this.darkTimeDelayBeta1);
                stringBuilder.append(",darkTimeDelayBeta2=");
                stringBuilder.append(this.darkTimeDelayBeta2);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() powerOnFastResponseLuxNum=");
                stringBuilder.append(this.powerOnFastResponseLuxNum);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() sceneMaxPoints=");
                stringBuilder.append(this.sceneMaxPoints);
                stringBuilder.append(",sceneGapPoints=");
                stringBuilder.append(this.sceneGapPoints);
                stringBuilder.append(",sceneMinPoints=");
                stringBuilder.append(this.sceneMinPoints);
                stringBuilder.append(",sceneAmbientLuxMaxWeight=");
                stringBuilder.append(this.sceneAmbientLuxMaxWeight);
                stringBuilder.append(",sceneAmbientLuxMinWeight=");
                stringBuilder.append(this.sceneAmbientLuxMinWeight);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() animationEqualRatioEnable=");
                stringBuilder.append(this.animationEqualRatioEnable);
                stringBuilder.append(",screenBrightnessMinNit=");
                stringBuilder.append(this.screenBrightnessMinNit);
                stringBuilder.append(",screenBrightnessMaxNit=");
                stringBuilder.append(this.screenBrightnessMaxNit);
                stringBuilder.append(",powerOnBrightenDebounceTime=");
                stringBuilder.append(this.powerOnBrightenDebounceTime);
                stringBuilder.append(",powerOnDarkenDebounceTime=");
                stringBuilder.append(this.powerOnDarkenDebounceTime);
                stringBuilder.append(",cameraModeEnable=");
                stringBuilder.append(this.cameraModeEnable);
                stringBuilder.append(",cameraAnimationTime=");
                stringBuilder.append(this.cameraAnimationTime);
                stringBuilder.append(",readingModeEnable=");
                stringBuilder.append(this.readingModeEnable);
                stringBuilder.append(",readingAnimationTime=");
                stringBuilder.append(this.readingAnimationTime);
                stringBuilder.append(",keyguardLuxThreshold=");
                stringBuilder.append(this.keyguardLuxThreshold);
                stringBuilder.append(",keyguardResponseBrightenTime=");
                stringBuilder.append(this.keyguardResponseBrightenTime);
                stringBuilder.append(",keyguardResponseDarkenTime=");
                stringBuilder.append(this.keyguardResponseDarkenTime);
                stringBuilder.append(",keyguardAnimationBrightenTime=");
                stringBuilder.append(this.keyguardAnimationBrightenTime);
                stringBuilder.append(",keyguardAnimationDarkenTime=");
                stringBuilder.append(this.keyguardAnimationDarkenTime);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() outdoorLowerLuxThreshold=");
                stringBuilder.append(this.outdoorLowerLuxThreshold);
                stringBuilder.append(", outdoorAnimationBrightenTime=");
                stringBuilder.append(this.outdoorAnimationBrightenTime);
                stringBuilder.append(", outdoorAnimationDarkenTime=");
                stringBuilder.append(this.outdoorAnimationDarkenTime);
                stringBuilder.append(", outdoorResponseBrightenRatio=");
                stringBuilder.append(this.outdoorResponseBrightenRatio);
                stringBuilder.append(", outdoorResponseDarkenRatio=");
                stringBuilder.append(this.outdoorResponseDarkenRatio);
                stringBuilder.append(", outdoorResponseBrightenTime=");
                stringBuilder.append(this.outdoorResponseBrightenTime);
                stringBuilder.append(", outdoorResponseDarkenTime=");
                stringBuilder.append(this.outdoorResponseDarkenTime);
                stringBuilder.append(", outdoorResponseCount=");
                stringBuilder.append(this.outdoorResponseCount);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() proximityLuxThreshold=");
                stringBuilder.append(this.proximityLuxThreshold);
                stringBuilder.append(", proximityResponseBrightenTime= ");
                stringBuilder.append(this.proximityResponseBrightenTime);
                stringBuilder.append(", initDoubleSensorInterfere =");
                stringBuilder.append(this.initDoubleSensorInterfere);
                stringBuilder.append(", initNumLastBuffer =");
                stringBuilder.append(this.initNumLastBuffer);
                stringBuilder.append(", initValidCloseTime =");
                stringBuilder.append(this.initValidCloseTime);
                stringBuilder.append(", initUpperLuxThreshold =");
                stringBuilder.append(this.initUpperLuxThreshold);
                stringBuilder.append(", initSigmoidFuncSlope  =");
                stringBuilder.append(this.initSigmoidFuncSlope);
                stringBuilder.append(", initSlowReponseUpperLuxThreshold  =");
                stringBuilder.append(this.initSlowReponseUpperLuxThreshold);
                stringBuilder.append(", initSlowReponseBrightTime  =");
                stringBuilder.append(this.initSlowReponseBrightTime);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() manualAnimationBrightenTime=");
                stringBuilder.append(this.manualAnimationBrightenTime);
                stringBuilder.append(", manualAnimationDarkenTime=");
                stringBuilder.append(this.manualAnimationDarkenTime);
                stringBuilder.append(", autoPowerSavingUseManualAnimationTimeEnable=");
                stringBuilder.append(this.autoPowerSavingUseManualAnimationTimeEnable);
                stringBuilder.append(", pgSceneDetectionDarkenDelayTime=");
                stringBuilder.append(this.pgSceneDetectionDarkenDelayTime);
                stringBuilder.append(", pgSceneDetectionBrightenDelayTime=");
                stringBuilder.append(this.pgSceneDetectionBrightenDelayTime);
                stringBuilder.append(", manualPowerSavingBrighnessLineEnable=");
                stringBuilder.append(this.manualPowerSavingBrighnessLineEnable);
                stringBuilder.append(", manualPowerSavingAnimationBrightenTime=");
                stringBuilder.append(this.manualPowerSavingAnimationBrightenTime);
                stringBuilder.append(", manualPowerSavingAnimationDarkenTime=");
                stringBuilder.append(this.manualPowerSavingAnimationDarkenTime);
                stringBuilder.append(", manualThermalModeAnimationBrightenTime=");
                stringBuilder.append(this.manualThermalModeAnimationBrightenTime);
                stringBuilder.append(", manualThermalModeAnimationDarkenTime=");
                stringBuilder.append(this.manualThermalModeAnimationDarkenTime);
                stringBuilder.append(", thermalModeBrightnessMappingEnable=");
                stringBuilder.append(this.thermalModeBrightnessMappingEnable);
                stringBuilder.append(", pgModeBrightnessMappingEnable=");
                stringBuilder.append(this.pgModeBrightnessMappingEnable);
                stringBuilder.append(", manualPowerSavingBrighnessLineDisableForDemo=");
                stringBuilder.append(this.manualPowerSavingBrighnessLineDisableForDemo);
                stringBuilder.append(", autoPowerSavingBrighnessLineDisableForDemo=");
                stringBuilder.append(this.autoPowerSavingBrighnessLineDisableForDemo);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() dayModeAlgoEnable=");
                stringBuilder.append(this.dayModeAlgoEnable);
                stringBuilder.append(", dayModeSwitchTime=");
                stringBuilder.append(this.dayModeSwitchTime);
                stringBuilder.append(", dayModeBeginTime=");
                stringBuilder.append(this.dayModeBeginTime);
                stringBuilder.append(", dayModeEndTime=");
                stringBuilder.append(this.dayModeEndTime);
                stringBuilder.append(", dayModeModifyNumPoint=");
                stringBuilder.append(this.dayModeModifyNumPoint);
                stringBuilder.append(", dayModeModifyMinBrightness=");
                stringBuilder.append(this.dayModeModifyMinBrightness);
                stringBuilder.append(", offsetResetSwitchTime =");
                stringBuilder.append(this.offsetResetSwitchTime);
                stringBuilder.append(", offsetResetEnable=");
                stringBuilder.append(this.offsetResetEnable);
                stringBuilder.append(", offsetResetShortSwitchTime=");
                stringBuilder.append(this.offsetResetShortSwitchTime);
                stringBuilder.append(", offsetResetShortLuxDelta=");
                stringBuilder.append(this.offsetResetShortLuxDelta);
                stringBuilder.append(", offsetBrightenDebounceTime=");
                stringBuilder.append(this.offsetBrightenDebounceTime);
                stringBuilder.append(", offsetDarkenDebounceTime=");
                stringBuilder.append(this.offsetDarkenDebounceTime);
                stringBuilder.append(", offsetValidAmbientLuxEnable=");
                stringBuilder.append(this.offsetValidAmbientLuxEnable);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() autoModeInOutDoorLimitEnble=");
                stringBuilder.append(this.autoModeInOutDoorLimitEnble);
                stringBuilder.append(", darkLightLevelMinThreshold=");
                stringBuilder.append(this.darkLightLevelMinThreshold);
                stringBuilder.append(", darkLightLevelMaxThreshold=");
                stringBuilder.append(this.darkLightLevelMaxThreshold);
                stringBuilder.append(", darkLightLevelRatio=");
                stringBuilder.append(this.darkLightLevelRatio);
                stringBuilder.append(", darkLightLuxMinThreshold=");
                stringBuilder.append(this.darkLightLuxMinThreshold);
                stringBuilder.append(", darkLightLuxMaxThreshold=");
                stringBuilder.append(this.darkLightLuxMaxThreshold);
                stringBuilder.append(", darkLightLuxDelta=");
                stringBuilder.append(this.darkLightLuxDelta);
                stringBuilder.append(", animatingForRGBWEnable=");
                stringBuilder.append(this.animatingForRGBWEnable);
                stringBuilder.append(", rebootFirstBrightnessAnimationEnable=");
                stringBuilder.append(this.rebootFirstBrightnessAnimationEnable);
                stringBuilder.append(", rebootFirstBrightness=");
                stringBuilder.append(this.rebootFirstBrightness);
                stringBuilder.append(", rebootFirstBrightnessAutoTime=");
                stringBuilder.append(this.rebootFirstBrightnessAutoTime);
                stringBuilder.append(", rebootFirstBrightnessManualTime=");
                stringBuilder.append(this.rebootFirstBrightnessManualTime);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() brightenlinePoints=");
                stringBuilder.append(this.brightenlinePoints);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() darkenlinePoints=");
                stringBuilder.append(this.darkenlinePoints);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() defaultBrightness=");
                stringBuilder.append(this.defaultBrightness);
                stringBuilder.append(", brightnessCalibrationEnabled=");
                stringBuilder.append(this.brightnessCalibrationEnabled);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() defaultBrighnessLinePoints=");
                stringBuilder.append(this.defaultBrighnessLinePoints);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() coverModeBrighnessLinePoints=");
                stringBuilder.append(this.coverModeBrighnessLinePoints);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() brightenGradualTime=");
                stringBuilder.append(this.brightenGradualTime);
                stringBuilder.append(", darkenGradualTime=");
                stringBuilder.append(this.darkenGradualTime);
                stringBuilder.append(", brightenThresholdFor255=");
                stringBuilder.append(this.brightenThresholdFor255);
                stringBuilder.append(", darkenTargetFor255=");
                stringBuilder.append(this.darkenTargetFor255);
                stringBuilder.append(", minAnimatingStep=");
                stringBuilder.append(this.minAnimatingStep);
                stringBuilder.append(", darkenCurrentFor255=");
                stringBuilder.append(this.darkenCurrentFor255);
                stringBuilder.append(", autoFastTimeFor255=");
                stringBuilder.append(this.autoFastTimeFor255);
                stringBuilder.append(", manualFastTimeFor255=");
                stringBuilder.append(this.manualFastTimeFor255);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() dimTime=");
                stringBuilder.append(this.dimTime);
                stringBuilder.append(", useVariableStep=");
                stringBuilder.append(this.useVariableStep);
                stringBuilder.append(", darkenGradualTimeMax=");
                stringBuilder.append(this.darkenGradualTimeMax);
                stringBuilder.append(", darkenGradualTimeMin=");
                stringBuilder.append(this.darkenGradualTimeMin);
                stringBuilder.append(", animatedStepRoundEnabled=");
                stringBuilder.append(this.animatedStepRoundEnabled);
                stringBuilder.append(", reportValueWhenSensorOnChange=");
                stringBuilder.append(this.reportValueWhenSensorOnChange);
                stringBuilder.append(", allowLabcUseProximity=");
                stringBuilder.append(this.allowLabcUseProximity);
                stringBuilder.append(", proximityPositiveDebounceTime=");
                stringBuilder.append(this.proximityPositiveDebounceTime);
                stringBuilder.append(", proximityNegativeDebounceTime=");
                stringBuilder.append(this.proximityNegativeDebounceTime);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() manualMode=");
                stringBuilder.append(this.manualMode);
                stringBuilder.append(", manualBrightnessMaxLimit=");
                stringBuilder.append(this.manualBrightnessMaxLimit);
                stringBuilder.append(", manualBrightnessMinLimit=");
                stringBuilder.append(this.manualBrightnessMinLimit);
                stringBuilder.append(", outDoorThreshold=");
                stringBuilder.append(this.outDoorThreshold);
                stringBuilder.append(", inDoorThreshold=");
                stringBuilder.append(this.inDoorThreshold);
                stringBuilder.append(", manualBrighenDebounceTime=");
                stringBuilder.append(this.manualBrighenDebounceTime);
                stringBuilder.append(", manualDarkenDebounceTime=");
                stringBuilder.append(this.manualDarkenDebounceTime);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() manualBrightenlinePoints=");
                stringBuilder.append(this.manualBrightenlinePoints);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() manualDarkenlinePoints=");
                stringBuilder.append(this.manualDarkenlinePoints);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() brightnessMappingPoints=");
                stringBuilder.append(this.brightnessMappingPoints);
                Slog.i(str, stringBuilder.toString());
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() ambientLuxValidBrightnessPoints=");
                stringBuilder.append(this.ambientLuxValidBrightnessPoints);
                Slog.i(str, stringBuilder.toString());
                if (this.darkAdapterEnable) {
                    str = HwBrightnessXmlLoader.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("printData() unadapt2AdaptingShortFilterLux=");
                    stringBuilder.append(this.unadapt2AdaptingShortFilterLux);
                    stringBuilder.append(", unadapt2AdaptingLongFilterLux=");
                    stringBuilder.append(this.unadapt2AdaptingLongFilterLux);
                    stringBuilder.append(", unadapt2AdaptingLongFilterSec=");
                    stringBuilder.append(this.unadapt2AdaptingLongFilterSec);
                    stringBuilder.append(", unadapt2AdaptingDimSec=");
                    stringBuilder.append(this.unadapt2AdaptingDimSec);
                    stringBuilder.append(", adapting2UnadaptShortFilterLux=");
                    stringBuilder.append(this.adapting2UnadaptShortFilterLux);
                    stringBuilder.append(", adapting2AdaptedOffDurationMinSec=");
                    stringBuilder.append(this.adapting2AdaptedOffDurationMinSec);
                    stringBuilder.append(", adapting2AdaptedOffDurationFilterSec=");
                    stringBuilder.append(this.adapting2AdaptedOffDurationFilterSec);
                    stringBuilder.append(", adapting2AdaptedOffDurationMaxSec=");
                    stringBuilder.append(this.adapting2AdaptedOffDurationMaxSec);
                    stringBuilder.append(", adapting2AdaptedOnClockNoFilterBeginHour=");
                    stringBuilder.append(this.adapting2AdaptedOnClockNoFilterBeginHour);
                    stringBuilder.append(", adapting2AdaptedOnClockNoFilterEndHour=");
                    stringBuilder.append(this.adapting2AdaptedOnClockNoFilterEndHour);
                    stringBuilder.append(", unadapt2AdaptedShortFilterLux=");
                    stringBuilder.append(this.unadapt2AdaptedShortFilterLux);
                    stringBuilder.append(", unadapt2AdaptedOffDurationMinSec=");
                    stringBuilder.append(this.unadapt2AdaptedOffDurationMinSec);
                    stringBuilder.append(", unadapt2AdaptedOnClockNoFilterBeginHour=");
                    stringBuilder.append(this.unadapt2AdaptedOnClockNoFilterBeginHour);
                    stringBuilder.append(", unadapt2AdaptedOnClockNoFilterEndHour=");
                    stringBuilder.append(this.unadapt2AdaptedOnClockNoFilterEndHour);
                    stringBuilder.append(", adapted2UnadaptShortFilterLux=");
                    stringBuilder.append(this.adapted2UnadaptShortFilterLux);
                    Slog.i(str, stringBuilder.toString());
                }
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() pgReregisterScene=");
                stringBuilder.append(this.pgReregisterScene);
                Slog.i(str, stringBuilder.toString());
                if (this.touchProximityEnable) {
                    str = HwBrightnessXmlLoader.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("printData() touchProximityYNearbyRatio=");
                    stringBuilder.append(this.touchProximityYNearbyRatio);
                    Slog.i(str, stringBuilder.toString());
                }
                if (this.vehicleModeEnable) {
                    str = HwBrightnessXmlLoader.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("printData() vehicleModeEnable=");
                    stringBuilder.append(this.vehicleModeEnable);
                    stringBuilder.append(",vehicleModeDisableTimeMillis=");
                    stringBuilder.append(this.vehicleModeDisableTimeMillis);
                    stringBuilder.append(",vehicleModeQuitTimeForPowerOn=");
                    stringBuilder.append(this.vehicleModeQuitTimeForPowerOn);
                    stringBuilder.append(",vehicleModeEnterTimeForPowerOn=");
                    stringBuilder.append(this.vehicleModeEnterTimeForPowerOn);
                    Slog.i(str, stringBuilder.toString());
                }
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() gameModeEnable=");
                stringBuilder.append(this.gameModeEnable);
                stringBuilder.append(",gameModeBrightenAnimationTime=");
                stringBuilder.append(this.gameModeBrightenAnimationTime);
                stringBuilder.append(",gameModeDarkentenAnimationTime=");
                stringBuilder.append(this.gameModeDarkentenAnimationTime);
                stringBuilder.append(",gameModeDarkentenLongAnimationTime=");
                stringBuilder.append(this.gameModeDarkentenLongAnimationTime);
                stringBuilder.append(",gameModeDarkentenLongTarget=");
                stringBuilder.append(this.gameModeDarkentenLongTarget);
                stringBuilder.append(",gameModeDarkentenLongCurrent=");
                stringBuilder.append(this.gameModeDarkentenLongCurrent);
                stringBuilder.append(",gameModeClearOffsetTime=");
                stringBuilder.append(this.gameModeClearOffsetTime);
                stringBuilder.append(",gameModeBrightenDebounceTime=");
                stringBuilder.append(this.gameModeBrightenDebounceTime);
                stringBuilder.append(",gameModeDarkenDebounceTime=");
                stringBuilder.append(this.gameModeDarkenDebounceTime);
                Slog.i(str, stringBuilder.toString());
                if (this.gameModeOffsetValidAmbientLuxEnable) {
                    str = HwBrightnessXmlLoader.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("printData() gameModeAmbientLuxValidBrightnessPoints=");
                    stringBuilder.append(this.gameModeAmbientLuxValidBrightnessPoints);
                    Slog.i(str, stringBuilder.toString());
                }
                if (this.backSensorCoverModeEnable) {
                    str = HwBrightnessXmlLoader.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("printData() backSensorCoverModeBrighnessLinePoints=");
                    stringBuilder.append(this.backSensorCoverModeBrighnessLinePoints);
                    Slog.i(str, stringBuilder.toString());
                    str = HwBrightnessXmlLoader.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("printData() backSensorCoverModeMinLuxInRing=");
                    stringBuilder.append(this.backSensorCoverModeMinLuxInRing);
                    Slog.i(str, stringBuilder.toString());
                }
                str = HwBrightnessXmlLoader.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("printData() cryogenicEnable=");
                stringBuilder.append(this.cryogenicEnable);
                stringBuilder.append(", cryogenicModeBrightnessMappingEnable=");
                stringBuilder.append(this.cryogenicModeBrightnessMappingEnable);
                stringBuilder.append(", cryogenicMaxBrightnessTimeOut=");
                stringBuilder.append(this.cryogenicMaxBrightnessTimeOut);
                stringBuilder.append(", cryogenicActiveScreenOffIntervalInMillis=");
                stringBuilder.append(this.cryogenicActiveScreenOffIntervalInMillis);
                stringBuilder.append(", cryogenicLagTimeInMillis=");
                stringBuilder.append(this.cryogenicLagTimeInMillis);
                Slog.i(str, stringBuilder.toString());
            }
        }

        public void loadDefaultConfig() {
            if (HwBrightnessXmlLoader.HWFLOW) {
                Slog.i(HwBrightnessXmlLoader.TAG, "loadDefaultConfig()");
            }
            this.lightSensorRateMills = 300;
            this.brighenDebounceTime = 1000;
            this.darkenDebounceTime = HwAPPQoEUtils.APP_TYPE_STREAMING;
            this.brightenDebounceTimeParaBig = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.darkenDebounceTimeParaBig = 1.0f;
            this.brightenDeltaLuxPara = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.darkenDeltaLuxPara = 1.0f;
            this.stabilityConstant = 5;
            this.stabilityTime1 = 20;
            this.stabilityTime2 = 10;
            this.brighenDebounceTimeForSmallThr = HwAPPQoEUtils.APP_TYPE_STREAMING;
            this.darkenDebounceTimeForSmallThr = 8000;
            this.ratioForBrightnenSmallThr = 1.0f;
            this.ratioForDarkenSmallThr = 1.0f;
            this.rebootAutoModeEnable = false;
            this.darkTimeDelayEnable = false;
            this.darkTimeDelay = 10000;
            this.darkTimeDelayLuxThreshold = 50.0f;
            this.coverModeFirstLux = 2210.0f;
            this.lastCloseScreenEnable = false;
            this.coverModeBrightenResponseTime = 1000;
            this.coverModeDarkenResponseTime = 1000;
            this.coverModelastCloseScreenEnable = false;
            this.coverModeDayEnable = false;
            this.coverModeDayBrightness = CPUFeature.MSG_RESET_ON_FIRE;
            this.converModeDayBeginTime = 6;
            this.coverModeDayEndTime = 18;
            this.postMaxMinAvgFilterNoFilterNum = 6;
            this.postMaxMinAvgFilterNum = 5;
            this.brightTimeDelayEnable = false;
            this.brightTimeDelay = 1000;
            this.brightTimeDelayLuxThreshold = 30.0f;
            this.preMethodNum = 0;
            this.preMeanFilterNoFilterNum = 7;
            this.preMeanFilterNum = 3;
            this.postMethodNum = 2;
            this.postMeanFilterNoFilterNum = 4;
            this.postMeanFilterNum = 3;
            this.preWeightedMeanFilterNoFilterNum = 7;
            this.preWeightedMeanFilterNum = 3;
            this.preWeightedMeanFilterMaxFuncLuxNum = 3;
            this.preWeightedMeanFilterAlpha = 0.5f;
            this.preWeightedMeanFilterLuxTh = 12.0f;
            this.darkTimeDelayBeta0 = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.darkTimeDelayBeta1 = 1.0f;
            this.darkTimeDelayBeta2 = 0.333f;
            this.powerOnFastResponseLuxNum = 8;
            this.sceneMaxPoints = 0;
            this.sceneGapPoints = 29;
            this.sceneMinPoints = 29;
            this.sceneAmbientLuxMaxWeight = 0.5f;
            this.sceneAmbientLuxMinWeight = 0.5f;
            this.animationEqualRatioEnable = false;
            this.screenBrightnessMinNit = 2.0f;
            this.screenBrightnessMaxNit = 530.0f;
            this.powerOnBrightenDebounceTime = 500;
            this.powerOnDarkenDebounceTime = 1000;
            this.cameraModeEnable = false;
            this.cameraAnimationTime = 3.0f;
            this.readingModeEnable = false;
            this.readingAnimationTime = 3.0f;
            this.keyguardResponseBrightenTime = 500;
            this.keyguardResponseDarkenTime = -1;
            this.keyguardAnimationBrightenTime = 0.5f;
            this.keyguardAnimationDarkenTime = -1.0f;
            this.keyguardLuxThreshold = 20.0f;
            this.outdoorLowerLuxThreshold = 1000;
            this.outdoorAnimationBrightenTime = 1.5f;
            this.outdoorAnimationDarkenTime = -1.0f;
            this.outdoorResponseBrightenRatio = -1.0f;
            this.outdoorResponseDarkenRatio = -1.0f;
            this.outdoorResponseBrightenTime = -1;
            this.outdoorResponseDarkenTime = -1;
            this.outdoorResponseCount = 5;
            this.proximityLuxThreshold = 20.0f;
            this.proximityResponseBrightenTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
            this.initDoubleSensorInterfere = 8.0f;
            this.initNumLastBuffer = 10;
            this.initValidCloseTime = -1;
            this.initUpperLuxThreshold = 20;
            this.initSigmoidFuncSlope = 0.75f;
            this.initSlowReponseUpperLuxThreshold = 20;
            this.initSlowReponseBrightTime = 0;
            this.manualAnimationBrightenTime = 0.5f;
            this.manualAnimationDarkenTime = 0.5f;
            this.autoPowerSavingUseManualAnimationTimeEnable = false;
            this.pgSceneDetectionDarkenDelayTime = 2500;
            this.pgSceneDetectionBrightenDelayTime = 500;
            this.manualPowerSavingBrighnessLineEnable = false;
            this.manualPowerSavingAnimationBrightenTime = 0.5f;
            this.manualPowerSavingAnimationDarkenTime = 0.5f;
            this.manualThermalModeAnimationBrightenTime = 0.5f;
            this.manualThermalModeAnimationDarkenTime = 0.5f;
            this.thermalModeBrightnessMappingEnable = false;
            this.pgModeBrightnessMappingEnable = false;
            this.manualPowerSavingBrighnessLineDisableForDemo = false;
            this.autoPowerSavingBrighnessLineDisableForDemo = false;
            this.dayModeAlgoEnable = false;
            this.dayModeSwitchTime = 30;
            this.dayModeBeginTime = 5;
            this.dayModeEndTime = 23;
            this.dayModeModifyNumPoint = 3;
            this.dayModeModifyMinBrightness = 6;
            this.offsetResetSwitchTime = 10;
            this.offsetResetEnable = false;
            this.offsetResetShortSwitchTime = 10;
            this.offsetResetShortLuxDelta = 50000;
            this.offsetBrightenDebounceTime = 1000;
            this.offsetDarkenDebounceTime = 1000;
            this.offsetValidAmbientLuxEnable = false;
            this.autoModeInOutDoorLimitEnble = false;
            this.darkLightLevelMinThreshold = 0;
            this.darkLightLevelMaxThreshold = 0;
            this.darkLightLevelRatio = 1.0f;
            this.darkLightLuxMinThreshold = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.darkLightLuxMaxThreshold = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.darkLightLuxDelta = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.animatingForRGBWEnable = false;
            this.rebootFirstBrightnessAnimationEnable = false;
            this.rebootFirstBrightness = 10000;
            this.rebootFirstBrightnessAutoTime = 3.0f;
            this.rebootFirstBrightnessManualTime = 3.0f;
            this.monitorEnable = false;
            this.brightenlinePoints.clear();
            this.brightenlinePoints.add(new PointF(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 15.0f));
            this.brightenlinePoints.add(new PointF(2.0f, 15.0f));
            this.brightenlinePoints.add(new PointF(10.0f, 19.0f));
            this.brightenlinePoints.add(new PointF(20.0f, 219.0f));
            this.brightenlinePoints.add(new PointF(100.0f, 539.0f));
            this.brightenlinePoints.add(new PointF(1000.0f, 989.0f));
            this.brightenlinePoints.add(new PointF(40000.0f, 989.0f));
            this.darkenlinePoints.clear();
            this.darkenlinePoints.add(new PointF(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 1.0f));
            this.darkenlinePoints.add(new PointF(1.0f, 1.0f));
            this.darkenlinePoints.add(new PointF(20.0f, 20.0f));
            this.darkenlinePoints.add(new PointF(40.0f, 20.0f));
            this.darkenlinePoints.add(new PointF(100.0f, 80.0f));
            this.darkenlinePoints.add(new PointF(600.0f, 580.0f));
            this.darkenlinePoints.add(new PointF(1180.0f, 580.0f));
            this.darkenlinePoints.add(new PointF(1200.0f, 600.0f));
            this.darkenlinePoints.add(new PointF(1800.0f, 600.0f));
            this.darkenlinePoints.add(new PointF(40000.0f, 38800.0f));
            this.defaultBrightness = 100.0f;
            this.brightnessCalibrationEnabled = false;
            this.defaultBrighnessLinePoints.clear();
            this.defaultBrighnessLinePoints.add(new PointF(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 4.0f));
            this.defaultBrighnessLinePoints.add(new PointF(25.0f, 46.5f));
            this.defaultBrighnessLinePoints.add(new PointF(1995.0f, 140.7f));
            this.defaultBrighnessLinePoints.add(new PointF(4000.0f, 255.0f));
            this.defaultBrighnessLinePoints.add(new PointF(40000.0f, 255.0f));
            this.coverModeBrighnessLinePoints.clear();
            this.coverModeBrighnessLinePoints.add(new PointF(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 20.0f));
            this.coverModeBrighnessLinePoints.add(new PointF(25.0f, 46.5f));
            this.coverModeBrighnessLinePoints.add(new PointF(250.0f, 100.0f));
            this.coverModeBrighnessLinePoints.add(new PointF(1995.0f, 154.7f));
            this.coverModeBrighnessLinePoints.add(new PointF(4000.0f, 255.0f));
            this.coverModeBrighnessLinePoints.add(new PointF(40000.0f, 255.0f));
            this.brightenGradualTime = 1.0f;
            this.darkenGradualTime = 3.0f;
            this.brightenThresholdFor255 = 1254;
            this.darkenTargetFor255 = 1254;
            this.minAnimatingStep = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.darkenCurrentFor255 = DeviceStatusConstant.TYPE_HEAD_DOWN;
            this.autoFastTimeFor255 = 0.5f;
            this.manualFastTimeFor255 = 0.5f;
            this.dimTime = 3.0f;
            this.useVariableStep = false;
            this.darkenGradualTimeMax = 3.0f;
            this.darkenGradualTimeMin = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.animatedStepRoundEnabled = false;
            this.reportValueWhenSensorOnChange = true;
            this.allowLabcUseProximity = false;
            this.proximityPositiveDebounceTime = 150;
            this.proximityNegativeDebounceTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
            this.manualMode = false;
            this.manualBrightnessMaxLimit = 255;
            this.manualBrightnessMinLimit = 4;
            this.outDoorThreshold = 8000;
            this.inDoorThreshold = HwAPPQoEUtils.APP_TYPE_STREAMING;
            this.manualBrighenDebounceTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
            this.manualDarkenDebounceTime = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
            this.manualBrightenlinePoints.clear();
            this.manualBrightenlinePoints.add(new PointF(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 1000.0f));
            this.manualBrightenlinePoints.add(new PointF(1000.0f, 5000.0f));
            this.manualBrightenlinePoints.add(new PointF(40000.0f, 10000.0f));
            this.manualDarkenlinePoints.clear();
            this.manualDarkenlinePoints.add(new PointF(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 1.0f));
            this.manualDarkenlinePoints.add(new PointF(500.0f, 10.0f));
            this.manualDarkenlinePoints.add(new PointF(1000.0f, 500.0f));
            this.manualDarkenlinePoints.add(new PointF(2000.0f, 1000.0f));
            this.manualDarkenlinePoints.add(new PointF(40000.0f, 30000.0f));
            this.brightnessMappingPoints.clear();
            this.brightnessMappingPoints.add(new PointF(4.0f, 4.0f));
            this.brightnessMappingPoints.add(new PointF(25.0f, 25.0f));
            this.brightnessMappingPoints.add(new PointF(245.0f, 245.0f));
            this.brightnessMappingPoints.add(new PointF(255.0f, 255.0f));
            this.ambientLuxValidBrightnessPoints.clear();
            this.ambientLuxValidBrightnessPoints.add(new HwXmlAmPoint(GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, 4.0f, 55.0f));
            this.ambientLuxValidBrightnessPoints.add(new HwXmlAmPoint(100.0f, 4.0f, 255.0f));
            this.ambientLuxValidBrightnessPoints.add(new HwXmlAmPoint(1000.0f, 4.0f, 255.0f));
            this.ambientLuxValidBrightnessPoints.add(new HwXmlAmPoint(5000.0f, 50.0f, 255.0f));
            this.ambientLuxValidBrightnessPoints.add(new HwXmlAmPoint(40000.0f, 50.0f, 255.0f));
            this.darkAdapterEnable = false;
            this.pgReregisterScene = false;
            this.touchProximityEnable = false;
            this.touchProximityYNearbyRatio = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
            this.vehicleModeEnable = false;
            this.vehicleModeDisableTimeMillis = MemoryConstant.MIN_INTERVAL_OP_TIMEOUT;
            this.vehicleModeQuitTimeForPowerOn = 200;
            this.vehicleModeEnterTimeForPowerOn = 200;
            this.gameModeEnable = false;
            this.gameModeBrightenAnimationTime = 0.5f;
            this.gameModeDarkentenAnimationTime = 0.5f;
            this.gameModeDarkentenLongAnimationTime = 0.5f;
            this.gameModeDarkentenLongTarget = 0;
            this.gameModeDarkentenLongCurrent = 0;
            this.gameModeClearOffsetTime = MemoryConstant.MIN_INTERVAL_OP_TIMEOUT;
            this.gameModeBrightenDebounceTime = 1000;
            this.gameModeDarkenDebounceTime = MemoryConstant.MIN_INTERVAL_OP_TIMEOUT;
            this.gameModeOffsetValidAmbientLuxEnable = false;
            this.gameModeAmbientLuxValidBrightnessPoints.clear();
            this.backSensorCoverModeEnable = false;
            this.backSensorCoverModeBrighnessLinePoints.clear();
            this.backSensorCoverModeMinLuxInRing = 0;
            this.cryogenicEnable = false;
            this.cryogenicModeBrightnessMappingEnable = false;
            this.cryogenicMaxBrightnessTimeOut = 5000;
            this.cryogenicActiveScreenOffIntervalInMillis = HwNetworkStatsService.UPLOAD_INTERVAL;
            this.cryogenicLagTimeInMillis = HwNetworkStatsService.UPLOAD_INTERVAL;
        }
    }

    private static class Element_AmbientLuxValidBrightnessPoints extends HwXmlElement {
        private Element_AmbientLuxValidBrightnessPoints() {
        }

        public String getName() {
            return "AmbientLuxValidBrightnessPoints";
        }

        protected boolean isOptional() {
            return true;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_AmbientLuxValidBrightnessPoints_Point extends HwXmlElement {
        private Element_AmbientLuxValidBrightnessPoints_Point() {
        }

        public String getName() {
            return "Point";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (!this.mIsParsed) {
                HwBrightnessXmlLoader.mData.ambientLuxValidBrightnessPoints.clear();
            }
            HwBrightnessXmlLoader.mData.ambientLuxValidBrightnessPoints = HwXmlElement.parseAmPointList(parser, HwBrightnessXmlLoader.mData.ambientLuxValidBrightnessPoints);
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.checkAmPointsListIsOK(HwBrightnessXmlLoader.mData.ambientLuxValidBrightnessPoints);
        }
    }

    private static class Element_AnimateGroup extends HwXmlElement {
        private Element_AnimateGroup() {
        }

        public String getName() {
            return "AnimateGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"BrightenGradualTime", "DarkenGradualTime", "BrightenThresholdFor255", "DarkenTargetFor255", "DarkenCurrentFor255"});
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1498609295:
                    if (valueName.equals("BrightenThresholdFor255")) {
                        z = true;
                        break;
                    }
                case 884036153:
                    if (valueName.equals("DarkenTargetFor255")) {
                        z = true;
                        break;
                    }
                case 1455932156:
                    if (valueName.equals("BrightenGradualTime")) {
                        z = false;
                        break;
                    }
                case 1739695104:
                    if (valueName.equals("DarkenGradualTime")) {
                        z = true;
                        break;
                    }
                case 1877080899:
                    if (valueName.equals("DarkenCurrentFor255")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.brightenGradualTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkenGradualTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.brightenThresholdFor255 = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkenTargetFor255 = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkenCurrentFor255 = HwXmlElement.string2Int(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.brightenGradualTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.darkenGradualTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
    }

    private static class Element_AnimateOptionalGroup extends HwXmlElement {
        private Element_AnimateOptionalGroup() {
        }

        public String getName() {
            return "AnimateOptionalGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"AutoFastTimeFor255", "ManualFastTimeFor255", "DimTime", "AnimationEqualRatioEnable", "ScreenBrightnessMinNit", "ScreenBrightnessMaxNit", "MinAnimatingStep"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1857716415:
                    if (valueName.equals("AutoFastTimeFor255")) {
                        z = false;
                        break;
                    }
                case -964927659:
                    if (valueName.equals("DimTime")) {
                        z = true;
                        break;
                    }
                case -935718286:
                    if (valueName.equals("ScreenBrightnessMaxNit")) {
                        z = true;
                        break;
                    }
                case -928628028:
                    if (valueName.equals("ScreenBrightnessMinNit")) {
                        z = true;
                        break;
                    }
                case -322892520:
                    if (valueName.equals("ManualFastTimeFor255")) {
                        z = true;
                        break;
                    }
                case 1325505822:
                    if (valueName.equals("AnimationEqualRatioEnable")) {
                        z = true;
                        break;
                    }
                case 1740888632:
                    if (valueName.equals("MinAnimatingStep")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.autoFastTimeFor255 = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.manualFastTimeFor255 = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.dimTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.animationEqualRatioEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.screenBrightnessMinNit = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.screenBrightnessMaxNit = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.minAnimatingStep = HwXmlElement.string2Float(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.screenBrightnessMinNit > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.screenBrightnessMaxNit > HwBrightnessXmlLoader.mData.screenBrightnessMinNit;
        }
    }

    private static class Element_BackSensorCoverModeBrighnessLinePoints extends HwXmlElement {
        private Element_BackSensorCoverModeBrighnessLinePoints() {
        }

        public String getName() {
            return "BackSensorCoverModeBrighnessLinePoints";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            HwBrightnessXmlLoader.mData.backSensorCoverModeEnable = true;
            return true;
        }

        protected boolean isOptional() {
            return true;
        }
    }

    private static class Element_BackSensorCoverModeBrighnessLinePoints_Point extends HwXmlElement {
        private Element_BackSensorCoverModeBrighnessLinePoints_Point() {
        }

        public String getName() {
            return "Point";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (!this.mIsParsed) {
                HwBrightnessXmlLoader.mData.backSensorCoverModeBrighnessLinePoints.clear();
            }
            HwBrightnessXmlLoader.mData.backSensorCoverModeBrighnessLinePoints = HwXmlElement.parsePointFList(parser, HwBrightnessXmlLoader.mData.backSensorCoverModeBrighnessLinePoints);
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.checkPointsListIsOK(HwBrightnessXmlLoader.mData.backSensorCoverModeBrighnessLinePoints);
        }
    }

    private static class Element_BrightenlinePoints extends HwXmlElement {
        private Element_BrightenlinePoints() {
        }

        public String getName() {
            return "BrightenlinePoints";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_BrightenlinePoints_Point extends HwXmlElement {
        private Element_BrightenlinePoints_Point() {
        }

        public String getName() {
            return "Point";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (!this.mIsParsed) {
                HwBrightnessXmlLoader.mData.brightenlinePoints.clear();
            }
            HwBrightnessXmlLoader.mData.brightenlinePoints = HwXmlElement.parsePointFList(parser, HwBrightnessXmlLoader.mData.brightenlinePoints);
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.checkPointsListIsOK(HwBrightnessXmlLoader.mData.brightenlinePoints);
        }
    }

    private static class Element_BrightnessMappingPoints extends HwXmlElement {
        private Element_BrightnessMappingPoints() {
        }

        public String getName() {
            return "BrightnessMappingPoints";
        }

        protected boolean isOptional() {
            return true;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_BrightnessMappingPoints_Point extends HwXmlElement {
        private Element_BrightnessMappingPoints_Point() {
        }

        public String getName() {
            return "Point";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (!this.mIsParsed) {
                HwBrightnessXmlLoader.mData.brightnessMappingPoints.clear();
            }
            HwBrightnessXmlLoader.mData.brightnessMappingPoints = HwXmlElement.parsePointFList(parser, HwBrightnessXmlLoader.mData.brightnessMappingPoints);
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.checkPointsListIsOK(HwBrightnessXmlLoader.mData.brightnessMappingPoints);
        }
    }

    private static class Element_CoverModeBrighnessLinePoints extends HwXmlElement {
        private Element_CoverModeBrighnessLinePoints() {
        }

        public String getName() {
            return "CoverModeBrighnessLinePoints";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }

        protected boolean isOptional() {
            return true;
        }
    }

    private static class Element_CoverModeBrighnessLinePoints_Point extends HwXmlElement {
        private Element_CoverModeBrighnessLinePoints_Point() {
        }

        public String getName() {
            return "Point";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (!this.mIsParsed) {
                HwBrightnessXmlLoader.mData.coverModeBrighnessLinePoints.clear();
            }
            HwBrightnessXmlLoader.mData.coverModeBrighnessLinePoints = HwXmlElement.parsePointFList(parser, HwBrightnessXmlLoader.mData.coverModeBrighnessLinePoints);
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.checkPointsListIsOK(HwBrightnessXmlLoader.mData.coverModeBrighnessLinePoints);
        }
    }

    private static class Element_CoverModeGroup extends HwXmlElement {
        private Element_CoverModeGroup() {
        }

        public String getName() {
            return "CoverModeGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"CoverModeFirstLux", "LastCloseScreenEnable", "CoverModeBrightenResponseTime", "CoverModeDarkenResponseTime", "CoverModelastCloseScreenEnable", "CoverModeDayEnable", "CoverModeDayBrightness", "ConverModeDayBeginTime", "CoverModeDayEndTime", "BackSensorCoverModeMinLuxInRing"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -2135500393:
                    if (valueName.equals("CoverModelastCloseScreenEnable")) {
                        z = true;
                        break;
                    }
                case -2016036730:
                    if (valueName.equals("CoverModeDayEndTime")) {
                        z = true;
                        break;
                    }
                case -1487844013:
                    if (valueName.equals("CoverModeDayBrightness")) {
                        z = true;
                        break;
                    }
                case -1073894082:
                    if (valueName.equals("ConverModeDayBeginTime")) {
                        z = true;
                        break;
                    }
                case -163823189:
                    if (valueName.equals("BackSensorCoverModeMinLuxInRing")) {
                        z = true;
                        break;
                    }
                case 1181816709:
                    if (valueName.equals("CoverModeDayEnable")) {
                        z = true;
                        break;
                    }
                case 1364232977:
                    if (valueName.equals("LastCloseScreenEnable")) {
                        z = true;
                        break;
                    }
                case 1460433721:
                    if (valueName.equals("CoverModeFirstLux")) {
                        z = false;
                        break;
                    }
                case 1871618155:
                    if (valueName.equals("CoverModeBrightenResponseTime")) {
                        z = true;
                        break;
                    }
                case 2140032615:
                    if (valueName.equals("CoverModeDarkenResponseTime")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.coverModeFirstLux = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.lastCloseScreenEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.coverModeBrightenResponseTime = HwXmlElement.string2Long(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.coverModeDarkenResponseTime = HwXmlElement.string2Long(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.coverModelastCloseScreenEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.coverModeDayEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.coverModeDayBrightness = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.converModeDayBeginTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.coverModeDayEndTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.backSensorCoverModeMinLuxInRing = HwXmlElement.string2Int(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.coverModeBrightenResponseTime >= 0 && HwBrightnessXmlLoader.mData.coverModeDarkenResponseTime >= 0 && HwBrightnessXmlLoader.mData.coverModeDayBrightness > 0 && HwBrightnessXmlLoader.mData.converModeDayBeginTime >= 0 && HwBrightnessXmlLoader.mData.converModeDayBeginTime < 24 && HwBrightnessXmlLoader.mData.coverModeDayEndTime >= 0 && HwBrightnessXmlLoader.mData.coverModeDayEndTime < 24 && HwBrightnessXmlLoader.mData.backSensorCoverModeMinLuxInRing >= 0;
        }
    }

    private static class Element_CryogenicGroup extends HwXmlElement {
        private Element_CryogenicGroup() {
        }

        public String getName() {
            return "CryogenicGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"CryogenicEnable", "CryogenicModeBrightnessMappingEnable", "CryogenicMaxBrightnessTimeOut", "CryogenicActiveScreenOffIntervalInMillis", "CryogenicLagTimeInMillis"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -1243020920) {
                if (hashCode != 971881064) {
                    if (hashCode != 1229222385) {
                        if (hashCode == 1592001029 && valueName.equals("CryogenicLagTimeInMillis")) {
                            z = true;
                            switch (z) {
                                case false:
                                    HwBrightnessXmlLoader.mData.cryogenicEnable = HwXmlElement.string2Boolean(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.cryogenicMaxBrightnessTimeOut = HwXmlElement.string2Long(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.cryogenicActiveScreenOffIntervalInMillis = HwXmlElement.string2Long(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.cryogenicLagTimeInMillis = HwXmlElement.string2Long(parser.nextText());
                                    break;
                                default:
                                    String str = this.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("unknow valueName=");
                                    stringBuilder.append(valueName);
                                    Slog.e(str, stringBuilder.toString());
                                    return false;
                            }
                            return true;
                        }
                    } else if (valueName.equals("CryogenicMaxBrightnessTimeOut")) {
                        z = true;
                        switch (z) {
                            case false:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                } else if (valueName.equals("CryogenicActiveScreenOffIntervalInMillis")) {
                    z = true;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                    return true;
                }
            } else if (valueName.equals("CryogenicEnable")) {
                z = false;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.cryogenicMaxBrightnessTimeOut > 0 && HwBrightnessXmlLoader.mData.cryogenicActiveScreenOffIntervalInMillis > 0 && HwBrightnessXmlLoader.mData.cryogenicLagTimeInMillis > 0;
        }
    }

    private static class Element_DarkAdapter extends HwXmlElement {
        private Element_DarkAdapter() {
        }

        public String getName() {
            return "DarkAdapter";
        }

        protected boolean isOptional() {
            return true;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            HwBrightnessXmlLoader.mData.darkAdapterEnable = true;
            return true;
        }
    }

    private static class Element_DarkAdapterGroup1 extends HwXmlElement {
        private Element_DarkAdapterGroup1() {
        }

        public String getName() {
            return "DarkAdapterGroup1";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"Unadapt2AdaptingShortFilterLux", "Unadapt2AdaptingLongFilterLux", "Unadapt2AdaptingLongFilterSec", "Unadapt2AdaptingDimSec", "Adapting2UnadaptShortFilterLux", "Adapting2AdaptedOffDurationMinSec", "Adapting2AdaptedOffDurationFilterSec", "Adapting2AdaptedOffDurationMaxSec", "Adapting2AdaptedOnClockNoFilterBeginHour", "Adapting2AdaptedOnClockNoFilterEndHour"});
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1713045852:
                    if (valueName.equals("Unadapt2AdaptingShortFilterLux")) {
                        z = false;
                        break;
                    }
                case -1656034879:
                    if (valueName.equals("Adapting2AdaptedOffDurationMaxSec")) {
                        z = true;
                        break;
                    }
                case -1648944621:
                    if (valueName.equals("Adapting2AdaptedOffDurationMinSec")) {
                        z = true;
                        break;
                    }
                case -1292961038:
                    if (valueName.equals("Unadapt2AdaptingLongFilterLux")) {
                        z = true;
                        break;
                    }
                case -1292954828:
                    if (valueName.equals("Unadapt2AdaptingLongFilterSec")) {
                        z = true;
                        break;
                    }
                case 557886292:
                    if (valueName.equals("Adapting2AdaptedOnClockNoFilterBeginHour")) {
                        z = true;
                        break;
                    }
                case 758345798:
                    if (valueName.equals("Adapting2AdaptedOnClockNoFilterEndHour")) {
                        z = true;
                        break;
                    }
                case 824735218:
                    if (valueName.equals("Unadapt2AdaptingDimSec")) {
                        z = true;
                        break;
                    }
                case 896190834:
                    if (valueName.equals("Adapting2UnadaptShortFilterLux")) {
                        z = true;
                        break;
                    }
                case 1064291269:
                    if (valueName.equals("Adapting2AdaptedOffDurationFilterSec")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.unadapt2AdaptingShortFilterLux = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.unadapt2AdaptingLongFilterLux = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.unadapt2AdaptingLongFilterSec = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.unadapt2AdaptingDimSec = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.adapting2UnadaptShortFilterLux = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.adapting2AdaptedOffDurationMinSec = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.adapting2AdaptedOffDurationFilterSec = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.adapting2AdaptedOffDurationMaxSec = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.adapting2AdaptedOnClockNoFilterBeginHour = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.adapting2AdaptedOnClockNoFilterEndHour = HwXmlElement.string2Int(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.unadapt2AdaptingShortFilterLux > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.unadapt2AdaptingLongFilterLux > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.unadapt2AdaptingLongFilterSec > 0 && HwBrightnessXmlLoader.mData.unadapt2AdaptingDimSec > 0 && HwBrightnessXmlLoader.mData.adapting2UnadaptShortFilterLux > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.adapting2AdaptedOffDurationMinSec > 0 && HwBrightnessXmlLoader.mData.adapting2AdaptedOffDurationFilterSec > 0 && HwBrightnessXmlLoader.mData.adapting2AdaptedOffDurationFilterSec > HwBrightnessXmlLoader.mData.adapting2AdaptedOffDurationMinSec && HwBrightnessXmlLoader.mData.adapting2AdaptedOffDurationMaxSec > 0 && HwBrightnessXmlLoader.mData.adapting2AdaptedOffDurationMaxSec > HwBrightnessXmlLoader.mData.adapting2AdaptedOffDurationFilterSec && HwBrightnessXmlLoader.mData.adapting2AdaptedOnClockNoFilterBeginHour >= 0 && HwBrightnessXmlLoader.mData.adapting2AdaptedOnClockNoFilterBeginHour < 24 && HwBrightnessXmlLoader.mData.adapting2AdaptedOnClockNoFilterEndHour >= 0 && HwBrightnessXmlLoader.mData.adapting2AdaptedOnClockNoFilterEndHour < 24;
        }
    }

    private static class Element_DarkAdapterGroup2 extends HwXmlElement {
        private Element_DarkAdapterGroup2() {
        }

        public String getName() {
            return "DarkAdapterGroup2";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"Unadapt2AdaptedShortFilterLux", "Unadapt2AdaptedOffDurationMinSec", "Unadapt2AdaptedOnClockNoFilterBeginHour", "Unadapt2AdaptedOnClockNoFilterEndHour", "Adapted2UnadaptShortFilterLux"});
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1872793361:
                    if (valueName.equals("Unadapt2AdaptedOnClockNoFilterEndHour")) {
                        z = true;
                        break;
                    }
                case -966441933:
                    if (valueName.equals("Adapted2UnadaptShortFilterLux")) {
                        z = true;
                        break;
                    }
                case -698098637:
                    if (valueName.equals("Unadapt2AdaptedShortFilterLux")) {
                        z = false;
                        break;
                    }
                case 603296714:
                    if (valueName.equals("Unadapt2AdaptedOffDurationMinSec")) {
                        z = true;
                        break;
                    }
                case 1768891837:
                    if (valueName.equals("Unadapt2AdaptedOnClockNoFilterBeginHour")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.unadapt2AdaptedShortFilterLux = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.unadapt2AdaptedOffDurationMinSec = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.unadapt2AdaptedOnClockNoFilterBeginHour = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.unadapt2AdaptedOnClockNoFilterEndHour = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.adapted2UnadaptShortFilterLux = HwXmlElement.string2Float(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.unadapt2AdaptedShortFilterLux > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.unadapt2AdaptedOffDurationMinSec > 0 && HwBrightnessXmlLoader.mData.unadapt2AdaptedOnClockNoFilterBeginHour >= 0 && HwBrightnessXmlLoader.mData.unadapt2AdaptedOnClockNoFilterBeginHour < 24 && HwBrightnessXmlLoader.mData.unadapt2AdaptedOnClockNoFilterEndHour >= 0 && HwBrightnessXmlLoader.mData.unadapt2AdaptedOnClockNoFilterEndHour < 24 && HwBrightnessXmlLoader.mData.adapted2UnadaptShortFilterLux > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
    }

    private static class Element_DarkLightGroup extends HwXmlElement {
        private Element_DarkLightGroup() {
        }

        public String getName() {
            return "DarkLightGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"DarkLightLevelMinThreshold", "DarkLightLevelMaxThreshold", "DarkLightLevelRatio", "DarkLightLuxMinThreshold", "DarkLightLuxMaxThreshold", "DarkLightLuxDelta"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1821009752:
                    if (valueName.equals("DarkLightLuxMinThreshold")) {
                        z = true;
                        break;
                    }
                case -1305576921:
                    if (valueName.equals("DarkLightLevelRatio")) {
                        z = true;
                        break;
                    }
                case 739225035:
                    if (valueName.equals("DarkLightLevelMaxThreshold")) {
                        z = true;
                        break;
                    }
                case 1213651101:
                    if (valueName.equals("DarkLightLevelMinThreshold")) {
                        z = false;
                        break;
                    }
                case 1899056169:
                    if (valueName.equals("DarkLightLuxDelta")) {
                        z = true;
                        break;
                    }
                case 1999531478:
                    if (valueName.equals("DarkLightLuxMaxThreshold")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.darkLightLevelMinThreshold = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkLightLevelMaxThreshold = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkLightLevelRatio = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkLightLuxMinThreshold = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkLightLuxMaxThreshold = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkLightLuxDelta = HwXmlElement.string2Float(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.darkLightLevelMinThreshold >= 0 && HwBrightnessXmlLoader.mData.darkLightLevelMaxThreshold >= 0 && HwBrightnessXmlLoader.mData.darkLightLevelRatio > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.darkLightLuxMinThreshold >= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.darkLightLuxMaxThreshold >= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
    }

    private static class Element_DarkenlinePoints extends HwXmlElement {
        private Element_DarkenlinePoints() {
        }

        public String getName() {
            return "DarkenlinePoints";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_DarkenlinePoints_Point extends HwXmlElement {
        private Element_DarkenlinePoints_Point() {
        }

        public String getName() {
            return "Point";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (!this.mIsParsed) {
                HwBrightnessXmlLoader.mData.darkenlinePoints.clear();
            }
            HwBrightnessXmlLoader.mData.darkenlinePoints = HwXmlElement.parsePointFList(parser, HwBrightnessXmlLoader.mData.darkenlinePoints);
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.checkPointsListIsOK(HwBrightnessXmlLoader.mData.darkenlinePoints);
        }
    }

    private static class Element_DayModeGroup extends HwXmlElement {
        private Element_DayModeGroup() {
        }

        public String getName() {
            return "DayModeGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"DayModeAlgoEnable", "DayModeSwitchTime", "DayModeBeginTime", "DayModeEndTime", "DayModeModifyNumPoint", "DayModeModifyMinBrightness"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1913291113:
                    if (valueName.equals("DayModeBeginTime")) {
                        z = true;
                        break;
                    }
                case -1608117751:
                    if (valueName.equals("DayModeEndTime")) {
                        z = true;
                        break;
                    }
                case -818760726:
                    if (valueName.equals("DayModeModifyMinBrightness")) {
                        z = true;
                        break;
                    }
                case -631473984:
                    if (valueName.equals("DayModeSwitchTime")) {
                        z = true;
                        break;
                    }
                case 1126774357:
                    if (valueName.equals("DayModeAlgoEnable")) {
                        z = false;
                        break;
                    }
                case 1716702627:
                    if (valueName.equals("DayModeModifyNumPoint")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.dayModeAlgoEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.dayModeSwitchTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.dayModeBeginTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.dayModeEndTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.dayModeModifyNumPoint = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.dayModeModifyMinBrightness = HwXmlElement.string2Int(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.dayModeSwitchTime > 0 && HwBrightnessXmlLoader.mData.dayModeBeginTime >= 0 && HwBrightnessXmlLoader.mData.dayModeBeginTime < 24 && HwBrightnessXmlLoader.mData.dayModeEndTime >= 0 && HwBrightnessXmlLoader.mData.dayModeEndTime < 24 && HwBrightnessXmlLoader.mData.dayModeModifyNumPoint > 0 && HwBrightnessXmlLoader.mData.dayModeModifyMinBrightness >= 4;
        }
    }

    private static class Element_DefaultBrightnessPoints extends HwXmlElement {
        private Element_DefaultBrightnessPoints() {
        }

        public String getName() {
            return "DefaultBrightnessPoints";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_DefaultBrightnessPoints_Point extends HwXmlElement {
        private Element_DefaultBrightnessPoints_Point() {
        }

        public String getName() {
            return "Point";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (!this.mIsParsed) {
                HwBrightnessXmlLoader.mData.defaultBrighnessLinePoints.clear();
            }
            HwBrightnessXmlLoader.mData.defaultBrighnessLinePoints = HwXmlElement.parsePointFList(parser, HwBrightnessXmlLoader.mData.defaultBrighnessLinePoints);
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.checkPointsListIsOK(HwBrightnessXmlLoader.mData.defaultBrighnessLinePoints);
        }
    }

    private static class Element_GameModeAmbientLuxValidBrightnessPoints extends HwXmlElement {
        private Element_GameModeAmbientLuxValidBrightnessPoints() {
        }

        public String getName() {
            return "GameModeAmbientLuxValidBrightnessPoints";
        }

        protected boolean isOptional() {
            return true;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            HwBrightnessXmlLoader.mData.gameModeOffsetValidAmbientLuxEnable = true;
            return true;
        }
    }

    private static class Element_GameModeAmbientLuxValidBrightnessPoints_Point extends HwXmlElement {
        private Element_GameModeAmbientLuxValidBrightnessPoints_Point() {
        }

        public String getName() {
            return "Point";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (!this.mIsParsed) {
                HwBrightnessXmlLoader.mData.gameModeAmbientLuxValidBrightnessPoints.clear();
            }
            HwBrightnessXmlLoader.mData.gameModeAmbientLuxValidBrightnessPoints = HwXmlElement.parseAmPointList(parser, HwBrightnessXmlLoader.mData.gameModeAmbientLuxValidBrightnessPoints);
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.checkAmPointsListIsOK(HwBrightnessXmlLoader.mData.gameModeAmbientLuxValidBrightnessPoints);
        }
    }

    private static class Element_GameModeGroup extends HwXmlElement {
        private Element_GameModeGroup() {
        }

        public String getName() {
            return "VehicleModeGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"GameModeEnable", "GameModeBrightenAnimationTime", "GameModeDarkentenAnimationTime", "GameModeDarkentenLongAnimationTime", "GameModeDarkentenLongTarget", "GameModeDarkentenLongCurrent", "GameModeClearOffsetTime", "GameModeBrightenDebounceTime", "GameModeDarkenDebounceTime"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1810836822:
                    if (valueName.equals("GameModeDarkenDebounceTime")) {
                        z = true;
                        break;
                    }
                case -1148292424:
                    if (valueName.equals("GameModeEnable")) {
                        z = false;
                        break;
                    }
                case -1094024722:
                    if (valueName.equals("GameModeBrightenDebounceTime")) {
                        z = true;
                        break;
                    }
                case -963189976:
                    if (valueName.equals("GameModeDarkentenAnimationTime")) {
                        z = true;
                        break;
                    }
                case -407732679:
                    if (valueName.equals("GameModeBrightenAnimationTime")) {
                        z = true;
                        break;
                    }
                case 1171599316:
                    if (valueName.equals("GameModeDarkentenLongCurrent")) {
                        z = true;
                        break;
                    }
                case 1419525784:
                    if (valueName.equals("GameModeClearOffsetTime")) {
                        z = true;
                        break;
                    }
                case 1832157836:
                    if (valueName.equals("GameModeDarkentenLongAnimationTime")) {
                        z = true;
                        break;
                    }
                case 2030028758:
                    if (valueName.equals("GameModeDarkentenLongTarget")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.gameModeEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.gameModeBrightenAnimationTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.gameModeDarkentenAnimationTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.gameModeDarkentenLongAnimationTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.gameModeDarkentenLongTarget = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.gameModeDarkentenLongCurrent = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.gameModeClearOffsetTime = HwXmlElement.string2Long(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.gameModeBrightenDebounceTime = HwXmlElement.string2Long(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.gameModeDarkenDebounceTime = HwXmlElement.string2Long(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.gameModeBrightenAnimationTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.gameModeDarkentenAnimationTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.gameModeDarkentenLongAnimationTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.gameModeDarkentenLongTarget >= 0 && HwBrightnessXmlLoader.mData.gameModeDarkentenLongCurrent >= 0 && HwBrightnessXmlLoader.mData.gameModeClearOffsetTime >= 0 && HwBrightnessXmlLoader.mData.gameModeBrightenDebounceTime >= 0 && HwBrightnessXmlLoader.mData.gameModeDarkenDebounceTime >= 0;
        }
    }

    private static class Element_InitGroup extends HwXmlElement {
        private Element_InitGroup() {
        }

        public String getName() {
            return "InitGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"InitDoubleSensorInterfere", "InitNumLastBuffer", "InitValidCloseTime", "InitUpperLuxThreshold", "InitSigmoidFuncSlope", "InitSlowReponseUpperLuxThreshold", "InitSlowReponseBrightTime"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -2087819444:
                    if (valueName.equals("InitNumLastBuffer")) {
                        z = true;
                        break;
                    }
                case -2027950061:
                    if (valueName.equals("InitDoubleSensorInterfere")) {
                        z = false;
                        break;
                    }
                case -1707591794:
                    if (valueName.equals("InitUpperLuxThreshold")) {
                        z = true;
                        break;
                    }
                case 416004804:
                    if (valueName.equals("InitSlowReponseBrightTime")) {
                        z = true;
                        break;
                    }
                case 672175713:
                    if (valueName.equals("InitSlowReponseUpperLuxThreshold")) {
                        z = true;
                        break;
                    }
                case 1178571689:
                    if (valueName.equals("InitSigmoidFuncSlope")) {
                        z = true;
                        break;
                    }
                case 1260225945:
                    if (valueName.equals("InitValidCloseTime")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.initDoubleSensorInterfere = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.initNumLastBuffer = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.initValidCloseTime = (long) HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.initUpperLuxThreshold = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.initSigmoidFuncSlope = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.initSlowReponseUpperLuxThreshold = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.initSlowReponseBrightTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.initDoubleSensorInterfere > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.initUpperLuxThreshold > 0 && HwBrightnessXmlLoader.mData.initSigmoidFuncSlope > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.initSlowReponseUpperLuxThreshold > 0;
        }
    }

    private static class Element_KeyguardResponseGroup extends HwXmlElement {
        private Element_KeyguardResponseGroup() {
        }

        public String getName() {
            return "KeyguardResponseGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"KeyguardResponseBrightenTime", "KeyguardResponseDarkenTime", "KeyguardAnimationBrightenTime", "KeyguardAnimationDarkenTime", "KeyguardLuxThreshold"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1788786797:
                    if (valueName.equals("KeyguardResponseDarkenTime")) {
                        z = true;
                        break;
                    }
                case -662444278:
                    if (valueName.equals("KeyguardAnimationDarkenTime")) {
                        z = true;
                        break;
                    }
                case 884106242:
                    if (valueName.equals("KeyguardLuxThreshold")) {
                        z = true;
                        break;
                    }
                case 1083838039:
                    if (valueName.equals("KeyguardResponseBrightenTime")) {
                        z = false;
                        break;
                    }
                case 1167240206:
                    if (valueName.equals("KeyguardAnimationBrightenTime")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.keyguardResponseBrightenTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.keyguardResponseDarkenTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.keyguardAnimationBrightenTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.keyguardAnimationDarkenTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.keyguardLuxThreshold = HwXmlElement.string2Float(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }
    }

    private class Element_LABCConfig extends HwXmlElement {
        private boolean mParseStarted;

        private Element_LABCConfig() {
        }

        public String getName() {
            return HwBrightnessXmlLoader.XML_NAME_NOEXT;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (this.mParseStarted) {
                return false;
            }
            if (HwBrightnessXmlLoader.this.mDeviceActualBrightnessLevel == 0) {
                if (this.HWFLOW) {
                    Slog.i(this.TAG, "actualDeviceLevel = 0, load started");
                }
                this.mParseStarted = true;
                return true;
            }
            String deviceLevelString = parser.getAttributeValue(null, MemoryConstant.MEM_FILECACHE_ITEM_LEVEL);
            String str;
            if (deviceLevelString == null || deviceLevelString.length() == 0) {
                if (this.HWFLOW) {
                    str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("actualDeviceLevel = ");
                    stringBuilder.append(HwBrightnessXmlLoader.this.mDeviceActualBrightnessLevel);
                    stringBuilder.append(", but can't find level in XML, load start");
                    Slog.i(str, stringBuilder.toString());
                }
                this.mParseStarted = true;
                return true;
            } else if (HwXmlElement.string2Int(deviceLevelString) != HwBrightnessXmlLoader.this.mDeviceActualBrightnessLevel) {
                return false;
            } else {
                if (this.HWFLOW) {
                    str = this.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("actualDeviceLevel = ");
                    stringBuilder2.append(HwBrightnessXmlLoader.this.mDeviceActualBrightnessLevel);
                    stringBuilder2.append(", find matched level in XML, load start");
                    Slog.i(str, stringBuilder2.toString());
                }
                this.mParseStarted = true;
                return true;
            }
        }
    }

    private static class Element_ManualBrightenLinePoints extends HwXmlElement {
        private Element_ManualBrightenLinePoints() {
        }

        public String getName() {
            return "ManualBrightenLinePoints";
        }

        protected boolean isOptional() {
            return HwBrightnessXmlLoader.mData.manualMode ^ 1;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_ManualBrightenLinePoints_Point extends HwXmlElement {
        private Element_ManualBrightenLinePoints_Point() {
        }

        public String getName() {
            return "Point";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (!this.mIsParsed) {
                HwBrightnessXmlLoader.mData.manualBrightenlinePoints.clear();
            }
            HwBrightnessXmlLoader.mData.manualBrightenlinePoints = HwXmlElement.parsePointFList(parser, HwBrightnessXmlLoader.mData.manualBrightenlinePoints);
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.checkPointsListIsOK(HwBrightnessXmlLoader.mData.manualBrightenlinePoints);
        }
    }

    private static class Element_ManualDarkenLinePoints extends HwXmlElement {
        private Element_ManualDarkenLinePoints() {
        }

        public String getName() {
            return "ManualDarkenLinePoints";
        }

        protected boolean isOptional() {
            return HwBrightnessXmlLoader.mData.manualMode ^ 1;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_ManualDarkenLinePoints_Point extends HwXmlElement {
        private Element_ManualDarkenLinePoints_Point() {
        }

        public String getName() {
            return "Point";
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (!this.mIsParsed) {
                HwBrightnessXmlLoader.mData.manualDarkenlinePoints.clear();
            }
            HwBrightnessXmlLoader.mData.manualDarkenlinePoints = HwXmlElement.parsePointFList(parser, HwBrightnessXmlLoader.mData.manualDarkenlinePoints);
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.checkPointsListIsOK(HwBrightnessXmlLoader.mData.manualDarkenlinePoints);
        }
    }

    private static class Element_ManualGroup extends HwXmlElement {
        private Element_ManualGroup() {
        }

        public String getName() {
            return "ManualGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"ManualMode", "ManualBrightnessMaxLimit", "ManualBrightnessMinLimit", "OutDoorThreshold", "InDoorThreshold", "ManualBrighenDebounceTime", "ManualDarkenDebounceTime"});
        }

        protected boolean isOptional() {
            return HwBrightnessXmlLoader.mData.manualMode ^ 1;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            boolean z2 = false;
            switch (valueName.hashCode()) {
                case -1693268306:
                    if (valueName.equals("ManualBrightnessMaxLimit")) {
                        z = true;
                        break;
                    }
                case -790346984:
                    if (valueName.equals("InDoorThreshold")) {
                        z = true;
                        break;
                    }
                case -585412741:
                    if (valueName.equals("ManualDarkenDebounceTime")) {
                        z = true;
                        break;
                    }
                case 346705267:
                    if (valueName.equals("ManualBrighenDebounceTime")) {
                        z = true;
                        break;
                    }
                case 597434025:
                    if (valueName.equals("ManualMode")) {
                        z = false;
                        break;
                    }
                case 701544399:
                    if (valueName.equals("OutDoorThreshold")) {
                        z = true;
                        break;
                    }
                case 825502336:
                    if (valueName.equals("ManualBrightnessMinLimit")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    Data access$6100 = HwBrightnessXmlLoader.mData;
                    if (HwXmlElement.string2Int(parser.nextText()) == 1) {
                        z2 = true;
                    }
                    access$6100.manualMode = z2;
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.manualBrightnessMaxLimit = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.manualBrightnessMinLimit = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.outDoorThreshold = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.inDoorThreshold = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.manualBrighenDebounceTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.manualDarkenDebounceTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.manualBrightnessMaxLimit > 0 && HwBrightnessXmlLoader.mData.manualBrightnessMinLimit <= 255 && HwBrightnessXmlLoader.mData.inDoorThreshold <= HwBrightnessXmlLoader.mData.outDoorThreshold && HwBrightnessXmlLoader.mData.manualBrighenDebounceTime >= 0 && HwBrightnessXmlLoader.mData.manualDarkenDebounceTime >= 0;
        }
    }

    private static class Element_ManualOptionalGroup extends HwXmlElement {
        private Element_ManualOptionalGroup() {
        }

        public String getName() {
            return "ManualOptionalGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"ManualAnimationBrightenTime", "ManualAnimationDarkenTime"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -1800575254) {
                if (hashCode == -1654934546 && valueName.equals("ManualAnimationBrightenTime")) {
                    z = false;
                    switch (z) {
                        case false:
                            HwBrightnessXmlLoader.mData.manualAnimationBrightenTime = HwXmlElement.string2Float(parser.nextText());
                            break;
                        case true:
                            HwBrightnessXmlLoader.mData.manualAnimationDarkenTime = HwXmlElement.string2Float(parser.nextText());
                            break;
                        default:
                            String str = this.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("unknow valueName=");
                            stringBuilder.append(valueName);
                            Slog.e(str, stringBuilder.toString());
                            return false;
                    }
                    return true;
                }
            } else if (valueName.equals("ManualAnimationDarkenTime")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.manualAnimationBrightenTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.manualAnimationDarkenTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
    }

    private static class Element_MiscGroup extends HwXmlElement {
        private Element_MiscGroup() {
        }

        public String getName() {
            return "MiscGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"lightSensorRateMills", "DefaultBrightness"});
        }

        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != 604635474) {
                if (hashCode == 803831879 && valueName.equals("lightSensorRateMills")) {
                    z = false;
                    switch (z) {
                        case false:
                            HwBrightnessXmlLoader.mData.lightSensorRateMills = HwXmlElement.string2Int(parser.nextText());
                            break;
                        case true:
                            HwBrightnessXmlLoader.mData.defaultBrightness = HwXmlElement.string2Float(parser.nextText());
                            break;
                        default:
                            String str = this.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("unknow valueName=");
                            stringBuilder.append(valueName);
                            Slog.e(str, stringBuilder.toString());
                            return false;
                    }
                    return true;
                }
            } else if (valueName.equals("DefaultBrightness")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.defaultBrightness > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
    }

    private static class Element_MiscOptionalGroup1 extends HwXmlElement {
        private Element_MiscOptionalGroup1() {
        }

        public String getName() {
            return "MiscOptionalGroup1";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"RebootAutoModeEnable", "PostMaxMinAvgFilterNoFilterNum", "PostMaxMinAvgFilterNum", "PowerOnFastResponseLuxNum", "PowerOnBrightenDebounceTime", "PowerOnDarkenDebounceTime", "CameraModeEnable", "CameraAnimationTime", "ProximityLuxThreshold", "ProximityResponseBrightenTime", "AnimatingForRGBWEnable", "MonitorEnable", "BrightnessCalibrationEnabled"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -2080828579:
                    if (valueName.equals("MonitorEnable")) {
                        z = true;
                        break;
                    }
                case -1912197839:
                    if (valueName.equals("PostMaxMinAvgFilterNoFilterNum")) {
                        z = true;
                        break;
                    }
                case -1540132752:
                    if (valueName.equals("ProximityResponseBrightenTime")) {
                        z = true;
                        break;
                    }
                case -1498785192:
                    if (valueName.equals("AnimatingForRGBWEnable")) {
                        z = true;
                        break;
                    }
                case -1271211816:
                    if (valueName.equals("PowerOnFastResponseLuxNum")) {
                        z = true;
                        break;
                    }
                case -457425365:
                    if (valueName.equals("CameraModeEnable")) {
                        z = true;
                        break;
                    }
                case -416433876:
                    if (valueName.equals("CameraAnimationTime")) {
                        z = true;
                        break;
                    }
                case -70231992:
                    if (valueName.equals("BrightnessCalibrationEnabled")) {
                        z = true;
                        break;
                    }
                case 782050106:
                    if (valueName.equals("RebootAutoModeEnable")) {
                        z = false;
                        break;
                    }
                case 1554640189:
                    if (valueName.equals("PowerOnBrightenDebounceTime")) {
                        z = true;
                        break;
                    }
                case 1784147739:
                    if (valueName.equals("ProximityLuxThreshold")) {
                        z = true;
                        break;
                    }
                case 1960905866:
                    if (valueName.equals("PostMaxMinAvgFilterNum")) {
                        z = true;
                        break;
                    }
                case 1981859257:
                    if (valueName.equals("PowerOnDarkenDebounceTime")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.rebootAutoModeEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.postMaxMinAvgFilterNoFilterNum = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.postMaxMinAvgFilterNum = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.powerOnFastResponseLuxNum = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.powerOnBrightenDebounceTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.powerOnDarkenDebounceTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.cameraModeEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.cameraAnimationTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.proximityLuxThreshold = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.proximityResponseBrightenTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.animatingForRGBWEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.monitorEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.brightnessCalibrationEnabled = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.postMaxMinAvgFilterNum > 0 && HwBrightnessXmlLoader.mData.postMaxMinAvgFilterNum <= HwBrightnessXmlLoader.mData.postMaxMinAvgFilterNoFilterNum && HwBrightnessXmlLoader.mData.powerOnFastResponseLuxNum > 0 && HwBrightnessXmlLoader.mData.powerOnBrightenDebounceTime >= 0 && HwBrightnessXmlLoader.mData.powerOnDarkenDebounceTime >= 0 && HwBrightnessXmlLoader.mData.cameraAnimationTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && ((double) HwBrightnessXmlLoader.mData.proximityLuxThreshold) > 1.0E-6d;
        }
    }

    private static class Element_MiscOptionalGroup2 extends HwXmlElement {
        private Element_MiscOptionalGroup2() {
        }

        public String getName() {
            return "MiscOptionalGroup2";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"ReportValueWhenSensorOnChange", "PgReregisterScene"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -2012946113) {
                if (hashCode == 1259478400 && valueName.equals("ReportValueWhenSensorOnChange")) {
                    z = false;
                    switch (z) {
                        case false:
                            HwBrightnessXmlLoader.mData.reportValueWhenSensorOnChange = HwXmlElement.string2Boolean(parser.nextText());
                            break;
                        case true:
                            HwBrightnessXmlLoader.mData.pgReregisterScene = HwXmlElement.string2Boolean(parser.nextText());
                            break;
                        default:
                            String str = this.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("unknow valueName=");
                            stringBuilder.append(valueName);
                            Slog.e(str, stringBuilder.toString());
                            return false;
                    }
                    return true;
                }
            } else if (valueName.equals("PgReregisterScene")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    private static class Element_OffsetGroup extends HwXmlElement {
        private Element_OffsetGroup() {
        }

        public String getName() {
            return "OffsetGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"OffsetBrightenDebounceTime", "OffsetDarkenDebounceTime", "OffsetValidAmbientLuxEnable", "AutoModeInOutDoorLimitEnble"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -1841552692) {
                if (hashCode != -1168465949) {
                    if (hashCode != 110170888) {
                        if (hashCode == 1853282876 && valueName.equals("AutoModeInOutDoorLimitEnble")) {
                            z = true;
                            switch (z) {
                                case false:
                                    HwBrightnessXmlLoader.mData.offsetBrightenDebounceTime = HwXmlElement.string2Int(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.offsetDarkenDebounceTime = HwXmlElement.string2Int(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.offsetValidAmbientLuxEnable = HwXmlElement.string2Boolean(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.autoModeInOutDoorLimitEnble = HwXmlElement.string2Boolean(parser.nextText());
                                    break;
                                default:
                                    String str = this.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("unknow valueName=");
                                    stringBuilder.append(valueName);
                                    Slog.e(str, stringBuilder.toString());
                                    return false;
                            }
                            return true;
                        }
                    } else if (valueName.equals("OffsetDarkenDebounceTime")) {
                        z = true;
                        switch (z) {
                            case false:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                } else if (valueName.equals("OffsetValidAmbientLuxEnable")) {
                    z = true;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                    return true;
                }
            } else if (valueName.equals("OffsetBrightenDebounceTime")) {
                z = false;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.offsetBrightenDebounceTime >= 0 && HwBrightnessXmlLoader.mData.offsetDarkenDebounceTime >= 0;
        }
    }

    private static class Element_OffsetResetGroup extends HwXmlElement {
        private Element_OffsetResetGroup() {
        }

        public String getName() {
            return "OffsetResetGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"OffsetResetSwitchTime", "OffsetResetEnable", "OffsetResetShortSwitchTime", "OffsetResetShortLuxDelta"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -1142640385) {
                if (hashCode != -454240631) {
                    if (hashCode != 82139585) {
                        if (hashCode == 1655845277 && valueName.equals("OffsetResetSwitchTime")) {
                            z = false;
                            switch (z) {
                                case false:
                                    HwBrightnessXmlLoader.mData.offsetResetSwitchTime = HwXmlElement.string2Int(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.offsetResetEnable = HwXmlElement.string2Boolean(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.offsetResetShortSwitchTime = HwXmlElement.string2Int(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.offsetResetShortLuxDelta = HwXmlElement.string2Int(parser.nextText());
                                    break;
                                default:
                                    String str = this.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("unknow valueName=");
                                    stringBuilder.append(valueName);
                                    Slog.e(str, stringBuilder.toString());
                                    return false;
                            }
                            return true;
                        }
                    } else if (valueName.equals("OffsetResetShortSwitchTime")) {
                        z = true;
                        switch (z) {
                            case false:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                } else if (valueName.equals("OffsetResetShortLuxDelta")) {
                    z = true;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                    return true;
                }
            } else if (valueName.equals("OffsetResetEnable")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.offsetResetSwitchTime >= 0 && HwBrightnessXmlLoader.mData.offsetResetShortSwitchTime >= 0 && HwBrightnessXmlLoader.mData.offsetResetShortLuxDelta >= 0;
        }
    }

    private static class Element_OutdoorResponseGroup extends HwXmlElement {
        private Element_OutdoorResponseGroup() {
        }

        public String getName() {
            return "OutdoorResponseGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"OutdoorLowerLuxThreshold", "OutdoorAnimationBrightenTime", "OutdoorAnimationDarkenTime", "OutdoorResponseBrightenRatio", "OutdoorResponseDarkenRatio", "OutdoorResponseBrightenTime", "OutdoorResponseDarkenTime", "OutdoorResponseCount"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1820473548:
                    if (valueName.equals("OutdoorAnimationDarkenTime")) {
                        z = true;
                        break;
                    }
                case -1232436373:
                    if (valueName.equals("OutdoorResponseBrightenRatio")) {
                        z = true;
                        break;
                    }
                case -856311255:
                    if (valueName.equals("OutdoorResponseDarkenTime")) {
                        z = true;
                        break;
                    }
                case -777923537:
                    if (valueName.equals("OutdoorResponseDarkenRatio")) {
                        z = true;
                        break;
                    }
                case -455330963:
                    if (valueName.equals("OutdoorResponseBrightenTime")) {
                        z = true;
                        break;
                    }
                case -110162222:
                    if (valueName.equals("OutdoorResponseCount")) {
                        z = true;
                        break;
                    }
                case 697641400:
                    if (valueName.equals("OutdoorAnimationBrightenTime")) {
                        z = true;
                        break;
                    }
                case 2114610753:
                    if (valueName.equals("OutdoorLowerLuxThreshold")) {
                        z = false;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.outdoorLowerLuxThreshold = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.outdoorAnimationBrightenTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.outdoorAnimationDarkenTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.outdoorResponseBrightenRatio = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.outdoorResponseDarkenRatio = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.outdoorResponseBrightenTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.outdoorResponseDarkenTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.outdoorResponseCount = HwXmlElement.string2Int(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return ((float) HwBrightnessXmlLoader.mData.outdoorLowerLuxThreshold) > -1.0E-6f && ((double) HwBrightnessXmlLoader.mData.outdoorResponseCount) > 1.0E-6d;
        }
    }

    private static class Element_PowerAndThermalGroup extends HwXmlElement {
        private Element_PowerAndThermalGroup() {
        }

        public String getName() {
            return "PowerAndThermalGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"ManualPowerSavingBrighnessLineEnable", "ManualPowerSavingAnimationBrightenTime", "ManualPowerSavingAnimationDarkenTime", "ManualThermalModeAnimationBrightenTime", "ManualThermalModeAnimationDarkenTime", "ThermalModeBrightnessMappingEnable", "PgModeBrightnessMappingEnable", "AutoPowerSavingUseManualAnimationTimeEnable", "PgSceneDetectionDarkenDelayTime", "PgSceneDetectionBrightenDelayTime", "ManualPowerSavingBrighnessLineDisableForDemo", "AutoPowerSavingBrighnessLineDisableForDemo"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1332786641:
                    if (valueName.equals("ManualPowerSavingBrighnessLineEnable")) {
                        z = false;
                        break;
                    }
                case -1199462784:
                    if (valueName.equals("ManualThermalModeAnimationBrightenTime")) {
                        z = true;
                        break;
                    }
                case -785273573:
                    if (valueName.equals("ManualPowerSavingAnimationBrightenTime")) {
                        z = true;
                        break;
                    }
                case -687994128:
                    if (valueName.equals("ManualPowerSavingBrighnessLineDisableForDemo")) {
                        z = true;
                        break;
                    }
                case -578235879:
                    if (valueName.equals("AutoPowerSavingBrighnessLineDisableForDemo")) {
                        z = true;
                        break;
                    }
                case 397849367:
                    if (valueName.equals("AutoPowerSavingUseManualAnimationTimeEnable")) {
                        z = true;
                        break;
                    }
                case 1064002566:
                    if (valueName.equals("ThermalModeBrightnessMappingEnable")) {
                        z = true;
                        break;
                    }
                case 1131515809:
                    if (valueName.equals("PgSceneDetectionDarkenDelayTime")) {
                        z = true;
                        break;
                    }
                case 1254090086:
                    if (valueName.equals("PgModeBrightnessMappingEnable")) {
                        z = true;
                        break;
                    }
                case 1265817084:
                    if (valueName.equals("ManualThermalModeAnimationDarkenTime")) {
                        z = true;
                        break;
                    }
                case 1914292055:
                    if (valueName.equals("ManualPowerSavingAnimationDarkenTime")) {
                        z = true;
                        break;
                    }
                case 1952764317:
                    if (valueName.equals("PgSceneDetectionBrightenDelayTime")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.manualPowerSavingBrighnessLineEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.manualPowerSavingAnimationBrightenTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.manualPowerSavingAnimationDarkenTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.manualThermalModeAnimationBrightenTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.manualThermalModeAnimationDarkenTime = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.thermalModeBrightnessMappingEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.pgModeBrightnessMappingEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.autoPowerSavingUseManualAnimationTimeEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.pgSceneDetectionDarkenDelayTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.pgSceneDetectionBrightenDelayTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.manualPowerSavingBrighnessLineDisableForDemo = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.autoPowerSavingBrighnessLineDisableForDemo = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.manualPowerSavingAnimationBrightenTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.manualPowerSavingAnimationDarkenTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.manualThermalModeAnimationBrightenTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.manualThermalModeAnimationDarkenTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.pgSceneDetectionDarkenDelayTime >= 0 && HwBrightnessXmlLoader.mData.pgSceneDetectionBrightenDelayTime >= 0;
        }
    }

    private static class Element_PreProcessing extends HwXmlElement {
        private Element_PreProcessing() {
        }

        public String getName() {
            return "PreProcessing";
        }

        protected boolean isOptional() {
            return true;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_PreProcessingGroup extends HwXmlElement {
        private Element_PreProcessingGroup() {
        }

        public String getName() {
            return "PreProcessingGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"BrightTimeDelayEnable", "BrightTimeDelay", "BrightTimeDelayLuxThreshold", "PreMethodNum", "PreMeanFilterNoFilterNum", "PreMeanFilterNum", "PostMethodNum", "PostMeanFilterNoFilterNum", "PostMeanFilterNum"});
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1216473595:
                    if (valueName.equals("PostMethodNum")) {
                        z = true;
                        break;
                    }
                case -591649441:
                    if (valueName.equals("BrightTimeDelayEnable")) {
                        z = false;
                        break;
                    }
                case 157937894:
                    if (valueName.equals("PreMeanFilterNum")) {
                        z = true;
                        break;
                    }
                case 620980681:
                    if (valueName.equals("PostMeanFilterNum")) {
                        z = true;
                        break;
                    }
                case 700394488:
                    if (valueName.equals("BrightTimeDelayLuxThreshold")) {
                        z = true;
                        break;
                    }
                case 1373278396:
                    if (valueName.equals("BrightTimeDelay")) {
                        z = true;
                        break;
                    }
                case 1431493488:
                    if (valueName.equals("PostMeanFilterNoFilterNum")) {
                        z = true;
                        break;
                    }
                case 1443034509:
                    if (valueName.equals("PreMeanFilterNoFilterNum")) {
                        z = true;
                        break;
                    }
                case 1524020130:
                    if (valueName.equals("PreMethodNum")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.brightTimeDelayEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.brightTimeDelay = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.brightTimeDelayLuxThreshold = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.preMethodNum = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.preMeanFilterNoFilterNum = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.preMeanFilterNum = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.postMethodNum = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.postMeanFilterNoFilterNum = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.postMeanFilterNum = HwXmlElement.string2Int(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.preMeanFilterNum > 0 && HwBrightnessXmlLoader.mData.preMeanFilterNum <= HwBrightnessXmlLoader.mData.preMeanFilterNoFilterNum && HwBrightnessXmlLoader.mData.postMeanFilterNum > 0 && HwBrightnessXmlLoader.mData.postMeanFilterNum <= HwBrightnessXmlLoader.mData.postMeanFilterNoFilterNum;
        }
    }

    private static class Element_PreWeightedGroup extends HwXmlElement {
        private Element_PreWeightedGroup() {
        }

        public String getName() {
            return "PreWeightedGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"PreWeightedMeanFilterNoFilterNum", "PreWeightedMeanFilterMaxFuncLuxNum", "PreWeightedMeanFilterNum", "PreWeightedMeanFilterAlpha", "PreWeightedMeanFilterLuxTh"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1627681866:
                    if (valueName.equals("PreWeightedMeanFilterNoFilterNum")) {
                        z = false;
                        break;
                    }
                case 246136847:
                    if (valueName.equals("PreWeightedMeanFilterNum")) {
                        z = true;
                        break;
                    }
                case 302040999:
                    if (valueName.equals("PreWeightedMeanFilterAlpha")) {
                        z = true;
                        break;
                    }
                case 312474924:
                    if (valueName.equals("PreWeightedMeanFilterLuxTh")) {
                        z = true;
                        break;
                    }
                case 1095802536:
                    if (valueName.equals("PreWeightedMeanFilterMaxFuncLuxNum")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.preWeightedMeanFilterNoFilterNum = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.preWeightedMeanFilterMaxFuncLuxNum = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.preWeightedMeanFilterNum = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.preWeightedMeanFilterAlpha = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.preWeightedMeanFilterLuxTh = HwXmlElement.string2Float(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.preWeightedMeanFilterNoFilterNum > 0 && HwBrightnessXmlLoader.mData.preWeightedMeanFilterMaxFuncLuxNum > 0 && HwBrightnessXmlLoader.mData.preWeightedMeanFilterNum > 0 && HwBrightnessXmlLoader.mData.preWeightedMeanFilterNum <= HwBrightnessXmlLoader.mData.preWeightedMeanFilterNoFilterNum;
        }
    }

    private static class Element_Proximity extends HwXmlElement {
        private Element_Proximity() {
        }

        public String getName() {
            return "Proximity";
        }

        protected boolean isOptional() {
            return true;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_ProximityGroup extends HwXmlElement {
        private Element_ProximityGroup() {
        }

        public String getName() {
            return "ProximityGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"AllowLabcUseProximity", "ProximityPositiveDebounceTime", "ProximityNegativeDebounceTime"});
        }

        /* JADX WARNING: Removed duplicated region for block: B:17:0x003c  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x003c  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x003c  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0054  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -636219254) {
                if (hashCode != 1277664151) {
                    if (hashCode == 1847924814 && valueName.equals("ProximityPositiveDebounceTime")) {
                        z = true;
                        switch (z) {
                            case false:
                                HwBrightnessXmlLoader.mData.allowLabcUseProximity = HwXmlElement.string2Boolean(parser.nextText());
                                break;
                            case true:
                                HwBrightnessXmlLoader.mData.proximityPositiveDebounceTime = HwXmlElement.string2Int(parser.nextText());
                                break;
                            case true:
                                HwBrightnessXmlLoader.mData.proximityNegativeDebounceTime = HwXmlElement.string2Int(parser.nextText());
                                break;
                            default:
                                String str = this.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("unknow valueName=");
                                stringBuilder.append(valueName);
                                Slog.e(str, stringBuilder.toString());
                                return false;
                        }
                        return true;
                    }
                } else if (valueName.equals("AllowLabcUseProximity")) {
                    z = false;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                    return true;
                }
            } else if (valueName.equals("ProximityNegativeDebounceTime")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    private static class Element_ReadingAnimationTime extends HwXmlElement {
        private Element_ReadingAnimationTime() {
        }

        public String getName() {
            return "ReadingAnimationTime";
        }

        protected boolean isOptional() {
            return true;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            HwBrightnessXmlLoader.mData.readingAnimationTime = HwXmlElement.string2Float(parser.nextText());
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.readingAnimationTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
    }

    private static class Element_ReadingModeEnable extends HwXmlElement {
        private Element_ReadingModeEnable() {
        }

        public String getName() {
            return "ReadingModeEnable";
        }

        protected boolean isOptional() {
            return true;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            HwBrightnessXmlLoader.mData.readingModeEnable = HwXmlElement.string2Boolean(parser.nextText());
            return true;
        }
    }

    private static class Element_RebootGroup extends HwXmlElement {
        private Element_RebootGroup() {
        }

        public String getName() {
            return "RebootGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"RebootFirstBrightnessAnimationEnable", "RebootFirstBrightness", "RebootFirstBrightnessAutoTime", "RebootFirstBrightnessManualTime"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -2008780644) {
                if (hashCode != -1474056744) {
                    if (hashCode != -752945685) {
                        if (hashCode == -260795025 && valueName.equals("RebootFirstBrightnessManualTime")) {
                            z = true;
                            switch (z) {
                                case false:
                                    HwBrightnessXmlLoader.mData.rebootFirstBrightnessAnimationEnable = HwXmlElement.string2Boolean(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.rebootFirstBrightness = HwXmlElement.string2Int(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.rebootFirstBrightnessAutoTime = HwXmlElement.string2Float(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.rebootFirstBrightnessManualTime = HwXmlElement.string2Float(parser.nextText());
                                    break;
                                default:
                                    String str = this.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("unknow valueName=");
                                    stringBuilder.append(valueName);
                                    Slog.e(str, stringBuilder.toString());
                                    return false;
                            }
                            return true;
                        }
                    } else if (valueName.equals("RebootFirstBrightnessAnimationEnable")) {
                        z = false;
                        switch (z) {
                            case false:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                } else if (valueName.equals("RebootFirstBrightnessAutoTime")) {
                    z = true;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                    return true;
                }
            } else if (valueName.equals("RebootFirstBrightness")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.rebootFirstBrightness >= 0 && HwBrightnessXmlLoader.mData.rebootFirstBrightnessAutoTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.rebootFirstBrightnessManualTime > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
    }

    private static class Element_ResponseTimeGroup extends HwXmlElement {
        private Element_ResponseTimeGroup() {
        }

        public String getName() {
            return "ResponseTimeGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"BrighenDebounceTime", "DarkenDebounceTime", "BrightenDebounceTimeParaBig", "DarkenDebounceTimeParaBig", "BrightenDeltaLuxPara", "DarkenDeltaLuxPara", "StabilityConstant", "StabilityTime1", "StabilityTime2"});
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1403734451:
                    if (valueName.equals("StabilityConstant")) {
                        z = true;
                        break;
                    }
                case -599365355:
                    if (valueName.equals("DarkenDebounceTime")) {
                        z = true;
                        break;
                    }
                case -85825767:
                    if (valueName.equals("BrighenDebounceTime")) {
                        z = false;
                        break;
                    }
                case -37334485:
                    if (valueName.equals("DarkenDebounceTimeParaBig")) {
                        z = true;
                        break;
                    }
                case 1002334874:
                    if (valueName.equals("BrightenDeltaLuxPara")) {
                        z = true;
                        break;
                    }
                case 1209051670:
                    if (valueName.equals("DarkenDeltaLuxPara")) {
                        z = true;
                        break;
                    }
                case 1578851579:
                    if (valueName.equals("StabilityTime1")) {
                        z = true;
                        break;
                    }
                case 1578851580:
                    if (valueName.equals("StabilityTime2")) {
                        z = true;
                        break;
                    }
                case 1866183975:
                    if (valueName.equals("BrightenDebounceTimeParaBig")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.brighenDebounceTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkenDebounceTime = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.brightenDebounceTimeParaBig = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkenDebounceTimeParaBig = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.brightenDeltaLuxPara = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkenDeltaLuxPara = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.stabilityConstant = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.stabilityTime1 = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.stabilityTime2 = HwXmlElement.string2Int(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.brighenDebounceTime > 0 && HwBrightnessXmlLoader.mData.darkenDebounceTime > 0;
        }
    }

    private static class Element_ResponseTimeOptionalGroup extends HwXmlElement {
        private Element_ResponseTimeOptionalGroup() {
        }

        public String getName() {
            return "ResponseTimeOptionalGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"BrighenDebounceTimeForSmallThr", "DarkenDebounceTimeForSmallThr", "RatioForBrightnenSmallThr", "RatioForDarkenSmallThr", "DarkTimeDelayEnable", "DarkTimeDelay", "DarkTimeDelayLuxThreshold", "DarkTimeDelayBeta0", "DarkTimeDelayBeta1", "DarkTimeDelayBeta2"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -2041716473:
                    if (valueName.equals("BrighenDebounceTimeForSmallThr")) {
                        z = false;
                        break;
                    }
                case -1203123972:
                    if (valueName.equals("DarkTimeDelayLuxThreshold")) {
                        z = true;
                        break;
                    }
                case -785220512:
                    if (valueName.equals("DarkTimeDelayBeta0")) {
                        z = true;
                        break;
                    }
                case -785220511:
                    if (valueName.equals("DarkTimeDelayBeta1")) {
                        z = true;
                        break;
                    }
                case -785220510:
                    if (valueName.equals("DarkTimeDelayBeta2")) {
                        z = true;
                        break;
                    }
                case -487716469:
                    if (valueName.equals("DarkenDebounceTimeForSmallThr")) {
                        z = true;
                        break;
                    }
                case 633705408:
                    if (valueName.equals("DarkTimeDelay")) {
                        z = true;
                        break;
                    }
                case 1174414900:
                    if (valueName.equals("RatioForDarkenSmallThr")) {
                        z = true;
                        break;
                    }
                case 1521603939:
                    if (valueName.equals("DarkTimeDelayEnable")) {
                        z = true;
                        break;
                    }
                case 1843038262:
                    if (valueName.equals("RatioForBrightnenSmallThr")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.brighenDebounceTimeForSmallThr = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkenDebounceTimeForSmallThr = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.ratioForBrightnenSmallThr = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.ratioForDarkenSmallThr = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkTimeDelayEnable = HwXmlElement.string2Boolean(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkTimeDelay = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkTimeDelayLuxThreshold = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkTimeDelayBeta0 = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkTimeDelayBeta1 = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.darkTimeDelayBeta2 = HwXmlElement.string2Float(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.brighenDebounceTimeForSmallThr > 0 && HwBrightnessXmlLoader.mData.darkenDebounceTimeForSmallThr > 0 && HwBrightnessXmlLoader.mData.ratioForBrightnenSmallThr > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.ratioForDarkenSmallThr > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
    }

    private static class Element_SceneProcessing extends HwXmlElement {
        private Element_SceneProcessing() {
        }

        public String getName() {
            return "SceneProcessing";
        }

        protected boolean isOptional() {
            return true;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_SceneProcessingGroup extends HwXmlElement {
        private Element_SceneProcessingGroup() {
        }

        public String getName() {
            return "SceneProcessingGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"SceneMaxPoints", "SceneGapPoints", "SceneMinPoints", "SceneAmbientLuxMaxWeight", "SceneAmbientLuxMinWeight"});
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            switch (valueName.hashCode()) {
                case -1975443769:
                    if (valueName.equals("SceneAmbientLuxMinWeight")) {
                        z = true;
                        break;
                    }
                case 180392013:
                    if (valueName.equals("SceneGapPoints")) {
                        z = true;
                        break;
                    }
                case 730661979:
                    if (valueName.equals("SceneMaxPoints")) {
                        z = false;
                        break;
                    }
                case 1503140553:
                    if (valueName.equals("SceneMinPoints")) {
                        z = true;
                        break;
                    }
                case 1547044953:
                    if (valueName.equals("SceneAmbientLuxMaxWeight")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    HwBrightnessXmlLoader.mData.sceneMaxPoints = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.sceneGapPoints = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.sceneMinPoints = HwXmlElement.string2Int(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.sceneAmbientLuxMaxWeight = HwXmlElement.string2Float(parser.nextText());
                    break;
                case true:
                    HwBrightnessXmlLoader.mData.sceneAmbientLuxMinWeight = HwXmlElement.string2Float(parser.nextText());
                    break;
                default:
                    String str = this.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unknow valueName=");
                    stringBuilder.append(valueName);
                    Slog.e(str, stringBuilder.toString());
                    return false;
            }
            return true;
        }
    }

    private static class Element_TouchProximity extends HwXmlElement {
        private Element_TouchProximity() {
        }

        public String getName() {
            return "TouchProximity";
        }

        protected boolean isOptional() {
            return true;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            return true;
        }
    }

    private static class Element_TouchProximityGroup extends HwXmlElement {
        private Element_TouchProximityGroup() {
        }

        public String getName() {
            return "TouchProximityGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"Enable", "YNearbyRatio"});
        }

        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* JADX WARNING: Removed duplicated region for block: B:12:0x002d  */
        /* JADX WARNING: Removed duplicated region for block: B:15:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -286453069) {
                if (hashCode == 2079986083 && valueName.equals("Enable")) {
                    z = false;
                    switch (z) {
                        case false:
                            HwBrightnessXmlLoader.mData.touchProximityEnable = HwXmlElement.string2Boolean(parser.nextText());
                            break;
                        case true:
                            HwBrightnessXmlLoader.mData.touchProximityYNearbyRatio = HwXmlElement.string2Float(parser.nextText());
                            break;
                        default:
                            String str = this.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("unknow valueName=");
                            stringBuilder.append(valueName);
                            Slog.e(str, stringBuilder.toString());
                            return false;
                    }
                    return true;
                }
            } else if (valueName.equals("YNearbyRatio")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }

        protected boolean checkValue() {
            return !HwBrightnessXmlLoader.mData.touchProximityEnable || (HwBrightnessXmlLoader.mData.touchProximityYNearbyRatio > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.touchProximityYNearbyRatio < 1.0f);
        }
    }

    private static class Element_VariableStep extends HwXmlElement {
        private Element_VariableStep() {
        }

        public String getName() {
            return "VariableStep";
        }

        protected boolean isOptional() {
            return true;
        }

        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            HwBrightnessXmlLoader.mData.useVariableStep = true;
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.darkenGradualTimeMin <= HwBrightnessXmlLoader.mData.darkenGradualTimeMax;
        }
    }

    private static class Element_VariableStepGroup extends HwXmlElement {
        private Element_VariableStepGroup() {
        }

        public String getName() {
            return "VariableStepGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"DarkenGradualTimeMax", "DarkenGradualTimeMin", "AnimatedStepRoundEnabled"});
        }

        /* JADX WARNING: Removed duplicated region for block: B:17:0x003c  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x003c  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0054  */
        /* JADX WARNING: Removed duplicated region for block: B:17:0x003c  */
        /* JADX WARNING: Removed duplicated region for block: B:21:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:20:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:19:0x0054  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -1259361630) {
                if (hashCode != -113440444) {
                    if (hashCode == -113440206 && valueName.equals("DarkenGradualTimeMin")) {
                        z = true;
                        switch (z) {
                            case false:
                                HwBrightnessXmlLoader.mData.darkenGradualTimeMax = HwXmlElement.string2Float(parser.nextText());
                                break;
                            case true:
                                HwBrightnessXmlLoader.mData.darkenGradualTimeMin = HwXmlElement.string2Float(parser.nextText());
                                break;
                            case true:
                                HwBrightnessXmlLoader.mData.animatedStepRoundEnabled = HwXmlElement.string2Boolean(parser.nextText());
                                break;
                            default:
                                String str = this.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("unknow valueName=");
                                stringBuilder.append(valueName);
                                Slog.e(str, stringBuilder.toString());
                                return false;
                        }
                        return true;
                    }
                } else if (valueName.equals("DarkenGradualTimeMax")) {
                    z = false;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                    return true;
                }
            } else if (valueName.equals("AnimatedStepRoundEnabled")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.darkenGradualTimeMax > GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO && HwBrightnessXmlLoader.mData.darkenGradualTimeMin >= GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        }
    }

    private static class Element_VehicleModeGroup extends HwXmlElement {
        private Element_VehicleModeGroup() {
        }

        public String getName() {
            return "VehicleModeGroup";
        }

        protected List<String> getNameList() {
            return Arrays.asList(new String[]{"VehicleModeEnable", "VehicleModeDisableTimeMillis", "VehicleModeQuitTimeForPowerOn", "VehicleModeEnterTimeForPowerOn"});
        }

        protected boolean isOptional() {
            return true;
        }

        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* JADX WARNING: Removed duplicated region for block: B:22:0x004b  */
        /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
        /* JADX WARNING: Removed duplicated region for block: B:26:0x0081  */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0072  */
        /* JADX WARNING: Removed duplicated region for block: B:24:0x0063  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected boolean parseValue(XmlPullParser parser) throws XmlPullParserException, IOException {
            boolean z;
            String valueName = parser.getName();
            int hashCode = valueName.hashCode();
            if (hashCode != -946673391) {
                if (hashCode != 349236422) {
                    if (hashCode != 689432882) {
                        if (hashCode == 939700588 && valueName.equals("VehicleModeDisableTimeMillis")) {
                            z = true;
                            switch (z) {
                                case false:
                                    HwBrightnessXmlLoader.mData.vehicleModeEnable = HwXmlElement.string2Boolean(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.vehicleModeDisableTimeMillis = HwXmlElement.string2Long(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.vehicleModeQuitTimeForPowerOn = HwXmlElement.string2Long(parser.nextText());
                                    break;
                                case true:
                                    HwBrightnessXmlLoader.mData.vehicleModeEnterTimeForPowerOn = HwXmlElement.string2Long(parser.nextText());
                                    break;
                                default:
                                    String str = this.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("unknow valueName=");
                                    stringBuilder.append(valueName);
                                    Slog.e(str, stringBuilder.toString());
                                    return false;
                            }
                            return true;
                        }
                    } else if (valueName.equals("VehicleModeEnable")) {
                        z = false;
                        switch (z) {
                            case false:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            case true:
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                } else if (valueName.equals("VehicleModeQuitTimeForPowerOn")) {
                    z = true;
                    switch (z) {
                        case false:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        case true:
                            break;
                        default:
                            break;
                    }
                    return true;
                }
            } else if (valueName.equals("VehicleModeEnterTimeForPowerOn")) {
                z = true;
                switch (z) {
                    case false:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    case true:
                        break;
                    default:
                        break;
                }
                return true;
            }
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
                case true:
                    break;
                default:
                    break;
            }
            return true;
        }

        protected boolean checkValue() {
            return HwBrightnessXmlLoader.mData.vehicleModeDisableTimeMillis >= 0 && HwBrightnessXmlLoader.mData.vehicleModeQuitTimeForPowerOn >= 0 && HwBrightnessXmlLoader.mData.vehicleModeEnterTimeForPowerOn >= 0;
        }
    }

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    public static Data getData() {
        Data retData = null;
        synchronized (mLock) {
            Data data;
            try {
                if (mLoader == null) {
                    mLoader = new HwBrightnessXmlLoader();
                }
                retData = mData;
                if (retData == null) {
                    data = new Data();
                    retData = data;
                    retData.loadDefaultConfig();
                }
            } catch (RuntimeException e) {
                try {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getData() failed! ");
                    stringBuilder.append(e);
                    Slog.e(str, stringBuilder.toString());
                    if (null == null) {
                        data = new Data();
                    }
                } catch (Throwable th) {
                    if (null == null) {
                        new Data().loadDefaultConfig();
                    }
                }
            }
        }
        return retData;
    }

    private static int getDeviceActualBrightnessLevel() {
        LightsManager lightsManager = (LightsManager) LocalServices.getService(LightsManager.class);
        if (lightsManager == null) {
            Slog.e(TAG, "getDeviceActualBrightnessLevel() can't get LightsManager");
            return 0;
        }
        Light lcdLight = lightsManager.getLight(0);
        if (lcdLight != null) {
            return lcdLight.getDeviceActualBrightnessLevel();
        }
        Slog.e(TAG, "getDeviceActualBrightnessLevel() can't get Light");
        return 0;
    }

    private HwBrightnessXmlLoader() {
        if (HWDEBUG) {
            Slog.d(TAG, "HwBrightnessXmlLoader()");
        }
        this.mDeviceActualBrightnessLevel = getDeviceActualBrightnessLevel();
        if (!parseXml(getXmlPath())) {
            mData.loadDefaultConfig();
        }
        mData.printData();
    }

    private boolean parseXml(String xmlPath) {
        if (xmlPath == null) {
            Slog.e(TAG, "parseXml() error! xmlPath is null");
            return false;
        }
        HwXmlParser xmlParser = new HwXmlParser(xmlPath);
        registerElement(xmlParser);
        if (!xmlParser.parse()) {
            Slog.e(TAG, "parseXml() error! xmlParser.parse() failed!");
            return false;
        } else if (xmlParser.check()) {
            if (HWFLOW) {
                Slog.i(TAG, "parseXml() load success!");
            }
            return true;
        } else {
            Slog.e(TAG, "parseXml() error! xmlParser.check() failed!");
            return false;
        }
    }

    private void registerElement(HwXmlParser parser) {
        HwXmlElement rootElement = parser.registerRootElement(new Element_LABCConfig());
        rootElement.registerChildElement(new Element_MiscGroup());
        rootElement.registerChildElement(new Element_MiscOptionalGroup1());
        rootElement.registerChildElement(new Element_MiscOptionalGroup2());
        rootElement.registerChildElement(new Element_ReadingModeEnable());
        rootElement.registerChildElement(new Element_ReadingAnimationTime());
        rootElement.registerChildElement(new Element_ResponseTimeGroup());
        rootElement.registerChildElement(new Element_ResponseTimeOptionalGroup());
        rootElement.registerChildElement(new Element_CoverModeGroup());
        rootElement.registerChildElement(new Element_CoverModeBrighnessLinePoints()).registerChildElement(new Element_CoverModeBrighnessLinePoints_Point());
        rootElement.registerChildElement(new Element_BackSensorCoverModeBrighnessLinePoints()).registerChildElement(new Element_BackSensorCoverModeBrighnessLinePoints_Point());
        rootElement.registerChildElement(new Element_PreWeightedGroup());
        rootElement.registerChildElement(new Element_KeyguardResponseGroup());
        rootElement.registerChildElement(new Element_OutdoorResponseGroup());
        rootElement.registerChildElement(new Element_InitGroup());
        rootElement.registerChildElement(new Element_PowerAndThermalGroup());
        rootElement.registerChildElement(new Element_DayModeGroup());
        rootElement.registerChildElement(new Element_OffsetResetGroup());
        rootElement.registerChildElement(new Element_OffsetGroup());
        rootElement.registerChildElement(new Element_DarkLightGroup());
        rootElement.registerChildElement(new Element_RebootGroup());
        rootElement.registerChildElement(new Element_SceneProcessing()).registerChildElement(new Element_SceneProcessingGroup());
        rootElement.registerChildElement(new Element_PreProcessing()).registerChildElement(new Element_PreProcessingGroup());
        rootElement.registerChildElement(new Element_BrightenlinePoints()).registerChildElement(new Element_BrightenlinePoints_Point());
        rootElement.registerChildElement(new Element_DarkenlinePoints()).registerChildElement(new Element_DarkenlinePoints_Point());
        rootElement.registerChildElement(new Element_DefaultBrightnessPoints()).registerChildElement(new Element_DefaultBrightnessPoints_Point());
        rootElement.registerChildElement(new Element_AnimateGroup());
        rootElement.registerChildElement(new Element_AnimateOptionalGroup());
        rootElement.registerChildElement(new Element_VariableStep()).registerChildElement(new Element_VariableStepGroup());
        rootElement.registerChildElement(new Element_Proximity()).registerChildElement(new Element_ProximityGroup());
        rootElement.registerChildElement(new Element_ManualGroup());
        rootElement.registerChildElement(new Element_ManualOptionalGroup());
        rootElement.registerChildElement(new Element_ManualBrightenLinePoints()).registerChildElement(new Element_ManualBrightenLinePoints_Point());
        rootElement.registerChildElement(new Element_ManualDarkenLinePoints()).registerChildElement(new Element_ManualDarkenLinePoints_Point());
        rootElement.registerChildElement(new Element_BrightnessMappingPoints()).registerChildElement(new Element_BrightnessMappingPoints_Point());
        HwXmlElement ambientLuxValidBrightnessPoints = rootElement.registerChildElement(new Element_AmbientLuxValidBrightnessPoints());
        ambientLuxValidBrightnessPoints.registerChildElement(new Element_AmbientLuxValidBrightnessPoints_Point());
        HwXmlElement darkAdapter = rootElement.registerChildElement(new Element_DarkAdapter());
        darkAdapter.registerChildElement(new Element_DarkAdapterGroup1());
        darkAdapter.registerChildElement(new Element_DarkAdapterGroup2());
        rootElement.registerChildElement(new Element_TouchProximity()).registerChildElement(new Element_TouchProximityGroup());
        rootElement.registerChildElement(new Element_VehicleModeGroup());
        rootElement.registerChildElement(new Element_GameModeGroup());
        rootElement.registerChildElement(new Element_CryogenicGroup());
        rootElement.registerChildElement(new Element_GameModeAmbientLuxValidBrightnessPoints()).registerChildElement(new Element_GameModeAmbientLuxValidBrightnessPoints_Point());
    }

    private File getFactoryXmlFile() {
        String xmlPath = String.format("/xml/lcd/%s_%s%s", new Object[]{XML_NAME_NOEXT, "factory", XML_EXT});
        File xmlFile = HwCfgFilePolicy.getCfgFile(xmlPath, 0);
        if (xmlFile != null) {
            return xmlFile;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("get xmlFile :");
        stringBuilder.append(xmlPath);
        stringBuilder.append(" failed!");
        Slog.e(str, stringBuilder.toString());
        return null;
    }

    private String getLcdPanelName() {
        IBinder binder = ServiceManager.getService("DisplayEngineExService");
        String panelName = null;
        if (binder == null) {
            Slog.i(TAG, "getLcdPanelName() binder is null!");
            return null;
        }
        IDisplayEngineServiceEx mService = Stub.asInterface(binder);
        if (mService == null) {
            Slog.e(TAG, "getLcdPanelName() mService is null!");
            return null;
        }
        byte[] name = new byte[128];
        int ret = 0;
        try {
            int ret2 = mService.getEffect(14, 0, name, name.length);
            if (ret2 != 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getLcdPanelName() getEffect failed! ret=");
                stringBuilder.append(ret2);
                Slog.e(str, stringBuilder.toString());
                return null;
            }
            try {
                panelName = new String(name, "UTF-8").trim().replace(' ', '_');
            } catch (UnsupportedEncodingException e) {
                Slog.e(TAG, "Unsupported encoding type!");
            }
            return panelName;
        } catch (RemoteException e2) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getLcdPanelName() RemoteException ");
            stringBuilder2.append(e2);
            Slog.e(str2, stringBuilder2.toString());
            return null;
        }
    }

    private String getVersionFromTouchOemInfo() {
        String version = null;
        try {
            File file = new File(String.format("%s", new Object[]{TOUCH_OEM_INFO_PATH}));
            if (file.exists()) {
                String touch_oem_info = FileUtils.readTextFile(file, 0, null).trim();
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("touch_oem_info=");
                stringBuilder.append(touch_oem_info);
                Slog.i(str, stringBuilder.toString());
                String[] versionInfo = touch_oem_info.split(",");
                if (versionInfo.length > 15) {
                    try {
                        int productYear = Integer.parseInt(versionInfo[12]);
                        int productMonth = Integer.parseInt(versionInfo[13]);
                        int productDay = Integer.parseInt(versionInfo[14]);
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("lcdversionInfo orig productYear=");
                        stringBuilder2.append(productYear);
                        stringBuilder2.append(",productMonth=");
                        stringBuilder2.append(productMonth);
                        stringBuilder2.append(",productDay=");
                        stringBuilder2.append(productDay);
                        Slog.i(str2, stringBuilder2.toString());
                        String str3;
                        StringBuilder stringBuilder3;
                        if (productYear < 48 || productYear > 57) {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("lcdversionInfo not valid productYear=");
                            stringBuilder3.append(productYear);
                            Slog.i(str3, stringBuilder3.toString());
                            return null;
                        }
                        String version2;
                        productYear -= 48;
                        if (productMonth >= 48 && productMonth <= 57) {
                            productMonth -= 48;
                        } else if (productMonth < 65 || productMonth > 67) {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("lcdversionInfo not valid productMonth=");
                            stringBuilder3.append(productMonth);
                            Slog.i(str3, stringBuilder3.toString());
                            return null;
                        } else {
                            productMonth = (productMonth - 65) + 10;
                        }
                        if (productDay >= 48 && productDay <= 57) {
                            productDay -= 48;
                        } else if (productDay < 65 || productDay > 88) {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("lcdversionInfo not valid productDay=");
                            stringBuilder3.append(productDay);
                            Slog.i(str3, stringBuilder3.toString());
                            return null;
                        } else {
                            productDay = (productDay - 65) + 10;
                        }
                        if (productYear > 8) {
                            version2 = "vn2";
                        } else if (productYear == 8 && productMonth > 1) {
                            version2 = "vn2";
                        } else if (productYear == 8 && productMonth == 1 && productDay >= 22) {
                            version2 = "vn2";
                        } else {
                            str3 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("lcdversionInfo not valid version;productYear=");
                            stringBuilder3.append(productYear);
                            stringBuilder3.append(",productMonth=");
                            stringBuilder3.append(productMonth);
                            stringBuilder3.append(",productDay=");
                            stringBuilder3.append(productDay);
                            Slog.i(str3, stringBuilder3.toString());
                            return null;
                        }
                        version = version2;
                        version2 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("lcdversionInfo real vn2,productYear=");
                        stringBuilder4.append(productYear);
                        stringBuilder4.append(",productMonth=");
                        stringBuilder4.append(productMonth);
                        stringBuilder4.append(",productDay=");
                        stringBuilder4.append(productDay);
                        Slog.i(version2, stringBuilder4.toString());
                    } catch (NumberFormatException e) {
                        Slog.i(TAG, "lcdversionInfo versionfile num is not valid,no need version");
                        return null;
                    }
                }
                Slog.i(TAG, "lcdversionInfo versionfile info length is not valid,no need version");
            } else {
                Slog.i(TAG, "lcdversionInfo versionfile is not exists, no need version,filePath=/sys/touchscreen/touch_oem_info");
            }
        } catch (IOException e2) {
            Slog.w(TAG, "Error reading touch_oem_info", e2);
        }
        return version;
    }

    private String getVersionFromLCD() {
        IBinder binder = ServiceManager.getService("DisplayEngineExService");
        String panelVersion = null;
        if (binder == null) {
            Slog.i(TAG, "getLcdPanelName() binder is null!");
            return null;
        }
        IDisplayEngineServiceEx mService = Stub.asInterface(binder);
        if (mService == null) {
            Slog.e(TAG, "getLcdPanelName() mService is null!");
            return null;
        }
        byte[] name = new byte[32];
        String key;
        try {
            int ret = mService.getEffect(14, 3, name, name.length);
            String str;
            StringBuilder stringBuilder;
            if (ret != 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getLcdPanelName() getEffect failed! ret=");
                stringBuilder.append(ret);
                Slog.e(str, stringBuilder.toString());
                return null;
            }
            try {
                str = new String(name, "UTF-8").trim();
                key = "VER:";
                int index = str.indexOf(key);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getVersionFromLCD() index=");
                stringBuilder2.append(index);
                stringBuilder2.append(",lcdVersion=");
                stringBuilder2.append(str);
                Slog.i(str2, stringBuilder2.toString());
                if (index != -1) {
                    panelVersion = str.substring(key.length() + index);
                }
            } catch (UnsupportedEncodingException e) {
                Slog.e(TAG, "Unsupported encoding type!");
            }
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("getVersionFromLCD() panelVersion=");
            stringBuilder.append(panelVersion);
            Slog.i(str, stringBuilder.toString());
            return panelVersion;
        } catch (RemoteException e2) {
            key = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("getLcdPanelName() RemoteException ");
            stringBuilder3.append(e2);
            Slog.e(key, stringBuilder3.toString());
            return null;
        }
    }

    private File getNormalXmlFile() {
        String lcdname = getLcdPanelName();
        String lcdversion = getVersionFromTouchOemInfo();
        String lcdversionNew = getVersionFromLCD();
        String screenColor = SystemProperties.get("ro.config.devicecolor");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("screenColor=");
        stringBuilder.append(screenColor);
        Slog.i(str, stringBuilder.toString());
        ArrayList<String> xmlPathList = new ArrayList();
        if (lcdversion != null) {
            xmlPathList.add(String.format("/xml/lcd/%s_%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, lcdversion, screenColor, XML_EXT}));
            xmlPathList.add(String.format("/xml/lcd/%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, lcdversion, XML_EXT}));
        }
        if (lcdversionNew != null) {
            xmlPathList.add(String.format("/xml/lcd/%s_%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, lcdversionNew, screenColor, XML_EXT}));
            xmlPathList.add(String.format("/xml/lcd/%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, lcdversionNew, XML_EXT}));
        }
        xmlPathList.add(String.format("/xml/lcd/%s_%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, screenColor, XML_EXT}));
        xmlPathList.add(String.format("/xml/lcd/%s_%s%s", new Object[]{XML_NAME_NOEXT, lcdname, XML_EXT}));
        xmlPathList.add(String.format("/xml/lcd/%s_%s%s", new Object[]{XML_NAME_NOEXT, screenColor, XML_EXT}));
        xmlPathList.add(String.format("/xml/lcd/%s", new Object[]{XML_NAME}));
        File xmlFile = null;
        Iterator it = xmlPathList.iterator();
        while (it.hasNext()) {
            xmlFile = HwCfgFilePolicy.getCfgFile((String) it.next(), 2);
            if (xmlFile != null) {
                return xmlFile;
            }
        }
        Slog.e(TAG, "get failed!");
        return xmlFile;
    }

    private String getXmlPath() {
        File xmlFile;
        String currentMode = SystemProperties.get("ro.runmode");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("currentMode=");
        stringBuilder.append(currentMode);
        Slog.i(str, stringBuilder.toString());
        if (currentMode == null) {
            xmlFile = getNormalXmlFile();
            if (xmlFile == null) {
                return null;
            }
        } else if (currentMode.equals("factory")) {
            xmlFile = getFactoryXmlFile();
            if (xmlFile == null) {
                return null;
            }
        } else if (currentMode.equals("normal")) {
            xmlFile = getNormalXmlFile();
            if (xmlFile == null) {
                return null;
            }
        } else {
            xmlFile = getNormalXmlFile();
            if (xmlFile == null) {
                return null;
            }
        }
        return xmlFile.getAbsolutePath();
    }

    private static boolean checkPointsListIsOK(List<PointF> list) {
        if (list == null) {
            Slog.e(TAG, "checkPointsListIsOK() error! list is null");
            return false;
        } else if (list.size() < 3 || list.size() >= 100) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkPointsListIsOK() error! list size=");
            stringBuilder.append(list.size());
            stringBuilder.append(" is out of range");
            Slog.e(str, stringBuilder.toString());
            return false;
        } else {
            PointF lastPoint = null;
            for (PointF point : list) {
                if (lastPoint == null || point.x > lastPoint.x) {
                    lastPoint = point;
                } else {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("checkPointsListIsOK() error! x in list isn't a increasing sequence, ");
                    stringBuilder2.append(point.x);
                    stringBuilder2.append("<=");
                    stringBuilder2.append(lastPoint.x);
                    Slog.e(str2, stringBuilder2.toString());
                    return false;
                }
            }
            return true;
        }
    }

    private static boolean checkAmPointsListIsOK(List<HwXmlAmPoint> list) {
        if (list == null) {
            Slog.e(TAG, "checkPointsListIsOK() error! list is null");
            return false;
        } else if (list.size() < 3 || list.size() >= 100) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkPointsListIsOK() error! list size=");
            stringBuilder.append(list.size());
            stringBuilder.append(" is out of range");
            Slog.e(str, stringBuilder.toString());
            return false;
        } else {
            HwXmlAmPoint lastPoint = null;
            for (HwXmlAmPoint point : list) {
                if (lastPoint == null || point.x > lastPoint.x) {
                    lastPoint = point;
                } else {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("checkPointsListIsOK() error! x in list isn't a increasing sequence, ");
                    stringBuilder2.append(point.x);
                    stringBuilder2.append("<=");
                    stringBuilder2.append(lastPoint.x);
                    Slog.e(str2, stringBuilder2.toString());
                    return false;
                }
            }
            return true;
        }
    }
}
