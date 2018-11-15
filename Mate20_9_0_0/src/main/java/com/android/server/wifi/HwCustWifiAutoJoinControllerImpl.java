package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.util.Log;

public class HwCustWifiAutoJoinControllerImpl extends HwCustWifiAutoJoinController {
    private static final boolean HWDBG;
    public static final int MAX_ORDER = 100;
    public static final int MIN_ORDER = -100;
    static final String TAG = "HwCustWifiAutoJoinCtl";

    static {
        boolean z = (Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3))) ? true : HWDBG;
        HWDBG = z;
    }

    public boolean isWifiAutoJoinPriority(Context mContext) {
        return "true".equals(Global.getString(mContext.getContentResolver(), "wifi_autojoin_prio"));
    }

    public WifiConfiguration attemptAutoJoinCust(WifiConfiguration candidate, WifiConfiguration config) {
        int order = compareWifiConfigurationsKeyMgt(candidate, config);
        if (HWDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("attemptAutoJoinCust order=");
            stringBuilder.append(order);
            Log.d(str, stringBuilder.toString());
        }
        if (order > 0) {
            return config;
        }
        return candidate;
    }

    public int compareWifiConfigurationsKeyMgt(WifiConfiguration a, WifiConfiguration b) {
        int scoreA = getWifiKeyMgmtScore(a);
        int scoreB = getWifiKeyMgmtScore(b);
        if (HWDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("compareWifiConfigurationsKeyMgt scoreA=");
            stringBuilder.append(scoreA);
            stringBuilder.append(", scoreB=");
            stringBuilder.append(scoreB);
            Log.d(str, stringBuilder.toString());
        }
        if (scoreB - scoreA > 0) {
            return 100;
        }
        return -100;
    }

    private int getWifiKeyMgmtScore(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(4) || config.allowedKeyManagement.get(1)) {
            return 3;
        }
        if (config.allowedKeyManagement.get(3) || config.allowedKeyManagement.get(2)) {
            return 2;
        }
        if (config.allowedKeyManagement.get(0)) {
            return 1;
        }
        return 0;
    }

    public boolean isDeleteReenableAutoJoin() {
        return SystemProperties.getBoolean("ro.config.delete_re-autojoin", HWDBG);
    }
}
