package com.huawei.nb.model.aimodel;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class AiModelAuthority extends AManagedObject {
    public static final Creator<AiModelAuthority> CREATOR = new Creator<AiModelAuthority>() {
        public AiModelAuthority createFromParcel(Parcel in) {
            return new AiModelAuthority(in);
        }

        public AiModelAuthority[] newArray(int size) {
            return new AiModelAuthority[size];
        }
    };
    private Long aimodel_id;
    private Integer authority;
    private String business_attribute;
    private String business_name;
    private Long id;
    private String reserved_1;

    public AiModelAuthority(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.aimodel_id = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        this.business_name = cursor.getString(3);
        this.business_attribute = cursor.getString(4);
        if (!cursor.isNull(5)) {
            num = Integer.valueOf(cursor.getInt(5));
        }
        this.authority = num;
        this.reserved_1 = cursor.getString(6);
    }

    public AiModelAuthority(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.aimodel_id = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.business_name = in.readByte() == (byte) 0 ? null : in.readString();
        this.business_attribute = in.readByte() == (byte) 0 ? null : in.readString();
        this.authority = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reserved_1 = str;
    }

    private AiModelAuthority(Long id, Long aimodel_id, String business_name, String business_attribute, Integer authority, String reserved_1) {
        this.id = id;
        this.aimodel_id = aimodel_id;
        this.business_name = business_name;
        this.business_attribute = business_attribute;
        this.authority = authority;
        this.reserved_1 = reserved_1;
    }

    public int describeContents() {
        return 0;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
        setValue();
    }

    public Long getAimodel_id() {
        return this.aimodel_id;
    }

    public void setAimodel_id(Long aimodel_id) {
        this.aimodel_id = aimodel_id;
        setValue();
    }

    public String getBusiness_name() {
        return this.business_name;
    }

    public void setBusiness_name(String business_name) {
        this.business_name = business_name;
        setValue();
    }

    public String getBusiness_attribute() {
        return this.business_attribute;
    }

    public void setBusiness_attribute(String business_attribute) {
        this.business_attribute = business_attribute;
        setValue();
    }

    public Integer getAuthority() {
        return this.authority;
    }

    public void setAuthority(Integer authority) {
        this.authority = authority;
        setValue();
    }

    public String getReserved_1() {
        return this.reserved_1;
    }

    public void setReserved_1(String reserved_1) {
        this.reserved_1 = reserved_1;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.id.longValue());
        } else {
            out.writeByte((byte) 0);
            out.writeLong(1);
        }
        if (this.aimodel_id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.aimodel_id.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.business_name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.business_name);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.business_attribute != null) {
            out.writeByte((byte) 1);
            out.writeString(this.business_attribute);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.authority != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.authority.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved_1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved_1);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<AiModelAuthority> getHelper() {
        return AiModelAuthorityHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.aimodel.AiModelAuthority";
    }

    public String getDatabaseName() {
        return "dsAiModel";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("AiModelAuthority { id: ").append(this.id);
        sb.append(", aimodel_id: ").append(this.aimodel_id);
        sb.append(", business_name: ").append(this.business_name);
        sb.append(", business_attribute: ").append(this.business_attribute);
        sb.append(", authority: ").append(this.authority);
        sb.append(", reserved_1: ").append(this.reserved_1);
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
        return "0.0.8";
    }

    public int getDatabaseVersionCode() {
        return 8;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
