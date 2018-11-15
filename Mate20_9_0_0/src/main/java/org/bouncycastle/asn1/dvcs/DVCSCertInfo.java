package org.bouncycastle.asn1.dvcs;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.cmp.PKIStatusInfo;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.PolicyInformation;

public class DVCSCertInfo extends ASN1Object {
    private static final int DEFAULT_VERSION = 1;
    private static final int TAG_CERTS = 3;
    private static final int TAG_DV_STATUS = 0;
    private static final int TAG_POLICY = 1;
    private static final int TAG_REQ_SIGNATURE = 2;
    private ASN1Sequence certs;
    private DVCSRequestInformation dvReqInfo;
    private PKIStatusInfo dvStatus;
    private Extensions extensions;
    private DigestInfo messageImprint;
    private PolicyInformation policy;
    private ASN1Set reqSignature;
    private DVCSTime responseTime;
    private ASN1Integer serialNumber;
    private int version = 1;

    /* JADX WARNING: Removed duplicated region for block: B:13:0x0054  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private DVCSCertInfo(ASN1Sequence aSN1Sequence) {
        int i;
        Object objectAt;
        int i2;
        int i3;
        ASN1Encodable objectAt2 = aSN1Sequence.getObjectAt(0);
        try {
            this.version = ASN1Integer.getInstance(objectAt2).getValue().intValue();
            i = 2;
            try {
                objectAt = aSN1Sequence.getObjectAt(1);
            } catch (IllegalArgumentException e) {
            }
        } catch (IllegalArgumentException e2) {
            i = 1;
            objectAt = objectAt2;
            this.dvReqInfo = DVCSRequestInformation.getInstance(objectAt);
            i2 = i + 1;
            this.messageImprint = DigestInfo.getInstance(aSN1Sequence.getObjectAt(i));
            i3 = i2 + 1;
            this.serialNumber = ASN1Integer.getInstance(aSN1Sequence.getObjectAt(i2));
            i2 = i3 + 1;
            this.responseTime = DVCSTime.getInstance(aSN1Sequence.getObjectAt(i3));
            while (i2 < aSN1Sequence.size()) {
            }
        }
        this.dvReqInfo = DVCSRequestInformation.getInstance(objectAt);
        i2 = i + 1;
        this.messageImprint = DigestInfo.getInstance(aSN1Sequence.getObjectAt(i));
        i3 = i2 + 1;
        this.serialNumber = ASN1Integer.getInstance(aSN1Sequence.getObjectAt(i2));
        i2 = i3 + 1;
        this.responseTime = DVCSTime.getInstance(aSN1Sequence.getObjectAt(i3));
        while (i2 < aSN1Sequence.size()) {
            i3 = i2 + 1;
            ASN1Encodable objectAt3 = aSN1Sequence.getObjectAt(i2);
            if (objectAt3 instanceof ASN1TaggedObject) {
                ASN1TaggedObject instance = ASN1TaggedObject.getInstance(objectAt3);
                i = instance.getTagNo();
                switch (i) {
                    case 0:
                        this.dvStatus = PKIStatusInfo.getInstance(instance, false);
                        break;
                    case 1:
                        this.policy = PolicyInformation.getInstance(ASN1Sequence.getInstance(instance, false));
                        break;
                    case 2:
                        this.reqSignature = ASN1Set.getInstance(instance, false);
                        break;
                    case 3:
                        this.certs = ASN1Sequence.getInstance(instance, false);
                        break;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown tag encountered: ");
                        stringBuilder.append(i);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            try {
                this.extensions = Extensions.getInstance(objectAt3);
            } catch (IllegalArgumentException e3) {
            }
            i2 = i3;
        }
    }

    public DVCSCertInfo(DVCSRequestInformation dVCSRequestInformation, DigestInfo digestInfo, ASN1Integer aSN1Integer, DVCSTime dVCSTime) {
        this.dvReqInfo = dVCSRequestInformation;
        this.messageImprint = digestInfo;
        this.serialNumber = aSN1Integer;
        this.responseTime = dVCSTime;
    }

    public static DVCSCertInfo getInstance(Object obj) {
        return obj instanceof DVCSCertInfo ? (DVCSCertInfo) obj : obj != null ? new DVCSCertInfo(ASN1Sequence.getInstance(obj)) : null;
    }

    public static DVCSCertInfo getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    private void setDvReqInfo(DVCSRequestInformation dVCSRequestInformation) {
        this.dvReqInfo = dVCSRequestInformation;
    }

    private void setMessageImprint(DigestInfo digestInfo) {
        this.messageImprint = digestInfo;
    }

    private void setVersion(int i) {
        this.version = i;
    }

    public TargetEtcChain[] getCerts() {
        return this.certs != null ? TargetEtcChain.arrayFromSequence(this.certs) : null;
    }

    public DVCSRequestInformation getDvReqInfo() {
        return this.dvReqInfo;
    }

    public PKIStatusInfo getDvStatus() {
        return this.dvStatus;
    }

    public Extensions getExtensions() {
        return this.extensions;
    }

    public DigestInfo getMessageImprint() {
        return this.messageImprint;
    }

    public PolicyInformation getPolicy() {
        return this.policy;
    }

    public ASN1Set getReqSignature() {
        return this.reqSignature;
    }

    public DVCSTime getResponseTime() {
        return this.responseTime;
    }

    public ASN1Integer getSerialNumber() {
        return this.serialNumber;
    }

    public int getVersion() {
        return this.version;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.version != 1) {
            aSN1EncodableVector.add(new ASN1Integer((long) this.version));
        }
        aSN1EncodableVector.add(this.dvReqInfo);
        aSN1EncodableVector.add(this.messageImprint);
        aSN1EncodableVector.add(this.serialNumber);
        aSN1EncodableVector.add(this.responseTime);
        if (this.dvStatus != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 0, this.dvStatus));
        }
        if (this.policy != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 1, this.policy));
        }
        if (this.reqSignature != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 2, this.reqSignature));
        }
        if (this.certs != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 3, this.certs));
        }
        if (this.extensions != null) {
            aSN1EncodableVector.add(this.extensions);
        }
        return new DERSequence(aSN1EncodableVector);
    }

    public String toString() {
        StringBuilder stringBuilder;
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("DVCSCertInfo {\n");
        if (this.version != 1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("version: ");
            stringBuilder.append(this.version);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("dvReqInfo: ");
        stringBuilder.append(this.dvReqInfo);
        stringBuilder.append("\n");
        stringBuffer.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("messageImprint: ");
        stringBuilder.append(this.messageImprint);
        stringBuilder.append("\n");
        stringBuffer.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("serialNumber: ");
        stringBuilder.append(this.serialNumber);
        stringBuilder.append("\n");
        stringBuffer.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("responseTime: ");
        stringBuilder.append(this.responseTime);
        stringBuilder.append("\n");
        stringBuffer.append(stringBuilder.toString());
        if (this.dvStatus != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("dvStatus: ");
            stringBuilder.append(this.dvStatus);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        if (this.policy != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("policy: ");
            stringBuilder.append(this.policy);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        if (this.reqSignature != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("reqSignature: ");
            stringBuilder.append(this.reqSignature);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        if (this.certs != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("certs: ");
            stringBuilder.append(this.certs);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        if (this.extensions != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("extensions: ");
            stringBuilder.append(this.extensions);
            stringBuilder.append("\n");
            stringBuffer.append(stringBuilder.toString());
        }
        stringBuffer.append("}\n");
        return stringBuffer.toString();
    }
}
