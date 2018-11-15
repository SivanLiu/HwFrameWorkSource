package com.android.server.connectivity.tethering;

import android.content.ContentResolver;
import android.net.ITetheringStatsProvider;
import android.net.ITetheringStatsProvider.Stub;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkStats;
import android.net.NetworkStats.Entry;
import android.net.RouteInfo;
import android.net.netlink.ConntrackMessage;
import android.net.netlink.NetlinkConstants;
import android.net.netlink.NetlinkSocket;
import android.net.util.IpUtils;
import android.net.util.SharedLog;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings.Global;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.text.TextUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.connectivity.tethering.OffloadHardwareInterface.ControlCallback;
import com.android.server.connectivity.tethering.OffloadHardwareInterface.ForwardedStats;
import com.android.server.job.controllers.JobStatus;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OffloadController {
    private static final String ANYIP = "0.0.0.0";
    private static final boolean DBG = false;
    private static final ForwardedStats EMPTY_STATS = new ForwardedStats();
    private static final String TAG = OffloadController.class.getSimpleName();
    private boolean mConfigInitialized;
    private final ContentResolver mContentResolver;
    private boolean mControlInitialized;
    private final HashMap<String, LinkProperties> mDownstreams;
    private Set<IpPrefix> mExemptPrefixes;
    private ConcurrentHashMap<String, ForwardedStats> mForwardedStats = new ConcurrentHashMap(16, 0.75f, 1);
    private final Handler mHandler;
    private final OffloadHardwareInterface mHwInterface;
    private HashMap<String, Long> mInterfaceQuotas = new HashMap();
    private Set<String> mLastLocalPrefixStrs;
    private final SharedLog mLog;
    private int mNatUpdateCallbacksReceived;
    private int mNatUpdateNetlinkErrors;
    private final INetworkManagementService mNms;
    private final ITetheringStatsProvider mStatsProvider;
    private LinkProperties mUpstreamLinkProperties;

    private class OffloadTetheringStatsProvider extends Stub {
        private OffloadTetheringStatsProvider() {
        }

        /* synthetic */ OffloadTetheringStatsProvider(OffloadController x0, AnonymousClass1 x1) {
            this();
        }

        public NetworkStats getTetherStats(int how) {
            Runnable updateStats = new -$$Lambda$OffloadController$OffloadTetheringStatsProvider$3TF0NI3fE8A-xW0925oMv3YzAOk(this);
            if (Looper.myLooper() == OffloadController.this.mHandler.getLooper()) {
                updateStats.run();
            } else {
                OffloadController.this.mHandler.post(updateStats);
            }
            NetworkStats stats = new NetworkStats(SystemClock.elapsedRealtime(), 0);
            Entry entry = new Entry();
            entry.set = 0;
            entry.tag = 0;
            entry.uid = how == 1 ? -5 : -1;
            for (Map.Entry<String, ForwardedStats> kv : OffloadController.this.mForwardedStats.entrySet()) {
                ForwardedStats value = (ForwardedStats) kv.getValue();
                entry.iface = (String) kv.getKey();
                entry.rxBytes = value.rxBytes;
                entry.txBytes = value.txBytes;
                stats.addValues(entry);
            }
            return stats;
        }

        public void setInterfaceQuota(String iface, long quotaBytes) {
            OffloadController.this.mHandler.post(new -$$Lambda$OffloadController$OffloadTetheringStatsProvider$qF4r7cON-_Hdae6JwwsXWcAUxEQ(this, quotaBytes, iface));
        }

        public static /* synthetic */ void lambda$setInterfaceQuota$1(OffloadTetheringStatsProvider offloadTetheringStatsProvider, long quotaBytes, String iface) {
            if (quotaBytes == -1) {
                OffloadController.this.mInterfaceQuotas.remove(iface);
            } else {
                OffloadController.this.mInterfaceQuotas.put(iface, Long.valueOf(quotaBytes));
            }
            OffloadController.this.maybeUpdateDataLimit(iface);
        }
    }

    private enum UpdateType {
        IF_NEEDED,
        FORCE
    }

    public OffloadController(Handler h, OffloadHardwareInterface hwi, ContentResolver contentResolver, INetworkManagementService nms, SharedLog log) {
        this.mHandler = h;
        this.mHwInterface = hwi;
        this.mContentResolver = contentResolver;
        this.mNms = nms;
        this.mStatsProvider = new OffloadTetheringStatsProvider(this, null);
        this.mLog = log.forSubComponent(TAG);
        this.mDownstreams = new HashMap();
        this.mExemptPrefixes = new HashSet();
        this.mLastLocalPrefixStrs = new HashSet();
        try {
            this.mNms.registerTetheringStatsProvider(this.mStatsProvider, getClass().getSimpleName());
        } catch (RemoteException e) {
            SharedLog sharedLog = this.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot register offload stats provider: ");
            stringBuilder.append(e);
            sharedLog.e(stringBuilder.toString());
        }
    }

    public boolean start() {
        if (started()) {
            return true;
        }
        if (isOffloadDisabled()) {
            this.mLog.i("tethering offload disabled");
            return false;
        }
        if (!this.mConfigInitialized) {
            this.mConfigInitialized = this.mHwInterface.initOffloadConfig();
            if (!this.mConfigInitialized) {
                this.mLog.i("tethering offload config not supported");
                stop();
                return false;
            }
        }
        this.mControlInitialized = this.mHwInterface.initOffloadControl(new ControlCallback() {
            public void onStarted() {
                if (OffloadController.this.started()) {
                    OffloadController.this.mLog.log("onStarted");
                }
            }

            public void onStoppedError() {
                if (OffloadController.this.started()) {
                    OffloadController.this.mLog.log("onStoppedError");
                }
            }

            public void onStoppedUnsupported() {
                if (OffloadController.this.started()) {
                    OffloadController.this.mLog.log("onStoppedUnsupported");
                    OffloadController.this.updateStatsForAllUpstreams();
                    OffloadController.this.forceTetherStatsPoll();
                }
            }

            public void onSupportAvailable() {
                if (OffloadController.this.started()) {
                    OffloadController.this.mLog.log("onSupportAvailable");
                    OffloadController.this.updateStatsForAllUpstreams();
                    OffloadController.this.forceTetherStatsPoll();
                    OffloadController.this.computeAndPushLocalPrefixes(UpdateType.FORCE);
                    OffloadController.this.pushAllDownstreamState();
                    OffloadController.this.pushUpstreamParameters(null);
                }
            }

            public void onStoppedLimitReached() {
                if (OffloadController.this.started()) {
                    OffloadController.this.mLog.log("onStoppedLimitReached");
                    OffloadController.this.updateStatsForCurrentUpstream();
                    OffloadController.this.forceTetherStatsPoll();
                }
            }

            public void onNatTimeoutUpdate(int proto, String srcAddr, int srcPort, String dstAddr, int dstPort) {
                if (OffloadController.this.started()) {
                    OffloadController.this.updateNatTimeout(proto, srcAddr, srcPort, dstAddr, dstPort);
                }
            }
        });
        boolean isStarted = started();
        if (isStarted) {
            this.mLog.log("tethering offload started");
            this.mNatUpdateCallbacksReceived = 0;
            this.mNatUpdateNetlinkErrors = 0;
        } else {
            this.mLog.i("tethering offload control not supported");
            stop();
        }
        return isStarted;
    }

    public void stop() {
        boolean wasStarted = started();
        updateStatsForCurrentUpstream();
        this.mUpstreamLinkProperties = null;
        this.mHwInterface.stopOffloadControl();
        this.mControlInitialized = false;
        this.mConfigInitialized = false;
        if (wasStarted) {
            this.mLog.log("tethering offload stopped");
        }
    }

    private boolean started() {
        return this.mConfigInitialized && this.mControlInitialized;
    }

    private String currentUpstreamInterface() {
        return this.mUpstreamLinkProperties != null ? this.mUpstreamLinkProperties.getInterfaceName() : null;
    }

    private void maybeUpdateStats(String iface) {
        if (!TextUtils.isEmpty(iface)) {
            ForwardedStats diff = this.mHwInterface.getForwardedStats(iface);
            ForwardedStats base = (ForwardedStats) this.mForwardedStats.get(iface);
            if (base != null) {
                diff.add(base);
            }
            this.mForwardedStats.put(iface, diff);
        }
    }

    private boolean maybeUpdateDataLimit(String iface) {
        if (!started() || !TextUtils.equals(iface, currentUpstreamInterface())) {
            return true;
        }
        Long limit = (Long) this.mInterfaceQuotas.get(iface);
        if (limit == null) {
            limit = Long.valueOf(JobStatus.NO_LATEST_RUNTIME);
        }
        return this.mHwInterface.setDataLimit(iface, limit.longValue());
    }

    private void updateStatsForCurrentUpstream() {
        maybeUpdateStats(currentUpstreamInterface());
    }

    private void updateStatsForAllUpstreams() {
        for (Map.Entry<String, ForwardedStats> kv : this.mForwardedStats.entrySet()) {
            maybeUpdateStats((String) kv.getKey());
        }
    }

    private void forceTetherStatsPoll() {
        try {
            this.mNms.tetherLimitReached(this.mStatsProvider);
        } catch (RemoteException e) {
            SharedLog sharedLog = this.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot report data limit reached: ");
            stringBuilder.append(e);
            sharedLog.e(stringBuilder.toString());
        }
    }

    public void setUpstreamLinkProperties(LinkProperties lp) {
        if (started() && !Objects.equals(this.mUpstreamLinkProperties, lp)) {
            String prevUpstream = currentUpstreamInterface();
            this.mUpstreamLinkProperties = lp != null ? new LinkProperties(lp) : null;
            String iface = currentUpstreamInterface();
            if (!TextUtils.isEmpty(iface)) {
                this.mForwardedStats.putIfAbsent(iface, EMPTY_STATS);
            }
            computeAndPushLocalPrefixes(UpdateType.IF_NEEDED);
            pushUpstreamParameters(prevUpstream);
        }
    }

    public void setLocalPrefixes(Set<IpPrefix> localPrefixes) {
        this.mExemptPrefixes = localPrefixes;
        if (started()) {
            computeAndPushLocalPrefixes(UpdateType.IF_NEEDED);
        }
    }

    public void notifyDownstreamLinkProperties(LinkProperties lp) {
        LinkProperties oldLp = (LinkProperties) this.mDownstreams.put(lp.getInterfaceName(), new LinkProperties(lp));
        if (!Objects.equals(oldLp, lp) && started()) {
            pushDownstreamState(oldLp, lp);
        }
    }

    private void pushDownstreamState(LinkProperties oldLp, LinkProperties newLp) {
        String ifname = newLp.getInterfaceName();
        List<RouteInfo> oldRoutes = oldLp != null ? oldLp.getRoutes() : Collections.EMPTY_LIST;
        List<RouteInfo> newRoutes = newLp.getRoutes();
        for (RouteInfo ri : oldRoutes) {
            if (!shouldIgnoreDownstreamRoute(ri)) {
                if (!newRoutes.contains(ri)) {
                    this.mHwInterface.removeDownstreamPrefix(ifname, ri.getDestination().toString());
                }
            }
        }
        for (RouteInfo ri2 : newRoutes) {
            if (!shouldIgnoreDownstreamRoute(ri2)) {
                if (!oldRoutes.contains(ri2)) {
                    this.mHwInterface.addDownstreamPrefix(ifname, ri2.getDestination().toString());
                }
            }
        }
    }

    private void pushAllDownstreamState() {
        for (LinkProperties lp : this.mDownstreams.values()) {
            pushDownstreamState(null, lp);
        }
    }

    public void removeDownstreamInterface(String ifname) {
        LinkProperties lp = (LinkProperties) this.mDownstreams.remove(ifname);
        if (lp != null && started()) {
            for (RouteInfo route : lp.getRoutes()) {
                if (!shouldIgnoreDownstreamRoute(route)) {
                    this.mHwInterface.removeDownstreamPrefix(ifname, route.getDestination().toString());
                }
            }
        }
    }

    private boolean isOffloadDisabled() {
        return Global.getInt(this.mContentResolver, "tether_offload_disabled", this.mHwInterface.getDefaultTetherOffloadDisabled()) != 0;
    }

    private boolean pushUpstreamParameters(String prevUpstream) {
        String iface = currentUpstreamInterface();
        boolean success = false;
        if (TextUtils.isEmpty(iface)) {
            boolean rval = this.mHwInterface.setUpstreamParameters(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, ANYIP, ANYIP, null);
            maybeUpdateStats(prevUpstream);
            return rval;
        }
        ArrayList<String> v6gateways = new ArrayList();
        String v4addr = null;
        String v4gateway = null;
        for (InetAddress ip : this.mUpstreamLinkProperties.getAddresses()) {
            if (ip instanceof Inet4Address) {
                v4addr = ip.getHostAddress();
                break;
            }
        }
        for (RouteInfo ri : this.mUpstreamLinkProperties.getRoutes()) {
            if (ri.hasGateway()) {
                String gateway = ri.getGateway().getHostAddress();
                if (ri.isIPv4Default()) {
                    v4gateway = gateway;
                } else if (ri.isIPv6Default()) {
                    v6gateways.add(gateway);
                }
            }
        }
        OffloadHardwareInterface offloadHardwareInterface = this.mHwInterface;
        if (!v6gateways.isEmpty()) {
            success = v6gateways;
        }
        success = offloadHardwareInterface.setUpstreamParameters(iface, v4addr, v4gateway, success);
        if (!success) {
            return success;
        }
        maybeUpdateStats(prevUpstream);
        success = maybeUpdateDataLimit(iface);
        if (!success) {
            SharedLog sharedLog = this.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Setting data limit for ");
            stringBuilder.append(iface);
            stringBuilder.append(" failed, disabling offload.");
            sharedLog.log(stringBuilder.toString());
            stop();
        }
        return success;
    }

    private boolean computeAndPushLocalPrefixes(UpdateType how) {
        boolean force = how == UpdateType.FORCE;
        Set<String> localPrefixStrs = computeLocalPrefixStrings(this.mExemptPrefixes, this.mUpstreamLinkProperties);
        if (!force && this.mLastLocalPrefixStrs.equals(localPrefixStrs)) {
            return true;
        }
        this.mLastLocalPrefixStrs = localPrefixStrs;
        return this.mHwInterface.setLocalPrefixes(new ArrayList(localPrefixStrs));
    }

    private static Set<String> computeLocalPrefixStrings(Set<IpPrefix> localPrefixes, LinkProperties upstreamLinkProperties) {
        Set<IpPrefix> prefixSet = new HashSet(localPrefixes);
        if (upstreamLinkProperties != null) {
            for (LinkAddress linkAddr : upstreamLinkProperties.getLinkAddresses()) {
                if (linkAddr.isGlobalPreferred()) {
                    InetAddress ip = linkAddr.getAddress();
                    if (ip instanceof Inet6Address) {
                        prefixSet.add(new IpPrefix(ip, 128));
                    }
                }
            }
        }
        HashSet<String> localPrefixStrs = new HashSet();
        for (IpPrefix pfx : prefixSet) {
            localPrefixStrs.add(pfx.toString());
        }
        return localPrefixStrs;
    }

    private static boolean shouldIgnoreDownstreamRoute(RouteInfo route) {
        if (route.getDestinationLinkAddress().isGlobalPreferred()) {
            return false;
        }
        return true;
    }

    public void dump(IndentingPrintWriter pw) {
        if (isOffloadDisabled()) {
            pw.println("Offload disabled");
            return;
        }
        boolean isStarted = started();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Offload HALs ");
        stringBuilder.append(isStarted ? "started" : "not started");
        pw.println(stringBuilder.toString());
        LinkProperties lp = this.mUpstreamLinkProperties;
        String upstream = lp != null ? lp.getInterfaceName() : null;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Current upstream: ");
        stringBuilder2.append(upstream);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Exempt prefixes: ");
        stringBuilder2.append(this.mLastLocalPrefixStrs);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("NAT timeout update callbacks received during the ");
        stringBuilder2.append(isStarted ? "current" : "last");
        stringBuilder2.append(" offload session: ");
        stringBuilder2.append(this.mNatUpdateCallbacksReceived);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("NAT timeout update netlink errors during the ");
        stringBuilder2.append(isStarted ? "current" : "last");
        stringBuilder2.append(" offload session: ");
        stringBuilder2.append(this.mNatUpdateNetlinkErrors);
        pw.println(stringBuilder2.toString());
    }

    private void updateNatTimeout(int proto, String srcAddr, int srcPort, String dstAddr, int dstPort) {
        String str = srcAddr;
        String str2 = dstAddr;
        SharedLog sharedLog;
        if (protoNameFor(proto) == null) {
            sharedLog = this.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown NAT update callback protocol: ");
            stringBuilder.append(proto);
            sharedLog.e(stringBuilder.toString());
            return;
        }
        int i = proto;
        Inet4Address src = parseIPv4Address(srcAddr);
        StringBuilder stringBuilder2;
        if (src == null) {
            sharedLog = this.mLog;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to parse IPv4 address: ");
            stringBuilder2.append(str);
            sharedLog.e(stringBuilder2.toString());
        } else if (IpUtils.isValidUdpOrTcpPort(srcPort)) {
            int i2 = srcPort;
            Inet4Address dst = parseIPv4Address(dstAddr);
            if (dst == null) {
                sharedLog = this.mLog;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to parse IPv4 address: ");
                stringBuilder2.append(str2);
                sharedLog.e(stringBuilder2.toString());
            } else if (IpUtils.isValidUdpOrTcpPort(dstPort)) {
                int i3 = dstPort;
                this.mNatUpdateCallbacksReceived++;
                str = String.format("%s (%s, %s) -> (%s, %s)", new Object[]{protoName, str, Integer.valueOf(srcPort), str2, Integer.valueOf(dstPort)});
                byte[] msg = ConntrackMessage.newIPv4TimeoutUpdateRequest(i, src, i2, dst, i3, connectionTimeoutUpdateSecondsFor(proto));
                try {
                    NetlinkSocket.sendOneShotKernelMessage(OsConstants.NETLINK_NETFILTER, msg);
                } catch (ErrnoException e) {
                    this.mNatUpdateNetlinkErrors++;
                    SharedLog sharedLog2 = this.mLog;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Error updating NAT conntrack entry >");
                    stringBuilder3.append(str);
                    stringBuilder3.append("<: ");
                    stringBuilder3.append(e);
                    stringBuilder3.append(", msg: ");
                    stringBuilder3.append(NetlinkConstants.hexify(msg));
                    sharedLog2.e(stringBuilder3.toString());
                    sharedLog2 = this.mLog;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("NAT timeout update callbacks received: ");
                    stringBuilder3.append(this.mNatUpdateCallbacksReceived);
                    sharedLog2.log(stringBuilder3.toString());
                    sharedLog2 = this.mLog;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("NAT timeout update netlink errors: ");
                    stringBuilder3.append(this.mNatUpdateNetlinkErrors);
                    sharedLog2.log(stringBuilder3.toString());
                }
            } else {
                sharedLog = this.mLog;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Invalid dst port: ");
                stringBuilder2.append(dstPort);
                sharedLog.e(stringBuilder2.toString());
            }
        } else {
            sharedLog = this.mLog;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Invalid src port: ");
            stringBuilder2.append(srcPort);
            sharedLog.e(stringBuilder2.toString());
        }
    }

    private static Inet4Address parseIPv4Address(String addrString) {
        try {
            InetAddress ip = InetAddress.parseNumericAddress(addrString);
            if (ip instanceof Inet4Address) {
                return (Inet4Address) ip;
            }
        } catch (IllegalArgumentException e) {
        }
        return null;
    }

    private static String protoNameFor(int proto) {
        if (proto == OsConstants.IPPROTO_UDP) {
            return "UDP";
        }
        if (proto == OsConstants.IPPROTO_TCP) {
            return "TCP";
        }
        return null;
    }

    private static int connectionTimeoutUpdateSecondsFor(int proto) {
        if (proto == OsConstants.IPPROTO_TCP) {
            return 432000;
        }
        return 180;
    }
}
