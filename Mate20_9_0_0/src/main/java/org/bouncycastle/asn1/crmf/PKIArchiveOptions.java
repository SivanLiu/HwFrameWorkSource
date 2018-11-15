package org.bouncycastle.asn1.crmf;

import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1Choice;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERTaggedObject;

public class PKIArchiveOptions extends ASN1Object implements ASN1Choice {
    public static final int archiveRemGenPrivKey = 2;
    public static final int encryptedPrivKey = 0;
    public static final int keyGenParameters = 1;
    private ASN1Encodable value;

    public PKIArchiveOptions(ASN1OctetString aSN1OctetString) {
        this.value = aSN1OctetString;
    }

    private PKIArchiveOptions(ASN1TaggedObject aSN1TaggedObject) {
        ASN1Encodable instance;
        switch (aSN1TaggedObject.getTagNo()) {
            case 0:
                instance = EncryptedKey.getInstance(aSN1TaggedObject.getObject());
                break;
            case 1:
                instance = ASN1OctetString.getInstance(aSN1TaggedObject, false);
                break;
            case 2:
                instance = ASN1Boolean.getInstance(aSN1TaggedObject, false);
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown tag number: ");
                stringBuilder.append(aSN1TaggedObject.getTagNo());
                throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.value = instance;
    }

    public PKIArchiveOptions(EncryptedKey encryptedKey) {
        this.value = encryptedKey;
    }

    public PKIArchiveOptions(boolean z) {
        this.value = ASN1Boolean.getInstance(z);
    }

    public static PKIArchiveOptions getInstance(Object obj) {
        if (obj == null || (obj instanceof PKIArchiveOptions)) {
            return (PKIArchiveOptions) obj;
        }
        if (obj instanceof ASN1TaggedObject) {
            return new PKIArchiveOptions((ASN1TaggedObject) obj);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unknown object: ");
        stringBuilder.append(obj);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public int getType() {
        return this.value instanceof EncryptedKey ? 0 : this.value instanceof ASN1OctetString ? 1 : 2;
    }

    public ASN1Encodable getValue() {
        return this.value;
    }

    public ASN1Primitive toASN1Primitive() {
        return this.value instanceof EncryptedKey ? new DERTaggedObject(true, 0, this.value) : this.value instanceof ASN1OctetString ? new DERTaggedObject(false, 1, this.value) : new DERTaggedObject(false, 2, this.value);
    }
}
