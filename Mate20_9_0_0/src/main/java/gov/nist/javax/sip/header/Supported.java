package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.header.SupportedHeader;

public class Supported extends SIPHeader implements SupportedHeader {
    private static final long serialVersionUID = -7679667592702854542L;
    protected String optionTag;

    public Supported() {
        super("Supported");
        this.optionTag = null;
    }

    public Supported(String option_tag) {
        super("Supported");
        this.optionTag = option_tag;
    }

    public String encode() {
        StringBuilder stringBuilder;
        String retval = new StringBuilder();
        retval.append(this.headerName);
        retval.append(Separators.COLON);
        retval = retval.toString();
        if (this.optionTag != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(retval);
            stringBuilder.append(Separators.SP);
            stringBuilder.append(this.optionTag);
            retval = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(retval);
        stringBuilder.append(Separators.NEWLINE);
        return stringBuilder.toString();
    }

    public String encodeBody() {
        return this.optionTag != null ? this.optionTag : "";
    }

    public void setOptionTag(String optionTag) throws ParseException {
        if (optionTag != null) {
            this.optionTag = optionTag;
            return;
        }
        throw new NullPointerException("JAIN-SIP Exception, Supported, setOptionTag(), the optionTag parameter is null");
    }

    public String getOptionTag() {
        return this.optionTag;
    }
}
