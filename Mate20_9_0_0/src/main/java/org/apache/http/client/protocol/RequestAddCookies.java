package org.apache.http.client.protocol;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.ProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecRegistry;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

@Deprecated
public class RequestAddCookies implements HttpRequestInterceptor {
    private final Log log = LogFactory.getLog(getClass());

    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        RequestAddCookies requestAddCookies = this;
        HttpRequest httpRequest = request;
        HttpContext httpContext = context;
        if (httpRequest == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        } else if (httpContext != null) {
            CookieStore cookieStore = (CookieStore) httpContext.getAttribute(ClientContext.COOKIE_STORE);
            if (cookieStore == null) {
                requestAddCookies.log.info("Cookie store not available in HTTP context");
                return;
            }
            CookieSpecRegistry registry = (CookieSpecRegistry) httpContext.getAttribute(ClientContext.COOKIESPEC_REGISTRY);
            if (registry == null) {
                requestAddCookies.log.info("CookieSpec registry not available in HTTP context");
                return;
            }
            HttpHost targetHost = (HttpHost) httpContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            CookieSpecRegistry cookieSpecRegistry;
            if (targetHost != null) {
                ManagedClientConnection conn = (ManagedClientConnection) httpContext.getAttribute(ExecutionContext.HTTP_CONNECTION);
                if (conn != null) {
                    URI requestURI;
                    CookieStore cookieStore2;
                    String policy = HttpClientParams.getCookiePolicy(request.getParams());
                    if (requestAddCookies.log.isDebugEnabled()) {
                        Log log = requestAddCookies.log;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("CookieSpec selected: ");
                        stringBuilder.append(policy);
                        log.debug(stringBuilder.toString());
                    }
                    if (httpRequest instanceof HttpUriRequest) {
                        requestURI = ((HttpUriRequest) httpRequest).getURI();
                    } else {
                        try {
                            requestURI = new URI(request.getRequestLine().getUri());
                        } catch (URISyntaxException ex) {
                            cookieStore2 = cookieStore;
                            cookieSpecRegistry = registry;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Invalid request URI: ");
                            stringBuilder2.append(request.getRequestLine().getUri());
                            throw new ProtocolException(stringBuilder2.toString(), ex);
                        }
                    }
                    String hostName = targetHost.getHostName();
                    int port = targetHost.getPort();
                    if (port < 0) {
                        port = conn.getRemotePort();
                    }
                    CookieOrigin cookieOrigin = new CookieOrigin(hostName, port, requestURI.getPath(), conn.isSecure());
                    CookieSpec cookieSpec = registry.getCookieSpec(policy, request.getParams());
                    List<Cookie> cookies = new ArrayList(cookieStore.getCookies());
                    List<Cookie> matchedCookies = new ArrayList();
                    for (Cookie requestURI2 : cookies) {
                        URI requestURI3 = requestURI;
                        if (cookieSpec.match(requestURI2, cookieOrigin)) {
                            cookieStore2 = cookieStore;
                            if (requestAddCookies.log.isDebugEnabled()) {
                                Log log2 = requestAddCookies.log;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                cookieSpecRegistry = registry;
                                stringBuilder3.append("Cookie ");
                                stringBuilder3.append(requestURI2);
                                stringBuilder3.append(" match ");
                                stringBuilder3.append(cookieOrigin);
                                log2.debug(stringBuilder3.toString());
                            } else {
                                cookieSpecRegistry = registry;
                            }
                            matchedCookies.add(requestURI2);
                        } else {
                            cookieStore2 = cookieStore;
                            cookieSpecRegistry = registry;
                        }
                        requestURI = requestURI3;
                        cookieStore = cookieStore2;
                        registry = cookieSpecRegistry;
                        requestAddCookies = this;
                    }
                    cookieStore2 = cookieStore;
                    cookieSpecRegistry = registry;
                    if (!matchedCookies.isEmpty()) {
                        for (Header header : cookieSpec.formatCookies(matchedCookies)) {
                            httpRequest.addHeader(header);
                        }
                    }
                    int ver = cookieSpec.getVersion();
                    if (ver > 0) {
                        boolean needVersionHeader = false;
                        for (Cookie registry2 : matchedCookies) {
                            if (ver != registry2.getVersion()) {
                                needVersionHeader = true;
                            }
                        }
                        if (needVersionHeader) {
                            cookieStore = cookieSpec.getVersionHeader();
                            if (cookieStore != null) {
                                httpRequest.addHeader(cookieStore);
                            }
                        }
                    }
                    httpContext.setAttribute(ClientContext.COOKIE_SPEC, cookieSpec);
                    httpContext.setAttribute(ClientContext.COOKIE_ORIGIN, cookieOrigin);
                    return;
                }
                cookieSpecRegistry = registry;
                throw new IllegalStateException("Client connection not specified in HTTP context");
            }
            cookieSpecRegistry = registry;
            throw new IllegalStateException("Target host not specified in HTTP context");
        } else {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
    }
}
