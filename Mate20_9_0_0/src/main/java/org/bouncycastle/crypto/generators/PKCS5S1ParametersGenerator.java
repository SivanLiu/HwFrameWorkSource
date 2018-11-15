package org.bouncycastle.crypto.generators;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class PKCS5S1ParametersGenerator extends PBEParametersGenerator {
    private Digest digest;

    public PKCS5S1ParametersGenerator(Digest digest) {
        this.digest = digest;
    }

    private byte[] generateDerivedKey() {
        byte[] bArr = new byte[this.digest.getDigestSize()];
        this.digest.update(this.password, 0, this.password.length);
        this.digest.update(this.salt, 0, this.salt.length);
        this.digest.doFinal(bArr, 0);
        for (int i = 1; i < this.iterationCount; i++) {
            this.digest.update(bArr, 0, bArr.length);
            this.digest.doFinal(bArr, 0);
        }
        return bArr;
    }

    public CipherParameters generateDerivedMacParameters(int i) {
        return generateDerivedParameters(i);
    }

    public CipherParameters generateDerivedParameters(int i) {
        i /= 8;
        if (i <= this.digest.getDigestSize()) {
            return new KeyParameter(generateDerivedKey(), 0, i);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can't generate a derived key ");
        stringBuilder.append(i);
        stringBuilder.append(" bytes long.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public CipherParameters generateDerivedParameters(int i, int i2) {
        i /= 8;
        i2 /= 8;
        int i3 = i + i2;
        if (i3 <= this.digest.getDigestSize()) {
            byte[] generateDerivedKey = generateDerivedKey();
            return new ParametersWithIV(new KeyParameter(generateDerivedKey, 0, i), generateDerivedKey, i, i2);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can't generate a derived key ");
        stringBuilder.append(i3);
        stringBuilder.append(" bytes long.");
        throw new IllegalArgumentException(stringBuilder.toString());
    }
}
