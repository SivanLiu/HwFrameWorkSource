package com.android.server.hidata.wavemapping.entity;

public class LocationInfo {
    private String DataImsi;
    private String cellCluster;
    private String dateType;
    private String id;
    private String mainAp;
    private String place;
    private String update;
    private String wifiCluster;

    public LocationInfo(String id, String place) {
        this.id = id;
        this.place = place;
    }

    public String getUpdate() {
        return this.update;
    }

    public void setUpdate(String update) {
        this.update = update;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlace() {
        return this.place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getDateType() {
        return this.dateType;
    }

    public void setDateType(String dateType) {
        this.dateType = dateType;
    }

    public String getMainAp() {
        return this.mainAp;
    }

    public void setMainAp(String mainAp) {
        this.mainAp = mainAp;
    }

    public String getWifiCluster() {
        return this.wifiCluster;
    }

    public void setWifiCluster(String wifiCluster) {
        this.wifiCluster = wifiCluster;
    }

    public String getCellCluster() {
        return this.cellCluster;
    }

    public void setCellCluster(String cellCluster) {
        this.cellCluster = cellCluster;
    }

    public String getDataImsi() {
        return this.DataImsi;
    }

    public void setDataImsi(String dataImsi) {
        this.DataImsi = dataImsi;
    }
}
