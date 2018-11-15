package com.huawei.nb.model.coordinator;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class DeviceToken extends AManagedObject {
    public static final Creator<DeviceToken> CREATOR = new Creator<DeviceToken>() {
        public DeviceToken createFromParcel(Parcel in) {
            return new DeviceToken(in);
        }

        public DeviceToken[] newArray(int size) {
            return new DeviceToken[size];
        }
    };
    private Long id;
    private String reportFlag;
    private String reportTime;
    private String token;

    public DeviceToken(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.token = cursor.getString(2);
        this.reportFlag = cursor.getString(3);
        this.reportTime = cursor.getString(4);
    }

    public DeviceToken(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.token = in.readByte() == (byte) 0 ? null : in.readString();
        this.reportFlag = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reportTime = str;
    }

    private DeviceToken(Long id, String token, String reportFlag, String reportTime) {
        this.id = id;
        this.token = token;
        this.reportFlag = reportFlag;
        this.reportTime = reportTime;
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

    public String getToken() {
        return this.token;
    }

    public void setToken(String token) {
        this.token = token;
        setValue();
    }

    public String getReportFlag() {
        return this.reportFlag;
    }

    public void setReportFlag(String reportFlag) {
        this.reportFlag = reportFlag;
        setValue();
    }

    public String getReportTime() {
        return this.reportTime;
    }

    public void setReportTime(String reportTime) {
        this.reportTime = reportTime;
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
        if (this.token != null) {
            out.writeByte((byte) 1);
            out.writeString(this.token);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reportFlag != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reportFlag);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reportTime != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reportTime);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<DeviceToken> getHelper() {
        return DeviceTokenHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.coordinator.DeviceToken";
    }

    public String getDatabaseName() {
        return "dsMeta";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DeviceToken { id: ").append(this.id);
        sb.append(", token: ").append(this.token);
        sb.append(", reportFlag: ").append(this.reportFlag);
        sb.append(", reportTime: ").append(this.reportTime);
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
