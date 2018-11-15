package com.huawei.nb.model.meta;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class BusinessInfo extends AManagedObject {
    public static final Creator<BusinessInfo> CREATOR = new Creator<BusinessInfo>() {
        public BusinessInfo createFromParcel(Parcel in) {
            return new BusinessInfo(in);
        }

        public BusinessInfo[] newArray(int size) {
            return new BusinessInfo[size];
        }
    };
    private String mBusinessName;
    private String mDescription;
    private Integer mId;

    public BusinessInfo(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.mBusinessName = cursor.getString(2);
        this.mDescription = cursor.getString(3);
    }

    public BusinessInfo(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mBusinessName = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.mDescription = str;
    }

    private BusinessInfo(Integer mId, String mBusinessName, String mDescription) {
        this.mId = mId;
        this.mBusinessName = mBusinessName;
        this.mDescription = mDescription;
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

    public String getMBusinessName() {
        return this.mBusinessName;
    }

    public void setMBusinessName(String mBusinessName) {
        this.mBusinessName = mBusinessName;
        setValue();
    }

    public String getMDescription() {
        return this.mDescription;
    }

    public void setMDescription(String mDescription) {
        this.mDescription = mDescription;
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
        if (this.mBusinessName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mBusinessName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.mDescription != null) {
            out.writeByte((byte) 1);
            out.writeString(this.mDescription);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<BusinessInfo> getHelper() {
        return BusinessInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.meta.BusinessInfo";
    }

    public String getDatabaseName() {
        return "dsMeta";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("BusinessInfo { mId: ").append(this.mId);
        sb.append(", mBusinessName: ").append(this.mBusinessName);
        sb.append(", mDescription: ").append(this.mDescription);
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
        return "0.0.13";
    }

    public int getDatabaseVersionCode() {
        return 13;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
