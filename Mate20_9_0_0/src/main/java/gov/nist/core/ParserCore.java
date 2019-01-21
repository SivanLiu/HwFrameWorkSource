package gov.nist.core;

import java.io.PrintStream;
import java.text.ParseException;

public abstract class ParserCore {
    public static final boolean debug = Debug.parserDebug;
    static int nesting_level;
    protected LexerCore lexer;

    protected NameValue nameValue(char separator) throws ParseException {
        if (debug) {
            dbg_enter("nameValue");
        }
        Token name;
        try {
            this.lexer.match(4095);
            name = this.lexer.getNextToken();
            this.lexer.SPorHT();
            boolean quoted = false;
            NameValue nv;
            if (this.lexer.lookAhead(0) == separator) {
                String str;
                this.lexer.consume(1);
                this.lexer.SPorHT();
                boolean isFlag = false;
                if (this.lexer.lookAhead(0) == '\"') {
                    quoted = true;
                    str = this.lexer.quotedString();
                } else {
                    this.lexer.match(4095);
                    str = this.lexer.getNextToken().tokenValue;
                    if (str == null) {
                        str = "";
                        isFlag = true;
                    }
                }
                nv = new NameValue(name.tokenValue, str, isFlag);
                if (quoted) {
                    nv.setQuotedValue();
                }
                if (debug) {
                    dbg_leave("nameValue");
                }
                return nv;
            }
            nv = new NameValue(name.tokenValue, "", true);
            if (debug) {
                dbg_leave("nameValue");
            }
            return nv;
        } catch (ParseException e) {
            NameValue nameValue = new NameValue(name.tokenValue, null, false);
            if (debug) {
                dbg_leave("nameValue");
            }
            return nameValue;
        } catch (Throwable th) {
            if (debug) {
                dbg_leave("nameValue");
            }
            throw th;
        }
    }

    protected void dbg_enter(String rule) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < nesting_level; i++) {
            stringBuffer.append(Separators.GREATER_THAN);
        }
        if (debug) {
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuffer);
            stringBuilder.append(rule);
            stringBuilder.append("\nlexer buffer = \n");
            stringBuilder.append(this.lexer.getRest());
            printStream.println(stringBuilder.toString());
        }
        nesting_level++;
    }

    protected void dbg_leave(String rule) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < nesting_level; i++) {
            stringBuffer.append(Separators.LESS_THAN);
        }
        if (debug) {
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(stringBuffer);
            stringBuilder.append(rule);
            stringBuilder.append("\nlexer buffer = \n");
            stringBuilder.append(this.lexer.getRest());
            printStream.println(stringBuilder.toString());
        }
        nesting_level--;
    }

    protected NameValue nameValue() throws ParseException {
        return nameValue('=');
    }

    protected void peekLine(String rule) {
        if (debug) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(rule);
            stringBuilder.append(Separators.SP);
            stringBuilder.append(this.lexer.peekLine());
            Debug.println(stringBuilder.toString());
        }
    }
}
