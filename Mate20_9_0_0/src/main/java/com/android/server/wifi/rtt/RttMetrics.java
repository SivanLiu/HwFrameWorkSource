package com.android.server.wifi.rtt;

import android.hardware.wifi.V1_0.RttResult;
import android.net.MacAddress;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.ResponderConfig;
import android.os.WorkSource;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.server.wifi.Clock;
import com.android.server.wifi.ScoringParams;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.nano.WifiMetricsProto.WifiRttLog;
import com.android.server.wifi.nano.WifiMetricsProto.WifiRttLog.HistogramBucket;
import com.android.server.wifi.nano.WifiMetricsProto.WifiRttLog.RttIndividualStatusHistogramBucket;
import com.android.server.wifi.nano.WifiMetricsProto.WifiRttLog.RttOverallStatusHistogramBucket;
import com.android.server.wifi.nano.WifiMetricsProto.WifiRttLog.RttToPeerLog;
import com.android.server.wifi.util.MetricsUtils;
import com.android.server.wifi.util.MetricsUtils.GenericBucket;
import com.android.server.wifi.util.MetricsUtils.LogHistParms;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RttMetrics {
    private static final LogHistParms COUNT_LOG_HISTOGRAM = new LogHistParms(0, 1, 10, 1, 7);
    private static final int[] DISTANCE_MM_HISTOGRAM = new int[]{0, ScoringParams.BAND5, 15000, WifiStateMachine.LAST_SELECTED_NETWORK_EXPIRATION_AGE_MILLIS, 60000, 100000};
    private static final int PEER_AP = 0;
    private static final int PEER_AWARE = 1;
    private static final String TAG = "RttMetrics";
    private static final boolean VDBG = false;
    private final Clock mClock;
    boolean mDbg = false;
    private final Object mLock = new Object();
    private int mNumStartRangingCalls = 0;
    private SparseIntArray mOverallStatusHistogram = new SparseIntArray();
    private PerPeerTypeInfo[] mPerPeerTypeInfo;

    private class PerPeerTypeInfo {
        public SparseIntArray measuredDistanceHistogram;
        public int numCalls;
        public int numIndividualCalls;
        public SparseIntArray numRequestsHistogram;
        public SparseArray<PerUidInfo> perUidInfo;
        public SparseIntArray requestGapHistogram;
        public SparseIntArray statusHistogram;

        private PerPeerTypeInfo() {
            this.perUidInfo = new SparseArray();
            this.numRequestsHistogram = new SparseIntArray();
            this.requestGapHistogram = new SparseIntArray();
            this.statusHistogram = new SparseIntArray();
            this.measuredDistanceHistogram = new SparseIntArray();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("numCalls=");
            stringBuilder.append(this.numCalls);
            stringBuilder.append(", numIndividualCalls=");
            stringBuilder.append(this.numIndividualCalls);
            stringBuilder.append(", perUidInfo=");
            stringBuilder.append(this.perUidInfo);
            stringBuilder.append(", numRequestsHistogram=");
            stringBuilder.append(this.numRequestsHistogram);
            stringBuilder.append(", requestGapHistogram=");
            stringBuilder.append(this.requestGapHistogram);
            stringBuilder.append(", measuredDistanceHistogram=");
            stringBuilder.append(this.measuredDistanceHistogram);
            return stringBuilder.toString();
        }
    }

    private class PerUidInfo {
        public long lastRequestMs;
        public int numRequests;

        private PerUidInfo() {
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("numRequests=");
            stringBuilder.append(this.numRequests);
            stringBuilder.append(", lastRequestMs=");
            stringBuilder.append(this.lastRequestMs);
            return stringBuilder.toString();
        }
    }

    public RttMetrics(Clock clock) {
        this.mClock = clock;
        this.mPerPeerTypeInfo = new PerPeerTypeInfo[2];
        this.mPerPeerTypeInfo[0] = new PerPeerTypeInfo();
        this.mPerPeerTypeInfo[1] = new PerPeerTypeInfo();
    }

    public void recordRequest(WorkSource ws, RangingRequest requests) {
        this.mNumStartRangingCalls++;
        int numApRequests = 0;
        int numAwareRequests = 0;
        for (ResponderConfig request : requests.mRttPeers) {
            if (request != null) {
                if (request.responderType == 4) {
                    numAwareRequests++;
                } else if (request.responderType == 0) {
                    numApRequests++;
                } else if (this.mDbg) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected Responder type: ");
                    stringBuilder.append(request.responderType);
                    Log.d(str, stringBuilder.toString());
                }
            }
        }
        updatePeerInfoWithRequestInfo(this.mPerPeerTypeInfo[0], ws, numApRequests);
        updatePeerInfoWithRequestInfo(this.mPerPeerTypeInfo[1], ws, numAwareRequests);
    }

    public void recordResult(RangingRequest requests, List<RttResult> results) {
        Map<MacAddress, ResponderConfig> requestEntries = new HashMap();
        for (ResponderConfig responder : requests.mRttPeers) {
            requestEntries.put(responder.macAddress, responder);
        }
        if (results != null) {
            for (RttResult result : results) {
                if (result != null) {
                    ResponderConfig responder2 = (ResponderConfig) requestEntries.remove(MacAddress.fromBytes(result.addr));
                    String str;
                    StringBuilder stringBuilder;
                    if (responder2 == null) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("recordResult: found a result which doesn't match any requests: ");
                        stringBuilder.append(result);
                        Log.e(str, stringBuilder.toString());
                    } else if (responder2.responderType == 0) {
                        updatePeerInfoWithResultInfo(this.mPerPeerTypeInfo[0], result);
                    } else if (responder2.responderType == 4) {
                        updatePeerInfoWithResultInfo(this.mPerPeerTypeInfo[1], result);
                    } else {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("recordResult: unexpected peer type in responder: ");
                        stringBuilder.append(responder2);
                        Log.e(str, stringBuilder.toString());
                    }
                }
            }
        }
        for (ResponderConfig responder3 : requestEntries.values()) {
            PerPeerTypeInfo peerInfo;
            if (responder3.responderType == 0) {
                peerInfo = this.mPerPeerTypeInfo[0];
            } else if (responder3.responderType == 4) {
                peerInfo = this.mPerPeerTypeInfo[1];
            } else {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("recordResult: unexpected peer type in responder: ");
                stringBuilder2.append(responder3);
                Log.e(str2, stringBuilder2.toString());
            }
            peerInfo.statusHistogram.put(17, peerInfo.statusHistogram.get(17) + 1);
        }
    }

    public void recordOverallStatus(int status) {
        this.mOverallStatusHistogram.put(status, this.mOverallStatusHistogram.get(status) + 1);
    }

    private void updatePeerInfoWithRequestInfo(PerPeerTypeInfo peerInfo, WorkSource ws, int numIndividualCalls) {
        if (numIndividualCalls != 0) {
            long nowMs = this.mClock.getElapsedSinceBootMillis();
            peerInfo.numCalls++;
            peerInfo.numIndividualCalls += numIndividualCalls;
            peerInfo.numRequestsHistogram.put(numIndividualCalls, peerInfo.numRequestsHistogram.get(numIndividualCalls) + 1);
            boolean recordedIntervals = false;
            for (int i = 0; i < ws.size(); i++) {
                int uid = ws.get(i);
                PerUidInfo perUidInfo = (PerUidInfo) peerInfo.perUidInfo.get(uid);
                if (perUidInfo == null) {
                    perUidInfo = new PerUidInfo();
                }
                perUidInfo.numRequests++;
                if (!(recordedIntervals || perUidInfo.lastRequestMs == 0)) {
                    recordedIntervals = true;
                    MetricsUtils.addValueToLogHistogram(nowMs - perUidInfo.lastRequestMs, peerInfo.requestGapHistogram, COUNT_LOG_HISTOGRAM);
                }
                perUidInfo.lastRequestMs = nowMs;
                peerInfo.perUidInfo.put(uid, perUidInfo);
            }
        }
    }

    private void updatePeerInfoWithResultInfo(PerPeerTypeInfo peerInfo, RttResult result) {
        int protoStatus = convertRttStatusTypeToProtoEnum(result.status);
        peerInfo.statusHistogram.put(protoStatus, peerInfo.statusHistogram.get(protoStatus) + 1);
        MetricsUtils.addValueToLinearHistogram(result.distanceInMm, peerInfo.measuredDistanceHistogram, DISTANCE_MM_HISTOGRAM);
    }

    public WifiRttLog consolidateProto() {
        WifiRttLog log = new WifiRttLog();
        log.rttToAp = new RttToPeerLog();
        log.rttToAware = new RttToPeerLog();
        synchronized (this.mLock) {
            log.numRequests = this.mNumStartRangingCalls;
            log.histogramOverallStatus = consolidateOverallStatus(this.mOverallStatusHistogram);
            consolidatePeerType(log.rttToAp, this.mPerPeerTypeInfo[0]);
            consolidatePeerType(log.rttToAware, this.mPerPeerTypeInfo[1]);
        }
        return log;
    }

    private RttOverallStatusHistogramBucket[] consolidateOverallStatus(SparseIntArray histogram) {
        RttOverallStatusHistogramBucket[] h = new RttOverallStatusHistogramBucket[histogram.size()];
        for (int i = 0; i < histogram.size(); i++) {
            h[i] = new RttOverallStatusHistogramBucket();
            h[i].statusType = histogram.keyAt(i);
            h[i].count = histogram.valueAt(i);
        }
        return h;
    }

    private void consolidatePeerType(RttToPeerLog peerLog, PerPeerTypeInfo peerInfo) {
        peerLog.numRequests = peerInfo.numCalls;
        peerLog.numIndividualRequests = peerInfo.numIndividualCalls;
        peerLog.numApps = peerInfo.perUidInfo.size();
        peerLog.histogramNumPeersPerRequest = consolidateNumPeersPerRequest(peerInfo.numRequestsHistogram);
        peerLog.histogramNumRequestsPerApp = consolidateNumRequestsPerApp(peerInfo.perUidInfo);
        peerLog.histogramRequestIntervalMs = genericBucketsToRttBuckets(MetricsUtils.logHistogramToGenericBuckets(peerInfo.requestGapHistogram, COUNT_LOG_HISTOGRAM));
        peerLog.histogramIndividualStatus = consolidateIndividualStatus(peerInfo.statusHistogram);
        peerLog.histogramDistance = genericBucketsToRttBuckets(MetricsUtils.linearHistogramToGenericBuckets(peerInfo.measuredDistanceHistogram, DISTANCE_MM_HISTOGRAM));
    }

    private RttIndividualStatusHistogramBucket[] consolidateIndividualStatus(SparseIntArray histogram) {
        RttIndividualStatusHistogramBucket[] h = new RttIndividualStatusHistogramBucket[histogram.size()];
        for (int i = 0; i < histogram.size(); i++) {
            h[i] = new RttIndividualStatusHistogramBucket();
            h[i].statusType = histogram.keyAt(i);
            h[i].count = histogram.valueAt(i);
        }
        return h;
    }

    private HistogramBucket[] consolidateNumPeersPerRequest(SparseIntArray data) {
        HistogramBucket[] protoArray = new HistogramBucket[data.size()];
        for (int i = 0; i < data.size(); i++) {
            protoArray[i] = new HistogramBucket();
            protoArray[i].start = (long) data.keyAt(i);
            protoArray[i].end = (long) data.keyAt(i);
            protoArray[i].count = data.valueAt(i);
        }
        return protoArray;
    }

    private HistogramBucket[] consolidateNumRequestsPerApp(SparseArray<PerUidInfo> perUidInfos) {
        SparseIntArray histogramNumRequestsPerUid = new SparseIntArray();
        for (int i = 0; i < perUidInfos.size(); i++) {
            MetricsUtils.addValueToLogHistogram((long) ((PerUidInfo) perUidInfos.valueAt(i)).numRequests, histogramNumRequestsPerUid, COUNT_LOG_HISTOGRAM);
        }
        return genericBucketsToRttBuckets(MetricsUtils.logHistogramToGenericBuckets(histogramNumRequestsPerUid, COUNT_LOG_HISTOGRAM));
    }

    private HistogramBucket[] genericBucketsToRttBuckets(GenericBucket[] genericHistogram) {
        HistogramBucket[] histogram = new HistogramBucket[genericHistogram.length];
        for (int i = 0; i < genericHistogram.length; i++) {
            histogram[i] = new HistogramBucket();
            histogram[i].start = genericHistogram[i].start;
            histogram[i].end = genericHistogram[i].end;
            histogram[i].count = genericHistogram[i].count;
        }
        return histogram;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mLock) {
            pw.println("RTT Metrics:");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mNumStartRangingCalls:");
            stringBuilder.append(this.mNumStartRangingCalls);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mOverallStatusHistogram:");
            stringBuilder.append(this.mOverallStatusHistogram);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("AP:");
            stringBuilder.append(this.mPerPeerTypeInfo[0]);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("AWARE:");
            stringBuilder.append(this.mPerPeerTypeInfo[1]);
            pw.println(stringBuilder.toString());
        }
    }

    public void clear() {
        synchronized (this.mLock) {
            this.mNumStartRangingCalls = 0;
            this.mOverallStatusHistogram.clear();
            this.mPerPeerTypeInfo[0] = new PerPeerTypeInfo();
            this.mPerPeerTypeInfo[1] = new PerPeerTypeInfo();
        }
    }

    public static int convertRttStatusTypeToProtoEnum(int rttStatusType) {
        switch (rttStatusType) {
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
            case 13:
                return 14;
            case 14:
                return 15;
            case 15:
                return 16;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unrecognized RttStatus: ");
                stringBuilder.append(rttStatusType);
                Log.e(str, stringBuilder.toString());
                return 0;
        }
    }
}
