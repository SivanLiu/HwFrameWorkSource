package org.bouncycastle.math.ec;

import java.math.BigInteger;
import org.bouncycastle.math.ec.ECCurve.AbstractF2m;
import org.bouncycastle.math.ec.endo.ECEndomorphism;
import org.bouncycastle.math.ec.endo.GLVEndomorphism;
import org.bouncycastle.math.field.FiniteField;
import org.bouncycastle.math.field.PolynomialExtensionField;

public class ECAlgorithms {
    static ECPoint implShamirsTrickJsf(ECPoint eCPoint, BigInteger bigInteger, ECPoint eCPoint2, BigInteger bigInteger2) {
        ECCurve curve = eCPoint.getCurve();
        ECPoint infinity = curve.getInfinity();
        ECPoint add = eCPoint.add(eCPoint2);
        ECPoint subtract = eCPoint.subtract(eCPoint2);
        curve.normalizeAll(new ECPoint[]{eCPoint2, subtract, eCPoint, add});
        ECPoint[] eCPointArr = new ECPoint[]{r5[3].negate(), r5[2].negate(), r5[1].negate(), r5[0].negate(), infinity, r5[0], r5[1], r5[2], r5[3]};
        byte[] generateJSF = WNafUtil.generateJSF(bigInteger, bigInteger2);
        int length = generateJSF.length;
        while (true) {
            length--;
            if (length < 0) {
                return infinity;
            }
            byte b = generateJSF[length];
            infinity = infinity.twicePlus(eCPointArr[((((b << 24) >> 28) * 3) + 4) + ((b << 28) >> 28)]);
        }
    }

    static ECPoint implShamirsTrickWNaf(ECPoint eCPoint, BigInteger bigInteger, ECPoint eCPoint2, BigInteger bigInteger2) {
        boolean z = false;
        boolean z2 = bigInteger.signum() < 0;
        if (bigInteger2.signum() < 0) {
            z = true;
        }
        bigInteger = bigInteger.abs();
        bigInteger2 = bigInteger2.abs();
        int max = Math.max(2, Math.min(16, WNafUtil.getWindowSize(bigInteger.bitLength())));
        int max2 = Math.max(2, Math.min(16, WNafUtil.getWindowSize(bigInteger2.bitLength())));
        WNafPreCompInfo precompute = WNafUtil.precompute(eCPoint, max, true);
        WNafPreCompInfo precompute2 = WNafUtil.precompute(eCPoint2, max2, true);
        return implShamirsTrickWNaf(z2 ? precompute.getPreCompNeg() : precompute.getPreComp(), z2 ? precompute.getPreComp() : precompute.getPreCompNeg(), WNafUtil.generateWindowNaf(max, bigInteger), z ? precompute2.getPreCompNeg() : precompute2.getPreComp(), z ? precompute2.getPreComp() : precompute2.getPreCompNeg(), WNafUtil.generateWindowNaf(max2, bigInteger2));
    }

    static ECPoint implShamirsTrickWNaf(ECPoint eCPoint, BigInteger bigInteger, ECPointMap eCPointMap, BigInteger bigInteger2) {
        boolean z = false;
        boolean z2 = bigInteger.signum() < 0;
        if (bigInteger2.signum() < 0) {
            z = true;
        }
        bigInteger = bigInteger.abs();
        bigInteger2 = bigInteger2.abs();
        int max = Math.max(2, Math.min(16, WNafUtil.getWindowSize(Math.max(bigInteger.bitLength(), bigInteger2.bitLength()))));
        ECPoint mapPointWithPrecomp = WNafUtil.mapPointWithPrecomp(eCPoint, max, true, eCPointMap);
        WNafPreCompInfo wNafPreCompInfo = WNafUtil.getWNafPreCompInfo(eCPoint);
        WNafPreCompInfo wNafPreCompInfo2 = WNafUtil.getWNafPreCompInfo(mapPointWithPrecomp);
        return implShamirsTrickWNaf(z2 ? wNafPreCompInfo.getPreCompNeg() : wNafPreCompInfo.getPreComp(), z2 ? wNafPreCompInfo.getPreComp() : wNafPreCompInfo.getPreCompNeg(), WNafUtil.generateWindowNaf(max, bigInteger), z ? wNafPreCompInfo2.getPreCompNeg() : wNafPreCompInfo2.getPreComp(), z ? wNafPreCompInfo2.getPreComp() : wNafPreCompInfo2.getPreCompNeg(), WNafUtil.generateWindowNaf(max, bigInteger2));
    }

    private static ECPoint implShamirsTrickWNaf(ECPoint[] eCPointArr, ECPoint[] eCPointArr2, byte[] bArr, ECPoint[] eCPointArr3, ECPoint[] eCPointArr4, byte[] bArr2) {
        int max = Math.max(bArr.length, bArr2.length);
        ECPoint infinity = eCPointArr[0].getCurve().getInfinity();
        max--;
        int i = 0;
        ECPoint eCPoint = infinity;
        while (max >= 0) {
            int i2 = max < bArr.length ? bArr[max] : 0;
            int i3 = max < bArr2.length ? bArr2[max] : 0;
            if ((i2 | i3) == 0) {
                i++;
            } else {
                ECPoint add;
                if (i2 != 0) {
                    add = infinity.add((i2 < 0 ? eCPointArr2 : eCPointArr)[Math.abs(i2) >>> 1]);
                } else {
                    add = infinity;
                }
                if (i3 != 0) {
                    add = add.add((i3 < 0 ? eCPointArr4 : eCPointArr3)[Math.abs(i3) >>> 1]);
                }
                if (i > 0) {
                    eCPoint = eCPoint.timesPow2(i);
                    i = 0;
                }
                eCPoint = eCPoint.twicePlus(add);
            }
            max--;
        }
        return i > 0 ? eCPoint.timesPow2(i) : eCPoint;
    }

    static ECPoint implSumOfMultiplies(ECPoint[] eCPointArr, ECPointMap eCPointMap, BigInteger[] bigIntegerArr) {
        ECPoint[] eCPointArr2 = eCPointArr;
        int length = eCPointArr2.length;
        int i = length << 1;
        boolean[] zArr = new boolean[i];
        WNafPreCompInfo[] wNafPreCompInfoArr = new WNafPreCompInfo[i];
        byte[][] bArr = new byte[i][];
        for (int i2 = 0; i2 < length; i2++) {
            int i3 = i2 << 1;
            int i4 = i3 + 1;
            BigInteger bigInteger = bigIntegerArr[i3];
            zArr[i3] = bigInteger.signum() < 0;
            bigInteger = bigInteger.abs();
            BigInteger bigInteger2 = bigIntegerArr[i4];
            zArr[i4] = bigInteger2.signum() < 0;
            bigInteger2 = bigInteger2.abs();
            int max = Math.max(2, Math.min(16, WNafUtil.getWindowSize(Math.max(bigInteger.bitLength(), bigInteger2.bitLength()))));
            ECPoint eCPoint = eCPointArr2[i2];
            ECPoint mapPointWithPrecomp = WNafUtil.mapPointWithPrecomp(eCPoint, max, true, eCPointMap);
            wNafPreCompInfoArr[i3] = WNafUtil.getWNafPreCompInfo(eCPoint);
            wNafPreCompInfoArr[i4] = WNafUtil.getWNafPreCompInfo(mapPointWithPrecomp);
            bArr[i3] = WNafUtil.generateWindowNaf(max, bigInteger);
            bArr[i4] = WNafUtil.generateWindowNaf(max, bigInteger2);
        }
        return implSumOfMultiplies(zArr, wNafPreCompInfoArr, bArr);
    }

    static ECPoint implSumOfMultiplies(ECPoint[] eCPointArr, BigInteger[] bigIntegerArr) {
        int length = eCPointArr.length;
        boolean[] zArr = new boolean[length];
        WNafPreCompInfo[] wNafPreCompInfoArr = new WNafPreCompInfo[length];
        byte[][] bArr = new byte[length][];
        for (int i = 0; i < length; i++) {
            BigInteger bigInteger = bigIntegerArr[i];
            zArr[i] = bigInteger.signum() < 0;
            bigInteger = bigInteger.abs();
            int max = Math.max(2, Math.min(16, WNafUtil.getWindowSize(bigInteger.bitLength())));
            wNafPreCompInfoArr[i] = WNafUtil.precompute(eCPointArr[i], max, true);
            bArr[i] = WNafUtil.generateWindowNaf(max, bigInteger);
        }
        return implSumOfMultiplies(zArr, wNafPreCompInfoArr, bArr);
    }

    private static ECPoint implSumOfMultiplies(boolean[] zArr, WNafPreCompInfo[] wNafPreCompInfoArr, byte[][] bArr) {
        int length = bArr.length;
        int i = 0;
        int i2 = i;
        while (i < length) {
            i2 = Math.max(i2, bArr[i].length);
            i++;
        }
        ECPoint infinity = wNafPreCompInfoArr[0].getPreComp()[0].getCurve().getInfinity();
        i2--;
        int i3 = 0;
        ECPoint eCPoint = infinity;
        while (i2 >= 0) {
            ECPoint eCPoint2 = infinity;
            for (int i4 = 0; i4 < length; i4++) {
                byte[] bArr2 = bArr[i4];
                int i5 = i2 < bArr2.length ? bArr2[i2] : 0;
                if (i5 != 0) {
                    int abs = Math.abs(i5);
                    WNafPreCompInfo wNafPreCompInfo = wNafPreCompInfoArr[i4];
                    eCPoint2 = eCPoint2.add(((i5 < 0) == zArr[i4] ? wNafPreCompInfo.getPreComp() : wNafPreCompInfo.getPreCompNeg())[abs >>> 1]);
                }
            }
            if (eCPoint2 == infinity) {
                i3++;
            } else {
                if (i3 > 0) {
                    eCPoint = eCPoint.timesPow2(i3);
                    i3 = 0;
                }
                eCPoint = eCPoint.twicePlus(eCPoint2);
            }
            i2--;
        }
        return i3 > 0 ? eCPoint.timesPow2(i3) : eCPoint;
    }

    static ECPoint implSumOfMultipliesGLV(ECPoint[] eCPointArr, BigInteger[] bigIntegerArr, GLVEndomorphism gLVEndomorphism) {
        int i = 0;
        BigInteger order = eCPointArr[0].getCurve().getOrder();
        int length = eCPointArr.length;
        int i2 = length << 1;
        BigInteger[] bigIntegerArr2 = new BigInteger[i2];
        int i3 = 0;
        int i4 = i3;
        while (i3 < length) {
            BigInteger[] decomposeScalar = gLVEndomorphism.decomposeScalar(bigIntegerArr[i3].mod(order));
            int i5 = i4 + 1;
            bigIntegerArr2[i4] = decomposeScalar[0];
            i4 = i5 + 1;
            bigIntegerArr2[i5] = decomposeScalar[1];
            i3++;
        }
        ECPointMap pointMap = gLVEndomorphism.getPointMap();
        if (gLVEndomorphism.hasEfficientPointMap()) {
            return implSumOfMultiplies(eCPointArr, pointMap, bigIntegerArr2);
        }
        ECPoint[] eCPointArr2 = new ECPoint[i2];
        int i6 = 0;
        while (i < length) {
            ECPoint eCPoint = eCPointArr[i];
            ECPoint map = pointMap.map(eCPoint);
            i4 = i6 + 1;
            eCPointArr2[i6] = eCPoint;
            i6 = i4 + 1;
            eCPointArr2[i4] = map;
            i++;
        }
        return implSumOfMultiplies(eCPointArr2, bigIntegerArr2);
    }

    public static ECPoint importPoint(ECCurve eCCurve, ECPoint eCPoint) {
        if (eCCurve.equals(eCPoint.getCurve())) {
            return eCCurve.importPoint(eCPoint);
        }
        throw new IllegalArgumentException("Point must be on the same curve");
    }

    public static boolean isF2mCurve(ECCurve eCCurve) {
        return isF2mField(eCCurve.getField());
    }

    public static boolean isF2mField(FiniteField finiteField) {
        return finiteField.getDimension() > 1 && finiteField.getCharacteristic().equals(ECConstants.TWO) && (finiteField instanceof PolynomialExtensionField);
    }

    public static boolean isFpCurve(ECCurve eCCurve) {
        return isFpField(eCCurve.getField());
    }

    public static boolean isFpField(FiniteField finiteField) {
        return finiteField.getDimension() == 1;
    }

    public static void montgomeryTrick(ECFieldElement[] eCFieldElementArr, int i, int i2) {
        montgomeryTrick(eCFieldElementArr, i, i2, null);
    }

    public static void montgomeryTrick(ECFieldElement[] eCFieldElementArr, int i, int i2, ECFieldElement eCFieldElement) {
        ECFieldElement[] eCFieldElementArr2 = new ECFieldElement[i2];
        int i3 = 0;
        eCFieldElementArr2[0] = eCFieldElementArr[i];
        while (true) {
            i3++;
            if (i3 >= i2) {
                break;
            }
            eCFieldElementArr2[i3] = eCFieldElementArr2[i3 - 1].multiply(eCFieldElementArr[i + i3]);
        }
        i3--;
        if (eCFieldElement != null) {
            eCFieldElementArr2[i3] = eCFieldElementArr2[i3].multiply(eCFieldElement);
        }
        ECFieldElement invert = eCFieldElementArr2[i3].invert();
        while (i3 > 0) {
            int i4 = i3 - 1;
            i3 += i;
            ECFieldElement eCFieldElement2 = eCFieldElementArr[i3];
            eCFieldElementArr[i3] = eCFieldElementArr2[i4].multiply(invert);
            invert = invert.multiply(eCFieldElement2);
            i3 = i4;
        }
        eCFieldElementArr[i] = invert;
    }

    public static ECPoint referenceMultiply(ECPoint eCPoint, BigInteger bigInteger) {
        BigInteger abs = bigInteger.abs();
        ECPoint infinity = eCPoint.getCurve().getInfinity();
        int bitLength = abs.bitLength();
        if (bitLength > 0) {
            if (abs.testBit(0)) {
                infinity = eCPoint;
            }
            for (int i = 1; i < bitLength; i++) {
                eCPoint = eCPoint.twice();
                if (abs.testBit(i)) {
                    infinity = infinity.add(eCPoint);
                }
            }
        }
        return bigInteger.signum() < 0 ? infinity.negate() : infinity;
    }

    public static ECPoint shamirsTrick(ECPoint eCPoint, BigInteger bigInteger, ECPoint eCPoint2, BigInteger bigInteger2) {
        return validatePoint(implShamirsTrickJsf(eCPoint, bigInteger, importPoint(eCPoint.getCurve(), eCPoint2), bigInteger2));
    }

    public static ECPoint sumOfMultiplies(ECPoint[] eCPointArr, BigInteger[] bigIntegerArr) {
        if (!(eCPointArr == null || bigIntegerArr == null || eCPointArr.length != bigIntegerArr.length)) {
            int i = 1;
            if (eCPointArr.length >= 1) {
                int length = eCPointArr.length;
                switch (length) {
                    case 1:
                        return eCPointArr[0].multiply(bigIntegerArr[0]);
                    case 2:
                        return sumOfTwoMultiplies(eCPointArr[0], bigIntegerArr[0], eCPointArr[1], bigIntegerArr[1]);
                    default:
                        ECPoint eCPoint = eCPointArr[0];
                        ECCurve curve = eCPoint.getCurve();
                        ECPoint[] eCPointArr2 = new ECPoint[length];
                        eCPointArr2[0] = eCPoint;
                        while (i < length) {
                            eCPointArr2[i] = importPoint(curve, eCPointArr[i]);
                            i++;
                        }
                        ECEndomorphism endomorphism = curve.getEndomorphism();
                        return endomorphism instanceof GLVEndomorphism ? validatePoint(implSumOfMultipliesGLV(eCPointArr2, bigIntegerArr, (GLVEndomorphism) endomorphism)) : validatePoint(implSumOfMultiplies(eCPointArr2, bigIntegerArr));
                }
            }
        }
        throw new IllegalArgumentException("point and scalar arrays should be non-null, and of equal, non-zero, length");
    }

    public static ECPoint sumOfTwoMultiplies(ECPoint eCPoint, BigInteger bigInteger, ECPoint eCPoint2, BigInteger bigInteger2) {
        ECCurve curve = eCPoint.getCurve();
        eCPoint2 = importPoint(curve, eCPoint2);
        if ((curve instanceof AbstractF2m) && ((AbstractF2m) curve).isKoblitz()) {
            eCPoint = eCPoint.multiply(bigInteger).add(eCPoint2.multiply(bigInteger2));
        } else {
            ECEndomorphism endomorphism = curve.getEndomorphism();
            if (endomorphism instanceof GLVEndomorphism) {
                eCPoint = implSumOfMultipliesGLV(new ECPoint[]{eCPoint, eCPoint2}, new BigInteger[]{bigInteger, bigInteger2}, (GLVEndomorphism) endomorphism);
            } else {
                eCPoint = implShamirsTrickWNaf(eCPoint, bigInteger, eCPoint2, bigInteger2);
            }
        }
        return validatePoint(eCPoint);
    }

    public static ECPoint validatePoint(ECPoint eCPoint) {
        if (eCPoint.isValid()) {
            return eCPoint;
        }
        throw new IllegalArgumentException("Invalid point");
    }
}
