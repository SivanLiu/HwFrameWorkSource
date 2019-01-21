package org.bouncycastle.operator.jcajce;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.jcajce.util.ProviderJcaJceHelper;
import org.bouncycastle.operator.ContentVerifier;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OperatorStreamException;
import org.bouncycastle.operator.RawContentVerifier;
import org.bouncycastle.operator.RuntimeOperatorException;

public class JcaContentVerifierProviderBuilder {
    private OperatorHelper helper = new OperatorHelper(new DefaultJcaJceHelper());

    private class SignatureOutputStream extends OutputStream {
        private Signature sig;

        SignatureOutputStream(Signature signature) {
            this.sig = signature;
        }

        boolean verify(byte[] bArr) throws SignatureException {
            return this.sig.verify(bArr);
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

    private class SigVerifier implements ContentVerifier {
        private AlgorithmIdentifier algorithm;
        protected SignatureOutputStream stream;

        SigVerifier(AlgorithmIdentifier algorithmIdentifier, SignatureOutputStream signatureOutputStream) {
            this.algorithm = algorithmIdentifier;
            this.stream = signatureOutputStream;
        }

        public AlgorithmIdentifier getAlgorithmIdentifier() {
            return this.algorithm;
        }

        public OutputStream getOutputStream() {
            if (this.stream != null) {
                return this.stream;
            }
            throw new IllegalStateException("verifier not initialised");
        }

        public boolean verify(byte[] bArr) {
            try {
                return this.stream.verify(bArr);
            } catch (SignatureException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception obtaining signature: ");
                stringBuilder.append(e.getMessage());
                throw new RuntimeOperatorException(stringBuilder.toString(), e);
            }
        }
    }

    private class RawSigVerifier extends SigVerifier implements RawContentVerifier {
        private Signature rawSignature;

        RawSigVerifier(AlgorithmIdentifier algorithmIdentifier, SignatureOutputStream signatureOutputStream, Signature signature) {
            super(algorithmIdentifier, signatureOutputStream);
            this.rawSignature = signature;
        }

        public boolean verify(byte[] bArr) {
            try {
                boolean verify = super.verify(bArr);
                return verify;
            } finally {
                try {
                    this.rawSignature.verify(bArr);
                } catch (Exception e) {
                }
            }
        }

        public boolean verify(byte[] bArr, byte[] bArr2) {
            try {
                this.rawSignature.update(bArr);
                boolean verify = this.rawSignature.verify(bArr2);
                try {
                    this.stream.verify(bArr2);
                    return verify;
                } catch (Exception e) {
                    return verify;
                }
            } catch (SignatureException e2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception obtaining raw signature: ");
                stringBuilder.append(e2.getMessage());
                throw new RuntimeOperatorException(stringBuilder.toString(), e2);
            } catch (Throwable th) {
                try {
                    this.stream.verify(bArr2);
                } catch (Exception e3) {
                }
            }
        }
    }

    private Signature createRawSig(AlgorithmIdentifier algorithmIdentifier, PublicKey publicKey) {
        Signature createRawSignature;
        try {
            createRawSignature = this.helper.createRawSignature(algorithmIdentifier);
            if (createRawSignature != null) {
                createRawSignature.initVerify(publicKey);
                return createRawSignature;
            }
        } catch (Exception e) {
            createRawSignature = null;
        }
        return createRawSignature;
    }

    private SignatureOutputStream createSignatureStream(AlgorithmIdentifier algorithmIdentifier, PublicKey publicKey) throws OperatorCreationException {
        try {
            Signature createSignature = this.helper.createSignature(algorithmIdentifier);
            createSignature.initVerify(publicKey);
            return new SignatureOutputStream(createSignature);
        } catch (GeneralSecurityException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception on setup: ");
            stringBuilder.append(e);
            throw new OperatorCreationException(stringBuilder.toString(), e);
        }
    }

    public ContentVerifierProvider build(final PublicKey publicKey) throws OperatorCreationException {
        return new ContentVerifierProvider() {
            public ContentVerifier get(AlgorithmIdentifier algorithmIdentifier) throws OperatorCreationException {
                SignatureOutputStream access$200 = JcaContentVerifierProviderBuilder.this.createSignatureStream(algorithmIdentifier, publicKey);
                Signature access$100 = JcaContentVerifierProviderBuilder.this.createRawSig(algorithmIdentifier, publicKey);
                return access$100 != null ? new RawSigVerifier(algorithmIdentifier, access$200, access$100) : new SigVerifier(algorithmIdentifier, access$200);
            }

            public X509CertificateHolder getAssociatedCertificate() {
                return null;
            }

            public boolean hasAssociatedCertificate() {
                return false;
            }
        };
    }

    public ContentVerifierProvider build(final X509Certificate x509Certificate) throws OperatorCreationException {
        try {
            final JcaX509CertificateHolder jcaX509CertificateHolder = new JcaX509CertificateHolder(x509Certificate);
            return new ContentVerifierProvider() {
                private SignatureOutputStream stream;

                public ContentVerifier get(AlgorithmIdentifier algorithmIdentifier) throws OperatorCreationException {
                    try {
                        Signature createSignature = JcaContentVerifierProviderBuilder.this.helper.createSignature(algorithmIdentifier);
                        createSignature.initVerify(x509Certificate.getPublicKey());
                        this.stream = new SignatureOutputStream(createSignature);
                        createSignature = JcaContentVerifierProviderBuilder.this.createRawSig(algorithmIdentifier, x509Certificate.getPublicKey());
                        return createSignature != null ? new RawSigVerifier(algorithmIdentifier, this.stream, createSignature) : new SigVerifier(algorithmIdentifier, this.stream);
                    } catch (GeneralSecurityException e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("exception on setup: ");
                        stringBuilder.append(e);
                        throw new OperatorCreationException(stringBuilder.toString(), e);
                    }
                }

                public X509CertificateHolder getAssociatedCertificate() {
                    return jcaX509CertificateHolder;
                }

                public boolean hasAssociatedCertificate() {
                    return true;
                }
            };
        } catch (CertificateEncodingException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cannot process certificate: ");
            stringBuilder.append(e.getMessage());
            throw new OperatorCreationException(stringBuilder.toString(), e);
        }
    }

    public ContentVerifierProvider build(SubjectPublicKeyInfo subjectPublicKeyInfo) throws OperatorCreationException {
        return build(this.helper.convertPublicKey(subjectPublicKeyInfo));
    }

    public ContentVerifierProvider build(X509CertificateHolder x509CertificateHolder) throws OperatorCreationException, CertificateException {
        return build(this.helper.convertCertificate(x509CertificateHolder));
    }

    public JcaContentVerifierProviderBuilder setProvider(String str) {
        this.helper = new OperatorHelper(new NamedJcaJceHelper(str));
        return this;
    }

    public JcaContentVerifierProviderBuilder setProvider(Provider provider) {
        this.helper = new OperatorHelper(new ProviderJcaJceHelper(provider));
        return this;
    }
}
