package com.android.server.wifi;

import android.net.wifi.WifiScanLog;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiNative.RoamingCapabilities;
import com.android.server.wifi.WifiNative.RoamingConfig;
import java.util.ArrayList;
import java.util.Arrays;

public class WifiConnectivityHelper {
    @VisibleForTesting
    public static int INVALID_LIST_SIZE = -1;
    private static final String TAG = "WifiConnectivityHelper";
    public String mCurrentScanKeys = "";
    private boolean mFirmwareRoamingSupported = false;
    private int mMaxNumBlacklistBssid = INVALID_LIST_SIZE;
    private int mMaxNumWhitelistSsid = INVALID_LIST_SIZE;
    private final WifiNative mWifiNative;

    WifiConnectivityHelper(WifiNative wifiNative) {
        this.mWifiNative = wifiNative;
    }

    public boolean getFirmwareRoamingInfo() {
        this.mFirmwareRoamingSupported = false;
        this.mMaxNumBlacklistBssid = INVALID_LIST_SIZE;
        this.mMaxNumWhitelistSsid = INVALID_LIST_SIZE;
        int fwFeatureSet = this.mWifiNative.getSupportedFeatureSet(this.mWifiNative.getClientInterfaceName());
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Firmware supported feature set: ");
        stringBuilder.append(Integer.toHexString(fwFeatureSet));
        Log.d(str, stringBuilder.toString());
        if ((8388608 & fwFeatureSet) == 0) {
            Log.d(TAG, "Firmware roaming is not supported");
            return true;
        }
        RoamingCapabilities roamingCap = new RoamingCapabilities();
        StringBuilder stringBuilder2;
        if (!this.mWifiNative.getRoamingCapabilities(this.mWifiNative.getClientInterfaceName(), roamingCap)) {
            Log.e(TAG, "Failed to get firmware roaming capabilities");
        } else if (roamingCap.maxBlacklistSize < 0 || roamingCap.maxWhitelistSize < 0) {
            String str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Invalid firmware roaming capabilities: max num blacklist bssid=");
            stringBuilder2.append(roamingCap.maxBlacklistSize);
            stringBuilder2.append(" max num whitelist ssid=");
            stringBuilder2.append(roamingCap.maxWhitelistSize);
            Log.e(str2, stringBuilder2.toString());
        } else {
            this.mFirmwareRoamingSupported = true;
            this.mMaxNumBlacklistBssid = roamingCap.maxBlacklistSize;
            this.mMaxNumWhitelistSsid = roamingCap.maxWhitelistSize;
            String str3 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Firmware roaming supported with capabilities: max num blacklist bssid=");
            stringBuilder2.append(this.mMaxNumBlacklistBssid);
            stringBuilder2.append(" max num whitelist ssid=");
            stringBuilder2.append(this.mMaxNumWhitelistSsid);
            Log.d(str3, stringBuilder2.toString());
            return true;
        }
        return false;
    }

    public boolean isFirmwareRoamingSupported() {
        return this.mFirmwareRoamingSupported;
    }

    public int getMaxNumBlacklistBssid() {
        if (this.mFirmwareRoamingSupported) {
            return this.mMaxNumBlacklistBssid;
        }
        Log.e(TAG, "getMaxNumBlacklistBssid: Firmware roaming is not supported");
        return INVALID_LIST_SIZE;
    }

    public int getMaxNumWhitelistSsid() {
        if (this.mFirmwareRoamingSupported) {
            return this.mMaxNumWhitelistSsid;
        }
        Log.e(TAG, "getMaxNumWhitelistSsid: Firmware roaming is not supported");
        return INVALID_LIST_SIZE;
    }

    public boolean setFirmwareRoamingConfiguration(ArrayList<String> blacklistBssids, ArrayList<String> whitelistSsids) {
        if (!this.mFirmwareRoamingSupported) {
            Log.e(TAG, "Firmware roaming is not supported");
            return false;
        } else if (blacklistBssids == null || whitelistSsids == null) {
            Log.e(TAG, "Invalid firmware roaming configuration settings");
            return false;
        } else {
            int blacklistSize = blacklistBssids.size();
            int whitelistSize = whitelistSsids.size();
            if (blacklistSize > this.mMaxNumBlacklistBssid || whitelistSize > this.mMaxNumWhitelistSsid) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid BSSID blacklist size ");
                stringBuilder.append(blacklistSize);
                stringBuilder.append(" SSID whitelist size ");
                stringBuilder.append(whitelistSize);
                stringBuilder.append(". Max blacklist size: ");
                stringBuilder.append(this.mMaxNumBlacklistBssid);
                stringBuilder.append(", max whitelist size: ");
                stringBuilder.append(this.mMaxNumWhitelistSsid);
                Log.e(str, stringBuilder.toString());
                return false;
            }
            RoamingConfig roamConfig = new RoamingConfig();
            roamConfig.blacklistBssids = blacklistBssids;
            roamConfig.whitelistSsids = whitelistSsids;
            return this.mWifiNative.configureRoaming(this.mWifiNative.getClientInterfaceName(), roamConfig);
        }
    }

    public void removeNetworkIfCurrent(int networkId) {
        this.mWifiNative.removeNetworkIfCurrent(this.mWifiNative.getClientInterfaceName(), networkId);
    }

    public static void localLog(LocalLog localLog, String scanKey, String eventKey, String log) {
        localLog(localLog, scanKey, eventKey, log, null);
    }

    public static void localLog(LocalLog localLog, String scanKey, String eventKey, String log, Object... params) {
        WifiScanLog.getDefault().addEvent(scanKey, eventKey, log, params);
        String fullLog = new StringBuilder();
        fullLog.append(scanKey);
        fullLog.append(eventKey);
        fullLog.append(" ");
        fullLog = fullLog.toString();
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(fullLog);
            stringBuilder.append(String.format(log, params));
            fullLog = stringBuilder.toString();
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(fullLog);
            stringBuilder2.append(log);
            fullLog = stringBuilder2.toString();
            if (params != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(fullLog);
                stringBuilder2.append(Arrays.toString(params));
                fullLog = stringBuilder2.toString();
            }
        }
        localLog.log(fullLog);
    }
}
