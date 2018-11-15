package org.bouncycastle.crypto.tls;

public class FiniteFieldDHEGroup {
    public static final short ffdhe2432 = (short) 0;
    public static final short ffdhe3072 = (short) 1;
    public static final short ffdhe4096 = (short) 2;
    public static final short ffdhe6144 = (short) 3;
    public static final short ffdhe8192 = (short) 4;

    public static boolean isValid(short s) {
        return s >= (short) 0 && s <= (short) 4;
    }
}
