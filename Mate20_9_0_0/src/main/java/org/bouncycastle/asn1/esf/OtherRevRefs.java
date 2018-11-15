package org.bouncycastle.asn1.esf;

import java.io.IOException;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;

public class OtherRevRefs extends ASN1Object {
    private ASN1ObjectIdentifier otherRevRefType;
    private ASN1Encodable otherRevRefs;

    public OtherRevRefs(ASN1ObjectIdentifier aSN1ObjectIdentifier, ASN1Encodable aSN1Encodable) {
        this.otherRevRefType = aSN1ObjectIdentifier;
        this.otherRevRefs = aSN1Encodable;
    }

    private OtherRevRefs(ASN1Sequence aSN1Sequence) {
        if (aSN1Sequence.size() == 2) {
            this.otherRevRefType = new ASN1ObjectIdentifier(((ASN1ObjectIdentifier) aSN1Sequence.getObjectAt(0)).getId());
            try {
                this.otherRevRefs = ASN1Primitive.fromByteArray(aSN1Sequence.getObjectAt(1).toASN1Primitive().getEncoded(ASN1Encoding.DER));
                return;
            } catch (IOException e) {
                throw new IllegalStateException();
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad sequence size: ");
        stringBuilder.append(aSN1Sequence.size());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static OtherRevRefs getInstance(Object obj) {
        return obj instanceof OtherRevRefs ? (OtherRevRefs) obj : obj != null ? new OtherRevRefs(ASN1Sequence.getInstance(obj)) : null;
    }

    public ASN1ObjectIdentifier getOtherRevRefType() {
        return this.otherRevRefType;
    }

    public ASN1Encodable getOtherRevRefs() {
        return this.otherRevRefs;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.otherRevRefType);
        aSN1EncodableVector.add(this.otherRevRefs);
        return new DERSequence(aSN1EncodableVector);
    }
}
