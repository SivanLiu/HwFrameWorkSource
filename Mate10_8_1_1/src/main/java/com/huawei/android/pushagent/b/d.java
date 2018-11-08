package com.huawei.android.pushagent.b;

import com.huawei.android.pushagent.utils.a.b;

final class d implements Runnable {
    final /* synthetic */ int iu;
    final /* synthetic */ String iv;

    d(int i, String str) {
        this.iu = i;
        this.iv = str;
    }

    public void run() {
        if (a.appCtx == null || a.ir == null) {
            b.y("PushLog2976", "Please init reporter first");
        } else if (a.aap(this.iu)) {
            a.ir.zx(String.valueOf(this.iu), this.iv);
        }
    }
}
