package com.android.okhttp;

import com.android.okhttp.HttpUrl.Builder;
import com.android.okhttp.internal.Util;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.List;
import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

public final class Address {
    final Authenticator authenticator;
    final CertificatePinner certificatePinner;
    final List<ConnectionSpec> connectionSpecs;
    final Dns dns;
    final HostnameVerifier hostnameVerifier;
    final List<Protocol> protocols;
    final Proxy proxy;
    final ProxySelector proxySelector;
    final SocketFactory socketFactory;
    final SSLSocketFactory sslSocketFactory;
    final HttpUrl url;

    public Address(String uriHost, int uriPort, Dns dns, SocketFactory socketFactory, SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier, CertificatePinner certificatePinner, Authenticator authenticator, Proxy proxy, List<Protocol> protocols, List<ConnectionSpec> connectionSpecs, ProxySelector proxySelector) {
        this.url = new Builder().scheme(sslSocketFactory != null ? "https" : "http").host(uriHost).port(uriPort).build();
        if (dns != null) {
            this.dns = dns;
            if (socketFactory != null) {
                this.socketFactory = socketFactory;
                if (authenticator != null) {
                    this.authenticator = authenticator;
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
                                return;
                            }
                            throw new IllegalArgumentException("proxySelector == null");
                        }
                        throw new IllegalArgumentException("connectionSpecs == null");
                    }
                    throw new IllegalArgumentException("protocols == null");
                }
                throw new IllegalArgumentException("authenticator == null");
            }
            throw new IllegalArgumentException("socketFactory == null");
        }
        throw new IllegalArgumentException("dns == null");
    }

    public HttpUrl url() {
        return this.url;
    }

    @Deprecated
    public String getUriHost() {
        return this.url.host();
    }

    @Deprecated
    public int getUriPort() {
        return this.url.port();
    }

    public Dns getDns() {
        return this.dns;
    }

    public SocketFactory getSocketFactory() {
        return this.socketFactory;
    }

    public Authenticator getAuthenticator() {
        return this.authenticator;
    }

    public List<Protocol> getProtocols() {
        return this.protocols;
    }

    public List<ConnectionSpec> getConnectionSpecs() {
        return this.connectionSpecs;
    }

    public ProxySelector getProxySelector() {
        return this.proxySelector;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return this.sslSocketFactory;
    }

    public HostnameVerifier getHostnameVerifier() {
        return this.hostnameVerifier;
    }

    public CertificatePinner getCertificatePinner() {
        return this.certificatePinner;
    }

    public boolean equals(Object other) {
        boolean z = false;
        if (!(other instanceof Address)) {
            return false;
        }
        Address that = (Address) other;
        if (this.url.equals(that.url) && this.dns.equals(that.dns) && this.authenticator.equals(that.authenticator) && this.protocols.equals(that.protocols) && this.connectionSpecs.equals(that.connectionSpecs) && this.proxySelector.equals(that.proxySelector) && Util.equal(this.proxy, that.proxy) && Util.equal(this.sslSocketFactory, that.sslSocketFactory) && Util.equal(this.hostnameVerifier, that.hostnameVerifier) && Util.equal(this.certificatePinner, that.certificatePinner)) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        int i = 0;
        int result = 31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * ((31 * 17) + this.url.hashCode())) + this.dns.hashCode())) + this.authenticator.hashCode())) + this.protocols.hashCode())) + this.connectionSpecs.hashCode())) + this.proxySelector.hashCode())) + (this.proxy != null ? this.proxy.hashCode() : 0))) + (this.sslSocketFactory != null ? this.sslSocketFactory.hashCode() : 0))) + (this.hostnameVerifier != null ? this.hostnameVerifier.hashCode() : 0));
        if (this.certificatePinner != null) {
            i = this.certificatePinner.hashCode();
        }
        return result + i;
    }
}
