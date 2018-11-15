package org.bouncycastle.asn1.ess;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.PolicyInformation;

public class SigningCertificateV2 extends ASN1Object {
    ASN1Sequence certs;
    ASN1Sequence policies;

    private SigningCertificateV2(ASN1Sequence aSN1Sequence) {
        if (aSN1Sequence.size() < 1 || aSN1Sequence.size() > 2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad sequence size: ");
            stringBuilder.append(aSN1Sequence.size());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.certs = ASN1Sequence.getInstance(aSN1Sequence.getObjectAt(0));
        if (aSN1Sequence.size() > 1) {
            this.policies = ASN1Sequence.getInstance(aSN1Sequence.getObjectAt(1));
        }
    }

    public SigningCertificateV2(ESSCertIDv2 eSSCertIDv2) {
        this.certs = new DERSequence((ASN1Encodable) eSSCertIDv2);
    }

    public SigningCertificateV2(ESSCertIDv2[] eSSCertIDv2Arr) {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        for (ASN1Encodable add : eSSCertIDv2Arr) {
            aSN1EncodableVector.add(add);
        }
        this.certs = new DERSequence(aSN1EncodableVector);
    }

    public SigningCertificateV2(ESSCertIDv2[] eSSCertIDv2Arr, PolicyInformation[] policyInformationArr) {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        int i = 0;
        for (ASN1Encodable add : eSSCertIDv2Arr) {
            aSN1EncodableVector.add(add);
        }
        this.certs = new DERSequence(aSN1EncodableVector);
        if (policyInformationArr != null) {
            ASN1EncodableVector aSN1EncodableVector2 = new ASN1EncodableVector();
            while (i < policyInformationArr.length) {
                aSN1EncodableVector2.add(policyInformationArr[i]);
                i++;
            }
            this.policies = new DERSequence(aSN1EncodableVector2);
        }
    }

    public static SigningCertificateV2 getInstance(Object obj) {
        return (obj == null || (obj instanceof SigningCertificateV2)) ? (SigningCertificateV2) obj : obj instanceof ASN1Sequence ? new SigningCertificateV2((ASN1Sequence) obj) : null;
    }

    public ESSCertIDv2[] getCerts() {
        ESSCertIDv2[] eSSCertIDv2Arr = new ESSCertIDv2[this.certs.size()];
        for (int i = 0; i != this.certs.size(); i++) {
            eSSCertIDv2Arr[i] = ESSCertIDv2.getInstance(this.certs.getObjectAt(i));
        }
        return eSSCertIDv2Arr;
    }

    public PolicyInformation[] getPolicies() {
        if (this.policies == null) {
            return null;
        }
        PolicyInformation[] policyInformationArr = new PolicyInformation[this.policies.size()];
        for (int i = 0; i != this.policies.size(); i++) {
            policyInformationArr[i] = PolicyInformation.getInstance(this.policies.getObjectAt(i));
        }
        return policyInformationArr;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.certs);
        if (this.policies != null) {
            aSN1EncodableVector.add(this.policies);
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
