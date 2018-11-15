package android.net.ip;

import android.content.Context;
import android.net.LinkProperties;
import android.net.LinkProperties.ProvisioningChange;
import android.net.RouteInfo;
import android.net.ip.IpNeighborMonitor.NeighborEvent;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.IpReachabilityEvent;
import android.net.util.InterfaceParams;
import android.net.util.MultinetworkPolicyTracker;
import android.net.util.SharedLog;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.DumpUtils.Dump;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.io.PrintWriter;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class IpReachabilityMonitor {
    private static final boolean DBG = false;
    private static final String TAG = "IpReachabilityMonitor";
    private static final boolean VDBG = false;
    private final Callback mCallback;
    private final Dependencies mDependencies;
    private final InterfaceParams mInterfaceParams;
    private final IpNeighborMonitor mIpNeighborMonitor;
    private volatile long mLastProbeTimeMs;
    private LinkProperties mLinkProperties;
    private final SharedLog mLog;
    private final IpConnectivityLog mMetricsLog;
    private final MultinetworkPolicyTracker mMultinetworkPolicyTracker;
    private Map<InetAddress, NeighborEvent> mNeighborWatchList;

    public interface Callback {
        void notifyLost(InetAddress inetAddress, String str);
    }

    interface Dependencies {
        void acquireWakeLock(long j);

        static Dependencies makeDefault(Context context, String iface) {
            String lockName = new StringBuilder();
            lockName.append("IpReachabilityMonitor.");
            lockName.append(iface);
            final WakeLock lock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, lockName.toString());
            return new Dependencies() {
                public void acquireWakeLock(long durationMs) {
                    lock.acquire(durationMs);
                }
            };
        }
    }

    public IpReachabilityMonitor(Context context, InterfaceParams ifParams, Handler h, SharedLog log, Callback callback, MultinetworkPolicyTracker tracker) {
        this(ifParams, h, log, callback, tracker, Dependencies.makeDefault(context, ifParams.name));
    }

    @VisibleForTesting
    IpReachabilityMonitor(InterfaceParams ifParams, Handler h, SharedLog log, Callback callback, MultinetworkPolicyTracker tracker, Dependencies dependencies) {
        this.mMetricsLog = new IpConnectivityLog();
        this.mLinkProperties = new LinkProperties();
        this.mNeighborWatchList = new HashMap();
        if (ifParams != null) {
            this.mInterfaceParams = ifParams;
            this.mLog = log.forSubComponent(TAG);
            this.mCallback = callback;
            this.mMultinetworkPolicyTracker = tracker;
            this.mDependencies = dependencies;
            this.mIpNeighborMonitor = new IpNeighborMonitor(h, this.mLog, new -$$Lambda$IpReachabilityMonitor$5Sg30oRgfU2r5ogQj53SRYnnFiQ(this));
            this.mIpNeighborMonitor.start();
            return;
        }
        throw new IllegalArgumentException("null InterfaceParams");
    }

    public static /* synthetic */ void lambda$new$0(IpReachabilityMonitor ipReachabilityMonitor, NeighborEvent event) {
        if (ipReachabilityMonitor.mInterfaceParams.index == event.ifindex && ipReachabilityMonitor.mNeighborWatchList.containsKey(event.ip)) {
            NeighborEvent prev = (NeighborEvent) ipReachabilityMonitor.mNeighborWatchList.put(event.ip, event);
            if (event.nudState == (short) 32) {
                SharedLog sharedLog = ipReachabilityMonitor.mLog;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ALERT neighbor went from: ");
                stringBuilder.append(prev);
                stringBuilder.append(" to: ");
                stringBuilder.append(event);
                sharedLog.w(stringBuilder.toString());
                ipReachabilityMonitor.handleNeighborLost(event);
            }
        }
    }

    public void stop() {
        this.mIpNeighborMonitor.stop();
        clearLinkProperties();
    }

    public void dump(PrintWriter pw) {
        DumpUtils.dumpAsync(this.mIpNeighborMonitor.getHandler(), new Dump() {
            public void dump(PrintWriter pw, String prefix) {
                pw.println(IpReachabilityMonitor.this.describeWatchList("\n"));
            }
        }, pw, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, 1000);
    }

    private String describeWatchList() {
        return describeWatchList(" ");
    }

    private String describeWatchList(String sep) {
        StringBuilder sb = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("iface{");
        stringBuilder.append(this.mInterfaceParams);
        stringBuilder.append("},");
        stringBuilder.append(sep);
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("ntable=[");
        stringBuilder.append(sep);
        sb.append(stringBuilder.toString());
        String delimiter = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        for (Entry<InetAddress, NeighborEvent> entry : this.mNeighborWatchList.entrySet()) {
            sb.append(delimiter);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(((InetAddress) entry.getKey()).getHostAddress());
            stringBuilder2.append(SliceAuthority.DELIMITER);
            stringBuilder2.append(entry.getValue());
            sb.append(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(",");
            stringBuilder2.append(sep);
            delimiter = stringBuilder2.toString();
        }
        sb.append("]");
        return sb.toString();
    }

    private static boolean isOnLink(List<RouteInfo> routes, InetAddress ip) {
        for (RouteInfo route : routes) {
            if (!route.hasGateway() && route.matches(ip)) {
                return true;
            }
        }
        return false;
    }

    public void updateLinkProperties(LinkProperties lp) {
        if (this.mInterfaceParams.name.equals(lp.getInterfaceName())) {
            this.mLinkProperties = new LinkProperties(lp);
            Map<InetAddress, NeighborEvent> newNeighborWatchList = new HashMap();
            List<RouteInfo> routes = this.mLinkProperties.getRoutes();
            for (RouteInfo route : routes) {
                if (route.hasGateway()) {
                    InetAddress gw = route.getGateway();
                    if (isOnLink(routes, gw)) {
                        newNeighborWatchList.put(gw, (NeighborEvent) this.mNeighborWatchList.getOrDefault(gw, null));
                    }
                }
            }
            for (InetAddress dns : lp.getDnsServers()) {
                if (isOnLink(routes, dns)) {
                    newNeighborWatchList.put(dns, (NeighborEvent) this.mNeighborWatchList.getOrDefault(dns, null));
                }
            }
            this.mNeighborWatchList = newNeighborWatchList;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requested LinkProperties interface '");
        stringBuilder.append(lp.getInterfaceName());
        stringBuilder.append("' does not match: ");
        stringBuilder.append(this.mInterfaceParams.name);
        Log.wtf(str, stringBuilder.toString());
    }

    public void clearLinkProperties() {
        this.mLinkProperties.clear();
        this.mNeighborWatchList.clear();
    }

    private void handleNeighborLost(NeighborEvent event) {
        LinkProperties whatIfLp = new LinkProperties(this.mLinkProperties);
        InetAddress ip = null;
        for (Entry<InetAddress, NeighborEvent> entry : this.mNeighborWatchList.entrySet()) {
            if (((NeighborEvent) entry.getValue()).nudState == (short) 32) {
                ip = (InetAddress) entry.getKey();
                for (RouteInfo route : this.mLinkProperties.getRoutes()) {
                    if (ip.equals(route.getGateway())) {
                        whatIfLp.removeRoute(route);
                    }
                }
                if (avoidingBadLinks() || !(ip instanceof Inet6Address)) {
                    whatIfLp.removeDnsServer(ip);
                }
            }
        }
        ProvisioningChange delta = LinkProperties.compareProvisioning(this.mLinkProperties, whatIfLp);
        if (delta == ProvisioningChange.LOST_PROVISIONING) {
            String logMsg = new StringBuilder();
            logMsg.append("FAILURE: LOST_PROVISIONING, ");
            logMsg.append(event);
            logMsg = logMsg.toString();
            Log.w(TAG, logMsg);
            if (this.mCallback != null) {
                this.mCallback.notifyLost(ip, logMsg);
            }
        }
        logNudFailed(delta);
    }

    private boolean avoidingBadLinks() {
        return this.mMultinetworkPolicyTracker == null || this.mMultinetworkPolicyTracker.getAvoidBadWifi();
    }

    public void probeAll() {
        List<InetAddress> ipProbeList = new ArrayList(this.mNeighborWatchList.keySet());
        if (!ipProbeList.isEmpty()) {
            this.mDependencies.acquireWakeLock(getProbeWakeLockDuration());
        }
        for (InetAddress ip : ipProbeList) {
            int rval = IpNeighborMonitor.startKernelNeighborProbe(this.mInterfaceParams.index, ip);
            this.mLog.log(String.format("put neighbor %s into NUD_PROBE state (rval=%d)", new Object[]{ip.getHostAddress(), Integer.valueOf(rval)}));
            logEvent(256, rval);
        }
        this.mLastProbeTimeMs = SystemClock.elapsedRealtime();
    }

    private static long getProbeWakeLockDuration() {
        return 3500;
    }

    private void logEvent(int probeType, int errorCode) {
        this.mMetricsLog.log(this.mInterfaceParams.name, new IpReachabilityEvent((errorCode & 255) | probeType));
    }

    private void logNudFailed(ProvisioningChange delta) {
        boolean isProvisioningLost = false;
        boolean isFromProbe = SystemClock.elapsedRealtime() - this.mLastProbeTimeMs < getProbeWakeLockDuration();
        if (delta == ProvisioningChange.LOST_PROVISIONING) {
            isProvisioningLost = true;
        }
        this.mMetricsLog.log(this.mInterfaceParams.name, new IpReachabilityEvent(IpReachabilityEvent.nudFailureEventType(isFromProbe, isProvisioningLost)));
    }
}
