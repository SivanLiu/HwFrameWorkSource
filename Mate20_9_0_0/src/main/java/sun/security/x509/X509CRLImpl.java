package sun.security.x509;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.security.auth.x500.X500Principal;
import sun.misc.HexDumpEncoder;
import sun.security.provider.X509Factory;
import sun.security.util.DerEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

public class X509CRLImpl extends X509CRL implements DerEncoder {
    private static final long YR_2050 = 2524636800000L;
    private static final boolean isExplicit = true;
    private CRLExtensions extensions;
    private AlgorithmId infoSigAlgId;
    private X500Name issuer;
    private X500Principal issuerPrincipal;
    private Date nextUpdate;
    private boolean readOnly;
    private List<X509CRLEntry> revokedList;
    private Map<X509IssuerSerial, X509CRLEntry> revokedMap;
    private AlgorithmId sigAlgId;
    private byte[] signature;
    private byte[] signedCRL;
    private byte[] tbsCertList;
    private Date thisUpdate;
    private String verifiedProvider;
    private PublicKey verifiedPublicKey;
    private int version;

    private static final class X509IssuerSerial implements Comparable<X509IssuerSerial> {
        volatile int hashcode;
        final X500Principal issuer;
        final BigInteger serial;

        X509IssuerSerial(X500Principal issuer, BigInteger serial) {
            this.hashcode = 0;
            this.issuer = issuer;
            this.serial = serial;
        }

        X509IssuerSerial(X509Certificate cert) {
            this(cert.getIssuerX500Principal(), cert.getSerialNumber());
        }

        X500Principal getIssuer() {
            return this.issuer;
        }

        BigInteger getSerial() {
            return this.serial;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return X509CRLImpl.isExplicit;
            }
            if (!(o instanceof X509IssuerSerial)) {
                return false;
            }
            X509IssuerSerial other = (X509IssuerSerial) o;
            if (this.serial.equals(other.getSerial()) && this.issuer.equals(other.getIssuer())) {
                return X509CRLImpl.isExplicit;
            }
            return false;
        }

        public int hashCode() {
            if (this.hashcode == 0) {
                this.hashcode = (37 * ((37 * 17) + this.issuer.hashCode())) + this.serial.hashCode();
            }
            return this.hashcode;
        }

        public int compareTo(X509IssuerSerial another) {
            int cissuer = this.issuer.toString().compareTo(another.issuer.toString());
            if (cissuer != 0) {
                return cissuer;
            }
            return this.serial.compareTo(another.serial);
        }
    }

    private X509CRLImpl() {
        this.signedCRL = null;
        this.signature = null;
        this.tbsCertList = null;
        this.sigAlgId = null;
        this.issuer = null;
        this.issuerPrincipal = null;
        this.thisUpdate = null;
        this.nextUpdate = null;
        this.revokedMap = new TreeMap();
        this.revokedList = new LinkedList();
        this.extensions = null;
        this.readOnly = false;
    }

    public X509CRLImpl(byte[] crlData) throws CRLException {
        this.signedCRL = null;
        this.signature = null;
        this.tbsCertList = null;
        this.sigAlgId = null;
        this.issuer = null;
        this.issuerPrincipal = null;
        this.thisUpdate = null;
        this.nextUpdate = null;
        this.revokedMap = new TreeMap();
        this.revokedList = new LinkedList();
        this.extensions = null;
        this.readOnly = false;
        try {
            parse(new DerValue(crlData));
        } catch (IOException e) {
            this.signedCRL = null;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Parsing error: ");
            stringBuilder.append(e.getMessage());
            throw new CRLException(stringBuilder.toString());
        }
    }

    public X509CRLImpl(DerValue val) throws CRLException {
        this.signedCRL = null;
        this.signature = null;
        this.tbsCertList = null;
        this.sigAlgId = null;
        this.issuer = null;
        this.issuerPrincipal = null;
        this.thisUpdate = null;
        this.nextUpdate = null;
        this.revokedMap = new TreeMap();
        this.revokedList = new LinkedList();
        this.extensions = null;
        this.readOnly = false;
        try {
            parse(val);
        } catch (IOException e) {
            this.signedCRL = null;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Parsing error: ");
            stringBuilder.append(e.getMessage());
            throw new CRLException(stringBuilder.toString());
        }
    }

    public X509CRLImpl(InputStream inStrm) throws CRLException {
        this.signedCRL = null;
        this.signature = null;
        this.tbsCertList = null;
        this.sigAlgId = null;
        this.issuer = null;
        this.issuerPrincipal = null;
        this.thisUpdate = null;
        this.nextUpdate = null;
        this.revokedMap = new TreeMap();
        this.revokedList = new LinkedList();
        this.extensions = null;
        this.readOnly = false;
        try {
            parse(new DerValue(inStrm));
        } catch (IOException e) {
            this.signedCRL = null;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Parsing error: ");
            stringBuilder.append(e.getMessage());
            throw new CRLException(stringBuilder.toString());
        }
    }

    public X509CRLImpl(X500Name issuer, Date thisDate, Date nextDate) {
        this.signedCRL = null;
        this.signature = null;
        this.tbsCertList = null;
        this.sigAlgId = null;
        this.issuer = null;
        this.issuerPrincipal = null;
        this.thisUpdate = null;
        this.nextUpdate = null;
        this.revokedMap = new TreeMap();
        this.revokedList = new LinkedList();
        this.extensions = null;
        this.readOnly = false;
        this.issuer = issuer;
        this.thisUpdate = thisDate;
        this.nextUpdate = nextDate;
    }

    public X509CRLImpl(X500Name issuer, Date thisDate, Date nextDate, X509CRLEntry[] badCerts) throws CRLException {
        this.signedCRL = null;
        this.signature = null;
        this.tbsCertList = null;
        this.sigAlgId = null;
        this.issuer = null;
        this.issuerPrincipal = null;
        this.thisUpdate = null;
        this.nextUpdate = null;
        this.revokedMap = new TreeMap();
        this.revokedList = new LinkedList();
        this.extensions = null;
        int i = 0;
        this.readOnly = false;
        this.issuer = issuer;
        this.thisUpdate = thisDate;
        this.nextUpdate = nextDate;
        if (badCerts != null) {
            X500Principal crlIssuer = getIssuerX500Principal();
            X500Principal badCertIssuer = crlIssuer;
            while (i < badCerts.length) {
                X509CRLEntryImpl badCert = badCerts[i];
                try {
                    badCertIssuer = getCertIssuer(badCert, badCertIssuer);
                    badCert.setCertificateIssuer(crlIssuer, badCertIssuer);
                    this.revokedMap.put(new X509IssuerSerial(badCertIssuer, badCert.getSerialNumber()), badCert);
                    this.revokedList.add(badCert);
                    if (badCert.hasExtensions()) {
                        this.version = 1;
                    }
                    i++;
                } catch (IOException ioe) {
                    throw new CRLException(ioe);
                }
            }
        }
    }

    public X509CRLImpl(X500Name issuer, Date thisDate, Date nextDate, X509CRLEntry[] badCerts, CRLExtensions crlExts) throws CRLException {
        this(issuer, thisDate, nextDate, badCerts);
        if (crlExts != null) {
            this.extensions = crlExts;
            this.version = 1;
        }
    }

    public byte[] getEncodedInternal() throws CRLException {
        if (this.signedCRL != null) {
            return this.signedCRL;
        }
        throw new CRLException("Null CRL to encode");
    }

    public byte[] getEncoded() throws CRLException {
        return (byte[]) getEncodedInternal().clone();
    }

    public void encodeInfo(OutputStream out) throws CRLException {
        try {
            DerOutputStream tmp = new DerOutputStream();
            DerOutputStream rCerts = new DerOutputStream();
            DerOutputStream seq = new DerOutputStream();
            if (this.version != 0) {
                tmp.putInteger(this.version);
            }
            this.infoSigAlgId.encode(tmp);
            if (this.version == 0) {
                if (this.issuer.toString() == null) {
                    throw new CRLException("Null Issuer DN not allowed in v1 CRL");
                }
            }
            this.issuer.encode(tmp);
            if (this.thisUpdate.getTime() < YR_2050) {
                tmp.putUTCTime(this.thisUpdate);
            } else {
                tmp.putGeneralizedTime(this.thisUpdate);
            }
            if (this.nextUpdate != null) {
                if (this.nextUpdate.getTime() < YR_2050) {
                    tmp.putUTCTime(this.nextUpdate);
                } else {
                    tmp.putGeneralizedTime(this.nextUpdate);
                }
            }
            if (!this.revokedList.isEmpty()) {
                for (X509CRLEntry entry : this.revokedList) {
                    ((X509CRLEntryImpl) entry).encode(rCerts);
                }
                tmp.write((byte) 48, rCerts);
            }
            if (this.extensions != null) {
                this.extensions.encode(tmp, isExplicit);
            }
            seq.write((byte) 48, tmp);
            this.tbsCertList = seq.toByteArray();
            out.write(this.tbsCertList);
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Encoding error: ");
            stringBuilder.append(e.getMessage());
            throw new CRLException(stringBuilder.toString());
        }
    }

    public void verify(PublicKey key) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        verify(key, "");
    }

    public synchronized void verify(PublicKey key, String sigProvider) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        if (sigProvider == null) {
            try {
                sigProvider = "";
            } catch (Throwable th) {
            }
        }
        if (this.verifiedPublicKey == null || !this.verifiedPublicKey.equals(key) || !sigProvider.equals(this.verifiedProvider)) {
            if (this.signedCRL != null) {
                Signature sigVerf;
                if (sigProvider.length() == 0) {
                    sigVerf = Signature.getInstance(this.sigAlgId.getName());
                } else {
                    sigVerf = Signature.getInstance(this.sigAlgId.getName(), sigProvider);
                }
                sigVerf.initVerify(key);
                if (this.tbsCertList != null) {
                    sigVerf.update(this.tbsCertList, 0, this.tbsCertList.length);
                    if (sigVerf.verify(this.signature)) {
                        this.verifiedPublicKey = key;
                        this.verifiedProvider = sigProvider;
                        return;
                    }
                    throw new SignatureException("Signature does not match.");
                }
                throw new CRLException("Uninitialized CRL");
            }
            throw new CRLException("Uninitialized CRL");
        }
    }

    public synchronized void verify(PublicKey key, Provider sigProvider) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        if (this.signedCRL != null) {
            Signature sigVerf;
            if (sigProvider == null) {
                sigVerf = Signature.getInstance(this.sigAlgId.getName());
            } else {
                sigVerf = Signature.getInstance(this.sigAlgId.getName(), sigProvider);
            }
            sigVerf.initVerify(key);
            if (this.tbsCertList != null) {
                sigVerf.update(this.tbsCertList, 0, this.tbsCertList.length);
                if (sigVerf.verify(this.signature)) {
                    this.verifiedPublicKey = key;
                } else {
                    throw new SignatureException("Signature does not match.");
                }
            }
            throw new CRLException("Uninitialized CRL");
        }
        throw new CRLException("Uninitialized CRL");
    }

    public void sign(PrivateKey key, String algorithm) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        sign(key, algorithm, null);
    }

    public void sign(PrivateKey key, String algorithm, String provider) throws CRLException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        try {
            if (this.readOnly) {
                throw new CRLException("cannot over-write existing CRL");
            }
            Signature sigEngine;
            DerOutputStream out;
            DerOutputStream tmp;
            if (provider != null) {
                if (provider.length() != 0) {
                    sigEngine = Signature.getInstance(algorithm, provider);
                    sigEngine.initSign(key);
                    this.sigAlgId = AlgorithmId.get(sigEngine.getAlgorithm());
                    this.infoSigAlgId = this.sigAlgId;
                    out = new DerOutputStream();
                    tmp = new DerOutputStream();
                    encodeInfo(tmp);
                    this.sigAlgId.encode(tmp);
                    sigEngine.update(this.tbsCertList, 0, this.tbsCertList.length);
                    this.signature = sigEngine.sign();
                    tmp.putBitString(this.signature);
                    out.write((byte) 48, tmp);
                    this.signedCRL = out.toByteArray();
                    this.readOnly = isExplicit;
                }
            }
            sigEngine = Signature.getInstance(algorithm);
            sigEngine.initSign(key);
            this.sigAlgId = AlgorithmId.get(sigEngine.getAlgorithm());
            this.infoSigAlgId = this.sigAlgId;
            out = new DerOutputStream();
            tmp = new DerOutputStream();
            encodeInfo(tmp);
            this.sigAlgId.encode(tmp);
            sigEngine.update(this.tbsCertList, 0, this.tbsCertList.length);
            this.signature = sigEngine.sign();
            tmp.putBitString(this.signature);
            out.write((byte) 48, tmp);
            this.signedCRL = out.toByteArray();
            this.readOnly = isExplicit;
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error while encoding data: ");
            stringBuilder.append(e.getMessage());
            throw new CRLException(stringBuilder.toString());
        }
    }

    public String toString() {
        StringBuilder stringBuilder;
        StringBuffer sb = new StringBuffer();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("X.509 CRL v");
        stringBuilder2.append(this.version + 1);
        stringBuilder2.append("\n");
        sb.append(stringBuilder2.toString());
        if (this.sigAlgId != null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Signature Algorithm: ");
            stringBuilder2.append(this.sigAlgId.toString());
            stringBuilder2.append(", OID=");
            stringBuilder2.append(this.sigAlgId.getOID().toString());
            stringBuilder2.append("\n");
            sb.append(stringBuilder2.toString());
        }
        if (this.issuer != null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Issuer: ");
            stringBuilder2.append(this.issuer.toString());
            stringBuilder2.append("\n");
            sb.append(stringBuilder2.toString());
        }
        if (this.thisUpdate != null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("\nThis Update: ");
            stringBuilder2.append(this.thisUpdate.toString());
            stringBuilder2.append("\n");
            sb.append(stringBuilder2.toString());
        }
        if (this.nextUpdate != null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Next Update: ");
            stringBuilder2.append(this.nextUpdate.toString());
            stringBuilder2.append("\n");
            sb.append(stringBuilder2.toString());
        }
        if (this.revokedList.isEmpty()) {
            sb.append("\nNO certificates have been revoked\n");
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("\nRevoked Certificates: ");
            stringBuilder2.append(this.revokedList.size());
            sb.append(stringBuilder2.toString());
            int i = 1;
            for (X509CRLEntry entry : this.revokedList) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("\n[");
                int i2 = i + 1;
                stringBuilder.append(i);
                stringBuilder.append("] ");
                stringBuilder.append(entry.toString());
                sb.append(stringBuilder.toString());
                i = i2;
            }
        }
        if (this.extensions != null) {
            Object[] objs = this.extensions.getAllExtensions().toArray();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("\nCRL Extensions: ");
            stringBuilder3.append(objs.length);
            sb.append(stringBuilder3.toString());
            for (int i3 = 0; i3 < objs.length; i3++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("\n[");
                stringBuilder.append(i3 + 1);
                stringBuilder.append("]: ");
                sb.append(stringBuilder.toString());
                Extension ext = objs[i3];
                try {
                    if (OIDMap.getClass(ext.getExtensionId()) == null) {
                        sb.append(ext.toString());
                        byte[] extValue = ext.getExtensionValue();
                        if (extValue != null) {
                            DerOutputStream out = new DerOutputStream();
                            out.putOctetString(extValue);
                            extValue = out.toByteArray();
                            HexDumpEncoder enc = new HexDumpEncoder();
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("Extension unknown: DER encoded OCTET string =\n");
                            stringBuilder4.append(enc.encodeBuffer(extValue));
                            stringBuilder4.append("\n");
                            sb.append(stringBuilder4.toString());
                        }
                    } else {
                        sb.append(ext.toString());
                    }
                } catch (Exception e) {
                    sb.append(", Error parsing this extension");
                }
            }
        }
        if (this.signature != null) {
            HexDumpEncoder encoder = new HexDumpEncoder();
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append("\nSignature:\n");
            stringBuilder5.append(encoder.encodeBuffer(this.signature));
            stringBuilder5.append("\n");
            sb.append(stringBuilder5.toString());
        } else {
            sb.append("NOT signed yet\n");
        }
        return sb.toString();
    }

    public boolean isRevoked(Certificate cert) {
        if (this.revokedMap.isEmpty() || !(cert instanceof X509Certificate)) {
            return false;
        }
        return this.revokedMap.containsKey(new X509IssuerSerial((X509Certificate) cert));
    }

    public int getVersion() {
        return this.version + 1;
    }

    public Principal getIssuerDN() {
        return this.issuer;
    }

    public X500Principal getIssuerX500Principal() {
        if (this.issuerPrincipal == null) {
            this.issuerPrincipal = this.issuer.asX500Principal();
        }
        return this.issuerPrincipal;
    }

    public Date getThisUpdate() {
        return new Date(this.thisUpdate.getTime());
    }

    public Date getNextUpdate() {
        if (this.nextUpdate == null) {
            return null;
        }
        return new Date(this.nextUpdate.getTime());
    }

    public X509CRLEntry getRevokedCertificate(BigInteger serialNumber) {
        if (this.revokedMap.isEmpty()) {
            return null;
        }
        return (X509CRLEntry) this.revokedMap.get(new X509IssuerSerial(getIssuerX500Principal(), serialNumber));
    }

    public X509CRLEntry getRevokedCertificate(X509Certificate cert) {
        if (this.revokedMap.isEmpty()) {
            return null;
        }
        return (X509CRLEntry) this.revokedMap.get(new X509IssuerSerial(cert));
    }

    public Set<X509CRLEntry> getRevokedCertificates() {
        if (this.revokedList.isEmpty()) {
            return null;
        }
        return new TreeSet(this.revokedList);
    }

    public byte[] getTBSCertList() throws CRLException {
        if (this.tbsCertList != null) {
            return (byte[]) this.tbsCertList.clone();
        }
        throw new CRLException("Uninitialized CRL");
    }

    public byte[] getSignature() {
        if (this.signature == null) {
            return null;
        }
        return (byte[]) this.signature.clone();
    }

    public String getSigAlgName() {
        if (this.sigAlgId == null) {
            return null;
        }
        return this.sigAlgId.getName();
    }

    public String getSigAlgOID() {
        if (this.sigAlgId == null) {
            return null;
        }
        return this.sigAlgId.getOID().toString();
    }

    public byte[] getSigAlgParams() {
        if (this.sigAlgId == null) {
            return null;
        }
        try {
            return this.sigAlgId.getEncodedParams();
        } catch (IOException e) {
            return null;
        }
    }

    public AlgorithmId getSigAlgId() {
        return this.sigAlgId;
    }

    public KeyIdentifier getAuthKeyId() throws IOException {
        AuthorityKeyIdentifierExtension aki = getAuthKeyIdExtension();
        if (aki != null) {
            return (KeyIdentifier) aki.get("key_id");
        }
        return null;
    }

    public AuthorityKeyIdentifierExtension getAuthKeyIdExtension() throws IOException {
        return (AuthorityKeyIdentifierExtension) getExtension(PKIXExtensions.AuthorityKey_Id);
    }

    public CRLNumberExtension getCRLNumberExtension() throws IOException {
        return (CRLNumberExtension) getExtension(PKIXExtensions.CRLNumber_Id);
    }

    public BigInteger getCRLNumber() throws IOException {
        CRLNumberExtension numExt = getCRLNumberExtension();
        if (numExt != null) {
            return numExt.get("value");
        }
        return null;
    }

    public DeltaCRLIndicatorExtension getDeltaCRLIndicatorExtension() throws IOException {
        return (DeltaCRLIndicatorExtension) getExtension(PKIXExtensions.DeltaCRLIndicator_Id);
    }

    public BigInteger getBaseCRLNumber() throws IOException {
        DeltaCRLIndicatorExtension dciExt = getDeltaCRLIndicatorExtension();
        if (dciExt != null) {
            return dciExt.get("value");
        }
        return null;
    }

    public IssuerAlternativeNameExtension getIssuerAltNameExtension() throws IOException {
        return (IssuerAlternativeNameExtension) getExtension(PKIXExtensions.IssuerAlternativeName_Id);
    }

    public IssuingDistributionPointExtension getIssuingDistributionPointExtension() throws IOException {
        return (IssuingDistributionPointExtension) getExtension(PKIXExtensions.IssuingDistributionPoint_Id);
    }

    public boolean hasUnsupportedCriticalExtension() {
        if (this.extensions == null) {
            return false;
        }
        return this.extensions.hasUnsupportedCriticalExtension();
    }

    public Set<String> getCriticalExtensionOIDs() {
        if (this.extensions == null) {
            return null;
        }
        Set<String> extSet = new TreeSet();
        for (Extension ex : this.extensions.getAllExtensions()) {
            if (ex.isCritical()) {
                extSet.add(ex.getExtensionId().toString());
            }
        }
        return extSet;
    }

    public Set<String> getNonCriticalExtensionOIDs() {
        if (this.extensions == null) {
            return null;
        }
        Set<String> extSet = new TreeSet();
        for (Extension ex : this.extensions.getAllExtensions()) {
            if (!ex.isCritical()) {
                extSet.add(ex.getExtensionId().toString());
            }
        }
        return extSet;
    }

    public byte[] getExtensionValue(String oid) {
        if (this.extensions == null) {
            return null;
        }
        try {
            String extAlias = OIDMap.getName(new ObjectIdentifier(oid));
            Extension crlExt = null;
            if (extAlias == null) {
                Object findOID = new ObjectIdentifier(oid);
                Enumeration<Extension> e = this.extensions.getElements();
                while (e.hasMoreElements()) {
                    Extension ex = (Extension) e.nextElement();
                    if (ex.getExtensionId().equals(findOID)) {
                        crlExt = ex;
                        break;
                    }
                }
            }
            crlExt = this.extensions.get(extAlias);
            if (crlExt == null) {
                return null;
            }
            byte[] extData = crlExt.getExtensionValue();
            if (extData == null) {
                return null;
            }
            DerOutputStream out = new DerOutputStream();
            out.putOctetString(extData);
            return out.toByteArray();
        } catch (Exception e2) {
            return null;
        }
    }

    public Object getExtension(ObjectIdentifier oid) {
        if (this.extensions == null) {
            return null;
        }
        return this.extensions.get(OIDMap.getName(oid));
    }

    private void parse(DerValue val) throws CRLException, IOException {
        if (this.readOnly) {
            throw new CRLException("cannot over-write existing CRL");
        } else if (val.getData() == null || val.tag != (byte) 48) {
            throw new CRLException("Invalid DER-encoded CRL data");
        } else {
            this.signedCRL = val.toByteArray();
            DerValue[] seq = new DerValue[]{val.data.getDerValue(), val.data.getDerValue(), val.data.getDerValue()};
            if (val.data.available() != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("signed overrun, bytes = ");
                stringBuilder.append(val.data.available());
                throw new CRLException(stringBuilder.toString());
            } else if (seq[0].tag == (byte) 48) {
                this.sigAlgId = AlgorithmId.parse(seq[1]);
                this.signature = seq[2].getBitString();
                if (seq[1].data.available() != 0) {
                    throw new CRLException("AlgorithmId field overrun");
                } else if (seq[2].data.available() == 0) {
                    this.tbsCertList = seq[0].toByteArray();
                    DerInputStream derStrm = seq[0].data;
                    this.version = 0;
                    if (((byte) derStrm.peekByte()) == (byte) 2) {
                        this.version = derStrm.getInteger();
                        if (this.version != 1) {
                            throw new CRLException("Invalid version");
                        }
                    }
                    AlgorithmId tmpId = AlgorithmId.parse(derStrm.getDerValue());
                    if (tmpId.equals(this.sigAlgId)) {
                        this.infoSigAlgId = tmpId;
                        this.issuer = new X500Name(derStrm);
                        if (this.issuer.isEmpty()) {
                            throw new CRLException("Empty issuer DN not allowed in X509CRLs");
                        }
                        int nextByte = (byte) derStrm.peekByte();
                        if (nextByte == (byte) 23) {
                            this.thisUpdate = derStrm.getUTCTime();
                        } else if (nextByte == (byte) 24) {
                            this.thisUpdate = derStrm.getGeneralizedTime();
                        } else {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Invalid encoding for thisUpdate (tag=");
                            stringBuilder2.append(nextByte);
                            stringBuilder2.append(")");
                            throw new CRLException(stringBuilder2.toString());
                        }
                        if (derStrm.available() != 0) {
                            byte nextByte2 = (byte) derStrm.peekByte();
                            if (nextByte2 == (byte) 23) {
                                this.nextUpdate = derStrm.getUTCTime();
                            } else if (nextByte2 == (byte) 24) {
                                this.nextUpdate = derStrm.getGeneralizedTime();
                            }
                            if (derStrm.available() != 0) {
                                nextByte2 = (byte) derStrm.peekByte();
                                if (nextByte2 == (byte) 48 && (nextByte2 & 192) != 128) {
                                    DerValue[] badCerts = derStrm.getSequence(4);
                                    X500Principal crlIssuer = getIssuerX500Principal();
                                    X500Principal badCertIssuer = crlIssuer;
                                    for (DerValue x509CRLEntryImpl : badCerts) {
                                        X509CRLEntryImpl entry = new X509CRLEntryImpl(x509CRLEntryImpl);
                                        badCertIssuer = getCertIssuer(entry, badCertIssuer);
                                        entry.setCertificateIssuer(crlIssuer, badCertIssuer);
                                        this.revokedMap.put(new X509IssuerSerial(badCertIssuer, entry.getSerialNumber()), entry);
                                        this.revokedList.add(entry);
                                    }
                                }
                                if (derStrm.available() != 0) {
                                    DerValue tmp = derStrm.getDerValue();
                                    if (tmp.isConstructed() && tmp.isContextSpecific((byte) 0)) {
                                        this.extensions = new CRLExtensions(tmp.data);
                                    }
                                    this.readOnly = isExplicit;
                                    return;
                                }
                                return;
                            }
                            return;
                        }
                        return;
                    }
                    throw new CRLException("Signature algorithm mismatch");
                } else {
                    throw new CRLException("Signature field overrun");
                }
            } else {
                throw new CRLException("signed CRL fields invalid");
            }
        }
    }

    public static X500Principal getIssuerX500Principal(X509CRL crl) {
        try {
            DerInputStream tbsIn = new DerInputStream(crl.getEncoded()).getSequence(3)[0].data;
            if (((byte) tbsIn.peekByte()) == (byte) 2) {
                tbsIn.getDerValue();
            }
            DerValue tmp = tbsIn.getDerValue();
            return new X500Principal(tbsIn.getDerValue().toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Could not parse issuer", e);
        }
    }

    public static byte[] getEncodedInternal(X509CRL crl) throws CRLException {
        if (crl instanceof X509CRLImpl) {
            return ((X509CRLImpl) crl).getEncodedInternal();
        }
        return crl.getEncoded();
    }

    public static X509CRLImpl toImpl(X509CRL crl) throws CRLException {
        if (crl instanceof X509CRLImpl) {
            return (X509CRLImpl) crl;
        }
        return X509Factory.intern(crl);
    }

    private X500Principal getCertIssuer(X509CRLEntryImpl entry, X500Principal prevCertIssuer) throws IOException {
        CertificateIssuerExtension ciExt = entry.getCertificateIssuerExtension();
        if (ciExt != null) {
            return ((X500Name) ciExt.get("issuer").get(0).getName()).asX500Principal();
        }
        return prevCertIssuer;
    }

    public void derEncode(OutputStream out) throws IOException {
        if (this.signedCRL != null) {
            out.write((byte[]) this.signedCRL.clone());
            return;
        }
        throw new IOException("Null CRL to encode");
    }
}
