package org.bouncycastle.crypto.signers;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DSA;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECConstants;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECMultiplier;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;

public class ECDSASigner implements ECConstants, DSA {
    private final DSAKCalculator kCalculator;
    private ECKeyParameters key;
    private SecureRandom random;

    public ECDSASigner() {
        this.kCalculator = new RandomDSAKCalculator();
    }

    public ECDSASigner(DSAKCalculator dSAKCalculator) {
        this.kCalculator = dSAKCalculator;
    }

    protected BigInteger calculateE(BigInteger bigInteger, byte[] bArr) {
        int bitLength = bigInteger.bitLength();
        int length = bArr.length * 8;
        BigInteger bigInteger2 = new BigInteger(1, bArr);
        return bitLength < length ? bigInteger2.shiftRight(length - bitLength) : bigInteger2;
    }

    protected ECMultiplier createBasePointMultiplier() {
        return new FixedPointCombMultiplier();
    }

    public BigInteger[] generateSignature(byte[] bArr) {
        ECDomainParameters parameters = this.key.getParameters();
        BigInteger n = parameters.getN();
        BigInteger calculateE = calculateE(n, bArr);
        BigInteger d = ((ECPrivateKeyParameters) this.key).getD();
        if (this.kCalculator.isDeterministic()) {
            this.kCalculator.init(n, d, bArr);
        } else {
            this.kCalculator.init(n, this.random);
        }
        ECMultiplier createBasePointMultiplier = createBasePointMultiplier();
        while (true) {
            BigInteger nextK = this.kCalculator.nextK();
            BigInteger mod = createBasePointMultiplier.multiply(parameters.getG(), nextK).normalize().getAffineXCoord().toBigInteger().mod(n);
            if (!mod.equals(ZERO)) {
                if (!nextK.modInverse(n).multiply(calculateE.add(d.multiply(mod))).mod(n).equals(ZERO)) {
                    return new BigInteger[]{mod, nextK.modInverse(n).multiply(calculateE.add(d.multiply(mod))).mod(n)};
                }
            }
        }
    }

    protected ECFieldElement getDenominator(int i, ECPoint eCPoint) {
        switch (i) {
            case 1:
            case 6:
            case 7:
                return eCPoint.getZCoord(0);
            case 2:
            case 3:
            case 4:
                return eCPoint.getZCoord(0).square();
            default:
                return null;
        }
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        ECKeyParameters eCKeyParameters;
        SecureRandom random;
        if (!z) {
            eCKeyParameters = (ECPublicKeyParameters) cipherParameters;
        } else if (cipherParameters instanceof ParametersWithRandom) {
            ParametersWithRandom parametersWithRandom = (ParametersWithRandom) cipherParameters;
            this.key = (ECPrivateKeyParameters) parametersWithRandom.getParameters();
            random = parametersWithRandom.getRandom();
            z = z && !this.kCalculator.isDeterministic();
            this.random = initSecureRandom(z, random);
        } else {
            eCKeyParameters = (ECPrivateKeyParameters) cipherParameters;
        }
        this.key = eCKeyParameters;
        random = null;
        if (!z) {
        }
        this.random = initSecureRandom(z, random);
    }

    protected SecureRandom initSecureRandom(boolean z, SecureRandom secureRandom) {
        return !z ? null : secureRandom != null ? secureRandom : new SecureRandom();
    }

    /* JADX WARNING: Missing block: B:34:0x00b1, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean verifySignature(byte[] bArr, BigInteger bigInteger, BigInteger bigInteger2) {
        ECDomainParameters parameters = this.key.getParameters();
        BigInteger n = parameters.getN();
        BigInteger calculateE = calculateE(n, bArr);
        if (bigInteger.compareTo(ONE) < 0 || bigInteger.compareTo(n) >= 0 || bigInteger2.compareTo(ONE) < 0 || bigInteger2.compareTo(n) >= 0) {
            return false;
        }
        bigInteger2 = bigInteger2.modInverse(n);
        ECPoint sumOfTwoMultiplies = ECAlgorithms.sumOfTwoMultiplies(parameters.getG(), calculateE.multiply(bigInteger2).mod(n), ((ECPublicKeyParameters) this.key).getQ(), bigInteger.multiply(bigInteger2).mod(n));
        if (sumOfTwoMultiplies.isInfinity()) {
            return false;
        }
        ECCurve curve = sumOfTwoMultiplies.getCurve();
        if (curve != null) {
            BigInteger cofactor = curve.getCofactor();
            if (cofactor != null && cofactor.compareTo(EIGHT) <= 0) {
                ECFieldElement denominator = getDenominator(curve.getCoordinateSystem(), sumOfTwoMultiplies);
                if (!(denominator == null || denominator.isZero())) {
                    ECFieldElement xCoord = sumOfTwoMultiplies.getXCoord();
                    while (curve.isValidFieldElement(bigInteger)) {
                        if (curve.fromBigInteger(bigInteger).multiply(denominator).equals(xCoord)) {
                            return true;
                        }
                        bigInteger = bigInteger.add(n);
                    }
                    return false;
                }
            }
        }
        return sumOfTwoMultiplies.normalize().getAffineXCoord().toBigInteger().mod(n).equals(bigInteger);
    }
}
