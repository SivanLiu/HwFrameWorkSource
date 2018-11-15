package com.huawei.android.pushagent.model.channel.a;

import android.content.Context;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.f.b;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.SecureRandom;

public class a implements c {
    private static byte[] ek;
    private static byte[] en;
    private static byte[] eo;
    private boolean ej = false;
    private Context el;
    private Socket em;

    public a(Context context) {
        this.el = context;
    }

    public synchronized boolean ni(Socket socket) {
        if (nj(socket)) {
            this.em = socket;
            try {
                byte[] nd = nd(this.el);
                OutputStream outputStream = this.em.getOutputStream();
                if (outputStream == null || nd.length == 0) {
                    c.eo("PushLog3413", "outputStream or secureKeyExchangeReqData is null");
                    nb();
                    return false;
                }
                c.ep("PushLog3413", "process cmdid to send to pushSrv" + b.em((byte) 22));
                outputStream.write(nd);
                outputStream.flush();
                InputStream inputStream = this.em.getInputStream();
                if (nj(socket)) {
                    int read = inputStream.read();
                    if (-1 == read) {
                        c.ep("PushLog3413", " read -1 when init secure channel, socket maybe closed");
                    } else if (23 == read) {
                        c.ep("PushLog3413", "process cmdid to receive from pushSrv" + b.em((byte) 23));
                        nd = ne(inputStream);
                        if (nd != null) {
                            nn(nd);
                            no(com.huawei.android.pushagent.utils.a.c.l(nd, en));
                            this.ej = true;
                            c.ep("PushLog3413", "CustSecureChannel isInitialized success!");
                            return true;
                        }
                        c.ep("PushLog3413", "get server key error");
                    } else {
                        c.ep("PushLog3413", "cmdId is not CMD_SECUREKEYEXCHANGE_RSP");
                    }
                }
                nb();
                return false;
            } catch (Throwable e) {
                c.es("PushLog3413", "call send cause:" + e.toString(), e);
            }
        } else {
            nb();
            return false;
        }
    }

    public synchronized void nb() {
        c.er("PushLog3413", "enter pushChannel:close()");
        this.ej = false;
        try {
            if (this.em == null || (this.em.isClosed() ^ 1) == 0) {
                c.eo("PushLog3413", "socket has been closed");
            } else {
                this.em.close();
            }
            this.em = null;
        } catch (Throwable e) {
            c.es("PushLog3413", "close socket error: " + e.toString(), e);
            this.em = null;
        } catch (Throwable th) {
            this.em = null;
        }
        return;
    }

    public synchronized boolean nl(byte[] bArr) {
        if (this.em == null) {
            c.eo("PushLog3413", "socket is null");
            return false;
        } else if (this.ej) {
            try {
                byte[] na = na(bArr, false);
                OutputStream outputStream = this.em.getOutputStream();
                if (outputStream == null) {
                    c.eo("PushLog3413", "outputStream is null");
                    return false;
                } else if (na.length == 0) {
                    c.eo("PushLog3413", "data is null");
                    return false;
                } else {
                    outputStream.write(na);
                    outputStream.flush();
                    return true;
                }
            } catch (Throwable e) {
                c.es("PushLog3413", "call send cause:" + e.toString(), e);
                nb();
                return false;
            }
        } else {
            c.eo("PushLog3413", "secure socket is not initialized, can not write any data");
            nb();
            return false;
        }
    }

    public boolean nh() {
        if (this.em != null) {
            return this.em.isConnected();
        }
        c.eo("PushLog3413", "socket is null");
        return false;
    }

    private boolean nj(Socket socket) {
        if (socket == null) {
            c.eo("PushLog3413", "socket is null");
            return false;
        } else if (socket.isConnected()) {
            return true;
        } else {
            c.eo("PushLog3413", "when init Channel, socket is not ready");
            return false;
        }
    }

    public Socket ng() {
        return this.em;
    }

    public InputStream nc() {
        try {
            if (this.em != null) {
                return new b(this, this.em.getInputStream());
            }
            c.eo("PushLog3413", "socket is null");
            return null;
        } catch (Throwable e) {
            c.es("PushLog3413", "call socket.getInputStream cause:" + e.toString(), e);
        }
    }

    private byte[] nd(Context context) {
        byte te = (byte) k.rh(context).te();
        String tf = k.rh(context).tf();
        byte[] bArr = new byte[16];
        new SecureRandom().nextBytes(bArr);
        c.er("PushLog3413", "ready to send SecureChannelReqMessage, save clientKey for decode serverKey");
        nm(bArr);
        byte[] p = com.huawei.android.pushagent.utils.a.c.p(bArr, tf);
        if (p == null) {
            c.eo("PushLog3413", "rsa encrypr clientKey error");
            return new byte[0];
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(22);
        byteArrayOutputStream.write(g.gd(((p.length + 1) + 1) + 2));
        byteArrayOutputStream.write(te);
        byteArrayOutputStream.write(p);
        return byteArrayOutputStream.toByteArray();
    }

    private byte[] ne(InputStream inputStream) {
        nk(inputStream, new byte[2]);
        byte[] bArr = new byte[1];
        nk(inputStream, bArr);
        byte b = bArr[0];
        c.er("PushLog3413", "login result is " + b);
        if (b == (byte) 0) {
            bArr = new byte[48];
            nk(inputStream, bArr);
            return bArr;
        }
        c.eo("PushLog3413", "secure key exchange error");
        return null;
    }

    private static void nk(InputStream inputStream, byte[] bArr) {
        int i = 0;
        while (i < bArr.length) {
            int read = inputStream.read(bArr, i, bArr.length - i);
            if (-1 == read) {
                com.huawei.android.pushagent.a.a.hx(89);
                throw new IOException("read length return -1, invalid length");
            }
            i += read;
        }
    }

    public static byte[] na(byte[] bArr, boolean z) {
        byte[] n;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(48);
        if (z) {
            n = com.huawei.android.pushagent.utils.a.c.n(bArr, eo, ek);
        } else {
            n = com.huawei.android.pushagent.utils.a.c.m(bArr, eo);
        }
        if (n == null) {
            c.er("PushLog3413", "aes encrypt pushMsgData error");
            return new byte[0];
        }
        byteArrayOutputStream.write(g.gd((n.length + 1) + 2));
        byteArrayOutputStream.write(n);
        return byteArrayOutputStream.toByteArray();
    }

    private static byte[] nf(InputStream inputStream) {
        byte[] bArr = new byte[2];
        nk(inputStream, bArr);
        bArr = new byte[(g.gx(bArr) - 3)];
        nk(inputStream, bArr);
        return bArr;
    }

    private static void nm(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            c.er("PushLog3413", "key is null");
            return;
        }
        en = new byte[bArr.length];
        System.arraycopy(bArr, 0, en, 0, bArr.length);
    }

    private static void no(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            c.er("PushLog3413", "key is null");
            return;
        }
        eo = new byte[bArr.length];
        System.arraycopy(bArr, 0, eo, 0, bArr.length);
    }

    private static void nn(byte[] bArr) {
        if (bArr == null || bArr.length < 16) {
            c.eq("PushLog3413", "iv is null");
            return;
        }
        ek = new byte[16];
        System.arraycopy(bArr, 0, ek, 0, 16);
    }
}
