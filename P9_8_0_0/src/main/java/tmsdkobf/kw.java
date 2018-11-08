package tmsdkobf;

import java.util.ArrayList;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.creator.ManagerCreatorC;

public class kw {
    private static et b(ov ovVar) {
        int i = 0;
        Object -l_1_R = new et();
        -l_1_R.kH = bu(ovVar.getPackageName());
        -l_1_R.name = bu(ovVar.getAppName());
        -l_1_R.version = bu(ovVar.getVersion());
        -l_1_R.kK = ovVar.getVersionCode();
        -l_1_R.kJ = ovVar.hz();
        -l_1_R.kG = "" + ovVar.getUid();
        if (ovVar.hx()) {
            i = 1;
        }
        -l_1_R.ib = i;
        -l_1_R.kR = (int) ovVar.getSize();
        return -l_1_R;
    }

    private static String bu(String str) {
        return str != null ? str : "";
    }

    public static synchronized void dP() {
        synchronized (kw.class) {
            try {
                if (dQ()) {
                    Object -l_0_R = new em();
                    -l_0_R.kr = new ArrayList();
                    Object -l_3_R = TMServiceFactory.getSystemInfoService().f(25, 2).iterator();
                    while (-l_3_R.hasNext()) {
                        ov -l_4_R = (ov) -l_3_R.next();
                        if (-l_4_R != null) {
                            Object -l_5_R = new eu();
                            -l_5_R.kT = b(-l_4_R);
                            -l_0_R.kr.add(-l_5_R);
                        }
                    }
                    if (((ot) ManagerCreatorC.getManager(ot.class)).a(-l_0_R) == 0) {
                        gf.S().b(System.currentTimeMillis());
                    }
                } else {
                    return;
                }
            } catch (Throwable th) {
            }
        }
    }

    static boolean dQ() {
        boolean z = true;
        int -l_0_I = 0;
        if (!gf.S().Z().booleanValue()) {
            return false;
        }
        long -l_1_J = System.currentTimeMillis();
        long -l_3_J = gf.S().X();
        if (-l_3_J == 0) {
            gf.S().b(System.currentTimeMillis());
            return false;
        }
        if (!(-l_1_J <= -l_3_J)) {
            long -l_5_J = gf.S().ah();
            if (!(-l_5_J >= 0)) {
                -l_5_J = 604800000;
            }
            if (-l_1_J - -l_3_J >= -l_5_J) {
                z = false;
            }
            if (!z) {
                -l_0_I = 1;
            }
        }
        return -l_0_I;
    }
}
