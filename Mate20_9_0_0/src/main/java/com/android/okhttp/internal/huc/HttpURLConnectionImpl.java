package com.android.okhttp.internal.huc;

import com.android.okhttp.Connection;
import com.android.okhttp.Handshake;
import com.android.okhttp.Headers;
import com.android.okhttp.Headers.Builder;
import com.android.okhttp.HttpUrl;
import com.android.okhttp.OkHttpClient;
import com.android.okhttp.Protocol;
import com.android.okhttp.Request;
import com.android.okhttp.RequestBody;
import com.android.okhttp.Response;
import com.android.okhttp.Route;
import com.android.okhttp.internal.Internal;
import com.android.okhttp.internal.Platform;
import com.android.okhttp.internal.URLFilter;
import com.android.okhttp.internal.Util;
import com.android.okhttp.internal.Version;
import com.android.okhttp.internal.http.HttpDate;
import com.android.okhttp.internal.http.HttpEngine;
import com.android.okhttp.internal.http.HttpMethod;
import com.android.okhttp.internal.http.OkHeaders;
import com.android.okhttp.internal.http.RequestException;
import com.android.okhttp.internal.http.RetryableSink;
import com.android.okhttp.internal.http.RouteException;
import com.android.okhttp.internal.http.StatusLine;
import com.android.okhttp.internal.http.StreamAllocation;
import com.android.okhttp.okio.BufferedSink;
import com.android.okhttp.okio.Sink;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketPermission;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class HttpURLConnectionImpl extends HttpURLConnection {
    private static final RequestBody EMPTY_REQUEST_BODY = RequestBody.create(null, new byte[0]);
    private static final Set<String> METHODS = new LinkedHashSet(Arrays.asList(new String[]{"OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "PATCH"}));
    final OkHttpClient client;
    private long fixedContentLength;
    private int followUpCount;
    Handshake handshake;
    protected HttpEngine httpEngine;
    protected IOException httpEngineFailure;
    private Builder requestHeaders;
    private Headers responseHeaders;
    private Route route;
    private URLFilter urlFilter;

    public HttpURLConnectionImpl(URL url, OkHttpClient client) {
        super(url);
        this.requestHeaders = new Builder();
        this.fixedContentLength = -1;
        this.client = client;
    }

    public HttpURLConnectionImpl(URL url, OkHttpClient client, URLFilter urlFilter) {
        this(url, client);
        this.urlFilter = urlFilter;
    }

    public final void connect() throws IOException {
        initHttpEngine();
        do {
        } while (!execute(false));
    }

    public final void disconnect() {
        if (this.httpEngine != null) {
            this.httpEngine.cancel();
        }
    }

    public final InputStream getErrorStream() {
        try {
            HttpEngine response = getResponse();
            if (!HttpEngine.hasBody(response.getResponse()) || response.getResponse().code() < 400) {
                return null;
            }
            return response.getResponse().body().byteStream();
        } catch (IOException e) {
            return null;
        }
    }

    private Headers getHeaders() throws IOException {
        if (this.responseHeaders == null) {
            Response response = getResponse().getResponse();
            this.responseHeaders = response.headers().newBuilder().add(OkHeaders.SELECTED_PROTOCOL, response.protocol().toString()).add(OkHeaders.RESPONSE_SOURCE, responseSourceHeader(response)).build();
        }
        return this.responseHeaders;
    }

    private static String responseSourceHeader(Response response) {
        StringBuilder stringBuilder;
        if (response.networkResponse() == null) {
            if (response.cacheResponse() == null) {
                return "NONE";
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("CACHE ");
            stringBuilder.append(response.code());
            return stringBuilder.toString();
        } else if (response.cacheResponse() == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("NETWORK ");
            stringBuilder.append(response.code());
            return stringBuilder.toString();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("CONDITIONAL_CACHE ");
            stringBuilder.append(response.networkResponse().code());
            return stringBuilder.toString();
        }
    }

    public final String getHeaderField(int position) {
        try {
            return getHeaders().value(position);
        } catch (IOException e) {
            return null;
        }
    }

    public final String getHeaderField(String fieldName) {
        IOException e;
        if (fieldName == null) {
            try {
                e = StatusLine.get(getResponse().getResponse()).toString();
            } catch (IOException e2) {
                return null;
            }
        }
        e = getHeaders().get(fieldName);
        return e;
    }

    public final String getHeaderFieldKey(int position) {
        try {
            return getHeaders().name(position);
        } catch (IOException e) {
            return null;
        }
    }

    public final Map<String, List<String>> getHeaderFields() {
        try {
            return OkHeaders.toMultimap(getHeaders(), StatusLine.get(getResponse().getResponse()).toString());
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    public final Map<String, List<String>> getRequestProperties() {
        if (!this.connected) {
            return OkHeaders.toMultimap(this.requestHeaders.build(), null);
        }
        throw new IllegalStateException("Cannot access request header fields after connection is set");
    }

    public final InputStream getInputStream() throws IOException {
        if (this.doInput) {
            HttpEngine response = getResponse();
            if (getResponseCode() < 400) {
                return response.getResponse().body().byteStream();
            }
            throw new FileNotFoundException(this.url.toString());
        }
        throw new ProtocolException("This protocol does not support input");
    }

    public final OutputStream getOutputStream() throws IOException {
        connect();
        BufferedSink sink = this.httpEngine.getBufferedRequestBody();
        if (sink == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("method does not support a request body: ");
            stringBuilder.append(this.method);
            throw new ProtocolException(stringBuilder.toString());
        } else if (!this.httpEngine.hasResponse()) {
            return sink.outputStream();
        } else {
            throw new ProtocolException("cannot write request body after response has been read");
        }
    }

    public final Permission getPermission() throws IOException {
        int hostPort;
        URL url = getURL();
        String hostName = url.getHost();
        if (url.getPort() != -1) {
            hostPort = url.getPort();
        } else {
            hostPort = HttpUrl.defaultPort(url.getProtocol());
        }
        if (usingProxy()) {
            InetSocketAddress proxyAddress = (InetSocketAddress) this.client.getProxy().address();
            hostName = proxyAddress.getHostName();
            hostPort = proxyAddress.getPort();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(hostName);
        stringBuilder.append(":");
        stringBuilder.append(hostPort);
        return new SocketPermission(stringBuilder.toString(), "connect, resolve");
    }

    public final String getRequestProperty(String field) {
        if (field == null) {
            return null;
        }
        return this.requestHeaders.get(field);
    }

    public void setConnectTimeout(int timeoutMillis) {
        this.client.setConnectTimeout((long) timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public void setInstanceFollowRedirects(boolean followRedirects) {
        this.client.setFollowRedirects(followRedirects);
    }

    public boolean getInstanceFollowRedirects() {
        return this.client.getFollowRedirects();
    }

    public int getConnectTimeout() {
        return this.client.getConnectTimeout();
    }

    public void setReadTimeout(int timeoutMillis) {
        this.client.setReadTimeout((long) timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public int getReadTimeout() {
        return this.client.getReadTimeout();
    }

    private void initHttpEngine() throws IOException {
        if (this.httpEngineFailure != null) {
            throw this.httpEngineFailure;
        } else if (this.httpEngine == null) {
            this.connected = true;
            try {
                if (this.doOutput) {
                    if (this.method.equals("GET")) {
                        this.method = "POST";
                    } else if (!HttpMethod.permitsRequestBody(this.method)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(this.method);
                        stringBuilder.append(" does not support writing");
                        throw new ProtocolException(stringBuilder.toString());
                    }
                }
                this.httpEngine = newHttpEngine(this.method, null, null, null);
            } catch (IOException e) {
                this.httpEngineFailure = e;
                throw e;
            }
        }
    }

    private HttpEngine newHttpEngine(String method, StreamAllocation streamAllocation, RetryableSink requestBody, Response priorResponse) throws MalformedURLException, UnknownHostException {
        RequestBody placeholderBody;
        if (HttpMethod.requiresRequestBody(method)) {
            placeholderBody = EMPTY_REQUEST_BODY;
        } else {
            placeholderBody = null;
        }
        Request.Builder builder = new Request.Builder().url(Internal.instance.getHttpUrlChecked(getURL().toString())).method(method, placeholderBody);
        Headers headers = this.requestHeaders.build();
        int size = headers.size();
        for (int i = 0; i < size; i++) {
            builder.addHeader(headers.name(i), headers.value(i));
        }
        boolean bufferRequestBody = false;
        if (HttpMethod.permitsRequestBody(method)) {
            if (this.fixedContentLength != -1) {
                builder.header("Content-Length", Long.toString(this.fixedContentLength));
            } else if (this.chunkLength > 0) {
                builder.header("Transfer-Encoding", "chunked");
            } else {
                bufferRequestBody = true;
            }
            if (headers.get("Content-Type") == null) {
                builder.header("Content-Type", "application/x-www-form-urlencoded");
            }
        }
        if (headers.get("User-Agent") == null) {
            builder.header("User-Agent", defaultUserAgent());
        }
        Request request = builder.build();
        OkHttpClient engineClient = this.client;
        if (!(Internal.instance.internalCache(engineClient) == null || getUseCaches())) {
            engineClient = this.client.clone().setCache(null);
        }
        return new HttpEngine(engineClient, request, bufferRequestBody, true, false, streamAllocation, requestBody, priorResponse);
    }

    private String defaultUserAgent() {
        String agent = System.getProperty("http.agent");
        return agent != null ? Util.toHumanReadableAscii(agent) : Version.userAgent();
    }

    private HttpEngine getResponse() throws IOException {
        initHttpEngine();
        if (this.httpEngine.hasResponse()) {
            return this.httpEngine;
        }
        while (true) {
            if (execute(true)) {
                Response response = this.httpEngine.getResponse();
                Request followUp = this.httpEngine.followUpRequest();
                if (followUp == null) {
                    this.httpEngine.releaseStreamAllocation();
                    return this.httpEngine;
                }
                int i = this.followUpCount + 1;
                this.followUpCount = i;
                if (i <= 20) {
                    this.url = followUp.url();
                    this.requestHeaders = followUp.headers().newBuilder();
                    Sink requestBody = this.httpEngine.getRequestBody();
                    if (!followUp.method().equals(this.method)) {
                        requestBody = null;
                    }
                    if (requestBody == null || (requestBody instanceof RetryableSink)) {
                        StreamAllocation streamAllocation = this.httpEngine.close();
                        if (!this.httpEngine.sameConnection(followUp.httpUrl())) {
                            streamAllocation.release();
                            streamAllocation = null;
                        }
                        this.httpEngine = newHttpEngine(followUp.method(), streamAllocation, (RetryableSink) requestBody, response);
                    } else {
                        throw new HttpRetryException("Cannot retry streamed HTTP body", this.responseCode);
                    }
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Too many follow-up requests: ");
                stringBuilder.append(this.followUpCount);
                throw new ProtocolException(stringBuilder.toString());
            }
        }
    }

    private boolean execute(boolean readResponse) throws IOException {
        IOException toThrow;
        HttpEngine retryEngine;
        if (this.urlFilter != null) {
            this.urlFilter.checkURLPermitted(this.httpEngine.getRequest().url());
        }
        try {
            this.httpEngine.sendRequest();
            Connection connection = this.httpEngine.getConnection();
            if (connection != null) {
                this.route = connection.getRoute();
                this.handshake = connection.getHandshake();
            } else {
                this.route = null;
                this.handshake = null;
            }
            if (readResponse) {
                this.httpEngine.readResponse();
            }
            if (false) {
                this.httpEngine.close().release();
            }
            return true;
        } catch (RequestException e) {
            toThrow = e.getCause();
            this.httpEngineFailure = toThrow;
            throw toThrow;
        } catch (RouteException e2) {
            retryEngine = this.httpEngine.recover(e2);
            if (retryEngine != null) {
                this.httpEngine = retryEngine;
                if (false) {
                    this.httpEngine.close().release();
                }
                return false;
            }
            IOException toThrow2 = e2.getLastConnectException();
            this.httpEngineFailure = toThrow2;
            throw toThrow2;
        } catch (IOException toThrow3) {
            retryEngine = this.httpEngine.recover(toThrow3);
            if (retryEngine != null) {
                this.httpEngine = retryEngine;
                if (false) {
                    this.httpEngine.close().release();
                }
                return false;
            }
            this.httpEngineFailure = toThrow3;
            throw toThrow3;
        } catch (Throwable th) {
            if (true) {
                this.httpEngine.close().release();
            }
        }
    }

    public final boolean usingProxy() {
        Proxy proxy;
        if (this.route != null) {
            proxy = this.route.getProxy();
        } else {
            proxy = this.client.getProxy();
        }
        return (proxy == null || proxy.type() == Type.DIRECT) ? false : true;
    }

    public String getResponseMessage() throws IOException {
        return getResponse().getResponse().message();
    }

    public final int getResponseCode() throws IOException {
        return getResponse().getResponse().code();
    }

    public final void setRequestProperty(String field, String newValue) {
        if (this.connected) {
            throw new IllegalStateException("Cannot set request property after connection is made");
        } else if (field == null) {
            throw new NullPointerException("field == null");
        } else if (newValue == null) {
            Platform platform = Platform.get();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring header ");
            stringBuilder.append(field);
            stringBuilder.append(" because its value was null.");
            platform.logW(stringBuilder.toString());
        } else {
            if ("X-Android-Transports".equals(field) || "X-Android-Protocols".equals(field)) {
                setProtocols(newValue, false);
            } else {
                this.requestHeaders.set(field, newValue);
            }
        }
    }

    public void setIfModifiedSince(long newValue) {
        super.setIfModifiedSince(newValue);
        if (this.ifModifiedSince != 0) {
            this.requestHeaders.set("If-Modified-Since", HttpDate.format(new Date(this.ifModifiedSince)));
        } else {
            this.requestHeaders.removeAll("If-Modified-Since");
        }
    }

    public final void addRequestProperty(String field, String value) {
        if (this.connected) {
            throw new IllegalStateException("Cannot add request property after connection is made");
        } else if (field == null) {
            throw new NullPointerException("field == null");
        } else if (value == null) {
            Platform platform = Platform.get();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring header ");
            stringBuilder.append(field);
            stringBuilder.append(" because its value was null.");
            platform.logW(stringBuilder.toString());
        } else {
            if ("X-Android-Transports".equals(field) || "X-Android-Protocols".equals(field)) {
                setProtocols(value, true);
            } else {
                this.requestHeaders.add(field, value);
            }
        }
    }

    private void setProtocols(String protocolsString, boolean append) {
        List<Protocol> protocolsList = new ArrayList();
        if (append) {
            protocolsList.addAll(this.client.getProtocols());
        }
        String[] split = protocolsString.split(",", -1);
        int length = split.length;
        int i = 0;
        while (i < length) {
            try {
                protocolsList.add(Protocol.get(split[i]));
                i++;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        this.client.setProtocols(protocolsList);
    }

    public void setRequestMethod(String method) throws ProtocolException {
        if (METHODS.contains(method)) {
            this.method = method;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Expected one of ");
        stringBuilder.append(METHODS);
        stringBuilder.append(" but was ");
        stringBuilder.append(method);
        throw new ProtocolException(stringBuilder.toString());
    }

    public void setFixedLengthStreamingMode(int contentLength) {
        setFixedLengthStreamingMode((long) contentLength);
    }

    public void setFixedLengthStreamingMode(long contentLength) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        } else if (this.chunkLength > 0) {
            throw new IllegalStateException("Already in chunked mode");
        } else if (contentLength >= 0) {
            this.fixedContentLength = contentLength;
            this.fixedContentLength = (int) Math.min(contentLength, 2147483647L);
        } else {
            throw new IllegalArgumentException("contentLength < 0");
        }
    }
}
