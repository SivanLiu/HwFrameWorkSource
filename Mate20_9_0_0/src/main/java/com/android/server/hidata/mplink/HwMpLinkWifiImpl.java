package com.android.server.hidata.mplink;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.os.Handler;
import android.os.Message;
import huawei.android.net.hwmplink.MpLinkCommonUtils;

public class HwMpLinkWifiImpl {
    private static final String TAG = "HiData_HwMpLinkWifiImpl";
    private Context mContext;
    private Handler mHandler;
    private boolean mWifiConnectState = false;
    private boolean mWifiVpnConnected = false;

    public HwMpLinkWifiImpl(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
    }

    private void sendMessage(int what) {
        this.mHandler.sendMessage(Message.obtain(this.mHandler, what));
    }

    public boolean isWifiConnected() {
        return this.mWifiConnectState;
    }

    public boolean getCurrentWifiConnectState() {
        return this.mWifiConnectState;
    }

    public boolean getCurrentWifiVpnState() {
        return this.mWifiVpnConnected;
    }

    public void setCurrentWifiVpnState(boolean vpnconnected) {
        this.mWifiVpnConnected = vpnconnected;
    }

    public void handleWifiNetworkStateChanged(NetworkInfo netInfo) {
        if (netInfo != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WIFI NETWORK_STATE_CHANGED_ACTION state:");
            stringBuilder.append(netInfo.getState());
            MpLinkCommonUtils.logI(str, stringBuilder.toString());
            if (netInfo.getState() == State.DISCONNECTED) {
                this.mWifiConnectState = false;
                HwMpLinkContentAware.getInstance(this.mContext).resetAiDeviceType();
                sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_WIFI_DISCONNECTED);
            } else if (netInfo.getState() == State.CONNECTED || netInfo.getDetailedState() == DetailedState.VERIFYING_POOR_LINK) {
                this.mWifiConnectState = true;
                sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_WIFI_CONNECTED);
            }
        }
    }

    public void handleVpnStateChange(boolean vpnconnected) {
        if (this.mWifiVpnConnected != vpnconnected) {
            this.mWifiVpnConnected = vpnconnected;
            if (vpnconnected) {
                MpLinkCommonUtils.logD(TAG, "WIFI_VPN_CONNETED");
                sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_CONNETED);
                return;
            }
            MpLinkCommonUtils.logD(TAG, "WIFI_VPN_DISCONNETED");
            sendMessage(HwMpLinkServiceImpl.MPLINK_MSG_WIFI_VPN_DISCONNETED);
        }
    }
}
