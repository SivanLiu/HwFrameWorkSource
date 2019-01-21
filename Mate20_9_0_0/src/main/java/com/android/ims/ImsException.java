package com.android.ims;

public class ImsException extends Exception {
    private int mCode;

    public ImsException(String message, int code) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(message);
        stringBuilder.append("(");
        stringBuilder.append(code);
        stringBuilder.append(")");
        super(stringBuilder.toString());
        this.mCode = code;
    }

    public ImsException(String message, Throwable cause, int code) {
        super(message, cause);
        this.mCode = code;
    }

    public int getCode() {
        return this.mCode;
    }
}
