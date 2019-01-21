package org.bouncycastle.crypto.prng;

public class EntropyUtil {
    public static byte[] generateSeed(EntropySource entropySource, int i) {
        byte[] bArr = new byte[i];
        if (i * 8 <= entropySource.entropySize()) {
            System.arraycopy(entropySource.getEntropy(), 0, bArr, 0, bArr.length);
            return bArr;
        }
        i = entropySource.entropySize() / 8;
        int i2 = 0;
        while (i2 < bArr.length) {
            byte[] entropy = entropySource.getEntropy();
            System.arraycopy(entropy, 0, bArr, i2, entropy.length <= bArr.length - i2 ? entropy.length : bArr.length - i2);
            i2 += i;
        }
        return bArr;
    }
}
