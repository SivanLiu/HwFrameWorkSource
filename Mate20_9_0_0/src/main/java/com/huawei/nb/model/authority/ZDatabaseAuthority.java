package com.huawei.nb.model.authority;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class ZDatabaseAuthority extends AManagedObject {
    public static final Creator<ZDatabaseAuthority> CREATOR = new Creator<ZDatabaseAuthority>() {
        public ZDatabaseAuthority createFromParcel(Parcel in) {
            return new ZDatabaseAuthority(in);
        }

        public ZDatabaseAuthority[] newArray(int size) {
            return new ZDatabaseAuthority[size];
        }
    };
    private Integer authority;
    private Long dbId;
    private String dbName;
    private Long id;
    private String packageName;
    private Long packageUid;
    private String reserved;
    private Boolean supportGroupAuthority = Boolean.valueOf(true);

    public ZDatabaseAuthority(Cursor cursor) {
        Boolean bool = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.dbId = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        this.dbName = cursor.getString(3);
        this.packageUid = cursor.isNull(4) ? null : Long.valueOf(cursor.getLong(4));
        this.packageName = cursor.getString(5);
        this.authority = cursor.isNull(6) ? null : Integer.valueOf(cursor.getInt(6));
        if (!cursor.isNull(7)) {
            bool = Boolean.valueOf(cursor.getInt(7) != 0);
        }
        this.supportGroupAuthority = bool;
        this.reserved = cursor.getString(8);
    }

    public ZDatabaseAuthority(Parcel in) {
        Boolean bool;
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.dbId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.dbName = in.readByte() == (byte) 0 ? null : in.readString();
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

    private ZDatabaseAuthority(Long id, Long dbId, String dbName, Long packageUid, String packageName, Integer authority, Boolean supportGroupAuthority, String reserved) {
        this.id = id;
        this.dbId = dbId;
        this.dbName = dbName;
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

    public Long getDbId() {
        return this.dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
        setValue();
    }

    public String getDbName() {
        return this.dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
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
        if (this.dbId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.dbId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dbName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.dbName);
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

    public AEntityHelper<ZDatabaseAuthority> getHelper() {
        return ZDatabaseAuthorityHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.authority.ZDatabaseAuthority";
    }

    public String getDatabaseName() {
        return "dsWeather";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ZDatabaseAuthority { id: ").append(this.id);
        sb.append(", dbId: ").append(this.dbId);
        sb.append(", dbName: ").append(this.dbName);
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
