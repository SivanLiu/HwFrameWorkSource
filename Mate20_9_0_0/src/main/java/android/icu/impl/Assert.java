package android.icu.impl;

public class Assert {
    public static void fail(Exception e) {
        fail(e.toString());
    }

    public static void fail(String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("failure '");
        stringBuilder.append(msg);
        stringBuilder.append("'");
        throw new IllegalStateException(stringBuilder.toString());
    }

    public static void assrt(boolean val) {
        if (!val) {
            throw new IllegalStateException("assert failed");
        }
    }

    public static void assrt(String msg, boolean val) {
        if (!val) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("assert '");
            stringBuilder.append(msg);
            stringBuilder.append("' failed");
            throw new IllegalStateException(stringBuilder.toString());
        }
    }
}
