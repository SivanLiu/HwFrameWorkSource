package android.icu.text;

import android.icu.impl.Utility;
import android.icu.text.Transliterator.Position;

class TransliterationRule {
    static final int ANCHOR_END = 2;
    static final int ANCHOR_START = 1;
    private StringMatcher anteContext;
    private int anteContextLength;
    private final Data data;
    byte flags;
    private StringMatcher key;
    private int keyLength;
    private UnicodeReplacer output;
    private String pattern;
    private StringMatcher postContext;
    UnicodeMatcher[] segments;

    public TransliterationRule(String input, int anteContextPos, int postContextPos, String output, int cursorPos, int cursorOffset, UnicodeMatcher[] segs, boolean anchorStart, boolean anchorEnd, Data theData) {
        String str;
        String str2;
        UnicodeMatcher[] unicodeMatcherArr;
        int i = anteContextPos;
        int i2 = postContextPos;
        int cursorPos2 = cursorPos;
        this.data = theData;
        if (i < 0) {
            this.anteContextLength = 0;
        } else if (i <= input.length()) {
            this.anteContextLength = i;
        } else {
            str = input;
            str2 = output;
            unicodeMatcherArr = segs;
            throw new IllegalArgumentException("Invalid ante context");
        }
        if (i2 < 0) {
            this.keyLength = input.length() - this.anteContextLength;
        } else if (i2 < this.anteContextLength || i2 > input.length()) {
            str = input;
            str2 = output;
            unicodeMatcherArr = segs;
            throw new IllegalArgumentException("Invalid post context");
        } else {
            this.keyLength = i2 - this.anteContextLength;
        }
        if (cursorPos2 < 0) {
            cursorPos2 = output.length();
        } else if (cursorPos2 > output.length()) {
            str = input;
            str2 = output;
            unicodeMatcherArr = segs;
            throw new IllegalArgumentException("Invalid cursor position");
        }
        this.segments = segs;
        this.pattern = input;
        this.flags = (byte) 0;
        if (anchorStart) {
            this.flags = (byte) (this.flags | 1);
        }
        if (anchorEnd) {
            this.flags = (byte) (this.flags | 2);
        }
        this.anteContext = null;
        if (this.anteContextLength > 0) {
            this.anteContext = new StringMatcher(this.pattern.substring(0, this.anteContextLength), 0, this.data);
        }
        this.key = null;
        if (this.keyLength > 0) {
            this.key = new StringMatcher(this.pattern.substring(this.anteContextLength, this.anteContextLength + this.keyLength), 0, this.data);
        }
        int postContextLength = (this.pattern.length() - this.keyLength) - this.anteContextLength;
        this.postContext = null;
        if (postContextLength > 0) {
            this.postContext = new StringMatcher(this.pattern.substring(this.anteContextLength + this.keyLength), 0, this.data);
        }
        this.output = new StringReplacer(output, cursorPos2 + cursorOffset, this.data);
    }

    public int getAnteContextLength() {
        int i = this.anteContextLength;
        int i2 = 1;
        if ((this.flags & 1) == 0) {
            i2 = 0;
        }
        return i + i2;
    }

    final int getIndexValue() {
        int i = -1;
        if (this.anteContextLength == this.pattern.length()) {
            return -1;
        }
        int c = UTF16.charAt(this.pattern, this.anteContextLength);
        if (this.data.lookupMatcher(c) == null) {
            i = c & 255;
        }
        return i;
    }

    final boolean matchesIndexValue(int v) {
        UnicodeMatcher m = this.key != null ? this.key : this.postContext;
        return m != null ? m.matchesIndexValue(v) : true;
    }

    public boolean masks(TransliterationRule r2) {
        int len = this.pattern.length();
        int left = this.anteContextLength;
        int left2 = r2.anteContextLength;
        int right = this.pattern.length() - left;
        int right2 = r2.pattern.length() - left2;
        boolean z = true;
        if (left == left2 && right == right2 && this.keyLength <= r2.keyLength && r2.pattern.regionMatches(0, this.pattern, 0, len)) {
            if (this.flags != r2.flags && (!((this.flags & 1) == 0 && (this.flags & 2) == 0) && ((r2.flags & 1) == 0 || (r2.flags & 2) == 0))) {
                z = false;
            }
            return z;
        }
        if (left > left2 || ((right >= right2 && (right != right2 || this.keyLength > r2.keyLength)) || !r2.pattern.regionMatches(left2 - left, this.pattern, 0, len))) {
            z = false;
        }
        return z;
    }

    static final int posBefore(Replaceable str, int pos) {
        if (pos > 0) {
            return pos - UTF16.getCharCount(str.char32At(pos - 1));
        }
        return pos - 1;
    }

    static final int posAfter(Replaceable str, int pos) {
        if (pos < 0 || pos >= str.length()) {
            return pos + 1;
        }
        return UTF16.getCharCount(str.char32At(pos)) + pos;
    }

    public int matchAndReplace(Replaceable text, Position pos, boolean incremental) {
        if (this.segments != null) {
            for (UnicodeMatcher unicodeMatcher : this.segments) {
                ((StringMatcher) unicodeMatcher).resetMatch();
            }
        }
        int[] intRef = new int[1];
        int anteLimit = posBefore(text, pos.contextStart);
        intRef[0] = posBefore(text, pos.start);
        if (this.anteContext != null && this.anteContext.matches(text, intRef, anteLimit, false) != 2) {
            return 0;
        }
        int oText = intRef[0];
        int minOText = posAfter(text, oText);
        if ((this.flags & 1) != 0 && oText != anteLimit) {
            return 0;
        }
        int match;
        int match2;
        intRef[0] = pos.start;
        if (this.key != null) {
            match = this.key.matches(text, intRef, pos.limit, incremental);
            if (match != 2) {
                return match;
            }
        }
        match = intRef[0];
        if (this.postContext != null) {
            if (incremental && match == pos.limit) {
                return 1;
            }
            match2 = this.postContext.matches(text, intRef, pos.contextLimit, incremental);
            if (match2 != 2) {
                return match2;
            }
        }
        oText = intRef[0];
        if ((this.flags & 2) != 0) {
            if (oText != pos.contextLimit) {
                return 0;
            }
            if (incremental) {
                return 1;
            }
        }
        match2 = this.output.replace(text, pos.start, match, intRef) - (match - pos.start);
        pos.limit += match2;
        pos.contextLimit += match2;
        pos.start = Math.max(minOText, Math.min(Math.min(oText += match2, pos.limit), intRef[0]));
        return 2;
    }

    public String toRule(boolean escapeUnprintable) {
        StringBuffer rule = new StringBuffer();
        StringBuffer quoteBuf = new StringBuffer();
        boolean emitBraces = (this.anteContext == null && this.postContext == null) ? false : true;
        if ((this.flags & 1) != 0) {
            rule.append('^');
        }
        Utility.appendToRule(rule, this.anteContext, escapeUnprintable, quoteBuf);
        if (emitBraces) {
            Utility.appendToRule(rule, 123, true, escapeUnprintable, quoteBuf);
        }
        Utility.appendToRule(rule, this.key, escapeUnprintable, quoteBuf);
        if (emitBraces) {
            Utility.appendToRule(rule, 125, true, escapeUnprintable, quoteBuf);
        }
        Utility.appendToRule(rule, this.postContext, escapeUnprintable, quoteBuf);
        if ((this.flags & 2) != 0) {
            rule.append(SymbolTable.SYMBOL_REF);
        }
        Utility.appendToRule(rule, " > ", true, escapeUnprintable, quoteBuf);
        Utility.appendToRule(rule, this.output.toReplacerPattern(escapeUnprintable), true, escapeUnprintable, quoteBuf);
        Utility.appendToRule(rule, 59, true, escapeUnprintable, quoteBuf);
        return rule.toString();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('{');
        stringBuilder.append(toRule(true));
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    void addSourceTargetSet(UnicodeSet filter, UnicodeSet sourceSet, UnicodeSet targetSet, UnicodeSet revisiting) {
        int limit = this.anteContextLength + this.keyLength;
        UnicodeSet tempSource = new UnicodeSet();
        UnicodeSet temp = new UnicodeSet();
        int i = this.anteContextLength;
        while (i < limit) {
            int ch = UTF16.charAt(this.pattern, i);
            i += UTF16.getCharCount(ch);
            UnicodeMatcher matcher = this.data.lookupMatcher(ch);
            if (matcher != null) {
                try {
                    if (filter.containsSome((UnicodeSet) matcher)) {
                        matcher.addMatchSetTo(tempSource);
                    } else {
                        return;
                    }
                } catch (ClassCastException e) {
                    temp.clear();
                    matcher.addMatchSetTo(temp);
                    if (filter.containsSome(temp)) {
                        tempSource.addAll(temp);
                    } else {
                        return;
                    }
                }
            } else if (filter.contains(ch)) {
                tempSource.add(ch);
            } else {
                return;
            }
        }
        sourceSet.addAll(tempSource);
        this.output.addReplacementSetTo(targetSet);
    }
}
