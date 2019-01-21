package com.android.org.bouncycastle.asn1.x9;

import com.android.org.bouncycastle.asn1.ASN1Integer;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import java.math.BigInteger;

public class DHPublicKey extends ASN1Object {
    private ASN1Integer y;

    public static DHPublicKey getInstance(ASN1TaggedObject obj, boolean explicit) {
        return getInstance(ASN1Integer.getInstance(obj, explicit));
    }

    public static DHPublicKey getInstance(Object obj) {
        if (obj == null || (obj instanceof DHPublicKey)) {
            return (DHPublicKey) obj;
        }
        if (obj instanceof ASN1Integer) {
            return new DHPublicKey((ASN1Integer) obj);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid DHPublicKey: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private DHPublicKey(ASN1Integer y) {
        if (y != null) {
            this.y = y;
            return;
        }
        throw new IllegalArgumentException("'y' cannot be null");
    }

    public DHPublicKey(BigInteger y) {
        if (y != null) {
            this.y = new ASN1Integer(y);
            return;
        }
        throw new IllegalArgumentException("'y' cannot be null");
    }

    public BigInteger getY() {
        return this.y.getPositiveValue();
    }

    public ASN1Primitive toASN1Primitive() {
        return this.y;
    }
}
