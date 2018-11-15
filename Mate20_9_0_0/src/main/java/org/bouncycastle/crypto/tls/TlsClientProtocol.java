package org.bouncycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.bouncycastle.util.Arrays;

public class TlsClientProtocol extends TlsProtocol {
    protected TlsAuthentication authentication = null;
    protected CertificateRequest certificateRequest = null;
    protected CertificateStatus certificateStatus = null;
    protected TlsKeyExchange keyExchange = null;
    protected byte[] selectedSessionID = null;
    protected TlsClient tlsClient = null;
    TlsClientContextImpl tlsClientContext = null;

    public TlsClientProtocol(InputStream inputStream, OutputStream outputStream, SecureRandom secureRandom) {
        super(inputStream, outputStream, secureRandom);
    }

    public TlsClientProtocol(SecureRandom secureRandom) {
        super(secureRandom);
    }

    protected void cleanupHandshake() {
        super.cleanupHandshake();
        this.selectedSessionID = null;
        this.keyExchange = null;
        this.authentication = null;
        this.certificateStatus = null;
        this.certificateRequest = null;
    }

    public void connect(TlsClient tlsClient) throws IOException {
        if (tlsClient == null) {
            throw new IllegalArgumentException("'tlsClient' cannot be null");
        } else if (this.tlsClient == null) {
            this.tlsClient = tlsClient;
            this.securityParameters = new SecurityParameters();
            this.securityParameters.entity = 1;
            this.tlsClientContext = new TlsClientContextImpl(this.secureRandom, this.securityParameters);
            this.securityParameters.clientRandom = TlsProtocol.createRandomBlock(tlsClient.shouldUseGMTUnixTime(), this.tlsClientContext.getNonceRandomGenerator());
            this.tlsClient.init(this.tlsClientContext);
            this.recordStream.init(this.tlsClientContext);
            TlsSession sessionToResume = tlsClient.getSessionToResume();
            if (sessionToResume != null && sessionToResume.isResumable()) {
                SessionParameters exportSessionParameters = sessionToResume.exportSessionParameters();
                if (exportSessionParameters != null) {
                    this.tlsSession = sessionToResume;
                    this.sessionParameters = exportSessionParameters;
                }
            }
            sendClientHelloMessage();
            this.connection_state = (short) 1;
            blockForHandshake();
        } else {
            throw new IllegalStateException("'connect' can only be called once");
        }
    }

    protected TlsContext getContext() {
        return this.tlsClientContext;
    }

    AbstractTlsContext getContextAdmin() {
        return this.tlsClientContext;
    }

    protected TlsPeer getPeer() {
        return this.tlsClient;
    }

    /* JADX WARNING: Missing block: B:39:0x0080, code:
            r7.keyExchange.skipServerCredentials();
            r7.authentication = null;
     */
    /* JADX WARNING: Missing block: B:40:0x0087, code:
            r7.keyExchange.skipServerKeyExchange();
     */
    /* JADX WARNING: Missing block: B:41:0x008c, code:
            org.bouncycastle.crypto.tls.TlsProtocol.assertEmpty(r9);
            r7.connection_state = (short) 8;
            r7.recordStream.getHandshakeHash().sealHashAlgorithms();
            r8 = r7.tlsClient.getClientSupplementalData();
     */
    /* JADX WARNING: Missing block: B:42:0x00a2, code:
            if (r8 == null) goto L_0x00a7;
     */
    /* JADX WARNING: Missing block: B:43:0x00a4, code:
            sendSupplementalDataMessage(r8);
     */
    /* JADX WARNING: Missing block: B:44:0x00a7, code:
            r7.connection_state = (short) 9;
     */
    /* JADX WARNING: Missing block: B:45:0x00ad, code:
            if (r7.certificateRequest != null) goto L_0x00b6;
     */
    /* JADX WARNING: Missing block: B:46:0x00af, code:
            r7.keyExchange.skipClientCredentials();
            r8 = null;
     */
    /* JADX WARNING: Missing block: B:47:0x00b6, code:
            r8 = r7.authentication.getClientCredentials(r7.certificateRequest);
     */
    /* JADX WARNING: Missing block: B:48:0x00be, code:
            if (r8 != null) goto L_0x00cb;
     */
    /* JADX WARNING: Missing block: B:49:0x00c0, code:
            r7.keyExchange.skipClientCredentials();
            r9 = org.bouncycastle.crypto.tls.Certificate.EMPTY_CHAIN;
     */
    /* JADX WARNING: Missing block: B:50:0x00c7, code:
            sendCertificateMessage(r9);
     */
    /* JADX WARNING: Missing block: B:51:0x00cb, code:
            r7.keyExchange.processClientCredentials(r8);
            r9 = r8.getCertificate();
     */
    /* JADX WARNING: Missing block: B:52:0x00d5, code:
            r7.connection_state = (short) 10;
            sendClientKeyExchangeMessage();
            r7.connection_state = (short) 11;
     */
    /* JADX WARNING: Missing block: B:53:0x00e6, code:
            if (org.bouncycastle.crypto.tls.TlsUtils.isSSL(getContext()) == false) goto L_0x00f1;
     */
    /* JADX WARNING: Missing block: B:54:0x00e8, code:
            org.bouncycastle.crypto.tls.TlsProtocol.establishMasterSecret(getContext(), r7.keyExchange);
     */
    /* JADX WARNING: Missing block: B:55:0x00f1, code:
            r9 = r7.recordStream.prepareToFinish();
            r7.securityParameters.sessionHash = org.bouncycastle.crypto.tls.TlsProtocol.getCurrentPRFHash(getContext(), r9, null);
     */
    /* JADX WARNING: Missing block: B:56:0x010b, code:
            if (org.bouncycastle.crypto.tls.TlsUtils.isSSL(getContext()) != false) goto L_0x0116;
     */
    /* JADX WARNING: Missing block: B:57:0x010d, code:
            org.bouncycastle.crypto.tls.TlsProtocol.establishMasterSecret(getContext(), r7.keyExchange);
     */
    /* JADX WARNING: Missing block: B:58:0x0116, code:
            r7.recordStream.setPendingConnectionState(getPeer().getCompression(), getPeer().getCipher());
     */
    /* JADX WARNING: Missing block: B:59:0x012b, code:
            if (r8 == null) goto L_0x015c;
     */
    /* JADX WARNING: Missing block: B:61:0x012f, code:
            if ((r8 instanceof org.bouncycastle.crypto.tls.TlsSignerCredentials) == false) goto L_0x015c;
     */
    /* JADX WARNING: Missing block: B:62:0x0131, code:
            r8 = (org.bouncycastle.crypto.tls.TlsSignerCredentials) r8;
            r0 = org.bouncycastle.crypto.tls.TlsUtils.getSignatureAndHashAlgorithm(getContext(), r8);
     */
    /* JADX WARNING: Missing block: B:63:0x013b, code:
            if (r0 != null) goto L_0x0144;
     */
    /* JADX WARNING: Missing block: B:64:0x013d, code:
            r9 = r7.securityParameters.getSessionHash();
     */
    /* JADX WARNING: Missing block: B:65:0x0144, code:
            r9 = r9.getFinalHash(r0.getHash());
     */
    /* JADX WARNING: Missing block: B:66:0x014c, code:
            sendCertificateVerifyMessage(new org.bouncycastle.crypto.tls.DigitallySigned(r0, r8.generateCertificateSignature(r9)));
            r7.connection_state = (short) 12;
     */
    /* JADX WARNING: Missing block: B:67:0x015c, code:
            sendChangeCipherSpecMessage();
            sendFinishedMessage();
            r7.connection_state = (short) 13;
     */
    /* JADX WARNING: Missing block: B:68:0x0164, code:
            return;
     */
    /* JADX WARNING: Missing block: B:84:0x01b4, code:
            r7.keyExchange.skipServerCredentials();
            r7.authentication = null;
     */
    /* JADX WARNING: Missing block: B:85:0x01bb, code:
            r7.keyExchange.processServerKeyExchange(r9);
            org.bouncycastle.crypto.tls.TlsProtocol.assertEmpty(r9);
            r8 = (short) 6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void handleHandshakeMessage(short s, ByteArrayInputStream byteArrayInputStream) throws IOException {
        if (!this.resumedSession) {
            if (s == (short) 0) {
                TlsProtocol.assertEmpty(byteArrayInputStream);
                if (this.connection_state == (short) 16) {
                    refuseRenegotiation();
                }
            } else if (s != (short) 2) {
                if (s != (short) 4) {
                    if (s != (short) 20) {
                        switch (s) {
                            case (short) 11:
                                switch (this.connection_state) {
                                    case (short) 2:
                                        handleSupplementalData(null);
                                        break;
                                    case (short) 3:
                                        break;
                                    default:
                                        throw new TlsFatalAlert((short) 10);
                                }
                                this.peerCertificate = Certificate.parse(byteArrayInputStream);
                                TlsProtocol.assertEmpty(byteArrayInputStream);
                                if (this.peerCertificate == null || this.peerCertificate.isEmpty()) {
                                    this.allowCertificateStatus = false;
                                }
                                this.keyExchange.processServerCertificate(this.peerCertificate);
                                this.authentication = this.tlsClient.getAuthentication();
                                this.authentication.notifyServerCertificate(this.peerCertificate);
                                this.connection_state = (short) 4;
                                return;
                            case (short) 12:
                                switch (this.connection_state) {
                                    case (short) 2:
                                        handleSupplementalData(null);
                                        break;
                                    case (short) 3:
                                        break;
                                    case (short) 4:
                                    case (short) 5:
                                        break;
                                    default:
                                        throw new TlsFatalAlert((short) 10);
                                }
                            case (short) 13:
                                switch (this.connection_state) {
                                    case (short) 4:
                                    case (short) 5:
                                        this.keyExchange.skipServerKeyExchange();
                                        break;
                                    case (short) 6:
                                        break;
                                    default:
                                        throw new TlsFatalAlert((short) 10);
                                }
                                if (this.authentication != null) {
                                    this.certificateRequest = CertificateRequest.parse(getContext(), byteArrayInputStream);
                                    TlsProtocol.assertEmpty(byteArrayInputStream);
                                    this.keyExchange.validateCertificateRequest(this.certificateRequest);
                                    TlsUtils.trackHashAlgorithms(this.recordStream.getHandshakeHash(), this.certificateRequest.getSupportedSignatureAlgorithms());
                                    s = (short) 7;
                                    break;
                                }
                                throw new TlsFatalAlert((short) 40);
                            case (short) 14:
                                switch (this.connection_state) {
                                    case (short) 2:
                                        handleSupplementalData(null);
                                        break;
                                    case (short) 3:
                                        break;
                                    case (short) 4:
                                    case (short) 5:
                                        break;
                                    case (short) 6:
                                    case (short) 7:
                                        break;
                                    default:
                                        throw new TlsFatalAlert((short) 10);
                                }
                            default:
                                switch (s) {
                                    case (short) 22:
                                        if (this.connection_state != (short) 4) {
                                            throw new TlsFatalAlert((short) 10);
                                        } else if (this.allowCertificateStatus) {
                                            this.certificateStatus = CertificateStatus.parse(byteArrayInputStream);
                                            TlsProtocol.assertEmpty(byteArrayInputStream);
                                            s = (short) 5;
                                            break;
                                        } else {
                                            throw new TlsFatalAlert((short) 10);
                                        }
                                    case (short) 23:
                                        if (this.connection_state == (short) 2) {
                                            handleSupplementalData(TlsProtocol.readSupplementalDataMessage(byteArrayInputStream));
                                            return;
                                        }
                                        throw new TlsFatalAlert((short) 10);
                                    default:
                                        throw new TlsFatalAlert((short) 10);
                                }
                        }
                    }
                    switch (this.connection_state) {
                        case (short) 13:
                            if (this.expectSessionTicket) {
                                throw new TlsFatalAlert((short) 10);
                            }
                            break;
                        case (short) 14:
                            break;
                        default:
                            throw new TlsFatalAlert((short) 10);
                    }
                    processFinishedMessage(byteArrayInputStream);
                    this.connection_state = (short) 15;
                    completeHandshake();
                    return;
                } else if (this.connection_state != (short) 13) {
                    throw new TlsFatalAlert((short) 10);
                } else if (this.expectSessionTicket) {
                    invalidateSession();
                    receiveNewSessionTicketMessage(byteArrayInputStream);
                    s = (short) 14;
                } else {
                    throw new TlsFatalAlert((short) 10);
                }
                this.connection_state = s;
            } else if (this.connection_state == (short) 1) {
                receiveServerHelloMessage(byteArrayInputStream);
                this.connection_state = (short) 2;
                this.recordStream.notifyHelloComplete();
                applyMaxFragmentLengthExtension();
                if (this.resumedSession) {
                    this.securityParameters.masterSecret = Arrays.clone(this.sessionParameters.getMasterSecret());
                    this.recordStream.setPendingConnectionState(getPeer().getCompression(), getPeer().getCipher());
                    return;
                }
                invalidateSession();
                if (this.selectedSessionID.length > 0) {
                    this.tlsSession = new TlsSessionImpl(this.selectedSessionID, null);
                }
            } else {
                throw new TlsFatalAlert((short) 10);
            }
        } else if (s == (short) 20 && this.connection_state == (short) 2) {
            processFinishedMessage(byteArrayInputStream);
            this.connection_state = (short) 15;
            sendChangeCipherSpecMessage();
            sendFinishedMessage();
            this.connection_state = (short) 13;
            completeHandshake();
        } else {
            throw new TlsFatalAlert((short) 10);
        }
    }

    protected void handleSupplementalData(Vector vector) throws IOException {
        this.tlsClient.processServerSupplementalData(vector);
        this.connection_state = (short) 3;
        this.keyExchange = this.tlsClient.getKeyExchange();
        this.keyExchange.init(getContext());
    }

    protected void receiveNewSessionTicketMessage(ByteArrayInputStream byteArrayInputStream) throws IOException {
        NewSessionTicket parse = NewSessionTicket.parse(byteArrayInputStream);
        TlsProtocol.assertEmpty(byteArrayInputStream);
        this.tlsClient.notifyNewSessionTicket(parse);
    }

    protected void receiveServerHelloMessage(ByteArrayInputStream byteArrayInputStream) throws IOException {
        ProtocolVersion readVersion = TlsUtils.readVersion(byteArrayInputStream);
        if (readVersion.isDTLS()) {
            throw new TlsFatalAlert((short) 47);
        } else if (!readVersion.equals(this.recordStream.getReadVersion())) {
            throw new TlsFatalAlert((short) 47);
        } else if (readVersion.isEqualOrEarlierVersionOf(getContext().getClientVersion())) {
            this.recordStream.setWriteVersion(readVersion);
            getContextAdmin().setServerVersion(readVersion);
            this.tlsClient.notifyServerVersion(readVersion);
            this.securityParameters.serverRandom = TlsUtils.readFully(32, (InputStream) byteArrayInputStream);
            this.selectedSessionID = TlsUtils.readOpaque8(byteArrayInputStream);
            if (this.selectedSessionID.length <= 32) {
                this.tlsClient.notifySessionID(this.selectedSessionID);
                boolean z = false;
                boolean z2 = this.selectedSessionID.length > 0 && this.tlsSession != null && Arrays.areEqual(this.selectedSessionID, this.tlsSession.getSessionID());
                this.resumedSession = z2;
                int readUint16 = TlsUtils.readUint16(byteArrayInputStream);
                if (!Arrays.contains(this.offeredCipherSuites, readUint16) || readUint16 == 0 || CipherSuite.isSCSV(readUint16) || !TlsUtils.isValidCipherSuiteForVersion(readUint16, getContext().getServerVersion())) {
                    throw new TlsFatalAlert((short) 47);
                }
                this.tlsClient.notifySelectedCipherSuite(readUint16);
                short readUint8 = TlsUtils.readUint8(byteArrayInputStream);
                if (Arrays.contains(this.offeredCompressionMethods, readUint8)) {
                    this.tlsClient.notifySelectedCompressionMethod(readUint8);
                    this.serverExtensions = TlsProtocol.readExtensions(byteArrayInputStream);
                    if (this.serverExtensions != null) {
                        Enumeration keys = this.serverExtensions.keys();
                        while (keys.hasMoreElements()) {
                            Integer num = (Integer) keys.nextElement();
                            if (!num.equals(EXT_RenegotiationInfo)) {
                                if (TlsUtils.getExtensionData(this.clientExtensions, num) != null) {
                                    boolean z3 = this.resumedSession;
                                } else {
                                    throw new TlsFatalAlert(AlertDescription.unsupported_extension);
                                }
                            }
                        }
                    }
                    byte[] extensionData = TlsUtils.getExtensionData(this.serverExtensions, EXT_RenegotiationInfo);
                    if (extensionData != null) {
                        this.secure_renegotiation = true;
                        if (!Arrays.constantTimeAreEqual(extensionData, TlsProtocol.createRenegotiationInfo(TlsUtils.EMPTY_BYTES))) {
                            throw new TlsFatalAlert((short) 40);
                        }
                    }
                    this.tlsClient.notifySecureRenegotiation(this.secure_renegotiation);
                    Hashtable hashtable = this.clientExtensions;
                    Hashtable hashtable2 = this.serverExtensions;
                    if (this.resumedSession) {
                        if (readUint16 == this.sessionParameters.getCipherSuite() && readUint8 == this.sessionParameters.getCompressionAlgorithm()) {
                            hashtable = null;
                            hashtable2 = this.sessionParameters.readServerExtensions();
                        } else {
                            throw new TlsFatalAlert((short) 47);
                        }
                    }
                    this.securityParameters.cipherSuite = readUint16;
                    this.securityParameters.compressionAlgorithm = readUint8;
                    if (hashtable2 != null) {
                        boolean hasEncryptThenMACExtension = TlsExtensionsUtils.hasEncryptThenMACExtension(hashtable2);
                        if (!hasEncryptThenMACExtension || TlsUtils.isBlockCipherSuite(readUint16)) {
                            this.securityParameters.encryptThenMAC = hasEncryptThenMACExtension;
                            this.securityParameters.extendedMasterSecret = TlsExtensionsUtils.hasExtendedMasterSecretExtension(hashtable2);
                            this.securityParameters.maxFragmentLength = processMaxFragmentLengthExtension(hashtable, hashtable2, (short) 47);
                            this.securityParameters.truncatedHMac = TlsExtensionsUtils.hasTruncatedHMacExtension(hashtable2);
                            z2 = !this.resumedSession && TlsUtils.hasExpectedEmptyExtensionData(hashtable2, TlsExtensionsUtils.EXT_status_request, (short) 47);
                            this.allowCertificateStatus = z2;
                            if (!this.resumedSession && TlsUtils.hasExpectedEmptyExtensionData(hashtable2, TlsProtocol.EXT_SessionTicket, (short) 47)) {
                                z = true;
                            }
                            this.expectSessionTicket = z;
                        } else {
                            throw new TlsFatalAlert((short) 47);
                        }
                    }
                    if (hashtable != null) {
                        this.tlsClient.processServerExtensions(hashtable2);
                    }
                    this.securityParameters.prfAlgorithm = TlsProtocol.getPRFAlgorithm(getContext(), this.securityParameters.getCipherSuite());
                    this.securityParameters.verifyDataLength = 12;
                    return;
                }
                throw new TlsFatalAlert((short) 47);
            }
            throw new TlsFatalAlert((short) 47);
        } else {
            throw new TlsFatalAlert((short) 47);
        }
    }

    protected void sendCertificateVerifyMessage(DigitallySigned digitallySigned) throws IOException {
        OutputStream handshakeMessage = new HandshakeMessage(this, (short) 15);
        digitallySigned.encode(handshakeMessage);
        handshakeMessage.writeToRecordStream();
    }

    protected void sendClientHelloMessage() throws IOException {
        this.recordStream.setWriteVersion(this.tlsClient.getClientHelloRecordLayerVersion());
        ProtocolVersion clientVersion = this.tlsClient.getClientVersion();
        if (clientVersion.isDTLS()) {
            throw new TlsFatalAlert((short) 80);
        }
        getContextAdmin().setClientVersion(clientVersion);
        byte[] bArr = TlsUtils.EMPTY_BYTES;
        if (this.tlsSession != null) {
            bArr = this.tlsSession.getSessionID();
            if (bArr == null || bArr.length > 32) {
                bArr = TlsUtils.EMPTY_BYTES;
            }
        }
        boolean isFallback = this.tlsClient.isFallback();
        this.offeredCipherSuites = this.tlsClient.getCipherSuites();
        this.offeredCompressionMethods = this.tlsClient.getCompressionMethods();
        if (!(bArr.length <= 0 || this.sessionParameters == null || (Arrays.contains(this.offeredCipherSuites, this.sessionParameters.getCipherSuite()) && Arrays.contains(this.offeredCompressionMethods, this.sessionParameters.getCompressionAlgorithm())))) {
            bArr = TlsUtils.EMPTY_BYTES;
        }
        this.clientExtensions = this.tlsClient.getClientExtensions();
        OutputStream handshakeMessage = new HandshakeMessage(this, (short) 1);
        TlsUtils.writeVersion(clientVersion, handshakeMessage);
        handshakeMessage.write(this.securityParameters.getClientRandom());
        TlsUtils.writeOpaque8(bArr, handshakeMessage);
        int contains = Arrays.contains(this.offeredCipherSuites, 255) ^ 1;
        if (!((TlsUtils.getExtensionData(this.clientExtensions, EXT_RenegotiationInfo) == null ? (short) 1 : (short) 0) == (short) 0 || contains == 0)) {
            this.offeredCipherSuites = Arrays.append(this.offeredCipherSuites, 255);
        }
        if (isFallback && !Arrays.contains(this.offeredCipherSuites, (int) CipherSuite.TLS_FALLBACK_SCSV)) {
            this.offeredCipherSuites = Arrays.append(this.offeredCipherSuites, (int) CipherSuite.TLS_FALLBACK_SCSV);
        }
        TlsUtils.writeUint16ArrayWithUint16Length(this.offeredCipherSuites, handshakeMessage);
        TlsUtils.writeUint8ArrayWithUint8Length(this.offeredCompressionMethods, handshakeMessage);
        if (this.clientExtensions != null) {
            TlsProtocol.writeExtensions(handshakeMessage, this.clientExtensions);
        }
        handshakeMessage.writeToRecordStream();
    }

    protected void sendClientKeyExchangeMessage() throws IOException {
        OutputStream handshakeMessage = new HandshakeMessage(this, (short) 16);
        this.keyExchange.generateClientKeyExchange(handshakeMessage);
        handshakeMessage.writeToRecordStream();
    }
}
