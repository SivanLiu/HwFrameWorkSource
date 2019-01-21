package com.huawei.android.pushagent.utils.threadpool;

import com.huawei.android.pushagent.utils.b.a;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class c {
    private static Map<AsyncExec$ThreadType, ExecutorService> gj;

    private static synchronized void sr() {
        synchronized (c.class) {
            if (gj == null) {
                HashMap hashMap = new HashMap();
                ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), new a("IO"));
                threadPoolExecutor.allowCoreThreadTimeOut(true);
                ThreadPoolExecutor threadPoolExecutor2 = new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), new a("Net"));
                threadPoolExecutor2.allowCoreThreadTimeOut(true);
                ThreadPoolExecutor threadPoolExecutor3 = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), new a("SeqNet"));
                threadPoolExecutor3.allowCoreThreadTimeOut(true);
                ThreadPoolExecutor threadPoolExecutor4 = new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), new a("Cal"));
                threadPoolExecutor4.allowCoreThreadTimeOut(true);
                ThreadPoolExecutor threadPoolExecutor5 = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), new a("Seq"));
                ThreadPoolExecutor threadPoolExecutor6 = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), new a("Report"));
                hashMap.put(AsyncExec$ThreadType.IO, threadPoolExecutor);
                hashMap.put(AsyncExec$ThreadType.NETWORK, threadPoolExecutor2);
                hashMap.put(AsyncExec$ThreadType.SEQNETWORK, threadPoolExecutor3);
                hashMap.put(AsyncExec$ThreadType.CALCULATION, threadPoolExecutor4);
                hashMap.put(AsyncExec$ThreadType.SEQUENCE, threadPoolExecutor5);
                hashMap.put(AsyncExec$ThreadType.REPORT_SEQ, threadPoolExecutor6);
                gj = hashMap;
            }
        }
    }

    static {
        sr();
    }

    static void ss(Runnable runnable, AsyncExec$ThreadType asyncExec$ThreadType) {
        if (runnable != null) {
            ExecutorService executorService = (ExecutorService) gj.get(asyncExec$ThreadType);
            if (executorService != null) {
                executorService.execute(new b(runnable));
            } else {
                a.sx("PushLog3414", "no executor for type: " + asyncExec$ThreadType);
            }
        }
    }

    public static void sq(Runnable runnable) {
        ss(runnable, AsyncExec$ThreadType.IO);
    }

    public static void sp(Runnable runnable) {
        ss(runnable, AsyncExec$ThreadType.NETWORK);
    }

    public static void sn(Runnable runnable) {
        ss(runnable, AsyncExec$ThreadType.SEQNETWORK);
    }

    public static void so(Runnable runnable) {
        ss(runnable, AsyncExec$ThreadType.REPORT_SEQ);
    }
}
