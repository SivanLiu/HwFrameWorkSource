package com.huawei.android.pushagent.utils.e;

import android.content.Context;
import android.os.Build.VERSION;
import com.huawei.android.pushagent.constant.HttpMethod;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.c;
import com.huawei.android.pushagent.utils.c.g;
import com.huawei.android.pushagent.utils.d;
import com.huawei.android.pushagent.utils.f;
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
    private String aj;
    private int ak = 30000;
    private int al = 0;
    private List<String> am;
    private int an = 30000;
    private boolean ao = false;
    private Context appCtx;

    public a(Context context, String str, List<String> list) {
        this.appCtx = context.getApplicationContext();
        this.aj = str;
        this.am = list;
    }

    public void dg(int i) {
        this.al = i;
    }

    public void di(boolean z) {
        this.ao = z;
    }

    public void df(int i) {
        this.ak = i;
    }

    public void dh(int i) {
        this.an = i;
    }

    public String dd(String str, HttpMethod httpMethod) {
        HttpURLConnection dc;
        Closeable bufferedOutputStream;
        Closeable inputStream;
        Closeable closeable;
        Throwable th;
        if (str == null) {
            return null;
        }
        String dj = new c().dl(this.aj).dm(this.am).dn(this.al).do().dj();
        b.x("PushLog2976", "execute url is: " + g.cc(dj));
        int i = -1;
        try {
            dc = dc(dj, httpMethod.xd());
            if (dc == null) {
                d.fk(null);
                d.fk(null);
                d.fk(null);
                d.fl(dc);
                b.z("PushLog2976", "close connection");
                return null;
            }
            try {
                bufferedOutputStream = new BufferedOutputStream(dc.getOutputStream());
                try {
                    bufferedOutputStream.write(str.getBytes("UTF-8"));
                    bufferedOutputStream.flush();
                    i = dc.getResponseCode();
                    inputStream = dc.getInputStream();
                } catch (IOException e) {
                    closeable = null;
                    inputStream = null;
                    b.ab("PushLog2976", "http execute encounter IOException - http code:" + i);
                    d.fk(bufferedOutputStream);
                    d.fk(inputStream);
                    d.fk(closeable);
                    d.fl(dc);
                    b.z("PushLog2976", "close connection");
                    return null;
                } catch (RuntimeException e2) {
                    closeable = null;
                    inputStream = null;
                    b.ab("PushLog2976", "http execute encounter RuntimeException - http code:" + i);
                    d.fk(bufferedOutputStream);
                    d.fk(inputStream);
                    d.fk(closeable);
                    d.fl(dc);
                    b.z("PushLog2976", "close connection");
                    return null;
                } catch (Exception e3) {
                    closeable = null;
                    inputStream = null;
                    try {
                        b.ab("PushLog2976", "http execute encounter unknown exception - http code:" + i);
                        d.fk(bufferedOutputStream);
                        d.fk(inputStream);
                        d.fk(closeable);
                        d.fl(dc);
                        b.z("PushLog2976", "close connection");
                        return null;
                    } catch (Throwable th2) {
                        th = th2;
                        d.fk(bufferedOutputStream);
                        d.fk(inputStream);
                        d.fk(closeable);
                        d.fl(dc);
                        b.z("PushLog2976", "close connection");
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    closeable = null;
                    inputStream = null;
                    d.fk(bufferedOutputStream);
                    d.fk(inputStream);
                    d.fk(closeable);
                    d.fl(dc);
                    b.z("PushLog2976", "close connection");
                    throw th;
                }
            } catch (IOException e4) {
                closeable = null;
                inputStream = null;
                bufferedOutputStream = null;
                b.ab("PushLog2976", "http execute encounter IOException - http code:" + i);
                d.fk(bufferedOutputStream);
                d.fk(inputStream);
                d.fk(closeable);
                d.fl(dc);
                b.z("PushLog2976", "close connection");
                return null;
            } catch (RuntimeException e5) {
                closeable = null;
                inputStream = null;
                bufferedOutputStream = null;
                b.ab("PushLog2976", "http execute encounter RuntimeException - http code:" + i);
                d.fk(bufferedOutputStream);
                d.fk(inputStream);
                d.fk(closeable);
                d.fl(dc);
                b.z("PushLog2976", "close connection");
                return null;
            } catch (Exception e6) {
                closeable = null;
                inputStream = null;
                bufferedOutputStream = null;
                b.ab("PushLog2976", "http execute encounter unknown exception - http code:" + i);
                d.fk(bufferedOutputStream);
                d.fk(inputStream);
                d.fk(closeable);
                d.fl(dc);
                b.z("PushLog2976", "close connection");
                return null;
            } catch (Throwable th4) {
                th = th4;
                closeable = null;
                inputStream = null;
                bufferedOutputStream = null;
                d.fk(bufferedOutputStream);
                d.fk(inputStream);
                d.fk(closeable);
                d.fl(dc);
                b.z("PushLog2976", "close connection");
                throw th;
            }
            try {
                closeable = new BufferedInputStream(inputStream);
                try {
                    String fj = d.fj(closeable);
                    d.fk(bufferedOutputStream);
                    d.fk(inputStream);
                    d.fk(closeable);
                    d.fl(dc);
                    b.z("PushLog2976", "close connection");
                    return fj;
                } catch (IOException e7) {
                    b.ab("PushLog2976", "http execute encounter IOException - http code:" + i);
                    d.fk(bufferedOutputStream);
                    d.fk(inputStream);
                    d.fk(closeable);
                    d.fl(dc);
                    b.z("PushLog2976", "close connection");
                    return null;
                } catch (RuntimeException e8) {
                    b.ab("PushLog2976", "http execute encounter RuntimeException - http code:" + i);
                    d.fk(bufferedOutputStream);
                    d.fk(inputStream);
                    d.fk(closeable);
                    d.fl(dc);
                    b.z("PushLog2976", "close connection");
                    return null;
                } catch (Exception e9) {
                    b.ab("PushLog2976", "http execute encounter unknown exception - http code:" + i);
                    d.fk(bufferedOutputStream);
                    d.fk(inputStream);
                    d.fk(closeable);
                    d.fl(dc);
                    b.z("PushLog2976", "close connection");
                    return null;
                }
            } catch (IOException e10) {
                closeable = null;
                b.ab("PushLog2976", "http execute encounter IOException - http code:" + i);
                d.fk(bufferedOutputStream);
                d.fk(inputStream);
                d.fk(closeable);
                d.fl(dc);
                b.z("PushLog2976", "close connection");
                return null;
            } catch (RuntimeException e11) {
                closeable = null;
                b.ab("PushLog2976", "http execute encounter RuntimeException - http code:" + i);
                d.fk(bufferedOutputStream);
                d.fk(inputStream);
                d.fk(closeable);
                d.fl(dc);
                b.z("PushLog2976", "close connection");
                return null;
            } catch (Exception e12) {
                closeable = null;
                b.ab("PushLog2976", "http execute encounter unknown exception - http code:" + i);
                d.fk(bufferedOutputStream);
                d.fk(inputStream);
                d.fk(closeable);
                d.fl(dc);
                b.z("PushLog2976", "close connection");
                return null;
            } catch (Throwable th5) {
                th = th5;
                closeable = null;
                d.fk(bufferedOutputStream);
                d.fk(inputStream);
                d.fk(closeable);
                d.fl(dc);
                b.z("PushLog2976", "close connection");
                throw th;
            }
        } catch (IOException e13) {
            dc = null;
            closeable = null;
            inputStream = null;
            bufferedOutputStream = null;
            b.ab("PushLog2976", "http execute encounter IOException - http code:" + i);
            d.fk(bufferedOutputStream);
            d.fk(inputStream);
            d.fk(closeable);
            d.fl(dc);
            b.z("PushLog2976", "close connection");
            return null;
        } catch (RuntimeException e14) {
            dc = null;
            closeable = null;
            inputStream = null;
            bufferedOutputStream = null;
            b.ab("PushLog2976", "http execute encounter RuntimeException - http code:" + i);
            d.fk(bufferedOutputStream);
            d.fk(inputStream);
            d.fk(closeable);
            d.fl(dc);
            b.z("PushLog2976", "close connection");
            return null;
        } catch (Exception e15) {
            dc = null;
            closeable = null;
            inputStream = null;
            bufferedOutputStream = null;
            b.ab("PushLog2976", "http execute encounter unknown exception - http code:" + i);
            d.fk(bufferedOutputStream);
            d.fk(inputStream);
            d.fk(closeable);
            d.fl(dc);
            b.z("PushLog2976", "close connection");
            return null;
        } catch (Throwable th6) {
            th = th6;
            dc = null;
            closeable = null;
            inputStream = null;
            bufferedOutputStream = null;
            d.fk(bufferedOutputStream);
            d.fk(inputStream);
            d.fk(closeable);
            d.fl(dc);
            b.z("PushLog2976", "close connection");
            throw th;
        }
    }

    private HttpsURLConnection dc(String str, String str2) {
        HttpsURLConnection httpsURLConnection;
        b.z("PushLog2976", "createConnection");
        URL url = new URL(str);
        if (this.ao) {
            Proxy de = de();
            if (de == null) {
                return null;
            }
            httpsURLConnection = (HttpsURLConnection) url.openConnection(de);
        } else {
            httpsURLConnection = (HttpsURLConnection) url.openConnection();
        }
        httpsURLConnection.setSSLSocketFactory(new com.huawei.secure.android.common.a(c.fi(), ""));
        httpsURLConnection.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
        httpsURLConnection.setRequestMethod(str2);
        httpsURLConnection.setConnectTimeout(this.ak);
        httpsURLConnection.setReadTimeout(this.an);
        httpsURLConnection.setDoOutput(true);
        httpsURLConnection.setDoInput(true);
        httpsURLConnection.setRequestProperty("Content-type", "json/text; charset=UTF-8");
        return httpsURLConnection;
    }

    private Proxy de() {
        Exception e;
        if (1 == f.fp(this.appCtx)) {
            return null;
        }
        String property;
        int parseInt;
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
                    b.y("PushLog2976", "get proxy ip or port error:" + e.getMessage());
                    parseInt = -1;
                    if (property != null) {
                    }
                    return null;
                }
                if (property != null || property.length() <= 0 || parseInt == -1) {
                    return null;
                }
                b.x("PushLog2976", "use Proxy " + property + ":" + parseInt);
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
            b.y("PushLog2976", "get proxy ip or port error:" + e.getMessage());
            parseInt = -1;
            if (property != null) {
            }
            return null;
        }
    }
}
