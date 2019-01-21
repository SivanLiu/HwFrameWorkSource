package android.icu.util;

public class IllformedLocaleException extends RuntimeException {
    private static final long serialVersionUID = 1;
    private int _errIdx;

    public IllformedLocaleException() {
        this._errIdx = -1;
    }

    public IllformedLocaleException(String message) {
        super(message);
        this._errIdx = -1;
    }

    public IllformedLocaleException(String message, int errorIndex) {
        String str;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(message);
        if (errorIndex < 0) {
            str = "";
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" [at index ");
            stringBuilder2.append(errorIndex);
            stringBuilder2.append("]");
            str = stringBuilder2.toString();
        }
        stringBuilder.append(str);
        super(stringBuilder.toString());
        this._errIdx = -1;
        this._errIdx = errorIndex;
    }

    public int getErrorIndex() {
        return this._errIdx;
    }
}
