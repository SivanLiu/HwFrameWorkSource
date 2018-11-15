package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class MetaAppProbe extends AManagedObject {
    public static final Creator<MetaAppProbe> CREATOR = new Creator<MetaAppProbe>() {
        public MetaAppProbe createFromParcel(Parcel in) {
            return new MetaAppProbe(in);
        }

        public MetaAppProbe[] newArray(int size) {
            return new MetaAppProbe[size];
        }
    };
    private String mAppVersion;
    private String mContent;
    private Integer mEventID;
    private Integer mId;
    private String mPackageName;
    private Integer mReservedInt;
    private String mReservedText;
    private Date mTimeStamp;

    public MetaAppProbe(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mEventID = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.mPackageName = cursor.getString(4);
        this.mContent = cursor.getString(5);
        this.mAppVersion = cursor.getString(6);
        if (!cursor.isNull(7)) {
            num = Integer.valueOf(cursor.getInt(7));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(8);
    }

    public MetaAppProbe(Parcel in) {
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
        this.mPackageName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mContent = in.readByte() == (byte) 0 ? null : in.readString();
        this.mAppVersion = in.readByte() == (byte) 0 ? null : in.readString();
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private MetaAppProbe(Integer mId, Date mTimeStamp, Integer mEventID, String mPackageName, String mContent, String mAppVersion, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mEventID = mEventID;
        this.mPackageName = mPackageName;
        this.mContent = mContent;
        this.mAppVersion = mAppVersion;
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

    public String getMPackageName() {
        return this.mPackageName;
    }

    public void setMPackageName(String mPackageName) {
        this.mPackageName = mPackageName;
        setValue();
    }

    public String getMContent() {
        return this.mContent;
    }

    public void setMContent(String mContent) {
        this.mContent = mContent;
        setValue();
    }

    public String getMAppVersion() {
        return this.mAppVersion;
    }

    public void setMAppVersion(String mAppVersion) {
        this.mAppVersion = mAppVersion;
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
        if (this.mPackageName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mPackageName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mContent != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mContent);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mAppVersion != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mAppVersion);
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

    public AEntityHelper<MetaAppProbe> getHelper() {
        return MetaAppProbeHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.MetaAppProbe";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("MetaAppProbe { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mEventID: ").append(this.mEventID);
        sb.append(", mPackageName: ").append(this.mPackageName);
        sb.append(", mContent: ").append(this.mContent);
        sb.append(", mAppVersion: ").append(this.mAppVersion);
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
