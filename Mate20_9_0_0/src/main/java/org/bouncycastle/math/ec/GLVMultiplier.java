package org.bouncycastle.math.ec;

import java.math.BigInteger;
import org.bouncycastle.math.ec.endo.GLVEndomorphism;

public class GLVMultiplier extends AbstractECMultiplier {
    protected final ECCurve curve;
    protected final GLVEndomorphism glvEndomorphism;

    public GLVMultiplier(ECCurve eCCurve, GLVEndomorphism gLVEndomorphism) {
        if (eCCurve == null || eCCurve.getOrder() == null) {
            throw new IllegalArgumentException("Need curve with known group order");
        }
        this.curve = eCCurve;
        this.glvEndomorphism = gLVEndomorphism;
    }

    protected ECPoint multiplyPositive(ECPoint eCPoint, BigInteger bigInteger) {
        if (this.curve.equals(eCPoint.getCurve())) {
            BigInteger[] decomposeScalar = this.glvEndomorphism.decomposeScalar(bigInteger.mod(eCPoint.getCurve().getOrder()));
            BigInteger bigInteger2 = decomposeScalar[0];
            bigInteger = decomposeScalar[1];
            ECPointMap pointMap = this.glvEndomorphism.getPointMap();
            return this.glvEndomorphism.hasEfficientPointMap() ? ECAlgorithms.implShamirsTrickWNaf(eCPoint, bigInteger2, pointMap, bigInteger) : ECAlgorithms.implShamirsTrickWNaf(eCPoint, bigInteger2, pointMap.map(eCPoint), bigInteger);
        } else {
            throw new IllegalStateException();
        }
    }
}
