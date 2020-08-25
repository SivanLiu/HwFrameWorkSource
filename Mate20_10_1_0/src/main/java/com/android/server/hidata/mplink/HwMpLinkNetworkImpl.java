package com.android.server.hidata.mplink;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.os.UserHandle;
import com.android.server.intellicom.common.SmartDualCardConsts;
import huawei.android.net.hwmplink.MpLinkCommonUtils;

public class HwMpLinkNetworkImpl {
    private static final String LTE_FIELD = "LTE";
    private static final String MOBILE_FIELD = "MOBILE";
    private static final String TAG = "HiData_HwMpLinkNetworkImpl";
    private static final String WIFI_FIELD = "WIFI";
    private Context mContext;

    public HwMpLinkNetworkImpl(Context context) {
        this.mContext = context;
    }

    public void handleNetworkStrategy(int strategy, int uid) {
        if (uid > 0 && strategy == 1) {
            sendNetChangedMobileConnected(uid);
        }
    }

    public NetworkInfo createMobileNetworkInfo() {
        NetworkInfo networkInfo = new NetworkInfo(0, 0, "MOBILE", LTE_FIELD);
        networkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, "");
        networkInfo.setIsAvailable(true);
        return networkInfo;
    }

    public NetworkInfo createWifiNetworkInfo() {
        NetworkInfo networkInfo = new NetworkInfo(1, 0, "WIFI", "");
        networkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, "");
        networkInfo.setIsAvailable(true);
        return networkInfo;
    }

    public void sendNetChangedWifiDisconnected(int uid) {
        NetworkInfo mNetworkInfo = new NetworkInfo(1, 0, "WIFI", "");
        mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, "");
        mNetworkInfo.setIsAvailable(true);
        Intent intent = new Intent(SmartDualCardConsts.ACTION_CONNECTIVITY_CHANGE);
        intent.putExtra("networkInfo", mNetworkInfo);
        intent.putExtra("networkType", mNetworkInfo.getType());
        intent.setPackage(MpLinkCommonUtils.getPackageName(this.mContext, uid));
        MpLinkCommonUtils.logI(TAG, false, "mNetworkInfo %{public}s", new Object[]{mNetworkInfo.toString()});
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void sendNetChangedWifiConnected(int uid) {
        NetworkInfo mNetworkInfo = new NetworkInfo(1, 0, "WIFI", "");
        mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, "");
        mNetworkInfo.setIsAvailable(true);
        Intent intent = new Intent(SmartDualCardConsts.ACTION_CONNECTIVITY_CHANGE);
        intent.putExtra("networkInfo", mNetworkInfo);
        intent.putExtra("networkType", mNetworkInfo.getType());
        intent.setPackage(MpLinkCommonUtils.getPackageName(this.mContext, uid));
        MpLinkCommonUtils.logI(TAG, false, "mNetworkInfo %{public}s", new Object[]{mNetworkInfo.toString()});
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void sendNetChangedMobileConnected(int uid) {
        NetworkInfo mNetworkInfo = new NetworkInfo(0, 0, "MOBILE", LTE_FIELD);
        mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, "");
        mNetworkInfo.setIsAvailable(true);
        Intent intent = new Intent(SmartDualCardConsts.ACTION_CONNECTIVITY_CHANGE);
        intent.putExtra("networkInfo", mNetworkInfo);
        intent.putExtra("networkType", mNetworkInfo.getType());
        intent.setPackage(MpLinkCommonUtils.getPackageName(this.mContext, uid));
        MpLinkCommonUtils.logD(TAG, false, "send networkChange NetworkInfo %{public}s", new Object[]{mNetworkInfo.toString()});
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void sendWifiDisabledBroadcast(int uid) {
        Intent intent = new Intent("android.net.wifi.WIFI_STATE_CHANGED");
        intent.putExtra("wifi_state", 1);
        intent.putExtra("previous_wifi_state", 3);
        intent.setPackage(MpLinkCommonUtils.getPackageName(this.mContext, uid));
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void sendWifiDisconnectedBroadcast(int uid) {
        MpLinkCommonUtils.logD(TAG, false, "sendWifiDisconnectedBroadcast %{public}s", new Object[]{MpLinkCommonUtils.getPackageName(this.mContext, uid)});
        Intent intent = new Intent(SmartDualCardConsts.SYSTEM_STATE_NAME_WIFI_NETWORK_STATE_CHANGED);
        intent.addFlags(67108864);
        NetworkInfo mNetworkInfo = new NetworkInfo(1, 0, "WIFI", "");
        mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, "");
        mNetworkInfo.setIsAvailable(false);
        intent.putExtra("networkInfo", mNetworkInfo);
        intent.setPackage(MpLinkCommonUtils.getPackageName(this.mContext, uid));
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }
}
