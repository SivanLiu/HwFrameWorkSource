package com.android.server.locksettings.recoverablekeystore.certificate;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class CertUtils {
    private static final String CERT_FORMAT = "X.509";
    private static final String CERT_PATH_ALG = "PKIX";
    private static final String CERT_STORE_ALG = "Collection";
    static final int MUST_EXIST_AT_LEAST_ONE = 2;
    static final int MUST_EXIST_EXACTLY_ONE = 1;
    static final int MUST_EXIST_UNENFORCED = 0;
    private static final String SIGNATURE_ALG = "SHA256withRSA";

    @Retention(RetentionPolicy.SOURCE)
    @interface MustExist {
    }

    private CertUtils() {
    }

    static X509Certificate decodeCert(byte[] certBytes) throws CertParsingException {
        return decodeCert(new ByteArrayInputStream(certBytes));
    }

    static X509Certificate decodeCert(InputStream inStream) throws CertParsingException {
        try {
            try {
                return (X509Certificate) CertificateFactory.getInstance(CERT_FORMAT).generateCertificate(inStream);
            } catch (Exception e) {
                throw new CertParsingException(e);
            }
        } catch (CertificateException e2) {
            throw new RuntimeException(e2);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x001d A:{Splitter: B:0:0x0000, ExcHandler: org.xml.sax.SAXException (r0_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:3:0x001d A:{Splitter: B:0:0x0000, ExcHandler: org.xml.sax.SAXException (r0_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:3:0x001d, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0023, code:
            throw new com.android.server.locksettings.recoverablekeystore.certificate.CertParsingException(r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static Element getXmlRootNode(byte[] xmlBytes) throws CertParsingException {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));
            document.getDocumentElement().normalize();
            return document.getDocumentElement();
        } catch (Exception e) {
        }
    }

    static List<String> getXmlNodeContents(int mustExist, Element rootNode, String... nodeTags) throws CertParsingException {
        String expression = String.join(SliceAuthority.DELIMITER, nodeTags);
        try {
            NodeList nodeList = (NodeList) XPathFactory.newInstance().newXPath().compile(expression).evaluate(rootNode, XPathConstants.NODESET);
            StringBuilder stringBuilder;
            switch (mustExist) {
                case 0:
                    break;
                case 1:
                    if (nodeList.getLength() != 1) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("The XML file must contain exactly one node with the path ");
                        stringBuilder.append(expression);
                        throw new CertParsingException(stringBuilder.toString());
                    }
                    break;
                case 2:
                    if (nodeList.getLength() == 0) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("The XML file must contain at least one node with the path ");
                        stringBuilder.append(expression);
                        throw new CertParsingException(stringBuilder.toString());
                    }
                    break;
                default:
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("This value of MustExist is not supported: ");
                    stringBuilder.append(mustExist);
                    throw new UnsupportedOperationException(stringBuilder.toString());
            }
            List<String> result = new ArrayList();
            for (int i = 0; i < nodeList.getLength(); i++) {
                result.add(nodeList.item(i).getTextContent().replaceAll("\\s", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
            }
            return result;
        } catch (Exception e) {
            throw new CertParsingException(e);
        }
    }

    public static byte[] decodeBase64(String str) throws CertParsingException {
        try {
            return Base64.getDecoder().decode(str);
        } catch (Exception e) {
            throw new CertParsingException(e);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:8:0x001e A:{Splitter: B:2:0x0008, ExcHandler: java.security.InvalidKeyException (r1_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:8:0x001e, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:10:0x0024, code:
            throw new com.android.server.locksettings.recoverablekeystore.certificate.CertValidationException(r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static void verifyRsaSha256Signature(PublicKey signerPublicKey, byte[] signature, byte[] signedBytes) throws CertValidationException {
        try {
            Signature verifier = Signature.getInstance(SIGNATURE_ALG);
            try {
                verifier.initVerify(signerPublicKey);
                verifier.update(signedBytes);
                if (!verifier.verify(signature)) {
                    throw new CertValidationException("The signature is invalid");
                }
            } catch (Exception e) {
            }
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException(e2);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:6:0x0016 A:{Splitter: B:3:0x0011, ExcHandler: java.security.cert.CertPathValidatorException (r3_0 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:6:0x0016, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:8:0x001c, code:
            throw new com.android.server.locksettings.recoverablekeystore.certificate.CertValidationException(r3);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static CertPath validateCert(Date validationDate, X509Certificate trustedRoot, List<X509Certificate> intermediateCerts, X509Certificate leafCert) throws CertValidationException {
        PKIXParameters pkixParams = buildPkixParams(validationDate, trustedRoot, intermediateCerts, leafCert);
        CertPath certPath = buildCertPath(pkixParams);
        try {
            try {
                CertPathValidator.getInstance(CERT_PATH_ALG).validate(certPath, pkixParams);
                return certPath;
            } catch (Exception e) {
            }
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException(e2);
        }
    }

    public static void validateCertPath(X509Certificate trustedRoot, CertPath certPath) throws CertValidationException {
        validateCertPath(null, trustedRoot, certPath);
    }

    @VisibleForTesting
    static void validateCertPath(Date validationDate, X509Certificate trustedRoot, CertPath certPath) throws CertValidationException {
        if (certPath.getCertificates().isEmpty()) {
            throw new CertValidationException("The given certificate path is empty");
        } else if (certPath.getCertificates().get(0) instanceof X509Certificate) {
            List<X509Certificate> certificates = certPath.getCertificates();
            validateCert(validationDate, trustedRoot, certificates.subList(1, certificates.size()), (X509Certificate) certificates.get(0));
        } else {
            throw new CertValidationException("The given certificate path does not contain X509 certificates");
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:5:0x0011 A:{Splitter: B:2:0x0008, ExcHandler: java.security.cert.CertPathBuilderException (r1_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:5:0x0011, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:7:0x0017, code:
            throw new com.android.server.locksettings.recoverablekeystore.certificate.CertValidationException(r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @VisibleForTesting
    static CertPath buildCertPath(PKIXParameters pkixParams) throws CertValidationException {
        try {
            try {
                return CertPathBuilder.getInstance(CERT_PATH_ALG).build(pkixParams).getCertPath();
            } catch (Exception e) {
            }
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException(e2);
        }
    }

    @VisibleForTesting
    static PKIXParameters buildPkixParams(Date validationDate, X509Certificate trustedRoot, List<X509Certificate> intermediateCerts, X509Certificate leafCert) throws CertValidationException {
        Set<TrustAnchor> trustedAnchors = new HashSet();
        trustedAnchors.add(new TrustAnchor(trustedRoot, null));
        List<X509Certificate> certs = new ArrayList(intermediateCerts);
        certs.add(leafCert);
        try {
            CertStore certStore = CertStore.getInstance(CERT_STORE_ALG, new CollectionCertStoreParameters(certs));
            X509CertSelector certSelector = new X509CertSelector();
            certSelector.setCertificate(leafCert);
            try {
                PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustedAnchors, certSelector);
                pkixParams.addCertStore(certStore);
                pkixParams.setDate(validationDate);
                pkixParams.setRevocationEnabled(false);
                return pkixParams;
            } catch (Exception e) {
                throw new CertValidationException(e);
            }
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException(e2);
        } catch (Exception e3) {
            throw new CertValidationException(e3);
        }
    }
}
