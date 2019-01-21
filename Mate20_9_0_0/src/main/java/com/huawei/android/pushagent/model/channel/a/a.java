package com.huawei.android.pushagent.model.channel.a;

import android.content.Context;
import com.huawei.android.pushagent.utils.b.c;
import com.huawei.android.pushagent.utils.d;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.SecureRandom;

public class a implements c {
    private static byte[] bp;
    private static byte[] bs;
    private static byte[] bt;
    private boolean bo = false;
    private Context bq;
    private Socket br;

    public a(Context context) {
        this.bq = context;
    }

    public synchronized boolean dp(Socket socket) {
        if (dq(socket)) {
            this.br = socket;
            try {
                byte[] dk = dk(this.bq);
                OutputStream outputStream = this.br.getOutputStream();
                if (outputStream == null || dk.length == 0) {
                    com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "outputStream or secureKeyExchangeReqData is null");
                    di();
                    return false;
                }
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "process cmdid to send to pushSrv" + c.ts((byte) 22));
                outputStream.write(dk);
                outputStream.flush();
                InputStream inputStream = this.br.getInputStream();
                if (dq(socket)) {
                    int read = inputStream.read();
                    if (-1 == read) {
                        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", " read -1 when init secure channel, socket maybe closed");
                    } else if (23 == read) {
                        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "process cmdid to receive from pushSrv" + c.ts((byte) 23));
                        dk = dl(inputStream);
                        if (dk != null) {
                            du(dk);
                            dv(com.huawei.android.pushagent.utils.e.a.vw(dk, bs));
                            this.bo = true;
                            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "CustSecureChannel isInitialized success!");
                            return true;
                        }
                        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "get server key error");
                    } else {
                        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "cmdId is not CMD_SECUREKEYEXCHANGE_RSP");
                    }
                }
                di();
                return false;
            } catch (Exception e) {
                com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "call send cause:" + e.toString(), e);
            }
        } else {
            di();
            return false;
        }
    }

    public synchronized void di() {
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "enter pushChannel:close()");
        this.bo = false;
        try {
            if (this.br == null || (this.br.isClosed() ^ 1) == 0) {
                com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "socket has been closed");
            } else {
                this.br.close();
            }
            this.br = null;
        } catch (IOException e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "close socket error: " + e.toString(), e);
            this.br = null;
        } catch (Throwable th) {
            this.br = null;
        }
        return;
    }

    public synchronized boolean ds(byte[] bArr) {
        if (this.br == null) {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "socket is null");
            return false;
        } else if (this.bo) {
            try {
                byte[] dh = dh(bArr, false);
                OutputStream outputStream = this.br.getOutputStream();
                if (outputStream == null) {
                    com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "outputStream is null");
                    return false;
                } else if (dh.length == 0) {
                    com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "data is null");
                    return false;
                } else {
                    outputStream.write(dh);
                    outputStream.flush();
                    return true;
                }
            } catch (Exception e) {
                com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "call send cause:" + e.toString(), e);
                di();
                return false;
            }
        } else {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "secure socket is not initialized, can not write any data");
            di();
            return false;
        }
    }

    /* renamed from: do */
    public boolean m1do() {
        if (this.br != null) {
            return this.br.isConnected();
        }
        com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "socket is null");
        return false;
    }

    private boolean dq(Socket socket) {
        if (socket == null) {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "socket is null");
            return false;
        } else if (socket.isConnected()) {
            return true;
        } else {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "when init Channel, socket is not ready");
            return false;
        }
    }

    public Socket dn() {
        return this.br;
    }

    public InputStream dj() {
        try {
            if (this.br != null) {
                return new b(this, this.br.getInputStream());
            }
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "socket is null");
            return null;
        } catch (IOException e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "call socket.getInputStream cause:" + e.toString(), e);
        }
    }

    private byte[] dk(Context context) {
        byte gk = (byte) com.huawei.android.pushagent.model.prefs.a.ff(context).gk();
        String gw = com.huawei.android.pushagent.model.prefs.a.ff(context).gw();
        byte[] bArr = new byte[16];
        new SecureRandom().nextBytes(bArr);
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "ready to send SecureChannelReqMessage, save clientKey for decode serverKey");
        dt(bArr);
        byte[] vz = com.huawei.android.pushagent.utils.e.a.vz(bArr, gw);
        if (vz == null) {
            com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "rsa encrypr clientKey error");
            return new byte[0];
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(22);
        byteArrayOutputStream.write(d.zr(((vz.length + 1) + 1) + 2));
        byteArrayOutputStream.write(gk);
        byteArrayOutputStream.write(vz);
        return byteArrayOutputStream.toByteArray();
    }

    private byte[] dl(InputStream inputStream) {
        dr(inputStream, new byte[2]);
        byte[] bArr = new byte[1];
        dr(inputStream, bArr);
        byte b = bArr[0];
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "login result is " + b);
        if (b == (byte) 0) {
            bArr = new byte[48];
            dr(inputStream, bArr);
            return bArr;
        }
        com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "secure key exchange error");
        return null;
    }

    private static void dr(InputStream inputStream, byte[] bArr) {
        int i = 0;
        while (i < bArr.length) {
            int read = inputStream.read(bArr, i, bArr.length - i);
            if (-1 == read) {
                com.huawei.android.pushagent.b.a.abd(89);
                throw new IOException("read length return -1, invalid length");
            }
            i += read;
        }
    }

    public static byte[] dh(byte[] bArr, boolean z) {
        byte[] vy;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(48);
        if (z) {
            vy = com.huawei.android.pushagent.utils.e.a.vy(bArr, bt, bp);
        } else {
            vy = com.huawei.android.pushagent.utils.e.a.vx(bArr, bt);
        }
        if (vy == null) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "aes encrypt pushMsgData error");
            return new byte[0];
        }
        byteArrayOutputStream.write(d.zr((vy.length + 1) + 2));
        byteArrayOutputStream.write(vy);
        return byteArrayOutputStream.toByteArray();
    }

    private static byte[] dm(InputStream inputStream) {
        byte[] bArr = new byte[2];
        dr(inputStream, bArr);
        bArr = new byte[(d.yg(bArr) - 3)];
        dr(inputStream, bArr);
        return bArr;
    }

    private static void dt(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "key is null");
            return;
        }
        bs = new byte[bArr.length];
        System.arraycopy(bArr, 0, bs, 0, bArr.length);
    }

    private static void dv(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "key is null");
            return;
        }
        bt = new byte[bArr.length];
        System.arraycopy(bArr, 0, bt, 0, bArr.length);
    }

    private static void du(byte[] bArr) {
        if (bArr == null || bArr.length < 16) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "iv is null");
            return;
        }
        bp = new byte[16];
        System.arraycopy(bArr, 0, bp, 0, 16);
    }
}
