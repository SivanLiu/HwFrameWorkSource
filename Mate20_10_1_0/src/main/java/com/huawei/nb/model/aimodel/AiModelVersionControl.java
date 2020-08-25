package com.huawei.nb.model.aimodel;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class AiModelVersionControl extends AManagedObject {
    public static final Parcelable.Creator<AiModelVersionControl> CREATOR = new Parcelable.Creator<AiModelVersionControl>() {
        /* class com.huawei.nb.model.aimodel.AiModelVersionControl.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public AiModelVersionControl createFromParcel(Parcel in) {
            return new AiModelVersionControl(in);
        }

        @Override // android.os.Parcelable.Creator
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
        this.current_version = !cursor.isNull(3) ? Long.valueOf(cursor.getLong(3)) : l;
    }

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public AiModelVersionControl(Parcel in) {
        super(in);
        Long l = null;
        if (in.readByte() == 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.aimodel_id = in.readByte() == 0 ? null : Long.valueOf(in.readLong());
        this.current_version = in.readByte() != 0 ? Long.valueOf(in.readLong()) : l;
    }

    private AiModelVersionControl(Long id2, Long aimodel_id2, Long current_version2) {
        this.id = id2;
        this.aimodel_id = aimodel_id2;
        this.current_version = current_version2;
    }

    public AiModelVersionControl() {
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int describeContents() {
        return 0;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id2) {
        this.id = id2;
        setValue();
    }

    public Long getAimodel_id() {
        return this.aimodel_id;
    }

    public void setAimodel_id(Long aimodel_id2) {
        this.aimodel_id = aimodel_id2;
        setValue();
    }

    public Long getCurrent_version() {
        return this.current_version;
    }

    public void setCurrent_version(Long current_version2) {
        this.current_version = current_version2;
        setValue();
    }

    @Override // com.huawei.odmf.core.AManagedObject
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

    @Override // com.huawei.odmf.core.AManagedObject
    public AEntityHelper<AiModelVersionControl> getHelper() {
        return AiModelVersionControlHelper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.aimodel.AiModelVersionControl";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
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

    @Override // com.huawei.odmf.core.AManagedObject
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override // com.huawei.odmf.core.AManagedObject
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
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
