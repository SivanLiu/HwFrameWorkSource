package com.huawei.android.pushagent.model.a;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.model.c.e;
import com.huawei.android.pushagent.model.channel.a;
import com.huawei.android.pushagent.model.flowcontrol.ReconnectMgr$RECONNECTEVENT;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
import com.huawei.android.pushagent.utils.tools.d;

public class c implements d {
    private int ck = -1;

    public c(Context context) {
    }

    public void onReceive(Context context, Intent intent) {
        try {
            b.x("PushLog2976", "enter ConnectReceiver:onReceive");
            String action = intent.getAction();
            String stringExtra = intent.getStringExtra("EXTRA_INTENT_TYPE");
            jz(intent, action);
            if ("com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT".equals(action)) {
                a.ws(context).xb(intent);
                return;
            }
            if (jy(context, intent, action, stringExtra)) {
                a.ws(context).xb(intent);
            } else if (jx(context, action, stringExtra)) {
                jw(context, intent);
            } else if ("com.huawei.android.push.intent.CONNECTING".equals(action)) {
                b.z("PushLog2976", "receive the ACTION_CONNECTING action.");
            } else if ("com.huawei.android.push.intent.CONNECTED".equals(action)) {
                b.z("PushLog2976", "receive the ACTION_CONNECTED action.");
            } else if ("com.huawei.intent.action.PUSH_OFF".equals(action)) {
                action = intent.getStringExtra("Remote_Package_Name");
                if (action == null || (action.equals(context.getPackageName()) ^ 1) != 0) {
                    b.x("PushLog2976", "need stop PkgName:" + action + " is not me, need not stop!");
                    return;
                }
                a.ws(context).wv();
                if (com.huawei.android.pushagent.utils.bastet.a.cg(context).cn()) {
                    com.huawei.android.pushagent.utils.bastet.a.cg(context).cr();
                }
                PushService.abb();
            }
        } catch (Throwable e) {
            b.ae("PushLog2976", e.toString(), e);
        }
    }

    private void jz(Intent intent, String str) {
        try {
            long longExtra = intent.getLongExtra("expectTriggerTime", 0);
            if (longExtra > 0) {
                b.z("PushLog2976", str + " alarm trigger, expectTriggerTime:" + longExtra + ", current trigger time:" + System.currentTimeMillis());
            }
        } catch (Exception e) {
            b.x("PushLog2976", "get expectTriggerTime exception.");
        }
    }

    private void jw(Context context, Intent intent) {
        try {
            com.huawei.android.pushagent.b.a.aak(30);
            String action = intent.getAction();
            String stringExtra = intent.getStringExtra("PkgName");
            b.z("PushLog2976", "PkgName:" + stringExtra + "，action:" + action);
            if (!e.ti(context)) {
                com.huawei.android.pushagent.b.a.aak(31);
                com.huawei.android.pushagent.b.a.aak(83);
                a.ws(context).wv();
                b.z("PushLog2976", "no push client, stop push apk service");
                d.p(context, new Intent("com.huawei.intent.action.PUSH_OFF").setPackage(context.getPackageName()).putExtra("Remote_Package_Name", context.getPackageName()), i.mj(context).pk() * 1000);
            } else if (!f.gr(context, stringExtra)) {
            } else {
                if (i.mj(context).isValid()) {
                    boolean z;
                    if ("com.huawei.android.push.intent.TRS_QUERY_SUCCESS".equals(action)) {
                        com.huawei.android.pushagent.b.a.aak(33);
                        a.wr().fq.uo();
                        com.huawei.android.pushagent.model.flowcontrol.a.hw(context);
                        com.huawei.android.pushagent.model.flowcontrol.b.ib(context).il(context);
                    }
                    int fp = f.fp(context);
                    if (-1 == fp || fp != this.ck) {
                        if (-1 == fp) {
                            b.x("PushLog2976", "no network in ConnectReceiver:connect, so close socket");
                        } else {
                            b.x("PushLog2976", "net work switch from:" + this.ck + " to " + fp);
                        }
                        try {
                            a.ws(context).wv();
                        } catch (Throwable e) {
                            b.aa("PushLog2976", "call channel.close cause exception:" + e.toString(), e);
                        }
                    }
                    if (this.ck != fp) {
                        z = true;
                    } else {
                        z = false;
                    }
                    b.z("PushLog2976", "lastnetWorkType:" + this.ck + " " + "curNetWorkType:" + fp + ", [-1:NONE, 0:MOBILE, 1:WIFI]");
                    this.ck = fp;
                    if (fp == 0 && k.pt(context).qk() == 0) {
                        b.z("PushLog2976", "It is mobile network and network policy is close of NC, so not connect push.");
                        return;
                    } else {
                        jv(context, action, z);
                        return;
                    }
                }
                com.huawei.android.pushagent.b.a.aak(32);
                b.z("PushLog2976", "connect srv: TRS is invalid, so need to query TRS");
                com.huawei.android.pushagent.model.c.c.sp(context).sq(false);
            }
        } catch (Exception e2) {
            b.y("PushLog2976", "call switchChannel cause Exceptino:" + e2.toString());
        }
    }

    private boolean jy(Context context, Intent intent, String str, String str2) {
        if (("com.huawei.intent.action.PUSH".equals(str) && "com.huawei.android.push.intent.HEARTBEAT_REQ".equals(str2)) || "android.intent.action.TIME_SET".equals(str)) {
            return true;
        }
        return "android.intent.action.TIMEZONE_CHANGED".equals(str);
    }

    private boolean jx(Context context, String str, String str2) {
        if ("com.huawei.push.action.NET_CHANGED".equals(str) || "com.huawei.action.CONNECT_PUSHSRV".equals(str) || "com.huawei.action.CONNECT_PUSHSRV_PUSHSRV".equals(str) || "com.huawei.android.push.intent.TRS_QUERY_SUCCESS".equals(str)) {
            return true;
        }
        return "com.huawei.intent.action.PUSH".equals(str) ? "com.huawei.intent.action.PUSH_ON".equals(str2) : false;
    }

    private void jv(Context context, String str, boolean z) {
        if (context == null || str == null) {
            b.y("PushLog2976", "context or action is null");
            return;
        }
        if ("com.huawei.action.CONNECT_PUSHSRV_PUSHSRV".equals(str)) {
            b.x("PushLog2976", "get " + str + " so get a pushSrv to connect");
            a.wr().uu(true);
        } else if (a.ws(context).wt().vc()) {
            com.huawei.android.pushagent.b.a.aak(36);
            b.x("PushLog2976", "pushChannel already connect");
        } else {
            b.x("PushLog2976", "get " + str + " so get a srv to connect");
            if (z) {
                com.huawei.android.pushagent.model.flowcontrol.b.ib(context).ik(context, ReconnectMgr$RECONNECTEVENT.NETWORK_CHANGE, new Bundle());
            }
            a.ws(context).wt().uu(false);
        }
    }
}
