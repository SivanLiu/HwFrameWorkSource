package com.huawei.android.pushagent.utils.bastet;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.huawei.android.bastet.HwBastet;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatReqMessage;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatRspMessage;
import com.huawei.android.pushagent.model.prefs.f;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.tools.d;
import java.lang.reflect.Method;
import java.net.Socket;

public class a {
    private static a au = null;
    private PushBastet$BasteProxyStatus aq = PushBastet$BasteProxyStatus.Stoped;
    private Context ar;
    private Handler as = null;
    private HwBastet at = null;

    public static synchronized a dk(Context context) {
        synchronized (a.class) {
            a aVar;
            if (au != null) {
                aVar = au;
                return aVar;
            }
            au = new a(context);
            aVar = au;
            return aVar;
        }
    }

    private a(Context context) {
        this.ar = context;
    }

    public synchronized boolean dg() {
        try {
            if (di()) {
                c.ep("PushLog3413", "bastet has started, need not restart");
                return true;
            } else if (dp()) {
                com.huawei.android.pushagent.a.a.hx(73);
                if (dq(3, 900000)) {
                    c.ep("PushLog3413", "startPushBastetProxy success");
                    dw(PushBastet$BasteProxyStatus.Started);
                    return true;
                }
                c.ep("PushLog3413", "startPushBastetProxy failed");
                return false;
            } else {
                com.huawei.android.pushagent.a.a.hx(72);
                c.ep("PushLog3413", "init push bastet failed!");
                return false;
            }
        } catch (Throwable e) {
            c.es("PushLog3413", "startPushBastetProxy failed:" + e.getMessage(), e);
        }
    }

    public boolean dj() {
        if (1 != g.fw(this.ar)) {
            return dr() && ds();
        } else {
            c.ep("PushLog3413", "not support bastet in wifi");
            return false;
        }
    }

    private boolean dp() {
        c.er("PushLog3413", "initPushBastet");
        if (!dj()) {
            return false;
        }
        Socket mi = com.huawei.android.pushagent.model.channel.a.ns().mi();
        if (mi == null || !m2do()) {
            return false;
        }
        try {
            this.at = new HwBastet("PUSH_BASTET", mi, this.as, this.ar);
            if (this.at.isBastetAvailable()) {
                this.at.reconnectSwitch(true);
                return true;
            }
            c.ep("PushLog3413", "isBastetAvailable false, can't use bastet.");
            dl();
            return false;
        } catch (Throwable e) {
            c.ev("PushLog3413", "init bastet error", e);
            dl();
            return false;
        }
    }

    private boolean ds() {
        boolean tc = k.rh(this.ar).tc();
        c.er("PushLog3413", "isPushServerAllowBastet: " + tc);
        return tc;
    }

    private boolean dr() {
        try {
            Class.forName("com.huawei.android.bastet.HwBastet");
            return true;
        } catch (ClassNotFoundException e) {
            c.eq("PushLog3413", "bastet not exist");
            return false;
        }
    }

    private synchronized void dt() {
        c.ep("PushLog3413", "enter quitLooper");
        try {
            if (!(this.as == null || this.as.getLooper() == null)) {
                this.as.getLooper().quitSafely();
                c.ep("PushLog3413", "bastet loop quitSafely");
            }
            this.as = null;
        } catch (Throwable e) {
            c.es("PushLog3413", "PushBastetListener release error", e);
        }
        return;
    }

    private void du() {
        c.ep("PushLog3413", "reConnectPush");
        dl();
        com.huawei.android.pushagent.model.channel.a.nt(this.ar).nw().md();
        com.huawei.android.pushagent.a.a.hx(80);
        PushService.abr(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(this.ar.getPackageName()));
    }

    private void dm(Message message) {
        int i = message.arg1;
        int fw = g.fw(this.ar);
        com.huawei.android.pushagent.a.a.hq(71, com.huawei.android.pushagent.a.a.hr(f.py(this.ar).qg(this.ar, fw), String.valueOf(4), String.valueOf(i)));
        c.ep("PushLog3413", "receive handler message BASTET_HEARTBEAT_CYCLE " + i);
        if (i > 10) {
            f.py(this.ar).pz(this.ar, 999);
            f.py(this.ar).qb(this.ar, 999, ((long) i) * 1000);
            dq(i, ((long) i) * 1000);
            c.ep("PushLog3413", "receive new bastet handler message BASTET_HEARTBEAT_CYCLE:" + i + " s.");
            dv(this.ar, ((long) i) * 1000);
        } else {
            dq(i, ((long) (i * 5)) * 60000);
            c.ep("PushLog3413", "receive old bastet handler message BASTET_HEARTBEAT_CYCLE:" + i);
            dv(this.ar, ((long) i) * 1000);
        }
        try {
            this.at.pauseHeartbeat();
        } catch (Throwable e) {
            c.es("PushLog3413", "adaptPushBastet error :" + e.toString(), e);
        }
    }

    private boolean dq(int i, long j) {
        c.er("PushLog3413", "initPushHeartBeatDataContent");
        try {
            HeartBeatReqMessage heartBeatReqMessage = new HeartBeatReqMessage();
            heartBeatReqMessage.jj((byte) ((int) Math.ceil((((double) j) * 1.0d) / 60000.0d)));
            byte[] na = com.huawei.android.pushagent.model.channel.a.a.na(heartBeatReqMessage.is(), false);
            byte[] na2 = com.huawei.android.pushagent.model.channel.a.a.na(new HeartBeatRspMessage().is(), true);
            int td = k.rh(this.ar).td();
            this.at.setAolHeartbeat(td, na, na2);
            c.er("PushLog3413", "set bastet heartbeat interval level as: " + td);
            return true;
        } catch (Throwable e) {
            c.es("PushLog3413", "initPushHeartBeatDataContent error :" + e.toString(), e);
            return false;
        }
    }

    public void dw(PushBastet$BasteProxyStatus pushBastet$BasteProxyStatus) {
        this.aq = pushBastet$BasteProxyStatus;
    }

    public boolean di() {
        return this.at != null && PushBastet$BasteProxyStatus.Started == this.aq;
    }

    public void dl() {
        c.ep("PushLog3413", "resetBastet");
        dn();
        this.at = null;
        dw(PushBastet$BasteProxyStatus.Stoped);
        dt();
        com.huawei.android.pushagent.model.channel.a.ns().ml(false);
        c.ep("PushLog3413", "after setExistResetBastetAlarm");
        d.cy(this.ar, "com.huawei.android.push.intent.RESET_BASTET");
    }

    /* renamed from: do */
    private synchronized boolean m2do() {
        c.ep("PushLog3413", "initMsgHandler");
        try {
            if (!(this.as == null || this.as.getLooper() == null)) {
                this.as.getLooper().quitSafely();
            }
            HandlerThread handlerThread = new HandlerThread("bastetRspHandlerThread");
            handlerThread.start();
            int i = 0;
            while (!handlerThread.isAlive()) {
                int i2 = i + 1;
                try {
                    wait(10);
                    if (i2 % 100 == 0) {
                        c.eq("PushLog3413", "wait bastetRspHandlerThread start take time: " + (i2 * 10) + " ms");
                    }
                    if (i2 > 500) {
                        c.eq("PushLog3413", "reached the max retry times:500");
                        return false;
                    }
                    i = i2;
                } catch (Throwable e) {
                    c.es("PushLog3413", "InterruptedException error", e);
                }
            }
            if (handlerThread.getLooper() == null) {
                c.eq("PushLog3413", "looper is null when initMsgHandler");
                return false;
            }
            this.as = new b(this, handlerThread.getLooper());
            return true;
        } catch (Throwable e2) {
            c.es("PushLog3413", "initMsgHandler error:" + e2.getMessage(), e2);
            dl();
            return false;
        }
    }

    public long dh() {
        return 900000;
    }

    private void dn() {
        c.ep("PushLog3413", "enter clearBastetProxy!");
        if (this.at == null) {
            c.ep("PushLog3413", "enter clearBastetProxy, mHwBastet is null");
            return;
        }
        try {
            Method declaredMethod = this.at.getClass().getDeclaredMethod("clearBastetProxy", new Class[0]);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(this.at, new Object[0]);
            c.ep("PushLog3413", "clearBastetProxy success!");
        } catch (Throwable e) {
            c.es("PushLog3413", e.toString(), e);
        } catch (Throwable e2) {
            c.es("PushLog3413", e2.toString(), e2);
        } catch (Throwable e22) {
            c.es("PushLog3413", e22.toString(), e22);
        } catch (Throwable e222) {
            c.es("PushLog3413", e222.toString(), e222);
        } catch (Throwable e2222) {
            c.ev("PushLog3413", e2222.toString(), e2222);
        }
    }

    public static void dv(Context context, long j) {
        c.ep("PushLog3413", "send current push bestHeartBeat to powergenie for bastet:" + j);
        Intent intent = new Intent("com.huawei.android.push.bestHB");
        intent.putExtra("networkType", "bastet");
        intent.putExtra("heartbeat", j);
        intent.setPackage("com.huawei.powergenie");
        context.sendBroadcast(intent);
    }
}
