package com.huawei.nb.model.rule;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class ConditionGroup extends AManagedObject {
    public static final Creator<ConditionGroup> CREATOR = new Creator<ConditionGroup>() {
        public ConditionGroup createFromParcel(Parcel in) {
            return new ConditionGroup(in);
        }

        public ConditionGroup[] newArray(int size) {
            return new ConditionGroup[size];
        }
    };
    private Integer conditionNumber;
    private Integer conditionType;
    private Long groupId;
    private Long id;
    private Long relationId;
    private Integer relationType;

    public ConditionGroup(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.groupId = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        this.relationId = cursor.isNull(3) ? null : Long.valueOf(cursor.getLong(3));
        this.conditionNumber = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        this.relationType = cursor.isNull(5) ? null : Integer.valueOf(cursor.getInt(5));
        if (!cursor.isNull(6)) {
            num = Integer.valueOf(cursor.getInt(6));
        }
        this.conditionType = num;
    }

    public ConditionGroup(Parcel in) {
        Integer num = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.groupId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.relationId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.conditionNumber = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.relationType = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            num = Integer.valueOf(in.readInt());
        }
        this.conditionType = num;
    }

    private ConditionGroup(Long id, Long groupId, Long relationId, Integer conditionNumber, Integer relationType, Integer conditionType) {
        this.id = id;
        this.groupId = groupId;
        this.relationId = relationId;
        this.conditionNumber = conditionNumber;
        this.relationType = relationType;
        this.conditionType = conditionType;
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

    public Long getGroupId() {
        return this.groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
        setValue();
    }

    public Long getRelationId() {
        return this.relationId;
    }

    public void setRelationId(Long relationId) {
        this.relationId = relationId;
        setValue();
    }

    public Integer getConditionNumber() {
        return this.conditionNumber;
    }

    public void setConditionNumber(Integer conditionNumber) {
        this.conditionNumber = conditionNumber;
        setValue();
    }

    public Integer getRelationType() {
        return this.relationType;
    }

    public void setRelationType(Integer relationType) {
        this.relationType = relationType;
        setValue();
    }

    public Integer getConditionType() {
        return this.conditionType;
    }

    public void setConditionType(Integer conditionType) {
        this.conditionType = conditionType;
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
        if (this.groupId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.groupId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.relationId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.relationId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.conditionNumber != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.conditionNumber.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.relationType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.relationType.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.conditionType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.conditionType.intValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<ConditionGroup> getHelper() {
        return ConditionGroupHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.rule.ConditionGroup";
    }

    public String getDatabaseName() {
        return "dsRule";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ConditionGroup { id: ").append(this.id);
        sb.append(", groupId: ").append(this.groupId);
        sb.append(", relationId: ").append(this.relationId);
        sb.append(", conditionNumber: ").append(this.conditionNumber);
        sb.append(", relationType: ").append(this.relationType);
        sb.append(", conditionType: ").append(this.conditionType);
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
