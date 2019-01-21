package android.os;

public class ServiceSpecificException extends RuntimeException {
    public final int errorCode;

    public ServiceSpecificException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ServiceSpecificException(int errorCode) {
        this.errorCode = errorCode;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.toString());
        stringBuilder.append(" (code ");
        stringBuilder.append(this.errorCode);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
