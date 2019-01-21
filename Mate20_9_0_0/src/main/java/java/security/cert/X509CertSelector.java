package java.security.cert;

import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.security.auth.x500.X500Principal;
import sun.misc.HexDumpEncoder;
import sun.security.util.Debug;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificatePoliciesExtension;
import sun.security.x509.CertificatePolicyId;
import sun.security.x509.CertificatePolicySet;
import sun.security.x509.DNSName;
import sun.security.x509.EDIPartyName;
import sun.security.x509.ExtendedKeyUsageExtension;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralNames;
import sun.security.x509.GeneralSubtree;
import sun.security.x509.GeneralSubtrees;
import sun.security.x509.IPAddressName;
import sun.security.x509.NameConstraintsExtension;
import sun.security.x509.OIDName;
import sun.security.x509.OtherName;
import sun.security.x509.PolicyInformation;
import sun.security.x509.PrivateKeyUsageExtension;
import sun.security.x509.RFC822Name;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.URIName;
import sun.security.x509.X400Address;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509Key;

public class X509CertSelector implements CertSelector {
    private static final ObjectIdentifier ANY_EXTENDED_KEY_USAGE = ObjectIdentifier.newInternal(new int[]{2, 5, 29, 37, 0});
    private static final int CERT_POLICIES_ID = 3;
    private static final int EXTENDED_KEY_USAGE_ID = 4;
    private static final String[] EXTENSION_OIDS = new String[5];
    private static final Boolean FALSE = Boolean.FALSE;
    static final int NAME_ANY = 0;
    private static final int NAME_CONSTRAINTS_ID = 2;
    static final int NAME_DIRECTORY = 4;
    static final int NAME_DNS = 2;
    static final int NAME_EDI = 5;
    static final int NAME_IP = 7;
    static final int NAME_OID = 8;
    static final int NAME_RFC822 = 1;
    static final int NAME_URI = 6;
    static final int NAME_X400 = 3;
    private static final int NUM_OF_EXTENSIONS = 5;
    private static final int PRIVATE_KEY_USAGE_ID = 0;
    private static final int SUBJECT_ALT_NAME_ID = 1;
    private static final Debug debug = Debug.getInstance("certpath");
    private byte[] authorityKeyID;
    private int basicConstraints = -1;
    private Date certificateValid;
    private X500Principal issuer;
    private Set<ObjectIdentifier> keyPurposeOIDSet;
    private Set<String> keyPurposeSet;
    private boolean[] keyUsage;
    private boolean matchAllSubjectAltNames = true;
    private NameConstraintsExtension nc;
    private byte[] ncBytes;
    private Set<GeneralNameInterface> pathToGeneralNames;
    private Set<List<?>> pathToNames;
    private CertificatePolicySet policy;
    private Set<String> policySet;
    private Date privateKeyValid;
    private BigInteger serialNumber;
    private X500Principal subject;
    private Set<GeneralNameInterface> subjectAlternativeGeneralNames;
    private Set<List<?>> subjectAlternativeNames;
    private byte[] subjectKeyID;
    private PublicKey subjectPublicKey;
    private ObjectIdentifier subjectPublicKeyAlgID;
    private byte[] subjectPublicKeyBytes;
    private X509Certificate x509Cert;

    static {
        CertPathHelperImpl.initialize();
        EXTENSION_OIDS[0] = "2.5.29.16";
        EXTENSION_OIDS[1] = "2.5.29.17";
        EXTENSION_OIDS[2] = "2.5.29.30";
        EXTENSION_OIDS[3] = "2.5.29.32";
        EXTENSION_OIDS[4] = "2.5.29.37";
    }

    public void setCertificate(X509Certificate cert) {
        this.x509Cert = cert;
    }

    public void setSerialNumber(BigInteger serial) {
        this.serialNumber = serial;
    }

    public void setIssuer(X500Principal issuer) {
        this.issuer = issuer;
    }

    public void setIssuer(String issuerDN) throws IOException {
        if (issuerDN == null) {
            this.issuer = null;
        } else {
            this.issuer = new X500Name(issuerDN).asX500Principal();
        }
    }

    public void setIssuer(byte[] issuerDN) throws IOException {
        X500Principal x500Principal;
        if (issuerDN == null) {
            x500Principal = null;
        } else {
            try {
                x500Principal = new X500Principal(issuerDN);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid name", e);
            }
        }
        this.issuer = x500Principal;
    }

    public void setSubject(X500Principal subject) {
        this.subject = subject;
    }

    public void setSubject(String subjectDN) throws IOException {
        if (subjectDN == null) {
            this.subject = null;
        } else {
            this.subject = new X500Name(subjectDN).asX500Principal();
        }
    }

    public void setSubject(byte[] subjectDN) throws IOException {
        X500Principal x500Principal;
        if (subjectDN == null) {
            x500Principal = null;
        } else {
            try {
                x500Principal = new X500Principal(subjectDN);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid name", e);
            }
        }
        this.subject = x500Principal;
    }

    public void setSubjectKeyIdentifier(byte[] subjectKeyID) {
        if (subjectKeyID == null) {
            this.subjectKeyID = null;
        } else {
            this.subjectKeyID = (byte[]) subjectKeyID.clone();
        }
    }

    public void setAuthorityKeyIdentifier(byte[] authorityKeyID) {
        if (authorityKeyID == null) {
            this.authorityKeyID = null;
        } else {
            this.authorityKeyID = (byte[]) authorityKeyID.clone();
        }
    }

    public void setCertificateValid(Date certValid) {
        if (certValid == null) {
            this.certificateValid = null;
        } else {
            this.certificateValid = (Date) certValid.clone();
        }
    }

    public void setPrivateKeyValid(Date privateKeyValid) {
        if (privateKeyValid == null) {
            this.privateKeyValid = null;
        } else {
            this.privateKeyValid = (Date) privateKeyValid.clone();
        }
    }

    public void setSubjectPublicKeyAlgID(String oid) throws IOException {
        if (oid == null) {
            this.subjectPublicKeyAlgID = null;
        } else {
            this.subjectPublicKeyAlgID = new ObjectIdentifier(oid);
        }
    }

    public void setSubjectPublicKey(PublicKey key) {
        if (key == null) {
            this.subjectPublicKey = null;
            this.subjectPublicKeyBytes = null;
            return;
        }
        this.subjectPublicKey = key;
        this.subjectPublicKeyBytes = key.getEncoded();
    }

    public void setSubjectPublicKey(byte[] key) throws IOException {
        if (key == null) {
            this.subjectPublicKey = null;
            this.subjectPublicKeyBytes = null;
            return;
        }
        this.subjectPublicKeyBytes = (byte[]) key.clone();
        this.subjectPublicKey = X509Key.parse(new DerValue(this.subjectPublicKeyBytes));
    }

    public void setKeyUsage(boolean[] keyUsage) {
        if (keyUsage == null) {
            this.keyUsage = null;
        } else {
            this.keyUsage = (boolean[]) keyUsage.clone();
        }
    }

    public void setExtendedKeyUsage(Set<String> keyPurposeSet) throws IOException {
        if (keyPurposeSet == null || keyPurposeSet.isEmpty()) {
            this.keyPurposeSet = null;
            this.keyPurposeOIDSet = null;
            return;
        }
        this.keyPurposeSet = Collections.unmodifiableSet(new HashSet((Collection) keyPurposeSet));
        this.keyPurposeOIDSet = new HashSet();
        for (String s : this.keyPurposeSet) {
            this.keyPurposeOIDSet.add(new ObjectIdentifier(s));
        }
    }

    public void setMatchAllSubjectAltNames(boolean matchAllNames) {
        this.matchAllSubjectAltNames = matchAllNames;
    }

    public void setSubjectAlternativeNames(Collection<List<?>> names) throws IOException {
        if (names == null) {
            this.subjectAlternativeNames = null;
            this.subjectAlternativeGeneralNames = null;
        } else if (names.isEmpty()) {
            this.subjectAlternativeNames = null;
            this.subjectAlternativeGeneralNames = null;
        } else {
            Set<List<?>> tempNames = cloneAndCheckNames(names);
            this.subjectAlternativeGeneralNames = parseNames(tempNames);
            this.subjectAlternativeNames = tempNames;
        }
    }

    public void addSubjectAlternativeName(int type, String name) throws IOException {
        addSubjectAlternativeNameInternal(type, name);
    }

    public void addSubjectAlternativeName(int type, byte[] name) throws IOException {
        addSubjectAlternativeNameInternal(type, name.clone());
    }

    private void addSubjectAlternativeNameInternal(int type, Object name) throws IOException {
        GeneralNameInterface tempName = makeGeneralNameInterface(type, name);
        if (this.subjectAlternativeNames == null) {
            this.subjectAlternativeNames = new HashSet();
        }
        if (this.subjectAlternativeGeneralNames == null) {
            this.subjectAlternativeGeneralNames = new HashSet();
        }
        List<Object> list = new ArrayList(2);
        list.add(Integer.valueOf(type));
        list.add(name);
        this.subjectAlternativeNames.add(list);
        this.subjectAlternativeGeneralNames.add(tempName);
    }

    private static Set<GeneralNameInterface> parseNames(Collection<List<?>> names) throws IOException {
        Set<GeneralNameInterface> genNames = new HashSet();
        for (List<?> nameList : names) {
            if (nameList.size() == 2) {
                Object o = nameList.get(null);
                if (o instanceof Integer) {
                    genNames.add(makeGeneralNameInterface(((Integer) o).intValue(), nameList.get(1)));
                } else {
                    throw new IOException("expected an Integer");
                }
            }
            throw new IOException("name list size not 2");
        }
        return genNames;
    }

    static boolean equalNames(Collection<?> object1, Collection<?> object2) {
        if (object1 != null && object2 != null) {
            return object1.equals(object2);
        }
        return object1 == object2;
    }

    static GeneralNameInterface makeGeneralNameInterface(int type, Object name) throws IOException {
        Debug debug;
        StringBuilder stringBuilder;
        if (debug != null) {
            debug = debug;
            stringBuilder = new StringBuilder();
            stringBuilder.append("X509CertSelector.makeGeneralNameInterface(");
            stringBuilder.append(type);
            stringBuilder.append(")...");
            debug.println(stringBuilder.toString());
        }
        StringBuilder stringBuilder2;
        if (name instanceof String) {
            GeneralNameInterface result;
            if (debug != null) {
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("X509CertSelector.makeGeneralNameInterface() name is String: ");
                stringBuilder.append(name);
                debug.println(stringBuilder.toString());
            }
            switch (type) {
                case 1:
                    result = new RFC822Name((String) name);
                    break;
                case 2:
                    result = new DNSName((String) name);
                    break;
                case 4:
                    result = new X500Name((String) name);
                    break;
                case 6:
                    result = new URIName((String) name);
                    break;
                case 7:
                    result = new IPAddressName((String) name);
                    break;
                case 8:
                    result = new OIDName((String) name);
                    break;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("unable to parse String names of type ");
                    stringBuilder.append(type);
                    throw new IOException(stringBuilder.toString());
            }
            if (debug == null) {
                return result;
            }
            Debug debug2 = debug;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("X509CertSelector.makeGeneralNameInterface() result: ");
            stringBuilder2.append(result.toString());
            debug2.println(stringBuilder2.toString());
            return result;
        } else if (name instanceof byte[]) {
            GeneralNameInterface result2;
            DerValue val = new DerValue((byte[]) name);
            if (debug != null) {
                debug.println("X509CertSelector.makeGeneralNameInterface() is byte[]");
            }
            switch (type) {
                case 0:
                    result2 = new OtherName(val);
                    break;
                case 1:
                    result2 = new RFC822Name(val);
                    break;
                case 2:
                    result2 = new DNSName(val);
                    break;
                case 3:
                    result2 = new X400Address(val);
                    break;
                case 4:
                    result2 = new X500Name(val);
                    break;
                case 5:
                    result2 = new EDIPartyName(val);
                    break;
                case 6:
                    result2 = new URIName(val);
                    break;
                case 7:
                    result2 = new IPAddressName(val);
                    break;
                case 8:
                    result2 = new OIDName(val);
                    break;
                default:
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("unable to parse byte array names of type ");
                    stringBuilder2.append(type);
                    throw new IOException(stringBuilder2.toString());
            }
            if (debug != null) {
                Debug debug3 = debug;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("X509CertSelector.makeGeneralNameInterface() result: ");
                stringBuilder3.append(result2.toString());
                debug3.println(stringBuilder3.toString());
            }
            return result2;
        } else {
            if (debug != null) {
                debug.println("X509CertSelector.makeGeneralName() input name not String or byte array");
            }
            throw new IOException("name not String or byte array");
        }
    }

    public void setNameConstraints(byte[] bytes) throws IOException {
        if (bytes == null) {
            this.ncBytes = null;
            this.nc = null;
            return;
        }
        this.ncBytes = (byte[]) bytes.clone();
        this.nc = new NameConstraintsExtension(FALSE, (Object) bytes);
    }

    public void setBasicConstraints(int minMaxPathLen) {
        if (minMaxPathLen >= -2) {
            this.basicConstraints = minMaxPathLen;
            return;
        }
        throw new IllegalArgumentException("basic constraints less than -2");
    }

    public void setPolicy(Set<String> certPolicySet) throws IOException {
        if (certPolicySet == null) {
            this.policySet = null;
            this.policy = null;
            return;
        }
        Set<String> tempSet = Collections.unmodifiableSet(new HashSet((Collection) certPolicySet));
        Vector polIdVector = new Vector();
        for (Object o : tempSet) {
            if (o instanceof String) {
                polIdVector.add(new CertificatePolicyId(new ObjectIdentifier((String) o)));
            } else {
                throw new IOException("non String in certPolicySet");
            }
        }
        this.policySet = tempSet;
        this.policy = new CertificatePolicySet(polIdVector);
    }

    public void setPathToNames(Collection<List<?>> names) throws IOException {
        if (names == null || names.isEmpty()) {
            this.pathToNames = null;
            this.pathToGeneralNames = null;
            return;
        }
        Set<List<?>> tempNames = cloneAndCheckNames(names);
        this.pathToGeneralNames = parseNames(tempNames);
        this.pathToNames = tempNames;
    }

    void setPathToNamesInternal(Set<GeneralNameInterface> names) {
        this.pathToNames = Collections.emptySet();
        this.pathToGeneralNames = names;
    }

    public void addPathToName(int type, String name) throws IOException {
        addPathToNameInternal(type, name);
    }

    public void addPathToName(int type, byte[] name) throws IOException {
        addPathToNameInternal(type, name.clone());
    }

    private void addPathToNameInternal(int type, Object name) throws IOException {
        GeneralNameInterface tempName = makeGeneralNameInterface(type, name);
        if (this.pathToGeneralNames == null) {
            this.pathToNames = new HashSet();
            this.pathToGeneralNames = new HashSet();
        }
        List<Object> list = new ArrayList(2);
        list.add(Integer.valueOf(type));
        list.add(name);
        this.pathToNames.add(list);
        this.pathToGeneralNames.add(tempName);
    }

    public X509Certificate getCertificate() {
        return this.x509Cert;
    }

    public BigInteger getSerialNumber() {
        return this.serialNumber;
    }

    public X500Principal getIssuer() {
        return this.issuer;
    }

    public String getIssuerAsString() {
        return this.issuer == null ? null : this.issuer.getName();
    }

    public byte[] getIssuerAsBytes() throws IOException {
        return this.issuer == null ? null : this.issuer.getEncoded();
    }

    public X500Principal getSubject() {
        return this.subject;
    }

    public String getSubjectAsString() {
        return this.subject == null ? null : this.subject.getName();
    }

    public byte[] getSubjectAsBytes() throws IOException {
        return this.subject == null ? null : this.subject.getEncoded();
    }

    public byte[] getSubjectKeyIdentifier() {
        if (this.subjectKeyID == null) {
            return null;
        }
        return (byte[]) this.subjectKeyID.clone();
    }

    public byte[] getAuthorityKeyIdentifier() {
        if (this.authorityKeyID == null) {
            return null;
        }
        return (byte[]) this.authorityKeyID.clone();
    }

    public Date getCertificateValid() {
        if (this.certificateValid == null) {
            return null;
        }
        return (Date) this.certificateValid.clone();
    }

    public Date getPrivateKeyValid() {
        if (this.privateKeyValid == null) {
            return null;
        }
        return (Date) this.privateKeyValid.clone();
    }

    public String getSubjectPublicKeyAlgID() {
        if (this.subjectPublicKeyAlgID == null) {
            return null;
        }
        return this.subjectPublicKeyAlgID.toString();
    }

    public PublicKey getSubjectPublicKey() {
        return this.subjectPublicKey;
    }

    public boolean[] getKeyUsage() {
        if (this.keyUsage == null) {
            return null;
        }
        return (boolean[]) this.keyUsage.clone();
    }

    public Set<String> getExtendedKeyUsage() {
        return this.keyPurposeSet;
    }

    public boolean getMatchAllSubjectAltNames() {
        return this.matchAllSubjectAltNames;
    }

    public Collection<List<?>> getSubjectAlternativeNames() {
        if (this.subjectAlternativeNames == null) {
            return null;
        }
        return cloneNames(this.subjectAlternativeNames);
    }

    private static Set<List<?>> cloneNames(Collection<List<?>> names) {
        try {
            return cloneAndCheckNames(names);
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cloneNames encountered IOException: ");
            stringBuilder.append(e.getMessage());
            throw new RuntimeException(stringBuilder.toString());
        }
    }

    private static Set<List<?>> cloneAndCheckNames(Collection<List<?>> names) throws IOException {
        Set<List<?>> namesCopy = new HashSet();
        for (Collection o : names) {
            namesCopy.add(new ArrayList(o));
        }
        for (List<Object> nameList : namesCopy) {
            if (nameList.size() == 2) {
                Object o2 = nameList.get(null);
                if (o2 instanceof Integer) {
                    int nameType = ((Integer) o2).intValue();
                    if (nameType < 0 || nameType > 8) {
                        throw new IOException("name type not 0-8");
                    }
                    Object nameObject = nameList.get(1);
                    if (!(nameObject instanceof byte[]) && !(nameObject instanceof String)) {
                        if (debug != null) {
                            debug.println("X509CertSelector.cloneAndCheckNames() name not byte array");
                        }
                        throw new IOException("name not byte array or String");
                    } else if (nameObject instanceof byte[]) {
                        nameList.set(1, ((byte[]) nameObject).clone());
                    }
                } else {
                    throw new IOException("expected an Integer");
                }
            }
            throw new IOException("name list size not 2");
        }
        return namesCopy;
    }

    public byte[] getNameConstraints() {
        if (this.ncBytes == null) {
            return null;
        }
        return (byte[]) this.ncBytes.clone();
    }

    public int getBasicConstraints() {
        return this.basicConstraints;
    }

    public Set<String> getPolicy() {
        return this.policySet;
    }

    public Collection<List<?>> getPathToNames() {
        if (this.pathToNames == null) {
            return null;
        }
        return cloneNames(this.pathToNames);
    }

    public String toString() {
        StringBuilder stringBuilder;
        HexDumpEncoder enc;
        StringBuilder stringBuilder2;
        StringBuffer sb = new StringBuffer();
        sb.append("X509CertSelector: [\n");
        if (this.x509Cert != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Certificate: ");
            stringBuilder.append(this.x509Cert.toString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.serialNumber != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Serial Number: ");
            stringBuilder.append(this.serialNumber.toString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.issuer != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Issuer: ");
            stringBuilder.append(getIssuerAsString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.subject != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Subject: ");
            stringBuilder.append(getSubjectAsString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("  matchAllSubjectAltNames flag: ");
        stringBuilder.append(String.valueOf(this.matchAllSubjectAltNames));
        stringBuilder.append("\n");
        sb.append(stringBuilder.toString());
        if (this.subjectAlternativeNames != null) {
            sb.append("  SubjectAlternativeNames:\n");
            for (List<?> list : this.subjectAlternativeNames) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("    type ");
                stringBuilder3.append(list.get(0));
                stringBuilder3.append(", name ");
                stringBuilder3.append(list.get(1));
                stringBuilder3.append("\n");
                sb.append(stringBuilder3.toString());
            }
        }
        if (this.subjectKeyID != null) {
            enc = new HexDumpEncoder();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  Subject Key Identifier: ");
            stringBuilder2.append(enc.encodeBuffer(this.subjectKeyID));
            stringBuilder2.append("\n");
            sb.append(stringBuilder2.toString());
        }
        if (this.authorityKeyID != null) {
            enc = new HexDumpEncoder();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  Authority Key Identifier: ");
            stringBuilder2.append(enc.encodeBuffer(this.authorityKeyID));
            stringBuilder2.append("\n");
            sb.append(stringBuilder2.toString());
        }
        if (this.certificateValid != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Certificate Valid: ");
            stringBuilder.append(this.certificateValid.toString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.privateKeyValid != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Private Key Valid: ");
            stringBuilder.append(this.privateKeyValid.toString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.subjectPublicKeyAlgID != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Subject Public Key AlgID: ");
            stringBuilder.append(this.subjectPublicKeyAlgID.toString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.subjectPublicKey != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Subject Public Key: ");
            stringBuilder.append(this.subjectPublicKey.toString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.keyUsage != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Key Usage: ");
            stringBuilder.append(keyUsageToString(this.keyUsage));
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.keyPurposeSet != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Extended Key Usage: ");
            stringBuilder.append(this.keyPurposeSet.toString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.policy != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  Policy: ");
            stringBuilder.append(this.policy.toString());
            stringBuilder.append("\n");
            sb.append(stringBuilder.toString());
        }
        if (this.pathToGeneralNames != null) {
            sb.append("  Path to names:\n");
            for (Object append : this.pathToGeneralNames) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("    ");
                stringBuilder2.append(append);
                stringBuilder2.append("\n");
                sb.append(stringBuilder2.toString());
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String keyUsageToString(boolean[] k) {
        StringBuilder stringBuilder;
        String s = "KeyUsage [\n";
        try {
            if (k[0]) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append("  DigitalSignature\n");
                s = stringBuilder.toString();
            }
            if (k[1]) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append("  Non_repudiation\n");
                s = stringBuilder.toString();
            }
            if (k[2]) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append("  Key_Encipherment\n");
                s = stringBuilder.toString();
            }
            if (k[3]) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append("  Data_Encipherment\n");
                s = stringBuilder.toString();
            }
            if (k[4]) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append("  Key_Agreement\n");
                s = stringBuilder.toString();
            }
            if (k[5]) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append("  Key_CertSign\n");
                s = stringBuilder.toString();
            }
            if (k[6]) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append("  Crl_Sign\n");
                s = stringBuilder.toString();
            }
            if (k[7]) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append("  Encipher_Only\n");
                s = stringBuilder.toString();
            }
            if (k[8]) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(s);
                stringBuilder.append("  Decipher_Only\n");
                s = stringBuilder.toString();
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(s);
        stringBuilder.append("]\n");
        return stringBuilder.toString();
    }

    private static Extension getExtensionObject(X509Certificate cert, int extId) throws IOException {
        if (cert instanceof X509CertImpl) {
            X509CertImpl impl = (X509CertImpl) cert;
            switch (extId) {
                case 0:
                    return impl.getPrivateKeyUsageExtension();
                case 1:
                    return impl.getSubjectAlternativeNameExtension();
                case 2:
                    return impl.getNameConstraintsExtension();
                case 3:
                    return impl.getCertificatePoliciesExtension();
                case 4:
                    return impl.getExtendedKeyUsageExtension();
                default:
                    return null;
            }
        }
        byte[] rawExtVal = cert.getExtensionValue(EXTENSION_OIDS[extId]);
        if (rawExtVal == null) {
            return null;
        }
        Object encoded = new DerInputStream(rawExtVal).getOctetString();
        switch (extId) {
            case 0:
                try {
                    return new PrivateKeyUsageExtension(FALSE, encoded);
                } catch (CertificateException ex) {
                    throw new IOException(ex.getMessage());
                }
            case 1:
                return new SubjectAlternativeNameExtension(FALSE, encoded);
            case 2:
                return new NameConstraintsExtension(FALSE, encoded);
            case 3:
                return new CertificatePoliciesExtension(FALSE, encoded);
            case 4:
                return new ExtendedKeyUsageExtension(FALSE, encoded);
            default:
                return null;
        }
    }

    public boolean match(Certificate cert) {
        boolean result = false;
        if (!(cert instanceof X509Certificate)) {
            return false;
        }
        X509Certificate xcert = (X509Certificate) cert;
        if (debug != null) {
            Debug debug = debug;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("X509CertSelector.match(SN: ");
            stringBuilder.append(xcert.getSerialNumber().toString(16));
            stringBuilder.append("\n  Issuer: ");
            stringBuilder.append(xcert.getIssuerDN());
            stringBuilder.append("\n  Subject: ");
            stringBuilder.append(xcert.getSubjectDN());
            stringBuilder.append(")");
            debug.println(stringBuilder.toString());
        }
        if (this.x509Cert != null && !this.x509Cert.equals(xcert)) {
            if (debug != null) {
                debug.println("X509CertSelector.match: certs don't match");
            }
            return false;
        } else if (this.serialNumber != null && !this.serialNumber.equals(xcert.getSerialNumber())) {
            if (debug != null) {
                debug.println("X509CertSelector.match: serial numbers don't match");
            }
            return false;
        } else if (this.issuer != null && !this.issuer.equals(xcert.getIssuerX500Principal())) {
            if (debug != null) {
                debug.println("X509CertSelector.match: issuer DNs don't match");
            }
            return false;
        } else if (this.subject == null || this.subject.equals(xcert.getSubjectX500Principal())) {
            if (this.certificateValid != null) {
                try {
                    xcert.checkValidity(this.certificateValid);
                } catch (CertificateException e) {
                    if (debug != null) {
                        debug.println("X509CertSelector.match: certificate not within validity period");
                    }
                    return false;
                }
            }
            if (this.subjectPublicKeyBytes != null) {
                if (!Arrays.equals(this.subjectPublicKeyBytes, xcert.getPublicKey().getEncoded())) {
                    if (debug != null) {
                        debug.println("X509CertSelector.match: subject public keys don't match");
                    }
                    return false;
                }
            }
            if (matchBasicConstraints(xcert) && matchKeyUsage(xcert) && matchExtendedKeyUsage(xcert) && matchSubjectKeyID(xcert) && matchAuthorityKeyID(xcert) && matchPrivateKeyValid(xcert) && matchSubjectPublicKeyAlgID(xcert) && matchPolicy(xcert) && matchSubjectAlternativeNames(xcert) && matchPathToNames(xcert) && matchNameConstraints(xcert)) {
                result = true;
            }
            if (result && debug != null) {
                debug.println("X509CertSelector.match returning: true");
            }
            return result;
        } else {
            if (debug != null) {
                debug.println("X509CertSelector.match: subject DNs don't match");
            }
            return false;
        }
    }

    private boolean matchSubjectKeyID(X509Certificate xcert) {
        if (this.subjectKeyID == null) {
            return true;
        }
        try {
            byte[] extVal = xcert.getExtensionValue("2.5.29.14");
            if (extVal == null) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: no subject key ID extension");
                }
                return false;
            }
            byte[] certSubjectKeyID = new DerInputStream(extVal).getOctetString();
            if (certSubjectKeyID != null) {
                if (Arrays.equals(this.subjectKeyID, certSubjectKeyID)) {
                    return true;
                }
            }
            if (debug != null) {
                debug.println("X509CertSelector.match: subject key IDs don't match");
            }
            return false;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("X509CertSelector.match: exception in subject key ID check");
            }
            return false;
        }
    }

    private boolean matchAuthorityKeyID(X509Certificate xcert) {
        if (this.authorityKeyID == null) {
            return true;
        }
        try {
            byte[] extVal = xcert.getExtensionValue("2.5.29.35");
            if (extVal == null) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: no authority key ID extension");
                }
                return false;
            }
            byte[] certAuthKeyID = new DerInputStream(extVal).getOctetString();
            if (certAuthKeyID != null) {
                if (Arrays.equals(this.authorityKeyID, certAuthKeyID)) {
                    return true;
                }
            }
            if (debug != null) {
                debug.println("X509CertSelector.match: authority key IDs don't match");
            }
            return false;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("X509CertSelector.match: exception in authority key ID check");
            }
            return false;
        }
    }

    private boolean matchPrivateKeyValid(X509Certificate xcert) {
        String time;
        Debug debug;
        StringBuilder stringBuilder;
        if (this.privateKeyValid == null) {
            return true;
        }
        PrivateKeyUsageExtension ext = null;
        try {
            ext = (PrivateKeyUsageExtension) getExtensionObject(xcert, 0);
            if (ext != null) {
                ext.valid(this.privateKeyValid);
            }
            return true;
        } catch (CertificateExpiredException e1) {
            if (debug != null) {
                time = "n/a";
                try {
                    time = ext.get(PrivateKeyUsageExtension.NOT_AFTER).toString();
                } catch (CertificateException e) {
                }
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("X509CertSelector.match: private key usage not within validity date; ext.NOT_After: ");
                stringBuilder.append(time);
                stringBuilder.append("; X509CertSelector: ");
                stringBuilder.append(toString());
                debug.println(stringBuilder.toString());
                e1.printStackTrace();
            }
            return false;
        } catch (CertificateNotYetValidException e2) {
            if (debug != null) {
                time = "n/a";
                try {
                    time = ext.get(PrivateKeyUsageExtension.NOT_BEFORE).toString();
                } catch (CertificateException e3) {
                }
                debug = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("X509CertSelector.match: private key usage not within validity date; ext.NOT_BEFORE: ");
                stringBuilder.append(time);
                stringBuilder.append("; X509CertSelector: ");
                stringBuilder.append(toString());
                debug.println(stringBuilder.toString());
                e2.printStackTrace();
            }
            return false;
        } catch (IOException e4) {
            if (debug != null) {
                Debug debug2 = debug;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("X509CertSelector.match: IOException in private key usage check; X509CertSelector: ");
                stringBuilder2.append(toString());
                debug2.println(stringBuilder2.toString());
                e4.printStackTrace();
            }
            return false;
        }
    }

    private boolean matchSubjectPublicKeyAlgID(X509Certificate xcert) {
        if (this.subjectPublicKeyAlgID == null) {
            return true;
        }
        try {
            DerValue val = new DerValue(xcert.getPublicKey().getEncoded());
            if (val.tag == (byte) 48) {
                AlgorithmId algID = AlgorithmId.parse(val.data.getDerValue());
                if (debug != null) {
                    Debug debug = debug;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("X509CertSelector.match: subjectPublicKeyAlgID = ");
                    stringBuilder.append(this.subjectPublicKeyAlgID);
                    stringBuilder.append(", xcert subjectPublicKeyAlgID = ");
                    stringBuilder.append(algID.getOID());
                    debug.println(stringBuilder.toString());
                }
                if (this.subjectPublicKeyAlgID.equals(algID.getOID())) {
                    return true;
                }
                if (debug != null) {
                    debug.println("X509CertSelector.match: subject public key alg IDs don't match");
                }
                return false;
            }
            throw new IOException("invalid key format");
        } catch (IOException e) {
            if (debug != null) {
                debug.println("X509CertSelector.match: IOException in subject public key algorithm OID check");
            }
            return false;
        }
    }

    private boolean matchKeyUsage(X509Certificate xcert) {
        if (this.keyUsage == null) {
            return true;
        }
        boolean[] certKeyUsage = xcert.getKeyUsage();
        if (certKeyUsage != null) {
            int keyBit = 0;
            while (keyBit < this.keyUsage.length) {
                if (!this.keyUsage[keyBit] || (keyBit < certKeyUsage.length && certKeyUsage[keyBit])) {
                    keyBit++;
                } else {
                    if (debug != null) {
                        debug.println("X509CertSelector.match: key usage bits don't match");
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean matchExtendedKeyUsage(X509Certificate xcert) {
        if (this.keyPurposeSet == null || this.keyPurposeSet.isEmpty()) {
            return true;
        }
        try {
            ExtendedKeyUsageExtension ext = (ExtendedKeyUsageExtension) getExtensionObject(xcert, 4);
            if (ext != null) {
                Vector<ObjectIdentifier> certKeyPurposeVector = ext.get(ExtendedKeyUsageExtension.USAGES);
                if (!(certKeyPurposeVector.contains(ANY_EXTENDED_KEY_USAGE) || certKeyPurposeVector.containsAll(this.keyPurposeOIDSet))) {
                    if (debug != null) {
                        debug.println("X509CertSelector.match: cert failed extendedKeyUsage criterion");
                    }
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("X509CertSelector.match: IOException in extended key usage check");
            }
            return false;
        }
    }

    private boolean matchSubjectAlternativeNames(X509Certificate xcert) {
        if (this.subjectAlternativeNames == null || this.subjectAlternativeNames.isEmpty()) {
            return true;
        }
        try {
            SubjectAlternativeNameExtension sanExt = (SubjectAlternativeNameExtension) getExtensionObject(xcert, 1);
            if (sanExt == null) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: no subject alternative name extension");
                }
                return false;
            }
            GeneralNames certNames = sanExt.get(SubjectAlternativeNameExtension.SUBJECT_NAME);
            Iterator<GeneralNameInterface> i = this.subjectAlternativeGeneralNames.iterator();
            while (i.hasNext()) {
                Object matchName = (GeneralNameInterface) i.next();
                boolean found = false;
                Iterator<GeneralName> t = certNames.iterator();
                while (t.hasNext() && !found) {
                    found = ((GeneralName) t.next()).getName().equals(matchName);
                }
                if (found || (!this.matchAllSubjectAltNames && i.hasNext())) {
                    if (found && !this.matchAllSubjectAltNames) {
                        break;
                    }
                } else {
                    if (debug != null) {
                        Debug debug = debug;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("X509CertSelector.match: subject alternative name ");
                        stringBuilder.append(matchName);
                        stringBuilder.append(" not found");
                        debug.println(stringBuilder.toString());
                    }
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("X509CertSelector.match: IOException in subject alternative name check");
            }
            return false;
        }
    }

    private boolean matchNameConstraints(X509Certificate xcert) {
        if (this.nc == null) {
            return true;
        }
        try {
            if (this.nc.verify(xcert)) {
                return true;
            }
            if (debug != null) {
                debug.println("X509CertSelector.match: name constraints not satisfied");
            }
            return false;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("X509CertSelector.match: IOException in name constraints check");
            }
            return false;
        }
    }

    private boolean matchPolicy(X509Certificate xcert) {
        if (this.policy == null) {
            return true;
        }
        try {
            CertificatePoliciesExtension ext = (CertificatePoliciesExtension) getExtensionObject(xcert, 3);
            if (ext == null) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: no certificate policy extension");
                }
                return false;
            }
            List<PolicyInformation> policies = ext.get(CertificatePoliciesExtension.POLICIES);
            List<CertificatePolicyId> policyIDs = new ArrayList(policies.size());
            for (PolicyInformation info : policies) {
                policyIDs.add(info.getPolicyIdentifier());
            }
            if (this.policy != null) {
                boolean foundOne = false;
                if (!this.policy.getCertPolicyIds().isEmpty()) {
                    for (CertificatePolicyId id : this.policy.getCertPolicyIds()) {
                        if (policyIDs.contains(id)) {
                            foundOne = true;
                            break;
                        }
                    }
                    if (!foundOne) {
                        if (debug != null) {
                            debug.println("X509CertSelector.match: cert failed policyAny criterion");
                        }
                        return false;
                    }
                } else if (policyIDs.isEmpty()) {
                    if (debug != null) {
                        debug.println("X509CertSelector.match: cert failed policyAny criterion");
                    }
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("X509CertSelector.match: IOException in certificate policy ID check");
            }
            return false;
        }
    }

    private boolean matchPathToNames(X509Certificate xcert) {
        if (this.pathToGeneralNames == null) {
            return true;
        }
        try {
            NameConstraintsExtension ext = (NameConstraintsExtension) getExtensionObject(xcert, 2);
            if (ext == null) {
                return true;
            }
            if (debug != null && Debug.isOn("certpath")) {
                debug.println("X509CertSelector.match pathToNames:\n");
                for (Object append : this.pathToGeneralNames) {
                    Debug debug = debug;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("    ");
                    stringBuilder.append(append);
                    stringBuilder.append("\n");
                    debug.println(stringBuilder.toString());
                }
            }
            GeneralSubtrees permitted = ext.get(NameConstraintsExtension.PERMITTED_SUBTREES);
            GeneralSubtrees excluded = ext.get(NameConstraintsExtension.EXCLUDED_SUBTREES);
            if (excluded != null && !matchExcluded(excluded)) {
                return false;
            }
            if (permitted == null || matchPermitted(permitted)) {
                return true;
            }
            return false;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("X509CertSelector.match: IOException in name constraints check");
            }
            return false;
        }
    }

    private boolean matchExcluded(GeneralSubtrees excluded) {
        Iterator<GeneralSubtree> t = excluded.iterator();
        while (t.hasNext()) {
            GeneralNameInterface excludedName = ((GeneralSubtree) t.next()).getName().getName();
            for (Object pathToName : this.pathToGeneralNames) {
                if (excludedName.getType() == pathToName.getType()) {
                    int constrains = pathToName.constrains(excludedName);
                    if (constrains == 0 || constrains == 2) {
                        if (debug != null) {
                            debug.println("X509CertSelector.match: name constraints inhibit path to specified name");
                            Debug debug = debug;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("X509CertSelector.match: excluded name: ");
                            stringBuilder.append(pathToName);
                            debug.println(stringBuilder.toString());
                        }
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean matchPermitted(GeneralSubtrees permitted) {
        for (GeneralNameInterface pathToName : this.pathToGeneralNames) {
            Iterator<GeneralSubtree> t = permitted.iterator();
            boolean permittedNameFound = false;
            boolean nameTypeFound = false;
            String names = "";
            while (t.hasNext() && !permittedNameFound) {
                Object permittedName = ((GeneralSubtree) t.next()).getName().getName();
                if (permittedName.getType() == pathToName.getType()) {
                    nameTypeFound = true;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(names);
                    stringBuilder.append("  ");
                    stringBuilder.append(permittedName);
                    names = stringBuilder.toString();
                    int constrains = pathToName.constrains(permittedName);
                    if (constrains == 0 || constrains == 2) {
                        permittedNameFound = true;
                    }
                }
            }
            if (!permittedNameFound && nameTypeFound) {
                if (debug != null) {
                    Debug debug = debug;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("X509CertSelector.match: name constraints inhibit path to specified name; permitted names of type ");
                    stringBuilder2.append(pathToName.getType());
                    stringBuilder2.append(": ");
                    stringBuilder2.append(names);
                    debug.println(stringBuilder2.toString());
                }
                return false;
            }
        }
        return true;
    }

    private boolean matchBasicConstraints(X509Certificate xcert) {
        if (this.basicConstraints == -1) {
            return true;
        }
        int maxPathLen = xcert.getBasicConstraints();
        if (this.basicConstraints == -2) {
            if (maxPathLen != -1) {
                if (debug != null) {
                    debug.println("X509CertSelector.match: not an EE cert");
                }
                return false;
            }
        } else if (maxPathLen < this.basicConstraints) {
            if (debug != null) {
                Debug debug = debug;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("X509CertSelector.match: cert's maxPathLen is less than the min maxPathLen set by basicConstraints. (");
                stringBuilder.append(maxPathLen);
                stringBuilder.append(" < ");
                stringBuilder.append(this.basicConstraints);
                stringBuilder.append(")");
                debug.println(stringBuilder.toString());
            }
            return false;
        }
        return true;
    }

    private static <T> Set<T> cloneSet(Set<T> set) {
        if (set instanceof HashSet) {
            return (Set) ((HashSet) set).clone();
        }
        return new HashSet((Collection) set);
    }

    public Object clone() {
        try {
            X509CertSelector copy = (X509CertSelector) super.clone();
            if (this.subjectAlternativeNames != null) {
                copy.subjectAlternativeNames = cloneSet(this.subjectAlternativeNames);
                copy.subjectAlternativeGeneralNames = cloneSet(this.subjectAlternativeGeneralNames);
            }
            if (this.pathToGeneralNames != null) {
                copy.pathToNames = cloneSet(this.pathToNames);
                copy.pathToGeneralNames = cloneSet(this.pathToGeneralNames);
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }
}
