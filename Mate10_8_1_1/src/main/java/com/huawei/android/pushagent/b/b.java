package com.huawei.android.pushagent.b;

import android.content.Context;

final class b implements Runnable {
    final /* synthetic */ Context is;

    b(Context context) {
        this.is = context;
    }

    public void run() {
        if (this.is == null) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "init reporter failed, context is null");
            return;
        }
        a.appCtx = this.is.getApplicationContext();
        a.ir = new com.huawei.android.pushagent.b.a.b();
        a.ir.zw(this.is);
    }
}
