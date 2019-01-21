package com.android.internal.util;

import android.os.Process;
import android.util.Slog;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentUtils {
    private ConcurrentUtils() {
    }

    public static ExecutorService newFixedThreadPool(int nThreads, final String poolName, final int linuxThreadPriority) {
        return Executors.newFixedThreadPool(nThreads, new ThreadFactory() {
            private final AtomicInteger threadNum = new AtomicInteger(0);

            public Thread newThread(final Runnable r) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(poolName);
                stringBuilder.append(this.threadNum.incrementAndGet());
                return new Thread(stringBuilder.toString()) {
                    public void run() {
                        Process.setThreadPriority(linuxThreadPriority);
                        r.run();
                    }
                };
            }
        });
    }

    public static <T> T waitForFutureNoInterrupt(Future<T> future, String description) {
        StringBuilder stringBuilder;
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stringBuilder = new StringBuilder();
            stringBuilder.append(description);
            stringBuilder.append(" interrupted");
            throw new IllegalStateException(stringBuilder.toString());
        } catch (ExecutionException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(description);
            stringBuilder.append(" failed");
            throw new RuntimeException(stringBuilder.toString(), e2);
        }
    }

    public static void waitForCountDownNoInterrupt(CountDownLatch countDownLatch, long timeoutMs, String description) {
        try {
            if (!countDownLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(description);
                stringBuilder.append(" timed out.");
                throw new IllegalStateException(stringBuilder.toString());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(description);
            stringBuilder2.append(" interrupted.");
            throw new IllegalStateException(stringBuilder2.toString());
        }
    }

    public static void wtfIfLockHeld(String tag, Object lock) {
        if (Thread.holdsLock(lock)) {
            Slog.wtf(tag, "Lock mustn't be held");
        }
    }

    public static void wtfIfLockNotHeld(String tag, Object lock) {
        if (!Thread.holdsLock(lock)) {
            Slog.wtf(tag, "Lock must be held");
        }
    }
}
