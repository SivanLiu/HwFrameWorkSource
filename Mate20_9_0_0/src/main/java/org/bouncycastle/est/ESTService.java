package org.bouncycastle.est;

import com.huawei.security.hccm.common.connection.HttpConnection.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.est.CsrAttrs;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.DisplayText;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cmc.CMCException;
import org.bouncycastle.cmc.SimplePKIResponse;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;

public class ESTService {
    protected static final String CACERTS = "/cacerts";
    protected static final String CSRATTRS = "/csrattrs";
    protected static final String FULLCMC = "/fullcmc";
    protected static final String SERVERGEN = "/serverkeygen";
    protected static final String SIMPLE_ENROLL = "/simpleenroll";
    protected static final String SIMPLE_REENROLL = "/simplereenroll";
    protected static final Set<String> illegalParts = new HashSet();
    private static final Pattern pathInvalid = Pattern.compile("^[0-9a-zA-Z_\\-.~!$&'()*+,;=]+");
    private final ESTClientProvider clientProvider;
    private final String server;

    static {
        illegalParts.add(CACERTS.substring(1));
        illegalParts.add(SIMPLE_ENROLL.substring(1));
        illegalParts.add(SIMPLE_REENROLL.substring(1));
        illegalParts.add(FULLCMC.substring(1));
        illegalParts.add(SERVERGEN.substring(1));
        illegalParts.add(CSRATTRS.substring(1));
    }

    ESTService(String str, String str2, ESTClientProvider eSTClientProvider) {
        str = verifyServer(str);
        if (str2 != null) {
            str2 = verifyLabel(str2);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("https://");
            stringBuilder.append(str);
            stringBuilder.append("/.well-known/est/");
            stringBuilder.append(str2);
            str = stringBuilder.toString();
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("https://");
            stringBuilder2.append(str);
            stringBuilder2.append("/.well-known/est");
            str = stringBuilder2.toString();
        }
        this.server = str;
        this.clientProvider = eSTClientProvider;
    }

    private String annotateRequest(byte[] bArr) {
        Writer stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        int i = 0;
        do {
            int i2 = i + 48;
            if (i2 < bArr.length) {
                printWriter.print(Base64.toBase64String(bArr, i, 48));
                i = i2;
            } else {
                printWriter.print(Base64.toBase64String(bArr, i, bArr.length - i));
                i = bArr.length;
            }
            printWriter.print(10);
        } while (i < bArr.length);
        printWriter.flush();
        return stringWriter.toString();
    }

    public static X509CertificateHolder[] storeToArray(Store<X509CertificateHolder> store) {
        return storeToArray(store, null);
    }

    public static X509CertificateHolder[] storeToArray(Store<X509CertificateHolder> store, Selector<X509CertificateHolder> selector) {
        Collection matches = store.getMatches(selector);
        return (X509CertificateHolder[]) matches.toArray(new X509CertificateHolder[matches.size()]);
    }

    private String verifyLabel(String str) {
        while (str.endsWith("/") && str.length() > 0) {
            str = str.substring(0, str.length() - 1);
        }
        while (str.startsWith("/") && str.length() > 0) {
            str = str.substring(1);
        }
        StringBuilder stringBuilder;
        if (str.length() == 0) {
            throw new IllegalArgumentException("Label set but after trimming '/' is not zero length string.");
        } else if (!pathInvalid.matcher(str).matches()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Server path ");
            stringBuilder.append(str);
            stringBuilder.append(" contains invalid characters");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (!illegalParts.contains(str)) {
            return str;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Label ");
            stringBuilder.append(str);
            stringBuilder.append(" is a reserved path segment.");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private String verifyServer(String str) {
        StringBuilder stringBuilder;
        while (str.endsWith("/") && str.length() > 0) {
            try {
                str = str.substring(0, str.length() - 1);
            } catch (Throwable e) {
                if (e instanceof IllegalArgumentException) {
                    throw ((IllegalArgumentException) e);
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Scheme and host is invalid: ");
                stringBuilder.append(e.getMessage());
                throw new IllegalArgumentException(stringBuilder.toString(), e);
            }
        }
        if (str.contains("://")) {
            throw new IllegalArgumentException("Server contains scheme, must only be <dnsname/ipaddress>:port, https:// will be added arbitrarily.");
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("https://");
        stringBuilder.append(str);
        URL url = new URL(stringBuilder.toString());
        if (url.getPath().length() == 0 || url.getPath().equals("/")) {
            return str;
        }
        throw new IllegalArgumentException("Server contains path, must only be <dnsname/ipaddress>:port, a path of '/.well-known/est/<label>' will be added arbitrarily.");
    }

    /* JADX WARNING: Removed duplicated region for block: B:59:0x0195 A:{SYNTHETIC, Splitter: B:59:0x0195} */
    /* JADX WARNING: Removed duplicated region for block: B:55:0x0188 A:{Catch:{ all -> 0x0192 }} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0185 A:{Catch:{ all -> 0x0192 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public CACertsResponse getCACerts() throws Exception {
        Throwable th;
        ESTResponse doRequest;
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.server);
            stringBuilder.append(CACERTS);
            URL url = new URL(stringBuilder.toString());
            ESTClient makeClient = this.clientProvider.makeClient();
            ESTRequest build = new ESTRequestBuilder(HttpHeaders.GET, url).withClient(makeClient).build();
            doRequest = makeClient.doRequest(build);
            StringBuilder stringBuilder2;
            try {
                Store store;
                Store store2;
                StringBuilder stringBuilder3;
                Throwable e;
                if (doRequest.getStatusCode() == DisplayText.DISPLAY_TEXT_MAXIMUM_SIZE) {
                    if ("application/pkcs7-mime".equals(doRequest.getHeaders().getFirstValue(HttpHeaders.CONTENT_TYPE))) {
                        Store store3;
                        Store store4;
                        if (doRequest.getContentLength() == null || doRequest.getContentLength().longValue() <= 0) {
                            store3 = null;
                            store4 = store3;
                        } else {
                            SimplePKIResponse simplePKIResponse = new SimplePKIResponse(ContentInfo.getInstance((ASN1Sequence) new ASN1InputStream(doRequest.getInputStream()).readObject()));
                            store3 = simplePKIResponse.getCertificates();
                            store4 = simplePKIResponse.getCRLs();
                        }
                        store = store3;
                        store2 = store4;
                    } else {
                        String stringBuilder4;
                        if (doRequest.getHeaders().getFirstValue(HttpHeaders.CONTENT_TYPE) != null) {
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append(" got ");
                            stringBuilder5.append(doRequest.getHeaders().getFirstValue(HttpHeaders.CONTENT_TYPE));
                            stringBuilder4 = stringBuilder5.toString();
                        } else {
                            stringBuilder4 = " but was not present.";
                        }
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("Response : ");
                        stringBuilder3.append(url.toString());
                        stringBuilder3.append("Expecting application/pkcs7-mime ");
                        stringBuilder3.append(stringBuilder4);
                        throw new ESTException(stringBuilder3.toString(), null, doRequest.getStatusCode(), doRequest.getInputStream());
                    }
                } else if (doRequest.getStatusCode() == 204) {
                    store = null;
                    store2 = store;
                } else {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Get CACerts: ");
                    stringBuilder2.append(url.toString());
                    throw new ESTException(stringBuilder2.toString(), null, doRequest.getStatusCode(), doRequest.getInputStream());
                }
                CACertsResponse cACertsResponse = new CACertsResponse(store, store2, build, doRequest.getSource(), this.clientProvider.isTrusted());
                if (doRequest != null) {
                    try {
                        doRequest.close();
                    } catch (Exception e2) {
                        e = e2;
                    }
                }
                e = null;
                if (e == null) {
                    return cACertsResponse;
                }
                if (e instanceof ESTException) {
                    throw e;
                }
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Get CACerts: ");
                stringBuilder3.append(url.toString());
                throw new ESTException(stringBuilder3.toString(), e, doRequest.getStatusCode(), null);
            } catch (Throwable th2) {
                th = th2;
                try {
                    if (th instanceof ESTException) {
                    }
                } catch (Throwable th3) {
                    th = th3;
                    if (doRequest != null) {
                        try {
                            doRequest.close();
                        } catch (Exception e3) {
                        }
                    }
                    throw th;
                }
            }
        } catch (Throwable th4) {
            doRequest = null;
            th = th4;
            if (doRequest != null) {
            }
            throw th;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x00a2 A:{SYNTHETIC, Splitter: B:22:0x00a2} */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x00c0  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x00ab  */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0117 A:{SYNTHETIC, Splitter: B:53:0x0117} */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x010a A:{Catch:{ all -> 0x0114 }} */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x0107 A:{Catch:{ all -> 0x0114 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public CSRRequestResponse getCSRAttributes() throws ESTException {
        Throwable th;
        if (this.clientProvider.isTrusted()) {
            ESTResponse doRequest;
            Throwable e;
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.server);
                stringBuilder.append(CSRATTRS);
                URL url = new URL(stringBuilder.toString());
                ESTClient makeClient = this.clientProvider.makeClient();
                ESTRequest build = new ESTRequestBuilder(HttpHeaders.GET, url).withClient(makeClient).build();
                doRequest = makeClient.doRequest(build);
                StringBuilder stringBuilder2;
                try {
                    CSRAttributesResponse cSRAttributesResponse;
                    int statusCode = doRequest.getStatusCode();
                    if (statusCode != DisplayText.DISPLAY_TEXT_MAXIMUM_SIZE) {
                        if (!(statusCode == 204 || statusCode == 404)) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("CSR Attribute request: ");
                            stringBuilder2.append(build.getURL().toString());
                            throw new ESTException(stringBuilder2.toString(), null, doRequest.getStatusCode(), doRequest.getInputStream());
                        }
                    } else if (doRequest.getContentLength() != null && doRequest.getContentLength().longValue() > 0) {
                        cSRAttributesResponse = new CSRAttributesResponse(CsrAttrs.getInstance((ASN1Sequence) new ASN1InputStream(doRequest.getInputStream()).readObject()));
                        if (doRequest != null) {
                            try {
                                doRequest.close();
                            } catch (Exception e2) {
                                e = e2;
                            }
                        }
                        e = null;
                        if (e != null) {
                            return new CSRRequestResponse(cSRAttributesResponse, doRequest.getSource());
                        }
                        if (e instanceof ESTException) {
                            throw ((ESTException) e);
                        }
                        throw new ESTException(e.getMessage(), e, doRequest.getStatusCode(), null);
                    }
                    cSRAttributesResponse = null;
                    if (doRequest != null) {
                    }
                    e = null;
                    if (e != null) {
                    }
                } catch (Throwable th2) {
                    th = th2;
                    try {
                        if (th instanceof ESTException) {
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        if (doRequest != null) {
                        }
                        throw th;
                    }
                }
            } catch (Throwable e3) {
                doRequest = null;
                th = e3;
                if (doRequest != null) {
                    try {
                        doRequest.close();
                    } catch (Exception e4) {
                    }
                }
                throw th;
            }
        }
        throw new IllegalStateException("No trust anchors.");
    }

    protected EnrollmentResponse handleEnrollResponse(ESTResponse eSTResponse) throws IOException {
        ESTRequest originalRequest = eSTResponse.getOriginalRequest();
        if (eSTResponse.getStatusCode() == 202) {
            String header = eSTResponse.getHeader("Retry-After");
            if (header != null) {
                long currentTimeMillis;
                try {
                    currentTimeMillis = System.currentTimeMillis() + (Long.parseLong(header) * 1000);
                } catch (NumberFormatException e) {
                    try {
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
                        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                        currentTimeMillis = simpleDateFormat.parse(header).getTime();
                    } catch (Exception e2) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unable to parse Retry-After header:");
                        stringBuilder.append(originalRequest.getURL().toString());
                        stringBuilder.append(" ");
                        stringBuilder.append(e2.getMessage());
                        throw new ESTException(stringBuilder.toString(), null, eSTResponse.getStatusCode(), eSTResponse.getInputStream());
                    }
                }
                return new EnrollmentResponse(null, currentTimeMillis, originalRequest, eSTResponse.getSource());
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Got Status 202 but not Retry-After header from: ");
            stringBuilder2.append(originalRequest.getURL().toString());
            throw new ESTException(stringBuilder2.toString());
        } else if (eSTResponse.getStatusCode() == DisplayText.DISPLAY_TEXT_MAXIMUM_SIZE) {
            try {
                return new EnrollmentResponse(new SimplePKIResponse(ContentInfo.getInstance(new ASN1InputStream(eSTResponse.getInputStream()).readObject())).getCertificates(), -1, null, eSTResponse.getSource());
            } catch (CMCException e3) {
                throw new ESTException(e3.getMessage(), e3.getCause());
            }
        } else {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Simple Enroll: ");
            stringBuilder3.append(originalRequest.getURL().toString());
            throw new ESTException(stringBuilder3.toString(), null, eSTResponse.getStatusCode(), eSTResponse.getInputStream());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x004e  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0042 A:{Catch:{ all -> 0x0038 }} */
    /* JADX WARNING: Removed duplicated region for block: B:19:0x003f A:{Catch:{ all -> 0x0038 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public EnrollmentResponse simpleEnroll(EnrollmentResponse enrollmentResponse) throws Exception {
        Throwable th;
        Throwable th2;
        if (this.clientProvider.isTrusted()) {
            ESTResponse eSTResponse = null;
            try {
                ESTClient makeClient = this.clientProvider.makeClient();
                ESTResponse doRequest = makeClient.doRequest(new ESTRequestBuilder(enrollmentResponse.getRequestToRetry()).withClient(makeClient).build());
                try {
                    EnrollmentResponse handleEnrollResponse = handleEnrollResponse(doRequest);
                    if (doRequest != null) {
                        doRequest.close();
                    }
                    return handleEnrollResponse;
                } catch (Throwable th3) {
                    th = th3;
                    eSTResponse = doRequest;
                    th2 = th;
                    if (eSTResponse != null) {
                        eSTResponse.close();
                    }
                    throw th2;
                }
            } catch (Throwable th4) {
                th2 = th4;
                if (th2 instanceof ESTException) {
                }
            }
        } else {
            throw new IllegalStateException("No trust anchors.");
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x0095 A:{Catch:{ all -> 0x008b }} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0092 A:{Catch:{ all -> 0x008b }} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x00a1  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public EnrollmentResponse simpleEnroll(boolean z, PKCS10CertificationRequest pKCS10CertificationRequest, ESTAuth eSTAuth) throws IOException {
        Throwable th;
        if (this.clientProvider.isTrusted()) {
            ESTResponse eSTResponse = null;
            try {
                byte[] bytes = annotateRequest(pKCS10CertificationRequest.getEncoded()).getBytes();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.server);
                stringBuilder.append(z ? SIMPLE_REENROLL : SIMPLE_ENROLL);
                URL url = new URL(stringBuilder.toString());
                ESTClient makeClient = this.clientProvider.makeClient();
                ESTRequestBuilder withClient = new ESTRequestBuilder(HttpHeaders.POST, url).withData(bytes).withClient(makeClient);
                withClient.addHeader(HttpHeaders.CONTENT_TYPE, "application/pkcs10");
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("");
                stringBuilder2.append(bytes.length);
                withClient.addHeader("Content-Length", stringBuilder2.toString());
                withClient.addHeader("Content-Transfer-Encoding", "base64");
                if (eSTAuth != null) {
                    eSTAuth.applyAuth(withClient);
                }
                ESTResponse doRequest = makeClient.doRequest(withClient.build());
                try {
                    EnrollmentResponse handleEnrollResponse = handleEnrollResponse(doRequest);
                    if (doRequest != null) {
                        doRequest.close();
                    }
                    return handleEnrollResponse;
                } catch (Throwable th2) {
                    eSTResponse = doRequest;
                    th = th2;
                    if (eSTResponse != null) {
                        eSTResponse.close();
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                if (th instanceof ESTException) {
                    throw ((ESTException) th);
                }
                throw new ESTException(th.getMessage(), th);
            }
        }
        throw new IllegalStateException("No trust anchors.");
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x0069 A:{Catch:{ all -> 0x005f }} */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0066 A:{Catch:{ all -> 0x005f }} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0075  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public EnrollmentResponse simpleEnrollPoP(boolean z, final PKCS10CertificationRequestBuilder pKCS10CertificationRequestBuilder, final ContentSigner contentSigner, ESTAuth eSTAuth) throws IOException {
        Throwable th;
        if (this.clientProvider.isTrusted()) {
            ESTResponse eSTResponse = null;
            try {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.server);
                stringBuilder.append(z ? SIMPLE_REENROLL : SIMPLE_ENROLL);
                URL url = new URL(stringBuilder.toString());
                ESTClient makeClient = this.clientProvider.makeClient();
                ESTRequestBuilder withConnectionListener = new ESTRequestBuilder(HttpHeaders.POST, url).withClient(makeClient).withConnectionListener(new ESTSourceConnectionListener() {
                    public ESTRequest onConnection(Source source, ESTRequest eSTRequest) throws IOException {
                        if (source instanceof TLSUniqueProvider) {
                            TLSUniqueProvider tLSUniqueProvider = (TLSUniqueProvider) source;
                            if (tLSUniqueProvider.isTLSUniqueAvailable()) {
                                PKCS10CertificationRequestBuilder pKCS10CertificationRequestBuilder = new PKCS10CertificationRequestBuilder(pKCS10CertificationRequestBuilder);
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                pKCS10CertificationRequestBuilder.setAttribute(PKCSObjectIdentifiers.pkcs_9_at_challengePassword, new DERPrintableString(Base64.toBase64String(tLSUniqueProvider.getTLSUnique())));
                                byteArrayOutputStream.write(ESTService.this.annotateRequest(pKCS10CertificationRequestBuilder.build(contentSigner).getEncoded()).getBytes());
                                byteArrayOutputStream.flush();
                                ESTRequestBuilder withData = new ESTRequestBuilder(eSTRequest).withData(byteArrayOutputStream.toByteArray());
                                withData.setHeader(HttpHeaders.CONTENT_TYPE, "application/pkcs10");
                                withData.setHeader("Content-Transfer-Encoding", "base64");
                                withData.setHeader("Content-Length", Long.toString((long) byteArrayOutputStream.size()));
                                return withData.build();
                            }
                        }
                        throw new IOException("Source does not supply TLS unique.");
                    }
                });
                if (eSTAuth != null) {
                    eSTAuth.applyAuth(withConnectionListener);
                }
                ESTResponse doRequest = makeClient.doRequest(withConnectionListener.build());
                try {
                    EnrollmentResponse handleEnrollResponse = handleEnrollResponse(doRequest);
                    if (doRequest != null) {
                        doRequest.close();
                    }
                    return handleEnrollResponse;
                } catch (Throwable th2) {
                    eSTResponse = doRequest;
                    th = th2;
                    if (eSTResponse != null) {
                        eSTResponse.close();
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                if (th instanceof ESTException) {
                }
            }
        } else {
            throw new IllegalStateException("No trust anchors.");
        }
    }
}
