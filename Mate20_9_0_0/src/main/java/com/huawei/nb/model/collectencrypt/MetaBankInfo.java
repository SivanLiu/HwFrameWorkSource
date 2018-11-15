package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class MetaBankInfo extends AManagedObject {
    public static final Creator<MetaBankInfo> CREATOR = new Creator<MetaBankInfo>() {
        public MetaBankInfo createFromParcel(Parcel in) {
            return new MetaBankInfo(in);
        }

        public MetaBankInfo[] newArray(int size) {
            return new MetaBankInfo[size];
        }
    };
    private Integer mId;
    private Integer mInRange;
    private Integer mOutRange;
    private Integer mReservedInt;
    private String mReservedText;
    private Date mTimeStamp;

    public MetaBankInfo(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mInRange = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.mOutRange = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        if (!cursor.isNull(5)) {
            num = Integer.valueOf(cursor.getInt(5));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(6);
    }

    public MetaBankInfo(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mInRange = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mOutRange = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private MetaBankInfo(Integer mId, Date mTimeStamp, Integer mInRange, Integer mOutRange, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mInRange = mInRange;
        this.mOutRange = mOutRange;
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

    public Integer getMInRange() {
        return this.mInRange;
    }

    public void setMInRange(Integer mInRange) {
        this.mInRange = mInRange;
        setValue();
    }

    public Integer getMOutRange() {
        return this.mOutRange;
    }

    public void setMOutRange(Integer mOutRange) {
        this.mOutRange = mOutRange;
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
        if (this.mInRange != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mInRange.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mOutRange != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mOutRange.intValue());
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

    public AEntityHelper<MetaBankInfo> getHelper() {
        return MetaBankInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.MetaBankInfo";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("MetaBankInfo { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mInRange: ").append(this.mInRange);
        sb.append(", mOutRange: ").append(this.mOutRange);
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
