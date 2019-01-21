package android.net;

import android.annotation.SystemApi;
import android.net.wifi.ParcelUtil;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Objects;
import java.util.regex.Pattern;

@SystemApi
public class WifiKey implements Parcelable {
    private static final Pattern BSSID_PATTERN = Pattern.compile("([\\p{XDigit}]{2}:){5}[\\p{XDigit}]{2}");
    public static final Creator<WifiKey> CREATOR = new Creator<WifiKey>() {
        public WifiKey createFromParcel(Parcel in) {
            return new WifiKey(in, null);
        }

        public WifiKey[] newArray(int size) {
            return new WifiKey[size];
        }
    };
    private static final Pattern SSID_PATTERN = Pattern.compile("(\".*\")|(0x[\\p{XDigit}]+)", 32);
    public final String bssid;
    public final String ssid;

    public WifiKey(String ssid, String bssid) {
        StringBuilder stringBuilder;
        if (ssid == null || !SSID_PATTERN.matcher(ssid).matches()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid ssid: ");
            stringBuilder.append(ssid);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (bssid == null || !BSSID_PATTERN.matcher(bssid).matches()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid bssid: ");
            stringBuilder.append(bssid);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            this.ssid = ssid;
            this.bssid = bssid;
        }
    }

    private WifiKey(Parcel in) {
        this.ssid = in.readString();
        this.bssid = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.ssid);
        out.writeString(this.bssid);
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WifiKey wifiKey = (WifiKey) o;
        if (!(Objects.equals(this.ssid, wifiKey.ssid) && Objects.equals(this.bssid, wifiKey.bssid))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.ssid, this.bssid});
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WifiKey[SSID=");
        stringBuilder.append(this.ssid);
        stringBuilder.append(",BSSID=");
        stringBuilder.append(ParcelUtil.safeDisplayMac(this.bssid));
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
