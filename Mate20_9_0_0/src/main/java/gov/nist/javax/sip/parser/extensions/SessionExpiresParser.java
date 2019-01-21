package gov.nist.javax.sip.parser.extensions;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.extensions.SessionExpires;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;
import java.io.PrintStream;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;

public class SessionExpiresParser extends ParametersParser {
    public SessionExpiresParser(String text) {
        super(text);
    }

    protected SessionExpiresParser(Lexer lexer) {
        super(lexer);
    }

    public SIPHeader parse() throws ParseException {
        SessionExpires se = new SessionExpires();
        if (debug) {
            dbg_enter("parse");
        }
        try {
            headerName(TokenTypes.SESSIONEXPIRES_TO);
            se.setExpires(Integer.parseInt(this.lexer.getNextId()));
            this.lexer.SPorHT();
            super.parse(se);
            if (debug) {
                dbg_leave("parse");
            }
            return se;
        } catch (NumberFormatException e) {
            throw createParseException("bad integer format");
        } catch (InvalidArgumentException ex) {
            throw createParseException(ex.getMessage());
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("parse");
            }
        }
    }

    public static void main(String[] args) throws ParseException {
        String[] to = new String[]{"Session-Expires: 30\n", "Session-Expires: 45;refresher=uac\n"};
        for (String sessionExpiresParser : to) {
            SessionExpires t = (SessionExpires) new SessionExpiresParser(sessionExpiresParser).parse();
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("encoded = ");
            stringBuilder.append(t.encode());
            printStream.println(stringBuilder.toString());
            printStream = System.out;
            stringBuilder = new StringBuilder();
            stringBuilder.append("\ntime=");
            stringBuilder.append(t.getExpires());
            printStream.println(stringBuilder.toString());
            if (t.getParameter(SessionExpires.REFRESHER) != null) {
                printStream = System.out;
                stringBuilder = new StringBuilder();
                stringBuilder.append("refresher=");
                stringBuilder.append(t.getParameter(SessionExpires.REFRESHER));
                printStream.println(stringBuilder.toString());
            }
        }
    }
}
