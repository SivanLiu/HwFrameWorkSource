package com.android.server.wifi.hotspot2;

import android.text.TextUtils;

public class ANQPNetworkKey {
    private final int mAnqpDomainID;
    private final long mBSSID;
    private final long mHESSID;
    private final String mSSID;

    public ANQPNetworkKey(String ssid, long bssid, long hessid, int anqpDomainID) {
        this.mSSID = ssid;
        this.mBSSID = bssid;
        this.mHESSID = hessid;
        this.mAnqpDomainID = anqpDomainID;
    }

    public static ANQPNetworkKey buildKey(String ssid, long bssid, long hessid, int anqpDomainId) {
        if (anqpDomainId == 0) {
            return new ANQPNetworkKey(ssid, bssid, 0, 0);
        }
        if (hessid != 0) {
            return new ANQPNetworkKey(null, 0, hessid, anqpDomainId);
        }
        return new ANQPNetworkKey(ssid, 0, 0, anqpDomainId);
    }

    public int hashCode() {
        if (this.mHESSID != 0) {
            return (int) (((((this.mHESSID >>> 32) * 31) + this.mHESSID) * 31) + ((long) this.mAnqpDomainID));
        }
        if (this.mBSSID != 0) {
            return (int) (((((long) (this.mSSID.hashCode() * 31)) + (this.mBSSID >>> 32)) * 31) + this.mBSSID);
        }
        return (this.mSSID.hashCode() * 31) + this.mAnqpDomainID;
    }

    public boolean equals(Object thatObject) {
        boolean z = true;
        if (thatObject == this) {
            return true;
        }
        if (!(thatObject instanceof ANQPNetworkKey)) {
            return false;
        }
        ANQPNetworkKey that = (ANQPNetworkKey) thatObject;
        if (!(TextUtils.equals(that.mSSID, this.mSSID) && that.mBSSID == this.mBSSID && that.mHESSID == this.mHESSID && that.mAnqpDomainID == this.mAnqpDomainID)) {
            z = false;
        }
        return z;
    }

    public String toString() {
        StringBuilder stringBuilder;
        if (this.mHESSID != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(Utils.macToString(this.mHESSID));
            stringBuilder.append(":");
            stringBuilder.append(this.mAnqpDomainID);
            return stringBuilder.toString();
        } else if (this.mBSSID != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(Utils.macToString(this.mBSSID));
            stringBuilder.append(":<");
            stringBuilder.append(this.mSSID);
            stringBuilder.append(">");
            return stringBuilder.toString();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("<");
            stringBuilder.append(this.mSSID);
            stringBuilder.append(">:");
            stringBuilder.append(this.mAnqpDomainID);
            return stringBuilder.toString();
        }
    }
}
