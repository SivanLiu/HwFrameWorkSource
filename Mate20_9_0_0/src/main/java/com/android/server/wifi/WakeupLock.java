package com.android.server.wifi;

import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WakeupConfigStoreData.DataSource;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class WakeupLock {
    @VisibleForTesting
    static final int CONSECUTIVE_MISSED_SCANS_REQUIRED_TO_EVICT = 3;
    @VisibleForTesting
    static final long MAX_LOCK_TIME_MILLIS = 600000;
    private static final String TAG = WakeupLock.class.getSimpleName();
    private final Clock mClock;
    private boolean mIsInitialized;
    private long mLockTimestamp;
    private final Map<ScanResultMatchInfo, Integer> mLockedNetworks = new ArrayMap();
    private int mNumScans;
    private boolean mVerboseLoggingEnabled;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiWakeMetrics mWifiWakeMetrics;

    private class WakeupLockDataSource implements DataSource<Set<ScanResultMatchInfo>> {
        private WakeupLockDataSource() {
        }

        public Set<ScanResultMatchInfo> getData() {
            return WakeupLock.this.mLockedNetworks.keySet();
        }

        public void setData(Set<ScanResultMatchInfo> data) {
            WakeupLock.this.mLockedNetworks.clear();
            for (ScanResultMatchInfo network : data) {
                WakeupLock.this.mLockedNetworks.put(network, Integer.valueOf(3));
            }
            WakeupLock.this.mIsInitialized = true;
        }
    }

    public WakeupLock(WifiConfigManager wifiConfigManager, WifiWakeMetrics wifiWakeMetrics, Clock clock) {
        this.mWifiConfigManager = wifiConfigManager;
        this.mWifiWakeMetrics = wifiWakeMetrics;
        this.mClock = clock;
    }

    public void setLock(Collection<ScanResultMatchInfo> scanResultList) {
        this.mLockTimestamp = this.mClock.getElapsedSinceBootMillis();
        this.mIsInitialized = false;
        this.mNumScans = 0;
        this.mLockedNetworks.clear();
        for (ScanResultMatchInfo scanResultMatchInfo : scanResultList) {
            this.mLockedNetworks.put(scanResultMatchInfo, Integer.valueOf(3));
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Lock set. Number of networks: ");
        stringBuilder.append(this.mLockedNetworks.size());
        Log.d(str, stringBuilder.toString());
        this.mWifiConfigManager.saveToStore(false);
    }

    private void maybeSetInitializedByScans(int numScans) {
        if (!this.mIsInitialized) {
            if (numScans >= 3) {
                this.mIsInitialized = true;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Lock initialized by handled scans. Scans: ");
                stringBuilder.append(numScans);
                Log.d(str, stringBuilder.toString());
                if (this.mVerboseLoggingEnabled) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("State of lock: ");
                    stringBuilder.append(this.mLockedNetworks);
                    Log.d(str, stringBuilder.toString());
                }
                this.mWifiWakeMetrics.recordInitializeEvent(this.mNumScans, this.mLockedNetworks.size());
            }
        }
    }

    private void maybeSetInitializedByTimeout(long timestampMillis) {
        if (!this.mIsInitialized) {
            long elapsedTime = timestampMillis - this.mLockTimestamp;
            if (elapsedTime > MAX_LOCK_TIME_MILLIS) {
                this.mIsInitialized = true;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Lock initialized by timeout. Elapsed time: ");
                stringBuilder.append(elapsedTime);
                Log.d(str, stringBuilder.toString());
                if (this.mNumScans == 0) {
                    Log.w(TAG, "Lock initialized with 0 handled scans!");
                }
                if (this.mVerboseLoggingEnabled) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("State of lock: ");
                    stringBuilder.append(this.mLockedNetworks);
                    Log.d(str, stringBuilder.toString());
                }
                this.mWifiWakeMetrics.recordInitializeEvent(this.mNumScans, this.mLockedNetworks.size());
            }
        }
    }

    public boolean isInitialized() {
        return this.mIsInitialized;
    }

    private void addToLock(Collection<ScanResultMatchInfo> networkList) {
        if (this.mVerboseLoggingEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Initializing lock with networks: ");
            stringBuilder.append(networkList);
            Log.d(str, stringBuilder.toString());
        }
        boolean hasChanged = false;
        for (ScanResultMatchInfo network : networkList) {
            if (!this.mLockedNetworks.containsKey(network)) {
                this.mLockedNetworks.put(network, Integer.valueOf(3));
                hasChanged = true;
            }
        }
        if (hasChanged) {
            this.mWifiConfigManager.saveToStore(false);
        }
        maybeSetInitializedByScans(this.mNumScans);
    }

    private void removeFromLock(Collection<ScanResultMatchInfo> networkList) {
        if (this.mVerboseLoggingEnabled) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Filtering lock with networks: ");
            stringBuilder.append(networkList);
            Log.d(str, stringBuilder.toString());
        }
        boolean hasChanged = false;
        Iterator<Entry<ScanResultMatchInfo, Integer>> it = this.mLockedNetworks.entrySet().iterator();
        while (it.hasNext()) {
            Entry<ScanResultMatchInfo, Integer> entry = (Entry) it.next();
            String str2;
            StringBuilder stringBuilder2;
            if (networkList.contains(entry.getKey())) {
                if (this.mVerboseLoggingEnabled) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Found network in lock: ");
                    stringBuilder2.append(((ScanResultMatchInfo) entry.getKey()).networkSsid);
                    Log.d(str2, stringBuilder2.toString());
                }
                entry.setValue(Integer.valueOf(3));
            } else {
                entry.setValue(Integer.valueOf(((Integer) entry.getValue()).intValue() - 1));
                if (((Integer) entry.getValue()).intValue() <= 0) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Removed network from lock: ");
                    stringBuilder2.append(((ScanResultMatchInfo) entry.getKey()).networkSsid);
                    Log.d(str2, stringBuilder2.toString());
                    it.remove();
                    hasChanged = true;
                }
            }
        }
        if (hasChanged) {
            this.mWifiConfigManager.saveToStore(false);
        }
        if (isUnlocked()) {
            Log.d(TAG, "Lock emptied. Recording unlock event.");
            this.mWifiWakeMetrics.recordUnlockEvent(this.mNumScans);
        }
    }

    public void update(Collection<ScanResultMatchInfo> networkList) {
        if (!isUnlocked()) {
            maybeSetInitializedByTimeout(this.mClock.getElapsedSinceBootMillis());
            this.mNumScans++;
            if (this.mIsInitialized) {
                removeFromLock(networkList);
            } else {
                addToLock(networkList);
            }
        }
    }

    public boolean isUnlocked() {
        return this.mIsInitialized && this.mLockedNetworks.isEmpty();
    }

    public DataSource<Set<ScanResultMatchInfo>> getDataSource() {
        return new WakeupLockDataSource();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WakeupLock: ");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mNumScans: ");
        stringBuilder.append(this.mNumScans);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mIsInitialized: ");
        stringBuilder.append(this.mIsInitialized);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("Locked networks: ");
        stringBuilder.append(this.mLockedNetworks.size());
        pw.println(stringBuilder.toString());
        for (Entry<ScanResultMatchInfo, Integer> entry : this.mLockedNetworks.entrySet()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(entry.getKey());
            stringBuilder2.append(", scans to evict: ");
            stringBuilder2.append(entry.getValue());
            pw.println(stringBuilder2.toString());
        }
    }

    public void enableVerboseLogging(boolean enabled) {
        this.mVerboseLoggingEnabled = enabled;
    }
}
