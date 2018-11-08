package com.android.org.conscrypt;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

final class SSLParametersImpl implements Cloneable {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static volatile SSLParametersImpl defaultParameters;
    private static volatile X509KeyManager defaultX509KeyManager;
    private static volatile X509TrustManager defaultX509TrustManager;
    byte[] alpnProtocols;
    boolean channelIdEnabled;
    private final ClientSessionContext clientSessionContext;
    private boolean client_mode = true;
    private boolean ctVerificationEnabled;
    private boolean enable_session_creation = true;
    String[] enabledCipherSuites;
    String[] enabledProtocols;
    private String endpointIdentificationAlgorithm;
    boolean isEnabledProtocolsFiltered;
    private boolean need_client_auth = false;
    byte[] ocspResponse;
    private final PSKKeyManager pskKeyManager;
    byte[] sctExtension;
    private SecureRandom secureRandom;
    private final ServerSessionContext serverSessionContext;
    private boolean useCipherSuitesOrder;
    boolean useSessionTickets;
    private Boolean useSni;
    private boolean want_client_auth = false;
    private final X509KeyManager x509KeyManager;
    private final X509TrustManager x509TrustManager;

    interface AliasChooser {
        String chooseClientAlias(X509KeyManager x509KeyManager, X500Principal[] x500PrincipalArr, String[] strArr);

        String chooseServerAlias(X509KeyManager x509KeyManager, String str);
    }

    interface PSKCallbacks {
        String chooseClientPSKIdentity(PSKKeyManager pSKKeyManager, String str);

        String chooseServerPSKIdentityHint(PSKKeyManager pSKKeyManager);

        SecretKey getPSKKey(PSKKeyManager pSKKeyManager, String str, String str2);
    }

    SSLParametersImpl(KeyManager[] kms, TrustManager[] tms, SecureRandom sr, ClientSessionContext clientSessionContext, ServerSessionContext serverSessionContext, String[] protocols) throws KeyManagementException {
        this.serverSessionContext = serverSessionContext;
        this.clientSessionContext = clientSessionContext;
        if (kms == null) {
            this.x509KeyManager = getDefaultX509KeyManager();
            this.pskKeyManager = null;
        } else {
            this.x509KeyManager = findFirstX509KeyManager(kms);
            this.pskKeyManager = findFirstPSKKeyManager(kms);
        }
        if (tms == null) {
            this.x509TrustManager = getDefaultX509TrustManager();
        } else {
            this.x509TrustManager = findFirstX509TrustManager(tms);
        }
        this.secureRandom = sr;
        if (protocols == null) {
            protocols = NativeCrypto.DEFAULT_PROTOCOLS;
        }
        this.enabledProtocols = (String[]) NativeCrypto.checkEnabledProtocols(protocols).clone();
        boolean x509CipherSuitesNeeded = (this.x509KeyManager == null && this.x509TrustManager == null) ? false : true;
        this.enabledCipherSuites = getDefaultCipherSuites(x509CipherSuitesNeeded, this.pskKeyManager != null);
    }

    static SSLParametersImpl getDefault() throws KeyManagementException {
        SSLParametersImpl result = defaultParameters;
        if (result == null) {
            result = new SSLParametersImpl(null, null, null, new ClientSessionContext(), new ServerSessionContext(), null);
            defaultParameters = result;
        }
        return (SSLParametersImpl) result.clone();
    }

    AbstractSessionContext getSessionContext() {
        return this.client_mode ? this.clientSessionContext : this.serverSessionContext;
    }

    ClientSessionContext getClientSessionContext() {
        return this.clientSessionContext;
    }

    X509KeyManager getX509KeyManager() {
        return this.x509KeyManager;
    }

    PSKKeyManager getPSKKeyManager() {
        return this.pskKeyManager;
    }

    X509TrustManager getX509TrustManager() {
        return this.x509TrustManager;
    }

    String[] getEnabledCipherSuites() {
        return (String[]) this.enabledCipherSuites.clone();
    }

    void setEnabledCipherSuites(String[] cipherSuites) {
        this.enabledCipherSuites = (String[]) NativeCrypto.checkEnabledCipherSuites(cipherSuites).clone();
    }

    String[] getEnabledProtocols() {
        return (String[]) this.enabledProtocols.clone();
    }

    void setEnabledProtocols(String[] protocols) {
        if (protocols == null) {
            throw new IllegalArgumentException("protocols == null");
        }
        String[] filteredProtocols = filterFromProtocols(protocols, "SSLv3");
        this.isEnabledProtocolsFiltered = protocols.length != filteredProtocols.length;
        this.enabledProtocols = (String[]) NativeCrypto.checkEnabledProtocols(filteredProtocols).clone();
    }

    void setAlpnProtocols(String[] alpnProtocols) {
        setAlpnProtocols(SSLUtils.toLengthPrefixedList(alpnProtocols));
    }

    void setAlpnProtocols(byte[] alpnProtocols) {
        if (alpnProtocols == null || alpnProtocols.length != 0) {
            this.alpnProtocols = alpnProtocols;
            return;
        }
        throw new IllegalArgumentException("alpnProtocols.length == 0");
    }

    byte[] getAlpnProtocols() {
        return this.alpnProtocols;
    }

    void setUseClientMode(boolean mode) {
        this.client_mode = mode;
    }

    boolean getUseClientMode() {
        return this.client_mode;
    }

    void setNeedClientAuth(boolean need) {
        this.need_client_auth = need;
        this.want_client_auth = false;
    }

    boolean getNeedClientAuth() {
        return this.need_client_auth;
    }

    void setWantClientAuth(boolean want) {
        this.want_client_auth = want;
        this.need_client_auth = false;
    }

    boolean getWantClientAuth() {
        return this.want_client_auth;
    }

    void setEnableSessionCreation(boolean flag) {
        this.enable_session_creation = flag;
    }

    boolean getEnableSessionCreation() {
        return this.enable_session_creation;
    }

    void setUseSessionTickets(boolean useSessionTickets) {
        this.useSessionTickets = useSessionTickets;
    }

    void setUseSni(boolean flag) {
        this.useSni = Boolean.valueOf(flag);
    }

    boolean getUseSni() {
        return this.useSni != null ? this.useSni.booleanValue() : isSniEnabledByDefault();
    }

    void setCTVerificationEnabled(boolean enabled) {
        this.ctVerificationEnabled = enabled;
    }

    void setSCTExtension(byte[] extension) {
        this.sctExtension = extension;
    }

    void setOCSPResponse(byte[] response) {
        this.ocspResponse = response;
    }

    byte[] getOCSPResponse() {
        return this.ocspResponse;
    }

    private static String[] filterFromProtocols(String[] protocols, String obsoleteProtocol) {
        int i = 0;
        if (protocols.length == 1 && obsoleteProtocol.equals(protocols[0])) {
            return EMPTY_STRING_ARRAY;
        }
        ArrayList<String> newProtocols = new ArrayList();
        int length = protocols.length;
        while (i < length) {
            String protocol = protocols[i];
            if (!obsoleteProtocol.equals(protocol)) {
                newProtocols.add(protocol);
            }
            i++;
        }
        return (String[]) newProtocols.toArray(EMPTY_STRING_ARRAY);
    }

    private boolean isSniEnabledByDefault() {
        try {
            String enableSNI = System.getProperty("jsse.enableSNIExtension", "true");
            if ("true".equalsIgnoreCase(enableSNI)) {
                return true;
            }
            if ("false".equalsIgnoreCase(enableSNI)) {
                return false;
            }
            throw new RuntimeException("Can only set \"jsse.enableSNIExtension\" to \"true\" or \"false\"");
        } catch (SecurityException e) {
            return true;
        }
    }

    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    private static X509KeyManager getDefaultX509KeyManager() throws KeyManagementException {
        X509KeyManager result = defaultX509KeyManager;
        if (result != null) {
            return result;
        }
        result = createDefaultX509KeyManager();
        defaultX509KeyManager = result;
        return result;
    }

    private static X509KeyManager createDefaultX509KeyManager() throws KeyManagementException {
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(null, null);
            KeyManager[] kms = kmf.getKeyManagers();
            X509KeyManager result = findFirstX509KeyManager(kms);
            if (result != null) {
                return result;
            }
            throw new KeyManagementException("No X509KeyManager among default KeyManagers: " + Arrays.toString(kms));
        } catch (NoSuchAlgorithmException e) {
            throw new KeyManagementException(e);
        } catch (KeyStoreException e2) {
            throw new KeyManagementException(e2);
        } catch (UnrecoverableKeyException e3) {
            throw new KeyManagementException(e3);
        }
    }

    private static X509KeyManager findFirstX509KeyManager(KeyManager[] kms) {
        for (KeyManager km : kms) {
            if (km instanceof X509KeyManager) {
                return (X509KeyManager) km;
            }
        }
        return null;
    }

    private static PSKKeyManager findFirstPSKKeyManager(KeyManager[] kms) {
        int i = 0;
        int length = kms.length;
        while (i < length) {
            KeyManager km = kms[i];
            if (km instanceof PSKKeyManager) {
                return (PSKKeyManager) km;
            }
            if (km != null) {
                try {
                    return DuckTypedPSKKeyManager.getInstance(km);
                } catch (NoSuchMethodException e) {
                }
            } else {
                i++;
            }
        }
        return null;
    }

    static X509TrustManager getDefaultX509TrustManager() throws KeyManagementException {
        X509TrustManager result = defaultX509TrustManager;
        if (result != null) {
            return result;
        }
        result = createDefaultX509TrustManager();
        defaultX509TrustManager = result;
        return result;
    }

    private static X509TrustManager createDefaultX509TrustManager() throws KeyManagementException {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            TrustManager[] tms = tmf.getTrustManagers();
            X509TrustManager trustManager = findFirstX509TrustManager(tms);
            if (trustManager != null) {
                return trustManager;
            }
            throw new KeyManagementException("No X509TrustManager in among default TrustManagers: " + Arrays.toString(tms));
        } catch (NoSuchAlgorithmException e) {
            throw new KeyManagementException(e);
        } catch (KeyStoreException e2) {
            throw new KeyManagementException(e2);
        }
    }

    private static X509TrustManager findFirstX509TrustManager(TrustManager[] tms) {
        for (TrustManager tm : tms) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
        }
        return null;
    }

    String getEndpointIdentificationAlgorithm() {
        return this.endpointIdentificationAlgorithm;
    }

    void setEndpointIdentificationAlgorithm(String endpointIdentificationAlgorithm) {
        this.endpointIdentificationAlgorithm = endpointIdentificationAlgorithm;
    }

    boolean getUseCipherSuitesOrder() {
        return this.useCipherSuitesOrder;
    }

    void setUseCipherSuitesOrder(boolean useCipherSuitesOrder) {
        this.useCipherSuitesOrder = useCipherSuitesOrder;
    }

    private static String[] getDefaultCipherSuites(boolean x509CipherSuitesNeeded, boolean pskCipherSuitesNeeded) {
        String[][] strArr;
        if (x509CipherSuitesNeeded) {
            if (pskCipherSuitesNeeded) {
                strArr = new String[3][];
                strArr[0] = NativeCrypto.DEFAULT_PSK_CIPHER_SUITES;
                strArr[1] = NativeCrypto.DEFAULT_X509_CIPHER_SUITES;
                strArr[2] = new String[]{"TLS_EMPTY_RENEGOTIATION_INFO_SCSV"};
                return concat(strArr);
            }
            strArr = new String[2][];
            strArr[0] = NativeCrypto.DEFAULT_X509_CIPHER_SUITES;
            strArr[1] = new String[]{"TLS_EMPTY_RENEGOTIATION_INFO_SCSV"};
            return concat(strArr);
        } else if (pskCipherSuitesNeeded) {
            strArr = new String[2][];
            strArr[0] = NativeCrypto.DEFAULT_PSK_CIPHER_SUITES;
            strArr[1] = new String[]{"TLS_EMPTY_RENEGOTIATION_INFO_SCSV"};
            return concat(strArr);
        } else {
            return new String[]{"TLS_EMPTY_RENEGOTIATION_INFO_SCSV"};
        }
    }

    private static String[] concat(String[]... arrays) {
        int resultLength = 0;
        for (String[] array : arrays) {
            resultLength += array.length;
        }
        String[] result = new String[resultLength];
        int resultOffset = 0;
        for (String[] array2 : arrays) {
            System.arraycopy(array2, 0, result, resultOffset, array2.length);
            resultOffset += array2.length;
        }
        return result;
    }

    boolean isCTVerificationEnabled(String hostname) {
        if (hostname == null) {
            return false;
        }
        if (this.ctVerificationEnabled) {
            return true;
        }
        return Platform.isCTVerificationRequired(hostname);
    }
}
