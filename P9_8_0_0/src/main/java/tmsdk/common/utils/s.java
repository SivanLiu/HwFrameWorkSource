package tmsdk.common.utils;

import tmsdk.common.TMSDKContext;
import tmsdkobf.fj;
import tmsdkobf.fs;
import tmsdkobf.gf;
import tmsdkobf.ki;
import tmsdkobf.kv;

public class s {
    private static long nd = 0;

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static synchronized void bW(int i) {
        Object obj = 1;
        synchronized (s.class) {
            kv.d("WakeupUtil", "1]wakeup-flag:[" + i + "]");
            if (i != -1) {
                if ((gf.S().ak() & i) == 0) {
                }
            }
            final Object -l_1_R = new boolean[]{true};
            if (i != -1) {
                -l_1_R[0] = false;
                long -l_2_J = System.currentTimeMillis();
                if (-l_2_J - nd < 3540000) {
                    obj = null;
                }
                if (obj == null) {
                    return;
                }
                nd = -l_2_J;
            }
            ((ki) fj.D(4)).addTask(new Runnable() {
                public void run() {
                    kv.d("WakeupUtil", "processWakeLogicSync");
                    fs.c(TMSDKContext.getApplicaionContext()).c(-l_1_R[0]);
                }
            }, "checkStart");
        }
    }
}
