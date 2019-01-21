package com.huawei.okhttp3;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;

public final class Route {
    final Address address;
    final InetSocketAddress inetSocketAddress;
    final Proxy proxy;

    public Route(Address address, Proxy proxy, InetSocketAddress inetSocketAddress) {
        if (address == null) {
            throw new NullPointerException("address == null");
        } else if (proxy == null) {
            throw new NullPointerException("proxy == null");
        } else if (inetSocketAddress != null) {
            this.address = address;
            this.proxy = proxy;
            this.inetSocketAddress = inetSocketAddress;
        } else {
            throw new NullPointerException("inetSocketAddress == null");
        }
    }

    public Address address() {
        return this.address;
    }

    public Proxy proxy() {
        return this.proxy;
    }

    public InetSocketAddress socketAddress() {
        return this.inetSocketAddress;
    }

    public boolean requiresTunnel() {
        return this.address.sslSocketFactory != null && this.proxy.type() == Type.HTTP;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof Route)) {
            return false;
        }
        Route other = (Route) obj;
        if (this.address.equals(other.address) && this.proxy.equals(other.proxy) && this.inetSocketAddress.equals(other.inetSocketAddress)) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return (31 * ((31 * ((31 * 17) + this.address.hashCode())) + this.proxy.hashCode())) + this.inetSocketAddress.hashCode();
    }
}
