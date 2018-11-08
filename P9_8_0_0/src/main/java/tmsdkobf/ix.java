package tmsdkobf;

import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import tmsdk.common.creator.BaseManagerC;
import tmsdkobf.pd.c;

public class ix extends BaseManagerC implements tmsdkobf.iw.a, pd {
    private static long sC = 0;
    private static long sD = 0;
    private static tmsdkobf.pd.a sG = new tmsdkobf.pd.a() {
        public void a(c cVar) {
            Object -l_2_R = ix.st.iterator();
            while (-l_2_R.hasNext()) {
                ((tmsdkobf.pd.a) -l_2_R.next()).a(cVar);
            }
        }

        public void a(c cVar, int i) {
            Object -l_3_R = ix.st.iterator();
            while (-l_3_R.hasNext()) {
                ((tmsdkobf.pd.a) -l_3_R.next()).a(cVar, i);
            }
        }

        public void b(c cVar) {
            Object -l_2_R = ix.st.iterator();
            while (-l_2_R.hasNext()) {
                ((tmsdkobf.pd.a) -l_2_R.next()).b(cVar);
            }
        }
    };
    private static ArrayList<tmsdkobf.pd.a> st = new ArrayList();
    private boolean isActive = false;
    private Object mLock = new Object();
    private HandlerThread sA;
    private b sB;
    private volatile boolean sE = false;
    private ja sF;
    private ArrayList<tmsdkobf.pd.b> ss = new ArrayList();
    protected PriorityBlockingQueue<Runnable> su = new PriorityBlockingQueue(5);
    protected LinkedList<a> sv = new LinkedList();
    protected ArrayList<a> sw = new ArrayList();
    protected HashMap<a, Thread> sx = new HashMap();
    private int sy;
    protected iw sz = null;

    class a implements Comparable<a>, Runnable {
        final /* synthetic */ ix sH;
        private c sI = new c();

        public a(ix ixVar, int i, Runnable runnable, String str, long j, boolean z, Object obj) {
            this.sH = ixVar;
            if (str == null || str.length() == 0) {
                str = runnable.getClass().getName();
            }
            this.sI.Jk = 1;
            this.sI.priority = i;
            this.sI.name = str;
            this.sI.eA = j;
            this.sI.Jp = runnable;
            this.sI.Jo = z;
            this.sI.Jq = obj;
            this.sI.Jl = System.currentTimeMillis();
        }

        public int a(a aVar) {
            int -l_4_I = (int) (Math.abs(System.currentTimeMillis() - this.sI.Jl) / 200);
            int -l_5_I = this.sI.priority;
            if (-l_4_I > 0) {
                -l_5_I += -l_4_I;
            }
            return aVar.sI.priority - -l_5_I;
        }

        public c cm() {
            return this.sI;
        }

        public /* synthetic */ int compareTo(Object obj) {
            return a((a) obj);
        }

        public void run() {
            if (this.sI != null && this.sI.Jp != null) {
                this.sI.Jp.run();
            }
        }
    }

    class b extends Handler {
        final /* synthetic */ ix sH;

        public b(ix ixVar, Looper looper) {
            this.sH = ixVar;
            super(looper);
        }

        public void handleMessage(Message message) {
            int i = 0;
            switch (message.what) {
                case 1:
                    removeMessages(message.what);
                    if (this.sH.cg()) {
                        mb.n("ThreadPool", "thread pool is pause");
                        long -l_2_J = System.currentTimeMillis();
                        if ((ix.sC <= 0 ? 1 : 0) == 0) {
                            if (Math.abs(ix.sD - -l_2_J) <= ix.sC) {
                                i = 1;
                            }
                            if (i == 0) {
                                mb.n("ThreadPool", "thread pool is auto wakeup");
                                this.sH.cf();
                            }
                        }
                        sendEmptyMessageDelayed(1, 1000);
                        return;
                    }
                    this.sH.cd();
                    return;
                default:
                    return;
            }
        }
    }

    private int cb() {
        int -l_2_I = (Runtime.getRuntime().availableProcessors() * 4) + 2;
        return -l_2_I <= 16 ? -l_2_I : 16;
    }

    private int cc() {
        return cb() * 2;
    }

    private void cd() {
        synchronized (this.mLock) {
            if (!this.sv.isEmpty()) {
                Object -l_2_R = this.sv.iterator();
                if (-l_2_R != null && -l_2_R.hasNext()) {
                    a -l_3_R = (a) -l_2_R.next();
                    -l_2_R.remove();
                    ce();
                    this.sz.execute(-l_3_R);
                    Object -l_4_R = st.iterator();
                    while (-l_4_R.hasNext()) {
                        ((tmsdkobf.pd.a) -l_4_R.next()).a(-l_3_R.cm(), this.sz.getActiveCount());
                    }
                }
            }
            if (!this.sv.isEmpty()) {
                this.sB.sendEmptyMessage(1);
            }
        }
    }

    private void ce() {
        if (this.sz.getCorePoolSize() < this.sy) {
            this.sz.setCorePoolSize(this.sy);
            this.sz.setMaximumPoolSize(this.sy);
        }
    }

    private boolean cg() {
        return this.sE;
    }

    private void ch() {
        if (this.sF == null) {
            this.sF = new ja();
            this.sF.a(new tmsdkobf.pd.a(this) {
                final /* synthetic */ ix sH;

                {
                    this.sH = r1;
                }

                public void a(c cVar) {
                    Object -l_2_R = ix.st.iterator();
                    while (-l_2_R.hasNext()) {
                        ((tmsdkobf.pd.a) -l_2_R.next()).a(cVar);
                    }
                }

                public void a(c cVar, int i) {
                    Object -l_3_R = ix.st.iterator();
                    while (-l_3_R.hasNext()) {
                        ((tmsdkobf.pd.a) -l_3_R.next()).a(cVar, i);
                    }
                }

                public void b(c cVar) {
                    Object -l_2_R = ix.st.iterator();
                    while (-l_2_R.hasNext()) {
                        ((tmsdkobf.pd.a) -l_2_R.next()).b(cVar);
                    }
                }
            });
        }
    }

    public static tmsdkobf.pd.a ci() {
        return sG;
    }

    public HandlerThread a(String str, int i, long j) {
        return iy.a(str, i, j);
    }

    public Thread a(Runnable runnable, String str, long j) {
        ch();
        return this.sF.a(runnable, str, j);
    }

    public void a(int i, Runnable runnable, String str, long j, boolean z, Object obj) {
        synchronized (this.mLock) {
            Object -l_9_R = new a(this, i, runnable, str, j, z, obj);
            this.sv.add(-l_9_R);
            this.sw.add(-l_9_R);
            this.sB.sendEmptyMessage(1);
        }
    }

    public void a(Runnable runnable) {
        synchronized (this.mLock) {
            Object -l_3_R = c(runnable);
            if (-l_3_R == null) {
                b(runnable);
            } else {
                -l_3_R.interrupt();
            }
        }
    }

    public void a(Runnable runnable, String str, long j, boolean z, Object obj) {
        a(5, runnable, str, j, z, obj);
    }

    public void afterExecute(Runnable runnable, Throwable th) {
        synchronized (this.mLock) {
            int -l_6_I;
            a -l_4_R = (a) runnable;
            Object -l_5_R = this.sx.keySet().iterator();
            if (-l_5_R != null) {
                -l_6_I = 0;
                while (-l_5_R.hasNext()) {
                    a -l_7_R = (a) -l_5_R.next();
                    if (-l_7_R != null && -l_7_R.equals(-l_4_R)) {
                        -l_5_R.remove();
                        -l_6_I = 1;
                        break;
                    }
                }
                if (-l_6_I != 0) {
                    -l_4_R.cm().Jm = System.currentTimeMillis() - -l_4_R.cm().Jm;
                    -l_4_R.cm().Jn = Debug.threadCpuTimeNanos() - -l_4_R.cm().Jn;
                    Object -l_11_R = st.iterator();
                    while (-l_11_R.hasNext()) {
                        ((tmsdkobf.pd.a) -l_11_R.next()).b(-l_4_R.cm());
                    }
                }
            }
            -l_6_I = this.sz.getActiveCount();
            int -l_7_I = this.sz.getQueue().size();
            int -l_8_I = this.sz.getCorePoolSize();
            if (-l_6_I == 1 && -l_7_I == 0) {
                if (-l_8_I > 0) {
                    this.sy = cb();
                    this.sz.setCorePoolSize(0);
                    this.sz.setMaximumPoolSize(this.sy + 2);
                    mb.n("ThreadPool", "shrink core pool size: " + this.sz.getCorePoolSize());
                }
                Object -l_9_R = this.ss.iterator();
                while (-l_9_R.hasNext()) {
                    ((tmsdkobf.pd.b) -l_9_R.next()).hG();
                }
                this.isActive = false;
            }
        }
    }

    public void b(Runnable runnable, String str, long j, boolean z, Object obj) {
        synchronized (this.mLock) {
            Object -l_8_R = new a(this, Integer.MAX_VALUE, runnable, str, j, z, obj);
            this.sw.add(-l_8_R);
            this.sz.execute(-l_8_R);
            if (this.sz.getActiveCount() >= this.sy && this.sy < cc()) {
                this.sy++;
                this.sz.setCorePoolSize(this.sy);
                this.sz.setMaximumPoolSize(this.sy);
                mb.n("ThreadPool", "expand urgent core pool size: " + this.sy);
            } else {
                ce();
            }
            Object -l_9_R = st.iterator();
            while (-l_9_R.hasNext()) {
                ((tmsdkobf.pd.a) -l_9_R.next()).a(-l_8_R.cm(), this.sz.getActiveCount());
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean b(Runnable runnable) {
        if (runnable == null) {
            return false;
        }
        synchronized (this.mLock) {
            Object -l_3_R = this.sw.iterator();
            if (-l_3_R != null) {
                Runnable -l_4_R = null;
                while (-l_3_R.hasNext()) {
                    a -l_5_R = (a) -l_3_R.next();
                    if (-l_5_R != null && -l_5_R.cm() != null && runnable.equals(-l_5_R.cm().Jp)) {
                        -l_3_R.remove();
                        Object -l_4_R2 = -l_5_R;
                        break;
                    }
                }
                if (-l_4_R != null) {
                    this.sz.remove(-l_4_R);
                    return true;
                }
            }
        }
    }

    public void beforeExecute(Thread thread, Runnable runnable) {
        synchronized (this.mLock) {
            Object -l_4_R = this.sw.iterator();
            if (-l_4_R != null) {
                a -l_5_R = (a) runnable;
                int -l_6_I = -l_5_R.cm().priority;
                if (-l_6_I < 1) {
                    -l_6_I = 1;
                } else if (-l_6_I > 10) {
                    -l_6_I = 10;
                }
                thread.setPriority(-l_6_I);
                int -l_7_I = 0;
                while (-l_4_R.hasNext()) {
                    a -l_8_R = (a) -l_4_R.next();
                    if (-l_8_R != null && -l_8_R.equals(-l_5_R)) {
                        -l_4_R.remove();
                        -l_7_I = 1;
                        break;
                    }
                }
                if (-l_7_I != 0) {
                    Object -l_8_R2;
                    if (!this.isActive) {
                        -l_8_R2 = this.ss.iterator();
                        while (-l_8_R2.hasNext()) {
                            ((tmsdkobf.pd.b) -l_8_R2.next()).hF();
                        }
                    }
                    -l_8_R2 = st.iterator();
                    while (-l_8_R2.hasNext()) {
                        ((tmsdkobf.pd.a) -l_8_R2.next()).a(-l_5_R.cm());
                    }
                    -l_5_R.cm().Jm = System.currentTimeMillis();
                    -l_5_R.cm().Jn = Debug.threadCpuTimeNanos();
                    this.sx.put(-l_5_R, thread);
                    thread.setName(-l_5_R.cm().name);
                    this.isActive = true;
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Thread c(Runnable runnable) {
        if (runnable == null) {
            return null;
        }
        synchronized (this.mLock) {
            Object -l_3_R = this.sx.keySet().iterator();
            if (-l_3_R != null) {
                Object -l_4_R = null;
                while (-l_3_R.hasNext()) {
                    a -l_5_R = (a) -l_3_R.next();
                    if (-l_5_R != null && -l_5_R.cm() != null && runnable.equals(-l_5_R.cm().Jp)) {
                        a -l_4_R2 = -l_5_R;
                        break;
                    }
                }
                if (-l_4_R != null) {
                    Thread thread = (Thread) this.sx.get(-l_4_R);
                    return thread;
                }
            }
        }
    }

    public void cf() {
        synchronized (this.mLock) {
            this.sE = false;
            sD = 0;
            sC = 0;
            mb.n("ThreadPool", "wake up threa pool");
        }
    }

    public int getSingletonType() {
        return 1;
    }

    public void onCreate(Context context) {
        this.sy = cb();
        this.sz = new iw(0, this.sy + 2, 3, TimeUnit.SECONDS, this.su, new CallerRunsPolicy());
        this.sz.a(this);
        this.sA = new HandlerThread("TMS_THREAD_POOL_HANDLER");
        this.sA.start();
        this.sB = new b(this, this.sA.getLooper());
    }
}
