package org.bouncycastle.asn1.x9;

import java.math.BigInteger;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECFieldElement;

public class X9IntegerConverter {
    public int getByteLength(ECCurve eCCurve) {
        return (eCCurve.getFieldSize() + 7) / 8;
    }

    public int getByteLength(ECFieldElement eCFieldElement) {
        return (eCFieldElement.getFieldSize() + 7) / 8;
    }

    public byte[] integerToBytes(BigInteger bigInteger, int i) {
        Object toByteArray = bigInteger.toByteArray();
        Object obj;
        if (i < toByteArray.length) {
            obj = new byte[i];
            System.arraycopy(toByteArray, toByteArray.length - obj.length, obj, 0, obj.length);
            return obj;
        } else if (i <= toByteArray.length) {
            return toByteArray;
        } else {
            obj = new byte[i];
            System.arraycopy(toByteArray, 0, obj, obj.length - toByteArray.length, toByteArray.length);
            return obj;
        }
    }
}
