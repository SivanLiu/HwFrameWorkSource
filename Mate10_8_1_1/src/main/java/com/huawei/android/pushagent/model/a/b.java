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
import com.huawei.android.pushagent.model.c.e;
import com.huawei.android.pushagent.model.channel.a;
import com.huawei.android.pushagent.model.prefs.h;
import com.huawei.android.pushagent.model.prefs.j;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.model.token.TokenApply;
import com.huawei.android.pushagent.utils.c.c;
import com.huawei.android.pushagent.utils.f;
import com.huawei.android.pushagent.utils.tools.d;
import org.json.JSONObject;

public class b implements d {
    public b(Context context) {
        com.huawei.android.pushagent.model.c.b.sc().sg(context);
    }

    public void onReceive(Context context, Intent intent) {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "enter CommandReceiver:onReceive, intent is:" + intent);
        String action = intent.getAction();
        if ("com.huawei.android.push.intent.CONNECTED".equals(action)) {
            je(context, intent);
        } else if ("com.huawei.android.push.intent.MSG_RECEIVED".equals(action)) {
            jp(context, intent);
        } else if ("com.huawei.action.push.intent.REPORT_EVENTS".equals(action)) {
            jr(intent);
        } else if ("com.huawei.action.push.intent.UPDATE_CHANNEL_STATE".equals(action)) {
            ju(intent);
        } else if ("com.huawei.android.push.intent.REGISTER".equals(action) || "com.huawei.android.push.intent.REGISTER_SPECIAL".equals(action)) {
            jm(context, intent);
        } else if ("com.huawei.android.push.intent.ACTION_TERMINAL_PROTOCAL".equals(action)) {
            ji(context, intent);
        } else if ("com.huawei.android.push.intent.DEREGISTER".equals(action)) {
            jn(context, intent);
        } else if ("com.huawei.intent.action.SELF_SHOW_FLAG".equals(action)) {
            jj(context, intent);
        } else if ("com.huawei.android.push.intent.MSG_RESPONSE".equals(action)) {
            iz(context, intent);
        } else if ("com.huawei.android.push.intent.MSG_RSP_TIMEOUT".equals(action)) {
            jb(context);
        } else if ("com.huawei.android.push.intent.RESET_BASTET".equals(action)) {
            jd(context, intent);
        } else if ("com.huawei.android.push.intent.RESPONSE_FAIL".equals(action)) {
            jh(context, intent);
        } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
            jc(context, intent);
        } else if ("android.ctrlsocket.all.allowed".equals(action)) {
            jf(context, intent);
        } else if ("android.scroff.ctrlsocket.status".equals(action)) {
            jg(context, intent);
        } else if ("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE".equals(action)) {
            iy(context, null);
            d.s(context, new Intent("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE").setPackage(context.getPackageName()), 1200000);
        } else if ("com.huawei.systemmanager.changedata".equals(action)) {
            ja(context, intent);
        }
    }

    private void ja(Context context, Intent intent) {
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "receive network policy change event from system manager.");
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "intent is null");
            return;
        }
        String stringExtra = intent.getStringExtra("packagename");
        if ("com.huawei.android.pushagent".equals(stringExtra)) {
            int intExtra = intent.getIntExtra("switch", 1);
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "network policy change, pkg:" + stringExtra + ", flag:" + intExtra);
            k.pt(context).qi(intExtra);
            if (intExtra == 0) {
                Intent intent2 = new Intent("com.huawei.intent.action.PUSH_OFF");
                intent2.setPackage(context.getPackageName());
                intent2.putExtra("Remote_Package_Name", context.getPackageName());
                PushService.aax(intent2);
            } else {
                iy(context, stringExtra);
            }
        }
    }

    private void ju(Intent intent) {
        if (intent == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "updatePushChannel intent is null");
            return;
        }
        if (!a.wr().vc()) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "push is disconnected, update network state when reconnecting.");
        }
        int intExtra = intent.getIntExtra("networkState", 0);
        long longExtra = intent.getLongExtra("duration", 0);
        if (intExtra == 0 && a.wr().vj() == 0) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "current push limit state is allow, no need report.");
            return;
        }
        IPushMessage decoupledPushMessage = new DecoupledPushMessage((byte) 66);
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("cmdid", -6);
            jSONObject.put("lock", intExtra);
            jSONObject.put("time", longExtra);
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "create DecoupledPushMessage for update push channel params error:" + e.toString(), e);
        }
        decoupledPushMessage.yy((byte) -6);
        decoupledPushMessage.zc(jSONObject);
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "update push channel state to push server.[limitState:" + intExtra + ",duration:" + longExtra + "]");
        try {
            a.wr().vl(decoupledPushMessage);
            a.wr().vm(intExtra);
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "update push channel state to push server exception");
        }
    }

    private void jr(Intent intent) {
        if (intent == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "sendEventsToServer intent is null");
            return;
        }
        CharSequence stringExtra = intent.getStringExtra("events");
        if (TextUtils.isEmpty(stringExtra)) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "sendEventsToServer events is null");
            return;
        }
        IPushMessage decoupledPushMessage = new DecoupledPushMessage((byte) 66);
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("cmdid", -10);
            jSONObject.put("v", 2976);
            jSONObject.put("e", stringExtra);
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "create DecoupledPushMessage params error:" + e.toString(), e);
        }
        decoupledPushMessage.yy((byte) -10);
        decoupledPushMessage.zc(jSONObject);
        try {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "reportEvent start to send report events");
            a.wr().vl(decoupledPushMessage);
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "send events to push server failed");
        }
    }

    private void je(Context context, Intent intent) {
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "intent is null");
            return;
        }
        String gk = f.gk(context);
        if (gk == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "cannot get imei when receviced ACTION_CONNECTED");
            return;
        }
        try {
            a.wr().vl(jk(context, gk));
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "call ChannelMgr.getPushChannel().send cause:" + e.toString(), e);
        }
    }

    private void jf(Context context, Intent intent) {
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "intent is null");
        } else {
            com.huawei.android.pushagent.model.c.b.sc().sk(context);
        }
    }

    private void jg(Context context, Intent intent) {
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "intent is null");
        } else {
            com.huawei.android.pushagent.model.c.b.sc().sj(context, intent);
        }
    }

    private void jh(Context context, Intent intent) {
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "intent is null");
            return;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "srv response fail, close channel and set alarm to reconnect!");
        try {
            long longExtra = intent.getLongExtra("expectTriggerTime", 0);
            byte byteExtra = intent.getByteExtra("cmdId", (byte) -1);
            byte byteExtra2 = intent.getByteExtra("subCmdId", (byte) -1);
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "wait the response of pushserver timeout, cmdId:" + com.huawei.android.pushagent.utils.a.a.v(byteExtra) + ", subCmdId:" + com.huawei.android.pushagent.utils.a.a.v(byteExtra2) + ", expectTriggerTime:" + longExtra + ", current trigger time:" + System.currentTimeMillis());
            if ((byte) 66 == byteExtra && (byte) -6 == byteExtra2) {
                com.huawei.android.pushagent.b.a.aak(104);
            } else {
                com.huawei.android.pushagent.b.a.aak(84);
            }
            a.ws(context).wv();
            PushService.aax(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(context.getPackageName()));
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "call channel.close cause exception:" + e.toString(), e);
        }
    }

    private void jd(Context context, Intent intent) {
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "intent is null");
            return;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "reset bastet alarm reach, and reconnect pushserver");
        com.huawei.android.pushagent.utils.bastet.a.cg(context).cr();
        PushService.aax(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(context.getPackageName()));
    }

    private void jp(Context context, Intent intent) {
        PushMessage pushMessage = (PushMessage) intent.getSerializableExtra("push_msg");
        if (pushMessage == null) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "msg is null");
            return;
        }
        switch (pushMessage.yq()) {
            case (byte) 65:
                com.huawei.android.pushagent.b.a.aak(74);
                DeviceRegisterRspMessage deviceRegisterRspMessage = (DeviceRegisterRspMessage) pushMessage;
                if (deviceRegisterRspMessage.getResult() != (byte) 0) {
                    com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "CommandReceiver device register fail:" + deviceRegisterRspMessage.getResult());
                    a.ws(context).wv();
                    break;
                }
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "CommandReceiver device register success");
                a.wx(context).vw(context);
                TokenApply.execute(context);
                com.huawei.android.pushagent.model.token.a.execute(context);
                break;
            case (byte) 67:
                jl(context, (DecoupledPushMessage) pushMessage);
                break;
            case (byte) 68:
                jq(context, (PushDataReqMessage) pushMessage);
                f.gf(context, 100);
                break;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void jl(Context context, DecoupledPushMessage decoupledPushMessage) {
        boolean z = true;
        if (decoupledPushMessage == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "decoupledPushMessage is null");
            return;
        }
        try {
            JSONObject za = decoupledPushMessage.za();
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "parse decoupled msg.");
            switch (decoupledPushMessage.yr()) {
                case (byte) -9:
                    int optInt = za.optInt("result", 1);
                    com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "report result is:" + optInt);
                    if (optInt != 0) {
                        z = false;
                    }
                    com.huawei.android.pushagent.b.a.aar(z);
                    com.huawei.android.pushagent.model.c.f.tk().tl();
                    break;
                case (byte) -7:
                    TokenApplyRsp tokenApplyRsp = (TokenApplyRsp) com.huawei.android.pushagent.utils.f.b.du(za.toString(), TokenApplyRsp.class, new Class[0]);
                    if (tokenApplyRsp != null) {
                        new TokenApply(context).responseToken(tokenApplyRsp);
                        break;
                    } else {
                        com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "parse decoupledPushMessage failed.");
                        break;
                    }
                case (byte) -5:
                    com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "update channel result: " + za.optInt("result", 1));
                    break;
                default:
                    com.huawei.android.pushagent.utils.a.b.ab("PushLog2976", "decoupledPushMessage subCmdid is not right");
                    break;
            }
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "parseDecoupledPushTokenMsg error:" + e.getMessage(), e);
        }
    }

    private void jq(Context context, PushDataReqMessage pushDataReqMessage) {
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "enter rspPushMessage");
        if (pushDataReqMessage.isValid()) {
            byte[] zh = pushDataReqMessage.zh();
            String u = com.huawei.android.pushagent.utils.a.a.u(zh);
            PushDataRspMessage pushDataRspMessage = new PushDataRspMessage(zh, (byte) 0, pushDataReqMessage.zd(), pushDataReqMessage.ze(), pushDataReqMessage.zj());
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "Device type is :" + com.huawei.android.pushagent.model.prefs.b.kp(context).km() + " [1:NOT_GDPR, 2:GDPR]");
            if (com.huawei.android.pushagent.model.c.d.td().te(u)) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "msgId has cached, do not sent again");
            } else {
                com.huawei.android.pushagent.model.c.d.td().tc(u);
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "msgType: " + pushDataReqMessage.zi() + " [0:PassBy msg, 1:System notification, 2:normal notification]");
                com.huawei.android.pushagent.model.b.d rd = com.huawei.android.pushagent.model.b.a.rd(context, pushDataReqMessage.zi(), pushDataReqMessage);
                if (rd == null) {
                    com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "invalid msgType: " + pushDataReqMessage.zi());
                    pushDataRspMessage.zq((byte) 20);
                } else {
                    pushDataRspMessage.zq(rd.rn());
                }
            }
            jo(pushDataRspMessage);
            return;
        }
        com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "reqMsg is inValid");
    }

    private void jm(Context context, Intent intent) {
        d.r(context, new Intent("com.huawei.intent.action.PUSH_OFF").setPackage(context.getPackageName()).putExtra("Remote_Package_Name", context.getPackageName()));
        String stringExtra = intent.getStringExtra("pkg_name");
        String stringExtra2 = intent.getStringExtra("userid");
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "CommandReceiver: get the packageName: " + stringExtra + "; userid is " + stringExtra2);
        if (TextUtils.isEmpty(stringExtra)) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "CommandReceiver: get the wrong package name from the Client!");
        } else {
            int fd = com.huawei.android.pushagent.utils.a.fd(stringExtra2);
            String fe = com.huawei.android.pushagent.utils.a.fe(fd);
            boolean lt = com.huawei.android.pushagent.model.prefs.f.lu(context).lt(stringExtra);
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "responseClientRegistration disagreeFlag:" + lt);
            if (lt) {
                com.huawei.android.pushagent.model.prefs.f.lu(context).lw(stringExtra, false);
            }
            if (f.gq(context, stringExtra, fd)) {
                stringExtra2 = f.fz(stringExtra, fe);
                h.md(context).me(stringExtra2, true);
                Object qp = l.qo(context).qp(stringExtra2);
                if (!TextUtils.isEmpty(qp)) {
                    l.qo(context).qq(c.bb(qp));
                }
                iy(context, stringExtra);
                if (e.th(context, stringExtra, fe)) {
                    com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "CommandReceiver: this package:" + stringExtra + " have already registered ");
                    com.huawei.android.pushagent.model.c.f.tk().tm(stringExtra);
                    f.gg(context, stringExtra, fe, com.huawei.android.pushagent.model.prefs.a.kc(context).ke(stringExtra2));
                } else {
                    js(context, stringExtra, fe);
                }
            } else {
                com.huawei.android.pushagent.b.a.aaj(60, stringExtra);
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "rec register toke request , but the packageName:" + stringExtra + " was not install !!");
            }
        }
    }

    private void iy(Context context, String str) {
        if (a.wr().vc()) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "check push connection is exist, sendHearBeat.");
            a.wx(context).uq();
            return;
        }
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "check push connection is not exist, reconnect it.");
        PushService.aax(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(context.getPackageName()).putExtra("PkgName", str));
    }

    private void js(Context context, String str, String str2) {
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "begin to get token from pushSrv, pkgName is: " + str + ", userId is " + str2);
        if (f.gr(context, str)) {
            String fz = f.fz(str, str2);
            jt(context, fz);
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "begin to get token from server, userid is: " + str2);
            j.pn(context).pq(fz);
            TokenApply.execute(context);
        }
    }

    private void jt(Context context, String str) {
        if (!a.wr().vc() && TextUtils.isEmpty(j.pn(context).po(str))) {
            a.wr().vn(true);
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "It is a new gettoken event, set force connect skip control.");
        }
    }

    private DeviceRegisterReqMessage jk(Context context, String str) {
        int parseInt = Integer.parseInt(f.gs(context));
        int km = com.huawei.android.pushagent.model.prefs.b.kp(context).km();
        int se = com.huawei.android.pushagent.model.c.b.se(context);
        long sd = com.huawei.android.pushagent.model.c.b.sd();
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "Device type is :" + km + " [1:NOT_GDPR, 2:GDPR]");
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "push register network state:" + se + "[0:Allow, 1:Limit], duration:" + sd);
        DeviceRegisterReqMessage deviceRegisterReqMessage = new DeviceRegisterReqMessage(str, (byte) km, (byte) f.fp(context), parseInt);
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("lock", se);
            jSONObject.put("time", sd);
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "create DecoupledPushMessage for update push channel params error:" + e.toString(), e);
        }
        a.wr().vm(se);
        deviceRegisterReqMessage.zl(jSONObject);
        return deviceRegisterReqMessage;
    }

    private void jn(Context context, Intent intent) {
        String stringExtra = intent.getStringExtra("pkg_name");
        if (TextUtils.isEmpty(stringExtra)) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "packagename is null, cannot deregister");
            return;
        }
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "responseClientUnRegistration: packagename = " + stringExtra);
        String stringExtra2 = intent.getStringExtra("device_token");
        if (TextUtils.isEmpty(stringExtra2)) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "origin token is null, cannot deregister");
            return;
        }
        if (intent.getBooleanExtra("isTokenEncrypt", false)) {
            stringExtra2 = c.bb(stringExtra2);
        }
        if (TextUtils.isEmpty(stringExtra2)) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "token is null, cannot deregister");
            return;
        }
        String kd = com.huawei.android.pushagent.model.prefs.a.kc(context).kd(stringExtra2);
        if (TextUtils.isEmpty(kd) || (stringExtra.equals(f.gh(kd)) ^ 1) != 0) {
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "token is not exist or not match, don't need to unreg");
            return;
        }
        if (!"unInstall".equals(intent.getStringExtra("from"))) {
            h.md(context).mf(kd);
        }
        j.pn(context).remove(kd);
        com.huawei.android.pushagent.model.prefs.a.kc(context).ki(kd);
        if (com.huawei.android.pushagent.utils.tools.a.j()) {
            com.huawei.android.pushagent.utils.tools.a.h(stringExtra);
        }
        l.qo(context).qr(stringExtra2, kd);
        com.huawei.android.pushagent.model.token.a.execute(context);
    }

    private void jc(Context context, Intent intent) {
        String str = "";
        Uri data = intent.getData();
        if (data != null) {
            str = data.getSchemeSpecificPart();
        }
        boolean booleanExtra = intent.getBooleanExtra("android.intent.extra.DATA_REMOVED", true);
        com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "ACTION_PACKAGE_REMOVED : isRemoveData=" + booleanExtra + " remove pkgName:" + str);
        if (booleanExtra) {
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "responseRemovePackage pkgName= " + str);
            String fz = f.fz(str, String.valueOf(com.huawei.android.pushagent.utils.a.fc()));
            com.huawei.android.pushagent.model.prefs.c.kw(context).la(fz);
            fz = com.huawei.android.pushagent.model.prefs.a.kc(context).ke(fz);
            Intent intent2 = new Intent();
            intent2.putExtra("pkg_name", str);
            intent2.putExtra("device_token", fz);
            intent2.putExtra("from", "unInstall");
            jn(context, intent2);
        }
    }

    private void iz(Context context, Intent intent) {
        if (context != null && intent != null) {
            Object stringExtra = intent.getStringExtra("msgIdStr");
            if (!TextUtils.isEmpty(stringExtra)) {
                String bb = c.bb(stringExtra);
                if (!TextUtils.isEmpty(bb)) {
                    com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "enter collectAndReportHiAnalytics, msgId is " + bb);
                    com.huawei.android.pushagent.model.c.a.ry().rz(context, bb);
                }
            }
        }
    }

    private void jb(Context context) {
        com.huawei.android.pushagent.model.c.a.ry().sa(context);
    }

    private void jj(Context context, Intent intent) {
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "enableReceiveNotifyMsg, context or intent is null");
            return;
        }
        try {
            Object stringExtra = intent.getStringExtra("enalbeFlag");
            if (TextUtils.isEmpty(stringExtra)) {
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "pkgAndFlagEncrypt is null");
                return;
            }
            stringExtra = c.bb(stringExtra);
            if (TextUtils.isEmpty(stringExtra)) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "pkgAndFlag is empty");
                return;
            }
            String[] split = stringExtra.split("#");
            if (2 != split.length) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "pkgAndFlag is invalid");
                return;
            }
            String str = split[0];
            boolean booleanValue = Boolean.valueOf(split[1]).booleanValue();
            com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "pkgName:" + str + ",flag:" + booleanValue);
            com.huawei.android.pushagent.model.prefs.c.kw(context).lb(f.fz(str, String.valueOf(com.huawei.android.pushagent.utils.a.fc())), booleanValue ^ 1);
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", e.toString(), e);
        }
    }

    private void ji(Context context, Intent intent) {
        com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "enter dealwithTerminateAgreement");
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "dealwithTerminateAgreement, context or intent is null");
            return;
        }
        try {
            Object stringExtra = intent.getStringExtra("pkg_name");
            boolean booleanExtra = intent.getBooleanExtra("has_disagree_protocal", false);
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "pkg:" + stringExtra + ",flag:" + booleanExtra);
            if (TextUtils.isEmpty(stringExtra)) {
                com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "dealwithTerminateAgreement, pkgName is empty");
            } else {
                com.huawei.android.pushagent.model.prefs.f.lu(context).lw(stringExtra, booleanExtra);
            }
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", e.toString(), e);
        }
    }

    private void jo(PushDataRspMessage pushDataRspMessage) {
        if (pushDataRspMessage == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "rspMsg or msgId is null");
            return;
        }
        try {
            a.wr().vl(pushDataRspMessage);
            com.huawei.android.pushagent.utils.a.b.z("PushLog2976", "rspPushMessage the response msg is :" + pushDataRspMessage.yx() + ",msgId:" + com.huawei.android.pushagent.utils.a.a.u(pushDataRspMessage.zr()) + ",flag:" + com.huawei.android.pushagent.utils.a.a.v(pushDataRspMessage.zs()));
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.a.b.aa("PushLog2976", "call ChannelMgr.getPushChannel().send cause:" + e.toString(), e);
        }
    }
}
