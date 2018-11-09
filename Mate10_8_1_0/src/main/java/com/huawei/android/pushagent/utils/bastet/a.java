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
import com.huawei.android.pushagent.model.prefs.d;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
import java.lang.reflect.Method;
import java.net.Socket;

public class a {
    private static a aa = null;
    private PushBastet$BasteProxyStatus w = PushBastet$BasteProxyStatus.Stoped;
    private Context x;
    private Handler y = null;
    private HwBastet z = null;

    public static synchronized a cg(Context context) {
        synchronized (a.class) {
            if (aa != null) {
                a aVar = aa;
                return aVar;
            }
            aa = new a(context);
            aVar = aa;
            return aVar;
        }
    }

    private a(Context context) {
        this.x = context;
    }

    public synchronized boolean cu() {
        try {
            if (cn()) {
                b.z("PushLog2976", "bastet has started, need not restart");
                return true;
            } else if (cj()) {
                com.huawei.android.pushagent.b.a.aak(73);
                if (ck(3, 900000)) {
                    b.z("PushLog2976", "startPushBastetProxy success");
                    ct(PushBastet$BasteProxyStatus.Started);
                    return true;
                }
                b.z("PushLog2976", "startPushBastetProxy failed");
                return false;
            } else {
                com.huawei.android.pushagent.b.a.aak(72);
                b.z("PushLog2976", "init push bastet failed!");
                return false;
            }
        } catch (Throwable e) {
            b.aa("PushLog2976", "startPushBastetProxy failed:" + e.getMessage(), e);
        }
    }

    public boolean cl() {
        if (1 != f.fp(this.x)) {
            return cm() && co();
        } else {
            b.z("PushLog2976", "not support bastet in wifi");
            return false;
        }
    }

    private boolean cj() {
        b.x("PushLog2976", "initPushBastet");
        if (!cl()) {
            return false;
        }
        Socket vd = com.huawei.android.pushagent.model.channel.a.wr().vd();
        if (vd == null || !ci()) {
            return false;
        }
        try {
            this.z = new HwBastet("PUSH_BASTET", vd, this.y, this.x);
            if (this.z.isBastetAvailable()) {
                this.z.reconnectSwitch(true);
                return true;
            }
            b.z("PushLog2976", "isBastetAvailable false, can't use bastet.");
            cr();
            return false;
        } catch (Throwable e) {
            b.ac("PushLog2976", "init bastet error", e);
            cr();
            return false;
        }
    }

    private boolean co() {
        boolean mv = i.mj(this.x).mv();
        b.x("PushLog2976", "isPushServerAllowBastet: " + mv);
        return mv;
    }

    private boolean cm() {
        try {
            Class.forName("com.huawei.android.bastet.HwBastet");
            return true;
        } catch (ClassNotFoundException e) {
            b.y("PushLog2976", "bastet not exist");
            return false;
        }
    }

    private synchronized void cp() {
        b.z("PushLog2976", "enter quitLooper");
        try {
            if (!(this.y == null || this.y.getLooper() == null)) {
                this.y.getLooper().quitSafely();
                b.z("PushLog2976", "bastet loop quitSafely");
            }
            this.y = null;
        } catch (Throwable e) {
            b.aa("PushLog2976", "PushBastetListener release error", e);
        }
    }

    private void cq() {
        b.z("PushLog2976", "reConnectPush");
        cr();
        com.huawei.android.pushagent.model.channel.a.ws(this.x).wt().ve();
        com.huawei.android.pushagent.b.a.aak(80);
        PushService.aax(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(this.x.getPackageName()));
    }

    private void ce(Message message) {
        int i = message.arg1;
        int fp = f.fp(this.x);
        com.huawei.android.pushagent.b.a.aaj(71, com.huawei.android.pushagent.b.a.aal(d.lc(this.x).ld(this.x, fp), String.valueOf(4), String.valueOf(i)));
        b.z("PushLog2976", "receive handler message BASTET_HEARTBEAT_CYCLE " + i);
        if (i > 10) {
            d.lc(this.x).le(this.x, 999);
            d.lc(this.x).lf(this.x, 999, ((long) i) * 1000);
            ck(i, ((long) i) * 1000);
            b.z("PushLog2976", "receive new bastet handler message BASTET_HEARTBEAT_CYCLE:" + i + " s.");
            cs(this.x, ((long) i) * 1000);
        } else {
            ck(i, ((long) (i * 5)) * 60000);
            b.z("PushLog2976", "receive old bastet handler message BASTET_HEARTBEAT_CYCLE:" + i);
            cs(this.x, ((long) i) * 1000);
        }
        try {
            this.z.pauseHeartbeat();
        } catch (Throwable e) {
            b.aa("PushLog2976", "adaptPushBastet error :" + e.toString(), e);
        }
    }

    private boolean ck(int i, long j) {
        b.x("PushLog2976", "initPushHeartBeatDataContent");
        try {
            HeartBeatReqMessage heartBeatReqMessage = new HeartBeatReqMessage();
            heartBeatReqMessage.zo((byte) ((int) Math.ceil((((double) j) * 1.0d) / 60000.0d)));
            byte[] wf = com.huawei.android.pushagent.model.channel.a.b.wf(heartBeatReqMessage.yp(), false);
            byte[] wf2 = com.huawei.android.pushagent.model.channel.a.b.wf(new HeartBeatRspMessage().yp(), true);
            int mw = i.mj(this.x).mw();
            this.z.setAolHeartbeat(mw, wf, wf2);
            b.x("PushLog2976", "set bastet heartbeat interval level as: " + mw);
            return true;
        } catch (Throwable e) {
            b.aa("PushLog2976", "initPushHeartBeatDataContent error :" + e.toString(), e);
            return false;
        }
    }

    public void ct(PushBastet$BasteProxyStatus pushBastet$BasteProxyStatus) {
        this.w = pushBastet$BasteProxyStatus;
    }

    public boolean cn() {
        return this.z != null && PushBastet$BasteProxyStatus.Started == this.w;
    }

    public void cr() {
        b.z("PushLog2976", "resetBastet");
        cf();
        this.z = null;
        ct(PushBastet$BasteProxyStatus.Stoped);
        cp();
        com.huawei.android.pushagent.model.channel.a.wr().vf(false);
        b.z("PushLog2976", "after setExistResetBastetAlarm");
        com.huawei.android.pushagent.utils.tools.d.o(this.x, "com.huawei.android.push.intent.RESET_BASTET");
    }

    private synchronized boolean ci() {
        b.z("PushLog2976", "initMsgHandler");
        try {
            if (!(this.y == null || this.y.getLooper() == null)) {
                this.y.getLooper().quitSafely();
            }
            HandlerThread handlerThread = new HandlerThread("bastetRspHandlerThread");
            handlerThread.start();
            int i = 0;
            while (!handlerThread.isAlive()) {
                int i2 = i + 1;
                try {
                    wait(10);
                    if (i2 % 100 == 0) {
                        b.y("PushLog2976", "wait bastetRspHandlerThread start take time: " + (i2 * 10) + " ms");
                    }
                    if (i2 > 500) {
                        b.y("PushLog2976", "reached the max retry times:500");
                        return false;
                    }
                    i = i2;
                } catch (Throwable e) {
                    b.aa("PushLog2976", "InterruptedException error", e);
                }
            }
            if (handlerThread.getLooper() == null) {
                b.y("PushLog2976", "looper is null when initMsgHandler");
                return false;
            }
            this.y = new b(this, handlerThread.getLooper());
            return true;
        } catch (Throwable e2) {
            b.aa("PushLog2976", "initMsgHandler error:" + e2.getMessage(), e2);
            cr();
            return false;
        }
    }

    public long ch() {
        return 900000;
    }

    private void cf() {
        b.z("PushLog2976", "enter clearBastetProxy!");
        if (this.z == null) {
            b.z("PushLog2976", "enter clearBastetProxy, mHwBastet is null");
            return;
        }
        try {
            Method declaredMethod = this.z.getClass().getDeclaredMethod("clearBastetProxy", new Class[0]);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(this.z, new Object[0]);
            b.z("PushLog2976", "clearBastetProxy success!");
        } catch (Throwable e) {
            b.aa("PushLog2976", e.toString(), e);
        } catch (Throwable e2) {
            b.aa("PushLog2976", e2.toString(), e2);
        } catch (Throwable e22) {
            b.aa("PushLog2976", e22.toString(), e22);
        } catch (Throwable e222) {
            b.aa("PushLog2976", e222.toString(), e222);
        } catch (Throwable e2222) {
            b.ac("PushLog2976", e2222.toString(), e2222);
        }
    }

    public static void cs(Context context, long j) {
        b.z("PushLog2976", "send current push bestHeartBeat to powergenie for bastet:" + j);
        Intent intent = new Intent("com.huawei.android.push.bestHB");
        intent.putExtra("networkType", "bastet");
        intent.putExtra("heartbeat", j);
        intent.setPackage("com.huawei.powergenie");
        context.sendBroadcast(intent);
    }
}
