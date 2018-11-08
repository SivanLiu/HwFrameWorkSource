package com.tencent.tcuser.util;

public class a {
    public static byte[] at(String str) {
        if (str == null || str.trim().length() <= 0) {
            return new byte[0];
        }
        int -l_1_I = str.length() / 2;
        Object -l_2_R = new byte[-l_1_I];
        Object -l_3_R = str.toCharArray();
        for (int -l_4_I = 0; -l_4_I < -l_1_I; -l_4_I++) {
            int -l_5_I = -l_4_I * 2;
            -l_2_R[-l_4_I] = (byte) ((byte) ((b(-l_3_R[-l_5_I]) << 4) | b(-l_3_R[-l_5_I + 1])));
        }
        return -l_2_R;
    }

    public static byte au(String str) {
        if (str != null) {
            try {
                if (str.trim().length() > 0) {
                    return Byte.valueOf(str).byteValue();
                }
            } catch (Throwable th) {
                return (byte) -1;
            }
        }
        return (byte) -1;
    }

    public static int av(String str) {
        if (str != null) {
            try {
                if (str.trim().length() > 0) {
                    return Integer.valueOf(str).intValue();
                }
            } catch (Throwable th) {
                return -1;
            }
        }
        return -1;
    }

    public static long aw(String str) {
        if (str != null) {
            try {
                if (str.trim().length() > 0) {
                    return Long.valueOf(str).longValue();
                }
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }

    private static byte b(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static final String bytesToHexString(byte[] bArr) {
        if (bArr == null || bArr.length <= 0) {
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
}
