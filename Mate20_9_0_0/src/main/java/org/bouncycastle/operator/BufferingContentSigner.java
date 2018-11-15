package org.bouncycastle.operator;

import java.io.OutputStream;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.util.io.BufferingOutputStream;

public class BufferingContentSigner implements ContentSigner {
    private final ContentSigner contentSigner;
    private final OutputStream output;

    public BufferingContentSigner(ContentSigner contentSigner) {
        this.contentSigner = contentSigner;
        this.output = new BufferingOutputStream(contentSigner.getOutputStream());
    }

    public BufferingContentSigner(ContentSigner contentSigner, int i) {
        this.contentSigner = contentSigner;
        this.output = new BufferingOutputStream(contentSigner.getOutputStream(), i);
    }

    public AlgorithmIdentifier getAlgorithmIdentifier() {
        return this.contentSigner.getAlgorithmIdentifier();
    }

    public OutputStream getOutputStream() {
        return this.output;
    }

    public byte[] getSignature() {
        return this.contentSigner.getSignature();
    }
}
