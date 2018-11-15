package org.bouncycastle.asn1.cmc;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.cms.ContentInfo;

public class TaggedContentInfo extends ASN1Object {
    private final BodyPartID bodyPartID;
    private final ContentInfo contentInfo;

    private TaggedContentInfo(ASN1Sequence aSN1Sequence) {
        if (aSN1Sequence.size() == 2) {
            this.bodyPartID = BodyPartID.getInstance(aSN1Sequence.getObjectAt(0));
            this.contentInfo = ContentInfo.getInstance(aSN1Sequence.getObjectAt(1));
            return;
        }
        throw new IllegalArgumentException("incorrect sequence size");
    }

    public TaggedContentInfo(BodyPartID bodyPartID, ContentInfo contentInfo) {
        this.bodyPartID = bodyPartID;
        this.contentInfo = contentInfo;
    }

    public static TaggedContentInfo getInstance(Object obj) {
        return obj instanceof TaggedContentInfo ? (TaggedContentInfo) obj : obj != null ? new TaggedContentInfo(ASN1Sequence.getInstance(obj)) : null;
    }

    public static TaggedContentInfo getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public BodyPartID getBodyPartID() {
        return this.bodyPartID;
    }

    public ContentInfo getContentInfo() {
        return this.contentInfo;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.bodyPartID);
        aSN1EncodableVector.add(this.contentInfo);
        return new DERSequence(aSN1EncodableVector);
    }
}
