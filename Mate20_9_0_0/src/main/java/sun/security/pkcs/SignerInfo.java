package sun.security.pkcs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.CryptoPrimitive;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.Timestamp;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import sun.misc.HexDumpEncoder;
import sun.security.timestamp.TimestampToken;
import sun.security.util.Debug;
import sun.security.util.DerEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.DisabledAlgorithmConstraints;
import sun.security.util.KeyUtil;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;
import sun.security.x509.KeyUsageExtension;
import sun.security.x509.X500Name;

public class SignerInfo implements DerEncoder {
    private static final Set<CryptoPrimitive> DIGEST_PRIMITIVE_SET = Collections.unmodifiableSet(EnumSet.of(CryptoPrimitive.MESSAGE_DIGEST));
    private static final DisabledAlgorithmConstraints JAR_DISABLED_CHECK = new DisabledAlgorithmConstraints(DisabledAlgorithmConstraints.PROPERTY_JAR_DISABLED_ALGS);
    private static final Set<CryptoPrimitive> SIG_PRIMITIVE_SET = Collections.unmodifiableSet(EnumSet.of(CryptoPrimitive.SIGNATURE));
    PKCS9Attributes authenticatedAttributes;
    BigInteger certificateSerialNumber;
    AlgorithmId digestAlgorithmId;
    AlgorithmId digestEncryptionAlgorithmId;
    byte[] encryptedDigest;
    private boolean hasTimestamp;
    X500Name issuerName;
    Timestamp timestamp;
    PKCS9Attributes unauthenticatedAttributes;
    BigInteger version;

    public SignerInfo(X500Name issuerName, BigInteger serial, AlgorithmId digestAlgorithmId, AlgorithmId digestEncryptionAlgorithmId, byte[] encryptedDigest) {
        this.hasTimestamp = true;
        this.version = BigInteger.ONE;
        this.issuerName = issuerName;
        this.certificateSerialNumber = serial;
        this.digestAlgorithmId = digestAlgorithmId;
        this.digestEncryptionAlgorithmId = digestEncryptionAlgorithmId;
        this.encryptedDigest = encryptedDigest;
    }

    public SignerInfo(X500Name issuerName, BigInteger serial, AlgorithmId digestAlgorithmId, PKCS9Attributes authenticatedAttributes, AlgorithmId digestEncryptionAlgorithmId, byte[] encryptedDigest, PKCS9Attributes unauthenticatedAttributes) {
        this.hasTimestamp = true;
        this.version = BigInteger.ONE;
        this.issuerName = issuerName;
        this.certificateSerialNumber = serial;
        this.digestAlgorithmId = digestAlgorithmId;
        this.authenticatedAttributes = authenticatedAttributes;
        this.digestEncryptionAlgorithmId = digestEncryptionAlgorithmId;
        this.encryptedDigest = encryptedDigest;
        this.unauthenticatedAttributes = unauthenticatedAttributes;
    }

    public SignerInfo(DerInputStream derin) throws IOException, ParsingException {
        this(derin, false);
    }

    public SignerInfo(DerInputStream derin, boolean oldStyle) throws IOException, ParsingException {
        this.hasTimestamp = true;
        this.version = derin.getBigInteger();
        DerValue[] issuerAndSerialNumber = derin.getSequence(2);
        this.issuerName = new X500Name(new DerValue((byte) 48, issuerAndSerialNumber[0].toByteArray()));
        this.certificateSerialNumber = issuerAndSerialNumber[1].getBigInteger();
        this.digestAlgorithmId = AlgorithmId.parse(derin.getDerValue());
        if (oldStyle) {
            derin.getSet(0);
        } else if (((byte) derin.peekByte()) == (byte) -96) {
            this.authenticatedAttributes = new PKCS9Attributes(derin);
        }
        this.digestEncryptionAlgorithmId = AlgorithmId.parse(derin.getDerValue());
        this.encryptedDigest = derin.getOctetString();
        if (oldStyle) {
            derin.getSet(0);
        } else if (derin.available() != 0 && ((byte) derin.peekByte()) == (byte) -95) {
            this.unauthenticatedAttributes = new PKCS9Attributes(derin, true);
        }
        if (derin.available() != 0) {
            throw new ParsingException("extra data at the end");
        }
    }

    public void encode(DerOutputStream out) throws IOException {
        derEncode(out);
    }

    public void derEncode(OutputStream out) throws IOException {
        DerOutputStream seq = new DerOutputStream();
        seq.putInteger(this.version);
        DerOutputStream issuerAndSerialNumber = new DerOutputStream();
        this.issuerName.encode(issuerAndSerialNumber);
        issuerAndSerialNumber.putInteger(this.certificateSerialNumber);
        seq.write((byte) 48, issuerAndSerialNumber);
        this.digestAlgorithmId.encode(seq);
        if (this.authenticatedAttributes != null) {
            this.authenticatedAttributes.encode((byte) -96, seq);
        }
        this.digestEncryptionAlgorithmId.encode(seq);
        seq.putOctetString(this.encryptedDigest);
        if (this.unauthenticatedAttributes != null) {
            this.unauthenticatedAttributes.encode((byte) -95, seq);
        }
        DerOutputStream tmp = new DerOutputStream();
        tmp.write((byte) 48, seq);
        out.write(tmp.toByteArray());
    }

    public X509Certificate getCertificate(PKCS7 block) throws IOException {
        return block.getCertificate(this.certificateSerialNumber, this.issuerName);
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0071 A:{LOOP_END, LOOP:0: B:9:0x002e->B:22:0x0071} */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0070 A:{SYNTHETIC} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public ArrayList<X509Certificate> getCertificateChain(PKCS7 block) throws IOException {
        X509Certificate userCert = block.getCertificate(this.certificateSerialNumber, this.issuerName);
        if (userCert == null) {
            return null;
        }
        ArrayList<X509Certificate> certList = new ArrayList();
        certList.add(userCert);
        X509Certificate[] pkcsCerts = block.getCertificates();
        if (pkcsCerts == null || userCert.getSubjectDN().equals(userCert.getIssuerDN())) {
            return certList;
        }
        Principal issuer = userCert.getIssuerDN();
        int start = 0;
        while (true) {
            boolean match = false;
            int i = start;
            while (i < pkcsCerts.length) {
                if (issuer.equals(pkcsCerts[i].getSubjectDN())) {
                    certList.add(pkcsCerts[i]);
                    if (pkcsCerts[i].getSubjectDN().equals(pkcsCerts[i].getIssuerDN())) {
                        start = pkcsCerts.length;
                    } else {
                        issuer = pkcsCerts[i].getIssuerDN();
                        X509Certificate tmpCert = pkcsCerts[start];
                        pkcsCerts[start] = pkcsCerts[i];
                        pkcsCerts[i] = tmpCert;
                        start++;
                    }
                    match = true;
                    if (match) {
                        return certList;
                    }
                } else {
                    i++;
                }
            }
            if (match) {
            }
        }
    }

    SignerInfo verify(PKCS7 block, byte[] data) throws NoSuchAlgorithmException, SignatureException {
        try {
            return verify(block, new ByteArrayInputStream(data));
        } catch (IOException e) {
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:82:0x0184 A:{Splitter:B:7:0x0016, ExcHandler: InvalidKeyException (e java.security.InvalidKeyException)} */
    /* JADX WARNING: Missing block: B:82:0x0184, code skipped:
            r0 = e;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    SignerInfo verify(PKCS7 block, InputStream inputStream) throws NoSuchAlgorithmException, SignatureException, IOException {
        IOException e;
        StringBuilder stringBuilder;
        InputStream inputStream2;
        try {
            ContentInfo content = block.getContentInfo();
            if (inputStream == null) {
                inputStream2 = new ByteArrayInputStream(content.getContentBytes());
            } else {
                inputStream2 = inputStream;
            }
            try {
                InputStream dataSigned;
                byte[] messageDigest;
                String digestAlgname = getDigestAlgorithmId().getName();
                if (this.authenticatedAttributes == null) {
                    dataSigned = inputStream2;
                } else {
                    ObjectIdentifier contentType = (ObjectIdentifier) this.authenticatedAttributes.getAttributeValue(PKCS9Attribute.CONTENT_TYPE_OID);
                    if (contentType != null) {
                        if (contentType.equals(content.contentType)) {
                            messageDigest = (byte[]) this.authenticatedAttributes.getAttributeValue(PKCS9Attribute.MESSAGE_DIGEST_OID);
                            if (messageDigest == null) {
                                return null;
                            }
                            if (JAR_DISABLED_CHECK.permits(DIGEST_PRIMITIVE_SET, digestAlgname, null)) {
                                MessageDigest md = MessageDigest.getInstance(digestAlgname);
                                byte[] buffer = new byte[4096];
                                int read = 0;
                                while (true) {
                                    int read2 = inputStream2.read(buffer);
                                    read = read2;
                                    if (read2 == -1) {
                                        break;
                                    }
                                    md.update(buffer, 0, read);
                                }
                                byte[] computedMessageDigest = md.digest();
                                if (messageDigest.length != computedMessageDigest.length) {
                                    return null;
                                }
                                for (int i = 0; i < messageDigest.length; i++) {
                                    if (messageDigest[i] != computedMessageDigest[i]) {
                                        return null;
                                    }
                                }
                                dataSigned = new ByteArrayInputStream(this.authenticatedAttributes.getDerEncoding());
                            } else {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Digest check failed. Disabled algorithm used: ");
                                stringBuilder2.append(digestAlgname);
                                throw new SignatureException(stringBuilder2.toString());
                            }
                        }
                    }
                    return null;
                }
                InputStream dataSigned2 = dataSigned;
                String encryptionAlgname = getDigestEncryptionAlgorithmId().getName();
                messageDigest = AlgorithmId.getEncAlgFromSigAlg(encryptionAlgname);
                if (messageDigest != null) {
                    encryptionAlgname = messageDigest;
                }
                String algname = AlgorithmId.makeSigAlg(digestAlgname, encryptionAlgname);
                StringBuilder stringBuilder3;
                if (JAR_DISABLED_CHECK.permits(SIG_PRIMITIVE_SET, algname, null)) {
                    X509Certificate cert = getCertificate(block);
                    PublicKey key = cert.getPublicKey();
                    if (cert == null) {
                        return null;
                    }
                    if (!JAR_DISABLED_CHECK.permits(SIG_PRIMITIVE_SET, (Key) key)) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Public key check failed. Disabled key used: ");
                        stringBuilder3.append(KeyUtil.getKeySize(key));
                        stringBuilder3.append(" bit ");
                        stringBuilder3.append(key.getAlgorithm());
                        throw new SignatureException(stringBuilder3.toString());
                    } else if (cert.hasUnsupportedCriticalExtension()) {
                        throw new SignatureException("Certificate has unsupported critical extension(s)");
                    } else {
                        boolean[] keyUsageBits = cert.getKeyUsage();
                        if (keyUsageBits != null) {
                            KeyUsageExtension keyUsage = new KeyUsageExtension(keyUsageBits);
                            boolean digSigAllowed = keyUsage.get(KeyUsageExtension.DIGITAL_SIGNATURE).booleanValue();
                            boolean nonRepuAllowed = keyUsage.get(KeyUsageExtension.NON_REPUDIATION).booleanValue();
                            if (!digSigAllowed) {
                                if (!nonRepuAllowed) {
                                    throw new SignatureException("Key usage restricted: cannot be used for digital signatures");
                                }
                            }
                        }
                        Signature sig = Signature.getInstance(algname);
                        sig.initVerify(key);
                        byte[] buffer2 = new byte[4096];
                        while (true) {
                            int read3 = dataSigned2.read(buffer2);
                            int read4 = read3;
                            if (read3 == -1) {
                                break;
                            }
                            sig.update(buffer2, 0, read4);
                        }
                        if (sig.verify(this.encryptedDigest)) {
                            return this;
                        }
                        return null;
                    }
                }
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Signature check failed. Disabled algorithm used: ");
                stringBuilder3.append(algname);
                throw new SignatureException(stringBuilder3.toString());
            } catch (IOException e2) {
                throw new SignatureException("Failed to parse keyUsage extension");
            } catch (InvalidKeyException e3) {
            } catch (IOException e4) {
                e = e4;
                stringBuilder = new StringBuilder();
                stringBuilder.append("IO error verifying signature:\n");
                stringBuilder.append(e.getMessage());
                throw new SignatureException(stringBuilder.toString());
            }
        } catch (IOException e5) {
            e = e5;
            inputStream2 = inputStream;
            stringBuilder = new StringBuilder();
            stringBuilder.append("IO error verifying signature:\n");
            stringBuilder.append(e.getMessage());
            throw new SignatureException(stringBuilder.toString());
        } catch (InvalidKeyException e6) {
            InvalidKeyException e7 = e6;
            inputStream2 = inputStream;
            stringBuilder = new StringBuilder();
            stringBuilder.append("InvalidKey: ");
            stringBuilder.append(e7.getMessage());
            throw new SignatureException(stringBuilder.toString());
        }
    }

    SignerInfo verify(PKCS7 block) throws NoSuchAlgorithmException, SignatureException {
        return verify(block, (byte[]) null);
    }

    public BigInteger getVersion() {
        return this.version;
    }

    public X500Name getIssuerName() {
        return this.issuerName;
    }

    public BigInteger getCertificateSerialNumber() {
        return this.certificateSerialNumber;
    }

    public AlgorithmId getDigestAlgorithmId() {
        return this.digestAlgorithmId;
    }

    public PKCS9Attributes getAuthenticatedAttributes() {
        return this.authenticatedAttributes;
    }

    public AlgorithmId getDigestEncryptionAlgorithmId() {
        return this.digestEncryptionAlgorithmId;
    }

    public byte[] getEncryptedDigest() {
        return this.encryptedDigest;
    }

    public PKCS9Attributes getUnauthenticatedAttributes() {
        return this.unauthenticatedAttributes;
    }

    public PKCS7 getTsToken() throws IOException {
        if (this.unauthenticatedAttributes == null) {
            return null;
        }
        PKCS9Attribute tsTokenAttr = this.unauthenticatedAttributes.getAttribute(PKCS9Attribute.SIGNATURE_TIMESTAMP_TOKEN_OID);
        if (tsTokenAttr == null) {
            return null;
        }
        return new PKCS7((byte[]) tsTokenAttr.getValue());
    }

    public Timestamp getTimestamp() throws IOException, NoSuchAlgorithmException, SignatureException, CertificateException {
        if (this.timestamp != null || !this.hasTimestamp) {
            return this.timestamp;
        }
        PKCS7 tsToken = getTsToken();
        if (tsToken == null) {
            this.hasTimestamp = false;
            return null;
        }
        byte[] encTsTokenInfo = tsToken.getContentInfo().getData();
        CertPath tsaChain = CertificateFactory.getInstance("X.509").generateCertPath(tsToken.verify(encTsTokenInfo)[0].getCertificateChain(tsToken));
        TimestampToken tsTokenInfo = new TimestampToken(encTsTokenInfo);
        verifyTimestamp(tsTokenInfo);
        this.timestamp = new Timestamp(tsTokenInfo.getDate(), tsaChain);
        return this.timestamp;
    }

    private void verifyTimestamp(TimestampToken token) throws NoSuchAlgorithmException, SignatureException {
        String digestAlgname = token.getHashAlgorithm().getName();
        if (JAR_DISABLED_CHECK.permits(DIGEST_PRIMITIVE_SET, digestAlgname, null)) {
            if (!Arrays.equals(token.getHashedMessage(), MessageDigest.getInstance(digestAlgname).digest(this.encryptedDigest))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Signature timestamp (#");
                stringBuilder.append(token.getSerialNumber());
                stringBuilder.append(") generated on ");
                stringBuilder.append(token.getDate());
                stringBuilder.append(" is inapplicable");
                throw new SignatureException(stringBuilder.toString());
            }
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Timestamp token digest check failed. Disabled algorithm used: ");
        stringBuilder2.append(digestAlgname);
        throw new SignatureException(stringBuilder2.toString());
    }

    public String toString() {
        HexDumpEncoder hexDump = new HexDumpEncoder();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("");
        stringBuilder.append("Signer Info for (issuer): ");
        stringBuilder.append(this.issuerName);
        stringBuilder.append("\n");
        String out = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(out);
        stringBuilder.append("\tversion: ");
        stringBuilder.append(Debug.toHexString(this.version));
        stringBuilder.append("\n");
        out = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(out);
        stringBuilder.append("\tcertificateSerialNumber: ");
        stringBuilder.append(Debug.toHexString(this.certificateSerialNumber));
        stringBuilder.append("\n");
        out = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(out);
        stringBuilder.append("\tdigestAlgorithmId: ");
        stringBuilder.append(this.digestAlgorithmId);
        stringBuilder.append("\n");
        out = stringBuilder.toString();
        if (this.authenticatedAttributes != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(out);
            stringBuilder.append("\tauthenticatedAttributes: ");
            stringBuilder.append(this.authenticatedAttributes);
            stringBuilder.append("\n");
            out = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(out);
        stringBuilder.append("\tdigestEncryptionAlgorithmId: ");
        stringBuilder.append(this.digestEncryptionAlgorithmId);
        stringBuilder.append("\n");
        out = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(out);
        stringBuilder.append("\tencryptedDigest: \n");
        stringBuilder.append(hexDump.encodeBuffer(this.encryptedDigest));
        stringBuilder.append("\n");
        out = stringBuilder.toString();
        if (this.unauthenticatedAttributes == null) {
            return out;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(out);
        stringBuilder.append("\tunauthenticatedAttributes: ");
        stringBuilder.append(this.unauthenticatedAttributes);
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
