package com.huawei.android.pushagent.a.a.a;

import android.text.TextUtils;
import com.huawei.android.pushagent.a.a.a;
import java.security.MessageDigest;

public class c {
    public static String a(String str) {
        Object -l_1_R = "";
        if (!TextUtils.isEmpty(str)) {
            try {
                -l_1_R = a.a(MessageDigest.getInstance("MD5").digest(b(str)));
            } catch (Object -l_2_R) {
                com.huawei.android.pushagent.a.a.c.d("PushLogSC2907", "NoSuchAlgorithmException / " + -l_2_R.toString(), -l_2_R);
            } catch (Object -l_2_R2) {
                com.huawei.android.pushagent.a.a.c.d("PushLogSC2907", "getMD5str failed:" + -l_2_R2.toString(), -l_2_R2);
            }
        }
        return -l_1_R;
    }

    private static byte[] b(String str) {
        if (TextUtils.isEmpty(str)) {
            com.huawei.android.pushagent.a.a.c.d("PushLogSC2907", "getUTF8Bytes, str is empty");
            return new byte[0];
        }
        try {
            return str.getBytes("UTF-8");
        } catch (Object -l_1_R) {
            com.huawei.android.pushagent.a.a.c.d("PushLogSC2907", "getBytes error:" + str, -l_1_R);
            return new byte[0];
        }
    }
}
