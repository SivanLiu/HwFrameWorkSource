package tmsdkobf;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class fz {
    public static byte[] P() {
        Object -l_0_R;
        try {
            -l_0_R = new StringBuffer();
            Object -l_1_R = "http://pmir.3g.qq.com";
            Object -l_2_R = new int[]{-36, -46, -45, -77, -22, -10, 47, -77, -72, -69, -32, 25, 21, -21, -6, -75, -71, 31, -39, -49, -49};
            for (int -l_3_I = 0; -l_3_I < -l_1_R.length(); -l_3_I++) {
                -l_0_R.append((char) (-l_1_R.charAt(-l_3_I) + -l_2_R[-l_3_I]));
            }
            return -l_0_R.toString().getBytes("UTF-8");
        } catch (Object -l_0_R2) {
            -l_0_R2.printStackTrace();
            return null;
        }
    }

    private static void a(byte[] bArr, int[] iArr) {
        int -l_4_I = bArr.length >> 2;
        int -l_2_I = 0;
        int -l_3_I = 0;
        while (-l_2_I < -l_4_I) {
            int -l_3_I2 = -l_3_I + 1;
            iArr[-l_2_I] = bArr[-l_3_I] & 255;
            -l_3_I = -l_3_I2 + 1;
            iArr[-l_2_I] = iArr[-l_2_I] | ((bArr[-l_3_I2] & 255) << 8);
            -l_3_I2 = -l_3_I + 1;
            iArr[-l_2_I] = iArr[-l_2_I] | ((bArr[-l_3_I] & 255) << 16);
            -l_3_I = -l_3_I2 + 1;
            iArr[-l_2_I] = iArr[-l_2_I] | ((bArr[-l_3_I2] & 255) << 24);
            -l_2_I++;
        }
        if (-l_3_I >= bArr.length) {
            -l_3_I2 = -l_3_I;
            return;
        }
        -l_3_I2 = -l_3_I + 1;
        iArr[-l_2_I] = bArr[-l_3_I] & 255;
        int -l_5_I = 8;
        while (-l_3_I2 < bArr.length) {
            iArr[-l_2_I] = iArr[-l_2_I] | ((bArr[-l_3_I2] & 255) << -l_5_I);
            -l_3_I2++;
            -l_5_I += 8;
        }
    }

    private static void a(int[] iArr, int -l_5_I, byte[] bArr) {
        int i;
        int -l_5_I2 = bArr.length >> 2;
        if (-l_5_I2 > -l_5_I) {
            -l_5_I2 = -l_5_I;
        }
        int -l_3_I = 0;
        int -l_4_I = 0;
        while (-l_3_I < -l_5_I2) {
            i = -l_4_I + 1;
            bArr[-l_4_I] = (byte) ((byte) (iArr[-l_3_I] & 255));
            -l_4_I = i + 1;
            bArr[i] = (byte) ((byte) ((iArr[-l_3_I] >>> 8) & 255));
            i = -l_4_I + 1;
            bArr[-l_4_I] = (byte) ((byte) ((iArr[-l_3_I] >>> 16) & 255));
            -l_4_I = i + 1;
            bArr[i] = (byte) ((byte) ((iArr[-l_3_I] >>> 24) & 255));
            -l_3_I++;
        }
        if (-l_5_I > -l_5_I2 && -l_4_I < bArr.length) {
            i = -l_4_I + 1;
            bArr[-l_4_I] = (byte) ((byte) (iArr[-l_3_I] & 255));
            int -l_6_I = 8;
            -l_4_I = i;
            while (-l_6_I <= 24 && -l_4_I < bArr.length) {
                i = -l_4_I + 1;
                bArr[-l_4_I] = (byte) ((byte) ((iArr[-l_3_I] >>> -l_6_I) & 255));
                -l_6_I += 8;
                -l_4_I = i;
            }
        }
        i = -l_4_I;
    }

    public static byte[] a(byte[] bArr, byte[] bArr2) {
        byte[] d = d(bArr2);
        if (bArr == null || d == null || bArr.length == 0) {
            return bArr;
        }
        int -l_5_I;
        int -l_4_I = bArr.length % 4 != 0 ? (bArr.length >>> 2) + 2 : (bArr.length >>> 2) + 1;
        int[] -l_2_R = new int[-l_4_I];
        a(bArr, -l_2_R);
        -l_2_R[-l_4_I - 1] = bArr.length;
        -l_4_I = d.length % 4 != 0 ? (d.length >>> 2) + 1 : d.length >>> 2;
        if (-l_4_I < 4) {
            -l_4_I = 4;
        }
        int[] -l_3_R = new int[-l_4_I];
        for (-l_5_I = 0; -l_5_I < -l_4_I; -l_5_I++) {
            -l_3_R[-l_5_I] = 0;
        }
        a(d, -l_3_R);
        -l_4_I = -l_2_R.length - 1;
        -l_5_I = -l_2_R[-l_4_I];
        int -l_6_I = -l_2_R[0];
        int -l_8_I = 0;
        int -l_11_I = (52 / (-l_4_I + 1)) + 6;
        while (true) {
            int -l_11_I2 = -l_11_I - 1;
            if (-l_11_I <= 0) {
                Object -l_12_R = new byte[(-l_2_R.length << 2)];
                a(-l_2_R, -l_2_R.length, -l_12_R);
                return -l_12_R;
            }
            -l_8_I -= 1640531527;
            int -l_9_I = (-l_8_I >>> 2) & 3;
            int -l_10_I = 0;
            while (-l_10_I < -l_4_I) {
                -l_6_I = -l_2_R[-l_10_I + 1];
                -l_5_I = -l_2_R[-l_10_I] + ((((-l_5_I >>> 5) ^ (-l_6_I << 2)) + ((-l_6_I >>> 3) ^ (-l_5_I << 4))) ^ ((-l_8_I ^ -l_6_I) + (-l_3_R[(-l_10_I & 3) ^ -l_9_I] ^ -l_5_I)));
                -l_2_R[-l_10_I] = -l_5_I;
                -l_10_I++;
            }
            -l_6_I = -l_2_R[0];
            -l_5_I = -l_2_R[-l_4_I] + ((((-l_5_I >>> 5) ^ (-l_6_I << 2)) + ((-l_6_I >>> 3) ^ (-l_5_I << 4))) ^ ((-l_8_I ^ -l_6_I) + (-l_3_R[(-l_10_I & 3) ^ -l_9_I] ^ -l_5_I)));
            -l_2_R[-l_4_I] = -l_5_I;
            -l_11_I = -l_11_I2;
        }
    }

    public static byte[] b(byte[] bArr, byte[] bArr2) {
        byte[] d = d(bArr2);
        if (bArr == null || d == null || bArr.length == 0) {
            return bArr;
        }
        if (bArr.length % 4 != 0 || bArr.length < 8) {
            return null;
        }
        int -l_5_I;
        int[] -l_2_R = new int[(bArr.length >>> 2)];
        a(bArr, -l_2_R);
        int -l_4_I = d.length % 4 != 0 ? (d.length >>> 2) + 1 : d.length >>> 2;
        if (-l_4_I < 4) {
            -l_4_I = 4;
        }
        int[] -l_3_R = new int[-l_4_I];
        for (-l_5_I = 0; -l_5_I < -l_4_I; -l_5_I++) {
            -l_3_R[-l_5_I] = 0;
        }
        a(d, -l_3_R);
        -l_4_I = -l_2_R.length - 1;
        -l_5_I = -l_2_R[-l_4_I];
        int -l_6_I = -l_2_R[0];
        for (int -l_8_I = ((52 / (-l_4_I + 1)) + 6) * -1640531527; -l_8_I != 0; -l_8_I -= -1640531527) {
            int -l_9_I = (-l_8_I >>> 2) & 3;
            int -l_10_I = -l_4_I;
            while (-l_10_I > 0) {
                -l_5_I = -l_2_R[-l_10_I - 1];
                -l_6_I = -l_2_R[-l_10_I] - ((((-l_5_I >>> 5) ^ (-l_6_I << 2)) + ((-l_6_I >>> 3) ^ (-l_5_I << 4))) ^ ((-l_8_I ^ -l_6_I) + (-l_3_R[(-l_10_I & 3) ^ -l_9_I] ^ -l_5_I)));
                -l_2_R[-l_10_I] = -l_6_I;
                -l_10_I--;
            }
            -l_5_I = -l_2_R[-l_4_I];
            -l_6_I = -l_2_R[0] - ((((-l_5_I >>> 5) ^ (-l_6_I << 2)) + ((-l_6_I >>> 3) ^ (-l_5_I << 4))) ^ ((-l_8_I ^ -l_6_I) + (-l_3_R[(-l_10_I & 3) ^ -l_9_I] ^ -l_5_I)));
            -l_2_R[0] = -l_6_I;
        }
        -l_4_I = -l_2_R[-l_2_R.length - 1];
        if (-l_4_I < 0 || -l_4_I > ((-l_2_R.length - 1) << 2)) {
            return null;
        }
        Object -l_12_R = new byte[-l_4_I];
        a(-l_2_R, -l_2_R.length - 1, -l_12_R);
        return -l_12_R;
    }

    private static byte[] d(byte[] -l_1_R) {
        if (-l_1_R == null || -l_1_R.length <= 16) {
            return -l_1_R;
        }
        try {
            Object -l_2_R = MessageDigest.getInstance("MD5");
            -l_2_R.update(-l_1_R);
            return -l_2_R.digest();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
