package org.apache.commons.logging;

@Deprecated
public class LogConfigurationException extends RuntimeException {
    protected Throwable cause;

    public LogConfigurationException() {
        this.cause = null;
    }

    public LogConfigurationException(String message) {
        super(message);
        this.cause = null;
    }

    public LogConfigurationException(Throwable cause) {
        this(cause == null ? null : cause.toString(), cause);
    }

    public LogConfigurationException(String message, Throwable cause) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(message);
        stringBuilder.append(" (Caused by ");
        stringBuilder.append(cause);
        stringBuilder.append(")");
        super(stringBuilder.toString());
        this.cause = null;
        this.cause = cause;
    }

    public Throwable getCause() {
        return this.cause;
    }
}
