package org.bouncycastle.asn1.cmc;

import java.io.IOException;
import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;

public class BodyPartReference extends ASN1Object implements ASN1Choice {
    private final BodyPartID bodyPartID;
    private final BodyPartPath bodyPartPath;

    public BodyPartReference(BodyPartID bodyPartID) {
        this.bodyPartID = bodyPartID;
        this.bodyPartPath = null;
    }

    public BodyPartReference(BodyPartPath bodyPartPath) {
        this.bodyPartID = null;
        this.bodyPartPath = bodyPartPath;
    }

    public static BodyPartReference getInstance(Object obj) {
        if (obj instanceof BodyPartReference) {
            return (BodyPartReference) obj;
        }
        if (obj == null) {
            return null;
        }
        if (obj instanceof ASN1Encodable) {
            ASN1Primitive toASN1Primitive = ((ASN1Encodable) obj).toASN1Primitive();
            if (toASN1Primitive instanceof ASN1Integer) {
                return new BodyPartReference(BodyPartID.getInstance(toASN1Primitive));
            }
            if (toASN1Primitive instanceof ASN1Sequence) {
                return new BodyPartReference(BodyPartPath.getInstance(toASN1Primitive));
            }
        }
        if (obj instanceof byte[]) {
            try {
                return getInstance(ASN1Primitive.fromByteArray((byte[]) obj));
            } catch (IOException e) {
                throw new IllegalArgumentException("unknown encoding in getInstance()");
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unknown object in getInstance(): ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public BodyPartID getBodyPartID() {
        return this.bodyPartID;
    }

    public BodyPartPath getBodyPartPath() {
        return this.bodyPartPath;
    }

    public boolean isBodyPartID() {
        return this.bodyPartID != null;
    }

    public ASN1Primitive toASN1Primitive() {
        return this.bodyPartID != null ? this.bodyPartID.toASN1Primitive() : this.bodyPartPath.toASN1Primitive();
    }
}
