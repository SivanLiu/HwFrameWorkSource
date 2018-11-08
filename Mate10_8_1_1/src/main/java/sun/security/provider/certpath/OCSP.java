package sun.security.provider.certpath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.cert.CRLReason;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.cert.CertificateException;
import java.security.cert.Extension;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import sun.security.action.GetIntegerAction;
import sun.security.util.Debug;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AccessDescription;
import sun.security.x509.AuthorityInfoAccessExtension;
import sun.security.x509.GeneralName;
import sun.security.x509.URIName;
import sun.security.x509.X509CertImpl;

public final class OCSP {
    private static final int CONNECT_TIMEOUT = initializeTimeout();
    private static final int DEFAULT_CONNECT_TIMEOUT = 15000;
    static final ObjectIdentifier NONCE_EXTENSION_OID = ObjectIdentifier.newInternal(new int[]{1, 3, 6, 1, 5, 5, 7, 48, 1, 2});
    private static final Debug debug = Debug.getInstance("certpath");

    public interface RevocationStatus {

        public enum CertStatus {
            GOOD,
            REVOKED,
            UNKNOWN
        }

        CertStatus getCertStatus();

        CRLReason getRevocationReason();

        Date getRevocationTime();

        Map<String, Extension> getSingleExtensions();
    }

    private static int initializeTimeout() {
        Integer tmp = (Integer) AccessController.doPrivileged(new GetIntegerAction("com.sun.security.ocsp.timeout"));
        if (tmp == null || tmp.intValue() < 0) {
            return DEFAULT_CONNECT_TIMEOUT;
        }
        return tmp.intValue() * 1000;
    }

    private OCSP() {
    }

    public static RevocationStatus check(X509Certificate cert, X509Certificate issuerCert) throws IOException, CertPathValidatorException {
        try {
            X509CertImpl certImpl = X509CertImpl.toImpl(cert);
            URI responderURI = getResponderURI(certImpl);
            if (responderURI == null) {
                throw new CertPathValidatorException("No OCSP Responder URI in certificate");
            }
            CertId certId = new CertId(issuerCert, certImpl.getSerialNumberObject());
            return check(Collections.singletonList(certId), responderURI, issuerCert, null, null, Collections.emptyList()).getSingleResponse(certId);
        } catch (Exception e) {
            throw new CertPathValidatorException("Exception while encoding OCSPRequest", e);
        }
    }

    public static RevocationStatus check(X509Certificate cert, X509Certificate issuerCert, URI responderURI, X509Certificate responderCert, Date date) throws IOException, CertPathValidatorException {
        return check(cert, issuerCert, responderURI, responderCert, date, Collections.emptyList());
    }

    public static RevocationStatus check(X509Certificate cert, X509Certificate issuerCert, URI responderURI, X509Certificate responderCert, Date date, List<Extension> extensions) throws IOException, CertPathValidatorException {
        try {
            CertId certId = new CertId(issuerCert, X509CertImpl.toImpl(cert).getSerialNumberObject());
            return check(Collections.singletonList(certId), responderURI, issuerCert, responderCert, date, (List) extensions).getSingleResponse(certId);
        } catch (Exception e) {
            throw new CertPathValidatorException("Exception while encoding OCSPRequest", e);
        }
    }

    static OCSPResponse check(List<CertId> certIds, URI responderURI, X509Certificate issuerCert, X509Certificate responderCert, Date date, List<Extension> extensions) throws IOException, CertPathValidatorException {
        IOException ioe;
        try {
            OCSPRequest oCSPRequest = new OCSPRequest(certIds, extensions);
            try {
                byte[] bytes = oCSPRequest.encodeBytes();
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    int i;
                    URL url = responderURI.toURL();
                    if (debug != null) {
                        debug.println("connecting to OCSP service at: " + url);
                    }
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(CONNECT_TIMEOUT);
                    con.setReadTimeout(CONNECT_TIMEOUT);
                    con.setDoOutput(true);
                    con.setDoInput(true);
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-type", "application/ocsp-request");
                    con.setRequestProperty("Content-length", String.valueOf(bytes.length));
                    outputStream = con.getOutputStream();
                    outputStream.write(bytes);
                    outputStream.flush();
                    if (!(debug == null || con.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                        debug.println("Received HTTP error: " + con.getResponseCode() + " - " + con.getResponseMessage());
                    }
                    inputStream = con.getInputStream();
                    int contentLength = con.getContentLength();
                    if (contentLength == -1) {
                        contentLength = Integer.MAX_VALUE;
                    }
                    if (contentLength > 2048) {
                        i = 2048;
                    } else {
                        i = contentLength;
                    }
                    byte[] response = new byte[i];
                    int total = 0;
                    while (total < contentLength) {
                        int count = inputStream.read(response, total, response.length - total);
                        if (count < 0) {
                            break;
                        }
                        total += count;
                        if (total >= response.length && total < contentLength) {
                            response = Arrays.copyOf(response, total * 2);
                        }
                    }
                    response = Arrays.copyOf(response, total);
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ioe2) {
                            throw ioe2;
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException ioe22) {
                            throw ioe22;
                        }
                    }
                    try {
                        OCSPResponse ocspResponse = new OCSPResponse(response);
                        ocspResponse.verify(certIds, issuerCert, responderCert, date, oCSPRequest.getNonce());
                        return ocspResponse;
                    } catch (Throwable ioe3) {
                        throw new CertPathValidatorException(ioe3);
                    }
                } catch (IOException ioe222) {
                    throw new CertPathValidatorException("Unable to determine revocation status due to network error", ioe222, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                } catch (Throwable th) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ioe2222) {
                            throw ioe2222;
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException ioe22222) {
                            throw ioe22222;
                        }
                    }
                }
            } catch (IOException e) {
                ioe22222 = e;
                OCSPRequest request = oCSPRequest;
                throw new CertPathValidatorException("Exception while encoding OCSPRequest", ioe22222);
            }
        } catch (IOException e2) {
            ioe22222 = e2;
            throw new CertPathValidatorException("Exception while encoding OCSPRequest", ioe22222);
        }
    }

    public static URI getResponderURI(X509Certificate cert) {
        try {
            return getResponderURI(X509CertImpl.toImpl(cert));
        } catch (CertificateException e) {
            return null;
        }
    }

    static URI getResponderURI(X509CertImpl certImpl) {
        AuthorityInfoAccessExtension aia = certImpl.getAuthorityInfoAccessExtension();
        if (aia == null) {
            return null;
        }
        for (AccessDescription description : aia.getAccessDescriptions()) {
            if (description.getAccessMethod().equals(AccessDescription.Ad_OCSP_Id)) {
                GeneralName generalName = description.getAccessLocation();
                if (generalName.getType() == 6) {
                    return ((URIName) generalName.getName()).getURI();
                }
            }
        }
        return null;
    }
}
