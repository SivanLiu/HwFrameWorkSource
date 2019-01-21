package java.util;

public class IllegalFormatFlagsException extends IllegalFormatException {
    private static final long serialVersionUID = 790824;
    private String flags;

    public IllegalFormatFlagsException(String f) {
        if (f != null) {
            this.flags = f;
            return;
        }
        throw new NullPointerException();
    }

    public String getFlags() {
        return this.flags;
    }

    public String getMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Flags = '");
        stringBuilder.append(this.flags);
        stringBuilder.append("'");
        return stringBuilder.toString();
    }
}
