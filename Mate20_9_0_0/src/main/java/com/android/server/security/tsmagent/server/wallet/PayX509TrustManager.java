package com.android.server.security.tsmagent.server.wallet;

import android.content.Context;
import com.android.server.security.tsmagent.utils.HwLog;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class PayX509TrustManager implements X509TrustManager {
    private static final String KEY_TYPE = "bks";
    private static final String TRUST_FILE = "hicloudroot.bks";
    private static final String TRUST_MANAGER_TYPE = "X509";
    private static final String TRUST_PWD = "";
    protected List<X509TrustManager> m509TrustManager = new ArrayList();

    /* JADX WARNING: Removed duplicated region for block: B:19:0x0067 A:{ExcHandler: java.io.IOException (r1_4 'e' java.lang.Exception), Splitter: B:2:0x000d} */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0067 A:{ExcHandler: java.io.IOException (r1_4 'e' java.lang.Exception), Splitter: B:2:0x000d} */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x0067 A:{ExcHandler: java.io.IOException (r1_4 'e' java.lang.Exception), Splitter: B:2:0x000d} */
    /* JADX WARNING: Missing block: B:19:0x0067, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:21:?, code:
            r2 = new java.lang.StringBuilder();
            r2.append("PayX509TrustManager instance exception: ");
            r2.append(r1.getMessage());
            com.android.server.security.tsmagent.utils.HwLog.e(r2.toString());
     */
    /* JADX WARNING: Missing block: B:22:0x0080, code:
            if (r0 != null) goto L_0x0082;
     */
    /* JADX WARNING: Missing block: B:24:?, code:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:35:?, code:
            return;
     */
    /* JADX WARNING: Missing block: B:38:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public PayX509TrustManager(Context context) {
        InputStream is = null;
        if (context != null) {
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TRUST_MANAGER_TYPE);
                KeyStore trustKeyStore = KeyStore.getInstance(KEY_TYPE);
                is = context.getAssets().open(TRUST_FILE);
                is.reset();
                trustKeyStore.load(is, "".toCharArray());
                is.close();
                trustManagerFactory.init(trustKeyStore);
                TrustManager[] tms = trustManagerFactory.getTrustManagers();
                for (int i = 0; i < tms.length; i++) {
                    if (tms[i] instanceof X509TrustManager) {
                        this.m509TrustManager.add((X509TrustManager) tms[i]);
                    }
                }
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        HwLog.e("can not close InputStream");
                    }
                }
            } catch (Exception e2) {
            } catch (Throwable th) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e3) {
                        HwLog.e("can not close InputStream");
                    }
                }
            }
        } else {
            throw new IOException("context cannot be null");
        }
    }

    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkClientTrusted start,authType= ");
        stringBuilder.append(authType);
        HwLog.d(stringBuilder.toString());
        try {
            if (this.m509TrustManager.isEmpty()) {
                HwLog.e("Couldn't find a X509TrustManager");
            } else {
                ((X509TrustManager) this.m509TrustManager.get(0)).checkClientTrusted(chain, authType);
            }
        } catch (CertificateException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("CertificateException:");
            stringBuilder2.append(e.getMessage());
            HwLog.e(stringBuilder2.toString());
        }
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkServerTrusted  start, authType= ");
        stringBuilder.append(authType);
        HwLog.d(stringBuilder.toString());
        try {
            if (this.m509TrustManager.isEmpty()) {
                HwLog.e("checkServerTrusted- Couldn't find a X509TrustManager");
            } else {
                ((X509TrustManager) this.m509TrustManager.get(0)).checkServerTrusted(chain, authType);
            }
        } catch (CertificateException e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("CertificateException:");
            stringBuilder2.append(e.getMessage());
            HwLog.e(stringBuilder2.toString());
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        HwLog.v("getAcceptedIssuers start");
        ArrayList<X509Certificate> list = new ArrayList();
        for (X509TrustManager tm : this.m509TrustManager) {
            list.addAll(Arrays.asList(tm.getAcceptedIssuers()));
        }
        return (X509Certificate[]) list.toArray(new X509Certificate[list.size()]);
    }
}
