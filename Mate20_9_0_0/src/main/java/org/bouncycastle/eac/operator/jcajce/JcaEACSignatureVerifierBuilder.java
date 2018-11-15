package org.bouncycastle.eac.operator.jcajce;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.eac.EACObjectIdentifiers;
import org.bouncycastle.eac.operator.EACSignatureVerifier;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OperatorStreamException;
import org.bouncycastle.operator.RuntimeOperatorException;

public class JcaEACSignatureVerifierBuilder {
    private EACHelper helper = new DefaultEACHelper();

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
            } catch (Throwable e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception in content signer: ");
                stringBuilder.append(e.getMessage());
                throw new OperatorStreamException(stringBuilder.toString(), e);
            }
        }

        public void write(byte[] bArr) throws IOException {
            try {
                this.sig.update(bArr);
            } catch (Throwable e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception in content signer: ");
                stringBuilder.append(e.getMessage());
                throw new OperatorStreamException(stringBuilder.toString(), e);
            }
        }

        public void write(byte[] bArr, int i, int i2) throws IOException {
            try {
                this.sig.update(bArr, i, i2);
            } catch (Throwable e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("exception in content signer: ");
                stringBuilder.append(e.getMessage());
                throw new OperatorStreamException(stringBuilder.toString(), e);
            }
        }
    }

    private static byte[] derEncode(byte[] bArr) throws IOException {
        int length = bArr.length / 2;
        Object obj = new byte[length];
        Object obj2 = new byte[length];
        System.arraycopy(bArr, 0, obj, 0, length);
        System.arraycopy(bArr, length, obj2, 0, length);
        ASN1EncodableVector aSN1EncodableVector = new ASN1EncodableVector();
        aSN1EncodableVector.add(new ASN1Integer(new BigInteger(1, obj)));
        aSN1EncodableVector.add(new ASN1Integer(new BigInteger(1, obj2)));
        return new DERSequence(aSN1EncodableVector).getEncoded();
    }

    public EACSignatureVerifier build(final ASN1ObjectIdentifier aSN1ObjectIdentifier, PublicKey publicKey) throws OperatorCreationException {
        StringBuilder stringBuilder;
        try {
            Signature signature = this.helper.getSignature(aSN1ObjectIdentifier);
            signature.initVerify(publicKey);
            final SignatureOutputStream signatureOutputStream = new SignatureOutputStream(signature);
            return new EACSignatureVerifier() {
                public OutputStream getOutputStream() {
                    return signatureOutputStream;
                }

                public ASN1ObjectIdentifier getUsageIdentifier() {
                    return aSN1ObjectIdentifier;
                }

                public boolean verify(byte[] bArr) {
                    try {
                        if (!aSN1ObjectIdentifier.on(EACObjectIdentifiers.id_TA_ECDSA)) {
                            return signatureOutputStream.verify(bArr);
                        }
                        try {
                            return signatureOutputStream.verify(JcaEACSignatureVerifierBuilder.derEncode(bArr));
                        } catch (Exception e) {
                            return false;
                        }
                    } catch (Throwable e2) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("exception obtaining signature: ");
                        stringBuilder.append(e2.getMessage());
                        throw new RuntimeOperatorException(stringBuilder.toString(), e2);
                    }
                }
            };
        } catch (Throwable e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("unable to find algorithm: ");
            stringBuilder.append(e.getMessage());
            throw new OperatorCreationException(stringBuilder.toString(), e);
        } catch (Throwable e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("unable to find provider: ");
            stringBuilder.append(e2.getMessage());
            throw new OperatorCreationException(stringBuilder.toString(), e2);
        } catch (Throwable e22) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("invalid key: ");
            stringBuilder.append(e22.getMessage());
            throw new OperatorCreationException(stringBuilder.toString(), e22);
        }
    }

    public JcaEACSignatureVerifierBuilder setProvider(String str) {
        this.helper = new NamedEACHelper(str);
        return this;
    }

    public JcaEACSignatureVerifierBuilder setProvider(Provider provider) {
        this.helper = new ProviderEACHelper(provider);
        return this;
    }
}
