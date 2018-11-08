package com.huawei.android.pushagent.a.a.a;

import android.text.TextUtils;
import com.huawei.android.pushagent.a.a.b;
import com.huawei.android.pushagent.a.a.c;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class a {
    public static String a(String str) {
        return !TextUtils.isEmpty(str) ? a(str, a()) : "";
    }

    private static String a(String str, String str2) {
        Object -l_2_R;
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str2)) {
            return "";
        }
        try {
            -l_2_R = new StringBuffer();
            -l_2_R.append(str2.substring(0, 6));
            -l_2_R.append(str.substring(0, 6));
            -l_2_R.append(str2.substring(6, 10));
            -l_2_R.append(str.substring(6, 16));
            -l_2_R.append(str2.substring(10, 16));
            -l_2_R.append(str.substring(16));
            -l_2_R.append(str2.substring(16));
            return -l_2_R.toString();
        } catch (Object -l_2_R2) {
            c.d("AES128_CBC", -l_2_R2.toString());
            return "";
        }
    }

    public static String a(String str, byte[] bArr) {
        Object -l_2_R;
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        if (bArr == null || bArr.length <= 0) {
            return "";
        }
        try {
            -l_2_R = new SecretKeySpec(bArr, "AES");
            Object -l_3_R = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] -l_5_R = new byte[16];
            new SecureRandom().nextBytes(-l_5_R);
            -l_3_R.init(1, -l_2_R, new IvParameterSpec(-l_5_R));
            return a(com.huawei.android.pushagent.a.a.a.a(-l_5_R), com.huawei.android.pushagent.a.a.a.a(-l_3_R.doFinal(str.getBytes("UTF-8"))));
        } catch (Object -l_2_R2) {
            c.d("AES128_CBC", "aes cbc encrypter data error", -l_2_R2);
            return null;
        } catch (Object -l_2_R22) {
            c.d("AES128_CBC", "aes cbc encrypter data error", -l_2_R22);
            return null;
        } catch (Object -l_2_R222) {
            c.d("AES128_CBC", "aes cbc encrypter data error", -l_2_R222);
            return null;
        } catch (Object -l_2_R2222) {
            c.d("AES128_CBC", "aes cbc encrypter data error", -l_2_R2222);
            return null;
        } catch (Object -l_2_R22222) {
            c.d("AES128_CBC", "aes cbc encrypter data error", -l_2_R22222);
            return null;
        } catch (Object -l_2_R222222) {
            c.d("AES128_CBC", "aes cbc encrypter data error", -l_2_R222222);
            return null;
        } catch (Object -l_2_R2222222) {
            c.d("AES128_CBC", "aes cbc encrypter data error", -l_2_R2222222);
            return null;
        } catch (Object -l_2_R22222222) {
            c.d("AES128_CBC", "aes cbc encrypter data error", -l_2_R22222222);
            return null;
        } catch (Object -l_2_R222222222) {
            c.d("AES128_CBC", "aes cbc encrypter data error", -l_2_R222222222);
            return null;
        }
    }

    private static byte[] a() {
        byte[] -l_0_R = com.huawei.android.pushagent.a.a.a.a(b.a());
        byte[] -l_1_R = com.huawei.android.pushagent.a.a.a.a(b.a());
        return a(a(a(-l_0_R, -l_1_R), com.huawei.android.pushagent.a.a.a.a("2A57086C86EF54970C1E6EB37BFC72B1")));
    }

    private static byte[] a(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            return new byte[0];
        }
        for (int -l_1_I = 0; -l_1_I < bArr.length; -l_1_I++) {
            bArr[-l_1_I] = (byte) ((byte) (bArr[-l_1_I] >> 2));
        }
        return bArr;
    }

    private static byte[] a(byte[] bArr, byte[] bArr2) {
        if (bArr == null || bArr2 == null || bArr.length == 0 || bArr2.length == 0) {
            return new byte[0];
        }
        int -l_2_I = bArr.length;
        if (-l_2_I != bArr2.length) {
            return new byte[0];
        }
        Object -l_4_R = new byte[-l_2_I];
        for (int -l_5_I = 0; -l_5_I < -l_2_I; -l_5_I++) {
            -l_4_R[-l_5_I] = (byte) ((byte) (bArr[-l_5_I] ^ bArr2[-l_5_I]));
        }
        return -l_4_R;
    }

    public static String b(String str) {
        return !TextUtils.isEmpty(str) ? b(str, a()) : "";
    }

    public static String b(String str, byte[] bArr) {
        Object -l_2_R;
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        if (bArr == null || bArr.length <= 0) {
            return "";
        }
        try {
            -l_2_R = new SecretKeySpec(bArr, "AES");
            Object -l_3_R = Cipher.getInstance("AES/CBC/PKCS5Padding");
            String -l_4_R = c(str);
            String -l_5_R = d(str);
            if (TextUtils.isEmpty(-l_4_R) || TextUtils.isEmpty(-l_5_R)) {
                c.b("AES128_CBC", "ivParameter or encrypedWord is null");
                return "";
            }
            -l_3_R.init(2, -l_2_R, new IvParameterSpec(com.huawei.android.pushagent.a.a.a.a(-l_4_R)));
            return new String(-l_3_R.doFinal(com.huawei.android.pushagent.a.a.a.a(-l_5_R)), "UTF-8");
        } catch (Object -l_2_R2) {
            c.d("AES128_CBC", "aes cbc decrypter data error", -l_2_R2);
            return "";
        } catch (Object -l_2_R22) {
            c.d("AES128_CBC", "aes cbc decrypter data error", -l_2_R22);
            return "";
        } catch (Object -l_2_R222) {
            c.d("AES128_CBC", "aes cbc decrypter data error", -l_2_R222);
            return "";
        } catch (Object -l_2_R2222) {
            c.d("AES128_CBC", "aes cbc decrypter data error", -l_2_R2222);
            return "";
        } catch (Object -l_2_R22222) {
            c.d("AES128_CBC", "aes cbc decrypter data error", -l_2_R22222);
            return "";
        } catch (Object -l_2_R222222) {
            c.d("AES128_CBC", "aes cbc decrypter data error", -l_2_R222222);
            return "";
        } catch (Object -l_2_R2222222) {
            c.d("AES128_CBC", "aes cbc decrypter data error", -l_2_R2222222);
            return "";
        } catch (Object -l_2_R22222222) {
            c.d("AES128_CBC", "aes cbc decrypter data error", -l_2_R22222222);
            return "";
        } catch (Object -l_2_R222222222) {
            c.d("AES128_CBC", "aes cbc encrypter data error", -l_2_R222222222);
            return "";
        }
    }

    private static String c(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        Object -l_1_R;
        try {
            -l_1_R = new StringBuffer();
            -l_1_R.append(str.substring(6, 12));
            -l_1_R.append(str.substring(16, 26));
            -l_1_R.append(str.substring(32, 48));
            return -l_1_R.toString();
        } catch (Object -l_1_R2) {
            c.d("AES128_CBC", -l_1_R2.toString());
            return "";
        }
    }

    private static String d(String str) {
        if (TextUtils.isEmpty(str)) {
            return "";
        }
        Object -l_1_R;
        try {
            -l_1_R = new StringBuffer();
            -l_1_R.append(str.substring(0, 6));
            -l_1_R.append(str.substring(12, 16));
            -l_1_R.append(str.substring(26, 32));
            -l_1_R.append(str.substring(48));
            return -l_1_R.toString();
        } catch (Object -l_1_R2) {
            c.d("AES128_CBC", -l_1_R2.toString());
            return "";
        }
    }
}
