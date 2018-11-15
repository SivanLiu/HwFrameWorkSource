package com.android.server.devicepolicy;

import android.util.Log;

public class HwLog {
    public static final String ERROR_PREFIX = "error_mdm: ";
    private static final boolean HWDBG;
    private static final boolean HWINFO;
    public static final String TAG = "HwDPMS";
    public static final String WARNING_PREFIX = "warning_mdm: ";

    static {
        boolean z = true;
        boolean z2 = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDBG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWINFO = z;
    }

    public static void v(String tag, String msg) {
        if (HWDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(makeupForString(tag));
            stringBuilder.append(msg);
            Log.v(str, stringBuilder.toString());
        }
    }

    public static void d(String tag, String msg) {
        if (HWDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(makeupForString(tag));
            stringBuilder.append(msg);
            Log.d(str, stringBuilder.toString());
        }
    }

    public static void i(String tag, String msg) {
        if (HWINFO) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(makeupForString(tag));
            stringBuilder.append(msg);
            Log.i(str, stringBuilder.toString());
        }
    }

    public static void w(String tag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(makeupForString(tag));
        stringBuilder.append(WARNING_PREFIX);
        stringBuilder.append(msg);
        Log.w(str, stringBuilder.toString());
    }

    public static void e(String tag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(makeupForString(tag));
        stringBuilder.append(ERROR_PREFIX);
        stringBuilder.append(msg);
        Log.e(str, stringBuilder.toString());
    }

    public static String makeupForString(String tag) {
        StringBuffer sb = new StringBuffer(tag);
        while (sb.length() < 8) {
            sb.append("-");
        }
        return sb.toString();
    }
}
