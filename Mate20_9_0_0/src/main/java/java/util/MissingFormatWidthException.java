package java.util;

public class MissingFormatWidthException extends IllegalFormatException {
    private static final long serialVersionUID = 15560123;
    private String s;

    public MissingFormatWidthException(String s) {
        if (s != null) {
            this.s = s;
            return;
        }
        throw new NullPointerException();
    }

    public String getFormatSpecifier() {
        return this.s;
    }

    public String getMessage() {
        return this.s;
    }
}
