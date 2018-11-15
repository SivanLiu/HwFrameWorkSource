package com.android.server.security.securityprofile;

import android.util.Slog;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class CertificateVerifier {
    static final String root = "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";
    String TAG = "SecurityProfileService";
    KeyStore trustStore;

    public CertificateVerifier() {
        String str;
        StringBuilder stringBuilder;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            this.trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            this.trustStore.load(null, null);
            this.trustStore.setCertificateEntry("root", cf.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(root))));
        } catch (CertificateException e) {
            str = this.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("CertificateException:");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
        } catch (NoSuchAlgorithmException e2) {
            str = this.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NoSuchAlgorithmException:");
            stringBuilder.append(e2.getMessage());
            Slog.e(str, stringBuilder.toString());
        } catch (KeyStoreException e3) {
            str = this.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("KeyStoreException:");
            stringBuilder.append(e3.getMessage());
            Slog.e(str, stringBuilder.toString());
        } catch (IOException e4) {
            str = this.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("IOException:");
            stringBuilder.append(e4.getMessage());
            Slog.e(str, stringBuilder.toString());
        }
    }

    public boolean verifyCertificateChain(List<Certificate> certChain, Date signingDate) {
        String str;
        StringBuilder stringBuilder;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            List<Certificate> certx = new ArrayList(certChain.size());
            for (Certificate c : certChain) {
                certx.add(c);
            }
            CertPath path = cf.generateCertPath(certx);
            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            PKIXParameters params = new PKIXParameters(this.trustStore);
            params.setRevocationEnabled(false);
            params.setDate(signingDate);
            params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters()));
            PKIXCertPathValidatorResult r = (PKIXCertPathValidatorResult) validator.validate(path, params);
            if (params.getTrustAnchors().contains(r.getTrustAnchor())) {
                return true;
            }
            String str2 = this.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("anchor is not trusted:");
            stringBuilder2.append(Base64.getEncoder().encodeToString(r.getTrustAnchor().getTrustedCert().getEncoded()));
            Slog.e(str2, stringBuilder2.toString());
            return false;
        } catch (CertPathValidatorException e) {
            str = this.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("CertPathValidatorException:");
            stringBuilder.append(e.getMessage());
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (InvalidAlgorithmParameterException e2) {
            str = this.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("InvalidAlgorithmParameterException:");
            stringBuilder.append(e2.getMessage());
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (CertificateException e3) {
            str = this.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("CertificateException:");
            stringBuilder.append(e3.getMessage());
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (NoSuchAlgorithmException e4) {
            str = this.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("NoSuchAlgorithmException:");
            stringBuilder.append(e4.getMessage());
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (KeyStoreException e5) {
            str = this.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("KeyStoreException:");
            stringBuilder.append(e5.getMessage());
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (Exception e6) {
            str = this.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("verifyCertificateChain Exception:");
            stringBuilder.append(e6.getMessage());
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }
}
