package com.huawei.android.pushagent.utils.a;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.f.b;
import com.huawei.android.pushagent.utils.f.c;
import java.security.Key;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class g {
    g() {
    }

    public static String w(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return y(str, ac());
    }

    public static String y(String str, byte[] bArr) {
        if (TextUtils.isEmpty(str) || bArr == null || bArr.length <= 0) {
            return null;
        }
        try {
            Key secretKeySpec = new SecretKeySpec(bArr, "AES");
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] bArr2 = new byte[16];
            new SecureRandom().nextBytes(bArr2);
            instance.init(1, secretKeySpec, new IvParameterSpec(bArr2));
            return af(b.el(bArr2), b.el(instance.doFinal(str.getBytes("UTF-8"))));
        } catch (Throwable e) {
            c.es("PushLog3413", "aes cbc encrypter data error", e);
            return null;
        }
    }

    public static String x(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return z(str, ac());
    }

    public static String z(String str, byte[] bArr) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        if (bArr == null || bArr.length <= 0) {
            return "";
        }
        try {
            Key secretKeySpec = new SecretKeySpec(bArr, "AES");
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
            Object ae = ae(str);
            Object ad = ad(str);
            if (TextUtils.isEmpty(ae) || TextUtils.isEmpty(ad)) {
                c.ep("PushLog3413", "ivParameter or encrypedWord is null");
                return "";
            }
            instance.init(2, secretKeySpec, new IvParameterSpec(b.en(ae)));
            return new String(instance.doFinal(b.en(ad)), "UTF-8");
        } catch (Throwable e) {
            c.es("PushLog3413", "aes cbc decrypter data error", e);
            return "";
        }
    }

    private static byte[] ac() {
        byte[] en = b.en(com.huawei.android.pushagent.constant.b.abo());
        byte[] en2 = b.en(d.q());
        return ab(aa(aa(en, en2), b.en("2A57086C86EF54970C1E6EB37BFC72B1")));
    }

    private static byte[] aa(byte[] bArr, byte[] bArr2) {
        if (bArr == null || bArr2 == null) {
            return null;
        }
        int length = bArr.length;
        if (length != bArr2.length) {
            return null;
        }
        byte[] bArr3 = new byte[length];
        for (int i = 0; i < length; i++) {
            bArr3[i] = (byte) (bArr[i] ^ bArr2[i]);
        }
        return bArr3;
    }

    private static byte[] ab(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = (byte) (bArr[i] >> 2);
        }
        return bArr;
    }

    private static String af(String str, String str2) {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            return "";
        }
        try {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(str2.substring(0, 6));
            stringBuffer.append(str.substring(0, 6));
            stringBuffer.append(str2.substring(6, 10));
            stringBuffer.append(str.substring(6, 16));
            stringBuffer.append(str2.substring(10, 16));
            stringBuffer.append(str.substring(16));
            stringBuffer.append(str2.substring(16));
            return stringBuffer.toString();
        } catch (Exception e) {
            c.eq("PushLog3413", e.toString());
            return "";
        }
    }

    private static String ae(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        try {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(str.substring(6, 12));
            stringBuffer.append(str.substring(16, 26));
            stringBuffer.append(str.substring(32, 48));
            return stringBuffer.toString();
        } catch (Exception e) {
            c.eq("PushLog3413", e.toString());
            return "";
        }
    }

    private static String ad(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        try {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(str.substring(0, 6));
            stringBuffer.append(str.substring(12, 16));
            stringBuffer.append(str.substring(26, 32));
            stringBuffer.append(str.substring(48));
            return stringBuffer.toString();
        } catch (Exception e) {
            c.eq("PushLog3413", e.toString());
            return "";
        }
    }
}
