package com.android.server.wifi;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class WifiLastResortWatchdog {
    public static final String BSSID_ANY = "any";
    public static final String BUGREPORT_TITLE = "Wifi watchdog triggered";
    public static final int FAILURE_CODE_ASSOCIATION = 1;
    public static final int FAILURE_CODE_AUTHENTICATION = 2;
    public static final int FAILURE_CODE_DHCP = 3;
    public static final int FAILURE_THRESHOLD = 7;
    public static final int MAX_BSSID_AGE = 10;
    public static final double PROB_TAKE_BUGREPORT_DEFAULT = 0.08d;
    private static final String TAG = "WifiLastResortWatchdog";
    private double mBugReportProbability = 0.08d;
    private Clock mClock;
    private Map<String, AvailableNetworkFailureCount> mRecentAvailableNetworks = new HashMap();
    private SelfRecovery mSelfRecovery;
    private Map<String, Pair<AvailableNetworkFailureCount, Integer>> mSsidFailureCount = new HashMap();
    private long mTimeLastTrigger;
    private boolean mVerboseLoggingEnabled = false;
    private boolean mWatchdogAllowedToTrigger = true;
    private boolean mWatchdogFixedWifi = true;
    private boolean mWifiIsConnected = false;
    private WifiMetrics mWifiMetrics;
    private WifiStateMachine mWifiStateMachine;
    private Looper mWifiStateMachineLooper;

    public static class AvailableNetworkFailureCount {
        public int age = 0;
        public int associationRejection = 0;
        public int authenticationFailure = 0;
        public WifiConfiguration config;
        public int dhcpFailure = 0;
        public String ssid = "";

        AvailableNetworkFailureCount(WifiConfiguration configParam) {
            this.config = configParam;
        }

        public void incrementFailureCount(int reason) {
            switch (reason) {
                case 1:
                    this.associationRejection++;
                    return;
                case 2:
                    this.authenticationFailure++;
                    return;
                case 3:
                    this.dhcpFailure++;
                    return;
                default:
                    return;
            }
        }

        void resetCounts() {
            this.associationRejection = 0;
            this.authenticationFailure = 0;
            this.dhcpFailure = 0;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.ssid);
            stringBuilder.append(" HasEverConnected: ");
            stringBuilder.append(this.config != null ? Boolean.valueOf(this.config.getNetworkSelectionStatus().getHasEverConnected()) : "null_config");
            stringBuilder.append(", Failures: {Assoc: ");
            stringBuilder.append(this.associationRejection);
            stringBuilder.append(", Auth: ");
            stringBuilder.append(this.authenticationFailure);
            stringBuilder.append(", Dhcp: ");
            stringBuilder.append(this.dhcpFailure);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    WifiLastResortWatchdog(SelfRecovery selfRecovery, Clock clock, WifiMetrics wifiMetrics, WifiStateMachine wsm, Looper wifiStateMachineLooper) {
        this.mSelfRecovery = selfRecovery;
        this.mClock = clock;
        this.mWifiMetrics = wifiMetrics;
        this.mWifiStateMachine = wsm;
        this.mWifiStateMachineLooper = wifiStateMachineLooper;
    }

    public void updateAvailableNetworks(List<Pair<ScanDetail, WifiConfiguration>> availableNetworks) {
        if (this.mVerboseLoggingEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateAvailableNetworks: size = ");
            stringBuilder.append(availableNetworks.size());
            Log.v(str, stringBuilder.toString());
        }
        if (availableNetworks != null) {
            for (Pair<ScanDetail, WifiConfiguration> pair : availableNetworks) {
                ScanDetail scanDetail = pair.first;
                WifiConfiguration config = pair.second;
                ScanResult scanResult = scanDetail.getScanResult();
                if (scanResult != null) {
                    String bssid = scanResult.BSSID;
                    String ssid = new StringBuilder();
                    ssid.append("\"");
                    ssid.append(scanDetail.getSSID());
                    ssid.append("\"");
                    ssid = ssid.toString();
                    if (this.mVerboseLoggingEnabled) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(" ");
                        stringBuilder2.append(bssid);
                        stringBuilder2.append(": ");
                        stringBuilder2.append(scanDetail.getSSID());
                        Log.v(str2, stringBuilder2.toString());
                    }
                    AvailableNetworkFailureCount availableNetworkFailureCount = (AvailableNetworkFailureCount) this.mRecentAvailableNetworks.get(bssid);
                    if (availableNetworkFailureCount == null) {
                        availableNetworkFailureCount = new AvailableNetworkFailureCount(config);
                        availableNetworkFailureCount.ssid = ssid;
                        Pair<AvailableNetworkFailureCount, Integer> ssidFailsAndApCount = (Pair) this.mSsidFailureCount.get(ssid);
                        if (ssidFailsAndApCount == null) {
                            ssidFailsAndApCount = Pair.create(new AvailableNetworkFailureCount(config), Integer.valueOf(1));
                            setWatchdogTriggerEnabled(true);
                        } else {
                            ssidFailsAndApCount = Pair.create((AvailableNetworkFailureCount) ssidFailsAndApCount.first, Integer.valueOf(ssidFailsAndApCount.second.intValue() + 1));
                        }
                        this.mSsidFailureCount.put(ssid, ssidFailsAndApCount);
                    }
                    if (config != null) {
                        availableNetworkFailureCount.config = config;
                    }
                    availableNetworkFailureCount.age = -1;
                    this.mRecentAvailableNetworks.put(bssid, availableNetworkFailureCount);
                }
            }
        }
        Iterator<Entry<String, AvailableNetworkFailureCount>> it = this.mRecentAvailableNetworks.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, AvailableNetworkFailureCount> entry = (Entry) it.next();
            if (((AvailableNetworkFailureCount) entry.getValue()).age < 9) {
                AvailableNetworkFailureCount availableNetworkFailureCount2 = (AvailableNetworkFailureCount) entry.getValue();
                availableNetworkFailureCount2.age++;
            } else {
                String ssid2 = ((AvailableNetworkFailureCount) entry.getValue()).ssid;
                Pair<AvailableNetworkFailureCount, Integer> ssidFails = (Pair) this.mSsidFailureCount.get(ssid2);
                if (ssidFails != null) {
                    Integer apCount = Integer.valueOf(((Integer) ssidFails.second).intValue() - 1);
                    if (apCount.intValue() > 0) {
                        this.mSsidFailureCount.put(ssid2, Pair.create((AvailableNetworkFailureCount) ssidFails.first, apCount));
                    } else {
                        this.mSsidFailureCount.remove(ssid2);
                    }
                } else {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("updateAvailableNetworks: SSID to AP count mismatch for ");
                    stringBuilder3.append(ssid2);
                    Log.d(str3, stringBuilder3.toString());
                }
                it.remove();
            }
        }
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, toString());
        }
    }

    public boolean noteConnectionFailureAndTriggerIfNeeded(String ssid, String bssid, int reason) {
        if (this.mVerboseLoggingEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("noteConnectionFailureAndTriggerIfNeeded: [");
            stringBuilder.append(ssid);
            stringBuilder.append(", ");
            stringBuilder.append(bssid);
            stringBuilder.append(", ");
            stringBuilder.append(reason);
            stringBuilder.append("]");
            Log.v(str, stringBuilder.toString());
        }
        updateFailureCountForNetwork(ssid, bssid, reason);
        if (!this.mWatchdogAllowedToTrigger) {
            this.mWifiMetrics.incrementWatchdogTotalConnectionFailureCountAfterTrigger();
            this.mWatchdogFixedWifi = false;
        }
        boolean isRestartNeeded = checkTriggerCondition();
        if (this.mVerboseLoggingEnabled) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isRestartNeeded = ");
            stringBuilder2.append(isRestartNeeded);
            Log.v(str2, stringBuilder2.toString());
        }
        if (isRestartNeeded) {
            setWatchdogTriggerEnabled(false);
            this.mWatchdogFixedWifi = true;
            Log.e(TAG, "Watchdog triggering recovery");
            this.mTimeLastTrigger = this.mClock.getElapsedSinceBootMillis();
            this.mSelfRecovery.trigger(0);
            incrementWifiMetricsTriggerCounts();
            clearAllFailureCounts();
        }
        return isRestartNeeded;
    }

    public void connectedStateTransition(boolean isEntering) {
        if (this.mVerboseLoggingEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("connectedStateTransition: isEntering = ");
            stringBuilder.append(isEntering);
            Log.v(str, stringBuilder.toString());
        }
        this.mWifiIsConnected = isEntering;
        if (isEntering) {
            if (!this.mWatchdogAllowedToTrigger && this.mWatchdogFixedWifi && checkIfAtleastOneNetworkHasEverConnected()) {
                takeBugReportWithCurrentProbability("Wifi fixed after restart");
                this.mWifiMetrics.incrementNumLastResortWatchdogSuccesses();
                this.mWifiMetrics.setWatchdogSuccessTimeDurationMs(this.mClock.getElapsedSinceBootMillis() - this.mTimeLastTrigger);
            }
            clearAllFailureCounts();
            setWatchdogTriggerEnabled(true);
        }
    }

    private void takeBugReportWithCurrentProbability(String bugDetail) {
        if (this.mBugReportProbability > Math.random()) {
            new Handler(this.mWifiStateMachineLooper).post(new -$$Lambda$WifiLastResortWatchdog$mIpWfGvGcRn_UMBeTeYZKeAS5FM(this, bugDetail));
        }
    }

    private void updateFailureCountForNetwork(String ssid, String bssid, int reason) {
        if (this.mVerboseLoggingEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateFailureCountForNetwork: [");
            stringBuilder.append(ssid);
            stringBuilder.append(", ");
            stringBuilder.append(bssid);
            stringBuilder.append(", ");
            stringBuilder.append(reason);
            stringBuilder.append("]");
            Log.v(str, stringBuilder.toString());
        }
        if ("any".equals(bssid)) {
            incrementSsidFailureCount(ssid, reason);
        } else {
            incrementBssidFailureCount(ssid, bssid, reason);
        }
    }

    private void incrementSsidFailureCount(String ssid, int reason) {
        Pair<AvailableNetworkFailureCount, Integer> ssidFails = (Pair) this.mSsidFailureCount.get(ssid);
        if (ssidFails == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateFailureCountForNetwork: No networks for ssid = ");
            stringBuilder.append(ssid);
            Log.d(str, stringBuilder.toString());
            return;
        }
        ssidFails.first.incrementFailureCount(reason);
    }

    private void incrementBssidFailureCount(String ssid, String bssid, int reason) {
        AvailableNetworkFailureCount availableNetworkFailureCount = (AvailableNetworkFailureCount) this.mRecentAvailableNetworks.get(bssid);
        String str;
        StringBuilder stringBuilder;
        if (availableNetworkFailureCount == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateFailureCountForNetwork: Unable to find Network [");
            stringBuilder.append(ssid);
            stringBuilder.append(", ");
            stringBuilder.append(bssid);
            stringBuilder.append("]");
            Log.d(str, stringBuilder.toString());
        } else if (availableNetworkFailureCount.ssid.equals(ssid)) {
            if (availableNetworkFailureCount.config == null && this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("updateFailureCountForNetwork: network has no config [");
                stringBuilder.append(ssid);
                stringBuilder.append(", ");
                stringBuilder.append(bssid);
                stringBuilder.append("]");
                Log.v(str, stringBuilder.toString());
            }
            availableNetworkFailureCount.incrementFailureCount(reason);
            incrementSsidFailureCount(ssid, reason);
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateFailureCountForNetwork: Failed connection attempt has wrong ssid. Failed [");
            stringBuilder.append(ssid);
            stringBuilder.append(", ");
            stringBuilder.append(bssid);
            stringBuilder.append("], buffered [");
            stringBuilder.append(availableNetworkFailureCount.ssid);
            stringBuilder.append(", ");
            stringBuilder.append(bssid);
            stringBuilder.append("]");
            Log.d(str, stringBuilder.toString());
        }
    }

    private boolean checkTriggerCondition() {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "checkTriggerCondition.");
        }
        if ("factory".equals(SystemProperties.get("ro.runmode", "normal"))) {
            Log.d(TAG, "factory version, don't check Watchdog trigger");
            return false;
        } else if (this.mWifiIsConnected || !this.mWatchdogAllowedToTrigger) {
            return false;
        } else {
            for (Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
                if (!isOverFailureThreshold((String) entry.getKey())) {
                    return false;
                }
            }
            boolean atleastOneNetworkHasEverConnected = checkIfAtleastOneNetworkHasEverConnected();
            if (this.mVerboseLoggingEnabled) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checkTriggerCondition: return = ");
                stringBuilder.append(atleastOneNetworkHasEverConnected);
                Log.v(str, stringBuilder.toString());
            }
            return checkIfAtleastOneNetworkHasEverConnected();
        }
    }

    private boolean checkIfAtleastOneNetworkHasEverConnected() {
        for (Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
            if (((AvailableNetworkFailureCount) entry.getValue()).config != null && ((AvailableNetworkFailureCount) entry.getValue()).config.getNetworkSelectionStatus().getHasEverConnected()) {
                return true;
            }
        }
        return false;
    }

    private void incrementWifiMetricsTriggerCounts() {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "incrementWifiMetricsTriggerCounts.");
        }
        this.mWifiMetrics.incrementNumLastResortWatchdogTriggers();
        this.mWifiMetrics.addCountToNumLastResortWatchdogAvailableNetworksTotal(this.mSsidFailureCount.size());
        int badAuth = 0;
        int badAssoc = 0;
        int badDhcp = 0;
        for (Entry<String, Pair<AvailableNetworkFailureCount, Integer>> entry : this.mSsidFailureCount.entrySet()) {
            int i = 0;
            badAuth += ((AvailableNetworkFailureCount) ((Pair) entry.getValue()).first).authenticationFailure >= 7 ? 1 : 0;
            badAssoc += ((AvailableNetworkFailureCount) ((Pair) entry.getValue()).first).associationRejection >= 7 ? 1 : 0;
            if (((AvailableNetworkFailureCount) ((Pair) entry.getValue()).first).dhcpFailure >= 7) {
                i = 1;
            }
            badDhcp += i;
        }
        if (badAuth > 0) {
            this.mWifiMetrics.addCountToNumLastResortWatchdogBadAuthenticationNetworksTotal(badAuth);
            this.mWifiMetrics.incrementNumLastResortWatchdogTriggersWithBadAuthentication();
        }
        if (badAssoc > 0) {
            this.mWifiMetrics.addCountToNumLastResortWatchdogBadAssociationNetworksTotal(badAssoc);
            this.mWifiMetrics.incrementNumLastResortWatchdogTriggersWithBadAssociation();
        }
        if (badDhcp > 0) {
            this.mWifiMetrics.addCountToNumLastResortWatchdogBadDhcpNetworksTotal(badDhcp);
            this.mWifiMetrics.incrementNumLastResortWatchdogTriggersWithBadDhcp();
        }
    }

    public void clearAllFailureCounts() {
        if (this.mVerboseLoggingEnabled) {
            Log.v(TAG, "clearAllFailureCounts.");
        }
        for (Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
            ((AvailableNetworkFailureCount) entry.getValue()).resetCounts();
        }
        for (Entry<String, Pair<AvailableNetworkFailureCount, Integer>> entry2 : this.mSsidFailureCount.entrySet()) {
            ((AvailableNetworkFailureCount) ((Pair) entry2.getValue()).first).resetCounts();
        }
    }

    Map<String, AvailableNetworkFailureCount> getRecentAvailableNetworks() {
        return this.mRecentAvailableNetworks;
    }

    private void setWatchdogTriggerEnabled(boolean enable) {
        if (this.mVerboseLoggingEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setWatchdogTriggerEnabled: enable = ");
            stringBuilder.append(enable);
            Log.v(str, stringBuilder.toString());
        }
        this.mWatchdogAllowedToTrigger = enable;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("mWatchdogAllowedToTrigger: ");
        sb.append(this.mWatchdogAllowedToTrigger);
        sb.append("\nmWifiIsConnected: ");
        sb.append(this.mWifiIsConnected);
        sb.append("\nmRecentAvailableNetworks: ");
        sb.append(this.mRecentAvailableNetworks.size());
        for (Entry<String, AvailableNetworkFailureCount> entry : this.mRecentAvailableNetworks.entrySet()) {
            sb.append("\n ");
            sb.append((String) entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append(", Age: ");
            sb.append(((AvailableNetworkFailureCount) entry.getValue()).age);
        }
        sb.append("\nmSsidFailureCount:");
        for (Entry<String, Pair<AvailableNetworkFailureCount, Integer>> entry2 : this.mSsidFailureCount.entrySet()) {
            AvailableNetworkFailureCount failureCount = ((Pair) entry2.getValue()).first;
            Integer apCount = ((Pair) entry2.getValue()).second;
            sb.append("\n");
            sb.append((String) entry2.getKey());
            sb.append(": ");
            sb.append(apCount);
            sb.append(",");
            sb.append(failureCount.toString());
        }
        return sb.toString();
    }

    public boolean isOverFailureThreshold(String bssid) {
        if (getFailureCount(bssid, 1) >= 7 || getFailureCount(bssid, 2) >= 7 || getFailureCount(bssid, 3) >= 7) {
            return true;
        }
        return false;
    }

    public int getFailureCount(String bssid, int reason) {
        AvailableNetworkFailureCount availableNetworkFailureCount = (AvailableNetworkFailureCount) this.mRecentAvailableNetworks.get(bssid);
        if (availableNetworkFailureCount == null) {
            return 0;
        }
        String ssid = availableNetworkFailureCount.ssid;
        Pair<AvailableNetworkFailureCount, Integer> ssidFails = (Pair) this.mSsidFailureCount.get(ssid);
        if (ssidFails == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getFailureCount: Could not find SSID count for ");
            stringBuilder.append(ssid);
            Log.d(str, stringBuilder.toString());
            return 0;
        }
        AvailableNetworkFailureCount failCount = ssidFails.first;
        switch (reason) {
            case 1:
                return failCount.associationRejection;
            case 2:
                return failCount.authenticationFailure;
            case 3:
                return failCount.dhcpFailure;
            default:
                return 0;
        }
    }

    protected void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mVerboseLoggingEnabled = true;
        } else {
            this.mVerboseLoggingEnabled = false;
        }
    }

    @VisibleForTesting
    protected void setBugReportProbability(double newProbability) {
        this.mBugReportProbability = newProbability;
    }
}
