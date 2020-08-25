package com.google.json;

import com.huawei.odmf.model.ARelationship;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EvalMinifier {
    static final /* synthetic */ boolean $assertionsDisabled;
    private static final int BOILERPLATE_COST = "(function(){return}())".length();
    private static final String ENVELOPE_P1 = "(function(";
    private static final String ENVELOPE_P2 = "){return";
    private static final String ENVELOPE_P3 = "}(";
    private static final String ENVELOPE_P4 = "))";
    private static final int MARGINAL_VAR_COST = ",,".length();
    /* access modifiers changed from: private */
    public static final String[][] RESERVED_KEYWORDS = {new String[0], new String[0], new String[]{"do", "if", "in"}, new String[]{"for", "let", "new", "try", "var"}, new String[]{"case", "else", "enum", "eval", "null", "this", "true", "void", "with"}, new String[]{"catch", "class", "const", "false", "super", "throw", "while", "yield"}, new String[]{ARelationship.DELETE_CASCADE, "export", "import", "return", "switch", "static", "typeof"}, new String[]{"default", "extends", "public", "private"}, new String[]{"continue", "function"}, new String[]{"arguments"}, new String[]{"implements", "instanceof"}};
    private static final int SAVINGS_THRESHOLD = 32;

    static {
        boolean z;
        if (!EvalMinifier.class.desiredAssertionStatus()) {
            z = true;
        } else {
            z = false;
        }
        $assertionsDisabled = z;
    }

    public static String minify(String jsonish) {
        JsonSanitizer s = new JsonSanitizer(jsonish);
        s.sanitize();
        return minify(s.toCharSequence()).toString();
    }

    public static String minify(String jsonish, int maximumNestingDepth) {
        JsonSanitizer s = new JsonSanitizer(jsonish, maximumNestingDepth);
        s.sanitize();
        return minify(s.toCharSequence()).toString();
    }

    private static CharSequence minify(CharSequence json) {
        Token tok;
        int limit;
        int tokEnd;
        Token tok2;
        Token last;
        Map<Token, Token> pool = new HashMap<>();
        int n = json.length();
        int i = 0;
        while (i < n) {
            char ch = json.charAt(i);
            if (ch == '\"') {
                tokEnd = i + 1;
                while (true) {
                    if (tokEnd >= n) {
                        break;
                    }
                    char tch = json.charAt(tokEnd);
                    if (tch == '\\') {
                        tokEnd++;
                    } else if (tch == '\"') {
                        tokEnd++;
                        break;
                    }
                    tokEnd++;
                }
            } else if (isLetterOrNumberChar(ch)) {
                int tokEnd2 = i + 1;
                while (tokEnd < n && isLetterOrNumberChar(json.charAt(tokEnd))) {
                    tokEnd2 = tokEnd + 1;
                }
            } else {
                i++;
            }
            int nextNonWhitespace = tokEnd;
            while (nextNonWhitespace < n && ((wch = json.charAt(nextNonWhitespace)) == '\t' || wch == '\n' || wch == '\r' || wch == ' ')) {
                nextNonWhitespace++;
            }
            if ((nextNonWhitespace == n || (':' != json.charAt(nextNonWhitespace) && tokEnd - i >= 4)) && (last = pool.put((tok2 = new Token(i, tokEnd, json)), tok2)) != null) {
                tok2.prev = last;
            }
            i = nextNonWhitespace - 1;
            i++;
        }
        int potentialSavings = 0;
        List<Token> dupes = new ArrayList<>();
        Iterator<Token> values = pool.values().iterator();
        while (values.hasNext()) {
            Token tok3 = values.next();
            if (tok3.prev == null) {
                values.remove();
            } else {
                int chainDepth = 0;
                for (Token t = tok3; t != null; t = t.prev) {
                    chainDepth++;
                }
                int tokSavings = ((chainDepth - 1) * (tok3.end - tok3.start)) - MARGINAL_VAR_COST;
                if (tokSavings > 0) {
                    potentialSavings += tokSavings;
                    for (Token t2 = tok3; t2 != null; t2 = t2.prev) {
                        dupes.add(t2);
                    }
                }
            }
        }
        if (potentialSavings <= BOILERPLATE_COST + 32) {
            return json;
        }
        Collections.sort(dupes);
        int nTokens = dupes.size();
        StringBuilder sb = new StringBuilder(n);
        sb.append(ENVELOPE_P1);
        NameGenerator nameGenerator = new NameGenerator();
        boolean first = true;
        for (Token t3 : pool.values()) {
            String name = nameGenerator.next();
            for (; t3 != null; t3 = t3.prev) {
                t3.name = name;
            }
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(name);
        }
        sb.append(ENVELOPE_P2);
        int afterReturn = sb.length();
        int pos = 0;
        int tokIndex = 0;
        while (true) {
            if (tokIndex < nTokens) {
                tokIndex++;
                tok = dupes.get(tokIndex);
            } else {
                tok = null;
                tokIndex = tokIndex;
            }
            if (tok != null) {
                limit = tok.start;
            } else {
                limit = n;
            }
            boolean inString = false;
            int i2 = pos;
            while (i2 < limit) {
                char ch2 = json.charAt(i2);
                if (inString) {
                    if (ch2 == '\"') {
                        inString = false;
                    } else if (ch2 == '\\') {
                        i2++;
                    }
                } else if (ch2 == '\t' || ch2 == '\n' || ch2 == '\r' || ch2 == ' ') {
                    if (pos != i2) {
                        sb.append(json, pos, i2);
                    }
                    pos = i2 + 1;
                } else if (ch2 == '\"') {
                    inString = true;
                }
                i2++;
            }
            if ($assertionsDisabled || !inString) {
                if (pos != limit) {
                    sb.append(json, pos, limit);
                }
                if (tok == null) {
                    char ch3 = sb.charAt(afterReturn);
                    if (!(ch3 == '{' || ch3 == '[' || ch3 == '\"')) {
                        sb.insert(afterReturn, ' ');
                    }
                    sb.append(ENVELOPE_P3);
                    boolean first2 = true;
                    for (Token tok4 : pool.values()) {
                        if (first2) {
                            first2 = false;
                        } else {
                            sb.append(',');
                        }
                        sb.append(tok4.seq, tok4.start, tok4.end);
                    }
                    sb.append(ENVELOPE_P4);
                    return sb;
                }
                sb.append(tok.name);
                pos = tok.end;
            } else {
                throw new AssertionError();
            }
        }
    }

    private static boolean isLetterOrNumberChar(char ch) {
        if ('0' <= ch && ch <= '9') {
            return true;
        }
        char lch = (char) (ch | ' ');
        if (('a' <= lch && lch <= 'z') || ch == '_' || ch == '$' || ch == '-' || ch == '.') {
            return true;
        }
        return false;
    }

    private static final class Token implements Comparable<Token> {
        /* access modifiers changed from: private */
        public final int end;
        private final int hashCode;
        @Nullable
        String name;
        @Nullable
        Token prev;
        /* access modifiers changed from: private */
        @Nonnull
        public final CharSequence seq;
        /* access modifiers changed from: private */
        public final int start;

        Token(int start2, int end2, CharSequence seq2) {
            this.start = start2;
            this.end = end2;
            this.seq = seq2;
            int hc = 0;
            for (int i = start2; i < end2; i++) {
                hc = (hc * 31) + seq2.charAt(i);
            }
            this.hashCode = hc;
        }

        public boolean equals(@Nullable Object o) {
            if (!(o instanceof Token)) {
                return false;
            }
            Token that = (Token) o;
            if (this.hashCode == that.hashCode) {
                return EvalMinifier.regionMatches(this.seq, this.start, this.end, that.seq, that.start, that.end);
            }
            return false;
        }

        public int hashCode() {
            return this.hashCode;
        }

        public int compareTo(Token t) {
            return this.start - t.start;
        }
    }

    static boolean regionMatches(CharSequence a, int as, int ae, CharSequence b, int bs, int be) {
        if (be - bs != ae - as) {
            return false;
        }
        int ai = as;
        int bi = bs;
        while (ai < ae) {
            if (a.charAt(ai) != b.charAt(bi)) {
                return false;
            }
            ai++;
            bi++;
        }
        return true;
    }

    static final class NameGenerator {
        private final StringBuilder sb = new StringBuilder("a");

        NameGenerator() {
        }

        public String next() {
            String name;
            int nameLen;
            do {
                name = this.sb.toString();
                int i = this.sb.length();
                while (true) {
                    i--;
                    if (i < 0) {
                        break;
                    }
                    int next = EvalMinifier.nextIdentChar(this.sb.charAt(i), i != 0);
                    if (next >= 0) {
                        this.sb.setCharAt(i, (char) next);
                        break;
                    }
                    this.sb.setCharAt(i, 'a');
                    if (i == 0) {
                        this.sb.append('a');
                    }
                }
                nameLen = name.length();
                if (nameLen >= EvalMinifier.RESERVED_KEYWORDS.length) {
                    break;
                }
            } while (Arrays.binarySearch(EvalMinifier.RESERVED_KEYWORDS[nameLen], name) >= 0);
            return name;
        }
    }

    static int nextIdentChar(char ch, boolean allowDigits) {
        if (ch == 'z') {
            return 65;
        }
        if (ch == 'Z') {
            return 95;
        }
        if (ch == '_') {
            return 36;
        }
        if (ch == '$') {
            if (allowDigits) {
                return 48;
            }
            return -1;
        } else if (ch == '9') {
            return -1;
        } else {
            return (char) (ch + 1);
        }
    }
}
