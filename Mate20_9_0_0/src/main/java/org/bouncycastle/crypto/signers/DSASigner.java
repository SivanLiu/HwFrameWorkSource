package org.bouncycastle.crypto.signers;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DSA;
import org.bouncycastle.crypto.params.DSAKeyParameters;
import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;

public class DSASigner implements DSA {
    private final DSAKCalculator kCalculator;
    private DSAKeyParameters key;
    private SecureRandom random;

    public DSASigner() {
        this.kCalculator = new RandomDSAKCalculator();
    }

    public DSASigner(DSAKCalculator dSAKCalculator) {
        this.kCalculator = dSAKCalculator;
    }

    private BigInteger calculateE(BigInteger bigInteger, byte[] bArr) {
        if (bigInteger.bitLength() >= bArr.length * 8) {
            return new BigInteger(1, bArr);
        }
        Object obj = new byte[(bigInteger.bitLength() / 8)];
        System.arraycopy(bArr, 0, obj, 0, obj.length);
        return new BigInteger(1, obj);
    }

    private BigInteger getRandomizer(BigInteger bigInteger, SecureRandom secureRandom) {
        Random secureRandom2;
        if (secureRandom2 == null) {
            secureRandom2 = new SecureRandom();
        }
        return new BigInteger(7, secureRandom2).add(BigInteger.valueOf(128)).multiply(bigInteger);
    }

    public BigInteger[] generateSignature(byte[] bArr) {
        DSAParameters parameters = this.key.getParameters();
        BigInteger q = parameters.getQ();
        BigInteger calculateE = calculateE(q, bArr);
        BigInteger x = ((DSAPrivateKeyParameters) this.key).getX();
        if (this.kCalculator.isDeterministic()) {
            this.kCalculator.init(q, x, bArr);
        } else {
            this.kCalculator.init(q, this.random);
        }
        BigInteger nextK = this.kCalculator.nextK();
        nextK = nextK.modInverse(q).multiply(calculateE.add(x.multiply(parameters.getG().modPow(nextK.add(getRandomizer(q, this.random)), parameters.getP()).mod(q)))).mod(q);
        return new BigInteger[]{r0, nextK};
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        DSAKeyParameters dSAKeyParameters;
        SecureRandom random;
        if (!z) {
            dSAKeyParameters = (DSAPublicKeyParameters) cipherParameters;
        } else if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.key = (DSAPrivateKeyParameters) parametersWithRandom.getParameters();
            random = parametersWithRandom.getRandom();
            z = z && !this.kCalculator.isDeterministic();
            this.random = initSecureRandom(z, random);
        } else {
            dSAKeyParameters = (DSAPrivateKeyParameters) cipherParameters;
        }
        this.key = dSAKeyParameters;
        random = null;
        if (!z) {
        }
        this.random = initSecureRandom(z, random);
    }

    protected SecureRandom initSecureRandom(boolean z, SecureRandom secureRandom) {
        return !z ? null : secureRandom != null ? secureRandom : new SecureRandom();
    }

    /* JADX WARNING: Missing block: B:12:0x006c, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean verifySignature(byte[] bArr, BigInteger bigInteger, BigInteger bigInteger2) {
        DSAParameters parameters = this.key.getParameters();
        BigInteger q = parameters.getQ();
        BigInteger calculateE = calculateE(q, bArr);
        BigInteger valueOf = BigInteger.valueOf(0);
        if (valueOf.compareTo(bigInteger) >= 0 || q.compareTo(bigInteger) <= 0 || valueOf.compareTo(bigInteger2) >= 0 || q.compareTo(bigInteger2) <= 0) {
            return false;
        }
        bigInteger2 = bigInteger2.modInverse(q);
        calculateE = calculateE.multiply(bigInteger2).mod(q);
        bigInteger2 = bigInteger.multiply(bigInteger2).mod(q);
        valueOf = parameters.getP();
        return parameters.getG().modPow(calculateE, valueOf).multiply(((DSAPublicKeyParameters) this.key).getY().modPow(bigInteger2, valueOf)).mod(valueOf).mod(q).equals(bigInteger);
    }
}
