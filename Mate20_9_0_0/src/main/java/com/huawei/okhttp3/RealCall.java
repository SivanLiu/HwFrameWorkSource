package com.huawei.okhttp3;

import com.huawei.okhttp3.internal.NamedRunnable;
import com.huawei.okhttp3.internal.cache.CacheInterceptor;
import com.huawei.okhttp3.internal.connection.ConnectInterceptor;
import com.huawei.okhttp3.internal.connection.Http2ConnectionInterceptor;
import com.huawei.okhttp3.internal.connection.StreamAllocation;
import com.huawei.okhttp3.internal.http.BridgeInterceptor;
import com.huawei.okhttp3.internal.http.CallServerInterceptor;
import com.huawei.okhttp3.internal.http.RealInterceptorChain;
import com.huawei.okhttp3.internal.http.RetryAndFollowUpInterceptor;
import com.huawei.okhttp3.internal.platform.Platform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class RealCall implements Call {
    final OkHttpClient client;
    private boolean executed;
    final boolean forWebSocket;
    final Request originalRequest;
    final RetryAndFollowUpInterceptor retryAndFollowUpInterceptor;

    final class AsyncCall extends NamedRunnable {
        private boolean forCreateConnectionOnly = false;
        private final Callback responseCallback;

        AsyncCall(Callback responseCallback) {
            super("OkHttp %s", this$0.redactedUrl());
            this.responseCallback = responseCallback;
        }

        String host() {
            return RealCall.this.originalRequest.url().host();
        }

        Request request() {
            return RealCall.this.originalRequest;
        }

        RealCall get() {
            return RealCall.this;
        }

        OkHttpClient client() {
            return RealCall.this.client;
        }

        boolean forCreateConnectionOnly() {
            return this.forCreateConnectionOnly;
        }

        public void setForCreateConnectionOnly() {
            this.forCreateConnectionOnly = true;
        }

        protected void execute() {
            Platform platform;
            StringBuilder stringBuilder;
            try {
                Response response;
                if (this.forCreateConnectionOnly) {
                    response = RealCall.this.getResponseForCreateConnectionOnly();
                } else {
                    response = RealCall.this.getResponseWithInterceptorChain();
                }
                if (RealCall.this.retryAndFollowUpInterceptor.isCanceled()) {
                    this.responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
                } else {
                    this.responseCallback.onResponse(RealCall.this, response);
                }
            } catch (IOException e) {
                if (null != null) {
                    platform = Platform.get();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Callback failure for ");
                    stringBuilder.append(RealCall.this.toLoggableString());
                    platform.log(4, stringBuilder.toString(), e);
                } else {
                    this.responseCallback.onFailure(RealCall.this, e);
                }
            } catch (Exception e2) {
                if (null != null) {
                    platform = Platform.get();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Callback failure for ");
                    stringBuilder.append(RealCall.this.toLoggableString());
                    platform.log(4, stringBuilder.toString(), e2);
                } else {
                    this.responseCallback.onFailure(RealCall.this, new IOException(e2));
                }
            } catch (Throwable th) {
                RealCall.this.client.dispatcher().finished(this);
            }
            RealCall.this.client.dispatcher().finished(this);
        }
    }

    RealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
        this.client = client;
        this.originalRequest = originalRequest;
        this.forWebSocket = forWebSocket;
        this.retryAndFollowUpInterceptor = new RetryAndFollowUpInterceptor(client, forWebSocket);
    }

    public Request request() {
        return this.originalRequest;
    }

    public Response execute() throws IOException {
        synchronized (this) {
            if (this.executed) {
                throw new IllegalStateException("Already Executed");
            }
            this.executed = true;
        }
        captureCallStackTrace();
        try {
            this.client.dispatcher().executed(this);
            Response result = getResponseWithInterceptorChain();
            if (result != null) {
                this.client.dispatcher().finished(this);
                return result;
            }
            throw new IOException("Canceled");
        } catch (Exception e) {
            throw new IOException(e);
        } catch (Throwable th) {
            this.client.dispatcher().finished(this);
        }
    }

    private void captureCallStackTrace() {
        this.retryAndFollowUpInterceptor.setCallStackTrace(Platform.get().getStackTraceForCloseable("response.body().close()"));
    }

    public void enqueue(Callback responseCallback) {
        synchronized (this) {
            if (this.executed) {
                throw new IllegalStateException("Already Executed");
            }
            this.executed = true;
        }
        captureCallStackTrace();
        this.client.dispatcher().enqueue(new AsyncCall(responseCallback));
    }

    public void cancel() {
        this.retryAndFollowUpInterceptor.cancel();
    }

    public synchronized boolean isExecuted() {
        return this.executed;
    }

    public boolean isCanceled() {
        return this.retryAndFollowUpInterceptor.isCanceled();
    }

    public RealCall clone() {
        return new RealCall(this.client, this.originalRequest, this.forWebSocket);
    }

    StreamAllocation streamAllocation() {
        return this.retryAndFollowUpInterceptor.streamAllocation();
    }

    String toLoggableString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(isCanceled() ? "canceled " : "");
        stringBuilder.append(this.forWebSocket ? "web socket" : "call");
        stringBuilder.append(" to ");
        stringBuilder.append(redactedUrl());
        return stringBuilder.toString();
    }

    String redactedUrl() {
        return this.originalRequest.url().redact();
    }

    Response getResponseWithInterceptorChain() throws IOException {
        List<Interceptor> interceptors = new ArrayList();
        interceptors.addAll(this.client.interceptors());
        interceptors.add(this.retryAndFollowUpInterceptor);
        interceptors.add(new BridgeInterceptor(this.client.cookieJar()));
        interceptors.add(new CacheInterceptor(this.client.internalCache()));
        interceptors.add(new ConnectInterceptor(this.client));
        if (!this.forWebSocket) {
            interceptors.addAll(this.client.networkInterceptors());
        }
        interceptors.add(new CallServerInterceptor(this.forWebSocket));
        return new RealInterceptorChain(interceptors, null, null, null, 0, this.originalRequest).proceed(this.originalRequest);
    }

    Response getResponseForCreateConnectionOnly() throws IOException {
        List<Interceptor> interceptors = new ArrayList();
        interceptors.add(this.retryAndFollowUpInterceptor);
        interceptors.add(new Http2ConnectionInterceptor(this.client));
        return new RealInterceptorChain(interceptors, null, null, null, 0, this.originalRequest).proceed(this.originalRequest);
    }
}
