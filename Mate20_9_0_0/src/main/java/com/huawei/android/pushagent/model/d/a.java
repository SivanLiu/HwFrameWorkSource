package com.huawei.android.pushagent.model.d;

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
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.model.c.b;
import com.huawei.android.pushagent.model.c.f;
import com.huawei.android.pushagent.model.prefs.e;
import com.huawei.android.pushagent.model.prefs.g;
import com.huawei.android.pushagent.model.prefs.j;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.model.token.TokenApply;
import com.huawei.android.pushagent.utils.b.c;
import com.huawei.android.pushagent.utils.d;
import org.json.JSONException;
import org.json.JSONObject;

public class a implements c {
    public a(Context context) {
        b.pa().pb(context);
    }

    public void onReceive(Context context, Intent intent) {
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "enter CommandReceiver:onReceive, intent is:" + intent);
        String action = intent.getAction();
        if ("com.huawei.android.push.intent.CONNECTED".equals(action)) {
            qu(context, intent);
        } else if ("com.huawei.android.push.intent.MSG_RECEIVED".equals(action)) {
            rf(context, intent);
        } else if ("com.huawei.action.push.intent.REPORT_EVENTS".equals(action)) {
            rh(intent);
        } else if ("com.huawei.action.push.intent.UPDATE_CHANNEL_STATE".equals(action)) {
            rk(intent);
        } else if ("com.huawei.android.push.intent.REGISTER".equals(action) || "com.huawei.android.push.intent.REGISTER_SPECIAL".equals(action)) {
            rc(context, intent);
        } else if ("com.huawei.android.push.intent.ACTION_TERMINAL_PROTOCAL".equals(action)) {
            qy(context, intent);
        } else if ("com.huawei.android.push.intent.DEREGISTER".equals(action)) {
            rd(context, intent);
        } else if ("com.huawei.intent.action.SELF_SHOW_FLAG".equals(action)) {
            qz(context, intent);
        } else if ("com.huawei.android.push.intent.MSG_RESPONSE".equals(action)) {
            qp(context, intent);
        } else if ("com.huawei.android.push.intent.MSG_RSP_TIMEOUT".equals(action)) {
            qr(context);
        } else if ("com.huawei.android.push.intent.RESET_BASTET".equals(action)) {
            qt(context, intent);
        } else if ("com.huawei.android.push.intent.RESPONSE_FAIL".equals(action)) {
            qx(context, intent);
        } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
            qs(context, intent);
        } else if ("android.ctrlsocket.all.allowed".equals(action)) {
            qv(context, intent);
        } else if ("android.scroff.ctrlsocket.status".equals(action)) {
            qw(context, intent);
        } else if ("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE".equals(action)) {
            qo(context, null);
            com.huawei.android.pushagent.utils.tools.a.se(context, new Intent("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE").setPackage(context.getPackageName()), 1200000);
        } else if ("com.huawei.systemmanager.changedata".equals(action)) {
            qq(context, intent);
        } else if ("android.intent.action.SCREEN_ON".equals(action)) {
            qo(context, "android");
        }
    }

    private void qq(Context context, Intent intent) {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "receive network policy change event from system manager.");
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "intent is null");
            return;
        }
        String stringExtra = intent.getStringExtra("packagename");
        if ("com.huawei.android.pushagent".equals(stringExtra)) {
            int intExtra = intent.getIntExtra("switch", 1);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "network policy change, pkg:" + stringExtra + ", flag:" + intExtra);
            e.jj(context).jo(intExtra);
            if (intExtra == 0) {
                Intent intent2 = new Intent("com.huawei.intent.action.PUSH_OFF");
                intent2.setPackage(context.getPackageName());
                intent2.putExtra("Remote_Package_Name", context.getPackageName());
                PushService.abv(intent2);
            } else {
                qo(context, stringExtra);
            }
        }
    }

    private void rk(Intent intent) {
        if (intent == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "updatePushChannel intent is null");
            return;
        }
        if (!com.huawei.android.pushagent.model.channel.a.dz().cp()) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "push is disconnected, update network state when reconnecting.");
        }
        int intExtra = intent.getIntExtra("networkState", 0);
        long longExtra = intent.getLongExtra("duration", 0);
        if (intExtra == 0 && com.huawei.android.pushagent.model.channel.a.dz().cx() == 0) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "current push limit state is allow, no need report.");
            return;
        }
        DecoupledPushMessage decoupledPushMessage = new DecoupledPushMessage((byte) 66);
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("cmdid", -6);
            jSONObject.put("lock", intExtra);
            jSONObject.put("time", longExtra);
        } catch (JSONException e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "create DecoupledPushMessage for update push channel params error:" + e.toString(), e);
        }
        decoupledPushMessage.k((byte) -6);
        decoupledPushMessage.r(jSONObject);
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "update push channel state to push server.[limitState:" + intExtra + ",duration:" + longExtra + "]");
        try {
            com.huawei.android.pushagent.model.channel.a.dz().co(decoupledPushMessage);
            com.huawei.android.pushagent.model.channel.a.dz().cy(intExtra);
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "update push channel state to push server exception");
        }
    }

    private void rh(Intent intent) {
        if (intent == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "sendEventsToServer intent is null");
            return;
        }
        String stringExtra = intent.getStringExtra("events");
        if (TextUtils.isEmpty(stringExtra)) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "sendEventsToServer events is null");
            return;
        }
        DecoupledPushMessage decoupledPushMessage = new DecoupledPushMessage((byte) 66);
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("cmdid", -10);
            jSONObject.put("v", 3414);
            jSONObject.put("e", stringExtra);
        } catch (JSONException e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "create DecoupledPushMessage params error:" + e.toString(), e);
        }
        decoupledPushMessage.k((byte) -10);
        decoupledPushMessage.r(jSONObject);
        try {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "reportEvent start to send report events");
            com.huawei.android.pushagent.model.channel.a.dz().co(decoupledPushMessage);
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "send events to push server failed");
        }
    }

    private void qu(Context context, Intent intent) {
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "intent is null");
            return;
        }
        String yq = d.yq(context);
        if (yq == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "cannot get imei when receviced ACTION_CONNECTED");
            return;
        }
        try {
            com.huawei.android.pushagent.model.channel.a.dz().co(ra(context, yq));
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "call ChannelMgr.getPushChannel().send cause:" + e.toString(), e);
        }
    }

    private void qv(Context context, Intent intent) {
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "intent is null");
        } else {
            b.pa().pc(context);
        }
    }

    private void qw(Context context, Intent intent) {
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "intent is null");
        } else {
            b.pa().pd(context, intent);
        }
    }

    private void qx(Context context, Intent intent) {
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "intent is null");
            return;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "srv response fail, close channel and set alarm to reconnect!");
        try {
            long longExtra = intent.getLongExtra("expectTriggerTime", 0);
            byte byteExtra = intent.getByteExtra("cmdId", (byte) -1);
            byte byteExtra2 = intent.getByteExtra("subCmdId", (byte) -1);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "wait the response of pushserver timeout, cmdId:" + c.ts(byteExtra) + ", subCmdId:" + c.ts(byteExtra2) + ", expectTriggerTime:" + longExtra + ", current trigger time:" + System.currentTimeMillis());
            if ((byte) 66 == byteExtra && (byte) -6 == byteExtra2) {
                com.huawei.android.pushagent.b.a.abd(104);
            } else {
                com.huawei.android.pushagent.b.a.abd(84);
            }
            com.huawei.android.pushagent.model.channel.a.ea(context).ed();
            PushService.abv(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(context.getPackageName()));
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "call channel.close cause exception:" + e.toString(), e);
        }
    }

    private void qt(Context context, Intent intent) {
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "intent is null");
            return;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "reset bastet alarm reach, and reconnect pushserver");
        com.huawei.android.pushagent.utils.bastet.a.xd(context).xe();
        PushService.abv(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(context.getPackageName()));
    }

    private void rf(Context context, Intent intent) {
        PushMessage pushMessage = (PushMessage) intent.getSerializableExtra("push_msg");
        if (pushMessage == null) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "msg is null");
            return;
        }
        switch (pushMessage.c()) {
            case (byte) -37:
                com.huawei.android.pushagent.utils.tools.a.se(context, new Intent("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE").setPackage(context.getPackageName()), 1200000);
                break;
            case (byte) 65:
                com.huawei.android.pushagent.b.a.abd(74);
                DeviceRegisterRspMessage deviceRegisterRspMessage = (DeviceRegisterRspMessage) pushMessage;
                if (deviceRegisterRspMessage.getResult() != (byte) 0) {
                    com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "CommandReceiver device register fail:" + deviceRegisterRspMessage.getResult());
                    com.huawei.android.pushagent.model.channel.a.ea(context).ed();
                    break;
                }
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "CommandReceiver device register success");
                com.huawei.android.pushagent.model.channel.a.ee(context).cn(context);
                TokenApply.execute(context);
                com.huawei.android.pushagent.model.token.a.execute(context);
                com.huawei.android.pushagent.utils.tools.a.se(context, new Intent("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE").setPackage(context.getPackageName()), 1200000);
                break;
            case (byte) 67:
                rb(context, (DecoupledPushMessage) pushMessage);
                com.huawei.android.pushagent.utils.tools.a.se(context, new Intent("com.huawei.action.push.intent.CHECK_CHANNEL_CYCLE").setPackage(context.getPackageName()), 1200000);
                break;
            case (byte) 68:
                rg(context, (PushDataReqMessage) pushMessage);
                d.yr(context, 100);
                break;
        }
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void rb(Context context, DecoupledPushMessage decoupledPushMessage) {
        boolean z = true;
        if (decoupledPushMessage == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "decoupledPushMessage is null");
            return;
        }
        try {
            JSONObject s = decoupledPushMessage.s();
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "parse decoupled msg.");
            switch (decoupledPushMessage.g()) {
                case (byte) -9:
                    int optInt = s.optInt("result", 1);
                    com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "report result is:" + optInt);
                    if (optInt != 0) {
                        z = false;
                    }
                    com.huawei.android.pushagent.b.a.abe(z);
                    com.huawei.android.pushagent.model.c.d.pt().pu();
                    break;
                case (byte) -7:
                    TokenApplyRsp tokenApplyRsp = (TokenApplyRsp) com.huawei.android.pushagent.utils.d.a.ui(s.toString(), TokenApplyRsp.class, new Class[0]);
                    if (tokenApplyRsp != null) {
                        new TokenApply(context).responseToken(tokenApplyRsp);
                        break;
                    } else {
                        com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "parse decoupledPushMessage failed.");
                        break;
                    }
                case (byte) -5:
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "update channel result: " + s.optInt("result", 1));
                    break;
                default:
                    com.huawei.android.pushagent.utils.b.a.sx("PushLog3414", "decoupledPushMessage subCmdid is not right");
                    break;
            }
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "parseDecoupledPushTokenMsg error:" + e.getMessage(), e);
        }
    }

    private void rg(Context context, PushDataReqMessage pushDataReqMessage) {
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "enter rspPushMessage");
        if (pushDataReqMessage.isValid()) {
            byte[] y = pushDataReqMessage.y();
            String tr = c.tr(y);
            PushDataRspMessage pushDataRspMessage = new PushDataRspMessage(y, (byte) 0, pushDataReqMessage.z(), pushDataReqMessage.aa(), pushDataReqMessage.w());
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "Device type is :" + g.kp(context).kq() + " [1:NOT_GDPR, 2:GDPR]");
            if (com.huawei.android.pushagent.model.c.a.oy().oz(tr)) {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "msgId has cached, do not sent again");
            } else {
                com.huawei.android.pushagent.model.c.a.oy().ox(tr);
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "msgType: " + pushDataReqMessage.ab() + " [0:PassBy msg, 1:System notification, 2:normal notification]");
                com.huawei.android.pushagent.model.a.e fb = com.huawei.android.pushagent.model.a.b.fb(context, pushDataReqMessage.ab(), pushDataReqMessage);
                if (fb == null) {
                    com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "invalid msgType: " + pushDataReqMessage.ab());
                    pushDataRspMessage.m((byte) 20);
                } else {
                    pushDataRspMessage.m(fb.fa());
                }
            }
            re(pushDataRspMessage);
            return;
        }
        com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "reqMsg is inValid");
    }

    private void rc(Context context, Intent intent) {
        com.huawei.android.pushagent.utils.tools.a.sc(context, "com.huawei.intent.action.PUSH_OFF");
        String stringExtra = intent.getStringExtra("pkg_name");
        String stringExtra2 = intent.getStringExtra("userid");
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "CommandReceiver: get the packageName: " + stringExtra + "; userid is " + stringExtra2);
        if (TextUtils.isEmpty(stringExtra)) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "CommandReceiver: get the wrong package name from the Client!");
        } else {
            int xz = com.huawei.android.pushagent.utils.a.xz(stringExtra2);
            String ya = com.huawei.android.pushagent.utils.a.ya(xz);
            boolean lt = k.ls(context).lt(stringExtra);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "responseClientRegistration disagreeFlag:" + lt);
            if (lt) {
                k.ls(context).lu(stringExtra, false);
            }
            if (d.yn(context, stringExtra, xz)) {
                stringExtra2 = d.ys(stringExtra, ya);
                com.huawei.android.pushagent.model.prefs.c.iu(context).iv(stringExtra2, true);
                String jb = com.huawei.android.pushagent.model.prefs.d.ja(context).jb(stringExtra2);
                if (!TextUtils.isEmpty(jb)) {
                    com.huawei.android.pushagent.model.prefs.d.ja(context).jc(com.huawei.android.pushagent.utils.e.a.vt(jb));
                }
                qo(context, stringExtra);
                if (f.qj(context, stringExtra, ya)) {
                    com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "CommandReceiver: this package:" + stringExtra + " have already registered ");
                    com.huawei.android.pushagent.model.c.d.pt().pv(stringExtra);
                    d.yt(context, stringExtra, ya, com.huawei.android.pushagent.model.prefs.b.il(context).in(stringExtra2));
                } else {
                    ri(context, stringExtra, ya);
                }
            } else {
                com.huawei.android.pushagent.b.a.abc(60, stringExtra);
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "rec register toke request , but the packageName:" + stringExtra + " was not install !!");
            }
        }
    }

    private void qo(Context context, String str) {
        if (com.huawei.android.pushagent.model.channel.a.dz().cp()) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "check push connection is exist, sendHearBeat.");
            com.huawei.android.pushagent.model.channel.a.ee(context).bt();
            return;
        }
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "check push connection is not exist, reconnect it.");
        PushService.abv(new Intent("com.huawei.action.CONNECT_PUSHSRV").setPackage(context.getPackageName()).putExtra("PkgName", str));
    }

    private void ri(Context context, String str, String str2) {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "begin to get token from pushSrv, pkgName is: " + str + ", userId is " + str2);
        if (d.yu(context, str)) {
            String ys = d.ys(str, str2);
            rj(context, ys);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "begin to get token from server, userid is: " + str2);
            j.lm(context).ln(ys);
            TokenApply.execute(context);
        }
    }

    private void rj(Context context, String str) {
        if (!com.huawei.android.pushagent.model.channel.a.dz().cp() && TextUtils.isEmpty(j.lm(context).lo(str))) {
            com.huawei.android.pushagent.model.channel.a.dz().cs(true);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "It is a new gettoken event, set force connect skip control.");
        }
    }

    private DeviceRegisterReqMessage ra(Context context, String str) {
        int parseInt = Integer.parseInt(d.yv(context));
        int kq = g.kp(context).kq();
        int pe = b.pe(context);
        long pf = b.pf();
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "Device type is :" + kq + " [1:NOT_GDPR, 2:GDPR]");
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "push register network state:" + pe + "[0:Allow, 1:Limit], duration:" + pf);
        DeviceRegisterReqMessage deviceRegisterReqMessage = new DeviceRegisterReqMessage(str, (byte) kq, (byte) d.yh(context), parseInt);
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("lock", pe);
            jSONObject.put("time", pf);
        } catch (JSONException e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "create DecoupledPushMessage for update push channel params error:" + e.toString(), e);
        }
        com.huawei.android.pushagent.model.channel.a.dz().cy(pe);
        deviceRegisterReqMessage.ad(jSONObject);
        return deviceRegisterReqMessage;
    }

    private void rd(Context context, Intent intent) {
        String stringExtra = intent.getStringExtra("pkg_name");
        if (TextUtils.isEmpty(stringExtra)) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "packagename is null, cannot deregister");
            return;
        }
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "responseClientUnRegistration: packagename = " + stringExtra);
        CharSequence stringExtra2 = intent.getStringExtra("device_token");
        if (TextUtils.isEmpty(stringExtra2)) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "origin token is null, cannot deregister");
            return;
        }
        if (intent.getBooleanExtra("isTokenEncrypt", false)) {
            stringExtra2 = com.huawei.android.pushagent.utils.e.a.vt(stringExtra2);
        }
        if (TextUtils.isEmpty(stringExtra2)) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "token is null, cannot deregister");
            return;
        }
        String im = com.huawei.android.pushagent.model.prefs.b.il(context).im(stringExtra2);
        if (TextUtils.isEmpty(im) || (stringExtra.equals(d.yw(im)) ^ 1) != 0) {
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "token is not exist or not match, don't need to unreg");
            return;
        }
        if (!"unInstall".equals(intent.getStringExtra("from"))) {
            com.huawei.android.pushagent.model.prefs.c.iu(context).iw(im);
        }
        j.lm(context).remove(im);
        com.huawei.android.pushagent.model.prefs.b.il(context).ir(im);
        if (com.huawei.android.pushagent.utils.tools.b.sf()) {
            com.huawei.android.pushagent.utils.tools.b.sh(stringExtra);
        }
        com.huawei.android.pushagent.model.prefs.d.ja(context).jd(stringExtra2, im);
        com.huawei.android.pushagent.model.token.a.execute(context);
    }

    private void qs(Context context, Intent intent) {
        String str = "";
        Uri data = intent.getData();
        if (data != null) {
            str = data.getSchemeSpecificPart();
        }
        boolean booleanExtra = intent.getBooleanExtra("android.intent.extra.DATA_REMOVED", true);
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "ACTION_PACKAGE_REMOVED : isRemoveData=" + booleanExtra + " remove pkgName:" + str);
        if (booleanExtra) {
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "responseRemovePackage pkgName= " + str);
            String ys = d.ys(str, String.valueOf(com.huawei.android.pushagent.utils.a.xy()));
            l.lw(context).lx(ys);
            ys = com.huawei.android.pushagent.model.prefs.b.il(context).in(ys);
            Intent intent2 = new Intent();
            intent2.putExtra("pkg_name", str);
            intent2.putExtra("device_token", ys);
            intent2.putExtra("from", "unInstall");
            rd(context, intent2);
        }
    }

    private void qp(Context context, Intent intent) {
        if (context != null && intent != null) {
            String stringExtra = intent.getStringExtra("msgIdStr");
            if (!TextUtils.isEmpty(stringExtra)) {
                stringExtra = com.huawei.android.pushagent.utils.e.a.vt(stringExtra);
                if (!TextUtils.isEmpty(stringExtra)) {
                    com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "enter collectAndReportHiAnalytics, msgId is " + stringExtra);
                    com.huawei.android.pushagent.model.c.c.po().pp(context, stringExtra);
                }
            }
        }
    }

    private void qr(Context context) {
        com.huawei.android.pushagent.model.c.c.po().pq(context);
    }

    private void qz(Context context, Intent intent) {
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "enableReceiveNotifyMsg, context or intent is null");
            return;
        }
        try {
            String stringExtra = intent.getStringExtra("enalbeFlag");
            if (TextUtils.isEmpty(stringExtra)) {
                com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "pkgAndFlagEncrypt is null");
                return;
            }
            stringExtra = com.huawei.android.pushagent.utils.e.a.vt(stringExtra);
            if (TextUtils.isEmpty(stringExtra)) {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "pkgAndFlag is empty");
                return;
            }
            String[] split = stringExtra.split("#");
            if (2 != split.length) {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "pkgAndFlag is invalid");
                return;
            }
            String str = split[0];
            boolean booleanValue = Boolean.valueOf(split[1]).booleanValue();
            com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "pkgName:" + str + ",flag:" + booleanValue);
            l.lw(context).ly(d.ys(str, String.valueOf(com.huawei.android.pushagent.utils.a.xy())), booleanValue ^ 1);
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e.toString(), e);
        }
    }

    private void qy(Context context, Intent intent) {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "enter dealwithTerminateAgreement");
        if (context == null || intent == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "dealwithTerminateAgreement, context or intent is null");
            return;
        }
        try {
            String stringExtra = intent.getStringExtra("pkg_name");
            boolean booleanExtra = intent.getBooleanExtra("has_disagree_protocal", false);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "pkg:" + stringExtra + ",flag:" + booleanExtra);
            if (TextUtils.isEmpty(stringExtra)) {
                com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "dealwithTerminateAgreement, pkgName is empty");
            } else {
                k.ls(context).lu(stringExtra, booleanExtra);
            }
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", e.toString(), e);
        }
    }

    private void re(PushDataRspMessage pushDataRspMessage) {
        if (pushDataRspMessage == null) {
            com.huawei.android.pushagent.utils.b.a.su("PushLog3414", "rspMsg or msgId is null");
            return;
        }
        try {
            com.huawei.android.pushagent.model.channel.a.dz().co(pushDataRspMessage);
            com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "rspPushMessage the response msg is :" + pushDataRspMessage.d() + ",msgId:" + c.tr(pushDataRspMessage.n()) + ",flag:" + c.ts(pushDataRspMessage.o()));
        } catch (Exception e) {
            com.huawei.android.pushagent.utils.b.a.sw("PushLog3414", "call ChannelMgr.getPushChannel().send cause:" + e.toString(), e);
        }
    }
}
