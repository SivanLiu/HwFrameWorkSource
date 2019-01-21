package org.bouncycastle.asn1.bc;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.bouncycastle.asn1.x509.Certificate;

public class EncryptedPrivateKeyData extends ASN1Object {
    private final Certificate[] certificateChain;
    private final EncryptedPrivateKeyInfo encryptedPrivateKeyInfo;

    private EncryptedPrivateKeyData(ASN1Sequence aSN1Sequence) {
        int i = 0;
        this.encryptedPrivateKeyInfo = EncryptedPrivateKeyInfo.getInstance(aSN1Sequence.getObjectAt(0));
        aSN1Sequence = ASN1Sequence.getInstance(aSN1Sequence.getObjectAt(1));
        this.certificateChain = new Certificate[aSN1Sequence.size()];
        while (i != this.certificateChain.length) {
            this.certificateChain[i] = Certificate.getInstance(aSN1Sequence.getObjectAt(i));
            i++;
        }
    }

    public EncryptedPrivateKeyData(EncryptedPrivateKeyInfo encryptedPrivateKeyInfo, Certificate[] certificateArr) {
        this.encryptedPrivateKeyInfo = encryptedPrivateKeyInfo;
        this.certificateChain = new Certificate[certificateArr.length];
        System.arraycopy(certificateArr, 0, this.certificateChain, 0, certificateArr.length);
    }

    public static EncryptedPrivateKeyData getInstance(Object obj) {
        return obj instanceof EncryptedPrivateKeyData ? (EncryptedPrivateKeyData) obj : obj != null ? new EncryptedPrivateKeyData(ASN1Sequence.getInstance(obj)) : null;
    }

    public Certificate[] getCertificateChain() {
        Certificate[] certificateArr = new Certificate[this.certificateChain.length];
        System.arraycopy(this.certificateChain, 0, certificateArr, 0, this.certificateChain.length);
        return certificateArr;
    }

    public EncryptedPrivateKeyInfo getEncryptedPrivateKeyInfo() {
        return this.encryptedPrivateKeyInfo;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.encryptedPrivateKeyInfo);
        aSN1EncodableVector.add(new DERSequence(this.certificateChain));
        return new DERSequence(aSN1EncodableVector);
    }
}
