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

final class ConscryptEngine extends SSLEngine implements SSLHandshakeCallbacks, AliasChooser, PSKCallbacks {
    private static final SSLEngineResult CLOSED_NOT_HANDSHAKING = new SSLEngineResult(Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
    private static final ByteBuffer EMPTY = ByteBuffer.allocateDirect(0);
    private static final SSLEngineResult NEED_UNWRAP_CLOSED = new SSLEngineResult(Status.CLOSED, HandshakeStatus.NEED_UNWRAP, 0, 0);
    private static final SSLEngineResult NEED_UNWRAP_OK = new SSLEngineResult(Status.OK, HandshakeStatus.NEED_UNWRAP, 0, 0);
    private static final SSLEngineResult NEED_WRAP_CLOSED = new SSLEngineResult(Status.CLOSED, HandshakeStatus.NEED_WRAP, 0, 0);
    private static final SSLEngineResult NEED_WRAP_OK = new SSLEngineResult(Status.OK, HandshakeStatus.NEED_WRAP, 0, 0);
    private BufferAllocator bufferAllocator;
    private OpenSSLKey channelIdPrivateKey;
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
    private final SslWrapper ssl;
    private final SSLParametersImpl sslParameters;
    private final ActiveSession sslSession;
    private int state;
    private final Object stateLock;

    ConscryptEngine(SSLParametersImpl sslParameters) {
        this.stateLock = new Object();
        this.state = 0;
        this.singleSrcBuffer = new ByteBuffer[1];
        this.singleDstBuffer = new ByteBuffer[1];
        this.sslParameters = sslParameters;
        this.peerInfoProvider = PeerInfoProvider.nullProvider();
        this.ssl = newSsl(sslParameters, this);
        this.networkBio = this.ssl.newBio();
        this.sslSession = new ActiveSession(this.ssl, sslParameters.getSessionContext());
    }

    ConscryptEngine(String host, int port, SSLParametersImpl sslParameters) {
        this.stateLock = new Object();
        this.state = 0;
        this.singleSrcBuffer = new ByteBuffer[1];
        this.singleDstBuffer = new ByteBuffer[1];
        this.sslParameters = sslParameters;
        this.peerInfoProvider = PeerInfoProvider.forHostAndPort(host, port);
        this.ssl = newSsl(sslParameters, this);
        this.networkBio = this.ssl.newBio();
        this.sslSession = new ActiveSession(this.ssl, sslParameters.getSessionContext());
    }

    ConscryptEngine(SSLParametersImpl sslParameters, PeerInfoProvider peerInfoProvider) {
        this.stateLock = new Object();
        this.state = 0;
        this.singleSrcBuffer = new ByteBuffer[1];
        this.singleDstBuffer = new ByteBuffer[1];
        this.sslParameters = sslParameters;
        this.peerInfoProvider = (PeerInfoProvider) Preconditions.checkNotNull(peerInfoProvider, "peerInfoProvider");
        this.ssl = newSsl(sslParameters, this);
        this.networkBio = this.ssl.newBio();
        this.sslSession = new ActiveSession(this.ssl, sslParameters.getSessionContext());
    }

    private static SslWrapper newSsl(SSLParametersImpl sslParameters, ConscryptEngine engine) {
        try {
            return SslWrapper.newInstance(sslParameters, engine, engine, engine);
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    void setBufferAllocator(BufferAllocator bufferAllocator) {
        synchronized (this.stateLock) {
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
        synchronized (this.stateLock) {
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
        synchronized (this.stateLock) {
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
            synchronized (this.stateLock) {
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
                }
            }
        } else {
            throw new IllegalStateException("Not allowed in server mode");
        }
    }

    void setHandshakeListener(HandshakeListener handshakeListener) {
        synchronized (this.stateLock) {
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
        synchronized (this.stateLock) {
            beginHandshakeInternal();
        }
    }

    private void beginHandshakeInternal() throws SSLException {
        switch (this.state) {
            case 1:
                this.state = 2;
                try {
                    this.ssl.initialize(getHostname(), this.channelIdPrivateKey);
                    if (getUseClientMode()) {
                        SslSessionWrapper cachedSession = clientSessionContext().getCachedSession(getHostname(), getPeerPort(), this.sslParameters);
                        if (cachedSession != null) {
                            cachedSession.offerToResume(this.ssl);
                        }
                    }
                    this.maxSealOverhead = this.ssl.getMaxSealOverhead();
                    handshake();
                    if (false) {
                        this.state = 8;
                        shutdownAndFreeSslNative();
                        return;
                    }
                    return;
                } catch (IOException e) {
                    if (e.getMessage().contains("unexpected CCS")) {
                        Platform.logEvent(String.format("ssl_unexpected_ccs: host=%s", new Object[]{getPeerHost()}));
                    }
                    throw SSLUtils.toSSLHandshakeException(e);
                } catch (Throwable th) {
                    if (true) {
                        this.state = 8;
                        shutdownAndFreeSslNative();
                    }
                }
            case 2:
                throw new IllegalStateException("Handshake has already been started");
            case 6:
            case 7:
            case 8:
                throw new IllegalStateException("Engine has already been closed");
            default:
                throw new IllegalStateException("Client/server mode must be set before handshake");
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void closeInbound() throws SSLException {
        synchronized (this.stateLock) {
            if (this.state == 8) {
            } else if (this.state == 7) {
                this.state = 8;
            } else {
                this.state = 6;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void closeOutbound() {
        synchronized (this.stateLock) {
            if (this.state == 8 || this.state == 7) {
            } else {
                if (isHandshakeStarted()) {
                    shutdownAndFreeSslNative();
                }
                if (this.state == 6) {
                    this.state = 8;
                } else {
                    this.state = 7;
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
        synchronized (this.stateLock) {
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
                throw new IllegalStateException("Unexpected engine state: " + this.state);
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

    public SSLSession getHandshakeSession() {
        return handshakeSession();
    }

    SSLSession handshakeSession() {
        SSLSession sSLSession;
        synchronized (this.stateLock) {
            sSLSession = this.state == 2 ? this.sslSession : null;
        }
        return sSLSession;
    }

    public SSLSession getSession() {
        synchronized (this.stateLock) {
            if (this.state < 3) {
                SSLSession nullSession = SSLNullSession.getNullSession();
                return nullSession;
            }
            nullSession = Platform.wrapSSLSession(this.sslSession);
            return nullSession;
        }
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
        synchronized (this.stateLock) {
            if (this.state == 8 || this.state == 6) {
                return true;
            }
            return this.ssl.wasShutdownReceived();
        }
    }

    public boolean isOutboundDone() {
        synchronized (this.stateLock) {
            if (this.state == 8 || this.state == 7) {
                return true;
            }
            return this.ssl.wasShutdownSent();
        }
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
        synchronized (this.stateLock) {
            if (isHandshakeStarted()) {
                throw new IllegalArgumentException("Can not change mode after handshake: state == " + this.state);
            }
            this.state = 1;
        }
        this.sslParameters.setUseClientMode(mode);
    }

    public void setWantClientAuth(boolean want) {
        this.sslParameters.setWantClientAuth(want);
    }

    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
        SSLEngineResult unwrap;
        synchronized (this.stateLock) {
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
        synchronized (this.stateLock) {
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
        synchronized (this.stateLock) {
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
        boolean z;
        boolean z2 = true;
        if (srcs != null) {
            z = true;
        } else {
            z = false;
        }
        Preconditions.checkArgument(z, "srcs is null");
        if (dsts == null) {
            z2 = false;
        }
        Preconditions.checkArgument(z2, "dsts is null");
        return unwrap(srcs, 0, srcs.length, dsts, 0, dsts.length);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    SSLEngineResult unwrap(ByteBuffer[] srcs, int srcsOffset, int srcsLength, ByteBuffer[] dsts, int dstsOffset, int dstsLength) throws SSLException {
        Preconditions.checkArgument(srcs != null, "srcs is null");
        Preconditions.checkArgument(dsts != null, "dsts is null");
        Preconditions.checkPositionIndexes(srcsOffset, srcsOffset + srcsLength, srcs.length);
        Preconditions.checkPositionIndexes(dstsOffset, dstsOffset + dstsLength, dsts.length);
        int dstLength = calcDstsLength(dsts, dstsOffset, dstsLength);
        int endOffset = dstsOffset + dstsLength;
        int srcsEndOffset = srcsOffset + srcsLength;
        long srcLength = calcSrcsLength(srcs, srcsOffset, srcsEndOffset);
        synchronized (this.stateLock) {
            SSLEngineResult sSLEngineResult;
            switch (this.state) {
                case 0:
                    throw new IllegalStateException("Client/server mode must be set before calling unwrap");
                case 1:
                    beginHandshakeInternal();
                case 6:
                case 8:
                    sSLEngineResult = new SSLEngineResult(Status.CLOSED, getHandshakeStatusInternal(), 0, 0);
                    return sSLEngineResult;
                default:
                    SSLEngineResult sSLEngineResult2;
                    HandshakeStatus handshakeStatus = HandshakeStatus.NOT_HANDSHAKING;
                    if (!this.handshakeFinished) {
                        handshakeStatus = handshake();
                        if (handshakeStatus == HandshakeStatus.NEED_WRAP) {
                            sSLEngineResult2 = NEED_WRAP_OK;
                            return sSLEngineResult2;
                        } else if (this.state == 8) {
                            sSLEngineResult2 = NEED_WRAP_CLOSED;
                            return sSLEngineResult2;
                        }
                    }
                    boolean noCleartextDataAvailable = pendingInboundCleartextBytes() <= 0;
                    int lenRemaining = 0;
                    if (srcLength <= 0 || !noCleartextDataAvailable) {
                        if (noCleartextDataAvailable) {
                            sSLEngineResult = new SSLEngineResult(Status.BUFFER_UNDERFLOW, getHandshakeStatus(), 0, 0);
                            return sSLEngineResult;
                        }
                    } else if (srcLength < 5) {
                        sSLEngineResult = new SSLEngineResult(Status.BUFFER_UNDERFLOW, getHandshakeStatus(), 0, 0);
                        return sSLEngineResult;
                    } else {
                        int packetLength = SSLUtils.getEncryptedPacketLength(srcs, srcsOffset);
                        if (packetLength < 0) {
                            throw new SSLException("Unable to parse TLS packet header");
                        } else if (srcLength < ((long) packetLength)) {
                            sSLEngineResult = new SSLEngineResult(Status.BUFFER_UNDERFLOW, getHandshakeStatus(), 0, 0);
                            return sSLEngineResult;
                        } else {
                            lenRemaining = packetLength;
                        }
                    }
                    int bytesConsumed = 0;
                    if (lenRemaining > 0 && srcsOffset < srcsEndOffset) {
                        do {
                            ByteBuffer src = srcs[srcsOffset];
                            int remaining = src.remaining();
                            if (remaining == 0) {
                                srcsOffset++;
                            } else {
                                int written = writeEncryptedData(src, Math.min(lenRemaining, remaining));
                                if (written > 0) {
                                    bytesConsumed += written;
                                    lenRemaining -= written;
                                    if (lenRemaining != 0 && written == remaining) {
                                        srcsOffset++;
                                    }
                                } else {
                                    NativeCrypto.SSL_clear_error();
                                }
                            }
                        } while (srcsOffset < srcsEndOffset);
                    }
                    int bytesProduced = 0;
                    if (dstLength > 0) {
                        int idx = dstsOffset;
                        while (idx < endOffset) {
                            try {
                                ByteBuffer dst = dsts[idx];
                                if (dst.hasRemaining()) {
                                    int bytesRead = readPlaintextData(dst);
                                    if (bytesRead > 0) {
                                        bytesProduced += bytesRead;
                                        if (!dst.hasRemaining()) {
                                        }
                                    } else {
                                        switch (bytesRead) {
                                            case -3:
                                            case -2:
                                                sSLEngineResult2 = newResult(bytesConsumed, bytesProduced, handshakeStatus);
                                                return sSLEngineResult2;
                                            default:
                                                throw shutdownWithError("SSL_read");
                                        }
                                    }
                                }
                                idx++;
                            } catch (SSLException e) {
                                if (pendingOutboundEncryptedBytes() > 0) {
                                    if (!this.handshakeFinished && this.handshakeException == null) {
                                        this.handshakeException = e;
                                    }
                                    return new SSLEngineResult(Status.OK, HandshakeStatus.NEED_WRAP, bytesConsumed, bytesProduced);
                                }
                                shutdown();
                                throw convertException(e);
                            } catch (InterruptedIOException e2) {
                                return newResult(bytesConsumed, bytesProduced, handshakeStatus);
                            } catch (EOFException e3) {
                                closeAll();
                                throw convertException(e3);
                            } catch (IOException e4) {
                                shutdown();
                                throw convertException(e4);
                            }
                        }
                    }
                    readPlaintextData(EMPTY);
                    if ((this.handshakeFinished ? pendingInboundCleartextBytes() : 0) > 0) {
                        Status status = Status.BUFFER_OVERFLOW;
                        if (handshakeStatus != HandshakeStatus.FINISHED) {
                            handshakeStatus = getHandshakeStatusInternal();
                        }
                        sSLEngineResult = new SSLEngineResult(status, mayFinishHandshake(handshakeStatus), bytesConsumed, bytesProduced);
                        return sSLEngineResult;
                    }
                    sSLEngineResult2 = newResult(bytesConsumed, bytesProduced, handshakeStatus);
                    return sSLEngineResult2;
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
        for (int i = srcsOffset; i < srcsEndOffset; i++) {
            ByteBuffer src = srcs[i];
            if (src == null) {
                throw new IllegalArgumentException("srcs[" + i + "] is null");
            }
            len += (long) src.remaining();
        }
        return len;
    }

    private HandshakeStatus handshake() throws SSLException {
        SSLException e;
        try {
            if (this.handshakeException == null) {
                switch (this.ssl.doHandshake()) {
                    case 2:
                        return pendingStatus(pendingOutboundEncryptedBytes());
                    case CTConstants.CERTIFICATE_LENGTH_BYTES /*3*/:
                        return HandshakeStatus.NEED_WRAP;
                    default:
                        this.sslSession.onSessionEstablished(getPeerHost(), getPeerPort());
                        finishHandshake();
                        return HandshakeStatus.FINISHED;
                }
            } else if (pendingOutboundEncryptedBytes() > 0) {
                return HandshakeStatus.NEED_WRAP;
            } else {
                e = this.handshakeException;
                this.handshakeException = null;
                throw e;
            }
        } catch (SSLException e2) {
            if (pendingOutboundEncryptedBytes() > 0) {
                this.handshakeException = e2;
                return HandshakeStatus.NEED_WRAP;
            }
            shutdown();
            throw e2;
        } catch (IOException e3) {
            shutdown();
            throw e3;
        } catch (Exception e4) {
            throw SSLUtils.toSSLHandshakeException(e4);
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
        if ((e instanceof SSLHandshakeException) || (this.handshakeFinished ^ 1) != 0) {
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
            Status status2;
            if (dst.remaining() < pendingNet) {
                status2 = Status.BUFFER_OVERFLOW;
                if (status != HandshakeStatus.FINISHED) {
                    status = getHandshakeStatus(pendingNet);
                }
                return new SSLEngineResult(status2, mayFinishHandshake(status), bytesConsumed, bytesProduced);
            }
            int produced = readEncryptedData(dst, pendingNet);
            if (produced <= 0) {
                NativeCrypto.SSL_clear_error();
            } else {
                bytesProduced += produced;
                pendingNet -= produced;
            }
            status2 = getEngineStatus();
            if (status != HandshakeStatus.FINISHED) {
                status = getHandshakeStatus(pendingNet);
            }
            return new SSLEngineResult(status2, mayFinishHandshake(status), bytesConsumed, bytesProduced);
        } catch (Exception e) {
            throw convertException(e);
        }
    }

    private int readEncryptedData(ByteBuffer dst, int pending) throws SSLException {
        int bytesRead = 0;
        try {
            int pos = dst.position();
            if (dst.remaining() >= pending) {
                int len = Math.min(pending, dst.limit() - pos);
                if (dst.isDirect()) {
                    bytesRead = readEncryptedDataDirect(dst, pos, len);
                    if (bytesRead > 0) {
                        dst.position(pos + bytesRead);
                    }
                } else {
                    bytesRead = readEncryptedDataHeap(dst, len);
                }
            }
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

    private SSLException shutdownWithError(String err) {
        shutdown();
        if (this.handshakeFinished) {
            return new SSLHandshakeException(err);
        }
        return new SSLException(err);
    }

    private SSLEngineResult newResult(int bytesConsumed, int bytesProduced, HandshakeStatus status) throws SSLException {
        Status engineStatus = getEngineStatus();
        if (status != HandshakeStatus.FINISHED) {
            status = getHandshakeStatusInternal();
        }
        return new SSLEngineResult(engineStatus, mayFinishHandshake(status), bytesConsumed, bytesProduced);
    }

    public final SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
        SSLEngineResult wrap;
        synchronized (this.stateLock) {
            try {
                wrap = wrap(singleSrcBuffer(src), dst);
                resetSingleSrcBuffer();
            } catch (Throwable th) {
                resetSingleSrcBuffer();
            }
        }
        return wrap;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public SSLEngineResult wrap(ByteBuffer[] srcs, int srcsOffset, int srcsLength, ByteBuffer dst) throws SSLException {
        Preconditions.checkArgument(srcs != null, "srcs is null");
        Preconditions.checkArgument(dst != null, "dst is null");
        Preconditions.checkPositionIndexes(srcsOffset, srcsOffset + srcsLength, srcs.length);
        if (dst.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        synchronized (this.stateLock) {
            SSLEngineResult sSLEngineResult;
            switch (this.state) {
                case 0:
                    throw new IllegalStateException("Client/server mode must be set before calling wrap");
                case 1:
                    beginHandshakeInternal();
                case 7:
                case 8:
                    sSLEngineResult = new SSLEngineResult(Status.CLOSED, getHandshakeStatusInternal(), 0, 0);
                    return sSLEngineResult;
                default:
                    int i;
                    ByteBuffer src;
                    HandshakeStatus handshakeStatus = HandshakeStatus.NOT_HANDSHAKING;
                    if (!this.handshakeFinished) {
                        handshakeStatus = handshake();
                        if (handshakeStatus == HandshakeStatus.NEED_UNWRAP) {
                            sSLEngineResult = NEED_UNWRAP_OK;
                            return sSLEngineResult;
                        } else if (this.state == 8) {
                            sSLEngineResult = NEED_UNWRAP_CLOSED;
                            return sSLEngineResult;
                        }
                    }
                    int srcsLen = 0;
                    int endOffset = srcsOffset + srcsLength;
                    for (i = srcsOffset; i < endOffset; i++) {
                        src = srcs[i];
                        if (src == null) {
                            throw new IllegalArgumentException("srcs[" + i + "] is null");
                        }
                        if (srcsLen != 16384) {
                            srcsLen += src.remaining();
                            if (srcsLen > 16384 || srcsLen < 0) {
                                srcsLen = 16384;
                            }
                        }
                    }
                    if (dst.remaining() < SSLUtils.calculateOutNetBufSize(srcsLen)) {
                        sSLEngineResult = new SSLEngineResult(Status.BUFFER_OVERFLOW, getHandshakeStatusInternal(), 0, 0);
                        return sSLEngineResult;
                    }
                    SSLEngineResult pendingNetResult;
                    int bytesProduced = 0;
                    int bytesConsumed = 0;
                    for (i = srcsOffset; i < endOffset; i++) {
                        src = srcs[i];
                        Preconditions.checkArgument(src != null, "srcs[%d] is null", Integer.valueOf(i));
                        while (src.hasRemaining()) {
                            int result = writePlaintextData(src, Math.min(src.remaining(), 16384 - bytesConsumed));
                            if (result > 0) {
                                bytesConsumed += result;
                                pendingNetResult = readPendingBytesFromBIO(dst, bytesConsumed, bytesProduced, handshakeStatus);
                                if (pendingNetResult != null) {
                                    if (pendingNetResult.getStatus() != Status.OK) {
                                        return pendingNetResult;
                                    }
                                    bytesProduced = pendingNetResult.bytesProduced();
                                }
                                if (bytesConsumed == 16384) {
                                    if (bytesConsumed == 0) {
                                        pendingNetResult = readPendingBytesFromBIO(dst, 0, bytesProduced, handshakeStatus);
                                        if (pendingNetResult != null) {
                                            return pendingNetResult;
                                        }
                                    }
                                    sSLEngineResult = newResult(bytesConsumed, bytesProduced, handshakeStatus);
                                    return sSLEngineResult;
                                }
                            }
                            switch (this.ssl.getError(result)) {
                                case 2:
                                    pendingNetResult = readPendingBytesFromBIO(dst, bytesConsumed, bytesProduced, handshakeStatus);
                                    if (pendingNetResult == null) {
                                        pendingNetResult = new SSLEngineResult(getEngineStatus(), HandshakeStatus.NEED_UNWRAP, bytesConsumed, bytesProduced);
                                        break;
                                    }
                                    break;
                                case CTConstants.CERTIFICATE_LENGTH_BYTES /*3*/:
                                    pendingNetResult = readPendingBytesFromBIO(dst, bytesConsumed, bytesProduced, handshakeStatus);
                                    if (pendingNetResult == null) {
                                        pendingNetResult = NEED_WRAP_CLOSED;
                                        break;
                                    }
                                    break;
                                case 6:
                                    closeAll();
                                    pendingNetResult = readPendingBytesFromBIO(dst, bytesConsumed, bytesProduced, handshakeStatus);
                                    if (pendingNetResult == null) {
                                        pendingNetResult = CLOSED_NOT_HANDSHAKING;
                                        break;
                                    }
                                    break;
                                default:
                                    throw shutdownWithError("SSL_write");
                            }
                        }
                    }
                    if (bytesConsumed == 0) {
                        pendingNetResult = readPendingBytesFromBIO(dst, 0, bytesProduced, handshakeStatus);
                        if (pendingNetResult != null) {
                            return pendingNetResult;
                        }
                    }
                    sSLEngineResult = newResult(bytesConsumed, bytesProduced, handshakeStatus);
                    return sSLEngineResult;
            }
        }
    }

    public int clientPSKKeyRequested(String identityHint, byte[] identity, byte[] key) {
        return this.ssl.clientPSKKeyRequested(identityHint, identity, key);
    }

    public int serverPSKKeyRequested(String identityHint, String identity, byte[] key) {
        return this.ssl.serverPSKKeyRequested(identityHint, identity, key);
    }

    public void onSSLStateChange(int type, int val) {
        synchronized (this.stateLock) {
            switch (type) {
                case 16:
                    this.state = 2;
                    break;
                case 32:
                    if (this.state == 2 || this.state == 4) {
                        this.state = 3;
                        break;
                    }
                    throw new IllegalStateException("Completed handshake while in mode " + this.state);
                    break;
            }
        }
    }

    public void onNewSessionEstablished(long sslSessionNativePtr) {
        try {
            NativeCrypto.SSL_SESSION_up_ref(sslSessionNativePtr);
            sessionContext().cacheSession(SslSessionWrapper.newInstance(new SSL_SESSION(sslSessionNativePtr), this.sslSession));
        } catch (Exception e) {
        }
    }

    public long serverSessionRequested(byte[] id) {
        return 0;
    }

    public void verifyCertificateChain(long[] certRefs, String authMethod) throws CertificateException {
        try {
            X509TrustManager x509tm = this.sslParameters.getX509TrustManager();
            if (x509tm == null) {
                throw new CertificateException("No X.509 TrustManager");
            }
            if (certRefs != null) {
                if (certRefs.length != 0) {
                    X509Certificate[] peerCertChain = OpenSSLX509Certificate.createCertChain(certRefs);
                    this.sslSession.onPeerCertificatesReceived(getPeerHost(), getPeerPort(), peerCertChain);
                    if (getUseClientMode()) {
                        Platform.checkServerTrusted(x509tm, peerCertChain, authMethod, this);
                        return;
                    } else {
                        Platform.checkClientTrusted(x509tm, peerCertChain, peerCertChain[0].getPublicKey().getAlgorithm(), this);
                        return;
                    }
                }
            }
            throw new SSLException("Peer sent no certificate");
        } catch (CertificateException e) {
            throw e;
        } catch (Exception e2) {
            throw new CertificateException(e2);
        }
    }

    public void clientCertificateRequested(byte[] keyTypeBytes, byte[][] asn1DerEncodedPrincipals) throws CertificateEncodingException, SSLException {
        this.ssl.chooseClientCertificate(keyTypeBytes, asn1DerEncodedPrincipals);
    }

    private void shutdown() {
        try {
            this.ssl.shutdown();
        } catch (IOException e) {
        }
    }

    private void shutdownAndFreeSslNative() {
        try {
            shutdown();
        } finally {
            free();
        }
    }

    private void free() {
        if (!this.ssl.isClosed()) {
            this.ssl.close();
            this.networkBio.close();
        }
    }

    protected void finalize() throws Throwable {
        try {
            free();
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

    void setAlpnProtocols(String[] alpnProtocols) {
        this.sslParameters.setAlpnProtocols(alpnProtocols);
    }

    void setAlpnProtocols(byte[] alpnProtocols) {
        this.sslParameters.setAlpnProtocols(alpnProtocols);
    }

    byte[] getAlpnSelectedProtocol() {
        return this.ssl.getAlpnSelectedProtocol();
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
}
