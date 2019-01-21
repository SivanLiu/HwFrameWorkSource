package java.util;

public class FormatFlagsConversionMismatchException extends IllegalFormatException {
    private static final long serialVersionUID = 19120414;
    private char c;
    private String f;

    public FormatFlagsConversionMismatchException(String f, char c) {
        if (f != null) {
            this.f = f;
            this.c = c;
            return;
        }
        throw new NullPointerException();
    }

    public String getFlags() {
        return this.f;
    }

    public char getConversion() {
        return this.c;
    }

    public String getMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Conversion = ");
        stringBuilder.append(this.c);
        stringBuilder.append(", Flags = ");
        stringBuilder.append(this.f);
        return stringBuilder.toString();
    }
}
