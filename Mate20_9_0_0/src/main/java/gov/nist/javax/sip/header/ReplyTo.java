package gov.nist.javax.sip.header;

import gov.nist.core.HostPort;
import gov.nist.core.Separators;
import gov.nist.javax.sip.address.AddressImpl;
import javax.sip.header.ReplyToHeader;

public final class ReplyTo extends AddressParametersHeader implements ReplyToHeader {
    private static final long serialVersionUID = -9103698729465531373L;

    public ReplyTo() {
        super("Reply-To");
    }

    public ReplyTo(AddressImpl address) {
        super("Reply-To");
        this.address = address;
    }

    public String encode() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.headerName);
        stringBuilder.append(Separators.COLON);
        stringBuilder.append(Separators.SP);
        stringBuilder.append(encodeBody());
        stringBuilder.append(Separators.NEWLINE);
        return stringBuilder.toString();
    }

    public String encodeBody() {
        StringBuilder stringBuilder;
        String retval = "";
        if (this.address.getAddressType() == 2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(retval);
            stringBuilder.append(Separators.LESS_THAN);
            retval = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(retval);
        stringBuilder.append(this.address.encode());
        retval = stringBuilder.toString();
        if (this.address.getAddressType() == 2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(retval);
            stringBuilder.append(Separators.GREATER_THAN);
            retval = stringBuilder.toString();
        }
        if (this.parameters.isEmpty()) {
            return retval;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(retval);
        stringBuilder.append(Separators.SEMICOLON);
        stringBuilder.append(this.parameters.encode());
        return stringBuilder.toString();
    }

    public HostPort getHostPort() {
        return this.address.getHostPort();
    }

    public String getDisplayName() {
        return this.address.getDisplayName();
    }
}
