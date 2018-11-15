package org.bouncycastle.crypto.generators;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;

public class OpenBSDBCrypt {
    private static final Set<String> allowedVersions = new HashSet();
    private static final byte[] decodingTable = new byte[128];
    private static final String defaultVersion = "2y";
    private static final byte[] encodingTable = new byte[]{(byte) 46, (byte) 47, (byte) 65, (byte) 66, (byte) 67, (byte) 68, (byte) 69, (byte) 70, (byte) 71, (byte) 72, (byte) 73, (byte) 74, (byte) 75, (byte) 76, (byte) 77, (byte) 78, (byte) 79, (byte) 80, (byte) 81, (byte) 82, (byte) 83, (byte) 84, (byte) 85, (byte) 86, (byte) 87, (byte) 88, (byte) 89, (byte) 90, (byte) 97, (byte) 98, (byte) 99, (byte) 100, (byte) 101, (byte) 102, (byte) 103, (byte) 104, (byte) 105, (byte) 106, (byte) 107, (byte) 108, (byte) 109, (byte) 110, (byte) 111, (byte) 112, (byte) 113, (byte) 114, (byte) 115, (byte) 116, (byte) 117, (byte) 118, (byte) 119, (byte) 120, (byte) 121, (byte) 122, (byte) 48, (byte) 49, (byte) 50, (byte) 51, (byte) 52, (byte) 53, (byte) 54, (byte) 55, (byte) 56, (byte) 57};

    static {
        allowedVersions.add("2a");
        allowedVersions.add(defaultVersion);
        allowedVersions.add("2b");
        int i = 0;
        for (int i2 = 0; i2 < decodingTable.length; i2++) {
            decodingTable[i2] = (byte) -1;
        }
        while (i < encodingTable.length) {
            decodingTable[encodingTable[i]] = (byte) i;
            i++;
        }
    }

    public static boolean checkPassword(String str, char[] cArr) {
        StringBuilder stringBuilder;
        if (str.length() != 60) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Bcrypt String length: ");
            stringBuilder2.append(str.length());
            stringBuilder2.append(", 60 required.");
            throw new DataLengthException(stringBuilder2.toString());
        } else if (str.charAt(0) == '$' && str.charAt(3) == '$' && str.charAt(6) == '$') {
            String substring = str.substring(1, 3);
            if (allowedVersions.contains(substring)) {
                try {
                    int parseInt = Integer.parseInt(str.substring(4, 6));
                    if (parseInt < 4 || parseInt > 31) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Invalid cost factor: ");
                        stringBuilder3.append(parseInt);
                        stringBuilder3.append(", 4 < cost < 31 expected.");
                        throw new IllegalArgumentException(stringBuilder3.toString());
                    } else if (cArr != null) {
                        return str.equals(generate(substring, cArr, decodeSaltString(str.substring(str.lastIndexOf(36) + 1, str.length() - 31)), parseInt));
                    } else {
                        throw new IllegalArgumentException("Missing password.");
                    }
                } catch (NumberFormatException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid cost factor: ");
                    stringBuilder.append(str.substring(4, 6));
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bcrypt version '");
            stringBuilder.append(str.substring(1, 3));
            stringBuilder.append("' is not supported by this implementation");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            throw new IllegalArgumentException("Invalid Bcrypt String format.");
        }
    }

    private static String createBcryptString(String str, byte[] bArr, byte[] bArr2, int i) {
        if (allowedVersions.contains(str)) {
            StringBuffer stringBuffer = new StringBuffer(60);
            stringBuffer.append('$');
            stringBuffer.append(str);
            stringBuffer.append('$');
            if (i < 10) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("0");
                stringBuilder.append(i);
                str = stringBuilder.toString();
            } else {
                str = Integer.toString(i);
            }
            stringBuffer.append(str);
            stringBuffer.append('$');
            stringBuffer.append(encodeData(bArr2));
            stringBuffer.append(encodeData(BCrypt.generate(bArr, bArr2, i)));
            return stringBuffer.toString();
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Version ");
        stringBuilder2.append(str);
        stringBuilder2.append(" is not accepted by this implementation.");
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    private static byte[] decodeSaltString(String str) {
        Object toCharArray = str.toCharArray();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(16);
        if (toCharArray.length == 22) {
            for (char c : toCharArray) {
                if (c > 'z' || c < '.' || (c > '9' && c < 'A')) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Salt string contains invalid character: ");
                    stringBuilder.append(c);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            Object obj = new char[24];
            System.arraycopy(toCharArray, 0, obj, 0, toCharArray.length);
            int length = obj.length;
            for (int i = 0; i < length; i += 4) {
                byte b = decodingTable[obj[i]];
                byte b2 = decodingTable[obj[i + 1]];
                byte b3 = decodingTable[obj[i + 2]];
                byte b4 = decodingTable[obj[i + 3]];
                byteArrayOutputStream.write((b << 2) | (b2 >> 4));
                byteArrayOutputStream.write((b2 << 4) | (b3 >> 2));
                byteArrayOutputStream.write((b3 << 6) | b4);
            }
            toCharArray = byteArrayOutputStream.toByteArray();
            Object obj2 = new byte[16];
            System.arraycopy(toCharArray, 0, obj2, 0, obj2.length);
            return obj2;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Invalid base64 salt length: ");
        stringBuilder2.append(toCharArray.length);
        stringBuilder2.append(" , 22 required.");
        throw new DataLengthException(stringBuilder2.toString());
    }

    private static String encodeData(byte[] bArr) {
        if (bArr.length == 24 || bArr.length == 16) {
            int i;
            if (bArr.length == 16) {
                Object obj = new byte[18];
                System.arraycopy(bArr, 0, obj, 0, bArr.length);
                bArr = obj;
                i = 1;
            } else {
                bArr[bArr.length - 1] = (byte) 0;
                i = (byte) 0;
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int length = bArr.length;
            for (int i2 = 0; i2 < length; i2 += 3) {
                int i3 = bArr[i2] & 255;
                int i4 = bArr[i2 + 1] & 255;
                int i5 = bArr[i2 + 2] & 255;
                byteArrayOutputStream.write(encodingTable[(i3 >>> 2) & 63]);
                byteArrayOutputStream.write(encodingTable[((i3 << 4) | (i4 >>> 4)) & 63]);
                byteArrayOutputStream.write(encodingTable[((i4 << 2) | (i5 >>> 6)) & 63]);
                byteArrayOutputStream.write(encodingTable[i5 & 63]);
            }
            String fromByteArray = Strings.fromByteArray(byteArrayOutputStream.toByteArray());
            return fromByteArray.substring(0, i == 1 ? 22 : fromByteArray.length() - 1);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid length: ");
        stringBuilder.append(bArr.length);
        stringBuilder.append(", 24 for key or 16 for salt expected");
        throw new DataLengthException(stringBuilder.toString());
    }

    public static String generate(String str, char[] cArr, byte[] bArr, int i) {
        if (!allowedVersions.contains(str)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Version ");
            stringBuilder.append(str);
            stringBuilder.append(" is not accepted by this implementation.");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (cArr == null) {
            throw new IllegalArgumentException("Password required.");
        } else if (bArr == null) {
            throw new IllegalArgumentException("Salt required.");
        } else if (bArr.length != 16) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("16 byte salt required: ");
            stringBuilder2.append(bArr.length);
            throw new DataLengthException(stringBuilder2.toString());
        } else if (i < 4 || i > 31) {
            throw new IllegalArgumentException("Invalid cost factor.");
        } else {
            byte[] toUTF8ByteArray = Strings.toUTF8ByteArray(cArr);
            int i2 = 72;
            if (toUTF8ByteArray.length < 72) {
                i2 = toUTF8ByteArray.length + 1;
            }
            byte[] bArr2 = new byte[i2];
            System.arraycopy(toUTF8ByteArray, 0, bArr2, 0, bArr2.length > toUTF8ByteArray.length ? toUTF8ByteArray.length : bArr2.length);
            Arrays.fill(toUTF8ByteArray, (byte) 0);
            str = createBcryptString(str, bArr2, bArr, i);
            Arrays.fill(bArr2, (byte) 0);
            return str;
        }
    }

    public static String generate(char[] cArr, byte[] bArr, int i) {
        return generate(defaultVersion, cArr, bArr, i);
    }
}
