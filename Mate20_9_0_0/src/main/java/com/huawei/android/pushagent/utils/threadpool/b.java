package com.huawei.android.pushagent.utils.threadpool;

import com.huawei.android.pushagent.utils.f.c;

public class b implements Runnable {
    private Runnable af;

    public b(Runnable runnable) {
        this.af = runnable;
    }

    public void run() {
        if (this.af != null) {
            try {
                this.af.run();
            } catch (Throwable th) {
                c.eq("PushLog3413", "exception in task run");
            }
        }
    }
}
