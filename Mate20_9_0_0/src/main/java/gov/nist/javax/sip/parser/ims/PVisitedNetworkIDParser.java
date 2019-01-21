package gov.nist.javax.sip.parser.ims;

import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PVisitedNetworkID;
import gov.nist.javax.sip.header.ims.PVisitedNetworkIDList;
import gov.nist.javax.sip.parser.Lexer;
import gov.nist.javax.sip.parser.ParametersParser;
import gov.nist.javax.sip.parser.TokenTypes;
import java.text.ParseException;

public class PVisitedNetworkIDParser extends ParametersParser implements TokenTypes {
    public PVisitedNetworkIDParser(String networkID) {
        super(networkID);
    }

    protected PVisitedNetworkIDParser(Lexer lexer) {
        super(lexer);
    }

    public SIPHeader parse() throws ParseException {
        PVisitedNetworkIDList visitedNetworkIDList = new PVisitedNetworkIDList();
        if (debug) {
            dbg_enter("VisitedNetworkIDParser.parse");
        }
        try {
            char la;
            this.lexer.match(TokenTypes.P_VISITED_NETWORK_ID);
            this.lexer.SPorHT();
            this.lexer.match(58);
            this.lexer.SPorHT();
            while (true) {
                PVisitedNetworkID visitedNetworkID = new PVisitedNetworkID();
                if (this.lexer.lookAhead(0) == '\"') {
                    parseQuotedString(visitedNetworkID);
                } else {
                    parseToken(visitedNetworkID);
                }
                visitedNetworkIDList.add((SIPHeader) visitedNetworkID);
                this.lexer.SPorHT();
                la = this.lexer.lookAhead(0);
                if (la != ',') {
                    break;
                }
                this.lexer.match(44);
                this.lexer.SPorHT();
            }
            if (la == 10) {
                return visitedNetworkIDList;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unexpected char = ");
            stringBuilder.append(la);
            throw createParseException(stringBuilder.toString());
        } finally {
            if (debug) {
                dbg_leave("VisitedNetworkIDParser.parse");
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0027, code skipped:
            r6.setVisitedNetworkID(r0.toString());
            super.parse(r6);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void parseQuotedString(PVisitedNetworkID visitedNetworkID) throws ParseException {
        if (debug) {
            dbg_enter("parseQuotedString");
        }
        try {
            StringBuffer retval = new StringBuffer();
            if (this.lexer.lookAhead(0) == '\"') {
                this.lexer.consume(1);
                while (true) {
                    char next = this.lexer.getNextChar();
                    if (next == '\"') {
                        break;
                    } else if (next == 0) {
                        throw new ParseException("unexpected EOL", 1);
                    } else if (next == '\\') {
                        retval.append(next);
                        retval.append(this.lexer.getNextChar());
                    } else {
                        retval.append(next);
                    }
                }
                return;
            }
            throw createParseException("unexpected char");
        } finally {
            if (debug) {
                dbg_leave("parseQuotedString.parse");
            }
        }
    }

    protected void parseToken(PVisitedNetworkID visitedNetworkID) throws ParseException {
        this.lexer.match(4095);
        visitedNetworkID.setVisitedNetworkID(this.lexer.getNextToken());
        super.parse(visitedNetworkID);
    }
}
