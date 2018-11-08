package com.huawei.android.pushselfshow.richpush.html.api;

import com.huawei.android.pushagent.a.a.c;

class a implements Runnable {
    final /* synthetic */ OnlineEventsBridgeMode a;

    a(OnlineEventsBridgeMode onlineEventsBridgeMode) {
        this.a = onlineEventsBridgeMode;
    }

    public void run() {
        boolean z = false;
        int -l_1_I = this.a.c.d();
        c.a("PushSelfShowLog", "bEmptyMsg is " + -l_1_I);
        if (-l_1_I == 0) {
            OnlineEventsBridgeMode onlineEventsBridgeMode = this.a;
            if (!this.a.a) {
                z = true;
            }
            onlineEventsBridgeMode.a = z;
            this.a.c.a.setNetworkAvailable(this.a.a);
            c.a("PushSelfShowLog", "setNetworkAvailable ： " + this.a.a);
        }
    }
}
