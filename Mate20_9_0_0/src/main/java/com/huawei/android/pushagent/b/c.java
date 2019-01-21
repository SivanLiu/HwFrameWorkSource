package com.huawei.android.pushagent.b;

import com.huawei.android.pushagent.utils.b.a;

final class c implements Runnable {
    final /* synthetic */ boolean iy;

    c(boolean z) {
        this.iy = z;
    }

    public void run() {
        if (a.appCtx == null || a.iw == null) {
            a.su("PushLog3414", "Please init reporter first");
        } else {
            a.iw.aba(this.iy);
        }
    }
}
