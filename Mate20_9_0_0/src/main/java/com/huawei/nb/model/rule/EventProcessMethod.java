package com.huawei.nb.model.rule;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class EventProcessMethod extends AManagedObject {
    public static final Creator<EventProcessMethod> CREATOR = new Creator<EventProcessMethod>() {
        public EventProcessMethod createFromParcel(Parcel in) {
            return new EventProcessMethod(in);
        }

        public EventProcessMethod[] newArray(int size) {
            return new EventProcessMethod[size];
        }
    };
    private String condition;
    private Long eventId;
    private Long id;
    private Long itemId;
    private String method;
    private String methodArgs;
    private Long operatorId;
    private Long ruleId;
    private Integer seqId;

    public EventProcessMethod(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.eventId = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        this.operatorId = cursor.isNull(3) ? null : Long.valueOf(cursor.getLong(3));
        this.itemId = cursor.isNull(4) ? null : Long.valueOf(cursor.getLong(4));
        this.ruleId = cursor.isNull(5) ? null : Long.valueOf(cursor.getLong(5));
        this.method = cursor.getString(6);
        this.methodArgs = cursor.getString(7);
        this.condition = cursor.getString(8);
        if (!cursor.isNull(9)) {
            num = Integer.valueOf(cursor.getInt(9));
        }
        this.seqId = num;
    }

    public EventProcessMethod(Parcel in) {
        Integer num = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.eventId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.operatorId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.itemId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.ruleId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.method = in.readByte() == (byte) 0 ? null : in.readString();
        this.methodArgs = in.readByte() == (byte) 0 ? null : in.readString();
        this.condition = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            num = Integer.valueOf(in.readInt());
        }
        this.seqId = num;
    }

    private EventProcessMethod(Long id, Long eventId, Long operatorId, Long itemId, Long ruleId, String method, String methodArgs, String condition, Integer seqId) {
        this.id = id;
        this.eventId = eventId;
        this.operatorId = operatorId;
        this.itemId = itemId;
        this.ruleId = ruleId;
        this.method = method;
        this.methodArgs = methodArgs;
        this.condition = condition;
        this.seqId = seqId;
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

    public Long getEventId() {
        return this.eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
        setValue();
    }

    public Long getOperatorId() {
        return this.operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
        setValue();
    }

    public Long getItemId() {
        return this.itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
        setValue();
    }

    public Long getRuleId() {
        return this.ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
        setValue();
    }

    public String getMethod() {
        return this.method;
    }

    public void setMethod(String method) {
        this.method = method;
        setValue();
    }

    public String getMethodArgs() {
        return this.methodArgs;
    }

    public void setMethodArgs(String methodArgs) {
        this.methodArgs = methodArgs;
        setValue();
    }

    public String getCondition() {
        return this.condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
        setValue();
    }

    public Integer getSeqId() {
        return this.seqId;
    }

    public void setSeqId(Integer seqId) {
        this.seqId = seqId;
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
        if (this.eventId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.eventId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.operatorId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.operatorId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.itemId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.itemId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.ruleId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.ruleId.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.method != null) {
            out.writeByte((byte) 1);
            out.writeString(this.method);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.methodArgs != null) {
            out.writeByte((byte) 1);
            out.writeString(this.methodArgs);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.condition != null) {
            out.writeByte((byte) 1);
            out.writeString(this.condition);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.seqId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.seqId.intValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<EventProcessMethod> getHelper() {
        return EventProcessMethodHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.rule.EventProcessMethod";
    }

    public String getDatabaseName() {
        return "dsRule";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("EventProcessMethod { id: ").append(this.id);
        sb.append(", eventId: ").append(this.eventId);
        sb.append(", operatorId: ").append(this.operatorId);
        sb.append(", itemId: ").append(this.itemId);
        sb.append(", ruleId: ").append(this.ruleId);
        sb.append(", method: ").append(this.method);
        sb.append(", methodArgs: ").append(this.methodArgs);
        sb.append(", condition: ").append(this.condition);
        sb.append(", seqId: ").append(this.seqId);
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
