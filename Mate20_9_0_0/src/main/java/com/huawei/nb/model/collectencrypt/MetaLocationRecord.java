package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class MetaLocationRecord extends AManagedObject {
    public static final Creator<MetaLocationRecord> CREATOR = new Creator<MetaLocationRecord>() {
        public MetaLocationRecord createFromParcel(Parcel in) {
            return new MetaLocationRecord(in);
        }

        public MetaLocationRecord[] newArray(int size) {
            return new MetaLocationRecord[size];
        }
    };
    private Integer mCellID;
    private Integer mCellLAC;
    private Integer mCellRSSI;
    private Integer mId;
    private Double mLatitude;
    private Character mLocationType;
    private Double mLongitude;
    private Integer mReservedInt;
    private String mReservedText;
    private Date mTimeStamp;
    private String mWifiBSSID;
    private Integer mWifiLevel;

    public MetaLocationRecord(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mLocationType = cursor.isNull(3) ? null : Character.valueOf(cursor.getString(3).charAt(0));
        this.mLongitude = cursor.isNull(4) ? null : Double.valueOf(cursor.getDouble(4));
        this.mLatitude = cursor.isNull(5) ? null : Double.valueOf(cursor.getDouble(5));
        this.mCellID = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        this.mCellLAC = cursor.isNull(7) ? null : Integer.valueOf(cursor.getInt(7));
        this.mCellRSSI = cursor.isNull(8) ? null : Integer.valueOf(cursor.getInt(8));
        this.mWifiBSSID = cursor.getString(9);
        this.mWifiLevel = cursor.isNull(10) ? null : Integer.valueOf(cursor.getInt(10));
        if (!cursor.isNull(11)) {
            num = Integer.valueOf(cursor.getInt(11));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(12);
    }

    public MetaLocationRecord(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mLocationType = in.readByte() == (byte) 0 ? null : Character.valueOf(in.createCharArray()[0]);
        this.mLongitude = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mLatitude = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mCellID = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mCellLAC = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mCellRSSI = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mWifiBSSID = in.readByte() == (byte) 0 ? null : in.readString();
        this.mWifiLevel = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private MetaLocationRecord(Integer mId, Date mTimeStamp, Character mLocationType, Double mLongitude, Double mLatitude, Integer mCellID, Integer mCellLAC, Integer mCellRSSI, String mWifiBSSID, Integer mWifiLevel, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mLocationType = mLocationType;
        this.mLongitude = mLongitude;
        this.mLatitude = mLatitude;
        this.mCellID = mCellID;
        this.mCellLAC = mCellLAC;
        this.mCellRSSI = mCellRSSI;
        this.mWifiBSSID = mWifiBSSID;
        this.mWifiLevel = mWifiLevel;
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

    public Character getMLocationType() {
        return this.mLocationType;
    }

    public void setMLocationType(Character mLocationType) {
        this.mLocationType = mLocationType;
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

    public Integer getMCellID() {
        return this.mCellID;
    }

    public void setMCellID(Integer mCellID) {
        this.mCellID = mCellID;
        setValue();
    }

    public Integer getMCellLAC() {
        return this.mCellLAC;
    }

    public void setMCellLAC(Integer mCellLAC) {
        this.mCellLAC = mCellLAC;
        setValue();
    }

    public Integer getMCellRSSI() {
        return this.mCellRSSI;
    }

    public void setMCellRSSI(Integer mCellRSSI) {
        this.mCellRSSI = mCellRSSI;
        setValue();
    }

    public String getMWifiBSSID() {
        return this.mWifiBSSID;
    }

    public void setMWifiBSSID(String mWifiBSSID) {
        this.mWifiBSSID = mWifiBSSID;
        setValue();
    }

    public Integer getMWifiLevel() {
        return this.mWifiLevel;
    }

    public void setMWifiLevel(Integer mWifiLevel) {
        this.mWifiLevel = mWifiLevel;
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
        if (this.mLocationType != null) {
            out.writeByte((byte) 1);
            out.writeCharArray(new char[]{this.mLocationType.charValue()});
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
        if (this.mCellID != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mCellID.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCellLAC != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mCellLAC.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCellRSSI != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mCellRSSI.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mWifiBSSID != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mWifiBSSID);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mWifiLevel != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mWifiLevel.intValue());
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

    public AEntityHelper<MetaLocationRecord> getHelper() {
        return MetaLocationRecordHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.MetaLocationRecord";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("MetaLocationRecord { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mLocationType: ").append(this.mLocationType);
        sb.append(", mLongitude: ").append(this.mLongitude);
        sb.append(", mLatitude: ").append(this.mLatitude);
        sb.append(", mCellID: ").append(this.mCellID);
        sb.append(", mCellLAC: ").append(this.mCellLAC);
        sb.append(", mCellRSSI: ").append(this.mCellRSSI);
        sb.append(", mWifiBSSID: ").append(this.mWifiBSSID);
        sb.append(", mWifiLevel: ").append(this.mWifiLevel);
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
