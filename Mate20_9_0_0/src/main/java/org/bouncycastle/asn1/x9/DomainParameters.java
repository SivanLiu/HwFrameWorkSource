package org.bouncycastle.asn1.x9;

import java.math.BigInteger;
import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;

public class DomainParameters extends ASN1Object {
    private final ASN1Integer g;
    private final ASN1Integer j;
    private final ASN1Integer p;
    private final ASN1Integer q;
    private final ValidationParams validationParams;

    public DomainParameters(BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3, BigInteger bigInteger4, ValidationParams validationParams) {
        if (bigInteger == null) {
            throw new IllegalArgumentException("'p' cannot be null");
        } else if (bigInteger2 == null) {
            throw new IllegalArgumentException("'g' cannot be null");
        } else if (bigInteger3 != null) {
            this.p = new ASN1Integer(bigInteger);
            this.g = new ASN1Integer(bigInteger2);
            this.q = new ASN1Integer(bigInteger3);
            this.j = bigInteger4 != null ? new ASN1Integer(bigInteger4) : null;
            this.validationParams = validationParams;
        } else {
            throw new IllegalArgumentException("'q' cannot be null");
        }
    }

    private DomainParameters(ASN1Sequence aSN1Sequence) {
        if (aSN1Sequence.size() < 3 || aSN1Sequence.size() > 5) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad sequence size: ");
            stringBuilder.append(aSN1Sequence.size());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        Enumeration objects = aSN1Sequence.getObjects();
        this.p = ASN1Integer.getInstance(objects.nextElement());
        this.g = ASN1Integer.getInstance(objects.nextElement());
        this.q = ASN1Integer.getInstance(objects.nextElement());
        ASN1Encodable next = getNext(objects);
        if (next == null || !(next instanceof ASN1Integer)) {
            this.j = null;
        } else {
            this.j = ASN1Integer.getInstance(next);
            next = getNext(objects);
        }
        if (next != null) {
            this.validationParams = ValidationParams.getInstance(next.toASN1Primitive());
        } else {
            this.validationParams = null;
        }
    }

    public static DomainParameters getInstance(Object obj) {
        return obj instanceof DomainParameters ? (DomainParameters) obj : obj != null ? new DomainParameters(ASN1Sequence.getInstance(obj)) : null;
    }

    public static DomainParameters getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    private static ASN1Encodable getNext(Enumeration enumeration) {
        return enumeration.hasMoreElements() ? (ASN1Encodable) enumeration.nextElement() : null;
    }

    public BigInteger getG() {
        return this.g.getPositiveValue();
    }

    public BigInteger getJ() {
        return this.j == null ? null : this.j.getPositiveValue();
    }

    public BigInteger getP() {
        return this.p.getPositiveValue();
    }

    public BigInteger getQ() {
        return this.q.getPositiveValue();
    }

    public ValidationParams getValidationParams() {
        return this.validationParams;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.p);
        aSN1EncodableVector.add(this.g);
        aSN1EncodableVector.add(this.q);
        if (this.j != null) {
            aSN1EncodableVector.add(this.j);
        }
        if (this.validationParams != null) {
            aSN1EncodableVector.add(this.validationParams);
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
