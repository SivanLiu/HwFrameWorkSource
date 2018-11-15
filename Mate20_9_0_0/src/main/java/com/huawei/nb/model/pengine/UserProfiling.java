package com.huawei.nb.model.pengine;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class UserProfiling extends AManagedObject {
    public static final Creator<UserProfiling> CREATOR = new Creator<UserProfiling>() {
        public UserProfiling createFromParcel(Parcel in) {
            return new UserProfiling(in);
        }

        public UserProfiling[] newArray(int size) {
            return new UserProfiling[size];
        }
    };
    private Integer id;
    private Integer level;
    private String parent;
    private Long timestamp;
    private String uriKey;
    private String uriValue;

    public UserProfiling(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.parent = cursor.getString(2);
        this.level = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.uriKey = cursor.getString(4);
        this.uriValue = cursor.getString(5);
        if (!cursor.isNull(6)) {
            l = Long.valueOf(cursor.getLong(6));
        }
        this.timestamp = l;
    }

    public UserProfiling(Parcel in) {
        Long l = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.parent = in.readByte() == (byte) 0 ? null : in.readString();
        this.level = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.uriKey = in.readByte() == (byte) 0 ? null : in.readString();
        this.uriValue = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            l = Long.valueOf(in.readLong());
        }
        this.timestamp = l;
    }

    private UserProfiling(Integer id, String parent, Integer level, String uriKey, String uriValue, Long timestamp) {
        this.id = id;
        this.parent = parent;
        this.level = level;
        this.uriKey = uriKey;
        this.uriValue = uriValue;
        this.timestamp = timestamp;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
        setValue();
    }

    public String getParent() {
        return this.parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
        setValue();
    }

    public Integer getLevel() {
        return this.level;
    }

    public void setLevel(Integer level) {
        this.level = level;
        setValue();
    }

    public String getUriKey() {
        return this.uriKey;
    }

    public void setUriKey(String uriKey) {
        this.uriKey = uriKey;
        setValue();
    }

    public String getUriValue() {
        return this.uriValue;
    }

    public void setUriValue(String uriValue) {
        this.uriValue = uriValue;
        setValue();
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.id.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.parent != null) {
            out.writeByte((byte) 1);
            out.writeString(this.parent);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.level != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.level.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.uriKey != null) {
            out.writeByte((byte) 1);
            out.writeString(this.uriKey);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.uriValue != null) {
            out.writeByte((byte) 1);
            out.writeString(this.uriValue);
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

    public AEntityHelper<UserProfiling> getHelper() {
        return UserProfilingHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.pengine.UserProfiling";
    }

    public String getDatabaseName() {
        return "dsPengineData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("UserProfiling { id: ").append(this.id);
        sb.append(", parent: ").append(this.parent);
        sb.append(", level: ").append(this.level);
        sb.append(", uriKey: ").append(this.uriKey);
        sb.append(", uriValue: ").append(this.uriValue);
        sb.append(", timestamp: ").append(this.timestamp);
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
        return "0.0.7";
    }

    public int getDatabaseVersionCode() {
        return 7;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
