package org.bouncycastle.pqc.crypto.newhope;

import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.KeyGenerationParameters;

public class NHKeyPairGenerator implements AsymmetricCipherKeyPairGenerator {
    private SecureRandom random;

    public AsymmetricCipherKeyPair generateKeyPair() {
        byte[] bArr = new byte[NewHope.SENDA_BYTES];
        short[] sArr = new short[1024];
        NewHope.keygen(this.random, bArr, sArr);
        return new AsymmetricCipherKeyPair(new NHPublicKeyParameters(bArr), new NHPrivateKeyParameters(sArr));
    }

    public void init(KeyGenerationParameters keyGenerationParameters) {
        this.random = keyGenerationParameters.getRandom();
    }
}
