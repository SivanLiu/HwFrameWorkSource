package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import javax.sip.header.ExtensionHeader;

public class ExtensionHeaderImpl extends SIPHeader implements ExtensionHeader {
    private static final long serialVersionUID = -8693922839612081849L;
    protected String value;

    public ExtensionHeaderImpl(String headerName) {
        super(headerName);
    }

    public void setName(String headerName) {
        this.headerName = headerName;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getHeaderValue() {
        if (this.value != null) {
            return this.value;
        }
        String encodedHdr = null;
        try {
            encodedHdr = new StringBuffer(encode());
            while (encodedHdr.length() > 0 && encodedHdr.charAt(0) != ':') {
                encodedHdr.deleteCharAt(0);
            }
            encodedHdr.deleteCharAt(0);
            this.value = encodedHdr.toString().trim();
            return this.value;
        } catch (Exception e) {
            return null;
        }
    }

    public String encode() {
        StringBuffer stringBuffer = new StringBuffer(this.headerName);
        stringBuffer.append(Separators.COLON);
        stringBuffer.append(Separators.SP);
        stringBuffer.append(this.value);
        stringBuffer.append(Separators.NEWLINE);
        return stringBuffer.toString();
    }

    public String encodeBody() {
        return getHeaderValue();
    }
}
