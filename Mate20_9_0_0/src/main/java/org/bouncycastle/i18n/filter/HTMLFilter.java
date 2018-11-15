package org.bouncycastle.i18n.filter;

import org.bouncycastle.asn1.eac.EACTags;

public class HTMLFilter implements Filter {
    public String doFilter(String str) {
        StringBuffer stringBuffer = new StringBuffer(str);
        int i = 0;
        while (i < stringBuffer.length()) {
            int i2;
            String str2;
            char charAt = stringBuffer.charAt(i);
            if (charAt == '+') {
                i2 = i + 1;
                str2 = "&#43";
            } else if (charAt == '-') {
                i2 = i + 1;
                str2 = "&#45";
            } else if (charAt != '>') {
                switch (charAt) {
                    case '\"':
                        i2 = i + 1;
                        str2 = "&#34";
                        break;
                    case '#':
                        i2 = i + 1;
                        str2 = "&#35";
                        break;
                    default:
                        switch (charAt) {
                            case EACTags.APPLICATION_EFFECTIVE_DATE /*37*/:
                                i2 = i + 1;
                                str2 = "&#37";
                                break;
                            case EACTags.CARD_EFFECTIVE_DATE /*38*/:
                                i2 = i + 1;
                                str2 = "&#38";
                                break;
                            case EACTags.INTERCHANGE_CONTROL /*39*/:
                                i2 = i + 1;
                                str2 = "&#39";
                                break;
                            case '(':
                                i2 = i + 1;
                                str2 = "&#40";
                                break;
                            case EACTags.INTERCHANGE_PROFILE /*41*/:
                                i2 = i + 1;
                                str2 = "&#41";
                                break;
                            default:
                                switch (charAt) {
                                    case ';':
                                        i2 = i + 1;
                                        str2 = "&#59";
                                        break;
                                    case '<':
                                        i2 = i + 1;
                                        str2 = "&#60";
                                        break;
                                    default:
                                        i -= 3;
                                        continue;
                                        continue;
                                        continue;
                                }
                        }
                }
            } else {
                i2 = i + 1;
                str2 = "&#62";
            }
            stringBuffer.replace(i, i2, str2);
            i += 4;
        }
        return stringBuffer.toString();
    }

    public String doFilterUrl(String str) {
        return doFilter(str);
    }
}
