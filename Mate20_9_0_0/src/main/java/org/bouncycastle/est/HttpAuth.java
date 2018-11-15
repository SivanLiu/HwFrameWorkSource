package org.bouncycastle.est;

import com.huawei.security.hccm.common.connection.HttpConnection.HttpHeaders;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSAttributeTableGenerator;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

public class HttpAuth implements ESTAuth {
    private static final DigestAlgorithmIdentifierFinder digestAlgorithmIdentifierFinder = new DefaultDigestAlgorithmIdentifierFinder();
    private static final Set<String> validParts;
    private final DigestCalculatorProvider digestCalculatorProvider;
    private final SecureRandom nonceGenerator;
    private final char[] password;
    private final String realm;
    private final String username;

    static {
        Set hashSet = new HashSet();
        hashSet.add("realm");
        hashSet.add("nonce");
        hashSet.add("opaque");
        hashSet.add("algorithm");
        hashSet.add("qop");
        validParts = Collections.unmodifiableSet(hashSet);
    }

    public HttpAuth(String str, String str2, char[] cArr) {
        this(str, str2, cArr, null, null);
    }

    public HttpAuth(String str, String str2, char[] cArr, SecureRandom secureRandom, DigestCalculatorProvider digestCalculatorProvider) {
        this.realm = str;
        this.username = str2;
        this.password = cArr;
        this.nonceGenerator = secureRandom;
        this.digestCalculatorProvider = digestCalculatorProvider;
    }

    public HttpAuth(String str, char[] cArr) {
        this(null, str, cArr, null, null);
    }

    public HttpAuth(String str, char[] cArr, SecureRandom secureRandom, DigestCalculatorProvider digestCalculatorProvider) {
        this(null, str, cArr, secureRandom, digestCalculatorProvider);
    }

    private ESTResponse doDigestFunction(ESTResponse eSTResponse) throws IOException {
        ESTResponse eSTResponse2 = eSTResponse;
        eSTResponse.close();
        ESTRequest originalRequest = eSTResponse.getOriginalRequest();
        try {
            Map splitCSL = HttpUtil.splitCSL("Digest", eSTResponse2.getHeader("WWW-Authenticate"));
            StringBuilder stringBuilder;
            try {
                StringBuilder stringBuilder2;
                String path = originalRequest.getURL().toURI().getPath();
                for (Object next : splitCSL.keySet()) {
                    if (!validParts.contains(next)) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unrecognised entry in WWW-Authenticate header: '");
                        stringBuilder2.append(next);
                        stringBuilder2.append("'");
                        throw new ESTException(stringBuilder2.toString());
                    }
                }
                String method = originalRequest.getMethod();
                String str = (String) splitCSL.get("realm");
                String str2 = (String) splitCSL.get("nonce");
                String str3 = (String) splitCSL.get("opaque");
                String str4 = (String) splitCSL.get("algorithm");
                String str5 = (String) splitCSL.get("qop");
                List arrayList = new ArrayList();
                if (this.realm == null || this.realm.equals(str)) {
                    if (str4 == null) {
                        str4 = "MD5";
                    }
                    if (str4.length() != 0) {
                        str4 = Strings.toUpperCase(str4);
                        if (str5 == null) {
                            throw new ESTException("Qop is not defined in WWW-Authenticate header.");
                        } else if (str5.length() != 0) {
                            String[] split = Strings.toLowerCase(str5).split(",");
                            int i = 0;
                            while (i != split.length) {
                                if (split[i].equals("auth") || split[i].equals("auth-int")) {
                                    String trim = split[i].trim();
                                    if (!arrayList.contains(trim)) {
                                        arrayList.add(trim);
                                    }
                                    i++;
                                } else {
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("QoP value unknown: '");
                                    stringBuilder2.append(i);
                                    stringBuilder2.append("'");
                                    throw new ESTException(stringBuilder2.toString());
                                }
                            }
                            AlgorithmIdentifier lookupDigest = lookupDigest(str4);
                            if (lookupDigest == null || lookupDigest.getAlgorithm() == null) {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("auth digest algorithm unknown: ");
                                stringBuilder2.append(str4);
                                throw new IOException(stringBuilder2.toString());
                            }
                            DigestCalculator digestCalculator;
                            OutputStream outputStream;
                            Object obj;
                            Object obj2;
                            ESTRequestBuilder withHijacker;
                            DigestCalculator digestCalculator2 = getDigestCalculator(str4, lookupDigest);
                            OutputStream outputStream2 = digestCalculator2.getOutputStream();
                            String makeNonce = makeNonce(10);
                            update(outputStream2, this.username);
                            update(outputStream2, ":");
                            update(outputStream2, str);
                            update(outputStream2, ":");
                            update(outputStream2, this.password);
                            outputStream2.close();
                            byte[] digest = digestCalculator2.getDigest();
                            if (str4.endsWith("-SESS")) {
                                digestCalculator = getDigestCalculator(str4, lookupDigest);
                                outputStream = digestCalculator.getOutputStream();
                                update(outputStream, Hex.toHexString(digest));
                                update(outputStream, ":");
                                update(outputStream, str2);
                                update(outputStream, ":");
                                update(outputStream, makeNonce);
                                outputStream.close();
                                digest = digestCalculator.getDigest();
                            }
                            String toHexString = Hex.toHexString(digest);
                            digestCalculator = getDigestCalculator(str4, lookupDigest);
                            outputStream = digestCalculator.getOutputStream();
                            if (((String) arrayList.get(0)).equals("auth-int")) {
                                DigestCalculator digestCalculator3 = getDigestCalculator(str4, lookupDigest);
                                OutputStream outputStream3 = digestCalculator3.getOutputStream();
                                originalRequest.writeData(outputStream3);
                                outputStream3.close();
                                byte[] digest2 = digestCalculator3.getDigest();
                                update(outputStream, method);
                                update(outputStream, ":");
                                update(outputStream, path);
                                update(outputStream, ":");
                                update(outputStream, Hex.toHexString(digest2));
                            } else if (((String) arrayList.get(0)).equals("auth")) {
                                update(outputStream, method);
                                update(outputStream, ":");
                                update(outputStream, path);
                            }
                            outputStream.close();
                            method = Hex.toHexString(digestCalculator.getDigest());
                            DigestCalculator digestCalculator4 = getDigestCalculator(str4, lookupDigest);
                            OutputStream outputStream4 = digestCalculator4.getOutputStream();
                            if (arrayList.contains("missing")) {
                                update(outputStream4, toHexString);
                                update(outputStream4, ":");
                                update(outputStream4, str2);
                            } else {
                                update(outputStream4, toHexString);
                                update(outputStream4, ":");
                                update(outputStream4, str2);
                                update(outputStream4, ":");
                                update(outputStream4, "00000001");
                                update(outputStream4, ":");
                                update(outputStream4, makeNonce);
                                update(outputStream4, ":");
                                update(outputStream4, ((String) arrayList.get(0)).equals("auth-int") ? "auth-int" : "auth");
                            }
                            update(outputStream4, ":");
                            update(outputStream4, method);
                            outputStream4.close();
                            str5 = Hex.toHexString(digestCalculator4.getDigest());
                            Map hashMap = new HashMap();
                            hashMap.put("username", this.username);
                            hashMap.put("realm", str);
                            hashMap.put("nonce", str2);
                            hashMap.put("uri", path);
                            hashMap.put("response", str5);
                            if (((String) arrayList.get(0)).equals("auth-int")) {
                                obj = "qop";
                                obj2 = "auth-int";
                            } else {
                                if (((String) arrayList.get(0)).equals("auth")) {
                                    obj = "qop";
                                    obj2 = "auth";
                                }
                                hashMap.put("algorithm", str4);
                                if (str3 == null || str3.length() == 0) {
                                    hashMap.put("opaque", makeNonce(20));
                                }
                                withHijacker = new ESTRequestBuilder(originalRequest).withHijacker(null);
                                withHijacker.setHeader(HttpHeaders.AUTHENTICATION, HttpUtil.mergeCSL("Digest", hashMap));
                                return originalRequest.getClient().doRequest(withHijacker.build());
                            }
                            hashMap.put(obj, obj2);
                            hashMap.put("nc", "00000001");
                            hashMap.put("cnonce", makeNonce);
                            hashMap.put("algorithm", str4);
                            hashMap.put("opaque", makeNonce(20));
                            withHijacker = new ESTRequestBuilder(originalRequest).withHijacker(null);
                            withHijacker.setHeader(HttpHeaders.AUTHENTICATION, HttpUtil.mergeCSL("Digest", hashMap));
                            return originalRequest.getClient().doRequest(withHijacker.build());
                        } else {
                            throw new ESTException("QoP value is empty.");
                        }
                    }
                    throw new ESTException("WWW-Authenticate no algorithm defined.");
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("Supplied realm '");
                stringBuilder.append(this.realm);
                stringBuilder.append("' does not match server realm '");
                stringBuilder.append(str);
                stringBuilder.append("'");
                throw new ESTException(stringBuilder.toString(), null, 401, null);
            } catch (Exception e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("unable to process URL in request: ");
                stringBuilder.append(e.getMessage());
                throw new IOException(stringBuilder.toString());
            }
        } catch (Throwable th) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Parsing WWW-Authentication header: ");
            stringBuilder3.append(th.getMessage());
            ESTException eSTException = new ESTException(stringBuilder3.toString(), th, eSTResponse.getStatusCode(), new ByteArrayInputStream(eSTResponse2.getHeader("WWW-Authenticate").getBytes()));
        }
    }

    private DigestCalculator getDigestCalculator(String str, AlgorithmIdentifier algorithmIdentifier) throws IOException {
        try {
            return this.digestCalculatorProvider.get(algorithmIdentifier);
        } catch (OperatorCreationException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cannot create digest calculator for ");
            stringBuilder.append(str);
            stringBuilder.append(": ");
            stringBuilder.append(e.getMessage());
            throw new IOException(stringBuilder.toString());
        }
    }

    private AlgorithmIdentifier lookupDigest(String str) {
        if (str.endsWith("-SESS")) {
            str = str.substring(0, str.length() - "-SESS".length());
        }
        return str.equals("SHA-512-256") ? new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512_256, DERNull.INSTANCE) : digestAlgorithmIdentifierFinder.find(str);
    }

    private String makeNonce(int i) {
        byte[] bArr = new byte[i];
        this.nonceGenerator.nextBytes(bArr);
        return Hex.toHexString(bArr);
    }

    private void update(OutputStream outputStream, String str) throws IOException {
        outputStream.write(Strings.toUTF8ByteArray(str));
    }

    private void update(OutputStream outputStream, char[] cArr) throws IOException {
        outputStream.write(Strings.toUTF8ByteArray(cArr));
    }

    public void applyAuth(ESTRequestBuilder eSTRequestBuilder) {
        eSTRequestBuilder.withHijacker(new ESTHijacker() {
            public ESTResponse hijack(ESTRequest eSTRequest, Source source) throws IOException {
                ESTResponse eSTResponse = new ESTResponse(eSTRequest, source);
                if (eSTResponse.getStatusCode() != 401) {
                    return eSTResponse;
                }
                String header = eSTResponse.getHeader("WWW-Authenticate");
                if (header != null) {
                    header = Strings.toLowerCase(header);
                    if (header.startsWith(CMSAttributeTableGenerator.DIGEST)) {
                        return HttpAuth.this.doDigestFunction(eSTResponse);
                    }
                    StringBuilder stringBuilder;
                    if (header.startsWith("basic")) {
                        eSTResponse.close();
                        Map splitCSL = HttpUtil.splitCSL("Basic", eSTResponse.getHeader("WWW-Authenticate"));
                        if (HttpAuth.this.realm == null || HttpAuth.this.realm.equals(splitCSL.get("realm"))) {
                            ESTRequestBuilder withHijacker = new ESTRequestBuilder(eSTRequest).withHijacker(null);
                            if (HttpAuth.this.realm != null && HttpAuth.this.realm.length() > 0) {
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Basic realm=\"");
                                stringBuilder2.append(HttpAuth.this.realm);
                                stringBuilder2.append("\"");
                                withHijacker.setHeader("WWW-Authenticate", stringBuilder2.toString());
                            }
                            if (HttpAuth.this.username.contains(":")) {
                                throw new IllegalArgumentException("User must not contain a ':'");
                            }
                            char[] cArr = new char[((HttpAuth.this.username.length() + 1) + HttpAuth.this.password.length)];
                            System.arraycopy(HttpAuth.this.username.toCharArray(), 0, cArr, 0, HttpAuth.this.username.length());
                            cArr[HttpAuth.this.username.length()] = ':';
                            System.arraycopy(HttpAuth.this.password, 0, cArr, HttpAuth.this.username.length() + 1, HttpAuth.this.password.length);
                            String str = HttpHeaders.AUTHENTICATION;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Basic ");
                            stringBuilder3.append(Base64.toBase64String(Strings.toByteArray(cArr)));
                            withHijacker.setHeader(str, stringBuilder3.toString());
                            ESTResponse doRequest = eSTRequest.getClient().doRequest(withHijacker.build());
                            Arrays.fill(cArr, 0);
                            return doRequest;
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Supplied realm '");
                        stringBuilder.append(HttpAuth.this.realm);
                        stringBuilder.append("' does not match server realm '");
                        stringBuilder.append((String) splitCSL.get("realm"));
                        stringBuilder.append("'");
                        throw new ESTException(stringBuilder.toString(), null, 401, null);
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown auth mode: ");
                    stringBuilder.append(header);
                    throw new ESTException(stringBuilder.toString());
                }
                throw new ESTException("Status of 401 but no WWW-Authenticate header");
            }
        });
    }
}
