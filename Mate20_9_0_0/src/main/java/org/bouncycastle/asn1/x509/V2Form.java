package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public class V2Form extends ASN1Object {
    IssuerSerial baseCertificateID;
    GeneralNames issuerName;
    ObjectDigestInfo objectDigestInfo;

    public V2Form(ASN1Sequence aSN1Sequence) {
        if (aSN1Sequence.size() <= 3) {
            int i;
            if (aSN1Sequence.getObjectAt(0) instanceof ASN1TaggedObject) {
                i = 0;
            } else {
                this.issuerName = GeneralNames.getInstance(aSN1Sequence.getObjectAt(0));
                i = 1;
            }
            while (i != aSN1Sequence.size()) {
                ASN1TaggedObject instance = ASN1TaggedObject.getInstance(aSN1Sequence.getObjectAt(i));
                if (instance.getTagNo() == 0) {
                    this.baseCertificateID = IssuerSerial.getInstance(instance, false);
                } else if (instance.getTagNo() == 1) {
                    this.objectDigestInfo = ObjectDigestInfo.getInstance(instance, false);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad tag number: ");
                    stringBuilder.append(instance.getTagNo());
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
                i++;
            }
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Bad sequence size: ");
        stringBuilder2.append(aSN1Sequence.size());
        throw new IllegalArgumentException(stringBuilder2.toString());
    }

    public V2Form(GeneralNames generalNames) {
        this(generalNames, null, null);
    }

    public V2Form(GeneralNames generalNames, IssuerSerial issuerSerial) {
        this(generalNames, issuerSerial, null);
    }

    public V2Form(GeneralNames generalNames, IssuerSerial issuerSerial, ObjectDigestInfo objectDigestInfo) {
        this.issuerName = generalNames;
        this.baseCertificateID = issuerSerial;
        this.objectDigestInfo = objectDigestInfo;
    }

    public V2Form(GeneralNames generalNames, ObjectDigestInfo objectDigestInfo) {
        this(generalNames, null, objectDigestInfo);
    }

    public static V2Form getInstance(Object obj) {
        return obj instanceof V2Form ? (V2Form) obj : obj != null ? new V2Form(ASN1Sequence.getInstance(obj)) : null;
    }

    public static V2Form getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public IssuerSerial getBaseCertificateID() {
        return this.baseCertificateID;
    }

    public GeneralNames getIssuerName() {
        return this.issuerName;
    }

    public ObjectDigestInfo getObjectDigestInfo() {
        return this.objectDigestInfo;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.issuerName != null) {
            aSN1EncodableVector.add(this.issuerName);
        }
        if (this.baseCertificateID != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 0, this.baseCertificateID));
        }
        if (this.objectDigestInfo != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 1, this.objectDigestInfo));
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
