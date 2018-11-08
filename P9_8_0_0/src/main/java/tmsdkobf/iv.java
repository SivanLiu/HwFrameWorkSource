package tmsdkobf;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class iv implements ThreadFactory, jb {
    private final ThreadGroup so = new ThreadGroup("TMS-COMMON");
    private final AtomicInteger sp = new AtomicInteger(1);
    private final String sq = ("Common Thread Pool-" + sO.getAndIncrement() + "-Thread-");

    iv() {
    }

    public Thread newThread(Runnable runnable) {
        Object -l_2_R = new Thread(this.so, runnable, this.sq + this.sp.getAndIncrement(), 0);
        if (-l_2_R.isDaemon()) {
            -l_2_R.setDaemon(false);
        }
        if (-l_2_R.getPriority() != 5) {
            -l_2_R.setPriority(5);
        }
        return -l_2_R;
    }
}
