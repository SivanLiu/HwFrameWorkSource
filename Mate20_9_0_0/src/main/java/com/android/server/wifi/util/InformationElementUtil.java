package com.android.server.wifi.util;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.ReasonCode;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.StatusCode;
import android.net.wifi.ScanResult.InformationElement;
import android.util.Log;
import com.android.server.wifi.ByteBufferReader;
import com.android.server.wifi.WifiConfigManager;
import com.android.server.wifi.hotspot2.NetworkDetail.Ant;
import com.android.server.wifi.hotspot2.NetworkDetail.HSRelease;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.eap.AuthParam;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;

public class InformationElementUtil {
    private static final boolean HWFLOW;
    private static final String TAG = "InformationElementUtil";

    public static class APCapInfo {
        private int mStream1 = 0;
        private int mStream2 = 0;
        private int mStream3 = 0;
        private int mStream4 = 0;
        private int mTxMcsSet = 0;

        public int getTxMcsSet() {
            return this.mTxMcsSet;
        }

        public int getStream1() {
            return this.mStream1;
        }

        public int getStream2() {
            return this.mStream2;
        }

        public int getStream3() {
            return this.mStream3;
        }

        public int getStream4() {
            return this.mStream4;
        }

        public void from(InformationElement ie) {
            try {
                StringBuilder stringBuilder;
                if (ie.id != 45) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Element id is not APCapInfo, : ");
                    stringBuilder.append(ie.id);
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (ie.bytes.length >= 16) {
                    ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
                    data.position(data.position() + 3);
                    this.mStream1 = data.get();
                    this.mStream2 = data.get();
                    this.mStream3 = data.get();
                    this.mStream4 = data.get();
                    int i = 1;
                    this.mStream1 = this.mStream1 == 0 ? 0 : 1;
                    this.mStream2 = this.mStream2 == 0 ? 0 : 1;
                    this.mStream3 = this.mStream3 == 0 ? 0 : 1;
                    if (this.mStream4 == 0) {
                        i = 0;
                    }
                    this.mStream4 = i;
                    data.position(data.position() + 8);
                    this.mTxMcsSet = (data.get() >> 4) & 15;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("APCapInfo length smaller than 16: ");
                    stringBuilder.append(ie.bytes.length);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } catch (IllegalArgumentException e) {
                if (InformationElementUtil.HWFLOW) {
                    String str = InformationElementUtil.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("APCapInfo");
                    stringBuilder2.append(e);
                    Log.d(str, stringBuilder2.toString());
                }
            }
        }
    }

    public static class BssLoad {
        public int capacity = 0;
        public int channelUtilization = 0;
        public int stationCount = 0;

        public void from(InformationElement ie) {
            StringBuilder stringBuilder;
            if (ie.id != 11) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Element id is not BSS_LOAD, : ");
                stringBuilder.append(ie.id);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (ie.bytes.length == 5) {
                ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
                this.stationCount = data.getShort() & Constants.SHORT_MASK;
                this.channelUtilization = data.get() & Constants.BYTE_MASK;
                this.capacity = data.getShort() & Constants.SHORT_MASK;
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("BSS Load element length is not 5: ");
                stringBuilder.append(ie.bytes.length);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    public static class Capabilities {
        private static final int CAP_ESS_BIT_OFFSET = 0;
        private static final int CAP_PRIVACY_BIT_OFFSET = 4;
        private static final int CAP_WAPI_BIT_OFFSET = 7;
        private static final byte MASK_11V = (byte) 8;
        private static final int OFFSET_11V = 2;
        private static final int PMF_C = 1;
        private static final int PMF_INVALID = 3;
        private static final int PMF_KEY_MGMT_EAP = 1;
        private static final int PMF_KEY_MGMT_EAP_SHA256 = 5;
        private static final int PMF_KEY_MGMT_FT_EAP = 3;
        private static final int PMF_KEY_MGMT_FT_PSK = 4;
        private static final int PMF_KEY_MGMT_PSK = 2;
        private static final int PMF_KEY_MGMT_PSK_SHA256 = 6;
        private static final int PMF_NONE = 0;
        private static final int PMF_R = 2;
        private static final short RSNE_VERSION = (short) 1;
        private static final int RSN_CIPHER_CCMP = 78384896;
        private static final int RSN_CIPHER_NONE = 11276032;
        private static final int RSN_CIPHER_NO_GROUP_ADDRESSED = 128716544;
        private static final int RSN_CIPHER_TKIP = 44830464;
        private static final int RSN_MFPC_BIT_OFFSET = 7;
        private static final int RSN_MFPR_BIT_OFFSET = 6;
        private static final byte WAPI_AKM_CERT = (byte) 1;
        private static final byte WAPI_AKM_PSK = (byte) 2;
        private static final int WPA2_AKM_EAP = 28053248;
        private static final int WPA2_AKM_EAP_SHA256 = 95162112;
        private static final int WPA2_AKM_FT_EAP = 61607680;
        private static final int WPA2_AKM_FT_PSK = 78384896;
        private static final int WPA2_AKM_PSK = 44830464;
        private static final int WPA2_AKM_PSK_SHA256 = 111939328;
        private static final int WPA_AKM_EAP = 32657408;
        private static final int WPA_AKM_PSK = 49434624;
        private static final int WPA_CIPHER_CCMP = 82989056;
        private static final int WPA_CIPHER_NONE = 15880192;
        private static final int WPA_CIPHER_TKIP = 49434624;
        private static final int WPA_VENDOR_OUI_TYPE_ONE = 32657408;
        private static final short WPA_VENDOR_OUI_VERSION = (short) 1;
        private static final int WPS_VENDOR_OUI_TYPE = 82989056;
        public ArrayList<Integer> groupCipher;
        private boolean is80211K = false;
        private boolean is80211R = false;
        private boolean is80211V = false;
        public boolean isESS;
        public boolean isPrivacy;
        public boolean isWPS;
        public ArrayList<ArrayList<Integer>> keyManagement;
        public ArrayList<ArrayList<Integer>> pairwiseCipher;
        public String pmfCapabilities = "";
        private int pmfType = 0;
        public ArrayList<Integer> protocol;

        private void parseRsnElement(InformationElement ie) {
            ByteBuffer buf = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            try {
                if (buf.getShort() == (short) 1) {
                    short i;
                    StringBuilder stringBuilder;
                    int i2;
                    StringBuilder stringBuilder2;
                    StringBuilder stringBuilder3;
                    this.protocol.add(Integer.valueOf(2));
                    this.groupCipher.add(Integer.valueOf(parseRsnCipher(buf.getInt())));
                    short cipherCount = buf.getShort();
                    ArrayList<Integer> rsnPairwiseCipher = new ArrayList();
                    for (i = (short) 0; i < cipherCount; i++) {
                        rsnPairwiseCipher.add(Integer.valueOf(parseRsnCipher(buf.getInt())));
                    }
                    this.pairwiseCipher.add(rsnPairwiseCipher);
                    i = buf.getShort();
                    ArrayList<Integer> rsnKeyManagement = new ArrayList();
                    ArrayList<Integer> pmfKeyManagement = new ArrayList();
                    for (short i3 = (short) 0; i3 < i; i3++) {
                        switch (buf.getInt()) {
                            case WPA2_AKM_EAP /*28053248*/:
                                rsnKeyManagement.add(Integer.valueOf(2));
                                pmfKeyManagement.add(Integer.valueOf(1));
                                break;
                            case 44830464:
                                rsnKeyManagement.add(Integer.valueOf(1));
                                pmfKeyManagement.add(Integer.valueOf(2));
                                break;
                            case WPA2_AKM_FT_EAP /*61607680*/:
                                rsnKeyManagement.add(Integer.valueOf(4));
                                pmfKeyManagement.add(Integer.valueOf(3));
                                break;
                            case 78384896:
                                rsnKeyManagement.add(Integer.valueOf(3));
                                pmfKeyManagement.add(Integer.valueOf(4));
                                break;
                            case WPA2_AKM_EAP_SHA256 /*95162112*/:
                                rsnKeyManagement.add(Integer.valueOf(6));
                                pmfKeyManagement.add(Integer.valueOf(5));
                                break;
                            case WPA2_AKM_PSK_SHA256 /*111939328*/:
                                rsnKeyManagement.add(Integer.valueOf(5));
                                pmfKeyManagement.add(Integer.valueOf(6));
                                break;
                            default:
                                break;
                        }
                    }
                    if (rsnKeyManagement.isEmpty()) {
                        rsnKeyManagement.add(Integer.valueOf(2));
                    }
                    this.keyManagement.add(rsnKeyManagement);
                    this.pmfCapabilities = "";
                    short pmf = buf.getShort();
                    if ((pmf & 128) != 0) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(this.pmfCapabilities);
                        stringBuilder.append("C:1 ");
                        this.pmfCapabilities = stringBuilder.toString();
                        this.pmfType = 1;
                        i2 = 0;
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(this.pmfCapabilities);
                        stringBuilder2.append("C:0 ");
                        this.pmfCapabilities = stringBuilder2.toString();
                        i2 = 0;
                        this.pmfType = 0;
                    }
                    if ((pmf & 64) != 0) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(this.pmfCapabilities);
                        stringBuilder.append("R:1 ");
                        this.pmfCapabilities = stringBuilder.toString();
                        if (this.pmfType == 0) {
                            this.pmfType = 3;
                        } else {
                            this.pmfType = 2;
                        }
                    } else {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(this.pmfCapabilities);
                        stringBuilder3.append("R:0 ");
                        this.pmfCapabilities = stringBuilder3.toString();
                    }
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(this.pmfCapabilities);
                    stringBuilder3.append("K:");
                    this.pmfCapabilities = stringBuilder3.toString();
                    if (pmfKeyManagement.isEmpty()) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(this.pmfCapabilities);
                        stringBuilder2.append("NONE");
                        this.pmfCapabilities = stringBuilder2.toString();
                    } else {
                        int rsnKeyCount = pmfKeyManagement.size();
                        while (i2 < rsnKeyCount) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(this.pmfCapabilities);
                            stringBuilder.append(i2 == 0 ? "" : "+");
                            stringBuilder.append(String.valueOf(pmfKeyManagement.get(i2)));
                            this.pmfCapabilities = stringBuilder.toString();
                            i2++;
                        }
                    }
                }
            } catch (BufferUnderflowException e) {
                Log.e("IE_Capabilities", "Couldn't parse RSNE, buffer underflow");
            }
        }

        private static int parseWpaCipher(int cipher) {
            if (cipher == WPA_CIPHER_NONE) {
                return 0;
            }
            if (cipher == 49434624) {
                return 2;
            }
            if (cipher == 82989056) {
                return 3;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown WPA cipher suite: ");
            stringBuilder.append(Integer.toHexString(cipher));
            Log.w("IE_Capabilities", stringBuilder.toString());
            return 0;
        }

        private static int parseRsnCipher(int cipher) {
            if (cipher == RSN_CIPHER_NONE) {
                return 0;
            }
            if (cipher == 44830464) {
                return 2;
            }
            if (cipher == 78384896) {
                return 3;
            }
            if (cipher == RSN_CIPHER_NO_GROUP_ADDRESSED) {
                return 1;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown RSN cipher suite: ");
            stringBuilder.append(Integer.toHexString(cipher));
            Log.w("IE_Capabilities", stringBuilder.toString());
            return 0;
        }

        private static boolean isWpsElement(InformationElement ie) {
            boolean z = false;
            try {
                if (ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN).getInt() == 82989056) {
                    z = true;
                }
                return z;
            } catch (BufferUnderflowException e) {
                Log.e("IE_Capabilities", "Couldn't parse VSA IE, buffer underflow");
                return false;
            }
        }

        private static boolean isWpaOneElement(InformationElement ie) {
            boolean z = false;
            try {
                if (ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN).getInt() == 32657408) {
                    z = true;
                }
                return z;
            } catch (BufferUnderflowException e) {
                Log.e("IE_Capabilities", "Couldn't parse VSA IE, buffer underflow");
                return false;
            }
        }

        private void parseWpaOneElement(InformationElement ie) {
            ByteBuffer buf = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            try {
                buf.getInt();
                if (buf.getShort() == (short) 1) {
                    short i;
                    this.protocol.add(Integer.valueOf(1));
                    this.groupCipher.add(Integer.valueOf(parseWpaCipher(buf.getInt())));
                    short cipherCount = buf.getShort();
                    ArrayList<Integer> wpaPairwiseCipher = new ArrayList();
                    short i2 = (short) 0;
                    for (i = (short) 0; i < cipherCount; i++) {
                        wpaPairwiseCipher.add(Integer.valueOf(parseWpaCipher(buf.getInt())));
                    }
                    this.pairwiseCipher.add(wpaPairwiseCipher);
                    i = buf.getShort();
                    ArrayList<Integer> wpaKeyManagement = new ArrayList();
                    while (i2 < i) {
                        int akm = buf.getInt();
                        if (akm == 32657408) {
                            wpaKeyManagement.add(Integer.valueOf(2));
                        } else if (akm == 49434624) {
                            wpaKeyManagement.add(Integer.valueOf(1));
                        }
                        i2++;
                    }
                    if (wpaKeyManagement.isEmpty()) {
                        wpaKeyManagement.add(Integer.valueOf(2));
                    }
                    this.keyManagement.add(wpaKeyManagement);
                }
            } catch (BufferUnderflowException e) {
                Log.e("IE_Capabilities", "Couldn't parse type 1 WPA, buffer underflow");
            }
        }

        private void parseWapiElement(InformationElement ie) {
            if (ie.bytes != null && ie.bytes.length > 7) {
                this.protocol.add(Integer.valueOf(4));
                ArrayList<Integer> wapiKeyManagement = new ArrayList();
                ArrayList<Integer> wapiPairwiseCipher = new ArrayList();
                byte akm = ie.bytes[7];
                switch (akm) {
                    case (byte) 1:
                        Log.d(InformationElementUtil.TAG, "parseWapiElement: This is a WAPI CERT network");
                        wapiKeyManagement.add(Integer.valueOf(8));
                        break;
                    case (byte) 2:
                        Log.d(InformationElementUtil.TAG, "parseWapiElement: This is a WAPI PSK network");
                        wapiKeyManagement.add(Integer.valueOf(1));
                        break;
                    default:
                        String str = InformationElementUtil.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("parseWapiElement: akm=");
                        stringBuilder.append(akm);
                        stringBuilder.append(" Unknown WAPI network type");
                        Log.e(str, stringBuilder.toString());
                        break;
                }
                this.keyManagement.add(wapiKeyManagement);
                this.pairwiseCipher.add(wapiPairwiseCipher);
            }
        }

        public void from(InformationElement[] ies, BitSet beaconCap) {
            this.protocol = new ArrayList();
            this.keyManagement = new ArrayList();
            this.groupCipher = new ArrayList();
            this.pairwiseCipher = new ArrayList();
            if (ies != null && beaconCap != null) {
                this.pmfCapabilities = "NO RSN IE";
                this.isESS = beaconCap.get(0);
                this.isPrivacy = beaconCap.get(4);
                for (InformationElement ie : ies) {
                    if (ie.id == 48) {
                        parseRsnElement(ie);
                    }
                    if (ie.id == AuthParam.PARAM_TYPE_VENDOR_SPECIFIC) {
                        if (isWpaOneElement(ie)) {
                            parseWpaOneElement(ie);
                        }
                        if (isWpsElement(ie)) {
                            this.isWPS = true;
                        }
                    }
                    if (ie.id == 70) {
                        this.is80211K = true;
                    }
                    if (ie.id == 127 && ie.bytes.length > 2) {
                        this.is80211V = (ie.bytes[2] & 8) == 8;
                    }
                    if (ie.id == 54) {
                        this.is80211R = true;
                    }
                    if (ie.id == 68) {
                        parseWapiElement(ie);
                    }
                }
            }
        }

        private String protocolToString(int protocol) {
            if (protocol == 4) {
                return "WAPI";
            }
            switch (protocol) {
                case 0:
                    return "None";
                case 1:
                    return "WPA";
                case 2:
                    return "WPA2";
                default:
                    return "?";
            }
        }

        private String keyManagementToString(int akm) {
            switch (akm) {
                case 0:
                    return "None";
                case 1:
                    return "PSK";
                case 2:
                    return "EAP";
                case 3:
                    return "FT/PSK";
                case 4:
                    return "FT/EAP";
                case 5:
                    return "PSK-SHA256";
                case 6:
                    return "EAP-SHA256";
                case 8:
                    return "CERT";
                default:
                    return "?";
            }
        }

        private String cipherToString(int cipher) {
            if (cipher == 0) {
                return "None";
            }
            switch (cipher) {
                case 2:
                    return "TKIP";
                case 3:
                    return "CCMP";
                default:
                    return "?";
            }
        }

        public String generateCapabilitiesString() {
            StringBuilder stringBuilder;
            String capabilities = "";
            boolean isWEP = this.protocol.isEmpty() && this.isPrivacy;
            if (isWEP) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(capabilities);
                stringBuilder2.append("[WEP]");
                capabilities = stringBuilder2.toString();
            }
            String capabilities2 = capabilities;
            for (int i = 0; i < this.protocol.size(); i++) {
                String capabilities3;
                int j;
                StringBuilder stringBuilder3;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(capabilities2);
                stringBuilder4.append("[");
                stringBuilder4.append(protocolToString(((Integer) this.protocol.get(i)).intValue()));
                capabilities2 = stringBuilder4.toString();
                if (i < this.keyManagement.size()) {
                    capabilities3 = capabilities2;
                    j = 0;
                    while (j < ((ArrayList) this.keyManagement.get(i)).size()) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(capabilities3);
                        stringBuilder3.append(j == 0 ? "-" : "+");
                        stringBuilder3.append(keyManagementToString(((Integer) ((ArrayList) this.keyManagement.get(i)).get(j)).intValue()));
                        capabilities3 = stringBuilder3.toString();
                        j++;
                    }
                    capabilities2 = capabilities3;
                }
                if (i < this.pairwiseCipher.size()) {
                    capabilities3 = capabilities2;
                    j = 0;
                    while (j < ((ArrayList) this.pairwiseCipher.get(i)).size()) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(capabilities3);
                        stringBuilder3.append(j == 0 ? "-" : "+");
                        stringBuilder3.append(cipherToString(((Integer) ((ArrayList) this.pairwiseCipher.get(i)).get(j)).intValue()));
                        capabilities3 = stringBuilder3.toString();
                        j++;
                    }
                    capabilities2 = capabilities3;
                }
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append(capabilities2);
                stringBuilder4.append("]");
                capabilities2 = stringBuilder4.toString();
            }
            if (this.isESS) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(capabilities2);
                stringBuilder.append("[ESS]");
                capabilities2 = stringBuilder.toString();
            }
            if (this.isWPS) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(capabilities2);
                stringBuilder.append("[WPS]");
                capabilities2 = stringBuilder.toString();
            }
            if (this.is80211K) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(capabilities2);
                stringBuilder.append("[K]");
                capabilities2 = stringBuilder.toString();
            }
            if (this.is80211V) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(capabilities2);
                stringBuilder.append("[V]");
                capabilities2 = stringBuilder.toString();
            }
            if (this.is80211R) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(capabilities2);
                stringBuilder.append("[R]");
                capabilities2 = stringBuilder.toString();
            }
            if (this.pmfType == 1) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(capabilities2);
                stringBuilder.append("[PMFC]");
                capabilities2 = stringBuilder.toString();
            }
            if (this.pmfType == 2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(capabilities2);
                stringBuilder.append("[PMFR]");
                capabilities2 = stringBuilder.toString();
            }
            if (this.pmfType != 3) {
                return capabilities2;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(capabilities2);
            stringBuilder.append("[PMFE]");
            return stringBuilder.toString();
        }
    }

    public static class Dot11vNetwork {
        private static final byte MASK_11V = (byte) 8;
        private static final int OFFSET_11V = 2;
        public boolean dot11vNetwork = false;

        public void from(InformationElement[] ies) {
            if (ies != null) {
                for (InformationElement ie : ies) {
                    if (127 == ie.id && ie.bytes.length > 2) {
                        this.dot11vNetwork = (ie.bytes[2] & 8) == 8;
                    }
                }
            }
        }
    }

    private static class EncryptionType {
        public ArrayList<Integer> keyManagement = new ArrayList();
        public ArrayList<Integer> pairwiseCipher = new ArrayList();
        public int protocol = 0;

        public EncryptionType(int protocol, ArrayList<Integer> keyManagement, ArrayList<Integer> pairwiseCipher) {
            this.protocol = protocol;
            this.keyManagement = keyManagement;
            this.pairwiseCipher = pairwiseCipher;
        }
    }

    public static class ExtendedCapabilities {
        private static final int RTT_RESP_ENABLE_BIT = 70;
        private static final int SSID_UTF8_BIT = 48;
        public BitSet capabilitiesBitSet;

        public boolean isStrictUtf8() {
            return this.capabilitiesBitSet.get(48);
        }

        public boolean is80211McRTTResponder() {
            return this.capabilitiesBitSet.get(RTT_RESP_ENABLE_BIT);
        }

        public ExtendedCapabilities() {
            this.capabilitiesBitSet = new BitSet();
        }

        public ExtendedCapabilities(ExtendedCapabilities other) {
            this.capabilitiesBitSet = other.capabilitiesBitSet;
        }

        public void from(InformationElement ie) {
            this.capabilitiesBitSet = BitSet.valueOf(ie.bytes);
        }
    }

    public static class HiLinkNetwork {
        private static final int[] FORMAT_HILINK = new int[]{0, 224, 252, 128, 0, 0, 0, 1, 0};
        private static final int[] FORMAT_HILINK_OUI = new int[]{0, 224, 252, 64, 0, 0, 0};
        private static final int HILINK_OUI_HEAD_LEN = 9;
        private static final int LOGO_ID = 249;
        private static final int MASK_HILINK = 255;
        private static final int NORMAL_WIFI = 0;
        public boolean isHiLinkNetwork = false;

        public int parseHiLogoTag(InformationElement[] ies) {
            if (ies == null) {
                return 0;
            }
            int i;
            int i2;
            for (InformationElement ie : ies) {
                if (AuthParam.PARAM_TYPE_VENDOR_SPECIFIC == ie.id && checkHiLinkOUISection(ie.bytes)) {
                    i2 = 9;
                    while (i2 < ie.bytes.length) {
                        i = ie.bytes[i2] & 255;
                        String str = InformationElementUtil.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("element:");
                        stringBuilder.append(i);
                        Log.d(str, stringBuilder.toString());
                        if (i == LOGO_ID) {
                            return ie.bytes[i2 + 2];
                        }
                        try {
                            i2 += ie.bytes[i2 + 1] + 2;
                        } catch (IndexOutOfBoundsException e) {
                            Log.w(InformationElementUtil.TAG, "the information elements is invalid");
                            return 0;
                        }
                    }
                    return 0;
                }
            }
            return 0;
        }

        private boolean checkHiLinkOUISection(byte[] bytes) {
            if (bytes == null || bytes.length < FORMAT_HILINK_OUI.length) {
                Log.w(InformationElementUtil.TAG, "the information elements's length is invalid");
                return false;
            }
            for (int index = 0; index < FORMAT_HILINK_OUI.length; index++) {
                if ((bytes[index] & 255) != FORMAT_HILINK_OUI[index]) {
                    return false;
                }
            }
            return true;
        }

        public void from(InformationElement[] ies) {
            if (ies != null) {
                for (InformationElement ie : ies) {
                    if (AuthParam.PARAM_TYPE_VENDOR_SPECIFIC == ie.id && checkHiLinkSection(ie.bytes)) {
                        this.isHiLinkNetwork = true;
                    }
                }
            }
        }

        private boolean checkHiLinkSection(byte[] bytes) {
            if (bytes == null || bytes.length < FORMAT_HILINK.length) {
                return false;
            }
            for (int index = 0; index < FORMAT_HILINK.length; index++) {
                if ((bytes[index] & 255) != FORMAT_HILINK[index]) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class HtOperation {
        public int secondChannelOffset = 0;

        public int getChannelWidth() {
            if (this.secondChannelOffset != 0) {
                return 1;
            }
            return 0;
        }

        public int getCenterFreq0(int primaryFrequency) {
            if (this.secondChannelOffset == 0) {
                return 0;
            }
            if (this.secondChannelOffset == 1) {
                return primaryFrequency + 10;
            }
            if (this.secondChannelOffset == 3) {
                return primaryFrequency - 10;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error on secondChannelOffset: ");
            stringBuilder.append(this.secondChannelOffset);
            Log.e("HtOperation", stringBuilder.toString());
            return 0;
        }

        public void from(InformationElement ie) {
            if (ie.id == 61) {
                this.secondChannelOffset = ie.bytes[1] & 3;
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Element id is not HT_OPERATION, : ");
            stringBuilder.append(ie.id);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static class Interworking {
        public Ant ant = null;
        public long hessid = 0;
        public boolean internet = false;

        public void from(InformationElement ie) {
            if (ie.id == StatusCode.AUTHORIZATION_DEENABLED) {
                ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
                int anOptions = data.get() & Constants.BYTE_MASK;
                this.ant = Ant.values()[anOptions & 15];
                this.internet = (anOptions & 16) != 0;
                if (ie.bytes.length == 1 || ie.bytes.length == 3 || ie.bytes.length == 7 || ie.bytes.length == 9) {
                    if (ie.bytes.length == 3 || ie.bytes.length == 9) {
                        ByteBufferReader.readInteger(data, ByteOrder.BIG_ENDIAN, 2);
                    }
                    if (ie.bytes.length == 7 || ie.bytes.length == 9) {
                        this.hessid = ByteBufferReader.readInteger(data, ByteOrder.BIG_ENDIAN, 6);
                        return;
                    }
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Bad Interworking element length: ");
                stringBuilder.append(ie.bytes.length);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Element id is not INTERWORKING, : ");
            stringBuilder2.append(ie.id);
            throw new IllegalArgumentException(stringBuilder2.toString());
        }
    }

    public static class RoamingConsortium {
        public int anqpOICount = 0;
        private long[] roamingConsortiums = null;

        public long[] getRoamingConsortiums() {
            return this.roamingConsortiums;
        }

        public void from(InformationElement ie) {
            if (ie.id == 111) {
                ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
                this.anqpOICount = data.get() & Constants.BYTE_MASK;
                int oi12Length = data.get() & Constants.BYTE_MASK;
                int oi1Length = oi12Length & 15;
                int oi2Length = (oi12Length >>> 4) & 15;
                int oi3Length = ((ie.bytes.length - 2) - oi1Length) - oi2Length;
                int oiCount = 0;
                if (oi1Length > 0) {
                    oiCount = 0 + 1;
                    if (oi2Length > 0) {
                        oiCount++;
                        if (oi3Length > 0) {
                            oiCount++;
                        }
                    }
                }
                this.roamingConsortiums = new long[oiCount];
                if (oi1Length > 0 && this.roamingConsortiums.length > 0) {
                    this.roamingConsortiums[0] = ByteBufferReader.readInteger(data, ByteOrder.BIG_ENDIAN, oi1Length);
                }
                if (oi2Length > 0 && this.roamingConsortiums.length > 1) {
                    this.roamingConsortiums[1] = ByteBufferReader.readInteger(data, ByteOrder.BIG_ENDIAN, oi2Length);
                }
                if (oi3Length > 0 && this.roamingConsortiums.length > 2) {
                    this.roamingConsortiums[2] = ByteBufferReader.readInteger(data, ByteOrder.BIG_ENDIAN, oi3Length);
                    return;
                }
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Element id is not ROAMING_CONSORTIUM, : ");
            stringBuilder.append(ie.id);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static class SupportedRates {
        public static final int MASK = 127;
        public ArrayList<Integer> mRates = new ArrayList();
        public boolean mValid = false;

        public boolean isValid() {
            return this.mValid;
        }

        public static int getRateFromByte(int byteVal) {
            switch (byteVal & 127) {
                case 2:
                    return 1000000;
                case 4:
                    return 2000000;
                case 11:
                    return 5500000;
                case 12:
                    return 6000000;
                case 18:
                    return 9000000;
                case 22:
                    return 11000000;
                case 24:
                    return 12000000;
                case ReasonCode.STA_LEAVING /*36*/:
                    return 18000000;
                case StatusCode.UNSUPPORTED_RSN_IE_VERSION /*44*/:
                    return 22000000;
                case 48:
                    return 24000000;
                case ReasonCode.MESH_CHANNEL_SWITCH_UNSPECIFIED /*66*/:
                    return 33000000;
                case StatusCode.INVALID_RSNIE /*72*/:
                    return 36000000;
                case StatusCode.REJECT_DSE_BAND /*96*/:
                    return 48000000;
                case 108:
                    return 54000000;
                default:
                    return -1;
            }
        }

        public void from(InformationElement ie) {
            int i = 0;
            this.mValid = false;
            if (ie != null && ie.bytes != null && ie.bytes.length <= 8 && ie.bytes.length >= 1) {
                ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
                while (i < ie.bytes.length) {
                    try {
                        int rate = getRateFromByte(data.get());
                        if (rate > 0) {
                            this.mRates.add(Integer.valueOf(rate));
                            i++;
                        } else {
                            return;
                        }
                    } catch (BufferUnderflowException e) {
                        return;
                    }
                }
                this.mValid = true;
            }
        }

        public String toString() {
            StringBuilder sbuf = new StringBuilder();
            Iterator it = this.mRates.iterator();
            while (it.hasNext()) {
                Integer rate = (Integer) it.next();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.format("%.1f", new Object[]{Double.valueOf(((double) rate.intValue()) / 1000000.0d)}));
                stringBuilder.append(", ");
                sbuf.append(stringBuilder.toString());
            }
            return sbuf.toString();
        }
    }

    public static class TrafficIndicationMap {
        private static final int MAX_TIM_LENGTH = 254;
        public int mBitmapControl = 0;
        public int mDtimCount = -1;
        public int mDtimPeriod = -1;
        public int mLength = 0;
        private boolean mValid = false;

        public boolean isValid() {
            return this.mValid;
        }

        public void from(InformationElement ie) {
            this.mValid = false;
            if (ie != null && ie.bytes != null) {
                this.mLength = ie.bytes.length;
                ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
                try {
                    this.mDtimCount = data.get() & Constants.BYTE_MASK;
                    this.mDtimPeriod = data.get() & Constants.BYTE_MASK;
                    this.mBitmapControl = data.get() & Constants.BYTE_MASK;
                    data.get();
                    if (this.mLength <= MAX_TIM_LENGTH && this.mDtimPeriod > 0) {
                        this.mValid = true;
                    }
                } catch (BufferUnderflowException e) {
                }
            }
        }
    }

    public static class VhtOperation {
        public int centerFreqIndex1 = 0;
        public int centerFreqIndex2 = 0;
        public int channelMode = 0;

        public boolean isValid() {
            return this.channelMode != 0;
        }

        public int getChannelWidth() {
            return this.channelMode + 1;
        }

        public int getCenterFreq0() {
            return ((this.centerFreqIndex1 - 36) * 5) + 5180;
        }

        public int getCenterFreq1() {
            if (this.channelMode > 1) {
                return ((this.centerFreqIndex2 - 36) * 5) + 5180;
            }
            return 0;
        }

        public void from(InformationElement ie) {
            if (ie.id == WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE) {
                this.channelMode = ie.bytes[0] & Constants.BYTE_MASK;
                this.centerFreqIndex1 = ie.bytes[1] & Constants.BYTE_MASK;
                this.centerFreqIndex2 = ie.bytes[2] & Constants.BYTE_MASK;
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Element id is not VHT_OPERATION, : ");
            stringBuilder.append(ie.id);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static class Vsa {
        private static final int ANQP_DOMID_BIT = 4;
        public int anqpDomainID = 0;
        public HSRelease hsRelease = null;

        public void from(InformationElement ie) {
            ByteBuffer data = ByteBuffer.wrap(ie.bytes).order(ByteOrder.LITTLE_ENDIAN);
            if (ie.bytes.length >= 5 && data.getInt() == Constants.HS20_FRAME_PREFIX) {
                int hsConf = data.get() & Constants.BYTE_MASK;
                switch ((hsConf >> 4) & 15) {
                    case 0:
                        this.hsRelease = HSRelease.R1;
                        break;
                    case 1:
                        this.hsRelease = HSRelease.R2;
                        break;
                    default:
                        this.hsRelease = HSRelease.Unknown;
                        break;
                }
                if ((hsConf & 4) == 0) {
                    return;
                }
                if (ie.bytes.length >= 7) {
                    this.anqpDomainID = data.getShort() & Constants.SHORT_MASK;
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("HS20 indication element too short: ");
                stringBuilder.append(ie.bytes.length);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    public static class WifiMode {
        public static final int MODE_11A = 1;
        public static final int MODE_11AC = 5;
        public static final int MODE_11B = 2;
        public static final int MODE_11G = 3;
        public static final int MODE_11N = 4;
        public static final int MODE_UNDEFINED = 0;

        public static int determineMode(int frequency, int maxRate, boolean foundVht, boolean foundHt, boolean foundErp) {
            if (foundVht) {
                return 5;
            }
            if (foundHt) {
                return 4;
            }
            if (foundErp) {
                return 3;
            }
            if (frequency >= 3000) {
                return 1;
            }
            if (maxRate < 24000000) {
                return 2;
            }
            return 3;
        }

        public static String toString(int mode) {
            switch (mode) {
                case 1:
                    return "MODE_11A";
                case 2:
                    return "MODE_11B";
                case 3:
                    return "MODE_11G";
                case 4:
                    return "MODE_11N";
                case 5:
                    return "MODE_11AC";
                default:
                    return "MODE_UNDEFINED";
            }
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    public static InformationElement[] parseInformationElements(byte[] bytes) {
        boolean found_ssid = false;
        if (bytes == null) {
            return new InformationElement[0];
        }
        ByteBuffer data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        ArrayList<InformationElement> infoElements = new ArrayList();
        while (data.remaining() > 1) {
            int eid = data.get() & Constants.BYTE_MASK;
            int elementLength = data.get() & Constants.BYTE_MASK;
            if (elementLength > data.remaining() || (eid == 0 && found_ssid)) {
                break;
            }
            if (eid == 0) {
                found_ssid = true;
            }
            InformationElement ie = new InformationElement();
            ie.id = eid;
            ie.bytes = new byte[elementLength];
            data.get(ie.bytes);
            infoElements.add(ie);
        }
        return (InformationElement[]) infoElements.toArray(new InformationElement[infoElements.size()]);
    }

    public static RoamingConsortium getRoamingConsortiumIE(InformationElement[] ies) {
        RoamingConsortium roamingConsortium = new RoamingConsortium();
        if (ies != null) {
            for (InformationElement ie : ies) {
                if (ie.id == 111) {
                    try {
                        roamingConsortium.from(ie);
                    } catch (RuntimeException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to parse Roaming Consortium IE: ");
                        stringBuilder.append(e.getMessage());
                        Log.e(str, stringBuilder.toString());
                    }
                }
            }
        }
        return roamingConsortium;
    }

    public static Vsa getHS2VendorSpecificIE(InformationElement[] ies) {
        Vsa vsa = new Vsa();
        if (ies != null) {
            for (InformationElement ie : ies) {
                if (ie.id == AuthParam.PARAM_TYPE_VENDOR_SPECIFIC) {
                    try {
                        vsa.from(ie);
                    } catch (RuntimeException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to parse Vendor Specific IE: ");
                        stringBuilder.append(e.getMessage());
                        Log.e(str, stringBuilder.toString());
                    }
                }
            }
        }
        return vsa;
    }

    public static Interworking getInterworkingIE(InformationElement[] ies) {
        Interworking interworking = new Interworking();
        if (ies != null) {
            for (InformationElement ie : ies) {
                if (ie.id == StatusCode.AUTHORIZATION_DEENABLED) {
                    try {
                        interworking.from(ie);
                    } catch (RuntimeException e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to parse Interworking IE: ");
                        stringBuilder.append(e.getMessage());
                        Log.e(str, stringBuilder.toString());
                    }
                }
            }
        }
        return interworking;
    }
}
