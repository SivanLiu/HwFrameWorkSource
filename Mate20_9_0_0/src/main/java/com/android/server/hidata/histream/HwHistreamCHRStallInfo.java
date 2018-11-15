package com.android.server.hidata.histream;

import com.android.server.hidata.appqoe.HwAPPQoEUtils;

public class HwHistreamCHRStallInfo {
    public String mAPKName = HwAPPQoEUtils.INVALID_STRING_VALUE;
    public int mApRtt = -1;
    public int mCellRsrq = -1;
    public int mCellSig = -1;
    public int mCellSinr = -1;
    public int mDlTup = -1;
    public int mEventId = -1;
    public int mNeiborApRssi = -1;
    public int mNetDlTup = -1;
    public int mNetRtt = -1;
    public int mRAT = -1;
    public int mScenario = -1;
    public int mUlTup = -1;
    public int mWifiChload = -1;
    public int mWifiSnr = -1;

    public void printCHRStallInfo() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("printCHRStallInfo mEventId = ");
        stringBuilder.append(this.mEventId);
        stringBuilder.append(" mAPKName = ");
        stringBuilder.append(this.mAPKName);
        stringBuilder.append(" mScenario = ");
        stringBuilder.append(this.mScenario);
        stringBuilder.append(" mRAT = ");
        stringBuilder.append(this.mRAT);
        stringBuilder.append(" mUlTup = ");
        stringBuilder.append(this.mUlTup);
        stringBuilder.append(" mDlTup = ");
        stringBuilder.append(this.mDlTup);
        stringBuilder.append(" mApRtt = ");
        stringBuilder.append(this.mApRtt);
        stringBuilder.append(" mNetRtt = ");
        stringBuilder.append(this.mNetRtt);
        stringBuilder.append(" mCellSig = ");
        stringBuilder.append(this.mCellSig);
        stringBuilder.append(" mCellRsrq = ");
        stringBuilder.append(this.mCellRsrq);
        stringBuilder.append(" mCellSinr = ");
        stringBuilder.append(this.mCellSinr);
        stringBuilder.append(" mNeiborApRssi = ");
        stringBuilder.append(this.mNeiborApRssi);
        stringBuilder.append(" mWifiSnr = ");
        stringBuilder.append(this.mWifiSnr);
        stringBuilder.append(" mWifiChload = ");
        stringBuilder.append(this.mWifiChload);
        stringBuilder.append(" mNetDlTup = ");
        stringBuilder.append(this.mNetDlTup);
        HwHiStreamUtils.logD(stringBuilder.toString());
    }
}
