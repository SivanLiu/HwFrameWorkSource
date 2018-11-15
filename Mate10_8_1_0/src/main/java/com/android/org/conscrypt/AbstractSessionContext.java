package com.android.org.conscrypt;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import org.conscrypt.SslSessionWrapper;

abstract class AbstractSessionContext implements SSLSessionContext {
    private static final int DEFAULT_SESSION_TIMEOUT_SECONDS = 28800;
    private volatile int maximumSize;
    private final Map<ByteArray, SslSessionWrapper> sessions = new LinkedHashMap<ByteArray, SslSessionWrapper>() {
        protected boolean removeEldestEntry(Entry<ByteArray, SslSessionWrapper> eldest) {
            if (AbstractSessionContext.this.maximumSize <= 0 || size() <= AbstractSessionContext.this.maximumSize) {
                return false;
            }
            AbstractSessionContext.this.onBeforeRemoveSession((SslSessionWrapper) eldest.getValue());
            return true;
        }
    };
    final long sslCtxNativePointer = NativeCrypto.SSL_CTX_new();
    private volatile int timeout = DEFAULT_SESSION_TIMEOUT_SECONDS;

    abstract SslSessionWrapper getSessionFromPersistentCache(byte[] bArr);

    abstract void onBeforeAddSession(SslSessionWrapper sslSessionWrapper);

    abstract void onBeforeRemoveSession(SslSessionWrapper sslSessionWrapper);

    AbstractSessionContext(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    public final Enumeration<byte[]> getIds() {
        final Iterator<SslSessionWrapper> iter;
        synchronized (this.sessions) {
            iter = Arrays.asList((SslSessionWrapper[]) this.sessions.values().toArray(new SslSessionWrapper[this.sessions.size()])).iterator();
        }
        return new Enumeration<byte[]>() {
            private SslSessionWrapper next;

            public boolean hasMoreElements() {
                if (this.next != null) {
                    return true;
                }
                while (iter.hasNext()) {
                    SslSessionWrapper session = (SslSessionWrapper) iter.next();
                    if (session.isValid()) {
                        this.next = session;
                        return true;
                    }
                }
                this.next = null;
                return false;
            }

            public byte[] nextElement() {
                if (hasMoreElements()) {
                    byte[] id = this.next.getId();
                    this.next = null;
                    return id;
                }
                throw new NoSuchElementException();
            }
        };
    }

    public final SSLSession getSession(byte[] sessionId) {
        if (sessionId == null) {
            throw new NullPointerException("sessionId");
        }
        ByteArray key = new ByteArray(sessionId);
        synchronized (this.sessions) {
            SslSessionWrapper session = (SslSessionWrapper) this.sessions.get(key);
        }
        if (session == null || !session.isValid()) {
            return null;
        }
        return session.toSSLSession();
    }

    public final int getSessionCacheSize() {
        return this.maximumSize;
    }

    public final int getSessionTimeout() {
        return this.timeout;
    }

    public final void setSessionTimeout(int seconds) throws IllegalArgumentException {
        if (seconds < 0) {
            throw new IllegalArgumentException("seconds < 0");
        }
        synchronized (this.sessions) {
            this.timeout = seconds;
            NativeCrypto.SSL_CTX_set_timeout(this.sslCtxNativePointer, (long) seconds);
            Iterator<SslSessionWrapper> i = this.sessions.values().iterator();
            while (i.hasNext()) {
                SslSessionWrapper session = (SslSessionWrapper) i.next();
                if (!session.isValid()) {
                    onBeforeRemoveSession(session);
                    i.remove();
                }
            }
        }
    }

    public final void setSessionCacheSize(int size) throws IllegalArgumentException {
        if (size < 0) {
            throw new IllegalArgumentException("size < 0");
        }
        int oldMaximum = this.maximumSize;
        this.maximumSize = size;
        if (size < oldMaximum) {
            trimToSize();
        }
    }

    protected void finalize() throws Throwable {
        try {
            NativeCrypto.SSL_CTX_free(this.sslCtxNativePointer);
        } finally {
            super.finalize();
        }
    }

    final void cacheSession(SslSessionWrapper session) {
        byte[] id = session.getId();
        if (id != null && id.length != 0) {
            onBeforeAddSession(session);
            ByteArray key = new ByteArray(id);
            synchronized (this.sessions) {
                this.sessions.put(key, session);
            }
        }
    }

    final SslSessionWrapper getSessionFromCache(byte[] sessionId) {
        if (sessionId == null) {
            return null;
        }
        synchronized (this.sessions) {
            SslSessionWrapper session = (SslSessionWrapper) this.sessions.get(new ByteArray(sessionId));
        }
        if (session == null || !session.isValid()) {
            return getSessionFromPersistentCache(sessionId);
        }
        return session;
    }

    private void trimToSize() {
        synchronized (this.sessions) {
            int size = this.sessions.size();
            if (size > this.maximumSize) {
                int removals = size - this.maximumSize;
                Iterator<SslSessionWrapper> i = this.sessions.values().iterator();
                int removals2 = removals;
                while (true) {
                    removals = removals2 - 1;
                    if (removals2 <= 0) {
                        break;
                    }
                    onBeforeRemoveSession((SslSessionWrapper) i.next());
                    i.remove();
                    removals2 = removals;
                }
            }
        }
    }
}
