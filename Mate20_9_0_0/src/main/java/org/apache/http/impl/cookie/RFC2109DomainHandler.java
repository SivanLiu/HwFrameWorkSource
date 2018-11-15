package org.apache.http.impl.cookie;

import java.util.Locale;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class RFC2109DomainHandler implements CookieAttributeHandler {
    public void parse(SetCookie cookie, String value) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        } else if (value == null) {
            throw new MalformedCookieException("Missing value for domain attribute");
        } else if (value.trim().length() != 0) {
            cookie.setDomain(value);
        } else {
            throw new MalformedCookieException("Blank value for domain attribute");
        }
    }

    public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        } else if (origin != null) {
            String host = origin.getHost();
            String domain = cookie.getDomain();
            if (domain == null) {
                throw new MalformedCookieException("Cookie domain may not be null");
            } else if (!domain.equals(host)) {
                StringBuilder stringBuilder;
                if (domain.indexOf(46) == -1) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Domain attribute \"");
                    stringBuilder.append(domain);
                    stringBuilder.append("\" does not match the host \"");
                    stringBuilder.append(host);
                    stringBuilder.append("\"");
                    throw new MalformedCookieException(stringBuilder.toString());
                } else if (domain.startsWith(".")) {
                    int dotIndex = domain.indexOf(46, 1);
                    if (dotIndex < 0 || dotIndex == domain.length() - 1) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Domain attribute \"");
                        stringBuilder.append(domain);
                        stringBuilder.append("\" violates RFC 2109: domain must contain an embedded dot");
                        throw new MalformedCookieException(stringBuilder.toString());
                    }
                    host = host.toLowerCase(Locale.ENGLISH);
                    if (!host.endsWith(domain)) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Illegal domain attribute \"");
                        stringBuilder.append(domain);
                        stringBuilder.append("\". Domain of origin: \"");
                        stringBuilder.append(host);
                        stringBuilder.append("\"");
                        throw new MalformedCookieException(stringBuilder.toString());
                    } else if (host.substring(null, host.length() - domain.length()).indexOf(46) != -1) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Domain attribute \"");
                        stringBuilder.append(domain);
                        stringBuilder.append("\" violates RFC 2109: host minus domain may not contain any dots");
                        throw new MalformedCookieException(stringBuilder.toString());
                    }
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Domain attribute \"");
                    stringBuilder.append(domain);
                    stringBuilder.append("\" violates RFC 2109: domain must start with a dot");
                    throw new MalformedCookieException(stringBuilder.toString());
                }
            }
        } else {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
    }

    public boolean match(Cookie cookie, CookieOrigin origin) {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        } else if (origin != null) {
            String host = origin.getHost();
            String domain = cookie.getDomain();
            boolean z = false;
            if (domain == null) {
                return false;
            }
            if (host.equals(domain) || (domain.startsWith(".") && host.endsWith(domain))) {
                z = true;
            }
            return z;
        } else {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
    }
}
