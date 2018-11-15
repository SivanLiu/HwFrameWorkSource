package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class DicFieldCollectPolicy extends AManagedObject {
    public static final Creator<DicFieldCollectPolicy> CREATOR = new Creator<DicFieldCollectPolicy>() {
        public DicFieldCollectPolicy createFromParcel(Parcel in) {
            return new DicFieldCollectPolicy(in);
        }

        public DicFieldCollectPolicy[] newArray(int size) {
            return new DicFieldCollectPolicy[size];
        }
    };
    private Integer mCollectMethod;
    private String mFieldName;
    private Integer mId;
    private Integer mReservedInt;
    private String mReservedText;
    private String mTableName;

    public DicFieldCollectPolicy(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTableName = cursor.getString(2);
        this.mFieldName = cursor.getString(3);
        this.mCollectMethod = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        if (!cursor.isNull(5)) {
            num = Integer.valueOf(cursor.getInt(5));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(6);
    }

    public DicFieldCollectPolicy(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTableName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mFieldName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mCollectMethod = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private DicFieldCollectPolicy(Integer mId, String mTableName, String mFieldName, Integer mCollectMethod, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTableName = mTableName;
        this.mFieldName = mFieldName;
        this.mCollectMethod = mCollectMethod;
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

    public String getMTableName() {
        return this.mTableName;
    }

    public void setMTableName(String mTableName) {
        this.mTableName = mTableName;
        setValue();
    }

    public String getMFieldName() {
        return this.mFieldName;
    }

    public void setMFieldName(String mFieldName) {
        this.mFieldName = mFieldName;
        setValue();
    }

    public Integer getMCollectMethod() {
        return this.mCollectMethod;
    }

    public void setMCollectMethod(Integer mCollectMethod) {
        this.mCollectMethod = mCollectMethod;
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
        if (this.mTableName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mTableName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mFieldName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mFieldName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCollectMethod != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mCollectMethod.intValue());
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

    public AEntityHelper<DicFieldCollectPolicy> getHelper() {
        return DicFieldCollectPolicyHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.DicFieldCollectPolicy";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DicFieldCollectPolicy { mId: ").append(this.mId);
        sb.append(", mTableName: ").append(this.mTableName);
        sb.append(", mFieldName: ").append(this.mFieldName);
        sb.append(", mCollectMethod: ").append(this.mCollectMethod);
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
