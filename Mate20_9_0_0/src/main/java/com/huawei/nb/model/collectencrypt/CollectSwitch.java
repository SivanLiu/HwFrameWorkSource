package com.huawei.nb.model.collectencrypt;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class CollectSwitch extends AManagedObject {
    public static final Creator<CollectSwitch> CREATOR = new Creator<CollectSwitch>() {
        public CollectSwitch createFromParcel(Parcel in) {
            return new CollectSwitch(in);
        }

        public CollectSwitch[] newArray(int size) {
            return new CollectSwitch[size];
        }
    };
    private String mDataName;
    private String mModuleName;
    private Integer mReservedInt;
    private String mReservedText;
    private String mTimeText;

    public CollectSwitch(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mDataName = cursor.getString(1);
        this.mModuleName = cursor.getString(2);
        this.mTimeText = cursor.getString(3);
        this.mReservedInt = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        this.mReservedText = cursor.getString(5);
    }

    public CollectSwitch(Parcel in) {
        String str = null;
        super(in);
        this.mDataName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mModuleName = in.readByte() == (byte) 0 ? null : in.readString();
        this.mTimeText = in.readByte() == (byte) 0 ? null : in.readString();
        this.mReservedInt = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mReservedText = str;
    }

    private CollectSwitch(String mDataName, String mModuleName, String mTimeText, Integer mReservedInt, String mReservedText) {
        this.mDataName = mDataName;
        this.mModuleName = mModuleName;
        this.mTimeText = mTimeText;
        this.mReservedInt = mReservedInt;
        this.mReservedText = mReservedText;
    }

    public int describeContents() {
        return 0;
    }

    public String getMDataName() {
        return this.mDataName;
    }

    public void setMDataName(String mDataName) {
        this.mDataName = mDataName;
        setValue();
    }

    public String getMModuleName() {
        return this.mModuleName;
    }

    public void setMModuleName(String mModuleName) {
        this.mModuleName = mModuleName;
        setValue();
    }

    public String getMTimeText() {
        return this.mTimeText;
    }

    public void setMTimeText(String mTimeText) {
        this.mTimeText = mTimeText;
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
        if (this.mDataName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mDataName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mModuleName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mModuleName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mTimeText != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mTimeText);
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

    public AEntityHelper<CollectSwitch> getHelper() {
        return CollectSwitchHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.collectencrypt.CollectSwitch";
    }

    public String getDatabaseName() {
        return "dsCollectEncrypt";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("CollectSwitch { mDataName: ").append(this.mDataName);
        sb.append(", mModuleName: ").append(this.mModuleName);
        sb.append(", mTimeText: ").append(this.mTimeText);
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
