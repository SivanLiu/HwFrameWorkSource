package org.bouncycastle.pqc.jcajce.provider.xmss;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.digests.SHAKEDigest;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.pqc.crypto.xmss.XMSSPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.xmss.XMSSSigner;
import org.bouncycastle.pqc.jcajce.interfaces.StateAwareSignature;

public class XMSSSignatureSpi extends Signature implements StateAwareSignature {
    private Digest digest;
    private SecureRandom random;
    private XMSSSigner signer;
    private ASN1ObjectIdentifier treeDigest;

    public static class withSha256 extends XMSSSignatureSpi {
        public withSha256() {
            super("SHA256withXMSS", new SHA256Digest(), new XMSSSigner());
        }
    }

    public static class withSha512 extends XMSSSignatureSpi {
        public withSha512() {
            super("SHA512withXMSS", new SHA512Digest(), new XMSSSigner());
        }
    }

    public static class withShake128 extends XMSSSignatureSpi {
        public withShake128() {
            super("SHAKE128withXMSSMT", new SHAKEDigest(128), new XMSSSigner());
        }
    }

    public static class withShake256 extends XMSSSignatureSpi {
        public withShake256() {
            super("SHAKE256withXMSS", new SHAKEDigest(256), new XMSSSigner());
        }
    }

    protected XMSSSignatureSpi(String str) {
        super(str);
    }

    protected XMSSSignatureSpi(String str, Digest digest, XMSSSigner xMSSSigner) {
        super(str);
        this.digest = digest;
        this.signer = xMSSSigner;
    }

    protected Object engineGetParameter(String str) {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        if (privateKey instanceof BCXMSSPrivateKey) {
            BCXMSSPrivateKey bCXMSSPrivateKey = (BCXMSSPrivateKey) privateKey;
            CipherParameters keyParams = bCXMSSPrivateKey.getKeyParams();
            this.treeDigest = bCXMSSPrivateKey.getTreeDigestOID();
            CipherParameters parametersWithRandom = this.random != null ? new ParametersWithRandom(keyParams, this.random) : keyParams;
            this.digest.reset();
            this.signer.init(true, parametersWithRandom);
            return;
        }
        throw new InvalidKeyException("unknown private key passed to XMSS");
    }

    protected void engineInitSign(PrivateKey privateKey, SecureRandom secureRandom) throws InvalidKeyException {
        this.random = secureRandom;
        engineInitSign(privateKey);
    }

    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        if (publicKey instanceof BCXMSSPublicKey) {
            CipherParameters keyParams = ((BCXMSSPublicKey) publicKey).getKeyParams();
            this.treeDigest = null;
            this.digest.reset();
            this.signer.init(false, keyParams);
            return;
        }
        throw new InvalidKeyException("unknown public key passed to XMSS");
    }

    protected void engineSetParameter(String str, Object obj) {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    protected void engineSetParameter(AlgorithmParameterSpec algorithmParameterSpec) {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    protected byte[] engineSign() throws SignatureException {
        try {
            return this.signer.generateSignature(DigestUtil.getDigestResult(this.digest));
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw new SignatureException(e.getMessage());
            }
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
        return this.signer.verifySignature(DigestUtil.getDigestResult(this.digest), bArr);
    }

    public PrivateKey getUpdatedPrivateKey() {
        if (this.treeDigest != null) {
            PrivateKey bCXMSSPrivateKey = new BCXMSSPrivateKey(this.treeDigest, (XMSSPrivateKeyParameters) this.signer.getUpdatedPrivateKey());
            this.treeDigest = null;
            return bCXMSSPrivateKey;
        }
        throw new IllegalStateException("signature object not in a signing state");
    }
}
