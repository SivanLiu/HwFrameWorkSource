package org.bouncycastle.crypto.tls;

public class AlertLevel {
    public static final short fatal = (short) 2;
    public static final short warning = (short) 1;

    public static String getName(short s) {
        switch (s) {
            case (short) 1:
                return "warning";
            case (short) 2:
                return "fatal";
            default:
                return "UNKNOWN";
        }
    }

    public static String getText(short s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getName(s));
        stringBuilder.append("(");
        stringBuilder.append(s);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
