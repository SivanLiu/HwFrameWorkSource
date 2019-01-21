package android.net.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.RequestContent;

class Request {
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

    Request(String method, HttpHost host, HttpHost proxyHost, String path, InputStream bodyProvider, int bodyLength, EventHandler eventHandler, Map<String, String> headers) {
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

    synchronized void setLoadingPaused(boolean pause) {
        this.mLoadingPaused = pause;
        if (!this.mLoadingPaused) {
            notify();
        }
    }

    void setConnection(Connection connection) {
        this.mConnection = connection;
    }

    EventHandler getEventHandler() {
        return this.mEventHandler;
    }

    void addHeader(String name, String value) {
        String damage;
        if (name == null) {
            damage = "Null http header name";
            HttpLog.e(damage);
            throw new NullPointerException(damage);
        } else if (value == null || value.length() == 0) {
            damage = new StringBuilder();
            damage.append("Null or empty value for header \"");
            damage.append(name);
            damage.append("\"");
            damage = damage.toString();
            HttpLog.e(damage);
            throw new RuntimeException(damage);
        } else {
            this.mHttpRequest.addHeader(name, value);
        }
    }

    void addHeaders(Map<String, String> headers) {
        if (headers != null) {
            for (Entry<String, String> entry : headers.entrySet()) {
                addHeader((String) entry.getKey(), (String) entry.getValue());
            }
        }
    }

    void sendRequest(AndroidHttpClientConnection httpClientConnection) throws HttpException, IOException {
        if (!this.mCancelled) {
            requestContentProcessor.process(this.mHttpRequest, this.mConnection.getHttpContext());
            httpClientConnection.sendRequestHeader(this.mHttpRequest);
            if (this.mHttpRequest instanceof HttpEntityEnclosingRequest) {
                httpClientConnection.sendRequestEntity((HttpEntityEnclosingRequest) this.mHttpRequest);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:112:0x0112 A:{SYNTHETIC, EDGE_INSN: B:112:0x0112->B:81:0x0112 ?: BREAK  , EDGE_INSN: B:112:0x0112->B:81:0x0112 ?: BREAK  } */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x009d A:{SYNTHETIC, Splitter:B:30:0x009d} */
    /* JADX WARNING: Removed duplicated region for block: B:100:0x013f A:{Catch:{ EOFException -> 0x0138, IOException -> 0x011d, all -> 0x0117, all -> 0x0145 }} */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0148  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0148  */
    /* JADX WARNING: Removed duplicated region for block: B:100:0x013f A:{Catch:{ EOFException -> 0x0138, IOException -> 0x011d, all -> 0x0117, all -> 0x0145 }} */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0126  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0148  */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0126  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x0148  */
    /* JADX WARNING: Missing block: B:50:?, code skipped:
            r15 = r13.read(r14, r2, r14.length - r2);
     */
    /* JADX WARNING: Missing block: B:51:0x00d7, code skipped:
            if (r15 == true) goto L_0x00e1;
     */
    /* JADX WARNING: Missing block: B:52:0x00d9, code skipped:
            r2 = r2 + r15;
     */
    /* JADX WARNING: Missing block: B:53:0x00da, code skipped:
            if (r10 == false) goto L_0x00e1;
     */
    /* JADX WARNING: Missing block: B:54:0x00dc, code skipped:
            r1.mReceivedBytes += r15;
     */
    /* JADX WARNING: Missing block: B:56:0x00e2, code skipped:
            if (r15 == true) goto L_0x00ec;
     */
    /* JADX WARNING: Missing block: B:57:0x00e4, code skipped:
            if (r2 < r7) goto L_0x00e7;
     */
    /* JADX WARNING: Missing block: B:60:0x00ec, code skipped:
            r1.mEventHandler.data(r14, r2);
     */
    /* JADX WARNING: Missing block: B:61:0x00f1, code skipped:
            r2 = 0;
     */
    /* JADX WARNING: Missing block: B:68:0x00f8, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:69:0x00f9, code skipped:
            r15 = r2;
     */
    /* JADX WARNING: Missing block: B:70:0x00fb, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:71:0x00fc, code skipped:
            r15 = r2;
     */
    /* JADX WARNING: Missing block: B:73:0x00ff, code skipped:
            r15 = r2;
     */
    /* JADX WARNING: Missing block: B:82:0x0114, code skipped:
            if (r13 == null) goto L_0x0156;
     */
    /* JADX WARNING: Missing block: B:91:0x012b, code skipped:
            if (r13 != null) goto L_0x014e;
     */
    /* JADX WARNING: Missing block: B:105:0x014c, code skipped:
            if (r13 != null) goto L_0x014e;
     */
    /* JADX WARNING: Missing block: B:106:0x014e, code skipped:
            r13.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void readResponse(AndroidHttpClientConnection httpClientConnection) throws IOException, ParseException {
        int count;
        ProtocolVersion v;
        Throwable th;
        IOException e;
        AndroidHttpClientConnection androidHttpClientConnection = httpClientConnection;
        if (!this.mCancelled) {
            StatusLine statusLine;
            int statusCode;
            httpClientConnection.flush();
            Headers header = new Headers();
            while (true) {
                statusLine = androidHttpClientConnection.parseResponseHeader(header);
                statusCode = statusLine.getStatusCode();
                if (statusCode >= HttpStatus.SC_OK) {
                    break;
                }
                StatusLine statusLine2 = statusLine;
            }
            ProtocolVersion v2 = statusLine.getProtocolVersion();
            this.mEventHandler.status(v2.getMajor(), v2.getMinor(), statusCode, statusLine.getReasonPhrase());
            this.mEventHandler.headers(header);
            HttpEntity entity = null;
            boolean hasBody = canResponseHaveBody(this.mHttpRequest, statusCode);
            if (hasBody) {
                entity = androidHttpClientConnection.receiveResponseEntity(header);
            }
            HttpEntity entity2 = entity;
            boolean supportPartialContent = "bytes".equalsIgnoreCase(header.getAcceptRanges());
            if (entity2 != null) {
                InputStream nis;
                int lowWater;
                boolean len;
                boolean hasBody2;
                InputStream is = entity2.getContent();
                Header contentEncoding = entity2.getContentEncoding();
                InputStream nis2 = null;
                byte[] buf = null;
                int count2 = 0;
                if (contentEncoding != null) {
                    try {
                        if (contentEncoding.getValue().equals("gzip")) {
                            nis = new GZIPInputStream(is);
                            nis2 = nis;
                            buf = this.mConnection.getBuf();
                            lowWater = buf.length / 2;
                            count = count2;
                            len = false;
                            while (true) {
                                hasBody2 = hasBody;
                                if (!len) {
                                    break;
                                }
                                try {
                                    synchronized (this) {
                                        while (this.mLoadingPaused) {
                                            try {
                                                wait();
                                            } catch (InterruptedException e2) {
                                                InterruptedException interruptedException = e2;
                                                StringBuilder stringBuilder = new StringBuilder();
                                                v = v2;
                                                stringBuilder.append("Interrupted exception whilst network thread paused at WebCore's request. ");
                                                stringBuilder.append(e2.getMessage());
                                                HttpLog.e(stringBuilder.toString());
                                                v2 = v;
                                            } catch (Throwable th2) {
                                                th = th2;
                                                throw th;
                                            }
                                        }
                                        v = v2;
                                    }
                                } catch (EOFException e3) {
                                    v = v2;
                                    count2 = count;
                                    if (count2 > 0) {
                                        this.mEventHandler.data(buf, count2);
                                    }
                                } catch (IOException e4) {
                                    e = e4;
                                    v = v2;
                                    count2 = count;
                                    if (statusCode != HttpStatus.SC_OK || statusCode == HttpStatus.SC_PARTIAL_CONTENT) {
                                        if (supportPartialContent && count2 > 0) {
                                            this.mEventHandler.data(buf, count2);
                                        }
                                        throw e;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    v = v2;
                                    count2 = count;
                                    if (nis2 != null) {
                                        nis2.close();
                                    }
                                    throw th;
                                }
                                hasBody = hasBody2;
                                v2 = v;
                            }
                        }
                    } catch (EOFException e5) {
                        hasBody2 = hasBody;
                        v = v2;
                        if (count2 > 0) {
                        }
                    } catch (IOException e6) {
                        e = e6;
                        hasBody2 = hasBody;
                        v = v2;
                        if (statusCode != HttpStatus.SC_OK) {
                        }
                        this.mEventHandler.data(buf, count2);
                        throw e;
                    } catch (Throwable th4) {
                        th = th4;
                        hasBody2 = hasBody;
                        v = v2;
                        if (nis2 != null) {
                        }
                        throw th;
                    }
                }
                nis = is;
                nis2 = nis;
                try {
                    buf = this.mConnection.getBuf();
                    lowWater = buf.length / 2;
                    count = count2;
                    len = false;
                    while (true) {
                        hasBody2 = hasBody;
                        if (!len) {
                        }
                        hasBody = hasBody2;
                        v2 = v;
                    }
                } catch (EOFException e7) {
                    hasBody2 = hasBody;
                    v = v2;
                    if (count2 > 0) {
                    }
                } catch (IOException e8) {
                    e = e8;
                    hasBody2 = hasBody;
                    v = v2;
                    if (statusCode != HttpStatus.SC_OK) {
                    }
                    this.mEventHandler.data(buf, count2);
                    throw e;
                } catch (Throwable th5) {
                    th = th5;
                    if (nis2 != null) {
                    }
                    throw th;
                }
            }
            v = v2;
            this.mConnection.setCanPersist(entity2, statusLine.getProtocolVersion(), header.getConnectionType());
            this.mEventHandler.endData();
            complete();
        }
    }

    synchronized void cancel() {
        this.mLoadingPaused = false;
        notify();
        this.mCancelled = true;
        if (this.mConnection != null) {
            this.mConnection.cancel();
        }
    }

    String getHostPort() {
        String myScheme = this.mHost.getSchemeName();
        int myPort = this.mHost.getPort();
        if ((myPort == 80 || !myScheme.equals(HttpHost.DEFAULT_SCHEME_NAME)) && (myPort == 443 || !myScheme.equals("https"))) {
            return this.mHost.getHostName();
        }
        return this.mHost.toHostString();
    }

    String getUri() {
        if (this.mProxyHost == null || this.mHost.getSchemeName().equals("https")) {
            return this.mPath;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mHost.getSchemeName());
        stringBuilder.append("://");
        stringBuilder.append(getHostPort());
        stringBuilder.append(this.mPath);
        return stringBuilder.toString();
    }

    public String toString() {
        return this.mPath;
    }

    void reset() {
        this.mHttpRequest.removeHeaders("content-length");
        if (this.mBodyProvider != null) {
            try {
                this.mBodyProvider.reset();
            } catch (IOException e) {
            }
            setBodyProvider(this.mBodyProvider, this.mBodyLength);
        }
        if (this.mReceivedBytes > 0) {
            this.mFailCount = 0;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("*** Request.reset() to range:");
            stringBuilder.append(this.mReceivedBytes);
            HttpLog.v(stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("bytes=");
            stringBuilder2.append(this.mReceivedBytes);
            stringBuilder2.append("-");
            this.mHttpRequest.setHeader("Range", stringBuilder2.toString());
        }
    }

    void waitUntilComplete() {
        synchronized (this.mClientResource) {
            try {
                this.mClientResource.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    void complete() {
        synchronized (this.mClientResource) {
            this.mClientResource.notifyAll();
        }
    }

    private static boolean canResponseHaveBody(HttpRequest request, int status) {
        boolean z = false;
        if (HttpHead.METHOD_NAME.equalsIgnoreCase(request.getRequestLine().getMethod())) {
            return false;
        }
        if (!(status < HttpStatus.SC_OK || status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_NOT_MODIFIED)) {
            z = true;
        }
        return z;
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
        HttpsConnection connection = this.mConnection;
        if (connection != null) {
            connection.restartConnection(proceed);
        }
    }

    void error(int errorId, String errorMessage) {
        this.mEventHandler.error(errorId, errorMessage);
    }
}
