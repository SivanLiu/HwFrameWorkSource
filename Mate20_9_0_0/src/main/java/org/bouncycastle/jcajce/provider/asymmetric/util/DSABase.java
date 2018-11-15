package org.bouncycastle.jcajce.provider.asymmetric.util;

import java.math.BigInteger;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.spec.AlgorithmParameterSpec;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.DSA;
import org.bouncycastle.crypto.Digest;

public abstract class DSABase extends SignatureSpi implements PKCSObjectIdentifiers, X509ObjectIdentifiers {
    protected Digest digest;
    protected DSAEncoder encoder;
    protected DSA signer;

    protected DSABase(Digest digest, DSA dsa, DSAEncoder dSAEncoder) {
        this.digest = digest;
        this.signer = dsa;
        this.encoder = dSAEncoder;
    }

    protected Object engineGetParameter(String str) {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    protected void engineSetParameter(String str, Object obj) {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    protected void engineSetParameter(AlgorithmParameterSpec algorithmParameterSpec) {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    protected byte[] engineSign() throws SignatureException {
        byte[] bArr = new byte[this.digest.getDigestSize()];
        this.digest.doFinal(bArr, 0);
        try {
            BigInteger[] generateSignature = this.signer.generateSignature(bArr);
            return this.encoder.encode(generateSignature[0], generateSignature[1]);
        } catch (Exception e) {
            throw new SignatureException(e.toString());
        }
    }

    protected void engineUpdate(byte b) throws SignatureException {
        this.digest.update(b);
    }

    protected void engineUpdate(byte[] bArr, int i, int i2) throws SignatureException {
        this.digest.update(bArr, i, i2);
    }

    protected boolean engineVerify(byte[] bArr) throws SignatureException {
        byte[] bArr2 = new byte[this.digest.getDigestSize()];
        this.digest.doFinal(bArr2, 0);
        try {
            BigInteger[] decode = this.encoder.decode(bArr);
            return this.signer.verifySignature(bArr2, decode[0], decode[1]);
        } catch (Exception e) {
            throw new SignatureException("error decoding signature bytes.");
        }
    }
}
