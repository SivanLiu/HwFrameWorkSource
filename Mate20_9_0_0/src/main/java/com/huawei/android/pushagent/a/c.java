package com.huawei.android.pushagent.a;

final class c implements Runnable {
    final /* synthetic */ boolean bu;

    c(boolean z) {
        this.bu = z;
    }

    public void run() {
        if (a.appCtx == null || a.bs == null) {
            com.huawei.android.pushagent.utils.f.c.eq("PushLog3413", "Please init reporter first");
        } else {
            a.bs.hg(this.bu);
        }
    }
}
