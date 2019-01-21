package com.android.org.conscrypt;

import com.android.org.conscrypt.ct.CTConstants;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECKey;
import java.security.spec.ECParameterSpec;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

final class ConscryptEngine extends AbstractConscryptEngine implements SSLHandshakeCallbacks, AliasChooser, PSKCallbacks {
    private static final SSLEngineResult CLOSED_NOT_HANDSHAKING = new SSLEngineResult(Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
    private static final ByteBuffer EMPTY = ByteBuffer.allocateDirect(0);
    private static final SSLEngineResult NEED_UNWRAP_CLOSED = new SSLEngineResult(Status.CLOSED, HandshakeStatus.NEED_UNWRAP, 0, 0);
    private static final SSLEngineResult NEED_UNWRAP_OK = new SSLEngineResult(Status.OK, HandshakeStatus.NEED_UNWRAP, 0, 0);
    private static final SSLEngineResult NEED_WRAP_CLOSED = new SSLEngineResult(Status.CLOSED, HandshakeStatus.NEED_WRAP, 0, 0);
    private static final SSLEngineResult NEED_WRAP_OK = new SSLEngineResult(Status.OK, HandshakeStatus.NEED_WRAP, 0, 0);
    private final ActiveSession activeSession;
    private BufferAllocator bufferAllocator;
    private OpenSSLKey channelIdPrivateKey;
    private SessionSnapshot closedSession;
    private final SSLSession externalSession;
    private SSLException handshakeException;
    private boolean handshakeFinished;
    private HandshakeListener handshakeListener;
    private ByteBuffer lazyDirectBuffer;
    private int maxSealOverhead;
    private final BioWrapper networkBio;
    private String peerHostname;
    private final PeerInfoProvider peerInfoProvider;
    private final ByteBuffer[] singleDstBuffer;
    private final ByteBuffer[] singleSrcBuffer;
    private final NativeSsl ssl;
    private final SSLParametersImpl sslParameters;
    private int state;

    ConscryptEngine(SSLParametersImpl sslParameters) {
        this.state = 0;
        this.externalSession = Platform.wrapSSLSession(new ExternalSession(new Provider() {
            public ConscryptSession provideSession() {
                return ConscryptEngine.this.provideSession();
            }
        }));
        this.singleSrcBuffer = new ByteBuffer[1];
        this.singleDstBuffer = new ByteBuffer[1];
        this.sslParameters = sslParameters;
        this.peerInfoProvider = PeerInfoProvider.nullProvider();
        this.ssl = newSsl(sslParameters, this);
        this.networkBio = this.ssl.newBio();
        this.activeSession = new ActiveSession(this.ssl, sslParameters.getSessionContext());
    }

    ConscryptEngine(String host, int port, SSLParametersImpl sslParameters) {
        this.state = 0;
        this.externalSession = Platform.wrapSSLSession(new ExternalSession(/* anonymous class already generated */));
        this.singleSrcBuffer = new ByteBuffer[1];
        this.singleDstBuffer = new ByteBuffer[1];
        this.sslParameters = sslParameters;
        this.peerInfoProvider = PeerInfoProvider.forHostAndPort(host, port);
        this.ssl = newSsl(sslParameters, this);
        this.networkBio = this.ssl.newBio();
        this.activeSession = new ActiveSession(this.ssl, sslParameters.getSessionContext());
    }

    ConscryptEngine(SSLParametersImpl sslParameters, PeerInfoProvider peerInfoProvider) {
        this.state = 0;
        this.externalSession = Platform.wrapSSLSession(new ExternalSession(/* anonymous class already generated */));
        this.singleSrcBuffer = new ByteBuffer[1];
        this.singleDstBuffer = new ByteBuffer[1];
        this.sslParameters = sslParameters;
        this.peerInfoProvider = (PeerInfoProvider) Preconditions.checkNotNull(peerInfoProvider, "peerInfoProvider");
        this.ssl = newSsl(sslParameters, this);
        this.networkBio = this.ssl.newBio();
        this.activeSession = new ActiveSession(this.ssl, sslParameters.getSessionContext());
    }

    private static NativeSsl newSsl(SSLParametersImpl sslParameters, ConscryptEngine engine) {
        try {
            return NativeSsl.newInstance(sslParameters, engine, engine, engine);
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    void setBufferAllocator(BufferAllocator bufferAllocator) {
        synchronized (this.ssl) {
            if (isHandshakeStarted()) {
                throw new IllegalStateException("Could not set buffer allocator after the initial handshake has begun.");
            }
            this.bufferAllocator = bufferAllocator;
        }
    }

    int maxSealOverhead() {
        return this.maxSealOverhead;
    }

    void setChannelIdEnabled(boolean enabled) {
        synchronized (this.ssl) {
            if (getUseClientMode()) {
                throw new IllegalStateException("Not allowed in client mode");
            } else if (isHandshakeStarted()) {
                throw new IllegalStateException("Could not enable/disable Channel ID after the initial handshake has begun.");
            } else {
                this.sslParameters.channelIdEnabled = enabled;
            }
        }
    }

    byte[] getChannelId() throws SSLException {
        byte[] tlsChannelId;
        synchronized (this.ssl) {
            if (getUseClientMode()) {
                throw new IllegalStateException("Not allowed in client mode");
            } else if (isHandshakeStarted()) {
                throw new IllegalStateException("Channel ID is only available after handshake completes");
            } else {
                tlsChannelId = this.ssl.getTlsChannelId();
            }
        }
        return tlsChannelId;
    }

    void setChannelIdPrivateKey(PrivateKey privateKey) {
        if (getUseClientMode()) {
            synchronized (this.ssl) {
                if (isHandshakeStarted()) {
                    throw new IllegalStateException("Could not change Channel ID private key after the initial handshake has begun.");
                } else if (privateKey == null) {
                    this.sslParameters.channelIdEnabled = false;
                    this.channelIdPrivateKey = null;
                    return;
                } else {
                    this.sslParameters.channelIdEnabled = true;
                    ECParameterSpec ecParams = null;
                    try {
                        if (privateKey instanceof ECKey) {
                            ecParams = ((ECKey) privateKey).getParams();
                        }
                        if (ecParams == null) {
                            ecParams = OpenSSLECGroupContext.getCurveByName("prime256v1").getECParameterSpec();
                        }
                        this.channelIdPrivateKey = OpenSSLKey.fromECPrivateKeyForTLSStackOnly(privateKey, ecParams);
                    } catch (InvalidKeyException e) {
                    }
                    return;
                }
            }
        }
        throw new IllegalStateException("Not allowed in server mode");
    }

    void setHandshakeListener(HandshakeListener handshakeListener) {
        synchronized (this.ssl) {
            if (isHandshakeStarted()) {
                throw new IllegalStateException("Handshake listener must be set before starting the handshake.");
            }
            this.handshakeListener = handshakeListener;
        }
    }

    private boolean isHandshakeStarted() {
        switch (this.state) {
            case 0:
            case 1:
                return false;
            default:
                return true;
        }
    }

    void setHostname(String hostname) {
        this.sslParameters.setUseSni(hostname != null);
        this.peerHostname = hostname;
    }

    String getHostname() {
        return this.peerHostname != null ? this.peerHostname : this.peerInfoProvider.getHostname();
    }

    public String getPeerHost() {
        return this.peerHostname != null ? this.peerHostname : this.peerInfoProvider.getHostnameOrIP();
    }

    public int getPeerPort() {
        return this.peerInfoProvider.getPort();
    }

    public void beginHandshake() throws SSLException {
        synchronized (this.ssl) {
            beginHandshakeInternal();
        }
    }

    private void beginHandshakeInternal() throws SSLException {
        int i = this.state;
        switch (i) {
            case 0:
                throw new IllegalStateException("Client/server mode must be set before handshake");
            case 1:
                transitionTo(2);
                boolean releaseResources = true;
                try {
                    this.ssl.initialize(getHostname(), this.channelIdPrivateKey);
                    if (getUseClientMode()) {
                        NativeSslSession cachedSession = clientSessionContext().getCachedSession(getHostname(), getPeerPort(), this.sslParameters);
                        if (cachedSession != null) {
                            cachedSession.offerToResume(this.ssl);
                        }
                    }
                    this.maxSealOverhead = this.ssl.getMaxSealOverhead();
                    handshake();
                    if (false) {
                        closeAndFreeResources();
                    }
                    return;
                } catch (IOException e) {
                    if (e.getMessage().contains("unexpected CCS")) {
                        Platform.logEvent(String.format("ssl_unexpected_ccs: host=%s", new Object[]{getPeerHost()}));
                    }
                    throw SSLUtils.toSSLHandshakeException(e);
                } catch (Throwable th) {
                    if (releaseResources) {
                        closeAndFreeResources();
                    }
                }
            default:
                switch (i) {
                    case 6:
                    case 7:
                    case 8:
                        throw new IllegalStateException("Engine has already been closed");
                    default:
                        return;
                }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x001d, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:15:0x001f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void closeInbound() throws SSLException {
        synchronized (this.ssl) {
            if (this.state != 8) {
                if (this.state != 6) {
                    if (isOutboundDone()) {
                        transitionTo(8);
                    } else {
                        transitionTo(6);
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x002a, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:18:0x002c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void closeOutbound() {
        synchronized (this.ssl) {
            if (this.state != 8) {
                if (this.state != 7) {
                    if (isHandshakeStarted()) {
                        sendSSLShutdown();
                        if (isInboundDone()) {
                            closeAndFreeResources();
                        } else {
                            transitionTo(7);
                        }
                    } else {
                        closeAndFreeResources();
                    }
                }
            }
        }
    }

    public Runnable getDelegatedTask() {
        return null;
    }

    public String[] getEnabledCipherSuites() {
        return this.sslParameters.getEnabledCipherSuites();
    }

    public String[] getEnabledProtocols() {
        return this.sslParameters.getEnabledProtocols();
    }

    public boolean getEnableSessionCreation() {
        return this.sslParameters.getEnableSessionCreation();
    }

    public SSLParameters getSSLParameters() {
        SSLParameters params = super.getSSLParameters();
        Platform.getSSLParameters(params, this.sslParameters, this);
        return params;
    }

    public void setSSLParameters(SSLParameters p) {
        super.setSSLParameters(p);
        Platform.setSSLParameters(p, this.sslParameters, this);
    }

    public HandshakeStatus getHandshakeStatus() {
        HandshakeStatus handshakeStatusInternal;
        synchronized (this.ssl) {
            handshakeStatusInternal = getHandshakeStatusInternal();
        }
        return handshakeStatusInternal;
    }

    private HandshakeStatus getHandshakeStatusInternal() {
        if (this.handshakeFinished) {
            return HandshakeStatus.NOT_HANDSHAKING;
        }
        switch (this.state) {
            case 0:
            case 1:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                return HandshakeStatus.NOT_HANDSHAKING;
            case 2:
                return pendingStatus(pendingOutboundEncryptedBytes());
            case CTConstants.CERTIFICATE_LENGTH_BYTES /*3*/:
                return HandshakeStatus.NEED_WRAP;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected engine state: ");
                stringBuilder.append(this.state);
                throw new IllegalStateException(stringBuilder.toString());
        }
    }

    private int pendingOutboundEncryptedBytes() {
        return this.networkBio.getPendingWrittenBytes();
    }

    private int pendingInboundCleartextBytes() {
        return this.ssl.getPendingReadableBytes();
    }

    private static HandshakeStatus pendingStatus(int pendingOutboundBytes) {
        return pendingOutboundBytes > 0 ? HandshakeStatus.NEED_WRAP : HandshakeStatus.NEED_UNWRAP;
    }

    public boolean getNeedClientAuth() {
        return this.sslParameters.getNeedClientAuth();
    }

    SSLSession handshakeSession() {
        synchronized (this.ssl) {
            if (this.state == 2) {
                SSLSession wrapSSLSession = Platform.wrapSSLSession(new ExternalSession(new Provider() {
                    public ConscryptSession provideSession() {
                        return ConscryptEngine.this.provideHandshakeSession();
                    }
                }));
                return wrapSSLSession;
            }
            return null;
        }
    }

    public SSLSession getSession() {
        return this.externalSession;
    }

    /* JADX WARNING: Missing block: B:10:0x0015, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ConscryptSession provideSession() {
        synchronized (this.ssl) {
            ConscryptSession nullSession;
            if (this.state == 8) {
                nullSession = this.closedSession != null ? this.closedSession : SSLNullSession.getNullSession();
            } else if (this.state < 3) {
                nullSession = SSLNullSession.getNullSession();
                return nullSession;
            } else {
                ActiveSession activeSession = this.activeSession;
                return activeSession;
            }
        }
    }

    private ConscryptSession provideHandshakeSession() {
        ConscryptSession conscryptSession;
        synchronized (this.ssl) {
            if (this.state == 2) {
                conscryptSession = this.activeSession;
            } else {
                conscryptSession = SSLNullSession.getNullSession();
            }
        }
        return conscryptSession;
    }

    public String[] getSupportedCipherSuites() {
        return NativeCrypto.getSupportedCipherSuites();
    }

    public String[] getSupportedProtocols() {
        return NativeCrypto.getSupportedProtocols();
    }

    public boolean getUseClientMode() {
        return this.sslParameters.getUseClientMode();
    }

    public boolean getWantClientAuth() {
        return this.sslParameters.getWantClientAuth();
    }

    public boolean isInboundDone() {
        boolean z;
        synchronized (this.ssl) {
            if (!(this.state == 8 || this.state == 6)) {
                if (!this.ssl.wasShutdownReceived()) {
                    z = false;
                }
            }
            z = true;
        }
        return z;
    }

    public boolean isOutboundDone() {
        boolean z;
        synchronized (this.ssl) {
            if (!(this.state == 8 || this.state == 7)) {
                if (!this.ssl.wasShutdownSent()) {
                    z = false;
                }
            }
            z = true;
        }
        return z;
    }

    public void setEnabledCipherSuites(String[] suites) {
        this.sslParameters.setEnabledCipherSuites(suites);
    }

    public void setEnabledProtocols(String[] protocols) {
        this.sslParameters.setEnabledProtocols(protocols);
    }

    public void setEnableSessionCreation(boolean flag) {
        this.sslParameters.setEnableSessionCreation(flag);
    }

    public void setNeedClientAuth(boolean need) {
        this.sslParameters.setNeedClientAuth(need);
    }

    public void setUseClientMode(boolean mode) {
        synchronized (this.ssl) {
            if (isHandshakeStarted()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can not change mode after handshake: state == ");
                stringBuilder.append(this.state);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            transitionTo(1);
            this.sslParameters.setUseClientMode(mode);
        }
    }

    public void setWantClientAuth(boolean want) {
        this.sslParameters.setWantClientAuth(want);
    }

    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
        SSLEngineResult unwrap;
        synchronized (this.ssl) {
            try {
                unwrap = unwrap(singleSrcBuffer(src), singleDstBuffer(dst));
                resetSingleSrcBuffer();
                resetSingleDstBuffer();
            } catch (Throwable th) {
                resetSingleSrcBuffer();
                resetSingleDstBuffer();
            }
        }
        return unwrap;
    }

    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts) throws SSLException {
        SSLEngineResult unwrap;
        synchronized (this.ssl) {
            try {
                unwrap = unwrap(singleSrcBuffer(src), dsts);
                resetSingleSrcBuffer();
            } catch (Throwable th) {
                resetSingleSrcBuffer();
            }
        }
        return unwrap;
    }

    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws SSLException {
        SSLEngineResult unwrap;
        synchronized (this.ssl) {
            try {
                unwrap = unwrap(singleSrcBuffer(src), 0, 1, dsts, offset, length);
                resetSingleSrcBuffer();
            } catch (Throwable th) {
                resetSingleSrcBuffer();
            }
        }
        return unwrap;
    }

    SSLEngineResult unwrap(ByteBuffer[] srcs, ByteBuffer[] dsts) throws SSLException {
        boolean z = false;
        Preconditions.checkArgument(srcs != null, "srcs is null");
        if (dsts != null) {
            z = true;
        }
        Preconditions.checkArgument(z, "dsts is null");
        return unwrap(srcs, 0, srcs.length, dsts, 0, dsts.length);
    }

    /* JADX WARNING: Exception block dominator not found, dom blocks: [B:126:0x016e, B:143:0x018e] */
    /* JADX WARNING: Missing block: B:159:0x01b8, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:162:0x01c1, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:169:0x01d1, code skipped:
            r0 = e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    SSLEngineResult unwrap(ByteBuffer[] srcs, int srcsOffset, int srcsLength, ByteBuffer[] dsts, int dstsOffset, int dstsLength) throws SSLException {
        long j;
        SSLException e;
        int i;
        SSLEngineResult sSLEngineResult;
        EOFException e2;
        IOException e3;
        ByteBuffer[] byteBufferArr = srcs;
        int lenRemaining = srcsOffset;
        ByteBuffer[] byteBufferArr2 = dsts;
        int i2 = dstsOffset;
        Preconditions.checkArgument(byteBufferArr != null, "srcs is null");
        Preconditions.checkArgument(byteBufferArr2 != null, "dsts is null");
        Preconditions.checkPositionIndexes(lenRemaining, lenRemaining + srcsLength, byteBufferArr.length);
        Preconditions.checkPositionIndexes(i2, i2 + dstsLength, byteBufferArr2.length);
        int dstLength = calcDstsLength(dsts, dstsOffset, dstsLength);
        int endOffset = i2 + dstsLength;
        int srcsEndOffset = lenRemaining + srcsLength;
        long srcLength = calcSrcsLength(byteBufferArr, lenRemaining, srcsEndOffset);
        synchronized (this.ssl) {
            Throwable th;
            try {
                int i3 = this.state;
                SSLEngineResult sSLEngineResult2;
                if (i3 == 6 || i3 == 8) {
                    sSLEngineResult2 = new SSLEngineResult(Status.CLOSED, getHandshakeStatusInternal(), 0, 0);
                    return sSLEngineResult2;
                }
                int lenRemaining2;
                int remaining;
                switch (i3) {
                    case 0:
                        throw new IllegalStateException("Client/server mode must be set before calling unwrap");
                    case 1:
                        beginHandshakeInternal();
                        break;
                    default:
                        break;
                }
                HandshakeStatus handshakeStatus = HandshakeStatus.NOT_HANDSHAKING;
                if (!this.handshakeFinished) {
                    try {
                        handshakeStatus = handshake();
                        if (handshakeStatus == HandshakeStatus.NEED_WRAP) {
                            sSLEngineResult2 = NEED_WRAP_OK;
                            return sSLEngineResult2;
                        } else if (this.state == 8) {
                            sSLEngineResult2 = NEED_WRAP_CLOSED;
                            return sSLEngineResult2;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        i2 = lenRemaining;
                        j = srcLength;
                    }
                }
                boolean noCleartextDataAvailable = pendingInboundCleartextBytes() <= 0;
                int lenRemaining3;
                if (srcLength <= 0 || !noCleartextDataAvailable) {
                    lenRemaining3 = 0;
                    j = srcLength;
                    if (noCleartextDataAvailable) {
                        sSLEngineResult2 = new SSLEngineResult(Status.BUFFER_UNDERFLOW, getHandshakeStatus(), 0, 0);
                        return sSLEngineResult2;
                    }
                    lenRemaining2 = lenRemaining3;
                } else if (srcLength < 5) {
                    lenRemaining3 = 0;
                    lenRemaining2 = new SSLEngineResult(Status.BUFFER_UNDERFLOW, getHandshakeStatus(), 0, 0);
                    return lenRemaining2;
                } else {
                    lenRemaining3 = 0;
                    lenRemaining2 = SSLUtils.getEncryptedPacketLength(srcs, srcsOffset);
                    if (lenRemaining2 < 0) {
                        throw new SSLException("Unable to parse TLS packet header");
                    } else if (srcLength < ((long) lenRemaining2)) {
                        try {
                            SSLEngineResult sSLEngineResult3 = new SSLEngineResult(Status.BUFFER_UNDERFLOW, getHandshakeStatus(), 0, 0);
                            return sSLEngineResult3;
                        } catch (Throwable th3) {
                            th = th3;
                            i2 = lenRemaining;
                        }
                    }
                }
                int bytesConsumed = 0;
                if (lenRemaining2 > 0 && lenRemaining < srcsEndOffset) {
                    do {
                        ByteBuffer src = byteBufferArr[lenRemaining];
                        remaining = src.remaining();
                        if (remaining == 0) {
                            lenRemaining++;
                            continue;
                        } else {
                            srcLength = writeEncryptedData(src, Math.min(lenRemaining2, remaining));
                            if (srcLength > null) {
                                bytesConsumed += srcLength;
                                lenRemaining2 -= srcLength;
                                if (lenRemaining2 != 0) {
                                    if (srcLength == remaining) {
                                        lenRemaining++;
                                        continue;
                                    }
                                }
                            } else {
                                NativeCrypto.SSL_clear_error();
                            }
                        }
                    } while (lenRemaining < srcsEndOffset);
                }
                i2 = lenRemaining;
                lenRemaining = lenRemaining2;
                remaining = 0;
                if (dstLength > 0) {
                    lenRemaining2 = dstsOffset;
                    while (lenRemaining2 < endOffset) {
                        try {
                            srcLength = byteBufferArr2[lenRemaining2];
                            lenRemaining2++;
                        } catch (SSLException e4) {
                            e = e4;
                            i = lenRemaining;
                            if (pendingOutboundEncryptedBytes() > 0) {
                                if (!this.handshakeFinished && this.handshakeException == null) {
                                    this.handshakeException = e;
                                }
                                sSLEngineResult = new SSLEngineResult(Status.OK, HandshakeStatus.NEED_WRAP, bytesConsumed, remaining);
                                return sSLEngineResult;
                            }
                            sendSSLShutdown();
                            throw convertException(e);
                        } catch (InterruptedIOException e5) {
                            i = lenRemaining;
                            sSLEngineResult = newResult(bytesConsumed, remaining, handshakeStatus);
                            return sSLEngineResult;
                        } catch (EOFException e6) {
                            e2 = e6;
                            i = lenRemaining;
                            closeAll();
                            throw convertException(e2);
                        } catch (IOException e7) {
                            e3 = e7;
                            i = lenRemaining;
                            sendSSLShutdown();
                            throw convertException(e3);
                        }
                        if (srcLength.hasRemaining()) {
                            int bytesRead = readPlaintextData(srcLength);
                            if (bytesRead > 0) {
                                remaining += bytesRead;
                                try {
                                    if (srcLength.hasRemaining()) {
                                        i = lenRemaining;
                                    }
                                } catch (SSLException e8) {
                                    e = e8;
                                    i = lenRemaining;
                                } catch (InterruptedIOException e9) {
                                    i = lenRemaining;
                                    sSLEngineResult = newResult(bytesConsumed, remaining, handshakeStatus);
                                    return sSLEngineResult;
                                } catch (EOFException e10) {
                                    e2 = e10;
                                    i = lenRemaining;
                                    closeAll();
                                    throw convertException(e2);
                                } catch (IOException e11) {
                                    e3 = e11;
                                    i = lenRemaining;
                                    sendSSLShutdown();
                                    throw convertException(e3);
                                }
                            }
                            if (bytesRead != -6) {
                                switch (bytesRead) {
                                    case -3:
                                    case -2:
                                        sSLEngineResult2 = newResult(bytesConsumed, remaining, handshakeStatus);
                                        return sSLEngineResult2;
                                    default:
                                        sendSSLShutdown();
                                        throw newSslExceptionWithMessage("SSL_read");
                                }
                                th = th;
                                throw th;
                            }
                            closeInbound();
                            sendSSLShutdown();
                            sSLEngineResult2 = new SSLEngineResult(Status.CLOSED, pendingOutboundEncryptedBytes() > 0 ? HandshakeStatus.NEED_WRAP : HandshakeStatus.NOT_HANDSHAKING, bytesConsumed, remaining);
                            return sSLEngineResult2;
                        }
                    }
                } else {
                    readPlaintextData(EMPTY);
                }
                if ((this.handshakeFinished ? pendingInboundCleartextBytes() : 0) > 0) {
                    Status status = Status.BUFFER_OVERFLOW;
                    if (handshakeStatus == HandshakeStatus.FINISHED) {
                        srcLength = handshakeStatus;
                    } else {
                        srcLength = getHandshakeStatusInternal();
                    }
                    sSLEngineResult = new SSLEngineResult(status, mayFinishHandshake(srcLength), bytesConsumed, remaining);
                    return sSLEngineResult;
                }
                sSLEngineResult = newResult(bytesConsumed, remaining, handshakeStatus);
                return sSLEngineResult;
            } catch (Throwable th4) {
                th = th4;
                j = srcLength;
                i2 = lenRemaining;
            }
        }
    }

    private static int calcDstsLength(ByteBuffer[] dsts, int dstsOffset, int dstsLength) {
        int capacity = 0;
        int i = 0;
        while (i < dsts.length) {
            ByteBuffer dst = dsts[i];
            Preconditions.checkArgument(dst != null, "dsts[%d] is null", Integer.valueOf(i));
            if (dst.isReadOnly()) {
                throw new ReadOnlyBufferException();
            }
            if (i >= dstsOffset && i < dstsOffset + dstsLength) {
                capacity += dst.remaining();
            }
            i++;
        }
        return capacity;
    }

    private static long calcSrcsLength(ByteBuffer[] srcs, int srcsOffset, int srcsEndOffset) {
        long len = 0;
        int i = srcsOffset;
        while (i < srcsEndOffset) {
            ByteBuffer src = srcs[i];
            if (src != null) {
                len += (long) src.remaining();
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("srcs[");
                stringBuilder.append(i);
                stringBuilder.append("] is null");
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        return len;
    }

    private HandshakeStatus handshake() throws SSLException {
        try {
            if (this.handshakeException == null) {
                switch (this.ssl.doHandshake()) {
                    case 2:
                        return pendingStatus(pendingOutboundEncryptedBytes());
                    case CTConstants.CERTIFICATE_LENGTH_BYTES /*3*/:
                        return HandshakeStatus.NEED_WRAP;
                    default:
                        this.activeSession.onPeerCertificateAvailable(getPeerHost(), getPeerPort());
                        finishHandshake();
                        return HandshakeStatus.FINISHED;
                }
                throw SSLUtils.toSSLHandshakeException(e);
            } else if (pendingOutboundEncryptedBytes() > 0) {
                return HandshakeStatus.NEED_WRAP;
            } else {
                SSLException e = this.handshakeException;
                this.handshakeException = null;
                throw e;
            }
        } catch (SSLException e2) {
            if (pendingOutboundEncryptedBytes() > 0) {
                this.handshakeException = e2;
                return HandshakeStatus.NEED_WRAP;
            }
            sendSSLShutdown();
            throw e2;
        } catch (IOException e22) {
            sendSSLShutdown();
            throw e22;
        } catch (Exception e222) {
            throw SSLUtils.toSSLHandshakeException(e222);
        }
    }

    private void finishHandshake() throws SSLException {
        this.handshakeFinished = true;
        if (this.handshakeListener != null) {
            this.handshakeListener.onHandshakeFinished();
        }
    }

    private int writePlaintextData(ByteBuffer src, int len) throws SSLException {
        try {
            int sslWrote;
            int pos = src.position();
            if (src.isDirect()) {
                sslWrote = writePlaintextDataDirect(src, pos, len);
            } else {
                sslWrote = writePlaintextDataHeap(src, pos, len);
            }
            if (sslWrote > 0) {
                src.position(pos + sslWrote);
            }
            return sslWrote;
        } catch (Exception e) {
            throw convertException(e);
        }
    }

    private int writePlaintextDataDirect(ByteBuffer src, int pos, int len) throws IOException {
        return this.ssl.writeDirectByteBuffer(directByteBufferAddress(src, pos), len);
    }

    private int writePlaintextDataHeap(ByteBuffer src, int pos, int len) throws IOException {
        AllocatedBuffer allocatedBuffer = null;
        try {
            ByteBuffer buffer;
            if (this.bufferAllocator != null) {
                allocatedBuffer = this.bufferAllocator.allocateDirectBuffer(len);
                buffer = allocatedBuffer.nioBuffer();
            } else {
                buffer = getOrCreateLazyDirectBuffer();
            }
            int limit = src.limit();
            int bytesToWrite = Math.min(len, buffer.remaining());
            src.limit(pos + bytesToWrite);
            buffer.put(src);
            buffer.flip();
            src.limit(limit);
            src.position(pos);
            int writePlaintextDataDirect = writePlaintextDataDirect(buffer, 0, bytesToWrite);
            return writePlaintextDataDirect;
        } finally {
            if (allocatedBuffer != null) {
                allocatedBuffer.release();
            }
        }
    }

    private int readPlaintextData(ByteBuffer dst) throws IOException {
        try {
            int pos = dst.position();
            int len = Math.min(16709, dst.limit() - pos);
            if (!dst.isDirect()) {
                return readPlaintextDataHeap(dst, len);
            }
            int bytesRead = readPlaintextDataDirect(dst, pos, len);
            if (bytesRead > 0) {
                dst.position(pos + bytesRead);
            }
            return bytesRead;
        } catch (CertificateException e) {
            throw convertException(e);
        }
    }

    private int readPlaintextDataDirect(ByteBuffer dst, int pos, int len) throws IOException, CertificateException {
        return this.ssl.readDirectByteBuffer(directByteBufferAddress(dst, pos), len);
    }

    private int readPlaintextDataHeap(ByteBuffer dst, int len) throws IOException, CertificateException {
        AllocatedBuffer allocatedBuffer = null;
        try {
            ByteBuffer buffer;
            if (this.bufferAllocator != null) {
                allocatedBuffer = this.bufferAllocator.allocateDirectBuffer(len);
                buffer = allocatedBuffer.nioBuffer();
            } else {
                buffer = getOrCreateLazyDirectBuffer();
            }
            int bytesRead = readPlaintextDataDirect(buffer, 0, Math.min(len, buffer.remaining()));
            if (bytesRead > 0) {
                buffer.position(bytesRead);
                buffer.flip();
                dst.put(buffer);
            }
            if (allocatedBuffer != null) {
                allocatedBuffer.release();
            }
            return bytesRead;
        } catch (Throwable th) {
            if (allocatedBuffer != null) {
                allocatedBuffer.release();
            }
        }
    }

    private SSLException convertException(Throwable e) {
        if ((e instanceof SSLHandshakeException) || !this.handshakeFinished) {
            return SSLUtils.toSSLHandshakeException(e);
        }
        return SSLUtils.toSSLException(e);
    }

    private int writeEncryptedData(ByteBuffer src, int len) throws SSLException {
        try {
            int bytesWritten;
            int pos = src.position();
            if (src.isDirect()) {
                bytesWritten = writeEncryptedDataDirect(src, pos, len);
            } else {
                bytesWritten = writeEncryptedDataHeap(src, pos, len);
            }
            if (bytesWritten > 0) {
                src.position(pos + bytesWritten);
            }
            return bytesWritten;
        } catch (IOException e) {
            throw new SSLException(e);
        }
    }

    private int writeEncryptedDataDirect(ByteBuffer src, int pos, int len) throws IOException {
        return this.networkBio.writeDirectByteBuffer(directByteBufferAddress(src, pos), len);
    }

    private int writeEncryptedDataHeap(ByteBuffer src, int pos, int len) throws IOException {
        AllocatedBuffer allocatedBuffer = null;
        try {
            ByteBuffer buffer;
            if (this.bufferAllocator != null) {
                allocatedBuffer = this.bufferAllocator.allocateDirectBuffer(len);
                buffer = allocatedBuffer.nioBuffer();
            } else {
                buffer = getOrCreateLazyDirectBuffer();
            }
            int limit = src.limit();
            int bytesToCopy = Math.min(Math.min(limit - pos, len), buffer.remaining());
            src.limit(pos + bytesToCopy);
            buffer.put(src);
            src.limit(limit);
            src.position(pos);
            int bytesWritten = writeEncryptedDataDirect(buffer, 0, bytesToCopy);
            src.position(pos);
            return bytesWritten;
        } finally {
            if (allocatedBuffer != null) {
                allocatedBuffer.release();
            }
        }
    }

    private ByteBuffer getOrCreateLazyDirectBuffer() {
        if (this.lazyDirectBuffer == null) {
            this.lazyDirectBuffer = ByteBuffer.allocateDirect(Math.max(16384, 16709));
        }
        this.lazyDirectBuffer.clear();
        return this.lazyDirectBuffer;
    }

    private long directByteBufferAddress(ByteBuffer directBuffer, int pos) {
        return NativeCrypto.getDirectBufferAddress(directBuffer) + ((long) pos);
    }

    private SSLEngineResult readPendingBytesFromBIO(ByteBuffer dst, int bytesConsumed, int bytesProduced, HandshakeStatus status) throws SSLException {
        try {
            int pendingNet = pendingOutboundEncryptedBytes();
            if (pendingNet <= 0) {
                return null;
            }
            if (dst.remaining() < pendingNet) {
                HandshakeStatus handshakeStatus;
                Status status2 = Status.BUFFER_OVERFLOW;
                if (status == HandshakeStatus.FINISHED) {
                    handshakeStatus = status;
                } else {
                    handshakeStatus = getHandshakeStatus(pendingNet);
                }
                return new SSLEngineResult(status2, mayFinishHandshake(handshakeStatus), bytesConsumed, bytesProduced);
            }
            HandshakeStatus handshakeStatus2;
            int produced = readEncryptedData(dst, pendingNet);
            if (produced <= 0) {
                NativeCrypto.SSL_clear_error();
            } else {
                bytesProduced += produced;
                pendingNet -= produced;
            }
            Status engineStatus = getEngineStatus();
            if (status == HandshakeStatus.FINISHED) {
                handshakeStatus2 = status;
            } else {
                handshakeStatus2 = getHandshakeStatus(pendingNet);
            }
            return new SSLEngineResult(engineStatus, mayFinishHandshake(handshakeStatus2), bytesConsumed, bytesProduced);
        } catch (Exception e) {
            throw convertException(e);
        }
    }

    private int readEncryptedData(ByteBuffer dst, int pending) throws SSLException {
        try {
            int pos = dst.position();
            if (dst.remaining() < pending) {
                return 0;
            }
            int len = Math.min(pending, dst.limit() - pos);
            if (!dst.isDirect()) {
                return readEncryptedDataHeap(dst, len);
            }
            int bytesRead = readEncryptedDataDirect(dst, pos, len);
            if (bytesRead <= 0) {
                return bytesRead;
            }
            dst.position(pos + bytesRead);
            return bytesRead;
        } catch (Exception e) {
            throw convertException(e);
        }
    }

    private int readEncryptedDataDirect(ByteBuffer dst, int pos, int len) throws IOException {
        return this.networkBio.readDirectByteBuffer(directByteBufferAddress(dst, pos), len);
    }

    private int readEncryptedDataHeap(ByteBuffer dst, int len) throws IOException {
        AllocatedBuffer allocatedBuffer = null;
        try {
            ByteBuffer buffer;
            if (this.bufferAllocator != null) {
                allocatedBuffer = this.bufferAllocator.allocateDirectBuffer(len);
                buffer = allocatedBuffer.nioBuffer();
            } else {
                buffer = getOrCreateLazyDirectBuffer();
            }
            int bytesRead = readEncryptedDataDirect(buffer, 0, Math.min(len, buffer.remaining()));
            if (bytesRead > 0) {
                buffer.position(bytesRead);
                buffer.flip();
                dst.put(buffer);
            }
            if (allocatedBuffer != null) {
                allocatedBuffer.release();
            }
            return bytesRead;
        } catch (Throwable th) {
            if (allocatedBuffer != null) {
                allocatedBuffer.release();
            }
        }
    }

    private HandshakeStatus mayFinishHandshake(HandshakeStatus status) throws SSLException {
        if (this.handshakeFinished || status != HandshakeStatus.NOT_HANDSHAKING) {
            return status;
        }
        return handshake();
    }

    private HandshakeStatus getHandshakeStatus(int pending) {
        return !this.handshakeFinished ? pendingStatus(pending) : HandshakeStatus.NOT_HANDSHAKING;
    }

    private Status getEngineStatus() {
        switch (this.state) {
            case 6:
            case 7:
            case 8:
                return Status.CLOSED;
            default:
                return Status.OK;
        }
    }

    private void closeAll() throws SSLException {
        closeOutbound();
        closeInbound();
    }

    private SSLException newSslExceptionWithMessage(String err) {
        if (this.handshakeFinished) {
            return new SSLHandshakeException(err);
        }
        return new SSLException(err);
    }

    private SSLEngineResult newResult(int bytesConsumed, int bytesProduced, HandshakeStatus status) throws SSLException {
        return new SSLEngineResult(getEngineStatus(), mayFinishHandshake(status == HandshakeStatus.FINISHED ? status : getHandshakeStatusInternal()), bytesConsumed, bytesProduced);
    }

    public SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
        SSLEngineResult wrap;
        synchronized (this.ssl) {
            try {
                wrap = wrap(singleSrcBuffer(src), dst);
                resetSingleSrcBuffer();
            } catch (Throwable th) {
                resetSingleSrcBuffer();
            }
        }
        return wrap;
    }

    /* JADX WARNING: Removed duplicated region for block: B:104:0x016f  */
    /* JADX WARNING: Missing block: B:89:0x0134, code skipped:
            return r11;
     */
    /* JADX WARNING: Missing block: B:95:0x0150, code skipped:
            return r11;
     */
    /* JADX WARNING: Missing block: B:101:0x0162, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SSLEngineResult wrap(ByteBuffer[] srcs, int srcsOffset, int srcsLength, ByteBuffer dst) throws SSLException {
        ByteBuffer[] byteBufferArr = srcs;
        int i = srcsOffset;
        ByteBuffer byteBuffer = dst;
        boolean z = false;
        Preconditions.checkArgument(byteBufferArr != null, "srcs is null");
        Preconditions.checkArgument(byteBuffer != null, "dst is null");
        Preconditions.checkPositionIndexes(i, i + srcsLength, byteBufferArr.length);
        if (dst.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        synchronized (this.ssl) {
            SSLEngineResult pendingNetResult;
            switch (this.state) {
                case 0:
                    throw new IllegalStateException("Client/server mode must be set before calling wrap");
                case 1:
                    beginHandshakeInternal();
                    break;
                case 7:
                case 8:
                    pendingNetResult = readPendingBytesFromBIO(byteBuffer, 0, 0, HandshakeStatus.NOT_HANDSHAKING);
                    if (pendingNetResult != null) {
                        return pendingNetResult;
                    }
                    SSLEngineResult sSLEngineResult = new SSLEngineResult(Status.CLOSED, getHandshakeStatusInternal(), 0, 0);
                    return sSLEngineResult;
                default:
                    break;
            }
            HandshakeStatus handshakeStatus = HandshakeStatus.NOT_HANDSHAKING;
            if (!this.handshakeFinished) {
                handshakeStatus = handshake();
                if (handshakeStatus == HandshakeStatus.NEED_UNWRAP) {
                    pendingNetResult = NEED_UNWRAP_OK;
                    return pendingNetResult;
                } else if (this.state == 8) {
                    pendingNetResult = NEED_UNWRAP_CLOSED;
                    return pendingNetResult;
                }
            }
            int endOffset = i + srcsLength;
            int srcsLen = 0;
            int i2 = i;
            while (i2 < endOffset) {
                ByteBuffer src = byteBufferArr[i2];
                if (src != null) {
                    if (srcsLen != 16384) {
                        srcsLen += src.remaining();
                        if (srcsLen > 16384 || srcsLen < 0) {
                            srcsLen = 16384;
                        }
                    }
                    i2++;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("srcs[");
                    stringBuilder.append(i2);
                    stringBuilder.append("] is null");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            if (dst.remaining() < SSLUtils.calculateOutNetBufSize(srcsLen)) {
                pendingNetResult = new SSLEngineResult(Status.BUFFER_OVERFLOW, getHandshakeStatusInternal(), 0, 0);
                return pendingNetResult;
            }
            int bytesConsumed = 0;
            int bytesProduced = 0;
            i2 = i;
            while (i2 < endOffset) {
                ByteBuffer src2 = byteBufferArr[i2];
                Preconditions.checkArgument(src2 != null ? true : z, "srcs[%d] is null", Integer.valueOf(i2));
                while (src2.hasRemaining()) {
                    int result = writePlaintextData(src2, Math.min(src2.remaining(), 16384 - bytesConsumed));
                    SSLEngineResult pendingNetResult2;
                    if (result > 0) {
                        bytesConsumed += result;
                        pendingNetResult2 = readPendingBytesFromBIO(byteBuffer, bytesConsumed, bytesProduced, handshakeStatus);
                        if (pendingNetResult2 != null) {
                            if (pendingNetResult2.getStatus() != Status.OK) {
                                return pendingNetResult2;
                            }
                            bytesProduced = pendingNetResult2.bytesProduced();
                        }
                        if (bytesConsumed == 16384) {
                            if (bytesConsumed == 0) {
                                pendingNetResult = readPendingBytesFromBIO(byteBuffer, 0, bytesProduced, handshakeStatus);
                                if (pendingNetResult != null) {
                                    return pendingNetResult;
                                }
                            }
                            pendingNetResult = newResult(bytesConsumed, bytesProduced, handshakeStatus);
                            return pendingNetResult;
                        }
                        byteBufferArr = srcs;
                    } else {
                        int sslError = this.ssl.getError(result);
                        int i3;
                        if (sslError != 6) {
                            SSLEngineResult sSLEngineResult2;
                            switch (sslError) {
                                case 2:
                                    pendingNetResult2 = readPendingBytesFromBIO(byteBuffer, bytesConsumed, bytesProduced, handshakeStatus);
                                    if (pendingNetResult2 == null) {
                                        sSLEngineResult2 = new SSLEngineResult(getEngineStatus(), HandshakeStatus.NEED_UNWRAP, bytesConsumed, bytesProduced);
                                        break;
                                    }
                                    int i4 = result;
                                    i3 = sslError;
                                    sSLEngineResult2 = pendingNetResult2;
                                    break;
                                case CTConstants.CERTIFICATE_LENGTH_BYTES /*3*/:
                                    pendingNetResult2 = readPendingBytesFromBIO(byteBuffer, bytesConsumed, bytesProduced, handshakeStatus);
                                    if (pendingNetResult2 == null) {
                                        sSLEngineResult2 = NEED_WRAP_CLOSED;
                                        break;
                                    }
                                    sSLEngineResult2 = pendingNetResult2;
                                    break;
                                default:
                                    sendSSLShutdown();
                                    throw newSslExceptionWithMessage("SSL_write");
                            }
                        }
                        i3 = sslError;
                        closeAll();
                        pendingNetResult = readPendingBytesFromBIO(byteBuffer, bytesConsumed, bytesProduced, handshakeStatus);
                        SSLEngineResult sSLEngineResult3 = pendingNetResult != null ? pendingNetResult : CLOSED_NOT_HANDSHAKING;
                    }
                }
                i2++;
                int i5 = 16384;
                byteBufferArr = srcs;
                z = false;
            }
            if (bytesConsumed == 0) {
            }
            pendingNetResult = newResult(bytesConsumed, bytesProduced, handshakeStatus);
            return pendingNetResult;
        }
    }

    public int clientPSKKeyRequested(String identityHint, byte[] identity, byte[] key) {
        return this.ssl.clientPSKKeyRequested(identityHint, identity, key);
    }

    public int serverPSKKeyRequested(String identityHint, String identity, byte[] key) {
        return this.ssl.serverPSKKeyRequested(identityHint, identity, key);
    }

    public void onSSLStateChange(int type, int val) {
        synchronized (this.ssl) {
            if (type == 16) {
                transitionTo(2);
            } else if (type == 32) {
                if (this.state != 2) {
                    if (this.state != 4) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Completed handshake while in mode ");
                        stringBuilder.append(this.state);
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                }
                transitionTo(3);
            }
        }
    }

    public void onNewSessionEstablished(long sslSessionNativePtr) {
        try {
            NativeCrypto.SSL_SESSION_up_ref(sslSessionNativePtr);
            sessionContext().cacheSession(NativeSslSession.newInstance(new SSL_SESSION(sslSessionNativePtr), this.activeSession));
        } catch (Exception e) {
        }
    }

    public long serverSessionRequested(byte[] id) {
        return 0;
    }

    public void verifyCertificateChain(byte[][] certChain, String authMethod) throws CertificateException {
        if (certChain != null) {
            try {
                if (certChain.length != 0) {
                    X509Certificate[] peerCertChain = SSLUtils.decodeX509CertificateChain(certChain);
                    X509TrustManager x509tm = this.sslParameters.getX509TrustManager();
                    if (x509tm != null) {
                        this.activeSession.onPeerCertificatesReceived(getPeerHost(), getPeerPort(), peerCertChain);
                        if (getUseClientMode()) {
                            Platform.checkServerTrusted(x509tm, peerCertChain, authMethod, this);
                        } else {
                            Platform.checkClientTrusted(x509tm, peerCertChain, peerCertChain[0].getPublicKey().getAlgorithm(), this);
                        }
                        return;
                    }
                    throw new CertificateException("No X.509 TrustManager");
                }
            } catch (CertificateException e) {
                throw e;
            } catch (Exception e2) {
                throw new CertificateException(e2);
            }
        }
        throw new CertificateException("Peer sent no certificate");
    }

    public void clientCertificateRequested(byte[] keyTypeBytes, byte[][] asn1DerEncodedPrincipals) throws CertificateEncodingException, SSLException {
        this.ssl.chooseClientCertificate(keyTypeBytes, asn1DerEncodedPrincipals);
    }

    private void sendSSLShutdown() {
        try {
            this.ssl.shutdown();
        } catch (IOException e) {
        }
    }

    private void closeAndFreeResources() {
        transitionTo(8);
        if (!this.ssl.isClosed()) {
            this.ssl.close();
            this.networkBio.close();
        }
    }

    protected void finalize() throws Throwable {
        try {
            transitionTo(8);
        } finally {
            super.finalize();
        }
    }

    public String chooseServerAlias(X509KeyManager keyManager, String keyType) {
        if (keyManager instanceof X509ExtendedKeyManager) {
            return ((X509ExtendedKeyManager) keyManager).chooseEngineServerAlias(keyType, null, this);
        }
        return keyManager.chooseServerAlias(keyType, null, null);
    }

    public String chooseClientAlias(X509KeyManager keyManager, X500Principal[] issuers, String[] keyTypes) {
        if (keyManager instanceof X509ExtendedKeyManager) {
            return ((X509ExtendedKeyManager) keyManager).chooseEngineClientAlias(keyTypes, issuers, this);
        }
        return keyManager.chooseClientAlias(keyTypes, issuers, null);
    }

    public String chooseServerPSKIdentityHint(PSKKeyManager keyManager) {
        return keyManager.chooseServerKeyIdentityHint((SSLEngine) this);
    }

    public String chooseClientPSKIdentity(PSKKeyManager keyManager, String identityHint) {
        return keyManager.chooseClientKeyIdentity(identityHint, (SSLEngine) this);
    }

    public SecretKey getPSKKey(PSKKeyManager keyManager, String identityHint, String identity) {
        return keyManager.getKey(identityHint, identity, (SSLEngine) this);
    }

    void setUseSessionTickets(boolean useSessionTickets) {
        this.sslParameters.setUseSessionTickets(useSessionTickets);
    }

    String[] getApplicationProtocols() {
        return this.sslParameters.getApplicationProtocols();
    }

    void setApplicationProtocols(String[] protocols) {
        this.sslParameters.setApplicationProtocols(protocols);
    }

    void setApplicationProtocolSelector(ApplicationProtocolSelector selector) {
        setApplicationProtocolSelector(selector == null ? null : new ApplicationProtocolSelectorAdapter((SSLEngine) this, selector));
    }

    byte[] getTlsUnique() {
        return this.ssl.getTlsUnique();
    }

    void setApplicationProtocolSelector(ApplicationProtocolSelectorAdapter adapter) {
        this.sslParameters.setApplicationProtocolSelector(adapter);
    }

    public String getApplicationProtocol() {
        return SSLUtils.toProtocolString(this.ssl.getApplicationProtocol());
    }

    public String getHandshakeApplicationProtocol() {
        String applicationProtocol;
        synchronized (this.ssl) {
            applicationProtocol = this.state == 2 ? getApplicationProtocol() : null;
        }
        return applicationProtocol;
    }

    private ByteBuffer[] singleSrcBuffer(ByteBuffer src) {
        this.singleSrcBuffer[0] = src;
        return this.singleSrcBuffer;
    }

    private void resetSingleSrcBuffer() {
        this.singleSrcBuffer[0] = null;
    }

    private ByteBuffer[] singleDstBuffer(ByteBuffer src) {
        this.singleDstBuffer[0] = src;
        return this.singleDstBuffer;
    }

    private void resetSingleDstBuffer() {
        this.singleDstBuffer[0] = null;
    }

    private ClientSessionContext clientSessionContext() {
        return this.sslParameters.getClientSessionContext();
    }

    private AbstractSessionContext sessionContext() {
        return this.sslParameters.getSessionContext();
    }

    private void transitionTo(int newState) {
        if (newState == 2) {
            this.handshakeFinished = false;
        } else if (newState == 8 && !this.ssl.isClosed() && this.state >= 2 && this.state < 8) {
            this.closedSession = new SessionSnapshot(this.activeSession);
        }
        this.state = newState;
    }
}
