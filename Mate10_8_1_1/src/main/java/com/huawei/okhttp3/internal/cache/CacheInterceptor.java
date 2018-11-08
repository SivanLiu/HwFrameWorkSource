package com.huawei.okhttp3.internal.cache;

import com.huawei.android.os.HwTransCodeEx;
import com.huawei.motiondetection.MotionTypeApps;
import com.huawei.okhttp3.Headers;
import com.huawei.okhttp3.Interceptor;
import com.huawei.okhttp3.Interceptor.Chain;
import com.huawei.okhttp3.Protocol;
import com.huawei.okhttp3.Request;
import com.huawei.okhttp3.Response;
import com.huawei.okhttp3.Response.Builder;
import com.huawei.okhttp3.internal.Internal;
import com.huawei.okhttp3.internal.Util;
import com.huawei.okhttp3.internal.cache.CacheStrategy.Factory;
import com.huawei.okhttp3.internal.http.HttpHeaders;
import com.huawei.okhttp3.internal.http.HttpMethod;
import com.huawei.okhttp3.internal.http.RealResponseBody;
import com.huawei.okio.Buffer;
import com.huawei.okio.BufferedSink;
import com.huawei.okio.BufferedSource;
import com.huawei.okio.Okio;
import com.huawei.okio.Sink;
import com.huawei.okio.Source;
import com.huawei.okio.Timeout;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class CacheInterceptor implements Interceptor {
    final InternalCache cache;

    public CacheInterceptor(InternalCache cache) {
        this.cache = cache;
    }

    public Response intercept(Chain chain) throws IOException {
        Response response;
        if (this.cache != null) {
            response = this.cache.get(chain.request());
        } else {
            response = null;
        }
        CacheStrategy strategy = new Factory(System.currentTimeMillis(), chain.request(), response).get();
        Request networkRequest = strategy.networkRequest;
        Response cacheResponse = strategy.cacheResponse;
        if (this.cache != null) {
            this.cache.trackResponse(strategy);
        }
        if (response != null && cacheResponse == null) {
            Util.closeQuietly(response.body());
        }
        if (networkRequest == null && cacheResponse == null) {
            return new Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(HwTransCodeEx.GET_PACKAGE_NAME_FOR_PID_TRANSACTION).message("Unsatisfiable Request (only-if-cached)").body(Util.EMPTY_RESPONSE).sentRequestAtMillis(-1).receivedResponseAtMillis(System.currentTimeMillis()).build();
        }
        if (networkRequest == null) {
            return cacheResponse.newBuilder().cacheResponse(stripBody(cacheResponse)).build();
        }
        Response response2 = null;
        try {
            Response response3;
            response2 = chain.proceed(networkRequest);
            if (response2 == null) {
            }
            if (cacheResponse != null) {
                if (response2.code() == MotionTypeApps.TYPE_PROXIMITY_SPEAKER) {
                    response3 = cacheResponse.newBuilder().headers(combine(cacheResponse.headers(), response2.headers())).sentRequestAtMillis(response2.sentRequestAtMillis()).receivedResponseAtMillis(response2.receivedResponseAtMillis()).cacheResponse(stripBody(cacheResponse)).networkResponse(stripBody(response2)).build();
                    response2.body().close();
                    this.cache.trackConditionalCacheHit();
                    this.cache.update(cacheResponse, response3);
                    return response3;
                }
                Util.closeQuietly(cacheResponse.body());
            }
            response3 = response2.newBuilder().cacheResponse(stripBody(cacheResponse)).networkResponse(stripBody(response2)).build();
            if (HttpHeaders.hasBody(response3)) {
                response3 = cacheWritingResponse(maybeCache(response3, response2.request(), this.cache), response3);
            }
            return response3;
        } finally {
            if (response != null) {
                Util.closeQuietly(response.body());
            }
        }
    }

    private static Response stripBody(Response response) {
        if (response == null || response.body() == null) {
            return response;
        }
        return response.newBuilder().body(null).build();
    }

    private CacheRequest maybeCache(Response userResponse, Request networkRequest, InternalCache responseCache) throws IOException {
        if (responseCache == null) {
            return null;
        }
        if (CacheStrategy.isCacheable(userResponse, networkRequest)) {
            return responseCache.put(userResponse);
        }
        if (HttpMethod.invalidatesCache(networkRequest.method())) {
            try {
                responseCache.remove(networkRequest);
            } catch (IOException e) {
            }
        }
        return null;
    }

    private Response cacheWritingResponse(final CacheRequest cacheRequest, Response response) throws IOException {
        if (cacheRequest == null) {
            return response;
        }
        Sink cacheBodyUnbuffered = cacheRequest.body();
        if (cacheBodyUnbuffered == null) {
            return response;
        }
        final BufferedSource source = response.body().source();
        final BufferedSink cacheBody = Okio.buffer(cacheBodyUnbuffered);
        return response.newBuilder().body(new RealResponseBody(response.headers(), Okio.buffer(new Source() {
            boolean cacheRequestClosed;

            public long read(Buffer sink, long byteCount) throws IOException {
                try {
                    long bytesRead = source.read(sink, byteCount);
                    if (bytesRead == -1) {
                        if (!this.cacheRequestClosed) {
                            this.cacheRequestClosed = true;
                            cacheBody.close();
                        }
                        return -1;
                    }
                    sink.copyTo(cacheBody.buffer(), sink.size() - bytesRead, bytesRead);
                    cacheBody.emitCompleteSegments();
                    return bytesRead;
                } catch (IOException e) {
                    if (!this.cacheRequestClosed) {
                        this.cacheRequestClosed = true;
                        cacheRequest.abort();
                    }
                    throw e;
                }
            }

            public Timeout timeout() {
                return source.timeout();
            }

            public void close() throws IOException {
                if (!(this.cacheRequestClosed || (Util.discard(this, 100, TimeUnit.MILLISECONDS) ^ 1) == 0)) {
                    this.cacheRequestClosed = true;
                    cacheRequest.abort();
                }
                source.close();
            }
        }))).build();
    }

    private static Headers combine(Headers cachedHeaders, Headers networkHeaders) {
        int i;
        Headers.Builder result = new Headers.Builder();
        int size = cachedHeaders.size();
        for (i = 0; i < size; i++) {
            String fieldName = cachedHeaders.name(i);
            String value = cachedHeaders.value(i);
            if (!("Warning".equalsIgnoreCase(fieldName) && value.startsWith("1")) && (!isEndToEnd(fieldName) || networkHeaders.get(fieldName) == null)) {
                Internal.instance.addLenient(result, fieldName, value);
            }
        }
        size = networkHeaders.size();
        for (i = 0; i < size; i++) {
            fieldName = networkHeaders.name(i);
            if (!"Content-Length".equalsIgnoreCase(fieldName) && isEndToEnd(fieldName)) {
                Internal.instance.addLenient(result, fieldName, networkHeaders.value(i));
            }
        }
        return result.build();
    }

    static boolean isEndToEnd(String fieldName) {
        return ("Connection".equalsIgnoreCase(fieldName) || ("Keep-Alive".equalsIgnoreCase(fieldName) ^ 1) == 0 || ("Proxy-Authenticate".equalsIgnoreCase(fieldName) ^ 1) == 0 || ("Proxy-Authorization".equalsIgnoreCase(fieldName) ^ 1) == 0 || ("TE".equalsIgnoreCase(fieldName) ^ 1) == 0 || ("Trailers".equalsIgnoreCase(fieldName) ^ 1) == 0 || ("Transfer-Encoding".equalsIgnoreCase(fieldName) ^ 1) == 0) ? false : "Upgrade".equalsIgnoreCase(fieldName) ^ 1;
    }
}
