package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import java.text.ParseException;
import javax.sip.InvalidArgumentException;
import javax.sip.header.SubscriptionStateHeader;

public class SubscriptionState extends ParametersHeader implements SubscriptionStateHeader {
    private static final long serialVersionUID = -6673833053927258745L;
    protected int expires = -1;
    protected String reasonCode;
    protected int retryAfter = -1;
    protected String state;

    public SubscriptionState() {
        super("Subscription-State");
    }

    public void setExpires(int expires) throws InvalidArgumentException {
        if (expires >= 0) {
            this.expires = expires;
            return;
        }
        throw new InvalidArgumentException("JAIN-SIP Exception, SubscriptionState, setExpires(), the expires parameter is  < 0");
    }

    public int getExpires() {
        return this.expires;
    }

    public void setRetryAfter(int retryAfter) throws InvalidArgumentException {
        if (retryAfter > 0) {
            this.retryAfter = retryAfter;
            return;
        }
        throw new InvalidArgumentException("JAIN-SIP Exception, SubscriptionState, setRetryAfter(), the retryAfter parameter is <=0");
    }

    public int getRetryAfter() {
        return this.retryAfter;
    }

    public String getReasonCode() {
        return this.reasonCode;
    }

    public void setReasonCode(String reasonCode) throws ParseException {
        if (reasonCode != null) {
            this.reasonCode = reasonCode;
            return;
        }
        throw new NullPointerException("JAIN-SIP Exception, SubscriptionState, setReasonCode(), the reasonCode parameter is null");
    }

    public String getState() {
        return this.state;
    }

    public void setState(String state) throws ParseException {
        if (state != null) {
            this.state = state;
            return;
        }
        throw new NullPointerException("JAIN-SIP Exception, SubscriptionState, setState(), the state parameter is null");
    }

    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    protected StringBuffer encodeBody(StringBuffer buffer) {
        if (this.state != null) {
            buffer.append(this.state);
        }
        if (this.reasonCode != null) {
            buffer.append(";reason=");
            buffer.append(this.reasonCode);
        }
        if (this.expires != -1) {
            buffer.append(";expires=");
            buffer.append(this.expires);
        }
        if (this.retryAfter != -1) {
            buffer.append(";retry-after=");
            buffer.append(this.retryAfter);
        }
        if (!this.parameters.isEmpty()) {
            buffer.append(Separators.SEMICOLON);
            this.parameters.encode(buffer);
        }
        return buffer;
    }
}
