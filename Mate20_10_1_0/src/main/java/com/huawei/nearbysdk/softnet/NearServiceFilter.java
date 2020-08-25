package com.huawei.nearbysdk.softnet;

import android.os.Parcel;
import android.os.Parcelable;

public class NearServiceFilter implements Parcelable {
    public static final Creator<NearServiceFilter> CREATOR = new Creator<NearServiceFilter>() {
        /* class com.huawei.nearbysdk.softnet.NearServiceFilter.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public NearServiceFilter createFromParcel(Parcel in) {
            return new NearServiceFilter(in);
        }

        @Override // android.os.Parcelable.Creator
        public NearServiceFilter[] newArray(int size) {
            return new NearServiceFilter[size];
        }
    };
    /* access modifiers changed from: private */
    public byte[] mFilterData;
    /* access modifiers changed from: private */
    public byte[] mFilterMask;
    /* access modifiers changed from: private */
    public String mServiceId;

    protected NearServiceFilter(Parcel in) {
        this.mServiceId = in.readString();
        this.mFilterData = in.createByteArray();
        this.mFilterMask = in.createByteArray();
    }

    private NearServiceFilter() {
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mServiceId);
        dest.writeByteArray(this.mFilterData);
        dest.writeByteArray(this.mFilterMask);
    }

    public int describeContents() {
        return 0;
    }

    public byte[] getFilterData() {
        return this.mFilterData;
    }

    public String getServiceId() {
        return this.mServiceId;
    }

    public byte[] getFilterMask() {
        return this.mFilterMask;
    }

    public static class Builder {
        NearServiceFilter filter = new NearServiceFilter();

        public Builder serviceId(String serviceId) {
            String unused = this.filter.mServiceId = serviceId;
            return this;
        }

        public Builder filterData(byte[] filterData) {
            byte[] unused = this.filter.mFilterData = filterData;
            return this;
        }

        public Builder filterMask(byte[] filterMask) {
            byte[] unused = this.filter.mFilterMask = filterMask;
            return this;
        }

        public NearServiceFilter build() {
            return this.filter;
        }
    }
}
