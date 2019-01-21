package com.android.okhttp.internal.io;

import com.android.okhttp.Address;
import com.android.okhttp.CertificatePinner;
import com.android.okhttp.Connection;
import com.android.okhttp.ConnectionSpec;
import com.android.okhttp.Handshake;
import com.android.okhttp.HttpUrl;
import com.android.okhttp.Protocol;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import com.android.okhttp.Route;
import com.android.okhttp.internal.ConnectionSpecSelector;
import com.android.okhttp.internal.Platform;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.Version;
import com.android.okhttp.internal.framed.FramedConnection;
import com.android.okhttp.internal.framed.FramedConnection.Builder;
import com.android.okhttp.internal.http.Http1xStream;
import com.android.okhttp.internal.http.OkHeaders;
import com.android.okhttp.internal.http.RouteException;
import com.android.okhttp.internal.http.StreamAllocation;
import com.android.okhttp.internal.tls.CertificateChainCleaner;
import com.android.okhttp.internal.tls.OkHostnameVerifier;
import com.android.okhttp.internal.tls.TrustRootIndex;
import com.android.okhttp.okio.BufferedSink;
import com.android.okhttp.okio.BufferedSource;
import com.android.okhttp.okio.Okio;
import com.android.okhttp.okio.Source;
import java.io.IOException;
import java.lang.ref.Reference;
import java.net.ConnectException;
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
import javax.net.ssl.SSLSocketFactory;

public final class RealConnection implements Connection {
    private static SSLSocketFactory lastSslSocketFactory;
    private static TrustRootIndex lastTrustRootIndex;
    public final List<Reference<StreamAllocation>> allocations = new ArrayList();
    public volatile FramedConnection framedConnection;
    private Handshake handshake;
    public long idleAtNanos = Long.MAX_VALUE;
    public boolean noNewStreams;
    private Protocol protocol;
    private Socket rawSocket;
    private final Route route;
    public BufferedSink sink;
    public Socket socket;
    public BufferedSource source;
    public int streamCount;

    public RealConnection(Route route) {
        this.route = route;
    }

    public void connect(int connectTimeout, int readTimeout, int writeTimeout, List<ConnectionSpec> connectionSpecs, boolean connectionRetryEnabled) throws RouteException {
        if (this.protocol == null) {
            RouteException routeException = null;
            ConnectionSpecSelector connectionSpecSelector = new ConnectionSpecSelector(connectionSpecs);
            Proxy proxy = this.route.getProxy();
            Address address = this.route.getAddress();
            if (this.route.getAddress().getSslSocketFactory() != null || connectionSpecs.contains(ConnectionSpec.CLEARTEXT)) {
                while (this.protocol == null) {
                    try {
                        Socket socket;
                        if (proxy.type() != Type.DIRECT) {
                            if (proxy.type() != Type.HTTP) {
                                socket = new Socket(proxy);
                                this.rawSocket = socket;
                                connectSocket(connectTimeout, readTimeout, writeTimeout, connectionSpecSelector);
                            }
                        }
                        socket = address.getSocketFactory().createSocket();
                        this.rawSocket = socket;
                        connectSocket(connectTimeout, readTimeout, writeTimeout, connectionSpecSelector);
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
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CLEARTEXT communication not supported: ");
            stringBuilder.append(connectionSpecs);
            throw new RouteException(new UnknownServiceException(stringBuilder.toString()));
        }
        throw new IllegalStateException("already connected");
    }

    private void connectSocket(int connectTimeout, int readTimeout, int writeTimeout, ConnectionSpecSelector connectionSpecSelector) throws IOException {
        this.rawSocket.setSoTimeout(readTimeout);
        try {
            Platform.get().connectSocket(this.rawSocket, this.route.getSocketAddress(), connectTimeout);
            this.source = Okio.buffer(Okio.source(this.rawSocket));
            this.sink = Okio.buffer(Okio.sink(this.rawSocket));
            if (this.route.getAddress().getSslSocketFactory() != null) {
                connectTls(readTimeout, writeTimeout, connectionSpecSelector);
            } else {
                this.protocol = Protocol.HTTP_1_1;
                this.socket = this.rawSocket;
            }
            if (this.protocol == Protocol.SPDY_3 || this.protocol == Protocol.HTTP_2) {
                this.socket.setSoTimeout(0);
                FramedConnection framedConnection = new Builder(true).socket(this.socket, this.route.getAddress().url().host(), this.source, this.sink).protocol(this.protocol).build();
                framedConnection.sendConnectionPreface();
                this.framedConnection = framedConnection;
            }
        } catch (ConnectException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to connect to ");
            stringBuilder.append(this.route.getSocketAddress());
            throw new ConnectException(stringBuilder.toString());
        }
    }

    private void connectTls(int readTimeout, int writeTimeout, ConnectionSpecSelector connectionSpecSelector) throws IOException {
        if (this.route.requiresTunnel()) {
            createTunnel(readTimeout, writeTimeout);
        }
        Address address = this.route.getAddress();
        String maybeProtocol = null;
        Socket sslSocket = null;
        try {
            sslSocket = (SSLSocket) address.getSslSocketFactory().createSocket(this.rawSocket, address.getUriHost(), address.getUriPort(), true);
            ConnectionSpec connectionSpec = connectionSpecSelector.configureSecureSocket(sslSocket);
            if (connectionSpec.supportsTlsExtensions()) {
                Platform.get().configureTlsExtensions(sslSocket, address.getUriHost(), address.getProtocols());
            }
            sslSocket.startHandshake();
            Handshake unverifiedHandshake = Handshake.get(sslSocket.getSession());
            if (address.getHostnameVerifier().verify(address.getUriHost(), sslSocket.getSession())) {
                Protocol protocol;
                if (address.getCertificatePinner() != CertificatePinner.DEFAULT) {
                    address.getCertificatePinner().check(address.getUriHost(), new CertificateChainCleaner(trustRootIndex(address.getSslSocketFactory())).clean(unverifiedHandshake.peerCertificates()));
                }
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
            stringBuilder.append(address.getUriHost());
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

    private static synchronized TrustRootIndex trustRootIndex(SSLSocketFactory sslSocketFactory) {
        TrustRootIndex trustRootIndex;
        synchronized (RealConnection.class) {
            if (sslSocketFactory != lastSslSocketFactory) {
                lastTrustRootIndex = Platform.get().trustRootIndex(Platform.get().trustManager(sslSocketFactory));
                lastSslSocketFactory = sslSocketFactory;
            }
            trustRootIndex = lastTrustRootIndex;
        }
        return trustRootIndex;
    }

    private void createTunnel(int readTimeout, int writeTimeout) throws IOException {
        Request tunnelRequest = createTunnelRequest();
        HttpUrl url = tunnelRequest.httpUrl();
        String requestLine = new StringBuilder();
        requestLine.append("CONNECT ");
        requestLine.append(Util.hostHeader(url, true));
        requestLine.append(" HTTP/1.1");
        requestLine = requestLine.toString();
        while (true) {
            Http1xStream tunnelConnection = new Http1xStream(null, this.source, this.sink);
            this.source.timeout().timeout((long) readTimeout, TimeUnit.MILLISECONDS);
            this.sink.timeout().timeout((long) writeTimeout, TimeUnit.MILLISECONDS);
            tunnelConnection.writeRequest(tunnelRequest.headers(), requestLine);
            tunnelConnection.finishRequest();
            Response response = tunnelConnection.readResponse().request(tunnelRequest).build();
            long contentLength = OkHeaders.contentLength(response);
            if (contentLength == -1) {
                contentLength = 0;
            }
            Source body = tunnelConnection.newFixedLengthSource(contentLength);
            Util.skipAll(body, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
            body.close();
            int code = response.code();
            if (code != 200) {
                if (code == 407) {
                    tunnelRequest = OkHeaders.processAuthHeader(this.route.getAddress().getAuthenticator(), response, this.route.getProxy());
                    if (tunnelRequest == null) {
                        throw new IOException("Failed to authenticate with proxy");
                    }
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected response code for CONNECT: ");
                    stringBuilder.append(response.code());
                    throw new IOException(stringBuilder.toString());
                }
            } else if (!this.source.buffer().exhausted() || !this.sink.buffer().exhausted()) {
                throw new IOException("TLS tunnel buffered too many bytes!");
            } else {
                return;
            }
        }
    }

    private Request createTunnelRequest() throws IOException {
        return new Request.Builder().url(this.route.getAddress().url()).header("Host", Util.hostHeader(this.route.getAddress().url(), true)).header("Proxy-Connection", "Keep-Alive").header("User-Agent", Version.userAgent()).build();
    }

    boolean isConnected() {
        return this.protocol != null;
    }

    public Route getRoute() {
        return this.route;
    }

    public void cancel() {
        Util.closeQuietly(this.rawSocket);
    }

    public Socket getSocket() {
        return this.socket;
    }

    public int allocationLimit() {
        FramedConnection framedConnection = this.framedConnection;
        if (framedConnection != null) {
            return framedConnection.maxConcurrentStreams();
        }
        return 1;
    }

    public boolean isHealthy(boolean doExtensiveChecks) {
        if (this.socket.isClosed() || this.socket.isInputShutdown() || this.socket.isOutputShutdown()) {
            return false;
        }
        if (this.framedConnection == null && doExtensiveChecks) {
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

    public Handshake getHandshake() {
        return this.handshake;
    }

    public boolean isMultiplexed() {
        return this.framedConnection != null;
    }

    public Protocol getProtocol() {
        return this.protocol != null ? this.protocol : Protocol.HTTP_1_1;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Connection{");
        stringBuilder.append(this.route.getAddress().url().host());
        stringBuilder.append(":");
        stringBuilder.append(this.route.getAddress().url().port());
        stringBuilder.append(", proxy=");
        stringBuilder.append(this.route.getProxy());
        stringBuilder.append(" hostAddress=");
        stringBuilder.append(this.route.getSocketAddress());
        stringBuilder.append(" cipherSuite=");
        stringBuilder.append(this.handshake != null ? this.handshake.cipherSuite() : "none");
        stringBuilder.append(" protocol=");
        stringBuilder.append(this.protocol);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
