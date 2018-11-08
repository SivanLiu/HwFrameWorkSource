package tmsdkobf;

import java.security.MessageDigest;

public class mc {
    private static final char[] zI = "0123456789abcdef".toCharArray();

    public static byte[] bT(String str) {
        return l(str.getBytes());
    }

    public static String bU(String str) {
        Object -l_1_R = bT(str);
        if (-l_1_R == null) {
            return null;
        }
        Object -l_2_R = new StringBuilder(-l_1_R.length * 2);
        Object -l_3_R = -l_1_R;
        for (int -l_6_I : -l_1_R) {
            -l_2_R.append(Integer.toHexString(-l_6_I & 255).substring(0, 1));
        }
        return -l_2_R.toString();
    }

    public static byte[] l(byte[] bArr) {
        Object -l_1_R;
        try {
            -l_1_R = MessageDigest.getInstance("MD5");
            -l_1_R.update(bArr);
            return -l_1_R.digest();
        } catch (Object -l_1_R2) {
            -l_1_R2.printStackTrace();
            return null;
        }
    }

    public static String m(byte[] -l_2_R) {
        Object -l_1_R = new StringBuilder(-l_2_R.length * 3);
        for (int -l_5_I : -l_2_R) {
            int -l_5_I2 = -l_5_I2 & 255;
            -l_1_R.append(zI[-l_5_I2 >> 4]);
            -l_1_R.append(zI[-l_5_I2 & 15]);
        }
        return -l_1_R.toString().toUpperCase();
    }

    public static String n(byte[] bArr) {
        return m(l(bArr));
    }
}
