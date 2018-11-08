package tmsdk.bg.creator;

import android.content.Context;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import tmsdk.common.TMSDKContext;
import tmsdkobf.ic;

public final class ManagerCreatorB {
    private static volatile ManagerCreatorB tM = null;
    private Context mContext;
    private HashMap<Class<? extends ic>, ic> tN = new HashMap();
    private HashMap<Class<? extends ic>, WeakReference<? extends ic>> tO = new HashMap();

    private ManagerCreatorB(Context context) {
        this.mContext = context.getApplicationContext();
    }

    private <T extends BaseManagerB> T a(Class<T> -l_3_R) {
        Object -l_4_R;
        if (-l_3_R != null) {
            Object -l_2_R;
            synchronized (-l_3_R) {
                BaseManagerB -l_2_R2;
                -l_2_R = (BaseManagerB) -l_3_R.cast(this.tN.get(-l_3_R));
                if (-l_2_R == null) {
                    WeakReference -l_4_R2 = (WeakReference) this.tO.get(-l_3_R);
                    if (-l_4_R2 != null) {
                        -l_2_R2 = (BaseManagerB) -l_3_R.cast(-l_4_R2.get());
                    }
                }
                if (-l_2_R == null) {
                    try {
                        -l_2_R2 = (BaseManagerB) -l_3_R.newInstance();
                        -l_2_R2.onCreate(this.mContext);
                        if (-l_2_R2.getSingletonType() == 1) {
                            -l_4_R = ManagerCreatorB.class;
                            synchronized (ManagerCreatorB.class) {
                                this.tN.put(-l_3_R, -l_2_R2);
                            }
                        } else if (-l_2_R2.getSingletonType() == 0) {
                            this.tO.put(-l_3_R, new WeakReference(-l_2_R2));
                        }
                    } catch (Object -l_4_R3) {
                        throw new RuntimeException(-l_4_R3);
                    }
                }
            }
            return -l_2_R;
        }
        throw new NullPointerException("the param of getManager can't be null.");
    }

    private void b(Class<? extends ic> cls) {
        Object -l_2_R = ManagerCreatorB.class;
        synchronized (ManagerCreatorB.class) {
            this.tN.remove(cls);
        }
    }

    static ManagerCreatorB cO() {
        if (tM == null) {
            Object -l_0_R = ManagerCreatorB.class;
            synchronized (ManagerCreatorB.class) {
                if (tM == null) {
                    tM = new ManagerCreatorB(TMSDKContext.getApplicaionContext());
                }
            }
        }
        return tM;
    }

    public static void destroyManager(BaseManagerB baseManagerB) {
        if (baseManagerB != null) {
            cO().b(baseManagerB.getClass());
        }
    }

    public static <T extends BaseManagerB> T getManager(Class<T> cls) {
        return cO().a(cls);
    }
}
