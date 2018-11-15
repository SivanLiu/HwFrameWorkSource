package com.android.server.wifi.MSS;

public class HwMSSDatabaseItem implements Comparable {
    public String bssid;
    public int reasoncode;
    public String ssid;
    public long updatetime;

    public HwMSSDatabaseItem(String ssid, String bssid, int reasoncode) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.reasoncode = reasoncode;
    }

    public HwMSSDatabaseItem(String ssid, String bssid, int reasoncode, long updatetime) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.reasoncode = reasoncode;
        this.updatetime = updatetime;
    }

    public int compareTo(Object obj) {
        HwMSSDatabaseItem dataInfo = (HwMSSDatabaseItem) obj;
        int i = -1;
        if (dataInfo == null) {
            return -1;
        }
        long diff = dataInfo.updatetime - this.updatetime;
        if (diff > 0) {
            i = 1;
        } else if (diff >= 0) {
            i = 0;
        }
        return i;
    }

    public int hashCode() {
        return Long.valueOf(this.updatetime).hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (this.updatetime == ((HwMSSDatabaseItem) obj).updatetime) {
            return true;
        }
        return false;
    }
}
