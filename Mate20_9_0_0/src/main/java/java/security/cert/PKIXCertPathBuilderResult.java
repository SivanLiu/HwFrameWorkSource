package java.security.cert;

import java.security.PublicKey;

public class PKIXCertPathBuilderResult extends PKIXCertPathValidatorResult implements CertPathBuilderResult {
    private CertPath certPath;

    public PKIXCertPathBuilderResult(CertPath certPath, TrustAnchor trustAnchor, PolicyNode policyTree, PublicKey subjectPublicKey) {
        super(trustAnchor, policyTree, subjectPublicKey);
        if (certPath != null) {
            this.certPath = certPath;
            return;
        }
        throw new NullPointerException("certPath must be non-null");
    }

    public CertPath getCertPath() {
        return this.certPath;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("PKIXCertPathBuilderResult: [\n");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  Certification Path: ");
        stringBuilder.append(this.certPath);
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  Trust Anchor: ");
        stringBuilder.append(getTrustAnchor().toString());
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  Policy Tree: ");
        stringBuilder.append(String.valueOf(getPolicyTree()));
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  Subject Public Key: ");
        stringBuilder.append(getPublicKey());
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        sb.append("]");
        return sb.toString();
    }
}
