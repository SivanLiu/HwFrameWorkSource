package com.huawei.hwwifiproservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.UserHandle;
import android.util.Log;
import com.android.server.wifi.ClientModeImpl;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifipro.WifiProCommonUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkQosMonitor {
    public static final String ACTION_NOTIFY_WIFI_INTERNET_STATUS = "com.huawei.wifipro.action.ACTION_NOTIFY_WIFI_INTERNET_STATUS";
    private static final int CHECK_NETWORK_PROPERTY_TIMEOUT = 18000;
    private static final int CURRENT_NETWORK_CHECKER_THREAD = 0;
    public static final String EXTRA_INTERNET_STATUS = "internet_status";
    public static final String EXTRA_NETWORK_CONNECTED_STATUS = "network_connected_status_portal";
    private static final String KEYWORD_EVENT_DATA = "eventData";
    private static final String KEYWORD_EVENT_ID = "eventId";
    private static final String KEY_PORTAL_DETECT_RESULT = "portal_detect_result";
    public static final int MSG_BQE_DETECTION_RESULT = 10;
    private static final int MSG_CHECK_NETWORK_PROPERTY_TIMEOUT = 19;
    public static final int MSG_NETWORK_DETECTION_RESULT = 2;
    public static final int MSG_NETWORK_QOS_RESULT = 1;
    public static final int MSG_NETWORK_TYPE_BY_WEBVIEW_REQ = 15;
    public static final int MSG_NETWORK_TYPE_BY_WEBVIEW_RESP = 16;
    public static final int MSG_NETWORK_TYPE_BY_WEBVIEW_RESP_TIMEOUT = 17;
    public static final int MSG_NOTIFY_MCC = 5;
    public static final int MSG_QUERY_PKT_TIMEOUT = 7;
    public static final int MSG_QUERY_RTT = 9;
    public static final int MSG_QUERY_TCPRX_TIMEOUT = 11;
    public static final int MSG_REQUEST_CHECK_NETWORK_PROPERTY = 6;
    public static final int MSG_RESET_RTT = 8;
    public static final int MSG_RETURN_AP_AVAILABLE_RSSI_TH = 12;
    public static final int MSG_RETURN_AP_CURRENT_RSSI = 14;
    public static final int MSG_RETURN_AP_HISTORY_SCORE_RS = 13;
    public static final int MSG_SEND_TCP_CHK_RESULT_QUERY = 4;
    public static final int MSG_WIFI_SECURITY_RESPONSE = 18;
    private static final int PORTAL_NETWORK = 1;
    public static final int QUERY_TCPRX_TIMEOUT = 500;
    public static final String TAG = "WiFi_PRO";
    private static final int TCPF_CA_CWR = 4;
    private static final int TCPF_CA_DISORDER = 2;
    private static final int TCPF_CA_LOSS = 16;
    private static final int TCPF_CA_OPEN = 1;
    private static final int TCPF_CA_RECOVERY = 8;
    public static final int TYPE_HAS_INTERNET = 101;
    public static final int TYPE_NO_INTERNET = 102;
    public static final int TYPE_STANDARD_PORTAL = 103;
    public static final int TYPE_UNKNWON = 100;
    public static final int TYPE_UNSTANDARD_PORTAL = 104;
    public static final int WEBVIEW_RESP_TIMEOUT_MS = 12000;
    public static final int WIFIPRO_MOBILE_BQE_RTT = 2;
    public static final int WIFIPRO_NOTIFY_MOBILE_BQE_RTT = 3;
    public static final int WIFIPRO_NOTIFY_WLAN_BQE_RTT = 2;
    public static final int WIFIPRO_NOTIFY_WLAN_SAMPLE_RTT = 4;
    public static final int WIFIPRO_WLAN_BQE_RTT = 1;
    public static final int WIFIPRO_WLAN_SAMPLE_RTT = 3;
    public static final int WLAN_TCP_RX_ERROR = -1;
    private boolean isBetaVersion = false;
    /* access modifiers changed from: private */
    public WifiproBqeUtils mBqeClient;
    /* access modifiers changed from: private */
    public AtomicBoolean mBroadcastNotified = new AtomicBoolean(false);
    private BroadcastReceiver mBroadcastReceiver;
    /* access modifiers changed from: private */
    public INetworkQosCallBack mCallBack;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public HuaweiWifiWatchdogStateMachine mHuaweiWifiWatchdogStateMachine;
    /* access modifiers changed from: private */
    public HwNetworkPropertyChecker mHwNetworkPropertyChecker;
    private IntentFilter mIntentFilter;
    /* access modifiers changed from: private */
    public IPQosMonitor mIpQosMonitor = null;
    private boolean mIsBQEServiceStart;
    /* access modifiers changed from: private */
    public AtomicBoolean mIsNetworkChecking = new AtomicBoolean(false);
    /* access modifiers changed from: private */
    public boolean mIsWaitingResponse;
    /* access modifiers changed from: private */
    public int mLastNotifyNetworkType = 100;
    /* access modifiers changed from: private */
    public MobileQosDetector mMobileQosDetector;
    /* access modifiers changed from: private */
    public final Object mNetworkCheckLock = new Object();
    /* access modifiers changed from: private */
    public List<NetworkCheckThread> mNetworkCheckThreads = new ArrayList();
    /* access modifiers changed from: private */
    public Handler mNetworkHandler;
    private final Object mQueryTcpRxLock = new Object();
    /* access modifiers changed from: private */
    public final Object mReceiverLock = new Object();
    /* access modifiers changed from: private */
    public final Object mSyncNotifyLock = new Object();
    private int mTcpRxCounter;
    /* access modifiers changed from: private */
    public boolean mTcpRxRequestedViaBroadcast = false;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;
    /* access modifiers changed from: private */
    public WifiProUIDisplayManager mWifiProUIDisplayManager;
    private ClientModeImpl mWifiStateMachine;

    public NetworkQosMonitor(Context context, INetworkQosCallBack callBack, Messenger dstMessenger, WifiProUIDisplayManager wifiProUIDisplayManager) {
        this.mContext = context;
        this.mCallBack = callBack;
        this.mWifiProUIDisplayManager = wifiProUIDisplayManager;
        initialize();
        this.mIpQosMonitor = new IPQosMonitor(this.mNetworkHandler);
        this.mHwNetworkPropertyChecker = new HwNetworkPropertyChecker(context, null, null, true, null, false);
        this.mBqeClient = WifiproBqeUtils.getInstance(context);
        this.mHuaweiWifiWatchdogStateMachine = HuaweiWifiWatchdogStateMachine.makeHuaweiWifiWatchdogStateMachine(this.mContext, dstMessenger, this.mNetworkHandler);
        this.isBetaVersion = isBetaVer(this.mContext);
        registerBootCompleteReceiver(callBack);
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mWifiStateMachine = WifiInjector.getInstance().getClientModeImpl();
    }

    public void registerCallBack(INetworkQosCallBack callBack) {
        if (this.mCallBack == null) {
            this.mCallBack = callBack;
        }
        registerMonitorReceiver();
    }

    private void registerMonitorReceiver() {
        this.mBroadcastNotified.set(false);
        this.mTcpRxRequestedViaBroadcast = false;
        if (this.mContext != null && this.mBroadcastReceiver == null) {
            this.mIntentFilter = new IntentFilter();
            this.mIntentFilter.addAction(WifiProCommonDefs.ACTION_NETWOR_PROPERTY_NOTIFICATION);
            this.mIntentFilter.addAction(WifiProCommonDefs.ACTION_NETWORK_PROPERTY_CHR);
            this.mIntentFilter.addAction(WifiProCommonDefs.ACTION_REQUEST_TCP_RX_COUNTER);
            this.mIntentFilter.addAction(WifiProCommonDefs.ACTION_REQUEST_WEBVIEW_CHECK);
            this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
            this.mIntentFilter.addAction(WifiProCommonDefs.ACTION_NOTIFY_WIFI_SECURITY_STATUS);
            this.mBroadcastReceiver = new BroadcastReceiver() {
                /* class com.huawei.hwwifiproservice.NetworkQosMonitor.AnonymousClass1 */

                public void onReceive(Context context, Intent intent) {
                    synchronized (NetworkQosMonitor.this.mReceiverLock) {
                        Log.i("WiFi_PRO", "action received = " + intent.getAction() + ", mBroadcastNotified = " + NetworkQosMonitor.this.mBroadcastNotified.get() + ", mTcpRxRequestedViaBroadcast = " + NetworkQosMonitor.this.mTcpRxRequestedViaBroadcast);
                        if (WifiProCommonDefs.ACTION_NETWOR_PROPERTY_NOTIFICATION.equals(intent.getAction()) && !NetworkQosMonitor.this.mBroadcastNotified.get()) {
                            NetworkQosMonitor.this.handleNetworkPropertyNotification(intent);
                        } else if (WifiProCommonDefs.ACTION_NETWORK_PROPERTY_CHR.equals(intent.getAction())) {
                            NetworkQosMonitor.this.uploadNetworkCheckChr(intent);
                        } else {
                            int i = 1;
                            if (WifiProCommonDefs.ACTION_REQUEST_TCP_RX_COUNTER.equals(intent.getAction()) && !NetworkQosMonitor.this.mTcpRxRequestedViaBroadcast) {
                                boolean unused = NetworkQosMonitor.this.mTcpRxRequestedViaBroadcast = true;
                                NetworkQosMonitor.this.mIpQosMonitor.queryPackets(0);
                            } else if (WifiProCommonDefs.ACTION_REQUEST_WEBVIEW_CHECK.equals(intent.getAction())) {
                                boolean oversea = intent.getBooleanExtra(WifiProCommonDefs.EXTRA_FLAG_OVERSEA, false);
                                Log.i("WiFi_PRO", "ACTION_REQUEST_WEBVIEW_CHECK:: begin, oversea = " + oversea);
                                Handler access$600 = NetworkQosMonitor.this.mNetworkHandler;
                                Handler access$6002 = NetworkQosMonitor.this.mNetworkHandler;
                                if (!oversea) {
                                    i = 0;
                                }
                                access$600.sendMessage(Message.obtain(access$6002, 15, i, 0));
                                NetworkQosMonitor.this.mNetworkHandler.sendEmptyMessageDelayed(17, 12000);
                            } else if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
                                NetworkInfo info = null;
                                Object tempInfo = intent.getParcelableExtra("networkInfo");
                                if (tempInfo instanceof NetworkInfo) {
                                    info = (NetworkInfo) tempInfo;
                                } else {
                                    Log.e("WiFi_PRO", "NetworkInfo is not match the class");
                                }
                                if (info != null && NetworkInfo.DetailedState.DISCONNECTED == info.getDetailedState()) {
                                    NetworkQosMonitor.this.mBroadcastNotified.set(false);
                                    synchronized (NetworkQosMonitor.this.mSyncNotifyLock) {
                                        int unused2 = NetworkQosMonitor.this.mLastNotifyNetworkType = 100;
                                    }
                                }
                            } else if (WifiProCommonDefs.ACTION_NOTIFY_WIFI_SECURITY_STATUS.equals(intent.getAction())) {
                                NetworkQosMonitor.this.notifySecurityResult(intent.getExtras());
                            }
                        }
                    }
                }
            };
            this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter);
        }
    }

    private WifiConfiguration getCurrentWifiConfig() {
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager == null) {
            return null;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configNetworks == null || wifiInfo == null) {
            return null;
        }
        int configNetworksSize = configNetworks.size();
        for (int i = 0; i < configNetworksSize; i++) {
            WifiConfiguration config = configNetworks.get(i);
            if (config.networkId == wifiInfo.getNetworkId()) {
                return config;
            }
        }
        return null;
    }

    /* access modifiers changed from: private */
    public void uploadNetworkCheckChr(Intent intent) {
        String portalDetectStatInfo;
        ClientModeImpl clientModeImpl;
        String portalDetectStatInfo2;
        WifiConfiguration currentWifiConfig = getCurrentWifiConfig();
        if (currentWifiConfig != null && (portalDetectStatInfo = intent.getStringExtra(WifiProCommonUtils.KEY_PORTAL_DETECT_STAT_INFO)) != null && (clientModeImpl = this.mWifiStateMachine) != null) {
            boolean isFirstConnected = clientModeImpl.isFirstConnect();
            Bundle data = new Bundle();
            if (isFirstConnected || currentWifiConfig.portalNetwork) {
                portalDetectStatInfo2 = (isFirstConnected ? 1 : 0) + "|" + currentWifiConfig.internetHistory + "|" + portalDetectStatInfo;
            } else {
                portalDetectStatInfo2 = intent.getStringExtra(KEY_PORTAL_DETECT_RESULT);
                if (portalDetectStatInfo2 == null) {
                    return;
                }
            }
            Log.i("WiFi_PRO", "network probe CHR is to be uploaded, portalDetectStatInfo = " + portalDetectStatInfo2);
            data.putString("portalDetectionStatInfo", portalDetectStatInfo2);
            Bundle dftEventData = new Bundle();
            dftEventData.putInt(KEYWORD_EVENT_ID, 909002060);
            dftEventData.putBundle(KEYWORD_EVENT_DATA, data);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 2, dftEventData);
        }
    }

    /* access modifiers changed from: private */
    public void handleNetworkPropertyNotification(Intent intent) {
        int property = intent.getIntExtra(WifiProCommonDefs.EXTRA_FLAG_NETWORK_PROPERTY, WifiproUtils.NET_INET_QOS_LEVEL_UNKNOWN);
        if (property == 6) {
            handleNetworkProperty6Level(intent);
        }
        Handler handler = this.mNetworkHandler;
        handler.sendMessage(Message.obtain(handler, 2, 1, property));
        this.mBroadcastNotified.set(true);
        if (property == -1 || property == -101) {
            HwWifiProFeatureControl.getInstance();
            HwWifiProFeatureControl.notifyInternetFailureDetected(true);
            HwWifiConnectivityMonitor hwWifiConnectivityMonitor = HwWifiConnectivityMonitor.getInstance();
            if (hwWifiConnectivityMonitor != null && HwWifiProFeatureControl.sWifiProRoamingCtrl) {
                hwWifiConnectivityMonitor.requestRoamingByNoInternet();
            }
            syncNotifyPowerSaveGenie(false, 102, true);
        } else if (property == 5) {
            syncNotifyPowerSaveGenie(true, 101, true);
        }
    }

    private void handleNetworkProperty6Level(Intent intent) {
        this.mHwNetworkPropertyChecker.setRawRedirectedHostName(intent.getStringExtra(WifiProCommonDefs.EXTRA_RAW_REDIRECTED_HOST));
        syncNotifyPowerSaveGenie(false, intent.getBooleanExtra(WifiProCommonDefs.EXTRA_STANDARD_PORTAL_NETWORK, false) ? 103 : 104, true);
        if (HwAutoConnectManager.getInstance() != null) {
            HwAutoConnectManager.getInstance().notifyPortalNetworkConnected();
        }
        String configKey = intent.getStringExtra(WifiProCommonUtils.KEY_PORTAL_CONFIG_KEY);
        String redirectedUrl = intent.getStringExtra(WifiProCommonUtils.KEY_PORTAL_REDIRECTED_URL);
        boolean firstDetected = intent.getBooleanExtra(WifiProCommonUtils.KEY_PORTAL_FIRST_DETECT, false);
        int respCode = intent.getIntExtra(WifiProCommonUtils.KEY_PORTAL_HTTP_RESP_CODE, 599);
        Bundle data = new Bundle();
        data.putString("configKey", configKey);
        data.putString("redirectedUrl", redirectedUrl);
        data.putBoolean("firstDetected", firstDetected);
        data.putInt("respCode", respCode);
        WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 5, data);
    }

    public void unRegisterCallBack() {
        this.mCallBack = null;
        BroadcastReceiver broadcastReceiver = this.mBroadcastReceiver;
        if (broadcastReceiver != null) {
            this.mContext.unregisterReceiver(broadcastReceiver);
            this.mBroadcastReceiver = null;
        }
    }

    private void registerBootCompleteReceiver(final INetworkQosCallBack callBack) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            /* class com.huawei.hwwifiproservice.NetworkQosMonitor.AnonymousClass2 */

            public void onReceive(Context context, Intent intent) {
                if (intent != null && "android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
                    Log.i("WiFi_PRO", "receive ACTION_BOOT_COMPLETED concurrently");
                    if (NetworkQosMonitor.this.mMobileQosDetector == null) {
                        Log.i("WiFi_PRO", "create mNetworkQosMonitor");
                        NetworkQosMonitor networkQosMonitor = NetworkQosMonitor.this;
                        MobileQosDetector unused = networkQosMonitor.mMobileQosDetector = new MobileQosDetector(networkQosMonitor.mContext, callBack, NetworkQosMonitor.this.mNetworkHandler, NetworkQosMonitor.this.mWifiProUIDisplayManager);
                    }
                }
            }
        }, intentFilter);
    }

    public WifiProUIDisplayManager getWifiProUIDisplayManager() {
        return this.mWifiProUIDisplayManager;
    }

    public void setIpQosEnabled(boolean enabled) {
        if (enabled) {
            Log.i("ipQos", "startMonitor");
            this.mIpQosMonitor.startMonitor();
            return;
        }
        Log.i("ipQos", "stopMonitor");
        this.mIpQosMonitor.stopMonitor();
    }

    /* access modifiers changed from: private */
    public void respTcpRxViaBroadcast(int[] rxResp, int len) {
        if (rxResp != null && len > 7) {
            Log.i("WiFi_PRO", "respTcpRxViaBroadcast, rx packets counter = " + rxResp[7]);
            this.mTcpRxRequestedViaBroadcast = false;
            Intent intent = new Intent(WifiProCommonDefs.ACTION_RESPONSE_TCP_RX_COUNTER);
            intent.setFlags(67108864);
            intent.putExtra(WifiProCommonDefs.EXTRA_FLAG_TCP_RX_COUNTER, rxResp[7]);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, WifiProCommonDefs.NETWORK_CHECKER_RECV_PERMISSION);
        }
    }

    private void initialize() {
        this.mNetworkHandler = new Handler() {
            /* class com.huawei.hwwifiproservice.NetworkQosMonitor.AnonymousClass3 */

            public void handleMessage(Message msg) {
                int type = msg.arg1;
                int level = msg.arg2;
                int i = msg.what;
                boolean isOversea = true;
                if (i == 1) {
                    NetworkQosMonitor.this.sendNetworkLevel(1, type, level);
                } else if (i == 2) {
                    if (hasMessages(19)) {
                        removeMessages(19);
                    }
                    NetworkQosMonitor.this.sendNetworkLevel(2, type, level);
                    if (!NetworkQosMonitor.this.mNetworkCheckThreads.isEmpty()) {
                        synchronized (NetworkQosMonitor.this.mNetworkCheckLock) {
                            NetworkQosMonitor.this.mNetworkCheckThreads.remove(0);
                        }
                        NetworkQosMonitor.this.checkAndStartNetworkThread();
                    }
                } else if (i != 100) {
                    switch (i) {
                        case 4:
                            NetworkQosMonitor.this.mIpQosMonitor.queryPackets(0);
                            break;
                        case 5:
                            NetworkQosMonitor.this.mIpQosMonitor.notifyMCC(msg.arg1);
                            break;
                        case 6:
                            Log.i("WiFi_PRO", "MSG_REQUEST_CHECK_NETWORK_PROPERTY received, start to recheck network property");
                            if (msg.obj != null) {
                                Log.i("WiFi_PRO", "MSG_REQUEST_CHECK_NETWORK_PROPERTY mIsNetworkChecking = " + NetworkQosMonitor.this.mIsNetworkChecking.get());
                                if (!NetworkQosMonitor.this.mIsNetworkChecking.get()) {
                                    NetworkQosMonitor networkQosMonitor = NetworkQosMonitor.this;
                                    if (msg.arg1 != 1) {
                                        isOversea = false;
                                    }
                                    NetworkQosMonitor.this.mNetworkCheckThreads.add(new NetworkCheckThread(isOversea, ((Boolean) msg.obj).booleanValue()));
                                    NetworkQosMonitor.this.checkAndStartNetworkThread();
                                    break;
                                }
                            }
                            break;
                        case 8:
                            NetworkQosMonitor.this.mIpQosMonitor.resetRtt(msg.arg1);
                            break;
                        case 9:
                            NetworkQosMonitor.this.mIpQosMonitor.queryRtt(msg.arg1);
                            break;
                        case 10:
                            if (NetworkQosMonitor.this.mCallBack != null) {
                                NetworkQosMonitor.this.mCallBack.onWifiBqeDetectionResult(level);
                            }
                            NetworkQosMonitor.this.mHuaweiWifiWatchdogStateMachine.setBqeLevel(level);
                            break;
                        case 11:
                            if (NetworkQosMonitor.this.mIsWaitingResponse) {
                                Log.w("WiFi_PRO", "MSG_QUERY_TCPRX_TIMEOUT happened!");
                                boolean unused = NetworkQosMonitor.this.mIsWaitingResponse = false;
                                NetworkQosMonitor.this.responseTcpRxPacketsCounter(-1);
                                break;
                            }
                            break;
                        case 12:
                            if (msg.obj != null) {
                                Object obj = msg.obj;
                                if (!(obj instanceof WifiProEstimateApInfo)) {
                                    Log.e("WiFi_PRO", "MSG_RETURN_AP_AVAILABLE_RSSI_TH:class is not match");
                                    break;
                                } else {
                                    WifiProEstimateApInfo estimateApInfo = (WifiProEstimateApInfo) obj;
                                    Log.i("WiFi_PRO", "get 5G RSSI TH = " + estimateApInfo.getRetRssiTH());
                                    if (NetworkQosMonitor.this.mCallBack != null) {
                                        NetworkQosMonitor.this.mCallBack.onWifiBqeReturnRssiTH(estimateApInfo);
                                        break;
                                    }
                                }
                            }
                            break;
                        case 13:
                            if (msg.obj != null) {
                                Object obj2 = msg.obj;
                                if (!(obj2 instanceof WifiProEstimateApInfo)) {
                                    Log.e("WiFi_PRO", "MSG_RETURN_AP_HISTORY_SCORE_RS:class is not match");
                                    break;
                                } else {
                                    WifiProEstimateApInfo estimateApInfo2 = (WifiProEstimateApInfo) obj2;
                                    Log.i("WiFi_PRO", "get AP history socre = " + estimateApInfo2.getRetHistoryScore());
                                    if (NetworkQosMonitor.this.mCallBack != null) {
                                        NetworkQosMonitor.this.mCallBack.onWifiBqeReturnHistoryScore(estimateApInfo2);
                                        break;
                                    }
                                }
                            }
                            break;
                        case 14:
                            if (msg.obj != null) {
                                int rssi = 0;
                                Object obj3 = msg.obj;
                                if (obj3 instanceof Integer) {
                                    rssi = ((Integer) obj3).intValue();
                                } else {
                                    Log.e("WiFi_PRO", "MSG_RETURN_AP_CURRENT_RSSI:class is not match");
                                }
                                Log.i("WiFi_PRO", "MSG_RETURN_AP_CURRY_RSSI rssi = " + rssi);
                                if (NetworkQosMonitor.this.mCallBack != null) {
                                    NetworkQosMonitor.this.mCallBack.onWifiBqeReturnCurrentRssi(rssi);
                                    break;
                                }
                            }
                            break;
                        case 15:
                            Log.i("WiFi_PRO", "MSG_NETWORK_TYPE_BY_WEBVIEW_REQ:: begin");
                            if (msg.arg1 != 1) {
                                isOversea = false;
                            }
                            NetworkQosMonitor.this.startBqeService();
                            NetworkQosMonitor.this.mBqeClient.recheckNetworkTypeByWebView(NetworkQosMonitor.this.mNetworkHandler, isOversea);
                            break;
                        case 16:
                            Log.i("WiFi_PRO", "MSG_NETWORK_TYPE_BY_WEBVIEW_RESP:: begin, result = " + msg.arg1);
                            if (NetworkQosMonitor.this.mNetworkHandler.hasMessages(17)) {
                                NetworkQosMonitor.this.mNetworkHandler.removeMessages(17);
                            }
                            NetworkQosMonitor.this.stopBqeService();
                            Intent intentResponse = new Intent(WifiProCommonDefs.ACTION_RESP_WEBVIEW_CHECK);
                            intentResponse.setFlags(67108864);
                            intentResponse.putExtra(WifiProCommonDefs.EXTRA_FLAG_NETWORK_TYPE, msg.arg1);
                            NetworkQosMonitor.this.mContext.sendBroadcastAsUser(intentResponse, UserHandle.ALL, WifiProCommonDefs.NETWORK_CHECKER_RECV_PERMISSION);
                            break;
                        case 17:
                            Log.i("WiFi_PRO", "MSG_NETWORK_TYPE_BY_WEBVIEW_RESP_TIMEOUT");
                            NetworkQosMonitor.this.stopBqeService();
                            Intent intentTimeout = new Intent(WifiProCommonDefs.ACTION_RESP_WEBVIEW_CHECK);
                            intentTimeout.setFlags(67108864);
                            intentTimeout.putExtra(WifiProCommonDefs.EXTRA_FLAG_NETWORK_TYPE, 100);
                            NetworkQosMonitor.this.mContext.sendBroadcastAsUser(intentTimeout, UserHandle.ALL, WifiProCommonDefs.NETWORK_CHECKER_RECV_PERMISSION);
                            break;
                        case 18:
                            if (msg.obj != null) {
                                NetworkQosMonitor.this.mCallBack.onNotifyWifiSecurityStatus((Bundle) msg.obj);
                                break;
                            }
                            break;
                        case 19:
                            Log.i("WiFi_PRO", "MSG_CHECK_NETWORK_PROPERTY_TIMEOUT received");
                            NetworkQosMonitor.this.notifyNetworkResult(WifiProCommonUtils.RESP_CODE_TIMEOUT);
                            break;
                    }
                } else if (msg.obj != null) {
                    if (NetworkQosMonitor.this.mIsWaitingResponse && msg.arg1 == 0 && msg.arg2 > 7) {
                        NetworkQosMonitor.this.responseTcpRxPacketsCounter((int[]) msg.obj, msg.arg2);
                    }
                    if (NetworkQosMonitor.this.mTcpRxRequestedViaBroadcast && msg.arg1 == 0 && msg.arg2 > 7) {
                        NetworkQosMonitor.this.respTcpRxViaBroadcast((int[]) msg.obj, msg.arg2);
                    } else if (msg.arg1 == 2) {
                        NetworkQosMonitor.this.mBqeClient.setWlanRtt((int[]) msg.obj, msg.arg2);
                    } else if (msg.arg1 == 4) {
                        NetworkQosMonitor.this.mHuaweiWifiWatchdogStateMachine.setSampleRtt((int[]) msg.obj, msg.arg2);
                    } else {
                        int[] intArray = null;
                        Object currentMsg = msg.obj;
                        if (currentMsg instanceof int[]) {
                            intArray = (int[]) currentMsg;
                        } else {
                            Log.e("WiFi_PRO", "IPQosMonitor.MSG_REPORT_IPQOS:class is not match");
                        }
                        int reportType = msg.arg1;
                        int len = msg.arg2;
                        NetworkQosMonitor.this.mHuaweiWifiWatchdogStateMachine.tcpChkResultUpdate(intArray, len, reportType);
                        if (NetworkQosMonitor.this.mMobileQosDetector == null || intArray == null) {
                            Log.e("WiFi_PRO", "**error  mMobileQosDetector.setIPQos ********");
                        } else {
                            NetworkQosMonitor.this.mMobileQosDetector.setIPQos(reportType, len, intArray);
                        }
                    }
                }
                super.handleMessage(msg);
            }
        };
    }

    /* access modifiers changed from: private */
    public void sendNetworkLevel(int msg, int net_type, int level) {
        INetworkQosCallBack iNetworkQosCallBack = this.mCallBack;
        if (iNetworkQosCallBack != null) {
            if (msg == 1) {
                iNetworkQosCallBack.onNetworkQosChange(net_type, level, true);
            } else if (msg == 2) {
                Log.i("WiFi_PRO", "sendNetworkLevel, MSG_NETWORK_DETECTION_RESULT, net_type = " + net_type + ", level = " + level);
                this.mCallBack.onNetworkDetectionResult(net_type, level);
            }
        }
    }

    public void setMonitorMobileQos(boolean enabled) {
        Log.i("WiFi_PRO", "setMonitorMobileQos, enabled = " + enabled);
        MobileQosDetector mobileQosDetector = this.mMobileQosDetector;
        if (mobileQosDetector != null) {
            mobileQosDetector.monitorNetworkQos(enabled);
        } else {
            Log.e("WiFi_PRO", "**error  setMonitorMobileQos ********");
        }
    }

    public void setMonitorWifiQos(int type, boolean enabled) {
        if (1 == type) {
            Log.i("WiFi_PRO", "setMonitorMobileQos,type = WIFI_CONNECT_WITH_DATA_LINK , enabled = " + enabled);
            if (enabled) {
                this.mHuaweiWifiWatchdogStateMachine.doWifiOTACheck(WifiproUtils.WIFIPRO_START_VERIFY_WITH_DATA_LINK);
            } else {
                this.mHuaweiWifiWatchdogStateMachine.doWifiOTACheck(WifiproUtils.WIFIPRO_STOP_VERIFY_WITH_DATA_LINK);
            }
        } else if (2 == type) {
            Log.i("WiFi_PRO", "setMonitorMobileQos,type = WIFI_VERIFY_NO_DATA_LINK , enabled = " + enabled);
            if (enabled) {
                this.mHuaweiWifiWatchdogStateMachine.doWifiOTACheck(WifiproUtils.WIFIPRO_START_VERIFY_WITH_NOT_DATA_LINK);
            } else {
                this.mHuaweiWifiWatchdogStateMachine.doWifiOTACheck(WifiproUtils.WIFIPRO_STOP_VERIFY_WITH_NOT_DATA_LINK);
            }
        }
    }

    public void queryNetworkQos(int network_type, boolean portal, boolean authen, boolean wifiBackground) {
        if (network_type == 0) {
            Log.i("WiFi_PRO", "Query Mobile Qos");
            MobileQosDetector mobileQosDetector = this.mMobileQosDetector;
            if (mobileQosDetector != null) {
                mobileQosDetector.queryNetworkQos();
            } else {
                Log.e("WiFi_PRO", "**error  mMobileQosDetector.queryNetworkQos ********");
            }
        } else if (1 == network_type) {
            Log.i("WiFi_PRO", "start check wifi internet, portal = " + portal + ", authen = " + authen + ", wifiBackground =  " + wifiBackground);
            Handler handler = this.mNetworkHandler;
            handler.sendMessage(Message.obtain(handler, 6, portal ? 1 : 0, authen ? 1 : 0, Boolean.valueOf(wifiBackground)));
        }
    }

    public HwNetworkPropertyChecker getNetworkPropertyChecker() {
        return this.mHwNetworkPropertyChecker;
    }

    public void queryRxPackets() {
        this.mIpQosMonitor.queryPackets(0);
    }

    public void notifyNetworkResult(int respCode) {
        int property = -1;
        if (respCode == 204) {
            property = 5;
        } else if (WifiProCommonUtils.isRedirectedRespCodeByGoogle(respCode)) {
            property = 6;
        } else if (respCode == 600) {
            property = 7;
        }
        syncNotifyPowerSaveGenie(property == 5, 100, false);
        Log.i("WiFi_PRO", "notifyNetworkResult, respCode = " + respCode + ", property = " + property);
        Handler handler = this.mNetworkHandler;
        handler.sendMessage(Message.obtain(handler, 2, 1, property));
    }

    public void setWifiWatchDogEnabled(boolean enabled) {
        Log.i("WiFi_PRO", "setWifiWatchDogEnabled, enabled = " + enabled);
        this.mHuaweiWifiWatchdogStateMachine.enableCheck(enabled);
        if (enabled) {
            Log.i("ipQos", " ipQos init");
            this.mIpQosMonitor.init();
            return;
        }
        Log.i("ipQos", " ipQos release");
        this.mIpQosMonitor.release();
        stopALLMonitor();
    }

    public void stopALLMonitor() {
        Log.w("WiFi_PRO", "stopALLMonitor");
        setIpQosEnabled(false);
        setMonitorMobileQos(false);
        setMonitorWifiQos(1, false);
        setMonitorWifiQos(2, false);
    }

    public void resetMonitorStatus() {
        Log.w("WiFi_PRO", "resetMonitorStatus");
        this.mBroadcastNotified.set(false);
        HwNetworkPropertyChecker hwNetworkPropertyChecker = this.mHwNetworkPropertyChecker;
        if (hwNetworkPropertyChecker != null) {
            hwNetworkPropertyChecker.resetCheckerStatus();
        }
    }

    public synchronized boolean startBqeService() {
        if (!this.mIsBQEServiceStart) {
            this.mIsBQEServiceStart = true;
            this.mBqeClient.startBqeService();
            Log.i("WiFi_PRO", "startBqeService ");
            return true;
        }
        Log.i("WiFi_PRO", "BqeService is running,ignore restart ");
        return false;
    }

    public synchronized void stopBqeService() {
        Log.w("WiFi_PRO", "stopBqeService, mIsBQEServiceStart = " + this.mIsBQEServiceStart);
        if (this.mIsBQEServiceStart) {
            this.mIsBQEServiceStart = false;
            this.mBqeClient.stopBqeService();
        }
    }

    public void startWiFiBqeDetect(int interval) {
        this.mBqeClient.resetBqeState();
        this.mBqeClient.requestBqeOnce(interval, this.mNetworkHandler);
    }

    public boolean isBqeServicesStarted() {
        return this.mBqeClient.isBqeServicesStarted();
    }

    public void queryWifiSecurity(String ssid, String bssid) {
        Log.i("WiFi_PRO", "queryWifiSecurity, ssid = " + StringUtilEx.safeDisplaySsid(ssid));
        if (ssid != null && bssid != null) {
            Intent intent = new Intent(WifiProCommonDefs.ACTION_QUERY_WIFI_SECURITY);
            intent.setFlags(67108864);
            intent.putExtra(WifiProCommonDefs.FLAG_SSID, ssid);
            intent.putExtra(WifiProCommonDefs.FLAG_BSSID, bssid);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, WifiProCommonDefs.WIFI_PRO_SECURITY_RECV_PERMISSION);
        }
    }

    public synchronized int getCurrentWiFiLevel() {
        int wifilevel;
        wifilevel = this.mHuaweiWifiWatchdogStateMachine.updateQosLevelByHistory();
        Log.i("WiFi_PRO", "CurrentWiFi QOE level: " + wifilevel);
        return wifilevel;
    }

    public static boolean isBetaVer(Context context) {
        return Version.isBeta(context);
    }

    public void setRoveOutToMobileState(int roState) {
        MobileQosDetector mobileQosDetector = this.mMobileQosDetector;
        if (mobileQosDetector != null) {
            mobileQosDetector.setRoveOutToMobileState(roState);
        }
    }

    public boolean isHighDataFlowModel() {
        return this.mHuaweiWifiWatchdogStateMachine.isHighDataFlowModel();
    }

    public void get5GApRssiThreshold(WifiProEstimateApInfo apInfo) {
        if (apInfo == null) {
            Log.e("WiFi_PRO", "getHandover5GApRssiThreshold apInfo null error");
        } else {
            this.mHuaweiWifiWatchdogStateMachine.getHandover5GApRssiThreshold(apInfo);
        }
    }

    public void getApHistoryQualityScore(WifiProEstimateApInfo apInfo) {
        if (apInfo == null) {
            Log.e("WiFi_PRO", "getHandover5GApRssiThreshold apInfo null error");
        } else {
            this.mHuaweiWifiWatchdogStateMachine.getApHistoryQualityScore(apInfo);
        }
    }

    public void getApHistoryQualityScoreForWifi2Wifi(WifiProEstimateApInfo apInfo) {
        if (apInfo == null) {
            Log.e("WiFi_PRO", "getApHistoryQualityScoreForWifi2Wifi apInfo null error");
        } else {
            this.mHuaweiWifiWatchdogStateMachine.getApHistoryQualityScoreForWifi2Wifi(apInfo);
        }
    }

    public Bundle getApHistoryQuality(WifiProEstimateApInfo apInfo) {
        if (apInfo != null) {
            return this.mHuaweiWifiWatchdogStateMachine.getApHistoryQuality(apInfo);
        }
        Log.e("WiFi_PRO", "getApHistoryQuality apInfo null error");
        return new Bundle();
    }

    public int requestTcpRxPacketsCounter() {
        int rxCounter = -1;
        Log.i("WiFi_PRO", "requestTcpRxPacketsCounter, start to request tcp rx packets counter.");
        synchronized (this.mQueryTcpRxLock) {
            try {
                if (this.mIpQosMonitor != null) {
                    this.mIpQosMonitor.queryPackets(0);
                    this.mIsWaitingResponse = true;
                    this.mNetworkHandler.sendEmptyMessageDelayed(11, 500);
                    while (this.mIsWaitingResponse) {
                        Log.i("WiFi_PRO", "requestTcpRxCounter, start wait for notify");
                        this.mQueryTcpRxLock.wait();
                    }
                    if (this.mTcpRxCounter >= 0) {
                        rxCounter = this.mTcpRxCounter;
                    }
                    Log.i("WiFi_PRO", "tcp rx packets counter received rx = " + this.mTcpRxCounter);
                }
            } catch (InterruptedException e) {
                Log.e("WiFi_PRO", "requestTcpRxPacketsCounter, exception: " + e.getMessage());
            }
            this.mTcpRxCounter = 0;
        }
        return rxCounter;
    }

    /* access modifiers changed from: private */
    public void responseTcpRxPacketsCounter(int[] rxResp, int len) {
        if (rxResp != null && len > 7) {
            Log.i("WiFi_PRO", "responseTcpRxPacketsCounter, rx packets counter = " + rxResp[7]);
            this.mIsWaitingResponse = false;
            if (this.mNetworkHandler.hasMessages(11)) {
                this.mNetworkHandler.removeMessages(11);
            }
            responseTcpRxPacketsCounter(rxResp[7]);
        }
    }

    /* access modifiers changed from: private */
    public void responseTcpRxPacketsCounter(int counter) {
        synchronized (this.mQueryTcpRxLock) {
            this.mTcpRxCounter = counter;
            this.mIsWaitingResponse = false;
            Log.i("WiFi_PRO", "responseTcpRxPacketsCounter, mQueryTcpRxLock.notifyAll() called, current counter = " + this.mTcpRxCounter);
            this.mQueryTcpRxLock.notifyAll();
        }
    }

    private void sendResutl4PowerSaveGenie(boolean hasInternetAccess, boolean portalConnected) {
        Log.i("WiFi_PRO", "sendResutl4PowerSaveGenie, hasInternetAccess = " + hasInternetAccess + ", portalConnected = " + portalConnected);
        Intent intent = new Intent("com.huawei.wifipro.action.ACTION_NOTIFY_WIFI_INTERNET_STATUS");
        intent.setFlags(67108864);
        intent.putExtra("internet_status", hasInternetAccess);
        intent.putExtra("network_connected_status_portal", portalConnected);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void syncNotifyPowerSaveGenie(boolean hasInternetAccess, int detailedType, boolean init) {
        synchronized (this.mSyncNotifyLock) {
            Log.i("WiFi_PRO", "syncNotifyPowerSaveGenie, hasInternetAccess = " + hasInternetAccess + ", init = " + init + ", lastNotifyNetworkType = " + this.mLastNotifyNetworkType + ", detailedType = " + detailedType);
            boolean z = false;
            if (init) {
                this.mLastNotifyNetworkType = detailedType;
                if (detailedType == 104) {
                    hasInternetAccess = true;
                }
                if (detailedType >= 103) {
                    z = true;
                }
                sendResutl4PowerSaveGenie(hasInternetAccess, z);
            } else if (hasInternetAccess && (this.mLastNotifyNetworkType == 102 || this.mLastNotifyNetworkType == 103)) {
                this.mLastNotifyNetworkType = 101;
                sendResutl4PowerSaveGenie(hasInternetAccess, false);
            } else if (!hasInternetAccess && this.mLastNotifyNetworkType == 101) {
                this.mLastNotifyNetworkType = 102;
                sendResutl4PowerSaveGenie(hasInternetAccess, false);
            }
        }
    }

    /* access modifiers changed from: private */
    public void notifySecurityResult(Bundle bundle) {
        Message.obtain(this.mNetworkHandler, 18, bundle).sendToTarget();
    }

    /* access modifiers changed from: private */
    public void checkAndStartNetworkThread() {
        if (!this.mNetworkCheckThreads.isEmpty()) {
            this.mIsNetworkChecking.set(true);
            this.mNetworkHandler.sendEmptyMessageDelayed(19, 18000);
            this.mNetworkCheckThreads.get(0).start();
            return;
        }
        Log.i("WiFi_PRO", "network check thread complete");
        this.mIsNetworkChecking.set(false);
    }

    /* access modifiers changed from: private */
    public class NetworkCheckThread extends Thread {
        private boolean portalNetwork;
        private boolean wifiBackground;

        public NetworkCheckThread(boolean portal, boolean wifiBackground2) {
            this.portalNetwork = portal;
            this.wifiBackground = wifiBackground2;
        }

        public void run() {
            String startSsid = WifiProCommonUtils.getCurrentSsid(NetworkQosMonitor.this.mWifiManager);
            int respCode = NetworkQosMonitor.this.mHwNetworkPropertyChecker.isCaptivePortal(true, this.portalNetwork, this.wifiBackground);
            String endSsid = WifiProCommonUtils.getCurrentSsid(NetworkQosMonitor.this.mWifiManager);
            Log.i("WiFi_PRO", "startSsid = " + StringUtilEx.safeDisplaySsid(startSsid) + ", endSsid = " + StringUtilEx.safeDisplaySsid(endSsid));
            if (startSsid != null && startSsid.equals(endSsid)) {
                NetworkQosMonitor.this.notifyNetworkResult(respCode);
            }
        }
    }

    public Bundle getChrHandoverNetworkQuality() {
        return this.mHuaweiWifiWatchdogStateMachine.getChrHandoverNetworkQuality();
    }
}
