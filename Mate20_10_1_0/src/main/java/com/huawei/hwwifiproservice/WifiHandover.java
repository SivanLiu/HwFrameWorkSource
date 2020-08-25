package com.huawei.hwwifiproservice;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
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
import android.util.wifi.HwHiLog;
import com.android.server.wifi.hwUtil.StringUtilEx;
import com.android.server.wifipro.WifiProCommonUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
    private static final String EVENT_SSID_SWITCH = "SsidSwitchEnter";
    private static final int HANDLER_CMD_NOTIFY_GOOD_AP = 4;
    private static final int HANDLER_CMD_REQUEST_SCAN_TIME_OUT = 7;
    private static final int HANDLER_CMD_SCAN_RESULTS = 1;
    private static final int HANDLER_CMD_WIFI_2_WIFI = 3;
    private static final int HANDLER_CMD_WIFI_CONNECTED = 2;
    private static final int HANDLER_CMD_WIFI_DISCONNECTED = 6;
    private static final int HANDOVER_MIN_LEVEL_INTERVAL = 2;
    public static final int HANDOVER_NO_CANDIDATE = 22;
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
    private static final int SIGNAL_FOUR_LEVEL = 4;
    private static final int SIGNAL_THREE_LEVEL = 3;
    private static final int SIGNAL_TWO_LEVEL = 2;
    public static final String TAG = "WifiHandover";
    private static final int TARGET_AP_QUALITYSCORE = 40;
    private static final int TYPE_BATTERY_PREFERENCE = 2;
    private static final int TYPE_NON_PREFERENCE = 0;
    private static final int TYPE_USER_PREFERENCE = 1;
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
    private IntentFilter mIntentFilter;
    private boolean mIsWiFiInternet = false;
    /* access modifiers changed from: private */
    public int mNetwoksHandoverState;
    private int mNetwoksHandoverType;
    /* access modifiers changed from: private */
    public NetworkBlackListManager mNetworkBlackListManager = null;
    /* access modifiers changed from: private */
    public Handler mNetworkHandler;
    private NetworkQosMonitor mNetworkQosMonitor;
    private String mOldConfigKey = null;
    private ArrayList<String> mSwitchBlacklist;
    private String mTargetBssid = null;
    private String mToConnectBssid;
    private String mToConnectConfigKey;
    private String mToConnectDualbandBssid = null;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;
    private WifiProChrUploadManager uploadManager;

    public WifiHandover(Context context, INetworksHandoverCallBack callBack) {
        this.mCallBack = callBack;
        this.mContext = context;
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
        Context context;
        this.mCallBack = null;
        BroadcastReceiver broadcastReceiver = this.mBroadcastReceiver;
        if (broadcastReceiver != null && (context = this.mContext) != null) {
            context.unregisterReceiver(broadcastReceiver);
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
        this.uploadManager = WifiProChrUploadManager.getInstance(this.mContext);
    }

    private void registerReceiverAndHandler() {
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.mIntentFilter.addAction("android.net.wifi.SCAN_RESULTS");
        this.mIntentFilter.addAction("com.huawei.wifi.action.RESPONSE_WIFI_2_WIFI");
        this.mIntentFilter.addAction("com.huawei.wifi.action.RESPONSE_DUAL_BAND_WIFI_HANDOVER");
        this.mNetwoksHandoverType = -1;
        this.mNetwoksHandoverState = 0;
        this.mNetworkHandler = new Handler() {
            /* class com.huawei.hwwifiproservice.WifiHandover.AnonymousClass1 */

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        if (WifiHandover.this.mNetworkHandler.hasMessages(7)) {
                            WifiHandover.this.mNetworkHandler.removeMessages(7);
                        }
                        if (WifiHandover.this.mNetwoksHandoverState != 1) {
                            if (WifiHandover.this.mNetwoksHandoverState == 4) {
                                WifiHandover wifiHandover = WifiHandover.this;
                                wifiHandover.trySwitchWifiNetwork(wifiHandover.selectQualifiedCandidate());
                                break;
                            }
                        } else {
                            boolean unused = WifiHandover.this.handleScanResultsForConnectWiFi();
                            break;
                        }
                        break;
                    case 2:
                        WifiHandover.this.handleWifiConnectMsg();
                        break;
                    case 3:
                        int code = msg.arg1;
                        if (!(msg.obj instanceof Bundle)) {
                            WifiHandover.logE("HANDLER_CMD_WIFI_2_WIFI:Class is not match");
                            break;
                        } else {
                            WifiHandover.this.handleWifi2WifiMsg((Bundle) msg.obj, code);
                            break;
                        }
                    case 4:
                        int unused2 = WifiHandover.this.notifyGoodApFound();
                        break;
                    case 5:
                        int handoverStatus = msg.arg1;
                        if (!(msg.obj instanceof Bundle)) {
                            WifiHandover.logE("NETWORK_HANDLER_CMD_DUAL_BAND_WIFI_CONNECT:Class is not match");
                            break;
                        } else {
                            WifiHandover.this.handleDualBandWifiConnectMsg((Bundle) msg.obj, handoverStatus);
                            break;
                        }
                    case 6:
                        WifiHandover.this.mNetworkBlackListManager.cleanAbnormalWifiBlacklist();
                        break;
                    case 7:
                        WifiHandover.logI("HANDLER_CMD_REQUEST_SCAN_TIME_OUT");
                        WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 1));
                        break;
                }
                super.handleMessage(msg);
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            /* class com.huawei.hwwifiproservice.WifiHandover.AnonymousClass2 */

            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    if ("android.net.wifi.SCAN_RESULTS".equals(intent.getAction())) {
                        if (WifiHandover.this.mNetwoksHandoverState == 1 || WifiHandover.this.mNetwoksHandoverState == 4) {
                            WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 1));
                        } else if (WifiProCommonUtils.isWifiConnected(WifiHandover.this.mWifiManager) && WifiHandover.this.mNetwoksHandoverState == 0) {
                            WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 4));
                        }
                    } else if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {
                        NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                        if (netInfo == null || netInfo.getState() != NetworkInfo.State.CONNECTED) {
                            if (netInfo != null && netInfo.getState() == NetworkInfo.State.DISCONNECTED) {
                                WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 6));
                            }
                        } else if (WifiHandover.this.mNetwoksHandoverState == 1 || WifiHandover.this.mNetwoksHandoverState == 2) {
                            WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 2));
                        }
                    } else if ("com.huawei.wifi.action.RESPONSE_WIFI_2_WIFI".equals(intent.getAction())) {
                        if (WifiHandover.this.mNetwoksHandoverState == 3) {
                            WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 3, intent.getIntExtra("com.huawei.wifi.handover.status", -100), -1, WifiHandover.this.buildBundleFromIntent(intent)));
                        }
                    } else if ("com.huawei.wifi.action.RESPONSE_DUAL_BAND_WIFI_HANDOVER".equals(intent.getAction()) && WifiHandover.this.mNetwoksHandoverState == 5) {
                        WifiHandover.this.mNetworkHandler.sendMessage(Message.obtain(WifiHandover.this.mNetworkHandler, 5, intent.getIntExtra("com.huawei.wifi.handover.status", -100), -1, WifiHandover.this.buildBundleFromIntent(intent)));
                    }
                }
            }
        };
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.mIntentFilter, "com.huawei.wifipro.permission.RECV.WIFI_HANDOVER", null);
    }

    /* access modifiers changed from: private */
    public void handleWifiConnectMsg() {
        boolean connectStatus = false;
        WifiInfo newWifiInfo = this.mWifiManager.getConnectionInfo();
        logI("HANDLER_CMD_WIFI_CONNECTED, newWifiInfo = " + newWifiInfo);
        if (!(newWifiInfo == null || newWifiInfo.getBSSID() == null || !newWifiInfo.getBSSID().equals(this.mToConnectBssid))) {
            connectStatus = true;
        }
        sendNetworkHandoverResult(this.mNetwoksHandoverType, connectStatus, this.mToConnectBssid, 0);
        this.mNetwoksHandoverType = -1;
        this.mNetwoksHandoverState = 0;
        this.mToConnectBssid = null;
    }

    /* access modifiers changed from: private */
    public void handleWifi2WifiMsg(Bundle bundle, int code) {
        if (bundle == null) {
            logW("handleWifi2WifiMsg , invalid parameter");
            return;
        }
        String bssid = bundle.getString("com.huawei.wifi.handover.bssid");
        String configKey = bundle.getString("com.huawei.wifi.handover.configkey");
        boolean handoverStatusOk = false;
        if (code == 0 && configKey != null && !configKey.equals(this.mOldConfigKey)) {
            handoverStatusOk = true;
        }
        if ((configKey != null && code != 0) || handoverStatusOk) {
            logI("HANDLER_CMD_WIFI_2_WIFI , wifi 2 wifi cleanTempBlackList");
            this.mNetworkBlackListManager.cleanTempWifiBlackList();
        } else if (this.mNetworkBlackListManager.isFailedMultiTimes(this.mTargetBssid)) {
            logI("HANDLER_CMD_WIFI_2_WIFI failed!, add to abnormal black list");
            this.mNetworkBlackListManager.addAbnormalWifiBlacklist(this.mTargetBssid);
            this.mNetworkBlackListManager.cleanTempWifiBlackList();
        }
        sendNetworkHandoverResult(this.mNetwoksHandoverType, handoverStatusOk, bssid, 0);
        this.mNetwoksHandoverType = -1;
        this.mNetwoksHandoverState = 0;
        this.mOldConfigKey = null;
        this.mSwitchBlacklist = null;
        this.mTargetBssid = null;
    }

    /* access modifiers changed from: private */
    public void handleDualBandWifiConnectMsg(Bundle result, int handoverStatus) {
        if (result == null) {
            logW("handleDualBandWifiConnectMsg , invalid parameter");
            return;
        }
        result.getString("com.huawei.wifi.handover.ssid");
        String dualBandBssid = result.getString("com.huawei.wifi.handover.bssid");
        String dualBandConfigKey = result.getString("com.huawei.wifi.handover.configkey");
        boolean dualbandhandoverOK = false;
        String str = this.mToConnectConfigKey;
        boolean isSameBssid = true;
        boolean isSameConfigKey = str != null && str.equals(dualBandConfigKey);
        String str2 = this.mToConnectDualbandBssid;
        if (str2 == null || !str2.equals(dualBandBssid)) {
            isSameBssid = false;
        }
        int status = handoverStatus;
        if (status == 0 && isSameConfigKey && isSameBssid) {
            dualbandhandoverOK = true;
        }
        if (!dualbandhandoverOK && status != -7) {
            status = -6;
        }
        sendNetworkHandoverResult(this.mNetwoksHandoverType, dualbandhandoverOK, dualBandBssid, status);
        this.mNetwoksHandoverType = -1;
        this.mNetwoksHandoverState = 0;
        this.mToConnectConfigKey = null;
        this.mToConnectDualbandBssid = null;
    }

    /* access modifiers changed from: private */
    public Bundle buildBundleFromIntent(Intent intent) {
        Bundle bundle = null;
        if (intent != null) {
            int status = intent.getIntExtra("com.huawei.wifi.handover.status", -100);
            String bssid = intent.getStringExtra("com.huawei.wifi.handover.bssid");
            String ssid = intent.getStringExtra("com.huawei.wifi.handover.ssid");
            String configKey = intent.getStringExtra("com.huawei.wifi.handover.configkey");
            bundle = new Bundle();
            bundle.putString("com.huawei.wifi.handover.bssid", bssid);
            bundle.putString("com.huawei.wifi.handover.ssid", ssid);
            bundle.putString("com.huawei.wifi.handover.configkey", configKey);
            if ("com.huawei.wifi.action.RESPONSE_WIFI_2_WIFI".equals(intent.getAction())) {
                logI("ACTION_RESPONSE_WIFI_2_WIFI received, status = " + status + ", ssid = " + StringUtilEx.safeDisplaySsid(ssid));
            } else if ("com.huawei.wifi.action.RESPONSE_DUAL_BAND_WIFI_HANDOVER".equals(intent.getAction())) {
                logI("ACTION_RESPONSE_DUAL_BAND_WIFI_HANDOVER received, status = " + status + ", ssid = " + StringUtilEx.safeDisplaySsid(ssid));
            } else {
                logW("unhandled action = " + intent.getAction());
            }
        }
        return bundle;
    }

    private void sendNetworkHandoverResult(int type, boolean status, String bssid, int errorReason) {
        if (this.mCallBack != null && type != -1) {
            logW("sendNetworkHandoverResult, type = " + type + ", status = " + status + ", bssid = " + StringUtilEx.safeDisplayBssid(bssid) + ", errorReason = " + errorReason);
            this.mCallBack.onWifiHandoverChange(type, status, bssid, errorReason);
        }
    }

    private void notifyWifiAvailableStatus(boolean status, int bestRssi, String targetBssid, int preferType, int freq) {
        INetworksHandoverCallBack iNetworksHandoverCallBack = this.mCallBack;
        if (iNetworksHandoverCallBack == null) {
            logW("notifyWifiAvailableStatus, mCallBack == null");
        } else {
            iNetworksHandoverCallBack.onCheckAvailableWifi(status, bestRssi, targetBssid, preferType, freq);
        }
    }

    /* access modifiers changed from: private */
    public int notifyGoodApFound() {
        int nextRssi;
        Bundle result;
        int nextRssi2 = INVALID_RSSI;
        String nextPreferBssid = null;
        int preferType = 0;
        Bundle result2 = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 18, new Bundle());
        boolean isScanAndManualConnectMode = false;
        if (result2 != null) {
            isScanAndManualConnectMode = result2.getBoolean("isScanAndManualConnectMode");
        }
        if (isScanAndManualConnectMode) {
            return INVALID_RSSI;
        }
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        if (scanResults != null) {
            if (scanResults.size() != 0) {
                List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
                if (configNetworks != null) {
                    if (configNetworks.size() != 0) {
                        WifiConfiguration current = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
                        String currentSsid = WifiProCommonUtils.getCurrentSsid(this.mWifiManager);
                        String currentBssid = WifiProCommonUtils.getCurrentBssid(this.mWifiManager);
                        String currentEncrypt = current != null ? current.configKey() : null;
                        WifiConfiguration preferConfig = getUserPreferredNetwork(currentSsid, configNetworks);
                        Iterator<ScanResult> it = scanResults.iterator();
                        int nextFreq = -1;
                        String nextPreferBssid2 = null;
                        boolean perferFound = false;
                        int nextPreferRssi = -200;
                        while (true) {
                            boolean sameSsid = false;
                            if (!it.hasNext()) {
                                break;
                            }
                            ScanResult nextResult = it.next();
                            String scanSsid = "\"" + nextResult.SSID + "\"";
                            String scanResultEncrypt = nextResult.capabilities;
                            boolean sameBssid = currentBssid != null && currentBssid.equals(nextResult.BSSID);
                            boolean sameConfigKey = currentSsid != null && currentSsid.equals(scanSsid) && WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, currentEncrypt);
                            if (sameBssid) {
                                result = result2;
                            } else if (sameConfigKey) {
                                result = result2;
                            } else {
                                result = result2;
                                if (this.mNetworkBlackListManager.isInAbnormalWifiBlacklist(nextResult.BSSID)) {
                                    logW("find bssid: " + StringUtilEx.safeDisplayBssid(nextResult.BSSID) + " is in abnormal wifi black list");
                                } else {
                                    if (preferConfig != null) {
                                        logI("scan: " + StringUtilEx.safeDisplaySsid(nextResult.SSID) + ", prefer: " + StringUtilEx.safeDisplaySsid(preferConfig.SSID));
                                        if (preferConfig.SSID != null && preferConfig.SSID.equals(scanSsid) && WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, preferConfig.configKey())) {
                                            sameSsid = true;
                                        }
                                        if (sameSsid) {
                                            logI("found USER PREFERED network: ssid=" + StringUtilEx.safeDisplaySsid(preferConfig.SSID) + ", cur rssi=" + nextResult.level + ", pre rssi=" + nextPreferRssi);
                                            if (nextResult.level > nextPreferRssi) {
                                                logI("update USER PREFERED network: rssi=" + nextPreferRssi);
                                                nextPreferRssi = nextResult.level;
                                                perferFound = true;
                                                nextPreferBssid2 = nextResult.BSSID;
                                                preferType = 1;
                                                nextPreferBssid = nextPreferBssid;
                                                result2 = result;
                                            }
                                        }
                                    }
                                    if (isBetterScanResult(configNetworks, nextResult, nextRssi2)) {
                                        nextRssi2 = nextResult.level;
                                        nextPreferBssid = nextResult.BSSID;
                                        nextFreq = nextResult.frequency;
                                    } else {
                                        nextPreferBssid = nextPreferBssid;
                                    }
                                    preferType = preferType;
                                    result2 = result;
                                }
                            }
                            nextPreferBssid = nextPreferBssid;
                            preferType = preferType;
                            result2 = result;
                        }
                        String nextBssid = nextPreferBssid;
                        int preferType2 = preferType;
                        if (perferFound) {
                            logI("final network: bssid=" + StringUtilEx.safeDisplayBssid(nextPreferBssid2));
                            nextRssi = nextPreferRssi;
                            nextBssid = nextPreferBssid2;
                        } else {
                            nextRssi = nextRssi2;
                            preferType2 = 0;
                        }
                        notifyWifiAvailableStatus(nextRssi != -200, nextRssi, nextBssid, preferType2, nextFreq);
                        return nextRssi;
                    }
                }
                logW("notifyGoodApFound, WiFi configured networks are invalid, getConfiguredNetworks is null");
                return INVALID_RSSI;
            }
        }
        logW("notifyGoodApFound, WiFi scan results are invalid, getScanResults is null ");
        return INVALID_RSSI;
    }

    private boolean isBetterScanResult(List<WifiConfiguration> configNetworks, ScanResult nextResult, int nextRssi) {
        if (configNetworks == null || configNetworks.size() == 0) {
            logW("WiFi configured networks are invalid, getConfiguredNetworks is null");
            return false;
        } else if (nextResult == null) {
            return false;
        } else {
            trackGoodApDetailslnfo(configNetworks, nextResult);
            if (HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(nextResult.frequency, nextResult.level) <= 2) {
                return false;
            }
            String scanSsid = "\"" + nextResult.SSID + "\"";
            String scanResultEncrypt = nextResult.capabilities;
            if (nextResult.level > nextRssi) {
                for (WifiConfiguration nextConfig : configNetworks) {
                    int disableReason = nextConfig.getNetworkSelectionStatus().getNetworkSelectionDisableReason();
                    if (!nextConfig.noInternetAccess && !nextConfig.portalNetwork && disableReason <= 0 && !WifiProCommonUtils.isOpenAndPortal(nextConfig) && !WifiProCommonUtils.isOpenAndMaybePortal(nextConfig) && nextConfig.SSID != null && nextConfig.SSID.equals(scanSsid) && WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, nextConfig.configKey())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /* access modifiers changed from: private */
    public boolean handleScanResultsForConnectWiFi() {
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        if (scanResults == null || scanResults.size() == 0) {
            logW("handleScanResultsForConnectWiFi, WiFi scan results are invalid, getScanResults is null");
            return false;
        }
        List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
        if (configNetworks == null || configNetworks.size() == 0) {
            logW("handleScanResultsForConnectWiFi, WiFi configured networks are invalid, getConfiguredNetworks is null");
            return false;
        }
        int nextId = -1;
        int nextRssi = INVALID_RSSI;
        boolean found = false;
        WifiConfiguration nextConfig = null;
        for (ScanResult nextResult : scanResults) {
            if (found) {
                break;
            }
            String scanSsid = "\"" + nextResult.SSID + "\"";
            if (nextResult.BSSID.equals(this.mToConnectBssid)) {
                Iterator<WifiConfiguration> it = configNetworks.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    nextConfig = it.next();
                    if (nextConfig.SSID != null && nextConfig.SSID.equals(scanSsid)) {
                        nextRssi = nextResult.level;
                        nextId = nextConfig.networkId;
                        found = true;
                        break;
                    }
                }
            }
        }
        logI("handleScanResultsForConnectWiFi, nextId = " + nextId + ", nextRssi = " + nextRssi);
        if (nextId == -1) {
            return false;
        }
        Bundle data = new Bundle();
        data.putParcelable("WifiConfiguration", nextConfig);
        data.putBoolean("isToast", false);
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 11, data);
        boolean isWifiRestricted = false;
        if (result != null) {
            isWifiRestricted = result.getBoolean("isWifiRestricted");
        }
        if (isWifiRestricted) {
            Log.w(TAG, "MDM deny connect!");
            return false;
        }
        this.mNetwoksHandoverState = 2;
        this.mWifiManager.connect(nextId, null);
        return true;
    }

    private boolean invalidConfigNetwork(WifiConfiguration config, int signalLevel, int currentLevel, String bssid) {
        if (this.mNetwoksHandoverState == 4 && this.mSwitchBlacklist != null) {
            int l = 0;
            while (l < this.mSwitchBlacklist.size()) {
                if (bssid == null || !bssid.equals(this.mSwitchBlacklist.get(l)) || (signalLevel > 3 && signalLevel - currentLevel >= 2)) {
                    l++;
                } else {
                    logW("selectQualifiedCandidate, switch black list filter it, bssid = " + StringUtilEx.safeDisplayBssid(bssid));
                    return true;
                }
            }
        }
        int disableReason = config.getNetworkSelectionStatus().getNetworkSelectionDisableReason();
        if (disableReason >= 2 && disableReason <= 9) {
            logW("selectQualifiedCandidate, wifi switch, ssid = " + StringUtilEx.safeDisplaySsid(config.SSID) + ", disableReason = " + disableReason);
            return true;
        } else if (WifiProCommonUtils.isOpenAndPortal(config) || WifiProCommonUtils.isOpenAndMaybePortal(config)) {
            HwHiLog.w(TAG, false, "ignore the open portal network known", new Object[0]);
            return true;
        } else if (WifiProCommonUtils.matchedRequestByHistory(config.internetHistory, 103)) {
            logW("ignore the network that has no internet history" + config.internetHistory);
            return true;
        } else if (!config.noInternetAccess && !config.portalNetwork) {
            return false;
        } else {
            HwHiLog.d(TAG, false, "ignore the network has no internet, ssid=%{public}s", new Object[]{config.SSID});
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
        HwHiLog.w(TAG, false, "BackupSwitchCandidate is null", new Object[0]);
        return null;
    }

    private WifiSwitchCandidate getBestSwitchCandidate(WifiConfiguration currentConfig, ScanResult currentResult, WifiSwitchCandidate lastCandidate) {
        if (currentConfig == null || currentResult == null) {
            HwHiLog.d(TAG, false, "currentConfig or currentResult = null", new Object[0]);
            return lastCandidate;
        } else if (lastCandidate == null || lastCandidate.bestWifiConfig == null || lastCandidate.bestScanResult == null) {
            HwHiLog.d(TAG, false, "lastCandidate or its bestWifiConfig or its bestScanResult  = null", new Object[0]);
            return new WifiSwitchCandidate(currentConfig, currentResult);
        } else if (this.mNetwoksHandoverState != 0 || SystemClock.elapsedRealtime() - (currentResult.timestamp / 1000) <= 200) {
            ScanResult lastResult = lastCandidate.bestScanResult;
            int currentScore = WifiProCommonUtils.calculateScore(lastResult);
            int newScore = WifiProCommonUtils.calculateScore(currentResult);
            dumpCandidateScore(newScore, currentScore, currentResult);
            if (newScore > currentScore || (newScore == currentScore && currentResult.level > lastResult.level)) {
                return new WifiSwitchCandidate(currentConfig, currentResult);
            }
            return lastCandidate;
        } else {
            HwHiLog.d(TAG, false, "skip the scan results if they are not found for this scan", new Object[0]);
            return lastCandidate;
        }
    }

    private void dumpCandidateScore(int newScore, int currentScore, ScanResult scanResult) {
        WifiManager wifiManager = this.mWifiManager;
        if (wifiManager == null || scanResult == null) {
            HwHiLog.d(TAG, false, "mWifiManager is null", new Object[0]);
        } else if (wifiManager.getVerboseLoggingLevel() > 0) {
            HwHiLog.i(TAG, false, "BSSID = %{public}s, is5g = %{public}s, supportedWifiCategory = %{public}d, rssi = %{public}d, newScore = %{public}d, currentScore= %{public}d", new Object[]{StringUtilEx.safeDisplayBssid(scanResult.BSSID), String.valueOf(ScanResult.is5GHz(scanResult.frequency)), Integer.valueOf(scanResult.supportedWifiCategory), Integer.valueOf(scanResult.level), Integer.valueOf(newScore), Integer.valueOf(currentScore)});
        }
    }

    private WifiSwitchCandidate getAutoRoamCandidate(int currentSignalRssi, WifiSwitchCandidate bestCandidate) {
        if (!(this.mNetwoksHandoverState != 0 || bestCandidate == null || bestCandidate.bestScanResult == null)) {
            if (HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(bestCandidate.bestScanResult.frequency, bestCandidate.bestScanResult.level) - WifiProCommonUtils.getCurrenSignalLevel(this.mWifiManager.getConnectionInfo()) < 2 || bestCandidate.bestScanResult.level - currentSignalRssi < 10) {
                HwHiLog.w(TAG, false, "AutoRoamCandidate is null", new Object[0]);
                return null;
            }
        }
        return bestCandidate;
    }

    public void updateWiFiInternetAccess(boolean isWiFiInternet) {
        this.mIsWiFiInternet = isWiFiInternet;
    }

    private boolean isNetworkSelectionAvailable(List<ScanResult> scanResults, WifiConfiguration currentConfiguration, List<WifiConfiguration> configNetworks) {
        if (scanResults == null || scanResults.size() == 0) {
            logW("isNetworkSelectionAvailable, get scan result invalid!");
            return false;
        } else if (currentConfiguration == null || currentConfiguration.SSID == null) {
            logW("isNetworkSelectionAvailable, current connected wifi configuration is null");
            return false;
        } else if (configNetworks != null && configNetworks.size() != 0) {
            return true;
        } else {
            logW("isNetworkSelectionAvailable, WiFi configured networks are invalid, getConfiguredNetworks is null");
            return false;
        }
    }

    private WifiSwitchCandidate getBetterWifiCandidate(WifiSwitchCandidate switchCandidate, List<ScanResult> scanResults, int currentLevel) {
        int targetLevel = INVALID_RSSI;
        int targetRssi = INVALID_RSSI;
        String targetBssid = null;
        WifiSwitchCandidate betterWifiCandidate = switchCandidate;
        if (betterWifiCandidate == null || !this.mIsWiFiInternet) {
            logI("getBetterWifiCandidate invalid mIsWiFiInternet = " + this.mIsWiFiInternet);
            return betterWifiCandidate;
        }
        Iterator<ScanResult> it = scanResults.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            ScanResult nextResult = it.next();
            if (isNetworkMatched(betterWifiCandidate.getWifiConfig(), "\"" + nextResult.SSID + "\"", nextResult.capabilities)) {
                targetRssi = nextResult.level;
                targetBssid = nextResult.BSSID;
                targetLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(nextResult.frequency, nextResult.level);
                logI("targetRssi=" + targetRssi + " targetLevel=" + targetLevel + " targetBssid=" + StringUtilEx.safeDisplayBssid(targetBssid) + " scanSsid=" + StringUtilEx.safeDisplaySsid(nextResult.SSID));
                break;
            }
        }
        if ((currentLevel == 3 && targetLevel >= 3) || (currentLevel == 4 && targetLevel == 4)) {
            WifiProEstimateApInfo apInfo = new WifiProEstimateApInfo();
            apInfo.setApBssid(targetBssid);
            if (betterWifiCandidate.getWifiConfig() == null) {
                return null;
            }
            apInfo.setEstimateApSsid(betterWifiCandidate.getWifiConfig().SSID);
            apInfo.setApRssi(targetRssi);
            this.mNetworkQosMonitor.getApHistoryQualityScoreForWifi2Wifi(apInfo);
            logI("WiFi2WiFi targetAp:HistoryScore " + apInfo.getRetHistoryScore());
            if (apInfo.getRetHistoryScore() < 40) {
                betterWifiCandidate = null;
            }
        }
        if (currentLevel != 4 || targetLevel > 3) {
            return betterWifiCandidate;
        }
        logI("currentLevel is level_4 and targetlevel <= level_3");
        return null;
    }

    /* access modifiers changed from: private */
    public WifiSwitchCandidate selectQualifiedCandidate() {
        String currentEncrypt;
        String currentBssid;
        String currentSsid;
        List<ScanResult> scanResults = this.mWifiManager.getScanResults();
        WifiConfiguration current = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
        List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
        WifiSwitchCandidate bestSwitchCandidate = null;
        if (!isNetworkSelectionAvailable(scanResults, current, configNetworks)) {
            return null;
        }
        WifiInfo wifiInfo = this.mWifiManager.getConnectionInfo();
        int currentLevel = WifiProCommonUtils.getCurrenSignalLevel(wifiInfo);
        String currentSsid2 = current.SSID;
        String currentBssid2 = WifiProCommonUtils.getCurrentBssid(this.mWifiManager);
        String currentEncrypt2 = current.configKey();
        WifiConfiguration preferConfig = getUserPreferredNetwork(currentSsid2, configNetworks);
        WifiSwitchCandidate preferSwitchCandidate = null;
        if (preferConfig == null && this.mNetwoksHandoverState == 0 && currentLevel >= 4) {
            HwHiLog.w(TAG, false, "CurrentLevel >= 4, QualifiedCandidate is null", new Object[0]);
            return null;
        }
        WifiSwitchCandidate backupSwitchCandidate = null;
        int nextPreferRssi = INVALID_RSSI;
        boolean perferFound = false;
        for (ScanResult nextResult : scanResults) {
            String scanSsid = "\"" + nextResult.SSID + "\"";
            String scanBssid = nextResult.BSSID;
            String scanResultEncrypt = nextResult.capabilities;
            if (isScanResultSkip(nextResult, currentSsid2, currentBssid2, currentEncrypt2)) {
                currentSsid = currentSsid2;
                currentBssid = currentBssid2;
                currentEncrypt = currentEncrypt2;
            } else {
                currentSsid = currentSsid2;
                currentBssid = currentBssid2;
                currentEncrypt = currentEncrypt2;
                int signalLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(nextResult.frequency, nextResult.level);
                if (signalLevel > 2) {
                    if (preferConfig != null) {
                        logI("scan: " + StringUtilEx.safeDisplaySsid(nextResult.SSID) + ", prefer: " + StringUtilEx.safeDisplaySsid(preferConfig.SSID));
                        if (isNetworkMatched(preferConfig, scanSsid, scanResultEncrypt) && !invalidConfigNetwork(preferConfig, signalLevel, currentLevel, scanBssid)) {
                            logI("found USER PREFERED network: ssid=" + StringUtilEx.safeDisplaySsid(preferConfig.SSID) + ", cur rssi=" + nextResult.level + ", pre rssi=" + nextPreferRssi);
                            if (nextResult.level > nextPreferRssi) {
                                logI("update USER PREFERED network: rssi=" + nextPreferRssi);
                                nextPreferRssi = nextResult.level;
                                preferSwitchCandidate = new WifiSwitchCandidate(preferConfig, nextResult);
                                perferFound = true;
                                current = current;
                                wifiInfo = wifiInfo;
                                scanResults = scanResults;
                                currentSsid2 = currentSsid;
                                currentBssid2 = currentBssid;
                                currentEncrypt2 = currentEncrypt;
                            }
                        }
                    }
                    Iterator<WifiConfiguration> it = configNetworks.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        WifiConfiguration nextConfig = it.next();
                        if (isNetworkMatched(nextConfig, scanSsid, scanResultEncrypt) && !invalidConfigNetwork(nextConfig, signalLevel, currentLevel, scanBssid)) {
                            WifiSwitchCandidate backupSwitchCandidate2 = getBackupSwitchCandidate(nextConfig, nextResult, backupSwitchCandidate);
                            bestSwitchCandidate = getBestSwitchCandidate(nextConfig, nextResult, bestSwitchCandidate);
                            backupSwitchCandidate = backupSwitchCandidate2;
                            break;
                        }
                    }
                    current = current;
                    wifiInfo = wifiInfo;
                    scanResults = scanResults;
                    currentSsid2 = currentSsid;
                    currentBssid2 = currentBssid;
                    currentEncrypt2 = currentEncrypt;
                }
            }
            current = current;
            wifiInfo = wifiInfo;
            scanResults = scanResults;
            currentSsid2 = currentSsid;
            currentBssid2 = currentBssid;
            currentEncrypt2 = currentEncrypt;
        }
        if (bestSwitchCandidate == null && backupSwitchCandidate != null) {
            HwHiLog.d(TAG, false, "Wifi Swtich: try to use the backup one to connect if no other choice.", new Object[0]);
            bestSwitchCandidate = backupSwitchCandidate;
        }
        if (perferFound && preferSwitchCandidate != null) {
            bestSwitchCandidate = preferSwitchCandidate;
            logI("final network: ssid=" + StringUtilEx.safeDisplaySsid(bestSwitchCandidate.getWifiConfig().SSID));
        }
        return getBetterWifiCandidate(getAutoRoamCandidate(WifiProCommonUtils.getCurrentRssi(this.mWifiManager), bestSwitchCandidate), scanResults, currentLevel);
    }

    private boolean isNetworkMatched(WifiConfiguration nextConfig, String scanSsid, String scanResultEncrypt) {
        if (nextConfig == null || nextConfig.SSID == null) {
            HwHiLog.w(TAG, false, "nextConfig or nextConfig.SSID is null", new Object[0]);
            return false;
        } else if (scanSsid == null || scanResultEncrypt == null) {
            HwHiLog.w(TAG, false, "scanSsid or scanResultEncrypt is null", new Object[0]);
            return false;
        } else {
            boolean isSameSsid = nextConfig.SSID.equals(scanSsid);
            boolean isSameEncryptType = WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, nextConfig.configKey());
            if (isSameSsid) {
                HwHiLog.w(TAG, false, "isSameEncryptType = %{public}s", new Object[]{Boolean.valueOf(isSameEncryptType)});
            }
            if (!isSameSsid || !isSameEncryptType) {
                return false;
            }
            HwHiLog.w(TAG, false, "find out the Confignetwork from the scanresults", new Object[0]);
            return true;
        }
    }

    private boolean isScanResultSkip(ScanResult nextResult, String currentSsid, String currentBssid, String currentEncrypt) {
        if (nextResult == null || currentSsid == null || currentBssid == null || currentEncrypt == null) {
            return true;
        }
        String scanSsid = "\"" + nextResult.SSID + "\"";
        String scanResultEncrypt = nextResult.capabilities;
        boolean bssidSame = currentBssid.equals(nextResult.BSSID);
        boolean isSameSsidAndEncryptType = currentSsid.equals(scanSsid) && WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, currentEncrypt);
        boolean switchConfigKeySame = this.mNetwoksHandoverState == 4 && isSameSsidAndEncryptType;
        boolean idleConfigKeyDiff = this.mNetwoksHandoverState == 0 && !isSameSsidAndEncryptType;
        if (bssidSame || switchConfigKeySame || idleConfigKeyDiff) {
            return true;
        }
        if (!this.mNetworkBlackListManager.isInAbnormalWifiBlacklist(nextResult.BSSID)) {
            return false;
        }
        logW("selectQualifiedCandidate, scanSsid" + StringUtilEx.safeDisplaySsid(scanSsid) + " is in black list!");
        return true;
    }

    private WifiConfiguration getUserPreferredNetwork(String currentSsid, List<WifiConfiguration> configNetworks) {
        Bundle result;
        HashMap<Integer, String> preferList;
        if (!WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 24, new Bundle()).getBoolean("isHwArbitrationManagerNotNull") || (result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 25, new Bundle())) == null || (preferList = (HashMap) result.getSerializable("preferList")) == null || !preferList.containsKey(0)) {
            return null;
        }
        for (WifiConfiguration nextConfig : configNetworks) {
            if (nextConfig.SSID != null && nextConfig.SSID.equals(preferList.get(0))) {
                logI("found user preferred network in configNW");
                int disableReason = nextConfig.getNetworkSelectionStatus().getNetworkSelectionDisableReason();
                if (!nextConfig.noInternetAccess && !nextConfig.portalNetwork && disableReason <= 0 && !WifiProCommonUtils.isOpenAndPortal(nextConfig) && !WifiProCommonUtils.isOpenAndMaybePortal(nextConfig)) {
                    logI("found avalible user preferred network: " + StringUtilEx.safeDisplaySsid(nextConfig.SSID));
                    return nextConfig;
                }
            }
        }
        return null;
    }

    /* access modifiers changed from: private */
    public void trySwitchWifiNetwork(WifiSwitchCandidate switchCandidate) {
        WifiConfiguration current = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
        if (current == null || switchCandidate == null) {
            logW("trySwitchWifiNetwork# CurrentWifiConfig is null or switchCandidate is null!!");
            if (this.mNetwoksHandoverState == 4) {
                sendNetworkHandoverResult(this.mNetwoksHandoverType, false, null, 22);
            }
            this.mNetwoksHandoverType = -1;
            this.mNetwoksHandoverState = 0;
            this.mOldConfigKey = null;
            this.mSwitchBlacklist = null;
            return;
        }
        WifiConfiguration best = switchCandidate.getWifiConfig();
        if (this.mNetwoksHandoverState == 4) {
            this.mNetwoksHandoverState = 3;
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 19, new Bundle());
        }
        if (current.networkId == best.networkId) {
            logI("trySwitchWifiNetwork: waiting for chipset roam!");
        } else if (this.mNetwoksHandoverState == 3) {
            logI("trySwitchWifiNetwork: Reconnect from " + StringUtilEx.safeDisplaySsid(current.SSID) + " to " + StringUtilEx.safeDisplaySsid(best.SSID));
            String bssid = switchCandidate.getScanResult().BSSID;
            if (!TextUtils.isEmpty(bssid)) {
                this.mTargetBssid = bssid;
                Bundle data = new Bundle();
                data.putString("WIFIPRO_CONNECT", "WIFIPRO_CONNECT");
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 15, data);
                updateWifiSwitchTimeStamp(System.currentTimeMillis());
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 21, new Bundle());
                data.clear();
                data.putInt("networkId", best.networkId);
                data.putInt("CallingUid", Binder.getCallingUid());
                data.putString("bssid", bssid);
                WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 22, data);
            }
        }
    }

    private void updateWifiSwitchTimeStamp(long ts) {
        WifiConfiguration config = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
        if (config != null && ts > 0) {
            config.lastTrySwitchWifiTimestamp = ts;
            Bundle data = new Bundle();
            data.putParcelable("WifiConfiguration", config);
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 16, data);
        }
    }

    public boolean handleWifiToWifi(ArrayList<String> invalidNetworks, int threshold, int qosLevel) {
        if (invalidNetworks == null) {
            logW("handleWifiToWifi, inputed arg is invalid, invalidNetworks is null");
            return false;
        }
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 18, new Bundle());
        boolean isScanAndManualConnectMode = false;
        if (result != null) {
            isScanAndManualConnectMode = result.getBoolean("isScanAndManualConnectMode");
        }
        if (isScanAndManualConnectMode) {
            logW("Only allow Manual Connection, ignore auto connection.");
            return false;
        }
        this.mSwitchBlacklist = (ArrayList) invalidNetworks.clone();
        WifiConfiguration currConfig = WifiProCommonUtils.getCurrentWifiConfig(this.mWifiManager);
        if (currConfig == null || currConfig.configKey() == null) {
            logW("handleWifiToWifi, getCurrentWifiConfig is null.");
            return false;
        }
        List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
        if (configNetworks == null || configNetworks.size() == 0) {
            logW("handleWifiToWifi, WiFi configured networks are invalid, getConfiguredNetworks is null");
            return false;
        }
        this.mOldConfigKey = currConfig.configKey();
        this.mNetwoksHandoverType = 1;
        this.mNetwoksHandoverState = 4;
        Handler handler = this.mNetworkHandler;
        handler.sendMessageDelayed(Message.obtain(handler, 7), HANDOVER_WAIT_SCAN_TIME_OUT);
        requestScan();
        WifiProChrUploadManager.uploadDisconnectedEvent(EVENT_SSID_SWITCH);
        if (this.uploadManager != null) {
            Bundle ssidSwitch = new Bundle();
            ssidSwitch.putInt(WifiproUtils.SWITCH_SUCCESS_INDEX, 0);
            this.uploadManager.addChrBundleStat(WifiproUtils.WIFI_SWITCH_EVENT, WifiproUtils.WIFI_SWITCH_CNT_EVENT, ssidSwitch);
        }
        return true;
    }

    public boolean hasAvailableWifiNetwork(List<String> invalidNetworks, int threshold, String currBssid, String currSSid) {
        logI("hasAvailableWifiNetwork, invalidNetworks = " + invalidNetworks + ", threshold = " + threshold + ", currSSid = " + StringUtilEx.safeDisplaySsid(currSSid));
        List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
        if (configNetworks == null || configNetworks.size() == 0) {
            logW("hasAvailableWifiNetwork, WiFi configured networks are invalid, getConfiguredNetworks is null");
            return false;
        }
        this.mNetwoksHandoverType = -1;
        this.mNetwoksHandoverState = 0;
        requestScan();
        return true;
    }

    private void requestScan() {
        if (this.mContext != null) {
            Bundle data = new Bundle();
            data.putInt("CallingUid", Binder.getCallingUid());
            data.putString("packageName", this.mContext.getOpPackageName());
            WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 23, data);
        }
    }

    public boolean connectWifiNetwork(String bssid) {
        if (bssid == null || bssid.length() == 0) {
            logW("connectWifiNetwork, inputed arg is invalid");
            return false;
        }
        WifiInfo currWifiInfo = this.mWifiManager.getConnectionInfo();
        if (currWifiInfo == null || !bssid.equals(currWifiInfo.getBSSID())) {
            List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
            if (configNetworks == null || configNetworks.size() == 0) {
                logW("connectWifiNetwork, WiFi configured networks are invalid, getConfiguredNetworks is null");
                return false;
            }
            this.mToConnectBssid = bssid;
            this.mNetwoksHandoverType = 2;
            this.mNetwoksHandoverState = 1;
            requestScan();
            return true;
        }
        logW("connectWifiNetwork, already connected, ignore it.");
        return true;
    }

    public int handleDualBandWifiConnect(String bssid, String ssid, int authType, int switchType) {
        HashMap<Integer, String> preferList;
        Log.d(TAG, "DualBandWifiConnect, ssid = " + StringUtilEx.safeDisplaySsid(ssid) + ", authType = " + authType + ", switchType = " + switchType);
        if (bssid == null || ssid == null) {
            Log.d(TAG, "DualBandWifiConnect, inputed arg is invalid, ssid is null ");
            return 1;
        } else if (switchType < 1 || switchType > 3) {
            Log.d(TAG, "DualBandWifiConnect, inputed arg is invalid, switchType = " + switchType);
            return 1;
        } else if (isBssidConnected(this.mWifiManager.getConnectionInfo(), bssid)) {
            Log.d(TAG, "DualBandWifiConnect, already connected, ignore it.");
            return 10;
        } else {
            List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
            if (configNetworks == null || configNetworks.size() == 0) {
                Log.d(TAG, "DualBandWifiConnect, WiFi configured networks are invalid, getConfiguredNetworks is null");
                return 1;
            }
            WifiConfiguration changeConfig = null;
            Iterator<WifiConfiguration> it = configNetworks.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                WifiConfiguration nextConfig = it.next();
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
                return 1;
            }
            if (WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 24, new Bundle()).getBoolean("isHwArbitrationManagerNotNull")) {
                Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 25, new Bundle());
                if (result == null || (preferList = (HashMap) result.getSerializable("preferList")) == null) {
                    return 1;
                }
                logI("getWifiPreferenceFromHiData: " + preferList.toString());
                if (preferList.containsKey(0) && !changeConfig.SSID.equals(preferList.get(0))) {
                    Log.d(TAG, "DualBandWifiConnect, found user preference but not target ssid, ignore it.");
                    return 11;
                }
            }
            Log.d(TAG, "DualBandWifiConnect, changeConfig.configKey = " + changeConfig.configKey() + ", AuthType = " + changeConfig.getAuthType());
            changeConfig.BSSID = bssid;
            this.mToConnectDualbandBssid = bssid;
            this.mToConnectConfigKey = changeConfig.configKey();
            this.mNetwoksHandoverType = 4;
            this.mNetwoksHandoverState = 5;
            if (switchType == 1) {
                updateWifiSwitchTimeStamp(System.currentTimeMillis());
            }
            Intent intent = new Intent("com.huawei.wifi.action.REQUEST_DUAL_BAND_WIFI_HANDOVER");
            Bundle mBundle = new Bundle();
            mBundle.putParcelable("com.huawei.wifi.handover.wificonfig", changeConfig);
            intent.putExtras(mBundle);
            intent.putExtra("com.huawei.wifi.handover.switchtype", switchType);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL, "com.huawei.wifipro.permission.RECV.WIFI_HANDOVER");
            return 0;
        }
    }

    public int getNetwoksHandoverType() {
        return this.mNetwoksHandoverType;
    }

    private boolean isValidConfig(WifiConfiguration config) {
        if (config == null) {
            return false;
        }
        int cc = config.allowedKeyManagement.cardinality();
        Log.e(TAG, "config isValid cardinality=" + cc);
        if (cc <= 1) {
            return true;
        }
        return false;
    }

    private boolean isBssidConnected(WifiInfo currWifiInfo, String bssid) {
        if (currWifiInfo == null || bssid == null) {
            return false;
        }
        String curBssid = currWifiInfo.getBSSID();
        if (curBssid != null) {
            return curBssid.equals(bssid);
        }
        Log.e(TAG, "curBssid is null, return.");
        return false;
    }

    /* access modifiers changed from: private */
    public static class WifiSwitchCandidate {
        /* access modifiers changed from: private */
        public ScanResult bestScanResult;
        /* access modifiers changed from: private */
        public WifiConfiguration bestWifiConfig;

        public WifiSwitchCandidate(WifiConfiguration bestWifiConfig2, ScanResult bestScanResult2) {
            this.bestWifiConfig = bestWifiConfig2;
            this.bestScanResult = bestScanResult2;
        }

        public WifiConfiguration getWifiConfig() {
            return this.bestWifiConfig;
        }

        public ScanResult getScanResult() {
            return this.bestScanResult;
        }
    }

    private void trackGoodApDetailslnfo(List<WifiConfiguration> configNetworks, ScanResult nextResult) {
        if (nextResult == null) {
            logW("trackGoodApDetailslnfo nextResult is null");
            return;
        }
        String scanSsid = "\"" + nextResult.SSID + "\"";
        String scanResultEncrypt = nextResult.capabilities;
        for (WifiConfiguration nextConfig : configNetworks) {
            if (nextConfig.SSID != null && nextConfig.SSID.equals(scanSsid) && WifiProCommonUtils.isSameEncryptType(scanResultEncrypt, nextConfig.configKey())) {
                int signalLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(nextResult.frequency, nextResult.level);
                int disableReason = nextConfig.getNetworkSelectionStatus().getNetworkSelectionDisableReason();
                boolean isPortalNetwork = nextConfig.portalNetwork;
                boolean isOpenAndPortal = WifiProCommonUtils.isOpenAndPortal(nextConfig);
                boolean isOpenAndMaybePortal = WifiProCommonUtils.isOpenAndMaybePortal(nextConfig);
                StringBuilder sb = new StringBuilder();
                sb.append("trackGoodApDetailslnfo SSID = ");
                sb.append(StringUtilEx.safeDisplaySsid(nextResult.SSID));
                sb.append(" BSSID = ");
                sb.append(StringUtilEx.safeDisplayBssid(nextResult.BSSID));
                sb.append(" signalLevel = ");
                sb.append(signalLevel);
                sb.append(" disableReason = ");
                sb.append(disableReason);
                sb.append(" hasInternetAccess = ");
                sb.append(!nextConfig.noInternetAccess);
                sb.append(" isPortalNetwork = ");
                sb.append(isPortalNetwork);
                sb.append(" isOpenAndPortal = ");
                sb.append(isOpenAndPortal);
                sb.append(" isOpenAndMaybePortal = ");
                sb.append(isOpenAndMaybePortal);
                sb.append(" internetHistory = ");
                sb.append(nextConfig.internetHistory);
                sb.append(" rssi = ");
                sb.append(nextResult.level);
                logW(sb.toString());
            }
        }
    }

    private static void logD(String msg) {
        Log.d(TAG, msg);
    }

    /* access modifiers changed from: private */
    public static void logI(String msg) {
        Log.i(TAG, msg);
    }

    private static void logW(String msg) {
        Log.w(TAG, msg);
    }

    /* access modifiers changed from: private */
    public static void logE(String msg) {
        Log.e(TAG, msg);
    }
}
