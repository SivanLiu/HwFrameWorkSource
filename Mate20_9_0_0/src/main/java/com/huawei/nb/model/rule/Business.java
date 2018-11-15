package com.huawei.nb.model.rule;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class Business extends AManagedObject {
    public static final Creator<Business> CREATOR = new Creator<Business>() {
        public Business createFromParcel(Parcel in) {
            return new Business(in);
        }

        public Business[] newArray(int size) {
            return new Business[size];
        }
    };
    private Integer businessType;
    private String description;
    private Long id;
    private Integer level;
    private String name;
    private Long parentId;

    public Business(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.name = cursor.getString(2);
        this.businessType = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.level = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        this.description = cursor.getString(5);
        if (!cursor.isNull(6)) {
            l = Long.valueOf(cursor.getLong(6));
        }
        this.parentId = l;
    }

    public Business(Parcel in) {
        Long l = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.name = in.readByte() == (byte) 0 ? null : in.readString();
        this.businessType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.level = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.description = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            l = Long.valueOf(in.readLong());
        }
        this.parentId = l;
    }

    private Business(Long id, String name, Integer businessType, Integer level, String description, Long parentId) {
        this.id = id;
        this.name = name;
        this.businessType = businessType;
        this.level = level;
        this.description = description;
        this.parentId = parentId;
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

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        setValue();
    }

    public Integer getBusinessType() {
        return this.businessType;
    }

    public void setBusinessType(Integer businessType) {
        this.businessType = businessType;
        setValue();
    }

    public Integer getLevel() {
        return this.level;
    }

    public void setLevel(Integer level) {
        this.level = level;
        setValue();
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
        setValue();
    }

    public Long getParentId() {
        return this.parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
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
        if (this.name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.name);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.businessType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.businessType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.level != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.level.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.description != null) {
            out.writeByte((byte) 1);
            out.writeString(this.description);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.parentId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.parentId.longValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<Business> getHelper() {
        return BusinessHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.rule.Business";
    }

    public String getDatabaseName() {
        return "dsRule";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Business { id: ").append(this.id);
        sb.append(", name: ").append(this.name);
        sb.append(", businessType: ").append(this.businessType);
        sb.append(", level: ").append(this.level);
        sb.append(", description: ").append(this.description);
        sb.append(", parentId: ").append(this.parentId);
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
        return "0.0.3";
    }

    public int getDatabaseVersionCode() {
        return 3;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
