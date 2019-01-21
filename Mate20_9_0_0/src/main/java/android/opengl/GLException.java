package android.opengl;

public class GLException extends RuntimeException {
    private final int mError;

    public GLException(int error) {
        super(getErrorString(error));
        this.mError = error;
    }

    public GLException(int error, String string) {
        super(string);
        this.mError = error;
    }

    private static String getErrorString(int error) {
        String errorString = GLU.gluErrorString(error);
        if (errorString != null) {
            return errorString;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown error 0x");
        stringBuilder.append(Integer.toHexString(error));
        return stringBuilder.toString();
    }

    int getError() {
        return this.mError;
    }
}
