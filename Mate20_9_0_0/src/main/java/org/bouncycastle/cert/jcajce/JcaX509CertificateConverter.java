package org.bouncycastle.cert.jcajce;

import java.io.ByteArrayInputStream;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import org.bouncycastle.cert.X509CertificateHolder;

public class JcaX509CertificateConverter {
    private CertHelper helper;

    private class ExCertificateException extends CertificateException {
        private Throwable cause;

        public ExCertificateException(String str, Throwable th) {
            super(str);
            this.cause = th;
        }

        public Throwable getCause() {
            return this.cause;
        }
    }

    private class ExCertificateParsingException extends CertificateParsingException {
        private Throwable cause;

        public ExCertificateParsingException(String str, Throwable th) {
            super(str);
            this.cause = th;
        }

        public Throwable getCause() {
            return this.cause;
        }
    }

    public JcaX509CertificateConverter() {
        this.helper = new DefaultCertHelper();
        this.helper = new DefaultCertHelper();
    }

    public X509Certificate getCertificate(X509CertificateHolder x509CertificateHolder) throws CertificateException {
        StringBuilder stringBuilder;
        try {
            return (X509Certificate) this.helper.getCertificateFactory("X.509").generateCertificate(new ByteArrayInputStream(x509CertificateHolder.getEncoded()));
        } catch (Throwable e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception parsing certificate: ");
            stringBuilder.append(e.getMessage());
            throw new ExCertificateParsingException(stringBuilder.toString(), e);
        } catch (Throwable e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("cannot find required provider:");
            stringBuilder.append(e2.getMessage());
            throw new ExCertificateException(stringBuilder.toString(), e2);
        }
    }

    public JcaX509CertificateConverter setProvider(String str) {
        this.helper = new NamedCertHelper(str);
        return this;
    }

    public JcaX509CertificateConverter setProvider(Provider provider) {
        this.helper = new ProviderCertHelper(provider);
        return this;
    }
}
