package com.android.internal.telephony;

public class EncodeException extends Exception {
    public EncodeException(String s) {
        super(s);
    }

    public EncodeException(char c) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unencodable char: '");
        stringBuilder.append(c);
        stringBuilder.append("'");
        super(stringBuilder.toString());
    }
}
