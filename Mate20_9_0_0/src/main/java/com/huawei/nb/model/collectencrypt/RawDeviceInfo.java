package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RawDeviceInfo extends AManagedObject {
    public static final Creator<RawDeviceInfo> CREATOR = new Creator<RawDeviceInfo>() {
        public RawDeviceInfo createFromParcel(Parcel in) {
            return new RawDeviceInfo(in);
        }

        public RawDeviceInfo[] newArray(int size) {
            return new RawDeviceInfo[size];
        }
    };
    private String mDeviceName;
    private String mHardwareVer;
    private String mIMEI1;
    private String mIMEI2;
    private String mIMSI1;
    private String mIMSI2;
    private Integer mId;
    private String mLanguageRegion;
    private String mPhoneNum;
    private Integer mReservedInt;
    private String mReservedText;
    private String mSN;
    private String mSoftwareVer;
    private Date mTimeStamp;

    public RawDeviceInfo(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mDeviceName = cursor.getString(3);
        this.mHardwareVer = cursor.getString(4);
        this.mSoftwareVer = cursor.getString(5);
        this.mIMEI1 = cursor.getString(6);
        this.mIMEI2 = cursor.getString(7);
        this.mIMSI1 = cursor.getString(8);
        this.mIMSI2 = cursor.getString(9);
        this.mSN = cursor.getString(10);
        this.mLanguageRegion = cursor.getString(11);
        if (!cursor.isNull(12)) {
            num = Integer.valueOf(cursor.getInt(12));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(13);
        this.mPhoneNum = cursor.getString(14);
    }

    public RawDeviceInfo(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mDeviceName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mHardwareVer = in.readByte() == (byte) 0 ? null : in.readString();
        this.mSoftwareVer = in.readByte() == (byte) 0 ? null : in.readString();
        this.mIMEI1 = in.readByte() == (byte) 0 ? null : in.readString();
        this.mIMEI2 = in.readByte() == (byte) 0 ? null : in.readString();
        this.mIMSI1 = in.readByte() == (byte) 0 ? null : in.readString();
        this.mIMSI2 = in.readByte() == (byte) 0 ? null : in.readString();
        this.mSN = in.readByte() == (byte) 0 ? null : in.readString();
        this.mLanguageRegion = in.readByte() == (byte) 0 ? null : in.readString();
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedText = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mPhoneNum = str;
    }

    private RawDeviceInfo(Integer mId, Date mTimeStamp, String mDeviceName, String mHardwareVer, String mSoftwareVer, String mIMEI1, String mIMEI2, String mIMSI1, String mIMSI2, String mSN, String mLanguageRegion, Integer mReservedInt, String mReservedText, String mPhoneNum) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mDeviceName = mDeviceName;
        this.mHardwareVer = mHardwareVer;
        this.mSoftwareVer = mSoftwareVer;
        this.mIMEI1 = mIMEI1;
        this.mIMEI2 = mIMEI2;
        this.mIMSI1 = mIMSI1;
        this.mIMSI2 = mIMSI2;
        this.mSN = mSN;
        this.mLanguageRegion = mLanguageRegion;
        this.mReservedInt = mReservedInt;
        this.mReservedText = mReservedText;
        this.mPhoneNum = mPhoneNum;
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

    public String getMDeviceName() {
        return this.mDeviceName;
    }

    public void setMDeviceName(String mDeviceName) {
        this.mDeviceName = mDeviceName;
        setValue();
    }

    public String getMHardwareVer() {
        return this.mHardwareVer;
    }

    public void setMHardwareVer(String mHardwareVer) {
        this.mHardwareVer = mHardwareVer;
        setValue();
    }

    public String getMSoftwareVer() {
        return this.mSoftwareVer;
    }

    public void setMSoftwareVer(String mSoftwareVer) {
        this.mSoftwareVer = mSoftwareVer;
        setValue();
    }

    public String getMIMEI1() {
        return this.mIMEI1;
    }

    public void setMIMEI1(String mIMEI1) {
        this.mIMEI1 = mIMEI1;
        setValue();
    }

    public String getMIMEI2() {
        return this.mIMEI2;
    }

    public void setMIMEI2(String mIMEI2) {
        this.mIMEI2 = mIMEI2;
        setValue();
    }

    public String getMIMSI1() {
        return this.mIMSI1;
    }

    public void setMIMSI1(String mIMSI1) {
        this.mIMSI1 = mIMSI1;
        setValue();
    }

    public String getMIMSI2() {
        return this.mIMSI2;
    }

    public void setMIMSI2(String mIMSI2) {
        this.mIMSI2 = mIMSI2;
        setValue();
    }

    public String getMSN() {
        return this.mSN;
    }

    public void setMSN(String mSN) {
        this.mSN = mSN;
        setValue();
    }

    public String getMLanguageRegion() {
        return this.mLanguageRegion;
    }

    public void setMLanguageRegion(String mLanguageRegion) {
        this.mLanguageRegion = mLanguageRegion;
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

    public String getMPhoneNum() {
        return this.mPhoneNum;
    }

    public void setMPhoneNum(String mPhoneNum) {
        this.mPhoneNum = mPhoneNum;
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
        if (this.mDeviceName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mDeviceName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mHardwareVer != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mHardwareVer);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSoftwareVer != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mSoftwareVer);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mIMEI1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mIMEI1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mIMEI2 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mIMEI2);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mIMSI1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mIMSI1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mIMSI2 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mIMSI2);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSN != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mSN);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mLanguageRegion != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mLanguageRegion);
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
        if (this.mPhoneNum != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mPhoneNum);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<RawDeviceInfo> getHelper() {
        return RawDeviceInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RawDeviceInfo";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RawDeviceInfo { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mDeviceName: ").append(this.mDeviceName);
        sb.append(", mHardwareVer: ").append(this.mHardwareVer);
        sb.append(", mSoftwareVer: ").append(this.mSoftwareVer);
        sb.append(", mIMEI1: ").append(this.mIMEI1);
        sb.append(", mIMEI2: ").append(this.mIMEI2);
        sb.append(", mIMSI1: ").append(this.mIMSI1);
        sb.append(", mIMSI2: ").append(this.mIMSI2);
        sb.append(", mSN: ").append(this.mSN);
        sb.append(", mLanguageRegion: ").append(this.mLanguageRegion);
        sb.append(", mReservedInt: ").append(this.mReservedInt);
        sb.append(", mReservedText: ").append(this.mReservedText);
        sb.append(", mPhoneNum: ").append(this.mPhoneNum);
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
        return "0.0.2";
    }

    public int getEntityVersionCode() {
        return 2;
    }
}
