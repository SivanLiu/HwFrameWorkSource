package org.bouncycastle.cms;

public class CMSRuntimeException extends RuntimeException {
    Exception e;

    public CMSRuntimeException(String str) {
        super(str);
    }

    public CMSRuntimeException(String str, Exception exception) {
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
