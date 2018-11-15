package org.apache.http.impl.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.ProtocolVersion;
import org.apache.http.auth.AuthSchemeRegistry;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.cookie.CookieSpecRegistry;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.DefaultedHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

@Deprecated
public abstract class AbstractHttpClient implements HttpClient {
    private static final int FAKE_HTTP_RESPONSE_CODE = 404;
    private static final String FAKE_HTTP_RESPONSE_REASON = "error";
    private static final int FAKE_PROTOCOL_MAJOR_VERSION = 100;
    private static final int FAKE_PROTOCOL_MINOR_VERSION = 80;
    private static final String FAKE_PROTOCOL_NAME = "HTTP";
    private static final int MAX_PDU_PARSER_LENGTH = 1000;
    private static final String MMS_CONTENT_TYPE = "application/vnd.wap.mms-message";
    private static final int MMS_SEND_TYPE_NAME = 140;
    private static final int MMS_SEND_TYPE_VALUE = 128;
    private ClientConnectionManager connManager;
    private CookieStore cookieStore;
    private CredentialsProvider credsProvider;
    private HttpParams defaultParams;
    private BasicHttpProcessor httpProcessor;
    private ConnectionKeepAliveStrategy keepAliveStrategy;
    private final Log log = LogFactory.getLog(getClass());
    private AuthenticationHandler proxyAuthHandler;
    private RedirectHandler redirectHandler;
    private HttpRequestExecutor requestExec;
    private HttpRequestRetryHandler retryHandler;
    private ConnectionReuseStrategy reuseStrategy;
    private HttpRoutePlanner routePlanner;
    private AuthSchemeRegistry supportedAuthSchemes;
    private CookieSpecRegistry supportedCookieSpecs;
    private AuthenticationHandler targetAuthHandler;
    private UserTokenHandler userTokenHandler;

    protected abstract AuthSchemeRegistry createAuthSchemeRegistry();

    protected abstract ClientConnectionManager createClientConnectionManager();

    protected abstract ConnectionKeepAliveStrategy createConnectionKeepAliveStrategy();

    protected abstract ConnectionReuseStrategy createConnectionReuseStrategy();

    protected abstract CookieSpecRegistry createCookieSpecRegistry();

    protected abstract CookieStore createCookieStore();

    protected abstract CredentialsProvider createCredentialsProvider();

    protected abstract HttpContext createHttpContext();

    protected abstract HttpParams createHttpParams();

    protected abstract BasicHttpProcessor createHttpProcessor();

    protected abstract HttpRequestRetryHandler createHttpRequestRetryHandler();

    protected abstract HttpRoutePlanner createHttpRoutePlanner();

    protected abstract AuthenticationHandler createProxyAuthenticationHandler();

    protected abstract RedirectHandler createRedirectHandler();

    protected abstract HttpRequestExecutor createRequestExecutor();

    protected abstract AuthenticationHandler createTargetAuthenticationHandler();

    protected abstract UserTokenHandler createUserTokenHandler();

    protected AbstractHttpClient(ClientConnectionManager conman, HttpParams params) {
        this.defaultParams = params;
        this.connManager = conman;
    }

    public final synchronized HttpParams getParams() {
        if (this.defaultParams == null) {
            this.defaultParams = createHttpParams();
        }
        return this.defaultParams;
    }

    public synchronized void setParams(HttpParams params) {
        this.defaultParams = params;
    }

    public final synchronized ClientConnectionManager getConnectionManager() {
        if (this.connManager == null) {
            this.connManager = createClientConnectionManager();
        }
        return this.connManager;
    }

    public final synchronized HttpRequestExecutor getRequestExecutor() {
        if (this.requestExec == null) {
            this.requestExec = createRequestExecutor();
        }
        return this.requestExec;
    }

    public final synchronized AuthSchemeRegistry getAuthSchemes() {
        if (this.supportedAuthSchemes == null) {
            this.supportedAuthSchemes = createAuthSchemeRegistry();
        }
        return this.supportedAuthSchemes;
    }

    public synchronized void setAuthSchemes(AuthSchemeRegistry authSchemeRegistry) {
        this.supportedAuthSchemes = authSchemeRegistry;
    }

    public final synchronized CookieSpecRegistry getCookieSpecs() {
        if (this.supportedCookieSpecs == null) {
            this.supportedCookieSpecs = createCookieSpecRegistry();
        }
        return this.supportedCookieSpecs;
    }

    public synchronized void setCookieSpecs(CookieSpecRegistry cookieSpecRegistry) {
        this.supportedCookieSpecs = cookieSpecRegistry;
    }

    public final synchronized ConnectionReuseStrategy getConnectionReuseStrategy() {
        if (this.reuseStrategy == null) {
            this.reuseStrategy = createConnectionReuseStrategy();
        }
        return this.reuseStrategy;
    }

    public synchronized void setReuseStrategy(ConnectionReuseStrategy reuseStrategy) {
        this.reuseStrategy = reuseStrategy;
    }

    public final synchronized ConnectionKeepAliveStrategy getConnectionKeepAliveStrategy() {
        if (this.keepAliveStrategy == null) {
            this.keepAliveStrategy = createConnectionKeepAliveStrategy();
        }
        return this.keepAliveStrategy;
    }

    public synchronized void setKeepAliveStrategy(ConnectionKeepAliveStrategy keepAliveStrategy) {
        this.keepAliveStrategy = keepAliveStrategy;
    }

    public final synchronized HttpRequestRetryHandler getHttpRequestRetryHandler() {
        if (this.retryHandler == null) {
            this.retryHandler = createHttpRequestRetryHandler();
        }
        return this.retryHandler;
    }

    public synchronized void setHttpRequestRetryHandler(HttpRequestRetryHandler retryHandler) {
        this.retryHandler = retryHandler;
    }

    public final synchronized RedirectHandler getRedirectHandler() {
        if (this.redirectHandler == null) {
            this.redirectHandler = createRedirectHandler();
        }
        return this.redirectHandler;
    }

    public synchronized void setRedirectHandler(RedirectHandler redirectHandler) {
        this.redirectHandler = redirectHandler;
    }

    public final synchronized AuthenticationHandler getTargetAuthenticationHandler() {
        if (this.targetAuthHandler == null) {
            this.targetAuthHandler = createTargetAuthenticationHandler();
        }
        return this.targetAuthHandler;
    }

    public synchronized void setTargetAuthenticationHandler(AuthenticationHandler targetAuthHandler) {
        this.targetAuthHandler = targetAuthHandler;
    }

    public final synchronized AuthenticationHandler getProxyAuthenticationHandler() {
        if (this.proxyAuthHandler == null) {
            this.proxyAuthHandler = createProxyAuthenticationHandler();
        }
        return this.proxyAuthHandler;
    }

    public synchronized void setProxyAuthenticationHandler(AuthenticationHandler proxyAuthHandler) {
        this.proxyAuthHandler = proxyAuthHandler;
    }

    public final synchronized CookieStore getCookieStore() {
        if (this.cookieStore == null) {
            this.cookieStore = createCookieStore();
        }
        return this.cookieStore;
    }

    public synchronized void setCookieStore(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    public final synchronized CredentialsProvider getCredentialsProvider() {
        if (this.credsProvider == null) {
            this.credsProvider = createCredentialsProvider();
        }
        return this.credsProvider;
    }

    public synchronized void setCredentialsProvider(CredentialsProvider credsProvider) {
        this.credsProvider = credsProvider;
    }

    public final synchronized HttpRoutePlanner getRoutePlanner() {
        if (this.routePlanner == null) {
            this.routePlanner = createHttpRoutePlanner();
        }
        return this.routePlanner;
    }

    public synchronized void setRoutePlanner(HttpRoutePlanner routePlanner) {
        this.routePlanner = routePlanner;
    }

    public final synchronized UserTokenHandler getUserTokenHandler() {
        if (this.userTokenHandler == null) {
            this.userTokenHandler = createUserTokenHandler();
        }
        return this.userTokenHandler;
    }

    public synchronized void setUserTokenHandler(UserTokenHandler userTokenHandler) {
        this.userTokenHandler = userTokenHandler;
    }

    protected final synchronized BasicHttpProcessor getHttpProcessor() {
        if (this.httpProcessor == null) {
            this.httpProcessor = createHttpProcessor();
        }
        return this.httpProcessor;
    }

    public synchronized void addResponseInterceptor(HttpResponseInterceptor itcp) {
        getHttpProcessor().addInterceptor(itcp);
    }

    public synchronized void addResponseInterceptor(HttpResponseInterceptor itcp, int index) {
        getHttpProcessor().addInterceptor(itcp, index);
    }

    public synchronized HttpResponseInterceptor getResponseInterceptor(int index) {
        return getHttpProcessor().getResponseInterceptor(index);
    }

    public synchronized int getResponseInterceptorCount() {
        return getHttpProcessor().getResponseInterceptorCount();
    }

    public synchronized void clearResponseInterceptors() {
        getHttpProcessor().clearResponseInterceptors();
    }

    public void removeResponseInterceptorByClass(Class<? extends HttpResponseInterceptor> clazz) {
        getHttpProcessor().removeResponseInterceptorByClass(clazz);
    }

    public synchronized void addRequestInterceptor(HttpRequestInterceptor itcp) {
        getHttpProcessor().addInterceptor(itcp);
    }

    public synchronized void addRequestInterceptor(HttpRequestInterceptor itcp, int index) {
        getHttpProcessor().addInterceptor(itcp, index);
    }

    public synchronized HttpRequestInterceptor getRequestInterceptor(int index) {
        return getHttpProcessor().getRequestInterceptor(index);
    }

    public synchronized int getRequestInterceptorCount() {
        return getHttpProcessor().getRequestInterceptorCount();
    }

    public synchronized void clearRequestInterceptors() {
        getHttpProcessor().clearRequestInterceptors();
    }

    public void removeRequestInterceptorByClass(Class<? extends HttpRequestInterceptor> clazz) {
        getHttpProcessor().removeRequestInterceptorByClass(clazz);
    }

    public final HttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
        return execute(request, (HttpContext) null);
    }

    public final HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException {
        if (request != null) {
            return execute(determineTarget(request), (HttpRequest) request, context);
        }
        throw new IllegalArgumentException("Request must not be null.");
    }

    private HttpHost determineTarget(HttpUriRequest request) {
        URI requestURI = request.getURI();
        if (requestURI.isAbsolute()) {
            return new HttpHost(requestURI.getHost(), requestURI.getPort(), requestURI.getScheme());
        }
        return null;
    }

    public final HttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
        return execute(target, request, (HttpContext) null);
    }

    /* JADX WARNING: Missing block: B:18:0x0060, code:
            if (r16 == false) goto L_0x006e;
     */
    /* JADX WARNING: Missing block: B:21:0x0066, code:
            return getFakeResponse();
     */
    /* JADX WARNING: Missing block: B:22:0x0067, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:23:0x0068, code:
            r2 = r22;
            r4 = r15;
            r3 = r23;
     */
    /* JADX WARNING: Missing block: B:27:0x0077, code:
            return r1.execute(r22, r23, r15);
     */
    /* JADX WARNING: Missing block: B:28:0x0078, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:30:0x007e, code:
            throw new org.apache.http.client.ClientProtocolException(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
        Throwable th;
        HttpHost httpHost;
        HttpContext httpContext;
        HttpRequest httpRequest;
        HttpRequest httpRequest2 = request;
        HttpContext httpContext2 = context;
        if (httpRequest2 != null) {
            boolean isBlocked = checkRequestForMms(httpRequest2);
            synchronized (this) {
                try {
                    HttpContext execContext;
                    HttpContext defaultContext = createHttpContext();
                    if (httpContext2 == null) {
                        execContext = defaultContext;
                    } else {
                        Object execContext2 = new DefaultedHttpContext(httpContext2, defaultContext);
                    }
                    HttpContext execContext3 = execContext2;
                    try {
                        HttpRequestExecutor requestExecutor = getRequestExecutor();
                        ClientConnectionManager connectionManager = getConnectionManager();
                        ConnectionReuseStrategy connectionReuseStrategy = getConnectionReuseStrategy();
                        ConnectionKeepAliveStrategy connectionKeepAliveStrategy = getConnectionKeepAliveStrategy();
                        HttpRoutePlanner routePlanner = getRoutePlanner();
                        HttpProcessor copy = getHttpProcessor().copy();
                        HttpRequestRetryHandler httpRequestRetryHandler = getHttpRequestRetryHandler();
                        RedirectHandler redirectHandler = getRedirectHandler();
                        AuthenticationHandler targetAuthenticationHandler = getTargetAuthenticationHandler();
                        AuthenticationHandler proxyAuthenticationHandler = getProxyAuthenticationHandler();
                        UserTokenHandler userTokenHandler = getUserTokenHandler();
                        HttpParams determineParams = determineParams(httpRequest2);
                        HttpContext execContext4 = execContext3;
                        try {
                            RequestDirector director = createClientRequestDirector(requestExecutor, connectionManager, connectionReuseStrategy, connectionKeepAliveStrategy, routePlanner, copy, httpRequestRetryHandler, redirectHandler, targetAuthenticationHandler, proxyAuthenticationHandler, userTokenHandler, determineParams);
                            try {
                            } catch (Throwable th2) {
                                th = th2;
                                httpHost = target;
                                httpContext = execContext4;
                                httpRequest = request;
                                RequestDirector requestDirector = director;
                                execContext2 = httpContext;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th3) {
                                        th = th3;
                                    }
                                }
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            httpHost = target;
                            httpContext = execContext4;
                            httpRequest = request;
                            execContext2 = httpContext;
                            while (true) {
                                break;
                            }
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        httpHost = target;
                        httpRequest = httpRequest2;
                        execContext2 = execContext3;
                        while (true) {
                            break;
                        }
                        throw th;
                    }
                } catch (Throwable th6) {
                    th = th6;
                    httpHost = target;
                    httpRequest = httpRequest2;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
        }
        httpHost = target;
        httpRequest = httpRequest2;
        throw new IllegalArgumentException("Request must not be null.");
    }

    protected RequestDirector createClientRequestDirector(HttpRequestExecutor requestExec, ClientConnectionManager conman, ConnectionReuseStrategy reustrat, ConnectionKeepAliveStrategy kastrat, HttpRoutePlanner rouplan, HttpProcessor httpProcessor, HttpRequestRetryHandler retryHandler, RedirectHandler redirectHandler, AuthenticationHandler targetAuthHandler, AuthenticationHandler proxyAuthHandler, UserTokenHandler stateHandler, HttpParams params) {
        return new DefaultRequestDirector(requestExec, conman, reustrat, kastrat, rouplan, httpProcessor, retryHandler, redirectHandler, targetAuthHandler, proxyAuthHandler, stateHandler, params);
    }

    protected HttpParams determineParams(HttpRequest req) {
        return new ClientParamsStack(null, getParams(), req.getParams(), null);
    }

    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return execute(request, (ResponseHandler) responseHandler, null);
    }

    public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        return execute(determineTarget(request), request, responseHandler, context);
    }

    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
        return execute(target, request, responseHandler, null);
    }

    public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
        if (responseHandler != null) {
            HttpResponse response = execute(target, request, context);
            HttpEntity entity;
            try {
                T result = responseHandler.handleResponse(response);
                entity = response.getEntity();
                if (entity != null) {
                    entity.consumeContent();
                }
                return result;
            } catch (Throwable t2) {
                this.log.warn("Error consuming content after an exception.", t2);
            }
        } else {
            throw new IllegalArgumentException("Response handler must not be null.");
        }
        if (t instanceof Error) {
            Error error = (Error) t;
        } else if (t instanceof RuntimeException) {
            RuntimeException runtimeException = (RuntimeException) t;
        } else if (t instanceof IOException) {
            IOException iOException = (IOException) t;
        } else {
            UndeclaredThrowableException undeclaredThrowableException = new UndeclaredThrowableException(t);
        }
    }

    private boolean checkRequestForMms(HttpRequest request) {
        HttpRequest httpRequest = request;
        if (httpRequest instanceof HttpPost) {
            String contentType = null;
            try {
                HttpEntity entity = ((HttpPost) httpRequest).getEntity();
                if (!(entity == null || entity.getContentType() == null)) {
                    if (MMS_CONTENT_TYPE.equals(entity.getContentType().getValue())) {
                        InputStream mmsPdu = entity.getContent();
                        int typeName = 0;
                        int typeValue = 0;
                        if (mmsPdu != null) {
                            typeName = mmsPdu.read();
                            typeValue = mmsPdu.read();
                        }
                        if (MMS_SEND_TYPE_NAME == typeName && MMS_SEND_TYPE_VALUE == typeValue) {
                            Class<?> clazz = Class.forName("com.huawei.hsm.permission.ConnectPermission");
                            if (clazz.getField("isControl").getBoolean(clazz)) {
                                mmsPdu.read(new byte[1000]);
                                boolean z = true;
                                Boolean isBlocked = (Boolean) clazz.getDeclaredMethod("isBlocked", new Class[]{byte[].class}).invoke(clazz.newInstance(), new Object[]{pduBuf});
                                if (isBlocked == null || !isBlocked.booleanValue()) {
                                    z = false;
                                }
                                return z;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private BasicHttpResponse getFakeResponse() {
        return new BasicHttpResponse(new ProtocolVersion("HTTP", 100, FAKE_PROTOCOL_MINOR_VERSION), 404, FAKE_HTTP_RESPONSE_REASON);
    }
}
