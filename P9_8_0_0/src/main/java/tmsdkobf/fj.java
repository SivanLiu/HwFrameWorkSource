package tmsdkobf;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import tmsdk.common.CallerIdent;

public class fj {
    private static ReentrantReadWriteLock ms = new ReentrantReadWriteLock();
    private static HashMap<String, Object> mt = new HashMap();

    public static Object D(int i) {
        return a(i, CallerIdent.getIdent(1, 4294967296L));
    }

    private static Object a(int i, long j) {
        Object -l_3_R = "" + i + "-" + j;
        ms.readLock().lock();
        Object -l_4_R = mt.get(-l_3_R);
        ms.readLock().unlock();
        return -l_4_R != null ? -l_4_R : b(i, j);
    }

    private static Object b(int i, long j) {
        Object -l_4_R = null;
        Object -l_3_R = null;
        switch (i) {
            case 4:
                -l_4_R = "" + i + "-" + j;
                ms.readLock().lock();
                -l_3_R = mt.get(-l_4_R);
                ms.readLock().unlock();
                if (-l_3_R == null) {
                    -l_3_R = new ki(j, "com.tencent.meri");
                    mb.n("ServiceCenter", "create service: " + -l_4_R);
                    break;
                }
                break;
            case 5:
                -l_4_R = "" + i + "-" + j;
                ms.readLock().lock();
                -l_3_R = mt.get(-l_4_R);
                ms.readLock().unlock();
                if (-l_3_R == null) {
                    -l_3_R = new gw(j);
                    mb.n("ServiceCenter", "create service: " + -l_4_R);
                    break;
                }
                break;
            case 9:
                -l_4_R = "" + i + "-" + j;
                ms.readLock().lock();
                -l_3_R = mt.get(-l_4_R);
                ms.readLock().unlock();
                if (-l_3_R == null) {
                    -l_3_R = new hd(j);
                    mb.n("ServiceCenter", "create service: " + -l_4_R);
                    break;
                }
                break;
            case 12:
                -l_4_R = "" + i + "-" + j;
                ms.readLock().lock();
                -l_3_R = mt.get(-l_4_R);
                ms.readLock().unlock();
                if (-l_3_R == null) {
                    -l_3_R = new gl(j);
                    mb.n("ServiceCenter", "create service: " + -l_4_R);
                    break;
                }
                break;
            case 17:
                -l_4_R = "" + i + "-" + j;
                ms.readLock().lock();
                -l_3_R = mt.get(-l_4_R);
                ms.readLock().unlock();
                if (-l_3_R == null) {
                    -l_3_R = new fi(j);
                    mb.n("ServiceCenter", "create service: " + -l_4_R);
                    break;
                }
                break;
        }
        if (!(-l_4_R == null || -l_3_R == null)) {
            ms.writeLock().lock();
            if (mt.get(-l_4_R) == null) {
                mt.put(-l_4_R, -l_3_R);
            }
            ms.writeLock().unlock();
        }
        return -l_3_R;
    }
}
