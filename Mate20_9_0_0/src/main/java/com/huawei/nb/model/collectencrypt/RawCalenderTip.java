package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RawCalenderTip extends AManagedObject {
    public static final Creator<RawCalenderTip> CREATOR = new Creator<RawCalenderTip>() {
        public RawCalenderTip createFromParcel(Parcel in) {
            return new RawCalenderTip(in);
        }

        public RawCalenderTip[] newArray(int size) {
            return new RawCalenderTip[size];
        }
    };
    private Integer mId;
    private Integer mReservedInt;
    private String mReservedText;
    private Date mTimeStamp;
    private Date mTipAlarmTime;
    private String mTipContent;
    private Date mTipEndTime;
    private Date mTipStartTime;
    private String mTipTitle;

    public RawCalenderTip(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mTipTitle = cursor.getString(3);
        this.mTipContent = cursor.getString(4);
        this.mTipStartTime = cursor.isNull(5) ? null : new Date(cursor.getLong(5));
        this.mTipEndTime = cursor.isNull(6) ? null : new Date(cursor.getLong(6));
        this.mTipAlarmTime = cursor.isNull(7) ? null : new Date(cursor.getLong(7));
        if (!cursor.isNull(8)) {
            num = Integer.valueOf(cursor.getInt(8));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(9);
    }

    public RawCalenderTip(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mTipTitle = in.readByte() == (byte) 0 ? null : in.readString();
        this.mTipContent = in.readByte() == (byte) 0 ? null : in.readString();
        this.mTipStartTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mTipEndTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mTipAlarmTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private RawCalenderTip(Integer mId, Date mTimeStamp, String mTipTitle, String mTipContent, Date mTipStartTime, Date mTipEndTime, Date mTipAlarmTime, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mTipTitle = mTipTitle;
        this.mTipContent = mTipContent;
        this.mTipStartTime = mTipStartTime;
        this.mTipEndTime = mTipEndTime;
        this.mTipAlarmTime = mTipAlarmTime;
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

    public String getMTipTitle() {
        return this.mTipTitle;
    }

    public void setMTipTitle(String mTipTitle) {
        this.mTipTitle = mTipTitle;
        setValue();
    }

    public String getMTipContent() {
        return this.mTipContent;
    }

    public void setMTipContent(String mTipContent) {
        this.mTipContent = mTipContent;
        setValue();
    }

    public Date getMTipStartTime() {
        return this.mTipStartTime;
    }

    public void setMTipStartTime(Date mTipStartTime) {
        this.mTipStartTime = mTipStartTime;
        setValue();
    }

    public Date getMTipEndTime() {
        return this.mTipEndTime;
    }

    public void setMTipEndTime(Date mTipEndTime) {
        this.mTipEndTime = mTipEndTime;
        setValue();
    }

    public Date getMTipAlarmTime() {
        return this.mTipAlarmTime;
    }

    public void setMTipAlarmTime(Date mTipAlarmTime) {
        this.mTipAlarmTime = mTipAlarmTime;
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
        if (this.mTipTitle != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mTipTitle);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mTipContent != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mTipContent);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mTipStartTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mTipStartTime.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mTipEndTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mTipEndTime.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mTipAlarmTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mTipAlarmTime.getTime());
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

    public AEntityHelper<RawCalenderTip> getHelper() {
        return RawCalenderTipHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RawCalenderTip";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RawCalenderTip { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mTipTitle: ").append(this.mTipTitle);
        sb.append(", mTipContent: ").append(this.mTipContent);
        sb.append(", mTipStartTime: ").append(this.mTipStartTime);
        sb.append(", mTipEndTime: ").append(this.mTipEndTime);
        sb.append(", mTipAlarmTime: ").append(this.mTipAlarmTime);
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
