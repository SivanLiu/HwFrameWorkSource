package com.huawei.android.pushagent.model.channel.entity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import com.huawei.android.pushagent.datatype.tcp.base.IPushMessage;
import com.huawei.android.pushagent.model.channel.a.c;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.f.b;
import com.huawei.android.pushagent.utils.tools.d;
import java.net.Socket;

public abstract class a {
    private static final /* synthetic */ int[] dr = null;
    public c df;
    private byte[] dg = new byte[0];
    public com.huawei.android.pushagent.datatype.a.a dh;
    protected Context di;
    private boolean dj = false;
    public c dk;
    protected boolean dl;
    private final Object dm = new Object();
    protected int dn = 1;
    /* renamed from: do */
    private WakeLock f0do = null;
    private PowerManager dp;
    public b dq;

    private static /* synthetic */ int[] mq() {
        if (dr != null) {
            return dr;
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
        dr = iArr;
        return iArr;
    }

    public abstract void lt(boolean z, String str);

    public abstract void lx(SocketReadThread$SocketEvent socketReadThread$SocketEvent, Bundle bundle);

    public a(com.huawei.android.pushagent.datatype.a.a aVar, Context context, c cVar) {
        this.di = context;
        this.dh = aVar;
        this.dk = cVar;
        this.dp = (PowerManager) context.getSystemService("power");
    }

    public boolean mk() {
        return this.df != null ? this.df.nh() : false;
    }

    protected com.huawei.android.pushagent.datatype.a.a me(int i, int i2) {
        switch (mq()[ConnectMode$CONNECT_METHOD.values()[mf(i, i2)].ordinal()]) {
            case 1:
                return new com.huawei.android.pushagent.datatype.a.a(this.dh.ka(), 443, false);
            case 2:
                return new com.huawei.android.pushagent.datatype.a.a(this.dh.ka(), this.dh.kb(), false);
            case 3:
                return new com.huawei.android.pushagent.datatype.a.a(this.dh.ka(), this.dh.kb(), true);
            case 4:
                return new com.huawei.android.pushagent.datatype.a.a(this.dh.ka(), 443, true);
            default:
                return null;
        }
    }

    protected int mf(int i, int i2) {
        return Math.abs(i + i2) % ConnectMode$CONNECT_METHOD.values().length;
    }

    protected synchronized void mj() {
        this.f0do = this.dp.newWakeLock(1, "mWakeLockForThread");
        this.f0do.setReferenceCounted(false);
        this.f0do.acquire(1000);
    }

    /* JADX WARNING: Missing block: B:15:0x0027, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean mc(IPushMessage iPushMessage) {
        byte[] bArr = null;
        synchronized (this) {
            if (this.df == null || this.df.ng() == null) {
                com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "when send pushMsg, channel is null， curCls:" + getClass().getSimpleName());
            } else {
                mo();
                if (iPushMessage != null) {
                    bArr = iPushMessage.is();
                } else {
                    com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "pushMsg = null, send fail");
                }
                if (bArr == null || bArr.length == 0) {
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "when send PushMsg, encode Len is null");
                } else {
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "process cmdid to send to pushSrv:" + b.em(iPushMessage.it()) + ", subCmdId:" + b.em(iPushMessage.iu()));
                    if (this.df.nl(bArr)) {
                        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "send msg to remote srv success");
                        mp(iPushMessage);
                        return true;
                    }
                    com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "call channel.send false!!");
                }
            }
        }
    }

    public Socket mi() {
        if (this.df != null) {
            return this.df.ng();
        }
        return null;
    }

    public void md() {
        if (this.df != null) {
            try {
                this.df.nb();
                this.df = null;
            } catch (Throwable e) {
                com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "call channel.close() cause:" + e.toString(), e);
            }
            if (this.dq != null) {
                this.dq.interrupt();
                this.dq = null;
            }
        }
    }

    public String toString() {
        return this.dh.toString() + " " + this.dk.toString();
    }

    public synchronized void mn(boolean z) {
        this.dl = z;
    }

    public void ml(boolean z) {
        synchronized (this.dm) {
            this.dj = z;
        }
    }

    public boolean mg() {
        boolean z;
        synchronized (this.dm) {
            z = this.dj;
        }
        return z;
    }

    private void mo() {
        this.df.ng().setSoTimeout(0);
    }

    private void mp(IPushMessage iPushMessage) {
        if (iPushMessage != null) {
            if ((byte) 64 == iPushMessage.it() || (byte) 66 == iPushMessage.it()) {
                Intent intent = new Intent("com.huawei.android.push.intent.RESPONSE_FAIL");
                intent.setPackage(this.di.getPackageName());
                intent.putExtra("cmdId", iPushMessage.it());
                intent.putExtra("subCmdId", iPushMessage.iu());
                d.cw(this.di, intent, k.rh(this.di).sb());
            }
        }
    }

    public int mh() {
        int i;
        synchronized (this.dg) {
            i = this.dn;
        }
        return i;
    }

    public void mm(int i) {
        synchronized (this.dg) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "setLockState:" + i);
            this.dn = i;
        }
    }
}
