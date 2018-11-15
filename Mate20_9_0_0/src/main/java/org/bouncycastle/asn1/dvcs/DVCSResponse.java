package org.bouncycastle.asn1.dvcs;

import java.io.IOException;
import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERTaggedObject;

public class DVCSResponse extends ASN1Object implements ASN1Choice {
    private DVCSCertInfo dvCertInfo;
    private DVCSErrorNotice dvErrorNote;

    public DVCSResponse(DVCSCertInfo dVCSCertInfo) {
        this.dvCertInfo = dVCSCertInfo;
    }

    public DVCSResponse(DVCSErrorNotice dVCSErrorNotice) {
        this.dvErrorNote = dVCSErrorNotice;
    }

    public static DVCSResponse getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj == null || (obj instanceof DVCSResponse)) {
            return (DVCSResponse) obj;
        }
        if (obj instanceof byte[]) {
            try {
                return getInstance(ASN1Primitive.fromByteArray((byte[]) obj));
            } catch (IOException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("failed to construct sequence from byte[]: ");
                stringBuilder.append(e.getMessage());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        } else if (obj instanceof ASN1Sequence) {
            return new DVCSResponse(DVCSCertInfo.getInstance(obj));
        } else {
            if (obj instanceof ASN1TaggedObject) {
                return new DVCSResponse(DVCSErrorNotice.getInstance(ASN1TaggedObject.getInstance(obj), false));
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't convert from object to DVCSResponse: ");
            stringBuilder.append(obj.getClass().getName());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public static DVCSResponse getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public DVCSCertInfo getCertInfo() {
        return this.dvCertInfo;
    }

    public DVCSErrorNotice getErrorNotice() {
        return this.dvErrorNote;
    }

    public ASN1Primitive toASN1Primitive() {
        return this.dvCertInfo != null ? this.dvCertInfo.toASN1Primitive() : new DERTaggedObject(false, 0, this.dvErrorNote);
    }

    public String toString() {
        StringBuilder stringBuilder;
        String dVCSCertInfo;
        if (this.dvCertInfo != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("DVCSResponse {\ndvCertInfo: ");
            dVCSCertInfo = this.dvCertInfo.toString();
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("DVCSResponse {\ndvErrorNote: ");
            dVCSCertInfo = this.dvErrorNote.toString();
        }
        stringBuilder.append(dVCSCertInfo);
        stringBuilder.append("}\n");
        return stringBuilder.toString();
    }
}
