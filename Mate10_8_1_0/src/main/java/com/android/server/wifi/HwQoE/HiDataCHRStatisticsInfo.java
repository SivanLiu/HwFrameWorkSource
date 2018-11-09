package com.android.server.wifi.HwQoE;

public class HiDataCHRStatisticsInfo {
    public int mAPPType = 0;
    public int mCallInCellularDur = 0;
    public int mCallInWiFiDur = 0;
    public int mCallTotalCnt = 0;
    public int mCellLv1Cnt = 0;
    public int mCellLv2Cnt = 0;
    public int mCellLv3Cnt = 0;
    public long mLastUploadTime = 0;
    public int mStallSwitch0Cnt = 0;
    public int mStallSwitch1Cnt = 0;
    public int mStallSwitchAbove1Cnt = 0;
    public int mStallSwitchCnt = 0;
    public int mStartInCellularCnt = 0;
    public int mStartInWiFiCnt = 0;
    public long mStartTime = 0;
    public int mSwitch2CellCnt = 0;
    public int mSwitch2WifiCnt = 0;
    public int mTrfficCell = 0;
    public int mVipSwitchCnt = 0;
    public int mWiFiLv1Cnt = 0;
    public int mWiFiLv2Cnt = 0;
    public int mWiFiLv3Cnt = 0;

    public HiDataCHRStatisticsInfo(int appType) {
        this.mAPPType = appType;
    }
}
