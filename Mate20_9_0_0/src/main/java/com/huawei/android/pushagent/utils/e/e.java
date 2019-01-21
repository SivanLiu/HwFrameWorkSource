package com.huawei.android.pushagent.utils.e;

import android.text.TextUtils;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.b.c;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class e {
    e() {
    }

    public static String wl(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return wn(str, wr());
    }

    public static String wn(String str, byte[] bArr) {
        if (TextUtils.isEmpty(str) || bArr == null || bArr.length <= 0) {
            return null;
        }
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(bArr, "AES");
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] bArr2 = new byte[16];
            new SecureRandom().nextBytes(bArr2);
            instance.init(1, secretKeySpec, new IvParameterSpec(bArr2));
            return wu(c.tr(bArr2), c.tr(instance.doFinal(str.getBytes("UTF-8"))));
        } catch (Exception e) {
            a.sw("PushLog3414", "aes cbc encrypter data error", e);
            return null;
        }
    }

    public static String wm(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        return wo(str, wr());
    }

    public static String wo(String str, byte[] bArr) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        if (bArr == null || bArr.length <= 0) {
            return "";
        }
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(bArr, "AES");
            Cipher instance = Cipher.getInstance("AES/CBC/PKCS5Padding");
            String wt = wt(str);
            String ws = ws(str);
            if (TextUtils.isEmpty(wt) || TextUtils.isEmpty(ws)) {
                a.sv("PushLog3414", "ivParameter or encrypedWord is null");
                return "";
            }
            instance.init(2, secretKeySpec, new IvParameterSpec(c.tt(wt)));
            return new String(instance.doFinal(c.tt(ws)), "UTF-8");
        } catch (Exception e) {
            a.sw("PushLog3414", "aes cbc decrypter data error", e);
            return "";
        }
    }

    private static byte[] wr() {
        byte[] tt = c.tt(com.huawei.android.pushagent.constant.a.ba());
        byte[] tt2 = c.tt(d.wk());
        return wq(wp(wp(tt, tt2), c.tt("2A57086C86EF54970C1E6EB37BFC72B1")));
    }

    private static byte[] wp(byte[] bArr, byte[] bArr2) {
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

    private static byte[] wq(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = (byte) (bArr[i] >> 2);
        }
        return bArr;
    }

    private static String wu(String str, String str2) {
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
            a.su("PushLog3414", e.toString());
            return "";
        }
    }

    private static String wt(String str) {
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
            a.su("PushLog3414", e.toString());
            return "";
        }
    }

    private static String ws(String str) {
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
            a.su("PushLog3414", e.toString());
            return "";
        }
    }
}
