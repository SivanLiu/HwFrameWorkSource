package org.bouncycastle.crypto.engines;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.params.ElGamalKeyParameters;
import org.bouncycastle.crypto.params.ElGamalPrivateKeyParameters;
import org.bouncycastle.crypto.params.ElGamalPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.util.BigIntegers;

public class ElGamalEngine implements AsymmetricBlockCipher {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final BigInteger ZERO = BigInteger.valueOf(0);
    private int bitSize;
    private boolean forEncryption;
    private ElGamalKeyParameters key;
    private SecureRandom random;

    public int getInputBlockSize() {
        return this.forEncryption ? (this.bitSize - 1) / 8 : 2 * ((this.bitSize + 7) / 8);
    }

    public int getOutputBlockSize() {
        return this.forEncryption ? 2 * ((this.bitSize + 7) / 8) : (this.bitSize - 1) / 8;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        SecureRandom random;
        if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.key = (ElGamalKeyParameters) parametersWithRandom.getParameters();
            random = parametersWithRandom.getRandom();
        } else {
            this.key = (ElGamalKeyParameters) cipherParameters;
            random = new SecureRandom();
        }
        this.random = random;
        this.forEncryption = z;
        this.bitSize = this.key.getParameters().getP().bitLength();
        if (z) {
            if (!(this.key instanceof ElGamalPublicKeyParameters)) {
                throw new IllegalArgumentException("ElGamalPublicKeyParameters are required for encryption.");
            }
        } else if (!(this.key instanceof ElGamalPrivateKeyParameters)) {
            throw new IllegalArgumentException("ElGamalPrivateKeyParameters are required for decryption.");
        }
    }

    public byte[] processBlock(byte[] bArr, int i, int i2) {
        if (this.key != null) {
            if (i2 <= (this.forEncryption ? ((this.bitSize - 1) + 7) / 8 : getInputBlockSize())) {
                BigInteger p = this.key.getParameters().getP();
                Object obj;
                Object obj2;
                if (this.key instanceof ElGamalPrivateKeyParameters) {
                    i2 /= 2;
                    obj = new byte[i2];
                    obj2 = new byte[i2];
                    System.arraycopy(bArr, i, obj, 0, obj.length);
                    System.arraycopy(bArr, i + obj.length, obj2, 0, obj2.length);
                    return BigIntegers.asUnsignedByteArray(new BigInteger(1, obj).modPow(p.subtract(ONE).subtract(((ElGamalPrivateKeyParameters) this.key).getX()), p).multiply(new BigInteger(1, obj2)).mod(p));
                }
                if (!(i == 0 && i2 == bArr.length)) {
                    obj = new byte[i2];
                    System.arraycopy(bArr, i, obj, 0, i2);
                    bArr = obj;
                }
                BigInteger bigInteger = new BigInteger(1, bArr);
                if (bigInteger.compareTo(p) < 0) {
                    ElGamalPublicKeyParameters elGamalPublicKeyParameters = (ElGamalPublicKeyParameters) this.key;
                    i2 = p.bitLength();
                    BigInteger bigInteger2 = new BigInteger(i2, this.random);
                    while (true) {
                        if (!bigInteger2.equals(ZERO) && bigInteger2.compareTo(p.subtract(TWO)) <= 0) {
                            break;
                        }
                        bigInteger2 = new BigInteger(i2, this.random);
                    }
                    BigInteger modPow = this.key.getParameters().getG().modPow(bigInteger2, p);
                    BigInteger mod = bigInteger.multiply(elGamalPublicKeyParameters.getY().modPow(bigInteger2, p)).mod(p);
                    Object toByteArray = modPow.toByteArray();
                    Object toByteArray2 = mod.toByteArray();
                    obj2 = new byte[getOutputBlockSize()];
                    if (toByteArray.length > obj2.length / 2) {
                        System.arraycopy(toByteArray, 1, obj2, (obj2.length / 2) - (toByteArray.length - 1), toByteArray.length - 1);
                    } else {
                        System.arraycopy(toByteArray, 0, obj2, (obj2.length / 2) - toByteArray.length, toByteArray.length);
                    }
                    if (toByteArray2.length > obj2.length / 2) {
                        System.arraycopy(toByteArray2, 1, obj2, obj2.length - (toByteArray2.length - 1), toByteArray2.length - 1);
                        return obj2;
                    }
                    System.arraycopy(toByteArray2, 0, obj2, obj2.length - toByteArray2.length, toByteArray2.length);
                    return obj2;
                }
                throw new DataLengthException("input too large for ElGamal cipher.\n");
            }
            throw new DataLengthException("input too large for ElGamal cipher.\n");
        }
        throw new IllegalStateException("ElGamal engine not initialised");
    }
}
