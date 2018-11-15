package com.huawei.android.pushagent.model.channel.entity;

import android.content.Context;
import android.net.Proxy;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.text.TextUtils;
import com.huawei.android.pushagent.b.a;
import com.huawei.android.pushagent.datatype.b.d;
import com.huawei.android.pushagent.datatype.exception.PushException;
import com.huawei.android.pushagent.datatype.exception.PushException.ErrorType;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.c.g;
import com.huawei.android.pushagent.utils.f;
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
    protected a gd = null;
    protected Context ge = null;
    private d gf = null;

    protected abstract void ud();

    public b(a aVar) {
        super("SocketRead_" + new SimpleDateFormat("HH:mm:ss").format(new Date()));
        this.gd = aVar;
        this.ge = aVar.fo;
        this.gf = aVar.fn;
    }

    public void run() {
        long currentTimeMillis = System.currentTimeMillis();
        Bundle bundle = new Bundle();
        try {
            if (vr()) {
                currentTimeMillis = System.currentTimeMillis();
                ud();
            }
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "normal to quit.");
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "total connect Time:" + (System.currentTimeMillis() - currentTimeMillis) + " process quit, so close socket");
            if (this.gd.fl != null) {
                try {
                    this.gd.fl.wc();
                } catch (Exception e) {
                    com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e.toString());
                }
            }
        } catch (Throwable e2) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "connect occurs :" + e2.toString(), e2);
            Serializable serializable = e2.type;
            if (serializable != null) {
                bundle.putSerializable("errorType", serializable);
            }
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "total connect Time:" + (System.currentTimeMillis() - currentTimeMillis) + " process quit, so close socket");
            if (this.gd.fl != null) {
                try {
                    this.gd.fl.wc();
                } catch (Exception e3) {
                    com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e3.toString());
                }
            }
        } catch (Throwable e22) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "connect cause :" + e22.toString(), e22);
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "total connect Time:" + (System.currentTimeMillis() - currentTimeMillis) + " process quit, so close socket");
            if (this.gd.fl != null) {
                try {
                    this.gd.fl.wc();
                } catch (Exception e32) {
                    com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e32.toString());
                }
            }
        } catch (Throwable th) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "total connect Time:" + (System.currentTimeMillis() - currentTimeMillis) + " process quit, so close socket");
            if (this.gd.fl != null) {
                try {
                    this.gd.fl.wc();
                } catch (Exception e4) {
                    com.huawei.android.pushagent.utils.a.b.y("PushLog2976", e4.toString());
                }
            }
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "connect thread exit!");
        vu(SocketReadThread$SocketEvent.SocketEvent_CLOSE, bundle);
    }

    private boolean vr() {
        Socket socket = null;
        try {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "start to create socket");
            long currentTimeMillis = System.currentTimeMillis();
            if (this.gf == null || this.gf.yj() == null || this.gf.yj().length() == 0) {
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "the addr is " + this.gf + " is invalid");
                return false;
            }
            socket = vs(this.gf.yj(), this.gf.yk(), this.gf.yl());
            if (socket == null) {
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            }
            this.gd.fl = new com.huawei.android.pushagent.model.channel.a.b(this.ge);
            if (this.gd.fl.we(socket)) {
                socket.setSoTimeout(0);
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "connect success cost " + (System.currentTimeMillis() - currentTimeMillis) + " ms");
                if (this.gd.fl.wb()) {
                    a.aak(53);
                    this.gd.uy(SocketReadThread$SocketEvent.SocketEvent_CONNECTED, new Bundle());
                    return true;
                }
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "Socket connect failed");
                throw new PushException("create SSLSocket failed", ErrorType.Err_Connect);
            }
            a.aak(52);
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "call connectMode.protocol.init failed!!");
            socket.close();
            throw new PushException("init socket error", ErrorType.Err_Connect);
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "call connectSync cause:" + f.go(e));
            if (socket != null) {
                try {
                    socket.close();
                } catch (Throwable e2) {
                    com.huawei.android.pushagent.utils.a.b.ae("PushLog2976", "close socket cause:" + e2.toString(), e2);
                }
            }
            throw new PushException(e, ErrorType.Err_Connect);
        }
    }

    private Socket vs(String str, int i, boolean z) {
        Throwable e;
        boolean z2 = true;
        Socket socket;
        try {
            socket = new Socket();
            try {
                Object property;
                int parseInt;
                if (this instanceof com.huawei.android.pushagent.model.channel.entity.a.a) {
                    if (com.huawei.android.pushagent.utils.tools.a.j()) {
                        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "isSupportCtrlSocketV2, ctrlSocket");
                        com.huawei.android.pushagent.utils.tools.a.g(1, f.gp(socket));
                    } else {
                        f.fv(1, f.gp(socket));
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
                    property = Proxy.getHost(this.ge);
                    parseInt = Proxy.getPort(this.ge);
                }
                int fp = f.fp(this.ge);
                vu(SocketReadThread$SocketEvent.SocketEvent_CONNECTING, new Bundle());
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "enter createSocket, ip is: " + g.cc(str));
                if (TextUtils.isEmpty(property) || -1 == parseInt) {
                    z2 = false;
                } else if (1 == fp) {
                    z2 = false;
                }
                boolean ol = i.mj(this.ge).ol();
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "useProxy is valid:" + z2 + ", allow proxy:" + ol);
                if (z && z2 && ol) {
                    vt(socket, str, i, property, parseInt);
                } else {
                    com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "create socket without proxy");
                    socket.connect(new InetSocketAddress(str, i), ((int) i.mj(this.ge).om()) * 1000);
                }
                socket.setSoTimeout(((int) i.mj(this.ge).on()) * 1000);
                return socket;
            } catch (UnsupportedEncodingException e2) {
                e = e2;
                com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "call getBytes cause:" + e.toString(), e);
                a.aak(50);
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (Throwable e3) {
                        com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "call socket.close cause IOException:" + f.go(e3));
                    }
                }
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            } catch (SocketException e4) {
                e3 = e4;
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "call connectSync cause SocketException :" + f.go(e3));
                a.aak(50);
                if (socket != null) {
                    socket.close();
                }
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            } catch (IOException e5) {
                e3 = e5;
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "call connectSync cause IOException:" + f.go(e3));
                a.aak(50);
                if (socket != null) {
                    socket.close();
                }
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            } catch (Exception e6) {
                e3 = e6;
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "call connectSync cause Exception:" + f.go(e3));
                a.aak(50);
                if (socket != null) {
                    socket.close();
                }
                throw new PushException("create socket failed", ErrorType.Err_Connect);
            }
        } catch (UnsupportedEncodingException e7) {
            e3 = e7;
            socket = null;
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "call getBytes cause:" + e3.toString(), e3);
            a.aak(50);
            if (socket != null) {
                socket.close();
            }
            throw new PushException("create socket failed", ErrorType.Err_Connect);
        } catch (SocketException e8) {
            e3 = e8;
            socket = null;
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "call connectSync cause SocketException :" + f.go(e3));
            a.aak(50);
            if (socket != null) {
                socket.close();
            }
            throw new PushException("create socket failed", ErrorType.Err_Connect);
        } catch (IOException e9) {
            e3 = e9;
            socket = null;
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "call connectSync cause IOException:" + f.go(e3));
            a.aak(50);
            if (socket != null) {
                socket.close();
            }
            throw new PushException("create socket failed", ErrorType.Err_Connect);
        } catch (Exception e10) {
            e3 = e10;
            socket = null;
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "call connectSync cause Exception:" + f.go(e3));
            a.aak(50);
            if (socket != null) {
                socket.close();
            }
            throw new PushException("create socket failed", ErrorType.Err_Connect);
        }
    }

    private void vt(Socket socket, String str, int i, String str2, int i2) {
        if (socket == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "socket is null");
            return;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "use Proxy " + str2 + ":" + i2 + " to connect to push server.");
        socket.connect(new InetSocketAddress(str2, i2), ((int) i.mj(this.ge).om()) * 1000);
        String str3 = "CONNECT " + str + ":" + i;
        socket.getOutputStream().write((str3 + " HTTP/1.1\r\nHost: " + str3 + "\r\n\r\n").getBytes("UTF-8"));
        InputStream inputStream = socket.getInputStream();
        StringBuilder stringBuilder = new StringBuilder(100);
        int i3 = 0;
        do {
            char read = (char) inputStream.read();
            stringBuilder.append(read);
            if ((i3 == 0 || i3 == 2) && read == '\r') {
                i3++;
            } else if ((i3 == 1 || i3 == 3) && read == '\n') {
                i3++;
            } else {
                i3 = 0;
            }
        } while (i3 != 4);
        str3 = com.huawei.android.pushagent.utils.d.fm(new BufferedReader(new StringReader(stringBuilder.toString())));
        if (str3 != null) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "read data:" + g.cc(str3));
        }
    }

    private void vu(SocketReadThread$SocketEvent socketReadThread$SocketEvent, Bundle bundle) {
        this.gd.uy(socketReadThread$SocketEvent, bundle);
    }
}
