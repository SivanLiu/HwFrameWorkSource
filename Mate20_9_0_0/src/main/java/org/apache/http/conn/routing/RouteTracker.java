package org.apache.http.conn.routing;

import java.net.InetAddress;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.RouteInfo.LayerType;
import org.apache.http.conn.routing.RouteInfo.TunnelType;

@Deprecated
public final class RouteTracker implements RouteInfo, Cloneable {
    private boolean connected;
    private LayerType layered;
    private final InetAddress localAddress;
    private HttpHost[] proxyChain;
    private boolean secure;
    private final HttpHost targetHost;
    private TunnelType tunnelled;

    public RouteTracker(HttpHost target, InetAddress local) {
        if (target != null) {
            this.targetHost = target;
            this.localAddress = local;
            this.tunnelled = TunnelType.PLAIN;
            this.layered = LayerType.PLAIN;
            return;
        }
        throw new IllegalArgumentException("Target host may not be null.");
    }

    public RouteTracker(HttpRoute route) {
        this(route.getTargetHost(), route.getLocalAddress());
    }

    public final void connectTarget(boolean secure) {
        if (this.connected) {
            throw new IllegalStateException("Already connected.");
        }
        this.connected = true;
        this.secure = secure;
    }

    public final void connectProxy(HttpHost proxy, boolean secure) {
        if (proxy == null) {
            throw new IllegalArgumentException("Proxy host may not be null.");
        } else if (this.connected) {
            throw new IllegalStateException("Already connected.");
        } else {
            this.connected = true;
            this.proxyChain = new HttpHost[]{proxy};
            this.secure = secure;
        }
    }

    public final void tunnelTarget(boolean secure) {
        if (!this.connected) {
            throw new IllegalStateException("No tunnel unless connected.");
        } else if (this.proxyChain != null) {
            this.tunnelled = TunnelType.TUNNELLED;
            this.secure = secure;
        } else {
            throw new IllegalStateException("No tunnel without proxy.");
        }
    }

    public final void tunnelProxy(HttpHost proxy, boolean secure) {
        if (proxy == null) {
            throw new IllegalArgumentException("Proxy host may not be null.");
        } else if (!this.connected) {
            throw new IllegalStateException("No tunnel unless connected.");
        } else if (this.proxyChain != null) {
            HttpHost[] proxies = new HttpHost[(this.proxyChain.length + 1)];
            System.arraycopy(this.proxyChain, 0, proxies, 0, this.proxyChain.length);
            proxies[proxies.length - 1] = proxy;
            this.proxyChain = proxies;
            this.secure = secure;
        } else {
            throw new IllegalStateException("No proxy tunnel without proxy.");
        }
    }

    public final void layerProtocol(boolean secure) {
        if (this.connected) {
            this.layered = LayerType.LAYERED;
            this.secure = secure;
            return;
        }
        throw new IllegalStateException("No layered protocol unless connected.");
    }

    public final HttpHost getTargetHost() {
        return this.targetHost;
    }

    public final InetAddress getLocalAddress() {
        return this.localAddress;
    }

    public final int getHopCount() {
        if (!this.connected) {
            return 0;
        }
        if (this.proxyChain == null) {
            return 1;
        }
        return this.proxyChain.length + 1;
    }

    public final HttpHost getHopTarget(int hop) {
        if (hop >= 0) {
            int hopcount = getHopCount();
            if (hop >= hopcount) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Hop index ");
                stringBuilder.append(hop);
                stringBuilder.append(" exceeds tracked route length ");
                stringBuilder.append(hopcount);
                stringBuilder.append(".");
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

    public final boolean isConnected() {
        return this.connected;
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

    public final HttpRoute toRoute() {
        return !this.connected ? null : new HttpRoute(this.targetHost, this.localAddress, this.proxyChain, this.secure, this.tunnelled, this.layered);
    }

    public final boolean equals(Object o) {
        boolean equal = true;
        if (o == this) {
            return true;
        }
        int i = 0;
        if (!(o instanceof RouteTracker)) {
            return false;
        }
        RouteTracker that = (RouteTracker) o;
        boolean equal2 = this.targetHost.equals(that.targetHost);
        int i2 = (this.localAddress == that.localAddress || (this.localAddress != null && this.localAddress.equals(that.localAddress))) ? 1 : 0;
        equal2 &= i2;
        i2 = (this.proxyChain == that.proxyChain || !(this.proxyChain == null || that.proxyChain == null || this.proxyChain.length != that.proxyChain.length)) ? 1 : 0;
        equal2 &= i2;
        if (!(this.connected == that.connected && this.secure == that.secure && this.tunnelled == that.tunnelled && this.layered == that.layered)) {
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
            for (HttpHost hashCode : this.proxyChain) {
                hc ^= hashCode.hashCode();
            }
        }
        if (this.connected) {
            hc ^= 286331153;
        }
        if (this.secure) {
            hc ^= 572662306;
        }
        return (hc ^ this.tunnelled.hashCode()) ^ this.layered.hashCode();
    }

    public final String toString() {
        StringBuilder cab = new StringBuilder(50 + (getHopCount() * 30));
        cab.append("RouteTracker[");
        if (this.localAddress != null) {
            cab.append(this.localAddress);
            cab.append("->");
        }
        cab.append('{');
        if (this.connected) {
            cab.append('c');
        }
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
            for (Object append : this.proxyChain) {
                cab.append(append);
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
