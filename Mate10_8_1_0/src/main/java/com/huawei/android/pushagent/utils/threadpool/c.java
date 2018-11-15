package com.huawei.android.pushagent.utils.threadpool;

import com.huawei.android.pushagent.utils.a.b;

public class c implements Runnable {
    private Runnable m;

    public c(Runnable runnable) {
        this.m = runnable;
    }

    public void run() {
        if (this.m != null) {
            try {
                this.m.run();
            } catch (Throwable th) {
                b.y("PushLog2976", "exception in task run");
            }
        }
    }
}
