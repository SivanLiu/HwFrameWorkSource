package org.bouncycastle.asn1.pkcs;

import java.math.BigInteger;
import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;

public class RSAPublicKey extends ASN1Object {
    private BigInteger modulus;
    private BigInteger publicExponent;

    public RSAPublicKey(BigInteger bigInteger, BigInteger bigInteger2) {
        this.modulus = bigInteger;
        this.publicExponent = bigInteger2;
    }

    private RSAPublicKey(ASN1Sequence aSN1Sequence) {
        if (aSN1Sequence.size() == 2) {
            Enumeration objects = aSN1Sequence.getObjects();
            this.modulus = ASN1Integer.getInstance(objects.nextElement()).getPositiveValue();
            this.publicExponent = ASN1Integer.getInstance(objects.nextElement()).getPositiveValue();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad sequence size: ");
        stringBuilder.append(aSN1Sequence.size());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static RSAPublicKey getInstance(Object obj) {
        return obj instanceof RSAPublicKey ? (RSAPublicKey) obj : obj != null ? new RSAPublicKey(ASN1Sequence.getInstance(obj)) : null;
    }

    public static RSAPublicKey getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public BigInteger getModulus() {
        return this.modulus;
    }

    public BigInteger getPublicExponent() {
        return this.publicExponent;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(new ASN1Integer(getModulus()));
        aSN1EncodableVector.add(new ASN1Integer(getPublicExponent()));
        return new DERSequence(aSN1EncodableVector);
    }
}
