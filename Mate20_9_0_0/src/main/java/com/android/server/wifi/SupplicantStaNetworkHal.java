package com.android.server.wifi;

import android.content.Context;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.NetworkResponseEapSimGsmAuthParams;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetwork.NetworkResponseEapSimUmtsAuthParams;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback.NetworkRequestEapSimGsmAuthParams;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback.NetworkRequestEapSimUmtsAuthParams;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaNetworkCallback.Stub;
import android.hardware.wifi.supplicant.V1_0.SupplicantStatus;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.HidlSupport.Mutable;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.MutableBoolean;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.server.wifi.WifiBackupRestore.SupplicantBackupMigration;
import com.android.server.wifi.scanner.ScanResultRecords;
import com.android.server.wifi.util.NativeUtil;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.concurrent.ThreadSafe;
import org.json.JSONException;
import org.json.JSONObject;

@ThreadSafe
public class SupplicantStaNetworkHal {
    private static final Pattern GSM_AUTH_RESPONSE_PARAMS_PATTERN = Pattern.compile(":([0-9a-fA-F]+):([0-9a-fA-F]+)");
    @VisibleForTesting
    public static final String ID_STRING_KEY_CONFIG_KEY = "configKey";
    @VisibleForTesting
    public static final String ID_STRING_KEY_CREATOR_UID = "creatorUid";
    @VisibleForTesting
    public static final String ID_STRING_KEY_FQDN = "fqdn";
    private static final String TAG = "SupplicantStaNetworkHal";
    private static final Pattern UMTS_AUTH_RESPONSE_PARAMS_PATTERN = Pattern.compile("^:([0-9a-fA-F]+):([0-9a-fA-F]+):([0-9a-fA-F]+)$");
    private static final Pattern UMTS_AUTS_RESPONSE_PARAMS_PATTERN = Pattern.compile("^:([0-9a-fA-F]+)$");
    private int mAuthAlgMask;
    private byte[] mBssid;
    private String mEapAltSubjectMatch;
    private ArrayList<Byte> mEapAnonymousIdentity;
    private String mEapCACert;
    private String mEapCAPath;
    private String mEapClientCert;
    private String mEapDomainSuffixMatch;
    private boolean mEapEngine;
    private String mEapEngineID;
    private ArrayList<Byte> mEapIdentity;
    private int mEapMethod;
    private ArrayList<Byte> mEapPassword;
    private int mEapPhase2Method;
    private String mEapPrivateKeyId;
    private String mEapSubjectMatch;
    private int mGroupCipherMask;
    private ISupplicantStaNetwork mISupplicantStaNetwork;
    private ISupplicantStaNetworkCallback mISupplicantStaNetworkCallback;
    private String mIdStr;
    private final String mIfaceName;
    private int mKeyMgmtMask;
    private final Object mLock = new Object();
    private int mNetworkId;
    private int mPairwiseCipherMask;
    private int mProtoMask;
    private byte[] mPsk;
    private String mPskPassphrase;
    private boolean mRequirePmf;
    private boolean mScanSsid;
    private ArrayList<Byte> mSsid;
    private boolean mSystemSupportsFastBssTransition = false;
    private boolean mVerboseLoggingEnabled = false;
    private ArrayList<Byte> mWepKey;
    private int mWepTxKeyIdx;
    private final WifiMonitor mWifiMonitor;

    private class SupplicantStaNetworkHalCallback extends Stub {
        private final int mFramewokNetworkId;
        private final String mSsid;

        SupplicantStaNetworkHalCallback(int framewokNetworkId, String ssid) {
            this.mFramewokNetworkId = framewokNetworkId;
            this.mSsid = ssid;
        }

        public void onNetworkEapSimGsmAuthRequest(NetworkRequestEapSimGsmAuthParams params) {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                SupplicantStaNetworkHal.this.logCallback("onNetworkEapSimGsmAuthRequest");
                String[] data = new String[params.rands.size()];
                int i = 0;
                Iterator it = params.rands.iterator();
                while (it.hasNext()) {
                    int i2 = i + 1;
                    data[i] = NativeUtil.hexStringFromByteArray((byte[]) it.next());
                    i = i2;
                }
                SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkGsmAuthRequestEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid, data);
            }
        }

        public void onNetworkEapSimUmtsAuthRequest(NetworkRequestEapSimUmtsAuthParams params) {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                SupplicantStaNetworkHal.this.logCallback("onNetworkEapSimUmtsAuthRequest");
                String randHex = NativeUtil.hexStringFromByteArray(params.rand);
                String autnHex = NativeUtil.hexStringFromByteArray(params.autn);
                SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkUmtsAuthRequestEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid, new String[]{randHex, autnHex});
            }
        }

        public void onNetworkEapIdentityRequest() {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                SupplicantStaNetworkHal.this.logCallback("onNetworkEapIdentityRequest");
                SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkIdentityRequestEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid);
            }
        }
    }

    private class VendorSupplicantStaNetworkHalCallback extends vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaNetworkCallback.Stub {
        private SupplicantStaNetworkHalCallback callback;
        private final int mFramewokNetworkId;
        private final String mSsid;

        VendorSupplicantStaNetworkHalCallback(int framewokNetworkId, String ssid) {
            this.mFramewokNetworkId = framewokNetworkId;
            this.mSsid = ssid;
            this.callback = new SupplicantStaNetworkHalCallback(framewokNetworkId, ssid);
        }

        public void onNetworkEapSimGsmAuthRequest(NetworkRequestEapSimGsmAuthParams params) {
            this.callback.onNetworkEapSimGsmAuthRequest(params);
        }

        public void onNetworkEapSimUmtsAuthRequest(NetworkRequestEapSimUmtsAuthParams params) {
            this.callback.onNetworkEapSimUmtsAuthRequest(params);
        }

        public void onNetworkEapIdentityRequest() {
            this.callback.onNetworkEapIdentityRequest();
        }

        public void onNetworkEapNotificationErrorCode(int errorcode) {
            synchronized (SupplicantStaNetworkHal.this.mLock) {
                String str = SupplicantStaNetworkHal.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onNetworkEapNotificationErrorCode errorcode = ");
                stringBuilder.append(errorcode);
                Log.e(str, stringBuilder.toString());
                SupplicantStaNetworkHal.this.mWifiMonitor.broadcastNetworkEAPErrorcodeReportEvent(SupplicantStaNetworkHal.this.mIfaceName, this.mFramewokNetworkId, this.mSsid, errorcode);
            }
        }
    }

    SupplicantStaNetworkHal(ISupplicantStaNetwork iSupplicantStaNetwork, String ifaceName, Context context, WifiMonitor monitor) {
        this.mISupplicantStaNetwork = iSupplicantStaNetwork;
        this.mIfaceName = ifaceName;
        this.mWifiMonitor = monitor;
        this.mSystemSupportsFastBssTransition = context.getResources().getBoolean(17957077);
    }

    void enableVerboseLogging(boolean enable) {
        synchronized (this.mLock) {
            this.mVerboseLoggingEnabled = enable;
        }
    }

    public boolean loadWifiConfiguration(WifiConfiguration config, Map<String, String> networkExtras) {
        synchronized (this.mLock) {
            int i = 0;
            if (config == null) {
                return false;
            }
            config.SSID = null;
            if (!getSsid() || ArrayUtils.isEmpty(this.mSsid)) {
                Log.e(TAG, "failed to read ssid");
                return false;
            }
            config.SSID = NativeUtil.encodeSsid(this.mSsid);
            config.networkId = -1;
            if (getId()) {
                config.networkId = this.mNetworkId;
                config.getNetworkSelectionStatus().setNetworkSelectionBSSID(null);
                if (getBssid() && !ArrayUtils.isEmpty(this.mBssid)) {
                    config.getNetworkSelectionStatus().setNetworkSelectionBSSID(NativeUtil.macAddressFromByteArray(this.mBssid));
                }
                config.hiddenSSID = false;
                if (getScanSsid()) {
                    config.hiddenSSID = this.mScanSsid;
                }
                config.requirePMF = false;
                if (getRequirePmf()) {
                    config.requirePMF = this.mRequirePmf;
                }
                config.wepTxKeyIndex = -1;
                if (getWepTxKeyIdx()) {
                    config.wepTxKeyIndex = this.mWepTxKeyIdx;
                }
                while (i < 4) {
                    config.wepKeys[i] = null;
                    if (getWepKey(i) && !ArrayUtils.isEmpty(this.mWepKey)) {
                        config.wepKeys[i] = NativeUtil.bytesToHexOrQuotedString(this.mWepKey);
                    }
                    i++;
                }
                config.preSharedKey = null;
                if (getPskPassphrase() && !TextUtils.isEmpty(this.mPskPassphrase)) {
                    config.preSharedKey = NativeUtil.addEnclosingQuotes(this.mPskPassphrase);
                } else if (getPsk() && !ArrayUtils.isEmpty(this.mPsk)) {
                    config.preSharedKey = NativeUtil.hexStringFromByteArray(this.mPsk);
                }
                if (getKeyMgmt()) {
                    config.allowedKeyManagement = removeFastTransitionFlags(supplicantToWifiConfigurationKeyMgmtMask(this.mKeyMgmtMask));
                }
                if (getProto()) {
                    config.allowedProtocols = supplicantToWifiConfigurationProtoMask(this.mProtoMask);
                }
                if (getAuthAlg()) {
                    config.allowedAuthAlgorithms = supplicantToWifiConfigurationAuthAlgMask(this.mAuthAlgMask);
                }
                if (getGroupCipher()) {
                    config.allowedGroupCiphers = supplicantToWifiConfigurationGroupCipherMask(this.mGroupCipherMask);
                }
                if (getPairwiseCipher()) {
                    config.allowedPairwiseCiphers = supplicantToWifiConfigurationPairwiseCipherMask(this.mPairwiseCipherMask);
                }
                if (!getIdStr() || TextUtils.isEmpty(this.mIdStr)) {
                    Log.w(TAG, "getIdStr failed or empty");
                } else {
                    networkExtras.putAll(parseNetworkExtra(this.mIdStr));
                }
                boolean loadWifiEnterpriseConfig = loadWifiEnterpriseConfig(config.SSID, config.enterpriseConfig);
                return loadWifiEnterpriseConfig;
            }
            Log.e(TAG, "getId failed");
            return false;
        }
    }

    public boolean saveWifiConfiguration(WifiConfiguration config) {
        synchronized (this.mLock) {
            if (config == null) {
                return false;
            }
            if (config.SSID != null) {
                ArrayList<Byte> ssid = ScanResultRecords.getDefault().getOriSsid(config.getNetworkSelectionStatus().getNetworkSelectionBSSID(), config.SSID);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ssid=");
                stringBuilder.append(config.SSID);
                stringBuilder.append(" oriSsid=");
                stringBuilder.append(config.oriSsid);
                stringBuilder.append(" oriSsidRecord=");
                stringBuilder.append(ssid);
                Log.d(str, stringBuilder.toString());
                if (ssid == null) {
                    try {
                        if (TextUtils.isEmpty(config.oriSsid)) {
                            ssid = NativeUtil.decodeSsid(config.SSID);
                        } else {
                            ssid = NativeUtil.byteArrayToArrayList(NativeUtil.hexStringToByteArray(config.oriSsid));
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "saveWifiConfiguration: cannot be utf-8 encoded", e);
                        return false;
                    }
                }
                if (!setSsid(ssid)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("failed to set SSID: ");
                    stringBuilder.append(config.SSID);
                    Log.e(str, stringBuilder.toString());
                    return false;
                }
            }
            String bssidStr = config.getNetworkSelectionStatus().getNetworkSelectionBSSID();
            String str2;
            StringBuilder stringBuilder2;
            if (bssidStr == null || setBssid(NativeUtil.macAddressToByteArray(bssidStr))) {
                if (config.preSharedKey != null) {
                    if (config.preSharedKey.isEmpty()) {
                        Log.e(TAG, "psk is empty");
                        return false;
                    } else if (config.preSharedKey.startsWith("\"")) {
                        if (!setPskPassphrase(NativeUtil.removeEnclosingQuotes(config.preSharedKey))) {
                            Log.e(TAG, "failed to set psk passphrase");
                            return false;
                        }
                    } else if (!setPsk(NativeUtil.hexStringToByteArray(config.preSharedKey))) {
                        Log.e(TAG, "failed to set psk");
                        return false;
                    }
                }
                boolean hasSetKey = false;
                if (config.wepKeys != null) {
                    boolean hasSetKey2 = false;
                    for (int i = 0; i < config.wepKeys.length; i++) {
                        if (config.wepKeys[i] != null && config.allowedAuthAlgorithms.get(1)) {
                            if (TextUtils.isEmpty(config.wepKeys[i])) {
                                String str3 = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("index ");
                                stringBuilder3.append(i);
                                stringBuilder3.append(" key is empty");
                                Log.e(str3, stringBuilder3.toString());
                            } else if (setWepKey(i, NativeUtil.hexOrQuotedStringToBytes(config.wepKeys[i]))) {
                                hasSetKey2 = true;
                            } else {
                                String str4 = TAG;
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("failed to set wep_key ");
                                stringBuilder4.append(i);
                                Log.e(str4, stringBuilder4.toString());
                                return false;
                            }
                        }
                    }
                    hasSetKey = hasSetKey2;
                }
                if (hasSetKey && !setWepTxKeyIdx(config.wepTxKeyIndex)) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("failed to set wep_tx_keyidx: ");
                    stringBuilder2.append(config.wepTxKeyIndex);
                    Log.e(str2, stringBuilder2.toString());
                    return false;
                } else if (!setScanSsid(config.hiddenSSID)) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(config.SSID);
                    stringBuilder2.append(": failed to set hiddenSSID: ");
                    stringBuilder2.append(config.hiddenSSID);
                    Log.e(str2, stringBuilder2.toString());
                    return false;
                } else if (!setRequirePmf(config.requirePMF)) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(config.SSID);
                    stringBuilder2.append(": failed to set requirePMF: ");
                    stringBuilder2.append(config.requirePMF);
                    Log.e(str2, stringBuilder2.toString());
                    return false;
                } else if (config.allowedKeyManagement.cardinality() != 0 && !setKeyMgmt(wifiConfigurationToSupplicantKeyMgmtMask(addFastTransitionFlags(config.allowedKeyManagement)))) {
                    Log.e(TAG, "failed to set Key Management");
                    return false;
                } else if (config.allowedProtocols.cardinality() != 0 && !setProto(wifiConfigurationToSupplicantProtoMask(config.allowedProtocols))) {
                    Log.e(TAG, "failed to set Security Protocol");
                    return false;
                } else if (config.allowedAuthAlgorithms.cardinality() != 0 && !setAuthAlg(wifiConfigurationToSupplicantAuthAlgMask(config.allowedAuthAlgorithms))) {
                    Log.e(TAG, "failed to set AuthAlgorithm");
                    return false;
                } else if (config.allowedGroupCiphers.cardinality() != 0 && !setGroupCipher(wifiConfigurationToSupplicantGroupCipherMask(config.allowedGroupCiphers))) {
                    Log.e(TAG, "failed to set Group Cipher");
                    return false;
                } else if (config.allowedPairwiseCiphers.cardinality() == 0 || setPairwiseCipher(wifiConfigurationToSupplicantPairwiseCipherMask(config.allowedPairwiseCiphers))) {
                    Map<String, String> metadata = new HashMap();
                    if (config.isPasspoint()) {
                        metadata.put("fqdn", config.FQDN);
                    }
                    metadata.put("configKey", config.configKey());
                    metadata.put("creatorUid", Integer.toString(config.creatorUid));
                    if (!setIdStr(createNetworkExtra(metadata))) {
                        Log.e(TAG, "failed to set id string");
                        return false;
                    } else if (config.updateIdentifier != null && !setUpdateIdentifier(Integer.parseInt(config.updateIdentifier))) {
                        Log.e(TAG, "failed to set update identifier");
                        return false;
                    } else if (config.enterpriseConfig != null && config.enterpriseConfig.getEapMethod() != -1 && !saveWifiEnterpriseConfig(config.SSID, config.enterpriseConfig)) {
                        return false;
                    } else if (!isVendorSupplicantStaNetworkV2_0()) {
                        this.mISupplicantStaNetworkCallback = new SupplicantStaNetworkHalCallback(config.networkId, config.SSID);
                        if (registerCallback(this.mISupplicantStaNetworkCallback)) {
                            return true;
                        }
                        Log.e(TAG, "Failed to register callback");
                        return false;
                    } else if (setWapiConfiguration(config)) {
                        vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaNetworkCallback vendorNetworkCallback = new VendorSupplicantStaNetworkHalCallback(config.networkId, config.SSID);
                        if (hwStaNetworkRegisterCallback(vendorNetworkCallback)) {
                            this.mISupplicantStaNetworkCallback = vendorNetworkCallback;
                            Log.d(TAG, "Successfully seved vendor network configuration.");
                            return true;
                        }
                        Log.e(TAG, "Failed to register callback");
                        return false;
                    } else {
                        return false;
                    }
                } else {
                    Log.e(TAG, "failed to set PairwiseCipher");
                    return false;
                }
            }
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("failed to set BSSID: ");
            stringBuilder2.append(bssidStr);
            Log.e(str2, stringBuilder2.toString());
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:77:0x013a, code:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean loadWifiEnterpriseConfig(String ssid, WifiEnterpriseConfig eapConfig) {
        synchronized (this.mLock) {
            if (eapConfig == null) {
                return false;
            } else if (getEapMethod()) {
                eapConfig.setEapMethod(supplicantToWifiConfigurationEapMethod(this.mEapMethod));
                if (getEapPhase2Method()) {
                    eapConfig.setPhase2Method(supplicantToWifiConfigurationEapPhase2Method(this.mEapPhase2Method));
                    if (getEapIdentity() && !ArrayUtils.isEmpty(this.mEapIdentity)) {
                        eapConfig.setFieldValue("identity", NativeUtil.stringFromByteArrayList(this.mEapIdentity));
                    }
                    if (getEapAnonymousIdentity() && !ArrayUtils.isEmpty(this.mEapAnonymousIdentity)) {
                        eapConfig.setFieldValue("anonymous_identity", NativeUtil.stringFromByteArrayList(this.mEapAnonymousIdentity));
                    }
                    if (getEapPassword() && !ArrayUtils.isEmpty(this.mEapPassword)) {
                        eapConfig.setFieldValue("password", NativeUtil.stringFromByteArrayList(this.mEapPassword));
                    }
                    if (getEapClientCert() && !TextUtils.isEmpty(this.mEapClientCert)) {
                        eapConfig.setFieldValue(SupplicantBackupMigration.SUPPLICANT_KEY_CLIENT_CERT, this.mEapClientCert);
                    }
                    if (getEapCACert() && !TextUtils.isEmpty(this.mEapCACert)) {
                        eapConfig.setFieldValue(SupplicantBackupMigration.SUPPLICANT_KEY_CA_CERT, this.mEapCACert);
                    }
                    if (getEapSubjectMatch() && !TextUtils.isEmpty(this.mEapSubjectMatch)) {
                        eapConfig.setFieldValue("subject_match", this.mEapSubjectMatch);
                    }
                    if (getEapEngineID() && !TextUtils.isEmpty(this.mEapEngineID)) {
                        eapConfig.setFieldValue("engine_id", this.mEapEngineID);
                    }
                    if (getEapEngine() && !TextUtils.isEmpty(this.mEapEngineID)) {
                        String str;
                        String str2 = "engine";
                        if (this.mEapEngine) {
                            str = "1";
                        } else {
                            str = "0";
                        }
                        eapConfig.setFieldValue(str2, str);
                    }
                    if (getEapPrivateKeyId() && !TextUtils.isEmpty(this.mEapPrivateKeyId)) {
                        eapConfig.setFieldValue("key_id", this.mEapPrivateKeyId);
                    }
                    if (getEapAltSubjectMatch() && !TextUtils.isEmpty(this.mEapAltSubjectMatch)) {
                        eapConfig.setFieldValue("altsubject_match", this.mEapAltSubjectMatch);
                    }
                    if (getEapDomainSuffixMatch() && !TextUtils.isEmpty(this.mEapDomainSuffixMatch)) {
                        eapConfig.setFieldValue("domain_suffix_match", this.mEapDomainSuffixMatch);
                    }
                    if (getEapCAPath() && !TextUtils.isEmpty(this.mEapCAPath)) {
                        eapConfig.setFieldValue(SupplicantBackupMigration.SUPPLICANT_KEY_CA_PATH, this.mEapCAPath);
                    }
                } else {
                    Log.e(TAG, "failed to get eap phase2 method");
                    return false;
                }
            } else {
                Log.e(TAG, "failed to get eap method. Assumimg not an enterprise network");
                return true;
            }
        }
    }

    private boolean saveWifiEnterpriseConfig(String ssid, WifiEnterpriseConfig eapConfig) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (eapConfig == null) {
                return false;
            } else if (!setEapMethod(wifiConfigurationToSupplicantEapMethod(eapConfig.getEapMethod()))) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(ssid);
                stringBuilder.append(": failed to set eap method: ");
                stringBuilder.append(eapConfig.getEapMethod());
                Log.e(str, stringBuilder.toString());
                return false;
            } else if (setEapPhase2Method(wifiConfigurationToSupplicantEapPhase2Method(eapConfig.getPhase2Method()))) {
                str = eapConfig.getFieldValue("identity");
                String str2;
                StringBuilder stringBuilder2;
                if (TextUtils.isEmpty(str) || setEapIdentity(NativeUtil.stringToByteArrayList(str))) {
                    str = eapConfig.getFieldValue("anonymous_identity");
                    if (TextUtils.isEmpty(str) || setEapAnonymousIdentity(NativeUtil.stringToByteArrayList(str))) {
                        str = eapConfig.getFieldValue("password");
                        if (TextUtils.isEmpty(str) || setEapPassword(NativeUtil.stringToByteArrayList(str))) {
                            str = eapConfig.getFieldValue(SupplicantBackupMigration.SUPPLICANT_KEY_CLIENT_CERT);
                            if (TextUtils.isEmpty(str) || setEapClientCert(str)) {
                                str = eapConfig.getFieldValue(SupplicantBackupMigration.SUPPLICANT_KEY_CA_CERT);
                                if (TextUtils.isEmpty(str) || setEapCACert(str)) {
                                    str = eapConfig.getFieldValue("subject_match");
                                    if (TextUtils.isEmpty(str) || setEapSubjectMatch(str)) {
                                        str = eapConfig.getFieldValue("engine_id");
                                        if (TextUtils.isEmpty(str) || setEapEngineID(str)) {
                                            str = eapConfig.getFieldValue("engine");
                                            if (TextUtils.isEmpty(str) || setEapEngine(str.equals("1"))) {
                                                str = eapConfig.getFieldValue("key_id");
                                                if (TextUtils.isEmpty(str) || setEapPrivateKeyId(str)) {
                                                    str = eapConfig.getFieldValue("altsubject_match");
                                                    if (TextUtils.isEmpty(str) || setEapAltSubjectMatch(str)) {
                                                        str = eapConfig.getFieldValue("domain_suffix_match");
                                                        if (TextUtils.isEmpty(str) || setEapDomainSuffixMatch(str)) {
                                                            str = eapConfig.getFieldValue(SupplicantBackupMigration.SUPPLICANT_KEY_CA_PATH);
                                                            if (TextUtils.isEmpty(str) || setEapCAPath(str)) {
                                                                str = eapConfig.getFieldValue("proactive_key_caching");
                                                                if (TextUtils.isEmpty(str) || setEapProactiveKeyCaching(str.equals("1"))) {
                                                                    return true;
                                                                }
                                                                str2 = TAG;
                                                                stringBuilder2 = new StringBuilder();
                                                                stringBuilder2.append(ssid);
                                                                stringBuilder2.append(": failed to set proactive key caching: ");
                                                                stringBuilder2.append(str);
                                                                Log.e(str2, stringBuilder2.toString());
                                                                return false;
                                                            }
                                                            str2 = TAG;
                                                            stringBuilder2 = new StringBuilder();
                                                            stringBuilder2.append(ssid);
                                                            stringBuilder2.append(": failed to set eap ca path: ");
                                                            stringBuilder2.append(str);
                                                            Log.e(str2, stringBuilder2.toString());
                                                            return false;
                                                        }
                                                        str2 = TAG;
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append(ssid);
                                                        stringBuilder2.append(": failed to set eap domain suffix match: ");
                                                        stringBuilder2.append(str);
                                                        Log.e(str2, stringBuilder2.toString());
                                                        return false;
                                                    }
                                                    str2 = TAG;
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append(ssid);
                                                    stringBuilder2.append(": failed to set eap alt subject match: ");
                                                    stringBuilder2.append(str);
                                                    Log.e(str2, stringBuilder2.toString());
                                                    return false;
                                                }
                                                str2 = TAG;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append(ssid);
                                                stringBuilder2.append(": failed to set eap private key: ");
                                                stringBuilder2.append(str);
                                                Log.e(str2, stringBuilder2.toString());
                                                return false;
                                            }
                                            str2 = TAG;
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append(ssid);
                                            stringBuilder2.append(": failed to set eap engine: ");
                                            stringBuilder2.append(str);
                                            Log.e(str2, stringBuilder2.toString());
                                            return false;
                                        }
                                        str2 = TAG;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append(ssid);
                                        stringBuilder2.append(": failed to set eap engine id: ");
                                        stringBuilder2.append(str);
                                        Log.e(str2, stringBuilder2.toString());
                                        return false;
                                    }
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append(ssid);
                                    stringBuilder2.append(": failed to set eap subject match: ");
                                    stringBuilder2.append(str);
                                    Log.e(str2, stringBuilder2.toString());
                                    return false;
                                }
                                str2 = TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append(ssid);
                                stringBuilder2.append(": failed to set eap ca cert: ");
                                stringBuilder2.append(str);
                                Log.e(str2, stringBuilder2.toString());
                                return false;
                            }
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(ssid);
                            stringBuilder2.append(": failed to set eap client cert: ");
                            stringBuilder2.append(str);
                            Log.e(str2, stringBuilder2.toString());
                            return false;
                        }
                        str2 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(ssid);
                        stringBuilder2.append(": failed to set eap password");
                        Log.e(str2, stringBuilder2.toString());
                        return false;
                    }
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(ssid);
                    stringBuilder2.append(": failed to set eap anonymous identity: ");
                    stringBuilder2.append(str);
                    Log.e(str2, stringBuilder2.toString());
                    return false;
                }
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(ssid);
                stringBuilder2.append(": failed to set eap identity: ");
                stringBuilder2.append(str);
                Log.e(str2, stringBuilder2.toString());
                return false;
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(ssid);
                stringBuilder.append(": failed to set eap phase 2 method: ");
                stringBuilder.append(eapConfig.getPhase2Method());
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
    }

    private static int wifiConfigurationToSupplicantKeyMgmtMask(BitSet keyMgmt) {
        int mask = 0;
        int bit = keyMgmt.nextSetBit(0);
        while (bit != -1) {
            switch (bit) {
                case 0:
                    mask |= 4;
                    break;
                case 1:
                    mask |= 2;
                    break;
                case 2:
                    mask |= 1;
                    break;
                case 3:
                    mask |= 8;
                    break;
                case 5:
                    mask |= 32768;
                    break;
                case 6:
                    mask |= 64;
                    break;
                case 7:
                    mask |= 32;
                    break;
                case 8:
                    mask |= 4096;
                    break;
                case 9:
                    mask |= 8192;
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid protoMask bit in keyMgmt: ");
                    stringBuilder.append(bit);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
            bit = keyMgmt.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantProtoMask(BitSet protoMask) {
        int mask = 0;
        int bit = protoMask.nextSetBit(0);
        while (bit != -1) {
            switch (bit) {
                case 0:
                    mask |= 1;
                    break;
                case 1:
                    mask |= 2;
                    break;
                case 2:
                    mask |= 8;
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid protoMask bit in wificonfig: ");
                    stringBuilder.append(bit);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
            bit = protoMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantAuthAlgMask(BitSet authAlgMask) {
        int mask = 0;
        int bit = authAlgMask.nextSetBit(0);
        while (bit != -1) {
            switch (bit) {
                case 0:
                    mask |= 1;
                    break;
                case 1:
                    mask |= 2;
                    break;
                case 2:
                    mask |= 4;
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid authAlgMask bit in wificonfig: ");
                    stringBuilder.append(bit);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
            bit = authAlgMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantGroupCipherMask(BitSet groupCipherMask) {
        int mask = 0;
        int bit = groupCipherMask.nextSetBit(0);
        while (bit != -1) {
            switch (bit) {
                case 0:
                    mask |= 2;
                    break;
                case 1:
                    mask |= 4;
                    break;
                case 2:
                    mask |= 8;
                    break;
                case 3:
                    mask |= 16;
                    break;
                case 4:
                    mask |= 16384;
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid GroupCipherMask bit in wificonfig: ");
                    stringBuilder.append(bit);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
            bit = groupCipherMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int wifiConfigurationToSupplicantPairwiseCipherMask(BitSet pairwiseCipherMask) {
        int mask = 0;
        int bit = pairwiseCipherMask.nextSetBit(0);
        while (bit != -1) {
            switch (bit) {
                case 0:
                    mask |= 1;
                    break;
                case 1:
                    mask |= 8;
                    break;
                case 2:
                    mask |= 16;
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid pairwiseCipherMask bit in wificonfig: ");
                    stringBuilder.append(bit);
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
            bit = pairwiseCipherMask.nextSetBit(bit + 1);
        }
        return mask;
    }

    private static int supplicantToWifiConfigurationEapMethod(int value) {
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invalid eap method value from supplicant: ");
                stringBuilder.append(value);
                Log.e(str, stringBuilder.toString());
                return -1;
        }
    }

    private static int supplicantToWifiConfigurationEapPhase2Method(int value) {
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invalid eap phase2 method value from supplicant: ");
                stringBuilder.append(value);
                Log.e(str, stringBuilder.toString());
                return -1;
        }
    }

    private static int supplicantMaskValueToWifiConfigurationBitSet(int supplicantMask, int supplicantValue, BitSet bitset, int bitSetPosition) {
        bitset.set(bitSetPosition, (supplicantMask & supplicantValue) == supplicantValue);
        return (~supplicantValue) & supplicantMask;
    }

    private static BitSet supplicantToWifiConfigurationKeyMgmtMask(int mask) {
        BitSet bitset = new BitSet();
        mask = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 4, bitset, 0), 2, bitset, 1), 1, bitset, 2), 8, bitset, 3), 32768, bitset, 5), 64, bitset, 6), 32, bitset, 7);
        if (mask == 0) {
            return bitset;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid key mgmt mask from supplicant: ");
        stringBuilder.append(mask);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static BitSet supplicantToWifiConfigurationProtoMask(int mask) {
        BitSet bitset = new BitSet();
        mask = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 1, bitset, 0), 2, bitset, 1), 8, bitset, 2);
        if (mask == 0) {
            return bitset;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid proto mask from supplicant: ");
        stringBuilder.append(mask);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static BitSet supplicantToWifiConfigurationAuthAlgMask(int mask) {
        BitSet bitset = new BitSet();
        mask = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 1, bitset, 0), 2, bitset, 1), 4, bitset, 2);
        if (mask == 0) {
            return bitset;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid auth alg mask from supplicant: ");
        stringBuilder.append(mask);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static BitSet supplicantToWifiConfigurationGroupCipherMask(int mask) {
        BitSet bitset = new BitSet();
        mask = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 2, bitset, 0), 4, bitset, 1), 8, bitset, 2), 16, bitset, 3), 16384, bitset, 4);
        if (mask == 0) {
            return bitset;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid group cipher mask from supplicant: ");
        stringBuilder.append(mask);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static BitSet supplicantToWifiConfigurationPairwiseCipherMask(int mask) {
        BitSet bitset = new BitSet();
        mask = supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(supplicantMaskValueToWifiConfigurationBitSet(mask, 1, bitset, 0), 8, bitset, 1), 16, bitset, 2);
        if (mask == 0) {
            return bitset;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid pairwise cipher mask from supplicant: ");
        stringBuilder.append(mask);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private static int wifiConfigurationToSupplicantEapMethod(int value) {
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invalid eap method value from WifiConfiguration: ");
                stringBuilder.append(value);
                Log.e(str, stringBuilder.toString());
                return -1;
        }
    }

    private static int wifiConfigurationToSupplicantEapPhase2Method(int value) {
        switch (value) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            case 6:
                return 6;
            case 7:
                return 7;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invalid eap phase2 method value from WifiConfiguration: ");
                stringBuilder.append(value);
                Log.e(str, stringBuilder.toString());
                return -1;
        }
    }

    private boolean getId() {
        synchronized (this.mLock) {
            String methodStr = "getId";
            if (checkISupplicantStaNetworkAndLogFailure("getId")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getId(new -$$Lambda$SupplicantStaNetworkHal$IRxqwt7Zayh6hYF7VQ3jt-Epcrc(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getId");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getId$0(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, int idValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mNetworkId = idValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getId");
        }
    }

    private boolean registerCallback(ISupplicantStaNetworkCallback callback) {
        synchronized (this.mLock) {
            String methodStr = "registerCallback";
            if (checkISupplicantStaNetworkAndLogFailure("registerCallback")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.registerCallback(callback), "registerCallback");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "registerCallback");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setSsid(ArrayList<Byte> ssid) {
        synchronized (this.mLock) {
            String methodStr = "setSsid";
            if (checkISupplicantStaNetworkAndLogFailure("setSsid")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setSsid(ssid), "setSsid");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setSsid");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean setBssid(String bssidStr) {
        boolean bssid;
        synchronized (this.mLock) {
            try {
                bssid = setBssid(NativeUtil.macAddressToByteArray(bssidStr));
            } catch (IllegalArgumentException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal argument ");
                stringBuilder.append(bssidStr);
                Log.e(str, stringBuilder.toString(), e);
                return false;
            }
        }
        return bssid;
    }

    private boolean setBssid(byte[] bssid) {
        synchronized (this.mLock) {
            String methodStr = "setBssid";
            if (checkISupplicantStaNetworkAndLogFailure("setBssid")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setBssid(bssid), "setBssid");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setBssid");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setScanSsid(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setScanSsid";
            if (checkISupplicantStaNetworkAndLogFailure("setScanSsid")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setScanSsid(enable), "setScanSsid");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setScanSsid");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setKeyMgmt(int keyMgmtMask) {
        synchronized (this.mLock) {
            String methodStr = "setKeyMgmt";
            if (checkISupplicantStaNetworkAndLogFailure("setKeyMgmt")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setKeyMgmt(keyMgmtMask), "setKeyMgmt");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setKeyMgmt");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setProto(int protoMask) {
        synchronized (this.mLock) {
            String methodStr = "setProto";
            if (checkISupplicantStaNetworkAndLogFailure("setProto")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setProto(protoMask), "setProto");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setProto");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setAuthAlg(int authAlgMask) {
        synchronized (this.mLock) {
            String methodStr = "setAuthAlg";
            if (checkISupplicantStaNetworkAndLogFailure("setAuthAlg")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setAuthAlg(authAlgMask), "setAuthAlg");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setAuthAlg");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setGroupCipher(int groupCipherMask) {
        synchronized (this.mLock) {
            String methodStr = "setGroupCipher";
            if (checkISupplicantStaNetworkAndLogFailure("setGroupCipher")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setGroupCipher(groupCipherMask), "setGroupCipher");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setGroupCipher");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setPairwiseCipher(int pairwiseCipherMask) {
        synchronized (this.mLock) {
            String methodStr = "setPairwiseCipher";
            if (checkISupplicantStaNetworkAndLogFailure("setPairwiseCipher")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setPairwiseCipher(pairwiseCipherMask), "setPairwiseCipher");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setPairwiseCipher");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setPskPassphrase(String psk) {
        synchronized (this.mLock) {
            String methodStr = "setPskPassphrase";
            if (checkISupplicantStaNetworkAndLogFailure("setPskPassphrase")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setPskPassphrase(psk), "setPskPassphrase");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setPskPassphrase");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setPsk(byte[] psk) {
        synchronized (this.mLock) {
            String methodStr = "setPsk";
            if (checkISupplicantStaNetworkAndLogFailure("setPsk")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setPsk(psk), "setPsk");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setPsk");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setWepKey(int keyIdx, ArrayList<Byte> wepKey) {
        synchronized (this.mLock) {
            String methodStr = "setWepKey";
            if (checkISupplicantStaNetworkAndLogFailure("setWepKey")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setWepKey(keyIdx, wepKey), "setWepKey");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWepKey");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setWepTxKeyIdx(int keyIdx) {
        synchronized (this.mLock) {
            String methodStr = "setWepTxKeyIdx";
            if (checkISupplicantStaNetworkAndLogFailure("setWepTxKeyIdx")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setWepTxKeyIdx(keyIdx), "setWepTxKeyIdx");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWepTxKeyIdx");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setRequirePmf(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setRequirePmf";
            if (checkISupplicantStaNetworkAndLogFailure("setRequirePmf")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setRequirePmf(enable), "setRequirePmf");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setRequirePmf");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setUpdateIdentifier(int identifier) {
        synchronized (this.mLock) {
            String methodStr = "setUpdateIdentifier";
            if (checkISupplicantStaNetworkAndLogFailure("setUpdateIdentifier")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setUpdateIdentifier(identifier), "setUpdateIdentifier");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setUpdateIdentifier");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapMethod(int method) {
        synchronized (this.mLock) {
            String methodStr = "setEapMethod";
            if (checkISupplicantStaNetworkAndLogFailure("setEapMethod")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapMethod(method), "setEapMethod");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapMethod");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapPhase2Method(int method) {
        synchronized (this.mLock) {
            String methodStr = "setEapPhase2Method";
            if (checkISupplicantStaNetworkAndLogFailure("setEapPhase2Method")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapPhase2Method(method), "setEapPhase2Method");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapPhase2Method");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapIdentity(ArrayList<Byte> identity) {
        synchronized (this.mLock) {
            String methodStr = "setEapIdentity";
            if (checkISupplicantStaNetworkAndLogFailure("setEapIdentity")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapIdentity(identity), "setEapIdentity");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapIdentity");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapAnonymousIdentity(ArrayList<Byte> identity) {
        synchronized (this.mLock) {
            String methodStr = "setEapAnonymousIdentity";
            if (checkISupplicantStaNetworkAndLogFailure("setEapAnonymousIdentity")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapAnonymousIdentity(identity), "setEapAnonymousIdentity");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapAnonymousIdentity");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapPassword(ArrayList<Byte> password) {
        synchronized (this.mLock) {
            String methodStr = "setEapPassword";
            if (checkISupplicantStaNetworkAndLogFailure("setEapPassword")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapPassword(password), "setEapPassword");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapPassword");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapCACert(String path) {
        synchronized (this.mLock) {
            String methodStr = "setEapCACert";
            if (checkISupplicantStaNetworkAndLogFailure("setEapCACert")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapCACert(path), "setEapCACert");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapCACert");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapCAPath(String path) {
        synchronized (this.mLock) {
            String methodStr = "setEapCAPath";
            if (checkISupplicantStaNetworkAndLogFailure("setEapCAPath")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapCAPath(path), "setEapCAPath");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapCAPath");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapClientCert(String path) {
        synchronized (this.mLock) {
            String methodStr = "setEapClientCert";
            if (checkISupplicantStaNetworkAndLogFailure("setEapClientCert")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapClientCert(path), "setEapClientCert");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapClientCert");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapPrivateKeyId(String id) {
        synchronized (this.mLock) {
            String methodStr = "setEapPrivateKeyId";
            if (checkISupplicantStaNetworkAndLogFailure("setEapPrivateKeyId")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapPrivateKeyId(id), "setEapPrivateKeyId");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapPrivateKeyId");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapSubjectMatch(String match) {
        synchronized (this.mLock) {
            String methodStr = "setEapSubjectMatch";
            if (checkISupplicantStaNetworkAndLogFailure("setEapSubjectMatch")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapSubjectMatch(match), "setEapSubjectMatch");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapSubjectMatch");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapAltSubjectMatch(String match) {
        synchronized (this.mLock) {
            String methodStr = "setEapAltSubjectMatch";
            if (checkISupplicantStaNetworkAndLogFailure("setEapAltSubjectMatch")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapAltSubjectMatch(match), "setEapAltSubjectMatch");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapAltSubjectMatch");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapEngine(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setEapEngine";
            if (checkISupplicantStaNetworkAndLogFailure("setEapEngine")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapEngine(enable), "setEapEngine");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapEngine");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapEngineID(String id) {
        synchronized (this.mLock) {
            String methodStr = "setEapEngineID";
            if (checkISupplicantStaNetworkAndLogFailure("setEapEngineID")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapEngineID(id), "setEapEngineID");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapEngineID");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapDomainSuffixMatch(String match) {
        synchronized (this.mLock) {
            String methodStr = "setEapDomainSuffixMatch";
            if (checkISupplicantStaNetworkAndLogFailure("setEapDomainSuffixMatch")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setEapDomainSuffixMatch(match), "setEapDomainSuffixMatch");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapDomainSuffixMatch");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setEapProactiveKeyCaching(boolean enable) {
        synchronized (this.mLock) {
            String methodStr = "setEapProactiveKeyCaching";
            if (checkISupplicantStaNetworkAndLogFailure("setEapProactiveKeyCaching")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setProactiveKeyCaching(enable), "setEapProactiveKeyCaching");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setEapProactiveKeyCaching");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setIdStr(String idString) {
        synchronized (this.mLock) {
            String methodStr = "setIdStr";
            if (checkISupplicantStaNetworkAndLogFailure("setIdStr")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.setIdStr(idString), "setIdStr");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setIdStr");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean getSsid() {
        synchronized (this.mLock) {
            String methodStr = "getSsid";
            if (checkISupplicantStaNetworkAndLogFailure("getSsid")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getSsid(new -$$Lambda$SupplicantStaNetworkHal$dUChvV6L83ism85zKYVAhy_jP0M(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getSsid");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getSsid$1(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, ArrayList ssidValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mSsid = ssidValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getSsid");
        }
    }

    private boolean getBssid() {
        synchronized (this.mLock) {
            String methodStr = "getBssid";
            if (checkISupplicantStaNetworkAndLogFailure("getBssid")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getBssid(new -$$Lambda$SupplicantStaNetworkHal$6382Rt_N9IWM5_ofahWKN9I6IBU(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getBssid");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getBssid$2(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, byte[] bssidValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mBssid = bssidValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getBssid");
        }
    }

    private boolean getScanSsid() {
        synchronized (this.mLock) {
            String methodStr = "getScanSsid";
            if (checkISupplicantStaNetworkAndLogFailure("getScanSsid")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getScanSsid(new -$$Lambda$SupplicantStaNetworkHal$s22206N0y0P61x6iZFmcY8wHLUs(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getScanSsid");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getScanSsid$3(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, boolean enabledValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mScanSsid = enabledValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getScanSsid");
        }
    }

    private boolean getKeyMgmt() {
        synchronized (this.mLock) {
            String methodStr = "getKeyMgmt";
            if (checkISupplicantStaNetworkAndLogFailure("getKeyMgmt")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getKeyMgmt(new -$$Lambda$SupplicantStaNetworkHal$94_sN-P7lmR3x-O_DkNKgE4cVRw(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getKeyMgmt");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getKeyMgmt$4(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, int keyMgmtMaskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mKeyMgmtMask = keyMgmtMaskValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getKeyMgmt");
        }
    }

    private boolean getProto() {
        synchronized (this.mLock) {
            String methodStr = "getProto";
            if (checkISupplicantStaNetworkAndLogFailure("getProto")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getProto(new -$$Lambda$SupplicantStaNetworkHal$Wb2vJf7tZ_hiqFbe0Fygtypp6sY(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getProto");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getProto$5(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, int protoMaskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mProtoMask = protoMaskValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getProto");
        }
    }

    private boolean getAuthAlg() {
        synchronized (this.mLock) {
            String methodStr = "getAuthAlg";
            if (checkISupplicantStaNetworkAndLogFailure("getAuthAlg")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getAuthAlg(new -$$Lambda$SupplicantStaNetworkHal$57ytpnr8Sp3UGVvxEJ5230fLLTY(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getAuthAlg");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getAuthAlg$6(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, int authAlgMaskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mAuthAlgMask = authAlgMaskValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getAuthAlg");
        }
    }

    private boolean getGroupCipher() {
        synchronized (this.mLock) {
            String methodStr = "getGroupCipher";
            if (checkISupplicantStaNetworkAndLogFailure("getGroupCipher")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getGroupCipher(new -$$Lambda$SupplicantStaNetworkHal$rwAunRMwc7t4KILZpqpRZsbXFtM(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getGroupCipher");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getGroupCipher$7(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, int groupCipherMaskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mGroupCipherMask = groupCipherMaskValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getGroupCipher");
        }
    }

    private boolean getPairwiseCipher() {
        synchronized (this.mLock) {
            String methodStr = "getPairwiseCipher";
            if (checkISupplicantStaNetworkAndLogFailure("getPairwiseCipher")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getPairwiseCipher(new -$$Lambda$SupplicantStaNetworkHal$0jcKc7WXuB0Arhk10QAm3C9QEIE(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getPairwiseCipher");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getPairwiseCipher$8(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, int pairwiseCipherMaskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mPairwiseCipherMask = pairwiseCipherMaskValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getPairwiseCipher");
        }
    }

    private boolean getPskPassphrase() {
        synchronized (this.mLock) {
            String methodStr = "getPskPassphrase";
            if (checkISupplicantStaNetworkAndLogFailure("getPskPassphrase")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getPskPassphrase(new -$$Lambda$SupplicantStaNetworkHal$XB3HOYX--pQXZf5aoG3QXx0bxsQ(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getPskPassphrase");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getPskPassphrase$9(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, String pskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mPskPassphrase = pskValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getPskPassphrase");
        }
    }

    private boolean getPsk() {
        synchronized (this.mLock) {
            String methodStr = "getPsk";
            if (checkISupplicantStaNetworkAndLogFailure("getPsk")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getPsk(new -$$Lambda$SupplicantStaNetworkHal$O5yJ5gFO1MyXR7e5Vzh2R7tA2yw(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getPsk");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getPsk$10(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, byte[] pskValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mPsk = pskValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getPsk");
        }
    }

    private boolean getWepKey(int keyIdx) {
        synchronized (this.mLock) {
            String methodStr = "keyIdx";
            if (checkISupplicantStaNetworkAndLogFailure("keyIdx")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getWepKey(keyIdx, new -$$Lambda$SupplicantStaNetworkHal$LyGGO18bsqW92nxZnd_9Qw7-dsk(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "keyIdx");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getWepKey$11(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, ArrayList wepKeyValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mWepKey = wepKeyValue;
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("keyIdx,  failed: ");
        stringBuilder.append(status.debugMessage);
        Log.e(str, stringBuilder.toString());
    }

    private boolean getWepTxKeyIdx() {
        synchronized (this.mLock) {
            String methodStr = "getWepTxKeyIdx";
            if (checkISupplicantStaNetworkAndLogFailure("getWepTxKeyIdx")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getWepTxKeyIdx(new -$$Lambda$SupplicantStaNetworkHal$FYivkP8udIcXe0AkNuzvoqJF-rk(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getWepTxKeyIdx");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getWepTxKeyIdx$12(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, int keyIdxValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mWepTxKeyIdx = keyIdxValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getWepTxKeyIdx");
        }
    }

    private boolean getRequirePmf() {
        synchronized (this.mLock) {
            String methodStr = "getRequirePmf";
            if (checkISupplicantStaNetworkAndLogFailure("getRequirePmf")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getRequirePmf(new -$$Lambda$SupplicantStaNetworkHal$pD8kZFw93kpsZJ5TzAfSDwKXKys(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getRequirePmf");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getRequirePmf$13(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, boolean enabledValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mRequirePmf = enabledValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getRequirePmf");
        }
    }

    private boolean getEapMethod() {
        synchronized (this.mLock) {
            String methodStr = "getEapMethod";
            if (checkISupplicantStaNetworkAndLogFailure("getEapMethod")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapMethod(new -$$Lambda$SupplicantStaNetworkHal$wGNJ26OxvyJHYq1fe4S7Vh-N90Q(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapMethod");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapMethod$14(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, int methodValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapMethod = methodValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapMethod");
        }
    }

    private boolean getEapPhase2Method() {
        synchronized (this.mLock) {
            String methodStr = "getEapPhase2Method";
            if (checkISupplicantStaNetworkAndLogFailure("getEapPhase2Method")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapPhase2Method(new -$$Lambda$SupplicantStaNetworkHal$_l89XL2JTVAQUm99mVDJqv6rv3k(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapPhase2Method");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapPhase2Method$15(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, int methodValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapPhase2Method = methodValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapPhase2Method");
        }
    }

    private boolean getEapIdentity() {
        synchronized (this.mLock) {
            String methodStr = "getEapIdentity";
            if (checkISupplicantStaNetworkAndLogFailure("getEapIdentity")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapIdentity(new -$$Lambda$SupplicantStaNetworkHal$0d43mlDQDooIp70FIGqNn7FqJ-0(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapIdentity");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapIdentity$16(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, ArrayList identityValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapIdentity = identityValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapIdentity");
        }
    }

    private boolean getEapAnonymousIdentity() {
        synchronized (this.mLock) {
            String methodStr = "getEapAnonymousIdentity";
            if (checkISupplicantStaNetworkAndLogFailure("getEapAnonymousIdentity")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapAnonymousIdentity(new -$$Lambda$SupplicantStaNetworkHal$muLEmhohkNuEm8_NtxoLaZwKyeg(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapAnonymousIdentity");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapAnonymousIdentity$17(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, ArrayList identityValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapAnonymousIdentity = identityValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapAnonymousIdentity");
        }
    }

    public String fetchEapAnonymousIdentity() {
        synchronized (this.mLock) {
            if (getEapAnonymousIdentity()) {
                String stringFromByteArrayList = NativeUtil.stringFromByteArrayList(this.mEapAnonymousIdentity);
                return stringFromByteArrayList;
            }
            return null;
        }
    }

    private boolean getEapPassword() {
        synchronized (this.mLock) {
            String methodStr = "getEapPassword";
            if (checkISupplicantStaNetworkAndLogFailure("getEapPassword")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapPassword(new -$$Lambda$SupplicantStaNetworkHal$D76zmkeBYzFLYGofNu4UEppt9Ns(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapPassword");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapPassword$18(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, ArrayList passwordValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapPassword = passwordValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapPassword");
        }
    }

    private boolean getEapCACert() {
        synchronized (this.mLock) {
            String methodStr = "getEapCACert";
            if (checkISupplicantStaNetworkAndLogFailure("getEapCACert")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapCACert(new -$$Lambda$SupplicantStaNetworkHal$YGzwGo-1owFPzwLa5QB6NnyFDmA(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapCACert");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapCACert$19(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, String pathValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapCACert = pathValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapCACert");
        }
    }

    private boolean getEapCAPath() {
        synchronized (this.mLock) {
            String methodStr = "getEapCAPath";
            if (checkISupplicantStaNetworkAndLogFailure("getEapCAPath")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapCAPath(new -$$Lambda$SupplicantStaNetworkHal$y8X5Cv5711T-YReB8k1RE02gJfk(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapCAPath");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapCAPath$20(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, String pathValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapCAPath = pathValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapCAPath");
        }
    }

    private boolean getEapClientCert() {
        synchronized (this.mLock) {
            String methodStr = "getEapClientCert";
            if (checkISupplicantStaNetworkAndLogFailure("getEapClientCert")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapClientCert(new -$$Lambda$SupplicantStaNetworkHal$FcnwBKRdWmEJtv77LMWU0WTUZJk(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapClientCert");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapClientCert$21(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, String pathValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapClientCert = pathValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapClientCert");
        }
    }

    private boolean getEapPrivateKeyId() {
        synchronized (this.mLock) {
            String methodStr = "getEapPrivateKeyId";
            if (checkISupplicantStaNetworkAndLogFailure("getEapPrivateKeyId")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapPrivateKeyId(new -$$Lambda$SupplicantStaNetworkHal$tCanx7OFdyKhND7roxuvHiN2sng(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapPrivateKeyId");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapPrivateKeyId$22(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, String idValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapPrivateKeyId = idValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapPrivateKeyId");
        }
    }

    private boolean getEapSubjectMatch() {
        synchronized (this.mLock) {
            String methodStr = "getEapSubjectMatch";
            if (checkISupplicantStaNetworkAndLogFailure("getEapSubjectMatch")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapSubjectMatch(new -$$Lambda$SupplicantStaNetworkHal$lY0ry_ysqL3zOfiGl5o4h2YmrRc(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapSubjectMatch");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapSubjectMatch$23(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, String matchValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapSubjectMatch = matchValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapSubjectMatch");
        }
    }

    private boolean getEapAltSubjectMatch() {
        synchronized (this.mLock) {
            String methodStr = "getEapAltSubjectMatch";
            if (checkISupplicantStaNetworkAndLogFailure("getEapAltSubjectMatch")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapAltSubjectMatch(new -$$Lambda$SupplicantStaNetworkHal$o9n9RIGl_Plk5abyuUH8o4a3FWI(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapAltSubjectMatch");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapAltSubjectMatch$24(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, String matchValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapAltSubjectMatch = matchValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapAltSubjectMatch");
        }
    }

    private boolean getEapEngine() {
        synchronized (this.mLock) {
            String methodStr = "getEapEngine";
            if (checkISupplicantStaNetworkAndLogFailure("getEapEngine")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapEngine(new -$$Lambda$SupplicantStaNetworkHal$7tt4ABo2IFUGzb5YtygzlkNXmOE(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapEngine");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapEngine$25(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, boolean enabledValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapEngine = enabledValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapEngine");
        }
    }

    private boolean getEapEngineID() {
        synchronized (this.mLock) {
            String methodStr = "getEapEngineID";
            if (checkISupplicantStaNetworkAndLogFailure("getEapEngineID")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapEngineID(new -$$Lambda$SupplicantStaNetworkHal$UWYFgd0wI1IqmkI0ludQ7z31Oas(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapEngineID");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapEngineID$26(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, String idValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapEngineID = idValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapEngineID");
        }
    }

    private boolean getEapDomainSuffixMatch() {
        synchronized (this.mLock) {
            String methodStr = "getEapDomainSuffixMatch";
            if (checkISupplicantStaNetworkAndLogFailure("getEapDomainSuffixMatch")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getEapDomainSuffixMatch(new -$$Lambda$SupplicantStaNetworkHal$8QJCMB9JFU3IyKDwHmhPVE_ai_8(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getEapDomainSuffixMatch");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getEapDomainSuffixMatch$27(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, String matchValue) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mEapDomainSuffixMatch = matchValue;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getEapDomainSuffixMatch");
        }
    }

    private boolean getIdStr() {
        synchronized (this.mLock) {
            String methodStr = "getIdStr";
            if (checkISupplicantStaNetworkAndLogFailure("getIdStr")) {
                try {
                    MutableBoolean statusOk = new MutableBoolean(false);
                    this.mISupplicantStaNetwork.getIdStr(new -$$Lambda$SupplicantStaNetworkHal$SntX2N0HMHZYbsvv6YNLsqbnccs(this, statusOk));
                    boolean z = statusOk.value;
                    return z;
                } catch (RemoteException e) {
                    handleRemoteException(e, "getIdStr");
                    return false;
                }
            }
            return false;
        }
    }

    public static /* synthetic */ void lambda$getIdStr$28(SupplicantStaNetworkHal supplicantStaNetworkHal, MutableBoolean statusOk, SupplicantStatus status, String idString) {
        statusOk.value = status.code == 0;
        if (statusOk.value) {
            supplicantStaNetworkHal.mIdStr = idString;
        } else {
            supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getIdStr");
        }
    }

    private boolean enable(boolean noConnect) {
        synchronized (this.mLock) {
            String methodStr = "enable";
            if (checkISupplicantStaNetworkAndLogFailure("enable")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.enable(noConnect), "enable");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "enable");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean disable() {
        synchronized (this.mLock) {
            String methodStr = "disable";
            if (checkISupplicantStaNetworkAndLogFailure("disable")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.disable(), "disable");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "disable");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean select() {
        synchronized (this.mLock) {
            String methodStr = "select";
            if (checkISupplicantStaNetworkAndLogFailure("select")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.select(), "select");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "select");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean sendNetworkEapSimGsmAuthResponse(String paramsStr) {
        synchronized (this.mLock) {
            try {
                String str;
                StringBuilder stringBuilder;
                Matcher match = GSM_AUTH_RESPONSE_PARAMS_PATTERN.matcher(paramsStr);
                ArrayList params = new ArrayList();
                while (match.find()) {
                    if (match.groupCount() != 2) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Malformed gsm auth response params: ");
                        stringBuilder.append(paramsStr);
                        Log.e(str, stringBuilder.toString());
                        return false;
                    }
                    NetworkResponseEapSimGsmAuthParams param = new NetworkResponseEapSimGsmAuthParams();
                    byte[] kc = NativeUtil.hexStringToByteArray(match.group(1));
                    if (kc == null || kc.length != param.kc.length) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Invalid kc value: ");
                        stringBuilder2.append(match.group(1));
                        Log.e(str2, stringBuilder2.toString());
                        return false;
                    }
                    byte[] sres = NativeUtil.hexStringToByteArray(match.group(2));
                    if (sres == null || sres.length != param.sres.length) {
                        String str3 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Invalid sres value: ");
                        stringBuilder3.append(match.group(2));
                        Log.e(str3, stringBuilder3.toString());
                        return false;
                    }
                    System.arraycopy(kc, 0, param.kc, 0, param.kc.length);
                    System.arraycopy(sres, 0, param.sres, 0, param.sres.length);
                    params.add(param);
                }
                if (params.size() > 3 || params.size() < 2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Malformed gsm auth response params: ");
                    stringBuilder.append(paramsStr);
                    Log.e(str, stringBuilder.toString());
                    return false;
                }
                boolean sendNetworkEapSimGsmAuthResponse = sendNetworkEapSimGsmAuthResponse(params);
                return sendNetworkEapSimGsmAuthResponse;
            } catch (IllegalArgumentException e) {
                String str4 = TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("Illegal argument ");
                stringBuilder4.append(paramsStr);
                Log.e(str4, stringBuilder4.toString(), e);
                return false;
            }
        }
    }

    private boolean sendNetworkEapSimGsmAuthResponse(ArrayList<NetworkResponseEapSimGsmAuthParams> params) {
        synchronized (this.mLock) {
            String methodStr = "sendNetworkEapSimGsmAuthResponse";
            if (checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimGsmAuthResponse")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimGsmAuthResponse(params), "sendNetworkEapSimGsmAuthResponse");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "sendNetworkEapSimGsmAuthResponse");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean sendNetworkEapSimGsmAuthFailure() {
        synchronized (this.mLock) {
            String methodStr = "sendNetworkEapSimGsmAuthFailure";
            if (checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimGsmAuthFailure")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimGsmAuthFailure(), "sendNetworkEapSimGsmAuthFailure");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "sendNetworkEapSimGsmAuthFailure");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean sendNetworkEapSimUmtsAuthResponse(String paramsStr) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            try {
                Matcher match = UMTS_AUTH_RESPONSE_PARAMS_PATTERN.matcher(paramsStr);
                if (match.find() && match.groupCount() == 3) {
                    NetworkResponseEapSimUmtsAuthParams params = new NetworkResponseEapSimUmtsAuthParams();
                    byte[] ik = NativeUtil.hexStringToByteArray(match.group(1));
                    String str2;
                    if (ik == null || ik.length != params.ik.length) {
                        str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Invalid ik value: ");
                        stringBuilder2.append(match.group(1));
                        Log.e(str2, stringBuilder2.toString());
                        return false;
                    }
                    byte[] ck = NativeUtil.hexStringToByteArray(match.group(2));
                    if (ck == null || ck.length != params.ck.length) {
                        str2 = TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Invalid ck value: ");
                        stringBuilder3.append(match.group(2));
                        Log.e(str2, stringBuilder3.toString());
                        return false;
                    }
                    byte[] res = NativeUtil.hexStringToByteArray(match.group(3));
                    if (res == null || res.length == 0) {
                        String str3 = TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Invalid res value: ");
                        stringBuilder4.append(match.group(3));
                        Log.e(str3, stringBuilder4.toString());
                        return false;
                    }
                    System.arraycopy(ik, 0, params.ik, 0, params.ik.length);
                    System.arraycopy(ck, 0, params.ck, 0, params.ck.length);
                    for (byte b : res) {
                        params.res.add(Byte.valueOf(b));
                    }
                    return sendNetworkEapSimUmtsAuthResponse(params);
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Malformed umts auth response params: ");
                stringBuilder.append(paramsStr);
                Log.e(str, stringBuilder.toString());
                return false;
            } catch (IllegalArgumentException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal argument ");
                stringBuilder.append(paramsStr);
                Log.e(str, stringBuilder.toString(), e);
                return false;
            }
        }
    }

    private boolean sendNetworkEapSimUmtsAuthResponse(NetworkResponseEapSimUmtsAuthParams params) {
        synchronized (this.mLock) {
            String methodStr = "sendNetworkEapSimUmtsAuthResponse";
            if (checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimUmtsAuthResponse")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimUmtsAuthResponse(params), "sendNetworkEapSimUmtsAuthResponse");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "sendNetworkEapSimUmtsAuthResponse");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean sendNetworkEapSimUmtsAutsResponse(String paramsStr) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            try {
                Matcher match = UMTS_AUTS_RESPONSE_PARAMS_PATTERN.matcher(paramsStr);
                if (match.find() && match.groupCount() == 1) {
                    byte[] auts = NativeUtil.hexStringToByteArray(match.group(1));
                    if (auts == null || auts.length != 14) {
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Invalid auts value: ");
                        stringBuilder2.append(match.group(1));
                        Log.e(str2, stringBuilder2.toString());
                        return false;
                    }
                    boolean sendNetworkEapSimUmtsAutsResponse = sendNetworkEapSimUmtsAutsResponse(auts);
                    return sendNetworkEapSimUmtsAutsResponse;
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Malformed umts auts response params: ");
                stringBuilder.append(paramsStr);
                Log.e(str, stringBuilder.toString());
                return false;
            } catch (IllegalArgumentException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal argument ");
                stringBuilder.append(paramsStr);
                Log.e(str, stringBuilder.toString(), e);
                return false;
            }
        }
    }

    private boolean sendNetworkEapSimUmtsAutsResponse(byte[] auts) {
        synchronized (this.mLock) {
            String methodStr = "sendNetworkEapSimUmtsAutsResponse";
            if (checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimUmtsAutsResponse")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimUmtsAutsResponse(auts), "sendNetworkEapSimUmtsAutsResponse");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "sendNetworkEapSimUmtsAutsResponse");
                    return false;
                }
            }
            return false;
        }
    }

    public boolean sendNetworkEapSimUmtsAuthFailure() {
        synchronized (this.mLock) {
            String methodStr = "sendNetworkEapSimUmtsAuthFailure";
            if (checkISupplicantStaNetworkAndLogFailure("sendNetworkEapSimUmtsAuthFailure")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(this.mISupplicantStaNetwork.sendNetworkEapSimUmtsAuthFailure(), "sendNetworkEapSimUmtsAuthFailure");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "sendNetworkEapSimUmtsAuthFailure");
                    return false;
                }
            }
            return false;
        }
    }

    protected android.hardware.wifi.supplicant.V1_1.ISupplicantStaNetwork getSupplicantStaNetworkForV1_1Mockable() {
        if (this.mISupplicantStaNetwork == null) {
            return null;
        }
        return android.hardware.wifi.supplicant.V1_1.ISupplicantStaNetwork.castFrom(this.mISupplicantStaNetwork);
    }

    public boolean sendNetworkEapIdentityResponse(String identityStr, String encryptedIdentityStr) {
        boolean sendNetworkEapIdentityResponse;
        synchronized (this.mLock) {
            try {
                ArrayList unencryptedIdentity = NativeUtil.stringToByteArrayList(identityStr);
                ArrayList encryptedIdentity = null;
                if (!TextUtils.isEmpty(encryptedIdentityStr)) {
                    encryptedIdentity = NativeUtil.stringToByteArrayList(encryptedIdentityStr);
                }
                sendNetworkEapIdentityResponse = sendNetworkEapIdentityResponse(unencryptedIdentity, encryptedIdentity);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Illegal argument identityStr");
                return false;
            }
        }
        return sendNetworkEapIdentityResponse;
    }

    private boolean sendNetworkEapIdentityResponse(ArrayList<Byte> unencryptedIdentity, ArrayList<Byte> encryptedIdentity) {
        synchronized (this.mLock) {
            String methodStr = "sendNetworkEapIdentityResponse";
            if (checkISupplicantStaNetworkAndLogFailure("sendNetworkEapIdentityResponse")) {
                try {
                    SupplicantStatus status;
                    android.hardware.wifi.supplicant.V1_1.ISupplicantStaNetwork iSupplicantStaNetworkV11 = getSupplicantStaNetworkForV1_1Mockable();
                    if (iSupplicantStaNetworkV11 == null || encryptedIdentity == null) {
                        status = this.mISupplicantStaNetwork.sendNetworkEapIdentityResponse(unencryptedIdentity);
                    } else {
                        status = iSupplicantStaNetworkV11.sendNetworkEapIdentityResponse_1_1(unencryptedIdentity, encryptedIdentity);
                    }
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(status, "sendNetworkEapIdentityResponse");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "sendNetworkEapIdentityResponse");
                    return false;
                }
            }
            return false;
        }
    }

    public String getWpsNfcConfigurationToken() {
        synchronized (this.mLock) {
            ArrayList<Byte> token = getWpsNfcConfigurationTokenInternal();
            if (token == null) {
                return null;
            }
            String hexStringFromByteArray = NativeUtil.hexStringFromByteArray(NativeUtil.byteArrayFromArrayList(token));
            return hexStringFromByteArray;
        }
    }

    private ArrayList<Byte> getWpsNfcConfigurationTokenInternal() {
        synchronized (this.mLock) {
            String methodStr = "getWpsNfcConfigurationToken";
            if (checkISupplicantStaNetworkAndLogFailure("getWpsNfcConfigurationToken")) {
                Mutable<ArrayList<Byte>> gotToken = new Mutable();
                try {
                    this.mISupplicantStaNetwork.getWpsNfcConfigurationToken(new -$$Lambda$SupplicantStaNetworkHal$0GZaozzHg6lwqD39Pjp3fQRrgE0(this, gotToken));
                } catch (RemoteException e) {
                    handleRemoteException(e, "getWpsNfcConfigurationToken");
                }
                ArrayList<Byte> arrayList = (ArrayList) gotToken.value;
                return arrayList;
            }
            return null;
        }
    }

    public static /* synthetic */ void lambda$getWpsNfcConfigurationTokenInternal$29(SupplicantStaNetworkHal supplicantStaNetworkHal, Mutable gotToken, SupplicantStatus status, ArrayList token) {
        if (supplicantStaNetworkHal.checkStatusAndLogFailure(status, "getWpsNfcConfigurationToken")) {
            gotToken.value = token;
        }
    }

    private boolean checkStatusAndLogFailure(SupplicantStatus status, String methodStr) {
        synchronized (this.mLock) {
            String str;
            StringBuilder stringBuilder;
            if (status.code != 0) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicantStaNetwork.");
                stringBuilder.append(methodStr);
                stringBuilder.append(" failed: ");
                stringBuilder.append(status);
                Log.e(str, stringBuilder.toString());
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicantStaNetwork.");
                stringBuilder.append(methodStr);
                stringBuilder.append(" succeeded");
                Log.d(str, stringBuilder.toString());
            }
            return true;
        }
    }

    private void logCallback(String methodStr) {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ISupplicantStaNetworkCallback.");
                stringBuilder.append(methodStr);
                stringBuilder.append(" received");
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private boolean checkISupplicantStaNetworkAndLogFailure(String methodStr) {
        synchronized (this.mLock) {
            if (this.mISupplicantStaNetwork == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't call ");
                stringBuilder.append(methodStr);
                stringBuilder.append(", ISupplicantStaNetwork is null");
                Log.e(str, stringBuilder.toString());
                return false;
            }
            return true;
        }
    }

    private void handleRemoteException(RemoteException e, String methodStr) {
        synchronized (this.mLock) {
            this.mISupplicantStaNetwork = null;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ISupplicantStaNetwork.");
            stringBuilder.append(methodStr);
            stringBuilder.append(" failed with exception");
            Log.e(str, stringBuilder.toString(), e);
        }
    }

    /* JADX WARNING: Missing block: B:14:0x0026, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private BitSet addFastTransitionFlags(BitSet keyManagementFlags) {
        synchronized (this.mLock) {
            if (this.mSystemSupportsFastBssTransition) {
                BitSet modifiedFlags = (BitSet) keyManagementFlags.clone();
                if (keyManagementFlags.get(1)) {
                    modifiedFlags.set(6);
                }
                if (keyManagementFlags.get(2)) {
                    modifiedFlags.set(7);
                }
            } else {
                return keyManagementFlags;
            }
        }
    }

    private BitSet removeFastTransitionFlags(BitSet keyManagementFlags) {
        BitSet modifiedFlags;
        synchronized (this.mLock) {
            modifiedFlags = (BitSet) keyManagementFlags.clone();
            modifiedFlags.clear(6);
            modifiedFlags.clear(7);
        }
        return modifiedFlags;
    }

    public static String createNetworkExtra(Map<String, String> values) {
        String str;
        StringBuilder stringBuilder;
        try {
            return URLEncoder.encode(new JSONObject(values).toString(), "UTF-8");
        } catch (NullPointerException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to serialize networkExtra: ");
            stringBuilder.append(e.toString());
            Log.e(str, stringBuilder.toString());
            return null;
        } catch (UnsupportedEncodingException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to serialize networkExtra: ");
            stringBuilder.append(e2.toString());
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }

    public static Map<String, String> parseNetworkExtra(String encoded) {
        if (TextUtils.isEmpty(encoded)) {
            return null;
        }
        try {
            JSONObject json = new JSONObject(URLDecoder.decode(encoded, "UTF-8"));
            Map<String, String> values = new HashMap();
            Iterator<?> it = json.keys();
            while (it.hasNext()) {
                String key = (String) it.next();
                Object value = json.get(key);
                if (value instanceof String) {
                    values.put(key, (String) value);
                }
            }
            return values;
        } catch (UnsupportedEncodingException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to deserialize networkExtra: ");
            stringBuilder.append(e.toString());
            Log.e(str, stringBuilder.toString());
            return null;
        } catch (JSONException e2) {
            return null;
        }
    }

    private boolean isVendorSupplicantStaNetworkV2_0() {
        return getVendorSupplicantStaNetworkV2_0() != null;
    }

    private boolean checkVendorISupplicantStaNetworkAndLogFailure(String methodStr) {
        if (!checkISupplicantStaNetworkAndLogFailure(methodStr)) {
            return false;
        }
        if (getVendorSupplicantStaNetworkV2_0() != null) {
            return true;
        }
        Log.e(TAG, "Can't cast mISupplicantStaNetwork to vendor 2.0 version");
        return false;
    }

    private vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaNetwork getVendorSupplicantStaNetworkV2_0() {
        synchronized (this.mLock) {
            if (this.mISupplicantStaNetwork == null) {
                return null;
            }
            vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaNetwork castFrom = vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaNetwork.castFrom(this.mISupplicantStaNetwork);
            return castFrom;
        }
    }

    private boolean setWapiConfiguration(WifiConfiguration config) {
        String str;
        StringBuilder stringBuilder;
        if (config.wapiPskTypeBcm != -1 && !setWapiPskKeyType(config.wapiPskTypeBcm)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(config.SSID);
            stringBuilder.append(": failed to set wapi psk key type: ");
            stringBuilder.append(config.wapiPskTypeBcm);
            Log.e(str, stringBuilder.toString());
            return false;
        } else if (!TextUtils.isEmpty(config.wapiAsCertBcm) && !setWapiAsCertPath(config.wapiAsCertBcm)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(config.SSID);
            stringBuilder.append(": failed to set wapi as cert path: ");
            stringBuilder.append(config.wapiAsCertBcm);
            Log.e(str, stringBuilder.toString());
            return false;
        } else if (TextUtils.isEmpty(config.wapiUserCertBcm) || setWapiUserCertPath(config.wapiUserCertBcm)) {
            return true;
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(config.SSID);
            stringBuilder.append(": failed to set wapi user cert path: ");
            stringBuilder.append(config.wapiUserCertBcm);
            Log.e(str, stringBuilder.toString());
            return false;
        }
    }

    private boolean hwStaNetworkRegisterCallback(vendor.huawei.hardware.wifi.supplicant.V2_0.ISupplicantStaNetworkCallback callback) {
        synchronized (this.mLock) {
            Log.d(TAG, "Start to register network callback for vendor ISupplicantStaNetwork");
            String methodStr = "hwStaNetworkRegisterCallback";
            if (checkVendorISupplicantStaNetworkAndLogFailure("hwStaNetworkRegisterCallback")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(getVendorSupplicantStaNetworkV2_0().hwStaNetworkRegisterCallback(callback), "hwStaNetworkRegisterCallback");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "hwStaNetworkRegisterCallback");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setWapiPskKeyType(int type) {
        synchronized (this.mLock) {
            String methodStr = "setWAPIPskKeyType";
            if (checkVendorISupplicantStaNetworkAndLogFailure("setWAPIPskKeyType")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(getVendorSupplicantStaNetworkV2_0().setWAPIPskKeyType(type), "setWAPIPskKeyType");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWAPIPskKeyType");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setWapiAsCertPath(String path) {
        synchronized (this.mLock) {
            String methodStr = "setWapiAsCertPath";
            if (checkVendorISupplicantStaNetworkAndLogFailure("setWapiAsCertPath")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(getVendorSupplicantStaNetworkV2_0().setWAPIASCert(path), "setWapiAsCertPath");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWapiAsCertPath");
                    return false;
                }
            }
            return false;
        }
    }

    private boolean setWapiUserCertPath(String path) {
        synchronized (this.mLock) {
            String methodStr = "setWapiUserCertPath";
            if (checkVendorISupplicantStaNetworkAndLogFailure("setWapiUserCertPath")) {
                try {
                    boolean checkStatusAndLogFailure = checkStatusAndLogFailure(getVendorSupplicantStaNetworkV2_0().setWAPIASUECert(path), "setWapiUserCertPath");
                    return checkStatusAndLogFailure;
                } catch (RemoteException e) {
                    handleRemoteException(e, "setWapiUserCertPath");
                    return false;
                }
            }
            return false;
        }
    }
}
