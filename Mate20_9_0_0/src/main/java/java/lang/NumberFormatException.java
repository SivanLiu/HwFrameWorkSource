package java.lang;

public class NumberFormatException extends IllegalArgumentException {
    static final long serialVersionUID = -2848938806368998894L;

    public NumberFormatException(String s) {
        super(s);
    }

    static NumberFormatException forInputString(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("For input string: \"");
        stringBuilder.append(s);
        stringBuilder.append("\"");
        return new NumberFormatException(stringBuilder.toString());
    }
}
