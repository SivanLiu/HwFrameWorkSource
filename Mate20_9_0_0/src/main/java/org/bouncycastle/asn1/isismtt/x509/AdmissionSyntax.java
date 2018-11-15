package org.bouncycastle.asn1.isismtt.x509;

import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.GeneralName;

public class AdmissionSyntax extends ASN1Object {
    private GeneralName admissionAuthority;
    private ASN1Sequence contentsOfAdmissions;

    private AdmissionSyntax(ASN1Sequence aSN1Sequence) {
        Object objectAt;
        switch (aSN1Sequence.size()) {
            case 1:
                objectAt = aSN1Sequence.getObjectAt(0);
                break;
            case 2:
                this.admissionAuthority = GeneralName.getInstance(aSN1Sequence.getObjectAt(0));
                objectAt = aSN1Sequence.getObjectAt(1);
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Bad sequence size: ");
                stringBuilder.append(aSN1Sequence.size());
                throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.contentsOfAdmissions = ASN1Sequence.getInstance(objectAt);
    }

    public AdmissionSyntax(GeneralName generalName, ASN1Sequence aSN1Sequence) {
        this.admissionAuthority = generalName;
        this.contentsOfAdmissions = aSN1Sequence;
    }

    public static AdmissionSyntax getInstance(Object obj) {
        if (obj == null || (obj instanceof AdmissionSyntax)) {
            return (AdmissionSyntax) obj;
        }
        if (obj instanceof ASN1Sequence) {
            return new AdmissionSyntax((ASN1Sequence) obj);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("illegal object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public GeneralName getAdmissionAuthority() {
        return this.admissionAuthority;
    }

    public Admissions[] getContentsOfAdmissions() {
        Admissions[] admissionsArr = new Admissions[this.contentsOfAdmissions.size()];
        Enumeration objects = this.contentsOfAdmissions.getObjects();
        int i = 0;
        while (objects.hasMoreElements()) {
            int i2 = i + 1;
            admissionsArr[i] = Admissions.getInstance(objects.nextElement());
            i = i2;
        }
        return admissionsArr;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.admissionAuthority != null) {
            aSN1EncodableVector.add(this.admissionAuthority);
        }
        aSN1EncodableVector.add(this.contentsOfAdmissions);
        return new DERSequence(aSN1EncodableVector);
    }
}
