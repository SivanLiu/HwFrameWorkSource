package com.huawei.android.pushagent.utils.a;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.f.b;
import com.huawei.android.pushagent.utils.f.c;
import java.security.MessageDigest;

public class e {
    public static String r(String str) {
        String str2 = "";
        if (TextUtils.isEmpty(str)) {
            return str2;
        }
        try {
            return b.el(MessageDigest.getInstance("MD5").digest(s(str)));
        } catch (Throwable e) {
            c.es("PushLog3413", "NoSuchAlgorithmException / " + e.toString(), e);
            return str2;
        } catch (Throwable e2) {
            c.es("PushLog3413", "getMD5str failed:" + e2.toString(), e2);
            return str2;
        }
    }

    private static byte[] s(String str) {
        if (TextUtils.isEmpty(str)) {
            c.eq("PushLog3413", "getUTF8Bytes, str is empty");
            return new byte[0];
        }
        try {
            return str.getBytes("UTF-8");
        } catch (Throwable e) {
            c.es("PushLog3413", "getBytes error:" + str, e);
            return new byte[0];
        }
    }
}
