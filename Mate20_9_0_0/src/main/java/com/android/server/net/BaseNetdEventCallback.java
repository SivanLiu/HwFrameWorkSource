package com.android.server.net;

import android.net.INetdEventCallback.Stub;

public class BaseNetdEventCallback extends Stub {
    public void onDnsEvent(String hostname, String[] ipAddresses, int ipAddressesCount, long timestamp, int uid) {
    }

    public void onPrivateDnsValidationEvent(int netId, String ipAddress, String hostname, boolean validated) {
    }

    public void onConnectEvent(String ipAddr, int port, long timestamp, int uid) {
    }
}
