package com.huawei.nb.model.rule;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class EventTimer extends AManagedObject {
    public static final Creator<EventTimer> CREATOR = new Creator<EventTimer>() {
        public EventTimer createFromParcel(Parcel in) {
            return new EventTimer(in);
        }

        public EventTimer[] newArray(int size) {
            return new EventTimer[size];
        }
    };
    private Long id;
    private Long length;
    private Long ruleId;
    private Integer switchOn;

    public EventTimer(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.length = cursor.isNull(2) ? null : Long.valueOf(cursor.getLong(2));
        this.switchOn = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        if (!cursor.isNull(4)) {
            l = Long.valueOf(cursor.getLong(4));
        }
        this.ruleId = l;
    }

    public EventTimer(Parcel in) {
        Long l = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.length = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.switchOn = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            l = Long.valueOf(in.readLong());
        }
        this.ruleId = l;
    }

    private EventTimer(Long id, Long length, Integer switchOn, Long ruleId) {
        this.id = id;
        this.length = length;
        this.switchOn = switchOn;
        this.ruleId = ruleId;
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

    public Long getLength() {
        return this.length;
    }

    public void setLength(Long length) {
        this.length = length;
        setValue();
    }

    public Integer getSwitchOn() {
        return this.switchOn;
    }

    public void setSwitchOn(Integer switchOn) {
        this.switchOn = switchOn;
        setValue();
    }

    public Long getRuleId() {
        return this.ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
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
        if (this.length != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.length.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.switchOn != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.switchOn.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.ruleId != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.ruleId.longValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<EventTimer> getHelper() {
        return EventTimerHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.rule.EventTimer";
    }

    public String getDatabaseName() {
        return "dsRule";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("EventTimer { id: ").append(this.id);
        sb.append(", length: ").append(this.length);
        sb.append(", switchOn: ").append(this.switchOn);
        sb.append(", ruleId: ").append(this.ruleId);
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
