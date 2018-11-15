package com.android.org.conscrypt;

public final class ServerSessionContext extends AbstractSessionContext {
    private SSLServerSessionCache persistentCache;

    ServerSessionContext() {
        super(100);
        NativeCrypto.SSL_CTX_set_session_id_context(this.sslCtxNativePointer, new byte[]{(byte) 32});
    }

    public void setPersistentCache(SSLServerSessionCache persistentCache) {
        this.persistentCache = persistentCache;
    }

    SslSessionWrapper getSessionFromPersistentCache(byte[] sessionId) {
        if (this.persistentCache != null) {
            byte[] data = this.persistentCache.getSessionData(sessionId);
            if (data != null) {
                SslSessionWrapper session = SslSessionWrapper.newInstance(this, data, null, -1);
                if (session != null && session.isValid()) {
                    cacheSession(session);
                    return session;
                }
            }
        }
        return null;
    }

    void onBeforeAddSession(SslSessionWrapper session) {
        if (this.persistentCache != null) {
            byte[] data = session.toBytes();
            if (data != null) {
                this.persistentCache.putSessionData(session.toSSLSession(), data);
            }
        }
    }

    void onBeforeRemoveSession(SslSessionWrapper session) {
    }
}
