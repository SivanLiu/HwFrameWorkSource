package org.bouncycastle.asn1.x500.style;

public class X500NameTokenizer {
    private StringBuffer buf;
    private int index;
    private char separator;
    private String value;

    public X500NameTokenizer(String str) {
        this(str, ',');
    }

    public X500NameTokenizer(String str, char c) {
        this.buf = new StringBuffer();
        this.value = str;
        this.index = -1;
        this.separator = c;
    }

    public boolean hasMoreTokens() {
        return this.index != this.value.length();
    }

    public String nextToken() {
        if (this.index == this.value.length()) {
            return null;
        }
        int i = this.index + 1;
        this.buf.setLength(0);
        int i2 = 0;
        int i3 = i2;
        while (i != this.value.length()) {
            char charAt = this.value.charAt(i);
            if (charAt == '\"') {
                if (i2 == 0) {
                    i3 ^= 1;
                }
            } else if (i2 == 0 && i3 == 0) {
                if (charAt == '\\') {
                    this.buf.append(charAt);
                    i2 = 1;
                } else if (charAt == this.separator) {
                    break;
                } else {
                    this.buf.append(charAt);
                }
                i++;
            }
            this.buf.append(charAt);
            i2 = 0;
            i++;
        }
        this.index = i;
        return this.buf.toString();
    }
}
