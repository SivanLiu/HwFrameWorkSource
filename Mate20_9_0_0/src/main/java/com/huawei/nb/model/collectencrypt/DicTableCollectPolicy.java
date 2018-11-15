package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class DicTableCollectPolicy extends AManagedObject {
    public static final Creator<DicTableCollectPolicy> CREATOR = new Creator<DicTableCollectPolicy>() {
        public DicTableCollectPolicy createFromParcel(Parcel in) {
            return new DicTableCollectPolicy(in);
        }

        public DicTableCollectPolicy[] newArray(int size) {
            return new DicTableCollectPolicy[size];
        }
    };
    private Integer mColdDownTime;
    private Integer mId;
    private Integer mMaxRecordOneday;
    private Integer mReservedInt;
    private String mReservedText;
    private String mTblName;
    private Integer mTblType;
    private String mTriggerPolicy;

    public DicTableCollectPolicy(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTblName = cursor.getString(2);
        this.mTblType = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.mTriggerPolicy = cursor.getString(4);
        this.mMaxRecordOneday = cursor.isNull(5) ? null : Integer.valueOf(cursor.getInt(5));
        this.mColdDownTime = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        if (!cursor.isNull(7)) {
            num = Integer.valueOf(cursor.getInt(7));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(8);
    }

    public DicTableCollectPolicy(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTblName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mTblType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mTriggerPolicy = in.readByte() == (byte) 0 ? null : in.readString();
        this.mMaxRecordOneday = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mColdDownTime = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private DicTableCollectPolicy(Integer mId, String mTblName, Integer mTblType, String mTriggerPolicy, Integer mMaxRecordOneday, Integer mColdDownTime, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTblName = mTblName;
        this.mTblType = mTblType;
        this.mTriggerPolicy = mTriggerPolicy;
        this.mMaxRecordOneday = mMaxRecordOneday;
        this.mColdDownTime = mColdDownTime;
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

    public String getMTblName() {
        return this.mTblName;
    }

    public void setMTblName(String mTblName) {
        this.mTblName = mTblName;
        setValue();
    }

    public Integer getMTblType() {
        return this.mTblType;
    }

    public void setMTblType(Integer mTblType) {
        this.mTblType = mTblType;
        setValue();
    }

    public String getMTriggerPolicy() {
        return this.mTriggerPolicy;
    }

    public void setMTriggerPolicy(String mTriggerPolicy) {
        this.mTriggerPolicy = mTriggerPolicy;
        setValue();
    }

    public Integer getMMaxRecordOneday() {
        return this.mMaxRecordOneday;
    }

    public void setMMaxRecordOneday(Integer mMaxRecordOneday) {
        this.mMaxRecordOneday = mMaxRecordOneday;
        setValue();
    }

    public Integer getMColdDownTime() {
        return this.mColdDownTime;
    }

    public void setMColdDownTime(Integer mColdDownTime) {
        this.mColdDownTime = mColdDownTime;
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
        if (this.mTblName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mTblName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mTblType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mTblType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mTriggerPolicy != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mTriggerPolicy);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mMaxRecordOneday != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mMaxRecordOneday.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mColdDownTime != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mColdDownTime.intValue());
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

    public AEntityHelper<DicTableCollectPolicy> getHelper() {
        return DicTableCollectPolicyHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.DicTableCollectPolicy";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DicTableCollectPolicy { mId: ").append(this.mId);
        sb.append(", mTblName: ").append(this.mTblName);
        sb.append(", mTblType: ").append(this.mTblType);
        sb.append(", mTriggerPolicy: ").append(this.mTriggerPolicy);
        sb.append(", mMaxRecordOneday: ").append(this.mMaxRecordOneday);
        sb.append(", mColdDownTime: ").append(this.mColdDownTime);
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
