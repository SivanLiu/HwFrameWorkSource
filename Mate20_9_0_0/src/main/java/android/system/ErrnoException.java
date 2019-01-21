package android.system;

import java.io.IOException;
import java.net.SocketException;
import libcore.io.Libcore;

public final class ErrnoException extends Exception {
    public final int errno;
    private final String functionName;

    public ErrnoException(String functionName, int errno) {
        this.functionName = functionName;
        this.errno = errno;
    }

    public ErrnoException(String functionName, int errno, Throwable cause) {
        super(cause);
        this.functionName = functionName;
        this.errno = errno;
    }

    public String getMessage() {
        String errnoName = OsConstants.errnoName(this.errno);
        if (errnoName == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("errno ");
            stringBuilder.append(this.errno);
            errnoName = stringBuilder.toString();
        }
        String description = Libcore.os.strerror(this.errno);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(this.functionName);
        stringBuilder2.append(" failed: ");
        stringBuilder2.append(errnoName);
        stringBuilder2.append(" (");
        stringBuilder2.append(description);
        stringBuilder2.append(")");
        return stringBuilder2.toString();
    }

    public IOException rethrowAsIOException() throws IOException {
        IOException newException = new IOException(getMessage());
        newException.initCause(this);
        throw newException;
    }

    public SocketException rethrowAsSocketException() throws SocketException {
        throw new SocketException(getMessage(), this);
    }
}
