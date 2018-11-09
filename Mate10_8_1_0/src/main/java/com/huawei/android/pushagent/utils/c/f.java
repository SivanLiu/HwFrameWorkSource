package com.huawei.android.pushagent.utils.c;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.a;
import com.huawei.android.pushagent.utils.a.b;
import java.security.MessageDigest;

public class f {
    public static String bz(String str) {
        String str2 = "";
        if (TextUtils.isEmpty(str)) {
            return str2;
        }
        try {
            return a.u(MessageDigest.getInstance("MD5").digest(ca(str)));
        } catch (Throwable e) {
            b.aa("PushLog2976", "NoSuchAlgorithmException / " + e.toString(), e);
            return str2;
        } catch (Throwable e2) {
            b.aa("PushLog2976", "getMD5str failed:" + e2.toString(), e2);
            return str2;
        }
    }

    private static byte[] ca(String str) {
        if (TextUtils.isEmpty(str)) {
            b.y("PushLog2976", "getUTF8Bytes, str is empty");
            return new byte[0];
        }
        try {
            return str.getBytes("UTF-8");
        } catch (Throwable e) {
            b.aa("PushLog2976", "getBytes error:" + str, e);
            return new byte[0];
        }
    }
}
