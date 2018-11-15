package org.bouncycastle.cms;

public class CMSException extends Exception {
    Exception e;

    public CMSException(String str) {
        super(str);
    }

    public CMSException(String str, Exception exception) {
        super(str);
        this.e = exception;
    }

    public Throwable getCause() {
        return this.e;
    }

    public Exception getUnderlyingException() {
        return this.e;
    }
}
