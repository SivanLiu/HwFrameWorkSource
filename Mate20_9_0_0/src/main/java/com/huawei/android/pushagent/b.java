package com.huawei.android.pushagent;

import android.content.Intent;
import com.huawei.android.pushagent.model.d.c;
import com.huawei.android.pushagent.utils.b.a;

final class b implements Runnable {
    private Intent jg;
    private c jh;
    final /* synthetic */ a ji;

    /* synthetic */ b(a aVar, c cVar, Intent intent, b bVar) {
        this(aVar, cVar, intent);
    }

    private b(a aVar, c cVar, Intent intent) {
        this.ji = aVar;
        this.jh = cVar;
        this.jg = intent;
    }

    public void run() {
        try {
            this.jh.onReceive(this.ji.jc, this.jg);
        } catch (Exception e) {
            a.sw("PushLog3414", "ReceiverDispatcher: call Receiver:" + this.jh.getClass().getSimpleName() + ", intent:" + this.jg + " failed:" + e.toString(), e);
        }
    }
}
