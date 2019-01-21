package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import gov.nist.javax.sip.address.GenericURI;
import java.text.ParseException;
import javax.sip.address.URI;
import javax.sip.header.ErrorInfoHeader;

public final class ErrorInfo extends ParametersHeader implements ErrorInfoHeader {
    private static final long serialVersionUID = -6347702901964436362L;
    protected GenericURI errorInfo;

    public ErrorInfo() {
        super("Error-Info");
    }

    public ErrorInfo(GenericURI errorInfo) {
        this();
        this.errorInfo = errorInfo;
    }

    public String encodeBody() {
        StringBuffer retval = new StringBuffer(Separators.LESS_THAN);
        retval.append(this.errorInfo.toString());
        retval = retval.append(Separators.GREATER_THAN);
        if (!this.parameters.isEmpty()) {
            retval.append(Separators.SEMICOLON);
            retval.append(this.parameters.encode());
        }
        return retval.toString();
    }

    public void setErrorInfo(URI errorInfo) {
        this.errorInfo = (GenericURI) errorInfo;
    }

    public URI getErrorInfo() {
        return this.errorInfo;
    }

    public void setErrorMessage(String message) throws ParseException {
        if (message != null) {
            setParameter("message", message);
            return;
        }
        throw new NullPointerException("JAIN-SIP Exception , ErrorInfoHeader, setErrorMessage(), the message parameter is null");
    }

    public String getErrorMessage() {
        return getParameter("message");
    }

    public Object clone() {
        ErrorInfo retval = (ErrorInfo) super.clone();
        if (this.errorInfo != null) {
            retval.errorInfo = (GenericURI) this.errorInfo.clone();
        }
        return retval;
    }
}
