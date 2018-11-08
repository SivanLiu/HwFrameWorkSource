package com.huawei.android.pushagent.model.channel.entity.a;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.huawei.android.pushagent.PushService;
import com.huawei.android.pushagent.datatype.b.d;
import com.huawei.android.pushagent.datatype.exception.PushException;
import com.huawei.android.pushagent.datatype.tcp.base.IPushMessage;
import com.huawei.android.pushagent.model.channel.entity.SocketReadThread$SocketEvent;
import com.huawei.android.pushagent.model.channel.entity.a;
import com.huawei.android.pushagent.model.flowcontrol.ReconnectMgr$RECONNECTEVENT;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.f;
import java.net.InetSocketAddress;

public class c extends a {
    private static final /* synthetic */ int[] fk = null;
    private boolean fj = false;

    private static /* synthetic */ int[] vb() {
        if (fk != null) {
            return fk;
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
        fk = iArr;
        return iArr;
    }

    public c(d dVar, Context context) {
        super(dVar, context, new b(context));
        uw();
    }

    public final boolean uw() {
        if (this.fn == null) {
            this.fn = new d("", -1, false);
        }
        return true;
    }

    public boolean ut() {
        return this.fj;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void uu(boolean z) {
        try {
            if (com.huawei.android.pushagent.utils.bastet.a.cg(this.fo).cn()) {
                b.z("PushLog2976", "enter connect, bastetProxy is started");
                if (vc()) {
                    com.huawei.android.pushagent.b.a.aak(37);
                    b.z("PushLog2976", "enter connect, has Connection, do not reconnect");
                    return;
                } else if (!f.ge(this.fo)) {
                    com.huawei.android.pushagent.b.a.aak(38);
                    b.y("PushLog2976", "no network, so cannot connect");
                    return;
                } else if (this.fr) {
                    b.z("PushLog2976", "isSkipControl, resetBastet");
                    com.huawei.android.pushagent.utils.bastet.a.cg(this.fo).cr();
                } else {
                    boolean vi = vi();
                    b.z("PushLog2976", "enter connect, hasResetBastetAlarm " + vi);
                    if (vi) {
                        com.huawei.android.pushagent.b.a.aak(39);
                        return;
                    }
                    com.huawei.android.pushagent.b.a.aak(40);
                    vf(true);
                    com.huawei.android.pushagent.utils.tools.d.p(this.fo, new Intent("com.huawei.android.push.intent.RESET_BASTET").setPackage(this.fo.getPackageName()), i.mj(this.fo).pj());
                    b.z("PushLog2976", "bastetProxyStarted, setDelayAlarm");
                    return;
                }
            }
            b.z("PushLog2976", "enter PushConnectMode:connect(isForceToConnPushSrv:" + z + ")");
            if (!i.mj(this.fo).isValid()) {
                b.z("PushLog2976", "puserverip is not valid");
            } else if (!f.ge(this.fo)) {
                com.huawei.android.pushagent.b.a.aak(41);
                b.y("PushLog2976", "no network, so cannot connect");
            } else if (vc()) {
                com.huawei.android.pushagent.b.a.aak(42);
                if (z) {
                    b.x("PushLog2976", "hasConnect, but isForceToConnPushSrv:" + z + ", so send heartBeat");
                    this.fq.uq();
                } else {
                    b.x("PushLog2976", "aready connect, need not connect more");
                }
            } else {
                long id = com.huawei.android.pushagent.model.flowcontrol.b.ib(this.fo).id(this.fo);
                if (id <= 0) {
                    b.z("PushLog2976", "no limit to connect pushsvr");
                } else if (this.fr) {
                    com.huawei.android.pushagent.b.a.aak(43);
                    b.z("PushLog2976", "no limit to connect pushsvr, skipControl");
                    vn(false);
                } else {
                    com.huawei.android.pushagent.b.a.aak(46);
                    com.huawei.android.pushagent.model.channel.a.ws(this.fo).ww(id);
                    return;
                }
                uv(z);
            }
        } catch (Throwable e) {
            throw new PushException(e);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void uv(boolean z) {
        if (this.fw == null || (this.fw.isAlive() ^ 1) != 0) {
            b.x("PushLog2976", "begin to create new socket, so close socket");
            vk();
            ve();
            if (com.huawei.android.pushagent.model.flowcontrol.a.hn(this.fo, 1)) {
                this.fj = false;
                int qj = k.pt(this.fo).qj();
                InetSocketAddress sv = com.huawei.android.pushagent.model.c.c.sp(this.fo).sv(z);
                if (sv != null) {
                    b.x("PushLog2976", "get pushSrvAddr:" + sv);
                    this.fn.ym(sv.getAddress().getHostAddress());
                    this.fn.yn(sv.getPort());
                    this.fn = vg(qj, k.pt(this.fo).ql());
                    this.fw = new a(this);
                    this.fw.start();
                } else {
                    com.huawei.android.pushagent.b.a.aak(49);
                    b.x("PushLog2976", "no valid pushSrvAddr, just wait!!");
                    return;
                }
            }
            com.huawei.android.pushagent.b.a.aak(48);
            b.ab("PushLog2976", "can't connect push server because of flow control.");
            return;
        }
        b.z("PushLog2976", "It is in connecting...");
        com.huawei.android.pushagent.b.a.aak(47);
    }

    public void uy(SocketReadThread$SocketEvent socketReadThread$SocketEvent, Bundle bundle) {
        int ql = k.pt(this.fo).ql();
        int qj = k.pt(this.fo).qj();
        b.x("PushLog2976", "enter PushConnectMode. notifyEvent is " + socketReadThread$SocketEvent + ", " + " tryConnectPushSevTimes:" + ql + " lastConnctIdx:" + qj);
        switch (vb()[socketReadThread$SocketEvent.ordinal()]) {
            case 1:
                uz(bundle, ql, qj);
                return;
            case 2:
                va();
                return;
            case 3:
                PushService.aax(new Intent("com.huawei.android.push.intent.CONNECTING"));
                return;
            case 4:
                ux(bundle, ql, qj);
                return;
            default:
                return;
        }
    }

    private void va() {
        com.huawei.android.pushagent.model.flowcontrol.b.ib(this.fo).ik(this.fo, ReconnectMgr$RECONNECTEVENT.SOCKET_CONNECTED, new Bundle());
        PushService.aax(new Intent("com.huawei.android.push.intent.CONNECTED"));
    }

    private void uz(Bundle bundle, int i, int i2) {
        com.huawei.android.pushagent.utils.tools.d.o(this.fo, "com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
        com.huawei.android.pushagent.model.flowcontrol.b.ib(this.fo).ik(this.fo, ReconnectMgr$RECONNECTEVENT.SOCKET_CLOSE, bundle);
        if (!this.fj) {
            int i3 = i + 1;
            b.z("PushLog2976", "channel is not Regist, tryConnectPushSevTimes add to " + i3);
            k.pt(this.fo).qn(i3);
            k.pt(this.fo).qm(i2);
        }
    }

    private void ux(Bundle bundle, int i, int i2) {
        com.huawei.android.pushagent.utils.tools.d.o(this.fo, "com.huawei.android.push.intent.RESPONSE_FAIL");
        IPushMessage iPushMessage = (IPushMessage) bundle.getSerializable("push_msg");
        if (iPushMessage == null) {
            b.z("PushLog2976", "push_msg is null");
            return;
        }
        b.z("PushLog2976", "process cmdid to receive from pushSrv:" + com.huawei.android.pushagent.utils.a.a.v(iPushMessage.yq()) + ", subCmdId:" + com.huawei.android.pushagent.utils.a.a.v(iPushMessage.yr()));
        switch (iPushMessage.yq()) {
            case (byte) -37:
                com.huawei.android.pushagent.utils.tools.d.o(this.fo, "com.huawei.android.push.intent.HEARTBEAT_RSP_TIMEOUT");
                this.fq.ue(false);
                this.fq.vx();
                break;
            case (byte) 65:
                this.fj = true;
                k.pt(this.fo).qm(vh(i2, i));
                k.pt(this.fo).qn(0);
                com.huawei.android.pushagent.model.flowcontrol.b.ib(this.fo).ik(this.fo, ReconnectMgr$RECONNECTEVENT.SOCKET_REG_SUCCESS, new Bundle());
                break;
            default:
                this.fq.vx();
                break;
        }
        Intent intent = new Intent("com.huawei.android.push.intent.MSG_RECEIVED");
        intent.putExtra("push_msg", iPushMessage);
        PushService.aax(intent);
    }
}
