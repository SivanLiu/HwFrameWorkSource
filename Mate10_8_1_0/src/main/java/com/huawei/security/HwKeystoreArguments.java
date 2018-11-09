package com.huawei.security;

import android.os.Parcel;
import android.os.Parcelable.Creator;

public class HwKeystoreArguments {
    public static final Creator<HwKeystoreArguments> CREATOR = new Creator<HwKeystoreArguments>() {
        public HwKeystoreArguments createFromParcel(Parcel in) {
            return new HwKeystoreArguments(in);
        }

        public HwKeystoreArguments[] newArray(int size) {
            return new HwKeystoreArguments[size];
        }
    };
    public byte[][] args;

    public HwKeystoreArguments() {
        this.args = null;
    }

    public HwKeystoreArguments(byte[][] args) {
        this.args = args;
    }

    private HwKeystoreArguments(Parcel in) {
        readFromParcel(in);
    }

    public void writeToParcel(Parcel out, int flags) {
        int i = 0;
        if (this.args == null) {
            out.writeInt(0);
            return;
        }
        out.writeInt(this.args.length);
        byte[][] bArr = this.args;
        int length = bArr.length;
        while (i < length) {
            out.writeByteArray(bArr[i]);
            i++;
        }
    }

    private void readFromParcel(Parcel in) {
        int length = in.readInt();
        this.args = new byte[length][];
        for (int i = 0; i < length; i++) {
            this.args[i] = in.createByteArray();
        }
    }

    public int describeContents() {
        return 0;
    }
}
