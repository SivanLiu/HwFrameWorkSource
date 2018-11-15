package org.bouncycastle.crypto.tls;

import org.bouncycastle.crypto.DSA;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.signers.DSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;

public class TlsDSSSigner extends TlsDSASigner {
    protected DSA createDSAImpl(short s) {
        return new DSASigner(new HMacDSAKCalculator(TlsUtils.createHash(s)));
    }

    protected short getSignatureAlgorithm() {
        return (short) 2;
    }

    public boolean isValidPublicKey(AsymmetricKeyParameter asymmetricKeyParameter) {
        return asymmetricKeyParameter instanceof DSAPublicKeyParameters;
    }
}
