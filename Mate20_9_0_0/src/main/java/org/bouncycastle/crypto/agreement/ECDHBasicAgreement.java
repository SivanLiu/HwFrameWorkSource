package org.bouncycastle.crypto.agreement;

import java.math.BigInteger;
import org.bouncycastle.crypto.BasicAgreement;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;

public class ECDHBasicAgreement implements BasicAgreement {
    private ECPrivateKeyParameters key;

    public BigInteger calculateAgreement(CipherParameters cipherParameters) {
        ECPublicKeyParameters eCPublicKeyParameters = (ECPublicKeyParameters) cipherParameters;
        if (eCPublicKeyParameters.getParameters().equals(this.key.getParameters())) {
            ECPoint decodePoint = this.key.getParameters().getCurve().decodePoint(eCPublicKeyParameters.getQ().getEncoded(false));
            if (decodePoint.isInfinity()) {
                throw new IllegalStateException("Infinity is not a valid public key for ECDH");
            }
            decodePoint = decodePoint.multiply(this.key.getD()).normalize();
            if (!decodePoint.isInfinity()) {
                return decodePoint.getAffineXCoord().toBigInteger();
            }
            throw new IllegalStateException("Infinity is not a valid agreement value for ECDH");
        }
        throw new IllegalStateException("ECDH public key has wrong domain parameters");
    }

    public int getFieldSize() {
        return (this.key.getParameters().getCurve().getFieldSize() + 7) / 8;
    }

    public void init(CipherParameters cipherParameters) {
        this.key = (ECPrivateKeyParameters) cipherParameters;
    }
}
