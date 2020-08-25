package android.net.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.RequestContent;

public class Request {
    private static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";
    private static final String CONTENT_LENGTH_HEADER = "content-length";
    private static final String HOST_HEADER = "Host";
    private static RequestContent requestContentProcessor = new RequestContent();
    private int mBodyLength;
    private InputStream mBodyProvider;
    volatile boolean mCancelled = false;
    private final Object mClientResource = new Object();
    private Connection mConnection;
    EventHandler mEventHandler;
    int mFailCount = 0;
    HttpHost mHost;
    BasicHttpRequest mHttpRequest;
    private boolean mLoadingPaused = false;
    String mPath;
    HttpHost mProxyHost;
    private int mReceivedBytes = 0;

    public Request(String method, HttpHost host, HttpHost proxyHost, String path, InputStream bodyProvider, int bodyLength, EventHandler eventHandler, Map<String, String> headers) {
        this.mEventHandler = eventHandler;
        this.mHost = host;
        this.mProxyHost = proxyHost;
        this.mPath = path;
        this.mBodyProvider = bodyProvider;
        this.mBodyLength = bodyLength;
        if (bodyProvider != null || HttpPost.METHOD_NAME.equalsIgnoreCase(method)) {
            this.mHttpRequest = new BasicHttpEntityEnclosingRequest(method, getUri());
            if (bodyProvider != null) {
                setBodyProvider(bodyProvider, bodyLength);
            }
        } else {
            this.mHttpRequest = new BasicHttpRequest(method, getUri());
        }
        addHeader("Host", getHostPort());
        addHeader(ACCEPT_ENCODING_HEADER, "gzip");
        addHeaders(headers);
    }

    /* access modifiers changed from: package-private */
    public synchronized void setLoadingPaused(boolean pause) {
        this.mLoadingPaused = pause;
        if (!this.mLoadingPaused) {
            notify();
        }
    }

    /* access modifiers changed from: package-private */
    public void setConnection(Connection connection) {
        this.mConnection = connection;
    }

    /* access modifiers changed from: package-private */
    public EventHandler getEventHandler() {
        return this.mEventHandler;
    }

    /* access modifiers changed from: package-private */
    public void addHeader(String name, String value) {
        if (name == null) {
            HttpLog.e("Null http header name");
            throw new NullPointerException("Null http header name");
        } else if (value == null || value.length() == 0) {
            String damage = "Null or empty value for header \"" + name + "\"";
            HttpLog.e(damage);
            throw new RuntimeException(damage);
        } else {
            this.mHttpRequest.addHeader(name, value);
        }
    }

    /* access modifiers changed from: package-private */
    public void addHeaders(Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void sendRequest(AndroidHttpClientConnection httpClientConnection) throws HttpException, IOException {
        if (!this.mCancelled) {
            requestContentProcessor.process(this.mHttpRequest, this.mConnection.getHttpContext());
            httpClientConnection.sendRequestHeader(this.mHttpRequest);
            BasicHttpRequest basicHttpRequest = this.mHttpRequest;
            if (basicHttpRequest instanceof HttpEntityEnclosingRequest) {
                httpClientConnection.sendRequestEntity((HttpEntityEnclosingRequest) basicHttpRequest);
            }
        }
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:101:0x0159, code lost:
        if (r13 == null) goto L_0x0163;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:102:0x015b, code lost:
        r13.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:?, code lost:
        r15 = r13.read(r14, r2, r14.length - r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00db, code lost:
        if (r15 == -1) goto L_0x00e5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:0x00dd, code lost:
        r2 = r2 + r15;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x00de, code lost:
        if (r10 == false) goto L_0x00e5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x00e0, code lost:
        r19.mReceivedBytes += r15;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x00e6, code lost:
        if (r15 == -1) goto L_0x00f0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x00e8, code lost:
        if (r2 < r7) goto L_0x00eb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:56:0x00eb, code lost:
        r3 = r3;
        r8 = r8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:57:0x00f0, code lost:
        r19.mEventHandler.data(r14, r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:0x00f5, code lost:
        r2 = 0;
        r3 = r3;
        r8 = r8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:63:0x0100, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x0104, code lost:
        r0 = e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:66:0x0105, code lost:
        r15 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x0108, code lost:
        r15 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:87:0x0137, code lost:
        if (r13 != null) goto L_0x015b;
     */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x00a0 A[SYNTHETIC, Splitter:B:31:0x00a0] */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x011f  */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0132  */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x014c A[Catch:{ EOFException -> 0x0145, IOException -> 0x0129, all -> 0x0123, all -> 0x0152 }] */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x0155  */
    public void readResponse(AndroidHttpClientConnection httpClientConnection) throws IOException, ParseException {
        StatusLine statusLine;
        int statusCode;
        HttpEntity entity;
        int len;
        AndroidHttpClientConnection androidHttpClientConnection = httpClientConnection;
        if (!this.mCancelled) {
            httpClientConnection.flush();
            Headers header = new Headers();
            while (true) {
                statusLine = androidHttpClientConnection.parseResponseHeader(header);
                statusCode = statusLine.getStatusCode();
                if (statusCode >= 200) {
                    break;
                }
                androidHttpClientConnection = httpClientConnection;
            }
            ProtocolVersion v = statusLine.getProtocolVersion();
            this.mEventHandler.status(v.getMajor(), v.getMinor(), statusCode, statusLine.getReasonPhrase());
            this.mEventHandler.headers(header);
            boolean hasBody = canResponseHaveBody(this.mHttpRequest, statusCode);
            if (hasBody) {
                entity = androidHttpClientConnection.receiveResponseEntity(header);
            } else {
                entity = null;
            }
            boolean supportPartialContent = "bytes".equalsIgnoreCase(header.getAcceptRanges());
            if (entity != null) {
                InputStream is = entity.getContent();
                Header contentEncoding = entity.getContentEncoding();
                InputStream nis = null;
                byte[] buf = null;
                int count = 0;
                if (contentEncoding != null) {
                    try {
                        if (contentEncoding.getValue().equals("gzip")) {
                            nis = new GZIPInputStream(is);
                            buf = this.mConnection.getBuf();
                            int lowWater = buf.length / 2;
                            int count2 = 0;
                            len = 0;
                            while (len != -1) {
                                try {
                                    synchronized (this) {
                                        while (this.mLoadingPaused) {
                                            try {
                                                try {
                                                    wait();
                                                } catch (InterruptedException e) {
                                                    StringBuilder sb = new StringBuilder();
                                                    sb.append("Interrupted exception whilst network thread paused at WebCore's request. ");
                                                    sb.append(e.getMessage());
                                                    HttpLog.e(sb.toString());
                                                    v = v;
                                                } catch (Throwable th) {
                                                    th = th;
                                                    throw th;
                                                }
                                            } catch (Throwable th2) {
                                                th = th2;
                                                throw th;
                                            }
                                        }
                                    }
                                } catch (EOFException e2) {
                                    count = count2;
                                    if (count > 0) {
                                    }
                                } catch (IOException e3) {
                                    e = e3;
                                    count = count2;
                                    if (statusCode != 200) {
                                    }
                                    this.mEventHandler.data(buf, count);
                                    throw e;
                                } catch (Throwable th3) {
                                    e = th3;
                                    if (nis != null) {
                                    }
                                    throw e;
                                }
                            }
                            if (nis != null) {
                                nis.close();
                            }
                        }
                    } catch (EOFException e4) {
                        if (count > 0) {
                        }
                    } catch (IOException e5) {
                        e = e5;
                        if (statusCode != 200) {
                        }
                        this.mEventHandler.data(buf, count);
                        throw e;
                    } catch (Throwable th4) {
                        e = th4;
                        if (nis != null) {
                        }
                        throw e;
                    }
                }
                nis = is;
                try {
                    buf = this.mConnection.getBuf();
                    int lowWater2 = buf.length / 2;
                    int count22 = 0;
                    len = 0;
                    while (len != -1) {
                    }
                    if (nis != null) {
                    }
                } catch (EOFException e6) {
                    if (count > 0) {
                        this.mEventHandler.data(buf, count);
                    }
                } catch (IOException e7) {
                    e = e7;
                    if (statusCode != 200 || statusCode == 206) {
                        if (supportPartialContent && count > 0) {
                            this.mEventHandler.data(buf, count);
                        }
                        throw e;
                    }
                } catch (Throwable th5) {
                    e = th5;
                    if (nis != null) {
                    }
                    throw e;
                }
            }
            this.mConnection.setCanPersist(entity, statusLine.getProtocolVersion(), header.getConnectionType());
            this.mEventHandler.endData();
            complete();
        }
    }

    /* access modifiers changed from: package-private */
    public synchronized void cancel() {
        this.mLoadingPaused = false;
        notify();
        this.mCancelled = true;
        if (this.mConnection != null) {
            this.mConnection.cancel();
        }
    }

    /* access modifiers changed from: package-private */
    public String getHostPort() {
        String myScheme = this.mHost.getSchemeName();
        int myPort = this.mHost.getPort();
        if ((myPort == 80 || !myScheme.equals(HttpHost.DEFAULT_SCHEME_NAME)) && (myPort == 443 || !myScheme.equals("https"))) {
            return this.mHost.getHostName();
        }
        return this.mHost.toHostString();
    }

    /* access modifiers changed from: package-private */
    public String getUri() {
        if (this.mProxyHost == null || this.mHost.getSchemeName().equals("https")) {
            return this.mPath;
        }
        return this.mHost.getSchemeName() + "://" + getHostPort() + this.mPath;
    }

    public String toString() {
        return this.mPath;
    }

    /* access modifiers changed from: package-private */
    public void reset() {
        this.mHttpRequest.removeHeaders("content-length");
        InputStream inputStream = this.mBodyProvider;
        if (inputStream != null) {
            try {
                inputStream.reset();
            } catch (IOException e) {
            }
            setBodyProvider(this.mBodyProvider, this.mBodyLength);
        }
        if (this.mReceivedBytes > 0) {
            this.mFailCount = 0;
            HttpLog.v("*** Request.reset() to range:" + this.mReceivedBytes);
            BasicHttpRequest basicHttpRequest = this.mHttpRequest;
            basicHttpRequest.setHeader("Range", "bytes=" + this.mReceivedBytes + "-");
        }
    }

    /* access modifiers changed from: package-private */
    public void waitUntilComplete() {
        synchronized (this.mClientResource) {
            try {
                this.mClientResource.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void complete() {
        synchronized (this.mClientResource) {
            this.mClientResource.notifyAll();
        }
    }

    private static boolean canResponseHaveBody(HttpRequest request, int status) {
        if (!HttpHead.METHOD_NAME.equalsIgnoreCase(request.getRequestLine().getMethod()) && status >= 200 && status != 204 && status != 304) {
            return true;
        }
        return false;
    }

    private void setBodyProvider(InputStream bodyProvider, int bodyLength) {
        if (bodyProvider.markSupported()) {
            bodyProvider.mark(Integer.MAX_VALUE);
            ((BasicHttpEntityEnclosingRequest) this.mHttpRequest).setEntity(new InputStreamEntity(bodyProvider, (long) bodyLength));
            return;
        }
        throw new IllegalArgumentException("bodyProvider must support mark()");
    }

    public void handleSslErrorResponse(boolean proceed) {
        HttpsConnection connection = (HttpsConnection) this.mConnection;
        if (connection != null) {
            connection.restartConnection(proceed);
        }
    }

    /* access modifiers changed from: package-private */
    public void error(int errorId, String errorMessage) {
        this.mEventHandler.error(errorId, errorMessage);
    }
}
