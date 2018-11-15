package com.android.server.wifi;

import android.net.IpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.net.IpConfigStore;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.XmlUtil;
import com.android.server.wifi.util.XmlUtil.IpConfigurationXmlUtil;
import com.android.server.wifi.util.XmlUtil.WifiConfigurationXmlUtil;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class WifiBackupRestore {
    private static final float CURRENT_BACKUP_DATA_VERSION = 1.0f;
    private static final int INITIAL_BACKUP_DATA_VERSION = 1;
    private static final String PSK_MASK_LINE_MATCH_PATTERN = "<.*PreSharedKey.*>.*<.*>";
    private static final String PSK_MASK_REPLACE_PATTERN = "$1*$3";
    private static final String PSK_MASK_SEARCH_PATTERN = "(<.*PreSharedKey.*>)(.*)(<.*>)";
    private static final String TAG = "WifiBackupRestore";
    private static final String WEP_KEYS_MASK_LINE_END_MATCH_PATTERN = "</string-array>";
    private static final String WEP_KEYS_MASK_LINE_START_MATCH_PATTERN = "<string-array.*WEPKeys.*num=\"[0-9]\">";
    private static final String WEP_KEYS_MASK_REPLACE_PATTERN = "$1*$3";
    private static final String WEP_KEYS_MASK_SEARCH_PATTERN = "(<.*=)(.*)(/>)";
    private static final String XML_TAG_DOCUMENT_HEADER = "WifiBackupData";
    static final String XML_TAG_SECTION_HEADER_IP_CONFIGURATION = "IpConfiguration";
    static final String XML_TAG_SECTION_HEADER_NETWORK = "Network";
    static final String XML_TAG_SECTION_HEADER_NETWORK_LIST = "NetworkList";
    static final String XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION = "WifiConfiguration";
    private static final String XML_TAG_VERSION = "Version";
    private byte[] mDebugLastBackupDataRestored;
    private byte[] mDebugLastBackupDataRetrieved;
    private byte[] mDebugLastSupplicantBackupDataRestored;
    private boolean mVerboseLoggingEnabled = false;
    private final WifiPermissionsUtil mWifiPermissionsUtil;

    public static class SupplicantBackupMigration {
        private static final String PSK_MASK_LINE_MATCH_PATTERN = ".*psk.*=.*";
        private static final String PSK_MASK_REPLACE_PATTERN = "$1*";
        private static final String PSK_MASK_SEARCH_PATTERN = "(.*psk.*=)(.*)";
        public static final String SUPPLICANT_KEY_CA_CERT = "ca_cert";
        public static final String SUPPLICANT_KEY_CA_PATH = "ca_path";
        public static final String SUPPLICANT_KEY_CLIENT_CERT = "client_cert";
        public static final String SUPPLICANT_KEY_EAP = "eap";
        public static final String SUPPLICANT_KEY_HIDDEN = "scan_ssid";
        public static final String SUPPLICANT_KEY_ID_STR = "id_str";
        public static final String SUPPLICANT_KEY_KEY_MGMT = "key_mgmt";
        public static final String SUPPLICANT_KEY_PSK = "psk";
        public static final String SUPPLICANT_KEY_SSID = "ssid";
        public static final String SUPPLICANT_KEY_WEP_KEY0 = WifiConfiguration.wepKeyVarNames[0];
        public static final String SUPPLICANT_KEY_WEP_KEY1 = WifiConfiguration.wepKeyVarNames[1];
        public static final String SUPPLICANT_KEY_WEP_KEY2 = WifiConfiguration.wepKeyVarNames[2];
        public static final String SUPPLICANT_KEY_WEP_KEY3 = WifiConfiguration.wepKeyVarNames[3];
        public static final String SUPPLICANT_KEY_WEP_KEY_IDX = "wep_tx_keyidx";
        private static final String WEP_KEYS_MASK_LINE_MATCH_PATTERN;
        private static final String WEP_KEYS_MASK_REPLACE_PATTERN = "$1*";
        private static final String WEP_KEYS_MASK_SEARCH_PATTERN;

        static class SupplicantNetwork {
            public boolean certUsed = false;
            public boolean isEap = false;
            private String mParsedHiddenLine;
            private String mParsedIdStrLine;
            private String mParsedKeyMgmtLine;
            private String mParsedPskLine;
            private String mParsedSSIDLine;
            private String[] mParsedWepKeyLines = new String[4];
            private String mParsedWepTxKeyIdxLine;

            SupplicantNetwork() {
            }

            public static SupplicantNetwork readNetworkFromStream(BufferedReader in) {
                SupplicantNetwork n = new SupplicantNetwork();
                while (in.ready()) {
                    try {
                        String line = in.readLine();
                        if (line == null || line.startsWith("}")) {
                            break;
                        }
                        n.parseLine(line);
                    } catch (IOException e) {
                        return null;
                    }
                }
                return n;
            }

            void parseLine(String line) {
                line = line.trim();
                if (!line.isEmpty()) {
                    if (line.startsWith("ssid=")) {
                        this.mParsedSSIDLine = line;
                    } else if (line.startsWith("scan_ssid=")) {
                        this.mParsedHiddenLine = line;
                    } else if (line.startsWith("key_mgmt=")) {
                        this.mParsedKeyMgmtLine = line;
                        if (line.contains("EAP")) {
                            this.isEap = true;
                        }
                    } else if (line.startsWith("client_cert=")) {
                        this.certUsed = true;
                    } else if (line.startsWith("ca_cert=")) {
                        this.certUsed = true;
                    } else if (line.startsWith("ca_path=")) {
                        this.certUsed = true;
                    } else if (line.startsWith("eap=")) {
                        this.isEap = true;
                    } else if (line.startsWith("psk=")) {
                        this.mParsedPskLine = line;
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(SupplicantBackupMigration.SUPPLICANT_KEY_WEP_KEY0);
                        stringBuilder.append("=");
                        if (line.startsWith(stringBuilder.toString())) {
                            this.mParsedWepKeyLines[0] = line;
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(SupplicantBackupMigration.SUPPLICANT_KEY_WEP_KEY1);
                            stringBuilder.append("=");
                            if (line.startsWith(stringBuilder.toString())) {
                                this.mParsedWepKeyLines[1] = line;
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(SupplicantBackupMigration.SUPPLICANT_KEY_WEP_KEY2);
                                stringBuilder.append("=");
                                if (line.startsWith(stringBuilder.toString())) {
                                    this.mParsedWepKeyLines[2] = line;
                                } else {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(SupplicantBackupMigration.SUPPLICANT_KEY_WEP_KEY3);
                                    stringBuilder.append("=");
                                    if (line.startsWith(stringBuilder.toString())) {
                                        this.mParsedWepKeyLines[3] = line;
                                    } else if (line.startsWith("wep_tx_keyidx=")) {
                                        this.mParsedWepTxKeyIdxLine = line;
                                    } else if (line.startsWith("id_str=")) {
                                        this.mParsedIdStrLine = line;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            public WifiConfiguration createWifiConfiguration() {
                if (this.mParsedSSIDLine == null) {
                    return null;
                }
                WifiConfiguration configuration = new WifiConfiguration();
                configuration.SSID = this.mParsedSSIDLine.substring(this.mParsedSSIDLine.indexOf(61) + 1);
                if (this.mParsedHiddenLine != null) {
                    configuration.hiddenSSID = Integer.parseInt(this.mParsedHiddenLine.substring(this.mParsedHiddenLine.indexOf(61) + 1)) != 0;
                }
                if (this.mParsedKeyMgmtLine == null) {
                    configuration.allowedKeyManagement.set(1);
                    configuration.allowedKeyManagement.set(2);
                } else {
                    String[] typeStrings = this.mParsedKeyMgmtLine.substring(this.mParsedKeyMgmtLine.indexOf(61) + 1).split("\\s+");
                    for (String ktype : typeStrings) {
                        if (ktype.equals("NONE")) {
                            configuration.allowedKeyManagement.set(0);
                        } else if (ktype.equals("WPA-PSK")) {
                            configuration.allowedKeyManagement.set(1);
                        } else if (ktype.equals("WPA-EAP")) {
                            configuration.allowedKeyManagement.set(2);
                        } else if (ktype.equals("IEEE8021X")) {
                            configuration.allowedKeyManagement.set(3);
                        }
                    }
                }
                if (this.mParsedPskLine != null) {
                    configuration.preSharedKey = this.mParsedPskLine.substring(this.mParsedPskLine.indexOf(61) + 1);
                }
                if (this.mParsedWepKeyLines[0] != null) {
                    configuration.wepKeys[0] = this.mParsedWepKeyLines[0].substring(this.mParsedWepKeyLines[0].indexOf(61) + 1);
                }
                if (this.mParsedWepKeyLines[1] != null) {
                    configuration.wepKeys[1] = this.mParsedWepKeyLines[1].substring(this.mParsedWepKeyLines[1].indexOf(61) + 1);
                }
                if (this.mParsedWepKeyLines[2] != null) {
                    configuration.wepKeys[2] = this.mParsedWepKeyLines[2].substring(this.mParsedWepKeyLines[2].indexOf(61) + 1);
                }
                if (this.mParsedWepKeyLines[3] != null) {
                    configuration.wepKeys[3] = this.mParsedWepKeyLines[3].substring(this.mParsedWepKeyLines[3].indexOf(61) + 1);
                }
                if (this.mParsedWepTxKeyIdxLine != null) {
                    configuration.wepTxKeyIndex = Integer.valueOf(this.mParsedWepTxKeyIdxLine.substring(this.mParsedWepTxKeyIdxLine.indexOf(61) + 1)).intValue();
                }
                if (this.mParsedIdStrLine != null) {
                    String idString = this.mParsedIdStrLine.substring(this.mParsedIdStrLine.indexOf(61) + 1);
                    if (idString != null) {
                        Map<String, String> extras = SupplicantStaNetworkHal.parseNetworkExtra(NativeUtil.removeEnclosingQuotes(idString));
                        if (extras == null) {
                            Log.e(WifiBackupRestore.TAG, "Error parsing network extras, ignoring network.");
                            return null;
                        }
                        String configKey = (String) extras.get("configKey");
                        if (configKey == null) {
                            Log.e(WifiBackupRestore.TAG, "Configuration key was not passed, ignoring network.");
                            return null;
                        }
                        if (!configKey.equals(configuration.configKey())) {
                            String str = WifiBackupRestore.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Configuration key does not match. Retrieved: ");
                            stringBuilder.append(configKey);
                            stringBuilder.append(", Calculated: ");
                            stringBuilder.append(configuration.configKey());
                            Log.w(str, stringBuilder.toString());
                        }
                        if (Integer.parseInt((String) extras.get("creatorUid")) >= 10000) {
                            String str2 = WifiBackupRestore.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Ignoring network from non-system app: ");
                            stringBuilder2.append(configuration.configKey());
                            Log.d(str2, stringBuilder2.toString());
                            return null;
                        }
                    }
                }
                return configuration;
            }
        }

        static class SupplicantNetworks {
            final ArrayList<SupplicantNetwork> mNetworks = new ArrayList(8);

            SupplicantNetworks() {
            }

            public void readNetworksFromStream(BufferedReader in) {
                while (in.ready()) {
                    try {
                        String line = in.readLine();
                        if (line != null && line.startsWith("network")) {
                            SupplicantNetwork net = SupplicantNetwork.readNetworkFromStream(in);
                            if (net == null) {
                                Log.e(WifiBackupRestore.TAG, "Error while parsing the network.");
                            } else if (net.isEap || net.certUsed) {
                                String str = WifiBackupRestore.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Skipping enterprise network for restore: ");
                                stringBuilder.append(net.mParsedSSIDLine);
                                stringBuilder.append(" / ");
                                stringBuilder.append(net.mParsedKeyMgmtLine);
                                Log.d(str, stringBuilder.toString());
                            } else {
                                this.mNetworks.add(net);
                            }
                        }
                    } catch (IOException e) {
                        return;
                    }
                }
            }

            public List<WifiConfiguration> retrieveWifiConfigurations() {
                ArrayList<WifiConfiguration> wifiConfigurations = new ArrayList();
                Iterator it = this.mNetworks.iterator();
                while (it.hasNext()) {
                    try {
                        WifiConfiguration wifiConfiguration = ((SupplicantNetwork) it.next()).createWifiConfiguration();
                        if (wifiConfiguration != null) {
                            String str = WifiBackupRestore.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Parsed Configuration: ");
                            stringBuilder.append(wifiConfiguration.configKey());
                            Log.v(str, stringBuilder.toString());
                            wifiConfigurations.add(wifiConfiguration);
                        }
                    } catch (NumberFormatException e) {
                        String str2 = WifiBackupRestore.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Error parsing wifi configuration: ");
                        stringBuilder2.append(e);
                        Log.e(str2, stringBuilder2.toString());
                        return null;
                    }
                }
                return wifiConfigurations;
            }
        }

        static {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(".*");
            stringBuilder.append(SUPPLICANT_KEY_WEP_KEY0.replace("0", ""));
            stringBuilder.append(".*=.*");
            WEP_KEYS_MASK_LINE_MATCH_PATTERN = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append("(.*");
            stringBuilder.append(SUPPLICANT_KEY_WEP_KEY0.replace("0", ""));
            stringBuilder.append(".*=)(.*)");
            WEP_KEYS_MASK_SEARCH_PATTERN = stringBuilder.toString();
        }

        public static String createLogFromBackupData(byte[] data) {
            StringBuilder sb = new StringBuilder();
            try {
                for (String line : new String(data, StandardCharsets.UTF_8.name()).split("\n")) {
                    String line2;
                    if (line2.matches(PSK_MASK_LINE_MATCH_PATTERN)) {
                        line2 = line2.replaceAll(PSK_MASK_SEARCH_PATTERN, "$1*");
                    }
                    if (line2.matches(WEP_KEYS_MASK_LINE_MATCH_PATTERN)) {
                        line2 = line2.replaceAll(WEP_KEYS_MASK_SEARCH_PATTERN, "$1*");
                    }
                    sb.append(line2);
                    sb.append("\n");
                }
                return sb.toString();
            } catch (UnsupportedEncodingException e) {
                return "";
            }
        }
    }

    public WifiBackupRestore(WifiPermissionsUtil wifiPermissionsUtil) {
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
    }

    public byte[] retrieveBackupDataFromConfigurations(List<WifiConfiguration> configurations) {
        String str;
        StringBuilder stringBuilder;
        if (configurations == null) {
            Log.e(TAG, "Invalid configuration list received");
            return new byte[0];
        }
        try {
            XmlSerializer out = new FastXmlSerializer();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            out.setOutput(outputStream, StandardCharsets.UTF_8.name());
            XmlUtil.writeDocumentStart(out, XML_TAG_DOCUMENT_HEADER);
            XmlUtil.writeNextValue(out, XML_TAG_VERSION, Float.valueOf(CURRENT_BACKUP_DATA_VERSION));
            writeNetworkConfigurationsToXml(out, configurations);
            XmlUtil.writeDocumentEnd(out, XML_TAG_DOCUMENT_HEADER);
            byte[] data = outputStream.toByteArray();
            if (this.mVerboseLoggingEnabled) {
                this.mDebugLastBackupDataRetrieved = data;
            }
            return data;
        } catch (XmlPullParserException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error retrieving the backup data: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return new byte[0];
        } catch (IOException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error retrieving the backup data: ");
            stringBuilder.append(e2);
            Log.e(str, stringBuilder.toString());
            return new byte[0];
        }
    }

    private void writeNetworkConfigurationsToXml(XmlSerializer out, List<WifiConfiguration> configurations) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_NETWORK_LIST);
        for (WifiConfiguration configuration : configurations) {
            if (!configuration.isEnterprise()) {
                if (!configuration.isPasspoint()) {
                    if (this.mWifiPermissionsUtil.checkConfigOverridePermission(configuration.creatorUid)) {
                        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_NETWORK);
                        writeNetworkConfigurationToXml(out, configuration);
                        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_NETWORK);
                    } else {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Ignoring network from an app with no config override permission: ");
                        stringBuilder.append(configuration.configKey());
                        Log.d(str, stringBuilder.toString());
                    }
                }
            }
        }
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_NETWORK_LIST);
    }

    private void writeNetworkConfigurationToXml(XmlSerializer out, WifiConfiguration configuration) throws XmlPullParserException, IOException {
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION);
        WifiConfigurationXmlUtil.writeToXmlForBackup(out, configuration);
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_WIFI_CONFIGURATION);
        XmlUtil.writeNextSectionStart(out, XML_TAG_SECTION_HEADER_IP_CONFIGURATION);
        IpConfigurationXmlUtil.writeToXml(out, configuration.getIpConfiguration());
        XmlUtil.writeNextSectionEnd(out, XML_TAG_SECTION_HEADER_IP_CONFIGURATION);
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x0099 A:{Splitter: B:4:0x0008, ExcHandler: org.xmlpull.v1.XmlPullParserException (r1_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0099 A:{Splitter: B:4:0x0008, ExcHandler: org.xmlpull.v1.XmlPullParserException (r1_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0099 A:{Splitter: B:4:0x0008, ExcHandler: org.xmlpull.v1.XmlPullParserException (r1_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:24:0x0099, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:25:0x009a, code:
            r2 = TAG;
            r3 = new java.lang.StringBuilder();
            r3.append("Error parsing the backup data: ");
            r3.append(r1);
            android.util.Log.e(r2, r3.toString());
     */
    /* JADX WARNING: Missing block: B:26:0x00b0, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<WifiConfiguration> retrieveConfigurationsFromBackupData(byte[] data) {
        if (data == null || data.length == 0) {
            Log.e(TAG, "Invalid backup data received");
            return null;
        }
        try {
            int majorVersion;
            int minorVersion;
            if (this.mVerboseLoggingEnabled) {
                this.mDebugLastBackupDataRestored = data;
            }
            XmlPullParser in = Xml.newPullParser();
            in.setInput(new ByteArrayInputStream(data), StandardCharsets.UTF_8.name());
            XmlUtil.gotoDocumentStart(in, XML_TAG_DOCUMENT_HEADER);
            int rootTagDepth = in.getDepth();
            int minorVersion2 = -1;
            try {
                String versionStr = new Float(((Float) XmlUtil.readNextValueWithName(in, XML_TAG_VERSION)).floatValue()).toString();
                int separatorPos = versionStr.indexOf(46);
                if (separatorPos == -1) {
                    majorVersion = Integer.parseInt(versionStr);
                    minorVersion = 0;
                } else {
                    majorVersion = Integer.parseInt(versionStr.substring(0, separatorPos));
                    minorVersion = Integer.parseInt(versionStr.substring(separatorPos + 1));
                }
            } catch (ClassCastException e) {
                majorVersion = 1;
                minorVersion = 0;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Version of backup data - major: ");
            stringBuilder.append(majorVersion);
            stringBuilder.append("; minor: ");
            stringBuilder.append(minorVersion);
            Log.d(str, stringBuilder.toString());
            WifiBackupDataParser parser = getWifiBackupDataParser(majorVersion);
            if (parser != null) {
                return parser.parseNetworkConfigurationsFromXml(in, rootTagDepth, minorVersion);
            }
            Log.w(TAG, "Major version of backup data is unknown to this Android version; not restoring");
            return null;
        } catch (Exception e2) {
        }
    }

    private WifiBackupDataParser getWifiBackupDataParser(int majorVersion) {
        if (majorVersion == 1) {
            return new WifiBackupDataV1Parser();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unrecognized majorVersion of backup data: ");
        stringBuilder.append(majorVersion);
        Log.e(str, stringBuilder.toString());
        return null;
    }

    private String createLogFromBackupData(byte[] data) {
        StringBuilder sb = new StringBuilder();
        try {
            boolean wepKeysLine = false;
            for (String line : new String(data, StandardCharsets.UTF_8.name()).split("\n")) {
                String line2;
                if (line2.matches(PSK_MASK_LINE_MATCH_PATTERN)) {
                    line2 = line2.replaceAll(PSK_MASK_SEARCH_PATTERN, "$1*$3");
                }
                if (line2.matches(WEP_KEYS_MASK_LINE_START_MATCH_PATTERN)) {
                    wepKeysLine = true;
                } else if (line2.matches(WEP_KEYS_MASK_LINE_END_MATCH_PATTERN)) {
                    wepKeysLine = false;
                } else if (wepKeysLine) {
                    line2 = line2.replaceAll(WEP_KEYS_MASK_SEARCH_PATTERN, "$1*$3");
                }
                sb.append(line2);
                sb.append("\n");
            }
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public List<WifiConfiguration> retrieveConfigurationsFromSupplicantBackupData(byte[] supplicantData, byte[] ipConfigData) {
        if (supplicantData == null || supplicantData.length == 0) {
            Log.e(TAG, "Invalid supplicant backup data received");
            return null;
        }
        if (this.mVerboseLoggingEnabled) {
            this.mDebugLastSupplicantBackupDataRestored = supplicantData;
        }
        SupplicantNetworks supplicantNetworks = new SupplicantNetworks();
        char[] restoredAsChars = new char[supplicantData.length];
        int i = 0;
        for (int i2 = 0; i2 < supplicantData.length; i2++) {
            restoredAsChars[i2] = (char) supplicantData[i2];
        }
        supplicantNetworks.readNetworksFromStream(new BufferedReader(new CharArrayReader(restoredAsChars)));
        List<WifiConfiguration> configurations = supplicantNetworks.retrieveWifiConfigurations();
        if (ipConfigData == null || ipConfigData.length == 0) {
            Log.e(TAG, "Invalid ipconfig backup data received");
        } else {
            SparseArray<IpConfiguration> networks = IpConfigStore.readIpAndProxyConfigurations(new ByteArrayInputStream(ipConfigData));
            if (networks != null) {
                while (i < networks.size()) {
                    int id = networks.keyAt(i);
                    for (WifiConfiguration configuration : configurations) {
                        if (configuration.configKey().hashCode() == id) {
                            configuration.setIpConfiguration((IpConfiguration) networks.valueAt(i));
                        }
                    }
                    i++;
                }
            } else {
                Log.e(TAG, "Failed to parse ipconfig data");
            }
        }
        return configurations;
    }

    public void enableVerboseLogging(int verbose) {
        this.mVerboseLoggingEnabled = verbose > 0;
        if (!this.mVerboseLoggingEnabled) {
            this.mDebugLastBackupDataRetrieved = null;
            this.mDebugLastBackupDataRestored = null;
            this.mDebugLastSupplicantBackupDataRestored = null;
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder;
        pw.println("Dump of WifiBackupRestore");
        if (this.mDebugLastBackupDataRetrieved != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Last backup data retrieved: ");
            stringBuilder.append(createLogFromBackupData(this.mDebugLastBackupDataRetrieved));
            pw.println(stringBuilder.toString());
        }
        if (this.mDebugLastBackupDataRestored != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Last backup data restored: ");
            stringBuilder.append(createLogFromBackupData(this.mDebugLastBackupDataRestored));
            pw.println(stringBuilder.toString());
        }
        if (this.mDebugLastSupplicantBackupDataRestored != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Last old backup data restored: ");
            stringBuilder.append(SupplicantBackupMigration.createLogFromBackupData(this.mDebugLastSupplicantBackupDataRestored));
            pw.println(stringBuilder.toString());
        }
    }
}
