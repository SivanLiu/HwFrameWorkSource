package com.android.server.wifi;

import android.net.wifi.ScanResult;
import java.util.List;
import java.util.Set;

public class OpenNetworkRecommender {
    public ScanResult recommendNetwork(List<ScanDetail> networks, Set<String> blacklistedSsids) {
        ScanResult result = null;
        int highestRssi = Integer.MIN_VALUE;
        for (ScanDetail scanDetail : networks) {
            ScanResult scanResult = scanDetail.getScanResult();
            if (scanResult.level > highestRssi) {
                result = scanResult;
                highestRssi = scanResult.level;
            }
        }
        if (result == null || !blacklistedSsids.contains(result.SSID)) {
            return result;
        }
        return null;
    }
}
