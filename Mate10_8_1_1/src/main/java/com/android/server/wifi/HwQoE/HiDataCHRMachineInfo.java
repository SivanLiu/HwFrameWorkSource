package com.android.server.wifi.HwQoE;

public class HiDataCHRMachineInfo {
    public int mChLoad = 0;
    public int mRxTup1Bef = 0;
    public int mRxTup2Bef = 0;
    public int mTxFail1Bef = 0;
    public int mTxFail2Bef = 0;

    public void initAll() {
        this.mRxTup1Bef = 0;
        this.mRxTup2Bef = 0;
        this.mChLoad = 0;
        this.mTxFail1Bef = 0;
        this.mTxFail2Bef = 0;
    }
}
