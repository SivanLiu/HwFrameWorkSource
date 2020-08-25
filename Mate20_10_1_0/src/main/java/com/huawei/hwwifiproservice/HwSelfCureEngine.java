package com.huawei.hwwifiproservice;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.DhcpResults;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.StaticIpConfiguration;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.wifi.HwHiLog;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.HwWifiCHRHilink;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifi.hwUtil.WifiCommonUtils;
import com.huawei.hwwifiproservice.MobileQosDetector;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class HwSelfCureEngine extends StateMachine {
    private static final String ADD_REPLACE_DNS_RECORD = "replaceDnsCnt";
    private static final int ARP_DETECTED_FAILED_COUNT = 5;
    private static final String ARP_TEST = "arptest";
    private static final int BASE = 131072;
    private static final int CMD_ARP_FAILED_DETECTED = 139;
    private static final int CMD_AUTO_CONN_FAILED_CURE = 102;
    private static final int CMD_AUTO_CONN_FAILED_DETECT = 101;
    private static final int CMD_BSSID_DHCP_FAILED_EVENT = 135;
    private static final int CMD_CONFIGURED_NETWORK_DELETED = 107;
    private static final int CMD_CURE_CONNECTED_TIMEOUT = 103;
    private static final int CMD_DHCP_OFFER_PKT_RCV = 126;
    private static final int CMD_DHCP_RESULTS_UPDATE = 125;
    private static final int CMD_DNS_FAILED_DETECTED = 133;
    private static final int CMD_DNS_FAILED_MONITOR = 123;
    private static final int CMD_GATEWAY_CHANGED_DETECT = 115;
    private static final int CMD_HTTP_REACHABLE_RCV = 136;
    private static final int CMD_INTERNET_FAILED_SELF_CURE = 112;
    private static final int CMD_INTERNET_FAILURE_DETECTED = 122;
    private static final int CMD_INTERNET_RECOVERY_CONFIRM = 113;
    private static final int CMD_INTERNET_STATUS_DETECT = 111;
    private static final int CMD_INVALID_DHCP_OFFER_EVENT = 130;
    private static final int CMD_INVALID_IP_CONFIRM = 129;
    private static final int CMD_IP_CONFIG_COMPLETED = 117;
    private static final int CMD_IP_CONFIG_LOST_EVENT = 124;
    private static final int CMD_IP_CONFIG_TIMEOUT = 116;
    private static final int CMD_MULTIGW_SELFCURE = 138;
    private static final int CMD_NETWORK_CONNECTED_RCVD = 104;
    private static final int CMD_NETWORK_DISCONNECTED_RCVD = 108;
    private static final int CMD_NETWORK_ROAMING_DETECT = 110;
    private static final int CMD_NEW_RSSI_RCVD = 109;
    private static final int CMD_NEW_SCAN_RESULTS_RCV = 127;
    private static final int CMD_NO_TCP_RX_DETECTED = 121;
    private static final int CMD_P2P_DISCONNECTED_EVENT = 128;
    private static final int CMD_PERIODIC_ARP_DETECTED = 306;
    private static final int CMD_RESETUP_SELF_CURE_MONITOR = 118;
    private static final int CMD_ROUTER_GATEWAY_UNREACHABLE_EVENT = 132;
    private static final int CMD_SELF_CURE_WIFI_FAILED = 120;
    private static final int CMD_SELF_CURE_WIFI_LINK = 114;
    private static final int CMD_SETTINGS_DISPLAY_NO_INTERNET_EVENT = 131;
    private static final int CMD_UPDATE_CONN_SELF_CURE_HISTORY = 119;
    private static final int CMD_UPDATE_WIFIPRO_CONFIGURATIONS = 131672;
    private static final int CMD_USER_ENTER_WLAN_SETTINGS = 137;
    private static final int CMD_USER_PRESENT_RCVD = 134;
    private static final int CMD_WIFI6_SELFCURE = 140;
    private static final int CMD_WIFI6_WITHOUT_HTC_ARP_FAILED_DETECTED = 310;
    private static final int CMD_WIFI6_WITHOUT_HTC_PERIODIC_ARP_DETECTED = 307;
    private static final int CMD_WIFI6_WITH_HTC_ARP_FAILED_DETECTED = 309;
    private static final int CMD_WIFI6_WITH_HTC_PERIODIC_ARP_DETECTED = 308;
    private static final int CMD_WIFI_DISABLED_RCVD = 105;
    private static final int CMD_WIFI_ENABLED_RCVD = 106;
    private static final int CURE_OUT_OF_DATE_MS = 7200000;
    private static final int DEAUTH_BSSID_CNT = 3;
    private static final int DEFAULT_ARP_DETECTED_MS = 60000;
    private static final int DEFAULT_ARP_TIMEOUT_MS = 1000;
    private static final int DEFAULT_GATEWAY_NUMBER = 1;
    private static final int DELTA_DNS_FAIL_CNT = 3;
    private static final int DELTA_RX_CNT = 0;
    private static final int DHCP_CONFIRM_DELAYED_MS = 500;
    private static final int DHCP_RENEW_TIMEOUT_MS = 6000;
    private static final int DNS_UPDATE_CONFIRM_DELAYED_MS = 1000;
    private static final int ENABLE_NETWORK_RSSI_THR = -75;
    private static final int EVENT_ARP_DETECT = 1133;
    private static final int EVENT_AX_BLACKLIST = 131;
    private static final int EVENT_AX_CLOSE_HTC = 132;
    private static final int FAST_ARP_DETECTED_MS = 10000;
    private static final String GATEWAY = "gateway";
    private static final int GRATUITOUS_ARP_TIMEOUT_MS = 100;
    private static final int HANDLE_WIFI_ON_DELAYED_MS = 1000;
    private static final String IFACE = "wlan0";
    private static final int INITIAL_RSSI = -200;
    private static final int INTERNET_DETECT_INTERVAL_MS = 6000;
    private static final int INTERNET_FAILED_INVALID_IP = 305;
    private static final int INTERNET_FAILED_TYPE_DNS = 303;
    private static final int INTERNET_FAILED_TYPE_GATEWAY = 302;
    private static final int INTERNET_FAILED_TYPE_ROAMING = 301;
    private static final int INTERNET_FAILED_TYPE_TCP = 304;
    private static final int INTERNET_OK = 300;
    private static final int IP_CONFIG_CONFIRM_DELAYED_MS = 2000;
    private static final String PROP_DISABLE_SELF_CURE = "hw.wifi.disable_self_cure";
    private static final int REQ_HTTP_DELAYED_MS = 500;
    private static final int SELF_CURE_DELAYED_MS = 100;
    private static final String SELF_CURE_EVENT = "selfCureEvent";
    private static final String SELF_CURE_INTERNET_ERROR = "internetError";
    private static final int SELF_CURE_MONITOR_DELAYED_MS = 2000;
    private static final String SELF_CURE_STOP_USE = "stopUse";
    private static final String SELF_CURE_SUCC_EVENT = "selfCureSuccEvent";
    private static final int SELF_CURE_TIMEOUT_MS = 20000;
    private static final String SELF_CURE_USER_REJECT = "rejected";
    private static final String SELF_CURE_WRONG_PWD = "wrongPassword";
    private static final int SET_STATIC_IP_TIMEOUT_MS = 3000;
    private static final String TAG = "HwSelfCureEngine";
    private static final String UNIQUE_CURE_EVENT = "uniqueCureEvent";
    private static final int WIFI6_ARP_DETECTED_MS = 500;
    private static final long WIFI6_BLACKLIST_TIME_EXPIRED = 172800000;
    private static final int WIFI6_HTC_ARP_DETECTED_MS = 300;
    private static final int WIFI_6_SUPPORT = 2;
    /* access modifiers changed from: private */
    public static int mDisableReason = 0;
    private static HwSelfCureEngine mHwSelfCureEngine = null;
    /* access modifiers changed from: private */
    public static int mSelfCureReason = 0;
    /* access modifiers changed from: private */
    public Map<String, WifiConfiguration> autoConnectFailedNetworks = new HashMap();
    /* access modifiers changed from: private */
    public Map<String, Integer> autoConnectFailedNetworksRssi = new HashMap();
    /* access modifiers changed from: private */
    public int mArpDetectionFailedCnt = 0;
    /* access modifiers changed from: private */
    public State mConnectedMonitorState = new ConnectedMonitorState();
    /* access modifiers changed from: private */
    public long mConnectedTimeMills;
    /* access modifiers changed from: private */
    public String mConnectionCureConfigKey = null;
    /* access modifiers changed from: private */
    public State mConnectionSelfCureState = new ConnectionSelfCureState();
    /* access modifiers changed from: private */
    public Context mContext;
    private State mDefaultState = new DefaultState();
    /* access modifiers changed from: private */
    public final Object mDhcpFailedBssidLock = new Object();
    /* access modifiers changed from: private */
    public ArrayList<String> mDhcpFailedBssids = new ArrayList<>();
    /* access modifiers changed from: private */
    public ArrayList<String> mDhcpFailedConfigKeys = new ArrayList<>();
    /* access modifiers changed from: private */
    public Map<String, String> mDhcpOfferPackets = new HashMap();
    /* access modifiers changed from: private */
    public ArrayList<String> mDhcpResultsTestDone = new ArrayList<>();
    /* access modifiers changed from: private */
    public State mDisconnectedMonitorState = new DisconnectedMonitorState();
    /* access modifiers changed from: private */
    public boolean mHasTestWifi6Reassoc = false;
    private boolean mInitialized = false;
    /* access modifiers changed from: private */
    public State mInternetSelfCureState = new InternetSelfCureState();
    /* access modifiers changed from: private */
    public boolean mInternetUnknown = false;
    private int mIpConfigLostCnt = 0;
    /* access modifiers changed from: private */
    public boolean mIsCaptivePortalCheckEnabled;
    /* access modifiers changed from: private */
    public boolean mIsHttpRedirected = false;
    /* access modifiers changed from: private */
    public boolean mIsWifi6ArpSuccess = false;
    /* access modifiers changed from: private */
    public AtomicBoolean mIsWifiBackground = new AtomicBoolean(false);
    /* access modifiers changed from: private */
    public boolean mMobileHotspot = false;
    private HwNetworkPropertyChecker mNetworkChecker;
    /* access modifiers changed from: private */
    public WifiConfiguration mNoAutoConnConfig;
    /* access modifiers changed from: private */
    public int mNoAutoConnCounter = 0;
    /* access modifiers changed from: private */
    public int mNoAutoConnReason = -1;
    /* access modifiers changed from: private */
    public int mNoTcpRxCounter = 0;
    /* access modifiers changed from: private */
    public AtomicBoolean mP2pConnected = new AtomicBoolean(false);
    private PowerManager mPowerManager;
    /* access modifiers changed from: private */
    public HwRouterInternetDetector mRouterInternetDetector = null;
    /* access modifiers changed from: private */
    public WifiConfiguration mSelfCureConfig = null;
    /* access modifiers changed from: private */
    public AtomicBoolean mSelfCureOngoing = new AtomicBoolean(false);
    /* access modifiers changed from: private */
    public boolean mStaticIpCureSuccess = false;
    /* access modifiers changed from: private */
    public Map<String, Wifi6BlackListInfo> mWifi6BlackListCache = new HashMap();
    /* access modifiers changed from: private */
    public State mWifi6SelfCureState = new Wifi6SelfCureState();
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;
    /* access modifiers changed from: private */
    public WifiNative mWifiNative = null;
    /* access modifiers changed from: private */
    public Map<String, CureFailedNetworkInfo> networkCureFailedHistory = new HashMap();
    /* access modifiers changed from: private */
    public WifiProChrUploadManager uploadManager;

    static /* synthetic */ int access$4004(HwSelfCureEngine x0) {
        int i = x0.mNoAutoConnCounter + 1;
        x0.mNoAutoConnCounter = i;
        return i;
    }

    static /* synthetic */ int access$412(HwSelfCureEngine x0, int x1) {
        int i = x0.mNoTcpRxCounter + x1;
        x0.mNoTcpRxCounter = i;
        return i;
    }

    public static synchronized HwSelfCureEngine getInstance(Context context) {
        HwSelfCureEngine hwSelfCureEngine;
        synchronized (HwSelfCureEngine.class) {
            if (mHwSelfCureEngine == null) {
                mHwSelfCureEngine = new HwSelfCureEngine(context);
            }
            hwSelfCureEngine = mHwSelfCureEngine;
        }
        return hwSelfCureEngine;
    }

    public static synchronized HwSelfCureEngine getInstance() {
        HwSelfCureEngine hwSelfCureEngine;
        synchronized (HwSelfCureEngine.class) {
            hwSelfCureEngine = mHwSelfCureEngine;
        }
        return hwSelfCureEngine;
    }

    private HwSelfCureEngine(Context context) {
        super(TAG);
        boolean z = false;
        this.mContext = context;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mNetworkChecker = new HwNetworkPropertyChecker(context, this.mWifiManager, null, true, null, true);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mRouterInternetDetector = HwRouterInternetDetector.getInstance(context, this);
        this.uploadManager = WifiProChrUploadManager.getInstance(this.mContext);
        this.mIsCaptivePortalCheckEnabled = Settings.Global.getInt(this.mContext.getContentResolver(), "captive_portal_mode", 1) != 0 ? true : z;
        this.mWifiNative = WifiInjector.getInstance().getWifiNative();
        addState(this.mDefaultState);
        addState(this.mConnectedMonitorState, this.mDefaultState);
        addState(this.mDisconnectedMonitorState, this.mDefaultState);
        addState(this.mConnectionSelfCureState, this.mDefaultState);
        addState(this.mInternetSelfCureState, this.mDefaultState);
        addState(this.mWifi6SelfCureState, this.mInternetSelfCureState);
        setInitialState(this.mDisconnectedMonitorState);
        start();
        HwSelfCureUtils.initDnsServer();
    }

    public synchronized void setup() {
        if (!this.mInitialized) {
            this.mInitialized = true;
            HwHiLog.d(TAG, false, "setup DONE!", new Object[0]);
            registerReceivers();
        }
    }

    public void registerReceivers() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.SCAN_RESULTS");
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.RSSI_CHANGED");
        intentFilter.addAction("android.net.wifi.CONFIGURED_NETWORKS_CHANGE");
        intentFilter.addAction(WifiProCommonDefs.ACTION_DHCP_OFFER_INFO);
        intentFilter.addAction("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
        intentFilter.addAction("android.net.wifi.p2p.CONNECT_STATE_CHANGE");
        intentFilter.addAction(WifiProCommonDefs.ACTION_INVALID_DHCP_OFFER_RCVD);
        intentFilter.addAction("android.intent.action.USER_PRESENT");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            /* class com.huawei.hwwifiproservice.HwSelfCureEngine.AnonymousClass1 */

            public void onReceive(Context context, Intent intent) {
                HwSelfCureEngine.this.handleBroadcastReceiver(intent);
            }
        }, intentFilter);
    }

    /* access modifiers changed from: private */
    public void handleBroadcastReceiver(Intent intent) {
        if (intent != null) {
            if ("android.net.wifi.SCAN_RESULTS".equals(intent.getAction())) {
                if (!intent.getBooleanExtra("resultsUpdated", false)) {
                    return;
                }
                if (getCurrentState() == this.mConnectedMonitorState) {
                    sendMessage(127);
                } else {
                    sendMessage(101);
                }
            } else if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
                handleNetworkStateChanged(intent);
            } else if ("android.net.wifi.WIFI_STATE_CHANGED".equals(intent.getAction())) {
                if (this.mWifiManager.isWifiEnabled()) {
                    sendMessageDelayed(106, 1000);
                } else {
                    sendMessage(105);
                }
            } else if ("android.net.wifi.RSSI_CHANGED".equals(intent.getAction())) {
                int newRssi = intent.getIntExtra("newRssi", -127);
                if (newRssi != -127) {
                    sendMessage(109, newRssi, 0);
                }
            } else if ("android.net.wifi.CONFIGURED_NETWORKS_CHANGE".equals(intent.getAction())) {
                WifiConfiguration config = null;
                Object configTmp = intent.getParcelableExtra("wifiConfiguration");
                if (configTmp instanceof WifiConfiguration) {
                    config = (WifiConfiguration) configTmp;
                }
                if (intent.getIntExtra("changeReason", 0) == 1) {
                    sendMessage(107, config);
                }
            } else if (WifiProCommonDefs.ACTION_DHCP_OFFER_INFO.equals(intent.getAction())) {
                String dhcpResults = intent.getStringExtra(WifiProCommonDefs.FLAG_DHCP_OFFER_INFO);
                if (dhcpResults != null) {
                    sendMessage(126, dhcpResults);
                }
            } else if ("android.net.wifi.p2p.CONNECTION_STATE_CHANGE".equals(intent.getAction())) {
                handleP2pDisconnected(intent);
            } else {
                handleBroadcastReceiverAdditional(intent);
            }
        }
    }

    private void handleBroadcastReceiverAdditional(Intent intent) {
        if (intent != null) {
            if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                if (getCurrentState() == this.mConnectedMonitorState || getCurrentState() == this.mInternetSelfCureState) {
                    if (hasMessages(CMD_PERIODIC_ARP_DETECTED)) {
                        removeMessages(CMD_PERIODIC_ARP_DETECTED);
                    }
                    sendMessageDelayed(CMD_PERIODIC_ARP_DETECTED, 10000);
                }
            } else if (WifiProCommonDefs.ACTION_INVALID_DHCP_OFFER_RCVD.equals(intent.getAction())) {
                String dhcpResults = intent.getStringExtra(WifiProCommonDefs.FLAG_DHCP_OFFER_INFO);
                if (dhcpResults != null) {
                    HwHiLog.d(TAG, false, "ACTION_INVALID_DHCP_OFFER_RCVD, dhcpResults = %{private}s", new Object[]{dhcpResults});
                    sendMessageDelayed(CMD_INVALID_DHCP_OFFER_EVENT, dhcpResults, 2000);
                }
            } else if ("android.net.wifi.p2p.CONNECT_STATE_CHANGE".equals(intent.getAction())) {
                handleP2pConnected(intent);
            } else if ("android.intent.action.USER_PRESENT".equals(intent.getAction()) && getCurrentState() == this.mDisconnectedMonitorState) {
                sendMessage(CMD_USER_PRESENT_RCVD);
            }
        }
    }

    private void handleNetworkStateChanged(Intent intent) {
        NetworkInfo info = null;
        Object infoTmp = intent.getParcelableExtra("networkInfo");
        if (infoTmp instanceof NetworkInfo) {
            info = (NetworkInfo) infoTmp;
        }
        if (info != null && info.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED) {
            sendMessage(108);
        } else if (info != null && info.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
            sendMessage(104);
        }
    }

    private void handleP2pDisconnected(Intent intent) {
        NetworkInfo p2pNetworkInfo = null;
        Object p2pNetworkInfoTmp = intent.getParcelableExtra("networkInfo");
        if (p2pNetworkInfoTmp instanceof NetworkInfo) {
            p2pNetworkInfo = (NetworkInfo) p2pNetworkInfoTmp;
        }
        if (p2pNetworkInfo != null && p2pNetworkInfo.getState() == NetworkInfo.State.DISCONNECTED) {
            this.mP2pConnected.set(false);
            if (getCurrentState() == this.mInternetSelfCureState) {
                sendMessage(128);
            }
        }
    }

    private void handleP2pConnected(Intent intent) {
        int p2pState = intent.getIntExtra("extraState", -1);
        if (p2pState == 1 || p2pState == 2) {
            this.mP2pConnected.set(true);
        }
    }

    /* access modifiers changed from: private */
    public void sendNetworkCheckingStatus(String action, String flag, int property) {
        Intent intent = new Intent(action);
        intent.setFlags(67108864);
        intent.putExtra(flag, property);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message message) {
            if (message.what != 126) {
                return true;
            }
            handleDhcpOfferPacketRcv((String) message.obj);
            return true;
        }

        private void handleDhcpOfferPacketRcv(String dhcpResutls) {
            String gateway;
            Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 44, new Bundle());
            boolean isWifiProEvaluatingAP = false;
            if (result != null) {
                isWifiProEvaluatingAP = result.getBoolean("isWifiProEvaluatingAP");
            }
            if (dhcpResutls != null && HwSelfCureEngine.this.isSuppOnCompletedState() && !isWifiProEvaluatingAP && (gateway = WifiProCommonUtils.dhcpResults2Gateway(dhcpResutls)) != null) {
                HwSelfCureEngine.this.mDhcpOfferPackets.put(gateway.replace("/", ""), dhcpResutls);
            }
        }
    }

    class ConnectedMonitorState extends State {
        private int mConfigAuthType = -1;
        private boolean mGatewayInvalid = false;
        private boolean mHasInternetRecently;
        private boolean mIpv4DnsEnabled;
        private String mLastConnectedBssid;
        private int mLastDnsFailedCounter;
        private int mLastDnsRefuseCounter;
        private int mLastSignalLevel;
        private boolean mMobileHotspot;
        private boolean mPortalUnthenEver;
        private boolean mUserSetStaticIpConfig;
        private boolean mWifiSwitchAllowed;

        ConnectedMonitorState() {
        }

        public void enter() {
            HwHiLog.d(HwSelfCureEngine.TAG, false, "==> ##ConnectedMonitorState", new Object[0]);
            this.mLastConnectedBssid = WifiProCommonUtils.getCurrentBssid(HwSelfCureEngine.this.mWifiManager);
            this.mLastDnsFailedCounter = HwSelfCureUtils.getCurrentDnsFailedCounter();
            this.mLastDnsRefuseCounter = HwSelfCureUtils.getCurrentDnsRefuseCounter();
            int unused = HwSelfCureEngine.this.mNoTcpRxCounter = 0;
            this.mLastSignalLevel = 0;
            int unused2 = HwSelfCureEngine.this.mArpDetectionFailedCnt = 0;
            this.mHasInternetRecently = false;
            this.mPortalUnthenEver = false;
            boolean unused3 = HwSelfCureEngine.this.mInternetUnknown = false;
            this.mUserSetStaticIpConfig = false;
            HwSelfCureEngine.this.mSelfCureOngoing.set(false);
            this.mIpv4DnsEnabled = true;
            this.mWifiSwitchAllowed = false;
            this.mMobileHotspot = HwFrameworkFactory.getHwInnerWifiManager().getHwMeteredHint(HwSelfCureEngine.this.mContext);
            WifiInfo wifiInfo = HwSelfCureEngine.this.mWifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                this.mLastSignalLevel = WifiProCommonUtils.getCurrenSignalLevel(wifiInfo);
                HwHiLog.d(HwSelfCureEngine.TAG, false, "ConnectedMonitorState, network = %{public}s, signal = %{public}d, mobileHotspot = %{public}s", new Object[]{StringUtilEx.safeDisplaySsid(wifiInfo.getSSID()), Integer.valueOf(this.mLastSignalLevel), String.valueOf(this.mMobileHotspot)});
            }
            if (!HwSelfCureEngine.this.mIsWifiBackground.get() && !setupSelfCureMonitor()) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "ConnectedMonitorState, config is null when connected broadcast received, delay to setup again.", new Object[0]);
                HwSelfCureEngine.this.sendMessageDelayed(118, 2000);
            }
            HwSelfCureEngine.this.sendBlacklistToDriver();
            HwSelfCureEngine.this.sendMessageDelayed(HwSelfCureEngine.CMD_PERIODIC_ARP_DETECTED, 10000);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 104) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "ConnectedMonitorState, CMD_NETWORK_CONNECTED_RCVD!", new Object[0]);
                if (HwSelfCureEngine.this.mIsWifiBackground.get()) {
                    HwSelfCureEngine.this.mIsWifiBackground.set(false);
                    enter();
                }
            } else if (i != 115) {
                if (i == 118) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_RESETUP_SELF_CURE_MONITOR rcvd", new Object[0]);
                    setupSelfCureMonitor();
                } else if (i == 125) {
                    updateDhcpResultsByBssid(WifiProCommonUtils.getCurrentBssid(HwSelfCureEngine.this.mWifiManager), (String) message.obj);
                } else if (i == 127) {
                    handleNewScanResults();
                } else if (i == HwSelfCureEngine.CMD_INVALID_IP_CONFIRM) {
                    HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                    if (HwSelfCureEngine.this.isHttpReachable(false)) {
                        HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                        HwSelfCureEngine.this.notifyHttpReachableForWifiPro(true);
                        int unused = HwSelfCureEngine.this.mNoTcpRxCounter = 0;
                    } else {
                        int selfCureType = HwSelfCureEngine.this.mDhcpOfferPackets.size() >= 2 ? 302 : HwSelfCureEngine.INTERNET_FAILED_INVALID_IP;
                        int unused2 = HwSelfCureEngine.mSelfCureReason = selfCureType;
                        transitionToSelfCureState(selfCureType);
                    }
                } else if (i != HwSelfCureEngine.CMD_DNS_FAILED_DETECTED) {
                    if (i == HwSelfCureEngine.CMD_HTTP_REACHABLE_RCV) {
                        WifiConfiguration current = WifiProCommonUtils.getCurrentWifiConfig(HwSelfCureEngine.this.mWifiManager);
                        if (current != null) {
                            current.internetSelfCureHistory = "0|0|0|0|0|0|0|0|0|0|0|0|0|0";
                            current.validatedInternetAccess = true;
                            current.noInternetAccess = false;
                            current.wifiProNoInternetAccess = false;
                            current.wifiProNoInternetReason = 0;
                            Bundle data = new Bundle();
                            data.putInt("messageWhat", 131672);
                            data.putParcelable("messageObj", current);
                            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 28, data);
                        }
                    } else if (i != HwSelfCureEngine.CMD_ARP_FAILED_DETECTED) {
                        if (i != HwSelfCureEngine.CMD_PERIODIC_ARP_DETECTED) {
                            switch (i) {
                                case 108:
                                    if (HwSelfCureEngine.this.hasMessages(115)) {
                                        HwSelfCureEngine.this.removeMessages(115);
                                    }
                                    if (HwSelfCureEngine.this.hasMessages(118)) {
                                        HwSelfCureEngine.this.removeMessages(118);
                                    }
                                    HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                                    hwSelfCureEngine.transitionTo(hwSelfCureEngine.mDisconnectedMonitorState);
                                    break;
                                case 109:
                                    this.mLastSignalLevel = WifiProCommonUtils.getCurrenSignalLevel(HwSelfCureEngine.this.mWifiManager.getConnectionInfo());
                                    break;
                                case 110:
                                    if (!HwSelfCureEngine.this.mIsWifiBackground.get()) {
                                        String newBssid = message.obj != null ? (String) message.obj : null;
                                        if (newBssid == null || !newBssid.equals(this.mLastConnectedBssid)) {
                                            if (!this.mUserSetStaticIpConfig) {
                                                updateInternetAccessHistory();
                                                if (!this.mHasInternetRecently && !this.mPortalUnthenEver && !HwSelfCureEngine.this.mInternetUnknown) {
                                                    HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_NETWORK_ROAMING_DETECT rcvd, but no internet access always.", new Object[0]);
                                                    break;
                                                } else {
                                                    if (HwSelfCureEngine.this.hasMessages(115)) {
                                                        HwSelfCureEngine.this.removeMessages(115);
                                                    }
                                                    this.mLastConnectedBssid = newBssid;
                                                    DhcpResults dhcpResults = HwSelfCureEngine.this.syncGetDhcpResults();
                                                    if (dhcpResults != null) {
                                                        Bundle data2 = new Bundle();
                                                        data2.putSerializable(HwSelfCureEngine.GATEWAY, dhcpResults.gateway);
                                                        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 75, data2);
                                                        boolean ArpTest = false;
                                                        if (result != null) {
                                                            ArpTest = result.getBoolean(HwSelfCureEngine.ARP_TEST);
                                                        }
                                                        if (ArpTest) {
                                                            HwHiLog.d(HwSelfCureEngine.TAG, false, "last gateway reachable, don't use http-get, gateway unchanged after roaming!", new Object[0]);
                                                            HwSelfCureEngine.this.sendNetworkCheckingStatus(WifiProCommonDefs.ACTION_NETWORK_CONDITIONS_MEASURED, WifiProCommonDefs.EXTRA_IS_INTERNET_READY, 5);
                                                            break;
                                                        }
                                                    }
                                                    if (!HwSelfCureEngine.this.hasMessages(108)) {
                                                        HwHiLog.d(HwSelfCureEngine.TAG, false, "gateway changed or unknow, need to check http response!", new Object[0]);
                                                        HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                                                        int unused3 = HwSelfCureEngine.mSelfCureReason = 301;
                                                        transitionToSelfCureState(301);
                                                        break;
                                                    }
                                                }
                                            } else {
                                                HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_NETWORK_ROAMING_DETECT rcvd, but user set static ip config, ignore it.", new Object[0]);
                                                break;
                                            }
                                        } else {
                                            HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_NETWORK_ROAMING_DETECT rcvd, but bssid is unchanged, ignore it.", new Object[0]);
                                            break;
                                        }
                                    }
                                    break;
                                default:
                                    switch (i) {
                                        case 121:
                                            if (!this.mMobileHotspot && !this.mGatewayInvalid && !HwSelfCureEngine.this.mIsWifiBackground.get()) {
                                                updateInternetAccessHistory();
                                                handleNoTcpRxDetected();
                                                break;
                                            }
                                        case 122:
                                            if (((Boolean) message.obj).booleanValue()) {
                                                HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_INTERNET_FAILURE_DETECTED rcvd, delete dhcp cache", new Object[0]);
                                                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 45, new Bundle());
                                            }
                                            if (!this.mMobileHotspot || HwSelfCureEngine.this.isWifi6Network(this.mLastConnectedBssid)) {
                                                handleInternetFailedDetected(message);
                                                break;
                                            }
                                        case 123:
                                            handleDnsFailedMonitor();
                                            break;
                                        default:
                                            return false;
                                    }
                            }
                        } else {
                            HwSelfCureEngine.this.periodicArpDetection();
                        }
                    } else if (shouldTransitionToWifi6SelfCureState()) {
                        boolean access$600 = HwSelfCureEngine.this.mInternetUnknown;
                        HwSelfCureEngine hwSelfCureEngine2 = HwSelfCureEngine.this;
                        hwSelfCureEngine2.deferMessage(hwSelfCureEngine2.obtainMessage(HwSelfCureEngine.CMD_WIFI6_SELFCURE, access$600 ? 1 : 0, 0, false));
                        HwSelfCureEngine hwSelfCureEngine3 = HwSelfCureEngine.this;
                        hwSelfCureEngine3.transitionTo(hwSelfCureEngine3.mWifi6SelfCureState);
                    } else {
                        HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                        if (HwSelfCureEngine.this.isHttpReachable(false)) {
                            HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                            HwSelfCureEngine.this.notifyHttpReachableForWifiPro(true);
                        } else {
                            int unused4 = HwSelfCureEngine.mSelfCureReason = HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP;
                            transitionToSelfCureState(HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP);
                        }
                    }
                } else if (!this.mMobileHotspot && !this.mGatewayInvalid && !HwSelfCureEngine.this.mIsWifiBackground.get()) {
                    updateInternetAccessHistory();
                    handleDnsFailedDetected();
                }
            } else if (HwSelfCureEngine.this.mDhcpOfferPackets.size() >= 2 || (this.mHasInternetRecently && WifiProCommonUtils.isEncryptedAuthType(this.mConfigAuthType))) {
                checkHttpResponseAndSelfCure(115);
            }
            return true;
        }

        public void exit() {
            HwHiLog.d(HwSelfCureEngine.TAG, false, "ConnectedMonitorState exit", new Object[0]);
            HwSelfCureEngine.this.removeMessages(HwSelfCureEngine.CMD_PERIODIC_ARP_DETECTED);
        }

        private void handleNewScanResults() {
            List<ScanResult> scanResults = HwSelfCureEngine.this.mWifiManager.getScanResults();
            List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
            WifiConfiguration config = WifiProCommonUtils.getCurrentWifiConfig(HwSelfCureEngine.this.mWifiManager);
            this.mWifiSwitchAllowed = config != null && WifiProCommonUtils.isAllowWifiSwitch(scanResults, configNetworks, WifiProCommonUtils.getCurrentBssid(HwSelfCureEngine.this.mWifiManager), WifiProCommonUtils.getCurrentSsid(HwSelfCureEngine.this.mWifiManager), config.configKey(), -75);
        }

        private boolean setupSelfCureMonitor() {
            WifiConfiguration config = WifiProCommonUtils.getCurrentWifiConfig(HwSelfCureEngine.this.mWifiManager);
            if (config == null) {
                return false;
            }
            WifiConfiguration unused = HwSelfCureEngine.this.mSelfCureConfig = config;
            this.mConfigAuthType = config.allowedKeyManagement.cardinality() > 1 ? -1 : config.getAuthType();
            this.mUserSetStaticIpConfig = config.getIpAssignment() != null && config.getIpAssignment() == IpConfiguration.IpAssignment.STATIC;
            boolean unused2 = HwSelfCureEngine.this.mInternetUnknown = WifiProCommonUtils.matchedRequestByHistory(config.internetHistory, 103);
            updateInternetAccessHistory();
            HwHiLog.d(HwSelfCureEngine.TAG, false, "ConnectedMonitorState, hasInternet = %{public}s, portalUnthen = %{public}s, userSetStaticIp = %{public}s, history empty = %{public}s", new Object[]{String.valueOf(this.mHasInternetRecently), String.valueOf(this.mPortalUnthenEver), String.valueOf(this.mUserSetStaticIpConfig), String.valueOf(HwSelfCureEngine.this.mInternetUnknown)});
            if (!this.mMobileHotspot) {
                DhcpResults dhcpResults = HwSelfCureEngine.this.syncGetDhcpResults();
                this.mGatewayInvalid = dhcpResults == null || dhcpResults.gateway == null;
                HwHiLog.d(HwSelfCureEngine.TAG, false, "ConnectedMonitorState, gatewayInvalid = %{public}s", new Object[]{String.valueOf(this.mGatewayInvalid)});
                if (HwSelfCureEngine.this.mIsWifiBackground.get() || HwSelfCureEngine.this.mStaticIpCureSuccess || ((!this.mHasInternetRecently && !HwSelfCureEngine.this.mInternetUnknown) || !HwSelfCureEngine.this.isIpAddressInvalid())) {
                    StaticIpConfiguration staticIpConfig = WifiProCommonUtils.dhcpResults2StaticIpConfig(config.lastDhcpResults);
                    if ((this.mUserSetStaticIpConfig || dhcpResults == null || staticIpConfig == null || dhcpResults.gateway == null || staticIpConfig.gateway == null || staticIpConfig.ipAddress == null || staticIpConfig.dnsServers == null) ? false : true) {
                        String currentGateway = dhcpResults.gateway.getHostAddress();
                        String lastGateway = staticIpConfig.gateway.getHostAddress();
                        HwHiLog.d(HwSelfCureEngine.TAG, false, "ConnectedMonitorState, currentGateway = %{public}s, lastGateway = %{public}s", new Object[]{WifiProCommonUtils.safeDisplayIpAddress(currentGateway), WifiProCommonUtils.safeDisplayIpAddress(lastGateway)});
                        if (!HwSelfCureEngine.this.mStaticIpCureSuccess && currentGateway != null && lastGateway != null && !currentGateway.equals(lastGateway)) {
                            HwHiLog.d(HwSelfCureEngine.TAG, false, "current gateway is different with history gateway that has internet.", new Object[0]);
                            HwSelfCureEngine.this.sendMessageDelayed(115, 300);
                            return true;
                        }
                    } else if (TextUtils.isEmpty(config.lastDhcpResults) && HwSelfCureEngine.this.mInternetUnknown) {
                        HwSelfCureEngine.this.sendMessageDelayed(115, 2000);
                        return true;
                    }
                } else {
                    HwSelfCureEngine.this.sendMessageDelayed(HwSelfCureEngine.CMD_INVALID_IP_CONFIRM, 2000);
                    return true;
                }
            }
            if (!this.mMobileHotspot && !HwSelfCureEngine.this.mIsWifiBackground.get() && !HwSelfCureEngine.this.mStaticIpCureSuccess && this.mHasInternetRecently) {
                HwSelfCureEngine.this.sendMessageDelayed(123, 6000);
            }
            return true;
        }

        private boolean hasIpv4Dnses(DhcpResults dhcpResults) {
            if (dhcpResults == null || dhcpResults.dnsServers == null || dhcpResults.dnsServers.size() == 0) {
                return false;
            }
            for (int i = 0; i < dhcpResults.dnsServers.size(); i++) {
                InetAddress dns = (InetAddress) dhcpResults.dnsServers.get(i);
                if (dns != null && dns.getHostAddress() != null && (dns instanceof Inet4Address)) {
                    return true;
                }
            }
            return false;
        }

        private void handleDnsFailedMonitor() {
            if (this.mLastSignalLevel <= 1) {
                this.mLastDnsFailedCounter = HwSelfCureUtils.getCurrentDnsFailedCounter();
                HwSelfCureEngine.this.sendMessageDelayed(123, 6000);
                return;
            }
            int currentDnsFailedCounter = HwSelfCureUtils.getCurrentDnsFailedCounter();
            int deltaFailedDns = currentDnsFailedCounter - this.mLastDnsFailedCounter;
            this.mLastDnsFailedCounter = currentDnsFailedCounter;
            int currentDnsRefuseCounter = HwSelfCureUtils.getCurrentDnsRefuseCounter();
            int deltaDnsResfuseDns = currentDnsRefuseCounter - this.mLastDnsRefuseCounter;
            this.mLastDnsRefuseCounter = currentDnsRefuseCounter;
            if (this.mGatewayInvalid) {
                return;
            }
            if (deltaFailedDns >= 2 || deltaDnsResfuseDns >= 2) {
                HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                if (HwSelfCureEngine.this.isHttpReachable(true)) {
                    int unused = HwSelfCureEngine.this.mNoTcpRxCounter = 0;
                    HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                    HwSelfCureEngine.this.notifyHttpReachableForWifiPro(true);
                    return;
                }
                HwHiLog.d(HwSelfCureEngine.TAG, false, "handleDnsFailedMonitor, deltaFailedDns = %{public}d, and HTTP unreachable, transition to SelfCureState.", new Object[]{Integer.valueOf(deltaFailedDns)});
                int unused2 = HwSelfCureEngine.mSelfCureReason = 303;
                transitionToSelfCureState(303);
            }
        }

        private void checkHttpResponseAndSelfCure(int eventType) {
            HwSelfCureEngine.this.mSelfCureOngoing.set(true);
            if (!HwSelfCureEngine.this.isHttpReachable(false)) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "checkHttpResponseAndSelfCure, HTTP unreachable for eventType = %{public}d, dhcp offer size = %{public}d", new Object[]{Integer.valueOf(eventType), Integer.valueOf(HwSelfCureEngine.this.mDhcpOfferPackets.size())});
                int internetFailedReason = MobileQosDetector.TcpIpqRtt.RTT_FINE_5;
                if (eventType == 110) {
                    internetFailedReason = 301;
                } else if (eventType == 115) {
                    internetFailedReason = 302;
                }
                int unused = HwSelfCureEngine.mSelfCureReason = internetFailedReason;
                transitionToSelfCureState(internetFailedReason);
                return;
            }
            HwHiLog.d(HwSelfCureEngine.TAG, false, "checkHttpResponseAndSelfCure, HTTP reachable for eventType = %{public}d", new Object[]{Integer.valueOf(eventType)});
            this.mLastDnsFailedCounter = HwSelfCureUtils.getCurrentDnsFailedCounter();
            int unused2 = HwSelfCureEngine.this.mNoTcpRxCounter = 0;
            HwSelfCureEngine.this.mSelfCureOngoing.set(false);
            HwSelfCureEngine.this.notifyHttpReachableForWifiPro(true);
        }

        private void handleDnsFailedDetected() {
            if (this.mLastSignalLevel > 2) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "handleDnsFailedDetected, start scan and parse the context for wifi 2 wifi.", new Object[0]);
                HwSelfCureEngine.this.startScan();
                HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                if (HwSelfCureEngine.this.isHttpReachable(false)) {
                    HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                    this.mLastDnsFailedCounter = HwSelfCureUtils.getCurrentDnsFailedCounter();
                    int unused = HwSelfCureEngine.this.mNoTcpRxCounter = 0;
                    return;
                }
                handleNewScanResults();
                if (this.mWifiSwitchAllowed) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "handleDnsFailedDetected, notify WLAN+ to do wifi swtich first.", new Object[0]);
                    HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                    HwSelfCureEngine.this.notifyHttpReachableForWifiPro(false);
                    return;
                }
                HwHiLog.d(HwSelfCureEngine.TAG, false, "handleDnsFailedDetected, HTTP unreachable, transition to SelfCureState.", new Object[0]);
                int unused2 = HwSelfCureEngine.mSelfCureReason = 303;
                transitionToSelfCureState(303);
            }
        }

        private void handleNoTcpRxDetected() {
            if (this.mLastSignalLevel > 2) {
                HwSelfCureEngine.access$412(HwSelfCureEngine.this, 1);
                if (HwSelfCureEngine.this.mNoTcpRxCounter == 1) {
                    this.mLastDnsFailedCounter = HwSelfCureUtils.getCurrentDnsFailedCounter();
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "handleNoTcpRxDetected, start scan and parse the context for wifi 2 wifi.", new Object[0]);
                    HwSelfCureEngine.this.startScan();
                } else if (HwSelfCureEngine.this.mNoTcpRxCounter != 2 || !this.mWifiSwitchAllowed) {
                    if (this.mHasInternetRecently || this.mPortalUnthenEver) {
                        HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                        if (HwSelfCureEngine.this.isHttpReachable(true)) {
                            int unused = HwSelfCureEngine.this.mNoTcpRxCounter = 0;
                            HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                            return;
                        }
                        HwHiLog.d(HwSelfCureEngine.TAG, false, "handleNoTcpRxDetected, HTTP unreachable, transition to SelfCureState.", new Object[0]);
                        int unused2 = HwSelfCureEngine.mSelfCureReason = HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP;
                        transitionToSelfCureState(HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP);
                    }
                } else if (HwSelfCureEngine.this.isHttpReachable(false)) {
                    this.mWifiSwitchAllowed = false;
                    int unused3 = HwSelfCureEngine.this.mNoTcpRxCounter = 0;
                } else {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "handleNoTcpRxDetected, notify WLAN+ to do wifi swtich first.", new Object[0]);
                    HwSelfCureEngine.this.notifyHttpReachableForWifiPro(false);
                }
            }
        }

        private void handleInternetFailedDetected(Message message) {
            if (!isWifi6SelfCureNeed(message)) {
                boolean access$2900 = HwSelfCureEngine.this.mStaticIpCureSuccess;
                int i = HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP;
                if (access$2900 || !((Boolean) message.obj).booleanValue()) {
                    HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                    if (HwSelfCureEngine.this.isHttpReachable(false)) {
                        HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                        int unused = HwSelfCureEngine.this.mNoTcpRxCounter = 0;
                        if (HwSelfCureEngine.this.mIsHttpRedirected) {
                            HwSelfCureEngine.this.notifyHttpRedirectedForWifiPro();
                        } else {
                            HwSelfCureEngine.this.notifyHttpReachableForWifiPro(true);
                        }
                    } else {
                        String errReason = this.mPortalUnthenEver ? "ERROR_PORTAL" : "OTHER";
                        Bundle data = new Bundle();
                        data.putString("errReason", errReason);
                        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 60, data);
                        Bundle bundledata = new Bundle();
                        bundledata.putInt("reason", 0);
                        bundledata.putBoolean("succ", false);
                        bundledata.putBoolean("isPortalAP", this.mPortalUnthenEver);
                        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 32, bundledata);
                        HwHiLog.d(HwSelfCureEngine.TAG, false, "handleInternetFailedDetected, HTTP unreachable, transition to SelfCureState.", new Object[0]);
                        int currentDnsFailedCounter = HwSelfCureUtils.getCurrentDnsFailedCounter();
                        int deltaFailedDns = currentDnsFailedCounter - this.mLastDnsFailedCounter;
                        this.mLastDnsFailedCounter = currentDnsFailedCounter;
                        int currentDnsRefuseCounter = HwSelfCureUtils.getCurrentDnsRefuseCounter();
                        int deltaDnsResfuseDns = currentDnsRefuseCounter - this.mLastDnsRefuseCounter;
                        this.mLastDnsRefuseCounter = currentDnsRefuseCounter;
                        if (deltaFailedDns >= 2 || deltaDnsResfuseDns >= 2) {
                            i = 303;
                        }
                        int unused2 = HwSelfCureEngine.mSelfCureReason = i;
                        transitionToSelfCureState(HwSelfCureEngine.mSelfCureReason);
                    }
                } else if (this.mHasInternetRecently || this.mPortalUnthenEver || HwSelfCureEngine.this.mInternetUnknown) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "handleInternetFailedDetected, wifi has no internet when connected.", new Object[0]);
                    int unused3 = HwSelfCureEngine.mSelfCureReason = 303;
                    transitionToSelfCureState(303);
                } else if (!HwSelfCureEngine.this.mInternetUnknown || !HwSelfCureEngine.this.multiGateway()) {
                    HwHiLog.w(HwSelfCureEngine.TAG, false, "handleInternetFailedDetected, There is not a expectant condition !", new Object[0]);
                } else {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "handleInternetFailedDetected, multi gateway due to no internet access.", new Object[0]);
                    int unused4 = HwSelfCureEngine.mSelfCureReason = HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP;
                    transitionToSelfCureState(HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP);
                }
            }
        }

        private boolean isWifi6SelfCureNeed(Message message) {
            if (message == null) {
                return false;
            }
            boolean z = true;
            if (shouldTransitionToWifi6SelfCureState()) {
                boolean access$600 = HwSelfCureEngine.this.mInternetUnknown;
                HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                hwSelfCureEngine.deferMessage(hwSelfCureEngine.obtainMessage(HwSelfCureEngine.CMD_WIFI6_SELFCURE, access$600 ? 1 : 0, 0, message.obj));
                HwSelfCureEngine hwSelfCureEngine2 = HwSelfCureEngine.this;
                hwSelfCureEngine2.transitionTo(hwSelfCureEngine2.mWifi6SelfCureState);
                return true;
            }
            if (!HwSelfCureEngine.this.mInternetUnknown) {
                HwSelfCureEngine hwSelfCureEngine3 = HwSelfCureEngine.this;
                if (message.arg1 != 1) {
                    z = false;
                }
                boolean unused = hwSelfCureEngine3.mInternetUnknown = z;
            }
            return false;
        }

        private boolean shouldTransitionToWifi6SelfCureState() {
            if (HwSelfCureEngine.this.isWifi6Network(this.mLastConnectedBssid) && !HwSelfCureEngine.this.mWifi6BlackListCache.containsKey(this.mLastConnectedBssid) && !HwSelfCureEngine.this.mIsWifi6ArpSuccess && WifiProCommonUtils.getCurrentRssi(HwSelfCureEngine.this.mWifiManager) >= -75) {
                return true;
            }
            HwHiLog.d(HwSelfCureEngine.TAG, false, "shouldTransitionToWifi6SelfCureState return false", new Object[0]);
            return false;
        }

        private void transitionToSelfCureState(int reason) {
            boolean z = this.mMobileHotspot;
            if (z) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "transitionToSelfCureState, don't support SCE, do nothing or mMobileHotspot =%{public}s", new Object[]{String.valueOf(z)});
                HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                return;
            }
            DhcpResults dhcpResults = HwSelfCureEngine.this.syncGetDhcpResults();
            this.mIpv4DnsEnabled = hasIpv4Dnses(dhcpResults);
            this.mGatewayInvalid = dhcpResults == null || dhcpResults.gateway == null;
            if (SystemProperties.getBoolean(HwSelfCureEngine.PROP_DISABLE_SELF_CURE, false) || !HwSelfCureEngine.this.mIsCaptivePortalCheckEnabled || !this.mIpv4DnsEnabled || this.mGatewayInvalid || "factory".equals(SystemProperties.get("ro.runmode", "normal"))) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "transitionToSelfCureState, don't support SCE, do nothing or mIpv4DnsEnabled =%{public}s", new Object[]{String.valueOf(this.mIpv4DnsEnabled)});
                HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                return;
            }
            Message dmsg = Message.obtain();
            dmsg.what = 112;
            dmsg.arg1 = reason;
            HwSelfCureEngine.this.sendMessageDelayed(dmsg, 100);
            HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
            hwSelfCureEngine.transitionTo(hwSelfCureEngine.mInternetSelfCureState);
        }

        private void updateInternetAccessHistory() {
            WifiConfiguration config = WifiProCommonUtils.getCurrentWifiConfig(HwSelfCureEngine.this.mWifiManager);
            if (config != null) {
                this.mHasInternetRecently = WifiProCommonUtils.matchedRequestByHistory(config.internetHistory, 100);
                this.mPortalUnthenEver = WifiProCommonUtils.matchedRequestByHistory(config.internetHistory, 102);
            }
        }

        private void updateDhcpResultsByBssid(String bssid, String dhcpResults) {
            PortalDataBaseManager database;
            if (bssid != null && dhcpResults != null && (database = PortalDataBaseManager.getInstance(HwSelfCureEngine.this.mContext)) != null) {
                database.updateDhcpResultsByBssid(bssid, dhcpResults);
            }
        }
    }

    class DisconnectedMonitorState extends State {
        private boolean mSetStaticIpConfig;

        DisconnectedMonitorState() {
        }

        public void enter() {
            HwHiLog.d(HwSelfCureEngine.TAG, false, "==> ##DisconnectedMonitorState", new Object[0]);
            boolean unused = HwSelfCureEngine.this.mStaticIpCureSuccess = false;
            boolean unused2 = HwSelfCureEngine.this.mIsWifi6ArpSuccess = false;
            boolean unused3 = HwSelfCureEngine.this.mHasTestWifi6Reassoc = false;
            int unused4 = HwSelfCureEngine.this.mNoAutoConnCounter = 0;
            int unused5 = HwSelfCureEngine.this.mNoAutoConnReason = -1;
            WifiConfiguration unused6 = HwSelfCureEngine.this.mNoAutoConnConfig = null;
            WifiConfiguration unused7 = HwSelfCureEngine.this.mSelfCureConfig = null;
            String unused8 = HwSelfCureEngine.this.mConnectionCureConfigKey = null;
            HwSelfCureEngine.this.mIsWifiBackground.set(false);
            HwSelfCureEngine.this.mSelfCureOngoing.set(false);
            this.mSetStaticIpConfig = false;
            long unused9 = HwSelfCureEngine.this.mConnectedTimeMills = 0;
            HwSelfCureEngine.this.mDhcpOfferPackets.clear();
            HwSelfCureEngine.this.mDhcpResultsTestDone.clear();
            HwSelfCureEngine.this.mRouterInternetDetector.notifyDisconnected();
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 101) {
                WifiConfiguration wifiConfiguration = null;
                if (i != 102) {
                    if (i != 117) {
                        if (i == 124) {
                            String currentBssid = WifiProCommonUtils.getCurrentBssid(HwSelfCureEngine.this.mWifiManager);
                            if (message.obj != null) {
                                wifiConfiguration = (WifiConfiguration) message.obj;
                            }
                            this.mSetStaticIpConfig = handleIpConfigLost(currentBssid, wifiConfiguration);
                            return true;
                        } else if (i == HwSelfCureEngine.CMD_USER_ENTER_WLAN_SETTINGS) {
                            List<Integer> enabledReasons = new ArrayList<>();
                            enabledReasons.add(3);
                            enabledReasons.add(2);
                            enabledReasons.add(4);
                            HwSelfCureEngine.this.enableAllNetworksByEnterSettings(enabledReasons);
                            return true;
                        } else if (i == HwSelfCureEngine.CMD_USER_PRESENT_RCVD) {
                            handleUserPresentEvent();
                            return true;
                        } else if (i != HwSelfCureEngine.CMD_BSSID_DHCP_FAILED_EVENT) {
                            switch (i) {
                                case 104:
                                    if (HwSelfCureEngine.this.hasMessages(HwSelfCureEngine.CMD_INVALID_DHCP_OFFER_EVENT)) {
                                        HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_INVALID_DHCP_OFFER_EVENT msg removed because of rcv other Dhcp Offer.", new Object[0]);
                                        HwSelfCureEngine.this.removeMessages(HwSelfCureEngine.CMD_INVALID_DHCP_OFFER_EVENT);
                                    }
                                    HwSelfCureEngine.this.handleNetworkConnected();
                                    return true;
                                case 105:
                                    HwSelfCureEngine.this.handleWifiDisabled(false);
                                    return true;
                                case 106:
                                    HwSelfCureEngine.this.handleWifiEnabled();
                                    return true;
                                case 107:
                                    HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                                    if (message.obj != null) {
                                        wifiConfiguration = (WifiConfiguration) message.obj;
                                    }
                                    hwSelfCureEngine.handleNetworkRemoved(wifiConfiguration);
                                    return true;
                                default:
                                    return false;
                            }
                        } else {
                            handleBssidDhcpFailed((WifiConfiguration) message.obj);
                            return true;
                        }
                    } else if (!this.mSetStaticIpConfig) {
                        return true;
                    } else {
                        this.mSetStaticIpConfig = false;
                        HwSelfCureEngine.this.updateScCHRCount(12);
                        HwSelfCureEngine.this.updateScCHRCount(25);
                        HwSelfCureEngine.this.requestArpConflictTest(HwSelfCureEngine.this.syncGetDhcpResults());
                        return true;
                    }
                } else if (HwSelfCureUtils.isOnWlanSettings(HwSelfCureEngine.this.mContext)) {
                    return true;
                } else {
                    if (message.obj != null) {
                        wifiConfiguration = (WifiConfiguration) message.obj;
                    }
                    trySelfCureSelectedNetwork(wifiConfiguration);
                    return true;
                }
            } else if (HwSelfCureEngine.this.isConnectingOrConnected() || HwSelfCureUtils.isOnWlanSettings(HwSelfCureEngine.this.mContext)) {
                return true;
            } else {
                List<ScanResult> scanResults = HwSelfCureEngine.this.mWifiManager.getScanResults();
                updateAutoConnFailedNetworks(scanResults);
                HwSelfCureUtils.selectDisabledNetworks(scanResults, WifiproUtils.getAllConfiguredNetworks(), HwSelfCureEngine.this.autoConnectFailedNetworks, HwSelfCureEngine.this.autoConnectFailedNetworksRssi);
                selectHighestFailedNetworkAndCure();
                return true;
            }
        }

        private void handleUserPresentEvent() {
            if (HwSelfCureEngine.this.mWifiManager != null && HwSelfCureEngine.this.mWifiManager.isWifiEnabled() && !HwSelfCureEngine.this.isConnectingOrConnected() && !WifiProCommonUtils.isQueryActivityMatched(HwSelfCureEngine.this.mContext, WifiProCommonUtils.HUAWEI_SETTINGS_WLAN)) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "ENTER: handleUserPresentEvent()", new Object[0]);
                List<Integer> enabledReasons = new ArrayList<>();
                enabledReasons.add(2);
                enabledReasons.add(3);
                enabledReasons.add(4);
                HwSelfCureEngine.this.enableAllNetworksByReason(enabledReasons, true);
            }
        }

        private boolean handleIpConfigLost(String bssid, WifiConfiguration config) {
            if (bssid == null || config == null) {
                return false;
            }
            String dhcpResults = null;
            PortalDataBaseManager database = PortalDataBaseManager.getInstance(HwSelfCureEngine.this.mContext);
            if (database != null) {
                dhcpResults = database.syncQueryDhcpResultsByBssid(bssid);
            }
            if (dhcpResults == null) {
                dhcpResults = config.lastDhcpResults;
            }
            if (dhcpResults == null) {
                return false;
            }
            HwSelfCureEngine.this.requestUseStaticIpConfig(WifiProCommonUtils.dhcpResults2StaticIpConfig(dhcpResults));
            return true;
        }

        private void handleBssidDhcpFailed(WifiConfiguration config) {
            synchronized (HwSelfCureEngine.this.mDhcpFailedBssidLock) {
                String bssid = WifiProCommonUtils.getCurrentBssid(HwSelfCureEngine.this.mWifiManager);
                if (bssid != null && !HwSelfCureEngine.this.mDhcpFailedBssids.contains(bssid)) {
                    int bssidCnt = WifiProCommonUtils.getBssidCounter(config, HwSelfCureEngine.this.getScanResults());
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "handleBssidDhcpFailed, bssidCnt = %{public}d", new Object[]{Integer.valueOf(bssidCnt)});
                    if (bssidCnt >= 2) {
                        HwHiLog.d(HwSelfCureEngine.TAG, false, "handleBssidDhcpFailed, add key = %{public}s", new Object[]{StringUtilEx.safeDisplaySsid(config.getPrintableSsid())});
                        HwSelfCureEngine.this.mDhcpFailedBssids.add(bssid);
                        if (config.configKey() != null && !HwSelfCureEngine.this.mDhcpFailedConfigKeys.contains(config.configKey())) {
                            HwSelfCureEngine.this.mDhcpFailedConfigKeys.add(config.configKey());
                        }
                    }
                }
            }
        }

        private void updateAutoConnFailedNetworks(List<ScanResult> scanResults) {
            for (String itemRefreshedNetworksKey : HwSelfCureUtils.getRefreshedCureFailedNetworks(HwSelfCureEngine.this.networkCureFailedHistory)) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "updateAutoConnFailedNetworks, refreshed cure failed network, currKey = %{private}s", new Object[]{itemRefreshedNetworksKey});
                HwSelfCureEngine.this.networkCureFailedHistory.remove(itemRefreshedNetworksKey);
            }
            for (String item : HwSelfCureUtils.searchUnstableNetworks(HwSelfCureEngine.this.autoConnectFailedNetworks, scanResults)) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "updateAutoConnFailedNetworks, remove it due to signal unstable, currKey = %{private}s", new Object[]{item});
                HwSelfCureEngine.this.autoConnectFailedNetworks.remove(item);
                HwSelfCureEngine.this.autoConnectFailedNetworksRssi.remove(item);
            }
        }

        private void selectHighestFailedNetworkAndCure() {
            if (HwSelfCureEngine.this.autoConnectFailedNetworks.size() == 0) {
                int unused = HwSelfCureEngine.this.mNoAutoConnCounter = 0;
            } else if (HwSelfCureEngine.access$4004(HwSelfCureEngine.this) < 3) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "selectHighestFailedNetworkAndCure, MAX_FAILED_CURE unmatched, wait more time for self cure.", new Object[0]);
            } else {
                WifiConfiguration bestSelfCureCandidate = HwSelfCureUtils.selectHighestFailedNetwork(HwSelfCureEngine.this.networkCureFailedHistory, HwSelfCureEngine.this.autoConnectFailedNetworks, HwSelfCureEngine.this.autoConnectFailedNetworksRssi);
                if (bestSelfCureCandidate != null) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "selectHighestFailedNetworkAndCure, delay 1s to self cure the selected candidate = %{public}s", new Object[]{StringUtilEx.safeDisplaySsid(bestSelfCureCandidate.getPrintableSsid())});
                    Message dmsg = Message.obtain();
                    dmsg.what = 102;
                    dmsg.obj = bestSelfCureCandidate;
                    HwSelfCureEngine.this.sendMessageDelayed(dmsg, 100);
                }
            }
        }

        private void trySelfCureSelectedNetwork(WifiConfiguration config) {
            if (config != null && config.networkId != -1 && !HwSelfCureEngine.this.isConnectingOrConnected()) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "ENTER: trySelfCureSelectedNetwork(), config = %{public}s", new Object[]{StringUtilEx.safeDisplaySsid(config.getPrintableSsid())});
                if (WifiCommonUtils.doesNotWifiConnectRejectByCust(config.getNetworkSelectionStatus(), config.SSID, HwSelfCureEngine.this.mContext)) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "trySelfCureSelectedNetwork can not connect wifi", new Object[0]);
                    return;
                }
                if (WifiProCommonUtils.isWifiProSwitchOn(HwSelfCureEngine.this.mContext)) {
                    if (WifiProCommonUtils.isOpenAndPortal(config) || WifiProCommonUtils.isOpenAndMaybePortal(config)) {
                        HwSelfCureEngine.this.setWifiBackgroundReason(0);
                        HwHiLog.d(HwSelfCureEngine.TAG, false, "self cure at background, due to [maybe] portal, candidate = %{public}s", new Object[]{StringUtilEx.safeDisplaySsid(config.getPrintableSsid())});
                    } else if (config.noInternetAccess && !WifiProCommonUtils.allowWifiConfigRecovery(config.internetHistory)) {
                        HwSelfCureEngine.this.setWifiBackgroundReason(3);
                        HwHiLog.d(HwSelfCureEngine.TAG, false, "trySelfCureSelectedNetwork, self cure at background, due to no internet, candidate = %{private}s", new Object[]{config.configKey()});
                    }
                }
                Bundle data = new Bundle();
                data.putInt("networkId", config.networkId);
                data.putInt("CallingUid", Binder.getCallingUid());
                data.putString("bssid", null);
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 22, data);
                int disableReason = config.getNetworkSelectionStatus().getNetworkSelectionDisableReason();
                int unused = HwSelfCureEngine.mDisableReason = disableReason;
                HwHiLog.d(HwSelfCureEngine.TAG, false, "disableReason is: %{public}d", new Object[]{Integer.valueOf(disableReason)});
                updateSelfCure(disableReason);
                String unused2 = HwSelfCureEngine.this.mConnectionCureConfigKey = config.configKey();
                Message dmsg = Message.obtain();
                dmsg.what = 103;
                dmsg.obj = config.configKey();
                HwSelfCureEngine.this.sendMessageDelayed(dmsg, 20000);
                HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                hwSelfCureEngine.transitionTo(hwSelfCureEngine.mConnectionSelfCureState);
            }
        }

        private void updateSelfCure(int disableReason) {
            if (HwSelfCureEngine.this.uploadManager != null) {
                if (disableReason == 11) {
                    HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.SELF_CURE_EVENT, HwSelfCureEngine.SELF_CURE_STOP_USE);
                } else if (disableReason == 2) {
                    HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.SELF_CURE_EVENT, HwSelfCureEngine.SELF_CURE_USER_REJECT);
                } else if (disableReason == 13) {
                    HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.SELF_CURE_EVENT, HwSelfCureEngine.SELF_CURE_WRONG_PWD);
                } else if (disableReason == 4) {
                    HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.SELF_CURE_EVENT, HwSelfCureEngine.SELF_CURE_INTERNET_ERROR);
                } else {
                    HwHiLog.w(HwSelfCureEngine.TAG, false, "selfCureEvent StopUse, Rejected, WrongPassword, InternetError is not here", new Object[0]);
                }
            }
        }
    }

    class ConnectionSelfCureState extends State {
        ConnectionSelfCureState() {
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            WifiConfiguration wifiConfiguration = null;
            String str = null;
            if (i == 107) {
                HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                if (message.obj != null) {
                    wifiConfiguration = (WifiConfiguration) message.obj;
                }
                hwSelfCureEngine.handleNetworkRemoved(wifiConfiguration);
            } else if (i == 108) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_NETWORK_DISCONNECTED_RCVD during connection self cure state.", new Object[0]);
                if (HwSelfCureEngine.this.hasMessages(119)) {
                    HwSelfCureEngine.this.removeMessages(119);
                }
                handleConnSelfCureFailed(HwSelfCureEngine.this.mConnectionCureConfigKey);
            } else if (i != 119) {
                switch (i) {
                    case 103:
                        if (message.obj != null) {
                            str = (String) message.obj;
                        }
                        handleConnSelfCureFailed(str);
                        break;
                    case 104:
                        if (HwSelfCureEngine.this.hasMessages(103)) {
                            HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_CURE_CONNECTED_TIMEOUT msg removed", new Object[0]);
                            HwSelfCureEngine.this.removeMessages(103);
                        }
                        updateSelfCureSucc();
                        HwSelfCureEngine.this.handleNetworkConnected();
                        break;
                    case 105:
                        if (HwSelfCureEngine.this.hasMessages(103)) {
                            HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_CURE_CONNECTED_TIMEOUT msg removed", new Object[0]);
                            HwSelfCureEngine.this.removeMessages(103);
                        }
                        HwSelfCureEngine.this.handleWifiDisabled(true);
                        break;
                    default:
                        return false;
                }
            } else {
                boolean unused = HwSelfCureEngine.this.updateConnSelfCureFailedHistory();
            }
            return true;
        }

        private void handleConnSelfCureFailed(String configKey) {
            HwHiLog.d(HwSelfCureEngine.TAG, false, "ENTER: handleConnSelfCureFailed(), configKey = %{private}s", new Object[]{configKey});
            if (configKey != null) {
                int unused = HwSelfCureEngine.this.mNoAutoConnCounter = 0;
                HwSelfCureEngine.this.autoConnectFailedNetworks.clear();
                HwSelfCureEngine.this.autoConnectFailedNetworksRssi.clear();
                CureFailedNetworkInfo cureHistory = (CureFailedNetworkInfo) HwSelfCureEngine.this.networkCureFailedHistory.get(configKey);
                if (cureHistory != null) {
                    cureHistory.cureFailedCounter++;
                    cureHistory.lastCureFailedTime = System.currentTimeMillis();
                } else {
                    HwSelfCureEngine.this.networkCureFailedHistory.put(configKey, new CureFailedNetworkInfo(configKey, 1, System.currentTimeMillis()));
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "handleConnSelfCureFailed, networkCureFailedHistory added, configKey = %{private}s", new Object[]{configKey});
                }
            }
            if (HwSelfCureEngine.this.mNoAutoConnReason != -1) {
                HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                hwSelfCureEngine.updateScCHRCount(hwSelfCureEngine.mNoAutoConnReason);
            }
            HwSelfCureEngine hwSelfCureEngine2 = HwSelfCureEngine.this;
            hwSelfCureEngine2.transitionTo(hwSelfCureEngine2.mDisconnectedMonitorState);
        }

        private void updateSelfCureSucc() {
            if (HwSelfCureEngine.this.uploadManager != null) {
                if (HwSelfCureEngine.mDisableReason == 11) {
                    HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.SELF_CURE_SUCC_EVENT, HwSelfCureEngine.SELF_CURE_STOP_USE);
                } else if (HwSelfCureEngine.mDisableReason == 2) {
                    HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.SELF_CURE_SUCC_EVENT, HwSelfCureEngine.SELF_CURE_USER_REJECT);
                } else if (HwSelfCureEngine.mDisableReason == 13) {
                    HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.SELF_CURE_SUCC_EVENT, HwSelfCureEngine.SELF_CURE_WRONG_PWD);
                } else if (HwSelfCureEngine.mDisableReason == 4) {
                    HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.SELF_CURE_SUCC_EVENT, HwSelfCureEngine.SELF_CURE_INTERNET_ERROR);
                } else {
                    HwHiLog.w(HwSelfCureEngine.TAG, false, "CHR updateSelfCureSucc disable reason is not match", new Object[0]);
                }
                int unused = HwSelfCureEngine.mDisableReason = 0;
            }
        }
    }

    class InternetSelfCureState extends State {
        private String[] mAssignedDnses = null;
        private int mConfigAuthType = -1;
        private boolean mConfigStaticIp4GatewayChanged = false;
        private boolean mConfigStaticIp4MultiDhcpServer = false;
        private int mCurrentAbnormalType;
        private String mCurrentBssid = null;
        private String mCurrentGateway;
        private int mCurrentRssi;
        private int mCurrentSelfCureLevel;
        private boolean mDelayedReassocSelfCure = false;
        private boolean mDelayedResetSelfCure = false;
        private boolean mFinalSelfCureUsed = false;
        private boolean mHasInternetRecently;
        private long mLastHasInetTimeMillis;
        private int mLastMultiGwselfFailedType;
        private int mLastSelfCureLevel;
        private boolean mPortalUnthenEver;
        private int mRenewDhcpCount;
        private int mSelfCureFailedCounter;
        private InternetSelfCureHistoryInfo mSelfCureHistoryInfo;
        private boolean mSetStaticIp4InvalidIp = false;
        private List<Integer> mTestedSelfCureLevel = new ArrayList();
        private String mUnconflictedIp;
        private boolean mUsedMultiGwSelfcure = false;
        private boolean mUserSetStaticIpConfig;

        InternetSelfCureState() {
        }

        public void enter() {
            HwHiLog.d(HwSelfCureEngine.TAG, false, "==> ##InternetSelfCureState", new Object[0]);
            this.mCurrentRssi = -200;
            this.mSelfCureFailedCounter = 0;
            int i = -1;
            this.mCurrentAbnormalType = -1;
            this.mLastSelfCureLevel = -1;
            this.mCurrentSelfCureLevel = 200;
            this.mHasInternetRecently = false;
            this.mPortalUnthenEver = false;
            this.mUserSetStaticIpConfig = false;
            this.mCurrentGateway = getCurrentGateway();
            this.mTestedSelfCureLevel.clear();
            this.mFinalSelfCureUsed = false;
            this.mDelayedReassocSelfCure = false;
            this.mDelayedResetSelfCure = false;
            this.mSetStaticIp4InvalidIp = false;
            this.mUnconflictedIp = null;
            this.mRenewDhcpCount = 0;
            this.mLastMultiGwselfFailedType = -1;
            this.mUsedMultiGwSelfcure = false;
            WifiInfo wifiInfo = HwSelfCureEngine.this.mWifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                this.mCurrentRssi = wifiInfo.getRssi();
                this.mCurrentBssid = wifiInfo.getBSSID();
                HwHiLog.d(HwSelfCureEngine.TAG, false, "InternetSelfCureState, network = %{public}s, signal rssi = %{public}d", new Object[]{StringUtilEx.safeDisplaySsid(wifiInfo.getSSID()), Integer.valueOf(this.mCurrentRssi)});
            }
            HwSelfCureEngine.this.sendMessageDelayed(HwSelfCureEngine.CMD_PERIODIC_ARP_DETECTED, 60000);
            WifiConfiguration config = WifiProCommonUtils.getCurrentWifiConfig(HwSelfCureEngine.this.mWifiManager);
            if (config != null) {
                this.mSelfCureHistoryInfo = HwSelfCureUtils.string2InternetSelfCureHistoryInfo(config.internetSelfCureHistory);
                this.mHasInternetRecently = WifiProCommonUtils.matchedRequestByHistory(config.internetHistory, 100);
                this.mPortalUnthenEver = WifiProCommonUtils.matchedRequestByHistory(config.internetHistory, 102);
                this.mUserSetStaticIpConfig = config.getIpAssignment() != null && config.getIpAssignment() == IpConfiguration.IpAssignment.STATIC;
                this.mLastHasInetTimeMillis = config.lastHasInternetTimestamp;
                if (config.allowedKeyManagement.cardinality() <= 1) {
                    i = config.getAuthType();
                }
                this.mConfigAuthType = i;
                HwHiLog.d(HwSelfCureEngine.TAG, false, "InternetSelfCureState, hasInternet = %{public}s, portalUnthenEver = %{public}s, userSetStaticIp = %{public}s, historyInfo = %{public}s, gw = %{public}s", new Object[]{String.valueOf(this.mHasInternetRecently), String.valueOf(this.mPortalUnthenEver), String.valueOf(this.mUserSetStaticIpConfig), this.mSelfCureHistoryInfo, WifiProCommonUtils.safeDisplayIpAddress(this.mCurrentGateway)});
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 116) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_IP_CONFIG_TIMEOUT during self cure state. currentAbnormalType = %{public}d", new Object[]{Integer.valueOf(this.mCurrentAbnormalType)});
                HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 47, new Bundle());
                WifiProStateMachine wifiProStateMachine = WifiProStateMachine.getWifiProStateMachineImpl();
                if (wifiProStateMachine != null) {
                    wifiProStateMachine.notifyRenewDhcpTimeoutForWifiPro();
                }
                if (this.mCurrentAbnormalType == 301 && WifiProCommonUtils.isEncryptedAuthType(this.mConfigAuthType) && WifiProCommonUtils.getBssidCounter(HwSelfCureEngine.this.mSelfCureConfig, HwSelfCureEngine.this.getScanResults()) <= 3 && !this.mFinalSelfCureUsed) {
                    this.mFinalSelfCureUsed = true;
                    HwSelfCureEngine.this.sendMessage(114, HwSelfCureUtils.RESET_LEVEL_DEAUTH_BSSID, 0);
                }
            } else if (i == 117) {
                if (HwSelfCureEngine.this.hasMessages(116)) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_IP_CONFIG_TIMEOUT msg removed because of ip config success.", new Object[0]);
                    HwSelfCureEngine.this.removeMessages(116);
                    WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 47, new Bundle());
                    this.mCurrentGateway = getCurrentGateway();
                    HwSelfCureEngine.this.sendMessageDelayed(113, 2000);
                }
                if (HwSelfCureEngine.this.hasMessages(HwSelfCureEngine.CMD_INVALID_DHCP_OFFER_EVENT)) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_INVALID_DHCP_OFFER_EVENT msg removed because of rcv other Dhcp Offer.", new Object[0]);
                    HwSelfCureEngine.this.removeMessages(HwSelfCureEngine.CMD_INVALID_DHCP_OFFER_EVENT);
                }
            } else if (i == 120) {
                HwSelfCureUtils.updateSelfCureConnectHistoryInfo(this.mSelfCureHistoryInfo, this.mCurrentSelfCureLevel, false);
                updateWifiConfig(HwSelfCureEngine.this.mSelfCureConfig);
                HwSelfCureEngine.this.mSelfCureOngoing.set(false);
            } else if (i != 122) {
                if (i == 128) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_P2P_DISCONNECTED_EVENT during self cure state.", new Object[0]);
                    handleRssiChanged();
                } else if (i == HwSelfCureEngine.CMD_HTTP_REACHABLE_RCV) {
                    HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                    if (HwSelfCureEngine.this.mSelfCureConfig != null) {
                        HwSelfCureEngine.this.mSelfCureConfig.internetSelfCureHistory = "0|0|0|0|0|0|0|0|0|0|0|0|0|0";
                        HwSelfCureEngine.this.mSelfCureConfig.validatedInternetAccess = true;
                        HwSelfCureEngine.this.mSelfCureConfig.noInternetAccess = false;
                        HwSelfCureEngine.this.mSelfCureConfig.wifiProNoInternetAccess = false;
                        HwSelfCureEngine.this.mSelfCureConfig.wifiProNoInternetReason = 0;
                        Bundle data = new Bundle();
                        data.putInt("messageWhat", 131672);
                        data.putParcelable("messageObj", HwSelfCureEngine.this.mSelfCureConfig);
                        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 28, data);
                    }
                    HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                    hwSelfCureEngine.transitionTo(hwSelfCureEngine.mConnectedMonitorState);
                } else if (i == HwSelfCureEngine.CMD_PERIODIC_ARP_DETECTED) {
                    HwSelfCureEngine.this.periodicArpDetection();
                } else if (i == HwSelfCureEngine.CMD_MULTIGW_SELFCURE) {
                    multiGatewaySelfcure();
                } else if (i != HwSelfCureEngine.CMD_ARP_FAILED_DETECTED) {
                    switch (i) {
                        case 108:
                            HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_NETWORK_DISCONNECTED_RCVD during self cure state.", new Object[0]);
                            HwSelfCureEngine.this.removeMessages(113);
                            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 47, new Bundle());
                            HwSelfCureEngine hwSelfCureEngine2 = HwSelfCureEngine.this;
                            hwSelfCureEngine2.transitionTo(hwSelfCureEngine2.mDisconnectedMonitorState);
                            break;
                        case 109:
                            this.mCurrentRssi = message.arg1;
                            handleRssiChanged();
                            break;
                        case 110:
                            handleRoamingDetected((String) message.obj);
                            break;
                        default:
                            switch (i) {
                                case 112:
                                    HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                                    if (HwSelfCureEngine.this.isSuppOnCompletedState()) {
                                        selectSelfCureByFailedReason(message.arg1);
                                        break;
                                    }
                                    break;
                                case 113:
                                    if (this.mCurrentSelfCureLevel == 205) {
                                        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 62, new Bundle());
                                    }
                                    HwSelfCureUtils.updateSelfCureConnectHistoryInfo(this.mSelfCureHistoryInfo, this.mCurrentSelfCureLevel, true);
                                    if (!confirmInternetSelfCure(this.mCurrentSelfCureLevel)) {
                                        HwSelfCureEngine.this.notifyVoWiFiSelCureEnd(-1);
                                        break;
                                    } else {
                                        this.mCurrentSelfCureLevel = 200;
                                        this.mSelfCureFailedCounter = 0;
                                        this.mHasInternetRecently = true;
                                        HwSelfCureEngine.this.notifyVoWiFiSelCureEnd(0);
                                        break;
                                    }
                                case 114:
                                    if (HwSelfCureEngine.this.isSuppOnCompletedState()) {
                                        this.mCurrentSelfCureLevel = message.arg1;
                                        selfCureWifiLink(message.arg1);
                                        HwSelfCureEngine.this.notifyVoWiFiSelCureBegin();
                                        break;
                                    }
                                    break;
                                default:
                                    switch (i) {
                                        case HwSelfCureEngine.CMD_INVALID_DHCP_OFFER_EVENT /*{ENCODED_INT: 130}*/:
                                            this.mSetStaticIp4InvalidIp = HwSelfCureEngine.this.handleInvalidDhcpOffer(this.mUnconflictedIp);
                                            break;
                                        case 131:
                                            HwSelfCureEngine.this.updateScCHRCount(28);
                                            break;
                                        case 132:
                                            HwSelfCureEngine.this.updateScCHRCount(27);
                                            break;
                                        default:
                                            return false;
                                    }
                            }
                    }
                } else {
                    HwSelfCureEngine.this.handleArpFailedDetected();
                }
            } else if (((Boolean) message.obj).booleanValue()) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "CMD_INTERNET_FAILURE_DETECTED rcvd under InternetSelfCureState, delete dhcp cache", new Object[0]);
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 45, new Bundle());
            }
            return true;
        }

        public void exit() {
            HwHiLog.d(HwSelfCureEngine.TAG, false, "InternetSelfCureState exit", new Object[0]);
            int unused = HwSelfCureEngine.mSelfCureReason = 0;
            this.mConfigStaticIp4MultiDhcpServer = false;
            HwSelfCureEngine.this.removeMessages(HwSelfCureEngine.CMD_PERIODIC_ARP_DETECTED);
        }

        private void uploadCurrentAbnormalStatistics() {
            int i = this.mCurrentAbnormalType;
            if (i != -1) {
                HwSelfCureEngine.this.updateScCHRCount(i);
                this.mCurrentAbnormalType = -1;
            }
        }

        private void uploadInternetCureSuccCounter(int selfCureType) {
            uploadCurrentAbnormalStatistics();
            int chrType = -1;
            if (selfCureType == 201) {
                chrType = 4;
            } else if (selfCureType == 202) {
                chrType = 5;
            } else if (selfCureType == 203) {
                chrType = 6;
            } else if (selfCureType == 207) {
                chrType = 30;
            } else if (selfCureType == 204) {
                chrType = 7;
            } else if (selfCureType == 205) {
                int i = this.mLastSelfCureLevel;
                if (i == 201) {
                    chrType = 20;
                } else if (i == 202) {
                    chrType = 21;
                } else if (i == 203) {
                    chrType = 22;
                } else if (i == 204) {
                    chrType = 8;
                }
            } else if (selfCureType == 208) {
                chrType = 8;
            }
            if (chrType != -1) {
                HwSelfCureEngine.this.updateScCHRCount(chrType);
            }
        }

        private void handleInternetFailedAndUserSetStaticIp(int internetFailedType) {
            if (!this.mHasInternetRecently || !HwSelfCureUtils.selectedSelfCureAcceptable(this.mSelfCureHistoryInfo, 205)) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, " user set static ip config, ignore to update config for user.", new Object[0]);
                if (!HwSelfCureEngine.this.mInternetUnknown) {
                    this.mCurrentAbnormalType = HwSelfCureUtils.RESET_REJECTED_BY_STATIC_IP_ENABLED;
                    uploadCurrentAbnormalStatistics();
                    return;
                }
                return;
            }
            if (internetFailedType == 303) {
                this.mLastSelfCureLevel = 201;
            } else if (internetFailedType == 301) {
                this.mLastSelfCureLevel = 202;
            } else if (internetFailedType == 302) {
                this.mLastSelfCureLevel = 203;
            }
            HwSelfCureEngine.this.sendMessage(114, 205, 0);
        }

        private int selectBestSelfCureSolution(int internetFailedType) {
            boolean multipleDhcpServer = HwSelfCureEngine.this.mDhcpOfferPackets.size() >= 2;
            long j = this.mLastHasInetTimeMillis;
            boolean noInternetWhenConnected = j <= 0 || j < HwSelfCureEngine.this.mConnectedTimeMills;
            HwHiLog.d(HwSelfCureEngine.TAG, false, "selectBestSelfCureSolution, multipleDhcpServer = %{public}s, noInternetWhenConnected = %{public}s", new Object[]{String.valueOf(multipleDhcpServer), String.valueOf(noInternetWhenConnected)});
            if (multipleDhcpServer && noInternetWhenConnected && getNextTestDhcpResults() != null && HwSelfCureUtils.selectedSelfCureAcceptable(this.mSelfCureHistoryInfo, 203) && (internetFailedType == 303 || internetFailedType == HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP)) {
                this.mConfigStaticIp4MultiDhcpServer = true;
                return 203;
            } else if (internetFailedType == 302 && multipleDhcpServer && getNextTestDhcpResults() != null && HwSelfCureUtils.selectedSelfCureAcceptable(this.mSelfCureHistoryInfo, 203)) {
                this.mConfigStaticIp4MultiDhcpServer = true;
                return 203;
            } else if (internetFailedType == 302 && WifiProCommonUtils.isEncryptedAuthType(this.mConfigAuthType) && HwSelfCureUtils.selectedSelfCureAcceptable(this.mSelfCureHistoryInfo, 203)) {
                HwSelfCureEngine.this.mDhcpResultsTestDone.add(this.mCurrentGateway);
                this.mConfigStaticIp4GatewayChanged = true;
                return 203;
            } else if (internetFailedType == HwSelfCureEngine.INTERNET_FAILED_INVALID_IP) {
                return HwSelfCureUtils.RESET_LEVEL_RECONNECT_4_INVALID_IP;
            } else {
                if (internetFailedType == 301) {
                    if (HwSelfCureUtils.selectedSelfCureAcceptable(this.mSelfCureHistoryInfo, 202)) {
                        return 202;
                    }
                    return 200;
                } else if (internetFailedType == 303) {
                    if (HwSelfCureUtils.selectedSelfCureAcceptable(this.mSelfCureHistoryInfo, 201)) {
                        return 201;
                    }
                    return 200;
                } else if (internetFailedType != HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP || !HwSelfCureUtils.selectedSelfCureAcceptable(this.mSelfCureHistoryInfo, 204)) {
                    return 200;
                } else {
                    return 204;
                }
            }
        }

        private boolean isNeedMultiGatewaySelfcure() {
            HwHiLog.d(HwSelfCureEngine.TAG, false, "isNeedMultiGatewaySelfcure mUsedMultiGwSelfcure = %{public}s", new Object[]{String.valueOf(this.mUsedMultiGwSelfcure)});
            if (this.mUsedMultiGwSelfcure) {
                return false;
            }
            return HwSelfCureEngine.this.multiGateway();
        }

        private void multiGatewaySelfcure() {
            if (HwSelfCureEngine.this.isSuppOnCompletedState()) {
                HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                this.mUsedMultiGwSelfcure = true;
                Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 70, new Bundle());
                if (result != null) {
                    String mac = result.getString("mac");
                    String gateway = result.getString(HwSelfCureEngine.GATEWAY);
                    if (mac == null || gateway == null) {
                        HwHiLog.d(HwSelfCureEngine.TAG, false, "multi gateway selfcure failed.", new Object[0]);
                        int i = this.mLastMultiGwselfFailedType;
                        if (i != -1) {
                            selectSelfCureByFailedReason(i);
                        }
                    } else {
                        HwHiLog.d(HwSelfCureEngine.TAG, false, "start multi gateway selfcure", new Object[0]);
                        Bundle data = new Bundle();
                        data.putString(HwSelfCureEngine.GATEWAY, gateway);
                        data.putString("mac", mac);
                        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 57, data);
                        HwSelfCureEngine.this.flushVmDnsCache();
                        if (!HwSelfCureEngine.this.isHttpReachable(false)) {
                            HwHiLog.d(HwSelfCureEngine.TAG, false, "multi gateway selfcure failed , delStaticARP!", new Object[0]);
                            data.clear();
                            data.putString(HwSelfCureEngine.GATEWAY, gateway);
                            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 58, data);
                            HwSelfCureEngine.this.sendMessage(HwSelfCureEngine.CMD_MULTIGW_SELFCURE);
                        } else {
                            HwHiLog.d(HwSelfCureEngine.TAG, false, "multi gateway selfcure success!", new Object[0]);
                            HwSelfCureEngine.this.notifyHttpReachableForWifiPro(true);
                            HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                            hwSelfCureEngine.transitionTo(hwSelfCureEngine.mConnectedMonitorState);
                        }
                    }
                    HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                }
            }
        }

        private void selectSelfCureByFailedReason(int internetFailedType) {
            HwHiLog.d(HwSelfCureEngine.TAG, false, "selectSelfCureByFailedReason, internetFailedType = %{public}d, userSetStaticIp = %{public}s", new Object[]{Integer.valueOf(internetFailedType), String.valueOf(this.mUserSetStaticIpConfig)});
            if (isNeedMultiGatewaySelfcure()) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "Start multi gateway selfcure.", new Object[0]);
                this.mLastMultiGwselfFailedType = internetFailedType;
                HwSelfCureEngine.this.sendMessage(HwSelfCureEngine.CMD_MULTIGW_SELFCURE);
            } else if (!this.mUserSetStaticIpConfig || !(internetFailedType == 303 || internetFailedType == 302 || internetFailedType == 301)) {
                int requestSelfCureLevel = selectBestSelfCureSolution(internetFailedType);
                if (requestSelfCureLevel != 200) {
                    this.mCurrentAbnormalType = internetFailedType;
                    HwSelfCureEngine.this.sendMessage(114, requestSelfCureLevel, 0);
                } else if (HwSelfCureUtils.selectedSelfCureAcceptable(this.mSelfCureHistoryInfo, 205)) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "selectSelfCureByFailedReason, use wifi reset to cure this failed type = %{public}d", new Object[]{Integer.valueOf(internetFailedType)});
                    this.mCurrentAbnormalType = internetFailedType;
                    if (internetFailedType == 303) {
                        this.mLastSelfCureLevel = 201;
                    } else if (internetFailedType == 301) {
                        this.mLastSelfCureLevel = 202;
                    } else if (internetFailedType == HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP) {
                        this.mLastSelfCureLevel = 204;
                    }
                    HwSelfCureEngine.this.sendMessage(114, 205, 0);
                } else {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "selectSelfCureByFailedReason, no usable self cure for this failed type = %{public}d", new Object[]{Integer.valueOf(internetFailedType)});
                    handleHttpUnreachableFinally();
                }
            } else {
                handleInternetFailedAndUserSetStaticIp(internetFailedType);
            }
        }

        private boolean confirmInternetSelfCure(int currentCureLevel) {
            int curCureLevel = currentCureLevel;
            HwHiLog.d(HwSelfCureEngine.TAG, false, "confirmInternetSelfCure, cureLevel = %{public}d, last failed counter = %{public}d, finally = %{public}s", new Object[]{Integer.valueOf(curCureLevel), Integer.valueOf(this.mSelfCureFailedCounter), String.valueOf(this.mFinalSelfCureUsed)});
            if (curCureLevel != 200) {
                if (!HwSelfCureEngine.this.isHttpReachable(true)) {
                    if (curCureLevel == 201 && HwSelfCureEngine.this.mInternetUnknown) {
                        HwSelfCureUtils.requestUpdateDnsServers(new ArrayList(Arrays.asList(this.mAssignedDnses)));
                    }
                    this.mSelfCureFailedCounter++;
                    HwSelfCureUtils.updateSelfCureHistoryInfo(this.mSelfCureHistoryInfo, curCureLevel, false);
                    updateWifiConfig(null);
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "HTTP unreachable, self cure failed for %{public}d, selfCureHistoryInfo = %{public}s", new Object[]{Integer.valueOf(curCureLevel), this.mSelfCureHistoryInfo});
                    HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                    if (this.mFinalSelfCureUsed) {
                        handleHttpUnreachableFinally();
                        return false;
                    } else if (curCureLevel == 204 && HwSelfCureEngine.this.mHasTestWifi6Reassoc) {
                        Message msg = Message.obtain();
                        msg.what = 112;
                        msg.arg1 = 303;
                        HwSelfCureEngine.this.sendMessage(msg);
                        return false;
                    } else if (curCureLevel != 205) {
                        if (curCureLevel == 203) {
                            if (getNextTestDhcpResults() != null) {
                                this.mLastSelfCureLevel = curCureLevel;
                                HwHiLog.d(HwSelfCureEngine.TAG, false, "HTTP unreachable, and has next dhcp results, try next one.", new Object[0]);
                                HwSelfCureEngine.this.sendMessage(114, 203, 0);
                                return false;
                            }
                            this.mConfigStaticIp4MultiDhcpServer = false;
                            if (selectedSelfCureAcceptable()) {
                                return false;
                            }
                            if (this.mCurrentAbnormalType == 301) {
                                curCureLevel = 202;
                            }
                        } else if ((curCureLevel == 202 || curCureLevel == 201) && HwSelfCureEngine.this.mDhcpOfferPackets.size() >= 2 && getNextTestDhcpResults() != null && HwSelfCureUtils.selectedSelfCureAcceptable(this.mSelfCureHistoryInfo, 203)) {
                            this.mLastSelfCureLevel = curCureLevel;
                            this.mConfigStaticIp4MultiDhcpServer = true;
                            HwHiLog.d(HwSelfCureEngine.TAG, false, "HTTP unreachable, has next dhcp results, try next one for re-dhcp failed.", new Object[0]);
                            HwSelfCureEngine.this.sendMessage(114, 203, 0);
                            return false;
                        }
                        if (hasBeenTested(205) || !HwSelfCureUtils.selectedSelfCureAcceptable(this.mSelfCureHistoryInfo, 205)) {
                            handleHttpUnreachableFinally();
                        } else {
                            this.mLastSelfCureLevel = curCureLevel;
                            HwSelfCureEngine.this.sendMessage(114, 205, 0);
                        }
                    } else {
                        if (HwSelfCureEngine.this.mHasTestWifi6Reassoc) {
                            HwSelfCureEngine.this.sendBlacklistToDriver();
                        }
                        if (getNextTestDhcpResults() == null || hasBeenTested(203)) {
                            handleHttpUnreachableFinally();
                        } else {
                            this.mFinalSelfCureUsed = true;
                            this.mLastSelfCureLevel = curCureLevel;
                            this.mConfigStaticIp4MultiDhcpServer = true;
                            HwHiLog.d(HwSelfCureEngine.TAG, false, "HTTP unreachable, and has next dhcp results, try next one for wifi reset failed.", new Object[0]);
                            HwSelfCureEngine.this.sendMessage(114, 203, 0);
                        }
                    }
                } else {
                    updateChrSelfCureSucc(HwSelfCureEngine.mSelfCureReason, curCureLevel);
                    if (curCureLevel == 201 && HwSelfCureEngine.this.mInternetUnknown) {
                        HwSelfCureUtils.requestUpdateDnsServers(HwSelfCureUtils.getPublicDnsServers());
                    }
                    handleHttpReachableAfterSelfCure(curCureLevel);
                    HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                    hwSelfCureEngine.transitionTo(hwSelfCureEngine.mConnectedMonitorState);
                    return true;
                }
            }
            return false;
        }

        private void updateChrSelfCureSucc(int reason, int currentCureLevel) {
            switch (reason) {
                case 301:
                    addChrRoamingCnt(currentCureLevel);
                    return;
                case 302:
                    addChrMultiDhcpCnt(currentCureLevel);
                    return;
                case 303:
                    addChrDnsCnt(currentCureLevel);
                    return;
                case HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP /*{ENCODED_INT: 304}*/:
                    addChrTcpCnt(currentCureLevel);
                    return;
                default:
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "DHCP offer reason:%{public}d", new Object[]{Integer.valueOf(reason)});
                    return;
            }
        }

        private void addChrDnsCnt(int currentCureLevel) {
            if (HwSelfCureEngine.this.uploadManager != null) {
                if (currentCureLevel == 201) {
                    HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.UNIQUE_CURE_EVENT, "replaceDnsSuccCnt");
                } else if (currentCureLevel == 203) {
                    Bundle dhcpData = new Bundle();
                    dhcpData.putInt("selfCureType", 0);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("dnsCureRecoveryEvent", "dhcpOfferSuccCnt", dhcpData);
                } else if (currentCureLevel == 205) {
                    Bundle chipData = new Bundle();
                    chipData.putInt("selfCureType", 0);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("dnsCureRecoveryEvent", "chipCureSuccCnt", chipData);
                } else {
                    HwHiLog.w(HwSelfCureEngine.TAG, false, "addChrDnsCnt: update DNS selfcure succ cnt, but current Cure Level is not here", new Object[0]);
                }
            }
        }

        private void addChrTcpCnt(int currentCureLevel) {
            if (HwSelfCureEngine.this.uploadManager != null) {
                if (currentCureLevel == 204) {
                    HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.UNIQUE_CURE_EVENT, "reassocSuccCnt");
                } else if (currentCureLevel == 203) {
                    Bundle dhcpData = new Bundle();
                    dhcpData.putInt("selfCureType", 1);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("tcpCureRecoveryEvent", "dhcpOfferSuccCnt", dhcpData);
                } else if (currentCureLevel == 205) {
                    Bundle chipData = new Bundle();
                    chipData.putInt("selfCureType", 1);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("tcpCureRecoveryEvent", "chipCureSuccCnt", chipData);
                } else {
                    HwHiLog.w(HwSelfCureEngine.TAG, false, "addChrTcpCnt: update TCP selfcure succ cnt, but current Cure Level is not here", new Object[0]);
                }
            }
        }

        private void addChrRoamingCnt(int currentCureLevel) {
            if (HwSelfCureEngine.this.uploadManager != null) {
                if (currentCureLevel == 202) {
                    HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.UNIQUE_CURE_EVENT, "reDhcpSuccCnt");
                } else if (currentCureLevel == 203) {
                    Bundle dhcpData = new Bundle();
                    dhcpData.putInt("selfCureType", 2);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("roamCureRecoveryEvent", "dhcpOfferSuccCnt", dhcpData);
                } else if (currentCureLevel == 205) {
                    Bundle chipData = new Bundle();
                    chipData.putInt("selfCureType", 2);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("roamCureRecoveryEvent", "chipCureSuccCnt", chipData);
                } else {
                    HwHiLog.w(HwSelfCureEngine.TAG, false, "addChrRoamingCnt:but current Cure Level is not here", new Object[0]);
                }
            }
        }

        private void addChrMultiDhcpCnt(int currentCureLevel) {
            if (HwSelfCureEngine.this.uploadManager != null) {
                if (currentCureLevel == 203) {
                    Bundle dhcpData = new Bundle();
                    dhcpData.putInt("selfCureType", 3);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("multiCureRecoveryEvent", "dhcpOfferSuccCnt", dhcpData);
                } else if (currentCureLevel == 205) {
                    Bundle chipData = new Bundle();
                    chipData.putInt("selfCureType", 3);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("multiCureRecoveryEvent", "chipCureSuccCnt", chipData);
                } else {
                    HwHiLog.w(HwSelfCureEngine.TAG, false, "addChrMultiDhcpCnt:but current Cure Level is not here", new Object[0]);
                }
            }
        }

        private boolean selectedSelfCureAcceptable() {
            int i = this.mCurrentAbnormalType;
            if (i == 303 || i == 302) {
                this.mLastSelfCureLevel = 201;
                if (HwSelfCureUtils.selectedSelfCureAcceptable(this.mSelfCureHistoryInfo, 201)) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "HTTP unreachable, use dns replace to cure for dns failed.", new Object[0]);
                    HwSelfCureEngine.this.sendMessage(114, 201, 0);
                    return true;
                }
            } else if (i != HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP) {
                return false;
            } else {
                this.mLastSelfCureLevel = 204;
                if (HwSelfCureUtils.selectedSelfCureAcceptable(this.mSelfCureHistoryInfo, 204)) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "HTTP unreachable,  use reassoc to cure for no rx pkt.", new Object[0]);
                    HwSelfCureEngine.this.sendMessage(114, 204, 0);
                    return true;
                }
            }
            return false;
        }

        private boolean hasBeenTested(int cureLevel) {
            for (Integer num : this.mTestedSelfCureLevel) {
                if (num.intValue() == cureLevel) {
                    return true;
                }
            }
            return false;
        }

        private void handleHttpUnreachableFinally() {
            HwSelfCureEngine.this.mSelfCureOngoing.set(false);
            if (!HwSelfCureEngine.this.mInternetUnknown) {
                uploadCurrentAbnormalStatistics();
            }
            HwSelfCureEngine.this.notifyHttpReachableForWifiPro(false);
            HwSelfCureEngine.this.mRouterInternetDetector.notifyNoInternetAfterCure(this.mCurrentGateway, this.mConfigAuthType, HwSelfCureEngine.this.mMobileHotspot);
        }

        private void handleHttpReachableAfterSelfCure(int currentCureLevel) {
            HwHiLog.d(HwSelfCureEngine.TAG, false, "handleHttpReachableAfterSelfCure, cureLevel = %{public}d, HTTP reachable, --> ConnectedMonitorState.", new Object[]{Integer.valueOf(currentCureLevel)});
            HwSelfCureEngine.this.notifyHttpReachableForWifiPro(true);
            HwSelfCureEngine.this.mRouterInternetDetector.notifyInternetAccessRecovery();
            HwSelfCureUtils.updateSelfCureHistoryInfo(this.mSelfCureHistoryInfo, currentCureLevel, true);
            DhcpResults dhcpResults = HwSelfCureEngine.this.syncGetDhcpResults();
            String strDhcpResults = WifiProCommonUtils.dhcpResults2String(dhcpResults, -1);
            WifiConfiguration currentConfig = WifiProCommonUtils.getCurrentWifiConfig(HwSelfCureEngine.this.mWifiManager);
            if (!(currentConfig == null || strDhcpResults == null)) {
                currentConfig.lastDhcpResults = strDhcpResults;
            }
            updateWifiConfig(currentConfig);
            HwSelfCureEngine.this.mSelfCureOngoing.set(false);
            if (this.mSetStaticIp4InvalidIp) {
                HwSelfCureEngine.this.requestArpConflictTest(dhcpResults);
                boolean unused = HwSelfCureEngine.this.mStaticIpCureSuccess = true;
            } else if (currentCureLevel == 203) {
                this.mCurrentAbnormalType = 302;
                HwSelfCureEngine.this.requestArpConflictTest(dhcpResults);
                boolean unused2 = HwSelfCureEngine.this.mStaticIpCureSuccess = true;
            }
            uploadInternetCureSuccCounter(currentCureLevel);
            HwSelfCureEngine.this.sendMessageDelayed(125, strDhcpResults, 500);
        }

        private void handleRssiChanged() {
            if (WifiProCommonUtils.getCurrenSignalLevel(HwSelfCureEngine.this.mWifiManager.getConnectionInfo()) > 2 && !HwSelfCureEngine.this.mSelfCureOngoing.get() && !HwSelfCureEngine.this.mP2pConnected.get()) {
                if (this.mDelayedReassocSelfCure || this.mDelayedResetSelfCure) {
                    HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                    if (!HwSelfCureEngine.this.isHttpReachable(true)) {
                        HwHiLog.d(HwSelfCureEngine.TAG, false, "handleRssiChanged, Http failed, delayedReassoc = %{public}s, delayedReset = %{public}s", new Object[]{String.valueOf(this.mDelayedReassocSelfCure), String.valueOf(this.mDelayedResetSelfCure)});
                        HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                        if (this.mDelayedReassocSelfCure) {
                            HwSelfCureEngine.this.sendMessage(114, 204, 0);
                        } else if (this.mDelayedResetSelfCure) {
                            HwSelfCureEngine.this.sendMessage(114, 205, 0);
                        }
                    } else {
                        HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                        HwSelfCureEngine.this.notifyHttpReachableForWifiPro(true);
                        this.mDelayedReassocSelfCure = false;
                        this.mDelayedResetSelfCure = false;
                        HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                        hwSelfCureEngine.transitionTo(hwSelfCureEngine.mConnectedMonitorState);
                    }
                }
            }
        }

        private void handleRoamingDetected(String newBssid) {
            if (newBssid == null || newBssid.equals(this.mCurrentBssid)) {
                HwHiLog.e(HwSelfCureEngine.TAG, false, "handleRoamingDetected, but bssid is unchanged, ignore it.", new Object[0]);
            } else if (HwSelfCureEngine.this.canArpReachable()) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "last gateway reachable, don't use http-get, gateway unchanged after roaming!", new Object[0]);
                HwSelfCureEngine.this.sendNetworkCheckingStatus(WifiProCommonDefs.ACTION_NETWORK_CONDITIONS_MEASURED, WifiProCommonDefs.EXTRA_IS_INTERNET_READY, 5);
            } else {
                this.mCurrentBssid = newBssid;
                if (HwSelfCureEngine.this.hasMessages(114)) {
                    return;
                }
                if ((!hasBeenTested(202) || (hasBeenTested(202) && this.mRenewDhcpCount == 1)) && !HwSelfCureEngine.this.mSelfCureOngoing.get() && !this.mDelayedReassocSelfCure && !this.mDelayedResetSelfCure) {
                    HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                    if (!HwSelfCureEngine.this.isHttpReachable(false)) {
                        HwHiLog.d(HwSelfCureEngine.TAG, false, "Roaming self-cure event: Re-Dhcp enter", new Object[0]);
                        HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.UNIQUE_CURE_EVENT, "reDhcpCnt");
                        HwHiLog.d(HwSelfCureEngine.TAG, false, "handleRoamingDetected, and HTTP access failed, trigger Re-Dhcp for it first time.", new Object[0]);
                        HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                        HwSelfCureEngine.this.sendMessage(114, 202, 0);
                        return;
                    }
                    HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                    HwSelfCureEngine.this.notifyHttpReachableForWifiPro(true);
                    HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                    hwSelfCureEngine.transitionTo(hwSelfCureEngine.mConnectedMonitorState);
                }
            }
        }

        private String getCurrentGateway() {
            DhcpInfo dhcpInfo = HwSelfCureEngine.this.mWifiManager.getDhcpInfo();
            if (dhcpInfo == null || dhcpInfo.gateway == 0) {
                return null;
            }
            return NetworkUtils.intToInetAddress(dhcpInfo.gateway).getHostAddress();
        }

        private void updateWifiConfig(WifiConfiguration wifiConfig) {
            WifiConfiguration config;
            if (wifiConfig == null) {
                config = WifiProCommonUtils.getCurrentWifiConfig(HwSelfCureEngine.this.mWifiManager);
            } else {
                config = wifiConfig;
            }
            if (config != null) {
                config.internetSelfCureHistory = HwSelfCureUtils.internetSelfCureHistoryInfo2String(this.mSelfCureHistoryInfo);
                Bundle data = new Bundle();
                data.putInt("messageWhat", 131672);
                data.putParcelable("messageObj", config);
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 28, data);
            }
        }

        private String getNextTestDhcpResults() {
            for (Map.Entry entry : HwSelfCureEngine.this.mDhcpOfferPackets.entrySet()) {
                String gatewayKey = (String) entry.getKey();
                String dhcpResults = (String) entry.getValue();
                if (gatewayKey != null && !gatewayKey.equals(this.mCurrentGateway)) {
                    boolean untested = true;
                    int i = 0;
                    while (true) {
                        if (i >= HwSelfCureEngine.this.mDhcpResultsTestDone.size()) {
                            break;
                        } else if (gatewayKey.equals(HwSelfCureEngine.this.mDhcpResultsTestDone.get(i))) {
                            untested = false;
                            break;
                        } else {
                            i++;
                        }
                    }
                    if (untested) {
                        return dhcpResults;
                    }
                }
            }
            return null;
        }

        private void selfCureForDns() {
            int i = 0;
            HwHiLog.d(HwSelfCureEngine.TAG, false, "begin to self cure for internet access: RESET_LEVEL_LOW_1_DNS", new Object[0]);
            HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.UNIQUE_CURE_EVENT, HwSelfCureEngine.ADD_REPLACE_DNS_RECORD);
            HwSelfCureEngine.this.mSelfCureOngoing.set(true);
            this.mTestedSelfCureLevel.add(201);
            if (HwSelfCureEngine.this.mInternetUnknown) {
                ConnectivityManager connectivityManager = (ConnectivityManager) HwSelfCureEngine.this.mContext.getSystemService("connectivity");
                if (connectivityManager != null) {
                    Network[] allNetworks = connectivityManager.getAllNetworks();
                    int length = allNetworks.length;
                    while (true) {
                        if (i < length) {
                            Network network = allNetworks[i];
                            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                            if (networkInfo != null && networkInfo.getType() == 1) {
                                this.mAssignedDnses = NetworkUtils.makeStrings(connectivityManager.getLinkProperties(network).getDnsServers());
                                break;
                            }
                            i++;
                        } else {
                            break;
                        }
                    }
                    String[] strArr = this.mAssignedDnses;
                    if (strArr == null || strArr.length == 0) {
                        HwSelfCureUtils.requestUpdateDnsServers(HwSelfCureUtils.getPublicDnsServers());
                    } else {
                        HwSelfCureUtils.requestUpdateDnsServers(HwSelfCureUtils.getReplacedDnsServers(strArr));
                    }
                } else {
                    return;
                }
            } else {
                HwSelfCureUtils.requestUpdateDnsServers(HwSelfCureUtils.getPublicDnsServers());
            }
            HwSelfCureEngine.this.sendMessageDelayed(113, 1000);
        }

        private String getRecordDhcpResults() {
            if (!this.mConfigStaticIp4GatewayChanged) {
                return null;
            }
            HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
            String dhcpResult = hwSelfCureEngine.getDhcpResultsHasInternet(WifiProCommonUtils.getCurrentBssid(hwSelfCureEngine.mWifiManager), HwSelfCureEngine.this.mSelfCureConfig);
            StaticIpConfiguration dhcpIpConfig = WifiProCommonUtils.dhcpResults2StaticIpConfig(dhcpResult);
            if (dhcpIpConfig == null) {
                return null;
            }
            InetAddress dhcpGateAddr = dhcpIpConfig.gateway;
            if (!(dhcpGateAddr instanceof Inet4Address) || HwSelfCureEngine.this.doSlowArpTest((Inet4Address) dhcpGateAddr)) {
                return dhcpResult;
            }
            HwSelfCureEngine.this.sendMessageDelayed(113, 500);
            return null;
        }

        private void selfCureWifiLink(int requestCureLevel) {
            String dhcpResults;
            HwHiLog.d(HwSelfCureEngine.TAG, false, "selfCureWifiLink, cureLevel = %{public}d, signal rssi = %{public}d", new Object[]{Integer.valueOf(requestCureLevel), Integer.valueOf(this.mCurrentRssi)});
            if (requestCureLevel == 201) {
                selfCureForDns();
            } else if (requestCureLevel == 202) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "re-DHCP event (ROAMING event) : reDhcpCnt enter", new Object[0]);
                HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.UNIQUE_CURE_EVENT, "reDhcpCnt");
                HwHiLog.d(HwSelfCureEngine.TAG, false, "begin to self cure for internet access: RESET_LEVEL_LOW_2_RENEW_DHCP", new Object[0]);
                HwSelfCureEngine.this.mDhcpOfferPackets.clear();
                HwSelfCureEngine.this.mDhcpResultsTestDone.clear();
                HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                this.mTestedSelfCureLevel.add(Integer.valueOf(requestCureLevel));
                this.mRenewDhcpCount++;
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 48, new Bundle());
                HwSelfCureEngine.this.sendMessageDelayed(116, 6000);
            } else if (requestCureLevel == 208) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "begin to self cure for internet access: RESET_LEVEL_DEAUTH_BSSID", new Object[0]);
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 59, new Bundle());
                HwSelfCureEngine.this.sendMessageDelayed(113, 15000);
            } else if (requestCureLevel == 203) {
                if (this.mConfigStaticIp4MultiDhcpServer) {
                    dhcpResults = getNextTestDhcpResults();
                } else {
                    dhcpResults = getRecordDhcpResults();
                }
                String gatewayKey = WifiProCommonUtils.dhcpResults2Gateway(dhcpResults);
                if (dhcpResults == null || gatewayKey == null) {
                    HwHiLog.e(HwSelfCureEngine.TAG, false, "dhcpResults or gatewayKey is null", new Object[0]);
                    HwSelfCureEngine.this.notifyHttpReachableForWifiPro(false);
                    return;
                }
                Bundle data = new Bundle();
                if (HwSelfCureEngine.mSelfCureReason == 303) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "DHCP offer reason: DNS", new Object[0]);
                    data.putInt("selfCureType", 0);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("dnsCureRecoveryEvent", "dhcpOfferCnt", data);
                } else if (HwSelfCureEngine.mSelfCureReason == HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "DHCP offer reason: TCP", new Object[0]);
                    data.putInt("selfCureType", 1);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("tcpCureRecoveryEvent", "dhcpOfferCnt", data);
                } else if (HwSelfCureEngine.mSelfCureReason == 301) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "DHCP offer reason: ROAMING", new Object[0]);
                    data.putInt("selfCureType", 2);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("roamCureRecoveryEvent", "dhcpOfferCnt", data);
                } else if (HwSelfCureEngine.mSelfCureReason == 302) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "DHCP offer reason: Multi-DHCP", new Object[0]);
                    data.putInt("selfCureType", 3);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("multiCureRecoveryEvent", "dhcpOfferCnt", data);
                } else {
                    HwHiLog.w(HwSelfCureEngine.TAG, false, "DHCP Offer Reason is not the 4 reasons", new Object[0]);
                }
                String gatewayKey2 = gatewayKey.replace("/", "");
                HwHiLog.d(HwSelfCureEngine.TAG, false, "begin to self cure for internet access: TRY_NEXT_DHCP_OFFER", new Object[0]);
                HwSelfCureEngine.this.mDhcpResultsTestDone.add(gatewayKey2);
                StaticIpConfiguration staticIpConfig = WifiProCommonUtils.dhcpResults2StaticIpConfig(dhcpResults);
                HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                this.mTestedSelfCureLevel.add(Integer.valueOf(requestCureLevel));
                HwSelfCureEngine.this.requestUseStaticIpConfig(staticIpConfig);
                HwSelfCureEngine.this.sendMessageDelayed(116, 3000);
            } else if (requestCureLevel == 207) {
                HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                this.mUnconflictedIp = HwSelfCureEngine.this.getLegalIpConfiguration();
                HwHiLog.d(HwSelfCureEngine.TAG, false, "begin to self cure for internet access: RESET_LEVEL_RECONNECT_4_INVALID_IP", new Object[0]);
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 49, new Bundle());
            } else if (requestCureLevel == 204) {
                if (this.mCurrentRssi < -75 || HwSelfCureEngine.this.mP2pConnected.get()) {
                    HwSelfCureEngine.this.notifyHttpReachableForWifiPro(false);
                    this.mDelayedReassocSelfCure = true;
                    return;
                }
                HwHiLog.d(HwSelfCureEngine.TAG, false, "begin to self cure for internet access: RESET_LEVEL_MIDDLE_REASSOC", new Object[0]);
                HwHiLog.d(HwSelfCureEngine.TAG, false, "TCP self-cure event: reassocation enter", new Object[0]);
                HwSelfCureEngine.this.uploadManager.addChrSsidCntStat(HwSelfCureEngine.UNIQUE_CURE_EVENT, "reassocCnt");
                HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                this.mTestedSelfCureLevel.add(Integer.valueOf(requestCureLevel));
                this.mDelayedReassocSelfCure = false;
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 50, new Bundle());
            } else if (requestCureLevel != 205) {
            } else {
                if (HwSelfCureEngine.this.mInternetUnknown || WifiProCommonUtils.isQueryActivityMatched(HwSelfCureEngine.this.mContext, WifiProCommonUtils.HUAWEI_SETTINGS_WLAN)) {
                    HwSelfCureEngine.this.notifyHttpReachableForWifiPro(false);
                    HwSelfCureEngine.this.mRouterInternetDetector.notifyNoInternetAfterCure(this.mCurrentGateway, this.mConfigAuthType, HwSelfCureEngine.this.mMobileHotspot);
                    return;
                }
                HwSelfCureEngine.this.mWifiManager.getConnectionInfo();
                if (this.mCurrentRssi < -70 || HwSelfCureEngine.this.mP2pConnected.get()) {
                    HwSelfCureEngine.this.notifyHttpReachableForWifiPro(false);
                    this.mDelayedResetSelfCure = true;
                    return;
                }
                Bundle data2 = new Bundle();
                if (HwSelfCureEngine.mSelfCureReason == 303) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "Chip recovery reason: DNS", new Object[0]);
                    data2.putInt("selfCureType", 0);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("dnsCureRecoveryEvent", "chipCureCnt", data2);
                } else if (HwSelfCureEngine.mSelfCureReason == HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "Chip recovery reason: TCP", new Object[0]);
                    data2.putInt("selfCureType", 1);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("tcpCureRecoveryEvent", "chipCureCnt", data2);
                } else if (HwSelfCureEngine.mSelfCureReason == 301) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "Chip recovery reason: ROAMING", new Object[0]);
                    data2.putInt("selfCureType", 2);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("roamCureRecoveryEvent", "chipCureCnt", data2);
                } else if (HwSelfCureEngine.mSelfCureReason == 302) {
                    HwHiLog.d(HwSelfCureEngine.TAG, false, "Chip recovery reason: Multi-DHCP", new Object[0]);
                    data2.putInt("selfCureType", 3);
                    HwSelfCureEngine.this.uploadManager.addChrSsidBundleStat("multiCureRecoveryEvent", "chipCureCnt", data2);
                } else {
                    HwHiLog.w(HwSelfCureEngine.TAG, false, "Chip recovery Reason is not the 4 reasons", new Object[0]);
                }
                HwHiLog.d(HwSelfCureEngine.TAG, false, "begin to self cure for internet access: RESET_LEVEL_HIGH_RESET", new Object[0]);
                HwSelfCureEngine.this.mDhcpOfferPackets.clear();
                HwSelfCureEngine.this.mDhcpResultsTestDone.clear();
                HwSelfCureEngine.this.mSelfCureOngoing.set(true);
                this.mDelayedResetSelfCure = false;
                this.mTestedSelfCureLevel.add(Integer.valueOf(requestCureLevel));
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 51, new Bundle());
            }
        }
    }

    class Wifi6SelfCureState extends State {
        private static final int ACTION_TYPE_HTC = 0;
        private static final int ACTION_TYPE_WIFI6 = 1;
        private int mInternetValue = 0;
        private boolean mIsForceHttpCheck = true;
        private int mWifi6ArpDetectionFailedCnt = 0;
        private int mWifi6HtcArpDetectionFailedCnt = 0;

        Wifi6SelfCureState() {
        }

        public void enter() {
            HwHiLog.d(HwSelfCureEngine.TAG, false, "==> ## enter Wifi6SelfCureState", new Object[0]);
            this.mWifi6HtcArpDetectionFailedCnt = 0;
            this.mWifi6ArpDetectionFailedCnt = 0;
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 110) {
                if (i != HwSelfCureEngine.CMD_WIFI6_SELFCURE) {
                    switch (i) {
                        case 307:
                            periodicWifi6WithouHtcArpDetect();
                            break;
                        case HwSelfCureEngine.CMD_WIFI6_WITH_HTC_PERIODIC_ARP_DETECTED /*{ENCODED_INT: 308}*/:
                            periodicWifi6WithHtcArpDetect();
                            break;
                        case HwSelfCureEngine.CMD_WIFI6_WITH_HTC_ARP_FAILED_DETECTED /*{ENCODED_INT: 309}*/:
                            handleWifi6WithHtcArpFail();
                            break;
                        case HwSelfCureEngine.CMD_WIFI6_WITHOUT_HTC_ARP_FAILED_DETECTED /*{ENCODED_INT: 310}*/:
                            handleWifi6WithouHtcArpFail();
                            break;
                        default:
                            return false;
                    }
                } else {
                    this.mInternetValue = message.arg1;
                    if (message.obj instanceof Boolean) {
                        this.mIsForceHttpCheck = !((Boolean) message.obj).booleanValue();
                    }
                    HwSelfCureEngine.this.sendMessage(HwSelfCureEngine.CMD_WIFI6_WITH_HTC_PERIODIC_ARP_DETECTED);
                }
                return false;
            }
            HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
            hwSelfCureEngine.transitionTo(hwSelfCureEngine.mInternetSelfCureState);
            return false;
        }

        public void exit() {
            HwHiLog.d(HwSelfCureEngine.TAG, false, "==> ##Wifi6SelfCureState exit", new Object[0]);
        }

        private void handleWifi6WithHtcArpFail() {
            HwHiLog.d(HwSelfCureEngine.TAG, false, "wifi6 with htc arp detect failed", new Object[0]);
            boolean unused = HwSelfCureEngine.this.mIsWifi6ArpSuccess = false;
            Wifi6BlackListInfo wifi6BlackListInfo = new Wifi6BlackListInfo(0, SystemClock.elapsedRealtime());
            String lastConnectedBssid = WifiProCommonUtils.getCurrentBssid(HwSelfCureEngine.this.mWifiManager);
            HwSelfCureEngine.this.mWifi6BlackListCache.put(lastConnectedBssid, wifi6BlackListInfo);
            HwHiLog.d(HwSelfCureEngine.TAG, false, "add %{public}s to HTC blacklist", new Object[]{WifiProCommonUtils.safeDisplayBssid(lastConnectedBssid)});
            HwSelfCureEngine.this.sendBlacklistToDriver();
            HwSelfCureEngine.this.mWifiNative.mHwWifiNativeEx.sendCmdToDriver(HwSelfCureEngine.IFACE, 132, new byte[]{1});
            HwSelfCureEngine.this.sendMessage(307);
        }

        private void handleWifi6WithouHtcArpFail() {
            String lastConnectedBssid = WifiProCommonUtils.getCurrentBssid(HwSelfCureEngine.this.mWifiManager);
            HwHiLog.d(HwSelfCureEngine.TAG, false, "wifi6 without htc arp detect failed", new Object[0]);
            boolean unused = HwSelfCureEngine.this.mIsWifi6ArpSuccess = false;
            HwSelfCureEngine.this.mWifi6BlackListCache.put(lastConnectedBssid, new Wifi6BlackListInfo(1, SystemClock.elapsedRealtime()));
            HwHiLog.d(HwSelfCureEngine.TAG, false, "add %{public}s to WIFI6 blacklist", new Object[]{WifiProCommonUtils.safeDisplayBssid(lastConnectedBssid)});
            HwSelfCureEngine.this.sendBlacklistToDriver();
            wifi6ReassocSelfcure();
        }

        private void periodicWifi6WithHtcArpDetect() {
            if (doWifi6ArpDetec(true)) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "wifi6 with htc arp detect success", new Object[0]);
                this.mWifi6HtcArpDetectionFailedCnt = 0;
                boolean unused = HwSelfCureEngine.this.mIsWifi6ArpSuccess = true;
                HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                hwSelfCureEngine.deferMessage(hwSelfCureEngine.obtainMessage(122, this.mInternetValue, 0, Boolean.valueOf(true ^ this.mIsForceHttpCheck)));
                HwSelfCureEngine hwSelfCureEngine2 = HwSelfCureEngine.this;
                hwSelfCureEngine2.transitionTo(hwSelfCureEngine2.mConnectedMonitorState);
                return;
            }
            this.mWifi6HtcArpDetectionFailedCnt++;
            if (this.mWifi6HtcArpDetectionFailedCnt == 5) {
                HwSelfCureEngine.this.sendMessage(HwSelfCureEngine.CMD_WIFI6_WITH_HTC_ARP_FAILED_DETECTED);
            }
            int i = this.mWifi6HtcArpDetectionFailedCnt;
            if (i > 0 && i < 5) {
                HwSelfCureEngine.this.sendMessageDelayed(HwSelfCureEngine.CMD_WIFI6_WITH_HTC_PERIODIC_ARP_DETECTED, 300);
            }
        }

        private void periodicWifi6WithouHtcArpDetect() {
            if (doWifi6ArpDetec(false)) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "wifi6 without htc arp detect success", new Object[0]);
                this.mWifi6ArpDetectionFailedCnt = 0;
                boolean unused = HwSelfCureEngine.this.mIsWifi6ArpSuccess = true;
                if (!HwSelfCureEngine.this.isHttpReachable(false)) {
                    HwSelfCureEngine.this.sendMessageDelayed(122, this.mInternetValue, 0, Boolean.valueOf(!this.mIsForceHttpCheck), 100);
                } else {
                    HwSelfCureEngine.this.mSelfCureOngoing.set(false);
                    HwSelfCureEngine.this.notifyHttpReachableForWifiPro(true);
                }
                HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
                hwSelfCureEngine.transitionTo(hwSelfCureEngine.mConnectedMonitorState);
                return;
            }
            this.mWifi6ArpDetectionFailedCnt++;
            if (this.mWifi6ArpDetectionFailedCnt == 5) {
                HwSelfCureEngine.this.sendMessage(HwSelfCureEngine.CMD_WIFI6_WITHOUT_HTC_ARP_FAILED_DETECTED);
            }
            int i = this.mWifi6ArpDetectionFailedCnt;
            if (i > 0 && i < 5) {
                HwSelfCureEngine.this.sendMessageDelayed(307, 500);
            }
        }

        private void wifi6ReassocSelfcure() {
            boolean unused = HwSelfCureEngine.this.mHasTestWifi6Reassoc = true;
            Message msg = Message.obtain();
            msg.what = 112;
            msg.arg1 = HwSelfCureEngine.INTERNET_FAILED_TYPE_TCP;
            HwSelfCureEngine.this.deferMessage(msg);
            HwSelfCureEngine hwSelfCureEngine = HwSelfCureEngine.this;
            hwSelfCureEngine.transitionTo(hwSelfCureEngine.mInternetSelfCureState);
        }

        private boolean doWifi6ArpDetec(boolean isArpDetectWithHtc) {
            int interfaceId;
            if (isArpDetectWithHtc) {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "do wifi6 with htc arp detect", new Object[0]);
                interfaceId = 83;
            } else {
                HwHiLog.d(HwSelfCureEngine.TAG, false, "do wifi6 without htc arp detect", new Object[0]);
                interfaceId = 84;
            }
            Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, interfaceId, new Bundle());
            if (result != null) {
                return result.getBoolean("arpResult");
            }
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void handleArpFailedDetected() {
        this.mSelfCureOngoing.set(true);
        if (isHttpReachable(false)) {
            this.mSelfCureOngoing.set(false);
            notifyHttpReachableForWifiPro(true);
            return;
        }
        sendMessage(114, 204);
    }

    /* access modifiers changed from: private */
    public boolean canArpReachable() {
        DhcpResults dhcpResults = syncGetDhcpResults();
        if (dhcpResults == null) {
            return false;
        }
        Bundle data = new Bundle();
        data.putSerializable(GATEWAY, dhcpResults.gateway);
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 75, data);
        if (result != null) {
            return result.getBoolean(ARP_TEST);
        }
        return false;
    }

    /* access modifiers changed from: private */
    public DhcpResults syncGetDhcpResults() {
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 46, new Bundle());
        if (result == null) {
            return null;
        }
        result.setClassLoader(DhcpResults.class.getClassLoader());
        return result.getParcelable(PortalDbHelper.ITEM_DHCP_RESULTS);
    }

    private WifiInfo getWifiInfo() {
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 52, new Bundle());
        if (result != null) {
            return (WifiInfo) result.getParcelable("WifiInfo");
        }
        return null;
    }

    /* access modifiers changed from: private */
    public void requestUseStaticIpConfig(StaticIpConfiguration staticIpConfig) {
        Bundle data = new Bundle();
        data.putParcelable("staticIpConfig", staticIpConfig);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 53, data);
    }

    /* access modifiers changed from: private */
    public void setWifiBackgroundReason(int reason) {
        Bundle data = new Bundle();
        data.putInt("reason", reason);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 55, data);
    }

    /* access modifiers changed from: private */
    public void updateScCHRCount(int count) {
        Bundle data = new Bundle();
        data.putInt("count", count);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 61, data);
    }

    /* access modifiers changed from: private */
    public boolean doSlowArpTest(Inet4Address addr) {
        Bundle data = new Bundle();
        data.putSerializable("testIpAddr", addr);
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 76, data);
        if (result != null) {
            return result.getBoolean("slowArpTest");
        }
        return false;
    }

    /* access modifiers changed from: private */
    public String getDhcpResultsHasInternet(String currentBssid, WifiConfiguration config) {
        PortalDataBaseManager database;
        String dhcpResults = null;
        if (!(currentBssid == null || (database = PortalDataBaseManager.getInstance(this.mContext)) == null)) {
            dhcpResults = database.syncQueryDhcpResultsByBssid(currentBssid);
        }
        if (dhcpResults != null || config == null) {
            return dhcpResults;
        }
        return config.lastDhcpResults;
    }

    /* access modifiers changed from: private */
    public boolean handleInvalidDhcpOffer(String dhcpResults) {
        if (dhcpResults == null) {
            return false;
        }
        requestUseStaticIpConfig(WifiProCommonUtils.dhcpResults2StaticIpConfig(dhcpResults));
        return true;
    }

    /* access modifiers changed from: private */
    public String getLegalIpConfiguration() {
        DhcpResults dhcpResults = syncGetDhcpResults();
        HwHiLog.d(TAG, false, "getLegalIpConfiguration, dhcpResults are %{private}s", new Object[]{dhcpResults});
        if (dhcpResults == null || dhcpResults.gateway == null || dhcpResults.ipAddress == null) {
            return null;
        }
        InetAddress gateway = dhcpResults.gateway;
        InetAddress initialIpAddr = dhcpResults.ipAddress.getAddress();
        int testCnt = 0;
        ArrayList<InetAddress> conflictedIpAddr = new ArrayList<>();
        InetAddress testIpAddr = initialIpAddr;
        while (true) {
            int testCnt2 = testCnt + 1;
            if (testCnt < 3 && testIpAddr != null) {
                conflictedIpAddr.add(testIpAddr);
                testIpAddr = HwSelfCureUtils.getNextIpAddr(gateway, initialIpAddr, conflictedIpAddr);
                if (testIpAddr != null) {
                    if (!doSlowArpTest((Inet4Address) testIpAddr)) {
                        HwHiLog.d(TAG, false, "getLegalIpConfiguration, find a new unconflicted one.", new Object[0]);
                        dhcpResults.ipAddress = new LinkAddress(testIpAddr, dhcpResults.ipAddress.getPrefixLength(), dhcpResults.ipAddress.getFlags(), dhcpResults.ipAddress.getScope());
                        return WifiProCommonUtils.dhcpResults2String(dhcpResults, -1);
                    }
                }
                testCnt = testCnt2;
            }
        }
        try {
            byte[] oldIpAddr = dhcpResults.ipAddress.getAddress().getAddress();
            oldIpAddr[3] = -100;
            LinkAddress newIpAddress = new LinkAddress(InetAddress.getByAddress(oldIpAddr), dhcpResults.ipAddress.getPrefixLength(), dhcpResults.ipAddress.getFlags(), dhcpResults.ipAddress.getScope());
            HwHiLog.d(TAG, false, "getLegalIpConfiguration newIpAddress = %{private}s", new Object[]{newIpAddress});
            dhcpResults.ipAddress = newIpAddress;
            return WifiProCommonUtils.dhcpResults2String(dhcpResults, -1);
        } catch (UnknownHostException e) {
            HwHiLog.e(TAG, false, "Exception happened in getLegalIpConfiguration()", new Object[0]);
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void notifyHttpReachableForWifiPro(boolean httpReachable) {
        WifiProStateMachine wifiProStateMachine = WifiProStateMachine.getWifiProStateMachineImpl();
        if (wifiProStateMachine != null) {
            wifiProStateMachine.notifyHttpReachable(httpReachable);
        }
        HwWifiproLiteStateMachine liteStateMachine = HwWifiproLiteStateMachine.getInstance();
        if (liteStateMachine != null) {
            liteStateMachine.notifyHttpReachable(httpReachable);
        }
    }

    /* access modifiers changed from: private */
    public void notifyHttpRedirectedForWifiPro() {
        WifiProStateMachine wifiProStateMachine = WifiProStateMachine.getWifiProStateMachineImpl();
        if (wifiProStateMachine != null) {
            wifiProStateMachine.notifyHttpRedirectedForWifiPro();
        }
        HwWifiproLiteStateMachine liteStateMachine = HwWifiproLiteStateMachine.getInstance();
        if (liteStateMachine != null) {
            liteStateMachine.notifyHttpRedirectedForWifiPro();
        }
    }

    private void notifyRoamingCompletedForWifiPro(String newBssid) {
        WifiProStateMachine wifiProStateMachine = WifiProStateMachine.getWifiProStateMachineImpl();
        if (wifiProStateMachine != null) {
            wifiProStateMachine.notifyRoamingCompleted(newBssid);
        }
    }

    /* access modifiers changed from: private */
    public boolean isConnectingOrConnected() {
        WifiInfo info = getWifiInfo();
        if (info == null || info.getSupplicantState().ordinal() < SupplicantState.AUTHENTICATING.ordinal()) {
            return false;
        }
        HwHiLog.d(TAG, false, "Supplicant is connectingOrConnected, no need to self cure for auto connection.", new Object[0]);
        this.autoConnectFailedNetworks.clear();
        this.autoConnectFailedNetworksRssi.clear();
        this.mNoAutoConnCounter = 0;
        return true;
    }

    /* access modifiers changed from: private */
    public boolean isSuppOnCompletedState() {
        WifiInfo info = getWifiInfo();
        if (info == null || info.getSupplicantState().ordinal() != SupplicantState.COMPLETED.ordinal()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public void handleNetworkConnected() {
        HwHiLog.d(TAG, false, "ENTER: handleNetworkConnected()", new Object[0]);
        if (!updateConnSelfCureFailedHistory()) {
            HwHiLog.d(TAG, false, "handleNetworkConnected, config is null for update, delay 2s to update again.", new Object[0]);
            sendMessageDelayed(119, 2000);
        }
        this.mNoAutoConnCounter = 0;
        this.autoConnectFailedNetworks.clear();
        this.autoConnectFailedNetworksRssi.clear();
        List<Integer> enabledReasons = new ArrayList<>();
        enabledReasons.add(1);
        enabledReasons.add(10);
        enabledReasons.add(11);
        enableAllNetworksByReason(enabledReasons, false);
        this.mConnectedTimeMills = System.currentTimeMillis();
        synchronized (this.mDhcpFailedBssidLock) {
            this.mDhcpFailedBssids.clear();
            this.mDhcpFailedConfigKeys.clear();
        }
        transitionTo(this.mConnectedMonitorState);
    }

    /* access modifiers changed from: private */
    public boolean updateConnSelfCureFailedHistory() {
        WifiConfiguration config = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
        if (config == null || config.configKey() == null) {
            return false;
        }
        this.networkCureFailedHistory.remove(config.configKey());
        HwHiLog.d(TAG, false, "updateConnSelfCureFailedHistory(), networkCureFailedHistory remove %{public}s", new Object[]{StringUtilEx.safeDisplaySsid(config.getPrintableSsid())});
        return true;
    }

    /* access modifiers changed from: private */
    public void enableAllNetworksByReason(List<Integer> enabledReasons, boolean isNeedCheckRssi) {
        List<WifiConfiguration> savedNetworks = WifiproUtils.getAllConfiguredNetworks();
        if (savedNetworks == null || savedNetworks.size() == 0) {
            HwHiLog.e(TAG, false, "enableAllNetworksByReason, no saved networks found.", new Object[0]);
            return;
        }
        for (WifiConfiguration itemSaveNetworks : savedNetworks) {
            WifiConfiguration.NetworkSelectionStatus status = itemSaveNetworks.getNetworkSelectionStatus();
            int disableReason = status.getNetworkSelectionDisableReason();
            boolean isNeedEnableNetwork = false;
            if (WifiCommonUtils.doesNotWifiConnectRejectByCust(status, itemSaveNetworks.SSID, this.mContext)) {
                HwHiLog.d(TAG, false, "enableAllNetworksByReason can not enable wifi", new Object[0]);
            } else {
                if (!status.isNetworkEnabled()) {
                    HwHiLog.d(TAG, false, "enableAllNetworksByReason, isNeedCheckRssi: %{public}s, rssiStatusDisabled: %{public}d", new Object[]{String.valueOf(isNeedCheckRssi), Integer.valueOf(itemSaveNetworks.rssiStatusDisabled)});
                    if (!isNeedCheckRssi || (isNeedCheckRssi && itemSaveNetworks.rssiStatusDisabled != -200 && itemSaveNetworks.rssiStatusDisabled <= -75)) {
                        isNeedEnableNetwork = true;
                    }
                }
                if (isNeedEnableNetwork) {
                    Iterator<Integer> it = enabledReasons.iterator();
                    while (true) {
                        if (it.hasNext()) {
                            if (disableReason == it.next().intValue()) {
                                HwHiLog.d(TAG, false, "To enable network which status is %{public}d, config = %{public}s, id = %{public}d", new Object[]{Integer.valueOf(disableReason), StringUtilEx.safeDisplaySsid(itemSaveNetworks.getPrintableSsid()), Integer.valueOf(itemSaveNetworks.networkId)});
                                itemSaveNetworks.rssiStatusDisabled = -200;
                                this.mWifiManager.enableNetwork(itemSaveNetworks.networkId, false);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void enableAllNetworksByEnterSettings(List<Integer> enabledReasons) {
        List<WifiConfiguration> savedNetworks = WifiproUtils.getAllConfiguredNetworks();
        if (savedNetworks == null || savedNetworks.size() == 0) {
            HwHiLog.e(TAG, false, "enableAllNetworksByEnterSettings, no saved networks found.", new Object[0]);
            return;
        }
        int networkSize = savedNetworks.size();
        for (int i = 0; i < networkSize; i++) {
            WifiConfiguration nextConfig = savedNetworks.get(i);
            if (nextConfig != null) {
                WifiConfiguration.NetworkSelectionStatus status = nextConfig.getNetworkSelectionStatus();
                int disableReason = status.getNetworkSelectionDisableReason();
                if (WifiCommonUtils.doesNotWifiConnectRejectByCust(status, nextConfig.SSID, this.mContext)) {
                    HwHiLog.d(TAG, false, "enableAllNetworksByEnterSettings can not enable wifi", new Object[0]);
                } else if (!status.isNetworkEnabled() && (HwAutoConnectManager.KEY_HUAWEI_EMPLOYEE.equals(nextConfig.configKey()) || HwAutoConnectManager.KEY_HUAWEI_EMPLOYEE.equals(nextConfig.configKey()))) {
                    HwHiLog.d(TAG, false, "##enableAllNetworksByEnterSettings, HUAWEI_EMPLOYEE networkId = %{public}d", new Object[]{Integer.valueOf(nextConfig.networkId)});
                    this.mWifiManager.enableNetwork(nextConfig.networkId, false);
                } else if (!status.isNetworkEnabled() && !nextConfig.noInternetAccess && !nextConfig.portalNetwork) {
                    Iterator<Integer> it = enabledReasons.iterator();
                    while (true) {
                        if (it.hasNext()) {
                            if (disableReason == it.next().intValue()) {
                                HwHiLog.d(TAG, false, "enableAllNetworksByEnterSettings, status is %{public}d, config = %{public}s, id = %{public}d", new Object[]{Integer.valueOf(disableReason), StringUtilEx.safeDisplaySsid(nextConfig.getPrintableSsid()), Integer.valueOf(nextConfig.networkId)});
                                this.mWifiManager.enableNetwork(nextConfig.networkId, false);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isHttpReachable(boolean useDoubleServers) {
        synchronized (this.mNetworkChecker) {
            int mainSvrRespCode = this.mNetworkChecker.isCaptivePortal(true);
            if (WifiProCommonUtils.unreachableRespCodeByAndroid(mainSvrRespCode)) {
                return false;
            }
            if (mainSvrRespCode == 302) {
                this.mIsHttpRedirected = true;
            } else {
                this.mIsHttpRedirected = false;
            }
            return true;
        }
    }

    /* access modifiers changed from: private */
    public boolean isIpAddressInvalid() {
        byte[] currAddr;
        DhcpInfo dhcpInfo = this.mWifiManager.getDhcpInfo();
        if (!(dhcpInfo == null || dhcpInfo.ipAddress == 0 || (currAddr = NetworkUtils.intToInetAddress(dhcpInfo.ipAddress).getAddress()) == null || currAddr.length != 4)) {
            int intCurrAddr3 = currAddr[3] & 255;
            int netmaskLenth = NetworkUtils.netmaskIntToPrefixLength(dhcpInfo.netmask);
            HwHiLog.d(TAG, false, "isIpAddressLegal, currAddr[3] is %{public}d netmask lenth is: %{public}d", new Object[]{Integer.valueOf(intCurrAddr3), Integer.valueOf(netmaskLenth)});
            boolean ipEqualsGw = dhcpInfo.ipAddress == dhcpInfo.gateway;
            boolean invalidIp = intCurrAddr3 == 0 || intCurrAddr3 == 1 || intCurrAddr3 == 255;
            if (ipEqualsGw || (netmaskLenth == 24 && invalidIp)) {
                HwHiLog.w(TAG, false, "current rcvd ip is invalid, maybe no internet access, need to comfirm and cure it.", new Object[0]);
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void handleWifiDisabled(boolean selfCureGoing) {
        HwHiLog.d(TAG, false, "ENTER: handleWifiDisabled(), selfCureGoing = %{public}s", new Object[]{String.valueOf(selfCureGoing)});
        this.mNoAutoConnCounter = 0;
        this.autoConnectFailedNetworks.clear();
        this.autoConnectFailedNetworksRssi.clear();
        this.networkCureFailedHistory.clear();
        if (selfCureGoing) {
            transitionTo(this.mDisconnectedMonitorState);
        }
    }

    /* access modifiers changed from: private */
    public void handleWifiEnabled() {
        HwHiLog.d(TAG, false, "ENTER: handleWifiEnabled()", new Object[0]);
        List<Integer> enabledReasons = new ArrayList<>();
        enabledReasons.add(1);
        enabledReasons.add(10);
        enabledReasons.add(11);
        enableAllNetworksByReason(enabledReasons, false);
        sendBlacklistToDriver();
    }

    /* access modifiers changed from: private */
    public void handleNetworkRemoved(WifiConfiguration config) {
        if (config != null) {
            this.networkCureFailedHistory.remove(config.configKey());
            this.autoConnectFailedNetworks.remove(config.configKey());
            this.autoConnectFailedNetworksRssi.remove(config.configKey());
        }
    }

    private boolean hasDhcpResultsSaved(WifiConfiguration config) {
        return WifiProCommonUtils.dhcpResults2StaticIpConfig(config.lastDhcpResults) != null;
    }

    /* access modifiers changed from: private */
    public boolean multiGateway() {
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 71, new Bundle());
        if (result != null) {
            return result.getBoolean("isMultiGateway");
        }
        return false;
    }

    public synchronized void notifyWifiScanResultsAvailable(boolean success) {
        if (this.mInitialized && success) {
            sendMessage(101);
        }
    }

    public synchronized void notifyDhcpResultsInternetOk(String dhcpResults) {
        if (this.mInitialized && dhcpResults != null) {
            sendMessage(125, dhcpResults);
        }
    }

    public synchronized void notifyWifiConnectedBackground() {
        HwHiLog.d(TAG, false, "ENTER: notifyWifiConnectedBackground()", new Object[0]);
        if (this.mInitialized) {
            this.mIsWifiBackground.set(true);
            this.mIpConfigLostCnt = 0;
            sendMessage(104);
        }
    }

    public synchronized void notifyWifiRoamingCompleted(String bssid) {
        HwHiLog.d(TAG, false, "ENTER: notifyWifiRoamingCompleted()", new Object[0]);
        if (this.mInitialized && bssid != null) {
            sendMessageDelayed(110, bssid, 500);
            notifyRoamingCompletedForWifiPro(bssid);
        }
    }

    public synchronized void notifySefCureCompleted(int status) {
        HwHiLog.d(TAG, false, "ENTER: notifySefCureCompleted, status = %{public}d", new Object[]{Integer.valueOf(status)});
        if (!this.mInitialized || status != 0) {
            if (-1 != status) {
                if (-2 != status) {
                    this.mSelfCureOngoing.set(false);
                    notifyVoWiFiSelCureEnd(-1);
                }
            }
            sendMessage(120);
            notifyVoWiFiSelCureEnd(-1);
        } else {
            sendMessage(113);
        }
    }

    public synchronized void notifyTcpStatResults(int deltaTx, int deltaRx, int deltaReTx, int deltaDnsFailed) {
        if (this.mInitialized) {
            if (deltaDnsFailed < 3 || deltaRx != 0) {
                if (deltaTx < 3 || deltaRx != 0) {
                    if (deltaRx > 0) {
                        this.mNoTcpRxCounter = 0;
                        removeMessages(CMD_DNS_FAILED_DETECTED);
                        removeMessages(121);
                    }
                } else if (!hasMessages(121)) {
                    sendMessage(121);
                }
            } else if (!hasMessages(CMD_DNS_FAILED_DETECTED)) {
                sendMessage(CMD_DNS_FAILED_DETECTED);
            }
        }
    }

    public synchronized void notifyWifiDisconnected() {
        HwHiLog.d(TAG, false, "ENTER: notifyWifiDisconnected()", new Object[0]);
        if (this.mInitialized) {
            sendMessage(108);
        }
    }

    public synchronized void notifyIpConfigCompleted() {
        if (this.mInitialized) {
            HwHiLog.d(TAG, false, "ENTER: notifyIpConfigCompleted()", new Object[0]);
            this.mIpConfigLostCnt = 0;
            sendMessage(117);
        }
    }

    public synchronized boolean notifyIpConfigLostAndHandle(WifiConfiguration config) {
        if (this.mInitialized && config != null) {
            if (config.isEnterprise()) {
                HwHiLog.d(TAG, false, "notifyIpConfigLostAndHandle, no self cure for enterprise network", new Object[0]);
                return false;
            }
            int signalLevel = WifiProCommonUtils.getCurrenSignalLevel(this.mWifiManager.getConnectionInfo());
            this.mIpConfigLostCnt++;
            HwHiLog.d(TAG, false, "ENTER: notifyIpConfigLostAndHandle() IpConfigLostCnt = %{public}d, ssid = %{public}s, signalLevel = %{public}d", new Object[]{Integer.valueOf(this.mIpConfigLostCnt), StringUtilEx.safeDisplaySsid(config.SSID), Integer.valueOf(signalLevel)});
            if (signalLevel >= 3 && getCurrentState() == this.mDisconnectedMonitorState) {
                if (this.mIpConfigLostCnt == 2 && hasDhcpResultsSaved(config)) {
                    sendMessage(124, config);
                    return true;
                } else if (this.mIpConfigLostCnt >= 1 && !hasDhcpResultsSaved(config)) {
                    sendMessage(CMD_BSSID_DHCP_FAILED_EVENT, config);
                }
            }
        }
        return false;
    }

    public boolean isDhcpFailedBssid(String bssid) {
        boolean contains;
        synchronized (this.mDhcpFailedBssidLock) {
            contains = this.mDhcpFailedBssids.contains(bssid);
        }
        return contains;
    }

    public boolean isDhcpFailedConfigKey(String configKey) {
        boolean contains;
        synchronized (this.mDhcpFailedBssidLock) {
            contains = this.mDhcpFailedConfigKeys.contains(configKey);
        }
        return contains;
    }

    /* access modifiers changed from: private */
    public boolean isWifi6Network(String lastConnectedBssid) {
        List<ScanResult> scanResults = getScanResults();
        if (scanResults == null) {
            return false;
        }
        for (ScanResult scanResult : scanResults) {
            if (scanResult != null && scanResult.BSSID != null && scanResult.BSSID.equals(lastConnectedBssid) && WifiProCommonUtils.isSsidSupportWiFi6(scanResult)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void requestChangeWifiStatus(boolean enabled) {
        if (this.mInitialized && this.mWifiManager != null) {
            this.mWifiManager.setWifiEnabled(enabled);
        }
    }

    public synchronized boolean isSelfCureOngoing() {
        if (!this.mInitialized) {
            return false;
        }
        return this.mSelfCureOngoing.get();
    }

    public synchronized void notifyInternetFailureDetected(boolean forceNoHttpCheck) {
        if (this.mInitialized) {
            HwHiLog.d(TAG, false, "ENTER: notifyInternetFailureDetected, forceNoHttpCheck = %{public}s", new Object[]{String.valueOf(forceNoHttpCheck)});
            sendMessage(122, Boolean.valueOf(forceNoHttpCheck));
        }
    }

    public synchronized void notifyInternetAccessRecovery() {
        if (this.mInitialized) {
            this.mRouterInternetDetector.notifyInternetAccessRecovery();
            HwHiLog.d(TAG, false, "ENTER: notifyInternetAccessRecovery", new Object[0]);
            sendMessage(CMD_HTTP_REACHABLE_RCV);
        }
    }

    public synchronized void notifyUserEnterWlanSettings() {
        if (this.mInitialized) {
            sendMessage(CMD_USER_ENTER_WLAN_SETTINGS);
        }
    }

    public synchronized void notifySettingsDisplayNoInternet() {
        sendMessage(131);
    }

    public synchronized void notifyRouterGatewayUnreachable() {
        sendMessage(132);
    }

    public void requestArpConflictTest(DhcpResults dhcpResults) {
        InetAddress addr;
        if (dhcpResults != null && dhcpResults.ipAddress != null && (addr = dhcpResults.ipAddress.getAddress()) != null && (addr instanceof Inet4Address) && doSlowArpTest((Inet4Address) addr)) {
            HwHiLog.d(TAG, false, "requestArpConflictTest, Upload static ip conflicted chr!", new Object[0]);
            updateScCHRCount(26);
        }
    }

    /* access modifiers changed from: package-private */
    public static class CureFailedNetworkInfo {
        public String configKey;
        public int cureFailedCounter;
        public long lastCureFailedTime;

        public CureFailedNetworkInfo(String key, int counter, long time) {
            this.configKey = key;
            this.cureFailedCounter = counter;
            this.lastCureFailedTime = time;
        }

        public String toString() {
            return "[ " + "configKey = " + StringUtilEx.safeDisplaySsid(this.configKey) + ", cureFailedCounter = " + this.cureFailedCounter + ", lastCureFailedTime = " + DateFormat.getDateTimeInstance().format(new Date(this.lastCureFailedTime)) + " ]";
        }
    }

    static class InternetSelfCureHistoryInfo {
        public int dnsSelfCureFailedCnt = 0;
        public long lastDnsSelfCureFailedTs = 0;
        public long lastReassocSelfCureConnectFailedTs = 0;
        public long lastReassocSelfCureFailedTs = 0;
        public long lastRenewDhcpSelfCureFailedTs = 0;
        public long lastResetSelfCureConnectFailedTs = 0;
        public long lastResetSelfCureFailedTs = 0;
        public long lastStaticIpSelfCureFailedTs = 0;
        public int reassocSelfCureConnectFailedCnt = 0;
        public int reassocSelfCureFailedCnt = 0;
        public int renewDhcpSelfCureFailedCnt = 0;
        public int resetSelfCureConnectFailedCnt = 0;
        public int resetSelfCureFailedCnt = 0;
        public int staticIpSelfCureFailedCnt = 0;

        public String toString() {
            StringBuilder sbuf = new StringBuilder();
            sbuf.append("[ ");
            sbuf.append("dnsSelfCureFailedCnt = " + this.dnsSelfCureFailedCnt);
            sbuf.append(", renewDhcpSelfCureFailedCnt = " + this.renewDhcpSelfCureFailedCnt);
            sbuf.append(", staticIpSelfCureFailedCnt = " + this.staticIpSelfCureFailedCnt);
            sbuf.append(", reassocSelfCureFailedCnt = " + this.reassocSelfCureFailedCnt);
            sbuf.append(", resetSelfCureFailedCnt = " + this.resetSelfCureFailedCnt);
            sbuf.append(", reassocSelfCureConnectFailedCnt = " + this.reassocSelfCureConnectFailedCnt);
            sbuf.append(", resetSelfCureConnectFailedCnt = " + this.resetSelfCureConnectFailedCnt);
            sbuf.append(" ]");
            return sbuf.toString();
        }
    }

    public static class Wifi6BlackListInfo {
        private int actionType = -1;
        private long updateTime = 0;

        Wifi6BlackListInfo(int actionType2, long updateTime2) {
            this.actionType = actionType2;
            this.updateTime = updateTime2;
        }

        public int getActionType() {
            return this.actionType;
        }

        public long getUpdateTime() {
            return this.updateTime;
        }
    }

    /* access modifiers changed from: private */
    public void startScan() {
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 73, new Bundle());
    }

    /* access modifiers changed from: private */
    public List<ScanResult> getScanResults() {
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 74, new Bundle());
        if (result == null) {
            return null;
        }
        result.setClassLoader(ScanResult.class.getClassLoader());
        return result.getParcelableArrayList("results");
    }

    /* access modifiers changed from: private */
    public void periodicArpDetection() {
        int signalLevel = WifiProCommonUtils.getCurrenSignalLevel(this.mWifiManager.getConnectionInfo());
        HwHiLog.d(TAG, false, "periodicArpDetection signalLevel = %{public}d, isScreenOn = %{public}s , mArpDetectionFailedCnt = %{public}d", new Object[]{Integer.valueOf(signalLevel), String.valueOf(this.mPowerManager.isScreenOn()), Integer.valueOf(this.mArpDetectionFailedCnt)});
        if (hasMessages(CMD_PERIODIC_ARP_DETECTED)) {
            removeMessages(CMD_PERIODIC_ARP_DETECTED);
        }
        if (signalLevel >= 2 && this.mPowerManager.isScreenOn() && isSuppOnCompletedState() && !this.mSelfCureOngoing.get()) {
            Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 68, new Bundle());
            boolean arpResult = false;
            long time = 0;
            if (result != null) {
                arpResult = result.getBoolean("arpResult");
                time = result.getLong(WifiScanGenieDataBaseImpl.CHANNEL_TABLE_TIME);
            }
            Bundle data = new Bundle();
            data.putBoolean("succ", arpResult);
            data.putLong("spendTime", time);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 63, data);
            if (!arpResult) {
                this.mArpDetectionFailedCnt++;
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 67, new Bundle());
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 66, new Bundle());
                if (this.mArpDetectionFailedCnt == 5) {
                    data.clear();
                    data.putInt("eventId", 87);
                    data.putString("eventData", "ARP_DETECTED_FAILED");
                    WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 4, data);
                    sendMessage(CMD_ARP_FAILED_DETECTED);
                }
                int i = this.mArpDetectionFailedCnt;
                if (i > 0 && i < 5) {
                    sendMessageDelayed(CMD_PERIODIC_ARP_DETECTED, 10000);
                    updateArpDetect(this.mArpDetectionFailedCnt, time);
                    return;
                }
            } else {
                this.mArpDetectionFailedCnt = 0;
            }
            updateArpDetect(this.mArpDetectionFailedCnt, time);
        }
        sendMessageDelayed(CMD_PERIODIC_ARP_DETECTED, 60000);
    }

    /* access modifiers changed from: private */
    public void flushVmDnsCache() {
        Intent intent = new Intent("android.intent.action.CLEAR_DNS_CACHE");
        intent.addFlags(536870912);
        intent.addFlags(67108864);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    /* access modifiers changed from: private */
    public void notifyVoWiFiSelCureBegin() {
        if (this.mSelfCureOngoing.get()) {
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 69, new Bundle());
        }
    }

    /* access modifiers changed from: private */
    public void notifyVoWiFiSelCureEnd(int status) {
        boolean success = false;
        if (status == 0) {
            success = true;
        }
        Bundle data = new Bundle();
        data.putBoolean(HwWifiCHRHilink.WEB_DELAY_NEEDUPLOAD, success);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 72, data);
    }

    private void updateArpDetect(int failCnt, long rtt) {
        Bundle arp = new Bundle();
        arp.putInt("ARPFAILCNT", failCnt);
        arp.putLong("RTTARP", rtt);
        Bundle data = new Bundle();
        data.putInt("eventId", EVENT_ARP_DETECT);
        data.putBundle("eventData", arp);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, data);
    }

    /* access modifiers changed from: private */
    public void sendBlacklistToDriver() {
        Map<String, Wifi6BlackListInfo> map = this.mWifi6BlackListCache;
        if (map != null && !map.isEmpty()) {
            ageOutWifi6BlackList();
            byte[] bytes = HwSelfCureUtils.blackListToByteArray(this.mWifi6BlackListCache);
            HwHiLog.d(TAG, false, "sendBlacklistToDriver size:%{public}d", new Object[]{Integer.valueOf(this.mWifi6BlackListCache.size())});
            this.mWifiNative.mHwWifiNativeEx.sendCmdToDriver(IFACE, 131, bytes);
        }
    }

    private void ageOutWifi6BlackList() {
        Iterator<Map.Entry<String, Wifi6BlackListInfo>> iterator = this.mWifi6BlackListCache.entrySet().iterator();
        while (iterator.hasNext()) {
            if (SystemClock.elapsedRealtime() - iterator.next().getValue().getUpdateTime() >= WIFI6_BLACKLIST_TIME_EXPIRED) {
                iterator.remove();
            }
        }
        if (this.mWifi6BlackListCache.size() >= 16) {
            long earliestTime = Long.MAX_VALUE;
            String delBssid = null;
            for (Map.Entry<String, Wifi6BlackListInfo> map : this.mWifi6BlackListCache.entrySet()) {
                if (map.getValue().getUpdateTime() < earliestTime) {
                    delBssid = map.getKey();
                    earliestTime = map.getValue().getUpdateTime();
                }
            }
            this.mWifi6BlackListCache.remove(delBssid);
        }
    }
}
