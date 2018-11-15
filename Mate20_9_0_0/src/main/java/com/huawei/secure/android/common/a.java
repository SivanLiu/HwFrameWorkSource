package com.huawei.secure.android.common;

import android.os.Build.VERSION;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;

public class a extends SSLSocketFactory {
    public static final X509HostnameVerifier jx = new BrowserCompatHostnameVerifier();
    public static final X509HostnameVerifier jy = new StrictHostnameVerifier();
    private static final String[] jz = new String[]{"TEA", "SHA0", "MD2", "MD4", "RIPEMD", "RC4", "DES", "GCM", "DESX", "DES40", "RC2", "MD5", "ANON", "NULL", "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"};
    private static String[] kb = null;
    private SSLContext ka;

    public a(InputStream inputStream, String str) {
        this.ka = null;
        this.ka = SSLContext.getInstance("TLS");
        this.ka.init(null, new X509TrustManager[]{new b(inputStream, str)}, new SecureRandom());
    }

    private static void acn(SSLSocket sSLSocket) {
        if (sSLSocket != null) {
            String[] enabledCipherSuites = sSLSocket.getEnabledCipherSuites();
            List arrayList = new ArrayList();
            String str = "";
            for (String str2 : enabledCipherSuites) {
                Object obj;
                String toUpperCase = str2.toUpperCase(Locale.US);
                for (CharSequence contains : jz) {
                    if (toUpperCase.contains(contains)) {
                        obj = 1;
                        break;
                    }
                }
                obj = null;
                if (obj == null) {
                    arrayList.add(str2);
                }
            }
            kb = (String[]) arrayList.toArray(new String[arrayList.size()]);
            sSLSocket.setEnabledCipherSuites(kb);
        }
    }

    private void aco(Socket socket) {
        if (socket != null && (socket instanceof SSLSocket)) {
            acm((SSLSocket) socket);
            acn((SSLSocket) socket);
        }
    }

    private void acm(SSLSocket sSLSocket) {
        if (sSLSocket != null && VERSION.SDK_INT >= 16) {
            sSLSocket.setEnabledProtocols(new String[]{"TLSv1.1", "TLSv1.2"});
        }
    }

    public String[] getDefaultCipherSuites() {
        if (kb == null) {
            return new String[0];
        }
        return (String[]) kb.clone();
    }

    public String[] getSupportedCipherSuites() {
        return new String[0];
    }

    public Socket createSocket(String str, int i) {
        Socket createSocket = this.ka.getSocketFactory().createSocket(str, i);
        aco(createSocket);
        return createSocket;
    }

    public Socket createSocket(InetAddress inetAddress, int i) {
        return createSocket(inetAddress.getHostAddress(), i);
    }

    public Socket createSocket(String str, int i, InetAddress inetAddress, int i2) {
        return createSocket(str, i);
    }

    public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress2, int i2) {
        return createSocket(inetAddress.getHostAddress(), i);
    }

    public Socket createSocket(Socket socket, String str, int i, boolean z) {
        Socket createSocket = this.ka.getSocketFactory().createSocket(socket, str, i, z);
        aco(createSocket);
        return createSocket;
    }
}
