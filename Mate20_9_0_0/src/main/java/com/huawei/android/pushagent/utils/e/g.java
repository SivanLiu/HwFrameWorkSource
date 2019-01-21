package com.huawei.android.pushagent.utils.e;

import android.util.Base64;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

class g {
    g() {
    }

    private static PublicKey wy(String str) {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decode(str.getBytes("UTF-8"), 0)));
    }

    static byte[] wx(byte[] bArr, String str) {
        Cipher instance = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        instance.init(1, wy(str));
        return instance.doFinal(bArr);
    }
}
