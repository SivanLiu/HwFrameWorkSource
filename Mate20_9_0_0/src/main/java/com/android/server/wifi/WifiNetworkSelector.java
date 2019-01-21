package com.android.server.wifi;

import android.content.Context;
import android.net.NetworkKey;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.net.wifi.WifiInfo;
import android.provider.Settings.Global;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.hotspot2.PasspointNetworkEvaluator;
import com.android.server.wifi.util.PasspointUtil;
import com.android.server.wifi.util.ScanResultUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class WifiNetworkSelector {
    private static final long CONNECT_CHOICE_INVALID = 0;
    private static final long CONNECT_CHOICE_TIMEOUT = 50000;
    public static final int EVALUATOR_MIN_PRIORITY = 6;
    private static final long INVALID_TIME_STAMP = Long.MIN_VALUE;
    public static final int MAX_NUM_EVALUATORS = 6;
    @VisibleForTesting
    public static final int MINIMUM_NETWORK_SELECTION_INTERVAL_MS = 10000;
    private static final String TAG = "WifiNetworkSelector";
    private final Clock mClock;
    private volatile List<Pair<ScanDetail, WifiConfiguration>> mConnectableNetworks = new ArrayList();
    public WifiConnectivityHelper mConnectivityHelper;
    private Context mContext;
    public String mCurrentScanKeys = "";
    private final boolean mEnableAutoJoinWhenAssociated;
    private final NetworkEvaluator[] mEvaluators = new NetworkEvaluator[6];
    private List<ScanDetail> mFilteredNetworks = new ArrayList();
    private long mLastNetworkSelectionTimeStamp = INVALID_TIME_STAMP;
    private final LocalLog mLocalLog;
    private final ScoringParams mScoringParams;
    private final int mStayOnNetworkMinimumRxRate;
    private final int mStayOnNetworkMinimumTxRate;
    private final WifiConfigManager mWifiConfigManager;

    public interface NetworkEvaluator {
        WifiConfiguration evaluateNetworks(List<ScanDetail> list, WifiConfiguration wifiConfiguration, String str, boolean z, boolean z2, List<Pair<ScanDetail, WifiConfiguration>> list2);

        String getName();

        void update(List<ScanDetail> list);
    }

    private void localLog(String log) {
        this.mLocalLog.log(log);
    }

    private boolean isCurrentNetworkSufficient(WifiInfo wifiInfo, List<ScanDetail> scanDetails) {
        WifiConfiguration network = this.mWifiConfigManager.getConfiguredNetwork(wifiInfo.getNetworkId());
        if (network == null) {
            localLog("No current connected network.");
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Current connected network: ");
        stringBuilder.append(network.SSID);
        stringBuilder.append(" , ID: ");
        stringBuilder.append(network.networkId);
        localLog(stringBuilder.toString());
        int currentRssi = wifiInfo.getRssi();
        boolean hasQualifiedRssi = currentRssi > this.mScoringParams.getSufficientRssi(wifiInfo.getFrequency());
        boolean hasActiveStream = wifiInfo.txSuccessRate > ((double) this.mStayOnNetworkMinimumTxRate) || wifiInfo.rxSuccessRate > ((double) this.mStayOnNetworkMinimumRxRate);
        StringBuilder stringBuilder2;
        if (hasQualifiedRssi && hasActiveStream) {
            localLog("Stay on current network because of good RSSI and ongoing traffic");
            return true;
        } else if (network.ephemeral) {
            localLog("Current network is an ephemeral one.");
            return false;
        } else if (WifiConfigurationUtil.isConfigForOpenNetwork(network)) {
            localLog("Current network is a open one.");
            return false;
        } else if (wifiInfo.is24GHz() && is5GHzNetworkAvailable(scanDetails)) {
            localLog("Current network is 2.4GHz. 5GHz networks available.");
            return false;
        } else if (!hasQualifiedRssi) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Current network RSSI[");
            stringBuilder2.append(currentRssi);
            stringBuilder2.append("]-acceptable but not qualified.");
            localLog(stringBuilder2.toString());
            return false;
        } else if (network.numNoInternetAccessReports <= 0 || network.noInternetAccessExpected) {
            return true;
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Current network has [");
            stringBuilder2.append(network.numNoInternetAccessReports);
            stringBuilder2.append("] no-internet access reports.");
            localLog(stringBuilder2.toString());
            return false;
        }
    }

    private boolean is5GHzNetworkAvailable(List<ScanDetail> scanDetails) {
        for (ScanDetail detail : scanDetails) {
            if (detail.getScanResult().is5GHz()) {
                return true;
            }
        }
        return false;
    }

    private boolean isNetworkSelectionNeeded(List<ScanDetail> scanDetails, WifiInfo wifiInfo, boolean connected, boolean disconnected, String keys) {
        if (scanDetails.size() == 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(keys);
            stringBuilder.append("Empty connectivity scan results. Skip network selection.");
            localLog(stringBuilder.toString());
            return false;
        } else if (connected) {
            if (this.mContext != null && "true".equals(Global.getString(this.mContext.getContentResolver(), "hw_wifipro_enable"))) {
                return false;
            }
            if (this.mLastNetworkSelectionTimeStamp != INVALID_TIME_STAMP) {
                if (this.mClock.getElapsedSinceBootMillis() - this.mLastNetworkSelectionTimeStamp < 10000) {
                    localLog(keys, "48", "Too short since last network selection: %s ms.  Skip network selection.", Long.valueOf(this.mClock.getElapsedSinceBootMillis() - this.mLastNetworkSelectionTimeStamp));
                    return false;
                }
            }
            if (isCurrentNetworkSufficient(wifiInfo, scanDetails)) {
                localLog(keys, "49", "Current connected network already sufficient. Skip network selection.");
                return false;
            }
            localLog("Current connected network is not sufficient.");
            return true;
        } else if (disconnected) {
            return true;
        } else {
            localLog(keys, "50", "WifiStateMachine is in neither CONNECTED nor DISCONNECTED state. Skip network selection.");
            return false;
        }
    }

    public static String toScanId(ScanResult scanResult) {
        if (scanResult == null) {
            return "NULL";
        }
        return String.format("%s:%s", new Object[]{scanResult.SSID, ScanResultUtil.getConfusedBssid(scanResult.BSSID)});
    }

    public static String toNetworkString(WifiConfiguration network) {
        if (network == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(network.SSID);
        stringBuilder.append(":");
        stringBuilder.append(network.networkId);
        return stringBuilder.toString();
    }

    public boolean isSignalTooWeak(ScanResult scanResult) {
        return scanResult.level < this.mScoringParams.getEntryRssi(scanResult.frequency);
    }

    private List<ScanDetail> filterScanResults(List<ScanDetail> scanDetails, HashSet<String> bssidBlacklist, boolean isConnected, String currentBssid, String keys) {
        String str = currentBssid;
        String str2 = keys;
        localLog(str2, "51", "filterScanResults begin");
        ArrayList<NetworkKey> unscoredNetworks = new ArrayList();
        List<ScanDetail> validScanDetails = new ArrayList();
        StringBuffer noValidSsid = new StringBuffer();
        StringBuffer blacklistedBssid = new StringBuffer();
        StringBuffer lowRssi = new StringBuffer();
        boolean scanResultsHaveCurrentBssid = false;
        for (ScanDetail scanDetail : scanDetails) {
            ScanResult scanResult = scanDetail.getScanResult();
            if (TextUtils.isEmpty(scanResult.SSID)) {
                noValidSsid.append(ScanResultUtil.getConfusedBssid(scanResult.BSSID));
                noValidSsid.append(" / ");
            } else {
                if (scanResult.BSSID.equals(str)) {
                    scanResultsHaveCurrentBssid = true;
                }
                String scanId = toScanId(scanResult);
                if (bssidBlacklist.contains(scanResult.BSSID)) {
                    blacklistedBssid.append(scanId);
                    blacklistedBssid.append(" / ");
                }
                if (isSignalTooWeak(scanResult)) {
                    lowRssi.append(scanId);
                    lowRssi.append("(");
                    lowRssi.append(scanResult.is24GHz() ? "2.4GHz" : "5GHz");
                    lowRssi.append(")");
                    lowRssi.append(scanResult.level);
                    lowRssi.append(" / ");
                } else {
                    validScanDetails.add(scanDetail);
                }
            }
        }
        HashSet<String> hashSet = bssidBlacklist;
        if (!isConnected || scanResultsHaveCurrentBssid) {
            if (noValidSsid.length() != 0) {
                localLog(str2, "53", "Networks filtered out due to invalid SSID: %s", noValidSsid);
            }
            if (blacklistedBssid.length() != 0) {
                localLog(str2, "54", "Networks filtered out due to blacklist: %s", blacklistedBssid);
            }
            if (lowRssi.length() != 0) {
                localLog(str2, "55", "Networks filtered out due to low signal strength: %s", lowRssi);
            }
            return validScanDetails;
        }
        localLog(str2, "52", "Current connected BSSID %s is not in the scan results. Skip network selection.", str);
        validScanDetails.clear();
        return validScanDetails;
    }

    public List<ScanDetail> getFilteredScanDetailsForOpenUnsavedNetworks() {
        List<ScanDetail> openUnsavedNetworks = new ArrayList();
        for (ScanDetail scanDetail : this.mFilteredNetworks) {
            if (ScanResultUtil.isScanResultForOpenNetwork(scanDetail.getScanResult())) {
                if (this.mWifiConfigManager.getConfiguredNetworkForScanDetailAndCache(scanDetail) == null) {
                    openUnsavedNetworks.add(scanDetail);
                }
            }
        }
        return openUnsavedNetworks;
    }

    public List<ScanDetail> getFilteredScanDetailsForCarrierUnsavedNetworks(CarrierNetworkConfig carrierConfig) {
        List<ScanDetail> carrierUnsavedNetworks = new ArrayList();
        for (ScanDetail scanDetail : this.mFilteredNetworks) {
            ScanResult scanResult = scanDetail.getScanResult();
            if (ScanResultUtil.isScanResultForEapNetwork(scanResult)) {
                if (carrierConfig.isCarrierNetwork(scanResult.SSID)) {
                    if (this.mWifiConfigManager.getConfiguredNetworkForScanDetailAndCache(scanDetail) == null) {
                        carrierUnsavedNetworks.add(scanDetail);
                    }
                }
            }
        }
        return carrierUnsavedNetworks;
    }

    public List<Pair<ScanDetail, WifiConfiguration>> getConnectableScanDetails() {
        return this.mConnectableNetworks;
    }

    public boolean setUserConnectChoice(int netId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("userSelectNetwork: network ID=");
        stringBuilder.append(netId);
        localLog(stringBuilder.toString());
        WifiConfiguration selected = this.mWifiConfigManager.getConfiguredNetwork(netId);
        if (selected == null || selected.SSID == null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("userSelectNetwork: Invalid configuration with nid=");
            stringBuilder2.append(netId);
            localLog(stringBuilder2.toString());
            return false;
        }
        if (!selected.getNetworkSelectionStatus().isNetworkEnabled()) {
            this.mWifiConfigManager.updateNetworkSelectionStatus(netId, 0);
        }
        boolean change = false;
        String key = selected.configKey();
        long currentTime = this.mClock.getWallClockMillis();
        for (WifiConfiguration network : this.mWifiConfigManager.getSavedNetworks()) {
            NetworkSelectionStatus status = network.getNetworkSelectionStatus();
            StringBuilder stringBuilder3;
            if (network.networkId == selected.networkId) {
                if (status.getConnectChoice() != null) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Remove user selection preference of ");
                    stringBuilder3.append(status.getConnectChoice());
                    stringBuilder3.append(" Set Time: ");
                    stringBuilder3.append(status.getConnectChoiceTimestamp());
                    stringBuilder3.append(" from ");
                    stringBuilder3.append(network.SSID);
                    stringBuilder3.append(" : ");
                    stringBuilder3.append(network.networkId);
                    localLog(stringBuilder3.toString());
                    this.mWifiConfigManager.clearNetworkConnectChoice(network.networkId);
                    change = true;
                }
            } else if (status.getSeenInLastQualifiedNetworkSelection()) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Add key: ");
                stringBuilder3.append(key);
                stringBuilder3.append(" Set Time: ");
                stringBuilder3.append(currentTime);
                stringBuilder3.append(" to ");
                stringBuilder3.append(toNetworkString(network));
                localLog(stringBuilder3.toString());
                this.mWifiConfigManager.setNetworkConnectChoice(network.networkId, key, currentTime);
                change = true;
            }
        }
        return change;
    }

    private WifiConfiguration overrideCandidateWithUserConnectChoice(WifiConfiguration candidate) {
        WifiConfiguration tempConfig = candidate;
        WifiConfiguration originalCandidate = candidate;
        ScanResult scanResultCandidate = candidate.getNetworkSelectionStatus().getCandidate();
        while (tempConfig.getNetworkSelectionStatus().getConnectChoice() != null) {
            long choiceSetToGet = this.mClock.getWallClockMillis() - tempConfig.getNetworkSelectionStatus().getConnectChoiceTimestamp();
            if (choiceSetToGet < CONNECT_CHOICE_INVALID || choiceSetToGet > CONNECT_CHOICE_TIMEOUT) {
                break;
            }
            String key = tempConfig.getNetworkSelectionStatus().getConnectChoice();
            tempConfig = this.mWifiConfigManager.getConfiguredNetwork(key);
            if (tempConfig == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Connect choice: ");
                stringBuilder.append(key);
                stringBuilder.append(" has no corresponding saved config.");
                localLog(stringBuilder.toString());
                break;
            }
            NetworkSelectionStatus tempStatus = tempConfig.getNetworkSelectionStatus();
            if (tempStatus.getCandidate() != null && tempStatus.isNetworkEnabled()) {
                scanResultCandidate = tempStatus.getCandidate();
                candidate = tempConfig;
            }
        }
        if (candidate != originalCandidate) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("After user selection adjustment, the final candidate is:");
            stringBuilder2.append(toNetworkString(candidate));
            stringBuilder2.append(" : ");
            stringBuilder2.append(scanResultCandidate.BSSID);
            localLog(stringBuilder2.toString());
        }
        return candidate;
    }

    public WifiConfiguration selectNetwork(List<ScanDetail> scanDetails, HashSet<String> bssidBlacklist, WifiInfo wifiInfo, boolean connected, boolean disconnected, boolean untrustedNetworkAllowed) {
        this.mFilteredNetworks.clear();
        this.mConnectableNetworks.clear();
        String keys = this.mCurrentScanKeys;
        if (scanDetails.size() == 0) {
            localLog("Empty connectivity scan result");
            return null;
        }
        WifiConfiguration currentNetwork = this.mWifiConfigManager.getConfiguredNetwork(wifiInfo.getNetworkId());
        String currentBssid = wifiInfo.getBSSID();
        if (isNetworkSelectionNeeded(scanDetails, wifiInfo, connected, disconnected, keys)) {
            List<ScanDetail> list;
            boolean z = false;
            for (NetworkEvaluator registeredEvaluator : this.mEvaluators) {
                if (registeredEvaluator != null) {
                    registeredEvaluator.update(scanDetails);
                } else {
                    list = scanDetails;
                }
            }
            this.mFilteredNetworks = filterScanResults(scanDetails, bssidBlacklist, connected, currentBssid, keys);
            if (this.mFilteredNetworks.size() == 0) {
                localLog(keys, "57", "after filteredScanDetails size() == 0");
                return null;
            }
            boolean z2;
            NetworkEvaluator[] networkEvaluatorArr = this.mEvaluators;
            int length = networkEvaluatorArr.length;
            WifiConfiguration savedOpenNetwork = null;
            WifiConfiguration selectedNetwork = null;
            int selectedNetwork2 = 0;
            while (selectedNetwork2 < length) {
                NetworkEvaluator registeredEvaluator2 = networkEvaluatorArr[selectedNetwork2];
                if (registeredEvaluator2 != null) {
                    if (this.mConnectivityHelper != null) {
                        this.mConnectivityHelper.mCurrentScanKeys = keys;
                    }
                    if ((registeredEvaluator2 instanceof PasspointNetworkEvaluator) && !PasspointUtil.ishs2Enabled(this.mContext)) {
                        Log.w(TAG, "Passpoint is disabled.");
                    } else if (!(registeredEvaluator2 instanceof PasspointNetworkEvaluator) || PasspointUtil.ishs20EanbledBySim(this.mContext)) {
                        z2 = z;
                        selectedNetwork = registeredEvaluator2.evaluateNetworks(new ArrayList(this.mFilteredNetworks), currentNetwork, currentBssid, connected, untrustedNetworkAllowed, this.mConnectableNetworks);
                        if (selectedNetwork != null && WifiConfigurationUtil.isConfigForOpenNetwork(selectedNetwork) && PasspointUtil.ishs2Enabled(this.mContext) && (registeredEvaluator2 instanceof SavedNetworkEvaluator)) {
                            savedOpenNetwork = selectedNetwork;
                            selectedNetwork2++;
                            list = scanDetails;
                            z = z2;
                        } else if (selectedNetwork != null) {
                            localLog(keys, "58", "after %s evaluateNetworks, get wifi %s", registeredEvaluator2.getClass().getName(), selectedNetwork.SSID);
                            break;
                        } else {
                            selectedNetwork2++;
                            list = scanDetails;
                            z = z2;
                        }
                    } else {
                        Log.w(TAG, "Passpoint should be disabled as sim absent or not match.");
                    }
                }
                z2 = z;
                selectedNetwork2++;
                list = scanDetails;
                z = z2;
            }
            z2 = z;
            if ((selectedNetwork == null || !selectedNetwork.isPasspoint()) && savedOpenNetwork != null) {
                selectedNetwork = savedOpenNetwork;
            }
            boolean cloudSecurityCheckOn = Global.getInt(this.mContext.getContentResolver(), "wifi_cloud_security_check", z2) == 1 ? true : z2;
            if (!(selectedNetwork == null || selectedNetwork.cloudSecurityCheck == 0 || !cloudSecurityCheckOn)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SSID = ");
                stringBuilder.append(selectedNetwork.SSID);
                stringBuilder.append(",cloudSecurityCheck = ");
                stringBuilder.append(selectedNetwork.cloudSecurityCheck);
                stringBuilder.append(", don`t attemptAutoJoin.");
                Log.w("WifiScanLog", stringBuilder.toString());
                selectedNetwork = null;
            }
            if (selectedNetwork != null) {
                selectedNetwork = overrideCandidateWithUserConnectChoice(selectedNetwork);
                localLog(keys, "59", "after overrideCandidateWithUserConnectChoice, get wifi %s", selectedNetwork.SSID);
                this.mLastNetworkSelectionTimeStamp = this.mClock.getElapsedSinceBootMillis();
                if (HwWifiServiceFactory.getHwWifiDevicePolicy().isWifiRestricted(selectedNetwork, z2)) {
                    Log.w(TAG, "selectNetwork: MDM deny connect to restricted network!");
                    selectedNetwork = null;
                }
            }
            return selectedNetwork;
        }
        localLog(keys, "56", "isNetworkSelectionNeeded return false!");
        return null;
    }

    public boolean registerNetworkEvaluator(NetworkEvaluator evaluator, int priority) {
        StringBuilder stringBuilder;
        if (priority < 0 || priority >= 6) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid network evaluator priority: ");
            stringBuilder.append(priority);
            localLog(stringBuilder.toString());
            return false;
        } else if (this.mEvaluators[priority] != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Priority ");
            stringBuilder.append(priority);
            stringBuilder.append(" is already registered by ");
            stringBuilder.append(this.mEvaluators[priority].getName());
            localLog(stringBuilder.toString());
            return false;
        } else {
            this.mEvaluators[priority] = evaluator;
            return true;
        }
    }

    WifiNetworkSelector(Context context, ScoringParams scoringParams, WifiConfigManager configManager, Clock clock, LocalLog localLog) {
        this.mWifiConfigManager = configManager;
        this.mClock = clock;
        this.mScoringParams = scoringParams;
        this.mLocalLog = localLog;
        this.mContext = context;
        this.mEnableAutoJoinWhenAssociated = context.getResources().getBoolean(17957081);
        this.mStayOnNetworkMinimumTxRate = context.getResources().getInteger(17694906);
        this.mStayOnNetworkMinimumRxRate = context.getResources().getInteger(17694905);
    }

    void localLog(String scanKey, String eventKey, String log) {
        localLog(scanKey, eventKey, log, null);
    }

    void localLog(String scanKey, String eventKey, String log, Object... params) {
        WifiConnectivityHelper.localLog(this.mLocalLog, scanKey, eventKey, log, params);
    }
}
