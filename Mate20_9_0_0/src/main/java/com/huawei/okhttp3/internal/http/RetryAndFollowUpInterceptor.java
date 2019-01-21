package com.huawei.okhttp3.internal.http;

import com.huawei.motiondetection.MotionTypeApps;
import com.huawei.okhttp3.Address;
import com.huawei.okhttp3.CertificatePinner;
import com.huawei.okhttp3.Connection;
import com.huawei.okhttp3.HttpUrl;
import com.huawei.okhttp3.Interceptor;
import com.huawei.okhttp3.Interceptor.Chain;
import com.huawei.okhttp3.OkHttpClient;
import com.huawei.okhttp3.Request;
import com.huawei.okhttp3.Request.Builder;
import com.huawei.okhttp3.RequestBody;
import com.huawei.okhttp3.Response;
import com.huawei.okhttp3.Route;
import com.huawei.okhttp3.internal.Util;
import com.huawei.okhttp3.internal.connection.RouteException;
import com.huawei.okhttp3.internal.connection.StreamAllocation;
import com.huawei.okhttp3.internal.http2.ConnectionShutdownException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpRetryException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

public final class RetryAndFollowUpInterceptor implements Interceptor {
    private static final int MAX_FOLLOW_UPS = 20;
    private Object callStackTrace;
    private volatile boolean canceled;
    private final OkHttpClient client;
    private final boolean forWebSocket;
    private StreamAllocation streamAllocation;

    public RetryAndFollowUpInterceptor(OkHttpClient client, boolean forWebSocket) {
        this.client = client;
        this.forWebSocket = forWebSocket;
    }

    public void cancel() {
        this.canceled = true;
        StreamAllocation streamAllocation = this.streamAllocation;
        if (streamAllocation != null) {
            streamAllocation.cancel();
        }
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public void setCallStackTrace(Object callStackTrace) {
        this.callStackTrace = callStackTrace;
    }

    public StreamAllocation streamAllocation() {
        return this.streamAllocation;
    }

    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        this.streamAllocation = new StreamAllocation(this.client.connectionPool(), createAddress(request.url()), this.callStackTrace, request, this.client.connectionAttemptDelay());
        this.streamAllocation.address.setHeaderHost(request.header("host"));
        if (request.http2Indicator()) {
            this.streamAllocation.setHttp2Indicator();
        }
        int followUpCount = 0;
        Response priorResponse = null;
        Request request2 = request;
        while (!this.canceled) {
            boolean requestSendStarted = true;
            boolean releaseConnection = true;
            try {
                Response response = ((RealInterceptorChain) chain).proceed(request2, this.streamAllocation, null, null);
                if (false) {
                    this.streamAllocation.streamFailed(null);
                    this.streamAllocation.release();
                }
                if (priorResponse != null) {
                    response = response.newBuilder().priorResponse(priorResponse.newBuilder().body(null).build()).build();
                }
                Response response2 = response;
                Request followUp = followUpRequest(response2);
                if (followUp == null) {
                    if (!this.forWebSocket) {
                        this.streamAllocation.release();
                    }
                    return response2;
                }
                Util.closeQuietly(response2.body());
                int followUpCount2 = followUpCount + 1;
                StringBuilder stringBuilder;
                if (followUpCount2 > 20) {
                    this.streamAllocation.release();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Too many follow-up requests: ");
                    stringBuilder.append(followUpCount2);
                    throw new ProtocolException(stringBuilder.toString());
                } else if (followUp.body() instanceof UnrepeatableRequestBody) {
                    this.streamAllocation.release();
                    throw new HttpRetryException("Cannot retry streamed HTTP body", response2.code());
                } else {
                    if (!sameConnection(response2, followUp.url())) {
                        this.streamAllocation.release();
                        this.streamAllocation = new StreamAllocation(this.client.connectionPool(), createAddress(followUp.url()), this.callStackTrace, request2, this.client.connectionAttemptDelay());
                    } else if (this.streamAllocation.codec() != null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Closing the body of ");
                        stringBuilder.append(response2);
                        stringBuilder.append(" didn't close its backing stream. Bad interceptor?");
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                    request2 = followUp;
                    priorResponse = response2;
                    followUpCount = followUpCount2;
                }
            } catch (RouteException e) {
                if (recover(e.getLastConnectException(), false, request2)) {
                    if (!false) {
                    }
                    this.streamAllocation.streamFailed(null);
                    this.streamAllocation.release();
                } else {
                    throw e.getLastConnectException();
                }
            } catch (IOException e2) {
                if (e2 instanceof ConnectionShutdownException) {
                    requestSendStarted = false;
                }
                if (!recover(e2, requestSendStarted, request2)) {
                    throw e2;
                } else if (false) {
                    this.streamAllocation.streamFailed(null);
                    this.streamAllocation.release();
                }
            } catch (Throwable th) {
                if (releaseConnection) {
                    this.streamAllocation.streamFailed(null);
                    this.streamAllocation.release();
                }
            }
        }
        this.streamAllocation.release();
        throw new IOException("Canceled");
    }

    private Address createAddress(HttpUrl url) {
        SSLSocketFactory sslSocketFactory = null;
        HostnameVerifier hostnameVerifier = null;
        CertificatePinner certificatePinner = null;
        if (url.isHttps()) {
            sslSocketFactory = this.client.sslSocketFactory();
            hostnameVerifier = this.client.hostnameVerifier();
            certificatePinner = this.client.certificatePinner();
        }
        return new Address(url.host(), url.port(), this.client.dns(), this.client.socketFactory(), sslSocketFactory, hostnameVerifier, certificatePinner, this.client.proxyAuthenticator(), this.client.proxy(), this.client.protocols(), this.client.connectionSpecs(), this.client.proxySelector());
    }

    private boolean recover(IOException e, boolean requestSendStarted, Request userRequest) {
        this.streamAllocation.streamFailed(e);
        if (!this.client.retryOnConnectionFailure()) {
            return false;
        }
        if ((!requestSendStarted || !(userRequest.body() instanceof UnrepeatableRequestBody)) && isRecoverable(e, requestSendStarted) && this.streamAllocation.hasMoreRoutes()) {
            return true;
        }
        return false;
    }

    private boolean isRecoverable(IOException e, boolean requestSendStarted) {
        boolean z = false;
        if (e instanceof ProtocolException) {
            return false;
        }
        if (e instanceof InterruptedIOException) {
            if ((e instanceof SocketTimeoutException) && !requestSendStarted) {
                z = true;
            }
            return z;
        } else if (((e instanceof SSLHandshakeException) && (e.getCause() instanceof CertificateException)) || (e instanceof SSLPeerUnverifiedException)) {
            return false;
        } else {
            return true;
        }
    }

    private Request followUpRequest(Response userResponse) throws IOException {
        if (userResponse != null) {
            Route route;
            Connection connection = this.streamAllocation.connection();
            RequestBody requestBody = null;
            if (connection != null) {
                route = connection.route();
            } else {
                route = null;
            }
            int responseCode = userResponse.code();
            String method = userResponse.request().method();
            switch (responseCode) {
                case 300:
                case MotionTypeApps.TYPE_PROXIMITY_ANSWER /*301*/:
                case MotionTypeApps.TYPE_PROXIMITY_DIAL /*302*/:
                case MotionTypeApps.TYPE_PROXIMITY_SCREEN_OFF /*303*/:
                    break;
                case StatusLine.HTTP_TEMP_REDIRECT /*307*/:
                case StatusLine.HTTP_PERM_REDIRECT /*308*/:
                    if (!(method.equals("GET") || method.equals("HEAD"))) {
                        return null;
                    }
                case MotionTypeApps.TYPE_SHAKE_REFRESH /*401*/:
                    return this.client.authenticator().authenticate(route, userResponse);
                case 407:
                    Proxy selectedProxy;
                    if (route != null) {
                        selectedProxy = route.proxy();
                    } else {
                        selectedProxy = this.client.proxy();
                    }
                    if (selectedProxy.type() == Type.HTTP) {
                        return this.client.proxyAuthenticator().authenticate(route, userResponse);
                    }
                    throw new ProtocolException("Received HTTP_PROXY_AUTH (407) code while not using proxy");
                case 408:
                    if (userResponse.request().body() instanceof UnrepeatableRequestBody) {
                        return null;
                    }
                    return userResponse.request();
                default:
                    return null;
            }
            if (!this.client.followRedirects()) {
                return null;
            }
            String location = userResponse.header("Location");
            if (location == null) {
                return null;
            }
            HttpUrl url = userResponse.request().url().resolve(location);
            if (url == null) {
                return null;
            }
            if (!url.scheme().equals(userResponse.request().url().scheme()) && !this.client.followSslRedirects()) {
                return null;
            }
            Builder requestBuilder = userResponse.request().newBuilder();
            if (HttpMethod.permitsRequestBody(method)) {
                boolean maintainBody = HttpMethod.redirectsWithBody(method);
                if (HttpMethod.redirectsToGet(method)) {
                    requestBuilder.method("GET", null);
                } else {
                    if (maintainBody) {
                        requestBody = userResponse.request().body();
                    }
                    requestBuilder.method(method, requestBody);
                }
                if (!maintainBody) {
                    requestBuilder.removeHeader("Transfer-Encoding");
                    requestBuilder.removeHeader("Content-Length");
                    requestBuilder.removeHeader("Content-Type");
                }
            }
            if (!sameConnection(userResponse, url)) {
                requestBuilder.removeHeader("Authorization");
            }
            return requestBuilder.url(url).build();
        }
        throw new IllegalStateException();
    }

    private boolean sameConnection(Response response, HttpUrl followUp) {
        HttpUrl url = response.request().url();
        return url.host().equals(followUp.host()) && url.port() == followUp.port() && url.scheme().equals(followUp.scheme());
    }
}
