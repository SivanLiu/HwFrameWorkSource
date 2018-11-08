package tmsdkobf;

import java.security.interfaces.RSAPublicKey;
import javax.crypto.Cipher;
import javax.security.cert.X509Certificate;

final class ip {
    public static byte[] a(byte[] bArr, RSAPublicKey rSAPublicKey) {
        Object -l_2_R = null;
        if (rSAPublicKey != null) {
            Object -l_3_R;
            try {
                -l_3_R = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                -l_3_R.init(2, rSAPublicKey);
                -l_2_R = -l_3_R.doFinal(bArr);
            } catch (Object -l_3_R2) {
                -l_3_R2.printStackTrace();
            }
        }
        return -l_2_R;
    }

    public static RSAPublicKey h(byte[] bArr) {
        Object -l_1_R = null;
        try {
            return (RSAPublicKey) X509Certificate.getInstance(bArr).getPublicKey();
        } catch (Object -l_2_R) {
            -l_2_R.printStackTrace();
            return -l_1_R;
        }
    }
}
