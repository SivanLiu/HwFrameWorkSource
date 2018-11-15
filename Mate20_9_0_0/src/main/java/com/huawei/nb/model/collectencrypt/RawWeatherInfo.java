package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RawWeatherInfo extends AManagedObject {
    public static final Creator<RawWeatherInfo> CREATOR = new Creator<RawWeatherInfo>() {
        public RawWeatherInfo createFromParcel(Parcel in) {
            return new RawWeatherInfo(in);
        }

        public RawWeatherInfo[] newArray(int size) {
            return new RawWeatherInfo[size];
        }
    };
    private Integer mId;
    private Double mLatitude;
    private Double mLongitude;
    private Integer mReservedInt;
    private String mReservedText;
    private Integer mTemprature;
    private Date mTimeStamp;
    private Integer mWeatherIcon;

    public RawWeatherInfo(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mLongitude = cursor.isNull(3) ? null : Double.valueOf(cursor.getDouble(3));
        this.mLatitude = cursor.isNull(4) ? null : Double.valueOf(cursor.getDouble(4));
        this.mWeatherIcon = cursor.isNull(5) ? null : Integer.valueOf(cursor.getInt(5));
        this.mTemprature = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        if (!cursor.isNull(7)) {
            num = Integer.valueOf(cursor.getInt(7));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(8);
    }

    public RawWeatherInfo(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mLongitude = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mLatitude = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mWeatherIcon = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mTemprature = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private RawWeatherInfo(Integer mId, Date mTimeStamp, Double mLongitude, Double mLatitude, Integer mWeatherIcon, Integer mTemprature, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mLongitude = mLongitude;
        this.mLatitude = mLatitude;
        this.mWeatherIcon = mWeatherIcon;
        this.mTemprature = mTemprature;
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

    public Double getMLongitude() {
        return this.mLongitude;
    }

    public void setMLongitude(Double mLongitude) {
        this.mLongitude = mLongitude;
        setValue();
    }

    public Double getMLatitude() {
        return this.mLatitude;
    }

    public void setMLatitude(Double mLatitude) {
        this.mLatitude = mLatitude;
        setValue();
    }

    public Integer getMWeatherIcon() {
        return this.mWeatherIcon;
    }

    public void setMWeatherIcon(Integer mWeatherIcon) {
        this.mWeatherIcon = mWeatherIcon;
        setValue();
    }

    public Integer getMTemprature() {
        return this.mTemprature;
    }

    public void setMTemprature(Integer mTemprature) {
        this.mTemprature = mTemprature;
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
        if (this.mLongitude != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mLongitude.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mLatitude != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mLatitude.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mWeatherIcon != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mWeatherIcon.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mTemprature != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mTemprature.intValue());
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

    public AEntityHelper<RawWeatherInfo> getHelper() {
        return RawWeatherInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RawWeatherInfo";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RawWeatherInfo { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mLongitude: ").append(this.mLongitude);
        sb.append(", mLatitude: ").append(this.mLatitude);
        sb.append(", mWeatherIcon: ").append(this.mWeatherIcon);
        sb.append(", mTemprature: ").append(this.mTemprature);
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
