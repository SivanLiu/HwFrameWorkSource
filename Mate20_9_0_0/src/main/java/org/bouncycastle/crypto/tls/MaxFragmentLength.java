package org.bouncycastle.crypto.tls;

public class MaxFragmentLength {
    public static final short pow2_10 = (short) 2;
    public static final short pow2_11 = (short) 3;
    public static final short pow2_12 = (short) 4;
    public static final short pow2_9 = (short) 1;

    public static boolean isValid(short s) {
        return s >= (short) 1 && s <= (short) 4;
    }
}
