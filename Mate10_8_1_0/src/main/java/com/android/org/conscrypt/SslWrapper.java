package com.android.org.conscrypt;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.X509KeyManager;
import javax.security.auth.x500.X500Principal;

final class SslWrapper {
    private final AliasChooser aliasChooser;
    private final SSLHandshakeCallbacks handshakeCallbacks;
    private final SSLParametersImpl parameters;
    private final PSKCallbacks pskCallbacks;
    private long ssl;

    final class BioWrapper {
        private long bio;

        private BioWrapper() throws SSLException {
            this.bio = NativeCrypto.SSL_BIO_new(SslWrapper.this.ssl);
        }

        int getPendingWrittenBytes() {
            return NativeCrypto.SSL_pending_written_bytes_in_BIO(this.bio);
        }

        int writeDirectByteBuffer(long address, int length) throws IOException {
            return NativeCrypto.ENGINE_SSL_write_BIO_direct(SslWrapper.this.ssl, this.bio, address, length, SslWrapper.this.handshakeCallbacks);
        }

        int writeArray(byte[] sourceJava, int sourceOffset, int sourceLength) throws IOException {
            return NativeCrypto.ENGINE_SSL_write_BIO_heap(SslWrapper.this.ssl, this.bio, sourceJava, sourceOffset, sourceLength, SslWrapper.this.handshakeCallbacks);
        }

        int readDirectByteBuffer(long destAddress, int destLength) throws IOException {
            return NativeCrypto.ENGINE_SSL_read_BIO_direct(SslWrapper.this.ssl, this.bio, destAddress, destLength, SslWrapper.this.handshakeCallbacks);
        }

        int readArray(byte[] destJava, int destOffset, int destLength) throws IOException {
            return NativeCrypto.ENGINE_SSL_read_BIO_heap(SslWrapper.this.ssl, this.bio, destJava, destOffset, destLength, SslWrapper.this.handshakeCallbacks);
        }

        void close() {
            NativeCrypto.BIO_free_all(this.bio);
            this.bio = 0;
        }
    }

    static SslWrapper newInstance(SSLParametersImpl parameters, SSLHandshakeCallbacks handshakeCallbacks, AliasChooser chooser, PSKCallbacks pskCallbacks) throws SSLException {
        return new SslWrapper(NativeCrypto.SSL_new(parameters.getSessionContext().sslCtxNativePointer), parameters, handshakeCallbacks, chooser, pskCallbacks);
    }

    private SslWrapper(long ssl, SSLParametersImpl parameters, SSLHandshakeCallbacks handshakeCallbacks, AliasChooser aliasChooser, PSKCallbacks pskCallbacks) {
        this.ssl = ssl;
        this.parameters = parameters;
        this.handshakeCallbacks = handshakeCallbacks;
        this.aliasChooser = aliasChooser;
        this.pskCallbacks = pskCallbacks;
    }

    long ssl() {
        return this.ssl;
    }

    BioWrapper newBio() {
        try {
            return new BioWrapper();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    void offerToResumeSession(long sslSessionNativePointer) throws SSLException {
        NativeCrypto.SSL_set_session(this.ssl, sslSessionNativePointer);
    }

    byte[] getSessionId() {
        return NativeCrypto.SSL_session_id(this.ssl);
    }

    long getTime() {
        return NativeCrypto.SSL_get_time(this.ssl);
    }

    long getTimeout() {
        return NativeCrypto.SSL_get_timeout(this.ssl);
    }

    void setTimeout(long millis) {
        NativeCrypto.SSL_set_timeout(this.ssl, millis);
    }

    String getCipherSuite() {
        return NativeCrypto.cipherSuiteToJava(NativeCrypto.SSL_get_current_cipher(this.ssl));
    }

    OpenSSLX509Certificate[] getLocalCertificates() {
        return OpenSSLX509Certificate.createCertChain(NativeCrypto.SSL_get_certificate(this.ssl));
    }

    OpenSSLX509Certificate[] getPeerCertificates() {
        return OpenSSLX509Certificate.createCertChain(NativeCrypto.SSL_get_peer_cert_chain(this.ssl));
    }

    byte[] getPeerCertificateOcspData() {
        return NativeCrypto.SSL_get_ocsp_response(this.ssl);
    }

    byte[] getPeerTlsSctData() {
        return NativeCrypto.SSL_get_signed_cert_timestamp_list(this.ssl);
    }

    int clientPSKKeyRequested(String identityHint, byte[] identityBytesOut, byte[] key) {
        PSKKeyManager pskKeyManager = this.parameters.getPSKKeyManager();
        if (pskKeyManager == null) {
            return 0;
        }
        byte[] identityBytes;
        String identity = this.pskCallbacks.chooseClientPSKIdentity(pskKeyManager, identityHint);
        if (identity == null) {
            identity = "";
            identityBytes = EmptyArray.BYTE;
        } else if (identity.isEmpty()) {
            identityBytes = EmptyArray.BYTE;
        } else {
            try {
                identityBytes = identity.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 encoding not supported", e);
            }
        }
        if (identityBytes.length + 1 > identityBytesOut.length) {
            return 0;
        }
        if (identityBytes.length > 0) {
            System.arraycopy(identityBytes, 0, identityBytesOut, 0, identityBytes.length);
        }
        identityBytesOut[identityBytes.length] = (byte) 0;
        byte[] secretKeyBytes = this.pskCallbacks.getPSKKey(pskKeyManager, identityHint, identity).getEncoded();
        if (secretKeyBytes == null || secretKeyBytes.length > key.length) {
            return 0;
        }
        System.arraycopy(secretKeyBytes, 0, key, 0, secretKeyBytes.length);
        return secretKeyBytes.length;
    }

    int serverPSKKeyRequested(String identityHint, String identity, byte[] key) {
        PSKKeyManager pskKeyManager = this.parameters.getPSKKeyManager();
        if (pskKeyManager == null) {
            return 0;
        }
        byte[] secretKeyBytes = this.pskCallbacks.getPSKKey(pskKeyManager, identityHint, identity).getEncoded();
        if (secretKeyBytes == null || secretKeyBytes.length > key.length) {
            return 0;
        }
        System.arraycopy(secretKeyBytes, 0, key, 0, secretKeyBytes.length);
        return secretKeyBytes.length;
    }

    void chooseClientCertificate(byte[] keyTypeBytes, byte[][] asn1DerEncodedPrincipals) throws SSLException, CertificateEncodingException {
        X500Principal[] x500PrincipalArr;
        String chooseClientAlias;
        Set<String> keyTypesSet = SSLUtils.getSupportedClientKeyTypes(keyTypeBytes);
        String[] keyTypes = (String[]) keyTypesSet.toArray(new String[keyTypesSet.size()]);
        if (asn1DerEncodedPrincipals == null) {
            x500PrincipalArr = null;
        } else {
            x500PrincipalArr = new X500Principal[asn1DerEncodedPrincipals.length];
            for (int i = 0; i < asn1DerEncodedPrincipals.length; i++) {
                x500PrincipalArr[i] = new X500Principal(asn1DerEncodedPrincipals[i]);
            }
        }
        X509KeyManager keyManager = this.parameters.getX509KeyManager();
        if (keyManager != null) {
            chooseClientAlias = this.aliasChooser.chooseClientAlias(keyManager, x500PrincipalArr, keyTypes);
        } else {
            chooseClientAlias = null;
        }
        setCertificate(chooseClientAlias);
    }

    void setCertificate(String alias) throws CertificateEncodingException, SSLException {
        if (alias != null) {
            X509KeyManager keyManager = this.parameters.getX509KeyManager();
            if (keyManager != null) {
                PrivateKey privateKey = keyManager.getPrivateKey(alias);
                if (privateKey != null) {
                    X509Certificate[] certificates = keyManager.getCertificateChain(alias);
                    if (certificates != null) {
                        PublicKey publicKey = certificates.length > 0 ? certificates[0].getPublicKey() : null;
                        OpenSSLX509Certificate[] openSslCerts = new OpenSSLX509Certificate[certificates.length];
                        long[] x509refs = new long[certificates.length];
                        for (int i = 0; i < certificates.length; i++) {
                            OpenSSLX509Certificate openSslCert = OpenSSLX509Certificate.fromCertificate(certificates[i]);
                            openSslCerts[i] = openSslCert;
                            x509refs[i] = openSslCert.getContext();
                        }
                        NativeCrypto.SSL_use_certificate(this.ssl, x509refs);
                        try {
                            OpenSSLKey key = OpenSSLKey.fromPrivateKeyForTLSStackOnly(privateKey, publicKey);
                            NativeCrypto.SSL_use_PrivateKey(this.ssl, key.getNativeRef());
                            if (!key.isWrapped()) {
                                NativeCrypto.SSL_check_private_key(this.ssl);
                            }
                        } catch (InvalidKeyException e) {
                            throw new SSLException(e);
                        }
                    }
                }
            }
        }
    }

    String getVersion() {
        return NativeCrypto.SSL_get_version(this.ssl);
    }

    boolean isReused() {
        return NativeCrypto.SSL_session_reused(this.ssl);
    }

    String getRequestedServerName() {
        return NativeCrypto.SSL_get_servername(this.ssl);
    }

    byte[] getTlsChannelId() throws SSLException {
        return NativeCrypto.SSL_get_tls_channel_id(this.ssl);
    }

    void initialize(String hostname, OpenSSLKey channelIdPrivateKey) throws IOException {
        if (!this.parameters.getEnableSessionCreation()) {
            NativeCrypto.SSL_set_session_creation_enabled(this.ssl, false);
        }
        NativeCrypto.SSL_accept_renegotiations(this.ssl);
        if (isClient()) {
            NativeCrypto.SSL_set_connect_state(this.ssl);
            NativeCrypto.SSL_enable_ocsp_stapling(this.ssl);
            if (this.parameters.isCTVerificationEnabled(hostname)) {
                NativeCrypto.SSL_enable_signed_cert_timestamps(this.ssl);
            }
        } else {
            NativeCrypto.SSL_set_accept_state(this.ssl);
            if (this.parameters.getOCSPResponse() != null) {
                NativeCrypto.SSL_enable_ocsp_stapling(this.ssl);
            }
        }
        if (this.parameters.getEnabledProtocols().length == 0 && this.parameters.isEnabledProtocolsFiltered) {
            throw new SSLHandshakeException("No enabled protocols; SSLv3 is no longer supported and was filtered from the list");
        }
        NativeCrypto.SSL_configure_alpn(this.ssl, isClient(), this.parameters.alpnProtocols);
        NativeCrypto.setEnabledProtocols(this.ssl, this.parameters.enabledProtocols);
        NativeCrypto.setEnabledCipherSuites(this.ssl, this.parameters.enabledCipherSuites);
        if (!isClient()) {
            String keyType;
            Set<String> keyTypes = new HashSet();
            for (long sslCipherNativePointer : NativeCrypto.SSL_get_ciphers(this.ssl)) {
                keyType = SSLUtils.getServerX509KeyType(sslCipherNativePointer);
                if (keyType != null) {
                    keyTypes.add(keyType);
                }
            }
            X509KeyManager keyManager = this.parameters.getX509KeyManager();
            if (keyManager != null) {
                for (String keyType2 : keyTypes) {
                    try {
                        setCertificate(this.aliasChooser.chooseServerAlias(keyManager, keyType2));
                    } catch (CertificateEncodingException e) {
                        throw new IOException(e);
                    }
                }
            }
            NativeCrypto.SSL_set_options(this.ssl, 4194304);
            if (this.parameters.sctExtension != null) {
                NativeCrypto.SSL_set_signed_cert_timestamp_list(this.ssl, this.parameters.sctExtension);
            }
            if (this.parameters.ocspResponse != null) {
                NativeCrypto.SSL_set_ocsp_response(this.ssl, this.parameters.ocspResponse);
            }
        }
        enablePSKKeyManagerIfRequested();
        if (this.parameters.useSessionTickets) {
            NativeCrypto.SSL_clear_options(this.ssl, 16384);
        } else {
            NativeCrypto.SSL_set_options(this.ssl, NativeCrypto.SSL_get_options(this.ssl) | 16384);
        }
        if (this.parameters.getUseSni() && AddressUtils.isValidSniHostname(hostname)) {
            NativeCrypto.SSL_set_tlsext_host_name(this.ssl, hostname);
        }
        NativeCrypto.SSL_set_mode(this.ssl, 256);
        setCertificateValidation(this.ssl);
        setTlsChannelId(channelIdPrivateKey);
    }

    void doHandshake(FileDescriptor fd, int timeoutMillis) throws CertificateException, SocketTimeoutException, SSLException {
        NativeCrypto.SSL_do_handshake(this.ssl, fd, this.handshakeCallbacks, timeoutMillis);
    }

    int doHandshake() throws IOException {
        return NativeCrypto.ENGINE_SSL_do_handshake(this.ssl, this.handshakeCallbacks);
    }

    int read(FileDescriptor fd, byte[] buf, int offset, int len, int timeoutMillis) throws IOException {
        return NativeCrypto.SSL_read(this.ssl, fd, this.handshakeCallbacks, buf, offset, len, timeoutMillis);
    }

    void write(FileDescriptor fd, byte[] buf, int offset, int len, int timeoutMillis) throws IOException {
        NativeCrypto.SSL_write(this.ssl, fd, this.handshakeCallbacks, buf, offset, len, timeoutMillis);
    }

    private void enablePSKKeyManagerIfRequested() throws SSLException {
        PSKKeyManager pskKeyManager = this.parameters.getPSKKeyManager();
        if (pskKeyManager != null) {
            boolean pskEnabled = false;
            for (String enabledCipherSuite : this.parameters.enabledCipherSuites) {
                if (enabledCipherSuite != null && enabledCipherSuite.contains("PSK")) {
                    pskEnabled = true;
                    break;
                }
            }
            if (!pskEnabled) {
                return;
            }
            if (isClient()) {
                NativeCrypto.set_SSL_psk_client_callback_enabled(this.ssl, true);
                return;
            }
            NativeCrypto.set_SSL_psk_server_callback_enabled(this.ssl, true);
            NativeCrypto.SSL_use_psk_identity_hint(this.ssl, this.pskCallbacks.chooseServerPSKIdentityHint(pskKeyManager));
        }
    }

    private void setTlsChannelId(OpenSSLKey channelIdPrivateKey) throws SSLException {
        if (this.parameters.channelIdEnabled) {
            if (!this.parameters.getUseClientMode()) {
                NativeCrypto.SSL_enable_tls_channel_id(this.ssl);
            } else if (channelIdPrivateKey == null) {
                throw new SSLHandshakeException("Invalid TLS channel ID key specified");
            } else {
                NativeCrypto.SSL_set1_tls_channel_id(this.ssl, channelIdPrivateKey.getNativeRef());
            }
        }
    }

    private void setCertificateValidation(long sslNativePointer) throws SSLException {
        if (!isClient()) {
            boolean certRequested;
            if (this.parameters.getNeedClientAuth()) {
                NativeCrypto.SSL_set_verify(sslNativePointer, 3);
                certRequested = true;
            } else if (this.parameters.getWantClientAuth()) {
                NativeCrypto.SSL_set_verify(sslNativePointer, 1);
                certRequested = true;
            } else {
                NativeCrypto.SSL_set_verify(sslNativePointer, 0);
                certRequested = false;
            }
            if (certRequested) {
                X509Certificate[] issuers = this.parameters.getX509TrustManager().getAcceptedIssuers();
                if (issuers != null && issuers.length != 0) {
                    try {
                        NativeCrypto.SSL_set_client_CA_list(sslNativePointer, SSLUtils.encodeIssuerX509Principals(issuers));
                    } catch (CertificateEncodingException e) {
                        throw new SSLException("Problem encoding principals", e);
                    }
                }
            }
        }
    }

    void interrupt() {
        NativeCrypto.SSL_interrupt(this.ssl);
    }

    void shutdown(FileDescriptor fd) throws IOException {
        NativeCrypto.SSL_shutdown(this.ssl, fd, this.handshakeCallbacks);
    }

    void shutdown() throws IOException {
        NativeCrypto.ENGINE_SSL_shutdown(this.ssl, this.handshakeCallbacks);
    }

    boolean wasShutdownReceived() {
        return (NativeCrypto.SSL_get_shutdown(this.ssl) & 2) != 0;
    }

    boolean wasShutdownSent() {
        return (NativeCrypto.SSL_get_shutdown(this.ssl) & 1) != 0;
    }

    int readDirectByteBuffer(long destAddress, int destLength) throws IOException, CertificateException {
        return NativeCrypto.ENGINE_SSL_read_direct(this.ssl, destAddress, destLength, this.handshakeCallbacks);
    }

    int readArray(byte[] destJava, int destOffset, int destLength) throws IOException, CertificateException {
        return NativeCrypto.ENGINE_SSL_read_heap(this.ssl, destJava, destOffset, destLength, this.handshakeCallbacks);
    }

    int writeDirectByteBuffer(long sourceAddress, int sourceLength) throws IOException {
        return NativeCrypto.ENGINE_SSL_write_direct(this.ssl, sourceAddress, sourceLength, this.handshakeCallbacks);
    }

    int writeArray(byte[] sourceJava, int sourceOffset, int sourceLength) throws IOException {
        return NativeCrypto.ENGINE_SSL_write_heap(this.ssl, sourceJava, sourceOffset, sourceLength, this.handshakeCallbacks);
    }

    int getPendingReadableBytes() {
        return NativeCrypto.SSL_pending_readable_bytes(this.ssl);
    }

    int getMaxSealOverhead() {
        return NativeCrypto.SSL_max_seal_overhead(this.ssl);
    }

    void close() {
        NativeCrypto.SSL_free(this.ssl);
        this.ssl = 0;
    }

    boolean isClosed() {
        return this.ssl == 0;
    }

    int getError(int result) {
        return NativeCrypto.SSL_get_error(this.ssl, result);
    }

    byte[] getAlpnSelectedProtocol() {
        return NativeCrypto.SSL_get0_alpn_selected(this.ssl);
    }

    private boolean isClient() {
        return this.parameters.getUseClientMode();
    }
}
