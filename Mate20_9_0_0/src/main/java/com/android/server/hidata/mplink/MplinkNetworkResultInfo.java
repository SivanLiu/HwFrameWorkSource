package com.android.server.hidata.mplink;

import huawei.android.net.hwmplink.MpLinkCommonUtils;

public class MplinkNetworkResultInfo {
    public static final int MPLINK_DIS_NETCOEXIST_FAILED = 103;
    public static final int MPLINK_DIS_NETCOEXIST_SUCCESS = 102;
    public static final int MPLINK_NETCOEXIST_FAILED = 101;
    public static final int MPLINK_NETCOEXIST_SUCCESS = 100;
    public static final int MPLINK_REQUEST_FAIL_REASON_INTERNAL_DISABLED = 7;
    public static final int MPLINK_REQUEST_FAIL_REASON_MOBILE_DATA_CLOSED = 2;
    public static final int MPLINK_REQUEST_FAIL_REASON_MOBILE_DISCONNECTED = 9;
    public static final int MPLINK_REQUEST_FAIL_REASON_MOBILE_OUT_OF_SERVICE = 5;
    public static final int MPLINK_REQUEST_FAIL_REASON_MOBILE_ROAMING = 4;
    public static final int MPLINK_REQUEST_FAIL_REASON_MOBILE_TECH_NOT_SUITABLE = 3;
    public static final int MPLINK_REQUEST_FAIL_REASON_MPLINK_SWITCH_DISABLED = 8;
    public static final int MPLINK_REQUEST_FAIL_REASON_NONE = 0;
    public static final int MPLINK_REQUEST_FAIL_REASON_OTHER = 15;
    public static final int MPLINK_REQUEST_FAIL_REASON_TIMEOUT = 10;
    public static final int MPLINK_REQUEST_FAIL_REASON_WIFI_DISCONNECTED = 1;
    public static final int MPLINK_REQUEST_FAIL_REASON_WIFI_VPN_CONNECTED = 6;
    private static final String TAG = "HiData_MplinkNetworkResultInfo";
    int mActiveNetwork = -1;
    int mFailReason = 0;
    int mResult = 0;
    int mType = 0;

    public MplinkNetworkResultInfo(int result, int mFailReason, int mType) {
    }

    public int getResult() {
        return this.mResult;
    }

    public void setResult(int result) {
        this.mResult = result;
    }

    public int getAPType() {
        return this.mType;
    }

    public void setAPType(int type) {
        this.mType = type;
    }

    public int getFailReason() {
        return this.mFailReason;
    }

    public void setFailReason(int failReason) {
        this.mFailReason = failReason;
    }

    public int getActiveNetwork() {
        return this.mActiveNetwork;
    }

    public void setActiveNetwork(int activeNetwork) {
        this.mActiveNetwork = activeNetwork;
    }

    public void reset() {
        this.mResult = 0;
        this.mType = 0;
        this.mFailReason = 0;
        this.mActiveNetwork = -1;
    }

    public static int messgaeToFailReason(int msg) {
        if (msg == 202) {
            return 3;
        }
        if (msg == 204) {
            return 4;
        }
        if (msg == 206) {
            return 5;
        }
        if (msg == HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_CONNETED) {
            return 6;
        }
        if (msg == 210) {
            return 8;
        }
        if (msg == HwMpLinkServiceImpl.MPLINK_MSG_HIBRAIN_MPLINK_CLOSE) {
            return 7;
        }
        if (msg == HwMpLinkServiceImpl.MPLINK_MSG_WIFI_DISCONNECTED) {
            return 1;
        }
        if (msg != HwMpLinkServiceImpl.MPLINK_MSG_MOBILE_DATA_DISCONNECTED) {
            return 15;
        }
        return 9;
    }

    public void dump() {
        MpLinkCommonUtils.logI(TAG, "dump!");
    }
}
