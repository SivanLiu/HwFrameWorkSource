package com.huawei.nb.model.rule;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class ActionGroup extends AManagedObject {
    public static final Creator<ActionGroup> CREATOR = new Creator<ActionGroup>() {
        public ActionGroup createFromParcel(Parcel in) {
            return new ActionGroup(in);
        }

        public ActionGroup[] newArray(int size) {
            return new ActionGroup[size];
        }
    };
    private Integer actionNumber;
    private Long id;
    private Integer relationType;
    private Long ruleId;

    public ActionGroup(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.ruleId = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        this.actionNumber = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        if (!cursor.isNull(4)) {
            num = Integer.valueOf(cursor.getInt(4));
        }
        this.relationType = num;
    }

    public ActionGroup(Parcel in) {
        Integer num = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.ruleId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.actionNumber = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            num = Integer.valueOf(in.readInt());
        }
        this.relationType = num;
    }

    private ActionGroup(Long id, Long ruleId, Integer actionNumber, Integer relationType) {
        this.id = id;
        this.ruleId = ruleId;
        this.actionNumber = actionNumber;
        this.relationType = relationType;
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

    public Long getRuleId() {
        return this.ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
        setValue();
    }

    public Integer getActionNumber() {
        return this.actionNumber;
    }

    public void setActionNumber(Integer actionNumber) {
        this.actionNumber = actionNumber;
        setValue();
    }

    public Integer getRelationType() {
        return this.relationType;
    }

    public void setRelationType(Integer relationType) {
        this.relationType = relationType;
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
        if (this.ruleId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.ruleId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.actionNumber != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.actionNumber.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.relationType != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.relationType.intValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<ActionGroup> getHelper() {
        return ActionGroupHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.rule.ActionGroup";
    }

    public String getDatabaseName() {
        return "dsRule";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ActionGroup { id: ").append(this.id);
        sb.append(", ruleId: ").append(this.ruleId);
        sb.append(", actionNumber: ").append(this.actionNumber);
        sb.append(", relationType: ").append(this.relationType);
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
