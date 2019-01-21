package java.util;

public class MissingFormatArgumentException extends IllegalFormatException {
    private static final long serialVersionUID = 19190115;
    private String s;

    public MissingFormatArgumentException(String s) {
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Format specifier '");
        stringBuilder.append(this.s);
        stringBuilder.append("'");
        return stringBuilder.toString();
    }
}
