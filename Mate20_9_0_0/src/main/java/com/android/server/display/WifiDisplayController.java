package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.display.WifiDisplay;
import android.hardware.display.WifiDisplaySessionInfo;
import android.media.RemoteDisplay;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.os.Handler;
import android.provider.Settings.Global;
import android.util.Log;
import android.util.Slog;
import android.view.Surface;
import com.android.internal.util.DumpUtils.Dump;
import com.android.server.HwServiceFactory;
import com.android.server.display.hisi_wifi.WifiDisplayControllerHisiExt;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Objects;

final class WifiDisplayController implements IWifiDisplayControllerInner, Dump {
    private static final int CONNECTION_TIMEOUT_SECONDS = 30;
    private static final int CONNECT_MAX_RETRIES = 3;
    private static final int CONNECT_RETRY_DELAY_MILLIS = 500;
    private static final boolean DEBUG = true;
    private static final int DEFAULT_CONTROL_PORT = 7236;
    private static final int DISCOVER_PEERS_INTERVAL_MILLIS = 10000;
    private static final String HUAWEI_WIFI_1101_P2P = "huawei.android.permission.WIFI_1101_P2P";
    private static final boolean HWFLOW;
    private static final int MAX_THROUGHPUT = 50;
    private static final int RTSP_TIMEOUT_SECONDS = 30;
    private static final int RTSP_TIMEOUT_SECONDS_CERT_MODE = 120;
    private static final String TAG = "WifiDisplayController";
    private WifiDisplay mAdvertisedDisplay;
    private int mAdvertisedDisplayFlags;
    private int mAdvertisedDisplayHeight;
    private Surface mAdvertisedDisplaySurface;
    private int mAdvertisedDisplayWidth;
    private final ArrayList<WifiP2pDevice> mAvailableWifiDisplayPeers = new ArrayList();
    private WifiP2pDevice mCancelingDevice;
    private WifiP2pDevice mConnectedDevice;
    private WifiP2pGroup mConnectedDeviceGroupInfo;
    private WifiP2pDevice mConnectingDevice;
    private int mConnectionRetriesLeft;
    private final Runnable mConnectionTimeout = new Runnable() {
        public void run() {
            if (WifiDisplayController.this.mConnectingDevice != null && WifiDisplayController.this.mConnectingDevice == WifiDisplayController.this.mDesiredDevice) {
                String str = WifiDisplayController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Timed out waiting for Wifi display connection after 30 seconds: ");
                stringBuilder.append(WifiDisplayController.this.mConnectingDevice.deviceName);
                Slog.i(str, stringBuilder.toString());
                WifiDisplayController.this.updateConnectionErrorCode(6);
                WifiDisplayController.this.handleConnectionFailure(true);
            }
        }
    };
    private final Context mContext;
    private WifiP2pDevice mDesiredDevice;
    private WifiP2pDevice mDisconnectingDevice;
    private final Runnable mDiscoverPeers = new Runnable() {
        public void run() {
            WifiDisplayController.this.tryDiscoverPeers();
        }
    };
    private boolean mDiscoverPeersInProgress;
    IHwWifiDisplayControllerEx mHWdcEx = null;
    private final Handler mHandler;
    private final Listener mListener;
    private NetworkInfo mNetworkInfo;
    private RemoteDisplay mRemoteDisplay;
    private boolean mRemoteDisplayConnected;
    private String mRemoteDisplayInterface;
    private final Runnable mRtspTimeout = new Runnable() {
        public void run() {
            if (WifiDisplayController.this.mConnectedDevice != null && WifiDisplayController.this.mRemoteDisplay != null && !WifiDisplayController.this.mRemoteDisplayConnected) {
                String str = WifiDisplayController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Timed out waiting for Wifi display RTSP connection after 30 seconds: ");
                stringBuilder.append(WifiDisplayController.this.mConnectedDevice.deviceName);
                Slog.i(str, stringBuilder.toString());
                WifiDisplayController.this.updateConnectionErrorCode(7);
                WifiDisplayController.this.handleConnectionFailure(true);
            }
        }
    };
    private boolean mScanRequested;
    private WifiP2pDevice mThisDevice;
    private int mUIBCCap = 0;
    IHwUibcReceiver mUibcInterface = null;
    private boolean mUsingUIBC = false;
    private boolean mWfdEnabled;
    private boolean mWfdEnabling;
    private boolean mWifiDisplayCertMode;
    private boolean mWifiDisplayOnSetting;
    private int mWifiDisplayWpsConfig = 4;
    private final Channel mWifiP2pChannel;
    private boolean mWifiP2pEnabled;
    private final BroadcastReceiver mWifiP2pExReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.net.wifi.p2p.WIFI_P2P_FLAG_CHANGED_ACTION".equals(intent.getAction()) && WifiDisplayControllerHisiExt.hisiWifiEnabled()) {
                boolean z = false;
                if (intent.getIntExtra("extra_p2p_flag", 0) == 1) {
                    z = true;
                }
                boolean wifiP2pFlag = z;
                String str = WifiDisplayController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("WIFI_P2P_FLAG_CHANGED_ACTION,wifiP2pFlag:");
                stringBuilder.append(wifiP2pFlag);
                Slog.d(str, stringBuilder.toString());
                WifiDisplayController.this.handleStateChanged(wifiP2pFlag);
            }
        }
    };
    private final WifiP2pManager mWifiP2pManager;
    private final BroadcastReceiver mWifiP2pReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String str;
            StringBuilder stringBuilder;
            if (action.equals("android.net.wifi.p2p.STATE_CHANGED")) {
                boolean z = true;
                if (intent.getIntExtra("wifi_p2p_state", 1) != 2) {
                    z = false;
                }
                boolean enabled = z;
                str = WifiDisplayController.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Received WIFI_P2P_STATE_CHANGED_ACTION: enabled=");
                stringBuilder.append(enabled);
                Slog.d(str, stringBuilder.toString());
                WifiDisplayController.this.handleStateChanged(enabled);
            } else if (action.equals("android.net.wifi.p2p.PEERS_CHANGED")) {
                Slog.d(WifiDisplayController.TAG, "Received WIFI_P2P_PEERS_CHANGED_ACTION.");
                WifiDisplayController.this.handlePeersChanged();
            } else if (action.equals("android.net.wifi.p2p.CONNECTION_STATE_CHANGE")) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                str = WifiDisplayController.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Received WIFI_P2P_CONNECTION_CHANGED_ACTION: networkInfo=");
                stringBuilder.append(networkInfo);
                Slog.d(str, stringBuilder.toString());
                if (networkInfo == null) {
                    Slog.e(WifiDisplayController.TAG, "networkInfo is null, return!");
                    return;
                }
                WifiDisplayController.this.handleConnectionChanged(networkInfo);
            } else if (action.equals("android.net.wifi.p2p.THIS_DEVICE_CHANGED")) {
                WifiDisplayController.this.mThisDevice = (WifiP2pDevice) intent.getParcelableExtra("wifiP2pDevice");
                String str2 = WifiDisplayController.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Received WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: mThisDevice= ");
                stringBuilder2.append(WifiDisplayController.this.mThisDevice);
                Slog.d(str2, stringBuilder2.toString());
            }
        }
    };

    public interface Listener {
        void onDisplayCasting(WifiDisplay wifiDisplay);

        void onDisplayChanged(WifiDisplay wifiDisplay);

        void onDisplayConnected(WifiDisplay wifiDisplay, Surface surface, int i, int i2, int i3);

        void onDisplayConnecting(WifiDisplay wifiDisplay);

        void onDisplayConnectionFailed();

        void onDisplayDisconnected();

        void onDisplaySessionInfo(WifiDisplaySessionInfo wifiDisplaySessionInfo);

        void onFeatureStateChanged(int i);

        void onScanFinished();

        void onScanResults(WifiDisplay[] wifiDisplayArr);

        void onScanStarted();

        void onSetConnectionFailedReason(int i);

        void onSetUibcInfo(int i);
    }

    static /* synthetic */ int access$3920(WifiDisplayController x0, int x1) {
        int i = x0.mConnectionRetriesLeft - x1;
        x0.mConnectionRetriesLeft = i;
        return i;
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    public WifiDisplayController(Context context, Handler handler, Listener listener) {
        this.mContext = context;
        this.mHandler = handler;
        this.mListener = listener;
        this.mWifiP2pManager = (WifiP2pManager) context.getSystemService("wifip2p");
        this.mWifiP2pChannel = this.mWifiP2pManager.initialize(context, handler.getLooper(), null);
        this.mHWdcEx = HwServiceFactory.getHwWifiDisplayControllerEx(this, context, handler);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.p2p.STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.p2p.PEERS_CHANGED");
        intentFilter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        intentFilter.addAction("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
        context.registerReceiver(this.mWifiP2pReceiver, intentFilter, null, this.mHandler);
        context.registerReceiver(this.mWifiP2pExReceiver, new IntentFilter("android.net.wifi.p2p.WIFI_P2P_FLAG_CHANGED_ACTION"), HUAWEI_WIFI_1101_P2P, null);
        ContentObserver settingsObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange, Uri uri) {
                WifiDisplayController.this.updateSettings();
            }
        };
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Global.getUriFor("wifi_display_on"), false, settingsObserver);
        resolver.registerContentObserver(Global.getUriFor("wifi_display_certification_on"), false, settingsObserver);
        resolver.registerContentObserver(Global.getUriFor("wifi_display_wps_config"), false, settingsObserver);
        updateSettings();
    }

    private void updateSettings() {
        ContentResolver resolver = this.mContext.getContentResolver();
        boolean z = false;
        this.mWifiDisplayOnSetting = Global.getInt(resolver, "wifi_display_on", 0) != 0;
        if (Global.getInt(resolver, "wifi_display_certification_on", 0) != 0) {
            z = true;
        }
        this.mWifiDisplayCertMode = z;
        this.mWifiDisplayWpsConfig = 4;
        if (this.mWifiDisplayCertMode) {
            this.mWifiDisplayWpsConfig = Global.getInt(resolver, "wifi_display_wps_config", 4);
        }
        updateWfdEnableState();
    }

    public void dump(PrintWriter pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mWifiDisplayOnSetting=");
        stringBuilder.append(this.mWifiDisplayOnSetting);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mWifiP2pEnabled=");
        stringBuilder.append(this.mWifiP2pEnabled);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mWfdEnabled=");
        stringBuilder.append(this.mWfdEnabled);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mWfdEnabling=");
        stringBuilder.append(this.mWfdEnabling);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mNetworkInfo=");
        stringBuilder.append(this.mNetworkInfo);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mScanRequested=");
        stringBuilder.append(this.mScanRequested);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDiscoverPeersInProgress=");
        stringBuilder.append(this.mDiscoverPeersInProgress);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDesiredDevice=");
        stringBuilder.append(describeWifiP2pDevice(this.mDesiredDevice));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mConnectingDisplay=");
        stringBuilder.append(describeWifiP2pDevice(this.mConnectingDevice));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mDisconnectingDisplay=");
        stringBuilder.append(describeWifiP2pDevice(this.mDisconnectingDevice));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCancelingDisplay=");
        stringBuilder.append(describeWifiP2pDevice(this.mCancelingDevice));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mConnectedDevice=");
        stringBuilder.append(describeWifiP2pDevice(this.mConnectedDevice));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mConnectionRetriesLeft=");
        stringBuilder.append(this.mConnectionRetriesLeft);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mRemoteDisplay=");
        stringBuilder.append(this.mRemoteDisplay);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mRemoteDisplayInterface=");
        stringBuilder.append(this.mRemoteDisplayInterface);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mRemoteDisplayConnected=");
        stringBuilder.append(this.mRemoteDisplayConnected);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mAdvertisedDisplay=");
        stringBuilder.append(this.mAdvertisedDisplay);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mAdvertisedDisplaySurface=");
        stringBuilder.append(this.mAdvertisedDisplaySurface);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mAdvertisedDisplayWidth=");
        stringBuilder.append(this.mAdvertisedDisplayWidth);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mAdvertisedDisplayHeight=");
        stringBuilder.append(this.mAdvertisedDisplayHeight);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mAdvertisedDisplayFlags=");
        stringBuilder.append(this.mAdvertisedDisplayFlags);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mAvailableWifiDisplayPeers: size=");
        stringBuilder.append(this.mAvailableWifiDisplayPeers.size());
        pw.println(stringBuilder.toString());
        Iterator it = this.mAvailableWifiDisplayPeers.iterator();
        while (it.hasNext()) {
            WifiP2pDevice device = (WifiP2pDevice) it.next();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  ");
            stringBuilder2.append(describeWifiP2pDevice(device));
            pw.println(stringBuilder2.toString());
        }
    }

    public void requestStartScan() {
        if (!this.mScanRequested) {
            this.mScanRequested = true;
            updateScanState();
        }
    }

    public void requestStopScan() {
        if (this.mScanRequested) {
            this.mScanRequested = false;
            updateScanState();
        }
    }

    public void requestConnect(String address) {
        Iterator it = this.mAvailableWifiDisplayPeers.iterator();
        while (it.hasNext()) {
            WifiP2pDevice device = (WifiP2pDevice) it.next();
            if (device.deviceAddress.equals(address)) {
                connect(device);
            }
        }
    }

    public void requestPause() {
        if (this.mRemoteDisplay != null) {
            this.mRemoteDisplay.pause();
        }
    }

    public void requestResume() {
        if (this.mRemoteDisplay != null) {
            this.mRemoteDisplay.resume();
        }
    }

    public void requestDisconnect() {
        disconnect();
    }

    private void updateWfdEnableState() {
        if (!this.mWifiDisplayOnSetting || !this.mWifiP2pEnabled) {
            if (this.mWfdEnabled || this.mWfdEnabling) {
                WifiP2pWfdInfo wfdInfo = new WifiP2pWfdInfo();
                wfdInfo.setWfdEnabled(false);
                this.mWifiP2pManager.setWFDInfo(this.mWifiP2pChannel, wfdInfo, new ActionListener() {
                    public void onSuccess() {
                        Slog.d(WifiDisplayController.TAG, "Successfully set WFD info.");
                    }

                    public void onFailure(int reason) {
                        String str = WifiDisplayController.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to set WFD info with reason ");
                        stringBuilder.append(reason);
                        stringBuilder.append(".");
                        Slog.d(str, stringBuilder.toString());
                    }
                });
            }
            this.mWfdEnabling = false;
            this.mWfdEnabled = false;
            reportFeatureState();
            updateScanState();
            disconnect();
        } else if (!this.mWfdEnabled && !this.mWfdEnabling) {
            this.mWfdEnabling = true;
            WifiP2pWfdInfo wfdInfo2 = new WifiP2pWfdInfo();
            wfdInfo2.setWfdEnabled(true);
            wfdInfo2.setDeviceType(0);
            wfdInfo2.setSessionAvailable(true);
            wfdInfo2.setControlPort(DEFAULT_CONTROL_PORT);
            wfdInfo2.setMaxThroughput(50);
            this.mWifiP2pManager.setWFDInfo(this.mWifiP2pChannel, wfdInfo2, new ActionListener() {
                public void onSuccess() {
                    Slog.d(WifiDisplayController.TAG, "Successfully set WFD info.");
                    if (WifiDisplayController.this.mWfdEnabling) {
                        WifiDisplayController.this.mWfdEnabling = false;
                        WifiDisplayController.this.mWfdEnabled = true;
                        WifiDisplayController.this.reportFeatureState();
                        WifiDisplayController.this.updateScanState();
                    }
                }

                public void onFailure(int reason) {
                    String str = WifiDisplayController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to set WFD info with reason ");
                    stringBuilder.append(reason);
                    stringBuilder.append(".");
                    Slog.d(str, stringBuilder.toString());
                    WifiDisplayController.this.mWfdEnabling = false;
                }
            });
        }
    }

    private void reportFeatureState() {
        final int featureState = computeFeatureState();
        this.mHandler.post(new Runnable() {
            public void run() {
                WifiDisplayController.this.mListener.onFeatureStateChanged(featureState);
            }
        });
    }

    private int computeFeatureState() {
        if (!this.mWifiP2pEnabled) {
            return 1;
        }
        int i;
        if (this.mWifiDisplayOnSetting) {
            i = 3;
        } else {
            i = 2;
        }
        return i;
    }

    private void updateScanState() {
        String str;
        StringBuilder stringBuilder;
        if (HWFLOW) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateScanState mScanRequested=");
            stringBuilder.append(this.mScanRequested);
            stringBuilder.append(" updateScanState mWfdEnabled=");
            stringBuilder.append(this.mWfdEnabled);
            Slog.i(str, stringBuilder.toString());
        }
        if (HWFLOW) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mDiscoverPeersInProgress=");
            stringBuilder.append(this.mDiscoverPeersInProgress);
            stringBuilder.append(" mDesiredDevice=");
            stringBuilder.append(this.mDesiredDevice);
            Slog.i(str, stringBuilder.toString());
        }
        if (this.mScanRequested && this.mWfdEnabled && (this.mDesiredDevice == null || IHwWifiDisplayControllerEx.ENABLED_PC)) {
            if (!this.mDiscoverPeersInProgress) {
                Slog.i(TAG, "Starting Wifi display scan.");
                this.mDiscoverPeersInProgress = true;
                handleScanStarted();
                tryDiscoverPeers();
            }
        } else if (this.mDiscoverPeersInProgress) {
            this.mHandler.removeCallbacks(this.mDiscoverPeers);
            if (this.mDesiredDevice == null || this.mDesiredDevice == this.mConnectedDevice) {
                Slog.i(TAG, "Stopping Wifi display scan.");
                this.mDiscoverPeersInProgress = false;
                stopPeerDiscovery();
                handleScanFinished();
            }
        }
    }

    private void tryDiscoverPeers() {
        if (this.mHWdcEx == null || !this.mHWdcEx.tryDiscoverPeersEx()) {
            this.mWifiP2pManager.discoverPeers(this.mWifiP2pChannel, new ActionListener() {
                public void onSuccess() {
                    Slog.d(WifiDisplayController.TAG, "Discover peers succeeded.  Requesting peers now.");
                    if (WifiDisplayController.this.mDiscoverPeersInProgress) {
                        WifiDisplayController.this.requestPeers();
                    }
                }

                public void onFailure(int reason) {
                    String str = WifiDisplayController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Discover peers failed with reason ");
                    stringBuilder.append(reason);
                    stringBuilder.append(".");
                    Slog.d(str, stringBuilder.toString());
                }
            });
            this.mHandler.postDelayed(this.mDiscoverPeers, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
            return;
        }
        Slog.d(TAG, "used tryDiscoverPeersEx discover peers.");
    }

    private void stopPeerDiscovery() {
        this.mWifiP2pManager.stopPeerDiscovery(this.mWifiP2pChannel, new ActionListener() {
            public void onSuccess() {
                Slog.d(WifiDisplayController.TAG, "Stop peer discovery succeeded.");
            }

            public void onFailure(int reason) {
                String str = WifiDisplayController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Stop peer discovery failed with reason ");
                stringBuilder.append(reason);
                stringBuilder.append(".");
                Slog.d(str, stringBuilder.toString());
            }
        });
    }

    private void requestPeers() {
        this.mWifiP2pManager.requestPeers(this.mWifiP2pChannel, new PeerListListener() {
            public void onPeersAvailable(WifiP2pDeviceList peers) {
                Slog.d(WifiDisplayController.TAG, "Received list of peers.");
                WifiDisplayController.this.mAvailableWifiDisplayPeers.clear();
                for (WifiP2pDevice device : peers.getDeviceList()) {
                    String str = WifiDisplayController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  ");
                    stringBuilder.append(WifiDisplayController.describeWifiP2pDevice(device));
                    Slog.d(str, stringBuilder.toString());
                    if (WifiDisplayController.isWifiDisplay(device)) {
                        WifiDisplayController.this.mAvailableWifiDisplayPeers.add(device);
                    }
                }
                if (WifiDisplayController.this.mDiscoverPeersInProgress) {
                    WifiDisplayController.this.handleScanResults();
                }
            }
        });
    }

    private void handleScanStarted() {
        this.mHandler.post(new Runnable() {
            public void run() {
                WifiDisplayController.this.mListener.onScanStarted();
            }
        });
    }

    private void handleScanResults() {
        int count = this.mAvailableWifiDisplayPeers.size();
        final WifiDisplay[] displays = (WifiDisplay[]) WifiDisplay.CREATOR.newArray(count);
        for (int i = 0; i < count; i++) {
            WifiP2pDevice device = (WifiP2pDevice) this.mAvailableWifiDisplayPeers.get(i);
            displays[i] = createWifiDisplay(device);
            updateDesiredDevice(device);
        }
        this.mHandler.post(new Runnable() {
            public void run() {
                WifiDisplayController.this.mListener.onScanResults(displays);
            }
        });
    }

    private void handleScanFinished() {
        this.mHandler.post(new Runnable() {
            public void run() {
                WifiDisplayController.this.mListener.onScanFinished();
            }
        });
    }

    private void updateDesiredDevice(WifiP2pDevice device) {
        String address = device.deviceAddress;
        if (this.mDesiredDevice != null && this.mDesiredDevice.deviceAddress.equals(address)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateDesiredDevice: new information ");
            stringBuilder.append(describeWifiP2pDevice(device));
            Slog.d(str, stringBuilder.toString());
            this.mDesiredDevice.update(device);
            if (this.mAdvertisedDisplay != null && this.mAdvertisedDisplay.getDeviceAddress().equals(address)) {
                readvertiseDisplay(createWifiDisplay(this.mDesiredDevice));
            }
        }
    }

    private void connect(WifiP2pDevice device) {
        String str;
        StringBuilder stringBuilder;
        if (this.mDesiredDevice != null && !this.mDesiredDevice.deviceAddress.equals(device.deviceAddress)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("connect: nothing to do, already connecting to ");
            stringBuilder.append(describeWifiP2pDevice(device));
            Slog.d(str, stringBuilder.toString());
        } else if (this.mConnectedDevice != null && !this.mConnectedDevice.deviceAddress.equals(device.deviceAddress) && this.mDesiredDevice == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("connect: nothing to do, already connected to ");
            stringBuilder.append(describeWifiP2pDevice(device));
            stringBuilder.append(" and not part way through connecting to a different device.");
            Slog.d(str, stringBuilder.toString());
        } else if (this.mWfdEnabled) {
            this.mDesiredDevice = device;
            this.mConnectionRetriesLeft = 3;
            if (this.mHWdcEx != null) {
                this.mHWdcEx.resetDisplayParameters();
            }
            updateConnection();
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring request to connect to Wifi display because the  feature is currently disabled: ");
            stringBuilder.append(device.deviceName);
            Slog.i(str, stringBuilder.toString());
        }
    }

    private void disconnect() {
        this.mDesiredDevice = null;
        if (this.mUsingUIBC && this.mUibcInterface != null) {
            this.mUibcInterface.destroyReceiver();
            HwServiceFactory.clearHwUibcReceiver();
            this.mUibcInterface = null;
            this.mUIBCCap = 0;
        }
        updateConnection();
    }

    private void retryConnection() {
        this.mDesiredDevice = new WifiP2pDevice(this.mDesiredDevice);
        updateConnection();
    }

    private void updateConnection() {
        String str;
        StringBuilder stringBuilder;
        updateScanState();
        if (!(this.mRemoteDisplay == null || this.mConnectedDevice == this.mDesiredDevice)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Stopped listening for RTSP connection on ");
            stringBuilder.append(this.mRemoteDisplayInterface);
            stringBuilder.append(" from Wifi display: ");
            stringBuilder.append(this.mConnectedDevice.deviceName);
            Slog.i(str, stringBuilder.toString());
            this.mRemoteDisplay.dispose();
            this.mRemoteDisplay = null;
            this.mRemoteDisplayInterface = null;
            this.mRemoteDisplayConnected = false;
            this.mHandler.removeCallbacks(this.mRtspTimeout);
            this.mWifiP2pManager.setMiracastMode(0);
            unadvertiseDisplay();
            unregisterPGStateEvent();
        }
        if (this.mDisconnectingDevice == null) {
            StringBuilder stringBuilder2;
            final WifiP2pDevice oldDevice;
            if (this.mConnectedDevice != null && this.mConnectedDevice != this.mDesiredDevice) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Disconnecting from Wifi display: ");
                stringBuilder2.append(this.mConnectedDevice.deviceName);
                Slog.i(str, stringBuilder2.toString());
                this.mDisconnectingDevice = this.mConnectedDevice;
                this.mConnectedDevice = null;
                this.mConnectedDeviceGroupInfo = null;
                unadvertiseDisplay();
                oldDevice = this.mDisconnectingDevice;
                this.mWifiP2pManager.removeGroup(this.mWifiP2pChannel, new ActionListener() {
                    public void onSuccess() {
                        String str = WifiDisplayController.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Disconnected from Wifi display: ");
                        stringBuilder.append(oldDevice.deviceName);
                        Slog.i(str, stringBuilder.toString());
                        next();
                    }

                    public void onFailure(int reason) {
                        String str = WifiDisplayController.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to disconnect from Wifi display: ");
                        stringBuilder.append(oldDevice.deviceName);
                        stringBuilder.append(", reason=");
                        stringBuilder.append(reason);
                        Slog.i(str, stringBuilder.toString());
                        next();
                    }

                    private void next() {
                        if (WifiDisplayController.this.mDisconnectingDevice == oldDevice) {
                            WifiDisplayController.this.mDisconnectingDevice = null;
                            WifiDisplayController.this.updateConnection();
                        }
                    }
                });
            } else if (this.mCancelingDevice == null) {
                if (this.mConnectingDevice != null && this.mConnectingDevice != this.mDesiredDevice) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Canceling connection to Wifi display: ");
                    stringBuilder2.append(this.mConnectingDevice.deviceName);
                    Slog.i(str, stringBuilder2.toString());
                    this.mCancelingDevice = this.mConnectingDevice;
                    this.mConnectingDevice = null;
                    unadvertiseDisplay();
                    this.mHandler.removeCallbacks(this.mConnectionTimeout);
                    oldDevice = this.mCancelingDevice;
                    this.mWifiP2pManager.cancelConnect(this.mWifiP2pChannel, new ActionListener() {
                        public void onSuccess() {
                            String str = WifiDisplayController.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Canceled connection to Wifi display: ");
                            stringBuilder.append(oldDevice.deviceName);
                            Slog.i(str, stringBuilder.toString());
                            next();
                            WifiDisplayController.this.unregisterPGStateEvent();
                        }

                        public void onFailure(int reason) {
                            String str = WifiDisplayController.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to cancel connection to Wifi display: ");
                            stringBuilder.append(oldDevice.deviceName);
                            stringBuilder.append(", reason=");
                            stringBuilder.append(reason);
                            Slog.i(str, stringBuilder.toString());
                            next();
                        }

                        private void next() {
                            if (WifiDisplayController.this.mCancelingDevice == oldDevice) {
                                WifiDisplayController.this.mCancelingDevice = null;
                                WifiDisplayController.this.updateConnection();
                            }
                        }
                    });
                } else if (this.mDesiredDevice == null) {
                    if (this.mWifiDisplayCertMode) {
                        this.mListener.onDisplaySessionInfo(getSessionInfo(this.mConnectedDeviceGroupInfo, 0));
                    }
                    unadvertiseDisplay();
                } else if (this.mConnectedDevice == null && this.mConnectingDevice == null) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Connecting to Wifi display: ");
                    stringBuilder.append(this.mDesiredDevice.deviceName);
                    Slog.i(str, stringBuilder.toString());
                    this.mConnectingDevice = this.mDesiredDevice;
                    WifiP2pConfig config = new WifiP2pConfig();
                    WpsInfo wps = new WpsInfo();
                    if (this.mWifiDisplayWpsConfig != 4) {
                        wps.setup = this.mWifiDisplayWpsConfig;
                    } else if (this.mConnectingDevice.wpsPbcSupported()) {
                        wps.setup = 0;
                    } else if (this.mConnectingDevice.wpsDisplaySupported()) {
                        wps.setup = 2;
                    } else {
                        wps.setup = 1;
                    }
                    config.wps = wps;
                    config.deviceAddress = this.mConnectingDevice.deviceAddress;
                    config.groupOwnerIntent = 14;
                    advertiseDisplay(createWifiDisplay(this.mConnectingDevice), null, 0, 0, 0);
                    final WifiP2pDevice newDevice = this.mDesiredDevice;
                    this.mWifiP2pManager.connect(this.mWifiP2pChannel, config, new ActionListener() {
                        public void onSuccess() {
                            String str = WifiDisplayController.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Initiated connection to Wifi display: ");
                            stringBuilder.append(newDevice.deviceName);
                            Slog.i(str, stringBuilder.toString());
                            WifiDisplayController.this.mHandler.postDelayed(WifiDisplayController.this.mConnectionTimeout, 30000);
                        }

                        public void onFailure(int reason) {
                            if (WifiDisplayController.this.mConnectingDevice == newDevice) {
                                String str = WifiDisplayController.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Failed to initiate connection to Wifi display: ");
                                stringBuilder.append(newDevice.deviceName);
                                stringBuilder.append(", reason=");
                                stringBuilder.append(reason);
                                Slog.i(str, stringBuilder.toString());
                                WifiDisplayController.this.mConnectingDevice = null;
                                WifiDisplayController.this.updateConnectionErrorCode(reason);
                                WifiDisplayController.this.handleConnectionFailure(false);
                            }
                        }
                    });
                } else {
                    if (this.mConnectedDevice != null && this.mRemoteDisplay == null) {
                        Inet4Address addr = getInterfaceAddress(this.mConnectedDeviceGroupInfo);
                        if (addr == null) {
                            String str2 = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Failed to get local interface address for communicating with Wifi display: ");
                            stringBuilder.append(this.mConnectedDevice.deviceName);
                            Slog.i(str2, stringBuilder.toString());
                            updateConnectionErrorCode(3);
                            handleConnectionFailure(false);
                            return;
                        }
                        this.mWifiP2pManager.setMiracastMode(1);
                        final WifiP2pDevice oldDevice2 = this.mConnectedDevice;
                        int port = getPortNumber(this.mConnectedDevice);
                        String iface = new StringBuilder();
                        iface.append(addr.getHostAddress());
                        iface.append(":");
                        iface.append(port);
                        iface = iface.toString();
                        this.mRemoteDisplayInterface = iface;
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Listening for RTSP connection on ");
                        stringBuilder3.append(iface);
                        stringBuilder3.append(" from Wifi display: ");
                        stringBuilder3.append(this.mConnectedDevice.deviceName);
                        Slog.i(str3, stringBuilder3.toString());
                        this.mRemoteDisplay = RemoteDisplay.listen(iface, new android.media.RemoteDisplay.Listener() {
                            public void onDisplayConnected(Surface surface, int width, int height, int flags, int session) {
                                if (WifiDisplayController.this.mConnectedDevice == oldDevice2 && !WifiDisplayController.this.mRemoteDisplayConnected) {
                                    String str = WifiDisplayController.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Opened RTSP connection with Wifi display: ");
                                    stringBuilder.append(WifiDisplayController.this.mConnectedDevice.deviceName);
                                    Slog.i(str, stringBuilder.toString());
                                    WifiDisplayController.this.mRemoteDisplayConnected = true;
                                    WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mRtspTimeout);
                                    if (WifiDisplayController.this.mWifiDisplayCertMode) {
                                        WifiDisplayController.this.mListener.onDisplaySessionInfo(WifiDisplayController.this.getSessionInfo(WifiDisplayController.this.mConnectedDeviceGroupInfo, session));
                                    }
                                    WifiDisplayController.this.advertiseDisplay(WifiDisplayController.createWifiDisplay(WifiDisplayController.this.mConnectedDevice), surface, width, height, flags);
                                    if (WifiDisplayController.this.mHWdcEx != null) {
                                        WifiDisplayController.this.mHWdcEx.setVideoBitrate();
                                        WifiDisplayController.this.mHWdcEx.registerPGStateEvent();
                                    }
                                    if (WifiDisplayController.this.mUibcInterface != null) {
                                        WifiDisplayController.this.mUibcInterface.setRemoteScreenSize(width, height);
                                    }
                                }
                            }

                            public void onDisplayDisconnected() {
                                if (WifiDisplayController.this.mConnectedDevice == oldDevice2) {
                                    String str = WifiDisplayController.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Closed RTSP connection with Wifi display: ");
                                    stringBuilder.append(WifiDisplayController.this.mConnectedDevice.deviceName);
                                    Slog.i(str, stringBuilder.toString());
                                    WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mRtspTimeout);
                                    WifiDisplayController.this.disconnect();
                                    WifiDisplayController.this.unregisterPGStateEvent();
                                }
                            }

                            public void onDisplayError(int error) {
                                if (WifiDisplayController.this.mConnectedDevice == oldDevice2) {
                                    if (50 == error) {
                                        if (WifiDisplayController.this.mHWdcEx != null) {
                                            WifiDisplayController.this.mHWdcEx.advertisDisplayCasting(WifiDisplayController.this.mAdvertisedDisplay);
                                        }
                                        return;
                                    }
                                    String str = WifiDisplayController.TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("Lost RTSP connection with Wifi display due to error ");
                                    stringBuilder.append(error);
                                    stringBuilder.append(": ");
                                    stringBuilder.append(WifiDisplayController.this.mConnectedDevice.deviceName);
                                    Slog.i(str, stringBuilder.toString());
                                    WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mRtspTimeout);
                                    WifiDisplayController.this.updateConnectionErrorCode(4);
                                    WifiDisplayController.this.handleConnectionFailure(false);
                                    WifiDisplayController.this.unregisterPGStateEvent();
                                }
                            }

                            public int notifyUibcCreate(int capSupport) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Create and Start UIBC Receiver : cap ");
                                stringBuilder.append(capSupport);
                                Log.i("UIBC", stringBuilder.toString());
                                WifiDisplayController.this.mUibcInterface = HwServiceFactory.getHwUibcReceiver();
                                if (WifiDisplayController.this.mUibcInterface == null) {
                                    return -1;
                                }
                                int port = WifiDisplayController.this.mUibcInterface.createReceiver(WifiDisplayController.this.mContext, WifiDisplayController.this.mHandler);
                                if (!(port == -1 || WifiDisplayController.this.mUibcInterface.getAcceptCheck() == null)) {
                                    WifiDisplayController.this.mUibcInterface.setRemoteMacAddress(WifiDisplayController.this.mConnectedDevice.deviceAddress);
                                    WifiDisplayController.this.mUibcInterface.startReceiver();
                                    WifiDisplayController.this.mHandler.postDelayed(WifiDisplayController.this.mUibcInterface.getAcceptCheck(), 16000);
                                    WifiDisplayController.this.mUsingUIBC = true;
                                    WifiDisplayController.this.mUIBCCap = capSupport;
                                }
                                return port;
                            }
                        }, this.mHandler, this.mContext.getOpPackageName());
                        if (this.mHWdcEx != null) {
                            this.mHWdcEx.setDisplayParameters();
                        }
                        this.mHandler.postDelayed(this.mRtspTimeout, (long) ((this.mWifiDisplayCertMode ? RTSP_TIMEOUT_SECONDS_CERT_MODE : 30) * 1000));
                    }
                }
            }
        }
    }

    private WifiDisplaySessionInfo getSessionInfo(WifiP2pGroup info, int session) {
        if (info == null) {
            return null;
        }
        Inet4Address addr = getInterfaceAddress(info);
        int equals = info.getOwner().deviceAddress.equals(this.mThisDevice.deviceAddress) ^ 1;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(info.getOwner().deviceAddress);
        stringBuilder.append(" ");
        stringBuilder.append(info.getNetworkName());
        WifiDisplaySessionInfo sessionInfo = new WifiDisplaySessionInfo(equals, session, stringBuilder.toString(), info.getPassphrase(), addr != null ? addr.getHostAddress() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        Slog.d(TAG, sessionInfo.toString());
        return sessionInfo;
    }

    private void handleStateChanged(boolean enabled) {
        this.mWifiP2pEnabled = enabled;
        updateWfdEnableState();
    }

    private void handlePeersChanged() {
        requestPeers();
    }

    private void handleConnectionChanged(NetworkInfo networkInfo) {
        this.mNetworkInfo = networkInfo;
        if (!this.mWfdEnabled || !networkInfo.isConnected()) {
            this.mConnectedDeviceGroupInfo = null;
            if (!(this.mConnectingDevice == null && this.mConnectedDevice == null)) {
                disconnect();
            }
            if (this.mWfdEnabled) {
                requestPeers();
            }
        } else if (this.mDesiredDevice != null || this.mWifiDisplayCertMode) {
            this.mWifiP2pManager.requestGroupInfo(this.mWifiP2pChannel, new GroupInfoListener() {
                public void onGroupInfoAvailable(WifiP2pGroup info) {
                    String str = WifiDisplayController.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Received group info: ");
                    stringBuilder.append(WifiDisplayController.describeWifiP2pGroup(info));
                    Slog.d(str, stringBuilder.toString());
                    if (info == null) {
                        Slog.e(WifiDisplayController.TAG, "onGroupInfoAvailable info is null");
                    } else if (WifiDisplayController.this.mConnectingDevice != null && !info.contains(WifiDisplayController.this.mConnectingDevice)) {
                        str = WifiDisplayController.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Aborting connection to Wifi display because the current P2P group does not contain the device we expected to find: ");
                        stringBuilder.append(WifiDisplayController.this.mConnectingDevice.deviceName);
                        stringBuilder.append(", group info was: ");
                        stringBuilder.append(WifiDisplayController.describeWifiP2pGroup(info));
                        Slog.i(str, stringBuilder.toString());
                        WifiDisplayController.this.updateConnectionErrorCode(5);
                        WifiDisplayController.this.handleConnectionFailure(false);
                    } else if (WifiDisplayController.this.mDesiredDevice == null || info.contains(WifiDisplayController.this.mDesiredDevice)) {
                        if (WifiDisplayController.this.mWifiDisplayCertMode) {
                            boolean owner = info.getOwner().deviceAddress.equals(WifiDisplayController.this.mThisDevice.deviceAddress);
                            if (owner && info.getClientList().isEmpty()) {
                                WifiDisplayController.this.mConnectingDevice = WifiDisplayController.this.mDesiredDevice = null;
                                WifiDisplayController.this.mConnectedDeviceGroupInfo = info;
                                WifiDisplayController.this.updateConnection();
                            } else if (WifiDisplayController.this.mConnectingDevice == null && WifiDisplayController.this.mDesiredDevice == null) {
                                WifiDisplayController.this.mConnectingDevice = WifiDisplayController.this.mDesiredDevice = owner ? (WifiP2pDevice) info.getClientList().iterator().next() : info.getOwner();
                            }
                        }
                        if (WifiDisplayController.this.mConnectingDevice != null && WifiDisplayController.this.mConnectingDevice == WifiDisplayController.this.mDesiredDevice) {
                            str = WifiDisplayController.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Connected to Wifi display: ");
                            stringBuilder2.append(WifiDisplayController.this.mConnectingDevice.deviceName);
                            Slog.i(str, stringBuilder2.toString());
                            if (WifiDisplayController.this.mHWdcEx != null) {
                                WifiDisplayController.this.mHWdcEx.setWorkFrequence(info.getFrequence());
                            }
                            WifiDisplayController.this.mHandler.removeCallbacks(WifiDisplayController.this.mConnectionTimeout);
                            WifiDisplayController.this.mConnectedDeviceGroupInfo = info;
                            WifiDisplayController.this.mConnectedDevice = WifiDisplayController.this.mConnectingDevice;
                            WifiDisplayController.this.mConnectingDevice = null;
                            WifiDisplayController.this.updateConnection();
                        }
                    } else {
                        WifiDisplayController.this.disconnect();
                    }
                }
            });
        }
    }

    private void handleConnectionFailure(boolean timeoutOccurred) {
        Slog.i(TAG, "Wifi display connection failed!");
        if (this.mDesiredDevice == null) {
            return;
        }
        if (this.mConnectionRetriesLeft > 0) {
            final WifiP2pDevice oldDevice = this.mDesiredDevice;
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (WifiDisplayController.this.mDesiredDevice == oldDevice && WifiDisplayController.this.mConnectionRetriesLeft > 0) {
                        WifiDisplayController.access$3920(WifiDisplayController.this, 1);
                        String str = WifiDisplayController.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Retrying Wifi display connection.  Retries left: ");
                        stringBuilder.append(WifiDisplayController.this.mConnectionRetriesLeft);
                        Slog.i(str, stringBuilder.toString());
                        WifiDisplayController.this.retryConnection();
                    }
                }
            }, timeoutOccurred ? 0 : 500);
            return;
        }
        disconnect();
    }

    private void advertiseDisplay(WifiDisplay display, Surface surface, int width, int height, int flags) {
        WifiDisplay wifiDisplay = display;
        Surface surface2 = surface;
        int i = width;
        int i2 = height;
        int i3 = flags;
        if (!Objects.equals(this.mAdvertisedDisplay, wifiDisplay) || this.mAdvertisedDisplaySurface != surface2 || this.mAdvertisedDisplayWidth != i || this.mAdvertisedDisplayHeight != i2 || this.mAdvertisedDisplayFlags != i3) {
            WifiDisplay oldDisplay = this.mAdvertisedDisplay;
            Surface oldSurface = this.mAdvertisedDisplaySurface;
            this.mAdvertisedDisplay = wifiDisplay;
            this.mAdvertisedDisplaySurface = surface2;
            this.mAdvertisedDisplayWidth = i;
            this.mAdvertisedDisplayHeight = i2;
            this.mAdvertisedDisplayFlags = i3;
            Handler handler = this.mHandler;
            final Surface surface3 = oldSurface;
            final Surface surface4 = surface2;
            final WifiDisplay wifiDisplay2 = oldDisplay;
            final WifiDisplay wifiDisplay3 = wifiDisplay;
            AnonymousClass20 anonymousClass20 = r0;
            final int i4 = i;
            Handler handler2 = handler;
            final int i5 = i2;
            oldSurface = i3;
            AnonymousClass20 anonymousClass202 = new Runnable() {
                public void run() {
                    if (surface3 != null && surface4 != surface3) {
                        WifiDisplayController.this.mListener.onDisplayDisconnected();
                    } else if (!(wifiDisplay2 == null || wifiDisplay2.hasSameAddress(wifiDisplay3))) {
                        if (WifiDisplayController.this.mHWdcEx != null) {
                            WifiDisplayController.this.mListener.onSetConnectionFailedReason(WifiDisplayController.this.mHWdcEx.getConnectionErrorCode());
                        }
                        WifiDisplayController.this.updateConnectionErrorCode(-1);
                        WifiDisplayController.this.mListener.onDisplayConnectionFailed();
                    }
                    if (wifiDisplay3 != null) {
                        if (!wifiDisplay3.hasSameAddress(wifiDisplay2)) {
                            WifiDisplayController.this.mListener.onDisplayConnecting(wifiDisplay3);
                        } else if (!wifiDisplay3.equals(wifiDisplay2)) {
                            WifiDisplayController.this.mListener.onDisplayChanged(wifiDisplay3);
                        }
                        if (surface4 != null && surface4 != surface3) {
                            WifiDisplayController.this.mListener.onSetUibcInfo(WifiDisplayController.this.mUIBCCap);
                            WifiDisplayController.this.mListener.onDisplayConnected(wifiDisplay3, surface4, i4, i5, oldSurface);
                        }
                    }
                }
            };
            handler2.post(anonymousClass20);
        }
    }

    private void unadvertiseDisplay() {
        advertiseDisplay(null, null, 0, 0, 0);
    }

    private void readvertiseDisplay(WifiDisplay display) {
        advertiseDisplay(display, this.mAdvertisedDisplaySurface, this.mAdvertisedDisplayWidth, this.mAdvertisedDisplayHeight, this.mAdvertisedDisplayFlags);
    }

    private static Inet4Address getInterfaceAddress(WifiP2pGroup info) {
        NetworkInterface iface;
        String str;
        StringBuilder stringBuilder;
        if (WifiDisplayControllerHisiExt.hisiWifiEnabled()) {
            try {
                iface = NetworkInterface.getByName(WifiDisplayControllerHisiExt.getHisiWifiInface());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("get InterfaceAddress from network interface ");
                stringBuilder.append(WifiDisplayControllerHisiExt.getHisiWifiInface());
                Slog.w(str, stringBuilder.toString());
            } catch (SocketException ex) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Could not obtain address of network interface ");
                stringBuilder.append(WifiDisplayControllerHisiExt.getHisiWifiInface());
                Slog.w(str, stringBuilder.toString(), ex);
                return null;
            }
        }
        try {
            iface = NetworkInterface.getByName(info.getInterface());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get InterfaceAddress from network interface ");
            stringBuilder.append(info.getInterface());
            Slog.w(str, stringBuilder.toString());
        } catch (SocketException ex2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not obtain address of network interface ");
            stringBuilder.append(info.getInterface());
            Slog.w(str, stringBuilder.toString(), ex2);
            return null;
        }
        Enumeration<InetAddress> addrs = iface.getInetAddresses();
        while (addrs.hasMoreElements()) {
            InetAddress addr = (InetAddress) addrs.nextElement();
            if (addr instanceof Inet4Address) {
                return (Inet4Address) addr;
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Could not obtain address of network interface ");
        stringBuilder2.append(info.getInterface());
        stringBuilder2.append(" because it had no IPv4 addresses.");
        Slog.w(str2, stringBuilder2.toString());
        return null;
    }

    private static int getPortNumber(WifiP2pDevice device) {
        if (device.deviceName.startsWith("DIRECT-") && device.deviceName.endsWith("Broadcom")) {
            return 8554;
        }
        return DEFAULT_CONTROL_PORT;
    }

    private static boolean isWifiDisplay(WifiP2pDevice device) {
        return device.wfdInfo != null && device.wfdInfo.isWfdEnabled() && isPrimarySinkDeviceType(device.wfdInfo.getDeviceType());
    }

    private static boolean isPrimarySinkDeviceType(int deviceType) {
        return deviceType == 1 || deviceType == 3;
    }

    private static String describeWifiP2pDevice(WifiP2pDevice device) {
        return device != null ? device.toString().replace(10, ',') : "null";
    }

    private static String describeWifiP2pGroup(WifiP2pGroup group) {
        return group != null ? group.toString().replace(10, ',') : "null";
    }

    private static WifiDisplay createWifiDisplay(WifiP2pDevice device) {
        return new WifiDisplay(device.deviceAddress, device.deviceName, null, true, device.wfdInfo.isSessionAvailable(), false);
    }

    public RemoteDisplay getmRemoteDisplay() {
        return this.mRemoteDisplay;
    }

    public Listener getmListener() {
        return this.mListener;
    }

    public void requestStartScanInner() {
        requestStartScan();
    }

    public WifiP2pManager getWifiP2pManagerInner() {
        return this.mWifiP2pManager;
    }

    public Channel getWifiP2pChannelInner() {
        return this.mWifiP2pChannel;
    }

    public boolean getmDiscoverPeersInProgress() {
        return this.mDiscoverPeersInProgress;
    }

    public void requestPeersEx() {
        requestPeers();
    }

    public void disconnectInner() {
        disconnect();
    }

    public void postDelayedDiscover() {
        this.mHandler.postDelayed(this.mDiscoverPeers, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    private void unregisterPGStateEvent() {
        if (this.mHWdcEx != null) {
            this.mHWdcEx.unregisterPGStateEvent();
        }
    }

    private void updateConnectionErrorCode(int errorCode) {
        if (this.mHWdcEx != null) {
            this.mHWdcEx.updateConnectionErrorCode(errorCode);
        }
    }

    public void requestStartScan(int channelID) {
        if (this.mHWdcEx != null) {
            this.mHWdcEx.requestStartScan(channelID);
        } else {
            requestStartScan();
        }
    }

    public void setConnectParameters(boolean isSupportHdcp, String verificaitonCode) {
        if (this.mHWdcEx != null) {
            this.mHWdcEx.setConnectParameters(isSupportHdcp, verificaitonCode);
        }
    }

    public void checkVerificationResult(boolean isRight) {
        if (this.mHWdcEx != null) {
            this.mHWdcEx.checkVerificationResult(isRight);
        }
    }
}
