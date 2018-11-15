package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RawMailInfo extends AManagedObject {
    public static final Creator<RawMailInfo> CREATOR = new Creator<RawMailInfo>() {
        public RawMailInfo createFromParcel(Parcel in) {
            return new RawMailInfo(in);
        }

        public RawMailInfo[] newArray(int size) {
            return new RawMailInfo[size];
        }
    };
    private Integer mId;
    private String mMailAddress;
    private String mMailClientName;
    private String mMailContent;
    private String mMailFrom;
    private String mMailSubject;
    private Date mMailTime;
    private String mMailTo;
    private Integer mReservedInt;
    private String mReservedText;
    private Date mTimeStamp;

    public RawMailInfo(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mMailClientName = cursor.getString(3);
        this.mMailAddress = cursor.getString(4);
        this.mMailSubject = cursor.getString(5);
        this.mMailContent = cursor.getString(6);
        this.mMailTime = cursor.isNull(7) ? null : new Date(cursor.getLong(7));
        this.mMailFrom = cursor.getString(8);
        this.mMailTo = cursor.getString(9);
        if (!cursor.isNull(10)) {
            num = Integer.valueOf(cursor.getInt(10));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(11);
    }

    public RawMailInfo(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mMailClientName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mMailAddress = in.readByte() == (byte) 0 ? null : in.readString();
        this.mMailSubject = in.readByte() == (byte) 0 ? null : in.readString();
        this.mMailContent = in.readByte() == (byte) 0 ? null : in.readString();
        this.mMailTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mMailFrom = in.readByte() == (byte) 0 ? null : in.readString();
        this.mMailTo = in.readByte() == (byte) 0 ? null : in.readString();
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private RawMailInfo(Integer mId, Date mTimeStamp, String mMailClientName, String mMailAddress, String mMailSubject, String mMailContent, Date mMailTime, String mMailFrom, String mMailTo, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mMailClientName = mMailClientName;
        this.mMailAddress = mMailAddress;
        this.mMailSubject = mMailSubject;
        this.mMailContent = mMailContent;
        this.mMailTime = mMailTime;
        this.mMailFrom = mMailFrom;
        this.mMailTo = mMailTo;
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

    public String getMMailClientName() {
        return this.mMailClientName;
    }

    public void setMMailClientName(String mMailClientName) {
        this.mMailClientName = mMailClientName;
        setValue();
    }

    public String getMMailAddress() {
        return this.mMailAddress;
    }

    public void setMMailAddress(String mMailAddress) {
        this.mMailAddress = mMailAddress;
        setValue();
    }

    public String getMMailSubject() {
        return this.mMailSubject;
    }

    public void setMMailSubject(String mMailSubject) {
        this.mMailSubject = mMailSubject;
        setValue();
    }

    public String getMMailContent() {
        return this.mMailContent;
    }

    public void setMMailContent(String mMailContent) {
        this.mMailContent = mMailContent;
        setValue();
    }

    public Date getMMailTime() {
        return this.mMailTime;
    }

    public void setMMailTime(Date mMailTime) {
        this.mMailTime = mMailTime;
        setValue();
    }

    public String getMMailFrom() {
        return this.mMailFrom;
    }

    public void setMMailFrom(String mMailFrom) {
        this.mMailFrom = mMailFrom;
        setValue();
    }

    public String getMMailTo() {
        return this.mMailTo;
    }

    public void setMMailTo(String mMailTo) {
        this.mMailTo = mMailTo;
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
        if (this.mMailClientName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mMailClientName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mMailAddress != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mMailAddress);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mMailSubject != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mMailSubject);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mMailContent != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mMailContent);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mMailTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mMailTime.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mMailFrom != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mMailFrom);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mMailTo != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mMailTo);
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

    public AEntityHelper<RawMailInfo> getHelper() {
        return RawMailInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RawMailInfo";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RawMailInfo { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mMailClientName: ").append(this.mMailClientName);
        sb.append(", mMailAddress: ").append(this.mMailAddress);
        sb.append(", mMailSubject: ").append(this.mMailSubject);
        sb.append(", mMailContent: ").append(this.mMailContent);
        sb.append(", mMailTime: ").append(this.mMailTime);
        sb.append(", mMailFrom: ").append(this.mMailFrom);
        sb.append(", mMailTo: ").append(this.mMailTo);
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
