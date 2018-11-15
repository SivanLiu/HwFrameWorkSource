package org.bouncycastle.util.test;

import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.test.FixedSecureRandom.Data;
import org.bouncycastle.util.test.FixedSecureRandom.Source;

public class TestRandomData extends FixedSecureRandom {
    public TestRandomData(String str) {
        super(new Source[]{new Data(Hex.decode(str))});
    }

    public TestRandomData(byte[] bArr) {
        super(new Source[]{new Data(bArr)});
    }
}
