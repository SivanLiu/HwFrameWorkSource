package com.android.org.conscrypt;

import java.nio.ByteBuffer;
import java.security.PrivateKey;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

abstract class AbstractConscryptEngine extends SSLEngine {
    public abstract String getApplicationProtocol();

    abstract String[] getApplicationProtocols();

    abstract byte[] getChannelId() throws SSLException;

    public abstract String getHandshakeApplicationProtocol();

    abstract String getHostname();

    public abstract String getPeerHost();

    public abstract int getPeerPort();

    abstract byte[] getTlsUnique();

    abstract SSLSession handshakeSession();

    abstract int maxSealOverhead();

    abstract void setApplicationProtocolSelector(ApplicationProtocolSelector applicationProtocolSelector);

    abstract void setApplicationProtocols(String[] strArr);

    abstract void setBufferAllocator(BufferAllocator bufferAllocator);

    abstract void setChannelIdEnabled(boolean z);

    abstract void setChannelIdPrivateKey(PrivateKey privateKey);

    abstract void setHandshakeListener(HandshakeListener handshakeListener);

    abstract void setHostname(String str);

    abstract void setUseSessionTickets(boolean z);

    public abstract SSLEngineResult unwrap(ByteBuffer byteBuffer, ByteBuffer byteBuffer2) throws SSLException;

    public abstract SSLEngineResult unwrap(ByteBuffer byteBuffer, ByteBuffer[] byteBufferArr) throws SSLException;

    public abstract SSLEngineResult unwrap(ByteBuffer byteBuffer, ByteBuffer[] byteBufferArr, int i, int i2) throws SSLException;

    abstract SSLEngineResult unwrap(ByteBuffer[] byteBufferArr, int i, int i2, ByteBuffer[] byteBufferArr2, int i3, int i4) throws SSLException;

    abstract SSLEngineResult unwrap(ByteBuffer[] byteBufferArr, ByteBuffer[] byteBufferArr2) throws SSLException;

    public abstract SSLEngineResult wrap(ByteBuffer byteBuffer, ByteBuffer byteBuffer2) throws SSLException;

    public abstract SSLEngineResult wrap(ByteBuffer[] byteBufferArr, int i, int i2, ByteBuffer byteBuffer) throws SSLException;

    AbstractConscryptEngine() {
    }

    public final SSLSession getHandshakeSession() {
        return handshakeSession();
    }
}
