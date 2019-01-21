package com.huawei.device.connectivitychrlog;

import android.util.Log;

public class ChrLog {
    public static final boolean HWDBG;
    public static final boolean HWFLOW;
    private static final String TAG = TAG_PREFIX;
    private static String TAG_PREFIX = "CHR_";

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG_PREFIX, 3));
        HWDBG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG_PREFIX, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    public static void chrLogD(String tag, String values) {
        if (HWDBG) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(TAG_PREFIX);
            stringBuilder.append(tag);
            Log.d(stringBuilder.toString(), values);
        }
    }

    public static void chrLogE(String tag, String values) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(TAG_PREFIX);
        stringBuilder.append(tag);
        Log.e(stringBuilder.toString(), values);
    }

    public static void chrLogI(String tag, String values) {
        if (HWFLOW) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(TAG_PREFIX);
            stringBuilder.append(tag);
            Log.i(stringBuilder.toString(), values);
        }
    }

    public static void chrLogW(String tag, String values) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(TAG_PREFIX);
        stringBuilder.append(tag);
        Log.w(stringBuilder.toString(), values);
    }
}
