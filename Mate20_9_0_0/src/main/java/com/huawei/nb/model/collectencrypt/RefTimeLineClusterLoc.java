package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RefTimeLineClusterLoc extends AManagedObject {
    public static final Creator<RefTimeLineClusterLoc> CREATOR = new Creator<RefTimeLineClusterLoc>() {
        public RefTimeLineClusterLoc createFromParcel(Parcel in) {
            return new RefTimeLineClusterLoc(in);
        }

        public RefTimeLineClusterLoc[] newArray(int size) {
            return new RefTimeLineClusterLoc[size];
        }
    };
    private Integer mClusterID;
    private Integer mDuration;
    private Date mLastVisit;
    private Double mLatitude;
    private Double mLongitude;
    private Integer mRange;
    private String mReserved0;
    private String mReserved1;

    public RefTimeLineClusterLoc(Cursor cursor) {
        Date date = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mClusterID = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mLongitude = cursor.isNull(2) ? null : Double.valueOf(cursor.getDouble(2));
        this.mLatitude = cursor.isNull(3) ? null : Double.valueOf(cursor.getDouble(3));
        this.mRange = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        this.mDuration = cursor.isNull(5) ? null : Integer.valueOf(cursor.getInt(5));
        if (!cursor.isNull(6)) {
            date = new Date(cursor.getLong(6));
        }
        this.mLastVisit = date;
        this.mReserved0 = cursor.getString(7);
        this.mReserved1 = cursor.getString(8);
    }

    public RefTimeLineClusterLoc(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mClusterID = null;
            in.readInt();
        } else {
            this.mClusterID = Integer.valueOf(in.readInt());
        }
        this.mLongitude = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mLatitude = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mRange = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mDuration = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mLastVisit = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mReserved0 = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReserved1 = str;
    }

    private RefTimeLineClusterLoc(Integer mClusterID, Double mLongitude, Double mLatitude, Integer mRange, Integer mDuration, Date mLastVisit, String mReserved0, String mReserved1) {
        this.mClusterID = mClusterID;
        this.mLongitude = mLongitude;
        this.mLatitude = mLatitude;
        this.mRange = mRange;
        this.mDuration = mDuration;
        this.mLastVisit = mLastVisit;
        this.mReserved0 = mReserved0;
        this.mReserved1 = mReserved1;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getMClusterID() {
        return this.mClusterID;
    }

    public void setMClusterID(Integer mClusterID) {
        this.mClusterID = mClusterID;
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

    public Integer getMRange() {
        return this.mRange;
    }

    public void setMRange(Integer mRange) {
        this.mRange = mRange;
        setValue();
    }

    public Integer getMDuration() {
        return this.mDuration;
    }

    public void setMDuration(Integer mDuration) {
        this.mDuration = mDuration;
        setValue();
    }

    public Date getMLastVisit() {
        return this.mLastVisit;
    }

    public void setMLastVisit(Date mLastVisit) {
        this.mLastVisit = mLastVisit;
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
        if (this.mClusterID != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mClusterID.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
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
        if (this.mRange != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mRange.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mDuration != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mDuration.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mLastVisit != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mLastVisit.getTime());
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

    public AEntityHelper<RefTimeLineClusterLoc> getHelper() {
        return RefTimeLineClusterLocHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RefTimeLineClusterLoc";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RefTimeLineClusterLoc { mClusterID: ").append(this.mClusterID);
        sb.append(", mLongitude: ").append(this.mLongitude);
        sb.append(", mLatitude: ").append(this.mLatitude);
        sb.append(", mRange: ").append(this.mRange);
        sb.append(", mDuration: ").append(this.mDuration);
        sb.append(", mLastVisit: ").append(this.mLastVisit);
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
