package org.bouncycastle.math.ec;

import java.math.BigInteger;

public class WNafL2RMultiplier extends AbstractECMultiplier {
    protected int getWindowSize(int i) {
        return WNafUtil.getWindowSize(i);
    }

    protected ECPoint multiplyPositive(ECPoint eCPoint, BigInteger bigInteger) {
        int i;
        int max = Math.max(2, Math.min(16, getWindowSize(bigInteger.bitLength())));
        WNafPreCompInfo precompute = WNafUtil.precompute(eCPoint, max, true);
        ECPoint[] preComp = precompute.getPreComp();
        ECPoint[] preCompNeg = precompute.getPreCompNeg();
        int[] generateCompactWindowNaf = WNafUtil.generateCompactWindowNaf(max, bigInteger);
        eCPoint = eCPoint.getCurve().getInfinity();
        int length = generateCompactWindowNaf.length;
        if (length > 1) {
            ECPoint add;
            length--;
            int i2 = generateCompactWindowNaf[length];
            i = i2 >> 16;
            i2 &= 65535;
            int abs = Math.abs(i);
            ECPoint[] eCPointArr = i < 0 ? preCompNeg : preComp;
            if ((abs << 2) < (1 << max)) {
                byte b = LongArray.bitLengths[abs];
                int i3 = max - b;
                add = eCPointArr[((1 << (max - 1)) - 1) >>> 1].add(eCPointArr[(((abs ^ (1 << (b - 1))) << i3) + 1) >>> 1]);
                i2 -= i3;
            } else {
                add = eCPointArr[abs >>> 1];
            }
            eCPoint = add.timesPow2(i2);
        }
        while (length > 0) {
            length--;
            max = generateCompactWindowNaf[length];
            i = max >> 16;
            eCPoint = eCPoint.twicePlus((i < 0 ? preCompNeg : preComp)[Math.abs(i) >>> 1]).timesPow2(max & 65535);
        }
        return eCPoint;
    }
}
