package com.huawei.nb.model.policy;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.model.AEntityHelper;

public class TranslatedPolicyData extends AManagedObject {
    public static final Creator<TranslatedPolicyData> CREATOR = new Creator<TranslatedPolicyData>() {
        public TranslatedPolicyData createFromParcel(Parcel in) {
            return new TranslatedPolicyData(in);
        }

        public TranslatedPolicyData[] newArray(int size) {
            return new TranslatedPolicyData[size];
        }
    };
    private String policyFile;
    private Integer policyId;
    private String policyName;
    private String serviceName;

    public TranslatedPolicyData(Cursor cursor) {
        setRowId(Long.valueOf(cursor.getLong(0)));
        this.policyId = cursor.isNull(1) ? null : Integer.valueOf(cursor.getInt(1));
        this.policyName = cursor.getString(2);
        this.serviceName = cursor.getString(3);
        this.policyFile = cursor.getString(4);
    }

    public TranslatedPolicyData(Parcel in) {
        String str = null;
        super(in);
        if (in.readByte() == (byte) 0) {
            this.policyId = null;
            in.readInt();
        } else {
            this.policyId = Integer.valueOf(in.readInt());
        }
        this.policyName = in.readByte() == (byte) 0 ? null : in.readString();
        this.serviceName = in.readByte() == (byte) 0 ? null : in.readString();
        if (in.readByte() != (byte) 0) {
            str = in.readString();
        }
        this.policyFile = str;
    }

    private TranslatedPolicyData(Integer policyId, String policyName, String serviceName, String policyFile) {
        this.policyId = policyId;
        this.policyName = policyName;
        this.serviceName = serviceName;
        this.policyFile = policyFile;
    }

    public int describeContents() {
        return 0;
    }

    public Integer getPolicyId() {
        return this.policyId;
    }

    public void setPolicyId(Integer policyId) {
        this.policyId = policyId;
        setValue();
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
        if (this.policyId != null) {
            out.writeByte((byte) 1);
            out.writeInt(this.policyId.intValue());
        } else {
            out.writeByte((byte) 0);
            out.writeInt(1);
        }
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

    public AEntityHelper<TranslatedPolicyData> getHelper() {
        return TranslatedPolicyDataHelper.getInstance();
    }

    public String getEntityName() {
        return "com.huawei.nb.model.policy.TranslatedPolicyData";
    }

    public String getDatabaseName() {
        return "dsServiceMetaData";
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("TranslatedPolicyData { policyId: ").append(this.policyId);
        sb.append(", policyName: ").append(this.policyName);
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
        return "0.0.1";
    }

    public int getEntityVersionCode() {
        return 1;
    }
}
