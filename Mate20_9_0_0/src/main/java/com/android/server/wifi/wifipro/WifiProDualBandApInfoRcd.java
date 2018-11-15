package com.android.server.wifi.wifipro;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class WifiProDualBandApInfoRcd {
    private static final String TAG = "WifiProDualBandApInfoRcd";
    public String apBSSID;
    public int isInBlackList;
    public Short mApAuthType;
    public String mApSSID;
    public int mChannelFrequency;
    public int mDisappearCount;
    public Short mInetCapability;
    private List<WifiProRelateApRcd> mRelateApRcds = new ArrayList();
    public Short mServingBand;
    public long mUpdateTime;

    public WifiProDualBandApInfoRcd(String bssid) {
        resetAllParameters(bssid);
    }

    private void resetAllParameters(String bssid) {
        this.apBSSID = "DEAULT_STR";
        if (bssid != null) {
            this.apBSSID = bssid;
        }
        this.mApSSID = "DEAULT_STR";
        this.mInetCapability = Short.valueOf((short) 0);
        this.mServingBand = Short.valueOf((short) 0);
        this.mApAuthType = Short.valueOf((short) 0);
        this.mDisappearCount = 0;
        this.isInBlackList = 0;
        this.mChannelFrequency = 0;
        this.mUpdateTime = 0;
    }

    public List<WifiProRelateApRcd> getRelateApRcds() {
        return this.mRelateApRcds;
    }

    public void setRelateApRcds(List<WifiProRelateApRcd> relateApRcds) {
        this.mRelateApRcds = relateApRcds;
    }

    public void dumpAll() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("apBSSID:");
        stringBuilder.append(this.apBSSID);
        stringBuilder.append(", mApSSID:");
        stringBuilder.append(this.mApSSID);
        stringBuilder.append(", mInetCapability:");
        stringBuilder.append(this.mInetCapability);
        stringBuilder.append(", mServingBand:");
        stringBuilder.append(this.mServingBand);
        stringBuilder.append(", mApAuthType:");
        stringBuilder.append(this.mApAuthType);
        stringBuilder.append(", mChannelFrequency:");
        stringBuilder.append(this.mChannelFrequency);
        stringBuilder.append(", mDisappearCount:");
        stringBuilder.append(this.mDisappearCount);
        stringBuilder.append(", isInBlackList:");
        stringBuilder.append(this.isInBlackList);
        stringBuilder.append(", mUpdateTime:");
        stringBuilder.append(this.mUpdateTime);
        Log.d(str, stringBuilder.toString());
    }
}
