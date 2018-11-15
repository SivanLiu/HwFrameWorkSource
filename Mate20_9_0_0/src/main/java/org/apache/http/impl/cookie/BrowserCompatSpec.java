package org.apache.http.impl.cookie;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.http.FormattedHeader;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SM;
import org.apache.http.message.BufferedHeader;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public class BrowserCompatSpec extends CookieSpecBase {
    protected static final String[] DATE_PATTERNS = new String[]{"EEE, dd MMM yyyy HH:mm:ss zzz", DateUtils.PATTERN_RFC1036, DateUtils.PATTERN_ASCTIME, "EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd-MMM-yyyy HH-mm-ss z", "EEE, dd MMM yy HH:mm:ss z", "EEE dd-MMM-yyyy HH:mm:ss z", "EEE dd MMM yyyy HH:mm:ss z", "EEE dd-MMM-yyyy HH-mm-ss z", "EEE dd-MMM-yy HH:mm:ss z", "EEE dd MMM yy HH:mm:ss z", "EEE,dd-MMM-yy HH:mm:ss z", "EEE,dd-MMM-yyyy HH:mm:ss z", "EEE, dd-MM-yyyy HH:mm:ss z"};
    private final String[] datepatterns;

    public BrowserCompatSpec(String[] datepatterns) {
        if (datepatterns != null) {
            this.datepatterns = (String[]) datepatterns.clone();
        } else {
            this.datepatterns = DATE_PATTERNS;
        }
        registerAttribHandler(ClientCookie.PATH_ATTR, new BasicPathHandler());
        registerAttribHandler(ClientCookie.DOMAIN_ATTR, new BasicDomainHandler());
        registerAttribHandler(ClientCookie.MAX_AGE_ATTR, new BasicMaxAgeHandler());
        registerAttribHandler(ClientCookie.SECURE_ATTR, new BasicSecureHandler());
        registerAttribHandler(ClientCookie.COMMENT_ATTR, new BasicCommentHandler());
        registerAttribHandler("expires", new BasicExpiresHandler(this.datepatterns));
    }

    public BrowserCompatSpec() {
        this(null);
    }

    public List<Cookie> parse(Header header, CookieOrigin origin) throws MalformedCookieException {
        if (header == null) {
            throw new IllegalArgumentException("Header may not be null");
        } else if (origin != null) {
            HeaderElement[] elems;
            String headervalue = header.getValue();
            boolean isNetscapeCookie = false;
            int i1 = headervalue.toLowerCase(Locale.ENGLISH).indexOf("expires=");
            if (i1 != -1) {
                i1 += "expires=".length();
                int i2 = headervalue.indexOf(59, i1);
                if (i2 == -1) {
                    i2 = headervalue.length();
                }
                try {
                    DateUtils.parseDate(headervalue.substring(i1, i2), this.datepatterns);
                    isNetscapeCookie = true;
                } catch (DateParseException e) {
                }
            }
            if (isNetscapeCookie) {
                String s;
                ParserCursor cursor;
                NetscapeDraftHeaderParser parser = NetscapeDraftHeaderParser.DEFAULT;
                if (header instanceof FormattedHeader) {
                    s = ((FormattedHeader) header).getBuffer();
                    cursor = new ParserCursor(((FormattedHeader) header).getValuePos(), s.length());
                } else {
                    s = header.getValue();
                    if (s != null) {
                        CharArrayBuffer buffer = new CharArrayBuffer(s.length());
                        buffer.append(s);
                        s = buffer;
                        cursor = new ParserCursor(0, buffer.length());
                    } else {
                        throw new MalformedCookieException("Header value is null");
                    }
                }
                elems = new HeaderElement[]{parser.parseHeader(s, cursor)};
            } else {
                elems = header.getElements();
            }
            return parse(elems, origin);
        } else {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
    }

    public List<Header> formatCookies(List<Cookie> cookies) {
        if (cookies == null) {
            throw new IllegalArgumentException("List of cookies may not be null");
        } else if (cookies.isEmpty()) {
            throw new IllegalArgumentException("List of cookies may not be empty");
        } else {
            CharArrayBuffer buffer = new CharArrayBuffer(20 * cookies.size());
            buffer.append(SM.COOKIE);
            buffer.append(": ");
            for (int i = 0; i < cookies.size(); i++) {
                Cookie cookie = (Cookie) cookies.get(i);
                if (i > 0) {
                    buffer.append("; ");
                }
                buffer.append(cookie.getName());
                buffer.append("=");
                String s = cookie.getValue();
                if (s != null) {
                    buffer.append(s);
                }
            }
            List<Header> headers = new ArrayList(1);
            headers.add(new BufferedHeader(buffer));
            return headers;
        }
    }

    public int getVersion() {
        return 0;
    }

    public Header getVersionHeader() {
        return null;
    }
}
