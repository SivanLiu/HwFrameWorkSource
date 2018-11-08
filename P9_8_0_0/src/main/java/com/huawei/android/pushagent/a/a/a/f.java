package com.huawei.android.pushagent.a.a.a;

import com.huawei.android.pushagent.a.a.c;
import java.security.MessageDigest;

public class f {
    public static String a(String str) {
        try {
            Object -l_3_R = MessageDigest.getInstance("SHA-256");
            -l_3_R.update(str.getBytes("UTF-8"));
            Object -l_4_R = -l_3_R.digest();
            Object -l_5_R = new StringBuffer(40);
            for (byte b : -l_4_R) {
                int -l_7_I = b & 255;
                if (-l_7_I < 16) {
                    -l_5_R.append('0');
                }
                -l_5_R.append(Integer.toHexString(-l_7_I));
            }
            c.a("PushLogSC2907", "getSHA256str:" + -l_5_R.toString());
            return -l_5_R.toString();
        } catch (Object -l_1_R) {
            c.d("PushLogSC2907", -l_1_R.toString(), -l_1_R);
            return str;
        } catch (Object -l_1_R2) {
            c.d("PushLogSC2907", -l_1_R2.toString(), -l_1_R2);
            return str;
        } catch (Object -l_1_R22) {
            c.d("PushLogSC2907", -l_1_R22.toString(), -l_1_R22);
            return str;
        }
    }
}
