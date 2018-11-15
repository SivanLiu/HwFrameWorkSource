package com.android.server.wifi;

import android.content.pm.UserInfo;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.StaticIpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.GroupCipher;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.PairwiseCipher;
import android.net.wifi.WifiConfiguration.Protocol;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiScanner.PnoSettings.PnoNetwork;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.util.NativeUtil;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class WifiConfigurationUtil {
    private static final int ENCLOSING_QUTOES_LEN = 2;
    @VisibleForTesting
    public static final String PASSWORD_MASK = "*";
    private static final int PSK_ASCII_MAX_LEN = 65;
    private static final int PSK_ASCII_MIN_LEN = 10;
    private static final int PSK_HEX_LEN = 64;
    private static final int SSID_HEX_MAX_LEN = 64;
    private static final int SSID_HEX_MIN_LEN = 2;
    private static final int SSID_UTF_8_MAX_LEN = 34;
    private static final int SSID_UTF_8_MIN_LEN = 3;
    private static final String TAG = "WifiConfigurationUtil";
    public static final boolean VALIDATE_FOR_ADD = true;
    public static final boolean VALIDATE_FOR_UPDATE = false;

    public static abstract class WifiConfigurationComparator implements Comparator<WifiConfiguration> {
        private static final int ENABLED_NETWORK_SCORE = 3;
        private static final int PERMANENTLY_DISABLED_NETWORK_SCORE = 1;
        private static final int TEMPORARY_DISABLED_NETWORK_SCORE = 2;

        abstract int compareNetworksWithSameStatus(WifiConfiguration wifiConfiguration, WifiConfiguration wifiConfiguration2);

        public int compare(WifiConfiguration a, WifiConfiguration b) {
            int configAScore = getNetworkStatusScore(a);
            int configBScore = getNetworkStatusScore(b);
            if (configAScore == configBScore) {
                return compareNetworksWithSameStatus(a, b);
            }
            return Integer.compare(configBScore, configAScore);
        }

        private int getNetworkStatusScore(WifiConfiguration config) {
            if (config.getNetworkSelectionStatus().isNetworkEnabled()) {
                return 3;
            }
            if (config.getNetworkSelectionStatus().isNetworkTemporaryDisabled()) {
                return 2;
            }
            return 1;
        }
    }

    public static boolean isVisibleToAnyProfile(WifiConfiguration config, List<UserInfo> profiles) {
        return config.shared || doesUidBelongToAnyProfile(config.creatorUid, profiles);
    }

    public static boolean doesUidBelongToAnyProfile(int uid, List<UserInfo> profiles) {
        int userId = UserHandle.getUserId(uid);
        for (UserInfo profile : profiles) {
            if (profile.id == userId) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAnyValidWepKey(String[] wepKeys) {
        for (String str : wepKeys) {
            if (str != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isConfigForPskNetwork(WifiConfiguration config) {
        return config.allowedKeyManagement.get(1) || config.allowedKeyManagement.get(6) || config.allowedKeyManagement.get(8) || config.allowedKeyManagement.get(10);
    }

    public static boolean isConfigForEapNetwork(WifiConfiguration config) {
        return config.allowedKeyManagement.get(2) || config.allowedKeyManagement.get(3) || config.allowedKeyManagement.get(7);
    }

    public static boolean isConfigForWepNetwork(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(0) && hasAnyValidWepKey(config.wepKeys)) {
            return true;
        }
        return false;
    }

    public static boolean isConfigForCertNetwork(WifiConfiguration config) {
        return config.allowedKeyManagement.get(9) || config.allowedKeyManagement.get(11);
    }

    public static boolean isConfigForOpenNetwork(WifiConfiguration config) {
        return (isConfigForWepNetwork(config) || isConfigForPskNetwork(config) || isConfigForEapNetwork(config) || isConfigForCertNetwork(config)) ? false : true;
    }

    public static boolean hasIpChanged(WifiConfiguration existingConfig, WifiConfiguration newConfig) {
        if (existingConfig.getIpAssignment() != newConfig.getIpAssignment()) {
            return true;
        }
        if (newConfig.getIpAssignment() == IpAssignment.STATIC) {
            return Objects.equals(existingConfig.getStaticIpConfiguration(), newConfig.getStaticIpConfiguration()) ^ true;
        }
        return false;
    }

    public static boolean hasProxyChanged(WifiConfiguration existingConfig, WifiConfiguration newConfig) {
        boolean z = true;
        if (existingConfig == null) {
            if (newConfig.getProxySettings() == ProxySettings.NONE) {
                z = false;
            }
            return z;
        } else if (newConfig.getProxySettings() != existingConfig.getProxySettings()) {
            return true;
        } else {
            return true ^ Objects.equals(existingConfig.getHttpProxy(), newConfig.getHttpProxy());
        }
    }

    /* JADX WARNING: Missing block: B:19:0x0057, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting
    public static boolean hasEnterpriseConfigChanged(WifiEnterpriseConfig existingEnterpriseConfig, WifiEnterpriseConfig newEnterpriseConfig) {
        if (existingEnterpriseConfig == null || newEnterpriseConfig == null) {
            if (!(existingEnterpriseConfig == null && newEnterpriseConfig == null)) {
                return true;
            }
        } else if (!(existingEnterpriseConfig.getEapMethod() == newEnterpriseConfig.getEapMethod() && existingEnterpriseConfig.getPhase2Method() == newEnterpriseConfig.getPhase2Method() && TextUtils.equals(existingEnterpriseConfig.getIdentity(), newEnterpriseConfig.getIdentity()) && TextUtils.equals(existingEnterpriseConfig.getAnonymousIdentity(), newEnterpriseConfig.getAnonymousIdentity()) && TextUtils.equals(existingEnterpriseConfig.getPassword(), newEnterpriseConfig.getPassword()) && Arrays.equals(existingEnterpriseConfig.getCaCertificates(), newEnterpriseConfig.getCaCertificates()))) {
            return true;
        }
        return false;
    }

    public static boolean hasCredentialChanged(WifiConfiguration existingConfig, WifiConfiguration newConfig) {
        if (Objects.equals(existingConfig.allowedKeyManagement, newConfig.allowedKeyManagement) && Objects.equals(existingConfig.allowedProtocols, newConfig.allowedProtocols) && Objects.equals(existingConfig.allowedAuthAlgorithms, newConfig.allowedAuthAlgorithms) && Objects.equals(existingConfig.allowedPairwiseCiphers, newConfig.allowedPairwiseCiphers) && Objects.equals(existingConfig.allowedGroupCiphers, newConfig.allowedGroupCiphers) && Objects.equals(existingConfig.preSharedKey, newConfig.preSharedKey) && Arrays.equals(existingConfig.wepKeys, newConfig.wepKeys) && existingConfig.wepTxKeyIndex == newConfig.wepTxKeyIndex && existingConfig.hiddenSSID == newConfig.hiddenSSID && !hasEnterpriseConfigChanged(existingConfig.enterpriseConfig, newConfig.enterpriseConfig)) {
            return false;
        }
        return true;
    }

    private static boolean validateSsid(String ssid, boolean isAdd) {
        if (isAdd) {
            if (ssid == null) {
                Log.e(TAG, "validateSsid : null string");
                return false;
            }
        } else if (ssid == null) {
            return true;
        }
        if (ssid.isEmpty()) {
            Log.e(TAG, "validateSsid failed: empty string");
            return false;
        }
        StringBuilder stringBuilder;
        String str;
        StringBuilder stringBuilder2;
        if (ssid.startsWith("\"")) {
            byte[] ssidBytes = ssid.getBytes(StandardCharsets.UTF_8);
            if (ssidBytes.length < 3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("validateSsid failed: utf-8 ssid string size too small: ");
                stringBuilder.append(ssidBytes.length);
                Log.e(str, stringBuilder.toString());
                return false;
            } else if (ssidBytes.length > 34) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("validateSsid failed: utf-8 ssid string size too large: ");
                stringBuilder.append(ssidBytes.length);
                Log.e(str, stringBuilder.toString());
                return false;
            }
        } else if (ssid.length() < 2) {
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("validateSsid failed: hex string size too small: ");
            stringBuilder2.append(ssid.length());
            Log.e(str, stringBuilder2.toString());
            return false;
        } else if (ssid.length() > 64) {
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("validateSsid failed: hex string size too large: ");
            stringBuilder2.append(ssid.length());
            Log.e(str, stringBuilder2.toString());
            return false;
        }
        try {
            NativeUtil.decodeSsid(ssid);
            return true;
        } catch (IllegalArgumentException e) {
            String str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("validateSsid failed: malformed string: ");
            stringBuilder.append(ssid);
            Log.e(str2, stringBuilder.toString());
            return false;
        }
    }

    private static boolean validatePsk(String psk, boolean isAdd) {
        if (isAdd) {
            if (psk == null) {
                Log.e(TAG, "validatePsk: null string");
                return false;
            }
        } else if (psk == null || psk.equals("*")) {
            return true;
        }
        if (psk.isEmpty()) {
            Log.e(TAG, "validatePsk failed: empty string");
            return false;
        }
        StringBuilder stringBuilder;
        String str;
        if (psk.startsWith("\"")) {
            byte[] pskBytes = psk.getBytes(StandardCharsets.US_ASCII);
            if (pskBytes.length < 10) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("validatePsk failed: ascii string size too small: ");
                stringBuilder.append(pskBytes.length);
                Log.e(str, stringBuilder.toString());
                return false;
            } else if (pskBytes.length > 65) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("validatePsk failed: ascii string size too large: ");
                stringBuilder.append(pskBytes.length);
                Log.e(str, stringBuilder.toString());
                return false;
            }
        } else if (psk.length() != 64) {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("validatePsk failed: hex string size mismatch: ");
            stringBuilder2.append(psk.length());
            Log.e(str, stringBuilder2.toString());
            return false;
        }
        try {
            NativeUtil.hexOrQuotedStringToBytes(psk);
            return true;
        } catch (IllegalArgumentException e) {
            String str2 = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("validatePsk failed: malformed string: ");
            stringBuilder.append(psk);
            Log.e(str2, stringBuilder.toString());
            return false;
        }
    }

    private static boolean validateBitSet(BitSet bitSet, int validValuesLength) {
        if (bitSet == null) {
            return false;
        }
        BitSet clonedBitset = (BitSet) bitSet.clone();
        clonedBitset.clear(0, validValuesLength);
        return clonedBitset.isEmpty();
    }

    private static boolean validateBitSets(WifiConfiguration config) {
        String str;
        StringBuilder stringBuilder;
        if (!validateBitSet(config.allowedKeyManagement, KeyMgmt.strings.length)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("validateBitsets failed: invalid allowedKeyManagement bitset ");
            stringBuilder.append(config.allowedKeyManagement);
            Log.e(str, stringBuilder.toString());
            return false;
        } else if (!validateBitSet(config.allowedProtocols, Protocol.strings.length)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("validateBitsets failed: invalid allowedProtocols bitset ");
            stringBuilder.append(config.allowedProtocols);
            Log.e(str, stringBuilder.toString());
            return false;
        } else if (!validateBitSet(config.allowedAuthAlgorithms, AuthAlgorithm.strings.length)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("validateBitsets failed: invalid allowedAuthAlgorithms bitset ");
            stringBuilder.append(config.allowedAuthAlgorithms);
            Log.e(str, stringBuilder.toString());
            return false;
        } else if (!validateBitSet(config.allowedGroupCiphers, GroupCipher.strings.length)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("validateBitsets failed: invalid allowedGroupCiphers bitset ");
            stringBuilder.append(config.allowedGroupCiphers);
            Log.e(str, stringBuilder.toString());
            return false;
        } else if (validateBitSet(config.allowedPairwiseCiphers, PairwiseCipher.strings.length)) {
            return true;
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("validateBitsets failed: invalid allowedPairwiseCiphers bitset ");
            stringBuilder.append(config.allowedPairwiseCiphers);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    private static boolean validateKeyMgmt(BitSet keyMgmnt) {
        if (keyMgmnt.cardinality() > 1) {
            if (keyMgmnt.cardinality() != 2) {
                Log.e(TAG, "validateKeyMgmt failed: cardinality != 2");
                return false;
            } else if (!keyMgmnt.get(2)) {
                Log.e(TAG, "validateKeyMgmt failed: not WPA_EAP");
                return false;
            } else if (!(keyMgmnt.get(3) || keyMgmnt.get(1))) {
                Log.e(TAG, "validateKeyMgmt failed: not PSK or 8021X");
                return false;
            }
        }
        return true;
    }

    private static boolean validateIpConfiguration(IpConfiguration ipConfig) {
        if (ipConfig == null) {
            Log.e(TAG, "validateIpConfiguration failed: null IpConfiguration");
            return false;
        }
        if (ipConfig.getIpAssignment() == IpAssignment.STATIC) {
            StaticIpConfiguration staticIpConfig = ipConfig.getStaticIpConfiguration();
            if (staticIpConfig == null) {
                Log.e(TAG, "validateIpConfiguration failed: null StaticIpConfiguration");
                return false;
            } else if (staticIpConfig.ipAddress == null) {
                Log.e(TAG, "validateIpConfiguration failed: null static ip Address");
                return false;
            }
        }
        return true;
    }

    public static boolean validate(WifiConfiguration config, boolean isAdd) {
        if (!validateSsid(config.SSID, isAdd) || !validateBitSets(config) || !validateKeyMgmt(config.allowedKeyManagement)) {
            return false;
        }
        if ((!config.allowedKeyManagement.get(1) || validatePsk(config.preSharedKey, isAdd)) && validateIpConfiguration(config.getIpConfiguration())) {
            return true;
        }
        return false;
    }

    /* JADX WARNING: Missing block: B:17:0x0026, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isSameNetwork(WifiConfiguration config, WifiConfiguration config1) {
        if (config == null && config1 == null) {
            return true;
        }
        if (config == null || config1 == null || config.networkId != config1.networkId || !Objects.equals(config.SSID, config1.SSID) || hasCredentialChanged(config, config1)) {
            return false;
        }
        return true;
    }

    public static PnoNetwork createPnoNetwork(WifiConfiguration config) {
        PnoNetwork pnoNetwork = new PnoNetwork(config.SSID);
        if (config.hiddenSSID) {
            pnoNetwork.flags = (byte) (pnoNetwork.flags | 1);
        }
        pnoNetwork.flags = (byte) (pnoNetwork.flags | 2);
        pnoNetwork.flags = (byte) (pnoNetwork.flags | 4);
        if (config.allowedKeyManagement.get(1)) {
            pnoNetwork.authBitField = (byte) (pnoNetwork.authBitField | 2);
        } else if (config.allowedKeyManagement.get(2) || config.allowedKeyManagement.get(3)) {
            pnoNetwork.authBitField = (byte) (pnoNetwork.authBitField | 4);
        } else {
            pnoNetwork.authBitField = (byte) (pnoNetwork.authBitField | 1);
        }
        return pnoNetwork;
    }
}
