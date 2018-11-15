package org.bouncycastle.est;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bouncycastle.pqc.math.linearalgebra.Matrix;

class HttpUtil {

    static class Headers extends HashMap<String, String[]> {
        private String actualKey(String str) {
            if (containsKey(str)) {
                return str;
            }
            for (String str2 : keySet()) {
                if (str.equalsIgnoreCase(str2)) {
                    return str2;
                }
            }
            return null;
        }

        private String[] copy(String[] strArr) {
            Object obj = new String[strArr.length];
            System.arraycopy(strArr, 0, obj, 0, obj.length);
            return obj;
        }

        private boolean hasHeader(String str) {
            return actualKey(str) != null;
        }

        public void add(String str, String str2) {
            put(str, HttpUtil.append((String[]) get(str), str2));
        }

        public Object clone() {
            Headers headers = new Headers();
            for (Entry entry : entrySet()) {
                headers.put((String) entry.getKey(), copy((String[]) entry.getValue()));
            }
            return headers;
        }

        public void ensureHeader(String str, String str2) {
            if (!containsKey(str)) {
                set(str, str2);
            }
        }

        public String getFirstValue(String str) {
            String[] values = getValues(str);
            return (values == null || values.length <= 0) ? null : values[0];
        }

        public String[] getValues(String str) {
            str = actualKey(str);
            return str == null ? null : (String[]) get(str);
        }

        public void set(String str, String str2) {
            put(str, new String[]{str2});
        }
    }

    static class PartLexer {
        int last = 0;
        int p = 0;
        private final String src;

        PartLexer(String str) {
            this.src = str;
        }

        private String consumeAlpha() {
            String substring;
            while (true) {
                char charAt = this.src.charAt(this.p);
                if (this.p >= this.src.length() || ((charAt < 'a' || charAt > 'z') && (charAt < 'A' || charAt > Matrix.MATRIX_TYPE_ZERO))) {
                    substring = this.src.substring(this.last, this.p);
                    this.last = this.p;
                } else {
                    this.p++;
                }
            }
            substring = this.src.substring(this.last, this.p);
            this.last = this.p;
            return substring;
        }

        private boolean consumeIf(char c) {
            if (this.p >= this.src.length() || this.src.charAt(this.p) != c) {
                return false;
            }
            this.p++;
            return true;
        }

        private String consumeUntil(char c) {
            while (this.p < this.src.length() && this.src.charAt(this.p) != c) {
                this.p++;
            }
            String substring = this.src.substring(this.last, this.p);
            this.last = this.p;
            return substring;
        }

        private void discard() {
            this.last = this.p;
        }

        private void discard(int i) {
            this.p += i;
            this.last = this.p;
        }

        private void skipWhiteSpace() {
            while (this.p < this.src.length() && this.src.charAt(this.p) < '!') {
                this.p++;
            }
            this.last = this.p;
        }

        Map<String, String> Parse() {
            Map<String, String> hashMap = new HashMap();
            while (this.p < this.src.length()) {
                skipWhiteSpace();
                String consumeAlpha = consumeAlpha();
                if (consumeAlpha.length() != 0) {
                    skipWhiteSpace();
                    if (consumeIf('=')) {
                        skipWhiteSpace();
                        if (consumeIf('\"')) {
                            discard();
                            String consumeUntil = consumeUntil('\"');
                            discard(1);
                            hashMap.put(consumeAlpha, consumeUntil);
                            skipWhiteSpace();
                            if (!consumeIf(',')) {
                                return hashMap;
                            }
                            discard();
                        } else {
                            throw new IllegalArgumentException("Expecting start quote: '\"'");
                        }
                    }
                    throw new IllegalArgumentException("Expecting assign: '='");
                }
                throw new IllegalArgumentException("Expecting alpha label.");
            }
            return hashMap;
        }
    }

    HttpUtil() {
    }

    public static String[] append(String[] strArr, String str) {
        if (strArr == null) {
            return new String[]{str};
        }
        int length = strArr.length;
        Object obj = new String[(length + 1)];
        System.arraycopy(strArr, 0, obj, 0, length);
        obj[length] = str;
        return obj;
    }

    static String mergeCSL(String str, Map<String, String> map) {
        StringWriter stringWriter = new StringWriter();
        stringWriter.write(str);
        stringWriter.write(32);
        Object obj = null;
        for (Entry entry : map.entrySet()) {
            if (obj == null) {
                obj = 1;
            } else {
                stringWriter.write(44);
            }
            stringWriter.write((String) entry.getKey());
            stringWriter.write("=\"");
            stringWriter.write((String) entry.getValue());
            stringWriter.write(34);
        }
        return stringWriter.toString();
    }

    static Map<String, String> splitCSL(String str, String str2) {
        str2 = str2.trim();
        if (str2.startsWith(str)) {
            str2 = str2.substring(str.length());
        }
        return new PartLexer(str2).Parse();
    }
}
