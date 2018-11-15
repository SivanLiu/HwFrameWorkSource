package com.android.server.hidata.mplink;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import com.android.server.hidata.arbitration.IHiDataCHRCallBack;
import huawei.android.net.hwmplink.MpLinkCommonUtils;
import java.util.HashMap;
import java.util.Map.Entry;

public class HwMplinkChrImpl {
    public static final int EID_WIFI_MPLINK_INFO = 909009038;
    private static final String TAG = "HiData_HwMpLinkChrImpl";
    private int lastBindFailReason = -9999;
    private int lastCloseSocketFailReason = -9999;
    private int lastUnBindFailReason = -9999;
    private int mAiDeviceOpenCnt = 0;
    private HashMap<String, Integer> mAiDeviceOpenMap;
    private int mCoexistMobileDataSwitchClosed = 0;
    private int mCoexistWifiSwitchClosed = 0;
    private int mDefaultRouteChangeCnt = 0;
    private long mLteQequestTime;
    private long mMobileDataConnectedStamp;
    private String mMobileIface = "";
    private long mMobileRxBytesBase = 0;
    private long mMobileTxBytesBase = 0;
    private long mMpLinkOpenMobileDataStamp;
    private long mMpLinkedDurationTime;
    private long mMpLinkedStartime;
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
    private int mRssiLevel;
    private int mTotalDualNetworkCnt;
    private long mTotalRxBytes;
    private long mTotalTxBytes;
    private int mWiFiFreq24Cnt;
    private int mWiFiFreq5Cnt;
    private int mWiFiLevel0Cnt;
    private int mWiFiLevel1Cnt;
    private int mWiFiLevel2Cnt;
    private int mWiFiLevel3Cnt;
    private int mWiFiLevel4Cnt;
    private WifiManager mWifiManager;

    public HwMplinkChrImpl(Context context) {
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mAiDeviceOpenMap = new HashMap();
        this.mRequestOpenFailMap = new HashMap();
        this.mRequestCloseFailMap = new HashMap();
        this.mRequestBindFailMap = new HashMap();
        this.mRequestUnBindFailMap = new HashMap();
        this.mRequestCloseSocketFailMap = new HashMap();
    }

    public void setMobileIface(String iface) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setMobileIface:");
        stringBuilder.append(iface);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        this.mMobileIface = iface;
    }

    private void resetMobileStats() {
        this.mMobileTxBytesBase = 0;
        this.mMobileRxBytesBase = 0;
    }

    public void updateMobileStatsBase() {
        MpLinkCommonUtils.logI(TAG, "enter updateMobileStatsBase");
        if (!TextUtils.isEmpty(this.mMobileIface) && !this.mMobileIface.equals("")) {
            this.mMobileTxBytesBase = TrafficStats.getTxBytes(this.mMobileIface);
            this.mMobileRxBytesBase = TrafficStats.getRxBytes(this.mMobileIface);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateMobileStatsBase,Tx(");
            stringBuilder.append(this.mMobileTxBytesBase);
            stringBuilder.append("),Rx(");
            stringBuilder.append(this.mMobileRxBytesBase);
            stringBuilder.append(")");
            MpLinkCommonUtils.logI(str, stringBuilder.toString());
        }
    }

    public void updateMobileStats() {
        MpLinkCommonUtils.logI(TAG, "enter updateMobileStats");
        if (!(TextUtils.isEmpty(this.mMobileIface) || this.mMobileIface.equals(""))) {
            long txByte = TrafficStats.getTxBytes(this.mMobileIface);
            long rxByte = TrafficStats.getRxBytes(this.mMobileIface);
            long deltaTxByte = (txByte - this.mMobileTxBytesBase) / 1024;
            long deltaRxByte = (rxByte - this.mMobileRxBytesBase) / 1024;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateMobileStats,txByte(");
            stringBuilder.append(txByte);
            stringBuilder.append(") - mMobileTxBytesBase(");
            stringBuilder.append(this.mMobileTxBytesBase);
            stringBuilder.append(") = deltaTxByte(");
            stringBuilder.append(deltaTxByte);
            stringBuilder.append(")");
            MpLinkCommonUtils.logI(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateMobileStats,rxByte(");
            stringBuilder.append(rxByte);
            stringBuilder.append(") - mMobileRxBytesBase(");
            stringBuilder.append(this.mMobileRxBytesBase);
            stringBuilder.append(") = deltaRxByte(");
            stringBuilder.append(deltaRxByte);
            stringBuilder.append(")");
            MpLinkCommonUtils.logI(str, stringBuilder.toString());
            updateTotalTxBytes(deltaTxByte);
            updateTotalRxBytes(deltaRxByte);
        }
        resetMobileStats();
    }

    public void updateCoexistWifiSwitchClosedCnt() {
        MpLinkCommonUtils.logI(TAG, "updateCoexistWifiSwitchClosedCnt");
        this.mCoexistWifiSwitchClosed++;
    }

    public void updateCoexistMobileDataSwitchClosedCnt() {
        MpLinkCommonUtils.logI(TAG, "updateCoexistMobileDataSwitchClosedCnt");
        this.mCoexistMobileDataSwitchClosed++;
    }

    public void updateAiDeviceOpenCnt(String ApName) {
        MpLinkCommonUtils.logD(TAG, "updateAiDeviceOpenCnt");
        this.mAiDeviceOpenCnt++;
    }

    public void updateDefaultRouteChangeCnt() {
        MpLinkCommonUtils.logI(TAG, "updateDefaultRouteChangeCnt");
        this.mDefaultRouteChangeCnt++;
    }

    private void updateTotalTxBytes(long TxByes) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateTotalTxBytes,TxByes:");
        stringBuilder.append(TxByes);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        this.mTotalTxBytes += TxByes;
    }

    private void updateTotalRxBytes(long RxByes) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateTotalRxBytes,RxByes:");
        stringBuilder.append(RxByes);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        this.mTotalRxBytes += RxByes;
    }

    public void updateOpenSuccCnt() {
        MpLinkCommonUtils.logI(TAG, "updateOpenSuccCnt");
        this.mRequestOpenSucc++;
    }

    public void updateOpenFailCnt(int reason) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateOpenFailCnt,reason:");
        stringBuilder.append(reason);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        this.mRequestOpenFail++;
        updateHashMap(String.valueOf(reason), this.mRequestOpenFailMap);
    }

    public void updateCloseSuccCnt() {
        MpLinkCommonUtils.logI(TAG, "updateCloseSuccCnt");
        this.mRequestCloseSucc++;
    }

    public void updateCloseFailCnt(int reason) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCloseFailCnt,reason:");
        stringBuilder.append(reason);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        this.mRequestCloseFail++;
        updateHashMap(String.valueOf(reason), this.mRequestCloseFailMap);
    }

    public void updateBindSuccCnt() {
        MpLinkCommonUtils.logI(TAG, "updateBindSuccCnt");
        this.mRequestBindSucc++;
    }

    public void updateBindFailCnt(int reason) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateBindFailCnt,reason:");
        stringBuilder.append(reason);
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        this.mRequestBindFail++;
        this.lastBindFailReason = reason;
        updateHashMap(String.valueOf(reason), this.mRequestBindFailMap);
    }

    public void updateUnBindSuccCnt() {
        MpLinkCommonUtils.logI(TAG, "updateUnBindSuccCnt");
        this.mRequestUnBindSucc++;
    }

    public void updateMplinkCellBindState(boolean binded, String iface) {
        if (binded) {
            this.mMpLinkedStartime = System.currentTimeMillis();
            setMobileIface(iface);
            updataMpLinkedStartime();
            updateMobileStatsBase();
            updateWifiInfo(this.mWifiManager.getConnectionInfo());
        } else if (this.mMpLinkedStartime != 0) {
            updataMpLinkedDurationTime();
            updateMobileStats();
            this.mMpLinkedStartime = 0;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("TotalTxBytes: ");
            stringBuilder.append(this.mTotalTxBytes);
            stringBuilder.append(" KB, TotalRxBytes: ");
            stringBuilder.append(this.mTotalRxBytes);
            stringBuilder.append(" KB, duration:");
            stringBuilder.append(this.mMpLinkedDurationTime / 1000);
            stringBuilder.append(" s");
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
        }
    }

    private void updataMpLinkedStartime() {
        this.mMpLinkedStartime = System.currentTimeMillis();
    }

    public void updateOpenMobileDataStamp() {
        this.mMpLinkOpenMobileDataStamp = System.currentTimeMillis();
    }

    public void updateMobileDataConnectedStamp() {
        this.mMobileDataConnectedStamp = System.currentTimeMillis();
        if (0 != this.mMpLinkOpenMobileDataStamp) {
            this.mLteQequestTime = this.mMobileDataConnectedStamp - this.mMpLinkOpenMobileDataStamp;
        }
        this.mMpLinkOpenMobileDataStamp = 0;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateMobileDataConnectedStamp,mLteQequestTime[ ");
        stringBuilder.append(this.mLteQequestTime);
        stringBuilder.append(" ]ms");
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
    }

    public void updataDualNetworkCnt() {
        MpLinkCommonUtils.logI(TAG, "updataDualNetworkCnt");
        this.mTotalDualNetworkCnt++;
    }

    private void updataMpLinkedDurationTime() {
        MpLinkCommonUtils.logI(TAG, "updataMpLinkedDurationTime");
        this.mMpLinkedDurationTime += System.currentTimeMillis() - this.mMpLinkedStartime;
    }

    public void updateWifiInfo(WifiInfo info) {
        if (info == null) {
            MpLinkCommonUtils.logD(TAG, "WifiInfo is null");
            return;
        }
        if (info.is5GHz()) {
            this.mWiFiFreq5Cnt++;
        } else {
            this.mWiFiFreq24Cnt++;
        }
        this.mRssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(info.getFrequency(), info.getRssi());
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Freq:");
        stringBuilder.append(info.getFrequency());
        stringBuilder.append(", level: ");
        stringBuilder.append(this.mRssiLevel);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        switch (this.mRssiLevel) {
            case 0:
                this.mWiFiLevel0Cnt++;
                break;
            case 1:
                this.mWiFiLevel1Cnt++;
                break;
            case 2:
                this.mWiFiLevel2Cnt++;
                break;
            case 3:
                this.mWiFiLevel3Cnt++;
                break;
            case 4:
                this.mWiFiLevel4Cnt++;
                break;
        }
    }

    public void updateUnBindFailCnt(int reason) {
        MpLinkCommonUtils.logI(TAG, "updateUnBindFailCnt");
        this.mRequestUnBindFail++;
        this.lastUnBindFailReason = reason;
        updateHashMap(String.valueOf(reason), this.mRequestUnBindFailMap);
    }

    public void updateCloseSocketSuccCnt() {
        MpLinkCommonUtils.logI(TAG, "updateCloseSocketSuccCnt");
        this.mRequestCloseSocketSucc++;
    }

    public void updateCloseSocketFailCnt(int reason) {
        MpLinkCommonUtils.logI(TAG, "updateCloseSocketFailCnt");
        this.mRequestCloseSocketFail++;
        this.lastCloseSocketFailReason = reason;
        updateHashMap(String.valueOf(reason), this.mRequestCloseSocketFailMap);
    }

    private void updateHashMap(String name, HashMap<String, Integer> map) {
        if (map != null) {
            Integer cnt = (Integer) map.get(name);
            if (cnt == null) {
                map.put(name, Integer.valueOf(1));
            } else {
                map.put(name, Integer.valueOf(cnt.intValue() + 1));
            }
        }
    }

    private String hashMapToString(HashMap<String, Integer> map) {
        StringBuffer buf = new StringBuffer();
        buf.append("(");
        if (map != null) {
            int size = map.size();
            int i = 0;
            for (Entry<String, Integer> entry : map.entrySet()) {
                Integer value = (Integer) entry.getValue();
                buf.append((String) entry.getKey());
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
        buf.append(")");
        return buf.toString();
    }

    private int getValueFromMap(HashMap<String, Integer> map, int key) {
        String strKey = String.valueOf(key);
        if (map == null || map.get(strKey) == null) {
            return 0;
        }
        return ((Integer) map.get(strKey)).intValue();
    }

    private Bundle buildMpLinkChrBundle() {
        Bundle data = new Bundle();
        data.putInt("reCoxCnt", this.mRequestOpenSucc + this.mRequestOpenFail);
        data.putInt("reUncoxCnt", this.mRequestCloseSucc + this.mRequestCloseFail);
        data.putInt("coxSuccCnt", this.mRequestOpenSucc);
        data.putInt("wifiplusFail", this.mRequestCloseSucc);
        data.putInt("cellRoFail", 1);
        data.putInt("cellNoSrcFail", (int) this.mLteQequestTime);
        data.putInt("cellCloseFail", getValueFromMap(this.mRequestOpenFailMap, 2));
        data.putInt("VPNFail", getValueFromMap(this.mRequestOpenFailMap, 6));
        data.putInt("bindCnt", this.mRequestBindSucc);
        data.putInt("unbindCnt", this.mRequestUnBindSucc);
        data.putInt("routeChCnt", this.mDefaultRouteChangeCnt);
        data.putInt("totaTx", (int) this.mTotalTxBytes);
        data.putInt("totalRx", (int) this.mTotalRxBytes);
        data.putInt("smartDeCnt", this.mAiDeviceOpenCnt);
        data.putInt("brainFail", getValueFromMap(this.mRequestOpenFailMap, 1));
        data.putInt("simErrorFail", getValueFromMap(this.mRequestOpenFailMap, 3));
        data.putInt("bridgeFail", getValueFromMap(this.mRequestOpenFailMap, 10));
        data.putInt("coxFailCnt", this.mRequestOpenFail);
        data.putInt("w2c_gap", this.mRequestBindFail);
        data.putInt("w2cRfiCnt", this.mRequestUnBindFail);
        data.putInt("c2wRfiCnt", this.mCoexistWifiSwitchClosed);
        data.putInt("cwRfiCnt", this.mCoexistMobileDataSwitchClosed);
        data.putInt("w2cRfiStop", this.mRequestCloseSocketSucc);
        data.putInt("w2cRfiRej", this.mRequestCloseSocketFail);
        data.putInt("c2wRfiStop", this.lastBindFailReason);
        data.putInt("c2wRfiRej", this.lastUnBindFailReason);
        data.putInt("cwRfiStop", this.lastCloseSocketFailReason);
        data.putInt("wcRfiRej", this.mTotalDualNetworkCnt);
        data.putInt("w2cRfiL1", (int) (this.mMpLinkedDurationTime / 1000));
        data.putInt("w2cRfiL2", this.mWiFiLevel0Cnt);
        data.putInt("w2cRfiL3", this.mWiFiLevel1Cnt);
        data.putInt("c2wRfiL1", this.mWiFiLevel2Cnt);
        data.putInt("c2wRfiL2", this.mWiFiLevel3Cnt);
        data.putInt("cwRfiL1", this.mWiFiLevel4Cnt);
        data.putInt("cwRfiL2", this.mWiFiFreq24Cnt);
        data.putInt("cwRfiL3", this.mWiFiFreq5Cnt);
        return data;
    }

    public void sendDataToChr(IHiDataCHRCallBack mHiDataCHRCallBack) {
        if (mHiDataCHRCallBack != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendDataToChr:");
            stringBuilder.append(toString());
            MpLinkCommonUtils.logI(str, stringBuilder.toString());
            mHiDataCHRCallBack.uploadHiDataDFTEvent(EID_WIFI_MPLINK_INFO, buildMpLinkChrBundle());
            clearChrInfo();
        }
    }

    public void clearChrInfo() {
        MpLinkCommonUtils.logI(TAG, "clearChrInfo");
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
        this.mTotalTxBytes = 0;
        this.mTotalRxBytes = 0;
        this.mCoexistWifiSwitchClosed = 0;
        this.lastBindFailReason = 0;
        this.lastUnBindFailReason = -9999;
        this.lastCloseSocketFailReason = -9999;
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
        this.mLteQequestTime = 0;
        this.mRequestOpenFailMap.clear();
        this.mRequestCloseFailMap.clear();
        this.mRequestBindFailMap.clear();
        this.mRequestUnBindFailMap.clear();
        this.mRequestCloseSocketFailMap.clear();
        this.mAiDeviceOpenMap.clear();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MpLinkChrInfo:mAiDeviceOpenCnt(");
        stringBuilder.append(this.mAiDeviceOpenCnt);
        stringBuilder.append("),mDefaultRouteChangeCnt(");
        stringBuilder.append(this.mDefaultRouteChangeCnt);
        stringBuilder.append("),mRequestOpenFail(");
        stringBuilder.append(this.mRequestOpenFail);
        stringBuilder.append("),mRequestOpenSucc(");
        stringBuilder.append(this.mRequestOpenSucc);
        stringBuilder.append("),mRequestCloseFail(");
        stringBuilder.append(this.mRequestCloseFail);
        stringBuilder.append("),\nmRequestCloseSucc(");
        stringBuilder.append(this.mRequestCloseSucc);
        stringBuilder.append("),mRequestBindSucc(");
        stringBuilder.append(this.mRequestBindSucc);
        stringBuilder.append("),mRequestBindFail(");
        stringBuilder.append(this.mRequestBindFail);
        stringBuilder.append("),mRequestUnBindSucc(");
        stringBuilder.append(this.mRequestUnBindSucc);
        stringBuilder.append("),mRequestUnBindFail(");
        stringBuilder.append(this.mRequestUnBindFail);
        stringBuilder.append("),\nmTotalTxBytes(");
        stringBuilder.append(this.mTotalTxBytes);
        stringBuilder.append(" KB),mTotalRxBytes(");
        stringBuilder.append(this.mTotalRxBytes);
        stringBuilder.append(" KB),durationTime(");
        stringBuilder.append(this.mMpLinkedDurationTime / 1000);
        stringBuilder.append(" ),mLteQequestTime(");
        stringBuilder.append(this.mLteQequestTime);
        stringBuilder.append(" ms),dualNetworkCnt(");
        stringBuilder.append(this.mTotalDualNetworkCnt);
        stringBuilder.append(" ),mCoexistWifiSwitchClosed(");
        stringBuilder.append(this.mCoexistWifiSwitchClosed);
        stringBuilder.append("),lastBindFailReason(");
        stringBuilder.append(this.lastBindFailReason);
        stringBuilder.append("),lastUnBindFailReason(");
        stringBuilder.append(this.lastUnBindFailReason);
        stringBuilder.append("),lastCloseSocketFailReason(");
        stringBuilder.append(this.lastCloseSocketFailReason);
        stringBuilder.append("),mCoexistMobileDataSwitchClosed(");
        stringBuilder.append(this.mCoexistMobileDataSwitchClosed);
        stringBuilder.append(")\nmRequestOpenFailMap");
        stringBuilder.append(hashMapToString(this.mRequestOpenFailMap));
        stringBuilder.append("\nmRequestCloseFailMap");
        stringBuilder.append(hashMapToString(this.mRequestCloseFailMap));
        stringBuilder.append("\nmRequestBindFailMap");
        stringBuilder.append(hashMapToString(this.mRequestBindFailMap));
        stringBuilder.append("\nmRequestUnBindFailMap");
        stringBuilder.append(hashMapToString(this.mRequestUnBindFailMap));
        stringBuilder.append("\nmRequestCloseSocketFailMap");
        stringBuilder.append(hashMapToString(this.mRequestCloseSocketFailMap));
        stringBuilder.append("\nmAiDeviceOpenMap");
        stringBuilder.append(hashMapToString(this.mAiDeviceOpenMap));
        stringBuilder.append("");
        return stringBuilder.toString();
    }

    public void dump() {
        MpLinkCommonUtils.logI(TAG, "dump!");
    }
}
