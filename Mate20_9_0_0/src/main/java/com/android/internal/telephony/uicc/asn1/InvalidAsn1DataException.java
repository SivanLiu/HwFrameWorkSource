package com.android.internal.telephony.uicc.asn1;

public class InvalidAsn1DataException extends Exception {
    private final int mTag;

    public InvalidAsn1DataException(int tag, String message) {
        super(message);
        this.mTag = tag;
    }

    public InvalidAsn1DataException(int tag, String message, Throwable throwable) {
        super(message, throwable);
        this.mTag = tag;
    }

    public int getTag() {
        return this.mTag;
    }

    public String getMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.getMessage());
        stringBuilder.append(" (tag=");
        stringBuilder.append(this.mTag);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
