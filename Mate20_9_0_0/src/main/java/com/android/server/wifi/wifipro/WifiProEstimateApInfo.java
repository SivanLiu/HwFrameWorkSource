package com.android.server.wifi.wifipro;

public class WifiProEstimateApInfo {
    private String mApBssid = null;
    private int mApRssi = 0;
    private String mApSsid = null;
    private int mAuthType = 0;
    private boolean mIs5GAP = true;
    private int mIsDualbandAP = 0;
    private int mRetHistoryScore = 0;
    private int mRetRssiTH = 0;

    WifiProEstimateApInfo() {
    }

    public String toString() {
        String retStr = new StringBuilder();
        retStr.append("mApBssid: *** , mApSsid:");
        retStr.append(this.mApSsid);
        retStr.append(", mAuthType:");
        retStr.append(this.mAuthType);
        retStr.append(", mApRssi:");
        retStr.append(this.mApRssi);
        retStr.append(", mRetRssiTH:");
        retStr.append(this.mRetRssiTH);
        retStr.append(", mRetHistoryScore:");
        retStr.append(this.mRetHistoryScore);
        retStr.append(", mIsDualbandAP:");
        retStr.append(this.mIsDualbandAP);
        retStr.append(", mIs5GAP:");
        retStr.append(this.mIs5GAP);
        return retStr.toString();
    }

    public String getApBssid() {
        return this.mApBssid;
    }

    public void setApBssid(String bssid) {
        this.mApBssid = bssid;
    }

    public void setEstimateApSsid(String ssid) {
        this.mApSsid = ssid;
    }

    public String getApSsid() {
        return this.mApSsid;
    }

    public int getApAuthType() {
        return this.mAuthType;
    }

    public void setApAuthType(int authType) {
        this.mAuthType = authType;
    }

    public int getApRssi() {
        return this.mApRssi;
    }

    public void setApRssi(int apRssi) {
        this.mApRssi = apRssi;
    }

    public int getRetRssiTH() {
        return this.mRetRssiTH;
    }

    public void setRetRssiTH(int rssiTH) {
        this.mRetRssiTH = rssiTH;
    }

    public int getRetHistoryScore() {
        return this.mRetHistoryScore;
    }

    public void setRetHistoryScore(int retScore) {
        this.mRetHistoryScore = retScore;
    }

    public int getDualbandAPType() {
        return this.mIsDualbandAP;
    }

    public void setDualbandAPType(int dualbandap) {
        this.mIsDualbandAP = dualbandap;
    }

    public boolean is5GAP() {
        return this.mIs5GAP;
    }

    public void set5GAP(boolean is5GAP) {
        this.mIs5GAP = is5GAP;
    }
}
