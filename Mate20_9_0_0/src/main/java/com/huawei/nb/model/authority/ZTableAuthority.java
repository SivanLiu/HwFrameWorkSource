package com.huawei.nb.model.authority;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class ZTableAuthority extends AManagedObject {
    public static final Creator<ZTableAuthority> CREATOR = new Creator<ZTableAuthority>() {
        public ZTableAuthority createFromParcel(Parcel in) {
            return new ZTableAuthority(in);
        }

        public ZTableAuthority[] newArray(int size) {
            return new ZTableAuthority[size];
        }
    };
    private Integer authority;
    private Long id;
    private String packageName;
    private Long packageUid;
    private String reserved;
    private Boolean supportGroupAuthority = Boolean.valueOf(true);
    private Long tableId;
    private String tableName;

    public ZTableAuthority(Cursor cursor) {
        Boolean bool = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.tableId = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        this.tableName = cursor.getString(3);
        this.packageUid = cursor.isNull(4) ? null : Long.valueOf(cursor.getLong(4));
        this.packageName = cursor.getString(5);
        this.authority = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        if (!cursor.isNull(7)) {
            bool = Boolean.valueOf(cursor.getInt(7) != 0);
        }
        this.supportGroupAuthority = bool;
        this.reserved = cursor.getString(8);
    }

    public ZTableAuthority(Parcel in) {
        Boolean bool;
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.tableId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.tableName = in.readByte() == (byte) 0 ? null : in.readString();
        this.packageUid = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.packageName = in.readByte() == (byte) 0 ? null : in.readString();
        this.authority = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() == (byte) 0) {
            bool = null;
        } else {
            bool = Boolean.valueOf(in.readByte() != (byte) 0);
        }
        this.supportGroupAuthority = bool;
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reserved = str;
    }

    private ZTableAuthority(Long id, Long tableId, String tableName, Long packageUid, String packageName, Integer authority, Boolean supportGroupAuthority, String reserved) {
        this.id = id;
        this.tableId = tableId;
        this.tableName = tableName;
        this.packageUid = packageUid;
        this.packageName = packageName;
        this.authority = authority;
        this.supportGroupAuthority = supportGroupAuthority;
        this.reserved = reserved;
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

    public Long getTableId() {
        return this.tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
        setValue();
    }

    public String getTableName() {
        return this.tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
        setValue();
    }

    public Long getPackageUid() {
        return this.packageUid;
    }

    public void setPackageUid(Long packageUid) {
        this.packageUid = packageUid;
        setValue();
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
        setValue();
    }

    public Integer getAuthority() {
        return this.authority;
    }

    public void setAuthority(Integer authority) {
        this.authority = authority;
        setValue();
    }

    public Boolean getSupportGroupAuthority() {
        return this.supportGroupAuthority;
    }

    public void setSupportGroupAuthority(Boolean supportGroupAuthority) {
        this.supportGroupAuthority = supportGroupAuthority;
        setValue();
    }

    public String getReserved() {
        return this.reserved;
    }

    public void setReserved(String reserved) {
        this.reserved = reserved;
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
        if (this.tableId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.tableId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.tableName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.tableName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.packageUid != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.packageUid.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.packageName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.packageName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.authority != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.authority.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.supportGroupAuthority != null) {
            byte b;
            out.writeByte((byte) 1);
            if (this.supportGroupAuthority.booleanValue()) {
                b = (byte) 1;
            } else {
                b = (byte) 0;
            }
            out.writeByte(b);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<ZTableAuthority> getHelper() {
        return ZTableAuthorityHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.authority.ZTableAuthority";
    }

    public String getDatabaseName() {
        return "dsWeather";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ZTableAuthority { id: ").append(this.id);
        sb.append(", tableId: ").append(this.tableId);
        sb.append(", tableName: ").append(this.tableName);
        sb.append(", packageUid: ").append(this.packageUid);
        sb.append(", packageName: ").append(this.packageName);
        sb.append(", authority: ").append(this.authority);
        sb.append(", supportGroupAuthority: ").append(this.supportGroupAuthority);
        sb.append(", reserved: ").append(this.reserved);
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
