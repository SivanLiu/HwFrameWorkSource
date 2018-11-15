package org.apache.http.impl.client;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.NonRepeatableRequestException;
import org.apache.http.client.RedirectException;
import org.apache.http.client.RedirectHandler;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.BasicManagedEntity;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.routing.BasicRouteDirector;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRouteDirector;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;

@Deprecated
public class DefaultRequestDirector implements RequestDirector {
    private static Method cleartextTrafficPermittedMethod;
    private static Object networkSecurityPolicy;
    protected final ClientConnectionManager connManager;
    protected final HttpProcessor httpProcessor;
    protected final ConnectionKeepAliveStrategy keepAliveStrategy;
    private final Log log = LogFactory.getLog(getClass());
    protected ManagedClientConnection managedConn;
    private int maxRedirects;
    protected final HttpParams params;
    private final AuthenticationHandler proxyAuthHandler;
    private final AuthState proxyAuthState;
    private int redirectCount;
    protected final RedirectHandler redirectHandler;
    protected final HttpRequestExecutor requestExec;
    protected final HttpRequestRetryHandler retryHandler;
    protected final ConnectionReuseStrategy reuseStrategy;
    protected final HttpRoutePlanner routePlanner;
    private final AuthenticationHandler targetAuthHandler;
    private final AuthState targetAuthState;
    private final UserTokenHandler userTokenHandler;

    public DefaultRequestDirector(HttpRequestExecutor requestExec, ClientConnectionManager conman, ConnectionReuseStrategy reustrat, ConnectionKeepAliveStrategy kastrat, HttpRoutePlanner rouplan, HttpProcessor httpProcessor, HttpRequestRetryHandler retryHandler, RedirectHandler redirectHandler, AuthenticationHandler targetAuthHandler, AuthenticationHandler proxyAuthHandler, UserTokenHandler userTokenHandler, HttpParams params) {
        if (requestExec == null) {
            throw new IllegalArgumentException("Request executor may not be null.");
        } else if (conman == null) {
            throw new IllegalArgumentException("Client connection manager may not be null.");
        } else if (reustrat == null) {
            throw new IllegalArgumentException("Connection reuse strategy may not be null.");
        } else if (kastrat == null) {
            throw new IllegalArgumentException("Connection keep alive strategy may not be null.");
        } else if (rouplan == null) {
            throw new IllegalArgumentException("Route planner may not be null.");
        } else if (httpProcessor == null) {
            throw new IllegalArgumentException("HTTP protocol processor may not be null.");
        } else if (retryHandler == null) {
            throw new IllegalArgumentException("HTTP request retry handler may not be null.");
        } else if (redirectHandler == null) {
            throw new IllegalArgumentException("Redirect handler may not be null.");
        } else if (targetAuthHandler == null) {
            throw new IllegalArgumentException("Target authentication handler may not be null.");
        } else if (proxyAuthHandler == null) {
            throw new IllegalArgumentException("Proxy authentication handler may not be null.");
        } else if (userTokenHandler == null) {
            throw new IllegalArgumentException("User token handler may not be null.");
        } else if (params != null) {
            this.requestExec = requestExec;
            this.connManager = conman;
            this.reuseStrategy = reustrat;
            this.keepAliveStrategy = kastrat;
            this.routePlanner = rouplan;
            this.httpProcessor = httpProcessor;
            this.retryHandler = retryHandler;
            this.redirectHandler = redirectHandler;
            this.targetAuthHandler = targetAuthHandler;
            this.proxyAuthHandler = proxyAuthHandler;
            this.userTokenHandler = userTokenHandler;
            this.params = params;
            this.managedConn = null;
            this.redirectCount = 0;
            this.maxRedirects = this.params.getIntParameter(ClientPNames.MAX_REDIRECTS, 100);
            this.targetAuthState = new AuthState();
            this.proxyAuthState = new AuthState();
        } else {
            throw new IllegalArgumentException("HTTP parameters may not be null");
        }
    }

    private RequestWrapper wrapRequest(HttpRequest request) throws ProtocolException {
        if (request instanceof HttpEntityEnclosingRequest) {
            return new EntityEnclosingRequestWrapper((HttpEntityEnclosingRequest) request);
        }
        return new RequestWrapper(request);
    }

    protected void rewriteRequestURI(RequestWrapper request, HttpRoute route) throws ProtocolException {
        try {
            URI uri = request.getURI();
            if (route.getProxyHost() == null || route.isTunnelled()) {
                if (uri.isAbsolute()) {
                    request.setURI(URIUtils.rewriteURI(uri, null));
                }
            } else if (!uri.isAbsolute()) {
                request.setURI(URIUtils.rewriteURI(uri, route.getTargetHost()));
            }
        } catch (URISyntaxException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid URI: ");
            stringBuilder.append(request.getRequestLine().getUri());
            throw new ProtocolException(stringBuilder.toString(), ex);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:35:0x00a8 A:{Splitter: B:8:0x0044, ExcHandler: org.apache.http.HttpException (e org.apache.http.HttpException)} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x009a A:{Splitter: B:8:0x0044, ExcHandler: java.lang.RuntimeException (e java.lang.RuntimeException)} */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x016b A:{Splitter: B:75:0x0156, ExcHandler: org.apache.http.HttpException (e org.apache.http.HttpException)} */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x0165 A:{Splitter: B:75:0x0156, ExcHandler: java.lang.RuntimeException (e java.lang.RuntimeException)} */
    /* JADX WARNING: Removed duplicated region for block: B:222:0x025e A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x01ef A:{Catch:{ HttpException -> 0x02e7, IOException -> 0x02e2, RuntimeException -> 0x02dd }} */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x016b A:{Splitter: B:75:0x0156, ExcHandler: org.apache.http.HttpException (e org.apache.http.HttpException)} */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x0165 A:{Splitter: B:75:0x0156, ExcHandler: java.lang.RuntimeException (e java.lang.RuntimeException)} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:29:0x009a, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:30:0x009b, code:
            r21 = r3;
     */
    /* JADX WARNING: Missing block: B:31:0x009d, code:
            r23 = r5;
     */
    /* JADX WARNING: Missing block: B:34:0x00a4, code:
            r23 = r5;
     */
    /* JADX WARNING: Missing block: B:35:0x00a8, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:36:0x00a9, code:
            r21 = r3;
     */
    /* JADX WARNING: Missing block: B:37:0x00ab, code:
            r23 = r5;
     */
    /* JADX WARNING: Missing block: B:80:0x0165, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:81:0x0168, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:82:0x016b, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:100:0x01d8, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:101:0x01d9, code:
            r22 = r4;
     */
    /* JADX WARNING: Missing block: B:106:0x01f5, code:
            if (r1.log.isInfoEnabled() != false) goto L_0x01f7;
     */
    /* JADX WARNING: Missing block: B:107:0x01f7, code:
            r3 = r1.log;
            r4 = new java.lang.StringBuilder();
     */
    /* JADX WARNING: Missing block: B:108:0x01fe, code:
            r23 = r5;
     */
    /* JADX WARNING: Missing block: B:110:?, code:
            r4.append("I/O exception (");
            r4.append(r0.getClass().getName());
            r4.append(") caught when processing request: ");
            r4.append(r0.getMessage());
            r3.info(r4.toString());
     */
    /* JADX WARNING: Missing block: B:111:0x0224, code:
            r23 = r5;
     */
    /* JADX WARNING: Missing block: B:113:0x022c, code:
            if (r1.log.isDebugEnabled() != false) goto L_0x022e;
     */
    /* JADX WARNING: Missing block: B:114:0x022e, code:
            r1.log.debug(r0.getMessage(), r0);
     */
    /* JADX WARNING: Missing block: B:115:0x0237, code:
            r1.log.info("Retrying request");
     */
    /* JADX WARNING: Missing block: B:116:0x0243, code:
            if (r15.getHopCount() == 1) goto L_0x0245;
     */
    /* JADX WARNING: Missing block: B:117:0x0245, code:
            r1.log.debug("Reopening the direct connection.");
            r1.managedConn.open(r15, r2, r1.params);
            r3 = 1;
            r0 = r16;
            r4 = r22;
            r5 = r23;
     */
    /* JADX WARNING: Missing block: B:118:0x025d, code:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:119:0x025e, code:
            r23 = r5;
     */
    /* JADX WARNING: Missing block: B:120:0x0260, code:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:121:0x0261, code:
            r22 = r4;
            r23 = r5;
            r11.setParams(r1.params);
            r1.requestExec.postProcess(r11, r1.httpProcessor, r2);
            r10 = r1.reuseStrategy.keepAlive(r11, r2);
     */
    /* JADX WARNING: Missing block: B:122:0x0278, code:
            if (r10 == false) goto L_0x0287;
     */
    /* JADX WARNING: Missing block: B:123:0x027a, code:
            r1.managedConn.setIdleDuration(r1.keepAliveStrategy.getKeepAliveDuration(r11, r2), java.util.concurrent.TimeUnit.MILLISECONDS);
     */
    /* JADX WARNING: Missing block: B:124:0x0287, code:
            r0 = handleResponse(r6, r11, r2);
     */
    /* JADX WARNING: Missing block: B:125:0x028b, code:
            if (r0 != null) goto L_0x0290;
     */
    /* JADX WARNING: Missing block: B:126:0x028d, code:
            r12 = true;
     */
    /* JADX WARNING: Missing block: B:127:0x0290, code:
            if (r10 == false) goto L_0x02a8;
     */
    /* JADX WARNING: Missing block: B:128:0x0292, code:
            r1.log.debug("Connection kept alive");
            r3 = r11.getEntity();
     */
    /* JADX WARNING: Missing block: B:129:0x029d, code:
            if (r3 == null) goto L_0x02a2;
     */
    /* JADX WARNING: Missing block: B:130:0x029f, code:
            r3.consumeContent();
     */
    /* JADX WARNING: Missing block: B:131:0x02a2, code:
            r1.managedConn.markReusable();
     */
    /* JADX WARNING: Missing block: B:132:0x02a8, code:
            r1.managedConn.close();
     */
    /* JADX WARNING: Missing block: B:134:0x02b9, code:
            if (r0.getRoute().equals(r6.getRoute()) != false) goto L_0x02be;
     */
    /* JADX WARNING: Missing block: B:135:0x02bb, code:
            releaseConnection();
     */
    /* JADX WARNING: Missing block: B:136:0x02be, code:
            r6 = r0;
     */
    /* JADX WARNING: Missing block: B:137:0x02c0, code:
            r3 = r1.userTokenHandler.getUserToken(r2);
            r2.setAttribute(org.apache.http.client.protocol.ClientContext.USER_TOKEN, r3);
     */
    /* JADX WARNING: Missing block: B:138:0x02cd, code:
            if (r1.managedConn == null) goto L_0x02d4;
     */
    /* JADX WARNING: Missing block: B:139:0x02cf, code:
            r1.managedConn.setState(r3);
     */
    /* JADX WARNING: Missing block: B:140:0x02d4, code:
            r4 = r17;
            r3 = r21;
            r5 = r23;
     */
    /* JADX WARNING: Missing block: B:171:0x035f, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:172:0x0361, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:173:0x0363, code:
            r0 = e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException, IOException {
        RequestWrapper origWrapper;
        HttpException ex;
        HttpRequest httpRequest;
        HttpRoute httpRoute;
        IOException ex2;
        HttpContext httpContext = context;
        HttpRequest orig = request;
        RequestWrapper origWrapper2 = wrapRequest(orig);
        origWrapper2.setParams(this.params);
        HttpHost httpHost = target;
        HttpRoute origRoute = determineRoute(httpHost, origWrapper2, httpContext);
        RoutedRequest roureq = new RoutedRequest(origWrapper2, origRoute);
        long timeout = ConnManagerParams.getTimeout(this.params);
        boolean reuse = false;
        HttpResponse response = null;
        boolean done = false;
        int execCount = 0;
        HttpHost target2 = httpHost;
        while (!done) {
            try {
                RequestWrapper wrapper = roureq.getRequest();
                HttpRoute route = roureq.getRoute();
                Object userToken = httpContext.getAttribute(ClientContext.USER_TOKEN);
                if (this.managedConn == null) {
                    Object userToken2;
                    try {
                        origWrapper = origWrapper2;
                        userToken2 = userToken;
                    } catch (HttpException e) {
                        ex = e;
                        origWrapper = origWrapper2;
                        httpRequest = orig;
                        httpRoute = origRoute;
                    } catch (IOException e2) {
                        ex = e2;
                        origWrapper = origWrapper2;
                        httpRequest = orig;
                        httpRoute = origRoute;
                    } catch (RuntimeException e3) {
                        ex2 = e3;
                        origWrapper = origWrapper2;
                        httpRequest = orig;
                        httpRoute = origRoute;
                    }
                    ClientConnectionRequest connRequest;
                    try {
                        ClientConnectionRequest connRequest2 = this.connManager.requestConnection(route, userToken2);
                        if (orig instanceof AbortableHttpRequest) {
                            connRequest = connRequest2;
                            ((AbortableHttpRequest) orig).setConnectionRequest(connRequest);
                        } else {
                            connRequest = connRequest2;
                        }
                        this.managedConn = connRequest.getConnection(timeout, TimeUnit.MILLISECONDS);
                        if (HttpConnectionParams.isStaleCheckingEnabled(this.params)) {
                            this.log.debug("Stale connection check");
                            if (this.managedConn.isStale()) {
                                this.log.debug("Stale connection detected");
                                this.managedConn.close();
                            }
                        }
                    } catch (InterruptedException interrupted) {
                        ClientConnectionRequest clientConnectionRequest = connRequest;
                        origWrapper2 = new InterruptedIOException();
                        origWrapper2.initCause(interrupted);
                        throw origWrapper2;
                    } catch (HttpException e4) {
                    } catch (IOException e5) {
                        ex = e5;
                        httpRequest = orig;
                    } catch (RuntimeException e6) {
                    }
                } else {
                    origWrapper = origWrapper2;
                    Object obj = userToken;
                }
                try {
                    if (orig instanceof AbortableHttpRequest) {
                        ((AbortableHttpRequest) orig).setReleaseTrigger(this.managedConn);
                    }
                    if (this.managedConn.isOpen()) {
                        this.managedConn.setSocketTimeout(HttpConnectionParams.getSoTimeout(this.params));
                    } else {
                        this.managedConn.open(route, httpContext, this.params);
                    }
                    try {
                        establishRoute(route, httpContext);
                        wrapper.resetHeaders();
                        rewriteRequestURI(wrapper, route);
                        target2 = (HttpHost) wrapper.getParams().getParameter(ClientPNames.VIRTUAL_HOST);
                        if (target2 == null) {
                            target2 = route.getTargetHost();
                        }
                        HttpHost proxy = route.getProxyHost();
                        httpContext.setAttribute(ExecutionContext.HTTP_TARGET_HOST, target2);
                        httpContext.setAttribute(ExecutionContext.HTTP_PROXY_HOST, proxy);
                        httpRequest = orig;
                        try {
                            httpContext.setAttribute(ExecutionContext.HTTP_CONNECTION, this.managedConn);
                            httpContext.setAttribute(ClientContext.TARGET_AUTH_STATE, this.targetAuthState);
                            httpContext.setAttribute(ClientContext.PROXY_AUTH_STATE, this.proxyAuthState);
                            this.requestExec.preProcess(wrapper, this.httpProcessor, httpContext);
                            httpContext.setAttribute(ExecutionContext.HTTP_REQUEST, wrapper);
                            int i = 1;
                            boolean retrying = true;
                            while (true) {
                                boolean retrying2 = retrying;
                                if (!retrying2) {
                                    break;
                                }
                                StringBuilder stringBuilder;
                                HttpHost proxy2;
                                execCount++;
                                wrapper.incrementExecCount();
                                if (wrapper.getExecCount() > i) {
                                    try {
                                        if (!wrapper.isRepeatable()) {
                                            throw new NonRepeatableRequestException("Cannot retry request with a non-repeatable request entity");
                                        }
                                    } catch (IOException e7) {
                                        ex2 = e7;
                                        this.log.debug("Closing the connection.");
                                        this.managedConn.close();
                                        if (this.retryHandler.retryRequest(ex2, execCount, httpContext)) {
                                        }
                                    } catch (HttpException e8) {
                                    } catch (RuntimeException e9) {
                                    }
                                }
                                if (this.log.isDebugEnabled()) {
                                    Log log = this.log;
                                    stringBuilder = new StringBuilder();
                                    proxy2 = proxy;
                                    stringBuilder.append("Attempt ");
                                    stringBuilder.append(execCount);
                                    stringBuilder.append(" to execute request");
                                    log.debug(stringBuilder.toString());
                                } else {
                                    proxy2 = proxy;
                                }
                                if (route.isSecure() || isCleartextTrafficPermitted(route.getTargetHost().getHostName())) {
                                    response = this.requestExec.execute(wrapper, this.managedConn, httpContext);
                                    retrying = false;
                                    proxy = proxy2;
                                    i = 1;
                                } else {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Cleartext traffic not permitted: ");
                                    stringBuilder.append(route.getTargetHost());
                                    throw new IOException(stringBuilder.toString());
                                }
                            }
                        } catch (HttpException e10) {
                            ex = e10;
                            httpRoute = origRoute;
                            abortConnection();
                            throw ex;
                        } catch (IOException e11) {
                            ex = e11;
                            httpRoute = origRoute;
                            abortConnection();
                            throw ex;
                        } catch (RuntimeException e12) {
                            ex2 = e12;
                            httpRoute = origRoute;
                            abortConnection();
                            throw ex2;
                        }
                    } catch (TunnelRefusedException ex3) {
                        httpRequest = orig;
                        httpRoute = origRoute;
                        TunnelRefusedException orig2 = ex3;
                        if (this.log.isDebugEnabled()) {
                            this.log.debug(ex3.getMessage());
                        }
                        response = ex3.getResponse();
                    }
                } catch (HttpException e13) {
                    ex = e13;
                    httpRequest = orig;
                    httpRoute = origRoute;
                } catch (IOException e14) {
                    ex = e14;
                    httpRequest = orig;
                    httpRoute = origRoute;
                } catch (RuntimeException e15) {
                    ex2 = e15;
                    httpRequest = orig;
                    httpRoute = origRoute;
                }
            } catch (HttpException e16) {
                ex = e16;
                httpRequest = orig;
                origWrapper = origWrapper2;
                httpRoute = origRoute;
            } catch (IOException e17) {
                ex = e17;
                httpRequest = orig;
                origWrapper = origWrapper2;
                httpRoute = origRoute;
            } catch (RuntimeException e18) {
                ex2 = e18;
                httpRequest = orig;
                origWrapper = origWrapper2;
                httpRoute = origRoute;
            }
        }
        origWrapper = origWrapper2;
        httpRoute = origRoute;
        if (response == null || response.getEntity() == null || !response.getEntity().isStreaming()) {
            if (reuse) {
                this.managedConn.markReusable();
            }
            releaseConnection();
        } else {
            response.setEntity(new BasicManagedEntity(response.getEntity(), this.managedConn, reuse));
        }
        return response;
    }

    protected void releaseConnection() {
        try {
            this.managedConn.releaseConnection();
        } catch (IOException ignored) {
            this.log.debug("IOException releasing connection", ignored);
        }
        this.managedConn = null;
    }

    protected HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        if (target == null) {
            target = (HttpHost) request.getParams().getParameter(ClientPNames.DEFAULT_HOST);
        }
        if (target != null) {
            return this.routePlanner.determineRoute(target, request, context);
        }
        String scheme = null;
        String host = null;
        String path = null;
        if (request instanceof HttpUriRequest) {
            URI uri = ((HttpUriRequest) request).getURI();
            URI uri2 = uri;
            if (uri != null) {
                scheme = uri2.getScheme();
                host = uri2.getHost();
                path = uri2.getPath();
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Target host must not be null, or set in parameters. scheme=");
        stringBuilder.append(scheme);
        stringBuilder.append(", host=");
        stringBuilder.append(host);
        stringBuilder.append(", path=");
        stringBuilder.append(path);
        throw new IllegalStateException(stringBuilder.toString());
    }

    protected void establishRoute(HttpRoute route, HttpContext context) throws HttpException, IOException {
        HttpRouteDirector rowdy = new BasicRouteDirector();
        int step;
        do {
            HttpRoute fact = this.managedConn.getRoute();
            step = rowdy.nextStep(route, fact);
            StringBuilder stringBuilder;
            switch (step) {
                case -1:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to establish route.\nplanned = ");
                    stringBuilder.append(route);
                    stringBuilder.append("\ncurrent = ");
                    stringBuilder.append(fact);
                    throw new IllegalStateException(stringBuilder.toString());
                case 0:
                    break;
                case 1:
                case 2:
                    this.managedConn.open(route, context, this.params);
                    continue;
                case 3:
                    boolean secure = createTunnelToTarget(route, context);
                    this.log.debug("Tunnel to target created.");
                    this.managedConn.tunnelTarget(secure, this.params);
                    continue;
                case 4:
                    int hop = fact.getHopCount() - 1;
                    boolean secure2 = createTunnelToProxy(route, hop, context);
                    this.log.debug("Tunnel to proxy created.");
                    this.managedConn.tunnelProxy(route.getHopTarget(hop), secure2, this.params);
                    continue;
                case 5:
                    this.managedConn.layerProtocol(context, this.params);
                    continue;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown step indicator ");
                    stringBuilder.append(step);
                    stringBuilder.append(" from RouteDirector.");
                    throw new IllegalStateException(stringBuilder.toString());
            }
        } while (step > 0);
    }

    /* JADX WARNING: Removed duplicated region for block: B:62:0x01bb  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x018d  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x013e  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x011c  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected boolean createTunnelToTarget(HttpRoute route, HttpContext context) throws HttpException, IOException {
        boolean done;
        HttpRoute httpRoute;
        StringBuilder stringBuilder;
        AuthenticationException ex;
        HttpHost target;
        int i;
        Credentials credentials;
        AuthScheme authScheme;
        HttpContext httpContext = context;
        HttpHost proxy = route.getProxyHost();
        HttpHost target2 = route.getTargetHost();
        HttpResponse response = null;
        boolean done2 = false;
        while (!done2) {
            done = true;
            if (this.managedConn.isOpen()) {
                httpRoute = route;
            } else {
                this.managedConn.open(route, httpContext, this.params);
            }
            HttpRequest connect = createConnectRequest(route, context);
            String agent = HttpProtocolParams.getUserAgent(this.params);
            if (agent != null) {
                connect.addHeader(HTTP.USER_AGENT, agent);
            }
            connect.addHeader(HTTP.TARGET_HOST, target2.toHostString());
            AuthScheme authScheme2 = this.proxyAuthState.getAuthScheme();
            AuthScope authScope = this.proxyAuthState.getAuthScope();
            Credentials creds = this.proxyAuthState.getCredentials();
            if (!(creds == null || (authScope == null && authScheme2.isConnectionBased()))) {
                try {
                    connect.addHeader(authScheme2.authenticate(creds, connect));
                } catch (AuthenticationException ex2) {
                    if (this.log.isErrorEnabled()) {
                        Log log = this.log;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Proxy authentication error: ");
                        stringBuilder.append(ex2.getMessage());
                        log.error(stringBuilder.toString());
                    }
                }
            }
            HttpResponse response2 = this.requestExec.execute(connect, this.managedConn, httpContext);
            int status = response2.getStatusLine().getStatusCode();
            if (status >= HttpStatus.SC_OK) {
                CredentialsProvider credsProvider = (CredentialsProvider) httpContext.getAttribute(ClientContext.CREDS_PROVIDER);
                if (credsProvider == null || !HttpClientParams.isAuthenticating(this.params)) {
                    response = response2;
                    target = target2;
                } else if (this.proxyAuthHandler.isAuthenticationRequested(response2, httpContext)) {
                    HttpResponse response3;
                    this.log.debug("Proxy requested authentication");
                    try {
                        target = target2;
                        target2 = credsProvider;
                        response3 = response2;
                        try {
                            processChallenges(this.proxyAuthHandler.getChallenges(response2, httpContext), this.proxyAuthState, this.proxyAuthHandler, response3, httpContext);
                        } catch (AuthenticationException e) {
                            ex2 = e;
                        }
                    } catch (AuthenticationException e2) {
                        ex2 = e2;
                        i = status;
                        response3 = response2;
                        credentials = creds;
                        authScheme = authScheme2;
                        target = target2;
                        target2 = credsProvider;
                        if (this.log.isWarnEnabled()) {
                            Log log2 = this.log;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Authentication error: ");
                            stringBuilder.append(ex2.getMessage());
                            log2.warn(stringBuilder.toString());
                            response = response3;
                            if (response.getStatusLine().getStatusCode() <= 299) {
                            }
                        }
                        updateAuthState(this.proxyAuthState, proxy, target2);
                        if (this.proxyAuthState.getCredentials() == null) {
                        }
                        done2 = done;
                        target2 = target;
                    }
                    updateAuthState(this.proxyAuthState, proxy, target2);
                    if (this.proxyAuthState.getCredentials() == null) {
                        done = false;
                        response = response3;
                        if (this.reuseStrategy.keepAlive(response, httpContext)) {
                            this.log.debug("Connection kept alive");
                            HttpEntity entity = response.getEntity();
                            if (entity != null) {
                                entity.consumeContent();
                            }
                        } else {
                            this.managedConn.close();
                        }
                    } else {
                        response = response3;
                    }
                } else {
                    credentials = creds;
                    authScheme = authScheme2;
                    target = target2;
                    CredentialsProvider target3 = credsProvider;
                    response = response2;
                    this.proxyAuthState.setAuthScope(null);
                }
                done2 = done;
                target2 = target;
            } else {
                i = status;
                response = response2;
                credentials = creds;
                authScheme = authScheme2;
                target = target2;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Unexpected response to CONNECT request: ");
                stringBuilder2.append(response.getStatusLine());
                throw new HttpException(stringBuilder2.toString());
            }
        }
        httpRoute = route;
        target = target2;
        done = done2;
        if (response.getStatusLine().getStatusCode() <= 299) {
            HttpEntity entity2 = response.getEntity();
            if (entity2 != null) {
                response.setEntity(new BufferedHttpEntity(entity2));
            }
            this.managedConn.close();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("CONNECT refused by proxy: ");
            stringBuilder3.append(response.getStatusLine());
            throw new TunnelRefusedException(stringBuilder3.toString(), response);
        }
        this.managedConn.markReusable();
        return false;
    }

    protected boolean createTunnelToProxy(HttpRoute route, int hop, HttpContext context) throws HttpException, IOException {
        throw new UnsupportedOperationException("Proxy chains are not supported.");
    }

    protected HttpRequest createConnectRequest(HttpRoute route, HttpContext context) {
        HttpHost target = route.getTargetHost();
        String host = target.getHostName();
        int port = target.getPort();
        if (port < 0) {
            port = this.connManager.getSchemeRegistry().getScheme(target.getSchemeName()).getDefaultPort();
        }
        StringBuilder buffer = new StringBuilder(host.length() + 6);
        buffer.append(host);
        buffer.append(':');
        buffer.append(Integer.toString(port));
        return new BasicHttpRequest("CONNECT", buffer.toString(), HttpProtocolParams.getVersion(this.params));
    }

    /* JADX WARNING: Removed duplicated region for block: B:38:0x0135 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:37:0x0134 A:{RETURN} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected RoutedRequest handleResponse(RoutedRequest roureq, HttpResponse response, HttpContext context) throws HttpException, IOException {
        RoutedRequest request;
        AuthenticationException ex;
        Log log;
        StringBuilder stringBuilder;
        HttpResponse httpResponse = response;
        HttpContext httpContext = context;
        HttpRoute route = roureq.getRoute();
        HttpHost proxy = route.getProxyHost();
        RequestWrapper request2 = roureq.getRequest();
        HttpParams params = request2.getParams();
        StringBuilder stringBuilder2;
        if (!HttpClientParams.isRedirecting(params) || !this.redirectHandler.isRedirectRequested(httpResponse, httpContext)) {
            CredentialsProvider credsProvider = (CredentialsProvider) httpContext.getAttribute(ClientContext.CREDS_PROVIDER);
            RequestWrapper requestWrapper;
            if (credsProvider == null || !HttpClientParams.isAuthenticating(params)) {
                requestWrapper = request2;
                request2 = null;
            } else if (this.targetAuthHandler.isAuthenticationRequested(httpResponse, httpContext)) {
                HttpHost target = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (target == null) {
                    target = route.getTargetHost();
                }
                HttpHost target2 = target;
                this.log.debug("Target requested authentication");
                try {
                    route = target2;
                    request = null;
                    try {
                        processChallenges(this.targetAuthHandler.getChallenges(httpResponse, httpContext), this.targetAuthState, this.targetAuthHandler, httpResponse, httpContext);
                    } catch (AuthenticationException e) {
                        ex = e;
                    }
                } catch (AuthenticationException e2) {
                    ex = e2;
                    HttpRoute httpRoute = route;
                    requestWrapper = request2;
                    route = target2;
                    request = null;
                    if (this.log.isWarnEnabled()) {
                        log = this.log;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Authentication error: ");
                        stringBuilder.append(ex.getMessage());
                        log.warn(stringBuilder.toString());
                        return request;
                    }
                    updateAuthState(this.targetAuthState, route, credsProvider);
                    if (this.targetAuthState.getCredentials() == null) {
                    }
                }
                updateAuthState(this.targetAuthState, route, credsProvider);
                if (this.targetAuthState.getCredentials() == null) {
                    return roureq;
                }
                return request;
            } else {
                requestWrapper = request2;
                request2 = null;
                this.targetAuthState.setAuthScope(request2);
                if (this.proxyAuthHandler.isAuthenticationRequested(httpResponse, httpContext)) {
                    this.log.debug("Proxy requested authentication");
                    try {
                        processChallenges(this.proxyAuthHandler.getChallenges(httpResponse, httpContext), this.proxyAuthState, this.proxyAuthHandler, httpResponse, httpContext);
                    } catch (AuthenticationException ex2) {
                        if (this.log.isWarnEnabled()) {
                            log = this.log;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Authentication error: ");
                            stringBuilder.append(ex2.getMessage());
                            log.warn(stringBuilder.toString());
                            return request2;
                        }
                    }
                    updateAuthState(this.proxyAuthState, proxy, credsProvider);
                    if (this.proxyAuthState.getCredentials() != null) {
                        return roureq;
                    }
                    return request2;
                }
                this.proxyAuthState.setAuthScope(request2);
            }
            return request2;
        } else if (this.redirectCount < this.maxRedirects) {
            this.redirectCount++;
            URI uri = this.redirectHandler.getLocationURI(httpResponse, httpContext);
            HttpHost newTarget = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
            HttpGet redirect = new HttpGet(uri);
            redirect.setHeaders(request2.getOriginal().getAllHeaders());
            RequestWrapper wrapper = new RequestWrapper(redirect);
            wrapper.setParams(params);
            HttpRoute newRoute = determineRoute(newTarget, wrapper, httpContext);
            RoutedRequest newRequest = new RoutedRequest(wrapper, newRoute);
            if (this.log.isDebugEnabled()) {
                Log log2 = this.log;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Redirecting to '");
                stringBuilder2.append(uri);
                stringBuilder2.append("' via ");
                stringBuilder2.append(newRoute);
                log2.debug(stringBuilder2.toString());
            } else {
                HttpGet httpGet = redirect;
            }
            return newRequest;
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Maximum redirects (");
            stringBuilder2.append(this.maxRedirects);
            stringBuilder2.append(") exceeded");
            throw new RedirectException(stringBuilder2.toString());
        }
    }

    private void abortConnection() {
        ManagedClientConnection mcc = this.managedConn;
        if (mcc != null) {
            this.managedConn = null;
            try {
                mcc.abortConnection();
            } catch (IOException ex) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug(ex.getMessage(), ex);
                }
            }
            try {
                mcc.releaseConnection();
            } catch (IOException ex2) {
                this.log.debug("Error releasing connection", ex2);
            }
        }
    }

    private void processChallenges(Map<String, Header> challenges, AuthState authState, AuthenticationHandler authHandler, HttpResponse response, HttpContext context) throws MalformedChallengeException, AuthenticationException {
        AuthScheme authScheme = authState.getAuthScheme();
        if (authScheme == null) {
            authScheme = authHandler.selectScheme(challenges, response, context);
            authState.setAuthScheme(authScheme);
        }
        String id = authScheme.getSchemeName();
        Header challenge = (Header) challenges.get(id.toLowerCase(Locale.ENGLISH));
        if (challenge != null) {
            authScheme.processChallenge(challenge);
            this.log.debug("Authorization challenge processed");
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(id);
        stringBuilder.append(" authorization challenge expected, but not found");
        throw new AuthenticationException(stringBuilder.toString());
    }

    private void updateAuthState(AuthState authState, HttpHost host, CredentialsProvider credsProvider) {
        if (authState.isValid()) {
            String hostname = host.getHostName();
            int port = host.getPort();
            if (port < 0) {
                port = this.connManager.getSchemeRegistry().getScheme(host).getDefaultPort();
            }
            AuthScheme authScheme = authState.getAuthScheme();
            AuthScope authScope = new AuthScope(hostname, port, authScheme.getRealm(), authScheme.getSchemeName());
            if (this.log.isDebugEnabled()) {
                Log log = this.log;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Authentication scope: ");
                stringBuilder.append(authScope);
                log.debug(stringBuilder.toString());
            }
            Credentials creds = authState.getCredentials();
            if (creds == null) {
                creds = credsProvider.getCredentials(authScope);
                if (this.log.isDebugEnabled()) {
                    if (creds != null) {
                        this.log.debug("Found credentials");
                    } else {
                        this.log.debug("Credentials not found");
                    }
                }
            } else if (authScheme.isComplete()) {
                this.log.debug("Authentication failed");
                creds = null;
            }
            authState.setAuthScope(authScope);
            authState.setCredentials(creds);
        }
    }

    private static boolean isCleartextTrafficPermitted(String hostname) {
        try {
            Object policy;
            Method method;
            synchronized (DefaultRequestDirector.class) {
                if (cleartextTrafficPermittedMethod == null) {
                    Class<?> cls = Class.forName("android.security.NetworkSecurityPolicy");
                    networkSecurityPolicy = cls.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
                    cleartextTrafficPermittedMethod = cls.getMethod("isCleartextTrafficPermitted", new Class[]{String.class});
                }
                policy = networkSecurityPolicy;
                method = cleartextTrafficPermittedMethod;
            }
            return ((Boolean) method.invoke(policy, new Object[]{hostname})).booleanValue();
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }
}
