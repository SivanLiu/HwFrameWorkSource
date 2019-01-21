package org.bouncycastle.operator.bc;

import java.io.OutputStream;
import java.security.SecureRandom;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.RuntimeOperatorException;

public abstract class BcContentSignerBuilder {
    private AlgorithmIdentifier digAlgId;
    protected BcDigestProvider digestProvider = BcDefaultDigestProvider.INSTANCE;
    private SecureRandom random;
    private AlgorithmIdentifier sigAlgId;

    public BcContentSignerBuilder(AlgorithmIdentifier algorithmIdentifier, AlgorithmIdentifier algorithmIdentifier2) {
        this.sigAlgId = algorithmIdentifier;
        this.digAlgId = algorithmIdentifier2;
    }

    public ContentSigner build(AsymmetricKeyParameter asymmetricKeyParameter) throws OperatorCreationException {
        final Signer createSigner = createSigner(this.sigAlgId, this.digAlgId);
        if (this.random != null) {
            createSigner.init(true, new ParametersWithRandom(asymmetricKeyParameter, this.random));
        } else {
            createSigner.init(true, asymmetricKeyParameter);
        }
        return new ContentSigner() {
            private BcSignerOutputStream stream = new BcSignerOutputStream(createSigner);

            public AlgorithmIdentifier getAlgorithmIdentifier() {
                return BcContentSignerBuilder.this.sigAlgId;
            }

            public OutputStream getOutputStream() {
                return this.stream;
            }

            public byte[] getSignature() {
                try {
                    return this.stream.getSignature();
                } catch (CryptoException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("exception obtaining signature: ");
                    stringBuilder.append(e.getMessage());
                    throw new RuntimeOperatorException(stringBuilder.toString(), e);
                }
            }
        };
    }

    protected abstract Signer createSigner(AlgorithmIdentifier algorithmIdentifier, AlgorithmIdentifier algorithmIdentifier2) throws OperatorCreationException;

    public BcContentSignerBuilder setSecureRandom(SecureRandom secureRandom) {
        this.random = secureRandom;
        return this;
    }
}
