package android.system;

import java.net.UnknownHostException;
import libcore.io.Libcore;

public final class GaiException extends RuntimeException {
    public final int error;
    private final String functionName;

    public GaiException(String functionName, int error) {
        this.functionName = functionName;
        this.error = error;
    }

    public GaiException(String functionName, int error, Throwable cause) {
        super(cause);
        this.functionName = functionName;
        this.error = error;
    }

    public String getMessage() {
        String gaiName = OsConstants.gaiName(this.error);
        if (gaiName == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("GAI_ error ");
            stringBuilder.append(this.error);
            gaiName = stringBuilder.toString();
        }
        String description = Libcore.os.gai_strerror(this.error);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(this.functionName);
        stringBuilder2.append(" failed: ");
        stringBuilder2.append(gaiName);
        stringBuilder2.append(" (");
        stringBuilder2.append(description);
        stringBuilder2.append(")");
        return stringBuilder2.toString();
    }

    public UnknownHostException rethrowAsUnknownHostException(String detailMessage) throws UnknownHostException {
        UnknownHostException newException = new UnknownHostException(detailMessage);
        newException.initCause(this);
        throw newException;
    }

    public UnknownHostException rethrowAsUnknownHostException() throws UnknownHostException {
        throw rethrowAsUnknownHostException(getMessage());
    }
}
