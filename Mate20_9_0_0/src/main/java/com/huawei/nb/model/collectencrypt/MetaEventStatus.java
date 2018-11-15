package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class MetaEventStatus extends AManagedObject {
    public static final Creator<MetaEventStatus> CREATOR = new Creator<MetaEventStatus>() {
        public MetaEventStatus createFromParcel(Parcel in) {
            return new MetaEventStatus(in);
        }

        public MetaEventStatus[] newArray(int size) {
            return new MetaEventStatus[size];
        }
    };
    private Date mBegin;
    private Date mEnd;
    private String mEventParam;
    private Integer mId;
    private Integer mReservedInt;
    private String mReservedText;
    private String mStatus;
    private String mStatusName;

    public MetaEventStatus(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mStatusName = cursor.getString(2);
        this.mStatus = cursor.getString(3);
        this.mBegin = cursor.isNull(4) ? null : new Date(cursor.getLong(4));
        this.mEnd = cursor.isNull(5) ? null : new Date(cursor.getLong(5));
        this.mEventParam = cursor.getString(6);
        if (!cursor.isNull(7)) {
            num = Integer.valueOf(cursor.getInt(7));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(8);
    }

    public MetaEventStatus(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mStatusName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mStatus = in.readByte() == (byte) 0 ? null : in.readString();
        this.mBegin = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mEnd = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mEventParam = in.readByte() == (byte) 0 ? null : in.readString();
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private MetaEventStatus(Integer mId, String mStatusName, String mStatus, Date mBegin, Date mEnd, String mEventParam, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mStatusName = mStatusName;
        this.mStatus = mStatus;
        this.mBegin = mBegin;
        this.mEnd = mEnd;
        this.mEventParam = mEventParam;
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

    public String getMStatusName() {
        return this.mStatusName;
    }

    public void setMStatusName(String mStatusName) {
        this.mStatusName = mStatusName;
        setValue();
    }

    public String getMStatus() {
        return this.mStatus;
    }

    public void setMStatus(String mStatus) {
        this.mStatus = mStatus;
        setValue();
    }

    public Date getMBegin() {
        return this.mBegin;
    }

    public void setMBegin(Date mBegin) {
        this.mBegin = mBegin;
        setValue();
    }

    public Date getMEnd() {
        return this.mEnd;
    }

    public void setMEnd(Date mEnd) {
        this.mEnd = mEnd;
        setValue();
    }

    public String getMEventParam() {
        return this.mEventParam;
    }

    public void setMEventParam(String mEventParam) {
        this.mEventParam = mEventParam;
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
        if (this.mStatusName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mStatusName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mStatus != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mStatus);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBegin != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mBegin.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mEnd != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mEnd.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mEventParam != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mEventParam);
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

    public AEntityHelper<MetaEventStatus> getHelper() {
        return MetaEventStatusHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.MetaEventStatus";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("MetaEventStatus { mId: ").append(this.mId);
        sb.append(", mStatusName: ").append(this.mStatusName);
        sb.append(", mStatus: ").append(this.mStatus);
        sb.append(", mBegin: ").append(this.mBegin);
        sb.append(", mEnd: ").append(this.mEnd);
        sb.append(", mEventParam: ").append(this.mEventParam);
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
