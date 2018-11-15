package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class MetaEventAlarm extends AManagedObject {
    public static final Creator<MetaEventAlarm> CREATOR = new Creator<MetaEventAlarm>() {
        public MetaEventAlarm createFromParcel(Parcel in) {
            return new MetaEventAlarm(in);
        }

        public MetaEventAlarm[] newArray(int size) {
            return new MetaEventAlarm[size];
        }
    };
    private String mAddress;
    private Date mAlarmTime;
    private Integer mEventID;
    private String mEventInfo;
    private Integer mId;
    private Integer mReservedInt;
    private String mReservedText;
    private Date mTimeStamp;

    public MetaEventAlarm(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mEventID = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.mAlarmTime = cursor.isNull(4) ? null : new Date(cursor.getLong(4));
        this.mEventInfo = cursor.getString(5);
        this.mAddress = cursor.getString(6);
        if (!cursor.isNull(7)) {
            num = Integer.valueOf(cursor.getInt(7));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(8);
    }

    public MetaEventAlarm(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mEventID = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.mAlarmTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mEventInfo = in.readByte() == (byte) 0 ? null : in.readString();
        this.mAddress = in.readByte() == (byte) 0 ? null : in.readString();
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private MetaEventAlarm(Integer mId, Date mTimeStamp, Integer mEventID, Date mAlarmTime, String mEventInfo, String mAddress, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mEventID = mEventID;
        this.mAlarmTime = mAlarmTime;
        this.mEventInfo = mEventInfo;
        this.mAddress = mAddress;
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

    public Integer getMEventID() {
        return this.mEventID;
    }

    public void setMEventID(Integer mEventID) {
        this.mEventID = mEventID;
        setValue();
    }

    public Date getMAlarmTime() {
        return this.mAlarmTime;
    }

    public void setMAlarmTime(Date mAlarmTime) {
        this.mAlarmTime = mAlarmTime;
        setValue();
    }

    public String getMEventInfo() {
        return this.mEventInfo;
    }

    public void setMEventInfo(String mEventInfo) {
        this.mEventInfo = mEventInfo;
        setValue();
    }

    public String getMAddress() {
        return this.mAddress;
    }

    public void setMAddress(String mAddress) {
        this.mAddress = mAddress;
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
        if (this.mEventID != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mEventID.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mAlarmTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mAlarmTime.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mEventInfo != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mEventInfo);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mAddress != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mAddress);
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

    public AEntityHelper<MetaEventAlarm> getHelper() {
        return MetaEventAlarmHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.MetaEventAlarm";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("MetaEventAlarm { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mEventID: ").append(this.mEventID);
        sb.append(", mAlarmTime: ").append(this.mAlarmTime);
        sb.append(", mEventInfo: ").append(this.mEventInfo);
        sb.append(", mAddress: ").append(this.mAddress);
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
