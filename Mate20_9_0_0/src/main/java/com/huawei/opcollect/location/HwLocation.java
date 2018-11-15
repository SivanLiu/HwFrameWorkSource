package com.huawei.opcollect.location;

public class HwLocation implements Cloneable {
    private int accuracy;
    private double altitude;
    private String city;
    private String cityCode;
    private String country;
    private String detailAddress;
    private String district;
    private double latitude = 0.0d;
    private double longitude = 0.0d;
    private int priority;
    private String provider;
    private String province;
    private long timestamp = -1;

    public HwLocation(double longitude, double latitude, double altitude, int priority, String provider, String city, String country, String detailAddress, String district, String province, String cityCode) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.priority = priority;
        this.provider = provider;
        this.city = city;
        this.country = country;
        this.detailAddress = detailAddress;
        this.district = district;
        this.province = province;
        this.cityCode = cityCode;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getAltitude() {
        return this.altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCity() {
        return this.city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDetailAddress() {
        return this.detailAddress;
    }

    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public String getDistrict() {
        return this.district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getProvince() {
        return this.province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCityCode() {
        return this.cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public int getAccuracy() {
        return this.accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public HwLocation clone() throws CloneNotSupportedException {
        return (HwLocation) super.clone();
    }
}
