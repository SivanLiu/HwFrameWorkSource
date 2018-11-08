package com.huawei.secure.android.common;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class b implements X509TrustManager {
    protected ArrayList<X509TrustManager> jx = new ArrayList();

    public b(InputStream inputStream, String str) {
        abx(inputStream, str);
    }

    private void abx(InputStream inputStream, String str) {
        TrustManagerFactory instance = TrustManagerFactory.getInstance("X509");
        KeyStore instance2 = KeyStore.getInstance("bks");
        instance2.load(inputStream, str.toCharArray());
        instance.init(instance2);
        TrustManager[] trustManagers = instance.getTrustManagers();
        for (int i = 0; i < trustManagers.length; i++) {
            if (trustManagers[i] instanceof X509TrustManager) {
                this.jx.add((X509TrustManager) trustManagers[i]);
            }
        }
    }

    public void checkClientTrusted(X509Certificate[] x509CertificateArr, String str) {
        if (!this.jx.isEmpty()) {
            ((X509TrustManager) this.jx.get(0)).checkClientTrusted(x509CertificateArr, str);
        }
    }

    public void checkServerTrusted(X509Certificate[] x509CertificateArr, String str) {
        if (!this.jx.isEmpty()) {
            ((X509TrustManager) this.jx.get(0)).checkServerTrusted(x509CertificateArr, str);
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[this.jx.size()];
    }
}
