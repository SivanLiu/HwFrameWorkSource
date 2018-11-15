package org.apache.http.impl.auth;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.params.AuthParams;
import org.apache.http.message.BasicHeaderValueFormatter;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BufferedHeader;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EncodingUtils;

@Deprecated
public class DigestScheme extends RFC2617Scheme {
    private static final char[] HEXADECIMAL = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static final String NC = "00000001";
    private static final int QOP_AUTH = 2;
    private static final int QOP_AUTH_INT = 1;
    private static final int QOP_MISSING = 0;
    private String cnonce;
    private boolean complete = false;
    private int qopVariant = 0;

    public void processChallenge(Header header) throws MalformedChallengeException {
        super.processChallenge(header);
        if (getParameter("realm") == null) {
            throw new MalformedChallengeException("missing realm in challange");
        } else if (getParameter("nonce") != null) {
            boolean unsupportedQop = false;
            String qop = getParameter("qop");
            if (qop != null) {
                StringTokenizer tok = new StringTokenizer(qop, ",");
                while (tok.hasMoreTokens()) {
                    String variant = tok.nextToken().trim();
                    if (variant.equals("auth")) {
                        this.qopVariant = 2;
                        break;
                    } else if (variant.equals("auth-int")) {
                        this.qopVariant = 1;
                    } else {
                        unsupportedQop = true;
                    }
                }
            }
            if (unsupportedQop && this.qopVariant == 0) {
                throw new MalformedChallengeException("None of the qop methods is supported");
            }
            this.cnonce = null;
            this.complete = true;
        } else {
            throw new MalformedChallengeException("missing nonce in challange");
        }
    }

    public boolean isComplete() {
        if ("true".equalsIgnoreCase(getParameter("stale"))) {
            return false;
        }
        return this.complete;
    }

    public String getSchemeName() {
        return "digest";
    }

    public boolean isConnectionBased() {
        return false;
    }

    public void overrideParamter(String name, String value) {
        getParameters().put(name, value);
    }

    private String getCnonce() {
        if (this.cnonce == null) {
            this.cnonce = createCnonce();
        }
        return this.cnonce;
    }

    public Header authenticate(Credentials credentials, HttpRequest request) throws AuthenticationException {
        if (credentials == null) {
            throw new IllegalArgumentException("Credentials may not be null");
        } else if (request != null) {
            getParameters().put("methodname", request.getRequestLine().getMethod());
            getParameters().put("uri", request.getRequestLine().getUri());
            if (getParameter("charset") == null) {
                getParameters().put("charset", AuthParams.getCredentialCharset(request.getParams()));
            }
            return createDigestHeader(credentials, createDigest(credentials));
        } else {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
    }

    private static MessageDigest createMessageDigest(String digAlg) throws UnsupportedDigestAlgorithmException {
        try {
            return MessageDigest.getInstance(digAlg);
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported algorithm in HTTP Digest authentication: ");
            stringBuilder.append(digAlg);
            throw new UnsupportedDigestAlgorithmException(stringBuilder.toString());
        }
    }

    private String createDigest(Credentials credentials) throws AuthenticationException {
        String uri = getParameter("uri");
        String realm = getParameter("realm");
        String nonce = getParameter("nonce");
        String method = getParameter("methodname");
        String algorithm = getParameter("algorithm");
        String str;
        if (uri == null) {
            str = realm;
            throw new IllegalStateException("URI may not be null");
        } else if (realm == null) {
            str = realm;
            throw new IllegalStateException("Realm may not be null");
        } else if (nonce != null) {
            if (algorithm == null) {
                algorithm = "MD5";
            }
            String charset = getParameter("charset");
            if (charset == null) {
                charset = "ISO-8859-1";
            }
            if (this.qopVariant != 1) {
                String cnonce;
                String tmp2;
                StringBuilder stringBuilder;
                MessageDigest md5Helper = createMessageDigest("MD5");
                String uname = credentials.getUserPrincipal().getName();
                String pwd = credentials.getPassword();
                StringBuilder tmp = new StringBuilder(((uname.length() + realm.length()) + pwd.length()) + 2);
                tmp.append(uname);
                tmp.append(':');
                tmp.append(realm);
                tmp.append(':');
                tmp.append(pwd);
                String a1 = tmp.toString();
                if (algorithm.equalsIgnoreCase("MD5-sess")) {
                    cnonce = getCnonce();
                    tmp2 = encode(md5Helper.digest(EncodingUtils.getBytes(a1, charset)));
                    StringBuilder tmp3 = new StringBuilder(((tmp2.length() + nonce.length()) + cnonce.length()) + 2);
                    tmp3.append(tmp2);
                    tmp3.append(':');
                    tmp3.append(nonce);
                    tmp3.append(':');
                    tmp3.append(cnonce);
                    a1 = tmp3.toString();
                } else if (!algorithm.equalsIgnoreCase("MD5")) {
                    str = realm;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unhandled algorithm ");
                    stringBuilder.append(algorithm);
                    stringBuilder.append(" requested");
                    throw new AuthenticationException(stringBuilder.toString());
                }
                String md5a1 = encode(md5Helper.digest(EncodingUtils.getBytes(a1, charset)));
                String a2 = null;
                if (this.qopVariant != 1) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(method);
                    stringBuilder2.append(':');
                    stringBuilder2.append(uri);
                    a2 = stringBuilder2.toString();
                }
                cnonce = encode(md5Helper.digest(EncodingUtils.getAsciiBytes(a2)));
                if (this.qopVariant == 0) {
                    stringBuilder = new StringBuilder((md5a1.length() + nonce.length()) + cnonce.length());
                    stringBuilder.append(md5a1);
                    stringBuilder.append(':');
                    stringBuilder.append(nonce);
                    stringBuilder.append(':');
                    stringBuilder.append(cnonce);
                    uri = stringBuilder.toString();
                    str = realm;
                } else {
                    uri = getQopVariantString();
                    tmp2 = getCnonce();
                    StringBuilder tmp22 = new StringBuilder((((((md5a1.length() + nonce.length()) + NC.length()) + tmp2.length()) + uri.length()) + cnonce.length()) + 5);
                    tmp22.append(md5a1);
                    tmp22.append(':');
                    tmp22.append(nonce);
                    tmp22.append(':');
                    tmp22.append(NC);
                    tmp22.append(':');
                    tmp22.append(tmp2);
                    tmp22.append(':');
                    tmp22.append(uri);
                    tmp22.append(':');
                    tmp22.append(cnonce);
                    uri = tmp22.toString();
                }
                return encode(md5Helper.digest(EncodingUtils.getAsciiBytes(uri)));
            }
            str = realm;
            throw new AuthenticationException("Unsupported qop in HTTP Digest authentication");
        } else {
            str = realm;
            throw new IllegalStateException("Nonce may not be null");
        }
    }

    private Header createDigestHeader(Credentials credentials, String digest) throws AuthenticationException {
        CharArrayBuffer buffer = new CharArrayBuffer(128);
        if (isProxy()) {
            buffer.append(AUTH.PROXY_AUTH_RESP);
        } else {
            buffer.append(AUTH.WWW_AUTH_RESP);
        }
        buffer.append(": Digest ");
        String uri = getParameter("uri");
        String realm = getParameter("realm");
        String nonce = getParameter("nonce");
        String opaque = getParameter("opaque");
        String response = digest;
        String algorithm = getParameter("algorithm");
        String uname = credentials.getUserPrincipal().getName();
        List<BasicNameValuePair> params = new ArrayList(20);
        params.add(new BasicNameValuePair("username", uname));
        params.add(new BasicNameValuePair("realm", realm));
        params.add(new BasicNameValuePair("nonce", nonce));
        params.add(new BasicNameValuePair("uri", uri));
        params.add(new BasicNameValuePair("response", response));
        if (this.qopVariant != 0) {
            params.add(new BasicNameValuePair("qop", getQopVariantString()));
            params.add(new BasicNameValuePair("nc", NC));
            params.add(new BasicNameValuePair("cnonce", getCnonce()));
        }
        if (algorithm != null) {
            params.add(new BasicNameValuePair("algorithm", algorithm));
        }
        if (opaque != null) {
            params.add(new BasicNameValuePair("opaque", opaque));
        }
        for (int i = 0; i < params.size(); i++) {
            NameValuePair param = (BasicNameValuePair) params.get(i);
            if (i > 0) {
                buffer.append(", ");
            }
            boolean z = true;
            boolean noQuotes = "nc".equals(param.getName()) || "qop".equals(param.getName());
            BasicHeaderValueFormatter basicHeaderValueFormatter = BasicHeaderValueFormatter.DEFAULT;
            if (noQuotes) {
                z = false;
            }
            basicHeaderValueFormatter.formatNameValuePair(buffer, param, z);
        }
        return new BufferedHeader(buffer);
    }

    private String getQopVariantString() {
        if (this.qopVariant == 1) {
            return "auth-int";
        }
        return "auth";
    }

    private static String encode(byte[] binaryData) {
        if (binaryData.length != 16) {
            return null;
        }
        char[] buffer = new char[32];
        for (int i = 0; i < 16; i++) {
            int low = binaryData[i] & 15;
            buffer[i * 2] = HEXADECIMAL[(binaryData[i] & 240) >> 4];
            buffer[(i * 2) + 1] = HEXADECIMAL[low];
        }
        return new String(buffer);
    }

    public static String createCnonce() {
        return encode(createMessageDigest("MD5").digest(EncodingUtils.getAsciiBytes(Long.toString(System.currentTimeMillis()))));
    }
}
