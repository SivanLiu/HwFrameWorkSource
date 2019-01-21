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
import com.huawei.android.pushagent.model.prefs.m;
import com.huawei.android.pushagent.utils.d;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;

public class a {
    private static a hw = null;
    private PushBastet$BasteProxyStatus hs = PushBastet$BasteProxyStatus.Stoped;
    private Context ht;
    private Handler hu = null;
    private HwBastet hv = null;

    public static synchronized a xd(Context context) {
        synchronized (a.class) {
            a aVar;
            if (hw != null) {
                aVar = hw;
                return aVar;
            }
            hw = new a(context);
            aVar = hw;
            return aVar;
        }
    }

    private a(Context context) {
        this.ht = context;
    }

    public synchronized boolean wz() {
        try {
            if (xb()) {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "bastet has started, need not restart");
                return true;
            } else if (xi()) {
                com.huawei.android.pushagent.b.a.abd(73);
                if (xj(3, 900000)) {
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "startPushBastetProxy success");
                    xp(PushBastet$BasteProxyStatus.Started);
                    return true;
                }
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "startPushBastetProxy failed");
                return false;
            } else {
                com.huawei.android.pushagent.b.a.abd(72);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "init push bastet failed!");
                return false;
            }
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "startPushBastetProxy failed:" + e.getMessage(), e);
        }
    }

    public boolean xc() {
        if (1 != d.yh(this.ht)) {
            return xk() && xl();
        } else {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "not support bastet in wifi");
            return false;
        }
    }

    private boolean xi() {
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "initPushBastet");
        if (!xc()) {
            return false;
        }
        Socket cz = com.huawei.android.pushagent.model.channel.a.dz().cz();
        if (cz == null || !xh()) {
            return false;
        }
        try {
            this.hv = new HwBastet("PUSH_BASTET", cz, this.hu, this.ht);
            if (this.hv.isBastetAvailable()) {
                this.hv.reconnectSwitch(true);
                return true;
            }
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "isBastetAvailable false, can't use bastet.");
            xe();
            return false;
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.td("PushLog3414", "init bastet error", e);
            xe();
            return false;
        }
    }

    private boolean xl() {
        boolean id = com.huawei.android.pushagent.model.prefs.a.ff(this.ht).id();
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "isPushServerAllowBastet: " + id);
        return id;
    }

    private boolean xk() {
        try {
            Class.forName("com.huawei.android.bastet.HwBastet");
            return true;
        } catch (ClassNotFoundException e) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "bastet not exist");
            return false;
        }
    }

    private synchronized void xm() {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "enter quitLooper");
        try {
            if (!(this.hu == null || this.hu.getLooper() == null)) {
                this.hu.getLooper().quitSafely();
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "bastet loop quitSafely");
            }
            this.hu = null;
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "PushBastetListener release error", e);
        }
        return;
    }

    private void xn() {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "reConnectPush");
        xe();
        com.huawei.android.pushagent.model.channel.a.ea(this.ht).ef().cu();
        com.huawei.android.pushagent.b.a.abd(80);
        PushService.abv(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(this.ht.getPackageName()));
    }

    private void xf(Message message) {
        int i = message.arg1;
        int yh = d.yh(this.ht);
        com.huawei.android.pushagent.b.a.abc(71, com.huawei.android.pushagent.b.a.abb(m.mc(this.ht).mk(this.ht, yh), String.valueOf(4), String.valueOf(i)));
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "receive handler message BASTET_HEARTBEAT_CYCLE " + i);
        if (i > 10) {
            m.mc(this.ht).md(this.ht, 999);
            m.mc(this.ht).mf(this.ht, 999, ((long) i) * 1000);
            xj(i, ((long) i) * 1000);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "receive new bastet handler message BASTET_HEARTBEAT_CYCLE:" + i + " s.");
            xo(this.ht, ((long) i) * 1000);
        } else {
            xj(i, ((long) (i * 5)) * 60000);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "receive old bastet handler message BASTET_HEARTBEAT_CYCLE:" + i);
            xo(this.ht, ((long) i) * 1000);
        }
        try {
            this.hv.pauseHeartbeat();
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "adaptPushBastet error :" + e.toString(), e);
        }
    }

    private boolean xj(int i, long j) {
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "initPushHeartBeatDataContent");
        try {
            HeartBeatReqMessage heartBeatReqMessage = new HeartBeatReqMessage();
            heartBeatReqMessage.u((byte) ((int) Math.ceil((((double) j) * 1.0d) / 60000.0d)));
            byte[] dh = com.huawei.android.pushagent.model.channel.a.a.dh(heartBeatReqMessage.b(), false);
            byte[] dh2 = com.huawei.android.pushagent.model.channel.a.a.dh(new HeartBeatRspMessage().b(), true);
            int fw = com.huawei.android.pushagent.model.prefs.a.ff(this.ht).fw();
            this.hv.setAolHeartbeat(fw, dh, dh2);
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "set bastet heartbeat interval level as: " + fw);
            return true;
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "initPushHeartBeatDataContent error :" + e.toString(), e);
            return false;
        }
    }

    public void xp(PushBastet$BasteProxyStatus pushBastet$BasteProxyStatus) {
        this.hs = pushBastet$BasteProxyStatus;
    }

    public boolean xb() {
        return this.hv != null && PushBastet$BasteProxyStatus.Started == this.hs;
    }

    public void xe() {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "resetBastet");
        xg();
        this.hv = null;
        xp(PushBastet$BasteProxyStatus.Stoped);
        xm();
        com.huawei.android.pushagent.model.channel.a.dz().cr(false);
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "after setExistResetBastetAlarm");
        com.huawei.android.pushagent.utils.tools.a.sc(this.ht, "com.huawei.android.push.intent.RESET_BASTET");
    }

    private synchronized boolean xh() {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "initMsgHandler");
        try {
            if (!(this.hu == null || this.hu.getLooper() == null)) {
                this.hu.getLooper().quitSafely();
            }
            HandlerThread handlerThread = new HandlerThread("bastetRspHandlerThread");
            handlerThread.start();
            int i = 0;
            while (!handlerThread.isAlive()) {
                int i2 = i + 1;
                try {
                    wait(10);
                    if (i2 % 100 == 0) {
                        com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "wait bastetRspHandlerThread start take time: " + (i2 * 10) + " ms");
                    }
                    if (i2 > 500) {
                        com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "reached the max retry times:500");
                        return false;
                    }
                    i = i2;
                } catch (InterruptedException e) {
                    com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "InterruptedException error", e);
                }
            }
            if (handlerThread.getLooper() == null) {
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "looper is null when initMsgHandler");
                return false;
            }
            this.hu = new b(this, handlerThread.getLooper());
            return true;
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "initMsgHandler error:" + e2.getMessage(), e2);
            xe();
            return false;
        }
    }

    public long xa() {
        return 900000;
    }

    private void xg() {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "enter clearBastetProxy!");
        if (this.hv == null) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "enter clearBastetProxy, mHwBastet is null");
            return;
        }
        try {
            Method declaredMethod = this.hv.getClass().getDeclaredMethod("clearBastetProxy", new Class[0]);
            declaredMethod.setAccessible(true);
            declaredMethod.invoke(this.hv, new Object[0]);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "clearBastetProxy success!");
        } catch (SecurityException e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e.toString(), e);
        } catch (NoSuchMethodException e2) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e2.toString(), e2);
        } catch (IllegalArgumentException e3) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e3.toString(), e3);
        } catch (IllegalAccessException e4) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e4.toString(), e4);
        } catch (InvocationTargetException e5) {
            com.huawei.android.pushagent.utils.b.a.td("PushLog3414", e5.toString(), e5);
        }
    }

    public static void xo(Context context, long j) {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "send current push bestHeartBeat to powergenie for bastet:" + j);
        Intent intent = new Intent("com.huawei.android.push.bestHB");
        intent.putExtra("networkType", "bastet");
        intent.putExtra("heartbeat", j);
        intent.setPackage("com.huawei.powergenie");
        context.sendBroadcast(intent);
    }
}
