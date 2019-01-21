package com.android.okhttp;

import com.android.okhttp.Interceptor.Chain;
import com.android.okhttp.internal.Internal;
import com.android.okhttp.internal.NamedRunnable;
import com.android.okhttp.internal.http.HttpEngine;
import com.android.okhttp.internal.http.RequestException;
import com.android.okhttp.internal.http.RouteException;
import com.android.okhttp.internal.http.StreamAllocation;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Call {
    volatile boolean canceled;
    private final OkHttpClient client;
    HttpEngine engine;
    private boolean executed;
    Request originalRequest;

    class ApplicationInterceptorChain implements Chain {
        private final boolean forWebSocket;
        private final int index;
        private final Request request;

        ApplicationInterceptorChain(int index, Request request, boolean forWebSocket) {
            this.index = index;
            this.request = request;
            this.forWebSocket = forWebSocket;
        }

        public Connection connection() {
            return null;
        }

        public Request request() {
            return this.request;
        }

        public Response proceed(Request request) throws IOException {
            if (this.index >= Call.this.client.interceptors().size()) {
                return Call.this.getResponse(request, this.forWebSocket);
            }
            Interceptor interceptor = (Interceptor) Call.this.client.interceptors().get(this.index);
            Response interceptedResponse = interceptor.intercept(new ApplicationInterceptorChain(this.index + 1, request, this.forWebSocket));
            if (interceptedResponse != null) {
                return interceptedResponse;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("application interceptor ");
            stringBuilder.append(interceptor);
            stringBuilder.append(" returned null");
            throw new NullPointerException(stringBuilder.toString());
        }
    }

    final class AsyncCall extends NamedRunnable {
        private final boolean forWebSocket;
        private final Callback responseCallback;

        private AsyncCall(Callback responseCallback, boolean forWebSocket) {
            super("OkHttp %s", this$0.originalRequest.urlString());
            this.responseCallback = responseCallback;
            this.forWebSocket = forWebSocket;
        }

        String host() {
            return Call.this.originalRequest.httpUrl().host();
        }

        Request request() {
            return Call.this.originalRequest;
        }

        Object tag() {
            return Call.this.originalRequest.tag();
        }

        void cancel() {
            Call.this.cancel();
        }

        Call get() {
            return Call.this;
        }

        protected void execute() {
            try {
                Response response = Call.this.getResponseWithInterceptorChain(this.forWebSocket);
                if (Call.this.canceled) {
                    this.responseCallback.onFailure(Call.this.originalRequest, new IOException("Canceled"));
                } else {
                    this.responseCallback.onResponse(response);
                }
            } catch (IOException e) {
                if (false) {
                    Logger logger = Internal.logger;
                    Level level = Level.INFO;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Callback failure for ");
                    stringBuilder.append(Call.this.toLoggableString());
                    logger.log(level, stringBuilder.toString(), e);
                } else {
                    this.responseCallback.onFailure(Call.this.engine == null ? Call.this.originalRequest : Call.this.engine.getRequest(), e);
                }
            } catch (Throwable th) {
                Call.this.client.getDispatcher().finished(this);
            }
            Call.this.client.getDispatcher().finished(this);
        }
    }

    protected Call(OkHttpClient client, Request originalRequest) {
        this.client = client.copyWithDefaults();
        this.originalRequest = originalRequest;
    }

    public Response execute() throws IOException {
        synchronized (this) {
            if (this.executed) {
                throw new IllegalStateException("Already Executed");
            }
            this.executed = true;
        }
        try {
            this.client.getDispatcher().executed(this);
            Response result = getResponseWithInterceptorChain(null);
            if (result != null) {
                return result;
            }
            throw new IOException("Canceled");
        } finally {
            this.client.getDispatcher().finished(this);
        }
    }

    Object tag() {
        return this.originalRequest.tag();
    }

    public void enqueue(Callback responseCallback) {
        enqueue(responseCallback, false);
    }

    void enqueue(Callback responseCallback, boolean forWebSocket) {
        synchronized (this) {
            if (this.executed) {
                throw new IllegalStateException("Already Executed");
            }
            this.executed = true;
        }
        this.client.getDispatcher().enqueue(new AsyncCall(responseCallback, forWebSocket));
    }

    public void cancel() {
        this.canceled = true;
        if (this.engine != null) {
            this.engine.cancel();
        }
    }

    public synchronized boolean isExecuted() {
        return this.executed;
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    private String toLoggableString() {
        String string = this.canceled ? "canceled call" : "call";
        HttpUrl redactedUrl = this.originalRequest.httpUrl().resolve("/...");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(string);
        stringBuilder.append(" to ");
        stringBuilder.append(redactedUrl);
        return stringBuilder.toString();
    }

    private Response getResponseWithInterceptorChain(boolean forWebSocket) throws IOException {
        return new ApplicationInterceptorChain(0, this.originalRequest, forWebSocket).proceed(this.originalRequest);
    }

    Response getResponse(Request request, boolean forWebSocket) throws IOException {
        Request request2;
        HttpEngine retryEngine;
        RequestBody body = request.body();
        if (body != null) {
            request2 = request.newBuilder();
            MediaType contentType = body.contentType();
            if (contentType != null) {
                request2.header("Content-Type", contentType.toString());
            }
            long contentLength = body.contentLength();
            if (contentLength != -1) {
                request2.header("Content-Length", Long.toString(contentLength));
                request2.removeHeader("Transfer-Encoding");
            } else {
                request2.header("Transfer-Encoding", "chunked");
                request2.removeHeader("Content-Length");
            }
            request2 = request2.build();
        } else {
            request2 = request;
        }
        this.engine = new HttpEngine(this.client, request2, false, false, forWebSocket, null, null, null);
        int followUpCount = 0;
        while (!this.canceled) {
            boolean releaseConnection = true;
            try {
                this.engine.sendRequest();
                this.engine.readResponse();
                if (false) {
                    this.engine.close().release();
                }
                releaseConnection = this.engine.getResponse();
                Request followUp = this.engine.followUpRequest();
                if (followUp == null) {
                    if (!forWebSocket) {
                        this.engine.releaseStreamAllocation();
                    }
                    return releaseConnection;
                }
                StreamAllocation streamAllocation = this.engine.close();
                followUpCount++;
                if (followUpCount <= 20) {
                    if (!this.engine.sameConnection(followUp.httpUrl())) {
                        streamAllocation.release();
                        streamAllocation = null;
                    }
                    Request request3 = followUp;
                    this.engine = new HttpEngine(this.client, request3, false, false, forWebSocket, streamAllocation, null, releaseConnection);
                    Request request4 = request3;
                } else {
                    streamAllocation.release();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Too many follow-up requests: ");
                    stringBuilder.append(followUpCount);
                    throw new ProtocolException(stringBuilder.toString());
                }
            } catch (RequestException e) {
                throw e.getCause();
            } catch (RouteException e2) {
                retryEngine = this.engine.recover(e2);
                if (retryEngine != null) {
                    this.engine = retryEngine;
                    if (!false) {
                    }
                    this.engine.close().release();
                } else {
                    throw e2.getLastConnectException();
                }
            } catch (IOException e3) {
                retryEngine = this.engine.recover(e3, null);
                if (retryEngine != null) {
                    this.engine = retryEngine;
                    if (false) {
                        this.engine.close().release();
                    }
                } else {
                    throw e3;
                }
            } catch (Throwable th) {
                if (releaseConnection) {
                    this.engine.close().release();
                }
            }
        }
        this.engine.releaseStreamAllocation();
        throw new IOException("Canceled");
    }
}
