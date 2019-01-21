package gov.nist.core;

import java.io.PrintStream;
import java.text.ParseException;
import javax.sip.header.WarningHeader;

public class HostNameParser extends ParserCore {
    private static LexerCore Lexer;
    private static final char[] VALID_DOMAIN_LABEL_CHAR = new char[]{65533, '-', '.'};
    private boolean stripAddressScopeZones = false;

    public HostNameParser(String hname) {
        this.lexer = new LexerCore("charLexer", hname);
        this.stripAddressScopeZones = Boolean.getBoolean("gov.nist.core.STRIP_ADDR_SCOPES");
    }

    public HostNameParser(LexerCore lexer) {
        this.lexer = lexer;
        lexer.selectLexer("charLexer");
        this.stripAddressScopeZones = Boolean.getBoolean("gov.nist.core.STRIP_ADDR_SCOPES");
    }

    protected void consumeDomainLabel() throws ParseException {
        if (debug) {
            dbg_enter("domainLabel");
        }
        try {
            this.lexer.consumeValidChars(VALID_DOMAIN_LABEL_CHAR);
        } finally {
            if (debug) {
                dbg_leave("domainLabel");
            }
        }
    }

    protected String ipv6Reference() throws ParseException {
        StringBuffer retval = new StringBuffer();
        if (debug) {
            dbg_enter("ipv6Reference");
        }
        String stringBuffer;
        try {
            char la;
            if (this.stripAddressScopeZones) {
                while (this.lexer.hasMoreChars()) {
                    la = this.lexer.lookAhead(0);
                    if (!(StringTokenizer.isHexDigit(la) || la == '.' || la == ':')) {
                        if (la != '[') {
                            if (la == ']') {
                                this.lexer.consume(1);
                                retval.append(la);
                                stringBuffer = retval.toString();
                                return stringBuffer;
                            } else if (la == '%') {
                                this.lexer.consume(1);
                                stringBuffer = this.lexer.getRest();
                                if (!(stringBuffer == null || stringBuffer.length() == 0)) {
                                    int stripLen = stringBuffer.indexOf(93);
                                    if (stripLen != -1) {
                                        this.lexer.consume(stripLen + 1);
                                        retval.append("]");
                                        String stringBuffer2 = retval.toString();
                                        if (debug) {
                                            dbg_leave("ipv6Reference");
                                        }
                                        return stringBuffer2;
                                    }
                                }
                            }
                        }
                    }
                    this.lexer.consume(1);
                    retval.append(la);
                }
            } else {
                while (this.lexer.hasMoreChars()) {
                    la = this.lexer.lookAhead(0);
                    if (!(StringTokenizer.isHexDigit(la) || la == '.' || la == ':')) {
                        if (la != '[') {
                            if (la == ']') {
                                this.lexer.consume(1);
                                retval.append(la);
                                stringBuffer = retval.toString();
                                if (debug) {
                                    dbg_leave("ipv6Reference");
                                }
                                return stringBuffer;
                            }
                        }
                    }
                    this.lexer.consume(1);
                    retval.append(la);
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.lexer.getBuffer());
            stringBuilder.append(": Illegal Host name ");
            throw new ParseException(stringBuilder.toString(), this.lexer.getPtr());
        } finally {
            stringBuffer = debug;
            if (stringBuffer != null) {
                stringBuffer = "ipv6Reference";
                dbg_leave(stringBuffer);
            }
        }
    }

    public Host host() throws ParseException {
        if (debug) {
            dbg_enter("host");
        }
        Host host;
        try {
            String hostname;
            int startPtr;
            if (this.lexer.lookAhead(0) == '[') {
                hostname = ipv6Reference();
            } else if (isIPv6Address(this.lexer.getRest())) {
                startPtr = this.lexer.getPtr();
                this.lexer.consumeValidChars(new char[]{65533, ':'});
                StringBuffer stringBuffer = new StringBuffer("[");
                stringBuffer.append(this.lexer.getBuffer().substring(startPtr, this.lexer.getPtr()));
                stringBuffer.append("]");
                hostname = stringBuffer.toString();
            } else {
                startPtr = this.lexer.getPtr();
                consumeDomainLabel();
                hostname = this.lexer.getBuffer().substring(startPtr, this.lexer.getPtr());
            }
            if (hostname.length() != 0) {
                host = new Host(hostname);
                return host;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.lexer.getBuffer());
            stringBuilder.append(": Missing host name");
            throw new ParseException(stringBuilder.toString(), this.lexer.getPtr());
        } finally {
            host = debug;
            if (host != null) {
                host = "host";
                dbg_leave(host);
            }
        }
    }

    private boolean isIPv6Address(String uriHeader) {
        LexerCore lexerCore = Lexer;
        int hostEnd = uriHeader.indexOf(63);
        LexerCore lexerCore2 = Lexer;
        int semiColonIndex = uriHeader.indexOf(59);
        if (hostEnd == -1 || (semiColonIndex != -1 && hostEnd > semiColonIndex)) {
            hostEnd = semiColonIndex;
        }
        if (hostEnd == -1) {
            hostEnd = uriHeader.length();
        }
        String host = uriHeader.substring(0, hostEnd);
        LexerCore lexerCore3 = Lexer;
        int firstColonIndex = host.indexOf(58);
        if (firstColonIndex == -1) {
            return false;
        }
        LexerCore lexerCore4 = Lexer;
        if (host.indexOf(58, firstColonIndex + 1) == -1) {
            return false;
        }
        return true;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HostPort hostPort(boolean allowWS) throws ParseException {
        if (debug) {
            dbg_enter("hostPort");
        }
        StringBuilder stringBuilder;
        try {
            Host host = host();
            HostPort hp = new HostPort();
            hp.setHost(host);
            if (allowWS) {
                this.lexer.SPorHT();
            }
            if (this.lexer.hasMoreChars()) {
                switch (this.lexer.lookAhead(0)) {
                    case 9:
                    case WarningHeader.ATTRIBUTE_NOT_UNDERSTOOD /*10*/:
                    case 13:
                    case ' ':
                    case ',':
                    case '/':
                    case ';':
                    case '>':
                    case '?':
                        break;
                    case '%':
                        if (this.stripAddressScopeZones) {
                            break;
                        }
                    case ':':
                        this.lexer.consume(1);
                        if (allowWS) {
                            this.lexer.SPorHT();
                        }
                        hp.setPort(Integer.parseInt(this.lexer.number()));
                        break;
                    default:
                }
                if (!allowWS) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.lexer.getBuffer());
                    stringBuilder.append(" Illegal character in hostname:");
                    stringBuilder.append(this.lexer.lookAhead(0));
                    throw new ParseException(stringBuilder.toString(), this.lexer.getPtr());
                }
            }
            if (debug) {
                dbg_leave("hostPort");
            }
            return hp;
        } catch (NumberFormatException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.lexer.getBuffer());
            stringBuilder.append(" :Error parsing port ");
            throw new ParseException(stringBuilder.toString(), this.lexer.getPtr());
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("hostPort");
            }
        }
    }

    public static void main(String[] args) throws ParseException {
        String[] hostNames = new String[]{"foo.bar.com:1234", "proxima.chaplin.bt.co.uk", "129.6.55.181:2345", ":1234", "foo.bar.com:         1234", "foo.bar.com     :      1234   ", "MIK_S:1234"};
        for (String hostNameParser : hostNames) {
            try {
                HostPort hp = new HostNameParser(hostNameParser).hostPort(true);
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[");
                stringBuilder.append(hp.encode());
                stringBuilder.append("]");
                printStream.println(stringBuilder.toString());
            } catch (ParseException ex) {
                PrintStream printStream2 = System.out;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("exception text = ");
                stringBuilder2.append(ex.getMessage());
                printStream2.println(stringBuilder2.toString());
            }
        }
    }
}
