package com.android.org.conscrypt;

import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.util.List;
import java.util.function.BiFunction;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

final class Java8EngineWrapper extends AbstractConscryptEngine {
    private final ConscryptEngine delegate;
    private BiFunction<SSLEngine, List<String>, String> selector;

    Java8EngineWrapper(ConscryptEngine delegate) {
        this.delegate = (ConscryptEngine) Preconditions.checkNotNull(delegate, "delegate");
    }

    static SSLEngine getDelegate(SSLEngine engine) {
        if (engine instanceof Java8EngineWrapper) {
            return ((Java8EngineWrapper) engine).delegate;
        }
        return engine;
    }

    public SSLEngineResult wrap(ByteBuffer[] byteBuffers, ByteBuffer byteBuffer) throws SSLException {
        return this.delegate.wrap(byteBuffers, byteBuffer);
    }

    public SSLParameters getSSLParameters() {
        return this.delegate.getSSLParameters();
    }

    public void setSSLParameters(SSLParameters sslParameters) {
        this.delegate.setSSLParameters(sslParameters);
    }

    void setBufferAllocator(BufferAllocator bufferAllocator) {
        this.delegate.setBufferAllocator(bufferAllocator);
    }

    int maxSealOverhead() {
        return this.delegate.maxSealOverhead();
    }

    void setChannelIdEnabled(boolean enabled) {
        this.delegate.setChannelIdEnabled(enabled);
    }

    byte[] getChannelId() throws SSLException {
        return this.delegate.getChannelId();
    }

    void setChannelIdPrivateKey(PrivateKey privateKey) {
        this.delegate.setChannelIdPrivateKey(privateKey);
    }

    void setHandshakeListener(HandshakeListener handshakeListener) {
        this.delegate.setHandshakeListener(handshakeListener);
    }

    void setHostname(String hostname) {
        this.delegate.setHostname(hostname);
    }

    String getHostname() {
        return this.delegate.getHostname();
    }

    public String getPeerHost() {
        return this.delegate.getPeerHost();
    }

    public int getPeerPort() {
        return this.delegate.getPeerPort();
    }

    public void beginHandshake() throws SSLException {
        this.delegate.beginHandshake();
    }

    public void closeInbound() throws SSLException {
        this.delegate.closeInbound();
    }

    public void closeOutbound() {
        this.delegate.closeOutbound();
    }

    public Runnable getDelegatedTask() {
        return this.delegate.getDelegatedTask();
    }

    public String[] getEnabledCipherSuites() {
        return this.delegate.getEnabledCipherSuites();
    }

    public String[] getEnabledProtocols() {
        return this.delegate.getEnabledProtocols();
    }

    public boolean getEnableSessionCreation() {
        return this.delegate.getEnableSessionCreation();
    }

    public HandshakeStatus getHandshakeStatus() {
        return this.delegate.getHandshakeStatus();
    }

    public boolean getNeedClientAuth() {
        return this.delegate.getNeedClientAuth();
    }

    SSLSession handshakeSession() {
        return this.delegate.handshakeSession();
    }

    public SSLSession getSession() {
        return this.delegate.getSession();
    }

    public String[] getSupportedCipherSuites() {
        return this.delegate.getSupportedCipherSuites();
    }

    public String[] getSupportedProtocols() {
        return this.delegate.getSupportedProtocols();
    }

    public boolean getUseClientMode() {
        return this.delegate.getUseClientMode();
    }

    public boolean getWantClientAuth() {
        return this.delegate.getWantClientAuth();
    }

    public boolean isInboundDone() {
        return this.delegate.isInboundDone();
    }

    public boolean isOutboundDone() {
        return this.delegate.isOutboundDone();
    }

    public void setEnabledCipherSuites(String[] suites) {
        this.delegate.setEnabledCipherSuites(suites);
    }

    public void setEnabledProtocols(String[] protocols) {
        this.delegate.setEnabledProtocols(protocols);
    }

    public void setEnableSessionCreation(boolean flag) {
        this.delegate.setEnableSessionCreation(flag);
    }

    public void setNeedClientAuth(boolean need) {
        this.delegate.setNeedClientAuth(need);
    }

    public void setUseClientMode(boolean mode) {
        this.delegate.setUseClientMode(mode);
    }

    public void setWantClientAuth(boolean want) {
        this.delegate.setWantClientAuth(want);
    }

    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
        return this.delegate.unwrap(src, dst);
    }

    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts) throws SSLException {
        return this.delegate.unwrap(src, dsts);
    }

    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws SSLException {
        return this.delegate.unwrap(src, dsts, offset, length);
    }

    SSLEngineResult unwrap(ByteBuffer[] srcs, ByteBuffer[] dsts) throws SSLException {
        return this.delegate.unwrap(srcs, dsts);
    }

    SSLEngineResult unwrap(ByteBuffer[] srcs, int srcsOffset, int srcsLength, ByteBuffer[] dsts, int dstsOffset, int dstsLength) throws SSLException {
        return this.delegate.unwrap(srcs, srcsOffset, srcsLength, dsts, dstsOffset, dstsLength);
    }

    public SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException {
        return this.delegate.wrap(src, dst);
    }

    public SSLEngineResult wrap(ByteBuffer[] srcs, int srcsOffset, int srcsLength, ByteBuffer dst) throws SSLException {
        return this.delegate.wrap(srcs, srcsOffset, srcsLength, dst);
    }

    void setUseSessionTickets(boolean useSessionTickets) {
        this.delegate.setUseSessionTickets(useSessionTickets);
    }

    void setApplicationProtocols(String[] protocols) {
        this.delegate.setApplicationProtocols(protocols);
    }

    String[] getApplicationProtocols() {
        return this.delegate.getApplicationProtocols();
    }

    public String getApplicationProtocol() {
        return this.delegate.getApplicationProtocol();
    }

    void setApplicationProtocolSelector(ApplicationProtocolSelector selector) {
        this.delegate.setApplicationProtocolSelector(selector == null ? null : new ApplicationProtocolSelectorAdapter((SSLEngine) this, selector));
    }

    byte[] getTlsUnique() {
        return this.delegate.getTlsUnique();
    }

    public String getHandshakeApplicationProtocol() {
        return this.delegate.getHandshakeApplicationProtocol();
    }

    public void setHandshakeApplicationProtocolSelector(BiFunction<SSLEngine, List<String>, String> selector) {
        this.selector = selector;
        setApplicationProtocolSelector(toApplicationProtocolSelector(selector));
    }

    public BiFunction<SSLEngine, List<String>, String> getHandshakeApplicationProtocolSelector() {
        return this.selector;
    }

    private static ApplicationProtocolSelector toApplicationProtocolSelector(final BiFunction<SSLEngine, List<String>, String> selector) {
        return selector == null ? null : new ApplicationProtocolSelector() {
            public String selectApplicationProtocol(SSLEngine engine, List<String> protocols) {
                return (String) selector.apply(engine, protocols);
            }

            public String selectApplicationProtocol(SSLSocket socket, List<String> list) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
