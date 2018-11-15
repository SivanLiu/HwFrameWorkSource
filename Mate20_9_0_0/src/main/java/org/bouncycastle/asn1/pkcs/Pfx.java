package org.bouncycastle.asn1.pkcs;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.BERSequence;

public class Pfx extends ASN1Object implements PKCSObjectIdentifiers {
    private ContentInfo contentInfo;
    private MacData macData = null;

    private Pfx(ASN1Sequence aSN1Sequence) {
        if (ASN1Integer.getInstance(aSN1Sequence.getObjectAt(0)).getValue().intValue() == 3) {
            this.contentInfo = ContentInfo.getInstance(aSN1Sequence.getObjectAt(1));
            if (aSN1Sequence.size() == 3) {
                this.macData = MacData.getInstance(aSN1Sequence.getObjectAt(2));
                return;
            }
            return;
        }
        throw new IllegalArgumentException("wrong version for PFX PDU");
    }

    public Pfx(ContentInfo contentInfo, MacData macData) {
        this.contentInfo = contentInfo;
        this.macData = macData;
    }

    public static Pfx getInstance(Object obj) {
        return obj instanceof Pfx ? (Pfx) obj : obj != null ? new Pfx(ASN1Sequence.getInstance(obj)) : null;
    }

    public ContentInfo getAuthSafe() {
        return this.contentInfo;
    }

    public MacData getMacData() {
        return this.macData;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(new ASN1Integer(3));
        aSN1EncodableVector.add(this.contentInfo);
        if (this.macData != null) {
            aSN1EncodableVector.add(this.macData);
        }
        return new BERSequence(aSN1EncodableVector);
    }
}
