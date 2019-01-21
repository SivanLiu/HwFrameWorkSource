package com.huawei.android.pushagent.b;

import com.huawei.android.pushagent.utils.b.a;

final class d implements Runnable {
    final /* synthetic */ int iz;
    final /* synthetic */ String ja;

    d(int i, String str) {
        this.iz = i;
        this.ja = str;
    }

    public void run() {
        if (a.appCtx == null || a.iw == null) {
            a.su("PushLog3414", "Please init reporter first");
        } else if (a.abh(this.iz)) {
            a.iw.aay(String.valueOf(this.iz), this.ja);
        }
    }
}
