package com.android.server.wifi;

import android.net.wifi.WifiInfo;
import android.util.wifi.HwHiLog;

public class HwWifiScoreReportEx implements IHwWifiScoreReportEx {
    private static final int LOW_SCORE_COUNT_MAX = 10;
    private static final String TAG = "HwWifiScoreReportEx";
    private static final int WIFI_SCORE_BAD = 58;
    private static final int WIFI_SCORE_GOOD = 60;
    private static HwWifiScoreReportEx mHwWifiScoreReportEx;
    private int lowScoreCount = 0;
    private int mScore = 0;

    public void setLowScoreCount(int lowScoreCount2) {
        this.lowScoreCount = lowScoreCount2;
    }

    public static HwWifiScoreReportEx getDefault() {
        if (mHwWifiScoreReportEx == null) {
            mHwWifiScoreReportEx = new HwWifiScoreReportEx();
        }
        return mHwWifiScoreReportEx;
    }

    public boolean isScoreCalculated(WifiInfo wifiInfo, int score) {
        int rawScore = WifiInjector.getInstance().getClientModeImpl().resetScoreByInetAccess(score == 60 ? 60 : 58);
        boolean wifiConnectivityManagerEnabled = WifiInjector.getInstance().getClientModeImpl().isWifiConnectivityManagerEnabled();
        HwHiLog.i(TAG, false, "Score = %{public}d, wifiConnectivityManagerEnabled = %{public}s, lowScoreCount = %{public}d", new Object[]{Integer.valueOf(rawScore), String.valueOf(wifiConnectivityManagerEnabled), Integer.valueOf(this.lowScoreCount)});
        if (!WifiInjector.getInstance().getClientModeImpl().isWifiInObtainingIpState() || !(rawScore == 60 || rawScore == 58)) {
            if (rawScore == 60) {
                if (!wifiConnectivityManagerEnabled) {
                    this.lowScoreCount = 0;
                    if (rawScore == wifiInfo.score) {
                        return false;
                    }
                }
            } else if (rawScore != 58) {
                HwHiLog.i(TAG, false, "current wifi is verifying poor link", new Object[0]);
            } else if (wifiConnectivityManagerEnabled && rawScore == wifiInfo.score) {
                return false;
            } else {
                int i = this.lowScoreCount;
                this.lowScoreCount = i + 1;
                if (i < 10) {
                    return false;
                }
            }
            this.lowScoreCount = 0;
            this.mScore = rawScore;
            return true;
        }
        HwHiLog.i(TAG, false, "do not modify the WiFi Score during selfcure when the state is OBTAINING_IPADDR", new Object[0]);
        return false;
    }

    public int getScore() {
        return this.mScore;
    }
}
