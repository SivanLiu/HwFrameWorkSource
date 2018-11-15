package org.apache.http.conn.routing;

import java.net.InetAddress;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.RouteInfo.LayerType;
import org.apache.http.conn.routing.RouteInfo.TunnelType;

@Deprecated
public final class HttpRoute implements RouteInfo, Cloneable {
    private final LayerType layered;
    private final InetAddress localAddress;
    private final HttpHost[] proxyChain;
    private final boolean secure;
    private final HttpHost targetHost;
    private final TunnelType tunnelled;

    private HttpRoute(InetAddress local, HttpHost target, HttpHost[] proxies, boolean secure, TunnelType tunnelled, LayerType layered) {
        if (target == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        } else if (tunnelled == TunnelType.TUNNELLED && proxies == null) {
            throw new IllegalArgumentException("Proxy required if tunnelled.");
        } else {
            if (tunnelled == null) {
                tunnelled = TunnelType.PLAIN;
            }
            if (layered == null) {
                layered = LayerType.PLAIN;
            }
            this.targetHost = target;
            this.localAddress = local;
            this.proxyChain = proxies;
            this.secure = secure;
            this.tunnelled = tunnelled;
            this.layered = layered;
        }
    }

    public HttpRoute(HttpHost target, InetAddress local, HttpHost[] proxies, boolean secure, TunnelType tunnelled, LayerType layered) {
        this(local, target, toChain(proxies), secure, tunnelled, layered);
    }

    public HttpRoute(HttpHost target, InetAddress local, HttpHost proxy, boolean secure, TunnelType tunnelled, LayerType layered) {
        this(local, target, toChain(proxy), secure, tunnelled, layered);
    }

    public HttpRoute(HttpHost target, InetAddress local, boolean secure) {
        this(local, target, null, secure, TunnelType.PLAIN, LayerType.PLAIN);
    }

    public HttpRoute(HttpHost target) {
        this(null, target, null, false, TunnelType.PLAIN, LayerType.PLAIN);
    }

    public HttpRoute(HttpHost target, InetAddress local, HttpHost proxy, boolean secure) {
        this(local, target, toChain(proxy), secure, secure ? TunnelType.TUNNELLED : TunnelType.PLAIN, secure ? LayerType.LAYERED : LayerType.PLAIN);
        if (proxy == null) {
            throw new IllegalArgumentException("Proxy host may not be null.");
        }
    }

    private static HttpHost[] toChain(HttpHost proxy) {
        if (proxy == null) {
            return null;
        }
        return new HttpHost[]{proxy};
    }

    private static HttpHost[] toChain(HttpHost[] proxies) {
        if (proxies == null || proxies.length < 1) {
            return null;
        }
        int length = proxies.length;
        int i = 0;
        while (i < length) {
            if (proxies[i] != null) {
                i++;
            } else {
                throw new IllegalArgumentException("Proxy chain may not contain null elements.");
            }
        }
        HttpHost[] result = new HttpHost[proxies.length];
        System.arraycopy(proxies, 0, result, 0, proxies.length);
        return result;
    }

    public final HttpHost getTargetHost() {
        return this.targetHost;
    }

    public final InetAddress getLocalAddress() {
        return this.localAddress;
    }

    public final int getHopCount() {
        return this.proxyChain == null ? 1 : 1 + this.proxyChain.length;
    }

    public final HttpHost getHopTarget(int hop) {
        if (hop >= 0) {
            int hopcount = getHopCount();
            if (hop >= hopcount) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Hop index ");
                stringBuilder.append(hop);
                stringBuilder.append(" exceeds route length ");
                stringBuilder.append(hopcount);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (hop < hopcount - 1) {
                return this.proxyChain[hop];
            } else {
                return this.targetHost;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Hop index must not be negative: ");
        stringBuilder2.append(hop);
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    public final HttpHost getProxyHost() {
        return this.proxyChain == null ? null : this.proxyChain[0];
    }

    public final TunnelType getTunnelType() {
        return this.tunnelled;
    }

    public final boolean isTunnelled() {
        return this.tunnelled == TunnelType.TUNNELLED;
    }

    public final LayerType getLayerType() {
        return this.layered;
    }

    public final boolean isLayered() {
        return this.layered == LayerType.LAYERED;
    }

    public final boolean isSecure() {
        return this.secure;
    }

    public final boolean equals(Object o) {
        boolean equal = true;
        if (o == this) {
            return true;
        }
        int i = 0;
        if (!(o instanceof HttpRoute)) {
            return false;
        }
        HttpRoute that = (HttpRoute) o;
        boolean equal2 = this.targetHost.equals(that.targetHost);
        int i2 = (this.localAddress == that.localAddress || (this.localAddress != null && this.localAddress.equals(that.localAddress))) ? 1 : 0;
        equal2 &= i2;
        i2 = (this.proxyChain == that.proxyChain || !(this.proxyChain == null || that.proxyChain == null || this.proxyChain.length != that.proxyChain.length)) ? 1 : 0;
        equal2 &= i2;
        if (!(this.secure == that.secure && this.tunnelled == that.tunnelled && this.layered == that.layered)) {
            equal = false;
        }
        equal &= equal2;
        if (equal && this.proxyChain != null) {
            while (equal && i < this.proxyChain.length) {
                equal = this.proxyChain[i].equals(that.proxyChain[i]);
                i++;
            }
        }
        return equal;
    }

    public final int hashCode() {
        int hc = this.targetHost.hashCode();
        if (this.localAddress != null) {
            hc ^= this.localAddress.hashCode();
        }
        if (this.proxyChain != null) {
            hc ^= this.proxyChain.length;
            for (HttpHost aProxyChain : this.proxyChain) {
                hc ^= aProxyChain.hashCode();
            }
        }
        if (this.secure) {
            hc ^= 286331153;
        }
        return (hc ^ this.tunnelled.hashCode()) ^ this.layered.hashCode();
    }

    public final String toString() {
        StringBuilder cab = new StringBuilder(50 + (getHopCount() * 30));
        cab.append("HttpRoute[");
        if (this.localAddress != null) {
            cab.append(this.localAddress);
            cab.append("->");
        }
        cab.append('{');
        if (this.tunnelled == TunnelType.TUNNELLED) {
            cab.append('t');
        }
        if (this.layered == LayerType.LAYERED) {
            cab.append('l');
        }
        if (this.secure) {
            cab.append('s');
        }
        cab.append("}->");
        if (this.proxyChain != null) {
            for (HttpHost aProxyChain : this.proxyChain) {
                cab.append(aProxyChain);
                cab.append("->");
            }
        }
        cab.append(this.targetHost);
        cab.append(']');
        return cab.toString();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
