package com.android.server.wifi.util;

import android.net.wifi.WifiConfiguration;
import android.telephony.HwTelephonyManager;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.anqp.Constants;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.HashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class TelephonyUtil {
    public static final String DEFAULT_EAP_PREFIX = "\u0000";
    private static final HashMap<Integer, String> EAP_METHOD_PREFIX = new HashMap();
    private static final String IMSI_CIPHER_TRANSFORMATION = "RSA/ECB/OAEPwithSHA-256andMGF1Padding";
    public static final String TAG = "TelephonyUtil";
    private static final String THREE_GPP_NAI_REALM_FORMAT = "wlan.mnc%s.mcc%s.3gppnetwork.org";
    private static int mAssignedSubId = HwTelephonyManager.getDefault().getDefault4GSlotId();

    public static class SimAuthRequestData {
        public String[] data;
        public int networkId;
        public int protocol;
        public String ssid;

        public SimAuthRequestData(int networkId, int protocol, String ssid, String[] data) {
            this.networkId = networkId;
            this.protocol = protocol;
            this.ssid = ssid;
            this.data = data;
        }
    }

    public static class SimAuthResponseData {
        public String response;
        public String type;

        public SimAuthResponseData(String type, String response) {
            this.type = type;
            this.response = response;
        }
    }

    static {
        EAP_METHOD_PREFIX.put(Integer.valueOf(5), "0");
        EAP_METHOD_PREFIX.put(Integer.valueOf(4), "1");
        EAP_METHOD_PREFIX.put(Integer.valueOf(6), "6");
    }

    public static Pair<String, String> getSimIdentity(TelephonyManager tm, TelephonyUtil telephonyUtil, WifiConfiguration config) {
        if (tm == null) {
            Log.e(TAG, "No valid TelephonyManager");
            return null;
        }
        HwTelephonyManager hwTelephonyManager = HwTelephonyManager.getDefault();
        int subId = getEapSubId(tm, hwTelephonyManager, config);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("get identity by sim-card from subId:");
        stringBuilder.append(subId);
        Log.d(str, stringBuilder.toString());
        str = tm.getSubscriberId(subId);
        String cdmaGsmImsi = hwTelephonyManager.getCdmaGsmImsi();
        if (cdmaGsmImsi != null && hwTelephonyManager.isCDMASimCard(subId)) {
            String[] cdmaGsmImsiArray = cdmaGsmImsi.split(",");
            if (2 == cdmaGsmImsiArray.length) {
                str = cdmaGsmImsiArray[1];
                Log.d(TAG, "cdma prefer USIM/GSM imsi");
            }
        }
        String mccMnc = "";
        if (5 == tm.getSimState(subId)) {
            mccMnc = tm.getSimOperator(subId);
        }
        String identity;
        try {
            ImsiEncryptionInfo imsiEncryptionInfo = tm.getCarrierInfoForImsiEncryption(2);
            identity = buildIdentity(getSimMethodForConfig(config), str, mccMnc, false);
            if (identity == null) {
                Log.e(TAG, "Failed to build the identity");
                return null;
            }
            String encryptedIdentity = buildEncryptedIdentity(telephonyUtil, getSimMethodForConfig(config), str, mccMnc, imsiEncryptionInfo);
            if (encryptedIdentity == null) {
                encryptedIdentity = "";
            }
            return Pair.create(identity, encryptedIdentity);
        } catch (RuntimeException e) {
            identity = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to get imsi encryption info: ");
            stringBuilder2.append(e.getMessage());
            Log.e(identity, stringBuilder2.toString());
            return null;
        }
    }

    @VisibleForTesting
    public String encryptDataUsingPublicKey(PublicKey key, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(IMSI_CIPHER_TRANSFORMATION);
            cipher.init(1, key);
            byte[] encryptedBytes = cipher.doFinal(data);
            return Base64.encodeToString(encryptedBytes, 0, encryptedBytes.length, 0);
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Encryption failed: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    private static String buildEncryptedIdentity(TelephonyUtil telephonyUtil, int eapMethod, String imsi, String mccMnc, ImsiEncryptionInfo imsiEncryptionInfo) {
        if (imsiEncryptionInfo == null) {
            return null;
        }
        String prefix = (String) EAP_METHOD_PREFIX.get(Integer.valueOf(eapMethod));
        if (prefix == null) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append(imsi);
        String encryptedImsi = telephonyUtil.encryptDataUsingPublicKey(imsiEncryptionInfo.getPublicKey(), stringBuilder.toString().getBytes());
        if (encryptedImsi == null) {
            Log.e(TAG, "Failed to encrypt IMSI");
            return null;
        }
        String encryptedIdentity = buildIdentity(eapMethod, encryptedImsi, mccMnc, true);
        if (imsiEncryptionInfo.getKeyIdentifier() != null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(encryptedIdentity);
            stringBuilder2.append(",");
            stringBuilder2.append(imsiEncryptionInfo.getKeyIdentifier());
            encryptedIdentity = stringBuilder2.toString();
        }
        return encryptedIdentity;
    }

    private static String buildIdentity(int eapMethod, String imsi, String mccMnc, boolean isEncrypted) {
        if (imsi == null || imsi.isEmpty()) {
            Log.e(TAG, "No IMSI or IMSI is null");
            return null;
        }
        String prefix = isEncrypted ? DEFAULT_EAP_PREFIX : (String) EAP_METHOD_PREFIX.get(Integer.valueOf(eapMethod));
        if (prefix == null) {
            return null;
        }
        String mcc;
        String mnc;
        if (mccMnc == null || mccMnc.isEmpty()) {
            mcc = imsi.substring(0, 3);
            mnc = imsi.substring(3, 6);
        } else {
            mcc = mccMnc.substring(0, 3);
            mnc = mccMnc.substring(3);
            if (mnc.length() == 2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("0");
                stringBuilder.append(mnc);
                mnc = stringBuilder.toString();
            }
        }
        String naiRealm = String.format(THREE_GPP_NAI_REALM_FORMAT, new Object[]{mnc, mcc});
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(prefix);
        stringBuilder2.append(imsi);
        stringBuilder2.append("@");
        stringBuilder2.append(naiRealm);
        return stringBuilder2.toString();
    }

    private static int getSimMethodForConfig(WifiConfiguration config) {
        int i = -1;
        if (config == null || config.enterpriseConfig == null) {
            return -1;
        }
        int eapMethod = config.enterpriseConfig.getEapMethod();
        if (eapMethod == 0) {
            switch (config.enterpriseConfig.getPhase2Method()) {
                case 5:
                    eapMethod = 4;
                    break;
                case 6:
                    eapMethod = 5;
                    break;
                case 7:
                    eapMethod = 6;
                    break;
            }
        }
        if (isSimEapMethod(eapMethod)) {
            i = eapMethod;
        }
        return i;
    }

    public static boolean isSimConfig(WifiConfiguration config) {
        return getSimMethodForConfig(config) != -1;
    }

    public static boolean isSimEapMethod(int eapMethod) {
        return eapMethod == 4 || eapMethod == 5 || eapMethod == 6;
    }

    private static int parseHex(char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - 48;
        }
        if ('a' <= ch && ch <= 'f') {
            return (ch - 97) + 10;
        }
        if ('A' <= ch && ch <= 'F') {
            return (ch - 65) + 10;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append(ch);
        stringBuilder.append(" is not a valid hex digit");
        throw new NumberFormatException(stringBuilder.toString());
    }

    private static byte[] parseHex(String hex) {
        if (hex == null) {
            return new byte[0];
        }
        if (hex.length() % 2 == 0) {
            int j = 1;
            byte[] result = new byte[((hex.length() / 2) + 1)];
            result[0] = (byte) (hex.length() / 2);
            int i = 0;
            while (i < hex.length()) {
                result[j] = (byte) (((parseHex(hex.charAt(i)) * 16) + parseHex(hex.charAt(i + 1))) & Constants.BYTE_MASK);
                i += 2;
                j++;
            }
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(hex);
        stringBuilder.append(" is not a valid hex string");
        throw new NumberFormatException(stringBuilder.toString());
    }

    private static String makeHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int length = bytes.length;
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x", new Object[]{Byte.valueOf(bytes[i])}));
        }
        return sb.toString();
    }

    private static String makeHex(byte[] bytes, int from, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02x", new Object[]{Byte.valueOf(bytes[from + i])}));
        }
        return sb.toString();
    }

    private static byte[] concatHex(byte[] array1, byte[] array2) {
        int length;
        byte[] result = new byte[(array1.length + array2.length)];
        int index = 0;
        int i = 0;
        if (array1.length != 0) {
            int index2 = 0;
            for (byte b : array1) {
                result[index2] = b;
                index2++;
            }
            index = index2;
        }
        if (array2.length != 0) {
            length = array2.length;
            while (i < length) {
                result[index] = array2[i];
                index++;
                i++;
            }
        }
        return result;
    }

    public static String getGsmSimAuthResponse(String[] requestData, TelephonyManager tm) {
        String[] strArr = requestData;
        TelephonyManager telephonyManager = tm;
        WifiConfiguration wifiConfiguration = null;
        if (telephonyManager == null) {
            Log.e(TAG, "No valid TelephonyManager");
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int length = strArr.length;
        int i = 0;
        int i2 = 0;
        while (i2 < length) {
            WifiConfiguration wifiConfiguration2;
            String challenge = strArr[i2];
            if (challenge == null || challenge.isEmpty()) {
                wifiConfiguration2 = wifiConfiguration;
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RAND = ");
                stringBuilder.append(challenge);
                Log.d(str, stringBuilder.toString());
                byte[] rand = wifiConfiguration;
                try {
                    StringBuilder stringBuilder2;
                    byte[] rand2 = parseHex(challenge);
                    String base64Challenge = Base64.encodeToString(rand2, 2);
                    int subId = getEapSubId(telephonyManager, HwTelephonyManager.getDefault(), wifiConfiguration);
                    String str2 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("get SIM_AUTH response by EAP-SIM method, sim-card from subId:");
                    stringBuilder3.append(subId);
                    Log.d(str2, stringBuilder3.toString());
                    rand = telephonyManager.getIccAuthentication(subId, 2, 128, base64Challenge);
                    if (rand == null) {
                        rand = telephonyManager.getIccAuthentication(subId, 1, 128, base64Challenge);
                    }
                    str2 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Raw Response - ");
                    stringBuilder4.append(rand);
                    Log.v(str2, stringBuilder4.toString());
                    byte[] bArr;
                    if (rand == null) {
                    } else if (rand.length() <= 4) {
                        bArr = rand2;
                    } else {
                        byte[] result = Base64.decode(rand, i);
                        String str3 = TAG;
                        StringBuilder stringBuilder5 = new StringBuilder();
                        stringBuilder5.append("Hex Response -");
                        stringBuilder5.append(makeHex(result));
                        Log.v(str3, stringBuilder5.toString());
                        int sresLen = result[i];
                        if (sresLen >= result.length) {
                            bArr = rand2;
                        } else if (sresLen < 0) {
                            bArr = rand2;
                        } else {
                            str3 = makeHex(result, 1, sresLen);
                            int kcOffset = 1 + sresLen;
                            if (kcOffset >= result.length) {
                                String str4 = TAG;
                                StringBuilder stringBuilder6 = new StringBuilder();
                                stringBuilder6.append("malfomed response - ");
                                stringBuilder6.append(rand);
                                Log.e(str4, stringBuilder6.toString());
                                return null;
                            }
                            i = result[kcOffset];
                            if (kcOffset + i > result.length) {
                                rand2 = TAG;
                                StringBuilder stringBuilder7 = new StringBuilder();
                                stringBuilder7.append("malfomed response - ");
                                stringBuilder7.append(rand);
                                Log.e(rand2, stringBuilder7.toString());
                                return null;
                            }
                            rand2 = makeHex(result, 1 + kcOffset, i);
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(":");
                            stringBuilder3.append(rand2);
                            stringBuilder3.append(":");
                            stringBuilder3.append(str3);
                            sb.append(stringBuilder3.toString());
                            String str5 = TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("kc:");
                            stringBuilder3.append(rand2);
                            stringBuilder3.append(" sres:");
                            stringBuilder3.append(str3);
                            Log.v(str5, stringBuilder3.toString());
                            wifiConfiguration2 = null;
                        }
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("malfomed response - ");
                        stringBuilder2.append(rand);
                        Log.e(str, stringBuilder2.toString());
                        return null;
                    }
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("bad response - ");
                    stringBuilder2.append(rand);
                    Log.e(str, stringBuilder2.toString());
                    return null;
                } catch (NumberFormatException e) {
                    wifiConfiguration2 = wifiConfiguration;
                    NumberFormatException numberFormatException = e;
                    Log.e(TAG, "malformed challenge");
                }
            }
            i2++;
            wifiConfiguration = wifiConfiguration2;
            strArr = requestData;
            telephonyManager = tm;
            i = 0;
        }
        return sb.toString();
    }

    public static SimAuthResponseData get3GAuthResponse(SimAuthRequestData requestData, TelephonyManager tm) {
        String str;
        String str2;
        String auts;
        SimAuthRequestData simAuthRequestData = requestData;
        TelephonyManager telephonyManager = tm;
        StringBuilder sb = new StringBuilder();
        byte[] rand = null;
        byte[] authn = null;
        String resType = WifiNative.SIM_AUTH_RESP_TYPE_UMTS_AUTH;
        if (simAuthRequestData.data.length == 2) {
            try {
                rand = parseHex(simAuthRequestData.data[0]);
                authn = parseHex(simAuthRequestData.data[1]);
            } catch (NumberFormatException e) {
                Log.e(TAG, "malformed challenge");
            }
        } else {
            Log.e(TAG, "malformed challenge");
        }
        String tmResponse = "";
        if (!(rand == null || authn == null)) {
            String base64Challenge = Base64.encodeToString(concatHex(rand, authn), 2);
            if (telephonyManager != null) {
                int subId = getEapSubId(telephonyManager, HwTelephonyManager.getDefault(), null);
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("get SIM_AUTH response by EAP-AKA/AKA' method, sim-card from subId:");
                stringBuilder.append(subId);
                Log.d(str, stringBuilder.toString());
                tmResponse = telephonyManager.getIccAuthentication(subId, 2, 129, base64Challenge);
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Raw Response - ");
                stringBuilder.append(tmResponse);
                Log.v(str, stringBuilder.toString());
            } else {
                Log.e(TAG, "No valid TelephonyManager");
            }
        }
        boolean goodReponse = false;
        StringBuilder stringBuilder2;
        if (tmResponse == null || tmResponse.length() <= 4) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("bad response - ");
            stringBuilder2.append(tmResponse);
            Log.e(str2, stringBuilder2.toString());
        } else {
            byte[] result = Base64.decode(tmResponse, 0);
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Hex Response - ");
            stringBuilder3.append(makeHex(result));
            Log.e(str3, stringBuilder3.toString());
            byte tag = result[0];
            if (tag == (byte) -37) {
                Log.v(TAG, "successful 3G authentication ");
                int resLen = result[1];
                String res = makeHex(result, 2, resLen);
                int ckLen = result[resLen + 2];
                str3 = makeHex(result, resLen + 3, ckLen);
                str = makeHex(result, (resLen + ckLen) + 4, result[(resLen + ckLen) + 3]);
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(":");
                stringBuilder4.append(str);
                stringBuilder4.append(":");
                stringBuilder4.append(str3);
                stringBuilder4.append(":");
                stringBuilder4.append(res);
                sb.append(stringBuilder4.toString());
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("ik:");
                stringBuilder2.append(str);
                stringBuilder2.append("ck:");
                stringBuilder2.append(str3);
                stringBuilder2.append(" res:");
                stringBuilder2.append(res);
                Log.v(str2, stringBuilder2.toString());
                goodReponse = true;
            } else {
                if (tag == (byte) -36) {
                    Log.e(TAG, "synchronisation failure");
                    auts = makeHex(result, 2, result[1]);
                    rand = WifiNative.SIM_AUTH_RESP_TYPE_UMTS_AUTS;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append(":");
                    stringBuilder5.append(auts);
                    sb.append(stringBuilder5.toString());
                    resType = TAG;
                    StringBuilder stringBuilder6 = new StringBuilder();
                    stringBuilder6.append("auts:");
                    stringBuilder6.append(auts);
                    Log.v(resType, stringBuilder6.toString());
                    goodReponse = true;
                    resType = rand;
                } else {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("bad response - unknown tag = ");
                    stringBuilder2.append(tag);
                    Log.e(str2, stringBuilder2.toString());
                }
            }
        }
        if (!goodReponse) {
            return null;
        }
        str2 = sb.toString();
        auts = TAG;
        StringBuilder stringBuilder7 = new StringBuilder();
        stringBuilder7.append("Supplicant Response -");
        stringBuilder7.append(str2);
        Log.v(auts, stringBuilder7.toString());
        return new SimAuthResponseData(resType, str2);
    }

    private static int getEapSubId(TelephonyManager tm, HwTelephonyManager hwTelephonyManager, WifiConfiguration config) {
        if (config == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getEapSubId(): config is null, get subId=");
            stringBuilder.append(mAssignedSubId);
            Log.d(str, stringBuilder.toString());
            return mAssignedSubId;
        }
        int subId = config.enterpriseConfig.getEapSubId();
        boolean isMultiSimEnabled = tm.isMultiSimEnabled();
        int sub1State = tm.getSimState(0);
        int sub2State = tm.getSimState(1);
        if (!(isMultiSimEnabled && sub1State == 5 && sub2State == 5 && subId != Values.MAX_EXPID)) {
            subId = hwTelephonyManager.getDefault4GSlotId();
        }
        mAssignedSubId = subId;
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("checkUseDefaultSubId: isMultiSimEnabled=");
        stringBuilder2.append(isMultiSimEnabled);
        stringBuilder2.append(", sub1State=");
        stringBuilder2.append(sub1State);
        stringBuilder2.append(", sub2State=");
        stringBuilder2.append(sub2State);
        stringBuilder2.append(", subId=");
        stringBuilder2.append(subId);
        stringBuilder2.append(", mAssignedSubId=");
        stringBuilder2.append(mAssignedSubId);
        Log.d(str2, stringBuilder2.toString());
        return subId;
    }
}
