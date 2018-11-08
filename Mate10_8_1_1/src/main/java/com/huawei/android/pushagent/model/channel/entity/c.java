package com.huawei.android.pushagent.model.channel.entity;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.model.prefs.i;
import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.bastet.a;
import com.huawei.android.pushagent.utils.f;
import com.huawei.android.pushagent.utils.tools.d;

public abstract class c {
    public Context gl = null;
    public boolean gm = false;
    protected a gn;

    public abstract void ue(boolean z);

    public abstract void uf(boolean z);

    public abstract long uj(boolean z);

    public abstract void uo();

    public abstract void uq();

    public c(Context context) {
        this.gl = context;
        this.gn = a.cg(context);
    }

    public void vv(boolean z) {
        this.gm = z;
    }

    public void vx() {
        if (com.huawei.android.pushagent.model.channel.a.wx(this.gl) == this) {
            long uj = uj(false);
            b.z("PushLog2976", "after delayHeartBeatReq, nextHeartBeatTime, will be " + uj + "ms later");
            vy(uj, true);
        }
    }

    public void vw(Context context) {
        b.z("PushLog2976", "send the first heartbeat.");
        int fp = f.fp(context);
        long j = 0;
        if (fp == 0) {
            j = i.mj(context).ph();
        } else if (1 == fp) {
            j = i.mj(context).pi();
        }
        uf(true);
        vy(j, false);
    }

    private void vy(long j, boolean z) {
        if (this.gn.cn()) {
            b.z("PushLog2976", "support bastet, need not to send delayed heartbeat");
            return;
        }
        Intent intent = new Intent("com.huawei.intent.action.PUSH");
        intent.putExtra("EXTRA_INTENT_TYPE", "com.huawei.android.push.intent.HEARTBEAT_REQ");
        intent.putExtra("isHeartbeatReq", z);
        intent.setPackage(this.gl.getPackageName());
        d.p(this.gl, intent, j);
    }

    public String toString() {
        return new StringBuffer().append("heartBeatInterval").append(uj(false)).toString();
    }
}
