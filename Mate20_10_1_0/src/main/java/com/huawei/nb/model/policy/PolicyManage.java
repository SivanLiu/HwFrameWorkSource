package com.huawei.nb.model.policy;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class PolicyManage extends AManagedObject {
    public static final Parcelable.Creator<PolicyManage> CREATOR = new Parcelable.Creator<PolicyManage>() {
        /* class com.huawei.nb.model.policy.PolicyManage.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public PolicyManage createFromParcel(Parcel in) {
            return new PolicyManage(in);
        }

        @Override // android.os.Parcelable.Creator
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

    /* JADX INFO: super call moved to the top of the method (can break code semantics) */
    public PolicyManage(Parcel in) {
        super(in);
        String str = null;
        this.policyName = in.readByte() == 0 ? null : in.readString();
        this.serviceName = in.readByte() == 0 ? null : in.readString();
        this.policyFile = in.readByte() != 0 ? in.readString() : str;
    }

    private PolicyManage(String policyName2, String serviceName2, String policyFile2) {
        this.policyName = policyName2;
        this.serviceName = serviceName2;
        this.policyFile = policyFile2;
    }

    public PolicyManage() {
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int describeContents() {
        return 0;
    }

    public String getPolicyName() {
        return this.policyName;
    }

    public void setPolicyName(String policyName2) {
        this.policyName = policyName2;
        setValue();
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public void setServiceName(String serviceName2) {
        this.serviceName = serviceName2;
        setValue();
    }

    public String getPolicyFile() {
        return this.policyFile;
    }

    public void setPolicyFile(String policyFile2) {
        this.policyFile = policyFile2;
        setValue();
    }

    @Override // com.huawei.odmf.core.AManagedObject
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

    @Override // com.huawei.odmf.core.AManagedObject
    public AEntityHelper<PolicyManage> getHelper() {
        return PolicyManageHelper.getInstance();
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
    public String getEntityName() {
        return "com.huawei.nb.model.policy.PolicyManage";
    }

    @Override // com.huawei.odmf.core.AManagedObject, com.huawei.odmf.core.ManagedObject
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

    @Override // com.huawei.odmf.core.AManagedObject
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override // com.huawei.odmf.core.AManagedObject
    public int hashCode() {
        return super.hashCode();
    }

    public String getDatabaseVersion() {
        return "0.0.12";
    }

    public int getDatabaseVersionCode() {
        return 12;
    }

    public String getEntityVersion() {
        return "0.0.2";
    }

    public int getEntityVersionCode() {
        return 2;
    }
}
