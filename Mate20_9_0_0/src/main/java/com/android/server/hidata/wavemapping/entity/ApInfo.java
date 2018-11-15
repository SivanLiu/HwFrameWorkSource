package com.android.server.hidata.wavemapping.entity;

public class ApInfo {
    private String mac = "";
    private int srcType;
    private String ssid = "";
    private String uptime = "";

    public ApInfo(String ssid, String mac, String uptime, int srcType) {
        this.ssid = ssid;
        this.uptime = uptime;
        this.mac = mac;
        this.srcType = srcType;
    }

    public ApInfo(String ssid, String mac, String uptime) {
        this.ssid = ssid;
        this.uptime = uptime;
        this.mac = mac;
    }

    public ApInfo(String ssid, String uptime) {
        this.ssid = ssid;
        this.uptime = uptime;
    }

    public int getSrcType() {
        return this.srcType;
    }

    public void setSrcType(int srcType) {
        this.srcType = srcType;
    }

    public String getUptime() {
        return this.uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public String getSsid() {
        return this.ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getMac() {
        return this.mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ApInfo{ssid='");
        stringBuilder.append(this.ssid);
        stringBuilder.append('\'');
        stringBuilder.append(", mac='");
        stringBuilder.append(this.mac);
        stringBuilder.append('\'');
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
