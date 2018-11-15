package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RefSMARStatus extends AManagedObject {
    public static final Creator<RefSMARStatus> CREATOR = new Creator<RefSMARStatus>() {
        public RefSMARStatus createFromParcel(Parcel in) {
            return new RefSMARStatus(in);
        }

        public RefSMARStatus[] newArray(int size) {
            return new RefSMARStatus[size];
        }
    };
    private Date mEndTime;
    private Integer mMotionType;
    private String mReserved0;
    private String mReserved1;
    private Integer mSmarId;
    private Date mStartTime;

    public RefSMARStatus(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mSmarId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mStartTime = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mEndTime = cursor.isNull(3) ? null : new Date(cursor.getLong(3));
        if (!cursor.isNull(4)) {
            num = Integer.valueOf(cursor.getInt(4));
        }
        this.mMotionType = num;
        this.mReserved0 = cursor.getString(5);
        this.mReserved1 = cursor.getString(6);
    }

    public RefSMARStatus(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mSmarId = null;
            in.readInt();
        } else {
            this.mSmarId = Integer.valueOf(in.readInt());
        }
        this.mStartTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mEndTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mMotionType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReserved0 = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReserved1 = str;
    }

    private RefSMARStatus(Integer mSmarId, Date mStartTime, Date mEndTime, Integer mMotionType, String mReserved0, String mReserved1) {
        this.mSmarId = mSmarId;
        this.mStartTime = mStartTime;
        this.mEndTime = mEndTime;
        this.mMotionType = mMotionType;
        this.mReserved0 = mReserved0;
        this.mReserved1 = mReserved1;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getMSmarId() {
        return this.mSmarId;
    }

    public void setMSmarId(Integer mSmarId) {
        this.mSmarId = mSmarId;
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

    public Integer getMMotionType() {
        return this.mMotionType;
    }

    public void setMMotionType(Integer mMotionType) {
        this.mMotionType = mMotionType;
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
        if (this.mSmarId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mSmarId.intValue());
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
        if (this.mMotionType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mMotionType.intValue());
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

    public AEntityHelper<RefSMARStatus> getHelper() {
        return RefSMARStatusHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RefSMARStatus";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RefSMARStatus { mSmarId: ").append(this.mSmarId);
        sb.append(", mStartTime: ").append(this.mStartTime);
        sb.append(", mEndTime: ").append(this.mEndTime);
        sb.append(", mMotionType: ").append(this.mMotionType);
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
