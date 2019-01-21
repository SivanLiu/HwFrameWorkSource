package org.bouncycastle.crypto.agreement.kdf;

import java.io.IOException;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.DerivationFunction;
import org.bouncycastle.crypto.DerivationParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.OutputLengthException;
import org.bouncycastle.util.Pack;

public class DHKEKGenerator implements DerivationFunction {
    private ASN1ObjectIdentifier algorithm;
    private final Digest digest;
    private int keySize;
    private byte[] partyAInfo;
    private byte[] z;

    public DHKEKGenerator(Digest digest) {
        this.digest = digest;
    }

    public int generateBytes(byte[] bArr, int i, int i2) throws DataLengthException, IllegalArgumentException {
        Object obj = bArr;
        int i3 = i2;
        int i4 = i;
        if (obj.length - i3 >= i4) {
            long j = (long) i3;
            int digestSize = this.digest.getDigestSize();
            if (j <= 8589934591L) {
                long j2 = (long) digestSize;
                int i5 = (int) (((j + j2) - 1) / j2);
                byte[] bArr2 = new byte[this.digest.getDigestSize()];
                int i6 = 0;
                int i7 = i3;
                int i8 = i4;
                i3 = 0;
                i4 = 1;
                while (i3 < i5) {
                    boolean z;
                    this.digest.update(this.z, i6, this.z.length);
                    ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
                    ASN1EncodableVector aSN1EncodableVector2 = new ASN1EncodableVector();
                    aSN1EncodableVector2.add(this.algorithm);
                    aSN1EncodableVector2.add(new DEROctetString(Pack.intToBigEndian(i4)));
                    aSN1EncodableVector.add(new DERSequence(aSN1EncodableVector2));
                    if (this.partyAInfo != null) {
                        z = true;
                        aSN1EncodableVector.add(new DERTaggedObject(true, i6, new DEROctetString(this.partyAInfo)));
                    } else {
                        z = true;
                    }
                    aSN1EncodableVector.add(new DERTaggedObject(z, 2, new DEROctetString(Pack.intToBigEndian(this.keySize))));
                    try {
                        byte[] encoded = new DERSequence(aSN1EncodableVector).getEncoded(ASN1Encoding.DER);
                        this.digest.update(encoded, 0, encoded.length);
                        this.digest.doFinal(bArr2, 0);
                        if (i7 > digestSize) {
                            System.arraycopy(bArr2, 0, obj, i8, digestSize);
                            i8 += digestSize;
                            i7 -= digestSize;
                        } else {
                            System.arraycopy(bArr2, 0, obj, i8, i7);
                        }
                        i4++;
                        i3++;
                        i6 = 0;
                    } catch (IOException e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unable to encode parameter info: ");
                        stringBuilder.append(e.getMessage());
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                }
                this.digest.reset();
                return (int) j;
            }
            throw new IllegalArgumentException("Output length too large");
        }
        throw new OutputLengthException("output buffer too small");
    }

    public Digest getDigest() {
        return this.digest;
    }

    public void init(DerivationParameters derivationParameters) {
        DHKDFParameters dHKDFParameters = (DHKDFParameters) derivationParameters;
        this.algorithm = dHKDFParameters.getAlgorithm();
        this.keySize = dHKDFParameters.getKeySize();
        this.z = dHKDFParameters.getZ();
        this.partyAInfo = dHKDFParameters.getExtraInfo();
    }
}
