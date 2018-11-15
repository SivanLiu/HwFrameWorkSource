package com.huawei.nb.model.rule;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class Operator extends AManagedObject {
    public static final Creator<Operator> CREATOR = new Creator<Operator>() {
        public Operator createFromParcel(Parcel in) {
            return new Operator(in);
        }

        public Operator[] newArray(int size) {
            return new Operator[size];
        }
    };
    private Long id;
    private String name;
    private Long parentId;
    private String type;

    public Operator(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.name = cursor.getString(2);
        this.type = cursor.getString(3);
        if (!cursor.isNull(4)) {
            l = Long.valueOf(cursor.getLong(4));
        }
        this.parentId = l;
    }

    public Operator(Parcel in) {
        Long l = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.name = in.readByte() == (byte) 0 ? null : in.readString();
        this.type = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            l = Long.valueOf(in.readLong());
        }
        this.parentId = l;
    }

    private Operator(Long id, String name, String type, Long parentId) {
        this.id = id;
        this.name = name;
        this.type = type;
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

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
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
        if (this.type != null) {
            out.writeByte((byte) 1);
            out.writeString(this.type);
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

    public AEntityHelper<Operator> getHelper() {
        return OperatorHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.rule.Operator";
    }

    public String getDatabaseName() {
        return "dsRule";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Operator { id: ").append(this.id);
        sb.append(", name: ").append(this.name);
        sb.append(", type: ").append(this.type);
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
