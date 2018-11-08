package tmsdkobf;

import android.os.Debug;
import android.os.HandlerThread;
import java.util.HashMap;
import tmsdkobf.pd.a;
import tmsdkobf.pd.c;

public class iy {
    private static HashMap<Thread, c> sJ = new HashMap();
    private static a sK;
    private static iz.a sL = new iz.a() {
        public void a(Thread thread, Runnable runnable) {
            Object -l_3_R = new c();
            -l_3_R.Jk = 3;
            -l_3_R.eA = ((pc) thread).bL();
            -l_3_R.name = thread.getName();
            -l_3_R.priority = thread.getPriority();
            -l_3_R.Jm = -1;
            -l_3_R.Jn = -1;
            iy.sJ.put(thread, -l_3_R);
            iy.cn();
            iy.sK.a(-l_3_R, iy.activeCount());
        }

        public void b(Thread thread, Runnable runnable) {
            c -l_3_R = (c) iy.sJ.remove(thread);
            if (-l_3_R != null) {
                -l_3_R.Jm = System.currentTimeMillis() - -l_3_R.Jm;
                -l_3_R.Jn = Debug.threadCpuTimeNanos() - -l_3_R.Jn;
                iy.cn();
                iy.sK.b(-l_3_R);
            }
        }

        public void beforeExecute(Thread thread, Runnable runnable) {
            c -l_3_R = (c) iy.sJ.get(thread);
            if (-l_3_R != null) {
                iy.cn();
                iy.sK.a(-l_3_R);
                -l_3_R.Jm = System.currentTimeMillis();
                -l_3_R.Jn = Debug.threadCpuTimeNanos();
            }
        }
    };

    public static HandlerThread a(String str, int i, long j) {
        return new pc(str, i, j);
    }

    public static int activeCount() {
        return sJ.size();
    }

    private static void cn() {
        if (sK == null) {
            sK = ix.ci();
        }
    }

    public static iz.a co() {
        return sL;
    }
}
