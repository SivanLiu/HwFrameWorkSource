package com.huawei.nb.model.aimodel;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class AiModelVersionControl extends AManagedObject {
    public static final Creator<AiModelVersionControl> CREATOR = new Creator<AiModelVersionControl>() {
        public AiModelVersionControl createFromParcel(Parcel in) {
            return new AiModelVersionControl(in);
        }

        public AiModelVersionControl[] newArray(int size) {
            return new AiModelVersionControl[size];
        }
    };
    private Long aimodel_id;
    private Long current_version;
    private Long id;

    public AiModelVersionControl(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.aimodel_id = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        if (!cursor.isNull(3)) {
            l = Long.valueOf(cursor.getLong(3));
        }
        this.current_version = l;
    }

    public AiModelVersionControl(Parcel in) {
        Long l = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.aimodel_id = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        if (in.readByte() != (byte) 0) {
            l = Long.valueOf(in.readLong());
        }
        this.current_version = l;
    }

    private AiModelVersionControl(Long id, Long aimodel_id, Long current_version) {
        this.id = id;
        this.aimodel_id = aimodel_id;
        this.current_version = current_version;
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

    public Long getCurrent_version() {
        return this.current_version;
    }

    public void setCurrent_version(Long current_version) {
        this.current_version = current_version;
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
        if (this.current_version != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.current_version.longValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<AiModelVersionControl> getHelper() {
        return AiModelVersionControlHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.aimodel.AiModelVersionControl";
    }

    public String getDatabaseName() {
        return "dsAiModel";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("AiModelVersionControl { id: ").append(this.id);
        sb.append(", aimodel_id: ").append(this.aimodel_id);
        sb.append(", current_version: ").append(this.current_version);
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
