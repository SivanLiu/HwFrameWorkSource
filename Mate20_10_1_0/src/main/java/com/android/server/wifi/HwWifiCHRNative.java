package com.android.server.wifi;

import android.util.wifi.HwHiLog;

public class HwWifiCHRNative {
    public static final int NETLINK_MSG_WIFI_TCP_START = 1;
    public static final int NETLINK_MSG_WIFI_TCP_STOP = 0;
    private static String TAG = "HwWifiCHRNative";

    private static native int registerNatives();

    public static native void setTcpMonitorStat(int i);

    static {
        try {
            System.loadLibrary("wifichrstat");
            HwHiLog.i(TAG, false, "loadLibrary: libwifichrstat.so successful", new Object[0]);
            registerNatives();
        } catch (UnsatisfiedLinkError err) {
            HwHiLog.e(TAG, false, "loadLibrary: libwifichrstat.so, failure!!!", new Object[0]);
            err.printStackTrace();
        }
    }
}
