package com.huawei.nb.model.policy;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class PolicyRuntimeData extends AManagedObject {
    public static final Creator<PolicyRuntimeData> CREATOR = new Creator<PolicyRuntimeData>() {
        public PolicyRuntimeData createFromParcel(Parcel in) {
            return new PolicyRuntimeData(in);
        }

        public PolicyRuntimeData[] newArray(int size) {
            return new PolicyRuntimeData[size];
        }
    };
    private String category;
    private Long id;
    private String name;
    private String serviceName;
    private Long timeStamp;
    private String value;

    public PolicyRuntimeData(Cursor cursor) {
        Long l = null;
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.id = cursor.isNull(1) ? null : Long.valueOf(cursor.getLong(1));
        this.serviceName = cursor.getString(2);
        this.category = cursor.getString(3);
        this.name = cursor.getString(4);
        this.value = cursor.getString(5);
        if (!cursor.isNull(6)) {
            l = Long.valueOf(cursor.getLong(6));
        }
        this.timeStamp = l;
    }

    public PolicyRuntimeData(Parcel in) {
        Long l = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.id = null;
            in.readLong();
        } else {
            this.id = Long.valueOf(in.readLong());
        }
        this.serviceName = in.readByte() == (byte) 0 ? null : in.readString();
        this.category = in.readByte() == (byte) 0 ? null : in.readString();
        this.name = in.readByte() == (byte) 0 ? null : in.readString();
        this.value = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            l = Long.valueOf(in.readLong());
        }
        this.timeStamp = l;
    }

    private PolicyRuntimeData(Long id, String serviceName, String category, String name, String value, Long timeStamp) {
        this.id = id;
        this.serviceName = serviceName;
        this.category = category;
        this.name = name;
        this.value = value;
        this.timeStamp = timeStamp;
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

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
        setValue();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        setValue();
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
        setValue();
    }

    public Long getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
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
        if (this.serviceName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.serviceName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.category != null) {
            out.writeByte((byte) 1);
            out.writeString(this.category);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.name != null) {
            out.writeByte((byte) 1);
            out.writeString(this.name);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.value != null) {
            out.writeByte((byte) 1);
            out.writeString(this.value);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.timeStamp != null) {
            out.writeByte((byte) 1);
            out.writeLong(this.timeStamp.longValue());
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<PolicyRuntimeData> getHelper() {
        return PolicyRuntimeDataHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.policy.PolicyRuntimeData";
    }

    public String getDatabaseName() {
        return "dsServiceMetaData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("PolicyRuntimeData { id: ").append(this.id);
        sb.append(", serviceName: ").append(this.serviceName);
        sb.append(", category: ").append(this.category);
        sb.append(", name: ").append(this.name);
        sb.append(", value: ").append(this.value);
        sb.append(", timeStamp: ").append(this.timeStamp);
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
        return "0.0.2";
    }

    public int getEntityVersionCode() {
        return 2;
    }
}
