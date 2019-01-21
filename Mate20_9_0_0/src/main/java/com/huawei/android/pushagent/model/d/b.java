package com.huawei.android.pushagent.model.d;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.model.c.f;
import com.huawei.android.pushagent.model.flowcontrol.ReconnectMgr$RECONNECTEVENT;
import com.huawei.android.pushagent.model.prefs.e;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.d;

public class b implements c {
    private int fz = -1;

    public b(Context context) {
    }

    public void onReceive(Context context, Intent intent) {
        try {
            a.st("PushLog3414", "enter ConnectReceiver:onReceive");
            String action = intent.getAction();
            rp(intent, action);
            if ("com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT".equals(action)) {
                com.huawei.android.pushagent.model.channel.a.ea(context).ek(intent);
                return;
            }
            if (ro(action)) {
                com.huawei.android.pushagent.model.channel.a.ea(context).ek(intent);
            } else if (rn(action)) {
                rm(context, intent);
            } else if ("com.huawei.android.push.intent.CONNECTING".equals(action)) {
                a.sv("PushLog3414", "receive the ACTION_CONNECTING action.");
            } else if ("com.huawei.android.push.intent.CONNECTED".equals(action)) {
                a.sv("PushLog3414", "receive the ACTION_CONNECTED action.");
            } else if ("com.huawei.intent.action.PUSH_OFF".equals(action)) {
                action = intent.getStringExtra("Remote_Package_Name");
                if (action == null || (action.equals(context.getPackageName()) ^ 1) != 0) {
                    a.st("PushLog3414", "need stop PkgName:" + action + " is not me, need not stop!");
                    return;
                }
                com.huawei.android.pushagent.model.channel.a.ea(context).ed();
                if (com.huawei.android.pushagent.utils.bastet.a.xd(context).xb()) {
                    com.huawei.android.pushagent.utils.bastet.a.xd(context).xe();
                }
                PushService.abw();
            }
        } catch (Exception e) {
            a.sy("PushLog3414", e.toString(), e);
        }
    }

    private void rp(Intent intent, String str) {
        try {
            long longExtra = intent.getLongExtra("expectTriggerTime", 0);
            boolean z = false;
            a.sv("PushLog3414", str + " alarm trigger, expectTriggerTime:" + longExtra + ", current trigger time:" + System.currentTimeMillis());
            long currentTimeMillis = System.currentTimeMillis() - longExtra;
            if (longExtra > 0 && currentTimeMillis > 10000) {
                z = true;
            }
            intent.putExtra("isOffSet", z);
        } catch (Exception e) {
            a.st("PushLog3414", "get expectTriggerTime exception.");
        }
    }

    private void rm(Context context, Intent intent) {
        try {
            com.huawei.android.pushagent.b.a.abd(30);
            String action = intent.getAction();
            String stringExtra = intent.getStringExtra("PkgName");
            if (!f.qn(context)) {
                com.huawei.android.pushagent.b.a.abd(31);
                com.huawei.android.pushagent.b.a.abd(83);
                com.huawei.android.pushagent.model.channel.a.ea(context).ed();
                a.sv("PushLog3414", "no push client, stop push apk service");
                if (com.huawei.android.pushagent.model.channel.a.dz().cp()) {
                    com.huawei.android.pushagent.utils.tools.a.sa(context, new Intent("com.huawei.intent.action.PUSH_OFF").setPackage(context.getPackageName()).putExtra("Remote_Package_Name", context.getPackageName()), com.huawei.android.pushagent.model.prefs.a.ff(context).hu() * 1000);
                }
            } else if (!d.yu(context, stringExtra)) {
            } else {
                if (com.huawei.android.pushagent.model.prefs.a.ff(context).isValid()) {
                    boolean z;
                    if ("com.huawei.android.push.intent.TRS_QUERY_SUCCESS".equals(action)) {
                        com.huawei.android.pushagent.b.a.abd(33);
                        com.huawei.android.pushagent.model.channel.a.dz().as.bq();
                        com.huawei.android.pushagent.model.flowcontrol.a.nj(context);
                        com.huawei.android.pushagent.model.flowcontrol.b.nz(context).oc(context);
                    }
                    int yh = d.yh(context);
                    if (-1 == yh || yh != this.fz) {
                        if (-1 == yh) {
                            a.st("PushLog3414", "no network in ConnectReceiver:connect, so close socket");
                        } else {
                            a.st("PushLog3414", "net work switch from:" + this.fz + " to " + yh);
                        }
                        try {
                            com.huawei.android.pushagent.model.channel.a.ea(context).ed();
                        } catch (Exception e) {
                            a.sw("PushLog3414", "call channel.close cause exception:" + e.toString(), e);
                        }
                    }
                    if (this.fz != yh) {
                        z = true;
                    } else {
                        z = false;
                    }
                    a.sv("PushLog3414", "lastnetWorkType:" + this.fz + " " + "curNetWorkType:" + yh + ", [-1:NONE, 0:MOBILE, 1:WIFI]");
                    this.fz = yh;
                    if (com.huawei.android.pushagent.model.channel.a.dz().as.bn() || (z && this.fz != -1)) {
                        com.huawei.android.pushagent.model.channel.a.dz().as.bq();
                        com.huawei.android.pushagent.model.prefs.f.ke(context).kg();
                    }
                    if (yh == 0 && e.jj(context).jr() == 0) {
                        a.sv("PushLog3414", "It is mobile network and network policy is close of NC, so not connect push.");
                        return;
                    } else {
                        rl(context, action, z);
                        return;
                    }
                }
                com.huawei.android.pushagent.b.a.abd(32);
                a.sv("PushLog3414", "connect srv: TRS is invalid, so need to query TRS");
                com.huawei.android.pushagent.model.c.e.pw(context).py();
            }
        } catch (Exception e2) {
            a.su("PushLog3414", "call switchChannel cause Exceptino:" + e2.toString());
        }
    }

    private boolean ro(String str) {
        if ("com.huawei.push.alarm.HEARTBEAT".equals(str) || "android.intent.action.TIME_SET".equals(str)) {
            return true;
        }
        return "android.intent.action.TIMEZONE_CHANGED".equals(str);
    }

    private boolean rn(String str) {
        if ("com.huawei.push.action.NET_CHANGED".equals(str) || "com.huawei.action.CONNECT_PUSHSRV".equals(str) || "com.huawei.action.CONNECT_PUSHSRV_NON_WAKEUP".equals(str) || "com.huawei.action.CONNECT_PUSHSRV_FORCE".equals(str)) {
            return true;
        }
        return "com.huawei.android.push.intent.TRS_QUERY_SUCCESS".equals(str);
    }

    private void rl(Context context, String str, boolean z) {
        if (context == null || str == null) {
            a.su("PushLog3414", "context or action is null");
            return;
        }
        if ("com.huawei.action.CONNECT_PUSHSRV_FORCE".equals(str)) {
            a.st("PushLog3414", "get " + str + " so get a pushSrv to connect");
            com.huawei.android.pushagent.model.channel.a.dz().bw(true, str);
        } else if (com.huawei.android.pushagent.model.channel.a.ea(context).ef().cp()) {
            com.huawei.android.pushagent.b.a.abd(36);
            a.st("PushLog3414", "pushChannel already connect");
        } else {
            a.st("PushLog3414", "get " + str + " so get a srv to connect");
            if (z) {
                com.huawei.android.pushagent.model.flowcontrol.b.nz(context).ob(context, ReconnectMgr$RECONNECTEVENT.NETWORK_CHANGE, new Bundle());
            }
            com.huawei.android.pushagent.model.channel.a.ea(context).ef().bw(false, str);
        }
    }
}
