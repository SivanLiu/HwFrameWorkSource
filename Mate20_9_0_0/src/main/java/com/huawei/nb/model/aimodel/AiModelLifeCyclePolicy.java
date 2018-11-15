package com.huawei.nb.model.aimodel;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class AiModelLifeCyclePolicy extends AManagedObject {
    public static final Creator<AiModelLifeCyclePolicy> CREATOR = new Creator<AiModelLifeCyclePolicy>() {
        public AiModelLifeCyclePolicy createFromParcel(Parcel in) {
            return new AiModelLifeCyclePolicy(in);
        }

        public AiModelLifeCyclePolicy[] newArray(int size) {
            return new AiModelLifeCyclePolicy[size];
        }
    };
    private Long aimodel_id;
    private String delete_policy;
    private Long id;
    private String reserved_1;
    private String update_policy;

    public AiModelLifeCyclePolicy(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        if (!cursor.isNull(2)) {
            l = Long.valueOf(cursor.getLong(2));
        }
        this.aimodel_id = l;
        this.delete_policy = cursor.getString(3);
        this.update_policy = cursor.getString(4);
        this.reserved_1 = cursor.getString(5);
    }

    public AiModelLifeCyclePolicy(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.aimodel_id = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.delete_policy = in.readByte() == (byte) 0 ? null : in.readString();
        this.update_policy = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reserved_1 = str;
    }

    private AiModelLifeCyclePolicy(Long id, Long aimodel_id, String delete_policy, String update_policy, String reserved_1) {
        this.id = id;
        this.aimodel_id = aimodel_id;
        this.delete_policy = delete_policy;
        this.update_policy = update_policy;
        this.reserved_1 = reserved_1;
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

    public String getDelete_policy() {
        return this.delete_policy;
    }

    public void setDelete_policy(String delete_policy) {
        this.delete_policy = delete_policy;
        setValue();
    }

    public String getUpdate_policy() {
        return this.update_policy;
    }

    public void setUpdate_policy(String update_policy) {
        this.update_policy = update_policy;
        setValue();
    }

    public String getReserved_1() {
        return this.reserved_1;
    }

    public void setReserved_1(String reserved_1) {
        this.reserved_1 = reserved_1;
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
        if (this.delete_policy != null) {
            out.writeByte((byte) 1);
            out.writeString(this.delete_policy);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.update_policy != null) {
            out.writeByte((byte) 1);
            out.writeString(this.update_policy);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserved_1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserved_1);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<AiModelLifeCyclePolicy> getHelper() {
        return AiModelLifeCyclePolicyHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.aimodel.AiModelLifeCyclePolicy";
    }

    public String getDatabaseName() {
        return "dsAiModel";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("AiModelLifeCyclePolicy { id: ").append(this.id);
        sb.append(", aimodel_id: ").append(this.aimodel_id);
        sb.append(", delete_policy: ").append(this.delete_policy);
        sb.append(", update_policy: ").append(this.update_policy);
        sb.append(", reserved_1: ").append(this.reserved_1);
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
