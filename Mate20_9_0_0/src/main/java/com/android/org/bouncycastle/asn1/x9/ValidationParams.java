package com.android.org.bouncycastle.asn1.x9;

import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.org.bouncycastle.asn1.DERBitString;
import com.android.org.bouncycastle.asn1.DERSequence;
import java.math.BigInteger;

public class ValidationParams extends ASN1Object {
    private ASN1Integer pgenCounter;
    private DERBitString seed;

    public static ValidationParams getInstance(ASN1TaggedObject obj, boolean explicit) {
        return getInstance(ASN1Sequence.getInstance(obj, explicit));
    }

    public static ValidationParams getInstance(Object obj) {
        if (obj instanceof ValidationParams) {
            return (ValidationParams) obj;
        }
        if (obj != null) {
            return new ValidationParams(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    public ValidationParams(byte[] seed, int pgenCounter) {
        if (seed != null) {
            this.seed = new DERBitString(seed);
            this.pgenCounter = new ASN1Integer((long) pgenCounter);
            return;
        }
        throw new IllegalArgumentException("'seed' cannot be null");
    }

    public ValidationParams(DERBitString seed, ASN1Integer pgenCounter) {
        if (seed == null) {
            throw new IllegalArgumentException("'seed' cannot be null");
        } else if (pgenCounter != null) {
            this.seed = seed;
            this.pgenCounter = pgenCounter;
        } else {
            throw new IllegalArgumentException("'pgenCounter' cannot be null");
        }
    }

    private ValidationParams(ASN1Sequence seq) {
        if (seq.size() == 2) {
            this.seed = DERBitString.getInstance(seq.getObjectAt(0));
            this.pgenCounter = ASN1Integer.getInstance(seq.getObjectAt(1));
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad sequence size: ");
        stringBuilder.append(seq.size());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public byte[] getSeed() {
        return this.seed.getBytes();
    }

    public BigInteger getPgenCounter() {
        return this.pgenCounter.getPositiveValue();
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(this.seed);
        v.add(this.pgenCounter);
        return new DERSequence(v);
    }
}
