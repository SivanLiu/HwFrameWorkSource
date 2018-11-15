package org.apache.http.message;

import org.apache.http.HeaderElement;
import org.apache.http.NameValuePair;
import org.apache.http.util.CharArrayBuffer;

@Deprecated
public class BasicHeaderValueFormatter implements HeaderValueFormatter {
    public static final BasicHeaderValueFormatter DEFAULT = new BasicHeaderValueFormatter();
    public static final String SEPARATORS = " ;,:@()<>\\\"/[]?={}\t";
    public static final String UNSAFE_CHARS = "\"\\";

    public static final String formatElements(HeaderElement[] elems, boolean quote, HeaderValueFormatter formatter) {
        if (formatter == null) {
            formatter = DEFAULT;
        }
        return formatter.formatElements(null, elems, quote).toString();
    }

    public CharArrayBuffer formatElements(CharArrayBuffer buffer, HeaderElement[] elems, boolean quote) {
        if (elems != null) {
            int len = estimateElementsLen(elems);
            if (buffer == null) {
                buffer = new CharArrayBuffer(len);
            } else {
                buffer.ensureCapacity(len);
            }
            for (int i = 0; i < elems.length; i++) {
                if (i > 0) {
                    buffer.append(", ");
                }
                formatHeaderElement(buffer, elems[i], quote);
            }
            return buffer;
        }
        throw new IllegalArgumentException("Header element array must not be null.");
    }

    protected int estimateElementsLen(HeaderElement[] elems) {
        int i = 0;
        if (elems == null || elems.length < 1) {
            return 0;
        }
        int result = (elems.length - 1) * 2;
        while (i < elems.length) {
            result += estimateHeaderElementLen(elems[i]);
            i++;
        }
        return result;
    }

    public static final String formatHeaderElement(HeaderElement elem, boolean quote, HeaderValueFormatter formatter) {
        if (formatter == null) {
            formatter = DEFAULT;
        }
        return formatter.formatHeaderElement(null, elem, quote).toString();
    }

    public CharArrayBuffer formatHeaderElement(CharArrayBuffer buffer, HeaderElement elem, boolean quote) {
        if (elem != null) {
            int len = estimateHeaderElementLen(elem);
            if (buffer == null) {
                buffer = new CharArrayBuffer(len);
            } else {
                buffer.ensureCapacity(len);
            }
            buffer.append(elem.getName());
            String value = elem.getValue();
            if (value != null) {
                buffer.append('=');
                doFormatValue(buffer, value, quote);
            }
            int parcnt = elem.getParameterCount();
            if (parcnt > 0) {
                for (int i = 0; i < parcnt; i++) {
                    buffer.append("; ");
                    formatNameValuePair(buffer, elem.getParameter(i), quote);
                }
            }
            return buffer;
        }
        throw new IllegalArgumentException("Header element must not be null.");
    }

    protected int estimateHeaderElementLen(HeaderElement elem) {
        int i = 0;
        if (elem == null) {
            return 0;
        }
        int result = elem.getName().length();
        String value = elem.getValue();
        if (value != null) {
            result += 3 + value.length();
        }
        int parcnt = elem.getParameterCount();
        if (parcnt > 0) {
            while (i < parcnt) {
                result += 2 + estimateNameValuePairLen(elem.getParameter(i));
                i++;
            }
        }
        return result;
    }

    public static final String formatParameters(NameValuePair[] nvps, boolean quote, HeaderValueFormatter formatter) {
        if (formatter == null) {
            formatter = DEFAULT;
        }
        return formatter.formatParameters(null, nvps, quote).toString();
    }

    public CharArrayBuffer formatParameters(CharArrayBuffer buffer, NameValuePair[] nvps, boolean quote) {
        if (nvps != null) {
            int len = estimateParametersLen(nvps);
            if (buffer == null) {
                buffer = new CharArrayBuffer(len);
            } else {
                buffer.ensureCapacity(len);
            }
            for (int i = 0; i < nvps.length; i++) {
                if (i > 0) {
                    buffer.append("; ");
                }
                formatNameValuePair(buffer, nvps[i], quote);
            }
            return buffer;
        }
        throw new IllegalArgumentException("Parameters must not be null.");
    }

    protected int estimateParametersLen(NameValuePair[] nvps) {
        int i = 0;
        if (nvps == null || nvps.length < 1) {
            return 0;
        }
        int result = (nvps.length - 1) * 2;
        while (i < nvps.length) {
            result += estimateNameValuePairLen(nvps[i]);
            i++;
        }
        return result;
    }

    public static final String formatNameValuePair(NameValuePair nvp, boolean quote, HeaderValueFormatter formatter) {
        if (formatter == null) {
            formatter = DEFAULT;
        }
        return formatter.formatNameValuePair(null, nvp, quote).toString();
    }

    public CharArrayBuffer formatNameValuePair(CharArrayBuffer buffer, NameValuePair nvp, boolean quote) {
        if (nvp != null) {
            int len = estimateNameValuePairLen(nvp);
            if (buffer == null) {
                buffer = new CharArrayBuffer(len);
            } else {
                buffer.ensureCapacity(len);
            }
            buffer.append(nvp.getName());
            String value = nvp.getValue();
            if (value != null) {
                buffer.append('=');
                doFormatValue(buffer, value, quote);
            }
            return buffer;
        }
        throw new IllegalArgumentException("NameValuePair must not be null.");
    }

    protected int estimateNameValuePairLen(NameValuePair nvp) {
        if (nvp == null) {
            return 0;
        }
        int result = nvp.getName().length();
        String value = nvp.getValue();
        if (value != null) {
            result += 3 + value.length();
        }
        return result;
    }

    protected void doFormatValue(CharArrayBuffer buffer, String value, boolean quote) {
        int i = 0;
        if (!quote) {
            boolean quote2 = quote;
            for (int i2 = 0; i2 < value.length() && !quote2; i2++) {
                quote2 = isSeparator(value.charAt(i2));
            }
            quote = quote2;
        }
        if (quote) {
            buffer.append('\"');
        }
        while (i < value.length()) {
            char ch = value.charAt(i);
            if (isUnsafe(ch)) {
                buffer.append('\\');
            }
            buffer.append(ch);
            i++;
        }
        if (quote) {
            buffer.append('\"');
        }
    }

    protected boolean isSeparator(char ch) {
        return SEPARATORS.indexOf(ch) >= 0;
    }

    protected boolean isUnsafe(char ch) {
        return UNSAFE_CHARS.indexOf(ch) >= 0;
    }
}
