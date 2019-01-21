package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.GenericURI;
import java.text.ParseException;

public class AddressParser extends Parser {
    public AddressParser(Lexer lexer) {
        this.lexer = lexer;
        this.lexer.selectLexer("charLexer");
    }

    public AddressParser(String address) {
        this.lexer = new Lexer("charLexer", address);
    }

    protected AddressImpl nameAddr() throws ParseException {
        if (debug) {
            dbg_enter("nameAddr");
        }
        try {
            AddressImpl retval = 60;
            if (this.lexer.lookAhead(0) == retval) {
                this.lexer.consume(1);
                this.lexer.selectLexer("sip_urlLexer");
                this.lexer.SPorHT();
                GenericURI uri = new URLParser((Lexer) this.lexer).uriReference(true);
                retval = new AddressImpl();
                retval.setAddressType(1);
                retval.setURI(uri);
                this.lexer.SPorHT();
                this.lexer.match(62);
                return retval;
            }
            String name;
            AddressImpl addr = new AddressImpl();
            addr.setAddressType(1);
            if (this.lexer.lookAhead(0) == '\"') {
                name = this.lexer.quotedString();
                this.lexer.SPorHT();
            } else {
                name = this.lexer.getNextToken(retval);
            }
            addr.setDisplayName(name.trim());
            this.lexer.match(retval);
            this.lexer.SPorHT();
            GenericURI uri2 = new URLParser((Lexer) this.lexer).uriReference(true);
            AddressImpl retval2 = new AddressImpl();
            addr.setAddressType(1);
            addr.setURI(uri2);
            this.lexer.SPorHT();
            this.lexer.match(62);
            if (debug) {
                dbg_leave("nameAddr");
            }
            return addr;
        } finally {
            if (debug) {
                dbg_leave("nameAddr");
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x0072  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public AddressImpl address(boolean inclParams) throws ParseException {
        char la;
        AddressImpl retval;
        if (debug) {
            dbg_enter("address");
        }
        int k = 0;
        while (this.lexer.hasMoreChars()) {
            try {
                la = this.lexer.lookAhead(k);
                if (la == '<' || la == '\"' || la == ':') {
                    break;
                } else if (la == '/') {
                    break;
                } else if (la != 0) {
                    k++;
                } else {
                    throw createParseException("unexpected EOL");
                }
            } catch (Throwable th) {
                if (debug) {
                    dbg_leave("address");
                }
            }
        }
        la = this.lexer.lookAhead(k);
        if (la != '<') {
            if (la != '\"') {
                if (la != ':') {
                    if (la != '/') {
                        throw createParseException("Bad address spec");
                    }
                }
                retval = new AddressImpl();
                GenericURI uri = new URLParser((Lexer) this.lexer).uriReference(inclParams);
                retval.setAddressType(2);
                retval.setURI(uri);
                if (debug) {
                    dbg_leave("address");
                }
                return retval;
            }
        }
        retval = nameAddr();
        if (debug) {
        }
        return retval;
    }
}
