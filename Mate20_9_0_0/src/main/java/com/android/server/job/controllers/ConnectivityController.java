package com.android.server.job.controllers;

import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.ConnectivityManager.OnNetworkActiveListener;
import android.net.INetworkPolicyListener;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkPolicyManager;
import android.net.NetworkPolicyManager.Listener;
import android.net.NetworkRequest;
import android.net.NetworkRequest.Builder;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.JobSchedulerService;
import com.android.server.job.JobSchedulerService.Constants;
import java.util.Objects;
import java.util.function.Predicate;

public final class ConnectivityController extends StateController implements OnNetworkActiveListener {
    private static final boolean DEBUG;
    private static final String TAG = "JobScheduler.Connectivity";
    private final ConnectivityManager mConnManager = ((ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class));
    private final INetworkPolicyListener mNetPolicyListener = new Listener() {
        public void onUidRulesChanged(int uid, int uidRules) {
            if (ConnectivityController.DEBUG) {
                String str = ConnectivityController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onUidRulesChanged: ");
                stringBuilder.append(uid);
                Slog.v(str, stringBuilder.toString());
            }
            ConnectivityController.this.updateTrackedJobs(uid, null);
        }

        public void onUidPoliciesChanged(int uid, int uidPolicies) {
            if (ConnectivityController.DEBUG) {
                String str = ConnectivityController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Uid policy changed for ");
                stringBuilder.append(uid);
                Slog.v(str, stringBuilder.toString());
            }
            ConnectivityController.this.updateTrackedJobs(uid, null);
        }
    };
    private final NetworkPolicyManager mNetPolicyManager = ((NetworkPolicyManager) this.mContext.getSystemService(NetworkPolicyManager.class));
    private final NetworkCallback mNetworkCallback = new NetworkCallback() {
        public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
            if (ConnectivityController.DEBUG) {
                String str = ConnectivityController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onCapabilitiesChanged: ");
                stringBuilder.append(network);
                Slog.v(str, stringBuilder.toString());
            }
            ConnectivityController.this.updateTrackedJobs(-1, network);
        }

        public void onLost(Network network) {
            if (ConnectivityController.DEBUG) {
                String str = ConnectivityController.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onLost: ");
                stringBuilder.append(network);
                Slog.v(str, stringBuilder.toString());
            }
            ConnectivityController.this.updateTrackedJobs(-1, network);
        }
    };
    @GuardedBy("mLock")
    private final ArraySet<JobStatus> mTrackedJobs = new ArraySet();

    static {
        boolean z = JobSchedulerService.DEBUG || Log.isLoggable(TAG, 3);
        DEBUG = z;
    }

    public ConnectivityController(JobSchedulerService service) {
        super(service);
        this.mConnManager.registerNetworkCallback(new Builder().clearCapabilities().build(), this.mNetworkCallback);
        this.mNetPolicyManager.registerListener(this.mNetPolicyListener);
    }

    @GuardedBy("mLock")
    public void maybeStartTrackingJobLocked(JobStatus jobStatus, JobStatus lastJob) {
        if (jobStatus.hasConnectivityConstraint()) {
            updateConstraintsSatisfied(jobStatus);
            this.mTrackedJobs.add(jobStatus);
            jobStatus.setTrackingController(2);
        }
    }

    @GuardedBy("mLock")
    public void maybeStopTrackingJobLocked(JobStatus jobStatus, JobStatus incomingJob, boolean forUpdate) {
        if (jobStatus.clearTrackingController(2)) {
            this.mTrackedJobs.remove(jobStatus);
        }
    }

    private static boolean isInsane(JobStatus jobStatus, Network network, NetworkCapabilities capabilities, Constants constants) {
        long estimatedBytes = jobStatus.getEstimatedNetworkBytes();
        if (estimatedBytes == -1) {
            return false;
        }
        long slowest = (long) NetworkCapabilities.minBandwidth(capabilities.getLinkDownstreamBandwidthKbps(), capabilities.getLinkUpstreamBandwidthKbps());
        if (slowest == 0) {
            return false;
        }
        long estimatedMillis = (1000 * estimatedBytes) / ((1024 * slowest) / 8);
        if (estimatedMillis <= 600000) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Estimated ");
        stringBuilder.append(estimatedBytes);
        stringBuilder.append(" bytes over ");
        stringBuilder.append(slowest);
        stringBuilder.append(" kbps network would take ");
        stringBuilder.append(estimatedMillis);
        stringBuilder.append("ms; that's insane!");
        Slog.w(str, stringBuilder.toString());
        return true;
    }

    private static boolean isCongestionDelayed(JobStatus jobStatus, Network network, NetworkCapabilities capabilities, Constants constants) {
        boolean z = false;
        if (capabilities.hasCapability(20)) {
            return false;
        }
        if (jobStatus.getFractionRunTime() < constants.CONN_CONGESTION_DELAY_FRAC) {
            z = true;
        }
        return z;
    }

    private static boolean isStrictSatisfied(JobStatus jobStatus, Network network, NetworkCapabilities capabilities, Constants constants) {
        return jobStatus.getJob().getRequiredNetwork().networkCapabilities.satisfiedByNetworkCapabilities(capabilities);
    }

    private static boolean isRelaxedSatisfied(JobStatus jobStatus, Network network, NetworkCapabilities capabilities, Constants constants) {
        boolean z = false;
        if (!jobStatus.getJob().isPrefetch() || !new NetworkCapabilities(jobStatus.getJob().getRequiredNetwork().networkCapabilities).removeCapability(11).satisfiedByNetworkCapabilities(capabilities)) {
            return false;
        }
        if (jobStatus.getFractionRunTime() > constants.CONN_PREFETCH_RELAX_FRAC) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:16:0x0024, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting
    static boolean isSatisfied(JobStatus jobStatus, Network network, NetworkCapabilities capabilities, Constants constants) {
        if (network == null || capabilities == null || isInsane(jobStatus, network, capabilities, constants) || isCongestionDelayed(jobStatus, network, capabilities, constants)) {
            return false;
        }
        if (isStrictSatisfied(jobStatus, network, capabilities, constants) || isRelaxedSatisfied(jobStatus, network, capabilities, constants)) {
            return true;
        }
        return false;
    }

    private boolean updateConstraintsSatisfied(JobStatus jobStatus) {
        Network network = this.mConnManager.getActiveNetworkForUid(jobStatus.getSourceUid());
        return updateConstraintsSatisfied(jobStatus, network, this.mConnManager.getNetworkCapabilities(network));
    }

    private boolean updateConstraintsSatisfied(JobStatus jobStatus, Network network, NetworkCapabilities capabilities) {
        boolean changed = true;
        NetworkInfo info = this.mConnManager.getNetworkInfoForUid(network, jobStatus.getSourceUid(), (jobStatus.getFlags() & 1) != 0);
        boolean connected = info != null && info.isConnected();
        boolean satisfied = isSatisfied(jobStatus, network, capabilities, this.mConstants);
        if (!(connected && satisfied)) {
            changed = false;
        }
        changed = jobStatus.setConnectivityConstraintSatisfied(changed);
        jobStatus.network = network;
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Connectivity ");
            stringBuilder.append(changed ? "CHANGED" : "unchanged");
            stringBuilder.append(" for ");
            stringBuilder.append(jobStatus);
            stringBuilder.append(": connected=");
            stringBuilder.append(connected);
            stringBuilder.append(" satisfied=");
            stringBuilder.append(satisfied);
            Slog.i(str, stringBuilder.toString());
        }
        return changed;
    }

    private void updateTrackedJobs(int filterUid, Network filterNetwork) {
        int i = filterUid;
        Network network = filterNetwork;
        synchronized (this.mLock) {
            SparseArray<Network> uidToNetwork = new SparseArray();
            SparseArray<NetworkCapabilities> networkToCapabilities = new SparseArray();
            boolean changed = false;
            boolean z = true;
            int i2 = this.mTrackedJobs.size() - 1;
            while (i2 >= 0) {
                JobStatus js = (JobStatus) this.mTrackedJobs.valueAt(i2);
                int uid = js.getSourceUid();
                boolean networkMatch = false;
                int netId = -1;
                boolean uidMatch = (i == -1 || i == uid) ? z : false;
                if (uidMatch) {
                    Network network2 = (Network) uidToNetwork.get(uid);
                    if (network2 == null) {
                        network2 = this.mConnManager.getActiveNetworkForUid(uid);
                        uidToNetwork.put(uid, network2);
                    }
                    if (network == null || Objects.equals(network, network2)) {
                        networkMatch = z;
                    }
                    boolean forceUpdate = Objects.equals(js.network, network2) ^ z;
                    if (networkMatch || forceUpdate) {
                        NetworkCapabilities capabilities;
                        if (network2 != null) {
                            netId = network2.netId;
                        }
                        NetworkCapabilities capabilities2 = (NetworkCapabilities) networkToCapabilities.get(netId);
                        if (capabilities2 == null) {
                            capabilities = this.mConnManager.getNetworkCapabilities(network2);
                            networkToCapabilities.put(netId, capabilities);
                        } else {
                            capabilities = capabilities2;
                        }
                        changed |= updateConstraintsSatisfied(js, network2, capabilities);
                    }
                }
                i2--;
                z = true;
            }
            if (changed) {
                this.mStateChangedListener.onControllerStateChanged();
            }
        }
    }

    public void onNetworkActive() {
        synchronized (this.mLock) {
            for (int i = this.mTrackedJobs.size() - 1; i >= 0; i--) {
                JobStatus js = (JobStatus) this.mTrackedJobs.valueAt(i);
                if (js.isReady()) {
                    if (DEBUG) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Running ");
                        stringBuilder.append(js);
                        stringBuilder.append(" due to network activity.");
                        Slog.d(str, stringBuilder.toString());
                    }
                    this.mStateChangedListener.onRunJobNow(js);
                }
            }
        }
    }

    @GuardedBy("mLock")
    public void dumpControllerStateLocked(IndentingPrintWriter pw, Predicate<JobStatus> predicate) {
        for (int i = 0; i < this.mTrackedJobs.size(); i++) {
            JobStatus js = (JobStatus) this.mTrackedJobs.valueAt(i);
            if (predicate.test(js)) {
                pw.print("#");
                js.printUniqueId(pw);
                pw.print(" from ");
                UserHandle.formatUid(pw, js.getSourceUid());
                pw.print(": ");
                pw.print(js.getJob().getRequiredNetwork());
                pw.println();
            }
        }
    }

    @GuardedBy("mLock")
    public void dumpControllerStateLocked(ProtoOutputStream proto, long fieldId, Predicate<JobStatus> predicate) {
        ProtoOutputStream protoOutputStream = proto;
        long token = proto.start(fieldId);
        long mToken = protoOutputStream.start(1146756268035L);
        for (int i = 0; i < this.mTrackedJobs.size(); i++) {
            JobStatus js = (JobStatus) this.mTrackedJobs.valueAt(i);
            if (predicate.test(js)) {
                long jsToken = protoOutputStream.start(2246267895810L);
                js.writeToShortProto(protoOutputStream, 1146756268033L);
                protoOutputStream.write(1120986464258L, js.getSourceUid());
                NetworkRequest rn = js.getJob().getRequiredNetwork();
                if (rn != null) {
                    rn.writeToProto(protoOutputStream, 1146756268035L);
                }
                protoOutputStream.end(jsToken);
            }
        }
        Predicate<JobStatus> predicate2 = predicate;
        protoOutputStream.end(mToken);
        protoOutputStream.end(token);
    }
}
