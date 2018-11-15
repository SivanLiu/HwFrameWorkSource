package org.apache.http.impl.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.FormattedHeader;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthSchemeRegistry;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.AuthenticationHandler;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public abstract class AbstractAuthenticationHandler implements AuthenticationHandler {
    private static final List<String> DEFAULT_SCHEME_PRIORITY = Arrays.asList(new String[]{"ntlm", "digest", "basic"});
    private final Log log = LogFactory.getLog(getClass());

    protected Map<String, Header> parseChallenges(Header[] headers) throws MalformedChallengeException {
        Map<String, Header> map = new HashMap(headers.length);
        for (Header header : headers) {
            String s;
            int beginIndex;
            if (header instanceof FormattedHeader) {
                s = ((FormattedHeader) header).getBuffer();
                beginIndex = ((FormattedHeader) header).getValuePos();
            } else {
                s = header.getValue();
                if (s != null) {
                    CharArrayBuffer buffer = new CharArrayBuffer(s.length());
                    buffer.append(s);
                    s = buffer;
                    beginIndex = 0;
                } else {
                    throw new MalformedChallengeException("Header value is null");
                }
            }
            while (beginIndex < s.length() && HTTP.isWhitespace(s.charAt(beginIndex))) {
                beginIndex++;
            }
            int pos = beginIndex;
            while (pos < s.length() && !HTTP.isWhitespace(s.charAt(pos))) {
                pos++;
            }
            map.put(s.substring(beginIndex, pos).toLowerCase(Locale.ENGLISH), header);
        }
        return map;
    }

    protected List<String> getAuthPreferences() {
        return DEFAULT_SCHEME_PRIORITY;
    }

    public AuthScheme selectScheme(Map<String, Header> challenges, HttpResponse response, HttpContext context) throws AuthenticationException {
        AuthSchemeRegistry registry = (AuthSchemeRegistry) context.getAttribute(ClientContext.AUTHSCHEME_REGISTRY);
        if (registry != null) {
            List<?> authPrefs = (List) context.getAttribute(ClientContext.AUTH_SCHEME_PREF);
            if (authPrefs == null) {
                authPrefs = getAuthPreferences();
            }
            if (this.log.isDebugEnabled()) {
                Log log = this.log;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Authentication schemes in the order of preference: ");
                stringBuilder.append(authPrefs);
                log.debug(stringBuilder.toString());
            }
            AuthScheme authScheme = null;
            int i = 0;
            while (i < authPrefs.size()) {
                String id = (String) authPrefs.get(i);
                Log log2;
                StringBuilder stringBuilder2;
                if (((Header) challenges.get(id.toLowerCase(Locale.ENGLISH))) != null) {
                    if (this.log.isDebugEnabled()) {
                        log2 = this.log;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(id);
                        stringBuilder2.append(" authentication scheme selected");
                        log2.debug(stringBuilder2.toString());
                    }
                    try {
                        authScheme = registry.getAuthScheme(id, response.getParams());
                        break;
                    } catch (IllegalStateException e) {
                        if (this.log.isWarnEnabled()) {
                            Log log3 = this.log;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Authentication scheme ");
                            stringBuilder3.append(id);
                            stringBuilder3.append(" not supported");
                            log3.warn(stringBuilder3.toString());
                        }
                    }
                } else {
                    if (this.log.isDebugEnabled()) {
                        log2 = this.log;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Challenge for ");
                        stringBuilder2.append(id);
                        stringBuilder2.append(" authentication scheme not available");
                        log2.debug(stringBuilder2.toString());
                    }
                    i++;
                }
            }
            if (authScheme != null) {
                return authScheme;
            }
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Unable to respond to any of these challenges: ");
            stringBuilder4.append(challenges);
            throw new AuthenticationException(stringBuilder4.toString());
        }
        throw new IllegalStateException("AuthScheme registry not set in HTTP context");
    }
}
