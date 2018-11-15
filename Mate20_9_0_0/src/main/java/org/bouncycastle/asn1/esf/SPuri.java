package org.bouncycastle.asn1.esf;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERIA5String;

public class SPuri {
    private DERIA5String uri;

    public SPuri(DERIA5String dERIA5String) {
        this.uri = dERIA5String;
    }

    public static SPuri getInstance(Object obj) {
        return obj instanceof SPuri ? (SPuri) obj : obj instanceof DERIA5String ? new SPuri(DERIA5String.getInstance(obj)) : null;
    }

    public DERIA5String getUri() {
        return this.uri;
    }

    public ASN1Primitive toASN1Primitive() {
        return this.uri.toASN1Primitive();
    }
}
