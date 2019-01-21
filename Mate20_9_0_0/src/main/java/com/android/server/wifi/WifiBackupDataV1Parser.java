package com.android.server.wifi;

import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.GroupCipher;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.PairwiseCipher;
import android.net.wifi.WifiConfiguration.Protocol;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.util.XmlUtil;
import com.android.server.wifi.util.XmlUtil.IpConfigurationXmlUtil;
import com.android.server.wifi.util.XmlUtil.WifiConfigurationXmlUtil;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class WifiBackupDataV1Parser implements WifiBackupDataParser {
    private static final int HIGHEST_SUPPORTED_MINOR_VERSION = 0;
    private static final Set<String> IP_CONFIGURATION_MINOR_V0_SUPPORTED_TAGS = new HashSet(Arrays.asList(new String[]{IpConfigurationXmlUtil.XML_TAG_IP_ASSIGNMENT, IpConfigurationXmlUtil.XML_TAG_LINK_ADDRESS, IpConfigurationXmlUtil.XML_TAG_LINK_PREFIX_LENGTH, IpConfigurationXmlUtil.XML_TAG_GATEWAY_ADDRESS, IpConfigurationXmlUtil.XML_TAG_DNS_SERVER_ADDRESSES, IpConfigurationXmlUtil.XML_TAG_PROXY_SETTINGS, IpConfigurationXmlUtil.XML_TAG_PROXY_HOST, IpConfigurationXmlUtil.XML_TAG_PROXY_PORT, IpConfigurationXmlUtil.XML_TAG_PROXY_EXCLUSION_LIST, IpConfigurationXmlUtil.XML_TAG_PROXY_PAC_FILE}));
    private static final String TAG = "WifiBackupDataV1Parser";
    private static final Set<String> WIFI_CONFIGURATION_MINOR_V0_SUPPORTED_TAGS = new HashSet(Arrays.asList(new String[]{WifiConfigurationXmlUtil.XML_TAG_CONFIG_KEY, WifiConfigurationXmlUtil.XML_TAG_SSID, WifiConfigurationXmlUtil.XML_TAG_ORI_SSID, WifiConfigurationXmlUtil.XML_TAG_BSSID, WifiConfigurationXmlUtil.XML_TAG_PRE_SHARED_KEY, WifiConfigurationXmlUtil.XML_TAG_WEP_KEYS, WifiConfigurationXmlUtil.XML_TAG_WEP_TX_KEY_INDEX, WifiConfigurationXmlUtil.XML_TAG_HIDDEN_SSID, WifiConfigurationXmlUtil.XML_TAG_REQUIRE_PMF, WifiConfigurationXmlUtil.XML_TAG_ALLOWED_KEY_MGMT, WifiConfigurationXmlUtil.XML_TAG_ALLOWED_PROTOCOLS, WifiConfigurationXmlUtil.XML_TAG_ALLOWED_AUTH_ALGOS, WifiConfigurationXmlUtil.XML_TAG_ALLOWED_GROUP_CIPHERS, WifiConfigurationXmlUtil.XML_TAG_ALLOWED_PAIRWISE_CIPHERS, WifiConfigurationXmlUtil.XML_TAG_SHARED}));

    /* renamed from: com.android.server.wifi.WifiBackupDataV1Parser$1 */
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

    WifiBackupDataV1Parser() {
    }

    public List<WifiConfiguration> parseNetworkConfigurationsFromXml(XmlPullParser in, int outerTagDepth, int minorVersion) throws XmlPullParserException, IOException {
        if (minorVersion > 0) {
            minorVersion = 0;
        }
        XmlUtil.gotoNextSectionWithName(in, "NetworkList", outerTagDepth);
        int networkListTagDepth = outerTagDepth + 1;
        List<WifiConfiguration> configurations = new ArrayList();
        while (XmlUtil.gotoNextSectionWithNameOrEnd(in, "Network", networkListTagDepth)) {
            WifiConfiguration configuration = parseNetworkConfigurationFromXml(in, minorVersion, networkListTagDepth);
            if (configuration != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Parsed Configuration: ");
                stringBuilder.append(configuration.configKey());
                Log.v(str, stringBuilder.toString());
                configurations.add(configuration);
            }
        }
        return configurations;
    }

    private WifiConfiguration parseNetworkConfigurationFromXml(XmlPullParser in, int minorVersion, int outerTagDepth) throws XmlPullParserException, IOException {
        int networkTagDepth = outerTagDepth + 1;
        XmlUtil.gotoNextSectionWithName(in, "WifiConfiguration", networkTagDepth);
        int configTagDepth = networkTagDepth + 1;
        WifiConfiguration configuration = parseWifiConfigurationFromXmlAndValidateConfigKey(in, configTagDepth, minorVersion);
        if (configuration == null) {
            return null;
        }
        XmlUtil.gotoNextSectionWithName(in, "IpConfiguration", networkTagDepth);
        configuration.setIpConfiguration(parseIpConfigurationFromXml(in, configTagDepth, minorVersion));
        return configuration;
    }

    private WifiConfiguration parseWifiConfigurationFromXmlAndValidateConfigKey(XmlPullParser in, int outerTagDepth, int minorVersion) throws XmlPullParserException, IOException {
        Pair<String, WifiConfiguration> parsedConfig = parseWifiConfigurationFromXml(in, outerTagDepth, minorVersion);
        if (parsedConfig == null || parsedConfig.first == null || parsedConfig.second == null) {
            return null;
        }
        String configKeyParsed = parsedConfig.first;
        WifiConfiguration configuration = parsedConfig.second;
        String configKeyCalculated = configuration.configKey();
        if (!configKeyParsed.equals(configKeyCalculated)) {
            String configKeyMismatchLog = new StringBuilder();
            configKeyMismatchLog.append("Configuration key does not match. Retrieved: ");
            configKeyMismatchLog.append(configKeyParsed);
            configKeyMismatchLog.append(", Calculated: ");
            configKeyMismatchLog.append(configKeyCalculated);
            configKeyMismatchLog = configKeyMismatchLog.toString();
            if (configuration.shared) {
                Log.e(TAG, configKeyMismatchLog);
                return null;
            }
            Log.w(TAG, configKeyMismatchLog);
        }
        return configuration;
    }

    private static void clearAnyKnownIssuesInParsedConfiguration(WifiConfiguration config) {
        if (config.allowedKeyManagement.length() > KeyMgmt.strings.length) {
            config.allowedKeyManagement.clear(KeyMgmt.strings.length, config.allowedKeyManagement.length());
        }
        if (config.allowedProtocols.length() > Protocol.strings.length) {
            config.allowedProtocols.clear(Protocol.strings.length, config.allowedProtocols.length());
        }
        if (config.allowedAuthAlgorithms.length() > AuthAlgorithm.strings.length) {
            config.allowedAuthAlgorithms.clear(AuthAlgorithm.strings.length, config.allowedAuthAlgorithms.length());
        }
        if (config.allowedGroupCiphers.length() > GroupCipher.strings.length) {
            config.allowedGroupCiphers.clear(GroupCipher.strings.length, config.allowedGroupCiphers.length());
        }
        if (config.allowedPairwiseCiphers.length() > PairwiseCipher.strings.length) {
            config.allowedPairwiseCiphers.clear(PairwiseCipher.strings.length, config.allowedPairwiseCiphers.length());
        }
    }

    /* JADX WARNING: Missing block: B:44:0x00c5, code skipped:
            if (r7.equals(com.android.server.wifi.util.XmlUtil.WifiConfigurationXmlUtil.XML_TAG_SSID) != false) goto L_0x00ea;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Pair<String, WifiConfiguration> parseWifiConfigurationFromXml(XmlPullParser in, int outerTagDepth, int minorVersion) throws XmlPullParserException, IOException {
        WifiConfiguration configuration = new WifiConfiguration();
        String configKeyInData = null;
        Set<String> supportedTags = getSupportedWifiConfigurationTags(minorVersion);
        while (!XmlUtil.isNextSectionEnd(in, outerTagDepth)) {
            int i = 1;
            String[] valueName = new String[1];
            String value = XmlUtil.readCurrentValue(in, valueName);
            String tagName = valueName[0];
            if (tagName == null) {
                throw new XmlPullParserException("Missing value name");
            } else if (supportedTags.contains(tagName)) {
                switch (tagName.hashCode()) {
                    case -1819699067:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_SHARED)) {
                            i = 14;
                            break;
                        }
                    case -1704616680:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_ALLOWED_KEY_MGMT)) {
                            i = 9;
                            break;
                        }
                    case -181205965:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_ALLOWED_PROTOCOLS)) {
                            i = 10;
                            break;
                        }
                    case 2554747:
                        break;
                    case 63507133:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_BSSID)) {
                            i = 3;
                            break;
                        }
                    case 461626209:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_ORI_SSID)) {
                            i = 2;
                            break;
                        }
                    case 682791106:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_ALLOWED_PAIRWISE_CIPHERS)) {
                            i = 13;
                            break;
                        }
                    case 736944625:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_ALLOWED_GROUP_CIPHERS)) {
                            i = 12;
                            break;
                        }
                    case 797043831:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_PRE_SHARED_KEY)) {
                            i = 4;
                            break;
                        }
                    case 1199498141:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_CONFIG_KEY)) {
                            i = 0;
                            break;
                        }
                    case 1851050768:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_ALLOWED_AUTH_ALGOS)) {
                            i = 11;
                            break;
                        }
                    case 1905126713:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_WEP_TX_KEY_INDEX)) {
                            i = 6;
                            break;
                        }
                    case 1955037270:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_WEP_KEYS)) {
                            i = 5;
                            break;
                        }
                    case 1965854789:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_HIDDEN_SSID)) {
                            i = 7;
                            break;
                        }
                    case 2143705732:
                        if (tagName.equals(WifiConfigurationXmlUtil.XML_TAG_REQUIRE_PMF)) {
                            i = 8;
                            break;
                        }
                    default:
                        i = -1;
                        break;
                }
                switch (i) {
                    case 0:
                        configKeyInData = value;
                        break;
                    case 1:
                        configuration.SSID = value;
                        break;
                    case 2:
                        configuration.oriSsid = value;
                        break;
                    case 3:
                        configuration.BSSID = value;
                        break;
                    case 4:
                        configuration.preSharedKey = value;
                        break;
                    case 5:
                        populateWepKeysFromXmlValue(value, configuration.wepKeys);
                        break;
                    case 6:
                        configuration.wepTxKeyIndex = ((Integer) value).intValue();
                        break;
                    case 7:
                        configuration.hiddenSSID = ((Boolean) value).booleanValue();
                        break;
                    case 8:
                        configuration.requirePMF = ((Boolean) value).booleanValue();
                        break;
                    case 9:
                        configuration.allowedKeyManagement = BitSet.valueOf((byte[]) value);
                        break;
                    case 10:
                        configuration.allowedProtocols = BitSet.valueOf((byte[]) value);
                        break;
                    case 11:
                        configuration.allowedAuthAlgorithms = BitSet.valueOf((byte[]) value);
                        break;
                    case 12:
                        configuration.allowedGroupCiphers = BitSet.valueOf((byte[]) value);
                        break;
                    case 13:
                        configuration.allowedPairwiseCiphers = BitSet.valueOf((byte[]) value);
                        break;
                    case 14:
                        configuration.shared = ((Boolean) value).booleanValue();
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown value name found: ");
                        stringBuilder.append(valueName[0]);
                        throw new XmlPullParserException(stringBuilder.toString());
                }
            } else {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unsupported tag + \"");
                stringBuilder2.append(tagName);
                stringBuilder2.append("\" found in <WifiConfiguration> section, ignoring.");
                Log.w(str, stringBuilder2.toString());
            }
        }
        clearAnyKnownIssuesInParsedConfiguration(configuration);
        return Pair.create(configKeyInData, configuration);
    }

    private static Set<String> getSupportedWifiConfigurationTags(int minorVersion) {
        if (minorVersion == 0) {
            return WIFI_CONFIGURATION_MINOR_V0_SUPPORTED_TAGS;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid minorVersion: ");
        stringBuilder.append(minorVersion);
        Log.e(str, stringBuilder.toString());
        return Collections.emptySet();
    }

    private static void populateWepKeysFromXmlValue(Object value, String[] wepKeys) throws XmlPullParserException, IOException {
        String[] wepKeysInData = (String[]) value;
        if (wepKeysInData != null) {
            if (wepKeysInData.length == wepKeys.length) {
                for (int i = 0; i < wepKeys.length; i++) {
                    if (wepKeysInData[i].isEmpty()) {
                        wepKeys[i] = null;
                    } else {
                        wepKeys[i] = wepKeysInData[i];
                    }
                }
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid Wep Keys length: ");
            stringBuilder.append(wepKeysInData.length);
            throw new XmlPullParserException(stringBuilder.toString());
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static IpConfiguration parseIpConfigurationFromXml(XmlPullParser in, int outerTagDepth, int minorVersion) throws XmlPullParserException, IOException {
        Set<String> supportedTags;
        StringBuilder stringBuilder;
        String str;
        StringBuilder stringBuilder2;
        Set<String> supportedTags2 = getSupportedIpConfigurationTags(minorVersion);
        String ipAssignmentString = null;
        String linkAddressString = null;
        Integer linkPrefixLength = null;
        String gatewayAddressString = null;
        String[] dnsServerAddressesString = null;
        String proxySettingsString = null;
        String proxyHost = null;
        int proxyPort = -1;
        String proxyExclusionList = null;
        String proxyPacFile = null;
        while (!XmlUtil.isNextSectionEnd(in, outerTagDepth)) {
            String[] valueName = new String[1];
            Set<String> value = XmlUtil.readCurrentValue(in, valueName);
            String tagName = valueName[0];
            if (tagName != null) {
                if (supportedTags2.contains(tagName)) {
                    int i;
                    supportedTags = supportedTags2;
                    switch (tagName.hashCode()) {
                        case -1747338169:
                            if (tagName.equals(IpConfigurationXmlUtil.XML_TAG_DNS_SERVER_ADDRESSES) != null) {
                                i = 4;
                                break;
                            }
                        case -1520820614:
                            if (tagName.equals(IpConfigurationXmlUtil.XML_TAG_LINK_ADDRESS) != null) {
                                i = 1;
                                break;
                            }
                        case -1464842926:
                            if (tagName.equals(IpConfigurationXmlUtil.XML_TAG_LINK_PREFIX_LENGTH) != null) {
                                i = 2;
                                break;
                            }
                        case -920546460:
                            if (tagName.equals(IpConfigurationXmlUtil.XML_TAG_PROXY_PAC_FILE) != null) {
                                i = 9;
                                break;
                            }
                        case 162774900:
                            if (tagName.equals(IpConfigurationXmlUtil.XML_TAG_IP_ASSIGNMENT) != null) {
                                i = 0;
                                break;
                            }
                        case 858907952:
                            if (tagName.equals(IpConfigurationXmlUtil.XML_TAG_GATEWAY_ADDRESS) != null) {
                                i = 3;
                                break;
                            }
                        case 1527606550:
                            if (tagName.equals(IpConfigurationXmlUtil.XML_TAG_PROXY_HOST) != null) {
                                i = 6;
                                break;
                            }
                        case 1527844847:
                            if (tagName.equals(IpConfigurationXmlUtil.XML_TAG_PROXY_PORT) != null) {
                                i = 7;
                                break;
                            }
                        case 1940148190:
                            if (tagName.equals(IpConfigurationXmlUtil.XML_TAG_PROXY_EXCLUSION_LIST) != null) {
                                i = 8;
                                break;
                            }
                        case 1968819345:
                            if (tagName.equals(IpConfigurationXmlUtil.XML_TAG_PROXY_SETTINGS) != null) {
                                i = 5;
                                break;
                            }
                        default:
                            i = -1;
                            break;
                    }
                    switch (i) {
                        case 0:
                            ipAssignmentString = (String) value;
                            break;
                        case 1:
                            linkAddressString = (String) value;
                            break;
                        case 2:
                            linkPrefixLength = (Integer) value;
                            break;
                        case 3:
                            gatewayAddressString = (String) value;
                            break;
                        case 4:
                            dnsServerAddressesString = (String[]) value;
                            break;
                        case 5:
                            proxySettingsString = (String) value;
                            break;
                        case 6:
                            proxyHost = (String) value;
                            break;
                        case 7:
                            proxyPort = ((Integer) value).intValue();
                            break;
                        case 8:
                            proxyExclusionList = (String) value;
                            break;
                        case 9:
                            proxyPacFile = (String) value;
                            break;
                        default:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Unknown value name found: ");
                            stringBuilder.append(valueName[0]);
                            throw new XmlPullParserException(stringBuilder.toString());
                    }
                }
                str = TAG;
                stringBuilder2 = new StringBuilder();
                supportedTags = supportedTags2;
                stringBuilder2.append("Unsupported tag + \"");
                stringBuilder2.append(tagName);
                stringBuilder2.append("\" found in <IpConfiguration> section, ignoring.");
                Log.w(str, stringBuilder2.toString());
                supportedTags2 = supportedTags;
            } else {
                String str2 = tagName;
                throw new XmlPullParserException("Missing value name");
            }
        }
        XmlPullParser xmlPullParser = in;
        supportedTags = supportedTags2;
        IpConfiguration ipConfiguration = new IpConfiguration();
        String str3;
        if (ipAssignmentString != null) {
            IpAssignment ipAssignment = IpAssignment.valueOf(ipAssignmentString);
            ipConfiguration.setIpAssignment(ipAssignment);
            String str4;
            switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$IpAssignment[ipAssignment.ordinal()]) {
                case 1:
                    StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
                    if (linkAddressString == null || linkPrefixLength == null) {
                        str3 = linkAddressString;
                    } else {
                        str4 = ipAssignmentString;
                        LinkAddress ipAssignmentString2 = new LinkAddress(NetworkUtils.numericToInetAddress(linkAddressString), linkPrefixLength.intValue());
                        if (ipAssignmentString2.getAddress() instanceof Inet4Address) {
                            staticIpConfiguration.ipAddress = ipAssignmentString2;
                            str3 = linkAddressString;
                        } else {
                            str = TAG;
                            stringBuilder2 = new StringBuilder();
                            str3 = linkAddressString;
                            stringBuilder2.append("Non-IPv4 address: ");
                            stringBuilder2.append(ipAssignmentString2);
                            Log.w(str, stringBuilder2.toString());
                        }
                    }
                    if (gatewayAddressString != null) {
                        InetAddress gateway = NetworkUtils.numericToInetAddress(gatewayAddressString);
                        RouteInfo route = new RouteInfo(null, gateway);
                        if (route.isIPv4Default()) {
                            staticIpConfiguration.gateway = gateway;
                        } else {
                            String str5 = TAG;
                            LinkAddress dest = null;
                            ipAssignmentString = new StringBuilder();
                            ipAssignmentString.append("Non-IPv4 default route: ");
                            ipAssignmentString.append(route);
                            Log.w(str5, ipAssignmentString.toString());
                        }
                    }
                    if (dnsServerAddressesString != null) {
                        ipAssignmentString = dnsServerAddressesString.length;
                        int i2 = 0;
                        while (i2 < ipAssignmentString) {
                            String str6 = ipAssignmentString;
                            staticIpConfiguration.dnsServers.add(NetworkUtils.numericToInetAddress(dnsServerAddressesString[i2]));
                            i2++;
                            ipAssignmentString = str6;
                        }
                    }
                    ipConfiguration.setStaticIpConfiguration(staticIpConfiguration);
                    break;
                case 2:
                case 3:
                    str4 = ipAssignmentString;
                    str3 = linkAddressString;
                    break;
                default:
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Unknown ip assignment type: ");
                    stringBuilder3.append(ipAssignment);
                    throw new XmlPullParserException(stringBuilder3.toString());
            }
            if (proxySettingsString != null) {
                ipAssignmentString = ProxySettings.valueOf(proxySettingsString);
                ipConfiguration.setProxySettings(ipAssignmentString);
                switch (AnonymousClass1.$SwitchMap$android$net$IpConfiguration$ProxySettings[ipAssignmentString.ordinal()]) {
                    case 1:
                        if (proxyHost == null) {
                            throw new XmlPullParserException("ProxyHost was missing in IpConfiguration section");
                        } else if (proxyPort == -1) {
                            throw new XmlPullParserException("ProxyPort was missing in IpConfiguration section");
                        } else if (proxyExclusionList != null) {
                            ipConfiguration.setHttpProxy(new ProxyInfo(proxyHost, proxyPort, proxyExclusionList));
                            break;
                        } else {
                            throw new XmlPullParserException("ProxyExclusionList was missing in IpConfiguration section");
                        }
                    case 2:
                        if (proxyPacFile != null) {
                            ipConfiguration.setHttpProxy(new ProxyInfo(proxyPacFile));
                            break;
                        }
                        throw new XmlPullParserException("ProxyPac was missing in IpConfiguration section");
                    case 3:
                    case 4:
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown proxy settings type: ");
                        stringBuilder.append(ipAssignmentString);
                        throw new XmlPullParserException(stringBuilder.toString());
                }
                return ipConfiguration;
            }
            throw new XmlPullParserException("ProxySettings was missing in IpConfiguration section");
        }
        str3 = linkAddressString;
        throw new XmlPullParserException("IpAssignment was missing in IpConfiguration section");
    }

    private static Set<String> getSupportedIpConfigurationTags(int minorVersion) {
        if (minorVersion == 0) {
            return IP_CONFIGURATION_MINOR_V0_SUPPORTED_TAGS;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid minorVersion: ");
        stringBuilder.append(minorVersion);
        Log.e(str, stringBuilder.toString());
        return Collections.emptySet();
    }
}
