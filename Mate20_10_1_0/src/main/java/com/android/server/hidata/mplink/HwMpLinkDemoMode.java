package com.android.server.hidata.mplink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.widget.Toast;
import huawei.android.net.hwmplink.MpLinkCommonUtils;

public class HwMpLinkDemoMode {
    private static final String EXTRA_NETWORK_CHANGE_TYPE = "extra_network_change_type";
    private static final int MPLINK_BIND_NETWORK_STATE = 1;
    private static final int MPLINK_UNBIND_NETWORK_STATE = 2;
    private static final String TAG = "HiData_HwMpLinkDemoMode";
    private static final String TESTMODE_BCM_PERMISSION = "huawei.permission.RECEIVE_WIFI_PRO_STATE";
    private static final int TESTMODE_MSG_LTE_TEST = 10000;
    private static final String TESTMODE_NETWORK_CHANGE_ACTION = "huawei.wifi.pro.NETWORK_CHANGE";
    private static final int TIMEOUT_MILLISECOND = 100000;
    private static final int UNKOWEN_NETWORK = -1;
    private IntentFilter intentFilter = new IntentFilter();
    /* access modifiers changed from: private */
    public boolean isDeviceBootCompleted;
    /* access modifiers changed from: private */
    public LocationManager locationManager;
    private BroadcastReceiver mBroadcastReceiver = new MpLinkTestCaseBroadcastReceiver();
    /* access modifiers changed from: private */
    public ConnectivityManager mConnectivityManager;
    private Context mContext;
    private Handler mHandler;
    private MyNetworkCallback mNetworkCallback;

    public HwMpLinkDemoMode(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        registerBroadcastReceiver();
    }

    private void registerBroadcastReceiver() {
        this.intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        this.intentFilter.addAction("android.location.PROVIDERS_CHANGED");
        this.intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.intentFilter);
    }

    public void lteActivating() {
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(12);
        builder.addTransportType(0);
        NetworkRequest networkRequest = builder.build();
        this.mNetworkCallback = new MyNetworkCallback();
        this.mConnectivityManager.requestNetwork(networkRequest, this.mNetworkCallback, TIMEOUT_MILLISECOND);
    }

    public class MyNetworkCallback extends ConnectivityManager.NetworkCallback {
        public MyNetworkCallback() {
        }

        public void onPreCheck(Network network) {
            MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "onPreCheck,network: %{public}s", new Object[]{network});
        }

        public void onAvailable(Network network) {
            MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "onAvailable,network: %{public}s", new Object[]{network});
        }

        public void onUnavailable() {
            MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "onUnavailable", new Object[0]);
        }

        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "onCapabilitiesChanged,network: %{public}s", new Object[]{network});
        }

        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "onLinkPropertiesChanged,network: %{public}s", new Object[]{network});
        }

        public void onNetworkSuspended(Network network) {
            MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "onNetworkSuspended,network: %{public}s", new Object[]{network});
        }

        public void onNetworkResumed(Network network) {
            MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "onNetworkResumed,network: %{public}s", new Object[]{network});
        }
    }

    private class MpLinkTestCaseBroadcastReceiver extends BroadcastReceiver {
        private MpLinkTestCaseBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "received intent is null, return", new Object[0]);
                return;
            }
            String action = intent.getAction();
            if (HwMpLinkDemoMode.this.isDeviceBootCompleted && "android.bluetooth.adapter.action.STATE_CHANGED".equals(action)) {
                int blueState = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", 0);
                if (blueState == 10) {
                    HwMpLinkDemoMode.this.requestWiFiAndCellCoexist(false);
                } else if (blueState == 12) {
                    HwMpLinkDemoMode.this.requestWiFiAndCellCoexist(true);
                } else if (blueState == 10000) {
                    HwMpLinkDemoMode.this.lteActivating();
                }
            } else if (!HwMpLinkDemoMode.this.isDeviceBootCompleted || !"android.location.PROVIDERS_CHANGED".equals(action)) {
                if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                    MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "BOOT_COMPLETED", new Object[0]);
                    boolean unused = HwMpLinkDemoMode.this.isDeviceBootCompleted = true;
                    LocationManager unused2 = HwMpLinkDemoMode.this.locationManager = (LocationManager) context.getSystemService("location");
                    ConnectivityManager unused3 = HwMpLinkDemoMode.this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
                }
            } else if (HwMpLinkDemoMode.this.locationManager == null) {
                MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "locationManager == null", new Object[0]);
            } else {
                try {
                    if (HwMpLinkDemoMode.this.locationManager.isProviderEnabled("gps")) {
                        MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "Gps provider enabled.", new Object[0]);
                        HwMpLinkDemoMode.this.handleBindProcessToNetwork();
                        return;
                    }
                    MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "Gps provider disEnabled.", new Object[0]);
                    HwMpLinkDemoMode.this.handleClearBindProcessToNetwork();
                } catch (IllegalArgumentException e) {
                    MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, false, "e:%{public}s", new Object[]{e.getMessage()});
                }
            }
        }
    }

    public void notifyNetWorkHandover(int state) {
        MpLinkCommonUtils.logD(TAG, false, "notifyNetWorkHandover state = %{public}d", new Object[]{Integer.valueOf(state)});
        Intent intent = new Intent(TESTMODE_NETWORK_CHANGE_ACTION);
        intent.setFlags(67108864);
        intent.putExtra(EXTRA_NETWORK_CHANGE_TYPE, state);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, TESTMODE_BCM_PERMISSION);
    }

    private int getNetworkId(int networkType) {
        Network[] networks = this.mConnectivityManager.getAllNetworks();
        if (networks == null || networks.length == 0) {
            return -1;
        }
        for (Network networkItem : networks) {
            NetworkInfo netInfo = this.mConnectivityManager.getNetworkInfo(networkItem);
            if (netInfo != null && netInfo.getType() == networkType && networkItem != null) {
                return networkItem.netId;
            }
        }
        return -1;
    }

    /* access modifiers changed from: private */
    public void handleBindProcessToNetwork() {
        int netId = getNetworkId(0);
        MpLinkCommonUtils.logD(TAG, false, "Bluetooth STATE_ON. LTE netid:%{public}d", new Object[]{Integer.valueOf(netId)});
        if (netId != -1) {
            Handler handler = this.mHandler;
            handler.sendMessage(Message.obtain(handler, 2, netId, MpLinkCommonUtils.getForegroundAppUid(this.mContext), new MpLinkQuickSwitchConfiguration()));
            notifyNetWorkHandover(1);
            return;
        }
        MpLinkCommonUtils.logD(TAG, false, "LTE is disconnected", new Object[0]);
    }

    /* access modifiers changed from: private */
    public void handleClearBindProcessToNetwork() {
        int wifiNetId = getNetworkId(1);
        MpLinkCommonUtils.logD(TAG, false, "Bluetooth STATE_OFF. wifi netid:%{public}d", new Object[]{Integer.valueOf(wifiNetId)});
        if (wifiNetId != -1) {
            Handler handler = this.mHandler;
            handler.sendMessage(Message.obtain(handler, 3, wifiNetId, MpLinkCommonUtils.getForegroundAppUid(this.mContext), new MpLinkQuickSwitchConfiguration()));
            notifyNetWorkHandover(2);
            return;
        }
        MpLinkCommonUtils.logD(TAG, false, "wifi is disconnected", new Object[0]);
    }

    /* access modifiers changed from: private */
    public void requestWiFiAndCellCoexist(boolean isEnabled) {
        if (isEnabled) {
            Handler handler = this.mHandler;
            handler.sendMessage(Message.obtain(handler, 103, 0, 0));
            return;
        }
        Handler handler2 = this.mHandler;
        handler2.sendMessage(Message.obtain(handler2, 103, 1, 0));
    }

    public void showToast(String info) {
        Toast.makeText(this.mContext, info, 1).show();
    }

    public void wifiPlusHandover(IRFInterferenceCallback callback) {
        MpLinkCommonUtils.logD(TAG, false, "wifiPlusHandover", new Object[0]);
    }
}
