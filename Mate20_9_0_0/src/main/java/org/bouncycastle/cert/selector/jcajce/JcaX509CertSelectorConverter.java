package org.bouncycastle.cert.selector.jcajce;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509CertSelector;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.selector.X509CertificateHolderSelector;

public class JcaX509CertSelectorConverter {
    protected X509CertSelector doConversion(X500Name x500Name, BigInteger bigInteger, byte[] bArr) {
        StringBuilder stringBuilder;
        X509CertSelector x509CertSelector = new X509CertSelector();
        if (x500Name != null) {
            try {
                x509CertSelector.setIssuer(x500Name.getEncoded());
            } catch (IOException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("unable to convert issuer: ");
                stringBuilder.append(e.getMessage());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }
        if (bigInteger != null) {
            x509CertSelector.setSerialNumber(bigInteger);
        }
        if (bArr == null) {
            return x509CertSelector;
        }
        try {
            x509CertSelector.setSubjectKeyIdentifier(new DEROctetString(bArr).getEncoded());
            return x509CertSelector;
        } catch (IOException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("unable to convert issuer: ");
            stringBuilder.append(e2.getMessage());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public X509CertSelector getCertSelector(X509CertificateHolderSelector x509CertificateHolderSelector) {
        return doConversion(x509CertificateHolderSelector.getIssuer(), x509CertificateHolderSelector.getSerialNumber(), x509CertificateHolderSelector.getSubjectKeyIdentifier());
    }
}
