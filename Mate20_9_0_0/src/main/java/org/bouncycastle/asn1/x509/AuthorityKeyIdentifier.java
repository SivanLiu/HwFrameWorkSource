package org.bouncycastle.asn1.x509;

import java.math.BigInteger;
import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;

public class AuthorityKeyIdentifier extends ASN1Object {
    GeneralNames certissuer;
    ASN1Integer certserno;
    ASN1OctetString keyidentifier;

    protected AuthorityKeyIdentifier(ASN1Sequence aSN1Sequence) {
        this.keyidentifier = null;
        this.certissuer = null;
        this.certserno = null;
        Enumeration objects = aSN1Sequence.getObjects();
        while (objects.hasMoreElements()) {
            ASN1TaggedObject instance = ASN1TaggedObject.getInstance(objects.nextElement());
            switch (instance.getTagNo()) {
                case 0:
                    this.keyidentifier = ASN1OctetString.getInstance(instance, false);
                    break;
                case 1:
                    this.certissuer = GeneralNames.getInstance(instance, false);
                    break;
                case 2:
                    this.certserno = ASN1Integer.getInstance(instance, false);
                    break;
                default:
                    throw new IllegalArgumentException("illegal tag");
            }
        }
    }

    public AuthorityKeyIdentifier(GeneralNames generalNames, BigInteger bigInteger) {
        this((byte[]) null, generalNames, bigInteger);
    }

    public AuthorityKeyIdentifier(SubjectPublicKeyInfo subjectPublicKeyInfo) {
        this.keyidentifier = null;
        this.certissuer = null;
        this.certserno = null;
        Digest sHA1Digest = new SHA1Digest();
        byte[] bArr = new byte[sHA1Digest.getDigestSize()];
        byte[] bytes = subjectPublicKeyInfo.getPublicKeyData().getBytes();
        sHA1Digest.update(bytes, 0, bytes.length);
        sHA1Digest.doFinal(bArr, 0);
        this.keyidentifier = new DEROctetString(bArr);
    }

    public AuthorityKeyIdentifier(SubjectPublicKeyInfo subjectPublicKeyInfo, GeneralNames generalNames, BigInteger bigInteger) {
        this.keyidentifier = null;
        this.certissuer = null;
        this.certserno = null;
        Digest sHA1Digest = new SHA1Digest();
        byte[] bArr = new byte[sHA1Digest.getDigestSize()];
        byte[] bytes = subjectPublicKeyInfo.getPublicKeyData().getBytes();
        sHA1Digest.update(bytes, 0, bytes.length);
        sHA1Digest.doFinal(bArr, 0);
        this.keyidentifier = new DEROctetString(bArr);
        this.certissuer = GeneralNames.getInstance(generalNames.toASN1Primitive());
        this.certserno = new ASN1Integer(bigInteger);
    }

    public AuthorityKeyIdentifier(byte[] bArr) {
        this(bArr, null, null);
    }

    public AuthorityKeyIdentifier(byte[] bArr, GeneralNames generalNames, BigInteger bigInteger) {
        ASN1Integer aSN1Integer = null;
        this.keyidentifier = null;
        this.certissuer = null;
        this.certserno = null;
        this.keyidentifier = bArr != null ? new DEROctetString(bArr) : null;
        this.certissuer = generalNames;
        if (bigInteger != null) {
            aSN1Integer = new ASN1Integer(bigInteger);
        }
        this.certserno = aSN1Integer;
    }

    public static AuthorityKeyIdentifier fromExtensions(Extensions extensions) {
        return getInstance(extensions.getExtensionParsedValue(Extension.authorityKeyIdentifier));
    }

    public static AuthorityKeyIdentifier getInstance(Object obj) {
        return obj instanceof AuthorityKeyIdentifier ? (AuthorityKeyIdentifier) obj : obj != null ? new AuthorityKeyIdentifier(ASN1Sequence.getInstance(obj)) : null;
    }

    public static AuthorityKeyIdentifier getInstance(ASN1TaggedObject aSN1TaggedObject, boolean z) {
        return getInstance(ASN1Sequence.getInstance(aSN1TaggedObject, z));
    }

    public GeneralNames getAuthorityCertIssuer() {
        return this.certissuer;
    }

    public BigInteger getAuthorityCertSerialNumber() {
        return this.certserno != null ? this.certserno.getValue() : null;
    }

    public byte[] getKeyIdentifier() {
        return this.keyidentifier != null ? this.keyidentifier.getOctets() : null;
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        if (this.keyidentifier != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 0, this.keyidentifier));
        }
        if (this.certissuer != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 1, this.certissuer));
        }
        if (this.certserno != null) {
            aSN1EncodableVector.add(new DERTaggedObject(false, 2, this.certserno));
        }
        return new DERSequence(aSN1EncodableVector);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AuthorityKeyIdentifier: KeyID(");
        stringBuilder.append(this.keyidentifier.getOctets());
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
