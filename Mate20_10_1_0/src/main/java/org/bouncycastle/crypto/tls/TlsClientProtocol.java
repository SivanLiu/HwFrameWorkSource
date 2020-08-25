package org.bouncycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.bouncycastle.crypto.tls.TlsProtocol;
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

    /* access modifiers changed from: protected */
    @Override // org.bouncycastle.crypto.tls.TlsProtocol
    public void cleanupHandshake() {
        super.cleanupHandshake();
        this.selectedSessionID = null;
        this.keyExchange = null;
        this.authentication = null;
        this.certificateStatus = null;
        this.certificateRequest = null;
    }

    public void connect(TlsClient tlsClient2) throws IOException {
        SessionParameters exportSessionParameters;
        if (tlsClient2 == null) {
            throw new IllegalArgumentException("'tlsClient' cannot be null");
        } else if (this.tlsClient == null) {
            this.tlsClient = tlsClient2;
            this.securityParameters = new SecurityParameters();
            this.securityParameters.entity = 1;
            this.tlsClientContext = new TlsClientContextImpl(this.secureRandom, this.securityParameters);
            this.securityParameters.clientRandom = createRandomBlock(tlsClient2.shouldUseGMTUnixTime(), this.tlsClientContext.getNonceRandomGenerator());
            this.tlsClient.init(this.tlsClientContext);
            this.recordStream.init(this.tlsClientContext);
            tlsClient2.notifyCloseHandle(this);
            TlsSession sessionToResume = tlsClient2.getSessionToResume();
            if (sessionToResume != null && sessionToResume.isResumable() && (exportSessionParameters = sessionToResume.exportSessionParameters()) != null && exportSessionParameters.isExtendedMasterSecret()) {
                this.tlsSession = sessionToResume;
                this.sessionParameters = exportSessionParameters;
            }
            sendClientHelloMessage();
            this.connection_state = 1;
            blockForHandshake();
        } else {
            throw new IllegalStateException("'connect' can only be called once");
        }
    }

    /* access modifiers changed from: protected */
    @Override // org.bouncycastle.crypto.tls.TlsProtocol
    public TlsContext getContext() {
        return this.tlsClientContext;
    }

    /* access modifiers changed from: package-private */
    @Override // org.bouncycastle.crypto.tls.TlsProtocol
    public AbstractTlsContext getContextAdmin() {
        return this.tlsClientContext;
    }

    /* access modifiers changed from: protected */
    @Override // org.bouncycastle.crypto.tls.TlsProtocol
    public TlsPeer getPeer() {
        return this.tlsClient;
    }

    /* access modifiers changed from: protected */
    @Override // org.bouncycastle.crypto.tls.TlsProtocol
    public void handleHandshakeMessage(short s, ByteArrayInputStream byteArrayInputStream) throws IOException {
        TlsCredentials tlsCredentials;
        Certificate certificate;
        if (this.resumedSession) {
            if (s == 20 && this.connection_state == 2) {
                processFinishedMessage(byteArrayInputStream);
                this.connection_state = 15;
                sendChangeCipherSpecMessage();
                sendFinishedMessage();
                this.connection_state = 13;
                completeHandshake();
                return;
            }
            throw new TlsFatalAlert(10);
        } else if (s == 0) {
            assertEmpty(byteArrayInputStream);
            if (this.connection_state == 16) {
                refuseRenegotiation();
            }
        } else if (s != 2) {
            if (s != 4) {
                if (s == 20) {
                    short s2 = this.connection_state;
                    if (s2 != 13) {
                        if (s2 != 14) {
                            throw new TlsFatalAlert(10);
                        }
                    } else if (this.expectSessionTicket) {
                        throw new TlsFatalAlert(10);
                    }
                    processFinishedMessage(byteArrayInputStream);
                    this.connection_state = 15;
                    completeHandshake();
                } else if (s != 22) {
                    if (s != 23) {
                        switch (s) {
                            case 11:
                                short s3 = this.connection_state;
                                if (s3 == 2) {
                                    handleSupplementalData(null);
                                } else if (s3 != 3) {
                                    throw new TlsFatalAlert(10);
                                }
                                this.peerCertificate = Certificate.parse(byteArrayInputStream);
                                assertEmpty(byteArrayInputStream);
                                if (this.peerCertificate == null || this.peerCertificate.isEmpty()) {
                                    this.allowCertificateStatus = false;
                                }
                                this.keyExchange.processServerCertificate(this.peerCertificate);
                                this.authentication = this.tlsClient.getAuthentication();
                                this.authentication.notifyServerCertificate(this.peerCertificate);
                                this.connection_state = 4;
                                return;
                            case 12:
                                short s4 = this.connection_state;
                                if (s4 == 2) {
                                    handleSupplementalData(null);
                                } else if (s4 != 3) {
                                    if (!(s4 == 4 || s4 == 5)) {
                                        throw new TlsFatalAlert(10);
                                    }
                                    this.keyExchange.processServerKeyExchange(byteArrayInputStream);
                                    assertEmpty(byteArrayInputStream);
                                    this.connection_state = 6;
                                    return;
                                }
                                this.keyExchange.skipServerCredentials();
                                this.authentication = null;
                                this.keyExchange.processServerKeyExchange(byteArrayInputStream);
                                assertEmpty(byteArrayInputStream);
                                this.connection_state = 6;
                                return;
                            case 13:
                                short s5 = this.connection_state;
                                if (s5 == 4 || s5 == 5) {
                                    this.keyExchange.skipServerKeyExchange();
                                } else if (s5 != 6) {
                                    throw new TlsFatalAlert(10);
                                }
                                if (this.authentication != null) {
                                    this.certificateRequest = CertificateRequest.parse(getContext(), byteArrayInputStream);
                                    assertEmpty(byteArrayInputStream);
                                    this.keyExchange.validateCertificateRequest(this.certificateRequest);
                                    TlsUtils.trackHashAlgorithms(this.recordStream.getHandshakeHash(), this.certificateRequest.getSupportedSignatureAlgorithms());
                                    this.connection_state = 7;
                                    return;
                                }
                                throw new TlsFatalAlert(40);
                            case 14:
                                switch (this.connection_state) {
                                    case 2:
                                        handleSupplementalData(null);
                                    case 3:
                                        this.keyExchange.skipServerCredentials();
                                        this.authentication = null;
                                    case 4:
                                    case 5:
                                        this.keyExchange.skipServerKeyExchange();
                                        break;
                                    case 6:
                                    case 7:
                                        break;
                                    default:
                                        throw new TlsFatalAlert(10);
                                }
                                assertEmpty(byteArrayInputStream);
                                this.connection_state = 8;
                                this.recordStream.getHandshakeHash().sealHashAlgorithms();
                                Vector clientSupplementalData = this.tlsClient.getClientSupplementalData();
                                if (clientSupplementalData != null) {
                                    sendSupplementalDataMessage(clientSupplementalData);
                                }
                                this.connection_state = 9;
                                CertificateRequest certificateRequest2 = this.certificateRequest;
                                if (certificateRequest2 == null) {
                                    this.keyExchange.skipClientCredentials();
                                    tlsCredentials = null;
                                } else {
                                    tlsCredentials = this.authentication.getClientCredentials(certificateRequest2);
                                    TlsKeyExchange tlsKeyExchange = this.keyExchange;
                                    if (tlsCredentials == null) {
                                        tlsKeyExchange.skipClientCredentials();
                                        certificate = Certificate.EMPTY_CHAIN;
                                    } else {
                                        tlsKeyExchange.processClientCredentials(tlsCredentials);
                                        certificate = tlsCredentials.getCertificate();
                                    }
                                    sendCertificateMessage(certificate);
                                }
                                this.connection_state = 10;
                                sendClientKeyExchangeMessage();
                                this.connection_state = 11;
                                if (TlsUtils.isSSL(getContext())) {
                                    establishMasterSecret(getContext(), this.keyExchange);
                                }
                                TlsHandshakeHash prepareToFinish = this.recordStream.prepareToFinish();
                                this.securityParameters.sessionHash = getCurrentPRFHash(getContext(), prepareToFinish, null);
                                if (!TlsUtils.isSSL(getContext())) {
                                    establishMasterSecret(getContext(), this.keyExchange);
                                }
                                this.recordStream.setPendingConnectionState(getPeer().getCompression(), getPeer().getCipher());
                                if (tlsCredentials != null && (tlsCredentials instanceof TlsSignerCredentials)) {
                                    TlsSignerCredentials tlsSignerCredentials = (TlsSignerCredentials) tlsCredentials;
                                    SignatureAndHashAlgorithm signatureAndHashAlgorithm = TlsUtils.getSignatureAndHashAlgorithm(getContext(), tlsSignerCredentials);
                                    sendCertificateVerifyMessage(new DigitallySigned(signatureAndHashAlgorithm, tlsSignerCredentials.generateCertificateSignature(signatureAndHashAlgorithm == null ? this.securityParameters.getSessionHash() : prepareToFinish.getFinalHash(signatureAndHashAlgorithm.getHash()))));
                                    this.connection_state = 12;
                                }
                                sendChangeCipherSpecMessage();
                                sendFinishedMessage();
                                this.connection_state = 13;
                                return;
                            default:
                                throw new TlsFatalAlert(10);
                        }
                    } else if (this.connection_state == 2) {
                        handleSupplementalData(readSupplementalDataMessage(byteArrayInputStream));
                    } else {
                        throw new TlsFatalAlert(10);
                    }
                } else if (this.connection_state != 4) {
                    throw new TlsFatalAlert(10);
                } else if (this.allowCertificateStatus) {
                    this.certificateStatus = CertificateStatus.parse(byteArrayInputStream);
                    assertEmpty(byteArrayInputStream);
                    this.connection_state = 5;
                } else {
                    throw new TlsFatalAlert(10);
                }
            } else if (this.connection_state != 13) {
                throw new TlsFatalAlert(10);
            } else if (this.expectSessionTicket) {
                invalidateSession();
                receiveNewSessionTicketMessage(byteArrayInputStream);
                this.connection_state = 14;
            } else {
                throw new TlsFatalAlert(10);
            }
        } else if (this.connection_state == 1) {
            receiveServerHelloMessage(byteArrayInputStream);
            this.connection_state = 2;
            this.recordStream.notifyHelloComplete();
            applyMaxFragmentLengthExtension();
            if (this.resumedSession) {
                this.securityParameters.masterSecret = Arrays.clone(this.sessionParameters.getMasterSecret());
                this.recordStream.setPendingConnectionState(getPeer().getCompression(), getPeer().getCipher());
                return;
            }
            invalidateSession();
            byte[] bArr = this.selectedSessionID;
            if (bArr.length > 0) {
                this.tlsSession = new TlsSessionImpl(bArr, null);
            }
        } else {
            throw new TlsFatalAlert(10);
        }
    }

    /* access modifiers changed from: protected */
    public void handleSupplementalData(Vector vector) throws IOException {
        this.tlsClient.processServerSupplementalData(vector);
        this.connection_state = 3;
        this.keyExchange = this.tlsClient.getKeyExchange();
        this.keyExchange.init(getContext());
    }

    /* access modifiers changed from: protected */
    public void receiveNewSessionTicketMessage(ByteArrayInputStream byteArrayInputStream) throws IOException {
        NewSessionTicket parse = NewSessionTicket.parse(byteArrayInputStream);
        assertEmpty(byteArrayInputStream);
        this.tlsClient.notifyNewSessionTicket(parse);
    }

    /* access modifiers changed from: protected */
    public void receiveServerHelloMessage(ByteArrayInputStream byteArrayInputStream) throws IOException {
        ProtocolVersion readVersion = TlsUtils.readVersion(byteArrayInputStream);
        if (readVersion.isDTLS()) {
            throw new TlsFatalAlert(47);
        } else if (!readVersion.equals(this.recordStream.getReadVersion())) {
            throw new TlsFatalAlert(47);
        } else if (readVersion.isEqualOrEarlierVersionOf(getContext().getClientVersion())) {
            this.recordStream.setWriteVersion(readVersion);
            getContextAdmin().setServerVersion(readVersion);
            this.tlsClient.notifyServerVersion(readVersion);
            this.securityParameters.serverRandom = TlsUtils.readFully(32, byteArrayInputStream);
            this.selectedSessionID = TlsUtils.readOpaque8(byteArrayInputStream);
            byte[] bArr = this.selectedSessionID;
            if (bArr.length <= 32) {
                this.tlsClient.notifySessionID(bArr);
                boolean z = false;
                this.resumedSession = this.selectedSessionID.length > 0 && this.tlsSession != null && Arrays.areEqual(this.selectedSessionID, this.tlsSession.getSessionID());
                int readUint16 = TlsUtils.readUint16(byteArrayInputStream);
                if (!Arrays.contains(this.offeredCipherSuites, readUint16) || readUint16 == 0 || CipherSuite.isSCSV(readUint16) || !TlsUtils.isValidCipherSuiteForVersion(readUint16, getContext().getServerVersion())) {
                    throw new TlsFatalAlert(47);
                }
                this.tlsClient.notifySelectedCipherSuite(readUint16);
                short readUint8 = TlsUtils.readUint8(byteArrayInputStream);
                if (Arrays.contains(this.offeredCompressionMethods, readUint8)) {
                    this.tlsClient.notifySelectedCompressionMethod(readUint8);
                    this.serverExtensions = readExtensions(byteArrayInputStream);
                    this.securityParameters.extendedMasterSecret = !TlsUtils.isSSL(this.tlsClientContext) && TlsExtensionsUtils.hasExtendedMasterSecretExtension(this.serverExtensions);
                    if (this.securityParameters.isExtendedMasterSecret() || (!this.resumedSession && !this.tlsClient.requiresExtendedMasterSecret())) {
                        if (this.serverExtensions != null) {
                            Enumeration keys = this.serverExtensions.keys();
                            while (keys.hasMoreElements()) {
                                Integer num = (Integer) keys.nextElement();
                                if (!num.equals(EXT_RenegotiationInfo)) {
                                    if (TlsUtils.getExtensionData(this.clientExtensions, num) != null) {
                                        boolean z2 = this.resumedSession;
                                    } else {
                                        throw new TlsFatalAlert(AlertDescription.unsupported_extension);
                                    }
                                }
                            }
                        }
                        byte[] extensionData = TlsUtils.getExtensionData(this.serverExtensions, EXT_RenegotiationInfo);
                        if (extensionData != null) {
                            this.secure_renegotiation = true;
                            if (!Arrays.constantTimeAreEqual(extensionData, createRenegotiationInfo(TlsUtils.EMPTY_BYTES))) {
                                throw new TlsFatalAlert(40);
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
                                throw new TlsFatalAlert(47);
                            }
                        }
                        this.securityParameters.cipherSuite = readUint16;
                        this.securityParameters.compressionAlgorithm = readUint8;
                        if (hashtable2 != null && !hashtable2.isEmpty()) {
                            boolean hasEncryptThenMACExtension = TlsExtensionsUtils.hasEncryptThenMACExtension(hashtable2);
                            if (!hasEncryptThenMACExtension || TlsUtils.isBlockCipherSuite(readUint16)) {
                                this.securityParameters.encryptThenMAC = hasEncryptThenMACExtension;
                                this.securityParameters.maxFragmentLength = processMaxFragmentLengthExtension(hashtable, hashtable2, 47);
                                this.securityParameters.truncatedHMac = TlsExtensionsUtils.hasTruncatedHMacExtension(hashtable2);
                                this.allowCertificateStatus = !this.resumedSession && TlsUtils.hasExpectedEmptyExtensionData(hashtable2, TlsExtensionsUtils.EXT_status_request, 47);
                                if (!this.resumedSession && TlsUtils.hasExpectedEmptyExtensionData(hashtable2, TlsProtocol.EXT_SessionTicket, 47)) {
                                    z = true;
                                }
                                this.expectSessionTicket = z;
                            } else {
                                throw new TlsFatalAlert(47);
                            }
                        }
                        if (hashtable != null) {
                            this.tlsClient.processServerExtensions(hashtable2);
                        }
                        this.securityParameters.prfAlgorithm = getPRFAlgorithm(getContext(), this.securityParameters.getCipherSuite());
                        this.securityParameters.verifyDataLength = 12;
                        return;
                    }
                    throw new TlsFatalAlert(40);
                }
                throw new TlsFatalAlert(47);
            }
            throw new TlsFatalAlert(47);
        } else {
            throw new TlsFatalAlert(47);
        }
    }

    /* access modifiers changed from: protected */
    public void sendCertificateVerifyMessage(DigitallySigned digitallySigned) throws IOException {
        HandshakeMessage handshakeMessage = new HandshakeMessage(this, 15);
        digitallySigned.encode(handshakeMessage);
        handshakeMessage.writeToRecordStream();
    }

    /* access modifiers changed from: protected */
    public void sendClientHelloMessage() throws IOException {
        this.recordStream.setWriteVersion(this.tlsClient.getClientHelloRecordLayerVersion());
        ProtocolVersion clientVersion = this.tlsClient.getClientVersion();
        if (!clientVersion.isDTLS()) {
            getContextAdmin().setClientVersion(clientVersion);
            byte[] bArr = TlsUtils.EMPTY_BYTES;
            if (this.tlsSession != null && ((bArr = this.tlsSession.getSessionID()) == null || bArr.length > 32)) {
                bArr = TlsUtils.EMPTY_BYTES;
            }
            boolean isFallback = this.tlsClient.isFallback();
            this.offeredCipherSuites = this.tlsClient.getCipherSuites();
            this.offeredCompressionMethods = this.tlsClient.getCompressionMethods();
            if (bArr.length > 0 && this.sessionParameters != null && (!this.sessionParameters.isExtendedMasterSecret() || !Arrays.contains(this.offeredCipherSuites, this.sessionParameters.getCipherSuite()) || !Arrays.contains(this.offeredCompressionMethods, this.sessionParameters.getCompressionAlgorithm()))) {
                bArr = TlsUtils.EMPTY_BYTES;
            }
            this.clientExtensions = TlsExtensionsUtils.ensureExtensionsInitialised(this.tlsClient.getClientExtensions());
            if (!clientVersion.isSSL()) {
                TlsExtensionsUtils.addExtendedMasterSecretExtension(this.clientExtensions);
            }
            HandshakeMessage handshakeMessage = new HandshakeMessage(this, 1);
            TlsUtils.writeVersion(clientVersion, handshakeMessage);
            handshakeMessage.write(this.securityParameters.getClientRandom());
            TlsUtils.writeOpaque8(bArr, handshakeMessage);
            boolean z = TlsUtils.getExtensionData(this.clientExtensions, EXT_RenegotiationInfo) == null;
            boolean z2 = !Arrays.contains(this.offeredCipherSuites, 255);
            if (z && z2) {
                this.offeredCipherSuites = Arrays.append(this.offeredCipherSuites, 255);
            }
            if (isFallback && !Arrays.contains(this.offeredCipherSuites, (int) CipherSuite.TLS_FALLBACK_SCSV)) {
                this.offeredCipherSuites = Arrays.append(this.offeredCipherSuites, (int) CipherSuite.TLS_FALLBACK_SCSV);
            }
            TlsUtils.writeUint16ArrayWithUint16Length(this.offeredCipherSuites, handshakeMessage);
            TlsUtils.writeUint8ArrayWithUint8Length(this.offeredCompressionMethods, handshakeMessage);
            writeExtensions(handshakeMessage, this.clientExtensions);
            handshakeMessage.writeToRecordStream();
            return;
        }
        throw new TlsFatalAlert(80);
    }

    /* access modifiers changed from: protected */
    public void sendClientKeyExchangeMessage() throws IOException {
        HandshakeMessage handshakeMessage = new HandshakeMessage(this, 16);
        this.keyExchange.generateClientKeyExchange(handshakeMessage);
        handshakeMessage.writeToRecordStream();
    }
}
