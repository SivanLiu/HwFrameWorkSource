package com.huawei.android.pushagent.model.a;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.model.b.f;
import com.huawei.android.pushagent.model.channel.a;
import com.huawei.android.pushagent.model.flowcontrol.ReconnectMgr$RECONNECTEVENT;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;

public class d implements a {
    private int gp = -1;

    public d(Context context) {
    }

    public void onReceive(Context context, Intent intent) {
        try {
            c.er("PushLog3413", "enter ConnectReceiver:onReceive");
            String action = intent.getAction();
            wx(intent, action);
            if ("com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT".equals(action)) {
                a.nt(context).od(intent);
                return;
            }
            if (ww(action)) {
                a.nt(context).od(intent);
            } else if (wv(action)) {
                wu(context, intent);
            } else if ("com.huawei.android.push.intent.CONNECTING".equals(action)) {
                c.ep("PushLog3413", "receive the ACTION_CONNECTING action.");
            } else if ("com.huawei.android.push.intent.CONNECTED".equals(action)) {
                c.ep("PushLog3413", "receive the ACTION_CONNECTED action.");
            } else if ("com.huawei.intent.action.PUSH_OFF".equals(action)) {
                action = intent.getStringExtra("Remote_Package_Name");
                if (action == null || (action.equals(context.getPackageName()) ^ 1) != 0) {
                    c.er("PushLog3413", "need stop PkgName:" + action + " is not me, need not stop!");
                    return;
                }
                a.nt(context).nu();
                if (com.huawei.android.pushagent.utils.bastet.a.dk(context).di()) {
                    com.huawei.android.pushagent.utils.bastet.a.dk(context).dl();
                }
                PushService.abv();
            }
        } catch (Throwable e) {
            c.et("PushLog3413", e.toString(), e);
        }
    }

    private void wx(Intent intent, String str) {
        try {
            long longExtra = intent.getLongExtra("expectTriggerTime", 0);
            boolean z = false;
            c.ep("PushLog3413", str + " alarm trigger, expectTriggerTime:" + longExtra + ", current trigger time:" + System.currentTimeMillis());
            long currentTimeMillis = System.currentTimeMillis() - longExtra;
            if (longExtra > 0 && currentTimeMillis > 10000) {
                z = true;
            }
            intent.putExtra("isOffSet", z);
        } catch (Exception e) {
            c.er("PushLog3413", "get expectTriggerTime exception.");
        }
    }

    private void wu(Context context, Intent intent) {
        try {
            com.huawei.android.pushagent.a.a.hx(30);
            String action = intent.getAction();
            String stringExtra = intent.getStringExtra("PkgName");
            if (!com.huawei.android.pushagent.model.b.a.wz(context)) {
                com.huawei.android.pushagent.a.a.hx(31);
                com.huawei.android.pushagent.a.a.hx(83);
                a.nt(context).nu();
                c.ep("PushLog3413", "no push client, stop push apk service");
                if (a.ns().mk()) {
                    com.huawei.android.pushagent.utils.tools.d.cw(context, new Intent("com.huawei.intent.action.PUSH_OFF").setPackage(context.getPackageName()).putExtra("Remote_Package_Name", context.getPackageName()), k.rh(context).ue() * 1000);
                }
            } else if (!g.gj(context, stringExtra)) {
            } else {
                if (k.rh(context).isValid()) {
                    boolean z;
                    if ("com.huawei.android.push.intent.TRS_QUERY_SUCCESS".equals(action)) {
                        com.huawei.android.pushagent.a.a.hx(33);
                        a.ns().dk.lj();
                        com.huawei.android.pushagent.model.flowcontrol.c.abh(context);
                        com.huawei.android.pushagent.model.flowcontrol.a.zx(context).aah(context);
                    }
                    int fw = g.fw(context);
                    if (-1 == fw || fw != this.gp) {
                        if (-1 == fw) {
                            c.er("PushLog3413", "no network in ConnectReceiver:connect, so close socket");
                        } else {
                            c.er("PushLog3413", "net work switch from:" + this.gp + " to " + fw);
                        }
                        try {
                            a.nt(context).nu();
                        } catch (Throwable e) {
                            c.es("PushLog3413", "call channel.close cause exception:" + e.toString(), e);
                        }
                    }
                    if (this.gp != fw) {
                        z = true;
                    } else {
                        z = false;
                    }
                    c.ep("PushLog3413", "lastnetWorkType:" + this.gp + " " + "curNetWorkType:" + fw + ", [-1:NONE, 0:MOBILE, 1:WIFI]");
                    this.gp = fw;
                    if (a.ns().dk.lg() || (z && this.gp != -1)) {
                        a.ns().dk.lj();
                        com.huawei.android.pushagent.model.prefs.a.of(context).oh();
                    }
                    if (fw == 0 && l.ul(context).uy() == 0) {
                        c.ep("PushLog3413", "It is mobile network and network policy is close of NC, so not connect push.");
                        return;
                    } else {
                        wt(context, action, z);
                        return;
                    }
                }
                com.huawei.android.pushagent.a.a.hx(32);
                c.ep("PushLog3413", "connect srv: TRS is invalid, so need to query TRS");
                f.yc(context).yd();
            }
        } catch (Exception e2) {
            c.eq("PushLog3413", "call switchChannel cause Exceptino:" + e2.toString());
        }
    }

    private boolean ww(String str) {
        if ("com.huawei.push.alarm.HEARTBEAT".equals(str) || "android.intent.action.TIME_SET".equals(str)) {
            return true;
        }
        return "android.intent.action.TIMEZONE_CHANGED".equals(str);
    }

    private boolean wv(String str) {
        if ("com.huawei.push.action.NET_CHANGED".equals(str) || "com.huawei.action.CONNECT_PUSHSRV".equals(str) || "com.huawei.action.CONNECT_PUSHSRV_NON_WAKEUP".equals(str) || "com.huawei.action.CONNECT_PUSHSRV_FORCE".equals(str)) {
            return true;
        }
        return "com.huawei.android.push.intent.TRS_QUERY_SUCCESS".equals(str);
    }

    private void wt(Context context, String str, boolean z) {
        if (context == null || str == null) {
            c.eq("PushLog3413", "context or action is null");
            return;
        }
        if ("com.huawei.action.CONNECT_PUSHSRV_FORCE".equals(str)) {
            c.er("PushLog3413", "get " + str + " so get a pushSrv to connect");
            a.ns().lt(true, str);
        } else if (a.nt(context).nw().mk()) {
            com.huawei.android.pushagent.a.a.hx(36);
            c.er("PushLog3413", "pushChannel already connect");
        } else {
            c.er("PushLog3413", "get " + str + " so get a srv to connect");
            if (z) {
                com.huawei.android.pushagent.model.flowcontrol.a.zx(context).aag(context, ReconnectMgr$RECONNECTEVENT.NETWORK_CHANGE, new Bundle());
            }
            a.nt(context).nw().lt(false, str);
        }
    }
}
