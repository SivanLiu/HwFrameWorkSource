package com.android.server.hidata.mplink;

public class MpLinkQuickSwitchConfiguration {
    private static final int DEFAULT_CAPACITY = 16;
    public static final int MPLINK_SK_TCP_CLOSE = 1;
    public static final int MPLINK_SK_UDP_RX_ERROR = 4;
    public static final int MPLINK_SK_UDP_TX_ERROR = 2;
    public static final int SIMULATE_NETWORK_CHANGE_FOR_LTE_CONNECTED = 1;
    public static final int SIMULATE_NETWORK_NOT_REQUIRED = 0;
    private int appId;
    private int networkStrategy;
    private int reason;
    private int reserved;
    private int scenesId;
    private int socketStrategy;

    public MpLinkQuickSwitchConfiguration() {
    }

    public MpLinkQuickSwitchConfiguration(int socketStrategy2, int networkStrategy2) {
        this.socketStrategy = socketStrategy2;
        this.networkStrategy = networkStrategy2;
    }

    public void copyObjectValue(MpLinkQuickSwitchConfiguration configuration) {
        if (configuration != null) {
            this.socketStrategy = configuration.getSocketStrategy();
            this.networkStrategy = configuration.getNetworkStrategy();
            this.reserved = configuration.getReserved();
            this.reason = configuration.getReason();
            this.appId = configuration.getAppId();
            this.scenesId = configuration.getScenesId();
        }
    }

    public int getReason() {
        return this.reason;
    }

    public void setReason(int reason2) {
        this.reason = reason2;
    }

    public int getAppId() {
        return this.appId;
    }

    public void setAppId(int appId2) {
        this.appId = appId2;
    }

    public int getScenesId() {
        return this.scenesId;
    }

    public void setScenesId(int scenesId2) {
        this.scenesId = scenesId2;
    }

    public int getSocketStrategy() {
        return this.socketStrategy;
    }

    public void setSocketStrategy(int socketStrategy2) {
        this.socketStrategy = socketStrategy2;
    }

    public int getNetworkStrategy() {
        return this.networkStrategy;
    }

    public void setNetworkStrategy(int networkStrategy2) {
        this.networkStrategy = networkStrategy2;
    }

    public int getReserved() {
        return this.reserved;
    }

    public void setReserved(int reserved2) {
        this.reserved = reserved2;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer(16);
        buffer.append("socketStrategy: ");
        buffer.append(this.socketStrategy);
        buffer.append(", networkStrategy: ");
        buffer.append(this.networkStrategy);
        buffer.append(", reserved: ");
        buffer.append(this.reserved);
        buffer.append(", scenesId: ");
        buffer.append(this.scenesId);
        buffer.append(", appId: ");
        buffer.append(this.appId);
        return buffer.toString();
    }
}
