package com.android.server.wifi.wifipro;

public class WifiProEnterpriseApRecord {
    private static final String TAG = "WifiProEnterpriseApRecord";
    public String apSSID;
    public int apSecurityType;

    public WifiProEnterpriseApRecord(String ssid, int secType) {
        resetAllParameters(ssid, secType);
    }

    private void resetAllParameters(String ssid, int secType) {
        this.apSSID = "";
        if (ssid != null) {
            this.apSSID = ssid;
        }
        this.apSecurityType = secType;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("apSSID =");
        stringBuilder.append(this.apSSID);
        stringBuilder.append(", apSecurityType=");
        stringBuilder.append(this.apSecurityType);
        return stringBuilder.toString();
    }
}
