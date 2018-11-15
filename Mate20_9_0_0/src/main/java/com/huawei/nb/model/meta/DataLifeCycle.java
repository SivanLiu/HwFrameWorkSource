package com.huawei.nb.model.meta;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class DataLifeCycle extends AManagedObject {
    public static final Creator<DataLifeCycle> CREATOR = new Creator<DataLifeCycle>() {
        public DataLifeCycle createFromParcel(Parcel in) {
            return new DataLifeCycle(in);
        }

        public DataLifeCycle[] newArray(int size) {
            return new DataLifeCycle[size];
        }
    };
    private Integer mCount;
    private String mDBName;
    private Long mDBRekeyTime;
    private String mFieldName;
    private Integer mId;
    private Integer mMode;
    private String mTableName;
    private Integer mThreshold;
    private Integer mUnit;

    public DataLifeCycle(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mDBName = cursor.getString(2);
        this.mTableName = cursor.getString(3);
        this.mMode = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        this.mFieldName = cursor.getString(5);
        this.mCount = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        this.mDBRekeyTime = cursor.isNull(7) ? null : Long.valueOf(cursor.getLong(7));
        this.mThreshold = cursor.isNull(8) ? null : Integer.valueOf(cursor.getInt(8));
        if (!cursor.isNull(9)) {
            num = Integer.valueOf(cursor.getInt(9));
        }
        this.mUnit = num;
    }

    public DataLifeCycle(Parcel in) {
        Integer num = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mDBName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mTableName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mMode = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mFieldName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mCount = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mDBRekeyTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.mThreshold = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            num = Integer.valueOf(in.readInt());
        }
        this.mUnit = num;
    }

    private DataLifeCycle(Integer mId, String mDBName, String mTableName, Integer mMode, String mFieldName, Integer mCount, Long mDBRekeyTime, Integer mThreshold, Integer mUnit) {
        this.mId = mId;
        this.mDBName = mDBName;
        this.mTableName = mTableName;
        this.mMode = mMode;
        this.mFieldName = mFieldName;
        this.mCount = mCount;
        this.mDBRekeyTime = mDBRekeyTime;
        this.mThreshold = mThreshold;
        this.mUnit = mUnit;
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

    public String getMDBName() {
        return this.mDBName;
    }

    public void setMDBName(String mDBName) {
        this.mDBName = mDBName;
        setValue();
    }

    public String getMTableName() {
        return this.mTableName;
    }

    public void setMTableName(String mTableName) {
        this.mTableName = mTableName;
        setValue();
    }

    public Integer getMMode() {
        return this.mMode;
    }

    public void setMMode(Integer mMode) {
        this.mMode = mMode;
        setValue();
    }

    public String getMFieldName() {
        return this.mFieldName;
    }

    public void setMFieldName(String mFieldName) {
        this.mFieldName = mFieldName;
        setValue();
    }

    public Integer getMCount() {
        return this.mCount;
    }

    public void setMCount(Integer mCount) {
        this.mCount = mCount;
        setValue();
    }

    public Long getMDBRekeyTime() {
        return this.mDBRekeyTime;
    }

    public void setMDBRekeyTime(Long mDBRekeyTime) {
        this.mDBRekeyTime = mDBRekeyTime;
        setValue();
    }

    public Integer getMThreshold() {
        return this.mThreshold;
    }

    public void setMThreshold(Integer mThreshold) {
        this.mThreshold = mThreshold;
        setValue();
    }

    public Integer getMUnit() {
        return this.mUnit;
    }

    public void setMUnit(Integer mUnit) {
        this.mUnit = mUnit;
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
        if (this.mDBName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mDBName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mTableName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mTableName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mMode != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mMode.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mFieldName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mFieldName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCount != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mCount.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mDBRekeyTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mDBRekeyTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mThreshold != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mThreshold.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mUnit != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mUnit.intValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<DataLifeCycle> getHelper() {
        return DataLifeCycleHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.meta.DataLifeCycle";
    }

    public String getDatabaseName() {
        return "dsMeta";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DataLifeCycle { mId: ").append(this.mId);
        sb.append(", mDBName: ").append(this.mDBName);
        sb.append(", mTableName: ").append(this.mTableName);
        sb.append(", mMode: ").append(this.mMode);
        sb.append(", mFieldName: ").append(this.mFieldName);
        sb.append(", mCount: ").append(this.mCount);
        sb.append(", mDBRekeyTime: ").append(this.mDBRekeyTime);
        sb.append(", mThreshold: ").append(this.mThreshold);
        sb.append(", mUnit: ").append(this.mUnit);
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
        return "0.0.13";
    }

    public int getDatabaseVersionCode() {
        return 13;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
