package com.android.server.wifi;

public class PreconfiguredNetwork {
    private int eapMethod;
    private String ssid;

    public PreconfiguredNetwork(String ssid, int eapMethod) {
        this.ssid = ssid;
        this.eapMethod = eapMethod;
    }

    public String getSsid() {
        return this.ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public int getEapMethod() {
        return this.eapMethod;
    }

    public void setEapMethod(int eapMethod) {
        this.eapMethod = eapMethod;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PreconfiguredNetwork{ssid='");
        stringBuilder.append(this.ssid);
        stringBuilder.append('\'');
        stringBuilder.append(", eapMethod=");
        stringBuilder.append(this.eapMethod);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
