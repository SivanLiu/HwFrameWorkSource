package com.huawei.information;

import android.util.Log;

public class HwDeviceInfo {
    private static final String TAG = "HwDeviceInfo";
    private static final Object lock = new Object();

    private static native String get_emmc_id();

    static {
        try {
            System.loadLibrary("hwdeviceinfo");
        } catch (UnsatisfiedLinkError e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Load libarary hwdeviceinfo failed >>>>>");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        }
    }

    public static String getEMMCID() {
        String str;
        synchronized (lock) {
            try {
                Log.d(TAG, "HwDeviceInfo 64bits so, getEMMCID");
                str = get_emmc_id();
            } catch (UnsatisfiedLinkError e) {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("libarary hwdeviceinfo get_emmc_id failed >>>>>");
                stringBuilder.append(e);
                Log.e(str2, stringBuilder.toString());
                return null;
            } catch (Throwable th) {
            }
        }
        return str;
    }
}
