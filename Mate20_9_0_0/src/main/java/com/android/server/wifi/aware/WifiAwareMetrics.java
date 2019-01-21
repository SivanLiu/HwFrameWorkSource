package com.android.server.wifi.aware;

import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.Clock;
import com.android.server.wifi.aware.WifiAwareDataPathStateManager.AwareNetworkRequestInformation;
import com.android.server.wifi.nano.WifiMetricsProto.WifiAwareLog;
import com.android.server.wifi.nano.WifiMetricsProto.WifiAwareLog.HistogramBucket;
import com.android.server.wifi.nano.WifiMetricsProto.WifiAwareLog.NanStatusHistogramBucket;
import com.android.server.wifi.util.MetricsUtils;
import com.android.server.wifi.util.MetricsUtils.GenericBucket;
import com.android.server.wifi.util.MetricsUtils.LogHistParms;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class WifiAwareMetrics {
    private static final LogHistParms DURATION_LOG_HISTOGRAM = new LogHistParms(0, 1, 10, 9, 8);
    private static final int[] RANGING_LIMIT_METERS = new int[]{10, 30, 60, 100};
    private static final String TAG = "WifiAwareMetrics";
    private static final boolean VDBG = false;
    private Set<Integer> mAppsWithDiscoverySessionResourceFailure = new HashSet();
    private Map<Integer, AttachData> mAttachDataByUid = new HashMap();
    private SparseIntArray mAttachStatusData = new SparseIntArray();
    private long mAvailableTimeMs = 0;
    private final Clock mClock;
    boolean mDbg = false;
    private long mEnabledTimeMs = 0;
    private SparseIntArray mHistogramAttachDuration = new SparseIntArray();
    private SparseIntArray mHistogramAwareAvailableDurationMs = new SparseIntArray();
    private SparseIntArray mHistogramAwareEnabledDurationMs = new SparseIntArray();
    private SparseIntArray mHistogramNdpDuration = new SparseIntArray();
    private SparseIntArray mHistogramPublishDuration = new SparseIntArray();
    private SparseIntArray mHistogramSubscribeDuration = new SparseIntArray();
    private SparseIntArray mHistogramSubscribeGeofenceMax = new SparseIntArray();
    private SparseIntArray mHistogramSubscribeGeofenceMin = new SparseIntArray();
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
    private int mMaxPublishWithRangingInApp = 0;
    private int mMaxPublishWithRangingInSystem = 0;
    private int mMaxSecureNdpInApp = 0;
    private int mMaxSecureNdpInSystem = 0;
    private int mMaxSubscribeInApp = 0;
    private int mMaxSubscribeInSystem = 0;
    private int mMaxSubscribeWithRangingInApp = 0;
    private int mMaxSubscribeWithRangingInSystem = 0;
    private SparseIntArray mNdpCreationTimeDuration = new SparseIntArray();
    private long mNdpCreationTimeMax = 0;
    private long mNdpCreationTimeMin = -1;
    private long mNdpCreationTimeNumSamples = 0;
    private long mNdpCreationTimeSum = 0;
    private long mNdpCreationTimeSumSq = 0;
    private int mNumMatchesWithRanging = 0;
    private int mNumMatchesWithoutRangingForRangingEnabledSubscribes = 0;
    private int mNumSubscribesWithRanging = 0;
    private SparseIntArray mOutOfBandNdpStatusData = new SparseIntArray();
    private SparseIntArray mPublishStatusData = new SparseIntArray();
    private SparseIntArray mSubscribeStatusData = new SparseIntArray();

    private static class AttachData {
        int mMaxConcurrentAttaches;
        boolean mUsesIdentityCallback;

        private AttachData() {
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
            MetricsUtils.addValueToLogHistogram(now - this.mLastEnableUsageMs, this.mHistogramAwareAvailableDurationMs, DURATION_LOG_HISTOGRAM);
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
            MetricsUtils.addValueToLogHistogram(now - this.mLastEnableAwareMs, this.mHistogramAwareEnabledDurationMs, DURATION_LOG_HISTOGRAM);
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
            MetricsUtils.addValueToLogHistogram(this.mClock.getElapsedSinceBootMillis() - creationTime, this.mHistogramAttachDuration, DURATION_LOG_HISTOGRAM);
        }
    }

    public void recordDiscoverySession(int uid, SparseArray<WifiAwareClientState> clients) {
        recordDiscoverySessionInternal(uid, clients, false, -1, -1);
    }

    public void recordDiscoverySessionWithRanging(int uid, boolean isSubscriberWithRanging, int minRange, int maxRange, SparseArray<WifiAwareClientState> clients) {
        recordDiscoverySessionInternal(uid, clients, isSubscriberWithRanging, minRange, maxRange);
    }

    private void recordDiscoverySessionInternal(int uid, SparseArray<WifiAwareClientState> clients, boolean isRangingEnabledSubscriber, int minRange, int maxRange) {
        int i = minRange;
        int i2 = maxRange;
        int numPublishesOnUid = 0;
        int numSubscribesOnUid = 0;
        int numPublishesWithRangingInSystem = 0;
        int numSubscribesWithRangingInSystem = 0;
        int numSubscribesWithRangingOnUid = 0;
        int numPublishesWithRangingOnUid = 0;
        int numSubscribesInSystem = 0;
        int numPublishesInSystem = 0;
        int i3 = 0;
        while (i3 < clients.size()) {
            WifiAwareClientState client = (WifiAwareClientState) clients.valueAt(i3);
            boolean sameUid = client.getUid() == uid;
            SparseArray<WifiAwareDiscoverySessionState> sessions = client.getSessions();
            int numSubscribesWithRangingOnUid2 = numSubscribesWithRangingOnUid;
            numSubscribesWithRangingOnUid = numPublishesWithRangingOnUid;
            numPublishesWithRangingOnUid = numSubscribesWithRangingInSystem;
            numSubscribesWithRangingInSystem = numPublishesWithRangingInSystem;
            numPublishesWithRangingInSystem = numSubscribesOnUid;
            numSubscribesOnUid = numPublishesInSystem;
            numPublishesInSystem = 0;
            while (numPublishesInSystem < sessions.size()) {
                WifiAwareDiscoverySessionState session = (WifiAwareDiscoverySessionState) sessions.valueAt(numPublishesInSystem);
                boolean isRangingEnabledForThisSession = session.isRangingEnabled();
                if (session.isPublishSession()) {
                    numSubscribesOnUid++;
                    if (isRangingEnabledForThisSession) {
                        numSubscribesWithRangingInSystem++;
                    }
                    if (sameUid) {
                        numPublishesOnUid++;
                        if (isRangingEnabledForThisSession) {
                            numSubscribesWithRangingOnUid++;
                        }
                    }
                } else {
                    numSubscribesInSystem++;
                    if (isRangingEnabledForThisSession) {
                        numPublishesWithRangingOnUid++;
                    }
                    if (sameUid) {
                        numPublishesWithRangingInSystem++;
                        if (isRangingEnabledForThisSession) {
                            numSubscribesWithRangingOnUid2++;
                        }
                    }
                }
                numPublishesInSystem++;
                int i4 = uid;
            }
            i3++;
            numPublishesInSystem = numSubscribesOnUid;
            numSubscribesOnUid = numPublishesWithRangingInSystem;
            numPublishesWithRangingInSystem = numSubscribesWithRangingInSystem;
            numSubscribesWithRangingInSystem = numPublishesWithRangingOnUid;
            numPublishesWithRangingOnUid = numSubscribesWithRangingOnUid;
            numSubscribesWithRangingOnUid = numSubscribesWithRangingOnUid2;
        }
        synchronized (this.mLock) {
            this.mMaxPublishInApp = Math.max(this.mMaxPublishInApp, numPublishesOnUid);
            this.mMaxSubscribeInApp = Math.max(this.mMaxSubscribeInApp, numSubscribesOnUid);
            this.mMaxDiscoveryInApp = Math.max(this.mMaxDiscoveryInApp, numPublishesOnUid + numSubscribesOnUid);
            this.mMaxPublishInSystem = Math.max(this.mMaxPublishInSystem, numPublishesInSystem);
            this.mMaxSubscribeInSystem = Math.max(this.mMaxSubscribeInSystem, numSubscribesInSystem);
            this.mMaxDiscoveryInSystem = Math.max(this.mMaxDiscoveryInSystem, numPublishesInSystem + numSubscribesInSystem);
            this.mMaxPublishWithRangingInApp = Math.max(this.mMaxPublishWithRangingInApp, numPublishesWithRangingOnUid);
            this.mMaxSubscribeWithRangingInApp = Math.max(this.mMaxSubscribeWithRangingInApp, numSubscribesWithRangingOnUid);
            this.mMaxPublishWithRangingInSystem = Math.max(this.mMaxPublishWithRangingInSystem, numPublishesWithRangingInSystem);
            this.mMaxSubscribeWithRangingInSystem = Math.max(this.mMaxSubscribeWithRangingInSystem, numSubscribesWithRangingInSystem);
            if (isRangingEnabledSubscriber) {
                this.mNumSubscribesWithRanging++;
            }
            if (i != -1) {
                MetricsUtils.addValueToLinearHistogram(i, this.mHistogramSubscribeGeofenceMin, RANGING_LIMIT_METERS);
            }
            if (i2 != -1) {
                MetricsUtils.addValueToLinearHistogram(i2, this.mHistogramSubscribeGeofenceMax, RANGING_LIMIT_METERS);
            }
        }
    }

    public void recordDiscoveryStatus(int uid, int status, boolean isPublish) {
        synchronized (this.mLock) {
            if (isPublish) {
                try {
                    this.mPublishStatusData.put(status, this.mPublishStatusData.get(status) + 1);
                } catch (Throwable th) {
                }
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
            MetricsUtils.addValueToLogHistogram(this.mClock.getElapsedSinceBootMillis() - creationTime, isPublish ? this.mHistogramPublishDuration : this.mHistogramSubscribeDuration, DURATION_LOG_HISTOGRAM);
        }
    }

    public void recordMatchIndicationForRangeEnabledSubscribe(boolean rangeProvided) {
        if (rangeProvided) {
            this.mNumMatchesWithRanging++;
        } else {
            this.mNumMatchesWithoutRangingForRangingEnabledSubscribes++;
        }
    }

    public void recordNdpCreation(int uid, Map<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> networkRequestCache) {
        Map<String, Integer> ndpPerNdiMap = new HashMap();
        Set<String> ndiInApp = new HashSet();
        Set<String> ndiInSystem = new HashSet();
        int numSecureNdpInSystem = 0;
        int numNdpInSystem = 0;
        int numSecureNdpInApp = 0;
        int numNdpInApp = 0;
        for (AwareNetworkRequestInformation anri : networkRequestCache.values()) {
            if (anri.state == 102) {
                boolean isSecure = false;
                boolean sameUid = anri.uid == uid;
                if (!(TextUtils.isEmpty(anri.networkSpecifier.passphrase) && (anri.networkSpecifier.pmk == null || anri.networkSpecifier.pmk.length == 0))) {
                    isSecure = true;
                }
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
        int i = uid;
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
                try {
                    this.mOutOfBandNdpStatusData.put(status, this.mOutOfBandNdpStatusData.get(status) + 1);
                } catch (Throwable th) {
                }
            } else {
                this.mInBandNdpStatusData.put(status, this.mOutOfBandNdpStatusData.get(status) + 1);
            }
            if (status == 0) {
                long creationTime = this.mClock.getElapsedSinceBootMillis() - startTimestamp;
                MetricsUtils.addValueToLogHistogram(creationTime, this.mNdpCreationTimeDuration, DURATION_LOG_HISTOGRAM);
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
            MetricsUtils.addValueToLogHistogram(this.mClock.getElapsedSinceBootMillis() - creationTime, this.mHistogramNdpDuration, DURATION_LOG_HISTOGRAM);
        }
    }

    public WifiAwareLog consolidateProto() {
        WifiAwareLog log = new WifiAwareLog();
        long now = this.mClock.getElapsedSinceBootMillis();
        synchronized (this.mLock) {
            log.histogramAwareAvailableDurationMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mHistogramAwareAvailableDurationMs, DURATION_LOG_HISTOGRAM));
            log.availableTimeMs = this.mAvailableTimeMs;
            if (this.mLastEnableUsageInThisSampleWindowMs != 0) {
                log.availableTimeMs += now - this.mLastEnableUsageInThisSampleWindowMs;
            }
            log.histogramAwareEnabledDurationMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mHistogramAwareEnabledDurationMs, DURATION_LOG_HISTOGRAM));
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
            log.histogramAttachDurationMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mHistogramAttachDuration, DURATION_LOG_HISTOGRAM));
            log.maxConcurrentPublishInApp = this.mMaxPublishInApp;
            log.maxConcurrentSubscribeInApp = this.mMaxSubscribeInApp;
            log.maxConcurrentDiscoverySessionsInApp = this.mMaxDiscoveryInApp;
            log.maxConcurrentPublishInSystem = this.mMaxPublishInSystem;
            log.maxConcurrentSubscribeInSystem = this.mMaxSubscribeInSystem;
            log.maxConcurrentDiscoverySessionsInSystem = this.mMaxDiscoveryInSystem;
            log.histogramPublishStatus = histogramToProtoArray(this.mPublishStatusData);
            log.histogramSubscribeStatus = histogramToProtoArray(this.mSubscribeStatusData);
            log.numAppsWithDiscoverySessionFailureOutOfResources = this.mAppsWithDiscoverySessionResourceFailure.size();
            log.histogramPublishSessionDurationMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mHistogramPublishDuration, DURATION_LOG_HISTOGRAM));
            log.histogramSubscribeSessionDurationMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mHistogramSubscribeDuration, DURATION_LOG_HISTOGRAM));
            log.maxConcurrentPublishWithRangingInApp = this.mMaxPublishWithRangingInApp;
            log.maxConcurrentSubscribeWithRangingInApp = this.mMaxSubscribeWithRangingInApp;
            log.maxConcurrentPublishWithRangingInSystem = this.mMaxPublishWithRangingInSystem;
            log.maxConcurrentSubscribeWithRangingInSystem = this.mMaxSubscribeWithRangingInSystem;
            log.histogramSubscribeGeofenceMin = histogramToProtoArray(MetricsUtils.linearHistogramToGenericBuckets(this.mHistogramSubscribeGeofenceMin, RANGING_LIMIT_METERS));
            log.histogramSubscribeGeofenceMax = histogramToProtoArray(MetricsUtils.linearHistogramToGenericBuckets(this.mHistogramSubscribeGeofenceMax, RANGING_LIMIT_METERS));
            log.numSubscribesWithRanging = this.mNumSubscribesWithRanging;
            log.numMatchesWithRanging = this.mNumMatchesWithRanging;
            log.numMatchesWithoutRangingForRangingEnabledSubscribes = this.mNumMatchesWithoutRangingForRangingEnabledSubscribes;
            log.maxConcurrentNdiInApp = this.mMaxNdiInApp;
            log.maxConcurrentNdiInSystem = this.mMaxNdiInSystem;
            log.maxConcurrentNdpInApp = this.mMaxNdpInApp;
            log.maxConcurrentNdpInSystem = this.mMaxNdpInSystem;
            log.maxConcurrentSecureNdpInApp = this.mMaxSecureNdpInApp;
            log.maxConcurrentSecureNdpInSystem = this.mMaxSecureNdpInSystem;
            log.maxConcurrentNdpPerNdi = this.mMaxNdpPerNdi;
            log.histogramRequestNdpStatus = histogramToProtoArray(this.mInBandNdpStatusData);
            log.histogramRequestNdpOobStatus = histogramToProtoArray(this.mOutOfBandNdpStatusData);
            log.histogramNdpCreationTimeMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mNdpCreationTimeDuration, DURATION_LOG_HISTOGRAM));
            log.ndpCreationTimeMsMin = this.mNdpCreationTimeMin;
            log.ndpCreationTimeMsMax = this.mNdpCreationTimeMax;
            log.ndpCreationTimeMsSum = this.mNdpCreationTimeSum;
            log.ndpCreationTimeMsSumOfSq = this.mNdpCreationTimeSumSq;
            log.ndpCreationTimeMsNumSamples = this.mNdpCreationTimeNumSamples;
            log.histogramNdpSessionDurationMs = histogramToProtoArray(MetricsUtils.logHistogramToGenericBuckets(this.mHistogramNdpDuration, DURATION_LOG_HISTOGRAM));
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
            this.mMaxPublishWithRangingInApp = 0;
            this.mMaxSubscribeWithRangingInApp = 0;
            this.mMaxPublishWithRangingInSystem = 0;
            this.mMaxSubscribeWithRangingInSystem = 0;
            this.mHistogramSubscribeGeofenceMin.clear();
            this.mHistogramSubscribeGeofenceMax.clear();
            this.mNumSubscribesWithRanging = 0;
            this.mNumMatchesWithRanging = 0;
            this.mNumMatchesWithoutRangingForRangingEnabledSubscribes = 0;
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
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("mLastEnableUsageMs:");
            stringBuilder3.append(this.mLastEnableUsageMs);
            pw.println(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("mLastEnableUsageInThisSampleWindowMs:");
            stringBuilder3.append(this.mLastEnableUsageInThisSampleWindowMs);
            pw.println(stringBuilder3.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("mAvailableTimeMs:");
            stringBuilder3.append(this.mAvailableTimeMs);
            pw.println(stringBuilder3.toString());
            pw.println("mHistogramAwareAvailableDurationMs:");
            int i2 = 0;
            for (i = 0; i < this.mHistogramAwareAvailableDurationMs.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mHistogramAwareAvailableDurationMs.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mHistogramAwareAvailableDurationMs.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mLastEnableAwareMs:");
            stringBuilder4.append(this.mLastEnableAwareMs);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mLastEnableAwareInThisSampleWindowMs:");
            stringBuilder4.append(this.mLastEnableAwareInThisSampleWindowMs);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mEnabledTimeMs:");
            stringBuilder4.append(this.mEnabledTimeMs);
            pw.println(stringBuilder4.toString());
            pw.println("mHistogramAwareEnabledDurationMs:");
            for (i = 0; i < this.mHistogramAwareEnabledDurationMs.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mHistogramAwareEnabledDurationMs.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mHistogramAwareEnabledDurationMs.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            pw.println("mAttachDataByUid:");
            for (Entry<Integer, AttachData> ade : this.mAttachDataByUid.entrySet()) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  uid=");
                stringBuilder2.append(ade.getKey());
                stringBuilder2.append(": identity=");
                stringBuilder2.append(((AttachData) ade.getValue()).mUsesIdentityCallback);
                stringBuilder2.append(", maxConcurrent=");
                stringBuilder2.append(((AttachData) ade.getValue()).mMaxConcurrentAttaches);
                pw.println(stringBuilder2.toString());
            }
            pw.println("mAttachStatusData:");
            for (i = 0; i < this.mAttachStatusData.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mAttachStatusData.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mAttachStatusData.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            pw.println("mHistogramAttachDuration:");
            for (i = 0; i < this.mHistogramAttachDuration.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mHistogramAttachDuration.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mHistogramAttachDuration.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxPublishInApp:");
            stringBuilder4.append(this.mMaxPublishInApp);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxSubscribeInApp:");
            stringBuilder4.append(this.mMaxSubscribeInApp);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxDiscoveryInApp:");
            stringBuilder4.append(this.mMaxDiscoveryInApp);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxPublishInSystem:");
            stringBuilder4.append(this.mMaxPublishInSystem);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxSubscribeInSystem:");
            stringBuilder4.append(this.mMaxSubscribeInSystem);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxDiscoveryInSystem:");
            stringBuilder4.append(this.mMaxDiscoveryInSystem);
            pw.println(stringBuilder4.toString());
            pw.println("mPublishStatusData:");
            for (i = 0; i < this.mPublishStatusData.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mPublishStatusData.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mPublishStatusData.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            pw.println("mSubscribeStatusData:");
            for (i = 0; i < this.mSubscribeStatusData.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mSubscribeStatusData.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mSubscribeStatusData.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            pw.println("mHistogramPublishDuration:");
            for (i = 0; i < this.mHistogramPublishDuration.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mHistogramPublishDuration.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mHistogramPublishDuration.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            pw.println("mHistogramSubscribeDuration:");
            for (i = 0; i < this.mHistogramSubscribeDuration.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mHistogramSubscribeDuration.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mHistogramSubscribeDuration.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            pw.println("mAppsWithDiscoverySessionResourceFailure:");
            for (Integer uid : this.mAppsWithDiscoverySessionResourceFailure) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  ");
                stringBuilder2.append(uid);
                pw.println(stringBuilder2.toString());
            }
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxPublishWithRangingInApp:");
            stringBuilder4.append(this.mMaxPublishWithRangingInApp);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxSubscribeWithRangingInApp:");
            stringBuilder4.append(this.mMaxSubscribeWithRangingInApp);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxPublishWithRangingInSystem:");
            stringBuilder4.append(this.mMaxPublishWithRangingInSystem);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxSubscribeWithRangingInSystem:");
            stringBuilder4.append(this.mMaxSubscribeWithRangingInSystem);
            pw.println(stringBuilder4.toString());
            pw.println("mHistogramSubscribeGeofenceMin:");
            for (i = 0; i < this.mHistogramSubscribeGeofenceMin.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mHistogramSubscribeGeofenceMin.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mHistogramSubscribeGeofenceMin.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            pw.println("mHistogramSubscribeGeofenceMax:");
            for (i = 0; i < this.mHistogramSubscribeGeofenceMax.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mHistogramSubscribeGeofenceMax.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mHistogramSubscribeGeofenceMax.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mNumSubscribesWithRanging:");
            stringBuilder4.append(this.mNumSubscribesWithRanging);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mNumMatchesWithRanging:");
            stringBuilder4.append(this.mNumMatchesWithRanging);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mNumMatchesWithoutRangingForRangingEnabledSubscribes:");
            stringBuilder4.append(this.mNumMatchesWithoutRangingForRangingEnabledSubscribes);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxNdiInApp:");
            stringBuilder4.append(this.mMaxNdiInApp);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxNdpInApp:");
            stringBuilder4.append(this.mMaxNdpInApp);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxSecureNdpInApp:");
            stringBuilder4.append(this.mMaxSecureNdpInApp);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxNdiInSystem:");
            stringBuilder4.append(this.mMaxNdiInSystem);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxNdpInSystem:");
            stringBuilder4.append(this.mMaxNdpInSystem);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxSecureNdpInSystem:");
            stringBuilder4.append(this.mMaxSecureNdpInSystem);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mMaxNdpPerNdi:");
            stringBuilder4.append(this.mMaxNdpPerNdi);
            pw.println(stringBuilder4.toString());
            pw.println("mInBandNdpStatusData:");
            for (i = 0; i < this.mInBandNdpStatusData.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mInBandNdpStatusData.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mInBandNdpStatusData.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            pw.println("mOutOfBandNdpStatusData:");
            for (i = 0; i < this.mOutOfBandNdpStatusData.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mOutOfBandNdpStatusData.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mOutOfBandNdpStatusData.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            pw.println("mNdpCreationTimeDuration:");
            for (i = 0; i < this.mNdpCreationTimeDuration.size(); i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("  ");
                stringBuilder.append(this.mNdpCreationTimeDuration.keyAt(i));
                stringBuilder.append(": ");
                stringBuilder.append(this.mNdpCreationTimeDuration.valueAt(i));
                pw.println(stringBuilder.toString());
            }
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mNdpCreationTimeMin:");
            stringBuilder4.append(this.mNdpCreationTimeMin);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mNdpCreationTimeMax:");
            stringBuilder4.append(this.mNdpCreationTimeMax);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mNdpCreationTimeSum:");
            stringBuilder4.append(this.mNdpCreationTimeSum);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mNdpCreationTimeSumSq:");
            stringBuilder4.append(this.mNdpCreationTimeSumSq);
            pw.println(stringBuilder4.toString());
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("mNdpCreationTimeNumSamples:");
            stringBuilder4.append(this.mNdpCreationTimeNumSamples);
            pw.println(stringBuilder4.toString());
            pw.println("mHistogramNdpDuration:");
            while (i2 < this.mHistogramNdpDuration.size()) {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("  ");
                stringBuilder4.append(this.mHistogramNdpDuration.keyAt(i2));
                stringBuilder4.append(": ");
                stringBuilder4.append(this.mHistogramNdpDuration.valueAt(i2));
                pw.println(stringBuilder4.toString());
                i2++;
            }
        }
    }

    @VisibleForTesting
    public static HistogramBucket[] histogramToProtoArray(GenericBucket[] buckets) {
        HistogramBucket[] protoArray = new HistogramBucket[buckets.length];
        for (int i = 0; i < buckets.length; i++) {
            protoArray[i] = new HistogramBucket();
            protoArray[i].start = buckets[i].start;
            protoArray[i].end = buckets[i].end;
            protoArray[i].count = buckets[i].count;
        }
        return protoArray;
    }

    public static void addNanHalStatusToHistogram(int halStatus, SparseIntArray histogram) {
        int protoStatus = convertNanStatusTypeToProtoEnum(halStatus);
        histogram.put(protoStatus, histogram.get(protoStatus) + 1);
    }

    @VisibleForTesting
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
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unrecognized NanStatusType: ");
                stringBuilder.append(nanStatusType);
                Log.e(str, stringBuilder.toString());
                return 14;
        }
    }
}
