package com.huawei.nb.model.meta;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class VisitStatistic extends AManagedObject {
    public static final Creator<VisitStatistic> CREATOR = new Creator<VisitStatistic>() {
        public VisitStatistic createFromParcel(Parcel in) {
            return new VisitStatistic(in);
        }

        public VisitStatistic[] newArray(int size) {
            return new VisitStatistic[size];
        }
    };
    private Long deleteCount;
    private Integer guestId;
    private Integer hostId;
    private Integer id;
    private Long insertCount;
    private Long queryCount;
    private Long subscribeCount;
    private Integer type;
    private Long updateCount;
    private Long updateTime;
    private Long uploadTime;

    public VisitStatistic(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.guestId = cursor.isNull(2) ? null : Integer.valueOf(cursor.getInt(2));
        this.hostId = cursor.isNull(3) ? null : Integer.valueOf(cursor.getInt(3));
        this.type = cursor.isNull(4) ? null : Integer.valueOf(cursor.getInt(4));
        this.insertCount = cursor.isNull(5) ? null : Long.valueOf(cursor.getLong(5));
        this.updateCount = cursor.isNull(6) ? null : Long.valueOf(cursor.getLong(6));
        this.deleteCount = cursor.isNull(7) ? null : Long.valueOf(cursor.getLong(7));
        this.queryCount = cursor.isNull(8) ? null : Long.valueOf(cursor.getLong(8));
        this.subscribeCount = cursor.isNull(9) ? null : Long.valueOf(cursor.getLong(9));
        this.updateTime = cursor.isNull(10) ? null : Long.valueOf(cursor.getLong(10));
        if (!cursor.isNull(11)) {
            l = Long.valueOf(cursor.getLong(11));
        }
        this.uploadTime = l;
    }

    public VisitStatistic(Parcel in) {
        Long l = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readInt();
        } else {
            this.id = Integer.valueOf(in.readInt());
        }
        this.guestId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.hostId = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.type = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.insertCount = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.updateCount = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.deleteCount = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.queryCount = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.subscribeCount = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.updateTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        if (in.readByte() != (byte) 0) {
            l = Long.valueOf(in.readLong());
        }
        this.uploadTime = l;
    }

    private VisitStatistic(Integer id, Integer guestId, Integer hostId, Integer type, Long insertCount, Long updateCount, Long deleteCount, Long queryCount, Long subscribeCount, Long updateTime, Long uploadTime) {
        this.id = id;
        this.guestId = guestId;
        this.hostId = hostId;
        this.type = type;
        this.insertCount = insertCount;
        this.updateCount = updateCount;
        this.deleteCount = deleteCount;
        this.queryCount = queryCount;
        this.subscribeCount = subscribeCount;
        this.updateTime = updateTime;
        this.uploadTime = uploadTime;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
        setValue();
    }

    public Integer getGuestId() {
        return this.guestId;
    }

    public void setGuestId(Integer guestId) {
        this.guestId = guestId;
        setValue();
    }

    public Integer getHostId() {
        return this.hostId;
    }

    public void setHostId(Integer hostId) {
        this.hostId = hostId;
        setValue();
    }

    public Integer getType() {
        return this.type;
    }

    public void setType(Integer type) {
        this.type = type;
        setValue();
    }

    public Long getInsertCount() {
        return this.insertCount;
    }

    public void setInsertCount(Long insertCount) {
        this.insertCount = insertCount;
        setValue();
    }

    public Long getUpdateCount() {
        return this.updateCount;
    }

    public void setUpdateCount(Long updateCount) {
        this.updateCount = updateCount;
        setValue();
    }

    public Long getDeleteCount() {
        return this.deleteCount;
    }

    public void setDeleteCount(Long deleteCount) {
        this.deleteCount = deleteCount;
        setValue();
    }

    public Long getQueryCount() {
        return this.queryCount;
    }

    public void setQueryCount(Long queryCount) {
        this.queryCount = queryCount;
        setValue();
    }

    public Long getSubscribeCount() {
        return this.subscribeCount;
    }

    public void setSubscribeCount(Long subscribeCount) {
        this.subscribeCount = subscribeCount;
        setValue();
    }

    public Long getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
        setValue();
    }

    public Long getUploadTime() {
        return this.uploadTime;
    }

    public void setUploadTime(Long uploadTime) {
        this.uploadTime = uploadTime;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.id.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.guestId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.guestId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.hostId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.hostId.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.type != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.type.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.insertCount != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.insertCount.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.updateCount != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.updateCount.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.deleteCount != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.deleteCount.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.queryCount != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.queryCount.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.subscribeCount != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.subscribeCount.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.updateTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.updateTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.uploadTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.uploadTime.longValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<VisitStatistic> getHelper() {
        return VisitStatisticHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.meta.VisitStatistic";
    }

    public String getDatabaseName() {
        return "dsMeta";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("VisitStatistic { id: ").append(this.id);
        sb.append(", guestId: ").append(this.guestId);
        sb.append(", hostId: ").append(this.hostId);
        sb.append(", type: ").append(this.type);
        sb.append(", insertCount: ").append(this.insertCount);
        sb.append(", updateCount: ").append(this.updateCount);
        sb.append(", deleteCount: ").append(this.deleteCount);
        sb.append(", queryCount: ").append(this.queryCount);
        sb.append(", subscribeCount: ").append(this.subscribeCount);
        sb.append(", updateTime: ").append(this.updateTime);
        sb.append(", uploadTime: ").append(this.uploadTime);
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
