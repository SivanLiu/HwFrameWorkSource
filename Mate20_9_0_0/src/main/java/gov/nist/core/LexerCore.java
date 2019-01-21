package gov.nist.core;

import java.text.ParseException;
import java.util.Hashtable;

public class LexerCore extends StringTokenizer {
    public static final int ALPHA = 4099;
    static final char ALPHADIGIT_VALID_CHARS = '�';
    static final char ALPHA_VALID_CHARS = '￿';
    public static final int AND = 38;
    public static final int AT = 64;
    public static final int BACKSLASH = 92;
    public static final int BACK_QUOTE = 96;
    public static final int BAR = 124;
    public static final int COLON = 58;
    public static final int DIGIT = 4098;
    static final char DIGIT_VALID_CHARS = '￾';
    public static final int DOLLAR = 36;
    public static final int DOT = 46;
    public static final int DOUBLEQUOTE = 34;
    public static final int END = 4096;
    public static final int EQUALS = 61;
    public static final int EXCLAMATION = 33;
    public static final int GREATER_THAN = 62;
    public static final int HAT = 94;
    public static final int HT = 9;
    public static final int ID = 4095;
    public static final int LESS_THAN = 60;
    public static final int LPAREN = 40;
    public static final int L_CURLY = 123;
    public static final int L_SQUARE_BRACKET = 91;
    public static final int MINUS = 45;
    public static final int NULL = 0;
    public static final int PERCENT = 37;
    public static final int PLUS = 43;
    public static final int POUND = 35;
    public static final int QUESTION = 63;
    public static final int QUOTE = 39;
    public static final int RPAREN = 41;
    public static final int R_CURLY = 125;
    public static final int R_SQUARE_BRACKET = 93;
    public static final int SAFE = 4094;
    public static final int SEMICOLON = 59;
    public static final int SLASH = 47;
    public static final int SP = 32;
    public static final int STAR = 42;
    public static final int START = 2048;
    public static final int TILDE = 126;
    public static final int UNDERSCORE = 95;
    public static final int WHITESPACE = 4097;
    protected static final Hashtable globalSymbolTable = new Hashtable();
    protected static final Hashtable lexerTables = new Hashtable();
    protected Hashtable currentLexer;
    protected String currentLexerName;
    protected Token currentMatch;

    protected void addKeyword(String name, int value) {
        Integer val = Integer.valueOf(value);
        this.currentLexer.put(name, val);
        if (!globalSymbolTable.containsKey(val)) {
            globalSymbolTable.put(val, name);
        }
    }

    public String lookupToken(int value) {
        if (value > 2048) {
            return (String) globalSymbolTable.get(Integer.valueOf(value));
        }
        return Character.valueOf((char) value).toString();
    }

    protected Hashtable addLexer(String lexerName) {
        this.currentLexer = (Hashtable) lexerTables.get(lexerName);
        if (this.currentLexer == null) {
            this.currentLexer = new Hashtable();
            lexerTables.put(lexerName, this.currentLexer);
        }
        return this.currentLexer;
    }

    public void selectLexer(String lexerName) {
        this.currentLexerName = lexerName;
    }

    protected LexerCore() {
        this.currentLexer = new Hashtable();
        this.currentLexerName = "charLexer";
    }

    public LexerCore(String lexerName, String buffer) {
        super(buffer);
        this.currentLexerName = lexerName;
    }

    public String peekNextId() {
        int oldPtr = this.ptr;
        String retval = ttoken();
        this.savedPtr = this.ptr;
        this.ptr = oldPtr;
        return retval;
    }

    public String getNextId() {
        return ttoken();
    }

    public Token getNextToken() {
        return this.currentMatch;
    }

    public Token peekNextToken() throws ParseException {
        return peekNextToken(1)[0];
    }

    public Token[] peekNextToken(int ntokens) throws ParseException {
        int old = this.ptr;
        Token[] retval = new Token[ntokens];
        for (int i = 0; i < ntokens; i++) {
            Token tok = new Token();
            if (startsId()) {
                String id = ttoken();
                tok.tokenValue = id;
                String idUppercase = id.toUpperCase();
                if (this.currentLexer.containsKey(idUppercase)) {
                    tok.tokenType = ((Integer) this.currentLexer.get(idUppercase)).intValue();
                } else {
                    tok.tokenType = 4095;
                }
            } else {
                char nextChar = getNextChar();
                tok.tokenValue = String.valueOf(nextChar);
                if (StringTokenizer.isAlpha(nextChar)) {
                    tok.tokenType = 4099;
                } else if (StringTokenizer.isDigit(nextChar)) {
                    tok.tokenType = 4098;
                } else {
                    tok.tokenType = nextChar;
                }
            }
            retval[i] = tok;
        }
        this.savedPtr = this.ptr;
        this.ptr = old;
        return retval;
    }

    public Token match(int tok) throws ParseException {
        if (Debug.parserDebug) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("match ");
            stringBuilder.append(tok);
            Debug.println(stringBuilder.toString());
        }
        StringBuilder stringBuilder2;
        String id;
        StringBuilder stringBuilder3;
        if (tok <= 2048 || tok >= 4096) {
            char next;
            if (tok > 4096) {
                next = lookAhead(0);
                StringBuilder stringBuilder4;
                if (tok == 4098) {
                    if (StringTokenizer.isDigit(next)) {
                        this.currentMatch = new Token();
                        this.currentMatch.tokenValue = String.valueOf(next);
                        this.currentMatch.tokenType = tok;
                        consume(1);
                    } else {
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append(this.buffer);
                        stringBuilder4.append("\nExpecting DIGIT");
                        throw new ParseException(stringBuilder4.toString(), this.ptr);
                    }
                } else if (tok == 4099) {
                    if (StringTokenizer.isAlpha(next)) {
                        this.currentMatch = new Token();
                        this.currentMatch.tokenValue = String.valueOf(next);
                        this.currentMatch.tokenType = tok;
                        consume(1);
                    } else {
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append(this.buffer);
                        stringBuilder4.append("\nExpecting ALPHA");
                        throw new ParseException(stringBuilder4.toString(), this.ptr);
                    }
                }
            }
            char ch = (char) tok;
            next = lookAhead(0);
            if (next == ch) {
                consume(1);
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.buffer);
                stringBuilder2.append("\nExpecting  >>>");
                stringBuilder2.append(ch);
                stringBuilder2.append("<<< got >>>");
                stringBuilder2.append(next);
                stringBuilder2.append("<<<");
                throw new ParseException(stringBuilder2.toString(), this.ptr);
            }
        } else if (tok == 4095) {
            if (startsId()) {
                id = getNextId();
                this.currentMatch = new Token();
                this.currentMatch.tokenValue = id;
                this.currentMatch.tokenType = 4095;
            } else {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(this.buffer);
                stringBuilder3.append("\nID expected");
                throw new ParseException(stringBuilder3.toString(), this.ptr);
            }
        } else if (tok != SAFE) {
            String nexttok = getNextId();
            Integer cur = (Integer) this.currentLexer.get(nexttok.toUpperCase());
            if (cur == null || cur.intValue() != tok) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(this.buffer);
                stringBuilder2.append("\nUnexpected Token : ");
                stringBuilder2.append(nexttok);
                throw new ParseException(stringBuilder2.toString(), this.ptr);
            }
            this.currentMatch = new Token();
            this.currentMatch.tokenValue = nexttok;
            this.currentMatch.tokenType = tok;
        } else if (startsSafeToken()) {
            id = ttokenSafe();
            this.currentMatch = new Token();
            this.currentMatch.tokenValue = id;
            this.currentMatch.tokenType = SAFE;
        } else {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(this.buffer);
            stringBuilder3.append("\nID expected");
            throw new ParseException(stringBuilder3.toString(), this.ptr);
        }
        return this.currentMatch;
    }

    public void SPorHT() {
        try {
            char c = lookAhead(0);
            while (true) {
                if (c != ' ') {
                    if (c != 9) {
                        return;
                    }
                }
                consume(1);
                c = lookAhead(0);
            }
        } catch (ParseException e) {
        }
    }

    public static final boolean isTokenChar(char c) {
        if (StringTokenizer.isAlphaDigit(c)) {
            return true;
        }
        switch (c) {
            case '!':
            case '%':
            case '\'':
            case '*':
            case '+':
            case '-':
            case '.':
            case '_':
            case '`':
            case '~':
                return true;
            default:
                return false;
        }
    }

    public boolean startsId() {
        try {
            return isTokenChar(lookAhead(0));
        } catch (ParseException e) {
            return false;
        }
    }

    public boolean startsSafeToken() {
        try {
            char nextChar = lookAhead(0);
            if (StringTokenizer.isAlphaDigit(nextChar)) {
                return true;
            }
            switch (nextChar) {
                case '!':
                case '\"':
                case '#':
                case '$':
                case '%':
                case '\'':
                case '*':
                case '+':
                case '-':
                case '.':
                case '/':
                case ':':
                case ';':
                case '=':
                case '?':
                case '@':
                case '[':
                case ']':
                case '^':
                case '_':
                case '`':
                case '{':
                case '|':
                case '}':
                case '~':
                    return true;
                default:
                    return false;
            }
        } catch (ParseException e) {
            return false;
        }
    }

    public String ttoken() {
        int startIdx = this.ptr;
        while (hasMoreChars() && isTokenChar(lookAhead(0))) {
            try {
                consume(1);
            } catch (ParseException e) {
                return null;
            }
        }
        return this.buffer.substring(startIdx, this.ptr);
    }

    public String ttokenSafe() {
        int startIdx = this.ptr;
        while (hasMoreChars()) {
            try {
                char nextChar = lookAhead(0);
                if (StringTokenizer.isAlphaDigit(nextChar)) {
                    consume(1);
                } else {
                    boolean isValidChar = false;
                    switch (nextChar) {
                        case '!':
                        case '\"':
                        case '#':
                        case '$':
                        case '%':
                        case '\'':
                        case '*':
                        case '+':
                        case '-':
                        case '.':
                        case '/':
                        case ':':
                        case ';':
                        case '?':
                        case '@':
                        case '[':
                        case ']':
                        case '^':
                        case '_':
                        case '`':
                        case '{':
                        case '|':
                        case '}':
                        case '~':
                            isValidChar = true;
                            break;
                        default:
                            break;
                    }
                    if (!isValidChar) {
                        return this.buffer.substring(startIdx, this.ptr);
                    }
                    consume(1);
                }
            } catch (ParseException e) {
                return null;
            }
        }
        return this.buffer.substring(startIdx, this.ptr);
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x003d A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0037 A:{Catch:{ ParseException -> 0x003c }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void consumeValidChars(char[] validChars) {
        int validCharsLength = validChars.length;
        while (hasMoreChars()) {
            try {
                char nextChar = lookAhead(0);
                boolean isValid = false;
                int i = 0;
                while (i < validCharsLength) {
                    char validChar = validChars[i];
                    switch (validChar) {
                        case 65533:
                            isValid = StringTokenizer.isAlphaDigit(nextChar);
                            break;
                        case 65534:
                            isValid = StringTokenizer.isDigit(nextChar);
                            break;
                        case 65535:
                            isValid = StringTokenizer.isAlpha(nextChar);
                            break;
                        default:
                            boolean z;
                            if (nextChar == validChar) {
                                z = true;
                            } else {
                                z = false;
                            }
                            isValid = z;
                            break;
                    }
                    if (!isValid) {
                        i++;
                    } else if (!isValid) {
                        consume(1);
                    } else {
                        return;
                    }
                }
                if (!isValid) {
                }
            } catch (ParseException e) {
                return;
            }
        }
    }

    public String quotedString() throws ParseException {
        int startIdx = this.ptr + 1;
        if (lookAhead(0) != '\"') {
            return null;
        }
        consume(1);
        while (true) {
            char next = getNextChar();
            if (next == '\"') {
                return this.buffer.substring(startIdx, this.ptr - 1);
            }
            if (next == 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.buffer);
                stringBuilder.append(" :unexpected EOL");
                throw new ParseException(stringBuilder.toString(), this.ptr);
            } else if (next == '\\') {
                consume(1);
            }
        }
    }

    public String comment() throws ParseException {
        StringBuffer retval = new StringBuffer();
        if (lookAhead(0) != '(') {
            return null;
        }
        consume(1);
        while (true) {
            char next = getNextChar();
            if (next == ')') {
                return retval.toString();
            }
            StringBuilder stringBuilder;
            if (next == 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(this.buffer);
                stringBuilder.append(" :unexpected EOL");
                throw new ParseException(stringBuilder.toString(), this.ptr);
            } else if (next == '\\') {
                retval.append(next);
                next = getNextChar();
                if (next != 0) {
                    retval.append(next);
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.buffer);
                    stringBuilder.append(" : unexpected EOL");
                    throw new ParseException(stringBuilder.toString(), this.ptr);
                }
            } else {
                retval.append(next);
            }
        }
    }

    public String byteStringNoSemicolon() {
        StringBuffer retval = new StringBuffer();
        while (true) {
            try {
                char next = lookAhead(0);
                if (next == 0 || next == 10 || next == ';') {
                    break;
                } else if (next == ',') {
                    break;
                } else {
                    consume(1);
                    retval.append(next);
                }
            } catch (ParseException e) {
                return retval.toString();
            }
        }
        return retval.toString();
    }

    public String byteStringNoSlash() {
        StringBuffer retval = new StringBuffer();
        while (true) {
            try {
                char next = lookAhead(0);
                if (next == 0 || next == 10) {
                    break;
                } else if (next == '/') {
                    break;
                } else {
                    consume(1);
                    retval.append(next);
                }
            } catch (ParseException e) {
                return retval.toString();
            }
        }
        return retval.toString();
    }

    public String byteStringNoComma() {
        StringBuffer retval = new StringBuffer();
        while (true) {
            try {
                char next = lookAhead(0);
                if (next == 10) {
                    break;
                } else if (next == ',') {
                    break;
                } else {
                    consume(1);
                    retval.append(next);
                }
            } catch (ParseException e) {
            }
        }
        return retval.toString();
    }

    public static String charAsString(char ch) {
        return String.valueOf(ch);
    }

    public String charAsString(int nchars) {
        return this.buffer.substring(this.ptr, this.ptr + nchars);
    }

    public String number() throws ParseException {
        int startIdx = this.ptr;
        try {
            if (StringTokenizer.isDigit(lookAhead(0))) {
                consume(1);
                while (StringTokenizer.isDigit(lookAhead(0))) {
                    consume(1);
                }
                return this.buffer.substring(startIdx, this.ptr);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.buffer);
            stringBuilder.append(": Unexpected token at ");
            stringBuilder.append(lookAhead(0));
            throw new ParseException(stringBuilder.toString(), this.ptr);
        } catch (ParseException e) {
            return this.buffer.substring(startIdx, this.ptr);
        }
    }

    public int markInputPosition() {
        return this.ptr;
    }

    public void rewindInputPosition(int position) {
        this.ptr = position;
    }

    public String getRest() {
        if (this.ptr >= this.buffer.length()) {
            return null;
        }
        return this.buffer.substring(this.ptr);
    }

    public String getString(char c) throws ParseException {
        StringBuffer retval = new StringBuffer();
        while (true) {
            char next = lookAhead(0);
            if (next == 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.buffer);
                stringBuilder.append("unexpected EOL");
                throw new ParseException(stringBuilder.toString(), this.ptr);
            } else if (next == c) {
                consume(1);
                return retval.toString();
            } else if (next == '\\') {
                consume(1);
                char nextchar = lookAhead(0);
                if (nextchar != 0) {
                    consume(1);
                    retval.append(nextchar);
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(this.buffer);
                    stringBuilder2.append("unexpected EOL");
                    throw new ParseException(stringBuilder2.toString(), this.ptr);
                }
            } else {
                consume(1);
                retval.append(next);
            }
        }
    }

    public int getPtr() {
        return this.ptr;
    }

    public String getBuffer() {
        return this.buffer;
    }

    public ParseException createParseException() {
        return new ParseException(this.buffer, this.ptr);
    }
}
