package org.bouncycastle.asn1;

import java.io.IOException;
import java.io.OutputStream;

public class DEROutputStream extends ASN1OutputStream {
    public DEROutputStream(OutputStream outputStream) {
        super(outputStream);
    }

    ASN1OutputStream getDERSubStream() {
        return this;
    }

    ASN1OutputStream getDLSubStream() {
        return this;
    }

    public void writeObject(ASN1Encodable aSN1Encodable) throws IOException {
        if (aSN1Encodable != null) {
            aSN1Encodable.toASN1Primitive().toDERObject().encode(this);
            return;
        }
        throw new IOException("null object detected");
    }
}
