package android.net.ip;

import android.content.Context;
import android.net.DhcpResults;
import android.net.INetd;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.LinkProperties.ProvisioningChange;
import android.net.Network;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.apf.ApfCapabilities;
import android.net.apf.ApfFilter;
import android.net.apf.ApfFilter.ApfConfiguration;
import android.net.dhcp.DhcpClient;
import android.net.dhcp.HwDhcpClient;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.IpManagerEvent;
import android.net.util.InterfaceParams;
import android.net.util.MultinetworkPolicyTracker;
import android.net.util.NetdService;
import android.net.util.SharedLog;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.INetworkManagementService.Stub;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IState;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.net.NetlinkTracker;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IpClient extends StateMachine {
    private static final String CLAT_PREFIX = "v4-";
    private static final int CMD_CONFIRM = 4;
    private static final int CMD_SET_MULTICAST_FILTER = 9;
    private static final int CMD_SET_SCREEN_OFF_FILTER = 20;
    private static final int CMD_START = 3;
    private static final int CMD_STOP = 2;
    private static final int CMD_TERMINATE_AFTER_STOP = 1;
    private static final int CMD_UPDATE_HTTP_PROXY = 8;
    private static final int CMD_UPDATE_TCP_BUFFER_SIZES = 7;
    private static final boolean DBG = false;
    public static final String DUMP_ARG = "ipclient";
    public static final String DUMP_ARG_CONFIRM = "confirm";
    private static final int EVENT_DHCPACTION_TIMEOUT = 11;
    private static final int EVENT_NETLINK_LINKPROPERTIES_CHANGED = 6;
    private static final int EVENT_PRE_DHCP_ACTION_COMPLETE = 5;
    private static final int EVENT_PROVISIONING_TIMEOUT = 10;
    private static final int EVENT_READ_PACKET_FILTER_COMPLETE = 12;
    private static final int IMMEDIATE_FAILURE_DURATION = 0;
    private static final int MAX_LOG_RECORDS = 500;
    private static final int MAX_PACKET_RECORDS = 100;
    private static final boolean NO_CALLBACKS = false;
    private static final boolean SEND_CALLBACKS = true;
    private static final Class[] sMessageClasses = new Class[]{IpClient.class, DhcpClient.class};
    private static final ConcurrentHashMap<String, LocalLog> sPktLogs = new ConcurrentHashMap();
    private static final ConcurrentHashMap<String, SharedLog> sSmLogs = new ConcurrentHashMap();
    private static final SparseArray<String> sWhatToString = MessageUtils.findMessageNames(sMessageClasses);
    private boolean forceDhcpDiscovery;
    private final ConditionVariable mApfDataSnapshotComplete;
    private ApfFilter mApfFilter;
    @VisibleForTesting
    protected final Callback mCallback;
    private final String mClatInterfaceName;
    private ProvisioningConfiguration mConfiguration;
    private final LocalLog mConnectivityPacketLog;
    private final Context mContext;
    private final Dependencies mDependencies;
    private final WakeupMessage mDhcpActionTimeoutAlarm;
    private DhcpClient mDhcpClient;
    private DhcpResults mDhcpResults;
    private int mDhcp_Flag;
    private ProxyInfo mHttpProxy;
    private final InterfaceController mInterfaceCtrl;
    private final String mInterfaceName;
    private InterfaceParams mInterfaceParams;
    private IpReachabilityMonitor mIpReachabilityMonitor;
    private LinkProperties mLinkProperties;
    private final SharedLog mLog;
    private final IpConnectivityLog mMetricsLog;
    private final MessageHandlingLogger mMsgStateLogger;
    private boolean mMulticastFiltering;
    private MultinetworkPolicyTracker mMultinetworkPolicyTracker;
    private final NetlinkTracker mNetlinkTracker;
    private final INetworkManagementService mNwService;
    private String mPendingSSID;
    private final WakeupMessage mProvisioningTimeoutAlarm;
    private final State mRunningState;
    private final CountDownLatch mShutdownLatch;
    private long mStartTimeMillis;
    private final State mStartedState;
    private final State mStoppedState;
    private final State mStoppingState;
    private final String mTag;
    private String mTcpBufferSizes;

    /* renamed from: android.net.ip.IpClient$5 */
    static /* synthetic */ class AnonymousClass5 {
        static final /* synthetic */ int[] $SwitchMap$android$net$LinkProperties$ProvisioningChange = new int[ProvisioningChange.values().length];

        static {
            try {
                $SwitchMap$android$net$LinkProperties$ProvisioningChange[ProvisioningChange.GAINED_PROVISIONING.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$LinkProperties$ProvisioningChange[ProvisioningChange.LOST_PROVISIONING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public static class Callback {
        public void onPreDhcpAction() {
        }

        public void onPostDhcpAction() {
        }

        public void onNewDhcpResults(DhcpResults dhcpResults) {
        }

        public void onProvisioningSuccess(LinkProperties newLp) {
        }

        public void onProvisioningFailure(LinkProperties newLp) {
        }

        public void onLinkPropertiesChange(LinkProperties newLp) {
        }

        public void onReachabilityLost(String logMsg) {
        }

        public void onQuit() {
        }

        public void installPacketFilter(byte[] filter) {
        }

        public void startReadPacketFilter() {
        }

        public void setFallbackMulticastFilter(boolean enabled) {
        }

        public void setNeighborDiscoveryOffload(boolean enable) {
        }
    }

    public static class Dependencies {
        public INetworkManagementService getNMS() {
            return Stub.asInterface(ServiceManager.getService("network_management"));
        }

        public INetd getNetd() {
            return NetdService.getInstance();
        }

        public InterfaceParams getInterfaceParams(String ifname) {
            return InterfaceParams.getByName(ifname);
        }
    }

    public static class InitialConfiguration {
        public final Set<IpPrefix> directlyConnectedRoutes = new HashSet();
        public final Set<InetAddress> dnsServers = new HashSet();
        public Inet4Address gateway;
        public final Set<LinkAddress> ipAddresses = new HashSet();

        public static InitialConfiguration copy(InitialConfiguration config) {
            if (config == null) {
                return null;
            }
            InitialConfiguration configCopy = new InitialConfiguration();
            configCopy.ipAddresses.addAll(config.ipAddresses);
            configCopy.directlyConnectedRoutes.addAll(config.directlyConnectedRoutes);
            configCopy.dnsServers.addAll(config.dnsServers);
            return configCopy;
        }

        public String toString() {
            return String.format("InitialConfiguration(IPs: {%s}, prefixes: {%s}, DNS: {%s}, v4 gateway: %s)", new Object[]{IpClient.join(", ", this.ipAddresses), IpClient.join(", ", this.directlyConnectedRoutes), IpClient.join(", ", this.dnsServers), this.gateway});
        }

        public boolean isValid() {
            if (this.ipAddresses.isEmpty()) {
                return false;
            }
            for (LinkAddress addr : this.ipAddresses) {
                if (!IpClient.any(this.directlyConnectedRoutes, new -$$Lambda$IpClient$InitialConfiguration$UHB1S125oVRRJ33BRbWLXqGk32M(addr))) {
                    return false;
                }
            }
            for (InetAddress addr2 : this.dnsServers) {
                if (!IpClient.any(this.directlyConnectedRoutes, new -$$Lambda$IpClient$InitialConfiguration$Vb5KM9_T-c7Jv1Lqbet5CRNEyjU(addr2))) {
                    return false;
                }
            }
            if (IpClient.any(this.ipAddresses, IpClient.not(-$$Lambda$IpClient$InitialConfiguration$YwpJbnxCjWZ5CZ7ycLj8DIoOSd8.INSTANCE))) {
                return false;
            }
            if ((IpClient.any(this.directlyConnectedRoutes, -$$Lambda$IpClient$InitialConfiguration$gxc5co5uJUOrlWJ8HYqcngxR5gI.INSTANCE) && IpClient.all(this.ipAddresses, IpClient.not(-$$Lambda$IpClient$InitialConfiguration$WB134Aq_hrEPp-6UsNJgWvtMzBM.INSTANCE))) || IpClient.any(this.directlyConnectedRoutes, IpClient.not(-$$Lambda$IpClient$InitialConfiguration$-qxDAAo5wjq2G7x-F8gQeNSxIxY.INSTANCE))) {
                return false;
            }
            Stream stream = this.ipAddresses.stream();
            Class cls = Inet4Address.class;
            Objects.requireNonNull(cls);
            if (stream.filter(new -$$Lambda$IpClient$InitialConfiguration$guZQ276VyezHQuIEE0CG9osvUes(cls)).count() > 1) {
                return false;
            }
            return true;
        }

        public boolean isProvisionedBy(List<LinkAddress> addresses, List<RouteInfo> routes) {
            if (this.ipAddresses.isEmpty()) {
                return false;
            }
            for (LinkAddress addr : this.ipAddresses) {
                if (!IpClient.any(addresses, new -$$Lambda$IpClient$InitialConfiguration$Yj_oETEniyWPwMk9O1x-JZIJoNo(addr))) {
                    return false;
                }
            }
            if (routes != null) {
                for (IpPrefix prefix : this.directlyConnectedRoutes) {
                    if (!IpClient.any(routes, new -$$Lambda$IpClient$InitialConfiguration$Id7yPLmMAQz0Sm1dnrJVkXkUQNQ(prefix))) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static boolean isDirectlyConnectedRoute(RouteInfo route, IpPrefix prefix) {
            return !route.hasGateway() && prefix.equals(route.getDestination());
        }

        private static boolean isPrefixLengthCompliant(LinkAddress addr) {
            return addr.isIPv4() || isCompliantIPv6PrefixLength(addr.getPrefixLength());
        }

        private static boolean isPrefixLengthCompliant(IpPrefix prefix) {
            return prefix.isIPv4() || isCompliantIPv6PrefixLength(prefix.getPrefixLength());
        }

        private static boolean isCompliantIPv6PrefixLength(int prefixLength) {
            return 48 <= prefixLength && prefixLength <= 64;
        }

        private static boolean isIPv6DefaultRoute(IpPrefix prefix) {
            return prefix.getAddress().equals(Inet6Address.ANY);
        }

        private static boolean isIPv6GUA(LinkAddress addr) {
            return addr.isIPv6() && addr.isGlobalPreferred();
        }
    }

    private static class MessageHandlingLogger {
        public String processedInState;
        public String receivedInState;

        private MessageHandlingLogger() {
        }

        /* synthetic */ MessageHandlingLogger(AnonymousClass1 x0) {
            this();
        }

        public void reset() {
            this.processedInState = null;
            this.receivedInState = null;
        }

        public void handled(State processedIn, IState receivedIn) {
            this.processedInState = processedIn.getClass().getSimpleName();
            this.receivedInState = receivedIn.getName();
        }

        public String toString() {
            return String.format("rcvd_in=%s, proc_in=%s", new Object[]{this.receivedInState, this.processedInState});
        }
    }

    public static class ProvisioningConfiguration {
        private static final int DEFAULT_TIMEOUT_MS = 36000;
        ApfCapabilities mApfCapabilities;
        String mDisplayName = null;
        boolean mEnableIPv4 = true;
        boolean mEnableIPv6 = true;
        int mIPv6AddrGenMode = 2;
        InitialConfiguration mInitialConfig;
        Network mNetwork = null;
        int mProvisioningTimeoutMs = DEFAULT_TIMEOUT_MS;
        int mRequestedPreDhcpActionMs;
        StaticIpConfiguration mStaticIpConfig;
        boolean mUsingIpReachabilityMonitor = true;
        boolean mUsingMultinetworkPolicyTracker = true;

        public static class Builder {
            private ProvisioningConfiguration mConfig = new ProvisioningConfiguration();

            public Builder withoutIPv4() {
                this.mConfig.mEnableIPv4 = false;
                return this;
            }

            public Builder withoutIPv6() {
                this.mConfig.mEnableIPv6 = false;
                return this;
            }

            public Builder withoutMultinetworkPolicyTracker() {
                this.mConfig.mUsingMultinetworkPolicyTracker = false;
                return this;
            }

            public Builder withoutIpReachabilityMonitor() {
                this.mConfig.mUsingIpReachabilityMonitor = false;
                return this;
            }

            public Builder withPreDhcpAction() {
                this.mConfig.mRequestedPreDhcpActionMs = ProvisioningConfiguration.DEFAULT_TIMEOUT_MS;
                return this;
            }

            public Builder withPreDhcpAction(int dhcpActionTimeoutMs) {
                this.mConfig.mRequestedPreDhcpActionMs = dhcpActionTimeoutMs;
                return this;
            }

            public Builder withInitialConfiguration(InitialConfiguration initialConfig) {
                this.mConfig.mInitialConfig = initialConfig;
                return this;
            }

            public Builder withStaticConfiguration(StaticIpConfiguration staticConfig) {
                this.mConfig.mStaticIpConfig = staticConfig;
                return this;
            }

            public Builder withApfCapabilities(ApfCapabilities apfCapabilities) {
                this.mConfig.mApfCapabilities = apfCapabilities;
                return this;
            }

            public Builder withProvisioningTimeoutMs(int timeoutMs) {
                this.mConfig.mProvisioningTimeoutMs = timeoutMs;
                return this;
            }

            public Builder withRandomMacAddress() {
                this.mConfig.mIPv6AddrGenMode = 0;
                return this;
            }

            public Builder withStableMacAddress() {
                this.mConfig.mIPv6AddrGenMode = 2;
                return this;
            }

            public Builder withNetwork(Network network) {
                this.mConfig.mNetwork = network;
                return this;
            }

            public Builder withDisplayName(String displayName) {
                this.mConfig.mDisplayName = displayName;
                return this;
            }

            public ProvisioningConfiguration build() {
                return new ProvisioningConfiguration(this.mConfig);
            }
        }

        public ProvisioningConfiguration(ProvisioningConfiguration other) {
            this.mEnableIPv4 = other.mEnableIPv4;
            this.mEnableIPv6 = other.mEnableIPv6;
            this.mUsingIpReachabilityMonitor = other.mUsingIpReachabilityMonitor;
            this.mRequestedPreDhcpActionMs = other.mRequestedPreDhcpActionMs;
            this.mInitialConfig = InitialConfiguration.copy(other.mInitialConfig);
            this.mStaticIpConfig = other.mStaticIpConfig;
            this.mApfCapabilities = other.mApfCapabilities;
            this.mProvisioningTimeoutMs = other.mProvisioningTimeoutMs;
            this.mIPv6AddrGenMode = other.mIPv6AddrGenMode;
            this.mNetwork = other.mNetwork;
            this.mDisplayName = other.mDisplayName;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getClass().getSimpleName());
            stringBuilder.append("{");
            StringJoiner stringJoiner = new StringJoiner(", ", stringBuilder.toString(), "}");
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mEnableIPv4: ");
            stringBuilder2.append(this.mEnableIPv4);
            stringJoiner = stringJoiner.add(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mEnableIPv6: ");
            stringBuilder2.append(this.mEnableIPv6);
            stringJoiner = stringJoiner.add(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mUsingMultinetworkPolicyTracker: ");
            stringBuilder2.append(this.mUsingMultinetworkPolicyTracker);
            stringJoiner = stringJoiner.add(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mUsingIpReachabilityMonitor: ");
            stringBuilder2.append(this.mUsingIpReachabilityMonitor);
            stringJoiner = stringJoiner.add(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mRequestedPreDhcpActionMs: ");
            stringBuilder2.append(this.mRequestedPreDhcpActionMs);
            stringJoiner = stringJoiner.add(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mInitialConfig: ");
            stringBuilder2.append(this.mInitialConfig);
            stringJoiner = stringJoiner.add(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mStaticIpConfig: ");
            stringBuilder2.append(this.mStaticIpConfig);
            stringJoiner = stringJoiner.add(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mApfCapabilities: ");
            stringBuilder2.append(this.mApfCapabilities);
            stringJoiner = stringJoiner.add(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mProvisioningTimeoutMs: ");
            stringBuilder2.append(this.mProvisioningTimeoutMs);
            stringJoiner = stringJoiner.add(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mIPv6AddrGenMode: ");
            stringBuilder2.append(this.mIPv6AddrGenMode);
            stringJoiner = stringJoiner.add(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mNetwork: ");
            stringBuilder2.append(this.mNetwork);
            stringJoiner = stringJoiner.add(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mDisplayName: ");
            stringBuilder2.append(this.mDisplayName);
            return stringJoiner.add(stringBuilder2.toString()).toString();
        }

        public boolean isValid() {
            return this.mInitialConfig == null || this.mInitialConfig.isValid();
        }
    }

    class RunningState extends State {
        private boolean mDhcpActionInFlight;
        private ConnectivityPacketTracker mPacketTracker;

        RunningState() {
        }

        public void enter() {
            ApfConfiguration apfConfig = new ApfConfiguration();
            apfConfig.apfCapabilities = IpClient.this.mConfiguration.mApfCapabilities;
            apfConfig.multicastFilter = IpClient.this.mMulticastFiltering;
            apfConfig.ieee802_3Filter = IpClient.this.mContext.getResources().getBoolean(17956890);
            apfConfig.ethTypeBlackList = IpClient.this.mContext.getResources().getIntArray(17235980);
            IpClient.this.mApfFilter = ApfFilter.maybeCreate(IpClient.this.mContext, apfConfig, IpClient.this.mInterfaceParams, IpClient.this.mCallback);
            if (IpClient.this.mApfFilter == null) {
                IpClient.this.mCallback.setFallbackMulticastFilter(IpClient.this.mMulticastFiltering);
            }
            this.mPacketTracker = createPacketTracker();
            if (this.mPacketTracker != null) {
                this.mPacketTracker.start(IpClient.this.mConfiguration.mDisplayName);
            }
            if (IpClient.this.mConfiguration.mEnableIPv6 && !IpClient.this.startIPv6()) {
                IpClient.this.doImmediateProvisioningFailure(5);
                IpClient.this.transitionTo(IpClient.this.mStoppingState);
            } else if (!IpClient.this.mConfiguration.mEnableIPv4 || IpClient.this.startIPv4()) {
                InitialConfiguration config = IpClient.this.mConfiguration.mInitialConfig;
                if (config == null || IpClient.this.applyInitialConfig(config)) {
                    if (IpClient.this.mConfiguration.mUsingMultinetworkPolicyTracker) {
                        IpClient.this.mMultinetworkPolicyTracker = new MultinetworkPolicyTracker(IpClient.this.mContext, IpClient.this.getHandler(), new -$$Lambda$IpClient$RunningState$62CnAIrZ9p4JQ9DgmmpMjXifdaw(this));
                        IpClient.this.mMultinetworkPolicyTracker.start();
                    }
                    if (IpClient.this.mConfiguration.mUsingIpReachabilityMonitor && !IpClient.this.startIpReachabilityMonitor()) {
                        IpClient.this.doImmediateProvisioningFailure(6);
                        IpClient.this.transitionTo(IpClient.this.mStoppingState);
                        return;
                    }
                    return;
                }
                IpClient.this.doImmediateProvisioningFailure(7);
                IpClient.this.transitionTo(IpClient.this.mStoppingState);
            } else {
                IpClient.this.doImmediateProvisioningFailure(4);
                IpClient.this.transitionTo(IpClient.this.mStoppingState);
            }
        }

        public void exit() {
            stopDhcpAction();
            if (IpClient.this.mIpReachabilityMonitor != null) {
                IpClient.this.mIpReachabilityMonitor.stop();
                IpClient.this.mIpReachabilityMonitor = null;
            }
            if (IpClient.this.mMultinetworkPolicyTracker != null) {
                IpClient.this.mMultinetworkPolicyTracker.shutdown();
                IpClient.this.mMultinetworkPolicyTracker = null;
            }
            if (IpClient.this.mDhcpClient != null) {
                IpClient.this.mDhcpClient.sendMessage(DhcpClient.CMD_STOP_DHCP);
                IpClient.this.mDhcpClient.doQuit();
            }
            if (this.mPacketTracker != null) {
                this.mPacketTracker.stop();
                this.mPacketTracker = null;
            }
            if (IpClient.this.mApfFilter != null) {
                IpClient.this.mApfFilter.shutdown();
                IpClient.this.mApfFilter = null;
            }
            IpClient.this.resetLinkProperties();
        }

        private ConnectivityPacketTracker createPacketTracker() {
            try {
                return new ConnectivityPacketTracker(IpClient.this.getHandler(), IpClient.this.mInterfaceParams, IpClient.this.mConnectivityPacketLog);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private void ensureDhcpAction() {
            if (!this.mDhcpActionInFlight) {
                IpClient.this.mCallback.onPreDhcpAction();
                this.mDhcpActionInFlight = true;
                IpClient.this.mDhcpActionTimeoutAlarm.schedule(SystemClock.elapsedRealtime() + ((long) IpClient.this.mConfiguration.mRequestedPreDhcpActionMs));
            }
        }

        private void stopDhcpAction() {
            IpClient.this.mDhcpActionTimeoutAlarm.cancel();
            if (this.mDhcpActionInFlight) {
                IpClient.this.mCallback.onPostDhcpAction();
                this.mDhcpActionInFlight = false;
            }
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i != 20) {
                if (i != DhcpClient.CMD_TRY_CACHED_IP) {
                    switch (i) {
                        case 2:
                            Log.d(IpClient.this.mTag, "StartedState: CMD_STOP");
                            IpClient.this.transitionTo(IpClient.this.mStoppingState);
                            break;
                        case 3:
                            IpClient.this.logError("ALERT: START received in StartedState. Please fix caller.", new Object[0]);
                            break;
                        case 4:
                            if (IpClient.this.mIpReachabilityMonitor != null) {
                                IpClient.this.mIpReachabilityMonitor.probeAll();
                                break;
                            }
                            break;
                        case 5:
                            if (IpClient.this.mDhcpClient != null) {
                                IpClient.this.mDhcpClient.sendMessage(DhcpClient.CMD_PRE_DHCP_ACTION_COMPLETE);
                                break;
                            }
                            break;
                        case 6:
                            Log.d(IpClient.this.mTag, "StartedState: EVENT_NETLINK_LINKPROPERTIES_CHANGED");
                            if (!IpClient.this.handleLinkPropertiesUpdate(true)) {
                                Log.w(IpClient.this.mTag, "we have lost provisioning, transitions to StoppingState");
                                IpClient.this.transitionTo(IpClient.this.mStoppingState);
                                break;
                            }
                            break;
                        case 7:
                            IpClient.this.mTcpBufferSizes = (String) msg.obj;
                            IpClient.this.handleLinkPropertiesUpdate(true);
                            break;
                        case 8:
                            IpClient.this.mHttpProxy = (ProxyInfo) msg.obj;
                            IpClient.this.handleLinkPropertiesUpdate(true);
                            break;
                        case 9:
                            break;
                        default:
                            switch (i) {
                                case 11:
                                    Log.w(IpClient.this.mTag, "StartedState: EVENT_DHCPACTION_TIMEOUT");
                                    stopDhcpAction();
                                    break;
                                case 12:
                                    if (IpClient.this.mApfFilter != null) {
                                        IpClient.this.mApfFilter.setDataSnapshot((byte[]) msg.obj);
                                    }
                                    IpClient.this.mApfDataSnapshotComplete.open();
                                    break;
                                default:
                                    switch (i) {
                                        case DhcpClient.CMD_PRE_DHCP_ACTION /*196611*/:
                                            if (IpClient.this.mConfiguration.mRequestedPreDhcpActionMs <= 0) {
                                                IpClient.this.sendMessage(5);
                                                break;
                                            }
                                            ensureDhcpAction();
                                            break;
                                        case DhcpClient.CMD_POST_DHCP_ACTION /*196612*/:
                                            stopDhcpAction();
                                            switch (msg.arg1) {
                                                case 1:
                                                    IpClient.this.mDhcp_Flag = msg.arg2;
                                                    IpClient.this.handleIPv4Success((DhcpResults) msg.obj);
                                                    break;
                                                case 2:
                                                    IpClient.this.mDhcp_Flag = msg.arg2;
                                                    IpClient.this.handleIPv4Failure();
                                                    break;
                                                default:
                                                    IpClient.this.logError("Unknown CMD_POST_DHCP_ACTION status: %s", Integer.valueOf(msg.arg1));
                                                    break;
                                            }
                                        case DhcpClient.CMD_ON_QUIT /*196613*/:
                                            IpClient.this.logError("Unexpected CMD_ON_QUIT.", new Object[0]);
                                            IpClient.this.mDhcpClient = null;
                                            break;
                                        default:
                                            switch (i) {
                                                case DhcpClient.CMD_CLEAR_LINKADDRESS /*196615*/:
                                                    IpClient.this.mInterfaceCtrl.clearIPv4Address();
                                                    break;
                                                case DhcpClient.CMD_CONFIGURE_LINKADDRESS /*196616*/:
                                                    if (!IpClient.this.mInterfaceCtrl.setIPv4Address(msg.obj)) {
                                                        IpClient.this.logError("Failed to set IPv4 address.", new Object[0]);
                                                        IpClient.this.dispatchCallback(ProvisioningChange.LOST_PROVISIONING, new LinkProperties(IpClient.this.mLinkProperties));
                                                        IpClient.this.transitionTo(IpClient.this.mStoppingState);
                                                        break;
                                                    }
                                                    IpClient.this.mDhcpClient.sendMessage(DhcpClient.EVENT_LINKADDRESS_CONFIGURED);
                                                    break;
                                                default:
                                                    return false;
                                            }
                                    }
                            }
                    }
                }
                StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
                staticIpConfiguration.domains = "CMD_TRY_CACHED_IP";
                IpClient.this.mCallback.onNewDhcpResults(new DhcpResults(staticIpConfiguration));
                IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
                return true;
            }
            IpClient.this.mMulticastFiltering = ((Boolean) msg.obj).booleanValue();
            String access$400 = IpClient.this.mTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("set Multicast Filtering:");
            stringBuilder.append(IpClient.this.mMulticastFiltering);
            Log.d(access$400, stringBuilder.toString());
            if (IpClient.this.mApfFilter == null) {
                Log.w(IpClient.this.mTag, "set Multicast Filtering, mApfFilter is null");
                IpClient.this.mCallback.setFallbackMulticastFilter(IpClient.this.mMulticastFiltering);
            } else if (msg.what == 20) {
                IpClient.this.mApfFilter.setScreenOffMulticastFilter(IpClient.this.mMulticastFiltering);
            } else {
                IpClient.this.mApfFilter.setMulticastFilter(IpClient.this.mMulticastFiltering);
            }
            IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
            return true;
        }
    }

    class StartedState extends State {
        StartedState() {
        }

        public void enter() {
            IpClient.this.mStartTimeMillis = SystemClock.elapsedRealtime();
            if (IpClient.this.mConfiguration.mProvisioningTimeoutMs > 0) {
                IpClient.this.mProvisioningTimeoutAlarm.schedule(SystemClock.elapsedRealtime() + ((long) IpClient.this.mConfiguration.mProvisioningTimeoutMs));
            }
            if (readyToProceed()) {
                IpClient.this.transitionTo(IpClient.this.mRunningState);
            } else {
                IpClient.this.stopAllIP();
            }
        }

        public void exit() {
            IpClient.this.mProvisioningTimeoutAlarm.cancel();
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i == 2) {
                IpClient.this.transitionTo(IpClient.this.mStoppingState);
            } else if (i == 6) {
                IpClient.this.handleLinkPropertiesUpdate(false);
                if (readyToProceed()) {
                    IpClient.this.transitionTo(IpClient.this.mRunningState);
                }
            } else if (i != 10) {
                IpClient.this.deferMessage(msg);
            } else {
                IpClient.this.handleProvisioningFailure();
            }
            IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
            return true;
        }

        boolean readyToProceed() {
            return (IpClient.this.mLinkProperties.hasIPv4Address() || IpClient.this.mLinkProperties.hasGlobalIPv6Address()) ? false : true;
        }
    }

    class StoppedState extends State {
        StoppedState() {
        }

        public void enter() {
            IpClient.this.stopAllIP();
            IpClient.this.resetLinkProperties();
            if (IpClient.this.mStartTimeMillis > 0) {
                IpClient.this.recordMetric(3);
                IpClient.this.mStartTimeMillis = 0;
            }
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i != DhcpClient.CMD_ON_QUIT) {
                switch (i) {
                    case 1:
                        IpClient.this.stopStateMachineUpdaters();
                        IpClient.this.quit();
                        break;
                    case 2:
                        break;
                    case 3:
                        IpClient.this.mConfiguration = (ProvisioningConfiguration) msg.obj;
                        IpClient.this.transitionTo(IpClient.this.mStartedState);
                        break;
                    default:
                        switch (i) {
                            case 6:
                                IpClient.this.handleLinkPropertiesUpdate(false);
                                break;
                            case 7:
                                IpClient.this.mTcpBufferSizes = (String) msg.obj;
                                IpClient.this.handleLinkPropertiesUpdate(false);
                                break;
                            case 8:
                                IpClient.this.mHttpProxy = (ProxyInfo) msg.obj;
                                IpClient.this.handleLinkPropertiesUpdate(false);
                                break;
                            case 9:
                                IpClient.this.mMulticastFiltering = ((Boolean) msg.obj).booleanValue();
                                break;
                            default:
                                return false;
                        }
                }
            }
            IpClient.this.logError("Unexpected CMD_ON_QUIT (already stopped).", new Object[0]);
            IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
            return true;
        }
    }

    class StoppingState extends State {
        StoppingState() {
        }

        public void enter() {
            if (IpClient.this.mDhcpClient == null) {
                IpClient.this.transitionTo(IpClient.this.mStoppedState);
            }
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i != 2) {
                if (i == DhcpClient.CMD_ON_QUIT) {
                    IpClient.this.mDhcpClient = null;
                    IpClient.this.transitionTo(IpClient.this.mStoppedState);
                } else if (i != DhcpClient.CMD_CLEAR_LINKADDRESS) {
                    IpClient.this.deferMessage(msg);
                } else {
                    IpClient.this.mInterfaceCtrl.clearIPv4Address();
                }
            }
            IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
            return true;
        }
    }

    private class LoggingCallbackWrapper extends Callback {
        private static final String PREFIX = "INVOKE ";
        private Callback mCallback;

        public LoggingCallbackWrapper(Callback callback) {
            this.mCallback = callback;
        }

        private void log(String msg) {
            SharedLog access$000 = IpClient.this.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append(msg);
            access$000.log(stringBuilder.toString());
        }

        public void onPreDhcpAction() {
            this.mCallback.onPreDhcpAction();
            log("onPreDhcpAction()");
        }

        public void onPostDhcpAction() {
            this.mCallback.onPostDhcpAction();
            log("onPostDhcpAction()");
        }

        public void onNewDhcpResults(DhcpResults dhcpResults) {
            this.mCallback.onNewDhcpResults(dhcpResults);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onNewDhcpResults({");
            stringBuilder.append(dhcpResults);
            stringBuilder.append("})");
            log(stringBuilder.toString());
        }

        public void onProvisioningSuccess(LinkProperties newLp) {
            this.mCallback.onProvisioningSuccess(newLp);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onProvisioningSuccess({");
            stringBuilder.append(newLp);
            stringBuilder.append("})");
            log(stringBuilder.toString());
        }

        public void onProvisioningFailure(LinkProperties newLp) {
            this.mCallback.onProvisioningFailure(newLp);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onProvisioningFailure({");
            stringBuilder.append(newLp);
            stringBuilder.append("})");
            log(stringBuilder.toString());
        }

        public void onLinkPropertiesChange(LinkProperties newLp) {
            this.mCallback.onLinkPropertiesChange(newLp);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onLinkPropertiesChange({");
            stringBuilder.append(newLp);
            stringBuilder.append("})");
            log(stringBuilder.toString());
        }

        public void onReachabilityLost(String logMsg) {
            this.mCallback.onReachabilityLost(logMsg);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onReachabilityLost(");
            stringBuilder.append(logMsg);
            stringBuilder.append(")");
            log(stringBuilder.toString());
        }

        public void onQuit() {
            this.mCallback.onQuit();
            log("onQuit()");
        }

        public void installPacketFilter(byte[] filter) {
            this.mCallback.installPacketFilter(filter);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("installPacketFilter(byte[");
            stringBuilder.append(filter.length);
            stringBuilder.append("])");
            log(stringBuilder.toString());
        }

        public void startReadPacketFilter() {
            this.mCallback.startReadPacketFilter();
            log("startReadPacketFilter()");
        }

        public void setFallbackMulticastFilter(boolean enabled) {
            this.mCallback.setFallbackMulticastFilter(enabled);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setFallbackMulticastFilter(");
            stringBuilder.append(enabled);
            stringBuilder.append(")");
            log(stringBuilder.toString());
        }

        public void setNeighborDiscoveryOffload(boolean enable) {
            this.mCallback.setNeighborDiscoveryOffload(enable);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setNeighborDiscoveryOffload(");
            stringBuilder.append(enable);
            stringBuilder.append(")");
            log(stringBuilder.toString());
        }
    }

    public static class WaitForProvisioningCallback extends Callback {
        private final ConditionVariable mCV = new ConditionVariable();
        private LinkProperties mCallbackLinkProperties;

        public LinkProperties waitForProvisioning() {
            this.mCV.block();
            return this.mCallbackLinkProperties;
        }

        public void onProvisioningSuccess(LinkProperties newLp) {
            this.mCallbackLinkProperties = newLp;
            this.mCV.open();
        }

        public void onProvisioningFailure(LinkProperties newLp) {
            this.mCallbackLinkProperties = null;
            this.mCV.open();
        }
    }

    public static void dumpAllLogs(PrintWriter writer, String[] args) {
        for (String ifname : sSmLogs.keySet()) {
            if (ArrayUtils.isEmpty(args) || ArrayUtils.contains(args, ifname)) {
                writer.println(String.format("--- BEGIN %s ---", new Object[]{ifname}));
                SharedLog smLog = (SharedLog) sSmLogs.get(ifname);
                if (smLog != null) {
                    writer.println("State machine log:");
                    smLog.dump(null, writer, null);
                }
                writer.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                LocalLog pktLog = (LocalLog) sPktLogs.get(ifname);
                if (pktLog != null) {
                    writer.println("Connectivity packet log:");
                    pktLog.readOnlyLocalLog().dump(null, writer, null);
                }
                writer.println(String.format("--- END %s ---", new Object[]{ifname}));
            }
        }
    }

    public IpClient(Context context, String ifName, Callback callback) {
        this(context, ifName, callback, new Dependencies());
    }

    public IpClient(Context context, String ifName, Callback callback, final INetworkManagementService nwService) {
        this(context, ifName, callback, new Dependencies() {
            public INetworkManagementService getNMS() {
                return nwService;
            }
        });
    }

    @VisibleForTesting
    IpClient(Context context, String ifName, Callback callback, Dependencies deps) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(IpClient.class.getSimpleName());
        stringBuilder.append(".");
        stringBuilder.append(ifName);
        super(stringBuilder.toString());
        this.forceDhcpDiscovery = false;
        this.mStoppedState = new StoppedState();
        this.mStoppingState = new StoppingState();
        this.mStartedState = new StartedState();
        this.mRunningState = new RunningState();
        this.mMetricsLog = new IpConnectivityLog();
        this.mPendingSSID = null;
        this.mDhcp_Flag = 0;
        this.mApfDataSnapshotComplete = new ConditionVariable();
        Preconditions.checkNotNull(ifName);
        Preconditions.checkNotNull(callback);
        this.mTag = getName();
        this.mContext = context;
        this.mInterfaceName = ifName;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(CLAT_PREFIX);
        stringBuilder2.append(ifName);
        this.mClatInterfaceName = stringBuilder2.toString();
        this.mCallback = new LoggingCallbackWrapper(callback);
        this.mDependencies = deps;
        this.mShutdownLatch = new CountDownLatch(1);
        this.mNwService = deps.getNMS();
        this.forceDhcpDiscovery = false;
        sSmLogs.putIfAbsent(this.mInterfaceName, new SharedLog(500, this.mTag));
        this.mLog = (SharedLog) sSmLogs.get(this.mInterfaceName);
        sPktLogs.putIfAbsent(this.mInterfaceName, new LocalLog(100));
        this.mConnectivityPacketLog = (LocalLog) sPktLogs.get(this.mInterfaceName);
        this.mMsgStateLogger = new MessageHandlingLogger();
        this.mInterfaceCtrl = new InterfaceController(this.mInterfaceName, this.mNwService, deps.getNetd(), this.mLog);
        this.mNetlinkTracker = new NetlinkTracker(this.mInterfaceName, new com.android.server.net.NetlinkTracker.Callback() {
            public void update() {
                IpClient.this.sendMessage(6);
            }
        }) {
            public void interfaceAdded(String iface) {
                super.interfaceAdded(iface);
                if (IpClient.this.mClatInterfaceName.equals(iface)) {
                    IpClient.this.mCallback.setNeighborDiscoveryOffload(false);
                } else if (!IpClient.this.mInterfaceName.equals(iface)) {
                    return;
                }
                String msg = new StringBuilder();
                msg.append("interfaceAdded(");
                msg.append(iface);
                msg.append(")");
                logMsg(msg.toString());
            }

            public void interfaceRemoved(String iface) {
                super.interfaceRemoved(iface);
                if (IpClient.this.mClatInterfaceName.equals(iface)) {
                    IpClient.this.mCallback.setNeighborDiscoveryOffload(true);
                } else if (!IpClient.this.mInterfaceName.equals(iface)) {
                    return;
                }
                String msg = new StringBuilder();
                msg.append("interfaceRemoved(");
                msg.append(iface);
                msg.append(")");
                logMsg(msg.toString());
            }

            private void logMsg(String msg) {
                Log.d(IpClient.this.mTag, msg);
                IpClient.this.getHandler().post(new -$$Lambda$IpClient$3$97DqfCdvF_DK1fYMrSYfnetT_aU(this, msg));
            }

            public static /* synthetic */ void lambda$logMsg$0(AnonymousClass3 anonymousClass3, String msg) {
                SharedLog access$000 = IpClient.this.mLog;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("OBSERVED ");
                stringBuilder.append(msg);
                access$000.log(stringBuilder.toString());
            }
        };
        this.mLinkProperties = new LinkProperties();
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
        Context context2 = this.mContext;
        Handler handler = getHandler();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(this.mTag);
        stringBuilder3.append(".EVENT_PROVISIONING_TIMEOUT");
        this.mProvisioningTimeoutAlarm = new WakeupMessage(context2, handler, stringBuilder3.toString(), 10);
        context2 = this.mContext;
        handler = getHandler();
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append(this.mTag);
        stringBuilder3.append(".EVENT_DHCPACTION_TIMEOUT");
        this.mDhcpActionTimeoutAlarm = new WakeupMessage(context2, handler, stringBuilder3.toString(), 11);
        configureAndStartStateMachine();
        startStateMachineUpdaters();
    }

    private void configureAndStartStateMachine() {
        addState(this.mStoppedState);
        addState(this.mStartedState);
        addState(this.mRunningState, this.mStartedState);
        addState(this.mStoppingState);
        setInitialState(this.mStoppedState);
        super.start();
        try {
            this.mNwService.registerObserver(this.mNetlinkTracker);
        } catch (RemoteException e) {
            String str = this.mTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't register NetlinkTracker: ");
            stringBuilder.append(e.toString());
            Log.e(str, stringBuilder.toString());
        }
        resetLinkProperties();
    }

    private void startStateMachineUpdaters() {
        try {
            this.mNwService.registerObserver(this.mNetlinkTracker);
        } catch (RemoteException e) {
            logError("Couldn't register NetlinkTracker: %s", e);
        }
    }

    private void stopStateMachineUpdaters() {
        try {
            this.mNwService.unregisterObserver(this.mNetlinkTracker);
        } catch (RemoteException e) {
            logError("Couldn't unregister NetlinkTracker: %s", e);
        }
    }

    protected void onQuitting() {
        this.mCallback.onQuit();
        this.mShutdownLatch.countDown();
    }

    public void shutdown() {
        stop();
        sendMessage(1);
    }

    public void awaitShutdown() {
        try {
            this.mShutdownLatch.await();
        } catch (InterruptedException e) {
            SharedLog sharedLog = this.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Interrupted while awaiting shutdown: ");
            stringBuilder.append(e);
            sharedLog.e(stringBuilder.toString());
        }
    }

    public static Builder buildProvisioningConfiguration() {
        return new Builder();
    }

    public void startProvisioning(ProvisioningConfiguration req) {
        if (req.isValid()) {
            this.mInterfaceParams = this.mDependencies.getInterfaceParams(this.mInterfaceName);
            if (this.mInterfaceParams == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to find InterfaceParams for ");
                stringBuilder.append(this.mInterfaceName);
                logError(stringBuilder.toString(), new Object[0]);
                doImmediateProvisioningFailure(8);
                return;
            }
            this.mCallback.setNeighborDiscoveryOffload(true);
            sendMessage(3, new ProvisioningConfiguration(req));
            return;
        }
        doImmediateProvisioningFailure(7);
    }

    public void startProvisioning(StaticIpConfiguration staticIpConfig) {
        startProvisioning(buildProvisioningConfiguration().withStaticConfiguration(staticIpConfig).build());
    }

    public void startProvisioning() {
        startProvisioning(new ProvisioningConfiguration());
    }

    public void stop() {
        this.forceDhcpDiscovery = false;
        sendMessage(2);
    }

    public void putPendingSSID(String pendingSSID) {
        this.mPendingSSID = pendingSSID;
    }

    public void confirmConfiguration() {
        sendMessage(4);
    }

    public void completedPreDhcpAction() {
        sendMessage(5);
    }

    public void readPacketFilterComplete(byte[] data) {
        sendMessage(12, data);
    }

    public void setTcpBufferSizes(String tcpBufferSizes) {
        sendMessage(7, tcpBufferSizes);
    }

    public void setHttpProxy(ProxyInfo proxyInfo) {
        sendMessage(8, proxyInfo);
    }

    public void setMulticastFilter(boolean enabled) {
        sendMessage(9, Boolean.valueOf(enabled));
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        if (args == null || args.length <= 0 || !DUMP_ARG_CONFIRM.equals(args[0])) {
            StringBuilder stringBuilder;
            ApfFilter apfFilter = this.mApfFilter;
            ProvisioningConfiguration provisioningConfig = this.mConfiguration;
            ApfCapabilities apfCapabilities = provisioningConfig != null ? provisioningConfig.mApfCapabilities : null;
            IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.mTag);
            stringBuilder2.append(" APF dump:");
            pw.println(stringBuilder2.toString());
            pw.increaseIndent();
            if (apfFilter != null) {
                if (apfCapabilities.hasDataAccess()) {
                    this.mApfDataSnapshotComplete.close();
                    this.mCallback.startReadPacketFilter();
                    if (!this.mApfDataSnapshotComplete.block(1000)) {
                        pw.print("TIMEOUT: DUMPING STALE APF SNAPSHOT");
                    }
                }
                apfFilter.dump(pw);
            } else {
                pw.print("No active ApfFilter; ");
                if (provisioningConfig == null) {
                    pw.println("IpClient not yet started.");
                } else if (apfCapabilities == null || apfCapabilities.apfVersionSupported == 0) {
                    pw.println("Hardware does not support APF.");
                } else {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("ApfFilter not yet started, APF capabilities: ");
                    stringBuilder2.append(apfCapabilities);
                    pw.println(stringBuilder2.toString());
                }
            }
            pw.decreaseIndent();
            pw.println();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.mTag);
            stringBuilder2.append(" current ProvisioningConfiguration:");
            pw.println(stringBuilder2.toString());
            pw.increaseIndent();
            pw.println(Objects.toString(provisioningConfig, "N/A"));
            pw.decreaseIndent();
            IpReachabilityMonitor iprm = this.mIpReachabilityMonitor;
            if (iprm != null) {
                pw.println();
                stringBuilder = new StringBuilder();
                stringBuilder.append(this.mTag);
                stringBuilder.append(" current IpReachabilityMonitor state:");
                pw.println(stringBuilder.toString());
                pw.increaseIndent();
                iprm.dump(pw);
                pw.decreaseIndent();
            }
            pw.println();
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mTag);
            stringBuilder.append(" StateMachine dump:");
            pw.println(stringBuilder.toString());
            pw.increaseIndent();
            this.mLog.dump(fd, pw, args);
            pw.decreaseIndent();
            pw.println();
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mTag);
            stringBuilder.append(" connectivity packet log:");
            pw.println(stringBuilder.toString());
            pw.println();
            pw.println("Debug with python and scapy via:");
            pw.println("shell$ python");
            pw.println(">>> from scapy import all as scapy");
            pw.println(">>> scapy.Ether(\"<paste_hex_string>\".decode(\"hex\")).show2()");
            pw.println();
            pw.increaseIndent();
            this.mConnectivityPacketLog.readOnlyLocalLog().dump(fd, pw, args);
            pw.decreaseIndent();
            return;
        }
        confirmConfiguration();
    }

    protected String getWhatToString(int what) {
        SparseArray sparseArray = sWhatToString;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("UNKNOWN: ");
        stringBuilder.append(Integer.toString(what));
        return (String) sparseArray.get(what, stringBuilder.toString());
    }

    protected String getLogRecString(Message msg) {
        String logLine = "%s/%d %d %d %s [%s]";
        Object[] objArr = new Object[6];
        objArr[0] = this.mInterfaceName;
        objArr[1] = Integer.valueOf(this.mInterfaceParams == null ? -1 : this.mInterfaceParams.index);
        objArr[2] = Integer.valueOf(msg.arg1);
        objArr[3] = Integer.valueOf(msg.arg2);
        objArr[4] = Objects.toString(msg.obj);
        objArr[5] = this.mMsgStateLogger;
        logLine = String.format(logLine, objArr);
        if (msg.what == DhcpClient.CMD_CONFIGURE_LINKADDRESS) {
            String richerLogLine = new StringBuilder();
            richerLogLine.append(getWhatToString(msg.what));
            richerLogLine.append(" ");
            richerLogLine.append(this.mInterfaceName);
            this.mLog.log(richerLogLine.toString());
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getWhatToString(msg.what));
            stringBuilder.append(" ");
            stringBuilder.append(logLine);
            this.mLog.log(stringBuilder.toString());
        }
        this.mMsgStateLogger.reset();
        return logLine;
    }

    protected boolean recordLogRec(Message msg) {
        boolean shouldLog = msg.what != 6;
        if (!shouldLog) {
            this.mMsgStateLogger.reset();
        }
        return shouldLog;
    }

    private void logError(String fmt, Object... args) {
        String msg = new StringBuilder();
        msg.append("ERROR ");
        msg.append(String.format(fmt, args));
        msg = msg.toString();
        Log.e(this.mTag, msg);
        this.mLog.log(msg);
    }

    private void resetLinkProperties() {
        this.mNetlinkTracker.clearLinkProperties();
        this.mConfiguration = null;
        this.mDhcpResults = null;
        this.mTcpBufferSizes = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        this.mHttpProxy = null;
        this.mLinkProperties = new LinkProperties();
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
    }

    private void recordMetric(int type) {
        long j = 0;
        if (this.mStartTimeMillis > 0) {
            j = SystemClock.elapsedRealtime() - this.mStartTimeMillis;
        }
        this.mMetricsLog.log(this.mInterfaceName, new IpManagerEvent(type, j));
    }

    @VisibleForTesting
    static boolean isProvisioned(LinkProperties lp, InitialConfiguration config) {
        if (lp.hasIPv4Address() || lp.isProvisioned()) {
            return true;
        }
        if (config == null) {
            return false;
        }
        return config.isProvisionedBy(lp.getLinkAddresses(), lp.getRoutes());
    }

    private ProvisioningChange compareProvisioning(LinkProperties oldLp, LinkProperties newLp) {
        ProvisioningChange delta;
        InitialConfiguration config = this.mConfiguration != null ? this.mConfiguration.mInitialConfig : null;
        boolean wasProvisioned = isProvisioned(oldLp, config);
        boolean isProvisioned = isProvisioned(newLp, config);
        if (!wasProvisioned && isProvisioned) {
            delta = ProvisioningChange.GAINED_PROVISIONING;
        } else if (wasProvisioned && isProvisioned) {
            delta = ProvisioningChange.STILL_PROVISIONED;
        } else if (wasProvisioned || isProvisioned) {
            delta = ProvisioningChange.LOST_PROVISIONING;
            Log.d("IpClient", "compareProvisioning: LOST_PROVISIONING because 1");
        } else {
            delta = ProvisioningChange.STILL_NOT_PROVISIONED;
        }
        boolean ignoreIPv6ProvisioningLoss = false;
        boolean lostIPv6 = oldLp.isIPv6Provisioned() && !newLp.isIPv6Provisioned();
        boolean lostIPv4Address = oldLp.hasIPv4Address() && !newLp.hasIPv4Address();
        boolean lostIPv6Router = oldLp.hasIPv6DefaultRoute() && !newLp.hasIPv6DefaultRoute();
        if (!(this.mMultinetworkPolicyTracker == null || this.mMultinetworkPolicyTracker.getAvoidBadWifi())) {
            ignoreIPv6ProvisioningLoss = true;
        }
        if (lostIPv4Address || (lostIPv6 && !ignoreIPv6ProvisioningLoss)) {
            delta = ProvisioningChange.LOST_PROVISIONING;
            Log.d("IpClient", "compareProvisioning: LOST_PROVISIONING because 2");
        }
        if (oldLp.hasGlobalIPv6Address() && lostIPv6Router && !ignoreIPv6ProvisioningLoss) {
            delta = ProvisioningChange.LOST_PROVISIONING;
            Log.d("IpClient", "compareProvisioning: LOST_PROVISIONING because 3");
        }
        if (delta != ProvisioningChange.LOST_PROVISIONING || !oldLp.hasIPv4Address() || !newLp.hasIPv4Address()) {
            return delta;
        }
        delta = ProvisioningChange.STILL_PROVISIONED;
        Log.d("IpClient", "compareProvisioning: network has IPv4 address, dont loss of provisioning");
        return delta;
    }

    private void dispatchCallback(ProvisioningChange delta, LinkProperties newLp) {
        switch (AnonymousClass5.$SwitchMap$android$net$LinkProperties$ProvisioningChange[delta.ordinal()]) {
            case 1:
                recordMetric(1);
                this.mCallback.onProvisioningSuccess(newLp);
                return;
            case 2:
                recordMetric(2);
                this.mCallback.onProvisioningFailure(newLp);
                return;
            default:
                this.mCallback.onLinkPropertiesChange(newLp);
                return;
        }
    }

    private ProvisioningChange setLinkProperties(LinkProperties newLp) {
        if (this.mApfFilter != null) {
            this.mApfFilter.setLinkProperties(newLp);
        }
        if (this.mIpReachabilityMonitor != null) {
            this.mIpReachabilityMonitor.updateLinkProperties(newLp);
        }
        ProvisioningChange delta = compareProvisioning(this.mLinkProperties, newLp);
        this.mLinkProperties = new LinkProperties(newLp);
        if (delta == ProvisioningChange.GAINED_PROVISIONING) {
            this.mProvisioningTimeoutAlarm.cancel();
        }
        return delta;
    }

    private LinkProperties assembleLinkProperties() {
        LinkProperties newLp = new LinkProperties();
        newLp.setInterfaceName(this.mInterfaceName);
        LinkProperties netlinkLinkProperties = this.mNetlinkTracker.getLinkProperties();
        newLp.setLinkAddresses(netlinkLinkProperties.getLinkAddresses());
        for (RouteInfo route : netlinkLinkProperties.getRoutes()) {
            newLp.addRoute(route);
        }
        addAllReachableDnsServers(newLp, netlinkLinkProperties.getDnsServers());
        if (this.mDhcpResults != null) {
            for (RouteInfo route2 : this.mDhcpResults.getRoutes(this.mInterfaceName)) {
                newLp.addRoute(route2);
            }
            addAllReachableDnsServers(newLp, this.mDhcpResults.dnsServers);
            newLp.setDomains(this.mDhcpResults.domains);
            if (this.mDhcpResults.mtu != 0) {
                newLp.setMtu(this.mDhcpResults.mtu);
            }
        }
        if (!TextUtils.isEmpty(this.mTcpBufferSizes)) {
            newLp.setTcpBufferSizes(this.mTcpBufferSizes);
        }
        if (this.mHttpProxy != null) {
            newLp.setHttpProxy(this.mHttpProxy);
        }
        if (!(this.mConfiguration == null || this.mConfiguration.mInitialConfig == null)) {
            InitialConfiguration config = this.mConfiguration.mInitialConfig;
            if (config.isProvisionedBy(newLp.getLinkAddresses(), null)) {
                for (IpPrefix prefix : config.directlyConnectedRoutes) {
                    newLp.addRoute(new RouteInfo(prefix, null, this.mInterfaceName));
                }
            }
            addAllReachableDnsServers(newLp, config.dnsServers);
        }
        LinkProperties oldLp = this.mLinkProperties;
        return newLp;
    }

    private static void addAllReachableDnsServers(LinkProperties lp, Iterable<InetAddress> dnses) {
        for (InetAddress dns : dnses) {
            if (!dns.isAnyLocalAddress() && lp.isReachable(dns)) {
                lp.addDnsServer(dns);
            }
        }
    }

    private boolean handleLinkPropertiesUpdate(boolean sendCallbacks) {
        LinkProperties newLp = assembleLinkProperties();
        boolean z = true;
        if (Objects.equals(newLp, this.mLinkProperties)) {
            return true;
        }
        ProvisioningChange delta = setLinkProperties(newLp);
        if (sendCallbacks) {
            dispatchCallback(delta, newLp);
        }
        if (delta == ProvisioningChange.LOST_PROVISIONING) {
            z = false;
        }
        return z;
    }

    private void handleIPv4Success(DhcpResults dhcpResults) {
        this.mDhcpResults = new DhcpResults(dhcpResults);
        LinkProperties newLp = assembleLinkProperties();
        ProvisioningChange delta = setLinkProperties(newLp);
        this.mCallback.onNewDhcpResults(dhcpResults);
        dispatchCallback(delta, newLp);
    }

    private void handleIPv4Failure() {
        this.mInterfaceCtrl.clearIPv4Address();
        this.mDhcpResults = null;
        this.mCallback.onNewDhcpResults(null);
        handleProvisioningFailure();
    }

    private void handleProvisioningFailure() {
        LinkProperties newLp = assembleLinkProperties();
        ProvisioningChange delta = setLinkProperties(newLp);
        if (delta == ProvisioningChange.STILL_NOT_PROVISIONED) {
            delta = ProvisioningChange.LOST_PROVISIONING;
        }
        dispatchCallback(delta, newLp);
        if (delta == ProvisioningChange.LOST_PROVISIONING) {
            transitionTo(this.mStoppingState);
        }
    }

    private void doImmediateProvisioningFailure(int failureType) {
        logError("onProvisioningFailure(): %s", Integer.valueOf(failureType));
        recordMetric(failureType);
        this.mCallback.onProvisioningFailure(new LinkProperties(this.mLinkProperties));
    }

    private boolean startIPv4() {
        if (this.mConfiguration.mStaticIpConfig == null) {
            this.mDhcpClient = HwDhcpClient.makeHwDhcpClient(this.mContext, this, this.mInterfaceName);
            this.mDhcpClient.putPendingSSID(this.mPendingSSID);
            this.mDhcpClient.registerForPreDhcpNotification();
            this.mDhcpClient.sendMessage(DhcpClient.CMD_START_DHCP);
        } else if (!this.mInterfaceCtrl.setIPv4Address(this.mConfiguration.mStaticIpConfig.ipAddress)) {
            return false;
        } else {
            handleIPv4Success(new DhcpResults(this.mConfiguration.mStaticIpConfig));
        }
        return true;
    }

    private boolean startIPv6() {
        if ("factory".equals(SystemProperties.get("ro.runmode", "normal"))) {
            this.mConfiguration.mIPv6AddrGenMode = 0;
            Log.d(this.mTag, "Factory mode,set IPv6AddrGenMode EUI64");
        }
        if (this.mInterfaceCtrl.setIPv6PrivacyExtensions(true) && this.mInterfaceCtrl.setIPv6AddrGenModeIfSupported(this.mConfiguration.mIPv6AddrGenMode) && this.mInterfaceCtrl.enableIPv6()) {
            return true;
        }
        return false;
    }

    private boolean applyInitialConfig(InitialConfiguration config) {
        for (LinkAddress addr : findAll(config.ipAddresses, -$$Lambda$IpClient$GdLECAc1sQEo2Jjde3Y4ykVjDBg.INSTANCE)) {
            if (!this.mInterfaceCtrl.addAddress(addr)) {
                return false;
            }
        }
        return true;
    }

    private boolean startIpReachabilityMonitor() {
        try {
            this.mIpReachabilityMonitor = new IpReachabilityMonitor(this.mContext, this.mInterfaceParams, getHandler(), this.mLog, new android.net.ip.IpReachabilityMonitor.Callback() {
                public void notifyLost(InetAddress ip, String logMsg) {
                    IpClient.this.mCallback.onReachabilityLost(logMsg);
                }
            }, this.mMultinetworkPolicyTracker);
        } catch (IllegalArgumentException iae) {
            logError("IpReachabilityMonitor failure: %s", iae);
            this.mIpReachabilityMonitor = null;
        }
        if (this.mIpReachabilityMonitor != null) {
            return true;
        }
        return false;
    }

    private void stopAllIP() {
        this.mInterfaceCtrl.disableIPv6();
        this.mInterfaceCtrl.clearAllAddresses();
    }

    public int getDhcpFlag() {
        return this.mDhcp_Flag;
    }

    static <T> boolean any(Iterable<T> coll, Predicate<T> fn) {
        for (T t : coll) {
            if (fn.test(t)) {
                return true;
            }
        }
        return false;
    }

    static <T> boolean all(Iterable<T> coll, Predicate<T> fn) {
        return any(coll, not(fn)) ^ 1;
    }

    static <T> Predicate<T> not(Predicate<T> fn) {
        return new -$$Lambda$IpClient$avmS8r0wlmaRuIXDbDPiQFTUPlA(fn);
    }

    static <T> String join(String delimiter, Collection<T> coll) {
        return (String) coll.stream().map(-$$Lambda$IpClient$JsVbJ5mpbRjwJuW_A3bDJMqYpF0.INSTANCE).collect(Collectors.joining(delimiter));
    }

    static <T> T find(Iterable<T> coll, Predicate<T> fn) {
        for (T t : coll) {
            if (fn.test(t)) {
                return t;
            }
        }
        return null;
    }

    static <T> List<T> findAll(Collection<T> coll, Predicate<T> fn) {
        return (List) coll.stream().filter(fn).collect(Collectors.toList());
    }

    public void setForceDhcpDiscovery() {
        this.forceDhcpDiscovery = true;
    }

    public boolean isDhcpDiscoveryForced() {
        return this.forceDhcpDiscovery;
    }

    public void forceRemoveDhcpCache() {
        if (this.mDhcpClient != null) {
            this.mDhcpClient.forceRemoveDhcpCache();
        }
    }

    public void setScreenOffMulticastFilter(boolean enabled) {
        sendMessage(20, Boolean.valueOf(enabled));
    }
}
