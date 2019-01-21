package com.huawei.android.pushagent.utils.threadpool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class a implements ThreadFactory {
    private final ThreadGroup ge;
    private final String gf;
    private final int gg;
    private final AtomicInteger gh;

    public a(String str, int i) {
        this.gh = new AtomicInteger(1);
        this.gg = i;
        SecurityManager securityManager = System.getSecurityManager();
        this.ge = securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.gf = str + "-pool-thread-";
    }

    public a(String str) {
        this(str, 5);
    }

    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(this.ge, runnable, this.gf + this.gh.getAndIncrement(), 0);
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != this.gg) {
            thread.setPriority(this.gg);
        }
        return thread;
    }
}
