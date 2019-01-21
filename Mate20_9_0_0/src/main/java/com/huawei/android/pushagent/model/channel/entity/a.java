package com.huawei.android.pushagent.model.channel.entity;

import android.content.Context;
import android.content.Intent;
import com.huawei.android.pushagent.utils.d;

public abstract class a {
    public Context ak = null;
    public boolean al = false;
    public boolean am = false;
    protected com.huawei.android.pushagent.utils.bastet.a an;

    public abstract void bf(boolean z, boolean z2);

    public abstract void bg(boolean z);

    public abstract void bi();

    public abstract long bk(boolean z);

    public abstract boolean bn();

    public abstract void bq();

    public abstract void bt();

    public a(Context context) {
        this.ak = context;
        this.an = com.huawei.android.pushagent.utils.bastet.a.xd(context);
    }

    public void ck(boolean z) {
        this.am = z;
    }

    public void cj(boolean z) {
        this.al = z;
        com.huawei.android.pushagent.utils.b.a.st("PushLog3414", "setIsFirstHeartBeat:" + this.al);
    }

    public void cn(Context context) {
        com.huawei.android.pushagent.utils.b.a.sv("PushLog3414", "send the first heartbeat.");
        int yh = d.yh(context);
        long j = 0;
        if (yh == 0) {
            j = com.huawei.android.pushagent.model.prefs.a.ff(context).fo();
        } else if (1 == yh) {
            j = com.huawei.android.pushagent.model.prefs.a.ff(context).fp();
        }
        bg(true);
        cm(j, false);
        cj(true);
    }

    protected void cm(long j, boolean z) {
        if (!this.an.xb()) {
            Intent intent = new Intent("com.huawei.push.alarm.HEARTBEAT");
            intent.putExtra("isHeartbeatReq", z);
            intent.setPackage(this.ak.getPackageName());
            com.huawei.android.pushagent.utils.tools.a.sa(this.ak, intent, j);
        }
    }

    protected void cl(long j, long j2) {
        if (!this.an.xb()) {
            Intent intent = new Intent("com.huawei.push.alarm.HEARTBEAT");
            intent.putExtra("isHeartbeatReq", true);
            intent.setPackage(this.ak.getPackageName());
            com.huawei.android.pushagent.utils.tools.a.sb(this.ak, intent, j, j2);
        }
    }

    public String toString() {
        return new StringBuffer().append("heartBeatInterval").append(bk(false)).toString();
    }
}
