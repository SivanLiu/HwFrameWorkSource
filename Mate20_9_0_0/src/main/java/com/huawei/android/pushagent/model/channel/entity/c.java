package com.huawei.android.pushagent.model.channel.entity;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.model.prefs.k;
import com.huawei.android.pushagent.utils.bastet.a;
import com.huawei.android.pushagent.utils.g;
import com.huawei.android.pushagent.utils.tools.d;

public abstract class c {
    public Context ef = null;
    public boolean eg = false;
    public boolean eh = false;
    protected a ei;

    public abstract void ky(boolean z, boolean z2);

    public abstract void kz(boolean z);

    public abstract void lb();

    public abstract long ld(boolean z);

    public abstract boolean lg();

    public abstract void lj();

    public abstract void lm();

    public c(Context context) {
        this.ef = context;
        this.ei = a.dk(context);
    }

    public void mw(boolean z) {
        this.eh = z;
    }

    public void mv(boolean z) {
        this.eg = z;
        com.huawei.android.pushagent.utils.f.c.er("PushLog3413", "setIsFirstHeartBeat:" + this.eg);
    }

    public void mz(Context context) {
        com.huawei.android.pushagent.utils.f.c.ep("PushLog3413", "send the first heartbeat.");
        int fw = g.fw(context);
        long j = 0;
        if (fw == 0) {
            j = k.rh(context).ty();
        } else if (1 == fw) {
            j = k.rh(context).tz();
        }
        kz(true);
        my(j, false);
        mv(true);
    }

    protected void my(long j, boolean z) {
        if (!this.ei.di()) {
            Intent intent = new Intent("com.huawei.push.alarm.HEARTBEAT");
            intent.putExtra("isHeartbeatReq", z);
            intent.setPackage(this.ef.getPackageName());
            d.cw(this.ef, intent, j);
        }
    }

    protected void mx(long j, long j2) {
        if (!this.ei.di()) {
            Intent intent = new Intent("com.huawei.push.alarm.HEARTBEAT");
            intent.putExtra("isHeartbeatReq", true);
            intent.setPackage(this.ef.getPackageName());
            d.da(this.ef, intent, j, j2);
        }
    }

    public String toString() {
        return new StringBuffer().append("heartBeatInterval").append(ld(false)).toString();
    }
}
