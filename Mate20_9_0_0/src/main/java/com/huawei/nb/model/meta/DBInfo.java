package com.huawei.nb.model.meta;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class DBInfo extends AManagedObject {
    public static final Creator<DBInfo> CREATOR = new Creator<DBInfo>() {
        public DBInfo createFromParcel(Parcel in) {
            return new DBInfo(in);
        }

        public DBInfo[] newArray(int size) {
            return new DBInfo[size];
        }
    };
    private Integer actionAfterFull;
    private Long capacity;
    private String config;
    private String dataXml;
    private String dbName;
    private String dbPath;
    private Integer dbType;
    private String description;
    private Integer initMode;
    private Boolean isCreate;
    private Boolean isEncrypt;
    private Integer mId;
    private String modelXml;
    private Integer ownerBusinessId;
    private Integer recovery;

    public DBInfo(Cursor cursor) {
        Boolean bool;
        boolean z = true;
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.mId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.dbName = cursor.getString(2);
        this.dbPath = cursor.getString(3);
        this.dbType = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        this.description = cursor.getString(5);
        this.modelXml = cursor.getString(6);
        this.dataXml = cursor.getString(7);
        if (cursor.isNull(8)) {
            bool = null;
        } else {
            bool = Boolean.valueOf(cursor.getInt(8) != 0);
        }
        this.isEncrypt = bool;
        this.ownerBusinessId = cursor.isNull(9) ? null : Integer.valueOf(cursor.getInt(9));
        this.recovery = cursor.isNull(10) ? null : Integer.valueOf(cursor.getInt(10));
        if (cursor.isNull(11)) {
            bool = null;
        } else {
            if (cursor.getInt(11) == 0) {
                z = false;
            }
            bool = Boolean.valueOf(z);
        }
        this.isCreate = bool;
        this.initMode = cursor.isNull(12) ? null : Integer.valueOf(cursor.getInt(12));
        this.capacity = cursor.isNull(13) ? null : Long.valueOf(cursor.getLong(13));
        if (!cursor.isNull(14)) {
            num = Integer.valueOf(cursor.getInt(14));
        }
        this.actionAfterFull = num;
        this.config = cursor.getString(15);
    }

    public DBInfo(Parcel in) {
        Boolean bool;
        boolean z = true;
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.mId = null;
            in.readInt();
        } else {
            this.mId = Integer.valueOf(in.readInt());
        }
        this.dbName = in.readByte() == (byte) 0 ? null : in.readString();
        this.dbPath = in.readByte() == (byte) 0 ? null : in.readString();
        this.dbType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.description = in.readByte() == (byte) 0 ? null : in.readString();
        this.modelXml = in.readByte() == (byte) 0 ? null : in.readString();
        this.dataXml = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() == (byte) 0) {
            bool = null;
        } else {
            bool = Boolean.valueOf(in.readByte() != (byte) 0);
        }
        this.isEncrypt = bool;
        this.ownerBusinessId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.recovery = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() == (byte) 0) {
            bool = null;
        } else {
            if (in.readByte() == (byte) 0) {
                z = false;
            }
            bool = Boolean.valueOf(z);
        }
        this.isCreate = bool;
        this.initMode = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.capacity = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.actionAfterFull = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.config = str;
    }

    private DBInfo(Integer mId, String dbName, String dbPath, Integer dbType, String description, String modelXml, String dataXml, Boolean isEncrypt, Integer ownerBusinessId, Integer recovery, Boolean isCreate, Integer initMode, Long capacity, Integer actionAfterFull, String config) {
        this.mId = mId;
        this.dbName = dbName;
        this.dbPath = dbPath;
        this.dbType = dbType;
        this.description = description;
        this.modelXml = modelXml;
        this.dataXml = dataXml;
        this.isEncrypt = isEncrypt;
        this.ownerBusinessId = ownerBusinessId;
        this.recovery = recovery;
        this.isCreate = isCreate;
        this.initMode = initMode;
        this.capacity = capacity;
        this.actionAfterFull = actionAfterFull;
        this.config = config;
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

    public String getDbName() {
        return this.dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
        setValue();
    }

    public String getDbPath() {
        return this.dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
        setValue();
    }

    public Integer getDbType() {
        return this.dbType;
    }

    public void setDbType(Integer dbType) {
        this.dbType = dbType;
        setValue();
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
        setValue();
    }

    public String getModelXml() {
        return this.modelXml;
    }

    public void setModelXml(String modelXml) {
        this.modelXml = modelXml;
        setValue();
    }

    public String getDataXml() {
        return this.dataXml;
    }

    public void setDataXml(String dataXml) {
        this.dataXml = dataXml;
        setValue();
    }

    public Boolean getIsEncrypt() {
        return this.isEncrypt;
    }

    public void setIsEncrypt(Boolean isEncrypt) {
        this.isEncrypt = isEncrypt;
        setValue();
    }

    public Integer getOwnerBusinessId() {
        return this.ownerBusinessId;
    }

    public void setOwnerBusinessId(Integer ownerBusinessId) {
        this.ownerBusinessId = ownerBusinessId;
        setValue();
    }

    public Integer getRecovery() {
        return this.recovery;
    }

    public void setRecovery(Integer recovery) {
        this.recovery = recovery;
        setValue();
    }

    public Boolean getIsCreate() {
        return this.isCreate;
    }

    public void setIsCreate(Boolean isCreate) {
        this.isCreate = isCreate;
        setValue();
    }

    public Integer getInitMode() {
        return this.initMode;
    }

    public void setInitMode(Integer initMode) {
        this.initMode = initMode;
        setValue();
    }

    public Long getCapacity() {
        return this.capacity;
    }

    public void setCapacity(Long capacity) {
        this.capacity = capacity;
        setValue();
    }

    public Integer getActionAfterFull() {
        return this.actionAfterFull;
    }

    public void setActionAfterFull(Integer actionAfterFull) {
        this.actionAfterFull = actionAfterFull;
        setValue();
    }

    public String getConfig() {
        return this.config;
    }

    public void setConfig(String config) {
        this.config = config;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        byte b;
        super.writeToParcel(out, ignored);
        if (this.mId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.mId.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.dbName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.dbName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dbPath != null) {
            out.writeByte((byte) 1);
            out.writeString(this.dbPath);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dbType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.dbType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.description != null) {
            out.writeByte((byte) 1);
            out.writeString(this.description);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.modelXml != null) {
            out.writeByte((byte) 1);
            out.writeString(this.modelXml);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dataXml != null) {
            out.writeByte((byte) 1);
            out.writeString(this.dataXml);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.isEncrypt != null) {
            out.writeByte((byte) 1);
            if (this.isEncrypt.booleanValue()) {
                b = (byte) 1;
            } else {
                b = (byte) 0;
            }
            out.writeByte(b);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.ownerBusinessId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.ownerBusinessId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.recovery != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.recovery.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.isCreate != null) {
            out.writeByte((byte) 1);
            if (this.isCreate.booleanValue()) {
                b = (byte) 1;
            } else {
                b = (byte) 0;
            }
            out.writeByte(b);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.initMode != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.initMode.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.capacity != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.capacity.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.actionAfterFull != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.actionAfterFull.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.config != null) {
            out.writeByte((byte) 1);
            out.writeString(this.config);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<DBInfo> getHelper() {
        return DBInfoHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.meta.DBInfo";
    }

    public String getDatabaseName() {
        return "dsMeta";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DBInfo { mId: ").append(this.mId);
        sb.append(", dbName: ").append(this.dbName);
        sb.append(", dbPath: ").append(this.dbPath);
        sb.append(", dbType: ").append(this.dbType);
        sb.append(", description: ").append(this.description);
        sb.append(", modelXml: ").append(this.modelXml);
        sb.append(", dataXml: ").append(this.dataXml);
        sb.append(", isEncrypt: ").append(this.isEncrypt);
        sb.append(", ownerBusinessId: ").append(this.ownerBusinessId);
        sb.append(", recovery: ").append(this.recovery);
        sb.append(", isCreate: ").append(this.isCreate);
        sb.append(", initMode: ").append(this.initMode);
        sb.append(", capacity: ").append(this.capacity);
        sb.append(", actionAfterFull: ").append(this.actionAfterFull);
        sb.append(", config: ").append(this.config);
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
        return "0.0.3";
    }

    public int getEntityVersionCode() {
        return 3;
    }
}
