package com.huawei.android.pushagent.model.channel.a;

import android.content.Context;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a.a;
import com.huawei.android.pushagent.utils.c.c;
import com.huawei.android.pushagent.utils.f;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.SecureRandom;

public class b implements a {
    private static byte[] gp;
    private static byte[] gs;
    private static byte[] gt;
    private boolean go = false;
    private Context gq;
    private Socket gr;

    public b(Context context) {
        this.gq = context;
    }

    public synchronized boolean we(Socket socket) {
        if (wj(socket)) {
            this.gr = socket;
            try {
                byte[] wg = wg(this.gq);
                OutputStream outputStream = this.gr.getOutputStream();
                if (outputStream == null || wg.length == 0) {
                    com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "outputStream or secureKeyExchangeReqData is null");
                    wc();
                    return false;
                }
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "process cmdid to send to pushSrv" + a.v((byte) 22));
                outputStream.write(wg);
                outputStream.flush();
                InputStream inputStream = this.gr.getInputStream();
                if (wj(socket)) {
                    int read = inputStream.read();
                    if (-1 == read) {
                        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", " read -1 when init secure channel, socket maybe closed");
                    } else if (23 == read) {
                        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "process cmdid to receive from pushSrv" + a.v((byte) 23));
                        wg = wh(inputStream);
                        if (wg != null) {
                            wm(wg);
                            wn(c.be(wg, gs));
                            this.go = true;
                            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "CustSecureChannel isInitialized success!");
                            return true;
                        }
                        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "get server key error");
                    } else {
                        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "cmdId is not CMD_SECUREKEYEXCHANGE_RSP");
                    }
                }
                wc();
                return false;
            } catch (Throwable e) {
                com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "call send cause:" + e.toString(), e);
            }
        } else {
            wc();
            return false;
        }
    }

    public synchronized void wc() {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "enter pushChannel:close()");
        this.go = false;
        try {
            if (this.gr == null || (this.gr.isClosed() ^ 1) == 0) {
                com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "socket has been closed");
            } else {
                this.gr.close();
            }
            this.gr = null;
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "close socket error: " + e.toString(), e);
            this.gr = null;
        } catch (Throwable th) {
            this.gr = null;
        }
    }

    public synchronized boolean wd(byte[] bArr) {
        if (this.gr == null) {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "socket is null");
            return false;
        } else if (this.go) {
            try {
                byte[] wf = wf(bArr, false);
                OutputStream outputStream = this.gr.getOutputStream();
                if (outputStream == null) {
                    com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "outputStream is null");
                    return false;
                } else if (wf.length == 0) {
                    com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "data is null");
                    return false;
                } else {
                    outputStream.write(wf);
                    outputStream.flush();
                    return true;
                }
            } catch (Throwable e) {
                com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "call send cause:" + e.toString(), e);
                wc();
                return false;
            }
        } else {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "secure socket is not initialized, can not write any data");
            wc();
            return false;
        }
    }

    public boolean wb() {
        if (this.gr != null) {
            return this.gr.isConnected();
        }
        com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "socket is null");
        return false;
    }

    private boolean wj(Socket socket) {
        if (socket == null) {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "socket is null");
            return false;
        } else if (socket.isConnected()) {
            return true;
        } else {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "when init Channel, socket is not ready");
            return false;
        }
    }

    public Socket vz() {
        return this.gr;
    }

    public InputStream wa() {
        try {
            if (this.gr != null) {
                return new c(this, this.gr.getInputStream());
            }
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "socket is null");
            return null;
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "call socket.getInputStream cause:" + e.toString(), e);
        }
    }

    private byte[] wg(Context context) {
        byte oz = (byte) i.mj(context).oz();
        String pg = i.mj(context).pg();
        byte[] bArr = new byte[16];
        new SecureRandom().nextBytes(bArr);
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "ready to send SecureChannelReqMessage, save clientKey for decode serverKey");
        wl(bArr);
        byte[] bh = c.bh(bArr, pg);
        if (bh == null) {
            com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "rsa encrypr clientKey error");
            return new byte[0];
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(22);
        byteArrayOutputStream.write(f.fr(((bh.length + 1) + 1) + 2));
        byteArrayOutputStream.write(oz);
        byteArrayOutputStream.write(bh);
        return byteArrayOutputStream.toByteArray();
    }

    private byte[] wh(InputStream inputStream) {
        wk(inputStream, new byte[2]);
        byte[] bArr = new byte[1];
        wk(inputStream, bArr);
        byte b = bArr[0];
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "login result is " + b);
        if (b == (byte) 0) {
            bArr = new byte[48];
            wk(inputStream, bArr);
            return bArr;
        }
        com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "secure key exchange error");
        return null;
    }

    private static void wk(InputStream inputStream, byte[] bArr) {
        int i = 0;
        while (i < bArr.length) {
            int read = inputStream.read(bArr, i, bArr.length - i);
            if (-1 == read) {
                com.huawei.android.pushagent.b.a.aak(89);
                throw new IOException("read length return -1, invalid length");
            }
            i += read;
        }
    }

    public static byte[] wf(byte[] bArr, boolean z) {
        byte[] bg;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(48);
        if (z) {
            bg = c.bg(bArr, gt, gp);
        } else {
            bg = c.bf(bArr, gt);
        }
        if (bg == null) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "aes encrypt pushMsgData error");
            return new byte[0];
        }
        byteArrayOutputStream.write(f.fr((bg.length + 1) + 2));
        byteArrayOutputStream.write(bg);
        return byteArrayOutputStream.toByteArray();
    }

    private static byte[] wi(InputStream inputStream) {
        byte[] bArr = new byte[2];
        wk(inputStream, bArr);
        bArr = new byte[(f.ft(bArr) - 3)];
        wk(inputStream, bArr);
        return bArr;
    }

    private static void wl(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "key is null");
            return;
        }
        gs = new byte[bArr.length];
        System.arraycopy(bArr, 0, gs, 0, bArr.length);
    }

    private static void wn(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "key is null");
            return;
        }
        gt = new byte[bArr.length];
        System.arraycopy(bArr, 0, gt, 0, bArr.length);
    }

    private static void wm(byte[] bArr) {
        if (bArr == null || bArr.length < 16) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "iv is null");
            return;
        }
        gp = new byte[16];
        System.arraycopy(bArr, 0, gp, 0, 16);
    }
}
