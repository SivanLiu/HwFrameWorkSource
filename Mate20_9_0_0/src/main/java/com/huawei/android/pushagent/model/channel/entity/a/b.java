package com.huawei.android.pushagent.model.channel.entity.a;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.a.c;
import com.huawei.android.pushagent.datatype.exception.PushException;
import com.huawei.android.pushagent.datatype.tcp.base.IPushMessage;
import com.huawei.android.pushagent.model.channel.entity.SocketReadThread$SocketEvent;
import com.huawei.android.pushagent.model.flowcontrol.ReconnectMgr$RECONNECTEVENT;
import com.huawei.android.pushagent.model.prefs.e;
import com.huawei.android.pushagent.model.prefs.f;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.d;
import java.net.InetSocketAddress;

public class b extends com.huawei.android.pushagent.model.channel.entity.b {
    private static final /* synthetic */ int[] aj = null;
    private boolean ai = false;

    private static /* synthetic */ int[] cf() {
        if (aj != null) {
            return aj;
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
        aj = iArr;
        return iArr;
    }

    public b(c cVar, Context context) {
        super(cVar, context, new a(context));
        bz();
    }

    public final boolean bz() {
        if (this.ao == null) {
            this.ao = new c("", -1, false);
        }
        return true;
    }

    public boolean by() {
        return this.ai;
    }

    public synchronized void bw(boolean z, String str) {
        try {
            if ("com.huawei.action.CONNECT_PUSHSRV_NON_WAKEUP".equals(str)) {
                a.sv("PushLog3414", "non-wakeup connect push trigger");
                f.ke(this.ap).kh(false);
            }
            if (com.huawei.android.pushagent.utils.bastet.a.xd(this.ap).xb()) {
                a.sv("PushLog3414", "enter connect, bastetProxy is started");
                if (cp()) {
                    com.huawei.android.pushagent.b.a.abd(37);
                    a.sv("PushLog3414", "enter connect, has Connection, do not reconnect");
                    return;
                } else if (!d.yp(this.ap)) {
                    com.huawei.android.pushagent.b.a.abd(38);
                    a.su("PushLog3414", "no network, so cannot connect");
                    return;
                } else if (this.aq) {
                    a.sv("PushLog3414", "isSkipControl, resetBastet");
                    com.huawei.android.pushagent.utils.bastet.a.xd(this.ap).xe();
                } else {
                    boolean cq = cq();
                    a.sv("PushLog3414", "enter connect, hasResetBastetAlarm " + cq);
                    if (cq) {
                        com.huawei.android.pushagent.b.a.abd(39);
                        return;
                    }
                    com.huawei.android.pushagent.b.a.abd(40);
                    cr(true);
                    com.huawei.android.pushagent.utils.tools.a.sa(this.ap, new Intent("com.huawei.android.push.intent.RESET_BASTET").setPackage(this.ap.getPackageName()), com.huawei.android.pushagent.model.prefs.a.ff(this.ap).ft());
                    a.sv("PushLog3414", "bastetProxyStarted, setDelayAlarm");
                    return;
                }
            }
            a.sv("PushLog3414", "enter PushConnectMode:connect(isForceConnPush:" + z + ")");
            if (!com.huawei.android.pushagent.model.prefs.a.ff(this.ap).isValid()) {
                a.sv("PushLog3414", "puserverip is not valid");
            } else if (!d.yp(this.ap)) {
                com.huawei.android.pushagent.b.a.abd(41);
                a.su("PushLog3414", "no network, so cannot connect");
            } else if (cp()) {
                com.huawei.android.pushagent.b.a.abd(42);
                a.st("PushLog3414", "aready connect, need not connect more");
            } else if (cc(str)) {
                bx();
            }
        } catch (Exception e) {
            throw new PushException(e);
        }
    }

    private boolean cc(String str) {
        if ("com.huawei.action.CONNECT_PUSHSRV_NON_WAKEUP".equals(str)) {
            return true;
        }
        long j;
        boolean z;
        if (f.ke(this.ap).ki() >= 2) {
            j = 300000;
            z = true;
        } else {
            j = com.huawei.android.pushagent.model.flowcontrol.b.nz(this.ap).oa(this.ap);
            z = false;
        }
        if (j <= 0) {
            a.sv("PushLog3414", "no limit to connect pushsvr");
            return true;
        } else if (this.aq) {
            com.huawei.android.pushagent.b.a.abd(43);
            a.sv("PushLog3414", "no limit to connect pushsvr, skipControl");
            cs(false);
            return true;
        } else {
            com.huawei.android.pushagent.b.a.abd(46);
            if (!z) {
                com.huawei.android.pushagent.model.channel.a.ea(this.ap).ec(j);
            } else if (f.ke(this.ap).kj()) {
                a.sv("PushLog3414", "nonWakeAlarm has exist, no need set again.");
            } else {
                a.sv("PushLog3414", "The first heartbeat failure more than two times under constant, delay connect push with non-wakeup.");
                com.huawei.android.pushagent.b.a.abd(76);
                com.huawei.android.pushagent.model.channel.a.ea(this.ap).eb(j);
                f.ke(this.ap).kh(true);
            }
            return false;
        }
    }

    /* JADX WARNING: Missing block: B:17:0x009d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void bx() {
        if (this.ar == null || (this.ar.isAlive() ^ 1) != 0) {
            a.st("PushLog3414", "begin to create new socket, so close socket");
            ct();
            cu();
            if (com.huawei.android.pushagent.model.flowcontrol.a.ni(this.ap, 1)) {
                this.ai = false;
                int jk = e.jj(this.ap).jk();
                InetSocketAddress px = com.huawei.android.pushagent.model.c.e.pw(this.ap).px();
                if (px != null) {
                    a.st("PushLog3414", "get pushSrvAddr:" + px);
                    this.ao.as(px.getAddress().getHostAddress());
                    this.ao.at(px.getPort());
                    this.ao = cv(jk, e.jj(this.ap).jl());
                    this.ar = new c(this);
                    this.ar.start();
                } else {
                    com.huawei.android.pushagent.b.a.abd(49);
                    a.st("PushLog3414", "no valid pushSrvAddr, just wait!!");
                    return;
                }
            }
            com.huawei.android.pushagent.b.a.abd(48);
            a.sx("PushLog3414", "can't connect push server because of flow control.");
            return;
        }
        a.sv("PushLog3414", "It is in connecting...");
        com.huawei.android.pushagent.b.a.abd(47);
    }

    public void cb(SocketReadThread$SocketEvent socketReadThread$SocketEvent, Bundle bundle) {
        int jl = e.jj(this.ap).jl();
        int jk = e.jj(this.ap).jk();
        a.st("PushLog3414", "enter PushConnectMode. notifyEvent is " + socketReadThread$SocketEvent + ", " + " tryConnectPushSevTimes:" + jl + " lastConnctIdx:" + jk);
        switch (cf()[socketReadThread$SocketEvent.ordinal()]) {
            case 1:
                cd(bundle, jl, jk);
                return;
            case 2:
                ce();
                return;
            case 3:
                PushService.abv(new Intent("com.huawei.android.push.intent.CONNECTING"));
                return;
            case 4:
                ca(bundle, jl, jk);
                return;
            default:
                return;
        }
    }

    private void ce() {
        com.huawei.android.pushagent.model.flowcontrol.b.nz(this.ap).ob(this.ap, ReconnectMgr$RECONNECTEVENT.SOCKET_CONNECTED, new Bundle());
        PushService.abv(new Intent("com.huawei.android.push.intent.CONNECTED"));
    }

    private void cd(Bundle bundle, int i, int i2) {
        com.huawei.android.pushagent.utils.tools.a.sc(this.ap, "com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
        com.huawei.android.pushagent.model.flowcontrol.b.nz(this.ap).ob(this.ap, ReconnectMgr$RECONNECTEVENT.SOCKET_CLOSE, bundle);
        if (!this.ai) {
            int i3 = i + 1;
            a.sv("PushLog3414", "channel is not Regist, tryConnectPushSevTimes add to " + i3);
            e.jj(this.ap).jm(i3);
            e.jj(this.ap).jn(i2);
        }
    }

    private void ca(Bundle bundle, int i, int i2) {
        com.huawei.android.pushagent.utils.tools.a.sc(this.ap, "com.huawei.android.push.intent.RESPONSE_FAIL");
        IPushMessage iPushMessage = (IPushMessage) bundle.getSerializable("push_msg");
        if (iPushMessage == null) {
            a.sv("PushLog3414", "push_msg is null");
            return;
        }
        a.sv("PushLog3414", "process cmdid to receive from pushSrv:" + com.huawei.android.pushagent.utils.b.c.ts(iPushMessage.c()) + ", subCmdId:" + com.huawei.android.pushagent.utils.b.c.ts(iPushMessage.g()));
        switch (iPushMessage.c()) {
            case (byte) -37:
                com.huawei.android.pushagent.utils.tools.a.sc(this.ap, "com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
                this.as.bf(false, false);
                this.as.bi();
                break;
            case (byte) 65:
                this.ai = true;
                e.jj(this.ap).jn(cw(i2, i));
                e.jj(this.ap).jm(0);
                com.huawei.android.pushagent.model.flowcontrol.b.nz(this.ap).ob(this.ap, ReconnectMgr$RECONNECTEVENT.SOCKET_REG_SUCCESS, new Bundle());
                break;
            default:
                this.as.bi();
                break;
        }
        Intent intent = new Intent("com.huawei.android.push.intent.MSG_RECEIVED");
        intent.putExtra("push_msg", iPushMessage);
        PushService.abv(intent);
    }
}
