package org.bouncycastle.asn1.x509;

import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.x500.X500Name;

public class TBSCertificate extends ASN1Object {
    Time endDate;
    Extensions extensions;
    X500Name issuer;
    DERBitString issuerUniqueId;
    ASN1Sequence seq;
    ASN1Integer serialNumber;
    AlgorithmIdentifier signature;
    Time startDate;
    X500Name subject;
    SubjectPublicKeyInfo subjectPublicKeyInfo;
    DERBitString subjectUniqueId;
    ASN1Integer version;

    private TBSCertificate(ASN1Sequence aSN1Sequence) {
        int i;
        int i2;
        this.seq = aSN1Sequence;
        if (aSN1Sequence.getObjectAt(0) instanceof ASN1TaggedObject) {
            this.version = ASN1Integer.getInstance((ASN1TaggedObject) aSN1Sequence.getObjectAt(0), true);
            i = 0;
        } else {
            this.version = new ASN1Integer(0);
            i = -1;
        }
        boolean z;
        if (this.version.getValue().equals(BigInteger.valueOf(0))) {
            i2 = 0;
            z = true;
        } else if (this.version.getValue().equals(BigInteger.valueOf(1))) {
            z = false;
            i2 = true;
        } else if (this.version.getValue().equals(BigInteger.valueOf(2))) {
            z = false;
            i2 = z;
        } else {
            throw new IllegalArgumentException("version number not recognised");
        }
        this.serialNumber = ASN1Integer.getInstance(aSN1Sequence.getObjectAt(i + 1));
        this.signature = AlgorithmIdentifier.getInstance(aSN1Sequence.getObjectAt(i + 2));
        this.issuer = X500Name.getInstance(aSN1Sequence.getObjectAt(i + 3));
        ASN1Sequence aSN1Sequence2 = (ASN1Sequence) aSN1Sequence.getObjectAt(i + 4);
        this.startDate = Time.getInstance(aSN1Sequence2.getObjectAt(0));
        this.endDate = Time.getInstance(aSN1Sequence2.getObjectAt(1));
        this.subject = X500Name.getInstance(aSN1Sequence.getObjectAt(i + 5));
        i += 6;
        this.subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(aSN1Sequence.getObjectAt(i));
        int size = (aSN1Sequence.size() - i) - 1;
        if (size == 0 || !z) {
            while (size > 0) {
                ASN1TaggedObject aSN1TaggedObject = (ASN1TaggedObject) aSN1Sequence.getObjectAt(i + size);
                switch (aSN1TaggedObject.getTagNo()) {
                    case 1:
                        this.issuerUniqueId = DERBitString.getInstance(aSN1TaggedObject, false);
                        break;
                    case 2:
                        this.subjectUniqueId = DERBitString.getInstance(aSN1TaggedObject, false);
                        break;
                    case 3:
                        if (i2 == 0) {
                            this.extensions = Extensions.getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, true));
                            break;
                        }
                        throw new IllegalArgumentException("version 2 certificate cannot contain extensions");
                    default:
                        break;
                }
                size--;
            }
            return;
        }
        throw new IllegalArgumentException("version 1 certificate contains extra data");
    }

    public static TBSCertificate getInstance(Object obj) {
        return obj instanceof TBSCertificate ? (TBSCertificate) obj : obj != null ? new TBSCertificate(ASN1Sequence.getInstance(obj)) : null;
    }

    public static TBSCertificate getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public Time getEndDate() {
        return this.endDate;
    }

    public Extensions getExtensions() {
        return this.extensions;
    }

    public X500Name getIssuer() {
        return this.issuer;
    }

    public DERBitString getIssuerUniqueId() {
        return this.issuerUniqueId;
    }

    public ASN1Integer getSerialNumber() {
        return this.serialNumber;
    }

    public AlgorithmIdentifier getSignature() {
        return this.signature;
    }

    public Time getStartDate() {
        return this.startDate;
    }

    public X500Name getSubject() {
        return this.subject;
    }

    public SubjectPublicKeyInfo getSubjectPublicKeyInfo() {
        return this.subjectPublicKeyInfo;
    }

    public DERBitString getSubjectUniqueId() {
        return this.subjectUniqueId;
    }

    public ASN1Integer getVersion() {
        return this.version;
    }

    public int getVersionNumber() {
        return this.version.getValue().intValue() + 1;
    }

    public ASN1Primitive toASN1Primitive() {
        return this.seq;
    }
}
