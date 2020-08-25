package com.android.server.hidata.mplink;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import com.android.server.hidata.arbitration.IHiDataCHRCallBack;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import huawei.android.net.hwmplink.MpLinkCommonUtils;
import java.util.HashMap;
import java.util.Map;

public class HwMpLinkChrImpl {
    private static final String BIND_COUNT_FIELD = "BINDCNT";
    private static final String BIND_FAIL_REASON_FIELD = "C2WRFISTOP";
    private static final String BRAIN_FAIL_FIELD = "BRAINFAIL";
    private static final String BRIDGE_FAIL_FIELD = "BRIDGEFAIL";
    private static final int BYTE_UNIT = 1024;
    private static final String CELL_CLOSE_FAIL_FIELD = "CELLCLOSEFAIL";
    private static final String CELL_REQUEST_FAIL_FIELD = "CELLROFAIL";
    private static final String CELL_SRC_FAIL_FIELD = "CELLNOSRCFAIL";
    private static final String CLOSE_SOCKET_FAIL_REASON_FIELD = "CWRFISTOP";
    private static final String COEXIST_MOBILE_CLOSE_COUNT_FIELD = "CWRFICNT";
    private static final String COEXIST_SUCCESS_COUNT_FIELD = "COXSUCCCNT";
    private static final String COEXIST_WIFI_CLOSE_COUNT_FIELD = "C2WRFICNT";
    private static final String COLON = ":";
    private static final String COMMA = ",";
    private static final int DEFAULT_CAPACITY = 16;
    public static final int EID_WIFI_MPLINK_INFO = 909009038;
    private static final int FAIL_REASON_CODE = -9999;
    private static final String LEFT_BRACKET = "(";
    private static final String LINE_BREAK = System.lineSeparator();
    private static final int MICRO_SECOND = 1000;
    private static final String MPLINK_DURATION_TIME_FIELD = "W2CRFIL1";
    private static final String REQUEST_BIND_FAIL_FIELD = "W2CGAP";
    private static final String REQUEST_CLOSE_SOCKET_FAIL_FIELD = "W2CRFIREJ";
    private static final String REQUEST_CLOSE_SOCKET_SUCCESS_FIELD = "W2CRFISTOP";
    private static final String REQUEST_COEXIST_COUNT_FIELD = "RECOXCNT";
    private static final String REQUEST_OPEN_FAIL_COUNT_FIELD = "COXFAILCNT";
    private static final String REQUEST_UNBIND_FAIL_COUNT_FIELD = "W2CRFICNT";
    private static final String REQUEST_UNCOEXIST_COUNT_FIELD = "REUNCOXCNT";
    private static final String RIGHT_BRACKET = ")";
    private static final String RIGHT_BRACKET_AND_COMMA = "),";
    private static final String ROUTE_CHANGE_COUNT_FIELD = "ROUTECHCNT";
    private static final int RSSI_LEVEL_0 = 0;
    private static final int RSSI_LEVEL_1 = 1;
    private static final int RSSI_LEVEL_2 = 2;
    private static final int RSSI_LEVEL_3 = 3;
    private static final int RSSI_LEVEL_4 = 4;
    private static final String SIM_FAIL_FIELD = "SIMERRORFAIL";
    private static final String SMART_DEVICE_COUNT_FIELD = "SMARTDECNT";
    private static final String TAG = "HiData_HwMpLinkChrImpl";
    private static final String TOTAL_DUALNETWORK_COUNT_FIELD = "WCRFIREJ";
    private static final String TOTAL_RX_FIELD = "TOTALRX";
    private static final String TOTAL_TX_FIELD = "TOTATX";
    private static final String UNBIND_COUNT_FIELD = "UNBINDCNT";
    private static final String UNBIND_FAIL_REASON_FIELD = "C2WRFIREJ";
    private static final String VPN_FAIL_FIELD = "VPNFAIL";
    private static final String WIFI_FREQ24_COUNT_FIELD = "CWRFIL2";
    private static final String WIFI_FREQ5_COUNT_FIELD = "CWRFIL3";
    private static final String WIFI_LEVEL0_COUNT_FIELD = "W2CRFIL2";
    private static final String WIFI_LEVEL1_COUNT_FIELD = "W2CRFIL3";
    private static final String WIFI_LEVEL2_COUNT_FIELD = "C2WRFIL1";
    private static final String WIFI_LEVEL3_COUNT_FIELD = "C2WRFIL2";
    private static final String WIFI_LEVEL4_COUNT_FIELD = "CWRFIL1";
    private static final String WIFI_PLUS_FAIL_FIELD = "WIFIPLUSFAIL";
    private static HwMpLinkChrImpl mHwMpLinkChrImpl = null;
    private int mAiDeviceOpenCnt = 0;
    private HashMap<String, Integer> mAiDeviceOpenMap;
    private long mCellResidentTime = 0;
    private int mCellToWifiInGameScene = 0;
    private int mCoexistMobileDataSwitchClosed = 0;
    private int mCoexistWifiSwitchClosed = 0;
    private int mDefaultRouteChangeCnt = 0;
    private int mInterDisturbCheckFailedTime = 0;
    private int mInterDisturbCheckTriggerTime = 0;
    private int mInterDisturbHappenedTime = 0;
    private int mLastBindFailReason = FAIL_REASON_CODE;
    private int mLastCloseSocketFailReason = FAIL_REASON_CODE;
    private int mLastUnBindFailReason = FAIL_REASON_CODE;
    private long mLteRequestTime = 0;
    private long mMobileDataConnectedStamp = 0;
    private String mMobileIface = "";
    private long mMobileRxBytesBase = 0;
    private long mMobileTxBytesBase = 0;
    private long mMpLinkOpenMobileDataStamp = 0;
    private long mMpLinkedDurationTime = 0;
    private long mMpLinkedStartTime = 0;
    private int mNoInterDisturbHappenedTime = 0;
    private int mPingPongTimesForGame = 0;
    private int mRequestBindFail = 0;
    private HashMap<String, Integer> mRequestBindFailMap;
    private int mRequestBindSucc = 0;
    private int mRequestCloseFail = 0;
    private HashMap<String, Integer> mRequestCloseFailMap;
    private int mRequestCloseSocketFail = 0;
    private HashMap<String, Integer> mRequestCloseSocketFailMap;
    private int mRequestCloseSocketSucc = 0;
    private int mRequestCloseSucc = 0;
    private int mRequestOpenFail = 0;
    private HashMap<String, Integer> mRequestOpenFailMap;
    private int mRequestOpenSucc = 0;
    private int mRequestUnBindFail = 0;
    private HashMap<String, Integer> mRequestUnBindFailMap;
    private int mRequestUnBindSucc = 0;
    private int mRssiLevel = 0;
    private int mTotalDualNetworkCnt = 0;
    private long mTotalRxBytes = 0;
    private long mTotalTxBytes = 0;
    private int mWiFiFreq24Cnt = 0;
    private int mWiFiFreq5Cnt = 0;
    private int mWiFiLevel0Cnt = 0;
    private int mWiFiLevel1Cnt = 0;
    private int mWiFiLevel2Cnt = 0;
    private int mWiFiLevel3Cnt = 0;
    private int mWiFiLevel4Cnt = 0;
    private long mWifiConnectedTimeStamp = 0;
    private int mWifiDetectBadCnt = 0;
    private WifiManager mWifiManager = null;
    private int mWifiToCellInGameScene = 0;

    public HwMpLinkChrImpl(Context context) {
        this.mWifiManager = (WifiManager) context.getSystemService(DevSchedFeatureRT.WIFI_FEATURE);
        this.mAiDeviceOpenMap = new HashMap<>(16);
        this.mRequestOpenFailMap = new HashMap<>(16);
        this.mRequestCloseFailMap = new HashMap<>(16);
        this.mRequestBindFailMap = new HashMap<>(16);
        this.mRequestUnBindFailMap = new HashMap<>(16);
        this.mRequestCloseSocketFailMap = new HashMap<>(16);
    }

    public static HwMpLinkChrImpl getInstance(Context context) {
        if (mHwMpLinkChrImpl == null) {
            mHwMpLinkChrImpl = new HwMpLinkChrImpl(context);
        }
        return mHwMpLinkChrImpl;
    }

    public void setMobileIface(String name) {
        MpLinkCommonUtils.logI(TAG, false, "setMobileIface:%{public}s", new Object[]{name});
        this.mMobileIface = name;
    }

    private void resetMobileStats() {
        this.mMobileTxBytesBase = 0;
        this.mMobileRxBytesBase = 0;
    }

    public void updateMobileStatsBase() {
        MpLinkCommonUtils.logI(TAG, false, "enter updateMobileStatsBase", new Object[0]);
        if (!TextUtils.isEmpty(this.mMobileIface) && !"".equals(this.mMobileIface)) {
            this.mMobileTxBytesBase = TrafficStats.getTxBytes(this.mMobileIface);
            this.mMobileRxBytesBase = TrafficStats.getRxBytes(this.mMobileIface);
            MpLinkCommonUtils.logI(TAG, false, "updateMobileStatsBase,Tx(%{public}s),Rx(%{public}s)", new Object[]{String.valueOf(this.mMobileTxBytesBase), String.valueOf(this.mMobileRxBytesBase)});
        }
    }

    public void updateMobileStats() {
        MpLinkCommonUtils.logI(TAG, false, "enter updateMobileStats", new Object[0]);
        if (!TextUtils.isEmpty(this.mMobileIface) && !"".equals(this.mMobileIface)) {
            long txByte = TrafficStats.getTxBytes(this.mMobileIface);
            long rxByte = TrafficStats.getRxBytes(this.mMobileIface);
            long deltaTxByte = (txByte - this.mMobileTxBytesBase) / 1024;
            long deltaRxByte = (rxByte - this.mMobileRxBytesBase) / 1024;
            MpLinkCommonUtils.logI(TAG, false, "updateMobileStats,txByte(%{public}s) - mMobileTxBytesBase(%{public}s) = deltaTxByte(%{public}s)", new Object[]{String.valueOf(txByte), String.valueOf(this.mMobileTxBytesBase), String.valueOf(deltaTxByte)});
            MpLinkCommonUtils.logI(TAG, false, "updateMobileStats,rxByte(%{public}s) - mMobileRxBytesBase(%{public}s) = deltaRxByte(%{public}s)", new Object[]{String.valueOf(rxByte), String.valueOf(this.mMobileRxBytesBase), String.valueOf(deltaRxByte)});
            updateTotalTxBytes(deltaTxByte);
            updateTotalRxBytes(deltaRxByte);
        }
        resetMobileStats();
    }

    public void updateCoexistWifiSwitchClosedCnt() {
        MpLinkCommonUtils.logI(TAG, false, "updateCoexistWifiSwitchClosedCnt", new Object[0]);
        this.mCoexistWifiSwitchClosed++;
    }

    public void updateCoexistMobileDataSwitchClosedCnt() {
        MpLinkCommonUtils.logI(TAG, false, "updateCoexistMobileDataSwitchClosedCnt", new Object[0]);
        this.mCoexistMobileDataSwitchClosed++;
    }

    public void updateAiDeviceOpenCnt(String apName) {
        MpLinkCommonUtils.logD(TAG, false, "updateAiDeviceOpenCnt", new Object[0]);
        this.mAiDeviceOpenCnt++;
    }

    public void updateDefaultRouteChangeCnt() {
        MpLinkCommonUtils.logI(TAG, false, "updateDefaultRouteChangeCnt", new Object[0]);
        this.mDefaultRouteChangeCnt++;
    }

    private void updateTotalTxBytes(long txByes) {
        MpLinkCommonUtils.logI(TAG, false, "updateTotalTxBytes,TxByes:%{public}s", new Object[]{String.valueOf(txByes)});
        this.mTotalTxBytes += txByes;
    }

    private void updateTotalRxBytes(long rxByes) {
        MpLinkCommonUtils.logI(TAG, false, "updateTotalRxBytes,RxByes:%{public}s", new Object[]{String.valueOf(rxByes)});
        this.mTotalRxBytes += rxByes;
    }

    public void updateOpenSuccCnt() {
        MpLinkCommonUtils.logI(TAG, false, "updateOpenSuccCnt", new Object[0]);
        this.mRequestOpenSucc++;
    }

    public void updateOpenFailCnt(int reason) {
        MpLinkCommonUtils.logI(TAG, false, "updateOpenFailCnt,reason:%{public}d", new Object[]{Integer.valueOf(reason)});
        this.mRequestOpenFail++;
        updateHashMap(String.valueOf(reason), this.mRequestOpenFailMap);
    }

    public void updateCloseSuccCnt() {
        MpLinkCommonUtils.logI(TAG, false, "updateCloseSuccCnt", new Object[0]);
        this.mRequestCloseSucc++;
    }

    public void updateCloseFailCnt(int reason) {
        MpLinkCommonUtils.logI(TAG, false, "updateCloseFailCnt,reason:%{public}d", new Object[]{Integer.valueOf(reason)});
        this.mRequestCloseFail++;
        updateHashMap(String.valueOf(reason), this.mRequestCloseFailMap);
    }

    public void updateBindSuccCnt() {
        MpLinkCommonUtils.logI(TAG, false, "updateBindSuccCnt", new Object[0]);
        this.mRequestBindSucc++;
    }

    public void updateBindFailCnt(int reason) {
        MpLinkCommonUtils.logI(TAG, false, "updateBindFailCnt,reason:%{public}d", new Object[]{Integer.valueOf(reason)});
        this.mRequestBindFail++;
        this.mLastBindFailReason = reason;
        updateHashMap(String.valueOf(reason), this.mRequestBindFailMap);
    }

    public void updateUnBindSuccCnt() {
        MpLinkCommonUtils.logI(TAG, false, "updateUnBindSuccCnt", new Object[0]);
        this.mRequestUnBindSucc++;
    }

    public void updateMpLinkCellBindState(boolean isBinded, String name) {
        if (isBinded) {
            this.mMpLinkedStartTime = System.currentTimeMillis();
            setMobileIface(name);
            updateMpLinkedStartTime();
            updateMobileStatsBase();
            updateWifiInfo(this.mWifiManager.getConnectionInfo());
        } else if (this.mMpLinkedStartTime != 0) {
            updateMpLinkedDurationTime();
            updateMobileStats();
            this.mMpLinkedStartTime = 0;
            MpLinkCommonUtils.logD(TAG, false, "TotalTxBytes: %{public}s KB, TotalRxBytes: %{public}s KB, duration:%{public}s s", new Object[]{String.valueOf(this.mTotalTxBytes), String.valueOf(this.mTotalRxBytes), String.valueOf(this.mMpLinkedDurationTime / 1000)});
        }
    }

    private void updateMpLinkedStartTime() {
        this.mMpLinkedStartTime = System.currentTimeMillis();
    }

    public void updateOpenMobileDataStamp() {
        this.mMpLinkOpenMobileDataStamp = System.currentTimeMillis();
    }

    public void updateWifiConnectedTimeStamp() {
        this.mWifiConnectedTimeStamp = SystemClock.elapsedRealtime();
    }

    public void updateInterDisturbCheckTriggerTime() {
        this.mInterDisturbCheckTriggerTime++;
        MpLinkCommonUtils.logD(TAG, false, "updateInterDisturbCheckTriggerTime, mInterDisturbCheckTriggerTime = %{public}d", new Object[]{Integer.valueOf(this.mInterDisturbCheckTriggerTime)});
    }

    public void updateInterDisturbCheckFailedTime() {
        this.mInterDisturbCheckFailedTime++;
        MpLinkCommonUtils.logD(TAG, false, "updateInterDisturbCheckFailedTime, mInterDisturbCheckFailedTime = %{public}d", new Object[]{Integer.valueOf(this.mInterDisturbCheckFailedTime)});
    }

    public void updateInterDisturbHappenedTime() {
        this.mInterDisturbHappenedTime++;
        MpLinkCommonUtils.logD(TAG, false, "updateInterDisturbHappenedTime, mInterDisturbHappenedTime = %{public}d", new Object[]{Integer.valueOf(this.mInterDisturbHappenedTime)});
    }

    public void updateNoInterDisturbHappenedTime() {
        this.mNoInterDisturbHappenedTime++;
        MpLinkCommonUtils.logD(TAG, false, "updateNoInterDisturbHappenedTime, mNoInterDisturbHappenedTime = %{public}d", new Object[]{Integer.valueOf(this.mNoInterDisturbHappenedTime)});
    }

    public void updateMobileDataConnectedStamp() {
        this.mMobileDataConnectedStamp = System.currentTimeMillis();
        long j = this.mMpLinkOpenMobileDataStamp;
        if (j != 0) {
            this.mLteRequestTime = this.mMobileDataConnectedStamp - j;
        }
        this.mMpLinkOpenMobileDataStamp = 0;
        MpLinkCommonUtils.logD(TAG, false, "updateMobileDataConnectedStamp,mLteRequestTime[ %{public}s ]ms", new Object[]{String.valueOf(this.mLteRequestTime)});
    }

    public void updateCellResidentTime() {
        this.mCellResidentTime += (SystemClock.elapsedRealtime() - this.mWifiConnectedTimeStamp) / 1000;
    }

    public void updateDualNetworkCnt() {
        MpLinkCommonUtils.logI(TAG, false, "updateDualNetworkCnt", new Object[0]);
        this.mTotalDualNetworkCnt++;
    }

    public void updateCellToWifiCntInGameScene() {
        this.mCellToWifiInGameScene++;
    }

    public void updateWifiToCellCntInGameScene() {
        this.mWifiToCellInGameScene++;
    }

    public void updateWifiDetectBadCnt() {
        this.mWifiDetectBadCnt++;
    }

    public void updatePingPongTimesForGame() {
        this.mPingPongTimesForGame++;
    }

    private void updateMpLinkedDurationTime() {
        this.mMpLinkedDurationTime += System.currentTimeMillis() - this.mMpLinkedStartTime;
    }

    public void updateWifiInfo(WifiInfo info) {
        if (info == null) {
            MpLinkCommonUtils.logD(TAG, false, "WifiInfo is null", new Object[0]);
            return;
        }
        if (info.is5GHz()) {
            this.mWiFiFreq5Cnt++;
        } else {
            this.mWiFiFreq24Cnt++;
        }
        this.mRssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(info.getFrequency(), info.getRssi());
        MpLinkCommonUtils.logD(TAG, false, "Freq:%{public}d, level: %{public}d", new Object[]{Integer.valueOf(info.getFrequency()), Integer.valueOf(this.mRssiLevel)});
        int i = this.mRssiLevel;
        if (i == 0) {
            this.mWiFiLevel0Cnt++;
        } else if (i == 1) {
            this.mWiFiLevel1Cnt++;
        } else if (i == 2) {
            this.mWiFiLevel2Cnt++;
        } else if (i == 3) {
            this.mWiFiLevel3Cnt++;
        } else if (i == 4) {
            this.mWiFiLevel4Cnt++;
        }
    }

    public void updateUnBindFailCnt(int reason) {
        MpLinkCommonUtils.logI(TAG, false, "updateUnBindFailCnt", new Object[0]);
        this.mRequestUnBindFail++;
        this.mLastUnBindFailReason = reason;
        updateHashMap(String.valueOf(reason), this.mRequestUnBindFailMap);
    }

    public void updateCloseSocketSuccCnt() {
        MpLinkCommonUtils.logI(TAG, false, "updateCloseSocketSuccCnt", new Object[0]);
        this.mRequestCloseSocketSucc++;
    }

    public void updateCloseSocketFailCnt(int reason) {
        MpLinkCommonUtils.logI(TAG, false, "updateCloseSocketFailCnt", new Object[0]);
        this.mRequestCloseSocketFail++;
        this.mLastCloseSocketFailReason = reason;
        updateHashMap(String.valueOf(reason), this.mRequestCloseSocketFailMap);
    }

    private void updateHashMap(String name, HashMap<String, Integer> map) {
        if (map != null) {
            Integer cnt = map.get(name);
            if (cnt == null) {
                map.put(name, 1);
            } else {
                map.put(name, Integer.valueOf(cnt.intValue() + 1));
            }
        }
    }

    private String hashMapToString(HashMap<String, Integer> map) {
        StringBuffer buf = new StringBuffer(16);
        buf.append(LEFT_BRACKET);
        if (map != null) {
            int size = map.size();
            int i = 0;
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                Integer value = entry.getValue();
                buf.append(entry.getKey());
                buf.append(":");
                if (value != null) {
                    buf.append(value.intValue());
                }
                i++;
                if (i < size) {
                    buf.append(",");
                }
            }
        }
        buf.append(RIGHT_BRACKET);
        return buf.toString();
    }

    private int getValueFromMap(HashMap<String, Integer> map, int key) {
        String strKey = String.valueOf(key);
        if (map == null || map.get(strKey) == null) {
            return 0;
        }
        return map.get(strKey).intValue();
    }

    private Bundle buildMpLinkChrBundle() {
        Bundle data = new Bundle();
        data.putInt(REQUEST_COEXIST_COUNT_FIELD, this.mRequestOpenSucc + this.mRequestOpenFail);
        data.putInt(REQUEST_UNCOEXIST_COUNT_FIELD, this.mRequestCloseSucc + this.mRequestCloseFail);
        data.putInt(COEXIST_SUCCESS_COUNT_FIELD, this.mRequestOpenSucc);
        data.putInt(WIFI_PLUS_FAIL_FIELD, this.mRequestCloseSucc);
        data.putInt(CELL_SRC_FAIL_FIELD, (int) this.mLteRequestTime);
        data.putInt(BIND_COUNT_FIELD, this.mRequestBindSucc);
        data.putInt(UNBIND_COUNT_FIELD, this.mRequestUnBindSucc);
        data.putInt(ROUTE_CHANGE_COUNT_FIELD, this.mDefaultRouteChangeCnt);
        data.putInt(TOTAL_TX_FIELD, (int) this.mTotalTxBytes);
        data.putInt(TOTAL_RX_FIELD, (int) this.mTotalRxBytes);
        data.putInt(SMART_DEVICE_COUNT_FIELD, this.mAiDeviceOpenCnt);
        data.putInt(BRAIN_FAIL_FIELD, this.mWifiDetectBadCnt);
        data.putInt(SIM_FAIL_FIELD, this.mCellToWifiInGameScene);
        data.putInt(CELL_REQUEST_FAIL_FIELD, this.mWifiToCellInGameScene);
        data.putInt(CELL_CLOSE_FAIL_FIELD, this.mPingPongTimesForGame);
        data.putInt(VPN_FAIL_FIELD, (int) this.mCellResidentTime);
        data.putInt(BRIDGE_FAIL_FIELD, getValueFromMap(this.mRequestOpenFailMap, 10));
        data.putInt(REQUEST_OPEN_FAIL_COUNT_FIELD, this.mRequestOpenFail);
        data.putInt(REQUEST_BIND_FAIL_FIELD, this.mRequestBindFail);
        data.putInt(REQUEST_UNBIND_FAIL_COUNT_FIELD, this.mRequestUnBindFail);
        data.putInt(COEXIST_WIFI_CLOSE_COUNT_FIELD, this.mCoexistWifiSwitchClosed);
        data.putInt(COEXIST_MOBILE_CLOSE_COUNT_FIELD, this.mCoexistMobileDataSwitchClosed);
        data.putInt(REQUEST_CLOSE_SOCKET_SUCCESS_FIELD, this.mRequestCloseSocketSucc);
        data.putInt(REQUEST_CLOSE_SOCKET_FAIL_FIELD, this.mRequestCloseSocketFail);
        data.putInt(BIND_FAIL_REASON_FIELD, this.mLastBindFailReason);
        data.putInt(UNBIND_FAIL_REASON_FIELD, this.mLastUnBindFailReason);
        data.putInt(CLOSE_SOCKET_FAIL_REASON_FIELD, this.mLastCloseSocketFailReason);
        data.putInt(TOTAL_DUALNETWORK_COUNT_FIELD, this.mTotalDualNetworkCnt);
        data.putInt(MPLINK_DURATION_TIME_FIELD, (int) (this.mMpLinkedDurationTime / 1000));
        data.putInt(WIFI_LEVEL0_COUNT_FIELD, this.mWiFiLevel0Cnt);
        data.putInt(WIFI_LEVEL1_COUNT_FIELD, this.mWiFiLevel1Cnt);
        data.putInt(WIFI_LEVEL2_COUNT_FIELD, this.mWiFiLevel2Cnt);
        data.putInt(WIFI_LEVEL3_COUNT_FIELD, this.mWiFiLevel3Cnt);
        data.putInt(WIFI_LEVEL4_COUNT_FIELD, this.mWiFiLevel4Cnt);
        data.putInt(WIFI_FREQ24_COUNT_FIELD, this.mWiFiFreq24Cnt);
        data.putInt(WIFI_FREQ5_COUNT_FIELD, this.mWiFiFreq5Cnt);
        return data;
    }

    public void sendDataToChr(IHiDataCHRCallBack mHiDataChrCallBack) {
        if (mHiDataChrCallBack != null) {
            MpLinkCommonUtils.logI(TAG, false, "sendDataToChr:%{public}s", new Object[]{toString()});
            mHiDataChrCallBack.uploadHiDataDFTEvent(EID_WIFI_MPLINK_INFO, buildMpLinkChrBundle());
            clearChrInfo();
        }
    }

    public void clearChrInfo() {
        MpLinkCommonUtils.logI(TAG, false, "clearChrInfo", new Object[0]);
        this.mAiDeviceOpenCnt = 0;
        this.mDefaultRouteChangeCnt = 0;
        this.mRequestOpenFail = 0;
        this.mRequestOpenSucc = 0;
        this.mRequestCloseFail = 0;
        this.mRequestCloseSucc = 0;
        this.mRequestBindSucc = 0;
        this.mRequestBindFail = 0;
        this.mRequestUnBindSucc = 0;
        this.mRequestUnBindFail = 0;
        this.mRequestCloseSocketSucc = 0;
        this.mTotalTxBytes = 0;
        this.mTotalRxBytes = 0;
        this.mCoexistWifiSwitchClosed = 0;
        this.mLastBindFailReason = 0;
        this.mLastUnBindFailReason = FAIL_REASON_CODE;
        this.mLastCloseSocketFailReason = FAIL_REASON_CODE;
        this.mCoexistMobileDataSwitchClosed = 0;
        this.mWiFiLevel0Cnt = 0;
        this.mWiFiLevel1Cnt = 0;
        this.mWiFiLevel2Cnt = 0;
        this.mWiFiLevel3Cnt = 0;
        this.mWiFiLevel4Cnt = 0;
        this.mWiFiFreq24Cnt = 0;
        this.mWiFiFreq5Cnt = 0;
        this.mMpLinkedDurationTime = 0;
        this.mMpLinkOpenMobileDataStamp = 0;
        this.mMobileDataConnectedStamp = 0;
        this.mTotalDualNetworkCnt = 0;
        this.mLteRequestTime = 0;
        this.mRequestOpenFailMap.clear();
        this.mRequestCloseFailMap.clear();
        this.mRequestBindFailMap.clear();
        this.mRequestUnBindFailMap.clear();
        this.mRequestCloseSocketFailMap.clear();
        this.mAiDeviceOpenMap.clear();
        this.mInterDisturbCheckTriggerTime = 0;
        this.mInterDisturbCheckFailedTime = 0;
        this.mInterDisturbHappenedTime = 0;
        this.mNoInterDisturbHappenedTime = 0;
        this.mCellToWifiInGameScene = 0;
        this.mWifiToCellInGameScene = 0;
        this.mWifiDetectBadCnt = 0;
        this.mPingPongTimesForGame = 0;
        this.mCellResidentTime = 0;
    }

    public String toString() {
        return "MpLinkChrInfo:mAiDeviceOpenCnt(" + this.mAiDeviceOpenCnt + RIGHT_BRACKET_AND_COMMA + "mDefaultRouteChangeCnt(" + this.mDefaultRouteChangeCnt + RIGHT_BRACKET_AND_COMMA + "mRequestOpenFail(" + this.mRequestOpenFail + RIGHT_BRACKET_AND_COMMA + "mRequestOpenSucc(" + this.mRequestOpenSucc + RIGHT_BRACKET_AND_COMMA + "mRequestCloseFail(" + this.mRequestCloseFail + RIGHT_BRACKET_AND_COMMA + LINE_BREAK + "mRequestCloseSucc(" + this.mRequestCloseSucc + RIGHT_BRACKET_AND_COMMA + "mRequestBindSucc(" + this.mRequestBindSucc + RIGHT_BRACKET_AND_COMMA + "mRequestBindFail(" + this.mRequestBindFail + RIGHT_BRACKET_AND_COMMA + "mRequestUnBindSucc(" + this.mRequestUnBindSucc + RIGHT_BRACKET_AND_COMMA + "mRequestUnBindFail(" + this.mRequestUnBindFail + RIGHT_BRACKET_AND_COMMA + LINE_BREAK + "mTotalTxBytes(" + this.mTotalTxBytes + " KB" + RIGHT_BRACKET_AND_COMMA + "mTotalRxBytes(" + this.mTotalRxBytes + " KB" + RIGHT_BRACKET_AND_COMMA + "durationTime(" + (this.mMpLinkedDurationTime / 1000) + RIGHT_BRACKET_AND_COMMA + "mLteRequestTime(" + this.mLteRequestTime + " ms" + RIGHT_BRACKET_AND_COMMA + "dualNetworkCnt(" + this.mTotalDualNetworkCnt + RIGHT_BRACKET_AND_COMMA + "mCoexistWifiSwitchClosed(" + this.mCoexistWifiSwitchClosed + RIGHT_BRACKET_AND_COMMA + "mLastBindFailReason(" + this.mLastBindFailReason + RIGHT_BRACKET_AND_COMMA + "mLastUnBindFailReason(" + this.mLastUnBindFailReason + RIGHT_BRACKET_AND_COMMA + "mLastCloseSocketFailReason(" + this.mLastCloseSocketFailReason + RIGHT_BRACKET_AND_COMMA + "mCoexistMobileDataSwitchClosed(" + this.mCoexistMobileDataSwitchClosed + RIGHT_BRACKET_AND_COMMA + LINE_BREAK + "mRequestOpenFailMap" + hashMapToString(this.mRequestOpenFailMap) + LINE_BREAK + "mRequestCloseFailMap" + hashMapToString(this.mRequestCloseFailMap) + LINE_BREAK + "mRequestBindFailMap" + hashMapToString(this.mRequestBindFailMap) + LINE_BREAK + "mRequestUnBindFailMap" + hashMapToString(this.mRequestUnBindFailMap) + LINE_BREAK + "mRequestCloseSocketFailMap" + hashMapToString(this.mRequestCloseSocketFailMap) + LINE_BREAK + "mAiDeviceOpenMap" + hashMapToString(this.mAiDeviceOpenMap) + LINE_BREAK + "mInterDisturbCheckTriggerTime(" + this.mInterDisturbCheckTriggerTime + RIGHT_BRACKET_AND_COMMA + "mInterDisturbCheckFailedTime(" + this.mInterDisturbCheckFailedTime + RIGHT_BRACKET_AND_COMMA + "mInterDisturbHappenedTime(" + this.mInterDisturbHappenedTime + RIGHT_BRACKET_AND_COMMA + "mNoInterDisturbHappenedTime(" + this.mNoInterDisturbHappenedTime + RIGHT_BRACKET_AND_COMMA + LINE_BREAK;
    }

    public void dump() {
        MpLinkCommonUtils.logI(TAG, false, "dump!", new Object[0]);
    }
}
