package com.android.server.wifi.wificond;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public class HiddenNetwork implements Parcelable {
    public static final Creator<HiddenNetwork> CREATOR = new Creator<HiddenNetwork>() {
        /* class com.android.server.wifi.wificond.HiddenNetwork.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public HiddenNetwork createFromParcel(Parcel in) {
            HiddenNetwork result = new HiddenNetwork();
            result.ssid = in.createByteArray();
            return result;
        }

        @Override // android.os.Parcelable.Creator
        public HiddenNetwork[] newArray(int size) {
            return new HiddenNetwork[size];
        }
    };
    private static final String TAG = "HiddenNetwork";
    public byte[] ssid;

    public boolean equals(Object rhs) {
        if (this == rhs) {
            return true;
        }
        if (!(rhs instanceof HiddenNetwork)) {
            return false;
        }
        return Arrays.equals(this.ssid, ((HiddenNetwork) rhs).ssid);
    }

    public int hashCode() {
        return Arrays.hashCode(this.ssid);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeByteArray(this.ssid);
    }
}
