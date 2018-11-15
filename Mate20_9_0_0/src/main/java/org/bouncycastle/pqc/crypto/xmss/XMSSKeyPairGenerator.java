package org.bouncycastle.pqc.crypto.xmss;

import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.pqc.crypto.xmss.XMSSPrivateKeyParameters.Builder;

public final class XMSSKeyPairGenerator {
    private XMSSParameters params;
    private SecureRandom prng;

    private XMSSPrivateKeyParameters generatePrivateKey(XMSSParameters xMSSParameters, SecureRandom secureRandom) {
        int digestSize = xMSSParameters.getDigestSize();
        byte[] bArr = new byte[digestSize];
        secureRandom.nextBytes(bArr);
        byte[] bArr2 = new byte[digestSize];
        secureRandom.nextBytes(bArr2);
        byte[] bArr3 = new byte[digestSize];
        secureRandom.nextBytes(bArr3);
        return new Builder(xMSSParameters).withSecretKeySeed(bArr).withSecretKeyPRF(bArr2).withPublicSeed(bArr3).withBDSState(new BDS(xMSSParameters, bArr3, bArr, (OTSHashAddress) new Builder().build())).build();
    }

    public AsymmetricCipherKeyPair generateKeyPair() {
        XMSSPrivateKeyParameters generatePrivateKey = generatePrivateKey(this.params, this.prng);
        XMSSNode root = generatePrivateKey.getBDSState().getRoot();
        AsymmetricKeyParameter build = new Builder(this.params).withSecretKeySeed(generatePrivateKey.getSecretKeySeed()).withSecretKeyPRF(generatePrivateKey.getSecretKeyPRF()).withPublicSeed(generatePrivateKey.getPublicSeed()).withRoot(root.getValue()).withBDSState(generatePrivateKey.getBDSState()).build();
        return new AsymmetricCipherKeyPair(new XMSSPublicKeyParameters.Builder(this.params).withRoot(root.getValue()).withPublicSeed(build.getPublicSeed()).build(), build);
    }

    public void init(KeyGenerationParameters keyGenerationParameters) {
        XMSSKeyGenerationParameters xMSSKeyGenerationParameters = (XMSSKeyGenerationParameters) keyGenerationParameters;
        this.prng = xMSSKeyGenerationParameters.getRandom();
        this.params = xMSSKeyGenerationParameters.getParameters();
    }
}
