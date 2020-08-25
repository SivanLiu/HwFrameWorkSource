package org.bouncycastle.crypto.tls;

import org.bouncycastle.crypto.params.DHParameters;

public interface TlsDHVerifier {
    boolean accept(DHParameters dHParameters);
}
