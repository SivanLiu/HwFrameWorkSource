package com.android.server.ethernet;

import android.content.Context;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.StringNetworkSpecifier;
import android.net.ip.IpClient;
import android.net.ip.IpClient.Callback;
import android.net.ip.IpClient.ProvisioningConfiguration;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.util.concurrent.ConcurrentHashMap;

public class EthernetNetworkFactory extends NetworkFactory {
    static final boolean DBG = true;
    private static final int NETWORK_SCORE = 70;
    private static final String NETWORK_TYPE = "Ethernet";
    private static final String TAG = EthernetNetworkFactory.class.getSimpleName();
    private final Context mContext;
    private final Handler mHandler;
    private final ConcurrentHashMap<String, NetworkInterfaceState> mTrackingInterfaces = new ConcurrentHashMap();

    private static class NetworkInterfaceState {
        private static String sTcpBufferSizes = null;
        private final NetworkCapabilities mCapabilities;
        private final Context mContext;
        private final Handler mHandler;
        private final String mHwAddress;
        private IpClient mIpClient;
        private final Callback mIpClientCallback = new Callback() {
            public void onProvisioningSuccess(LinkProperties newLp) {
                NetworkInterfaceState.this.mHandler.post(new -$$Lambda$EthernetNetworkFactory$NetworkInterfaceState$1$9XedDO1NtZ_RFArLiXxHcePnujQ(this, newLp));
            }

            public void onProvisioningFailure(LinkProperties newLp) {
                NetworkInterfaceState.this.mHandler.post(new -$$Lambda$EthernetNetworkFactory$NetworkInterfaceState$1$f_QeN95E84S9ECYxfdEhjw7SAFc(this, newLp));
            }

            public void onLinkPropertiesChange(LinkProperties newLp) {
                NetworkInterfaceState.this.mHandler.post(new -$$Lambda$EthernetNetworkFactory$NetworkInterfaceState$1$a0284YqWC7oRla-yedW8TwdmRO0(this, newLp));
            }
        };
        private IpConfiguration mIpConfig;
        private LinkProperties mLinkProperties = new LinkProperties();
        private boolean mLinkUp;
        private NetworkAgent mNetworkAgent;
        private final NetworkInfo mNetworkInfo;
        final String name;
        long refCount = 0;

        NetworkInterfaceState(String ifaceName, String hwAddress, Handler handler, Context context, NetworkCapabilities capabilities) {
            this.name = ifaceName;
            this.mCapabilities = capabilities;
            this.mHandler = handler;
            this.mContext = context;
            this.mHwAddress = hwAddress;
            this.mNetworkInfo = new NetworkInfo(9, 0, EthernetNetworkFactory.NETWORK_TYPE, "");
            this.mNetworkInfo.setExtraInfo(this.mHwAddress);
            this.mNetworkInfo.setIsAvailable(EthernetNetworkFactory.DBG);
        }

        void setIpConfig(IpConfiguration ipConfig) {
            this.mIpConfig = ipConfig;
        }

        boolean statisified(NetworkCapabilities requestedCapabilities) {
            return requestedCapabilities.satisfiedByNetworkCapabilities(this.mCapabilities);
        }

        boolean isRestricted() {
            return this.mCapabilities.hasCapability(13);
        }

        private void start() {
            if (this.mIpClient != null) {
                Log.d(EthernetNetworkFactory.TAG, "IpClient already started");
                return;
            }
            Log.d(EthernetNetworkFactory.TAG, String.format("starting IpClient(%s): mNetworkInfo=%s", new Object[]{this.name, this.mNetworkInfo}));
            this.mNetworkInfo.setDetailedState(DetailedState.OBTAINING_IPADDR, null, this.mHwAddress);
            this.mIpClient = new IpClient(this.mContext, this.name, this.mIpClientCallback);
            if (sTcpBufferSizes == null) {
                sTcpBufferSizes = this.mContext.getResources().getString(17039803);
            }
            provisionIpClient(this.mIpClient, this.mIpConfig, sTcpBufferSizes);
        }

        void onIpLayerStarted(LinkProperties linkProperties) {
            if (this.mNetworkAgent != null) {
                Log.e(EthernetNetworkFactory.TAG, "Already have a NetworkAgent - aborting new request");
                stop();
                return;
            }
            this.mLinkProperties = linkProperties;
            this.mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, this.mHwAddress);
            this.mNetworkInfo.setIsAvailable(EthernetNetworkFactory.DBG);
            this.mNetworkAgent = new NetworkAgent(this, this.mHandler.getLooper(), this.mContext, EthernetNetworkFactory.NETWORK_TYPE, this.mNetworkInfo, this.mCapabilities, this.mLinkProperties, EthernetNetworkFactory.NETWORK_SCORE) {
                final /* synthetic */ NetworkInterfaceState this$0;

                public void unwanted() {
                    if (this == this.this$0.mNetworkAgent) {
                        this.this$0.stop();
                    } else if (this.this$0.mNetworkAgent != null) {
                        Log.d(EthernetNetworkFactory.TAG, "Ignoring unwanted as we have a more modern instance");
                    }
                }
            };
        }

        void onIpLayerStopped(LinkProperties linkProperties) {
            stop();
            start();
        }

        void updateLinkProperties(LinkProperties linkProperties) {
            this.mLinkProperties = linkProperties;
            if (this.mNetworkAgent != null) {
                this.mNetworkAgent.sendLinkProperties(linkProperties);
            }
        }

        boolean updateLinkState(boolean up) {
            if (this.mLinkUp == up) {
                return false;
            }
            this.mLinkUp = up;
            stop();
            if (up) {
                start();
            }
            return EthernetNetworkFactory.DBG;
        }

        void stop() {
            if (this.mIpClient != null) {
                this.mIpClient.shutdown();
                this.mIpClient.awaitShutdown();
                this.mIpClient = null;
            }
            this.mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, null, this.mHwAddress);
            if (this.mNetworkAgent != null) {
                updateAgent();
                this.mNetworkAgent = null;
            }
            clear();
        }

        private void updateAgent() {
            if (this.mNetworkAgent != null) {
                String access$300 = EthernetNetworkFactory.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Updating mNetworkAgent with: ");
                stringBuilder.append(this.mCapabilities);
                stringBuilder.append(", ");
                stringBuilder.append(this.mNetworkInfo);
                stringBuilder.append(", ");
                stringBuilder.append(this.mLinkProperties);
                Log.i(access$300, stringBuilder.toString());
                this.mNetworkAgent.sendNetworkCapabilities(this.mCapabilities);
                this.mNetworkAgent.sendNetworkInfo(this.mNetworkInfo);
                this.mNetworkAgent.sendLinkProperties(this.mLinkProperties);
                this.mNetworkAgent.sendNetworkScore(this.mLinkUp ? EthernetNetworkFactory.NETWORK_SCORE : 0);
            }
        }

        private void clear() {
            this.mLinkProperties.clear();
            this.mNetworkInfo.setDetailedState(DetailedState.IDLE, null, null);
            this.mNetworkInfo.setIsAvailable(false);
        }

        private static void provisionIpClient(IpClient ipClient, IpConfiguration config, String tcpBufferSizes) {
            ProvisioningConfiguration provisioningConfiguration;
            if (config.getProxySettings() == ProxySettings.STATIC || config.getProxySettings() == ProxySettings.PAC) {
                ipClient.setHttpProxy(config.getHttpProxy());
            }
            if (!TextUtils.isEmpty(tcpBufferSizes)) {
                ipClient.setTcpBufferSizes(tcpBufferSizes);
            }
            if (config.getIpAssignment() == IpAssignment.STATIC) {
                provisioningConfiguration = IpClient.buildProvisioningConfiguration().withStaticConfiguration(config.getStaticIpConfiguration()).build();
            } else {
                provisioningConfiguration = IpClient.buildProvisioningConfiguration().withProvisioningTimeoutMs(0).build();
            }
            ipClient.startProvisioning(provisioningConfiguration);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getClass().getSimpleName());
            stringBuilder.append("{ iface: ");
            stringBuilder.append(this.name);
            stringBuilder.append(", up: ");
            stringBuilder.append(this.mLinkUp);
            stringBuilder.append(", hwAddress: ");
            stringBuilder.append(this.mHwAddress);
            stringBuilder.append(", networkInfo: ");
            stringBuilder.append(this.mNetworkInfo);
            stringBuilder.append(", networkAgent: ");
            stringBuilder.append(this.mNetworkAgent);
            stringBuilder.append(", ipClient: ");
            stringBuilder.append(this.mIpClient);
            stringBuilder.append(",linkProperties: ");
            stringBuilder.append(this.mLinkProperties);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    public EthernetNetworkFactory(Handler handler, Context context, NetworkCapabilities filter) {
        super(handler.getLooper(), context, NETWORK_TYPE, filter);
        this.mHandler = handler;
        this.mContext = context;
        setScoreFilter(NETWORK_SCORE);
    }

    public boolean acceptRequest(NetworkRequest request, int score) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("acceptRequest, request: ");
        stringBuilder.append(request);
        stringBuilder.append(", score: ");
        stringBuilder.append(score);
        Log.d(str, stringBuilder.toString());
        return networkForRequest(request) != null ? DBG : false;
    }

    protected void needNetworkFor(NetworkRequest networkRequest, int score) {
        NetworkInterfaceState network = networkForRequest(networkRequest);
        if (network == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("needNetworkFor, failed to get a network for ");
            stringBuilder.append(networkRequest);
            Log.e(str, stringBuilder.toString());
            return;
        }
        long j = network.refCount + 1;
        network.refCount = j;
        if (j == 1) {
            network.start();
        }
    }

    protected void releaseNetworkFor(NetworkRequest networkRequest) {
        NetworkInterfaceState network = networkForRequest(networkRequest);
        if (network == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("needNetworkFor, failed to get a network for ");
            stringBuilder.append(networkRequest);
            Log.e(str, stringBuilder.toString());
            return;
        }
        long j = network.refCount - 1;
        network.refCount = j;
        if (j == 1) {
            network.stop();
        }
    }

    String[] getAvailableInterfaces(boolean includeRestricted) {
        return (String[]) this.mTrackingInterfaces.values().stream().filter(new -$$Lambda$EthernetNetworkFactory$b1ndnzBiSX1ihvZw7GtATwTUsto(includeRestricted)).sorted(-$$Lambda$EthernetNetworkFactory$EmftAjIay22czoGb8k_mrRGmnzg.INSTANCE).map(-$$Lambda$EthernetNetworkFactory$KXwxO15KBNVyyYS-UjD-Flm1vQ0.INSTANCE).toArray(-$$Lambda$EthernetNetworkFactory$TVQUJVMLGgbguTOK63vgn0fV1JA.INSTANCE);
    }

    static /* synthetic */ boolean lambda$getAvailableInterfaces$0(boolean includeRestricted, NetworkInterfaceState iface) {
        return (!iface.isRestricted() || includeRestricted) ? DBG : false;
    }

    static /* synthetic */ int lambda$getAvailableInterfaces$1(NetworkInterfaceState iface1, NetworkInterfaceState iface2) {
        int r = Boolean.compare(iface1.isRestricted(), iface2.isRestricted());
        return r == 0 ? iface1.name.compareTo(iface2.name) : r;
    }

    void addInterface(String ifaceName, String hwAddress, NetworkCapabilities capabilities, IpConfiguration ipConfiguration) {
        String str;
        StringBuilder stringBuilder;
        if (this.mTrackingInterfaces.containsKey(ifaceName)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Interface with name ");
            stringBuilder.append(ifaceName);
            stringBuilder.append(" already exists.");
            Log.e(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("addInterface, iface: ");
        stringBuilder.append(ifaceName);
        stringBuilder.append(", capabilities: ");
        stringBuilder.append(capabilities);
        Log.d(str, stringBuilder.toString());
        NetworkInterfaceState networkInterfaceState = new NetworkInterfaceState(ifaceName, hwAddress, this.mHandler, this.mContext, capabilities);
        networkInterfaceState.setIpConfig(ipConfiguration);
        this.mTrackingInterfaces.put(ifaceName, networkInterfaceState);
        updateCapabilityFilter();
    }

    private void updateCapabilityFilter() {
        NetworkCapabilities capabilitiesFilter = new NetworkCapabilities();
        capabilitiesFilter.clearAll();
        for (NetworkInterfaceState iface : this.mTrackingInterfaces.values()) {
            capabilitiesFilter.combineCapabilities(iface.mCapabilities);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCapabilityFilter: ");
        stringBuilder.append(capabilitiesFilter);
        Log.d(str, stringBuilder.toString());
        setCapabilityFilter(capabilitiesFilter);
    }

    void removeInterface(String interfaceName) {
        NetworkInterfaceState iface = (NetworkInterfaceState) this.mTrackingInterfaces.remove(interfaceName);
        if (iface != null) {
            iface.stop();
        }
        updateCapabilityFilter();
    }

    boolean updateInterfaceLinkState(String ifaceName, boolean up) {
        if (!this.mTrackingInterfaces.containsKey(ifaceName)) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateInterfaceLinkState, iface: ");
        stringBuilder.append(ifaceName);
        stringBuilder.append(", up: ");
        stringBuilder.append(up);
        Log.d(str, stringBuilder.toString());
        return ((NetworkInterfaceState) this.mTrackingInterfaces.get(ifaceName)).updateLinkState(up);
    }

    boolean hasInterface(String interfacName) {
        return this.mTrackingInterfaces.containsKey(interfacName);
    }

    void updateIpConfiguration(String iface, IpConfiguration ipConfiguration) {
        NetworkInterfaceState network = (NetworkInterfaceState) this.mTrackingInterfaces.get(iface);
        if (network != null) {
            network.setIpConfig(ipConfiguration);
        }
    }

    private NetworkInterfaceState networkForRequest(NetworkRequest request) {
        String requestedIface = null;
        NetworkSpecifier specifier = request.networkCapabilities.getNetworkSpecifier();
        if (specifier instanceof StringNetworkSpecifier) {
            requestedIface = ((StringNetworkSpecifier) specifier).specifier;
        }
        NetworkInterfaceState network = null;
        if (TextUtils.isEmpty(requestedIface)) {
            for (NetworkInterfaceState n : this.mTrackingInterfaces.values()) {
                if (n.statisified(request.networkCapabilities)) {
                    network = n;
                    break;
                }
            }
        } else {
            NetworkInterfaceState n2 = (NetworkInterfaceState) this.mTrackingInterfaces.get(requestedIface);
            if (n2 != null && n2.statisified(request.networkCapabilities)) {
                network = n2;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("networkForRequest, request: ");
        stringBuilder.append(request);
        stringBuilder.append(", network: ");
        stringBuilder.append(network);
        Log.i(str, stringBuilder.toString());
        return network;
    }

    void dump(FileDescriptor fd, IndentingPrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println(getClass().getSimpleName());
        pw.println("Tracking interfaces:");
        pw.increaseIndent();
        for (String iface : this.mTrackingInterfaces.keySet()) {
            NetworkInterfaceState ifaceState = (NetworkInterfaceState) this.mTrackingInterfaces.get(iface);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(iface);
            stringBuilder.append(":");
            stringBuilder.append(ifaceState);
            pw.println(stringBuilder.toString());
            pw.increaseIndent();
            IpClient ipClient = ifaceState.mIpClient;
            if (ipClient != null) {
                ipClient.dump(fd, pw, args);
            } else {
                pw.println("IpClient is null");
            }
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
    }
}
