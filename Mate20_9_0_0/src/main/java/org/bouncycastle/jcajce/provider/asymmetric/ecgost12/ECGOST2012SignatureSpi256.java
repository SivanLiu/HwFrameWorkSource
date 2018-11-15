package org.bouncycastle.jcajce.provider.asymmetric.ecgost12;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.spec.AlgorithmParameterSpec;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DSA;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.GOST3411_2012_256Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.ECGOST3410_2012Signer;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.jce.interfaces.ECKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class ECGOST2012SignatureSpi256 extends SignatureSpi implements PKCSObjectIdentifiers, X509ObjectIdentifiers {
    private Digest digest = new GOST3411_2012_256Digest();
    private int halfSize = (this.size / 2);
    private DSA signer = new ECGOST3410_2012Signer();
    private int size = 64;

    static AsymmetricKeyParameter generatePublicKeyParameter(PublicKey publicKey) throws InvalidKeyException {
        return publicKey instanceof BCECGOST3410_2012PublicKey ? ((BCECGOST3410_2012PublicKey) publicKey).engineGetKeyParameters() : ECUtil.generatePublicKeyParameter(publicKey);
    }

    protected Object engineGetParameter(String str) {
        throw new UnsupportedOperationException("engineSetParameter unsupported");
    }

    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        if (privateKey instanceof ECKey) {
            CipherParameters generatePrivateKeyParameter = ECUtil.generatePrivateKeyParameter(privateKey);
            this.digest.reset();
            if (this.appRandom != null) {
                this.signer.init(true, new ParametersWithRandom(generatePrivateKeyParameter, this.appRandom));
                return;
            } else {
                this.signer.init(true, generatePrivateKeyParameter);
                return;
            }
        }
        throw new InvalidKeyException("cannot recognise key type in ECGOST-2012-256 signer");
    }

    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        CipherParameters generatePublicKeyParameter;
        if (publicKey instanceof ECPublicKey) {
            generatePublicKeyParameter = generatePublicKeyParameter(publicKey);
        } else {
            try {
                generatePublicKeyParameter = ECUtil.generatePublicKeyParameter(BouncyCastleProvider.getPublicKey(SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())));
            } catch (Exception e) {
                throw new InvalidKeyException("cannot recognise key type in ECGOST-2012-256 signer");
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
            Object obj = new byte[this.size];
            BigInteger[] generateSignature = this.signer.generateSignature(bArr);
            Object toByteArray = generateSignature[0].toByteArray();
            Object toByteArray2 = generateSignature[1].toByteArray();
            if (toByteArray2[0] != (byte) 0) {
                System.arraycopy(toByteArray2, 0, obj, this.halfSize - toByteArray2.length, toByteArray2.length);
            } else {
                System.arraycopy(toByteArray2, 1, obj, this.halfSize - (toByteArray2.length - 1), toByteArray2.length - 1);
            }
            if (toByteArray[0] != (byte) 0) {
                System.arraycopy(toByteArray, 0, obj, this.size - toByteArray.length, toByteArray.length);
                return obj;
            }
            System.arraycopy(toByteArray, 1, obj, this.size - (toByteArray.length - 1), toByteArray.length - 1);
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
            Object obj = new byte[this.halfSize];
            System.arraycopy(bArr, 0, new byte[this.halfSize], 0, this.halfSize);
            System.arraycopy(bArr, this.halfSize, obj, 0, this.halfSize);
            BigInteger[] bigIntegerArr = new BigInteger[]{new BigInteger(1, obj), new BigInteger(1, r3)};
            return this.signer.verifySignature(bArr2, bigIntegerArr[0], bigIntegerArr[1]);
        } catch (Exception e) {
            throw new SignatureException("error decoding signature bytes.");
        }
    }
}
