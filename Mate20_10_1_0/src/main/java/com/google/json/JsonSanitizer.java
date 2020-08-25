package com.google.json;

import com.huawei.nb.authority.AuthorityValue;
import com.huawei.odmf.model.api.Attribute;

public final class JsonSanitizer {
    static final /* synthetic */ boolean $assertionsDisabled;
    public static final int DEFAULT_NESTING_DEPTH = 64;
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    public static final int MAXIMUM_NESTING_DEPTH = 4096;
    private static final boolean SUPER_VERBOSE_AND_SLOW_LOGGING = false;
    private static final UnbracketedComma UNBRACKETED_COMMA = new UnbracketedComma();
    private int bracketDepth;
    private int cleaned;
    private boolean[] isMap;
    private final String jsonish;
    private final int maximumNestingDepth;
    private StringBuilder sanitizedJson;

    private enum State {
        START_ARRAY,
        BEFORE_ELEMENT,
        AFTER_ELEMENT,
        START_MAP,
        BEFORE_KEY,
        AFTER_KEY,
        BEFORE_VALUE,
        AFTER_VALUE
    }

    static {
        boolean z;
        if (!JsonSanitizer.class.desiredAssertionStatus()) {
            z = true;
        } else {
            z = false;
        }
        $assertionsDisabled = z;
        UNBRACKETED_COMMA.setStackTrace(new StackTraceElement[0]);
    }

    public static String sanitize(String jsonish2) {
        return sanitize(jsonish2, 64);
    }

    public static String sanitize(String jsonish2, int maximumNestingDepth2) {
        JsonSanitizer s = new JsonSanitizer(jsonish2, maximumNestingDepth2);
        s.sanitize();
        return s.toString();
    }

    JsonSanitizer(String jsonish2) {
        this(jsonish2, 64);
    }

    JsonSanitizer(String jsonish2, int maximumNestingDepth2) {
        this.maximumNestingDepth = Math.min(Math.max(1, maximumNestingDepth2), (int) MAXIMUM_NESTING_DEPTH);
        this.jsonish = jsonish2 == null ? "null" : jsonish2;
    }

    /* access modifiers changed from: package-private */
    public int getMaximumNestingDepth() {
        return this.maximumNestingDepth;
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x034b  */
    /* JADX WARNING: Removed duplicated region for block: B:133:0x0356  */
    public void sanitize() {
        char c;
        this.cleaned = 0;
        this.bracketDepth = 0;
        this.sanitizedJson = null;
        State state = State.START_ARRAY;
        int n = this.jsonish.length();
        int i = 0;
        while (true) {
            if (i < n) {
                try {
                    char ch = this.jsonish.charAt(i);
                    switch (ch) {
                        case Attribute.DATE /*{ENCODED_INT: 9}*/:
                        case Attribute.TIME /*{ENCODED_INT: 10}*/:
                        case Attribute.TIMESTAMP /*{ENCODED_INT: 13}*/:
                        case AuthorityValue.AUTH_U:
                            break;
                        case '\"':
                        case '\'':
                            state = requireValueState(i, state, true);
                            int strEnd = endOfQuotedString(this.jsonish, i);
                            sanitizeString(i, strEnd);
                            i = strEnd - 1;
                            break;
                        case '(':
                        case ')':
                            elide(i, i + 1);
                            break;
                        case ',':
                            if (this.bracketDepth == 0) {
                                throw UNBRACKETED_COMMA;
                            }
                            switch (state) {
                                case BEFORE_VALUE:
                                    insert(i, "null");
                                    state = State.BEFORE_KEY;
                                    continue;
                                case BEFORE_ELEMENT:
                                case START_ARRAY:
                                    insert(i, "null");
                                    state = State.BEFORE_ELEMENT;
                                    continue;
                                case BEFORE_KEY:
                                case AFTER_KEY:
                                case START_MAP:
                                    elide(i, i + 1);
                                    continue;
                                case AFTER_ELEMENT:
                                    state = State.BEFORE_ELEMENT;
                                    continue;
                                case AFTER_VALUE:
                                    state = State.BEFORE_KEY;
                                    continue;
                            }
                        case '/':
                            int end = i + 1;
                            if (i + 1 < n) {
                                switch (this.jsonish.charAt(i + 1)) {
                                    case '*':
                                        end = n;
                                        if (i + 3 < n) {
                                            int j = i + 2;
                                            while (true) {
                                                j = this.jsonish.indexOf(47, j + 1);
                                                if (j >= 0) {
                                                    if (this.jsonish.charAt(j - 1) == '*') {
                                                        end = j + 1;
                                                        break;
                                                    }
                                                } else {
                                                    break;
                                                }
                                            }
                                        }
                                        break;
                                    case '/':
                                        end = n;
                                        int j2 = i + 2;
                                        while (true) {
                                            if (j2 >= n) {
                                                break;
                                            } else {
                                                char cch = this.jsonish.charAt(j2);
                                                if (cch == '\n' || cch == '\r' || cch == 8232 || cch == 8233) {
                                                    end = j2 + 1;
                                                    break;
                                                } else {
                                                    j2++;
                                                }
                                            }
                                        }
                                        end = j2 + 1;
                                        break;
                                }
                            }
                            elide(i, end);
                            i = end - 1;
                            break;
                        case ':':
                            if (state != State.AFTER_KEY) {
                                elide(i, i + 1);
                                break;
                            } else {
                                state = State.BEFORE_VALUE;
                                break;
                            }
                        case '[':
                        case '{':
                            requireValueState(i, state, false);
                            if (this.isMap == null) {
                                this.isMap = new boolean[this.maximumNestingDepth];
                            }
                            boolean map = ch == '{';
                            this.isMap[this.bracketDepth] = map;
                            this.bracketDepth++;
                            if (!map) {
                                state = State.START_ARRAY;
                                break;
                            } else {
                                state = State.START_MAP;
                                break;
                            }
                        case ']':
                        case '}':
                            if (this.bracketDepth != 0) {
                                switch (state) {
                                    case BEFORE_VALUE:
                                        insert(i, "null");
                                        break;
                                    case BEFORE_ELEMENT:
                                    case BEFORE_KEY:
                                        elideTrailingComma(i);
                                        break;
                                    case AFTER_KEY:
                                        insert(i, ":null");
                                        break;
                                }
                                this.bracketDepth--;
                                char closeBracket = this.isMap[this.bracketDepth] ? '}' : ']';
                                if (ch != closeBracket) {
                                    replace(i, i + 1, closeBracket);
                                }
                                if (this.bracketDepth != 0 && this.isMap[this.bracketDepth - 1]) {
                                    state = State.AFTER_VALUE;
                                    break;
                                } else {
                                    state = State.AFTER_ELEMENT;
                                    break;
                                }
                            } else {
                                elide(i, this.jsonish.length());
                                break;
                            }
                            break;
                        default:
                            int runEnd = i;
                            while (runEnd < n) {
                                char tch = this.jsonish.charAt(runEnd);
                                if (('a' > tch || tch > 'z') && (('0' > tch || tch > '9') && tch != '+' && tch != '-' && tch != '.' && (('A' > tch || tch > 'Z') && tch != '_' && tch != '$'))) {
                                    if (runEnd == i) {
                                        state = requireValueState(i, state, true);
                                        boolean isNumber = ('0' <= ch && ch <= '9') || ch == '.' || ch == '+' || ch == '-';
                                        boolean isKeyword = !isNumber && isKeyword(i, runEnd);
                                        if (!isNumber && !isKeyword) {
                                            while (runEnd < n && !isJsonSpecialChar(runEnd)) {
                                                runEnd++;
                                            }
                                            if (runEnd < n && this.jsonish.charAt(runEnd) == '\"') {
                                                runEnd++;
                                            }
                                        }
                                        if (state == State.AFTER_KEY) {
                                            insert(i, '\"');
                                            if (isNumber) {
                                                canonicalizeNumber(i, runEnd);
                                                insert(runEnd, '\"');
                                            } else {
                                                sanitizeString(i, runEnd);
                                            }
                                        } else if (isNumber) {
                                            normalizeNumber(i, runEnd);
                                        } else if (!isKeyword) {
                                            insert(i, '\"');
                                            sanitizeString(i, runEnd);
                                        }
                                        i = runEnd - 1;
                                        break;
                                    } else {
                                        elide(i, i + 1);
                                        break;
                                    }
                                } else {
                                    runEnd++;
                                }
                            }
                            if (runEnd == i) {
                            }
                            break;
                    }
                    i++;
                } catch (UnbracketedComma e) {
                    elide(i, this.jsonish.length());
                }
            }
        }
        if (state == State.START_ARRAY && this.bracketDepth == 0) {
            insert(n, "null");
            state = State.AFTER_ELEMENT;
        }
        if ((this.sanitizedJson != null && this.sanitizedJson.length() != 0) || this.cleaned != 0 || this.bracketDepth != 0) {
            if (this.sanitizedJson == null) {
                this.sanitizedJson = new StringBuilder(this.bracketDepth + n);
            }
            this.sanitizedJson.append((CharSequence) this.jsonish, this.cleaned, n);
            this.cleaned = n;
            switch (state) {
                case BEFORE_VALUE:
                    this.sanitizedJson.append("null");
                    break;
                case BEFORE_ELEMENT:
                case BEFORE_KEY:
                    elideTrailingComma(n);
                    break;
                case AFTER_KEY:
                    this.sanitizedJson.append(":null");
                    break;
            }
            while (this.bracketDepth != 0) {
                StringBuilder sb = this.sanitizedJson;
                boolean[] zArr = this.isMap;
                int i2 = this.bracketDepth - 1;
                this.bracketDepth = i2;
                if (zArr[i2]) {
                    c = '}';
                } else {
                    c = ']';
                }
                sb.append(c);
            }
        }
    }

    private void sanitizeString(int start, int end) {
        boolean closed = false;
        int i = start;
        while (i < end) {
            char ch = this.jsonish.charAt(i);
            switch (ch) {
                case Attribute.TIME /*{ENCODED_INT: 10}*/:
                    replace(i, i + 1, "\\n");
                    break;
                case Attribute.TIMESTAMP /*{ENCODED_INT: 13}*/:
                    replace(i, i + 1, "\\r");
                    break;
                case '\"':
                case '\'':
                    if (i == start) {
                        if (ch != '\'') {
                            break;
                        } else {
                            replace(i, i + 1, '\"');
                            break;
                        }
                    } else {
                        if (i + 1 == end) {
                            char startDelim = this.jsonish.charAt(start);
                            if (startDelim != '\'') {
                                startDelim = '\"';
                            }
                            closed = startDelim == ch;
                        }
                        if (!closed) {
                            if (ch != '\"') {
                                break;
                            } else {
                                insert(i, '\\');
                                break;
                            }
                        } else if (ch != '\'') {
                            break;
                        } else {
                            replace(i, i + 1, '\"');
                            break;
                        }
                    }
                case '/':
                    if (i > start && i + 2 < end && '<' == this.jsonish.charAt(i - 1) && 's' == (this.jsonish.charAt(i + 1) | ' ') && 'c' == (this.jsonish.charAt(i + 2) | ' ')) {
                        insert(i, '\\');
                        break;
                    }
                case '\\':
                    if (i + 1 == end) {
                        elide(i, i + 1);
                        break;
                    } else {
                        switch (this.jsonish.charAt(i + 1)) {
                            case '\"':
                            case '/':
                            case '\\':
                            case 'b':
                            case 'f':
                            case 'n':
                            case 'r':
                            case 't':
                                i++;
                                continue;
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                                int octalEnd = i + 1;
                                if (octalEnd + 1 < end && isOctAt(octalEnd + 1)) {
                                    octalEnd++;
                                    if (ch <= '3' && octalEnd + 1 < end && isOctAt(octalEnd + 1)) {
                                        octalEnd++;
                                    }
                                    int value = 0;
                                    for (int j = i; j < octalEnd; j++) {
                                        value = (value << 3) | (this.jsonish.charAt(j) - '0');
                                    }
                                    replace(i + 1, octalEnd, "u00");
                                    appendHex(value, 2);
                                }
                                i = octalEnd - 1;
                                continue;
                            case 'u':
                                if (i + 6 >= end || !isHexAt(i + 2) || !isHexAt(i + 3) || !isHexAt(i + 4) || !isHexAt(i + 5)) {
                                    elide(i, i + 1);
                                    break;
                                } else {
                                    i += 5;
                                    continue;
                                }
                                break;
                            case 'v':
                                replace(i, i + 2, "\\u0008");
                                i++;
                                continue;
                            case 'x':
                                if (i + 4 >= end || !isHexAt(i + 2) || !isHexAt(i + 3)) {
                                    elide(i, i + 1);
                                    break;
                                } else {
                                    replace(i, i + 2, "\\u00");
                                    i += 3;
                                    continue;
                                }
                                break;
                            default:
                                elide(i, i + 1);
                                continue;
                        }
                    }
                    break;
                case ']':
                    if (i + 2 < end && ']' == this.jsonish.charAt(i + 1) && '>' == this.jsonish.charAt(i + 2)) {
                        replace(i, i + 1, "\\u005d");
                        break;
                    }
                case 8232:
                    replace(i, i + 1, "\\u2028");
                    break;
                case 8233:
                    replace(i, i + 1, "\\u2029");
                    break;
                default:
                    if (ch >= ' ') {
                        if (ch >= 55296) {
                            if (ch >= 57344) {
                                if (ch <= 65533) {
                                    break;
                                }
                            } else if (Character.isHighSurrogate(ch) && i + 1 < end && Character.isLowSurrogate(this.jsonish.charAt(i + 1))) {
                                i++;
                                break;
                            }
                        } else {
                            break;
                        }
                    } else if (ch != '\t') {
                        if (ch != '\n') {
                            if (ch == '\r') {
                                break;
                            }
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                    replace(i, i + 1, "\\u");
                    int j2 = 4;
                    while (true) {
                        j2--;
                        if (j2 < 0) {
                            break;
                        } else {
                            this.sanitizedJson.append(HEX_DIGITS[(ch >>> (j2 << 2)) & 15]);
                        }
                    }
            }
            i++;
        }
        if (!closed) {
            insert(end, '\"');
        }
    }

    private State requireValueState(int pos, State state, boolean canBeKey) throws UnbracketedComma {
        switch (state) {
            case BEFORE_VALUE:
                return State.AFTER_VALUE;
            case BEFORE_ELEMENT:
            case START_ARRAY:
                return State.AFTER_ELEMENT;
            case BEFORE_KEY:
            case START_MAP:
                if (canBeKey) {
                    return State.AFTER_KEY;
                }
                insert(pos, "\"\":");
                return State.AFTER_VALUE;
            case AFTER_KEY:
                insert(pos, ':');
                return State.AFTER_VALUE;
            case AFTER_ELEMENT:
                if (this.bracketDepth == 0) {
                    throw UNBRACKETED_COMMA;
                }
                insert(pos, ',');
                return State.AFTER_ELEMENT;
            case AFTER_VALUE:
                if (canBeKey) {
                    insert(pos, ',');
                    return State.AFTER_KEY;
                }
                insert(pos, ",\"\":");
                return State.AFTER_VALUE;
            default:
                throw new AssertionError();
        }
    }

    private void insert(int pos, char ch) {
        replace(pos, pos, ch);
    }

    private void insert(int pos, String s) {
        replace(pos, pos, s);
    }

    private void elide(int start, int end) {
        if (this.sanitizedJson == null) {
            this.sanitizedJson = new StringBuilder(this.jsonish.length() + 16);
        }
        this.sanitizedJson.append((CharSequence) this.jsonish, this.cleaned, start);
        this.cleaned = end;
    }

    private void replace(int start, int end, char ch) {
        elide(start, end);
        this.sanitizedJson.append(ch);
    }

    private void replace(int start, int end, String s) {
        elide(start, end);
        this.sanitizedJson.append(s);
    }

    private static int endOfQuotedString(String s, int start) {
        int slashRunStart;
        char quote = s.charAt(start);
        int i = start;
        do {
            i = s.indexOf(quote, i + 1);
            if (i < 0) {
                return s.length();
            }
            slashRunStart = i;
            while (slashRunStart > start && s.charAt(slashRunStart - 1) == '\\') {
                slashRunStart--;
            }
        } while (((i - slashRunStart) & 1) != 0);
        return i + 1;
    }

    private void elideTrailingComma(int closeBracketPos) {
        int i = closeBracketPos;
        while (true) {
            i--;
            if (i >= this.cleaned) {
                switch (this.jsonish.charAt(i)) {
                    case Attribute.DATE /*{ENCODED_INT: 9}*/:
                    case Attribute.TIME /*{ENCODED_INT: 10}*/:
                    case Attribute.TIMESTAMP /*{ENCODED_INT: 13}*/:
                    case AuthorityValue.AUTH_U:
                        break;
                    case ',':
                        elide(i, i + 1);
                        return;
                    default:
                        throw new AssertionError("" + this.jsonish.charAt(i));
                }
            } else if ($assertionsDisabled || this.sanitizedJson != null) {
                int i2 = this.sanitizedJson.length();
                while (true) {
                    i2--;
                    if (i2 >= 0) {
                        switch (this.sanitizedJson.charAt(i2)) {
                            case Attribute.DATE /*{ENCODED_INT: 9}*/:
                            case Attribute.TIME /*{ENCODED_INT: 10}*/:
                            case Attribute.TIMESTAMP /*{ENCODED_INT: 13}*/:
                            case AuthorityValue.AUTH_U:
                                break;
                            case ',':
                                this.sanitizedJson.setLength(i2);
                                return;
                            default:
                                throw new AssertionError("" + this.sanitizedJson.charAt(i2));
                        }
                    } else {
                        throw new AssertionError("Trailing comma not found in " + this.jsonish + " or " + ((Object) this.sanitizedJson));
                    }
                }
            } else {
                throw new AssertionError();
            }
        }
    }

    private void normalizeNumber(int start, int end) {
        int digVal;
        int pos = start;
        if (pos < end) {
            switch (this.jsonish.charAt(pos)) {
                case '+':
                    elide(pos, pos + 1);
                    pos++;
                    break;
                case '-':
                    pos++;
                    break;
            }
        }
        int intEnd = endOfDigitRun(pos, end);
        if (pos == intEnd) {
            insert(pos, '0');
        } else if ('0' == this.jsonish.charAt(pos)) {
            if (intEnd - pos == 1 && intEnd < end && 'x' == (this.jsonish.charAt(intEnd) | ' ')) {
                int value = 0;
                intEnd++;
                while (intEnd < end) {
                    char ch = this.jsonish.charAt(intEnd);
                    if ('0' > ch || ch > '9') {
                        char ch2 = (char) (ch | ' ');
                        if ('a' <= ch2 && ch2 <= 'f') {
                            digVal = ch2 - 'W';
                        }
                        elide(pos, intEnd);
                        this.sanitizedJson.append(value);
                    } else {
                        digVal = ch - '0';
                    }
                    value = (value << 4) | digVal;
                    intEnd++;
                }
                elide(pos, intEnd);
                this.sanitizedJson.append(value);
            } else if (intEnd - pos > 1) {
                int value2 = 0;
                for (int i = pos; i < intEnd; i++) {
                    value2 = (value2 << 3) | (this.jsonish.charAt(i) - '0');
                }
                elide(pos, intEnd);
                this.sanitizedJson.append(value2);
            }
        }
        int pos2 = intEnd;
        if (pos2 < end && this.jsonish.charAt(pos2) == '.') {
            int pos3 = pos2 + 1;
            int fractionEnd = endOfDigitRun(pos3, end);
            if (fractionEnd == pos3) {
                insert(pos3, '0');
            }
            pos2 = fractionEnd;
        }
        if (pos2 < end && 'e' == (this.jsonish.charAt(pos2) | ' ')) {
            int pos4 = pos2 + 1;
            if (pos4 < end) {
                switch (this.jsonish.charAt(pos4)) {
                    case '+':
                    case '-':
                        pos4++;
                        break;
                }
            }
            int expEnd = endOfDigitRun(pos4, end);
            if (expEnd == pos4) {
                insert(pos4, '0');
            }
            pos2 = expEnd;
        }
        if (pos2 != end) {
            elide(pos2, end);
        }
    }

    private boolean canonicalizeNumber(int start, int end) {
        elide(start, start);
        int sanStart = this.sanitizedJson.length();
        normalizeNumber(start, end);
        elide(end, end);
        return canonicalizeNumber(this.sanitizedJson, sanStart, this.sanitizedJson.length());
    }

    private static boolean canonicalizeNumber(StringBuilder sanitizedJson2, int sanStart, int sanEnd) {
        int fractionStart;
        int fractionEnd;
        int expStart;
        int expEnd;
        int exp;
        char vdigit;
        int intStart = sanStart + (sanitizedJson2.charAt(sanStart) == '-' ? 1 : 0);
        int intEnd = intStart;
        while (intEnd < sanEnd && '0' <= (ch = sanitizedJson2.charAt(intEnd)) && ch <= '9') {
            intEnd++;
        }
        if (intEnd == sanEnd || '.' != sanitizedJson2.charAt(intEnd)) {
            fractionEnd = intEnd;
            fractionStart = intEnd;
        } else {
            fractionStart = intEnd + 1;
            fractionEnd = fractionStart;
            while (fractionEnd < sanEnd && '0' <= (ch = sanitizedJson2.charAt(fractionEnd)) && ch <= '9') {
                fractionEnd++;
            }
        }
        if (fractionEnd == sanEnd) {
            expEnd = sanEnd;
            expStart = sanEnd;
        } else if ($assertionsDisabled || 'e' == (sanitizedJson2.charAt(fractionEnd) | ' ')) {
            expStart = fractionEnd + 1;
            if (sanitizedJson2.charAt(expStart) == '+') {
                expStart++;
            }
            expEnd = sanEnd;
        } else {
            throw new AssertionError();
        }
        if ($assertionsDisabled || (intStart <= intEnd && intEnd <= fractionStart && fractionStart <= fractionEnd && fractionEnd <= expStart && expStart <= expEnd)) {
            if (expEnd == expStart) {
                exp = 0;
            } else {
                try {
                    exp = Integer.parseInt(sanitizedJson2.substring(expStart, expEnd), 10);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            int n = exp;
            boolean sawDecimal = false;
            boolean zero = true;
            int digitOutPos = intStart;
            int nZeroesPending = 0;
            for (int i = intStart; i < fractionEnd; i++) {
                char ch = sanitizedJson2.charAt(i);
                if (ch == '.') {
                    sawDecimal = true;
                    if (zero) {
                        nZeroesPending = 0;
                    }
                } else {
                    char digit = ch;
                    if ((!zero || digit != '0') && !sawDecimal) {
                        n++;
                    }
                    if (digit == '0') {
                        nZeroesPending++;
                    } else {
                        if (zero) {
                            if (sawDecimal) {
                                n -= nZeroesPending;
                            }
                            nZeroesPending = 0;
                        }
                        zero = false;
                        int digitOutPos2 = digitOutPos;
                        while (true) {
                            if (nZeroesPending == 0 && digit == 0) {
                                break;
                            }
                            if (nZeroesPending == 0) {
                                vdigit = digit;
                                digit = 0;
                            } else {
                                vdigit = '0';
                                nZeroesPending--;
                            }
                            sanitizedJson2.setCharAt(digitOutPos2, vdigit);
                            digitOutPos2++;
                        }
                        digitOutPos = digitOutPos2;
                    }
                }
            }
            sanitizedJson2.setLength(digitOutPos);
            int k = digitOutPos - intStart;
            if (zero) {
                sanitizedJson2.setLength(sanStart);
                sanitizedJson2.append('0');
                return true;
            }
            if (k <= n && n <= 21) {
                for (int i2 = k; i2 < n; i2++) {
                    sanitizedJson2.append('0');
                }
            } else if (n > 0 && n <= 21) {
                sanitizedJson2.insert(intStart + n, '.');
            } else if (-6 >= n || n > 0) {
                if (k != 1) {
                    sanitizedJson2.insert(intStart + 1, '.');
                }
                int nLess1 = n - 1;
                sanitizedJson2.append('e').append(nLess1 < 0 ? '-' : '+').append(Math.abs(nLess1));
            } else {
                sanitizedJson2.insert(intStart, "0.000000".substring(0, 2 - n));
            }
            return true;
        }
        throw new AssertionError();
    }

    private boolean isKeyword(int start, int end) {
        int n = end - start;
        if (n == 5) {
            return "false".regionMatches(0, this.jsonish, start, n);
        }
        if (n != 4) {
            return false;
        }
        if ("null".regionMatches(0, this.jsonish, start, n) || "true".regionMatches(0, this.jsonish, start, n)) {
            return true;
        }
        return false;
    }

    private boolean isOctAt(int i) {
        char ch = this.jsonish.charAt(i);
        return '0' <= ch && ch <= '7';
    }

    private boolean isHexAt(int i) {
        char ch = this.jsonish.charAt(i);
        if ('0' <= ch && ch <= '9') {
            return true;
        }
        char ch2 = (char) (ch | ' ');
        if ('a' > ch2 || ch2 > 'f') {
            return false;
        }
        return true;
    }

    private boolean isJsonSpecialChar(int i) {
        char ch = this.jsonish.charAt(i);
        if (ch <= ' ') {
            return true;
        }
        switch (ch) {
            case '\"':
            case ',':
            case ':':
            case '[':
            case ']':
            case '{':
            case '}':
                return true;
            default:
                return false;
        }
    }

    private void appendHex(int n, int nDigits) {
        int i = 0;
        int x = n;
        while (i < nDigits) {
            int dig = x & 15;
            this.sanitizedJson.append((dig < 10 ? 48 : 87) + dig);
            i++;
            x >>>= 4;
        }
    }

    private static final class UnbracketedComma extends Exception {
        private static final long serialVersionUID = 783239978717247850L;

        private UnbracketedComma() {
        }
    }

    private int endOfDigitRun(int start, int limit) {
        int end = start;
        while (end < limit) {
            char ch = this.jsonish.charAt(end);
            if ('0' > ch || ch > '9') {
                return end;
            }
            end++;
        }
        return limit;
    }

    /* access modifiers changed from: package-private */
    public CharSequence toCharSequence() {
        return this.sanitizedJson != null ? this.sanitizedJson : this.jsonish;
    }

    public String toString() {
        return this.sanitizedJson != null ? this.sanitizedJson.toString() : this.jsonish;
    }
}
