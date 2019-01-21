package com.android.org.bouncycastle.math.ec;

import java.math.BigInteger;

public class WNafL2RMultiplier extends AbstractECMultiplier {
    protected ECPoint multiplyPositive(ECPoint p, BigInteger k) {
        int highest;
        int width = Math.max(2, Math.min(16, getWindowSize(k.bitLength())));
        WNafPreCompInfo wnafPreCompInfo = WNafUtil.precompute(p, width, true);
        ECPoint[] preComp = wnafPreCompInfo.getPreComp();
        ECPoint[] preCompNeg = wnafPreCompInfo.getPreCompNeg();
        int[] wnaf = WNafUtil.generateCompactWindowNaf(width, k);
        ECPoint R = p.getCurve().getInfinity();
        int i = wnaf.length;
        if (i > 1) {
            i--;
            int wi = wnaf[i];
            int digit = wi >> 16;
            int zeroes = wi & 65535;
            int n = Math.abs(digit);
            ECPoint[] table = digit < 0 ? preCompNeg : preComp;
            if ((n << 2) < (1 << width)) {
                highest = LongArray.bitLengths[n];
                int scale = width - highest;
                width = table[((1 << (width - 1)) - 1) >>> 1].add(table[(((n ^ (1 << (highest - 1))) << scale) + 1) >>> 1]);
                zeroes -= scale;
            } else {
                width = table[n >>> 1];
            }
            R = width.timesPow2(zeroes);
        }
        while (i > 0) {
            i--;
            width = wnaf[i];
            highest = width >> 16;
            R = R.twicePlus((highest < 0 ? preCompNeg : preComp)[Math.abs(highest) >>> 1]).timesPow2(width & 65535);
        }
        return R;
    }

    protected int getWindowSize(int bits) {
        return WNafUtil.getWindowSize(bits);
    }
}
