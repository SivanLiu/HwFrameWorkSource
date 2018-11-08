package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;

public class LegacyConnectedScore extends ConnectedScore {
    private static final int BAD_LINKSPEED_PENALTY = 4;
    private static final int BAD_RSSI_COUNT_PENALTY = 2;
    private static final int GOOD_LINKSPEED_BONUS = 4;
    private static final int HOME_VISIBLE_NETWORK_MAX_COUNT = 6;
    private static final int LINK_STUCK_PENALTY = 2;
    private static final int MAX_BAD_RSSI_COUNT = 7;
    private static final int MAX_LOW_RSSI_COUNT = 1;
    private static final int MAX_STUCK_LINK_COUNT = 5;
    private static final int MAX_SUCCESS_RATE_OF_STUCK_LINK = 3;
    private static final int MIN_SUSTAINED_LINK_STUCK_COUNT = 1;
    private static final double MIN_TX_FAILURE_RATE_FOR_WORKING_LINK = 0.3d;
    private static final int SCAN_CACHE_COUNT_PENALTY = 2;
    private static final int SCAN_CACHE_VISIBILITY_MS = 12000;
    private static final int STARTING_SCORE = 56;
    private final int mBadLinkSpeed24;
    private final int mBadLinkSpeed5;
    private final int mGoodLinkSpeed24;
    private final int mGoodLinkSpeed5;
    private boolean mIsHomeNetwork;
    private boolean mMultiBandScanResults;
    private int mScore = 0;
    private final int mThresholdMinimumRssi24;
    private final int mThresholdMinimumRssi5;
    private final int mThresholdQualifiedRssi24;
    private final int mThresholdQualifiedRssi5;
    private final int mThresholdSaturatedRssi24;
    private final int mThresholdSaturatedRssi5;
    private boolean mVerboseLoggingEnabled = false;
    private final WifiConfigManager mWifiConfigManager;

    LegacyConnectedScore(Context context, WifiConfigManager wifiConfigManager, Clock clock) {
        super(clock);
        this.mThresholdMinimumRssi5 = context.getResources().getInteger(17694902);
        this.mThresholdQualifiedRssi5 = context.getResources().getInteger(17694910);
        this.mThresholdSaturatedRssi5 = context.getResources().getInteger(17694908);
        this.mThresholdMinimumRssi24 = context.getResources().getInteger(17694901);
        this.mThresholdQualifiedRssi24 = context.getResources().getInteger(17694909);
        this.mThresholdSaturatedRssi24 = context.getResources().getInteger(17694907);
        this.mBadLinkSpeed24 = context.getResources().getInteger(17694899);
        this.mBadLinkSpeed5 = context.getResources().getInteger(17694900);
        this.mGoodLinkSpeed24 = context.getResources().getInteger(17694905);
        this.mGoodLinkSpeed5 = context.getResources().getInteger(17694906);
        this.mWifiConfigManager = wifiConfigManager;
    }

    public void updateUsingWifiInfo(WifiInfo wifiInfo, long millis) {
        this.mMultiBandScanResults = multiBandScanResults(wifiInfo);
        this.mIsHomeNetwork = isHomeNetwork(wifiInfo);
        int rssiThreshBad = this.mThresholdMinimumRssi24;
        int rssiThreshLow = this.mThresholdQualifiedRssi24;
        if (wifiInfo.is5GHz() && (this.mMultiBandScanResults ^ 1) != 0) {
            rssiThreshBad = this.mThresholdMinimumRssi5;
            rssiThreshLow = this.mThresholdQualifiedRssi5;
        }
        int rssi = wifiInfo.getRssi();
        if (this.mIsHomeNetwork) {
            rssi += 5;
        }
        if (wifiInfo.txBadRate < 1.0d || wifiInfo.txSuccessRate >= 3.0d || rssi >= rssiThreshLow) {
            if (wifiInfo.txBadRate < MIN_TX_FAILURE_RATE_FOR_WORKING_LINK && wifiInfo.linkStuckCount > 0) {
                wifiInfo.linkStuckCount--;
            }
        } else if (wifiInfo.linkStuckCount < 5) {
            wifiInfo.linkStuckCount++;
        }
        if (rssi < rssiThreshBad) {
            if (wifiInfo.badRssiCount < 7) {
                wifiInfo.badRssiCount++;
            }
        } else if (rssi < rssiThreshLow) {
            wifiInfo.lowRssiCount = 1;
            if (wifiInfo.badRssiCount > 0) {
                wifiInfo.badRssiCount--;
            }
        } else {
            wifiInfo.badRssiCount = 0;
            wifiInfo.lowRssiCount = 0;
        }
        this.mScore = calculateScore(wifiInfo);
    }

    public void updateUsingRssi(int rssi, long millis, double standardDeviation) {
    }

    public int generateScore() {
        return this.mScore;
    }

    public void reset() {
        this.mScore = 0;
    }

    private int calculateScore(WifiInfo wifiInfo) {
        int score = 56;
        int rssiThreshSaturated = this.mThresholdSaturatedRssi24;
        int linkspeedThreshBad = this.mBadLinkSpeed24;
        int linkspeedThreshGood = this.mGoodLinkSpeed24;
        if (wifiInfo.is5GHz()) {
            if (!this.mMultiBandScanResults) {
                rssiThreshSaturated = this.mThresholdSaturatedRssi5;
            }
            linkspeedThreshBad = this.mBadLinkSpeed5;
            linkspeedThreshGood = this.mGoodLinkSpeed5;
        }
        int rssi = wifiInfo.getRssi();
        if (this.mIsHomeNetwork) {
            rssi += 5;
        }
        int linkSpeed = wifiInfo.getLinkSpeed();
        if (wifiInfo.linkStuckCount > 1) {
            score = 56 - ((wifiInfo.linkStuckCount - 1) * 2);
        }
        if (linkSpeed < linkspeedThreshBad) {
            score -= 4;
        } else if (linkSpeed >= linkspeedThreshGood && wifiInfo.txSuccessRate > 5.0d) {
            score += 4;
        }
        score -= (wifiInfo.badRssiCount * 2) + wifiInfo.lowRssiCount;
        if (rssi >= rssiThreshSaturated) {
            return score + 5;
        }
        return score;
    }

    private boolean multiBandScanResults(WifiInfo wifiInfo) {
        WifiConfiguration currentConfiguration = this.mWifiConfigManager.getConfiguredNetwork(wifiInfo.getNetworkId());
        if (currentConfiguration == null) {
            return false;
        }
        ScanDetailCache scanDetailCache = this.mWifiConfigManager.getScanDetailCacheForNetwork(wifiInfo.getNetworkId());
        if (scanDetailCache == null) {
            return false;
        }
        currentConfiguration.setVisibility(scanDetailCache.getVisibility(12000));
        if (currentConfiguration.visibility == null || currentConfiguration.visibility.rssi24 == WifiConfiguration.INVALID_RSSI || currentConfiguration.visibility.rssi5 == WifiConfiguration.INVALID_RSSI || currentConfiguration.visibility.rssi24 < currentConfiguration.visibility.rssi5 - 2) {
            return false;
        }
        return true;
    }

    private boolean isHomeNetwork(WifiInfo wifiInfo) {
        WifiConfiguration currentConfiguration = this.mWifiConfigManager.getConfiguredNetwork(wifiInfo.getNetworkId());
        if (currentConfiguration == null || currentConfiguration.allowedKeyManagement.cardinality() != 1 || !currentConfiguration.allowedKeyManagement.get(1)) {
            return false;
        }
        ScanDetailCache scanDetailCache = this.mWifiConfigManager.getScanDetailCacheForNetwork(wifiInfo.getNetworkId());
        return scanDetailCache != null && scanDetailCache.size() <= 6;
    }
}
