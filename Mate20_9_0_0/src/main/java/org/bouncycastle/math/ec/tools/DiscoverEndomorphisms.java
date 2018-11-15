package org.bouncycastle.math.ec.tools;

import java.io.PrintStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECConstants;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECFieldElement.Fp;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;

public class DiscoverEndomorphisms {
    private static final int radix = 16;

    private static boolean areRelativelyPrime(BigInteger bigInteger, BigInteger bigInteger2) {
        return bigInteger.gcd(bigInteger2).equals(ECConstants.ONE);
    }

    private static BigInteger[] calculateRange(BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3) {
        return order(bigInteger.subtract(bigInteger2).divide(bigInteger3), bigInteger.add(bigInteger2).divide(bigInteger3));
    }

    private static BigInteger[] chooseShortest(BigInteger[] bigIntegerArr, BigInteger[] bigIntegerArr2) {
        return isShorter(bigIntegerArr, bigIntegerArr2) ? bigIntegerArr : bigIntegerArr2;
    }

    private static void discoverEndomorphisms(String str) {
        X9ECParameters byName = ECNamedCurveTable.getByName(str);
        if (byName == null) {
            PrintStream printStream = System.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown curve: ");
            stringBuilder.append(str);
            printStream.println(stringBuilder.toString());
            return;
        }
        ECCurve curve = byName.getCurve();
        if (ECAlgorithms.isFpCurve(curve)) {
            BigInteger characteristic = curve.getField().getCharacteristic();
            if (curve.getA().isZero() && characteristic.mod(ECConstants.THREE).equals(ECConstants.ONE)) {
                PrintStream printStream2 = System.out;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Curve '");
                stringBuilder2.append(str);
                stringBuilder2.append("' has a 'GLV Type B' endomorphism with these parameters:");
                printStream2.println(stringBuilder2.toString());
                printGLVTypeBParameters(byName);
            }
        }
    }

    public static void discoverEndomorphisms(X9ECParameters x9ECParameters) {
        if (x9ECParameters != null) {
            ECCurve curve = x9ECParameters.getCurve();
            if (ECAlgorithms.isFpCurve(curve)) {
                BigInteger characteristic = curve.getField().getCharacteristic();
                if (curve.getA().isZero() && characteristic.mod(ECConstants.THREE).equals(ECConstants.ONE)) {
                    System.out.println("Curve has a 'GLV Type B' endomorphism with these parameters:");
                    printGLVTypeBParameters(x9ECParameters);
                    return;
                }
                return;
            }
            return;
        }
        throw new NullPointerException("x9");
    }

    private static BigInteger[] extEuclidBezout(BigInteger[] bigIntegerArr) {
        int i = bigIntegerArr[0].compareTo(bigIntegerArr[1]) < 0 ? 1 : 0;
        if (i != 0) {
            swap(bigIntegerArr);
        }
        BigInteger bigInteger = bigIntegerArr[0];
        BigInteger bigInteger2 = bigIntegerArr[1];
        BigInteger bigInteger3 = ECConstants.ONE;
        BigInteger bigInteger4 = ECConstants.ZERO;
        BigInteger bigInteger5 = ECConstants.ZERO;
        BigInteger bigInteger6 = ECConstants.ONE;
        BigInteger bigInteger7 = bigInteger;
        bigInteger = bigInteger2;
        bigInteger2 = bigInteger7;
        while (bigInteger.compareTo(ECConstants.ONE) > 0) {
            bigIntegerArr = bigInteger2.divideAndRemainder(bigInteger);
            BigInteger bigInteger8 = bigIntegerArr[0];
            bigInteger7 = bigInteger;
            bigInteger = bigIntegerArr[1];
            bigInteger2 = bigInteger7;
            BigInteger bigInteger9 = bigInteger4;
            bigInteger4 = bigInteger3.subtract(bigInteger8.multiply(bigInteger4));
            bigInteger3 = bigInteger9;
            BigInteger bigInteger10 = bigInteger6;
            bigInteger6 = bigInteger5.subtract(bigInteger8.multiply(bigInteger6));
            bigInteger5 = bigInteger10;
        }
        if (bigInteger.signum() <= 0) {
            return null;
        }
        bigIntegerArr = new BigInteger[]{bigInteger4, bigInteger6};
        if (i != 0) {
            swap(bigIntegerArr);
        }
        return bigIntegerArr;
    }

    private static BigInteger[] extEuclidGLV(BigInteger bigInteger, BigInteger bigInteger2) {
        BigInteger bigInteger3 = ECConstants.ZERO;
        BigInteger bigInteger4 = ECConstants.ONE;
        BigInteger bigInteger5 = bigInteger3;
        bigInteger3 = bigInteger2;
        bigInteger2 = bigInteger;
        while (true) {
            BigInteger[] divideAndRemainder = bigInteger2.divideAndRemainder(bigInteger3);
            BigInteger bigInteger6 = divideAndRemainder[0];
            BigInteger bigInteger7 = divideAndRemainder[1];
            bigInteger6 = bigInteger5.subtract(bigInteger6.multiply(bigInteger4));
            if (isLessThanSqrt(bigInteger3, bigInteger)) {
                return new BigInteger[]{bigInteger2, bigInteger5, bigInteger3, bigInteger4, bigInteger7, bigInteger6};
            }
            bigInteger2 = bigInteger3;
            bigInteger5 = bigInteger4;
            bigInteger3 = bigInteger7;
            bigInteger4 = bigInteger6;
        }
    }

    private static ECFieldElement[] findBetaValues(ECCurve eCCurve) {
        BigInteger modPow;
        BigInteger characteristic = eCCurve.getField().getCharacteristic();
        BigInteger divide = characteristic.divide(ECConstants.THREE);
        SecureRandom secureRandom = new SecureRandom();
        do {
            modPow = BigIntegers.createRandomInRange(ECConstants.TWO, characteristic.subtract(ECConstants.TWO), secureRandom).modPow(divide, characteristic);
        } while (modPow.equals(ECConstants.ONE));
        ECFieldElement fromBigInteger = eCCurve.fromBigInteger(modPow);
        return new ECFieldElement[]{fromBigInteger, fromBigInteger.square()};
    }

    private static BigInteger[] intersect(BigInteger[] bigIntegerArr, BigInteger[] bigIntegerArr2) {
        if (bigIntegerArr[0].max(bigIntegerArr2[0]).compareTo(bigIntegerArr[1].min(bigIntegerArr2[1])) > 0) {
            return null;
        }
        return new BigInteger[]{bigIntegerArr[0].max(bigIntegerArr2[0]), bigIntegerArr[1].min(bigIntegerArr2[1])};
    }

    private static boolean isLessThanSqrt(BigInteger bigInteger, BigInteger bigInteger2) {
        bigInteger = bigInteger.abs();
        bigInteger2 = bigInteger2.abs();
        int bitLength = bigInteger2.bitLength();
        int bitLength2 = bigInteger.bitLength() * 2;
        return bitLength2 + -1 <= bitLength && (bitLength2 < bitLength || bigInteger.multiply(bigInteger).compareTo(bigInteger2) < 0);
    }

    private static boolean isShorter(BigInteger[] bigIntegerArr, BigInteger[] bigIntegerArr2) {
        boolean z = false;
        BigInteger abs = bigIntegerArr[0].abs();
        BigInteger abs2 = bigIntegerArr[1].abs();
        BigInteger abs3 = bigIntegerArr2[0].abs();
        BigInteger abs4 = bigIntegerArr2[1].abs();
        boolean z2 = abs.compareTo(abs3) < 0;
        if (z2 == (abs2.compareTo(abs4) < 0)) {
            return z2;
        }
        if (abs.multiply(abs).add(abs2.multiply(abs2)).compareTo(abs3.multiply(abs3).add(abs4.multiply(abs4))) < 0) {
            z = true;
        }
        return z;
    }

    private static boolean isVectorBoundedBySqrt(BigInteger[] bigIntegerArr, BigInteger bigInteger) {
        return isLessThanSqrt(bigIntegerArr[0].abs().max(bigIntegerArr[1].abs()), bigInteger);
    }

    private static BigInteger isqrt(BigInteger bigInteger) {
        BigInteger shiftRight = bigInteger.shiftRight(bigInteger.bitLength() / 2);
        while (true) {
            BigInteger shiftRight2 = shiftRight.add(bigInteger.divide(shiftRight)).shiftRight(1);
            if (shiftRight2.equals(shiftRight)) {
                return shiftRight2;
            }
            shiftRight = shiftRight2;
        }
    }

    public static void main(String[] strArr) {
        if (strArr.length < 1) {
            System.err.println("Expected a list of curve names as arguments");
            return;
        }
        for (String discoverEndomorphisms : strArr) {
            discoverEndomorphisms(discoverEndomorphisms);
        }
    }

    private static BigInteger[] order(BigInteger bigInteger, BigInteger bigInteger2) {
        if (bigInteger.compareTo(bigInteger2) <= 0) {
            return new BigInteger[]{bigInteger, bigInteger2};
        }
        return new BigInteger[]{bigInteger2, bigInteger};
    }

    private static void printGLVTypeBParameters(X9ECParameters x9ECParameters) {
        BigInteger[] solveQuadraticEquation = solveQuadraticEquation(x9ECParameters.getN(), ECConstants.ONE, ECConstants.ONE);
        ECFieldElement[] findBetaValues = findBetaValues(x9ECParameters.getCurve());
        printGLVTypeBParameters(x9ECParameters, solveQuadraticEquation[0], findBetaValues);
        System.out.println("OR");
        printGLVTypeBParameters(x9ECParameters, solveQuadraticEquation[1], findBetaValues);
    }

    private static void printGLVTypeBParameters(X9ECParameters x9ECParameters, BigInteger bigInteger, ECFieldElement[] eCFieldElementArr) {
        ECPoint normalize = x9ECParameters.getG().normalize();
        ECPoint normalize2 = normalize.multiply(bigInteger).normalize();
        if (normalize.getYCoord().equals(normalize2.getYCoord())) {
            BigInteger divide;
            ECFieldElement eCFieldElement = eCFieldElementArr[0];
            if (!normalize.getXCoord().multiply(eCFieldElement).equals(normalize2.getXCoord())) {
                eCFieldElement = eCFieldElementArr[1];
                if (!normalize.getXCoord().multiply(eCFieldElement).equals(normalize2.getXCoord())) {
                    throw new IllegalStateException("Derivation of GLV Type B parameters failed unexpectedly");
                }
            }
            BigInteger n = x9ECParameters.getN();
            BigInteger[] extEuclidGLV = extEuclidGLV(n, bigInteger);
            BigInteger[] bigIntegerArr = new BigInteger[]{extEuclidGLV[2], extEuclidGLV[3].negate()};
            extEuclidGLV = chooseShortest(new BigInteger[]{extEuclidGLV[0], extEuclidGLV[1].negate()}, new BigInteger[]{extEuclidGLV[4], extEuclidGLV[5].negate()});
            if (!isVectorBoundedBySqrt(extEuclidGLV, n) && areRelativelyPrime(bigIntegerArr[0], bigIntegerArr[1])) {
                BigInteger bigInteger2 = bigIntegerArr[0];
                BigInteger bigInteger3 = bigIntegerArr[1];
                divide = bigInteger2.add(bigInteger3.multiply(bigInteger)).divide(n);
                BigInteger[] extEuclidBezout = extEuclidBezout(new BigInteger[]{divide.abs(), bigInteger3.abs()});
                if (extEuclidBezout != null) {
                    BigInteger bigInteger4 = extEuclidBezout[0];
                    BigInteger bigInteger5 = extEuclidBezout[1];
                    if (divide.signum() < 0) {
                        bigInteger4 = bigInteger4.negate();
                    }
                    if (bigInteger3.signum() > 0) {
                        bigInteger5 = bigInteger5.negate();
                    }
                    if (divide.multiply(bigInteger4).subtract(bigInteger3.multiply(bigInteger5)).equals(ECConstants.ONE)) {
                        divide = bigInteger5.multiply(n).subtract(bigInteger4.multiply(bigInteger));
                        bigInteger5 = bigInteger4.negate();
                        BigInteger negate = divide.negate();
                        BigInteger add = isqrt(n.subtract(ECConstants.ONE)).add(ECConstants.ONE);
                        extEuclidBezout = intersect(calculateRange(bigInteger5, add, bigInteger3), calculateRange(negate, add, bigInteger2));
                        if (extEuclidBezout != null) {
                            for (negate = extEuclidBezout[0]; negate.compareTo(extEuclidBezout[1]) <= 0; negate = negate.add(ECConstants.ONE)) {
                                BigInteger[] bigIntegerArr2 = new BigInteger[]{divide.add(negate.multiply(bigInteger2)), bigInteger4.add(negate.multiply(bigInteger3))};
                                if (isShorter(bigIntegerArr2, extEuclidGLV)) {
                                    extEuclidGLV = bigIntegerArr2;
                                }
                            }
                        }
                    } else {
                        throw new IllegalStateException();
                    }
                }
            }
            BigInteger subtract = bigIntegerArr[0].multiply(extEuclidGLV[1]).subtract(bigIntegerArr[1].multiply(extEuclidGLV[0]));
            int bitLength = (n.bitLength() + 16) - (n.bitLength() & 7);
            n = roundQuotient(extEuclidGLV[1].shiftLeft(bitLength), subtract);
            divide = roundQuotient(bigIntegerArr[1].shiftLeft(bitLength), subtract).negate();
            printProperty("Beta", eCFieldElement.toBigInteger().toString(16));
            printProperty("Lambda", bigInteger.toString(16));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{ ");
            stringBuilder.append(bigIntegerArr[0].toString(16));
            stringBuilder.append(", ");
            stringBuilder.append(bigIntegerArr[1].toString(16));
            stringBuilder.append(" }");
            printProperty("v1", stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("{ ");
            stringBuilder2.append(extEuclidGLV[0].toString(16));
            stringBuilder2.append(", ");
            stringBuilder2.append(extEuclidGLV[1].toString(16));
            stringBuilder2.append(" }");
            printProperty("v2", stringBuilder2.toString());
            printProperty("d", subtract.toString(16));
            printProperty("(OPT) g1", n.toString(16));
            printProperty("(OPT) g2", divide.toString(16));
            printProperty("(OPT) bits", Integer.toString(bitLength));
            return;
        }
        throw new IllegalStateException("Derivation of GLV Type B parameters failed unexpectedly");
    }

    private static void printProperty(String str, Object obj) {
        StringBuffer stringBuffer = new StringBuffer("  ");
        stringBuffer.append(str);
        while (stringBuffer.length() < 20) {
            stringBuffer.append(' ');
        }
        stringBuffer.append("= ");
        stringBuffer.append(obj.toString());
        System.out.println(stringBuffer.toString());
    }

    private static BigInteger roundQuotient(BigInteger bigInteger, BigInteger bigInteger2) {
        int i = bigInteger.signum() != bigInteger2.signum() ? 1 : 0;
        bigInteger = bigInteger.abs();
        bigInteger2 = bigInteger2.abs();
        bigInteger = bigInteger.add(bigInteger2.shiftRight(1)).divide(bigInteger2);
        return i != 0 ? bigInteger.negate() : bigInteger;
    }

    private static BigInteger[] solveQuadraticEquation(BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3) {
        bigInteger2 = new Fp(bigInteger, bigInteger2.multiply(bigInteger2).subtract(bigInteger3.shiftLeft(2)).mod(bigInteger)).sqrt().toBigInteger();
        bigInteger3 = bigInteger.subtract(bigInteger2);
        if (bigInteger2.testBit(0)) {
            bigInteger3 = bigInteger3.add(bigInteger);
        } else {
            bigInteger2 = bigInteger2.add(bigInteger);
        }
        return new BigInteger[]{bigInteger2.shiftRight(1), bigInteger3.shiftRight(1)};
    }

    private static void swap(BigInteger[] bigIntegerArr) {
        BigInteger bigInteger = bigIntegerArr[0];
        bigIntegerArr[0] = bigIntegerArr[1];
        bigIntegerArr[1] = bigInteger;
    }
}
