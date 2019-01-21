package java.net;

import java.io.UnsupportedEncodingException;

public class URLDecoder {
    static String dfltEncName = URLEncoder.dfltEncName;

    @Deprecated
    public static String decode(String s) {
        try {
            return decode(s, dfltEncName);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static String decode(String s, String enc) throws UnsupportedEncodingException {
        NumberFormatException e;
        int i;
        StringBuilder stringBuilder;
        boolean needToChange = false;
        char numChars = s.length();
        StringBuffer sb = new StringBuffer(numChars > 500 ? numChars / 2 : numChars);
        char i2 = 0;
        if (enc.length() != 0) {
            byte[] bytes = null;
            while (i2 < numChars) {
                char c = s.charAt(i2);
                if (c == '%') {
                    char c2;
                    if (bytes == null) {
                        try {
                            bytes = new byte[((numChars - i2) / 3)];
                        } catch (NumberFormatException e2) {
                            e = e2;
                            c2 = c;
                            c = i2;
                            i = e;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("URLDecoder: Illegal hex characters in escape (%) pattern - ");
                            stringBuilder.append(i.getMessage());
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                    }
                    c2 = c;
                    c = i2;
                    i = 0;
                    while (c + 2 < numChars && c2 == '%') {
                        try {
                            if (isValidHexChar(s.charAt(c + 1)) && isValidHexChar(s.charAt(c + 2))) {
                                int v = Integer.parseInt(s.substring(c + 1, c + 3), 16);
                                if (v >= 0) {
                                    int pos = i + 1;
                                    bytes[i] = (byte) v;
                                    c += 3;
                                    if (c < numChars) {
                                        c2 = s.charAt(c);
                                    }
                                    i = pos;
                                } else {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("URLDecoder: Illegal hex characters in escape (%) pattern - negative value : ");
                                    stringBuilder.append(s.substring(c, c + 3));
                                    throw new IllegalArgumentException(stringBuilder.toString());
                                }
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("URLDecoder: Illegal hex characters in escape (%) pattern : ");
                            stringBuilder.append(s.substring(c, c + 3));
                            throw new IllegalArgumentException(stringBuilder.toString());
                        } catch (NumberFormatException e3) {
                            e = e3;
                            i = e;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("URLDecoder: Illegal hex characters in escape (%) pattern - ");
                            stringBuilder.append(i.getMessage());
                            throw new IllegalArgumentException(stringBuilder.toString());
                        }
                    }
                    if (c < numChars) {
                        if (c2 == '%') {
                            throw new IllegalArgumentException("URLDecoder: Incomplete trailing escape (%) pattern");
                        }
                    }
                    sb.append(new String(bytes, 0, i, enc));
                    needToChange = true;
                    i2 = c;
                } else if (c != '+') {
                    sb.append(c);
                    i2++;
                } else {
                    sb.append(' ');
                    i2++;
                    needToChange = true;
                }
            }
            return needToChange ? sb.toString() : s;
        } else {
            throw new UnsupportedEncodingException("URLDecoder: empty string enc parameter");
        }
    }

    private static boolean isValidHexChar(char c) {
        return ('0' <= c && c <= '9') || (('a' <= c && c <= 'f') || ('A' <= c && c <= 'F'));
    }
}
