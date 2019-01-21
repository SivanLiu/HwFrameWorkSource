package android.net.http;

import android.content.Context;
import android.util.Log;
import com.android.org.conscrypt.Conscrypt;
import com.android.org.conscrypt.FileClientSessionCache;
import com.android.org.conscrypt.OpenSSLContextImpl;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class HttpsConnection extends Connection {
    private static SSLSocketFactory mSslSocketFactory = null;
    private boolean mAborted = false;
    private HttpHost mProxyHost;
    private Object mSuspendLock = new Object();
    private boolean mSuspended = false;

    public /* bridge */ /* synthetic */ String toString() {
        return super.toString();
    }

    static {
        initializeEngine(null);
    }

    public static void initializeEngine(File sessionDir) {
        KeyManagementException e = null;
        if (sessionDir != null) {
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Caching SSL sessions in ");
                stringBuilder.append(sessionDir);
                stringBuilder.append(".");
                Log.d("HttpsConnection", stringBuilder.toString());
                e = FileClientSessionCache.usingDirectory(sessionDir);
            } catch (KeyManagementException e2) {
                throw new RuntimeException(e2);
            } catch (IOException e22) {
                throw new RuntimeException(e22);
            }
        }
        OpenSSLContextImpl sslContext = (OpenSSLContextImpl) Conscrypt.newPreferredSSLContextSpi();
        sslContext.engineInit(null, new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }}, null);
        sslContext.engineGetClientSessionContext().setPersistentCache(e22);
        synchronized (HttpsConnection.class) {
            mSslSocketFactory = sslContext.engineGetSocketFactory();
        }
    }

    private static synchronized SSLSocketFactory getSocketFactory() {
        SSLSocketFactory sSLSocketFactory;
        synchronized (HttpsConnection.class) {
            sSLSocketFactory = mSslSocketFactory;
        }
        return sSLSocketFactory;
    }

    HttpsConnection(Context context, HttpHost host, HttpHost proxy, RequestFeeder requestFeeder) {
        super(context, host, requestFeeder);
        this.mProxyHost = proxy;
    }

    void setCertificate(SslCertificate certificate) {
        this.mCertificate = certificate;
    }

    AndroidHttpClientConnection openConnection(Request req) throws IOException {
        String errorMessage;
        Request request = req;
        SSLSocket sslSock = null;
        if (this.mProxyHost != null) {
            AndroidHttpClientConnection proxyConnection = null;
            Socket proxySock = null;
            try {
                proxySock = new Socket(this.mProxyHost.getHostName(), this.mProxyHost.getPort());
                proxySock.setSoTimeout(60000);
                proxyConnection = new AndroidHttpClientConnection();
                HttpParams params = new BasicHttpParams();
                HttpConnectionParams.setSocketBufferSize(params, 8192);
                proxyConnection.bind(proxySock, params);
                Headers headers = new Headers();
                try {
                    int statusCode;
                    StatusLine statusLine;
                    BasicHttpRequest proxyReq = new BasicHttpRequest("CONNECT", this.mHost.toHostString());
                    for (Header h : request.mHttpRequest.getAllHeaders()) {
                        String headerName = h.getName().toLowerCase(Locale.ROOT);
                        if (headerName.startsWith("proxy") || headerName.equals("keep-alive") || headerName.equals("host")) {
                            proxyReq.addHeader(h);
                        }
                    }
                    proxyConnection.sendRequestHeader(proxyReq);
                    proxyConnection.flush();
                    do {
                        statusLine = proxyConnection.parseResponseHeader(headers);
                        statusCode = statusLine.getStatusCode();
                    } while (statusCode < HttpStatus.SC_OK);
                    if (statusCode == HttpStatus.SC_OK) {
                        try {
                            sslSock = (SSLSocket) getSocketFactory().createSocket(proxySock, this.mHost.getHostName(), this.mHost.getPort(), true);
                        } catch (IOException e) {
                            if (sslSock != null) {
                                sslSock.close();
                            }
                            errorMessage = e.getMessage();
                            if (errorMessage == null) {
                                errorMessage = "failed to create an SSL socket";
                            }
                            throw new IOException(errorMessage);
                        }
                    }
                    ProtocolVersion version = statusLine.getProtocolVersion();
                    request.mEventHandler.status(version.getMajor(), version.getMinor(), statusCode, statusLine.getReasonPhrase());
                    request.mEventHandler.headers(headers);
                    request.mEventHandler.endData();
                    proxyConnection.close();
                    return null;
                } catch (ParseException e2) {
                    errorMessage = e2.getMessage();
                    if (errorMessage == null) {
                        errorMessage = "failed to send a CONNECT request";
                    }
                    throw new IOException(errorMessage);
                } catch (HttpException e3) {
                    errorMessage = e3.getMessage();
                    if (errorMessage == null) {
                        errorMessage = "failed to send a CONNECT request";
                    }
                    throw new IOException(errorMessage);
                } catch (IOException e4) {
                    errorMessage = e4.getMessage();
                    if (errorMessage == null) {
                        errorMessage = "failed to send a CONNECT request";
                    }
                    throw new IOException(errorMessage);
                }
            } catch (IOException e42) {
                if (proxyConnection != null) {
                    proxyConnection.close();
                }
                errorMessage = e42.getMessage();
                if (errorMessage == null) {
                    errorMessage = "failed to establish a connection to the proxy";
                }
                throw new IOException(errorMessage);
            }
        }
        try {
            sslSock = (SSLSocket) getSocketFactory().createSocket(this.mHost.getHostName(), this.mHost.getPort());
            sslSock.setSoTimeout(60000);
        } catch (IOException e422) {
            if (sslSock != null) {
                sslSock.close();
            }
            errorMessage = e422.getMessage();
            if (errorMessage == null) {
                errorMessage = "failed to create an SSL socket";
            }
            throw new IOException(errorMessage);
        }
        SslError error = CertificateChainValidator.getInstance().doHandshakeAndValidateServerCertificates(this, sslSock, this.mHost.getHostName());
        if (error != null) {
            synchronized (this.mSuspendLock) {
                this.mSuspended = true;
            }
            if (req.getEventHandler().handleSslErrorRequest(error)) {
                synchronized (this.mSuspendLock) {
                    if (this.mSuspended) {
                        try {
                            this.mSuspendLock.wait(600000);
                            if (this.mSuspended) {
                                this.mSuspended = false;
                                this.mAborted = true;
                            }
                        } catch (InterruptedException e5) {
                        }
                    }
                    if (this.mAborted) {
                        sslSock.close();
                        throw new SSLConnectionClosedByUserException("connection closed by the user");
                    }
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("failed to handle ");
                stringBuilder.append(error);
                throw new IOException(stringBuilder.toString());
            }
        }
        AndroidHttpClientConnection conn = new AndroidHttpClientConnection();
        BasicHttpParams params2 = new BasicHttpParams();
        params2.setIntParameter("http.socket.buffer-size", 8192);
        conn.bind(sslSock, params2);
        return conn;
    }

    void closeConnection() {
        if (this.mSuspended) {
            restartConnection(false);
        }
        try {
            if (this.mHttpClientConnection != null && this.mHttpClientConnection.isOpen()) {
                this.mHttpClientConnection.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void restartConnection(boolean proceed) {
        synchronized (this.mSuspendLock) {
            if (this.mSuspended) {
                this.mSuspended = false;
                this.mAborted = proceed ^ 1;
                this.mSuspendLock.notify();
            }
        }
    }

    String getScheme() {
        return "https";
    }
}
