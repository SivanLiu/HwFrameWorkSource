package com.huawei.nb.model.rule;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;
import java.util.Date;

public class Event extends AManagedObject {
    public static final Creator<Event> CREATOR = new Creator<Event>() {
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        public Event[] newArray(int size) {
            return new Event[size];
        }
    };
    private Integer continuity;
    private Integer count;
    private Long id;
    private Long itemId;
    private Date lastTime;
    private String name;
    private Long operatorId;
    private Date time;
    private Long timeout;
    private Integer totalCount;
    private String value;

    public Event(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.name = cursor.getString(2);
        this.operatorId = cursor.isNull(3) ? null : Long.valueOf(cursor.getLong(3));
        this.itemId = cursor.isNull(4) ? null : Long.valueOf(cursor.getLong(4));
        this.value = cursor.getString(5);
        this.timeout = cursor.isNull(6) ? null : Long.valueOf(cursor.getLong(6));
        this.continuity = cursor.isNull(7) ? null : Integer.valueOf(cursor.getInt(7));
        this.time = cursor.isNull(8) ? null : new Date(cursor.getLong(8));
        this.lastTime = cursor.isNull(9) ? null : new Date(cursor.getLong(9));
        this.count = cursor.isNull(10) ? null : Integer.valueOf(cursor.getInt(10));
        if (!cursor.isNull(11)) {
            num = Integer.valueOf(cursor.getInt(11));
        }
        this.totalCount = num;
    }

    public Event(Parcel in) {
        Integer num = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.name = in.readByte() == (byte) 0 ? null : in.readString();
        this.operatorId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.itemId = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.value = in.readByte() == (byte) 0 ? null : in.readString();
        this.timeout = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.continuity = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.time = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.lastTime = in.readByte() == (byte) 0 ? null : new Date(in.readLong());
        this.count = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            num = Integer.valueOf(in.readInt());
        }
        this.totalCount = num;
    }

    private Event(Long id, String name, Long operatorId, Long itemId, String value, Long timeout, Integer continuity, Date time, Date lastTime, Integer count, Integer totalCount) {
        this.id = id;
        this.name = name;
        this.operatorId = operatorId;
        this.itemId = itemId;
        this.value = value;
        this.timeout = timeout;
        this.continuity = continuity;
        this.time = time;
        this.lastTime = lastTime;
        this.count = count;
        this.totalCount = totalCount;
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

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
        setValue();
    }

    public Long getTimeout() {
        return this.timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
        setValue();
    }

    public Integer getContinuity() {
        return this.continuity;
    }

    public void setContinuity(Integer continuity) {
        this.continuity = continuity;
        setValue();
    }

    public Date getTime() {
        return this.time;
    }

    public void setTime(Date time) {
        this.time = time;
        setValue();
    }

    public Date getLastTime() {
        return this.lastTime;
    }

    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
        setValue();
    }

    public Integer getCount() {
        return this.count;
    }

    public void setCount(Integer count) {
        this.count = count;
        setValue();
    }

    public Integer getTotalCount() {
        return this.totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
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
        if (this.name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.name);
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
        if (this.value != null) {
            out.writeByte((byte) 1);
            out.writeString(this.value);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.timeout != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.timeout.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.continuity != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.continuity.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.time != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.time.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.lastTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.lastTime.getTime());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.count != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.count.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.totalCount != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.totalCount.intValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<Event> getHelper() {
        return EventHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.rule.Event";
    }

    public String getDatabaseName() {
        return "dsRule";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Event { id: ").append(this.id);
        sb.append(", name: ").append(this.name);
        sb.append(", operatorId: ").append(this.operatorId);
        sb.append(", itemId: ").append(this.itemId);
        sb.append(", value: ").append(this.value);
        sb.append(", timeout: ").append(this.timeout);
        sb.append(", continuity: ").append(this.continuity);
        sb.append(", time: ").append(this.time);
        sb.append(", lastTime: ").append(this.lastTime);
        sb.append(", count: ").append(this.count);
        sb.append(", totalCount: ").append(this.totalCount);
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
