package org.bouncycastle.asn1.ua;

import java.math.BigInteger;
import java.util.Random;
import org.bouncycastle.math.ec.ECConstants;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;
import org.bouncycastle.math.ec.ECPoint;

public abstract class DSTU4145PointEncoder {
    public static ECPoint decodePoint(ECCurve eCCurve, byte[] bArr) {
        ECFieldElement fromBigInteger = eCCurve.fromBigInteger(BigInteger.valueOf((long) (bArr[bArr.length - 1] & 1)));
        ECFieldElement fromBigInteger2 = eCCurve.fromBigInteger(new BigInteger(1, bArr));
        if (!trace(fromBigInteger2).equals(eCCurve.getA())) {
            fromBigInteger2 = fromBigInteger2.addOne();
        }
        ECFieldElement eCFieldElement = null;
        if (fromBigInteger2.isZero()) {
            eCFieldElement = eCCurve.getB().sqrt();
        } else {
            ECFieldElement solveQuadraticEquation = solveQuadraticEquation(eCCurve, fromBigInteger2.square().invert().multiply(eCCurve.getB()).add(eCCurve.getA()).add(fromBigInteger2));
            if (solveQuadraticEquation != null) {
                if (!trace(solveQuadraticEquation).equals(fromBigInteger)) {
                    solveQuadraticEquation = solveQuadraticEquation.addOne();
                }
                eCFieldElement = fromBigInteger2.multiply(solveQuadraticEquation);
            }
        }
        if (eCFieldElement != null) {
            return eCCurve.validatePoint(fromBigInteger2.toBigInteger(), eCFieldElement.toBigInteger());
        }
        throw new IllegalArgumentException("Invalid point compression");
    }

    public static byte[] encodePoint(ECPoint eCPoint) {
        eCPoint = eCPoint.normalize();
        ECFieldElement affineXCoord = eCPoint.getAffineXCoord();
        byte[] encoded = affineXCoord.getEncoded();
        if (!affineXCoord.isZero()) {
            int length;
            if (trace(eCPoint.getAffineYCoord().divide(affineXCoord)).isOne()) {
                length = encoded.length - 1;
                encoded[length] = (byte) (encoded[length] | 1);
                return encoded;
            }
            length = encoded.length - 1;
            encoded[length] = (byte) (encoded[length] & 254);
        }
        return encoded;
    }

    private static ECFieldElement solveQuadraticEquation(ECCurve eCCurve, ECFieldElement eCFieldElement) {
        if (eCFieldElement.isZero()) {
            return eCFieldElement;
        }
        ECFieldElement eCFieldElement2;
        ECFieldElement fromBigInteger = eCCurve.fromBigInteger(ECConstants.ZERO);
        Random random = new Random();
        int fieldSize = eCFieldElement.getFieldSize();
        do {
            ECFieldElement fromBigInteger2 = eCCurve.fromBigInteger(new BigInteger(fieldSize, random));
            ECFieldElement eCFieldElement3 = eCFieldElement;
            eCFieldElement2 = fromBigInteger;
            for (int i = 1; i <= fieldSize - 1; i++) {
                eCFieldElement3 = eCFieldElement3.square();
                eCFieldElement2 = eCFieldElement2.square().add(eCFieldElement3.multiply(fromBigInteger2));
                eCFieldElement3 = eCFieldElement3.add(eCFieldElement);
            }
            if (!eCFieldElement3.isZero()) {
                return null;
            }
        } while (eCFieldElement2.square().add(eCFieldElement2).isZero());
        return eCFieldElement2;
    }

    private static ECFieldElement trace(ECFieldElement eCFieldElement) {
        ECFieldElement eCFieldElement2 = eCFieldElement;
        for (int i = 1; i < eCFieldElement.getFieldSize(); i++) {
            eCFieldElement2 = eCFieldElement2.square().add(eCFieldElement);
        }
        return eCFieldElement2;
    }
}
