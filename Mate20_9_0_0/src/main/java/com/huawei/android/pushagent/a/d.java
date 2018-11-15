package com.huawei.android.pushagent.a;

import com.huawei.android.pushagent.utils.f.c;

final class d implements Runnable {
    final /* synthetic */ int bv;
    final /* synthetic */ String bw;

    d(int i, String str) {
        this.bv = i;
        this.bw = str;
    }

    public void run() {
        if (a.appCtx == null || a.bs == null) {
            c.eq("PushLog3413", "Please init reporter first");
        } else if (a.hu(this.bv)) {
            a.bs.he(String.valueOf(this.bv), this.bw);
        }
    }
}
