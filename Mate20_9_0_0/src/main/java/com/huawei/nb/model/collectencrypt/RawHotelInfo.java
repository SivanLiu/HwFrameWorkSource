package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RawHotelInfo extends AManagedObject {
    public static final Creator<RawHotelInfo> CREATOR = new Creator<RawHotelInfo>() {
        public RawHotelInfo createFromParcel(Parcel in) {
            return new RawHotelInfo(in);
        }

        public RawHotelInfo[] newArray(int size) {
            return new RawHotelInfo[size];
        }
    };
    private Date mCheckinTime;
    private String mHotelAddr;
    private String mHotelName;
    private String mHotelTelNo;
    private Integer mId;
    private Integer mReservedInt;
    private String mReservedText;
    private Date mTimeStamp;

    public RawHotelInfo(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mHotelTelNo = cursor.getString(3);
        this.mHotelAddr = cursor.getString(4);
        this.mHotelName = cursor.getString(5);
        this.mCheckinTime = cursor.isNull(6) ? null : new Date(cursor.getLong(6));
        if (!cursor.isNull(7)) {
            num = Integer.valueOf(cursor.getInt(7));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(8);
    }

    public RawHotelInfo(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mHotelTelNo = in.readByte() == (byte) 0 ? null : in.readString();
        this.mHotelAddr = in.readByte() == (byte) 0 ? null : in.readString();
        this.mHotelName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mCheckinTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private RawHotelInfo(Integer mId, Date mTimeStamp, String mHotelTelNo, String mHotelAddr, String mHotelName, Date mCheckinTime, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mHotelTelNo = mHotelTelNo;
        this.mHotelAddr = mHotelAddr;
        this.mHotelName = mHotelName;
        this.mCheckinTime = mCheckinTime;
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

    public String getMHotelTelNo() {
        return this.mHotelTelNo;
    }

    public void setMHotelTelNo(String mHotelTelNo) {
        this.mHotelTelNo = mHotelTelNo;
        setValue();
    }

    public String getMHotelAddr() {
        return this.mHotelAddr;
    }

    public void setMHotelAddr(String mHotelAddr) {
        this.mHotelAddr = mHotelAddr;
        setValue();
    }

    public String getMHotelName() {
        return this.mHotelName;
    }

    public void setMHotelName(String mHotelName) {
        this.mHotelName = mHotelName;
        setValue();
    }

    public Date getMCheckinTime() {
        return this.mCheckinTime;
    }

    public void setMCheckinTime(Date mCheckinTime) {
        this.mCheckinTime = mCheckinTime;
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
        if (this.mHotelTelNo != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mHotelTelNo);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mHotelAddr != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mHotelAddr);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mHotelName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mHotelName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCheckinTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mCheckinTime.getTime());
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

    public AEntityHelper<RawHotelInfo> getHelper() {
        return RawHotelInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RawHotelInfo";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RawHotelInfo { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mHotelTelNo: ").append(this.mHotelTelNo);
        sb.append(", mHotelAddr: ").append(this.mHotelAddr);
        sb.append(", mHotelName: ").append(this.mHotelName);
        sb.append(", mCheckinTime: ").append(this.mCheckinTime);
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
