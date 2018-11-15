package com.android.server.hidata.histream;

import com.android.server.hidata.appqoe.HwAPPQoEUtils;

public class HwHistreamCHRMachineInfo {
    public String mApkName = HwAPPQoEUtils.INVALID_STRING_VALUE;
    public int mCellQuality = -1;
    public int mCellSig = -1;
    public int mCellSinr = -1;
    public int mChLoad = -1;
    public int mNetDlTup = -1;
    public int mNetRtt = -1;
    public int mRAT = -1;
    public int mRxTup1Bef = -1;
    public int mRxTup2Bef = -1;
    public int mScenario = -1;
    public int mStreamQoe = -1;
    public int mTxFail1Bef = -1;
    public int mTxFail2Bef = -1;
    public int mWechatVideoQoe = -1;
    public int mWifiRssi = -1;
    public int mWifiSnr = -1;

    public void printCHRMachineInfo() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("printCHRMachineInfo mApkName = ");
        stringBuilder.append(this.mApkName);
        stringBuilder.append(" mScenario = ");
        stringBuilder.append(this.mScenario);
        stringBuilder.append(" mRxTup1Bef = ");
        stringBuilder.append(this.mRxTup1Bef);
        stringBuilder.append(" mRxTup2Bef = ");
        stringBuilder.append(this.mRxTup2Bef);
        stringBuilder.append(" mScenario = ");
        stringBuilder.append(this.mScenario);
        stringBuilder.append(" mChLoad = ");
        stringBuilder.append(this.mChLoad);
        stringBuilder.append(" mTxFail1Bef = ");
        stringBuilder.append(this.mTxFail1Bef);
        stringBuilder.append(" mTxFail2Bef = ");
        stringBuilder.append(this.mTxFail2Bef);
        stringBuilder.append(" mStreamQoe = ");
        stringBuilder.append(this.mStreamQoe);
        stringBuilder.append(" mWechatVideoQoe = ");
        stringBuilder.append(this.mWechatVideoQoe);
        stringBuilder.append(" mRAT = ");
        stringBuilder.append(this.mRAT);
        stringBuilder.append(" mWifiRssi = ");
        stringBuilder.append(this.mWifiRssi);
        stringBuilder.append(" mWifiSnr = ");
        stringBuilder.append(this.mWifiSnr);
        stringBuilder.append(" mWifiSnr = ");
        stringBuilder.append(this.mWifiSnr);
        stringBuilder.append(" mCellSig = ");
        stringBuilder.append(this.mCellSig);
        stringBuilder.append(" mCellQuality = ");
        stringBuilder.append(this.mCellQuality);
        stringBuilder.append(" mCellSinr = ");
        stringBuilder.append(this.mCellSinr);
        stringBuilder.append(" mNetDlTup = ");
        stringBuilder.append(this.mNetDlTup);
        stringBuilder.append(" mNetRtt = ");
        stringBuilder.append(this.mNetRtt);
        HwHiStreamUtils.logD(stringBuilder.toString());
    }
}
