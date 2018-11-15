package org.bouncycastle.asn1.cmc;

import java.io.IOException;
import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;

public class OtherStatusInfo extends ASN1Object implements ASN1Choice {
    private final ExtendedFailInfo extendedFailInfo;
    private final CMCFailInfo failInfo;
    private final PendInfo pendInfo;

    OtherStatusInfo(CMCFailInfo cMCFailInfo) {
        this(cMCFailInfo, null, null);
    }

    private OtherStatusInfo(CMCFailInfo cMCFailInfo, PendInfo pendInfo, ExtendedFailInfo extendedFailInfo) {
        this.failInfo = cMCFailInfo;
        this.pendInfo = pendInfo;
        this.extendedFailInfo = extendedFailInfo;
    }

    OtherStatusInfo(ExtendedFailInfo extendedFailInfo) {
        this(null, null, extendedFailInfo);
    }

    OtherStatusInfo(PendInfo pendInfo) {
        this(null, pendInfo, null);
    }

    public static OtherStatusInfo getInstance(Object obj) {
        StringBuilder stringBuilder;
        if (obj instanceof OtherStatusInfo) {
            return (OtherStatusInfo) obj;
        }
        if (obj instanceof ASN1Encodable) {
            ASN1Primitive toASN1Primitive = ((ASN1Encodable) obj).toASN1Primitive();
            if (toASN1Primitive instanceof ASN1Integer) {
                return new OtherStatusInfo(CMCFailInfo.getInstance(toASN1Primitive));
            }
            if (toASN1Primitive instanceof ASN1Sequence) {
                return ((ASN1Sequence) toASN1Primitive).getObjectAt(0) instanceof ASN1ObjectIdentifier ? new OtherStatusInfo(ExtendedFailInfo.getInstance(toASN1Primitive)) : new OtherStatusInfo(PendInfo.getInstance(toASN1Primitive));
            }
        } else if (obj instanceof byte[]) {
            try {
                return getInstance(ASN1Primitive.fromByteArray((byte[]) obj));
            } catch (IOException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("parsing error: ");
                stringBuilder.append(e.getMessage());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("unknown object in getInstance(): ");
        stringBuilder.append(obj.getClass().getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public boolean isExtendedFailInfo() {
        return this.extendedFailInfo != null;
    }

    public boolean isFailInfo() {
        return this.failInfo != null;
    }

    public boolean isPendingInfo() {
        return this.pendInfo != null;
    }

    public ASN1Primitive toASN1Primitive() {
        return this.pendInfo != null ? this.pendInfo.toASN1Primitive() : this.failInfo != null ? this.failInfo.toASN1Primitive() : this.extendedFailInfo.toASN1Primitive();
    }
}
