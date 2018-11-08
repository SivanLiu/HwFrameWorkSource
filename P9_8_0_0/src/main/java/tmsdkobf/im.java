package tmsdkobf;

import com.qq.taf.jce.JceStruct;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import tmsdk.common.IDualPhoneInfoFetcher;
import tmsdk.common.TMSDKContext;
import tmsdk.common.tcc.TccCryptor;
import tmsdk.common.utils.f;

public class im {
    private static List<WeakReference<a>> rC = new LinkedList();
    private static in rD;
    public static volatile qc rE = null;
    private static volatile boolean rF = false;
    private static boolean rG = true;
    public static IDualPhoneInfoFetcher rH = null;

    public interface a {
    }

    public static synchronized void a(a aVar) {
        synchronized (im.class) {
            c(rC);
            rC.add(new WeakReference(aVar));
        }
    }

    public static boolean bG() {
        return rG || gf.S().ac().booleanValue();
    }

    public static boolean bH() {
        bI();
        return rF;
    }

    public static void bI() {
        if (!rF) {
            rF = ma.f(TMSDKContext.getApplicaionContext(), "Tmsdk-2.0.10-mfr");
            if (rF) {
                try {
                    TMSDKContext.registerNatives(0, TccCryptor.class);
                } catch (Object -l_1_R) {
                    -l_1_R.printStackTrace();
                    rF = false;
                }
            }
            f.h("demo", "mIsSdkLibraryLoaded =" + rF);
        }
    }

    public static in bJ() {
        if (rD == null) {
            Object -l_0_R = im.class;
            synchronized (im.class) {
                if (rD == null) {
                    rD = new in(bL(), "com.tmsdk.common");
                }
            }
        }
        return rD;
    }

    public static ob bK() {
        return (ob) fj.D(5);
    }

    private static long bL() {
        int -l_0_I = 1 != bN() ? 2 != bN() ? 3 : 2 : 1;
        return ig.getIdent(-l_0_I, 4294967296L);
    }

    public static qc bM() {
        return rE;
    }

    public static int bN() {
        return 0;
    }

    public static IDualPhoneInfoFetcher bO() {
        return rH;
    }

    public static void bP() {
        f.h("ImsiChecker", "[API]onImsiChanged");
        if (new ge().R()) {
            jn.cx().onImsiChanged();
            bK().gm();
        }
    }

    public static String bQ() {
        return ir.bU().bQ();
    }

    private static <T> void c(List<WeakReference<T>> list) {
        Object -l_1_R = list.iterator();
        while (-l_1_R.hasNext()) {
            if (((WeakReference) -l_1_R.next()).get() == null) {
                -l_1_R.remove();
            }
        }
    }

    public static void requestDelUserData() {
        if ("874556".equals(bQ())) {
            Object -l_1_R = bK();
            JceStruct -l_2_R = new cz();
            Object -l_3_R = new ArrayList();
            -l_3_R.add(new Integer(1));
            -l_2_R.gh = -l_3_R;
            -l_1_R.a(4048, -l_2_R, new da(), 0, new jy() {
                public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                    if (i3 != 0) {
                        f.h("TMSDKContextInternal", "cmdId return :" + i2 + " code : " + i3);
                    } else if (jceStruct != null) {
                        String str = "TMSDKContextInternal";
                        f.h(str, "cmdId sucessed :" + i2 + " result: " + ((da) jceStruct).result);
                    }
                }
            });
        }
        kt.saveActionData(1320080);
    }

    public static void setAutoConnectionSwitch(boolean z) {
        rG = z;
    }

    public static void setDualPhoneInfoFetcher(IDualPhoneInfoFetcher iDualPhoneInfoFetcher) {
        f.h("TMSDKContextInternal", "setDualPhoneInfoFetcher:[" + iDualPhoneInfoFetcher + "]");
        f.h("TrafficCorrection", "setDualPhoneInfoFetcher:[" + iDualPhoneInfoFetcher + "]");
        rH = iDualPhoneInfoFetcher;
    }
}
