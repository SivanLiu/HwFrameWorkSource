package com.huawei.android.pushagent.utils.threadpool;

import com.huawei.android.pushagent.utils.b.a;

public class b implements Runnable {
    private Runnable gi;

    public b(Runnable runnable) {
        this.gi = runnable;
    }

    public void run() {
        if (this.gi != null) {
            try {
                this.gi.run();
            } catch (Throwable th) {
                a.su("PushLog3414", "exception in task run");
            }
        }
    }
}
