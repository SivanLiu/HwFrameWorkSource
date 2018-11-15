package org.apache.http.client.protocol;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SM;
import org.apache.http.protocol.HttpContext;

@Deprecated
public class ResponseProcessCookies implements HttpResponseInterceptor {
    private final Log log = LogFactory.getLog(getClass());

    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        if (response == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        } else if (context != null) {
            CookieStore cookieStore = (CookieStore) context.getAttribute(ClientContext.COOKIE_STORE);
            if (cookieStore == null) {
                this.log.info("Cookie store not available in HTTP context");
                return;
            }
            CookieSpec cookieSpec = (CookieSpec) context.getAttribute(ClientContext.COOKIE_SPEC);
            if (cookieSpec == null) {
                this.log.info("CookieSpec not available in HTTP context");
                return;
            }
            CookieOrigin cookieOrigin = (CookieOrigin) context.getAttribute(ClientContext.COOKIE_ORIGIN);
            if (cookieOrigin == null) {
                this.log.info("CookieOrigin not available in HTTP context");
                return;
            }
            processCookies(response.headerIterator(SM.SET_COOKIE), cookieSpec, cookieOrigin, cookieStore);
            if (cookieSpec.getVersion() > 0) {
                processCookies(response.headerIterator(SM.SET_COOKIE2), cookieSpec, cookieOrigin, cookieStore);
            }
        } else {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
    }

    private void processCookies(HeaderIterator iterator, CookieSpec cookieSpec, CookieOrigin cookieOrigin, CookieStore cookieStore) {
        while (iterator.hasNext()) {
            Header header = iterator.nextHeader();
            try {
                for (Cookie cookie : cookieSpec.parse(header, cookieOrigin)) {
                    try {
                        cookieSpec.validate(cookie, cookieOrigin);
                        cookieStore.addCookie(cookie);
                        if (this.log.isDebugEnabled()) {
                            Log log = this.log;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Cookie accepted: \"");
                            stringBuilder.append(cookieToString(cookie));
                            stringBuilder.append("\". ");
                            log.debug(stringBuilder.toString());
                        }
                    } catch (MalformedCookieException ex) {
                        if (this.log.isWarnEnabled()) {
                            Log log2 = this.log;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Cookie rejected: \"");
                            stringBuilder2.append(cookieToString(cookie));
                            stringBuilder2.append("\". ");
                            stringBuilder2.append(ex.getMessage());
                            log2.warn(stringBuilder2.toString());
                        }
                    }
                }
            } catch (MalformedCookieException ex2) {
                if (this.log.isWarnEnabled()) {
                    Log log3 = this.log;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Invalid cookie header: \"");
                    stringBuilder3.append(header);
                    stringBuilder3.append("\". ");
                    stringBuilder3.append(ex2.getMessage());
                    log3.warn(stringBuilder3.toString());
                }
            }
        }
    }

    private String cookieToString(Cookie cookie) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(cookie.getClass().getSimpleName());
        stringBuilder.append("[version=");
        stringBuilder.append(cookie.getVersion());
        stringBuilder.append(",name=");
        stringBuilder.append(cookie.getName());
        stringBuilder.append(",domain=");
        stringBuilder.append(cookie.getDomain());
        stringBuilder.append(",path=");
        stringBuilder.append(cookie.getPath());
        stringBuilder.append(",expiry=");
        stringBuilder.append(cookie.getExpiryDate());
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
