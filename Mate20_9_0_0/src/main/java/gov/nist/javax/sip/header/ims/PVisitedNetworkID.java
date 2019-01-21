package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.core.Token;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class PVisitedNetworkID extends ParametersHeader implements PVisitedNetworkIDHeader, SIPHeaderNamesIms, ExtensionHeader {
    private boolean isQuoted;
    private String networkID;

    public PVisitedNetworkID() {
        super("P-Visited-Network-ID");
    }

    public PVisitedNetworkID(String networkID) {
        super("P-Visited-Network-ID");
        setVisitedNetworkID(networkID);
    }

    public PVisitedNetworkID(Token tok) {
        super("P-Visited-Network-ID");
        setVisitedNetworkID(tok.getTokenValue());
    }

    protected String encodeBody() {
        StringBuilder stringBuilder;
        StringBuffer retval = new StringBuffer();
        if (getVisitedNetworkID() != null) {
            if (this.isQuoted) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(Separators.DOUBLE_QUOTE);
                stringBuilder.append(getVisitedNetworkID());
                stringBuilder.append(Separators.DOUBLE_QUOTE);
                retval.append(stringBuilder.toString());
            } else {
                retval.append(getVisitedNetworkID());
            }
        }
        if (!this.parameters.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(Separators.SEMICOLON);
            stringBuilder.append(this.parameters.encode());
            retval.append(stringBuilder.toString());
        }
        return retval.toString();
    }

    public void setVisitedNetworkID(String networkID) {
        if (networkID != null) {
            this.networkID = networkID;
            this.isQuoted = true;
            return;
        }
        throw new NullPointerException(" the networkID parameter is null");
    }

    public void setVisitedNetworkID(Token networkID) {
        if (networkID != null) {
            this.networkID = networkID.getTokenValue();
            this.isQuoted = false;
            return;
        }
        throw new NullPointerException(" the networkID parameter is null");
    }

    public String getVisitedNetworkID() {
        return this.networkID;
    }

    public void setValue(String value) throws ParseException {
        throw new ParseException(value, 0);
    }

    public boolean equals(Object other) {
        boolean z = false;
        if (!(other instanceof PVisitedNetworkIDHeader)) {
            return false;
        }
        PVisitedNetworkIDHeader o = (PVisitedNetworkIDHeader) other;
        if (getVisitedNetworkID().equals(o.getVisitedNetworkID()) && equalParameters(o)) {
            z = true;
        }
        return z;
    }

    public Object clone() {
        PVisitedNetworkID retval = (PVisitedNetworkID) super.clone();
        if (this.networkID != null) {
            retval.networkID = this.networkID;
        }
        retval.isQuoted = this.isQuoted;
        return retval;
    }
}
