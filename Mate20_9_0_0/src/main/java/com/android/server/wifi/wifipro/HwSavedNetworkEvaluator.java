package com.android.server.wifi.wifipro;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.os.UserHandle;
import android.util.LocalLog;
import android.util.Log;
import com.android.server.wifi.Clock;
import com.android.server.wifi.HwQoE.HidataWechatTraffic;
import com.android.server.wifi.HwQoE.HwQoEService;
import com.android.server.wifi.HwSelfCureEngine;
import com.android.server.wifi.SavedNetworkEvaluator;
import com.android.server.wifi.ScoringParams;
import com.android.server.wifi.WifiConfigManager;
import com.android.server.wifi.WifiConnectivityHelper;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifipro.WifiProCommonUtils;

public class HwSavedNetworkEvaluator extends SavedNetworkEvaluator {
    private static int BACKUP_UNUSED = 0;
    public static final int HANDOVER_STATUS_DISALLOWED = -4;
    public static final int HANDOVER_STATUS_OK = 0;
    private static int HAS_INET_SELECTED = 100;
    private static final String HOME_PKT_NAME = "com.huawei.android.launcher";
    private static final String HUAWEI_GUEST = "\"Huawei-Guest\"NONE";
    private static final int MIN_3_LEVEL = -75;
    private static int NORMAL_PORTAL_SELECTED = 103;
    private static int NO_INET_SELECTED = 104;
    private static int PORTAL_DISAPPEAR_THRESHOLD = 2;
    private static int RECOVERY_SELECTED = 101;
    private static final String TAG = "HwSavedNetworkEvaluator";
    private static int TRUSTED_PORTAL_SELECTED = 102;
    private int backupTypeSelected = BACKUP_UNUSED;
    private WifiConfiguration hasInetNetworkCandidate = null;
    private ScanResult hasInetScanResultCandidate = null;
    private Context mContext;
    private int mSelfCureCandidateLostCnt = 0;
    private WifiNative mWifiNative;
    private WifiStateMachine mWifiStateMachine;
    private WifiConfiguration noInetNetworkCandidate = null;
    private ScanResult noInetScanResultCandidate = null;
    private boolean portalDisappeared = true;
    private int portalDisappearedCounter = 0;
    private WifiConfiguration portalNetworkCandidate = null;
    private String portalNotifiedConfigKey = null;
    private boolean portalNotifiedHasInternet = false;
    private int portalNotifiedMaxRssi = WifiHandover.INVALID_RSSI;
    private ScanResult portalScanResultCandidate = null;
    private ScanResult portalScanResultTursted = null;
    private WifiConfiguration recoveryNetworkCandidate = null;
    private ScanResult recoveryScanResultCandidate = null;
    private WifiConfiguration selfCureNetworkCandidate = null;
    private ScanResult selfCureScanResultCandidate = null;

    public HwSavedNetworkEvaluator(Context context, ScoringParams scoringParams, WifiConfigManager configManager, Clock clock, LocalLog localLog, WifiStateMachine wsm, WifiConnectivityHelper connectivityHelper) {
        super(context, scoringParams, configManager, clock, localLog, connectivityHelper);
        this.mContext = context;
        this.mWifiStateMachine = wsm;
        this.mWifiNative = WifiInjector.getInstance().getWifiNative();
    }

    public synchronized void resetHwSelectedCandidates() {
        this.hasInetNetworkCandidate = null;
        this.hasInetScanResultCandidate = null;
        this.recoveryNetworkCandidate = null;
        this.recoveryScanResultCandidate = null;
        this.portalScanResultTursted = null;
        this.portalNetworkCandidate = null;
        this.portalScanResultCandidate = null;
        this.noInetNetworkCandidate = null;
        this.noInetScanResultCandidate = null;
        this.selfCureNetworkCandidate = null;
        this.selfCureScanResultCandidate = null;
        this.backupTypeSelected = BACKUP_UNUSED;
        this.portalDisappeared = true;
        this.portalNotifiedMaxRssi = WifiHandover.INVALID_RSSI;
    }

    public boolean isNetworkEnabledExtended(WifiConfiguration config, NetworkSelectionStatus status) {
        if (status.isNetworkEnabled() || HwAutoConnectManager.getInstance() == null || !HwAutoConnectManager.getInstance().allowAutoJoinDisabledNetworkAgain(config)) {
            return status.isNetworkEnabled();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isNetworkEnabledExtended, allowAutoJoinDisabledNetworkAgain = ");
        stringBuilder.append(config.configKey());
        LOGD(stringBuilder.toString());
        return true;
    }

    public boolean unselectDueToFailedLastTime(ScanResult scanResult, WifiConfiguration config) {
        if (!(scanResult == null || config == null || (config.lastConnFailedType != 3 && config.lastConnFailedType != 2 && config.lastConnFailedType != 4))) {
            long deltaMs = System.currentTimeMillis() - config.lastConnFailedTimestamp;
            NetworkSelectionStatus status = config.getNetworkSelectionStatus();
            int count = status.getDisableReasonCounter(config.lastConnFailedType);
            if (WifiProCommonUtils.getSignalLevel(scanResult.frequency, scanResult.level) >= 4) {
                if (config.rssiStatusDisabled != WifiHandover.INVALID_RSSI && config.rssiStatusDisabled < -75) {
                    status.setNetworkSelectionStatus(0);
                    status.setDisableTime(-1);
                    status.setNetworkSelectionDisableReason(0);
                    config.rssiStatusDisabled = WifiHandover.INVALID_RSSI;
                }
                return false;
            } else if (isUserOnWlanSettings()) {
                return false;
            } else {
                if (deltaMs > 300000) {
                    config.lastConnFailedType = 0;
                    config.lastConnFailedTimestamp = 0;
                    config.rssiStatusDisabled = WifiHandover.INVALID_RSSI;
                    return false;
                } else if ((count == 1 && deltaMs < 10000) || ((count == 2 && deltaMs < 30000) || ((count == 3 && deltaMs < HidataWechatTraffic.MIN_VALID_TIME) || (count == 4 && deltaMs < 90000)))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unselectDueToFailedLastTime, DELAYED!!! count = ");
                    stringBuilder.append(count);
                    stringBuilder.append(", deltaMs = ");
                    stringBuilder.append(deltaMs);
                    stringBuilder.append(", ssid = ");
                    stringBuilder.append(scanResult.SSID);
                    stringBuilder.append(", level = ");
                    stringBuilder.append(scanResult.level);
                    LOGD(stringBuilder.toString());
                    return true;
                }
            }
        }
        return false;
    }

    private boolean unselectDhcpFailedBssid(ScanResult scanResult, WifiConfiguration config) {
        if (scanResult == null || config == null || HwSelfCureEngine.getInstance() == null || !HwSelfCureEngine.getInstance().isDhcpFailedBssid(scanResult.BSSID)) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unselectDhcpFailedBssid, key = ");
        stringBuilder.append(config.configKey());
        LOGD(stringBuilder.toString());
        return true;
    }

    private boolean unselectPoorNetwork(ScanResult scanResult, WifiConfiguration config) {
        if (scanResult == null || config == null || isMobileDataInactive() || WifiProCommonUtils.getSignalLevel(scanResult.frequency, scanResult.level) >= 3 || (HwSelfCureEngine.getInstance() != null && HwSelfCureEngine.getInstance().isDhcpFailedConfigKey(config.configKey()))) {
            return false;
        }
        long ts = this.mWifiStateMachine.getWifiEnabledTimeStamp();
        if (ts != 0 && System.currentTimeMillis() - ts < 20000) {
            return false;
        }
        HwQoEService mHwQoEService = HwQoEService.getInstance();
        if (mHwQoEService != null && mHwQoEService.isConnectWhenWeChating(scanResult)) {
            return false;
        }
        String pktName = "";
        HwAutoConnectManager autoConnectManager = HwAutoConnectManager.getInstance();
        if (autoConnectManager != null) {
            pktName = autoConnectManager.getCurrentPackageName();
        }
        if (!HOME_PKT_NAME.equals(pktName)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unselectPoorNetwork, DELAY!!! level = ");
            stringBuilder.append(scanResult.level);
            stringBuilder.append(", ssid = ");
            stringBuilder.append(scanResult.SSID);
            LOGD(stringBuilder.toString());
            return true;
        }
        return false;
    }

    public boolean unselectDiscNonLocally(ScanResult scanResult, WifiConfiguration config) {
        if (unselectDhcpFailedBssid(scanResult, config) || unselectPoorNetwork(scanResult, config)) {
            return true;
        }
        if (scanResult == null || config == null || config.rssiDiscNonLocally == 0 || config.rssiDiscNonLocally == WifiHandover.INVALID_RSSI || WifiProCommonUtils.getSignalLevel(scanResult.frequency, scanResult.level) >= 3 || isMobileDataInactive()) {
            return false;
        }
        if (scanResult.level - config.rssiDiscNonLocally < (WifiProCommonUtils.isWpaOrWpa2(config) ? 5 : 8)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unselectDiscNonLocally, DELAYED this bssid !!! current = ");
            stringBuilder.append(scanResult.level);
            stringBuilder.append(", disc = ");
            stringBuilder.append(config.rssiDiscNonLocally);
            stringBuilder.append(", ssid = ");
            stringBuilder.append(config.configKey());
            LOGD(stringBuilder.toString());
            return true;
        }
        return false;
    }

    public synchronized WifiConfiguration getLastCandidateByWifiPro(WifiConfiguration config, ScanResult scanResultCandidate) {
        WifiConfiguration lastConfig;
        lastConfig = candidateUpdatedByWifiPro(config);
        scanResultUpdatedByWifiPro(lastConfig, scanResultCandidate);
        handleSelectNetworkCompleted(lastConfig);
        return lastConfig;
    }

    /* JADX WARNING: Missing block: B:17:0x005c, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:36:0x00c6, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean selectBestNetworkByWifiPro(WifiConfiguration config, ScanResult scanResult) {
        StringBuilder stringBuilder;
        if (this.mWifiStateMachine.isWifiSelfCuring() && config != null && config.networkId == this.mWifiStateMachine.getSelfCureNetworkId()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("selectBestNetworkByWifiPro, wifi self curing, forced connecting network = ");
            stringBuilder.append(config.configKey());
            LOGD(stringBuilder.toString());
            if (this.selfCureNetworkCandidate == null || this.selfCureScanResultCandidate == null) {
                this.selfCureNetworkCandidate = config;
                this.selfCureScanResultCandidate = scanResult;
                this.selfCureNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
            } else if (scanResult != null && scanResult.level > this.selfCureScanResultCandidate.level) {
                this.selfCureNetworkCandidate = config;
                this.selfCureScanResultCandidate = scanResult;
                this.selfCureNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
            }
        } else {
            if (isWifiProEnabled() && this.mWifiStateMachine.getCurrentNetwork() == null && config != null && scanResult != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("selectBestNetworkByWifiPro, current = ");
                stringBuilder.append(config.configKey(true));
                stringBuilder.append(", internetHistory = ");
                stringBuilder.append(config.internetHistory);
                stringBuilder.append(", level = ");
                stringBuilder.append(scanResult.level);
                stringBuilder.append(", 5GHz = ");
                stringBuilder.append(scanResult.is5GHz());
                LOGD(stringBuilder.toString());
                if (networkIgnoredByWifiPro(config, scanResult) || selectNetworkHasInternet(config, scanResult) || selectNetworkHasInternetEver(config, scanResult) || selectNetworkPortal(config, scanResult) || selectNetworkNoInternet(config, scanResult)) {
                }
            }
            return false;
        }
    }

    private boolean networkIgnoredByWifiPro(WifiConfiguration config, ScanResult scanResult) {
        if (config != null && config.isTempCreated) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(config.SSID);
            stringBuilder.append(", networkIgnoredByAPScore, skip candidate due to istempcreated");
            LOGD(stringBuilder.toString());
            return true;
        } else if (scanResult == null || HwAutoConnectManager.getInstance() == null || !HwAutoConnectManager.getInstance().isBssidMatchedBlacklist(scanResult.BSSID)) {
            return false;
        } else {
            return true;
        }
    }

    private boolean selectNetworkHasInternet(WifiConfiguration config, ScanResult scanResult) {
        if (config == null || HUAWEI_GUEST.equals(config.configKey())) {
            return false;
        }
        if (!hasInternet(config) && !maybeHasInternet(config) && (!config.noInternetAccess || config.internetRecoveryStatus != 5)) {
            return false;
        }
        if (this.hasInetNetworkCandidate == null) {
            this.hasInetNetworkCandidate = config;
            this.hasInetScanResultCandidate = scanResult;
            this.hasInetNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
        } else if (!maybeHasInternet(this.hasInetNetworkCandidate) && maybeHasInternet(config)) {
            return true;
        } else {
            if (maybeHasInternet(this.hasInetNetworkCandidate) && !maybeHasInternet(config)) {
                this.hasInetNetworkCandidate = config;
                this.hasInetScanResultCandidate = scanResult;
                this.hasInetNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
                return true;
            } else if ((ScanResult.is5GHz(this.hasInetScanResultCandidate.frequency) && ScanResult.is5GHz(scanResult.frequency)) || (ScanResult.is24GHz(this.hasInetScanResultCandidate.frequency) && ScanResult.is24GHz(scanResult.frequency))) {
                if (scanResult.level > this.hasInetScanResultCandidate.level) {
                    this.hasInetNetworkCandidate = config;
                    this.hasInetScanResultCandidate = scanResult;
                    this.hasInetNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
                }
            } else if (ScanResult.is5GHz(this.hasInetScanResultCandidate.frequency) && ScanResult.is24GHz(scanResult.frequency)) {
                if (this.hasInetScanResultCandidate.level < -72 && scanResult.level > this.hasInetScanResultCandidate.level) {
                    this.hasInetNetworkCandidate = config;
                    this.hasInetScanResultCandidate = scanResult;
                    this.hasInetNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
                }
            } else if (ScanResult.is24GHz(this.hasInetScanResultCandidate.frequency) && ScanResult.is5GHz(scanResult.frequency) && (scanResult.level > this.hasInetScanResultCandidate.level || scanResult.level >= -72)) {
                this.hasInetNetworkCandidate = config;
                this.hasInetScanResultCandidate = scanResult;
                this.hasInetNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
            }
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:21:0x0089, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean selectNetworkHasInternetEver(WifiConfiguration config, ScanResult scanResult) {
        boolean z = false;
        if (config == null || HUAWEI_GUEST.equals(config.configKey()) || !config.noInternetAccess || config.portalNetwork || !WifiProCommonUtils.allowWifiConfigRecovery(config.internetHistory)) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("selectNetworkHasInternetEver, recovery matched, candidate = ");
        stringBuilder.append(config.configKey(true));
        stringBuilder.append(", recoveryNetworkCandidate is null = ");
        if (this.recoveryNetworkCandidate == null) {
            z = true;
        }
        stringBuilder.append(z);
        LOGD(stringBuilder.toString());
        if (this.recoveryNetworkCandidate == null) {
            this.recoveryNetworkCandidate = config;
            this.recoveryScanResultCandidate = scanResult;
            this.recoveryNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
        } else if (rssiStronger(scanResult, this.recoveryScanResultCandidate)) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("selectNetworkHasInternetEver, use the stronger network for recovery candidate, new candidate = ");
            stringBuilder2.append(config.configKey(true));
            LOGD(stringBuilder2.toString());
            this.recoveryNetworkCandidate = config;
            this.recoveryScanResultCandidate = scanResult;
            this.recoveryNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
        }
        return true;
    }

    private boolean selectNetworkPortal(WifiConfiguration config, ScanResult scanResult) {
        if (config == null || scanResult == null) {
            return false;
        }
        if (!config.portalNetwork && !WifiProCommonUtils.isOpenAndMaybePortal(config) && !WifiProCommonUtils.isInMonitorList(config.configKey(), WifiProCommonUtils.NON_OPEN_PORTALS) && !HUAWEI_GUEST.equals(config.configKey())) {
            return false;
        }
        if (this.portalNotifiedConfigKey != null && this.portalNotifiedConfigKey.equals(config.configKey())) {
            this.portalDisappeared = false;
            this.portalDisappearedCounter = 0;
            if (scanResult.level > this.portalNotifiedMaxRssi) {
                this.portalNotifiedMaxRssi = scanResult.level;
            }
        }
        if (WifiProCommonUtils.getSignalLevel(scanResult.frequency, scanResult.level) <= 1 || (WifiProCommonUtils.getSignalLevel(scanResult.frequency, scanResult.level) <= 2 && (config.lastHasInternetTimestamp == 0 || System.currentTimeMillis() - config.lastHasInternetTimestamp >= 7200000))) {
            return true;
        }
        if (!isUserOnWlanSettings() && this.portalNotifiedConfigKey != null && !this.portalNotifiedConfigKey.equals(config.configKey()) && config.lastHasInternetTimestamp == 0) {
            return true;
        }
        if (!isUserOnWlanSettings() && this.portalNotifiedConfigKey != null && this.portalNotifiedConfigKey.equals(config.configKey()) && (!this.portalNotifiedHasInternet || WifiProCommonUtils.getSignalLevel(scanResult.frequency, scanResult.level) <= 2)) {
            return true;
        }
        if (!isUserOnWlanSettings() && HwAutoConnectManager.getInstance() != null && !HwAutoConnectManager.getInstance().allowCheckPortalNetwork(config.configKey(), scanResult.BSSID)) {
            return true;
        }
        StringBuilder stringBuilder;
        if (this.portalNetworkCandidate == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("selectNetworkPortal, portal status unknown, backup it if no other choice, candidate = ");
            stringBuilder.append(config.configKey());
            LOGD(stringBuilder.toString());
            this.portalNetworkCandidate = config;
            this.portalScanResultCandidate = scanResult;
            this.portalNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
        } else if (config.lastHasInternetTimestamp == 0 && this.portalNetworkCandidate.lastHasInternetTimestamp == 0) {
            if (rssiStronger(scanResult, this.portalScanResultCandidate)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("selectNetworkPortal, use the stronger rssi for portal unauthen candidate, new candidate = ");
                stringBuilder.append(config.configKey());
                LOGD(stringBuilder.toString());
                this.portalNetworkCandidate = config;
                this.portalScanResultCandidate = scanResult;
                this.portalNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
            }
        } else if (config.lastHasInternetTimestamp > this.portalNetworkCandidate.lastHasInternetTimestamp) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("selectNetworkPortal, use the portal network that login recently, new candidate = ");
            stringBuilder.append(config.configKey());
            LOGD(stringBuilder.toString());
            this.portalNetworkCandidate = config;
            this.portalScanResultCandidate = scanResult;
            this.portalNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
        }
        return true;
    }

    private boolean selectNetworkNoInternet(WifiConfiguration config, ScanResult scanResult) {
        if (!config.noInternetAccess || WifiProCommonUtils.allowWifiConfigRecovery(config.internetHistory)) {
            return false;
        }
        StringBuilder stringBuilder;
        if (this.recoveryNetworkCandidate == null && this.portalNetworkCandidate == null) {
            if (config.internetRecoveryCheckTimestamp > 0 && config.internetRecoveryStatus == 4 && System.currentTimeMillis() - config.internetRecoveryCheckTimestamp > 3600000) {
                LOGD("selectNetworkNoInternet, recovery unmatched, reset tobe unknown after 1h from last checking!");
                config.internetRecoveryStatus = 3;
            }
            if (config.internetRecoveryStatus == 4 && WifiProCommonUtils.isWifiProSwitchOn(this.mContext)) {
                return true;
            }
            if (this.noInetNetworkCandidate == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("selectNetworkNoInternet, no internet network = ");
                stringBuilder.append(config.configKey(true));
                stringBuilder.append(", backup it if no other better one.");
                LOGD(stringBuilder.toString());
                this.noInetNetworkCandidate = config;
                this.noInetScanResultCandidate = scanResult;
                this.noInetNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
            } else if (rssiStronger(scanResult, this.noInetScanResultCandidate)) {
                this.noInetNetworkCandidate = config;
                this.noInetScanResultCandidate = scanResult;
                this.noInetNetworkCandidate.getNetworkSelectionStatus().setCandidate(scanResult);
            }
            return true;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("selectNetworkNoInternet, better network selected, skip due to no internet, candidate = ");
        stringBuilder.append(config.configKey(true));
        LOGD(stringBuilder.toString());
        return true;
    }

    private WifiConfiguration candidateUpdatedByWifiPro(WifiConfiguration config) {
        if (this.mWifiStateMachine.isWifiSelfCuring() && this.selfCureNetworkCandidate != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Within Wifi Self Curing, the highest network is ");
            stringBuilder.append(this.selfCureNetworkCandidate.configKey(true));
            LOGD(stringBuilder.toString());
            this.mSelfCureCandidateLostCnt = 0;
            return this.selfCureNetworkCandidate;
        } else if (this.mWifiStateMachine.isWifiSelfCuring() && this.selfCureNetworkCandidate == null) {
            LOGD("candidateUpdatedByWifiPro, Within Wifi Self Curing and AP lost at this scan, skip connect others.");
            this.mSelfCureCandidateLostCnt++;
            if (this.mSelfCureCandidateLostCnt == 2) {
                LOGD("candidateUpdatedByWifiPro, stop self cure because AP lost 2 times");
                this.mWifiStateMachine.notifySelfCureNetworkLost();
            }
            return null;
        } else {
            WifiConfiguration newConfig = config;
            if (isWifiProEnabled()) {
                this.mWifiStateMachine.setWifiBackgroundReason(5);
                StringBuilder stringBuilder2;
                if (this.hasInetNetworkCandidate != null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("HwEvaluator, use the best candidate has internet always or unknown, network = ");
                    stringBuilder2.append(this.hasInetNetworkCandidate.configKey(true));
                    LOGD(stringBuilder2.toString());
                    newConfig = this.hasInetNetworkCandidate;
                    this.backupTypeSelected = HAS_INET_SELECTED;
                } else if (this.recoveryNetworkCandidate != null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("HwEvaluator, use the recovery matched candidate(has internet ever), network = ");
                    stringBuilder2.append(this.recoveryNetworkCandidate.configKey(true));
                    LOGD(stringBuilder2.toString());
                    newConfig = this.recoveryNetworkCandidate;
                    this.backupTypeSelected = RECOVERY_SELECTED;
                } else if (this.portalNetworkCandidate != null) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("HwEvaluator, use the best portal to connect, network = ");
                    stringBuilder3.append(this.portalNetworkCandidate.configKey(true));
                    LOGD(stringBuilder3.toString());
                    newConfig = this.portalNetworkCandidate;
                    this.backupTypeSelected = NORMAL_PORTAL_SELECTED;
                    if (!isUserOnWlanSettings()) {
                        this.mWifiStateMachine.setWifiBackgroundReason(0);
                    }
                } else if (this.noInetNetworkCandidate != null) {
                    if (!WifiProCommonUtils.isWifiProSwitchOn(this.mContext)) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("HwEvaluator, WLAN+ off, use no internet network(lowest priority) to connect, network = ");
                        stringBuilder2.append(this.noInetNetworkCandidate.configKey(true));
                        LOGD(stringBuilder2.toString());
                        newConfig = this.noInetNetworkCandidate;
                        this.backupTypeSelected = NO_INET_SELECTED;
                    } else if (!(isUserOnWlanSettings() || this.noInetNetworkCandidate.internetRecoveryStatus == 4 || !WifiProCommonUtils.matchedRequestByHistory(this.noInetNetworkCandidate.internetHistory, 104))) {
                        newConfig = this.noInetNetworkCandidate;
                        this.backupTypeSelected = NO_INET_SELECTED;
                        this.mWifiStateMachine.setWifiBackgroundReason(3);
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("HwEvaluator, background connection for no internet access, network = ");
                        stringBuilder2.append(this.noInetNetworkCandidate.configKey());
                        LOGD(stringBuilder2.toString());
                    }
                }
            }
            return newConfig;
        }
    }

    private ScanResult scanResultUpdatedByWifiPro(WifiConfiguration networkCandidate, ScanResult scanResult) {
        if (this.mWifiStateMachine.isWifiSelfCuring() && this.selfCureScanResultCandidate != null) {
            return this.selfCureScanResultCandidate;
        }
        if (this.mWifiStateMachine.isWifiSelfCuring() && this.selfCureScanResultCandidate == null) {
            LOGD("scanResultUpdatedByWifiPro, Within Wifi Self Curing and AP lost at this scan, skip connect others.");
            return null;
        }
        if (isWifiProEnabled() && networkCandidate != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwEvaluator, update scan result, selected type = ");
            stringBuilder.append(this.backupTypeSelected);
            LOGD(stringBuilder.toString());
            if (this.backupTypeSelected == HAS_INET_SELECTED) {
                return this.hasInetScanResultCandidate;
            }
            if (this.backupTypeSelected == RECOVERY_SELECTED) {
                return this.recoveryScanResultCandidate;
            }
            if (this.backupTypeSelected == TRUSTED_PORTAL_SELECTED) {
                return this.portalScanResultTursted;
            }
            if (this.backupTypeSelected == NORMAL_PORTAL_SELECTED) {
                return this.portalScanResultCandidate;
            }
            if (this.backupTypeSelected == NO_INET_SELECTED) {
                return this.noInetScanResultCandidate;
            }
        }
        return scanResult;
    }

    public synchronized void handleSelectNetworkCompleted(WifiConfiguration candidate) {
        if (isWifiProEnabled() && this.mWifiStateMachine.getCurrentNetwork() == null) {
            if ((candidate == null ? -4 : 0) == -4) {
                if (this.portalNotifiedConfigKey != null && this.portalDisappeared) {
                    this.portalDisappearedCounter++;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("handleAutoJoinCompleted, notified portal = ");
                    stringBuilder.append(this.portalNotifiedConfigKey);
                    stringBuilder.append(", disappear counter = ");
                    stringBuilder.append(this.portalDisappearedCounter);
                    LOGD(stringBuilder.toString());
                    if (this.portalDisappearedCounter >= PORTAL_DISAPPEAR_THRESHOLD) {
                        Intent intent = new Intent();
                        intent.setAction(WifiproUtils.ACTION_NOTIFY_PORTAL_OUT_OF_RANGE);
                        intent.setFlags(67108864);
                        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                    }
                } else if (!(this.portalNotifiedConfigKey == null || this.portalDisappeared || HwAutoConnectManager.getInstance() == null)) {
                    HwAutoConnectManager.getInstance().updatePopUpNetworkRssi(this.portalNotifiedConfigKey, this.portalNotifiedMaxRssi);
                }
            }
        }
    }

    public synchronized void resetSelfCureCandidateLostCnt() {
        this.mSelfCureCandidateLostCnt = 0;
    }

    public synchronized void portalNotifyChanged(boolean popUp, String configKey, boolean hasInternetAccess) {
        if (popUp) {
            try {
                this.portalNotifiedConfigKey = configKey;
                this.portalNotifiedHasInternet = hasInternetAccess;
            } catch (Throwable th) {
            }
        } else {
            this.portalNotifiedConfigKey = null;
            this.portalNotifiedHasInternet = false;
        }
        this.portalDisappearedCounter = 0;
    }

    private boolean rssiStronger(ScanResult newObj, ScanResult oldObj) {
        if (newObj == null || oldObj == null || newObj.level <= oldObj.level) {
            return false;
        }
        return true;
    }

    private boolean hasInternet(WifiConfiguration config) {
        return (config == null || config.noInternetAccess || config.portalNetwork || !WifiProCommonUtils.matchedRequestByHistory(config.internetHistory, 100)) ? false : true;
    }

    private boolean maybeHasInternet(WifiConfiguration config) {
        return (WifiProCommonUtils.isOpenType(config) || config.noInternetAccess || config.portalNetwork || !WifiProCommonUtils.matchedRequestByHistory(config.internetHistory, 103)) ? false : true;
    }

    private boolean isUserOnWlanSettings() {
        return WifiProCommonUtils.isQueryActivityMatched(this.mContext, "com.android.settings.Settings$WifiSettingsActivity");
    }

    private boolean isMobileDataInactive() {
        return WifiProCommonUtils.isMobileDataOff(this.mContext) || WifiProCommonUtils.isNoSIMCard(this.mContext) || WifiProCommonUtils.isWifiOnly(this.mContext);
    }

    private boolean isWifiProEnabled() {
        return true;
    }

    public synchronized boolean isWifiProEnabledOrSelfCureGoing() {
        boolean z;
        z = isWifiProEnabled() || this.mWifiStateMachine.isWifiSelfCuring();
        return z;
    }

    private void LOGD(String msg) {
        Log.d(TAG, msg);
    }
}
