package com.android.server.wifi.rtt;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.wifi.V1_0.RttResult;
import android.location.LocationManager;
import android.net.MacAddress;
import android.net.wifi.aware.IWifiAwareMacAddressProvider;
import android.net.wifi.aware.IWifiAwareManager;
import android.net.wifi.rtt.IRttCallback;
import android.net.wifi.rtt.IWifiRttManager.Stub;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingRequest.Builder;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.ResponderConfig;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.os.WorkSource;
import android.os.WorkSource.WorkChain;
import android.provider.Settings.Global;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.internal.util.WakeupMessage;
import com.android.server.wifi.Clock;
import com.android.server.wifi.FrameworkFacade;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class RttServiceImpl extends Stub {
    private static final int CONTROL_PARAM_OVERRIDE_ASSUME_NO_PRIVILEGE_DEFAULT = 0;
    private static final String CONTROL_PARAM_OVERRIDE_ASSUME_NO_PRIVILEGE_NAME = "override_assume_no_privilege";
    private static final int CONVERSION_US_TO_MS = 1000;
    private static final long DEFAULT_BACKGROUND_PROCESS_EXEC_GAP_MS = 1800000;
    private static final long HAL_RANGING_TIMEOUT_MS = 5000;
    static final String HAL_RANGING_TIMEOUT_TAG = "RttServiceImpl HAL Ranging Timeout";
    static final int MAX_QUEUED_PER_UID = 20;
    private static final String TAG = "RttServiceImpl";
    private static final boolean VDBG = false;
    private ActivityManager mActivityManager;
    private IWifiAwareManager mAwareBinder;
    private long mBackgroundProcessExecGapMs;
    private Clock mClock;
    private final Context mContext;
    private boolean mDbg = false;
    private FrameworkFacade mFrameworkFacade;
    private LocationManager mLocationManager;
    private PowerManager mPowerManager;
    private RttMetrics mRttMetrics;
    private RttNative mRttNative;
    private RttServiceSynchronized mRttServiceSynchronized;
    private final RttShellCommand mShellCommand;
    private WifiPermissionsUtil mWifiPermissionsUtil;

    private static class RttRequestInfo {
        public IBinder binder;
        public IRttCallback callback;
        public String callingPackage;
        public int cmdId;
        public boolean dispatchedToNative;
        public DeathRecipient dr;
        public boolean isCalledFromPrivilegedContext;
        public boolean peerHandlesTranslated;
        public RangingRequest request;
        public int uid;
        public WorkSource workSource;

        private RttRequestInfo() {
            this.cmdId = 0;
            this.dispatchedToNative = false;
            this.peerHandlesTranslated = false;
        }

        /* synthetic */ RttRequestInfo(AnonymousClass1 x0) {
            this();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder("RttRequestInfo: uid=");
            stringBuilder.append(this.uid);
            stringBuilder.append(", workSource=");
            stringBuilder.append(this.workSource);
            stringBuilder.append(", binder=");
            stringBuilder.append(this.binder);
            stringBuilder.append(", dr=");
            stringBuilder.append(this.dr);
            stringBuilder.append(", callingPackage=");
            stringBuilder.append(this.callingPackage);
            stringBuilder.append(", request=");
            stringBuilder.append(this.request.toString());
            stringBuilder.append(", callback=");
            stringBuilder.append(this.callback);
            stringBuilder.append(", cmdId=");
            stringBuilder.append(this.cmdId);
            stringBuilder.append(", peerHandlesTranslated=");
            stringBuilder.append(this.peerHandlesTranslated);
            stringBuilder.append(", isCalledFromPrivilegedContext=");
            stringBuilder.append(this.isCalledFromPrivilegedContext);
            return stringBuilder.toString();
        }
    }

    private static class RttRequesterInfo {
        public long lastRangingExecuted;

        private RttRequesterInfo() {
        }

        /* synthetic */ RttRequesterInfo(AnonymousClass1 x0) {
            this();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder("RttRequesterInfo: lastRangingExecuted=");
            stringBuilder.append(this.lastRangingExecuted);
            return stringBuilder.toString();
        }
    }

    private class RttServiceSynchronized {
        public Handler mHandler;
        private int mNextCommandId = RttServiceImpl.CONVERSION_US_TO_MS;
        private WakeupMessage mRangingTimeoutMessage = null;
        private RttNative mRttNative;
        private List<RttRequestInfo> mRttRequestQueue = new LinkedList();
        private Map<Integer, RttRequesterInfo> mRttRequesterInfo = new HashMap();

        RttServiceSynchronized(Looper looper, RttNative rttNative) {
            this.mRttNative = rttNative;
            this.mHandler = new Handler(looper);
            this.mRangingTimeoutMessage = new WakeupMessage(RttServiceImpl.this.mContext, this.mHandler, RttServiceImpl.HAL_RANGING_TIMEOUT_TAG, new -$$Lambda$RttServiceImpl$RttServiceSynchronized$nvl34lO7P1KT2zH6q5nTdziEODs(this));
        }

        private void cancelRanging(RttRequestInfo rri) {
            ArrayList<byte[]> macAddresses = new ArrayList();
            for (ResponderConfig peer : rri.request.mRttPeers) {
                macAddresses.add(peer.macAddress.toByteArray());
            }
            this.mRttNative.rangeCancel(rri.cmdId, macAddresses);
        }

        private void cleanUpOnDisable() {
            for (RttRequestInfo rri : this.mRttRequestQueue) {
                try {
                    if (rri.dispatchedToNative) {
                        cancelRanging(rri);
                    }
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(3);
                    rri.callback.onRangingFailure(2);
                } catch (RemoteException e) {
                    String str = RttServiceImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("RttServiceSynchronized.startRanging: disabled, callback failed -- ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                }
                rri.binder.unlinkToDeath(rri.dr, 0);
            }
            this.mRttRequestQueue.clear();
            this.mRangingTimeoutMessage.cancel();
        }

        private void cleanUpClientRequests(int uid, WorkSource workSource) {
            boolean dispatchedRequestAborted = false;
            ListIterator<RttRequestInfo> it = this.mRttRequestQueue.listIterator();
            while (true) {
                boolean match = true;
                if (!it.hasNext()) {
                    break;
                }
                RttRequestInfo rri = (RttRequestInfo) it.next();
                if (rri.uid != uid) {
                    match = false;
                }
                if (!(rri.workSource == null || workSource == null)) {
                    rri.workSource.remove(workSource);
                    if (rri.workSource.isEmpty()) {
                        match = true;
                    }
                }
                if (match) {
                    if (rri.dispatchedToNative) {
                        dispatchedRequestAborted = true;
                        String str = RttServiceImpl.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Client death - cancelling RTT operation in progress: cmdId=");
                        stringBuilder.append(rri.cmdId);
                        Log.d(str, stringBuilder.toString());
                        this.mRangingTimeoutMessage.cancel();
                        cancelRanging(rri);
                    } else {
                        it.remove();
                        rri.binder.unlinkToDeath(rri.dr, 0);
                    }
                }
            }
            if (dispatchedRequestAborted) {
                executeNextRangingRequestIfPossible(true);
            }
        }

        private void timeoutRangingRequest() {
            if (this.mRttRequestQueue.size() == 0) {
                Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.timeoutRangingRequest: but nothing in queue!?");
                return;
            }
            RttRequestInfo rri = (RttRequestInfo) this.mRttRequestQueue.get(0);
            if (rri.dispatchedToNative) {
                cancelRanging(rri);
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(4);
                    rri.callback.onRangingFailure(1);
                } catch (RemoteException e) {
                    String str = RttServiceImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("RttServiceSynchronized.timeoutRangingRequest: callback failed: ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                }
                executeNextRangingRequestIfPossible(true);
                return;
            }
            Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.timeoutRangingRequest: command not dispatched to native!?");
        }

        private void queueRangingRequest(int uid, WorkSource workSource, IBinder binder, DeathRecipient dr, String callingPackage, RangingRequest request, IRttCallback callback, boolean isCalledFromPrivilegedContext) {
            RttServiceImpl.this.mRttMetrics.recordRequest(workSource, request);
            if (isRequestorSpamming(workSource)) {
                String str = RttServiceImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Work source ");
                stringBuilder.append(workSource);
                stringBuilder.append(" is spamming, dropping request: ");
                stringBuilder.append(request);
                Log.w(str, stringBuilder.toString());
                binder.unlinkToDeath(dr, 0);
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(5);
                    callback.onRangingFailure(1);
                } catch (RemoteException e) {
                    String str2 = RttServiceImpl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("RttServiceSynchronized.queueRangingRequest: spamming, callback failed -- ");
                    stringBuilder.append(e);
                    Log.e(str2, stringBuilder.toString());
                }
                return;
            }
            RttRequestInfo newRequest = new RttRequestInfo();
            newRequest.uid = uid;
            newRequest.workSource = workSource;
            newRequest.binder = binder;
            newRequest.dr = dr;
            newRequest.callingPackage = callingPackage;
            newRequest.request = request;
            newRequest.callback = callback;
            newRequest.isCalledFromPrivilegedContext = isCalledFromPrivilegedContext;
            this.mRttRequestQueue.add(newRequest);
            executeNextRangingRequestIfPossible(false);
        }

        private boolean isRequestorSpamming(WorkSource ws) {
            SparseIntArray counts = new SparseIntArray();
            Iterator it = this.mRttRequestQueue.iterator();
            while (true) {
                int i = 0;
                if (!it.hasNext()) {
                    break;
                }
                int uid;
                RttRequestInfo rri = (RttRequestInfo) it.next();
                for (int i2 = 0; i2 < rri.workSource.size(); i2++) {
                    uid = rri.workSource.get(i2);
                    counts.put(uid, counts.get(uid) + 1);
                }
                ArrayList<WorkChain> workChains = rri.workSource.getWorkChains();
                if (workChains != null) {
                    while (i < workChains.size()) {
                        uid = ((WorkChain) workChains.get(i)).getAttributionUid();
                        counts.put(uid, counts.get(uid) + 1);
                        i++;
                    }
                }
            }
            for (int i3 = 0; i3 < ws.size(); i3++) {
                if (counts.get(ws.get(i3)) < 20) {
                    return false;
                }
            }
            ArrayList<WorkChain> workChains2 = ws.getWorkChains();
            if (workChains2 != null) {
                for (int i4 = 0; i4 < workChains2.size(); i4++) {
                    if (counts.get(((WorkChain) workChains2.get(i4)).getAttributionUid()) < 20) {
                        return false;
                    }
                }
            }
            if (RttServiceImpl.this.mDbg) {
                String str = RttServiceImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("isRequestorSpamming: ws=");
                stringBuilder.append(ws);
                stringBuilder.append(", someone is spamming: ");
                stringBuilder.append(counts);
                Log.v(str, stringBuilder.toString());
            }
            return true;
        }

        private void executeNextRangingRequestIfPossible(boolean popFirst) {
            if (popFirst) {
                if (this.mRttRequestQueue.size() == 0) {
                    Log.w(RttServiceImpl.TAG, "executeNextRangingRequestIfPossible: pop requested - but empty queue!? Ignoring pop.");
                } else {
                    RttRequestInfo topOfQueueRequest = (RttRequestInfo) this.mRttRequestQueue.remove(0);
                    topOfQueueRequest.binder.unlinkToDeath(topOfQueueRequest.dr, 0);
                }
            }
            if (this.mRttRequestQueue.size() != 0) {
                RttRequestInfo nextRequest = (RttRequestInfo) this.mRttRequestQueue.get(0);
                if (!nextRequest.peerHandlesTranslated && !nextRequest.dispatchedToNative) {
                    startRanging(nextRequest);
                }
            }
        }

        private void startRanging(RttRequestInfo nextRequest) {
            String str;
            StringBuilder stringBuilder;
            if (!RttServiceImpl.this.isAvailable()) {
                Log.d(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: disabled");
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(3);
                    nextRequest.callback.onRangingFailure(2);
                } catch (RemoteException e) {
                    str = RttServiceImpl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("RttServiceSynchronized.startRanging: disabled, callback failed -- ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                    executeNextRangingRequestIfPossible(true);
                    return;
                }
            }
            if (!processAwarePeerHandles(nextRequest)) {
                if (preExecThrottleCheck(nextRequest.workSource)) {
                    int i = this.mNextCommandId;
                    this.mNextCommandId = i + 1;
                    nextRequest.cmdId = i;
                    if (this.mRttNative.rangeRequest(nextRequest.cmdId, nextRequest.request, nextRequest.isCalledFromPrivilegedContext)) {
                        this.mRangingTimeoutMessage.schedule(RttServiceImpl.this.mClock.getElapsedSinceBootMillis() + RttServiceImpl.HAL_RANGING_TIMEOUT_MS);
                    } else {
                        Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.startRanging: native rangeRequest call failed");
                        try {
                            RttServiceImpl.this.mRttMetrics.recordOverallStatus(6);
                            nextRequest.callback.onRangingFailure(1);
                        } catch (RemoteException e2) {
                            str = RttServiceImpl.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("RttServiceSynchronized.startRanging: HAL request failed, callback failed -- ");
                            stringBuilder.append(e2);
                            Log.e(str, stringBuilder.toString());
                        }
                        executeNextRangingRequestIfPossible(true);
                    }
                    nextRequest.dispatchedToNative = true;
                    return;
                }
                String str2 = RttServiceImpl.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("RttServiceSynchronized.startRanging: execution throttled - nextRequest=");
                stringBuilder2.append(nextRequest);
                stringBuilder2.append(", mRttRequesterInfo=");
                stringBuilder2.append(this.mRttRequesterInfo);
                Log.w(str2, stringBuilder2.toString());
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(5);
                    nextRequest.callback.onRangingFailure(1);
                } catch (RemoteException e22) {
                    str = RttServiceImpl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("RttServiceSynchronized.startRanging: throttled, callback failed -- ");
                    stringBuilder.append(e22);
                    Log.e(str, stringBuilder.toString());
                }
                executeNextRangingRequestIfPossible(true);
            }
        }

        private boolean preExecThrottleCheck(WorkSource ws) {
            int i;
            RttRequesterInfo info;
            boolean allUidsInBackground = true;
            int i2 = 0;
            for (int i3 = 0; i3 < ws.size(); i3++) {
                if (RttServiceImpl.this.mActivityManager.getUidImportance(ws.get(i3)) <= 125) {
                    allUidsInBackground = false;
                    break;
                }
            }
            ArrayList<WorkChain> workChains = ws.getWorkChains();
            if (allUidsInBackground && workChains != null) {
                for (int i4 = 0; i4 < workChains.size(); i4++) {
                    if (RttServiceImpl.this.mActivityManager.getUidImportance(((WorkChain) workChains.get(i4)).getAttributionUid()) <= 125) {
                        allUidsInBackground = false;
                        break;
                    }
                }
            }
            boolean allowExecution = false;
            long mostRecentExecutionPermitted = RttServiceImpl.this.mClock.getElapsedSinceBootMillis() - RttServiceImpl.this.mBackgroundProcessExecGapMs;
            if (allUidsInBackground) {
                for (i = 0; i < ws.size(); i++) {
                    info = (RttRequesterInfo) this.mRttRequesterInfo.get(Integer.valueOf(ws.get(i)));
                    if (info == null || info.lastRangingExecuted < mostRecentExecutionPermitted) {
                        allowExecution = true;
                        break;
                    }
                }
                i = 1;
                int i5 = workChains != null ? 1 : 0;
                if (allowExecution) {
                    i = 0;
                }
                if ((i & i5) != 0) {
                    for (i = 0; i < workChains.size(); i++) {
                        RttRequesterInfo info2 = (RttRequesterInfo) this.mRttRequesterInfo.get(Integer.valueOf(((WorkChain) workChains.get(i)).getAttributionUid()));
                        if (info2 == null || info2.lastRangingExecuted < mostRecentExecutionPermitted) {
                            allowExecution = true;
                            break;
                        }
                    }
                }
            } else {
                allowExecution = true;
            }
            if (allowExecution) {
                for (i = 0; i < ws.size(); i++) {
                    info = (RttRequesterInfo) this.mRttRequesterInfo.get(Integer.valueOf(ws.get(i)));
                    if (info == null) {
                        info = new RttRequesterInfo();
                        this.mRttRequesterInfo.put(Integer.valueOf(ws.get(i)), info);
                    }
                    info.lastRangingExecuted = RttServiceImpl.this.mClock.getElapsedSinceBootMillis();
                }
                if (workChains != null) {
                    while (i2 < workChains.size()) {
                        WorkChain wc = (WorkChain) workChains.get(i2);
                        info = (RttRequesterInfo) this.mRttRequesterInfo.get(Integer.valueOf(wc.getAttributionUid()));
                        if (info == null) {
                            info = new RttRequesterInfo();
                            this.mRttRequesterInfo.put(Integer.valueOf(wc.getAttributionUid()), info);
                        }
                        info.lastRangingExecuted = RttServiceImpl.this.mClock.getElapsedSinceBootMillis();
                        i2++;
                    }
                }
            }
            return allowExecution;
        }

        private boolean processAwarePeerHandles(final RttRequestInfo request) {
            List<Integer> peerIdsNeedingTranslation = new ArrayList();
            for (ResponderConfig rttPeer : request.request.mRttPeers) {
                if (rttPeer.peerHandle != null && rttPeer.macAddress == null) {
                    peerIdsNeedingTranslation.add(Integer.valueOf(rttPeer.peerHandle.peerId));
                }
            }
            if (peerIdsNeedingTranslation.size() == 0) {
                return false;
            }
            if (request.peerHandlesTranslated) {
                String str = RttServiceImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("processAwarePeerHandles: request=");
                stringBuilder.append(request);
                stringBuilder.append(": PeerHandles translated - but information still missing!?");
                Log.w(str, stringBuilder.toString());
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(7);
                    request.callback.onRangingFailure(1);
                } catch (RemoteException e) {
                    String str2 = RttServiceImpl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("processAwarePeerHandles: onRangingResults failure -- ");
                    stringBuilder.append(e);
                    Log.e(str2, stringBuilder.toString());
                }
                executeNextRangingRequestIfPossible(true);
                return true;
            }
            request.peerHandlesTranslated = true;
            try {
                RttServiceImpl.this.mAwareBinder.requestMacAddresses(request.uid, peerIdsNeedingTranslation, new IWifiAwareMacAddressProvider.Stub() {
                    public void macAddress(Map peerIdToMacMap) {
                        RttServiceSynchronized.this.mHandler.post(new -$$Lambda$RttServiceImpl$RttServiceSynchronized$1$X3EitWNHg38OS5b_JDpRvNEeXDk(this, request, peerIdToMacMap));
                    }
                });
                return true;
            } catch (RemoteException e2) {
                String str3 = RttServiceImpl.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("processAwarePeerHandles: exception while calling requestMacAddresses -- ");
                stringBuilder2.append(e2);
                stringBuilder2.append(", aborting request=");
                stringBuilder2.append(request);
                Log.e(str3, stringBuilder2.toString());
                try {
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(7);
                    request.callback.onRangingFailure(1);
                } catch (RemoteException e22) {
                    str3 = RttServiceImpl.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("processAwarePeerHandles: onRangingResults failure -- ");
                    stringBuilder2.append(e22);
                    Log.e(str3, stringBuilder2.toString());
                }
                executeNextRangingRequestIfPossible(true);
                return true;
            }
        }

        private void processReceivedAwarePeerMacAddresses(RttRequestInfo request, Map<Integer, byte[]> peerIdToMacMap) {
            Map<Integer, byte[]> map;
            RttRequestInfo rttRequestInfo = request;
            Builder newRequestBuilder = new Builder();
            for (ResponderConfig rttPeer : rttRequestInfo.request.mRttPeers) {
                if (rttPeer.peerHandle == null || rttPeer.macAddress != null) {
                    map = peerIdToMacMap;
                    newRequestBuilder.addResponder(rttPeer);
                } else {
                    newRequestBuilder.addResponder(new ResponderConfig(MacAddress.fromBytes((byte[]) peerIdToMacMap.get(Integer.valueOf(rttPeer.peerHandle.peerId))), rttPeer.peerHandle, rttPeer.responderType, rttPeer.supports80211mc, rttPeer.channelWidth, rttPeer.frequency, rttPeer.centerFreq0, rttPeer.centerFreq1, rttPeer.preamble));
                }
            }
            map = peerIdToMacMap;
            rttRequestInfo.request = newRequestBuilder.build();
            startRanging(request);
        }

        private void onRangingResults(int cmdId, List<RttResult> results) {
            if (this.mRttRequestQueue.size() == 0) {
                Log.e(RttServiceImpl.TAG, "RttServiceSynchronized.onRangingResults: no current RTT request pending!?");
                return;
            }
            this.mRangingTimeoutMessage.cancel();
            boolean permissionGranted = false;
            RttRequestInfo topOfQueueRequest = (RttRequestInfo) this.mRttRequestQueue.get(0);
            if (topOfQueueRequest.cmdId != cmdId) {
                String str = RttServiceImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RttServiceSynchronized.onRangingResults: cmdId=");
                stringBuilder.append(cmdId);
                stringBuilder.append(", does not match pending RTT request cmdId=");
                stringBuilder.append(topOfQueueRequest.cmdId);
                Log.e(str, stringBuilder.toString());
                return;
            }
            if (RttServiceImpl.this.mWifiPermissionsUtil.checkCallersLocationPermission(topOfQueueRequest.callingPackage, topOfQueueRequest.uid) && RttServiceImpl.this.mLocationManager.isLocationEnabled()) {
                permissionGranted = true;
            }
            if (permissionGranted) {
                try {
                    List<RangingResult> finalResults = postProcessResults(topOfQueueRequest.request, results, topOfQueueRequest.isCalledFromPrivilegedContext);
                    RttServiceImpl.this.mRttMetrics.recordOverallStatus(1);
                    RttServiceImpl.this.mRttMetrics.recordResult(topOfQueueRequest.request, results);
                    topOfQueueRequest.callback.onRangingResults(finalResults);
                } catch (RemoteException e) {
                    String str2 = RttServiceImpl.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("RttServiceSynchronized.onRangingResults: callback exception -- ");
                    stringBuilder2.append(e);
                    Log.e(str2, stringBuilder2.toString());
                }
            } else {
                Log.w(RttServiceImpl.TAG, "RttServiceSynchronized.onRangingResults: location permission revoked - not forwarding results");
                RttServiceImpl.this.mRttMetrics.recordOverallStatus(8);
                topOfQueueRequest.callback.onRangingFailure(1);
            }
            executeNextRangingRequestIfPossible(true);
        }

        private List<RangingResult> postProcessResults(RangingRequest request, List<RttResult> results, boolean isCalledFromPrivilegedContext) {
            RttServiceSynchronized rttServiceSynchronized = this;
            RangingRequest rangingRequest = request;
            Map<MacAddress, RttResult> resultEntries = new HashMap();
            for (RttResult result : results) {
                resultEntries.put(MacAddress.fromBytes(result.addr), result);
            }
            List<RangingResult> finalResults = new ArrayList(rangingRequest.mRttPeers.size());
            for (ResponderConfig peer : rangingRequest.mRttPeers) {
                RttResult resultForRequest = (RttResult) resultEntries.get(peer.macAddress);
                if (resultForRequest == null) {
                    if (RttServiceImpl.this.mDbg) {
                        String str = RttServiceImpl.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("postProcessResults: missing=");
                        stringBuilder.append(peer.macAddress);
                        Log.v(str, stringBuilder.toString());
                    }
                    int errorCode = 1;
                    if (!(isCalledFromPrivilegedContext || peer.supports80211mc)) {
                        errorCode = 2;
                    }
                    RangingResult rangingResult;
                    if (peer.peerHandle == null) {
                        RangingResult rangingResult2 = rangingResult;
                        rangingResult = new RangingResult(errorCode, peer.macAddress, 0, 0, 0, 0, 0, null, null, 0);
                        finalResults.add(rangingResult2);
                    } else {
                        RangingResult rangingResult3 = rangingResult;
                        rangingResult = new RangingResult(errorCode, peer.peerHandle, 0, 0, 0, 0, 0, null, null, 0);
                        finalResults.add(rangingResult3);
                    }
                } else {
                    int status = resultForRequest.status == 0 ? 0 : 1;
                    byte[] lci = null;
                    byte[] lcr = null;
                    if (isCalledFromPrivilegedContext) {
                        lci = NativeUtil.byteArrayFromArrayList(resultForRequest.lci.data);
                        lcr = NativeUtil.byteArrayFromArrayList(resultForRequest.lcr.data);
                    }
                    byte[] lcr2 = lcr;
                    if (resultForRequest.successNumber <= 1 && resultForRequest.distanceSdInMm != 0) {
                        if (RttServiceImpl.this.mDbg) {
                            String str2 = RttServiceImpl.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("postProcessResults: non-zero distance stdev with 0||1 num samples!? result=");
                            stringBuilder2.append(resultForRequest);
                            Log.w(str2, stringBuilder2.toString());
                        }
                        resultForRequest.distanceSdInMm = 0;
                    }
                    if (peer.peerHandle == null) {
                        finalResults.add(new RangingResult(status, peer.macAddress, resultForRequest.distanceInMm, resultForRequest.distanceSdInMm, resultForRequest.rssi / -2, resultForRequest.numberPerBurstPeer, resultForRequest.successNumber, lci, lcr2, resultForRequest.timeStampInUs / 1000));
                    } else {
                        finalResults.add(new RangingResult(status, peer.peerHandle, resultForRequest.distanceInMm, resultForRequest.distanceSdInMm, resultForRequest.rssi / -2, resultForRequest.numberPerBurstPeer, resultForRequest.successNumber, lci, lcr2, resultForRequest.timeStampInUs / 1000));
                    }
                }
                rttServiceSynchronized = this;
                rangingRequest = request;
            }
            return finalResults;
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  mNextCommandId: ");
            stringBuilder.append(this.mNextCommandId);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mRttRequesterInfo: ");
            stringBuilder.append(this.mRttRequesterInfo);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mRttRequestQueue: ");
            stringBuilder.append(this.mRttRequestQueue);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mRangingTimeoutMessage: ");
            stringBuilder.append(this.mRangingTimeoutMessage);
            pw.println(stringBuilder.toString());
            RttServiceImpl.this.mRttMetrics.dump(fd, pw, args);
            this.mRttNative.dump(fd, pw, args);
        }
    }

    private class RttShellCommand extends ShellCommand {
        private Map<String, Integer> mControlParams;

        private RttShellCommand() {
            this.mControlParams = new HashMap();
        }

        /* synthetic */ RttShellCommand(RttServiceImpl x0, AnonymousClass1 x1) {
            this();
        }

        public int onCommand(String cmd) {
            int uid = Binder.getCallingUid();
            if (uid == 0) {
                PrintWriter pw = getErrPrintWriter();
                StringBuilder stringBuilder;
                try {
                    String name;
                    if ("reset".equals(cmd)) {
                        reset();
                        return 0;
                    } else if ("get".equals(cmd)) {
                        name = getNextArgRequired();
                        if (this.mControlParams.containsKey(name)) {
                            getOutPrintWriter().println(this.mControlParams.get(name));
                            return 0;
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown parameter name -- '");
                        stringBuilder.append(name);
                        stringBuilder.append("'");
                        pw.println(stringBuilder.toString());
                        return -1;
                    } else if ("set".equals(cmd)) {
                        name = getNextArgRequired();
                        String valueStr = getNextArgRequired();
                        if (this.mControlParams.containsKey(name)) {
                            try {
                                this.mControlParams.put(name, Integer.valueOf(valueStr));
                                return 0;
                            } catch (NumberFormatException e) {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Can't convert value to integer -- '");
                                stringBuilder2.append(valueStr);
                                stringBuilder2.append("'");
                                pw.println(stringBuilder2.toString());
                                return -1;
                            }
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown parameter name -- '");
                        stringBuilder.append(name);
                        stringBuilder.append("'");
                        pw.println(stringBuilder.toString());
                        return -1;
                    } else {
                        handleDefaultCommands(cmd);
                        return -1;
                    }
                } catch (Exception e2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception: ");
                    stringBuilder.append(e2);
                    pw.println(stringBuilder.toString());
                }
            } else {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Uid ");
                stringBuilder3.append(uid);
                stringBuilder3.append(" does not have access to wifirtt commands");
                throw new SecurityException(stringBuilder3.toString());
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Wi-Fi RTT (wifirt) commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("  reset");
            pw.println("    Reset parameters to default values.");
            pw.println("  get <name>");
            pw.println("    Get the value of the control parameter.");
            pw.println("  set <name> <value>");
            pw.println("    Set the value of the control parameter.");
            pw.println("  Control parameters:");
            for (String name : this.mControlParams.keySet()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("    ");
                stringBuilder.append(name);
                pw.println(stringBuilder.toString());
            }
            pw.println();
        }

        public int getControlParam(String name) {
            if (this.mControlParams.containsKey(name)) {
                return ((Integer) this.mControlParams.get(name)).intValue();
            }
            String str = RttServiceImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getControlParam for unknown variable: ");
            stringBuilder.append(name);
            Log.wtf(str, stringBuilder.toString());
            return 0;
        }

        public void reset() {
            this.mControlParams.put(RttServiceImpl.CONTROL_PARAM_OVERRIDE_ASSUME_NO_PRIVILEGE_NAME, Integer.valueOf(0));
        }
    }

    public RttServiceImpl(Context context) {
        this.mContext = context;
        this.mShellCommand = new RttShellCommand(this, null);
        this.mShellCommand.reset();
    }

    public void start(Looper looper, Clock clock, IWifiAwareManager awareBinder, RttNative rttNative, RttMetrics rttMetrics, WifiPermissionsUtil wifiPermissionsUtil, final FrameworkFacade frameworkFacade) {
        this.mClock = clock;
        this.mAwareBinder = awareBinder;
        this.mRttNative = rttNative;
        this.mRttMetrics = rttMetrics;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mFrameworkFacade = frameworkFacade;
        this.mRttServiceSynchronized = new RttServiceSynchronized(looper, rttNative);
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (RttServiceImpl.this.mDbg) {
                    String str = RttServiceImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("BroadcastReceiver: action=");
                    stringBuilder.append(action);
                    Log.v(str, stringBuilder.toString());
                }
                if (!"android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(action)) {
                    return;
                }
                if (RttServiceImpl.this.mPowerManager.isDeviceIdleMode()) {
                    RttServiceImpl.this.disable();
                } else {
                    RttServiceImpl.this.enableIfPossible();
                }
            }
        }, intentFilter);
        frameworkFacade.registerContentObserver(this.mContext, Global.getUriFor("wifi_verbose_logging_enabled"), true, new ContentObserver(this.mRttServiceSynchronized.mHandler) {
            public void onChange(boolean selfChange) {
                RttServiceImpl.this.enableVerboseLogging(frameworkFacade.getIntegerSetting(RttServiceImpl.this.mContext, "wifi_verbose_logging_enabled", 0));
            }
        });
        enableVerboseLogging(frameworkFacade.getIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", 0));
        frameworkFacade.registerContentObserver(this.mContext, Global.getUriFor("wifi_rtt_background_exec_gap_ms"), true, new ContentObserver(this.mRttServiceSynchronized.mHandler) {
            public void onChange(boolean selfChange) {
                RttServiceImpl.this.updateBackgroundThrottlingInterval(frameworkFacade);
            }
        });
        updateBackgroundThrottlingInterval(frameworkFacade);
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.location.MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (RttServiceImpl.this.mDbg) {
                    String str = RttServiceImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onReceive: MODE_CHANGED_ACTION: intent=");
                    stringBuilder.append(intent);
                    Log.v(str, stringBuilder.toString());
                }
                if (RttServiceImpl.this.mLocationManager.isLocationEnabled()) {
                    RttServiceImpl.this.enableIfPossible();
                } else {
                    RttServiceImpl.this.disable();
                }
            }
        }, intentFilter);
        this.mRttServiceSynchronized.mHandler.post(new -$$Lambda$RttServiceImpl$ehyq-_xe9BYccoyltP3Gc2lh51g(this, rttNative));
    }

    private void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            this.mDbg = true;
        } else {
            this.mDbg = false;
        }
        this.mRttNative.mDbg = this.mDbg;
        this.mRttMetrics.mDbg = this.mDbg;
    }

    private void updateBackgroundThrottlingInterval(FrameworkFacade frameworkFacade) {
        this.mBackgroundProcessExecGapMs = frameworkFacade.getLongSetting(this.mContext, "wifi_rtt_background_exec_gap_ms", DEFAULT_BACKGROUND_PROCESS_EXEC_GAP_MS);
    }

    public int getMockableCallingUid() {
        return getCallingUid();
    }

    public void enableIfPossible() {
        if (isAvailable()) {
            sendRttStateChangedBroadcast(true);
            this.mRttServiceSynchronized.mHandler.post(new -$$Lambda$RttServiceImpl$q9ANpyRqIip_-lKXLzaUsSwgxFs(this));
        }
    }

    public void disable() {
        sendRttStateChangedBroadcast(false);
        this.mRttServiceSynchronized.mHandler.post(new -$$Lambda$RttServiceImpl$wP--CWXsaxeveXsy_7abZeA-Q-w(this));
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        this.mShellCommand.exec(this, in, out, err, args, callback, resultReceiver);
    }

    public boolean isAvailable() {
        return this.mRttNative.isReady() && !this.mPowerManager.isDeviceIdleMode() && this.mLocationManager.isLocationEnabled();
    }

    public void startRanging(IBinder binder, String callingPackage, WorkSource workSource, RangingRequest request, IRttCallback callback) throws RemoteException {
        String str;
        StringBuilder stringBuilder;
        final IBinder iBinder = binder;
        RangingRequest rangingRequest = request;
        IRttCallback iRttCallback = callback;
        if (iBinder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        } else if (rangingRequest == null || rangingRequest.mRttPeers == null || rangingRequest.mRttPeers.size() == 0) {
            throw new IllegalArgumentException("Request must not be null or empty");
        } else {
            for (ResponderConfig responder : rangingRequest.mRttPeers) {
                if (responder == null) {
                    throw new IllegalArgumentException("Request must not contain null Responders");
                }
            }
            if (iRttCallback != null) {
                rangingRequest.enforceValidity(this.mAwareBinder != null);
                if (isAvailable()) {
                    final int uid = getMockableCallingUid();
                    enforceAccessPermission();
                    enforceChangePermission();
                    String str2 = callingPackage;
                    this.mWifiPermissionsUtil.enforceFineLocationPermission(str2, uid);
                    if (workSource != null) {
                        enforceLocationHardware();
                        workSource.clearNames();
                    }
                    boolean isCalledFromPrivilegedContext = checkLocationHardware() && this.mShellCommand.getControlParam(CONTROL_PARAM_OVERRIDE_ASSUME_NO_PRIVILEGE_NAME) == 0;
                    DeathRecipient dr = new DeathRecipient() {
                        public void binderDied() {
                            if (RttServiceImpl.this.mDbg) {
                                String str = RttServiceImpl.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("binderDied: uid=");
                                stringBuilder.append(uid);
                                Log.v(str, stringBuilder.toString());
                            }
                            iBinder.unlinkToDeath(this, 0);
                            RttServiceImpl.this.mRttServiceSynchronized.mHandler.post(new -$$Lambda$RttServiceImpl$5$I2FdRwlbNnYI33vDhQLuFz17gV4(this, uid));
                        }
                    };
                    try {
                        iBinder.linkToDeath(dr, 0);
                        Handler handler = this.mRttServiceSynchronized.mHandler;
                        -$$Lambda$RttServiceImpl$3Addfr11wJKJqRbBre_6uYT6vT0 -__lambda_rttserviceimpl_3addfr11wjkjqrbbre_6uyt6vt0 = r1;
                        -$$Lambda$RttServiceImpl$3Addfr11wJKJqRbBre_6uYT6vT0 -__lambda_rttserviceimpl_3addfr11wjkjqrbbre_6uyt6vt02 = new -$$Lambda$RttServiceImpl$3Addfr11wJKJqRbBre_6uYT6vT0(this, workSource, uid, iBinder, dr, str2, rangingRequest, iRttCallback, isCalledFromPrivilegedContext);
                        handler.post(-__lambda_rttserviceimpl_3addfr11wjkjqrbbre_6uyt6vt0);
                        return;
                    } catch (RemoteException e) {
                        DeathRecipient deathRecipient = dr;
                        RemoteException remoteException = e;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Error on linkToDeath - ");
                        stringBuilder.append(e);
                        Log.e(str, stringBuilder.toString());
                        return;
                    }
                }
                try {
                    this.mRttMetrics.recordOverallStatus(3);
                    iRttCallback.onRangingFailure(2);
                } catch (RemoteException e2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("startRanging: disabled, callback failed -- ");
                    stringBuilder.append(e2);
                    Log.e(str, stringBuilder.toString());
                }
                return;
            }
            throw new IllegalArgumentException("Callback must not be null");
        }
    }

    public static /* synthetic */ void lambda$startRanging$3(RttServiceImpl rttServiceImpl, WorkSource workSource, int uid, IBinder binder, DeathRecipient dr, String callingPackage, RangingRequest request, IRttCallback callback, boolean isCalledFromPrivilegedContext) {
        int i;
        WorkSource sourceToUse = workSource;
        if (workSource == null || workSource.isEmpty()) {
            i = uid;
            sourceToUse = new WorkSource(i);
        } else {
            i = uid;
        }
        rttServiceImpl.mRttServiceSynchronized.queueRangingRequest(i, sourceToUse, binder, dr, callingPackage, request, callback, isCalledFromPrivilegedContext);
    }

    public void cancelRanging(WorkSource workSource) throws RemoteException {
        enforceLocationHardware();
        if (workSource != null) {
            workSource.clearNames();
        }
        if (workSource == null || workSource.isEmpty()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cancelRanging: invalid work-source -- ");
            stringBuilder.append(workSource);
            Log.e(str, stringBuilder.toString());
            return;
        }
        this.mRttServiceSynchronized.mHandler.post(new -$$Lambda$RttServiceImpl$yKNVX3EBmF3Pff0jYyCC81kRfuk(this, workSource));
    }

    public void onRangingResults(int cmdId, List<RttResult> results) {
        this.mRttServiceSynchronized.mHandler.post(new -$$Lambda$RttServiceImpl$tujYHkgiwM9Q0G7bKGi1Mj7KnVg(this, cmdId, results));
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
    }

    private void enforceLocationHardware() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.LOCATION_HARDWARE", TAG);
    }

    private boolean checkLocationHardware() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.LOCATION_HARDWARE") == 0;
    }

    private void sendRttStateChangedBroadcast(boolean enabled) {
        Intent intent = new Intent("android.net.wifi.rtt.action.WIFI_RTT_STATE_CHANGED");
        intent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Permission Denial: can't dump RttService from pid=");
            stringBuilder.append(Binder.getCallingPid());
            stringBuilder.append(", uid=");
            stringBuilder.append(Binder.getCallingUid());
            pw.println(stringBuilder.toString());
            return;
        }
        pw.println("Wi-Fi RTT Service");
        this.mRttServiceSynchronized.dump(fd, pw, args);
    }
}
