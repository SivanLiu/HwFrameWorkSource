package org.bouncycastle.crypto.tls;

public class HashAlgorithm {
    public static final short md5 = (short) 1;
    public static final short none = (short) 0;
    public static final short sha1 = (short) 2;
    public static final short sha224 = (short) 3;
    public static final short sha256 = (short) 4;
    public static final short sha384 = (short) 5;
    public static final short sha512 = (short) 6;

    public static String getName(short s) {
        switch (s) {
            case (short) 0:
                return "none";
            case (short) 1:
                return "md5";
            case (short) 2:
                return "sha1";
            case (short) 3:
                return "sha224";
            case (short) 4:
                return "sha256";
            case (short) 5:
                return "sha384";
            case (short) 6:
                return "sha512";
            default:
                return "UNKNOWN";
        }
    }

    public static String getText(short s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getName(s));
        stringBuilder.append("(");
        stringBuilder.append(s);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    public static boolean isPrivate(short s) {
        return (short) 224 <= s && s <= (short) 255;
    }
}
