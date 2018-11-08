package sun.nio.ch;

import java.util.concurrent.ThreadFactory;

final /* synthetic */ class -$Lambda$MfIm8har2wOVFJJtpwtPJYPKn8o implements ThreadFactory {
    private final /* synthetic */ Thread $m$0(Runnable arg0) {
        return ThreadPool.lambda$-sun_nio_ch_ThreadPool_2676(arg0);
    }

    public final Thread newThread(Runnable runnable) {
        return $m$0(runnable);
    }
}
