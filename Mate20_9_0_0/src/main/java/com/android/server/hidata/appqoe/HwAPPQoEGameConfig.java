package com.android.server.hidata.appqoe;

public class HwAPPQoEGameConfig {
    public int mGameAction = -1;
    public int mGameId = -1;
    public int mGameKQI = -1;
    public String mGameName = HwAPPQoEUtils.INVALID_STRING_VALUE;
    public int mGameRtt = -1;
    public float mHistoryQoeBadTH = -1.0f;
    public String mReserved = HwAPPQoEUtils.INVALID_STRING_VALUE;
    public int mScenceId = -1;

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" HwAPPQoEGameConfig mGameName = ");
        stringBuilder.append(this.mGameName);
        stringBuilder.append(" mGameId = ");
        stringBuilder.append(this.mGameId);
        stringBuilder.append(" mGameId = ");
        stringBuilder.append(this.mGameId);
        stringBuilder.append(" mGameRtt = ");
        stringBuilder.append(this.mGameRtt);
        stringBuilder.append(" mGameKQI = ");
        stringBuilder.append(this.mGameKQI);
        stringBuilder.append(" mGameAction = ");
        stringBuilder.append(this.mGameAction);
        stringBuilder.append(" mHistoryQoeBadTH = ");
        stringBuilder.append(this.mHistoryQoeBadTH);
        stringBuilder.append(" mReserved = ");
        stringBuilder.append(this.mReserved);
        return stringBuilder.toString();
    }
}
