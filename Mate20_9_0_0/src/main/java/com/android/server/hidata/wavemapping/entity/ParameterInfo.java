package com.android.server.hidata.wavemapping.entity;

import android.hardware.biometrics.fingerprint.V2_1.RequestStatus;
import android.os.SystemProperties;
import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.rms.iaware.appmng.AwareAppAssociate;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class ParameterInfo {
    private static final String chipPlatformValue = SystemProperties.get("ro.board.platform", "UNDEFINED");
    private static final String logSysInfo = SystemProperties.get("ro.logsystem.usertype", "0");
    public int WIFI_DATA_SAMPLE = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
    private float abnMacRatioAllowUpd = 0.5f;
    private int activeSample = 60000;
    private int acumlteCount = 10;
    private int appTH_Duration_min = AwareAppAssociate.ASSOC_DECAY_MIN_TIME;
    private int appTH_GoodCnt_min = 0;
    private int appTH_PoorCnt_min = 5;
    private float appTH_Target_Ration_margin = 0.5f;
    private boolean back4GEnabled = true;
    private double back4GTH_duration_4gRatio = 0.5d;
    private int back4GTH_duration_min = 300000;
    private int back4GTH_out4G_interval = Constant.SEND_STALL_MSG_DELAY;
    private int back4GTH_signal_min = RequestStatus.SYS_ETIMEDOUT;
    private int batchID = 0;
    private int batchInterval = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
    private int bssidStart = 4;
    private int cellIDStart = 5;
    private int checkAgingAcumlteCount = 2000000000;
    private int config_ver = 0;
    private float connectedStrength = 0.2f;
    private int deleteFlag = 1;
    private int fg_batch_num = 20;
    private String forceTesting = "";
    private boolean isMainAp = false;
    private boolean isTest01 = false;
    private int knnMaxDist = 150;
    private float knnShareMacRatio = 0.7f;
    private int labelID = 1;
    private int limitBgScanCnt = 40;
    private int limitScanIntervalTime = 25000;
    private int limitStallScanCnt = 80;
    private int limitTotalScanCnt = 100;
    private int linkSpeedID = 3;
    private int lowerBound = -70;
    private int maxBssidNum = HwAPPQoEUtils.APP_TYPE_GENERAL_GAME;
    private float maxDist = 150.0f;
    private float maxDistBak = 150.0f;
    private float maxDistDecayRatio = 0.8f;
    private float maxDistMinLimit = 100.0f;
    private float maxShatterRatio = 0.8f;
    private int minBssidCount = 3;
    private float minDistinctValue = 5.0f;
    private int minFreq = 1;
    private float minMainAPOccUpd = 0.3f;
    private int minMeanRssi = -80;
    private int minModelTypes = 2;
    private float minNonzeroPerct = 0.2f;
    private float minSampleRatio = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
    private float minStdev = 2.0f;
    private int minTrainBssiLstLenUpd = 20;
    private float minUkwnRatioUpd = 0.1f;
    private float minUnkwnRatio = 0.25f;
    private int mobileApCheckLimit = 5;
    private int nClusters = 7;
    private int neighborNum = 3;
    private int[] paraPeriodBgScan = new int[]{30000, 30000};
    private int[] paraPeriodOut4gScan = new int[]{30000, 30000, 60000};
    private int[] paraPeriodStallScan = new int[]{20000, 30000, 40000, 60000};
    private boolean powerPreferEnabled = true;
    private double powerSaveGap = 0.9d;
    private int powerSaveType = 0;
    private double prefDurationRatio = 0.8d;
    private float prefFreqRatio = 0.8f;
    private int reGetPsRegStatus = 30000;
    private int reprMinOcc = 5;
    private int scanCH = 3;
    private int scanMAC = 1;
    private int scanRSSI = 2;
    private int scanSSID = 0;
    private int scanWifiStart = 20;
    private int servingWiFiLinkSpeed = 13;
    private int servingWiFiMAC = 11;
    private int servingWiFiRSSI = 14;
    private float shareRatioParam = 0.5f;
    private long startDuration = HwArbitrationDEFS.DelayTimeMillisB;
    private int startTimes = 5;
    private int testDataCnt = 100;
    private float testDataRatio = 0.2f;
    private int testDataSize = 40;
    private int threshold = -80;
    private int timestamp = 3;
    private int timestampID = 2;
    private float totalShatterRatio = 0.2f;
    private int trainDatasSize = 300;
    private boolean userPreferEnabled = true;
    private float weightParam = 0.3f;
    private String wifiSeperate = CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER;
    private long writeFileSample = HwArbitrationDEFS.NotificationMonitorPeriodMillis;

    public ParameterInfo(boolean isMainAp) {
        if (isMainAp) {
            this.config_ver = 0;
            this.scanWifiStart = 10;
            this.maxDist = 25.0f;
            this.weightParam = 0.3f;
            this.shareRatioParam = 0.99f;
            this.connectedStrength = 0.2f;
            this.minFreq = 5;
            this.knnMaxDist = 100;
            this.knnShareMacRatio = 1.0f;
            this.neighborNum = 3;
            this.testDataRatio = 0.2f;
            this.trainDatasSize = 200;
            this.testDataSize = 40;
            this.deleteFlag = 1;
            this.minNonzeroPerct = 0.2f;
            this.minDistinctValue = 5.0f;
            this.minStdev = 5.0f;
            this.threshold = -80;
            this.minBssidCount = 3;
            this.minMeanRssi = -80;
            this.reprMinOcc = 5;
            this.lowerBound = -70;
            this.minUnkwnRatio = 0.8f;
            this.testDataCnt = 100;
            this.maxShatterRatio = 0.5f;
            this.totalShatterRatio = 0.2f;
            this.maxDistDecayRatio = 0.8f;
            this.acumlteCount = 5;
            this.maxDistMinLimit = 25.0f;
            this.checkAgingAcumlteCount = 800;
            this.minMainAPOccUpd = 0.3f;
            this.minUkwnRatioUpd = 0.8f;
            this.minTrainBssiLstLenUpd = 20;
            this.abnMacRatioAllowUpd = 0.8f;
            this.fg_batch_num = 100;
            this.activeSample = 60000;
            this.isMainAp = true;
            this.maxBssidNum = 1000;
            return;
        }
        this.config_ver = 0;
        this.scanWifiStart = 20;
        this.maxDist = 150.0f;
        this.weightParam = 0.3f;
        this.shareRatioParam = 0.7f;
        this.connectedStrength = 0.2f;
        this.minFreq = 5;
        this.knnMaxDist = 150;
        this.knnShareMacRatio = 0.66f;
        this.neighborNum = 7;
        this.testDataRatio = 0.2f;
        this.trainDatasSize = 200;
        this.testDataSize = 40;
        this.deleteFlag = 1;
        this.minNonzeroPerct = 0.2f;
        this.minDistinctValue = 5.0f;
        this.minStdev = 2.0f;
        this.threshold = -80;
        this.minBssidCount = 3;
        this.minMeanRssi = -80;
        this.reprMinOcc = 1;
        this.lowerBound = -100;
        this.minUnkwnRatio = 0.6f;
        this.testDataCnt = 100;
        this.maxShatterRatio = 0.8f;
        this.totalShatterRatio = 0.2f;
        this.maxDistDecayRatio = 0.8f;
        this.acumlteCount = 5;
        this.maxDistMinLimit = 100.0f;
        this.checkAgingAcumlteCount = 500;
        this.minMainAPOccUpd = 0.3f;
        this.minUkwnRatioUpd = 0.7f;
        this.minTrainBssiLstLenUpd = 20;
        this.abnMacRatioAllowUpd = 0.5f;
        this.fg_batch_num = 100;
        this.isMainAp = false;
        this.maxBssidNum = 1000;
    }

    public int getConfig_ver() {
        return this.config_ver;
    }

    public void setConfig_ver(int config_ver) {
        this.config_ver = config_ver;
    }

    public boolean isBetaUser() {
        if (logSysInfo.equals("3")) {
            return true;
        }
        return false;
    }

    public int getTestDataSize() {
        return this.testDataSize;
    }

    public void setTestDataSize(int testDataSize) {
        this.testDataSize = testDataSize;
    }

    public float getTestDataRatio() {
        return this.testDataRatio;
    }

    public void setTestDataRatio(float testDataRatio) {
        this.testDataRatio = testDataRatio;
    }

    public int getMinFreq() {
        return this.minFreq;
    }

    public void setMinFreq(int minFreq) {
        this.minFreq = minFreq;
    }

    public float getConnectedStrength() {
        return this.connectedStrength;
    }

    public void setConnectedStrength(float connectedStrength) {
        this.connectedStrength = connectedStrength;
    }

    public int getMinTrainBssiLstLenUpd() {
        return this.minTrainBssiLstLenUpd;
    }

    public void setMinTrainBssiLstLenUpd(int minTrainBssiLstLenUpd) {
        this.minTrainBssiLstLenUpd = minTrainBssiLstLenUpd;
    }

    public float getAbnMacRatioAllowUpd() {
        return this.abnMacRatioAllowUpd;
    }

    public void setAbnMacRatioAllowUpd(float abnMacRatioAllowUpd) {
        this.abnMacRatioAllowUpd = abnMacRatioAllowUpd;
    }

    public boolean isTest01() {
        return this.isTest01;
    }

    public void setTest01(boolean test01) {
        this.isTest01 = test01;
    }

    public String getWifiSeperate() {
        return this.wifiSeperate;
    }

    public void setWifiSeperate(String wifiSeperate) {
        this.wifiSeperate = wifiSeperate;
    }

    public int getMinModelTypes() {
        return this.minModelTypes;
    }

    public void setMinModelTypes(int minModelTypes) {
        this.minModelTypes = minModelTypes;
    }

    public float getMinMainAPOccUpd() {
        return this.minMainAPOccUpd;
    }

    public void setMinMainAPOccUpd(float minMainAPOccUpd) {
        this.minMainAPOccUpd = minMainAPOccUpd;
    }

    public float getMaxDistMinLimit() {
        return this.maxDistMinLimit;
    }

    public void setMaxDistMinLimit(float maxDistMinLimit) {
        this.maxDistMinLimit = maxDistMinLimit;
    }

    public float getMaxDistBak() {
        return this.maxDistBak;
    }

    public void setMaxDistBak(float maxDistBak) {
        this.maxDistBak = maxDistBak;
    }

    public int getTrainDatasSize() {
        return this.trainDatasSize;
    }

    public void setTrainDatasSize(int trainDatasSize) {
        this.trainDatasSize = trainDatasSize;
    }

    public int getCheckAgingAcumlteCount() {
        return this.checkAgingAcumlteCount;
    }

    public void setCheckAgingAcumlteCount(int checkAgingAcumlteCount) {
        this.checkAgingAcumlteCount = checkAgingAcumlteCount;
    }

    public float getMinUkwnRatioUpd() {
        return this.minUkwnRatioUpd;
    }

    public void setMinUkwnRatioUpd(float minUkwnRatioUpd) {
        this.minUkwnRatioUpd = minUkwnRatioUpd;
    }

    public int getAcumlteCount() {
        return this.acumlteCount;
    }

    public void setAcumlteCount(int acumlteCount) {
        this.acumlteCount = acumlteCount;
    }

    public float getMaxDistDecayRatio() {
        return this.maxDistDecayRatio;
    }

    public void setMaxDistDecayRatio(float maxDistDecayRatio) {
        this.maxDistDecayRatio = maxDistDecayRatio;
    }

    public float getTotalShatterRatio() {
        return this.totalShatterRatio;
    }

    public void setTotalShatterRatio(float totalShatterRatio) {
        this.totalShatterRatio = totalShatterRatio;
    }

    public float getMinUnkwnRatio() {
        return this.minUnkwnRatio;
    }

    public void setMinUnkwnRatio(float minUnkwnRatio) {
        this.minUnkwnRatio = minUnkwnRatio;
    }

    public int getTestDataCnt() {
        return this.testDataCnt;
    }

    public void setTestDataCnt(int testDataCnt) {
        this.testDataCnt = testDataCnt;
    }

    public float getMaxShatterRatio() {
        return this.maxShatterRatio;
    }

    public void setMaxShatterRatio(float maxShatterRatio) {
        this.maxShatterRatio = maxShatterRatio;
    }

    public int getServingWiFiMAC() {
        return this.servingWiFiMAC;
    }

    public void setServingWiFiMAC(int servingWiFiMAC) {
        this.servingWiFiMAC = servingWiFiMAC;
    }

    public int getServingWiFiRSSI() {
        return this.servingWiFiRSSI;
    }

    public void setServingWiFiRSSI(int servingWiFiRSSI) {
        this.servingWiFiRSSI = servingWiFiRSSI;
    }

    public int getServingWiFiLinkSpeed() {
        return this.servingWiFiLinkSpeed;
    }

    public void setServingWiFiLinkSpeed(int servingWiFiLinkSpeed) {
        this.servingWiFiLinkSpeed = servingWiFiLinkSpeed;
    }

    public int getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getScanWifiStart() {
        return this.scanWifiStart;
    }

    public void setScanWifiStart(int scanWifiStart) {
        this.scanWifiStart = scanWifiStart;
    }

    public int getScanSSID() {
        return this.scanSSID;
    }

    public void setScanSSID(int scanSSID) {
        this.scanSSID = scanSSID;
    }

    public int getScanMAC() {
        return this.scanMAC;
    }

    public void setScanMAC(int scanMAC) {
        this.scanMAC = scanMAC;
    }

    public int getScanRSSI() {
        return this.scanRSSI;
    }

    public void setScanRSSI(int scanRSSI) {
        this.scanRSSI = scanRSSI;
    }

    public int getScanCH() {
        return this.scanCH;
    }

    public void setScanCH(int scanCH) {
        this.scanCH = scanCH;
    }

    public int getWifiDataSample() {
        return this.WIFI_DATA_SAMPLE;
    }

    public void setWifiDataSample(int wifiDataSample) {
        this.WIFI_DATA_SAMPLE = wifiDataSample;
    }

    public boolean isMainAp() {
        return this.isMainAp;
    }

    public void setMainAp(boolean mainAp) {
        this.isMainAp = mainAp;
    }

    public int getLowerBound() {
        return this.lowerBound;
    }

    public void setLowerBound(int lowerBound) {
        this.lowerBound = lowerBound;
    }

    public int getReprMinOcc() {
        return this.reprMinOcc;
    }

    public void setReprMinOcc(int reprMinOcc) {
        this.reprMinOcc = reprMinOcc;
    }

    public long getWriteFileSample() {
        return this.writeFileSample;
    }

    public void setWriteFileSample(long writeFileSample) {
        this.writeFileSample = writeFileSample;
    }

    public int getBatchInterval() {
        return this.batchInterval;
    }

    public void setBatchInterval(int batchInterval) {
        this.batchInterval = batchInterval;
    }

    public int getBatchID() {
        return this.batchID;
    }

    public void setBatchID(int batchID) {
        this.batchID = batchID;
    }

    public int getLabelID() {
        return this.labelID;
    }

    public void setLabelID(int labelID) {
        this.labelID = labelID;
    }

    public int getTimestampID() {
        return this.timestampID;
    }

    public void setTimestampID(int timestampID) {
        this.timestampID = timestampID;
    }

    public int getLinkSpeedID() {
        return this.linkSpeedID;
    }

    public void setLinkSpeedID(int linkSpeedID) {
        this.linkSpeedID = linkSpeedID;
    }

    public int getBssidStart() {
        return this.bssidStart;
    }

    public void setBssidStart(int bssidStart) {
        this.bssidStart = bssidStart;
    }

    public int getCellIDStart() {
        return this.cellIDStart;
    }

    public void setCellIDStart(int cellIDStart) {
        this.cellIDStart = cellIDStart;
    }

    public int getDeleteFlag() {
        return this.deleteFlag;
    }

    public void setDeleteFlag(int deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public float getMinNonzeroPerct() {
        return this.minNonzeroPerct;
    }

    public void setMinNonzeroPerct(float minNonzeroPerct) {
        this.minNonzeroPerct = minNonzeroPerct;
    }

    public float getMinDistinctValue() {
        return this.minDistinctValue;
    }

    public void setMinDistinctValue(float minDistinctValue) {
        this.minDistinctValue = minDistinctValue;
    }

    public float getMinStdev() {
        return this.minStdev;
    }

    public void setMinStdev(float minStdev) {
        this.minStdev = minStdev;
    }

    public int getThreshold() {
        return this.threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getMinBssidCount() {
        return this.minBssidCount;
    }

    public void setMinBssidCount(int minBssidCount) {
        this.minBssidCount = minBssidCount;
    }

    public int getMinMeanRssi() {
        return this.minMeanRssi;
    }

    public void setMinMeanRssi(int minMeanRssi) {
        this.minMeanRssi = minMeanRssi;
    }

    public int getnClusters() {
        return this.nClusters;
    }

    public void setnClusters(int nClusters) {
        this.nClusters = nClusters;
    }

    public float getMaxDist() {
        return this.maxDist;
    }

    public void setMaxDist(float maxDist) {
        this.maxDist = maxDist;
    }

    public float getMinSampleRatio() {
        return this.minSampleRatio;
    }

    public void setMinSampleRatio(float minSampleRatio) {
        this.minSampleRatio = minSampleRatio;
    }

    public float getWeightParam() {
        return this.weightParam;
    }

    public void setWeightParam(float weightParam) {
        this.weightParam = weightParam;
    }

    public float getShareRatioParam() {
        return this.shareRatioParam;
    }

    public void setShareRatioParam(float shareRatioParam) {
        this.shareRatioParam = shareRatioParam;
    }

    public int getNeighborNum() {
        return this.neighborNum;
    }

    public void setNeighborNum(int neighborNum) {
        this.neighborNum = neighborNum;
    }

    public int getKnnMaxDist() {
        return this.knnMaxDist;
    }

    public void setKnnMaxDist(int knnMaxDist) {
        this.knnMaxDist = knnMaxDist;
    }

    public float getKnnShareMacRatio() {
        return this.knnShareMacRatio;
    }

    public void setKnnShareMacRatio(float knnShareMacRatio) {
        this.knnShareMacRatio = knnShareMacRatio;
    }

    public int getMobileApCheckLimit() {
        return this.mobileApCheckLimit;
    }

    public void setMobileApCheckLimit(int mobileApCheckLimit) {
        this.mobileApCheckLimit = mobileApCheckLimit;
    }

    public int getUserPrefStartTimes() {
        return this.startTimes;
    }

    public void setUserPrefStartTimes(int times) {
        this.startTimes = times;
    }

    public long getUserPrefStartDuration() {
        return this.startDuration;
    }

    public void setUserPrefStartDuration(long duration) {
        this.startDuration = 1000 * duration;
    }

    public float getUserPrefFreqRatio() {
        return this.prefFreqRatio;
    }

    public void setUserPrefFreqRatio(float prefFreqRatio) {
        this.prefFreqRatio = prefFreqRatio;
    }

    public double getUserPrefDurationRatio() {
        return this.prefDurationRatio;
    }

    public void setUserPrefDurationRatio(double prefDurationRatio) {
        this.prefDurationRatio = prefDurationRatio;
    }

    public int getPowerSaveType() {
        return this.powerSaveType;
    }

    public void setPowerSaveType(int powerSaveType) {
        this.powerSaveType = powerSaveType;
    }

    public double getPowerSaveGap() {
        return this.powerSaveGap;
    }

    public void setPowerSaveGap(double powerSaveGap) {
        this.powerSaveGap = powerSaveGap;
    }

    public int getMaxBssidNum() {
        return this.maxBssidNum;
    }

    public void setMaxBssidNum(int maxBssidNum) {
        this.maxBssidNum = maxBssidNum;
    }

    public String[] toLineStr() {
        ArrayList<String> arrParams = new ArrayList();
        DecimalFormat df = new DecimalFormat("####.##");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fg_fingers_num=");
        stringBuilder.append(this.fg_batch_num);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("activeSample=");
        stringBuilder.append(this.activeSample);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("batchID=");
        stringBuilder.append(this.batchID);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("labelID=");
        stringBuilder.append(this.labelID);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("timestampID=");
        stringBuilder.append(this.timestampID);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("linkSpeedID=");
        stringBuilder.append(this.linkSpeedID);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("bssidStart=");
        stringBuilder.append(this.bssidStart);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("cellIDStart=");
        stringBuilder.append(this.cellIDStart);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("deleteFlag=");
        stringBuilder.append(this.deleteFlag);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("minNonzeroPerct=");
        stringBuilder.append(df.format((double) this.minNonzeroPerct));
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("minDistinctValue=");
        stringBuilder.append(df.format((double) this.minDistinctValue));
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("threshold=");
        stringBuilder.append(this.threshold);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("minBssidCount=");
        stringBuilder.append(this.minBssidCount);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("minMeanRssi=");
        stringBuilder.append(this.minMeanRssi);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("nClusters=");
        stringBuilder.append(this.nClusters);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("maxDist=");
        stringBuilder.append(this.maxDist);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("minSampleRatio=");
        stringBuilder.append(df.format((double) this.minSampleRatio));
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("weightParam=");
        stringBuilder.append(df.format((double) this.weightParam));
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("shareRatioParam=");
        stringBuilder.append(df.format((double) this.shareRatioParam));
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("minStdev=");
        stringBuilder.append(df.format((double) this.minStdev));
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("knnMaxDist=");
        stringBuilder.append(this.knnMaxDist);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("knnShareMacRatio=");
        stringBuilder.append(df.format((double) this.knnShareMacRatio));
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("lowerBound=");
        stringBuilder.append(this.lowerBound);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("reprMinOcc=");
        stringBuilder.append(this.reprMinOcc);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("isMainAp=");
        stringBuilder.append(this.isMainAp);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("connectedStrength=");
        stringBuilder.append(this.connectedStrength);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("minFreq=");
        stringBuilder.append(this.minFreq);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("maxBssidNum=");
        stringBuilder.append(this.maxBssidNum);
        arrParams.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("ParameterInfo:");
        stringBuilder.append(arrParams.size());
        LogUtil.i(stringBuilder.toString());
        return (String[]) arrParams.toArray(new String[arrParams.size()]);
    }

    public int getActiveSample() {
        return this.activeSample;
    }

    public void setActiveSample(int activeSample) {
        this.activeSample = activeSample;
    }

    public int getFg_batch_num() {
        return this.fg_batch_num;
    }

    public void setFg_batch_num(int fg_batch_num) {
        this.fg_batch_num = fg_batch_num;
    }

    public void setActScanLimit(int BgScanCnt, int StallScanCnt, int TotalScanCnt, int Interval) {
        this.limitBgScanCnt = BgScanCnt;
        this.limitStallScanCnt = StallScanCnt;
        this.limitTotalScanCnt = TotalScanCnt;
        this.limitScanIntervalTime = Interval;
    }

    public int getActScanLimit_bg() {
        return this.limitBgScanCnt;
    }

    public int getActScanLimit_stall() {
        return this.limitStallScanCnt;
    }

    public int getActScanLimit_total() {
        return this.limitTotalScanCnt;
    }

    public int getActScanLimit_interval() {
        return this.limitScanIntervalTime;
    }

    public void setActScanPeriods(int[] bgPeriods, int[] stallPeriods, int[] out4gPeriods) {
        this.paraPeriodBgScan = (int[]) bgPeriods.clone();
        this.paraPeriodStallScan = (int[]) stallPeriods.clone();
        this.paraPeriodOut4gScan = (int[]) out4gPeriods.clone();
    }

    public int[] getActBgScanPeriods() {
        return (int[]) this.paraPeriodBgScan.clone();
    }

    public int[] getActStallScanPeriods() {
        return (int[]) this.paraPeriodStallScan.clone();
    }

    public int[] getActOut4gScanPeriods() {
        return (int[]) this.paraPeriodOut4gScan.clone();
    }

    public int getAppTH_duration_min() {
        return this.appTH_Duration_min;
    }

    public int getAppTH_poorCnt_min() {
        return this.appTH_PoorCnt_min;
    }

    public int getAppTH_goodCnt_min() {
        return this.appTH_GoodCnt_min;
    }

    public float getAppTH_Target_Ration_margin() {
        return this.appTH_Target_Ration_margin;
    }

    public void setAppTH_duration_min(int duration) {
        this.appTH_Duration_min = duration;
    }

    public void setAppTH_poorCnt_min(int count) {
        this.appTH_PoorCnt_min = count;
    }

    public void setAppTH_goodCnt_min(int count) {
        this.appTH_GoodCnt_min = count;
    }

    public void setAppTH_Target_Ration_margin(float margin) {
        this.appTH_Target_Ration_margin = margin;
    }

    public int getBack4GTH_out4G_interval() {
        return this.back4GTH_out4G_interval;
    }

    public int getBack4GTH_duration_min() {
        return this.back4GTH_duration_min;
    }

    public int getBack4GTH_signal_min() {
        return this.back4GTH_signal_min;
    }

    public double getBack4GTH_duration_4gRatio() {
        return this.back4GTH_duration_4gRatio;
    }

    public void setBack4GTH_out4G_interval(int interval) {
        this.back4GTH_out4G_interval = interval;
    }

    public void setBack4GTH_duration_min(int duration) {
        this.back4GTH_duration_min = duration;
    }

    public void setBack4GTH_signal_min(int signal) {
        this.back4GTH_signal_min = signal;
    }

    public void setBack4GTH_duration_4gRatio(double ratio) {
        this.back4GTH_duration_4gRatio = ratio;
    }

    public boolean getBack4GEnabled() {
        if (chipPlatformValue.startsWith("kirin95") || chipPlatformValue.startsWith("kirin96") || chipPlatformValue.startsWith("kirin97") || chipPlatformValue.startsWith("kirin6") || chipPlatformValue.startsWith("hi") || chipPlatformValue.startsWith("sms")) {
            return false;
        }
        return this.back4GEnabled;
    }

    public boolean getUserPreferEnabled() {
        return this.userPreferEnabled;
    }

    public boolean getPowerPreferEnabled() {
        return this.powerPreferEnabled;
    }

    public void setBack4GEnable(boolean enabled) {
        this.back4GEnabled = enabled;
    }

    public void setUserPreferEnable(boolean enabled) {
        this.userPreferEnabled = enabled;
    }

    public void setPowerPreferEnable(boolean enabled) {
        this.powerPreferEnabled = enabled;
    }

    public int getReGetPsRegStatus() {
        return this.reGetPsRegStatus;
    }

    public String getChipPlatformValue() {
        return chipPlatformValue;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ParameterInfo{config_ver=");
        stringBuilder.append(this.config_ver);
        stringBuilder.append(", chipPlatformValue=");
        stringBuilder.append(chipPlatformValue);
        stringBuilder.append(", fg_fingers_num=");
        stringBuilder.append(this.fg_batch_num);
        stringBuilder.append(", activeSample=");
        stringBuilder.append(this.activeSample);
        stringBuilder.append(", batchInterval=");
        stringBuilder.append(this.batchInterval);
        stringBuilder.append(", deleteFlag=");
        stringBuilder.append(this.deleteFlag);
        stringBuilder.append(", minNonzeroPerct=");
        stringBuilder.append(this.minNonzeroPerct);
        stringBuilder.append(", minDistinctValue=");
        stringBuilder.append(this.minDistinctValue);
        stringBuilder.append(", minStdev=");
        stringBuilder.append(this.minStdev);
        stringBuilder.append(", threshold=");
        stringBuilder.append(this.threshold);
        stringBuilder.append(", minBssidCount=");
        stringBuilder.append(this.minBssidCount);
        stringBuilder.append(", minMeanRssi=");
        stringBuilder.append(this.minMeanRssi);
        stringBuilder.append(", nClusters=");
        stringBuilder.append(this.nClusters);
        stringBuilder.append(", maxDistBak=");
        stringBuilder.append(this.maxDistBak);
        stringBuilder.append(", maxDist=");
        stringBuilder.append(this.maxDist);
        stringBuilder.append(", maxDistMinLimit=");
        stringBuilder.append(this.maxDistMinLimit);
        stringBuilder.append(", trainDatasSize=");
        stringBuilder.append(this.trainDatasSize);
        stringBuilder.append(", minSampleRatio=");
        stringBuilder.append(this.minSampleRatio);
        stringBuilder.append(", weightParam=");
        stringBuilder.append(this.weightParam);
        stringBuilder.append(", shareRatioParam=");
        stringBuilder.append(this.shareRatioParam);
        stringBuilder.append(", neighborNum=");
        stringBuilder.append(this.neighborNum);
        stringBuilder.append(", knnMaxDist=");
        stringBuilder.append(this.knnMaxDist);
        stringBuilder.append(", knnShareMacRatio=");
        stringBuilder.append(this.knnShareMacRatio);
        stringBuilder.append(", writeFileSample=");
        stringBuilder.append(this.writeFileSample);
        stringBuilder.append(", lowerBound=");
        stringBuilder.append(this.lowerBound);
        stringBuilder.append(", reprMinOcc=");
        stringBuilder.append(this.reprMinOcc);
        stringBuilder.append(", isMainAp=");
        stringBuilder.append(this.isMainAp);
        stringBuilder.append(", paraPeriodBgScan=");
        stringBuilder.append(Arrays.toString(this.paraPeriodBgScan));
        stringBuilder.append(", paraPeriodStallScan=");
        stringBuilder.append(Arrays.toString(this.paraPeriodStallScan));
        stringBuilder.append(", limitBgScanCnt=");
        stringBuilder.append(this.limitBgScanCnt);
        stringBuilder.append(", limitStallScanCnt=");
        stringBuilder.append(this.limitStallScanCnt);
        stringBuilder.append(", limitTotalScanCnt=");
        stringBuilder.append(this.limitTotalScanCnt);
        stringBuilder.append(", limitScanIntervalTime=");
        stringBuilder.append(this.limitScanIntervalTime);
        stringBuilder.append(", WIFI_DATA_SAMPLE=");
        stringBuilder.append(this.WIFI_DATA_SAMPLE);
        stringBuilder.append(", mobileApCheckLimit=");
        stringBuilder.append(this.mobileApCheckLimit);
        stringBuilder.append(", servingWiFiRSSI=");
        stringBuilder.append(this.servingWiFiRSSI);
        stringBuilder.append(", forceTesting='");
        stringBuilder.append(this.forceTesting);
        stringBuilder.append('\'');
        stringBuilder.append(", startTimes=");
        stringBuilder.append(this.startTimes);
        stringBuilder.append(", startDuration=");
        stringBuilder.append(this.startDuration);
        stringBuilder.append(", appTH_Duration_min=");
        stringBuilder.append(this.appTH_Duration_min);
        stringBuilder.append(", appTH_PoorCnt_min=");
        stringBuilder.append(this.appTH_PoorCnt_min);
        stringBuilder.append(", appTH_GoodCnt_min=");
        stringBuilder.append(this.appTH_GoodCnt_min);
        stringBuilder.append(", appTH_Target_Ration_margin=");
        stringBuilder.append(this.appTH_Target_Ration_margin);
        stringBuilder.append(", minUnkwnRatio=");
        stringBuilder.append(this.minUnkwnRatio);
        stringBuilder.append(", testDataCnt=");
        stringBuilder.append(this.testDataCnt);
        stringBuilder.append(", maxShatterRatio=");
        stringBuilder.append(this.maxShatterRatio);
        stringBuilder.append(", totalShatterRatio=");
        stringBuilder.append(this.totalShatterRatio);
        stringBuilder.append(", maxDistDecayRatio=");
        stringBuilder.append(this.maxDistDecayRatio);
        stringBuilder.append(", minMainAPOccUpd=");
        stringBuilder.append(this.minMainAPOccUpd);
        stringBuilder.append(", acumlteCount=");
        stringBuilder.append(this.acumlteCount);
        stringBuilder.append(", minUkwnRatioUpd=");
        stringBuilder.append(this.minUkwnRatioUpd);
        stringBuilder.append(", minTrainBssiLstLenUpd=");
        stringBuilder.append(this.minTrainBssiLstLenUpd);
        stringBuilder.append(", abnMacRatioAllowUpd=");
        stringBuilder.append(this.abnMacRatioAllowUpd);
        stringBuilder.append(", checkAgingAcumlteCount=");
        stringBuilder.append(this.checkAgingAcumlteCount);
        stringBuilder.append(", minModelTypes=");
        stringBuilder.append(this.minModelTypes);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
