package com.android.server.hidata.mplink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.widget.Toast;
import com.android.server.security.tsmagent.logic.spi.tsm.laser.LaserTSMServiceImpl;
import huawei.android.net.hwmplink.MpLinkCommonUtils;

public class HwMpLinkDemoMode {
    private static final String EXTRA_NETWORK_CHANGE_TYPE = "extra_network_change_type";
    private static final String TAG = "HiData_HwMpLinkDemoMode";
    private static final String TESTMODE_BCM_PERMISSION = "huawei.permission.RECEIVE_WIFI_PRO_STATE";
    private static final int TESTMODE_MSG_LTE_TEST = 10000;
    private static final String TESTMODE_NETWORK_CHANGE_ACTION = "huawei.wifi.pro.NETWORK_CHANGE";
    private static final int UNKOWEN_NETWORK = -1;
    private IntentFilter intentFilter = new IntentFilter();
    private LocationManager locationManager;
    private BroadcastReceiver mBroadcastReceiver = new MpLinkTestCaseBroadcastReceiver();
    private ConnectivityManager mConnectivityManager;
    private Context mContext;
    private boolean mDeviceBootCommlied;
    private Handler mHandler;
    private MyNetworkCallback mNetworkCallback;

    private class MpLinkTestCaseBroadcastReceiver extends BroadcastReceiver {
        private MpLinkTestCaseBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (HwMpLinkDemoMode.this.mDeviceBootCommlied && "android.bluetooth.adapter.action.STATE_CHANGED".equals(action)) {
                int blueState = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", 0);
                if (blueState == 10) {
                    HwMpLinkDemoMode.this.requestWiFiAndCellCoexist(false);
                } else if (blueState == 12) {
                    HwMpLinkDemoMode.this.requestWiFiAndCellCoexist(true);
                } else if (blueState == 10000) {
                    HwMpLinkDemoMode.this.lteActivating();
                }
            } else if (HwMpLinkDemoMode.this.mDeviceBootCommlied && "android.location.PROVIDERS_CHANGED".equals(action)) {
                if (HwMpLinkDemoMode.this.locationManager == null) {
                    MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, "locationManager == null");
                    return;
                }
                try {
                    if (HwMpLinkDemoMode.this.locationManager.isProviderEnabled("gps")) {
                        MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, "Gps provider enabled.");
                        HwMpLinkDemoMode.this.handleBindProcessToNetwork();
                        return;
                    }
                    MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, "Gps provider disEnabled.");
                    HwMpLinkDemoMode.this.handleClearBindProcessToNetwork();
                } catch (IllegalArgumentException e) {
                    String str = HwMpLinkDemoMode.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("e:");
                    stringBuilder.append(e);
                    MpLinkCommonUtils.logD(str, stringBuilder.toString());
                }
            } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, "BOOT_COMPLETED");
                HwMpLinkDemoMode.this.mDeviceBootCommlied = true;
                HwMpLinkDemoMode.this.locationManager = (LocationManager) context.getSystemService("location");
                HwMpLinkDemoMode.this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
            }
        }
    }

    public class MyNetworkCallback extends NetworkCallback {
        public void onPreCheck(Network network) {
            String str = HwMpLinkDemoMode.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPreCheck,network: ");
            stringBuilder.append(network);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
        }

        public void onAvailable(Network network) {
            String str = HwMpLinkDemoMode.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onAvailable,network: ");
            stringBuilder.append(network);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
        }

        public void onLosing(Network network, int maxMsToLive) {
            String str = HwMpLinkDemoMode.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onLosing,network: ");
            stringBuilder.append(network);
            stringBuilder.append(",maxMsToLive:");
            stringBuilder.append(maxMsToLive);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
        }

        public void onLost(Network network) {
            String str = HwMpLinkDemoMode.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onLost,network: ");
            stringBuilder.append(network);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
        }

        public void onUnavailable() {
            MpLinkCommonUtils.logD(HwMpLinkDemoMode.TAG, "onUnavailable");
        }

        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            String str = HwMpLinkDemoMode.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCapabilitiesChanged,network: ");
            stringBuilder.append(network);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
        }

        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            String str = HwMpLinkDemoMode.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onLinkPropertiesChanged,network: ");
            stringBuilder.append(network);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
        }

        public void onNetworkSuspended(Network network) {
            String str = HwMpLinkDemoMode.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onNetworkSuspended,network: ");
            stringBuilder.append(network);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
        }

        public void onNetworkResumed(Network network) {
            String str = HwMpLinkDemoMode.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onNetworkResumed,network: ");
            stringBuilder.append(network);
            MpLinkCommonUtils.logD(str, stringBuilder.toString());
        }
    }

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
        Builder builder = new Builder();
        builder.addCapability(12);
        builder.addTransportType(0);
        NetworkRequest networkRequest = builder.build();
        this.mNetworkCallback = new MyNetworkCallback();
        this.mConnectivityManager.requestNetwork(networkRequest, this.mNetworkCallback, LaserTSMServiceImpl.EXCUTE_OTA_RESULT_SUCCESS);
    }

    public void notificateNetWorkHandover(int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notificateNetWorkHandover state = ");
        stringBuilder.append(state);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        Intent intent = new Intent(TESTMODE_NETWORK_CHANGE_ACTION);
        intent.setFlags(67108864);
        intent.putExtra(EXTRA_NETWORK_CHANGE_TYPE, state);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, TESTMODE_BCM_PERMISSION);
    }

    private int getNetworkID(int networkType) {
        Network[] networks = this.mConnectivityManager.getAllNetworks();
        if (networks != null) {
            int length = networks.length;
            for (int i = 0; i < length; i++) {
                NetworkInfo netInfo = this.mConnectivityManager.getNetworkInfo(networks[i]);
                if (netInfo != null && netInfo.getType() == networkType) {
                    Network network = networks[i];
                    if (network != null) {
                        return network.netId;
                    }
                }
            }
        }
        return -1;
    }

    private void handleBindProcessToNetwork() {
        int netid = getNetworkID(0);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bluetooth STATE_ON. LTE netid:");
        stringBuilder.append(netid);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        if (netid != -1) {
            this.mHandler.sendMessage(Message.obtain(this.mHandler, 2, netid, MpLinkCommonUtils.getForegroundAppUid(this.mContext), new MpLinkQuickSwitchConfiguration()));
            notificateNetWorkHandover(1);
            return;
        }
        MpLinkCommonUtils.logD(TAG, "LTE is disconneced");
    }

    private void handleClearBindProcessToNetwork() {
        int wifinetid = getNetworkID(1);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bluetooth STATE_OFF. wifi netid:");
        stringBuilder.append(wifinetid);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        if (wifinetid != -1) {
            this.mHandler.sendMessage(Message.obtain(this.mHandler, 3, wifinetid, MpLinkCommonUtils.getForegroundAppUid(this.mContext), new MpLinkQuickSwitchConfiguration()));
            notificateNetWorkHandover(2);
            return;
        }
        MpLinkCommonUtils.logD(TAG, "wifi is disconneced");
    }

    private void requestWiFiAndCellCoexist(boolean enabled) {
        if (enabled) {
            this.mHandler.sendMessage(Message.obtain(this.mHandler, 103, 0, 0));
        } else {
            this.mHandler.sendMessage(Message.obtain(this.mHandler, 103, 1, 0));
        }
    }

    public void showToast(String info) {
        Toast.makeText(this.mContext, info, 1).show();
    }

    public void wifiplusHandover(IRFInterferenceCallback callback) {
        MpLinkCommonUtils.logD(TAG, "wifiplusHandover");
    }
}
