package gov.nist.javax.sip.header;

import gov.nist.core.Separators;
import gov.nist.javax.sip.SIPConstants;

public final class StatusLine extends SIPObject implements SipStatusLine {
    private static final long serialVersionUID = -4738092215519950414L;
    protected boolean matchStatusClass;
    protected String reasonPhrase = null;
    protected String sipVersion = SIPConstants.SIP_VERSION_STRING;
    protected int statusCode;

    public boolean match(Object matchObj) {
        if (!(matchObj instanceof StatusLine)) {
            return false;
        }
        StatusLine sl = (StatusLine) matchObj;
        if (sl.matchExpression != null) {
            return sl.matchExpression.match(encode());
        }
        if (sl.sipVersion != null && !sl.sipVersion.equals(this.sipVersion)) {
            return false;
        }
        if (sl.statusCode != 0) {
            if (this.matchStatusClass) {
                int hiscode = sl.statusCode;
                if (Integer.toString(sl.statusCode).charAt(0) != Integer.toString(this.statusCode).charAt(0)) {
                    return false;
                }
            } else if (this.statusCode != sl.statusCode) {
                return false;
            }
        }
        if (sl.reasonPhrase == null || this.reasonPhrase == sl.reasonPhrase) {
            return true;
        }
        return this.reasonPhrase.equals(sl.reasonPhrase);
    }

    public void setMatchStatusClass(boolean flag) {
        this.matchStatusClass = flag;
    }

    public String encode() {
        StringBuilder stringBuilder;
        String encoding = new StringBuilder();
        encoding.append("SIP/2.0 ");
        encoding.append(this.statusCode);
        encoding = encoding.toString();
        if (this.reasonPhrase != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(encoding);
            stringBuilder.append(Separators.SP);
            stringBuilder.append(this.reasonPhrase);
            encoding = stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(encoding);
        stringBuilder.append(Separators.NEWLINE);
        return stringBuilder.toString();
    }

    public String getSipVersion() {
        return this.sipVersion;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    public String getReasonPhrase() {
        return this.reasonPhrase;
    }

    public void setSipVersion(String s) {
        this.sipVersion = s;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    public String getVersionMajor() {
        if (this.sipVersion == null) {
            return null;
        }
        String major = null;
        boolean slash = false;
        for (int i = 0; i < this.sipVersion.length(); i++) {
            if (this.sipVersion.charAt(i) == '.') {
                slash = false;
            }
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
}
