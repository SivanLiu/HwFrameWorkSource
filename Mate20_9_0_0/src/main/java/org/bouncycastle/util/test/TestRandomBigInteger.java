package org.bouncycastle.util.test;

import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.test.FixedSecureRandom.BigInteger;
import org.bouncycastle.util.test.FixedSecureRandom.Source;

public class TestRandomBigInteger extends FixedSecureRandom {
    public TestRandomBigInteger(int i, byte[] bArr) {
        super(new Source[]{new BigInteger(i, bArr)});
    }

    public TestRandomBigInteger(String str) {
        this(str, 10);
    }

    public TestRandomBigInteger(String str, int i) {
        super(new Source[]{new BigInteger(BigIntegers.asUnsignedByteArray(new java.math.BigInteger(str, i)))});
    }

    public TestRandomBigInteger(byte[] bArr) {
        super(new Source[]{new BigInteger(bArr)});
    }
}
