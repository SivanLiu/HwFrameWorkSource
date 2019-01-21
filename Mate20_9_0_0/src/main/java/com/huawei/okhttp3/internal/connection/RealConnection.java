package com.huawei.okhttp3.internal.connection;

import com.huawei.okhttp3.Address;
import com.huawei.okhttp3.CertificatePinner;
import com.huawei.okhttp3.Connection;
import com.huawei.okhttp3.ConnectionSpec;
import com.huawei.okhttp3.Handshake;
import com.huawei.okhttp3.HttpUrl;
import com.huawei.okhttp3.Protocol;
import com.huawei.okhttp3.Request;
import com.huawei.okhttp3.Response;
import com.huawei.okhttp3.Route;
import com.huawei.okhttp3.internal.Util;
import com.huawei.okhttp3.internal.Version;
import com.huawei.okhttp3.internal.http.HttpHeaders;
import com.huawei.okhttp3.internal.http1.Http1Codec;
import com.huawei.okhttp3.internal.http2.ErrorCode;
import com.huawei.okhttp3.internal.http2.Http2Connection;
import com.huawei.okhttp3.internal.http2.Http2Connection.Builder;
import com.huawei.okhttp3.internal.http2.Http2Connection.Listener;
import com.huawei.okhttp3.internal.http2.Http2Stream;
import com.huawei.okhttp3.internal.platform.Platform;
import com.huawei.okhttp3.internal.tls.OkHostnameVerifier;
import com.huawei.okio.BufferedSink;
import com.huawei.okio.BufferedSource;
import com.huawei.okio.Okio;
import com.huawei.okio.Source;
import java.io.IOException;
import java.lang.ref.Reference;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownServiceException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;

public final class RealConnection extends Listener implements Connection {
    private static final String NPE_THROW_WITH_NULL = "throw with null exception";
    public static final long maxReserveDurationNs = 1000000000;
    public int allocationLimit;
    public final List<Reference<StreamAllocation>> allocations = new ArrayList();
    private ConcurrentConnect concurrentConnect = null;
    private Route connectedRoute = null;
    private Handshake handshake;
    public volatile Http2Connection http2Connection;
    public long idleAtNanos = Long.MAX_VALUE;
    public long keepaliveTimestampNs = 0;
    public boolean noNewStreams;
    private Protocol protocol;
    private Socket rawSocket;
    private final Route route;
    private RouteSelector routeSelector = null;
    public BufferedSink sink;
    public Socket socket;
    public BufferedSource source;
    public int successCount;

    public RealConnection(Route route) {
        this.route = route;
    }

    public void prepareConcurrentConnect(ArrayList<InetSocketAddress> concurrentInetSocketAddresses, int connectionAttemptDelay) {
        if (concurrentInetSocketAddresses != null) {
            this.concurrentConnect = new ConcurrentConnect(concurrentInetSocketAddresses, connectionAttemptDelay);
        }
    }

    public void setRouteSelector(RouteSelector routeSelector) {
        this.routeSelector = routeSelector;
    }

    public void connect(int connectTimeout, int readTimeout, int writeTimeout, List<ConnectionSpec> connectionSpecs, boolean connectionRetryEnabled) {
        if (this.protocol == null) {
            RouteException routeException = null;
            ConnectionSpecSelector connectionSpecSelector = new ConnectionSpecSelector(connectionSpecs);
            if (this.route.address().sslSocketFactory() == null) {
                if (connectionSpecs.contains(ConnectionSpec.CLEARTEXT)) {
                    String host = this.route.address().url().host();
                    if (!Platform.get().isCleartextTrafficPermitted(host)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("CLEARTEXT communication to ");
                        stringBuilder.append(host);
                        stringBuilder.append(" not permitted by network security policy");
                        throw new RouteException(new UnknownServiceException(stringBuilder.toString()));
                    }
                }
                throw new RouteException(new UnknownServiceException("CLEARTEXT communication not enabled for client"));
            }
            while (this.protocol == null) {
                try {
                    if (this.route.requiresTunnel()) {
                        buildTunneledConnection(connectTimeout, readTimeout, writeTimeout, connectionSpecSelector);
                    } else {
                        buildConnection(connectTimeout, readTimeout, writeTimeout, connectionSpecSelector);
                    }
                } catch (IOException e) {
                    Util.closeQuietly(this.socket);
                    Util.closeQuietly(this.rawSocket);
                    this.socket = null;
                    this.rawSocket = null;
                    this.source = null;
                    this.sink = null;
                    this.handshake = null;
                    this.protocol = null;
                    if (routeException == null) {
                        routeException = new RouteException(e);
                    } else {
                        routeException.addConnectException(e);
                    }
                    if (!connectionRetryEnabled || !connectionSpecSelector.connectionFailed(e)) {
                        throw routeException;
                    }
                }
            }
            return;
        }
        throw new IllegalStateException("already connected");
    }

    private void buildTunneledConnection(int connectTimeout, int readTimeout, int writeTimeout, ConnectionSpecSelector connectionSpecSelector) throws IOException {
        Request tunnelRequest = createTunnelRequest();
        HttpUrl url = tunnelRequest.url();
        int attemptedConnections = 0;
        while (true) {
            attemptedConnections++;
            if (attemptedConnections <= 21) {
                connectSocket(connectTimeout, readTimeout);
                tunnelRequest = createTunnel(readTimeout, writeTimeout, tunnelRequest, url);
                if (tunnelRequest == null) {
                    establishProtocol(readTimeout, writeTimeout, connectionSpecSelector);
                    return;
                }
                Util.closeQuietly(this.rawSocket);
                this.rawSocket = null;
                this.sink = null;
                this.source = null;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Too many tunnel connections attempted: ");
                stringBuilder.append(21);
                throw new ProtocolException(stringBuilder.toString());
            }
        }
    }

    private void buildConnection(int connectTimeout, int readTimeout, int writeTimeout, ConnectionSpecSelector connectionSpecSelector) throws IOException {
        connectSocket(connectTimeout, readTimeout);
        establishProtocol(readTimeout, writeTimeout, connectionSpecSelector);
    }

    private void connectSocket(int connectTimeout, int readTimeout) throws IOException {
        Route targetRoute;
        if (this.concurrentConnect == null || this.connectedRoute != null) {
            Socket createSocket;
            targetRoute = this.connectedRoute != null ? this.connectedRoute : this.route;
            Proxy proxy = targetRoute.proxy();
            Address address = targetRoute.address();
            if (proxy.type() == Type.DIRECT || proxy.type() == Type.HTTP) {
                createSocket = address.socketFactory().createSocket();
            } else {
                createSocket = new Socket(proxy);
            }
            this.rawSocket = createSocket;
            this.rawSocket.setSoTimeout(readTimeout);
            try {
                Platform.get().connectSocket(this.rawSocket, targetRoute.socketAddress(), connectTimeout);
            } catch (ConnectException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to connect to ");
                stringBuilder.append(targetRoute.socketAddress());
                ConnectException ce = new ConnectException(stringBuilder.toString());
                ce.initCause(e);
                throw ce;
            }
        }
        this.rawSocket = this.concurrentConnect.getConnectedSocket((long) connectTimeout);
        if (this.routeSelector != null) {
            this.routeSelector.setFailedTcpAddresses(this.concurrentConnect.failedAddressList());
            if (this.rawSocket != null) {
                this.routeSelector.setConnectedTcpAddress((InetSocketAddress) this.rawSocket.getRemoteSocketAddress());
            }
        }
        if (this.rawSocket != null) {
            this.connectedRoute = new Route(this.route.address(), this.route.proxy(), (InetSocketAddress) this.rawSocket.getRemoteSocketAddress());
            this.rawSocket.setSoTimeout(readTimeout);
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to connect to host ");
            stringBuilder2.append(this.route.address().url().host());
            throw new ConnectException(stringBuilder2.toString());
        }
        try {
            this.source = Okio.buffer(Okio.source(this.rawSocket));
            this.sink = Okio.buffer(Okio.sink(this.rawSocket));
        } catch (NullPointerException targetRoute2) {
            if (NPE_THROW_WITH_NULL.equals(targetRoute2.getMessage())) {
                throw new IOException(targetRoute2);
            }
        }
    }

    private void establishProtocol(int readTimeout, int writeTimeout, ConnectionSpecSelector connectionSpecSelector) throws IOException {
        if (this.route.address().sslSocketFactory() != null) {
            connectTls(readTimeout, writeTimeout, connectionSpecSelector);
        } else {
            this.protocol = Protocol.HTTP_1_1;
            this.socket = this.rawSocket;
        }
        if (this.protocol == Protocol.HTTP_2) {
            this.socket.setSoTimeout(0);
            Http2Connection http2Connection = new Builder(true).socket(this.socket, this.route.address().url().host(), this.source, this.sink).listener(this).build();
            http2Connection.start();
            this.allocationLimit = http2Connection.maxConcurrentStreams();
            this.http2Connection = http2Connection;
            return;
        }
        this.allocationLimit = 1;
    }

    private void connectTls(int readTimeout, int writeTimeout, ConnectionSpecSelector connectionSpecSelector) throws IOException {
        Address address = this.route.address();
        String maybeProtocol = null;
        Socket sslSocket = null;
        try {
            sslSocket = (SSLSocket) address.sslSocketFactory().createSocket(this.rawSocket, address.url().host(), address.url().port(), true);
            String headerHost = address.headerHost();
            if (headerHost == null || headerHost.length() == 0) {
                headerHost = address.url().host();
            }
            ConnectionSpec connectionSpec = connectionSpecSelector.configureSecureSocket(sslSocket);
            if (connectionSpec.supportsTlsExtensions()) {
                Platform.get().configureTlsExtensions(sslSocket, headerHost, address.protocols());
            }
            sslSocket.startHandshake();
            Handshake unverifiedHandshake = Handshake.get(sslSocket.getSession());
            if (address.hostnameVerifier().verify(headerHost, sslSocket.getSession())) {
                Protocol protocol;
                address.certificatePinner().check(address.url().host(), unverifiedHandshake.peerCertificates());
                if (connectionSpec.supportsTlsExtensions()) {
                    maybeProtocol = Platform.get().getSelectedProtocol(sslSocket);
                }
                this.socket = sslSocket;
                this.source = Okio.buffer(Okio.source(this.socket));
                this.sink = Okio.buffer(Okio.sink(this.socket));
                this.handshake = unverifiedHandshake;
                if (maybeProtocol != null) {
                    protocol = Protocol.get(maybeProtocol);
                } else {
                    protocol = Protocol.HTTP_1_1;
                }
                this.protocol = protocol;
                if (sslSocket != null) {
                    Platform.get().afterHandshake(sslSocket);
                }
                if (!true) {
                    Util.closeQuietly(sslSocket);
                    return;
                }
                return;
            }
            X509Certificate cert = (X509Certificate) unverifiedHandshake.peerCertificates().get(0);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Hostname ");
            stringBuilder.append(address.url().host());
            stringBuilder.append(" not verified:\n    certificate: ");
            stringBuilder.append(CertificatePinner.pin(cert));
            stringBuilder.append("\n    DN: ");
            stringBuilder.append(cert.getSubjectDN().getName());
            stringBuilder.append("\n    subjectAltNames: ");
            stringBuilder.append(OkHostnameVerifier.allSubjectAltNames(cert));
            throw new SSLPeerUnverifiedException(stringBuilder.toString());
        } catch (AssertionError e) {
            if (Util.isAndroidGetsocknameError(e)) {
                throw new IOException(e);
            }
            throw e;
        } catch (Throwable th) {
            if (sslSocket != null) {
                Platform.get().afterHandshake(sslSocket);
            }
            if (!false) {
                Util.closeQuietly(sslSocket);
            }
        }
    }

    private Request createTunnel(int readTimeout, int writeTimeout, Request tunnelRequest, HttpUrl url) throws IOException {
        String requestLine = new StringBuilder();
        requestLine.append("CONNECT ");
        requestLine.append(Util.hostHeader(url, true));
        requestLine.append(" HTTP/1.1");
        requestLine = requestLine.toString();
        while (true) {
            Http1Codec tunnelConnection = new Http1Codec(null, null, this.source, this.sink);
            this.source.timeout().timeout((long) readTimeout, TimeUnit.MILLISECONDS);
            this.sink.timeout().timeout((long) writeTimeout, TimeUnit.MILLISECONDS);
            tunnelConnection.writeRequest(tunnelRequest.headers(), requestLine);
            tunnelConnection.finishRequest();
            Response response = tunnelConnection.readResponse().request(tunnelRequest).build();
            long contentLength = HttpHeaders.contentLength(response);
            if (contentLength == -1) {
                contentLength = 0;
            }
            Source body = tunnelConnection.newFixedLengthSource(contentLength);
            Util.skipAll(body, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
            body.close();
            int code = response.code();
            if (code != 200) {
                if (code == 407) {
                    tunnelRequest = this.route.address().proxyAuthenticator().authenticate(this.route, response);
                    if (tunnelRequest == null) {
                        throw new IOException("Failed to authenticate with proxy");
                    } else if ("close".equalsIgnoreCase(response.header("Connection"))) {
                        return tunnelRequest;
                    }
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected response code for CONNECT: ");
                    stringBuilder.append(response.code());
                    throw new IOException(stringBuilder.toString());
                }
            } else if (this.source.buffer().exhausted() && this.sink.buffer().exhausted()) {
                return null;
            } else {
                throw new IOException("TLS tunnel buffered too many bytes!");
            }
        }
    }

    private Request createTunnelRequest() {
        return new Request.Builder().url(this.route.address().url()).header("Host", Util.hostHeader(this.route.address().url(), true)).header("Proxy-Connection", "Keep-Alive").header("User-Agent", Version.userAgent()).build();
    }

    public boolean isEligible(Address address) {
        return this.allocations.size() < this.allocationLimit && address.equals(route().address()) && !this.noNewStreams;
    }

    public Route route() {
        return this.route;
    }

    public void cancel() {
        if (this.concurrentConnect != null) {
            this.concurrentConnect.cancel();
        }
        Util.closeQuietly(this.rawSocket);
    }

    public Socket socket() {
        return this.socket;
    }

    public boolean isHealthy(boolean doExtensiveChecks) {
        if (this.socket.isClosed() || this.socket.isInputShutdown() || this.socket.isOutputShutdown()) {
            return false;
        }
        if (this.http2Connection != null) {
            return this.http2Connection.isShutdown() ^ 1;
        }
        if (doExtensiveChecks) {
            int readTimeout;
            try {
                readTimeout = this.socket.getSoTimeout();
                this.socket.setSoTimeout(1);
                if (this.source.exhausted()) {
                    this.socket.setSoTimeout(readTimeout);
                    return false;
                }
                this.socket.setSoTimeout(readTimeout);
                return true;
            } catch (SocketTimeoutException e) {
            } catch (IOException e2) {
                return false;
            } catch (Throwable th) {
                this.socket.setSoTimeout(readTimeout);
            }
        }
        return true;
    }

    public void onStream(Http2Stream stream) throws IOException {
        stream.close(ErrorCode.REFUSED_STREAM);
    }

    public void onSettings(Http2Connection connection) {
        this.allocationLimit = connection.maxConcurrentStreams();
    }

    public Handshake handshake() {
        return this.handshake;
    }

    public boolean isMultiplexed() {
        return this.http2Connection != null;
    }

    public Protocol protocol() {
        if (this.http2Connection != null) {
            return Protocol.HTTP_2;
        }
        return this.protocol != null ? this.protocol : Protocol.HTTP_1_1;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Connection{");
        stringBuilder.append(this.route.address().url().host());
        stringBuilder.append(":");
        stringBuilder.append(this.route.address().url().port());
        stringBuilder.append(", proxy=");
        stringBuilder.append(this.route.proxy());
        stringBuilder.append(" hostAddress=");
        stringBuilder.append(this.route.socketAddress());
        stringBuilder.append(" cipherSuite=");
        stringBuilder.append(this.handshake != null ? this.handshake.cipherSuite() : "none");
        stringBuilder.append(" protocol=");
        stringBuilder.append(this.protocol);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
