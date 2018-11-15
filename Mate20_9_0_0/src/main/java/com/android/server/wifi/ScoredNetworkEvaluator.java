package com.android.server.wifi;

import android.content.Context;
import android.database.ContentObserver;
import android.net.NetworkKey;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiNetworkScoreCache;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Global;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.WifiNetworkSelector.NetworkEvaluator;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import java.util.ArrayList;
import java.util.List;

public class ScoredNetworkEvaluator implements NetworkEvaluator {
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String TAG = "ScoredNetworkEvaluator";
    private final ContentObserver mContentObserver;
    private final LocalLog mLocalLog;
    private boolean mNetworkRecommendationsEnabled;
    private final NetworkScoreManager mNetworkScoreManager;
    private WifiNetworkScoreCache mScoreCache;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiPermissionsUtil mWifiPermissionsUtil;

    class ScoreTracker {
        private static final int EXTERNAL_SCORED_NONE = 0;
        private static final int EXTERNAL_SCORED_SAVED_NETWORK = 1;
        private static final int EXTERNAL_SCORED_UNTRUSTED_NETWORK = 2;
        private int mBestCandidateType = 0;
        private WifiConfiguration mEphemeralConfig;
        private int mHighScore = -128;
        private WifiConfiguration mSavedConfig;
        private ScanResult mScanResultCandidate;

        ScoreTracker() {
        }

        private Integer getNetworkScore(ScanResult scanResult, boolean isCurrentNetwork) {
            if (!ScoredNetworkEvaluator.this.mScoreCache.isScoredNetwork(scanResult)) {
                return null;
            }
            int score = ScoredNetworkEvaluator.this.mScoreCache.getNetworkScore(scanResult, isCurrentNetwork);
            if (ScoredNetworkEvaluator.DEBUG) {
                LocalLog access$300 = ScoredNetworkEvaluator.this.mLocalLog;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(WifiNetworkSelector.toScanId(scanResult));
                stringBuilder.append(" has score: ");
                stringBuilder.append(score);
                stringBuilder.append(" isCurrentNetwork network: ");
                stringBuilder.append(isCurrentNetwork);
                access$300.log(stringBuilder.toString());
            }
            return Integer.valueOf(score);
        }

        void trackUntrustedCandidate(ScanResult scanResult) {
            Integer score = getNetworkScore(scanResult, null);
            if (score != null && score.intValue() > this.mHighScore) {
                this.mHighScore = score.intValue();
                this.mScanResultCandidate = scanResult;
                this.mBestCandidateType = 2;
                ScoredNetworkEvaluator scoredNetworkEvaluator = ScoredNetworkEvaluator.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(WifiNetworkSelector.toScanId(scanResult));
                stringBuilder.append(" becomes the new untrusted candidate.");
                scoredNetworkEvaluator.debugLog(stringBuilder.toString());
            }
        }

        void trackUntrustedCandidate(ScanResult scanResult, WifiConfiguration config, boolean isCurrentNetwork) {
            Integer score = getNetworkScore(scanResult, isCurrentNetwork);
            if (score != null && score.intValue() > this.mHighScore) {
                this.mHighScore = score.intValue();
                this.mScanResultCandidate = scanResult;
                this.mBestCandidateType = 2;
                this.mEphemeralConfig = config;
                ScoredNetworkEvaluator.this.mWifiConfigManager.setNetworkCandidateScanResult(config.networkId, scanResult, 0);
                ScoredNetworkEvaluator scoredNetworkEvaluator = ScoredNetworkEvaluator.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(WifiNetworkSelector.toScanId(scanResult));
                stringBuilder.append(" becomes the new untrusted candidate.");
                scoredNetworkEvaluator.debugLog(stringBuilder.toString());
            }
        }

        void trackExternallyScoredCandidate(ScanResult scanResult, WifiConfiguration config, boolean isCurrentNetwork) {
            Integer score = getNetworkScore(scanResult, isCurrentNetwork);
            if (score == null) {
                return;
            }
            if (score.intValue() > this.mHighScore || (this.mBestCandidateType == 2 && score.intValue() == this.mHighScore)) {
                this.mHighScore = score.intValue();
                this.mSavedConfig = config;
                this.mScanResultCandidate = scanResult;
                this.mBestCandidateType = 1;
                ScoredNetworkEvaluator.this.mWifiConfigManager.setNetworkCandidateScanResult(config.networkId, scanResult, 0);
                ScoredNetworkEvaluator scoredNetworkEvaluator = ScoredNetworkEvaluator.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(WifiNetworkSelector.toScanId(scanResult));
                stringBuilder.append(" becomes the new externally scored saved network candidate.");
                scoredNetworkEvaluator.debugLog(stringBuilder.toString());
            }
        }

        WifiConfiguration getCandidateConfiguration() {
            int candidateNetworkId = -1;
            switch (this.mBestCandidateType) {
                case 1:
                    candidateNetworkId = this.mSavedConfig.networkId;
                    ScoredNetworkEvaluator.this.mLocalLog.log(String.format("new saved network candidate %s network ID:%d", new Object[]{WifiNetworkSelector.toScanId(this.mScanResultCandidate), Integer.valueOf(candidateNetworkId)}));
                    break;
                case 2:
                    if (this.mEphemeralConfig == null) {
                        this.mEphemeralConfig = ScanResultUtil.createNetworkFromScanResult(this.mScanResultCandidate);
                        this.mEphemeralConfig.ephemeral = true;
                        this.mEphemeralConfig.meteredHint = ScoredNetworkEvaluator.this.mScoreCache.getMeteredHint(this.mScanResultCandidate);
                        NetworkUpdateResult result = ScoredNetworkEvaluator.this.mWifiConfigManager.addOrUpdateNetwork(this.mEphemeralConfig, 1010);
                        if (result.isSuccess()) {
                            if (!ScoredNetworkEvaluator.this.mWifiConfigManager.updateNetworkSelectionStatus(result.getNetworkId(), 0)) {
                                ScoredNetworkEvaluator.this.mLocalLog.log("Failed to make ephemeral network selectable");
                                break;
                            }
                            candidateNetworkId = result.getNetworkId();
                            ScoredNetworkEvaluator.this.mWifiConfigManager.setNetworkCandidateScanResult(candidateNetworkId, this.mScanResultCandidate, 0);
                            ScoredNetworkEvaluator.this.mLocalLog.log(String.format("new ephemeral candidate %s network ID:%d, meteredHint=%b", new Object[]{WifiNetworkSelector.toScanId(this.mScanResultCandidate), Integer.valueOf(candidateNetworkId), Boolean.valueOf(this.mEphemeralConfig.meteredHint)}));
                            break;
                        }
                        ScoredNetworkEvaluator.this.mLocalLog.log("Failed to add ephemeral network");
                        break;
                    }
                    candidateNetworkId = this.mEphemeralConfig.networkId;
                    ScoredNetworkEvaluator.this.mLocalLog.log(String.format("existing ephemeral candidate %s network ID:%d, meteredHint=%b", new Object[]{WifiNetworkSelector.toScanId(this.mScanResultCandidate), Integer.valueOf(candidateNetworkId), Boolean.valueOf(this.mEphemeralConfig.meteredHint)}));
                    break;
                default:
                    ScoredNetworkEvaluator.this.mLocalLog.log("ScoredNetworkEvaluator did not see any good candidates.");
                    break;
            }
            return ScoredNetworkEvaluator.this.mWifiConfigManager.getConfiguredNetwork(candidateNetworkId);
        }
    }

    ScoredNetworkEvaluator(final Context context, Looper looper, final FrameworkFacade frameworkFacade, NetworkScoreManager networkScoreManager, WifiConfigManager wifiConfigManager, LocalLog localLog, WifiNetworkScoreCache wifiNetworkScoreCache, WifiPermissionsUtil wifiPermissionsUtil) {
        this.mScoreCache = wifiNetworkScoreCache;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mNetworkScoreManager = networkScoreManager;
        this.mWifiConfigManager = wifiConfigManager;
        this.mLocalLog = localLog;
        this.mContentObserver = new ContentObserver(new Handler(looper)) {
            public void onChange(boolean selfChange) {
                ScoredNetworkEvaluator scoredNetworkEvaluator = ScoredNetworkEvaluator.this;
                boolean z = true;
                if (frameworkFacade.getIntegerSetting(context, "network_recommendations_enabled", 0) != 1) {
                    z = false;
                }
                scoredNetworkEvaluator.mNetworkRecommendationsEnabled = z;
            }
        };
        frameworkFacade.registerContentObserver(context, Global.getUriFor("network_recommendations_enabled"), false, this.mContentObserver);
        this.mContentObserver.onChange(false);
        LocalLog localLog2 = this.mLocalLog;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ScoredNetworkEvaluator constructed. mNetworkRecommendationsEnabled: ");
        stringBuilder.append(this.mNetworkRecommendationsEnabled);
        localLog2.log(stringBuilder.toString());
    }

    public void update(List<ScanDetail> scanDetails) {
        if (this.mNetworkRecommendationsEnabled) {
            updateNetworkScoreCache(scanDetails);
        }
    }

    private void updateNetworkScoreCache(List<ScanDetail> scanDetails) {
        ArrayList<NetworkKey> unscoredNetworks = new ArrayList();
        for (int i = 0; i < scanDetails.size(); i++) {
            NetworkKey networkKey = NetworkKey.createFromScanResult(((ScanDetail) scanDetails.get(i)).getScanResult());
            if (networkKey != null && this.mScoreCache.getScoredNetwork(networkKey) == null) {
                unscoredNetworks.add(networkKey);
            }
        }
        if (!unscoredNetworks.isEmpty() && activeScorerAllowedtoSeeScanResults()) {
            this.mNetworkScoreManager.requestScores((NetworkKey[]) unscoredNetworks.toArray(new NetworkKey[unscoredNetworks.size()]));
        }
    }

    private boolean activeScorerAllowedtoSeeScanResults() {
        NetworkScorerAppData networkScorerAppData = this.mNetworkScoreManager.getActiveScorer();
        String packageName = this.mNetworkScoreManager.getActiveScorerPackage();
        if (networkScorerAppData == null || packageName == null) {
            return false;
        }
        try {
            this.mWifiPermissionsUtil.enforceCanAccessScanResults(packageName, networkScorerAppData.packageUid);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:39:0x00c4, code:
            if (android.text.TextUtils.equals(r17, r8.BSSID) != false) goto L_0x00ca;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public WifiConfiguration evaluateNetworks(List<ScanDetail> scanDetails, WifiConfiguration currentNetwork, String currentBssid, boolean connected, boolean untrustedNetworkAllowed, List<Pair<ScanDetail, WifiConfiguration>> connectableNetworks) {
        WifiConfiguration wifiConfiguration = currentNetwork;
        List<Pair<ScanDetail, WifiConfiguration>> list = connectableNetworks;
        if (this.mNetworkRecommendationsEnabled) {
            String str;
            ScoreTracker scoreTracker = new ScoreTracker();
            for (int i = 0; i < scanDetails.size(); i++) {
                ScanDetail scanDetail = (ScanDetail) scanDetails.get(i);
                ScanResult scanResult = scanDetail.getScanResult();
                if (scanResult != null) {
                    if (this.mWifiConfigManager.wasEphemeralNetworkDeleted(ScanResultUtil.createQuotedSSID(scanResult.SSID))) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Ignoring disabled ephemeral SSID: ");
                        stringBuilder.append(scanResult.SSID);
                        debugLog(stringBuilder.toString());
                    } else {
                        WifiConfiguration configuredNetwork = this.mWifiConfigManager.getConfiguredNetworkForScanDetailAndCache(scanDetail);
                        boolean isCurrentNetwork = true;
                        boolean untrustedScanResult = configuredNetwork == null || configuredNetwork.ephemeral;
                        if (untrustedNetworkAllowed || !untrustedScanResult) {
                            if (configuredNetwork == null) {
                                if (ScanResultUtil.isScanResultForOpenNetwork(scanResult)) {
                                    scoreTracker.trackUntrustedCandidate(scanResult);
                                }
                            } else if (configuredNetwork.ephemeral || configuredNetwork.useExternalScores) {
                                if (!configuredNetwork.getNetworkSelectionStatus().isNetworkEnabled()) {
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Ignoring disabled SSID: ");
                                    stringBuilder2.append(configuredNetwork.SSID);
                                    debugLog(stringBuilder2.toString());
                                } else if (HwWifiServiceFactory.getHwWifiDevicePolicy().isWifiRestricted(configuredNetwork, false)) {
                                    Log.w(TAG, "evaluateNetworks: MDM deny connect to restricted network!");
                                } else {
                                    if (wifiConfiguration == null || wifiConfiguration.networkId != configuredNetwork.networkId) {
                                        str = currentBssid;
                                    }
                                    isCurrentNetwork = false;
                                    if (configuredNetwork.ephemeral) {
                                        scoreTracker.trackUntrustedCandidate(scanResult, configuredNetwork, isCurrentNetwork);
                                    } else {
                                        scoreTracker.trackExternallyScoredCandidate(scanResult, configuredNetwork, isCurrentNetwork);
                                    }
                                    if (list != null) {
                                        list.add(Pair.create(scanDetail, configuredNetwork));
                                    }
                                }
                            }
                        }
                    }
                }
                str = currentBssid;
            }
            List<ScanDetail> list2 = scanDetails;
            str = currentBssid;
            return scoreTracker.getCandidateConfiguration();
        }
        this.mLocalLog.log("Skipping evaluateNetworks; Network recommendations disabled.");
        return null;
    }

    private void debugLog(String msg) {
        if (DEBUG) {
            this.mLocalLog.log(msg);
        }
    }

    public String getName() {
        return TAG;
    }
}
