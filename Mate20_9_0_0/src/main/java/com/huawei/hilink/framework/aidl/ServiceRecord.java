package com.huawei.hilink.framework.aidl;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class ServiceRecord implements Parcelable {
    public static final Creator<ServiceRecord> CREATOR = new Creator<ServiceRecord>() {
        public ServiceRecord createFromParcel(Parcel source) {
            return new Builder().setRequestID(source.readInt()).setRemoteip(source.readString()).setRemotport(source.readInt()).setPayload(source.readString()).build();
        }

        public ServiceRecord[] newArray(int size) {
            return new ServiceRecord[size];
        }
    };
    private static final int IP_MAX_LEN = 40;
    private static final int PORT_MAX = 65535;
    private static final int PORT_MIN = 0;
    private String payload;
    private String remoteIP;
    private int remotePort;
    private int requestID;

    public static class Builder {
        private String payload = null;
        private String remoteIP = null;
        private int remotePort = 0;
        private int requestID = 0;

        public Builder setRequestID(int requestID) {
            this.requestID = requestID;
            return this;
        }

        public Builder setRemoteip(String remoteIP) {
            this.remoteIP = remoteIP;
            return this;
        }

        public Builder setPayload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder setRemotport(int remotePort) {
            this.remotePort = remotePort;
            return this;
        }

        public ServiceRecord build() {
            ServiceRecord serviceRecord = new ServiceRecord(this, null);
            if (serviceRecord.isLegal()) {
                return serviceRecord;
            }
            return null;
        }
    }

    private ServiceRecord(Builder para) {
        this.requestID = para.requestID;
        this.remoteIP = para.remoteIP;
        this.payload = para.payload;
        this.remotePort = para.remotePort;
    }

    /* synthetic */ ServiceRecord(Builder builder, ServiceRecord serviceRecord) {
        this(builder);
    }

    public int getRequestID() {
        return this.requestID;
    }

    public String getRemoteIP() {
        return this.remoteIP;
    }

    public int getRemotePort() {
        return this.remotePort;
    }

    public String getPayload() {
        return this.payload;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.requestID);
        dest.writeString(this.remoteIP);
        dest.writeInt(this.remotePort);
        dest.writeString(this.payload);
    }

    /* JADX WARNING: Missing block: B:13:0x0027, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isLegal() {
        if (this.remoteIP == null || this.remoteIP.length() == 0 || this.remoteIP.length() > 40 || this.remotePort < 0 || this.remotePort > PORT_MAX) {
            return false;
        }
        return true;
    }
}
