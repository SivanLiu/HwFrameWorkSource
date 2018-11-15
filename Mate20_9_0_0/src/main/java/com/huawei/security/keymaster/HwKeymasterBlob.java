package com.huawei.security.keymaster;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class HwKeymasterBlob implements Parcelable {
    public static final Creator<HwKeymasterBlob> CREATOR = new Creator<HwKeymasterBlob>() {
        public HwKeymasterBlob createFromParcel(Parcel in) {
            return new HwKeymasterBlob(in);
        }

        public HwKeymasterBlob[] newArray(int length) {
            return new HwKeymasterBlob[length];
        }
    };
    public byte[] blob;

    public HwKeymasterBlob(byte[] blob) {
        this.blob = blob;
    }

    protected HwKeymasterBlob(Parcel in) {
        this.blob = in.createByteArray();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeByteArray(this.blob);
    }
}
