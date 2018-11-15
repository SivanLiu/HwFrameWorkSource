package com.android.server.hidata.mplink;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.os.UserHandle;
import com.android.server.hidata.wavemapping.cons.Constant;
import huawei.android.net.hwmplink.MpLinkCommonUtils;

public class HwMpLinkNetworkImpl {
    private static final String TAG = "HiData_HwMpLinkNetworkImpl";
    private Context mContext;

    public HwMpLinkNetworkImpl(Context context) {
        this.mContext = context;
    }

    public void handleNetworkStrategy(int strategy, int uid) {
        if (uid > 0 && 1 == strategy) {
            sendNetChangedMobileConnected(uid);
        }
    }

    public NetworkInfo createMobileNetworkInfo() {
        NetworkInfo networkInfo = new NetworkInfo(0, 0, Constant.USERDB_APP_NAME_MOBILE, "LTE");
        networkInfo.setDetailedState(DetailedState.CONNECTED, null, "");
        networkInfo.setIsAvailable(true);
        return networkInfo;
    }

    public NetworkInfo createWifiNetworkInfo() {
        NetworkInfo networkInfo = new NetworkInfo(1, 0, Constant.USERDB_APP_NAME_WIFI, "");
        networkInfo.setDetailedState(DetailedState.CONNECTED, null, "");
        networkInfo.setIsAvailable(true);
        return networkInfo;
    }

    public void sendNetChangedWifiDisconnected(int uid) {
        NetworkInfo mNetworkInfo = new NetworkInfo(1, 0, Constant.USERDB_APP_NAME_WIFI, "");
        mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, null, "");
        mNetworkInfo.setIsAvailable(true);
        Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE");
        intent.putExtra("networkInfo", mNetworkInfo);
        intent.putExtra("networkType", mNetworkInfo.getType());
        intent.setPackage(MpLinkCommonUtils.getPackageName(this.mContext, uid));
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mNetworkInfo");
        stringBuilder.append(mNetworkInfo.toString());
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void sendNetChangedWifiConnected(int uid) {
        NetworkInfo mNetworkInfo = new NetworkInfo(1, 0, Constant.USERDB_APP_NAME_WIFI, "");
        mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, "");
        mNetworkInfo.setIsAvailable(true);
        Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE");
        intent.putExtra("networkInfo", mNetworkInfo);
        intent.putExtra("networkType", mNetworkInfo.getType());
        intent.setPackage(MpLinkCommonUtils.getPackageName(this.mContext, uid));
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mNetworkInfo");
        stringBuilder.append(mNetworkInfo.toString());
        MpLinkCommonUtils.logI(str, stringBuilder.toString());
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void sendNetChangedMobileConnected(int uid) {
        NetworkInfo mNetworkInfo = new NetworkInfo(0, 0, Constant.USERDB_APP_NAME_MOBILE, "LTE");
        mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, "");
        mNetworkInfo.setIsAvailable(true);
        Intent intent = new Intent("android.net.conn.CONNECTIVITY_CHANGE");
        intent.putExtra("networkInfo", mNetworkInfo);
        intent.putExtra("networkType", mNetworkInfo.getType());
        intent.setPackage(MpLinkCommonUtils.getPackageName(this.mContext, uid));
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("send networkChange NetworkInfo");
        stringBuilder.append(mNetworkInfo.toString());
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void sendWifiDisableddBroadcast(int uid) {
        Intent intent = new Intent("android.net.wifi.WIFI_STATE_CHANGED");
        intent.putExtra("wifi_state", 1);
        intent.putExtra("previous_wifi_state", 3);
        intent.setPackage(MpLinkCommonUtils.getPackageName(this.mContext, uid));
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void sendWifiDisconnectedBroadcast(int uid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendWifiDisconnectedBroadcast");
        stringBuilder.append(MpLinkCommonUtils.getPackageName(this.mContext, uid));
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        Intent intent = new Intent("android.net.wifi.STATE_CHANGE");
        intent.addFlags(67108864);
        NetworkInfo mNetworkInfo = new NetworkInfo(1, 0, Constant.USERDB_APP_NAME_WIFI, "");
        mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, null, "");
        mNetworkInfo.setIsAvailable(false);
        intent.putExtra("networkInfo", mNetworkInfo);
        intent.setPackage(MpLinkCommonUtils.getPackageName(this.mContext, uid));
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }
}
