package org.bouncycastle.asn1.cmc;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.Extension;

class Utils {
    Utils() {
    }

    static BodyPartID[] clone(BodyPartID[] bodyPartIDArr) {
        Object obj = new BodyPartID[bodyPartIDArr.length];
        System.arraycopy(bodyPartIDArr, 0, obj, 0, bodyPartIDArr.length);
        return obj;
    }

    static Extension[] clone(Extension[] extensionArr) {
        Object obj = new Extension[extensionArr.length];
        System.arraycopy(extensionArr, 0, obj, 0, extensionArr.length);
        return obj;
    }

    static BodyPartID[] toBodyPartIDArray(ASN1Sequence aSN1Sequence) {
        BodyPartID[] bodyPartIDArr = new BodyPartID[aSN1Sequence.size()];
        for (int i = 0; i != aSN1Sequence.size(); i++) {
            bodyPartIDArr[i] = BodyPartID.getInstance(aSN1Sequence.getObjectAt(i));
        }
        return bodyPartIDArr;
    }
}
