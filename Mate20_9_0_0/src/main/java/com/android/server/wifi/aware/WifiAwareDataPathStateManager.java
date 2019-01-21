package com.android.server.wifi.aware;

import android.content.Context;
import android.hardware.wifi.V1_2.NanDataPathChannelInfo;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.StatusCode;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.MacAddress;
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
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.util.WifiPermissionsUtil;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import libcore.util.HexEncoding;

public class WifiAwareDataPathStateManager {
    private static final String AGENT_TAG_PREFIX = "WIFI_AWARE_AGENT_";
    private static final String AWARE_INTERFACE_PREFIX = "aware_data";
    private static final int NETWORK_FACTORY_BANDWIDTH_AVAIL = 1;
    private static final int NETWORK_FACTORY_SCORE_AVAIL = 1;
    private static final int NETWORK_FACTORY_SIGNAL_STRENGTH_AVAIL = 1;
    private static final String NETWORK_TAG = "WIFI_AWARE_FACTORY";
    private static final String TAG = "WifiAwareDataPathStMgr";
    private static final boolean VDBG = false;
    private static final NetworkCapabilities sNetworkCapabilitiesFilter = new NetworkCapabilities();
    boolean mAllowNdpResponderFromAnyOverride = false;
    private WifiAwareMetrics mAwareMetrics;
    private Context mContext;
    boolean mDbg = false;
    private final Set<String> mInterfaces = new HashSet();
    private Looper mLooper;
    private final WifiAwareStateManager mMgr;
    private WifiAwareNetworkFactory mNetworkFactory;
    private final Map<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> mNetworkRequestsCache = new ArrayMap();
    public NetworkInterfaceWrapper mNiWrapper = new NetworkInterfaceWrapper();
    public INetworkManagementService mNwService;
    private WifiPermissionsWrapper mPermissionsWrapper;
    private WifiPermissionsUtil mWifiPermissionsUtil;

    @VisibleForTesting
    public static class AwareNetworkRequestInformation {
        static final int STATE_CONFIRMED = 102;
        static final int STATE_IDLE = 100;
        static final int STATE_INITIATOR_WAIT_FOR_REQUEST_RESPONSE = 103;
        static final int STATE_RESPONDER_WAIT_FOR_REQUEST = 104;
        static final int STATE_RESPONDER_WAIT_FOR_RESPOND_RESPONSE = 105;
        static final int STATE_TERMINATING = 106;
        static final int STATE_WAIT_FOR_CONFIRM = 101;
        public List<NanDataPathChannelInfo> channelInfo;
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

        static AwareNetworkRequestInformation processNetworkSpecifier(WifiAwareNetworkSpecifier ns, WifiAwareStateManager mgr, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper permissionWrapper, boolean allowNdpResponderFromAnyOverride) {
            WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier = ns;
            int pubSubId = 0;
            int peerInstanceId = 0;
            byte[] peerMac = wifiAwareNetworkSpecifier.peerMac;
            WifiPermissionsUtil wifiPermissionsUtil2;
            WifiPermissionsWrapper wifiPermissionsWrapper;
            String str;
            StringBuilder stringBuilder;
            if (wifiAwareNetworkSpecifier.type < 0 || wifiAwareNetworkSpecifier.type > 3) {
                WifiAwareStateManager wifiAwareStateManager = mgr;
                wifiPermissionsUtil2 = wifiPermissionsUtil;
                wifiPermissionsWrapper = permissionWrapper;
                str = WifiAwareDataPathStateManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("processNetworkSpecifier: networkSpecifier=");
                stringBuilder.append(wifiAwareNetworkSpecifier);
                stringBuilder.append(", invalid 'type' value");
                Log.e(str, stringBuilder.toString());
                return null;
            } else if (wifiAwareNetworkSpecifier.role != 0 && wifiAwareNetworkSpecifier.role != 1) {
                str = WifiAwareDataPathStateManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("processNetworkSpecifier: networkSpecifier=");
                stringBuilder.append(wifiAwareNetworkSpecifier);
                stringBuilder.append(" -- invalid 'role' value");
                Log.e(str, stringBuilder.toString());
                return null;
            } else if (wifiAwareNetworkSpecifier.role != 0 || wifiAwareNetworkSpecifier.type == 0 || wifiAwareNetworkSpecifier.type == 2) {
                WifiAwareClientState client = mgr.getClient(wifiAwareNetworkSpecifier.clientId);
                String str2;
                StringBuilder stringBuilder2;
                if (client == null) {
                    str2 = WifiAwareDataPathStateManager.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("processNetworkSpecifier: networkSpecifier=");
                    stringBuilder2.append(wifiAwareNetworkSpecifier);
                    stringBuilder2.append(" -- not client with this id -- clientId=");
                    stringBuilder2.append(wifiAwareNetworkSpecifier.clientId);
                    Log.e(str2, stringBuilder2.toString());
                    return null;
                }
                int uid = client.getUid();
                if (allowNdpResponderFromAnyOverride) {
                    wifiPermissionsUtil2 = wifiPermissionsUtil;
                } else {
                    if (!(wifiPermissionsUtil.isLegacyVersion(client.getCallingPackage(), 28) || wifiAwareNetworkSpecifier.type == 0 || wifiAwareNetworkSpecifier.type == 2)) {
                        str2 = WifiAwareDataPathStateManager.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("processNetworkSpecifier: networkSpecifier=");
                        stringBuilder2.append(wifiAwareNetworkSpecifier);
                        stringBuilder2.append(" -- no ANY specifications allowed for this API level");
                        Log.e(str2, stringBuilder2.toString());
                        return null;
                    }
                }
                if (wifiAwareNetworkSpecifier.type == 0 || wifiAwareNetworkSpecifier.type == 1) {
                    WifiAwareDiscoverySessionState session = client.getSession(wifiAwareNetworkSpecifier.sessionId);
                    StringBuilder stringBuilder3;
                    if (session == null) {
                        str2 = WifiAwareDataPathStateManager.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("processNetworkSpecifier: networkSpecifier=");
                        stringBuilder3.append(wifiAwareNetworkSpecifier);
                        stringBuilder3.append(" -- no session with this id -- sessionId=");
                        stringBuilder3.append(wifiAwareNetworkSpecifier.sessionId);
                        Log.e(str2, stringBuilder3.toString());
                        return null;
                    } else if ((session.isPublishSession() && wifiAwareNetworkSpecifier.role != 1) || (!session.isPublishSession() && wifiAwareNetworkSpecifier.role != 0)) {
                        str2 = WifiAwareDataPathStateManager.TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("processNetworkSpecifier: networkSpecifier=");
                        stringBuilder3.append(wifiAwareNetworkSpecifier);
                        stringBuilder3.append(" -- invalid role for session type");
                        Log.e(str2, stringBuilder3.toString());
                        return null;
                    } else if (wifiAwareNetworkSpecifier.type == 0) {
                        int pubSubId2 = session.getPubSubId();
                        PeerInfo peerInfo = session.getPeerInfo(wifiAwareNetworkSpecifier.peerId);
                        String str3;
                        StringBuilder stringBuilder4;
                        if (peerInfo == null) {
                            str3 = WifiAwareDataPathStateManager.TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("processNetworkSpecifier: networkSpecifier=");
                            stringBuilder4.append(wifiAwareNetworkSpecifier);
                            stringBuilder4.append(" -- no peer info associated with this peer id -- peerId=");
                            stringBuilder4.append(wifiAwareNetworkSpecifier.peerId);
                            Log.e(str3, stringBuilder4.toString());
                            return null;
                        }
                        peerInstanceId = peerInfo.mInstanceId;
                        try {
                            peerMac = peerInfo.mMac;
                            if (peerMac != null) {
                                if (peerMac.length == 6) {
                                    pubSubId = pubSubId2;
                                }
                            }
                            str3 = WifiAwareDataPathStateManager.TAG;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("processNetworkSpecifier: networkSpecifier=");
                            stringBuilder4.append(wifiAwareNetworkSpecifier);
                            stringBuilder4.append(" -- invalid peer MAC address");
                            Log.e(str3, stringBuilder4.toString());
                            return null;
                        } catch (IllegalArgumentException pubSubId3) {
                            String str4 = WifiAwareDataPathStateManager.TAG;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("processNetworkSpecifier: networkSpecifier=");
                            stringBuilder5.append(wifiAwareNetworkSpecifier);
                            stringBuilder5.append(" -- invalid peer MAC address -- e=");
                            stringBuilder5.append(pubSubId3);
                            Log.e(str4, stringBuilder5.toString());
                            return null;
                        }
                    }
                }
                if (wifiAwareNetworkSpecifier.requestorUid != uid) {
                    str2 = WifiAwareDataPathStateManager.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("processNetworkSpecifier: networkSpecifier=");
                    stringBuilder2.append(wifiAwareNetworkSpecifier.toString());
                    stringBuilder2.append(" -- UID mismatch to clientId's uid=");
                    stringBuilder2.append(uid);
                    Log.e(str2, stringBuilder2.toString());
                    return null;
                }
                if (wifiAwareNetworkSpecifier.pmk == null || wifiAwareNetworkSpecifier.pmk.length == 0) {
                    wifiPermissionsWrapper = permissionWrapper;
                } else {
                    if (permissionWrapper.getUidPermission("android.permission.CONNECTIVITY_INTERNAL", wifiAwareNetworkSpecifier.requestorUid) != 0) {
                        str2 = WifiAwareDataPathStateManager.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("processNetworkSpecifier: networkSpecifier=");
                        stringBuilder2.append(wifiAwareNetworkSpecifier.toString());
                        stringBuilder2.append(" -- UID doesn't have permission to use PMK API");
                        Log.e(str2, stringBuilder2.toString());
                        return null;
                    }
                }
                if (!TextUtils.isEmpty(wifiAwareNetworkSpecifier.passphrase) && !WifiAwareUtils.validatePassphrase(wifiAwareNetworkSpecifier.passphrase)) {
                    str2 = WifiAwareDataPathStateManager.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("processNetworkSpecifier: networkSpecifier=");
                    stringBuilder2.append(wifiAwareNetworkSpecifier.toString());
                    stringBuilder2.append(" -- invalid passphrase length: ");
                    stringBuilder2.append(wifiAwareNetworkSpecifier.passphrase.length());
                    Log.e(str2, stringBuilder2.toString());
                    return null;
                } else if (wifiAwareNetworkSpecifier.pmk == null || WifiAwareUtils.validatePmk(wifiAwareNetworkSpecifier.pmk)) {
                    AwareNetworkRequestInformation nnri = new AwareNetworkRequestInformation();
                    nnri.state = 100;
                    nnri.uid = uid;
                    nnri.pubSubId = pubSubId3;
                    nnri.peerInstanceId = peerInstanceId;
                    nnri.peerDiscoveryMac = peerMac;
                    nnri.networkSpecifier = wifiAwareNetworkSpecifier;
                    nnri.equivalentSpecifiers.add(wifiAwareNetworkSpecifier);
                    return nnri;
                } else {
                    str2 = WifiAwareDataPathStateManager.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("processNetworkSpecifier: networkSpecifier=");
                    stringBuilder2.append(wifiAwareNetworkSpecifier.toString());
                    stringBuilder2.append(" -- invalid pmk length: ");
                    stringBuilder2.append(wifiAwareNetworkSpecifier.pmk.length);
                    Log.e(str2, stringBuilder2.toString());
                    return null;
                }
            } else {
                str = WifiAwareDataPathStateManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("processNetworkSpecifier: networkSpecifier=");
                stringBuilder.append(wifiAwareNetworkSpecifier);
                stringBuilder.append(" -- invalid 'type' value for INITIATOR (only IB and OOB are permitted)");
                Log.e(str, stringBuilder.toString());
                return null;
            }
        }

        public String toString() {
            String str;
            StringBuilder sb = new StringBuilder("AwareNetworkRequestInformation: ");
            sb.append("state=");
            sb.append(this.state);
            sb.append(", ns=");
            sb.append(this.networkSpecifier);
            sb.append(", uid=");
            sb.append(this.uid);
            sb.append(", interfaceName=");
            sb.append(this.interfaceName);
            sb.append(", pubSubId=");
            sb.append(this.pubSubId);
            sb.append(", peerInstanceId=");
            sb.append(this.peerInstanceId);
            sb.append(", peerDiscoveryMac=");
            if (this.peerDiscoveryMac == null) {
                str = "";
            } else {
                str = String.valueOf(HexEncoding.encode(this.peerDiscoveryMac));
            }
            sb.append(str);
            sb.append(", ndpId=");
            sb.append(this.ndpId);
            sb.append(", peerDataMac=");
            if (this.peerDataMac == null) {
                str = "";
            } else {
                str = String.valueOf(HexEncoding.encode(this.peerDataMac));
            }
            sb.append(str);
            sb.append(", startTimestamp=");
            sb.append(this.startTimestamp);
            sb.append(", channelInfo=");
            sb.append(this.channelInfo);
            sb.append(", equivalentSpecifiers=[");
            for (WifiAwareNetworkSpecifier ns : this.equivalentSpecifiers) {
                sb.append(ns.toString());
                sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
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

        public boolean matches(CanonicalConnectionInfo other) {
            return (other.peerDiscoveryMac == null || Arrays.equals(this.peerDiscoveryMac, other.peerDiscoveryMac)) && Arrays.equals(this.pmk, other.pmk) && TextUtils.equals(this.passphrase, other.passphrase) && (TextUtils.isEmpty(this.passphrase) || this.sessionId == other.sessionId);
        }

        public String toString() {
            String str;
            StringBuilder sb = new StringBuilder("CanonicalConnectionInfo: [");
            sb.append("peerDiscoveryMac=");
            if (this.peerDiscoveryMac == null) {
                str = "";
            } else {
                str = String.valueOf(HexEncoding.encode(this.peerDiscoveryMac));
            }
            sb.append(str);
            sb.append(", pmk=");
            sb.append(this.pmk == null ? "" : "*");
            sb.append(", sessionId=");
            sb.append(this.sessionId);
            sb.append(", passphrase=");
            sb.append(this.passphrase == null ? "" : "*");
            sb.append("]");
            return sb.toString();
        }
    }

    @VisibleForTesting
    public class NetworkInterfaceWrapper {
        public boolean configureAgentProperties(AwareNetworkRequestInformation nnri, Set<WifiAwareNetworkSpecifier> networkSpecifiers, int ndpId, NetworkInfo networkInfo, NetworkCapabilities networkCapabilities, LinkProperties linkProperties) {
            InetAddress linkLocal = null;
            StringBuilder stringBuilder;
            try {
                NetworkInterface ni = NetworkInterface.getByName(nnri.interfaceName);
                String str;
                if (ni == null) {
                    str = WifiAwareDataPathStateManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onDataPathConfirm: ACCEPT nnri=");
                    stringBuilder.append(nnri);
                    stringBuilder.append(": can't get network interface (null)");
                    Log.e(str, stringBuilder.toString());
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
                    str = WifiAwareDataPathStateManager.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("onDataPathConfirm: ACCEPT nnri=");
                    stringBuilder2.append(nnri);
                    stringBuilder2.append(": no link local addresses");
                    Log.e(str, stringBuilder2.toString());
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
                String str2 = WifiAwareDataPathStateManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onDataPathConfirm: ACCEPT nnri=");
                stringBuilder.append(nnri);
                stringBuilder.append(": can't get network interface - ");
                stringBuilder.append(e);
                Log.e(str2, stringBuilder.toString());
                WifiAwareDataPathStateManager.this.mMgr.endDataPath(ndpId);
                nnri.state = StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                return false;
            }
        }
    }

    private class WifiAwareNetworkAgent extends NetworkAgent {
        private AwareNetworkRequestInformation mAwareNetworkRequestInfo;
        private NetworkInfo mNetworkInfo;
        final /* synthetic */ WifiAwareDataPathStateManager this$0;

        WifiAwareNetworkAgent(WifiAwareDataPathStateManager wifiAwareDataPathStateManager, Looper looper, Context context, String logTag, NetworkInfo ni, NetworkCapabilities nc, LinkProperties lp, int score, AwareNetworkRequestInformation anri) {
            this.this$0 = wifiAwareDataPathStateManager;
            super(looper, context, logTag, ni, nc, lp, score);
            this.mNetworkInfo = ni;
            this.mAwareNetworkRequestInfo = anri;
        }

        protected void unwanted() {
            this.this$0.mMgr.endDataPath(this.mAwareNetworkRequestInfo.ndpId);
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
                String str = WifiAwareDataPathStateManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("WifiAwareNetworkFactory.acceptRequest: request=");
                stringBuilder.append(request);
                stringBuilder.append(" -- No Aware interfaces are up");
                Log.w(str, stringBuilder.toString());
                return false;
            }
            NetworkSpecifier networkSpecifierBase = request.networkCapabilities.getNetworkSpecifier();
            if (networkSpecifierBase instanceof WifiAwareNetworkSpecifier) {
                WifiAwareNetworkSpecifier networkSpecifier = (WifiAwareNetworkSpecifier) networkSpecifierBase;
                AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) WifiAwareDataPathStateManager.this.mNetworkRequestsCache.get(networkSpecifier);
                if (nnri == null) {
                    nnri = AwareNetworkRequestInformation.processNetworkSpecifier(networkSpecifier, WifiAwareDataPathStateManager.this.mMgr, WifiAwareDataPathStateManager.this.mWifiPermissionsUtil, WifiAwareDataPathStateManager.this.mPermissionsWrapper, WifiAwareDataPathStateManager.this.mAllowNdpResponderFromAnyOverride);
                    if (nnri == null) {
                        String str2 = WifiAwareDataPathStateManager.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("WifiAwareNetworkFactory.acceptRequest: request=");
                        stringBuilder2.append(request);
                        stringBuilder2.append(" - can't parse network specifier");
                        Log.e(str2, stringBuilder2.toString());
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
            String str3 = WifiAwareDataPathStateManager.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("WifiAwareNetworkFactory.acceptRequest: request=");
            stringBuilder3.append(request);
            stringBuilder3.append(" - not a WifiAwareNetworkSpecifier");
            Log.w(str3, stringBuilder3.toString());
            return false;
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int score) {
            NetworkSpecifier networkSpecifierObj = networkRequest.networkCapabilities.getNetworkSpecifier();
            WifiAwareNetworkSpecifier networkSpecifier = null;
            if (networkSpecifierObj instanceof WifiAwareNetworkSpecifier) {
                networkSpecifier = (WifiAwareNetworkSpecifier) networkSpecifierObj;
            }
            AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) WifiAwareDataPathStateManager.this.mNetworkRequestsCache.get(networkSpecifier);
            String str;
            StringBuilder stringBuilder;
            if (nnri == null) {
                str = WifiAwareDataPathStateManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("WifiAwareNetworkFactory.needNetworkFor: networkRequest=");
                stringBuilder.append(networkRequest);
                stringBuilder.append(" not in cache!?");
                Log.e(str, stringBuilder.toString());
            } else if (nnri.state == 100) {
                if (nnri.networkSpecifier.role == 0) {
                    nnri.interfaceName = WifiAwareDataPathStateManager.this.selectInterfaceForRequest(nnri);
                    if (nnri.interfaceName == null) {
                        str = WifiAwareDataPathStateManager.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("needNetworkFor: request ");
                        stringBuilder.append(networkSpecifier);
                        stringBuilder.append(" no interface available");
                        Log.w(str, stringBuilder.toString());
                        WifiAwareDataPathStateManager.this.mNetworkRequestsCache.remove(networkSpecifier);
                        return;
                    }
                    WifiAwareDataPathStateManager.this.mMgr.initiateDataPathSetup(networkSpecifier, nnri.peerInstanceId, 0, WifiAwareDataPathStateManager.this.selectChannelForRequest(nnri), nnri.peerDiscoveryMac, nnri.interfaceName, nnri.networkSpecifier.pmk, nnri.networkSpecifier.passphrase, nnri.networkSpecifier.isOutOfBand());
                    nnri.state = 103;
                    nnri.startTimestamp = SystemClock.elapsedRealtime();
                } else {
                    nnri.state = StatusCode.ASSOC_DENIED_NO_VHT;
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
                String str = WifiAwareDataPathStateManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("WifiAwareNetworkFactory.releaseNetworkFor: networkRequest=");
                stringBuilder.append(networkRequest);
                stringBuilder.append(" not in cache!?");
                Log.e(str, stringBuilder.toString());
            } else if (nnri.networkAgent == null) {
                nnri.removeSupportForRequest(networkSpecifier);
                if (nnri.equivalentSpecifiers.isEmpty()) {
                    if (nnri.ndpId != 0) {
                        WifiAwareDataPathStateManager.this.mMgr.endDataPath(nnri.ndpId);
                        nnri.state = StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                    } else {
                        WifiAwareDataPathStateManager.this.mNetworkRequestsCache.remove(networkSpecifier);
                    }
                }
            }
        }
    }

    public WifiAwareDataPathStateManager(WifiAwareStateManager mgr) {
        this.mMgr = mgr;
    }

    public void start(Context context, Looper looper, WifiAwareMetrics awareMetrics, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper permissionsWrapper) {
        this.mContext = context;
        this.mAwareMetrics = awareMetrics;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mPermissionsWrapper = permissionsWrapper;
        this.mLooper = looper;
        sNetworkCapabilitiesFilter.clearAll();
        sNetworkCapabilitiesFilter.addTransportType(5);
        sNetworkCapabilitiesFilter.addCapability(15).addCapability(11).addCapability(18).addCapability(20).addCapability(13).addCapability(14);
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
            if (((AwareNetworkRequestInformation) entry.getValue()).getCanonicalDescriptor().matches(cci)) {
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
            String name = new StringBuilder();
            name.append(AWARE_INTERFACE_PREFIX);
            name.append(i);
            name = name.toString();
            if (this.mInterfaces.contains(name)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("createAllInterfaces(): interface already up, ");
                stringBuilder.append(name);
                stringBuilder.append(", possibly failed to delete - deleting/creating again to be safe");
                Log.e(str, stringBuilder.toString());
                this.mMgr.deleteDataPathInterface(name);
                this.mInterfaces.remove(name);
            }
            this.mMgr.createDataPathInterface(name);
        }
    }

    public void deleteAllInterfaces() {
        onAwareDownCleanupDataPaths();
        if (this.mMgr.getCapabilities() == null) {
            Log.e(TAG, "deleteAllInterfaces: capabilities aren't initialized yet!");
            return;
        }
        for (int i = 0; i < this.mMgr.getCapabilities().maxNdiInterfaces; i++) {
            String name = new StringBuilder();
            name.append(AWARE_INTERFACE_PREFIX);
            name.append(i);
            this.mMgr.deleteDataPathInterface(name.toString());
        }
        this.mMgr.releaseAwareInterface();
    }

    public void onInterfaceCreated(String interfaceName) {
        if (this.mInterfaces.contains(interfaceName)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onInterfaceCreated: already contains interface -- ");
            stringBuilder.append(interfaceName);
            Log.w(str, stringBuilder.toString());
        }
        this.mInterfaces.add(interfaceName);
    }

    public void onInterfaceDeleted(String interfaceName) {
        if (!this.mInterfaces.contains(interfaceName)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onInterfaceDeleted: interface not on list -- ");
            stringBuilder.append(interfaceName);
            Log.w(str, stringBuilder.toString());
        }
        this.mInterfaces.remove(interfaceName);
    }

    public void onDataPathInitiateSuccess(WifiAwareNetworkSpecifier networkSpecifier, int ndpId) {
        AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) this.mNetworkRequestsCache.get(networkSpecifier);
        String str;
        StringBuilder stringBuilder;
        if (nnri == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onDataPathInitiateSuccess: network request not found for networkSpecifier=");
            stringBuilder.append(networkSpecifier);
            Log.w(str, stringBuilder.toString());
            this.mMgr.endDataPath(ndpId);
        } else if (nnri.state != 103) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onDataPathInitiateSuccess: network request in incorrect state: state=");
            stringBuilder.append(nnri.state);
            Log.w(str, stringBuilder.toString());
            this.mNetworkRequestsCache.remove(networkSpecifier);
            this.mMgr.endDataPath(ndpId);
        } else {
            nnri.state = 101;
            nnri.ndpId = ndpId;
        }
    }

    public void onDataPathInitiateFail(WifiAwareNetworkSpecifier networkSpecifier, int reason) {
        AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) this.mNetworkRequestsCache.remove(networkSpecifier);
        String str;
        StringBuilder stringBuilder;
        if (nnri == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onDataPathInitiateFail: network request not found for networkSpecifier=");
            stringBuilder.append(networkSpecifier);
            Log.w(str, stringBuilder.toString());
            return;
        }
        if (nnri.state != 103) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onDataPathInitiateFail: network request in incorrect state: state=");
            stringBuilder.append(nnri.state);
            Log.w(str, stringBuilder.toString());
        }
        this.mAwareMetrics.recordNdpStatus(reason, networkSpecifier.isOutOfBand(), nnri.startTimestamp);
    }

    public WifiAwareNetworkSpecifier onDataPathRequest(int pubSubId, byte[] mac, int ndpId) {
        WifiAwareNetworkSpecifier networkSpecifier = null;
        AwareNetworkRequestInformation nnri = null;
        for (Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> entry : this.mNetworkRequestsCache.entrySet()) {
            if (((AwareNetworkRequestInformation) entry.getValue()).pubSubId == 0 || ((AwareNetworkRequestInformation) entry.getValue()).pubSubId == pubSubId) {
                if (((AwareNetworkRequestInformation) entry.getValue()).peerDiscoveryMac == null || Arrays.equals(((AwareNetworkRequestInformation) entry.getValue()).peerDiscoveryMac, mac)) {
                    if (((AwareNetworkRequestInformation) entry.getValue()).state == StatusCode.ASSOC_DENIED_NO_VHT) {
                        networkSpecifier = (WifiAwareNetworkSpecifier) entry.getKey();
                        nnri = (AwareNetworkRequestInformation) entry.getValue();
                        break;
                    }
                }
            }
        }
        String str;
        StringBuilder stringBuilder;
        if (nnri == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onDataPathRequest: can't find a request with specified pubSubId=");
            stringBuilder.append(pubSubId);
            stringBuilder.append(", mac=");
            stringBuilder.append(String.valueOf(HexEncoding.encode(mac)));
            Log.w(str, stringBuilder.toString());
            this.mMgr.respondToDataPathRequest(false, ndpId, "", null, null, false);
            return null;
        }
        if (nnri.peerDiscoveryMac == null) {
            nnri.peerDiscoveryMac = mac;
        }
        nnri.interfaceName = selectInterfaceForRequest(nnri);
        if (nnri.interfaceName == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onDataPathRequest: request ");
            stringBuilder.append(networkSpecifier);
            stringBuilder.append(" no interface available");
            Log.w(str, stringBuilder.toString());
            this.mMgr.respondToDataPathRequest(false, ndpId, "", null, null, false);
            this.mNetworkRequestsCache.remove(networkSpecifier);
            return null;
        }
        nnri.state = StatusCode.ENABLEMENT_DENIED;
        nnri.ndpId = ndpId;
        nnri.startTimestamp = SystemClock.elapsedRealtime();
        this.mMgr.respondToDataPathRequest(true, ndpId, nnri.interfaceName, nnri.networkSpecifier.pmk, nnri.networkSpecifier.passphrase, nnri.networkSpecifier.isOutOfBand());
        return networkSpecifier;
    }

    public void onRespondToDataPathRequest(int ndpId, boolean success, int reasonOnFailure) {
        WifiAwareNetworkSpecifier networkSpecifier = null;
        AwareNetworkRequestInformation nnri = null;
        for (Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> entry : this.mNetworkRequestsCache.entrySet()) {
            if (((AwareNetworkRequestInformation) entry.getValue()).ndpId == ndpId) {
                networkSpecifier = (WifiAwareNetworkSpecifier) entry.getKey();
                nnri = (AwareNetworkRequestInformation) entry.getValue();
                break;
            }
        }
        String str;
        StringBuilder stringBuilder;
        if (nnri == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onRespondToDataPathRequest: can't find a request with specified ndpId=");
            stringBuilder.append(ndpId);
            Log.w(str, stringBuilder.toString());
        } else if (!success) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onRespondToDataPathRequest: request ");
            stringBuilder.append(networkSpecifier);
            stringBuilder.append(" failed responding");
            Log.w(str, stringBuilder.toString());
            this.mMgr.endDataPath(ndpId);
            this.mNetworkRequestsCache.remove(networkSpecifier);
            this.mAwareMetrics.recordNdpStatus(reasonOnFailure, networkSpecifier.isOutOfBand(), nnri.startTimestamp);
        } else if (nnri.state != StatusCode.ENABLEMENT_DENIED) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onRespondToDataPathRequest: request ");
            stringBuilder.append(networkSpecifier);
            stringBuilder.append(" is incorrect state=");
            stringBuilder.append(nnri.state);
            Log.w(str, stringBuilder.toString());
            this.mMgr.endDataPath(ndpId);
            this.mNetworkRequestsCache.remove(networkSpecifier);
        } else {
            nnri.state = 101;
        }
    }

    public WifiAwareNetworkSpecifier onDataPathConfirm(int ndpId, byte[] mac, boolean accept, int reason, byte[] message, List<NanDataPathChannelInfo> channelInfo) {
        int i = ndpId;
        Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> nnriE = getNetworkRequestByNdpId(ndpId);
        String str;
        StringBuilder stringBuilder;
        if (nnriE == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onDataPathConfirm: network request not found for ndpId=");
            stringBuilder.append(i);
            Log.w(str, stringBuilder.toString());
            if (accept) {
                this.mMgr.endDataPath(i);
            }
            return null;
        }
        WifiAwareNetworkSpecifier networkSpecifier = (WifiAwareNetworkSpecifier) nnriE.getKey();
        AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) nnriE.getValue();
        if (nnri.state != 101) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onDataPathConfirm: invalid state=");
            stringBuilder.append(nnri.state);
            Log.w(str, stringBuilder.toString());
            this.mNetworkRequestsCache.remove(networkSpecifier);
            if (accept) {
                this.mMgr.endDataPath(i);
            }
            return networkSpecifier;
        }
        WifiAwareNetworkSpecifier networkSpecifier2;
        AwareNetworkRequestInformation nnri2;
        if (accept) {
            nnri.state = 102;
            nnri.peerDataMac = mac;
            nnri.channelInfo = channelInfo;
            NetworkInfo networkInfo = new NetworkInfo(-1, 0, NETWORK_TAG, "");
            NetworkCapabilities networkCapabilities = new NetworkCapabilities(sNetworkCapabilitiesFilter);
            LinkProperties linkProperties = new LinkProperties();
            if (!isInterfaceUpAndUsedByAnotherNdp(nnri)) {
                try {
                    this.mNwService.setInterfaceUp(nnri.interfaceName);
                    this.mNwService.enableIpv6(nnri.interfaceName);
                } catch (Exception e) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("onDataPathConfirm: ACCEPT nnri=");
                    stringBuilder2.append(nnri);
                    stringBuilder2.append(": can't configure network - ");
                    stringBuilder2.append(e);
                    Log.e(str2, stringBuilder2.toString());
                    this.mMgr.endDataPath(i);
                    nnri.state = StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                    return networkSpecifier;
                }
            }
            if (!this.mNiWrapper.configureAgentProperties(nnri, nnri.equivalentSpecifiers, i, networkInfo, networkCapabilities, linkProperties)) {
                return networkSpecifier;
            }
            Looper looper = this.mLooper;
            Context context = this.mContext;
            stringBuilder = new StringBuilder();
            stringBuilder.append(AGENT_TAG_PREFIX);
            stringBuilder.append(nnri.ndpId);
            String stringBuilder3 = stringBuilder.toString();
            NetworkInfo networkInfo2 = networkInfo;
            NetworkInfo networkInfo3 = new NetworkInfo(-1, 0, NETWORK_TAG, "");
            NetworkInfo networkInfo4 = networkInfo2;
            Looper looper2 = looper;
            int i2 = 0;
            nnri2 = nnri;
            networkSpecifier2 = networkSpecifier;
            nnri2.networkAgent = new WifiAwareNetworkAgent(this, looper2, context, stringBuilder3, networkInfo3, networkCapabilities, linkProperties, 1, nnri2);
            nnri2.networkAgent.sendNetworkInfo(networkInfo4);
            this.mAwareMetrics.recordNdpStatus(i2, networkSpecifier2.isOutOfBand(), nnri2.startTimestamp);
            nnri2.startTimestamp = SystemClock.elapsedRealtime();
            this.mAwareMetrics.recordNdpCreation(nnri2.uid, this.mNetworkRequestsCache);
            int i3 = reason;
        } else {
            nnri2 = nnri;
            networkSpecifier2 = networkSpecifier;
            Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> entry = nnriE;
            this.mNetworkRequestsCache.remove(networkSpecifier2);
            this.mAwareMetrics.recordNdpStatus(reason, networkSpecifier2.isOutOfBand(), nnri2.startTimestamp);
        }
        return networkSpecifier2;
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

    public void onDataPathSchedUpdate(byte[] peerMac, List<Integer> ndpIds, List<NanDataPathChannelInfo> channelInfo) {
        for (Integer ndpId : ndpIds) {
            int ndpId2 = ndpId.intValue();
            Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> nnriE = getNetworkRequestByNdpId(ndpId2);
            String str;
            StringBuilder stringBuilder;
            if (nnriE == null) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onDataPathSchedUpdate: ndpId=");
                stringBuilder.append(ndpId2);
                stringBuilder.append(" - not found");
                Log.e(str, stringBuilder.toString());
            } else if (Arrays.equals(peerMac, ((AwareNetworkRequestInformation) nnriE.getValue()).peerDiscoveryMac)) {
                ((AwareNetworkRequestInformation) nnriE.getValue()).channelInfo = channelInfo;
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onDataPathSchedUpdate: ndpId=");
                stringBuilder.append(ndpId2);
                stringBuilder.append(", report NMI=");
                stringBuilder.append(MacAddress.fromBytes(peerMac).toString());
                stringBuilder.append(" doesn't match NDP NMI=");
                stringBuilder.append(MacAddress.fromBytes(((AwareNetworkRequestInformation) nnriE.getValue()).peerDiscoveryMac).toString());
                Log.e(str, stringBuilder.toString());
            }
        }
    }

    public void onAwareDownCleanupDataPaths() {
        Iterator<Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation>> it = this.mNetworkRequestsCache.entrySet().iterator();
        while (it.hasNext()) {
            tearDownInterfaceIfPossible((AwareNetworkRequestInformation) ((Entry) it.next()).getValue());
            it.remove();
        }
    }

    public void handleDataPathTimeout(NetworkSpecifier networkSpecifier) {
        if (this.mDbg) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handleDataPathTimeout: networkSpecifier=");
            stringBuilder.append(networkSpecifier);
            Log.v(str, stringBuilder.toString());
        }
        AwareNetworkRequestInformation nnri = (AwareNetworkRequestInformation) this.mNetworkRequestsCache.remove(networkSpecifier);
        if (nnri == null) {
            if (this.mDbg) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("handleDataPathTimeout: network request not found for networkSpecifier=");
                stringBuilder2.append(networkSpecifier);
                Log.v(str2, stringBuilder2.toString());
            }
            return;
        }
        this.mAwareMetrics.recordNdpStatus(1, nnri.networkSpecifier.isOutOfBand(), nnri.startTimestamp);
        this.mMgr.endDataPath(nnri.ndpId);
        nnri.state = StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
    }

    private void tearDownInterfaceIfPossible(AwareNetworkRequestInformation nnri) {
        if (!(TextUtils.isEmpty(nnri.interfaceName) || isInterfaceUpAndUsedByAnotherNdp(nnri))) {
            try {
                this.mNwService.setInterfaceDown(nnri.interfaceName);
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("tearDownInterfaceIfPossible: nnri=");
                stringBuilder.append(nnri);
                stringBuilder.append(": can't bring interface down - ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
            }
        }
        if (nnri.networkAgent != null) {
            nnri.networkAgent.reconfigureAgentAsDisconnected();
        }
    }

    private boolean isInterfaceUpAndUsedByAnotherNdp(AwareNetworkRequestInformation nri) {
        for (AwareNetworkRequestInformation lnri : this.mNetworkRequestsCache.values()) {
            if (lnri != nri) {
                if (nri.interfaceName.equals(lnri.interfaceName) && (lnri.state == 102 || lnri.state == StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB)) {
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
            if (nnri != req) {
                if (Arrays.equals(req.peerDiscoveryMac, nnri.peerDiscoveryMac)) {
                    used.add(nnri.interfaceName);
                }
            }
        }
        for (String ifName : potential) {
            if (!used.contains(ifName)) {
                return ifName;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("selectInterfaceForRequest: req=");
        stringBuilder.append(req);
        stringBuilder.append(" - no interfaces available!");
        Log.e(str, stringBuilder.toString());
        return null;
    }

    private int selectChannelForRequest(AwareNetworkRequestInformation req) {
        return 2437;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("WifiAwareDataPathStateManager:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mInterfaces: ");
        stringBuilder.append(this.mInterfaces);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  sNetworkCapabilitiesFilter: ");
        stringBuilder.append(sNetworkCapabilitiesFilter);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mNetworkRequestsCache: ");
        stringBuilder.append(this.mNetworkRequestsCache);
        pw.println(stringBuilder.toString());
        pw.println("  mNetworkFactory:");
        this.mNetworkFactory.dump(fd, pw, args);
    }
}
