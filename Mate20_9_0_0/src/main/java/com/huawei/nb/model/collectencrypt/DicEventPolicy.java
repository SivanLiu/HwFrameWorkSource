package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class DicEventPolicy extends AManagedObject {
    public static final Creator<DicEventPolicy> CREATOR = new Creator<DicEventPolicy>() {
        public DicEventPolicy createFromParcel(Parcel in) {
            return new DicEventPolicy(in);
        }

        public DicEventPolicy[] newArray(int size) {
            return new DicEventPolicy[size];
        }
    };
    private Integer mColdDownTime;
    private String mEventDesc;
    private String mEventName;
    private Integer mEventType;
    private Integer mId;
    private Integer mMaxRecordOneday;
    private Integer mReservedInt;
    private String mReservedText;

    public DicEventPolicy(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mEventType = cursor.isNull(2) ? null : Integer.valueOf(cursor.getInt(2));
        this.mEventName = cursor.getString(3);
        this.mEventDesc = cursor.getString(4);
        this.mColdDownTime = cursor.isNull(5) ? null : Integer.valueOf(cursor.getInt(5));
        this.mMaxRecordOneday = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        if (!cursor.isNull(7)) {
            num = Integer.valueOf(cursor.getInt(7));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(8);
    }

    public DicEventPolicy(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mEventType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mEventName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mEventDesc = in.readByte() == (byte) 0 ? null : in.readString();
        this.mColdDownTime = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mMaxRecordOneday = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private DicEventPolicy(Integer mId, Integer mEventType, String mEventName, String mEventDesc, Integer mColdDownTime, Integer mMaxRecordOneday, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mEventType = mEventType;
        this.mEventName = mEventName;
        this.mEventDesc = mEventDesc;
        this.mColdDownTime = mColdDownTime;
        this.mMaxRecordOneday = mMaxRecordOneday;
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

    public Integer getMEventType() {
        return this.mEventType;
    }

    public void setMEventType(Integer mEventType) {
        this.mEventType = mEventType;
        setValue();
    }

    public String getMEventName() {
        return this.mEventName;
    }

    public void setMEventName(String mEventName) {
        this.mEventName = mEventName;
        setValue();
    }

    public String getMEventDesc() {
        return this.mEventDesc;
    }

    public void setMEventDesc(String mEventDesc) {
        this.mEventDesc = mEventDesc;
        setValue();
    }

    public Integer getMColdDownTime() {
        return this.mColdDownTime;
    }

    public void setMColdDownTime(Integer mColdDownTime) {
        this.mColdDownTime = mColdDownTime;
        setValue();
    }

    public Integer getMMaxRecordOneday() {
        return this.mMaxRecordOneday;
    }

    public void setMMaxRecordOneday(Integer mMaxRecordOneday) {
        this.mMaxRecordOneday = mMaxRecordOneday;
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
        if (this.mEventType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mEventType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mEventName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mEventName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mEventDesc != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mEventDesc);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mColdDownTime != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mColdDownTime.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mMaxRecordOneday != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mMaxRecordOneday.intValue());
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

    public AEntityHelper<DicEventPolicy> getHelper() {
        return DicEventPolicyHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.DicEventPolicy";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DicEventPolicy { mId: ").append(this.mId);
        sb.append(", mEventType: ").append(this.mEventType);
        sb.append(", mEventName: ").append(this.mEventName);
        sb.append(", mEventDesc: ").append(this.mEventDesc);
        sb.append(", mColdDownTime: ").append(this.mColdDownTime);
        sb.append(", mMaxRecordOneday: ").append(this.mMaxRecordOneday);
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
