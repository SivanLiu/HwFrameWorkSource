package com.android.server.ethernet;

import android.content.Context;
import android.net.IEthernetServiceListener;
import android.net.InterfaceConfiguration;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.NetworkCapabilities;
import android.net.StaticIpConfiguration;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.INetworkManagementService.Stub;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.net.BaseNetworkObserver;
import java.io.FileDescriptor;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

final class EthernetTracker {
    private static final boolean DBG = true;
    private static final String TAG = EthernetTracker.class.getSimpleName();
    private final EthernetConfigStore mConfigStore;
    private final EthernetNetworkFactory mFactory;
    private final Handler mHandler;
    private final String mIfaceMatch;
    private volatile IpConfiguration mIpConfigForDefaultInterface;
    private final ConcurrentHashMap<String, IpConfiguration> mIpConfigurations = new ConcurrentHashMap();
    private final RemoteCallbackList<IEthernetServiceListener> mListeners = new RemoteCallbackList();
    private final INetworkManagementService mNMService;
    private final ConcurrentHashMap<String, NetworkCapabilities> mNetworkCapabilities = new ConcurrentHashMap();

    private class InterfaceObserver extends BaseNetworkObserver {
        private InterfaceObserver() {
        }

        public void interfaceLinkStateChanged(String iface, boolean up) {
            String access$100 = EthernetTracker.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("interfaceLinkStateChanged, iface: ");
            stringBuilder.append(iface);
            stringBuilder.append(", up: ");
            stringBuilder.append(up);
            Log.i(access$100, stringBuilder.toString());
            EthernetTracker.this.mHandler.post(new -$$Lambda$EthernetTracker$InterfaceObserver$RwJV-Ek3mzxwZq-yoQwiconpRi8(this, iface, up));
        }

        public void interfaceAdded(String iface) {
            EthernetTracker.this.mHandler.post(new -$$Lambda$EthernetTracker$InterfaceObserver$d1ixKZZuAxwm1Dz_AX3HmL4JVLA(this, iface));
        }

        public void interfaceRemoved(String iface) {
            EthernetTracker.this.mHandler.post(new -$$Lambda$EthernetTracker$InterfaceObserver$N47vO7QrVbS59gsxVAc8Mt2Opco(this, iface));
        }
    }

    private static class ListenerInfo {
        boolean canUseRestrictedNetworks = false;

        ListenerInfo(boolean canUseRestrictedNetworks) {
            this.canUseRestrictedNetworks = canUseRestrictedNetworks;
        }
    }

    EthernetTracker(Context context, Handler handler) {
        this.mHandler = handler;
        this.mNMService = Stub.asInterface(ServiceManager.getService("network_management"));
        this.mIfaceMatch = context.getResources().getString(17039802);
        for (String strConfig : context.getResources().getStringArray(17236009)) {
            parseEthernetConfig(strConfig);
        }
        this.mConfigStore = new EthernetConfigStore();
        this.mFactory = new EthernetNetworkFactory(handler, context, createNetworkCapabilities(DBG));
        this.mFactory.register();
    }

    void start() {
        this.mConfigStore.read();
        this.mIpConfigForDefaultInterface = this.mConfigStore.getIpConfigurationForDefaultInterface();
        ArrayMap<String, IpConfiguration> configs = this.mConfigStore.getIpConfigurations();
        for (int i = 0; i < configs.size(); i++) {
            this.mIpConfigurations.put((String) configs.keyAt(i), (IpConfiguration) configs.valueAt(i));
        }
        try {
            this.mNMService.registerObserver(new InterfaceObserver());
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Could not register InterfaceObserver ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
        this.mHandler.post(new -$$Lambda$EthernetTracker$7ZSuSvoSqcExye5DLwv_gyq6gyM(this));
    }

    void updateIpConfiguration(String iface, IpConfiguration ipConfiguration) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateIpConfiguration, iface: ");
        stringBuilder.append(iface);
        stringBuilder.append(", cfg: ");
        stringBuilder.append(ipConfiguration);
        Log.i(str, stringBuilder.toString());
        this.mConfigStore.write(iface, ipConfiguration);
        this.mIpConfigurations.put(iface, ipConfiguration);
        this.mHandler.post(new -$$Lambda$EthernetTracker$WrfGoZ0jmrS_2ZYW4ZE33ZnJcBI(this, iface, ipConfiguration));
    }

    IpConfiguration getIpConfiguration(String iface) {
        return (IpConfiguration) this.mIpConfigurations.get(iface);
    }

    boolean isTrackingInterface(String iface) {
        return this.mFactory.hasInterface(iface);
    }

    String[] getInterfaces(boolean includeRestricted) {
        return this.mFactory.getAvailableInterfaces(includeRestricted);
    }

    boolean isRestrictedInterface(String iface) {
        NetworkCapabilities nc = (NetworkCapabilities) this.mNetworkCapabilities.get(iface);
        return (nc == null || nc.hasCapability(13)) ? false : DBG;
    }

    void addListener(IEthernetServiceListener listener, boolean canUseRestrictedNetworks) {
        this.mListeners.register(listener, new ListenerInfo(canUseRestrictedNetworks));
    }

    void removeListener(IEthernetServiceListener listener) {
        this.mListeners.unregister(listener);
    }

    private void removeInterface(String iface) {
        this.mFactory.removeInterface(iface);
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x000e A:{ExcHandler: android.os.RemoteException (r1_3 'e' java.lang.Exception), Splitter: B:1:0x0001} */
    /* JADX WARNING: Missing block: B:4:0x000e, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x000f, code:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Error upping interface ");
            r3.append(r8);
            android.util.Log.e(r2, r3.toString(), r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void addInterface(String iface) {
        InterfaceConfiguration config = null;
        try {
            this.mNMService.setInterfaceUp(iface);
            config = this.mNMService.getInterfaceConfig(iface);
        } catch (Exception e) {
        }
        String str;
        if (config == null) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Null interface config for ");
            stringBuilder.append(iface);
            stringBuilder.append(". Bailing out.");
            Log.e(str, stringBuilder.toString());
            return;
        }
        str = config.getHardwareAddress();
        NetworkCapabilities nc = (NetworkCapabilities) this.mNetworkCapabilities.get(iface);
        if (nc == null) {
            nc = (NetworkCapabilities) this.mNetworkCapabilities.get(str);
            if (nc == null) {
                nc = createDefaultNetworkCapabilities();
            }
        }
        IpConfiguration ipConfiguration = (IpConfiguration) this.mIpConfigurations.get(iface);
        if (ipConfiguration == null) {
            ipConfiguration = createDefaultIpConfiguration();
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Started tracking interface ");
        stringBuilder2.append(iface);
        Log.d(str2, stringBuilder2.toString());
        this.mFactory.addInterface(iface, str, nc, ipConfiguration);
        if (config.hasFlag("running")) {
            updateInterfaceState(iface, DBG);
        }
    }

    private void updateInterfaceState(String iface, boolean up) {
        if (this.mFactory.updateInterfaceLinkState(iface, up)) {
            boolean restricted = isRestrictedInterface(iface);
            int n = this.mListeners.beginBroadcast();
            for (int i = 0; i < n; i++) {
                if (restricted) {
                    try {
                        if (!((ListenerInfo) this.mListeners.getBroadcastCookie(i)).canUseRestrictedNetworks) {
                        }
                    } catch (RemoteException e) {
                    }
                }
                ((IEthernetServiceListener) this.mListeners.getBroadcastItem(i)).onAvailabilityChanged(iface, up);
            }
            this.mListeners.finishBroadcast();
        }
    }

    private void maybeTrackInterface(String iface) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("maybeTrackInterface ");
        stringBuilder.append(iface);
        Log.i(str, stringBuilder.toString());
        if (iface.matches(this.mIfaceMatch) && !this.mFactory.hasInterface(iface)) {
            if (this.mIpConfigForDefaultInterface != null) {
                updateIpConfiguration(iface, this.mIpConfigForDefaultInterface);
                this.mIpConfigForDefaultInterface = null;
            }
            addInterface(iface);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:5:0x0013 A:{ExcHandler: android.os.RemoteException (r0_2 'e' java.lang.Exception), Splitter: B:0:0x0000} */
    /* JADX WARNING: Missing block: B:5:0x0013, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:6:0x0014, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Could not get list of interfaces ");
            r2.append(r0);
            android.util.Log.e(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:8:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void trackAvailableInterfaces() {
        try {
            for (String iface : this.mNMService.listInterfaces()) {
                maybeTrackInterface(iface);
            }
        } catch (Exception e) {
        }
    }

    private void parseEthernetConfig(String configString) {
        String[] tokens = configString.split(";");
        String name = tokens[null];
        String capabilities = tokens.length > 1 ? tokens[1] : null;
        this.mNetworkCapabilities.put(name, createNetworkCapabilities(1 ^ TextUtils.isEmpty(capabilities), capabilities));
        if (tokens.length > 2 && !TextUtils.isEmpty(tokens[2])) {
            this.mIpConfigurations.put(name, parseStaticIpConfiguration(tokens[2]));
        }
    }

    private static NetworkCapabilities createDefaultNetworkCapabilities() {
        NetworkCapabilities nc = createNetworkCapabilities(null);
        nc.addCapability(12);
        nc.addCapability(13);
        nc.addCapability(11);
        nc.addCapability(18);
        nc.addCapability(20);
        return nc;
    }

    private static NetworkCapabilities createNetworkCapabilities(boolean clearDefaultCapabilities) {
        return createNetworkCapabilities(clearDefaultCapabilities, null);
    }

    private static NetworkCapabilities createNetworkCapabilities(boolean clearDefaultCapabilities, String commaSeparatedCapabilities) {
        NetworkCapabilities nc = new NetworkCapabilities();
        if (clearDefaultCapabilities) {
            nc.clearAll();
        }
        nc.addTransportType(3);
        nc.setLinkUpstreamBandwidthKbps(100000);
        nc.setLinkDownstreamBandwidthKbps(100000);
        if (!TextUtils.isEmpty(commaSeparatedCapabilities)) {
            for (String strNetworkCapability : commaSeparatedCapabilities.split(",")) {
                if (!TextUtils.isEmpty(strNetworkCapability)) {
                    nc.addCapability(Integer.valueOf(strNetworkCapability).intValue());
                }
            }
        }
        return nc;
    }

    /* JADX WARNING: Removed duplicated region for block: B:43:0x0071 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00bb  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00b8  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x00b1  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0090  */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x0071 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00bb  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00b8  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x00b1  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0090  */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x0071 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x00bb  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00b8  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x00b1  */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x0090  */
    /* JADX WARNING: Missing block: B:24:0x006a, code:
            if (r7.equals("gateway") != false) goto L_0x006e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting
    static IpConfiguration parseStaticIpConfiguration(String staticIpConfig) {
        StaticIpConfiguration ipConfig = new StaticIpConfiguration();
        for (String keyValueAsString : staticIpConfig.trim().split(" ")) {
            if (!TextUtils.isEmpty(keyValueAsString)) {
                String[] pair = keyValueAsString.split("=");
                int i = 2;
                StringBuilder stringBuilder;
                if (pair.length == 2) {
                    String key = pair[0];
                    String value = pair[1];
                    int hashCode = key.hashCode();
                    if (hashCode != -189118908) {
                        if (hashCode == 3367) {
                            if (key.equals("ip")) {
                                i = 0;
                                switch (i) {
                                    case 0:
                                        break;
                                    case 1:
                                        break;
                                    case 2:
                                        break;
                                    case 3:
                                        break;
                                    default:
                                        break;
                                }
                            }
                        } else if (hashCode == 99625) {
                            if (key.equals("dns")) {
                                i = 3;
                                switch (i) {
                                    case 0:
                                        break;
                                    case 1:
                                        break;
                                    case 2:
                                        break;
                                    case 3:
                                        break;
                                    default:
                                        break;
                                }
                            }
                        } else if (hashCode == 1837548591 && key.equals("domains")) {
                            i = 1;
                            switch (i) {
                                case 0:
                                    ipConfig.ipAddress = new LinkAddress(value);
                                    break;
                                case 1:
                                    ipConfig.domains = value;
                                    break;
                                case 2:
                                    ipConfig.gateway = InetAddress.parseNumericAddress(value);
                                    break;
                                case 3:
                                    ArrayList<InetAddress> dnsAddresses = new ArrayList();
                                    for (String address : value.split(",")) {
                                        dnsAddresses.add(InetAddress.parseNumericAddress(address));
                                    }
                                    ipConfig.dnsServers.addAll(dnsAddresses);
                                    break;
                                default:
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Unexpected key: ");
                                    stringBuilder.append(key);
                                    stringBuilder.append(" in ");
                                    stringBuilder.append(staticIpConfig);
                                    throw new IllegalArgumentException(stringBuilder.toString());
                            }
                        }
                    }
                    i = -1;
                    switch (i) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            break;
                        default:
                            break;
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected token: ");
                stringBuilder.append(keyValueAsString);
                stringBuilder.append(" in ");
                stringBuilder.append(staticIpConfig);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return new IpConfiguration(IpAssignment.STATIC, ProxySettings.NONE, ipConfig, null);
    }

    private static IpConfiguration createDefaultIpConfiguration() {
        return new IpConfiguration(IpAssignment.DHCP, ProxySettings.NONE, null, null);
    }

    private void postAndWaitForRunnable(Runnable r) {
        this.mHandler.runWithScissors(r, 2000);
    }

    void dump(FileDescriptor fd, IndentingPrintWriter pw, String[] args) {
        postAndWaitForRunnable(new -$$Lambda$EthernetTracker$rMvwcG7iXM6tWTHAeE-h6PlZCT8(this, pw, fd, args));
    }

    public static /* synthetic */ void lambda$dump$1(EthernetTracker ethernetTracker, IndentingPrintWriter pw, FileDescriptor fd, String[] args) {
        StringBuilder stringBuilder;
        pw.println(ethernetTracker.getClass().getSimpleName());
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Ethernet interface name filter: ");
        stringBuilder2.append(ethernetTracker.mIfaceMatch);
        pw.println(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Listeners: ");
        stringBuilder2.append(ethernetTracker.mListeners.getRegisteredCallbackCount());
        pw.println(stringBuilder2.toString());
        pw.println("IP Configurations:");
        pw.increaseIndent();
        for (String iface : ethernetTracker.mIpConfigurations.keySet()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(iface);
            stringBuilder.append(": ");
            stringBuilder.append(ethernetTracker.mIpConfigurations.get(iface));
            pw.println(stringBuilder.toString());
        }
        pw.decreaseIndent();
        pw.println();
        pw.println("Network Capabilities:");
        pw.increaseIndent();
        for (String iface2 : ethernetTracker.mNetworkCapabilities.keySet()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(iface2);
            stringBuilder.append(": ");
            stringBuilder.append(ethernetTracker.mNetworkCapabilities.get(iface2));
            pw.println(stringBuilder.toString());
        }
        pw.decreaseIndent();
        pw.println();
        ethernetTracker.mFactory.dump(fd, pw, args);
    }
}
