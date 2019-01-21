package android.rms.iaware;

import android.util.Log;

public final class AwareLog {
    private static final boolean HWDBG;
    private static final boolean HWFLOW;
    private static final boolean HWLOGW_E = true;
    private static final boolean HWVERBOSE;
    private static final String TAG = "AwareLog";

    static {
        boolean z = true;
        boolean z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 2));
        HWVERBOSE = z2;
        z2 = Log.HWLog || (Log.HWModuleLog && Log.isLoggable(TAG, 3));
        HWDBG = z2;
        if (!(Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)))) {
            z = false;
        }
        HWFLOW = z;
    }

    public static void v(String tag, String msg) {
        if (HWVERBOSE) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(": ");
            stringBuilder.append(msg);
            Log.v(str, stringBuilder.toString());
        }
    }

    public static void d(String tag, String msg) {
        if (HWDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(": ");
            stringBuilder.append(msg);
            Log.d(str, stringBuilder.toString());
        }
    }

    public static void i(String tag, String msg) {
        if (HWFLOW) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(": ");
            stringBuilder.append(msg);
            Log.i(str, stringBuilder.toString());
        }
    }

    public static void w(String tag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append(": ");
        stringBuilder.append(msg);
        Log.w(str, stringBuilder.toString());
    }

    public static void e(String tag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append(": ");
        stringBuilder.append(msg);
        Log.e(str, stringBuilder.toString());
    }
}
