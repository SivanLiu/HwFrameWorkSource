package org.bouncycastle.asn1.x509;

import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERTaggedObject;

public class Target extends ASN1Object implements ASN1Choice {
    public static final int targetGroup = 1;
    public static final int targetName = 0;
    private GeneralName targGroup;
    private GeneralName targName;

    public Target(int i, GeneralName generalName) {
        this(new DERTaggedObject(i, generalName));
    }

    private Target(ASN1TaggedObject aSN1TaggedObject) {
        switch (aSN1TaggedObject.getTagNo()) {
            case 0:
                this.targName = GeneralName.getInstance(aSN1TaggedObject, true);
                return;
            case 1:
                this.targGroup = GeneralName.getInstance(aSN1TaggedObject, true);
                return;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown tag: ");
                stringBuilder.append(aSN1TaggedObject.getTagNo());
                throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static Target getInstance(Object obj) {
        if (obj == null || (obj instanceof Target)) {
            return (Target) obj;
        }
        if (obj instanceof ASN1TaggedObject) {
            return new Target((ASN1TaggedObject) obj);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unknown object in factory: ");
        stringBuilder.append(obj.getClass());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public GeneralName getTargetGroup() {
        return this.targGroup;
    }

    public GeneralName getTargetName() {
        return this.targName;
    }

    public ASN1Primitive toASN1Primitive() {
        return this.targName != null ? new DERTaggedObject(true, 0, this.targName) : new DERTaggedObject(true, 1, this.targGroup);
    }
}
