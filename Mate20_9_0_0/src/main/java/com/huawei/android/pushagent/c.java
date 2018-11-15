package com.huawei.android.pushagent;

import android.content.Intent;
import com.huawei.android.pushagent.model.a.a;

final class c implements Runnable {
    private Intent jq;
    private a jr;
    final /* synthetic */ b js;

    /* synthetic */ c(b bVar, a aVar, Intent intent, c cVar) {
        this(bVar, aVar, intent);
    }

    private c(b bVar, a aVar, Intent intent) {
        this.js = bVar;
        this.jr = aVar;
        this.jq = intent;
    }

    public void run() {
        try {
            this.jr.onReceive(this.js.jn, this.jq);
        } catch (Throwable e) {
            com.huawei.android.pushagent.utils.f.c.es("PushLog3413", "ReceiverDispatcher: call Receiver:" + this.jr.getClass().getSimpleName() + ", intent:" + this.jq + " failed:" + e.toString(), e);
        }
    }
}
