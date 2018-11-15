package org.bouncycastle.crypto.tls;

public class CertChainType {
    public static final short individual_certs = (short) 0;
    public static final short pkipath = (short) 1;

    public static boolean isValid(short s) {
        return s >= (short) 0 && s <= (short) 1;
    }
}
