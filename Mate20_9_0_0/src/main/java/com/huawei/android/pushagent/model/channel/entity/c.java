package com.huawei.android.pushagent.model.channel.entity;

import android.content.Context;
import android.net.Proxy;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.exception.PushException;
import com.huawei.android.pushagent.datatype.exception.PushException.ErrorType;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.d;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.tools.b;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class c extends Thread {
    private com.huawei.android.pushagent.datatype.a.c bg = null;
    protected b bh = null;
    protected Context bi = null;

    protected abstract void ci();

    public c(b bVar) {
        super("SocketRead_" + new SimpleDateFormat("HH:mm:ss").format(new Date()));
        this.bh = bVar;
        this.bi = bVar.ap;
        this.bg = bVar.ao;
    }

    public void run() {
        long currentTimeMillis = System.currentTimeMillis();
        Bundle bundle = new Bundle();
        try {
            if (dd()) {
                currentTimeMillis = System.currentTimeMillis();
                ci();
            }
            a.st("PushLog3414", "normal to quit.");
            a.sv("PushLog3414", "total connect Time:" + (System.currentTimeMillis() - currentTimeMillis) + " process quit, so close socket");
            if (this.bh.az != null) {
                try {
                    this.bh.az.di();
                } catch (Exception e) {
                    a.su("PushLog3414", e.toString());
                }
            }
        } catch (PushException e2) {
            a.sw("PushLog3414", "connect occurs :" + e2.toString(), e2);
            ErrorType errorType = e2.type;
            if (errorType != null) {
                bundle.putSerializable("errorType", errorType);
            }
            a.sv("PushLog3414", "total connect Time:" + (System.currentTimeMillis() - currentTimeMillis) + " process quit, so close socket");
            if (this.bh.az != null) {
                try {
                    this.bh.az.di();
                } catch (Exception e3) {
                    a.su("PushLog3414", e3.toString());
                }
            }
        } catch (Exception e32) {
            a.sw("PushLog3414", "connect cause :" + e32.toString(), e32);
            a.sv("PushLog3414", "total connect Time:" + (System.currentTimeMillis() - currentTimeMillis) + " process quit, so close socket");
            if (this.bh.az != null) {
                try {
                    this.bh.az.di();
                } catch (Exception e322) {
                    a.su("PushLog3414", e322.toString());
                }
            }
        } catch (Throwable th) {
            a.sv("PushLog3414", "total connect Time:" + (System.currentTimeMillis() - currentTimeMillis) + " process quit, so close socket");
            if (this.bh.az != null) {
                try {
                    this.bh.az.di();
                } catch (Exception e4) {
                    a.su("PushLog3414", e4.toString());
                }
            }
        }
        a.sv("PushLog3414", "connect thread exit!");
        dg(SocketReadThread$SocketEvent.SocketEvent_CLOSE, bundle);
    }

    private boolean dd() {
        Socket socket = null;
        try {
            a.st("PushLog3414", "start to create socket");
            long currentTimeMillis = System.currentTimeMillis();
            if (this.bg == null || this.bg.au() == null || this.bg.au().length() == 0) {
                a.su("PushLog3414", "the addr is " + this.bg + " is invalid");
                return false;
            }
            socket = de(this.bg.au(), this.bg.av(), this.bg.aw());
            if (socket == null) {
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            }
            this.bh.az = new com.huawei.android.pushagent.model.channel.a.a(this.bi);
            if (this.bh.az.dp(socket)) {
                socket.setSoTimeout(0);
                a.sv("PushLog3414", "connect success cost " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
                if (this.bh.az.m0do()) {
                    com.huawei.android.pushagent.b.a.abd(53);
                    this.bh.cb(SocketReadThread$SocketEvent.SocketEvent_CONNECTED, new Bundle());
                    return true;
                }
                a.su("PushLog3414", "Socket connect failed");
                throw new PushException("create SSLSocket failed", ErrorType.Err_Connect);
            }
            com.huawei.android.pushagent.b.a.abd(52);
            a.su("PushLog3414", "call connectMode.protocol.init failed!!");
            socket.close();
            throw new PushException("init socket error", ErrorType.Err_Connect);
        } catch (Exception e) {
            a.su("PushLog3414", "call connectSync cause:" + d.ze(e));
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e2) {
                    a.sy("PushLog3414", "close socket cause:" + e2.toString(), e2);
                }
            }
            throw new PushException(e, ErrorType.Err_Connect);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter:B:30:0x00f2} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter:B:30:0x00f2} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter:B:30:0x00f2} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter:B:30:0x00f2} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter:B:30:0x00f2} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter:B:30:0x00f2} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter:B:30:0x00f2} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Socket de(String str, int i, boolean z) {
        UnsupportedEncodingException e;
        Throwable e2;
        boolean z2 = true;
        Socket socket;
        try {
            socket = new Socket();
            try {
                CharSequence property;
                int parseInt;
                if (this instanceof com.huawei.android.pushagent.model.channel.entity.a.c) {
                    if (b.sf()) {
                        a.sv("PushLog3414", "isSupportCtrlSocketV2, ctrlSocket");
                        b.sg(1, d.zf(socket));
                    } else {
                        d.ym(1, d.zf(socket));
                    }
                }
                if (VERSION.SDK_INT >= 11) {
                    property = System.getProperty("http.proxyHost");
                    String property2 = System.getProperty("http.proxyPort");
                    if (property2 == null) {
                        property2 = "-1";
                    }
                    parseInt = Integer.parseInt(property2);
                } else {
                    property = Proxy.getHost(this.bi);
                    parseInt = Proxy.getPort(this.bi);
                }
                int yh = d.yh(this.bi);
                dg(SocketReadThread$SocketEvent.SocketEvent_CONNECTING, new Bundle());
                a.sv("PushLog3414", "enter createSocket, ip is: " + com.huawei.android.pushagent.utils.e.c.wh(str));
                if (TextUtils.isEmpty(property) || -1 == parseInt) {
                    z2 = false;
                } else if (1 == yh) {
                    z2 = false;
                }
                boolean ie = com.huawei.android.pushagent.model.prefs.a.ff(this.bi).ie();
                a.sv("PushLog3414", "useProxy is valid:" + z2 + ", allow proxy:" + ie);
                if (z && z2 && ie) {
                    df(socket, str, i, property, parseInt);
                } else {
                    a.sv("PushLog3414", "create socket without proxy");
                    socket.connect(new InetSocketAddress(str, i), ((int) com.huawei.android.pushagent.model.prefs.a.ff(this.bi).hr()) * 1000);
                }
                socket.setSoTimeout(((int) com.huawei.android.pushagent.model.prefs.a.ff(this.bi).hs()) * 1000);
                return socket;
            } catch (UnsupportedEncodingException e3) {
                e = e3;
                a.sw("PushLog3414", "call getBytes cause:" + e.toString(), e);
                com.huawei.android.pushagent.b.a.abd(50);
                if (socket != null) {
                }
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            } catch (SocketException e4) {
                e2 = e4;
                a.su("PushLog3414", "call connectSync cause SocketException :" + d.ze(e2));
                com.huawei.android.pushagent.b.a.abd(50);
                if (socket != null) {
                }
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            } catch (IOException e5) {
                e2 = e5;
                a.su("PushLog3414", "call connectSync cause IOException:" + d.ze(e2));
                com.huawei.android.pushagent.b.a.abd(50);
                if (socket != null) {
                }
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            } catch (Exception e6) {
                e2 = e6;
                a.su("PushLog3414", "call connectSync cause Exception:" + d.ze(e2));
                com.huawei.android.pushagent.b.a.abd(50);
                if (socket != null) {
                }
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            }
        } catch (UnsupportedEncodingException e7) {
            e = e7;
            socket = null;
            a.sw("PushLog3414", "call getBytes cause:" + e.toString(), e);
            com.huawei.android.pushagent.b.a.abd(50);
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e8) {
                    a.su("PushLog3414", "call socket.close cause IOException:" + d.ze(e8));
                }
            }
            throw new PushException("create socket failed", ErrorType.Err_Connect);
        } catch (SocketException e9) {
            e2 = e9;
            socket = null;
            a.su("PushLog3414", "call connectSync cause SocketException :" + d.ze(e2));
            com.huawei.android.pushagent.b.a.abd(50);
            if (socket != null) {
            }
            throw new PushException("create socket failed", ErrorType.Err_Connect);
        } catch (IOException e10) {
            e2 = e10;
            socket = null;
            a.su("PushLog3414", "call connectSync cause IOException:" + d.ze(e2));
            com.huawei.android.pushagent.b.a.abd(50);
            if (socket != null) {
            }
            throw new PushException("create socket failed", ErrorType.Err_Connect);
        } catch (Exception e11) {
            e2 = e11;
            socket = null;
            a.su("PushLog3414", "call connectSync cause Exception:" + d.ze(e2));
            com.huawei.android.pushagent.b.a.abd(50);
            if (socket != null) {
            }
            throw new PushException("create socket failed", ErrorType.Err_Connect);
        }
    }

    private void df(Socket socket, String str, int i, String str2, int i2) {
        if (socket == null) {
            a.su("PushLog3414", "socket is null");
            return;
        }
        a.sv("PushLog3414", "use Proxy " + str2 + ":" + i2 + " to connect to push server.");
        socket.connect(new InetSocketAddress(str2, i2), ((int) com.huawei.android.pushagent.model.prefs.a.ff(this.bi).hr()) * 1000);
        String str3 = "CONNECT " + str + ":" + i;
        socket.getOutputStream().write((str3 + " HTTP/1.1\r\nHost: " + str3 + "\r\n\r\n").getBytes("UTF-8"));
        InputStream inputStream = socket.getInputStream();
        StringBuilder stringBuilder = new StringBuilder(100);
        int i3 = 0;
        do {
            char read = (char) inputStream.read();
            stringBuilder.append(read);
            if ((i3 == 0 || i3 == 2) && read == 13) {
                i3++;
            } else if ((i3 == 1 || i3 == 3) && read == 10) {
                i3++;
            } else {
                i3 = 0;
            }
        } while (i3 != 4);
        str3 = g.zz(new BufferedReader(new StringReader(stringBuilder.toString())));
        if (str3 != null) {
            a.st("PushLog3414", "read data:" + com.huawei.android.pushagent.utils.e.c.wh(str3));
        }
    }

    private void dg(SocketReadThread$SocketEvent socketReadThread$SocketEvent, Bundle bundle) {
        this.bh.cb(socketReadThread$SocketEvent, bundle);
    }
}
