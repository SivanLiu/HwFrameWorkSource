package com.android.server.wifi.scanner;

import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiScanner.ScanData;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.util.ArrayMap;
import android.util.LocalLog;
import android.util.Log;
import com.android.server.wifi.Clock;
import com.android.server.wifi.ScanDetail;
import com.android.server.wifi.WifiMonitor;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiNative.BucketSettings;
import com.android.server.wifi.WifiNative.PnoEventHandler;
import com.android.server.wifi.WifiNative.PnoNetwork;
import com.android.server.wifi.WifiNative.PnoSettings;
import com.android.server.wifi.WifiNative.ScanCapabilities;
import com.android.server.wifi.WifiNative.ScanEventHandler;
import com.android.server.wifi.WifiNative.ScanSettings;
import com.android.server.wifi.scanner.ChannelHelper.ChannelCollection;
import com.android.server.wifi.util.ScanResultUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.GuardedBy;

public class WificondScannerImpl extends WifiScannerImpl implements Callback {
    private static final boolean DBG = false;
    private static final int MAX_APS_PER_SCAN = 32;
    public static final int MAX_HIDDEN_NETWORK_IDS_PER_SCAN = 16;
    private static final int MAX_SCAN_BUCKETS = 16;
    private static final int SCAN_BUFFER_CAPACITY = 10;
    private static final long SCAN_TIMEOUT_MS = 15000;
    private static final String TAG = "WificondScannerImpl";
    public static final String TIMEOUT_ALARM_TAG = "WificondScannerImpl Scan Timeout";
    private final AlarmManager mAlarmManager;
    private final ChannelHelper mChannelHelper;
    private final Clock mClock;
    private final Context mContext;
    private final Handler mEventHandler;
    private final boolean mHwPnoScanSupported;
    private final String mIfaceName;
    private LastPnoScanSettings mLastPnoScanSettings = null;
    private LastScanSettings mLastScanSettings = null;
    private ScanData mLatestSingleScanResult = new ScanData(0, 0, new ScanResult[0]);
    private LocalLog mLocalLog = null;
    private ArrayList<ScanDetail> mNativePnoScanResults;
    private ArrayList<ScanDetail> mNativeScanResults;
    @GuardedBy("mSettingsLock")
    private OnAlarmListener mScanTimeoutListener;
    private final Object mSettingsLock = new Object();
    private final WifiMonitor mWifiMonitor;
    private final WifiNative mWifiNative;

    private static class LastPnoScanSettings {
        public PnoNetwork[] pnoNetworkList;
        public PnoEventHandler pnoScanEventHandler;
        public long startTime;

        LastPnoScanSettings(long startTime, PnoNetwork[] pnoNetworkList, PnoEventHandler pnoScanEventHandler) {
            this.startTime = startTime;
            this.pnoNetworkList = pnoNetworkList;
            this.pnoScanEventHandler = pnoScanEventHandler;
        }
    }

    private static class LastScanSettings {
        public boolean isHiddenSingleScan = false;
        public boolean reportSingleScanFullResults;
        public ScanEventHandler singleScanEventHandler;
        public ChannelCollection singleScanFreqs;
        public long startTime;

        LastScanSettings(long startTime, boolean reportSingleScanFullResults, ChannelCollection singleScanFreqs, ScanEventHandler singleScanEventHandler) {
            this.startTime = startTime;
            this.reportSingleScanFullResults = reportSingleScanFullResults;
            this.singleScanFreqs = singleScanFreqs;
            this.singleScanEventHandler = singleScanEventHandler;
        }
    }

    public WificondScannerImpl(Context context, String ifaceName, WifiNative wifiNative, WifiMonitor wifiMonitor, ChannelHelper channelHelper, Looper looper, Clock clock) {
        this.mContext = context;
        this.mIfaceName = ifaceName;
        this.mWifiNative = wifiNative;
        this.mWifiMonitor = wifiMonitor;
        this.mChannelHelper = channelHelper;
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mEventHandler = new Handler(looper, this);
        this.mClock = clock;
        this.mHwPnoScanSupported = this.mContext.getResources().getBoolean(17957071);
        wifiMonitor.registerHandler(this.mIfaceName, WifiMonitor.SCAN_FAILED_EVENT, this.mEventHandler);
        wifiMonitor.registerHandler(this.mIfaceName, 147474, this.mEventHandler);
        wifiMonitor.registerHandler(this.mIfaceName, WifiMonitor.SCAN_RESULTS_EVENT, this.mEventHandler);
    }

    public void cleanup() {
        synchronized (this.mSettingsLock) {
            stopHwPnoScan();
            this.mLastScanSettings = null;
            this.mLastPnoScanSettings = null;
            this.mWifiMonitor.deregisterHandler(this.mIfaceName, WifiMonitor.SCAN_FAILED_EVENT, this.mEventHandler);
            this.mWifiMonitor.deregisterHandler(this.mIfaceName, 147474, this.mEventHandler);
            this.mWifiMonitor.deregisterHandler(this.mIfaceName, WifiMonitor.SCAN_RESULTS_EVENT, this.mEventHandler);
        }
    }

    public boolean getScanCapabilities(ScanCapabilities capabilities) {
        capabilities.max_scan_cache_size = Values.MAX_EXPID;
        capabilities.max_scan_buckets = 16;
        capabilities.max_ap_cache_per_scan = 32;
        capabilities.max_rssi_sample_size = 8;
        capabilities.max_scan_reporting_threshold = 10;
        return true;
    }

    public ChannelHelper getChannelHelper() {
        return this.mChannelHelper;
    }

    /* JADX WARNING: Missing block: B:36:0x00e4, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean startSingleScan(ScanSettings settings, ScanEventHandler eventHandler) {
        ScanSettings scanSettings = settings;
        ScanEventHandler scanEventHandler = eventHandler;
        int i = 0;
        if (scanEventHandler == null || scanSettings == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid arguments for startSingleScan: settings=");
            stringBuilder.append(scanSettings);
            stringBuilder.append(",eventHandler=");
            stringBuilder.append(scanEventHandler);
            Log.w(str, stringBuilder.toString());
            return false;
        }
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings != null) {
                Log.w(TAG, "A single scan is already running");
                return false;
            }
            int i2;
            ChannelCollection allFreqs = this.mChannelHelper.createChannelCollection();
            boolean reportFullResults = false;
            for (i2 = 0; i2 < scanSettings.num_buckets; i2++) {
                BucketSettings bucketSettings = scanSettings.buckets[i2];
                if ((bucketSettings.report_events & 2) != 0) {
                    reportFullResults = true;
                }
                allFreqs.addChannels(bucketSettings);
            }
            ArrayList hiddenNetworkSSIDSet = new ArrayList();
            if (scanSettings.hiddenNetworks != null) {
                i2 = Math.min(scanSettings.hiddenNetworks.length, 16);
                while (i < i2) {
                    hiddenNetworkSSIDSet.add(scanSettings.hiddenNetworks[i].ssid);
                    i++;
                }
            }
            this.mLastScanSettings = new LastScanSettings(this.mClock.getElapsedSinceBootMillis(), reportFullResults, allFreqs, scanEventHandler);
            if (scanSettings.isHiddenSingleScan) {
                Log.d(TAG, "settings isHiddenSingleScan true");
                this.mLastScanSettings.isHiddenSingleScan = true;
            }
            boolean success = false;
            if (allFreqs.isEmpty()) {
                Log.e(TAG, "Failed to start scan because there is no available channel to scan");
            } else {
                Set<Integer> freqs = allFreqs.getScanFreqs();
                success = this.mWifiNative.scan(this.mIfaceName, scanSettings.scanType, freqs, hiddenNetworkSSIDSet);
                if (!success) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failed to start scan, freqs=");
                    stringBuilder2.append(freqs);
                    Log.e(str2, stringBuilder2.toString());
                }
            }
            if (success) {
                this.mScanTimeoutListener = new OnAlarmListener() {
                    public void onAlarm() {
                        WificondScannerImpl.this.handleScanTimeout();
                    }
                };
                this.mAlarmManager.set(2, this.mClock.getElapsedSinceBootMillis() + SCAN_TIMEOUT_MS, TIMEOUT_ALARM_TAG, this.mScanTimeoutListener, this.mEventHandler);
            } else {
                this.mEventHandler.post(new Runnable() {
                    public void run() {
                        WificondScannerImpl.this.reportScanFailure();
                    }
                });
            }
        }
    }

    public ScanData getLatestSingleScanResults() {
        return this.mLatestSingleScanResult;
    }

    public boolean startBatchedScan(ScanSettings settings, ScanEventHandler eventHandler) {
        Log.w(TAG, "startBatchedScan() is not supported");
        return false;
    }

    public void stopBatchedScan() {
        Log.w(TAG, "stopBatchedScan() is not supported");
    }

    public void pauseBatchedScan() {
        Log.w(TAG, "pauseBatchedScan() is not supported");
    }

    public void restartBatchedScan() {
        Log.w(TAG, "restartBatchedScan() is not supported");
    }

    private void handleScanTimeout() {
        synchronized (this.mSettingsLock) {
            Log.e(TAG, "Timed out waiting for scan result from wificond");
            reportScanFailure();
            this.mScanTimeoutListener = null;
        }
    }

    public void setWifiScanLogger(LocalLog logger) {
        this.mLocalLog = logger;
    }

    public void logWifiScan(String message) {
        if (this.mLocalLog != null) {
            LocalLog localLog = this.mLocalLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("<WifiScanLogger> ");
            stringBuilder.append(message);
            localLog.log(stringBuilder.toString());
        }
    }

    public boolean handleMessage(Message msg) {
        int i = msg.what;
        if (i != WifiMonitor.SCAN_RESULTS_EVENT) {
            switch (i) {
                case WifiMonitor.SCAN_FAILED_EVENT /*147473*/:
                    Log.w(TAG, "Scan failed");
                    cancelScanTimeout();
                    reportScanFailure();
                    break;
                case 147474:
                    pollLatestScanDataForPno();
                    break;
            }
        }
        cancelScanTimeout();
        pollLatestScanData();
        return true;
    }

    private void cancelScanTimeout() {
        synchronized (this.mSettingsLock) {
            if (this.mScanTimeoutListener != null) {
                this.mAlarmManager.cancel(this.mScanTimeoutListener);
                this.mScanTimeoutListener = null;
            }
        }
    }

    private void reportScanFailure() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings != null) {
                if (this.mLastScanSettings.singleScanEventHandler != null) {
                    this.mLastScanSettings.singleScanEventHandler.onScanStatus(3);
                }
                this.mLastScanSettings = null;
            }
        }
    }

    private void reportPnoScanFailure() {
        synchronized (this.mSettingsLock) {
            if (this.mLastPnoScanSettings != null) {
                if (this.mLastPnoScanSettings.pnoScanEventHandler != null) {
                    this.mLastPnoScanSettings.pnoScanEventHandler.onPnoScanFailed();
                }
                this.mLastPnoScanSettings = null;
            }
        }
    }

    /* JADX WARNING: Missing block: B:23:0x008d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void pollLatestScanDataForPno() {
        synchronized (this.mSettingsLock) {
            if (this.mLastPnoScanSettings == null) {
                return;
            }
            this.mNativePnoScanResults = this.mWifiNative.getPnoScanResults(this.mIfaceName);
            List<ScanResult> hwPnoScanResults = new ArrayList();
            int numFilteredScanResults = 0;
            for (int i = 0; i < this.mNativePnoScanResults.size(); i++) {
                ScanResult result = ((ScanDetail) this.mNativePnoScanResults.get(i)).getScanResult();
                if (result.timestamp / 1000 > this.mLastPnoScanSettings.startTime) {
                    hwPnoScanResults.add(result);
                } else {
                    numFilteredScanResults++;
                }
            }
            if (numFilteredScanResults != 0 || this.mNativePnoScanResults.size() == 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Pno Filtering out ");
                stringBuilder.append(numFilteredScanResults);
                stringBuilder.append(" pno scan results.total size ");
                stringBuilder.append(this.mNativePnoScanResults.size());
                Log.d("WifiScanLog", stringBuilder.toString());
            }
            if (this.mLastPnoScanSettings.pnoScanEventHandler != null) {
                this.mLastPnoScanSettings.pnoScanEventHandler.onPnoNetworkFound((ScanResult[]) hwPnoScanResults.toArray(new ScanResult[hwPnoScanResults.size()]));
            }
        }
    }

    private static boolean isAllChannelsScanned(ChannelCollection channelCollection) {
        if (channelCollection.containsBand(1) && channelCollection.containsBand(2)) {
            return true;
        }
        return false;
    }

    private ArrayMap<String, StringBuffer> getSavedNetworkScanResults() {
        long nowMs = System.currentTimeMillis();
        ArrayMap<String, StringBuffer> filterResults = new ArrayMap();
        WifiManager wifiMgr = (WifiManager) this.mContext.getSystemService("wifi");
        if (wifiMgr == null) {
            Log.d(TAG, "WifiManager is null,error!");
            return filterResults;
        }
        List<WifiConfiguration> savedNetworks = wifiMgr.getConfiguredNetworks();
        int savedNetworksListSize = savedNetworks.size();
        for (int i = 0; i < savedNetworksListSize; i++) {
            WifiConfiguration network = (WifiConfiguration) savedNetworks.get(i);
            long diffMs = nowMs - (0 == network.lastHasInternetTimestamp ? network.lastConnected : network.lastHasInternetTimestamp);
            if (0 < diffMs && diffMs < 604800000) {
                filterResults.put(network.SSID, null);
            }
        }
        return filterResults;
    }

    private void pollLatestScanData() {
        synchronized (this.mSettingsLock) {
            if (this.mLastScanSettings == null) {
                return;
            }
            ScanResult result;
            this.mNativeScanResults = this.mWifiNative.getScanResults(this.mIfaceName);
            ArrayList<ScanResult> singleScanResults = new ArrayList();
            ArrayList singleScanResultsForApp = new ArrayList();
            StringBuffer filteredResultsString = new StringBuffer();
            ArrayMap<String, StringBuffer> filterResults = getSavedNetworkScanResults();
            int numFilteredScanResults = 0;
            int list_size = this.mNativeScanResults.size();
            for (int i = 0; i < list_size; i++) {
                boolean isTimeFiltered;
                boolean isTimeFiltered2;
                boolean isFrequencyFiltered;
                result = ((ScanDetail) this.mNativeScanResults.get(i)).getScanResult();
                long timestamp_ms = result.timestamp / 1000;
                if (timestamp_ms > this.mLastScanSettings.startTime) {
                    isTimeFiltered = false;
                    if (this.mLastScanSettings.singleScanFreqs.containsChannel(result.frequency)) {
                        singleScanResults.add(result);
                        isTimeFiltered2 = false;
                    } else {
                        isTimeFiltered2 = true;
                    }
                } else {
                    isTimeFiltered = true;
                    isTimeFiltered2 = false;
                    if (this.mLastScanSettings.singleScanFreqs.containsChannel(result.frequency)) {
                        singleScanResultsForApp.add(result);
                    }
                }
                boolean isFrequencyFiltered2 = isTimeFiltered2;
                isTimeFiltered2 = isTimeFiltered;
                if (!isTimeFiltered2) {
                    if (!isFrequencyFiltered2) {
                        isFrequencyFiltered = isFrequencyFiltered2;
                    }
                }
                if (numFilteredScanResults == 0) {
                    filteredResultsString.append(this.mLastScanSettings.startTime);
                    filteredResultsString.append("/");
                }
                if (isFrequencyFiltered2) {
                    filteredResultsString.append(result.SSID);
                    filteredResultsString.append("|");
                    filteredResultsString.append(result.frequency);
                    filteredResultsString.append("|");
                    filteredResultsString.append(ScanResultUtil.getConfusedBssid(result.BSSID));
                    filteredResultsString.append("|");
                }
                if (isTimeFiltered2) {
                    StringBuffer stringBuffer = new StringBuffer("\"");
                    stringBuffer.append(result.SSID);
                    stringBuffer.append("\"");
                    String filterSsid = stringBuffer.toString();
                    if (filterResults.containsKey(filterSsid)) {
                        StringBuffer filterSsidValue;
                        stringBuffer = (StringBuffer) filterResults.get(filterSsid);
                        if (stringBuffer == null) {
                            filterSsidValue = new StringBuffer();
                        } else {
                            filterSsidValue = stringBuffer;
                        }
                        try {
                            filterSsidValue.append(result.BSSID.substring(result.BSSID.length() - 5));
                            filterSsidValue.append("|");
                            isFrequencyFiltered = isFrequencyFiltered2;
                            try {
                                filterSsidValue.append(result.timestamp / 1000);
                                filterSsidValue.append("|");
                                filterResults.put(filterSsid, filterSsidValue);
                            } catch (StringIndexOutOfBoundsException e) {
                            }
                        } catch (StringIndexOutOfBoundsException e2) {
                            isFrequencyFiltered = isFrequencyFiltered2;
                            Log.d(TAG, "substring: StringIndexOutOfBoundsException");
                            numFilteredScanResults++;
                        }
                        numFilteredScanResults++;
                    }
                }
                isFrequencyFiltered = isFrequencyFiltered2;
                numFilteredScanResults++;
            }
            for (Entry<String, StringBuffer> entry : filterResults.entrySet()) {
                StringBuffer arrayMap = (StringBuffer) entry.getValue();
                if (arrayMap != null) {
                    filteredResultsString.append((String) entry.getKey());
                    filteredResultsString.append("|");
                    filteredResultsString.append(arrayMap);
                }
            }
            singleScanResultsForApp.addAll(singleScanResults);
            if (numFilteredScanResults != 0 || this.mNativeScanResults.size() == 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Filtering out ");
                stringBuilder.append(numFilteredScanResults);
                stringBuilder.append(" scan results. total size ");
                stringBuilder.append(this.mNativeScanResults.size());
                stringBuilder.append(" , Filtered Results : ");
                stringBuilder.append(filteredResultsString);
                Log.d("WifiScanLog", stringBuilder.toString());
            }
            if (this.mLastScanSettings.singleScanEventHandler != null) {
                if (this.mLastScanSettings.reportSingleScanFullResults) {
                    for (ScanResult result2 : singleScanResults) {
                        this.mLastScanSettings.singleScanEventHandler.onFullScanResult(result2, 0);
                    }
                }
                Collections.sort(singleScanResults, SCAN_RESULT_SORT_COMPARATOR);
                Collections.sort(singleScanResultsForApp, SCAN_RESULT_SORT_COMPARATOR);
                this.mLatestSingleScanResult = new ScanData(0, 0, 0, isAllChannelsScanned(this.mLastScanSettings.singleScanFreqs), (ScanResult[]) singleScanResultsForApp.toArray(new ScanResult[singleScanResultsForApp.size()]), this.mLastScanSettings.isHiddenSingleScan);
                this.mLastScanSettings.singleScanEventHandler.onScanStatus(0);
            }
            this.mLastScanSettings = null;
        }
    }

    public ScanData[] getLatestBatchedScanResults(boolean flush) {
        return null;
    }

    private boolean startHwPnoScan(PnoSettings pnoSettings) {
        return this.mWifiNative.startPnoScan(this.mIfaceName, pnoSettings);
    }

    private void stopHwPnoScan() {
        this.mWifiNative.stopPnoScan(this.mIfaceName);
    }

    private boolean isHwPnoScanRequired(boolean isConnectedPno) {
        return !isConnectedPno && this.mHwPnoScanSupported;
    }

    public boolean setHwPnoList(PnoSettings settings, PnoEventHandler eventHandler) {
        synchronized (this.mSettingsLock) {
            if (this.mLastPnoScanSettings != null) {
                Log.w(TAG, "Already running a PNO scan");
                return false;
            } else if (isHwPnoScanRequired(settings.isConnected)) {
                if (startHwPnoScan(settings)) {
                    this.mLastPnoScanSettings = new LastPnoScanSettings(this.mClock.getElapsedSinceBootMillis(), settings.networkList, eventHandler);
                } else {
                    Log.e(TAG, "Failed to start PNO scan");
                    reportPnoScanFailure();
                }
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean resetHwPnoList() {
        synchronized (this.mSettingsLock) {
            if (this.mLastPnoScanSettings == null) {
                Log.w(TAG, "No PNO scan running");
                return false;
            }
            this.mLastPnoScanSettings = null;
            stopHwPnoScan();
            return true;
        }
    }

    public boolean isHwPnoSupported(boolean isConnectedPno) {
        return isHwPnoScanRequired(isConnectedPno);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mSettingsLock) {
            long nowMs = this.mClock.getElapsedSinceBootMillis();
            pw.println("Latest native scan results:");
            if (this.mNativeScanResults != null) {
                ScanResultUtil.dumpScanResults(pw, (List) this.mNativeScanResults.stream().map(-$$Lambda$WificondScannerImpl$CSjtYSyNiQ_mC6mOyQ4Gpky-lqY.INSTANCE).collect(Collectors.toList()), nowMs);
            }
            pw.println("Latest native pno scan results:");
            if (this.mNativePnoScanResults != null) {
                ScanResultUtil.dumpScanResults(pw, (List) this.mNativePnoScanResults.stream().map(-$$Lambda$WificondScannerImpl$VfxaUtYlcuU7--Z28abhvk42O2k.INSTANCE).collect(Collectors.toList()), nowMs);
            }
        }
    }
}
