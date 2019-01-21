package org.apache.http.impl.conn;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import org.apache.http.HttpHost;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

@Deprecated
public class DefaultClientConnectionOperator implements ClientConnectionOperator {
    public static final String SOCK_SEND_BUFF = "http.socket.send-buffer";
    private static final PlainSocketFactory staticPlainSocketFactory = new PlainSocketFactory();
    protected SchemeRegistry schemeRegistry;

    public DefaultClientConnectionOperator(SchemeRegistry schemes) {
        if (schemes != null) {
            this.schemeRegistry = schemes;
            return;
        }
        throw new IllegalArgumentException("Scheme registry must not be null.");
    }

    public OperatedClientConnection createConnection() {
        return new DefaultClientConnection();
    }

    /* JADX WARNING: Removed duplicated region for block: B:87:0x010f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x00f4 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x00e8 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x010f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x010f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x00e8 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x00f4 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x010f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x00d5 A:{ExcHandler: ConnectTimeoutException (e org.apache.http.conn.ConnectTimeoutException), Splitter:B:15:0x0046} */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x010f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x00f4 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x00e8 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x010f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x00f4 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x010f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x010f A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x00e8 A:{SYNTHETIC} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:55:0x00cd, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:56:0x00ce, code skipped:
            r4 = r24;
            r17 = r14;
            r14 = r8;
            r5 = r10;
     */
    /* JADX WARNING: Missing block: B:57:0x00d5, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:58:0x00d6, code skipped:
            r4 = r24;
            r18 = r9;
            r19 = r10;
            r17 = r14;
            r14 = r8;
     */
    /* JADX WARNING: Missing block: B:61:0x00e8, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:67:0x00f6, code skipped:
            if ((r0 instanceof java.net.ConnectException) != false) goto L_0x00f8;
     */
    /* JADX WARNING: Missing block: B:68:0x00f8, code skipped:
            r6 = (java.net.ConnectException) r0;
     */
    /* JADX WARNING: Missing block: B:69:0x00fc, code skipped:
            r6 = new java.net.ConnectException(r0.getMessage());
            r6.initCause(r0);
     */
    /* JADX WARNING: Missing block: B:71:0x010d, code skipped:
            throw new org.apache.http.conn.HttpHostConnectException(r3, r6);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void openConnection(OperatedClientConnection conn, HttpHost target, InetAddress local, HttpContext context, HttpParams params) throws IOException {
        SocketException ex;
        InetAddress[] addresses;
        ConnectTimeoutException ex2;
        OperatedClientConnection operatedClientConnection = conn;
        HttpHost httpHost = target;
        HttpParams httpParams = params;
        HttpContext httpContext;
        if (operatedClientConnection == null) {
            httpContext = context;
            throw new IllegalArgumentException("Connection must not be null.");
        } else if (httpHost == null) {
            httpContext = context;
            throw new IllegalArgumentException("Target host must not be null.");
        } else if (httpParams == null) {
            httpContext = context;
            throw new IllegalArgumentException("Parameters must not be null.");
        } else if (conn.isOpen()) {
            httpContext = context;
            throw new IllegalArgumentException("Connection must not be open.");
        } else {
            SocketFactory plain_sf;
            LayeredSocketFactory layered_sf;
            Scheme schm = this.schemeRegistry.getScheme(target.getSchemeName());
            SocketFactory sf = schm.getSocketFactory();
            if (sf instanceof LayeredSocketFactory) {
                plain_sf = staticPlainSocketFactory;
                layered_sf = (LayeredSocketFactory) sf;
            } else {
                plain_sf = sf;
                layered_sf = null;
            }
            SocketFactory plain_sf2 = plain_sf;
            LayeredSocketFactory layered_sf2 = layered_sf;
            InetAddress[] addresses2 = InetAddress.getAllByName(target.getHostName());
            int i = 0;
            while (true) {
                SocketFactory plain_sf3;
                int i2 = i;
                if (i2 < addresses2.length) {
                    Socket sock = plain_sf2.createSocket();
                    operatedClientConnection.opening(sock, httpHost);
                    int i3 = 1;
                    Socket plain_sf4;
                    try {
                        SocketFactory socketFactory = plain_sf2;
                        plain_sf3 = plain_sf2;
                        plain_sf4 = sock;
                        int i4 = i2;
                        InetAddress[] addresses3 = addresses2;
                        try {
                            Socket connsock = socketFactory.connectSocket(sock, addresses2[i2].getHostAddress(), schm.resolvePort(target.getPort()), local, 0, httpParams);
                            if (plain_sf4 != connsock) {
                                sock = connsock;
                                try {
                                    operatedClientConnection.opening(sock, httpHost);
                                } catch (SocketException e) {
                                    ex = e;
                                    httpContext = context;
                                    i2 = i4;
                                    addresses = addresses3;
                                    i3 = 1;
                                } catch (ConnectTimeoutException e2) {
                                    ex2 = e2;
                                    httpContext = context;
                                    i3 = 1;
                                    addresses = addresses3;
                                    i2 = i4;
                                    if (i2 != addresses.length - i3) {
                                    }
                                }
                            } else {
                                sock = plain_sf4;
                            }
                        } catch (SocketException e3) {
                            ex = e3;
                            httpContext = context;
                            i3 = 1;
                            sock = plain_sf4;
                            i2 = i4;
                            addresses = addresses3;
                            if (i2 == addresses.length - i3) {
                            }
                        } catch (ConnectTimeoutException e4) {
                            ex2 = e4;
                            httpContext = context;
                            i3 = 1;
                            sock = plain_sf4;
                            addresses = addresses3;
                            i2 = i4;
                            if (i2 != addresses.length - i3) {
                            }
                        }
                        try {
                            prepareSocket(sock, context, httpParams);
                            if (layered_sf2 == null) {
                                operatedClientConnection.openCompleted(sf.isSecure(sock), httpParams);
                                break;
                            }
                            i3 = 1;
                            try {
                                Socket layeredsock = layered_sf2.createSocket(sock, target.getHostName(), schm.resolvePort(target.getPort()), true);
                                if (layeredsock != sock) {
                                    operatedClientConnection.opening(layeredsock, httpHost);
                                }
                                operatedClientConnection.openCompleted(sf.isSecure(layeredsock), httpParams);
                            } catch (SocketException e5) {
                                ex = e5;
                                i2 = i4;
                                addresses = addresses3;
                                if (i2 == addresses.length - i3) {
                                }
                            } catch (ConnectTimeoutException e6) {
                                ex2 = e6;
                                addresses = addresses3;
                                i2 = i4;
                                if (i2 != addresses.length - i3) {
                                }
                            }
                        } catch (SocketException e7) {
                            ex = e7;
                            i3 = 1;
                            i2 = i4;
                            addresses = addresses3;
                            if (i2 == addresses.length - i3) {
                            }
                        } catch (ConnectTimeoutException e8) {
                            ex2 = e8;
                            i3 = 1;
                            addresses = addresses3;
                            i2 = i4;
                            if (i2 != addresses.length - i3) {
                            }
                        }
                    } catch (SocketException e9) {
                        ex = e9;
                        httpContext = context;
                        addresses = addresses2;
                        plain_sf3 = plain_sf2;
                        plain_sf4 = sock;
                        if (i2 == addresses.length - i3) {
                        }
                    } catch (ConnectTimeoutException e10) {
                    }
                } else {
                    httpContext = context;
                    addresses = addresses2;
                    plain_sf3 = plain_sf2;
                    return;
                }
                i = i2 + 1;
                addresses2 = addresses;
                plain_sf2 = plain_sf3;
            }
        }
    }

    public void updateSecureConnection(OperatedClientConnection conn, HttpHost target, HttpContext context, HttpParams params) throws IOException {
        if (conn == null) {
            throw new IllegalArgumentException("Connection must not be null.");
        } else if (target == null) {
            throw new IllegalArgumentException("Target host must not be null.");
        } else if (params == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        } else if (conn.isOpen()) {
            Scheme schm = this.schemeRegistry.getScheme(target.getSchemeName());
            if (schm.getSocketFactory() instanceof LayeredSocketFactory) {
                LayeredSocketFactory lsf = (LayeredSocketFactory) schm.getSocketFactory();
                try {
                    Socket sock = lsf.createSocket(conn.getSocket(), target.getHostName(), schm.resolvePort(target.getPort()), true);
                    prepareSocket(sock, context, params);
                    conn.update(sock, target, lsf.isSecure(sock), params);
                    return;
                } catch (ConnectException ex) {
                    throw new HttpHostConnectException(target, ex);
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Target scheme (");
            stringBuilder.append(schm.getName());
            stringBuilder.append(") must have layered socket factory.");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            throw new IllegalArgumentException("Connection must be open.");
        }
    }

    protected void prepareSocket(Socket sock, HttpContext context, HttpParams params) throws IOException {
        sock.setTcpNoDelay(HttpConnectionParams.getTcpNoDelay(params));
        sock.setSoTimeout(HttpConnectionParams.getSoTimeout(params));
        boolean z = false;
        int sendBufSize = params.getIntParameter(SOCK_SEND_BUFF, 0);
        if (sendBufSize > 0) {
            sock.setSendBufferSize(sendBufSize);
        }
        int linger = HttpConnectionParams.getLinger(params);
        if (linger >= 0) {
            if (linger > 0) {
                z = true;
            }
            sock.setSoLinger(z, linger);
        }
    }
}
