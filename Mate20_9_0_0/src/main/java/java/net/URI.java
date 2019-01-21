package java.net;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import sun.nio.cs.ThreadLocalCoders;
import sun.util.locale.BaseLocale;
import sun.util.locale.LanguageTag;

public final class URI implements Comparable<URI>, Serializable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final long H_ALPHA = (H_LOWALPHA | H_UPALPHA);
    private static final long H_ALPHANUM = (H_ALPHA | 0);
    private static final long H_DASH = highMask(LanguageTag.SEP);
    private static final long H_DIGIT = 0;
    private static final long H_DOT = highMask(".");
    private static final long H_ESCAPED = 0;
    private static final long H_HEX = (highMask('A', 'F') | highMask('a', 'f'));
    private static final long H_LEFT_BRACKET = highMask("[");
    private static final long H_LOWALPHA = highMask('a', 'z');
    private static final long H_MARK = highMask("-_.!~*'()");
    private static final long H_PATH = (H_PCHAR | highMask(";/"));
    private static final long H_PCHAR = ((H_UNRESERVED | 0) | highMask(":@&=+$,"));
    private static final long H_REG_NAME = ((H_UNRESERVED | 0) | highMask("$,;:@&=+"));
    private static final long H_RESERVED = highMask(";/?:@&=+$,[]");
    private static final long H_SCHEME = ((H_ALPHA | 0) | highMask("+-."));
    private static final long H_SERVER = (((H_USERINFO | H_ALPHANUM) | H_DASH) | highMask(".:@[]"));
    private static final long H_SERVER_PERCENT = (H_SERVER | highMask("%"));
    private static final long H_UNDERSCORE = highMask(BaseLocale.SEP);
    private static final long H_UNRESERVED = (H_ALPHANUM | H_MARK);
    private static final long H_UPALPHA = highMask('A', 'Z');
    private static final long H_URIC = ((H_RESERVED | H_UNRESERVED) | 0);
    private static final long H_URIC_NO_SLASH = ((H_UNRESERVED | 0) | highMask(";?:@&=+$,"));
    private static final long H_USERINFO = ((H_UNRESERVED | 0) | highMask(";:&=+$,"));
    private static final long L_ALPHA = 0;
    private static final long L_ALPHANUM = (L_DIGIT | 0);
    private static final long L_DASH = lowMask(LanguageTag.SEP);
    private static final long L_DIGIT = lowMask('0', '9');
    private static final long L_DOT = lowMask(".");
    private static final long L_ESCAPED = 1;
    private static final long L_HEX = L_DIGIT;
    private static final long L_LEFT_BRACKET = lowMask("[");
    private static final long L_LOWALPHA = 0;
    private static final long L_MARK = lowMask("-_.!~*'()");
    private static final long L_PATH = (L_PCHAR | lowMask(";/"));
    private static final long L_PCHAR = ((L_UNRESERVED | L_ESCAPED) | lowMask(":@&=+$,"));
    private static final long L_REG_NAME = ((L_UNRESERVED | L_ESCAPED) | lowMask("$,;:@&=+"));
    private static final long L_RESERVED = lowMask(";/?:@&=+$,[]");
    private static final long L_SCHEME = ((L_DIGIT | 0) | lowMask("+-."));
    private static final long L_SERVER = (((L_USERINFO | L_ALPHANUM) | L_DASH) | lowMask(".:@[]"));
    private static final long L_SERVER_PERCENT = (L_SERVER | lowMask("%"));
    private static final long L_UNDERSCORE = lowMask(BaseLocale.SEP);
    private static final long L_UNRESERVED = (L_ALPHANUM | L_MARK);
    private static final long L_UPALPHA = 0;
    private static final long L_URIC = ((L_RESERVED | L_UNRESERVED) | L_ESCAPED);
    private static final long L_URIC_NO_SLASH = ((L_UNRESERVED | L_ESCAPED) | lowMask(";?:@&=+$,"));
    private static final long L_USERINFO = ((L_UNRESERVED | L_ESCAPED) | lowMask(";:&=+$,"));
    private static final char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    static final long serialVersionUID = -6052424284110960213L;
    private transient String authority;
    private volatile transient String decodedAuthority;
    private volatile transient String decodedFragment;
    private volatile transient String decodedPath;
    private volatile transient String decodedQuery;
    private volatile transient String decodedSchemeSpecificPart;
    private volatile transient String decodedUserInfo;
    private transient String fragment;
    private volatile transient int hash;
    private transient String host;
    private transient String path;
    private transient int port;
    private transient String query;
    private transient String scheme;
    private volatile transient String schemeSpecificPart;
    private volatile String string;
    private transient String userInfo;

    private class Parser {
        private String input;
        private int ipv6byteCount = 0;
        private boolean requireServerAuthority = URI.$assertionsDisabled;

        Parser(String s) {
            this.input = s;
            URI.this.string = s;
        }

        private void fail(String reason) throws URISyntaxException {
            throw new URISyntaxException(this.input, reason);
        }

        private void fail(String reason, int p) throws URISyntaxException {
            throw new URISyntaxException(this.input, reason, p);
        }

        private void failExpecting(String expected, int p) throws URISyntaxException {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected ");
            stringBuilder.append(expected);
            fail(stringBuilder.toString(), p);
        }

        private void failExpecting(String expected, String prior, int p) throws URISyntaxException {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected ");
            stringBuilder.append(expected);
            stringBuilder.append(" following ");
            stringBuilder.append(prior);
            fail(stringBuilder.toString(), p);
        }

        private String substring(int start, int end) {
            return this.input.substring(start, end);
        }

        private char charAt(int p) {
            return this.input.charAt(p);
        }

        private boolean at(int start, int end, char c) {
            return (start >= end || charAt(start) != c) ? URI.$assertionsDisabled : true;
        }

        private boolean at(int start, int end, String s) {
            int p = start;
            int sn = s.length();
            int i = end - p;
            boolean z = URI.$assertionsDisabled;
            if (sn > i) {
                return URI.$assertionsDisabled;
            }
            i = p;
            p = 0;
            while (p < sn) {
                int p2 = i + 1;
                if (charAt(i) != s.charAt(p)) {
                    i = p2;
                    break;
                }
                p++;
                i = p2;
            }
            if (p == sn) {
                z = true;
            }
            return z;
        }

        private int scan(int start, int end, char c) {
            if (start >= end || charAt(start) != c) {
                return start;
            }
            return start + 1;
        }

        private int scan(int start, int end, String err, String stop) {
            int p = start;
            while (p < end) {
                int c = charAt(p);
                if (err.indexOf(c) >= 0) {
                    return -1;
                }
                if (stop.indexOf(c) >= 0) {
                    break;
                }
                p++;
            }
            return p;
        }

        private int scanEscape(int start, int n, char first) throws URISyntaxException {
            int p = start;
            char c = first;
            if (c == '%') {
                if (p + 3 <= n && URI.match(charAt(p + 1), URI.L_HEX, URI.H_HEX) && URI.match(charAt(p + 2), URI.L_HEX, URI.H_HEX)) {
                    return p + 3;
                }
                fail("Malformed escape pair", p);
            } else if (!(c <= 128 || Character.isSpaceChar(c) || Character.isISOControl(c))) {
                return p + 1;
            }
            return p;
        }

        private int scan(int start, int n, long lowMask, long highMask) throws URISyntaxException {
            int p = start;
            while (p < n) {
                char c = charAt(p);
                if (!URI.match(c, lowMask, highMask)) {
                    if ((URI.L_ESCAPED & lowMask) == 0) {
                        break;
                    }
                    int q = scanEscape(p, n, c);
                    if (q <= p) {
                        break;
                    }
                    p = q;
                } else {
                    p++;
                }
            }
            return p;
        }

        private void checkChars(int start, int end, long lowMask, long highMask, String what) throws URISyntaxException {
            int p = scan(start, end, lowMask, highMask);
            if (p < end) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal character in ");
                stringBuilder.append(what);
                fail(stringBuilder.toString(), p);
            }
        }

        private void checkChar(int p, long lowMask, long highMask, String what) throws URISyntaxException {
            checkChars(p, p + 1, lowMask, highMask, what);
        }

        void parse(boolean rsa) throws URISyntaxException {
            int ssp;
            this.requireServerAuthority = rsa;
            int n = this.input.length();
            int p = scan(0, n, "/?#", ":");
            if (p < 0 || !at(p, n, ':')) {
                ssp = 0;
                p = parseHierarchical(0, n);
            } else {
                if (p == 0) {
                    failExpecting("scheme name", 0);
                }
                checkChar(0, 0, URI.H_ALPHA, "scheme name");
                checkChars(1, p, URI.L_SCHEME, URI.H_SCHEME, "scheme name");
                URI.this.scheme = substring(0, p);
                p++;
                ssp = p;
                if (at(p, n, '/')) {
                    p = parseHierarchical(p, n);
                } else {
                    int q = scan(p, n, "", "#");
                    if (q <= p) {
                        failExpecting("scheme-specific part", p);
                    }
                    checkChars(p, q, URI.L_URIC, URI.H_URIC, "opaque part");
                    p = q;
                }
            }
            int p2 = p;
            URI.this.schemeSpecificPart = substring(ssp, p2);
            if (at(p2, n, '#')) {
                checkChars(p2 + 1, n, URI.L_URIC, URI.H_URIC, "fragment");
                URI.this.fragment = substring(p2 + 1, n);
                p2 = n;
            }
            if (p2 < n) {
                fail("end of URI", p2);
            }
        }

        private int parseHierarchical(int start, int n) throws URISyntaxException {
            int q;
            int p = start;
            if (at(p, n, '/') && at(p + 1, n, '/')) {
                p += 2;
                q = scan(p, n, "", "/?#");
                if (q > p) {
                    p = parseAuthority(p, q);
                } else if (q >= n) {
                    failExpecting("authority", p);
                }
            }
            q = scan(p, n, "", "?#");
            checkChars(p, q, URI.L_PATH, URI.H_PATH, "path");
            URI.this.path = substring(p, q);
            p = q;
            if (!at(p, n, '?')) {
                return p;
            }
            p++;
            q = scan(p, n, "", "#");
            checkChars(p, q, URI.L_URIC, URI.H_URIC, "query");
            URI.this.query = substring(p, q);
            return q;
        }

        private int parseAuthority(int start, int n) throws URISyntaxException {
            boolean serverChars;
            int p = start;
            int q = p;
            URISyntaxException ex = null;
            boolean z = true;
            if (scan(p, n, "", "]") > p) {
                serverChars = scan(p, n, URI.L_SERVER_PERCENT, URI.H_SERVER_PERCENT) == n ? true : URI.$assertionsDisabled;
            } else {
                serverChars = scan(p, n, URI.L_SERVER, URI.H_SERVER) == n ? true : URI.$assertionsDisabled;
            }
            boolean serverChars2 = serverChars;
            if (scan(p, n, URI.L_REG_NAME, URI.H_REG_NAME) != n) {
                z = false;
            }
            serverChars = z;
            if (!serverChars || serverChars2) {
                if (serverChars2) {
                    try {
                        q = parseServer(p, n);
                        if (q < n) {
                            failExpecting("end of authority", q);
                        }
                        URI.this.authority = substring(p, n);
                    } catch (URISyntaxException x) {
                        URI.this.userInfo = null;
                        URI.this.host = null;
                        URI.this.port = -1;
                        if (this.requireServerAuthority) {
                            throw x;
                        }
                        ex = x;
                        q = p;
                    }
                }
                if (q < n) {
                    if (serverChars) {
                        URI.this.authority = substring(p, n);
                    } else if (ex == null) {
                        fail("Illegal character in authority", q);
                    } else {
                        throw ex;
                    }
                }
                return n;
            }
            URI.this.authority = substring(p, n);
            return n;
        }

        private int parseServer(int start, int n) throws URISyntaxException {
            int r;
            int p = start;
            int q = scan(p, n, "/?#", "@");
            if (q >= p && at(q, n, '@')) {
                checkChars(p, q, URI.L_USERINFO, URI.H_USERINFO, "user info");
                URI.this.userInfo = substring(p, q);
                p = q + 1;
            }
            if (at(p, n, '[')) {
                p++;
                q = scan(p, n, "/?#", "]");
                if (q <= p || !at(q, n, ']')) {
                    failExpecting("closing bracket for IPv6 address", q);
                } else {
                    r = scan(p, q, "", "%");
                    if (r > p) {
                        parseIPv6Reference(p, r);
                        if (r + 1 == q) {
                            fail("scope id expected");
                        }
                        checkChars(r + 1, q, URI.L_ALPHANUM, URI.H_ALPHANUM, "scope id");
                    } else {
                        parseIPv6Reference(p, q);
                    }
                    URI.this.host = substring(p - 1, q + 1);
                    p = q + 1;
                }
            } else {
                int q2 = parseIPv4Address(p, n);
                if (q2 <= p) {
                    q2 = parseHostname(p, n);
                }
                p = q2;
            }
            if (at(p, n, ':')) {
                r = p + 1;
                q = scan(r, n, "", "/");
                if (q > r) {
                    checkChars(r, q, URI.L_DIGIT, 0, "port number");
                    try {
                        URI.this.port = Integer.parseInt(substring(r, q));
                    } catch (NumberFormatException e) {
                        fail("Malformed port number", r);
                    }
                    p = q;
                } else {
                    p = r;
                }
            }
            if (p < n) {
                failExpecting("port number", p);
            }
            return p;
        }

        private int scanByte(int start, int n) throws URISyntaxException {
            int p = start;
            int q = scan(p, n, URI.L_DIGIT, 0);
            if (q > p && Integer.parseInt(substring(p, q)) > 255) {
                return p;
            }
            return q;
        }

        private int scanIPv4Address(int start, int n, boolean strict) throws URISyntaxException {
            int p = start;
            int m = scan(p, n, URI.L_DIGIT | URI.L_DOT, 0 | URI.H_DOT);
            if (m <= p || (strict && m != n)) {
                return -1;
            }
            int scanByte = scanByte(p, m);
            int q = scanByte;
            if (scanByte > p) {
                p = q;
                int scan = scan(p, m, '.');
                q = scan;
                if (scan > p) {
                    p = q;
                    scan = scanByte(p, m);
                    q = scan;
                    if (scan > p) {
                        p = q;
                        scan = scan(p, m, '.');
                        q = scan;
                        if (scan > p) {
                            p = q;
                            scan = scanByte(p, m);
                            q = scan;
                            if (scan > p) {
                                p = q;
                                scanByte = scan(p, m, '.');
                                q = scanByte;
                                if (scanByte > p) {
                                    p = q;
                                    scanByte = scanByte(p, m);
                                    q = scanByte;
                                    if (scanByte > p) {
                                        p = q;
                                        if (q >= m) {
                                            return q;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            fail("Malformed IPv4 address", q);
            return -1;
        }

        private int takeIPv4Address(int start, int n, String expected) throws URISyntaxException {
            int p = scanIPv4Address(start, n, 1);
            if (p <= start) {
                failExpecting(expected, start);
            }
            return p;
        }

        private int parseIPv4Address(int start, int n) {
            try {
                int p = scanIPv4Address(start, n, 0);
                if (p > start && p < n && charAt(p) != ':') {
                    p = -1;
                }
                if (p > start) {
                    URI.this.host = substring(start, p);
                }
                return p;
            } catch (URISyntaxException e) {
                return -1;
            } catch (NumberFormatException e2) {
                return -1;
            }
        }

        private int parseHostname(int start, int n) throws URISyntaxException {
            int p = start;
            int l = -1;
            do {
                int q = scan(p, n, URI.L_ALPHANUM, URI.H_ALPHANUM);
                if (q <= p) {
                    break;
                }
                l = p;
                if (q > p) {
                    p = q;
                    q = scan(p, n, (URI.L_ALPHANUM | URI.L_DASH) | URI.L_UNDERSCORE, (URI.H_ALPHANUM | URI.H_DASH) | URI.H_UNDERSCORE);
                    if (q > p) {
                        if (charAt(q - 1) == '-') {
                            fail("Illegal character in hostname", q - 1);
                        }
                        p = q;
                    }
                }
                q = scan(p, n, '.');
                if (q <= p) {
                    break;
                }
                p = q;
            } while (p < n);
            if (p < n && !at(p, n, ':')) {
                fail("Illegal character in hostname", p);
            }
            if (l < 0) {
                failExpecting("hostname", start);
            }
            if (l > start && !URI.match(charAt(l), 0, URI.H_ALPHA)) {
                fail("Illegal character in hostname", l);
            }
            URI.this.host = substring(start, p);
            return p;
        }

        private int parseIPv6Reference(int start, int n) throws URISyntaxException {
            int p = start;
            boolean compressedZeros = URI.$assertionsDisabled;
            int q = scanHexSeq(p, n);
            if (q > p) {
                p = q;
                if (at(p, n, "::")) {
                    compressedZeros = true;
                    p = scanHexPost(p + 2, n);
                } else if (at(p, n, ':')) {
                    p = takeIPv4Address(p + 1, n, "IPv4 address");
                    this.ipv6byteCount += 4;
                }
            } else if (at(p, n, "::")) {
                compressedZeros = true;
                p = scanHexPost(p + 2, n);
            }
            if (p < n) {
                fail("Malformed IPv6 address", start);
            }
            if (this.ipv6byteCount > 16) {
                fail("IPv6 address too long", start);
            }
            if (!compressedZeros && this.ipv6byteCount < 16) {
                fail("IPv6 address too short", start);
            }
            if (compressedZeros && this.ipv6byteCount == 16) {
                fail("Malformed IPv6 address", start);
            }
            return p;
        }

        private int scanHexPost(int start, int n) throws URISyntaxException {
            int p = start;
            if (p == n) {
                return p;
            }
            int q = scanHexSeq(p, n);
            if (q > p) {
                p = q;
                if (at(p, n, ':')) {
                    p = takeIPv4Address(p + 1, n, "hex digits or IPv4 address");
                    this.ipv6byteCount += 4;
                }
            } else {
                p = takeIPv4Address(p, n, "hex digits or IPv4 address");
                this.ipv6byteCount += 4;
            }
            return p;
        }

        private int scanHexSeq(int start, int n) throws URISyntaxException {
            int p = start;
            int q = scan(p, n, URI.L_HEX, URI.H_HEX);
            if (q <= p || at(q, n, '.')) {
                return -1;
            }
            if (q > p + 4) {
                fail("IPv6 hexadecimal digit sequence too long", p);
            }
            this.ipv6byteCount += 2;
            int p2 = q;
            while (p2 < n && at(p2, n, ':') && !at(p2 + 1, n, ':')) {
                p2++;
                q = scan(p2, n, URI.L_HEX, URI.H_HEX);
                if (q <= p2) {
                    failExpecting("digits for an IPv6 address", p2);
                }
                if (at(q, n, '.')) {
                    p2--;
                    break;
                }
                if (q > p2 + 4) {
                    fail("IPv6 hexadecimal digit sequence too long", p2);
                }
                this.ipv6byteCount += 2;
                p2 = q;
            }
            return p2;
        }
    }

    private URI() {
        this.port = -1;
        this.decodedUserInfo = null;
        this.decodedAuthority = null;
        this.decodedPath = null;
        this.decodedQuery = null;
        this.decodedFragment = null;
        this.decodedSchemeSpecificPart = null;
    }

    public URI(String str) throws URISyntaxException {
        this.port = -1;
        this.decodedUserInfo = null;
        this.decodedAuthority = null;
        this.decodedPath = null;
        this.decodedQuery = null;
        this.decodedFragment = null;
        this.decodedSchemeSpecificPart = null;
        new Parser(str).parse($assertionsDisabled);
    }

    public URI(String scheme, String userInfo, String host, int port, String path, String query, String fragment) throws URISyntaxException {
        this.port = -1;
        this.decodedUserInfo = null;
        this.decodedAuthority = null;
        this.decodedPath = null;
        this.decodedQuery = null;
        this.decodedFragment = null;
        this.decodedSchemeSpecificPart = null;
        String str = scheme;
        String s = toString(str, null, null, userInfo, host, port, path, query, fragment);
        checkPath(s, str, path);
        new Parser(s).parse(true);
    }

    public URI(String scheme, String authority, String path, String query, String fragment) throws URISyntaxException {
        this.port = -1;
        this.decodedUserInfo = null;
        this.decodedAuthority = null;
        this.decodedPath = null;
        this.decodedQuery = null;
        this.decodedFragment = null;
        this.decodedSchemeSpecificPart = null;
        String str = scheme;
        String s = toString(str, null, authority, null, null, -1, path, query, fragment);
        checkPath(s, str, path);
        new Parser(s).parse($assertionsDisabled);
    }

    public URI(String scheme, String host, String path, String fragment) throws URISyntaxException {
        this(scheme, null, host, -1, path, null, fragment);
    }

    public URI(String scheme, String ssp, String fragment) throws URISyntaxException {
        this.port = -1;
        this.decodedUserInfo = null;
        this.decodedAuthority = null;
        this.decodedPath = null;
        this.decodedQuery = null;
        this.decodedFragment = null;
        this.decodedSchemeSpecificPart = null;
        new Parser(toString(scheme, ssp, null, null, null, -1, null, null, fragment)).parse($assertionsDisabled);
    }

    public static URI create(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException x) {
            throw new IllegalArgumentException(x.getMessage(), x);
        }
    }

    public URI parseServerAuthority() throws URISyntaxException {
        if (this.host != null || this.authority == null) {
            return this;
        }
        defineString();
        new Parser(this.string).parse(true);
        return this;
    }

    public URI normalize() {
        return normalize(this);
    }

    public URI resolve(URI uri) {
        return resolve(this, uri);
    }

    public URI resolve(String str) {
        return resolve(create(str));
    }

    public URI relativize(URI uri) {
        return relativize(this, uri);
    }

    public URL toURL() throws MalformedURLException {
        if (isAbsolute()) {
            return new URL(toString());
        }
        throw new IllegalArgumentException("URI is not absolute");
    }

    public String getScheme() {
        return this.scheme;
    }

    public boolean isAbsolute() {
        return this.scheme != null ? true : $assertionsDisabled;
    }

    public boolean isOpaque() {
        return this.path == null ? true : $assertionsDisabled;
    }

    public String getRawSchemeSpecificPart() {
        defineSchemeSpecificPart();
        return this.schemeSpecificPart;
    }

    public String getSchemeSpecificPart() {
        if (this.decodedSchemeSpecificPart == null) {
            this.decodedSchemeSpecificPart = decode(getRawSchemeSpecificPart());
        }
        return this.decodedSchemeSpecificPart;
    }

    public String getRawAuthority() {
        return this.authority;
    }

    public String getAuthority() {
        if (this.decodedAuthority == null) {
            this.decodedAuthority = decode(this.authority);
        }
        return this.decodedAuthority;
    }

    public String getRawUserInfo() {
        return this.userInfo;
    }

    public String getUserInfo() {
        if (this.decodedUserInfo == null && this.userInfo != null) {
            this.decodedUserInfo = decode(this.userInfo);
        }
        return this.decodedUserInfo;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getRawPath() {
        return this.path;
    }

    public String getPath() {
        if (this.decodedPath == null && this.path != null) {
            this.decodedPath = decode(this.path);
        }
        return this.decodedPath;
    }

    public String getRawQuery() {
        return this.query;
    }

    public String getQuery() {
        if (this.decodedQuery == null && this.query != null) {
            this.decodedQuery = decode(this.query);
        }
        return this.decodedQuery;
    }

    public String getRawFragment() {
        return this.fragment;
    }

    public String getFragment() {
        if (this.decodedFragment == null && this.fragment != null) {
            this.decodedFragment = decode(this.fragment);
        }
        return this.decodedFragment;
    }

    public boolean equals(Object ob) {
        if (ob == this) {
            return true;
        }
        if (!(ob instanceof URI)) {
            return $assertionsDisabled;
        }
        URI that = (URI) ob;
        if (isOpaque() != that.isOpaque() || !equalIgnoringCase(this.scheme, that.scheme) || !equal(this.fragment, that.fragment)) {
            return $assertionsDisabled;
        }
        if (isOpaque()) {
            return equal(this.schemeSpecificPart, that.schemeSpecificPart);
        }
        if (!equal(this.path, that.path) || !equal(this.query, that.query)) {
            return $assertionsDisabled;
        }
        if (this.authority == that.authority) {
            return true;
        }
        if (this.host != null) {
            if (equal(this.userInfo, that.userInfo) && equalIgnoringCase(this.host, that.host) && this.port == that.port) {
                return true;
            }
            return $assertionsDisabled;
        } else if (this.authority != null) {
            if (!equal(this.authority, that.authority)) {
                return $assertionsDisabled;
            }
        } else if (this.authority != that.authority) {
            return $assertionsDisabled;
        }
        return true;
    }

    public int hashCode() {
        if (this.hash != 0) {
            return this.hash;
        }
        int h = hash(hashIgnoringCase(0, this.scheme), this.fragment);
        if (isOpaque()) {
            h = hash(h, this.schemeSpecificPart);
        } else {
            h = hash(hash(h, this.path), this.query);
            if (this.host != null) {
                h = hashIgnoringCase(hash(h, this.userInfo), this.host) + (1949 * this.port);
            } else {
                h = hash(h, this.authority);
            }
        }
        this.hash = h;
        return h;
    }

    public int compareTo(URI that) {
        int compareIgnoringCase = compareIgnoringCase(this.scheme, that.scheme);
        int c = compareIgnoringCase;
        if (compareIgnoringCase != 0) {
            return c;
        }
        if (isOpaque()) {
            if (!that.isOpaque()) {
                return 1;
            }
            compareIgnoringCase = compare(this.schemeSpecificPart, that.schemeSpecificPart);
            c = compareIgnoringCase;
            if (compareIgnoringCase != 0) {
                return c;
            }
            return compare(this.fragment, that.fragment);
        } else if (that.isOpaque()) {
            return -1;
        } else {
            if (this.host == null || that.host == null) {
                compareIgnoringCase = compare(this.authority, that.authority);
                c = compareIgnoringCase;
                if (compareIgnoringCase != 0) {
                    return c;
                }
            }
            compareIgnoringCase = compare(this.userInfo, that.userInfo);
            c = compareIgnoringCase;
            if (compareIgnoringCase != 0) {
                return c;
            }
            compareIgnoringCase = compareIgnoringCase(this.host, that.host);
            c = compareIgnoringCase;
            if (compareIgnoringCase != 0) {
                return c;
            }
            compareIgnoringCase = this.port - that.port;
            c = compareIgnoringCase;
            if (compareIgnoringCase != 0) {
                return c;
            }
            compareIgnoringCase = compare(this.path, that.path);
            c = compareIgnoringCase;
            if (compareIgnoringCase != 0) {
                return c;
            }
            compareIgnoringCase = compare(this.query, that.query);
            c = compareIgnoringCase;
            if (compareIgnoringCase != 0) {
                return c;
            }
            return compare(this.fragment, that.fragment);
        }
    }

    public String toString() {
        defineString();
        return this.string;
    }

    public String toASCIIString() {
        defineString();
        return encode(this.string);
    }

    private void writeObject(ObjectOutputStream os) throws IOException {
        defineString();
        os.defaultWriteObject();
    }

    private void readObject(ObjectInputStream is) throws ClassNotFoundException, IOException {
        this.port = -1;
        is.defaultReadObject();
        try {
            new Parser(this.string).parse($assertionsDisabled);
        } catch (URISyntaxException x) {
            IOException y = new InvalidObjectException("Invalid URI");
            y.initCause(x);
            throw y;
        }
    }

    private static int toLower(char c) {
        if (c < 'A' || c > 'Z') {
            return c;
        }
        return c + 32;
    }

    private static int toUpper(char c) {
        if (c < 'a' || c > 'z') {
            return c;
        }
        return c - 32;
    }

    /* JADX WARNING: Missing block: B:30:0x0069, code skipped:
            return $assertionsDisabled;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static boolean equal(String s, String t) {
        if (s == t) {
            return true;
        }
        if (s == null || t == null || s.length() != t.length()) {
            return $assertionsDisabled;
        }
        if (s.indexOf(37) < 0) {
            return s.equals(t);
        }
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            char d = t.charAt(i);
            if (c != '%') {
                if (c != d) {
                    return $assertionsDisabled;
                }
                i++;
            } else if (d != '%') {
                return $assertionsDisabled;
            } else {
                i++;
                if (toLower(s.charAt(i)) != toLower(t.charAt(i))) {
                    return $assertionsDisabled;
                }
                i++;
                if (toLower(s.charAt(i)) != toLower(t.charAt(i))) {
                    return $assertionsDisabled;
                }
                i++;
            }
        }
        return true;
    }

    private static boolean equalIgnoringCase(String s, String t) {
        if (s == t) {
            return true;
        }
        if (s == null || t == null) {
            return $assertionsDisabled;
        }
        int n = s.length();
        if (t.length() != n) {
            return $assertionsDisabled;
        }
        for (int i = 0; i < n; i++) {
            if (toLower(s.charAt(i)) != toLower(t.charAt(i))) {
                return $assertionsDisabled;
            }
        }
        return true;
    }

    private static int hash(int hash, String s) {
        if (s == null) {
            return hash;
        }
        int hashCode;
        if (s.indexOf(37) < 0) {
            hashCode = (hash * 127) + s.hashCode();
        } else {
            hashCode = normalizedHash(hash, s);
        }
        return hashCode;
    }

    private static int normalizedHash(int hash, String s) {
        int h = 0;
        int index = 0;
        while (index < s.length()) {
            char ch = s.charAt(index);
            int h2 = (31 * h) + ch;
            if (ch == '%') {
                for (h = index + 1; h < index + 3; h++) {
                    h2 = (31 * h2) + toUpper(s.charAt(h));
                }
                index += 2;
            }
            h = h2;
            index++;
        }
        return (hash * 127) + h;
    }

    private static int hashIgnoringCase(int hash, String s) {
        if (s == null) {
            return hash;
        }
        int h = hash;
        for (int i = 0; i < s.length(); i++) {
            h = (31 * h) + toLower(s.charAt(i));
        }
        return h;
    }

    private static int compare(String s, String t) {
        if (s == t) {
            return 0;
        }
        if (s == null) {
            return -1;
        }
        if (t != null) {
            return s.compareTo(t);
        }
        return 1;
    }

    private static int compareIgnoringCase(String s, String t) {
        int i = 0;
        if (s == t) {
            return 0;
        }
        if (s == null) {
            return -1;
        }
        if (t == null) {
            return 1;
        }
        int sn = s.length();
        int tn = t.length();
        int n = sn < tn ? sn : tn;
        while (i < n) {
            int c = toLower(s.charAt(i)) - toLower(t.charAt(i));
            if (c != 0) {
                return c;
            }
            i++;
        }
        return sn - tn;
    }

    private static void checkPath(String s, String scheme, String path) throws URISyntaxException {
        if (scheme != null && path != null && path.length() > 0 && path.charAt(0) != '/') {
            throw new URISyntaxException(s, "Relative path in absolute URI");
        }
    }

    private void appendAuthority(StringBuffer sb, String authority, String userInfo, String host, int port) {
        boolean needBrackets = $assertionsDisabled;
        if (host != null) {
            sb.append("//");
            if (userInfo != null) {
                sb.append(quote(userInfo, L_USERINFO, H_USERINFO));
                sb.append('@');
            }
            if (!(host.indexOf(58) < 0 || host.startsWith("[") || host.endsWith("]"))) {
                needBrackets = true;
            }
            if (needBrackets) {
                sb.append('[');
            }
            sb.append(host);
            if (needBrackets) {
                sb.append(']');
            }
            if (port != -1) {
                sb.append(':');
                sb.append(port);
            }
        } else if (authority != null) {
            sb.append("//");
            if (authority.startsWith("[")) {
                int end = authority.indexOf("]");
                String doquote = authority;
                String dontquote = "";
                if (!(end == -1 || authority.indexOf(":") == -1)) {
                    if (end == authority.length()) {
                        dontquote = authority;
                        doquote = "";
                    } else {
                        dontquote = authority.substring(0, end + 1);
                        doquote = authority.substring(end + 1);
                    }
                }
                sb.append(dontquote);
                sb.append(quote(doquote, L_REG_NAME | L_SERVER, H_REG_NAME | H_SERVER));
                return;
            }
            sb.append(quote(authority, L_REG_NAME | L_SERVER, H_REG_NAME | H_SERVER));
        }
    }

    private void appendSchemeSpecificPart(StringBuffer sb, String opaquePart, String authority, String userInfo, String host, int port, String path, String query) {
        if (opaquePart == null) {
            appendAuthority(sb, authority, userInfo, host, port);
            if (path != null) {
                sb.append(quote(path, L_PATH, H_PATH));
            }
            if (query != null) {
                sb.append('?');
                sb.append(quote(query, L_URIC, H_URIC));
            }
        } else if (opaquePart.startsWith("//[")) {
            int end = opaquePart.indexOf("]");
            if (end != -1 && opaquePart.indexOf(":") != -1) {
                String dontquote;
                String doquote;
                if (end == opaquePart.length()) {
                    dontquote = opaquePart;
                    doquote = "";
                } else {
                    dontquote = opaquePart.substring(0, end + 1);
                    doquote = opaquePart.substring(end + 1);
                }
                sb.append(dontquote);
                sb.append(quote(doquote, L_URIC, H_URIC));
            }
        } else {
            sb.append(quote(opaquePart, L_URIC, H_URIC));
        }
    }

    private void appendFragment(StringBuffer sb, String fragment) {
        if (fragment != null) {
            sb.append('#');
            sb.append(quote(fragment, L_URIC, H_URIC));
        }
    }

    private String toString(String scheme, String opaquePart, String authority, String userInfo, String host, int port, String path, String query, String fragment) {
        String str = scheme;
        StringBuffer sb = new StringBuffer();
        if (str != null) {
            sb.append(str);
            sb.append(':');
        }
        appendSchemeSpecificPart(sb, opaquePart, authority, userInfo, host, port, path, query);
        appendFragment(sb, fragment);
        return sb.toString();
    }

    private void defineSchemeSpecificPart() {
        if (this.schemeSpecificPart == null) {
            StringBuffer sb = new StringBuffer();
            appendSchemeSpecificPart(sb, null, getAuthority(), getUserInfo(), this.host, this.port, getPath(), getQuery());
            if (sb.length() != 0) {
                this.schemeSpecificPart = sb.toString();
            }
        }
    }

    private void defineString() {
        if (this.string == null) {
            StringBuffer sb = new StringBuffer();
            if (this.scheme != null) {
                sb.append(this.scheme);
                sb.append(':');
            }
            if (isOpaque()) {
                sb.append(this.schemeSpecificPart);
            } else {
                if (this.host != null) {
                    sb.append("//");
                    if (this.userInfo != null) {
                        sb.append(this.userInfo);
                        sb.append('@');
                    }
                    boolean needBrackets = (this.host.indexOf(58) < 0 || this.host.startsWith("[") || this.host.endsWith("]")) ? $assertionsDisabled : true;
                    if (needBrackets) {
                        sb.append('[');
                    }
                    sb.append(this.host);
                    if (needBrackets) {
                        sb.append(']');
                    }
                    if (this.port != -1) {
                        sb.append(':');
                        sb.append(this.port);
                    }
                } else if (this.authority != null) {
                    sb.append("//");
                    sb.append(this.authority);
                }
                if (this.path != null) {
                    sb.append(this.path);
                }
                if (this.query != null) {
                    sb.append('?');
                    sb.append(this.query);
                }
            }
            if (this.fragment != null) {
                sb.append('#');
                sb.append(this.fragment);
            }
            this.string = sb.toString();
        }
    }

    private static String resolvePath(String base, String child, boolean absolute) {
        int i = base.lastIndexOf(47);
        int cn = child.length();
        String path = "";
        if (cn != 0) {
            StringBuffer sb = new StringBuffer(base.length() + cn);
            if (i >= 0) {
                sb.append(base.substring(0, i + 1));
            }
            sb.append(child);
            path = sb.toString();
        } else if (i >= 0) {
            path = base.substring(0, i + 1);
        }
        return normalize(path, true);
    }

    private static URI resolve(URI base, URI child) {
        if (child.isOpaque() || base.isOpaque()) {
            return child;
        }
        URI ru;
        if (child.scheme == null && child.authority == null && child.path.equals("") && child.fragment != null && child.query == null) {
            if (base.fragment != null && child.fragment.equals(base.fragment)) {
                return base;
            }
            ru = new URI();
            ru.scheme = base.scheme;
            ru.authority = base.authority;
            ru.userInfo = base.userInfo;
            ru.host = base.host;
            ru.port = base.port;
            ru.path = base.path;
            ru.fragment = child.fragment;
            ru.query = base.query;
            return ru;
        } else if (child.scheme != null) {
            return child;
        } else {
            ru = new URI();
            ru.scheme = base.scheme;
            ru.query = child.query;
            ru.fragment = child.fragment;
            if (child.authority == null) {
                ru.authority = base.authority;
                ru.host = base.host;
                ru.userInfo = base.userInfo;
                ru.port = base.port;
                if (child.path == null || child.path.isEmpty()) {
                    ru.path = base.path;
                    ru.query = child.query != null ? child.query : base.query;
                } else if (child.path.length() <= 0 || child.path.charAt(0) != '/') {
                    ru.path = resolvePath(base.path, child.path, base.isAbsolute());
                } else {
                    ru.path = normalize(child.path, true);
                }
            } else {
                ru.authority = child.authority;
                ru.host = child.host;
                ru.userInfo = child.userInfo;
                ru.host = child.host;
                ru.port = child.port;
                ru.path = child.path;
            }
            return ru;
        }
    }

    private static URI normalize(URI u) {
        if (u.isOpaque() || u.path == null || u.path.length() == 0) {
            return u;
        }
        String np = normalize(u.path);
        if (np == u.path) {
            return u;
        }
        URI v = new URI();
        v.scheme = u.scheme;
        v.fragment = u.fragment;
        v.authority = u.authority;
        v.userInfo = u.userInfo;
        v.host = u.host;
        v.port = u.port;
        v.path = np;
        v.query = u.query;
        return v;
    }

    /* JADX WARNING: Missing block: B:19:0x0068, code skipped:
            return r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static URI relativize(URI base, URI child) {
        if (child.isOpaque() || base.isOpaque() || !equalIgnoringCase(base.scheme, child.scheme) || !equal(base.authority, child.authority)) {
            return child;
        }
        String bp = normalize(base.path);
        String cp = normalize(child.path);
        if (!bp.equals(cp)) {
            if (bp.indexOf(47) != -1) {
                bp = bp.substring(0, bp.lastIndexOf(47) + 1);
            }
            if (!cp.startsWith(bp)) {
                return child;
            }
        }
        URI v = new URI();
        v.path = cp.substring(bp.length());
        v.query = child.query;
        v.fragment = child.fragment;
        return v;
    }

    private static int needsNormalization(String path) {
        boolean normal = true;
        int ns = 0;
        int end = path.length() - 1;
        int p = 0;
        while (p <= end && path.charAt(p) == '/') {
            p++;
        }
        if (p > 1) {
            normal = $assertionsDisabled;
        }
        while (p <= end) {
            if (path.charAt(p) == '.' && (p == end || path.charAt(p + 1) == '/' || (path.charAt(p + 1) == '.' && (p + 1 == end || path.charAt(p + 2) == '/')))) {
                normal = $assertionsDisabled;
            }
            ns++;
            while (p <= end) {
                int p2 = p + 1;
                if (path.charAt(p) != 47) {
                    p = p2;
                } else {
                    p = p2;
                    while (p <= end) {
                        if (path.charAt(p) != '/') {
                            break;
                        }
                        normal = $assertionsDisabled;
                        p++;
                    }
                }
            }
        }
        return normal ? -1 : ns;
    }

    private static void split(char[] path, int[] segs) {
        int end = path.length - 1;
        int p = 0;
        int i = 0;
        while (p <= end && path[p] == '/') {
            path[p] = 0;
            p++;
        }
        while (p <= end) {
            int i2 = i + 1;
            int p2 = p + 1;
            segs[i] = p;
            p = p2;
            while (p <= end) {
                i = p + 1;
                if (path[p] != 47) {
                    p = i;
                } else {
                    path[i - 1] = 0;
                    while (true) {
                        p = i;
                        if (p > end || path[p] != '/') {
                            break;
                        }
                        i = p + 1;
                        path[p] = 0;
                    }
                    i = i2;
                }
            }
            i = i2;
        }
        if (i != segs.length) {
            throw new InternalError();
        }
    }

    private static int join(char[] path, int[] segs) {
        int p;
        int end = path.length - 1;
        int p2 = 0;
        if (path[0] == 0) {
            p = 0 + 1;
            path[0] = '/';
            p2 = p;
        }
        for (int q : segs) {
            int q2;
            if (q2 != -1) {
                int p3;
                if (p2 == q2) {
                    while (p2 <= end && path[p2] != 0) {
                        p2++;
                    }
                    if (p2 <= end) {
                        p3 = p2 + 1;
                        path[p2] = '/';
                    }
                } else if (p2 < q2) {
                    while (q2 <= end && path[q2] != 0) {
                        p3 = p2 + 1;
                        int q3 = q2 + 1;
                        path[p2] = path[q2];
                        p2 = p3;
                        q2 = q3;
                    }
                    if (q2 <= end) {
                        p3 = p2 + 1;
                        path[p2] = '/';
                    }
                } else {
                    throw new InternalError();
                }
                p2 = p3;
            }
        }
        return p2;
    }

    private static void removeDots(char[] path, int[] segs, boolean removeLeading) {
        int ns = segs.length;
        int end = path.length - 1;
        int i = 0;
        while (i < ns) {
            int i2 = i;
            i = 0;
            do {
                int p = segs[i2];
                if (path[p] == '.') {
                    if (p != end) {
                        if (path[p + 1] != 0) {
                            if (path[p + 1] == '.' && (p + 1 == end || path[p + 2] == 0)) {
                                i = 2;
                                break;
                            }
                        }
                        i = 1;
                        break;
                    }
                    i = 1;
                    break;
                }
                i2++;
            } while (i2 < ns);
            if (i2 <= ns && i != 0) {
                if (i == 1) {
                    segs[i2] = -1;
                } else {
                    int j = i2 - 1;
                    while (j >= 0 && segs[j] == -1) {
                        j--;
                    }
                    if (j >= 0) {
                        int q = segs[j];
                        if (path[q] != '.' || path[q + 1] != '.' || path[q + 2] != 0) {
                            segs[i2] = -1;
                            segs[j] = -1;
                        }
                    } else if (removeLeading) {
                        segs[i2] = -1;
                    }
                }
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    private static void maybeAddLeadingDot(char[] path, int[] segs) {
        if (path[0] != 0) {
            int ns = segs.length;
            int f = 0;
            while (f < ns && segs[f] < 0) {
                f++;
            }
            if (f < ns && f != 0) {
                int p = segs[f];
                while (p < path.length && path[p] != ':' && path[p] != 0) {
                    p++;
                }
                if (p < path.length && path[p] != 0) {
                    path[0] = '.';
                    path[1] = 0;
                    segs[0] = 0;
                }
            }
        }
    }

    private static String normalize(String ps) {
        return normalize(ps, $assertionsDisabled);
    }

    private static String normalize(String ps, boolean removeLeading) {
        int ns = needsNormalization(ps);
        if (ns < 0) {
            return ps;
        }
        char[] path = ps.toCharArray();
        int[] segs = new int[ns];
        split(path, segs);
        removeDots(path, segs, removeLeading);
        maybeAddLeadingDot(path, segs);
        String s = new String(path, 0, join(path, segs));
        if (s.equals(ps)) {
            return ps;
        }
        return s;
    }

    private static long lowMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if (c < '@') {
                m |= L_ESCAPED << c;
            }
        }
        return m;
    }

    private static long highMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if (c >= '@' && c < 128) {
                m |= L_ESCAPED << (c - 64);
            }
        }
        return m;
    }

    private static long lowMask(char first, char last) {
        long m = 0;
        for (int i = Math.max(Math.min((int) first, 63), 0); i <= Math.max(Math.min((int) last, 63), 0); i++) {
            m |= L_ESCAPED << i;
        }
        return m;
    }

    private static long highMask(char first, char last) {
        long m = 0;
        for (int i = Math.max(Math.min((int) first, 127), 64) - 64; i <= Math.max(Math.min((int) last, 127), 64) - 64; i++) {
            m |= L_ESCAPED << i;
        }
        return m;
    }

    private static boolean match(char c, long lowMask, long highMask) {
        boolean z = $assertionsDisabled;
        if (c == 0) {
            return $assertionsDisabled;
        }
        if (c < '@') {
            if (((L_ESCAPED << c) & lowMask) != 0) {
                z = true;
            }
            return z;
        } else if (c >= 128) {
            return $assertionsDisabled;
        } else {
            if (((L_ESCAPED << (c - 64)) & highMask) != 0) {
                z = true;
            }
            return z;
        }
    }

    private static void appendEscape(StringBuffer sb, byte b) {
        sb.append('%');
        sb.append(hexDigits[(b >> 4) & 15]);
        sb.append(hexDigits[(b >> 0) & 15]);
    }

    private static void appendEncoded(StringBuffer sb, char c) {
        ByteBuffer bb = null;
        try {
            CharsetEncoder encoderFor = ThreadLocalCoders.encoderFor("UTF-8");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("");
            stringBuilder.append(c);
            bb = encoderFor.encode(CharBuffer.wrap(stringBuilder.toString()));
        } catch (CharacterCodingException e) {
        }
        while (bb.hasRemaining()) {
            int b = bb.get() & 255;
            if (b >= 128) {
                appendEscape(sb, (byte) b);
            } else {
                sb.append((char) b);
            }
        }
    }

    private static String quote(String s, long lowMask, long highMask) {
        int n = s.length();
        boolean allowNonASCII = (L_ESCAPED & lowMask) != 0 ? true : $assertionsDisabled;
        StringBuffer sb = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 128) {
                if (!match(c, lowMask, highMask)) {
                    if (sb == null) {
                        sb = new StringBuffer();
                        sb.append(s.substring(0, i));
                    }
                    appendEscape(sb, (byte) c);
                } else if (sb != null) {
                    sb.append(c);
                }
            } else if (allowNonASCII && (Character.isSpaceChar(c) || Character.isISOControl(c))) {
                if (sb == null) {
                    sb = new StringBuffer();
                    sb.append(s.substring(0, i));
                }
                appendEncoded(sb, c);
            } else if (sb != null) {
                sb.append(c);
            }
        }
        return sb == null ? s : sb.toString();
    }

    private static String encode(String s) {
        int n = s.length();
        if (n == 0) {
            return s;
        }
        int i = 0;
        while (s.charAt(i) < 128) {
            i++;
            if (i >= n) {
                return s;
            }
        }
        ByteBuffer bb = null;
        try {
            bb = ThreadLocalCoders.encoderFor("UTF-8").encode(CharBuffer.wrap(Normalizer.normalize(s, Form.NFC)));
        } catch (CharacterCodingException e) {
        }
        StringBuffer sb = new StringBuffer();
        while (bb.hasRemaining()) {
            int b = bb.get() & 255;
            if (b >= 128) {
                appendEscape(sb, (byte) b);
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }

    private static int decode(char c) {
        if (c >= '0' && c <= '9') {
            return c - 48;
        }
        if (c >= 'a' && c <= 'f') {
            return (c - 97) + 10;
        }
        if (c < 'A' || c > 'F') {
            return -1;
        }
        return (c - 65) + 10;
    }

    private static byte decode(char c1, char c2) {
        return (byte) (((decode(c1) & 15) << 4) | ((decode(c2) & 15) << 0));
    }

    private static String decode(String s) {
        if (s == null) {
            return s;
        }
        char n = s.length();
        if (n == 0 || s.indexOf(37) < 0) {
            return s;
        }
        StringBuffer sb = new StringBuffer((int) n);
        ByteBuffer bb = ByteBuffer.allocate(n);
        CharBuffer cb = CharBuffer.allocate(n);
        CharsetDecoder dec = ThreadLocalCoders.decoderFor("UTF-8").onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        char i = 0;
        char c = s.charAt(0);
        boolean betweenBrackets = $assertionsDisabled;
        while (i < n) {
            if (c == '[') {
                betweenBrackets = true;
            } else if (betweenBrackets && c == ']') {
                betweenBrackets = $assertionsDisabled;
            }
            if (c != '%' || betweenBrackets) {
                sb.append(c);
                i++;
                if (i >= n) {
                    break;
                }
                c = s.charAt(i);
            } else {
                bb.clear();
                char c2 = c;
                c = i;
                do {
                    c++;
                    char charAt = s.charAt(c);
                    c++;
                    bb.put(decode(charAt, s.charAt(c)));
                    c++;
                    if (c >= n) {
                        break;
                    }
                    c2 = s.charAt(c);
                } while (c2 == '%');
                bb.flip();
                cb.clear();
                dec.reset();
                CoderResult cr = dec.decode(bb, cb, true);
                cr = dec.flush(cb);
                sb.append(cb.flip().toString());
                i = c;
                c = c2;
            }
        }
        return sb.toString();
    }
}
