package huawei.android.security.facerecognition.utils;

import android.util.Log;

public class LogUtil {
    private static final boolean HWDBG;
    private static final boolean HWERROR = true;
    private static final boolean HWINFO;
    private static final String SEPARATOR = " - ";
    private static final String TAG = "FaceR";

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
            stringBuilder.append(tag);
            stringBuilder.append(SEPARATOR);
            stringBuilder.append(msg);
            Log.v(str, stringBuilder.toString());
        }
    }

    public static void v(String tag, String... msg) {
        if (HWDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(SEPARATOR);
            stringBuilder.append(appendString(msg));
            Log.v(str, stringBuilder.toString());
        }
    }

    public static void d(String tag, String msg) {
        if (HWDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(SEPARATOR);
            stringBuilder.append(msg);
            Log.d(str, stringBuilder.toString());
        }
    }

    public static void d(String tag, String... msg) {
        if (HWDBG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(SEPARATOR);
            stringBuilder.append(appendString(msg));
            Log.d(str, stringBuilder.toString());
        }
    }

    public static void i(String tag, String msg) {
        if (HWINFO) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(SEPARATOR);
            stringBuilder.append(msg);
            Log.i(str, stringBuilder.toString());
        }
    }

    public static void i(String tag, String... msg) {
        if (HWINFO) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(tag);
            stringBuilder.append(SEPARATOR);
            stringBuilder.append(appendString(msg));
            Log.i(str, stringBuilder.toString());
        }
    }

    public static void w(String tag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append(SEPARATOR);
        stringBuilder.append(msg);
        Log.w(str, stringBuilder.toString());
    }

    public static void w(String tag, String... msg) {
        w(tag, appendString(msg));
    }

    public static void e(String tag, String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tag);
        stringBuilder.append(SEPARATOR);
        stringBuilder.append(msg);
        Log.e(str, stringBuilder.toString());
    }

    private static String appendString(String... msg) {
        StringBuilder builder = new StringBuilder();
        for (String s : msg) {
            builder.append(s);
            builder.append(" ");
        }
        return builder.toString().trim();
    }
}
