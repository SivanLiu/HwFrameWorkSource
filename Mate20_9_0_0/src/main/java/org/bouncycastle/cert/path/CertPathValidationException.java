package org.bouncycastle.cert.path;

public class CertPathValidationException extends Exception {
    private final Exception cause;

    public CertPathValidationException(String str) {
        this(str, null);
    }

    public CertPathValidationException(String str, Exception exception) {
        super(str);
        this.cause = exception;
    }

    public Throwable getCause() {
        return this.cause;
    }
}
