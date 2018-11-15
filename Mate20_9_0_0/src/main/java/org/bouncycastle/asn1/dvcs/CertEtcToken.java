package org.bouncycastle.asn1.dvcs;

import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.cmp.PKIStatusInfo;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.ess.ESSCertID;
import org.bouncycastle.asn1.ocsp.CertID;
import org.bouncycastle.asn1.ocsp.CertStatus;
import org.bouncycastle.asn1.ocsp.OCSPResponse;
import org.bouncycastle.asn1.smime.SMIMECapabilities;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.asn1.x509.Extension;

public class CertEtcToken extends ASN1Object implements ASN1Choice {
    public static final int TAG_ASSERTION = 3;
    public static final int TAG_CAPABILITIES = 8;
    public static final int TAG_CERTIFICATE = 0;
    public static final int TAG_CRL = 4;
    public static final int TAG_ESSCERTID = 1;
    public static final int TAG_OCSPCERTID = 6;
    public static final int TAG_OCSPCERTSTATUS = 5;
    public static final int TAG_OCSPRESPONSE = 7;
    public static final int TAG_PKISTATUS = 2;
    private static final boolean[] explicit = new boolean[]{false, true, false, true, false, true, false, false, true};
    private Extension extension;
    private int tagNo;
    private ASN1Encodable value;

    public CertEtcToken(int i, ASN1Encodable aSN1Encodable) {
        this.tagNo = i;
        this.value = aSN1Encodable;
    }

    private CertEtcToken(ASN1TaggedObject aSN1TaggedObject) {
        ASN1Encodable instance;
        this.tagNo = aSN1TaggedObject.getTagNo();
        switch (this.tagNo) {
            case 0:
                instance = Certificate.getInstance(aSN1TaggedObject, false);
                break;
            case 1:
                instance = ESSCertID.getInstance(aSN1TaggedObject.getObject());
                break;
            case 2:
                instance = PKIStatusInfo.getInstance(aSN1TaggedObject, false);
                break;
            case 3:
                instance = ContentInfo.getInstance(aSN1TaggedObject.getObject());
                break;
            case 4:
                instance = CertificateList.getInstance(aSN1TaggedObject, false);
                break;
            case 5:
                instance = CertStatus.getInstance(aSN1TaggedObject.getObject());
                break;
            case 6:
                instance = CertID.getInstance(aSN1TaggedObject, false);
                break;
            case 7:
                instance = OCSPResponse.getInstance(aSN1TaggedObject, false);
                break;
            case 8:
                instance = SMIMECapabilities.getInstance(aSN1TaggedObject.getObject());
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown tag: ");
                stringBuilder.append(this.tagNo);
                throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.value = instance;
    }

    public CertEtcToken(Extension extension) {
        this.tagNo = -1;
        this.extension = extension;
    }

    public static CertEtcToken[] arrayFromSequence(ASN1Sequence aSN1Sequence) {
        CertEtcToken[] certEtcTokenArr = new CertEtcToken[aSN1Sequence.size()];
        for (int i = 0; i != certEtcTokenArr.length; i++) {
            certEtcTokenArr[i] = getInstance(aSN1Sequence.getObjectAt(i));
        }
        return certEtcTokenArr;
    }

    public static CertEtcToken getInstance(Object obj) {
        return obj instanceof CertEtcToken ? (CertEtcToken) obj : obj instanceof ASN1TaggedObject ? new CertEtcToken((ASN1TaggedObject) obj) : obj != null ? new CertEtcToken(Extension.getInstance(obj)) : null;
    }

    public Extension getExtension() {
        return this.extension;
    }

    public int getTagNo() {
        return this.tagNo;
    }

    public ASN1Encodable getValue() {
        return this.value;
    }

    public ASN1Primitive toASN1Primitive() {
        return this.extension == null ? new DERTaggedObject(explicit[this.tagNo], this.tagNo, this.value) : this.extension.toASN1Primitive();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CertEtcToken {\n");
        stringBuilder.append(this.value);
        stringBuilder.append("}\n");
        return stringBuilder.toString();
    }
}
