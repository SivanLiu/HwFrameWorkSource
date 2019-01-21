package com.android.okhttp.internal.http;

import com.android.okhttp.Address;
import com.android.okhttp.HttpUrl;
import com.android.okhttp.Route;
import com.android.okhttp.internal.RouteDatabase;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public final class RouteSelector {
    private final Address address;
    private List<InetSocketAddress> inetSocketAddresses = Collections.emptyList();
    private InetSocketAddress lastInetSocketAddress;
    private Proxy lastProxy;
    private int nextInetSocketAddressIndex;
    private int nextProxyIndex;
    private final List<Route> postponedRoutes = new ArrayList();
    private List<Proxy> proxies = Collections.emptyList();
    private final RouteDatabase routeDatabase;

    public RouteSelector(Address address, RouteDatabase routeDatabase) {
        this.address = address;
        this.routeDatabase = routeDatabase;
        resetNextProxy(address.url(), address.getProxy());
    }

    public boolean hasNext() {
        return hasNextInetSocketAddress() || hasNextProxy() || hasNextPostponed();
    }

    public Route next() throws IOException {
        if (!hasNextInetSocketAddress()) {
            if (hasNextProxy()) {
                this.lastProxy = nextProxy();
            } else if (hasNextPostponed()) {
                return nextPostponed();
            } else {
                throw new NoSuchElementException();
            }
        }
        this.lastInetSocketAddress = nextInetSocketAddress();
        Route route = new Route(this.address, this.lastProxy, this.lastInetSocketAddress);
        if (!this.routeDatabase.shouldPostpone(route)) {
            return route;
        }
        this.postponedRoutes.add(route);
        return next();
    }

    public void connectFailed(Route failedRoute, IOException failure) {
        if (!(failedRoute.getProxy().type() == Type.DIRECT || this.address.getProxySelector() == null)) {
            this.address.getProxySelector().connectFailed(this.address.url().uri(), failedRoute.getProxy().address(), failure);
        }
        this.routeDatabase.failed(failedRoute);
    }

    private void resetNextProxy(HttpUrl url, Proxy proxy) {
        if (proxy != null) {
            this.proxies = Collections.singletonList(proxy);
        } else {
            this.proxies = new ArrayList();
            List<Proxy> selectedProxies = this.address.getProxySelector().select(url.uri());
            if (selectedProxies != null) {
                this.proxies.addAll(selectedProxies);
            }
            this.proxies.removeAll(Collections.singleton(Proxy.NO_PROXY));
            this.proxies.add(Proxy.NO_PROXY);
        }
        this.nextProxyIndex = 0;
    }

    private boolean hasNextProxy() {
        return this.nextProxyIndex < this.proxies.size();
    }

    private Proxy nextProxy() throws IOException {
        if (hasNextProxy()) {
            List list = this.proxies;
            int i = this.nextProxyIndex;
            this.nextProxyIndex = i + 1;
            Proxy result = (Proxy) list.get(i);
            resetNextInetSocketAddress(result);
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No route to ");
        stringBuilder.append(this.address.getUriHost());
        stringBuilder.append("; exhausted proxy configurations: ");
        stringBuilder.append(this.proxies);
        throw new SocketException(stringBuilder.toString());
    }

    private void resetNextInetSocketAddress(Proxy proxy) throws IOException {
        String socketHost;
        int socketPort;
        this.inetSocketAddresses = new ArrayList();
        if (proxy.type() == Type.DIRECT || proxy.type() == Type.SOCKS) {
            socketHost = this.address.getUriHost();
            socketPort = this.address.getUriPort();
        } else {
            SocketAddress proxyAddress = proxy.address();
            if (proxyAddress instanceof InetSocketAddress) {
                InetSocketAddress proxySocketAddress = (InetSocketAddress) proxyAddress;
                socketHost = getHostString(proxySocketAddress);
                socketPort = proxySocketAddress.getPort();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Proxy.address() is not an InetSocketAddress: ");
                stringBuilder.append(proxyAddress.getClass());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        if (socketPort < 1 || socketPort > 65535) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("No route to ");
            stringBuilder2.append(socketHost);
            stringBuilder2.append(":");
            stringBuilder2.append(socketPort);
            stringBuilder2.append("; port is out of range");
            throw new SocketException(stringBuilder2.toString());
        }
        if (proxy.type() == Type.SOCKS) {
            this.inetSocketAddresses.add(InetSocketAddress.createUnresolved(socketHost, socketPort));
        } else {
            List<InetAddress> addresses = this.address.getDns().lookup(socketHost);
            int size = addresses.size();
            for (int i = 0; i < size; i++) {
                this.inetSocketAddresses.add(new InetSocketAddress((InetAddress) addresses.get(i), socketPort));
            }
        }
        this.nextInetSocketAddressIndex = 0;
    }

    static String getHostString(InetSocketAddress socketAddress) {
        InetAddress address = socketAddress.getAddress();
        if (address == null) {
            return socketAddress.getHostName();
        }
        return address.getHostAddress();
    }

    private boolean hasNextInetSocketAddress() {
        return this.nextInetSocketAddressIndex < this.inetSocketAddresses.size();
    }

    private InetSocketAddress nextInetSocketAddress() throws IOException {
        if (hasNextInetSocketAddress()) {
            List list = this.inetSocketAddresses;
            int i = this.nextInetSocketAddressIndex;
            this.nextInetSocketAddressIndex = i + 1;
            return (InetSocketAddress) list.get(i);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No route to ");
        stringBuilder.append(this.address.getUriHost());
        stringBuilder.append("; exhausted inet socket addresses: ");
        stringBuilder.append(this.inetSocketAddresses);
        throw new SocketException(stringBuilder.toString());
    }

    private boolean hasNextPostponed() {
        return this.postponedRoutes.isEmpty() ^ 1;
    }

    private Route nextPostponed() {
        return (Route) this.postponedRoutes.remove(0);
    }
}
