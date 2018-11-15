package org.bouncycastle.jcajce.spec;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import org.bouncycastle.util.Arrays;

public class DHUParameterSpec implements AlgorithmParameterSpec {
    private final PrivateKey ephemeralPrivateKey;
    private final PublicKey ephemeralPublicKey;
    private final PublicKey otherPartyEphemeralKey;
    private final byte[] userKeyingMaterial;

    public DHUParameterSpec(KeyPair keyPair, PublicKey publicKey) {
        this(keyPair.getPublic(), keyPair.getPrivate(), publicKey, null);
    }

    public DHUParameterSpec(KeyPair keyPair, PublicKey publicKey, byte[] bArr) {
        this(keyPair.getPublic(), keyPair.getPrivate(), publicKey, bArr);
    }

    public DHUParameterSpec(PrivateKey privateKey, PublicKey publicKey) {
        this(null, privateKey, publicKey, null);
    }

    public DHUParameterSpec(PrivateKey privateKey, PublicKey publicKey, byte[] bArr) {
        this(null, privateKey, publicKey, bArr);
    }

    public DHUParameterSpec(PublicKey publicKey, PrivateKey privateKey, PublicKey publicKey2) {
        this(publicKey, privateKey, publicKey2, null);
    }

    public DHUParameterSpec(PublicKey publicKey, PrivateKey privateKey, PublicKey publicKey2, byte[] bArr) {
        this.ephemeralPublicKey = publicKey;
        this.ephemeralPrivateKey = privateKey;
        this.otherPartyEphemeralKey = publicKey2;
        this.userKeyingMaterial = Arrays.clone(bArr);
    }

    public PrivateKey getEphemeralPrivateKey() {
        return this.ephemeralPrivateKey;
    }

    public PublicKey getEphemeralPublicKey() {
        return this.ephemeralPublicKey;
    }

    public PublicKey getOtherPartyEphemeralKey() {
        return this.otherPartyEphemeralKey;
    }

    public byte[] getUserKeyingMaterial() {
        return Arrays.clone(this.userKeyingMaterial);
    }
}
