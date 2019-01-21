package com.huawei.displayengine;

import android.util.Log;
import android.util.Slog;

public final class DElog {
    private static final boolean ASSERT = true;
    private static final boolean DEBUG;
    private static final boolean ERROR = true;
    private static final boolean INFO;
    private static final String TAG = "DE J DElog";
    private static final boolean VERBOSE = true;
    private static final boolean WARN = true;

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        DEBUG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        INFO = z;
    }

    private DElog() {
    }

    public static int v(String tag, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.v(tag, stringBuilder.toString());
    }

    public static int v(String tag, String msg, Throwable tr) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.v(tag, stringBuilder.toString(), tr);
    }

    public static int d(String tag, String msg) {
        if (!DEBUG) {
            return 0;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.d(tag, stringBuilder.toString());
    }

    public static int d(String tag, String msg, Throwable tr) {
        if (!DEBUG) {
            return 0;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.d(tag, stringBuilder.toString(), tr);
    }

    public static int i(String tag, String msg) {
        if (!INFO) {
            return 0;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.i(tag, stringBuilder.toString());
    }

    public static int i(String tag, String msg, Throwable tr) {
        if (!INFO) {
            return 0;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.i(tag, stringBuilder.toString(), tr);
    }

    public static int w(String tag, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.w(tag, stringBuilder.toString());
    }

    public static int w(String tag, String msg, Throwable tr) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.w(tag, stringBuilder.toString(), tr);
    }

    public static int w(String tag, Throwable tr) {
        return Slog.w(tag, tr);
    }

    public static int e(String tag, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.e(tag, stringBuilder.toString());
    }

    public static int e(String tag, String msg, Throwable tr) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.e(tag, stringBuilder.toString(), tr);
    }

    public static int wtf(String tag, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.wtf(tag, stringBuilder.toString());
    }

    public static void wtfQuiet(String tag, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        Slog.wtfQuiet(tag, stringBuilder.toString());
    }

    public static int wtfStack(String tag, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.wtfStack(tag, stringBuilder.toString());
    }

    public static int wtf(String tag, Throwable tr) {
        return Slog.wtf(tag, tr);
    }

    public static int wtf(String tag, String msg, Throwable tr) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.wtf(tag, stringBuilder.toString(), tr);
    }

    public static int println(int priority, String tag, String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[effect] ");
        stringBuilder.append(msg);
        return Slog.println(priority, tag, stringBuilder.toString());
    }
}
