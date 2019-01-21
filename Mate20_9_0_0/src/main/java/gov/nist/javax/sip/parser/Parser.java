package gov.nist.javax.sip.parser;

import gov.nist.core.Debug;
import gov.nist.core.LexerCore;
import gov.nist.core.ParserCore;
import gov.nist.core.Separators;
import gov.nist.core.Token;
import gov.nist.javax.sip.SIPConstants;
import java.text.ParseException;
import org.ccil.cowan.tagsoup.XMLWriter;

public abstract class Parser extends ParserCore implements TokenTypes {
    protected ParseException createParseException(String exceptionString) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.lexer.getBuffer());
        stringBuilder.append(Separators.COLON);
        stringBuilder.append(exceptionString);
        return new ParseException(stringBuilder.toString(), this.lexer.getPtr());
    }

    protected Lexer getLexer() {
        return (Lexer) this.lexer;
    }

    protected String sipVersion() throws ParseException {
        if (debug) {
            dbg_enter("sipVersion");
        }
        String str;
        try {
            if (!this.lexer.match(TokenTypes.SIP).getTokenValue().equalsIgnoreCase("SIP")) {
                createParseException("Expecting SIP");
            }
            this.lexer.match(47);
            if (!this.lexer.match(4095).getTokenValue().equals("2.0")) {
                createParseException("Expecting SIP/2.0");
            }
            str = SIPConstants.SIP_VERSION_STRING;
            return str;
        } finally {
            str = debug;
            if (str != null) {
                str = "sipVersion";
                dbg_leave(str);
            }
        }
    }

    protected String method() throws ParseException {
        try {
            if (debug) {
                dbg_enter(XMLWriter.METHOD);
            }
            Token token = this.lexer.peekNextToken(1)[null];
            if (!(token.getTokenType() == TokenTypes.INVITE || token.getTokenType() == TokenTypes.ACK || token.getTokenType() == TokenTypes.OPTIONS || token.getTokenType() == TokenTypes.BYE || token.getTokenType() == TokenTypes.REGISTER || token.getTokenType() == TokenTypes.CANCEL || token.getTokenType() == TokenTypes.SUBSCRIBE || token.getTokenType() == TokenTypes.NOTIFY || token.getTokenType() == TokenTypes.PUBLISH || token.getTokenType() == TokenTypes.MESSAGE)) {
                if (token.getTokenType() != 4095) {
                    throw createParseException("Invalid Method");
                }
            }
            this.lexer.consume();
            String tokenValue = token.getTokenValue();
            return tokenValue;
        } finally {
            if (Debug.debug) {
                dbg_leave(XMLWriter.METHOD);
            }
        }
    }

    public static final void checkToken(String token) throws ParseException {
        if (token == null || token.length() == 0) {
            throw new ParseException("null or empty token", -1);
        }
        int i = 0;
        while (i < token.length()) {
            if (LexerCore.isTokenChar(token.charAt(i))) {
                i++;
            } else {
                throw new ParseException("Invalid character(s) in string (not allowed in 'token')", i);
            }
        }
    }
}
