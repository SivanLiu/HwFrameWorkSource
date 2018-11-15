package org.bouncycastle.cert.path.validations;

import java.util.Collection;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.path.CertPathValidation;
import org.bouncycastle.cert.path.CertPathValidationContext;
import org.bouncycastle.cert.path.CertPathValidationException;
import org.bouncycastle.util.Memoable;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;

public class CRLValidation implements CertPathValidation {
    private Store crls;
    private X500Name workingIssuerName;

    public CRLValidation(X500Name x500Name, Store store) {
        this.workingIssuerName = x500Name;
        this.crls = store;
    }

    public Memoable copy() {
        return new CRLValidation(this.workingIssuerName, this.crls);
    }

    public void reset(Memoable memoable) {
        CRLValidation cRLValidation = (CRLValidation) memoable;
        this.workingIssuerName = cRLValidation.workingIssuerName;
        this.crls = cRLValidation.crls;
    }

    public void validate(CertPathValidationContext certPathValidationContext, X509CertificateHolder x509CertificateHolder) throws CertPathValidationException {
        Collection<X509CRLHolder> matches = this.crls.getMatches(new Selector() {
            public Object clone() {
                return this;
            }

            public boolean match(Object obj) {
                return ((X509CRLHolder) obj).getIssuer().equals(CRLValidation.this.workingIssuerName);
            }
        });
        if (matches.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CRL for ");
            stringBuilder.append(this.workingIssuerName);
            stringBuilder.append(" not found");
            throw new CertPathValidationException(stringBuilder.toString());
        }
        for (X509CRLHolder revokedCertificate : matches) {
            if (revokedCertificate.getRevokedCertificate(x509CertificateHolder.getSerialNumber()) != null) {
                throw new CertPathValidationException("Certificate revoked");
            }
        }
        this.workingIssuerName = x509CertificateHolder.getSubject();
    }
}
