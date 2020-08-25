package com.huawei.nb.model.aimodel;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class HiPlayUserAction1 extends AManagedObject {
    public static final Parcelable.Creator<HiPlayUserAction1> CREATOR = new Parcelable.Creator<HiPlayUserAction1>() {
        /* class com.huawei.nb.model.aimodel.HiPlayUserAction1.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public HiPlayUserAction1 createFromParcel(Parcel in) {
            return new HiPlayUserAction1(in);
        }

        @Override // android.os.Parcelable.Creator
        public HiPlayUserAction1[] newArray(int size) {
            return new HiPlayUserAction1[size];
        }
    };
    private String business;
    private Long id;
    private String sub_business;
    private Long timestamp;

    public HiPlayUserAction1(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.business = cursor.getString(2);
        this.sub_business = cursor.getString(3);
        this.timestamp = !cursor.isNull(4) ? Long.valueOf(cursor.getLong(4)) : l;
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public HiPlayUserAction1(Parcel in) {
        super(in);
        Long l = null;
        if (in.readByte() == 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.business = in.readByte() == 0 ? null : in.readString();
        this.sub_business = in.readByte() == 0 ? null : in.readString();
        this.timestamp = in.readByte() != 0 ? Long.valueOf(in.readLong()) : l;
    }

    private HiPlayUserAction1(Long id2, String business2, String sub_business2, Long timestamp2) {
        this.id = id2;
        this.business = business2;
        this.sub_business = sub_business2;
        this.timestamp = timestamp2;
    }

    public HiPlayUserAction1() {
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int describeContents() {
        return 0;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id2) {
        this.id = id2;
        setValue();
    }

    public String getBusiness() {
        return this.business;
    }

    public void setBusiness(String business2) {
        this.business = business2;
        setValue();
    }

    public String getSub_business() {
        return this.sub_business;
    }

    public void setSub_business(String sub_business2) {
        this.sub_business = sub_business2;
        setValue();
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Long timestamp2) {
        this.timestamp = timestamp2;
        setValue();
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.id.longValue());
        } else {
            out.writeByte((byte) 0);
            out.writeLong(1);
        }
        if (this.business != null) {
            out.writeByte((byte) 1);
            out.writeString(this.business);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.sub_business != null) {
            out.writeByte((byte) 1);
            out.writeString(this.sub_business);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.timestamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.timestamp.longValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public AEntityHelper<HiPlayUserAction1> getHelper() {
        return HiPlayUserAction1Helper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.aimodel.HiPlayUserAction1";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getDatabaseName() {
        return "dsAiModel";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("HiPlayUserAction1 { id: ").append(this.id);
        sb.append(", business: ").append(this.business);
        sb.append(", sub_business: ").append(this.sub_business);
        sb.append(", timestamp: ").append(this.timestamp);
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
        return "0.0.13";
    }

    public int getDatabaseVersionCode() {
        return 13;
    }

    public String getEntityVersion() {
        return "0.0.2";
    }

    public int getEntityVersionCode() {
        return 2;
    }
}
