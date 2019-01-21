package com.android.org.conscrypt;

import java.util.HashMap;
import java.util.Map;

public final class ClientSessionContext extends AbstractSessionContext {
    private SSLClientSessionCache persistentCache;
    private final Map<HostAndPort, NativeSslSession> sessionsByHostAndPort = new HashMap();

    private static final class HostAndPort {
        final String host;
        final int port;

        HostAndPort(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public int hashCode() {
            return (this.host.hashCode() * 31) + this.port;
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof HostAndPort)) {
                return false;
            }
            HostAndPort lhs = (HostAndPort) o;
            if (this.host.equals(lhs.host) && this.port == lhs.port) {
                z = true;
            }
            return z;
        }
    }

    ClientSessionContext() {
        super(10);
    }

    public void setPersistentCache(SSLClientSessionCache persistentCache) {
        this.persistentCache = persistentCache;
    }

    NativeSslSession getCachedSession(String hostName, int port, SSLParametersImpl sslParameters) {
        if (hostName == null) {
            return null;
        }
        NativeSslSession session = getSession(hostName, port);
        if (session == null) {
            return null;
        }
        String protocol = session.getProtocol();
        boolean protocolFound = false;
        int i = 0;
        for (String enabledProtocol : sslParameters.enabledProtocols) {
            if (protocol.equals(enabledProtocol)) {
                protocolFound = true;
                break;
            }
        }
        if (!protocolFound) {
            return null;
        }
        String cipherSuite = session.getCipherSuite();
        boolean cipherSuiteFound = false;
        String[] strArr = sslParameters.enabledCipherSuites;
        int length = strArr.length;
        while (i < length) {
            if (cipherSuite.equals(strArr[i])) {
                cipherSuiteFound = true;
                break;
            }
            i++;
        }
        if (cipherSuiteFound) {
            return session;
        }
        return null;
    }

    int size() {
        return this.sessionsByHostAndPort.size();
    }

    private NativeSslSession getSession(String host, int port) {
        if (host == null) {
            return null;
        }
        NativeSslSession session;
        HostAndPort key = new HostAndPort(host, port);
        synchronized (this.sessionsByHostAndPort) {
            session = (NativeSslSession) this.sessionsByHostAndPort.get(key);
        }
        if (session != null && session.isValid()) {
            return session;
        }
        if (this.persistentCache != null) {
            byte[] data = this.persistentCache.getSessionData(host, port);
            if (data != null) {
                session = NativeSslSession.newInstance(this, data, host, port);
                if (session != null && session.isValid()) {
                    synchronized (this.sessionsByHostAndPort) {
                        this.sessionsByHostAndPort.put(key, session);
                    }
                    return session;
                }
            }
        }
        return null;
    }

    void onBeforeAddSession(NativeSslSession session) {
        String host = session.getPeerHost();
        int port = session.getPeerPort();
        if (host != null) {
            HostAndPort key = new HostAndPort(host, port);
            synchronized (this.sessionsByHostAndPort) {
                this.sessionsByHostAndPort.put(key, session);
            }
            if (this.persistentCache != null) {
                byte[] data = session.toBytes();
                if (data != null) {
                    this.persistentCache.putSessionData(session.toSSLSession(), data);
                }
            }
        }
    }

    void onBeforeRemoveSession(NativeSslSession session) {
        String host = session.getPeerHost();
        if (host != null) {
            HostAndPort hostAndPortKey = new HostAndPort(host, session.getPeerPort());
            synchronized (this.sessionsByHostAndPort) {
                this.sessionsByHostAndPort.remove(hostAndPortKey);
            }
        }
    }

    NativeSslSession getSessionFromPersistentCache(byte[] sessionId) {
        return null;
    }
}
