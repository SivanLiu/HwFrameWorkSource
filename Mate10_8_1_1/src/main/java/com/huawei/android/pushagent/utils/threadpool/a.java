package com.huawei.android.pushagent.utils.threadpool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class a implements ThreadFactory {
    private final ThreadGroup a;
    private final String b;
    private final int c;
    private final AtomicInteger d;

    public a(String str, int i) {
        this.d = new AtomicInteger(1);
        this.c = i;
        SecurityManager securityManager = System.getSecurityManager();
        this.a = securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.b = str + "-pool-thread-";
    }

    public a(String str) {
        this(str, 5);
    }

    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(this.a, runnable, this.b + this.d.getAndIncrement(), 0);
        if (thread.isDaemon()) {
            thread.setDaemon(false);
        }
        if (thread.getPriority() != this.c) {
            thread.setPriority(this.c);
        }
        return thread;
    }
}
