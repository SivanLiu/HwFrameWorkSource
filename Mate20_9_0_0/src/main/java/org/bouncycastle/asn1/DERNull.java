package org.bouncycastle.asn1;

import java.io.IOException;

public class DERNull extends ASN1Null {
    public static final DERNull INSTANCE = new DERNull();
    private static final byte[] zeroBytes = new byte[0];

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        aSN1OutputStream.writeEncoded(5, zeroBytes);
    }

    int encodedLength() {
        return 2;
    }

    boolean isConstructed() {
        return false;
    }
}
