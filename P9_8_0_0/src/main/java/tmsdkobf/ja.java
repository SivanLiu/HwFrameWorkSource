package tmsdkobf;

import android.os.Debug;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import tmsdkobf.iz.a;
import tmsdkobf.pd.c;

public class ja implements a, jb {
    private HashMap<Thread, c> sJ = new HashMap();
    private pd.a sK;
    private final ThreadGroup so = new ThreadGroup("TMS_FREE_POOL_" + sO.getAndIncrement());
    private final AtomicInteger sp = new AtomicInteger(1);

    public Thread a(Runnable runnable, String str, long j) {
        if (str == null || str.length() == 0) {
            str = runnable.getClass().getName();
        }
        Object -l_5_R = new iz(this.so, runnable, "FreeThread-" + this.sp.getAndIncrement() + "-" + str, j);
        -l_5_R.a(this);
        if (-l_5_R.isDaemon()) {
            -l_5_R.setDaemon(false);
        }
        if (-l_5_R.getPriority() != 5) {
            -l_5_R.setPriority(5);
        }
        return -l_5_R;
    }

    public void a(Thread thread, Runnable runnable) {
        Object -l_3_R = new c();
        -l_3_R.Jk = 2;
        -l_3_R.eA = ((iz) thread).bL();
        -l_3_R.name = thread.getName();
        -l_3_R.priority = thread.getPriority();
        -l_3_R.Jm = -1;
        -l_3_R.Jn = -1;
        this.sJ.put(thread, -l_3_R);
        if (this.sK != null) {
            this.sK.a(-l_3_R, activeCount());
        }
    }

    public void a(pd.a aVar) {
        this.sK = aVar;
    }

    public int activeCount() {
        return this.sJ.size();
    }

    public void b(Thread thread, Runnable runnable) {
        c -l_3_R = (c) this.sJ.remove(thread);
        if (-l_3_R != null) {
            -l_3_R.Jm = System.currentTimeMillis() - -l_3_R.Jm;
            -l_3_R.Jn = Debug.threadCpuTimeNanos() - -l_3_R.Jn;
            if (this.sK != null) {
                this.sK.b(-l_3_R);
            }
        }
    }

    public void beforeExecute(Thread thread, Runnable runnable) {
        c -l_3_R = (c) this.sJ.get(thread);
        if (-l_3_R != null) {
            if (this.sK != null) {
                this.sK.a(-l_3_R);
            }
            -l_3_R.Jm = System.currentTimeMillis();
            -l_3_R.Jn = Debug.threadCpuTimeNanos();
        }
    }
}
