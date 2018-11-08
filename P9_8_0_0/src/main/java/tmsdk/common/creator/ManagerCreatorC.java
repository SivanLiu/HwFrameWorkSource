package tmsdk.common.creator;

import android.content.Context;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import tmsdk.common.TMSDKContext;
import tmsdkobf.ic;

public final class ManagerCreatorC {
    private static volatile ManagerCreatorC yz = null;
    private Context mContext;
    private final Object mLock = new Object();
    private HashMap<Class<? extends ic>, ic> tN = new HashMap();
    private HashMap<Class<? extends ic>, WeakReference<? extends ic>> tO = new HashMap();

    private ManagerCreatorC(Context context) {
        this.mContext = context.getApplicationContext();
    }

    private <T extends BaseManagerC> T c(Class<T> cls) {
        if (cls != null) {
            Object -l_2_R;
            synchronized (this.mLock) {
                BaseManagerC -l_2_R2;
                -l_2_R = (BaseManagerC) cls.cast(this.tN.get(cls));
                if (-l_2_R == null) {
                    WeakReference -l_4_R = (WeakReference) this.tO.get(cls);
                    if (-l_4_R != null) {
                        -l_2_R2 = (BaseManagerC) cls.cast(-l_4_R.get());
                    }
                }
                if (-l_2_R == null) {
                    try {
                        -l_2_R2 = (BaseManagerC) cls.newInstance();
                        -l_2_R2.onCreate(this.mContext);
                        if (-l_2_R2.getSingletonType() == 1) {
                            this.tN.put(cls, -l_2_R2);
                        } else if (-l_2_R2.getSingletonType() == 0) {
                            this.tO.put(cls, new WeakReference(-l_2_R2));
                        }
                    } catch (Object -l_4_R2) {
                        throw new RuntimeException(-l_4_R2);
                    }
                }
            }
            return -l_2_R;
        }
        throw new NullPointerException("the param of getManager can't be null.");
    }

    static ManagerCreatorC eB() {
        if (yz == null) {
            Object -l_0_R = ManagerCreatorC.class;
            synchronized (ManagerCreatorC.class) {
                if (yz == null) {
                    yz = new ManagerCreatorC(TMSDKContext.getApplicaionContext());
                }
            }
        }
        return yz;
    }

    public static <T extends BaseManagerC> T getManager(Class<T> cls) {
        return eB().c(cls);
    }
}
