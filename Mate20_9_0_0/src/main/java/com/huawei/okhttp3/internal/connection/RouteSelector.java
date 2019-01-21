package com.huawei.okhttp3.internal.connection;

import com.huawei.okhttp3.Address;
import com.huawei.okhttp3.HttpUrl;
import com.huawei.okhttp3.Route;
import com.huawei.okhttp3.internal.Util;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public final class RouteSelector {
    private final List<InetAddress> additionalInetAddresses;
    private final Address address;
    private boolean concurrentConnectEnabled;
    private InetSocketAddress connectedTcpAddress;
    private List<InetSocketAddress> inetSocketAddresses;
    private InetSocketAddress lastInetSocketAddress;
    private Proxy lastProxy;
    private int nextInetSocketAddressIndex;
    private int nextProxyIndex;
    private final List<Route> postponedRoutes;
    private List<Proxy> proxies;
    private final RouteDatabase routeDatabase;

    public RouteSelector(Address address, RouteDatabase routeDatabase, boolean concurrentConnectEnabled, List<InetAddress> additionalInetAddresses) {
        this.proxies = Collections.emptyList();
        this.inetSocketAddresses = Collections.emptyList();
        this.postponedRoutes = new ArrayList();
        this.connectedTcpAddress = null;
        this.address = address;
        this.routeDatabase = routeDatabase;
        this.concurrentConnectEnabled = concurrentConnectEnabled;
        this.additionalInetAddresses = additionalInetAddresses;
        resetNextProxy(address.url(), address.proxy());
        if (!this.concurrentConnectEnabled) {
            return;
        }
        if (this.proxies.size() > 1 || (this.proxies.size() == 1 && ((Proxy) this.proxies.get(0)).type() != Type.DIRECT)) {
            this.concurrentConnectEnabled = false;
        }
    }

    public RouteSelector(Address address, RouteDatabase routeDatabase) {
        this(address, routeDatabase, false, null);
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
        if (this.concurrentConnectEnabled || !this.routeDatabase.shouldPostpone(route)) {
            if (this.concurrentConnectEnabled && this.inetSocketAddresses.size() == 1) {
                this.concurrentConnectEnabled = false;
                this.nextInetSocketAddressIndex++;
            }
            return route;
        }
        this.postponedRoutes.add(route);
        return next();
    }

    public void connectFailed(Route failedRoute, IOException failure) {
        if (!(failedRoute.proxy().type() == Type.DIRECT || this.address.proxySelector() == null)) {
            this.address.proxySelector().connectFailed(this.address.url().uri(), failedRoute.proxy().address(), failure);
        }
        if (!this.concurrentConnectEnabled) {
            this.routeDatabase.failed(failedRoute);
        } else if (this.connectedTcpAddress != null) {
            this.inetSocketAddresses.remove(this.connectedTcpAddress);
            this.routeDatabase.failed(new Route(this.address, this.lastProxy, this.connectedTcpAddress));
            this.connectedTcpAddress = null;
        }
    }

    public void connected(Route route) {
        if (!this.concurrentConnectEnabled) {
            this.routeDatabase.connected(route);
        } else if (this.connectedTcpAddress != null) {
            this.inetSocketAddresses.remove(this.connectedTcpAddress);
            this.routeDatabase.connected(new Route(this.address, this.lastProxy, this.connectedTcpAddress));
            this.connectedTcpAddress = null;
        }
    }

    public ArrayList<InetSocketAddress> concurrentInetSocketAddresses() {
        return (ArrayList) this.inetSocketAddresses;
    }

    public boolean concurrentConnectEnabled() {
        return this.concurrentConnectEnabled;
    }

    public void setConnectedTcpAddress(InetSocketAddress connectedTcpAddress) {
        this.connectedTcpAddress = connectedTcpAddress;
    }

    public void setFailedTcpAddresses(ArrayList<InetSocketAddress> failedAddresses) {
        Iterator it = failedAddresses.iterator();
        while (it.hasNext()) {
            InetSocketAddress failedAddress = (InetSocketAddress) it.next();
            this.inetSocketAddresses.remove(failedAddress);
            this.routeDatabase.failed(new Route(this.address, this.lastProxy, failedAddress));
        }
    }

    private void resetNextProxy(HttpUrl url, Proxy proxy) {
        if (proxy != null) {
            this.proxies = Collections.singletonList(proxy);
        } else {
            List immutableList;
            List proxiesOrNull = this.address.proxySelector().select(url.uri());
            if (proxiesOrNull == null || proxiesOrNull.isEmpty()) {
                immutableList = Util.immutableList(Proxy.NO_PROXY);
            } else {
                immutableList = Util.immutableList(proxiesOrNull);
            }
            this.proxies = immutableList;
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
        stringBuilder.append(this.address.url().host());
        stringBuilder.append("; exhausted proxy configurations: ");
        stringBuilder.append(this.proxies);
        throw new SocketException(stringBuilder.toString());
    }

    private void resetNextInetSocketAddress(Proxy proxy) throws IOException {
        String socketHost;
        int socketPort;
        this.inetSocketAddresses = new ArrayList();
        if (proxy.type() == Type.DIRECT || proxy.type() == Type.SOCKS) {
            socketHost = this.address.url().host();
            socketPort = this.address.url().port();
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
            List<InetAddress> addresses = this.address.dns().lookup(socketHost);
            int size = addresses.size();
            for (int i = 0; i < size; i++) {
                this.inetSocketAddresses.add(new InetSocketAddress((InetAddress) addresses.get(i), socketPort));
            }
        }
        prepareConcurrentConnectAddresses(socketPort);
        this.nextInetSocketAddressIndex = 0;
    }

    private void prepareConcurrentConnectAddresses(int socketPort) {
        if (this.concurrentConnectEnabled) {
            if (this.additionalInetAddresses != null) {
                for (int i = this.additionalInetAddresses.size() - 1; i >= 0; i--) {
                    InetSocketAddress socketAddress = new InetSocketAddress((InetAddress) this.additionalInetAddresses.get(i), socketPort);
                    if (this.inetSocketAddresses.contains(socketAddress)) {
                        this.inetSocketAddresses.remove(socketAddress);
                    }
                    this.inetSocketAddresses.add(0, socketAddress);
                }
            }
            Proxy proxy = (Proxy) this.proxies.get(0);
            ArrayList<InetSocketAddress> postponedAddresses = new ArrayList();
            Iterator<InetSocketAddress> socketAddressesIterator = this.inetSocketAddresses.iterator();
            while (socketAddressesIterator.hasNext()) {
                InetSocketAddress socketAddress2 = (InetSocketAddress) socketAddressesIterator.next();
                if (this.routeDatabase.shouldPostpone(new Route(this.address, proxy, socketAddress2))) {
                    postponedAddresses.add(socketAddress2);
                    socketAddressesIterator.remove();
                }
            }
            this.inetSocketAddresses.addAll(postponedAddresses);
        }
    }

    static String getHostString(InetSocketAddress socketAddress) {
        InetAddress address = socketAddress.getAddress();
        if (address == null) {
            return socketAddress.getHostName();
        }
        return address.getHostAddress();
    }

    private boolean hasNextInetSocketAddress() {
        if (this.concurrentConnectEnabled) {
            if (this.inetSocketAddresses.size() <= 0) {
                return false;
            }
        } else if (this.nextInetSocketAddressIndex >= this.inetSocketAddresses.size()) {
            return false;
        }
        return true;
    }

    private InetSocketAddress nextInetSocketAddress() throws IOException {
        if (hasNextInetSocketAddress()) {
            List list;
            int i;
            if (this.concurrentConnectEnabled) {
                list = this.inetSocketAddresses;
                i = 0;
            } else {
                list = this.inetSocketAddresses;
                i = this.nextInetSocketAddressIndex;
                this.nextInetSocketAddressIndex = i + 1;
            }
            return (InetSocketAddress) list.get(i);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No route to ");
        stringBuilder.append(this.address.url().host());
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
