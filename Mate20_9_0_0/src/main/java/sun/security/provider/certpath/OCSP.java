package sun.security.provider.certpath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
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
            if (responderURI != null) {
                CertId certId = new CertId(issuerCert, certImpl.getSerialNumberObject());
                return check(Collections.singletonList(certId), responderURI, issuerCert, null, null, Collections.emptyList()).getSingleResponse(certId);
            }
            throw new CertPathValidatorException("No OCSP Responder URI in certificate");
        } catch (IOException | CertificateException e) {
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
        } catch (IOException | CertificateException e) {
            throw new CertPathValidatorException("Exception while encoding OCSPRequest", e);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:69:0x0124 A:{SYNTHETIC, Splitter:B:69:0x0124} */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x012d A:{SYNTHETIC, Splitter:B:75:0x012d} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static OCSPResponse check(List<CertId> certIds, URI responderURI, X509Certificate issuerCert, X509Certificate responderCert, Date date, List<Extension> extensions) throws IOException, CertPathValidatorException {
        IOException iOException;
        IOException ioe;
        IOException ioe2;
        IOException iOException2;
        OCSPRequest request = null;
        List<CertId> list;
        try {
            list = certIds;
            try {
                request = new OCSPRequest(list, extensions);
                byte[] bytes = request.encodeBytes();
                InputStream in = null;
                OutputStream out = null;
                byte[] response = null;
                try {
                    StringBuilder stringBuilder;
                    Object url = responderURI.toURL();
                    if (debug != null) {
                        Debug debug = debug;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("connecting to OCSP service at: ");
                        stringBuilder.append(url);
                        debug.println(stringBuilder.toString());
                    }
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setConnectTimeout(CONNECT_TIMEOUT);
                    con.setReadTimeout(CONNECT_TIMEOUT);
                    con.setDoOutput(true);
                    con.setDoInput(true);
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-type", "application/ocsp-request");
                    con.setRequestProperty("Content-length", String.valueOf(bytes.length));
                    OutputStream out2 = con.getOutputStream();
                    try {
                        out2.write(bytes);
                        out2.flush();
                        if (!(debug == null || con.getResponseCode() == HttpURLConnection.HTTP_OK)) {
                            Debug debug2 = debug;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Received HTTP error: ");
                            stringBuilder.append(con.getResponseCode());
                            stringBuilder.append(" - ");
                            stringBuilder.append(con.getResponseMessage());
                            debug2.println(stringBuilder.toString());
                        }
                        in = con.getInputStream();
                        int contentLength = con.getContentLength();
                        if (contentLength == -1) {
                            contentLength = Integer.MAX_VALUE;
                        }
                        int i = 2048;
                        if (contentLength <= 2048) {
                            i = contentLength;
                        }
                        response = new byte[i];
                        i = 0;
                        while (i < contentLength) {
                            int count = in.read(response, i, response.length - i);
                            if (count < 0) {
                                break;
                            }
                            i += count;
                            if (i >= response.length && i < contentLength) {
                                response = Arrays.copyOf(response, i * 2);
                            }
                        }
                        byte[] response2 = Arrays.copyOf(response, i);
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException ioe3) {
                                iOException = ioe3;
                                throw ioe3;
                            }
                        }
                        if (out2 != null) {
                            try {
                                out2.close();
                            } catch (IOException ioe32) {
                                iOException = ioe32;
                                throw ioe32;
                            }
                        }
                        OCSPResponse ocspResponse = null;
                        try {
                            OCSPResponse ocspResponse2 = new OCSPResponse(response2);
                            ocspResponse2.verify(list, issuerCert, responderCert, date, request.getNonce());
                            return ocspResponse2;
                        } catch (IOException ioe4) {
                            throw new CertPathValidatorException(ioe4);
                        }
                    } catch (IOException ioe322) {
                        ioe2 = ioe322;
                        out = out2;
                        try {
                            throw new CertPathValidatorException("Unable to determine revocation status due to network error", ioe2, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                        } catch (Throwable th) {
                            ioe322 = th;
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (IOException ioe3222) {
                                    iOException2 = ioe3222;
                                    throw ioe3222;
                                }
                            }
                            if (out != null) {
                                try {
                                    out.close();
                                } catch (IOException ioe32222) {
                                    iOException2 = ioe32222;
                                    throw ioe32222;
                                }
                            }
                            throw ioe32222;
                        }
                    } catch (Throwable th2) {
                        ioe32222 = th2;
                        out = out2;
                        if (in != null) {
                        }
                        if (out != null) {
                        }
                        throw ioe32222;
                    }
                } catch (IOException ioe322222) {
                    ioe2 = ioe322222;
                    throw new CertPathValidatorException("Unable to determine revocation status due to network error", ioe2, null, -1, BasicReason.UNDETERMINED_REVOCATION_STATUS);
                }
            } catch (IOException e) {
                ioe322222 = e;
                throw new CertPathValidatorException("Exception while encoding OCSPRequest", ioe322222);
            }
        } catch (IOException e2) {
            ioe322222 = e2;
            list = certIds;
            List<Extension> list2 = extensions;
            throw new CertPathValidatorException("Exception while encoding OCSPRequest", ioe322222);
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
