package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class RawAppProbeCount extends AManagedObject {
    public static final Creator<RawAppProbeCount> CREATOR = new Creator<RawAppProbeCount>() {
        public RawAppProbeCount createFromParcel(Parcel in) {
            return new RawAppProbeCount(in);
        }

        public RawAppProbeCount[] newArray(int size) {
            return new RawAppProbeCount[size];
        }
    };
    private Integer mCount;
    private Integer mEventID;
    private Integer mId;
    private String mPackageName;
    private Integer mReservedInt;
    private String mReservedText;

    public RawAppProbeCount(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mPackageName = cursor.getString(2);
        this.mCount = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.mEventID = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        if (!cursor.isNull(5)) {
            num = Integer.valueOf(cursor.getInt(5));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(6);
    }

    public RawAppProbeCount(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mPackageName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mCount = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mEventID = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private RawAppProbeCount(Integer mId, String mPackageName, Integer mCount, Integer mEventID, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mPackageName = mPackageName;
        this.mCount = mCount;
        this.mEventID = mEventID;
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

    public String getMPackageName() {
        return this.mPackageName;
    }

    public void setMPackageName(String mPackageName) {
        this.mPackageName = mPackageName;
        setValue();
    }

    public Integer getMCount() {
        return this.mCount;
    }

    public void setMCount(Integer mCount) {
        this.mCount = mCount;
        setValue();
    }

    public Integer getMEventID() {
        return this.mEventID;
    }

    public void setMEventID(Integer mEventID) {
        this.mEventID = mEventID;
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
        if (this.mPackageName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mPackageName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mCount != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mCount.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mEventID != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mEventID.intValue());
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

    public AEntityHelper<RawAppProbeCount> getHelper() {
        return RawAppProbeCountHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RawAppProbeCount";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RawAppProbeCount { mId: ").append(this.mId);
        sb.append(", mPackageName: ").append(this.mPackageName);
        sb.append(", mCount: ").append(this.mCount);
        sb.append(", mEventID: ").append(this.mEventID);
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
