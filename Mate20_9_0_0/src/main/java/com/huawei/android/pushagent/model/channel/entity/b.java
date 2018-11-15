package com.huawei.android.pushagent.model.channel.entity;

import android.content.Context;
import android.net.Proxy;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.text.TextUtils;
import com.huawei.android.pushagent.datatype.a.a;
import com.huawei.android.pushagent.datatype.exception.PushException;
import com.huawei.android.pushagent.datatype.exception.PushException.ErrorType;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.a.f;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class b extends Thread {
    protected a dx = null;
    protected Context dy = null;
    private a dz = null;

    protected abstract void lr();

    public b(a aVar) {
        super("SocketRead_" + new SimpleDateFormat("HH:mm:ss").format(new Date()));
        this.dx = aVar;
        this.dy = aVar.di;
        this.dz = aVar.dh;
    }

    public void run() {
        long currentTimeMillis = System.currentTimeMillis();
        Bundle bundle = new Bundle();
        try {
            if (mr()) {
                currentTimeMillis = System.currentTimeMillis();
                lr();
            }
            c.er("PushLog3413", "normal to quit.");
            c.ep("PushLog3413", "total connect Time:" + (System.currentTimeMillis() - currentTimeMillis) + " process quit, so close socket");
            if (this.dx.df != null) {
                try {
                    this.dx.df.nb();
                } catch (Exception e) {
                    c.eq("PushLog3413", e.toString());
                }
            }
        } catch (Throwable e2) {
            c.es("PushLog3413", "connect occurs :" + e2.toString(), e2);
            Serializable serializable = e2.type;
            if (serializable != null) {
                bundle.putSerializable("errorType", serializable);
            }
            c.ep("PushLog3413", "total connect Time:" + (System.currentTimeMillis() - currentTimeMillis) + " process quit, so close socket");
            if (this.dx.df != null) {
                try {
                    this.dx.df.nb();
                } catch (Exception e3) {
                    c.eq("PushLog3413", e3.toString());
                }
            }
        } catch (Throwable e22) {
            c.es("PushLog3413", "connect cause :" + e22.toString(), e22);
            c.ep("PushLog3413", "total connect Time:" + (System.currentTimeMillis() - currentTimeMillis) + " process quit, so close socket");
            if (this.dx.df != null) {
                try {
                    this.dx.df.nb();
                } catch (Exception e32) {
                    c.eq("PushLog3413", e32.toString());
                }
            }
        } catch (Throwable th) {
            c.ep("PushLog3413", "total connect Time:" + (System.currentTimeMillis() - currentTimeMillis) + " process quit, so close socket");
            if (this.dx.df != null) {
                try {
                    this.dx.df.nb();
                } catch (Exception e4) {
                    c.eq("PushLog3413", e4.toString());
                }
            }
        }
        c.ep("PushLog3413", "connect thread exit!");
        mu(SocketReadThread$SocketEvent.SocketEvent_CLOSE, bundle);
    }

    private boolean mr() {
        Socket socket = null;
        try {
            c.er("PushLog3413", "start to create socket");
            long currentTimeMillis = System.currentTimeMillis();
            if (this.dz == null || this.dz.ka() == null || this.dz.ka().length() == 0) {
                c.eq("PushLog3413", "the addr is " + this.dz + " is invalid");
                return false;
            }
            socket = ms(this.dz.ka(), this.dz.kb(), this.dz.kc());
            if (socket == null) {
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            }
            this.dx.df = new com.huawei.android.pushagent.model.channel.a.a(this.dy);
            if (this.dx.df.ni(socket)) {
                socket.setSoTimeout(0);
                c.ep("PushLog3413", "connect success cost " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
                if (this.dx.df.nh()) {
                    com.huawei.android.pushagent.a.a.hx(53);
                    this.dx.lx(SocketReadThread$SocketEvent.SocketEvent_CONNECTED, new Bundle());
                    return true;
                }
                c.eq("PushLog3413", "Socket connect failed");
                throw new PushException("create SSLSocket failed", ErrorType.Err_Connect);
            }
            com.huawei.android.pushagent.a.a.hx(52);
            c.eq("PushLog3413", "call connectMode.protocol.init failed!!");
            socket.close();
            throw new PushException("init socket error", ErrorType.Err_Connect);
        } catch (Throwable e) {
            c.eq("PushLog3413", "call connectSync cause:" + g.gg(e));
            if (socket != null) {
                try {
                    socket.close();
                } catch (Throwable e2) {
                    c.et("PushLog3413", "close socket cause:" + e2.toString(), e2);
                }
            }
            throw new PushException(e, ErrorType.Err_Connect);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter: B:30:0x00f2} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter: B:30:0x00f2} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter: B:30:0x00f2} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter: B:30:0x00f2} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter: B:30:0x00f2} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter: B:30:0x00f2} */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x00f2 A:{SYNTHETIC, Splitter: B:30:0x00f2} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private Socket ms(String str, int i, boolean z) {
        Throwable e;
        boolean z2 = true;
        Socket socket;
        try {
            socket = new Socket();
            try {
                Object property;
                int parseInt;
                if (this instanceof com.huawei.android.pushagent.model.channel.entity.a.b) {
                    if (com.huawei.android.pushagent.utils.tools.c.cr()) {
                        c.ep("PushLog3413", "isSupportCtrlSocketV2, ctrlSocket");
                        com.huawei.android.pushagent.utils.tools.c.ct(1, g.gh(socket));
                    } else {
                        g.ge(1, g.gh(socket));
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
                    property = Proxy.getHost(this.dy);
                    parseInt = Proxy.getPort(this.dy);
                }
                int fw = g.fw(this.dy);
                mu(SocketReadThread$SocketEvent.SocketEvent_CONNECTING, new Bundle());
                c.ep("PushLog3413", "enter createSocket, ip is: " + f.t(str));
                if (TextUtils.isEmpty(property) || -1 == parseInt) {
                    z2 = false;
                } else if (1 == fw) {
                    z2 = false;
                }
                boolean sc = k.rh(this.dy).sc();
                c.ep("PushLog3413", "useProxy is valid:" + z2 + ", allow proxy:" + sc);
                if (z && z2 && sc) {
                    mt(socket, str, i, property, parseInt);
                } else {
                    c.ep("PushLog3413", "create socket without proxy");
                    socket.connect(new InetSocketAddress(str, i), ((int) k.rh(this.dy).rw()) * 1000);
                }
                socket.setSoTimeout(((int) k.rh(this.dy).rx()) * 1000);
                return socket;
            } catch (UnsupportedEncodingException e2) {
                e = e2;
                c.es("PushLog3413", "call getBytes cause:" + e.toString(), e);
                com.huawei.android.pushagent.a.a.hx(50);
                if (socket != null) {
                }
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            } catch (SocketException e3) {
                e = e3;
                c.eq("PushLog3413", "call connectSync cause SocketException :" + g.gg(e));
                com.huawei.android.pushagent.a.a.hx(50);
                if (socket != null) {
                }
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            } catch (IOException e4) {
                e = e4;
                c.eq("PushLog3413", "call connectSync cause IOException:" + g.gg(e));
                com.huawei.android.pushagent.a.a.hx(50);
                if (socket != null) {
                }
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            } catch (Exception e5) {
                e = e5;
                c.eq("PushLog3413", "call connectSync cause Exception:" + g.gg(e));
                com.huawei.android.pushagent.a.a.hx(50);
                if (socket != null) {
                }
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            }
        } catch (UnsupportedEncodingException e6) {
            e = e6;
            socket = null;
            c.es("PushLog3413", "call getBytes cause:" + e.toString(), e);
            com.huawei.android.pushagent.a.a.hx(50);
            if (socket != null) {
                try {
                    socket.close();
                } catch (Throwable e7) {
                    c.eq("PushLog3413", "call socket.close cause IOException:" + g.gg(e7));
                }
            }
            throw new PushException("create socket failed", ErrorType.Err_Connect);
        } catch (SocketException e8) {
            e7 = e8;
            socket = null;
            c.eq("PushLog3413", "call connectSync cause SocketException :" + g.gg(e7));
            com.huawei.android.pushagent.a.a.hx(50);
            if (socket != null) {
            }
            throw new PushException("create socket failed", ErrorType.Err_Connect);
        } catch (IOException e9) {
            e7 = e9;
            socket = null;
            c.eq("PushLog3413", "call connectSync cause IOException:" + g.gg(e7));
            com.huawei.android.pushagent.a.a.hx(50);
            if (socket != null) {
            }
            throw new PushException("create socket failed", ErrorType.Err_Connect);
        } catch (Exception e10) {
            e7 = e10;
            socket = null;
            c.eq("PushLog3413", "call connectSync cause Exception:" + g.gg(e7));
            com.huawei.android.pushagent.a.a.hx(50);
            if (socket != null) {
            }
            throw new PushException("create socket failed", ErrorType.Err_Connect);
        }
    }

    private void mt(Socket socket, String str, int i, String str2, int i2) {
        if (socket == null) {
            c.eq("PushLog3413", "socket is null");
            return;
        }
        c.ep("PushLog3413", "use Proxy " + str2 + ":" + i2 + " to connect to push server.");
        socket.connect(new InetSocketAddress(str2, i2), ((int) k.rh(this.dy).rw()) * 1000);
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
        str3 = com.huawei.android.pushagent.utils.b.fh(new BufferedReader(new StringReader(stringBuilder.toString())));
        if (str3 != null) {
            c.er("PushLog3413", "read data:" + f.t(str3));
        }
    }

    private void mu(SocketReadThread$SocketEvent socketReadThread$SocketEvent, Bundle bundle) {
        this.dx.lx(socketReadThread$SocketEvent, bundle);
    }
}
