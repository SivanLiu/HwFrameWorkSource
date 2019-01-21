package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.internal.telephony.HuaweiTelephonyConfigs;
import com.android.server.wifi.WifiNetworkSelector.NetworkEvaluator;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.util.TelephonyUtil;
import com.android.server.wifi.util.WifiCommonUtils;
import com.android.server.wifipro.WifiProCommonUtils;
import huawei.cust.HwCustUtils;
import java.util.Iterator;
import java.util.List;

public class SavedNetworkEvaluator extends AbsSavedNetworkEvaluator implements NetworkEvaluator {
    private static final String NAME = "SavedNetworkEvaluator";
    private final int mBand5GHzAward;
    private final Clock mClock;
    private final WifiConnectivityHelper mConnectivityHelper;
    private Context mContext;
    HwCustWifiAutoJoinController mCust = ((HwCustWifiAutoJoinController) HwCustUtils.createObj(HwCustWifiAutoJoinController.class, new Object[0]));
    private final int mLastSelectionAward;
    private final LocalLog mLocalLog;
    private final int mRssiScoreOffset;
    private final int mRssiScoreSlope;
    private final int mSameBssidAward;
    private final int mSameNetworkAward;
    private final ScoringParams mScoringParams;
    private final int mSecurityAward;
    private final WifiConfigManager mWifiConfigManager;

    public SavedNetworkEvaluator(Context context, ScoringParams scoringParams, WifiConfigManager configManager, Clock clock, LocalLog localLog, WifiConnectivityHelper connectivityHelper) {
        this.mScoringParams = scoringParams;
        this.mWifiConfigManager = configManager;
        this.mClock = clock;
        this.mLocalLog = localLog;
        this.mConnectivityHelper = connectivityHelper;
        this.mContext = context;
        this.mRssiScoreSlope = context.getResources().getInteger(17694890);
        this.mRssiScoreOffset = context.getResources().getInteger(17694889);
        this.mSameBssidAward = context.getResources().getInteger(17694891);
        this.mSameNetworkAward = context.getResources().getInteger(17694901);
        this.mLastSelectionAward = context.getResources().getInteger(17694887);
        this.mSecurityAward = context.getResources().getInteger(17694892);
        this.mBand5GHzAward = context.getResources().getInteger(17694884);
    }

    private void localLog(String log) {
        this.mLocalLog.log(log);
    }

    public String getName() {
        return NAME;
    }

    private void updateSavedNetworkSelectionStatus() {
        List<WifiConfiguration> savedNetworks = this.mWifiConfigManager.getSavedNetworks();
        if (savedNetworks.size() == 0) {
            localLog("No saved networks.");
            return;
        }
        StringBuffer sbuf = new StringBuffer();
        for (WifiConfiguration network : savedNetworks) {
            if (!network.isPasspoint()) {
                if (WifiCommonUtils.doesNotWifiConnectRejectByCust(network.getNetworkSelectionStatus(), network.SSID, this.mContext)) {
                    Log.w(NAME, "updateSavedNetworkSelectionStatus: doesNotWifiConnectRejectByCust!");
                } else {
                    this.mWifiConfigManager.tryEnableNetwork(network.networkId);
                    this.mWifiConfigManager.clearNetworkCandidateScanResult(network.networkId);
                    NetworkSelectionStatus status = network.getNetworkSelectionStatus();
                    if (!status.isNetworkEnabled()) {
                        sbuf.append("  ");
                        sbuf.append(WifiNetworkSelector.toNetworkString(network));
                        sbuf.append(" ");
                        for (int index = 1; index < 17; index++) {
                            int count = status.getDisableReasonCounter(index);
                            if (count > 0) {
                                sbuf.append("reason=");
                                sbuf.append(NetworkSelectionStatus.getNetworkDisableReasonString(index));
                                sbuf.append(", count=");
                                sbuf.append(count);
                                sbuf.append("; ");
                            }
                        }
                        sbuf.append("\n");
                    }
                }
            }
        }
        if (sbuf.length() > 0) {
            localLog("Disabled saved networks:");
            localLog(sbuf.toString());
        }
    }

    public void update(List<ScanDetail> list) {
        updateSavedNetworkSelectionStatus();
    }

    private int calculateBssidScore(ScanResult scanResult, WifiConfiguration network, WifiConfiguration currentNetwork, String currentBssid, StringBuffer sbuf) {
        int rssi;
        ScanResult scanResult2 = scanResult;
        WifiConfiguration wifiConfiguration = network;
        WifiConfiguration wifiConfiguration2 = currentNetwork;
        String str = currentBssid;
        StringBuffer stringBuffer = sbuf;
        boolean is5GHz = scanResult.is5GHz();
        stringBuffer.append("[ ");
        stringBuffer.append(scanResult2.SSID);
        stringBuffer.append(" ");
        stringBuffer.append(ScanResultUtil.getConfusedBssid(scanResult2.BSSID));
        stringBuffer.append(" RSSI:");
        stringBuffer.append(scanResult2.level);
        stringBuffer.append(" ] ");
        int rssiSaturationThreshold = this.mScoringParams.getGoodRssi(scanResult2.frequency);
        if (scanResult2.level < rssiSaturationThreshold) {
            rssi = scanResult2.level;
        } else {
            rssi = rssiSaturationThreshold;
        }
        int score = 0 + ((this.mRssiScoreOffset + rssi) * this.mRssiScoreSlope);
        stringBuffer.append(" RSSI score: ");
        stringBuffer.append(score);
        stringBuffer.append(",");
        if (is5GHz) {
            score += this.mBand5GHzAward;
            stringBuffer.append(" 5GHz bonus: ");
            stringBuffer.append(this.mBand5GHzAward);
            stringBuffer.append(",");
        }
        int lastUserSelectedNetworkId = this.mWifiConfigManager.getLastSelectedNetwork();
        if (lastUserSelectedNetworkId != -1 && lastUserSelectedNetworkId == wifiConfiguration.networkId) {
            long timeDifference = this.mClock.getElapsedSinceBootMillis() - this.mWifiConfigManager.getLastSelectedTimeStamp();
            if (timeDifference > 0) {
                int bonus = this.mLastSelectionAward - ((int) ((timeDifference / 1000) / 60));
                score += bonus > 0 ? bonus : 0;
                stringBuffer.append(" User selection ");
                stringBuffer.append((timeDifference / 1000) / 60);
                stringBuffer.append(" minutes ago, bonus: ");
                stringBuffer.append(bonus);
                stringBuffer.append(",");
            }
        }
        if (wifiConfiguration2 != null && wifiConfiguration.networkId == wifiConfiguration2.networkId) {
            score += this.mSameNetworkAward;
            stringBuffer.append(" Same network bonus: ");
            stringBuffer.append(this.mSameNetworkAward);
            stringBuffer.append(",");
            if (!(!this.mConnectivityHelper.isFirmwareRoamingSupported() || str == null || str.equals(scanResult2.BSSID))) {
                score += this.mSameBssidAward;
                stringBuffer.append(" Equivalent BSSID bonus: ");
                stringBuffer.append(this.mSameBssidAward);
                stringBuffer.append(",");
            }
        }
        if (str != null && str.equals(scanResult2.BSSID)) {
            score += this.mSameBssidAward;
            stringBuffer.append(" Same BSSID bonus: ");
            stringBuffer.append(this.mSameBssidAward);
            stringBuffer.append(",");
        }
        if (!WifiConfigurationUtil.isConfigForOpenNetwork(network)) {
            score += this.mSecurityAward;
            stringBuffer.append(" Secure network bonus: ");
            stringBuffer.append(this.mSecurityAward);
            stringBuffer.append(",");
        }
        stringBuffer.append(" ## Total score: ");
        stringBuffer.append(score);
        stringBuffer.append("\n");
        return score;
    }

    /* JADX WARNING: Missing block: B:92:0x0275, code skipped:
            if (r12.level > r2.level) goto L_0x027e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public WifiConfiguration evaluateNetworks(List<ScanDetail> scanDetails, WifiConfiguration currentNetwork, String currentBssid, boolean connected, boolean untrustedNetworkAllowed, List<Pair<ScanDetail, WifiConfiguration>> connectableNetworks) {
        ScanResult scanResultCandidate;
        List<Pair<ScanDetail, WifiConfiguration>> list = connectableNetworks;
        String keys = this.mConnectivityHelper.mCurrentScanKeys;
        StringBuffer savedScan = new StringBuffer();
        StringBuffer scoreHistory = new StringBuffer();
        resetHwSelectedCandidates();
        Iterator it = scanDetails.iterator();
        int highestScore = Integer.MIN_VALUE;
        ScanResult scanResultCandidate2 = null;
        WifiConfiguration candidate = null;
        while (it.hasNext()) {
            StringBuffer savedScan2;
            Iterator it2;
            ScanDetail scanDetail = (ScanDetail) it.next();
            ScanResult scanResult = scanDetail.getScanResult();
            WifiConfiguration network = this.mWifiConfigManager.getConfiguredNetworkForScanDetailAndCache(scanDetail);
            if (network != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(scanResult.SSID);
                stringBuilder.append(" / ");
                savedScan.append(stringBuilder.toString());
                if (this.mCust == null || !this.mCust.isWifiAutoJoinPriority(this.mContext)) {
                    int i;
                    int network2;
                    if (network.isPasspoint()) {
                        i = 1;
                        savedScan2 = savedScan;
                        it2 = it;
                        scanResultCandidate = scanResultCandidate2;
                        savedScan = network;
                        network2 = 0;
                    } else if (network.isEphemeral()) {
                        i = 1;
                        savedScan2 = savedScan;
                        it2 = it;
                        scanResultCandidate = scanResultCandidate2;
                        savedScan = network;
                        scanResultCandidate2 = scanResult;
                        network2 = 0;
                    }
                    Object[] objArr = new Object[i];
                    objArr[network2] = savedScan;
                    localLog(keys, "6", "network.isPasspoint %s", objArr);
                    scanResultCandidate2 = scanResultCandidate;
                    it = it2;
                    savedScan = savedScan2;
                } else {
                    localLog(keys, "5", "isWifiAutoJoinPriority is true, ignore passpoint");
                }
                NetworkSelectionStatus status = network.getNetworkSelectionStatus();
                if (WifiCommonUtils.doesNotWifiConnectRejectByCust(status, network.SSID, this.mContext)) {
                    Log.w(NAME, "evaluateNetworks: doesNotWifiConnectRejectByCust!");
                } else {
                    status.setSeenInLastQualifiedNetworkSelection(true);
                    NetworkSelectionStatus networkSelectionStatus;
                    if (unselectDueToFailedLastTime(scanResult, network) || unselectDiscNonLocally(scanResult, network)) {
                        savedScan2 = savedScan;
                        it2 = it;
                        scanResultCandidate = scanResultCandidate2;
                        networkSelectionStatus = status;
                        savedScan = network;
                        scanResultCandidate2 = scanResult;
                    } else if (isNetworkEnabledExtended(network, status)) {
                        if (network.BSSID == null || network.BSSID.equals("any") || network.BSSID.equals(scanResult.BSSID)) {
                            it2 = it;
                            if (!TelephonyUtil.isSimConfig(network) || this.mWifiConfigManager.isSimPresent()) {
                                if (HwWifiServiceFactory.getHwWifiDevicePolicy().isWifiRestricted(network, false)) {
                                    Log.w(NAME, "evaluateNetworks: MDM deny connect to restricted network!");
                                } else if (this.mCust != null && this.mCust.isWifiAutoJoinPriority(this.mContext)) {
                                    if (candidate == null) {
                                        localLog(keys, "8", "isWifiAutoJoinPriority ");
                                        candidate = network;
                                    }
                                    candidate = this.mCust.attemptAutoJoinCust(candidate, network);
                                    scanResultCandidate2 = scanResult;
                                    status.setCandidate(scanResult);
                                    it = it2;
                                } else if (!WifiConfigStoreUtils.isSkipAutoConnect(this.mContext, network)) {
                                    networkSelectionStatus = status;
                                    savedScan2 = savedScan;
                                    WifiConfiguration savedScan3 = network;
                                    ScanResult scanResultCandidate3 = scanResultCandidate2;
                                    scanResultCandidate2 = scanResult;
                                    int score = calculateBssidScore(scanResult, network, currentNetwork, currentBssid, scoreHistory);
                                    if (score > networkSelectionStatus.getCandidateScore() || (score == networkSelectionStatus.getCandidateScore() && networkSelectionStatus.getCandidate() != null && scanResultCandidate2.level > networkSelectionStatus.getCandidate().level)) {
                                        this.mWifiConfigManager.setNetworkCandidateScanResult(savedScan3.networkId, scanResultCandidate2, score);
                                    }
                                    StringBuilder stringBuilder2;
                                    if (savedScan3.useExternalScores) {
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Network %S has external score.");
                                        stringBuilder2.append(WifiNetworkSelector.toNetworkString(savedScan3));
                                        localLog(keys, "9", stringBuilder2.toString());
                                    } else if ((!HuaweiTelephonyConfigs.isChinaMobile() || WifiProCommonUtils.isWifiProSwitchOn(this.mContext)) && selectBestNetworkByWifiPro(savedScan3, scanResultCandidate2)) {
                                        localLog(keys, "10", "selectBestNetworkByWifiPro");
                                    } else {
                                        if (list != null) {
                                            list.add(Pair.create(scanDetail, this.mWifiConfigManager.getConfiguredNetwork(savedScan3.networkId)));
                                        }
                                        if (HuaweiTelephonyConfigs.isChinaMobile()) {
                                            WifiConfiguration configurationCandidateForThisScan = this.mWifiConfigManager.getConfiguredNetwork(savedScan3.networkId);
                                            if (configurationCandidateForThisScan != null) {
                                                WifiConfiguration candidate2;
                                                if (candidate == null) {
                                                    scanResultCandidate = scanResultCandidate2;
                                                    this.mWifiConfigManager.setNetworkCandidateScanResult(savedScan3.networkId, scanResultCandidate, highestScore);
                                                    candidate2 = this.mWifiConfigManager.getConfiguredNetwork(savedScan3.networkId);
                                                    this.mWifiConfigManager.clearNetworkConnectChoice(savedScan3.networkId);
                                                    StringBuilder stringBuilder3 = new StringBuilder();
                                                    stringBuilder3.append("CMCC candidate set to ");
                                                    stringBuilder3.append(candidate2);
                                                    localLog(stringBuilder3.toString());
                                                } else if (configurationCandidateForThisScan.priority > candidate.priority) {
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("CMCC find more higher priority,candidate set to new : ");
                                                    stringBuilder2.append(configurationCandidateForThisScan.SSID);
                                                    localLog(stringBuilder2.toString());
                                                    scanResultCandidate = scanResultCandidate2;
                                                    this.mWifiConfigManager.setNetworkCandidateScanResult(savedScan3.networkId, scanResultCandidate, highestScore);
                                                    candidate2 = this.mWifiConfigManager.getConfiguredNetwork(savedScan3.networkId);
                                                    this.mWifiConfigManager.clearNetworkConnectChoice(savedScan3.networkId);
                                                }
                                                scanResultCandidate2 = scanResultCandidate;
                                                candidate = candidate2;
                                            }
                                        } else {
                                            if (this.mCust == null || !this.mCust.isWifiAutoJoinPriority(this.mContext)) {
                                                if (score <= highestScore) {
                                                    scanResultCandidate = (score != highestScore || scanResultCandidate3 == null) ? scanResultCandidate3 : scanResultCandidate3;
                                                }
                                                int highestScore2 = score;
                                                this.mWifiConfigManager.setNetworkCandidateScanResult(savedScan3.networkId, scanResultCandidate2, highestScore2);
                                                candidate = this.mWifiConfigManager.getConfiguredNetwork(savedScan3.networkId);
                                                highestScore = highestScore2;
                                            } else {
                                                StringBuilder stringBuilder4 = new StringBuilder();
                                                stringBuilder4.append("isWifiAutoJoinPriority candidate : ");
                                                stringBuilder4.append(candidate);
                                                localLog(stringBuilder4.toString());
                                                scanResultCandidate = scanResultCandidate3;
                                            }
                                            scanResultCandidate2 = scanResultCandidate;
                                        }
                                        it = it2;
                                        savedScan = savedScan2;
                                    }
                                    scanResultCandidate = scanResultCandidate3;
                                    scanResultCandidate2 = scanResultCandidate;
                                    it = it2;
                                    savedScan = savedScan2;
                                }
                            }
                        } else {
                            String toNetworkString = WifiNetworkSelector.toNetworkString(network);
                            r2 = new Object[2];
                            it2 = it;
                            r2[0] = network.BSSID;
                            r2[1] = scanResult.BSSID;
                            localLog(keys, "Network %s has specified BSSID %, Skip ", toNetworkString, r2);
                        }
                        savedScan2 = savedScan;
                        scanResultCandidate = scanResultCandidate2;
                        scanResultCandidate2 = scanResultCandidate;
                        it = it2;
                        savedScan = savedScan2;
                    } else {
                        savedScan2 = savedScan;
                        it2 = it;
                        scanResultCandidate = scanResultCandidate2;
                        networkSelectionStatus = status;
                        savedScan = network;
                        scanResultCandidate2 = scanResult;
                    }
                    localLog(keys, "7", "status.isNetworkEnabled is false, %s", savedScan);
                    scanResultCandidate2 = scanResultCandidate;
                    it = it2;
                    savedScan = savedScan2;
                }
            }
            savedScan2 = savedScan;
            it2 = it;
            scanResultCandidate = scanResultCandidate2;
            scanResultCandidate2 = scanResultCandidate;
            it = it2;
            savedScan = savedScan2;
        }
        scanResultCandidate = scanResultCandidate2;
        if (savedScan.length() > 0) {
            localLog(keys, "11", "savedScan %s", savedScan.toString());
        }
        if (scoreHistory.length() > 0) {
            localLog(keys, "12", "scoreHistory %s", scoreHistory.toString());
        }
        if ((!HuaweiTelephonyConfigs.isChinaMobile() || WifiProCommonUtils.isWifiProSwitchOn(this.mContext)) && isWifiProEnabledOrSelfCureGoing()) {
            localLog(keys, "13", "isWifiProEnabledOrSelfCureGoing true ");
            return getLastCandidateByWifiPro(candidate, scanResultCandidate);
        }
        if (scanResultCandidate == null) {
            localLog(keys, "14", "did not see any good candidates.");
        }
        return candidate;
    }

    public boolean isNetworkEnabledExtended(WifiConfiguration config, NetworkSelectionStatus status) {
        return true;
    }

    private void localLog(String scanKey, String eventKey, String log) {
        localLog(scanKey, eventKey, log, null);
    }

    private void localLog(String scanKey, String eventKey, String log, Object... params) {
        WifiConnectivityHelper.localLog(this.mLocalLog, scanKey, eventKey, log, params);
    }
}
