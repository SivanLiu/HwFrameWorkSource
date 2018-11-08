package tmsdkobf;

import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;

public class gr {
    private static int W(int i) {
        int -l_1_I = i + 1;
        return -l_1_I >= 0 ? -l_1_I : 0;
    }

    public static ar a(int i, int i2, ArrayList<JceStruct> arrayList) {
        Object -l_4_R;
        Object -l_3_R = new ar();
        -l_3_R.bK = i;
        -l_3_R.bO = i2;
        if (arrayList != null) {
            -l_3_R.bN = new ArrayList();
            -l_4_R = arrayList.iterator();
            while (-l_4_R.hasNext()) {
                JceStruct -l_5_R = (JceStruct) -l_4_R.next();
                if (-l_5_R != null) {
                    -l_3_R.bN.add(-l_5_R.toByteArray("UTF-8"));
                }
            }
        } else {
            -l_3_R.bN = null;
        }
        if (i2 != 0) {
            -l_4_R = gq.aZ().R(i);
            if (-l_4_R != null) {
                -l_3_R.bL = -l_4_R.bM;
                -l_3_R.bM = W(-l_3_R.bL);
                return -l_3_R;
            }
        }
        -l_3_R.bL = 0;
        -l_3_R.bM = W(-l_3_R.bL);
        return -l_3_R;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean a(ar arVar, ar arVar2) {
        if (arVar == null && arVar2 == null) {
            return true;
        }
        if (arVar == null || arVar2 == null || arVar2.bO != arVar.bO || arVar2.bK != arVar.bK) {
            return false;
        }
        if (arVar2.bN == null && arVar.bN == null) {
            return true;
        }
        if (arVar2.bN != arVar.bN) {
            if (arVar2.bN == null || arVar.bN == null) {
                return false;
            }
        }
        if (arVar2.bN.size() != arVar.bN.size()) {
            return false;
        }
        int -l_2_I = arVar2.bN.size();
        for (int -l_3_I = 0; -l_3_I < -l_2_I; -l_3_I++) {
            byte[] -l_4_R = (byte[]) arVar2.bN.get(-l_3_I);
            byte[] -l_5_R = (byte[]) arVar.bN.get(-l_3_I);
            if (-l_4_R == null) {
                if (-l_5_R == null) {
                    continue;
                }
            }
            if (-l_4_R != -l_5_R) {
                if (-l_4_R == null || -l_5_R == null) {
                    return false;
                }
            }
            if (-l_4_R.length != -l_5_R.length) {
                return false;
            }
            int -l_6_I = -l_4_R.length;
            for (int -l_7_I = 0; -l_7_I < -l_6_I; -l_7_I++) {
                if (-l_4_R[-l_7_I] != -l_5_R[-l_7_I]) {
                    return false;
                }
            }
            continue;
        }
        return true;
    }

    public static byte[] a(int i, int i2, JceStruct jceStruct) {
        Object -l_3_R = b(i, i2, jceStruct);
        return -l_3_R != null ? a(-l_3_R) : null;
    }

    public static final byte[] a(JceStruct jceStruct) {
        return jceStruct != null ? nh.c(null, jceStruct) : null;
    }

    public static ar b(int i, int i2, JceStruct jceStruct) {
        ArrayList -l_3_R = new ArrayList();
        -l_3_R.add(jceStruct);
        return a(i, i2, -l_3_R);
    }

    public static final ar f(byte[] bArr) {
        return bArr != null ? (ar) nh.a(null, bArr, new ar()) : null;
    }

    public static final void f(String str, String str2) {
        mb.d(str, str2);
    }
}
