package com.huawei.android.pushagent;

import android.content.Intent;
import com.huawei.android.pushagent.model.a.d;
import com.huawei.android.pushagent.utils.a.b;

final class c implements Runnable {
    private Intent jl;
    private d jm;
    final /* synthetic */ b jn;

    private c(b bVar, d dVar, Intent intent) {
        this.jn = bVar;
        this.jm = dVar;
        this.jl = intent;
    }

    public void run() {
        try {
            this.jm.onReceive(this.jn.ji, this.jl);
        } catch (Throwable e) {
            b.aa("PushLog2976", "ReceiverDispatcher: call Receiver:" + this.jm.getClass().getSimpleName() + ", intent:" + this.jl + " failed:" + e.toString(), e);
        }
    }
}
