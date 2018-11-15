package com.android.server.wifi;

import android.content.Context;
import android.database.ContentObserver;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiScanner.ScanData;
import android.net.wifi.WifiScanner.ScanListener;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings.Global;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WakeupConfigStoreData.DataSource;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WakeupController {
    private static final String TAG = "WakeupController";
    private static final boolean USE_PLATFORM_WIFI_WAKE = true;
    private final ContentObserver mContentObserver;
    private final Context mContext;
    private final FrameworkFacade mFrameworkFacade;
    private final Handler mHandler;
    private boolean mIsActive = false;
    private int mNumScansHandled = 0;
    private final ScanListener mScanListener = new ScanListener() {
        public void onPeriodChanged(int periodInMs) {
        }

        public void onResults(ScanData[] results) {
            if (results.length == 1 && results[0].isAllChannelsScanned()) {
                WakeupController.this.handleScanResults(WakeupController.this.filterDfsScanResults(Arrays.asList(results[0].getResults())));
            }
        }

        public void onFullResult(ScanResult fullScanResult) {
        }

        public void onSuccess() {
        }

        public void onFailure(int reason, String description) {
            String str = WakeupController.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ScanListener onFailure: ");
            stringBuilder.append(reason);
            stringBuilder.append(": ");
            stringBuilder.append(description);
            Log.e(str, stringBuilder.toString());
        }
    };
    private boolean mVerboseLoggingEnabled;
    private final WakeupConfigStoreData mWakeupConfigStoreData;
    private final WakeupEvaluator mWakeupEvaluator;
    private final WakeupLock mWakeupLock;
    private final WakeupOnboarding mWakeupOnboarding;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiInjector mWifiInjector;
    private final WifiWakeMetrics mWifiWakeMetrics;
    private boolean mWifiWakeupEnabled;

    private class IsActiveDataSource implements DataSource<Boolean> {
        private IsActiveDataSource() {
        }

        /* synthetic */ IsActiveDataSource(WakeupController x0, AnonymousClass1 x1) {
            this();
        }

        public Boolean getData() {
            return Boolean.valueOf(WakeupController.this.mIsActive);
        }

        public void setData(Boolean data) {
            WakeupController.this.mIsActive = data.booleanValue();
        }
    }

    public WakeupController(Context context, Looper looper, WakeupLock wakeupLock, WakeupEvaluator wakeupEvaluator, WakeupOnboarding wakeupOnboarding, WifiConfigManager wifiConfigManager, WifiConfigStore wifiConfigStore, WifiWakeMetrics wifiWakeMetrics, WifiInjector wifiInjector, FrameworkFacade frameworkFacade) {
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.mWakeupLock = wakeupLock;
        this.mWakeupEvaluator = wakeupEvaluator;
        this.mWakeupOnboarding = wakeupOnboarding;
        this.mWifiConfigManager = wifiConfigManager;
        this.mWifiWakeMetrics = wifiWakeMetrics;
        this.mFrameworkFacade = frameworkFacade;
        this.mWifiInjector = wifiInjector;
        this.mContentObserver = new ContentObserver(this.mHandler) {
            public void onChange(boolean selfChange) {
                WakeupController.this.readWifiWakeupEnabledFromSettings();
                WakeupController.this.mWakeupOnboarding.setOnboarded();
            }
        };
        this.mFrameworkFacade.registerContentObserver(this.mContext, Global.getUriFor("wifi_wakeup_enabled"), true, this.mContentObserver);
        readWifiWakeupEnabledFromSettings();
        this.mWakeupConfigStoreData = new WakeupConfigStoreData(new IsActiveDataSource(this, null), this.mWakeupOnboarding.getIsOnboadedDataSource(), this.mWakeupOnboarding.getNotificationsDataSource(), this.mWakeupLock.getDataSource());
        wifiConfigStore.registerStoreData(this.mWakeupConfigStoreData);
    }

    private void readWifiWakeupEnabledFromSettings() {
        boolean z = true;
        if (this.mFrameworkFacade.getIntegerSetting(this.mContext, "wifi_wakeup_enabled", 0) != 1) {
            z = false;
        }
        this.mWifiWakeupEnabled = z;
        this.mWifiWakeupEnabled = false;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WifiWake ");
        stringBuilder.append(this.mWifiWakeupEnabled ? "enabled" : "disabled");
        Log.d(str, stringBuilder.toString());
    }

    private void setActive(boolean isActive) {
        if (this.mIsActive != isActive) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Setting active to ");
            stringBuilder.append(isActive);
            Log.d(str, stringBuilder.toString());
            this.mIsActive = isActive;
            this.mWifiConfigManager.saveToStore(false);
        }
    }

    public void start() {
        Log.d(TAG, "start()");
        this.mWifiInjector.getWifiScanner().registerScanListener(this.mScanListener);
        if (this.mIsActive) {
            this.mWifiWakeMetrics.recordIgnoredStart();
            return;
        }
        setActive(true);
        if (isEnabled()) {
            this.mWakeupOnboarding.maybeShowNotification();
            Set<ScanResultMatchInfo> matchInfos = toMatchInfos(filterDfsScanResults(this.mWifiInjector.getWifiScanner().getSingleScanResults()));
            matchInfos.retainAll(getGoodSavedNetworks());
            if (this.mVerboseLoggingEnabled) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Saved networks in most recent scan:");
                stringBuilder.append(matchInfos);
                Log.d(str, stringBuilder.toString());
            }
            this.mWifiWakeMetrics.recordStartEvent(matchInfos.size());
            this.mWakeupLock.setLock(matchInfos);
        }
    }

    public void stop() {
        Log.d(TAG, "stop()");
        this.mWifiInjector.getWifiScanner().deregisterScanListener(this.mScanListener);
        this.mWakeupOnboarding.onStop();
    }

    public void reset() {
        Log.d(TAG, "reset()");
        this.mWifiWakeMetrics.recordResetEvent(this.mNumScansHandled);
        this.mNumScansHandled = 0;
        setActive(false);
    }

    public void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
        this.mWakeupLock.enableVerboseLogging(this.mVerboseLoggingEnabled);
    }

    private List<ScanResult> filterDfsScanResults(Collection<ScanResult> scanResults) {
        int[] dfsChannels = this.mWifiInjector.getWifiNative().getChannelsForBand(4);
        if (dfsChannels == null) {
            dfsChannels = new int[0];
        }
        return (List) scanResults.stream().filter(new -$$Lambda$WakeupController$sB8N4NPbyfefFu6fc4L75U1Md4E((Set) Arrays.stream(dfsChannels).boxed().collect(Collectors.toSet()))).collect(Collectors.toList());
    }

    private Set<ScanResultMatchInfo> getGoodSavedNetworks() {
        List<WifiConfiguration> savedNetworks = this.mWifiConfigManager.getSavedNetworks();
        Set<ScanResultMatchInfo> goodSavedNetworks = new HashSet(savedNetworks.size());
        for (WifiConfiguration config : savedNetworks) {
            if (!(isWideAreaNetwork(config) || config.hasNoInternetAccess() || config.noInternetAccessExpected)) {
                if (config.getNetworkSelectionStatus().getHasEverConnected()) {
                    goodSavedNetworks.add(ScanResultMatchInfo.fromWifiConfiguration(config));
                }
            }
        }
        return goodSavedNetworks;
    }

    private static boolean isWideAreaNetwork(WifiConfiguration config) {
        return false;
    }

    private void handleScanResults(Collection<ScanResult> scanResults) {
        if (isEnabled()) {
            this.mNumScansHandled++;
            if (this.mVerboseLoggingEnabled) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Incoming scan #");
                stringBuilder.append(this.mNumScansHandled);
                Log.d(str, stringBuilder.toString());
            }
            this.mWakeupOnboarding.maybeShowNotification();
            Set<ScanResultMatchInfo> goodSavedNetworks = getGoodSavedNetworks();
            Set<ScanResultMatchInfo> matchInfos = toMatchInfos(scanResults);
            matchInfos.retainAll(goodSavedNetworks);
            this.mWakeupLock.update(matchInfos);
            if (this.mWakeupLock.isUnlocked()) {
                ScanResult network = this.mWakeupEvaluator.findViableNetwork(scanResults, goodSavedNetworks);
                if (network != null) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Enabling wifi for network: ");
                    stringBuilder2.append(network.SSID);
                    Log.d(str2, stringBuilder2.toString());
                    enableWifi();
                }
                return;
            }
            return;
        }
        Log.d(TAG, "Attempted to handleScanResults while not enabled");
    }

    private static Set<ScanResultMatchInfo> toMatchInfos(Collection<ScanResult> scanResults) {
        return (Set) scanResults.stream().map(-$$Lambda$Sgsg9Ml_dxoj_SCBslbH-6YHea8.INSTANCE).collect(Collectors.toSet());
    }

    private void enableWifi() {
        if (this.mWifiInjector.getWifiSettingsStore().handleWifiToggled(true)) {
            this.mWifiInjector.getWifiController().sendMessage(155656);
            this.mWifiWakeMetrics.recordWakeupEvent(this.mNumScansHandled);
        }
    }

    @VisibleForTesting
    boolean isEnabled() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WifiWakeupEnabled ");
        stringBuilder.append(this.mWifiWakeupEnabled);
        Log.d(str, stringBuilder.toString());
        return this.mWifiWakeupEnabled && this.mWakeupConfigStoreData.hasBeenRead();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump of WakeupController");
        pw.println("USE_PLATFORM_WIFI_WAKE: true");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mWifiWakeupEnabled: ");
        stringBuilder.append(this.mWifiWakeupEnabled);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("isOnboarded: ");
        stringBuilder.append(this.mWakeupOnboarding.isOnboarded());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("configStore hasBeenRead: ");
        stringBuilder.append(this.mWakeupConfigStoreData.hasBeenRead());
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mIsActive: ");
        stringBuilder.append(this.mIsActive);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mNumScansHandled: ");
        stringBuilder.append(this.mNumScansHandled);
        pw.println(stringBuilder.toString());
        this.mWakeupLock.dump(fd, pw, args);
    }
}
