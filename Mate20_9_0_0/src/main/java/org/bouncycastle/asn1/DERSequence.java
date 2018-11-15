package org.bouncycastle.asn1;

import java.io.IOException;
import java.util.Enumeration;

public class DERSequence extends ASN1Sequence {
    private int bodyLength = -1;

    public DERSequence(ASN1Encodable aSN1Encodable) {
        super(aSN1Encodable);
    }

    public DERSequence(ASN1EncodableVector aSN1EncodableVector) {
        super(aSN1EncodableVector);
    }

    public DERSequence(ASN1Encodable[] aSN1EncodableArr) {
        super(aSN1EncodableArr);
    }

    private int getBodyLength() throws IOException {
        if (this.bodyLength < 0) {
            int i = 0;
            Enumeration objects = getObjects();
            while (objects.hasMoreElements()) {
                i += ((ASN1Encodable) objects.nextElement()).toASN1Primitive().toDERObject().encodedLength();
            }
            this.bodyLength = i;
        }
        return this.bodyLength;
    }

    void encode(ASN1OutputStream aSN1OutputStream) throws IOException {
        ASN1OutputStream dERSubStream = aSN1OutputStream.getDERSubStream();
        int bodyLength = getBodyLength();
        aSN1OutputStream.write(48);
        aSN1OutputStream.writeLength(bodyLength);
        Enumeration objects = getObjects();
        while (objects.hasMoreElements()) {
            dERSubStream.writeObject((ASN1Encodable) objects.nextElement());
        }
    }

    int encodedLength() throws IOException {
        int bodyLength = getBodyLength();
        return (1 + StreamUtil.calculateBodyLength(bodyLength)) + bodyLength;
    }
}
