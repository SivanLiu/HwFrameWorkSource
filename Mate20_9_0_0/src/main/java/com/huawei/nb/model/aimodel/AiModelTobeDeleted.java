package com.huawei.nb.model.aimodel;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class AiModelTobeDeleted extends AManagedObject {
    public static final Creator<AiModelTobeDeleted> CREATOR = new Creator<AiModelTobeDeleted>() {
        public AiModelTobeDeleted createFromParcel(Parcel in) {
            return new AiModelTobeDeleted(in);
        }

        public AiModelTobeDeleted[] newArray(int size) {
            return new AiModelTobeDeleted[size];
        }
    };
    private Long aimodel_id;
    private String file_path;
    private Long id;
    private String name;
    private Long time_expired;

    public AiModelTobeDeleted(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.aimodel_id = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        this.name = cursor.getString(3);
        this.file_path = cursor.getString(4);
        if (!cursor.isNull(5)) {
            l = Long.valueOf(cursor.getLong(5));
        }
        this.time_expired = l;
    }

    public AiModelTobeDeleted(Parcel in) {
        Long l = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.aimodel_id = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.name = in.readByte() == (byte) 0 ? null : in.readString();
        this.file_path = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            l = Long.valueOf(in.readLong());
        }
        this.time_expired = l;
    }

    private AiModelTobeDeleted(Long id, Long aimodel_id, String name, String file_path, Long time_expired) {
        this.id = id;
        this.aimodel_id = aimodel_id;
        this.name = name;
        this.file_path = file_path;
        this.time_expired = time_expired;
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

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        setValue();
    }

    public String getFile_path() {
        return this.file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
        setValue();
    }

    public Long getTime_expired() {
        return this.time_expired;
    }

    public void setTime_expired(Long time_expired) {
        this.time_expired = time_expired;
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
        if (this.name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.name);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.file_path != null) {
            out.writeByte((byte) 1);
            out.writeString(this.file_path);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.time_expired != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.time_expired.longValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<AiModelTobeDeleted> getHelper() {
        return AiModelTobeDeletedHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.aimodel.AiModelTobeDeleted";
    }

    public String getDatabaseName() {
        return "dsAiModel";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("AiModelTobeDeleted { id: ").append(this.id);
        sb.append(", aimodel_id: ").append(this.aimodel_id);
        sb.append(", name: ").append(this.name);
        sb.append(", file_path: ").append(this.file_path);
        sb.append(", time_expired: ").append(this.time_expired);
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
