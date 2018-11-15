package org.bouncycastle.crypto.engines;

import java.math.BigInteger;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

public class CramerShoupCiphertext {
    BigInteger e;
    BigInteger u1;
    BigInteger u2;
    BigInteger v;

    public CramerShoupCiphertext(BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3, BigInteger bigInteger4) {
        this.u1 = bigInteger;
        this.u2 = bigInteger2;
        this.e = bigInteger3;
        this.v = bigInteger4;
    }

    public CramerShoupCiphertext(byte[] bArr) {
        int bigEndianToInt = Pack.bigEndianToInt(bArr, 0) + 4;
        this.u1 = new BigInteger(Arrays.copyOfRange(bArr, 4, bigEndianToInt));
        int bigEndianToInt2 = Pack.bigEndianToInt(bArr, bigEndianToInt);
        bigEndianToInt += 4;
        bigEndianToInt2 += bigEndianToInt;
        this.u2 = new BigInteger(Arrays.copyOfRange(bArr, bigEndianToInt, bigEndianToInt2));
        bigEndianToInt = Pack.bigEndianToInt(bArr, bigEndianToInt2);
        bigEndianToInt2 += 4;
        bigEndianToInt += bigEndianToInt2;
        this.e = new BigInteger(Arrays.copyOfRange(bArr, bigEndianToInt2, bigEndianToInt));
        bigEndianToInt2 = Pack.bigEndianToInt(bArr, bigEndianToInt);
        bigEndianToInt += 4;
        this.v = new BigInteger(Arrays.copyOfRange(bArr, bigEndianToInt, bigEndianToInt2 + bigEndianToInt));
    }

    public BigInteger getE() {
        return this.e;
    }

    public BigInteger getU1() {
        return this.u1;
    }

    public BigInteger getU2() {
        return this.u2;
    }

    public BigInteger getV() {
        return this.v;
    }

    public void setE(BigInteger bigInteger) {
        this.e = bigInteger;
    }

    public void setU1(BigInteger bigInteger) {
        this.u1 = bigInteger;
    }

    public void setU2(BigInteger bigInteger) {
        this.u2 = bigInteger;
    }

    public void setV(BigInteger bigInteger) {
        this.v = bigInteger;
    }

    public byte[] toByteArray() {
        Object toByteArray = this.u1.toByteArray();
        int length = toByteArray.length;
        Object toByteArray2 = this.u2.toByteArray();
        int length2 = toByteArray2.length;
        Object toByteArray3 = this.e.toByteArray();
        int length3 = toByteArray3.length;
        Object toByteArray4 = this.v.toByteArray();
        int length4 = toByteArray4.length;
        byte[] bArr = new byte[((((length + length2) + length3) + length4) + 16)];
        Pack.intToBigEndian(length, bArr, 0);
        System.arraycopy(toByteArray, 0, bArr, 4, length);
        length += 4;
        Pack.intToBigEndian(length2, bArr, length);
        length += 4;
        System.arraycopy(toByteArray2, 0, bArr, length, length2);
        length += length2;
        Pack.intToBigEndian(length3, bArr, length);
        length += 4;
        System.arraycopy(toByteArray3, 0, bArr, length, length3);
        length += length3;
        Pack.intToBigEndian(length4, bArr, length);
        System.arraycopy(toByteArray4, 0, bArr, length + 4, length4);
        return bArr;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("u1: ");
        stringBuilder.append(this.u1.toString());
        stringBuffer.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("\nu2: ");
        stringBuilder.append(this.u2.toString());
        stringBuffer.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("\ne: ");
        stringBuilder.append(this.e.toString());
        stringBuffer.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("\nv: ");
        stringBuilder.append(this.v.toString());
        stringBuffer.append(stringBuilder.toString());
        return stringBuffer.toString();
    }
}
