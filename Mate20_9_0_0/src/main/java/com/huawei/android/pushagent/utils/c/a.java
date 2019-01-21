package com.huawei.android.pushagent.utils.c;

import android.content.Context;
import android.os.Build.VERSION;
import com.huawei.android.pushagent.constant.HttpMethod;
import com.huawei.android.pushagent.utils.d;
import com.huawei.android.pushagent.utils.e.c;
import com.huawei.android.pushagent.utils.g;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.apache.http.conn.ssl.SSLSocketFactory;

public class a {
    private Context appCtx;
    private String gv;
    private int gw = 30000;
    private int gx = 0;
    private List<String> gy;
    private int gz = 30000;
    private boolean ha = false;

    public a(Context context, String str, List<String> list) {
        this.appCtx = context.getApplicationContext();
        this.gv = str;
        this.gy = list;
    }

    public void ty(int i) {
        this.gx = i;
    }

    public void ua(boolean z) {
        this.ha = z;
    }

    public void tx(int i) {
        this.gw = i;
    }

    public void tz(int i) {
        this.gz = i;
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:21:0x00a4=Splitter:B:21:0x00a4, B:27:0x00d9=Splitter:B:27:0x00d9, B:32:0x010e=Splitter:B:32:0x010e} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String tv(String str, HttpMethod httpMethod) {
        Closeable inputStream;
        Closeable closeable;
        Throwable th;
        if (str == null) {
            return null;
        }
        String ub = new c().ud(this.gv).ue(this.gy).uf(this.gx).ug().ub();
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "execute url is: " + c.wh(ub));
        int i = -1;
        HttpURLConnection tu;
        Closeable bufferedOutputStream;
        try {
            tu = tu(ub, httpMethod.bb());
            if (tu == null) {
                g.zx(null);
                g.zx(null);
                g.zx(null);
                g.zy(tu);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                return null;
            }
            try {
                bufferedOutputStream = new BufferedOutputStream(tu.getOutputStream());
                try {
                    bufferedOutputStream.write(str.getBytes("UTF-8"));
                    bufferedOutputStream.flush();
                    i = tu.getResponseCode();
                    inputStream = tu.getInputStream();
                } catch (IOException e) {
                    closeable = null;
                    inputStream = null;
                } catch (RuntimeException e2) {
                    closeable = null;
                    inputStream = null;
                    com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter RuntimeException - http code:" + i);
                    g.zx(bufferedOutputStream);
                    g.zx(inputStream);
                    g.zx(closeable);
                    g.zy(tu);
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                    return null;
                } catch (Exception e3) {
                    closeable = null;
                    inputStream = null;
                    try {
                        com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter unknown exception - http code:" + i);
                        g.zx(bufferedOutputStream);
                        g.zx(inputStream);
                        g.zx(closeable);
                        g.zy(tu);
                        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                        return null;
                    } catch (Throwable th2) {
                        th = th2;
                        g.zx(bufferedOutputStream);
                        g.zx(inputStream);
                        g.zx(closeable);
                        g.zy(tu);
                        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    closeable = null;
                    inputStream = null;
                    g.zx(bufferedOutputStream);
                    g.zx(inputStream);
                    g.zx(closeable);
                    g.zy(tu);
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                    throw th;
                }
            } catch (IOException e4) {
                closeable = null;
                inputStream = null;
                bufferedOutputStream = null;
                com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter IOException - http code:" + i);
                g.zx(bufferedOutputStream);
                g.zx(inputStream);
                g.zx(closeable);
                g.zy(tu);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                return null;
            } catch (RuntimeException e5) {
                closeable = null;
                inputStream = null;
                bufferedOutputStream = null;
                com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter RuntimeException - http code:" + i);
                g.zx(bufferedOutputStream);
                g.zx(inputStream);
                g.zx(closeable);
                g.zy(tu);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                return null;
            } catch (Exception e6) {
                closeable = null;
                inputStream = null;
                bufferedOutputStream = null;
                com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter unknown exception - http code:" + i);
                g.zx(bufferedOutputStream);
                g.zx(inputStream);
                g.zx(closeable);
                g.zy(tu);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                return null;
            } catch (Throwable th4) {
                th = th4;
                closeable = null;
                inputStream = null;
                bufferedOutputStream = null;
                g.zx(bufferedOutputStream);
                g.zx(inputStream);
                g.zx(closeable);
                g.zy(tu);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                throw th;
            }
            try {
                closeable = new BufferedInputStream(inputStream);
                try {
                    String zw = g.zw(closeable);
                    g.zx(bufferedOutputStream);
                    g.zx(inputStream);
                    g.zx(closeable);
                    g.zy(tu);
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                    return zw;
                } catch (IOException e7) {
                    com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter IOException - http code:" + i);
                    g.zx(bufferedOutputStream);
                    g.zx(inputStream);
                    g.zx(closeable);
                    g.zy(tu);
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                    return null;
                } catch (RuntimeException e8) {
                    com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter RuntimeException - http code:" + i);
                    g.zx(bufferedOutputStream);
                    g.zx(inputStream);
                    g.zx(closeable);
                    g.zy(tu);
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                    return null;
                } catch (Exception e9) {
                    com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter unknown exception - http code:" + i);
                    g.zx(bufferedOutputStream);
                    g.zx(inputStream);
                    g.zx(closeable);
                    g.zy(tu);
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                    return null;
                }
            } catch (IOException e10) {
                closeable = null;
                com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter IOException - http code:" + i);
                g.zx(bufferedOutputStream);
                g.zx(inputStream);
                g.zx(closeable);
                g.zy(tu);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                return null;
            } catch (RuntimeException e11) {
                closeable = null;
                com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter RuntimeException - http code:" + i);
                g.zx(bufferedOutputStream);
                g.zx(inputStream);
                g.zx(closeable);
                g.zy(tu);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                return null;
            } catch (Exception e12) {
                closeable = null;
                com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter unknown exception - http code:" + i);
                g.zx(bufferedOutputStream);
                g.zx(inputStream);
                g.zx(closeable);
                g.zy(tu);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                return null;
            } catch (Throwable th5) {
                th = th5;
                closeable = null;
                g.zx(bufferedOutputStream);
                g.zx(inputStream);
                g.zx(closeable);
                g.zy(tu);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
                throw th;
            }
        } catch (IOException e13) {
            tu = null;
            closeable = null;
            inputStream = null;
            bufferedOutputStream = null;
        } catch (RuntimeException e14) {
            tu = null;
            closeable = null;
            inputStream = null;
            bufferedOutputStream = null;
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter RuntimeException - http code:" + i);
            g.zx(bufferedOutputStream);
            g.zx(inputStream);
            g.zx(closeable);
            g.zy(tu);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
            return null;
        } catch (Exception e15) {
            tu = null;
            closeable = null;
            inputStream = null;
            bufferedOutputStream = null;
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "http execute encounter unknown exception - http code:" + i);
            g.zx(bufferedOutputStream);
            g.zx(inputStream);
            g.zx(closeable);
            g.zy(tu);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
            return null;
        } catch (Throwable th6) {
            th = th6;
            tu = null;
            closeable = null;
            inputStream = null;
            bufferedOutputStream = null;
            g.zx(bufferedOutputStream);
            g.zx(inputStream);
            g.zx(closeable);
            g.zy(tu);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "close connection");
            throw th;
        }
    }

    private HttpsURLConnection tu(String str, String str2) {
        HttpsURLConnection httpsURLConnection;
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "createConnection");
        URL url = new URL(str);
        if (this.ha) {
            Proxy tw = tw();
            if (tw == null) {
                return null;
            }
            httpsURLConnection = (HttpsURLConnection) url.openConnection(tw);
        } else {
            httpsURLConnection = (HttpsURLConnection) url.openConnection();
        }
        httpsURLConnection.setSSLSocketFactory(new com.huawei.secure.android.common.a(com.huawei.android.pushagent.utils.c.ye(), ""));
        httpsURLConnection.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
        httpsURLConnection.setRequestMethod(str2);
        httpsURLConnection.setConnectTimeout(this.gw);
        httpsURLConnection.setReadTimeout(this.gz);
        httpsURLConnection.setDoOutput(true);
        httpsURLConnection.setDoInput(true);
        httpsURLConnection.setRequestProperty("Content-type", "json/text; charset=UTF-8");
        return httpsURLConnection;
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0028  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Proxy tw() {
        String property;
        int parseInt;
        Exception e;
        if (1 == d.yh(this.appCtx)) {
            return null;
        }
        try {
            if (VERSION.SDK_INT >= 11) {
                property = System.getProperty("http.proxyHost");
                try {
                    String property2 = System.getProperty("http.proxyPort");
                    if (property2 == null) {
                        property2 = "-1";
                    }
                    parseInt = Integer.parseInt(property2);
                } catch (Exception e2) {
                    e = e2;
                }
                if (property != null || property.length() <= 0 || parseInt == -1) {
                    return null;
                }
                com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "use Proxy " + property + ":" + parseInt);
                return new Proxy(Type.HTTP, new InetSocketAddress(property, parseInt));
            }
            property = android.net.Proxy.getHost(this.appCtx);
            parseInt = android.net.Proxy.getPort(this.appCtx);
            if (property != null) {
            }
            return null;
        } catch (Exception e3) {
            e = e3;
            property = null;
        }
        com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "get proxy ip or port error:" + e.getMessage());
        parseInt = -1;
        if (property != null) {
        }
        return null;
    }
}
