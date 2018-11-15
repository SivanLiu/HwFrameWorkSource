package com.huawei.nb.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class CoordinatorPolicy extends AManagedObject {
    public static final Creator<CoordinatorPolicy> CREATOR = new Creator<CoordinatorPolicy>() {
        public CoordinatorPolicy createFromParcel(Parcel in) {
            return new CoordinatorPolicy(in);
        }

        public CoordinatorPolicy[] newArray(int size) {
            return new CoordinatorPolicy[size];
        }
    };
    private Long dataTrafficSyncTime;
    private Long dataType;
    private String dbName;
    private Integer electricity;
    private Long isAllowOverWrite;
    private Long networkMode;
    private Integer policyNo;
    private String remoteUrl;
    private String startTime;
    private String syncField;
    private Long syncMode;
    private Integer syncPeriod;
    private Long syncPoint;
    private Long syncTime;
    private String tableName;
    private Integer tilingTime;

    public CoordinatorPolicy(Cursor cursor) {
        Integer num = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.policyNo = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.tableName = cursor.getString(2);
        this.dbName = cursor.getString(3);
        this.syncMode = cursor.isNull(4) ? null : Long.valueOf(cursor.getLong(4));
        this.syncTime = cursor.isNull(5) ? null : Long.valueOf(cursor.getLong(5));
        this.syncPoint = cursor.isNull(6) ? null : Long.valueOf(cursor.getLong(6));
        this.remoteUrl = cursor.getString(7);
        this.dataType = cursor.isNull(8) ? null : Long.valueOf(cursor.getLong(8));
        this.isAllowOverWrite = cursor.isNull(9) ? null : Long.valueOf(cursor.getLong(9));
        this.networkMode = cursor.isNull(10) ? null : Long.valueOf(cursor.getLong(10));
        this.syncField = cursor.getString(11);
        this.startTime = cursor.getString(12);
        this.electricity = cursor.isNull(13) ? null : Integer.valueOf(cursor.getInt(13));
        this.tilingTime = cursor.isNull(14) ? null : Integer.valueOf(cursor.getInt(14));
        this.dataTrafficSyncTime = cursor.isNull(15) ? null : Long.valueOf(cursor.getLong(15));
        if (!cursor.isNull(16)) {
            num = Integer.valueOf(cursor.getInt(16));
        }
        this.syncPeriod = num;
    }

    public CoordinatorPolicy(Parcel in) {
        Integer num = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.policyNo = null;
            in.readInt();
        } else {
            this.policyNo = Integer.valueOf(in.readInt());
        }
        this.tableName = in.readByte() == (byte) 0 ? null : in.readString();
        this.dbName = in.readByte() == (byte) 0 ? null : in.readString();
        this.syncMode = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.syncTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.syncPoint = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.remoteUrl = in.readByte() == (byte) 0 ? null : in.readString();
        this.dataType = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.isAllowOverWrite = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.networkMode = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        this.syncField = in.readByte() == (byte) 0 ? null : in.readString();
        this.startTime = in.readByte() == (byte) 0 ? null : in.readString();
        this.electricity = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.tilingTime = in.readByte() == (byte) 0 ? null : Integer.valueOf(in.readInt());
        this.dataTrafficSyncTime = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        if (in.readByte() != (byte) 0) {
            num = Integer.valueOf(in.readInt());
        }
        this.syncPeriod = num;
    }

    private CoordinatorPolicy(Integer policyNo, String tableName, String dbName, Long syncMode, Long syncTime, Long syncPoint, String remoteUrl, Long dataType, Long isAllowOverWrite, Long networkMode, String syncField, String startTime, Integer electricity, Integer tilingTime, Long dataTrafficSyncTime, Integer syncPeriod) {
        this.policyNo = policyNo;
        this.tableName = tableName;
        this.dbName = dbName;
        this.syncMode = syncMode;
        this.syncTime = syncTime;
        this.syncPoint = syncPoint;
        this.remoteUrl = remoteUrl;
        this.dataType = dataType;
        this.isAllowOverWrite = isAllowOverWrite;
        this.networkMode = networkMode;
        this.syncField = syncField;
        this.startTime = startTime;
        this.electricity = electricity;
        this.tilingTime = tilingTime;
        this.dataTrafficSyncTime = dataTrafficSyncTime;
        this.syncPeriod = syncPeriod;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getPolicyNo() {
        return this.policyNo;
    }

    public void setPolicyNo(Integer policyNo) {
        this.policyNo = policyNo;
        setValue();
    }

    public String getTableName() {
        return this.tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
        setValue();
    }

    public String getDbName() {
        return this.dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
        setValue();
    }

    public Long getSyncMode() {
        return this.syncMode;
    }

    public void setSyncMode(Long syncMode) {
        this.syncMode = syncMode;
        setValue();
    }

    public Long getSyncTime() {
        return this.syncTime;
    }

    public void setSyncTime(Long syncTime) {
        this.syncTime = syncTime;
        setValue();
    }

    public Long getSyncPoint() {
        return this.syncPoint;
    }

    public void setSyncPoint(Long syncPoint) {
        this.syncPoint = syncPoint;
        setValue();
    }

    public String getRemoteUrl() {
        return this.remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
        setValue();
    }

    public Long getDataType() {
        return this.dataType;
    }

    public void setDataType(Long dataType) {
        this.dataType = dataType;
        setValue();
    }

    public Long getIsAllowOverWrite() {
        return this.isAllowOverWrite;
    }

    public void setIsAllowOverWrite(Long isAllowOverWrite) {
        this.isAllowOverWrite = isAllowOverWrite;
        setValue();
    }

    public Long getNetworkMode() {
        return this.networkMode;
    }

    public void setNetworkMode(Long networkMode) {
        this.networkMode = networkMode;
        setValue();
    }

    public String getSyncField() {
        return this.syncField;
    }

    public void setSyncField(String syncField) {
        this.syncField = syncField;
        setValue();
    }

    public String getStartTime() {
        return this.startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
        setValue();
    }

    public Integer getElectricity() {
        return this.electricity;
    }

    public void setElectricity(Integer electricity) {
        this.electricity = electricity;
        setValue();
    }

    public Integer getTilingTime() {
        return this.tilingTime;
    }

    public void setTilingTime(Integer tilingTime) {
        this.tilingTime = tilingTime;
        setValue();
    }

    public Long getDataTrafficSyncTime() {
        return this.dataTrafficSyncTime;
    }

    public void setDataTrafficSyncTime(Long dataTrafficSyncTime) {
        this.dataTrafficSyncTime = dataTrafficSyncTime;
        setValue();
    }

    public Integer getSyncPeriod() {
        return this.syncPeriod;
    }

    public void setSyncPeriod(Integer syncPeriod) {
        this.syncPeriod = syncPeriod;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.policyNo != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.policyNo.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
        if (this.tableName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.tableName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dbName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.dbName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.syncMode != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.syncMode.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.syncTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.syncTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.syncPoint != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.syncPoint.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.remoteUrl != null) {
            out.writeByte((byte) 1);
            out.writeString(this.remoteUrl);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dataType != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.dataType.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.isAllowOverWrite != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.isAllowOverWrite.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.networkMode != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.networkMode.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.syncField != null) {
            out.writeByte((byte) 1);
            out.writeString(this.syncField);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.startTime != null) {
            out.writeByte((byte) 1);
            out.writeString(this.startTime);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.electricity != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.electricity.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.tilingTime != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.tilingTime.intValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.dataTrafficSyncTime != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.dataTrafficSyncTime.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.syncPeriod != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.syncPeriod.intValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<CoordinatorPolicy> getHelper() {
        return CoordinatorPolicyHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.CoordinatorPolicy";
    }

    public String getDatabaseName() {
        return "dsMeta";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("CoordinatorPolicy { policyNo: ").append(this.policyNo);
        sb.append(", tableName: ").append(this.tableName);
        sb.append(", dbName: ").append(this.dbName);
        sb.append(", syncMode: ").append(this.syncMode);
        sb.append(", syncTime: ").append(this.syncTime);
        sb.append(", syncPoint: ").append(this.syncPoint);
        sb.append(", remoteUrl: ").append(this.remoteUrl);
        sb.append(", dataType: ").append(this.dataType);
        sb.append(", isAllowOverWrite: ").append(this.isAllowOverWrite);
        sb.append(", networkMode: ").append(this.networkMode);
        sb.append(", syncField: ").append(this.syncField);
        sb.append(", startTime: ").append(this.startTime);
        sb.append(", electricity: ").append(this.electricity);
        sb.append(", tilingTime: ").append(this.tilingTime);
        sb.append(", dataTrafficSyncTime: ").append(this.dataTrafficSyncTime);
        sb.append(", syncPeriod: ").append(this.syncPeriod);
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
        return "0.0.2";
    }

    public int getEntityVersionCode() {
        return 2;
    }
}
