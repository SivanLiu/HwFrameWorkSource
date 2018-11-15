package org.bouncycastle.jcajce.provider.asymmetric.dh;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.DerivationFunction;
import org.bouncycastle.crypto.agreement.kdf.DHKEKGenerator;
import org.bouncycastle.crypto.util.DigestFactory;
import org.bouncycastle.jcajce.provider.asymmetric.util.BaseAgreementSpi;
import org.bouncycastle.jcajce.spec.UserKeyingMaterialSpec;

public class KeyAgreementSpi extends BaseAgreementSpi {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private BigInteger g;
    private BigInteger p;
    private BigInteger result;
    private BigInteger x;

    public static class DHwithRFC2631KDF extends KeyAgreementSpi {
        public DHwithRFC2631KDF() {
            super("DHwithRFC2631KDF", new DHKEKGenerator(DigestFactory.createSHA1()));
        }
    }

    public KeyAgreementSpi() {
        super("Diffie-Hellman", null);
    }

    public KeyAgreementSpi(String str, DerivationFunction derivationFunction) {
        super(str, derivationFunction);
    }

    protected byte[] bigIntToBytes(BigInteger bigInteger) {
        int bitLength = (this.p.bitLength() + 7) / 8;
        Object toByteArray = bigInteger.toByteArray();
        if (toByteArray.length == bitLength) {
            return toByteArray;
        }
        Object obj;
        if (toByteArray[0] == (byte) 0 && toByteArray.length == bitLength + 1) {
            obj = new byte[(toByteArray.length - 1)];
            System.arraycopy(toByteArray, 1, obj, 0, obj.length);
            return obj;
        }
        obj = new byte[bitLength];
        System.arraycopy(toByteArray, 0, obj, obj.length - toByteArray.length, toByteArray.length);
        return obj;
    }

    protected byte[] calcSecret() {
        return bigIntToBytes(this.result);
    }

    protected Key engineDoPhase(Key key, boolean z) throws InvalidKeyException, IllegalStateException {
        if (this.x == null) {
            throw new IllegalStateException("Diffie-Hellman not initialised.");
        } else if (key instanceof DHPublicKey) {
            DHPublicKey dHPublicKey = (DHPublicKey) key;
            if (dHPublicKey.getParams().getG().equals(this.g) && dHPublicKey.getParams().getP().equals(this.p)) {
                BigInteger y = dHPublicKey.getY();
                if (y == null || y.compareTo(TWO) < 0 || y.compareTo(this.p.subtract(ONE)) >= 0) {
                    throw new InvalidKeyException("Invalid DH PublicKey");
                }
                this.result = y.modPow(this.x, this.p);
                if (this.result.compareTo(ONE) != 0) {
                    return z ? null : new BCDHPublicKey(this.result, dHPublicKey.getParams());
                } else {
                    throw new InvalidKeyException("Shared key can't be 1");
                }
            }
            throw new InvalidKeyException("DHPublicKey not for this KeyAgreement!");
        } else {
            throw new InvalidKeyException("DHKeyAgreement doPhase requires DHPublicKey");
        }
    }

    protected int engineGenerateSecret(byte[] bArr, int i) throws IllegalStateException, ShortBufferException {
        if (this.x != null) {
            return super.engineGenerateSecret(bArr, i);
        }
        throw new IllegalStateException("Diffie-Hellman not initialised.");
    }

    protected SecretKey engineGenerateSecret(String str) throws NoSuchAlgorithmException {
        if (this.x != null) {
            return str.equals("TlsPremasterSecret") ? new SecretKeySpec(BaseAgreementSpi.trimZeroes(bigIntToBytes(this.result)), str) : super.engineGenerateSecret(str);
        } else {
            throw new IllegalStateException("Diffie-Hellman not initialised.");
        }
    }

    protected byte[] engineGenerateSecret() throws IllegalStateException {
        if (this.x != null) {
            return super.engineGenerateSecret();
        }
        throw new IllegalStateException("Diffie-Hellman not initialised.");
    }

    protected void engineInit(Key key, SecureRandom secureRandom) throws InvalidKeyException {
        if (key instanceof DHPrivateKey) {
            DHPrivateKey dHPrivateKey = (DHPrivateKey) key;
            this.p = dHPrivateKey.getParams().getP();
            this.g = dHPrivateKey.getParams().getG();
            BigInteger x = dHPrivateKey.getX();
            this.result = x;
            this.x = x;
            return;
        }
        throw new InvalidKeyException("DHKeyAgreement requires DHPrivateKey");
    }

    protected void engineInit(Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (key instanceof DHPrivateKey) {
            DHParameterSpec params;
            BigInteger x;
            DHPrivateKey dHPrivateKey = (DHPrivateKey) key;
            if (algorithmParameterSpec == null) {
                this.p = dHPrivateKey.getParams().getP();
                params = dHPrivateKey.getParams();
            } else if (algorithmParameterSpec instanceof DHParameterSpec) {
                params = (DHParameterSpec) algorithmParameterSpec;
                this.p = params.getP();
            } else if (algorithmParameterSpec instanceof UserKeyingMaterialSpec) {
                this.p = dHPrivateKey.getParams().getP();
                this.g = dHPrivateKey.getParams().getG();
                this.ukmParameters = ((UserKeyingMaterialSpec) algorithmParameterSpec).getUserKeyingMaterial();
                x = dHPrivateKey.getX();
                this.result = x;
                this.x = x;
                return;
            } else {
                throw new InvalidAlgorithmParameterException("DHKeyAgreement only accepts DHParameterSpec");
            }
            this.g = params.getG();
            x = dHPrivateKey.getX();
            this.result = x;
            this.x = x;
            return;
        }
        throw new InvalidKeyException("DHKeyAgreement requires DHPrivateKey for initialisation");
    }
}
