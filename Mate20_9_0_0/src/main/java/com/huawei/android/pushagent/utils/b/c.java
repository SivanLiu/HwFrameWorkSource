package com.huawei.android.pushagent.utils.b;

import android.content.Context;
import android.os.Build.VERSION;
import com.huawei.android.pushagent.constant.HttpMethod;
import com.huawei.android.pushagent.utils.a.f;
import com.huawei.android.pushagent.utils.b;
import com.huawei.android.pushagent.utils.d;
import com.huawei.android.pushagent.utils.g;
import com.huawei.secure.android.common.a;
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

public class c {
    private Context appCtx;
    private String o;
    private int p = 30000;
    private int q = 0;
    private List<String> r;
    private int s = 30000;
    private boolean t = false;

    public c(Context context, String str, List<String> list) {
        this.appCtx = context.getApplicationContext();
        this.o = str;
        this.r = list;
    }

    public void ap(int i) {
        this.q = i;
    }

    public void ao(boolean z) {
        this.t = z;
    }

    public void am(int i) {
        this.p = i;
    }

    public void an(int i) {
        this.s = i;
    }

    public String aq(String str, HttpMethod httpMethod) {
        Closeable inputStream;
        Closeable closeable;
        Throwable th;
        if (str == null) {
            return null;
        }
        String ah = new b().ak(this.o).ai(this.r).al(this.q).aj().ah();
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "execute url is: " + f.t(ah));
        int i = -1;
        HttpURLConnection ar;
        Closeable bufferedOutputStream;
        try {
            ar = ar(ah, httpMethod.abk());
            if (ar == null) {
                b.ff(null);
                b.ff(null);
                b.ff(null);
                b.fg(ar);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                return null;
            }
            try {
                bufferedOutputStream = new BufferedOutputStream(ar.getOutputStream());
                try {
                    bufferedOutputStream.write(str.getBytes("UTF-8"));
                    bufferedOutputStream.flush();
                    i = ar.getResponseCode();
                    inputStream = ar.getInputStream();
                } catch (IOException e) {
                    closeable = null;
                    inputStream = null;
                } catch (RuntimeException e2) {
                    closeable = null;
                    inputStream = null;
                    com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter RuntimeException - http code:" + i);
                    b.ff(bufferedOutputStream);
                    b.ff(inputStream);
                    b.ff(closeable);
                    b.fg(ar);
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                    return null;
                } catch (Exception e3) {
                    closeable = null;
                    inputStream = null;
                    try {
                        com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter unknown exception - http code:" + i);
                        b.ff(bufferedOutputStream);
                        b.ff(inputStream);
                        b.ff(closeable);
                        b.fg(ar);
                        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                        return null;
                    } catch (Throwable th2) {
                        th = th2;
                        b.ff(bufferedOutputStream);
                        b.ff(inputStream);
                        b.ff(closeable);
                        b.fg(ar);
                        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    closeable = null;
                    inputStream = null;
                    b.ff(bufferedOutputStream);
                    b.ff(inputStream);
                    b.ff(closeable);
                    b.fg(ar);
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                    throw th;
                }
            } catch (IOException e4) {
                closeable = null;
                inputStream = null;
                bufferedOutputStream = null;
                com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter IOException - http code:" + i);
                b.ff(bufferedOutputStream);
                b.ff(inputStream);
                b.ff(closeable);
                b.fg(ar);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                return null;
            } catch (RuntimeException e5) {
                closeable = null;
                inputStream = null;
                bufferedOutputStream = null;
                com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter RuntimeException - http code:" + i);
                b.ff(bufferedOutputStream);
                b.ff(inputStream);
                b.ff(closeable);
                b.fg(ar);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                return null;
            } catch (Exception e6) {
                closeable = null;
                inputStream = null;
                bufferedOutputStream = null;
                com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter unknown exception - http code:" + i);
                b.ff(bufferedOutputStream);
                b.ff(inputStream);
                b.ff(closeable);
                b.fg(ar);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                return null;
            } catch (Throwable th4) {
                th = th4;
                closeable = null;
                inputStream = null;
                bufferedOutputStream = null;
                b.ff(bufferedOutputStream);
                b.ff(inputStream);
                b.ff(closeable);
                b.fg(ar);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                throw th;
            }
            try {
                closeable = new BufferedInputStream(inputStream);
                try {
                    String fi = b.fi(closeable);
                    b.ff(bufferedOutputStream);
                    b.ff(inputStream);
                    b.ff(closeable);
                    b.fg(ar);
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                    return fi;
                } catch (IOException e7) {
                    com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter IOException - http code:" + i);
                    b.ff(bufferedOutputStream);
                    b.ff(inputStream);
                    b.ff(closeable);
                    b.fg(ar);
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                    return null;
                } catch (RuntimeException e8) {
                    com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter RuntimeException - http code:" + i);
                    b.ff(bufferedOutputStream);
                    b.ff(inputStream);
                    b.ff(closeable);
                    b.fg(ar);
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                    return null;
                } catch (Exception e9) {
                    com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter unknown exception - http code:" + i);
                    b.ff(bufferedOutputStream);
                    b.ff(inputStream);
                    b.ff(closeable);
                    b.fg(ar);
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                    return null;
                }
            } catch (IOException e10) {
                closeable = null;
                com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter IOException - http code:" + i);
                b.ff(bufferedOutputStream);
                b.ff(inputStream);
                b.ff(closeable);
                b.fg(ar);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                return null;
            } catch (RuntimeException e11) {
                closeable = null;
                com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter RuntimeException - http code:" + i);
                b.ff(bufferedOutputStream);
                b.ff(inputStream);
                b.ff(closeable);
                b.fg(ar);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                return null;
            } catch (Exception e12) {
                closeable = null;
                com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter unknown exception - http code:" + i);
                b.ff(bufferedOutputStream);
                b.ff(inputStream);
                b.ff(closeable);
                b.fg(ar);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                return null;
            } catch (Throwable th5) {
                th = th5;
                closeable = null;
                b.ff(bufferedOutputStream);
                b.ff(inputStream);
                b.ff(closeable);
                b.fg(ar);
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
                throw th;
            }
        } catch (IOException e13) {
            ar = null;
            closeable = null;
            inputStream = null;
            bufferedOutputStream = null;
        } catch (RuntimeException e14) {
            ar = null;
            closeable = null;
            inputStream = null;
            bufferedOutputStream = null;
            com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter RuntimeException - http code:" + i);
            b.ff(bufferedOutputStream);
            b.ff(inputStream);
            b.ff(closeable);
            b.fg(ar);
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
            return null;
        } catch (Exception e15) {
            ar = null;
            closeable = null;
            inputStream = null;
            bufferedOutputStream = null;
            com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "http execute encounter unknown exception - http code:" + i);
            b.ff(bufferedOutputStream);
            b.ff(inputStream);
            b.ff(closeable);
            b.fg(ar);
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
            return null;
        } catch (Throwable th6) {
            th = th6;
            ar = null;
            closeable = null;
            inputStream = null;
            bufferedOutputStream = null;
            b.ff(bufferedOutputStream);
            b.ff(inputStream);
            b.ff(closeable);
            b.fg(ar);
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "close connection");
            throw th;
        }
    }

    private HttpsURLConnection ar(String str, String str2) {
        HttpsURLConnection httpsURLConnection;
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "createConnection");
        URL url = new URL(str);
        if (this.t) {
            Proxy as = as();
            if (as == null) {
                return null;
            }
            httpsURLConnection = (HttpsURLConnection) url.openConnection(as);
        } else {
            httpsURLConnection = (HttpsURLConnection) url.openConnection();
        }
        httpsURLConnection.setSSLSocketFactory(new a(d.fm(), ""));
        httpsURLConnection.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
        httpsURLConnection.setRequestMethod(str2);
        httpsURLConnection.setConnectTimeout(this.p);
        httpsURLConnection.setReadTimeout(this.s);
        httpsURLConnection.setDoOutput(true);
        httpsURLConnection.setDoInput(true);
        httpsURLConnection.setRequestProperty("Content-type", "json/text; charset=UTF-8");
        return httpsURLConnection;
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:0x0028  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Proxy as() {
        String property;
        int parseInt;
        Exception e;
        if (1 == g.fw(this.appCtx)) {
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
                com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "use Proxy " + property + ":" + parseInt);
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
        com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "get proxy ip or port error:" + e.getMessage());
        parseInt = -1;
        if (property != null) {
        }
        return null;
    }
}
