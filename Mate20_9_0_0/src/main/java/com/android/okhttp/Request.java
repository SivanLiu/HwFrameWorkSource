package com.android.okhttp;

import com.android.okhttp.internal.http.HttpMethod;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

public final class Request {
    private final RequestBody body;
    private volatile CacheControl cacheControl;
    private final Headers headers;
    private volatile URI javaNetUri;
    private volatile URL javaNetUrl;
    private final String method;
    private final Object tag;
    private final HttpUrl url;

    public static class Builder {
        private RequestBody body;
        private com.android.okhttp.Headers.Builder headers;
        private String method;
        private Object tag;
        private HttpUrl url;

        public Builder() {
            this.method = "GET";
            this.headers = new com.android.okhttp.Headers.Builder();
        }

        private Builder(Request request) {
            this.url = request.url;
            this.method = request.method;
            this.body = request.body;
            this.tag = request.tag;
            this.headers = request.headers.newBuilder();
        }

        public Builder url(HttpUrl url) {
            if (url != null) {
                this.url = url;
                return this;
            }
            throw new IllegalArgumentException("url == null");
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
            throw new IllegalArgumentException("url == null");
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
            throw new IllegalArgumentException("url == null");
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
            return delete(RequestBody.create(null, new byte[0]));
        }

        public Builder put(RequestBody body) {
            return method("PUT", body);
        }

        public Builder patch(RequestBody body) {
            return method("PATCH", body);
        }

        public Builder method(String method, RequestBody body) {
            StringBuilder stringBuilder;
            if (method == null || method.length() == 0) {
                throw new IllegalArgumentException("method == null || method.length() == 0");
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

        public Request build() {
            if (this.url != null) {
                return new Request(this);
            }
            throw new IllegalStateException("url == null");
        }
    }

    private Request(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers.build();
        this.body = builder.body;
        this.tag = builder.tag != null ? builder.tag : this;
    }

    public HttpUrl httpUrl() {
        return this.url;
    }

    public URL url() {
        URL result = this.javaNetUrl;
        if (result != null) {
            return result;
        }
        URL url = this.url.url();
        this.javaNetUrl = url;
        return url;
    }

    public URI uri() throws IOException {
        try {
            URI result = this.javaNetUri;
            if (result != null) {
                return result;
            }
            URI uri = this.url.uri();
            this.javaNetUri = uri;
            return uri;
        } catch (IllegalStateException e) {
            throw new IOException(e.getMessage());
        }
    }

    public String urlString() {
        return this.url.toString();
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

    public Builder newBuilder() {
        return new Builder();
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
