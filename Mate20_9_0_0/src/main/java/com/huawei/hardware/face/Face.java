package com.huawei.hardware.face;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class Face implements Parcelable {
    public static final Creator<Face> CREATOR = new Creator<Face>() {
        public Face createFromParcel(Parcel in) {
            return new Face(in, null);
        }

        public Face[] newArray(int size) {
            return new Face[size];
        }
    };
    private long mDeviceId;

    /* synthetic */ Face(Parcel x0, AnonymousClass1 x1) {
        this(x0);
    }

    public Face(long deviceId) {
        this.mDeviceId = deviceId;
    }

    private Face(Parcel in) {
        this.mDeviceId = in.readLong();
    }

    public long getDeviceId() {
        return this.mDeviceId;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(this.mDeviceId);
    }
}
