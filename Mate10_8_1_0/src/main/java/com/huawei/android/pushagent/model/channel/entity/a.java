package com.huawei.android.pushagent.model.channel.entity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import com.huawei.android.pushagent.datatype.b.d;
import com.huawei.android.pushagent.datatype.tcp.base.IPushMessage;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a.b;
import java.net.Socket;

public abstract class a {
    private static final /* synthetic */ int[] fx = null;
    public com.huawei.android.pushagent.model.channel.a.a fl;
    private byte[] fm = new byte[0];
    public d fn;
    protected Context fo;
    private boolean fp = false;
    public c fq;
    protected boolean fr;
    private final Object fs = new Object();
    protected int ft = 1;
    private WakeLock fu = null;
    private PowerManager fv;
    public b fw;

    private static /* synthetic */ int[] vq() {
        if (fx != null) {
            return fx;
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
        fx = iArr;
        return iArr;
    }

    public abstract void uu(boolean z);

    public abstract void uy(SocketReadThread$SocketEvent socketReadThread$SocketEvent, Bundle bundle);

    public a(d dVar, Context context, c cVar) {
        this.fo = context;
        this.fn = dVar;
        this.fq = cVar;
        this.fv = (PowerManager) context.getSystemService("power");
    }

    public boolean vc() {
        return this.fl != null ? this.fl.wb() : false;
    }

    protected d vg(int i, int i2) {
        switch (vq()[ConnectMode$CONNECT_METHOD.values()[vh(i, i2)].ordinal()]) {
            case 1:
                return new d(this.fn.yj(), 443, false);
            case 2:
                return new d(this.fn.yj(), this.fn.yk(), false);
            case 3:
                return new d(this.fn.yj(), this.fn.yk(), true);
            case 4:
                return new d(this.fn.yj(), 443, true);
            default:
                return null;
        }
    }

    protected int vh(int i, int i2) {
        return Math.abs(i + i2) % ConnectMode$CONNECT_METHOD.values().length;
    }

    protected synchronized void vk() {
        this.fu = this.fv.newWakeLock(1, "mWakeLockForThread");
        this.fu.setReferenceCounted(false);
        this.fu.acquire(1000);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean vl(IPushMessage iPushMessage) {
        byte[] bArr = null;
        synchronized (this) {
            if (this.fl == null || this.fl.vz() == null) {
                b.y("PushLog2976", "when send pushMsg, channel is null， curCls:" + getClass().getSimpleName());
            } else {
                vo();
                if (iPushMessage != null) {
                    bArr = iPushMessage.yp();
                } else {
                    b.y("PushLog2976", "pushMsg = null, send fail");
                }
                if (bArr == null || bArr.length == 0) {
                    b.z("PushLog2976", "when send PushMsg, encode Len is null");
                } else {
                    b.z("PushLog2976", "process cmdid to send to pushSrv:" + com.huawei.android.pushagent.utils.a.a.v(iPushMessage.yq()) + ", subCmdId:" + com.huawei.android.pushagent.utils.a.a.v(iPushMessage.yr()));
                    if (this.fl.wd(bArr)) {
                        b.z("PushLog2976", "send msg to remote srv success");
                        vp(iPushMessage);
                        return true;
                    }
                    b.y("PushLog2976", "call channel.send false!!");
                }
            }
        }
    }

    public Socket vd() {
        if (this.fl != null) {
            return this.fl.vz();
        }
        return null;
    }

    public void ve() {
        if (this.fl != null) {
            try {
                this.fl.wc();
                this.fl = null;
            } catch (Throwable e) {
                b.aa("PushLog2976", "call channel.close() cause:" + e.toString(), e);
            }
            if (this.fw != null) {
                this.fw.interrupt();
                this.fw = null;
            }
        }
    }

    public String toString() {
        return this.fn.toString() + " " + this.fq.toString();
    }

    public synchronized void vn(boolean z) {
        this.fr = z;
    }

    public void vf(boolean z) {
        synchronized (this.fs) {
            this.fp = z;
        }
    }

    public boolean vi() {
        boolean z;
        synchronized (this.fs) {
            z = this.fp;
        }
        return z;
    }

    private void vo() {
        this.fl.vz().setSoTimeout(0);
    }

    private void vp(IPushMessage iPushMessage) {
        if (iPushMessage != null) {
            if ((byte) 64 == iPushMessage.yq() || (byte) 66 == iPushMessage.yq()) {
                Intent intent = new Intent("com.huawei.android.push.intent.RESPONSE_FAIL");
                intent.setPackage(this.fo.getPackageName());
                intent.putExtra("cmdId", iPushMessage.yq());
                intent.putExtra("subCmdId", iPushMessage.yr());
                com.huawei.android.pushagent.utils.tools.d.p(this.fo, intent, i.mj(this.fo).od());
            }
        }
    }

    public int vj() {
        int i;
        synchronized (this.fm) {
            i = this.ft;
        }
        return i;
    }

    public void vm(int i) {
        synchronized (this.fm) {
            b.z("PushLog2976", "setLockState:" + i);
            this.ft = i;
        }
    }
}
