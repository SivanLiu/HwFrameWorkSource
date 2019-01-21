package com.huawei.android.pushagent;

import com.huawei.android.pushagent.utils.b.a;
import java.lang.Thread.UncaughtExceptionHandler;

final class f implements UncaughtExceptionHandler {
    final /* synthetic */ PushService jv;

    f(PushService pushService) {
        this.jv = pushService;
    }

    public void uncaughtException(Thread thread, Throwable th) {
        a.sw(PushService.TAG, "uncaughtException:" + th.toString(), th);
        com.huawei.android.pushagent.utils.f.a.xs(this.jv.jj, th.toString());
        this.jv.stopSelf();
    }
}
