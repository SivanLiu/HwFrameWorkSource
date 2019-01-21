package com.android.org.bouncycastle.asn1.cms;

import com.android.org.bouncycastle.asn1.ASN1EncodableVector;
import com.android.org.bouncycastle.asn1.ASN1Object;
import com.android.org.bouncycastle.asn1.ASN1Primitive;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.org.bouncycastle.asn1.DERSequence;
import com.android.org.bouncycastle.asn1.DERTaggedObject;
import com.android.org.bouncycastle.asn1.x509.AlgorithmIdentifier;

public class CMSAlgorithmProtection extends ASN1Object {
    public static final int MAC = 2;
    public static final int SIGNATURE = 1;
    private final AlgorithmIdentifier digestAlgorithm;
    private final AlgorithmIdentifier macAlgorithm;
    private final AlgorithmIdentifier signatureAlgorithm;

    public CMSAlgorithmProtection(AlgorithmIdentifier digestAlgorithm, int type, AlgorithmIdentifier algorithmIdentifier) {
        if (digestAlgorithm == null || algorithmIdentifier == null) {
            throw new NullPointerException("AlgorithmIdentifiers cannot be null");
        }
        this.digestAlgorithm = digestAlgorithm;
        if (type == 1) {
            this.signatureAlgorithm = algorithmIdentifier;
            this.macAlgorithm = null;
        } else if (type == 2) {
            this.signatureAlgorithm = null;
            this.macAlgorithm = algorithmIdentifier;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown type: ");
            stringBuilder.append(type);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private CMSAlgorithmProtection(ASN1Sequence sequence) {
        if (sequence.size() == 2) {
            this.digestAlgorithm = AlgorithmIdentifier.getInstance(sequence.getObjectAt(0));
            ASN1TaggedObject tagged = ASN1TaggedObject.getInstance(sequence.getObjectAt(1));
            if (tagged.getTagNo() == 1) {
                this.signatureAlgorithm = AlgorithmIdentifier.getInstance(tagged, false);
                this.macAlgorithm = null;
                return;
            } else if (tagged.getTagNo() == 2) {
                this.signatureAlgorithm = null;
                this.macAlgorithm = AlgorithmIdentifier.getInstance(tagged, false);
                return;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown tag found: ");
                stringBuilder.append(tagged.getTagNo());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        throw new IllegalArgumentException("Sequence wrong size: One of signatureAlgorithm or macAlgorithm must be present");
    }

    public static CMSAlgorithmProtection getInstance(Object obj) {
        if (obj instanceof CMSAlgorithmProtection) {
            return (CMSAlgorithmProtection) obj;
        }
        if (obj != null) {
            return new CMSAlgorithmProtection(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    public AlgorithmIdentifier getDigestAlgorithm() {
        return this.digestAlgorithm;
    }

    public AlgorithmIdentifier getMacAlgorithm() {
        return this.macAlgorithm;
    }

    public AlgorithmIdentifier getSignatureAlgorithm() {
        return this.signatureAlgorithm;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(this.digestAlgorithm);
        if (this.signatureAlgorithm != null) {
            v.add(new DERTaggedObject(false, 1, this.signatureAlgorithm));
        }
        if (this.macAlgorithm != null) {
            v.add(new DERTaggedObject(false, 2, this.macAlgorithm));
        }
        return new DERSequence(v);
    }
}
