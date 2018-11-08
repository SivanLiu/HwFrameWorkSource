package com.hianalytics.android.a.a;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class b {
    public static byte[] a(String str, byte[] bArr) {
        try {
            Object -l_2_R = Cipher.getInstance("AES/CBC/NoPadding");
            int -l_3_I = -l_2_R.getBlockSize();
            int -l_4_I = bArr.length;
            if (-l_4_I % -l_3_I != 0) {
                -l_4_I += -l_3_I - (-l_4_I % -l_3_I);
            }
            Object -l_3_R = new byte[-l_4_I];
            System.arraycopy(bArr, 0, -l_3_R, 0, bArr.length);
            -l_2_R.init(1, new SecretKeySpec(str.getBytes("UTF-8"), "AES"), new IvParameterSpec(str.getBytes("UTF-8")));
            return -l_2_R.doFinal(-l_3_R);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
