package org.apache.http.impl.cookie;

import java.util.Locale;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class RFC2965DomainAttributeHandler implements CookieAttributeHandler {
    public void parse(SetCookie cookie, String domain) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        } else if (domain == null) {
            throw new MalformedCookieException("Missing value for domain attribute");
        } else if (domain.trim().length() != 0) {
            domain = domain.toLowerCase(Locale.ENGLISH);
            if (!domain.startsWith(".")) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append('.');
                stringBuilder.append(domain);
                domain = stringBuilder.toString();
            }
            cookie.setDomain(domain);
        } else {
            throw new MalformedCookieException("Blank value for domain attribute");
        }
    }

    public boolean domainMatch(String host, String domain) {
        return host.equals(domain) || (domain.startsWith(".") && host.endsWith(domain));
    }

    public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        } else if (origin != null) {
            String host = origin.getHost().toLowerCase(Locale.ENGLISH);
            if (cookie.getDomain() != null) {
                String cookieDomain = cookie.getDomain().toLowerCase(Locale.ENGLISH);
                StringBuilder stringBuilder;
                if ((cookie instanceof ClientCookie) && ((ClientCookie) cookie).containsAttribute(ClientCookie.DOMAIN_ATTR)) {
                    if (cookieDomain.startsWith(".")) {
                        int dotIndex = cookieDomain.indexOf(46, 1);
                        if ((dotIndex < 0 || dotIndex == cookieDomain.length() - 1) && !cookieDomain.equals(".local")) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Domain attribute \"");
                            stringBuilder.append(cookie.getDomain());
                            stringBuilder.append("\" violates RFC 2965: the value contains no embedded dots and the value is not .local");
                            throw new MalformedCookieException(stringBuilder.toString());
                        } else if (!domainMatch(host, cookieDomain)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Domain attribute \"");
                            stringBuilder.append(cookie.getDomain());
                            stringBuilder.append("\" violates RFC 2965: effective host name does not domain-match domain attribute.");
                            throw new MalformedCookieException(stringBuilder.toString());
                        } else if (host.substring(null, host.length() - cookieDomain.length()).indexOf(46) != -1) {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Domain attribute \"");
                            stringBuilder2.append(cookie.getDomain());
                            stringBuilder2.append("\" violates RFC 2965: effective host minus domain may not contain any dots");
                            throw new MalformedCookieException(stringBuilder2.toString());
                        } else {
                            return;
                        }
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Domain attribute \"");
                    stringBuilder.append(cookie.getDomain());
                    stringBuilder.append("\" violates RFC 2109: domain must start with a dot");
                    throw new MalformedCookieException(stringBuilder.toString());
                } else if (!cookie.getDomain().equals(host)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Illegal domain attribute: \"");
                    stringBuilder.append(cookie.getDomain());
                    stringBuilder.append("\".Domain of origin: \"");
                    stringBuilder.append(host);
                    stringBuilder.append("\"");
                    throw new MalformedCookieException(stringBuilder.toString());
                } else {
                    return;
                }
            }
            throw new MalformedCookieException("Invalid cookie state: domain not specified");
        } else {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
    }

    public boolean match(Cookie cookie, CookieOrigin origin) {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        } else if (origin != null) {
            String host = origin.getHost().toLowerCase(Locale.ENGLISH);
            String cookieDomain = cookie.getDomain();
            boolean z = false;
            if (!domainMatch(host, cookieDomain)) {
                return false;
            }
            if (host.substring(0, host.length() - cookieDomain.length()).indexOf(46) == -1) {
                z = true;
            }
            return z;
        } else {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
    }
}
