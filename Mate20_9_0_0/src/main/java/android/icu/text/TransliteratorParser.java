package android.icu.text;

import android.icu.impl.IllegalIcuArgumentException;
import android.icu.impl.PatternProps;
import android.icu.impl.Utility;
import android.icu.lang.UCharacter;
import android.icu.text.Normalizer.Mode;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TransliteratorParser {
    private static final char ALT_FORWARD_RULE_OP = '→';
    private static final char ALT_FUNCTION = '∆';
    private static final char ALT_FWDREV_RULE_OP = '↔';
    private static final char ALT_REVERSE_RULE_OP = '←';
    private static final char ANCHOR_START = '^';
    private static final char CONTEXT_ANTE = '{';
    private static final char CONTEXT_POST = '}';
    private static final char CURSOR_OFFSET = '@';
    private static final char CURSOR_POS = '|';
    private static final char DOT = '.';
    private static final String DOT_SET = "[^[:Zp:][:Zl:]\\r\\n$]";
    private static final char END_OF_RULE = ';';
    private static final char ESCAPE = '\\';
    private static final char FORWARD_RULE_OP = '>';
    private static final char FUNCTION = '&';
    private static final char FWDREV_RULE_OP = '~';
    private static final String HALF_ENDERS = "=><←→↔;";
    private static final String ID_TOKEN = "::";
    private static final int ID_TOKEN_LEN = 2;
    private static UnicodeSet ILLEGAL_FUNC = new UnicodeSet("[\\^\\(\\.\\*\\+\\?\\{\\}\\|\\@]");
    private static UnicodeSet ILLEGAL_SEG = new UnicodeSet("[\\{\\}\\|\\@]");
    private static UnicodeSet ILLEGAL_TOP = new UnicodeSet("[\\)]");
    private static final char KLEENE_STAR = '*';
    private static final char ONE_OR_MORE = '+';
    private static final String OPERATORS = "=><←→↔";
    private static final char QUOTE = '\'';
    private static final char REVERSE_RULE_OP = '<';
    private static final char RULE_COMMENT_CHAR = '#';
    private static final char SEGMENT_CLOSE = ')';
    private static final char SEGMENT_OPEN = '(';
    private static final char VARIABLE_DEF_OP = '=';
    private static final char ZERO_OR_ONE = '?';
    public UnicodeSet compoundFilter;
    private Data curData;
    public List<Data> dataVector;
    private int direction;
    private int dotStandIn = -1;
    public List<String> idBlockVector;
    private ParseData parseData;
    private List<StringMatcher> segmentObjects;
    private StringBuffer segmentStandins;
    private String undefinedVariableName;
    private char variableLimit;
    private Map<String, char[]> variableNames;
    private char variableNext;
    private List<Object> variablesVector;

    private static abstract class RuleBody {
        abstract String handleNextLine();

        abstract void reset();

        private RuleBody() {
        }

        String nextLine() {
            String s = handleNextLine();
            if (s == null || s.length() <= 0 || s.charAt(s.length() - 1) != '\\') {
                return s;
            }
            StringBuilder b = new StringBuilder(s);
            do {
                b.deleteCharAt(b.length() - 1);
                s = handleNextLine();
                if (s != null) {
                    b.append(s);
                    if (s.length() <= 0) {
                        break;
                    }
                } else {
                    break;
                }
            } while (s.charAt(s.length() - 1) == '\\');
            return b.toString();
        }
    }

    private static class RuleHalf {
        public boolean anchorEnd;
        public boolean anchorStart;
        public int ante;
        public int cursor;
        public int cursorOffset;
        private int cursorOffsetPos;
        private int nextSegmentNumber;
        public int post;
        public String text;

        private RuleHalf() {
            this.cursor = -1;
            this.ante = -1;
            this.post = -1;
            this.cursorOffset = 0;
            this.cursorOffsetPos = 0;
            this.anchorStart = false;
            this.anchorEnd = false;
            this.nextSegmentNumber = 1;
        }

        public int parse(String rule, int pos, int limit, TransliteratorParser parser) {
            int start = pos;
            StringBuffer buf = new StringBuffer();
            pos = parseSection(rule, pos, limit, parser, buf, TransliteratorParser.ILLEGAL_TOP, false);
            this.text = buf.toString();
            if (this.cursorOffset > 0 && this.cursor != this.cursorOffsetPos) {
                TransliteratorParser.syntaxError("Misplaced |", rule, start);
            }
            return pos;
        }

        /* JADX WARNING: Missing block: B:79:0x015c, code skipped:
            r22 = r4;
            r23 = r5;
            r24 = r6;
            r14 = r11;
            r37 = r15;
            r0 = true;
     */
        /* JADX WARNING: Missing block: B:80:0x0166, code skipped:
            r11 = r3;
            r15 = r7;
     */
        /* JADX WARNING: Missing block: B:116:0x0228, code skipped:
            if (r45 == false) goto L_0x0237;
     */
        /* JADX WARNING: Missing block: B:118:0x022e, code skipped:
            if (r43.length() != r5) goto L_0x0237;
     */
        /* JADX WARNING: Missing block: B:119:0x0230, code skipped:
            android.icu.text.TransliteratorParser.syntaxError("Misplaced quantifier", r10, r15);
     */
        /* JADX WARNING: Missing block: B:121:0x023b, code skipped:
            if (r43.length() != r6) goto L_0x0246;
     */
        /* JADX WARNING: Missing block: B:122:0x023d, code skipped:
            r0 = r17;
            r8 = r6;
     */
        /* JADX WARNING: Missing block: B:123:0x0240, code skipped:
            r27 = r0;
            r24 = r8;
     */
        /* JADX WARNING: Missing block: B:125:0x024a, code skipped:
            if (r43.length() != r4) goto L_0x0250;
     */
        /* JADX WARNING: Missing block: B:126:0x024c, code skipped:
            r0 = r18;
            r8 = r4;
     */
        /* JADX WARNING: Missing block: B:127:0x0250, code skipped:
            r0 = r43.length() - 1;
            r27 = r0;
            r24 = r0 + 1;
     */
        /* JADX WARNING: Missing block: B:129:?, code skipped:
            r21 = new android.icu.text.StringMatcher(r43.toString(), r27, r24, 0, android.icu.text.TransliteratorParser.access$100(r42));
     */
        /* JADX WARNING: Missing block: B:130:0x026f, code skipped:
            r19 = 0;
            r20 = Integer.MAX_VALUE;
     */
        /* JADX WARNING: Missing block: B:131:0x0278, code skipped:
            if (r2 == android.icu.text.TransliteratorParser.ONE_OR_MORE) goto L_0x0288;
     */
        /* JADX WARNING: Missing block: B:133:0x027c, code skipped:
            if (r2 == android.icu.text.TransliteratorParser.ZERO_OR_ONE) goto L_0x0283;
     */
        /* JADX WARNING: Missing block: B:134:0x027e, code skipped:
            r1 = r20;
     */
        /* JADX WARNING: Missing block: B:135:0x0283, code skipped:
            r19 = 0;
            r20 = 1;
     */
        /* JADX WARNING: Missing block: B:136:0x0288, code skipped:
            r19 = 1;
     */
        /* JADX WARNING: Missing block: B:137:0x028b, code skipped:
            r28 = r2;
            r0 = new android.icu.text.Quantifier(r21, r19, r1);
            r13.setLength(r27);
            r29 = r1;
            r13.append(r12.generateStandInFor(r0));
     */
        /* JADX WARNING: Missing block: B:138:0x02a3, code skipped:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:139:0x02a4, code skipped:
            r28 = r2;
            r2 = r27;
     */
        /* JADX WARNING: Missing block: B:140:0x02aa, code skipped:
            if (r3 < 50) goto L_0x02ac;
     */
        /* JADX WARNING: Missing block: B:141:0x02ac, code skipped:
            r8 = r10.substring(0, r3);
     */
        /* JADX WARNING: Missing block: B:142:0x02b2, code skipped:
            r8 = new java.lang.StringBuilder();
            r8.append("...");
            r8.append(r10.substring(r3 - 50, r3));
            r8 = r8.toString();
     */
        /* JADX WARNING: Missing block: B:143:0x02c9, code skipped:
            r1 = r8;
            r30 = r2;
     */
        /* JADX WARNING: Missing block: B:144:0x02d0, code skipped:
            if ((r11 - r3) <= '2') goto L_0x02d2;
     */
        /* JADX WARNING: Missing block: B:145:0x02d2, code skipped:
            r2 = r10.substring(r3, r11);
     */
        /* JADX WARNING: Missing block: B:146:0x02d7, code skipped:
            r2 = new java.lang.StringBuilder();
            r2.append(r10.substring(r3, r3 + 50));
            r2.append("...");
            r2 = r2.toString();
     */
        /* JADX WARNING: Missing block: B:147:0x02ee, code skipped:
            r31 = r3;
            r3 = new java.lang.StringBuilder();
            r32 = r4;
            r3.append("Failure in rule: ");
            r3.append(r1);
            r3.append("$$$");
            r3.append(r2);
     */
        /* JADX WARNING: Missing block: B:148:0x0314, code skipped:
            throw new android.icu.impl.IllegalIcuArgumentException(r3.toString()).initCause(r0);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private int parseSection(String rule, int pos, int limit, TransliteratorParser parser, StringBuffer buf, UnicodeSet illegal, boolean isSegment) {
            int bufStart;
            int quoteLimit;
            int i;
            String str = rule;
            int i2 = limit;
            TransliteratorParser transliteratorParser = parser;
            StringBuffer stringBuffer = buf;
            int start = pos;
            int varLimit = -1;
            int[] iref = new int[1];
            int bufStart2 = buf.length();
            ParsePosition pp = null;
            int quoteStart = -1;
            int quoteLimit2 = -1;
            int varStart = -1;
            int pos2 = pos;
            while (pos2 < i2) {
                int pos3 = pos2 + 1;
                char c = str.charAt(pos2);
                if (PatternProps.isWhiteSpace(c)) {
                    pos2 = pos3;
                } else {
                    int i3;
                    int i4;
                    int varLimit2;
                    if (TransliteratorParser.HALF_ENDERS.indexOf(c) >= 0) {
                        if (isSegment) {
                            TransliteratorParser.syntaxError("Unclosed segment", str, start);
                        }
                        i3 = pos3;
                        i4 = varLimit;
                    } else {
                        if (this.anchorEnd) {
                            TransliteratorParser.syntaxError("Malformed variable reference", str, start);
                        }
                        ParsePosition pp2;
                        int pos4;
                        if (UnicodeSet.resemblesPattern(str, pos3 - 1)) {
                            if (pp == null) {
                                pp2 = new ParsePosition(0);
                            } else {
                                pp2 = pp;
                            }
                            pp2.setIndex(pos3 - 1);
                            stringBuffer.append(transliteratorParser.parseSet(str, pp2));
                            pp = pp2;
                            pos2 = pp2.getIndex();
                        } else if (c == '\\') {
                            if (pos3 == i2) {
                                TransliteratorParser.syntaxError("Trailing backslash", str, start);
                            }
                            iref[0] = pos3;
                            pos2 = Utility.unescapeAt(str, iref);
                            pos4 = iref[0];
                            if (pos2 == -1) {
                                TransliteratorParser.syntaxError("Malformed escape", str, start);
                            }
                            transliteratorParser.checkVariableRange(pos2, str, start);
                            UTF16.append(stringBuffer, pos2);
                            pos2 = pos4;
                        } else if (c == '\'') {
                            pos4 = str.indexOf(39, pos3);
                            if (pos4 == pos3) {
                                stringBuffer.append(c);
                                pos2 = pos3 + 1;
                            } else {
                                quoteStart = buf.length();
                                while (true) {
                                    if (pos4 < 0) {
                                        TransliteratorParser.syntaxError("Unterminated quote", str, start);
                                    }
                                    stringBuffer.append(str.substring(pos3, pos4));
                                    pos3 = pos4 + 1;
                                    if (pos3 >= i2 || str.charAt(pos3) != '\'') {
                                        quoteLimit2 = buf.length();
                                    } else {
                                        pos4 = str.indexOf(39, pos3 + 1);
                                    }
                                }
                                quoteLimit2 = buf.length();
                                for (pos2 = quoteStart; pos2 < quoteLimit2; pos2++) {
                                    transliteratorParser.checkVariableRange(stringBuffer.charAt(pos2), str, start);
                                }
                                pos2 = pos3;
                            }
                        } else {
                            boolean z;
                            int[] iref2;
                            boolean z2;
                            int start2;
                            boolean z3;
                            transliteratorParser.checkVariableRange(c, str, start);
                            if (illegal.contains((int) c)) {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Illegal character '");
                                stringBuilder.append(c);
                                stringBuilder.append('\'');
                                TransliteratorParser.syntaxError(stringBuilder.toString(), str, start);
                            }
                            if (c != SymbolTable.SYMBOL_REF) {
                                int[] iref3;
                                if (c != TransliteratorParser.FUNCTION) {
                                    int pos5;
                                    boolean z4;
                                    if (c == TransliteratorParser.DOT) {
                                        pos5 = pos3;
                                        varLimit2 = varLimit;
                                        bufStart = bufStart2;
                                        quoteLimit = quoteLimit2;
                                        iref3 = iref;
                                        z4 = true;
                                        stringBuffer.append(parser.getDotStandIn());
                                    } else if (c != TransliteratorParser.ANCHOR_START) {
                                        if (c != TransliteratorParser.ALT_FUNCTION) {
                                            switch (c) {
                                                case '(':
                                                    char c2 = c;
                                                    i3 = pos3;
                                                    i4 = varLimit;
                                                    pos2 = buf.length();
                                                    pos4 = this.nextSegmentNumber;
                                                    this.nextSegmentNumber = pos4 + 1;
                                                    varLimit = i3;
                                                    pos5 = varLimit;
                                                    varLimit2 = i4;
                                                    bufStart = bufStart2;
                                                    quoteLimit = quoteLimit2;
                                                    i2 = pos4;
                                                    iref3 = iref;
                                                    pos4 = parseSection(str, varLimit, i2, transliteratorParser, stringBuffer, TransliteratorParser.ILLEGAL_SEG, 1);
                                                    transliteratorParser.setSegmentObject(i2, new StringMatcher(stringBuffer.substring(pos2), i2, parser.curData));
                                                    stringBuffer.setLength(pos2);
                                                    stringBuffer.append(transliteratorParser.getSegmentStandin(i2));
                                                    pos2 = pos4;
                                                    z = true;
                                                    bufStart2 = start;
                                                    iref2 = iref3;
                                                    i = limit;
                                                    break;
                                                case ')':
                                                    i3 = pos3;
                                                    i4 = varLimit;
                                                    break;
                                                case '*':
                                                case '+':
                                                    break;
                                                default:
                                                    StringBuilder stringBuilder2;
                                                    switch (c) {
                                                        case '?':
                                                            break;
                                                        case '@':
                                                            if (this.cursorOffset < 0) {
                                                                if (buf.length() > 0) {
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("Misplaced ");
                                                                    stringBuilder2.append(c);
                                                                    TransliteratorParser.syntaxError(stringBuilder2.toString(), str, start);
                                                                }
                                                                this.cursorOffset--;
                                                            } else if (this.cursorOffset > 0) {
                                                                if (buf.length() != this.cursorOffsetPos || this.cursor >= 0) {
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("Misplaced ");
                                                                    stringBuilder2.append(c);
                                                                    TransliteratorParser.syntaxError(stringBuilder2.toString(), str, start);
                                                                }
                                                                this.cursorOffset++;
                                                            } else if (this.cursor == 0 && buf.length() == 0) {
                                                                this.cursorOffset = -1;
                                                            } else if (this.cursor < 0) {
                                                                this.cursorOffsetPos = buf.length();
                                                                z2 = true;
                                                                this.cursorOffset = 1;
                                                                varLimit2 = varLimit;
                                                                bufStart = bufStart2;
                                                                quoteLimit = quoteLimit2;
                                                                i = i2;
                                                                start2 = start;
                                                                break;
                                                            } else {
                                                                stringBuilder2 = new StringBuilder();
                                                                stringBuilder2.append("Misplaced ");
                                                                stringBuilder2.append(c);
                                                                TransliteratorParser.syntaxError(stringBuilder2.toString(), str, start);
                                                            }
                                                            break;
                                                        default:
                                                            switch (c) {
                                                                case '{':
                                                                    if (this.ante >= 0) {
                                                                        TransliteratorParser.syntaxError("Multiple ante contexts", str, start);
                                                                    }
                                                                    this.ante = buf.length();
                                                                case '|':
                                                                    if (this.cursor >= 0) {
                                                                        TransliteratorParser.syntaxError("Multiple cursors", str, start);
                                                                    }
                                                                    this.cursor = buf.length();
                                                                case '}':
                                                                    if (this.post >= 0) {
                                                                        TransliteratorParser.syntaxError("Multiple post contexts", str, start);
                                                                    }
                                                                    this.post = buf.length();
                                                                default:
                                                                    if (c >= '!' && c <= TransliteratorParser.FWDREV_RULE_OP && ((c < '0' || c > '9') && ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z')))) {
                                                                        stringBuilder2 = new StringBuilder();
                                                                        stringBuilder2.append("Unquoted ");
                                                                        stringBuilder2.append(c);
                                                                        TransliteratorParser.syntaxError(stringBuilder2.toString(), str, start);
                                                                    }
                                                                    stringBuffer.append(c);
                                                            }
                                                    }
                                                    break;
                                            }
                                        }
                                    } else {
                                        pos5 = pos3;
                                        varLimit2 = varLimit;
                                        bufStart = bufStart2;
                                        quoteLimit = quoteLimit2;
                                        iref3 = iref;
                                        z4 = true;
                                        if (buf.length() != 0 || this.anchorStart) {
                                            TransliteratorParser.syntaxError("Misplaced anchor start", str, start);
                                        } else {
                                            this.anchorStart = true;
                                        }
                                    }
                                    z2 = z4;
                                    start2 = start;
                                    i2 = pos5;
                                    iref2 = iref3;
                                    i = limit;
                                }
                                varLimit2 = varLimit;
                                bufStart = bufStart2;
                                quoteLimit = quoteLimit2;
                                iref3 = iref;
                                iref3[0] = pos3;
                                int[] iref4 = iref3;
                                SingleID single = TransliteratorIDParser.parseFilterID(str, iref4);
                                if (single == null || !Utility.parseChar(str, iref4, TransliteratorParser.SEGMENT_OPEN)) {
                                    TransliteratorParser.syntaxError("Invalid function", str, start);
                                }
                                Transliterator t = single.getInstance();
                                if (t == null) {
                                    TransliteratorParser.syntaxError("Invalid function ID", str, start);
                                }
                                bufStart2 = limit;
                                i = bufStart2;
                                int bufSegStart = buf.length();
                                Transliterator t2 = t;
                                start2 = start;
                                iref2 = iref4;
                                pos4 = parseSection(str, iref4[0], bufStart2, transliteratorParser, stringBuffer, TransliteratorParser.ILLEGAL_FUNC, 1);
                                varLimit = bufSegStart;
                                c = new FunctionReplacer(t2, new StringReplacer(stringBuffer.substring(varLimit), parser.curData));
                                stringBuffer.setLength(varLimit);
                                stringBuffer.append(transliteratorParser.generateStandInFor(c));
                                pos2 = pos4;
                                bufStart2 = start2;
                                z = true;
                                z3 = z;
                                i2 = i;
                                iref = iref2;
                                varLimit = varLimit2;
                                quoteLimit2 = quoteLimit;
                                start = bufStart2;
                                bufStart2 = bufStart;
                            } else {
                                varLimit2 = varLimit;
                                bufStart = bufStart2;
                                quoteLimit = quoteLimit2;
                                i = i2;
                                start2 = start;
                                i2 = pos3;
                                iref2 = iref;
                                if (i2 == i) {
                                    z2 = true;
                                    this.anchorEnd = true;
                                } else {
                                    pos3 = UCharacter.digit(str.charAt(i2), 10);
                                    if (pos3 < 1 || pos3 > 9) {
                                        bufStart2 = start2;
                                        if (pp == null) {
                                            pp2 = new ParsePosition(0);
                                        } else {
                                            pp2 = pp;
                                        }
                                        pp2.setIndex(i2);
                                        String name = parser.parseData.parseReference(str, pp2, i);
                                        if (name == null) {
                                            z = true;
                                            this.anchorEnd = true;
                                            pp = pp2;
                                            pos2 = i2;
                                            z3 = z;
                                            i2 = i;
                                            iref = iref2;
                                            varLimit = varLimit2;
                                            quoteLimit2 = quoteLimit;
                                            start = bufStart2;
                                            bufStart2 = bufStart;
                                        } else {
                                            z = true;
                                            quoteLimit2 = pp2.getIndex();
                                            varStart = buf.length();
                                            transliteratorParser.appendVariableDef(name, stringBuffer);
                                            pp = pp2;
                                            varLimit2 = buf.length();
                                        }
                                    } else {
                                        iref2[0] = i2;
                                        pos2 = Utility.parseNumber(str, iref2, 10);
                                        if (pos2 < 0) {
                                            bufStart2 = start2;
                                            TransliteratorParser.syntaxError("Undefined segment reference", str, bufStart2);
                                        } else {
                                            bufStart2 = start2;
                                        }
                                        int pos6 = iref2[0];
                                        stringBuffer.append(transliteratorParser.getSegmentStandin(pos2));
                                        quoteLimit2 = pos6;
                                        z = true;
                                    }
                                    pos2 = quoteLimit2;
                                    z3 = z;
                                    i2 = i;
                                    iref = iref2;
                                    varLimit = varLimit2;
                                    quoteLimit2 = quoteLimit;
                                    start = bufStart2;
                                    bufStart2 = bufStart;
                                }
                            }
                            z = z2;
                            pos2 = i2;
                            bufStart2 = start2;
                            z3 = z;
                            i2 = i;
                            iref = iref2;
                            varLimit = varLimit2;
                            quoteLimit2 = quoteLimit;
                            start = bufStart2;
                            bufStart2 = bufStart;
                        }
                    }
                    bufStart = bufStart2;
                    quoteLimit = quoteLimit2;
                    i = i2;
                    bufStart2 = start;
                    varLimit2 = i4;
                    start = iref;
                    return i3;
                }
            }
            bufStart = bufStart2;
            quoteLimit = quoteLimit2;
            i = i2;
            return pos2;
        }

        void removeContext() {
            int i;
            String str = this.text;
            if (this.ante < 0) {
                i = 0;
            } else {
                i = this.ante;
            }
            this.text = str.substring(i, this.post < 0 ? this.text.length() : this.post);
            this.post = -1;
            this.ante = -1;
            this.anchorEnd = false;
            this.anchorStart = false;
        }

        public boolean isValidOutput(TransliteratorParser parser) {
            int i = 0;
            while (i < this.text.length()) {
                int c = UTF16.charAt(this.text, i);
                i += UTF16.getCharCount(c);
                if (!parser.parseData.isReplacer(c)) {
                    return false;
                }
            }
            return true;
        }

        public boolean isValidInput(TransliteratorParser parser) {
            int i = 0;
            while (i < this.text.length()) {
                int c = UTF16.charAt(this.text, i);
                i += UTF16.getCharCount(c);
                if (!parser.parseData.isMatcher(c)) {
                    return false;
                }
            }
            return true;
        }
    }

    private class ParseData implements SymbolTable {
        private ParseData() {
        }

        public char[] lookup(String name) {
            return (char[]) TransliteratorParser.this.variableNames.get(name);
        }

        public UnicodeMatcher lookupMatcher(int ch) {
            int i = ch - TransliteratorParser.this.curData.variablesBase;
            if (i < 0 || i >= TransliteratorParser.this.variablesVector.size()) {
                return null;
            }
            return (UnicodeMatcher) TransliteratorParser.this.variablesVector.get(i);
        }

        public String parseReference(String text, ParsePosition pos, int limit) {
            int start = pos.getIndex();
            int i = start;
            while (i < limit) {
                char c = text.charAt(i);
                if ((i == start && !UCharacter.isUnicodeIdentifierStart(c)) || !UCharacter.isUnicodeIdentifierPart(c)) {
                    break;
                }
                i++;
            }
            if (i == start) {
                return null;
            }
            pos.setIndex(i);
            return text.substring(start, i);
        }

        public boolean isMatcher(int ch) {
            int i = ch - TransliteratorParser.this.curData.variablesBase;
            if (i < 0 || i >= TransliteratorParser.this.variablesVector.size()) {
                return true;
            }
            return TransliteratorParser.this.variablesVector.get(i) instanceof UnicodeMatcher;
        }

        public boolean isReplacer(int ch) {
            int i = ch - TransliteratorParser.this.curData.variablesBase;
            if (i < 0 || i >= TransliteratorParser.this.variablesVector.size()) {
                return true;
            }
            return TransliteratorParser.this.variablesVector.get(i) instanceof UnicodeReplacer;
        }
    }

    private static class RuleArray extends RuleBody {
        String[] array;
        int i = 0;

        public RuleArray(String[] array) {
            super();
            this.array = array;
        }

        public String handleNextLine() {
            if (this.i >= this.array.length) {
                return null;
            }
            String[] strArr = this.array;
            int i = this.i;
            this.i = i + 1;
            return strArr[i];
        }

        public void reset() {
            this.i = 0;
        }
    }

    public void parse(String rules, int dir) {
        parseRules(new RuleArray(new String[]{rules}), dir);
    }

    /* JADX WARNING: Removed duplicated region for block: B:126:0x021b  */
    /* JADX WARNING: Removed duplicated region for block: B:125:0x0213  */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x022c A:{LOOP_END, LOOP:3: B:128:0x0224->B:130:0x022c} */
    /* JADX WARNING: Removed duplicated region for block: B:135:0x025d A:{Catch:{ IllegalArgumentException -> 0x02ad }} */
    /* JADX WARNING: Removed duplicated region for block: B:150:0x027e A:{LOOP_END, Catch:{ IllegalArgumentException -> 0x02ad }, LOOP:4: B:148:0x0276->B:150:0x027e} */
    /* JADX WARNING: Removed duplicated region for block: B:169:0x02ec A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x02ba  */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x02ed  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01d8 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01d8 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x02ed  */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x02ed  */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01d8 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:174:0x01d8 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x02ed  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void parseRules(RuleBody ruleArray, int dir) {
        int compoundFilterOffset;
        int i;
        int pos;
        Throwable e;
        IllegalArgumentException e2;
        this.dataVector = new ArrayList();
        this.idBlockVector = new ArrayList();
        Data pos2 = null;
        this.curData = null;
        this.direction = dir;
        this.compoundFilter = null;
        this.variablesVector = new ArrayList();
        this.variableNames = new HashMap();
        this.parseData = new ParseData();
        List<RuntimeException> errors = new ArrayList();
        ruleArray.reset();
        StringBuilder idBlockResult = new StringBuilder();
        this.compoundFilter = null;
        int errorCount = 0;
        int ruleCount = 0;
        boolean parsingIDs = true;
        int compoundFilterOffset2 = -1;
        loop0:
        while (true) {
            String rule = ruleArray.nextLine();
            int i2 = 1;
            int i3 = 0;
            if (rule == null) {
                compoundFilterOffset = compoundFilterOffset2;
                i = errorCount;
                break;
            }
            int compoundFilterOffset3;
            char c = 0;
            char limit = rule.length();
            i = errorCount;
            boolean parsingIDs2 = parsingIDs;
            int compoundFilterOffset4 = compoundFilterOffset2;
            while (c < limit) {
                char pos3 = c + 1;
                c = rule.charAt(c);
                if (!PatternProps.isWhiteSpace(c)) {
                    if (c == RULE_COMMENT_CHAR) {
                        pos3 = rule.indexOf("\n", pos3) + 1;
                        if (pos3 == 0) {
                            break;
                        }
                    } else if (c != END_OF_RULE) {
                        Data data;
                        int i4;
                        ruleCount++;
                        pos = pos3 - 1;
                        if ((pos + 2) + i2 <= limit) {
                            if (rule.regionMatches(pos, ID_TOKEN, i3, 2)) {
                                int[] p;
                                SingleID id;
                                SingleID singleID;
                                int[] iArr;
                                pos += 2;
                                pos3 = rule.charAt(pos);
                                while (PatternProps.isWhiteSpace(pos3) && pos < limit) {
                                    pos++;
                                    try {
                                        pos3 = rule.charAt(pos);
                                    } catch (IllegalArgumentException e3) {
                                        e = e3;
                                        compoundFilterOffset3 = compoundFilterOffset4;
                                        parsingIDs = parsingIDs2;
                                        if (i == 30) {
                                        }
                                    }
                                }
                                try {
                                    p = new int[i2];
                                    p[i3] = pos;
                                    if (!parsingIDs2) {
                                        if (this.curData != null) {
                                            if (this.direction == 0) {
                                                this.dataVector.add(this.curData);
                                            } else {
                                                this.dataVector.add(i3, this.curData);
                                            }
                                            this.curData = null;
                                        }
                                        parsingIDs2 = true;
                                    }
                                    id = TransliteratorIDParser.parseSingleID(rule, p, this.direction);
                                    if (p[i3] != pos) {
                                        if (Utility.parseChar(rule, p, END_OF_RULE)) {
                                            if (this.direction == 0) {
                                                idBlockResult.append(id.canonID);
                                                idBlockResult.append(END_OF_RULE);
                                            } else {
                                                StringBuilder stringBuilder = new StringBuilder();
                                                stringBuilder.append(id.canonID);
                                                stringBuilder.append(END_OF_RULE);
                                                idBlockResult.insert(0, stringBuilder.toString());
                                            }
                                            compoundFilterOffset3 = compoundFilterOffset4;
                                            singleID = id;
                                            compoundFilterOffset4 = compoundFilterOffset3;
                                            try {
                                                e2 = p[0];
                                                compoundFilterOffset3 = compoundFilterOffset4;
                                                c = e2;
                                                compoundFilterOffset4 = compoundFilterOffset3;
                                                data = null;
                                                i3 = 0;
                                                i4 = 1;
                                            } catch (IllegalArgumentException e4) {
                                                e = e4;
                                                compoundFilterOffset3 = compoundFilterOffset4;
                                                parsingIDs = parsingIDs2;
                                                if (i == 30) {
                                                }
                                            }
                                            pos2 = data;
                                            i2 = i4;
                                        }
                                    }
                                    iArr = new int[1];
                                } catch (IllegalArgumentException e5) {
                                    e = e5;
                                    compoundFilterOffset3 = compoundFilterOffset4;
                                    parsingIDs = parsingIDs2;
                                    if (i == 30) {
                                        IllegalIcuArgumentException icuEx = new IllegalIcuArgumentException("\nMore than 30 errors; further messages squelched");
                                        icuEx.initCause(e);
                                        errors.add(icuEx);
                                        compoundFilterOffset = compoundFilterOffset3;
                                        if (parsingIDs) {
                                        }
                                        if (this.direction != 0) {
                                        }
                                        while (compoundFilterOffset2 < this.dataVector.size()) {
                                        }
                                        this.variablesVector = null;
                                        if (this.compoundFilter != null) {
                                        }
                                        while (compoundFilterOffset2 < this.dataVector.size()) {
                                        }
                                        this.idBlockVector.remove(0);
                                        if (errors.size() == 0) {
                                        }
                                    } else {
                                        data = null;
                                        i3 = 0;
                                        e.fillInStackTrace();
                                        errors.add(e);
                                        i++;
                                        i4 = 1;
                                        parsingIDs2 = parsingIDs;
                                        c = ruleEnd(rule, pos, limit) + 1;
                                        compoundFilterOffset4 = compoundFilterOffset3;
                                        pos2 = data;
                                        i2 = i4;
                                    }
                                }
                                try {
                                    iArr[0] = -1;
                                    int[] withParens = iArr;
                                    compoundFilterOffset3 = compoundFilterOffset4;
                                    try {
                                        UnicodeSet f = TransliteratorIDParser.parseGlobalFilter(rule, p, this.direction, withParens, 0);
                                        if (f == null || !Utility.parseChar(rule, p, END_OF_RULE)) {
                                            syntaxError("Invalid ::ID", rule, pos);
                                            compoundFilterOffset4 = compoundFilterOffset3;
                                            e2 = p[0];
                                            compoundFilterOffset3 = compoundFilterOffset4;
                                            c = e2;
                                            compoundFilterOffset4 = compoundFilterOffset3;
                                            data = null;
                                            i3 = 0;
                                            i4 = 1;
                                            pos2 = data;
                                            i2 = i4;
                                        } else {
                                            SingleID singleID2 = this.direction == 0 ? true : null;
                                            if (withParens[0] == 0) {
                                                singleID = id;
                                                id = true;
                                            } else {
                                                id = null;
                                            }
                                            if (singleID2 == id) {
                                                if (this.compoundFilter != null) {
                                                    syntaxError("Multiple global filters", rule, pos);
                                                }
                                                this.compoundFilter = f;
                                                compoundFilterOffset4 = ruleCount;
                                                e2 = p[0];
                                                compoundFilterOffset3 = compoundFilterOffset4;
                                                c = e2;
                                                compoundFilterOffset4 = compoundFilterOffset3;
                                                data = null;
                                                i3 = 0;
                                                i4 = 1;
                                                pos2 = data;
                                                i2 = i4;
                                            }
                                            compoundFilterOffset4 = compoundFilterOffset3;
                                            e2 = p[0];
                                            compoundFilterOffset3 = compoundFilterOffset4;
                                            c = e2;
                                            compoundFilterOffset4 = compoundFilterOffset3;
                                            data = null;
                                            i3 = 0;
                                            i4 = 1;
                                            pos2 = data;
                                            i2 = i4;
                                        }
                                    } catch (IllegalArgumentException e6) {
                                        e = e6;
                                        parsingIDs = parsingIDs2;
                                        if (i == 30) {
                                        }
                                    }
                                } catch (IllegalArgumentException e7) {
                                    e = e7;
                                    compoundFilterOffset3 = compoundFilterOffset4;
                                    parsingIDs = parsingIDs2;
                                    if (i == 30) {
                                    }
                                }
                            }
                        }
                        compoundFilterOffset3 = compoundFilterOffset4;
                        if (parsingIDs2) {
                            if (this.direction == 0) {
                                this.idBlockVector.add(idBlockResult.toString());
                                compoundFilterOffset = 0;
                            } else {
                                compoundFilterOffset = 0;
                                this.idBlockVector.add(0, idBlockResult.toString());
                            }
                            idBlockResult.delete(compoundFilterOffset, idBlockResult.length());
                            parsingIDs2 = false;
                            this.curData = new Data();
                            setVariableRange(61440, 63743);
                        }
                        if (resemblesPragma(rule, pos, limit)) {
                            e2 = parsePragma(rule, pos, limit);
                            if (e2 < null) {
                                syntaxError("Unrecognized pragma", rule, pos);
                            }
                        } else {
                            e2 = parseRule(rule, pos, limit);
                        }
                        c = e2;
                        compoundFilterOffset4 = compoundFilterOffset3;
                        data = null;
                        i3 = 0;
                        i4 = 1;
                        pos2 = data;
                        i2 = i4;
                    }
                }
                c = pos3;
            }
            compoundFilterOffset3 = compoundFilterOffset4;
            parsingIDs = parsingIDs2;
            pos2 = pos2;
            errorCount = i;
            compoundFilterOffset2 = compoundFilterOffset3;
        }
        if (parsingIDs || idBlockResult.length() <= 0) {
            if (!(parsingIDs || this.curData == null)) {
                if (this.direction != 0) {
                    this.dataVector.add(this.curData);
                } else {
                    this.dataVector.add(0, this.curData);
                }
            }
        } else if (this.direction == 0) {
            this.idBlockVector.add(idBlockResult.toString());
        } else {
            this.idBlockVector.add(0, idBlockResult.toString());
        }
        for (compoundFilterOffset2 = 0; compoundFilterOffset2 < this.dataVector.size(); compoundFilterOffset2++) {
            Data data2 = (Data) this.dataVector.get(compoundFilterOffset2);
            data2.variables = new Object[this.variablesVector.size()];
            this.variablesVector.toArray(data2.variables);
            data2.variableNames = new HashMap();
            data2.variableNames.putAll(this.variableNames);
        }
        this.variablesVector = null;
        try {
            if (this.compoundFilter != null) {
                if (this.direction == 0) {
                    pos = 1;
                    if (compoundFilterOffset == 1) {
                    }
                    throw new IllegalIcuArgumentException("Compound filters misplaced");
                }
                pos = 1;
                if (this.direction == pos) {
                    if (compoundFilterOffset == ruleCount) {
                    }
                    throw new IllegalIcuArgumentException("Compound filters misplaced");
                }
            }
            for (compoundFilterOffset2 = 0; compoundFilterOffset2 < this.dataVector.size(); compoundFilterOffset2++) {
                ((Data) this.dataVector.get(compoundFilterOffset2)).ruleSet.freeze();
            }
            if (this.idBlockVector.size() == 1 && ((String) this.idBlockVector.get(0)).length() == 0) {
                this.idBlockVector.remove(0);
            }
        } catch (IllegalArgumentException e22) {
            e22.fillInStackTrace();
            errors.add(e22);
        }
        if (errors.size() == 0) {
            for (compoundFilterOffset2 = errors.size() - 1; compoundFilterOffset2 > 0; compoundFilterOffset2--) {
                RuntimeException pos4;
                pos = errors.get(compoundFilterOffset2 - 1);
                while (true) {
                    pos4 = (RuntimeException) pos;
                    if (pos4.getCause() == null) {
                        break;
                    }
                    pos = pos4.getCause();
                }
                pos4.initCause((Throwable) errors.get(compoundFilterOffset2));
            }
            throw ((RuntimeException) errors.get(0));
        }
    }

    /* JADX WARNING: Missing block: B:3:0x0039, code skipped:
            if (r8.indexOf(r9) < 0) goto L_0x003b;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int parseRule(String rule, int pos, int limit) {
        String str = rule;
        int i = limit;
        int start = pos;
        char operator = 0;
        this.segmentStandins = new StringBuffer();
        this.segmentObjects = new ArrayList();
        RuleHalf left = new RuleHalf();
        RuleHalf right = new RuleHalf();
        this.undefinedVariableName = null;
        int pos2 = left.parse(str, pos, i, this);
        if (pos2 != i) {
            String str2 = OPERATORS;
            pos2--;
            char charAt = str.charAt(pos2);
            operator = charAt;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No operator pos=");
        stringBuilder.append(pos2);
        syntaxError(stringBuilder.toString(), str, start);
        pos2++;
        if (operator == REVERSE_RULE_OP && pos2 < i && str.charAt(pos2) == FORWARD_RULE_OP) {
            pos2++;
            operator = FWDREV_RULE_OP;
        }
        if (operator == ALT_REVERSE_RULE_OP) {
            operator = REVERSE_RULE_OP;
        } else if (operator == ALT_FORWARD_RULE_OP) {
            operator = FORWARD_RULE_OP;
        } else if (operator == ALT_FWDREV_RULE_OP) {
            operator = FWDREV_RULE_OP;
        }
        pos2 = right.parse(str, pos2, i, this);
        if (pos2 < i) {
            pos2--;
            if (str.charAt(pos2) == END_OF_RULE) {
                pos2++;
            } else {
                syntaxError("Unquoted operator", str, start);
            }
        }
        int n;
        if (operator == VARIABLE_DEF_OP) {
            if (this.undefinedVariableName == null) {
                syntaxError("Missing '$' or duplicate definition", str, start);
            }
            if (!(left.text.length() == 1 && left.text.charAt(0) == this.variableLimit)) {
                syntaxError("Malformed LHS", str, start);
            }
            if (left.anchorStart || left.anchorEnd || right.anchorStart || right.anchorEnd) {
                syntaxError("Malformed variable def", str, start);
            }
            n = right.text.length();
            char[] value = new char[n];
            right.text.getChars(0, n, value, 0);
            this.variableNames.put(this.undefinedVariableName, value);
            this.variableLimit = (char) (this.variableLimit + 1);
            return pos2;
        }
        if (this.undefinedVariableName != null) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Undefined variable $");
            stringBuilder2.append(this.undefinedVariableName);
            syntaxError(stringBuilder2.toString(), str, start);
        }
        if (this.segmentStandins.length() > this.segmentObjects.size()) {
            syntaxError("Undefined segment reference", str, start);
        }
        for (n = 0; n < this.segmentStandins.length(); n++) {
            if (this.segmentStandins.charAt(n) == 0) {
                syntaxError("Internal error", str, start);
            }
        }
        for (n = 0; n < this.segmentObjects.size(); n++) {
            if (this.segmentObjects.get(n) == null) {
                syntaxError("Internal error", str, start);
            }
        }
        if (operator != FWDREV_RULE_OP) {
            if ((this.direction == 0 ? 1 : 0) != (operator == FORWARD_RULE_OP ? 1 : 0)) {
                return pos2;
            }
        }
        if (this.direction == 1) {
            RuleHalf temp = left;
            left = right;
            right = temp;
        }
        if (operator == FWDREV_RULE_OP) {
            right.removeContext();
            left.cursor = -1;
            left.cursorOffset = 0;
        }
        if (left.ante < 0) {
            left.ante = 0;
        }
        if (left.post < 0) {
            left.post = left.text.length();
        }
        if (right.ante >= 0 || right.post >= 0 || left.cursor >= 0 || ((right.cursorOffset != 0 && right.cursor < 0) || right.anchorStart || right.anchorEnd || !left.isValidInput(this) || !right.isValidOutput(this) || left.ante > left.post)) {
            syntaxError("Malformed rule", str, start);
        }
        UnicodeMatcher[] segmentsArray = null;
        if (this.segmentObjects.size() > 0) {
            segmentsArray = new UnicodeMatcher[this.segmentObjects.size()];
            this.segmentObjects.toArray(segmentsArray);
        }
        TransliterationRuleSet transliterationRuleSet = this.curData.ruleSet;
        String str3 = left.text;
        int i2 = left.ante;
        int i3 = left.post;
        String str4 = right.text;
        int i4 = right.cursor;
        int i5 = right.cursorOffset;
        boolean z = left.anchorStart;
        boolean z2 = left.anchorEnd;
        Data data = this.curData;
        TransliterationRule transliterationRule = r10;
        TransliterationRule transliterationRule2 = new TransliterationRule(str3, i2, i3, str4, i4, i5, segmentsArray, z, z2, data);
        transliterationRuleSet.addRule(transliterationRule);
        return pos2;
    }

    private void setVariableRange(int start, int end) {
        if (start > end || start < 0 || end > DateTimePatternGenerator.MATCH_ALL_FIELDS_LENGTH) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid variable range ");
            stringBuilder.append(start);
            stringBuilder.append(", ");
            stringBuilder.append(end);
            throw new IllegalIcuArgumentException(stringBuilder.toString());
        }
        this.curData.variablesBase = (char) start;
        if (this.dataVector.size() == 0) {
            this.variableNext = (char) start;
            this.variableLimit = (char) (end + 1);
        }
    }

    private void checkVariableRange(int ch, String rule, int start) {
        if (ch >= this.curData.variablesBase && ch < this.variableLimit) {
            syntaxError("Variable range character in rule", rule, start);
        }
    }

    private void pragmaMaximumBackup(int backup) {
        throw new IllegalIcuArgumentException("use maximum backup pragma not implemented yet");
    }

    private void pragmaNormalizeRules(Mode mode) {
        throw new IllegalIcuArgumentException("use normalize rules pragma not implemented yet");
    }

    static boolean resemblesPragma(String rule, int pos, int limit) {
        return Utility.parsePattern(rule, pos, limit, "use ", null) >= 0;
    }

    private int parsePragma(String rule, int pos, int limit) {
        int[] array = new int[2];
        pos += 4;
        int p = Utility.parsePattern(rule, pos, limit, "~variable range # #~;", array);
        if (p >= 0) {
            setVariableRange(array[0], array[1]);
            return p;
        }
        p = Utility.parsePattern(rule, pos, limit, "~maximum backup #~;", array);
        if (p >= 0) {
            pragmaMaximumBackup(array[0]);
            return p;
        }
        p = Utility.parsePattern(rule, pos, limit, "~nfd rules~;", null);
        if (p >= 0) {
            pragmaNormalizeRules(Normalizer.NFD);
            return p;
        }
        p = Utility.parsePattern(rule, pos, limit, "~nfc rules~;", null);
        if (p < 0) {
            return -1;
        }
        pragmaNormalizeRules(Normalizer.NFC);
        return p;
    }

    static final void syntaxError(String msg, String rule, int start) {
        int end = ruleEnd(rule, start, rule.length());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(msg);
        stringBuilder.append(" in \"");
        stringBuilder.append(Utility.escape(rule.substring(start, end)));
        stringBuilder.append('\"');
        throw new IllegalIcuArgumentException(stringBuilder.toString());
    }

    static final int ruleEnd(String rule, int start, int limit) {
        int end = Utility.quotedIndexOf(rule, start, limit, ";");
        if (end < 0) {
            return limit;
        }
        return end;
    }

    private final char parseSet(String rule, ParsePosition pos) {
        UnicodeSet set = new UnicodeSet(rule, pos, this.parseData);
        if (this.variableNext < this.variableLimit) {
            set.compact();
            return generateStandInFor(set);
        }
        throw new RuntimeException("Private use variables exhausted");
    }

    char generateStandInFor(Object obj) {
        for (int i = 0; i < this.variablesVector.size(); i++) {
            if (this.variablesVector.get(i) == obj) {
                return (char) (this.curData.variablesBase + i);
            }
        }
        if (this.variableNext < this.variableLimit) {
            this.variablesVector.add(obj);
            char c = this.variableNext;
            this.variableNext = (char) (c + 1);
            return c;
        }
        throw new RuntimeException("Variable range exhausted");
    }

    public char getSegmentStandin(int seg) {
        if (this.segmentStandins.length() < seg) {
            this.segmentStandins.setLength(seg);
        }
        char c = this.segmentStandins.charAt(seg - 1);
        if (c != 0) {
            return c;
        }
        if (this.variableNext < this.variableLimit) {
            char c2 = this.variableNext;
            this.variableNext = (char) (c2 + 1);
            c = c2;
            this.variablesVector.add(null);
            this.segmentStandins.setCharAt(seg - 1, c);
            return c;
        }
        throw new RuntimeException("Variable range exhausted");
    }

    public void setSegmentObject(int seg, StringMatcher obj) {
        while (this.segmentObjects.size() < seg) {
            this.segmentObjects.add(null);
        }
        int index = getSegmentStandin(seg) - this.curData.variablesBase;
        if (this.segmentObjects.get(seg - 1) == null && this.variablesVector.get(index) == null) {
            this.segmentObjects.set(seg - 1, obj);
            this.variablesVector.set(index, obj);
            return;
        }
        throw new RuntimeException();
    }

    char getDotStandIn() {
        if (this.dotStandIn == -1) {
            this.dotStandIn = generateStandInFor(new UnicodeSet(DOT_SET));
        }
        return (char) this.dotStandIn;
    }

    private void appendVariableDef(String name, StringBuffer buf) {
        char[] ch = (char[]) this.variableNames.get(name);
        if (ch != null) {
            buf.append(ch);
        } else if (this.undefinedVariableName == null) {
            this.undefinedVariableName = name;
            if (this.variableNext < this.variableLimit) {
                char c = (char) (this.variableLimit - 1);
                this.variableLimit = c;
                buf.append(c);
                return;
            }
            throw new RuntimeException("Private use variables exhausted");
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Undefined variable $");
            stringBuilder.append(name);
            throw new IllegalIcuArgumentException(stringBuilder.toString());
        }
    }
}
