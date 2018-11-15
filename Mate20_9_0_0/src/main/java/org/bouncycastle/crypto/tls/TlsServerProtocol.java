package org.bouncycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Vector;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.util.Arrays;

public class TlsServerProtocol extends TlsProtocol {
    protected CertificateRequest certificateRequest = null;
    protected short clientCertificateType = (short) -1;
    protected TlsKeyExchange keyExchange = null;
    protected TlsHandshakeHash prepareFinishHash = null;
    protected TlsCredentials serverCredentials = null;
    protected TlsServer tlsServer = null;
    TlsServerContextImpl tlsServerContext = null;

    public TlsServerProtocol(InputStream inputStream, OutputStream outputStream, SecureRandom secureRandom) {
        super(inputStream, outputStream, secureRandom);
    }

    public TlsServerProtocol(SecureRandom secureRandom) {
        super(secureRandom);
    }

    public void accept(TlsServer tlsServer) throws IOException {
        if (tlsServer == null) {
            throw new IllegalArgumentException("'tlsServer' cannot be null");
        } else if (this.tlsServer == null) {
            this.tlsServer = tlsServer;
            this.securityParameters = new SecurityParameters();
            this.securityParameters.entity = 0;
            this.tlsServerContext = new TlsServerContextImpl(this.secureRandom, this.securityParameters);
            this.securityParameters.serverRandom = TlsProtocol.createRandomBlock(tlsServer.shouldUseGMTUnixTime(), this.tlsServerContext.getNonceRandomGenerator());
            this.tlsServer.init(this.tlsServerContext);
            this.recordStream.init(this.tlsServerContext);
            this.recordStream.setRestrictReadVersion(false);
            blockForHandshake();
        } else {
            throw new IllegalStateException("'accept' can only be called once");
        }
    }

    protected void cleanupHandshake() {
        super.cleanupHandshake();
        this.keyExchange = null;
        this.serverCredentials = null;
        this.certificateRequest = null;
        this.prepareFinishHash = null;
    }

    protected boolean expectCertificateVerifyMessage() {
        return this.clientCertificateType >= (short) 0 && TlsUtils.hasSigningCapability(this.clientCertificateType);
    }

    protected TlsContext getContext() {
        return this.tlsServerContext;
    }

    AbstractTlsContext getContextAdmin() {
        return this.tlsServerContext;
    }

    protected TlsPeer getPeer() {
        return this.tlsServer;
    }

    /* JADX WARNING: Missing block: B:10:0x0024, code:
            notifyClientCertificate(org.bouncycastle.crypto.tls.Certificate.EMPTY_CHAIN);
            r2.connection_state = (short) 10;
     */
    /* JADX WARNING: Missing block: B:11:0x002b, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void handleAlertWarningMessage(short s) throws IOException {
        super.handleAlertWarningMessage(s);
        if (s == (short) 41) {
            if (TlsUtils.isSSL(getContext()) && this.certificateRequest != null) {
                switch (this.connection_state) {
                    case (short) 8:
                        this.tlsServer.processClientSupplementalData(null);
                        break;
                    case (short) 9:
                        break;
                }
            }
            throw new TlsFatalAlert((short) 10);
        }
    }

    protected void handleHandshakeMessage(short s, ByteArrayInputStream byteArrayInputStream) throws IOException {
        Certificate certificate = null;
        if (s == (short) 1) {
            s = this.connection_state;
            if (s == (short) 0) {
                receiveClientHelloMessage(byteArrayInputStream);
                this.connection_state = (short) 1;
                sendServerHelloMessage();
                this.connection_state = (short) 2;
                this.recordStream.notifyHelloComplete();
                Vector serverSupplementalData = this.tlsServer.getServerSupplementalData();
                if (serverSupplementalData != null) {
                    sendSupplementalDataMessage(serverSupplementalData);
                }
                this.connection_state = (short) 3;
                this.keyExchange = this.tlsServer.getKeyExchange();
                this.keyExchange.init(getContext());
                this.serverCredentials = this.tlsServer.getCredentials();
                if (this.serverCredentials == null) {
                    this.keyExchange.skipServerCredentials();
                } else {
                    this.keyExchange.processServerCredentials(this.serverCredentials);
                    certificate = this.serverCredentials.getCertificate();
                    sendCertificateMessage(certificate);
                }
                this.connection_state = (short) 4;
                boolean z = false;
                if (certificate == null || certificate.isEmpty()) {
                    this.allowCertificateStatus = false;
                }
                if (this.allowCertificateStatus) {
                    CertificateStatus certificateStatus = this.tlsServer.getCertificateStatus();
                    if (certificateStatus != null) {
                        sendCertificateStatusMessage(certificateStatus);
                    }
                }
                this.connection_state = (short) 5;
                byte[] generateServerKeyExchange = this.keyExchange.generateServerKeyExchange();
                if (generateServerKeyExchange != null) {
                    sendServerKeyExchangeMessage(generateServerKeyExchange);
                }
                this.connection_state = (short) 6;
                if (this.serverCredentials != null) {
                    this.certificateRequest = this.tlsServer.getCertificateRequest();
                    if (this.certificateRequest != null) {
                        boolean isTLSv12 = TlsUtils.isTLSv12(getContext());
                        if (this.certificateRequest.getSupportedSignatureAlgorithms() != null) {
                            z = true;
                        }
                        if (isTLSv12 == z) {
                            this.keyExchange.validateCertificateRequest(this.certificateRequest);
                            sendCertificateRequestMessage(this.certificateRequest);
                            TlsUtils.trackHashAlgorithms(this.recordStream.getHandshakeHash(), this.certificateRequest.getSupportedSignatureAlgorithms());
                        } else {
                            throw new TlsFatalAlert((short) 80);
                        }
                    }
                }
                this.connection_state = (short) 7;
                sendServerHelloDoneMessage();
                this.connection_state = (short) 8;
                this.recordStream.getHandshakeHash().sealHashAlgorithms();
            } else if (s == (short) 16) {
                refuseRenegotiation();
            } else {
                throw new TlsFatalAlert((short) 10);
            }
        } else if (s == (short) 11) {
            switch (this.connection_state) {
                case (short) 8:
                    this.tlsServer.processClientSupplementalData(null);
                    break;
                case (short) 9:
                    break;
                default:
                    throw new TlsFatalAlert((short) 10);
            }
            if (this.certificateRequest != null) {
                receiveCertificateMessage(byteArrayInputStream);
                this.connection_state = (short) 10;
                return;
            }
            throw new TlsFatalAlert((short) 10);
        } else if (s != (short) 20) {
            if (s != (short) 23) {
                switch (s) {
                    case (short) 15:
                        if (this.connection_state != (short) 11) {
                            throw new TlsFatalAlert((short) 10);
                        } else if (expectCertificateVerifyMessage()) {
                            receiveCertificateVerifyMessage(byteArrayInputStream);
                            s = (short) 12;
                            break;
                        } else {
                            throw new TlsFatalAlert((short) 10);
                        }
                    case (short) 16:
                        switch (this.connection_state) {
                            case (short) 8:
                                this.tlsServer.processClientSupplementalData(null);
                                break;
                            case (short) 9:
                                break;
                            case (short) 10:
                                break;
                            default:
                                throw new TlsFatalAlert((short) 10);
                        }
                        if (this.certificateRequest == null) {
                            this.keyExchange.skipClientCredentials();
                        } else if (TlsUtils.isTLSv12(getContext())) {
                            throw new TlsFatalAlert((short) 10);
                        } else if (!TlsUtils.isSSL(getContext())) {
                            notifyClientCertificate(Certificate.EMPTY_CHAIN);
                        } else if (this.peerCertificate == null) {
                            throw new TlsFatalAlert((short) 10);
                        }
                        receiveClientKeyExchangeMessage(byteArrayInputStream);
                        this.connection_state = (short) 11;
                        return;
                    default:
                        throw new TlsFatalAlert((short) 10);
                }
            } else if (this.connection_state == (short) 8) {
                this.tlsServer.processClientSupplementalData(TlsProtocol.readSupplementalDataMessage(byteArrayInputStream));
                s = (short) 9;
            } else {
                throw new TlsFatalAlert((short) 10);
            }
            this.connection_state = s;
        } else {
            switch (this.connection_state) {
                case (short) 11:
                    if (expectCertificateVerifyMessage()) {
                        throw new TlsFatalAlert((short) 10);
                    }
                    break;
                case (short) 12:
                    break;
                default:
                    throw new TlsFatalAlert((short) 10);
            }
            processFinishedMessage(byteArrayInputStream);
            this.connection_state = (short) 13;
            if (this.expectSessionTicket) {
                sendNewSessionTicketMessage(this.tlsServer.getNewSessionTicket());
            }
            this.connection_state = (short) 14;
            sendChangeCipherSpecMessage();
            sendFinishedMessage();
            this.connection_state = (short) 15;
            completeHandshake();
        }
    }

    protected void notifyClientCertificate(Certificate certificate) throws IOException {
        if (this.certificateRequest == null) {
            throw new IllegalStateException();
        } else if (this.peerCertificate == null) {
            this.peerCertificate = certificate;
            if (certificate.isEmpty()) {
                this.keyExchange.skipClientCredentials();
            } else {
                this.clientCertificateType = TlsUtils.getClientCertificateType(certificate, this.serverCredentials.getCertificate());
                this.keyExchange.processClientCertificate(certificate);
            }
            this.tlsServer.notifyClientCertificate(certificate);
        } else {
            throw new TlsFatalAlert((short) 10);
        }
    }

    protected void receiveCertificateMessage(ByteArrayInputStream byteArrayInputStream) throws IOException {
        Certificate parse = Certificate.parse(byteArrayInputStream);
        TlsProtocol.assertEmpty(byteArrayInputStream);
        notifyClientCertificate(parse);
    }

    protected void receiveCertificateVerifyMessage(ByteArrayInputStream byteArrayInputStream) throws IOException {
        if (this.certificateRequest != null) {
            DigitallySigned parse = DigitallySigned.parse(getContext(), byteArrayInputStream);
            TlsProtocol.assertEmpty(byteArrayInputStream);
            try {
                byte[] finalHash;
                SignatureAndHashAlgorithm algorithm = parse.getAlgorithm();
                if (TlsUtils.isTLSv12(getContext())) {
                    TlsUtils.verifySupportedSignatureAlgorithm(this.certificateRequest.getSupportedSignatureAlgorithms(), algorithm);
                    finalHash = this.prepareFinishHash.getFinalHash(algorithm.getHash());
                } else {
                    finalHash = this.securityParameters.getSessionHash();
                }
                AsymmetricKeyParameter createKey = PublicKeyFactory.createKey(this.peerCertificate.getCertificateAt(0).getSubjectPublicKeyInfo());
                TlsSigner createTlsSigner = TlsUtils.createTlsSigner(this.clientCertificateType);
                createTlsSigner.init(getContext());
                if (!createTlsSigner.verifyRawSignature(algorithm, parse.getSignature(), createKey, finalHash)) {
                    throw new TlsFatalAlert((short) 51);
                }
                return;
            } catch (TlsFatalAlert e) {
                throw e;
            } catch (Throwable e2) {
                throw new TlsFatalAlert((short) 51, e2);
            }
        }
        throw new IllegalStateException();
    }

    protected void receiveClientHelloMessage(ByteArrayInputStream byteArrayInputStream) throws IOException {
        ProtocolVersion readVersion = TlsUtils.readVersion(byteArrayInputStream);
        this.recordStream.setWriteVersion(readVersion);
        if (readVersion.isDTLS()) {
            throw new TlsFatalAlert((short) 47);
        }
        byte[] readFully = TlsUtils.readFully(32, (InputStream) byteArrayInputStream);
        if (TlsUtils.readOpaque8(byteArrayInputStream).length <= 32) {
            int readUint16 = TlsUtils.readUint16(byteArrayInputStream);
            if (readUint16 < 2 || (readUint16 & 1) != 0) {
                throw new TlsFatalAlert((short) 50);
            }
            this.offeredCipherSuites = TlsUtils.readUint16Array(readUint16 / 2, byteArrayInputStream);
            short readUint8 = TlsUtils.readUint8(byteArrayInputStream);
            if (readUint8 >= (short) 1) {
                this.offeredCompressionMethods = TlsUtils.readUint8Array(readUint8, byteArrayInputStream);
                this.clientExtensions = TlsProtocol.readExtensions(byteArrayInputStream);
                this.securityParameters.extendedMasterSecret = TlsExtensionsUtils.hasExtendedMasterSecretExtension(this.clientExtensions);
                getContextAdmin().setClientVersion(readVersion);
                this.tlsServer.notifyClientVersion(readVersion);
                this.tlsServer.notifyFallback(Arrays.contains(this.offeredCipherSuites, (int) CipherSuite.TLS_FALLBACK_SCSV));
                this.securityParameters.clientRandom = readFully;
                this.tlsServer.notifyOfferedCipherSuites(this.offeredCipherSuites);
                this.tlsServer.notifyOfferedCompressionMethods(this.offeredCompressionMethods);
                if (Arrays.contains(this.offeredCipherSuites, 255)) {
                    this.secure_renegotiation = true;
                }
                byte[] extensionData = TlsUtils.getExtensionData(this.clientExtensions, EXT_RenegotiationInfo);
                if (extensionData != null) {
                    this.secure_renegotiation = true;
                    if (!Arrays.constantTimeAreEqual(extensionData, TlsProtocol.createRenegotiationInfo(TlsUtils.EMPTY_BYTES))) {
                        throw new TlsFatalAlert((short) 40);
                    }
                }
                this.tlsServer.notifySecureRenegotiation(this.secure_renegotiation);
                if (this.clientExtensions != null) {
                    TlsExtensionsUtils.getPaddingExtension(this.clientExtensions);
                    this.tlsServer.processClientExtensions(this.clientExtensions);
                    return;
                }
                return;
            }
            throw new TlsFatalAlert((short) 47);
        }
        throw new TlsFatalAlert((short) 47);
    }

    protected void receiveClientKeyExchangeMessage(ByteArrayInputStream byteArrayInputStream) throws IOException {
        this.keyExchange.processClientKeyExchange(byteArrayInputStream);
        TlsProtocol.assertEmpty(byteArrayInputStream);
        if (TlsUtils.isSSL(getContext())) {
            TlsProtocol.establishMasterSecret(getContext(), this.keyExchange);
        }
        this.prepareFinishHash = this.recordStream.prepareToFinish();
        this.securityParameters.sessionHash = TlsProtocol.getCurrentPRFHash(getContext(), this.prepareFinishHash, null);
        if (!TlsUtils.isSSL(getContext())) {
            TlsProtocol.establishMasterSecret(getContext(), this.keyExchange);
        }
        this.recordStream.setPendingConnectionState(getPeer().getCompression(), getPeer().getCipher());
    }

    protected void sendCertificateRequestMessage(CertificateRequest certificateRequest) throws IOException {
        OutputStream handshakeMessage = new HandshakeMessage(this, (short) 13);
        certificateRequest.encode(handshakeMessage);
        handshakeMessage.writeToRecordStream();
    }

    protected void sendCertificateStatusMessage(CertificateStatus certificateStatus) throws IOException {
        OutputStream handshakeMessage = new HandshakeMessage(this, (short) 22);
        certificateStatus.encode(handshakeMessage);
        handshakeMessage.writeToRecordStream();
    }

    protected void sendNewSessionTicketMessage(NewSessionTicket newSessionTicket) throws IOException {
        if (newSessionTicket != null) {
            OutputStream handshakeMessage = new HandshakeMessage(this, (short) 4);
            newSessionTicket.encode(handshakeMessage);
            handshakeMessage.writeToRecordStream();
            return;
        }
        throw new TlsFatalAlert((short) 80);
    }

    protected void sendServerHelloDoneMessage() throws IOException {
        byte[] bArr = new byte[4];
        TlsUtils.writeUint8((short) 14, bArr, 0);
        TlsUtils.writeUint24(0, bArr, 1);
        writeHandshakeMessage(bArr, 0, bArr.length);
    }

    protected void sendServerHelloMessage() throws IOException {
        OutputStream handshakeMessage = new HandshakeMessage(this, (short) 2);
        ProtocolVersion serverVersion = this.tlsServer.getServerVersion();
        if (serverVersion.isEqualOrEarlierVersionOf(getContext().getClientVersion())) {
            this.recordStream.setReadVersion(serverVersion);
            this.recordStream.setWriteVersion(serverVersion);
            this.recordStream.setRestrictReadVersion(true);
            getContextAdmin().setServerVersion(serverVersion);
            TlsUtils.writeVersion(serverVersion, handshakeMessage);
            handshakeMessage.write(this.securityParameters.serverRandom);
            TlsUtils.writeOpaque8(TlsUtils.EMPTY_BYTES, handshakeMessage);
            int selectedCipherSuite = this.tlsServer.getSelectedCipherSuite();
            if (!Arrays.contains(this.offeredCipherSuites, selectedCipherSuite) || selectedCipherSuite == 0 || CipherSuite.isSCSV(selectedCipherSuite) || !TlsUtils.isValidCipherSuiteForVersion(selectedCipherSuite, getContext().getServerVersion())) {
                throw new TlsFatalAlert((short) 80);
            }
            this.securityParameters.cipherSuite = selectedCipherSuite;
            short selectedCompressionMethod = this.tlsServer.getSelectedCompressionMethod();
            if (Arrays.contains(this.offeredCompressionMethods, selectedCompressionMethod)) {
                this.securityParameters.compressionAlgorithm = selectedCompressionMethod;
                TlsUtils.writeUint16(selectedCipherSuite, handshakeMessage);
                TlsUtils.writeUint8(selectedCompressionMethod, handshakeMessage);
                this.serverExtensions = this.tlsServer.getServerExtensions();
                boolean z = false;
                if (this.secure_renegotiation) {
                    if (TlsUtils.getExtensionData(this.serverExtensions, EXT_RenegotiationInfo) == null) {
                        this.serverExtensions = TlsExtensionsUtils.ensureExtensionsInitialised(this.serverExtensions);
                        this.serverExtensions.put(EXT_RenegotiationInfo, TlsProtocol.createRenegotiationInfo(TlsUtils.EMPTY_BYTES));
                    }
                }
                if (this.securityParameters.extendedMasterSecret) {
                    this.serverExtensions = TlsExtensionsUtils.ensureExtensionsInitialised(this.serverExtensions);
                    TlsExtensionsUtils.addExtendedMasterSecretExtension(this.serverExtensions);
                }
                if (this.serverExtensions != null) {
                    this.securityParameters.encryptThenMAC = TlsExtensionsUtils.hasEncryptThenMACExtension(this.serverExtensions);
                    this.securityParameters.maxFragmentLength = processMaxFragmentLengthExtension(this.clientExtensions, this.serverExtensions, (short) 80);
                    this.securityParameters.truncatedHMac = TlsExtensionsUtils.hasTruncatedHMacExtension(this.serverExtensions);
                    boolean z2 = !this.resumedSession && TlsUtils.hasExpectedEmptyExtensionData(this.serverExtensions, TlsExtensionsUtils.EXT_status_request, (short) 80);
                    this.allowCertificateStatus = z2;
                    if (!this.resumedSession && TlsUtils.hasExpectedEmptyExtensionData(this.serverExtensions, TlsProtocol.EXT_SessionTicket, (short) 80)) {
                        z = true;
                    }
                    this.expectSessionTicket = z;
                    TlsProtocol.writeExtensions(handshakeMessage, this.serverExtensions);
                }
                this.securityParameters.prfAlgorithm = TlsProtocol.getPRFAlgorithm(getContext(), this.securityParameters.getCipherSuite());
                this.securityParameters.verifyDataLength = 12;
                applyMaxFragmentLengthExtension();
                handshakeMessage.writeToRecordStream();
                return;
            }
            throw new TlsFatalAlert((short) 80);
        }
        throw new TlsFatalAlert((short) 80);
    }

    protected void sendServerKeyExchangeMessage(byte[] bArr) throws IOException {
        HandshakeMessage handshakeMessage = new HandshakeMessage((short) 12, bArr.length);
        handshakeMessage.write(bArr);
        handshakeMessage.writeToRecordStream();
    }
}
