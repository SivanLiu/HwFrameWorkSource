package com.android.server.wifi.wifipro.hwintelligencewifi;

public class LocationAddress {
    private double distanceFromHome;
    private boolean isInvalid;
    private boolean isOversea;
    private double latitude;
    private double longitude;
    private long updateTime;

    public LocationAddress() {
        this(-1.0d, -1.0d);
    }

    public LocationAddress(double lat, double lng) {
        this(lat, lng, Long.valueOf(System.currentTimeMillis()));
    }

    public LocationAddress(double lat, double lng, Long time) {
        this(lat, lng, -1.0d, time);
    }

    public LocationAddress(double lat, double lng, double distance, Long time) {
        boolean z = lat < 0.0d || lng < 0.0d || (lat == 0.0d && lng == 0.0d);
        boolean z2 = z;
        this(lat, lng, distance, false, z2, time);
    }

    public LocationAddress(double lat, double lng, double distance, boolean isOversea, boolean isInvalid, Long time) {
        this.latitude = lat;
        this.longitude = lng;
        this.distanceFromHome = distance;
        this.isOversea = isOversea;
        this.isInvalid = isInvalid;
        this.updateTime = time.longValue();
    }

    public boolean isHome() {
        return this.distanceFromHome >= 0.0d && this.distanceFromHome < 200.0d;
    }

    public long getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(long time) {
        this.updateTime = time;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setinvalid(boolean invalid) {
        this.isInvalid = invalid;
    }

    public boolean isInvalid() {
        return this.isInvalid || (this.latitude <= 0.0d && this.longitude <= 0.0d);
    }

    public void setOversea(boolean Oversea) {
        this.isOversea = Oversea;
    }

    public boolean isOversea() {
        return this.isOversea;
    }

    public String toString() {
        String str = new StringBuilder();
        str.append("  latitude: ");
        str.append(this.latitude);
        str.append(",  longitude: ");
        str.append(this.longitude);
        str.append(",  distanceFromHome: ");
        str.append(this.distanceFromHome);
        str.append(",  updateTime: ");
        str.append(this.updateTime);
        str.append(",  isInvalid: ");
        str.append(this.isInvalid);
        str.append(",  isOversea: ");
        str.append(this.isOversea);
        return str.toString();
    }
}
