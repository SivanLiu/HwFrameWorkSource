package org.bouncycastle.crypto.agreement.kdf;

import java.io.IOException;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.DerivationParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.DigestDerivationFunction;
import org.bouncycastle.crypto.generators.KDF2BytesGenerator;
import org.bouncycastle.crypto.params.KDFParameters;
import org.bouncycastle.util.Pack;

public class ECDHKEKGenerator implements DigestDerivationFunction {
    private ASN1ObjectIdentifier algorithm;
    private DigestDerivationFunction kdf;
    private int keySize;
    private byte[] z;

    public ECDHKEKGenerator(Digest digest) {
        this.kdf = new KDF2BytesGenerator(digest);
    }

    public int generateBytes(byte[] bArr, int i, int i2) throws DataLengthException, IllegalArgumentException {
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(new AlgorithmIdentifier(this.algorithm, DERNull.INSTANCE));
        aSN1EncodableVector.add(new DERTaggedObject(true, 2, new DEROctetString(Pack.intToBigEndian(this.keySize))));
        try {
            this.kdf.init(new KDFParameters(this.z, new DERSequence(aSN1EncodableVector).getEncoded(ASN1Encoding.DER)));
            return this.kdf.generateBytes(bArr, i, i2);
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unable to initialise kdf: ");
            stringBuilder.append(e.getMessage());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public Digest getDigest() {
        return this.kdf.getDigest();
    }

    public void init(DerivationParameters derivationParameters) {
        DHKDFParameters dHKDFParameters = (DHKDFParameters) derivationParameters;
        this.algorithm = dHKDFParameters.getAlgorithm();
        this.keySize = dHKDFParameters.getKeySize();
        this.z = dHKDFParameters.getZ();
    }
}
