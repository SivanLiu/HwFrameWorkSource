package tmsdkobf;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public final class hp {
    public String mPkg;
    public int mState;
    public int qh;
    public boolean qi;

    static String a(List<hp> list) {
        Object -l_1_R = new StringBuilder();
        for (hp -l_3_R : list) {
            -l_1_R.append(a(-l_3_R) + "|");
        }
        return -l_1_R.toString();
    }

    static String a(hp hpVar) {
        Object -l_1_R = new StringBuilder();
        -l_1_R.append(hpVar.mPkg + ",");
        -l_1_R.append(hpVar.qh + ",");
        -l_1_R.append(hpVar.mState + ",");
        -l_1_R.append(hpVar.qi);
        return -l_1_R.toString();
    }

    static hp aC(String str) {
        Object -l_2_R = null;
        if (str != null) {
            -l_2_R = str.trim().split(",");
        }
        if (str == null || -l_2_R.length < 4) {
            return null;
        }
        Object -l_1_R = new hp();
        -l_1_R.mPkg = -l_2_R[0];
        -l_1_R.qh = Integer.parseInt(-l_2_R[1]);
        -l_1_R.mState = Integer.parseInt(-l_2_R[2]);
        -l_1_R.qi = Boolean.parseBoolean(-l_2_R[3]);
        return -l_1_R;
    }

    static ArrayList<hp> aD(String str) {
        Object -l_1_R = new ArrayList();
        if (str != null) {
            Object -l_2_R = str.trim().split("\\|");
            Object -l_3_R = -l_2_R;
            int -l_4_I = -l_2_R.length;
            for (int -l_5_I = 0; -l_5_I < -l_4_I; -l_5_I++) {
                Object -l_6_R = -l_3_R[-l_5_I];
                if (!TextUtils.isEmpty(-l_6_R)) {
                    -l_1_R.add(aC(-l_6_R));
                }
            }
        }
        return -l_1_R;
    }
}
