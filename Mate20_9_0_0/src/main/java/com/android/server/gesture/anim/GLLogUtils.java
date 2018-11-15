package com.android.server.gesture.anim;

import android.util.Log;

public class GLLogUtils {
    private static final String TAG = "GestureBackAnimation";

    private GLLogUtils() {
    }

    public static void logD(String tag, String content) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append("-");
        stringBuilder.append(content);
        Log.d(str, stringBuilder.toString());
    }

    public static void logV(String tag, String content) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append("-");
        stringBuilder.append(content);
        Log.v(str, stringBuilder.toString());
    }

    public static void logW(String tag, String content) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append("-");
        stringBuilder.append(content);
        Log.w(str, stringBuilder.toString());
    }

    public static void logE(String tag, String content) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append("-");
        stringBuilder.append(content);
        Log.e(str, stringBuilder.toString());
    }
}
