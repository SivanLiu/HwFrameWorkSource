package com.android.internal.telephony.dataconnection;

import android.hardware.radio.V1_0.ApnTypes;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.IccRecords;
import com.google.android.mms.pdu.CharacterSets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class ApnSetting {
    private static final boolean DBG = false;
    static final String LOG_TAG = "ApnSetting";
    static final String TAG = "ApnSetting";
    static final String V2_FORMAT_REGEX = "^\\[ApnSettingV2\\]\\s*";
    static final String V3_FORMAT_REGEX = "^\\[ApnSettingV3\\]\\s*";
    static final String V4_FORMAT_REGEX = "^\\[ApnSettingV4\\]\\s*";
    static final String V5_FORMAT_REGEX = "^\\[ApnSettingV5\\]\\s*";
    private static final boolean VDBG = false;
    public final String apn;
    public final int apnSetId;
    public final int authType;
    @Deprecated
    public final int bearer;
    @Deprecated
    public final int bearerBitmask;
    public final String carrier;
    public final boolean carrierEnabled;
    public final int id;
    public final int maxConns;
    public final int maxConnsTime;
    public final String mmsPort;
    public final String mmsProxy;
    public final String mmsc;
    public final boolean modemCognitive;
    public final int mtu;
    public final String mvnoMatchData;
    public final String mvnoType;
    public final int networkTypeBitmask;
    public final String numeric;
    public final String password;
    public boolean permanentFailed;
    public final String port;
    public final int profileId;
    public final String protocol;
    public final String proxy;
    public final String roamingProtocol;
    public String[] types;
    public final int typesBitmap;
    public final String user;
    public final int waitTime;

    @Deprecated
    public ApnSetting(int id, String numeric, String carrier, String apn, String proxy, String port, String mmsc, String mmsProxy, String mmsPort, String user, String password, int authType, String[] types, String protocol, String roamingProtocol, boolean carrierEnabled, int bearer, int bearerBitmask, int profileId, boolean modemCognitive, int maxConns, int waitTime, int maxConnsTime, int mtu, String mvnoType, String mvnoMatchData) {
        String[] strArr = types;
        this.permanentFailed = false;
        this.id = id;
        this.numeric = numeric;
        this.carrier = carrier;
        this.apn = apn;
        this.proxy = proxy;
        this.port = port;
        this.mmsc = mmsc;
        this.mmsProxy = mmsProxy;
        this.mmsPort = mmsPort;
        this.user = user;
        this.password = password;
        this.authType = authType;
        this.types = new String[strArr.length];
        int i = 0;
        int apnBitmap = 0;
        while (i < strArr.length) {
            this.types[i] = strArr[i].toLowerCase();
            apnBitmap |= getApnBitmask(this.types[i]);
            i++;
            int i2 = id;
            String str = numeric;
        }
        this.typesBitmap = apnBitmap;
        this.protocol = protocol;
        this.roamingProtocol = roamingProtocol;
        this.carrierEnabled = carrierEnabled;
        this.bearer = bearer;
        this.bearerBitmask = bearerBitmask | ServiceState.getBitmaskForTech(bearer);
        this.profileId = profileId;
        this.modemCognitive = modemCognitive;
        this.maxConns = maxConns;
        this.waitTime = waitTime;
        this.maxConnsTime = maxConnsTime;
        this.mtu = mtu;
        this.mvnoType = mvnoType;
        this.mvnoMatchData = mvnoMatchData;
        this.apnSetId = 0;
        this.networkTypeBitmask = ServiceState.convertBearerBitmaskToNetworkTypeBitmask(this.bearerBitmask);
    }

    public ApnSetting(int id, String numeric, String carrier, String apn, String proxy, String port, String mmsc, String mmsProxy, String mmsPort, String user, String password, int authType, String[] types, String protocol, String roamingProtocol, boolean carrierEnabled, int networkTypeBitmask, int profileId, boolean modemCognitive, int maxConns, int waitTime, int maxConnsTime, int mtu, String mvnoType, String mvnoMatchData) {
        this(id, numeric, carrier, apn, proxy, port, mmsc, mmsProxy, mmsPort, user, password, authType, types, protocol, roamingProtocol, carrierEnabled, networkTypeBitmask, profileId, modemCognitive, maxConns, waitTime, maxConnsTime, mtu, mvnoType, mvnoMatchData, 0);
    }

    public ApnSetting(int id, String numeric, String carrier, String apn, String proxy, String port, String mmsc, String mmsProxy, String mmsPort, String user, String password, int authType, String[] types, String protocol, String roamingProtocol, boolean carrierEnabled, int networkTypeBitmask, int profileId, boolean modemCognitive, int maxConns, int waitTime, int maxConnsTime, int mtu, String mvnoType, String mvnoMatchData, int apnSetId) {
        String[] strArr = types;
        this.permanentFailed = false;
        this.id = id;
        this.numeric = numeric;
        this.carrier = carrier;
        this.apn = apn;
        this.proxy = proxy;
        this.port = port;
        this.mmsc = mmsc;
        this.mmsProxy = mmsProxy;
        this.mmsPort = mmsPort;
        this.user = user;
        this.password = password;
        this.authType = authType;
        this.types = new String[strArr.length];
        int i = 0;
        int apnBitmap = 0;
        while (i < strArr.length) {
            this.types[i] = strArr[i].toLowerCase();
            apnBitmap |= getApnBitmask(this.types[i]);
            i++;
            int i2 = id;
            String str = numeric;
        }
        this.typesBitmap = apnBitmap;
        this.protocol = protocol;
        this.roamingProtocol = roamingProtocol;
        this.carrierEnabled = carrierEnabled;
        this.bearer = 0;
        this.bearerBitmask = ServiceState.convertNetworkTypeBitmaskToBearerBitmask(networkTypeBitmask);
        this.networkTypeBitmask = networkTypeBitmask;
        this.profileId = profileId;
        this.modemCognitive = modemCognitive;
        this.maxConns = maxConns;
        this.waitTime = waitTime;
        this.maxConnsTime = maxConnsTime;
        this.mtu = mtu;
        this.mvnoType = mvnoType;
        this.mvnoMatchData = mvnoMatchData;
        this.apnSetId = apnSetId;
    }

    public ApnSetting(ApnSetting apn) {
        ApnSetting apnSetting = apn;
        int i = apnSetting.id;
        String str = apnSetting.numeric;
        String str2 = apnSetting.carrier;
        String str3 = apnSetting.apn;
        String str4 = apnSetting.proxy;
        String str5 = apnSetting.port;
        String str6 = apnSetting.mmsc;
        String str7 = apnSetting.mmsProxy;
        String str8 = apnSetting.mmsPort;
        String str9 = apnSetting.user;
        String str10 = apnSetting.password;
        int i2 = apnSetting.authType;
        String[] strArr = apnSetting.types;
        String str11 = apnSetting.protocol;
        String str12 = apnSetting.roamingProtocol;
        String str13 = str11;
        String str14 = str12;
        str11 = str13;
        this(i, str, str2, str3, str4, str5, str6, str7, str8, str9, str10, i2, strArr, str11, str14, apnSetting.carrierEnabled, apnSetting.networkTypeBitmask, apnSetting.profileId, apnSetting.modemCognitive, apnSetting.maxConns, apnSetting.waitTime, apnSetting.maxConnsTime, apnSetting.mtu, apnSetting.mvnoType, apnSetting.mvnoMatchData, apnSetting.apnSetId);
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x005e  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x005d A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ApnSetting fromString(String data) {
        String str = data;
        if (str == null) {
            return null;
        }
        String data2;
        String[] a;
        int version;
        if (str.matches("^\\[ApnSettingV5\\]\\s*.*")) {
            version = 5;
            str = str.replaceFirst(V5_FORMAT_REGEX, "");
        } else if (str.matches("^\\[ApnSettingV4\\]\\s*.*")) {
            version = 4;
            str = str.replaceFirst(V4_FORMAT_REGEX, "");
        } else if (str.matches("^\\[ApnSettingV3\\]\\s*.*")) {
            version = 3;
            str = str.replaceFirst(V3_FORMAT_REGEX, "");
        } else if (str.matches("^\\[ApnSettingV2\\]\\s*.*")) {
            version = 2;
            str = str.replaceFirst(V2_FORMAT_REGEX, "");
        } else {
            data2 = str;
            version = 1;
            a = data2.split("\\s*,\\s*");
            if (a.length >= 14) {
                return null;
            }
            int authType;
            String protocol;
            String roamingProtocol;
            String[] typeArray;
            int profileId;
            boolean modemCognitive;
            int maxConns;
            int waitTime;
            int maxConnsTime;
            int mtu;
            String mvnoType;
            String mvnoMatchData;
            int apnSetId;
            boolean carrierEnabled;
            try {
                authType = Integer.parseInt(a[12]);
            } catch (NumberFormatException e) {
                authType = 0;
            }
            int bearerBitmask = 0;
            int networkTypeBitmask = 0;
            int profileId2 = 0;
            boolean modemCognitive2 = false;
            int maxConns2 = 0;
            int waitTime2 = 0;
            int maxConnsTime2 = 0;
            int mtu2 = 0;
            String mvnoType2 = "";
            String mvnoMatchData2 = "";
            String[] typeArray2;
            if (version == 1) {
                typeArray2 = new String[(a.length - 13)];
                System.arraycopy(a, 13, typeArray2, 0, a.length - 13);
                protocol = "IP";
                roamingProtocol = "IP";
                typeArray = typeArray2;
                profileId = 0;
                modemCognitive = false;
                maxConns = 0;
                waitTime = 0;
                maxConnsTime = 0;
                mtu = 0;
                mvnoType = mvnoType2;
                mvnoMatchData = mvnoMatchData2;
                apnSetId = 0;
                carrierEnabled = true;
            } else if (a.length < 18) {
                return null;
            } else {
                typeArray2 = a[13].split("\\s*\\|\\s*");
                protocol = a[14];
                roamingProtocol = a[15];
                carrierEnabled = Boolean.parseBoolean(a[16]);
                waitTime = 0;
                int bearerBitmask2 = ServiceState.getBitmaskFromString(a[17]);
                typeArray = typeArray2;
                if (a.length > 22) {
                    modemCognitive2 = Boolean.parseBoolean(a[19]);
                    try {
                        profileId2 = Integer.parseInt(a[18]);
                        maxConns2 = Integer.parseInt(a[20]);
                        waitTime2 = Integer.parseInt(a[21]);
                        maxConnsTime2 = Integer.parseInt(a[22]);
                    } catch (NumberFormatException e2) {
                    }
                }
                if (a.length > 23) {
                    try {
                        mtu2 = Integer.parseInt(a[23]);
                    } catch (NumberFormatException e3) {
                    }
                }
                if (a.length > 25) {
                    mvnoType2 = a[24];
                    mvnoMatchData2 = a[25];
                }
                if (a.length > 26) {
                    networkTypeBitmask = ServiceState.getBitmaskFromString(a[26]);
                }
                if (a.length > 27) {
                    apnSetId = Integer.parseInt(a[27]);
                    profileId = profileId2;
                    modemCognitive = modemCognitive2;
                    maxConns = maxConns2;
                    waitTime = waitTime2;
                    maxConnsTime = maxConnsTime2;
                    mtu = mtu2;
                    mvnoType = mvnoType2;
                    mvnoMatchData = mvnoMatchData2;
                } else {
                    profileId = profileId2;
                    modemCognitive = modemCognitive2;
                    maxConns = maxConns2;
                    waitTime = waitTime2;
                    maxConnsTime = maxConnsTime2;
                    mtu = mtu2;
                    mvnoType = mvnoType2;
                    mvnoMatchData = mvnoMatchData2;
                    apnSetId = 0;
                }
                bearerBitmask = bearerBitmask2;
            }
            if (networkTypeBitmask == 0) {
                networkTypeBitmask = ServiceState.convertBearerBitmaskToNetworkTypeBitmask(bearerBitmask);
            }
            int networkTypeBitmask2 = networkTypeBitmask;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(a[10]);
            stringBuilder.append(a[11]);
            return new ApnSetting(-1, stringBuilder.toString(), a[0], a[1], a[2], a[3], a[7], a[8], a[9], a[4], a[5], authType, typeArray, protocol, roamingProtocol, carrierEnabled, networkTypeBitmask2, profileId, modemCognitive, maxConns, waitTime, maxConnsTime, mtu, mvnoType, mvnoMatchData, apnSetId);
        }
        data2 = str;
        a = data2.split("\\s*,\\s*");
        if (a.length >= 14) {
        }
    }

    public static List<ApnSetting> arrayFromString(String data) {
        List<ApnSetting> retVal = new ArrayList();
        if (TextUtils.isEmpty(data)) {
            return retVal;
        }
        for (String apnString : data.split("\\s*;\\s*")) {
            ApnSetting apn = fromString(apnString);
            if (apn != null) {
                retVal.add(apn);
            }
        }
        return retVal;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ApnSettingV5] ");
        sb.append(this.carrier);
        sb.append(", ");
        sb.append(this.id);
        sb.append(", ");
        sb.append(this.numeric);
        sb.append(", ");
        sb.append(this.apn);
        sb.append(", ");
        sb.append(this.proxy);
        sb.append(", ");
        sb.append(this.mmsc);
        sb.append(", ");
        sb.append(this.mmsProxy);
        sb.append(", ");
        sb.append(this.mmsPort);
        sb.append(", ");
        sb.append(this.port);
        sb.append(", ");
        sb.append(this.authType);
        sb.append(", ");
        for (int i = 0; i < this.types.length; i++) {
            sb.append(this.types[i]);
            if (i < this.types.length - 1) {
                sb.append(" | ");
            }
        }
        sb.append(", ");
        sb.append(this.protocol);
        sb.append(", ");
        sb.append(this.roamingProtocol);
        sb.append(", ");
        sb.append(this.carrierEnabled);
        sb.append(", ");
        sb.append(this.bearer);
        sb.append(", ");
        sb.append(this.bearerBitmask);
        sb.append(", ");
        sb.append(this.profileId);
        sb.append(", ");
        sb.append(this.modemCognitive);
        sb.append(", ");
        sb.append(this.maxConns);
        sb.append(", ");
        sb.append(this.waitTime);
        sb.append(", ");
        sb.append(this.maxConnsTime);
        sb.append(", ");
        sb.append(this.mtu);
        sb.append(", ");
        sb.append(this.mvnoType);
        sb.append(", ");
        sb.append(this.mvnoMatchData);
        sb.append(", ");
        sb.append(this.permanentFailed);
        sb.append(", ");
        sb.append(this.networkTypeBitmask);
        sb.append(", ");
        sb.append(this.apnSetId);
        return sb.toString();
    }

    public boolean hasMvnoParams() {
        return (TextUtils.isEmpty(this.mvnoType) || TextUtils.isEmpty(this.mvnoMatchData)) ? false : true;
    }

    public boolean canHandleType(String type) {
        if (!this.carrierEnabled) {
            return false;
        }
        boolean wildcardable = true;
        if ("ia".equalsIgnoreCase(type)) {
            wildcardable = false;
        }
        for (String t : this.types) {
            if (t.equalsIgnoreCase(type) || ((wildcardable && t.equalsIgnoreCase(CharacterSets.MIMENAME_ANY_CHARSET)) || (t.equalsIgnoreCase("default") && type.equalsIgnoreCase("hipri")))) {
                return true;
            }
        }
        return false;
    }

    private static boolean iccidMatches(String mvnoData, String iccId) {
        for (String mvnoIccid : mvnoData.split(",")) {
            if (iccId.startsWith(mvnoIccid)) {
                Log.d("ApnSetting", "mvno icc id match found");
                return true;
            }
        }
        return false;
    }

    private static boolean imsiMatches(String imsiDB, String imsiSIM) {
        int len = imsiDB.length();
        if (len <= 0 || len > imsiSIM.length()) {
            return false;
        }
        int idx = 0;
        while (idx < len) {
            char c = imsiDB.charAt(idx);
            if (c != 'x' && c != 'X' && c != imsiSIM.charAt(idx)) {
                return false;
            }
            idx++;
        }
        return true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x006a A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x006a A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x006a A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean mvnoMatches(IccRecords r, String mvnoType, String mvnoMatchData) {
        if (mvnoType.equalsIgnoreCase("spn")) {
            return r.getServiceProviderName() != null && r.getServiceProviderName().equalsIgnoreCase(mvnoMatchData);
        } else {
            String imsiSIM;
            if (mvnoType.equalsIgnoreCase("imsi")) {
                imsiSIM = r.getIMSI();
                if (imsiSIM != null && imsiMatches(mvnoMatchData, imsiSIM)) {
                    return true;
                }
            } else if (mvnoType.equalsIgnoreCase("gid")) {
                imsiSIM = r.getGid1();
                int mvno_match_data_length = mvnoMatchData.length();
                if (imsiSIM != null && imsiSIM.length() >= mvno_match_data_length && imsiSIM.substring(0, mvno_match_data_length).equalsIgnoreCase(mvnoMatchData)) {
                    return true;
                }
            } else if (mvnoType.equalsIgnoreCase("iccid")) {
                imsiSIM = r.getIccId();
                if (imsiSIM != null && iccidMatches(mvnoMatchData, imsiSIM)) {
                    return true;
                }
            }
        }
    }

    public static boolean isMeteredApnType(String type, Phone phone) {
        if (phone == null) {
            return true;
        }
        String carrierConfig;
        boolean isRoaming = phone.getServiceState().getDataRoaming();
        boolean isIwlan = phone.getServiceState().getRilDataRadioTechnology() == 18;
        int subId = phone.getSubId();
        if (isIwlan) {
            carrierConfig = "carrier_metered_iwlan_apn_types_strings";
        } else if (isRoaming) {
            carrierConfig = "carrier_metered_roaming_apn_types_strings";
        } else {
            carrierConfig = "carrier_metered_apn_types_strings";
        }
        CarrierConfigManager configManager = (CarrierConfigManager) phone.getContext().getSystemService("carrier_config");
        if (configManager == null) {
            Rlog.e("ApnSetting", "Carrier config service is not available");
            return true;
        }
        PersistableBundle b = configManager.getConfigForSubId(subId);
        if (b == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Can't get the config. subId = ");
            stringBuilder.append(subId);
            Rlog.e("ApnSetting", stringBuilder.toString());
            return true;
        }
        String[] meteredApnTypes = b.getStringArray(carrierConfig);
        if (meteredApnTypes == null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(carrierConfig);
            stringBuilder2.append(" is not available. subId = ");
            stringBuilder2.append(subId);
            Rlog.e("ApnSetting", stringBuilder2.toString());
            return true;
        }
        HashSet<String> meteredApnSet = new HashSet(Arrays.asList(meteredApnTypes));
        if (meteredApnSet.contains(CharacterSets.MIMENAME_ANY_CHARSET) || meteredApnSet.contains(type)) {
            return true;
        }
        if (!type.equals(CharacterSets.MIMENAME_ANY_CHARSET) || meteredApnSet.size() <= 0) {
            return false;
        }
        return true;
    }

    public boolean isMetered(Phone phone) {
        if (phone == null) {
            return true;
        }
        for (String type : this.types) {
            if (isMeteredApnType(type, phone)) {
                return true;
            }
        }
        return false;
    }

    public boolean equals(Object o) {
        boolean z = false;
        if (!(o instanceof ApnSetting)) {
            return false;
        }
        ApnSetting other = (ApnSetting) o;
        if (this.carrier.equals(other.carrier) && this.id == other.id && this.numeric.equals(other.numeric) && this.apn.equals(other.apn) && this.proxy.equals(other.proxy) && this.mmsc.equals(other.mmsc) && this.mmsProxy.equals(other.mmsProxy) && TextUtils.equals(this.mmsPort, other.mmsPort) && this.port.equals(other.port) && TextUtils.equals(this.user, other.user) && TextUtils.equals(this.password, other.password) && this.authType == other.authType && Arrays.deepEquals(this.types, other.types) && this.typesBitmap == other.typesBitmap && this.protocol.equals(other.protocol) && this.roamingProtocol.equals(other.roamingProtocol) && this.carrierEnabled == other.carrierEnabled && this.bearer == other.bearer && this.bearerBitmask == other.bearerBitmask && this.profileId == other.profileId && this.modemCognitive == other.modemCognitive && this.maxConns == other.maxConns && this.waitTime == other.waitTime && this.maxConnsTime == other.maxConnsTime && this.mtu == other.mtu && this.mvnoType.equals(other.mvnoType) && this.mvnoMatchData.equals(other.mvnoMatchData) && this.networkTypeBitmask == other.networkTypeBitmask && this.apnSetId == other.apnSetId) {
            z = true;
        }
        return z;
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public boolean equals(Object o, boolean isDataRoaming) {
        boolean z = false;
        if (!(o instanceof ApnSetting)) {
            return false;
        }
        ApnSetting other = (ApnSetting) o;
        if (this.carrier.equals(other.carrier) && this.numeric.equals(other.numeric) && this.apn.equals(other.apn) && this.proxy.equals(other.proxy) && this.mmsc.equals(other.mmsc) && this.mmsProxy.equals(other.mmsProxy) && TextUtils.equals(this.mmsPort, other.mmsPort) && this.port.equals(other.port) && TextUtils.equals(this.user, other.user) && TextUtils.equals(this.password, other.password) && this.authType == other.authType && Arrays.deepEquals(this.types, other.types) && this.typesBitmap == other.typesBitmap && ((isDataRoaming || this.protocol.equals(other.protocol)) && ((!isDataRoaming || this.roamingProtocol.equals(other.roamingProtocol)) && this.carrierEnabled == other.carrierEnabled && this.profileId == other.profileId && this.modemCognitive == other.modemCognitive && this.maxConns == other.maxConns && this.waitTime == other.waitTime && this.maxConnsTime == other.maxConnsTime && this.mtu == other.mtu && this.mvnoType.equals(other.mvnoType) && this.mvnoMatchData.equals(other.mvnoMatchData) && this.apnSetId == other.apnSetId))) {
            z = true;
        }
        return z;
    }

    public boolean similar(ApnSetting other) {
        return !canHandleType("dun") && !other.canHandleType("dun") && Objects.equals(this.apn, other.apn) && !typeSameAny(this, other) && xorEquals(this.proxy, other.proxy) && xorEquals(this.port, other.port) && xorEquals(this.protocol, other.protocol) && xorEquals(this.roamingProtocol, other.roamingProtocol) && this.carrierEnabled == other.carrierEnabled && this.bearerBitmask == other.bearerBitmask && this.profileId == other.profileId && Objects.equals(this.mvnoType, other.mvnoType) && Objects.equals(this.mvnoMatchData, other.mvnoMatchData) && xorEquals(this.mmsc, other.mmsc) && xorEquals(this.mmsProxy, other.mmsProxy) && xorEquals(this.mmsPort, other.mmsPort) && this.networkTypeBitmask == other.networkTypeBitmask && this.apnSetId == other.apnSetId;
    }

    private boolean typeSameAny(ApnSetting first, ApnSetting second) {
        int index1 = 0;
        while (index1 < first.types.length) {
            int index2 = 0;
            while (index2 < second.types.length) {
                if (first.types[index1].equals(CharacterSets.MIMENAME_ANY_CHARSET) || second.types[index2].equals(CharacterSets.MIMENAME_ANY_CHARSET) || first.types[index1].equals(second.types[index2])) {
                    return true;
                }
                index2++;
            }
            index1++;
        }
        return false;
    }

    private boolean xorEquals(String first, String second) {
        return Objects.equals(first, second) || TextUtils.isEmpty(first) || TextUtils.isEmpty(second);
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static int getApnBitmask(String apn) {
        int i;
        switch (apn.hashCode()) {
            case 42:
                if (apn.equals(CharacterSets.MIMENAME_ANY_CHARSET)) {
                    i = 10;
                    break;
                }
            case 3352:
                if (apn.equals("ia")) {
                    i = 8;
                    break;
                }
            case 98292:
                if (apn.equals("cbs")) {
                    i = 7;
                    break;
                }
            case 99837:
                if (apn.equals("dun")) {
                    i = 3;
                    break;
                }
            case 104399:
                if (apn.equals("ims")) {
                    i = 6;
                    break;
                }
            case 108243:
                if (apn.equals("mms")) {
                    i = 1;
                    break;
                }
            case 3149046:
                if (apn.equals("fota")) {
                    i = 5;
                    break;
                }
            case 3541982:
                if (apn.equals("supl")) {
                    i = 2;
                    break;
                }
            case 99285510:
                if (apn.equals("hipri")) {
                    i = 4;
                    break;
                }
            case 1544803905:
                if (apn.equals("default")) {
                    i = 0;
                    break;
                }
            case 1629013393:
                if (apn.equals("emergency")) {
                    i = 9;
                    break;
                }
            default:
                i = -1;
                break;
        }
        switch (i) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 8;
            case 4:
                return 16;
            case 5:
                return 32;
            case 6:
                return 64;
            case 7:
                return 128;
            case 8:
                return 256;
            case 9:
                return 512;
            case 10:
                return ApnTypes.ALL;
            default:
                return 0;
        }
    }
}
