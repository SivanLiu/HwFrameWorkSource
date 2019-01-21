package com.huawei.okhttp3;

import com.huawei.okhttp3.internal.Util;
import com.huawei.okhttp3.internal.http.HttpMethod;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Request {
    final ArrayList<InetAddress> additionalInetAddresses;
    final RequestBody body;
    private volatile CacheControl cacheControl;
    final boolean concurrentConnectEnabled;
    final Headers headers;
    boolean http2Indicator = false;
    final String method;
    final Object tag;
    final HttpUrl url;

    public static class Builder {
        ArrayList<InetAddress> additionalInetAddresses;
        RequestBody body;
        boolean concurrentConnectEnabled;
        com.huawei.okhttp3.Headers.Builder headers;
        boolean http2Indicator;
        String method;
        Object tag;
        HttpUrl url;

        public Builder() {
            this.concurrentConnectEnabled = false;
            this.additionalInetAddresses = new ArrayList();
            this.method = "GET";
            this.headers = new com.huawei.okhttp3.Headers.Builder();
        }

        Builder(Request request) {
            this.concurrentConnectEnabled = false;
            this.additionalInetAddresses = new ArrayList();
            this.url = request.url;
            this.method = request.method;
            this.body = request.body;
            this.tag = request.tag;
            this.headers = request.headers.newBuilder();
            this.http2Indicator = request.http2Indicator;
            this.concurrentConnectEnabled = request.concurrentConnectEnabled;
            this.additionalInetAddresses = request.additionalInetAddresses;
        }

        public Builder url(HttpUrl url) {
            if (url != null) {
                this.url = url;
                return this;
            }
            throw new NullPointerException("url == null");
        }

        public Builder url(String url) {
            if (url != null) {
                StringBuilder stringBuilder;
                if (url.regionMatches(true, 0, "ws:", 0, 3)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("http:");
                    stringBuilder.append(url.substring(3));
                    url = stringBuilder.toString();
                } else {
                    if (url.regionMatches(true, 0, "wss:", 0, 4)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("https:");
                        stringBuilder.append(url.substring(4));
                        url = stringBuilder.toString();
                    }
                }
                HttpUrl parsed = HttpUrl.parse(url);
                if (parsed != null) {
                    return url(parsed);
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("unexpected url: ");
                stringBuilder2.append(url);
                throw new IllegalArgumentException(stringBuilder2.toString());
            }
            throw new NullPointerException("url == null");
        }

        public Builder url(URL url) {
            if (url != null) {
                HttpUrl parsed = HttpUrl.get(url);
                if (parsed != null) {
                    return url(parsed);
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected url: ");
                stringBuilder.append(url);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            throw new NullPointerException("url == null");
        }

        public Builder header(String name, String value) {
            this.headers.set(name, value);
            return this;
        }

        public Builder addHeader(String name, String value) {
            this.headers.add(name, value);
            return this;
        }

        public Builder removeHeader(String name) {
            this.headers.removeAll(name);
            return this;
        }

        public Builder headers(Headers headers) {
            this.headers = headers.newBuilder();
            return this;
        }

        public Builder cacheControl(CacheControl cacheControl) {
            String value = cacheControl.toString();
            if (value.isEmpty()) {
                return removeHeader("Cache-Control");
            }
            return header("Cache-Control", value);
        }

        public Builder get() {
            return method("GET", null);
        }

        public Builder head() {
            return method("HEAD", null);
        }

        public Builder post(RequestBody body) {
            return method("POST", body);
        }

        public Builder delete(RequestBody body) {
            return method("DELETE", body);
        }

        public Builder delete() {
            return delete(Util.EMPTY_REQUEST);
        }

        public Builder put(RequestBody body) {
            return method("PUT", body);
        }

        public Builder patch(RequestBody body) {
            return method("PATCH", body);
        }

        public Builder method(String method, RequestBody body) {
            StringBuilder stringBuilder;
            if (method == null) {
                throw new NullPointerException("method == null");
            } else if (method.length() == 0) {
                throw new IllegalArgumentException("method.length() == 0");
            } else if (body != null && !HttpMethod.permitsRequestBody(method)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("method ");
                stringBuilder.append(method);
                stringBuilder.append(" must not have a request body.");
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (body == null && HttpMethod.requiresRequestBody(method)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("method ");
                stringBuilder.append(method);
                stringBuilder.append(" must have a request body.");
                throw new IllegalArgumentException(stringBuilder.toString());
            } else {
                this.method = method;
                this.body = body;
                return this;
            }
        }

        public Builder tag(Object tag) {
            this.tag = tag;
            return this;
        }

        public Builder concurrentConnectEnabled(boolean concurrentConnectEnabled) {
            this.concurrentConnectEnabled = concurrentConnectEnabled;
            return this;
        }

        public Builder additionalIpAddresses(ArrayList<String> additionalIpAddresses) throws UnknownHostException {
            if (additionalIpAddresses != null) {
                Iterator it = additionalIpAddresses.iterator();
                while (it.hasNext()) {
                    addIpAddress((String) it.next());
                }
                return this;
            }
            throw new IllegalArgumentException("additionalIpAddresses is null");
        }

        public Builder addIpAddress(String ipAddress) throws UnknownHostException {
            if (ipAddress != null) {
                try {
                    for (InetAddress inetAddress : InetAddress.getAllByName(ipAddress)) {
                        this.additionalInetAddresses.add(inetAddress);
                    }
                    return this;
                } catch (NullPointerException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Broken system behaviour for dns lookup of ");
                    stringBuilder.append(ipAddress);
                    UnknownHostException unknownHostException = new UnknownHostException(stringBuilder.toString());
                    unknownHostException.initCause(e);
                    throw unknownHostException;
                }
            }
            throw new IllegalArgumentException("IP address is null");
        }

        public Request build() {
            if (this.url != null) {
                return new Request(this);
            }
            throw new IllegalStateException("url == null");
        }
    }

    Request(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers.build();
        this.body = builder.body;
        this.tag = builder.tag != null ? builder.tag : this;
        this.concurrentConnectEnabled = builder.concurrentConnectEnabled;
        this.additionalInetAddresses = builder.additionalInetAddresses;
        this.http2Indicator = builder.http2Indicator;
    }

    public HttpUrl url() {
        return this.url;
    }

    public String method() {
        return this.method;
    }

    public Headers headers() {
        return this.headers;
    }

    public String header(String name) {
        return this.headers.get(name);
    }

    public List<String> headers(String name) {
        return this.headers.values(name);
    }

    public RequestBody body() {
        return this.body;
    }

    public Object tag() {
        return this.tag;
    }

    public boolean concurrentConnectEnabled() {
        return this.concurrentConnectEnabled;
    }

    public ArrayList<InetAddress> additionalInetAddresses() {
        return this.additionalInetAddresses;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public CacheControl cacheControl() {
        CacheControl result = this.cacheControl;
        if (result != null) {
            return result;
        }
        CacheControl parse = CacheControl.parse(this.headers);
        this.cacheControl = parse;
        return parse;
    }

    public boolean isHttps() {
        return this.url.isHttps();
    }

    public boolean http2Indicator() {
        return this.http2Indicator;
    }

    public boolean setHttp2Indicator() {
        this.http2Indicator = true;
        return true;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Request{method=");
        stringBuilder.append(this.method);
        stringBuilder.append(", url=");
        stringBuilder.append(this.url);
        stringBuilder.append(", tag=");
        stringBuilder.append(this.tag != this ? this.tag : null);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
