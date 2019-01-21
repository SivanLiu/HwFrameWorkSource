package org.bouncycastle.math.ec;

import java.math.BigInteger;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;

public abstract class WNafUtil {
    private static final int[] DEFAULT_WINDOW_SIZE_CUTOFFS = new int[]{13, 41, 121, 337, 897, 2305};
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final int[] EMPTY_INTS = new int[0];
    private static final ECPoint[] EMPTY_POINTS = new ECPoint[0];
    public static final String PRECOMP_NAME = "bc_wnaf";

    public static int[] generateCompactNaf(BigInteger bigInteger) {
        if ((bigInteger.bitLength() >>> 16) != 0) {
            throw new IllegalArgumentException("'k' must have bitlength < 2^16");
        } else if (bigInteger.signum() == 0) {
            return EMPTY_INTS;
        } else {
            BigInteger add = bigInteger.shiftLeft(1).add(bigInteger);
            int bitLength = add.bitLength();
            int[] iArr = new int[(bitLength >> 1)];
            add = add.xor(bigInteger);
            bitLength--;
            int i = 0;
            int i2 = i;
            int i3 = 1;
            while (i3 < bitLength) {
                if (add.testBit(i3)) {
                    int i4 = i + 1;
                    iArr[i] = i2 | ((bigInteger.testBit(i3) ? -1 : 1) << 16);
                    i3++;
                    i2 = 1;
                    i = i4;
                } else {
                    i2++;
                }
                i3++;
            }
            int i5 = i + 1;
            iArr[i] = PKIFailureInfo.notAuthorized | i2;
            if (iArr.length > i5) {
                iArr = trim(iArr, i5);
            }
            return iArr;
        }
    }

    public static int[] generateCompactWindowNaf(int i, BigInteger bigInteger) {
        if (i == 2) {
            return generateCompactNaf(bigInteger);
        }
        if (i < 2 || i > 16) {
            throw new IllegalArgumentException("'width' must be in the range [2, 16]");
        } else if ((bigInteger.bitLength() >>> 16) != 0) {
            throw new IllegalArgumentException("'k' must have bitlength < 2^16");
        } else if (bigInteger.signum() == 0) {
            return EMPTY_INTS;
        } else {
            int[] iArr = new int[((bigInteger.bitLength() / i) + 1)];
            int i2 = 1 << i;
            int i3 = i2 - 1;
            int i4 = i2 >>> 1;
            BigInteger bigInteger2 = bigInteger;
            int i5 = 0;
            int i6 = i5;
            int i7 = i6;
            while (i5 <= bigInteger2.bitLength()) {
                if (bigInteger2.testBit(i5) == i6) {
                    i5++;
                } else {
                    bigInteger2 = bigInteger2.shiftRight(i5);
                    int intValue = bigInteger2.intValue() & i3;
                    if (i6 == true) {
                        intValue++;
                    }
                    i6 = (intValue & i4) != 0 ? 1 : 0;
                    if (i6 != 0) {
                        intValue -= i2;
                    }
                    if (i7 > 0) {
                        i5--;
                    }
                    int i8 = i7 + 1;
                    iArr[i7] = i5 | (intValue << 16);
                    i5 = i;
                    i7 = i8;
                }
            }
            if (iArr.length > i7) {
                iArr = trim(iArr, i7);
            }
            return iArr;
        }
    }

    public static byte[] generateJSF(BigInteger bigInteger, BigInteger bigInteger2) {
        byte[] bArr = new byte[(Math.max(bigInteger.bitLength(), bigInteger2.bitLength()) + 1)];
        BigInteger bigInteger3 = bigInteger;
        BigInteger bigInteger4 = bigInteger2;
        int i = 0;
        int i2 = i;
        int i3 = i2;
        int i4 = i3;
        while (true) {
            if ((i | i2) == 0 && bigInteger3.bitLength() <= i3 && bigInteger4.bitLength() <= i3) {
                break;
            }
            int intValue = ((bigInteger3.intValue() >>> i3) + i) & 7;
            int intValue2 = ((bigInteger4.intValue() >>> i3) + i2) & 7;
            int i5 = intValue & 1;
            if (i5 != 0) {
                i5 -= intValue & 2;
                if (intValue + i5 == 4 && (intValue2 & 3) == 2) {
                    i5 = -i5;
                }
            }
            int i6 = intValue2 & 1;
            if (i6 != 0) {
                i6 -= intValue2 & 2;
                if (intValue2 + i6 == 4 && (intValue & 3) == 2) {
                    i6 = -i6;
                }
            }
            if ((i << 1) == 1 + i5) {
                i ^= 1;
            }
            if ((i2 << 1) == 1 + i6) {
                i2 ^= 1;
            }
            i3++;
            if (i3 == 30) {
                bigInteger3 = bigInteger3.shiftRight(30);
                bigInteger4 = bigInteger4.shiftRight(30);
                i3 = 0;
            }
            intValue = i4 + 1;
            bArr[i4] = (byte) ((i5 << 4) | (i6 & 15));
            i4 = intValue;
        }
        return bArr.length > i4 ? trim(bArr, i4) : bArr;
    }

    public static byte[] generateNaf(BigInteger bigInteger) {
        if (bigInteger.signum() == 0) {
            return EMPTY_BYTES;
        }
        BigInteger add = bigInteger.shiftLeft(1).add(bigInteger);
        int bitLength = add.bitLength() - 1;
        byte[] bArr = new byte[bitLength];
        add = add.xor(bigInteger);
        int i = 1;
        while (i < bitLength) {
            if (add.testBit(i)) {
                bArr[i - 1] = (byte) (bigInteger.testBit(i) ? -1 : 1);
                i++;
            }
            i++;
        }
        bArr[bitLength - 1] = (byte) 1;
        return bArr;
    }

    public static byte[] generateWindowNaf(int i, BigInteger bigInteger) {
        if (i == 2) {
            return generateNaf(bigInteger);
        }
        if (i < 2 || i > 8) {
            throw new IllegalArgumentException("'width' must be in the range [2, 8]");
        } else if (bigInteger.signum() == 0) {
            return EMPTY_BYTES;
        } else {
            byte[] bArr = new byte[(bigInteger.bitLength() + 1)];
            int i2 = 1 << i;
            int i3 = i2 - 1;
            int i4 = i2 >>> 1;
            BigInteger bigInteger2 = bigInteger;
            int i5 = 0;
            int i6 = i5;
            int i7 = i6;
            while (i5 <= bigInteger2.bitLength()) {
                if (bigInteger2.testBit(i5) == i6) {
                    i5++;
                } else {
                    bigInteger2 = bigInteger2.shiftRight(i5);
                    int intValue = bigInteger2.intValue() & i3;
                    if (i6 == true) {
                        intValue++;
                    }
                    i6 = (intValue & i4) != 0 ? 1 : 0;
                    if (i6 != 0) {
                        intValue -= i2;
                    }
                    if (i7 > 0) {
                        i5--;
                    }
                    i7 += i5;
                    i5 = i7 + 1;
                    bArr[i7] = (byte) intValue;
                    i7 = i5;
                    i5 = i;
                }
            }
            if (bArr.length > i7) {
                bArr = trim(bArr, i7);
            }
            return bArr;
        }
    }

    public static int getNafWeight(BigInteger bigInteger) {
        return bigInteger.signum() == 0 ? 0 : bigInteger.shiftLeft(1).add(bigInteger).xor(bigInteger).bitCount();
    }

    public static WNafPreCompInfo getWNafPreCompInfo(ECPoint eCPoint) {
        return getWNafPreCompInfo(eCPoint.getCurve().getPreCompInfo(eCPoint, PRECOMP_NAME));
    }

    public static WNafPreCompInfo getWNafPreCompInfo(PreCompInfo preCompInfo) {
        return (preCompInfo == null || !(preCompInfo instanceof WNafPreCompInfo)) ? new WNafPreCompInfo() : (WNafPreCompInfo) preCompInfo;
    }

    public static int getWindowSize(int i) {
        return getWindowSize(i, DEFAULT_WINDOW_SIZE_CUTOFFS);
    }

    public static int getWindowSize(int i, int[] iArr) {
        int i2 = 0;
        while (i2 < iArr.length && i >= iArr[i2]) {
            i2++;
        }
        return i2 + 2;
    }

    public static ECPoint mapPointWithPrecomp(ECPoint eCPoint, int i, boolean z, ECPointMap eCPointMap) {
        ECCurve curve = eCPoint.getCurve();
        WNafPreCompInfo precompute = precompute(eCPoint, i, z);
        eCPoint = eCPointMap.map(eCPoint);
        WNafPreCompInfo wNafPreCompInfo = getWNafPreCompInfo(curve.getPreCompInfo(eCPoint, PRECOMP_NAME));
        ECPoint twice = precompute.getTwice();
        if (twice != null) {
            wNafPreCompInfo.setTwice(eCPointMap.map(twice));
        }
        ECPoint[] preComp = precompute.getPreComp();
        ECPoint[] eCPointArr = new ECPoint[preComp.length];
        int i2 = 0;
        for (int i3 = 0; i3 < preComp.length; i3++) {
            eCPointArr[i3] = eCPointMap.map(preComp[i3]);
        }
        wNafPreCompInfo.setPreComp(eCPointArr);
        if (z) {
            preComp = new ECPoint[eCPointArr.length];
            while (i2 < preComp.length) {
                preComp[i2] = eCPointArr[i2].negate();
                i2++;
            }
            wNafPreCompInfo.setPreCompNeg(preComp);
        }
        curve.setPreCompInfo(eCPoint, PRECOMP_NAME, wNafPreCompInfo);
        return eCPoint;
    }

    public static WNafPreCompInfo precompute(ECPoint eCPoint, int i, boolean z) {
        int i2;
        ECCurve curve = eCPoint.getCurve();
        WNafPreCompInfo wNafPreCompInfo = getWNafPreCompInfo(curve.getPreCompInfo(eCPoint, PRECOMP_NAME));
        int i3 = 0;
        i = 1 << Math.max(0, i - 2);
        ECPoint[] preComp = wNafPreCompInfo.getPreComp();
        if (preComp == null) {
            preComp = EMPTY_POINTS;
            i2 = 0;
        } else {
            i2 = preComp.length;
        }
        if (i2 < i) {
            preComp = resizeTable(preComp, i);
            if (i == 1) {
                preComp[0] = eCPoint.normalize();
            } else {
                int i4;
                if (i2 == 0) {
                    preComp[0] = eCPoint;
                    i4 = 1;
                } else {
                    i4 = i2;
                }
                ECFieldElement eCFieldElement = null;
                if (i == 2) {
                    preComp[1] = eCPoint.threeTimes();
                } else {
                    ECPoint twice = wNafPreCompInfo.getTwice();
                    ECPoint eCPoint2 = preComp[i4 - 1];
                    if (twice == null) {
                        twice = preComp[0].twice();
                        wNafPreCompInfo.setTwice(twice);
                        if (!twice.isInfinity() && ECAlgorithms.isFpCurve(curve) && curve.getFieldSize() >= 64) {
                            switch (curve.getCoordinateSystem()) {
                                case 2:
                                case 3:
                                case 4:
                                    eCFieldElement = twice.getZCoord(0);
                                    twice = curve.createPoint(twice.getXCoord().toBigInteger(), twice.getYCoord().toBigInteger());
                                    ECFieldElement square = eCFieldElement.square();
                                    eCPoint2 = eCPoint2.scaleX(square).scaleY(square.multiply(eCFieldElement));
                                    if (i2 == 0) {
                                        preComp[0] = eCPoint2;
                                        break;
                                    }
                                    break;
                            }
                        }
                    }
                    while (i4 < i) {
                        int i5 = i4 + 1;
                        eCPoint2 = eCPoint2.add(twice);
                        preComp[i4] = eCPoint2;
                        i4 = i5;
                    }
                }
                curve.normalizeAll(preComp, i2, i - i2, eCFieldElement);
            }
        }
        wNafPreCompInfo.setPreComp(preComp);
        if (z) {
            ECPoint[] preCompNeg = wNafPreCompInfo.getPreCompNeg();
            if (preCompNeg == null) {
                preCompNeg = new ECPoint[i];
            } else {
                i3 = preCompNeg.length;
                if (i3 < i) {
                    preCompNeg = resizeTable(preCompNeg, i);
                }
            }
            while (i3 < i) {
                preCompNeg[i3] = preComp[i3].negate();
                i3++;
            }
            wNafPreCompInfo.setPreCompNeg(preCompNeg);
        }
        curve.setPreCompInfo(eCPoint, PRECOMP_NAME, wNafPreCompInfo);
        return wNafPreCompInfo;
    }

    private static ECPoint[] resizeTable(ECPoint[] eCPointArr, int i) {
        ECPoint[] eCPointArr2 = new ECPoint[i];
        System.arraycopy(eCPointArr, 0, eCPointArr2, 0, eCPointArr.length);
        return eCPointArr2;
    }

    private static byte[] trim(byte[] bArr, int i) {
        byte[] bArr2 = new byte[i];
        System.arraycopy(bArr, 0, bArr2, 0, bArr2.length);
        return bArr2;
    }

    private static int[] trim(int[] iArr, int i) {
        int[] iArr2 = new int[i];
        System.arraycopy(iArr, 0, iArr2, 0, iArr2.length);
        return iArr2;
    }
}
