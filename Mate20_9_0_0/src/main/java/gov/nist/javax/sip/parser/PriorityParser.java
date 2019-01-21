package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.Priority;
import gov.nist.javax.sip.header.SIPHeader;
import java.io.PrintStream;
import java.text.ParseException;

public class PriorityParser extends HeaderParser {
    public PriorityParser(String priority) {
        super(priority);
    }

    protected PriorityParser(Lexer lexer) {
        super(lexer);
    }

    public SIPHeader parse() throws ParseException {
        if (debug) {
            dbg_enter("PriorityParser.parse");
        }
        Priority priority = new Priority();
        try {
            headerName(TokenTypes.PRIORITY);
            priority.setHeaderName("Priority");
            this.lexer.SPorHT();
            priority.setPriority(this.lexer.ttokenSafe());
            this.lexer.SPorHT();
            this.lexer.match(10);
            return priority;
        } finally {
            if (debug) {
                dbg_leave("PriorityParser.parse");
            }
        }
    }

    public static void main(String[] args) throws ParseException {
        String[] p = new String[]{"Priority: 8;a\n"};
        for (String priorityParser : p) {
            Priority prio = (Priority) new PriorityParser(priorityParser).parse();
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("encoded = ");
            stringBuilder.append(prio.encode());
            printStream.println(stringBuilder.toString());
        }
    }
}
