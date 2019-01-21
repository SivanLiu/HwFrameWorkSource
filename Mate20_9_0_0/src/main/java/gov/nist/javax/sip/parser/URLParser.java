package gov.nist.javax.sip.parser;

import gov.nist.core.HostNameParser;
import gov.nist.core.LexerCore;
import gov.nist.core.NameValue;
import gov.nist.core.NameValueList;
import gov.nist.core.Separators;
import gov.nist.core.StringTokenizer;
import gov.nist.core.Token;
import gov.nist.javax.sip.address.GenericURI;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.address.TelURLImpl;
import gov.nist.javax.sip.address.TelephoneNumber;
import java.io.PrintStream;
import java.text.ParseException;

public class URLParser extends Parser {
    public URLParser(String url) {
        this.lexer = new Lexer("sip_urlLexer", url);
    }

    public URLParser(Lexer lexer) {
        this.lexer = lexer;
        this.lexer.selectLexer("sip_urlLexer");
    }

    protected static boolean isMark(char next) {
        if (!(next == '!' || next == '_' || next == '~')) {
            switch (next) {
                case '\'':
                case '(':
                case ')':
                case '*':
                    break;
                default:
                    switch (next) {
                        case '-':
                        case '.':
                            break;
                        default:
                            return false;
                    }
            }
        }
        return true;
    }

    protected static boolean isUnreserved(char next) {
        return StringTokenizer.isAlphaDigit(next) || isMark(next);
    }

    protected static boolean isReservedNoSlash(char next) {
        switch (next) {
            case '$':
            case '&':
            case '+':
            case ',':
            case ':':
            case ';':
            case '?':
            case '@':
                return true;
            default:
                return false;
        }
    }

    protected static boolean isUserUnreserved(char la) {
        switch (la) {
            case '#':
            case '$':
            case '&':
            case '+':
            case ',':
            case '/':
            case ';':
            case '=':
            case '?':
                return true;
            default:
                return false;
        }
    }

    protected String unreserved() throws ParseException {
        char next = this.lexer.lookAhead(0);
        if (isUnreserved(next)) {
            this.lexer.consume(1);
            return String.valueOf(next);
        }
        throw createParseException("unreserved");
    }

    protected String paramNameOrValue() throws ParseException {
        int startIdx = this.lexer.getPtr();
        while (this.lexer.hasMoreChars()) {
            char next = this.lexer.lookAhead(0);
            boolean isValidChar = false;
            if (next == '$' || next == '&' || next == '+' || next == '/' || next == ':' || next == '[' || next == ']') {
                isValidChar = true;
            }
            if (!isValidChar && !isUnreserved(next)) {
                if (!isEscaped()) {
                    break;
                }
                this.lexer.consume(3);
            } else {
                this.lexer.consume(1);
            }
        }
        return this.lexer.getBuffer().substring(startIdx, this.lexer.getPtr());
    }

    private NameValue uriParam() throws ParseException {
        if (debug) {
            dbg_enter("uriParam");
        }
        try {
            String pvalue = "";
            String pname = paramNameOrValue();
            boolean isFlagParam = true;
            if (this.lexer.lookAhead(0) == '=') {
                this.lexer.consume(1);
                pvalue = paramNameOrValue();
                isFlagParam = false;
            }
            if (pname.length() == 0 && (pvalue == null || pvalue.length() == 0)) {
                if (debug) {
                    dbg_leave("uriParam");
                }
                return null;
            }
            NameValue nameValue = new NameValue(pname, pvalue, isFlagParam);
            if (debug) {
                dbg_leave("uriParam");
            }
            return nameValue;
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("uriParam");
            }
        }
    }

    protected static boolean isReserved(char next) {
        switch (next) {
            case '$':
            case '&':
            case '+':
            case ',':
            case '/':
            case ':':
            case ';':
            case '=':
            case '?':
            case '@':
                return true;
            default:
                return false;
        }
    }

    protected String reserved() throws ParseException {
        char next = this.lexer.lookAhead(0);
        if (isReserved(next)) {
            this.lexer.consume(1);
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(next);
            return stringBuffer.toString();
        }
        throw createParseException("reserved");
    }

    protected boolean isEscaped() {
        boolean z = false;
        try {
            if (this.lexer.lookAhead(0) == '%' && StringTokenizer.isHexDigit(this.lexer.lookAhead(1)) && StringTokenizer.isHexDigit(this.lexer.lookAhead(2))) {
                z = true;
            }
            return z;
        } catch (Exception e) {
            return false;
        }
    }

    protected String escaped() throws ParseException {
        if (debug) {
            dbg_enter("escaped");
        }
        try {
            StringBuffer retval = new StringBuffer();
            char next = this.lexer.lookAhead(0);
            char next1 = this.lexer.lookAhead(1);
            char next2 = this.lexer.lookAhead(2);
            String str = 37;
            if (next == '%') {
                str = StringTokenizer.isHexDigit(next1);
                if (str == true) {
                    str = StringTokenizer.isHexDigit(next2);
                    if (str == true) {
                        this.lexer.consume(3);
                        retval.append(next);
                        retval.append(next1);
                        retval.append(next2);
                        str = retval.toString();
                        return str;
                    }
                }
            }
            throw createParseException("escaped");
        } finally {
            if (debug) {
                dbg_leave("escaped");
            }
        }
    }

    protected String mark() throws ParseException {
        if (debug) {
            dbg_enter("mark");
        }
        try {
            String isMark = isMark(this.lexer.lookAhead(0));
            if (isMark != null) {
                this.lexer.consume(1);
                isMark = new String(new char[]{next});
                return isMark;
            }
            throw createParseException("mark");
        } finally {
            if (debug) {
                dbg_leave("mark");
            }
        }
    }

    protected String uric() {
        if (debug) {
            dbg_enter("uric");
        }
        try {
            char la = this.lexer.lookAhead(0);
            String charAsString;
            if (isUnreserved(la)) {
                this.lexer.consume(1);
                charAsString = LexerCore.charAsString(la);
                if (debug) {
                    dbg_leave("uric");
                }
                return charAsString;
            } else if (isReserved(la)) {
                this.lexer.consume(1);
                charAsString = LexerCore.charAsString(la);
                if (debug) {
                    dbg_leave("uric");
                }
                return charAsString;
            } else if (isEscaped()) {
                charAsString = this.lexer.charAsString(3);
                this.lexer.consume(3);
                if (debug) {
                    dbg_leave("uric");
                }
                return charAsString;
            } else {
                if (debug) {
                    dbg_leave("uric");
                }
                return null;
            }
        } catch (Exception e) {
            if (debug) {
                dbg_leave("uric");
            }
            return null;
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("uric");
            }
            throw th;
        }
    }

    protected String uricNoSlash() {
        if (debug) {
            dbg_enter("uricNoSlash");
        }
        try {
            char la = this.lexer.lookAhead(0);
            String retval;
            if (isEscaped()) {
                retval = this.lexer.charAsString(3);
                this.lexer.consume(3);
                if (debug) {
                    dbg_leave("uricNoSlash");
                }
                return retval;
            } else if (isUnreserved(la)) {
                this.lexer.consume(1);
                retval = LexerCore.charAsString(la);
                if (debug) {
                    dbg_leave("uricNoSlash");
                }
                return retval;
            } else if (isReservedNoSlash(la)) {
                this.lexer.consume(1);
                retval = LexerCore.charAsString(la);
                if (debug) {
                    dbg_leave("uricNoSlash");
                }
                return retval;
            } else {
                if (debug) {
                    dbg_leave("uricNoSlash");
                }
                return null;
            }
        } catch (ParseException e) {
            if (debug) {
                dbg_leave("uricNoSlash");
            }
            return null;
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("uricNoSlash");
            }
            throw th;
        }
    }

    protected String uricString() throws ParseException {
        StringBuffer retval = new StringBuffer();
        while (true) {
            String next = uric();
            if (next != null) {
                retval.append(next);
            } else if (this.lexer.lookAhead(0) != '[') {
                return retval.toString();
            } else {
                retval.append(new HostNameParser(getLexer()).hostPort(false).toString());
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x0069  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public GenericURI uriReference(boolean inBrackets) throws ParseException {
        if (debug) {
            dbg_enter("uriReference");
        }
        Token[] tokens = this.lexer.peekNextToken(2);
        Token t1 = tokens[null];
        Token t2 = tokens[1];
        try {
            GenericURI retval;
            if (t1.getTokenType() != TokenTypes.SIP) {
                if (t1.getTokenType() != TokenTypes.SIPS) {
                    if (t1.getTokenType() != TokenTypes.TEL) {
                        retval = new GenericURI(uricString());
                    } else if (t2.getTokenType() == 58) {
                        retval = telURL(inBrackets);
                    } else {
                        throw createParseException("Expecting ':'");
                    }
                    if (debug) {
                        dbg_leave("uriReference");
                    }
                    return retval;
                }
            }
            if (t2.getTokenType() == 58) {
                retval = sipURL(inBrackets);
                if (debug) {
                }
                return retval;
            }
            throw createParseException("Expecting ':'");
        } catch (ParseException ex) {
            throw createParseException(ex.getMessage());
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("uriReference");
            }
        }
    }

    private String base_phone_number() throws ParseException {
        String stringBuffer;
        StringBuffer s = new StringBuffer();
        if (debug) {
            dbg_enter("base_phone_number");
        }
        int lc = 0;
        while (this.lexer.hasMoreChars()) {
            try {
                char w = this.lexer.lookAhead(0);
                if (!(StringTokenizer.isDigit(w) || w == '-' || w == '.' || w == '(')) {
                    if (w != ')') {
                        if (lc > 0) {
                            stringBuffer = s.toString();
                            return stringBuffer;
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unexpected ");
                        stringBuilder.append(w);
                        throw createParseException(stringBuilder.toString());
                    }
                }
                this.lexer.consume(1);
                s.append(w);
                lc++;
            } finally {
                if (debug) {
                    dbg_leave("base_phone_number");
                }
            }
        }
        stringBuffer = s.toString();
        return stringBuffer;
    }

    private String local_number() throws ParseException {
        String stringBuffer;
        StringBuffer s = new StringBuffer();
        if (debug) {
            dbg_enter("local_number");
        }
        int lc = 0;
        while (this.lexer.hasMoreChars()) {
            try {
                char la = this.lexer.lookAhead(0);
                if (!(la == '*' || la == '#' || la == '-' || la == '.' || la == '(' || la == ')')) {
                    if (!StringTokenizer.isHexDigit(la)) {
                        if (lc > 0) {
                            stringBuffer = s.toString();
                            return stringBuffer;
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unexepcted ");
                        stringBuilder.append(la);
                        throw createParseException(stringBuilder.toString());
                    }
                }
                this.lexer.consume(1);
                s.append(la);
                lc++;
            } finally {
                if (debug) {
                    dbg_leave("local_number");
                }
            }
        }
        stringBuffer = s.toString();
        return stringBuffer;
    }

    public final TelephoneNumber parseTelephoneNumber(boolean inBrackets) throws ParseException {
        if (debug) {
            dbg_enter("telephone_subscriber");
        }
        this.lexer.selectLexer("charLexer");
        TelephoneNumber tn;
        try {
            char c = this.lexer.lookAhead(0);
            if (c == '+') {
                tn = global_phone_number(inBrackets);
            } else {
                if (!(StringTokenizer.isHexDigit(c) || c == '#' || c == '*' || c == '-' || c == '.' || c == '(')) {
                    if (c != ')') {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("unexpected char ");
                        stringBuilder.append(c);
                        throw createParseException(stringBuilder.toString());
                    }
                }
                tn = local_phone_number(inBrackets);
            }
            return tn;
        } finally {
            tn = debug;
            if (tn != null) {
                tn = "telephone_subscriber";
                dbg_leave(tn);
            }
        }
    }

    private final TelephoneNumber global_phone_number(boolean inBrackets) throws ParseException {
        if (debug) {
            dbg_enter("global_phone_number");
        }
        try {
            TelephoneNumber tn = new TelephoneNumber();
            tn.setGlobal(true);
            this.lexer.match(43);
            tn.setPhoneNumber(base_phone_number());
            if (this.lexer.hasMoreChars() && this.lexer.lookAhead(0) == ';' && inBrackets) {
                this.lexer.consume(1);
                tn.setParameters(tel_parameters());
            }
            if (debug) {
                dbg_leave("global_phone_number");
            }
            return tn;
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("global_phone_number");
            }
        }
    }

    private TelephoneNumber local_phone_number(boolean inBrackets) throws ParseException {
        if (debug) {
            dbg_enter("local_phone_number");
        }
        TelephoneNumber tn = new TelephoneNumber();
        tn.setGlobal(false);
        try {
            tn.setPhoneNumber(local_number());
            if (this.lexer.hasMoreChars()) {
                if (this.lexer.peekNextToken().getTokenType() == 59) {
                    if (inBrackets) {
                        this.lexer.consume(1);
                        tn.setParameters(tel_parameters());
                    }
                }
            }
            if (debug) {
                dbg_leave("local_phone_number");
            }
            return tn;
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("local_phone_number");
            }
        }
    }

    private NameValueList tel_parameters() throws ParseException {
        NameValueList nvList = new NameValueList();
        while (true) {
            NameValue nv;
            String pname = paramNameOrValue();
            if (pname.equalsIgnoreCase("phone-context")) {
                nv = phone_context();
            } else if (this.lexer.lookAhead(0) == '=') {
                this.lexer.consume(1);
                nv = new NameValue(pname, paramNameOrValue(), false);
            } else {
                nv = new NameValue(pname, "", true);
            }
            nvList.set(nv);
            if (this.lexer.lookAhead(0) != ';') {
                return nvList;
            }
            this.lexer.consume(1);
        }
    }

    private NameValue phone_context() throws ParseException {
        Object value;
        this.lexer.match(61);
        char la = this.lexer.lookAhead(0);
        if (la == '+') {
            this.lexer.consume(1);
            value = new StringBuilder();
            value.append("+");
            value.append(base_phone_number());
            value = value.toString();
        } else if (StringTokenizer.isAlphaDigit(la)) {
            value = this.lexer.match(4095).getTokenValue();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid phone-context:");
            stringBuilder.append(la);
            throw new ParseException(stringBuilder.toString(), -1);
        }
        return new NameValue("phone-context", value, false);
    }

    public TelURLImpl telURL(boolean inBrackets) throws ParseException {
        this.lexer.match(TokenTypes.TEL);
        this.lexer.match(58);
        TelephoneNumber tn = parseTelephoneNumber(inBrackets);
        TelURLImpl telUrl = new TelURLImpl();
        telUrl.setTelephoneNumber(tn);
        return telUrl;
    }

    public SipUri sipURL(boolean inBrackets) throws ParseException {
        if (debug) {
            dbg_enter("sipURL");
        }
        SipUri retval = new SipUri();
        Token nextToken = this.lexer.peekNextToken();
        int sipOrSips = TokenTypes.SIP;
        String scheme = "sip";
        if (nextToken.getTokenType() == TokenTypes.SIPS) {
            sipOrSips = TokenTypes.SIPS;
            scheme = "sips";
        }
        try {
            this.lexer.match(sipOrSips);
            this.lexer.match(58);
            retval.setScheme(scheme);
            int startOfUser = this.lexer.markInputPosition();
            String userOrHost = user();
            String passOrPort = null;
            if (this.lexer.lookAhead() == ':') {
                this.lexer.consume(1);
                passOrPort = password();
            }
            if (this.lexer.lookAhead() == '@') {
                this.lexer.consume(1);
                retval.setUser(userOrHost);
                if (passOrPort != null) {
                    retval.setUserPassword(passOrPort);
                }
            } else {
                this.lexer.rewindInputPosition(startOfUser);
            }
            retval.setHostPort(new HostNameParser(getLexer()).hostPort(false));
            this.lexer.selectLexer("charLexer");
            while (this.lexer.hasMoreChars() && this.lexer.lookAhead(0) == ';') {
                if (!inBrackets) {
                    break;
                }
                this.lexer.consume(1);
                NameValue parms = uriParam();
                if (parms != null) {
                    retval.setUriParameter(parms);
                }
            }
            if (this.lexer.hasMoreChars() && this.lexer.lookAhead(0) == '?') {
                this.lexer.consume(1);
                while (this.lexer.hasMoreChars()) {
                    retval.setQHeader(qheader());
                    if (this.lexer.hasMoreChars() && this.lexer.lookAhead(0) != '&') {
                        break;
                    }
                    this.lexer.consume(1);
                }
            }
            if (debug) {
                dbg_leave("sipURL");
            }
            return retval;
        } catch (RuntimeException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid URL: ");
            stringBuilder.append(this.lexer.getBuffer());
            throw new ParseException(stringBuilder.toString(), -1);
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("sipURL");
            }
        }
    }

    public String peekScheme() throws ParseException {
        Token[] tokens = this.lexer.peekNextToken(1);
        if (tokens.length == 0) {
            return null;
        }
        return tokens[0].getTokenValue();
    }

    protected NameValue qheader() throws ParseException {
        String name = this.lexer.getNextToken('=');
        this.lexer.consume(1);
        return new NameValue(name, hvalue(), false);
    }

    protected String hvalue() throws ParseException {
        StringBuffer retval = new StringBuffer();
        while (this.lexer.hasMoreChars()) {
            char la = this.lexer.lookAhead(0);
            boolean isValidChar = false;
            if (!(la == '$' || la == ':' || la == '?' || la == '[' || la == ']' || la == '_' || la == '~')) {
                switch (la) {
                    case '!':
                    case '\"':
                        break;
                    default:
                        switch (la) {
                            case '(':
                            case ')':
                            case '*':
                            case '+':
                                break;
                            default:
                                switch (la) {
                                    case '-':
                                    case '.':
                                    case '/':
                                        break;
                                }
                                break;
                        }
                }
            }
            isValidChar = true;
            if (isValidChar || StringTokenizer.isAlphaDigit(la)) {
                this.lexer.consume(1);
                retval.append(la);
            } else if (la != '%') {
                return retval.toString();
            } else {
                retval.append(escaped());
            }
        }
        return retval.toString();
    }

    protected String urlString() throws ParseException {
        StringBuffer retval = new StringBuffer();
        this.lexer.selectLexer("charLexer");
        while (this.lexer.hasMoreChars()) {
            char la = this.lexer.lookAhead(0);
            if (la == ' ' || la == 9 || la == 10 || la == '>' || la == '<') {
                break;
            }
            this.lexer.consume(0);
            retval.append(la);
        }
        return retval.toString();
    }

    protected String user() throws ParseException {
        if (debug) {
            dbg_enter("user");
        }
        String substring;
        try {
            int startIdx = this.lexer.getPtr();
            while (this.lexer.hasMoreChars()) {
                char la = this.lexer.lookAhead(0);
                if (!isUnreserved(la)) {
                    if (!isUserUnreserved(la)) {
                        if (!isEscaped()) {
                            break;
                        }
                        this.lexer.consume(3);
                    }
                }
                this.lexer.consume(1);
            }
            substring = this.lexer.getBuffer().substring(startIdx, this.lexer.getPtr());
            return substring;
        } finally {
            substring = debug;
            if (substring != null) {
                substring = "user";
                dbg_leave(substring);
            }
        }
    }

    protected String password() throws ParseException {
        int startIdx = this.lexer.getPtr();
        while (true) {
            char la = this.lexer.lookAhead(0);
            boolean isValidChar = false;
            if (!(la == '$' || la == '&' || la == '=')) {
                switch (la) {
                    case '+':
                    case ',':
                        break;
                }
            }
            isValidChar = true;
            if (isValidChar || isUnreserved(la)) {
                this.lexer.consume(1);
            } else if (!isEscaped()) {
                return this.lexer.getBuffer().substring(startIdx, this.lexer.getPtr());
            } else {
                this.lexer.consume(3);
            }
        }
    }

    public GenericURI parse() throws ParseException {
        return uriReference(true);
    }

    public static void main(String[] args) throws ParseException {
        String[] test = new String[]{"sip:alice@example.com", "sips:alice@examples.com", "sip:3Zqkv5dajqaaas0tCjCxT0xH2ZEuEMsFl0xoasip%3A%2B3519116786244%40siplab.domain.com@213.0.115.163:7070"};
        for (int i = 0; i < test.length; i++) {
            GenericURI uri = new URLParser(test[i]).parse();
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("uri type returned ");
            stringBuilder.append(uri.getClass().getName());
            printStream.println(stringBuilder.toString());
            printStream = System.out;
            stringBuilder = new StringBuilder();
            stringBuilder.append(test[i]);
            stringBuilder.append(" is SipUri? ");
            stringBuilder.append(uri.isSipURI());
            stringBuilder.append(Separators.GREATER_THAN);
            stringBuilder.append(uri.encode());
            printStream.println(stringBuilder.toString());
        }
    }
}
