package org.apache.http.impl.auth;

import org.apache.http.FormattedHeader;
import org.apache.http.Header;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public abstract class AuthSchemeBase implements AuthScheme {
    private boolean proxy;

    protected abstract void parseChallenge(CharArrayBuffer charArrayBuffer, int i, int i2) throws MalformedChallengeException;

    public void processChallenge(Header header) throws MalformedChallengeException {
        if (header != null) {
            String s;
            String authheader = header.getName();
            int beginIndex = 0;
            if (authheader.equalsIgnoreCase(AUTH.WWW_AUTH)) {
                this.proxy = false;
            } else if (authheader.equalsIgnoreCase(AUTH.PROXY_AUTH)) {
                this.proxy = true;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unexpected header name: ");
                stringBuilder.append(authheader);
                throw new MalformedChallengeException(stringBuilder.toString());
            }
            if (header instanceof FormattedHeader) {
                s = ((FormattedHeader) header).getBuffer();
                beginIndex = ((FormattedHeader) header).getValuePos();
            } else {
                s = header.getValue();
                if (s != null) {
                    CharArrayBuffer buffer = new CharArrayBuffer(s.length());
                    buffer.append(s);
                    s = buffer;
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
            String s2 = s.substring(beginIndex, pos);
            if (s2.equalsIgnoreCase(getSchemeName())) {
                parseChallenge(s, pos, s.length());
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Invalid scheme identifier: ");
            stringBuilder2.append(s2);
            throw new MalformedChallengeException(stringBuilder2.toString());
        }
        throw new IllegalArgumentException("Header may not be null");
    }

    public boolean isProxy() {
        return this.proxy;
    }
}
