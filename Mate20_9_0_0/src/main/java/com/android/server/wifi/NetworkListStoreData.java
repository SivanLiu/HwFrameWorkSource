package com.android.server.wifi;

import android.content.Context;
import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.WifiConfigStore.StoreData;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.util.XmlUtil;
import com.android.server.wifi.util.XmlUtil.IpConfigurationXmlUtil;
import com.android.server.wifi.util.XmlUtil.NetworkSelectionStatusXmlUtil;
import com.android.server.wifi.util.XmlUtil.WifiConfigurationXmlUtil;
import com.android.server.wifi.util.XmlUtil.WifiEnterpriseConfigXmlUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class NetworkListStoreData implements StoreData {
    private static final String TAG = "NetworkListStoreData";
    private static final String XML_TAG_SECTION_HEADER_IP_CONFIGURATION = "IpConfiguration";
    private static final String XML_TAG_SECTION_HEADER_NETWORK = "Network";
    private static final String XML_TAG_SECTION_HEADER_NETWORK_LIST = "NetworkList";
    private static final String XML_TAG_SECTION_HEADER_NETWORK_STATUS = "NetworkStatus";
    private static final String XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION = "WifiConfiguration";
    private static final String XML_TAG_SECTION_HEADER_WIFI_ENTERPRISE_CONFIGURATION = "WifiEnterpriseConfiguration";
    private final Context mContext;
    private List<WifiConfiguration> mSharedConfigurations;
    private List<WifiConfiguration> mUserConfigurations;

    NetworkListStoreData(Context context) {
        this.mContext = context;
    }

    public void serializeData(XmlSerializer out, boolean shared) throws XmlPullParserException, IOException {
        if (shared) {
            serializeNetworkList(out, this.mSharedConfigurations);
        } else {
            serializeNetworkList(out, this.mUserConfigurations);
        }
    }

    public void deserializeData(XmlPullParser in, int outerTagDepth, boolean shared) throws XmlPullParserException, IOException {
        if (in != null) {
            if (shared) {
                this.mSharedConfigurations = parseNetworkList(in, outerTagDepth);
            } else {
                this.mUserConfigurations = parseNetworkList(in, outerTagDepth);
            }
        }
    }

    public void resetData(boolean shared) {
        if (shared) {
            this.mSharedConfigurations = null;
        } else {
            this.mUserConfigurations = null;
        }
    }

    public String getName() {
        return XML_TAG_SECTION_HEADER_NETWORK_LIST;
    }

    public boolean supportShareData() {
        return true;
    }

    public void setSharedConfigurations(List<WifiConfiguration> configs) {
        this.mSharedConfigurations = configs;
    }

    public List<WifiConfiguration> getSharedConfigurations() {
        if (this.mSharedConfigurations == null) {
            return new ArrayList();
        }
        return this.mSharedConfigurations;
    }

    public void setUserConfigurations(List<WifiConfiguration> configs) {
        this.mUserConfigurations = configs;
    }

    public List<WifiConfiguration> getUserConfigurations() {
        if (this.mUserConfigurations == null) {
            return new ArrayList();
        }
        return this.mUserConfigurations;
    }

    private void serializeNetworkList(XmlSerializer out, List<WifiConfiguration> networkList) throws XmlPullParserException, IOException {
        if (networkList != null) {
            for (WifiConfiguration network : networkList) {
                serializeNetwork(out, network);
            }
        }
    }

    private void serializeNetwork(XmlSerializer out, WifiConfiguration config) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_NETWORK);
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION);
        WifiConfigurationXmlUtil.writeToXmlForConfigStore(out, config);
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION);
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_NETWORK_STATUS);
        NetworkSelectionStatusXmlUtil.writeToXml(out, config.getNetworkSelectionStatus());
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_NETWORK_STATUS);
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_IP_CONFIGURATION);
        IpConfigurationXmlUtil.writeToXml(out, config.getIpConfiguration());
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_IP_CONFIGURATION);
        if (!(config.enterpriseConfig == null || config.enterpriseConfig.getEapMethod() == -1)) {
            XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_WIFI_ENTERPRISE_CONFIGURATION);
            WifiEnterpriseConfigXmlUtil.writeToXml(out, config.enterpriseConfig);
            XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_WIFI_ENTERPRISE_CONFIGURATION);
        }
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_NETWORK);
    }

    private List<WifiConfiguration> parseNetworkList(XmlPullParser in, int outerTagDepth) throws XmlPullParserException, IOException {
        List<WifiConfiguration> networkList = new ArrayList();
        while (XmlUtil.gotoNextSectionWithNameOrEnd(in, XML_TAG_SECTION_HEADER_NETWORK, outerTagDepth)) {
            try {
                WifiConfiguration config = parseNetwork(in, outerTagDepth + 1);
                int appId = UserHandle.getAppId(config.creatorUid);
                if (config.BSSID != null && (appId == 0 || appId == 1000 || appId == 1010)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("parseFromXml creater: ");
                    stringBuilder.append(config.creatorUid);
                    stringBuilder.append(", ssid: ");
                    stringBuilder.append(config.SSID);
                    stringBuilder.append(", Bssid:");
                    stringBuilder.append(ScanResultUtil.getConfusedBssid(config.BSSID));
                    Log.w(str, stringBuilder.toString());
                    config.BSSID = null;
                }
                networkList.add(config);
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to parse network config. Skipping...", e);
            }
        }
        return networkList;
    }

    /* JADX WARNING: Removed duplicated region for block: B:68:0x0056 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00a4  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0080  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x006f  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x0056 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00a4  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0080  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x006f  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x0056 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00a4  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0080  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x006f  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x0056 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:39:0x00a4  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0092  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0080  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x006f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private WifiConfiguration parseNetwork(XmlPullParser in, int outerTagDepth) throws XmlPullParserException, IOException {
        String str;
        Pair<String, WifiConfiguration> parsedConfig = null;
        NetworkSelectionStatus status = null;
        IpConfiguration ipConfiguration = null;
        WifiEnterpriseConfig enterpriseConfig = null;
        String[] headerName = new String[1];
        while (XmlUtil.gotoNextSectionOrEnd(in, headerName, outerTagDepth)) {
            int i;
            str = headerName[0];
            int hashCode = str.hashCode();
            if (hashCode == -148477024) {
                if (str.equals(XML_TAG_SECTION_HEADER_NETWORK_STATUS)) {
                    i = 1;
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
            } else if (hashCode == 46473153) {
                if (str.equals(XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION)) {
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
            } else if (hashCode == 325854959) {
                if (str.equals(XML_TAG_SECTION_HEADER_IP_CONFIGURATION)) {
                    i = 2;
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
            } else if (hashCode == 1285464096 && str.equals(XML_TAG_SECTION_HEADER_WIFI_ENTERPRISE_CONFIGURATION)) {
                i = 3;
                switch (i) {
                    case 0:
                        if (parsedConfig == null) {
                            parsedConfig = WifiConfigurationXmlUtil.parseFromXml(in, outerTagDepth + 1);
                            break;
                        }
                        throw new XmlPullParserException("Detected duplicate tag for: WifiConfiguration");
                    case 1:
                        if (status == null) {
                            status = NetworkSelectionStatusXmlUtil.parseFromXml(in, outerTagDepth + 1);
                            break;
                        }
                        throw new XmlPullParserException("Detected duplicate tag for: NetworkStatus");
                    case 2:
                        if (ipConfiguration == null) {
                            ipConfiguration = IpConfigurationXmlUtil.parseFromXml(in, outerTagDepth + 1);
                            break;
                        }
                        throw new XmlPullParserException("Detected duplicate tag for: IpConfiguration");
                    case 3:
                        if (enterpriseConfig == null) {
                            enterpriseConfig = WifiEnterpriseConfigXmlUtil.parseFromXml(in, outerTagDepth + 1);
                            break;
                        }
                        throw new XmlPullParserException("Detected duplicate tag for: WifiEnterpriseConfiguration");
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown tag under Network: ");
                        stringBuilder.append(headerName[0]);
                        throw new XmlPullParserException(stringBuilder.toString());
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
        if (parsedConfig == null || parsedConfig.first == null || parsedConfig.second == null) {
            throw new XmlPullParserException("XML parsing of wifi configuration failed");
        }
        String configKeyParsed = parsedConfig.first;
        WifiConfiguration configuration = parsedConfig.second;
        str = configuration.configKey();
        if (configKeyParsed.equals(str)) {
            String creatorName = this.mContext.getPackageManager().getNameForUid(configuration.creatorUid);
            String str2;
            StringBuilder stringBuilder2;
            if (creatorName == null) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Invalid creatorUid for saved network ");
                stringBuilder2.append(configuration.configKey());
                stringBuilder2.append(", creatorUid=");
                stringBuilder2.append(configuration.creatorUid);
                Log.e(str2, stringBuilder2.toString());
                configuration.creatorUid = 1000;
                configuration.creatorName = this.mContext.getPackageManager().getNameForUid(1000);
            } else if (!creatorName.equals(configuration.creatorName)) {
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Invalid creatorName for saved network ");
                stringBuilder2.append(configuration.configKey());
                stringBuilder2.append(", creatorUid=");
                stringBuilder2.append(configuration.creatorUid);
                stringBuilder2.append(", creatorName=");
                stringBuilder2.append(configuration.creatorName);
                Log.w(str2, stringBuilder2.toString());
                configuration.creatorName = creatorName;
            }
            configuration.setNetworkSelectionStatus(status);
            configuration.setIpConfiguration(ipConfiguration);
            if (enterpriseConfig != null) {
                configuration.enterpriseConfig = enterpriseConfig;
            }
            return configuration;
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Configuration key does not match. Retrieved: ");
        stringBuilder3.append(configKeyParsed);
        stringBuilder3.append(", Calculated: ");
        stringBuilder3.append(str);
        throw new XmlPullParserException(stringBuilder3.toString());
    }
}
