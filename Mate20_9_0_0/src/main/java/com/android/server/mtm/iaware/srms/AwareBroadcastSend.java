package com.android.server.mtm.iaware.srms;

import android.app.mtm.iaware.appmng.AppMngConstant.AppMngFeature;
import android.content.Intent;
import android.hardware.health.V1_0.HealthInfo;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.rms.iaware.AwareLog;
import com.android.server.am.HwActivityManagerService;
import com.android.server.mtm.iaware.appmng.DecisionMaker;
import com.android.server.pfw.autostartup.comm.XmlConst.PreciseIgnore;
import com.android.server.rms.iaware.srms.BroadcastExFeature;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class AwareBroadcastSend {
    private static final int BATTERY_BR_DATA_COUNT = 14;
    private static final int BATTERY_CONFIG_ITEM_COUNT = 5;
    private static final String TAG = "AwareBroadcastSend brsend";
    private static final String TAG_MISC_BATTERY_CONFIG = "brsend_config_battery_changed";
    private static final String TAG_MISC_WIFI_SC_CONFIG = "brsend_config_wifi_state_change";
    private static AwareBroadcastSend mBroadcastSend = null;
    private int mBatteryNormalTempHigh;
    private int mBatteryNormalTempLow;
    private final Object mBatteryStatLock;
    private boolean mBrSendControlDynamicSwitch;
    private final Object mConfigLock;
    private ArrayList<String> mControlledBrs;
    private int mCountAbnormalTemp;
    private int mCountBatteryBrSkip;
    private int mCountBatteryBrTotal;
    private int mCountMainFactorChange;
    private int mCountMaxCVBigChange;
    private int mCountNormalTempBigChange;
    private int mCountVoltageBigChange;
    private int mCountWifiStateChangeSkip;
    private int mCountWifiStateChangeTotal;
    private HealthInfo mHealthInfo;
    private HwActivityManagerService mHwAMS;
    private int mInvalidCharger;
    private boolean mIsBatteryDataSuccessUpdated;
    private boolean mIsWifiSCDataSuccessUpdated;
    private int mLastBatteryHealth;
    private int mLastBatteryLevel;
    private boolean mLastBatteryPresent;
    private int mLastBatteryStatus;
    private int mLastBatteryTemperature;
    private int mLastBatteryVoltage;
    private int mLastChargeCounter;
    private int mLastInvalidCharger;
    private int mLastMaxChargingCurrent;
    private int mLastMaxChargingVoltage;
    private NetworkInfo mLastNetworkInfo;
    private int mLastPlugType;
    private int mMaxChargingVolChangeLowestStep;
    private NetworkInfo mNetworkInfo;
    private int mPlugType;
    private boolean mSkipAuthenticating;
    private int mTempChangeLowestStep;
    private int mVolChangeLowestStep;
    private final Object mWifiStatLock;

    public static synchronized AwareBroadcastSend getInstance() {
        AwareBroadcastSend awareBroadcastSend;
        synchronized (AwareBroadcastSend.class) {
            if (mBroadcastSend == null) {
                mBroadcastSend = new AwareBroadcastSend();
                mBroadcastSend.updateConfigData();
            }
            awareBroadcastSend = mBroadcastSend;
        }
        return awareBroadcastSend;
    }

    private AwareBroadcastSend() {
        this.mHwAMS = null;
        this.mConfigLock = new Object();
        this.mBrSendControlDynamicSwitch = true;
        this.mControlledBrs = null;
        this.mBatteryStatLock = new Object();
        this.mCountBatteryBrTotal = 0;
        this.mCountBatteryBrSkip = 0;
        this.mCountMainFactorChange = 0;
        this.mCountVoltageBigChange = 0;
        this.mCountMaxCVBigChange = 0;
        this.mCountAbnormalTemp = 0;
        this.mCountNormalTempBigChange = 0;
        this.mIsBatteryDataSuccessUpdated = false;
        this.mBatteryNormalTempLow = 150;
        this.mBatteryNormalTempHigh = 400;
        this.mTempChangeLowestStep = 20;
        this.mVolChangeLowestStep = 50;
        this.mMaxChargingVolChangeLowestStep = 50000;
        this.mWifiStatLock = new Object();
        this.mNetworkInfo = null;
        this.mLastNetworkInfo = null;
        this.mCountWifiStateChangeTotal = 0;
        this.mCountWifiStateChangeSkip = 0;
        this.mIsWifiSCDataSuccessUpdated = false;
        this.mSkipAuthenticating = true;
        this.mHwAMS = HwActivityManagerService.self();
    }

    public void updateConfigData() {
        if (this.mHwAMS == null) {
            AwareLog.e(TAG, "failed to get HwAMS");
            return;
        }
        DecisionMaker.getInstance().updateRule(AppMngFeature.BROADCAST, this.mHwAMS.getUiContext());
        synchronized (this.mConfigLock) {
            this.mControlledBrs = DecisionMaker.getInstance().getRawConfig(AppMngFeature.BROADCAST.getDesc(), BroadcastExFeature.BR_SEND_SWITCH);
            ArrayList<String> batteryConfigs = DecisionMaker.getInstance().getRawConfig(AppMngFeature.BROADCAST.getDesc(), TAG_MISC_BATTERY_CONFIG);
            if (batteryConfigs != null) {
                updateBatteryConfigData(batteryConfigs);
            }
            ArrayList<String> wifiSCConfigs = DecisionMaker.getInstance().getRawConfig(AppMngFeature.BROADCAST.getDesc(), TAG_MISC_WIFI_SC_CONFIG);
            if (wifiSCConfigs != null) {
                updateWifiSCConfigData(wifiSCConfigs);
            }
        }
    }

    private void updateBatteryConfigData(ArrayList<String> configs) {
        if (configs.size() == 5) {
            String cf0 = (String) configs.get(0);
            String cf1 = (String) configs.get(1);
            String cf2 = (String) configs.get(2);
            String cf3 = (String) configs.get(3);
            String cf4 = (String) configs.get(4);
            if (cf0 != null && cf1 != null && cf2 != null && cf3 != null && cf4 != null) {
                try {
                    int tempLow = Integer.parseInt(cf0.trim());
                    int tempHigh = Integer.parseInt(cf1.trim());
                    int tempStep = Integer.parseInt(cf2.trim());
                    int volStep = Integer.parseInt(cf3.trim());
                    int maxCVStep = Integer.parseInt(cf4.trim());
                    this.mBatteryNormalTempLow = tempLow;
                    this.mBatteryNormalTempHigh = tempHigh;
                    this.mTempChangeLowestStep = tempStep;
                    this.mVolChangeLowestStep = volStep;
                    this.mMaxChargingVolChangeLowestStep = maxCVStep;
                } catch (NumberFormatException e) {
                    AwareLog.e(TAG, "invalid battery config");
                }
            }
        }
    }

    private void updateWifiSCConfigData(ArrayList<String> configs) {
        if (configs.size() != 0) {
            String cf0 = (String) configs.get(0);
            if (cf0 != null) {
                try {
                    int tempValue = Integer.parseInt(cf0.trim());
                    if (tempValue == 0) {
                        this.mSkipAuthenticating = false;
                    } else if (tempValue == 1) {
                        this.mSkipAuthenticating = true;
                    }
                } catch (NumberFormatException e) {
                    AwareLog.e(TAG, "invalid wifi.STATE_CHANGE config");
                }
            }
        }
    }

    public boolean setData(String action, Object[] data) {
        if (action == null || data == null) {
            return false;
        }
        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
            setBatteryBrData(data);
            return true;
        } else if (!action.equals("android.net.wifi.STATE_CHANGE")) {
            return false;
        } else {
            setWifiStateChangeData(data);
            return true;
        }
    }

    private void setBatteryBrData(Object[] data) {
        if (data.length == 14) {
            this.mIsBatteryDataSuccessUpdated = false;
            try {
                this.mHealthInfo = (HealthInfo) data[0];
                if (this.mHealthInfo != null) {
                    this.mLastBatteryStatus = ((Integer) data[1]).intValue();
                    this.mLastBatteryHealth = ((Integer) data[2]).intValue();
                    this.mLastBatteryPresent = ((Boolean) data[3]).booleanValue();
                    this.mLastBatteryLevel = ((Integer) data[4]).intValue();
                    this.mPlugType = ((Integer) data[5]).intValue();
                    this.mLastPlugType = ((Integer) data[6]).intValue();
                    this.mLastBatteryVoltage = ((Integer) data[7]).intValue();
                    this.mLastBatteryTemperature = ((Integer) data[8]).intValue();
                    this.mLastMaxChargingCurrent = ((Integer) data[9]).intValue();
                    this.mLastMaxChargingVoltage = ((Integer) data[10]).intValue();
                    this.mLastChargeCounter = ((Integer) data[11]).intValue();
                    this.mInvalidCharger = ((Integer) data[12]).intValue();
                    this.mLastInvalidCharger = ((Integer) data[13]).intValue();
                    this.mIsBatteryDataSuccessUpdated = true;
                }
            } catch (ClassCastException e) {
                if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.e(TAG, "invalid battery data");
                }
            }
        }
    }

    private void setWifiStateChangeData(Object[] data) {
        if (data.length >= 1) {
            this.mIsWifiSCDataSuccessUpdated = false;
            try {
                Intent intent = data[0];
                if (intent != null) {
                    this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    this.mIsWifiSCDataSuccessUpdated = true;
                }
            } catch (ClassCastException e) {
                if (AwareBroadcastDebug.getDebugDetail()) {
                    AwareLog.e(TAG, "invalid wifi.STATE_CHANGE data");
                }
            }
        }
    }

    public boolean needSkipBroadcastSend(String action) {
        if (action == null || !this.mBrSendControlDynamicSwitch || !isBrControlled(action)) {
            return false;
        }
        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
            return needSkipBatteryBrSend();
        }
        if (action.equals("android.net.wifi.STATE_CHANGE")) {
            return needSkipWifiStateChangeBrSend();
        }
        return false;
    }

    private boolean isBrControlled(String action) {
        synchronized (this.mConfigLock) {
            if (this.mControlledBrs != null) {
                boolean contains = this.mControlledBrs.contains(action);
                return contains;
            }
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:56:0x00fb, code skipped:
            if (com.android.server.mtm.iaware.srms.AwareBroadcastDebug.getDebugDetail() == false) goto L_0x0117;
     */
    /* JADX WARNING: Missing block: B:57:0x00fd, code skipped:
            r0 = TAG;
            r1 = new java.lang.StringBuilder();
            r1.append("battery br summary (skip,total,mainFactorChange,maxCVBigChange,volBigChange,abnormalTemp,normalTempBigChange): ");
            r1.append(getBatteryStatisticsData());
            android.rms.iaware.AwareLog.i(r0, r1.toString());
     */
    /* JADX WARNING: Missing block: B:58:0x0117, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean needSkipBatteryBrSend() {
        if (!this.mIsBatteryDataSuccessUpdated) {
            return false;
        }
        if (AwareBroadcastDebug.getDebugDetail()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("battery  new data: ");
            stringBuilder.append(batteryInfoToString(true));
            AwareLog.i(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("battery last data: ");
            stringBuilder.append(batteryInfoToString(false));
            AwareLog.i(str, stringBuilder.toString());
        }
        synchronized (this.mBatteryStatLock) {
            this.mCountBatteryBrTotal++;
            if (this.mHealthInfo.batteryStatus == this.mLastBatteryStatus && this.mHealthInfo.batteryHealth == this.mLastBatteryHealth && this.mHealthInfo.batteryPresent == this.mLastBatteryPresent && this.mHealthInfo.batteryLevel == this.mLastBatteryLevel && this.mPlugType == this.mLastPlugType && this.mHealthInfo.maxChargingCurrent == this.mLastMaxChargingCurrent) {
                if (this.mInvalidCharger == this.mLastInvalidCharger) {
                    if (this.mHealthInfo.maxChargingVoltage != this.mLastMaxChargingVoltage && Math.abs(this.mHealthInfo.maxChargingVoltage - this.mLastMaxChargingVoltage) >= this.mMaxChargingVolChangeLowestStep) {
                        this.mCountMaxCVBigChange++;
                        return false;
                    } else if (this.mHealthInfo.batteryVoltage == this.mLastBatteryVoltage || Math.abs(this.mHealthInfo.batteryVoltage - this.mLastBatteryVoltage) < this.mVolChangeLowestStep) {
                        if (this.mHealthInfo.batteryTemperature != this.mLastBatteryTemperature) {
                            if (this.mHealthInfo.batteryTemperature > this.mBatteryNormalTempLow) {
                                if (this.mHealthInfo.batteryTemperature < this.mBatteryNormalTempHigh) {
                                    if (Math.abs(this.mHealthInfo.batteryTemperature - this.mLastBatteryTemperature) >= this.mTempChangeLowestStep) {
                                        this.mCountNormalTempBigChange++;
                                        return false;
                                    }
                                }
                            }
                            this.mCountAbnormalTemp++;
                            return false;
                        }
                        this.mCountBatteryBrSkip++;
                    } else {
                        this.mCountVoltageBigChange++;
                        return false;
                    }
                }
            }
            this.mCountMainFactorChange++;
            return false;
        }
    }

    private boolean needSkipWifiStateChangeBrSend() {
        if (!this.mIsWifiSCDataSuccessUpdated) {
            return false;
        }
        String str;
        StringBuilder stringBuilder;
        if (AwareBroadcastDebug.getDebugDetail()) {
            String dState = this.mNetworkInfo == null ? "Null" : this.mNetworkInfo.getDetailedState().toString();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("wifi.STATE_CHANGE detailedState: ");
            stringBuilder.append(dState);
            AwareLog.i(str, stringBuilder.toString());
        }
        boolean bSkip = false;
        synchronized (this.mWifiStatLock) {
            this.mCountWifiStateChangeTotal++;
            if (this.mNetworkInfo != null) {
                DetailedState detailedState = this.mNetworkInfo.getDetailedState();
                if (DetailedState.CONNECTED.equals(detailedState)) {
                    bSkip = false;
                } else if (DetailedState.AUTHENTICATING.equals(detailedState) && this.mSkipAuthenticating) {
                    if (AwareBroadcastDebug.getDebugDetail()) {
                        AwareLog.i(TAG, "Skip broadcast wifi.STATE_CHANGE, reason: detailedState is AUTHENTICATING");
                    }
                    this.mCountWifiStateChangeSkip++;
                    bSkip = true;
                } else if (this.mLastNetworkInfo == null) {
                    bSkip = false;
                } else if (this.mLastNetworkInfo.getDetailedState() == detailedState) {
                    if (AwareBroadcastDebug.getDebugDetail()) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Skip broadcast wifi.STATE_CHANGE, reason: detailedState same as previous (");
                        stringBuilder2.append(detailedState);
                        stringBuilder2.append(")");
                        AwareLog.i(str2, stringBuilder2.toString());
                    }
                    this.mCountWifiStateChangeSkip++;
                    bSkip = true;
                } else {
                    bSkip = false;
                }
            }
        }
        if (!bSkip) {
            this.mLastNetworkInfo = this.mNetworkInfo;
        }
        if (AwareBroadcastDebug.getDebugDetail()) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("wifi.STATE_CHANGE br summary (skip,total): ");
            stringBuilder.append(getWifiStateChangeDebugData());
            AwareLog.i(str, stringBuilder.toString());
        }
        return bSkip;
    }

    private String batteryInfoToString(boolean newData) {
        String info = "";
        StringBuilder stringBuilder;
        if (newData) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" voltage:");
            stringBuilder.append(this.mHealthInfo.batteryVoltage);
            stringBuilder.append(" temper:");
            stringBuilder.append(this.mHealthInfo.batteryTemperature);
            stringBuilder.append(" maxCV:");
            stringBuilder.append(this.mHealthInfo.maxChargingVoltage);
            stringBuilder.append(" maxCC:");
            stringBuilder.append(this.mHealthInfo.maxChargingCurrent);
            stringBuilder.append(" status:");
            stringBuilder.append(this.mHealthInfo.batteryStatus);
            stringBuilder.append(" health:");
            stringBuilder.append(this.mHealthInfo.batteryHealth);
            stringBuilder.append(" present:");
            stringBuilder.append(this.mHealthInfo.batteryPresent);
            stringBuilder.append(" level:");
            stringBuilder.append(this.mHealthInfo.batteryLevel);
            stringBuilder.append(" plug:");
            stringBuilder.append(this.mPlugType);
            stringBuilder.append(" invalidCharger:");
            stringBuilder.append(this.mInvalidCharger);
            stringBuilder.append(" chargeCntr:");
            stringBuilder.append(this.mHealthInfo.batteryChargeCounter);
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(" voltage:");
        stringBuilder.append(this.mLastBatteryVoltage);
        stringBuilder.append(" temper:");
        stringBuilder.append(this.mLastBatteryTemperature);
        stringBuilder.append(" maxCV:");
        stringBuilder.append(this.mLastMaxChargingVoltage);
        stringBuilder.append(" maxCC:");
        stringBuilder.append(this.mLastMaxChargingCurrent);
        stringBuilder.append(" status:");
        stringBuilder.append(this.mLastBatteryStatus);
        stringBuilder.append(" health:");
        stringBuilder.append(this.mLastBatteryHealth);
        stringBuilder.append(" present:");
        stringBuilder.append(this.mLastBatteryPresent);
        stringBuilder.append(" level:");
        stringBuilder.append(this.mLastBatteryLevel);
        stringBuilder.append(" plug:");
        stringBuilder.append(this.mLastPlugType);
        stringBuilder.append(" invalidCharger:");
        stringBuilder.append(this.mLastInvalidCharger);
        stringBuilder.append(" chargeCntr:");
        stringBuilder.append(this.mLastChargeCounter);
        return stringBuilder.toString();
    }

    public void changeSwitch(boolean switchValue) {
        this.mBrSendControlDynamicSwitch = switchValue;
    }

    public HashMap<String, String> getStatisticsData() {
        HashMap<String, String> data = new HashMap();
        data.put("android.intent.action.BATTERY_CHANGED", getBatteryStatisticsData());
        data.put("android.net.wifi.STATE_CHANGE", getWifiStateChangeStatisticsData());
        return data;
    }

    public void resetStatisticsData() {
        synchronized (this.mBatteryStatLock) {
            this.mCountBatteryBrSkip = 0;
            this.mCountBatteryBrTotal = 0;
            this.mCountMainFactorChange = 0;
            this.mCountMaxCVBigChange = 0;
            this.mCountVoltageBigChange = 0;
            this.mCountAbnormalTemp = 0;
            this.mCountNormalTempBigChange = 0;
        }
        synchronized (this.mWifiStatLock) {
            this.mCountWifiStateChangeSkip = 0;
            this.mCountWifiStateChangeTotal = 0;
        }
    }

    private String getBatteryStatisticsData() {
        String stringBuilder;
        synchronized (this.mBatteryStatLock) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.mCountBatteryBrSkip);
            stringBuilder2.append(",");
            stringBuilder2.append(this.mCountBatteryBrTotal);
            stringBuilder2.append(",");
            stringBuilder2.append(this.mCountMainFactorChange);
            stringBuilder2.append(",");
            stringBuilder2.append(this.mCountMaxCVBigChange);
            stringBuilder2.append(",");
            stringBuilder2.append(this.mCountVoltageBigChange);
            stringBuilder2.append(",");
            stringBuilder2.append(this.mCountAbnormalTemp);
            stringBuilder2.append(",");
            stringBuilder2.append(this.mCountNormalTempBigChange);
            stringBuilder = stringBuilder2.toString();
        }
        return stringBuilder;
    }

    private String getWifiStateChangeStatisticsData() {
        String stringBuilder;
        synchronized (this.mWifiStatLock) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.mCountWifiStateChangeSkip);
            stringBuilder2.append(",");
            stringBuilder2.append(this.mCountWifiStateChangeTotal);
            stringBuilder2.append(",0,0,0,0,0");
            stringBuilder = stringBuilder2.toString();
        }
        return stringBuilder;
    }

    private String getWifiStateChangeDebugData() {
        String stringBuilder;
        synchronized (this.mWifiStatLock) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.mCountWifiStateChangeSkip);
            stringBuilder2.append(",");
            stringBuilder2.append(this.mCountWifiStateChangeTotal);
            stringBuilder = stringBuilder2.toString();
        }
        return stringBuilder;
    }

    public void dumpBRSendInfo(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("battery br summary (skip,total,mainFactorChange,maxCVBigChange,volBigChange,abnormalTemp,normalTempBigChange): ");
        stringBuilder.append(getBatteryStatisticsData());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("wifi.STATE_CHANGE br summary (skip,total): ");
        stringBuilder.append(getWifiStateChangeDebugData());
        pw.println(stringBuilder.toString());
    }

    /* JADX WARNING: Missing block: B:18:0x0107, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void dumpBRSendConfig(PrintWriter pw) {
        updateConfigData();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BrSend feature enable: ");
        stringBuilder.append(BroadcastExFeature.isFeatureEnabled(2));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("switch dynamic status: ");
        stringBuilder.append(this.mBrSendControlDynamicSwitch ? PreciseIgnore.COMP_SCREEN_ON_VALUE_ : "off");
        pw.println(stringBuilder.toString());
        synchronized (this.mConfigLock) {
            if (this.mControlledBrs != null) {
                Iterator<String> iterator = this.mControlledBrs.iterator();
                if (iterator == null) {
                    return;
                }
                StringBuilder stringBuilder2;
                while (iterator.hasNext()) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Controlled Br: ");
                    stringBuilder2.append((String) iterator.next());
                    pw.println(stringBuilder2.toString());
                }
                pw.println("");
                pw.println("battery br send control configs:");
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("normal temp low threshold: ");
                stringBuilder2.append(this.mBatteryNormalTempLow);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("normal temp high threshold: ");
                stringBuilder2.append(this.mBatteryNormalTempHigh);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("normal temp change lowest step: ");
                stringBuilder2.append(this.mTempChangeLowestStep);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("voltage change lowest step: ");
                stringBuilder2.append(this.mVolChangeLowestStep);
                pw.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("max charging voltage change lowest step: ");
                stringBuilder2.append(this.mMaxChargingVolChangeLowestStep);
                pw.println(stringBuilder2.toString());
                pw.println("");
                pw.println("wifi.STATE_CHANGE configs:");
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Skip Authenticating: ");
                stringBuilder2.append(this.mSkipAuthenticating);
                pw.println(stringBuilder2.toString());
            }
        }
    }
}
