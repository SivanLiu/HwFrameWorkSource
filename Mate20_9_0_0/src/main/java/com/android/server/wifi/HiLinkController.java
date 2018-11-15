package com.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Process;
import android.util.Flog;
import android.util.Log;
import android.util.SparseArray;
import com.android.server.wifi.util.NativeUtil;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HiLinkController {
    private static final String TAG = "HiLinkController";
    private static WifiStateMachineUtils wifiStateMachineUtils = ((WifiStateMachineUtils) EasyInvokeFactory.getInvokeUtils(WifiStateMachineUtils.class));
    private Context mContext = null;
    private boolean mIsHiLinkActive = false;
    private WifiStateMachine mWifiStateMachine = null;

    public HiLinkController(Context context, WifiStateMachine wifiStateMachine) {
        this.mContext = context;
        this.mWifiStateMachine = wifiStateMachine;
    }

    public boolean isHiLinkActive() {
        return this.mIsHiLinkActive;
    }

    public void enableHiLinkHandshake(boolean uiEnable, String bssid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enableHiLinkHandshake:");
        stringBuilder.append(uiEnable);
        Log.d(str, stringBuilder.toString());
        if (uiEnable) {
            this.mIsHiLinkActive = true;
        } else {
            this.mIsHiLinkActive = false;
        }
    }

    public void sendWpsOkcStartedBroadcast() {
        Log.d(TAG, "sendBroadcast: android.net.wifi.action.HILINK_STATE_CHANGED");
        this.mContext.sendBroadcast(new Intent("android.net.wifi.action.HILINK_STATE_CHANGED"), "com.android.server.wifi.permission.HILINK_STATE_CHANGED");
        boolean ret = Flog.bdReport(this.mContext, 400);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("report HiLink connect action. EventId:400 ret:");
        stringBuilder.append(ret);
        Log.d(str, stringBuilder.toString());
    }

    public NetworkUpdateResult saveWpsOkcConfiguration(int connectionNetId, String connectionBssid, List<ScanResult> scanResults) {
        ScanResult connectionScanResult = null;
        for (ScanResult result : scanResults) {
            if (Objects.equals(connectionBssid, result.BSSID)) {
                connectionScanResult = result;
                break;
            }
        }
        if (connectionScanResult == null || !connectionScanResult.isHiLinkNetwork) {
            Log.d(TAG, "saveWpsOkcConfiguration: return");
            return null;
        }
        Log.d(TAG, "saveWpsOkcConfiguration: enter");
        String ifaceName = wifiStateMachineUtils.getInterfaceName(this.mWifiStateMachine);
        Map<String, WifiConfiguration> configs = new HashMap();
        if (WifiInjector.getInstance().getWifiNative().migrateNetworksFromSupplicant(ifaceName, configs, new SparseArray())) {
            WifiConfiguration config = null;
            for (WifiConfiguration value : configs.values()) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("wpa_supplicant.conf-->config.SSid=");
                stringBuilder.append(value.SSID);
                Log.d(str, stringBuilder.toString());
                if (Objects.equals(connectionScanResult.SSID, NativeUtil.removeEnclosingQuotes(value.SSID))) {
                    config = value;
                    break;
                }
            }
            if (config == null) {
                Log.e(TAG, "wpa_supplicant doesn't store this hilink bssid");
                return null;
            }
            int uid = Process.myUid();
            config.networkId = -1;
            config.isHiLinkNetwork = true;
            WifiConfigManager wifiConfigManager = wifiStateMachineUtils.getWifiConfigManager(this.mWifiStateMachine);
            NetworkUpdateResult result2 = wifiConfigManager.addOrUpdateNetwork(config, uid);
            if (result2.isSuccess()) {
                int netId = result2.getNetworkId();
                String str2;
                StringBuilder stringBuilder2;
                if (!wifiConfigManager.enableNetwork(result2.getNetworkId(), true, uid)) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("saveWpsOkcConfiguration: uid ");
                    stringBuilder2.append(uid);
                    stringBuilder2.append(" did not have the permissions to enable=");
                    stringBuilder2.append(netId);
                    Log.e(str2, stringBuilder2.toString());
                    return result2;
                } else if (wifiConfigManager.updateLastConnectUid(netId, uid)) {
                    WifiConnectivityManager wcm = wifiStateMachineUtils.getWifiConnectivityManager(this.mWifiStateMachine);
                    if (wcm != null) {
                        wcm.setUserConnectChoice(netId);
                    }
                    this.mWifiStateMachine.saveConnectingNetwork(wifiConfigManager.getConfiguredNetwork(netId));
                    return result2;
                } else {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("saveWpsOkcConfiguration: uid ");
                    stringBuilder2.append(uid);
                    stringBuilder2.append(" with insufficient permissions to connect=");
                    stringBuilder2.append(netId);
                    Log.e(str2, stringBuilder2.toString());
                    return result2;
                }
            }
            Log.e(TAG, "saveWpsOkcConfiguration: failed!");
            return null;
        }
        Log.e(TAG, "Failed to load networks from wpa_supplicant after Wps");
        return null;
    }
}
