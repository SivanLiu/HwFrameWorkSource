package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import javax.sip.InvalidArgumentException;
import javax.sip.header.MimeVersionHeader;

public class MimeVersion extends SIPHeader implements MimeVersionHeader {
    private static final long serialVersionUID = -7951589626435082068L;
    protected int majorVersion;
    protected int minorVersion;

    public MimeVersion() {
        super("MIME-Version");
    }

    public int getMinorVersion() {
        return this.minorVersion;
    }

    public int getMajorVersion() {
        return this.majorVersion;
    }

    public void setMinorVersion(int minorVersion) throws InvalidArgumentException {
        if (minorVersion >= 0) {
            this.minorVersion = minorVersion;
            return;
        }
        throw new InvalidArgumentException("JAIN-SIP Exception, MimeVersion, setMinorVersion(), the minorVersion parameter is null");
    }

    public void setMajorVersion(int majorVersion) throws InvalidArgumentException {
        if (majorVersion >= 0) {
            this.majorVersion = majorVersion;
            return;
        }
        throw new InvalidArgumentException("JAIN-SIP Exception, MimeVersion, setMajorVersion(), the majorVersion parameter is null");
    }

    public String encodeBody() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Integer.toString(this.majorVersion));
        stringBuilder.append(Separators.DOT);
        stringBuilder.append(Integer.toString(this.minorVersion));
        return stringBuilder.toString();
    }
}
