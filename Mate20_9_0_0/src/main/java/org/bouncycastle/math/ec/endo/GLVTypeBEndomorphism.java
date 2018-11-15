package org.bouncycastle.math.ec.endo;

import java.math.BigInteger;
import org.bouncycastle.math.ec.ECConstants;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPointMap;
import org.bouncycastle.math.ec.ScaleXPointMap;

public class GLVTypeBEndomorphism implements GLVEndomorphism {
    protected final ECCurve curve;
    protected final GLVTypeBParameters parameters;
    protected final ECPointMap pointMap;

    public GLVTypeBEndomorphism(ECCurve eCCurve, GLVTypeBParameters gLVTypeBParameters) {
        this.curve = eCCurve;
        this.parameters = gLVTypeBParameters;
        this.pointMap = new ScaleXPointMap(eCCurve.fromBigInteger(gLVTypeBParameters.getBeta()));
    }

    protected BigInteger calculateB(BigInteger bigInteger, BigInteger bigInteger2, int i) {
        Object obj = bigInteger2.signum() < 0 ? 1 : null;
        bigInteger = bigInteger.multiply(bigInteger2.abs());
        boolean testBit = bigInteger.testBit(i - 1);
        bigInteger = bigInteger.shiftRight(i);
        if (testBit) {
            bigInteger = bigInteger.add(ECConstants.ONE);
        }
        return obj != null ? bigInteger.negate() : bigInteger;
    }

    public BigInteger[] decomposeScalar(BigInteger bigInteger) {
        int bits = this.parameters.getBits();
        BigInteger calculateB = calculateB(bigInteger, this.parameters.getG1(), bits);
        BigInteger calculateB2 = calculateB(bigInteger, this.parameters.getG2(), bits);
        GLVTypeBParameters gLVTypeBParameters = this.parameters;
        bigInteger = bigInteger.subtract(calculateB.multiply(gLVTypeBParameters.getV1A()).add(calculateB2.multiply(gLVTypeBParameters.getV2A())));
        calculateB2 = calculateB.multiply(gLVTypeBParameters.getV1B()).add(calculateB2.multiply(gLVTypeBParameters.getV2B())).negate();
        return new BigInteger[]{bigInteger, calculateB2};
    }

    public ECPointMap getPointMap() {
        return this.pointMap;
    }

    public boolean hasEfficientPointMap() {
        return true;
    }
}
