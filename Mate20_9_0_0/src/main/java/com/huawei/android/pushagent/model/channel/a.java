package com.huawei.android.pushagent.model.channel;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.tools.d;
import java.net.Socket;

public class a {
    private static volatile a eu = null;
    private static final Object ew = new Object();
    private Context et;
    private boolean ev = false;
    private com.huawei.android.pushagent.model.channel.entity.a ex;

    public static a nt(Context context) {
        if (eu == null) {
            synchronized (ew) {
                if (eu == null) {
                    if (context != null) {
                        eu = new a(context);
                    } else {
                        eu = new a(PushService.abp().abq());
                    }
                    eu.oc();
                }
            }
        }
        return eu;
    }

    private a(Context context) {
        this.et = context;
    }

    private boolean oc() {
        c.er("PushLog3413", "begin to init ChannelMgr");
        this.ex = new com.huawei.android.pushagent.model.channel.entity.a.c(null, this.et);
        return true;
    }

    public static com.huawei.android.pushagent.model.channel.entity.c nv(Context context) {
        return nt(context).nw().dk;
    }

    public void nu() {
        nx(this.et);
        if (this.ex != null) {
            this.ex.md();
        }
    }

    public void nz(long j) {
        c.ep("PushLog3413", "next connect pushsvr will be after " + j);
        Intent intent = new Intent("com.huawei.action.CONNECT_PUSHSRV");
        intent.setPackage(this.et.getPackageName());
        d.cw(this.et, intent, j);
    }

    public void ny(long j) {
        c.ep("PushLog3413", "connect pushsvr without wakeup system after " + j);
        Intent intent = new Intent("com.huawei.action.CONNECT_PUSHSRV_NON_WAKEUP");
        intent.setPackage(this.et.getPackageName());
        d.cx(this.et, intent, j);
    }

    public com.huawei.android.pushagent.model.channel.entity.a nw() {
        return this.ex;
    }

    public static com.huawei.android.pushagent.model.channel.entity.a ns() {
        return nt(PushService.abp().abq()).ex;
    }

    public void od(Intent intent) {
        String action = intent.getAction();
        this.ev = intent.getBooleanExtra("isOffSet", false);
        if ("com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT".equals(action)) {
            ob();
        } else if ("com.huawei.push.alarm.HEARTBEAT".equals(action)) {
            oa(intent);
        } else if ("android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) {
            oe(action);
        }
    }

    private static void nx(Context context) {
        c.er("PushLog3413", "enter ConnectMgr:cancelDelayAlarm");
        d.cy(context, "com.huawei.action.CONNECT_PUSHSRV");
        d.cy(context, "com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
        d.cy(context, "com.huawei.push.alarm.HEARTBEAT");
    }

    private void ob() {
        com.huawei.android.pushagent.a.a.hq(70, String.valueOf(nv(this.et).ld(false)));
        com.huawei.android.pushagent.a.a.hx(81);
        c.ep("PushLog3413", "time out for wait heartbeat so reconnect");
        nv(this.et).ky(true, this.ev);
        Socket mi = nw().mi();
        boolean tg = k.rh(this.et).tg();
        if (mi != null && tg) {
            try {
                c.er("PushLog3413", "setSoLinger 0 when close socket after heartbeat timeout");
                mi.setSoLinger(true, 0);
            } catch (Throwable e) {
                c.es("PushLog3413", e.toString(), e);
            }
        }
        nw().md();
    }

    private void oa(Intent intent) {
        c.ep("PushLog3413", "heartbeatArrive");
        if (!(intent == null || -1 == g.fw(this.et))) {
            com.huawei.android.pushagent.model.channel.entity.a nw = nw();
            if (nw.mk()) {
                c.ep("PushLog3413", "heartbeatArrive, send heart beat");
                boolean booleanExtra = intent.getBooleanExtra("isHeartbeatReq", true);
                if (booleanExtra) {
                    nw.dk.kz(false);
                }
                nw.dk.mw(booleanExtra);
                nw.dk.lm();
            } else {
                PushService.abr(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(this.et.getPackageName()));
            }
        }
    }

    private void oe(String str) {
        if (str != null) {
            if (nw().mk()) {
                nv(this.et).mw(false);
                nv(this.et).lm();
            } else if (-1 != g.fw(this.et)) {
                c.er("PushLog3413", "received " + str + ", but not Connect, go to connect!");
                PushService.abr(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(this.et.getPackageName()));
            } else {
                c.ep("PushLog3413", "no net work, when recevice :" + str + ", do nothing");
            }
        }
    }
}
