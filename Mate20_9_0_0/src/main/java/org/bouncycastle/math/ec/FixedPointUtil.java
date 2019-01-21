package org.bouncycastle.math.ec;

import java.math.BigInteger;

public class FixedPointUtil {
    public static final String PRECOMP_NAME = "bc_fixed_point";

    public static int getCombSize(ECCurve eCCurve) {
        BigInteger order = eCCurve.getOrder();
        return order == null ? eCCurve.getFieldSize() + 1 : order.bitLength();
    }

    public static FixedPointPreCompInfo getFixedPointPreCompInfo(PreCompInfo preCompInfo) {
        return (preCompInfo == null || !(preCompInfo instanceof FixedPointPreCompInfo)) ? new FixedPointPreCompInfo() : (FixedPointPreCompInfo) preCompInfo;
    }

    public static FixedPointPreCompInfo precompute(ECPoint eCPoint) {
        ECCurve curve = eCPoint.getCurve();
        int i = getCombSize(curve) > 257 ? 6 : 5;
        int i2 = 1 << i;
        FixedPointPreCompInfo fixedPointPreCompInfo = getFixedPointPreCompInfo(curve.getPreCompInfo(eCPoint, PRECOMP_NAME));
        ECPoint[] preComp = fixedPointPreCompInfo.getPreComp();
        if (preComp == null || preComp.length < i2) {
            int i3;
            int combSize = ((getCombSize(curve) + i) - 1) / i;
            ECPoint[] eCPointArr = new ECPoint[(i + 1)];
            eCPointArr[0] = eCPoint;
            for (i3 = 1; i3 < i; i3++) {
                eCPointArr[i3] = eCPointArr[i3 - 1].timesPow2(combSize);
            }
            eCPointArr[i] = eCPointArr[0].subtract(eCPointArr[1]);
            curve.normalizeAll(eCPointArr);
            preComp = new ECPoint[i2];
            preComp[0] = eCPointArr[0];
            for (i3 = i - 1; i3 >= 0; i3--) {
                ECPoint eCPoint2 = eCPointArr[i3];
                int i4 = 1 << i3;
                for (int i5 = i4; i5 < i2; i5 += i4 << 1) {
                    preComp[i5] = preComp[i5 - i4].add(eCPoint2);
                }
            }
            curve.normalizeAll(preComp);
            fixedPointPreCompInfo.setLookupTable(curve.createCacheSafeLookupTable(preComp, 0, preComp.length));
            fixedPointPreCompInfo.setOffset(eCPointArr[i]);
            fixedPointPreCompInfo.setPreComp(preComp);
            fixedPointPreCompInfo.setWidth(i);
            curve.setPreCompInfo(eCPoint, PRECOMP_NAME, fixedPointPreCompInfo);
        }
        return fixedPointPreCompInfo;
    }

    public static FixedPointPreCompInfo precompute(ECPoint eCPoint, int i) {
        return precompute(eCPoint);
    }
}
