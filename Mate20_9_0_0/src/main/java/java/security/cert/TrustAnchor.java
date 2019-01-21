package java.security.cert;

import java.io.IOException;
import java.security.PublicKey;
import javax.security.auth.x500.X500Principal;
import sun.security.x509.NameConstraintsExtension;

public class TrustAnchor {
    private final String caName;
    private final X500Principal caPrincipal;
    private NameConstraintsExtension nc;
    private byte[] ncBytes;
    private final PublicKey pubKey;
    private final X509Certificate trustedCert;

    public TrustAnchor(X509Certificate trustedCert, byte[] nameConstraints) {
        if (trustedCert != null) {
            this.trustedCert = trustedCert;
            this.pubKey = null;
            this.caName = null;
            this.caPrincipal = null;
            setNameConstraints(nameConstraints);
            return;
        }
        throw new NullPointerException("the trustedCert parameter must be non-null");
    }

    public TrustAnchor(X500Principal caPrincipal, PublicKey pubKey, byte[] nameConstraints) {
        if (caPrincipal == null || pubKey == null) {
            throw new NullPointerException();
        }
        this.trustedCert = null;
        this.caPrincipal = caPrincipal;
        this.caName = caPrincipal.getName();
        this.pubKey = pubKey;
        setNameConstraints(nameConstraints);
    }

    public TrustAnchor(String caName, PublicKey pubKey, byte[] nameConstraints) {
        if (pubKey == null) {
            throw new NullPointerException("the pubKey parameter must be non-null");
        } else if (caName == null) {
            throw new NullPointerException("the caName parameter must be non-null");
        } else if (caName.length() != 0) {
            this.caPrincipal = new X500Principal(caName);
            this.pubKey = pubKey;
            this.caName = caName;
            this.trustedCert = null;
            setNameConstraints(nameConstraints);
        } else {
            throw new IllegalArgumentException("the caName parameter must be a non-empty String");
        }
    }

    public final X509Certificate getTrustedCert() {
        return this.trustedCert;
    }

    public final X500Principal getCA() {
        return this.caPrincipal;
    }

    public final String getCAName() {
        return this.caName;
    }

    public final PublicKey getCAPublicKey() {
        return this.pubKey;
    }

    private void setNameConstraints(byte[] bytes) {
        if (bytes == null) {
            this.ncBytes = null;
            this.nc = null;
            return;
        }
        this.ncBytes = (byte[]) bytes.clone();
        try {
            this.nc = new NameConstraintsExtension(Boolean.FALSE, (Object) bytes);
        } catch (IOException ioe) {
            IllegalArgumentException iae = new IllegalArgumentException(ioe.getMessage());
            iae.initCause(ioe);
            throw iae;
        }
    }

    public final byte[] getNameConstraints() {
        return this.ncBytes == null ? null : (byte[]) this.ncBytes.clone();
    }

    public String toString() {
        StringBuilder stringBuilder;
        StringBuffer sb = new StringBuffer();
        sb.append("[\n");
        if (this.pubKey != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Trusted CA Public Key: ");
            stringBuilder.append(this.pubKey.toString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Trusted CA Issuer Name: ");
            stringBuilder.append(String.valueOf(this.caName));
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Trusted CA cert: ");
            stringBuilder.append(this.trustedCert.toString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.nc != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Name Constraints: ");
            stringBuilder.append(this.nc.toString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        return sb.toString();
    }
}
