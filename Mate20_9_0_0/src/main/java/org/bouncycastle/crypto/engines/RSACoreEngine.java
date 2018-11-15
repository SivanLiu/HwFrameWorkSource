package org.bouncycastle.crypto.engines;

import java.math.BigInteger;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

class RSACoreEngine {
    private boolean forEncryption;
    private RSAKeyParameters key;

    RSACoreEngine() {
    }

    public BigInteger convertInput(byte[] bArr, int i, int i2) {
        if (i2 > getInputBlockSize() + 1) {
            throw new DataLengthException("input too large for RSA cipher.");
        } else if (i2 != getInputBlockSize() + 1 || this.forEncryption) {
            if (!(i == 0 && i2 == bArr.length)) {
                Object obj = new byte[i2];
                System.arraycopy(bArr, i, obj, 0, i2);
                bArr = obj;
            }
            BigInteger bigInteger = new BigInteger(1, bArr);
            if (bigInteger.compareTo(this.key.getModulus()) < 0) {
                return bigInteger;
            }
            throw new DataLengthException("input too large for RSA cipher.");
        } else {
            throw new DataLengthException("input too large for RSA cipher.");
        }
    }

    public byte[] convertOutput(BigInteger bigInteger) {
        Object toByteArray = bigInteger.toByteArray();
        Object obj;
        if (this.forEncryption) {
            if (toByteArray[0] == (byte) 0 && toByteArray.length > getOutputBlockSize()) {
                obj = new byte[(toByteArray.length - 1)];
                System.arraycopy(toByteArray, 1, obj, 0, obj.length);
                return obj;
            } else if (toByteArray.length < getOutputBlockSize()) {
                obj = new byte[getOutputBlockSize()];
                System.arraycopy(toByteArray, 0, obj, obj.length - toByteArray.length, toByteArray.length);
                return obj;
            }
        } else if (toByteArray[0] == (byte) 0) {
            obj = new byte[(toByteArray.length - 1)];
            System.arraycopy(toByteArray, 1, obj, 0, obj.length);
            return obj;
        }
        return toByteArray;
    }

    public int getInputBlockSize() {
        int bitLength = this.key.getModulus().bitLength();
        return this.forEncryption ? ((bitLength + 7) / 8) - 1 : (bitLength + 7) / 8;
    }

    public int getOutputBlockSize() {
        int bitLength = this.key.getModulus().bitLength();
        return this.forEncryption ? (bitLength + 7) / 8 : ((bitLength + 7) / 8) - 1;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        if (cipherParameters instanceof ParametersWithRandom) {
            cipherParameters = ((ParametersWithRandom) cipherParameters).getParameters();
        }
        this.key = (RSAKeyParameters) cipherParameters;
        this.forEncryption = z;
    }

    public BigInteger processBlock(BigInteger bigInteger) {
        if (!(this.key instanceof RSAPrivateCrtKeyParameters)) {
            return bigInteger.modPow(this.key.getExponent(), this.key.getModulus());
        }
        RSAPrivateCrtKeyParameters rSAPrivateCrtKeyParameters = (RSAPrivateCrtKeyParameters) this.key;
        BigInteger p = rSAPrivateCrtKeyParameters.getP();
        BigInteger q = rSAPrivateCrtKeyParameters.getQ();
        BigInteger dp = rSAPrivateCrtKeyParameters.getDP();
        BigInteger dq = rSAPrivateCrtKeyParameters.getDQ();
        BigInteger qInv = rSAPrivateCrtKeyParameters.getQInv();
        dp = bigInteger.remainder(p).modPow(dp, p);
        bigInteger = bigInteger.remainder(q).modPow(dq, q);
        return dp.subtract(bigInteger).multiply(qInv).mod(p).multiply(q).add(bigInteger);
    }
}
