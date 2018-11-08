package com.huawei.android.pushagent;

import com.huawei.android.pushagent.utils.a.b;
import com.huawei.android.pushagent.utils.d.a;
import java.lang.Thread.UncaughtExceptionHandler;

final class e implements UncaughtExceptionHandler {
    final /* synthetic */ PushService jp;

    e(PushService pushService) {
        this.jp = pushService;
    }

    public void uncaughtException(Thread thread, Throwable th) {
        b.aa(PushService.TAG, "uncaughtException:" + th.toString(), th);
        a.cz(this.jp.iw, th.toString());
        this.jp.stopSelf();
    }
}
