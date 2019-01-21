package com.android.okhttp.okio;

import java.io.UnsupportedEncodingException;

final class Base64 {
    private static final byte[] MAP = new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80, (byte) 81, (byte) 82, (byte) 83, (byte) 84, (byte) 85, (byte) 86, (byte) 87, (byte) 88, (byte) 89, (byte) 90, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102, (byte) 103, (byte) 104, (byte) 105, (byte) 106, (byte) 107, (byte) 108, (byte) 109, (byte) 110, (byte) 111, (byte) 112, (byte) 113, (byte) 114, (byte) 115, (byte) 116, (byte) 117, (byte) 118, (byte) 119, (byte) 120, (byte) 121, (byte) 122, (byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 43, (byte) 47};
    private static final byte[] URL_MAP = new byte[]{(byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80, (byte) 81, (byte) 82, (byte) 83, (byte) 84, (byte) 85, (byte) 86, (byte) 87, (byte) 88, (byte) 89, (byte) 90, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102, (byte) 103, (byte) 104, (byte) 105, (byte) 106, (byte) 107, (byte) 108, (byte) 109, (byte) 110, (byte) 111, (byte) 112, (byte) 113, (byte) 114, (byte) 115, (byte) 116, (byte) 117, (byte) 118, (byte) 119, (byte) 120, (byte) 121, (byte) 122, (byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57, (byte) 45, (byte) 95};

    private Base64() {
    }

    public static byte[] decode(String in) {
        int limit = in.length();
        while (limit > 0) {
            char c = in.charAt(limit - 1);
            if (c != '=' && c != 10 && c != 13 && c != ' ' && c != 9) {
                break;
            }
            limit--;
        }
        byte[] out = new byte[((int) ((((long) limit) * 6) / 8))];
        int inCount = 0;
        int word = 0;
        int outCount = 0;
        for (int pos = 0; pos < limit; pos++) {
            int bits;
            char c2 = in.charAt(pos);
            if (c2 >= 'A' && c2 <= 'Z') {
                bits = c2 - 65;
            } else if (c2 >= 'a' && c2 <= 'z') {
                bits = c2 - 71;
            } else if (c2 >= '0' && c2 <= '9') {
                bits = c2 + 4;
            } else if (c2 == '+' || c2 == '-') {
                bits = 62;
            } else if (c2 == '/' || c2 == '_') {
                bits = 63;
            } else {
                if (!(c2 == 10 || c2 == 13 || c2 == ' ' || c2 == 9)) {
                    return null;
                }
            }
            word = (word << 6) | ((byte) bits);
            inCount++;
            if (inCount % 4 == 0) {
                int outCount2 = outCount + 1;
                out[outCount] = (byte) (word >> 16);
                outCount = outCount2 + 1;
                out[outCount2] = (byte) (word >> 8);
                outCount2 = outCount + 1;
                out[outCount] = (byte) word;
                outCount = outCount2;
            }
        }
        int lastWordChars = inCount % 4;
        if (lastWordChars == 1) {
            return null;
        }
        int outCount3;
        if (lastWordChars == 2) {
            outCount3 = outCount + 1;
            out[outCount] = (byte) ((word << 12) >> 16);
            outCount = outCount3;
        } else if (lastWordChars == 3) {
            word <<= 6;
            outCount3 = outCount + 1;
            out[outCount] = (byte) (word >> 16);
            outCount = outCount3 + 1;
            out[outCount3] = (byte) (word >> 8);
        }
        if (outCount == out.length) {
            return out;
        }
        byte[] prefix = new byte[outCount];
        System.arraycopy(out, 0, prefix, 0, outCount);
        return prefix;
    }

    public static String encode(byte[] in) {
        return encode(in, MAP);
    }

    public static String encodeUrl(byte[] in) {
        return encode(in, URL_MAP);
    }

    private static String encode(byte[] in, byte[] map) {
        int i;
        byte[] out = new byte[(((in.length + 2) * 4) / 3)];
        int end = in.length - (in.length % 3);
        int index = 0;
        for (i = 0; i < end; i += 3) {
            int index2 = index + 1;
            out[index] = map[(in[i] & 255) >> 2];
            index = index2 + 1;
            out[index2] = map[((in[i] & 3) << 4) | ((in[i + 1] & 255) >> 4)];
            index2 = index + 1;
            out[index] = map[((in[i + 1] & 15) << 2) | ((in[i + 2] & 255) >> 6)];
            index = index2 + 1;
            out[index2] = map[in[i + 2] & 63];
        }
        switch (in.length % 3) {
            case 1:
                i = index + 1;
                out[index] = map[(in[end] & 255) >> 2];
                index = i + 1;
                out[i] = map[(in[end] & 3) << 4];
                i = index + 1;
                out[index] = (byte) 61;
                index = i + 1;
                out[i] = (byte) 61;
                break;
            case 2:
                i = index + 1;
                out[index] = map[(in[end] & 255) >> 2];
                index = i + 1;
                out[i] = map[((in[end] & 3) << 4) | ((in[end + 1] & 255) >> 4)];
                i = index + 1;
                out[index] = map[(in[end + 1] & 15) << 2];
                index = i + 1;
                out[i] = (byte) 61;
                break;
        }
        try {
            return new String(out, 0, index, "US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
}
