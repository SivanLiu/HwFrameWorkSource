package org.bouncycastle.crypto.tls;

public class ClientAuthenticationType {
    public static final short anonymous = (short) 0;
    public static final short certificate_based = (short) 1;
    public static final short psk = (short) 2;
}
