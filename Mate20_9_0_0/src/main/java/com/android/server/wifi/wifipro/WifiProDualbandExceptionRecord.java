package com.android.server.wifi.wifipro;

import android.util.Log;

public class WifiProDualbandExceptionRecord {
    private static final String TAG = "WifiProStatisticsRecord";
    public static final String WIFIPRO_DEAULT_STR = "DEAULT_STR";
    public static final short WIFIPRO_STATE_DISABLE = (short) 2;
    public static final short WIFIPRO_STATE_ENABLE = (short) 1;
    public static final short WIFIPRO_STATE_UNKNOW = (short) 0;
    public short mConnectTime_2G;
    public short mConnectTime_5G;
    public short mHandOverErrCode;
    public short mIsBluetoothConnected;
    public short mLossRate_2G;
    public short mLossRate_5G;
    public short mRSSI_2G;
    public short mRSSI_5G;
    public short mRTT_2G;
    public short mRTT_5G;
    public String mSSID_2G;
    public String mSSID_5G;
    public short mScan_Threshod_RSSI_2G;
    public short mScore_2G;
    public short mScore_5G;
    public short mSingleOrMixed;
    public short mTarget_RSSI_5G;

    public WifiProDualbandExceptionRecord() {
        resetRecord();
    }

    private void resetRecord() {
        this.mSSID_2G = "DEAULT_STR";
        this.mSSID_5G = "DEAULT_STR";
        this.mSingleOrMixed = (short) 0;
        this.mScan_Threshod_RSSI_2G = (short) 0;
        this.mTarget_RSSI_5G = (short) 0;
        this.mRSSI_2G = (short) 0;
        this.mRSSI_5G = (short) 0;
        this.mScore_2G = (short) 0;
        this.mScore_5G = (short) 0;
        this.mHandOverErrCode = (short) 0;
        this.mIsBluetoothConnected = (short) 0;
        this.mRTT_2G = (short) 0;
        this.mLossRate_2G = (short) 0;
        this.mConnectTime_2G = (short) 0;
        this.mRTT_5G = (short) 0;
        this.mLossRate_5G = (short) 0;
        this.mConnectTime_5G = (short) 0;
    }

    public void dumpAllParameter() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dumpChrStatRecord:, mSSID_2G= ");
        stringBuilder.append(this.mSSID_2G);
        stringBuilder.append(", mSSID_5G= ");
        stringBuilder.append(this.mSSID_5G);
        stringBuilder.append(", mSingleOrMixed= ");
        stringBuilder.append(this.mSingleOrMixed);
        stringBuilder.append(", mScan_Threshod_RSSI_2G= ");
        stringBuilder.append(this.mScan_Threshod_RSSI_2G);
        stringBuilder.append(", mTarget_RSSI_5G= ");
        stringBuilder.append(this.mTarget_RSSI_5G);
        stringBuilder.append(", mRSSI_2G= ");
        stringBuilder.append(this.mRSSI_2G);
        stringBuilder.append(", mRSSI_5G= ");
        stringBuilder.append(this.mRSSI_5G);
        stringBuilder.append(", mScore_2G= ");
        stringBuilder.append(this.mScore_2G);
        stringBuilder.append(", mScore_5G= ");
        stringBuilder.append(this.mScore_5G);
        stringBuilder.append(", mHandOverErrCode= ");
        stringBuilder.append(this.mHandOverErrCode);
        stringBuilder.append(", mIsBluetoothConnected= ");
        stringBuilder.append(this.mIsBluetoothConnected);
        stringBuilder.append(", mRTT_2G= ");
        stringBuilder.append(this.mRTT_2G);
        stringBuilder.append(", mLossRate_2G= ");
        stringBuilder.append(this.mLossRate_2G);
        stringBuilder.append(", mConnectTime_2G= ");
        stringBuilder.append(this.mConnectTime_2G);
        stringBuilder.append(", mRTT_5G= ");
        stringBuilder.append(this.mRTT_5G);
        stringBuilder.append(", mLossRate_5G= ");
        stringBuilder.append(this.mLossRate_5G);
        stringBuilder.append(", mConnectTime_5G= ");
        stringBuilder.append(this.mConnectTime_5G);
        Log.i(str, stringBuilder.toString());
    }
}
