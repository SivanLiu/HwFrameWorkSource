package android.icu.text;

import android.icu.impl.Utility;

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
        this.data = theData;
        if (anteContextPos < 0) {
            this.anteContextLength = 0;
        } else if (anteContextPos > input.length()) {
            throw new IllegalArgumentException("Invalid ante context");
        } else {
            this.anteContextLength = anteContextPos;
        }
        if (postContextPos < 0) {
            this.keyLength = input.length() - this.anteContextLength;
        } else if (postContextPos < this.anteContextLength || postContextPos > input.length()) {
            throw new IllegalArgumentException("Invalid post context");
        } else {
            this.keyLength = postContextPos - this.anteContextLength;
        }
        if (cursorPos < 0) {
            cursorPos = output.length();
        } else if (cursorPos > output.length()) {
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
        this.output = new StringReplacer(output, cursorPos + cursorOffset, this.data);
    }

    public int getAnteContextLength() {
        int i = 0;
        int i2 = this.anteContextLength;
        if ((this.flags & 1) != 0) {
            i = 1;
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
        boolean z = true;
        boolean z2 = false;
        int len = this.pattern.length();
        int left = this.anteContextLength;
        int left2 = r2.anteContextLength;
        int right = this.pattern.length() - left;
        int right2 = r2.pattern.length() - left2;
        if (left == left2 && right == right2 && this.keyLength <= r2.keyLength && r2.pattern.regionMatches(0, this.pattern, 0, len)) {
            if (this.flags != r2.flags && (!((this.flags & 1) == 0 && (this.flags & 2) == 0) && ((r2.flags & 1) == 0 || (r2.flags & 2) == 0))) {
                z = false;
            }
            return z;
        }
        if (left <= left2 && (right < right2 || (right == right2 && this.keyLength <= r2.keyLength))) {
            z2 = r2.pattern.regionMatches(left2 - left, this.pattern, 0, len);
        }
        return z2;
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

    public int matchAndReplace(android.icu.text.Replaceable r13, android.icu.text.Transliterator.Position r14, boolean r15) {
        /* JADX: method processing error */
/*
Error: java.lang.IndexOutOfBoundsException: bitIndex < 0: -1
	at java.util.BitSet.get(BitSet.java:623)
	at jadx.core.dex.visitors.CodeShrinker$ArgsInfo.usedArgAssign(CodeShrinker.java:138)
	at jadx.core.dex.visitors.CodeShrinker$ArgsInfo.canMove(CodeShrinker.java:129)
	at jadx.core.dex.visitors.CodeShrinker$ArgsInfo.checkInline(CodeShrinker.java:93)
	at jadx.core.dex.visitors.CodeShrinker.shrinkBlock(CodeShrinker.java:223)
	at jadx.core.dex.visitors.CodeShrinker.shrinkMethod(CodeShrinker.java:38)
	at jadx.core.dex.visitors.regions.LoopRegionVisitor.checkArrayForEach(LoopRegionVisitor.java:196)
	at jadx.core.dex.visitors.regions.LoopRegionVisitor.checkForIndexedLoop(LoopRegionVisitor.java:119)
	at jadx.core.dex.visitors.regions.LoopRegionVisitor.processLoopRegion(LoopRegionVisitor.java:65)
	at jadx.core.dex.visitors.regions.LoopRegionVisitor.enterRegion(LoopRegionVisitor.java:52)
	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:56)
	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:58)
	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:58)
	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverseInternal(DepthRegionTraversal.java:58)
	at jadx.core.dex.visitors.regions.DepthRegionTraversal.traverse(DepthRegionTraversal.java:18)
	at jadx.core.dex.visitors.regions.LoopRegionVisitor.visit(LoopRegionVisitor.java:46)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r12 = this;
        r10 = r12.segments;
        if (r10 == 0) goto L_0x0016;
    L_0x0004:
        r1 = 0;
    L_0x0005:
        r10 = r12.segments;
        r10 = r10.length;
        if (r1 >= r10) goto L_0x0016;
    L_0x000a:
        r10 = r12.segments;
        r10 = r10[r1];
        r10 = (android.icu.text.StringMatcher) r10;
        r10.resetMatch();
        r1 = r1 + 1;
        goto L_0x0005;
    L_0x0016:
        r10 = 1;
        r2 = new int[r10];
        r10 = r14.contextStart;
        r0 = posBefore(r13, r10);
        r10 = r14.start;
        r10 = posBefore(r13, r10);
        r11 = 0;
        r2[r11] = r10;
        r10 = r12.anteContext;
        if (r10 == 0) goto L_0x0038;
    L_0x002c:
        r10 = r12.anteContext;
        r11 = 0;
        r5 = r10.matches(r13, r2, r0, r11);
        r10 = 2;
        if (r5 == r10) goto L_0x0038;
    L_0x0036:
        r10 = 0;
        return r10;
    L_0x0038:
        r10 = 0;
        r9 = r2[r10];
        r6 = posAfter(r13, r9);
        r10 = r12.flags;
        r10 = r10 & 1;
        if (r10 == 0) goto L_0x0049;
    L_0x0045:
        if (r9 == r0) goto L_0x0049;
    L_0x0047:
        r10 = 0;
        return r10;
    L_0x0049:
        r10 = r14.start;
        r11 = 0;
        r2[r11] = r10;
        r10 = r12.key;
        if (r10 == 0) goto L_0x005e;
    L_0x0052:
        r10 = r12.key;
        r11 = r14.limit;
        r5 = r10.matches(r13, r2, r11, r15);
        r10 = 2;
        if (r5 == r10) goto L_0x005e;
    L_0x005d:
        return r5;
    L_0x005e:
        r10 = 0;
        r3 = r2[r10];
        r10 = r12.postContext;
        if (r10 == 0) goto L_0x0079;
    L_0x0065:
        if (r15 == 0) goto L_0x006d;
    L_0x0067:
        r10 = r14.limit;
        if (r3 != r10) goto L_0x006d;
    L_0x006b:
        r10 = 1;
        return r10;
    L_0x006d:
        r10 = r12.postContext;
        r11 = r14.contextLimit;
        r5 = r10.matches(r13, r2, r11, r15);
        r10 = 2;
        if (r5 == r10) goto L_0x0079;
    L_0x0078:
        return r5;
    L_0x0079:
        r10 = 0;
        r9 = r2[r10];
        r10 = r12.flags;
        r10 = r10 & 2;
        if (r10 == 0) goto L_0x008c;
    L_0x0082:
        r10 = r14.contextLimit;
        if (r9 == r10) goto L_0x0088;
    L_0x0086:
        r10 = 0;
        return r10;
    L_0x0088:
        if (r15 == 0) goto L_0x008c;
    L_0x008a:
        r10 = 1;
        return r10;
    L_0x008c:
        r10 = r12.output;
        r11 = r14.start;
        r7 = r10.replace(r13, r11, r3, r2);
        r10 = r14.start;
        r10 = r3 - r10;
        r4 = r7 - r10;
        r10 = 0;
        r8 = r2[r10];
        r9 = r9 + r4;
        r10 = r14.limit;
        r10 = r10 + r4;
        r14.limit = r10;
        r10 = r14.contextLimit;
        r10 = r10 + r4;
        r14.contextLimit = r10;
        r10 = r14.limit;
        r10 = java.lang.Math.min(r9, r10);
        r10 = java.lang.Math.min(r10, r8);
        r10 = java.lang.Math.max(r6, r10);
        r14.start = r10;
        r10 = 2;
        return r10;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.icu.text.TransliterationRule.matchAndReplace(android.icu.text.Replaceable, android.icu.text.Transliterator$Position, boolean):int");
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
        return '{' + toRule(true) + '}';
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
