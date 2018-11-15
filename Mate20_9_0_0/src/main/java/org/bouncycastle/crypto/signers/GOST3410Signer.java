package org.bouncycastle.crypto.signers;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DSA;
import org.bouncycastle.crypto.params.GOST3410KeyParameters;
import org.bouncycastle.crypto.params.GOST3410Parameters;
import org.bouncycastle.crypto.params.GOST3410PrivateKeyParameters;
import org.bouncycastle.crypto.params.GOST3410PublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;

public class GOST3410Signer implements DSA {
    GOST3410KeyParameters key;
    SecureRandom random;

    public BigInteger[] generateSignature(byte[] bArr) {
        BigInteger bigInteger;
        byte[] bArr2 = new byte[bArr.length];
        for (int i = 0; i != bArr2.length; i++) {
            bArr2[i] = bArr[(bArr2.length - 1) - i];
        }
        BigInteger bigInteger2 = new BigInteger(1, bArr2);
        GOST3410Parameters parameters = this.key.getParameters();
        do {
            bigInteger = new BigInteger(parameters.getQ().bitLength(), this.random);
        } while (bigInteger.compareTo(parameters.getQ()) >= 0);
        bigInteger2 = bigInteger.multiply(bigInteger2).add(((GOST3410PrivateKeyParameters) this.key).getX().multiply(parameters.getA().modPow(bigInteger, parameters.getP()).mod(parameters.getQ()))).mod(parameters.getQ());
        return new BigInteger[]{r3, bigInteger2};
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        GOST3410KeyParameters gOST3410KeyParameters;
        if (!z) {
            gOST3410KeyParameters = (GOST3410PublicKeyParameters) cipherParameters;
        } else if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.random = parametersWithRandom.getRandom();
            this.key = (GOST3410PrivateKeyParameters) parametersWithRandom.getParameters();
            return;
        } else {
            this.random = new SecureRandom();
            gOST3410KeyParameters = (GOST3410PrivateKeyParameters) cipherParameters;
        }
        this.key = gOST3410KeyParameters;
    }

    /* JADX WARNING: Missing block: B:16:0x00b2, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean verifySignature(byte[] bArr, BigInteger bigInteger, BigInteger bigInteger2) {
        byte[] bArr2 = new byte[bArr.length];
        for (int i = 0; i != bArr2.length; i++) {
            bArr2[i] = bArr[(bArr2.length - 1) - i];
        }
        BigInteger bigInteger3 = new BigInteger(1, bArr2);
        GOST3410Parameters parameters = this.key.getParameters();
        BigInteger valueOf = BigInteger.valueOf(0);
        if (valueOf.compareTo(bigInteger) >= 0 || parameters.getQ().compareTo(bigInteger) <= 0 || valueOf.compareTo(bigInteger2) >= 0 || parameters.getQ().compareTo(bigInteger2) <= 0) {
            return false;
        }
        bigInteger3 = bigInteger3.modPow(parameters.getQ().subtract(new BigInteger("2")), parameters.getQ());
        return parameters.getA().modPow(bigInteger2.multiply(bigInteger3).mod(parameters.getQ()), parameters.getP()).multiply(((GOST3410PublicKeyParameters) this.key).getY().modPow(parameters.getQ().subtract(bigInteger).multiply(bigInteger3).mod(parameters.getQ()), parameters.getP())).mod(parameters.getP()).mod(parameters.getQ()).equals(bigInteger);
    }
}
