package sun.security.provider.certpath;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Debug;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;

class BasicChecker extends PKIXCertPathChecker {
    private static final Debug debug = Debug.getInstance("certpath");
    private final X500Principal caName;
    private final Date date;
    private PublicKey prevPubKey;
    private X500Principal prevSubject;
    private final boolean sigOnly;
    private final String sigProvider;
    private final PublicKey trustedPubKey;

    BasicChecker(TrustAnchor anchor, Date date, String sigProvider, boolean sigOnly) {
        if (anchor.getTrustedCert() != null) {
            this.trustedPubKey = anchor.getTrustedCert().getPublicKey();
            this.caName = anchor.getTrustedCert().getSubjectX500Principal();
        } else {
            this.trustedPubKey = anchor.getCAPublicKey();
            this.caName = anchor.getCA();
        }
        this.date = date;
        this.sigProvider = sigProvider;
        this.sigOnly = sigOnly;
        this.prevPubKey = this.trustedPubKey;
    }

    public void init(boolean forward) throws CertPathValidatorException {
        if (forward) {
            throw new CertPathValidatorException("forward checking not supported");
        }
        this.prevPubKey = this.trustedPubKey;
        if (PKIX.isDSAPublicKeyWithoutParams(this.prevPubKey)) {
            throw new CertPathValidatorException("Key parameters missing");
        }
        this.prevSubject = this.caName;
    }

    public boolean isForwardCheckingSupported() {
        return false;
    }

    public Set<String> getSupportedExtensions() {
        return null;
    }

    public void check(Certificate cert, Collection<String> collection) throws CertPathValidatorException {
        X509Certificate currCert = (X509Certificate) cert;
        if (!this.sigOnly) {
            verifyTimestamp(currCert);
            verifyNameChaining(currCert);
        }
        verifySignature(currCert);
        updateState(currCert);
    }

    private void verifySignature(X509Certificate cert) throws CertPathValidatorException {
        Debug debug;
        StringBuilder stringBuilder;
        String msg = X509CertImpl.SIGNATURE;
        if (debug != null) {
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("---checking ");
            stringBuilder.append(msg);
            stringBuilder.append("...");
            debug.println(stringBuilder.toString());
        }
        try {
            if (this.sigProvider != null) {
                cert.verify(this.prevPubKey, this.sigProvider);
            } else {
                cert.verify(this.prevPubKey);
            }
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append(msg);
                stringBuilder.append(" verified.");
                debug.println(stringBuilder.toString());
            }
        } catch (SignatureException e) {
            SignatureException e2 = e;
            stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append(" check failed");
            throw new CertPathValidatorException(stringBuilder.toString(), e2, null, -1, BasicReason.INVALID_SIGNATURE);
        } catch (GeneralSecurityException e3) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(msg);
            stringBuilder2.append(" check failed");
            throw new CertPathValidatorException(stringBuilder2.toString(), e3);
        }
    }

    private void verifyTimestamp(X509Certificate cert) throws CertPathValidatorException {
        Debug debug;
        StringBuilder stringBuilder;
        String msg = "timestamp";
        if (debug != null) {
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("---checking ");
            stringBuilder.append(msg);
            stringBuilder.append(":");
            stringBuilder.append(this.date.toString());
            stringBuilder.append("...");
            debug.println(stringBuilder.toString());
        }
        try {
            cert.checkValidity(this.date);
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append(msg);
                stringBuilder.append(" verified.");
                debug.println(stringBuilder.toString());
            }
        } catch (CertificateExpiredException e) {
            CertificateExpiredException e2 = e;
            stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append(" check failed");
            throw new CertPathValidatorException(stringBuilder.toString(), e2, null, -1, BasicReason.EXPIRED);
        } catch (CertificateNotYetValidException e3) {
            CertificateNotYetValidException e4 = e3;
            stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append(" check failed");
            throw new CertPathValidatorException(stringBuilder.toString(), e4, null, -1, BasicReason.NOT_YET_VALID);
        }
    }

    private void verifyNameChaining(X509Certificate cert) throws CertPathValidatorException {
        if (this.prevSubject != null) {
            String msg = "subject/issuer name chaining";
            if (debug != null) {
                Debug debug = debug;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("---checking ");
                stringBuilder.append(msg);
                stringBuilder.append("...");
                debug.println(stringBuilder.toString());
            }
            X500Principal currIssuer = cert.getIssuerX500Principal();
            StringBuilder stringBuilder2;
            if (X500Name.asX500Name(currIssuer).isEmpty()) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(msg);
                stringBuilder2.append(" check failed: empty/null issuer DN in certificate is invalid");
                throw new CertPathValidatorException(stringBuilder2.toString(), null, null, -1, PKIXReason.NAME_CHAINING);
            } else if (!currIssuer.equals(this.prevSubject)) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(msg);
                stringBuilder2.append(" check failed");
                throw new CertPathValidatorException(stringBuilder2.toString(), null, null, -1, PKIXReason.NAME_CHAINING);
            } else if (debug != null) {
                Debug debug2 = debug;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(msg);
                stringBuilder2.append(" verified.");
                debug2.println(stringBuilder2.toString());
            }
        }
    }

    private void updateState(X509Certificate currCert) throws CertPathValidatorException {
        PublicKey cKey = currCert.getPublicKey();
        if (debug != null) {
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BasicChecker.updateState issuer: ");
            stringBuilder.append(currCert.getIssuerX500Principal().toString());
            stringBuilder.append("; subject: ");
            stringBuilder.append(currCert.getSubjectX500Principal());
            stringBuilder.append("; serial#: ");
            stringBuilder.append(currCert.getSerialNumber().toString());
            debug.println(stringBuilder.toString());
        }
        if (PKIX.isDSAPublicKeyWithoutParams(cKey)) {
            cKey = makeInheritedParamsKey(cKey, this.prevPubKey);
            if (debug != null) {
                debug.println("BasicChecker.updateState Made key with inherited params");
            }
        }
        this.prevPubKey = cKey;
        this.prevSubject = currCert.getSubjectX500Principal();
    }

    static PublicKey makeInheritedParamsKey(PublicKey keyValueKey, PublicKey keyParamsKey) throws CertPathValidatorException {
        if ((keyValueKey instanceof DSAPublicKey) && (keyParamsKey instanceof DSAPublicKey)) {
            DSAParams params = ((DSAPublicKey) keyParamsKey).getParams();
            if (params != null) {
                try {
                    return KeyFactory.getInstance("DSA").generatePublic(new DSAPublicKeySpec(((DSAPublicKey) keyValueKey).getY(), params.getP(), params.getQ(), params.getG()));
                } catch (GeneralSecurityException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to generate key with inherited parameters: ");
                    stringBuilder.append(e.getMessage());
                    throw new CertPathValidatorException(stringBuilder.toString(), e);
                }
            }
            throw new CertPathValidatorException("Key parameters missing");
        }
        throw new CertPathValidatorException("Input key is not appropriate type for inheriting parameters");
    }

    PublicKey getPublicKey() {
        return this.prevPubKey;
    }
}
