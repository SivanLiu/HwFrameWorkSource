package gov.nist.javax.sip.header;

import javax.sip.InvalidArgumentException;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.TooManyHopsException;

public class MaxForwards extends SIPHeader implements MaxForwardsHeader {
    private static final long serialVersionUID = -3096874323347175943L;
    protected int maxForwards;

    public MaxForwards() {
        super("Max-Forwards");
    }

    public MaxForwards(int m) throws InvalidArgumentException {
        super("Max-Forwards");
        setMaxForwards(m);
    }

    public int getMaxForwards() {
        return this.maxForwards;
    }

    public void setMaxForwards(int maxForwards) throws InvalidArgumentException {
        if (maxForwards < 0 || maxForwards > 255) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bad max forwards value ");
            stringBuilder.append(maxForwards);
            throw new InvalidArgumentException(stringBuilder.toString());
        }
        this.maxForwards = maxForwards;
    }

    public String encodeBody() {
        return encodeBody(new StringBuffer()).toString();
    }

    protected StringBuffer encodeBody(StringBuffer buffer) {
        buffer.append(this.maxForwards);
        return buffer;
    }

    public boolean hasReachedZero() {
        return this.maxForwards == 0;
    }

    public void decrementMaxForwards() throws TooManyHopsException {
        if (this.maxForwards > 0) {
            this.maxForwards--;
            return;
        }
        throw new TooManyHopsException("has already reached 0!");
    }

    public boolean equals(Object other) {
        boolean z = true;
        if (this == other) {
            return true;
        }
        if (!(other instanceof MaxForwardsHeader)) {
            return false;
        }
        if (getMaxForwards() != ((MaxForwardsHeader) other).getMaxForwards()) {
            z = false;
        }
        return z;
    }
}
