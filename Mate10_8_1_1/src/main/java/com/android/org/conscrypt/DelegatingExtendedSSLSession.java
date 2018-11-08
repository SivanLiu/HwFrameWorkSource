package com.android.org.conscrypt;

import java.lang.reflect.InvocationTargetException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSessionContext;
import javax.security.cert.X509Certificate;

final class DelegatingExtendedSSLSession extends ExtendedSSLSession {
    private static final String[] LOCAL_SUPPORTED_SIGNATURE_ALGORITHMS = new String[]{"SHA512withRSA", "SHA512withECDSA", "SHA384withRSA", "SHA384withECDSA", "SHA256withRSA", "SHA256withECDSA", "SHA224withRSA", "SHA224withECDSA", "SHA1withRSA", "SHA1withECDSA"};
    private static final String[] PEER_SUPPORTED_SIGNATURE_ALGORITHMS = new String[]{"SHA1withRSA", "SHA1withECDSA"};
    private final ActiveSession delegate;

    DelegatingExtendedSSLSession(ActiveSession delegate) {
        this.delegate = delegate;
    }

    ActiveSession getDelegate() {
        return this.delegate;
    }

    public String[] getLocalSupportedSignatureAlgorithms() {
        return (String[]) LOCAL_SUPPORTED_SIGNATURE_ALGORITHMS.clone();
    }

    public String[] getPeerSupportedSignatureAlgorithms() {
        return (String[]) PEER_SUPPORTED_SIGNATURE_ALGORITHMS.clone();
    }

    public List getRequestedServerNames() {
        try {
            if (this.delegate.getRequestedServerName() == null) {
                return null;
            }
            return Collections.singletonList(Class.forName("javax.net.ssl.SNIHostName").getConstructor(new Class[]{String.class}).newInstance(new Object[]{requestedServerName}));
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InvocationTargetException e2) {
            return null;
        } catch (IllegalAccessException e3) {
            return null;
        } catch (ClassNotFoundException e4) {
            return null;
        } catch (InstantiationException e5) {
            return null;
        }
    }

    public byte[] getId() {
        return this.delegate.getId();
    }

    public SSLSessionContext getSessionContext() {
        return this.delegate.getSessionContext();
    }

    public long getCreationTime() {
        return this.delegate.getCreationTime();
    }

    public long getLastAccessedTime() {
        return this.delegate.getLastAccessedTime();
    }

    public void invalidate() {
        this.delegate.invalidate();
    }

    public boolean isValid() {
        return this.delegate.isValid();
    }

    public void putValue(String name, Object value) {
        this.delegate.putValue(name, value);
    }

    public Object getValue(String name) {
        return this.delegate.getValue(name);
    }

    public void removeValue(String name) {
        this.delegate.removeValue(name);
    }

    public String[] getValueNames() {
        return this.delegate.getValueNames();
    }

    public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
        return this.delegate.getPeerCertificates();
    }

    public Certificate[] getLocalCertificates() {
        return this.delegate.getLocalCertificates();
    }

    public X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
        return this.delegate.getPeerCertificateChain();
    }

    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return this.delegate.getPeerPrincipal();
    }

    public Principal getLocalPrincipal() {
        return this.delegate.getLocalPrincipal();
    }

    public String getCipherSuite() {
        return this.delegate.getCipherSuite();
    }

    public String getProtocol() {
        return this.delegate.getProtocol();
    }

    public String getPeerHost() {
        return this.delegate.getPeerHost();
    }

    public int getPeerPort() {
        return this.delegate.getPeerPort();
    }

    public int getPacketBufferSize() {
        return this.delegate.getPacketBufferSize();
    }

    public int getApplicationBufferSize() {
        return this.delegate.getApplicationBufferSize();
    }
}
