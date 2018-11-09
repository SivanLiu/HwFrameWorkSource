package com.android.org.conscrypt;

import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionBindingEvent;
import javax.net.ssl.SSLSessionBindingListener;
import javax.net.ssl.SSLSessionContext;

final class ActiveSession implements SSLSession {
    private String cipherSuite;
    private long creationTime;
    private byte[] id;
    private long lastAccessedTime = 0;
    private X509Certificate[] localCertificates;
    private volatile javax.security.cert.X509Certificate[] peerCertificateChain;
    private byte[] peerCertificateOcspData;
    private X509Certificate[] peerCertificates;
    private String peerHost;
    private int peerPort = -1;
    private byte[] peerTlsSctData;
    private String protocol;
    private AbstractSessionContext sessionContext;
    private final SslWrapper ssl;
    private Map<String, Object> values;

    ActiveSession(SslWrapper ssl, AbstractSessionContext sessionContext) {
        this.ssl = (SslWrapper) Preconditions.checkNotNull(ssl, "ssl");
        this.sessionContext = (AbstractSessionContext) Preconditions.checkNotNull(sessionContext, "sessionContext");
    }

    public byte[] getId() {
        if (this.id == null) {
            this.id = this.ssl.getSessionId();
        }
        return this.id != null ? (byte[]) this.id.clone() : EmptyArray.BYTE;
    }

    void resetId() {
        this.id = null;
    }

    public SSLSessionContext getSessionContext() {
        return isValid() ? this.sessionContext : null;
    }

    public long getCreationTime() {
        if (this.creationTime == 0) {
            this.creationTime = this.ssl.getTime();
        }
        return this.creationTime;
    }

    public long getLastAccessedTime() {
        return this.lastAccessedTime == 0 ? getCreationTime() : this.lastAccessedTime;
    }

    void setLastAccessedTime(long accessTimeMillis) {
        this.lastAccessedTime = accessTimeMillis;
    }

    public List<byte[]> getStatusResponses() {
        if (this.peerCertificateOcspData == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList((byte[]) this.peerCertificateOcspData.clone());
    }

    byte[] getPeerSignedCertificateTimestamp() {
        if (this.peerTlsSctData == null) {
            return null;
        }
        return (byte[]) this.peerTlsSctData.clone();
    }

    String getRequestedServerName() {
        return this.ssl.getRequestedServerName();
    }

    public void invalidate() {
        this.ssl.setTimeout(0);
    }

    public boolean isValid() {
        return System.currentTimeMillis() - this.ssl.getTimeout() < this.ssl.getTime();
    }

    public void putValue(String name, Object value) {
        if (name == null) {
            throw new NullPointerException("name");
        } else if (value == null) {
            throw new NullPointerException("value");
        } else {
            Map<String, Object> values = this.values;
            if (values == null) {
                values = new HashMap(2);
                this.values = values;
            }
            Object old = values.put(name, value);
            if (value instanceof SSLSessionBindingListener) {
                ((SSLSessionBindingListener) value).valueBound(new SSLSessionBindingEvent(this, name));
            }
            if (old instanceof SSLSessionBindingListener) {
                ((SSLSessionBindingListener) old).valueUnbound(new SSLSessionBindingEvent(this, name));
            }
            notifyUnbound(old, name);
        }
    }

    public Object getValue(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        } else if (this.values == null) {
            return null;
        } else {
            return this.values.get(name);
        }
    }

    public void removeValue(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        Map<String, Object> values = this.values;
        if (values != null) {
            notifyUnbound(values.remove(name), name);
        }
    }

    public String[] getValueNames() {
        Map<String, Object> values = this.values;
        if (values == null || values.isEmpty()) {
            return EmptyArray.STRING;
        }
        return (String[]) values.keySet().toArray(new String[values.size()]);
    }

    public X509Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        checkPeerCertificatesPresent();
        return (X509Certificate[]) this.peerCertificates.clone();
    }

    public Certificate[] getLocalCertificates() {
        return this.localCertificates == null ? null : (X509Certificate[]) this.localCertificates.clone();
    }

    public javax.security.cert.X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        checkPeerCertificatesPresent();
        javax.security.cert.X509Certificate[] result = this.peerCertificateChain;
        if (result != null) {
            return result;
        }
        result = SSLUtils.toCertificateChain(this.peerCertificates);
        this.peerCertificateChain = result;
        return result;
    }

    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        checkPeerCertificatesPresent();
        return this.peerCertificates[0].getSubjectX500Principal();
    }

    public Principal getLocalPrincipal() {
        if (this.localCertificates == null || this.localCertificates.length <= 0) {
            return null;
        }
        return this.localCertificates[0].getSubjectX500Principal();
    }

    public String getCipherSuite() {
        if (this.cipherSuite == null) {
            this.cipherSuite = this.ssl.getCipherSuite();
        }
        return this.cipherSuite;
    }

    public String getProtocol() {
        String protocol = this.protocol;
        if (protocol != null) {
            return protocol;
        }
        protocol = this.ssl.getVersion();
        this.protocol = protocol;
        return protocol;
    }

    public String getPeerHost() {
        return this.peerHost;
    }

    public int getPeerPort() {
        return this.peerPort;
    }

    public int getPacketBufferSize() {
        return 16709;
    }

    public int getApplicationBufferSize() {
        return 16384;
    }

    void onPeerCertificatesReceived(String peerHost, int peerPort, OpenSSLX509Certificate[] peerCertificates) {
        configurePeer(peerHost, peerPort, peerCertificates);
    }

    void onSessionEstablished(String peerHost, int peerPort) {
        this.id = null;
        this.localCertificates = this.ssl.getLocalCertificates();
        configurePeer(peerHost, peerPort, this.ssl.getPeerCertificates());
    }

    private void configurePeer(String peerHost, int peerPort, OpenSSLX509Certificate[] peerCertificates) {
        this.peerHost = peerHost;
        this.peerPort = peerPort;
        this.peerCertificates = peerCertificates;
        this.peerCertificateOcspData = this.ssl.getPeerCertificateOcspData();
        this.peerTlsSctData = this.ssl.getPeerTlsSctData();
    }

    private X509Certificate[] getX509PeerCertificates() throws SSLPeerUnverifiedException {
        if (this.peerCertificates != null && this.peerCertificates.length != 0) {
            return this.peerCertificates;
        }
        throw new SSLPeerUnverifiedException("No peer certificates");
    }

    private void checkPeerCertificatesPresent() throws SSLPeerUnverifiedException {
        if (this.peerCertificates == null || this.peerCertificates.length == 0) {
            throw new SSLPeerUnverifiedException("No peer certificates");
        }
    }

    private void notifyUnbound(Object value, String name) {
        if (value instanceof SSLSessionBindingListener) {
            ((SSLSessionBindingListener) value).valueUnbound(new SSLSessionBindingEvent(this, name));
        }
    }
}
