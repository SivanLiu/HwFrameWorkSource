package com.huawei.okhttp3.internal.http;

import com.huawei.okhttp3.Connection;
import com.huawei.okhttp3.HttpUrl;
import com.huawei.okhttp3.Interceptor;
import com.huawei.okhttp3.Interceptor.Chain;
import com.huawei.okhttp3.Request;
import com.huawei.okhttp3.Response;
import com.huawei.okhttp3.internal.connection.StreamAllocation;
import java.io.IOException;
import java.util.List;

public final class RealInterceptorChain implements Chain {
    private int calls;
    private final Connection connection;
    private final HttpCodec httpCodec;
    private final int index;
    private final List<Interceptor> interceptors;
    private final Request request;
    private final StreamAllocation streamAllocation;

    public RealInterceptorChain(List<Interceptor> interceptors, StreamAllocation streamAllocation, HttpCodec httpCodec, Connection connection, int index, Request request) {
        this.interceptors = interceptors;
        this.connection = connection;
        this.streamAllocation = streamAllocation;
        this.httpCodec = httpCodec;
        this.index = index;
        this.request = request;
    }

    public Connection connection() {
        return this.connection;
    }

    public StreamAllocation streamAllocation() {
        return this.streamAllocation;
    }

    public HttpCodec httpStream() {
        return this.httpCodec;
    }

    public Request request() {
        return this.request;
    }

    public Response proceed(Request request) throws IOException {
        return proceed(request, this.streamAllocation, this.httpCodec, this.connection);
    }

    public Response proceed(Request request, StreamAllocation streamAllocation, HttpCodec httpCodec, Connection connection) throws IOException {
        if (this.index < this.interceptors.size()) {
            this.calls++;
            StringBuilder stringBuilder;
            if (this.httpCodec != null && !sameConnection(request.url())) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("network interceptor ");
                stringBuilder.append(this.interceptors.get(this.index - 1));
                stringBuilder.append(" must retain the same host and port");
                throw new IllegalStateException(stringBuilder.toString());
            } else if (this.httpCodec == null || this.calls <= 1) {
                RealInterceptorChain realInterceptorChain = new RealInterceptorChain(this.interceptors, streamAllocation, httpCodec, connection, this.index + 1, request);
                Interceptor interceptor = (Interceptor) this.interceptors.get(this.index);
                Response response = interceptor.intercept(realInterceptorChain);
                StringBuilder stringBuilder2;
                if (httpCodec != null && this.index + 1 < this.interceptors.size() && realInterceptorChain.calls != 1) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("network interceptor ");
                    stringBuilder2.append(interceptor);
                    stringBuilder2.append(" must call proceed() exactly once");
                    throw new IllegalStateException(stringBuilder2.toString());
                } else if (response != null) {
                    return response;
                } else {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("interceptor ");
                    stringBuilder2.append(interceptor);
                    stringBuilder2.append(" returned null");
                    throw new NullPointerException(stringBuilder2.toString());
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("network interceptor ");
                stringBuilder.append(this.interceptors.get(this.index - 1));
                stringBuilder.append(" must call proceed() exactly once");
                throw new IllegalStateException(stringBuilder.toString());
            }
        }
        throw new AssertionError();
    }

    private boolean sameConnection(HttpUrl url) {
        return url.host().equals(this.connection.route().address().url().host()) && url.port() == this.connection.route().address().url().port();
    }
}
