package com.huawei.android.pushagent.model.channel.entity.a;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.exception.PushException;
import com.huawei.android.pushagent.datatype.tcp.base.IPushMessage;
import com.huawei.android.pushagent.model.b.f;
import com.huawei.android.pushagent.model.channel.entity.SocketReadThread$SocketEvent;
import com.huawei.android.pushagent.model.channel.entity.a;
import com.huawei.android.pushagent.model.flowcontrol.ReconnectMgr$RECONNECTEVENT;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.model.prefs.l;
import com.huawei.android.pushagent.utils.f.b;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.tools.d;
import java.net.InetSocketAddress;

public class c extends a {
    private static final /* synthetic */ int[] de = null;
    private boolean dd = false;

    private static /* synthetic */ int[] mb() {
        if (de != null) {
            return de;
        }
        int[] iArr = new int[SocketReadThread$SocketEvent.values().length];
        try {
            iArr[SocketReadThread$SocketEvent.SocketEvent_CLOSE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[SocketReadThread$SocketEvent.SocketEvent_CONNECTED.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[SocketReadThread$SocketEvent.SocketEvent_CONNECTING.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[SocketReadThread$SocketEvent.SocketEvent_MSG_RECEIVED.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        de = iArr;
        return iArr;
    }

    public c(com.huawei.android.pushagent.datatype.a.a aVar, Context context) {
        super(aVar, context, new a(context));
        lv();
    }

    public final boolean lv() {
        if (this.dh == null) {
            this.dh = new com.huawei.android.pushagent.datatype.a.a("", -1, false);
        }
        return true;
    }

    public boolean ls() {
        return this.dd;
    }

    public synchronized void lt(boolean z, String str) {
        try {
            if ("com.huawei.action.CONNECT_PUSHSRV_NON_WAKEUP".equals(str)) {
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "non-wakeup connect push trigger");
                com.huawei.android.pushagent.model.prefs.a.of(this.di).op(false);
            }
            if (com.huawei.android.pushagent.utils.bastet.a.dk(this.di).di()) {
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "enter connect, bastetProxy is started");
                if (mk()) {
                    com.huawei.android.pushagent.a.a.hx(37);
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "enter connect, has Connection, do not reconnect");
                    return;
                } else if (!g.fq(this.di)) {
                    com.huawei.android.pushagent.a.a.hx(38);
                    com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "no network, so cannot connect");
                    return;
                } else if (this.dl) {
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "isSkipControl, resetBastet");
                    com.huawei.android.pushagent.utils.bastet.a.dk(this.di).dl();
                } else {
                    boolean mg = mg();
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "enter connect, hasResetBastetAlarm " + mg);
                    if (mg) {
                        com.huawei.android.pushagent.a.a.hx(39);
                        return;
                    }
                    com.huawei.android.pushagent.a.a.hx(40);
                    ml(true);
                    d.cw(this.di, new Intent("com.huawei.android.push.intent.RESET_BASTET").setPackage(this.di.getPackageName()), k.rh(this.di).ud());
                    com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "bastetProxyStarted, setDelayAlarm");
                    return;
                }
            }
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "enter PushConnectMode:connect(isForceConnPush:" + z + ")");
            if (!k.rh(this.di).isValid()) {
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "puserverip is not valid");
            } else if (!g.fq(this.di)) {
                com.huawei.android.pushagent.a.a.hx(41);
                com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "no network, so cannot connect");
            } else if (mk()) {
                com.huawei.android.pushagent.a.a.hx(42);
                com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "aready connect, need not connect more");
            } else if (ly(str)) {
                lu();
            }
        } catch (Throwable e) {
            throw new PushException(e);
        }
    }

    private boolean ly(String str) {
        if ("com.huawei.action.CONNECT_PUSHSRV_NON_WAKEUP".equals(str)) {
            return true;
        }
        long j;
        boolean z;
        if (com.huawei.android.pushagent.model.prefs.a.of(this.di).oj() >= 2) {
            j = 300000;
            z = true;
        } else {
            j = com.huawei.android.pushagent.model.flowcontrol.a.zx(this.di).zz(this.di);
            z = false;
        }
        if (j <= 0) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "no limit to connect pushsvr");
            return true;
        } else if (this.dl) {
            com.huawei.android.pushagent.a.a.hx(43);
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "no limit to connect pushsvr, skipControl");
            mn(false);
            return true;
        } else {
            com.huawei.android.pushagent.a.a.hx(46);
            if (!z) {
                com.huawei.android.pushagent.model.channel.a.nt(this.di).nz(j);
            } else if (com.huawei.android.pushagent.model.prefs.a.of(this.di).ok()) {
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "nonWakeAlarm has exist, no need set again.");
            } else {
                com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "The first heartbeat failure more than two times under constant, delay connect push with non-wakeup.");
                com.huawei.android.pushagent.a.a.hx(76);
                com.huawei.android.pushagent.model.channel.a.nt(this.di).ny(j);
                com.huawei.android.pushagent.model.prefs.a.of(this.di).op(true);
            }
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:17:0x009d, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void lu() {
        if (this.dq == null || (this.dq.isAlive() ^ 1) != 0) {
            com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "begin to create new socket, so close socket");
            mj();
            md();
            if (com.huawei.android.pushagent.model.flowcontrol.c.aay(this.di, 1)) {
                this.dd = false;
                int uw = l.ul(this.di).uw();
                InetSocketAddress yi = f.yc(this.di).yi();
                if (yi != null) {
                    com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "get pushSrvAddr:" + yi);
                    this.dh.kd(yi.getAddress().getHostAddress());
                    this.dh.ke(yi.getPort());
                    this.dh = me(uw, l.ul(this.di).va());
                    this.dq = new b(this);
                    this.dq.start();
                } else {
                    com.huawei.android.pushagent.a.a.hx(49);
                    com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "no valid pushSrvAddr, just wait!!");
                    return;
                }
            }
            com.huawei.android.pushagent.a.a.hx(48);
            com.huawei.android.pushagent.utils.f.c.eo("PushLog3413", "can't connect push server because of flow control.");
            return;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "It is in connecting...");
        com.huawei.android.pushagent.a.a.hx(47);
    }

    public void lx(SocketReadThread$SocketEvent socketReadThread$SocketEvent, Bundle bundle) {
        int va = l.ul(this.di).va();
        int uw = l.ul(this.di).uw();
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "enter PushConnectMode. notifyEvent is " + socketReadThread$SocketEvent + ", " + " tryConnectPushSevTimes:" + va + " lastConnctIdx:" + uw);
        switch (mb()[socketReadThread$SocketEvent.ordinal()]) {
            case 1:
                lz(bundle, va, uw);
                return;
            case 2:
                ma();
                return;
            case 3:
                PushService.abr(new Intent("com.huawei.android.push.intent.CONNECTING"));
                return;
            case 4:
                lw(bundle, va, uw);
                return;
            default:
                return;
        }
    }

    private void ma() {
        com.huawei.android.pushagent.model.flowcontrol.a.zx(this.di).aag(this.di, ReconnectMgr$RECONNECTEVENT.SOCKET_CONNECTED, new Bundle());
        PushService.abr(new Intent("com.huawei.android.push.intent.CONNECTED"));
    }

    private void lz(Bundle bundle, int i, int i2) {
        d.cy(this.di, "com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
        com.huawei.android.pushagent.model.flowcontrol.a.zx(this.di).aag(this.di, ReconnectMgr$RECONNECTEVENT.SOCKET_CLOSE, bundle);
        if (!this.dd) {
            int i3 = i + 1;
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "channel is not Regist, tryConnectPushSevTimes add to " + i3);
            l.ul(this.di).ve(i3);
            l.ul(this.di).vc(i2);
        }
    }

    private void lw(Bundle bundle, int i, int i2) {
        d.cy(this.di, "com.huawei.android.push.intent.RESPONSE_FAIL");
        IPushMessage iPushMessage = (IPushMessage) bundle.getSerializable("push_msg");
        if (iPushMessage == null) {
            com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "push_msg is null");
            return;
        }
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "process cmdid to receive from pushSrv:" + b.em(iPushMessage.it()) + ", subCmdId:" + b.em(iPushMessage.iu()));
        switch (iPushMessage.it()) {
            case (byte) -37:
                d.cy(this.di, "com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
                this.dk.ky(false, false);
                this.dk.lb();
                break;
            case (byte) 65:
                this.dd = true;
                l.ul(this.di).vc(mf(i2, i));
                l.ul(this.di).ve(0);
                com.huawei.android.pushagent.model.flowcontrol.a.zx(this.di).aag(this.di, ReconnectMgr$RECONNECTEVENT.SOCKET_REG_SUCCESS, new Bundle());
                break;
            default:
                this.dk.lb();
                break;
        }
        Intent intent = new Intent("com.huawei.android.push.intent.MSG_RECEIVED");
        intent.putExtra("push_msg", iPushMessage);
        PushService.abr(intent);
    }
}
