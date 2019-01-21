package sun.nio.fs;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;

class UnixUriUtils {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final long H_ALPHA = (H_LOWALPHA | H_UPALPHA);
    private static final long H_ALPHANUM = (H_ALPHA | 0);
    private static final long H_DIGIT = 0;
    private static final long H_LOWALPHA = highMask('a', 'z');
    private static final long H_MARK = highMask("-_.!~*'()");
    private static final long H_PATH = (H_PCHAR | highMask(";/"));
    private static final long H_PCHAR = (H_UNRESERVED | highMask(":@&=+$,"));
    private static final long H_UNRESERVED = (H_ALPHANUM | H_MARK);
    private static final long H_UPALPHA = highMask('A', 'Z');
    private static final long L_ALPHA = 0;
    private static final long L_ALPHANUM = (L_DIGIT | 0);
    private static final long L_DIGIT = lowMask('0', '9');
    private static final long L_LOWALPHA = 0;
    private static final long L_MARK = lowMask("-_.!~*'()");
    private static final long L_PATH = (L_PCHAR | lowMask(";/"));
    private static final long L_PCHAR = (L_UNRESERVED | lowMask(":@&=+$,"));
    private static final long L_UNRESERVED = (L_ALPHANUM | L_MARK);
    private static final long L_UPALPHA = 0;
    private static final char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private UnixUriUtils() {
    }

    static Path fromUri(UnixFileSystem fs, URI uri) {
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("URI is not absolute");
        } else if (uri.isOpaque()) {
            throw new IllegalArgumentException("URI is not hierarchical");
        } else {
            String scheme = uri.getScheme();
            if (scheme == null || !scheme.equalsIgnoreCase("file")) {
                throw new IllegalArgumentException("URI scheme is not \"file\"");
            } else if (uri.getAuthority() != null) {
                throw new IllegalArgumentException("URI has an authority component");
            } else if (uri.getFragment() != null) {
                throw new IllegalArgumentException("URI has a fragment component");
            } else if (uri.getQuery() != null) {
                throw new IllegalArgumentException("URI has a query component");
            } else if (!uri.toString().startsWith("file:///")) {
                return new File(uri).toPath();
            } else {
                String p = uri.getRawPath();
                char len = p.length();
                if (len != 0) {
                    if (p.endsWith("/") && len > 1) {
                        len--;
                    }
                    byte[] result = new byte[len];
                    int rlen = 0;
                    char c = 0;
                    while (c < len) {
                        byte b;
                        char pos = c + 1;
                        c = p.charAt(c);
                        if (c == '%') {
                            char c2 = pos + 1;
                            char pos2 = c2 + 1;
                            b = (byte) ((decode(p.charAt(pos)) << 4) | decode(p.charAt(c2)));
                            if (b != (byte) 0) {
                                pos = pos2;
                            } else {
                                throw new IllegalArgumentException("Nul character not allowed");
                            }
                        }
                        b = (byte) c;
                        int rlen2 = rlen + 1;
                        result[rlen] = b;
                        c = pos;
                        rlen = rlen2;
                    }
                    if (rlen != result.length) {
                        result = Arrays.copyOf(result, rlen);
                    }
                    return new UnixPath(fs, result);
                }
                throw new IllegalArgumentException("URI path component is empty");
            }
        }
    }

    static URI toUri(UnixPath up) {
        byte[] path = up.toAbsolutePath().asByteArray();
        StringBuilder sb = new StringBuilder("file:///");
        for (int i = 1; i < path.length; i++) {
            char c = (char) (path[i] & 255);
            if (match(c, L_PATH, H_PATH)) {
                sb.append(c);
            } else {
                sb.append('%');
                sb.append(hexDigits[(c >> 4) & 15]);
                sb.append(hexDigits[c & 15]);
            }
        }
        if (sb.charAt(sb.length() - 1) != '/') {
            try {
                if (UnixFileAttributes.get(up, true).isDirectory()) {
                    sb.append('/');
                }
            } catch (UnixException e) {
            }
        }
        try {
            return new URI(sb.toString());
        } catch (URISyntaxException x) {
            throw new AssertionError(x);
        }
    }

    private static long lowMask(String chars) {
        int n = chars.length();
        long m = 0;
        for (int i = 0; i < n; i++) {
            char c = chars.charAt(i);
            if (c < '@') {
                m |= 1 << c;
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
                m |= 1 << (c - 64);
            }
        }
        return m;
    }

    private static long lowMask(char first, char last) {
        long m = 0;
        for (int i = Math.max(Math.min((int) first, 63), 0); i <= Math.max(Math.min((int) last, 63), 0); i++) {
            m |= 1 << i;
        }
        return m;
    }

    private static long highMask(char first, char last) {
        long m = 0;
        for (int i = Math.max(Math.min((int) first, 127), 64) - 64; i <= Math.max(Math.min((int) last, 127), 64) - 64; i++) {
            m |= 1 << i;
        }
        return m;
    }

    private static boolean match(char c, long lowMask, long highMask) {
        boolean z = true;
        if (c < '@') {
            if (((1 << c) & lowMask) == 0) {
                z = false;
            }
            return z;
        } else if (c >= 128) {
            return false;
        } else {
            if (((1 << (c - 64)) & highMask) == 0) {
                z = false;
            }
            return z;
        }
    }

    private static int decode(char c) {
        if (c >= '0' && c <= '9') {
            return c - 48;
        }
        if (c >= 'a' && c <= 'f') {
            return (c - 97) + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return (c - 65) + 10;
        }
        throw new AssertionError();
    }
}
