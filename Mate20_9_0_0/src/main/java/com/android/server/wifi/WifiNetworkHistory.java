package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;
import android.os.Environment;
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

    /* JADX WARNING: Removed duplicated region for block: B:250:0x05c1 A:{Catch:{ EOFException -> 0x05b9, FileNotFoundException -> 0x05b7, NumberFormatException -> 0x05b5, IOException -> 0x05b3 }} */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x05af A:{SYNTHETIC, Splitter:B:240:0x05af} */
    /* JADX WARNING: Missing block: B:148:0x02ab, code skipped:
            r9 = -1;
     */
    /* JADX WARNING: Missing block: B:149:0x02ac, code skipped:
            switch(r9) {
                case 0: goto L_0x0564;
                case 1: goto L_0x054b;
                case 2: goto L_0x0532;
                case 3: goto L_0x0523;
                case 4: goto L_0x0510;
                case 5: goto L_0x04fd;
                case 6: goto L_0x04ea;
                case 7: goto L_0x04d7;
                case 8: goto L_0x04c4;
                case 9: goto L_0x04b5;
                case 10: goto L_0x04a6;
                case 11: goto L_0x0493;
                case 12: goto L_0x0480;
                case 13: goto L_0x046d;
                case 14: goto L_0x045a;
                case 15: goto L_0x0447;
                case 16: goto L_0x0434;
                case 17: goto L_0x0421;
                case 18: goto L_0x040e;
                case 19: goto L_0x03fb;
                case 20: goto L_0x03e8;
                case android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.ReasonCode.UNSUPPORTED_RSN_IE_VERSION :int: goto L_0x03d9;
                case 22: goto L_0x03c2;
                case 23: goto L_0x03ae;
                case 24: goto L_0x039e;
                case 25: goto L_0x038a;
                case android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.ReasonCode.TDLS_TEARDOWN_UNSPECIFIED :int: goto L_0x0366;
                case 27: goto L_0x0347;
                case 28: goto L_0x0333;
                case 29: goto L_0x0321;
                case 30: goto L_0x0314;
                case 31: goto L_0x02df;
                case 32: goto L_0x02cd;
                case 33: goto L_0x02ca;
                case 34: goto L_0x02c7;
                case 35: goto L_0x02c0;
                case android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.ReasonCode.STA_LEAVING :int: goto L_0x02b9;
                case 37: goto L_0x02b1;
                default: goto L_0x02af;
            };
     */
    /* JADX WARNING: Missing block: B:151:0x02b1, code skipped:
            r8.setHasEverConnected(java.lang.Boolean.parseBoolean(r12));
     */
    /* JADX WARNING: Missing block: B:152:0x02b9, code skipped:
            r0.shared = java.lang.Boolean.parseBoolean(r12);
     */
    /* JADX WARNING: Missing block: B:153:0x02c0, code skipped:
            r0.userApproved = java.lang.Integer.parseInt(r12);
     */
    /* JADX WARNING: Missing block: B:154:0x02c7, code skipped:
            r0.lastUpdateName = r12;
     */
    /* JADX WARNING: Missing block: B:155:0x02ca, code skipped:
            r0.creatorName = r12;
     */
    /* JADX WARNING: Missing block: B:157:0x02d1, code skipped:
            if (android.text.TextUtils.isEmpty(r12) != false) goto L_0x02d9;
     */
    /* JADX WARNING: Missing block: B:160:?, code skipped:
            r32.add(r12);
     */
    /* JADX WARNING: Missing block: B:161:0x02d9, code skipped:
            r9 = r32;
     */
    /* JADX WARNING: Missing block: B:162:0x02db, code skipped:
            r25 = r5;
     */
    /* JADX WARNING: Missing block: B:163:0x02df, code skipped:
            r9 = r32;
     */
    /* JADX WARNING: Missing block: B:164:0x02e1, code skipped:
            if (r4 == null) goto L_0x0597;
     */
    /* JADX WARNING: Missing block: B:165:0x02e3, code skipped:
            if (r5 == null) goto L_0x0597;
     */
    /* JADX WARNING: Missing block: B:167:0x02e9, code skipped:
            if (getScanDetailCache(r0, r2) == null) goto L_0x0597;
     */
    /* JADX WARNING: Missing block: B:168:0x02eb, code skipped:
            r25 = r5;
            r5 = r12;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            getScanDetailCache(r0, r2).put(new com.android.server.wifi.ScanDetail(android.net.wifi.WifiSsid.createFromAsciiEncoded(r5), r4, r6, r20, r7, 0, r21));
     */
    /* JADX WARNING: Missing block: B:169:0x0314, code skipped:
            r9 = r32;
            r25 = r5;
            r5 = r12;
            r26 = r13;
            r27 = r14;
            r28 = r15;
     */
    /* JADX WARNING: Missing block: B:170:0x0321, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r7 = java.lang.Integer.parseInt(r12);
     */
    /* JADX WARNING: Missing block: B:171:0x0333, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r20 = java.lang.Integer.parseInt(r12);
     */
    /* JADX WARNING: Missing block: B:172:0x0347, code skipped:
            r9 = r32;
            r25 = r5;
            r5 = r12;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r4 = null;
            r7 = 0;
            r6 = "";
            r8 = 0;
            r5 = null;
            r21 = null;
            r20 = android.net.wifi.WifiConfiguration.INVALID_RSSI;
     */
    /* JADX WARNING: Missing block: B:173:0x0366, code skipped:
            r9 = r32;
            r25 = r5;
            r5 = r12;
            r26 = r13;
            r27 = r14;
            r28 = r15;
     */
    /* JADX WARNING: Missing block: B:174:0x0373, code skipped:
            if (r0.linkedConfigurations != null) goto L_0x037e;
     */
    /* JADX WARNING: Missing block: B:175:0x0375, code skipped:
            r0.linkedConfigurations = new java.util.HashMap();
     */
    /* JADX WARNING: Missing block: B:176:0x037e, code skipped:
            r0.linkedConfigurations.put(r5, java.lang.Integer.valueOf(-1));
     */
    /* JADX WARNING: Missing block: B:177:0x038a, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r8.setConnectChoiceTimestamp(java.lang.Long.parseLong(r12));
     */
    /* JADX WARNING: Missing block: B:178:0x039e, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r8.setConnectChoice(r12);
     */
    /* JADX WARNING: Missing block: B:179:0x03ae, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r8.setNetworkSelectionDisableReason(java.lang.Integer.parseInt(r12));
     */
    /* JADX WARNING: Missing block: B:180:0x03c2, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r11 = java.lang.Integer.parseInt(r12);
     */
    /* JADX WARNING: Missing block: B:181:0x03d1, code skipped:
            if (r11 != 1) goto L_0x03d4;
     */
    /* JADX WARNING: Missing block: B:182:0x03d3, code skipped:
            r11 = 0;
     */
    /* JADX WARNING: Missing block: B:183:0x03d4, code skipped:
            r8.setNetworkSelectionStatus(r11);
     */
    /* JADX WARNING: Missing block: B:184:0x03d9, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.peerWifiConfiguration = r12;
     */
    /* JADX WARNING: Missing block: B:185:0x03e8, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.lastUpdateUid = java.lang.Integer.parseInt(r12);
     */
    /* JADX WARNING: Missing block: B:186:0x03fb, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.lastConnectUid = java.lang.Integer.parseInt(r12);
     */
    /* JADX WARNING: Missing block: B:187:0x040e, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.numAssociation = java.lang.Integer.parseInt(r12);
     */
    /* JADX WARNING: Missing block: B:188:0x0421, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.numScorerOverrideAndSwitchedNetwork = java.lang.Integer.parseInt(r12);
     */
    /* JADX WARNING: Missing block: B:189:0x0434, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.numScorerOverride = java.lang.Integer.parseInt(r12);
     */
    /* JADX WARNING: Missing block: B:190:0x0447, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.creatorUid = java.lang.Integer.parseInt(r12);
     */
    /* JADX WARNING: Missing block: B:191:0x045a, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.useExternalScores = java.lang.Boolean.parseBoolean(r12);
     */
    /* JADX WARNING: Missing block: B:192:0x046d, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.meteredOverride = java.lang.Integer.parseInt(r12);
     */
    /* JADX WARNING: Missing block: B:193:0x0480, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.meteredHint = java.lang.Boolean.parseBoolean(r12);
     */
    /* JADX WARNING: Missing block: B:194:0x0493, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.ephemeral = java.lang.Boolean.parseBoolean(r12);
     */
    /* JADX WARNING: Missing block: B:195:0x04a6, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.updateTime = r12;
     */
    /* JADX WARNING: Missing block: B:196:0x04b5, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.creationTime = r12;
     */
    /* JADX WARNING: Missing block: B:197:0x04c4, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.noInternetAccessExpected = java.lang.Boolean.parseBoolean(r12);
     */
    /* JADX WARNING: Missing block: B:198:0x04d7, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.validatedInternetAccess = java.lang.Boolean.parseBoolean(r12);
     */
    /* JADX WARNING: Missing block: B:199:0x04ea, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.numNoInternetAccessReports = java.lang.Integer.parseInt(r12);
     */
    /* JADX WARNING: Missing block: B:200:0x04fd, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.didSelfAdd = java.lang.Boolean.parseBoolean(r12);
     */
    /* JADX WARNING: Missing block: B:201:0x0510, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.selfAdded = java.lang.Boolean.parseBoolean(r12);
     */
    /* JADX WARNING: Missing block: B:202:0x0523, code skipped:
            r9 = r32;
            r25 = r5;
            r26 = r13;
            r27 = r14;
            r28 = r15;
            r0.defaultGwMacAddress = r12;
     */
    /* JADX WARNING: Missing block: B:203:0x0532, code skipped:
            r9 = r32;
            r25 = r5;
            r5 = r12;
            r26 = r13;
            r27 = r14;
            r28 = r15;
     */
    /* JADX WARNING: Missing block: B:204:0x0543, code skipped:
            if (r5.equals("null") == false) goto L_0x0547;
     */
    /* JADX WARNING: Missing block: B:205:0x0545, code skipped:
            r10 = null;
     */
    /* JADX WARNING: Missing block: B:206:0x0547, code skipped:
            r10 = r5;
     */
    /* JADX WARNING: Missing block: B:207:0x0548, code skipped:
            r0.FQDN = r10;
     */
    /* JADX WARNING: Missing block: B:208:0x054b, code skipped:
            r9 = r32;
            r25 = r5;
            r5 = r12;
            r26 = r13;
            r27 = r14;
            r28 = r15;
     */
    /* JADX WARNING: Missing block: B:209:0x055c, code skipped:
            if (r5.equals("null") == false) goto L_0x0560;
     */
    /* JADX WARNING: Missing block: B:210:0x055e, code skipped:
            r10 = null;
     */
    /* JADX WARNING: Missing block: B:211:0x0560, code skipped:
            r10 = r5;
     */
    /* JADX WARNING: Missing block: B:212:0x0561, code skipped:
            r0.BSSID = r10;
     */
    /* JADX WARNING: Missing block: B:213:0x0564, code skipped:
            r9 = r32;
            r25 = r5;
            r5 = r12;
            r26 = r13;
            r27 = r14;
            r28 = r15;
     */
    /* JADX WARNING: Missing block: B:214:0x0573, code skipped:
            if (r0.isPasspoint() == false) goto L_0x0576;
     */
    /* JADX WARNING: Missing block: B:216:0x0576, code skipped:
            r10 = r5;
     */
    /* JADX WARNING: Missing block: B:217:0x0579, code skipped:
            if (r0.SSID == null) goto L_0x058b;
     */
    /* JADX WARNING: Missing block: B:219:0x0581, code skipped:
            if (r0.SSID.equals(r10) != false) goto L_0x058b;
     */
    /* JADX WARNING: Missing block: B:220:0x0583, code skipped:
            loge("Error parsing network history file, mismatched SSIDs");
            r0 = null;
            r10 = null;
     */
    /* JADX WARNING: Missing block: B:221:0x058b, code skipped:
            r0.SSID = r10;
     */
    /* JADX WARNING: Missing block: B:222:0x058e, code skipped:
            r5 = r10;
     */
    /* JADX WARNING: Missing block: B:223:0x0591, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:224:0x0593, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:241:?, code skipped:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:242:0x05b3, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:243:0x05b5, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:244:0x05b7, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:246:0x05bb, code skipped:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:249:?, code skipped:
            r5.addSuppressed(r0);
     */
    /* JADX WARNING: Missing block: B:250:0x05c1, code skipped:
            r3.close();
     */
    /* JADX WARNING: Missing block: B:272:?, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void readNetworkHistory(Map<String, WifiConfiguration> configs, Map<Integer, ScanDetailCache> scanDetailCaches, Set<String> deletedEphemeralSSIDs) {
        Throwable th;
        Throwable th2;
        Throwable th3;
        String str;
        StringBuilder stringBuilder;
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
                        if (key.equals(CONFIG_KEY)) {
                            config = (WifiConfiguration) configs.get(value);
                            StringBuilder stringBuilder2;
                            if (config == null) {
                                String str2 = TAG;
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
                            Map<String, WifiConfiguration> map2 = configs;
                            status2 = status;
                            if (config != null) {
                                status = config.getNetworkSelectionStatus();
                                Object obj;
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
                                }
                            }
                            set = deletedEphemeralSSIDs;
                            String str4 = ssid;
                            status = status2;
                            ssid = str4;
                        }
                        status = status2;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                set = deletedEphemeralSSIDs;
                th2 = th;
                th3 = null;
                if (th3 == null) {
                }
                throw th2;
            }
        } catch (EOFException e) {
            set = deletedEphemeralSSIDs;
        } catch (FileNotFoundException e2) {
            FileNotFoundException e3 = e2;
            set = deletedEphemeralSSIDs;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("readNetworkHistory: no config file, ");
            stringBuilder.append(e3);
            Log.i(str, stringBuilder.toString());
        } catch (NumberFormatException e4) {
            NumberFormatException e5 = e4;
            set = deletedEphemeralSSIDs;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("readNetworkHistory: failed to parse, ");
            stringBuilder.append(e5);
            Log.e(str, stringBuilder.toString(), e5);
        } catch (IOException e6) {
            IOException e7 = e6;
            set = deletedEphemeralSSIDs;
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("readNetworkHistory: failed to read, ");
            stringBuilder.append(e7);
            Log.e(str, stringBuilder.toString(), e7);
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
