package com.huawei.nb.model.coordinator;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class PushMsgControl extends AManagedObject {
    public static final Creator<PushMsgControl> CREATOR = new Creator<PushMsgControl>() {
        public PushMsgControl createFromParcel(Parcel in) {
            return new PushMsgControl(in);
        }

        public PushMsgControl[] newArray(int size) {
            return new PushMsgControl[size];
        }
    };
    private Integer count;
    private Long id;
    private Integer maxReportInterval;
    private String msgType;
    private Integer presetCount;
    private String updateTime;

    public PushMsgControl(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.msgType = cursor.getString(2);
        this.presetCount = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.count = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        if (!cursor.isNull(5)) {
            num = Integer.valueOf(cursor.getInt(5));
        }
        this.maxReportInterval = num;
        this.updateTime = cursor.getString(6);
    }

    public PushMsgControl(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.msgType = in.readByte() == (byte) 0 ? null : in.readString();
        this.presetCount = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.count = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.maxReportInterval = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.updateTime = str;
    }

    private PushMsgControl(Long id, String msgType, Integer presetCount, Integer count, Integer maxReportInterval, String updateTime) {
        this.id = id;
        this.msgType = msgType;
        this.presetCount = presetCount;
        this.count = count;
        this.maxReportInterval = maxReportInterval;
        this.updateTime = updateTime;
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

    public String getMsgType() {
        return this.msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
        setValue();
    }

    public Integer getPresetCount() {
        return this.presetCount;
    }

    public void setPresetCount(Integer presetCount) {
        this.presetCount = presetCount;
        setValue();
    }

    public Integer getCount() {
        return this.count;
    }

    public void setCount(Integer count) {
        this.count = count;
        setValue();
    }

    public Integer getMaxReportInterval() {
        return this.maxReportInterval;
    }

    public void setMaxReportInterval(Integer maxReportInterval) {
        this.maxReportInterval = maxReportInterval;
        setValue();
    }

    public String getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
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
        if (this.msgType != null) {
            out.writeByte((byte) 1);
            out.writeString(this.msgType);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.presetCount != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.presetCount.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.count != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.count.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.maxReportInterval != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.maxReportInterval.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.updateTime != null) {
            out.writeByte((byte) 1);
            out.writeString(this.updateTime);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<PushMsgControl> getHelper() {
        return PushMsgControlHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.coordinator.PushMsgControl";
    }

    public String getDatabaseName() {
        return "dsMeta";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("PushMsgControl { id: ").append(this.id);
        sb.append(", msgType: ").append(this.msgType);
        sb.append(", presetCount: ").append(this.presetCount);
        sb.append(", count: ").append(this.count);
        sb.append(", maxReportInterval: ").append(this.maxReportInterval);
        sb.append(", updateTime: ").append(this.updateTime);
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
