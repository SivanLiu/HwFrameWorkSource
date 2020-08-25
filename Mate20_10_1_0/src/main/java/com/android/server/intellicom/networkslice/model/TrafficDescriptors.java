package com.android.server.intellicom.networkslice.model;

import android.os.Bundle;
import android.os.Messenger;
import android.text.TextUtils;
import java.net.InetAddress;

public class TrafficDescriptors {
    private static final byte MATCH_ALL = 1;
    private static final byte MATCH_AVAILABLE = 8;
    private static final byte MATCH_DNN = 2;
    private static final byte MATCH_FQDN = 4;
    private static final String TDS_APPIDS = "appIds";
    private static final String TDS_IPV4_ADDRANDMASK = "ipv4AddrAndMask";
    private static final String TDS_IPV4_NUM = "ipv4Num";
    private static final String TDS_IPV6_ADDRANDPREFIX = "ipv6AddrAndPrefix";
    private static final String TDS_IPV6_NUM = "ipv6Num";
    private static final String TDS_PROTOCOLIDS = "protocolIds";
    private static final String TDS_REMOTEPORTS = "remotePorts";
    private static final String TDS_ROUTE_BITMAP = "routeBitmap";
    private static final String TDS_URSP_PRECEDENCE = "urspPrecedence";
    private final String mAppIds;
    private final String mDnn;
    private final String mFqdn;
    private final FqdnIps mFqdnIps;
    private boolean mHasAvailableUrsp;
    private final InetAddress mIp;
    private final byte[] mIpv4AddrAndMask;
    private final byte mIpv4Num;
    private final byte[] mIpv6AddrAndPrefix;
    private final byte mIpv6Num;
    private final boolean mIsIpTriad;
    private final boolean mIsMatchAll;
    private final boolean mIsMatchDnn;
    private final boolean mIsMatchFqdn;
    private final boolean mIsNeedToCreateRequest;
    private boolean mIsRequestAgain;
    private final Messenger mMessenger;
    private final String mProtocolId;
    private final String mProtocolIds;
    private final String mRemotePort;
    private final String mRemotePorts;
    private RouteBindType mRouteBindType;
    private final byte mRouteBitmap;
    private final int mUid;
    private final byte mUrspPrecedence;

    public enum RouteBindType {
        UID_TDS,
        UID_IP_TDS,
        IP_TDS,
        INVALID_TDS
    }

    public static TrafficDescriptors makeMatchAllTrafficDescriptors(Bundle data) {
        if (data == null) {
            return new Builder().build();
        }
        return new Builder().setUrspPrecedence(data.getByte("urspPrecedence", (byte) 0).byteValue()).setRouteBitmap(data.getByte(TDS_ROUTE_BITMAP, (byte) 0).byteValue()).build();
    }

    public static TrafficDescriptors makeTrafficDescriptors(Bundle data) {
        if (data == null) {
            return new Builder().build();
        }
        return new Builder().setAppIds(data.getString(TDS_APPIDS, "")).setUrspPrecedence(data.getByte("urspPrecedence", (byte) 0).byteValue()).setIpv4Num(data.getByte("ipv4Num", (byte) 0).byteValue()).setIpv4AddrAndMask(data.getByteArray("ipv4AddrAndMask")).setIpv6Num(data.getByte("ipv6Num", (byte) 0).byteValue()).setIpv6AddrAndPrefix(data.getByteArray("ipv6AddrAndPrefix")).setProtocolIds(data.getString("protocolIds", "")).setRemotePorts(data.getString("remotePorts", "")).setRouteBitmap(data.getByte(TDS_ROUTE_BITMAP, (byte) 0).byteValue()).build();
    }

    private TrafficDescriptors(Builder builder) {
        this.mUrspPrecedence = builder.mUrspPrecedence;
        this.mAppIds = builder.mAppIds;
        this.mIpv4Num = builder.mIpv4Num;
        this.mIpv4AddrAndMask = builder.mIpv4AddrAndMask;
        this.mIpv6Num = builder.mIpv6Num;
        this.mIpv6AddrAndPrefix = builder.mIpv6AddrAndPrefix;
        this.mProtocolIds = builder.mProtocolIds;
        this.mRemotePorts = builder.mRemotePorts;
        this.mRouteBitmap = builder.mRouteBitmap;
        this.mDnn = builder.mDnn;
        this.mFqdn = builder.mFqdn;
        this.mIp = builder.mIp;
        this.mProtocolId = builder.mProtocolId;
        this.mRemotePort = builder.mRemotePort;
        this.mUid = builder.mUid;
        this.mMessenger = builder.mMessenger;
        this.mFqdnIps = builder.mFqdnIps;
        this.mIsNeedToCreateRequest = builder.mIsNeedToCreateRequest;
        boolean hasAppids = false;
        this.mIsIpTriad = (this.mIpv4Num == 0 && this.mIpv6Num == 0) ? false : true;
        this.mIsMatchFqdn = (this.mRouteBitmap & MATCH_FQDN) != 0;
        this.mIsMatchDnn = (this.mRouteBitmap & MATCH_DNN) != 0;
        this.mIsMatchAll = (this.mRouteBitmap & 1) != 0;
        this.mHasAvailableUrsp = (this.mRouteBitmap & MATCH_AVAILABLE) != 0;
        boolean hasIps = this.mIsIpTriad || this.mIsMatchFqdn;
        hasAppids = (!TextUtils.isEmpty(this.mAppIds) || this.mIsMatchDnn) ? true : hasAppids;
        this.mRouteBindType = RouteBindType.INVALID_TDS;
        if (hasAppids && hasIps) {
            this.mRouteBindType = RouteBindType.UID_IP_TDS;
        } else if (hasAppids) {
            this.mRouteBindType = RouteBindType.UID_TDS;
        } else if (hasIps) {
            this.mRouteBindType = RouteBindType.IP_TDS;
        }
    }

    public void setRequestAgain(boolean requestAgain) {
        this.mIsRequestAgain = requestAgain;
    }

    public boolean isRequestAgain() {
        return this.mIsRequestAgain;
    }

    public RouteBindType getRouteBindType() {
        return this.mRouteBindType;
    }

    public byte getUrspPrecedence() {
        return this.mUrspPrecedence;
    }

    public String getAppIds() {
        return this.mAppIds;
    }

    public byte getIpv4Num() {
        return this.mIpv4Num;
    }

    public byte[] getIpv4AddrAndMask() {
        return this.mIpv4AddrAndMask;
    }

    public byte getIpv6Num() {
        return this.mIpv6Num;
    }

    public byte[] getIpv6AddrAndPrefix() {
        return this.mIpv6AddrAndPrefix;
    }

    public String getProtocolIds() {
        return this.mProtocolIds;
    }

    public String getRemotePorts() {
        return this.mRemotePorts;
    }

    public byte getRouteBitmap() {
        return this.mRouteBitmap;
    }

    public FqdnIps getFqdnIps() {
        return this.mFqdnIps;
    }

    public Messenger getMessenger() {
        return this.mMessenger;
    }

    public int getUid() {
        return this.mUid;
    }

    public String getDnn() {
        return this.mDnn;
    }

    public String getFqdn() {
        return this.mFqdn;
    }

    public InetAddress getIp() {
        return this.mIp;
    }

    public String getProtocolId() {
        return this.mProtocolId;
    }

    public String getRemotePort() {
        return this.mRemotePort;
    }

    public boolean isNeedToCreateRequest() {
        return this.mIsNeedToCreateRequest;
    }

    public boolean isIpTriad() {
        return this.mIsIpTriad;
    }

    public static final class Builder {
        /* access modifiers changed from: private */
        public String mAppIds;
        /* access modifiers changed from: private */
        public String mDnn;
        /* access modifiers changed from: private */
        public String mFqdn;
        /* access modifiers changed from: private */
        public FqdnIps mFqdnIps;
        /* access modifiers changed from: private */
        public InetAddress mIp;
        /* access modifiers changed from: private */
        public byte[] mIpv4AddrAndMask;
        /* access modifiers changed from: private */
        public byte mIpv4Num;
        /* access modifiers changed from: private */
        public byte[] mIpv6AddrAndPrefix;
        /* access modifiers changed from: private */
        public byte mIpv6Num;
        /* access modifiers changed from: private */
        public boolean mIsNeedToCreateRequest;
        /* access modifiers changed from: private */
        public Messenger mMessenger;
        /* access modifiers changed from: private */
        public String mProtocolId;
        /* access modifiers changed from: private */
        public String mProtocolIds;
        /* access modifiers changed from: private */
        public String mRemotePort;
        /* access modifiers changed from: private */
        public String mRemotePorts;
        /* access modifiers changed from: private */
        public byte mRouteBitmap;
        /* access modifiers changed from: private */
        public int mUid;
        /* access modifiers changed from: private */
        public byte mUrspPrecedence;

        public Builder setUrspPrecedence(byte urspPrecedence) {
            this.mUrspPrecedence = urspPrecedence;
            return this;
        }

        public Builder setAppIds(String appIds) {
            this.mAppIds = appIds;
            return this;
        }

        public Builder setIpv4Num(byte ipv4Num) {
            this.mIpv4Num = ipv4Num;
            return this;
        }

        public Builder setIpv4AddrAndMask(byte[] ipv4AddrAndMask) {
            this.mIpv4AddrAndMask = ipv4AddrAndMask;
            return this;
        }

        public Builder setIpv6Num(byte ipv6Num) {
            this.mIpv6Num = ipv6Num;
            return this;
        }

        public Builder setIpv6AddrAndPrefix(byte[] ipv6AddrAndPrefix) {
            this.mIpv6AddrAndPrefix = ipv6AddrAndPrefix;
            return this;
        }

        public Builder setProtocolIds(String protocolIds) {
            this.mProtocolIds = protocolIds;
            return this;
        }

        public Builder setRemotePorts(String remotePorts) {
            this.mRemotePorts = remotePorts;
            return this;
        }

        public Builder setRouteBitmap(byte routeBitmap) {
            this.mRouteBitmap = routeBitmap;
            return this;
        }

        public Builder setUid(int uid) {
            this.mUid = uid;
            return this;
        }

        public Builder setDnn(String dnn) {
            this.mDnn = dnn;
            return this;
        }

        public Builder setFqdn(String fqdn) {
            this.mFqdn = fqdn;
            return this;
        }

        public Builder setIp(InetAddress ip) {
            this.mIp = ip;
            return this;
        }

        public Builder setProtocolId(String protocolId) {
            this.mProtocolId = protocolId;
            return this;
        }

        public Builder setRemotePort(String remotePort) {
            this.mRemotePort = remotePort;
            return this;
        }

        public Builder setMessenger(Messenger messenger) {
            this.mMessenger = messenger;
            return this;
        }

        public Builder setFqdnIps(FqdnIps fqdnIps) {
            this.mFqdnIps = fqdnIps;
            return this;
        }

        public Builder setNeedToCreateRequest(boolean needToCreateRequest) {
            this.mIsNeedToCreateRequest = needToCreateRequest;
            return this;
        }

        public TrafficDescriptors build() {
            return new TrafficDescriptors(this);
        }
    }

    public boolean isMatchAll() {
        return this.mIsMatchAll;
    }

    public boolean isMatchDnn() {
        return this.mIsMatchDnn;
    }

    public boolean isMatchFqdn() {
        return this.mIsMatchFqdn;
    }

    public boolean hasAvailableUrsp() {
        return this.mHasAvailableUrsp;
    }

    public String toString() {
        return "TrafficDescriptors{mRouteBitmap=" + ((int) this.mRouteBitmap) + ", mUrspPrecedence=" + ((int) this.mUrspPrecedence) + ", mIpv4Num=" + ((int) this.mIpv4Num) + ", mIpv6Num=" + ((int) this.mIpv6Num) + ", mProtocolIds='" + this.mProtocolIds + '\'' + ", mRouteBindType=" + this.mRouteBindType + ", mDnn='" + this.mDnn + ", mProtocolId='" + this.mProtocolId + '\'' + ", mRemotePort='" + this.mRemotePort + '\'' + ", mMessenger=" + this.mMessenger + ", mIsNeedToCreateRequest=" + this.mIsNeedToCreateRequest + ", mIsIpTriad=" + this.mIsIpTriad + ", mIsMatchDnn=" + this.mIsMatchDnn + ", mIsMatchFqdn=" + this.mIsMatchFqdn + ", mHasAvailableUrsp=" + this.mHasAvailableUrsp + '}';
    }
}
