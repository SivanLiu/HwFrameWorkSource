package java.security;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

@Deprecated
public abstract class Identity implements Principal, Serializable {
    private static final long serialVersionUID = 3609922007826600659L;
    Vector<Certificate> certificates;
    String info;
    private String name;
    private PublicKey publicKey;
    IdentityScope scope;

    protected Identity() {
        this("restoring...");
    }

    public Identity(String name, IdentityScope scope) throws KeyManagementException {
        this(name);
        if (scope != null) {
            scope.addIdentity(this);
        }
        this.scope = scope;
    }

    public Identity(String name) {
        this.info = "No further information available.";
        this.name = name;
    }

    public final String getName() {
        return this.name;
    }

    public final IdentityScope getScope() {
        return this.scope;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(PublicKey key) throws KeyManagementException {
        check("setIdentityPublicKey");
        this.publicKey = key;
        this.certificates = new Vector();
    }

    public void setInfo(String info) {
        check("setIdentityInfo");
        this.info = info;
    }

    public String getInfo() {
        return this.info;
    }

    public void addCertificate(Certificate certificate) throws KeyManagementException {
        check("addIdentityCertificate");
        if (this.certificates == null) {
            this.certificates = new Vector();
        }
        if (this.publicKey == null) {
            this.publicKey = certificate.getPublicKey();
        } else if (!keyEquals(this.publicKey, certificate.getPublicKey())) {
            throw new KeyManagementException("public key different from cert public key");
        }
        this.certificates.addElement(certificate);
    }

    private boolean keyEquals(PublicKey aKey, PublicKey anotherKey) {
        String aKeyFormat = aKey.getFormat();
        String anotherKeyFormat = anotherKey.getFormat();
        int i = 1;
        int i2 = aKeyFormat == null ? 1 : 0;
        if (anotherKeyFormat != null) {
            i = 0;
        }
        if ((i ^ i2) != 0) {
            return false;
        }
        if (aKeyFormat == null || anotherKeyFormat == null || aKeyFormat.equalsIgnoreCase(anotherKeyFormat)) {
            return Arrays.equals(aKey.getEncoded(), anotherKey.getEncoded());
        }
        return false;
    }

    public void removeCertificate(Certificate certificate) throws KeyManagementException {
        check("removeIdentityCertificate");
        if (this.certificates == null) {
            return;
        }
        if (certificate == null || !this.certificates.contains(certificate)) {
            throw new KeyManagementException();
        }
        this.certificates.removeElement(certificate);
    }

    public Certificate[] certificates() {
        if (this.certificates == null) {
            return new Certificate[0];
        }
        Certificate[] certs = new Certificate[this.certificates.size()];
        this.certificates.copyInto(certs);
        return certs;
    }

    public final boolean equals(Object identity) {
        if (identity == this) {
            return true;
        }
        if (!(identity instanceof Identity)) {
            return false;
        }
        Identity i = (Identity) identity;
        if (fullName().equals(i.fullName())) {
            return true;
        }
        return identityEquals(i);
    }

    protected boolean identityEquals(Identity identity) {
        if (!this.name.equalsIgnoreCase(identity.name)) {
            return false;
        }
        if (((this.publicKey == null ? 1 : 0) ^ (identity.publicKey == null ? 1 : 0)) != 0) {
            return false;
        }
        if (this.publicKey == null || identity.publicKey == null || this.publicKey.equals(identity.publicKey)) {
            return true;
        }
        return false;
    }

    String fullName() {
        String parsable = this.name;
        if (this.scope == null) {
            return parsable;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(parsable);
        stringBuilder.append(".");
        stringBuilder.append(this.scope.getName());
        return stringBuilder.toString();
    }

    public String toString() {
        check("printIdentity");
        String printable = this.name;
        if (this.scope == null) {
            return printable;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(printable);
        stringBuilder.append("[");
        stringBuilder.append(this.scope.getName());
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public String toString(boolean detailed) {
        String out = toString();
        if (!detailed) {
            return out;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(out);
        stringBuilder.append("\n");
        out = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(out);
        stringBuilder.append(printKeys());
        out = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(out);
        stringBuilder.append("\n");
        stringBuilder.append(printCertificates());
        out = stringBuilder.toString();
        if (this.info != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(out);
            stringBuilder.append("\n\t");
            stringBuilder.append(this.info);
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(out);
        stringBuilder.append("\n\tno additional information available.");
        return stringBuilder.toString();
    }

    String printKeys() {
        String key = "";
        if (this.publicKey != null) {
            return "\tpublic key initialized";
        }
        return "\tno public key";
    }

    String printCertificates() {
        String out = "";
        if (this.certificates == null) {
            return "\tno certificates";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(out);
        stringBuilder.append("\tcertificates: \n");
        out = stringBuilder.toString();
        int i = 1;
        Iterator it = this.certificates.iterator();
        while (it.hasNext()) {
            Certificate cert = (Certificate) it.next();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(out);
            stringBuilder2.append("\tcertificate ");
            int i2 = i + 1;
            stringBuilder2.append(i);
            stringBuilder2.append("\tfor  : ");
            stringBuilder2.append(cert.getPrincipal());
            stringBuilder2.append("\n");
            out = stringBuilder2.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(out);
            stringBuilder.append("\t\t\tfrom : ");
            stringBuilder.append(cert.getGuarantor());
            stringBuilder.append("\n");
            out = stringBuilder.toString();
            i = i2;
        }
        return out;
    }

    public int hashCode() {
        return this.name.hashCode();
    }

    private static void check(String directive) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkSecurityAccess(directive);
        }
    }
}
