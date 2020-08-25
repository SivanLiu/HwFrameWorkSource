package com.huawei.hwwifiproservice;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Messenger;
import android.util.wifi.HwHiLog;
import com.android.server.wifi.wifipro.IHwWifiProService;
import com.huawei.hwwifiproservice.HwWifiProService;
import java.util.ArrayList;
import java.util.List;

public class HwWifiProService {
    public static final int INTERFACE_CALL_HWPORTALEXCEPTIONMANAGER_INIT = 29;
    public static final int INTERFACE_CALL_HWPORTALEXCEPTIONMANAGER_NOTIFY_DISCONNETCTED = 42;
    public static final int INTERFACE_CALL_SAVEDNETWORKEVALUATOR_NOTIFY_PORTAL_CHANGED = 43;
    public static final int INTERFACE_CHECK_WIFI_DEFAULT_ROUTE = 65;
    public static final int INTERFACE_DEAUTH_ROAMING_BSSID = 59;
    public static final int INTERFACE_DEL_STATIC_ARP = 58;
    public static final int INTERFACE_DO_GATWAY_ARP_TEST = 75;
    public static final int INTERFACE_DO_GRATUITOUSARP = 67;
    public static final int INTERFACE_DO_SLOW_ARP_TEST = 76;
    public static final int INTERFACE_GET_AP_VENDOR_INFO = 1;
    public static final int INTERFACE_GET_AVAILABLE_CHANNELS = 13;
    public static final int INTERFACE_GET_DHCP_RESULTS = 46;
    public static final int INTERFACE_GET_GATEWAY_ARP_RESULT = 68;
    public static final int INTERFACE_GET_GW_ADDR = 70;
    public static final int INTERFACE_GET_NETWORK_FOR_TYPE_WIFI = 14;
    public static final int INTERFACE_GET_RSSI_PACKET_COUNT_INFO = 80;
    public static final int INTERFACE_GET_SCAN_RESULTS = 74;
    public static final int INTERFACE_GET_SCN_RESULTS_FROM_WSM = 12;
    public static final int INTERFACE_GET_WIFI6_WITHOUT_HTC_ARP_RESULT = 84;
    public static final int INTERFACE_GET_WIFI6_WITH_HTC_ARP_RESULT = 83;
    public static final int INTERFACE_GET_WIFIPLUS_FLAG_FROM_HIDATA = 39;
    public static final int INTERFACE_GET_WIFISTATEMACHINE_MESSENGER = 78;
    public static final int INTERFACE_GET_WIFI_INFO = 52;
    public static final int INTERFACE_GET_WIFI_PREFERENCE_FROM_HIDATA = 25;
    public static final int INTERFACE_HANDLE_CONNECTION_STATE_CHANGED = 77;
    public static final int INTERFACE_HANDLE_INVALID_IPADDR = 49;
    public static final int INTERFACE_HANDLE_NO_INTERNET_IP = 45;
    public static final int INTERFACE_INCR_ACCESS_WEB_RECORD = 32;
    public static final int INTERFACE_IS_BSSID_DISABLED = 30;
    public static final int INTERFACE_IS_FULL_SCREEN = 9;
    public static final int INTERFACE_IS_HILINK_UNCONFIG_ROUTER = 34;
    public static final int INTERFACE_IS_HWARBITRATIONMANAGER_NOT_NULL = 24;
    public static final int INTERFACE_IS_IN_GAME_ADN_NEED_DISC = 27;
    public static final int INTERFACE_IS_KEEP_CURRMPLINK_CONNECTED = 40;
    public static final int INTERFACE_IS_REACHABLEBY_ICMP = 17;
    public static final int INTERFACE_IS_SCAN_AND_MANUAL_CONNECT_MODE = 18;
    public static final int INTERFACE_IS_WIFIPRO_EVALUATING_AP = 44;
    public static final int INTERFACE_IS_WIFI_RESTRICTED = 11;
    public static final int INTERFACE_MULTI_GATEWAY = 71;
    public static final int INTERFACE_NOTIFY_HWWIFIPROSERVICE_ACCOMPLISHED = 0;
    public static final int INTERFACE_NOTIFY_PORTAL_AUTHEN_STATUS = 41;
    public static final int INTERFACE_NOTIFY_PORTAL_CONNECTED_INFO = 5;
    public static final int INTERFACE_NOTIFY_SELF_ENGINE_RESET_COMPLETE = 62;
    public static final int INTERFACE_NOTIFY_SELF_ENGINE_STATE_END = 72;
    public static final int INTERFACE_NOTIFY_SELF_ENGINE_STATE_START = 69;
    public static final int INTERFACE_PIGN_GATWAY = 66;
    public static final int INTERFACE_PORTAL_NOTIFY_CHANGED = 33;
    public static final int INTERFACE_QUERY_11VROAMING_NETWORK = 26;
    public static final int INTERFACE_QUERY_BQE_RTT_RESULT = 7;
    public static final int INTERFACE_READ_TCP_STAT_LINES = 79;
    public static final int INTERFACE_REQUEST_REASSOC_LINK = 50;
    public static final int INTERFACE_REQUEST_RENEW_DHCP = 48;
    public static final int INTERFACE_REQUEST_RESET_WIFI = 51;
    public static final int INTERFACE_REQUEST_UPDATE_DNS_SERVERS = 54;
    public static final int INTERFACE_REQUEST_USE_STATIC_IPCONFIG = 53;
    public static final int INTERFACE_REQUEST_WIFI_SOFT_SWITCH = 21;
    public static final int INTERFACE_RESET_IPCONFIG_STATUS = 47;
    public static final int INTERFACE_RESET_WLAN_RTT = 6;
    public static final int INTERFACE_SEND_MESSAGE_TO_WIFISTATEMACHINE = 28;
    public static final int INTERFACE_SEND_QOE_CMD = 8;
    public static final int INTERFACE_SET_LAA_ENABLED = 31;
    public static final int INTERFACE_SET_STATIC_ARP = 57;
    public static final int INTERFACE_SET_WIFI_BACKGROUND_REASON = 55;
    public static final int INTERFACE_START_CONNECT_TO_USER_SELECT_NETWORK = 22;
    public static final int INTERFACE_START_CUSTOMIZED_SCAN = 10;
    public static final int INTERFACE_START_PROXY_SCAN = 23;
    public static final int INTERFACE_START_ROAM_TO_NETWORK = 20;
    public static final int INTERFACE_START_SCAN = 73;
    public static final int INTERFACE_START_WIFI2WIFI_REQUEST = 19;
    public static final int INTERFACE_UPDATE_ACCESS_WEB_EXCEPTION = 60;
    public static final int INTERFACE_UPDATE_AP_VENDOR_INFO = 3;
    public static final int INTERFACE_UPDATE_ARP_SUMMERY = 63;
    public static final int INTERFACE_UPDATE_CONNECT_TYPE = 15;
    public static final int INTERFACE_UPDATE_EVALUATE_SCAN_RESULT = 82;
    public static final int INTERFACE_UPDATE_VPN_STATE_CHANGED = 36;
    public static final int INTERFACE_UPDATE_WIFI_CONNECTION_MODE = 35;
    public static final int INTERFACE_UPDATE_WIFI_EXCEPTION = 4;
    public static final int INTERFACE_UPDATE_WIFI_SWITCH_TIME_STAMP = 16;
    public static final int INTERFACE_UPFATE_SC_CHR_COUNT = 61;
    public static final int INTERFACE_UPLOAD_DFT_EVENT = 2;
    public static final String SERVICE_NAME = "WIFIPRO_SERVICE";
    private static final String TAG = "HwWifiProService";
    private boolean isHwWifiProServiceInitCompleted = false;
    /* access modifiers changed from: private */
    public Context mContext;
    private Handler mEvaluateHandler;
    /* access modifiers changed from: private */
    public HwAutoConnectManager mHwAutoConnectManager = null;
    private HwSelfCureEngine mHwSelfCureEngine;
    private HwUidTcpMonitor mHwUidTcpMonitor;
    private HwWifiConnectivityMonitor mHwWifiConnectivityMonitor;
    private HwWifiProServiceManager mHwWifiProServiceManager = new HwWifiProServiceManager();
    private WifiProStateMachine mWifiProStateMachine;
    private WifiScanGenieController mWifiScanGenieController;
    private WifiproBqeUtils mWifiproBqeUtils;
    private Messenger messenger;

    public HwWifiProService(Context context) {
        HwHiLog.i(TAG, false, "addService HwWifiProService().", new Object[0]);
        this.mContext = context;
        WifiManagerEx.init(context);
    }

    /* access modifiers changed from: private */
    public final class HwWifiProServiceManager implements IHwWifiProService {
        private HwWifiProServiceManager() {
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public long getRttDuration(int uid, int type) {
            return HwUidTcpMonitor.getInstance(HwWifiProService.this.mContext).getRttDuration(uid, type);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public long getRttSegs(int uid, int type) {
            return HwUidTcpMonitor.getInstance(HwWifiProService.this.mContext).getRttSegs(uid, type);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyWifiLinkPoor(boolean flag) {
            WifiProStateMachine.getWifiProStateMachineImpl().notifyWifiLinkPoor(flag, -1);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyUseFullChannels() {
            if (HwWifiProFeatureControl.sWifiProScanGenieCtrl) {
                WifiScanGenieController.createWifiScanGenieControllerImpl(HwWifiProService.this.mContext).notifyUseFullChannels();
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public List<String> getScanfrequencys() {
            if (!HwWifiProFeatureControl.sWifiProScanGenieCtrl) {
                return null;
            }
            WifiScanGenieController.createWifiScanGenieControllerImpl(HwWifiProService.this.mContext).getScanfrequencys();
            List<String> result = new ArrayList<>();
            List<Integer> frequencylist = WifiScanGenieController.createWifiScanGenieControllerImpl(HwWifiProService.this.mContext).getScanfrequencys();
            if (frequencylist == null || frequencylist.size() <= 0) {
                return null;
            }
            int j = frequencylist.size();
            for (int i = 0; i < j; i++) {
                result.add(String.valueOf(frequencylist.get(i)));
            }
            return result;
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void handleWiFiDisconnected() {
            if (HwWifiProFeatureControl.sWifiProScanGenieCtrl) {
                WifiScanGenieController.createWifiScanGenieControllerImpl(HwWifiProService.this.mContext).handleWiFiDisconnected();
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyWifiConnectedBackground() {
            if (HwWifiProFeatureControl.sWifiProScanGenieCtrl) {
                WifiScanGenieController.createWifiScanGenieControllerImpl(HwWifiProService.this.mContext).notifyWifiConnectedBackground();
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void handleWiFiConnected(WifiConfiguration currentWifiConfig, boolean flag) {
            if (HwWifiProFeatureControl.sWifiProScanGenieCtrl) {
                WifiScanGenieController.createWifiScanGenieControllerImpl(HwWifiProService.this.mContext).handleWiFiConnected(currentWifiConfig, flag);
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyNetworkRoamingCompleted(String newBssid) {
            if (HwWifiProFeatureControl.sWifiProScanGenieCtrl) {
                WifiScanGenieController.createWifiScanGenieControllerImpl(HwWifiProService.this.mContext).notifyNetworkRoamingCompleted(newBssid);
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void userHandoverWifi() {
            WifiProStateMachine.getWifiProStateMachineImpl().userHandoverWifi();
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void setWifiApEvaluateEnabled(boolean enablen) {
            WifiProStateMachine.getWifiProStateMachineImpl().setWifiApEvaluateEnabled(enablen);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public int getNetwoksHandoverType() {
            return WifiProStateMachine.getWifiProStateMachineImpl().getNetwoksHandoverType();
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyNetworkUserConnect(boolean flag) {
            WifiProStateMachine.getWifiProStateMachineImpl().notifyNetworkUserConnect(flag);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyApkChangeWifiStatus(boolean flag, String packageName) {
            WifiProStateMachine.getWifiProStateMachineImpl().notifyApkChangeWifiStatus(flag, packageName);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyWifiDisconnected(Intent intent) {
            WifiProStateMachine.getWifiProStateMachineImpl().notifyWifiDisconnected(intent);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public boolean isWifiEvaluating() {
            return WifiProStateMachine.isWifiEvaluating();
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void disconnectePoorWifi() {
            HwWifiConnectivityMonitor.getInstance(HwWifiProService.this.mContext).disconnectePoorWifi();
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyForegroundAppChanged(String packageName) {
            HwWifiConnectivityMonitor.getInstance(HwWifiProService.this.mContext).notifyForegroundAppChanged(packageName);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyWifiMonitorDisconnected() {
            HwWifiConnectivityMonitor.getInstance(HwWifiProService.this.mContext).notifyWifiDisconnected();
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyWifiRoamingStarted() {
            HwWifiConnectivityMonitor.getInstance(HwWifiProService.this.mContext).notifyWifiRoamingStarted();
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyWifiConnectivityRoamingCompleted() {
            HwWifiConnectivityMonitor.getInstance(HwWifiProService.this.mContext).notifyWifiRoamingCompleted();
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public boolean isPortalNotifyOn() {
            if (HwWifiProService.this.mHwAutoConnectManager == null) {
                return false;
            }
            return HwAutoConnectManager.getInstance().isPortalNotifyOn();
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public boolean isAutoJoinAllowedSetTargetBssid(WifiConfiguration candidate, String scanResultBssid) {
            if (HwWifiProService.this.mHwAutoConnectManager == null) {
                return false;
            }
            return HwAutoConnectManager.getInstance().isAutoJoinAllowedSetTargetBssid(candidate, scanResultBssid);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void releaseBlackListBssid(WifiConfiguration config, boolean flag) {
            if (HwWifiProService.this.mHwAutoConnectManager != null) {
                HwAutoConnectManager.getInstance().releaseBlackListBssid(config, flag);
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyAutoConnectManagerDisconnected() {
            HwAutoConnectManager.getInstance().notifyNetworkDisconnected();
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyWifiConnFailedInfo(WifiConfiguration selectedConfig, String bssid, int rssi, int reason) {
            if (HwWifiProService.this.mHwAutoConnectManager != null) {
                HwAutoConnectManager.getInstance().notifyWifiConnFailedInfo(selectedConfig, bssid, rssi, reason);
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyEnableSameNetworkId(int netId) {
            if (HwWifiProService.this.mHwAutoConnectManager != null) {
                HwAutoConnectManager.getInstance().notifyEnableSameNetworkId(netId);
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public boolean allowAutoJoinDisabledNetworkAgain(WifiConfiguration config) {
            if (HwWifiProService.this.mHwAutoConnectManager == null) {
                return false;
            }
            return HwAutoConnectManager.getInstance().allowAutoJoinDisabledNetworkAgain(config);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public String getCurrentPackageNameFromWifiPro() {
            if (HwWifiProService.this.mHwAutoConnectManager == null) {
                return "";
            }
            return HwAutoConnectManager.getInstance().getCurrentPackageName();
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public boolean isBssidMatchedBlacklist(String bssid) {
            if (HwWifiProService.this.mHwAutoConnectManager == null) {
                return false;
            }
            return HwAutoConnectManager.getInstance().isBssidMatchedBlacklist(bssid);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public boolean allowCheckPortalNetwork(String configKey, String bssid) {
            if (HwWifiProService.this.mHwAutoConnectManager == null) {
                return false;
            }
            return HwAutoConnectManager.getInstance().allowCheckPortalNetwork(configKey, bssid);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void updatePopUpNetworkRssi(String configKey, int maxRssi) {
            if (HwWifiProService.this.mHwAutoConnectManager != null) {
                HwAutoConnectManager.getInstance().updatePopUpNetworkRssi(configKey, maxRssi);
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void setWiFiProScanResultList(List<ScanResult> list) {
            HwIntelligenceWiFiManager.setWiFiProScanResultList(list);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public List<ScanResult> updateScanResultByWifiPro(List<ScanResult> scanResults) {
            if (scanResults != null && scanResults.size() > 0) {
                for (ScanResult tmp : scanResults) {
                    WifiProConfigStore.updateScanDetailByWifiPro(tmp);
                }
            }
            return scanResults;
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public ScanResult updateScanDetailByWifiPro(ScanResult scanResult) {
            WifiProConfigStore.updateScanDetailByWifiPro(scanResult);
            return scanResult;
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public boolean isDualbandScanning() {
            return HwDualBandManager.getInstance().isDualbandScanning();
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public boolean isDhcpFailedBssid(String bssid) {
            if (HwSelfCureEngine.getInstance() == null) {
                return false;
            }
            return HwSelfCureEngine.getInstance().isDhcpFailedBssid(bssid);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public boolean isDhcpFailedConfigKey(String configKey) {
            if (HwSelfCureEngine.getInstance() == null) {
                return false;
            }
            return HwSelfCureEngine.getInstance().isDhcpFailedConfigKey(configKey);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyDhcpResultsInternetOk(String dhcpResults) {
            if (HwSelfCureEngine.getInstance() != null) {
                HwSelfCureEngine.getInstance().notifyDhcpResultsInternetOk(dhcpResults);
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifySelfCureWifiConnectedBackground() {
            if (HwSelfCureEngine.getInstance() != null) {
                HwSelfCureEngine.getInstance().notifyWifiConnectedBackground();
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifySelfCureWifiDisconnected() {
            if (HwSelfCureEngine.getInstance() != null) {
                HwSelfCureEngine.getInstance().notifyWifiDisconnected();
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifySelfCureWifiScanResultsAvailable(boolean flag) {
            if (HwSelfCureEngine.getInstance() != null) {
                HwSelfCureEngine.getInstance().notifyWifiScanResultsAvailable(flag);
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifySelfCureWifiRoamingCompleted(String newBssid) {
            if (HwSelfCureEngine.getInstance() != null) {
                HwSelfCureEngine.getInstance().notifyWifiRoamingCompleted(newBssid);
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifySelfCureIpConfigCompleted() {
            if (HwSelfCureEngine.getInstance() != null) {
                HwSelfCureEngine.getInstance().notifyIpConfigCompleted();
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public boolean notifySelfCureIpConfigLostAndHandle(WifiConfiguration config) {
            if (HwSelfCureEngine.getInstance() == null) {
                return false;
            }
            return HwSelfCureEngine.getInstance().notifyIpConfigLostAndHandle(config);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void requestChangeWifiStatus(boolean flag) {
            HwSelfCureEngine.getInstance().requestChangeWifiStatus(flag);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifySefCureCompleted(int status) {
            if (HwSelfCureEngine.getInstance() != null) {
                HwSelfCureEngine.getInstance().notifySefCureCompleted(status);
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void sendMessageToHwDualBandStateMachine(int message) {
            if (message == 7) {
                HwDualBandManager.getHwDualBandStateMachine().getStateMachineHandler().sendEmptyMessage(7);
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyFirstConnectProbeResult(int respCode) {
            HwHiLog.i(HwWifiProService.TAG, false, "notifyFirstConnectProbeResult = %{public}d", new Object[]{Integer.valueOf(respCode)});
            if (WifiProStateMachine.getWifiProStateMachineImpl() != null) {
                WifiProStateMachine.getWifiProStateMachineImpl().notifyWifiProConnect();
            }
            HwWifiProFeatureControl.getInstance().notifyFirstConnectProbeResult(respCode);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyTcpStatResult(List<String> list) {
            if (list == null) {
                HwHiLog.e(HwWifiProService.TAG, false, "list is null, return.", new Object[0]);
            } else {
                HwUidTcpMonitor.getInstance(HwWifiProService.this.mContext).parseTcpStatLines(list);
            }
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public Bundle getWifiDisplayInfo(NetworkInfo networkInfo) {
            return WifiProStateMachine.getWifiProStateMachineImpl().getWifiDisplayInfo(networkInfo);
        }

        @Override // com.android.server.wifi.wifipro.IHwWifiProService
        public void notifyChrEvent(int eventId, String apType, String ssid) {
            if (apType != null) {
                WifiProChrUploadManager.getInstance(HwWifiProService.this.mContext).post(new Runnable(eventId, apType, ssid) {
                    /* class com.huawei.hwwifiproservice.$$Lambda$HwWifiProService$HwWifiProServiceManager$_tWyWqC4zX1pjSGy44Fjln5Q5hk */
                    private final /* synthetic */ int f$1;
                    private final /* synthetic */ String f$2;
                    private final /* synthetic */ String f$3;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                        this.f$3 = r4;
                    }

                    public final void run() {
                        HwWifiProServiceManager.this.lambda$notifyChrEvent$0$HwWifiProService$HwWifiProServiceManager(this.f$1, this.f$2, this.f$3);
                    }
                });
            }
        }

        public /* synthetic */ void lambda$notifyChrEvent$0$HwWifiProService$HwWifiProServiceManager(int eventId, String apType, String ssid) {
            WifiProChrUploadManager.getInstance(HwWifiProService.this.mContext).updateFwkEvent(eventId, apType, ssid);
        }
    }

    public HwWifiProServiceManager getHwWifiProServiceManager() {
        return this.mHwWifiProServiceManager;
    }

    public void initHwWifiProService() {
        if (this.isHwWifiProServiceInitCompleted) {
            HwHiLog.i(TAG, false, "Service has already completed initialization!", new Object[0]);
            return;
        }
        HwHiLog.i(TAG, false, "Enter initHwWifiProService", new Object[0]);
        WifiProChrUploadManager.getInstance(this.mContext).setup();
        HwHiLog.i(TAG, false, "start create HwWifiConnectivityMonitor", new Object[0]);
        HwWifiConnectivityMonitor.getInstance(this.mContext).setup();
        HwWifiProFeatureControl.getInstance(this.mContext).init();
        HwHiLog.i(TAG, false, "start create WifiScanGenieController", new Object[0]);
        if (HwWifiProFeatureControl.sWifiProScanGenieCtrl) {
            WifiScanGenieController.createWifiScanGenieControllerImpl(this.mContext).handleWiFiDisconnected();
        }
        this.mHwAutoConnectManager = HwAutoConnectManager.getInstance();
        if (this.mHwAutoConnectManager != null) {
            HwAutoConnectManager.getInstance().notifyNetworkDisconnected();
        }
        Bundle bundle = new Bundle();
        if (WifiProStateMachine.getWifiProStateMachineImpl() != null) {
            bundle.putBoolean("isWifiProStateMachineNotNull", true);
        }
        if (HwAutoConnectManager.getInstance() != null) {
            bundle.putBoolean("isHwAutoConnectManagerNotNull", true);
        }
        if (HwDualBandManager.getInstance() != null) {
            bundle.putBoolean("isHwDualBandManagerNotNull", true);
        }
        if (HwSelfCureEngine.getInstance() != null) {
            bundle.putBoolean("isHwSelfCureEngineNotNull", true);
        }
        WifiManagerEx.ctrlHwWifiNetwork(SERVICE_NAME, 0, bundle);
        HwHiLog.i(TAG, false, "initHwWifiProService INTERFACE_NOTIFY_HWWIFIPROSERVICE_ACCOMPLISHED", new Object[0]);
        this.isHwWifiProServiceInitCompleted = true;
    }
}
