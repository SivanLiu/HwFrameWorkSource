package com.android.server.wifi;

import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;

public interface HwWifiCHRService {
    public static final int APKACTION_AWAYS_SCAN_ENABLE = 3;
    public static final int APKACTION_AWAYS_SCAN_TRIGGER = 4;
    public static final int APKACTION_DISABLE_NETWORK = 2;
    public static final int APKACTION_DISCONNECT_WIFI = 8;
    public static final int APKACTION_ENABLE_WIFI_FALSE = 1;
    public static final int APKACTION_ENABLE_WIFI_TRUE = 5;
    public static final int APKACTION_FOREGROUND_SCAN_TRIGGER = 9;
    public static final int APKACTION_FORGET_WIFINETWORK = 11;
    public static final int APKACTION_REASSOC_WIFI = 7;
    public static final int APKACTION_RECONNECT_WIFI = 6;
    public static final int APKACTION_REMOVE_WIFINETWORK = 12;
    public static final int APKACTION_SELECT_WIFINETWORK = 10;
    public static final int HIDATA_USER_CLOSE_WIFI_USING_SETTINGS = 4;
    public static final int HIDATA_USER_CLOSE_WIFI_USING_UI = 3;
    public static final int HIDATA_USER_CONNECT_ANOTHER_AP_USING_SETTINGS = 11;
    public static final int HIDATA_USER_CONNECT_WIFI_USING_SETTINGS = 9;
    public static final int HIDATA_USER_DISCONECT_WIFI_USING_SETTINGS = 10;
    public static final int HIDATA_USER_OPEN_WIFI_USING_SETTINGS = 2;
    public static final int HIDATA_USER_OPEN_WIFI_USING_UI = 1;

    void assocRejectEvent(int i);

    void connectFromUser(WifiConfiguration wifiConfiguration);

    void connectFromUserByConfig(WifiConfiguration wifiConfiguration);

    void dhcpfailedEvent(int i);

    void disableNetwork(int i, int i2);

    void forgetFromUser(int i);

    int getPersistedScanAlwaysAvailable();

    String getProxyInfo();

    int getProxyStatus();

    int getWIFINetworkAvailableNotificationON();

    int getWIFIProStatus();

    int getWIFISleepPolicy();

    int getWIFITOPDP();

    void handleSupplicantStateChange(SupplicantState supplicantState);

    void removeOpenCloseMsg(boolean z);

    void setRssi(int i);

    void updateApkChangewWifiStatus(int i, String str);

    void updateConnectState(WifiConfiguration wifiConfiguration);

    void updateConnectStateByConfig(WifiConfiguration wifiConfiguration);

    void updateDhcpFailed();

    void updateDhcpFailedState();

    void updateTargetBssid(String str);

    void updateWIFIConfiguraion(WifiConfiguration wifiConfiguration);

    void updateWIFIConfiguraionByConfig(WifiConfiguration wifiConfiguration);

    void updateWifiState(int i);

    void updateWifiTriggerState(boolean z);
}
