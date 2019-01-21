package gov.nist.javax.sip.header.ims;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.ParametersHeader;
import java.text.ParseException;
import javax.sip.header.ExtensionHeader;

public class PUserDatabase extends ParametersHeader implements PUserDatabaseHeader, SIPHeaderNamesIms, ExtensionHeader {
    private String databaseName;

    public PUserDatabase(String databaseName) {
        super("P-User-Database");
        this.databaseName = databaseName;
    }

    public PUserDatabase() {
        super("P-User-Database");
    }

    public String getDatabaseName() {
        return this.databaseName;
    }

    public void setDatabaseName(String databaseName) {
        if (databaseName == null || databaseName.equals(Separators.SP)) {
            throw new NullPointerException("Database name is null");
        } else if (databaseName.contains("aaa://")) {
            this.databaseName = databaseName;
        } else {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("aaa://");
            stringBuffer.append(databaseName);
            this.databaseName = stringBuffer.toString();
        }
    }

    protected String encodeBody() {
        StringBuffer retval = new StringBuffer();
        retval.append(Separators.LESS_THAN);
        if (getDatabaseName() != null) {
            retval.append(getDatabaseName());
        }
        if (!this.parameters.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Separators.SEMICOLON);
            stringBuilder.append(this.parameters.encode());
            retval.append(stringBuilder.toString());
        }
        retval.append(Separators.GREATER_THAN);
        return retval.toString();
    }

    public boolean equals(Object other) {
        return (other instanceof PUserDatabaseHeader) && super.equals(other);
    }

    public Object clone() {
        return (PUserDatabase) super.clone();
    }

    public void setValue(String value) throws ParseException {
        throw new ParseException(value, 0);
    }
}
