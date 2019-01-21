package com.huawei.hilink.framework.aidl;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class CallRequest implements Parcelable {
    public static final Creator<CallRequest> CREATOR = new Creator<CallRequest>() {
        public CallRequest createFromParcel(Parcel source) {
            Builder builder = new Builder();
            builder.setServiceID(source.readString()).setQuery(source.readString());
            builder.setMethod(source.readInt());
            builder.setPayload(source.readString());
            builder.setRequestID(source.readInt());
            builder.setDeviceID(source.readString());
            builder.setRemoteIP(source.readString()).setRemotePort(source.readInt());
            return builder.build();
        }

        public CallRequest[] newArray(int size) {
            return new CallRequest[size];
        }
    };
    private static final int DEVID_MAX_LEN = 40;
    private static final int IP_MAX_LEN = 40;
    public static final int METHOD_GET = 1;
    public static final int METHOD_POST = 2;
    private static final int PORT_MAX = 65535;
    private static final int PORT_MIN = 0;
    private static final int QUERY_MAX_LEN = 128;
    private static final int SID_MAX_LEN = 64;
    private String deviceID;
    private int method;
    private String payload;
    private String query;
    private String remoteIP;
    private int remotePort;
    private int requestID;
    private String serviceID;

    public static class Builder {
        private String deviceID = null;
        private int method = 0;
        private String payload = null;
        private String query = null;
        private String remoteIP = null;
        private int remotePort = 0;
        private int requestID = 0;
        private String serviceID = null;

        public Builder setServiceID(String serviceid) {
            this.serviceID = serviceid;
            return this;
        }

        public Builder setRequestID(int requestID) {
            this.requestID = requestID;
            return this;
        }

        public Builder setQuery(String query) {
            this.query = query;
            return this;
        }

        public Builder setMethod(int method) {
            this.method = method;
            return this;
        }

        public Builder setPayload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder setDeviceID(String deviceID) {
            this.deviceID = deviceID;
            return this;
        }

        public Builder setRemoteIP(String remoteIP) {
            this.remoteIP = remoteIP;
            return this;
        }

        public Builder setRemotePort(int remotePort) {
            this.remotePort = remotePort;
            return this;
        }

        public CallRequest build() {
            CallRequest req = new CallRequest(this, null);
            if (req.isLegal()) {
                return req;
            }
            return null;
        }
    }

    private CallRequest(Builder builder) {
        this.serviceID = builder.serviceID;
        this.query = builder.query;
        this.method = builder.method;
        this.payload = builder.payload;
        this.requestID = builder.requestID;
        this.deviceID = builder.deviceID;
        this.remoteIP = builder.remoteIP;
        this.remotePort = builder.remotePort;
    }

    /* synthetic */ CallRequest(Builder builder, CallRequest callRequest) {
        this(builder);
    }

    public String getServiceID() {
        return this.serviceID;
    }

    public String getQuery() {
        return this.query;
    }

    public int getMethod() {
        return this.method;
    }

    public String getPayload() {
        return this.payload;
    }

    public int getRequestID() {
        return this.requestID;
    }

    public String getDeviceID() {
        return this.deviceID;
    }

    public String getRemoteIP() {
        return this.remoteIP;
    }

    public int getRemotePort() {
        return this.remotePort;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.serviceID);
        dest.writeString(this.query);
        dest.writeInt(this.method);
        dest.writeString(this.payload);
        dest.writeInt(this.requestID);
        dest.writeString(this.deviceID);
        dest.writeString(this.remoteIP);
        dest.writeInt(this.remotePort);
    }

    /* JADX WARNING: Missing block: B:40:0x0075, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isLegal() {
        if (this.serviceID == null || this.serviceID.length() == 0 || this.serviceID.length() > SID_MAX_LEN) {
            return false;
        }
        if (this.query != null && this.query.length() > QUERY_MAX_LEN) {
            return false;
        }
        if (this.method != 1 && this.method != 2) {
            return false;
        }
        if (this.method == 2 && (this.payload == null || this.payload.length() == 0)) {
            return false;
        }
        if ((this.deviceID == null || this.deviceID.length() <= 40) && this.remoteIP != null && this.remoteIP.length() != 0 && this.remoteIP.length() <= 40 && this.remotePort >= 0 && this.remotePort <= PORT_MAX) {
            return true;
        }
        return false;
    }
}
