package com.android.server.connectivity.tethering;

import android.net.INetd;
import android.net.INetworkStatsService;
import android.net.InterfaceConfiguration;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.ip.RouterAdvertisementDaemon;
import android.net.ip.RouterAdvertisementDaemon.RaParams;
import android.net.util.NetdService;
import android.net.util.SharedLog;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.os.SystemProperties;
import android.telephony.HwTelephonyManager;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;

public class TetherInterfaceStateMachine extends StateMachine {
    private static final int BASE_IFACE = 327780;
    public static final int CMD_INTERFACE_DOWN = 327784;
    public static final int CMD_IPV6_TETHER_UPDATE = 327793;
    public static final int CMD_IP_FORWARDING_DISABLE_ERROR = 327788;
    public static final int CMD_IP_FORWARDING_ENABLE_ERROR = 327787;
    public static final int CMD_SET_DNS_FORWARDERS_ERROR = 327791;
    public static final int CMD_START_TETHERING_ERROR = 327789;
    public static final int CMD_STOP_TETHERING_ERROR = 327790;
    public static final int CMD_TETHER_CONNECTION_CHANGED = 327792;
    public static final int CMD_TETHER_REQUESTED = 327782;
    public static final int CMD_TETHER_UNREQUESTED = 327783;
    private static final boolean DBG = false;
    private static final IpPrefix LINK_LOCAL_PREFIX = new IpPrefix("fe80::/64");
    private static final String PROPERTY_USBTETHERING_ON = "sys.isusbtetheringon";
    private static final String PROPERTY_WIFIHOTSPOT_ON = "sys.iswifihotspoton";
    private static final String TAG = "TetherInterfaceSM";
    private static final int USB_IS_CONNECTED = 1;
    private static final String USB_NEAR_IFACE_ADDR = "192.168.42.129";
    private static final int USB_NOT_CONNECTED = 0;
    private static final int USB_PREFIX_LENGTH = 24;
    private static final boolean VDBG = false;
    private static final String WIFI_HOST_IFACE_ADDR = "192.168.43.1";
    private static final int WIFI_HOST_IFACE_PREFIX_LENGTH = 24;
    private static final Class[] messageClasses = new Class[]{TetherInterfaceStateMachine.class};
    private static final SparseArray<String> sMagicDecoderRing = MessageUtils.findMessageNames(messageClasses);
    private boolean closeGROflag = false;
    private String faceName = null;
    private byte[] mHwAddr;
    private final String mIfaceName;
    private final State mInitialState;
    private final int mInterfaceType;
    private int mLastError;
    private LinkProperties mLastIPv6LinkProperties;
    private RaParams mLastRaParams;
    private final LinkProperties mLinkProperties;
    private final State mLocalHotspotState;
    private final SharedLog mLog;
    private String mMyUpstreamIfaceName;
    private final INetworkManagementService mNMService;
    private NetworkInterface mNetworkInterface;
    private RouterAdvertisementDaemon mRaDaemon;
    private int mServingMode;
    private final INetworkStatsService mStatsService;
    private final IControlsTethering mTetherController;
    private final State mTetheredState;
    private final State mUnavailableState;

    class BaseServingState extends State {
        BaseServingState() {
        }

        public void enter() {
            if (TetherInterfaceStateMachine.this.startIPv4()) {
                try {
                    TetherInterfaceStateMachine.this.mNMService.tetherInterface(TetherInterfaceStateMachine.this.mIfaceName);
                    if (!TetherInterfaceStateMachine.this.startIPv6()) {
                        TetherInterfaceStateMachine.this.mLog.e("Failed to startIPv6");
                        return;
                    }
                    return;
                } catch (Exception e) {
                    TetherInterfaceStateMachine.this.mLog.e("Error Tethering: " + e);
                    TetherInterfaceStateMachine.this.mLastError = 6;
                    return;
                }
            }
            TetherInterfaceStateMachine.this.mLastError = 10;
        }

        public void exit() {
            if (TetherInterfaceStateMachine.this.faceName != null && TetherInterfaceStateMachine.this.closeGROflag) {
                Log.d(TetherInterfaceStateMachine.TAG, "informModemTetherStatusToChangeGRO faild faceName =" + TetherInterfaceStateMachine.this.faceName);
                TetherInterfaceStateMachine.this.informModemTetherStatusToChangeGRO(0, TetherInterfaceStateMachine.this.faceName);
                TetherInterfaceStateMachine.this.faceName = null;
                TetherInterfaceStateMachine.this.closeGROflag = false;
            }
            TetherInterfaceStateMachine.this.stopIPv6();
            try {
                TetherInterfaceStateMachine.this.mNMService.untetherInterface(TetherInterfaceStateMachine.this.mIfaceName);
            } catch (Exception e) {
                TetherInterfaceStateMachine.this.mLastError = 7;
                TetherInterfaceStateMachine.this.mLog.e("Failed to untether interface: " + e);
            }
            TetherInterfaceStateMachine.this.stopIPv4();
            TetherInterfaceStateMachine.this.resetLinkProperties();
        }

        public boolean processMessage(Message message) {
            TetherInterfaceStateMachine.this.logMessage(this, message.what);
            switch (message.what) {
                case TetherInterfaceStateMachine.CMD_TETHER_UNREQUESTED /*327783*/:
                    TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mInitialState);
                    break;
                case TetherInterfaceStateMachine.CMD_INTERFACE_DOWN /*327784*/:
                    TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mUnavailableState);
                    break;
                case TetherInterfaceStateMachine.CMD_IP_FORWARDING_ENABLE_ERROR /*327787*/:
                case TetherInterfaceStateMachine.CMD_IP_FORWARDING_DISABLE_ERROR /*327788*/:
                case TetherInterfaceStateMachine.CMD_START_TETHERING_ERROR /*327789*/:
                case TetherInterfaceStateMachine.CMD_STOP_TETHERING_ERROR /*327790*/:
                case TetherInterfaceStateMachine.CMD_SET_DNS_FORWARDERS_ERROR /*327791*/:
                    TetherInterfaceStateMachine.this.mLastError = 5;
                    TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mInitialState);
                    break;
                case TetherInterfaceStateMachine.CMD_IPV6_TETHER_UPDATE /*327793*/:
                    TetherInterfaceStateMachine.this.updateUpstreamIPv6LinkProperties((LinkProperties) message.obj);
                    TetherInterfaceStateMachine.this.sendLinkProperties();
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    class InitialState extends State {
        InitialState() {
        }

        public void enter() {
            TetherInterfaceStateMachine.this.sendInterfaceState(1);
        }

        public boolean processMessage(Message message) {
            TetherInterfaceStateMachine.this.logMessage(this, message.what);
            switch (message.what) {
                case TetherInterfaceStateMachine.CMD_TETHER_REQUESTED /*327782*/:
                    TetherInterfaceStateMachine.this.mLastError = 0;
                    switch (message.arg1) {
                        case 2:
                            TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mTetheredState);
                            break;
                        case 3:
                            TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mLocalHotspotState);
                            break;
                        default:
                            TetherInterfaceStateMachine.this.mLog.e("Invalid tethering interface serving state specified.");
                            break;
                    }
                case TetherInterfaceStateMachine.CMD_INTERFACE_DOWN /*327784*/:
                    TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mUnavailableState);
                    break;
                case TetherInterfaceStateMachine.CMD_IPV6_TETHER_UPDATE /*327793*/:
                    TetherInterfaceStateMachine.this.updateUpstreamIPv6LinkProperties((LinkProperties) message.obj);
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    class LocalHotspotState extends BaseServingState {
        LocalHotspotState() {
            super();
        }

        public void enter() {
            super.enter();
            if (TetherInterfaceStateMachine.this.mLastError != 0) {
                TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mInitialState);
            }
            TetherInterfaceStateMachine.this.sendInterfaceState(3);
        }

        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            TetherInterfaceStateMachine.this.logMessage(this, message.what);
            switch (message.what) {
                case TetherInterfaceStateMachine.CMD_TETHER_REQUESTED /*327782*/:
                    TetherInterfaceStateMachine.this.mLog.e("CMD_TETHER_REQUESTED while in local-only hotspot mode.");
                    break;
                case TetherInterfaceStateMachine.CMD_TETHER_CONNECTION_CHANGED /*327792*/:
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    class TetheredState extends BaseServingState {
        TetheredState() {
            super();
        }

        public void enter() {
            super.enter();
            if (TetherInterfaceStateMachine.this.mLastError != 0) {
                TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mInitialState);
            }
            TetherInterfaceStateMachine.this.sendInterfaceState(2);
        }

        public void exit() {
            cleanupUpstream();
            super.exit();
        }

        private void cleanupUpstream() {
            if (TetherInterfaceStateMachine.this.mMyUpstreamIfaceName != null) {
                cleanupUpstreamInterface(TetherInterfaceStateMachine.this.mMyUpstreamIfaceName);
                TetherInterfaceStateMachine.this.mMyUpstreamIfaceName = null;
            }
        }

        private void cleanupUpstreamInterface(String upstreamIface) {
            try {
                TetherInterfaceStateMachine.this.mStatsService.forceUpdate();
            } catch (Exception e) {
            }
            try {
                TetherInterfaceStateMachine.this.mNMService.stopInterfaceForwarding(TetherInterfaceStateMachine.this.mIfaceName, upstreamIface);
            } catch (Exception e2) {
            }
            try {
                TetherInterfaceStateMachine.this.mNMService.disableNat(TetherInterfaceStateMachine.this.mIfaceName, upstreamIface);
            } catch (Exception e3) {
            }
        }

        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            TetherInterfaceStateMachine.this.logMessage(this, message.what);
            switch (message.what) {
                case TetherInterfaceStateMachine.CMD_TETHER_REQUESTED /*327782*/:
                    TetherInterfaceStateMachine.this.mLog.e("CMD_TETHER_REQUESTED while already tethering.");
                    break;
                case TetherInterfaceStateMachine.CMD_TETHER_CONNECTION_CHANGED /*327792*/:
                    String newUpstreamIfaceName = message.obj;
                    if (!(TetherInterfaceStateMachine.this.mMyUpstreamIfaceName == null && newUpstreamIfaceName == null) && (TetherInterfaceStateMachine.this.mMyUpstreamIfaceName == null || !TetherInterfaceStateMachine.this.mMyUpstreamIfaceName.equals(newUpstreamIfaceName))) {
                        cleanupUpstream();
                        if (newUpstreamIfaceName != null) {
                            try {
                                TetherInterfaceStateMachine.this.mNMService.enableNat(TetherInterfaceStateMachine.this.mIfaceName, newUpstreamIfaceName);
                                TetherInterfaceStateMachine.this.mNMService.startInterfaceForwarding(TetherInterfaceStateMachine.this.mIfaceName, newUpstreamIfaceName);
                                boolean isUsbTetherON = SystemProperties.getBoolean(TetherInterfaceStateMachine.PROPERTY_USBTETHERING_ON, false);
                                boolean isWifiHotspotON = SystemProperties.getBoolean(TetherInterfaceStateMachine.PROPERTY_WIFIHOTSPOT_ON, false);
                                if ((!TetherInterfaceStateMachine.this.closeGROflag && isUsbTetherON && "rndis0".equals(TetherInterfaceStateMachine.this.mIfaceName) && newUpstreamIfaceName.contains("v4-rmnet")) || (!TetherInterfaceStateMachine.this.closeGROflag && isWifiHotspotON && "wlan0".equals(TetherInterfaceStateMachine.this.mIfaceName) && newUpstreamIfaceName.contains("v4-rmnet"))) {
                                    TetherInterfaceStateMachine.this.closeGROflag = true;
                                    TetherInterfaceStateMachine.this.faceName = newUpstreamIfaceName.substring(newUpstreamIfaceName.indexOf("-") + 1);
                                    TetherInterfaceStateMachine.this.informModemTetherStatusToChangeGRO(1, TetherInterfaceStateMachine.this.faceName);
                                    Log.d(TetherInterfaceStateMachine.TAG, "informModemTetherStatusToChangeGRO success faceName =" + TetherInterfaceStateMachine.this.faceName);
                                }
                            } catch (Exception e) {
                                TetherInterfaceStateMachine.this.mLog.e("Exception enabling NAT: " + e);
                                cleanupUpstreamInterface(newUpstreamIfaceName);
                                TetherInterfaceStateMachine.this.mLastError = 8;
                                TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mInitialState);
                                return true;
                            }
                        }
                        TetherInterfaceStateMachine.this.mMyUpstreamIfaceName = newUpstreamIfaceName;
                        break;
                    }
                default:
                    return false;
            }
            return true;
        }
    }

    class UnavailableState extends State {
        UnavailableState() {
        }

        public void enter() {
            TetherInterfaceStateMachine.this.mLastError = 0;
            TetherInterfaceStateMachine.this.sendInterfaceState(0);
        }
    }

    public TetherInterfaceStateMachine(String ifaceName, Looper looper, int interfaceType, SharedLog log, INetworkManagementService nMService, INetworkStatsService statsService, IControlsTethering tetherController) {
        super(ifaceName, looper);
        this.mLog = log.forSubComponent(ifaceName);
        this.mNMService = nMService;
        this.mStatsService = statsService;
        this.mTetherController = tetherController;
        this.mIfaceName = ifaceName;
        this.mInterfaceType = interfaceType;
        this.mLinkProperties = new LinkProperties();
        resetLinkProperties();
        this.mLastError = 0;
        this.mServingMode = 1;
        this.mInitialState = new InitialState();
        this.mLocalHotspotState = new LocalHotspotState();
        this.mTetheredState = new TetheredState();
        this.mUnavailableState = new UnavailableState();
        addState(this.mInitialState);
        addState(this.mLocalHotspotState);
        addState(this.mTetheredState);
        addState(this.mUnavailableState);
        setInitialState(this.mInitialState);
    }

    public String interfaceName() {
        return this.mIfaceName;
    }

    public int interfaceType() {
        return this.mInterfaceType;
    }

    public int lastError() {
        return this.mLastError;
    }

    public int servingMode() {
        return this.mServingMode;
    }

    public LinkProperties linkProperties() {
        return new LinkProperties(this.mLinkProperties);
    }

    public void stop() {
        sendMessage(CMD_INTERFACE_DOWN);
    }

    public void unwanted() {
        sendMessage(CMD_TETHER_UNREQUESTED);
    }

    private boolean startIPv4() {
        return configureIPv4(true);
    }

    private void stopIPv4() {
        configureIPv4(false);
    }

    private boolean configureIPv4(boolean enabled) {
        String ipAsString;
        int prefixLen;
        if (this.mInterfaceType == 1) {
            ipAsString = USB_NEAR_IFACE_ADDR;
            prefixLen = 24;
        } else if (this.mInterfaceType != 0) {
            return true;
        } else {
            ipAsString = WIFI_HOST_IFACE_ADDR;
            prefixLen = 24;
        }
        try {
            InterfaceConfiguration ifcg = this.mNMService.getInterfaceConfig(this.mIfaceName);
            if (ifcg == null) {
                this.mLog.e("Received null interface config");
                return false;
            }
            LinkAddress linkAddr = new LinkAddress(NetworkUtils.numericToInetAddress(ipAsString), prefixLen);
            ifcg.setLinkAddress(linkAddr);
            if (this.mInterfaceType == 0) {
                ifcg.ignoreInterfaceUpDownStatus();
            } else if (enabled) {
                ifcg.setInterfaceUp();
            } else {
                ifcg.setInterfaceDown();
            }
            ifcg.clearFlag("running");
            this.mNMService.setInterfaceConfig(this.mIfaceName, ifcg);
            RouteInfo route = new RouteInfo(linkAddr);
            if (enabled) {
                this.mLinkProperties.addLinkAddress(linkAddr);
                this.mLinkProperties.addRoute(route);
            } else {
                this.mLinkProperties.removeLinkAddress(linkAddr);
                this.mLinkProperties.removeRoute(route);
            }
            return true;
        } catch (Exception e) {
            this.mLog.e("Error configuring interface " + e);
            return false;
        }
    }

    private boolean startIPv6() {
        try {
            this.mNetworkInterface = NetworkInterface.getByName(this.mIfaceName);
            if (this.mNetworkInterface == null) {
                this.mLog.e("Failed to find NetworkInterface");
                stopIPv6();
                return false;
            }
            try {
                this.mHwAddr = this.mNetworkInterface.getHardwareAddress();
                this.mRaDaemon = new RouterAdvertisementDaemon(this.mIfaceName, this.mNetworkInterface.getIndex(), this.mHwAddr);
                if (this.mRaDaemon.start()) {
                    return true;
                }
                stopIPv6();
                return false;
            } catch (SocketException e) {
                this.mLog.e("Failed to find hardware address: " + e);
                stopIPv6();
                return false;
            }
        } catch (SocketException e2) {
            this.mLog.e("Error looking up NetworkInterfaces: " + e2);
            stopIPv6();
            return false;
        }
    }

    private void stopIPv6() {
        this.mNetworkInterface = null;
        this.mHwAddr = null;
        setRaParams(null);
        if (this.mRaDaemon != null) {
            this.mRaDaemon.stop();
            this.mRaDaemon = null;
        }
    }

    private void updateUpstreamIPv6LinkProperties(LinkProperties v6only) {
        if (this.mRaDaemon != null && !Objects.equals(this.mLastIPv6LinkProperties, v6only)) {
            RaParams params = null;
            if (v6only != null) {
                params = new RaParams();
                params.mtu = v6only.getMtu();
                params.hasDefaultRoute = v6only.hasIPv6DefaultRoute();
                for (LinkAddress linkAddr : v6only.getLinkAddresses()) {
                    if (linkAddr.getPrefixLength() == 64) {
                        IpPrefix prefix = new IpPrefix(linkAddr.getAddress(), linkAddr.getPrefixLength());
                        params.prefixes.add(prefix);
                        Inet6Address dnsServer = getLocalDnsIpFor(prefix);
                        if (dnsServer != null) {
                            params.dnses.add(dnsServer);
                        }
                    }
                }
            }
            setRaParams(params);
            this.mLastIPv6LinkProperties = v6only;
        }
    }

    private void configureLocalIPv6Routes(HashSet<IpPrefix> deprecatedPrefixes, HashSet<IpPrefix> newPrefixes) {
        if (!deprecatedPrefixes.isEmpty()) {
            ArrayList<RouteInfo> toBeRemoved = getLocalRoutesFor(this.mIfaceName, deprecatedPrefixes);
            try {
                if (this.mNMService.removeRoutesFromLocalNetwork(toBeRemoved) > 0) {
                    this.mLog.e(String.format("Failed to remove %d IPv6 routes from local table.", new Object[]{Integer.valueOf(removalFailures)}));
                }
            } catch (RemoteException e) {
                this.mLog.e("Failed to remove IPv6 routes from local table: " + e);
            }
            for (RouteInfo route : toBeRemoved) {
                this.mLinkProperties.removeRoute(route);
            }
        }
        if (newPrefixes != null && (newPrefixes.isEmpty() ^ 1) != 0) {
            HashSet<IpPrefix> addedPrefixes = (HashSet) newPrefixes.clone();
            if (this.mLastRaParams != null) {
                addedPrefixes.removeAll(this.mLastRaParams.prefixes);
            }
            if (this.mLastRaParams == null || this.mLastRaParams.prefixes.isEmpty()) {
                addedPrefixes.add(LINK_LOCAL_PREFIX);
            }
            if (!addedPrefixes.isEmpty()) {
                ArrayList<RouteInfo> toBeAdded = getLocalRoutesFor(this.mIfaceName, addedPrefixes);
                try {
                    this.mNMService.addInterfaceToLocalNetwork(this.mIfaceName, toBeAdded);
                } catch (RemoteException e2) {
                    this.mLog.e("Failed to add IPv6 routes to local table: " + e2);
                }
                for (RouteInfo route2 : toBeAdded) {
                    this.mLinkProperties.addRoute(route2);
                }
            }
        }
    }

    private void configureLocalIPv6Dns(HashSet<Inet6Address> deprecatedDnses, HashSet<Inet6Address> newDnses) {
        String dnsString;
        INetd netd = NetdService.getInstance();
        if (netd == null) {
            if (newDnses != null) {
                newDnses.clear();
            }
            this.mLog.e("No netd service instance available; not setting local IPv6 addresses");
            return;
        }
        if (!deprecatedDnses.isEmpty()) {
            for (Inet6Address dns : deprecatedDnses) {
                dnsString = dns.getHostAddress();
                try {
                    netd.interfaceDelAddress(this.mIfaceName, dnsString, 64);
                } catch (Exception e) {
                    this.mLog.e("Failed to remove local dns IP " + dnsString + ": " + e);
                }
                this.mLinkProperties.removeLinkAddress(new LinkAddress(dns, 64));
            }
        }
        if (!(newDnses == null || (newDnses.isEmpty() ^ 1) == 0)) {
            HashSet<Inet6Address> addedDnses = (HashSet) newDnses.clone();
            if (this.mLastRaParams != null) {
                addedDnses.removeAll(this.mLastRaParams.dnses);
            }
            for (Inet6Address dns2 : addedDnses) {
                dnsString = dns2.getHostAddress();
                try {
                    netd.interfaceAddAddress(this.mIfaceName, dnsString, 64);
                } catch (Exception e2) {
                    this.mLog.e("Failed to add local dns IP " + dnsString + ": " + e2);
                    newDnses.remove(dns2);
                }
                this.mLinkProperties.addLinkAddress(new LinkAddress(dns2, 64));
            }
        }
        try {
            netd.tetherApplyDnsInterfaces();
        } catch (ServiceSpecificException e3) {
            this.mLog.e("Failed to update local DNS caching server");
            if (newDnses != null) {
                newDnses.clear();
            }
        }
    }

    private void setRaParams(RaParams newParams) {
        HashSet hashSet = null;
        if (this.mRaDaemon != null) {
            HashSet hashSet2;
            RaParams deprecatedParams = RaParams.getDeprecatedRaParams(this.mLastRaParams, newParams);
            HashSet hashSet3 = deprecatedParams.prefixes;
            if (newParams != null) {
                hashSet2 = newParams.prefixes;
            } else {
                hashSet2 = null;
            }
            configureLocalIPv6Routes(hashSet3, hashSet2);
            hashSet2 = deprecatedParams.dnses;
            if (newParams != null) {
                hashSet = newParams.dnses;
            }
            configureLocalIPv6Dns(hashSet2, hashSet);
            this.mRaDaemon.buildNewRa(deprecatedParams, newParams);
        }
        this.mLastRaParams = newParams;
    }

    private void logMessage(State state, int what) {
        this.mLog.log(state.getName() + " got " + ((String) sMagicDecoderRing.get(what, Integer.toString(what))));
    }

    private void sendInterfaceState(int newInterfaceState) {
        this.mServingMode = newInterfaceState;
        this.mTetherController.updateInterfaceState(this, newInterfaceState, this.mLastError);
        sendLinkProperties();
    }

    private void sendLinkProperties() {
        this.mTetherController.updateLinkProperties(this, new LinkProperties(this.mLinkProperties));
    }

    private void resetLinkProperties() {
        this.mLinkProperties.clear();
        this.mLinkProperties.setInterfaceName(this.mIfaceName);
    }

    private static ArrayList<RouteInfo> getLocalRoutesFor(String ifname, HashSet<IpPrefix> prefixes) {
        ArrayList<RouteInfo> localRoutes = new ArrayList();
        for (IpPrefix ipp : prefixes) {
            localRoutes.add(new RouteInfo(ipp, null, ifname));
        }
        return localRoutes;
    }

    private static Inet6Address getLocalDnsIpFor(IpPrefix localPrefix) {
        byte[] dnsBytes = localPrefix.getRawAddress();
        dnsBytes[dnsBytes.length - 1] = getRandomNonZeroByte();
        try {
            return Inet6Address.getByAddress(null, dnsBytes, 0);
        } catch (UnknownHostException e) {
            Slog.wtf(TAG, "Failed to construct Inet6Address from: " + localPrefix);
            return null;
        }
    }

    private static byte getRandomNonZeroByte() {
        byte random = (byte) new Random().nextInt();
        return random != (byte) 0 ? random : (byte) 1;
    }

    private void informModemTetherStatusToChangeGRO(int enable, String faceName) {
        HwTelephonyManager.getDefault().informModemTetherStatusToChangeGRO(enable, faceName);
    }
}
