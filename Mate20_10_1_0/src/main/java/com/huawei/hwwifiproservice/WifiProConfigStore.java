package com.huawei.hwwifiproservice;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Message;
import android.util.Log;
import com.android.internal.util.AsyncChannel;
import com.android.server.wifi.hwUtil.StringUtilEx;
import java.util.List;

public class WifiProConfigStore {
    public static final int ACCESS_TYPE = 1;
    private static final int CMD_UPDATE_WIFI_CONFIGURATIONS = 131672;
    private static final boolean DBG = true;
    public static final int QOS_LEVEL = 2;
    private static final int QOS_SCORE = 3;
    private static final String TAG = "WiFi_PRO_WifiProConfigStore";
    private Context mContext;
    private WifiManager mWifiManager = ((WifiManager) this.mContext.getSystemService("wifi"));
    private AsyncChannel mWsmChannel;

    public WifiProConfigStore(Context context, AsyncChannel wsmChannel) {
        this.mContext = context;
        this.mWsmChannel = wsmChannel;
    }

    private void updateWifiConfig(WifiConfiguration config) {
        if (config != null) {
            Message msg = Message.obtain();
            msg.what = 131672;
            msg.obj = config;
            msg.arg1 = 1;
            this.mWsmChannel.sendMessage(msg);
        }
    }

    public void updateWifiNoInternetAccessConfig(WifiConfiguration config, boolean noInternet, int reason, boolean noHandover) {
        if (config != null) {
            config.wifiProNoInternetAccess = noInternet;
            config.wifiProNoInternetReason = reason;
            config.wifiProNoHandoverNetwork = noHandover;
            updateWifiConfig(config);
            return;
        }
        Log.e(TAG, "WifiConfig == null, updateWifiConfig fail");
    }

    public void updateWifiEvaluateConfig(WifiConfiguration config, int accessType, int qosLevel, int qosScore) {
        if (config != null) {
            config.internetAccessType = accessType;
            config.networkQosLevel = qosLevel;
            config.networkQosScore = qosScore;
            if (accessType == 2 || accessType == 3) {
                config.wifiProNoInternetAccess = true;
            } else {
                config.wifiProNoInternetAccess = false;
            }
            updateWifiConfig(config);
            return;
        }
        Log.w(TAG, "WifiConfig == null, updateWifiConfig fail");
    }

    public void updateWifiEvaluateConfig(WifiConfiguration wificonfig, int attribute, int value, String ssid) {
        if (wificonfig != null) {
            boolean updateSucceed = false;
            if (attribute == 1) {
                if (wificonfig.internetAccessType != value) {
                    wificonfig.internetAccessType = value;
                    wificonfig.networkQosLevel = 0;
                    Log.i(TAG, StringUtilEx.safeDisplaySsid(ssid) + ", internetAccessType  updateWifiConfig Succeed");
                }
                updateSucceed = true;
                if (value == 2 || value == 3) {
                    wificonfig.wifiProNoInternetAccess = true;
                } else {
                    wificonfig.wifiProNoInternetAccess = false;
                }
            } else if (attribute != 2) {
                if (attribute == 3 && wificonfig.networkQosScore != value) {
                    wificonfig.networkQosScore = value;
                    updateSucceed = true;
                    Log.d(TAG, StringUtilEx.safeDisplaySsid(ssid) + "networkQosScore  updateWifiConfig Succeed");
                }
            } else if (wificonfig.networkQosLevel != value) {
                wificonfig.networkQosLevel = value;
                updateSucceed = true;
                Log.i(TAG, StringUtilEx.safeDisplaySsid(ssid) + ", networkQosLevel  updateWifiConfig Succeed");
            }
            if (updateSucceed) {
                updateWifiConfig(wificonfig);
                return;
            }
            return;
        }
        Log.w(TAG, "WifiConfig == null, updateWifiConfig fail");
    }

    public void resetTempCreatedConfig(WifiConfiguration wificonfig) {
        if (wificonfig != null) {
            wificonfig.isTempCreated = false;
            updateWifiConfig(wificonfig);
        }
    }

    public void cleanWifiProConfig() {
        List<WifiConfiguration> configNetworks = WifiproUtils.getAllConfiguredNetworks();
        if (configNetworks != null && !configNetworks.isEmpty()) {
            for (WifiConfiguration config : configNetworks) {
                config.wifiProNoInternetAccess = false;
                config.wifiProNoInternetReason = 0;
                config.wifiProNoHandoverNetwork = false;
                config.internetAccessType = 0;
                config.networkQosLevel = 0;
                config.networkQosScore = 0;
                updateWifiConfig(config);
            }
        }
    }

    public void cleanWifiProConfig(WifiConfiguration config) {
        if (config != null) {
            config.wifiProNoInternetAccess = false;
            config.wifiProNoInternetReason = 0;
            config.wifiProNoHandoverNetwork = false;
            config.internetAccessType = 0;
            config.networkQosLevel = 0;
            config.networkQosScore = 0;
            updateWifiConfig(config);
        }
    }

    public static ScanResult updateScanDetailByWifiPro(ScanResult sc) {
        WiFiProScoreInfo wiFiProScoreInfo;
        String ssid = "\"" + sc.SSID + "\"";
        if (WiFiProEvaluateController.isEvaluateRecordsEmpty() || (wiFiProScoreInfo = WiFiProEvaluateController.getCurrentWiFiProScore(ssid)) == null) {
            sc.internetAccessType = 0;
            sc.networkQosLevel = 0;
            sc.networkSecurity = -1;
            sc.networkQosScore = 0;
            return sc;
        } else if (wiFiProScoreInfo.internetAccessType != 1 || (wiFiProScoreInfo.failCounter >= 2 && HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(sc.frequency, sc.level) >= 2)) {
            sc.internetAccessType = wiFiProScoreInfo.internetAccessType;
            sc.networkQosLevel = wiFiProScoreInfo.networkQosLevel;
            sc.networkSecurity = wiFiProScoreInfo.networkSecurity;
            sc.networkQosScore = wiFiProScoreInfo.networkQosScore;
            return sc;
        } else {
            sc.internetAccessType = 0;
            sc.networkQosLevel = 0;
            return sc;
        }
    }
}
