package org.bouncycastle.crypto.tls;

public class NameType {
    public static final short host_name = (short) 0;

    public static boolean isValid(short s) {
        return s == (short) 0;
    }
}
