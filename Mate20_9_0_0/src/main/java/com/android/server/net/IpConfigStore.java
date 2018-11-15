package com.android.server.net;

import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Iterator;

public class IpConfigStore {
    private static final boolean DBG = false;
    protected static final String DNS_KEY = "dns";
    protected static final String EOS = "eos";
    protected static final String EXCLUSION_LIST_KEY = "exclusionList";
    protected static final String GATEWAY_KEY = "gateway";
    protected static final String ID_KEY = "id";
    protected static final int IPCONFIG_FILE_VERSION = 3;
    protected static final String IP_ASSIGNMENT_KEY = "ipAssignment";
    protected static final String LINK_ADDRESS_KEY = "linkAddress";
    protected static final String PROXY_HOST_KEY = "proxyHost";
    protected static final String PROXY_PAC_FILE = "proxyPac";
    protected static final String PROXY_PORT_KEY = "proxyPort";
    protected static final String PROXY_SETTINGS_KEY = "proxySettings";
    private static final String TAG = "IpConfigStore";
    protected final DelayedDiskWrite mWriter;

    /* renamed from: com.android.server.net.IpConfigStore$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$net$IpConfiguration$IpAssignment = new int[IpAssignment.values().length];
        static final /* synthetic */ int[] $SwitchMap$android$net$IpConfiguration$ProxySettings = new int[ProxySettings.values().length];

        static {
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[ProxySettings.STATIC.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[ProxySettings.PAC.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[ProxySettings.NONE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$ProxySettings[ProxySettings.UNASSIGNED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpAssignment.STATIC.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpAssignment.DHCP.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$net$IpConfiguration$IpAssignment[IpAssignment.UNASSIGNED.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    public IpConfigStore(DelayedDiskWrite writer) {
        this.mWriter = writer;
    }

    public IpConfigStore() {
        this(new DelayedDiskWrite());
    }

    private static boolean writeConfig(DataOutputStream out, String configKey, IpConfiguration config) throws IOException {
        return writeConfig(out, configKey, config, 3);
    }

    @VisibleForTesting
    public static boolean writeConfig(DataOutputStream out, String configKey, IpConfiguration config, int version) throws IOException {
        boolean written = false;
        try {
            switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$IpAssignment[config.ipAssignment.ordinal()]) {
                case 1:
                    out.writeUTF(IP_ASSIGNMENT_KEY);
                    out.writeUTF(config.ipAssignment.toString());
                    StaticIpConfiguration staticIpConfiguration = config.staticIpConfiguration;
                    if (staticIpConfiguration != null) {
                        if (staticIpConfiguration.ipAddress != null) {
                            LinkAddress ipAddress = staticIpConfiguration.ipAddress;
                            out.writeUTF(LINK_ADDRESS_KEY);
                            out.writeUTF(ipAddress.getAddress().getHostAddress());
                            out.writeInt(ipAddress.getPrefixLength());
                        }
                        if (staticIpConfiguration.gateway != null) {
                            out.writeUTF(GATEWAY_KEY);
                            out.writeInt(0);
                            out.writeInt(1);
                            out.writeUTF(staticIpConfiguration.gateway.getHostAddress());
                        }
                        Iterator it = staticIpConfiguration.dnsServers.iterator();
                        while (it.hasNext()) {
                            InetAddress inetAddr = (InetAddress) it.next();
                            out.writeUTF(DNS_KEY);
                            out.writeUTF(inetAddr.getHostAddress());
                        }
                    }
                    written = true;
                    break;
                case 2:
                    out.writeUTF(IP_ASSIGNMENT_KEY);
                    out.writeUTF(config.ipAssignment.toString());
                    written = true;
                    break;
                case 3:
                    break;
                default:
                    loge("Ignore invalid ip assignment while writing");
                    break;
            }
            ProxyInfo proxyProperties;
            switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$ProxySettings[config.proxySettings.ordinal()]) {
                case 1:
                    proxyProperties = config.httpProxy;
                    String exclusionList = proxyProperties.getExclusionListAsString();
                    out.writeUTF(PROXY_SETTINGS_KEY);
                    out.writeUTF(config.proxySettings.toString());
                    out.writeUTF(PROXY_HOST_KEY);
                    out.writeUTF(proxyProperties.getHost());
                    out.writeUTF(PROXY_PORT_KEY);
                    out.writeInt(proxyProperties.getPort());
                    if (exclusionList != null) {
                        out.writeUTF(EXCLUSION_LIST_KEY);
                        out.writeUTF(exclusionList);
                    }
                    written = true;
                    break;
                case 2:
                    proxyProperties = config.httpProxy;
                    out.writeUTF(PROXY_SETTINGS_KEY);
                    out.writeUTF(config.proxySettings.toString());
                    out.writeUTF(PROXY_PAC_FILE);
                    out.writeUTF(proxyProperties.getPacFileUrl().toString());
                    written = true;
                    break;
                case 3:
                    out.writeUTF(PROXY_SETTINGS_KEY);
                    out.writeUTF(config.proxySettings.toString());
                    written = true;
                    break;
                case 4:
                    break;
                default:
                    loge("Ignore invalid proxy settings while writing");
                    break;
            }
            if (written) {
                out.writeUTF(ID_KEY);
                if (version < 3) {
                    out.writeInt(Integer.valueOf(configKey).intValue());
                } else {
                    out.writeUTF(configKey);
                }
            }
        } catch (NullPointerException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failure in writing ");
            stringBuilder.append(config);
            stringBuilder.append(e);
            loge(stringBuilder.toString());
        }
        out.writeUTF(EOS);
        return written;
    }

    @Deprecated
    public void writeIpAndProxyConfigurationsToFile(String filePath, SparseArray<IpConfiguration> networks) {
        this.mWriter.write(filePath, new -$$Lambda$IpConfigStore$O2tmBZ0pfEt3xGZYo5ZrQq4edzM(networks));
    }

    static /* synthetic */ void lambda$writeIpAndProxyConfigurationsToFile$0(SparseArray networks, DataOutputStream out) throws IOException {
        out.writeInt(3);
        for (int i = 0; i < networks.size(); i++) {
            writeConfig(out, String.valueOf(networks.keyAt(i)), (IpConfiguration) networks.valueAt(i));
        }
    }

    public void writeIpConfigurations(String filePath, ArrayMap<String, IpConfiguration> networks) {
        this.mWriter.write(filePath, new -$$Lambda$IpConfigStore$rFY3yG3j6RGRgrQey7yYfi0Yze0(networks));
    }

    static /* synthetic */ void lambda$writeIpConfigurations$1(ArrayMap networks, DataOutputStream out) throws IOException {
        out.writeInt(3);
        for (int i = 0; i < networks.size(); i++) {
            writeConfig(out, (String) networks.keyAt(i), (IpConfiguration) networks.valueAt(i));
        }
    }

    public static ArrayMap<String, IpConfiguration> readIpConfigurations(String filePath) {
        try {
            return readIpConfigurations(new BufferedInputStream(new FileInputStream(filePath)));
        } catch (FileNotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error opening configuration file: ");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            return new ArrayMap(0);
        }
    }

    @Deprecated
    public static SparseArray<IpConfiguration> readIpAndProxyConfigurations(String filePath) {
        try {
            return readIpAndProxyConfigurations(new BufferedInputStream(new FileInputStream(filePath)));
        } catch (FileNotFoundException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error opening configuration file: ");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
            return new SparseArray();
        }
    }

    @Deprecated
    public static SparseArray<IpConfiguration> readIpAndProxyConfigurations(InputStream inputStream) {
        ArrayMap<String, IpConfiguration> networks = readIpConfigurations(inputStream);
        if (networks == null) {
            return null;
        }
        SparseArray<IpConfiguration> networksById = new SparseArray();
        for (int i = 0; i < networks.size(); i++) {
            networksById.put(Integer.valueOf((String) networks.keyAt(i)).intValue(), (IpConfiguration) networks.valueAt(i));
        }
        return networksById;
    }

    /* JADX WARNING: Missing block: B:112:0x027e, code:
            if (r3 == null) goto L_0x0292;
     */
    /* JADX WARNING: Missing block: B:114:?, code:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:122:0x028f, code:
            if (r3 == null) goto L_0x0292;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ArrayMap<String, IpConfiguration> readIpConfigurations(InputStream inputStream) {
        IllegalArgumentException e;
        ArrayMap<String, IpConfiguration> networks = new ArrayMap();
        ArrayMap<String, IpConfiguration> arrayMap = null;
        DataInputStream in = null;
        try {
            in = new DataInputStream(inputStream);
            int version = in.readInt();
            int i = 3;
            if (version == 3 || version == 2 || version == 1) {
                while (true) {
                    IpAssignment ipAssignment = IpAssignment.DHCP;
                    ProxySettings proxySettings = ProxySettings.NONE;
                    StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
                    int proxyPort = -1;
                    String pacFileUrl = null;
                    String proxyHost = null;
                    ProxySettings proxySettings2 = proxySettings;
                    IpAssignment ipAssignment2 = ipAssignment;
                    String uniqueToken = null;
                    String exclusionList = arrayMap;
                    while (true) {
                        InputStream inputStream2;
                        String exclusionList2 = exclusionList;
                        String key = in.readUTF();
                        String key2;
                        try {
                            key2 = key;
                            try {
                                LinkAddress linkAddr;
                                if (key2.equals(ID_KEY)) {
                                    if (version < i) {
                                        exclusionList = String.valueOf(in.readInt());
                                    } else {
                                        exclusionList = in.readUTF();
                                    }
                                    uniqueToken = exclusionList;
                                } else if (key2.equals(IP_ASSIGNMENT_KEY)) {
                                    ipAssignment2 = IpAssignment.valueOf(in.readUTF());
                                } else if (key2.equals(LINK_ADDRESS_KEY)) {
                                    linkAddr = new LinkAddress(NetworkUtils.numericToInetAddress(in.readUTF()), in.readInt());
                                    if ((linkAddr.getAddress() instanceof Inet4Address) && staticIpConfiguration.ipAddress == null) {
                                        staticIpConfiguration.ipAddress = linkAddr;
                                    } else {
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Non-IPv4 or duplicate address: ");
                                        stringBuilder.append(linkAddr);
                                        loge(stringBuilder.toString());
                                    }
                                } else if (key2.equals(GATEWAY_KEY)) {
                                    InetAddress gateway = null;
                                    StringBuilder stringBuilder2;
                                    LinkAddress dest;
                                    if (version == 1) {
                                        gateway = NetworkUtils.numericToInetAddress(in.readUTF());
                                        if (staticIpConfiguration.gateway == null) {
                                            staticIpConfiguration.gateway = gateway;
                                        } else {
                                            stringBuilder2 = new StringBuilder();
                                            dest = null;
                                            stringBuilder2.append("Duplicate gateway: ");
                                            stringBuilder2.append(gateway.getHostAddress());
                                            loge(stringBuilder2.toString());
                                        }
                                    } else {
                                        dest = null;
                                        if (in.readInt() == 1) {
                                            linkAddr = new LinkAddress(NetworkUtils.numericToInetAddress(in.readUTF()), in.readInt());
                                        } else {
                                            linkAddr = dest;
                                        }
                                        if (in.readInt() == 1) {
                                            gateway = NetworkUtils.numericToInetAddress(in.readUTF());
                                        }
                                        RouteInfo route = new RouteInfo(linkAddr, gateway);
                                        if (route.isIPv4Default() && staticIpConfiguration.gateway == null) {
                                            staticIpConfiguration.gateway = gateway;
                                        } else {
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append("Non-IPv4 default or duplicate route: ");
                                            stringBuilder2.append(route);
                                            loge(stringBuilder2.toString());
                                        }
                                    }
                                } else if (key2.equals(DNS_KEY)) {
                                    staticIpConfiguration.dnsServers.add(NetworkUtils.numericToInetAddress(in.readUTF()));
                                } else if (key2.equals(PROXY_SETTINGS_KEY)) {
                                    proxySettings2 = ProxySettings.valueOf(in.readUTF());
                                } else if (key2.equals(PROXY_HOST_KEY)) {
                                    proxyHost = in.readUTF();
                                } else if (key2.equals(PROXY_PORT_KEY)) {
                                    proxyPort = in.readInt();
                                } else if (key2.equals(PROXY_PAC_FILE)) {
                                    pacFileUrl = in.readUTF();
                                } else if (key2.equals(EXCLUSION_LIST_KEY)) {
                                    exclusionList = in.readUTF();
                                    inputStream2 = inputStream;
                                    i = 3;
                                } else if (key2.equals(EOS)) {
                                    if (uniqueToken != null) {
                                        IpConfiguration config = new IpConfiguration();
                                        networks.put(uniqueToken, config);
                                        switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$IpAssignment[ipAssignment2.ordinal()]) {
                                            case 1:
                                                config.staticIpConfiguration = staticIpConfiguration;
                                                config.ipAssignment = ipAssignment2;
                                                break;
                                            case 2:
                                                config.ipAssignment = ipAssignment2;
                                                break;
                                            case 3:
                                                loge("BUG: Found UNASSIGNED IP on file, use DHCP");
                                                config.ipAssignment = IpAssignment.DHCP;
                                                break;
                                            default:
                                                loge("Ignore invalid ip assignment while reading.");
                                                config.ipAssignment = IpAssignment.UNASSIGNED;
                                                break;
                                        }
                                        ProxyInfo proxyInfo;
                                        switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$ProxySettings[proxySettings2.ordinal()]) {
                                            case 1:
                                                proxyInfo = new ProxyInfo(proxyHost, proxyPort, exclusionList2);
                                                config.proxySettings = proxySettings2;
                                                config.httpProxy = proxyInfo;
                                                break;
                                            case 2:
                                                proxyInfo = new ProxyInfo(pacFileUrl);
                                                config.proxySettings = proxySettings2;
                                                config.httpProxy = proxyInfo;
                                                break;
                                            case 3:
                                                config.proxySettings = proxySettings2;
                                                break;
                                            case 4:
                                                loge("BUG: Found UNASSIGNED proxy on file, use NONE");
                                                config.proxySettings = ProxySettings.NONE;
                                                break;
                                            default:
                                                loge("Ignore invalid proxy settings while reading");
                                                config.proxySettings = ProxySettings.UNASSIGNED;
                                                break;
                                        }
                                    }
                                    arrayMap = null;
                                    inputStream2 = inputStream;
                                    i = 3;
                                } else {
                                    exclusionList = new StringBuilder();
                                    exclusionList.append("Ignore unknown key ");
                                    exclusionList.append(key2);
                                    exclusionList.append("while reading");
                                    loge(exclusionList.toString());
                                }
                                exclusionList = exclusionList2;
                            } catch (IllegalArgumentException e2) {
                                e = e2;
                            }
                        } catch (IllegalArgumentException e3) {
                            e = e3;
                            key2 = key;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Ignore invalid address while reading");
                            stringBuilder3.append(e);
                            loge(stringBuilder3.toString());
                            exclusionList = exclusionList2;
                            inputStream2 = inputStream;
                            i = 3;
                        }
                        inputStream2 = inputStream;
                        i = 3;
                    }
                }
            } else {
                loge("Bad version on IP configuration file, ignore read");
                try {
                    in.close();
                } catch (Exception e4) {
                }
                return null;
            }
        } catch (EOFException e5) {
        } catch (IOException e6) {
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Error parsing configuration: ");
            stringBuilder4.append(e6);
            loge(stringBuilder4.toString());
        } catch (Throwable th) {
            Throwable th2 = th;
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e7) {
                }
            }
        }
        return networks;
    }

    protected static void loge(String s) {
        Log.e(TAG, s);
    }

    protected static void log(String s) {
        Log.d(TAG, s);
    }
}
