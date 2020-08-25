package com.huawei.hiai.awareness.common.log;

import android.util.Log;

public class LogUtil {
    public static final String LOG_TAG = " CAWARENESS_CLIENT: ";
    private static boolean sIsDebugLogEnable;
    private static boolean sIsErrorLogEnable = true;
    private static boolean sIsInfoLogEnable = true;
    private static boolean sIsWarnLogEnable = true;

    static {
        sIsDebugLogEnable = false;
        if (SystemPropertiesUtil.isDebugOn()) {
            Log.i(LOG_TAG, "debug log on");
            sIsDebugLogEnable = true;
            return;
        }
        Log.i(LOG_TAG, "debug log off");
    }

    private LogUtil() {
    }

    public static void i(String tag, String msg) {
        if (tag != null && msg != null && sIsInfoLogEnable) {
            Log.i(tag, LOG_TAG + msg);
        }
    }

    public static void i(String tag, String msg, Throwable e) {
        if (tag != null && msg != null && sIsInfoLogEnable) {
            Log.i(tag, LOG_TAG + msg, e);
        }
    }

    public static void d(String tag, String msg) {
        if (tag != null && msg != null && sIsDebugLogEnable) {
            Log.d(tag, LOG_TAG + msg);
        }
    }

    public static void d(String tag, String msg, Throwable e) {
        if (tag != null && msg != null && sIsDebugLogEnable) {
            Log.d(tag, LOG_TAG + msg, e);
        }
    }

    public static void w(String tag, String msg) {
        if (tag != null && msg != null && sIsWarnLogEnable) {
            Log.w(tag, LOG_TAG + msg);
        }
    }

    public static void w(String tag, String msg, Throwable e) {
        if (tag != null && msg != null && sIsWarnLogEnable) {
            Log.w(tag, LOG_TAG + msg, e);
        }
    }

    public static void e(String tag, String msg) {
        if (tag != null && msg != null && sIsErrorLogEnable) {
            Log.e(tag, LOG_TAG + msg);
        }
    }

    public static void e(String tag, String msg, Throwable e) {
        if (tag != null && msg != null && sIsErrorLogEnable) {
            Log.e(tag, LOG_TAG + msg, e);
        }
    }
}
