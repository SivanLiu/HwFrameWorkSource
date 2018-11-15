package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class RawBrowserBookmark extends AManagedObject {
    public static final Creator<RawBrowserBookmark> CREATOR = new Creator<RawBrowserBookmark>() {
        public RawBrowserBookmark createFromParcel(Parcel in) {
            return new RawBrowserBookmark(in);
        }

        public RawBrowserBookmark[] newArray(int size) {
            return new RawBrowserBookmark[size];
        }
    };
    private Date mBookmarkAddTime;
    private String mBookmarkTitle;
    private String mBookmarkUrl;
    private Integer mId;
    private Integer mReservedInt;
    private String mReservedText;
    private Date mTimeStamp;

    public RawBrowserBookmark(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mTimeStamp = cursor.isNull(2) ? null : new Date(cursor.getLong(2));
        this.mBookmarkTitle = cursor.getString(3);
        this.mBookmarkUrl = cursor.getString(4);
        this.mBookmarkAddTime = cursor.isNull(5) ? null : new Date(cursor.getLong(5));
        if (!cursor.isNull(6)) {
            num = Integer.valueOf(cursor.getInt(6));
        }
        this.mReservedInt = num;
        this.mReservedText = cursor.getString(7);
    }

    public RawBrowserBookmark(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mTimeStamp = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mBookmarkTitle = in.readByte() == (byte) 0 ? null : in.readString();
        this.mBookmarkUrl = in.readByte() == (byte) 0 ? null : in.readString();
        this.mBookmarkAddTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private RawBrowserBookmark(Integer mId, Date mTimeStamp, String mBookmarkTitle, String mBookmarkUrl, Date mBookmarkAddTime, Integer mReservedInt, String mReservedText) {
        this.mId = mId;
        this.mTimeStamp = mTimeStamp;
        this.mBookmarkTitle = mBookmarkTitle;
        this.mBookmarkUrl = mBookmarkUrl;
        this.mBookmarkAddTime = mBookmarkAddTime;
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

    public String getMBookmarkTitle() {
        return this.mBookmarkTitle;
    }

    public void setMBookmarkTitle(String mBookmarkTitle) {
        this.mBookmarkTitle = mBookmarkTitle;
        setValue();
    }

    public String getMBookmarkUrl() {
        return this.mBookmarkUrl;
    }

    public void setMBookmarkUrl(String mBookmarkUrl) {
        this.mBookmarkUrl = mBookmarkUrl;
        setValue();
    }

    public Date getMBookmarkAddTime() {
        return this.mBookmarkAddTime;
    }

    public void setMBookmarkAddTime(Date mBookmarkAddTime) {
        this.mBookmarkAddTime = mBookmarkAddTime;
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
        if (this.mBookmarkTitle != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mBookmarkTitle);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBookmarkUrl != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mBookmarkUrl);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mBookmarkAddTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.mBookmarkAddTime.getTime());
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

    public AEntityHelper<RawBrowserBookmark> getHelper() {
        return RawBrowserBookmarkHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.RawBrowserBookmark";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("RawBrowserBookmark { mId: ").append(this.mId);
        sb.append(", mTimeStamp: ").append(this.mTimeStamp);
        sb.append(", mBookmarkTitle: ").append(this.mBookmarkTitle);
        sb.append(", mBookmarkUrl: ").append(this.mBookmarkUrl);
        sb.append(", mBookmarkAddTime: ").append(this.mBookmarkAddTime);
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
