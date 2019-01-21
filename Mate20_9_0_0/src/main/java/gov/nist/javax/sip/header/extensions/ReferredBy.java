package gov.nist.javax.sip.header.extensions;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.AddressParametersHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public final class ReferredBy extends AddressParametersHeader implements ExtensionHeader, ReferredByHeader {
    public static final String NAME = "Referred-By";
    private static final long serialVersionUID = 3134344915465784267L;

    public ReferredBy() {
        super("Referred-By");
    }

    public void setValue(String value) throws ParseException {
        throw new ParseException(value, 0);
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
