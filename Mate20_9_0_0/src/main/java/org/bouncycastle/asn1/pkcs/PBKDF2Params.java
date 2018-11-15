package org.bouncycastle.asn1.pkcs;

import java.math.BigInteger;
import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.util.Arrays;

public class PBKDF2Params extends ASN1Object {
    private static final AlgorithmIdentifier algid_hmacWithSHA1 = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_hmacWithSHA1, DERNull.INSTANCE);
    private final ASN1Integer iterationCount;
    private final ASN1Integer keyLength;
    private final ASN1OctetString octStr;
    private final AlgorithmIdentifier prf;

    private PBKDF2Params(ASN1Sequence aSN1Sequence) {
        Enumeration objects = aSN1Sequence.getObjects();
        this.octStr = (ASN1OctetString) objects.nextElement();
        this.iterationCount = (ASN1Integer) objects.nextElement();
        if (objects.hasMoreElements()) {
            Object nextElement;
            Object nextElement2 = objects.nextElement();
            if (nextElement2 instanceof ASN1Integer) {
                this.keyLength = ASN1Integer.getInstance(nextElement2);
                nextElement = objects.hasMoreElements() ? objects.nextElement() : null;
            } else {
                this.keyLength = null;
                nextElement = nextElement2;
            }
            if (nextElement != null) {
                this.prf = AlgorithmIdentifier.getInstance(nextElement);
                return;
            }
        }
        this.keyLength = null;
        this.prf = null;
    }

    public PBKDF2Params(byte[] bArr, int i) {
        this(bArr, i, 0);
    }

    public PBKDF2Params(byte[] bArr, int i, int i2) {
        this(bArr, i, i2, null);
    }

    public PBKDF2Params(byte[] bArr, int i, int i2, AlgorithmIdentifier algorithmIdentifier) {
        this.octStr = new DEROctetString(Arrays.clone(bArr));
        this.iterationCount = new ASN1Integer((long) i);
        this.keyLength = i2 > 0 ? new ASN1Integer((long) i2) : null;
        this.prf = algorithmIdentifier;
    }

    public PBKDF2Params(byte[] bArr, int i, AlgorithmIdentifier algorithmIdentifier) {
        this(bArr, i, 0, algorithmIdentifier);
    }

    public static PBKDF2Params getInstance(Object obj) {
        return obj instanceof PBKDF2Params ? (PBKDF2Params) obj : obj != null ? new PBKDF2Params(ASN1Sequence.getInstance(obj)) : null;
    }

    public BigInteger getIterationCount() {
        return this.iterationCount.getValue();
    }

    public BigInteger getKeyLength() {
        return this.keyLength != null ? this.keyLength.getValue() : null;
    }

    public AlgorithmIdentifier getPrf() {
        return this.prf != null ? this.prf : algid_hmacWithSHA1;
    }

    public byte[] getSalt() {
        return this.octStr.getOctets();
    }

    public boolean isDefaultPrf() {
        return this.prf == null || this.prf.equals(algid_hmacWithSHA1);
    }

    public ASN1Primitive toASN1Primitive() {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(this.octStr);
        aSN1EncodableVector.add(this.iterationCount);
        if (this.keyLength != null) {
            aSN1EncodableVector.add(this.keyLength);
        }
        if (!(this.prf == null || this.prf.equals(algid_hmacWithSHA1))) {
            aSN1EncodableVector.add(this.prf);
        }
        return new DERSequence(aSN1EncodableVector);
    }
}
