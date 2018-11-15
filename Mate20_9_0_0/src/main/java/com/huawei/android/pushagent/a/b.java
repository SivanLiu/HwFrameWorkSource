package com.huawei.android.pushagent.a;

import android.content.Context;
import com.huawei.android.pushagent.utils.f.c;

final class b implements Runnable {
    final /* synthetic */ Context bt;

    b(Context context) {
        this.bt = context;
    }

    public void run() {
        if (this.bt == null) {
            c.eq("PushLog3413", "init reporter failed, context is null");
            return;
        }
        a.appCtx = this.bt.getApplicationContext();
        a.bs = new com.huawei.android.pushagent.a.a.c();
        a.bs.hd(this.bt);
    }
}
