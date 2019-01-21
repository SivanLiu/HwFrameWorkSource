package com.huawei.android.pushagent.model.channel;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.model.channel.entity.b;
import com.huawei.android.pushagent.utils.d;
import java.net.Socket;
import java.net.SocketException;

public class a {
    private static volatile a bz = null;
    private static final Object cb = new Object();
    private Context by;
    private boolean ca = false;
    private b cc;

    public static a ea(Context context) {
        if (bz == null) {
            synchronized (cb) {
                if (bz == null) {
                    if (context != null) {
                        bz = new a(context);
                    } else {
                        bz = new a(PushService.abt().abu());
                    }
                    bz.ej();
                }
            }
        }
        return bz;
    }

    private a(Context context) {
        this.by = context;
    }

    private boolean ej() {
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "begin to init ChannelMgr");
        this.cc = new com.huawei.android.pushagent.model.channel.entity.a.b(null, this.by);
        return true;
    }

    public static com.huawei.android.pushagent.model.channel.entity.a ee(Context context) {
        return ea(context).ef().as;
    }

    public void ed() {
        eg(this.by);
        if (this.cc != null) {
            this.cc.cu();
        }
    }

    public void ec(long j) {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "next connect pushsvr will be after " + j);
        Intent intent = new Intent("com.huawei.action.CONNECT_PUSHSRV");
        intent.setPackage(this.by.getPackageName());
        com.huawei.android.pushagent.utils.tools.a.sa(this.by, intent, j);
    }

    public void eb(long j) {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "connect pushsvr without wakeup system after " + j);
        Intent intent = new Intent("com.huawei.action.CONNECT_PUSHSRV_NON_WAKEUP");
        intent.setPackage(this.by.getPackageName());
        com.huawei.android.pushagent.utils.tools.a.se(this.by, intent, j);
    }

    public b ef() {
        return this.cc;
    }

    public static b dz() {
        return ea(PushService.abt().abu()).cc;
    }

    public void ek(Intent intent) {
        String action = intent.getAction();
        this.ca = intent.getBooleanExtra("isOffSet", false);
        if ("com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT".equals(action)) {
            ei();
        } else if ("com.huawei.push.alarm.HEARTBEAT".equals(action)) {
            eh(intent);
        } else if ("android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) {
            el(action);
        }
    }

    private static void eg(Context context) {
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "enter ConnectMgr:cancelDelayAlarm");
        com.huawei.android.pushagent.utils.tools.a.sc(context, "com.huawei.action.CONNECT_PUSHSRV");
        com.huawei.android.pushagent.utils.tools.a.sc(context, "com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
        com.huawei.android.pushagent.utils.tools.a.sc(context, "com.huawei.push.alarm.HEARTBEAT");
    }

    private void ei() {
        com.huawei.android.pushagent.b.a.abc(70, String.valueOf(ee(this.by).bk(false)));
        com.huawei.android.pushagent.b.a.abd(81);
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "time out for wait heartbeat so reconnect");
        ee(this.by).bf(true, this.ca);
        Socket cz = ef().cz();
        boolean z = com.huawei.android.pushagent.model.prefs.a.ff(this.by).m2if();
        if (cz != null && z) {
            try {
                com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "setSoLinger 0 when close socket after heartbeat timeout");
                cz.setSoLinger(true, 0);
            } catch (SocketException e) {
                com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e.toString(), e);
            }
        }
        ef().cu();
    }

    private void eh(Intent intent) {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "heartbeatArrive");
        if (!(intent == null || -1 == d.yh(this.by))) {
            b ef = ef();
            if (ef.cp()) {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "heartbeatArrive, send heart beat");
                boolean booleanExtra = intent.getBooleanExtra("isHeartbeatReq", true);
                if (booleanExtra) {
                    ef.as.bg(false);
                }
                ef.as.ck(booleanExtra);
                ef.as.bt();
            } else {
                PushService.abv(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(this.by.getPackageName()));
            }
        }
    }

    private void el(String str) {
        if (str != null) {
            if (ef().cp()) {
                ee(this.by).ck(false);
                ee(this.by).bt();
            } else if (-1 != d.yh(this.by)) {
                com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "received " + str + ", but not Connect, go to connect!");
                PushService.abv(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(this.by.getPackageName()));
            } else {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "no net work, when recevice :" + str + ", do nothing");
            }
        }
    }
}
