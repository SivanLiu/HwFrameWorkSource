package org.bouncycastle.operator.jcajce;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.jcajce.util.ProviderJcaJceHelper;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OperatorStreamException;
import org.bouncycastle.operator.RuntimeOperatorException;

public class JcaContentSignerBuilder {
    private OperatorHelper helper = new OperatorHelper(new DefaultJcaJceHelper());
    private SecureRandom random;
    private AlgorithmIdentifier sigAlgId;
    private String signatureAlgorithm;

    private class SignatureOutputStream extends OutputStream {
        private Signature sig;

        SignatureOutputStream(Signature signature) {
            this.sig = signature;
        }

        byte[] getSignature() throws SignatureException {
            return this.sig.sign();
        }

        public void write(int i) throws IOException {
            try {
                this.sig.update((byte) i);
            } catch (SignatureException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception in content signer: ");
                stringBuilder.append(e.getMessage());
                throw new OperatorStreamException(stringBuilder.toString(), e);
            }
        }

        public void write(byte[] bArr) throws IOException {
            try {
                this.sig.update(bArr);
            } catch (SignatureException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception in content signer: ");
                stringBuilder.append(e.getMessage());
                throw new OperatorStreamException(stringBuilder.toString(), e);
            }
        }

        public void write(byte[] bArr, int i, int i2) throws IOException {
            try {
                this.sig.update(bArr, i, i2);
            } catch (SignatureException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception in content signer: ");
                stringBuilder.append(e.getMessage());
                throw new OperatorStreamException(stringBuilder.toString(), e);
            }
        }
    }

    public JcaContentSignerBuilder(String str) {
        this.signatureAlgorithm = str;
        this.sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(str);
    }

    public ContentSigner build(PrivateKey privateKey) throws OperatorCreationException {
        try {
            final Signature createSignature = this.helper.createSignature(this.sigAlgId);
            final AlgorithmIdentifier algorithmIdentifier = this.sigAlgId;
            if (this.random != null) {
                createSignature.initSign(privateKey, this.random);
            } else {
                createSignature.initSign(privateKey);
            }
            return new ContentSigner() {
                private SignatureOutputStream stream = new SignatureOutputStream(createSignature);

                public AlgorithmIdentifier getAlgorithmIdentifier() {
                    return algorithmIdentifier;
                }

                public OutputStream getOutputStream() {
                    return this.stream;
                }

                public byte[] getSignature() {
                    try {
                        return this.stream.getSignature();
                    } catch (SignatureException e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("exception obtaining signature: ");
                        stringBuilder.append(e.getMessage());
                        throw new RuntimeOperatorException(stringBuilder.toString(), e);
                    }
                }
            };
        } catch (GeneralSecurityException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cannot create signer: ");
            stringBuilder.append(e.getMessage());
            throw new OperatorCreationException(stringBuilder.toString(), e);
        }
    }

    public JcaContentSignerBuilder setProvider(String str) {
        this.helper = new OperatorHelper(new NamedJcaJceHelper(str));
        return this;
    }

    public JcaContentSignerBuilder setProvider(Provider provider) {
        this.helper = new OperatorHelper(new ProviderJcaJceHelper(provider));
        return this;
    }

    public JcaContentSignerBuilder setSecureRandom(SecureRandom secureRandom) {
        this.random = secureRandom;
        return this;
    }
}
