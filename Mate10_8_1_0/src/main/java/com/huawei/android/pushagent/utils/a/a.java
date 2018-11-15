package com.huawei.android.pushagent.utils.a;

import java.io.UnsupportedEncodingException;

public class a {
    private static final char[] q = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String u(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        if (bArr.length == 0) {
            return "";
        }
        char[] cArr = new char[(bArr.length * 2)];
        for (int i = 0; i < bArr.length; i++) {
            byte b = bArr[i];
            cArr[i * 2] = q[(b & 240) >> 4];
            cArr[(i * 2) + 1] = q[b & 15];
        }
        return new String(cArr);
    }

    public static String v(byte b) {
        return new String(new char[]{q[(b & 240) >> 4], q[b & 15]});
    }

    public static byte[] w(String str) {
        byte[] bArr = new byte[(str.length() / 2)];
        try {
            byte[] bytes = str.getBytes("UTF-8");
            for (int i = 0; i < bArr.length; i++) {
                bArr[i] = (byte) (((byte) (Byte.decode("0x" + new String(new byte[]{bytes[i * 2]}, "UTF-8")).byteValue() << 4)) ^ Byte.decode("0x" + new String(new byte[]{bytes[(i * 2) + 1]}, "UTF-8")).byteValue());
            }
        } catch (UnsupportedEncodingException e) {
            b.y("PushLog2976", e.toString());
        }
        return bArr;
    }
}
