package org.bouncycastle.crypto.tls;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.bouncycastle.crypto.tls.SessionParameters.Builder;
import org.bouncycastle.util.Arrays;

public class DTLSClientProtocol extends DTLSProtocol {

    protected static class ClientHandshakeState {
        boolean allowCertificateStatus = false;
        TlsAuthentication authentication = null;
        CertificateRequest certificateRequest = null;
        CertificateStatus certificateStatus = null;
        TlsClient client = null;
        TlsClientContextImpl clientContext = null;
        TlsCredentials clientCredentials = null;
        Hashtable clientExtensions = null;
        boolean expectSessionTicket = false;
        TlsKeyExchange keyExchange = null;
        int[] offeredCipherSuites = null;
        short[] offeredCompressionMethods = null;
        boolean resumedSession = false;
        boolean secure_renegotiation = false;
        byte[] selectedSessionID = null;
        Hashtable serverExtensions = null;
        SessionParameters sessionParameters = null;
        Builder sessionParametersBuilder = null;
        TlsSession tlsSession = null;

        protected ClientHandshakeState() {
        }
    }

    public DTLSClientProtocol(SecureRandom secureRandom) {
        super(secureRandom);
    }

    protected static byte[] patchClientHelloWithCookie(byte[] bArr, byte[] bArr2) throws IOException {
        int readUint8 = 35 + TlsUtils.readUint8(bArr, 34);
        int i = readUint8 + 1;
        byte[] bArr3 = new byte[(bArr.length + bArr2.length)];
        System.arraycopy(bArr, 0, bArr3, 0, readUint8);
        TlsUtils.checkUint8(bArr2.length);
        TlsUtils.writeUint8(bArr2.length, bArr3, readUint8);
        System.arraycopy(bArr2, 0, bArr3, i, bArr2.length);
        System.arraycopy(bArr, i, bArr3, bArr2.length + i, bArr.length - i);
        return bArr3;
    }

    protected void abortClientHandshake(ClientHandshakeState clientHandshakeState, DTLSRecordLayer dTLSRecordLayer, short s) {
        dTLSRecordLayer.fail(s);
        invalidateSession(clientHandshakeState);
    }

    protected DTLSTransport clientHandshake(ClientHandshakeState clientHandshakeState, DTLSRecordLayer dTLSRecordLayer) throws IOException {
        SecurityParameters securityParameters = clientHandshakeState.clientContext.getSecurityParameters();
        DTLSReliableHandshake dTLSReliableHandshake = new DTLSReliableHandshake(clientHandshakeState.clientContext, dTLSRecordLayer);
        byte[] generateClientHello = generateClientHello(clientHandshakeState, clientHandshakeState.client);
        dTLSRecordLayer.setWriteVersion(ProtocolVersion.DTLSv10);
        dTLSReliableHandshake.sendMessage((short) 1, generateClientHello);
        while (true) {
            Message receiveMessage = dTLSReliableHandshake.receiveMessage();
            if (receiveMessage.getType() == (short) 3) {
                if (dTLSRecordLayer.getReadVersion().isEqualOrEarlierVersionOf(clientHandshakeState.clientContext.getClientVersion())) {
                    dTLSRecordLayer.setReadVersion(null);
                    byte[] patchClientHelloWithCookie = patchClientHelloWithCookie(generateClientHello, processHelloVerifyRequest(clientHandshakeState, receiveMessage.getBody()));
                    dTLSReliableHandshake.resetHandshakeMessagesDigest();
                    dTLSReliableHandshake.sendMessage((short) 1, patchClientHelloWithCookie);
                } else {
                    throw new TlsFatalAlert((short) 47);
                }
            } else if (receiveMessage.getType() == (short) 2) {
                ProtocolVersion readVersion = dTLSRecordLayer.getReadVersion();
                reportServerVersion(clientHandshakeState, readVersion);
                dTLSRecordLayer.setWriteVersion(readVersion);
                processServerHello(clientHandshakeState, receiveMessage.getBody());
                dTLSReliableHandshake.notifyHelloComplete();
                DTLSProtocol.applyMaxFragmentLengthExtension(dTLSRecordLayer, securityParameters.maxFragmentLength);
                if (clientHandshakeState.resumedSession) {
                    securityParameters.masterSecret = Arrays.clone(clientHandshakeState.sessionParameters.getMasterSecret());
                    dTLSRecordLayer.initPendingEpoch(clientHandshakeState.client.getCipher());
                    processFinished(dTLSReliableHandshake.receiveMessageBody((short) 20), TlsUtils.calculateVerifyData(clientHandshakeState.clientContext, ExporterLabel.server_finished, TlsProtocol.getCurrentPRFHash(clientHandshakeState.clientContext, dTLSReliableHandshake.getHandshakeHash(), null)));
                    dTLSReliableHandshake.sendMessage((short) 20, TlsUtils.calculateVerifyData(clientHandshakeState.clientContext, ExporterLabel.client_finished, TlsProtocol.getCurrentPRFHash(clientHandshakeState.clientContext, dTLSReliableHandshake.getHandshakeHash(), null)));
                    dTLSReliableHandshake.finish();
                    clientHandshakeState.clientContext.setResumableSession(clientHandshakeState.tlsSession);
                    clientHandshakeState.client.notifyHandshakeComplete();
                    return new DTLSTransport(dTLSRecordLayer);
                }
                Certificate processServerCertificate;
                invalidateSession(clientHandshakeState);
                if (clientHandshakeState.selectedSessionID.length > 0) {
                    clientHandshakeState.tlsSession = new TlsSessionImpl(clientHandshakeState.selectedSessionID, null);
                }
                Message receiveMessage2 = dTLSReliableHandshake.receiveMessage();
                if (receiveMessage2.getType() == (short) 23) {
                    processServerSupplementalData(clientHandshakeState, receiveMessage2.getBody());
                    receiveMessage2 = dTLSReliableHandshake.receiveMessage();
                } else {
                    clientHandshakeState.client.processServerSupplementalData(null);
                }
                clientHandshakeState.keyExchange = clientHandshakeState.client.getKeyExchange();
                clientHandshakeState.keyExchange.init(clientHandshakeState.clientContext);
                if (receiveMessage2.getType() == (short) 11) {
                    processServerCertificate = processServerCertificate(clientHandshakeState, receiveMessage2.getBody());
                    receiveMessage = dTLSReliableHandshake.receiveMessage();
                } else {
                    clientHandshakeState.keyExchange.skipServerCredentials();
                    receiveMessage = receiveMessage2;
                    processServerCertificate = null;
                }
                if (processServerCertificate == null || processServerCertificate.isEmpty()) {
                    clientHandshakeState.allowCertificateStatus = false;
                }
                if (receiveMessage.getType() == (short) 22) {
                    processCertificateStatus(clientHandshakeState, receiveMessage.getBody());
                    receiveMessage = dTLSReliableHandshake.receiveMessage();
                }
                if (receiveMessage.getType() == (short) 12) {
                    processServerKeyExchange(clientHandshakeState, receiveMessage.getBody());
                    receiveMessage = dTLSReliableHandshake.receiveMessage();
                } else {
                    clientHandshakeState.keyExchange.skipServerKeyExchange();
                }
                if (receiveMessage.getType() == (short) 13) {
                    processCertificateRequest(clientHandshakeState, receiveMessage.getBody());
                    TlsUtils.trackHashAlgorithms(dTLSReliableHandshake.getHandshakeHash(), clientHandshakeState.certificateRequest.getSupportedSignatureAlgorithms());
                    receiveMessage = dTLSReliableHandshake.receiveMessage();
                }
                if (receiveMessage.getType() != (short) 14) {
                    throw new TlsFatalAlert((short) 10);
                } else if (receiveMessage.getBody().length == 0) {
                    dTLSReliableHandshake.getHandshakeHash().sealHashAlgorithms();
                    Vector clientSupplementalData = clientHandshakeState.client.getClientSupplementalData();
                    if (clientSupplementalData != null) {
                        dTLSReliableHandshake.sendMessage((short) 23, DTLSProtocol.generateSupplementalData(clientSupplementalData));
                    }
                    if (clientHandshakeState.certificateRequest != null) {
                        clientHandshakeState.clientCredentials = clientHandshakeState.authentication.getClientCredentials(clientHandshakeState.certificateRequest);
                        Certificate certificate = clientHandshakeState.clientCredentials != null ? clientHandshakeState.clientCredentials.getCertificate() : null;
                        if (certificate == null) {
                            certificate = Certificate.EMPTY_CHAIN;
                        }
                        dTLSReliableHandshake.sendMessage((short) 11, DTLSProtocol.generateCertificate(certificate));
                    }
                    if (clientHandshakeState.clientCredentials != null) {
                        clientHandshakeState.keyExchange.processClientCredentials(clientHandshakeState.clientCredentials);
                    } else {
                        clientHandshakeState.keyExchange.skipClientCredentials();
                    }
                    dTLSReliableHandshake.sendMessage((short) 16, generateClientKeyExchange(clientHandshakeState));
                    TlsHandshakeHash prepareToFinish = dTLSReliableHandshake.prepareToFinish();
                    securityParameters.sessionHash = TlsProtocol.getCurrentPRFHash(clientHandshakeState.clientContext, prepareToFinish, null);
                    TlsProtocol.establishMasterSecret(clientHandshakeState.clientContext, clientHandshakeState.keyExchange);
                    dTLSRecordLayer.initPendingEpoch(clientHandshakeState.client.getCipher());
                    if (clientHandshakeState.clientCredentials != null && (clientHandshakeState.clientCredentials instanceof TlsSignerCredentials)) {
                        TlsSignerCredentials tlsSignerCredentials = (TlsSignerCredentials) clientHandshakeState.clientCredentials;
                        SignatureAndHashAlgorithm signatureAndHashAlgorithm = TlsUtils.getSignatureAndHashAlgorithm(clientHandshakeState.clientContext, tlsSignerCredentials);
                        dTLSReliableHandshake.sendMessage((short) 15, generateCertificateVerify(clientHandshakeState, new DigitallySigned(signatureAndHashAlgorithm, tlsSignerCredentials.generateCertificateSignature(signatureAndHashAlgorithm == null ? securityParameters.getSessionHash() : prepareToFinish.getFinalHash(signatureAndHashAlgorithm.getHash())))));
                    }
                    dTLSReliableHandshake.sendMessage((short) 20, TlsUtils.calculateVerifyData(clientHandshakeState.clientContext, ExporterLabel.client_finished, TlsProtocol.getCurrentPRFHash(clientHandshakeState.clientContext, dTLSReliableHandshake.getHandshakeHash(), null)));
                    if (clientHandshakeState.expectSessionTicket) {
                        receiveMessage = dTLSReliableHandshake.receiveMessage();
                        if (receiveMessage.getType() == (short) 4) {
                            processNewSessionTicket(clientHandshakeState, receiveMessage.getBody());
                        } else {
                            throw new TlsFatalAlert((short) 10);
                        }
                    }
                    processFinished(dTLSReliableHandshake.receiveMessageBody((short) 20), TlsUtils.calculateVerifyData(clientHandshakeState.clientContext, ExporterLabel.server_finished, TlsProtocol.getCurrentPRFHash(clientHandshakeState.clientContext, dTLSReliableHandshake.getHandshakeHash(), null)));
                    dTLSReliableHandshake.finish();
                    if (clientHandshakeState.tlsSession != null) {
                        clientHandshakeState.sessionParameters = new Builder().setCipherSuite(securityParameters.getCipherSuite()).setCompressionAlgorithm(securityParameters.getCompressionAlgorithm()).setMasterSecret(securityParameters.getMasterSecret()).setPeerCertificate(processServerCertificate).setPSKIdentity(securityParameters.getPSKIdentity()).setSRPIdentity(securityParameters.getSRPIdentity()).setServerExtensions(clientHandshakeState.serverExtensions).build();
                        clientHandshakeState.tlsSession = TlsUtils.importSession(clientHandshakeState.tlsSession.getSessionID(), clientHandshakeState.sessionParameters);
                        clientHandshakeState.clientContext.setResumableSession(clientHandshakeState.tlsSession);
                    }
                    clientHandshakeState.client.notifyHandshakeComplete();
                    return new DTLSTransport(dTLSRecordLayer);
                } else {
                    throw new TlsFatalAlert((short) 50);
                }
            } else {
                throw new TlsFatalAlert((short) 10);
            }
        }
    }

    public DTLSTransport connect(TlsClient tlsClient, DatagramTransport datagramTransport) throws IOException {
        if (tlsClient == null) {
            throw new IllegalArgumentException("'client' cannot be null");
        } else if (datagramTransport != null) {
            SecurityParameters securityParameters = new SecurityParameters();
            securityParameters.entity = 1;
            ClientHandshakeState clientHandshakeState = new ClientHandshakeState();
            clientHandshakeState.client = tlsClient;
            clientHandshakeState.clientContext = new TlsClientContextImpl(this.secureRandom, securityParameters);
            securityParameters.clientRandom = TlsProtocol.createRandomBlock(tlsClient.shouldUseGMTUnixTime(), clientHandshakeState.clientContext.getNonceRandomGenerator());
            tlsClient.init(clientHandshakeState.clientContext);
            DTLSRecordLayer dTLSRecordLayer = new DTLSRecordLayer(datagramTransport, clientHandshakeState.clientContext, tlsClient, (short) 22);
            TlsSession sessionToResume = clientHandshakeState.client.getSessionToResume();
            if (sessionToResume != null && sessionToResume.isResumable()) {
                SessionParameters exportSessionParameters = sessionToResume.exportSessionParameters();
                if (exportSessionParameters != null) {
                    clientHandshakeState.tlsSession = sessionToResume;
                    clientHandshakeState.sessionParameters = exportSessionParameters;
                }
            }
            try {
                DTLSTransport clientHandshake = clientHandshake(clientHandshakeState, dTLSRecordLayer);
                securityParameters.clear();
                return clientHandshake;
            } catch (TlsFatalAlert e) {
                abortClientHandshake(clientHandshakeState, dTLSRecordLayer, e.getAlertDescription());
                throw e;
            } catch (IOException e2) {
                abortClientHandshake(clientHandshakeState, dTLSRecordLayer, (short) 80);
                throw e2;
            } catch (Throwable e3) {
                abortClientHandshake(clientHandshakeState, dTLSRecordLayer, (short) 80);
                throw new TlsFatalAlert((short) 80, e3);
            } catch (Throwable th) {
                securityParameters.clear();
            }
        } else {
            throw new IllegalArgumentException("'transport' cannot be null");
        }
    }

    protected byte[] generateCertificateVerify(ClientHandshakeState clientHandshakeState, DigitallySigned digitallySigned) throws IOException {
        OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        digitallySigned.encode(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    protected byte[] generateClientHello(ClientHandshakeState clientHandshakeState, TlsClient tlsClient) throws IOException {
        OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ProtocolVersion clientVersion = tlsClient.getClientVersion();
        if (clientVersion.isDTLS()) {
            TlsClientContextImpl tlsClientContextImpl = clientHandshakeState.clientContext;
            tlsClientContextImpl.setClientVersion(clientVersion);
            TlsUtils.writeVersion(clientVersion, byteArrayOutputStream);
            byteArrayOutputStream.write(tlsClientContextImpl.getSecurityParameters().getClientRandom());
            byte[] bArr = TlsUtils.EMPTY_BYTES;
            if (clientHandshakeState.tlsSession != null) {
                bArr = clientHandshakeState.tlsSession.getSessionID();
                if (bArr == null || bArr.length > 32) {
                    bArr = TlsUtils.EMPTY_BYTES;
                }
            }
            TlsUtils.writeOpaque8(bArr, byteArrayOutputStream);
            TlsUtils.writeOpaque8(TlsUtils.EMPTY_BYTES, byteArrayOutputStream);
            boolean isFallback = tlsClient.isFallback();
            clientHandshakeState.offeredCipherSuites = tlsClient.getCipherSuites();
            clientHandshakeState.clientExtensions = tlsClient.getClientExtensions();
            int contains = Arrays.contains(clientHandshakeState.offeredCipherSuites, 255) ^ 1;
            if (!((TlsUtils.getExtensionData(clientHandshakeState.clientExtensions, TlsProtocol.EXT_RenegotiationInfo) == null ? 1 : (short) 0) == 0 || contains == 0)) {
                clientHandshakeState.offeredCipherSuites = Arrays.append(clientHandshakeState.offeredCipherSuites, 255);
            }
            if (isFallback && !Arrays.contains(clientHandshakeState.offeredCipherSuites, (int) CipherSuite.TLS_FALLBACK_SCSV)) {
                clientHandshakeState.offeredCipherSuites = Arrays.append(clientHandshakeState.offeredCipherSuites, (int) CipherSuite.TLS_FALLBACK_SCSV);
            }
            TlsUtils.writeUint16ArrayWithUint16Length(clientHandshakeState.offeredCipherSuites, byteArrayOutputStream);
            clientHandshakeState.offeredCompressionMethods = new short[]{(short) 0};
            TlsUtils.writeUint8ArrayWithUint8Length(clientHandshakeState.offeredCompressionMethods, byteArrayOutputStream);
            if (clientHandshakeState.clientExtensions != null) {
                TlsProtocol.writeExtensions(byteArrayOutputStream, clientHandshakeState.clientExtensions);
            }
            return byteArrayOutputStream.toByteArray();
        }
        throw new TlsFatalAlert((short) 80);
    }

    protected byte[] generateClientKeyExchange(ClientHandshakeState clientHandshakeState) throws IOException {
        OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        clientHandshakeState.keyExchange.generateClientKeyExchange(byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    protected void invalidateSession(ClientHandshakeState clientHandshakeState) {
        if (clientHandshakeState.sessionParameters != null) {
            clientHandshakeState.sessionParameters.clear();
            clientHandshakeState.sessionParameters = null;
        }
        if (clientHandshakeState.tlsSession != null) {
            clientHandshakeState.tlsSession.invalidate();
            clientHandshakeState.tlsSession = null;
        }
    }

    protected void processCertificateRequest(ClientHandshakeState clientHandshakeState, byte[] bArr) throws IOException {
        if (clientHandshakeState.authentication != null) {
            InputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            clientHandshakeState.certificateRequest = CertificateRequest.parse(clientHandshakeState.clientContext, byteArrayInputStream);
            TlsProtocol.assertEmpty(byteArrayInputStream);
            clientHandshakeState.keyExchange.validateCertificateRequest(clientHandshakeState.certificateRequest);
            return;
        }
        throw new TlsFatalAlert((short) 40);
    }

    protected void processCertificateStatus(ClientHandshakeState clientHandshakeState, byte[] bArr) throws IOException {
        if (clientHandshakeState.allowCertificateStatus) {
            InputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            clientHandshakeState.certificateStatus = CertificateStatus.parse(byteArrayInputStream);
            TlsProtocol.assertEmpty(byteArrayInputStream);
            return;
        }
        throw new TlsFatalAlert((short) 10);
    }

    protected byte[] processHelloVerifyRequest(ClientHandshakeState clientHandshakeState, byte[] bArr) throws IOException {
        InputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
        ProtocolVersion readVersion = TlsUtils.readVersion(byteArrayInputStream);
        byte[] readOpaque8 = TlsUtils.readOpaque8(byteArrayInputStream);
        TlsProtocol.assertEmpty(byteArrayInputStream);
        if (!readVersion.isEqualOrEarlierVersionOf(clientHandshakeState.clientContext.getClientVersion())) {
            throw new TlsFatalAlert((short) 47);
        } else if (ProtocolVersion.DTLSv12.isEqualOrEarlierVersionOf(readVersion) || readOpaque8.length <= 32) {
            return readOpaque8;
        } else {
            throw new TlsFatalAlert((short) 47);
        }
    }

    protected void processNewSessionTicket(ClientHandshakeState clientHandshakeState, byte[] bArr) throws IOException {
        InputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
        NewSessionTicket parse = NewSessionTicket.parse(byteArrayInputStream);
        TlsProtocol.assertEmpty(byteArrayInputStream);
        clientHandshakeState.client.notifyNewSessionTicket(parse);
    }

    protected Certificate processServerCertificate(ClientHandshakeState clientHandshakeState, byte[] bArr) throws IOException {
        InputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
        Certificate parse = Certificate.parse(byteArrayInputStream);
        TlsProtocol.assertEmpty(byteArrayInputStream);
        clientHandshakeState.keyExchange.processServerCertificate(parse);
        clientHandshakeState.authentication = clientHandshakeState.client.getAuthentication();
        clientHandshakeState.authentication.notifyServerCertificate(parse);
        return parse;
    }

    protected void processServerHello(ClientHandshakeState clientHandshakeState, byte[] bArr) throws IOException {
        SecurityParameters securityParameters = clientHandshakeState.clientContext.getSecurityParameters();
        InputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
        reportServerVersion(clientHandshakeState, TlsUtils.readVersion(byteArrayInputStream));
        securityParameters.serverRandom = TlsUtils.readFully(32, byteArrayInputStream);
        clientHandshakeState.selectedSessionID = TlsUtils.readOpaque8(byteArrayInputStream);
        if (clientHandshakeState.selectedSessionID.length <= 32) {
            clientHandshakeState.client.notifySessionID(clientHandshakeState.selectedSessionID);
            boolean z = false;
            boolean z2 = clientHandshakeState.selectedSessionID.length > 0 && clientHandshakeState.tlsSession != null && Arrays.areEqual(clientHandshakeState.selectedSessionID, clientHandshakeState.tlsSession.getSessionID());
            clientHandshakeState.resumedSession = z2;
            int readUint16 = TlsUtils.readUint16(byteArrayInputStream);
            if (!Arrays.contains(clientHandshakeState.offeredCipherSuites, readUint16) || readUint16 == 0 || CipherSuite.isSCSV(readUint16) || !TlsUtils.isValidCipherSuiteForVersion(readUint16, clientHandshakeState.clientContext.getServerVersion())) {
                throw new TlsFatalAlert((short) 47);
            }
            DTLSProtocol.validateSelectedCipherSuite(readUint16, (short) 47);
            clientHandshakeState.client.notifySelectedCipherSuite(readUint16);
            short readUint8 = TlsUtils.readUint8(byteArrayInputStream);
            if (Arrays.contains(clientHandshakeState.offeredCompressionMethods, readUint8)) {
                clientHandshakeState.client.notifySelectedCompressionMethod(readUint8);
                clientHandshakeState.serverExtensions = TlsProtocol.readExtensions(byteArrayInputStream);
                if (clientHandshakeState.serverExtensions != null) {
                    Enumeration keys = clientHandshakeState.serverExtensions.keys();
                    while (keys.hasMoreElements()) {
                        Integer num = (Integer) keys.nextElement();
                        if (!num.equals(TlsProtocol.EXT_RenegotiationInfo)) {
                            if (TlsUtils.getExtensionData(clientHandshakeState.clientExtensions, num) != null) {
                                boolean z3 = clientHandshakeState.resumedSession;
                            } else {
                                throw new TlsFatalAlert(AlertDescription.unsupported_extension);
                            }
                        }
                    }
                }
                byte[] extensionData = TlsUtils.getExtensionData(clientHandshakeState.serverExtensions, TlsProtocol.EXT_RenegotiationInfo);
                if (extensionData != null) {
                    clientHandshakeState.secure_renegotiation = true;
                    if (!Arrays.constantTimeAreEqual(extensionData, TlsProtocol.createRenegotiationInfo(TlsUtils.EMPTY_BYTES))) {
                        throw new TlsFatalAlert((short) 40);
                    }
                }
                clientHandshakeState.client.notifySecureRenegotiation(clientHandshakeState.secure_renegotiation);
                Hashtable hashtable = clientHandshakeState.clientExtensions;
                Hashtable hashtable2 = clientHandshakeState.serverExtensions;
                if (clientHandshakeState.resumedSession) {
                    if (readUint16 == clientHandshakeState.sessionParameters.getCipherSuite() && readUint8 == clientHandshakeState.sessionParameters.getCompressionAlgorithm()) {
                        hashtable = null;
                        hashtable2 = clientHandshakeState.sessionParameters.readServerExtensions();
                    } else {
                        throw new TlsFatalAlert((short) 47);
                    }
                }
                securityParameters.cipherSuite = readUint16;
                securityParameters.compressionAlgorithm = readUint8;
                if (hashtable2 != null) {
                    z2 = TlsExtensionsUtils.hasEncryptThenMACExtension(hashtable2);
                    if (!z2 || TlsUtils.isBlockCipherSuite(securityParameters.getCipherSuite())) {
                        securityParameters.encryptThenMAC = z2;
                        securityParameters.extendedMasterSecret = TlsExtensionsUtils.hasExtendedMasterSecretExtension(hashtable2);
                        securityParameters.maxFragmentLength = DTLSProtocol.evaluateMaxFragmentLengthExtension(clientHandshakeState.resumedSession, hashtable, hashtable2, (short) 47);
                        securityParameters.truncatedHMac = TlsExtensionsUtils.hasTruncatedHMacExtension(hashtable2);
                        z2 = !clientHandshakeState.resumedSession && TlsUtils.hasExpectedEmptyExtensionData(hashtable2, TlsExtensionsUtils.EXT_status_request, (short) 47);
                        clientHandshakeState.allowCertificateStatus = z2;
                        if (!clientHandshakeState.resumedSession && TlsUtils.hasExpectedEmptyExtensionData(hashtable2, TlsProtocol.EXT_SessionTicket, (short) 47)) {
                            z = true;
                        }
                        clientHandshakeState.expectSessionTicket = z;
                    } else {
                        throw new TlsFatalAlert((short) 47);
                    }
                }
                if (hashtable != null) {
                    clientHandshakeState.client.processServerExtensions(hashtable2);
                }
                securityParameters.prfAlgorithm = TlsProtocol.getPRFAlgorithm(clientHandshakeState.clientContext, securityParameters.getCipherSuite());
                securityParameters.verifyDataLength = 12;
                return;
            }
            throw new TlsFatalAlert((short) 47);
        }
        throw new TlsFatalAlert((short) 47);
    }

    protected void processServerKeyExchange(ClientHandshakeState clientHandshakeState, byte[] bArr) throws IOException {
        InputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
        clientHandshakeState.keyExchange.processServerKeyExchange(byteArrayInputStream);
        TlsProtocol.assertEmpty(byteArrayInputStream);
    }

    protected void processServerSupplementalData(ClientHandshakeState clientHandshakeState, byte[] bArr) throws IOException {
        clientHandshakeState.client.processServerSupplementalData(TlsProtocol.readSupplementalDataMessage(new ByteArrayInputStream(bArr)));
    }

    protected void reportServerVersion(ClientHandshakeState clientHandshakeState, ProtocolVersion protocolVersion) throws IOException {
        TlsClientContextImpl tlsClientContextImpl = clientHandshakeState.clientContext;
        ProtocolVersion serverVersion = tlsClientContextImpl.getServerVersion();
        if (serverVersion == null) {
            tlsClientContextImpl.setServerVersion(protocolVersion);
            clientHandshakeState.client.notifyServerVersion(protocolVersion);
        } else if (!serverVersion.equals(protocolVersion)) {
            throw new TlsFatalAlert((short) 47);
        }
    }
}
