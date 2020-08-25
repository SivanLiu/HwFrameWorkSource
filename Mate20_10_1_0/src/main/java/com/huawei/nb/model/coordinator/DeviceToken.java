package com.huawei.nb.model.coordinator;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class DeviceToken extends AManagedObject {
    public static final Parcelable.Creator<DeviceToken> CREATOR = new Parcelable.Creator<DeviceToken>() {
        /* class com.huawei.nb.model.coordinator.DeviceToken.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public DeviceToken createFromParcel(Parcel in) {
            return new DeviceToken(in);
        }

        @Override // android.os.Parcelable.Creator
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

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public DeviceToken(Parcel in) {
        super(in);
        String str = null;
        if (in.readByte() == 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.token = in.readByte() == 0 ? null : in.readString();
        this.reportFlag = in.readByte() == 0 ? null : in.readString();
        this.reportTime = in.readByte() != 0 ? in.readString() : str;
    }

    private DeviceToken(Long id2, String token2, String reportFlag2, String reportTime2) {
        this.id = id2;
        this.token = token2;
        this.reportFlag = reportFlag2;
        this.reportTime = reportTime2;
    }

    public DeviceToken() {
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int describeContents() {
        return 0;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id2) {
        this.id = id2;
        setValue();
    }

    public String getToken() {
        return this.token;
    }

    public void setToken(String token2) {
        this.token = token2;
        setValue();
    }

    public String getReportFlag() {
        return this.reportFlag;
    }

    public void setReportFlag(String reportFlag2) {
        this.reportFlag = reportFlag2;
        setValue();
    }

    public String getReportTime() {
        return this.reportTime;
    }

    public void setReportTime(String reportTime2) {
        this.reportTime = reportTime2;
        setValue();
    }

    @Override // com.huawei.odmf.core.AManagedObject
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

    @Override // com.huawei.odmf.core.AManagedObject
    public AEntityHelper<DeviceToken> getHelper() {
        return DeviceTokenHelper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.coordinator.DeviceToken";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
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

    @Override // com.huawei.odmf.core.AManagedObject
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.16";
    }

    public int getDatabaseVersionCode() {
        return 16;
    }

    public String getEntityVersion() {
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
