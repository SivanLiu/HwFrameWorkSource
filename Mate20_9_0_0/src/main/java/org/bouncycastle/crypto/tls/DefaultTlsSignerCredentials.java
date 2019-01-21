package org.bouncycastle.crypto.tls;

import java.io.IOException;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;

public class DefaultTlsSignerCredentials extends AbstractTlsSignerCredentials {
    protected Certificate certificate;
    protected TlsContext context;
    protected AsymmetricKeyParameter privateKey;
    protected SignatureAndHashAlgorithm signatureAndHashAlgorithm;
    protected TlsSigner signer;

    public DefaultTlsSignerCredentials(TlsContext tlsContext, Certificate certificate, AsymmetricKeyParameter asymmetricKeyParameter) {
        this(tlsContext, certificate, asymmetricKeyParameter, null);
    }

    public DefaultTlsSignerCredentials(TlsContext tlsContext, Certificate certificate, AsymmetricKeyParameter asymmetricKeyParameter, SignatureAndHashAlgorithm signatureAndHashAlgorithm) {
        if (certificate == null) {
            throw new IllegalArgumentException("'certificate' cannot be null");
        } else if (certificate.isEmpty()) {
            throw new IllegalArgumentException("'certificate' cannot be empty");
        } else if (asymmetricKeyParameter == null) {
            throw new IllegalArgumentException("'privateKey' cannot be null");
        } else if (!asymmetricKeyParameter.isPrivate()) {
            throw new IllegalArgumentException("'privateKey' must be private");
        } else if (TlsUtils.isTLSv12(tlsContext) && signatureAndHashAlgorithm == null) {
            throw new IllegalArgumentException("'signatureAndHashAlgorithm' cannot be null for (D)TLS 1.2+");
        } else {
            TlsSigner tlsRSASigner;
            if (asymmetricKeyParameter instanceof RSAKeyParameters) {
                tlsRSASigner = new TlsRSASigner();
            } else if (asymmetricKeyParameter instanceof DSAPrivateKeyParameters) {
                tlsRSASigner = new TlsDSSSigner();
            } else if (asymmetricKeyParameter instanceof ECPrivateKeyParameters) {
                tlsRSASigner = new TlsECDSASigner();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("'privateKey' type not supported: ");
                stringBuilder.append(asymmetricKeyParameter.getClass().getName());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.signer = tlsRSASigner;
            this.signer.init(tlsContext);
            this.context = tlsContext;
            this.certificate = certificate;
            this.privateKey = asymmetricKeyParameter;
            this.signatureAndHashAlgorithm = signatureAndHashAlgorithm;
        }
    }

    public byte[] generateCertificateSignature(byte[] bArr) throws IOException {
        try {
            return TlsUtils.isTLSv12(this.context) ? this.signer.generateRawSignature(this.signatureAndHashAlgorithm, this.privateKey, bArr) : this.signer.generateRawSignature(this.privateKey, bArr);
        } catch (CryptoException e) {
            throw new TlsFatalAlert((short) 80, e);
        }
    }

    public Certificate getCertificate() {
        return this.certificate;
    }

    public SignatureAndHashAlgorithm getSignatureAndHashAlgorithm() {
        return this.signatureAndHashAlgorithm;
    }
}
