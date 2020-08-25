package com.huawei.nearbysdk.softnet;

import android.os.Parcel;
import android.os.Parcelable;

public class NearDeviceDesc implements Parcelable {
    public static final Creator<NearDeviceDesc> CREATOR = new Creator<NearDeviceDesc>() {
        /* class com.huawei.nearbysdk.softnet.NearDeviceDesc.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public NearDeviceDesc createFromParcel(Parcel in) {
            return new NearDeviceDesc(in);
        }

        @Override // android.os.Parcelable.Creator
        public NearDeviceDesc[] newArray(int size) {
            return new NearDeviceDesc[size];
        }
    };
    /* access modifiers changed from: private */
    public String mBtMac;
    /* access modifiers changed from: private */
    public int[] mCapabilityBitmap;
    /* access modifiers changed from: private */
    public int mCapabilityBitmapNum;
    /* access modifiers changed from: private */
    public String mDeviceId;
    /* access modifiers changed from: private */
    public String mDeviceName;
    /* access modifiers changed from: private */
    public int mDeviceType;
    /* access modifiers changed from: private */
    public String mIpv4;
    /* access modifiers changed from: private */
    public String mIpv6;
    /* access modifiers changed from: private */
    public int mPort;
    /* access modifiers changed from: private */
    public String mReservedInfo;
    /* access modifiers changed from: private */
    public String mWifiMac;

    protected NearDeviceDesc(Parcel in) {
        this.mDeviceName = in.readString();
        this.mDeviceId = in.readString();
        this.mIpv4 = in.readString();
        this.mIpv6 = in.readString();
        this.mPort = in.readInt();
        this.mWifiMac = in.readString();
        this.mBtMac = in.readString();
        this.mDeviceType = in.readInt();
        this.mCapabilityBitmapNum = in.readInt();
        this.mCapabilityBitmap = in.createIntArray();
        this.mReservedInfo = in.readString();
    }

    private NearDeviceDesc() {
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mDeviceName);
        dest.writeString(this.mDeviceId);
        dest.writeString(this.mIpv4);
        dest.writeString(this.mIpv6);
        dest.writeInt(this.mPort);
        dest.writeString(this.mWifiMac);
        dest.writeString(this.mBtMac);
        dest.writeInt(this.mDeviceType);
        dest.writeInt(this.mCapabilityBitmapNum);
        dest.writeIntArray(this.mCapabilityBitmap);
        dest.writeString(this.mReservedInfo);
    }

    public int describeContents() {
        return 0;
    }

    public String getDeviceName() {
        return this.mDeviceName;
    }

    public String getDeviceId() {
        return this.mDeviceId;
    }

    public String getIpv4() {
        return this.mIpv4;
    }

    public String getIpv6() {
        return this.mIpv6;
    }

    public int getPort() {
        return this.mPort;
    }

    public String getBtMac() {
        return this.mBtMac;
    }

    public String getWifiMac() {
        return this.mWifiMac;
    }

    public int getDeviceType() {
        return this.mDeviceType;
    }

    public int getCapabilityBitmapNum() {
        return this.mCapabilityBitmapNum;
    }

    public int[] getCapabilityBitmap() {
        return this.mCapabilityBitmap;
    }

    public String getReservedInfo() {
        return this.mReservedInfo;
    }

    public static class Builder {
        private NearDeviceDesc info = new NearDeviceDesc();

        public Builder deviceName(String deviceName) {
            String unused = this.info.mDeviceName = deviceName;
            return this;
        }

        public Builder deviceId(String deviceId) {
            String unused = this.info.mDeviceId = deviceId;
            return this;
        }

        public Builder deviceType(int deviceType) {
            int unused = this.info.mDeviceType = deviceType;
            return this;
        }

        public Builder wifiMac(String wifiMac) {
            String unused = this.info.mWifiMac = wifiMac;
            return this;
        }

        public Builder btMac(String btMac) {
            String unused = this.info.mBtMac = btMac;
            return this;
        }

        public Builder ipv4(String ipv4) {
            String unused = this.info.mIpv4 = ipv4;
            return this;
        }

        public Builder ipv6(String ipv6) {
            String unused = this.info.mIpv6 = ipv6;
            return this;
        }

        public Builder port(int port) {
            int unused = this.info.mPort = port;
            return this;
        }

        public Builder capabilityBitmapNum(int capabilityBitmapNum) {
            int unused = this.info.mCapabilityBitmapNum = capabilityBitmapNum;
            return this;
        }

        public Builder capabilityBitmap(int[] capabilityBitmap) {
            int[] unused = this.info.mCapabilityBitmap = capabilityBitmap;
            return this;
        }

        public Builder reservedInfo(String reservedInfo) {
            String unused = this.info.mReservedInfo = reservedInfo;
            return this;
        }

        public NearDeviceDesc build() {
            return this.info;
        }
    }
}
