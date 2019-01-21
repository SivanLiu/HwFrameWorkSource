package sun.security.provider.certpath;

import java.io.IOException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import sun.security.util.Debug;
import sun.security.x509.NameConstraintsExtension;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.X509CertImpl;

class ConstraintsChecker extends PKIXCertPathChecker {
    private static final Debug debug = Debug.getInstance("certpath");
    private final int certPathLength;
    private int i;
    private int maxPathLength;
    private NameConstraintsExtension prevNC;
    private Set<String> supportedExts;

    ConstraintsChecker(int certPathLength) {
        this.certPathLength = certPathLength;
    }

    public void init(boolean forward) throws CertPathValidatorException {
        if (forward) {
            throw new CertPathValidatorException("forward checking not supported");
        }
        this.i = 0;
        this.maxPathLength = this.certPathLength;
        this.prevNC = null;
    }

    public boolean isForwardCheckingSupported() {
        return false;
    }

    public Set<String> getSupportedExtensions() {
        if (this.supportedExts == null) {
            this.supportedExts = new HashSet(2);
            this.supportedExts.add(PKIXExtensions.BasicConstraints_Id.toString());
            this.supportedExts.add(PKIXExtensions.NameConstraints_Id.toString());
            this.supportedExts = Collections.unmodifiableSet(this.supportedExts);
        }
        return this.supportedExts;
    }

    public void check(Certificate cert, Collection<String> unresCritExts) throws CertPathValidatorException {
        X509Certificate currCert = (X509Certificate) cert;
        this.i++;
        checkBasicConstraints(currCert);
        verifyNameConstraints(currCert);
        if (unresCritExts != null && !unresCritExts.isEmpty()) {
            unresCritExts.remove(PKIXExtensions.BasicConstraints_Id.toString());
            unresCritExts.remove(PKIXExtensions.NameConstraints_Id.toString());
        }
    }

    private void verifyNameConstraints(X509Certificate currCert) throws CertPathValidatorException {
        Debug debug;
        StringBuilder stringBuilder;
        String msg = "name constraints";
        if (debug != null) {
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("---checking ");
            stringBuilder.append(msg);
            stringBuilder.append("...");
            debug.println(stringBuilder.toString());
        }
        if (this.prevNC != null && (this.i == this.certPathLength || !X509CertImpl.isSelfIssued(currCert))) {
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("prevNC = ");
                stringBuilder.append(this.prevNC);
                stringBuilder.append(", currDN = ");
                stringBuilder.append(currCert.getSubjectX500Principal());
                debug.println(stringBuilder.toString());
            }
            try {
                if (!this.prevNC.verify(currCert)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(msg);
                    stringBuilder.append(" check failed");
                    throw new CertPathValidatorException(stringBuilder.toString(), null, null, -1, PKIXReason.INVALID_NAME);
                }
            } catch (IOException ioe) {
                throw new CertPathValidatorException(ioe);
            }
        }
        this.prevNC = mergeNameConstraints(currCert, this.prevNC);
        if (debug != null) {
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append(" verified.");
            debug.println(stringBuilder.toString());
        }
    }

    static NameConstraintsExtension mergeNameConstraints(X509Certificate currCert, NameConstraintsExtension prevNC) throws CertPathValidatorException {
        try {
            Debug debug;
            StringBuilder stringBuilder;
            Object newConstraints = X509CertImpl.toImpl(currCert).getNameConstraintsExtension();
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("prevNC = ");
                stringBuilder.append((Object) prevNC);
                stringBuilder.append(", newNC = ");
                stringBuilder.append(String.valueOf(newConstraints));
                debug.println(stringBuilder.toString());
            }
            if (prevNC == null) {
                if (debug != null) {
                    debug = debug;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mergedNC = ");
                    stringBuilder.append(String.valueOf(newConstraints));
                    debug.println(stringBuilder.toString());
                }
                if (newConstraints == null) {
                    return newConstraints;
                }
                return (NameConstraintsExtension) newConstraints.clone();
            }
            try {
                prevNC.merge(newConstraints);
                if (debug != null) {
                    debug = debug;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mergedNC = ");
                    stringBuilder.append((Object) prevNC);
                    debug.println(stringBuilder.toString());
                }
                return prevNC;
            } catch (IOException ioe) {
                throw new CertPathValidatorException(ioe);
            }
        } catch (CertificateException ce) {
            throw new CertPathValidatorException(ce);
        }
    }

    private void checkBasicConstraints(X509Certificate currCert) throws CertPathValidatorException {
        Debug debug;
        StringBuilder stringBuilder;
        String msg = "basic constraints";
        if (debug != null) {
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("---checking ");
            stringBuilder.append(msg);
            stringBuilder.append("...");
            debug.println(stringBuilder.toString());
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("i = ");
            stringBuilder.append(this.i);
            stringBuilder.append(", maxPathLength = ");
            stringBuilder.append(this.maxPathLength);
            debug.println(stringBuilder.toString());
        }
        if (this.i < this.certPathLength) {
            int pathLenConstraint = -1;
            if (currCert.getVersion() >= 3) {
                pathLenConstraint = currCert.getBasicConstraints();
            } else if (this.i == 1 && X509CertImpl.isSelfIssued(currCert)) {
                pathLenConstraint = Integer.MAX_VALUE;
            }
            StringBuilder stringBuilder2;
            if (pathLenConstraint != -1) {
                if (!X509CertImpl.isSelfIssued(currCert)) {
                    if (this.maxPathLength > 0) {
                        this.maxPathLength--;
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(msg);
                        stringBuilder2.append(" check failed: pathLenConstraint violated - this cert must be the last cert in the certification path");
                        throw new CertPathValidatorException(stringBuilder2.toString(), null, null, -1, PKIXReason.PATH_TOO_LONG);
                    }
                }
                if (pathLenConstraint < this.maxPathLength) {
                    this.maxPathLength = pathLenConstraint;
                }
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(msg);
                stringBuilder2.append(" check failed: this is not a CA certificate");
                throw new CertPathValidatorException(stringBuilder2.toString(), null, null, -1, PKIXReason.NOT_CA_CERT);
            }
        }
        if (debug != null) {
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("after processing, maxPathLength = ");
            stringBuilder.append(this.maxPathLength);
            debug.println(stringBuilder.toString());
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append(" verified.");
            debug.println(stringBuilder.toString());
        }
    }

    static int mergeBasicConstraints(X509Certificate cert, int maxPathLength) {
        int pathLenConstraint = cert.getBasicConstraints();
        if (!X509CertImpl.isSelfIssued(cert)) {
            maxPathLength--;
        }
        if (pathLenConstraint < maxPathLength) {
            return pathLenConstraint;
        }
        return maxPathLength;
    }
}
