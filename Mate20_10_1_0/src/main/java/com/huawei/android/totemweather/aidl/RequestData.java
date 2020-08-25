package com.huawei.android.totemweather.aidl;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

public final class RequestData implements Parcelable {
    public static final Creator<RequestData> CREATOR = new Creator<RequestData>() {
        /* class com.huawei.android.totemweather.aidl.RequestData.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public RequestData createFromParcel(Parcel p) {
            return new RequestData(p);
        }

        @Override // android.os.Parcelable.Creator
        public RequestData[] newArray(int size) {
            return new RequestData[size];
        }
    };
    public static final int TYPE_HOME_CITY = 2;
    public static final int TYPE_HOME_CITY_FIRST = 3;
    public static final int TYPE_MY_LOCATION = 1;
    public static final int TYPE_MY_LOCATION_FIRST = 4;
    private String mCityId;
    private int mCityType = 2;
    private boolean mIsAllDay = true;
    private double mLatitude;
    private double mLongitude;
    private String mPackageName;
    private String mRequestFlag;

    public RequestData() {
    }

    public RequestData(Parcel in) {
        readFromParcel(in);
    }

    public RequestData(Context context, double latitude, double longitude) {
        this.mPackageName = context.getPackageName();
        this.mRequestFlag = latitude + "," + longitude;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
    }

    public RequestData(Context context, String cityId) {
        this.mPackageName = context.getPackageName();
        this.mRequestFlag = cityId;
        this.mCityId = cityId;
    }

    public RequestData(Context context, int cityType) {
        this.mPackageName = context.getPackageName();
        this.mRequestFlag = cityType + "";
        this.mCityType = cityType;
    }

    public String getmPackageName() {
        return this.mPackageName;
    }

    public void setmPackageName(String packageName) {
        this.mPackageName = packageName;
    }

    public String getmRequestFlag() {
        return this.mRequestFlag;
    }

    public void setmRequestFlag(String requestFlag) {
        this.mRequestFlag = requestFlag;
    }

    public String getmCityId() {
        return this.mCityId;
    }

    public void setmCityId(String cityId) {
        this.mCityId = cityId;
    }

    public double getmLatitude() {
        return this.mLatitude;
    }

    public void setmLatitude(double latitude) {
        this.mLatitude = latitude;
    }

    public double getmLongitude() {
        return this.mLongitude;
    }

    public void setmLongitude(double longitude) {
        this.mLongitude = longitude;
    }

    public int getmCityType() {
        return this.mCityType;
    }

    public void setmCityType(int cityType) {
        this.mCityType = cityType;
    }

    public boolean ismAllDay() {
        return this.mIsAllDay;
    }

    public void setmAllDay(boolean isAllDay) {
        this.mIsAllDay = isAllDay;
    }

    public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel in) {
        boolean z = true;
        this.mPackageName = in.readString();
        this.mRequestFlag = in.readString();
        this.mCityId = in.readString();
        this.mLatitude = in.readDouble();
        this.mLongitude = in.readDouble();
        this.mCityType = in.readInt();
        if (in.readInt() != 1) {
            z = false;
        }
        this.mIsAllDay = z;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mPackageName);
        dest.writeString(this.mRequestFlag);
        dest.writeString(this.mCityId);
        dest.writeDouble(this.mLatitude);
        dest.writeDouble(this.mLongitude);
        dest.writeInt(this.mCityType);
        dest.writeInt(this.mIsAllDay ? 1 : 0);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("RequestData [mPackageName=").append(this.mPackageName).append(", mRequestFlag=").append(this.mRequestFlag).append(", mCityId=").append(this.mCityId).append(", mCityType=").append(this.mCityType).append(", mIsAllDay=").append(this.mIsAllDay).append("]");
        return stringBuilder.toString();
    }
}
