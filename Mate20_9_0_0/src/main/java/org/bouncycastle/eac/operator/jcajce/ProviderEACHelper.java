package org.bouncycastle.eac.operator.jcajce;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Signature;

class ProviderEACHelper extends EACHelper {
    private final Provider provider;

    ProviderEACHelper(Provider provider) {
        this.provider = provider;
    }

    protected Signature createSignature(String str) throws NoSuchAlgorithmException {
        return Signature.getInstance(str, this.provider);
    }
}
