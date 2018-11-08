package tmsdkobf;

import android.os.IBinder;
import android.os.RemoteException;
import java.util.concurrent.ConcurrentLinkedQueue;
import tmsdk.common.DataEntity;
import tmsdkobf.ih.b;

public final class ik extends b {
    private static ConcurrentLinkedQueue<ii> rA = new ConcurrentLinkedQueue();
    private static volatile ik rB = null;

    private ik() {
    }

    public static boolean a(ii iiVar) {
        return rA.add(iiVar);
    }

    public static ik bF() {
        if (rB == null) {
            Object -l_0_R = ik.class;
            synchronized (ik.class) {
                if (rB == null) {
                    rB = new ik();
                }
            }
        }
        return rB;
    }

    public IBinder asBinder() {
        return this;
    }

    public DataEntity sendMessage(DataEntity dataEntity) throws RemoteException {
        int -l_2_I = dataEntity.what();
        Object -l_4_R = rA.iterator();
        while (-l_4_R.hasNext()) {
            ii -l_5_R = (ii) -l_4_R.next();
            if (-l_5_R.isMatch(-l_2_I)) {
                return -l_5_R.onProcessing(dataEntity);
            }
        }
        return null;
    }
}
