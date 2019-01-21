package com.huawei.okhttp3;

import com.huawei.okhttp3.Call.Factory;
import com.huawei.okhttp3.ConnectionPool.Http2ConnectionEventListener;
import com.huawei.okhttp3.internal.Internal;
import com.huawei.okhttp3.internal.Util;
import com.huawei.okhttp3.internal.cache.InternalCache;
import com.huawei.okhttp3.internal.connection.RealConnection;
import com.huawei.okhttp3.internal.connection.RouteDatabase;
import com.huawei.okhttp3.internal.connection.StreamAllocation;
import com.huawei.okhttp3.internal.platform.Platform;
import com.huawei.okhttp3.internal.tls.CertificateChainCleaner;
import com.huawei.okhttp3.internal.tls.OkHostnameVerifier;
import com.huawei.okhttp3.internal.ws.RealWebSocket;
import java.io.Closeable;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class OkHttpClient implements Cloneable, Factory, WebSocket.Factory {
    static final int CONNECTION_ATTEMPT_DELAY_DEFAULT = 200;
    static final int CONNECTION_ATTEMPT_DELAY_MAX = 2000;
    static final int CONNECTION_ATTEMPT_DELAY_MIN = 100;
    static final List<ConnectionSpec> DEFAULT_CONNECTION_SPECS = Util.immutableList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT);
    static final List<Protocol> DEFAULT_PROTOCOLS = Util.immutableList(Protocol.HTTP_2, Protocol.HTTP_1_1);
    final Authenticator authenticator;
    final Cache cache;
    final CertificateChainCleaner certificateChainCleaner;
    final CertificatePinner certificatePinner;
    final int connectTimeout;
    final int connectionAttemptDelay;
    final ConnectionPool connectionPool;
    final ConnectionPoolListener connectionPoolListener;
    final List<ConnectionSpec> connectionSpecs;
    final CookieJar cookieJar;
    final AbsDispatcher dispatcher;
    final Dns dns;
    final boolean followRedirects;
    final boolean followSslRedirects;
    final HostnameVerifier hostnameVerifier;
    final List<Interceptor> interceptors;
    final InternalCache internalCache;
    final List<Interceptor> networkInterceptors;
    final int pingInterval;
    final List<Protocol> protocols;
    final Proxy proxy;
    final Authenticator proxyAuthenticator;
    final ProxySelector proxySelector;
    final int readTimeout;
    final boolean retryOnConnectionFailure;
    final SocketFactory socketFactory;
    final SSLSocketFactory sslSocketFactory;
    final int writeTimeout;

    public static final class Builder implements iDispatcherFactory {
        Authenticator authenticator;
        Cache cache;
        CertificateChainCleaner certificateChainCleaner;
        CertificatePinner certificatePinner;
        int connectTimeout;
        int connectionAttemptDelay;
        ConnectionPool connectionPool;
        List<ConnectionSpec> connectionSpecs;
        CookieJar cookieJar;
        AbsDispatcher dispatcher;
        Dns dns;
        boolean followRedirects;
        boolean followSslRedirects;
        HostnameVerifier hostnameVerifier;
        final List<Interceptor> interceptors;
        InternalCache internalCache;
        final List<Interceptor> networkInterceptors;
        int pingInterval;
        List<Protocol> protocols;
        Proxy proxy;
        Authenticator proxyAuthenticator;
        ProxySelector proxySelector;
        int readTimeout;
        boolean retryOnConnectionFailure;
        SocketFactory socketFactory;
        SSLSocketFactory sslSocketFactory;
        int writeTimeout;

        public Builder() {
            this.interceptors = new ArrayList();
            this.networkInterceptors = new ArrayList();
            this.dispatcher = new Dispatcher();
            this.protocols = OkHttpClient.DEFAULT_PROTOCOLS;
            this.connectionSpecs = OkHttpClient.DEFAULT_CONNECTION_SPECS;
            this.proxySelector = ProxySelector.getDefault();
            this.cookieJar = CookieJar.NO_COOKIES;
            this.socketFactory = SocketFactory.getDefault();
            this.hostnameVerifier = OkHostnameVerifier.INSTANCE;
            this.certificatePinner = CertificatePinner.DEFAULT;
            this.proxyAuthenticator = Authenticator.NONE;
            this.authenticator = Authenticator.NONE;
            this.connectionPool = new ConnectionPool();
            this.dns = Dns.SYSTEM;
            this.followSslRedirects = true;
            this.followRedirects = true;
            this.retryOnConnectionFailure = true;
            this.connectTimeout = 10000;
            this.readTimeout = 10000;
            this.writeTimeout = 10000;
            this.pingInterval = 0;
            this.connectionAttemptDelay = 200;
        }

        Builder(OkHttpClient okHttpClient) {
            this.interceptors = new ArrayList();
            this.networkInterceptors = new ArrayList();
            this.dispatcher = okHttpClient.dispatcher;
            this.proxy = okHttpClient.proxy;
            this.protocols = okHttpClient.protocols;
            this.connectionSpecs = okHttpClient.connectionSpecs;
            this.interceptors.addAll(okHttpClient.interceptors);
            this.networkInterceptors.addAll(okHttpClient.networkInterceptors);
            this.proxySelector = okHttpClient.proxySelector;
            this.cookieJar = okHttpClient.cookieJar;
            this.internalCache = okHttpClient.internalCache;
            this.cache = okHttpClient.cache;
            this.socketFactory = okHttpClient.socketFactory;
            this.sslSocketFactory = okHttpClient.sslSocketFactory;
            this.certificateChainCleaner = okHttpClient.certificateChainCleaner;
            this.hostnameVerifier = okHttpClient.hostnameVerifier;
            this.certificatePinner = okHttpClient.certificatePinner;
            this.proxyAuthenticator = okHttpClient.proxyAuthenticator;
            this.authenticator = okHttpClient.authenticator;
            this.connectionPool = okHttpClient.connectionPool;
            this.dns = okHttpClient.dns;
            this.followSslRedirects = okHttpClient.followSslRedirects;
            this.followRedirects = okHttpClient.followRedirects;
            this.retryOnConnectionFailure = okHttpClient.retryOnConnectionFailure;
            this.connectTimeout = okHttpClient.connectTimeout;
            this.readTimeout = okHttpClient.readTimeout;
            this.writeTimeout = okHttpClient.writeTimeout;
            this.pingInterval = okHttpClient.pingInterval;
            this.connectionAttemptDelay = okHttpClient.connectionAttemptDelay;
        }

        public Builder connectTimeout(long timeout, TimeUnit unit) {
            this.connectTimeout = checkDuration("timeout", timeout, unit);
            if (this.connectionAttemptDelay < this.connectTimeout) {
                return this;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Connection Attempt Delay (");
            stringBuilder.append(this.connectionAttemptDelay);
            stringBuilder.append(" ms) is greater than or equal to Connect Timeout (");
            stringBuilder.append(this.connectTimeout);
            stringBuilder.append(" ms)");
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public Builder readTimeout(long timeout, TimeUnit unit) {
            this.readTimeout = checkDuration("timeout", timeout, unit);
            return this;
        }

        public Builder writeTimeout(long timeout, TimeUnit unit) {
            this.writeTimeout = checkDuration("timeout", timeout, unit);
            return this;
        }

        public Builder pingInterval(long interval, TimeUnit unit) {
            this.pingInterval = checkDuration("interval", interval, unit);
            return this;
        }

        public Builder connectionAttemptDelay(long interval, TimeUnit unit) {
            this.connectionAttemptDelay = checkDuration("connectionAttemptDelay", interval, unit);
            if (this.connectionAttemptDelay < 100 || this.connectionAttemptDelay > 2000) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Connection Attempt Delay ");
                stringBuilder.append(this.connectionAttemptDelay);
                stringBuilder.append("ms is out of range (");
                stringBuilder.append(100);
                stringBuilder.append("ms ~ ");
                stringBuilder.append(2000);
                stringBuilder.append("ms).");
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (this.connectionAttemptDelay < this.connectTimeout) {
                return this;
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Connection Attempt Delay (");
                stringBuilder2.append(this.connectionAttemptDelay);
                stringBuilder2.append(" ms) is greater than or equal to Connect Timeout (");
                stringBuilder2.append(this.connectTimeout);
                stringBuilder2.append(" ms)");
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
        }

        private static int checkDuration(String name, long duration, TimeUnit unit) {
            StringBuilder stringBuilder;
            if (duration < 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(name);
                stringBuilder.append(" < 0");
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (unit != null) {
                long millis = unit.toMillis(duration);
                if (millis > 2147483647L) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(name);
                    stringBuilder.append(" too large.");
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (millis != 0 || duration <= 0) {
                    return (int) millis;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(name);
                    stringBuilder.append(" too small.");
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } else {
                throw new NullPointerException("unit == null");
            }
        }

        public Builder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder proxySelector(ProxySelector proxySelector) {
            this.proxySelector = proxySelector;
            return this;
        }

        public Builder cookieJar(CookieJar cookieJar) {
            if (cookieJar != null) {
                this.cookieJar = cookieJar;
                return this;
            }
            throw new NullPointerException("cookieJar == null");
        }

        void setInternalCache(InternalCache internalCache) {
            this.internalCache = internalCache;
            this.cache = null;
        }

        public Builder cache(Cache cache) {
            this.cache = cache;
            this.internalCache = null;
            return this;
        }

        public Builder dns(Dns dns) {
            if (dns != null) {
                this.dns = dns;
                return this;
            }
            throw new NullPointerException("dns == null");
        }

        public Builder socketFactory(SocketFactory socketFactory) {
            if (socketFactory != null) {
                this.socketFactory = socketFactory;
                return this;
            }
            throw new NullPointerException("socketFactory == null");
        }

        public Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
            if (sslSocketFactory != null) {
                X509TrustManager trustManager = Platform.get().trustManager(sslSocketFactory);
                if (trustManager != null) {
                    this.sslSocketFactory = sslSocketFactory;
                    this.certificateChainCleaner = CertificateChainCleaner.get(trustManager);
                    return this;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to extract the trust manager on ");
                stringBuilder.append(Platform.get());
                stringBuilder.append(", sslSocketFactory is ");
                stringBuilder.append(sslSocketFactory.getClass());
                throw new IllegalStateException(stringBuilder.toString());
            }
            throw new NullPointerException("sslSocketFactory == null");
        }

        public Builder sslSocketFactory(SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {
            if (sslSocketFactory == null) {
                throw new NullPointerException("sslSocketFactory == null");
            } else if (trustManager != null) {
                this.sslSocketFactory = sslSocketFactory;
                this.certificateChainCleaner = CertificateChainCleaner.get(trustManager);
                return this;
            } else {
                throw new NullPointerException("trustManager == null");
            }
        }

        public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
            if (hostnameVerifier != null) {
                this.hostnameVerifier = hostnameVerifier;
                return this;
            }
            throw new NullPointerException("hostnameVerifier == null");
        }

        public Builder certificatePinner(CertificatePinner certificatePinner) {
            if (certificatePinner != null) {
                this.certificatePinner = certificatePinner;
                return this;
            }
            throw new NullPointerException("certificatePinner == null");
        }

        public Builder authenticator(Authenticator authenticator) {
            if (authenticator != null) {
                this.authenticator = authenticator;
                return this;
            }
            throw new NullPointerException("authenticator == null");
        }

        public Builder proxyAuthenticator(Authenticator proxyAuthenticator) {
            if (proxyAuthenticator != null) {
                this.proxyAuthenticator = proxyAuthenticator;
                return this;
            }
            throw new NullPointerException("proxyAuthenticator == null");
        }

        public Builder connectionPool(ConnectionPool connectionPool) {
            if (connectionPool != null) {
                this.connectionPool = connectionPool;
                return this;
            }
            throw new NullPointerException("connectionPool == null");
        }

        public Builder followSslRedirects(boolean followProtocolRedirects) {
            this.followSslRedirects = followProtocolRedirects;
            return this;
        }

        public Builder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public Builder retryOnConnectionFailure(boolean retryOnConnectionFailure) {
            this.retryOnConnectionFailure = retryOnConnectionFailure;
            return this;
        }

        public Builder dispatcher(AbsDispatcher dispatcher) {
            if (dispatcher != null) {
                this.dispatcher = dispatcher;
                return this;
            }
            throw new IllegalArgumentException("dispatcher == null");
        }

        public Builder protocols(List<Protocol> protocols) {
            ArrayList protocols2 = new ArrayList(protocols);
            StringBuilder stringBuilder;
            if (!protocols2.contains(Protocol.HTTP_1_1)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("protocols doesn't contain http/1.1: ");
                stringBuilder.append(protocols2);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (protocols2.contains(Protocol.HTTP_1_0)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("protocols must not contain http/1.0: ");
                stringBuilder.append(protocols2);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (protocols2.contains(null)) {
                throw new IllegalArgumentException("protocols must not contain null");
            } else {
                if (protocols2.contains(Protocol.SPDY_3)) {
                    protocols2.remove(Protocol.SPDY_3);
                }
                this.protocols = Collections.unmodifiableList(protocols2);
                return this;
            }
        }

        public Builder connectionSpecs(List<ConnectionSpec> connectionSpecs) {
            this.connectionSpecs = Util.immutableList((List) connectionSpecs);
            return this;
        }

        public List<Interceptor> interceptors() {
            return this.interceptors;
        }

        public Builder addInterceptor(Interceptor interceptor) {
            this.interceptors.add(interceptor);
            return this;
        }

        public List<Interceptor> networkInterceptors() {
            return this.networkInterceptors;
        }

        public Builder addNetworkInterceptor(Interceptor interceptor) {
            this.networkInterceptors.add(interceptor);
            return this;
        }

        public OkHttpClient build() {
            return new OkHttpClient(this);
        }

        public AbsDispatcher createDispatcher(Protocol httpProtocol) {
            switch (httpProtocol) {
                case HTTP_2:
                    return new Http2Dispatcher();
                case HTTP_1_0:
                case HTTP_1_1:
                case SPDY_3:
                    return new Dispatcher();
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("there is no dispatcher fit for the protocol ");
                    stringBuilder.append(httpProtocol.toString());
                    throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
    }

    private class ConnectionPoolListener implements Http2ConnectionEventListener {
        private ConnectionPoolListener() {
        }

        /* synthetic */ ConnectionPoolListener(OkHttpClient x0, AnonymousClass1 x1) {
            this();
        }

        public void onEvicted(String hostName, int port, String scheme) {
            OkHttpClient.this.dispatcher.removeHttp2Host(hostName, port, scheme);
        }
    }

    static {
        Internal.instance = new Internal() {
            public void addLenient(com.huawei.okhttp3.Headers.Builder builder, String line) {
                builder.addLenient(line);
            }

            public void addLenient(com.huawei.okhttp3.Headers.Builder builder, String name, String value) {
                builder.addLenient(name, value);
            }

            public void setCache(Builder builder, InternalCache internalCache) {
                builder.setInternalCache(internalCache);
            }

            public boolean connectionBecameIdle(ConnectionPool pool, RealConnection connection) {
                return pool.connectionBecameIdle(connection);
            }

            public RealConnection get(ConnectionPool pool, Address address, StreamAllocation streamAllocation) {
                return pool.get(address, streamAllocation);
            }

            public Closeable deduplicate(OkHttpClient client, ConnectionPool pool, Address address, StreamAllocation streamAllocation) {
                return pool.deduplicate(address, streamAllocation, client.dispatcher().getMaxHttp2ConnectionPerHost());
            }

            public void put(ConnectionPool pool, RealConnection connection) {
                pool.put(connection);
            }

            public RouteDatabase routeDatabase(ConnectionPool connectionPool) {
                return connectionPool.routeDatabase;
            }

            public void apply(ConnectionSpec tlsConfiguration, SSLSocket sslSocket, boolean isFallback) {
                tlsConfiguration.apply(sslSocket, isFallback);
            }

            public HttpUrl getHttpUrlChecked(String url) throws MalformedURLException, UnknownHostException {
                return HttpUrl.getChecked(url);
            }

            public StreamAllocation streamAllocation(Call call) {
                return ((RealCall) call).streamAllocation();
            }

            public Call newWebSocketCall(OkHttpClient client, Request originalRequest) {
                return new RealCall(client, originalRequest, true);
            }
        };
    }

    public OkHttpClient() {
        this(new Builder());
    }

    OkHttpClient(Builder builder) {
        this.connectionPoolListener = new ConnectionPoolListener(this, null);
        this.dispatcher = builder.dispatcher;
        this.proxy = builder.proxy;
        this.protocols = builder.protocols;
        this.connectionSpecs = builder.connectionSpecs;
        this.interceptors = Util.immutableList(builder.interceptors);
        this.networkInterceptors = Util.immutableList(builder.networkInterceptors);
        this.proxySelector = builder.proxySelector;
        this.cookieJar = builder.cookieJar;
        this.cache = builder.cache;
        this.internalCache = builder.internalCache;
        this.socketFactory = builder.socketFactory;
        boolean isTLS = false;
        for (ConnectionSpec spec : this.connectionSpecs) {
            boolean z = isTLS || spec.isTls();
            isTLS = z;
        }
        if (builder.sslSocketFactory == null && isTLS) {
            X509TrustManager trustManager = systemDefaultTrustManager();
            this.sslSocketFactory = systemDefaultSslSocketFactory(trustManager);
            this.certificateChainCleaner = CertificateChainCleaner.get(trustManager);
        } else {
            this.sslSocketFactory = builder.sslSocketFactory;
            this.certificateChainCleaner = builder.certificateChainCleaner;
        }
        this.hostnameVerifier = builder.hostnameVerifier;
        this.certificatePinner = builder.certificatePinner.withCertificateChainCleaner(this.certificateChainCleaner);
        this.proxyAuthenticator = builder.proxyAuthenticator;
        this.authenticator = builder.authenticator;
        this.connectionPool = builder.connectionPool;
        this.dns = builder.dns;
        this.followSslRedirects = builder.followSslRedirects;
        this.followRedirects = builder.followRedirects;
        this.retryOnConnectionFailure = builder.retryOnConnectionFailure;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.writeTimeout = builder.writeTimeout;
        this.pingInterval = builder.pingInterval;
        this.connectionPool.addHttp2Listener(this.connectionPoolListener);
        this.connectionAttemptDelay = builder.connectionAttemptDelay;
    }

    private X509TrustManager systemDefaultTrustManager() {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length == 1 && (trustManagers[0] instanceof X509TrustManager)) {
                return (X509TrustManager) trustManagers[0];
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected default trust managers:");
            stringBuilder.append(Arrays.toString(trustManagers));
            throw new IllegalStateException(stringBuilder.toString());
        } catch (GeneralSecurityException e) {
            throw new AssertionError();
        }
    }

    private SSLSocketFactory systemDefaultSslSocketFactory(X509TrustManager trustManager) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            return sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new AssertionError();
        }
    }

    public int connectTimeoutMillis() {
        return this.connectTimeout;
    }

    public int readTimeoutMillis() {
        return this.readTimeout;
    }

    public int writeTimeoutMillis() {
        return this.writeTimeout;
    }

    public int pingIntervalMillis() {
        return this.pingInterval;
    }

    public Proxy proxy() {
        return this.proxy;
    }

    public ProxySelector proxySelector() {
        return this.proxySelector;
    }

    public CookieJar cookieJar() {
        return this.cookieJar;
    }

    public Cache cache() {
        return this.cache;
    }

    InternalCache internalCache() {
        return this.cache != null ? this.cache.internalCache : this.internalCache;
    }

    public Dns dns() {
        return this.dns;
    }

    public SocketFactory socketFactory() {
        return this.socketFactory;
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

    public Authenticator authenticator() {
        return this.authenticator;
    }

    public Authenticator proxyAuthenticator() {
        return this.proxyAuthenticator;
    }

    public ConnectionPool connectionPool() {
        return this.connectionPool;
    }

    public boolean followSslRedirects() {
        return this.followSslRedirects;
    }

    public boolean followRedirects() {
        return this.followRedirects;
    }

    public boolean retryOnConnectionFailure() {
        return this.retryOnConnectionFailure;
    }

    public AbsDispatcher dispatcher() {
        return this.dispatcher;
    }

    public void addHttp2Host(String hostName, int port, String scheme) {
        this.dispatcher.addHttp2Host(hostName, port, scheme);
    }

    public List<Protocol> protocols() {
        return this.protocols;
    }

    public List<ConnectionSpec> connectionSpecs() {
        return this.connectionSpecs;
    }

    public int connectionAttemptDelay() {
        return this.connectionAttemptDelay;
    }

    public List<Interceptor> interceptors() {
        return this.interceptors;
    }

    public List<Interceptor> networkInterceptors() {
        return this.networkInterceptors;
    }

    public Call newCall(Request request) {
        return new RealCall(this, request, false);
    }

    public int http2ConnectionCount(String hostName, int port, String scheme) {
        return this.connectionPool.http2ConnectionCount(hostName, port, scheme);
    }

    public boolean keepHttp2ConnectionAlive(String hostName, int port, String scheme) {
        return this.connectionPool.keepHttp2ConnectionAlive(hostName, port, scheme);
    }

    public WebSocket newWebSocket(Request request, WebSocketListener listener) {
        RealWebSocket webSocket = new RealWebSocket(request, listener, new SecureRandom());
        webSocket.connect(this);
        return webSocket;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }
}
