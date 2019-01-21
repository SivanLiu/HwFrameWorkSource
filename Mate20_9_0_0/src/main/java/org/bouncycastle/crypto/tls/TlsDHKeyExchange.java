package org.bouncycastle.crypto.tls;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;

public class TlsDHKeyExchange extends AbstractTlsKeyExchange {
    protected TlsAgreementCredentials agreementCredentials;
    protected DHPrivateKeyParameters dhAgreePrivateKey;
    protected DHPublicKeyParameters dhAgreePublicKey;
    protected DHParameters dhParameters;
    protected AsymmetricKeyParameter serverPublicKey;
    protected TlsSigner tlsSigner;

    public TlsDHKeyExchange(int i, Vector vector, DHParameters dHParameters) {
        TlsSigner tlsDSSSigner;
        super(i, vector);
        if (i == 3) {
            tlsDSSSigner = new TlsDSSSigner();
        } else if (i == 5) {
            tlsDSSSigner = new TlsRSASigner();
        } else if (i == 7 || i == 9 || i == 11) {
            tlsDSSSigner = null;
        } else {
            throw new IllegalArgumentException("unsupported key exchange algorithm");
        }
        this.tlsSigner = tlsDSSSigner;
        this.dhParameters = dHParameters;
    }

    public void generateClientKeyExchange(OutputStream outputStream) throws IOException {
        if (this.agreementCredentials == null) {
            this.dhAgreePrivateKey = TlsDHUtils.generateEphemeralClientKeyExchange(this.context.getSecureRandom(), this.dhParameters, outputStream);
        }
    }

    public byte[] generatePremasterSecret() throws IOException {
        if (this.agreementCredentials != null) {
            return this.agreementCredentials.generateAgreement(this.dhAgreePublicKey);
        }
        if (this.dhAgreePrivateKey != null) {
            return TlsDHUtils.calculateDHBasicAgreement(this.dhAgreePublicKey, this.dhAgreePrivateKey);
        }
        throw new TlsFatalAlert((short) 80);
    }

    public byte[] generateServerKeyExchange() throws IOException {
        if (!requiresServerKeyExchange()) {
            return null;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        this.dhAgreePrivateKey = TlsDHUtils.generateEphemeralServerKeyExchange(this.context.getSecureRandom(), this.dhParameters, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    protected int getMinimumPrimeBits() {
        return 1024;
    }

    public void init(TlsContext tlsContext) {
        super.init(tlsContext);
        if (this.tlsSigner != null) {
            this.tlsSigner.init(tlsContext);
        }
    }

    public void processClientCertificate(Certificate certificate) throws IOException {
        if (this.keyExchange == 11) {
            throw new TlsFatalAlert((short) 10);
        }
    }

    public void processClientCredentials(TlsCredentials tlsCredentials) throws IOException {
        if (this.keyExchange == 11) {
            throw new TlsFatalAlert((short) 80);
        } else if (tlsCredentials instanceof TlsAgreementCredentials) {
            this.agreementCredentials = (TlsAgreementCredentials) tlsCredentials;
        } else if (!(tlsCredentials instanceof TlsSignerCredentials)) {
            throw new TlsFatalAlert((short) 80);
        }
    }

    public void processClientKeyExchange(InputStream inputStream) throws IOException {
        if (this.dhAgreePublicKey == null) {
            this.dhAgreePublicKey = TlsDHUtils.validateDHPublicKey(new DHPublicKeyParameters(TlsDHUtils.readDHParameter(inputStream), this.dhParameters));
        }
    }

    public void processServerCertificate(Certificate certificate) throws IOException {
        if (this.keyExchange == 11) {
            throw new TlsFatalAlert((short) 10);
        } else if (certificate.isEmpty()) {
            throw new TlsFatalAlert((short) 42);
        } else {
            Certificate certificateAt = certificate.getCertificateAt(0);
            try {
                int i;
                this.serverPublicKey = PublicKeyFactory.createKey(certificateAt.getSubjectPublicKeyInfo());
                if (this.tlsSigner == null) {
                    try {
                        this.dhAgreePublicKey = TlsDHUtils.validateDHPublicKey((DHPublicKeyParameters) this.serverPublicKey);
                        this.dhParameters = validateDHParameters(this.dhAgreePublicKey.getParameters());
                        i = 8;
                    } catch (ClassCastException e) {
                        throw new TlsFatalAlert((short) 46, e);
                    }
                } else if (this.tlsSigner.isValidPublicKey(this.serverPublicKey)) {
                    i = 128;
                } else {
                    throw new TlsFatalAlert((short) 46);
                }
                TlsUtils.validateKeyUsage(certificateAt, i);
                super.processServerCertificate(certificate);
            } catch (RuntimeException e2) {
                throw new TlsFatalAlert((short) 43, e2);
            }
        }
    }

    public void processServerKeyExchange(InputStream inputStream) throws IOException {
        if (requiresServerKeyExchange()) {
            this.dhAgreePublicKey = TlsDHUtils.validateDHPublicKey(ServerDHParams.parse(inputStream).getPublicKey());
            this.dhParameters = validateDHParameters(this.dhAgreePublicKey.getParameters());
            return;
        }
        throw new TlsFatalAlert((short) 10);
    }

    public boolean requiresServerKeyExchange() {
        int i = this.keyExchange;
        return i == 3 || i == 5 || i == 11;
    }

    public void skipServerCredentials() throws IOException {
        if (this.keyExchange != 11) {
            throw new TlsFatalAlert((short) 10);
        }
    }

    public void validateCertificateRequest(CertificateRequest certificateRequest) throws IOException {
        if (this.keyExchange != 11) {
            short[] certificateTypes = certificateRequest.getCertificateTypes();
            for (short s : certificateTypes) {
                if (s != (short) 64) {
                    switch (s) {
                        case (short) 1:
                        case (short) 2:
                        case (short) 3:
                        case (short) 4:
                            break;
                        default:
                            throw new TlsFatalAlert((short) 47);
                    }
                }
            }
            return;
        }
        throw new TlsFatalAlert((short) 40);
    }

    protected DHParameters validateDHParameters(DHParameters dHParameters) throws IOException {
        if (dHParameters.getP().bitLength() >= getMinimumPrimeBits()) {
            return TlsDHUtils.validateDHParameters(dHParameters);
        }
        throw new TlsFatalAlert((short) 71);
    }
}
