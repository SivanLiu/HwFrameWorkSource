package org.bouncycastle.crypto.util;

import java.io.IOException;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.util.Arrays;

class DerUtil {
    DerUtil() {
    }

    static ASN1OctetString getOctetString(byte[] bArr) {
        return bArr == null ? new DEROctetString(new byte[0]) : new DEROctetString(Arrays.clone(bArr));
    }

    static byte[] toByteArray(ASN1Primitive aSN1Primitive) {
        try {
            return aSN1Primitive.getEncoded();
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot get encoding: ");
            stringBuilder.append(e.getMessage());
            throw new IllegalStateException(stringBuilder.toString()) {
                public Throwable getCause() {
                    return e;
                }
            };
        }
    }
}
