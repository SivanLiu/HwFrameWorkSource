package com.huawei.nb.model.policy;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class PolicyManage extends AManagedObject {
    public static final Creator<PolicyManage> CREATOR = new Creator<PolicyManage>() {
        public PolicyManage createFromParcel(Parcel in) {
            return new PolicyManage(in);
        }

        public PolicyManage[] newArray(int size) {
            return new PolicyManage[size];
        }
    };
    private String policyFile;
    private String policyName;
    private String serviceName;

    public PolicyManage(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.policyName = cursor.getString(1);
        this.serviceName = cursor.getString(2);
        this.policyFile = cursor.getString(3);
    }

    public PolicyManage(Parcel in) {
        String str = null;
        super(in);
        this.policyName = in.readByte() == (byte) 0 ? null : in.readString();
        this.serviceName = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.policyFile = str;
    }

    private PolicyManage(String policyName, String serviceName, String policyFile) {
        this.policyName = policyName;
        this.serviceName = serviceName;
        this.policyFile = policyFile;
    }

    public int describeContents() {
        return 0;
    }

    public String getPolicyName() {
        return this.policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
        setValue();
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
        setValue();
    }

    public String getPolicyFile() {
        return this.policyFile;
    }

    public void setPolicyFile(String policyFile) {
        this.policyFile = policyFile;
        setValue();
    }

    public void writeToParcel(Parcel out, int ignored) {
        super.writeToParcel(out, ignored);
        if (this.policyName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.policyName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.serviceName != null) {
            out.writeByte((byte) 1);
            out.writeString(this.serviceName);
        } else {
            out.writeByte((byte) 0);
        }
        if (this.policyFile != null) {
            out.writeByte((byte) 1);
            out.writeString(this.policyFile);
            return;
        }
        out.writeByte((byte) 0);
    }

    public AEntityHelper<PolicyManage> getHelper() {
        return PolicyManageHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.policy.PolicyManage";
    }

    public String getDatabaseName() {
        return "dsServiceMetaData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("PolicyManage { policyName: ").append(this.policyName);
        sb.append(", serviceName: ").append(this.serviceName);
        sb.append(", policyFile: ").append(this.policyFile);
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
