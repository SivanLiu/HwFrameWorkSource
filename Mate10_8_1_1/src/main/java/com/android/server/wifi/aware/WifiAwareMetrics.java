package com.android.server.wifi.aware;

import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.server.wifi.Clock;
import com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation;
import com.android.server.wifi.nano.WifiMetricsProto.WifiAwareLog;
import com.android.server.wifi.nano.WifiMetricsProto.WifiAwareLog.HistogramBucket;
import com.android.server.wifi.nano.WifiMetricsProto.WifiAwareLog.NanStatusHistogramBucket;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class WifiAwareMetrics {
    private static final boolean DBG = false;
    private static final HistParms DURATION_LOG_HISTOGRAM = new HistParms(0, 1, 10, 9, 8);
    private static final String TAG = "WifiAwareMetrics";
    private Set<Integer> mAppsWithDiscoverySessionResourceFailure = new HashSet();
    private Map<Integer, AttachData> mAttachDataByUid = new HashMap();
    private SparseIntArray mAttachStatusData = new SparseIntArray();
    private long mAvailableTimeMs = 0;
    private final Clock mClock;
    private long mEnabledTimeMs = 0;
    private SparseIntArray mHistogramAttachDuration = new SparseIntArray();
    private SparseIntArray mHistogramAwareAvailableDurationMs = new SparseIntArray();
    private SparseIntArray mHistogramAwareEnabledDurationMs = new SparseIntArray();
    private SparseIntArray mHistogramNdpDuration = new SparseIntArray();
    private SparseIntArray mHistogramPublishDuration = new SparseIntArray();
    private SparseIntArray mHistogramSubscribeDuration = new SparseIntArray();
    private SparseIntArray mInBandNdpStatusData = new SparseIntArray();
    private long mLastEnableAwareInThisSampleWindowMs = 0;
    private long mLastEnableAwareMs = 0;
    private long mLastEnableUsageInThisSampleWindowMs = 0;
    private long mLastEnableUsageMs = 0;
    private final Object mLock = new Object();
    private int mMaxDiscoveryInApp = 0;
    private int mMaxDiscoveryInSystem = 0;
    private int mMaxNdiInApp = 0;
    private int mMaxNdiInSystem = 0;
    private int mMaxNdpInApp = 0;
    private int mMaxNdpInSystem = 0;
    private int mMaxNdpPerNdi = 0;
    private int mMaxPublishInApp = 0;
    private int mMaxPublishInSystem = 0;
    private int mMaxSecureNdpInApp = 0;
    private int mMaxSecureNdpInSystem = 0;
    private int mMaxSubscribeInApp = 0;
    private int mMaxSubscribeInSystem = 0;
    private SparseIntArray mNdpCreationTimeDuration = new SparseIntArray();
    private long mNdpCreationTimeMax = 0;
    private long mNdpCreationTimeMin = -1;
    private long mNdpCreationTimeNumSamples = 0;
    private long mNdpCreationTimeSum = 0;
    private long mNdpCreationTimeSumSq = 0;
    private SparseIntArray mOutOfBandNdpStatusData = new SparseIntArray();
    private SparseIntArray mPublishStatusData = new SparseIntArray();
    private SparseIntArray mSubscribeStatusData = new SparseIntArray();

    private static class AttachData {
        int mMaxConcurrentAttaches;
        boolean mUsesIdentityCallback;

        private AttachData() {
        }
    }

    public static class HistParms {
        public int b;
        public double[] bb;
        public int m;
        public double mLog;
        public int n;
        public int p;
        public int s;
        public double[] sbw;

        public HistParms(int b, int p, int m, int s, int n) {
            this.b = b;
            this.p = p;
            this.m = m;
            this.s = s;
            this.n = n;
            this.mLog = Math.log((double) m);
            this.bb = new double[n];
            this.sbw = new double[n];
            this.bb[0] = (double) (b + p);
            this.sbw[0] = (((double) p) * (((double) m) - 1.0d)) / ((double) s);
            for (int i = 1; i < n; i++) {
                this.bb[i] = (((double) m) * (this.bb[i - 1] - ((double) b))) + ((double) b);
                this.sbw[i] = ((double) m) * this.sbw[i - 1];
            }
        }
    }

    public WifiAwareMetrics(Clock clock) {
        this.mClock = clock;
    }

    public void recordEnableUsage() {
        synchronized (this.mLock) {
            if (this.mLastEnableUsageMs != 0) {
                Log.w(TAG, "enableUsage: mLastEnableUsage*Ms initialized!?");
            }
            this.mLastEnableUsageMs = this.mClock.getElapsedSinceBootMillis();
            this.mLastEnableUsageInThisSampleWindowMs = this.mLastEnableUsageMs;
        }
    }

    public void recordDisableUsage() {
        synchronized (this.mLock) {
            if (this.mLastEnableUsageMs == 0) {
                Log.e(TAG, "disableUsage: mLastEnableUsage not initialized!?");
                return;
            }
            long now = this.mClock.getElapsedSinceBootMillis();
            addLogValueToHistogram(now - this.mLastEnableUsageMs, this.mHistogramAwareAvailableDurationMs, DURATION_LOG_HISTOGRAM);
            this.mAvailableTimeMs += now - this.mLastEnableUsageInThisSampleWindowMs;
            this.mLastEnableUsageMs = 0;
            this.mLastEnableUsageInThisSampleWindowMs = 0;
        }
    }

    public void recordEnableAware() {
        synchronized (this.mLock) {
            if (this.mLastEnableAwareMs != 0) {
                return;
            }
            this.mLastEnableAwareMs = this.mClock.getElapsedSinceBootMillis();
            this.mLastEnableAwareInThisSampleWindowMs = this.mLastEnableAwareMs;
        }
    }

    public void recordDisableAware() {
        synchronized (this.mLock) {
            if (this.mLastEnableAwareMs == 0) {
                return;
            }
            long now = this.mClock.getElapsedSinceBootMillis();
            addLogValueToHistogram(now - this.mLastEnableAwareMs, this.mHistogramAwareEnabledDurationMs, DURATION_LOG_HISTOGRAM);
            this.mEnabledTimeMs += now - this.mLastEnableAwareInThisSampleWindowMs;
            this.mLastEnableAwareMs = 0;
            this.mLastEnableAwareInThisSampleWindowMs = 0;
        }
    }

    public void recordAttachSession(int uid, boolean usesIdentityCallback, SparseArray<WifiAwareClientState> clients) {
        int currentConcurrentCount = 0;
        for (int i = 0; i < clients.size(); i++) {
            if (((WifiAwareClientState) clients.valueAt(i)).getUid() == uid) {
                currentConcurrentCount++;
            }
        }
        synchronized (this.mLock) {
            AttachData data = (AttachData) this.mAttachDataByUid.get(Integer.valueOf(uid));
            if (data == null) {
                data = new AttachData();
                this.mAttachDataByUid.put(Integer.valueOf(uid), data);
            }
            data.mUsesIdentityCallback |= usesIdentityCallback;
            data.mMaxConcurrentAttaches = Math.max(data.mMaxConcurrentAttaches, currentConcurrentCount);
            recordAttachStatus(0);
        }
    }

    public void recordAttachStatus(int status) {
        synchronized (this.mLock) {
            this.mAttachStatusData.put(status, this.mAttachStatusData.get(status) + 1);
        }
    }

    public void recordAttachSessionDuration(long creationTime) {
        synchronized (this.mLock) {
            addLogValueToHistogram(this.mClock.getElapsedSinceBootMillis() - creationTime, this.mHistogramAttachDuration, DURATION_LOG_HISTOGRAM);
        }
    }

    public void recordDiscoverySession(int uid, boolean isPublish, SparseArray<WifiAwareClientState> clients) {
        int numPublishesInSystem = 0;
        int numSubscribesInSystem = 0;
        int numPublishesOnUid = 0;
        int numSubscribesOnUid = 0;
        for (int i = 0; i < clients.size(); i++) {
            WifiAwareClientState client = (WifiAwareClientState) clients.valueAt(i);
            boolean sameUid = client.getUid() == uid;
            SparseArray<WifiAwareDiscoverySessionState> sessions = client.getSessions();
            for (int j = 0; j < sessions.size(); j++) {
                if (((WifiAwareDiscoverySessionState) sessions.valueAt(j)).isPublishSession()) {
                    numPublishesInSystem++;
                    if (sameUid) {
                        numPublishesOnUid++;
                    }
                } else {
                    numSubscribesInSystem++;
                    if (sameUid) {
                        numSubscribesOnUid++;
                    }
                }
            }
        }
        synchronized (this.mLock) {
            this.mMaxPublishInApp = Math.max(this.mMaxPublishInApp, numPublishesOnUid);
            this.mMaxSubscribeInApp = Math.max(this.mMaxSubscribeInApp, numSubscribesOnUid);
            this.mMaxDiscoveryInApp = Math.max(this.mMaxDiscoveryInApp, numPublishesOnUid + numSubscribesOnUid);
            this.mMaxPublishInSystem = Math.max(this.mMaxPublishInSystem, numPublishesInSystem);
            this.mMaxSubscribeInSystem = Math.max(this.mMaxSubscribeInSystem, numSubscribesInSystem);
            this.mMaxDiscoveryInSystem = Math.max(this.mMaxDiscoveryInSystem, numPublishesInSystem + numSubscribesInSystem);
        }
    }

    public void recordDiscoveryStatus(int uid, int status, boolean isPublish) {
        synchronized (this.mLock) {
            if (isPublish) {
                this.mPublishStatusData.put(status, this.mPublishStatusData.get(status) + 1);
            } else {
                this.mSubscribeStatusData.put(status, this.mSubscribeStatusData.get(status) + 1);
            }
            if (status == 4) {
                this.mAppsWithDiscoverySessionResourceFailure.add(Integer.valueOf(uid));
            }
        }
    }

    public void recordDiscoverySessionDuration(long creationTime, boolean isPublish) {
        synchronized (this.mLock) {
            addLogValueToHistogram(this.mClock.getElapsedSinceBootMillis() - creationTime, isPublish ? this.mHistogramPublishDuration : this.mHistogramSubscribeDuration, DURATION_LOG_HISTOGRAM);
        }
    }

    public void recordNdpCreation(int uid, Map<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> networkRequestCache) {
        int numNdpInApp = 0;
        int numSecureNdpInApp = 0;
        int numNdpInSystem = 0;
        int numSecureNdpInSystem = 0;
        Map<String, Integer> ndpPerNdiMap = new HashMap();
        Set<String> ndiInApp = new HashSet();
        Set<String> ndiInSystem = new HashSet();
        for (AwareNetworkRequestInformation anri : networkRequestCache.values()) {
            if (anri.state == 102) {
                boolean sameUid = anri.uid == uid;
                boolean isSecure = TextUtils.isEmpty(anri.networkSpecifier.passphrase) ? anri.networkSpecifier.pmk != null ? anri.networkSpecifier.pmk.length != 0 : false : true;
                if (sameUid) {
                    numNdpInApp++;
                    if (isSecure) {
                        numSecureNdpInApp++;
                    }
                    ndiInApp.add(anri.interfaceName);
                }
                numNdpInSystem++;
                if (isSecure) {
                    numSecureNdpInSystem++;
                }
                Integer ndpCount = (Integer) ndpPerNdiMap.get(anri.interfaceName);
                if (ndpCount == null) {
                    ndpPerNdiMap.put(anri.interfaceName, Integer.valueOf(1));
                } else {
                    ndpPerNdiMap.put(anri.interfaceName, Integer.valueOf(ndpCount.intValue() + 1));
                }
                ndiInSystem.add(anri.interfaceName);
            }
        }
        synchronized (this.mLock) {
            this.mMaxNdiInApp = Math.max(this.mMaxNdiInApp, ndiInApp.size());
            this.mMaxNdpInApp = Math.max(this.mMaxNdpInApp, numNdpInApp);
            this.mMaxSecureNdpInApp = Math.max(this.mMaxSecureNdpInApp, numSecureNdpInApp);
            this.mMaxNdiInSystem = Math.max(this.mMaxNdiInSystem, ndiInSystem.size());
            this.mMaxNdpInSystem = Math.max(this.mMaxNdpInSystem, numNdpInSystem);
            this.mMaxSecureNdpInSystem = Math.max(this.mMaxSecureNdpInSystem, numSecureNdpInSystem);
            this.mMaxNdpPerNdi = Math.max(this.mMaxNdpPerNdi, ((Integer) Collections.max(ndpPerNdiMap.values())).intValue());
        }
    }

    public void recordNdpStatus(int status, boolean isOutOfBand, long startTimestamp) {
        synchronized (this.mLock) {
            if (isOutOfBand) {
                this.mOutOfBandNdpStatusData.put(status, this.mOutOfBandNdpStatusData.get(status) + 1);
            } else {
                this.mInBandNdpStatusData.put(status, this.mOutOfBandNdpStatusData.get(status) + 1);
            }
            if (status == 0) {
                long creationTime = this.mClock.getElapsedSinceBootMillis() - startTimestamp;
                addLogValueToHistogram(creationTime, this.mNdpCreationTimeDuration, DURATION_LOG_HISTOGRAM);
                this.mNdpCreationTimeMin = this.mNdpCreationTimeMin == -1 ? creationTime : Math.min(this.mNdpCreationTimeMin, creationTime);
                this.mNdpCreationTimeMax = Math.max(this.mNdpCreationTimeMax, creationTime);
                this.mNdpCreationTimeSum += creationTime;
                this.mNdpCreationTimeSumSq += creationTime * creationTime;
                this.mNdpCreationTimeNumSamples++;
            }
        }
    }

    public void recordNdpSessionDuration(long creationTime) {
        synchronized (this.mLock) {
            addLogValueToHistogram(this.mClock.getElapsedSinceBootMillis() - creationTime, this.mHistogramNdpDuration, DURATION_LOG_HISTOGRAM);
        }
    }

    public WifiAwareLog consolidateProto() {
        WifiAwareLog log = new WifiAwareLog();
        long now = this.mClock.getElapsedSinceBootMillis();
        synchronized (this.mLock) {
            log.histogramAwareAvailableDurationMs = histogramToProtoArray(this.mHistogramAwareAvailableDurationMs, DURATION_LOG_HISTOGRAM);
            log.availableTimeMs = this.mAvailableTimeMs;
            if (this.mLastEnableUsageInThisSampleWindowMs != 0) {
                log.availableTimeMs += now - this.mLastEnableUsageInThisSampleWindowMs;
            }
            log.histogramAwareEnabledDurationMs = histogramToProtoArray(this.mHistogramAwareEnabledDurationMs, DURATION_LOG_HISTOGRAM);
            log.enabledTimeMs = this.mEnabledTimeMs;
            if (this.mLastEnableAwareInThisSampleWindowMs != 0) {
                log.enabledTimeMs += now - this.mLastEnableAwareInThisSampleWindowMs;
            }
            log.numApps = this.mAttachDataByUid.size();
            log.numAppsUsingIdentityCallback = 0;
            log.maxConcurrentAttachSessionsInApp = 0;
            for (AttachData ad : this.mAttachDataByUid.values()) {
                if (ad.mUsesIdentityCallback) {
                    log.numAppsUsingIdentityCallback++;
                }
                log.maxConcurrentAttachSessionsInApp = Math.max(log.maxConcurrentAttachSessionsInApp, ad.mMaxConcurrentAttaches);
            }
            log.histogramAttachSessionStatus = histogramToProtoArray(this.mAttachStatusData);
            log.histogramAttachDurationMs = histogramToProtoArray(this.mHistogramAttachDuration, DURATION_LOG_HISTOGRAM);
            log.maxConcurrentPublishInApp = this.mMaxPublishInApp;
            log.maxConcurrentSubscribeInApp = this.mMaxSubscribeInApp;
            log.maxConcurrentDiscoverySessionsInApp = this.mMaxDiscoveryInApp;
            log.maxConcurrentPublishInSystem = this.mMaxPublishInSystem;
            log.maxConcurrentSubscribeInSystem = this.mMaxSubscribeInSystem;
            log.maxConcurrentDiscoverySessionsInSystem = this.mMaxDiscoveryInSystem;
            log.histogramPublishStatus = histogramToProtoArray(this.mPublishStatusData);
            log.histogramSubscribeStatus = histogramToProtoArray(this.mSubscribeStatusData);
            log.numAppsWithDiscoverySessionFailureOutOfResources = this.mAppsWithDiscoverySessionResourceFailure.size();
            log.histogramPublishSessionDurationMs = histogramToProtoArray(this.mHistogramPublishDuration, DURATION_LOG_HISTOGRAM);
            log.histogramSubscribeSessionDurationMs = histogramToProtoArray(this.mHistogramSubscribeDuration, DURATION_LOG_HISTOGRAM);
            log.maxConcurrentNdiInApp = this.mMaxNdiInApp;
            log.maxConcurrentNdiInSystem = this.mMaxNdiInSystem;
            log.maxConcurrentNdpInApp = this.mMaxNdpInApp;
            log.maxConcurrentNdpInSystem = this.mMaxNdpInSystem;
            log.maxConcurrentSecureNdpInApp = this.mMaxSecureNdpInApp;
            log.maxConcurrentSecureNdpInSystem = this.mMaxSecureNdpInSystem;
            log.maxConcurrentNdpPerNdi = this.mMaxNdpPerNdi;
            log.histogramRequestNdpStatus = histogramToProtoArray(this.mInBandNdpStatusData);
            log.histogramRequestNdpOobStatus = histogramToProtoArray(this.mOutOfBandNdpStatusData);
            log.histogramNdpCreationTimeMs = histogramToProtoArray(this.mNdpCreationTimeDuration, DURATION_LOG_HISTOGRAM);
            log.ndpCreationTimeMsMin = this.mNdpCreationTimeMin;
            log.ndpCreationTimeMsMax = this.mNdpCreationTimeMax;
            log.ndpCreationTimeMsSum = this.mNdpCreationTimeSum;
            log.ndpCreationTimeMsSumOfSq = this.mNdpCreationTimeSumSq;
            log.ndpCreationTimeMsNumSamples = this.mNdpCreationTimeNumSamples;
            log.histogramNdpSessionDurationMs = histogramToProtoArray(this.mHistogramNdpDuration, DURATION_LOG_HISTOGRAM);
        }
        return log;
    }

    public void clear() {
        long now = this.mClock.getElapsedSinceBootMillis();
        synchronized (this.mLock) {
            this.mHistogramAwareAvailableDurationMs.clear();
            this.mAvailableTimeMs = 0;
            if (this.mLastEnableUsageInThisSampleWindowMs != 0) {
                this.mLastEnableUsageInThisSampleWindowMs = now;
            }
            this.mHistogramAwareEnabledDurationMs.clear();
            this.mEnabledTimeMs = 0;
            if (this.mLastEnableAwareInThisSampleWindowMs != 0) {
                this.mLastEnableAwareInThisSampleWindowMs = now;
            }
            this.mAttachDataByUid.clear();
            this.mAttachStatusData.clear();
            this.mHistogramAttachDuration.clear();
            this.mMaxPublishInApp = 0;
            this.mMaxSubscribeInApp = 0;
            this.mMaxDiscoveryInApp = 0;
            this.mMaxPublishInSystem = 0;
            this.mMaxSubscribeInSystem = 0;
            this.mMaxDiscoveryInSystem = 0;
            this.mPublishStatusData.clear();
            this.mSubscribeStatusData.clear();
            this.mHistogramPublishDuration.clear();
            this.mHistogramSubscribeDuration.clear();
            this.mAppsWithDiscoverySessionResourceFailure.clear();
            this.mMaxNdiInApp = 0;
            this.mMaxNdpInApp = 0;
            this.mMaxSecureNdpInApp = 0;
            this.mMaxNdiInSystem = 0;
            this.mMaxNdpInSystem = 0;
            this.mMaxSecureNdpInSystem = 0;
            this.mMaxNdpPerNdi = 0;
            this.mInBandNdpStatusData.clear();
            this.mOutOfBandNdpStatusData.clear();
            this.mNdpCreationTimeDuration.clear();
            this.mNdpCreationTimeMin = -1;
            this.mNdpCreationTimeMax = 0;
            this.mNdpCreationTimeSum = 0;
            this.mNdpCreationTimeSumSq = 0;
            this.mNdpCreationTimeNumSamples = 0;
            this.mHistogramNdpDuration.clear();
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mLock) {
            int i;
            pw.println("mLastEnableUsageMs:" + this.mLastEnableUsageMs);
            pw.println("mLastEnableUsageInThisSampleWindowMs:" + this.mLastEnableUsageInThisSampleWindowMs);
            pw.println("mAvailableTimeMs:" + this.mAvailableTimeMs);
            pw.println("mHistogramAwareAvailableDurationMs:");
            for (i = 0; i < this.mHistogramAwareAvailableDurationMs.size(); i++) {
                pw.println("  " + this.mHistogramAwareAvailableDurationMs.keyAt(i) + ": " + this.mHistogramAwareAvailableDurationMs.valueAt(i));
            }
            pw.println("mLastEnableAwareMs:" + this.mLastEnableAwareMs);
            pw.println("mLastEnableAwareInThisSampleWindowMs:" + this.mLastEnableAwareInThisSampleWindowMs);
            pw.println("mEnabledTimeMs:" + this.mEnabledTimeMs);
            pw.println("mHistogramAwareEnabledDurationMs:");
            for (i = 0; i < this.mHistogramAwareEnabledDurationMs.size(); i++) {
                pw.println("  " + this.mHistogramAwareEnabledDurationMs.keyAt(i) + ": " + this.mHistogramAwareEnabledDurationMs.valueAt(i));
            }
            pw.println("mAttachDataByUid:");
            for (Entry<Integer, AttachData> ade : this.mAttachDataByUid.entrySet()) {
                pw.println("  uid=" + ade.getKey() + ": identity=" + ((AttachData) ade.getValue()).mUsesIdentityCallback + ", maxConcurrent=" + ((AttachData) ade.getValue()).mMaxConcurrentAttaches);
            }
            pw.println("mAttachStatusData:");
            for (i = 0; i < this.mAttachStatusData.size(); i++) {
                pw.println("  " + this.mAttachStatusData.keyAt(i) + ": " + this.mAttachStatusData.valueAt(i));
            }
            pw.println("mHistogramAttachDuration:");
            for (i = 0; i < this.mHistogramAttachDuration.size(); i++) {
                pw.println("  " + this.mHistogramAttachDuration.keyAt(i) + ": " + this.mHistogramAttachDuration.valueAt(i));
            }
            pw.println("mMaxPublishInApp:" + this.mMaxPublishInApp);
            pw.println("mMaxSubscribeInApp:" + this.mMaxSubscribeInApp);
            pw.println("mMaxDiscoveryInApp:" + this.mMaxDiscoveryInApp);
            pw.println("mMaxPublishInSystem:" + this.mMaxPublishInSystem);
            pw.println("mMaxSubscribeInSystem:" + this.mMaxSubscribeInSystem);
            pw.println("mMaxDiscoveryInSystem:" + this.mMaxDiscoveryInSystem);
            pw.println("mPublishStatusData:");
            for (i = 0; i < this.mPublishStatusData.size(); i++) {
                pw.println("  " + this.mPublishStatusData.keyAt(i) + ": " + this.mPublishStatusData.valueAt(i));
            }
            pw.println("mSubscribeStatusData:");
            for (i = 0; i < this.mSubscribeStatusData.size(); i++) {
                pw.println("  " + this.mSubscribeStatusData.keyAt(i) + ": " + this.mSubscribeStatusData.valueAt(i));
            }
            pw.println("mHistogramPublishDuration:");
            for (i = 0; i < this.mHistogramPublishDuration.size(); i++) {
                pw.println("  " + this.mHistogramPublishDuration.keyAt(i) + ": " + this.mHistogramPublishDuration.valueAt(i));
            }
            pw.println("mHistogramSubscribeDuration:");
            for (i = 0; i < this.mHistogramSubscribeDuration.size(); i++) {
                pw.println("  " + this.mHistogramSubscribeDuration.keyAt(i) + ": " + this.mHistogramSubscribeDuration.valueAt(i));
            }
            pw.println("mAppsWithDiscoverySessionResourceFailure:");
            for (Integer uid : this.mAppsWithDiscoverySessionResourceFailure) {
                pw.println("  " + uid);
            }
            pw.println("mMaxNdiInApp:" + this.mMaxNdiInApp);
            pw.println("mMaxNdpInApp:" + this.mMaxNdpInApp);
            pw.println("mMaxSecureNdpInApp:" + this.mMaxSecureNdpInApp);
            pw.println("mMaxNdiInSystem:" + this.mMaxNdiInSystem);
            pw.println("mMaxNdpInSystem:" + this.mMaxNdpInSystem);
            pw.println("mMaxSecureNdpInSystem:" + this.mMaxSecureNdpInSystem);
            pw.println("mMaxNdpPerNdi:" + this.mMaxNdpPerNdi);
            pw.println("mInBandNdpStatusData:");
            for (i = 0; i < this.mInBandNdpStatusData.size(); i++) {
                pw.println("  " + this.mInBandNdpStatusData.keyAt(i) + ": " + this.mInBandNdpStatusData.valueAt(i));
            }
            pw.println("mOutOfBandNdpStatusData:");
            for (i = 0; i < this.mOutOfBandNdpStatusData.size(); i++) {
                pw.println("  " + this.mOutOfBandNdpStatusData.keyAt(i) + ": " + this.mOutOfBandNdpStatusData.valueAt(i));
            }
            pw.println("mNdpCreationTimeDuration:");
            for (i = 0; i < this.mNdpCreationTimeDuration.size(); i++) {
                pw.println("  " + this.mNdpCreationTimeDuration.keyAt(i) + ": " + this.mNdpCreationTimeDuration.valueAt(i));
            }
            pw.println("mNdpCreationTimeMin:" + this.mNdpCreationTimeMin);
            pw.println("mNdpCreationTimeMax:" + this.mNdpCreationTimeMax);
            pw.println("mNdpCreationTimeSum:" + this.mNdpCreationTimeSum);
            pw.println("mNdpCreationTimeSumSq:" + this.mNdpCreationTimeSumSq);
            pw.println("mNdpCreationTimeNumSamples:" + this.mNdpCreationTimeNumSamples);
            pw.println("mHistogramNdpDuration:");
            for (i = 0; i < this.mHistogramNdpDuration.size(); i++) {
                pw.println("  " + this.mHistogramNdpDuration.keyAt(i) + ": " + this.mHistogramNdpDuration.valueAt(i));
            }
        }
    }

    public static int addLogValueToHistogram(long x, SparseIntArray histogram, HistParms hp) {
        int subBucketIndex;
        double logArg = ((double) (x - ((long) hp.b))) / ((double) hp.p);
        int bigBucketIndex = -1;
        if (logArg > 0.0d) {
            bigBucketIndex = (int) (Math.log(logArg) / hp.mLog);
        }
        if (bigBucketIndex < 0) {
            bigBucketIndex = 0;
            subBucketIndex = 0;
        } else if (bigBucketIndex >= hp.n) {
            bigBucketIndex = hp.n - 1;
            subBucketIndex = hp.s - 1;
        } else {
            subBucketIndex = (int) ((((double) x) - hp.bb[bigBucketIndex]) / hp.sbw[bigBucketIndex]);
            if (subBucketIndex >= hp.s) {
                bigBucketIndex++;
                if (bigBucketIndex >= hp.n) {
                    bigBucketIndex = hp.n - 1;
                    subBucketIndex = hp.s - 1;
                } else {
                    subBucketIndex = (int) ((((double) x) - hp.bb[bigBucketIndex]) / hp.sbw[bigBucketIndex]);
                }
            }
        }
        int key = (hp.s * bigBucketIndex) + subBucketIndex;
        int newValue = histogram.get(key) + 1;
        histogram.put(key, newValue);
        return newValue;
    }

    public static HistogramBucket[] histogramToProtoArray(SparseIntArray histogram, HistParms hp) {
        HistogramBucket[] protoArray = new HistogramBucket[histogram.size()];
        for (int i = 0; i < histogram.size(); i++) {
            int key = histogram.keyAt(i);
            protoArray[i] = new HistogramBucket();
            protoArray[i].start = (long) (hp.bb[key / hp.s] + (hp.sbw[key / hp.s] * ((double) (key % hp.s))));
            protoArray[i].end = (long) (((double) protoArray[i].start) + hp.sbw[key / hp.s]);
            protoArray[i].count = histogram.valueAt(i);
        }
        return protoArray;
    }

    public static void addNanHalStatusToHistogram(int halStatus, SparseIntArray histogram) {
        int protoStatus = convertNanStatusTypeToProtoEnum(halStatus);
        histogram.put(protoStatus, histogram.get(protoStatus) + 1);
    }

    public static NanStatusHistogramBucket[] histogramToProtoArray(SparseIntArray histogram) {
        NanStatusHistogramBucket[] protoArray = new NanStatusHistogramBucket[histogram.size()];
        for (int i = 0; i < histogram.size(); i++) {
            protoArray[i] = new NanStatusHistogramBucket();
            protoArray[i].nanStatusType = histogram.keyAt(i);
            protoArray[i].count = histogram.valueAt(i);
        }
        return protoArray;
    }

    public static int convertNanStatusTypeToProtoEnum(int nanStatusType) {
        switch (nanStatusType) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 6;
            case 6:
                return 7;
            case 7:
                return 8;
            case 8:
                return 9;
            case 9:
                return 10;
            case 10:
                return 11;
            case 11:
                return 12;
            case 12:
                return 13;
            default:
                Log.e(TAG, "Unrecognized NanStatusType: " + nanStatusType);
                return 14;
        }
    }
}
