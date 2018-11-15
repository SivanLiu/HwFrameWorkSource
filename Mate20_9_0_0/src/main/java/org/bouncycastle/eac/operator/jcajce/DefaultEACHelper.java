package org.bouncycastle.eac.operator.jcajce;

import java.security.NoSuchAlgorithmException;
import java.security.Signature;

class DefaultEACHelper extends EACHelper {
    DefaultEACHelper() {
    }

    protected Signature createSignature(String str) throws NoSuchAlgorithmException {
        return Signature.getInstance(str);
    }
}
