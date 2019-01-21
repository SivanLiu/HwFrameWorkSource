package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import javax.sip.header.ReferToHeader;

public final class ReferTo extends AddressParametersHeader implements ReferToHeader {
    private static final long serialVersionUID = -1666700428440034851L;

    public ReferTo() {
        super(ReferToHeader.NAME);
    }

    protected String encodeBody() {
        if (this.address == null) {
            return null;
        }
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
        if (!this.parameters.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(retval);
            stringBuilder.append(Separators.SEMICOLON);
            stringBuilder.append(this.parameters.encode());
            retval = stringBuilder.toString();
        }
        return retval;
    }
}
