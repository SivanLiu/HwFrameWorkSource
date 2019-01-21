package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.RetryAfterHeader;

public class RetryAfter extends ParametersHeader implements RetryAfterHeader {
    public static final String DURATION = "duration";
    private static final long serialVersionUID = -1029458515616146140L;
    protected String comment;
    protected Integer retryAfter = new Integer(0);

    public RetryAfter() {
        super("Retry-After");
    }

    public String encodeBody() {
        StringBuilder stringBuilder;
        StringBuffer s = new StringBuffer();
        if (this.retryAfter != null) {
            s.append(this.retryAfter);
        }
        if (this.comment != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" (");
            stringBuilder.append(this.comment);
            stringBuilder.append(Separators.RPAREN);
            s.append(stringBuilder.toString());
        }
        if (!this.parameters.isEmpty()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(Separators.SEMICOLON);
            stringBuilder.append(this.parameters.encode());
            s.append(stringBuilder.toString());
        }
        return s.toString();
    }

    public boolean hasComment() {
        return this.comment != null;
    }

    public void removeComment() {
        this.comment = null;
    }

    public void removeDuration() {
        super.removeParameter("duration");
    }

    public void setRetryAfter(int retryAfter) throws InvalidArgumentException {
        if (retryAfter >= 0) {
            this.retryAfter = Integer.valueOf(retryAfter);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid parameter ");
        stringBuilder.append(retryAfter);
        throw new InvalidArgumentException(stringBuilder.toString());
    }

    public int getRetryAfter() {
        return this.retryAfter.intValue();
    }

    public String getComment() {
        return this.comment;
    }

    public void setComment(String comment) throws ParseException {
        if (comment != null) {
            this.comment = comment;
            return;
        }
        throw new NullPointerException("the comment parameter is null");
    }

    public void setDuration(int duration) throws InvalidArgumentException {
        if (duration >= 0) {
            setParameter("duration", duration);
            return;
        }
        throw new InvalidArgumentException("the duration parameter is <0");
    }

    public int getDuration() {
        if (getParameter("duration") == null) {
            return -1;
        }
        return super.getParameterAsInt("duration");
    }
}
