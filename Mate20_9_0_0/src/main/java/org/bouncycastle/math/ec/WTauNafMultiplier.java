package org.bouncycastle.math.ec;

import java.math.BigInteger;
import org.bouncycastle.math.ec.ECPoint.AbstractF2m;

public class WTauNafMultiplier extends AbstractECMultiplier {
    static final String PRECOMP_NAME = "bc_wtnaf";

    private static AbstractF2m multiplyFromWTnaf(AbstractF2m abstractF2m, byte[] bArr, PreCompInfo preCompInfo) {
        AbstractF2m[] preComp;
        int i;
        ECCurve.AbstractF2m abstractF2m2 = (ECCurve.AbstractF2m) abstractF2m.getCurve();
        byte byteValue = abstractF2m2.getA().toBigInteger().byteValue();
        if (preCompInfo == null || !(preCompInfo instanceof WTauNafPreCompInfo)) {
            preComp = Tnaf.getPreComp(abstractF2m, byteValue);
            PreCompInfo wTauNafPreCompInfo = new WTauNafPreCompInfo();
            wTauNafPreCompInfo.setPreComp(preComp);
            abstractF2m2.setPreCompInfo(abstractF2m, PRECOMP_NAME, wTauNafPreCompInfo);
        } else {
            preComp = ((WTauNafPreCompInfo) preCompInfo).getPreComp();
        }
        AbstractF2m[] abstractF2mArr = new AbstractF2m[preComp.length];
        for (i = 0; i < preComp.length; i++) {
            abstractF2mArr[i] = (AbstractF2m) preComp[i].negate();
        }
        AbstractF2m abstractF2m3 = (AbstractF2m) abstractF2m.getCurve().getInfinity();
        int i2 = 0;
        for (i = bArr.length - 1; i >= 0; i--) {
            i2++;
            byte b = bArr[i];
            if (b != (byte) 0) {
                abstractF2m3 = (AbstractF2m) abstractF2m3.tauPow(i2).add(b > (byte) 0 ? preComp[b >>> 1] : abstractF2mArr[(-b) >>> 1]);
                i2 = 0;
            }
        }
        return i2 > 0 ? abstractF2m3.tauPow(i2) : abstractF2m3;
    }

    private AbstractF2m multiplyWTnaf(AbstractF2m abstractF2m, ZTauElement zTauElement, PreCompInfo preCompInfo, byte b, byte b2) {
        return multiplyFromWTnaf(abstractF2m, Tnaf.tauAdicWNaf(b2, zTauElement, (byte) 4, BigInteger.valueOf(16), Tnaf.getTw(b2, 4), b == (byte) 0 ? Tnaf.alpha0 : Tnaf.alpha1), preCompInfo);
    }

    protected ECPoint multiplyPositive(ECPoint eCPoint, BigInteger bigInteger) {
        if (eCPoint instanceof AbstractF2m) {
            AbstractF2m abstractF2m = (AbstractF2m) eCPoint;
            ECCurve.AbstractF2m abstractF2m2 = (ECCurve.AbstractF2m) abstractF2m.getCurve();
            int fieldSize = abstractF2m2.getFieldSize();
            byte byteValue = abstractF2m2.getA().toBigInteger().byteValue();
            byte mu = Tnaf.getMu((int) byteValue);
            byte b = byteValue;
            return multiplyWTnaf(abstractF2m, Tnaf.partModReduction(bigInteger, fieldSize, b, abstractF2m2.getSi(), mu, (byte) 10), abstractF2m2.getPreCompInfo(abstractF2m, PRECOMP_NAME), b, mu);
        }
        throw new IllegalArgumentException("Only ECPoint.AbstractF2m can be used in WTauNafMultiplier");
    }
}
