package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RefTimeLineLocationRecord extends AManagedObject {
    public static final Creator<RefTimeLineLocationRecord> CREATOR = new Creator<RefTimeLineLocationRecord>() {
        public RefTimeLineLocationRecord createFromParcel(Parcel in) {
            return new RefTimeLineLocationRecord(in);
        }

        public RefTimeLineLocationRecord[] newArray(int size) {
            return new RefTimeLineLocationRecord[size];
        }
    };
    private Integer mClusterLocId;
    private Date mEndTime;
    private Integer mRecordId;
    private String mReserved0;
    private String mReserved1;
    private Date mStartTime;

    public RefTimeLineLocationRecord(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mRecordId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mStartTime = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mEndTime = cursor.isNull(3) ? null : new Date(cursor.getLong(3));
        if (!cursor.isNull(4)) {
            num = Integer.valueOf(cursor.getInt(4));
        }
        this.mClusterLocId = num;
        this.mReserved0 = cursor.getString(5);
        this.mReserved1 = cursor.getString(6);
    }

    public RefTimeLineLocationRecord(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mRecordId = null;
            in.readInt();
        } else {
            this.mRecordId = Integer.valueOf(in.readInt());
        }
        this.mStartTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mEndTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mClusterLocId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReserved0 = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReserved1 = str;
    }

    private RefTimeLineLocationRecord(Integer mRecordId, Date mStartTime, Date mEndTime, Integer mClusterLocId, String mReserved0, String mReserved1) {
        this.mRecordId = mRecordId;
        this.mStartTime = mStartTime;
        this.mEndTime = mEndTime;
        this.mClusterLocId = mClusterLocId;
        this.mReserved0 = mReserved0;
        this.mReserved1 = mReserved1;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getMRecordId() {
        return this.mRecordId;
    }

    public void setMRecordId(Integer mRecordId) {
        this.mRecordId = mRecordId;
        setValue();
    }

    public Date getMStartTime() {
        return this.mStartTime;
    }

    public void setMStartTime(Date mStartTime) {
        this.mStartTime = mStartTime;
        setValue();
    }

    public Date getMEndTime() {
        return this.mEndTime;
    }

    public void setMEndTime(Date mEndTime) {
        this.mEndTime = mEndTime;
        setValue();
    }

    public Integer getMClusterLocId() {
        return this.mClusterLocId;
    }

    public void setMClusterLocId(Integer mClusterLocId) {
        this.mClusterLocId = mClusterLocId;
        setValue();
    }

    public String getMReserved0() {
        return this.mReserved0;
    }

    public void setMReserved0(String mReserved0) {
        this.mReserved0 = mReserved0;
        setValue();
    }

    public String getMReserved1() {
        return this.mReserved1;
    }

    public void setMReserved1(String mReserved1) {
        this.mReserved1 = mReserved1;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.mRecordId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mRecordId.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.mStartTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mStartTime.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mEndTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mEndTime.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mClusterLocId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mClusterLocId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReserved0 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mReserved0);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mReserved1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mReserved1);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<RefTimeLineLocationRecord> getHelper() {
        return RefTimeLineLocationRecordHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RefTimeLineLocationRecord";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RefTimeLineLocationRecord { mRecordId: ").append(this.mRecordId);
        sb.append(", mStartTime: ").append(this.mStartTime);
        sb.append(", mEndTime: ").append(this.mEndTime);
        sb.append(", mClusterLocId: ").append(this.mClusterLocId);
        sb.append(", mReserved0: ").append(this.mReserved0);
        sb.append(", mReserved1: ").append(this.mReserved1);
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
