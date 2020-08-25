package com.huawei.nb.model.meta;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class BusinessInfo extends AManagedObject {
    public static final Parcelable.Creator<BusinessInfo> CREATOR = new Parcelable.Creator<BusinessInfo>() {
        /* class com.huawei.nb.model.meta.BusinessInfo.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public BusinessInfo createFromParcel(Parcel in) {
            return new BusinessInfo(in);
        }

        @Override // android.os.Parcelable.Creator
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

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public BusinessInfo(Parcel in) {
        super(in);
        String str = null;
        if (in.readByte() == 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.mBusinessName = in.readByte() == 0 ? null : in.readString();
        this.mDescription = in.readByte() != 0 ? in.readString() : str;
    }

    private BusinessInfo(Integer mId2, String mBusinessName2, String mDescription2) {
        this.mId = mId2;
        this.mBusinessName = mBusinessName2;
        this.mDescription = mDescription2;
    }

    public BusinessInfo() {
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int describeContents() {
        return 0;
    }

    public Integer getMId() {
        return this.mId;
    }

    public void setMId(Integer mId2) {
        this.mId = mId2;
        setValue();
    }

    public String getMBusinessName() {
        return this.mBusinessName;
    }

    public void setMBusinessName(String mBusinessName2) {
        this.mBusinessName = mBusinessName2;
        setValue();
    }

    public String getMDescription() {
        return this.mDescription;
    }

    public void setMDescription(String mDescription2) {
        this.mDescription = mDescription2;
        setValue();
    }

    @Override // com.huawei.odmf.core.AManagedObject
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

    @Override // com.huawei.odmf.core.AManagedObject
    public AEntityHelper<BusinessInfo> getHelper() {
        return BusinessInfoHelper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.meta.BusinessInfo";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
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

    @Override // com.huawei.odmf.core.AManagedObject
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.16";
    }

    public int getDatabaseVersionCode() {
        return 16;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
