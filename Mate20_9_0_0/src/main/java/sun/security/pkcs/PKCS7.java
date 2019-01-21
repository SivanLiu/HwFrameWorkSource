package sun.security.pkcs;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.security.auth.x500.X500Principal;
import sun.security.util.Debug;
import sun.security.util.DerEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;
import sun.security.x509.X509CRLImpl;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

public class PKCS7 {
    private Principal[] certIssuerNames;
    private X509Certificate[] certificates;
    private ContentInfo contentInfo;
    private ObjectIdentifier contentType;
    private X509CRL[] crls;
    private AlgorithmId[] digestAlgorithmIds;
    private boolean oldStyle;
    private SignerInfo[] signerInfos;
    private BigInteger version;

    private static class WrappedX509Certificate extends X509Certificate {
        private final X509Certificate wrapped;

        public WrappedX509Certificate(X509Certificate wrapped) {
            this.wrapped = wrapped;
        }

        public Set<String> getCriticalExtensionOIDs() {
            return this.wrapped.getCriticalExtensionOIDs();
        }

        public byte[] getExtensionValue(String oid) {
            return this.wrapped.getExtensionValue(oid);
        }

        public Set<String> getNonCriticalExtensionOIDs() {
            return this.wrapped.getNonCriticalExtensionOIDs();
        }

        public boolean hasUnsupportedCriticalExtension() {
            return this.wrapped.hasUnsupportedCriticalExtension();
        }

        public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
            this.wrapped.checkValidity();
        }

        public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
            this.wrapped.checkValidity(date);
        }

        public int getVersion() {
            return this.wrapped.getVersion();
        }

        public BigInteger getSerialNumber() {
            return this.wrapped.getSerialNumber();
        }

        public Principal getIssuerDN() {
            return this.wrapped.getIssuerDN();
        }

        public Principal getSubjectDN() {
            return this.wrapped.getSubjectDN();
        }

        public Date getNotBefore() {
            return this.wrapped.getNotBefore();
        }

        public Date getNotAfter() {
            return this.wrapped.getNotAfter();
        }

        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return this.wrapped.getTBSCertificate();
        }

        public byte[] getSignature() {
            return this.wrapped.getSignature();
        }

        public String getSigAlgName() {
            return this.wrapped.getSigAlgName();
        }

        public String getSigAlgOID() {
            return this.wrapped.getSigAlgOID();
        }

        public byte[] getSigAlgParams() {
            return this.wrapped.getSigAlgParams();
        }

        public boolean[] getIssuerUniqueID() {
            return this.wrapped.getIssuerUniqueID();
        }

        public boolean[] getSubjectUniqueID() {
            return this.wrapped.getSubjectUniqueID();
        }

        public boolean[] getKeyUsage() {
            return this.wrapped.getKeyUsage();
        }

        public int getBasicConstraints() {
            return this.wrapped.getBasicConstraints();
        }

        public byte[] getEncoded() throws CertificateEncodingException {
            return this.wrapped.getEncoded();
        }

        public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
            this.wrapped.verify(key);
        }

        public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
            this.wrapped.verify(key, sigProvider);
        }

        public String toString() {
            return this.wrapped.toString();
        }

        public PublicKey getPublicKey() {
            return this.wrapped.getPublicKey();
        }

        public List<String> getExtendedKeyUsage() throws CertificateParsingException {
            return this.wrapped.getExtendedKeyUsage();
        }

        public Collection<List<?>> getIssuerAlternativeNames() throws CertificateParsingException {
            return this.wrapped.getIssuerAlternativeNames();
        }

        public X500Principal getIssuerX500Principal() {
            return this.wrapped.getIssuerX500Principal();
        }

        public Collection<List<?>> getSubjectAlternativeNames() throws CertificateParsingException {
            return this.wrapped.getSubjectAlternativeNames();
        }

        public X500Principal getSubjectX500Principal() {
            return this.wrapped.getSubjectX500Principal();
        }

        public void verify(PublicKey key, Provider sigProvider) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
            this.wrapped.verify(key, sigProvider);
        }
    }

    private static class VerbatimX509Certificate extends WrappedX509Certificate {
        private byte[] encodedVerbatim;

        public VerbatimX509Certificate(X509Certificate wrapped, byte[] encodedVerbatim) {
            super(wrapped);
            this.encodedVerbatim = encodedVerbatim;
        }

        public byte[] getEncoded() throws CertificateEncodingException {
            return this.encodedVerbatim;
        }
    }

    public PKCS7(InputStream in) throws ParsingException, IOException {
        this.version = null;
        this.digestAlgorithmIds = null;
        this.contentInfo = null;
        this.certificates = null;
        this.crls = null;
        this.signerInfos = null;
        this.oldStyle = false;
        DataInputStream dis = new DataInputStream(in);
        byte[] data = new byte[dis.available()];
        dis.readFully(data);
        parse(new DerInputStream(data));
    }

    public PKCS7(DerInputStream derin) throws ParsingException {
        this.version = null;
        this.digestAlgorithmIds = null;
        this.contentInfo = null;
        this.certificates = null;
        this.crls = null;
        this.signerInfos = null;
        this.oldStyle = false;
        parse(derin);
    }

    public PKCS7(byte[] bytes) throws ParsingException {
        this.version = null;
        this.digestAlgorithmIds = null;
        this.contentInfo = null;
        this.certificates = null;
        this.crls = null;
        this.signerInfos = null;
        this.oldStyle = false;
        try {
            parse(new DerInputStream(bytes));
        } catch (IOException ioe1) {
            ParsingException pe = new ParsingException("Unable to parse the encoded bytes");
            pe.initCause(ioe1);
            throw pe;
        }
    }

    private void parse(DerInputStream derin) throws ParsingException {
        try {
            derin.mark(derin.available());
            parse(derin, false);
        } catch (IOException ioe) {
            try {
                derin.reset();
                parse(derin, true);
                this.oldStyle = true;
            } catch (IOException ioe1) {
                ParsingException pe = new ParsingException(ioe1.getMessage());
                pe.initCause(ioe);
                pe.addSuppressed(ioe1);
                throw pe;
            }
        }
    }

    private void parse(DerInputStream derin, boolean oldStyle) throws IOException {
        this.contentInfo = new ContentInfo(derin, oldStyle);
        this.contentType = this.contentInfo.contentType;
        DerValue content = this.contentInfo.getContent();
        if (this.contentType.equals(ContentInfo.SIGNED_DATA_OID)) {
            parseSignedData(content);
        } else if (this.contentType.equals(ContentInfo.OLD_SIGNED_DATA_OID)) {
            parseOldSignedData(content);
        } else if (this.contentType.equals(ContentInfo.NETSCAPE_CERT_SEQUENCE_OID)) {
            parseNetscapeCertChain(content);
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("content type ");
            stringBuilder.append(this.contentType);
            stringBuilder.append(" not supported.");
            throw new ParsingException(stringBuilder.toString());
        }
    }

    public PKCS7(AlgorithmId[] digestAlgorithmIds, ContentInfo contentInfo, X509Certificate[] certificates, X509CRL[] crls, SignerInfo[] signerInfos) {
        this.version = null;
        this.digestAlgorithmIds = null;
        this.contentInfo = null;
        this.certificates = null;
        this.crls = null;
        this.signerInfos = null;
        this.oldStyle = false;
        this.version = BigInteger.ONE;
        this.digestAlgorithmIds = digestAlgorithmIds;
        this.contentInfo = contentInfo;
        this.certificates = certificates;
        this.crls = crls;
        this.signerInfos = signerInfos;
    }

    public PKCS7(AlgorithmId[] digestAlgorithmIds, ContentInfo contentInfo, X509Certificate[] certificates, SignerInfo[] signerInfos) {
        this(digestAlgorithmIds, contentInfo, certificates, null, signerInfos);
    }

    private void parseNetscapeCertChain(DerValue val) throws ParsingException, IOException {
        ParsingException pe;
        DerValue[] contents = new DerInputStream(val.toByteArray()).getSequence(2, 1);
        this.certificates = new X509Certificate[contents.length];
        CertificateFactory certfac = null;
        try {
            certfac = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
        }
        int i = 0;
        while (i < contents.length) {
            ByteArrayInputStream bais = null;
            try {
                byte[] original = contents[i].getOriginalEncodedForm();
                if (certfac == null) {
                    this.certificates[i] = new X509CertImpl(contents[i], original);
                } else {
                    bais = new ByteArrayInputStream(original);
                    this.certificates[i] = new VerbatimX509Certificate((X509Certificate) certfac.generateCertificate(bais), original);
                    bais.close();
                    bais = null;
                }
                if (bais != null) {
                    bais.close();
                }
                i++;
            } catch (CertificateException ce) {
                pe = new ParsingException(ce.getMessage());
                pe.initCause(ce);
                throw pe;
            } catch (IOException ioe) {
                pe = new ParsingException(ioe.getMessage());
                pe.initCause(ioe);
                throw pe;
            } catch (Throwable th) {
                if (bais != null) {
                    bais.close();
                }
            }
        }
    }

    private void parseSignedData(DerValue val) throws ParsingException, IOException {
        ParsingException pe;
        DerInputStream dis = val.toDerInputStream();
        this.version = dis.getBigInteger();
        DerValue[] digestAlgorithmIdVals = dis.getSet(1);
        int len = digestAlgorithmIdVals.length;
        this.digestAlgorithmIds = new AlgorithmId[len];
        int i = 0;
        while (i < len) {
            try {
                this.digestAlgorithmIds[i] = AlgorithmId.parse(digestAlgorithmIdVals[i]);
                i++;
            } catch (IOException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error parsing digest AlgorithmId IDs: ");
                stringBuilder.append(e.getMessage());
                pe = new ParsingException(stringBuilder.toString());
                pe.initCause(e);
                throw pe;
            }
        }
        this.contentInfo = new ContentInfo(dis);
        CertificateFactory certfac = null;
        try {
            certfac = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e2) {
        }
        if (((byte) dis.peekByte()) == (byte) -96) {
            DerValue[] certVals = dis.getSet(2, true, true);
            len = certVals.length;
            this.certificates = new X509Certificate[len];
            int count = 0;
            i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= len) {
                    break;
                }
                ByteArrayInputStream bais = null;
                try {
                    if (certVals[i2].getTag() == (byte) 48) {
                        byte[] original = certVals[i2].getOriginalEncodedForm();
                        if (certfac == null) {
                            this.certificates[count] = new X509CertImpl(certVals[i2], original);
                        } else {
                            bais = new ByteArrayInputStream(original);
                            this.certificates[count] = new VerbatimX509Certificate((X509Certificate) certfac.generateCertificate(bais), original);
                            bais.close();
                            bais = null;
                        }
                        count++;
                    }
                    if (bais != null) {
                        bais.close();
                    }
                    i = i2 + 1;
                } catch (CertificateException ce) {
                    pe = new ParsingException(ce.getMessage());
                    pe.initCause(ce);
                    throw pe;
                } catch (IOException e3) {
                    pe = new ParsingException(e3.getMessage());
                    pe.initCause(e3);
                    throw pe;
                } catch (Throwable th) {
                    if (bais != null) {
                        bais.close();
                    }
                }
            }
            if (count != len) {
                this.certificates = (X509Certificate[]) Arrays.copyOf(this.certificates, count);
            }
        }
        if (((byte) dis.peekByte()) == (byte) -95) {
            DerValue[] crlVals = dis.getSet(1, true);
            len = crlVals.length;
            this.crls = new X509CRL[len];
            i = 0;
            while (true) {
                int i3 = i;
                if (i3 >= len) {
                    break;
                }
                ByteArrayInputStream bais2 = null;
                if (certfac == null) {
                    try {
                        this.crls[i3] = new X509CRLImpl(crlVals[i3]);
                    } catch (CRLException e4) {
                        pe = new ParsingException(e4.getMessage());
                        pe.initCause(e4);
                        throw pe;
                    } catch (Throwable th2) {
                        if (bais2 != null) {
                            bais2.close();
                        }
                    }
                } else {
                    bais2 = new ByteArrayInputStream(crlVals[i3].toByteArray());
                    this.crls[i3] = (X509CRL) certfac.generateCRL(bais2);
                    bais2.close();
                    bais2 = null;
                }
                if (bais2 != null) {
                    bais2.close();
                }
                i = i3 + 1;
            }
        }
        DerValue[] signerInfoVals = dis.getSet(1);
        int len2 = signerInfoVals.length;
        this.signerInfos = new SignerInfo[len2];
        int i4 = 0;
        while (true) {
            len = i4;
            if (len < len2) {
                this.signerInfos[len] = new SignerInfo(signerInfoVals[len].toDerInputStream());
                i4 = len + 1;
            } else {
                return;
            }
        }
    }

    private void parseOldSignedData(DerValue val) throws ParsingException, IOException {
        ParsingException pe;
        DerInputStream dis = val.toDerInputStream();
        this.version = dis.getBigInteger();
        DerValue[] digestAlgorithmIdVals = dis.getSet(1);
        int len = digestAlgorithmIdVals.length;
        this.digestAlgorithmIds = new AlgorithmId[len];
        int i = 0;
        int i2 = 0;
        while (i2 < len) {
            try {
                this.digestAlgorithmIds[i2] = AlgorithmId.parse(digestAlgorithmIdVals[i2]);
                i2++;
            } catch (IOException e) {
                throw new ParsingException("Error parsing digest AlgorithmId IDs");
            }
        }
        this.contentInfo = new ContentInfo(dis, true);
        CertificateFactory certfac = null;
        try {
            certfac = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e2) {
        }
        DerValue[] certVals = dis.getSet(2, false, true);
        len = certVals.length;
        this.certificates = new X509Certificate[len];
        int i3 = 0;
        while (i3 < len) {
            ByteArrayInputStream bais = null;
            try {
                byte[] original = certVals[i3].getOriginalEncodedForm();
                if (certfac == null) {
                    this.certificates[i3] = new X509CertImpl(certVals[i3], original);
                } else {
                    bais = new ByteArrayInputStream(original);
                    this.certificates[i3] = new VerbatimX509Certificate((X509Certificate) certfac.generateCertificate(bais), original);
                    bais.close();
                    bais = null;
                }
                if (bais != null) {
                    bais.close();
                }
                i3++;
            } catch (CertificateException ce) {
                pe = new ParsingException(ce.getMessage());
                pe.initCause(ce);
                throw pe;
            } catch (IOException ioe) {
                pe = new ParsingException(ioe.getMessage());
                pe.initCause(ioe);
                throw pe;
            } catch (Throwable th) {
                if (bais != null) {
                    bais.close();
                }
            }
        }
        dis.getSet(0);
        DerValue[] signerInfoVals = dis.getSet(1);
        len = signerInfoVals.length;
        this.signerInfos = new SignerInfo[len];
        while (i < len) {
            this.signerInfos[i] = new SignerInfo(signerInfoVals[i].toDerInputStream(), true);
            i++;
        }
    }

    public void encodeSignedData(OutputStream out) throws IOException {
        DerOutputStream derout = new DerOutputStream();
        encodeSignedData(derout);
        out.write(derout.toByteArray());
    }

    public void encodeSignedData(DerOutputStream out) throws IOException {
        DerOutputStream signedData = new DerOutputStream();
        signedData.putInteger(this.version);
        signedData.putOrderedSetOf((byte) 49, this.digestAlgorithmIds);
        this.contentInfo.encode(signedData);
        int i = 0;
        if (!(this.certificates == null || this.certificates.length == 0)) {
            X509CertImpl[] implCerts = new X509CertImpl[this.certificates.length];
            for (int i2 = 0; i2 < this.certificates.length; i2++) {
                if (this.certificates[i2] instanceof X509CertImpl) {
                    implCerts[i2] = (X509CertImpl) this.certificates[i2];
                } else {
                    try {
                        implCerts[i2] = new X509CertImpl(this.certificates[i2].getEncoded());
                    } catch (CertificateException ce) {
                        throw new IOException(ce);
                    }
                }
            }
            signedData.putOrderedSetOf((byte) -96, implCerts);
        }
        if (!(this.crls == null || this.crls.length == 0)) {
            Set<X509CRLImpl> implCRLs = new HashSet(this.crls.length);
            X509CRL[] x509crlArr = this.crls;
            int length = x509crlArr.length;
            while (i < length) {
                X509CRL crl = x509crlArr[i];
                if (crl instanceof X509CRLImpl) {
                    implCRLs.add((X509CRLImpl) crl);
                } else {
                    try {
                        implCRLs.add(new X509CRLImpl(crl.getEncoded()));
                    } catch (CRLException ce2) {
                        throw new IOException(ce2);
                    }
                }
                i++;
            }
            signedData.putOrderedSetOf((byte) -95, (DerEncoder[]) implCRLs.toArray(new X509CRLImpl[implCRLs.size()]));
        }
        signedData.putOrderedSetOf((byte) 49, this.signerInfos);
        new ContentInfo(ContentInfo.SIGNED_DATA_OID, new DerValue((byte) 48, signedData.toByteArray())).encode(out);
    }

    public SignerInfo verify(SignerInfo info, byte[] bytes) throws NoSuchAlgorithmException, SignatureException {
        return info.verify(this, bytes);
    }

    public SignerInfo verify(SignerInfo info, InputStream dataInputStream) throws NoSuchAlgorithmException, SignatureException, IOException {
        return info.verify(this, dataInputStream);
    }

    public SignerInfo[] verify(byte[] bytes) throws NoSuchAlgorithmException, SignatureException {
        Vector<SignerInfo> intResult = new Vector();
        for (SignerInfo signerInfo : this.signerInfos) {
            SignerInfo signerInfo2 = verify(signerInfo2, bytes);
            if (signerInfo2 != null) {
                intResult.addElement(signerInfo2);
            }
        }
        if (intResult.isEmpty()) {
            return null;
        }
        SignerInfo[] result = new SignerInfo[intResult.size()];
        intResult.copyInto(result);
        return result;
    }

    public SignerInfo[] verify() throws NoSuchAlgorithmException, SignatureException {
        return verify(null);
    }

    public BigInteger getVersion() {
        return this.version;
    }

    public AlgorithmId[] getDigestAlgorithmIds() {
        return this.digestAlgorithmIds;
    }

    public ContentInfo getContentInfo() {
        return this.contentInfo;
    }

    public X509Certificate[] getCertificates() {
        if (this.certificates != null) {
            return (X509Certificate[]) this.certificates.clone();
        }
        return null;
    }

    public X509CRL[] getCRLs() {
        if (this.crls != null) {
            return (X509CRL[]) this.crls.clone();
        }
        return null;
    }

    public SignerInfo[] getSignerInfos() {
        return this.signerInfos;
    }

    public X509Certificate getCertificate(BigInteger serial, X500Name issuerName) {
        if (this.certificates != null) {
            if (this.certIssuerNames == null) {
                populateCertIssuerNames();
            }
            int i = 0;
            while (i < this.certificates.length) {
                X509Certificate cert = this.certificates[i];
                if (serial.equals(cert.getSerialNumber()) && issuerName.equals(this.certIssuerNames[i])) {
                    return cert;
                }
                i++;
            }
        }
        return null;
    }

    private void populateCertIssuerNames() {
        if (this.certificates != null) {
            this.certIssuerNames = new Principal[this.certificates.length];
            for (int i = 0; i < this.certificates.length; i++) {
                X509Certificate cert = this.certificates[i];
                Principal certIssuerName = cert.getIssuerDN();
                if (!(certIssuerName instanceof X500Name)) {
                    try {
                        certIssuerName = (Principal) new X509CertInfo(cert.getTBSCertificate()).get("issuer.dname");
                    } catch (Exception e) {
                    }
                }
                this.certIssuerNames[i] = certIssuerName;
            }
        }
    }

    public String toString() {
        String out;
        int i;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("");
        stringBuilder2.append(this.contentInfo);
        stringBuilder2.append("\n");
        String out2 = stringBuilder2.toString();
        if (this.version != null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(out2);
            stringBuilder2.append("PKCS7 :: version: ");
            stringBuilder2.append(Debug.toHexString(this.version));
            stringBuilder2.append("\n");
            out2 = stringBuilder2.toString();
        }
        int i2 = 0;
        if (this.digestAlgorithmIds != null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(out2);
            stringBuilder2.append("PKCS7 :: digest AlgorithmIds: \n");
            out = stringBuilder2.toString();
            for (Object append : this.digestAlgorithmIds) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(out);
                stringBuilder.append("\t");
                stringBuilder.append(append);
                stringBuilder.append("\n");
                out = stringBuilder.toString();
            }
            out2 = out;
        }
        if (this.certificates != null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(out2);
            stringBuilder2.append("PKCS7 :: certificates: \n");
            out = stringBuilder2.toString();
            for (i = 0; i < this.certificates.length; i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(out);
                stringBuilder.append("\t");
                stringBuilder.append(i);
                stringBuilder.append(".   ");
                stringBuilder.append(this.certificates[i]);
                stringBuilder.append("\n");
                out = stringBuilder.toString();
            }
            out2 = out;
        }
        if (this.crls != null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(out2);
            stringBuilder2.append("PKCS7 :: crls: \n");
            out = stringBuilder2.toString();
            for (i = 0; i < this.crls.length; i++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(out);
                stringBuilder.append("\t");
                stringBuilder.append(i);
                stringBuilder.append(".   ");
                stringBuilder.append(this.crls[i]);
                stringBuilder.append("\n");
                out = stringBuilder.toString();
            }
            out2 = out;
        }
        if (this.signerInfos != null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(out2);
            stringBuilder2.append("PKCS7 :: signer infos: \n");
            out2 = stringBuilder2.toString();
            while (true) {
                int i3 = i2;
                if (i3 >= this.signerInfos.length) {
                    break;
                }
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(out2);
                stringBuilder3.append("\t");
                stringBuilder3.append(i3);
                stringBuilder3.append(".  ");
                stringBuilder3.append(this.signerInfos[i3]);
                stringBuilder3.append("\n");
                out2 = stringBuilder3.toString();
                i2 = i3 + 1;
            }
        }
        return out2;
    }

    public boolean isOldStyle() {
        return this.oldStyle;
    }
}
