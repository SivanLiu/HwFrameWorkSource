package com.android.org.conscrypt;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.PrivateKey;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

abstract class AbstractConscryptSocket extends SSLSocket {
    @Deprecated
    abstract byte[] getAlpnSelectedProtocol();

    public abstract String getApplicationProtocol();

    abstract String[] getApplicationProtocols();

    abstract byte[] getChannelId() throws SSLException;

    public abstract FileDescriptor getFileDescriptor$();

    public abstract String getHandshakeApplicationProtocol();

    public abstract SSLSession getHandshakeSession();

    abstract String getHostname();

    abstract String getHostnameOrIP();

    abstract int getSoWriteTimeout() throws SocketException;

    abstract byte[] getTlsUnique();

    abstract PeerInfoProvider peerInfoProvider();

    @Deprecated
    abstract void setAlpnProtocols(byte[] bArr);

    @Deprecated
    abstract void setAlpnProtocols(String[] strArr);

    abstract void setApplicationProtocolSelector(ApplicationProtocolSelector applicationProtocolSelector);

    abstract void setApplicationProtocols(String[] strArr);

    abstract void setChannelIdEnabled(boolean z);

    abstract void setChannelIdPrivateKey(PrivateKey privateKey);

    abstract void setHandshakeTimeout(int i) throws SocketException;

    abstract void setHostname(String str);

    abstract void setSoWriteTimeout(int i) throws SocketException;

    abstract void setUseSessionTickets(boolean z);

    AbstractConscryptSocket() {
    }

    AbstractConscryptSocket(String hostname, int port) throws IOException {
        super(hostname, port);
    }

    AbstractConscryptSocket(InetAddress address, int port) throws IOException {
        super(address, port);
    }

    AbstractConscryptSocket(String hostname, int port, InetAddress clientAddress, int clientPort) throws IOException {
        super(hostname, port, clientAddress, clientPort);
    }

    AbstractConscryptSocket(InetAddress address, int port, InetAddress clientAddress, int clientPort) throws IOException {
        super(address, port, clientAddress, clientPort);
    }

    @Deprecated
    byte[] getNpnSelectedProtocol() {
        return null;
    }

    @Deprecated
    void setNpnProtocols(byte[] npnProtocols) {
    }
}
