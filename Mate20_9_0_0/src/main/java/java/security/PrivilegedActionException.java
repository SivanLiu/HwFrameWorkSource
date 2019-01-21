package java.security;

public class PrivilegedActionException extends Exception {
    private static final long serialVersionUID = 4724086851538908602L;
    private Exception exception;

    public PrivilegedActionException(Exception exception) {
        super((Throwable) null);
        this.exception = exception;
    }

    public Exception getException() {
        return this.exception;
    }

    public Throwable getCause() {
        return this.exception;
    }

    public String toString() {
        String s = getClass().getName();
        if (this.exception == null) {
            return s;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(s);
        stringBuilder.append(": ");
        stringBuilder.append(this.exception.toString());
        return stringBuilder.toString();
    }
}
