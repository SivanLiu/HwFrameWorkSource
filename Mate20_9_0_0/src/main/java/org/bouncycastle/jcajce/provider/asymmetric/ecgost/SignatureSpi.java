package org.bouncycastle.jcajce.provider.asymmetric.ecgost;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DSA;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.GOST3411Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.ECGOST3410Signer;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.jcajce.provider.asymmetric.util.GOST3410Util;
import org.bouncycastle.jce.interfaces.ECKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.interfaces.GOST3410Key;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class SignatureSpi extends java.security.SignatureSpi implements PKCSObjectIdentifiers, X509ObjectIdentifiers {
    private Digest digest = new GOST3411Digest();
    private DSA signer = new ECGOST3410Signer();

    static AsymmetricKeyParameter generatePublicKeyParameter(PublicKey publicKey) throws InvalidKeyException {
        return publicKey instanceof BCECGOST3410PublicKey ? ((BCECGOST3410PublicKey) publicKey).engineGetKeyParameters() : ECUtil.generatePublicKeyParameter(publicKey);
    }

    protected Object engineGetParameter(String str) {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        CipherParameters generatePrivateKeyParameter = privateKey instanceof ECKey ? ECUtil.generatePrivateKeyParameter(privateKey) : GOST3410Util.generatePrivateKeyParameter(privateKey);
        this.digest.reset();
        if (this.appRandom != null) {
            this.signer.init(true, new ParametersWithRandom(generatePrivateKeyParameter, this.appRandom));
        } else {
            this.signer.init(true, generatePrivateKeyParameter);
        }
    }

    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        CipherParameters generatePublicKeyParameter;
        if (publicKey instanceof ECPublicKey) {
            generatePublicKeyParameter = generatePublicKeyParameter(publicKey);
        } else if (publicKey instanceof GOST3410Key) {
            generatePublicKeyParameter = GOST3410Util.generatePublicKeyParameter(publicKey);
        } else {
            try {
                generatePublicKeyParameter = ECUtil.generatePublicKeyParameter(BouncyCastleProvider.getPublicKey(SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())));
            } catch (Exception e) {
                throw new InvalidKeyException("can't recognise key type in DSA based signer");
            }
        }
        this.digest.reset();
        this.signer.init(false, generatePublicKeyParameter);
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
            Object obj = new byte[64];
            BigInteger[] generateSignature = this.signer.generateSignature(bArr);
            Object toByteArray = generateSignature[0].toByteArray();
            Object toByteArray2 = generateSignature[1].toByteArray();
            if (toByteArray2[0] != (byte) 0) {
                System.arraycopy(toByteArray2, 0, obj, 32 - toByteArray2.length, toByteArray2.length);
            } else {
                System.arraycopy(toByteArray2, 1, obj, 32 - (toByteArray2.length - 1), toByteArray2.length - 1);
            }
            if (toByteArray[0] != (byte) 0) {
                System.arraycopy(toByteArray, 0, obj, 64 - toByteArray.length, toByteArray.length);
                return obj;
            }
            System.arraycopy(toByteArray, 1, obj, 64 - (toByteArray.length - 1), toByteArray.length - 1);
            return obj;
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
            Object obj = new byte[32];
            System.arraycopy(bArr, 0, new byte[32], 0, 32);
            System.arraycopy(bArr, 32, obj, 0, 32);
            BigInteger[] bigIntegerArr = new BigInteger[]{new BigInteger(1, obj), new BigInteger(1, r4)};
            return this.signer.verifySignature(bArr2, bigIntegerArr[0], bigIntegerArr[1]);
        } catch (Exception e) {
            throw new SignatureException("error decoding signature bytes.");
        }
    }
}
