package com.huawei.android.pushagent.utils.c;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.a.a;
import com.huawei.android.pushagent.utils.a.b;
import java.security.Key;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class e {
    e() {
    }

    public static String bp(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return br(str, bv());
    }

    public static String br(String str, byte[] bArr) {
        if (TextUtils.isEmpty(str) || bArr == null || bArr.length <= 0) {
            return null;
        }
        try {
            Key secretKeySpec = new SecretKeySpec(bArr, "AES");
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] bArr2 = new byte[16];
            new SecureRandom().nextBytes(bArr2);
            instance.init(1, secretKeySpec, new IvParameterSpec(bArr2));
            return by(a.u(bArr2), a.u(instance.doFinal(str.getBytes("UTF-8"))));
        } catch (Throwable e) {
            b.aa("PushLog2976", "aes cbc encrypter data error", e);
            return null;
        }
    }

    public static String bq(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return bs(str, bv());
    }

    public static String bs(String str, byte[] bArr) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        if (bArr == null || bArr.length <= 0) {
            return "";
        }
        try {
            Key secretKeySpec = new SecretKeySpec(bArr, "AES");
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
            Object bx = bx(str);
            Object bw = bw(str);
            if (TextUtils.isEmpty(bx) || TextUtils.isEmpty(bw)) {
                b.z("PushLog2976", "ivParameter or encrypedWord is null");
                return "";
            }
            instance.init(2, secretKeySpec, new IvParameterSpec(a.w(bx)));
            return new String(instance.doFinal(a.w(bw)), "UTF-8");
        } catch (Throwable e) {
            b.aa("PushLog2976", "aes cbc decrypter data error", e);
            return "";
        }
    }

    private static byte[] bv() {
        byte[] w = a.w(com.huawei.android.pushagent.constant.b.xh());
        byte[] w2 = a.w(a.ay());
        return bu(bt(bt(w, w2), a.w("2A57086C86EF54970C1E6EB37BFC72B1")));
    }

    private static byte[] bt(byte[] bArr, byte[] bArr2) {
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

    private static byte[] bu(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = (byte) (bArr[i] >> 2);
        }
        return bArr;
    }

    private static String by(String str, String str2) {
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
            b.y("PushLog2976", e.toString());
            return "";
        }
    }

    private static String bx(String str) {
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
            b.y("PushLog2976", e.toString());
            return "";
        }
    }

    private static String bw(String str) {
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
            b.y("PushLog2976", e.toString());
            return "";
        }
    }
}
