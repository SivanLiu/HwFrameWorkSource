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
        byte[] toByteArray = bigInteger.toByteArray();
        byte[] bArr;
        if (i < toByteArray.length) {
            bArr = new byte[i];
            System.arraycopy(toByteArray, toByteArray.length - bArr.length, bArr, 0, bArr.length);
            return bArr;
        } else if (i <= toByteArray.length) {
            return toByteArray;
        } else {
            bArr = new byte[i];
            System.arraycopy(toByteArray, 0, bArr, bArr.length - toByteArray.length, toByteArray.length);
            return bArr;
        }
    }
}
