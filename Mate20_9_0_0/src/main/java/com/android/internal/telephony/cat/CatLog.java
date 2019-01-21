package com.android.internal.telephony.cat;

import android.telephony.Rlog;

public abstract class CatLog {
    static final boolean DEBUG = true;

    public static void d(Object caller, String msg) {
        String className = caller.getClass().getName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(className.substring(className.lastIndexOf(46) + 1));
        stringBuilder.append(": ");
        stringBuilder.append(msg);
        Rlog.d("CAT", stringBuilder.toString());
    }

    public static void d(String caller, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(caller);
        stringBuilder.append(": ");
        stringBuilder.append(msg);
        Rlog.d("CAT", stringBuilder.toString());
    }

    public static void e(Object caller, String msg) {
        String className = caller.getClass().getName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(className.substring(className.lastIndexOf(46) + 1));
        stringBuilder.append(": ");
        stringBuilder.append(msg);
        Rlog.e("CAT", stringBuilder.toString());
    }

    public static void e(String caller, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(caller);
        stringBuilder.append(": ");
        stringBuilder.append(msg);
        Rlog.e("CAT", stringBuilder.toString());
    }
}
