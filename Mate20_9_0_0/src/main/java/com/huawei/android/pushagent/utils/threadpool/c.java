package com.huawei.android.pushagent.utils.threadpool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class c implements ThreadFactory {
    private final ThreadGroup ag;
    private final String ah;
    private final int ai;
    private final AtomicInteger aj;

    public c(String str, int i) {
        this.aj = new AtomicInteger(1);
        this.ai = i;
        SecurityManager securityManager = System.getSecurityManager();
        this.ag = securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.ah = str + "-pool-thread-";
    }

    public c(String str) {
        this(str, 5);
    }

    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(this.ag, runnable, this.ah + this.aj.getAndIncrement(), 0);
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != this.ai) {
            thread.setPriority(this.ai);
        }
        return thread;
    }
}
