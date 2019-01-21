package com.huawei.okhttp3;

import com.huawei.okhttp3.HttpUrl.Builder;
import com.huawei.okhttp3.internal.Util;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.List;
import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

public final class Address {
    final CertificatePinner certificatePinner;
    final List<ConnectionSpec> connectionSpecs;
    final Dns dns;
    private String headerHost;
    final HostnameVerifier hostnameVerifier;
    final List<Protocol> protocols;
    final Proxy proxy;
    final Authenticator proxyAuthenticator;
    final ProxySelector proxySelector;
    final SocketFactory socketFactory;
    final SSLSocketFactory sslSocketFactory;
    final HttpUrl url;

    public Address(String uriHost, int uriPort, Dns dns, SocketFactory socketFactory, SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier, CertificatePinner certificatePinner, Authenticator proxyAuthenticator, Proxy proxy, List<Protocol> protocols, List<ConnectionSpec> connectionSpecs, ProxySelector proxySelector) {
        this.url = new Builder().scheme(sslSocketFactory != null ? "https" : "http").host(uriHost).port(uriPort).build();
        if (dns != null) {
            this.dns = dns;
            if (socketFactory != null) {
                this.socketFactory = socketFactory;
                if (proxyAuthenticator != null) {
                    this.proxyAuthenticator = proxyAuthenticator;
                    if (protocols != null) {
                        this.protocols = Util.immutableList((List) protocols);
                        if (connectionSpecs != null) {
                            this.connectionSpecs = Util.immutableList((List) connectionSpecs);
                            if (proxySelector != null) {
                                this.proxySelector = proxySelector;
                                this.proxy = proxy;
                                this.sslSocketFactory = sslSocketFactory;
                                this.hostnameVerifier = hostnameVerifier;
                                this.certificatePinner = certificatePinner;
                                this.headerHost = null;
                                return;
                            }
                            throw new NullPointerException("proxySelector == null");
                        }
                        throw new NullPointerException("connectionSpecs == null");
                    }
                    throw new NullPointerException("protocols == null");
                }
                throw new NullPointerException("proxyAuthenticator == null");
            }
            throw new NullPointerException("socketFactory == null");
        }
        throw new NullPointerException("dns == null");
    }

    public String headerHost() {
        return this.headerHost;
    }

    public void setHeaderHost(String headerHost) {
        this.headerHost = headerHost;
    }

    public HttpUrl url() {
        return this.url;
    }

    public Dns dns() {
        return this.dns;
    }

    public SocketFactory socketFactory() {
        return this.socketFactory;
    }

    public Authenticator proxyAuthenticator() {
        return this.proxyAuthenticator;
    }

    public List<Protocol> protocols() {
        return this.protocols;
    }

    public List<ConnectionSpec> connectionSpecs() {
        return this.connectionSpecs;
    }

    public ProxySelector proxySelector() {
        return this.proxySelector;
    }

    public Proxy proxy() {
        return this.proxy;
    }

    public SSLSocketFactory sslSocketFactory() {
        return this.sslSocketFactory;
    }

    public HostnameVerifier hostnameVerifier() {
        return this.hostnameVerifier;
    }

    public CertificatePinner certificatePinner() {
        return this.certificatePinner;
    }

    public boolean equals(Object other) {
        boolean z = false;
        if (!(other instanceof Address)) {
            return false;
        }
        Address that = (Address) other;
        if (this.url.equals(that.url) && this.dns.equals(that.dns) && this.proxyAuthenticator.equals(that.proxyAuthenticator) && this.protocols.equals(that.protocols) && this.connectionSpecs.equals(that.connectionSpecs) && this.proxySelector.equals(that.proxySelector) && Util.equal(this.proxy, that.proxy) && Util.equal(this.sslSocketFactory, that.sslSocketFactory) && Util.equal(this.hostnameVerifier, that.hostnameVerifier) && Util.equal(this.certificatePinner, that.certificatePinner)) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        int i = 0;
        int result = 31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * 17) + this.url.hashCode())) + this.dns.hashCode())) + this.proxyAuthenticator.hashCode())) + this.protocols.hashCode())) + this.connectionSpecs.hashCode())) + this.proxySelector.hashCode())) + (this.proxy != null ? this.proxy.hashCode() : 0))) + (this.sslSocketFactory != null ? this.sslSocketFactory.hashCode() : 0))) + (this.hostnameVerifier != null ? this.hostnameVerifier.hashCode() : 0));
        if (this.certificatePinner != null) {
            i = this.certificatePinner.hashCode();
        }
        return result + i;
    }
}
