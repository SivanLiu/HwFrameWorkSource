package com.huawei.android.pushagent.a.a.a;

import android.util.Base64;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

public class e {
    private static PublicKey a(String str) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(str, 0)));
    }

    public static byte[] a(byte[] bArr, String str) throws Exception {
        Object -l_2_R = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        -l_2_R.init(1, a(str));
        return -l_2_R.doFinal(bArr);
    }
}
