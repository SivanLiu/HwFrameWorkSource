package com.huawei.android.pushselfshow.utils.b;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Proxy;
import android.os.Build.VERSION;
import com.huawei.android.pushagent.a.a.c;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;

public class d {
    private static String b = "PushSelfShowLog";
    private Context a;

    public d(Context context) {
        this.a = context;
    }

    public String a() {
        Object -l_1_R = null;
        try {
            -l_1_R = VERSION.SDK_INT < 11 ? Proxy.getHost(this.a) : System.getProperty("http.proxyHost");
            c.b(b, "proxyHost=" + -l_1_R);
        } catch (Object -l_2_R) {
            c.d(b, "getProxyHost error:" + -l_2_R.getMessage());
        }
        return -l_1_R;
    }

    public HttpResponse a(String str, HttpClient httpClient, HttpGet httpGet) {
        Object -l_5_R;
        Object -l_4_R = null;
        try {
            -l_5_R = httpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(-l_5_R, 30000);
            HttpConnectionParams.setSoTimeout(-l_5_R, 30000);
            HttpClientParams.setRedirecting(-l_5_R, true);
            HttpProtocolParams.setUseExpectContinue(-l_5_R, false);
            a((HttpRequest) httpGet, httpClient, str);
            -l_4_R = httpClient.execute(httpGet);
        } catch (Object -l_5_R2) {
            c.d(b, "SocketTimeoutException occur" + -l_5_R2.getMessage());
        } catch (Object -l_5_R22) {
            c.d(b, "ClientProtocolException occur" + -l_5_R22.getMessage());
        } catch (Object -l_5_R222) {
            c.d(b, "IOException occur" + -l_5_R222.getMessage());
        } catch (Object -l_5_R2222) {
            c.d(b, "Exception occur" + -l_5_R2222.getMessage());
        }
        return -l_4_R;
    }

    public void a(HttpRequest httpRequest, HttpClient httpClient, String str) {
        httpRequest.setHeader("Accept-Encoding", "");
        Object -l_4_R = a();
        int -l_5_I = b();
        Object -l_7_R = ((ConnectivityManager) this.a.getSystemService("connectivity")).getActiveNetworkInfo();
        if (-l_7_R != null && -l_7_R.getType() == 0 && -l_4_R != null && -l_4_R.length() > 0 && -l_5_I != -1) {
            Object -l_8_R = httpClient.getParams();
            ConnRouteParams.setDefaultProxy(-l_8_R, new HttpHost(a(), b()));
            httpRequest.setParams(-l_8_R);
        }
    }

    public int b() {
        int -l_1_I = -1;
        try {
            if (VERSION.SDK_INT < 11) {
                -l_1_I = Proxy.getPort(this.a);
            } else {
                Object -l_2_R;
                -l_2_R = System.getProperty("http.proxyPort");
                if (-l_2_R == null) {
                    -l_2_R = "-1";
                }
                -l_1_I = Integer.parseInt(-l_2_R);
            }
            c.b(b, "proxyPort=" + -l_1_I);
        } catch (Object -l_2_R2) {
            c.d(b, "proxyPort error:" + -l_2_R2.getMessage());
        }
        return -l_1_I;
    }
}
