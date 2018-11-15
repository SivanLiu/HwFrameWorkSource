package com.huawei.android.totemweather.aidl;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class RequestData implements Parcelable {
    public static final Creator<RequestData> CREATOR = new Creator<RequestData>() {
        public RequestData createFromParcel(Parcel p) {
            return new RequestData(p);
        }

        public RequestData[] newArray(int size) {
            return new RequestData[size];
        }
    };
    public static final int TYPE_HOME_CITY = 2;
    public static final int TYPE_HOME_CITY_FIRST = 3;
    public static final int TYPE_MY_LOCATION = 1;
    public static final int TYPE_MY_LOCATION_FIRST = 4;
    private boolean mAllDay = true;
    private String mCityId;
    private int mCityType = 2;
    private double mLatitude;
    private double mLongitude;
    private String mPackageName;
    private String mRequesetFlag;

    public RequestData(Parcel in) {
        readFromParcel(in);
    }

    public RequestData(Context context, double latitude, double longitude) {
        this.mPackageName = context.getPackageName();
        this.mRequesetFlag = latitude + "," + longitude;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
    }

    public RequestData(Context context, String cityId) {
        this.mPackageName = context.getPackageName();
        this.mRequesetFlag = cityId;
        this.mCityId = cityId;
    }

    public RequestData(Context context, int cityType) {
        this.mPackageName = context.getPackageName();
        this.mRequesetFlag = cityType + "";
        this.mCityType = cityType;
    }

    public String getmPackageName() {
        return this.mPackageName;
    }

    public void setmPackageName(String mPackageName) {
        this.mPackageName = mPackageName;
    }

    public String getmRequesetFlag() {
        return this.mRequesetFlag;
    }

    public void setmRequesetFlag(String mRequesetFlag) {
        this.mRequesetFlag = mRequesetFlag;
    }

    public String getmCityId() {
        return this.mCityId;
    }

    public void setmCityId(String mCityId) {
        this.mCityId = mCityId;
    }

    public double getmLatitude() {
        return this.mLatitude;
    }

    public void setmLatitude(double mLatitude) {
        this.mLatitude = mLatitude;
    }

    public double getmLongitude() {
        return this.mLongitude;
    }

    public void setmLongitude(double mLongitude) {
        this.mLongitude = mLongitude;
    }

    public int getmCityType() {
        return this.mCityType;
    }

    public void setmCityType(int mCityType) {
        this.mCityType = mCityType;
    }

    public boolean ismAllDay() {
        return this.mAllDay;
    }

    public void setmAllDay(boolean mAllDay) {
        this.mAllDay = mAllDay;
    }

    public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel in) {
        boolean z = true;
        this.mPackageName = in.readString();
        this.mRequesetFlag = in.readString();
        this.mCityId = in.readString();
        this.mLatitude = in.readDouble();
        this.mLongitude = in.readDouble();
        this.mCityType = in.readInt();
        if (in.readInt() != 1) {
            z = false;
        }
        this.mAllDay = z;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mPackageName);
        dest.writeString(this.mRequesetFlag);
        dest.writeString(this.mCityId);
        dest.writeDouble(this.mLatitude);
        dest.writeDouble(this.mLongitude);
        dest.writeInt(this.mCityType);
        dest.writeInt(this.mAllDay ? 1 : 0);
    }

    public String toString() {
        return "RequestData [mPackageName=" + this.mPackageName + ", mRequesetFlag=" + this.mRequesetFlag + ", mCityId=" + this.mCityId + ", mCityType=" + this.mCityType + ", mAllDay=" + this.mAllDay + "]";
    }
}
