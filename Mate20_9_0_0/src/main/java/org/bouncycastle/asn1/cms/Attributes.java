package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DLSet;

public class Attributes extends ASN1Object {
    private ASN1Set attributes;

    public Attributes(ASN1EncodableVector aSN1EncodableVector) {
        this.attributes = new DLSet(aSN1EncodableVector);
    }

    private Attributes(ASN1Set aSN1Set) {
        this.attributes = aSN1Set;
    }

    public static Attributes getInstance(Object obj) {
        return obj instanceof Attributes ? (Attributes) obj : obj != null ? new Attributes(ASN1Set.getInstance(obj)) : null;
    }

    public Attribute[] getAttributes() {
        Attribute[] attributeArr = new Attribute[this.attributes.size()];
        for (int i = 0; i != attributeArr.length; i++) {
            attributeArr[i] = Attribute.getInstance(this.attributes.getObjectAt(i));
        }
        return attributeArr;
    }

    public ASN1Primitive toASN1Primitive() {
        return this.attributes;
    }
}
