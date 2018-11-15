package com.android.server.hidata.mplink;

public class MpLinkQuickSwitchConfiguration {
    public static final int MPLINK_SK_TCP_CLOSE = 1;
    public static final int MPLINK_SK_UDP_RX_ERROR = 4;
    public static final int MPLINK_SK_UDP_TX_ERROR = 2;
    public static final int SIMULATE_NETWORK_CHANGE_FOR_LTE_CONNECTED = 1;
    public static final int SIMULATE_NETWORK_NOT_REQUIRED = 0;
    private int networkStrategy;
    private int reserved;
    private int socketStrategy;

    public MpLinkQuickSwitchConfiguration(int socketStrategy, int networkStrategy) {
        this.socketStrategy = socketStrategy;
        this.networkStrategy = networkStrategy;
    }

    public void copyObjectValue(MpLinkQuickSwitchConfiguration configuration) {
        if (configuration != null) {
            this.socketStrategy = configuration.getSocketStrategy();
            this.networkStrategy = configuration.getNetworkStrategy();
            this.reserved = configuration.getReserved();
        }
    }

    public int getSocketStrategy() {
        return this.socketStrategy;
    }

    public void setSocketStrategy(int socketStrategy) {
        this.socketStrategy = socketStrategy;
    }

    public int getNetworkStrategy() {
        return this.networkStrategy;
    }

    public void setNetworkStrategy(int networkStrategy) {
        this.networkStrategy = networkStrategy;
    }

    public int getReserved() {
        return this.reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("socketStrategy: ");
        sb.append(this.socketStrategy);
        sb.append(", networkStrategy: ");
        sb.append(this.networkStrategy);
        sb.append(", reserved: ");
        sb.append(this.reserved);
        return sb.toString();
    }
}
