package org.bouncycastle.asn1.cmc;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;

public class PKIData extends ASN1Object {
    private final TaggedContentInfo[] cmsSequence;
    private final TaggedAttribute[] controlSequence;
    private final OtherMsg[] otherMsgSequence;
    private final TaggedRequest[] reqSequence;

    private PKIData(ASN1Sequence aSN1Sequence) {
        if (aSN1Sequence.size() == 4) {
            int i;
            int i2 = 0;
            ASN1Sequence aSN1Sequence2 = (ASN1Sequence) aSN1Sequence.getObjectAt(0);
            this.controlSequence = new TaggedAttribute[aSN1Sequence2.size()];
            for (i = 0; i < this.controlSequence.length; i++) {
                this.controlSequence[i] = TaggedAttribute.getInstance(aSN1Sequence2.getObjectAt(i));
            }
            aSN1Sequence2 = (ASN1Sequence) aSN1Sequence.getObjectAt(1);
            this.reqSequence = new TaggedRequest[aSN1Sequence2.size()];
            for (i = 0; i < this.reqSequence.length; i++) {
                this.reqSequence[i] = TaggedRequest.getInstance(aSN1Sequence2.getObjectAt(i));
            }
            aSN1Sequence2 = (ASN1Sequence) aSN1Sequence.getObjectAt(2);
            this.cmsSequence = new TaggedContentInfo[aSN1Sequence2.size()];
            for (i = 0; i < this.cmsSequence.length; i++) {
                this.cmsSequence[i] = TaggedContentInfo.getInstance(aSN1Sequence2.getObjectAt(i));
            }
            aSN1Sequence = (ASN1Sequence) aSN1Sequence.getObjectAt(3);
            this.otherMsgSequence = new OtherMsg[aSN1Sequence.size()];
            while (i2 < this.otherMsgSequence.length) {
                this.otherMsgSequence[i2] = OtherMsg.getInstance(aSN1Sequence.getObjectAt(i2));
                i2++;
            }
            return;
        }
        throw new IllegalArgumentException("Sequence not 4 elements.");
    }

    public PKIData(TaggedAttribute[] taggedAttributeArr, TaggedRequest[] taggedRequestArr, TaggedContentInfo[] taggedContentInfoArr, OtherMsg[] otherMsgArr) {
        this.controlSequence = taggedAttributeArr;
        this.reqSequence = taggedRequestArr;
        this.cmsSequence = taggedContentInfoArr;
        this.otherMsgSequence = otherMsgArr;
    }

    public static PKIData getInstance(Object obj) {
        return obj instanceof PKIData ? (PKIData) obj : obj != null ? new PKIData(ASN1Sequence.getInstance(obj)) : null;
    }

    public TaggedContentInfo[] getCmsSequence() {
        return this.cmsSequence;
    }

    public TaggedAttribute[] getControlSequence() {
        return this.controlSequence;
    }

    public OtherMsg[] getOtherMsgSequence() {
        return this.otherMsgSequence;
    }

    public TaggedRequest[] getReqSequence() {
        return this.reqSequence;
    }

    public ASN1Primitive toASN1Primitive() {
        return new DERSequence(new ASN1Encodable[]{new DERSequence(this.controlSequence), new DERSequence(this.reqSequence), new DERSequence(this.cmsSequence), new DERSequence(this.otherMsgSequence)});
    }
}
