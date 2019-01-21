package org.bouncycastle.crypto.engines;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.params.CramerShoupKeyParameters;
import org.bouncycastle.crypto.params.CramerShoupPrivateKeyParameters;
import org.bouncycastle.crypto.params.CramerShoupPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.util.BigIntegers;

public class CramerShoupCoreEngine {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private boolean forEncryption;
    private CramerShoupKeyParameters key;
    private String label = null;
    private SecureRandom random;

    public static class CramerShoupCiphertextException extends Exception {
        private static final long serialVersionUID = -6360977166495345076L;

        public CramerShoupCiphertextException(String str) {
            super(str);
        }
    }

    private BigInteger generateRandomElement(BigInteger bigInteger, SecureRandom secureRandom) {
        return BigIntegers.createRandomInRange(ONE, bigInteger.subtract(ONE), secureRandom);
    }

    private boolean isValidMessage(BigInteger bigInteger, BigInteger bigInteger2) {
        return bigInteger.compareTo(bigInteger2) < 0;
    }

    public BigInteger convertInput(byte[] bArr, int i, int i2) {
        if (i2 > getInputBlockSize() + 1) {
            throw new DataLengthException("input too large for Cramer Shoup cipher.");
        } else if (i2 == getInputBlockSize() + 1 && this.forEncryption) {
            throw new DataLengthException("input too large for Cramer Shoup cipher.");
        } else {
            if (!(i == 0 && i2 == bArr.length)) {
                byte[] bArr2 = new byte[i2];
                System.arraycopy(bArr, i, bArr2, 0, i2);
                bArr = bArr2;
            }
            BigInteger bigInteger = new BigInteger(1, bArr);
            if (bigInteger.compareTo(this.key.getParameters().getP()) < 0) {
                return bigInteger;
            }
            throw new DataLengthException("input too large for Cramer Shoup cipher.");
        }
    }

    public byte[] convertOutput(BigInteger bigInteger) {
        byte[] toByteArray = bigInteger.toByteArray();
        byte[] bArr;
        if (this.forEncryption) {
            if (toByteArray[0] == (byte) 0) {
                bArr = new byte[(toByteArray.length - 1)];
                System.arraycopy(toByteArray, 1, bArr, 0, bArr.length);
                return bArr;
            }
        } else if (toByteArray[0] == (byte) 0 && toByteArray.length > getOutputBlockSize()) {
            bArr = new byte[(toByteArray.length - 1)];
            System.arraycopy(toByteArray, 1, bArr, 0, bArr.length);
            return bArr;
        } else if (toByteArray.length < getOutputBlockSize()) {
            bArr = new byte[getOutputBlockSize()];
            System.arraycopy(toByteArray, 0, bArr, bArr.length - toByteArray.length, toByteArray.length);
            return bArr;
        }
        return toByteArray;
    }

    public BigInteger decryptBlock(CramerShoupCiphertext cramerShoupCiphertext) throws CramerShoupCiphertextException {
        if (!this.key.isPrivate() || this.forEncryption || !(this.key instanceof CramerShoupPrivateKeyParameters)) {
            return null;
        }
        CramerShoupPrivateKeyParameters cramerShoupPrivateKeyParameters = (CramerShoupPrivateKeyParameters) this.key;
        BigInteger p = cramerShoupPrivateKeyParameters.getParameters().getP();
        Digest h = cramerShoupPrivateKeyParameters.getParameters().getH();
        byte[] toByteArray = cramerShoupCiphertext.getU1().toByteArray();
        h.update(toByteArray, 0, toByteArray.length);
        toByteArray = cramerShoupCiphertext.getU2().toByteArray();
        h.update(toByteArray, 0, toByteArray.length);
        toByteArray = cramerShoupCiphertext.getE().toByteArray();
        h.update(toByteArray, 0, toByteArray.length);
        if (this.label != null) {
            toByteArray = this.label.getBytes();
            h.update(toByteArray, 0, toByteArray.length);
        }
        toByteArray = new byte[h.getDigestSize()];
        h.doFinal(toByteArray, 0);
        BigInteger bigInteger = new BigInteger(1, toByteArray);
        if (cramerShoupCiphertext.v.equals(cramerShoupCiphertext.u1.modPow(cramerShoupPrivateKeyParameters.getX1().add(cramerShoupPrivateKeyParameters.getY1().multiply(bigInteger)), p).multiply(cramerShoupCiphertext.u2.modPow(cramerShoupPrivateKeyParameters.getX2().add(cramerShoupPrivateKeyParameters.getY2().multiply(bigInteger)), p)).mod(p))) {
            return cramerShoupCiphertext.e.multiply(cramerShoupCiphertext.u1.modPow(cramerShoupPrivateKeyParameters.getZ(), p).modInverse(p)).mod(p);
        }
        throw new CramerShoupCiphertextException("Sorry, that ciphertext is not correct");
    }

    public CramerShoupCiphertext encryptBlock(BigInteger bigInteger) {
        CramerShoupCiphertext cramerShoupCiphertext = null;
        if (!this.key.isPrivate() && this.forEncryption && (this.key instanceof CramerShoupPublicKeyParameters)) {
            CramerShoupPublicKeyParameters cramerShoupPublicKeyParameters = (CramerShoupPublicKeyParameters) this.key;
            BigInteger p = cramerShoupPublicKeyParameters.getParameters().getP();
            BigInteger g1 = cramerShoupPublicKeyParameters.getParameters().getG1();
            BigInteger g2 = cramerShoupPublicKeyParameters.getParameters().getG2();
            BigInteger h = cramerShoupPublicKeyParameters.getH();
            if (!isValidMessage(bigInteger, p)) {
                return null;
            }
            BigInteger generateRandomElement = generateRandomElement(p, this.random);
            g1 = g1.modPow(generateRandomElement, p);
            g2 = g2.modPow(generateRandomElement, p);
            bigInteger = h.modPow(generateRandomElement, p).multiply(bigInteger).mod(p);
            Digest h2 = cramerShoupPublicKeyParameters.getParameters().getH();
            byte[] toByteArray = g1.toByteArray();
            h2.update(toByteArray, 0, toByteArray.length);
            toByteArray = g2.toByteArray();
            h2.update(toByteArray, 0, toByteArray.length);
            toByteArray = bigInteger.toByteArray();
            h2.update(toByteArray, 0, toByteArray.length);
            if (this.label != null) {
                toByteArray = this.label.getBytes();
                h2.update(toByteArray, 0, toByteArray.length);
            }
            toByteArray = new byte[h2.getDigestSize()];
            h2.doFinal(toByteArray, 0);
            cramerShoupCiphertext = new CramerShoupCiphertext(g1, g2, bigInteger, cramerShoupPublicKeyParameters.getC().modPow(generateRandomElement, p).multiply(cramerShoupPublicKeyParameters.getD().modPow(generateRandomElement.multiply(new BigInteger(1, toByteArray)), p)).mod(p));
        }
        return cramerShoupCiphertext;
    }

    public int getInputBlockSize() {
        int bitLength = this.key.getParameters().getP().bitLength();
        return this.forEncryption ? ((bitLength + 7) / 8) - 1 : (bitLength + 7) / 8;
    }

    public int getOutputBlockSize() {
        int bitLength = this.key.getParameters().getP().bitLength();
        return this.forEncryption ? (bitLength + 7) / 8 : ((bitLength + 7) / 8) - 1;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        SecureRandom random;
        if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.key = (CramerShoupKeyParameters) parametersWithRandom.getParameters();
            random = parametersWithRandom.getRandom();
        } else {
            this.key = (CramerShoupKeyParameters) cipherParameters;
            random = null;
        }
        this.random = initSecureRandom(z, random);
        this.forEncryption = z;
    }

    public void init(boolean z, CipherParameters cipherParameters, String str) {
        init(z, cipherParameters);
        this.label = str;
    }

    protected SecureRandom initSecureRandom(boolean z, SecureRandom secureRandom) {
        return !z ? null : secureRandom != null ? secureRandom : new SecureRandom();
    }
}
