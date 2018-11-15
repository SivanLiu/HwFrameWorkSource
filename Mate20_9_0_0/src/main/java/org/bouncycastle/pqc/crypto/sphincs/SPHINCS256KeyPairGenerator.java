package org.bouncycastle.pqc.crypto.sphincs;

import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.KeyGenerationParameters;

public class SPHINCS256KeyPairGenerator implements AsymmetricCipherKeyPairGenerator {
    private SecureRandom random;
    private Digest treeDigest;

    public AsymmetricCipherKeyPair generateKeyPair() {
        leafaddr leafaddr = new leafaddr();
        Object obj = new byte[1088];
        this.random.nextBytes(obj);
        Object obj2 = new byte[1056];
        System.arraycopy(obj, 32, obj2, 0, 1024);
        leafaddr.level = 11;
        leafaddr.subtree = 0;
        leafaddr.subleaf = 0;
        Tree.treehash(new HashFunctions(this.treeDigest), obj2, 1024, 5, obj, leafaddr, obj2, 0);
        return new AsymmetricCipherKeyPair(new SPHINCSPublicKeyParameters(obj2), new SPHINCSPrivateKeyParameters(obj));
    }

    public void init(KeyGenerationParameters keyGenerationParameters) {
        this.random = keyGenerationParameters.getRandom();
        this.treeDigest = ((SPHINCS256KeyGenerationParameters) keyGenerationParameters).getTreeDigest();
    }
}
