package com.android.server.hidata.wavemapping.entity;

public class IdentifyResult implements Comparable<IdentifyResult> {
    private int batch;
    private String bssid;
    private int dist = 0;
    private String modelName;
    private int preLabel = 0;
    private String result;
    private String serveMac;
    private String ssid;

    public String getSsid() {
        return this.ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getServeMac() {
        return this.serveMac;
    }

    public void setServeMac(String serveMac) {
        this.serveMac = serveMac;
    }

    public String getBssid() {
        return this.bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public int getBatch() {
        return this.batch;
    }

    public void setBatch(int batch) {
        this.batch = batch;
    }

    public String getResult() {
        return this.result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getDist() {
        return this.dist;
    }

    public void setDist(int dist) {
        this.dist = dist;
    }

    public int getPreLabel() {
        return this.preLabel;
    }

    public void setPreLabel(int preLabel) {
        this.preLabel = preLabel;
    }

    public String getModelName() {
        return this.modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int compareTo(IdentifyResult s) {
        int num = new Integer(this.dist).compareTo(new Integer(s.dist));
        if (num == 0) {
            return new Integer(this.preLabel).compareTo(new Integer(s.preLabel));
        }
        return num;
    }

    public IdentifyResult(int batch, int preLabel, int dist) {
        this.batch = batch;
        this.preLabel = preLabel;
        this.dist = 0;
    }

    public IdentifyResult(int preLabel) {
        this.preLabel = preLabel;
    }

    public IdentifyResult(int dist, int preLabel) {
        this.dist = dist;
        this.preLabel = preLabel;
    }
}
