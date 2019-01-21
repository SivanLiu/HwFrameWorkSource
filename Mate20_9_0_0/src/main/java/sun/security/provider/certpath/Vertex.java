package sun.security.provider.certpath;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import sun.security.util.Debug;
import sun.security.x509.AuthorityKeyIdentifierExtension;
import sun.security.x509.KeyIdentifier;
import sun.security.x509.SubjectKeyIdentifierExtension;
import sun.security.x509.X509CertImpl;

public class Vertex {
    private static final Debug debug = Debug.getInstance("certpath");
    private X509Certificate cert;
    private int index = -1;
    private Throwable throwable;

    Vertex(X509Certificate cert) {
        this.cert = cert;
    }

    public X509Certificate getCertificate() {
        return this.cert;
    }

    public int getIndex() {
        return this.index;
    }

    void setIndex(int ndx) {
        this.index = ndx;
    }

    public Throwable getThrowable() {
        return this.throwable;
    }

    void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(certToString());
        stringBuilder.append(throwableToString());
        stringBuilder.append(indexToString());
        return stringBuilder.toString();
    }

    public String certToString() {
        StringBuilder sb = new StringBuilder();
        try {
            int length;
            X509CertImpl x509Cert = X509CertImpl.toImpl(this.cert);
            sb.append("Issuer:     ");
            sb.append(x509Cert.getIssuerX500Principal());
            sb.append("\n");
            sb.append("Subject:    ");
            sb.append(x509Cert.getSubjectX500Principal());
            sb.append("\n");
            sb.append("SerialNum:  ");
            sb.append(x509Cert.getSerialNumber().toString(16));
            sb.append("\n");
            sb.append("Expires:    ");
            sb.append(x509Cert.getNotAfter().toString());
            sb.append("\n");
            boolean[] iUID = x509Cert.getIssuerUniqueID();
            int i = 0;
            if (iUID != null) {
                sb.append("IssuerUID:  ");
                for (int b : iUID) {
                    sb.append(b);
                }
                sb.append("\n");
            }
            boolean[] sUID = x509Cert.getSubjectUniqueID();
            if (sUID != null) {
                sb.append("SubjectUID: ");
                length = sUID.length;
                while (i < length) {
                    sb.append(sUID[i]);
                    i++;
                }
                sb.append("\n");
            }
            try {
                SubjectKeyIdentifierExtension sKeyID = x509Cert.getSubjectKeyIdentifierExtension();
                if (sKeyID != null) {
                    KeyIdentifier keyID = sKeyID.get("key_id");
                    sb.append("SubjKeyID:  ");
                    sb.append(keyID.toString());
                }
                AuthorityKeyIdentifierExtension aKeyID = x509Cert.getAuthorityKeyIdentifierExtension();
                if (aKeyID != null) {
                    KeyIdentifier keyID2 = (KeyIdentifier) aKeyID.get("key_id");
                    sb.append("AuthKeyID:  ");
                    sb.append(keyID2.toString());
                }
            } catch (IOException e) {
                if (debug != null) {
                    debug.println("Vertex.certToString() unexpected exception");
                    e.printStackTrace();
                }
            }
            return sb.toString();
        } catch (CertificateException ce) {
            if (debug != null) {
                debug.println("Vertex.certToString() unexpected exception");
                ce.printStackTrace();
            }
            return sb.toString();
        }
    }

    public String throwableToString() {
        StringBuilder sb = new StringBuilder("Exception:  ");
        if (this.throwable != null) {
            sb.append(this.throwable.toString());
        } else {
            sb.append("null");
        }
        sb.append("\n");
        return sb.toString();
    }

    public String moreToString() {
        StringBuilder sb = new StringBuilder("Last cert?  ");
        sb.append(this.index == -1 ? "Yes" : "No");
        sb.append("\n");
        return sb.toString();
    }

    public String indexToString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Index:      ");
        stringBuilder.append(this.index);
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
