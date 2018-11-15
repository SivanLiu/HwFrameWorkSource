package org.apache.http.impl.cookie;

import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieAttributeHandler;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class BasicPathHandler implements CookieAttributeHandler {
    public void parse(SetCookie cookie, String value) throws MalformedCookieException {
        if (cookie != null) {
            if (value == null || value.trim().length() == 0) {
                value = "/";
            }
            cookie.setPath(value);
            return;
        }
        throw new IllegalArgumentException("Cookie may not be null");
    }

    public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
        if (!match(cookie, origin)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Illegal path attribute \"");
            stringBuilder.append(cookie.getPath());
            stringBuilder.append("\". Path of origin: \"");
            stringBuilder.append(origin.getPath());
            stringBuilder.append("\"");
            throw new MalformedCookieException(stringBuilder.toString());
        }
    }

    public boolean match(Cookie cookie, CookieOrigin origin) {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        } else if (origin != null) {
            String targetpath = origin.getPath();
            String topmostPath = cookie.getPath();
            if (topmostPath == null) {
                topmostPath = "/";
            }
            boolean z = false;
            if (topmostPath.length() > 1 && topmostPath.endsWith("/")) {
                topmostPath = topmostPath.substring(0, topmostPath.length() - 1);
            }
            boolean match = targetpath.startsWith(topmostPath);
            if (!match || targetpath.length() == topmostPath.length() || topmostPath.endsWith("/")) {
                return match;
            }
            if (targetpath.charAt(topmostPath.length()) == '/') {
                z = true;
            }
            return z;
        } else {
            throw new IllegalArgumentException("Cookie origin may not be null");
        }
    }
}
