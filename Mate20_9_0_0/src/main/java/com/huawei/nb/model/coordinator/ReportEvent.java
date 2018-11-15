package com.huawei.nb.model.coordinator;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class ReportEvent extends AManagedObject {
    public static final Creator<ReportEvent> CREATOR = new Creator<ReportEvent>() {
        public ReportEvent createFromParcel(Parcel in) {
            return new ReportEvent(in);
        }

        public ReportEvent[] newArray(int size) {
            return new ReportEvent[size];
        }
    };
    private Integer eventNo;
    private String id;
    private String params;
    private String type;

    public ReportEvent(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.eventNo = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.id = cursor.getString(2);
        this.type = cursor.getString(3);
        this.params = cursor.getString(4);
    }

    public ReportEvent(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.eventNo = null;
            in.readInt();
        } else {
            this.eventNo = Integer.valueOf(in.readInt());
        }
        this.id = in.readByte() == (byte) 0 ? null : in.readString();
        this.type = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.params = str;
    }

    private ReportEvent(Integer eventNo, String id, String type, String params) {
        this.eventNo = eventNo;
        this.id = id;
        this.type = type;
        this.params = params;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getEventNo() {
        return this.eventNo;
    }

    public void setEventNo(Integer eventNo) {
        this.eventNo = eventNo;
        setValue();
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
        setValue();
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
        setValue();
    }

    public String getParams() {
        return this.params;
    }

    public void setParams(String params) {
        this.params = params;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.eventNo != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.eventNo.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeString(this.id);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.type != null) {
            out.writeByte((byte) 1);
            out.writeString(this.type);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.params != null) {
            out.writeByte((byte) 1);
            out.writeString(this.params);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<ReportEvent> getHelper() {
        return ReportEventHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.coordinator.ReportEvent";
    }

    public String getDatabaseName() {
        return "dsServiceMetaData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ReportEvent { eventNo: ").append(this.eventNo);
        sb.append(", id: ").append(this.id);
        sb.append(", type: ").append(this.type);
        sb.append(", params: ").append(this.params);
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
        return "0.0.11";
    }

    public int getDatabaseVersionCode() {
        return 11;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
