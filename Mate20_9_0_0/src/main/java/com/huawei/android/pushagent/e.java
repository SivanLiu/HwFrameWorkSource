package com.huawei.android.pushagent;

import com.huawei.android.pushagent.utils.e.a;
import com.huawei.android.pushagent.utils.f.c;
import java.lang.Thread.UncaughtExceptionHandler;

final class e implements UncaughtExceptionHandler {
    final /* synthetic */ PushService ju;

    e(PushService pushService) {
        this.ju = pushService;
    }

    public void uncaughtException(Thread thread, Throwable th) {
        c.es(PushService.TAG, "uncaughtException:" + th.toString(), th);
        a.db(this.ju.jb, th.toString());
        this.ju.stopSelf();
    }
}
