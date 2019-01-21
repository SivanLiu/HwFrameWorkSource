package org.bouncycastle.crypto.tls;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DSA;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.digests.NullDigest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.DSADigestSigner;

public abstract class TlsDSASigner extends AbstractTlsSigner {
    protected abstract DSA createDSAImpl(short s);

    public Signer createSigner(SignatureAndHashAlgorithm signatureAndHashAlgorithm, AsymmetricKeyParameter asymmetricKeyParameter) {
        return makeSigner(signatureAndHashAlgorithm, false, true, asymmetricKeyParameter);
    }

    public Signer createVerifyer(SignatureAndHashAlgorithm signatureAndHashAlgorithm, AsymmetricKeyParameter asymmetricKeyParameter) {
        return makeSigner(signatureAndHashAlgorithm, false, false, asymmetricKeyParameter);
    }

    public byte[] generateRawSignature(SignatureAndHashAlgorithm signatureAndHashAlgorithm, AsymmetricKeyParameter asymmetricKeyParameter, byte[] bArr) throws CryptoException {
        int i;
        int i2;
        Signer makeSigner = makeSigner(signatureAndHashAlgorithm, true, true, new ParametersWithRandom(asymmetricKeyParameter, this.context.getSecureRandom()));
        if (signatureAndHashAlgorithm == null) {
            i = 16;
            i2 = 20;
        } else {
            i = 0;
            i2 = bArr.length;
        }
        makeSigner.update(bArr, i, i2);
        return makeSigner.generateSignature();
    }

    protected abstract short getSignatureAlgorithm();

    protected CipherParameters makeInitParameters(boolean z, CipherParameters cipherParameters) {
        return cipherParameters;
    }

    protected Signer makeSigner(SignatureAndHashAlgorithm signatureAndHashAlgorithm, boolean z, boolean z2, CipherParameters cipherParameters) {
        if ((signatureAndHashAlgorithm != null) != TlsUtils.isTLSv12(this.context)) {
            throw new IllegalStateException();
        } else if (signatureAndHashAlgorithm == null || signatureAndHashAlgorithm.getSignature() == getSignatureAlgorithm()) {
            short hash = signatureAndHashAlgorithm == null ? (short) 2 : signatureAndHashAlgorithm.getHash();
            DSADigestSigner dSADigestSigner = new DSADigestSigner(createDSAImpl(hash), z ? new NullDigest() : TlsUtils.createHash(hash));
            dSADigestSigner.init(z2, makeInitParameters(z2, cipherParameters));
            return dSADigestSigner;
        } else {
            throw new IllegalStateException();
        }
    }

    public boolean verifyRawSignature(SignatureAndHashAlgorithm signatureAndHashAlgorithm, byte[] bArr, AsymmetricKeyParameter asymmetricKeyParameter, byte[] bArr2) throws CryptoException {
        Signer makeSigner = makeSigner(signatureAndHashAlgorithm, true, false, asymmetricKeyParameter);
        if (signatureAndHashAlgorithm == null) {
            makeSigner.update(bArr2, 16, 20);
        } else {
            makeSigner.update(bArr2, 0, bArr2.length);
        }
        return makeSigner.verifySignature(bArr);
    }
}
