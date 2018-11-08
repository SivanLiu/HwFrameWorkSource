package com.huawei.android.pushselfshow.richpush.favorites;

import com.huawei.android.pushagent.a.a.c;

class d implements Runnable {
    final /* synthetic */ c a;

    d(c cVar) {
        this.a = cVar;
    }

    public void run() {
        c.a("PushSelfShowLog", "deleteTipDialog mThread run");
        int -l_2_I = 0;
        for (e -l_4_R : this.a.a.h.a()) {
            if (-l_4_R.a()) {
                com.huawei.android.pushselfshow.utils.a.d.a(this.a.a.b, -l_4_R.c());
                -l_2_I = 1;
            }
        }
        if (-l_2_I != 0) {
            if (!this.a.a.k) {
                this.a.a.h.b();
            }
            this.a.a.a.sendEmptyMessage(1001);
        }
    }
}
