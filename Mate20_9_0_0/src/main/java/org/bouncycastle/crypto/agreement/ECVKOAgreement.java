package org.bouncycastle.crypto.agreement;

import java.math.BigInteger;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithUKM;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;

public class ECVKOAgreement {
    private final Digest digest;
    private ECPrivateKeyParameters key;
    private BigInteger ukm;

    public ECVKOAgreement(Digest digest) {
        this.digest = digest;
    }

    private byte[] fromPoint(ECPoint eCPoint) {
        BigInteger toBigInteger = eCPoint.getAffineXCoord().toBigInteger();
        BigInteger toBigInteger2 = eCPoint.getAffineYCoord().toBigInteger();
        int i = toBigInteger.toByteArray().length > 33 ? 64 : 32;
        byte[] bArr = new byte[(2 * i)];
        byte[] asUnsignedByteArray = BigIntegers.asUnsignedByteArray(i, toBigInteger);
        byte[] asUnsignedByteArray2 = BigIntegers.asUnsignedByteArray(i, toBigInteger2);
        for (int i2 = 0; i2 != i; i2++) {
            bArr[i2] = asUnsignedByteArray[(i - i2) - 1];
        }
        for (int i3 = 0; i3 != i; i3++) {
            bArr[i + i3] = asUnsignedByteArray2[(i - i3) - 1];
        }
        this.digest.update(bArr, 0, bArr.length);
        asUnsignedByteArray2 = new byte[this.digest.getDigestSize()];
        this.digest.doFinal(asUnsignedByteArray2, 0);
        return asUnsignedByteArray2;
    }

    private static BigInteger toInteger(byte[] bArr) {
        byte[] bArr2 = new byte[bArr.length];
        for (int i = 0; i != bArr2.length; i++) {
            bArr2[i] = bArr[(bArr.length - i) - 1];
        }
        return new BigInteger(1, bArr2);
    }

    public byte[] calculateAgreement(CipherParameters cipherParameters) {
        ECPublicKeyParameters eCPublicKeyParameters = (ECPublicKeyParameters) cipherParameters;
        ECDomainParameters parameters = eCPublicKeyParameters.getParameters();
        if (parameters.equals(this.key.getParameters())) {
            ECPoint normalize = eCPublicKeyParameters.getQ().multiply(parameters.getH().multiply(this.ukm).multiply(this.key.getD()).mod(parameters.getN())).normalize();
            if (!normalize.isInfinity()) {
                return fromPoint(normalize.normalize());
            }
            throw new IllegalStateException("Infinity is not a valid agreement value for ECVKO");
        }
        throw new IllegalStateException("ECVKO public key has wrong domain parameters");
    }

    public int getFieldSize() {
        return (this.key.getParameters().getCurve().getFieldSize() + 7) / 8;
    }

    public void init(CipherParameters cipherParameters) {
        ParametersWithUKM parametersWithUKM = (ParametersWithUKM) cipherParameters;
        this.key = (ECPrivateKeyParameters) parametersWithUKM.getParameters();
        this.ukm = toInteger(parametersWithUKM.getUKM());
    }
}
