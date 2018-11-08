package tmsdkobf;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public class rg {
    public static float df(String str) {
        if (TextUtils.isEmpty(str)) {
            return 0.0f;
        }
        float -l_1_F;
        try {
            -l_1_F = Float.parseFloat(str);
        } catch (Exception e) {
            -l_1_F = 0.0f;
        }
        return -l_1_F;
    }

    public static int dg(String str) {
        int -l_1_I = 0;
        if (TextUtils.isEmpty(str)) {
            return -l_1_I;
        }
        try {
            -l_1_I = Integer.valueOf(str).intValue();
        } catch (NumberFormatException e) {
        }
        return -l_1_I < 0 ? 0 : -l_1_I;
    }

    public static List<Integer> dh(String str) {
        if (str == null) {
            return null;
        }
        List<Integer> -l_1_R = new ArrayList();
        Object -l_2_R = str.split("\\|");
        Object -l_3_R = -l_2_R;
        int -l_4_I = -l_2_R.length;
        for (int -l_5_I = 0; -l_5_I < -l_4_I; -l_5_I++) {
            try {
                -l_1_R.add(Integer.valueOf(-l_3_R[-l_5_I]));
            } catch (Exception e) {
                -l_1_R = null;
            }
        }
        return -l_1_R;
    }
}
