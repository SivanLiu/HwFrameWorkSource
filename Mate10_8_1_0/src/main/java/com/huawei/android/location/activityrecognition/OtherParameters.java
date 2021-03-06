package com.huawei.android.location.activityrecognition;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class OtherParameters implements Parcelable {
    public static final Creator<OtherParameters> CREATOR = new Creator<OtherParameters>() {
        public OtherParameters createFromParcel(Parcel source) {
            return new OtherParameters(source.readDouble(), source.readDouble(), source.readDouble(), source.readDouble(), source.readString());
        }

        public OtherParameters[] newArray(int size) {
            return new OtherParameters[size];
        }
    };
    private double mParam1;
    private double mParam2;
    private double mParam3;
    private double mParam4;
    private String mParam5;

    public OtherParameters(double param1, double param2, double param3, double param4, String param5) {
        this.mParam1 = param1;
        this.mParam2 = param2;
        this.mParam3 = param3;
        this.mParam4 = param4;
        this.mParam5 = param5;
    }

    public void setmParam1(double mParam1) {
        this.mParam1 = mParam1;
    }

    public void setmParam2(double mParam2) {
        this.mParam2 = mParam2;
    }

    public void setmParam3(double mParam3) {
        this.mParam3 = mParam3;
    }

    public void setmParam4(double mParam4) {
        this.mParam4 = mParam4;
    }

    public void setmParam5(String mParam5) {
        this.mParam5 = mParam5;
    }

    public double getmParam1() {
        return this.mParam1;
    }

    public double getmParam2() {
        return this.mParam2;
    }

    public double getmParam3() {
        return this.mParam3;
    }

    public double getmParam4() {
        return this.mParam4;
    }

    public String getmParam5() {
        return this.mParam5;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeDouble(this.mParam1);
        parcel.writeDouble(this.mParam2);
        parcel.writeDouble(this.mParam3);
        parcel.writeDouble(this.mParam4);
        parcel.writeString(this.mParam5);
    }

    public String toString() {
        return String.format("Param1=%s, Param2=%s, Param3=%s, Param4=%s, Param5=%s", new Object[]{Double.valueOf(this.mParam1), Double.valueOf(this.mParam2), Double.valueOf(this.mParam3), Double.valueOf(this.mParam4), this.mParam5});
    }
}
