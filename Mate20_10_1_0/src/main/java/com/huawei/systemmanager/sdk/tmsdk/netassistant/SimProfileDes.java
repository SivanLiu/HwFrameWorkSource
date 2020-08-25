package com.huawei.systemmanager.sdk.tmsdk.netassistant;

import android.os.Parcel;
import android.os.Parcelable;

public class SimProfileDes implements Parcelable {
    public static final Creator<SimProfileDes> CREATOR = new Creator<SimProfileDes>() {
        /* class com.huawei.systemmanager.sdk.tmsdk.netassistant.SimProfileDes.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public SimProfileDes createFromParcel(Parcel in) {
            return new SimProfileDes();
        }

        @Override // android.os.Parcelable.Creator
        public SimProfileDes[] newArray(int size) {
            return new SimProfileDes[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
    }
}
