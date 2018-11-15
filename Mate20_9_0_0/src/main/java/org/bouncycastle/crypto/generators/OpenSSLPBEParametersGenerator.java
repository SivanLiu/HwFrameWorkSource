package org.bouncycastle.crypto.generators;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.util.DigestFactory;

public class OpenSSLPBEParametersGenerator extends PBEParametersGenerator {
    private Digest digest = DigestFactory.createMD5();

    private byte[] generateDerivedKey(int i) {
        Object obj = new byte[this.digest.getDigestSize()];
        Object obj2 = new byte[i];
        int i2 = 0;
        while (true) {
            this.digest.update(this.password, 0, this.password.length);
            this.digest.update(this.salt, 0, this.salt.length);
            this.digest.doFinal(obj, 0);
            int length = i > obj.length ? obj.length : i;
            System.arraycopy(obj, 0, obj2, i2, length);
            i2 += length;
            i -= length;
            if (i == 0) {
                return obj2;
            }
            this.digest.reset();
            this.digest.update(obj, 0, obj.length);
        }
    }

    public CipherParameters generateDerivedMacParameters(int i) {
        return generateDerivedParameters(i);
    }

    public CipherParameters generateDerivedParameters(int i) {
        i /= 8;
        return new KeyParameter(generateDerivedKey(i), 0, i);
    }

    public CipherParameters generateDerivedParameters(int i, int i2) {
        i /= 8;
        i2 /= 8;
        byte[] generateDerivedKey = generateDerivedKey(i + i2);
        return new ParametersWithIV(new KeyParameter(generateDerivedKey, 0, i), generateDerivedKey, i, i2);
    }

    public void init(byte[] bArr, byte[] bArr2) {
        super.init(bArr, bArr2, 1);
    }
}
