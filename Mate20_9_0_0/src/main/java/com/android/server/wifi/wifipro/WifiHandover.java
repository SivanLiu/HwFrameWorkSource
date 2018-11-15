package com.android.server.wifi.wifipro;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.wifi.HwWifiCHRService;
import com.android.server.wifi.HwWifiCHRServiceImpl;
import com.android.server.wifi.HwWifiServiceFactory;
import com.android.server.wifi.HwWifiStateMachine;
import com.android.server.wifi.ScanRequestProxy;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifipro.WifiProCommonUtils;
import java.util.ArrayList;
import java.util.List;

public class WifiHandover {
    public static final String ACTION_REQUEST_DUAL_BAND_WIFI_HANDOVER = "com.huawei.wifi.action.REQUEST_DUAL_BAND_WIFI_HANDOVER";
    public static final String ACTION_RESPONSE_DUAL_BAND_WIFI_HANDOVER = "com.huawei.wifi.action.RESPONSE_DUAL_BAND_WIFI_HANDOVER";
    public static final String ACTION_RESPONSE_WIFI_2_WIFI = "com.huawei.wifi.action.RESPONSE_WIFI_2_WIFI";
    private static final int CURRENT_STATE_IDLE = 0;
    private static final int CURRENT_STATE_WAITING_CONNECTION_COMPLETED = 2;
    private static final int CURRENT_STATE_WAITING_SCAN_RESULTS_FOR_CONNECT_WIFI = 1;
    private static final int CURRENT_STATE_WAITING_SCAN_RESULTS_FOR_WIFI_SWITCH = 4;
    private static final int CURRENT_STATE_WAITING_WIFI_2_WIFI_COMPLETED = 3;
    private static final int HANDLER_CMD_NOTIFY_GOOD_AP = 4;
    private static final int HANDLER_CMD_REQUEST_SCAN_TIME_OUT = 7;
    private static final int HANDLER_CMD_SCAN_RESULTS = 1;
    private static final int HANDLER_CMD_WIFI_2_WIFI = 3;
    private static final int HANDLER_CMD_WIFI_CONNECTED = 2;
    private static final int HANDLER_CMD_WIFI_DISCONNECTED = 6;
    private static final int HANDOVER_MIN_LEVEL_INTERVAL = 2;
    private static final int HANDOVER_STATE_WAITING_DUAL_BAND_WIFI_CONNECT = 5;
    public static final int HANDOVER_STATUS_CONNECT_AUTH_FAILED = -7;
    public static final int HANDOVER_STATUS_CONNECT_REJECT_FAILED = -6;
    public static final int HANDOVER_STATUS_DISALLOWED = -4;
    public static final int HANDOVER_STATUS_OK = 0;
    public static final long HANDOVER_WAIT_SCAN_TIME_OUT = 4000;
    public static final int INVALID_RSSI = -200;
    private static final int NETWORK_HANDLER_CMD_DUAL_BAND_WIFI_CONNECT = 5;
    public static final int NETWORK_HANDOVER_TYPE_CONNECT_SPECIFIC_WIFI = 2;
    public static final int NETWORK_HANDOVER_TYPE_DUAL_BAND_WIFI_CONNECT = 4;
    public static final int NETWORK_HANDOVER_TYPE_UNKNOWN = -1;
    public static final int NETWORK_HANDOVER_TYPE_WIFI_TO_WIFI = 1;
    private static final int SIGNAL_LEVEL_3 = 3;
    public static final String TAG = "WifiHandover";
    public static final String WIFI_HANDOVER_COMPLETED_STATUS = "com.huawei.wifi.handover.status";
    public static final String WIFI_HANDOVER_NETWORK_BSSID = "com.huawei.wifi.handover.bssid";
    public static final String WIFI_HANDOVER_NETWORK_CONFIGKYE = "com.huawei.wifi.handover.configkey";
    public static final String WIFI_HANDOVER_NETWORK_SSID = "com.huawei.wifi.handover.ssid";
    public static final String WIFI_HANDOVER_NETWORK_SWITCHTYPE = "com.huawei.wifi.handover.switchtype";
    public static final String WIFI_HANDOVER_NETWORK_WIFICONFIG = "com.huawei.wifi.handover.wificonfig";
    public static final String WIFI_HANDOVER_RECV_PERMISSION = "com.huawei.wifipro.permission.RECV.WIFI_HANDOVER";
    private BroadcastReceiver mBroadcastReceiver;
    private INetworksHandoverCallBack mCallBack;
    private Context mContext = null;
    private HwWifiCHRService mHwWifiCHRService;
    private IntentFilter mIntentFilter;
    private int mNetwoksHandoverState;
    private int mNetwoksHandoverType;
    private NetworkBlackListManager mNetworkBlackListManager = null;
    private Handler mNetworkHandler;
    private NetworkQosMonitor mNetworkQosMonitor;
    private String mOldConfigKey = null;
    private ArrayList<String> mSwitchBlacklist;
    private String mTargetBssid = null;
    private String mToConnectBssid;
    private String mToConnectConfigKey;
    private String mToConnectDualbandBssid = null;
    private WifiManager mWifiManager;
    private WifiStateMachine mWifiStateMachine;

    private static class WifiSwitchCandidate {
        private ScanResult bestScanResult;
        private WifiConfiguration bestWifiConfig;

        public WifiSwitchCandidate(WifiConfiguration bestWifiConfig, ScanResult bestScanResult) {
            this.bestWifiConfig = bestWifiConfig;
            this.bestScanResult = bestScanResult;
        }

        public WifiConfiguration getWifiConfig() {
            return this.bestWifiConfig;
        }

        public ScanResult getScanResult() {
            return this.bestScanResult;
        }
    }

    public WifiHandover(Context context, INetworksHandoverCallBack callBack) {
        this.mCallBack = callBack;
        this.mContext = context;
        this.mHwWifiCHRService = HwWifiCHRServiceImpl.getInstance();
        this.mWifiStateMachine = WifiInjector.getInstance().getWifiStateMachine();
        initialize();
        registerReceiverAndHandler();
    }

    public void registerCallBack(INetworksHandoverCallBack callBack, NetworkQosMonitor qosMonitor) {
        if (this.mCallBack == null) {
            this.mCallBack = callBack;
        }
        if (this.mBroadcastReceiver == null && this.mContext != null) {
            registerReceiverAndHandler();
        }
        this.mNetworkQosMonitor = qosMonitor;
    }

    public void unRegisterCallBack() {
        this.mCallBack = null;
        if (this.mBroadcastReceiver != null && this.mContext != null) {
            this.mContext.unregisterReceiver(this.mBroadcastReceiver);
            this.mBroadcastReceiver = null;
            this.mIntentFilter = null;
            this.mNetworkHandler = null;
        }
    }

    private void initialize() {
        this.mNetwoksHandoverType = -1;
        this.mNetwoksHandoverState = 0;
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mNetworkBlackListManager = NetworkBlackListManager.getNetworkBlackListManagerInstance(this.mContext);
    }

    private void registerReceiverAndHandler() {
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mIntentFilter.addAction(ACTION_RESPONSE_WIFI_2_WIFI);
        this.mIntentFilter.addAction(ACTION_RESPONSE_DUAL_BAND_WIFI_HANDOVER);
        this.mNetwoksHandoverType = -1;
        this.mNetwoksHandoverState = 0;
        this.mNetworkHandler = new Handler() {
            public void handleMessage(Message msg) {
                int code;
                Bundle bundle;
                String ssid;
                String configKey;
                switch (msg.what) {
                    case 1:
                        if (WifiHandover.this.mNetworkHandler.hasMessages(7)) {
                            WifiHandover.this.mNetworkHandler.removeMessages(7);
                        }
                        if (WifiHandover.this.mNetwoksHandoverState != 1) {
                            if (WifiHandover.this.mNetwoksHandoverState == 4) {
                                WifiHandover.this.trySwitchWifiNetwork(WifiHandover.this.selectQualifiedCandidate());
                                break;
                            }
                        }
                        WifiHandover.this.handleScanResultsForConnectWiFi();
                        break;
                        break;
                    case 2:
                        boolean connectStatus = false;
                        WifiInfo newWifiInfo = WifiHandover.this.mWifiManager.getConnectionInfo();
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("HANDLER_CMD_WIFI_CONNECTED, newWifiInfo = ");
                        stringBuilder.append(newWifiInfo);
                        WifiHandover.LOGD(stringBuilder.toString());
                        if (!(newWifiInfo == null || newWifiInfo.getBSSID() == null || !newWifiInfo.getBSSID().equals(WifiHandover.this.mToConnectBssid))) {
                            connectStatus = true;
                        }
                        WifiHandover.this.sendNetworkHandoverResult(WifiHandover.this.mNetwoksHandoverType, connectStatus, WifiHandover.this.mToConnectBssid, 0);
                        WifiHandover.this.mNetwoksHandoverType = -1;
                        WifiHandover.this.mNetwoksHandoverState = 0;
                        WifiHandover.this.mToConnectBssid = null;
                        break;
                    case 3:
                        code = msg.arg1;
                        bundle = msg.obj;
                        ssid = bundle.getString(WifiHandover.WIFI_HANDOVER_NETWORK_SSID);
                        configKey = bundle.getString(WifiHandover.WIFI_HANDOVER_NETWORK_CONFIGKYE);
                        boolean handoverStatusOk = false;
                        if (!(code != 0 || configKey == null || configKey.equals(WifiHandover.this.mOldConfigKey))) {
                            handoverStatusOk = true;
                            WifiHandover.this.mNetworkQosMonitor.resetMonitorStatus();
                        }
                        if ((configKey != null && code != 0) || handoverStatusOk) {
                            WifiHandover.LOGD("HANDLER_CMD_WIFI_2_WIFI , wifi 2 wifi cleanTempBlackList");
                            WifiHandover.this.mNetworkBlackListManager.cleanTempWifiBlackList();
                        } else if (WifiHandover.this.mNetworkBlackListManager.isFailedMultiTimes(WifiHandover.this.mTargetBssid)) {
                            WifiHandover.LOGD("HANDLER_CMD_WIFI_2_WIFI failed!, add to abnormal black list");
                            WifiHandover.this.mNetworkBlackListManager.addAbnormalWifiBlacklist(WifiHandover.this.mTargetBssid);
                            WifiHandover.this.mNetworkBlackListManager.cleanTempWifiBlackList();
                        }
                        WifiHandover.this.sendNetworkHandoverResult(WifiHandover.this.mNetwoksHandoverType, handoverStatusOk, ssid, 0);
                        WifiHandover.this.mNetwoksHandoverType = -1;
                        WifiHandover.this.mNetwoksHandoverState = 0;
                        WifiHandover.this.mOldConfigKey = null;
                        WifiHandover.this.mSwitchBlacklist = null;
                        WifiHandover.this.mTargetBssid = null;
                        break;
                    case 4:
                        WifiHandover.this.notifyGoodApFound();
                        break;
                    case 5:
                        code = msg.arg1;
                        bundle = msg.obj;
                        ssid = bundle.getString(WifiHandover.WIFI_HANDOVER_NETWORK_SSID);
                        configKey = bundle.getString(WifiHandover.WIFI_HANDOVER_NETWORK_BSSID);
                        String dualBandConfigKey = bundle.getString(WifiHandover.WIFI_HANDOVER_NETWORK_CONFIGKYE);
                        boolean dualbandhandoverOK = false;
                        if (code == 0 && WifiHandover.this.mToConnectConfigKey != null && WifiHandover.this.mToConnectConfigKey.equals(dualBandConfigKey) && WifiHandover.this.mToConnectDualbandBssid != null && WifiHandover.this.mToConnectDualbandBssid.equals(configKey)) {
                            dualbandhandoverOK = true;
                            WifiHandover.this.mNetworkQosMonitor.resetMonitorStatus();
                        }
                        if (!(dualbandhandoverOK || code == -7)) {
                            code = -6;
                        }
                        WifiHandover.this.sendNetworkHandoverResult(WifiHandover.this.mNetwoksHandoverType, dualbandhandoverOK, ssid, code);
                        WifiHandover.this.mNetwoksHandoverType = -1;
                        WifiHandover.this.mNetwoksHandoverState = 0;
                        WifiHandover.this.mToConnectConfigKey = null;
                        WifiHandover.this.mToConnectDualbandBssid = null;
                        break;
                    case 6:
                        WifiHandover.this.mNetworkBlackListManager.cleanAbnormalWifiBlacklist();
                        break;
                    case 7:
                        WifiHandover.LOGD("HANDLER_CMD_REQUEST_SCAN_TIME_OUT");
                        WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 1));
                        break;
                }
                super.handleMessage(msg);
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int status;
                String bssid;
                String ssid;
                String configKey;
                Bundle bundle;
                StringBuilder stringBuilder;
                if ("android.net.wifi.SCAN_RESULTS".equals(intent.getAction())) {
                    if (WifiHandover.this.mNetwoksHandoverState == 1 || WifiHandover.this.mNetwoksHandoverState == 4) {
                        WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 1));
                    } else if (WifiProCommonUtils.isWifiConnected(WifiHandover.this.mWifiManager) && WifiHandover.this.mNetwoksHandoverState == 0) {
                        WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 4));
                    }
                } else if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
                    NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (netInfo == null || netInfo.getState() != State.CONNECTED) {
                        if (netInfo != null && netInfo.getState() == State.DISCONNECTED) {
                            WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 6));
                        }
                    } else if (WifiHandover.this.mNetwoksHandoverState == 1 || WifiHandover.this.mNetwoksHandoverState == 2) {
                        WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 2));
                    }
                } else if (WifiHandover.ACTION_RESPONSE_WIFI_2_WIFI.equals(intent.getAction())) {
                    if (WifiHandover.this.mNetwoksHandoverState == 3) {
                        status = intent.getIntExtra(WifiHandover.WIFI_HANDOVER_COMPLETED_STATUS, -100);
                        bssid = intent.getStringExtra(WifiHandover.WIFI_HANDOVER_NETWORK_BSSID);
                        ssid = intent.getStringExtra(WifiHandover.WIFI_HANDOVER_NETWORK_SSID);
                        configKey = intent.getStringExtra(WifiHandover.WIFI_HANDOVER_NETWORK_CONFIGKYE);
                        bundle = new Bundle();
                        bundle.putString(WifiHandover.WIFI_HANDOVER_NETWORK_BSSID, bssid);
                        bundle.putString(WifiHandover.WIFI_HANDOVER_NETWORK_SSID, ssid);
                        bundle.putString(WifiHandover.WIFI_HANDOVER_NETWORK_CONFIGKYE, configKey);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("ACTION_RESPONSE_WIFI_2_WIFI received, status = ");
                        stringBuilder.append(status);
                        stringBuilder.append(", configKey = ");
                        stringBuilder.append(configKey);
                        stringBuilder.append(", mOldConfigKey = ");
                        stringBuilder.append(WifiHandover.this.mOldConfigKey);
                        WifiHandover.LOGW(stringBuilder.toString());
                        WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 3, status, -1, bundle));
                    }
                } else if (WifiHandover.ACTION_RESPONSE_DUAL_BAND_WIFI_HANDOVER.equals(intent.getAction()) && WifiHandover.this.mNetwoksHandoverState == 5) {
                    status = intent.getIntExtra(WifiHandover.WIFI_HANDOVER_COMPLETED_STATUS, -100);
                    bssid = intent.getStringExtra(WifiHandover.WIFI_HANDOVER_NETWORK_BSSID);
                    ssid = intent.getStringExtra(WifiHandover.WIFI_HANDOVER_NETWORK_SSID);
                    configKey = intent.getStringExtra(WifiHandover.WIFI_HANDOVER_NETWORK_CONFIGKYE);
                    bundle = new Bundle();
                    bundle.putString(WifiHandover.WIFI_HANDOVER_NETWORK_BSSID, bssid);
                    bundle.putString(WifiHandover.WIFI_HANDOVER_NETWORK_SSID, ssid);
                    bundle.putString(WifiHandover.WIFI_HANDOVER_NETWORK_CONFIGKYE, configKey);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ACTION_RESPONSE_DUAL_BAND_WIFI_HANDOVER received, status = ");
                    stringBuilder.append(status);
                    stringBuilder.append(", ssid = ");
                    stringBuilder.append(ssid);
                    stringBuilder.append(", configKey = ");
                    stringBuilder.append(configKey);
                    WifiHandover.LOGD(stringBuilder.toString());
                    WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 5, status, -1, bundle));
                }
            }
        };
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter, WIFI_HANDOVER_RECV_PERMISSION, null);
    }

    private void sendNetworkHandoverResult(int type, boolean status, String ssid, int errorReason) {
        if (this.mCallBack != null && type != -1) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendNetworkHandoverResult, type = ");
            stringBuilder.append(type);
            stringBuilder.append(", status = ");
            stringBuilder.append(status);
            stringBuilder.append(" ,errorReason = ");
            stringBuilder.append(errorReason);
            LOGW(stringBuilder.toString());
            this.mCallBack.onWifiHandoverChange(type, status, ssid, errorReason);
        }
    }

    private void notifyWifiAvailableStatus(boolean status, int bestRssi, String targetSsid, int freq) {
        if (this.mCallBack == null) {
            LOGW("notifyWifiAvailableStatus, mCallBack == null");
        } else {
            this.mCallBack.onCheckAvailableWifi(status, bestRssi, targetSsid, freq);
        }
    }

    private int notifyGoodApFound() {
        int nextRssi = INVALID_RSSI;
        int nextFreq = -1;
        String nextSsid = null;
        if (this.mWifiStateMachine != null && this.mWifiStateMachine.isScanAndManualConnectMode()) {
            return INVALID_RSSI;
        }
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        List<ScanResult> list;
        if (scanResults == null) {
        } else if (scanResults.size() == 0) {
            list = scanResults;
        } else {
            List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
            List<WifiConfiguration> list2;
            if (configNetworks == null) {
                list2 = configNetworks;
            } else if (configNetworks.size() == 0) {
                list = scanResults;
                list2 = configNetworks;
            } else {
                WifiConfiguration current;
                String currentSsid;
                WifiConfiguration current2 = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
                String currentSsid2 = WifiProCommonUtils.getCurrentSsid(this.mWifiManager);
                String currentBssid = WifiProCommonUtils.getCurrentBssid(this.mWifiManager);
                String currentEncrypt = current2 != null ? current2.configKey() : null;
                int nextId = -1;
                int i = 0;
                while (i < scanResults.size()) {
                    ScanResult nextResult = (ScanResult) scanResults.get(i);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("\"");
                    stringBuilder.append(nextResult.SSID);
                    stringBuilder.append("\"");
                    String scanSsid = stringBuilder.toString();
                    String scanResultEncrypt = nextResult.capabilities;
                    boolean sameBssid = currentBssid != null && currentBssid.equals(nextResult.BSSID);
                    boolean sameConfigKey = currentSsid2 != null && currentSsid2.equals(scanSsid) && WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, currentEncrypt);
                    if (sameBssid) {
                        list = scanResults;
                        list2 = configNetworks;
                        current = current2;
                        currentSsid = currentSsid2;
                    } else {
                        if (sameConfigKey) {
                            list = scanResults;
                            list2 = configNetworks;
                            current = current2;
                        } else {
                            list = scanResults;
                            current = current2;
                            if (this.mNetworkBlackListManager.isInAbnormalWifiBlacklist(nextResult.BSSID) != null) {
                                list2 = configNetworks;
                            } else {
                                currentSsid = currentSsid2;
                                scanResults = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(nextResult.frequency, nextResult.level);
                                if (scanResults <= 2) {
                                    list2 = configNetworks;
                                } else {
                                    if (nextResult.level > nextRssi) {
                                        current2 = null;
                                        while (current2 < configNetworks.size()) {
                                            WifiConfiguration currentSsid3 = (WifiConfiguration) configNetworks.get(current2);
                                            int signalLevel = scanResults;
                                            scanResults = currentSsid3.getNetworkSelectionStatus().getNetworkSelectionDisableReason();
                                            list2 = configNetworks;
                                            if (currentSsid3.noInternetAccess == null && !currentSsid3.portalNetwork && scanResults <= null && !WifiProCommonUtils.isOpenAndPortal(currentSsid3) && !WifiProCommonUtils.isOpenAndMaybePortal(currentSsid3) && currentSsid3.SSID != null && currentSsid3.SSID.equals(scanSsid) && WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, currentSsid3.configKey())) {
                                                nextRssi = nextResult.level;
                                                int nextId2 = currentSsid3.networkId;
                                                nextSsid = nextResult.SSID;
                                                nextFreq = nextResult.frequency;
                                                nextId = nextId2;
                                                break;
                                            }
                                            current2++;
                                            scanResults = signalLevel;
                                            configNetworks = list2;
                                        }
                                    }
                                    list2 = configNetworks;
                                }
                            }
                        }
                        currentSsid = currentSsid2;
                    }
                    i++;
                    scanResults = list;
                    current2 = current;
                    currentSsid2 = currentSsid;
                    configNetworks = list2;
                }
                list2 = configNetworks;
                current = current2;
                currentSsid = currentSsid2;
                notifyWifiAvailableStatus(nextRssi != INVALID_RSSI ? 1 : 0, nextRssi, nextSsid, nextFreq);
                return nextRssi;
            }
            LOGW("notifyGoodApFound, WiFi configured networks are invalid, getConfiguredNetworks is null");
            return INVALID_RSSI;
        }
        LOGW("notifyGoodApFound, WiFi scan results are invalid, getScanResults is null ");
        return INVALID_RSSI;
    }

    private boolean handleScanResultsForConnectWiFi() {
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        if (scanResults == null || scanResults.size() == 0) {
            LOGW("handleScanResultsForConnectWiFi, WiFi scan results are invalid, getScanResults is null");
            return false;
        }
        List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configNetworks == null || configNetworks.size() == 0) {
            LOGW("handleScanResultsForConnectWiFi, WiFi configured networks are invalid, getConfiguredNetworks is null");
            return false;
        }
        boolean found = false;
        WifiConfiguration nextConfig = null;
        int nextRssi = INVALID_RSSI;
        int nextId = -1;
        int i = 0;
        while (!found && i < scanResults.size()) {
            ScanResult nextResult = (ScanResult) scanResults.get(i);
            String scanSsid = new StringBuilder();
            scanSsid.append("\"");
            scanSsid.append(nextResult.SSID);
            scanSsid.append("\"");
            scanSsid = scanSsid.toString();
            if (nextResult.BSSID.equals(this.mToConnectBssid)) {
                WifiConfiguration nextConfig2 = nextConfig;
                for (int k = 0; k < configNetworks.size(); k++) {
                    nextConfig2 = (WifiConfiguration) configNetworks.get(k);
                    if (nextConfig2.SSID != null && nextConfig2.SSID.equals(scanSsid)) {
                        nextRssi = nextResult.level;
                        nextId = nextConfig2.networkId;
                        found = true;
                        break;
                    }
                }
                nextConfig = nextConfig2;
            }
            i++;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleScanResultsForConnectWiFi, nextId = ");
        stringBuilder.append(nextId);
        stringBuilder.append(", nextRssi = ");
        stringBuilder.append(nextRssi);
        LOGD(stringBuilder.toString());
        if (nextId == -1) {
            return false;
        }
        if (HwWifiServiceFactory.getHwWifiDevicePolicy().isWifiRestricted(nextConfig, false)) {
            Log.w(TAG, "MDM deny connect!");
            return false;
        }
        this.mNetwoksHandoverState = 2;
        this.mWifiManager.connect(nextId, null);
        return true;
    }

    /* JADX WARNING: Missing block: B:32:0x008a, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean invalidConfigNetwork(WifiConfiguration config, int signalLevel, int currentLevel) {
        StringBuilder stringBuilder;
        if (this.mNetwoksHandoverState == 4 && this.mSwitchBlacklist != null) {
            int l = 0;
            while (l < this.mSwitchBlacklist.size()) {
                if (!config.SSID.equals(this.mSwitchBlacklist.get(l)) || (signalLevel > 3 && signalLevel - currentLevel >= 2)) {
                    l++;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("selectQualifiedCandidate, switch black list filter it, ssid = ");
                    stringBuilder.append(config.SSID);
                    LOGW(stringBuilder.toString());
                    return true;
                }
            }
        }
        int disableReason = config.getNetworkSelectionStatus().getNetworkSelectionDisableReason();
        if (disableReason < 2 || disableReason > 9) {
            return WifiProCommonUtils.isOpenAndPortal(config) || WifiProCommonUtils.isOpenAndMaybePortal(config) || config.noInternetAccess || config.portalNetwork;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("selectQualifiedCandidate, wifi switch, ssid = ");
            stringBuilder.append(config.SSID);
            stringBuilder.append(", disableReason = ");
            stringBuilder.append(disableReason);
            LOGW(stringBuilder.toString());
            return true;
        }
    }

    private WifiSwitchCandidate getBetterSignalCandidate(WifiSwitchCandidate last, WifiSwitchCandidate current) {
        if (current == null || current.bestScanResult == null || (last != null && last.bestScanResult != null && last.bestScanResult.level >= current.bestScanResult.level)) {
            return last;
        }
        return current;
    }

    private WifiSwitchCandidate getBackupSwitchCandidate(WifiConfiguration currentConfig, ScanResult currentResult, WifiSwitchCandidate lastCandidate) {
        if (this.mNetwoksHandoverState == 4 && currentConfig.noInternetAccess && WifiProCommonUtils.allowWifiConfigRecovery(currentConfig.internetHistory)) {
            return getBetterSignalCandidate(lastCandidate, new WifiSwitchCandidate(currentConfig, currentResult));
        }
        return null;
    }

    private WifiSwitchCandidate getBestSwitchCandidate(WifiConfiguration currentConfig, ScanResult currentResult, WifiSwitchCandidate lastCandidate) {
        if (currentConfig == null || currentResult == null) {
            return lastCandidate;
        }
        if (lastCandidate == null || lastCandidate.bestWifiConfig == null || lastCandidate.bestScanResult == null) {
            return new WifiSwitchCandidate(currentConfig, currentResult);
        }
        if (this.mNetwoksHandoverState == 0 && SystemClock.elapsedRealtime() - (currentResult.timestamp / 1000) > 200) {
            return lastCandidate;
        }
        ScanResult lastResult = lastCandidate.bestScanResult;
        if (((ScanResult.is5GHz(lastResult.frequency) && ScanResult.is5GHz(currentResult.frequency)) || (ScanResult.is24GHz(lastResult.frequency) && ScanResult.is24GHz(currentResult.frequency))) && currentResult.level > lastResult.level) {
            return new WifiSwitchCandidate(currentConfig, currentResult);
        }
        if (ScanResult.is5GHz(lastResult.frequency) && ScanResult.is24GHz(currentResult.frequency) && lastResult.level < -72 && currentResult.level > lastResult.level) {
            return new WifiSwitchCandidate(currentConfig, currentResult);
        }
        if (ScanResult.is24GHz(lastResult.frequency) && ScanResult.is5GHz(currentResult.frequency) && (currentResult.level > lastResult.level || currentResult.level >= -72)) {
            return new WifiSwitchCandidate(currentConfig, currentResult);
        }
        return lastCandidate;
    }

    private WifiSwitchCandidate getAutoRoamCandidate(int currentSignalRssi, WifiSwitchCandidate bestCandidate) {
        if (!(this.mNetwoksHandoverState != 0 || bestCandidate == null || bestCandidate.bestScanResult == null)) {
            if (HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(bestCandidate.bestScanResult.frequency, bestCandidate.bestScanResult.level) - WifiProCommonUtils.getCurrenSignalLevel(this.mWifiManager.getConnectionInfo()) < 2 || bestCandidate.bestScanResult.level - currentSignalRssi < 10) {
                return null;
            }
        }
        return bestCandidate;
    }

    /* JADX WARNING: Removed duplicated region for block: B:53:0x0113  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00f9  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private WifiSwitchCandidate selectQualifiedCandidate() {
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        WifiConfiguration current = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
        int currentRssi = WifiProCommonUtils.getCurrentRssi(this.mWifiManager);
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        int currentLevel = WifiProCommonUtils.getCurrenSignalLevel(wifiInfo);
        WifiConfiguration wifiConfiguration;
        WifiInfo wifiInfo2;
        List<ScanResult> list;
        if (scanResults == null) {
            wifiConfiguration = current;
            wifiInfo2 = wifiInfo;
        } else if (scanResults.size() == 0) {
            list = scanResults;
            wifiConfiguration = current;
            wifiInfo2 = wifiInfo;
        } else {
            if (current == null) {
                wifiConfiguration = current;
                wifiInfo2 = wifiInfo;
            } else if (current.SSID == null) {
                list = scanResults;
                wifiConfiguration = current;
                wifiInfo2 = wifiInfo;
            } else if (this.mNetwoksHandoverState == 0 && currentLevel >= 4) {
                return null;
            } else {
                List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
                if (configNetworks == null) {
                    wifiConfiguration = current;
                    wifiInfo2 = wifiInfo;
                } else if (configNetworks.size() == 0) {
                    list = scanResults;
                    wifiConfiguration = current;
                    wifiInfo2 = wifiInfo;
                } else {
                    String currentSsid;
                    String currentBssid;
                    String currentEncrypt;
                    String currentSsid2 = current.SSID;
                    String currentBssid2 = WifiProCommonUtils.getCurrentBssid(this.mWifiManager);
                    String currentEncrypt2 = current.configKey();
                    WifiSwitchCandidate backupSwitchCandidate = null;
                    WifiSwitchCandidate bestSwitchCandidate = null;
                    int i = 0;
                    while (i < scanResults.size()) {
                        boolean switchConfigKeySame;
                        boolean idleConfigKeyDiff;
                        int signalLevel;
                        ScanResult nextResult = (ScanResult) scanResults.get(i);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("\"");
                        stringBuilder.append(nextResult.SSID);
                        stringBuilder.append("\"");
                        String scanSsid = stringBuilder.toString();
                        String scanResultEncrypt = nextResult.capabilities;
                        if (currentBssid2 != null) {
                            list = scanResults;
                            if (currentBssid2.equals(nextResult.BSSID) != null) {
                                scanResults = 1;
                                wifiConfiguration = current;
                                wifiInfo2 = wifiInfo;
                                switchConfigKeySame = this.mNetwoksHandoverState != 4 && currentSsid2.equals(scanSsid) && WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, currentEncrypt2);
                                idleConfigKeyDiff = (this.mNetwoksHandoverState == 0 || (currentSsid2.equals(scanSsid) && WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, currentEncrypt2))) ? false : true;
                                currentSsid = currentSsid2;
                                currentBssid = currentBssid2;
                                currentEncrypt = currentEncrypt2;
                                signalLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(nextResult.frequency, nextResult.level);
                                if (scanResults == null && !switchConfigKeySame && !idleConfigKeyDiff && signalLevel > 2) {
                                    if (this.mNetworkBlackListManager.isInAbnormalWifiBlacklist(nextResult.BSSID)) {
                                        int k = 0;
                                        while (k < configNetworks.size()) {
                                            boolean bssidSame;
                                            WifiConfiguration nextConfig = (WifiConfiguration) configNetworks.get(k);
                                            if (nextConfig != null) {
                                                bssidSame = scanResults;
                                                if (!(nextConfig.SSID == null || nextConfig.SSID.equals(scanSsid) == null || WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, nextConfig.configKey()) == null)) {
                                                    scanResults = 1;
                                                    if (scanResults == null && !invalidConfigNetwork(nextConfig, signalLevel, currentLevel)) {
                                                        backupSwitchCandidate = getBackupSwitchCandidate(nextConfig, nextResult, backupSwitchCandidate);
                                                        bestSwitchCandidate = getBestSwitchCandidate(nextConfig, nextResult, bestSwitchCandidate);
                                                        break;
                                                    }
                                                    k++;
                                                    scanResults = bssidSame;
                                                }
                                            } else {
                                                bssidSame = scanResults;
                                            }
                                            scanResults = null;
                                            if (scanResults == null) {
                                            }
                                            k++;
                                            scanResults = bssidSame;
                                        }
                                    } else {
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("selectQualifiedCandidate, scanSsid");
                                        stringBuilder2.append(scanSsid);
                                        stringBuilder2.append(" is in black list!");
                                        LOGW(stringBuilder2.toString());
                                    }
                                }
                                i++;
                                scanResults = list;
                                current = wifiConfiguration;
                                wifiInfo = wifiInfo2;
                                currentSsid2 = currentSsid;
                                currentBssid2 = currentBssid;
                                currentEncrypt2 = currentEncrypt;
                            }
                        } else {
                            list = scanResults;
                        }
                        scanResults = null;
                        wifiConfiguration = current;
                        wifiInfo2 = wifiInfo;
                        if (this.mNetwoksHandoverState != 4) {
                        }
                        if (this.mNetwoksHandoverState == 0) {
                        }
                        currentSsid = currentSsid2;
                        currentBssid = currentBssid2;
                        currentEncrypt = currentEncrypt2;
                        signalLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(nextResult.frequency, nextResult.level);
                        if (this.mNetworkBlackListManager.isInAbnormalWifiBlacklist(nextResult.BSSID)) {
                        }
                        i++;
                        scanResults = list;
                        current = wifiConfiguration;
                        wifiInfo = wifiInfo2;
                        currentSsid2 = currentSsid;
                        currentBssid2 = currentBssid;
                        currentEncrypt2 = currentEncrypt;
                    }
                    wifiConfiguration = current;
                    wifiInfo2 = wifiInfo;
                    currentSsid = currentSsid2;
                    currentBssid = currentBssid2;
                    currentEncrypt = currentEncrypt2;
                    if (bestSwitchCandidate == null && backupSwitchCandidate != null) {
                        bestSwitchCandidate = backupSwitchCandidate;
                    }
                    return getAutoRoamCandidate(currentRssi, bestSwitchCandidate);
                }
                LOGW("selectQualifiedCandidate, WiFi configured networks are invalid, getConfiguredNetworks is null");
                return null;
            }
            LOGW("selectQualifiedCandidate, current connected wifi configuration is null");
            return null;
        }
        LOGW("selectQualifiedCandidate, WiFi scan results are invalid, getScanResults is null");
        return null;
    }

    private void trySwitchWifiNetwork(WifiSwitchCandidate switchCandidate) {
        WifiConfiguration current = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
        if (current == null || switchCandidate == null) {
            LOGW("trySwitchWifiNetwork# CurrentWifiConfig is null or switchCandidate is null!!");
            if (this.mNetwoksHandoverState == 4) {
                sendNetworkHandoverResult(this.mNetwoksHandoverType, false, null, 0);
            }
            this.mNetwoksHandoverType = -1;
            this.mNetwoksHandoverState = 0;
            this.mOldConfigKey = null;
            this.mSwitchBlacklist = null;
            return;
        }
        WifiConfiguration best = switchCandidate.getWifiConfig();
        if (this.mWifiStateMachine != null) {
            if (this.mNetwoksHandoverState == 4) {
                this.mNetwoksHandoverState = 3;
                this.mWifiStateMachine.startWifi2WifiRequest();
            }
            StringBuilder stringBuilder;
            if (current.networkId == best.networkId || current.isLinked(best)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("trySwitchWifiNetwork: Soft reconnect from ");
                stringBuilder.append(current.SSID);
                stringBuilder.append(" to ");
                stringBuilder.append(best.SSID);
                LOGD(stringBuilder.toString());
                updateWifiSwitchTimeStamp(System.currentTimeMillis());
                this.mWifiStateMachine.startRoamToNetwork(best.networkId, switchCandidate.getScanResult());
            } else if (this.mNetwoksHandoverState == 3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("trySwitchWifiNetwork: Reconnect from ");
                stringBuilder.append(current.SSID);
                stringBuilder.append(" to ");
                stringBuilder.append(best.SSID);
                LOGD(stringBuilder.toString());
                String bssid = switchCandidate.getScanResult().BSSID;
                if (!TextUtils.isEmpty(bssid)) {
                    this.mTargetBssid = bssid;
                    if (this.mHwWifiCHRService != null) {
                        this.mHwWifiCHRService.updateConnectType("WIFIPRO_CONNECT");
                    }
                    updateWifiSwitchTimeStamp(System.currentTimeMillis());
                    this.mWifiStateMachine.requestWifiSoftSwitch();
                    this.mWifiStateMachine.startConnectToUserSelectNetwork(best.networkId, Binder.getCallingUid(), bssid);
                }
            }
        }
    }

    private void updateWifiSwitchTimeStamp(long ts) {
        WifiConfiguration config = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
        if (config != null && ts > 0) {
            config.lastTrySwitchWifiTimestamp = ts;
            this.mWifiStateMachine.sendMessage(HwWifiStateMachine.CMD_UPDATE_WIFIPRO_CONFIGURATIONS, config);
        }
    }

    public boolean handleWifiToWifi(ArrayList<String> invalidNetworks, int threshold, int qosLevel) {
        if (invalidNetworks == null) {
            LOGW("handleWifiToWifi, inputed arg is invalid, invalidNetworks is null");
            return false;
        } else if (this.mWifiStateMachine == null || !this.mWifiStateMachine.isScanAndManualConnectMode()) {
            this.mSwitchBlacklist = (ArrayList) invalidNetworks.clone();
            WifiConfiguration currConfig = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
            if (currConfig == null || currConfig.configKey() == null) {
                LOGW("handleWifiToWifi, getCurrentWifiConfig is null.");
                return false;
            }
            List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
            if (configNetworks == null || configNetworks.size() == 0) {
                LOGW("handleWifiToWifi, WiFi configured networks are invalid, getConfiguredNetworks is null");
                return false;
            }
            this.mOldConfigKey = currConfig.configKey();
            this.mNetwoksHandoverType = 1;
            this.mNetwoksHandoverState = 4;
            this.mNetworkHandler.sendMessageDelayed(Message.obtain(this.mNetworkHandler, 7), HANDOVER_WAIT_SCAN_TIME_OUT);
            requestScan();
            return true;
        } else {
            LOGW("Only allow Manual Connection, ignore auto connection.");
            return false;
        }
    }

    public boolean hasAvailableWifiNetwork(List<String> invalidNetworks, int threshold, String currBssid, String currSSid) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hasAvailableWifiNetwork, invalidNetworks = ");
        stringBuilder.append(invalidNetworks);
        stringBuilder.append(", threshold = ");
        stringBuilder.append(threshold);
        stringBuilder.append(", currSSid = ");
        stringBuilder.append(currSSid);
        LOGD(stringBuilder.toString());
        List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configNetworks == null || configNetworks.size() == 0) {
            LOGW("hasAvailableWifiNetwork, WiFi configured networks are invalid, getConfiguredNetworks is null");
            return false;
        }
        this.mNetwoksHandoverType = -1;
        this.mNetwoksHandoverState = 0;
        requestScan();
        return true;
    }

    private void requestScan() {
        if (this.mWifiStateMachine != null && this.mContext != null) {
            ScanRequestProxy scanRequest = WifiInjector.getInstance().getScanRequestProxy();
            if (scanRequest != null) {
                scanRequest.startScan(Binder.getCallingUid(), this.mContext.getOpPackageName());
            } else {
                LOGD("can't start wifi scan!");
            }
        }
    }

    public boolean connectWifiNetwork(String bssid) {
        if (bssid == null || bssid.length() == 0) {
            LOGW("connectWifiNetwork, inputed arg is invalid");
            return false;
        }
        WifiInfo currWifiInfo = this.mWifiManager.getConnectionInfo();
        if (currWifiInfo == null || !bssid.equals(currWifiInfo.getBSSID())) {
            List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
            if (configNetworks == null || configNetworks.size() == 0) {
                LOGW("connectWifiNetwork, WiFi configured networks are invalid, getConfiguredNetworks is null");
                return false;
            }
            this.mToConnectBssid = bssid;
            this.mNetwoksHandoverType = 2;
            this.mNetwoksHandoverState = 1;
            requestScan();
            return true;
        }
        LOGW("connectWifiNetwork, already connected, ignore it.");
        return true;
    }

    public boolean handleDualBandWifiConnect(String bssid, String ssid, int authType, int switchType) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DualBandWifiConnect, ssid = ");
        stringBuilder.append(ssid);
        stringBuilder.append(", authType = ");
        stringBuilder.append(authType);
        stringBuilder.append(", switchType = ");
        stringBuilder.append(switchType);
        Log.d(str, stringBuilder.toString());
        if (bssid == null || ssid == null || switchType < 1 || switchType > 3) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("DualBandWifiConnect, inputed arg is invalid, ssid is null  , switchType = ");
            stringBuilder2.append(switchType);
            Log.d(str2, stringBuilder2.toString());
            return false;
        }
        WifiInfo currWifiInfo = this.mWifiManager.getConnectionInfo();
        if (currWifiInfo == null || !bssid.equals(currWifiInfo.getBSSID())) {
            List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
            if (configNetworks == null || configNetworks.size() == 0) {
                Log.d(TAG, "DualBandWifiConnect, WiFi configured networks are invalid, getConfiguredNetworks is null");
                return false;
            }
            WifiConfiguration changeConfig = null;
            for (WifiConfiguration nextConfig : configNetworks) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("DualBandWifiConnect, nextConfig.SSID = ");
                stringBuilder3.append(nextConfig.SSID);
                LOGD(stringBuilder3.toString());
                if (isValidConfig(nextConfig) && ssid.equals(nextConfig.SSID) && authType == nextConfig.getAuthType()) {
                    changeConfig = nextConfig;
                    break;
                }
            }
            if (changeConfig == null) {
                Log.d(TAG, "DualBandWifiConnect, WifiConfiguration is null ");
                HwDualBandInformationManager mManager = HwDualBandInformationManager.getInstance();
                if (mManager != null) {
                    mManager.delectDualBandAPInfoBySsid(ssid, authType);
                }
                return false;
            }
            str = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("DualBandWifiConnect, changeConfig.configKey = ");
            stringBuilder4.append(changeConfig.configKey());
            stringBuilder4.append(", AuthType = ");
            stringBuilder4.append(changeConfig.getAuthType());
            Log.d(str, stringBuilder4.toString());
            changeConfig.BSSID = bssid;
            this.mToConnectDualbandBssid = bssid;
            this.mToConnectConfigKey = changeConfig.configKey();
            this.mNetwoksHandoverType = 4;
            this.mNetwoksHandoverState = 5;
            if (switchType == 1) {
                updateWifiSwitchTimeStamp(System.currentTimeMillis());
            }
            Intent intent = new Intent(ACTION_REQUEST_DUAL_BAND_WIFI_HANDOVER);
            Bundle mBundle = new Bundle();
            mBundle.putParcelable(WIFI_HANDOVER_NETWORK_WIFICONFIG, changeConfig);
            intent.putExtras(mBundle);
            intent.putExtra(WIFI_HANDOVER_NETWORK_SWITCHTYPE, switchType);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, WIFI_HANDOVER_RECV_PERMISSION);
            return true;
        }
        Log.d(TAG, "DualBandWifiConnect, already connected, ignore it.");
        return true;
    }

    public int getNetwoksHandoverType() {
        return this.mNetwoksHandoverType;
    }

    private boolean isValidConfig(WifiConfiguration config) {
        boolean z = false;
        if (config == null) {
            return false;
        }
        int cc = config.allowedKeyManagement.cardinality();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("config isValid cardinality=");
        stringBuilder.append(cc);
        Log.e(str, stringBuilder.toString());
        if (cc <= 1) {
            z = true;
        }
        return z;
    }

    private static void LOGD(String msg) {
        Log.d(TAG, msg);
    }

    private static void LOGW(String msg) {
        Log.w(TAG, msg);
    }
}
