package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RawSportHealth extends AManagedObject {
    public static final Creator<RawSportHealth> CREATOR = new Creator<RawSportHealth>() {
        public RawSportHealth createFromParcel(Parcel in) {
            return new RawSportHealth(in);
        }

        public RawSportHealth[] newArray(int size) {
            return new RawSportHealth[size];
        }
    };
    private Double mBloodPressureHigh;
    private Double mBloodPressureLow;
    private Double mBloodSugar;
    private Double mHeartRat;
    private Double mHeight;
    private Integer mId;
    private Integer mReservedInt;
    private String mReservedText;
    private Double mSleep;
    private String mSportAR;
    private Double mSportDistance;
    private Double mSportHeat;
    private Double mSportHeight;
    private Double mSportPaces;
    private Date mTimeStamp;
    private Double mWeight;

    public RawSportHealth(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mHeight = cursor.isNull(3) ? null : Double.valueOf(cursor.getDouble(3));
        this.mWeight = cursor.isNull(4) ? null : Double.valueOf(cursor.getDouble(4));
        this.mHeartRat = cursor.isNull(5) ? null : Double.valueOf(cursor.getDouble(5));
        this.mBloodPressureLow = cursor.isNull(6) ? null : Double.valueOf(cursor.getDouble(6));
        this.mBloodPressureHigh = cursor.isNull(7) ? null : Double.valueOf(cursor.getDouble(7));
        this.mBloodSugar = cursor.isNull(8) ? null : Double.valueOf(cursor.getDouble(8));
        this.mSportDistance = cursor.isNull(9) ? null : Double.valueOf(cursor.getDouble(9));
        this.mSportHeight = cursor.isNull(10) ? null : Double.valueOf(cursor.getDouble(10));
        this.mSportHeat = cursor.isNull(11) ? null : Double.valueOf(cursor.getDouble(11));
        this.mSportPaces = cursor.isNull(12) ? null : Double.valueOf(cursor.getDouble(12));
        this.mSleep = cursor.isNull(13) ? null : Double.valueOf(cursor.getDouble(13));
        this.mSportAR = cursor.getString(14);
        if (!cursor.isNull(15)) {
            num = Integer.valueOf(cursor.getInt(15));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(16);
    }

    public RawSportHealth(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mHeight = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mWeight = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mHeartRat = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mBloodPressureLow = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mBloodPressureHigh = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mBloodSugar = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mSportDistance = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mSportHeight = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mSportHeat = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mSportPaces = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mSleep = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mSportAR = in.readByte() == (byte) 0 ? null : in.readString();
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private RawSportHealth(Integer mId, Date mTimeStamp, Double mHeight, Double mWeight, Double mHeartRat, Double mBloodPressureLow, Double mBloodPressureHigh, Double mBloodSugar, Double mSportDistance, Double mSportHeight, Double mSportHeat, Double mSportPaces, Double mSleep, String mSportAR, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mHeight = mHeight;
        this.mWeight = mWeight;
        this.mHeartRat = mHeartRat;
        this.mBloodPressureLow = mBloodPressureLow;
        this.mBloodPressureHigh = mBloodPressureHigh;
        this.mBloodSugar = mBloodSugar;
        this.mSportDistance = mSportDistance;
        this.mSportHeight = mSportHeight;
        this.mSportHeat = mSportHeat;
        this.mSportPaces = mSportPaces;
        this.mSleep = mSleep;
        this.mSportAR = mSportAR;
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

    public Double getMHeartRat() {
        return this.mHeartRat;
    }

    public void setMHeartRat(Double mHeartRat) {
        this.mHeartRat = mHeartRat;
        setValue();
    }

    public Double getMBloodPressureLow() {
        return this.mBloodPressureLow;
    }

    public void setMBloodPressureLow(Double mBloodPressureLow) {
        this.mBloodPressureLow = mBloodPressureLow;
        setValue();
    }

    public Double getMBloodPressureHigh() {
        return this.mBloodPressureHigh;
    }

    public void setMBloodPressureHigh(Double mBloodPressureHigh) {
        this.mBloodPressureHigh = mBloodPressureHigh;
        setValue();
    }

    public Double getMBloodSugar() {
        return this.mBloodSugar;
    }

    public void setMBloodSugar(Double mBloodSugar) {
        this.mBloodSugar = mBloodSugar;
        setValue();
    }

    public Double getMSportDistance() {
        return this.mSportDistance;
    }

    public void setMSportDistance(Double mSportDistance) {
        this.mSportDistance = mSportDistance;
        setValue();
    }

    public Double getMSportHeight() {
        return this.mSportHeight;
    }

    public void setMSportHeight(Double mSportHeight) {
        this.mSportHeight = mSportHeight;
        setValue();
    }

    public Double getMSportHeat() {
        return this.mSportHeat;
    }

    public void setMSportHeat(Double mSportHeat) {
        this.mSportHeat = mSportHeat;
        setValue();
    }

    public Double getMSportPaces() {
        return this.mSportPaces;
    }

    public void setMSportPaces(Double mSportPaces) {
        this.mSportPaces = mSportPaces;
        setValue();
    }

    public Double getMSleep() {
        return this.mSleep;
    }

    public void setMSleep(Double mSleep) {
        this.mSleep = mSleep;
        setValue();
    }

    public String getMSportAR() {
        return this.mSportAR;
    }

    public void setMSportAR(String mSportAR) {
        this.mSportAR = mSportAR;
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
        if (this.mHeartRat != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mHeartRat.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBloodPressureLow != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mBloodPressureLow.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBloodPressureHigh != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mBloodPressureHigh.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBloodSugar != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mBloodSugar.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSportDistance != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mSportDistance.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSportHeight != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mSportHeight.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSportHeat != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mSportHeat.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSportPaces != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mSportPaces.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSleep != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mSleep.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mSportAR != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mSportAR);
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

    public AEntityHelper<RawSportHealth> getHelper() {
        return RawSportHealthHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RawSportHealth";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RawSportHealth { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mHeight: ").append(this.mHeight);
        sb.append(", mWeight: ").append(this.mWeight);
        sb.append(", mHeartRat: ").append(this.mHeartRat);
        sb.append(", mBloodPressureLow: ").append(this.mBloodPressureLow);
        sb.append(", mBloodPressureHigh: ").append(this.mBloodPressureHigh);
        sb.append(", mBloodSugar: ").append(this.mBloodSugar);
        sb.append(", mSportDistance: ").append(this.mSportDistance);
        sb.append(", mSportHeight: ").append(this.mSportHeight);
        sb.append(", mSportHeat: ").append(this.mSportHeat);
        sb.append(", mSportPaces: ").append(this.mSportPaces);
        sb.append(", mSleep: ").append(this.mSleep);
        sb.append(", mSportAR: ").append(this.mSportAR);
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
