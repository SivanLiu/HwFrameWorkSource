package org.bouncycastle.asn1.esf;

import org.bouncycastle.asn1.ASN1Null;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERNull;

public class SignaturePolicyIdentifier extends ASN1Object {
    private boolean isSignaturePolicyImplied = true;
    private SignaturePolicyId signaturePolicyId;

    public SignaturePolicyIdentifier(SignaturePolicyId signaturePolicyId) {
        this.signaturePolicyId = signaturePolicyId;
    }

    public static SignaturePolicyIdentifier getInstance(Object obj) {
        return obj instanceof SignaturePolicyIdentifier ? (SignaturePolicyIdentifier) obj : ((obj instanceof ASN1Null) || ASN1Object.hasEncodedTagValue(obj, 5)) ? new SignaturePolicyIdentifier() : obj != null ? new SignaturePolicyIdentifier(SignaturePolicyId.getInstance(obj)) : null;
    }

    public SignaturePolicyId getSignaturePolicyId() {
        return this.signaturePolicyId;
    }

    public boolean isSignaturePolicyImplied() {
        return this.isSignaturePolicyImplied;
    }

    public ASN1Primitive toASN1Primitive() {
        return this.isSignaturePolicyImplied ? DERNull.INSTANCE : this.signaturePolicyId.toASN1Primitive();
    }
}
