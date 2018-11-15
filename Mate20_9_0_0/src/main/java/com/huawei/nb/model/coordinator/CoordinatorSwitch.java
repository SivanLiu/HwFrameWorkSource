package com.huawei.nb.model.coordinator;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class CoordinatorSwitch extends AManagedObject {
    public static final Creator<CoordinatorSwitch> CREATOR = new Creator<CoordinatorSwitch>() {
        public CoordinatorSwitch createFromParcel(Parcel in) {
            return new CoordinatorSwitch(in);
        }

        public CoordinatorSwitch[] newArray(int size) {
            return new CoordinatorSwitch[size];
        }
    };
    private boolean canUseFlowData;
    private double currentFlowData;
    private Long id;
    private boolean isAutoUpdate;
    private boolean isSwitchOn;
    private Long latestTimestamp;
    private double maxFlowData;
    private String packageName;
    private String reserve1;
    private String reserve2;
    private String serviceName;

    public CoordinatorSwitch(Cursor cursor) {
        boolean z;
        Long l = null;
        boolean z2 = true;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.serviceName = cursor.getString(2);
        this.packageName = cursor.getString(3);
        if (cursor.getInt(4) != 0) {
            z = true;
        } else {
            z = false;
        }
        this.isSwitchOn = z;
        if (cursor.getInt(5) != 0) {
            z = true;
        } else {
            z = false;
        }
        this.isAutoUpdate = z;
        if (!cursor.isNull(6)) {
            l = Long.valueOf(cursor.getLong(6));
        }
        this.latestTimestamp = l;
        if (cursor.getInt(7) == 0) {
            z2 = false;
        }
        this.canUseFlowData = z2;
        this.currentFlowData = cursor.getDouble(8);
        this.maxFlowData = cursor.getDouble(9);
        this.reserve1 = cursor.getString(10);
        this.reserve2 = cursor.getString(11);
    }

    public CoordinatorSwitch(Parcel in) {
        boolean z;
        boolean z2 = true;
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.serviceName = in.readByte() == (byte) 0 ? null : in.readString();
        this.packageName = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            z = true;
        } else {
            z = false;
        }
        this.isSwitchOn = z;
        if (in.readByte() != (byte) 0) {
            z = true;
        } else {
            z = false;
        }
        this.isAutoUpdate = z;
        this.latestTimestamp = in.readByte() == (byte) 0 ? null : Long.valueOf(in.readLong());
        if (in.readByte() == (byte) 0) {
            z2 = false;
        }
        this.canUseFlowData = z2;
        this.currentFlowData = in.readDouble();
        this.maxFlowData = in.readDouble();
        this.reserve1 = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.reserve2 = str;
    }

    private CoordinatorSwitch(Long id, String serviceName, String packageName, boolean isSwitchOn, boolean isAutoUpdate, Long latestTimestamp, boolean canUseFlowData, double currentFlowData, double maxFlowData, String reserve1, String reserve2) {
        this.id = id;
        this.serviceName = serviceName;
        this.packageName = packageName;
        this.isSwitchOn = isSwitchOn;
        this.isAutoUpdate = isAutoUpdate;
        this.latestTimestamp = latestTimestamp;
        this.canUseFlowData = canUseFlowData;
        this.currentFlowData = currentFlowData;
        this.maxFlowData = maxFlowData;
        this.reserve1 = reserve1;
        this.reserve2 = reserve2;
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

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
        setValue();
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
        setValue();
    }

    public boolean getIsSwitchOn() {
        return this.isSwitchOn;
    }

    public void setIsSwitchOn(boolean isSwitchOn) {
        this.isSwitchOn = isSwitchOn;
        setValue();
    }

    public boolean getIsAutoUpdate() {
        return this.isAutoUpdate;
    }

    public void setIsAutoUpdate(boolean isAutoUpdate) {
        this.isAutoUpdate = isAutoUpdate;
        setValue();
    }

    public Long getLatestTimestamp() {
        return this.latestTimestamp;
    }

    public void setLatestTimestamp(Long latestTimestamp) {
        this.latestTimestamp = latestTimestamp;
        setValue();
    }

    public boolean getCanUseFlowData() {
        return this.canUseFlowData;
    }

    public void setCanUseFlowData(boolean canUseFlowData) {
        this.canUseFlowData = canUseFlowData;
        setValue();
    }

    public double getCurrentFlowData() {
        return this.currentFlowData;
    }

    public void setCurrentFlowData(double currentFlowData) {
        this.currentFlowData = currentFlowData;
        setValue();
    }

    public double getMaxFlowData() {
        return this.maxFlowData;
    }

    public void setMaxFlowData(double maxFlowData) {
        this.maxFlowData = maxFlowData;
        setValue();
    }

    public String getReserve1() {
        return this.reserve1;
    }

    public void setReserve1(String reserve1) {
        this.reserve1 = reserve1;
        setValue();
    }

    public String getReserve2() {
        return this.reserve2;
    }

    public void setReserve2(String reserve2) {
        this.reserve2 = reserve2;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        byte b;
        super.writeToParcel(out, ignored);
        if (this.id != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.id.longValue());
        } else {
            out.writeByte((byte) 0);
            out.writeLong(1);
        }
        if (this.serviceName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.serviceName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.packageName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.packageName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.isSwitchOn) {
            b = (byte) 1;
        } else {
            b = (byte) 0;
        }
        out.writeByte(b);
        if (this.isAutoUpdate) {
            b = (byte) 1;
        } else {
            b = (byte) 0;
        }
        out.writeByte(b);
        if (this.latestTimestamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.latestTimestamp.longValue());
        } else {
            out.writeByte((byte) 0);
        }
        if (this.canUseFlowData) {
            b = (byte) 1;
        } else {
            b = (byte) 0;
        }
        out.writeByte(b);
        out.writeDouble(this.currentFlowData);
        out.writeDouble(this.maxFlowData);
        if (this.reserve1 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserve1);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.reserve2 != null) {
            out.writeByte((byte) 1);
            out.writeString(this.reserve2);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<CoordinatorSwitch> getHelper() {
        return CoordinatorSwitchHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.coordinator.CoordinatorSwitch";
    }

    public String getDatabaseName() {
        return "dsMeta";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("CoordinatorSwitch { id: ").append(this.id);
        sb.append(", serviceName: ").append(this.serviceName);
        sb.append(", packageName: ").append(this.packageName);
        sb.append(", isSwitchOn: ").append(this.isSwitchOn);
        sb.append(", isAutoUpdate: ").append(this.isAutoUpdate);
        sb.append(", latestTimestamp: ").append(this.latestTimestamp);
        sb.append(", canUseFlowData: ").append(this.canUseFlowData);
        sb.append(", currentFlowData: ").append(this.currentFlowData);
        sb.append(", maxFlowData: ").append(this.maxFlowData);
        sb.append(", reserve1: ").append(this.reserve1);
        sb.append(", reserve2: ").append(this.reserve2);
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
