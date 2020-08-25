package com.huawei.nb.model.aimodel;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class HiPlayUserAction2 extends AManagedObject {
    public static final Parcelable.Creator<HiPlayUserAction2> CREATOR = new Parcelable.Creator<HiPlayUserAction2>() {
        /* class com.huawei.nb.model.aimodel.HiPlayUserAction2.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public HiPlayUserAction2 createFromParcel(Parcel in) {
            return new HiPlayUserAction2(in);
        }

        @Override // android.os.Parcelable.Creator
        public HiPlayUserAction2[] newArray(int size) {
            return new HiPlayUserAction2[size];
        }
    };
    private String business;
    private String content_main_type;
    private String device_id;
    private String device_type;
    private Long id;
    private String reserver1;
    private String reserver2;
    private String sub_business;
    private Long timestamp;

    public HiPlayUserAction2(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.business = cursor.getString(2);
        this.sub_business = cursor.getString(3);
        this.content_main_type = cursor.getString(4);
        this.device_id = cursor.getString(5);
        this.device_type = cursor.getString(6);
        this.reserver1 = cursor.getString(7);
        this.reserver2 = cursor.getString(8);
        this.timestamp = !cursor.isNull(9) ? Long.valueOf(cursor.getLong(9)) : l;
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public HiPlayUserAction2(Parcel in) {
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
        this.content_main_type = in.readByte() == 0 ? null : in.readString();
        this.device_id = in.readByte() == 0 ? null : in.readString();
        this.device_type = in.readByte() == 0 ? null : in.readString();
        this.reserver1 = in.readByte() == 0 ? null : in.readString();
        this.reserver2 = in.readByte() == 0 ? null : in.readString();
        this.timestamp = in.readByte() != 0 ? Long.valueOf(in.readLong()) : l;
    }

    private HiPlayUserAction2(Long id2, String business2, String sub_business2, String content_main_type2, String device_id2, String device_type2, String reserver12, String reserver22, Long timestamp2) {
        this.id = id2;
        this.business = business2;
        this.sub_business = sub_business2;
        this.content_main_type = content_main_type2;
        this.device_id = device_id2;
        this.device_type = device_type2;
        this.reserver1 = reserver12;
        this.reserver2 = reserver22;
        this.timestamp = timestamp2;
    }

    public HiPlayUserAction2() {
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

    public String getContent_main_type() {
        return this.content_main_type;
    }

    public void setContent_main_type(String content_main_type2) {
        this.content_main_type = content_main_type2;
        setValue();
    }

    public String getDevice_id() {
        return this.device_id;
    }

    public void setDevice_id(String device_id2) {
        this.device_id = device_id2;
        setValue();
    }

    public String getDevice_type() {
        return this.device_type;
    }

    public void setDevice_type(String device_type2) {
        this.device_type = device_type2;
        setValue();
    }

    public String getReserver1() {
        return this.reserver1;
    }

    public void setReserver1(String reserver12) {
        this.reserver1 = reserver12;
        setValue();
    }

    public String getReserver2() {
        return this.reserver2;
    }

    public void setReserver2(String reserver22) {
        this.reserver2 = reserver22;
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
        if (this.content_main_type != null) {
            out.writeByte((byte) 1);
            out.writeString(this.content_main_type);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.device_id != null) {
            out.writeByte((byte) 1);
            out.writeString(this.device_id);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.device_type != null) {
            out.writeByte((byte) 1);
            out.writeString(this.device_type);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserver1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserver1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserver2 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserver2);
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
    public AEntityHelper<HiPlayUserAction2> getHelper() {
        return HiPlayUserAction2Helper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.aimodel.HiPlayUserAction2";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getDatabaseName() {
        return "dsAiModel";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("HiPlayUserAction2 { id: ").append(this.id);
        sb.append(", business: ").append(this.business);
        sb.append(", sub_business: ").append(this.sub_business);
        sb.append(", content_main_type: ").append(this.content_main_type);
        sb.append(", device_id: ").append(this.device_id);
        sb.append(", device_type: ").append(this.device_type);
        sb.append(", reserver1: ").append(this.reserver1);
        sb.append(", reserver2: ").append(this.reserver2);
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
