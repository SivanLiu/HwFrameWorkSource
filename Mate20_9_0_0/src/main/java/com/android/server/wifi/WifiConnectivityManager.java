package com.android.server.wifi;

import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiScanner.ChannelSpec;
import android.net.wifi.WifiScanner.PnoSettings;
import android.net.wifi.WifiScanner.PnoSettings.PnoNetwork;
import android.net.wifi.WifiScanner.ScanData;
import android.net.wifi.WifiScanner.ScanListener;
import android.net.wifi.WifiScanner.ScanSettings;
import android.net.wifi.WifiScanner.ScanSettings.HiddenNetwork;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.hotspot2.PasspointNetworkEvaluator;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.util.StringUtil;
import huawei.android.app.admin.HwDevicePolicyManagerEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class WifiConnectivityManager extends AbsWifiConnectivityManager {
    @VisibleForTesting
    public static final int BSSID_BLACKLIST_EXPIRE_TIME_MS = 300000;
    @VisibleForTesting
    public static final int BSSID_BLACKLIST_THRESHOLD = 3;
    private static final int CHANNEL_LIST_AGE_MS = 3600000;
    private static final int CONNECTED_PNO_SCAN_INTERVAL_MS = 160000;
    private static final int DISCONNECTED_PNO_SCAN_INTERVAL_MS = 20000;
    public static final String HOUR_PERIODIC_SCAN_TIMER_TAG = "WifiConnectivityManager Schedule Hour Periodic Scan Timer";
    private static final int HOUR_PERIOD_SCAN_INTERVAL_MS = 3600000;
    public static final int HW_MIN_PERIODIC_SCAN_INTERVAL_MS = 10000;
    private static final int LOW_RSSI_NETWORK_RETRY_MAX_DELAY_MS = 80000;
    private static final int LOW_RSSI_NETWORK_RETRY_START_DELAY_MS = 20000;
    public static final int MAX_CONNECTION_ATTEMPTS_RATE = 6;
    public static final int MAX_CONNECTION_ATTEMPTS_TIME_INTERVAL_MS = 240000;
    @VisibleForTesting
    public static final int MAX_PERIODIC_SCAN_INTERVAL_MS = 160000;
    public static final int MAX_PNO_SCAN_RESTART_ALLOWED = 2;
    @VisibleForTesting
    public static final int MAX_SCAN_RESTART_ALLOWED = 5;
    private static final int PASSPOINT_NETWORK_EVALUATOR_PRIORITY = 2;
    @VisibleForTesting
    public static final int PERIODIC_SCAN_INTERVAL_MS = 20000;
    public static final String PERIODIC_SCAN_TIMER_TAG = "WifiConnectivityManager Schedule Periodic Scan Timer";
    private static final String POLICY_AUTO_CONNECT = "policy-auto-connect";
    protected static final int PRECONFIGURED_NETWORK_EVALUATOR_PRIORITY = 4;
    private static final boolean R1;
    @VisibleForTesting
    public static final int REASON_CODE_AP_UNABLE_TO_HANDLE_NEW_STA = 17;
    private static final long RESET_TIME_STAMP = Long.MIN_VALUE;
    public static final String RESTART_CONNECTIVITY_SCAN_TIMER_TAG = "WifiConnectivityManager Restart Scan";
    private static final int RESTART_SCAN_DELAY_MS = 2000;
    public static final String RESTART_SINGLE_SCAN_TIMER_TAG = "WifiConnectivityManager Restart Single Scan";
    private static final int SAVED_NETWORK_EVALUATOR_PRIORITY = 1;
    public static final int SCAN_COUNT_CHANGE_REASON_ADD = 0;
    public static final int SCAN_COUNT_CHANGE_REASON_MINUS = 1;
    public static final int SCAN_COUNT_CHANGE_REASON_RESET = 2;
    private static final boolean SCAN_IMMEDIATELY = true;
    private static final boolean SCAN_ON_SCHEDULE = false;
    private static final int SCORED_NETWORK_EVALUATOR_PRIORITY = 3;
    private static final int SWITCH_TO_WIFI_AUTO = 0;
    private static final String TAG = "WifiConnectivityManager";
    private static final int VALID_ROAM_BLACK_LIST_MIN_SSID_NUM = 2;
    private static final String VALUE_DISABLE = "value_disable";
    private static final int WATCHDOG_INTERVAL_MS = 1680000;
    public static final String WATCHDOG_TIMER_TAG = "WifiConnectivityManager Schedule Watchdog Timer";
    public static final int WIFI_STATE_CONNECTED = 1;
    public static final int WIFI_STATE_DISCONNECTED = 2;
    public static final int WIFI_STATE_TRANSITIONING = 3;
    public static final int WIFI_STATE_UNKNOWN = 0;
    private final AlarmManager mAlarmManager;
    private final AllSingleScanListener mAllSingleScanListener = new AllSingleScanListener(this, null);
    private int mBand5GHzBonus;
    private Map<String, BssidBlacklistStatus> mBssidBlacklist = new HashMap();
    private final CarrierNetworkConfig mCarrierNetworkConfig;
    private final CarrierNetworkNotifier mCarrierNetworkNotifier;
    private final Clock mClock;
    private final WifiConfigManager mConfigManager;
    private final LinkedList<Long> mConnectionAttemptTimeStamps;
    private final WifiConnectivityHelper mConnectivityHelper;
    private Context mContext;
    private int mCurrentConnectionBonus;
    private boolean mDbg = false;
    private boolean mEnableAutoJoinWhenAssociated;
    private final Handler mEventHandler;
    private int mFullScanMaxRxRate;
    private int mFullScanMaxTxRate;
    private final OnAlarmListener mHourPeriodicScanTimerListener = new OnAlarmListener() {
        public void onAlarm() {
            WifiConnectivityManager.this.startHourPeriodicSingleScan();
        }
    };
    private boolean mHourPeriodicScanTimerSet = false;
    private HwWifiCHRService mHwWifiCHRService;
    private int mInitialScoreMax;
    private String mLastConnectionAttemptBssid = null;
    private long mLastHourPeriodicSingleScanTimeStamp = RESET_TIME_STAMP;
    private long mLastPeriodicSingleScanTimeStamp = RESET_TIME_STAMP;
    private final LocalLog mLocalLog;
    private int mMin24GHzRssi;
    private int mMin5GHzRssi;
    protected final WifiNetworkSelector mNetworkSelector;
    private final Set<String> mOldSsidList = new HashSet();
    private final OpenNetworkNotifier mOpenNetworkNotifier;
    private final OnAlarmListener mPeriodicScanTimerListener = new OnAlarmListener() {
        public void onAlarm() {
            WifiConnectivityManager.this.periodicScanTimerHandler();
        }
    };
    private boolean mPeriodicScanTimerSet = false;
    private int mPeriodicSingleScanInterval = PERIODIC_SCAN_INTERVAL_MS;
    private final PnoScanListener mPnoScanListener = new PnoScanListener(this, null);
    private int mPnoScanRestartCount = 0;
    private boolean mPnoScanStarted = false;
    private final OnAlarmListener mRestartScanListener = new OnAlarmListener() {
        public void onAlarm() {
            WifiConnectivityManager.this.startConnectivityScan(true, true);
        }
    };
    private int mSameNetworkBonus;
    private int mScanRestartCount = 0;
    private final WifiScanner mScanner;
    private final ScoringParams mScoringParams;
    private boolean mScreenOn = false;
    private int mSecureBonus;
    private int mSingleScanRestartCount = 0;
    private boolean mSingleScanStarted = false;
    private final WifiStateMachine mStateMachine;
    private int mTotalConnectivityAttemptsRateLimited = 0;
    private boolean mUntrustedConnectionAllowed = false;
    private boolean mUseSingleRadioChainScanResults = false;
    private boolean mWaitForFullBandScanResults = false;
    private final OnAlarmListener mWatchdogListener = new OnAlarmListener() {
        public void onAlarm() {
            WifiConnectivityManager.this.watchdogHandler();
        }
    };
    private boolean mWifiConnectivityManagerEnabled = true;
    private boolean mWifiEnabled = false;
    private final WifiInfo mWifiInfo;
    private final WifiLastResortWatchdog mWifiLastResortWatchdog;
    private final WifiMetrics mWifiMetrics;
    private WifiNetworkNotifier mWifiNetworkNotifier = null;
    protected int mWifiState = 0;

    private class AllSingleScanListener implements ScanListener {
        private boolean mNeedLog;
        private int mNumScanResultsIgnoredDueToSingleRadioChain;
        private List<ScanDetail> mScanDetails;

        private AllSingleScanListener() {
            this.mScanDetails = new ArrayList();
            this.mNeedLog = true;
            this.mNumScanResultsIgnoredDueToSingleRadioChain = 0;
        }

        /* synthetic */ AllSingleScanListener(WifiConnectivityManager x0, AnonymousClass1 x1) {
            this();
        }

        public void clearScanDetails() {
            WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.getScanKey(this), "29", "AllScan clearScanDetails.");
            this.mScanDetails.clear();
            this.mNumScanResultsIgnoredDueToSingleRadioChain = 0;
        }

        public void onSuccess() {
            WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.getScanKey(this), "30", "AllScan registerScanListener onSuccess");
        }

        public void onFailure(int reason, String description) {
            WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.getScanKey(this), "31", "AllScan registerScanListener onFailure: reason:%s description:%s", Integer.valueOf(reason), description);
        }

        public void onPeriodChanged(int periodInMs) {
            WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.getScanKey(this), "32", "AllScan onPeriodChanged periodInMs:%s", Integer.valueOf(periodInMs));
        }

        public void onResults(ScanData[] results) {
            if (results.length > 0 && results[0].isHiddenScanResult()) {
                Log.d(WifiConnectivityManager.TAG, "HiddenScanResult allSingleScanlister retrun");
            } else if (WifiConnectivityManager.this.mWifiEnabled && WifiConnectivityManager.this.mWifiConnectivityManagerEnabled) {
                if (WifiConnectivityManager.this.mWaitForFullBandScanResults) {
                    if (results[0].isAllChannelsScanned()) {
                        WifiConnectivityManager.this.mWaitForFullBandScanResults = false;
                    } else {
                        WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.mScanner.mCurrentScanKeys, "34", "AllScan waiting for full band scan results.");
                        clearScanDetails();
                        return;
                    }
                }
                if (results.length > 0) {
                    WifiConnectivityManager.this.mWifiMetrics.incrementAvailableNetworksHistograms(this.mScanDetails, results[0].isAllChannelsScanned());
                }
                if (this.mNumScanResultsIgnoredDueToSingleRadioChain > 0) {
                    String str = WifiConnectivityManager.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Number of scan results ignored due to single radio chain scan: ");
                    stringBuilder.append(this.mNumScanResultsIgnoredDueToSingleRadioChain);
                    Log.i(str, stringBuilder.toString());
                }
                boolean wasConnectAttempted = WifiConnectivityManager.this.handleScanResults(this.mScanDetails, "AllSingleScanListener", WifiConnectivityManager.this.mScanner.mCurrentScanKeys);
                String result = ScanResultUtil.getScanResultLogs(WifiConnectivityManager.this.mOldSsidList, this.mScanDetails);
                WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.mScanner.mCurrentScanKeys, "35", "AllScan wasConnectAttempted:%s, results:%s", Boolean.valueOf(wasConnectAttempted), result);
                clearScanDetails();
                if (WifiConnectivityManager.this.mPnoScanStarted) {
                    if (wasConnectAttempted) {
                        WifiConnectivityManager.this.mWifiMetrics.incrementNumConnectivityWatchdogPnoBad();
                    } else {
                        WifiConnectivityManager.this.mWifiMetrics.incrementNumConnectivityWatchdogPnoGood();
                    }
                }
                if (WifiConnectivityManager.this.mScreenOn && WifiConnectivityManager.this.mWifiState != 1 && !wasConnectAttempted && WifiConnectivityManager.this.isWifiScanSpecialChannels() && WifiConnectivityManager.this.mSingleScanStarted) {
                    Log.w(WifiConnectivityManager.TAG, "*******wifi scan special channels, but no connect ap ,  force fullband scan ****");
                    WifiConnectivityManager.this.handleScanCountChanged(0);
                    WifiConnectivityManager.this.startSingleScan(true, WifiStateMachine.WIFI_WORK_SOURCE);
                    WifiConnectivityManager.this.mSingleScanStarted = false;
                }
                WifiConnectivityManager.this.mScanRestartCount = 0;
            } else {
                WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.mScanner.mCurrentScanKeys, "33", "AllScan onResults returned mWifiEnabled:%s, mWifiConnectivityManagerEnabled:%s", Boolean.valueOf(WifiConnectivityManager.this.mWifiEnabled), Boolean.valueOf(WifiConnectivityManager.this.mWifiConnectivityManagerEnabled));
                clearScanDetails();
                WifiConnectivityManager.this.mWaitForFullBandScanResults = false;
            }
        }

        public void onFullResult(ScanResult fullScanResult) {
            if (WifiConnectivityManager.this.mWifiEnabled && WifiConnectivityManager.this.mWifiConnectivityManagerEnabled) {
                this.mNeedLog = true;
                if (WifiConnectivityManager.this.mDbg) {
                    WifiConnectivityManager wifiConnectivityManager = WifiConnectivityManager.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(WifiConnectivityManager.this.mScanner.mCurrentScanKeys);
                    stringBuilder.append("AllSingleScanListener onFullResult: ");
                    stringBuilder.append(fullScanResult.SSID);
                    stringBuilder.append(" capabilities ");
                    stringBuilder.append(fullScanResult.capabilities);
                    wifiConnectivityManager.localLog(stringBuilder.toString());
                }
                if (WifiConnectivityManager.this.mUseSingleRadioChainScanResults || fullScanResult.radioChainInfos == null || fullScanResult.radioChainInfos.length != 1) {
                    this.mScanDetails.add(ScanResultUtil.toScanDetail(fullScanResult));
                    WifiConnectivityManager.this.mScanRestartCount = 0;
                    return;
                }
                this.mNumScanResultsIgnoredDueToSingleRadioChain++;
                return;
            }
            if (this.mNeedLog) {
                WifiConnectivityManager.this.localLog("Key#00:", "36", "AllScan onFullResult returned mWifiEnabled:%s, mWifiConnectivityManagerEnabled:%s", Boolean.valueOf(WifiConnectivityManager.this.mWifiEnabled), Boolean.valueOf(WifiConnectivityManager.this.mWifiConnectivityManagerEnabled));
            }
            this.mNeedLog = false;
        }
    }

    private static class BssidBlacklistStatus {
        public long blacklistedTimeStamp;
        public int counter;
        public boolean isBlacklisted;

        private BssidBlacklistStatus() {
            this.blacklistedTimeStamp = WifiConnectivityManager.RESET_TIME_STAMP;
        }

        /* synthetic */ BssidBlacklistStatus(AnonymousClass1 x0) {
            this();
        }
    }

    private class PnoScanListener implements android.net.wifi.WifiScanner.PnoScanListener {
        private int mLowRssiNetworkRetryDelay;
        private List<ScanDetail> mScanDetails;

        private PnoScanListener() {
            this.mScanDetails = new CopyOnWriteArrayList();
            this.mLowRssiNetworkRetryDelay = WifiConnectivityManager.PERIODIC_SCAN_INTERVAL_MS;
        }

        /* synthetic */ PnoScanListener(WifiConnectivityManager x0, AnonymousClass1 x1) {
            this();
        }

        public void clearScanDetails() {
            WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.getScanKey(this), "39", "PnoScan clearScanDetails.");
            this.mScanDetails.clear();
        }

        public void resetLowRssiNetworkRetryDelay() {
            WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.getScanKey(this), "40", "PnoScan resetLowRssiNetworkRetryDelay.");
            this.mLowRssiNetworkRetryDelay = WifiConnectivityManager.PERIODIC_SCAN_INTERVAL_MS;
        }

        @VisibleForTesting
        public int getLowRssiNetworkRetryDelay() {
            return this.mLowRssiNetworkRetryDelay;
        }

        public void onSuccess() {
            WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.getScanKey(this), "41", "PnoScanListener onSuccess");
        }

        public void onFailure(int reason, String description) {
            WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.getScanKey(this), "42", "PnoScanListener onFailure: reason: %s, description:%s", Integer.valueOf(reason), description);
            if (WifiConnectivityManager.this.mScanRestartCount = WifiConnectivityManager.this.mScanRestartCount + 1 < 5) {
                WifiConnectivityManager.this.scheduleDelayedConnectivityScan(WifiConnectivityManager.RESTART_SCAN_DELAY_MS);
                return;
            }
            WifiConnectivityManager.this.mScanRestartCount = 0;
            WifiConnectivityManager.this.localLog("Failed to successfully start PNO scan for 5 times");
        }

        public void onPeriodChanged(int periodInMs) {
            WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.getScanKey(this), "43", "PnoScanListener onPeriodChanged: actual scan period ms", Integer.valueOf(periodInMs));
        }

        public void onResults(ScanData[] results) {
        }

        public void onFullResult(ScanResult fullScanResult) {
        }

        public void onPnoNetworkFound(ScanResult[] results) {
            WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.getScanKey(this), "44", "PnoScanListener: onPnoNetworkFound: results len = %s", Integer.valueOf(results.length));
            for (ScanResult result : results) {
                if (result.informationElements == null) {
                    WifiConnectivityManager.this.localLog("Skipping scan result with null information elements");
                } else {
                    this.mScanDetails.add(ScanResultUtil.toScanDetail(result));
                }
            }
            boolean wasConnectAttempted = WifiConnectivityManager.this.handleScanResults(this.mScanDetails, "PnoScanListener", WifiConnectivityManager.this.getScanKey(this));
            clearScanDetails();
            WifiConnectivityManager.this.mScanRestartCount = 0;
            if (wasConnectAttempted) {
                resetLowRssiNetworkRetryDelay();
            } else {
                if (this.mLowRssiNetworkRetryDelay > WifiConnectivityManager.LOW_RSSI_NETWORK_RETRY_MAX_DELAY_MS) {
                    this.mLowRssiNetworkRetryDelay = WifiConnectivityManager.LOW_RSSI_NETWORK_RETRY_MAX_DELAY_MS;
                }
                if (WifiConnectivityManager.this.mPnoScanRestartCount = WifiConnectivityManager.this.mPnoScanRestartCount + 1 < 2) {
                    WifiConnectivityManager.this.mAlarmManager.set(3, ((long) this.mLowRssiNetworkRetryDelay) + WifiConnectivityManager.this.mClock.getElapsedSinceBootMillis(), WifiConnectivityManager.RESTART_CONNECTIVITY_SCAN_TIMER_TAG, WifiConnectivityManager.this.mRestartScanListener, WifiConnectivityManager.this.mEventHandler);
                }
                this.mLowRssiNetworkRetryDelay *= 2;
            }
            WifiConnectivityManager.this.mScanRestartCount = 0;
        }
    }

    private class RestartSingleScanListener implements OnAlarmListener {
        private final boolean mIsFullBandScan;

        RestartSingleScanListener(boolean isFullBandScan) {
            this.mIsFullBandScan = isFullBandScan;
        }

        public void onAlarm() {
            WifiConnectivityManager.this.startSingleScan(this.mIsFullBandScan, WifiStateMachine.WIFI_WORK_SOURCE);
        }
    }

    private class SingleScanListener implements ScanListener {
        private final boolean mIsFullBandScan;

        SingleScanListener(boolean isFullBandScan) {
            this.mIsFullBandScan = isFullBandScan;
        }

        public void onSuccess() {
        }

        public void onFailure(int reason, String description) {
            WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.getScanKey(this), "37", "SingleScanListener onFailure: reason:%s description:%s SingleScanRestartCount:%s", Integer.valueOf(reason), description, Integer.valueOf(WifiConnectivityManager.this.mSingleScanRestartCount));
            if (WifiConnectivityManager.this.mSingleScanRestartCount = WifiConnectivityManager.this.mSingleScanRestartCount + 1 < 5) {
                WifiConnectivityManager.this.scheduleDelayedSingleScan(this.mIsFullBandScan);
                return;
            }
            WifiConnectivityManager.this.mSingleScanRestartCount = 0;
            WifiConnectivityManager.this.localLog("Failed to successfully start single scan for 5 times");
        }

        public void onPeriodChanged(int periodInMs) {
            WifiConnectivityManager.this.localLog(WifiConnectivityManager.this.getScanKey(this), "38", "SingleScanListener onPeriodChanged: actual scan period %s ms", Integer.valueOf(periodInMs));
        }

        public void onResults(ScanData[] results) {
            WifiConnectivityManager.this.mSingleScanStarted = true;
        }

        public void onFullResult(ScanResult fullScanResult) {
            if (WifiConnectivityManager.this.mDbg) {
                WifiConnectivityManager wifiConnectivityManager = WifiConnectivityManager.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SingleScanListener onFullResult: ");
                stringBuilder.append(fullScanResult.SSID);
                stringBuilder.append(" capabilities ");
                stringBuilder.append(fullScanResult.capabilities);
                wifiConnectivityManager.localLog(stringBuilder.toString());
            }
            WifiConnectivityManager.this.mSingleScanRestartCount = 0;
        }
    }

    private class OnSavedNetworkUpdateListener implements com.android.server.wifi.WifiConfigManager.OnSavedNetworkUpdateListener {
        private OnSavedNetworkUpdateListener() {
        }

        /* synthetic */ OnSavedNetworkUpdateListener(WifiConnectivityManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onSavedNetworkAdded(int networkId) {
            updatePnoScan();
        }

        public void onSavedNetworkEnabled(int networkId) {
            updatePnoScan();
        }

        public void onSavedNetworkRemoved(int networkId) {
            updatePnoScan();
        }

        public void onSavedNetworkUpdated(int networkId) {
            WifiConnectivityManager.this.mStateMachine.updateCapabilities();
            updatePnoScan();
        }

        public void onSavedNetworkTemporarilyDisabled(int networkId, int disableReason) {
            if (disableReason != 6) {
                WifiConnectivityManager.this.mConnectivityHelper.removeNetworkIfCurrent(networkId);
            }
        }

        public void onSavedNetworkPermanentlyDisabled(int networkId, int disableReason) {
            WifiConnectivityManager.this.mConnectivityHelper.removeNetworkIfCurrent(networkId);
            updatePnoScan();
        }

        private void updatePnoScan() {
            if (!WifiConnectivityManager.this.mScreenOn) {
                WifiConnectivityManager.this.localLog("Saved networks updated");
                WifiConnectivityManager.this.startConnectivityScan(false, false);
            }
        }
    }

    static {
        boolean z = SystemProperties.get("ro.config.hw_opta", "0").equals("389") && SystemProperties.get("ro.config.hw_optb", "0").equals("840");
        R1 = z;
    }

    private void localLog(String log) {
        this.mLocalLog.log(log);
    }

    private boolean handleScanResults(List<ScanDetail> scanDetails, String listenerName, String keys) {
        refreshBssidBlacklist();
        if (System.getInt(this.mContext.getContentResolver(), "wifi_connect_type", 0) != 0) {
            return false;
        }
        if (this.mStateMachine.isSupplicantTransientState()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(keys);
            stringBuilder.append(listenerName);
            stringBuilder.append(" onResults: No network selection because supplicantTransient is ");
            stringBuilder.append(this.mStateMachine.isSupplicantTransientState());
            Log.i("WifiScanLog", stringBuilder.toString());
            return false;
        }
        localLog(keys, "27", " onResults: start network selection");
        this.mNetworkSelector.mCurrentScanKeys = keys;
        WifiConfiguration candidate = this.mNetworkSelector.selectNetwork(scanDetails, buildBssidBlacklist(), this.mWifiInfo, this.mStateMachine.isConnected(), this.mStateMachine.isDisconnected(), this.mUntrustedConnectionAllowed);
        this.mWifiLastResortWatchdog.updateAvailableNetworks(this.mNetworkSelector.getConnectableScanDetails());
        this.mWifiMetrics.countScanResults(scanDetails);
        if (candidate != null) {
            ScanResult scanResultCandidate = candidate.getNetworkSelectionStatus().getCandidate();
            if (!isWifiScanSpecialChannels() || scanResultCandidate == null || HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(scanResultCandidate.frequency, scanResultCandidate.level) > 2) {
                localLog(keys, "28", "WNS selectNetwork candidate-%s", candidate.SSID);
                connectToNetwork(candidate, keys);
                return true;
            }
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("candidate = ");
            stringBuilder2.append(candidate.configKey());
            stringBuilder2.append(", don't connect to poor network because use specified-channels-scan, rssi = ");
            stringBuilder2.append(scanResultCandidate.level);
            Log.d(str, stringBuilder2.toString());
            return false;
        }
        if (this.mWifiState == 2) {
            if (R1) {
                this.mWifiNetworkNotifier.handleScanResults(this.mNetworkSelector.getFilteredScanDetailsForOpenUnsavedNetworks());
            } else {
                Log.d(TAG, "DO NOT notification for Open unsaved networks.");
            }
            if (this.mCarrierNetworkConfig.isCarrierEncryptionInfoAvailable()) {
                this.mCarrierNetworkNotifier.handleScanResults(this.mNetworkSelector.getFilteredScanDetailsForCarrierUnsavedNetworks(this.mCarrierNetworkConfig));
            }
        }
        return false;
    }

    WifiConnectivityManager(Context context, ScoringParams scoringParams, WifiStateMachine stateMachine, WifiScanner scanner, WifiConfigManager configManager, WifiInfo wifiInfo, WifiNetworkSelector networkSelector, WifiConnectivityHelper connectivityHelper, WifiLastResortWatchdog wifiLastResortWatchdog, OpenNetworkNotifier openNetworkNotifier, CarrierNetworkNotifier carrierNetworkNotifier, CarrierNetworkConfig carrierNetworkConfig, WifiMetrics wifiMetrics, Looper looper, Clock clock, LocalLog localLog, boolean enable, FrameworkFacade frameworkFacade, SavedNetworkEvaluator savedNetworkEvaluator, ScoredNetworkEvaluator scoredNetworkEvaluator, PasspointNetworkEvaluator passpointNetworkEvaluator) {
        Context context2 = context;
        WifiConnectivityHelper wifiConnectivityHelper = connectivityHelper;
        Looper looper2 = looper;
        boolean z = enable;
        this.mContext = context2;
        this.mStateMachine = stateMachine;
        this.mScanner = scanner;
        this.mConfigManager = configManager;
        this.mWifiInfo = wifiInfo;
        this.mNetworkSelector = networkSelector;
        this.mNetworkSelector.mConnectivityHelper = wifiConnectivityHelper;
        this.mConnectivityHelper = wifiConnectivityHelper;
        this.mLocalLog = localLog;
        this.mWifiLastResortWatchdog = wifiLastResortWatchdog;
        this.mOpenNetworkNotifier = openNetworkNotifier;
        this.mCarrierNetworkNotifier = carrierNetworkNotifier;
        this.mCarrierNetworkConfig = carrierNetworkConfig;
        if (R1) {
            this.mWifiNetworkNotifier = new WifiNetworkNotifier(context2, looper2, frameworkFacade);
        } else {
            FrameworkFacade frameworkFacade2 = frameworkFacade;
        }
        this.mWifiMetrics = wifiMetrics;
        this.mAlarmManager = (AlarmManager) context2.getSystemService("alarm");
        this.mEventHandler = new Handler(looper2);
        this.mClock = clock;
        this.mScoringParams = scoringParams;
        this.mConnectionAttemptTimeStamps = new LinkedList();
        this.mMin5GHzRssi = this.mScoringParams.getEntryRssi(ScoringParams.BAND5);
        this.mMin24GHzRssi = this.mScoringParams.getEntryRssi(ScoringParams.BAND2);
        this.mBand5GHzBonus = context.getResources().getInteger(17694884);
        this.mCurrentConnectionBonus = context.getResources().getInteger(17694901);
        this.mSameNetworkBonus = context.getResources().getInteger(17694891);
        this.mSecureBonus = context.getResources().getInteger(17694892);
        this.mEnableAutoJoinWhenAssociated = context.getResources().getBoolean(17957080);
        this.mUseSingleRadioChainScanResults = context.getResources().getBoolean(17957082);
        this.mInitialScoreMax = (Math.max(this.mScoringParams.getGoodRssi(ScoringParams.BAND2), this.mScoringParams.getGoodRssi(ScoringParams.BAND5)) + context.getResources().getInteger(17694889)) * context.getResources().getInteger(17694890);
        this.mFullScanMaxTxRate = context.getResources().getInteger(17694904);
        this.mFullScanMaxRxRate = context.getResources().getInteger(17694903);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PNO settings: min5GHzRssi ");
        stringBuilder.append(this.mMin5GHzRssi);
        stringBuilder.append(" min24GHzRssi ");
        stringBuilder.append(this.mMin24GHzRssi);
        stringBuilder.append(" currentConnectionBonus ");
        stringBuilder.append(this.mCurrentConnectionBonus);
        stringBuilder.append(" sameNetworkBonus ");
        stringBuilder.append(this.mSameNetworkBonus);
        stringBuilder.append(" secureNetworkBonus ");
        stringBuilder.append(this.mSecureBonus);
        stringBuilder.append(" initialScoreMax ");
        stringBuilder.append(this.mInitialScoreMax);
        localLog(stringBuilder.toString());
        boolean hs2Enabled = context.getPackageManager().hasSystemFeature("android.hardware.wifi.passpoint");
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Passpoint is: ");
        stringBuilder2.append(hs2Enabled ? "enabled" : "disabled");
        localLog(stringBuilder2.toString());
        this.mNetworkSelector.registerNetworkEvaluator(savedNetworkEvaluator, 1);
        if (hs2Enabled) {
            boolean z2 = hs2Enabled;
            this.mNetworkSelector.registerNetworkEvaluator(passpointNetworkEvaluator, 2);
        } else {
            hs2Enabled = passpointNetworkEvaluator;
        }
        this.mNetworkSelector.registerNetworkEvaluator(scoredNetworkEvaluator, 3);
        this.mScanner.registerScanListener(this.mAllSingleScanListener);
        this.mConfigManager.setOnSavedNetworkUpdateListener(new OnSavedNetworkUpdateListener(this, null));
        this.mWifiConnectivityManagerEnabled = z;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("ConnectivityScanManager initialized and ");
        stringBuilder3.append(z ? "enabled" : "disabled");
        localLog(stringBuilder3.toString());
        this.mHwWifiCHRService = HwWifiServiceFactory.getHwWifiCHRService();
    }

    private boolean shouldSkipConnectionAttempt(Long timeMillis) {
        Iterator<Long> attemptIter = this.mConnectionAttemptTimeStamps.iterator();
        while (attemptIter.hasNext()) {
            if (timeMillis.longValue() - ((Long) attemptIter.next()).longValue() <= 240000) {
                break;
            }
            attemptIter.remove();
        }
        return this.mConnectionAttemptTimeStamps.size() >= 6;
    }

    private void noteConnectionAttempt(Long timeMillis) {
        this.mConnectionAttemptTimeStamps.addLast(timeMillis);
    }

    private void clearConnectionAttemptTimeStamps() {
        this.mConnectionAttemptTimeStamps.clear();
    }

    private void connectToNetwork(WifiConfiguration candidate, String keys) {
        ScanResult scanResultCandidate = candidate.getNetworkSelectionStatus().getCandidate();
        if (scanResultCandidate == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(keys);
            stringBuilder.append("connectToNetwork: bad candidate - ");
            stringBuilder.append(candidate);
            stringBuilder.append(" scanResult: ");
            stringBuilder.append(scanResultCandidate);
            Log.i("WifiScanLog", stringBuilder.toString());
            return;
        }
        String targetBssid = scanResultCandidate.BSSID;
        String targetAssociationId = new StringBuilder();
        targetAssociationId.append(candidate.SSID);
        targetAssociationId.append(" : ");
        targetAssociationId.append(targetBssid);
        targetAssociationId = targetAssociationId.toString();
        StringBuilder stringBuilder2;
        if (targetBssid != null && ((targetBssid.equals(this.mLastConnectionAttemptBssid) || targetBssid.equals(this.mWifiInfo.getBSSID())) && SupplicantState.isConnecting(this.mWifiInfo.getSupplicantState()))) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(keys);
            stringBuilder2.append("connectToNetwork: Either already connected or is connecting to ");
            stringBuilder2.append(targetAssociationId);
            Log.i("WifiScanLog", stringBuilder2.toString());
        } else if (candidate.BSSID == null || candidate.BSSID.equals("any") || candidate.BSSID.equals(targetBssid)) {
            long elapsedTimeMillis = this.mClock.getElapsedSinceBootMillis();
            if (this.mScreenOn || !shouldSkipConnectionAttempt(Long.valueOf(elapsedTimeMillis))) {
                String currentAssociationId;
                noteConnectionAttempt(Long.valueOf(elapsedTimeMillis));
                this.mLastConnectionAttemptBssid = targetBssid;
                WifiConfiguration currentConnectedNetwork = this.mConfigManager.getConfiguredNetwork(this.mWifiInfo.getNetworkId());
                if (currentConnectedNetwork == null) {
                    currentAssociationId = "Disconnected";
                } else {
                    currentAssociationId = new StringBuilder();
                    currentAssociationId.append(this.mWifiInfo.getSSID());
                    currentAssociationId.append(" : ");
                    currentAssociationId.append(this.mWifiInfo.getBSSID());
                    currentAssociationId = currentAssociationId.toString();
                }
                this.mStateMachine.setCHRConnectingSartTimestamp(elapsedTimeMillis);
                StringBuilder stringBuilder3;
                if (currentConnectedNetwork == null || currentConnectedNetwork.networkId != candidate.networkId) {
                    Bundle bundle = new HwDevicePolicyManagerEx().getPolicy(null, POLICY_AUTO_CONNECT);
                    if (bundle == null || !bundle.getBoolean(VALUE_DISABLE)) {
                        if (this.mConnectivityHelper.isFirmwareRoamingSupported() && (candidate.BSSID == null || candidate.BSSID.equals("any"))) {
                            targetBssid = "any";
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("connectToNetwork: Connect to ");
                            stringBuilder3.append(candidate.SSID);
                            stringBuilder3.append(":");
                            stringBuilder3.append(targetBssid);
                            stringBuilder3.append(" from ");
                            stringBuilder3.append(currentAssociationId);
                            localLog(stringBuilder3.toString());
                        } else {
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("connectToNetwork: Connect to ");
                            stringBuilder3.append(targetAssociationId);
                            stringBuilder3.append(" from ");
                            stringBuilder3.append(currentAssociationId);
                            localLog(stringBuilder3.toString());
                        }
                        if (this.mStateMachine.isScanAndManualConnectMode()) {
                            Log.d(TAG, "Only allow Manual Connection, ignore auto connection.");
                            return;
                        }
                        localLog(keys, "46", "WifiStateMachine startConnectToNetwork");
                        if (this.mHwWifiCHRService != null) {
                            this.mHwWifiCHRService.updateConnectType("AUTO_CONNECT");
                        }
                        this.mStateMachine.startConnectToNetwork(candidate.networkId, 1010, unselectDhcpFailedBssid(targetBssid, scanResultCandidate.BSSID, candidate));
                    } else {
                        Log.w(TAG, "connectToNetwork: MDM deny auto connect!");
                        return;
                    }
                } else if (this.mConnectivityHelper.isFirmwareRoamingSupported()) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(keys);
                    stringBuilder3.append("connectToNetwork: Roaming candidate - ");
                    stringBuilder3.append(targetAssociationId);
                    stringBuilder3.append(". The actual roaming target is up to the firmware.");
                    Log.i("WifiScanLog", stringBuilder3.toString());
                } else if (this.mStateMachine.isWifiRepeaterStarted()) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(keys);
                    stringBuilder3.append("WifiRepeater is started, do not allow auto roam.");
                    Log.i("WifiScanLog", stringBuilder3.toString());
                } else {
                    localLog(keys, "45", "connectToNetwork: Roaming to %s  from %s!", targetAssociationId, currentAssociationId);
                    this.mStateMachine.startRoamToNetwork(candidate.networkId, scanResultCandidate);
                }
                return;
            }
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append(keys);
            stringBuilder4.append("connectToNetwork: Too many connection attempts. Skipping this attempt!");
            Log.i("WifiScanLog", stringBuilder4.toString());
            this.mTotalConnectivityAttemptsRateLimited++;
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(keys);
            stringBuilder2.append("connecToNetwork: target BSSID ");
            stringBuilder2.append(targetBssid);
            stringBuilder2.append(" does not match the config specified BSSID ");
            stringBuilder2.append(candidate.BSSID);
            stringBuilder2.append(". Drop it!");
            Log.i("WifiScanLog", stringBuilder2.toString());
        }
    }

    private int getScanBand() {
        return getScanBand(true);
    }

    private int getScanBand(boolean isFullBandScan) {
        if (isFullBandScan) {
            return 7;
        }
        return 0;
    }

    private boolean setScanChannels(ScanSettings settings) {
        WifiConfiguration config = this.mStateMachine.getCurrentWifiConfiguration();
        if (config == null) {
            return false;
        }
        Set<Integer> freqs = this.mConfigManager.fetchChannelSetForNetworkForPartialScan(config.networkId, 3600000, this.mWifiInfo.getFrequency());
        if (freqs == null || freqs.size() == 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No scan channels for ");
            stringBuilder.append(config.configKey());
            stringBuilder.append(". Perform full band scan");
            localLog(stringBuilder.toString());
            return false;
        }
        int index = 0;
        settings.channels = new ChannelSpec[freqs.size()];
        for (Integer freq : freqs) {
            int index2 = index + 1;
            settings.channels[index] = new ChannelSpec(freq.intValue());
            index = index2;
        }
        return true;
    }

    private void watchdogHandler() {
        localLog("watchdogHandler");
        String str = "WifiScanLog";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("start a single scan from watchdogHandler ? ");
        stringBuilder.append(this.mWifiState == 2);
        Log.i(str, stringBuilder.toString());
        if (this.mWifiState == 2) {
            localLog("start a single scan from watchdogHandler");
            scheduleWatchdogTimer();
            startSingleScan(true, WifiStateMachine.WIFI_WORK_SOURCE);
        }
    }

    private void startPeriodicSingleScan() {
        long currentTimeStamp = this.mClock.getElapsedSinceBootMillis();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("****start Periodic SingleScan,mPeriodicSingleScanInterval : ");
        stringBuilder.append(this.mPeriodicSingleScanInterval / 1000);
        stringBuilder.append(" s");
        localLog(stringBuilder.toString());
        if (!(this.mLastPeriodicSingleScanTimeStamp == RESET_TIME_STAMP || handleForceScan())) {
            long msSinceLastScan = currentTimeStamp - this.mLastPeriodicSingleScanTimeStamp;
            int mPeriodicScanInterval = PERIODIC_SCAN_INTERVAL_MS;
            if (isSupportWifiScanGenie()) {
                mPeriodicScanInterval = 10000;
            }
            if (msSinceLastScan < ((long) mPeriodicScanInterval)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Last periodic single scan started ");
                stringBuilder2.append(msSinceLastScan);
                stringBuilder2.append("ms ago, defer this new scan request.");
                localLog(stringBuilder2.toString());
                schedulePeriodicScanTimer(mPeriodicScanInterval - ((int) msSinceLastScan));
                return;
            }
        }
        boolean isScanNeeded = true;
        boolean isFullBandScan = true;
        boolean isTrafficOverThreshold = this.mWifiInfo.txSuccessRate > ((double) this.mFullScanMaxTxRate) || this.mWifiInfo.rxSuccessRate > ((double) this.mFullScanMaxRxRate);
        if (this.mWifiState == 1 && isTrafficOverThreshold) {
            if (this.mConnectivityHelper.isFirmwareRoamingSupported()) {
                localLog("No partial scan because firmware roaming is supported.");
                isScanNeeded = false;
            } else {
                localLog("No full band scan due to ongoing traffic");
                isFullBandScan = false;
            }
        }
        if (isScanNeeded && isScanThisPeriod(this.mStateMachine.isP2pConnected())) {
            this.mLastPeriodicSingleScanTimeStamp = currentTimeStamp;
            handleScanCountChanged(0);
            startSingleScan(isFullBandScan, WifiStateMachine.WIFI_WORK_SOURCE);
        }
        this.mPeriodicSingleScanInterval = getPeriodicSingleScanInterval();
        schedulePeriodicScanTimer(this.mPeriodicSingleScanInterval);
    }

    private void resetLastPeriodicSingleScanTimeStamp() {
        this.mLastPeriodicSingleScanTimeStamp = RESET_TIME_STAMP;
    }

    private void periodicScanTimerHandler() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("periodicScanTimerHandler mScreenOn ");
        stringBuilder.append(this.mScreenOn);
        Log.i(str, stringBuilder.toString());
        localLog("periodicScanTimerHandler");
        if (this.mScreenOn) {
            startPeriodicSingleScan();
        }
    }

    private void startHourPeriodicSingleScan() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startHourPeriodicSingleScan: screenOn=");
        stringBuilder.append(this.mScreenOn);
        stringBuilder.append(" wifiEnabled=");
        stringBuilder.append(this.mWifiEnabled);
        stringBuilder.append(" wifiConnectivityManagerEnabled=");
        stringBuilder.append(this.mWifiConnectivityManagerEnabled);
        localLog(stringBuilder.toString());
        if (this.mScreenOn && this.mWifiEnabled && !this.mWifiConnectivityManagerEnabled) {
            long currentTimeStamp = this.mClock.getElapsedSinceBootMillis();
            if (this.mLastHourPeriodicSingleScanTimeStamp != RESET_TIME_STAMP) {
                long msSinceLastScan = currentTimeStamp - this.mLastHourPeriodicSingleScanTimeStamp;
                if (msSinceLastScan < 3600000) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Last hour periodic single scan started ");
                    stringBuilder2.append(msSinceLastScan);
                    stringBuilder2.append("ms ago, defer this new scan request.");
                    localLog(stringBuilder2.toString());
                    scheduleHourPeriodicScanTimer(3600000 - ((int) msSinceLastScan));
                    return;
                }
            }
            startScan(true, WifiStateMachine.WIFI_WORK_SOURCE);
            this.mLastHourPeriodicSingleScanTimeStamp = currentTimeStamp;
            scheduleHourPeriodicScanTimer(3600000);
        }
    }

    private void scheduleHourPeriodicScanTimer(int intervalMs) {
        this.mAlarmManager.setExact(3, ((long) intervalMs) + this.mClock.getElapsedSinceBootMillis(), HOUR_PERIODIC_SCAN_TIMER_TAG, this.mHourPeriodicScanTimerListener, this.mEventHandler);
        this.mHourPeriodicScanTimerSet = true;
    }

    private void stopHourPeriodicSingleScan() {
        if (this.mHourPeriodicScanTimerSet) {
            this.mAlarmManager.cancel(this.mHourPeriodicScanTimerListener);
            this.mHourPeriodicScanTimerSet = false;
        }
    }

    private void startSingleScan(boolean isFullBandScan, WorkSource workSource) {
        if (this.mWifiEnabled && this.mWifiConnectivityManagerEnabled) {
            startScan(isFullBandScan, workSource);
        }
    }

    private void startScan(boolean isFullBandScan, WorkSource workSource) {
        this.mPnoScanListener.resetLowRssiNetworkRetryDelay();
        ScanSettings settings = null;
        if (!isWifiScanSpecialChannels() || this.mWifiState == 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isWifiScanSpecialChannels is false,mWifiState : ");
            stringBuilder.append(this.mWifiState);
            Log.e(str, stringBuilder.toString());
        } else {
            settings = getScanGenieSettings();
            if (settings != null) {
                isFullBandScan = false;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("****isWifiScanSpecialChannels *settings =**:");
            stringBuilder2.append(settings);
            localLog(stringBuilder2.toString());
        }
        if (settings == null) {
            settings = new ScanSettings();
            if (!(isFullBandScan || setScanChannels(settings))) {
                isFullBandScan = true;
            }
        }
        settings.type = 2;
        settings.band = getScanBand(isFullBandScan);
        settings.reportEvents = 3;
        int i = 0;
        settings.numBssidsPerScan = 0;
        List<HiddenNetwork> hiddenNetworkList = this.mConfigManager.retrieveHiddenNetworkList();
        settings.hiddenNetworks = (HiddenNetwork[]) hiddenNetworkList.toArray(new HiddenNetwork[hiddenNetworkList.size()]);
        SingleScanListener singleScanListener = new SingleScanListener(isFullBandScan);
        if (settings.channels != null) {
            while (i < settings.channels.length) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("settings  channels frequency: ");
                stringBuilder3.append(settings.channels[i].frequency);
                stringBuilder3.append(", dwellTimeMS: ");
                stringBuilder3.append(settings.channels[i].dwellTimeMS);
                stringBuilder3.append(", passive :");
                stringBuilder3.append(settings.channels[i].passive);
                localLog(stringBuilder3.toString());
                i++;
            }
        }
        this.mScanner.startScan(settings, singleScanListener, workSource);
        this.mWifiMetrics.incrementConnectivityOneshotScanCount();
    }

    private void startPeriodicScan(boolean scanImmediately) {
        this.mPnoScanListener.resetLowRssiNetworkRetryDelay();
        if (this.mWifiState != 1 || this.mEnableAutoJoinWhenAssociated) {
            if (scanImmediately) {
                resetLastPeriodicSingleScanTimeStamp();
            }
            this.mPeriodicSingleScanInterval = PERIODIC_SCAN_INTERVAL_MS;
            resetPeriodicSingleScanInterval();
            startPeriodicSingleScan();
        }
    }

    private void startDisconnectedPnoScan() {
        PnoSettings pnoSettings = new PnoSettings();
        List<PnoNetwork> pnoNetworkList = this.mConfigManager.retrievePnoNetworkList();
        int listSize = pnoNetworkList.size();
        if (listSize == 0) {
            localLog("No saved network for starting disconnected PNO.");
            return;
        }
        pnoSettings.networkList = new PnoNetwork[listSize];
        pnoSettings.networkList = (PnoNetwork[]) pnoNetworkList.toArray(pnoSettings.networkList);
        pnoSettings.min5GHzRssi = this.mMin5GHzRssi;
        pnoSettings.min24GHzRssi = this.mMin24GHzRssi;
        pnoSettings.initialScoreMax = this.mInitialScoreMax;
        pnoSettings.currentConnectionBonus = this.mCurrentConnectionBonus;
        pnoSettings.sameNetworkBonus = this.mSameNetworkBonus;
        pnoSettings.secureBonus = this.mSecureBonus;
        pnoSettings.band5GHzBonus = this.mBand5GHzBonus;
        ScanSettings scanSettings = new ScanSettings();
        scanSettings.band = getScanBand();
        scanSettings.reportEvents = 4;
        scanSettings.numBssidsPerScan = 0;
        scanSettings.periodInMs = PERIODIC_SCAN_INTERVAL_MS;
        this.mPnoScanListener.clearScanDetails();
        this.mScanner.startDisconnectedPnoScan(scanSettings, pnoSettings, this.mPnoScanListener);
        this.mPnoScanStarted = true;
    }

    private void stopPnoScan() {
        if (this.mPnoScanStarted) {
            this.mScanner.stopPnoScan(this.mPnoScanListener);
        }
        this.mPnoScanStarted = false;
    }

    private void scheduleWatchdogTimer() {
        localLog("scheduleWatchdogTimer");
        long elapsedSinceBootMillis = 1680000 + this.mClock.getElapsedSinceBootMillis();
        this.mAlarmManager.set(2, elapsedSinceBootMillis, WATCHDOG_TIMER_TAG, this.mWatchdogListener, this.mEventHandler);
    }

    private void schedulePeriodicScanTimer(int intervalMs) {
        this.mAlarmManager.setExact(2, ((long) intervalMs) + this.mClock.getElapsedSinceBootMillis(), PERIODIC_SCAN_TIMER_TAG, this.mPeriodicScanTimerListener, this.mEventHandler);
        this.mPeriodicScanTimerSet = true;
    }

    private void cancelPeriodicScanTimer() {
        if (this.mPeriodicScanTimerSet) {
            this.mAlarmManager.cancel(this.mPeriodicScanTimerListener);
            this.mPeriodicScanTimerSet = false;
        }
    }

    private void scheduleDelayedSingleScan(boolean isFullBandScan) {
        localLog("scheduleDelayedSingleScan");
        RestartSingleScanListener restartSingleScanListener = new RestartSingleScanListener(isFullBandScan);
        long elapsedSinceBootMillis = 2000 + this.mClock.getElapsedSinceBootMillis();
        this.mAlarmManager.set(2, elapsedSinceBootMillis, RESTART_SINGLE_SCAN_TIMER_TAG, restartSingleScanListener, this.mEventHandler);
    }

    private void scheduleDelayedConnectivityScan(int msFromNow) {
        localLog("scheduleDelayedConnectivityScan");
        long elapsedSinceBootMillis = ((long) msFromNow) + this.mClock.getElapsedSinceBootMillis();
        this.mAlarmManager.set(2, elapsedSinceBootMillis, RESTART_CONNECTIVITY_SCAN_TIMER_TAG, this.mRestartScanListener, this.mEventHandler);
    }

    protected void startConnectivityScan(boolean scanImmediately, boolean isRestartScan) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startConnectivityScan: screenOn=");
        stringBuilder.append(this.mScreenOn);
        stringBuilder.append(" wifiState=");
        stringBuilder.append(stateToString(this.mWifiState));
        stringBuilder.append(" scanImmediately=");
        stringBuilder.append(scanImmediately);
        stringBuilder.append(" wifiEnabled=");
        stringBuilder.append(this.mWifiEnabled);
        stringBuilder.append(" wifiConnectivityManagerEnabled=");
        stringBuilder.append(this.mWifiConnectivityManagerEnabled);
        localLog(stringBuilder.toString());
        if (this.mWifiEnabled && this.mWifiConnectivityManagerEnabled) {
            stopConnectivityScan(isRestartScan);
            if (this.mWifiState == 1 || this.mWifiState == 2) {
                if (this.mScreenOn) {
                    startPeriodicScan(scanImmediately);
                } else if (this.mWifiState == 2 && !this.mPnoScanStarted) {
                    startDisconnectedPnoScan();
                }
            }
        }
    }

    private void stopConnectivityScan(boolean isRestartScan) {
        cancelPeriodicScanTimer();
        stopPnoScan();
        if (!isRestartScan) {
            this.mScanRestartCount = 0;
        }
    }

    public void handleScreenStateChanged(boolean screenOn) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleScreenStateChanged: screenOn=");
        stringBuilder.append(screenOn);
        localLog(stringBuilder.toString());
        this.mScreenOn = screenOn;
        if (R1) {
            this.mWifiNetworkNotifier.handleScreenStateChanged(screenOn);
        } else {
            this.mOpenNetworkNotifier.handleScreenStateChanged(screenOn);
        }
        this.mCarrierNetworkNotifier.handleScreenStateChanged(screenOn);
        startConnectivityScan(false, false);
        if (screenOn) {
            startHourPeriodicSingleScan();
        } else {
            stopHourPeriodicSingleScan();
        }
    }

    private static String stateToString(int state) {
        switch (state) {
            case 1:
                return "connected";
            case 2:
                return "disconnected";
            case 3:
                return "transitioning";
            default:
                return "unknown";
        }
    }

    public void handleConnectionStateChanged(int state) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleConnectionStateChanged: state=");
        stringBuilder.append(stateToString(state));
        localLog(stringBuilder.toString());
        this.mWifiState = state;
        if (this.mWifiState == 1) {
            if (R1) {
                this.mWifiNetworkNotifier.clearPendingNotification();
            } else {
                this.mOpenNetworkNotifier.handleWifiConnected();
            }
            this.mCarrierNetworkNotifier.handleWifiConnected();
        }
        handleScanCountChanged(2);
        if (this.mWifiState == 2) {
            this.mLastConnectionAttemptBssid = null;
            scheduleWatchdogTimer();
            startConnectivityScan(true, false);
            return;
        }
        if (this.mWifiState == 1) {
            this.mPnoScanRestartCount = 0;
        }
        startConnectivityScan(false, false);
    }

    public void handleConnectionAttemptEnded(int failureCode) {
        if (failureCode != 1) {
            this.mOpenNetworkNotifier.handleConnectionFailure();
            this.mCarrierNetworkNotifier.handleConnectionFailure();
        }
    }

    public void setUntrustedConnectionAllowed(boolean allowed) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setUntrustedConnectionAllowed: allowed=");
        stringBuilder.append(allowed);
        localLog(stringBuilder.toString());
        if (this.mUntrustedConnectionAllowed != allowed) {
            this.mUntrustedConnectionAllowed = allowed;
            startConnectivityScan(true, false);
        }
    }

    public void setUserConnectChoice(int netId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setUserConnectChoice: netId=");
        stringBuilder.append(netId);
        localLog(stringBuilder.toString());
        this.mNetworkSelector.setUserConnectChoice(netId);
    }

    public void prepareForForcedConnection(int netId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("prepareForForcedConnection: netId=");
        stringBuilder.append(netId);
        localLog(stringBuilder.toString());
        clearConnectionAttemptTimeStamps();
        clearBssidBlacklist();
    }

    public void forceConnectivityScan(WorkSource workSource) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("forceConnectivityScan in request of ");
        stringBuilder.append(workSource);
        localLog(stringBuilder.toString());
        this.mWaitForFullBandScanResults = true;
        startSingleScan(true, workSource);
    }

    private boolean updateBssidBlacklist(String bssid, boolean enable, int reasonCode) {
        boolean z = false;
        if (enable) {
            if (this.mBssidBlacklist.remove(bssid) != null) {
                z = true;
            }
            return z;
        } else if (!this.mStateMachine.isConnected() || getScanResultsHasSameSsid(bssid).size() < 2) {
            return false;
        } else {
            BssidBlacklistStatus status = (BssidBlacklistStatus) this.mBssidBlacklist.get(bssid);
            if (status == null) {
                status = new BssidBlacklistStatus();
                this.mBssidBlacklist.put(bssid, status);
            }
            status.blacklistedTimeStamp = this.mClock.getElapsedSinceBootMillis();
            status.counter++;
            if (status.isBlacklisted || (status.counter < 3 && reasonCode != 17)) {
                return false;
            }
            status.isBlacklisted = true;
            return true;
        }
    }

    public boolean trackBssid(String bssid, boolean enable, int reasonCode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("trackBssid: ");
        stringBuilder.append(enable ? "enable " : "disable ");
        stringBuilder.append(StringUtil.safeDisplayBssid(bssid));
        stringBuilder.append(" reason code ");
        stringBuilder.append(reasonCode);
        localLog(stringBuilder.toString());
        if (bssid == null || !updateBssidBlacklist(bssid, enable, reasonCode)) {
            return false;
        }
        updateFirmwareRoamingConfiguration();
        if (!enable) {
            startConnectivityScan(true, false);
        }
        return true;
    }

    @VisibleForTesting
    public boolean isBssidDisabled(String bssid) {
        BssidBlacklistStatus status = (BssidBlacklistStatus) this.mBssidBlacklist.get(bssid);
        return status == null ? false : status.isBlacklisted;
    }

    private HashSet<String> buildBssidBlacklist() {
        HashSet<String> blacklistedBssids = new HashSet();
        for (String bssid : this.mBssidBlacklist.keySet()) {
            if (isBssidDisabled(bssid)) {
                blacklistedBssids.add(bssid);
            }
        }
        return blacklistedBssids;
    }

    private void updateFirmwareRoamingConfiguration() {
        if (this.mConnectivityHelper.isFirmwareRoamingSupported()) {
            int maxBlacklistSize = this.mConnectivityHelper.getMaxNumBlacklistBssid();
            if (maxBlacklistSize <= 0) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid max BSSID blacklist size:  ");
                stringBuilder.append(maxBlacklistSize);
                Log.wtf(str, stringBuilder.toString());
                return;
            }
            ArrayList<String> blacklistedBssids = new ArrayList(buildBssidBlacklist());
            int blacklistSize = blacklistedBssids.size();
            if (blacklistSize > maxBlacklistSize) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Attempt to write ");
                stringBuilder2.append(blacklistSize);
                stringBuilder2.append(" blacklisted BSSIDs, max size is ");
                stringBuilder2.append(maxBlacklistSize);
                Log.wtf(str2, stringBuilder2.toString());
                blacklistedBssids = new ArrayList(blacklistedBssids.subList(0, maxBlacklistSize));
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Trim down BSSID blacklist size from ");
                stringBuilder3.append(blacklistSize);
                stringBuilder3.append(" to ");
                stringBuilder3.append(blacklistedBssids.size());
                localLog(stringBuilder3.toString());
            }
            if (!this.mConnectivityHelper.setFirmwareRoamingConfiguration(blacklistedBssids, new ArrayList())) {
                localLog("Failed to set firmware roaming configuration.");
            }
        }
    }

    private void refreshBssidBlacklist() {
        if (!this.mBssidBlacklist.isEmpty()) {
            boolean updated = false;
            Iterator<BssidBlacklistStatus> iter = this.mBssidBlacklist.values().iterator();
            Long currentTimeStamp = Long.valueOf(this.mClock.getElapsedSinceBootMillis());
            while (iter.hasNext()) {
                BssidBlacklistStatus status = (BssidBlacklistStatus) iter.next();
                if (status.isBlacklisted && currentTimeStamp.longValue() - status.blacklistedTimeStamp >= 300000) {
                    iter.remove();
                    updated = true;
                }
            }
            if (updated) {
                updateFirmwareRoamingConfiguration();
            }
        }
    }

    private void clearBssidBlacklist() {
        this.mBssidBlacklist.clear();
        updateFirmwareRoamingConfiguration();
    }

    private List<ScanResult> getScanResultsHasSameSsid(String bssid) {
        if (TextUtils.isEmpty(bssid)) {
            Log.d(TAG, "getScanResultsHasSameSsid: bssid is empty.");
            return new ArrayList();
        }
        List<ScanResult> scanResults = null;
        if (this.mScanner != null) {
            scanResults = this.mScanner.getSingleScanResults();
        }
        if (scanResults.isEmpty()) {
            Log.d(TAG, "getScanResultsHasSameSsid: WifiStateMachine.ScanResultsList is empty.");
            return scanResults;
        }
        String ssid = null;
        for (ScanResult result : scanResults) {
            if (bssid.equals(result.BSSID)) {
                ssid = result.SSID;
                break;
            }
        }
        if (TextUtils.isEmpty(ssid)) {
            Log.d(TAG, "getScanResultsHasSameSsid: can't find the corresponding ssid with the given bssid.");
            return scanResults;
        }
        List<ScanResult> sameSsidList = new ArrayList();
        for (ScanResult result2 : scanResults) {
            if (ssid.equals(result2.SSID)) {
                sameSsidList.add(result2);
            }
        }
        return sameSsidList;
    }

    private void start() {
        this.mConnectivityHelper.getFirmwareRoamingInfo();
        clearBssidBlacklist();
        startConnectivityScan(true, false);
    }

    private void stop() {
        stopConnectivityScan(false);
        clearBssidBlacklist();
        resetLastPeriodicSingleScanTimeStamp();
        if (R1) {
            this.mWifiNetworkNotifier.clearPendingNotification();
        } else {
            this.mOpenNetworkNotifier.clearPendingNotification(true);
        }
        this.mCarrierNetworkNotifier.clearPendingNotification(true);
        this.mLastConnectionAttemptBssid = null;
        this.mWaitForFullBandScanResults = false;
    }

    private void updateRunningState() {
        if (this.mWifiEnabled && this.mWifiConnectivityManagerEnabled) {
            localLog("Starting up WifiConnectivityManager");
            start();
            return;
        }
        localLog("Stopping WifiConnectivityManager");
        stop();
    }

    public void setWifiEnabled(boolean enable) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Set WiFi ");
        stringBuilder.append(enable ? "enabled" : "disabled");
        localLog(stringBuilder.toString());
        this.mWifiEnabled = enable;
        if (!this.mWifiEnabled) {
            this.mOldSsidList.clear();
        }
        updateRunningState();
    }

    public boolean isWifiConnectivityManagerEnabled() {
        return this.mWifiConnectivityManagerEnabled;
    }

    public void enable(boolean enable) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Set WiFiConnectivityManager ");
        stringBuilder.append(enable ? "enabled" : "disabled");
        localLog(stringBuilder.toString());
        this.mWifiConnectivityManagerEnabled = enable;
        updateRunningState();
        if (enable) {
            stopHourPeriodicSingleScan();
        } else {
            startHourPeriodicSingleScan();
        }
    }

    @VisibleForTesting
    int getLowRssiNetworkRetryDelay() {
        return this.mPnoScanListener.getLowRssiNetworkRetryDelay();
    }

    @VisibleForTesting
    long getLastPeriodicSingleScanTimeStamp() {
        return this.mLastPeriodicSingleScanTimeStamp;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of WifiConnectivityManager");
        pw.println("WifiConnectivityManager - Log Begin ----");
        this.mLocalLog.dump(fd, pw, args);
        pw.println("WifiConnectivityManager - Log End ----");
        this.mOpenNetworkNotifier.dump(fd, pw, args);
        this.mCarrierNetworkNotifier.dump(fd, pw, args);
    }

    private String getScanKey(ScanListener scanListener) {
        int key = this.mScanner.getScanKey(this);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Key#");
        stringBuilder.append(key);
        stringBuilder.append(":");
        return stringBuilder.toString();
    }

    void localLog(String scanKey, String eventKey, String log) {
        localLog(scanKey, eventKey, log, null);
    }

    void localLog(String scanKey, String eventKey, String log, Object... params) {
        if (!"Key#0:".equals(scanKey)) {
            WifiConnectivityHelper.localLog(this.mLocalLog, scanKey, eventKey, log, params);
        }
    }
}
