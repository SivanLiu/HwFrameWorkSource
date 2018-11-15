package android.text.method;

public class HideReturnsTransformationMethod extends ReplacementTransformationMethod {
    private static char[] ORIGINAL = new char[]{'\r'};
    private static char[] REPLACEMENT = new char[]{'﻿'};
    private static HideReturnsTransformationMethod sInstance;

    protected char[] getOriginal() {
        return ORIGINAL;
    }

    protected char[] getReplacement() {
        return REPLACEMENT;
    }

    public static HideReturnsTransformationMethod getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        sInstance = new HideReturnsTransformationMethod();
        return sInstance;
    }
}
