package com.huawei.opcollect.utils;

import android.os.SystemProperties;
import android.util.Log;

public final class OPCollectLog {
    private static final boolean DEBUG_D = false;
    private static final boolean DEBUG_E = true;
    private static final boolean DEBUG_I = true;
    private static final boolean DEBUG_V = false;
    private static final boolean DEBUG_W = true;
    private static final String TAG = "OPCollectLog";

    public static void v(String tag, String msg) {
        if (readLogState()) {
            Log.v(TAG, tag + ":" + msg);
        }
    }

    public static void d(String tag, String msg) {
        if (readLogState()) {
            Log.d(TAG, tag + ":" + msg);
        }
    }

    public static void i(String tag, String msg) {
        Log.i(TAG, tag + ":" + msg);
    }

    public static void w(String tag, String msg) {
        Log.w(TAG, tag + ":" + msg);
    }

    public static void e(String tag, String msg) {
        Log.e(TAG, tag + ":" + msg);
    }

    public static void r(String tag, String msg) {
        Log.i(TAG, tag + ":" + msg);
    }

    private static boolean readLogState() {
        return SystemProperties.getBoolean("persist.opcollect.debug", false);
    }
}
