package com.android.server.pfw.log;

import android.util.Log;

public class HwPFWLogger {
    private static final boolean LOG_V = false;
    private static final String TAG = "HwPFWLogger";

    public static void v(String subtag, String msg) {
    }

    public static void d(String subtag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(subtag);
        stringBuilder.append(":");
        stringBuilder.append(msg);
        Log.d(str, stringBuilder.toString());
    }

    public static void i(String subtag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(subtag);
        stringBuilder.append(":");
        stringBuilder.append(msg);
        Log.i(str, stringBuilder.toString());
    }

    public static void w(String subtag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(subtag);
        stringBuilder.append(":");
        stringBuilder.append(msg);
        Log.w(str, stringBuilder.toString());
    }

    public static void e(String subtag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(subtag);
        stringBuilder.append(":");
        stringBuilder.append(msg);
        Log.e(str, stringBuilder.toString());
    }
}
