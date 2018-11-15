package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class MetaExerciseData extends AManagedObject {
    public static final Creator<MetaExerciseData> CREATOR = new Creator<MetaExerciseData>() {
        public MetaExerciseData createFromParcel(Parcel in) {
            return new MetaExerciseData(in);
        }

        public MetaExerciseData[] newArray(int size) {
            return new MetaExerciseData[size];
        }
    };
    private Integer mClimb;
    private Integer mCycling;
    private Integer mDecline;
    private Double mHeight;
    private Integer mId;
    private Integer mReservedInt;
    private String mReservedText;
    private Integer mRun;
    private Double mSleep;
    private String mSportAR;
    private Double mSportDistance;
    private Double mSportHeat;
    private Double mSportPaces;
    private Date mTimeStamp;
    private Integer mWalk;
    private Double mWeight;

    public MetaExerciseData(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mSportHeat = cursor.isNull(3) ? null : Double.valueOf(cursor.getDouble(3));
        this.mClimb = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        this.mDecline = cursor.isNull(5) ? null : Integer.valueOf(cursor.getInt(5));
        this.mSportDistance = cursor.isNull(6) ? null : Double.valueOf(cursor.getDouble(6));
        this.mSleep = cursor.isNull(7) ? null : Double.valueOf(cursor.getDouble(7));
        this.mSportPaces = cursor.isNull(8) ? null : Double.valueOf(cursor.getDouble(8));
        this.mHeight = cursor.isNull(9) ? null : Double.valueOf(cursor.getDouble(9));
        this.mWeight = cursor.isNull(10) ? null : Double.valueOf(cursor.getDouble(10));
        this.mSportAR = cursor.getString(11);
        this.mWalk = cursor.isNull(12) ? null : Integer.valueOf(cursor.getInt(12));
        this.mRun = cursor.isNull(13) ? null : Integer.valueOf(cursor.getInt(13));
        this.mCycling = cursor.isNull(14) ? null : Integer.valueOf(cursor.getInt(14));
        if (!cursor.isNull(15)) {
            num = Integer.valueOf(cursor.getInt(15));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(16);
    }

    public MetaExerciseData(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mSportHeat = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mClimb = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mDecline = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mSportDistance = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mSleep = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mSportPaces = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mHeight = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mWeight = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mSportAR = in.readByte() == (byte) 0 ? null : in.readString();
        this.mWalk = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mRun = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mCycling = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private MetaExerciseData(Integer mId, Date mTimeStamp, Double mSportHeat, Integer mClimb, Integer mDecline, Double mSportDistance, Double mSleep, Double mSportPaces, Double mHeight, Double mWeight, String mSportAR, Integer mWalk, Integer mRun, Integer mCycling, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mSportHeat = mSportHeat;
        this.mClimb = mClimb;
        this.mDecline = mDecline;
        this.mSportDistance = mSportDistance;
        this.mSleep = mSleep;
        this.mSportPaces = mSportPaces;
        this.mHeight = mHeight;
        this.mWeight = mWeight;
        this.mSportAR = mSportAR;
        this.mWalk = mWalk;
        this.mRun = mRun;
        this.mCycling = mCycling;
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

    public Double getMSportHeat() {
        return this.mSportHeat;
    }

    public void setMSportHeat(Double mSportHeat) {
        this.mSportHeat = mSportHeat;
        setValue();
    }

    public Integer getMClimb() {
        return this.mClimb;
    }

    public void setMClimb(Integer mClimb) {
        this.mClimb = mClimb;
        setValue();
    }

    public Integer getMDecline() {
        return this.mDecline;
    }

    public void setMDecline(Integer mDecline) {
        this.mDecline = mDecline;
        setValue();
    }

    public Double getMSportDistance() {
        return this.mSportDistance;
    }

    public void setMSportDistance(Double mSportDistance) {
        this.mSportDistance = mSportDistance;
        setValue();
    }

    public Double getMSleep() {
        return this.mSleep;
    }

    public void setMSleep(Double mSleep) {
        this.mSleep = mSleep;
        setValue();
    }

    public Double getMSportPaces() {
        return this.mSportPaces;
    }

    public void setMSportPaces(Double mSportPaces) {
        this.mSportPaces = mSportPaces;
        setValue();
    }

    public Double getMHeight() {
        return this.mHeight;
    }

    public void setMHeight(Double mHeight) {
        this.mHeight = mHeight;
        setValue();
    }

    public Double getMWeight() {
        return this.mWeight;
    }

    public void setMWeight(Double mWeight) {
        this.mWeight = mWeight;
        setValue();
    }

    public String getMSportAR() {
        return this.mSportAR;
    }

    public void setMSportAR(String mSportAR) {
        this.mSportAR = mSportAR;
        setValue();
    }

    public Integer getMWalk() {
        return this.mWalk;
    }

    public void setMWalk(Integer mWalk) {
        this.mWalk = mWalk;
        setValue();
    }

    public Integer getMRun() {
        return this.mRun;
    }

    public void setMRun(Integer mRun) {
        this.mRun = mRun;
        setValue();
    }

    public Integer getMCycling() {
        return this.mCycling;
    }

    public void setMCycling(Integer mCycling) {
        this.mCycling = mCycling;
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
        if (this.mSportHeat != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mSportHeat.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mClimb != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mClimb.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mDecline != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mDecline.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSportDistance != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mSportDistance.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSleep != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mSleep.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSportPaces != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mSportPaces.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mHeight != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mHeight.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mWeight != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mWeight.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSportAR != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mSportAR);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mWalk != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mWalk.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mRun != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mRun.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCycling != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mCycling.intValue());
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

    public AEntityHelper<MetaExerciseData> getHelper() {
        return MetaExerciseDataHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.MetaExerciseData";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("MetaExerciseData { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mSportHeat: ").append(this.mSportHeat);
        sb.append(", mClimb: ").append(this.mClimb);
        sb.append(", mDecline: ").append(this.mDecline);
        sb.append(", mSportDistance: ").append(this.mSportDistance);
        sb.append(", mSleep: ").append(this.mSleep);
        sb.append(", mSportPaces: ").append(this.mSportPaces);
        sb.append(", mHeight: ").append(this.mHeight);
        sb.append(", mWeight: ").append(this.mWeight);
        sb.append(", mSportAR: ").append(this.mSportAR);
        sb.append(", mWalk: ").append(this.mWalk);
        sb.append(", mRun: ").append(this.mRun);
        sb.append(", mCycling: ").append(this.mCycling);
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
