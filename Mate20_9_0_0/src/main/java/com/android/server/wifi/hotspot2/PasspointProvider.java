package com.android.server.wifi.hotspot2;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.hotspot2.pps.Credential.CertificateCredential;
import android.net.wifi.hotspot2.pps.Credential.SimCredential;
import android.net.wifi.hotspot2.pps.Credential.UserCredential;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.server.wifi.IMSIParameter;
import com.android.server.wifi.SIMAccessor;
import com.android.server.wifi.WifiKeyStore;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants.ANQPElementType;
import com.android.server.wifi.hotspot2.anqp.DomainNameElement;
import com.android.server.wifi.hotspot2.anqp.NAIRealmElement;
import com.android.server.wifi.hotspot2.anqp.RoamingConsortiumElement;
import com.android.server.wifi.hotspot2.anqp.ThreeGPPNetworkElement;
import com.android.server.wifi.hotspot2.anqp.eap.AuthParam;
import com.android.server.wifi.hotspot2.anqp.eap.NonEAPInnerAuth;
import com.android.server.wifi.util.InformationElementUtil.RoamingConsortium;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PasspointProvider {
    private static final String ALIAS_HS_TYPE = "HS2_";
    private static final String TAG = "PasspointProvider";
    private final AuthParam mAuthParam;
    private String mCaCertificateAlias;
    private String mClientCertificateAlias;
    private String mClientPrivateKeyAlias;
    private final PasspointConfiguration mConfig;
    private final int mCreatorUid;
    private final int mEAPMethodID;
    private boolean mHasEverConnected;
    private final IMSIParameter mImsiParameter;
    private boolean mIsShared;
    private final WifiKeyStore mKeyStore;
    private final List<String> mMatchingSIMImsiList;
    private final long mProviderId;

    public PasspointProvider(PasspointConfiguration config, WifiKeyStore keyStore, SIMAccessor simAccessor, long providerId, int creatorUid) {
        this(config, keyStore, simAccessor, providerId, creatorUid, null, null, null, false, false);
    }

    public PasspointProvider(PasspointConfiguration config, WifiKeyStore keyStore, SIMAccessor simAccessor, long providerId, int creatorUid, String caCertificateAlias, String clientCertificateAlias, String clientPrivateKeyAlias, boolean hasEverConnected, boolean isShared) {
        this.mConfig = new PasspointConfiguration(config);
        this.mKeyStore = keyStore;
        this.mProviderId = providerId;
        this.mCreatorUid = creatorUid;
        this.mCaCertificateAlias = caCertificateAlias;
        this.mClientCertificateAlias = clientCertificateAlias;
        this.mClientPrivateKeyAlias = clientPrivateKeyAlias;
        this.mHasEverConnected = hasEverConnected;
        this.mIsShared = isShared;
        if (this.mConfig.getCredential().getUserCredential() != null) {
            this.mEAPMethodID = 21;
            this.mAuthParam = new NonEAPInnerAuth(NonEAPInnerAuth.getAuthTypeID(this.mConfig.getCredential().getUserCredential().getNonEapInnerMethod()));
            this.mImsiParameter = null;
            this.mMatchingSIMImsiList = null;
        } else if (this.mConfig.getCredential().getCertCredential() != null) {
            this.mEAPMethodID = 13;
            this.mAuthParam = null;
            this.mImsiParameter = null;
            this.mMatchingSIMImsiList = null;
        } else {
            this.mEAPMethodID = this.mConfig.getCredential().getSimCredential().getEapType();
            this.mAuthParam = null;
            this.mImsiParameter = IMSIParameter.build(this.mConfig.getCredential().getSimCredential().getImsi());
            this.mMatchingSIMImsiList = simAccessor.getMatchingImsis(this.mImsiParameter);
        }
    }

    public PasspointConfiguration getConfig() {
        return new PasspointConfiguration(this.mConfig);
    }

    public String getCaCertificateAlias() {
        return this.mCaCertificateAlias;
    }

    public String getClientPrivateKeyAlias() {
        return this.mClientPrivateKeyAlias;
    }

    public String getClientCertificateAlias() {
        return this.mClientCertificateAlias;
    }

    public long getProviderId() {
        return this.mProviderId;
    }

    public int getCreatorUid() {
        return this.mCreatorUid;
    }

    public boolean getHasEverConnected() {
        return this.mHasEverConnected;
    }

    public void setHasEverConnected(boolean hasEverConnected) {
        this.mHasEverConnected = hasEverConnected;
    }

    public boolean installCertsAndKeys() {
        String certName;
        StringBuilder stringBuilder;
        if (this.mConfig.getCredential().getCaCertificate() != null) {
            certName = new StringBuilder();
            certName.append("CACERT_HS2_");
            certName.append(this.mProviderId);
            if (this.mKeyStore.putCertInKeyStore(certName.toString(), this.mConfig.getCredential().getCaCertificate())) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(ALIAS_HS_TYPE);
                stringBuilder.append(this.mProviderId);
                this.mCaCertificateAlias = stringBuilder.toString();
            } else {
                Log.e(TAG, "Failed to install CA Certificate");
                uninstallCertsAndKeys();
                return false;
            }
        }
        if (this.mConfig.getCredential().getClientPrivateKey() != null) {
            certName = new StringBuilder();
            certName.append("USRPKEY_HS2_");
            certName.append(this.mProviderId);
            if (this.mKeyStore.putKeyInKeyStore(certName.toString(), this.mConfig.getCredential().getClientPrivateKey())) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(ALIAS_HS_TYPE);
                stringBuilder.append(this.mProviderId);
                this.mClientPrivateKeyAlias = stringBuilder.toString();
            } else {
                Log.e(TAG, "Failed to install client private key");
                uninstallCertsAndKeys();
                return false;
            }
        }
        if (this.mConfig.getCredential().getClientCertificateChain() != null) {
            X509Certificate clientCert = getClientCertificate(this.mConfig.getCredential().getClientCertificateChain(), this.mConfig.getCredential().getCertCredential().getCertSha256Fingerprint());
            if (clientCert == null) {
                Log.e(TAG, "Failed to locate client certificate");
                uninstallCertsAndKeys();
                return false;
            }
            String certName2 = new StringBuilder();
            certName2.append("USRCERT_HS2_");
            certName2.append(this.mProviderId);
            if (this.mKeyStore.putCertInKeyStore(certName2.toString(), clientCert)) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(ALIAS_HS_TYPE);
                stringBuilder2.append(this.mProviderId);
                this.mClientCertificateAlias = stringBuilder2.toString();
            } else {
                Log.e(TAG, "Failed to install client certificate");
                uninstallCertsAndKeys();
                return false;
            }
        }
        this.mConfig.getCredential().setCaCertificate(null);
        this.mConfig.getCredential().setClientPrivateKey(null);
        this.mConfig.getCredential().setClientCertificateChain(null);
        return true;
    }

    public void uninstallCertsAndKeys() {
        WifiKeyStore wifiKeyStore;
        StringBuilder stringBuilder;
        String str;
        if (this.mCaCertificateAlias != null) {
            wifiKeyStore = this.mKeyStore;
            stringBuilder = new StringBuilder();
            stringBuilder.append("CACERT_");
            stringBuilder.append(this.mCaCertificateAlias);
            if (!wifiKeyStore.removeEntryFromKeyStore(stringBuilder.toString())) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to remove entry: ");
                stringBuilder.append(this.mCaCertificateAlias);
                Log.e(str, stringBuilder.toString());
            }
            this.mCaCertificateAlias = null;
        }
        if (this.mClientPrivateKeyAlias != null) {
            wifiKeyStore = this.mKeyStore;
            stringBuilder = new StringBuilder();
            stringBuilder.append("USRPKEY_");
            stringBuilder.append(this.mClientPrivateKeyAlias);
            if (!wifiKeyStore.removeEntryFromKeyStore(stringBuilder.toString())) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to remove entry: ");
                stringBuilder.append(this.mClientPrivateKeyAlias);
                Log.e(str, stringBuilder.toString());
            }
            this.mClientPrivateKeyAlias = null;
        }
        if (this.mClientCertificateAlias != null) {
            wifiKeyStore = this.mKeyStore;
            stringBuilder = new StringBuilder();
            stringBuilder.append("USRCERT_");
            stringBuilder.append(this.mClientCertificateAlias);
            if (!wifiKeyStore.removeEntryFromKeyStore(stringBuilder.toString())) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to remove entry: ");
                stringBuilder.append(this.mClientCertificateAlias);
                Log.e(str, stringBuilder.toString());
            }
            this.mClientCertificateAlias = null;
        }
    }

    public PasspointMatch match(Map<ANQPElementType, ANQPElement> anqpElements, RoamingConsortium roamingConsortium) {
        PasspointMatch providerMatch = matchProvider(anqpElements, roamingConsortium);
        int authMatch = ANQPMatcher.matchNAIRealm((NAIRealmElement) anqpElements.get(ANQPElementType.ANQPNAIRealm), this.mConfig.getCredential().getRealm(), this.mEAPMethodID, this.mAuthParam);
        if (authMatch == -1) {
            return PasspointMatch.None;
        }
        if ((authMatch & 4) == 0) {
            return providerMatch;
        }
        return providerMatch == PasspointMatch.None ? PasspointMatch.RoamingProvider : providerMatch;
    }

    public WifiConfiguration getWifiConfig() {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.FQDN = this.mConfig.getHomeSp().getFqdn();
        if (this.mConfig.getHomeSp().getRoamingConsortiumOis() != null) {
            wifiConfig.roamingConsortiumIds = Arrays.copyOf(this.mConfig.getHomeSp().getRoamingConsortiumOis(), this.mConfig.getHomeSp().getRoamingConsortiumOis().length);
        }
        wifiConfig.providerFriendlyName = this.mConfig.getHomeSp().getFriendlyName();
        wifiConfig.allowedKeyManagement.set(2);
        wifiConfig.allowedKeyManagement.set(3);
        WifiEnterpriseConfig enterpriseConfig = new WifiEnterpriseConfig();
        enterpriseConfig.setRealm(this.mConfig.getCredential().getRealm());
        enterpriseConfig.setDomainSuffixMatch(this.mConfig.getHomeSp().getFqdn());
        if (this.mConfig.getCredential().getUserCredential() != null) {
            buildEnterpriseConfigForUserCredential(enterpriseConfig, this.mConfig.getCredential().getUserCredential());
            setAnonymousIdentityToNaiRealm(enterpriseConfig, this.mConfig.getCredential().getRealm());
        } else if (this.mConfig.getCredential().getCertCredential() != null) {
            buildEnterpriseConfigForCertCredential(enterpriseConfig);
            setAnonymousIdentityToNaiRealm(enterpriseConfig, this.mConfig.getCredential().getRealm());
        } else {
            buildEnterpriseConfigForSimCredential(enterpriseConfig, this.mConfig.getCredential().getSimCredential());
        }
        wifiConfig.enterpriseConfig = enterpriseConfig;
        wifiConfig.shared = this.mIsShared;
        return wifiConfig;
    }

    public boolean isSimCredential() {
        return this.mConfig.getCredential().getSimCredential() != null;
    }

    public static PasspointConfiguration convertFromWifiConfig(WifiConfiguration wifiConfig) {
        PasspointConfiguration passpointConfig = new PasspointConfiguration();
        HomeSp homeSp = new HomeSp();
        if (TextUtils.isEmpty(wifiConfig.FQDN)) {
            Log.e(TAG, "Missing FQDN");
            return null;
        }
        homeSp.setFqdn(wifiConfig.FQDN);
        homeSp.setFriendlyName(wifiConfig.providerFriendlyName);
        if (wifiConfig.roamingConsortiumIds != null) {
            homeSp.setRoamingConsortiumOis(Arrays.copyOf(wifiConfig.roamingConsortiumIds, wifiConfig.roamingConsortiumIds.length));
        }
        passpointConfig.setHomeSp(homeSp);
        Credential credential = new Credential();
        credential.setRealm(wifiConfig.enterpriseConfig.getRealm());
        switch (wifiConfig.enterpriseConfig.getEapMethod()) {
            case 1:
                CertificateCredential certCred = new CertificateCredential();
                certCred.setCertType("x509v3");
                credential.setCertCredential(certCred);
                break;
            case 2:
                credential.setUserCredential(buildUserCredentialFromEnterpriseConfig(wifiConfig.enterpriseConfig));
                break;
            case 4:
                credential.setSimCredential(buildSimCredentialFromEnterpriseConfig(18, wifiConfig.enterpriseConfig));
                break;
            case 5:
                credential.setSimCredential(buildSimCredentialFromEnterpriseConfig(23, wifiConfig.enterpriseConfig));
                break;
            case 6:
                credential.setSimCredential(buildSimCredentialFromEnterpriseConfig(50, wifiConfig.enterpriseConfig));
                break;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupport EAP method: ");
                stringBuilder.append(wifiConfig.enterpriseConfig.getEapMethod());
                Log.e(str, stringBuilder.toString());
                return null;
        }
        if (credential.getUserCredential() == null && credential.getCertCredential() == null && credential.getSimCredential() == null) {
            Log.e(TAG, "Missing credential");
            return null;
        }
        passpointConfig.setCredential(credential);
        return passpointConfig;
    }

    public boolean equals(Object thatObject) {
        boolean z = true;
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof PasspointProvider)) {
            return false;
        }
        PasspointProvider that = (PasspointProvider) thatObject;
        if (!(this.mProviderId == that.mProviderId && TextUtils.equals(this.mCaCertificateAlias, that.mCaCertificateAlias) && TextUtils.equals(this.mClientCertificateAlias, that.mClientCertificateAlias) && TextUtils.equals(this.mClientPrivateKeyAlias, that.mClientPrivateKeyAlias) && (!this.mConfig != null ? that.mConfig != null : !this.mConfig.equals(that.mConfig)))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{Long.valueOf(this.mProviderId), this.mCaCertificateAlias, this.mClientCertificateAlias, this.mClientPrivateKeyAlias, this.mConfig});
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ProviderId: ");
        builder.append(this.mProviderId);
        builder.append("\n");
        builder.append("CreatorUID: ");
        builder.append(this.mCreatorUid);
        builder.append("\n");
        builder.append("Configuration Begin ---\n");
        builder.append(this.mConfig);
        builder.append("Configuration End ---\n");
        return builder.toString();
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x0027 A:{Splitter: B:3:0x0004, ExcHandler: java.security.cert.CertificateEncodingException (e java.security.cert.CertificateEncodingException)} */
    /* JADX WARNING: Missing block: B:12:0x0028, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static X509Certificate getClientCertificate(X509Certificate[] certChain, byte[] expectedSha256Fingerprint) {
        if (certChain == null) {
            return null;
        }
        try {
            MessageDigest digester = MessageDigest.getInstance("SHA-256");
            for (X509Certificate certificate : certChain) {
                digester.reset();
                if (Arrays.equals(expectedSha256Fingerprint, digester.digest(certificate.getEncoded()))) {
                    return certificate;
                }
            }
            return null;
        } catch (CertificateEncodingException e) {
        }
    }

    private PasspointMatch matchProvider(Map<ANQPElementType, ANQPElement> anqpElements, RoamingConsortium roamingConsortium) {
        if (ANQPMatcher.matchDomainName((DomainNameElement) anqpElements.get(ANQPElementType.ANQPDomName), this.mConfig.getHomeSp().getFqdn(), this.mImsiParameter, this.mMatchingSIMImsiList)) {
            return PasspointMatch.HomeProvider;
        }
        long[] providerOIs = this.mConfig.getHomeSp().getRoamingConsortiumOis();
        if (ANQPMatcher.matchRoamingConsortium((RoamingConsortiumElement) anqpElements.get(ANQPElementType.ANQPRoamingConsortium), providerOIs)) {
            return PasspointMatch.RoamingProvider;
        }
        long[] roamingConsortiums = roamingConsortium.getRoamingConsortiums();
        if (!(roamingConsortiums == null || providerOIs == null)) {
            for (long sta_oi : roamingConsortiums) {
                for (long ap_oi : providerOIs) {
                    if (sta_oi == ap_oi) {
                        return PasspointMatch.RoamingProvider;
                    }
                }
            }
        }
        if (ANQPMatcher.matchThreeGPPNetwork((ThreeGPPNetworkElement) anqpElements.get(ANQPElementType.ANQP3GPPNetwork), this.mImsiParameter, this.mMatchingSIMImsiList)) {
            return PasspointMatch.RoamingProvider;
        }
        return PasspointMatch.None;
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x007c  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x007a  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0078  */
    /* JADX WARNING: Removed duplicated region for block: B:16:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x007c  */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x007a  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0078  */
    /* JADX WARNING: Missing block: B:13:0x0056, code:
            if (r5.equals("PAP") != false) goto L_0x005a;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void buildEnterpriseConfigForUserCredential(WifiEnterpriseConfig config, UserCredential credential) {
        int i = 0;
        String decodedPassword = new String(Base64.decode(credential.getPassword(), 0), StandardCharsets.UTF_8);
        config.setEapMethod(2);
        config.setIdentity(credential.getUsername());
        config.setPassword(decodedPassword);
        config.setCaCertificateAlias(this.mCaCertificateAlias);
        int phase2Method = 0;
        String nonEapInnerMethod = credential.getNonEapInnerMethod();
        int hashCode = nonEapInnerMethod.hashCode();
        if (hashCode != 78975) {
            if (hashCode != 632512142) {
                if (hashCode == 2038151963 && nonEapInnerMethod.equals("MS-CHAP")) {
                    i = 1;
                    switch (i) {
                        case 0:
                            phase2Method = 1;
                            break;
                        case 1:
                            phase2Method = 2;
                            break;
                        case 2:
                            phase2Method = 3;
                            break;
                        default:
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unsupported Auth: ");
                            stringBuilder.append(credential.getNonEapInnerMethod());
                            Log.wtf(str, stringBuilder.toString());
                            break;
                    }
                    config.setPhase2Method(phase2Method);
                }
            } else if (nonEapInnerMethod.equals("MS-CHAP-V2")) {
                i = 2;
                switch (i) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    default:
                        break;
                }
                config.setPhase2Method(phase2Method);
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
            default:
                break;
        }
        config.setPhase2Method(phase2Method);
    }

    private void buildEnterpriseConfigForCertCredential(WifiEnterpriseConfig config) {
        config.setEapMethod(1);
        config.setClientCertificateAlias(this.mClientCertificateAlias);
        config.setCaCertificateAlias(this.mCaCertificateAlias);
    }

    private void buildEnterpriseConfigForSimCredential(WifiEnterpriseConfig config, SimCredential credential) {
        int eapMethod = -1;
        int eapType = credential.getEapType();
        if (eapType == 18) {
            eapMethod = 4;
        } else if (eapType == 23) {
            eapMethod = 5;
        } else if (eapType != 50) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported EAP Method: ");
            stringBuilder.append(credential.getEapType());
            Log.wtf(str, stringBuilder.toString());
        } else {
            eapMethod = 6;
        }
        config.setEapMethod(eapMethod);
        config.setPlmn(credential.getImsi());
    }

    private static void setAnonymousIdentityToNaiRealm(WifiEnterpriseConfig config, String realm) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("anonymous@");
        stringBuilder.append(realm);
        config.setAnonymousIdentity(stringBuilder.toString());
    }

    private static UserCredential buildUserCredentialFromEnterpriseConfig(WifiEnterpriseConfig config) {
        UserCredential userCredential = new UserCredential();
        userCredential.setEapType(21);
        if (TextUtils.isEmpty(config.getIdentity())) {
            Log.e(TAG, "Missing username for user credential");
            return null;
        }
        userCredential.setUsername(config.getIdentity());
        if (TextUtils.isEmpty(config.getPassword())) {
            Log.e(TAG, "Missing password for user credential");
            return null;
        }
        userCredential.setPassword(new String(Base64.encode(config.getPassword().getBytes(StandardCharsets.UTF_8), 0), StandardCharsets.UTF_8));
        switch (config.getPhase2Method()) {
            case 1:
                userCredential.setNonEapInnerMethod("PAP");
                break;
            case 2:
                userCredential.setNonEapInnerMethod("MS-CHAP");
                break;
            case 3:
                userCredential.setNonEapInnerMethod("MS-CHAP-V2");
                break;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported phase2 method for TTLS: ");
                stringBuilder.append(config.getPhase2Method());
                Log.e(str, stringBuilder.toString());
                return null;
        }
        return userCredential;
    }

    private static SimCredential buildSimCredentialFromEnterpriseConfig(int eapType, WifiEnterpriseConfig config) {
        SimCredential simCredential = new SimCredential();
        if (TextUtils.isEmpty(config.getPlmn())) {
            Log.e(TAG, "Missing IMSI for SIM credential");
            return null;
        }
        simCredential.setImsi(config.getPlmn());
        simCredential.setEapType(eapType);
        return simCredential;
    }
}
