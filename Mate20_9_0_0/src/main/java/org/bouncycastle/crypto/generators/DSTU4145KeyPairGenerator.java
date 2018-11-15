package org.bouncycastle.crypto.generators;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;

public class DSTU4145KeyPairGenerator extends ECKeyPairGenerator {
    public AsymmetricCipherKeyPair generateKeyPair() {
        AsymmetricCipherKeyPair generateKeyPair = super.generateKeyPair();
        ECPublicKeyParameters eCPublicKeyParameters = (ECPublicKeyParameters) generateKeyPair.getPublic();
        return new AsymmetricCipherKeyPair(new ECPublicKeyParameters(eCPublicKeyParameters.getQ().negate(), eCPublicKeyParameters.getParameters()), (ECPrivateKeyParameters) generateKeyPair.getPrivate());
    }
}
