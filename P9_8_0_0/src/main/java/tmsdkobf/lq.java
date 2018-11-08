package tmsdkobf;

public class lq {
    public static byte[] aO(int i) {
        return new byte[]{(byte) ((byte) (i & 255)), (byte) ((byte) ((i >> 8) & 255)), (byte) ((byte) ((i >> 16) & 255)), (byte) ((byte) ((i >> 24) & 255))};
    }

    public static byte[] at(String str) {
        int -l_1_I = str.length() / 2;
        Object -l_2_R = new byte[-l_1_I];
        Object -l_3_R = str.toCharArray();
        for (int -l_4_I = 0; -l_4_I < -l_1_I; -l_4_I++) {
            int -l_5_I = -l_4_I * 2;
            -l_2_R[-l_4_I] = (byte) ((byte) ((b(-l_3_R[-l_5_I]) << 4) | b(-l_3_R[-l_5_I + 1])));
        }
        return -l_2_R;
    }

    private static byte b(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static byte[] bJ(String str) {
        Object -l_1_R = new byte[str.length()];
        Object -l_2_R = str.toCharArray();
        for (int -l_3_I = 0; -l_3_I < str.length(); -l_3_I++) {
            -l_1_R[-l_3_I] = (byte) ((byte) -l_2_R[-l_3_I]);
        }
        return -l_1_R;
    }

    public static final String bytesToHexString(byte[] bArr) {
        if (bArr == null) {
            return "";
        }
        Object -l_1_R = new StringBuffer(bArr.length);
        for (byte b : bArr) {
            Object -l_2_R = Integer.toHexString(b & 255);
            if (-l_2_R.length() < 2) {
                -l_1_R.append(0);
            }
            -l_1_R.append(-l_2_R.toUpperCase());
        }
        return -l_1_R.toString();
    }

    public static final String bytesToString(byte[] bArr) {
        if (bArr == null) {
            return "";
        }
        Object -l_1_R = new StringBuffer(bArr.length);
        for (byte b : bArr) {
            -l_1_R.append((char) b);
        }
        return -l_1_R.toString().toUpperCase();
    }

    public static int k(byte[] bArr) {
        return bArr.length == 4 ? (((bArr[0] & 255) | ((bArr[1] & 255) << 8)) | ((bArr[2] & 255) << 16)) | ((bArr[3] & 255) << 24) : 0;
    }
}
