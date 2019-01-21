package gov.nist.javax.sip.header;

import javax.sip.InvalidArgumentException;
import javax.sip.header.ExpiresHeader;

public class Expires extends SIPHeader implements ExpiresHeader {
    private static final long serialVersionUID = 3134344915465784267L;
    protected int expires;

    public Expires() {
        super("Expires");
    }

    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    protected StringBuffer encodeBody(StringBuffer buffer) {
        buffer.append(this.expires);
        return buffer;
    }

    public int getExpires() {
        return this.expires;
    }

    public void setExpires(int expires) throws InvalidArgumentException {
        if (expires >= 0) {
            this.expires = expires;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bad argument ");
        stringBuilder.append(expires);
        throw new InvalidArgumentException(stringBuilder.toString());
    }
}
