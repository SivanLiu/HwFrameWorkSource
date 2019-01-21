package java.security.cert;

import java.security.PublicKey;

public class PKIXCertPathValidatorResult implements CertPathValidatorResult {
    private PolicyNode policyTree;
    private PublicKey subjectPublicKey;
    private TrustAnchor trustAnchor;

    public PKIXCertPathValidatorResult(TrustAnchor trustAnchor, PolicyNode policyTree, PublicKey subjectPublicKey) {
        if (subjectPublicKey == null) {
            throw new NullPointerException("subjectPublicKey must be non-null");
        } else if (trustAnchor != null) {
            this.trustAnchor = trustAnchor;
            this.policyTree = policyTree;
            this.subjectPublicKey = subjectPublicKey;
        } else {
            throw new NullPointerException("trustAnchor must be non-null");
        }
    }

    public TrustAnchor getTrustAnchor() {
        return this.trustAnchor;
    }

    public PolicyNode getPolicyTree() {
        return this.policyTree;
    }

    public PublicKey getPublicKey() {
        return this.subjectPublicKey;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("PKIXCertPathValidatorResult: [\n");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  Trust Anchor: ");
        stringBuilder.append(this.trustAnchor.toString());
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  Policy Tree: ");
        stringBuilder.append(String.valueOf(this.policyTree));
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  Subject Public Key: ");
        stringBuilder.append(this.subjectPublicKey);
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        sb.append("]");
        return sb.toString();
    }
}
