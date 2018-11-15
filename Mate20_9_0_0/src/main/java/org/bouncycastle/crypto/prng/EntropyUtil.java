package org.bouncycastle.crypto.prng;

public class EntropyUtil {
    public static byte[] generateSeed(EntropySource entropySource, int i) {
        Object obj = new byte[i];
        if (i * 8 <= entropySource.entropySize()) {
            System.arraycopy(entropySource.getEntropy(), 0, obj, 0, obj.length);
            return obj;
        }
        i = entropySource.entropySize() / 8;
        int i2 = 0;
        while (i2 < obj.length) {
            Object entropy = entropySource.getEntropy();
            System.arraycopy(entropy, 0, obj, i2, entropy.length <= obj.length - i2 ? entropy.length : obj.length - i2);
            i2 += i;
        }
        return obj;
    }
}
