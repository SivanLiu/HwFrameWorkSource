package org.bouncycastle.asn1.isismtt.x509;

import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.GeneralName;

public class Admissions extends ASN1Object {
    private GeneralName admissionAuthority;
    private NamingAuthority namingAuthority;
    private ASN1Sequence professionInfos;

    private Admissions(ASN1Sequence aSN1Sequence) {
        StringBuilder stringBuilder;
        if (aSN1Sequence.size() <= 3) {
            ASN1TaggedObject aSN1TaggedObject;
            Enumeration objects = aSN1Sequence.getObjects();
            Object obj = (ASN1Encodable) objects.nextElement();
            if (obj instanceof ASN1TaggedObject) {
                aSN1TaggedObject = (ASN1TaggedObject) obj;
                switch (aSN1TaggedObject.getTagNo()) {
                    case 0:
                        this.admissionAuthority = GeneralName.getInstance(aSN1TaggedObject, true);
                        break;
                    case 1:
                        this.namingAuthority = NamingAuthority.getInstance(aSN1TaggedObject, true);
                        break;
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Bad tag number: ");
                        stringBuilder.append(aSN1TaggedObject.getTagNo());
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
                obj = (ASN1Encodable) objects.nextElement();
            }
            if (obj instanceof ASN1TaggedObject) {
                aSN1TaggedObject = (ASN1TaggedObject) obj;
                if (aSN1TaggedObject.getTagNo() == 1) {
                    this.namingAuthority = NamingAuthority.getInstance(aSN1TaggedObject, true);
                    obj = (ASN1Encodable) objects.nextElement();
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad tag number: ");
                    stringBuilder.append(aSN1TaggedObject.getTagNo());
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            this.professionInfos = ASN1Sequence.getInstance(obj);
            if (objects.hasMoreElements()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Bad object encountered: ");
                stringBuilder.append(objects.nextElement().getClass());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Bad sequence size: ");
        stringBuilder.append(aSN1Sequence.size());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public Admissions(GeneralName generalName, NamingAuthority namingAuthority, ProfessionInfo[] professionInfoArr) {
        this.admissionAuthority = generalName;
        this.namingAuthority = namingAuthority;
        this.professionInfos = new DERSequence((ASN1Encodable[]) professionInfoArr);
    }

    public static Admissions getInstance(Object obj) {
        if (obj == null || (obj instanceof Admissions)) {
            return (Admissions) obj;
        }
        if (obj instanceof ASN1Sequence) {
            return new Admissions((ASN1Sequence) obj);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("illegal object in getInstance: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public GeneralName getAdmissionAuthority() {
        return this.admissionAuthority;
    }

    public NamingAuthority getNamingAuthority() {
        return this.namingAuthority;
    }

    public ProfessionInfo[] getProfessionInfos() {
        ProfessionInfo[] professionInfoArr = new ProfessionInfo[this.professionInfos.size()];
        Enumeration objects = this.professionInfos.getObjects();
        int i = 0;
        while (objects.hasMoreElements()) {
            int i2 = i + 1;
            professionInfoArr[i] = ProfessionInfo.getInstance(objects.nextElement());
            i = i2;
        }
        return professionInfoArr;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.admissionAuthority != null) {
            aSN1EncodableVector.add(new DERTaggedObject(true, 0, this.admissionAuthority));
        }
        if (this.namingAuthority != null) {
            aSN1EncodableVector.add(new DERTaggedObject(true, 1, this.namingAuthority));
        }
        aSN1EncodableVector.add(this.professionInfos);
        return new DERSequence(aSN1EncodableVector);
    }
}
