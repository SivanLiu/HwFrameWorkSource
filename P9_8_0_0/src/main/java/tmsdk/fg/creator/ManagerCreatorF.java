package tmsdk.fg.creator;

import android.content.Context;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import tmsdk.common.TMSDKContext;
import tmsdkobf.ic;

public final class ManagerCreatorF {
    private static volatile ManagerCreatorF Mi = null;
    private Context mContext;
    private HashMap<Class<? extends ic>, ic> tN = new HashMap();
    private HashMap<Class<? extends ic>, WeakReference<? extends ic>> tO = new HashMap();

    private ManagerCreatorF(Context context) {
        this.mContext = context.getApplicationContext();
    }

    private <T extends BaseManagerF> T d(Class<T> -l_3_R) {
        if (-l_3_R != null) {
            Object -l_2_R;
            synchronized (-l_3_R) {
                BaseManagerF -l_2_R2;
                -l_2_R = (BaseManagerF) -l_3_R.cast(this.tN.get(-l_3_R));
                if (-l_2_R == null) {
                    WeakReference -l_4_R = (WeakReference) this.tO.get(-l_3_R);
                    if (-l_4_R != null) {
                        -l_2_R2 = (BaseManagerF) -l_3_R.cast(-l_4_R.get());
                    }
                }
                if (-l_2_R == null) {
                    try {
                        -l_2_R2 = (BaseManagerF) -l_3_R.newInstance();
                        -l_2_R2.onCreate(this.mContext);
                        if (-l_2_R2.getSingletonType() == 1) {
                            this.tN.put(-l_3_R, -l_2_R2);
                        } else if (-l_2_R2.getSingletonType() == 0) {
                            this.tO.put(-l_3_R, new WeakReference(-l_2_R2));
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

    public static <T extends BaseManagerF> T getManager(Class<T> cls) {
        return ji().d(cls);
    }

    static ManagerCreatorF ji() {
        if (Mi == null) {
            Object -l_0_R = ManagerCreatorF.class;
            synchronized (ManagerCreatorF.class) {
                if (Mi == null) {
                    Mi = new ManagerCreatorF(TMSDKContext.getApplicaionContext());
                }
            }
        }
        return Mi;
    }
}
