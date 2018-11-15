package com.huawei.android.pushagent.model.a;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.http.server.TokenApplyRsp;
import com.huawei.android.pushagent.datatype.tcp.DecoupledPushMessage;
import com.huawei.android.pushagent.datatype.tcp.DeviceRegisterReqMessage;
import com.huawei.android.pushagent.datatype.tcp.DeviceRegisterRspMessage;
import com.huawei.android.pushagent.datatype.tcp.PushDataReqMessage;
import com.huawei.android.pushagent.datatype.tcp.PushDataRspMessage;
import com.huawei.android.pushagent.datatype.tcp.base.IPushMessage;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.model.b.e;
import com.huawei.android.pushagent.model.channel.a;
import com.huawei.android.pushagent.model.prefs.h;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.model.prefs.j;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.model.prefs.m;
import com.huawei.android.pushagent.model.token.TokenApply;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.tools.d;
import org.json.JSONObject;

public class b implements a {
    public b(Context context) {
        com.huawei.android.pushagent.model.b.b.xd().xe(context);
    }

    public void onReceive(Context context, Intent intent) {
        c.er("PushLog3413", "enter CommandReceiver:onReceive, intent is:" + intent);
        String action = intent.getAction();
        if ("com.huawei.android.push.intent.CONNECTED".equals(action)) {
            wb(context, intent);
        } else if ("com.huawei.android.push.intent.MSG_RECEIVED".equals(action)) {
            wm(context, intent);
        } else if ("com.huawei.action.push.intent.REPORT_EVENTS".equals(action)) {
            wo(intent);
        } else if ("com.huawei.action.push.intent.UPDATE_CHANNEL_STATE".equals(action)) {
            wr(intent);
        } else if ("com.huawei.android.push.intent.REGISTER".equals(action) || "com.huawei.android.push.intent.REGISTER_SPECIAL".equals(action)) {
            wj(context, intent);
        } else if ("com.huawei.android.push.intent.ACTION_TERMINAL_PROTOCAL".equals(action)) {
            wf(context, intent);
        } else if ("com.huawei.android.push.intent.DEREGISTER".equals(action)) {
            wk(context, intent);
        } else if ("com.huawei.intent.action.SELF_SHOW_FLAG".equals(action)) {
            wg(context, intent);
        } else if ("com.huawei.android.push.intent.MSG_RESPONSE".equals(action)) {
            vw(context, intent);
        } else if ("com.huawei.android.push.intent.MSG_RSP_TIMEOUT".equals(action)) {
            vy(context);
        } else if ("com.huawei.android.push.intent.RESET_BASTET".equals(action)) {
            wa(context, intent);
        } else if ("com.huawei.android.push.intent.RESPONSE_FAIL".equals(action)) {
            we(context, intent);
        } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
            vz(context, intent);
        } else if ("android.ctrlsocket.all.allowed".equals(action)) {
            wc(context, intent);
        } else if ("android.scroff.ctrlsocket.status".equals(action)) {
            wd(context, intent);
        } else if ("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE".equals(action)) {
            vv(context, null);
            d.cx(context, new Intent("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE").setPackage(context.getPackageName()), 1200000);
        } else if ("com.huawei.systemmanager.changedata".equals(action)) {
            vx(context, intent);
        } else if ("android.intent.action.SCREEN_ON".equals(action)) {
            vv(context, "android");
        }
    }

    private void vx(Context context, Intent intent) {
        c.ep("PushLog3413", "receive network policy change event from system manager.");
        if (context == null || intent == null) {
            c.eq("PushLog3413", "intent is null");
            return;
        }
        String stringExtra = intent.getStringExtra("packagename");
        if ("com.huawei.android.pushagent".equals(stringExtra)) {
            int intExtra = intent.getIntExtra("switch", 1);
            c.ep("PushLog3413", "network policy change, pkg:" + stringExtra + ", flag:" + intExtra);
            l.ul(context).uo(intExtra);
            if (intExtra == 0) {
                Intent intent2 = new Intent("com.huawei.intent.action.PUSH_OFF");
                intent2.setPackage(context.getPackageName());
                intent2.putExtra("Remote_Package_Name", context.getPackageName());
                PushService.abr(intent2);
            } else {
                vv(context, stringExtra);
            }
        }
    }

    private void wr(Intent intent) {
        if (intent == null) {
            c.eq("PushLog3413", "updatePushChannel intent is null");
            return;
        }
        if (!a.ns().mk()) {
            c.ep("PushLog3413", "push is disconnected, update network state when reconnecting.");
        }
        int intExtra = intent.getIntExtra("networkState", 0);
        long longExtra = intent.getLongExtra("duration", 0);
        if (intExtra == 0 && a.ns().mh() == 0) {
            c.ep("PushLog3413", "current push limit state is allow, no need report.");
            return;
        }
        IPushMessage decoupledPushMessage = new DecoupledPushMessage((byte) 66);
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("cmdid", -6);
            jSONObject.put("lock", intExtra);
            jSONObject.put("time", longExtra);
        } catch (Throwable e) {
            c.es("PushLog3413", "create DecoupledPushMessage for update push channel params error:" + e.toString(), e);
        }
        decoupledPushMessage.ix((byte) -6);
        decoupledPushMessage.jm(jSONObject);
        c.ep("PushLog3413", "update push channel state to push server.[limitState:" + intExtra + ",duration:" + longExtra + "]");
        try {
            a.ns().mc(decoupledPushMessage);
            a.ns().mm(intExtra);
        } catch (Exception e2) {
            c.eq("PushLog3413", "update push channel state to push server exception");
        }
    }

    private void wo(Intent intent) {
        if (intent == null) {
            c.eq("PushLog3413", "sendEventsToServer intent is null");
            return;
        }
        CharSequence stringExtra = intent.getStringExtra("events");
        if (TextUtils.isEmpty(stringExtra)) {
            c.eq("PushLog3413", "sendEventsToServer events is null");
            return;
        }
        IPushMessage decoupledPushMessage = new DecoupledPushMessage((byte) 66);
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("cmdid", -10);
            jSONObject.put("v", 3413);
            jSONObject.put("e", stringExtra);
        } catch (Throwable e) {
            c.es("PushLog3413", "create DecoupledPushMessage params error:" + e.toString(), e);
        }
        decoupledPushMessage.ix((byte) -10);
        decoupledPushMessage.jm(jSONObject);
        try {
            c.er("PushLog3413", "reportEvent start to send report events");
            a.ns().mc(decoupledPushMessage);
        } catch (Exception e2) {
            c.eq("PushLog3413", "send events to push server failed");
        }
    }

    private void wb(Context context, Intent intent) {
        if (context == null || intent == null) {
            c.eq("PushLog3413", "intent is null");
            return;
        }
        String gi = g.gi(context);
        if (gi == null) {
            c.eq("PushLog3413", "cannot get imei when receviced ACTION_CONNECTED");
            return;
        }
        try {
            a.ns().mc(wh(context, gi));
        } catch (Throwable e) {
            c.es("PushLog3413", "call ChannelMgr.getPushChannel().send cause:" + e.toString(), e);
        }
    }

    private void wc(Context context, Intent intent) {
        if (context == null || intent == null) {
            c.eq("PushLog3413", "intent is null");
        } else {
            com.huawei.android.pushagent.model.b.b.xd().xf(context);
        }
    }

    private void wd(Context context, Intent intent) {
        if (context == null || intent == null) {
            c.eq("PushLog3413", "intent is null");
        } else {
            com.huawei.android.pushagent.model.b.b.xd().xg(context, intent);
        }
    }

    private void we(Context context, Intent intent) {
        if (context == null || intent == null) {
            c.eq("PushLog3413", "intent is null");
            return;
        }
        c.ep("PushLog3413", "srv response fail, close channel and set alarm to reconnect!");
        try {
            long longExtra = intent.getLongExtra("expectTriggerTime", 0);
            byte byteExtra = intent.getByteExtra("cmdId", (byte) -1);
            byte byteExtra2 = intent.getByteExtra("subCmdId", (byte) -1);
            c.ep("PushLog3413", "wait the response of pushserver timeout, cmdId:" + com.huawei.android.pushagent.utils.f.b.em(byteExtra) + ", subCmdId:" + com.huawei.android.pushagent.utils.f.b.em(byteExtra2) + ", expectTriggerTime:" + longExtra + ", current trigger time:" + System.currentTimeMillis());
            if ((byte) 66 == byteExtra && (byte) -6 == byteExtra2) {
                com.huawei.android.pushagent.a.a.hx(104);
            } else {
                com.huawei.android.pushagent.a.a.hx(84);
            }
            a.nt(context).nu();
            PushService.abr(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(context.getPackageName()));
        } catch (Throwable e) {
            c.es("PushLog3413", "call channel.close cause exception:" + e.toString(), e);
        }
    }

    private void wa(Context context, Intent intent) {
        if (context == null || intent == null) {
            c.eq("PushLog3413", "intent is null");
            return;
        }
        c.ep("PushLog3413", "reset bastet alarm reach, and reconnect pushserver");
        com.huawei.android.pushagent.utils.bastet.a.dk(context).dl();
        PushService.abr(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(context.getPackageName()));
    }

    private void wm(Context context, Intent intent) {
        PushMessage pushMessage = (PushMessage) intent.getSerializableExtra("push_msg");
        if (pushMessage == null) {
            c.ep("PushLog3413", "msg is null");
            return;
        }
        switch (pushMessage.it()) {
            case (byte) -37:
                d.cx(context, new Intent("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE").setPackage(context.getPackageName()), 1200000);
                break;
            case (byte) 65:
                com.huawei.android.pushagent.a.a.hx(74);
                DeviceRegisterRspMessage deviceRegisterRspMessage = (DeviceRegisterRspMessage) pushMessage;
                if (deviceRegisterRspMessage.getResult() != (byte) 0) {
                    c.eq("PushLog3413", "CommandReceiver device register fail:" + deviceRegisterRspMessage.getResult());
                    a.nt(context).nu();
                    break;
                }
                c.ep("PushLog3413", "CommandReceiver device register success");
                a.nv(context).mz(context);
                TokenApply.execute(context);
                com.huawei.android.pushagent.model.token.a.execute(context);
                d.cx(context, new Intent("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE").setPackage(context.getPackageName()), 1200000);
                break;
            case (byte) 67:
                wi(context, (DecoupledPushMessage) pushMessage);
                d.cx(context, new Intent("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE").setPackage(context.getPackageName()), 1200000);
                break;
            case (byte) 68:
                wn(context, (PushDataReqMessage) pushMessage);
                g.fr(context, 100);
                break;
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void wi(Context context, DecoupledPushMessage decoupledPushMessage) {
        boolean z = true;
        if (decoupledPushMessage == null) {
            c.eq("PushLog3413", "decoupledPushMessage is null");
            return;
        }
        try {
            JSONObject jn = decoupledPushMessage.jn();
            c.ep("PushLog3413", "parse decoupled msg.");
            switch (decoupledPushMessage.iu()) {
                case (byte) -9:
                    int optInt = jn.optInt("result", 1);
                    c.er("PushLog3413", "report result is:" + optInt);
                    if (optInt != 0) {
                        z = false;
                    }
                    com.huawei.android.pushagent.a.a.hw(z);
                    com.huawei.android.pushagent.model.b.d.xw().xx();
                    break;
                case (byte) -7:
                    TokenApplyRsp tokenApplyRsp = (TokenApplyRsp) com.huawei.android.pushagent.utils.c.a.br(jn.toString(), TokenApplyRsp.class, new Class[0]);
                    if (tokenApplyRsp != null) {
                        new TokenApply(context).responseToken(tokenApplyRsp);
                        break;
                    } else {
                        c.eq("PushLog3413", "parse decoupledPushMessage failed.");
                        break;
                    }
                case (byte) -5:
                    c.ep("PushLog3413", "update channel result: " + jn.optInt("result", 1));
                    break;
                default:
                    c.eo("PushLog3413", "decoupledPushMessage subCmdid is not right");
                    break;
            }
        } catch (Throwable e) {
            c.es("PushLog3413", "parseDecoupledPushTokenMsg error:" + e.getMessage(), e);
        }
    }

    private void wn(Context context, PushDataReqMessage pushDataReqMessage) {
        c.er("PushLog3413", "enter rspPushMessage");
        if (pushDataReqMessage.isValid()) {
            byte[] ju = pushDataReqMessage.ju();
            String el = com.huawei.android.pushagent.utils.f.b.el(ju);
            PushDataRspMessage pushDataRspMessage = new PushDataRspMessage(ju, (byte) 0, pushDataReqMessage.jv(), pushDataReqMessage.jw(), pushDataReqMessage.js());
            c.ep("PushLog3413", "Device type is :" + com.huawei.android.pushagent.model.prefs.b.oq(context).or() + " [1:NOT_GDPR, 2:GDPR]");
            if (e.xz().ya(el)) {
                c.ep("PushLog3413", "msgId has cached, do not sent again");
            } else {
                e.xz().yb(el);
                c.ep("PushLog3413", "msgType: " + pushDataReqMessage.jx() + " [0:PassBy msg, 1:System notification, 2:normal notification]");
                com.huawei.android.pushagent.model.d.b zn = com.huawei.android.pushagent.model.d.d.zn(context, pushDataReqMessage.jx(), pushDataReqMessage);
                if (zn == null) {
                    c.eq("PushLog3413", "invalid msgType: " + pushDataReqMessage.jx());
                    pushDataRspMessage.ji((byte) 20);
                } else {
                    pushDataRspMessage.ji(zn.zl());
                }
            }
            wl(pushDataRspMessage);
            return;
        }
        c.eq("PushLog3413", "reqMsg is inValid");
    }

    private void wj(Context context, Intent intent) {
        d.cy(context, "com.huawei.intent.action.PUSH_OFF");
        String stringExtra = intent.getStringExtra("pkg_name");
        String stringExtra2 = intent.getStringExtra("userid");
        c.ep("PushLog3413", "CommandReceiver: get the packageName: " + stringExtra + "; userid is " + stringExtra2);
        if (TextUtils.isEmpty(stringExtra)) {
            c.eq("PushLog3413", "CommandReceiver: get the wrong package name from the Client!");
        } else {
            int fd = com.huawei.android.pushagent.utils.a.fd(stringExtra2);
            String fa = com.huawei.android.pushagent.utils.a.fa(fd);
            boolean qy = i.qx(context).qy(stringExtra);
            c.ep("PushLog3413", "responseClientRegistration disagreeFlag:" + qy);
            if (qy) {
                i.qx(context).qz(stringExtra, false);
            }
            if (g.gc(context, stringExtra, fd)) {
                stringExtra2 = g.fs(stringExtra, fa);
                j.rb(context).re(stringExtra2, true);
                Object vj = m.vg(context).vj(stringExtra2);
                if (!TextUtils.isEmpty(vj)) {
                    m.vg(context).vk(com.huawei.android.pushagent.utils.a.c.j(vj));
                }
                vv(context, stringExtra);
                if (com.huawei.android.pushagent.model.b.a.xa(context, stringExtra, fa)) {
                    c.er("PushLog3413", "CommandReceiver: this package:" + stringExtra + " have already registered ");
                    com.huawei.android.pushagent.model.b.d.xw().xy(stringExtra);
                    g.ft(context, stringExtra, fa, com.huawei.android.pushagent.model.prefs.e.pn(context).ps(stringExtra2));
                } else {
                    wp(context, stringExtra, fa);
                }
            } else {
                com.huawei.android.pushagent.a.a.hq(60, stringExtra);
                c.eq("PushLog3413", "rec register toke request , but the packageName:" + stringExtra + " was not install !!");
            }
        }
    }

    private void vv(Context context, String str) {
        if (a.ns().mk()) {
            c.ep("PushLog3413", "check push connection is exist, sendHearBeat.");
            a.nv(context).lm();
            return;
        }
        c.ep("PushLog3413", "check push connection is not exist, reconnect it.");
        PushService.abr(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(context.getPackageName()).putExtra("PkgName", str));
    }

    private void wp(Context context, String str, String str2) {
        c.ep("PushLog3413", "begin to get token from pushSrv, pkgName is: " + str + ", userId is " + str2);
        if (g.gj(context, str)) {
            String fs = g.fs(str, str2);
            wq(context, fs);
            c.ep("PushLog3413", "begin to get token from server, userid is: " + str2);
            h.qr(context).qv(fs);
            TokenApply.execute(context);
        }
    }

    private void wq(Context context, String str) {
        if (!a.ns().mk() && TextUtils.isEmpty(h.qr(context).qs(str))) {
            a.ns().mn(true);
            c.ep("PushLog3413", "It is a new gettoken event, set force connect skip control.");
        }
    }

    private DeviceRegisterReqMessage wh(Context context, String str) {
        int parseInt = Integer.parseInt(g.gk(context));
        int or = com.huawei.android.pushagent.model.prefs.b.oq(context).or();
        int xh = com.huawei.android.pushagent.model.b.b.xh(context);
        long xi = com.huawei.android.pushagent.model.b.b.xi();
        c.ep("PushLog3413", "Device type is :" + or + " [1:NOT_GDPR, 2:GDPR]");
        c.ep("PushLog3413", "push register network state:" + xh + "[0:Allow, 1:Limit], duration:" + xi);
        DeviceRegisterReqMessage deviceRegisterReqMessage = new DeviceRegisterReqMessage(str, (byte) or, (byte) g.fw(context), parseInt);
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("lock", xh);
            jSONObject.put("time", xi);
        } catch (Throwable e) {
            c.es("PushLog3413", "create DecoupledPushMessage for update push channel params error:" + e.toString(), e);
        }
        a.ns().mm(xh);
        deviceRegisterReqMessage.jp(jSONObject);
        return deviceRegisterReqMessage;
    }

    private void wk(Context context, Intent intent) {
        String stringExtra = intent.getStringExtra("pkg_name");
        if (TextUtils.isEmpty(stringExtra)) {
            c.er("PushLog3413", "packagename is null, cannot deregister");
            return;
        }
        c.er("PushLog3413", "responseClientUnRegistration: packagename = " + stringExtra);
        String stringExtra2 = intent.getStringExtra("device_token");
        if (TextUtils.isEmpty(stringExtra2)) {
            c.er("PushLog3413", "origin token is null, cannot deregister");
            return;
        }
        if (intent.getBooleanExtra("isTokenEncrypt", false)) {
            stringExtra2 = com.huawei.android.pushagent.utils.a.c.j(stringExtra2);
        }
        if (TextUtils.isEmpty(stringExtra2)) {
            c.er("PushLog3413", "token is null, cannot deregister");
            return;
        }
        String pu = com.huawei.android.pushagent.model.prefs.e.pn(context).pu(stringExtra2);
        if (TextUtils.isEmpty(pu) || (stringExtra.equals(g.fu(pu)) ^ 1) != 0) {
            c.ep("PushLog3413", "token is not exist or not match, don't need to unreg");
            return;
        }
        if (!"unInstall".equals(intent.getStringExtra("from"))) {
            j.rb(context).rf(pu);
        }
        h.qr(context).remove(pu);
        com.huawei.android.pushagent.model.prefs.e.pn(context).pr(pu);
        if (com.huawei.android.pushagent.utils.tools.c.cr()) {
            com.huawei.android.pushagent.utils.tools.c.cu(stringExtra);
        }
        m.vg(context).vl(stringExtra2, pu);
        com.huawei.android.pushagent.model.token.a.execute(context);
    }

    private void vz(Context context, Intent intent) {
        String str = "";
        Uri data = intent.getData();
        if (data != null) {
            str = data.getSchemeSpecificPart();
        }
        boolean booleanExtra = intent.getBooleanExtra("android.intent.extra.DATA_REMOVED", true);
        c.er("PushLog3413", "ACTION_PACKAGE_REMOVED : isRemoveData=" + booleanExtra + " remove pkgName:" + str);
        if (booleanExtra) {
            c.er("PushLog3413", "responseRemovePackage pkgName= " + str);
            String fs = g.fs(str, String.valueOf(com.huawei.android.pushagent.utils.a.fb()));
            com.huawei.android.pushagent.model.prefs.d.ph(context).pi(fs);
            fs = com.huawei.android.pushagent.model.prefs.e.pn(context).ps(fs);
            Intent intent2 = new Intent();
            intent2.putExtra("pkg_name", str);
            intent2.putExtra("device_token", fs);
            intent2.putExtra("from", "unInstall");
            wk(context, intent2);
        }
    }

    private void vw(Context context, Intent intent) {
        if (context != null && intent != null) {
            Object stringExtra = intent.getStringExtra("msgIdStr");
            if (!TextUtils.isEmpty(stringExtra)) {
                String j = com.huawei.android.pushagent.utils.a.c.j(stringExtra);
                if (!TextUtils.isEmpty(j)) {
                    c.ep("PushLog3413", "enter collectAndReportHiAnalytics, msgId is " + j);
                    com.huawei.android.pushagent.model.b.c.xr().xs(context, j);
                }
            }
        }
    }

    private void vy(Context context) {
        com.huawei.android.pushagent.model.b.c.xr().xt(context);
    }

    private void wg(Context context, Intent intent) {
        if (context == null || intent == null) {
            c.eq("PushLog3413", "enableReceiveNotifyMsg, context or intent is null");
            return;
        }
        try {
            Object stringExtra = intent.getStringExtra("enalbeFlag");
            if (TextUtils.isEmpty(stringExtra)) {
                c.eq("PushLog3413", "pkgAndFlagEncrypt is null");
                return;
            }
            stringExtra = com.huawei.android.pushagent.utils.a.c.j(stringExtra);
            if (TextUtils.isEmpty(stringExtra)) {
                c.ep("PushLog3413", "pkgAndFlag is empty");
                return;
            }
            String[] split = stringExtra.split("#");
            if (2 != split.length) {
                c.ep("PushLog3413", "pkgAndFlag is invalid");
                return;
            }
            String str = split[0];
            boolean booleanValue = Boolean.valueOf(split[1]).booleanValue();
            c.er("PushLog3413", "pkgName:" + str + ",flag:" + booleanValue);
            com.huawei.android.pushagent.model.prefs.d.ph(context).pj(g.fs(str, String.valueOf(com.huawei.android.pushagent.utils.a.fb())), booleanValue ^ 1);
        } catch (Throwable e) {
            c.es("PushLog3413", e.toString(), e);
        }
    }

    private void wf(Context context, Intent intent) {
        c.ep("PushLog3413", "enter dealwithTerminateAgreement");
        if (context == null || intent == null) {
            c.eq("PushLog3413", "dealwithTerminateAgreement, context or intent is null");
            return;
        }
        try {
            Object stringExtra = intent.getStringExtra("pkg_name");
            boolean booleanExtra = intent.getBooleanExtra("has_disagree_protocal", false);
            c.ep("PushLog3413", "pkg:" + stringExtra + ",flag:" + booleanExtra);
            if (TextUtils.isEmpty(stringExtra)) {
                c.ep("PushLog3413", "dealwithTerminateAgreement, pkgName is empty");
            } else {
                i.qx(context).qz(stringExtra, booleanExtra);
            }
        } catch (Throwable e) {
            c.es("PushLog3413", e.toString(), e);
        }
    }

    private void wl(PushDataRspMessage pushDataRspMessage) {
        if (pushDataRspMessage == null) {
            c.eq("PushLog3413", "rspMsg or msgId is null");
            return;
        }
        try {
            a.ns().mc(pushDataRspMessage);
            c.ep("PushLog3413", "rspPushMessage the response msg is :" + pushDataRspMessage.iw() + ",msgId:" + com.huawei.android.pushagent.utils.f.b.el(pushDataRspMessage.jh()) + ",flag:" + com.huawei.android.pushagent.utils.f.b.em(pushDataRspMessage.jf()));
        } catch (Throwable e) {
            c.es("PushLog3413", "call ChannelMgr.getPushChannel().send cause:" + e.toString(), e);
        }
    }
}
