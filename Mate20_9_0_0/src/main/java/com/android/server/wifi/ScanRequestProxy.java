package com.android.server.wifi;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiScanner.ScanData;
import android.net.wifi.WifiScanner.ScanListener;
import android.net.wifi.WifiScanner.ScanSettings;
import android.net.wifi.WifiScanner.ScanSettings.HiddenNetwork;
import android.os.Binder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class ScanRequestProxy {
    private static final int ALLOW_SEND_HILINK_SCAN_RESULTS_BROADCAST_TRIES = 10;
    private static final String HILINK_STATE_CHANGE_ACTION = "com.android.server.wifi.huawei.action.NETWORK_CONNECTED";
    @VisibleForTesting
    public static final int SCAN_REQUEST_THROTTLE_INTERVAL_BG_APPS_MS = 1800000;
    @VisibleForTesting
    public static final int SCAN_REQUEST_THROTTLE_MAX_IN_TIME_WINDOW_FG_APPS = 4;
    @VisibleForTesting
    public static final int SCAN_REQUEST_THROTTLE_TIME_WINDOW_FG_APPS_MS = 120000;
    private static final String TAG = "WifiScanRequestProxy";
    private final ActivityManager mActivityManager;
    boolean mAllowSendHiLinkScanResultsBroadcast = false;
    private final AppOpsManager mAppOps;
    private final Clock mClock;
    private final Context mContext;
    private long mHilinkLastHashCode = 0;
    private long mHilinkLastLevelCode = 0;
    private boolean mIsScanProcessingComplete = true;
    private final List<ScanResult> mLastScanResults = new ArrayList();
    private long mLastScanTimestampForBgApps = 0;
    private final ArrayMap<Pair<Integer, String>, LinkedList<Long>> mLastScanTimestampsForFgApps = new ArrayMap();
    private boolean mScanningForHiddenNetworksEnabled = false;
    int mSendHiLinkScanResultsBroadcastTries = 0;
    private boolean mVerboseLoggingEnabled = false;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiInjector mWifiInjector;
    private final WifiMetrics mWifiMetrics;
    private final WifiPermissionsUtil mWifiPermissionsUtil;
    private WifiScanner mWifiScanner;

    private class ScanRequestProxyScanListener implements ScanListener {
        private ScanRequestProxyScanListener() {
        }

        public void onSuccess() {
            if (ScanRequestProxy.this.mVerboseLoggingEnabled) {
                Log.d(ScanRequestProxy.TAG, "Scan request succeeded");
            }
        }

        public void onFailure(int reason, String description) {
            String str = ScanRequestProxy.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Scan failure received. reason: ");
            stringBuilder.append(reason);
            stringBuilder.append(",description: ");
            stringBuilder.append(description);
            Log.e(str, stringBuilder.toString());
            ScanRequestProxy.this.sendScanResultBroadcastIfScanProcessingNotComplete(false);
        }

        public void onResults(ScanData[] scanDatas) {
            if (ScanRequestProxy.this.mVerboseLoggingEnabled) {
                Log.d(ScanRequestProxy.TAG, "Scan results received");
            }
            if (scanDatas.length != 1) {
                Log.wtf(ScanRequestProxy.TAG, "Found more than 1 batch of scan results, Failing...");
                ScanRequestProxy.this.sendScanResultBroadcastIfScanProcessingNotComplete(false);
                return;
            }
            ScanResult[] scanResults = scanDatas[0].getResults();
            if (ScanRequestProxy.this.mVerboseLoggingEnabled) {
                String str = ScanRequestProxy.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Received ");
                stringBuilder.append(scanResults.length);
                stringBuilder.append(" scan results");
                Log.d(str, stringBuilder.toString());
            }
            synchronized (ScanRequestProxy.this) {
                ScanRequestProxy.this.mLastScanResults.clear();
                ScanRequestProxy.this.mLastScanResults.addAll(Arrays.asList(scanResults));
            }
            ScanRequestProxy.this.sendScanResultBroadcastIfScanProcessingNotComplete(true);
            if (ScanRequestProxy.this.mAllowSendHiLinkScanResultsBroadcast) {
                ScanRequestProxy.this.sendHilinkscanResultBroadcast();
            }
        }

        public void onFullResult(ScanResult fullScanResult) {
        }

        public void onPeriodChanged(int periodInMs) {
        }
    }

    ScanRequestProxy(Context context, AppOpsManager appOpsManager, ActivityManager activityManager, WifiInjector wifiInjector, WifiConfigManager configManager, WifiPermissionsUtil wifiPermissionUtil, WifiMetrics wifiMetrics, Clock clock) {
        this.mContext = context;
        this.mAppOps = appOpsManager;
        this.mActivityManager = activityManager;
        this.mWifiInjector = wifiInjector;
        this.mWifiConfigManager = configManager;
        this.mWifiPermissionsUtil = wifiPermissionUtil;
        this.mWifiMetrics = wifiMetrics;
        this.mClock = clock;
    }

    public void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
    }

    public void enableScanningForHiddenNetworks(boolean enable) {
        if (this.mVerboseLoggingEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Scanning for hidden networks is ");
            stringBuilder.append(enable ? "enabled" : "disabled");
            Log.d(str, stringBuilder.toString());
        }
        this.mScanningForHiddenNetworksEnabled = enable;
    }

    private boolean retrieveWifiScannerIfNecessary() {
        if (this.mWifiScanner == null) {
            this.mWifiScanner = this.mWifiInjector.getWifiScanner();
        }
        return this.mWifiScanner != null;
    }

    private void sendScanResultBroadcastIfScanProcessingNotComplete(boolean scanSucceeded) {
        if (this.mIsScanProcessingComplete) {
            Log.i(TAG, "No ongoing scan request. Don't send scan broadcast.");
            return;
        }
        sendScanResultBroadcast(scanSucceeded);
        this.mIsScanProcessingComplete = true;
    }

    private void sendScanResultBroadcast(boolean scanSucceeded) {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
            intent.addFlags(67108864);
            intent.putExtra("resultsUpdated", scanSucceeded);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private void sendScanResultFailureBroadcastToPackage(String packageName) {
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            Intent intent = new Intent("android.net.wifi.SCAN_RESULTS");
            intent.addFlags(67108864);
            intent.putExtra("resultsUpdated", false);
            intent.setPackage(packageName);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private void trimPastScanRequestTimesForForegroundApp(List<Long> scanRequestTimestamps, long currentTimeMillis) {
        Iterator<Long> timestampsIter = scanRequestTimestamps.iterator();
        while (timestampsIter.hasNext() && currentTimeMillis - ((Long) timestampsIter.next()).longValue() > 120000) {
            timestampsIter.remove();
        }
    }

    private LinkedList<Long> getOrCreateScanRequestTimestampsForForegroundApp(int callingUid, String packageName) {
        Pair<Integer, String> uidAndPackageNamePair = Pair.create(Integer.valueOf(callingUid), packageName);
        LinkedList<Long> scanRequestTimestamps = (LinkedList) this.mLastScanTimestampsForFgApps.get(uidAndPackageNamePair);
        if (scanRequestTimestamps != null) {
            return scanRequestTimestamps;
        }
        scanRequestTimestamps = new LinkedList();
        this.mLastScanTimestampsForFgApps.put(uidAndPackageNamePair, scanRequestTimestamps);
        return scanRequestTimestamps;
    }

    private boolean shouldScanRequestBeThrottledForForegroundApp(int callingUid, String packageName) {
        LinkedList<Long> scanRequestTimestamps = getOrCreateScanRequestTimestampsForForegroundApp(callingUid, packageName);
        long currentTimeMillis = this.mClock.getElapsedSinceBootMillis();
        trimPastScanRequestTimesForForegroundApp(scanRequestTimestamps, currentTimeMillis);
        if (scanRequestTimestamps.size() >= 4) {
            return true;
        }
        scanRequestTimestamps.addLast(Long.valueOf(currentTimeMillis));
        return false;
    }

    private boolean shouldScanRequestBeThrottledForBackgroundApp() {
        long lastScanMs = this.mLastScanTimestampForBgApps;
        long elapsedRealtime = this.mClock.getElapsedSinceBootMillis();
        if (lastScanMs != 0 && elapsedRealtime - lastScanMs < 1800000) {
            return true;
        }
        this.mLastScanTimestampForBgApps = elapsedRealtime;
        return false;
    }

    private boolean isRequestFromBackground(int callingUid, String packageName) {
        this.mAppOps.checkPackage(callingUid, packageName);
        long callingIdentity = Binder.clearCallingIdentity();
        try {
            boolean z = this.mActivityManager.getPackageImportance(packageName) > 125;
            Binder.restoreCallingIdentity(callingIdentity);
            return z;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(callingIdentity);
        }
    }

    private boolean shouldScanRequestBeThrottledForApp(int callingUid, String packageName) {
        boolean isThrottled;
        String str;
        StringBuilder stringBuilder;
        if (packageName == null || packageName.length() == 0 || !isRequestFromBackground(callingUid, packageName)) {
            if ("com.huawei.smarthome".equals(packageName) || "com.huawei.smartspeaker".equals(packageName)) {
                Log.d(TAG, "special package in whitelist");
                isThrottled = false;
            } else {
                isThrottled = shouldScanRequestBeThrottledForForegroundApp(callingUid, packageName);
            }
            if (isThrottled) {
                if (this.mVerboseLoggingEnabled) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Foreground scan app request [");
                    stringBuilder.append(callingUid);
                    stringBuilder.append(", ");
                    stringBuilder.append(packageName);
                    stringBuilder.append("]");
                    Log.v(str, stringBuilder.toString());
                }
                this.mWifiMetrics.incrementExternalForegroundAppOneshotScanRequestsThrottledCount();
            }
        } else {
            isThrottled = shouldScanRequestBeThrottledForBackgroundApp();
            if (isThrottled) {
                if (this.mVerboseLoggingEnabled) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Background scan app request [");
                    stringBuilder.append(callingUid);
                    stringBuilder.append(", ");
                    stringBuilder.append(packageName);
                    stringBuilder.append("]");
                    Log.v(str, stringBuilder.toString());
                }
                this.mWifiMetrics.incrementExternalBackgroundAppOneshotScanRequestsThrottledCount();
            }
        }
        this.mWifiMetrics.incrementExternalAppOneshotScanRequestsCount();
        return isThrottled;
    }

    public boolean startScan(int callingUid, String packageName) {
        return startScanForSpecBand(callingUid, packageName, 7);
    }

    public boolean startScanForSpecBand(int callingUid, String packageName, int band) {
        if (retrieveWifiScannerIfNecessary()) {
            boolean fromSettingsOrSetupWizard = this.mWifiPermissionsUtil.checkNetworkSettingsPermission(callingUid) || this.mWifiPermissionsUtil.checkNetworkSetupWizardPermission(callingUid);
            if (fromSettingsOrSetupWizard || !shouldScanRequestBeThrottledForApp(callingUid, packageName)) {
                WorkSource workSource = new WorkSource(callingUid);
                ScanSettings settings = new ScanSettings();
                if (fromSettingsOrSetupWizard) {
                    settings.type = 2;
                }
                settings.band = 7;
                if (band >= 0 && band <= 7) {
                    settings.band = band;
                }
                settings.reportEvents = 3;
                if (this.mScanningForHiddenNetworksEnabled) {
                    List<HiddenNetwork> hiddenNetworkList = this.mWifiConfigManager.retrieveHiddenNetworkList();
                    settings.hiddenNetworks = (HiddenNetwork[]) hiddenNetworkList.toArray(new HiddenNetwork[hiddenNetworkList.size()]);
                }
                this.mWifiScanner.startScan(settings, new ScanRequestProxyScanListener(), workSource);
                this.mIsScanProcessingComplete = false;
                return true;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Scan request from ");
            stringBuilder.append(packageName);
            stringBuilder.append(" throttled");
            Log.i(str, stringBuilder.toString());
            sendScanResultFailureBroadcastToPackage(packageName);
            return false;
        }
        Log.e(TAG, "Failed to retrieve wifiscanner");
        sendScanResultFailureBroadcastToPackage(packageName);
        return false;
    }

    public List<ScanResult> getScanResults() {
        List list;
        synchronized (this) {
            list = this.mLastScanResults;
        }
        return list;
    }

    public void clearScanResults() {
        synchronized (this) {
            this.mLastScanResults.clear();
        }
        this.mLastScanTimestampForBgApps = 0;
        this.mLastScanTimestampsForFgApps.clear();
    }

    void startScanForHiddenNetwork(ScanListener listener, WifiConfiguration config) {
        if (retrieveWifiScannerIfNecessary()) {
            List<HiddenNetwork> hiddenNetworkList = new ArrayList();
            int i = 0;
            String[] charsets = SystemProperties.getBoolean("ro.config.wifi_use_euc-kr", false) ? new String[]{"UTF-8", "EUC-KR", "KSC5601"} : new String[]{"UTF-8", "GBK"};
            int length = charsets.length;
            while (i < length) {
                String hex = NativeUtil.quotedAsciiStringToHex(config.SSID, charsets[i]);
                if (hex != null) {
                    hiddenNetworkList.add(new HiddenNetwork(hex));
                }
                i++;
            }
            ScanSettings settings = new ScanSettings();
            settings.band = 7;
            settings.reportEvents = 3;
            settings.hiddenNetworks = (HiddenNetwork[]) hiddenNetworkList.toArray(new HiddenNetwork[hiddenNetworkList.size()]);
            settings.isHiddenSigleScan = true;
            Log.d(TAG, "startScanForHiddenNetwork");
            this.mWifiScanner.startScan(settings, listener);
            return;
        }
        Log.e(TAG, "Failed to retrieve wifiscanner");
    }

    /* JADX WARNING: Missing block: B:24:0x004e, code skipped:
            if (r1 != r11.mHilinkLastHashCode) goto L_0x0056;
     */
    /* JADX WARNING: Missing block: B:26:0x0054, code skipped:
            if (r6 <= r11.mHilinkLastLevelCode) goto L_0x0076;
     */
    /* JADX WARNING: Missing block: B:27:0x0056, code skipped:
            android.util.Log.d(TAG, "Hilink sendHilinkscanResultBroadcast");
            r0 = new android.content.Intent(HILINK_STATE_CHANGE_ACTION);
            r0.putExtra("TYPE", "SCAN_RESULTS");
            r11.mContext.sendBroadcastAsUser(r0, android.os.UserHandle.ALL);
            r11.mHilinkLastHashCode = r1;
            r11.mAllowSendHiLinkScanResultsBroadcast = false;
     */
    /* JADX WARNING: Missing block: B:28:0x0076, code skipped:
            r11.mHilinkLastLevelCode = r6;
            r0 = r11.mSendHiLinkScanResultsBroadcastTries + 1;
            r11.mSendHiLinkScanResultsBroadcastTries = r0;
     */
    /* JADX WARNING: Missing block: B:29:0x0080, code skipped:
            if (r0 <= 10) goto L_?;
     */
    /* JADX WARNING: Missing block: B:30:0x0082, code skipped:
            r11.mAllowSendHiLinkScanResultsBroadcast = false;
     */
    /* JADX WARNING: Missing block: B:44:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:45:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void sendHilinkscanResultBroadcast() {
        long currentHilinkHashCode;
        synchronized (this) {
            long currentLevelHilinkHashCode;
            long currentHilinkHashCode2;
            try {
                int resultSize = this.mLastScanResults.size();
                currentLevelHilinkHashCode = 0;
                currentHilinkHashCode2 = 0;
                int i = 0;
                while (i < resultSize) {
                    try {
                        ScanResult scanResult = (ScanResult) this.mLastScanResults.get(i);
                        if (scanResult != null && scanResult.SSID != null && scanResult.SSID.length() == 32 && scanResult.SSID.startsWith("Hi")) {
                            int itemHashCode = scanResult.SSID.hashCode();
                            if (itemHashCode < 0) {
                                itemHashCode = -itemHashCode;
                            }
                            currentHilinkHashCode2 += (long) itemHashCode;
                            if (scanResult.level >= -45) {
                                currentLevelHilinkHashCode += (long) itemHashCode;
                            }
                        }
                        i++;
                    } catch (Throwable th) {
                        currentHilinkHashCode = th;
                        throw currentHilinkHashCode;
                    }
                }
            } catch (Throwable th2) {
                currentLevelHilinkHashCode = 0;
                currentHilinkHashCode2 = 0;
                currentHilinkHashCode = th2;
                throw currentHilinkHashCode;
            }
        }
    }

    public void clearScanRequestTimestampsForApp(String packageName, int uid) {
        if (this.mVerboseLoggingEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Clearing scan request timestamps for uid=");
            stringBuilder.append(uid);
            stringBuilder.append(", packageName=");
            stringBuilder.append(packageName);
            Log.v(str, stringBuilder.toString());
        }
        this.mLastScanTimestampsForFgApps.remove(Pair.create(Integer.valueOf(uid), packageName));
    }
}
