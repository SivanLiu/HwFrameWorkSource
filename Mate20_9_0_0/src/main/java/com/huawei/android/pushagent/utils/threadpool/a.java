package com.huawei.android.pushagent.utils.threadpool;

import com.huawei.android.pushagent.utils.f.c;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class a {
    private static Map<AsyncExec$ThreadType, ExecutorService> x;

    private static synchronized void ch() {
        synchronized (a.class) {
            if (x == null) {
                Map hashMap = new HashMap();
                ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), new c("IO"));
                threadPoolExecutor.allowCoreThreadTimeOut(true);
                ThreadPoolExecutor threadPoolExecutor2 = new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), new c("Net"));
                threadPoolExecutor2.allowCoreThreadTimeOut(true);
                ThreadPoolExecutor threadPoolExecutor3 = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), new c("SeqNet"));
                threadPoolExecutor3.allowCoreThreadTimeOut(true);
                ThreadPoolExecutor threadPoolExecutor4 = new ThreadPoolExecutor(3, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), new c("Cal"));
                threadPoolExecutor4.allowCoreThreadTimeOut(true);
                ThreadPoolExecutor threadPoolExecutor5 = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), new c("Seq"));
                ThreadPoolExecutor threadPoolExecutor6 = new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), new c("Report"));
                hashMap.put(AsyncExec$ThreadType.IO, threadPoolExecutor);
                hashMap.put(AsyncExec$ThreadType.NETWORK, threadPoolExecutor2);
                hashMap.put(AsyncExec$ThreadType.SEQNETWORK, threadPoolExecutor3);
                hashMap.put(AsyncExec$ThreadType.CALCULATION, threadPoolExecutor4);
                hashMap.put(AsyncExec$ThreadType.SEQUENCE, threadPoolExecutor5);
                hashMap.put(AsyncExec$ThreadType.REPORT_SEQ, threadPoolExecutor6);
                x = hashMap;
            }
        }
    }

    static {
        ch();
    }

    static void ci(Runnable runnable, AsyncExec$ThreadType asyncExec$ThreadType) {
        if (runnable != null) {
            ExecutorService executorService = (ExecutorService) x.get(asyncExec$ThreadType);
            if (executorService != null) {
                executorService.execute(new b(runnable));
            } else {
                c.eo("PushLog3413", "no executor for type: " + asyncExec$ThreadType);
            }
        }
    }

    public static void cj(Runnable runnable) {
        ci(runnable, AsyncExec$ThreadType.IO);
    }

    public static void ck(Runnable runnable) {
        ci(runnable, AsyncExec$ThreadType.NETWORK);
    }

    public static void cf(Runnable runnable) {
        ci(runnable, AsyncExec$ThreadType.SEQNETWORK);
    }

    public static void cg(Runnable runnable) {
        ci(runnable, AsyncExec$ThreadType.REPORT_SEQ);
    }
}
