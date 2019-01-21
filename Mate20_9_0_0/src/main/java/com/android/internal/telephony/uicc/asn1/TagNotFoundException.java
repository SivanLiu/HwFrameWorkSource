package com.android.internal.telephony.uicc.asn1;

public class TagNotFoundException extends Exception {
    private final int mTag;

    public TagNotFoundException(int tag) {
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
