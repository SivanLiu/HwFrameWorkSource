package com.huawei.android.pushagent.b;

import com.huawei.android.pushagent.utils.a.b;

final class c implements Runnable {
    final /* synthetic */ boolean it;

    c(boolean z) {
        this.it = z;
    }

    public void run() {
        if (a.appCtx == null || a.ir == null) {
            b.y("PushLog2976", "Please init reporter first");
        } else {
            a.ir.zz(this.it);
        }
    }
}
