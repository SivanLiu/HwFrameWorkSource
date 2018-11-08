package tmsdkobf;

import android.text.TextUtils;
import com.tencent.tcuser.util.a;
import java.util.List;
import tmsdk.common.module.intelli_sms.SmsCheckResult;

public class kz {
    public static ky aJ(int i) {
        ky kyVar;
        Object -l_3_R;
        ky -l_2_R;
        boolean z = false;
        Object -l_1_R = null;
        switch (i) {
            case 33:
                -l_1_R = dZ();
                break;
            case 90:
                -l_1_R = dY();
                break;
            case 139:
                -l_1_R = ek();
                break;
            case 141:
                -l_1_R = dU();
                break;
            case SmsCheckResult.ESCT_146 /*146*/:
                -l_1_R = dW();
                break;
            case 150:
                -l_1_R = ea();
                break;
            case 151:
                -l_1_R = ed();
                break;
            case SmsCheckResult.ESCT_163 /*163*/:
                -l_1_R = eg();
                break;
            default:
                if (!TextUtils.isEmpty(-l_1_R)) {
                    return null;
                }
                kyVar = null;
                try {
                    -l_3_R = -l_1_R.split(";");
                    -l_2_R = new ky();
                    try {
                        -l_2_R.xY = a.av(-l_3_R[0]);
                        if (a.av(-l_3_R[1]) == 1) {
                            z = true;
                        }
                        -l_2_R.xZ = z;
                        if (-l_3_R.length >= 3) {
                            -l_2_R.ya = a.av(-l_3_R[2]);
                        }
                        if (-l_3_R.length >= 4) {
                            -l_2_R.yb = -l_3_R[3];
                        }
                        kyVar = -l_2_R;
                    } catch (Throwable th) {
                        kyVar = -l_2_R;
                        return kyVar;
                    }
                } catch (Throwable th2) {
                    return kyVar;
                }
                return kyVar;
        }
        if (!TextUtils.isEmpty(-l_1_R)) {
            return null;
        }
        kyVar = null;
        -l_3_R = -l_1_R.split(";");
        -l_2_R = new ky();
        -l_2_R.xY = a.av(-l_3_R[0]);
        if (a.av(-l_3_R[1]) == 1) {
            z = true;
        }
        -l_2_R.xZ = z;
        if (-l_3_R.length >= 3) {
            -l_2_R.ya = a.av(-l_3_R[2]);
        }
        if (-l_3_R.length >= 4) {
            -l_2_R.yb = -l_3_R[3];
        }
        kyVar = -l_2_R;
        return kyVar;
    }

    static void bA(String str) {
        new md("b_d_pre").a("f", str, true);
    }

    public static void bB(String str) {
        new md("b_d_pre").a("g", str, true);
    }

    public static void bC(String str) {
        new md("b_d_pre").a("h", str, true);
    }

    public static void bv(String str) {
        new md("b_d_pre").a("aaa", str, true);
    }

    static void bw(String str) {
        new md("b_d_pre").a("b", str, true);
    }

    static void bx(String str) {
        new md("b_d_pre").a("c", str, true);
    }

    static void by(String str) {
        new md("b_d_pre").a("d", str, true);
    }

    static void bz(String str) {
        new md("b_d_pre").a("e", str, true);
    }

    static String dU() {
        return new md("b_d_pre").getString("aaa", "141;1;;;");
    }

    public static long dV() {
        return new md("b_d_pre").getLong("aaar", -1);
    }

    static String dW() {
        return new md("b_d_pre").getString("b", "146;1;;;");
    }

    public static long dX() {
        return new md("b_d_pre").getLong("br", -1);
    }

    static String dY() {
        return new md("b_d_pre").getString("c", "90;0;;;");
    }

    static String dZ() {
        return new md("b_d_pre").getString("d", "33;0;;;");
    }

    static String ea() {
        return new md("b_d_pre").getString("e", "150;0;;;");
    }

    public static long eb() {
        return new md("b_d_pre").getLong("ea", -1);
    }

    public static long ec() {
        return new md("b_d_pre").getLong("eb", -1);
    }

    static String ed() {
        return new md("b_d_pre").getString("f", "151;0;;;");
    }

    public static long ee() {
        return new md("b_d_pre").getLong("fa", -1);
    }

    public static long ef() {
        return new md("b_d_pre").getLong("fb", -1);
    }

    public static String eg() {
        return new md("b_d_pre").getString("g", "163;0;;;");
    }

    public static long eh() {
        return new md("b_d_pre").getLong("ga", -1);
    }

    public static long ei() {
        return new md("b_d_pre").getLong("gb", -1);
    }

    public static long ej() {
        return new md("b_d_pre").getLong("gc", -1);
    }

    static String ek() {
        return new md("b_d_pre").getString("h", "139;0;;;");
    }

    public static void i(List<String> list) {
        if (list != null && !list.isEmpty()) {
            for (String -l_3_R : list) {
                try {
                    Object -l_4_R = -l_3_R.split(";");
                    switch (a.av(-l_4_R[0])) {
                        case 33:
                            by(-l_3_R);
                            if (a.av(-l_4_R[1]) != 0) {
                                break;
                            }
                            la.bF(lg.getPath());
                            break;
                        case 90:
                            bx(-l_3_R);
                            if (a.av(-l_4_R[1]) != 0) {
                                break;
                            }
                            la.bF(lf.getPath());
                            break;
                        case 139:
                            bC(-l_3_R);
                            break;
                        case 141:
                            bv(-l_3_R);
                            break;
                        case SmsCheckResult.ESCT_146 /*146*/:
                            bw(-l_3_R);
                            if (a.av(-l_4_R[1]) != 0) {
                                break;
                            }
                            la.bF(lh.getPath());
                            break;
                        case 150:
                            bz(-l_3_R);
                            if (a.av(-l_4_R[1]) != 0) {
                                break;
                            }
                            la.bF(le.getPath());
                            break;
                        case 151:
                            bA(-l_3_R);
                            if (a.av(-l_4_R[1]) == 1) {
                                ld.et();
                                break;
                            }
                            ld.es();
                            la.bF(ld.getPath());
                            break;
                        case SmsCheckResult.ESCT_163 /*163*/:
                            bB(-l_3_R);
                            if (a.av(-l_4_R[1]) != 0) {
                                break;
                            }
                            la.bF(lc.getPath());
                            break;
                        default:
                            break;
                    }
                } catch (Throwable th) {
                }
            }
        }
    }

    public static void k(long j) {
        new md("b_d_pre").a("aaar", j, true);
    }

    public static void l(long j) {
        new md("b_d_pre").a("br", j, true);
    }

    public static void m(long j) {
        new md("b_d_pre").a("ea", j, true);
    }

    public static void n(long j) {
        new md("b_d_pre").a("eb", j, true);
    }

    public static void o(long j) {
        new md("b_d_pre").a("fa", j, true);
    }

    public static void p(long j) {
        new md("b_d_pre").a("fb", j, true);
    }

    public static void q(long j) {
        new md("b_d_pre").a("ga", j, true);
    }

    public static void r(long j) {
        new md("b_d_pre").a("gb", j, true);
    }

    public static void s(long j) {
        new md("b_d_pre").a("gc", j, true);
    }
}
