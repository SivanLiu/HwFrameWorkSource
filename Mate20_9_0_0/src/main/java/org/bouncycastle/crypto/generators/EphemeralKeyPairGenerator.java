package org.bouncycastle.crypto.generators;

import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.EphemeralKeyPair;
import org.bouncycastle.crypto.KeyEncoder;

public class EphemeralKeyPairGenerator {
    private AsymmetricCipherKeyPairGenerator gen;
    private KeyEncoder keyEncoder;

    public EphemeralKeyPairGenerator(AsymmetricCipherKeyPairGenerator asymmetricCipherKeyPairGenerator, KeyEncoder keyEncoder) {
        this.gen = asymmetricCipherKeyPairGenerator;
        this.keyEncoder = keyEncoder;
    }

    public EphemeralKeyPair generate() {
        return new EphemeralKeyPair(this.gen.generateKeyPair(), this.keyEncoder);
    }
}
