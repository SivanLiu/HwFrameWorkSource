package com.huawei.nearbysdk.softnet;

import android.os.Parcel;
import android.os.Parcelable;

public class NearServiceDesc implements Parcelable {
    public static final Creator<NearServiceDesc> CREATOR = new Creator<NearServiceDesc>() {
        /* class com.huawei.nearbysdk.softnet.NearServiceDesc.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public NearServiceDesc createFromParcel(Parcel in) {
            return new NearServiceDesc(in);
        }

        @Override // android.os.Parcelable.Creator
        public NearServiceDesc[] newArray(int size) {
            return new NearServiceDesc[size];
        }
    };
    private static final String TAG = "Nearby";
    /* access modifiers changed from: private */
    public byte[] mServiceData;
    /* access modifiers changed from: private */
    public String mServiceId;
    /* access modifiers changed from: private */
    public String mServiceName;

    protected NearServiceDesc(Parcel in) {
        this.mServiceId = in.readString();
        this.mServiceName = in.readString();
        this.mServiceData = in.createByteArray();
    }

    private NearServiceDesc() {
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mServiceId);
        dest.writeString(this.mServiceName);
        dest.writeByteArray(this.mServiceData);
    }

    public int describeContents() {
        return 0;
    }

    public String getServiceId() {
        return this.mServiceId;
    }

    public String getServiceName() {
        return this.mServiceName;
    }

    public byte[] getServiceData() {
        return this.mServiceData;
    }

    public static class Builder {
        NearServiceDesc info = new NearServiceDesc();

        public Builder serviceId(String serviceId) {
            String unused = this.info.mServiceId = serviceId;
            return this;
        }

        public Builder serviceName(String serviceName) {
            String unused = this.info.mServiceName = serviceName;
            return this;
        }

        public Builder serviceData(byte[] serviceData) {
            byte[] unused = this.info.mServiceData = serviceData;
            return this;
        }

        public NearServiceDesc build() {
            return this.info;
        }
    }
}
