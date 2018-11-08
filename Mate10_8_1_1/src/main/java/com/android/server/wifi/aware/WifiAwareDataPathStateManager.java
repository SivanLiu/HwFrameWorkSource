package com.android.server.wifi.aware;

import android.content.Context;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.StatusCode;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.MatchAllNetworkSpecifier;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.RouteInfo;
import android.net.wifi.aware.WifiAwareAgentNetworkSpecifier;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.net.wifi.aware.WifiAwareUtils;
import android.os.INetworkManagementService;
import android.os.INetworkManagementService.Stub;
import android.os.Looper;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import libcore.util.HexEncoding;

public class WifiAwareDataPathStateManager {
    private static final String AGENT_TAG_PREFIX = "WIFI_AWARE_AGENT_";
    private static final String AWARE_INTERFACE_PREFIX = "aware_data";
    private static final boolean DBG = false;
    private static final int NETWORK_FACTORY_BANDWIDTH_AVAIL = 1;
    private static final int NETWORK_FACTORY_SCORE_AVAIL = 1;
    private static final int NETWORK_FACTORY_SIGNAL_STRENGTH_AVAIL = 1;
    private static final String NETWORK_TAG = "WIFI_AWARE_FACTORY";
    private static final String TAG = "WifiAwareDataPathStMgr";
    private static final boolean VDBG = false;
    private static final NetworkCapabilities sNetworkCapabilitiesFilter = new NetworkCapabilities();
    private WifiAwareMetrics mAwareMetrics;
    private Context mContext;
    private final Set<String> mInterfaces = new HashSet();
    private Looper mLooper;
    private final WifiAwareStateManager mMgr;
    private WifiAwareNetworkFactory mNetworkFactory;
    private final Map<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> mNetworkRequestsCache = new ArrayMap();
    public NetworkInterfaceWrapper mNiWrapper = new NetworkInterfaceWrapper();
    public INetworkManagementService mNwService;
    private WifiPermissionsWrapper mPermissionsWrapper;

    public static class AwareNetworkRequestInformation {
        static final int STATE_CONFIRMED = 102;
        static final int STATE_IDLE = 100;
        static final int STATE_INITIATOR_WAIT_FOR_REQUEST_RESPONSE = 103;
        static final int STATE_RESPONDER_WAIT_FOR_REQUEST = 104;
        static final int STATE_RESPONDER_WAIT_FOR_RESPOND_RESPONSE = 105;
        static final int STATE_TERMINATING = 106;
        static final int STATE_WAIT_FOR_CONFIRM = 101;
        public Set<WifiAwareNetworkSpecifier> equivalentSpecifiers = new HashSet();
        public String interfaceName;
        public int ndpId = 0;
        public WifiAwareNetworkAgent networkAgent;
        public WifiAwareNetworkSpecifier networkSpecifier;
        public byte[] peerDataMac;
        public byte[] peerDiscoveryMac = null;
        public int peerInstanceId = 0;
        public int pubSubId = 0;
        public long startTimestamp = 0;
        public int state;
        public int uid;

        void updateToSupportNewRequest(WifiAwareNetworkSpecifier ns) {
            if (this.equivalentSpecifiers.add(ns) && this.state == 102) {
                if (this.networkAgent == null) {
                    Log.wtf(WifiAwareDataPathStateManager.TAG, "updateToSupportNewRequest: null agent in CONFIRMED state!?");
                    return;
                }
                this.networkAgent.sendNetworkCapabilities(getNetworkCapabilities());
            }
        }

        void removeSupportForRequest(WifiAwareNetworkSpecifier ns) {
            this.equivalentSpecifiers.remove(ns);
        }

        private NetworkCapabilities getNetworkCapabilities() {
            NetworkCapabilities nc = new NetworkCapabilities(WifiAwareDataPathStateManager.sNetworkCapabilitiesFilter);
            nc.setNetworkSpecifier(new WifiAwareAgentNetworkSpecifier((WifiAwareNetworkSpecifier[]) this.equivalentSpecifiers.toArray(new WifiAwareNetworkSpecifier[this.equivalentSpecifiers.size()])));
            return nc;
        }

        CanonicalConnectionInfo getCanonicalDescriptor() {
            return new CanonicalConnectionInfo(this.peerDiscoveryMac, this.networkSpecifier.pmk, this.networkSpecifier.sessionId, this.networkSpecifier.passphrase);
        }

        static AwareNetworkRequestInformation processNetworkSpecifier(WifiAwareNetworkSpecifier ns, WifiAwareStateManager mgr, WifiPermissionsWrapper permissionWrapper) {
            int pubSubId = 0;
            int peerInstanceId = 0;
            byte[] peerMac = ns.peerMac;
            if (ns.type < 0 || ns.type > 3) {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns + ", invalid 'type' value");
                return null;
            } else if (ns.role != 0 && ns.role != 1) {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns + " -- invalid 'role' value");
                return null;
            } else if (ns.role != 0 || ns.type == 0 || ns.type == 2) {
                WifiAwareClientState client = mgr.getClient(ns.clientId);
                if (client == null) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns + " -- not client with this id -- clientId=" + ns.clientId);
                    return null;
                }
                int uid = client.getUid();
                if (ns.type == 0 || ns.type == 1) {
                    WifiAwareDiscoverySessionState session = client.getSession(ns.sessionId);
                    if (session == null) {
                        Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns + " -- no session with this id -- sessionId=" + ns.sessionId);
                        return null;
                    } else if ((session.isPublishSession() && ns.role != 1) || (!session.isPublishSession() && ns.role != 0)) {
                        Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns + " -- invalid role for session type");
                        return null;
                    } else if (ns.type == 0) {
                        pubSubId = session.getPubSubId();
                        PeerInfo peerInfo = session.getPeerInfo(ns.peerId);
                        if (peerInfo == null) {
                            Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns + " -- no peer info associated with this peer id -- peerId=" + ns.peerId);
                            return null;
                        }
                        peerInstanceId = peerInfo.mInstanceId;
                        try {
                            peerMac = peerInfo.mMac;
                            if (peerMac == null || peerMac.length != 6) {
                                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns + " -- invalid peer MAC address");
                                return null;
                            }
                        } catch (IllegalArgumentException e) {
                            Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns + " -- invalid peer MAC address -- e=" + e);
                            return null;
                        }
                    }
                }
                if (ns.requestorUid != uid) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns.toString() + " -- UID mismatch to clientId's uid=" + uid);
                    return null;
                } else if (ns.pmk != null && ns.pmk.length != 0 && permissionWrapper.getUidPermission("android.permission.CONNECTIVITY_INTERNAL", ns.requestorUid) != 0) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns.toString() + " -- UID doesn't have permission to use PMK API");
                    return null;
                } else if (!TextUtils.isEmpty(ns.passphrase) && !WifiAwareUtils.validatePassphrase(ns.passphrase)) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns.toString() + " -- invalid passphrase length: " + ns.passphrase.length());
                    return null;
                } else if (ns.pmk == null || (WifiAwareUtils.validatePmk(ns.pmk) ^ 1) == 0) {
                    AwareNetworkRequestInformation nnri = new AwareNetworkRequestInformation();
                    nnri.state = 100;
                    nnri.uid = uid;
                    nnri.pubSubId = pubSubId;
                    nnri.peerInstanceId = peerInstanceId;
                    nnri.peerDiscoveryMac = peerMac;
                    nnri.networkSpecifier = ns;
                    nnri.equivalentSpecifiers.add(ns);
                    return nnri;
                } else {
                    Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns.toString() + " -- invalid pmk length: " + ns.pmk.length);
                    return null;
                }
            } else {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + ns + " -- invalid 'type' value for INITIATOR (only IB and OOB are " + "permitted)");
                return null;
            }
        }

        public String toString() {
            String str;
            StringBuilder sb = new StringBuilder("AwareNetworkRequestInformation: ");
            StringBuilder append = sb.append("state=").append(this.state).append(", ns=").append(this.networkSpecifier).append(", uid=").append(this.uid).append(", interfaceName=").append(this.interfaceName).append(", pubSubId=").append(this.pubSubId).append(", peerInstanceId=").append(this.peerInstanceId).append(", peerDiscoveryMac=");
            if (this.peerDiscoveryMac == null) {
                str = "";
            } else {
                str = String.valueOf(HexEncoding.encode(this.peerDiscoveryMac));
            }
            append = append.append(str).append(", ndpId=").append(this.ndpId).append(", peerDataMac=");
            if (this.peerDataMac == null) {
                str = "";
            } else {
                str = String.valueOf(HexEncoding.encode(this.peerDataMac));
            }
            append.append(str).append(", startTimestamp=").append(this.startTimestamp).append(", equivalentSpecifiers=[");
            for (WifiAwareNetworkSpecifier ns : this.equivalentSpecifiers) {
                sb.append(ns.toString()).append(", ");
            }
            return sb.append("]").toString();
        }
    }

    static class CanonicalConnectionInfo {
        public final String passphrase;
        public final byte[] peerDiscoveryMac;
        public final byte[] pmk;
        public final int sessionId;

        CanonicalConnectionInfo(byte[] peerDiscoveryMac, byte[] pmk, int sessionId, String passphrase) {
            this.peerDiscoveryMac = peerDiscoveryMac;
            this.pmk = pmk;
            this.sessionId = sessionId;
            this.passphrase = passphrase;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{Integer.valueOf(Arrays.hashCode(this.peerDiscoveryMac)), Integer.valueOf(Arrays.hashCode(this.pmk)), Integer.valueOf(this.sessionId), this.passphrase});
        }

        public boolean equals(Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CanonicalConnectionInfo)) {
                return false;
            }
            CanonicalConnectionInfo lhs = (CanonicalConnectionInfo) obj;
            if (!Arrays.equals(this.peerDiscoveryMac, lhs.peerDiscoveryMac) || !Arrays.equals(this.pmk, lhs.pmk) || !TextUtils.equals(this.passphrase, lhs.passphrase)) {
                z = false;
            } else if (this.sessionId != lhs.sessionId) {
                z = false;
            }
            return z;
        }

        public String toString() {
            String str;
            StringBuilder sb = new StringBuilder("CanonicalConnectionInfo: [");
            StringBuilder append = sb.append("peerDiscoveryMac=");
            if (this.peerDiscoveryMac == null) {
                str = "";
            } else {
                str = String.valueOf(HexEncoding.encode(this.peerDiscoveryMac));
            }
            append.append(str).append("pmk=").append(this.pmk == null ? "" : "*").append("sessionId=").append(this.sessionId).append("passphrase=").append(this.passphrase == null ? "" : "*").append("]");
            return sb.toString();
        }
    }

    public class NetworkInterfaceWrapper {
        public boolean configureAgentProperties(AwareNetworkRequestInformation nnri, Set<WifiAwareNetworkSpecifier> networkSpecifiers, int ndpId, NetworkInfo networkInfo, NetworkCapabilities networkCapabilities, LinkProperties linkProperties) {
            InetAddress linkLocal = null;
            try {
                NetworkInterface ni = NetworkInterface.getByName(nnri.interfaceName);
                if (ni == null) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "onDataPathConfirm: ACCEPT nnri=" + nnri + ": can't get network interface (null)");
                    WifiAwareDataPathStateManager.this.mMgr.endDataPath(ndpId);
                    nnri.state = StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                    return false;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = (InetAddress) addresses.nextElement();
                    if ((ip instanceof Inet6Address) && ip.isLinkLocalAddress()) {
                        linkLocal = ip;
                        break;
                    }
                }
                if (linkLocal == null) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "onDataPathConfirm: ACCEPT nnri=" + nnri + ": no link local addresses");
                    WifiAwareDataPathStateManager.this.mMgr.endDataPath(ndpId);
                    nnri.state = StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                    return false;
                }
                networkInfo.setIsAvailable(true);
                networkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
                networkCapabilities.setNetworkSpecifier(new WifiAwareAgentNetworkSpecifier((WifiAwareNetworkSpecifier[]) networkSpecifiers.toArray(new WifiAwareNetworkSpecifier[0])));
                linkProperties.setInterfaceName(nnri.interfaceName);
                linkProperties.addLinkAddress(new LinkAddress(linkLocal, 64));
                linkProperties.addRoute(new RouteInfo(new IpPrefix("fe80::/64"), null, nnri.interfaceName));
                return true;
            } catch (SocketException e) {
                Log.e(WifiAwareDataPathStateManager.TAG, "onDataPathConfirm: ACCEPT nnri=" + nnri + ": can't get network interface - " + e);
                WifiAwareDataPathStateManager.this.mMgr.endDataPath(ndpId);
                nnri.state = StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                return false;
            }
        }
    }

    private class WifiAwareNetworkAgent extends NetworkAgent {
        private AwareNetworkRequestInformation mAwareNetworkRequestInfo;
        private NetworkInfo mNetworkInfo;

        WifiAwareNetworkAgent(Looper looper, Context context, String logTag, NetworkInfo ni, NetworkCapabilities nc, LinkProperties lp, int score, AwareNetworkRequestInformation anri) {
            super(looper, context, logTag, ni, nc, lp, score);
            this.mNetworkInfo = ni;
            this.mAwareNetworkRequestInfo = anri;
        }

        protected void unwanted() {
            WifiAwareDataPathStateManager.this.mMgr.endDataPath(this.mAwareNetworkRequestInfo.ndpId);
            this.mAwareNetworkRequestInfo.state = StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
        }

        void reconfigureAgentAsDisconnected() {
            this.mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, null, "");
            sendNetworkInfo(this.mNetworkInfo);
        }
    }

    private class WifiAwareNetworkFactory extends NetworkFactory {
        private boolean mWaitingForTermination = false;

        WifiAwareNetworkFactory(Looper looper, Context context, NetworkCapabilities filter) {
            super(looper, context, WifiAwareDataPathStateManager.NETWORK_TAG, filter);
        }

        public void tickleConnectivityIfWaiting() {
            if (this.mWaitingForTermination) {
                this.mWaitingForTermination = false;
                reevaluateAllRequests();
            }
        }

        public boolean acceptRequest(NetworkRequest request, int score) {
            if (!WifiAwareDataPathStateManager.this.mMgr.isUsageEnabled()) {
                return false;
            }
            if (WifiAwareDataPathStateManager.this.mInterfaces.isEmpty()) {
                Log.w(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + request + " -- No Aware interfaces are up");
                return false;
            }
            NetworkSpecifier networkSpecifierBase = request.networkCapabilities.getNetworkSpecifier();
            if (networkSpecifierBase instanceof WifiAwareNetworkSpecifier) {
                WifiAwareNetworkSpecifier networkSpecifier = (WifiAwareNetworkSpecifier) networkSpecifierBase;
                AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) WifiAwareDataPathStateManager.this.mNetworkRequestsCache.get(networkSpecifier);
                if (nnri == null) {
                    nnri = AwareNetworkRequestInformation.processNetworkSpecifier(networkSpecifier, WifiAwareDataPathStateManager.this.mMgr, WifiAwareDataPathStateManager.this.mPermissionsWrapper);
                    if (nnri == null) {
                        Log.e(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + request + " - can't parse network specifier");
                        return false;
                    }
                    Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> primaryRequest = WifiAwareDataPathStateManager.this.getNetworkRequestByCanonicalDescriptor(nnri.getCanonicalDescriptor());
                    if (primaryRequest != null) {
                        if (((AwareNetworkRequestInformation) primaryRequest.getValue()).state == StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB) {
                            this.mWaitingForTermination = true;
                        } else {
                            ((AwareNetworkRequestInformation) primaryRequest.getValue()).updateToSupportNewRequest(networkSpecifier);
                        }
                        return false;
                    }
                    WifiAwareDataPathStateManager.this.mNetworkRequestsCache.put(networkSpecifier, nnri);
                    return true;
                } else if (nnri.state != StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB) {
                    return true;
                } else {
                    this.mWaitingForTermination = true;
                    return false;
                }
            }
            Log.w(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + request + " - not a WifiAwareNetworkSpecifier");
            return false;
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int score) {
            NetworkSpecifier networkSpecifierObj = networkRequest.networkCapabilities.getNetworkSpecifier();
            WifiAwareNetworkSpecifier networkSpecifier = null;
            if (networkSpecifierObj instanceof WifiAwareNetworkSpecifier) {
                networkSpecifier = (WifiAwareNetworkSpecifier) networkSpecifierObj;
            }
            AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) WifiAwareDataPathStateManager.this.mNetworkRequestsCache.get(networkSpecifier);
            if (nnri == null) {
                Log.e(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.needNetworkFor: networkRequest=" + networkRequest + " not in cache!?");
            } else if (nnri.state == 100) {
                if (nnri.networkSpecifier.role == 0) {
                    nnri.interfaceName = WifiAwareDataPathStateManager.this.selectInterfaceForRequest(nnri);
                    if (nnri.interfaceName == null) {
                        Log.w(WifiAwareDataPathStateManager.TAG, "needNetworkFor: request " + networkSpecifier + " no interface available");
                        WifiAwareDataPathStateManager.this.mNetworkRequestsCache.remove(networkSpecifier);
                        return;
                    }
                    WifiAwareDataPathStateManager.this.mMgr.initiateDataPathSetup(networkSpecifier, nnri.peerInstanceId, 0, WifiAwareDataPathStateManager.this.selectChannelForRequest(nnri), nnri.peerDiscoveryMac, nnri.interfaceName, nnri.networkSpecifier.pmk, nnri.networkSpecifier.passphrase, nnri.networkSpecifier.isOutOfBand());
                    nnri.state = 103;
                    nnri.startTimestamp = SystemClock.elapsedRealtime();
                } else {
                    nnri.state = 104;
                }
            }
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            NetworkSpecifier networkSpecifierObj = networkRequest.networkCapabilities.getNetworkSpecifier();
            WifiAwareNetworkSpecifier networkSpecifier = null;
            if (networkSpecifierObj instanceof WifiAwareNetworkSpecifier) {
                networkSpecifier = (WifiAwareNetworkSpecifier) networkSpecifierObj;
            }
            AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) WifiAwareDataPathStateManager.this.mNetworkRequestsCache.get(networkSpecifier);
            if (nnri == null) {
                Log.e(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.releaseNetworkFor: networkRequest=" + networkRequest + " not in cache!?");
            } else if (nnri.networkAgent == null) {
                nnri.removeSupportForRequest(networkSpecifier);
                if (nnri.equivalentSpecifiers.isEmpty() && nnri.ndpId != 0) {
                    WifiAwareDataPathStateManager.this.mMgr.endDataPath(nnri.ndpId);
                    nnri.state = StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                }
            }
        }
    }

    public WifiAwareDataPathStateManager(WifiAwareStateManager mgr) {
        this.mMgr = mgr;
    }

    public void start(Context context, Looper looper, WifiAwareMetrics awareMetrics, WifiPermissionsWrapper permissionsWrapper) {
        this.mContext = context;
        this.mAwareMetrics = awareMetrics;
        this.mPermissionsWrapper = permissionsWrapper;
        this.mLooper = looper;
        sNetworkCapabilitiesFilter.clearAll();
        sNetworkCapabilitiesFilter.addTransportType(5);
        sNetworkCapabilitiesFilter.addCapability(15).addCapability(11).addCapability(13).addCapability(14);
        sNetworkCapabilitiesFilter.setNetworkSpecifier(new MatchAllNetworkSpecifier());
        sNetworkCapabilitiesFilter.setLinkUpstreamBandwidthKbps(1);
        sNetworkCapabilitiesFilter.setLinkDownstreamBandwidthKbps(1);
        sNetworkCapabilitiesFilter.setSignalStrength(1);
        this.mNetworkFactory = new WifiAwareNetworkFactory(looper, context, sNetworkCapabilitiesFilter);
        this.mNetworkFactory.setScoreFilter(1);
        this.mNetworkFactory.register();
        this.mNwService = Stub.asInterface(ServiceManager.getService("network_management"));
    }

    private Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> getNetworkRequestByNdpId(int ndpId) {
        for (Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> entry : this.mNetworkRequestsCache.entrySet()) {
            if (((AwareNetworkRequestInformation) entry.getValue()).ndpId == ndpId) {
                return entry;
            }
        }
        return null;
    }

    private Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> getNetworkRequestByCanonicalDescriptor(CanonicalConnectionInfo cci) {
        for (Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> entry : this.mNetworkRequestsCache.entrySet()) {
            if (((AwareNetworkRequestInformation) entry.getValue()).getCanonicalDescriptor().equals(cci)) {
                return entry;
            }
        }
        return null;
    }

    public void createAllInterfaces() {
        if (this.mMgr.getCapabilities() == null) {
            Log.e(TAG, "createAllInterfaces: capabilities aren't initialized yet!");
            return;
        }
        for (int i = 0; i < this.mMgr.getCapabilities().maxNdiInterfaces; i++) {
            String name = AWARE_INTERFACE_PREFIX + i;
            if (this.mInterfaces.contains(name)) {
                Log.e(TAG, "createAllInterfaces(): interface already up, " + name + ", possibly failed to delete - deleting/creating again to be safe");
                this.mMgr.deleteDataPathInterface(name);
                this.mInterfaces.remove(name);
            }
            this.mMgr.createDataPathInterface(name);
        }
    }

    public void deleteAllInterfaces() {
        if (this.mMgr.getCapabilities() == null) {
            Log.e(TAG, "deleteAllInterfaces: capabilities aren't initialized yet!");
            return;
        }
        for (int i = 0; i < this.mMgr.getCapabilities().maxNdiInterfaces; i++) {
            this.mMgr.deleteDataPathInterface(AWARE_INTERFACE_PREFIX + i);
        }
    }

    public void onInterfaceCreated(String interfaceName) {
        if (this.mInterfaces.contains(interfaceName)) {
            Log.w(TAG, "onInterfaceCreated: already contains interface -- " + interfaceName);
        }
        this.mInterfaces.add(interfaceName);
    }

    public void onInterfaceDeleted(String interfaceName) {
        if (!this.mInterfaces.contains(interfaceName)) {
            Log.w(TAG, "onInterfaceDeleted: interface not on list -- " + interfaceName);
        }
        this.mInterfaces.remove(interfaceName);
    }

    public void onDataPathInitiateSuccess(WifiAwareNetworkSpecifier networkSpecifier, int ndpId) {
        AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) this.mNetworkRequestsCache.get(networkSpecifier);
        if (nnri == null) {
            Log.w(TAG, "onDataPathInitiateSuccess: network request not found for networkSpecifier=" + networkSpecifier);
            this.mMgr.endDataPath(ndpId);
        } else if (nnri.state != 103) {
            Log.w(TAG, "onDataPathInitiateSuccess: network request in incorrect state: state=" + nnri.state);
            this.mNetworkRequestsCache.remove(networkSpecifier);
            this.mMgr.endDataPath(ndpId);
        } else {
            nnri.state = 101;
            nnri.ndpId = ndpId;
        }
    }

    public void onDataPathInitiateFail(WifiAwareNetworkSpecifier networkSpecifier, int reason) {
        AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) this.mNetworkRequestsCache.remove(networkSpecifier);
        if (nnri == null) {
            Log.w(TAG, "onDataPathInitiateFail: network request not found for networkSpecifier=" + networkSpecifier);
            return;
        }
        if (nnri.state != 103) {
            Log.w(TAG, "onDataPathInitiateFail: network request in incorrect state: state=" + nnri.state);
        }
        this.mNetworkRequestsCache.remove(networkSpecifier);
        this.mAwareMetrics.recordNdpStatus(reason, networkSpecifier.isOutOfBand(), nnri.startTimestamp);
    }

    public WifiAwareNetworkSpecifier onDataPathRequest(int pubSubId, byte[] mac, int ndpId) {
        WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier = null;
        AwareNetworkRequestInformation awareNetworkRequestInformation = null;
        for (Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> entry : this.mNetworkRequestsCache.entrySet()) {
            if ((((AwareNetworkRequestInformation) entry.getValue()).pubSubId == 0 || ((AwareNetworkRequestInformation) entry.getValue()).pubSubId == pubSubId) && ((((AwareNetworkRequestInformation) entry.getValue()).peerDiscoveryMac == null || (Arrays.equals(((AwareNetworkRequestInformation) entry.getValue()).peerDiscoveryMac, mac) ^ 1) == 0) && ((AwareNetworkRequestInformation) entry.getValue()).state == 104)) {
                wifiAwareNetworkSpecifier = (WifiAwareNetworkSpecifier) entry.getKey();
                awareNetworkRequestInformation = (AwareNetworkRequestInformation) entry.getValue();
                break;
            }
        }
        if (awareNetworkRequestInformation == null) {
            Log.w(TAG, "onDataPathRequest: can't find a request with specified pubSubId=" + pubSubId + ", mac=" + String.valueOf(HexEncoding.encode(mac)));
            this.mMgr.respondToDataPathRequest(false, ndpId, "", null, null, false);
            return null;
        } else if (awareNetworkRequestInformation.state != 104) {
            Log.w(TAG, "onDataPathRequest: request " + wifiAwareNetworkSpecifier + " is incorrect state=" + awareNetworkRequestInformation.state);
            this.mMgr.respondToDataPathRequest(false, ndpId, "", null, null, false);
            this.mNetworkRequestsCache.remove(wifiAwareNetworkSpecifier);
            return null;
        } else {
            awareNetworkRequestInformation.interfaceName = selectInterfaceForRequest(awareNetworkRequestInformation);
            if (awareNetworkRequestInformation.interfaceName == null) {
                Log.w(TAG, "onDataPathRequest: request " + wifiAwareNetworkSpecifier + " no interface available");
                this.mMgr.respondToDataPathRequest(false, ndpId, "", null, null, false);
                this.mNetworkRequestsCache.remove(wifiAwareNetworkSpecifier);
                return null;
            }
            awareNetworkRequestInformation.state = StatusCode.ENABLEMENT_DENIED;
            awareNetworkRequestInformation.ndpId = ndpId;
            awareNetworkRequestInformation.startTimestamp = SystemClock.elapsedRealtime();
            this.mMgr.respondToDataPathRequest(true, ndpId, awareNetworkRequestInformation.interfaceName, awareNetworkRequestInformation.networkSpecifier.pmk, awareNetworkRequestInformation.networkSpecifier.passphrase, awareNetworkRequestInformation.networkSpecifier.isOutOfBand());
            return wifiAwareNetworkSpecifier;
        }
    }

    public void onRespondToDataPathRequest(int ndpId, boolean success, int reasonOnFailure) {
        WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier = null;
        AwareNetworkRequestInformation awareNetworkRequestInformation = null;
        for (Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> entry : this.mNetworkRequestsCache.entrySet()) {
            if (((AwareNetworkRequestInformation) entry.getValue()).ndpId == ndpId) {
                wifiAwareNetworkSpecifier = (WifiAwareNetworkSpecifier) entry.getKey();
                awareNetworkRequestInformation = (AwareNetworkRequestInformation) entry.getValue();
                break;
            }
        }
        if (awareNetworkRequestInformation == null) {
            Log.w(TAG, "onRespondToDataPathRequest: can't find a request with specified ndpId=" + ndpId);
        } else if (!success) {
            Log.w(TAG, "onRespondToDataPathRequest: request " + wifiAwareNetworkSpecifier + " failed responding");
            this.mMgr.endDataPath(ndpId);
            this.mNetworkRequestsCache.remove(wifiAwareNetworkSpecifier);
            this.mAwareMetrics.recordNdpStatus(reasonOnFailure, wifiAwareNetworkSpecifier.isOutOfBand(), awareNetworkRequestInformation.startTimestamp);
        } else if (awareNetworkRequestInformation.state != StatusCode.ENABLEMENT_DENIED) {
            Log.w(TAG, "onRespondToDataPathRequest: request " + wifiAwareNetworkSpecifier + " is incorrect state=" + awareNetworkRequestInformation.state);
            this.mMgr.endDataPath(ndpId);
            this.mNetworkRequestsCache.remove(wifiAwareNetworkSpecifier);
        } else {
            awareNetworkRequestInformation.state = 101;
        }
    }

    public WifiAwareNetworkSpecifier onDataPathConfirm(int ndpId, byte[] mac, boolean accept, int reason, byte[] message) {
        Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> nnriE = getNetworkRequestByNdpId(ndpId);
        if (nnriE == null) {
            Log.w(TAG, "onDataPathConfirm: network request not found for ndpId=" + ndpId);
            if (accept) {
                this.mMgr.endDataPath(ndpId);
            }
            return null;
        }
        WifiAwareNetworkSpecifier networkSpecifier = (WifiAwareNetworkSpecifier) nnriE.getKey();
        AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) nnriE.getValue();
        if (nnri.state != 101) {
            Log.w(TAG, "onDataPathConfirm: invalid state=" + nnri.state);
            this.mNetworkRequestsCache.remove(networkSpecifier);
            if (accept) {
                this.mMgr.endDataPath(ndpId);
            }
            return networkSpecifier;
        }
        if (accept) {
            nnri.state = 102;
            nnri.peerDataMac = mac;
            NetworkInfo networkInfo = new NetworkInfo(-1, 0, NETWORK_TAG, "");
            NetworkCapabilities networkCapabilities = new NetworkCapabilities(sNetworkCapabilitiesFilter);
            LinkProperties linkProperties = new LinkProperties();
            if (!isInterfaceUpAndUsedByAnotherNdp(nnri)) {
                try {
                    this.mNwService.setInterfaceUp(nnri.interfaceName);
                    this.mNwService.enableIpv6(nnri.interfaceName);
                } catch (Exception e) {
                    Log.e(TAG, "onDataPathConfirm: ACCEPT nnri=" + nnri + ": can't configure network - " + e);
                    this.mMgr.endDataPath(ndpId);
                    nnri.state = StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                    return networkSpecifier;
                }
            }
            if (!this.mNiWrapper.configureAgentProperties(nnri, nnri.equivalentSpecifiers, ndpId, networkInfo, networkCapabilities, linkProperties)) {
                return networkSpecifier;
            }
            nnri.networkAgent = new WifiAwareNetworkAgent(this.mLooper, this.mContext, AGENT_TAG_PREFIX + nnri.ndpId, new NetworkInfo(-1, 0, NETWORK_TAG, ""), networkCapabilities, linkProperties, 1, nnri);
            nnri.networkAgent.sendNetworkInfo(networkInfo);
            this.mAwareMetrics.recordNdpStatus(0, networkSpecifier.isOutOfBand(), nnri.startTimestamp);
            nnri.startTimestamp = SystemClock.elapsedRealtime();
            this.mAwareMetrics.recordNdpCreation(nnri.uid, this.mNetworkRequestsCache);
        } else {
            this.mNetworkRequestsCache.remove(networkSpecifier);
            this.mAwareMetrics.recordNdpStatus(reason, networkSpecifier.isOutOfBand(), nnri.startTimestamp);
        }
        return networkSpecifier;
    }

    public void onDataPathEnd(int ndpId) {
        Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> nnriE = getNetworkRequestByNdpId(ndpId);
        if (nnriE != null) {
            tearDownInterfaceIfPossible((AwareNetworkRequestInformation) nnriE.getValue());
            if (((AwareNetworkRequestInformation) nnriE.getValue()).state == 102 || ((AwareNetworkRequestInformation) nnriE.getValue()).state == StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB) {
                this.mAwareMetrics.recordNdpSessionDuration(((AwareNetworkRequestInformation) nnriE.getValue()).startTimestamp);
            }
            this.mNetworkRequestsCache.remove(nnriE.getKey());
            this.mNetworkFactory.tickleConnectivityIfWaiting();
        }
    }

    public void onAwareDownCleanupDataPaths() {
        for (AwareNetworkRequestInformation nnri : this.mNetworkRequestsCache.values()) {
            tearDownInterfaceIfPossible(nnri);
        }
        this.mNetworkRequestsCache.clear();
    }

    public void handleDataPathTimeout(NetworkSpecifier networkSpecifier) {
        AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) this.mNetworkRequestsCache.remove(networkSpecifier);
        if (nnri != null) {
            this.mAwareMetrics.recordNdpStatus(1, nnri.networkSpecifier.isOutOfBand(), nnri.startTimestamp);
            this.mMgr.endDataPath(nnri.ndpId);
            nnri.state = StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
        }
    }

    private void tearDownInterfaceIfPossible(AwareNetworkRequestInformation nnri) {
        if (!(TextUtils.isEmpty(nnri.interfaceName) || isInterfaceUpAndUsedByAnotherNdp(nnri))) {
            try {
                this.mNwService.setInterfaceDown(nnri.interfaceName);
            } catch (Exception e) {
                Log.e(TAG, "tearDownInterfaceIfPossible: nnri=" + nnri + ": can't bring interface down - " + e);
            }
        }
        if (nnri.networkAgent != null) {
            nnri.networkAgent.reconfigureAgentAsDisconnected();
        }
    }

    private boolean isInterfaceUpAndUsedByAnotherNdp(AwareNetworkRequestInformation nri) {
        for (AwareNetworkRequestInformation lnri : this.mNetworkRequestsCache.values()) {
            if (lnri != nri && nri.interfaceName.equals(lnri.interfaceName)) {
                if (lnri.state == 102 || lnri.state == StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB) {
                    return true;
                }
            }
        }
        return false;
    }

    private String selectInterfaceForRequest(AwareNetworkRequestInformation req) {
        SortedSet<String> potential = new TreeSet(this.mInterfaces);
        Set<String> used = new HashSet();
        for (AwareNetworkRequestInformation nnri : this.mNetworkRequestsCache.values()) {
            if (nnri != req && Arrays.equals(req.peerDiscoveryMac, nnri.peerDiscoveryMac)) {
                used.add(nnri.interfaceName);
            }
        }
        for (String ifName : potential) {
            if (!used.contains(ifName)) {
                return ifName;
            }
        }
        Log.e(TAG, "selectInterfaceForRequest: req=" + req + " - no interfaces available!");
        return null;
    }

    private int selectChannelForRequest(AwareNetworkRequestInformation req) {
        return 2437;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiAwareDataPathStateManager:");
        pw.println("  mInterfaces: " + this.mInterfaces);
        pw.println("  sNetworkCapabilitiesFilter: " + sNetworkCapabilitiesFilter);
        pw.println("  mNetworkRequestsCache: " + this.mNetworkRequestsCache);
        pw.println("  mNetworkFactory:");
        this.mNetworkFactory.dump(fd, pw, args);
    }
}
