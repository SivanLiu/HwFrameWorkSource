package java.io;

public class WriteAbortedException extends ObjectStreamException {
    private static final long serialVersionUID = -3326426625597282442L;
    public Exception detail;

    public WriteAbortedException(String s, Exception ex) {
        super(s);
        initCause(null);
        this.detail = ex;
    }

    public String getMessage() {
        if (this.detail == null) {
            return super.getMessage();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.getMessage());
        stringBuilder.append("; ");
        stringBuilder.append(this.detail.toString());
        return stringBuilder.toString();
    }

    public Throwable getCause() {
        return this.detail;
    }
}
