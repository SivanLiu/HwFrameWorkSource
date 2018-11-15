package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.WifiNetworkSelector.NetworkEvaluator;
import com.android.server.wifi.util.ScanResultUtil;
import java.util.ArrayList;
import java.util.List;

public class PreconfiguredNetworkEvaluator implements NetworkEvaluator {
    private static final String NAME = "PreconfiguredNetworkEvaluator";
    private Context mContext;
    private WifiConfigManager mWifiConfigManager;
    private WifiEapUIManager wifiEapUIManager;

    public PreconfiguredNetworkEvaluator(Context context, WifiConfigManager wifiConfigManager) {
        this.mContext = context;
        this.mWifiConfigManager = wifiConfigManager;
        this.wifiEapUIManager = new WifiEapUIManager(context);
    }

    public String getName() {
        return NAME;
    }

    public void update(List<ScanDetail> list) {
    }

    public WifiConfiguration evaluateNetworks(List<ScanDetail> scanDetails, WifiConfiguration currentNetwork, String currentBssid, boolean connected, boolean untrustedNetworkAllowed, List<Pair<ScanDetail, WifiConfiguration>> list) {
        WifiConfiguration candidate = null;
        ScanDetail candidateScanDetail = null;
        List<WifiConfiguration> matchList = new ArrayList();
        int list_size = scanDetails.size();
        for (int i = 0; i < list_size; i++) {
            ScanDetail scanDetail = (ScanDetail) scanDetails.get(i);
            if (ScanResultUtil.isScanResultForEapNetwork(scanDetail.getScanResult())) {
                PreconfiguredNetwork matchResult = PreconfiguredNetworkManager.getInstance().match(scanDetail.getSSID());
                if (matchResult != null) {
                    WifiConfiguration wifiConfiguration = this.mWifiConfigManager.getConfiguredNetworkForScanDetail(scanDetail);
                    if (wifiConfiguration == null) {
                        Log.d(NAME, "wifiConfiguration is null.");
                        WifiConfiguration wifiConfig = new WifiConfiguration();
                        wifiConfig.allowedAuthAlgorithms.clear();
                        wifiConfig.allowedGroupCiphers.clear();
                        wifiConfig.allowedKeyManagement.clear();
                        wifiConfig.allowedPairwiseCiphers.clear();
                        wifiConfig.allowedPairwiseCiphers.clear();
                        wifiConfig.allowedProtocols.clear();
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("\"");
                        stringBuilder.append(matchResult.getSsid());
                        stringBuilder.append("\"");
                        wifiConfig.SSID = stringBuilder.toString();
                        wifiConfig.allowedKeyManagement.set(2);
                        wifiConfig.allowedKeyManagement.set(3);
                        wifiConfig.enterpriseConfig = new WifiEnterpriseConfig();
                        wifiConfig.enterpriseConfig.setEapMethod(matchResult.getEapMethod());
                        NetworkUpdateResult result = this.mWifiConfigManager.addOrUpdateNetwork(wifiConfig, HwArpVerifier.MSG_DUMP_LOG);
                        if (result.isSuccess()) {
                            this.mWifiConfigManager.enableNetwork(result.getNetworkId(), false, HwArpVerifier.MSG_DUMP_LOG);
                            wifiConfiguration = this.mWifiConfigManager.getConfiguredNetwork(result.getNetworkId());
                        } else {
                            Log.d(NAME, "Failed to add preconfigured network.");
                        }
                    }
                    this.mWifiConfigManager.setNetworkCandidateScanResult(wifiConfiguration.networkId, scanDetail.getScanResult(), 0);
                    this.mWifiConfigManager.updateScanDetailForNetwork(wifiConfiguration.networkId, scanDetail);
                    String str;
                    StringBuilder stringBuilder2;
                    if (wifiConfiguration.getNetworkSelectionStatus().isNetworkEnabled()) {
                        str = NAME;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("network is enabled : ");
                        stringBuilder2.append(wifiConfiguration);
                        Log.d(str, stringBuilder2.toString());
                        matchList.add(wifiConfiguration);
                        if (candidate == null) {
                            candidate = wifiConfiguration;
                            candidateScanDetail = scanDetail;
                        } else if (scanDetail.getScanResult().level > candidateScanDetail.getScanResult().level) {
                            candidate = wifiConfiguration;
                            candidateScanDetail = scanDetail;
                        }
                    } else {
                        str = NAME;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("network is disabled : ");
                        stringBuilder2.append(wifiConfiguration);
                        Log.d(str, stringBuilder2.toString());
                    }
                }
            }
        }
        List<ScanDetail> list2 = scanDetails;
        if (candidate == null) {
            Log.d(NAME, "Cannot find candidate.");
            return null;
        } else if (this.mWifiConfigManager.isSimPresent()) {
            return candidate;
        } else {
            int list_size2 = matchList.size();
            for (list_size = 0; list_size < list_size2; list_size++) {
                this.mWifiConfigManager.disableNetwork(((WifiConfiguration) matchList.get(list_size)).networkId, 1000);
            }
            return null;
        }
    }
}
