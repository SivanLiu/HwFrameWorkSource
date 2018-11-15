package com.android.server.hidata.appqoe;

public class HwAPPQoEAPKConfig {
    public String className = HwAPPQoEUtils.INVALID_STRING_VALUE;
    public int mAction = -1;
    public int mAggressiveStallTH = -1;
    public int mAppAlgorithm = -1;
    public int mAppId = -1;
    public int mAppPeriod = -1;
    public int mGeneralStallTH = -1;
    public float mHistoryQoeBadTH = -1.0f;
    public int mQci = -1;
    public String mReserved = HwAPPQoEUtils.INVALID_STRING_VALUE;
    public int mScenceId = -1;
    public int mScenceType = 0;
    public int monitorUserLearning = -1;
    public String packageName = HwAPPQoEUtils.INVALID_STRING_VALUE;

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" HwAPPQoEAPKConfig packageName = ");
        stringBuilder.append(this.packageName);
        stringBuilder.append(" className = ");
        stringBuilder.append(this.className);
        stringBuilder.append(" mAppId = ");
        stringBuilder.append(this.mAppId);
        stringBuilder.append(" mScenceId = ");
        stringBuilder.append(this.mScenceId);
        stringBuilder.append(" mScenceType = ");
        stringBuilder.append(this.mScenceType);
        stringBuilder.append(" mAppPeriod = ");
        stringBuilder.append(this.mAppPeriod);
        stringBuilder.append(" mAppAlgorithm = ");
        stringBuilder.append(this.mAppAlgorithm);
        stringBuilder.append(" mQci = ");
        stringBuilder.append(this.mQci);
        stringBuilder.append(" monitorUserLearning = ");
        stringBuilder.append(this.monitorUserLearning);
        stringBuilder.append(" mAction = ");
        stringBuilder.append(this.mAction);
        stringBuilder.append(" mHistoryQoeBadTH = ");
        stringBuilder.append(this.mHistoryQoeBadTH);
        stringBuilder.append(" mGeneralStallTH = ");
        stringBuilder.append(this.mGeneralStallTH);
        stringBuilder.append(" mAggressiveStallTH = ");
        stringBuilder.append(this.mAggressiveStallTH);
        stringBuilder.append(" mReserved = ");
        stringBuilder.append(this.mReserved);
        return stringBuilder.toString();
    }
}
