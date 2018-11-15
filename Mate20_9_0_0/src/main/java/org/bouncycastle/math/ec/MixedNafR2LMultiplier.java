package org.bouncycastle.math.ec;

import java.math.BigInteger;

public class MixedNafR2LMultiplier extends AbstractECMultiplier {
    protected int additionCoord;
    protected int doublingCoord;

    public MixedNafR2LMultiplier() {
        this(2, 4);
    }

    public MixedNafR2LMultiplier(int i, int i2) {
        this.additionCoord = i;
        this.doublingCoord = i2;
    }

    protected ECCurve configureCurve(ECCurve eCCurve, int i) {
        if (eCCurve.getCoordinateSystem() == i) {
            return eCCurve;
        }
        if (eCCurve.supportsCoordinateSystem(i)) {
            return eCCurve.configure().setCoordinateSystem(i).create();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Coordinate system ");
        stringBuilder.append(i);
        stringBuilder.append(" not supported by this curve");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    protected ECPoint multiplyPositive(ECPoint eCPoint, BigInteger bigInteger) {
        ECCurve curve = eCPoint.getCurve();
        ECCurve configureCurve = configureCurve(curve, this.additionCoord);
        ECCurve configureCurve2 = configureCurve(curve, this.doublingCoord);
        int[] generateCompactNaf = WNafUtil.generateCompactNaf(bigInteger);
        ECPoint infinity = configureCurve.getInfinity();
        eCPoint = configureCurve2.importPoint(eCPoint);
        int i = 0;
        ECPoint eCPoint2 = infinity;
        infinity = eCPoint;
        int i2 = 0;
        while (i < generateCompactNaf.length) {
            int i3 = generateCompactNaf[i];
            int i4 = i3 >> 16;
            infinity = infinity.timesPow2(i2 + (i3 & 65535));
            eCPoint = configureCurve.importPoint(infinity);
            if (i4 < 0) {
                eCPoint = eCPoint.negate();
            }
            eCPoint2 = eCPoint2.add(eCPoint);
            i++;
            i2 = 1;
        }
        return curve.importPoint(eCPoint2);
    }
}
