package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RawLocationRecord extends AManagedObject {
    public static final Creator<RawLocationRecord> CREATOR = new Creator<RawLocationRecord>() {
        public RawLocationRecord createFromParcel(Parcel in) {
            return new RawLocationRecord(in);
        }

        public RawLocationRecord[] newArray(int size) {
            return new RawLocationRecord[size];
        }
    };
    private String geodeticSystem;
    private Double mAltitude;
    private Integer mCellID;
    private Integer mCellLAC;
    private Integer mCellMCC;
    private Integer mCellMNC;
    private Integer mCellRSSI;
    private String mCity;
    private String mCountry;
    private String mDetailAddress;
    private String mDistrict;
    private Integer mId;
    private Double mLatitude;
    private Character mLocationType;
    private Double mLongitude;
    private String mProvince;
    private Integer mReservedInt;
    private String mReservedText;
    private Date mTimeStamp;
    private String mWifiBSSID;
    private Integer mWifiLevel;

    public RawLocationRecord(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mLocationType = cursor.isNull(3) ? null : Character.valueOf(cursor.getString(3).charAt(0));
        this.mLongitude = cursor.isNull(4) ? null : Double.valueOf(cursor.getDouble(4));
        this.mLatitude = cursor.isNull(5) ? null : Double.valueOf(cursor.getDouble(5));
        this.mAltitude = cursor.isNull(6) ? null : Double.valueOf(cursor.getDouble(6));
        this.mCity = cursor.getString(7);
        this.mCountry = cursor.getString(8);
        this.mDetailAddress = cursor.getString(9);
        this.mDistrict = cursor.getString(10);
        this.mProvince = cursor.getString(11);
        this.mCellID = cursor.isNull(12) ? null : Integer.valueOf(cursor.getInt(12));
        this.mCellMCC = cursor.isNull(13) ? null : Integer.valueOf(cursor.getInt(13));
        this.mCellMNC = cursor.isNull(14) ? null : Integer.valueOf(cursor.getInt(14));
        this.mCellLAC = cursor.isNull(15) ? null : Integer.valueOf(cursor.getInt(15));
        this.mCellRSSI = cursor.isNull(16) ? null : Integer.valueOf(cursor.getInt(16));
        this.mWifiBSSID = cursor.getString(17);
        this.mWifiLevel = cursor.isNull(18) ? null : Integer.valueOf(cursor.getInt(18));
        if (!cursor.isNull(19)) {
            num = Integer.valueOf(cursor.getInt(19));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(20);
        this.geodeticSystem = cursor.getString(21);
    }

    public RawLocationRecord(Parcel in) {
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
        this.mAltitude = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mCity = in.readByte() == (byte) 0 ? null : in.readString();
        this.mCountry = in.readByte() == (byte) 0 ? null : in.readString();
        this.mDetailAddress = in.readByte() == (byte) 0 ? null : in.readString();
        this.mDistrict = in.readByte() == (byte) 0 ? null : in.readString();
        this.mProvince = in.readByte() == (byte) 0 ? null : in.readString();
        this.mCellID = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mCellMCC = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mCellMNC = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mCellLAC = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mCellRSSI = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mWifiBSSID = in.readByte() == (byte) 0 ? null : in.readString();
        this.mWifiLevel = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedText = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.geodeticSystem = str;
    }

    private RawLocationRecord(Integer mId, Date mTimeStamp, Character mLocationType, Double mLongitude, Double mLatitude, Double mAltitude, String mCity, String mCountry, String mDetailAddress, String mDistrict, String mProvince, Integer mCellID, Integer mCellMCC, Integer mCellMNC, Integer mCellLAC, Integer mCellRSSI, String mWifiBSSID, Integer mWifiLevel, Integer mReservedInt, String mReservedText, String geodeticSystem) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mLocationType = mLocationType;
        this.mLongitude = mLongitude;
        this.mLatitude = mLatitude;
        this.mAltitude = mAltitude;
        this.mCity = mCity;
        this.mCountry = mCountry;
        this.mDetailAddress = mDetailAddress;
        this.mDistrict = mDistrict;
        this.mProvince = mProvince;
        this.mCellID = mCellID;
        this.mCellMCC = mCellMCC;
        this.mCellMNC = mCellMNC;
        this.mCellLAC = mCellLAC;
        this.mCellRSSI = mCellRSSI;
        this.mWifiBSSID = mWifiBSSID;
        this.mWifiLevel = mWifiLevel;
        this.mReservedInt = mReservedInt;
        this.mReservedText = mReservedText;
        this.geodeticSystem = geodeticSystem;
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

    public Double getMAltitude() {
        return this.mAltitude;
    }

    public void setMAltitude(Double mAltitude) {
        this.mAltitude = mAltitude;
        setValue();
    }

    public String getMCity() {
        return this.mCity;
    }

    public void setMCity(String mCity) {
        this.mCity = mCity;
        setValue();
    }

    public String getMCountry() {
        return this.mCountry;
    }

    public void setMCountry(String mCountry) {
        this.mCountry = mCountry;
        setValue();
    }

    public String getMDetailAddress() {
        return this.mDetailAddress;
    }

    public void setMDetailAddress(String mDetailAddress) {
        this.mDetailAddress = mDetailAddress;
        setValue();
    }

    public String getMDistrict() {
        return this.mDistrict;
    }

    public void setMDistrict(String mDistrict) {
        this.mDistrict = mDistrict;
        setValue();
    }

    public String getMProvince() {
        return this.mProvince;
    }

    public void setMProvince(String mProvince) {
        this.mProvince = mProvince;
        setValue();
    }

    public Integer getMCellID() {
        return this.mCellID;
    }

    public void setMCellID(Integer mCellID) {
        this.mCellID = mCellID;
        setValue();
    }

    public Integer getMCellMCC() {
        return this.mCellMCC;
    }

    public void setMCellMCC(Integer mCellMCC) {
        this.mCellMCC = mCellMCC;
        setValue();
    }

    public Integer getMCellMNC() {
        return this.mCellMNC;
    }

    public void setMCellMNC(Integer mCellMNC) {
        this.mCellMNC = mCellMNC;
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

    public String getGeodeticSystem() {
        return this.geodeticSystem;
    }

    public void setGeodeticSystem(String geodeticSystem) {
        this.geodeticSystem = geodeticSystem;
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
        if (this.mAltitude != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mAltitude.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCity != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mCity);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCountry != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mCountry);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mDetailAddress != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mDetailAddress);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mDistrict != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mDistrict);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mProvince != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mProvince);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCellID != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mCellID.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCellMCC != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mCellMCC.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCellMNC != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mCellMNC.intValue());
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
        } else {
            out.writeByte((byte) 0);
        }
        if (this.geodeticSystem != null) {
            out.writeByte((byte) 1);
            out.writeString(this.geodeticSystem);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<RawLocationRecord> getHelper() {
        return RawLocationRecordHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RawLocationRecord";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RawLocationRecord { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mLocationType: ").append(this.mLocationType);
        sb.append(", mLongitude: ").append(this.mLongitude);
        sb.append(", mLatitude: ").append(this.mLatitude);
        sb.append(", mAltitude: ").append(this.mAltitude);
        sb.append(", mCity: ").append(this.mCity);
        sb.append(", mCountry: ").append(this.mCountry);
        sb.append(", mDetailAddress: ").append(this.mDetailAddress);
        sb.append(", mDistrict: ").append(this.mDistrict);
        sb.append(", mProvince: ").append(this.mProvince);
        sb.append(", mCellID: ").append(this.mCellID);
        sb.append(", mCellMCC: ").append(this.mCellMCC);
        sb.append(", mCellMNC: ").append(this.mCellMNC);
        sb.append(", mCellLAC: ").append(this.mCellLAC);
        sb.append(", mCellRSSI: ").append(this.mCellRSSI);
        sb.append(", mWifiBSSID: ").append(this.mWifiBSSID);
        sb.append(", mWifiLevel: ").append(this.mWifiLevel);
        sb.append(", mReservedInt: ").append(this.mReservedInt);
        sb.append(", mReservedText: ").append(this.mReservedText);
        sb.append(", geodeticSystem: ").append(this.geodeticSystem);
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
        return "0.0.3";
    }

    public int getEntityVersionCode() {
        return 3;
    }
}
