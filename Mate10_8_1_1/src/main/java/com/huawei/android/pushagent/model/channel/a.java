package com.huawei.android.pushagent.model.channel;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.model.channel.entity.a.c;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
import com.huawei.android.pushagent.utils.tools.d;
import java.net.Socket;

public class a {
    private static volatile a gz = null;
    private static final Object ha = new Object();
    private Context gy;
    private com.huawei.android.pushagent.model.channel.entity.a hb;

    public static a ws(Context context) {
        if (gz == null) {
            synchronized (ha) {
                if (gz == null) {
                    if (context != null) {
                        gz = new a(context);
                    } else {
                        gz = new a(PushService.abd().abc());
                    }
                    gz.xa();
                }
            }
        }
        return gz;
    }

    private a(Context context) {
        this.gy = context;
    }

    private boolean xa() {
        b.x("PushLog2976", "begin to init ChannelMgr");
        this.hb = new c(null, this.gy);
        return true;
    }

    public static com.huawei.android.pushagent.model.channel.entity.c wx(Context context) {
        return ws(context).wt().fq;
    }

    public void wv() {
        wu(this.gy);
        if (this.hb != null) {
            this.hb.ve();
        }
    }

    public void ww(long j) {
        b.z("PushLog2976", "next connect pushsvr will be after " + j);
        Intent intent = new Intent("com.huawei.action.CONNECT_PUSHSRV");
        intent.setPackage(this.gy.getPackageName());
        d.p(this.gy, intent, j);
    }

    public com.huawei.android.pushagent.model.channel.entity.a wt() {
        return this.hb;
    }

    public static com.huawei.android.pushagent.model.channel.entity.a wr() {
        return ws(PushService.abd().abc()).hb;
    }

    public void xb(Intent intent) {
        String action = intent.getAction();
        String stringExtra = intent.getStringExtra("EXTRA_INTENT_TYPE");
        if ("com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT".equals(action)) {
            wz();
        } else if ("com.huawei.intent.action.PUSH".equals(action) && "com.huawei.android.push.intent.HEARTBEAT_REQ".equals(stringExtra)) {
            wy(intent);
        } else if ("android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) {
            xc(action);
        }
    }

    private static void wu(Context context) {
        b.x("PushLog2976", "enter ConnectMgr:cancelDelayAlarm");
        d.o(context, "com.huawei.action.CONNECT_PUSHSRV");
        d.o(context, "com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
        d.r(context, new Intent("com.huawei.intent.action.PUSH").putExtra("EXTRA_INTENT_TYPE", "com.huawei.android.push.intent.HEARTBEAT_REQ").setPackage(context.getPackageName()));
    }

    private void wz() {
        com.huawei.android.pushagent.b.a.aaj(70, String.valueOf(wx(this.gy).uj(false)));
        com.huawei.android.pushagent.b.a.aak(81);
        b.z("PushLog2976", "time out for wait heartbeat so reconnect");
        wx(this.gy).ue(true);
        Socket vd = wt().vd();
        boolean oo = i.mj(this.gy).oo();
        if (vd != null && oo) {
            try {
                b.x("PushLog2976", "setSoLinger 0 when close socket after heartbeat timeout");
                vd.setSoLinger(true, 0);
            } catch (Throwable e) {
                b.aa("PushLog2976", e.toString(), e);
            }
        }
        wt().ve();
    }

    private void wy(Intent intent) {
        b.z("PushLog2976", "heartbeatArrive");
        if (!(intent == null || -1 == f.fp(this.gy))) {
            com.huawei.android.pushagent.model.channel.entity.a wt = wt();
            if (wt.vc()) {
                b.z("PushLog2976", "heartbeatArrive, send heart beat");
                boolean booleanExtra = intent.getBooleanExtra("isHeartbeatReq", true);
                if (booleanExtra) {
                    wt.fq.uf(false);
                }
                wt.fq.vv(booleanExtra);
                wt.fq.uq();
            } else {
                PushService.aax(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(this.gy.getPackageName()));
            }
        }
    }

    private void xc(String str) {
        if (str != null) {
            if (wt().vc()) {
                wx(this.gy).vv(false);
                wx(this.gy).uq();
            } else if (-1 != f.fp(this.gy)) {
                b.x("PushLog2976", "received " + str + ", but not Connect, go to connect!");
                PushService.aax(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(this.gy.getPackageName()));
            } else {
                b.z("PushLog2976", "no net work, when recevice :" + str + ", do nothing");
            }
        }
    }
}
