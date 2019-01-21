package gov.nist.javax.sip.parser;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.UserAgent;
import java.io.PrintStream;
import java.text.ParseException;

public class UserAgentParser extends HeaderParser {
    public UserAgentParser(String userAgent) {
        super(userAgent);
    }

    protected UserAgentParser(Lexer lexer) {
        super(lexer);
    }

    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("UserAgentParser.parse");
        }
        UserAgent userAgent = new UserAgent();
        try {
            headerName(TokenTypes.USER_AGENT);
            if (this.lexer.lookAhead(0) != 10) {
                while (this.lexer.lookAhead(0) != 10 && this.lexer.lookAhead(0) != 0) {
                    String comment;
                    if (this.lexer.lookAhead(0) == '(') {
                        comment = this.lexer.comment();
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append('(');
                        stringBuilder.append(comment);
                        stringBuilder.append(')');
                        userAgent.addProductToken(stringBuilder.toString());
                    } else {
                        getLexer().SPorHT();
                        comment = this.lexer.byteStringNoSlash();
                        if (comment != null) {
                            StringBuffer productSb = new StringBuffer(comment);
                            if (this.lexer.peekNextToken().getTokenType() == 47) {
                                this.lexer.match(47);
                                getLexer().SPorHT();
                                String productVersion = this.lexer.byteStringNoSlash();
                                if (productVersion != null) {
                                    productSb.append(Separators.SLASH);
                                    productSb.append(productVersion);
                                } else {
                                    throw createParseException("Expected product version");
                                }
                            }
                            userAgent.addProductToken(productSb.toString());
                        } else {
                            throw createParseException("Expected product string");
                        }
                    }
                    this.lexer.SPorHT();
                }
                if (debug) {
                    dbg_leave("UserAgentParser.parse");
                }
                return userAgent;
            }
            throw createParseException("empty header");
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("UserAgentParser.parse");
            }
        }
    }

    public static void main(String[] args) throws ParseException {
        String[] userAgent = new String[]{"User-Agent: Softphone/Beta1.5 \n", "User-Agent:Nist/Beta1 (beta version) \n", "User-Agent: Nist UA (beta version)\n", "User-Agent: Nist1.0/Beta2 Ubi/vers.1.0 (very cool) \n", "User-Agent: SJphone/1.60.299a/L (SJ Labs)\n", "User-Agent: sipXecs/3.5.11 sipXecs/sipxbridge (Linux)\n"};
        for (String userAgentParser : userAgent) {
            UserAgent ua = (UserAgent) new UserAgentParser(userAgentParser).parse();
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("encoded = ");
            stringBuilder.append(ua.encode());
            printStream.println(stringBuilder.toString());
        }
    }
}
