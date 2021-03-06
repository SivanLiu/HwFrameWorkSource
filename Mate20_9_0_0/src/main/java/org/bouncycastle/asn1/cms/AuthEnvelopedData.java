package org.bouncycastle.asn1.cms;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.BERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public class AuthEnvelopedData extends ASN1Object {
    private ASN1Set authAttrs;
    private EncryptedContentInfo authEncryptedContentInfo;
    private ASN1OctetString mac;
    private OriginatorInfo originatorInfo;
    private ASN1Set recipientInfos;
    private ASN1Set unauthAttrs;
    private ASN1Integer version;

    private AuthEnvelopedData(ASN1Sequence aSN1Sequence) {
        this.version = (ASN1Integer) aSN1Sequence.getObjectAt(0).toASN1Primitive();
        if (this.version.getValue().intValue() == 0) {
            int i;
            Object toASN1Primitive;
            ASN1Primitive aSN1Primitive;
            ASN1Primitive toASN1Primitive2 = aSN1Sequence.getObjectAt(1).toASN1Primitive();
            if (toASN1Primitive2 instanceof ASN1TaggedObject) {
                this.originatorInfo = OriginatorInfo.getInstance((ASN1TaggedObject) toASN1Primitive2, false);
                i = 3;
                toASN1Primitive = aSN1Sequence.getObjectAt(2).toASN1Primitive();
            } else {
                aSN1Primitive = toASN1Primitive2;
                i = 2;
                toASN1Primitive = aSN1Primitive;
            }
            this.recipientInfos = ASN1Set.getInstance(toASN1Primitive);
            if (this.recipientInfos.size() != 0) {
                int i2 = i + 1;
                this.authEncryptedContentInfo = EncryptedContentInfo.getInstance(aSN1Sequence.getObjectAt(i).toASN1Primitive());
                i = i2 + 1;
                toASN1Primitive = aSN1Sequence.getObjectAt(i2).toASN1Primitive();
                if (toASN1Primitive instanceof ASN1TaggedObject) {
                    this.authAttrs = ASN1Set.getInstance((ASN1TaggedObject) toASN1Primitive, false);
                    i2 = i + 1;
                    aSN1Primitive = aSN1Sequence.getObjectAt(i).toASN1Primitive();
                    i = i2;
                    toASN1Primitive = aSN1Primitive;
                } else if (!this.authEncryptedContentInfo.getContentType().equals(CMSObjectIdentifiers.data) && (this.authAttrs == null || this.authAttrs.size() == 0)) {
                    throw new IllegalArgumentException("authAttrs must be present with non-data content");
                }
                this.mac = ASN1OctetString.getInstance(toASN1Primitive);
                if (aSN1Sequence.size() > i) {
                    this.unauthAttrs = ASN1Set.getInstance((ASN1TaggedObject) aSN1Sequence.getObjectAt(i).toASN1Primitive(), false);
                    return;
                }
                return;
            }
            throw new IllegalArgumentException("AuthEnvelopedData requires at least 1 RecipientInfo");
        }
        throw new IllegalArgumentException("AuthEnvelopedData version number must be 0");
    }

    public AuthEnvelopedData(OriginatorInfo originatorInfo, ASN1Set aSN1Set, EncryptedContentInfo encryptedContentInfo, ASN1Set aSN1Set2, ASN1OctetString aSN1OctetString, ASN1Set aSN1Set3) {
        this.version = new ASN1Integer(0);
        this.originatorInfo = originatorInfo;
        this.recipientInfos = aSN1Set;
        if (this.recipientInfos.size() != 0) {
            this.authEncryptedContentInfo = encryptedContentInfo;
            this.authAttrs = aSN1Set2;
            if (encryptedContentInfo.getContentType().equals(CMSObjectIdentifiers.data) || !(aSN1Set2 == null || aSN1Set2.size() == 0)) {
                this.mac = aSN1OctetString;
                this.unauthAttrs = aSN1Set3;
                return;
            }
            throw new IllegalArgumentException("authAttrs must be present with non-data content");
        }
        throw new IllegalArgumentException("AuthEnvelopedData requires at least 1 RecipientInfo");
    }

    public static AuthEnvelopedData getInstance(Object obj) {
        if (obj == null || (obj instanceof AuthEnvelopedData)) {
            return (AuthEnvelopedData) obj;
        }
        if (obj instanceof ASN1Sequence) {
            return new AuthEnvelopedData((ASN1Sequence) obj);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid AuthEnvelopedData: ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static AuthEnvelopedData getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public ASN1Set getAuthAttrs() {
        return this.authAttrs;
    }

    public EncryptedContentInfo getAuthEncryptedContentInfo() {
        return this.authEncryptedContentInfo;
    }

    public ASN1OctetString getMac() {
        return this.mac;
    }

    public OriginatorInfo getOriginatorInfo() {
        return this.originatorInfo;
    }

    public ASN1Set getRecipientInfos() {
        return this.recipientInfos;
    }

    public ASN1Set getUnauthAttrs() {
        return this.unauthAttrs;
    }

    public ASN1Integer getVersion() {
        return this.version;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.version);
        if (this.originatorInfo != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 0, this.originatorInfo));
        }
        aSN1EncodableVector.add(this.recipientInfos);
        aSN1EncodableVector.add(this.authEncryptedContentInfo);
        if (this.authAttrs != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 1, this.authAttrs));
        }
        aSN1EncodableVector.add(this.mac);
        if (this.unauthAttrs != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 2, this.unauthAttrs));
        }
        return new BERSequence(aSN1EncodableVector);
    }
}
