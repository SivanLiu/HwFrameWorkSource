package org.bouncycastle.i18n.filter;

public class SQLFilter implements Filter {
    public String doFilter(String str) {
        StringBuffer stringBuffer = new StringBuffer(str);
        int i = 0;
        while (i < stringBuffer.length()) {
            int i2;
            String str2;
            char charAt = stringBuffer.charAt(i);
            if (charAt == 10) {
                i2 = i + 1;
                str2 = "\\n";
            } else if (charAt == 13) {
                i2 = i + 1;
                str2 = "\\r";
            } else if (charAt == '\"') {
                i2 = i + 1;
                str2 = "\\\"";
            } else if (charAt == '\'') {
                i2 = i + 1;
                str2 = "\\'";
            } else if (charAt == '-') {
                i2 = i + 1;
                str2 = "\\-";
            } else if (charAt == '/') {
                i2 = i + 1;
                str2 = "\\/";
            } else if (charAt == ';') {
                i2 = i + 1;
                str2 = "\\;";
            } else if (charAt == '=') {
                i2 = i + 1;
                str2 = "\\=";
            } else if (charAt != '\\') {
                i2 = i;
                i = i2 + 1;
            } else {
                i2 = i + 1;
                str2 = "\\\\";
            }
            stringBuffer.replace(i, i2, str2);
            i = i2 + 1;
        }
        return stringBuffer.toString();
    }

    public String doFilterUrl(String str) {
        return doFilter(str);
    }
}
