package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import gov.nist.javax.sip.SIPConstants;
import gov.nist.javax.sip.address.GenericURI;
import javax.sip.address.URI;

public class RequestLine extends SIPObject implements SipRequestLine {
    private static final long serialVersionUID = -3286426172326043129L;
    protected String method;
    protected String sipVersion = SIPConstants.SIP_VERSION_STRING;
    protected GenericURI uri;

    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    public StringBuffer encode(StringBuffer buffer) {
        if (this.method != null) {
            buffer.append(this.method);
            buffer.append(Separators.SP);
        }
        if (this.uri != null) {
            this.uri.encode(buffer);
            buffer.append(Separators.SP);
        }
        buffer.append(this.sipVersion);
        buffer.append(Separators.NEWLINE);
        return buffer;
    }

    public GenericURI getUri() {
        return this.uri;
    }

    public RequestLine(GenericURI requestURI, String method) {
        this.uri = requestURI;
        this.method = method;
    }

    public String getMethod() {
        return this.method;
    }

    public String getSipVersion() {
        return this.sipVersion;
    }

    public void setUri(URI uri) {
        this.uri = (GenericURI) uri;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setSipVersion(String version) {
        this.sipVersion = version;
    }

    public String getVersionMajor() {
        if (this.sipVersion == null) {
            return null;
        }
        String major = null;
        boolean slash = false;
        int i = 0;
        while (i < this.sipVersion.length() && this.sipVersion.charAt(i) != '.') {
            if (slash) {
                StringBuilder stringBuilder;
                if (major == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("");
                    stringBuilder.append(this.sipVersion.charAt(i));
                    major = stringBuilder.toString();
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(major);
                    stringBuilder.append(this.sipVersion.charAt(i));
                    major = stringBuilder.toString();
                }
            }
            if (this.sipVersion.charAt(i) == '/') {
                slash = true;
            }
            i++;
        }
        return major;
    }

    public String getVersionMinor() {
        if (this.sipVersion == null) {
            return null;
        }
        String minor = null;
        boolean dot = false;
        for (int i = 0; i < this.sipVersion.length(); i++) {
            if (dot) {
                StringBuilder stringBuilder;
                if (minor == null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("");
                    stringBuilder.append(this.sipVersion.charAt(i));
                    minor = stringBuilder.toString();
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(minor);
                    stringBuilder.append(this.sipVersion.charAt(i));
                    minor = stringBuilder.toString();
                }
            }
            if (this.sipVersion.charAt(i) == '.') {
                dot = true;
            }
        }
        return minor;
    }

    public boolean equals(Object other) {
        boolean retval = false;
        if (!other.getClass().equals(getClass())) {
            return false;
        }
        RequestLine that = (RequestLine) other;
        try {
            if (this.method.equals(that.method) && this.uri.equals(that.uri) && this.sipVersion.equals(that.sipVersion)) {
                retval = true;
            }
        } catch (NullPointerException e) {
        }
        return retval;
    }

    public Object clone() {
        RequestLine retval = (RequestLine) super.clone();
        if (this.uri != null) {
            retval.uri = (GenericURI) this.uri.clone();
        }
        return retval;
    }
}
