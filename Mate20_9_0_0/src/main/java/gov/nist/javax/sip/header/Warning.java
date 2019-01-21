package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.WarningHeader;

public class Warning extends SIPHeader implements WarningHeader {
    private static final long serialVersionUID = -3433328864230783899L;
    protected String agent;
    protected int code;
    protected String text;

    public Warning() {
        super("Warning");
    }

    public String encodeBody() {
        StringBuilder stringBuilder;
        if (this.text != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(Integer.toString(this.code));
            stringBuilder.append(Separators.SP);
            stringBuilder.append(this.agent);
            stringBuilder.append(Separators.SP);
            stringBuilder.append(Separators.DOUBLE_QUOTE);
            stringBuilder.append(this.text);
            stringBuilder.append(Separators.DOUBLE_QUOTE);
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(Integer.toString(this.code));
        stringBuilder.append(Separators.SP);
        stringBuilder.append(this.agent);
        return stringBuilder.toString();
    }

    public int getCode() {
        return this.code;
    }

    public String getAgent() {
        return this.agent;
    }

    public String getText() {
        return this.text;
    }

    public void setCode(int code) throws InvalidArgumentException {
        if (code <= 99 || code >= 1000) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Code parameter in the Warning header is invalid: code=");
            stringBuilder.append(code);
            throw new InvalidArgumentException(stringBuilder.toString());
        }
        this.code = code;
    }

    public void setAgent(String host) throws ParseException {
        if (host != null) {
            this.agent = host;
            return;
        }
        throw new NullPointerException("the host parameter in the Warning header is null");
    }

    public void setText(String text) throws ParseException {
        if (text != null) {
            this.text = text;
            return;
        }
        throw new ParseException("The text parameter in the Warning header is null", 0);
    }
}
