package com.huawei.android.pushagent.model.channel.entity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import com.huawei.android.pushagent.datatype.a.c;
import com.huawei.android.pushagent.datatype.tcp.base.IPushMessage;
import com.huawei.android.pushagent.utils.b.a;
import java.net.Socket;

public abstract class b {
    private static final /* synthetic */ int[] ba = null;
    public c ao;
    protected Context ap;
    protected boolean aq;
    public c ar;
    public a as;
    private byte[] at = new byte[0];
    private boolean au = false;
    private final Object av = new Object();
    protected int aw = 1;
    private WakeLock ax = null;
    private PowerManager ay;
    public com.huawei.android.pushagent.model.channel.a.c az;

    private static /* synthetic */ int[] dc() {
        if (ba != null) {
            return ba;
        }
        int[] iArr = new int[ConnectMode$CONNECT_METHOD.values().length];
        try {
            iArr[ConnectMode$CONNECT_METHOD.CONNECT_METHOD_DIRECT_DefaultPort.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[ConnectMode$CONNECT_METHOD.CONNECT_METHOD_DIRECT_TrsPort.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[ConnectMode$CONNECT_METHOD.CONNECT_METHOD_Proxy_DefaultPort.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[ConnectMode$CONNECT_METHOD.CONNECT_METHOD_Proxy_TrsPort.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        ba = iArr;
        return iArr;
    }

    public abstract void bw(boolean z, String str);

    public abstract void cb(SocketReadThread$SocketEvent socketReadThread$SocketEvent, Bundle bundle);

    public b(c cVar, Context context, a aVar) {
        this.ap = context;
        this.ao = cVar;
        this.as = aVar;
        this.ay = (PowerManager) context.getSystemService("power");
    }

    public boolean cp() {
        return this.az != null ? this.az.m0do() : false;
    }

    protected c cv(int i, int i2) {
        switch (dc()[ConnectMode$CONNECT_METHOD.values()[cw(i, i2)].ordinal()]) {
            case 1:
                return new c(this.ao.au(), 443, false);
            case 2:
                return new c(this.ao.au(), this.ao.av(), false);
            case 3:
                return new c(this.ao.au(), this.ao.av(), true);
            case 4:
                return new c(this.ao.au(), 443, true);
            default:
                return null;
        }
    }

    protected int cw(int i, int i2) {
        return Math.abs(i + i2) % ConnectMode$CONNECT_METHOD.values().length;
    }

    protected synchronized void ct() {
        this.ax = this.ay.newWakeLock(1, "mWakeLockForThread");
        this.ax.setReferenceCounted(false);
        this.ax.acquire(1000);
    }

    /* JADX WARNING: Missing block: B:15:0x0027, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean co(IPushMessage iPushMessage) {
        byte[] bArr = null;
        synchronized (this) {
            if (this.az == null || this.az.dn() == null) {
                a.su("PushLog3414", "when send pushMsg, channel is null， curCls:" + getClass().getSimpleName());
            } else {
                da();
                if (iPushMessage != null) {
                    bArr = iPushMessage.b();
                } else {
                    a.su("PushLog3414", "pushMsg = null, send fail");
                }
                if (bArr == null || bArr.length == 0) {
                    a.sv("PushLog3414", "when send PushMsg, encode Len is null");
                } else {
                    a.sv("PushLog3414", "process cmdid to send to pushSrv:" + com.huawei.android.pushagent.utils.b.c.ts(iPushMessage.c()) + ", subCmdId:" + com.huawei.android.pushagent.utils.b.c.ts(iPushMessage.g()));
                    if (this.az.ds(bArr)) {
                        a.sv("PushLog3414", "send msg to remote srv success");
                        db(iPushMessage);
                        return true;
                    }
                    a.su("PushLog3414", "call channel.send false!!");
                }
            }
        }
    }

    public Socket cz() {
        if (this.az != null) {
            return this.az.dn();
        }
        return null;
    }

    public void cu() {
        if (this.az != null) {
            try {
                this.az.di();
                this.az = null;
            } catch (Exception e) {
                a.sw("PushLog3414", "call channel.close() cause:" + e.toString(), e);
            }
            if (this.ar != null) {
                this.ar.interrupt();
                this.ar = null;
            }
        }
    }

    public String toString() {
        return this.ao.toString() + " " + this.as.toString();
    }

    public synchronized void cs(boolean z) {
        this.aq = z;
    }

    public void cr(boolean z) {
        synchronized (this.av) {
            this.au = z;
        }
    }

    public boolean cq() {
        boolean z;
        synchronized (this.av) {
            z = this.au;
        }
        return z;
    }

    private void da() {
        this.az.dn().setSoTimeout(0);
    }

    private void db(IPushMessage iPushMessage) {
        if (iPushMessage != null) {
            if ((byte) 64 == iPushMessage.c() || (byte) 66 == iPushMessage.c()) {
                Intent intent = new Intent("com.huawei.android.push.intent.RESPONSE_FAIL");
                intent.setPackage(this.ap.getPackageName());
                intent.putExtra("cmdId", iPushMessage.c());
                intent.putExtra("subCmdId", iPushMessage.g());
                com.huawei.android.pushagent.utils.tools.a.sa(this.ap, intent, com.huawei.android.pushagent.model.prefs.a.ff(this.ap).hi());
            }
        }
    }

    public int cx() {
        int i;
        synchronized (this.at) {
            i = this.aw;
        }
        return i;
    }

    public void cy(int i) {
        synchronized (this.at) {
            a.sv("PushLog3414", "setLockState:" + i);
            this.aw = i;
        }
    }
}
