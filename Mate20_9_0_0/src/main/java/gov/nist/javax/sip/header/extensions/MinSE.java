package gov.nist.javax.sip.header.extensions;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.ExtensionHeader;

public class MinSE extends ParametersHeader implements ExtensionHeader, MinSEHeader {
    public static final String NAME = "Min-SE";
    private static final long serialVersionUID = 3134344915465784267L;
    public int expires;

    public MinSE() {
        super("Min-SE");
    }

    public String encodeBody() {
        String retval = Integer.toString(this.expires);
        if (this.parameters.isEmpty()) {
            return retval;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(retval);
        stringBuilder.append(Separators.SEMICOLON);
        stringBuilder.append(this.parameters.encode());
        return stringBuilder.toString();
    }

    public void setValue(String value) throws ParseException {
        throw new ParseException(value, 0);
    }

    public int getExpires() {
        return this.expires;
    }

    public void setExpires(int expires) throws InvalidArgumentException {
        if (expires >= 0) {
            this.expires = expires;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bad argument ");
        stringBuilder.append(expires);
        throw new InvalidArgumentException(stringBuilder.toString());
    }
}
