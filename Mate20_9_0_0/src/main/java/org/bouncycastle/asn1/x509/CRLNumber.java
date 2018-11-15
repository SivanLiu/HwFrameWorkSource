package org.bouncycastle.asn1.x509;

import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;

public class CRLNumber extends ASN1Object {
    private BigInteger number;

    public CRLNumber(BigInteger bigInteger) {
        this.number = bigInteger;
    }

    public static CRLNumber getInstance(Object obj) {
        return obj instanceof CRLNumber ? (CRLNumber) obj : obj != null ? new CRLNumber(ASN1Integer.getInstance(obj).getValue()) : null;
    }

    public BigInteger getCRLNumber() {
        return this.number;
    }

    public ASN1Primitive toASN1Primitive() {
        return new ASN1Integer(this.number);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CRLNumber: ");
        stringBuilder.append(getCRLNumber());
        return stringBuilder.toString();
    }
}
