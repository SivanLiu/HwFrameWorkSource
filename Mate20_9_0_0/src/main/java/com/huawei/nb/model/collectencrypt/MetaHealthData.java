package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class MetaHealthData extends AManagedObject {
    public static final Creator<MetaHealthData> CREATOR = new Creator<MetaHealthData>() {
        public MetaHealthData createFromParcel(Parcel in) {
            return new MetaHealthData(in);
        }

        public MetaHealthData[] newArray(int size) {
            return new MetaHealthData[size];
        }
    };
    private Double mBloodPressure_high;
    private Double mBloodPressure_low;
    private Double mBloodSugar;
    private Double mHeartRat;
    private Integer mId;
    private Integer mReservedInt;
    private String mReservedText;
    private Date mTimeStamp;

    public MetaHealthData(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mHeartRat = cursor.isNull(3) ? null : Double.valueOf(cursor.getDouble(3));
        this.mBloodPressure_low = cursor.isNull(4) ? null : Double.valueOf(cursor.getDouble(4));
        this.mBloodPressure_high = cursor.isNull(5) ? null : Double.valueOf(cursor.getDouble(5));
        this.mBloodSugar = cursor.isNull(6) ? null : Double.valueOf(cursor.getDouble(6));
        if (!cursor.isNull(7)) {
            num = Integer.valueOf(cursor.getInt(7));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(8);
    }

    public MetaHealthData(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mHeartRat = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mBloodPressure_low = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mBloodPressure_high = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mBloodSugar = in.readByte() == (byte) 0 ? null : Double.valueOf(in.readDouble());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private MetaHealthData(Integer mId, Date mTimeStamp, Double mHeartRat, Double mBloodPressure_low, Double mBloodPressure_high, Double mBloodSugar, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mHeartRat = mHeartRat;
        this.mBloodPressure_low = mBloodPressure_low;
        this.mBloodPressure_high = mBloodPressure_high;
        this.mBloodSugar = mBloodSugar;
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

    public Double getMHeartRat() {
        return this.mHeartRat;
    }

    public void setMHeartRat(Double mHeartRat) {
        this.mHeartRat = mHeartRat;
        setValue();
    }

    public Double getMBloodPressure_low() {
        return this.mBloodPressure_low;
    }

    public void setMBloodPressure_low(Double mBloodPressure_low) {
        this.mBloodPressure_low = mBloodPressure_low;
        setValue();
    }

    public Double getMBloodPressure_high() {
        return this.mBloodPressure_high;
    }

    public void setMBloodPressure_high(Double mBloodPressure_high) {
        this.mBloodPressure_high = mBloodPressure_high;
        setValue();
    }

    public Double getMBloodSugar() {
        return this.mBloodSugar;
    }

    public void setMBloodSugar(Double mBloodSugar) {
        this.mBloodSugar = mBloodSugar;
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
        if (this.mHeartRat != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mHeartRat.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBloodPressure_low != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mBloodPressure_low.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBloodPressure_high != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mBloodPressure_high.doubleValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBloodSugar != null) {
            out.writeByte((byte) 1);
            out.writeDouble(this.mBloodSugar.doubleValue());
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

    public AEntityHelper<MetaHealthData> getHelper() {
        return MetaHealthDataHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.MetaHealthData";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("MetaHealthData { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mHeartRat: ").append(this.mHeartRat);
        sb.append(", mBloodPressure_low: ").append(this.mBloodPressure_low);
        sb.append(", mBloodPressure_high: ").append(this.mBloodPressure_high);
        sb.append(", mBloodSugar: ").append(this.mBloodSugar);
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
