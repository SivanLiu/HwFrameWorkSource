package com.huawei.hilink.framework.aidl;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class DiscoverRequest implements Parcelable {
    public static final Creator<DiscoverRequest> CREATOR = new Creator<DiscoverRequest>() {
        public DiscoverRequest createFromParcel(Parcel source) {
            return new Builder().setServiceType(source.readString()).setQuery(source.readString()).setRequestID(source.readInt()).build();
        }

        public DiscoverRequest[] newArray(int size) {
            return new DiscoverRequest[size];
        }
    };
    private static final int QUERY_MAX_LEN = 128;
    private static final int ST_MAX_LEN = 32;
    private String query;
    private int requestID;
    private String serviceType;

    public static class Builder {
        private String query = null;
        private int requestID = 0;
        private String serviceType = null;

        public Builder setServiceType(String serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        public Builder setQuery(String query) {
            this.query = query;
            return this;
        }

        public Builder setRequestID(int requestID) {
            this.requestID = requestID;
            return this;
        }

        public DiscoverRequest build() {
            DiscoverRequest req = new DiscoverRequest(this, null);
            if (req.isLegal()) {
                return req;
            }
            return null;
        }
    }

    private DiscoverRequest(Builder para) {
        this.serviceType = para.serviceType;
        this.query = para.query;
        this.requestID = para.requestID;
    }

    /* synthetic */ DiscoverRequest(Builder builder, DiscoverRequest discoverRequest) {
        this(builder);
    }

    public String getServiceType() {
        return this.serviceType;
    }

    public String getQuery() {
        return this.query;
    }

    public int getRequestID() {
        return this.requestID;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.serviceType);
        dest.writeString(this.query);
        dest.writeInt(this.requestID);
    }

    public boolean isLegal() {
        if (this.serviceType == null || this.serviceType.length() == 0 || this.serviceType.length() > 32) {
            return false;
        }
        if (this.query == null || this.query.length() <= QUERY_MAX_LEN) {
            return true;
        }
        return false;
    }
}
