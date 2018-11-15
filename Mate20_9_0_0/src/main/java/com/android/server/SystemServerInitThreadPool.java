package com.android.server;

import android.os.Build;
import android.util.Slog;
import com.android.internal.util.ConcurrentUtils;
import com.android.internal.util.Preconditions;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SystemServerInitThreadPool {
    private static final boolean IS_DEBUGGABLE = Build.IS_DEBUGGABLE;
    private static final int SHUTDOWN_TIMEOUT_MILLIS = 20000;
    private static final String TAG = SystemServerInitThreadPool.class.getSimpleName();
    private static SystemServerInitThreadPool sInstance;
    private ExecutorService mService = ConcurrentUtils.newFixedThreadPool(4, "system-server-init-thread", -2);

    public static synchronized SystemServerInitThreadPool get() {
        SystemServerInitThreadPool systemServerInitThreadPool;
        synchronized (SystemServerInitThreadPool.class) {
            if (sInstance == null) {
                sInstance = new SystemServerInitThreadPool();
            }
            boolean z = sInstance.mService != null;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot get ");
            stringBuilder.append(TAG);
            stringBuilder.append(" - it has been shut down");
            Preconditions.checkState(z, stringBuilder.toString());
            systemServerInitThreadPool = sInstance;
        }
        return systemServerInitThreadPool;
    }

    public Future<?> submit(Runnable runnable, String description) {
        return IS_DEBUGGABLE ? this.mService.submit(new -$$Lambda$SystemServerInitThreadPool$7wfLGkZF7FvYZv7xj3ghvuiJJGk(description, runnable)) : this.mService.submit(runnable);
    }

    static /* synthetic */ void lambda$submit$0(String description, Runnable runnable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Started executing ");
        stringBuilder.append(description);
        Slog.d(str, stringBuilder.toString());
        try {
            runnable.run();
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Finished executing ");
            stringBuilder.append(description);
            Slog.d(str, stringBuilder.toString());
        } catch (RuntimeException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failure in ");
            stringBuilder2.append(description);
            stringBuilder2.append(": ");
            stringBuilder2.append(e);
            Slog.e(str2, stringBuilder2.toString(), e);
            throw e;
        }
    }

    static synchronized void shutdown() {
        synchronized (SystemServerInitThreadPool.class) {
            if (!(sInstance == null || sInstance.mService == null)) {
                sInstance.mService.shutdown();
                try {
                    boolean terminated = sInstance.mService.awaitTermination(20000, TimeUnit.MILLISECONDS);
                    List<Runnable> unstartedRunnables = sInstance.mService.shutdownNow();
                    if (terminated) {
                        sInstance.mService = null;
                        Slog.d(TAG, "Shutdown successful");
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Cannot shutdown. Unstarted tasks ");
                        stringBuilder.append(unstartedRunnables);
                        throw new IllegalStateException(stringBuilder.toString());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(TAG);
                    stringBuilder2.append(" init interrupted");
                    throw new IllegalStateException(stringBuilder2.toString());
                }
            }
        }
    }
}
