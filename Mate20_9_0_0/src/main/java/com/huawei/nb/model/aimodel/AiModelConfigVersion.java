package com.huawei.nb.model.aimodel;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class AiModelConfigVersion extends AManagedObject {
    public static final Creator<AiModelConfigVersion> CREATOR = new Creator<AiModelConfigVersion>() {
        public AiModelConfigVersion createFromParcel(Parcel in) {
            return new AiModelConfigVersion(in);
        }

        public AiModelConfigVersion[] newArray(int size) {
            return new AiModelConfigVersion[size];
        }
    };
    private Long id;
    private Long type;
    private Long version;

    public AiModelConfigVersion(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.type = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        if (!cursor.isNull(3)) {
            l = Long.valueOf(cursor.getLong(3));
        }
        this.version = l;
    }

    public AiModelConfigVersion(Parcel in) {
        Long l = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.type = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        if (in.readByte() != (byte) 0) {
            l = Long.valueOf(in.readLong());
        }
        this.version = l;
    }

    private AiModelConfigVersion(Long id, Long type, Long version) {
        this.id = id;
        this.type = type;
        this.version = version;
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

    public Long getType() {
        return this.type;
    }

    public void setType(Long type) {
        this.type = type;
        setValue();
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
        this.version = version;
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
        if (this.type != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.type.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.version != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.version.longValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<AiModelConfigVersion> getHelper() {
        return AiModelConfigVersionHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.aimodel.AiModelConfigVersion";
    }

    public String getDatabaseName() {
        return "dsAiModel";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("AiModelConfigVersion { id: ").append(this.id);
        sb.append(", type: ").append(this.type);
        sb.append(", version: ").append(this.version);
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
