package org.apache.http.impl.cookie;

import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;

@Deprecated
public class BasicExpiresHandler extends AbstractCookieAttributeHandler {
    private final String[] datepatterns;

    public BasicExpiresHandler(String[] datepatterns) {
        if (datepatterns != null) {
            this.datepatterns = datepatterns;
            return;
        }
        throw new IllegalArgumentException("Array of date patterns may not be null");
    }

    public void parse(SetCookie cookie, String value) throws MalformedCookieException {
        if (cookie == null) {
            throw new IllegalArgumentException("Cookie may not be null");
        } else if (value != null) {
            try {
                cookie.setExpiryDate(DateUtils.parseDate(value, this.datepatterns));
            } catch (DateParseException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to parse expires attribute: ");
                stringBuilder.append(value);
                throw new MalformedCookieException(stringBuilder.toString());
            }
        } else {
            throw new MalformedCookieException("Missing value for expires attribute");
        }
    }
}
