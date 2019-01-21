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
                byte[] bArr2;
                byte[] bArr3;
                if (this.key instanceof ElGamalPrivateKeyParameters) {
                    i2 /= 2;
                    bArr2 = new byte[i2];
                    bArr3 = new byte[i2];
                    System.arraycopy(bArr, i, bArr2, 0, bArr2.length);
                    System.arraycopy(bArr, i + bArr2.length, bArr3, 0, bArr3.length);
                    return BigIntegers.asUnsignedByteArray(new BigInteger(1, bArr2).modPow(p.subtract(ONE).subtract(((ElGamalPrivateKeyParameters) this.key).getX()), p).multiply(new BigInteger(1, bArr3)).mod(p));
                }
                if (!(i == 0 && i2 == bArr.length)) {
                    bArr2 = new byte[i2];
                    System.arraycopy(bArr, i, bArr2, 0, i2);
                    bArr = bArr2;
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
                    byte[] toByteArray = modPow.toByteArray();
                    bArr = mod.toByteArray();
                    bArr3 = new byte[getOutputBlockSize()];
                    if (toByteArray.length > bArr3.length / 2) {
                        System.arraycopy(toByteArray, 1, bArr3, (bArr3.length / 2) - (toByteArray.length - 1), toByteArray.length - 1);
                    } else {
                        System.arraycopy(toByteArray, 0, bArr3, (bArr3.length / 2) - toByteArray.length, toByteArray.length);
                    }
                    if (bArr.length > bArr3.length / 2) {
                        System.arraycopy(bArr, 1, bArr3, bArr3.length - (bArr.length - 1), bArr.length - 1);
                        return bArr3;
                    }
                    System.arraycopy(bArr, 0, bArr3, bArr3.length - bArr.length, bArr.length);
                    return bArr3;
                }
                throw new DataLengthException("input too large for ElGamal cipher.\n");
            }
            throw new DataLengthException("input too large for ElGamal cipher.\n");
        }
        throw new IllegalStateException("ElGamal engine not initialised");
    }
}
