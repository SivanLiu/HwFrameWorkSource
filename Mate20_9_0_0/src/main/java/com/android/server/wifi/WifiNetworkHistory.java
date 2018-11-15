package com.android.server.wifi;

import android.content.Context;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.ReasonCode;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.net.wifi.WifiSsid;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.net.DelayedDiskWrite;
import com.android.server.net.DelayedDiskWrite.Writer;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WifiNetworkHistory {
    private static final String AUTH_KEY = "AUTH";
    private static final String BSSID_KEY = "BSSID";
    private static final String BSSID_KEY_END = "/BSSID";
    private static final String BSSID_STATUS_KEY = "BSSID_STATUS";
    private static final String CHOICE_KEY = "CHOICE";
    private static final String CHOICE_TIME_KEY = "CHOICE_TIME";
    private static final String CONFIG_BSSID_KEY = "CONFIG_BSSID";
    static final String CONFIG_KEY = "CONFIG";
    private static final String CONNECT_UID_KEY = "CONNECT_UID_KEY";
    private static final String CREATION_TIME_KEY = "CREATION_TIME";
    private static final String CREATOR_NAME_KEY = "CREATOR_NAME";
    static final String CREATOR_UID_KEY = "CREATOR_UID_KEY";
    private static final String DATE_KEY = "DATE";
    private static final boolean DBG = true;
    private static final String DEFAULT_GW_KEY = "DEFAULT_GW";
    private static final String DELETED_EPHEMERAL_KEY = "DELETED_EPHEMERAL";
    private static final String DID_SELF_ADD_KEY = "DID_SELF_ADD";
    private static final String EPHEMERAL_KEY = "EPHEMERAL";
    private static final String FQDN_KEY = "FQDN";
    private static final String FREQ_KEY = "FREQ";
    private static final String HAS_EVER_CONNECTED_KEY = "HAS_EVER_CONNECTED";
    private static final String LINK_KEY = "LINK";
    private static final String METERED_HINT_KEY = "METERED_HINT";
    private static final String METERED_OVERRIDE_KEY = "METERED_OVERRIDE";
    private static final String MILLI_KEY = "MILLI";
    static final String NETWORK_HISTORY_CONFIG_FILE;
    private static final String NETWORK_ID_KEY = "ID";
    private static final String NETWORK_SELECTION_DISABLE_REASON_KEY = "NETWORK_SELECTION_DISABLE_REASON";
    private static final String NETWORK_SELECTION_STATUS_KEY = "NETWORK_SELECTION_STATUS";
    private static final String NL = "\n";
    private static final String NO_INTERNET_ACCESS_EXPECTED_KEY = "NO_INTERNET_ACCESS_EXPECTED";
    private static final String NO_INTERNET_ACCESS_REPORTS_KEY = "NO_INTERNET_ACCESS_REPORTS";
    private static final String NUM_ASSOCIATION_KEY = "NUM_ASSOCIATION";
    private static final String PEER_CONFIGURATION_KEY = "PEER_CONFIGURATION";
    private static final String PRIORITY_KEY = "PRIORITY";
    private static final String RSSI_KEY = "RSSI";
    private static final String SCORER_OVERRIDE_AND_SWITCH_KEY = "SCORER_OVERRIDE_AND_SWITCH";
    private static final String SCORER_OVERRIDE_KEY = "SCORER_OVERRIDE";
    private static final String SELF_ADDED_KEY = "SELF_ADDED";
    private static final String SEPARATOR = ":  ";
    static final String SHARED_KEY = "SHARED";
    private static final String SSID_KEY = "SSID";
    public static final String TAG = "WifiNetworkHistory";
    private static final String UPDATE_NAME_KEY = "UPDATE_NAME";
    private static final String UPDATE_TIME_KEY = "UPDATE_TIME";
    private static final String UPDATE_UID_KEY = "UPDATE_UID";
    private static final String USER_APPROVED_KEY = "USER_APPROVED";
    private static final String USE_EXTERNAL_SCORES_KEY = "USE_EXTERNAL_SCORES";
    private static final String VALIDATED_INTERNET_ACCESS_KEY = "VALIDATED_INTERNET_ACCESS";
    private static final boolean VDBG = true;
    Context mContext;
    HashSet<String> mLostConfigsDbg = new HashSet();
    protected final DelayedDiskWrite mWriter;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getDataDirectory());
        stringBuilder.append("/misc/wifi/networkHistory.txt");
        NETWORK_HISTORY_CONFIG_FILE = stringBuilder.toString();
    }

    public WifiNetworkHistory(Context c, DelayedDiskWrite writer) {
        this.mContext = c;
        this.mWriter = writer;
    }

    public void writeKnownNetworkHistory(final List<WifiConfiguration> networks, final ConcurrentHashMap<Integer, ScanDetailCache> scanDetailCaches, final Set<String> deletedEphemeralSSIDs) {
        this.mWriter.write(NETWORK_HISTORY_CONFIG_FILE, new Writer() {
            public void onWriteCalled(DataOutputStream out) throws IOException {
                for (WifiConfiguration config : networks) {
                    String disableTime;
                    StringBuilder stringBuilder;
                    NetworkSelectionStatus status = config.getNetworkSelectionStatus();
                    int numlink = 0;
                    if (config.linkedConfigurations != null) {
                        numlink = config.linkedConfigurations.size();
                    }
                    if (config.getNetworkSelectionStatus().isNetworkEnabled()) {
                        disableTime = "";
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Disable time: ");
                        stringBuilder.append(DateFormat.getInstance().format(Long.valueOf(config.getNetworkSelectionStatus().getDisableTime())));
                        disableTime = stringBuilder.toString();
                    }
                    WifiNetworkHistory wifiNetworkHistory = WifiNetworkHistory.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("saving network history: ");
                    stringBuilder2.append(config.configKey());
                    stringBuilder2.append(" gw: ");
                    stringBuilder2.append(config.defaultGwMacAddress);
                    stringBuilder2.append(" Network Selection-status: ");
                    stringBuilder2.append(status.getNetworkStatusString());
                    stringBuilder2.append(disableTime);
                    stringBuilder2.append(" ephemeral=");
                    stringBuilder2.append(config.ephemeral);
                    stringBuilder2.append(" choice:");
                    stringBuilder2.append(status.getConnectChoice());
                    stringBuilder2.append(" link:");
                    stringBuilder2.append(numlink);
                    stringBuilder2.append(" status:");
                    stringBuilder2.append(config.status);
                    stringBuilder2.append(" nid:");
                    stringBuilder2.append(config.networkId);
                    stringBuilder2.append(" hasEverConnected: ");
                    stringBuilder2.append(status.getHasEverConnected());
                    wifiNetworkHistory.logd(stringBuilder2.toString());
                    if (WifiNetworkHistory.this.isValid(config)) {
                        if (config.SSID == null) {
                            WifiNetworkHistory.this.logv("writeKnownNetworkHistory trying to write config with null SSID");
                        } else {
                            StringBuilder stringBuilder3;
                            WifiNetworkHistory wifiNetworkHistory2 = WifiNetworkHistory.this;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("writeKnownNetworkHistory write config ");
                            stringBuilder.append(config.configKey());
                            wifiNetworkHistory2.logv(stringBuilder.toString());
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("CONFIG:  ");
                            stringBuilder4.append(config.configKey());
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            if (config.SSID != null) {
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("SSID:  ");
                                stringBuilder4.append(config.SSID);
                                stringBuilder4.append(WifiNetworkHistory.NL);
                                out.writeUTF(stringBuilder4.toString());
                            }
                            if (config.BSSID != null) {
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("CONFIG_BSSID:  ");
                                stringBuilder4.append(config.BSSID);
                                stringBuilder4.append(WifiNetworkHistory.NL);
                                out.writeUTF(stringBuilder4.toString());
                            } else {
                                out.writeUTF("CONFIG_BSSID:  null\n");
                            }
                            if (config.FQDN != null) {
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("FQDN:  ");
                                stringBuilder4.append(config.FQDN);
                                stringBuilder4.append(WifiNetworkHistory.NL);
                                out.writeUTF(stringBuilder4.toString());
                            }
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("PRIORITY:  ");
                            stringBuilder4.append(Integer.toString(config.priority));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("ID:  ");
                            stringBuilder4.append(Integer.toString(config.networkId));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("SELF_ADDED:  ");
                            stringBuilder4.append(Boolean.toString(config.selfAdded));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("DID_SELF_ADD:  ");
                            stringBuilder4.append(Boolean.toString(config.didSelfAdd));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("NO_INTERNET_ACCESS_REPORTS:  ");
                            stringBuilder4.append(Integer.toString(config.numNoInternetAccessReports));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("VALIDATED_INTERNET_ACCESS:  ");
                            stringBuilder4.append(Boolean.toString(config.validatedInternetAccess));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("NO_INTERNET_ACCESS_EXPECTED:  ");
                            stringBuilder4.append(Boolean.toString(config.noInternetAccessExpected));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("EPHEMERAL:  ");
                            stringBuilder4.append(Boolean.toString(config.ephemeral));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("METERED_HINT:  ");
                            stringBuilder4.append(Boolean.toString(config.meteredHint));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("METERED_OVERRIDE:  ");
                            stringBuilder4.append(Integer.toString(config.meteredOverride));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("USE_EXTERNAL_SCORES:  ");
                            stringBuilder4.append(Boolean.toString(config.useExternalScores));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            if (config.creationTime != null) {
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("CREATION_TIME:  ");
                                stringBuilder4.append(config.creationTime);
                                stringBuilder4.append(WifiNetworkHistory.NL);
                                out.writeUTF(stringBuilder4.toString());
                            }
                            if (config.updateTime != null) {
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("UPDATE_TIME:  ");
                                stringBuilder4.append(config.updateTime);
                                stringBuilder4.append(WifiNetworkHistory.NL);
                                out.writeUTF(stringBuilder4.toString());
                            }
                            if (config.peerWifiConfiguration != null) {
                                stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("PEER_CONFIGURATION:  ");
                                stringBuilder4.append(config.peerWifiConfiguration);
                                stringBuilder4.append(WifiNetworkHistory.NL);
                                out.writeUTF(stringBuilder4.toString());
                            }
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("SCORER_OVERRIDE:  ");
                            stringBuilder4.append(Integer.toString(config.numScorerOverride));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("SCORER_OVERRIDE_AND_SWITCH:  ");
                            stringBuilder4.append(Integer.toString(config.numScorerOverrideAndSwitchedNetwork));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("NUM_ASSOCIATION:  ");
                            stringBuilder4.append(Integer.toString(config.numAssociation));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("CREATOR_UID_KEY:  ");
                            stringBuilder4.append(Integer.toString(config.creatorUid));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("CONNECT_UID_KEY:  ");
                            stringBuilder4.append(Integer.toString(config.lastConnectUid));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("UPDATE_UID:  ");
                            stringBuilder4.append(Integer.toString(config.lastUpdateUid));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("CREATOR_NAME:  ");
                            stringBuilder4.append(config.creatorName);
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("UPDATE_NAME:  ");
                            stringBuilder4.append(config.lastUpdateName);
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("USER_APPROVED:  ");
                            stringBuilder4.append(Integer.toString(config.userApproved));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("SHARED:  ");
                            stringBuilder4.append(Boolean.toString(config.shared));
                            stringBuilder4.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder4.toString());
                            String allowedKeyManagementString = WifiNetworkHistory.makeString(config.allowedKeyManagement, KeyMgmt.strings);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("AUTH:  ");
                            stringBuilder.append(allowedKeyManagementString);
                            stringBuilder.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder.toString());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("NETWORK_SELECTION_STATUS:  ");
                            stringBuilder.append(status.getNetworkSelectionStatus());
                            stringBuilder.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder.toString());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("NETWORK_SELECTION_DISABLE_REASON:  ");
                            stringBuilder.append(status.getNetworkSelectionDisableReason());
                            stringBuilder.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder.toString());
                            if (status.getConnectChoice() != null) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("CHOICE:  ");
                                stringBuilder.append(status.getConnectChoice());
                                stringBuilder.append(WifiNetworkHistory.NL);
                                out.writeUTF(stringBuilder.toString());
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("CHOICE_TIME:  ");
                                stringBuilder.append(status.getConnectChoiceTimestamp());
                                stringBuilder.append(WifiNetworkHistory.NL);
                                out.writeUTF(stringBuilder.toString());
                            }
                            if (config.linkedConfigurations != null) {
                                WifiNetworkHistory wifiNetworkHistory3 = WifiNetworkHistory.this;
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("writeKnownNetworkHistory write linked ");
                                stringBuilder3.append(config.linkedConfigurations.size());
                                wifiNetworkHistory3.log(stringBuilder3.toString());
                                for (String key : config.linkedConfigurations.keySet()) {
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("LINK:  ");
                                    stringBuilder2.append(key);
                                    stringBuilder2.append(WifiNetworkHistory.NL);
                                    out.writeUTF(stringBuilder2.toString());
                                }
                            }
                            disableTime = config.defaultGwMacAddress;
                            if (disableTime != null) {
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("DEFAULT_GW:  ");
                                stringBuilder3.append(disableTime);
                                stringBuilder3.append(WifiNetworkHistory.NL);
                                out.writeUTF(stringBuilder3.toString());
                            }
                            if (WifiNetworkHistory.this.getScanDetailCache(config, scanDetailCaches) != null) {
                                for (ScanDetail scanDetail : WifiNetworkHistory.this.getScanDetailCache(config, scanDetailCaches).values()) {
                                    ScanResult result = scanDetail.getScanResult();
                                    StringBuilder stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("BSSID:  ");
                                    stringBuilder5.append(result.BSSID);
                                    stringBuilder5.append(WifiNetworkHistory.NL);
                                    out.writeUTF(stringBuilder5.toString());
                                    stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("FREQ:  ");
                                    stringBuilder5.append(Integer.toString(result.frequency));
                                    stringBuilder5.append(WifiNetworkHistory.NL);
                                    out.writeUTF(stringBuilder5.toString());
                                    stringBuilder5 = new StringBuilder();
                                    stringBuilder5.append("RSSI:  ");
                                    stringBuilder5.append(Integer.toString(result.level));
                                    stringBuilder5.append(WifiNetworkHistory.NL);
                                    out.writeUTF(stringBuilder5.toString());
                                    out.writeUTF("/BSSID\n");
                                }
                            }
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("HAS_EVER_CONNECTED:  ");
                            stringBuilder3.append(Boolean.toString(status.getHasEverConnected()));
                            stringBuilder3.append(WifiNetworkHistory.NL);
                            out.writeUTF(stringBuilder3.toString());
                            out.writeUTF(WifiNetworkHistory.NL);
                            out.writeUTF(WifiNetworkHistory.NL);
                            out.writeUTF(WifiNetworkHistory.NL);
                        }
                    }
                }
                if (deletedEphemeralSSIDs != null && deletedEphemeralSSIDs.size() > 0) {
                    for (String ssid : deletedEphemeralSSIDs) {
                        out.writeUTF(WifiNetworkHistory.DELETED_EPHEMERAL_KEY);
                        out.writeUTF(ssid);
                        out.writeUTF(WifiNetworkHistory.NL);
                    }
                }
            }
        });
    }

    /* JADX WARNING: Removed duplicated region for block: B:247:0x05c1 A:{Catch:{ EOFException -> 0x05b9, FileNotFoundException -> 0x05b7, NumberFormatException -> 0x05b5, IOException -> 0x05b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:237:0x05af A:{SYNTHETIC, Splitter: B:237:0x05af} */
    /* JADX WARNING: Missing block: B:159:0x02d9, code:
            r9 = r32;
     */
    /* JADX WARNING: Missing block: B:160:0x02db, code:
            r25 = r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void readNetworkHistory(Map<String, WifiConfiguration> configs, Map<Integer, ScanDetailCache> scanDetailCaches, Set<String> deletedEphemeralSSIDs) {
        Throwable th;
        Throwable th2;
        Throwable th3;
        FileNotFoundException e;
        String str;
        StringBuilder stringBuilder;
        NumberFormatException e2;
        IOException e3;
        Map<Integer, ScanDetailCache> map = scanDetailCaches;
        Set<String> set;
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(NETWORK_HISTORY_CONFIG_FILE)));
            try {
                long seen = 0;
                int rssi = WifiConfiguration.INVALID_RSSI;
                int freq = 0;
                int status = 0;
                String caps = null;
                String ssid = null;
                String bssid = null;
                WifiConfiguration config = null;
                while (true) {
                    String line = in.readUTF();
                    if (line == null) {
                        in.close();
                        set = deletedEphemeralSSIDs;
                        return;
                    }
                    int colon = line.indexOf(58);
                    if (colon >= 0) {
                        int status2;
                        String key = line.substring(0, colon).trim();
                        String value = line.substring(colon + 1).trim();
                        String str2;
                        if (key.equals(CONFIG_KEY)) {
                            config = (WifiConfiguration) configs.get(value);
                            StringBuilder stringBuilder2;
                            if (config == null) {
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                status2 = status;
                                stringBuilder2.append("readNetworkHistory didnt find netid for hash=");
                                stringBuilder2.append(Integer.toString(value.hashCode()));
                                stringBuilder2.append(" key: ");
                                stringBuilder2.append(value);
                                Log.e(str2, stringBuilder2.toString());
                                this.mLostConfigsDbg.add(value);
                                status = status2;
                            } else {
                                status2 = status;
                                if (config.creatorName == null || config.lastUpdateName == null) {
                                    config.creatorName = this.mContext.getPackageManager().getNameForUid(1000);
                                    config.lastUpdateName = config.creatorName;
                                    String str3 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("Upgrading network ");
                                    stringBuilder2.append(config.networkId);
                                    stringBuilder2.append(" to ");
                                    stringBuilder2.append(config.creatorName);
                                    Log.w(str3, stringBuilder2.toString());
                                }
                                set = deletedEphemeralSSIDs;
                            }
                        } else {
                            String str4;
                            Map<String, WifiConfiguration> map2 = configs;
                            status2 = status;
                            if (config != null) {
                                Object obj;
                                status = config.getNetworkSelectionStatus();
                                switch (key.hashCode()) {
                                    case -1946896213:
                                        if (key.equals(USER_APPROVED_KEY)) {
                                            obj = 35;
                                            break;
                                        }
                                    case -1866906821:
                                        if (key.equals(CONNECT_UID_KEY)) {
                                            obj = 19;
                                            break;
                                        }
                                    case -1865201711:
                                        if (key.equals(VALIDATED_INTERNET_ACCESS_KEY)) {
                                            obj = 7;
                                            break;
                                        }
                                    case -1850236827:
                                        if (key.equals(SHARED_KEY)) {
                                            obj = 36;
                                            break;
                                        }
                                    case -1842190690:
                                        if (key.equals(METERED_HINT_KEY)) {
                                            obj = 12;
                                            break;
                                        }
                                    case -1646996988:
                                        if (key.equals(NO_INTERNET_ACCESS_REPORTS_KEY)) {
                                            obj = 6;
                                            break;
                                        }
                                    case -1103645511:
                                        if (key.equals(PEER_CONFIGURATION_KEY)) {
                                            obj = 21;
                                            break;
                                        }
                                    case -1025978637:
                                        if (key.equals(NO_INTERNET_ACCESS_EXPECTED_KEY)) {
                                            obj = 8;
                                            break;
                                        }
                                    case -944985731:
                                        if (key.equals(EPHEMERAL_KEY)) {
                                            obj = 11;
                                            break;
                                        }
                                    case -552300306:
                                        if (key.equals(DID_SELF_ADD_KEY)) {
                                            obj = 5;
                                            break;
                                        }
                                    case -534910080:
                                        if (key.equals(CONFIG_BSSID_KEY)) {
                                            obj = 1;
                                            break;
                                        }
                                    case -375674826:
                                        if (key.equals(NETWORK_SELECTION_STATUS_KEY)) {
                                            obj = 22;
                                            break;
                                        }
                                    case 2090926:
                                        if (key.equals(DATE_KEY)) {
                                            obj = 30;
                                            break;
                                        }
                                    case 2165397:
                                        if (key.equals("FQDN")) {
                                            obj = 2;
                                            break;
                                        }
                                    case 2166392:
                                        if (key.equals(FREQ_KEY)) {
                                            obj = 29;
                                            break;
                                        }
                                    case 2336762:
                                        if (key.equals(LINK_KEY)) {
                                            obj = 26;
                                            break;
                                        }
                                    case 2525271:
                                        if (key.equals(RSSI_KEY)) {
                                            obj = 28;
                                            break;
                                        }
                                    case 2554747:
                                        if (key.equals("SSID")) {
                                            obj = null;
                                            break;
                                        }
                                    case 63507133:
                                        if (key.equals("BSSID")) {
                                            obj = 27;
                                            break;
                                        }
                                    case 89250059:
                                        if (key.equals(SCORER_OVERRIDE_KEY)) {
                                            obj = 16;
                                            break;
                                        }
                                    case 190453690:
                                        if (key.equals(UPDATE_UID_KEY)) {
                                            obj = 20;
                                            break;
                                        }
                                    case 417823927:
                                        if (key.equals(DELETED_EPHEMERAL_KEY)) {
                                            obj = 32;
                                            break;
                                        }
                                    case 501180973:
                                        if (key.equals(SELF_ADDED_KEY)) {
                                            obj = 4;
                                            break;
                                        }
                                    case 740738782:
                                        if (key.equals(CREATOR_NAME_KEY)) {
                                            obj = 33;
                                            break;
                                        }
                                    case 782347629:
                                        if (key.equals(HAS_EVER_CONNECTED_KEY)) {
                                            obj = 37;
                                            break;
                                        }
                                    case 783161389:
                                        if (key.equals(CREATION_TIME_KEY)) {
                                            obj = 9;
                                            break;
                                        }
                                    case 1163774926:
                                        if (key.equals(DEFAULT_GW_KEY)) {
                                            obj = 3;
                                            break;
                                        }
                                    case 1187625059:
                                        if (key.equals(METERED_OVERRIDE_KEY)) {
                                            obj = 13;
                                            break;
                                        }
                                    case 1366197215:
                                        if (key.equals(NETWORK_SELECTION_DISABLE_REASON_KEY)) {
                                            obj = 23;
                                            break;
                                        }
                                    case 1409077230:
                                        if (key.equals(BSSID_KEY_END)) {
                                            obj = 31;
                                            break;
                                        }
                                    case 1477121648:
                                        if (key.equals(SCORER_OVERRIDE_AND_SWITCH_KEY)) {
                                            obj = 17;
                                            break;
                                        }
                                    case 1608881217:
                                        if (key.equals(UPDATE_NAME_KEY)) {
                                            obj = 34;
                                            break;
                                        }
                                    case 1609067651:
                                        if (key.equals(UPDATE_TIME_KEY)) {
                                            obj = 10;
                                            break;
                                        }
                                    case 1614319275:
                                        if (key.equals(CHOICE_TIME_KEY)) {
                                            obj = 25;
                                            break;
                                        }
                                    case 1928336648:
                                        if (key.equals(NUM_ASSOCIATION_KEY)) {
                                            obj = 18;
                                            break;
                                        }
                                    case 1946216573:
                                        if (key.equals(CREATOR_UID_KEY)) {
                                            obj = 15;
                                            break;
                                        }
                                    case 1987072417:
                                        if (key.equals(CHOICE_KEY)) {
                                            obj = 24;
                                            break;
                                        }
                                    case 2026657853:
                                        if (key.equals(USE_EXTERNAL_SCORES_KEY)) {
                                            obj = 14;
                                            break;
                                        }
                                    default:
                                        obj = -1;
                                        break;
                                }
                                String str5;
                                int i;
                                String str6;
                                switch (obj) {
                                    case null:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        ssid = value;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        if (!config.isPasspoint()) {
                                            str2 = ssid;
                                            if (config.SSID == null || config.SSID.equals(str2)) {
                                                config.SSID = str2;
                                            } else {
                                                loge("Error parsing network history file, mismatched SSIDs");
                                                config = null;
                                                str2 = null;
                                            }
                                            ssid = str2;
                                            break;
                                        }
                                        break;
                                    case 1:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        ssid = value;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.BSSID = ssid.equals("null") ? null : ssid;
                                        break;
                                    case 2:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        ssid = value;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.FQDN = ssid.equals("null") ? null : ssid;
                                        break;
                                    case 3:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.defaultGwMacAddress = value;
                                        break;
                                    case 4:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.selfAdded = Boolean.parseBoolean(value);
                                        break;
                                    case 5:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.didSelfAdd = Boolean.parseBoolean(value);
                                        break;
                                    case 6:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.numNoInternetAccessReports = Integer.parseInt(value);
                                        break;
                                    case 7:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.validatedInternetAccess = Boolean.parseBoolean(value);
                                        break;
                                    case 8:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.noInternetAccessExpected = Boolean.parseBoolean(value);
                                        break;
                                    case 9:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.creationTime = value;
                                        break;
                                    case 10:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.updateTime = value;
                                        break;
                                    case 11:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.ephemeral = Boolean.parseBoolean(value);
                                        break;
                                    case 12:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.meteredHint = Boolean.parseBoolean(value);
                                        break;
                                    case 13:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.meteredOverride = Integer.parseInt(value);
                                        break;
                                    case 14:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.useExternalScores = Boolean.parseBoolean(value);
                                        break;
                                    case 15:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.creatorUid = Integer.parseInt(value);
                                        break;
                                    case 16:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.numScorerOverride = Integer.parseInt(value);
                                        break;
                                    case 17:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.numScorerOverrideAndSwitchedNetwork = Integer.parseInt(value);
                                        break;
                                    case 18:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.numAssociation = Integer.parseInt(value);
                                        break;
                                    case 19:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.lastConnectUid = Integer.parseInt(value);
                                        break;
                                    case 20:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.lastUpdateUid = Integer.parseInt(value);
                                        break;
                                    case ReasonCode.UNSUPPORTED_RSN_IE_VERSION /*21*/:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        config.peerWifiConfiguration = value;
                                        break;
                                    case 22:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        int networkStatusValue = Integer.parseInt(value);
                                        if (networkStatusValue == 1) {
                                            networkStatusValue = 0;
                                        }
                                        status.setNetworkSelectionStatus(networkStatusValue);
                                        break;
                                    case 23:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        status.setNetworkSelectionDisableReason(Integer.parseInt(value));
                                        break;
                                    case 24:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        status.setConnectChoice(value);
                                        break;
                                    case 25:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        status.setConnectChoiceTimestamp(Long.parseLong(value));
                                        break;
                                    case ReasonCode.TDLS_TEARDOWN_UNSPECIFIED /*26*/:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        ssid = value;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        if (config.linkedConfigurations != null) {
                                            config.linkedConfigurations.put(ssid, Integer.valueOf(-1));
                                            break;
                                        } else {
                                            config.linkedConfigurations = new HashMap();
                                            break;
                                        }
                                    case 27:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        bssid = null;
                                        freq = 0;
                                        caps = "";
                                        status = 0;
                                        ssid = null;
                                        seen = null;
                                        rssi = WifiConfiguration.INVALID_RSSI;
                                        break;
                                    case 28:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        rssi = Integer.parseInt(value);
                                        break;
                                    case 29:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        freq = Integer.parseInt(value);
                                        break;
                                    case 30:
                                        set = deletedEphemeralSSIDs;
                                        str4 = ssid;
                                        ssid = value;
                                        str5 = key;
                                        i = colon;
                                        str6 = line;
                                        break;
                                    case 31:
                                        set = deletedEphemeralSSIDs;
                                        if (!(bssid == null || ssid == null || getScanDetailCache(config, map) == null)) {
                                            str4 = ssid;
                                            ssid = value;
                                            getScanDetailCache(config, map).put(new ScanDetail(WifiSsid.createFromAsciiEncoded(ssid), bssid, caps, rssi, freq, 0, seen));
                                            break;
                                        }
                                    case 32:
                                        if (!TextUtils.isEmpty(value)) {
                                            try {
                                                deletedEphemeralSSIDs.add(value);
                                                break;
                                            } catch (Throwable th4) {
                                                th = th4;
                                                th2 = th;
                                                th3 = null;
                                                if (th3 == null) {
                                                    try {
                                                        in.close();
                                                    } catch (Throwable th5) {
                                                        try {
                                                            th3.addSuppressed(th5);
                                                        } catch (EOFException e4) {
                                                            return;
                                                        } catch (FileNotFoundException e5) {
                                                            e = e5;
                                                            str = TAG;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("readNetworkHistory: no config file, ");
                                                            stringBuilder.append(e);
                                                            Log.i(str, stringBuilder.toString());
                                                        } catch (NumberFormatException e6) {
                                                            e2 = e6;
                                                            str = TAG;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("readNetworkHistory: failed to parse, ");
                                                            stringBuilder.append(e2);
                                                            Log.e(str, stringBuilder.toString(), e2);
                                                        } catch (IOException e7) {
                                                            e3 = e7;
                                                            str = TAG;
                                                            stringBuilder = new StringBuilder();
                                                            stringBuilder.append("readNetworkHistory: failed to read, ");
                                                            stringBuilder.append(e3);
                                                            Log.e(str, stringBuilder.toString(), e3);
                                                        }
                                                    }
                                                }
                                                in.close();
                                                throw th2;
                                            }
                                        }
                                        break;
                                    case 33:
                                        config.creatorName = value;
                                        break;
                                    case 34:
                                        config.lastUpdateName = value;
                                        break;
                                    case 35:
                                        config.userApproved = Integer.parseInt(value);
                                        break;
                                    case ReasonCode.STA_LEAVING /*36*/:
                                        config.shared = Boolean.parseBoolean(value);
                                        break;
                                    case 37:
                                        status.setHasEverConnected(Boolean.parseBoolean(value));
                                        break;
                                }
                            }
                            set = deletedEphemeralSSIDs;
                            str4 = ssid;
                            status = status2;
                            ssid = str4;
                        }
                        status = status2;
                    }
                }
            } catch (Throwable th6) {
                th5 = th6;
                set = deletedEphemeralSSIDs;
                th2 = th5;
                th3 = null;
                if (th3 == null) {
                }
                throw th2;
            }
        } catch (EOFException e8) {
            set = deletedEphemeralSSIDs;
        } catch (FileNotFoundException e9) {
            e = e9;
            set = deletedEphemeralSSIDs;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("readNetworkHistory: no config file, ");
            stringBuilder.append(e);
            Log.i(str, stringBuilder.toString());
        } catch (NumberFormatException e10) {
            e2 = e10;
            set = deletedEphemeralSSIDs;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("readNetworkHistory: failed to parse, ");
            stringBuilder.append(e2);
            Log.e(str, stringBuilder.toString(), e2);
        } catch (IOException e11) {
            e3 = e11;
            set = deletedEphemeralSSIDs;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("readNetworkHistory: failed to read, ");
            stringBuilder.append(e3);
            Log.e(str, stringBuilder.toString(), e3);
        }
    }

    public boolean isValid(WifiConfiguration config) {
        if (config.allowedKeyManagement == null) {
            return false;
        }
        if (config.allowedKeyManagement.cardinality() > 1) {
            if (config.allowedKeyManagement.cardinality() != 2 || !config.allowedKeyManagement.get(2)) {
                return false;
            }
            if (config.allowedKeyManagement.get(3) || config.allowedKeyManagement.get(1)) {
                return true;
            }
            return false;
        }
        return true;
    }

    private static String makeString(BitSet set, String[] strings) {
        StringBuffer buf = new StringBuffer();
        int nextSetBit = -1;
        set = set.get(0, strings.length);
        while (true) {
            int nextSetBit2 = set.nextSetBit(nextSetBit + 1);
            nextSetBit = nextSetBit2;
            if (nextSetBit2 == -1) {
                break;
            }
            buf.append(strings[nextSetBit].replace('_', '-'));
            buf.append(' ');
        }
        if (set.cardinality() > 0) {
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }

    protected void logv(String s) {
        Log.v(TAG, s);
    }

    protected void logd(String s) {
        Log.d(TAG, s);
    }

    protected void log(String s) {
        Log.d(TAG, s);
    }

    protected void loge(String s) {
        loge(s, false);
    }

    protected void loge(String s, boolean stack) {
        if (stack) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(s);
            stringBuilder.append(" stack:");
            stringBuilder.append(Thread.currentThread().getStackTrace()[2].getMethodName());
            stringBuilder.append(" - ");
            stringBuilder.append(Thread.currentThread().getStackTrace()[3].getMethodName());
            stringBuilder.append(" - ");
            stringBuilder.append(Thread.currentThread().getStackTrace()[4].getMethodName());
            stringBuilder.append(" - ");
            stringBuilder.append(Thread.currentThread().getStackTrace()[5].getMethodName());
            Log.e(str, stringBuilder.toString());
            return;
        }
        Log.e(TAG, s);
    }

    private ScanDetailCache getScanDetailCache(WifiConfiguration config, Map<Integer, ScanDetailCache> scanDetailCaches) {
        if (config == null || scanDetailCaches == null) {
            return null;
        }
        ScanDetailCache cache = (ScanDetailCache) scanDetailCaches.get(Integer.valueOf(config.networkId));
        if (cache == null && config.networkId != -1) {
            cache = new ScanDetailCache(config, WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE, 128);
            scanDetailCaches.put(Integer.valueOf(config.networkId), cache);
        }
        return cache;
    }
}
