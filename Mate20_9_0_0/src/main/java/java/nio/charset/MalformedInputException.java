package java.nio.charset;

public class MalformedInputException extends CharacterCodingException {
    private static final long serialVersionUID = -3438823399834806194L;
    private int inputLength;

    public MalformedInputException(int inputLength) {
        this.inputLength = inputLength;
    }

    public int getInputLength() {
        return this.inputLength;
    }

    public String getMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Input length = ");
        stringBuilder.append(this.inputLength);
        return stringBuilder.toString();
    }
}
