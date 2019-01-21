package com.huawei.android.pushagent.utils.e;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.b.c;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class f {
    public static String wv(String str) {
        String str2 = "";
        if (TextUtils.isEmpty(str)) {
            return str2;
        }
        try {
            return c.tr(MessageDigest.getInstance("MD5").digest(ww(str)));
        } catch (NoSuchAlgorithmException e) {
            a.sw("PushLog3414", "NoSuchAlgorithmException / " + e.toString(), e);
            return str2;
        } catch (Exception e2) {
            a.sw("PushLog3414", "getMD5str failed:" + e2.toString(), e2);
            return str2;
        }
    }

    private static byte[] ww(String str) {
        if (TextUtils.isEmpty(str)) {
            a.su("PushLog3414", "getUTF8Bytes, str is empty");
            return new byte[0];
        }
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            a.sw("PushLog3414", "getBytes error:" + str, e);
            return new byte[0];
        }
    }
}
