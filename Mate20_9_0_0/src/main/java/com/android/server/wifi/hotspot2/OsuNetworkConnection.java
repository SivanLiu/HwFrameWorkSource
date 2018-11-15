package com.android.server.wifi.hotspot2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest.Builder;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

public class OsuNetworkConnection {
    private static final String TAG = "OsuNetworkConnection";
    private static final int TIMEOUT_MS = 10000;
    private Callbacks mCallbacks;
    private boolean mConnected = false;
    private ConnectivityCallbacks mConnectivityCallbacks;
    private ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private Handler mHandler;
    private Network mNetwork = null;
    private int mNetworkId = -1;
    private boolean mVerboseLoggingEnabled = false;
    private boolean mWifiEnabled = false;
    private WifiManager mWifiManager;

    public interface Callbacks {
        void onConnected(Network network);

        void onDisconnected();

        void onTimeOut();

        void onWifiDisabled();

        void onWifiEnabled();
    }

    private class ConnectivityCallbacks extends NetworkCallback {
        private ConnectivityCallbacks() {
        }

        /* synthetic */ ConnectivityCallbacks(OsuNetworkConnection x0, AnonymousClass1 x1) {
            this();
        }

        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            if (OsuNetworkConnection.this.mVerboseLoggingEnabled) {
                String str = OsuNetworkConnection.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onLinkPropertiesChanged for network=");
                stringBuilder.append(network);
                stringBuilder.append(" isProvisioned?");
                stringBuilder.append(linkProperties.isProvisioned());
                Log.v(str, stringBuilder.toString());
            }
            if (linkProperties.isProvisioned() && OsuNetworkConnection.this.mNetwork == null) {
                OsuNetworkConnection.this.mNetwork = network;
                OsuNetworkConnection.this.mConnected = true;
                if (OsuNetworkConnection.this.mCallbacks != null) {
                    OsuNetworkConnection.this.mCallbacks.onConnected(network);
                }
            }
        }

        public void onUnavailable() {
            if (OsuNetworkConnection.this.mVerboseLoggingEnabled) {
                Log.v(OsuNetworkConnection.TAG, "onUnvailable ");
            }
            if (OsuNetworkConnection.this.mCallbacks != null) {
                OsuNetworkConnection.this.mCallbacks.onTimeOut();
            }
        }

        public void onLost(Network network) {
            if (OsuNetworkConnection.this.mVerboseLoggingEnabled) {
                String str = OsuNetworkConnection.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onLost ");
                stringBuilder.append(network);
                Log.v(str, stringBuilder.toString());
            }
            if (network != OsuNetworkConnection.this.mNetwork) {
                Log.w(OsuNetworkConnection.TAG, "Irrelevant network lost notification");
                return;
            }
            if (OsuNetworkConnection.this.mCallbacks != null) {
                OsuNetworkConnection.this.mCallbacks.onDisconnected();
            }
        }
    }

    public OsuNetworkConnection(Context context) {
        this.mContext = context;
    }

    public void init(Handler handler) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    int state = intent.getIntExtra("wifi_state", 4);
                    if (state == 1 && OsuNetworkConnection.this.mWifiEnabled) {
                        OsuNetworkConnection.this.mWifiEnabled = false;
                        if (OsuNetworkConnection.this.mCallbacks != null) {
                            OsuNetworkConnection.this.mCallbacks.onWifiDisabled();
                        }
                    }
                    if (state == 3 && !OsuNetworkConnection.this.mWifiEnabled) {
                        OsuNetworkConnection.this.mWifiEnabled = true;
                        if (OsuNetworkConnection.this.mCallbacks != null) {
                            OsuNetworkConnection.this.mCallbacks.onWifiEnabled();
                        }
                    }
                }
            }
        };
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mContext.registerReceiver(receiver, filter, null, handler);
        this.mWifiEnabled = this.mWifiManager.isWifiEnabled();
        this.mConnectivityManager = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        this.mConnectivityCallbacks = new ConnectivityCallbacks(this, null);
        this.mHandler = handler;
    }

    public void disconnectIfNeeded() {
        if (this.mNetworkId < 0) {
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "No connection to tear down");
            }
            return;
        }
        this.mWifiManager.removeNetwork(this.mNetworkId);
        this.mNetworkId = -1;
        this.mNetwork = null;
        this.mConnected = false;
    }

    public void setEventCallback(Callbacks callbacks) {
        this.mCallbacks = callbacks;
    }

    public boolean connect(WifiSsid ssid, String nai) {
        if (this.mConnected) {
            if (this.mVerboseLoggingEnabled) {
                Log.v(TAG, "Connect called twice");
            }
            return true;
        } else if (this.mWifiEnabled) {
            WifiConfiguration config = new WifiConfiguration();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\"");
            stringBuilder.append(ssid.toString());
            stringBuilder.append("\"");
            config.SSID = stringBuilder.toString();
            if (TextUtils.isEmpty(nai)) {
                config.allowedKeyManagement.set(0);
                this.mNetworkId = this.mWifiManager.addNetwork(config);
                if (this.mNetworkId < 0) {
                    Log.e(TAG, "Unable to add network");
                    return false;
                }
                this.mConnectivityManager.requestNetwork(new Builder().addTransportType(1).build(), this.mConnectivityCallbacks, this.mHandler, 10000);
                StringBuilder stringBuilder2;
                if (this.mWifiManager.enableNetwork(this.mNetworkId, true)) {
                    if (this.mVerboseLoggingEnabled) {
                        String str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Current network ID ");
                        stringBuilder2.append(this.mNetworkId);
                        Log.v(str, stringBuilder2.toString());
                    }
                    return true;
                }
                String str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unable to enable network ");
                stringBuilder2.append(this.mNetworkId);
                Log.e(str2, stringBuilder2.toString());
                disconnectIfNeeded();
                return false;
            }
            Log.w(TAG, "OSEN not supported");
            return false;
        } else {
            Log.w(TAG, "Wifi is not enabled");
            return false;
        }
    }

    public void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
    }
}
