package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RawRecomIntegratedInfo extends AManagedObject {
    public static final Creator<RawRecomIntegratedInfo> CREATOR = new Creator<RawRecomIntegratedInfo>() {
        public RawRecomIntegratedInfo createFromParcel(Parcel in) {
            return new RawRecomIntegratedInfo(in);
        }

        public RawRecomIntegratedInfo[] newArray(int size) {
            return new RawRecomIntegratedInfo[size];
        }
    };
    private String mApkName;
    private Integer mArActivityType;
    private String mBatteryStatus;
    private Integer mCellId;
    private Integer mCurrentTemperature;
    private Date mDateTime;
    private Integer mHeadset;
    private Integer mId;
    private String mLatitude;
    private Integer mLocationType;
    private String mLongitude;
    private Integer mNetworkType;
    private Integer mReservedInt;
    private String mReservedText;
    private String mService;
    private Date mTimeStamp;
    private String mTotalTime;
    private Integer mWeatherIcon;
    private Integer mWeek;
    private String mWifiBssid;

    public RawRecomIntegratedInfo(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mDateTime = cursor.isNull(3) ? null : new Date(cursor.getLong(3));
        this.mApkName = cursor.getString(4);
        this.mArActivityType = cursor.isNull(5) ? null : Integer.valueOf(cursor.getInt(5));
        this.mHeadset = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        this.mWeek = cursor.isNull(7) ? null : Integer.valueOf(cursor.getInt(7));
        this.mNetworkType = cursor.isNull(8) ? null : Integer.valueOf(cursor.getInt(8));
        this.mLocationType = cursor.isNull(9) ? null : Integer.valueOf(cursor.getInt(9));
        this.mBatteryStatus = cursor.getString(10);
        this.mWeatherIcon = cursor.isNull(11) ? null : Integer.valueOf(cursor.getInt(11));
        this.mCurrentTemperature = cursor.isNull(12) ? null : Integer.valueOf(cursor.getInt(12));
        this.mLatitude = cursor.getString(13);
        this.mLongitude = cursor.getString(14);
        this.mTotalTime = cursor.getString(15);
        this.mCellId = cursor.isNull(16) ? null : Integer.valueOf(cursor.getInt(16));
        this.mWifiBssid = cursor.getString(17);
        this.mService = cursor.getString(18);
        if (!cursor.isNull(19)) {
            num = Integer.valueOf(cursor.getInt(19));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(20);
    }

    public RawRecomIntegratedInfo(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mDateTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mApkName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mArActivityType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mHeadset = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mWeek = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mNetworkType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mLocationType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mBatteryStatus = in.readByte() == (byte) 0 ? null : in.readString();
        this.mWeatherIcon = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mCurrentTemperature = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mLatitude = in.readByte() == (byte) 0 ? null : in.readString();
        this.mLongitude = in.readByte() == (byte) 0 ? null : in.readString();
        this.mTotalTime = in.readByte() == (byte) 0 ? null : in.readString();
        this.mCellId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mWifiBssid = in.readByte() == (byte) 0 ? null : in.readString();
        this.mService = in.readByte() == (byte) 0 ? null : in.readString();
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private RawRecomIntegratedInfo(Integer mId, Date mTimeStamp, Date mDateTime, String mApkName, Integer mArActivityType, Integer mHeadset, Integer mWeek, Integer mNetworkType, Integer mLocationType, String mBatteryStatus, Integer mWeatherIcon, Integer mCurrentTemperature, String mLatitude, String mLongitude, String mTotalTime, Integer mCellId, String mWifiBssid, String mService, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mDateTime = mDateTime;
        this.mApkName = mApkName;
        this.mArActivityType = mArActivityType;
        this.mHeadset = mHeadset;
        this.mWeek = mWeek;
        this.mNetworkType = mNetworkType;
        this.mLocationType = mLocationType;
        this.mBatteryStatus = mBatteryStatus;
        this.mWeatherIcon = mWeatherIcon;
        this.mCurrentTemperature = mCurrentTemperature;
        this.mLatitude = mLatitude;
        this.mLongitude = mLongitude;
        this.mTotalTime = mTotalTime;
        this.mCellId = mCellId;
        this.mWifiBssid = mWifiBssid;
        this.mService = mService;
        this.mReservedInt = mReservedInt;
        this.mReservedText = mReservedText;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getMId() {
        return this.mId;
    }

    public void setMId(Integer mId) {
        this.mId = mId;
        setValue();
    }

    public Date getMTimeStamp() {
        return this.mTimeStamp;
    }

    public void setMTimeStamp(Date mTimeStamp) {
        this.mTimeStamp = mTimeStamp;
        setValue();
    }

    public Date getMDateTime() {
        return this.mDateTime;
    }

    public void setMDateTime(Date mDateTime) {
        this.mDateTime = mDateTime;
        setValue();
    }

    public String getMApkName() {
        return this.mApkName;
    }

    public void setMApkName(String mApkName) {
        this.mApkName = mApkName;
        setValue();
    }

    public Integer getMArActivityType() {
        return this.mArActivityType;
    }

    public void setMArActivityType(Integer mArActivityType) {
        this.mArActivityType = mArActivityType;
        setValue();
    }

    public Integer getMHeadset() {
        return this.mHeadset;
    }

    public void setMHeadset(Integer mHeadset) {
        this.mHeadset = mHeadset;
        setValue();
    }

    public Integer getMWeek() {
        return this.mWeek;
    }

    public void setMWeek(Integer mWeek) {
        this.mWeek = mWeek;
        setValue();
    }

    public Integer getMNetworkType() {
        return this.mNetworkType;
    }

    public void setMNetworkType(Integer mNetworkType) {
        this.mNetworkType = mNetworkType;
        setValue();
    }

    public Integer getMLocationType() {
        return this.mLocationType;
    }

    public void setMLocationType(Integer mLocationType) {
        this.mLocationType = mLocationType;
        setValue();
    }

    public String getMBatteryStatus() {
        return this.mBatteryStatus;
    }

    public void setMBatteryStatus(String mBatteryStatus) {
        this.mBatteryStatus = mBatteryStatus;
        setValue();
    }

    public Integer getMWeatherIcon() {
        return this.mWeatherIcon;
    }

    public void setMWeatherIcon(Integer mWeatherIcon) {
        this.mWeatherIcon = mWeatherIcon;
        setValue();
    }

    public Integer getMCurrentTemperature() {
        return this.mCurrentTemperature;
    }

    public void setMCurrentTemperature(Integer mCurrentTemperature) {
        this.mCurrentTemperature = mCurrentTemperature;
        setValue();
    }

    public String getMLatitude() {
        return this.mLatitude;
    }

    public void setMLatitude(String mLatitude) {
        this.mLatitude = mLatitude;
        setValue();
    }

    public String getMLongitude() {
        return this.mLongitude;
    }

    public void setMLongitude(String mLongitude) {
        this.mLongitude = mLongitude;
        setValue();
    }

    public String getMTotalTime() {
        return this.mTotalTime;
    }

    public void setMTotalTime(String mTotalTime) {
        this.mTotalTime = mTotalTime;
        setValue();
    }

    public Integer getMCellId() {
        return this.mCellId;
    }

    public void setMCellId(Integer mCellId) {
        this.mCellId = mCellId;
        setValue();
    }

    public String getMWifiBssid() {
        return this.mWifiBssid;
    }

    public void setMWifiBssid(String mWifiBssid) {
        this.mWifiBssid = mWifiBssid;
        setValue();
    }

    public String getMService() {
        return this.mService;
    }

    public void setMService(String mService) {
        this.mService = mService;
        setValue();
    }

    public Integer getMReservedInt() {
        return this.mReservedInt;
    }

    public void setMReservedInt(Integer mReservedInt) {
        this.mReservedInt = mReservedInt;
        setValue();
    }

    public String getMReservedText() {
        return this.mReservedText;
    }

    public void setMReservedText(String mReservedText) {
        this.mReservedText = mReservedText;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.mId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mId.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.mTimeStamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mTimeStamp.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mDateTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mDateTime.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mApkName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mApkName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mArActivityType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mArActivityType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mHeadset != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mHeadset.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mWeek != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mWeek.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mNetworkType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mNetworkType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mLocationType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mLocationType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBatteryStatus != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mBatteryStatus);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mWeatherIcon != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mWeatherIcon.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCurrentTemperature != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mCurrentTemperature.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mLatitude != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mLatitude);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mLongitude != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mLongitude);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mTotalTime != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mTotalTime);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCellId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mCellId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mWifiBssid != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mWifiBssid);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mService != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mService);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReservedInt != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mReservedInt.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReservedText != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mReservedText);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<RawRecomIntegratedInfo> getHelper() {
        return RawRecomIntegratedInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RawRecomIntegratedInfo";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RawRecomIntegratedInfo { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mDateTime: ").append(this.mDateTime);
        sb.append(", mApkName: ").append(this.mApkName);
        sb.append(", mArActivityType: ").append(this.mArActivityType);
        sb.append(", mHeadset: ").append(this.mHeadset);
        sb.append(", mWeek: ").append(this.mWeek);
        sb.append(", mNetworkType: ").append(this.mNetworkType);
        sb.append(", mLocationType: ").append(this.mLocationType);
        sb.append(", mBatteryStatus: ").append(this.mBatteryStatus);
        sb.append(", mWeatherIcon: ").append(this.mWeatherIcon);
        sb.append(", mCurrentTemperature: ").append(this.mCurrentTemperature);
        sb.append(", mLatitude: ").append(this.mLatitude);
        sb.append(", mLongitude: ").append(this.mLongitude);
        sb.append(", mTotalTime: ").append(this.mTotalTime);
        sb.append(", mCellId: ").append(this.mCellId);
        sb.append(", mWifiBssid: ").append(this.mWifiBssid);
        sb.append(", mService: ").append(this.mService);
        sb.append(", mReservedInt: ").append(this.mReservedInt);
        sb.append(", mReservedText: ").append(this.mReservedText);
        sb.append(" }");
        return sb.toString();
    }

    public boolean equals(Object o) {
        return super.equals(o);
    }

    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.10";
    }

    public int getDatabaseVersionCode() {
        return 10;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
