package com.huawei.tmr.util;

import android.util.Log;

public class TMRManager {
    private static final String TAG = "TMRManager";

    public static native int[] getAddr(String str);

    public native String getVersion();

    static {
        try {
            System.loadLibrary("HwTmr");
        } catch (UnsatisfiedLinkError e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("loadLibrary tmr has an error  >>>> ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }
}
