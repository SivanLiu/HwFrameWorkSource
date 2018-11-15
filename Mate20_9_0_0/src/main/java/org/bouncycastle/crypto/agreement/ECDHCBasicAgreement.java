package org.bouncycastle.crypto.agreement;

import java.math.BigInteger;
import org.bouncycastle.crypto.BasicAgreement;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;

public class ECDHCBasicAgreement implements BasicAgreement {
    ECPrivateKeyParameters key;

    public BigInteger calculateAgreement(CipherParameters cipherParameters) {
        ECPublicKeyParameters eCPublicKeyParameters = (ECPublicKeyParameters) cipherParameters;
        ECDomainParameters parameters = eCPublicKeyParameters.getParameters();
        if (parameters.equals(this.key.getParameters())) {
            ECPoint normalize = eCPublicKeyParameters.getQ().multiply(parameters.getH().multiply(this.key.getD()).mod(parameters.getN())).normalize();
            if (!normalize.isInfinity()) {
                return normalize.getAffineXCoord().toBigInteger();
            }
            throw new IllegalStateException("Infinity is not a valid agreement value for ECDHC");
        }
        throw new IllegalStateException("ECDHC public key has wrong domain parameters");
    }

    public int getFieldSize() {
        return (this.key.getParameters().getCurve().getFieldSize() + 7) / 8;
    }

    public void init(CipherParameters cipherParameters) {
        this.key = (ECPrivateKeyParameters) cipherParameters;
    }
}
