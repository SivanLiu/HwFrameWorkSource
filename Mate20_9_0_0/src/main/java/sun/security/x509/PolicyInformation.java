package sun.security.x509;

import java.io.IOException;
import java.security.cert.PolicyQualifierInfo;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

public class PolicyInformation {
    public static final String ID = "id";
    public static final String NAME = "PolicyInformation";
    public static final String QUALIFIERS = "qualifiers";
    private CertificatePolicyId policyIdentifier;
    private Set<PolicyQualifierInfo> policyQualifiers;

    public PolicyInformation(CertificatePolicyId policyIdentifier, Set<PolicyQualifierInfo> policyQualifiers) throws IOException {
        if (policyQualifiers != null) {
            this.policyQualifiers = new LinkedHashSet((Collection) policyQualifiers);
            this.policyIdentifier = policyIdentifier;
            return;
        }
        throw new NullPointerException("policyQualifiers is null");
    }

    public PolicyInformation(DerValue val) throws IOException {
        if (val.tag == (byte) 48) {
            this.policyIdentifier = new CertificatePolicyId(val.data.getDerValue());
            if (val.data.available() != 0) {
                this.policyQualifiers = new LinkedHashSet();
                DerValue opt = val.data.getDerValue();
                if (opt.tag != (byte) 48) {
                    throw new IOException("Invalid encoding of PolicyInformation");
                } else if (opt.data.available() != 0) {
                    while (opt.data.available() != 0) {
                        this.policyQualifiers.add(new PolicyQualifierInfo(opt.data.getDerValue().toByteArray()));
                    }
                    return;
                } else {
                    throw new IOException("No data available in policyQualifiers");
                }
            }
            this.policyQualifiers = Collections.emptySet();
            return;
        }
        throw new IOException("Invalid encoding of PolicyInformation");
    }

    public boolean equals(Object other) {
        if (!(other instanceof PolicyInformation)) {
            return false;
        }
        PolicyInformation piOther = (PolicyInformation) other;
        if (this.policyIdentifier.equals(piOther.getPolicyIdentifier())) {
            return this.policyQualifiers.equals(piOther.getPolicyQualifiers());
        }
        return false;
    }

    public int hashCode() {
        return (37 * (this.policyIdentifier.hashCode() + 37)) + this.policyQualifiers.hashCode();
    }

    public CertificatePolicyId getPolicyIdentifier() {
        return this.policyIdentifier;
    }

    public Set<PolicyQualifierInfo> getPolicyQualifiers() {
        return this.policyQualifiers;
    }

    public Object get(String name) throws IOException {
        if (name.equalsIgnoreCase(ID)) {
            return this.policyIdentifier;
        }
        if (name.equalsIgnoreCase(QUALIFIERS)) {
            return this.policyQualifiers;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Attribute name [");
        stringBuilder.append(name);
        stringBuilder.append("] not recognized by PolicyInformation.");
        throw new IOException(stringBuilder.toString());
    }

    public void set(String name, Object obj) throws IOException {
        if (name.equalsIgnoreCase(ID)) {
            if (obj instanceof CertificatePolicyId) {
                this.policyIdentifier = (CertificatePolicyId) obj;
                return;
            }
            throw new IOException("Attribute value must be instance of CertificatePolicyId.");
        } else if (!name.equalsIgnoreCase(QUALIFIERS)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attribute name [");
            stringBuilder.append(name);
            stringBuilder.append("] not recognized by PolicyInformation");
            throw new IOException(stringBuilder.toString());
        } else if (this.policyIdentifier == null) {
            throw new IOException("Attribute must have a CertificatePolicyIdentifier value before PolicyQualifierInfo can be set.");
        } else if (obj instanceof Set) {
            for (Object obj1 : (Set) obj) {
                if (!(obj1 instanceof PolicyQualifierInfo)) {
                    throw new IOException("Attribute value must be aSet of PolicyQualifierInfo objects.");
                }
            }
            this.policyQualifiers = (Set) obj;
        } else {
            throw new IOException("Attribute value must be of type Set.");
        }
    }

    public void delete(String name) throws IOException {
        if (name.equalsIgnoreCase(QUALIFIERS)) {
            this.policyQualifiers = Collections.emptySet();
        } else if (name.equalsIgnoreCase(ID)) {
            throw new IOException("Attribute ID may not be deleted from PolicyInformation.");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Attribute name [");
            stringBuilder.append(name);
            stringBuilder.append("] not recognized by PolicyInformation.");
            throw new IOException(stringBuilder.toString());
        }
    }

    public Enumeration<String> getElements() {
        AttributeNameEnumeration elements = new AttributeNameEnumeration();
        elements.addElement(ID);
        elements.addElement(QUALIFIERS);
        return elements.elements();
    }

    public String getName() {
        return NAME;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  [");
        stringBuilder.append(this.policyIdentifier.toString());
        StringBuilder s = new StringBuilder(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(this.policyQualifiers);
        stringBuilder.append("  ]\n");
        s.append(stringBuilder.toString());
        return s.toString();
    }

    public void encode(DerOutputStream out) throws IOException {
        DerOutputStream tmp = new DerOutputStream();
        this.policyIdentifier.encode(tmp);
        if (!this.policyQualifiers.isEmpty()) {
            DerOutputStream tmp2 = new DerOutputStream();
            for (PolicyQualifierInfo pq : this.policyQualifiers) {
                tmp2.write(pq.getEncoded());
            }
            tmp.write((byte) 48, tmp2);
        }
        out.write((byte) 48, tmp);
    }
}
