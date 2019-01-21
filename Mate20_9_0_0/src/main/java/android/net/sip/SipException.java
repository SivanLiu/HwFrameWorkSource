package android.net.sip;

public class SipException extends Exception {
    public SipException(String message) {
        super(message);
    }

    public SipException(String message, Throwable cause) {
        Throwable th;
        if (!(cause instanceof javax.sip.SipException) || cause.getCause() == null) {
            th = cause;
        } else {
            th = cause.getCause();
        }
        super(message, th);
    }
}
