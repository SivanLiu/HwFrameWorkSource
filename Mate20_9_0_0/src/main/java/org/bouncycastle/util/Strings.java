package org.bouncycastle.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Vector;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.pqc.math.linearalgebra.Matrix;

public final class Strings {
    private static String LINE_SEPARATOR;

    private static class StringListImpl extends ArrayList<String> implements StringList {
        private StringListImpl() {
        }

        /* synthetic */ StringListImpl(AnonymousClass1 anonymousClass1) {
            this();
        }

        public void add(int i, String str) {
            super.add(i, str);
        }

        public boolean add(String str) {
            return super.add(str);
        }

        public /* bridge */ /* synthetic */ String get(int i) {
            return (String) super.get(i);
        }

        public String set(int i, String str) {
            return (String) super.set(i, str);
        }

        public String[] toStringArray() {
            String[] strArr = new String[size()];
            for (int i = 0; i != strArr.length; i++) {
                strArr[i] = (String) get(i);
            }
            return strArr;
        }

        public String[] toStringArray(int i, int i2) {
            String[] strArr = new String[(i2 - i)];
            int i3 = i;
            while (i3 != size() && i3 != i2) {
                strArr[i3 - i] = (String) get(i3);
                i3++;
            }
            return strArr;
        }
    }

    static {
        try {
            LINE_SEPARATOR = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty("line.separator");
                }
            });
        } catch (Exception e) {
            try {
                LINE_SEPARATOR = String.format("%n", new Object[0]);
            } catch (Exception e2) {
                LINE_SEPARATOR = "\n";
            }
        }
    }

    public static char[] asCharArray(byte[] bArr) {
        char[] cArr = new char[bArr.length];
        for (int i = 0; i != cArr.length; i++) {
            cArr[i] = (char) (bArr[i] & 255);
        }
        return cArr;
    }

    public static String fromByteArray(byte[] bArr) {
        return new String(asCharArray(bArr));
    }

    public static String fromUTF8ByteArray(byte[] bArr) {
        int i = 0;
        int i2 = 0;
        int i3 = i2;
        while (i2 < bArr.length) {
            i3++;
            if ((bArr[i2] & 240) == 240) {
                i3++;
                i2 += 4;
            } else {
                i2 = (bArr[i2] & 224) == 224 ? i2 + 3 : (bArr[i2] & 192) == 192 ? i2 + 2 : i2 + 1;
            }
        }
        char[] cArr = new char[i3];
        i3 = 0;
        while (i < bArr.length) {
            char c;
            int i4;
            int i5;
            if ((bArr[i] & 240) == 240) {
                i5 = (((((bArr[i] & 3) << 18) | ((bArr[i + 1] & 63) << 12)) | ((bArr[i + 2] & 63) << 6)) | (bArr[i + 3] & 63)) - PKIFailureInfo.notAuthorized;
                c = (char) ((i5 & 1023) | 56320);
                int i6 = i3 + 1;
                cArr[i3] = (char) (55296 | (i5 >> 10));
                i += 4;
                i3 = i6;
            } else if ((bArr[i] & 224) == 224) {
                c = (char) ((((bArr[i] & 15) << 12) | ((bArr[i + 1] & 63) << 6)) | (bArr[i + 2] & 63));
                i += 3;
            } else {
                if ((bArr[i] & 208) == 208) {
                    i5 = (bArr[i] & 31) << 6;
                    i4 = bArr[i + 1];
                } else if ((bArr[i] & 192) == 192) {
                    i5 = (bArr[i] & 31) << 6;
                    i4 = bArr[i + 1];
                } else {
                    c = (char) (bArr[i] & 255);
                    i++;
                }
                c = (char) (i5 | (i4 & 63));
                i += 2;
            }
            i4 = i3 + 1;
            cArr[i3] = c;
            i3 = i4;
        }
        return new String(cArr);
    }

    public static String lineSeparator() {
        return LINE_SEPARATOR;
    }

    public static StringList newList() {
        return new StringListImpl();
    }

    public static String[] split(String str, char c) {
        int i;
        Vector vector = new Vector();
        int i2 = 1;
        while (true) {
            i = 0;
            if (i2 == 0) {
                break;
            }
            int indexOf = str.indexOf(c);
            if (indexOf > 0) {
                vector.addElement(str.substring(0, indexOf));
                str = str.substring(indexOf + 1);
            } else {
                vector.addElement(str);
                i2 = 0;
            }
        }
        String[] strArr = new String[vector.size()];
        while (i != strArr.length) {
            strArr[i] = (String) vector.elementAt(i);
            i++;
        }
        return strArr;
    }

    public static int toByteArray(String str, byte[] bArr, int i) {
        int length = str.length();
        for (int i2 = 0; i2 < length; i2++) {
            bArr[i + i2] = (byte) str.charAt(i2);
        }
        return length;
    }

    public static byte[] toByteArray(String str) {
        byte[] bArr = new byte[str.length()];
        for (int i = 0; i != bArr.length; i++) {
            bArr[i] = (byte) str.charAt(i);
        }
        return bArr;
    }

    public static byte[] toByteArray(char[] cArr) {
        byte[] bArr = new byte[cArr.length];
        for (int i = 0; i != bArr.length; i++) {
            bArr[i] = (byte) cArr[i];
        }
        return bArr;
    }

    public static String toLowerCase(String str) {
        char[] toCharArray = str.toCharArray();
        int i = 0;
        int i2 = 0;
        while (i != toCharArray.length) {
            char c = toCharArray[i];
            if ('A' <= c && Matrix.MATRIX_TYPE_ZERO >= c) {
                toCharArray[i] = (char) ((c - 65) + 97);
                i2 = 1;
            }
            i++;
        }
        return i2 != 0 ? new String(toCharArray) : str;
    }

    public static void toUTF8ByteArray(char[] cArr, OutputStream outputStream) throws IOException {
        int i = 0;
        while (i < cArr.length) {
            int i2 = cArr[i];
            if (i2 >= 128) {
                int i3;
                if (i2 < 2048) {
                    i3 = 192 | (i2 >> 6);
                } else if (i2 < 55296 || i2 > 57343) {
                    outputStream.write(224 | (i2 >> 12));
                    i3 = ((i2 >> 6) & 63) | 128;
                } else {
                    i++;
                    if (i < cArr.length) {
                        char c = cArr[i];
                        if (i2 <= 56319) {
                            i2 = (((i2 & 1023) << 10) | (c & 1023)) + PKIFailureInfo.notAuthorized;
                            outputStream.write(240 | (i2 >> 18));
                            outputStream.write(((i2 >> 12) & 63) | 128);
                            outputStream.write(((i2 >> 6) & 63) | 128);
                            i2 = (i2 & 63) | 128;
                        } else {
                            throw new IllegalStateException("invalid UTF-16 codepoint");
                        }
                    }
                    throw new IllegalStateException("invalid UTF-16 codepoint");
                }
                outputStream.write(i3);
                i2 = (i2 & 63) | 128;
            }
            outputStream.write(i2);
            i++;
        }
    }

    public static byte[] toUTF8ByteArray(String str) {
        return toUTF8ByteArray(str.toCharArray());
    }

    public static byte[] toUTF8ByteArray(char[] cArr) {
        OutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            toUTF8ByteArray(cArr, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("cannot encode string to byte array!");
        }
    }

    public static String toUpperCase(String str) {
        char[] toCharArray = str.toCharArray();
        int i = 0;
        int i2 = 0;
        while (i != toCharArray.length) {
            char c = toCharArray[i];
            if ('a' <= c && 'z' >= c) {
                toCharArray[i] = (char) ((c - 97) + 65);
                i2 = 1;
            }
            i++;
        }
        return i2 != 0 ? new String(toCharArray) : str;
    }
}
